package com.strefagentelmena.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.AgentDebugLog
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fireBase.FirebaseEmployeeFunctions
import com.strefagentelmena.functions.fireBase.FirebaseFunctionsAppointments
import com.strefagentelmena.functions.fireBase.FirebaseProfilePreferences
import com.strefagentelmena.functions.fireBase.getAllCustomersFromFirebase
import com.strefagentelmena.functions.fireBase.patchCustomerDerivedVisitFields
import com.strefagentelmena.functions.isNotificationSendWindow
import com.strefagentelmena.functions.smsManager
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.DEFAULT_APPOINTMENT_DURATION_MINUTES
import com.strefagentelmena.models.appoimentsModel.effectiveEmployeeId
import com.strefagentelmena.models.appoimentsModel.parseAppointmentTimeString
import com.strefagentelmena.models.appoimentsModel.findFirstSchedulingConflict
import com.strefagentelmena.models.appoimentsModel.formatSchedulingConflictMessage
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.CustomerNote
import com.strefagentelmena.models.effectiveCustomerId
import com.strefagentelmena.models.computeCustomerVisitAggregates
import com.strefagentelmena.models.mergeCustomersWithVisitStats
import com.strefagentelmena.models.notesOrderedNewestFirst
import com.strefagentelmena.models.normalizedForFirebaseWrite
import com.strefagentelmena.models.withVisitAggregatesFromAppointments
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import com.strefagentelmena.models.settngsModel.isOnVacationOn
import com.strefagentelmena.models.settngsModel.vacationRangeLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

class ScheduleModelView : ViewModel() {
    val customersList = MutableLiveData<List<Customer>>()
    val appointmentsList = MutableLiveData<List<Appointment>>()
    private val profilePreferences = MutableLiveData<ProfilePreferences>()
    val selectedEmployee = MutableLiveData<Employee?>()
    val employeeList = MutableLiveData<List<Employee>>()
    private val currentBaseAppointmentsList = MutableLiveData<List<Appointment>>()

    /** Wszystkie wizyty (przed filtrem dnia) — m.in. kropki na pasku dni harmonogramu. */
    val allAppointmentsUnfiltered: LiveData<List<Appointment>>
        get() = currentBaseAppointmentsList

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val currentDate: String = LocalDate.now().format(dateFormatter)
    private val currentTime: String = LocalTime.now().format(timeFormatter)

    val selectedClient = MutableLiveData<Customer>()
    /** Konto rodzica / główne przy wyborze wizyty (zawsze [Customer.parentCustomerId] == 0). */
    val selectedFamilyRootCustomer = MutableLiveData<Customer>(Customer())
    val isNewAppointment = MutableLiveData<Boolean>(false)

    val messages = MutableLiveData<String?>("")
    val viewState = MutableLiveData<AppState>(AppState.Idle)

    /**
     *Dialogs states
     */
    val appointmentDialog = MutableLiveData(false)
    val deleteDialogState = MutableLiveData(false)
    val onNotificationClickState = MutableLiveData(false)
    /** Po zapisie edycji wizyty — pytanie o ponowne wysłanie SMS (null = brak dialogu). */
    val resendNotificationAfterEdit = MutableLiveData<Appointment?>(null)
    val appointmentError = MutableLiveData("")
    /** Informacje o nakładaniu się wizyt / poza godzinami pracy — bez blokady zapisu. */
    val appointmentScheduleNotice = MutableLiveData("")
    val shouldOpenDialog = MutableLiveData(false)

    /**
     * Appoiments Data
     *
     */
    val selectedAppointmentDate = MutableLiveData(currentDate)
    val selectedAppointmentStartTime = MutableLiveData(currentTime)
    val selectedAppointment = MutableLiveData(Appointment())
    val selectedAppointmentEndTime =
        MutableLiveData(LocalTime.now().plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES).format(timeFormatter))
    val selectedAppointmentNote = MutableLiveData("")
    val selectedAppointmentNoteHistory = MutableLiveData<List<CustomerNote>>(emptyList())
    /** Zakres usług na karcie harmonogramu (makieta: `.appt-service`). */
    val selectedAppointmentServiceDescription = MutableLiveData("")


    fun setNewTime(time: String) {
        val parsed = parseAppointmentTimeString(time)
        // #region agent log
        AgentDebugLog.log(
            hypothesisId = "H1",
            location = "ScheduleModelView.setNewTime",
            message = "time_picker_or_update",
            data = mapOf(
                "rawLen" to time.length.toString(),
                "parsedOk" to (parsed != null).toString(),
                "isNewAppt" to (isNewAppointment.value == true).toString(),
            ),
        )
        // #endregion
        if (parsed != null) {
            selectedAppointmentStartTime.value = parsed.format(timeFormatter)
        } else {
            appointmentError.value = "Niepoprawny format godziny"
        }
    }

    fun setAppoimentNote(newValue: String) {
        selectedAppointmentNote.value = newValue
    }

    fun appendSelectedAppointmentNote() {
        val text = selectedAppointmentNote.value?.trim().orEmpty()
        if (text.isBlank()) return
        val entry = CustomerNote(text = text, addedAtMillis = System.currentTimeMillis())
        selectedAppointmentNoteHistory.value = listOf(entry) + selectedAppointmentNoteHistory.value.orEmpty()
        selectedAppointmentNote.value = ""
    }

    private fun notesArrayListForSelectedClientSave(): ArrayList<CustomerNote>? {
        val draft = selectedAppointmentNote.value?.trim().orEmpty()
        val fromHistory = selectedAppointmentNoteHistory.value.orEmpty()
        val combined = buildList {
            if (draft.isNotEmpty()) add(CustomerNote(draft, System.currentTimeMillis()))
            addAll(fromHistory)
        }.sortedByDescending { it.addedAtMillis }
        if (combined.isEmpty()) return null
        return ArrayList(combined)
    }

    fun setAppointmentServiceDescription(newValue: String) {
        selectedAppointmentServiceDescription.value = newValue
    }

    fun setAppoimentState(newValue: Boolean) {
        isNewAppointment.value = newValue
    }

    fun setAppointmentEndTime(newValue: LocalTime) {
        selectedAppointmentEndTime.value = newValue.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    fun setAppoimentError(newValue: String) {
        appointmentError.value = newValue
    }

    fun clearAppointmentScheduleNotice() {
        appointmentScheduleNotice.value = ""
    }

    fun setEmpolyee(newValue: Employee) {
        selectedEmployee.value = newValue
        checkAppointmentsList()
    }

    fun changeNotificationDialogState() {
        onNotificationClickState.value = !onNotificationClickState.value!!
    }

    fun openNotificationDialog() {
        onNotificationClickState.value = true
    }

    fun dismissNotificationDialog() {
        onNotificationClickState.value = false
    }

    fun dismissResendNotificationAfterEdit() {
        resendNotificationAfterEdit.value = null
    }

    fun confirmResendNotificationAfterEdit() {
        val ap = resendNotificationAfterEdit.value ?: return
        resendNotificationAfterEdit.value = null
        sendNotificationResend(ap)
    }


    fun clearMessages() {
        messages.value = null
    }

    fun setMessages(newValue: String) {
        messages.value = newValue
    }

    fun setSelectedClient(newValue: Customer) {
        selectedClient.value = newValue
        selectedAppointmentNote.value = ""
        selectedAppointmentNoteHistory.value = newValue.notesOrderedNewestFirst()
    }

    fun setSelectedFamilyRootForAppointment(root: Customer) {
        selectedFamilyRootCustomer.value = root
        setSelectedClient(root)
        checkAppointmentsList()
    }

    fun setAppointmentSubjectForFamily(subject: Customer) {
        setSelectedClient(subject)
        checkAppointmentsList()
    }

    private fun syncFamilyRootFromSubject(subject: Customer) {
        if (subject.parentCustomerId > 0) {
            val root = customersList.value?.firstOrNull { it.id == subject.parentCustomerId }
                ?: Customer()
            selectedFamilyRootCustomer.value = root
        } else {
            selectedFamilyRootCustomer.value = subject
        }
    }

    /** Numer do SMS: rodzic, gdy wizyta jest zapisana na dziecko. */
    fun resolveSmsContactPhone(appointment: Appointment): String? {
        val parentId = appointment.customer.parentCustomerId.takeIf { it > 0 }
            ?: appointment.smsContactCustomerId.takeIf { it > 0 }
            ?: return null
        return customersList.value
            ?.firstOrNull { it.id == parentId }
            ?.phoneNumber
            ?.takeIf { it.isNotBlank() }
    }

    fun setNewAppoimentsDate(newValue: LocalDate) {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val formattedDate = newValue.format(formatter)
        selectedAppointmentDate.value = formattedDate
        ensureSelectedEmployeeAvailableForCurrentDate()
        getsAppoiments()
        checkAppointmentsList()
    }

    fun setAppState(newValue: AppState) {
        viewState.value = newValue
    }

    /**
     * Clear Date.
     *
     */
    fun clearDate() {
        appointmentError.value = ""
        appointmentScheduleNotice.value = ""
        selectedClient.value = Customer()
        selectedFamilyRootCustomer.value = Customer()
        selectedAppointment.value = null
        selectedAppointmentNote.value = ""
        selectedAppointmentNoteHistory.value = emptyList()
        selectedAppointmentServiceDescription.value = ""
    }

    fun setNewDataAppointment(newValue: String) {
        val formattedDate = LocalDate.parse(newValue, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        selectedAppointmentDate.value = formattedDate
    }


    fun setDeleteDialogState() {
        deleteDialogState.value = !deleteDialogState.value!!
    }

    /**
     * Select Appointment.
     *
     * @param appointment
     */
    fun selectAppointmentAndClient(appointment: Appointment) {
        selectedAppointment.value = appointment
        selectedAppointmentStartTime.value = appointment.startTime
        selectedAppointmentEndTime.value = appointment.endTime
        selectedAppointmentDate.value = appointment.date
        selectedAppointmentServiceDescription.value = appointment.serviceDescription

        val cid = appointment.effectiveCustomerId()
        val fromList = customersList.value?.firstOrNull { it.id == cid }
        val cust = fromList ?: appointment.customer
        setSelectedClient(cust)
        syncFamilyRootFromSubject(cust)
    }

    /**
     * Show Apoiment Dialog.
     *
     */
    fun changeAppointmentDialogState() {
        appointmentDialog.value = !appointmentDialog.value!!
    }

    /**
     * Formularz nowej wizyty z wybranym klientem (np. z dashboardu „ostatnie wizyty”).
     * [followUpFrom] — kolejna wizyta po trwającej: ta sama data, początek od końca wizyty, ten sam pracownik (jeśli znany).
     */
    fun openNewAppointmentWithCustomer(
        customer: Customer,
        followUpFrom: Appointment? = null,
    ) {
        isNewAppointment.value = true
        appointmentError.value = ""
        appointmentScheduleNotice.value = ""
        syncFamilyRootFromSubject(customer)
        selectedClient.value = customer
        selectedAppointmentNote.value = ""
        selectedAppointmentNoteHistory.value = customer.notesOrderedNewestFirst()
        selectedAppointmentServiceDescription.value = ""
        selectedAppointment.value = Appointment()
        val follow = followUpFrom
        if (follow != null) {
            selectedAppointmentDate.value = follow.date.trim().ifEmpty {
                LocalDate.now().format(dateFormatter)
            }
            val endT = parseAppointmentTimeString(follow.endTime)
            val startForNext = endT ?: LocalTime.now()
            selectedAppointmentStartTime.value = startForNext.format(timeFormatter)
            setAppointmentEndTime(startForNext.plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES))
            val eid = follow.effectiveEmployeeId()
            if (eid != null && eid != 0) {
                employeeList.value?.firstOrNull { it.id == eid }?.let { emp ->
                    selectedEmployee.value = emp
                    checkAppointmentsList()
                }
            }
        } else {
            selectedAppointmentDate.value = LocalDate.now().format(dateFormatter)
            val now = LocalTime.now()
            selectedAppointmentStartTime.value = now.format(timeFormatter)
            setAppointmentEndTime(now.plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES))
        }
        appointmentDialog.value = true
    }

    /**
     * Find customer by name
     *
     * @param name
     * @return
     */
    fun findCustomerByName(name: String): Customer? {
        return customersList.value?.firstOrNull { it.fullName == name }
    }

    fun findWorkerByDisplayName(label: String): Employee? {
        val t = label.trim()
        if (t.isEmpty()) return null
        return employeeList.value?.firstOrNull { it.displayName == t }
            ?: employeeList.value?.firstOrNull { it.name.trim() == t }
    }

    fun prepareAppointmentDetails() {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        if (isNewAppointment.value == true) {
            val currentTime = LocalTime.now()
            clearDate()
            setNewTime(currentTime.format(timeFormatter)) // `setNewTime` oczekuje String, tutaj jest OK
            setAppointmentEndTime(currentTime.plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES)) // Przekazujemy LocalTime
        } else {
            val endTime = selectedAppointment.value?.endTime?.let {
                try {
                    LocalTime.parse(it, timeFormatter)
                } catch (e: Exception) {
                    null // Wartość null, jeśli parsowanie się nie powiedzie
                }
            } ?: LocalTime.now().plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES) // Domyślny czas

            setAppointmentEndTime(endTime) // Przekazujemy LocalTime
        }
    }


    fun createNewAppointment(
        isNew: Boolean,
    ) {
        vacationViolationMessageOrNull()?.let {
            appointmentError.value = it
            return
        }

        val id = if (isNew) appointmentsList.value?.size else selectedAppointment.value?.id
        if (!isNew && id == null) {
            appointmentError.value = "Brak wybranej wizyty"
            return
        }
        // #region agent log
        AgentDebugLog.log(
            hypothesisId = "H-C",
            location = "ScheduleModelView.createNewAppointment",
            message = "temp_id_before_build",
            data = mapOf(
                "isNew" to isNew.toString(),
                "tempId" to id?.toString(),
                "filteredListSize" to (appointmentsList.value?.size ?: -1).toString(),
            ),
        )
        // #endregion

        val startParsed = parseAppointmentTimeString(selectedAppointmentStartTime.value)
        val endParsed = parseAppointmentTimeString(selectedAppointmentEndTime.value)
        if (startParsed == null || endParsed == null) {
            appointmentError.value = "Niepoprawny format godziny"
            return
        }
        val startTime = startParsed.format(timeFormatter)
        val endTime = endParsed.format(timeFormatter)

        val emp = selectedEmployee.value ?: return
        val subject = selectedClient.value ?: Customer()
        if (subject.id <= 0) {
            appointmentError.value = "Wybierz rodzica / konto i osobę na wizytę"
            return
        }
        val resolvedId = id ?: ((appointmentsList.value?.size ?: 0) + 1)
        val notesForSave = notesArrayListForSelectedClientSave()
        val customerWithNotes = subject.copy(
            notes = notesForSave,
            noted = notesForSave?.firstOrNull()?.text
                ?.trim()
                .orEmpty()
                .ifEmpty { (selectedClient.value ?: Customer()).noted.trim() },
        )
        selectedClient.value = customerWithNotes

        val smsParent = customerWithNotes.parentCustomerId.takeIf { it > 0 } ?: 0
        val new = Appointment(
            id = resolvedId,
            customer = customerWithNotes,
            date = selectedAppointmentDate.value ?: return,
            startTime = startTime, // Przechowywany jako String
            notificationSent = false,
            endTime = endTime, // Przechowywany jako String
            serviceDescription = selectedAppointmentServiceDescription.value.orEmpty(),
            employeeId = emp.id ?: 0,
            employee = emp,
            smsContactCustomerId = smsParent,
        )

        selectedAppointment.value = new

        addAppointment(firebaseDatabase = FirebaseDatabase.getInstance())
        changeAppointmentDialogState()
    }


    private suspend fun loadEmployesFromFireBase(): Employee? {
        val firebaseDatabase = FirebaseDatabase.getInstance()

        // Pobieramy listę pracowników z Firebase
        val employees = FirebaseEmployeeFunctions().loadEmployeesFromFirebase(firebaseDatabase)

        // Jeśli selectedEmployee jest null, zwróć pierwszego pracownika z listy
        return if (selectedEmployee.value == null) {
            employees.firstOrNull() // Zwracamy pierwszy pracownik lub null, jeśli lista jest pusta
        } else {
            selectedEmployee.value // Jeśli selectedEmployee jest ustawiony, zwrócimy tego pracownika
        }
    }


    private fun setWorkersList(newValue: List<Employee>) {
        employeeList.value = newValue
    }

    private fun selectWorker(newValue: Employee) {
        selectedEmployee.value = newValue
    }


    /**
     * Add Appointment.
     *
     */
    private fun addAppointment(firebaseDatabase: FirebaseDatabase) {
        viewModelScope.launch {
            try {
                // Pobierz aktualną listę wizyt z Firebase
                val currentAppointments =
                    FirebaseFunctionsAppointments().loadAppointmentsFromFirebase(firebaseDatabase)
                        .toMutableList()

                // Sprawdź nową wizytę
                val newAppointment = selectedAppointment.value
                if (newAppointment != null) {
                    // Znajdź maksymalne ID w istniejących wizytach
                    val nextAppointmentId = if (currentAppointments.isNotEmpty()) {
                        currentAppointments.maxOf { it.id } + 1
                    } else {
                        0 // Jeśli brak wizyt, zaczynamy od 0
                    }

                    val appointmentWithId =
                        newAppointment.copy(id = nextAppointmentId).normalizedForFirebaseWrite()

                    // Dodaj wizytę do lokalnej listy
                    currentAppointments.add(appointmentWithId)

                    // Pełna lista w bazie lokalnej; dzień + pracownik — przez getsAppoiments()
                    currentBaseAppointmentsList.value = currentAppointments
                    getsAppoiments()
                    // #region agent log
                    AgentDebugLog.log(
                        hypothesisId = "H-A",
                        location = "ScheduleModelView.addAppointment",
                        message = "after_set_full_list_before_firebase_callback",
                        data = mapOf(
                            "liveDataSizeNow" to (currentAppointments.size).toString(),
                            "selectedDate" to (selectedAppointmentDate.value ?: ""),
                            "workerId" to (selectedEmployee.value?.id?.toString() ?: "null"),
                        ),
                    )
                    // #endregion

                    // Dodaj wizytę do Firebase
                    FirebaseFunctionsAppointments().addNewAppointmentToFirebase(
                        firebaseDatabase, appointmentWithId
                    ) { success ->
                        if (success) {
                            val snapshot = currentAppointments.toList()
                            patchCustomerDerivedVisitFields(
                                firebaseDatabase,
                                appointmentWithId.customer.id,
                                snapshot,
                            )
                            refreshCustomerAggregatesInSchedule(
                                appointmentWithId.customer.id,
                                snapshot,
                            )
                            setMessages("${appointmentWithId.customer.fullName} już niedługo na twoim fotelu")
                            // #region agent log
                            AgentDebugLog.log(
                                hypothesisId = "H-A",
                                location = "ScheduleModelView.addAppointment.callback",
                                message = "firebase_ok_before_getsAppoiments",
                                data = mapOf(
                                    "baseSize" to (currentBaseAppointmentsList.value?.size ?: -1).toString(),
                                ),
                            )
                            // #endregion
                            getsAppoiments()
                            Log.d(
                                "Firebase",
                                "Appointment added successfully with ID: ${appointmentWithId.id}"
                            )
                        } else {
                            setMessages("Błąd dodawania wizyty")
                            Log.e("Firebase", "Error adding appointment")
                        }
                    }
                } else {
                    Log.e("AddAppointment", "New appointment is null")
                    setMessages("Nie można dodać pustej wizyty")
                }
            } catch (e: Exception) {
                Log.e("AddAppointment", "Error while adding appointment: ${e.message}", e)
                setMessages("Wystąpił błąd podczas dodawania wizyty")
            }
        }
    }


    private fun getSelectedClient(): Customer? {
        val customers = customersList.value ?: return null
        val sid = selectedClient.value?.id?.takeIf { it > 0 }
        if (sid != null) {
            customers.firstOrNull { it.id == sid }?.let { return it }
        }
        val aid = selectedAppointment.value?.effectiveCustomerId()?.takeIf { it > 0 }
        if (aid != null) {
            customers.firstOrNull { it.id == aid }?.let { return it }
        }
        val clientName =
            selectedClient.value?.fullName ?: selectedAppointment.value?.customer?.fullName
        return clientName?.let { findCustomerByName(it) }
    }

    /**
     * Remove Appointment.
     *
     * @param id
     */
    fun removeAppointment(id: Int) {
        val currentAppointments = currentBaseAppointmentsList.value?.toMutableList() ?: return
        val removed = currentAppointments.firstOrNull { it.id == id } ?: return
        val customerId = removed.effectiveCustomerId()
        currentAppointments.removeAll { it.id == id }
        val snapshotAfter = currentAppointments.toList()

        currentBaseAppointmentsList.value = currentAppointments
        getsAppoiments()
        // #region agent log
        AgentDebugLog.log(
            hypothesisId = "H-B",
            location = "ScheduleModelView.removeAppointment",
            message = "after_base_list_getsAppoiments",
            data = mapOf(
                "liveDataSizeNow" to currentAppointments.size.toString(),
                "selectedDate" to (selectedAppointmentDate.value ?: ""),
                "workerId" to (selectedEmployee.value?.id?.toString() ?: "null"),
            ),
        )
        // #endregion

        FirebaseFunctionsAppointments().deleteAppointmentFromFirebase(
            FirebaseDatabase.getInstance(),
            selectedAppointment.value?.id ?: return,
            completion = { success ->
                if (success) {
                    val db = FirebaseDatabase.getInstance()
                    patchCustomerDerivedVisitFields(db, customerId, snapshotAfter)
                    refreshCustomerAggregatesInSchedule(customerId, snapshotAfter)
                    setMessages("Wizyta została usunięta")
                } else {
                    setMessages("Błąd usuwania wizyty")
                }
            },
        )
        setMessages("Jedna wizyta mniej do zrobienia")
    }

    /**
     * Edit Appointment.
     *
     */
    fun editAppointment(
        firebaseDatabase: FirebaseDatabase, notificationIsSent: Boolean = false
    ): Boolean {
        // #region agent log
        AgentDebugLog.log(
            hypothesisId = "H5",
            location = "ScheduleModelView.editAppointment",
            message = "entry",
            data = mapOf(
                "apptId" to (selectedAppointment.value?.id?.toString() ?: "null"),
                "clientNameLen" to (selectedClient.value?.fullName?.length ?: 0).toString(),
                "resolvedClient" to (getSelectedClient() != null).toString(),
                "start" to (selectedAppointmentStartTime.value ?: ""),
                "end" to (selectedAppointmentEndTime.value ?: ""),
            ),
        )
        // #endregion
        val appointmentsList = currentBaseAppointmentsList.value?.toMutableList() ?: run {
            logError("No appointments list")
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H5",
                location = "ScheduleModelView.editAppointment",
                message = "exit_false",
                data = mapOf("reason" to "no_appointments_list"),
            )
            // #endregion
            return false
        }
        val selectedClient = getSelectedClient() ?: run {
            logError("Selected client is null")
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H5",
                location = "ScheduleModelView.editAppointment",
                message = "exit_false",
                data = mapOf("reason" to "client_null"),
            )
            // #endregion
            return false
        }
        val customers = customersList.value?.toMutableList() ?: run {
            logError("Customers list is null")
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H5",
                location = "ScheduleModelView.editAppointment",
                message = "exit_false",
                data = mapOf("reason" to "customers_null"),
            )
            // #endregion
            return false
        }

        val appointmentIndex =
            appointmentsList.indexOfFirst { it.id == selectedAppointment.value?.id }
        val clientIndex = customers.indexOfFirst { it.id == selectedClient.id }

        if (appointmentIndex == -1 || clientIndex == -1) {
            logError("Appointment or client not found.")
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H5",
                location = "ScheduleModelView.editAppointment",
                message = "exit_false",
                data = mapOf(
                    "reason" to "index_not_found",
                    "appointmentIndex" to appointmentIndex.toString(),
                    "clientIndex" to clientIndex.toString(),
                ),
            )
            // #endregion
            return false
        }

        val priorDate = selectedAppointment.value?.date.orEmpty()
        val priorStart = selectedAppointment.value?.startTime.orEmpty()
        val priorEnd = selectedAppointment.value?.endTime.orEmpty()
        val priorService = selectedAppointment.value?.serviceDescription.orEmpty()
        val priorCustomerId = selectedAppointment.value?.customer?.id ?: 0
        val priorPhone = selectedAppointment.value?.customer?.phoneNumber
        val priorSmsContact = selectedAppointment.value?.smsContactCustomerId ?: 0

        val mergedNotes = notesArrayListForSelectedClientSave()
        selectedClient.notes = mergedNotes
        selectedClient.noted = mergedNotes?.firstOrNull()?.text?.trim().orEmpty()
            .ifEmpty { selectedClient.noted.trim() }

        onNotificationClickState.value = notificationIsSent

        val startTime =
            validateAndParseTime(selectedAppointmentStartTime.value, "Start time") ?: run {
                // #region agent log
                AgentDebugLog.log(
                    hypothesisId = "H5",
                    location = "ScheduleModelView.editAppointment",
                    message = "exit_false",
                    data = mapOf("reason" to "bad_start_time"),
                )
                // #endregion
                return false
            }
        val endTime =
            validateAndParseTime(selectedAppointmentEndTime.value, "End time") ?: run {
                // #region agent log
                AgentDebugLog.log(
                    hypothesisId = "H5",
                    location = "ScheduleModelView.editAppointment",
                    message = "exit_false",
                    data = mapOf("reason" to "bad_end_time"),
                )
                // #endregion
                return false
            }
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val appointmentDate = selectedAppointmentDate.value?.takeIf { it.isNotEmpty() }
            ?: run {
                logError("Invalid appointment date")
                // #region agent log
                AgentDebugLog.log(
                    hypothesisId = "H5",
                    location = "ScheduleModelView.editAppointment",
                    message = "exit_false",
                    data = mapOf("reason" to "bad_date"),
                )
                // #endregion
                return false
            }
        if (LocalDate.parse(appointmentDate, dateFormatter)
                .isBefore(LocalDate.now())
        ) {
            logError("Appointment date is in the past.")
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H5",
                location = "ScheduleModelView.editAppointment",
                message = "exit_false",
                data = mapOf("reason" to "date_in_past"),
            )
            // #endregion
            return false
        }

        vacationViolationMessageOrNull()?.let {
            appointmentError.value = it
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H5",
                location = "ScheduleModelView.editAppointment",
                message = "exit_false",
                data = mapOf("reason" to "vacation", "errLen" to it.length.toString()),
            )
            // #endregion
            return false
        }

        val smsContact = selectedClient.parentCustomerId.takeIf { it > 0 } ?: 0
        val updatedAppointment = selectedAppointment.value?.copy(
            notificationSent = onNotificationClickState.value ?: notificationIsSent,
            date = appointmentDate,
            startTime = startTime.format(timeFormatter),
            endTime = endTime.format(timeFormatter),
            serviceDescription = selectedAppointmentServiceDescription.value.orEmpty(),
            customer = selectedClient,
            employee = selectedEmployee.value ?: Employee(),
            smsContactCustomerId = smsContact,
        )?.normalizedForFirebaseWrite() ?: run {
            logError("Selected appointment is null")
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H5",
                location = "ScheduleModelView.editAppointment",
                message = "exit_false",
                data = mapOf("reason" to "updated_null"),
            )
            // #endregion
            return false
        }

        appointmentsList[appointmentIndex] = updatedAppointment
        val agg = computeCustomerVisitAggregates(appointmentsList, selectedClient.id)
        customers[clientIndex] = selectedClient.copy(
            lastVisit = agg.lastVisit,
            appointment = agg.latestSlimAppointment,
            visitCount = agg.visitCount,
            avgWeeksBetweenVisits = agg.avgWeeksBetweenVisits,
        )

        updateState(appointmentsList, customers)

        try {
            saveAppointmentToFirebase(
                firebaseDatabase,
                updatedAppointment,
                appointmentsList.toList(),
            )
            Log.d(
                "EditAppointment",
                "Appointment updated successfully with ID: ${updatedAppointment.id}"
            )
        } catch (e: Exception) {
            logError("Error saving appointment to Firebase: ${e.message}")
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H5",
                location = "ScheduleModelView.editAppointment",
                message = "exit_false",
                data = mapOf("reason" to "firebase_exception"),
            )
            // #endregion
            return false
        }

        // #region agent log
        AgentDebugLog.log(
            hypothesisId = "H5",
            location = "ScheduleModelView.editAppointment",
            message = "exit_true",
            data = mapOf("apptId" to updatedAppointment.id.toString()),
        )
        // #endregion
        selectedAppointment.value = updatedAppointment
        if (isNewAppointment.value != true &&
            shouldOfferResendSmsAfterEdit(
                priorDate,
                priorStart,
                priorEnd,
                priorService,
                priorCustomerId,
                priorPhone,
                priorSmsContact,
                updatedAppointment,
            )
        ) {
            resendNotificationAfterEdit.value = updatedAppointment
        }
        return true
    }

    private fun shouldOfferResendSmsAfterEdit(
        priorDate: String,
        priorStart: String,
        priorEnd: String,
        priorService: String,
        priorCustomerId: Int,
        priorPhone: String?,
        priorSmsContact: Int,
        after: Appointment,
    ): Boolean {
        if (priorDate != after.date) return true
        if (priorStart != after.startTime) return true
        if (priorEnd != after.endTime) return true
        if (priorService != after.serviceDescription) return true
        if (priorCustomerId != after.customer.id) return true
        if ((priorPhone ?: "") != (after.customer.phoneNumber ?: "")) return true
        if (priorSmsContact != after.smsContactCustomerId) return true
        return false
    }

    // Funkcja pomocnicza do logowania błędów
    private fun logError(message: String): Unit {
        Log.e("EditAppointment", message)
    }

    // Funkcja pomocnicza do aktualizacji stanu
    private fun updateState(
        appointmentsList: MutableList<Appointment>, customers: MutableList<Customer>
    ) {
        currentBaseAppointmentsList.value = appointmentsList
        customersList.value = customers
        // Tylko wizyty wybranego dnia + pracownika — unikamy mieszania wszystkich dni na osi czasu (ANR / „zamrożenie”).
        getsAppoiments()
    }


    // Funkcja walidująca i parsująca czas
    private fun validateAndParseTime(time: String?, fieldName: String): LocalTime? {
        if (time.isNullOrEmpty()) {
            Log.e("EditAppointment", "$fieldName is missing.")
            return null
        }
        val parsed = parseAppointmentTimeString(time)
        if (parsed == null) {
            Log.e("EditAppointment", "Invalid $fieldName format: $time")
        }
        return parsed
    }

    // Funkcja zapisująca dane do Firebase
    private fun saveAppointmentToFirebase(
        firebaseDatabase: FirebaseDatabase,
        updatedAppointment: Appointment,
        allAppointments: List<Appointment>,
    ) {
        FirebaseFunctionsAppointments().editAppointmentInFirebase(
            firebaseDatabase, updatedAppointment
        ) { success ->
            if (success) {
                patchCustomerDerivedVisitFields(
                    firebaseDatabase,
                    updatedAppointment.customer.id,
                    allAppointments,
                )
                refreshCustomerAggregatesInSchedule(
                    updatedAppointment.customer.id,
                    allAppointments,
                )
                setMessages("Wizyta ${updatedAppointment.customer.fullName} została zaktualizowana.")
            } else {
                setMessages("Błąd edycji wizyty.")
            }
        }
    }

    private fun refreshCustomerAggregatesInSchedule(customerId: Int, appointments: List<Appointment>) {
        val customers = customersList.value?.toMutableList() ?: return
        val idx = customers.indexOfFirst { it.id == customerId }
        if (idx < 0) return
        customers[idx] = customers[idx].withVisitAggregatesFromAppointments(appointments)
        customersList.value = customers
    }


    fun loadAllData() {
        // #region agent log
        AgentDebugLog.log(
            hypothesisId = "H4",
            location = "ScheduleModelView.loadAllData",
            message = "entry",
            data = mapOf(
                "thread" to Thread.currentThread().name,
                "apptFilteredSize" to (appointmentsList.value?.size ?: -1).toString(),
            ),
        )
        // #endregion
        viewModelScope.launch {
            setAppState(AppState.Loading)
            clearMessages()

            try {
                val customersDeferred = async(Dispatchers.IO) {
                    getAllCustomersFromFirebase(FirebaseDatabase.getInstance())
                }
                val appointmentsDeferred = async(Dispatchers.IO) {
                    FirebaseFunctionsAppointments().loadAppointmentsFromFirebase(FirebaseDatabase.getInstance())
                }
                val employeeDeferred = async(Dispatchers.IO) {
                    loadEmployesFromFireBase()
                }
                val profileDeferred = async(Dispatchers.IO) {
                    FirebaseProfilePreferences().loadProfilePreferencesFromFirebase(FirebaseDatabase.getInstance())
                }
                val employeeListDeferred = async(Dispatchers.IO) {
                    FirebaseEmployeeFunctions().loadEmployeesFromFirebase(FirebaseDatabase.getInstance())
                }

                // Pobieranie i przypisanie danych
                val customersRaw = customersDeferred.await() ?: emptyList()

                val employee = employeeDeferred.await()
                if (employee != null) {
                    selectedEmployee.value = employee
                } else {
                    Log.w("LoadAllData", "Pracownik nie został załadowany")
                }

                currentBaseAppointmentsList.value = appointmentsDeferred.await() ?: emptyList()
                setCustomersList(
                    mergeCustomersWithVisitStats(customersRaw, currentBaseAppointmentsList.value.orEmpty()),
                )
                employeeList.value = employeeListDeferred.await() ?: emptyList()
                profilePreferences.value = profileDeferred.await() ?: ProfilePreferences()

                ensureSelectedEmployeeAvailableForCurrentDate()
                setAppointmentsList(currentBaseAppointmentsList.value.orEmpty())
                getsAppoiments()

                setAppState(AppState.Success)
            } catch (e: Exception) {
                Log.e("LoadAllData", "Error loading data: ${e.message}", e)

                setMessages("Błąd ładowania danych")
                setAppState(AppState.Error)
            }
        }
    }


    fun closeAllDialog() {
        appointmentDialog.value = false
        deleteDialogState.value = false
    }

    fun sendNotificationForAppointment() {
        if (selectedAppointment.value?.notificationSent == true) return

        val appointment = selectedAppointment.value ?: return
        val profile = profilePreferences.value ?: return

        val notificationSent = try {
            smsManager.sendNotification(
                appointment,
                profile,
                resolveSmsContactPhone(appointment),
            )

        } catch (e: Exception) {
            setMessages("Błąd podczas wysyłania powiadomienia: ${e.message}")
            Log.e("SendNotification", "Error sending notification: ${e.message}", e)
            return
        }

        if (notificationSent) {
            setMessages("Powiadomienie wysłane do ${appointment.customer.fullName}")
            editAppointment(FirebaseDatabase.getInstance(), true)
        } else {
            explainSmsNotificationNotSent(appointment, profile)
        }
    }

    /**
     * Wysyłka SMS po edycji wizyty — ignoruje flagę [Appointment.notificationSent]
     * (w przeciwieństwie do [sendNotificationForAppointment]).
     */
    fun sendNotificationResend(appointment: Appointment) {
        val profile = profilePreferences.value ?: return

        val notificationSent = try {
            smsManager.sendNotification(
                appointment,
                profile,
                resolveSmsContactPhone(appointment),
            )
        } catch (e: Exception) {
            setMessages("Błąd podczas wysyłania powiadomienia: ${e.message}")
            Log.e("SendNotification", "Error sending notification: ${e.message}", e)
            return
        }

        if (notificationSent) {
            setMessages("Powiadomienie wysłane do ${appointment.customer.fullName}")
            val withFlag = appointment.copy(notificationSent = true).normalizedForFirebaseWrite()
            val appointmentsMutable = currentBaseAppointmentsList.value?.toMutableList() ?: return
            val idx = appointmentsMutable.indexOfFirst { it.id == appointment.id }
            if (idx >= 0) {
                appointmentsMutable[idx] = withFlag
                val customersMutable = customersList.value?.toMutableList() ?: return
                updateState(appointmentsMutable, customersMutable)
                FirebaseFunctionsAppointments().editAppointmentInFirebase(
                    FirebaseDatabase.getInstance(),
                    withFlag,
                ) { success ->
                    if (!success) {
                        setMessages("Nie udało się zapisać statusu powiadomienia dla ${appointment.customer.fullName}")
                    }
                }
            }
            selectedAppointment.value = withFlag
        } else {
            explainSmsNotificationNotSent(appointment, profile)
        }
    }

    private fun explainSmsNotificationNotSent(appointment: Appointment, profile: ProfilePreferences) {
        val apptStart = try {
            LocalDateTime.parse(
                "${appointment.date} ${appointment.startTime}",
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
            )
        } catch (e: Exception) {
            Log.e("SendNotification", "Błąd parsowania daty wizyty: ${e.message}", e)
            setMessages("Powiadomienie nie wysłane — błąd daty wizyty.")
            return
        }
        val days = ChronoUnit.DAYS.between(LocalDate.now(), apptStart.toLocalDate())
        if (days != 0L && days != 1L) {
            setMessages("Powiadomienie SMS działa tylko dla wizyt na dziś lub jutro.")
            return
        }
        val (start, end) = try {
            LocalTime.parse(profile.notificationSendStartTime.trim(), DateTimeFormatter.ofPattern("HH:mm")) to
                LocalTime.parse(profile.notificationSendEndTime.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: DateTimeParseException) {
            setMessages("Nieprawidłowy format godziny wysyłki w ustawieniach.")
            Log.e("SendNotification", "Invalid time format: ${e.message}", e)
            return
        }
        if (!isNotificationSendWindow(LocalTime.now(), start, end)) {
            setMessages("Poza oknem godzin wysyłki — zmień ustawienia lub spróbuj później.")
        } else {
            setMessages("Nie wysłano SMS (sprawdź numer telefonu lub sieć).")
        }
    }

    private fun setCustomersList(customers: List<Customer>) {
        customersList.value = customers
    }

    private fun setAppointmentsList(appointments: List<Appointment>) {
        appointmentsList.value = appointments
    }

    private fun buildSchedulingProbeOrNull(): Appointment? {
        val date = selectedAppointmentDate.value?.takeIf { it.isNotBlank() } ?: return null
        val emp = selectedEmployee.value ?: return null
        val startStr = selectedAppointmentStartTime.value?.takeIf { it.isNotBlank() } ?: return null
        val endStr = selectedAppointmentEndTime.value?.takeIf { it.isNotBlank() } ?: return null
        val start = parseAppointmentTimeString(startStr)?.format(timeFormatter) ?: return null
        val end = parseAppointmentTimeString(endStr)?.format(timeFormatter) ?: return null
        return Appointment(
            date = date,
            startTime = start,
            endTime = end,
            employeeId = emp.id ?: 0,
            employee = emp,
        )
    }

    private fun overlapNoticeMessageOrNull(): String? {
        val probe = buildSchedulingProbeOrNull() ?: return null
        val all = currentBaseAppointmentsList.value ?: return null
        val excludeId =
            if (isNewAppointment.value == true) null else selectedAppointment.value?.id
        val conflict = probe.findFirstSchedulingConflict(all, excludeId) ?: return null
        return formatSchedulingConflictMessage(probe, conflict)
    }

    private fun workHoursOutsideNoticeMessageOrNull(): String? {
        val emp = selectedEmployee.value ?: return null
        val (workStart, workEnd) = effectiveEmployeeWorkBounds(emp)
        if (!workEnd.isAfter(workStart)) return null

        val startStr = selectedAppointmentStartTime.value ?: return null
        val endStr = selectedAppointmentEndTime.value ?: return null
        val apptStart = parseAppointmentTimeString(startStr) ?: return null
        val apptEnd = parseAppointmentTimeString(endStr) ?: return null
        if (!apptEnd.isAfter(apptStart)) return null

        if (apptStart.isBefore(workStart) || apptEnd.isAfter(workEnd)) {
            return "Wizyta jest poza godzinami pracy pracownika (${workStart.format(timeFormatter)}–${workEnd.format(timeFormatter)}). Możesz zapisać mimo to."
        }
        return null
    }

    private fun buildScheduleInformationNoticeOrNull(): String? {
        val parts = listOfNotNull(
            workHoursOutsideNoticeMessageOrNull(),
            overlapNoticeMessageOrNull(),
        )
        if (parts.isEmpty()) return null
        return parts.joinToString("\n")
    }

    fun checkAppointmentsList() {
        appointmentScheduleNotice.value = buildScheduleInformationNoticeOrNull().orEmpty()
        val vacation = vacationViolationMessageOrNull()
        // #region agent log
        AgentDebugLog.log(
            hypothesisId = "H3",
            location = "ScheduleModelView.checkAppointmentsList",
            message = "conflict_probe",
            data = mapOf(
                "hasVacationBlock" to (vacation != null).toString(),
                "noticeLen" to (appointmentScheduleNotice.value?.length ?: 0).toString(),
                "start" to (selectedAppointmentStartTime.value ?: ""),
                "end" to (selectedAppointmentEndTime.value ?: ""),
                "baseListSize" to (currentBaseAppointmentsList.value?.size ?: -1).toString(),
            ),
        )
        // #endregion
        if (vacation != null) {
            appointmentError.value = vacation
            return
        }
        if ((buildSchedulingProbeOrNull() != null || workHoursProbeParsable()) &&
            appointmentError.value != "Niepoprawny format godziny"
        ) {
            appointmentError.value = ""
        }
    }

    private fun parseEmployeeWorkTime(value: String?): LocalTime? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalTime.parse(value.trim(), timeFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun effectiveEmployeeWorkBounds(emp: Employee): Pair<LocalTime, LocalTime> {
        val start = parseEmployeeWorkTime(emp.workStartTime)
            ?: LocalTime.parse(Employee.DEFAULT_WORK_START, timeFormatter)
        val end = parseEmployeeWorkTime(emp.workEndTime)
            ?: LocalTime.parse(Employee.DEFAULT_WORK_END, timeFormatter)
        return start to end
    }

    /** Gdy start/koniec wizyty da się sparsować — do czyszczenia błędu po poprawnym oknie pracy. */
    private fun workHoursProbeParsable(): Boolean {
        val startStr = selectedAppointmentStartTime.value ?: return false
        val endStr = selectedAppointmentEndTime.value ?: return false
        return parseAppointmentTimeString(startStr) != null &&
            parseAppointmentTimeString(endStr) != null
    }

    private fun vacationViolationMessageOrNull(): String? {
        val emp = selectedEmployee.value ?: return null
        val date = selectedAppointmentDate.value?.takeIf { it.isNotBlank() } ?: return null
        if (isNewAppointment.value != true) {
            val existing = selectedAppointment.value
            val sameSlot =
                existing != null &&
                    existing.id != null && existing.id != 0 &&
                    existing.date == date &&
                    existing.effectiveEmployeeId() == emp.id
            if (sameSlot) return null
        }
        if (!emp.isOnVacationOn(date)) return null
        val label = emp.vacationRangeLabel().ifBlank { date }
        return "${emp.displayName} ma urlop w tym dniu ($label). Wybierz innego pracownika lub zmień datę."
    }

    /** Gdy wybrany pracownik ma urlop w aktualnie wybranym dniu harmonogramu — pierwszy dostępny. */
    fun ensureSelectedEmployeeAvailableForCurrentDate() {
        val date = selectedAppointmentDate.value?.takeIf { it.isNotBlank() } ?: return
        val list = employeeList.value ?: return
        val current = selectedEmployee.value
        if (current != null && !current.isOnVacationOn(date)) return
        val fallback = list.firstOrNull { !it.isOnVacationOn(date) }
        if (fallback != null) {
            selectedEmployee.value = fallback
        }
    }

    fun getsAppoiments() {
        // Sprawdzamy, czy lista wizyt nie jest pusta oraz czy wybrany pracownik i data są prawidłowe
        if (currentBaseAppointmentsList.value?.isNotEmpty() == true && selectedAppointmentDate.value != null && selectedEmployee.value?.id != null) {

            val filteredAppointments = currentBaseAppointmentsList.value?.filter { appointment ->
                // Filtrujemy wizyty na podstawie daty i pracownika
                appointment.date == selectedAppointmentDate.value &&
                    appointment.effectiveEmployeeId() == selectedEmployee.value!!.id
            }

            // Przypisujemy przefiltrowane wyniki, jeśli są
            appointmentsList.value = filteredAppointments ?: emptyList()
        } else {
            // Jeśli dane są niewłaściwe (np. brak daty lub pracownika), przypisujemy pustą listę
            appointmentsList.value = emptyList()
        }
        // #region agent log
        val base = currentBaseAppointmentsList.value
        AgentDebugLog.log(
            hypothesisId = "H-E",
            location = "ScheduleModelView.getsAppoiments",
            message = "filter_applied",
            data = mapOf(
                "baseSize" to (base?.size ?: 0).toString(),
                "filteredSize" to (appointmentsList.value?.size ?: 0).toString(),
                "selectedDate" to (selectedAppointmentDate.value ?: ""),
                "workerId" to (selectedEmployee.value?.id?.toString() ?: "null"),
            ),
        )
        // #endregion
    }


}

