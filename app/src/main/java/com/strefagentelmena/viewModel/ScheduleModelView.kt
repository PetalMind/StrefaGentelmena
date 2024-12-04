package com.strefagentelmena.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fireBase.FirebaseEmployeeFunctions
import com.strefagentelmena.functions.fireBase.FirebaseFunctionsAppointments
import com.strefagentelmena.functions.fireBase.FirebaseProfilePreferences
import com.strefagentelmena.functions.fireBase.getAllCustomersFromFirebase
import com.strefagentelmena.functions.smsManager
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.CustomerIdGenerator
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ScheduleModelView : ViewModel() {
    val customersList = MutableLiveData<List<Customer>>(emptyList())
    val appointmentsList = MutableLiveData<List<Appointment>?>(emptyList())
    private val profilePreferences = MutableLiveData<ProfilePreferences>(ProfilePreferences())
    val selectedEmployee = MutableLiveData<Employee>(Employee())
    val employeeList = MutableLiveData<List<Employee>>(emptyList())
    val currentBaseAppointmentsList = MutableLiveData<List<Appointment>>(emptyList())

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val currentDate: String = LocalDate.now().format(dateFormatter)
    val currentTime: String = LocalTime.now().format(timeFormatter)

    val selectedClient = MutableLiveData<Customer>(Customer())
    val isNewAppointment = MutableLiveData<Boolean>(false)

    val messages = MutableLiveData<String?>("")
    val viewState = MutableLiveData<AppState>(AppState.Idle)

    /**
     *Dialogs states
     */
    val appointmentDialog = MutableLiveData(false)
    val deleteDialogState = MutableLiveData(false)
    val onNotificationClickState = MutableLiveData(false)
    val appointmentError = MutableLiveData("")
    val shouldOpenDialog = MutableLiveData(false)

    /**
     * Appoiments Data
     *
     */
    val selectedAppointmentDate = MutableLiveData(currentDate)
    val selectedAppointmentStartTime = MutableLiveData(currentTime)
    val selectedAppointment = MutableLiveData(Appointment())
    val selectedAppointmentEndTime =
        MutableLiveData(LocalTime.now().plusHours(1).format(timeFormatter))
    val selectedAppointmentNote = MutableLiveData("")

    fun setAppoimentNote(newValue: String) {
        selectedAppointmentNote.value = newValue
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

    fun setEmpolyee(newValue: Employee) {
        selectedEmployee.value = newValue
    }

    fun showNotificationState() {
        onNotificationClickState.value = true
    }

    fun hideNotificationState() {
        onNotificationClickState.value = false
    }


    fun clearMessages() {
        messages.value = null
    }

    fun setMessages(newValue: String) {
        messages.value = newValue
    }

    fun setSelectedClient(newValue: Customer) {
        selectedClient.value = newValue
    }

    fun setNewAppoimentsDate(newValue: LocalDate) {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val formattedDate = newValue.format(formatter)
        selectedAppointmentDate.value = formattedDate
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
        selectedClient.value = Customer()
        selectedAppointment.value = null
    }

    fun setNewDataAppointment(newValue: String) {
        val formattedDate = LocalDate.parse(newValue, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        selectedAppointmentDate.value = formattedDate
    }

    fun setNewTime(newValue: String) {
        val formattedTime = LocalTime.parse(newValue, DateTimeFormatter.ofPattern("HH:mm"))
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        selectedAppointmentStartTime.value = formattedTime
    }


    fun showDeleteDialog() {
        deleteDialogState.value = true
    }

    /**
     * Hide Delete Dialog.
     *
     */
    fun hideDeleteDialog() {
        deleteDialogState.value = false
    }

    /**
     * Select Appointment.
     *
     * @param appointment
     */
    fun selectAppointmentAndClient(appointment: Appointment) {
        selectedAppointment.value = appointment
        selectedAppointmentStartTime.value = appointment.startTime.format(timeFormatter)
        selectedAppointmentDate.value = appointment.date.format(dateFormatter)

        setSelectedClient(appointment.customer)
    }

    /**
     * Show Apoiment Dialog.
     *
     */
    fun showApoimentDialog() {
        appointmentDialog.value = true
    }

    /**
     * Hide Apoiment Dialog.
     *
     */
    fun hideApoimentDialog() {
        appointmentDialog.value = false
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

    fun findWorkerByName(name: String): Employee? {
        return employeeList.value?.firstOrNull { it.name == name }
    }

    fun prepareAppointmentDetails() {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        if (isNewAppointment.value == true) {
            val currentTime = LocalTime.now()
            clearDate()
            setNewTime(currentTime.format(timeFormatter)) // `setNewTime` oczekuje String, tutaj jest OK
            setAppointmentEndTime(currentTime.plusHours(1)) // Przekazujemy LocalTime
        } else {
            val endTime = selectedAppointment.value?.endTime?.let {
                try {
                    LocalTime.parse(it, timeFormatter)
                } catch (e: Exception) {
                    null // Wartość null, jeśli parsowanie się nie powiedzie
                }
            } ?: LocalTime.now().plusHours(1) // Domyślny czas

            setAppointmentEndTime(endTime) // Przekazujemy LocalTime
        }
    }


    fun createNewAppointment(
        isNew: Boolean,
        context: Context,
    ) {
        val id = if (isNew) CustomerIdGenerator().generateId() else selectedAppointment.value?.id!!

        // Formatuj czasy jako String przed przekazaniem do konstruktora Appointment
        val startTime = selectedAppointmentStartTime.value?.let {
            LocalTime.parse(it, timeFormatter).format(timeFormatter)
        } ?: return

        val endTime = selectedAppointmentEndTime.value?.let {
            LocalTime.parse(it, timeFormatter).format(timeFormatter)
        } ?: return

        val new = Appointment(
            id = id,
            customer = selectedClient.value ?: Customer(),
            date = selectedAppointmentDate.value ?: return,
            startTime = startTime, // Przechowywany jako String
            notificationSent = false,
            endTime = endTime, // Przechowywany jako String
            employee = selectedEmployee.value ?: return,
        )

        selectedAppointment.value = new

        addAppointment(firebaseDatabase = FirebaseDatabase.getInstance(), context = context)
        hideApoimentDialog()
    }


    private suspend fun loadWorkersFromFireBase(): Employee? {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        return FirebaseEmployeeFunctions().loadEmployeesFromFirebase(firebaseDatabase)
            .firstOrNull() // Zwracamy pierwszy pracownik lub null
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
     * @param context
     */
    private fun addAppointment(context: Context, firebaseDatabase: FirebaseDatabase) {
        val currentAppointments =
            currentBaseAppointmentsList.value?.toMutableList() ?: mutableListOf()
        val newAppointment = selectedAppointment.value ?: return

        // Dodajemy appointment do lokalnej listy
        currentAppointments.add(newAppointment)

        // Ustawiamy zaktualizowaną listę
        setAppointmentsList(currentAppointments)
        currentBaseAppointmentsList.value = currentAppointments

        // Dodajemy appointment do Firebase
        FirebaseFunctionsAppointments().addNewAppointmentToFirebase(
            firebaseDatabase, newAppointment
        ) { success ->
            if (success) {
                setMessages("${newAppointment.customer.fullName} już niedługo na twoim fotelu")
                Log.e(
                    "Log", "Appointment added successfully for ${newAppointment.customer.fullName}"
                )
            } else {
                setMessages("Błąd dodawania wizyty")
                Log.e("Log", "Error adding appointment")
            }
        }
    }


    private fun getAppointmentsForSelectedWorker(appointments: List<Appointment>): List<Appointment> {
        if (selectedEmployee.value == null) return emptyList()

        val selectedWorkerName =
            selectedEmployee.value?.id ?: return emptyList() // Handle null case
        return appointments.filter { it.employee.id == selectedWorkerName }
    }

    private fun getSelectedClient(): Customer? {
        val clientName =
            selectedClient.value?.fullName ?: selectedAppointment.value?.customer?.fullName
        return clientName?.let { findCustomerByName(it) }
    }

    /**
     * Remove Appointment.
     *
     * @param id
     */
    fun removeAppointment(id: Int, context: Context) {
        val currentAppointments = currentBaseAppointmentsList.value?.toMutableList() ?: return
        currentAppointments.removeAll { it.id == id }

        setAppointmentsList(currentAppointments)
        currentBaseAppointmentsList.value = currentAppointments


        setMessages("Jedna wizyta mniej do zrobienia")
    }

    /**
     * Edit Appointment.
     *
     */
    fun editAppointment(
        firebaseDatabase: FirebaseDatabase, notificationIsSent: Boolean = false
    ) {
        val appointmentsList = currentBaseAppointmentsList.value?.toMutableList() ?: return
        val index = appointmentsList.indexOfFirst { it.id == selectedAppointment.value?.id }
        val selectedClient = getSelectedClient() ?: return
        val clientIndex = customersList.value?.indexOf(selectedClient) ?: return

        if (index == -1 || clientIndex == -1) return

        selectedClient.noted = selectedAppointmentNote.value ?: ""

        try {
            val startTimeString = selectedAppointmentStartTime.value
            val endTimeString = selectedAppointmentEndTime.value

            // Upewniamy się, że pola czasowe nie są null ani puste
            if (startTimeString.isNullOrEmpty() || endTimeString.isNullOrEmpty()) {
                Log.e("EditAppointment", "Start time or end time is missing.")
                return
            }

            val startTime = try {
                LocalTime.parse(startTimeString, timeFormatter)
            } catch (e: DateTimeParseException) {
                Log.e("EditAppointment", "Invalid start time format: $startTimeString")
                return
            }

            val endTime = try {
                LocalTime.parse(endTimeString, timeFormatter)
            } catch (e: DateTimeParseException) {
                Log.e("EditAppointment", "Invalid end time format: $endTimeString")
                return
            }

            // Tworzenie nowego obiektu wizyty
            val updatedAppointment = selectedAppointment.value?.copy(
                notificationSent = onNotificationClickState.value ?: notificationIsSent,
                date = selectedAppointmentDate.value ?: "",
                startTime = startTime.toString(),
                endTime = endTime.toString(),
                customer = selectedClient,
                employee = selectedEmployee.value ?: Employee()
            ) ?: return

            // Aktualizacja listy wizyt i klienta
            appointmentsList[index] = updatedAppointment
            (customersList.value as MutableList<Customer>?)?.set(
                clientIndex, selectedClient.copy(appointment = updatedAppointment)
            )

            // Zaktualizuj dane bazowe i listę wizyt
            currentBaseAppointmentsList.value = appointmentsList
            setAppointmentsList(getAppointmentsForSelectedWorker(appointmentsList))

            // Zapis danych do Firebase
            FirebaseFunctionsAppointments().editAppointmentInFirebase(
                firebaseDatabase, updatedAppointment
            ) { success ->
                if (success) {
                    setMessages("Wizyta ${updatedAppointment.customer.fullName} została zaktualizowana.")
                } else {
                    setMessages("Błąd edycji wizyty.")
                }
            }

            // Zapisz zmiany w plikach (jeśli to potrzebne)
            // fileFunctionsClients.saveCustomersToFile(context, customersList.value ?: return)
            // fileFunctionsAppoiments.saveAppointmentToFile(context, appointmentsList)
        } catch (e: Exception) {
            Log.e("EditAppointment", "Unexpected error: ${e.message}")
        }
    }


    fun loadAllData() {
        viewModelScope.launch {
            try {
                val customersDeferred =
                    async(Dispatchers.IO) { getAllCustomersFromFirebase(FirebaseDatabase.getInstance()) }
                val appointmentsDeferred = async(Dispatchers.IO) {
                    FirebaseFunctionsAppointments().loadAppointmentsFromFirebase(FirebaseDatabase.getInstance())
                }
                val employeeDeferred: Deferred<Employee?> = async(Dispatchers.IO) {
                    loadWorkersFromFireBase()
                }

                profilePreferences.value =
                    FirebaseProfilePreferences().loadProfilePreferencesFromFirebase(firebaseDatabase = FirebaseDatabase.getInstance())

                // Pobieranie danych
                setCustomersList(customersDeferred.await())

                val appointments = appointmentsDeferred.await()
                currentBaseAppointmentsList.value = appointments
                setAppointmentsList(appointments)
                getsAppoiments()

                // Ustawienie wybranego pracownika
                selectedEmployee.value = employeeDeferred.await()

                viewState.value = AppState.Success
            } catch (e: Exception) {
                Log.e("LoadAllData", "Error loading data: ${e.message}", e)
                viewState.value = AppState.Error
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
            smsManager.sendNotification(appointment, profile)
        } catch (e: Exception) {
            setMessages("Błąd podczas wysyłania powiadomienia: ${e.message}")
            return
        }

        if (notificationSent) {
            setMessages("Powiadomienie wysłane do ${appointment.customer.fullName}")
            editAppointment(FirebaseDatabase.getInstance(), true)
        } else {
            val endTime = LocalTime.parse(
                profile.notificationSendEndTime, DateTimeFormatter.ofPattern("HH:mm")
            )
            if (LocalTime.now().isAfter(endTime)) {
                setMessages("Powiadomienie nie może być wysłane, zmień godziny wysyłki w ustawieniach")
            } else {
                setMessages("Powiadomienie nie wysłane do ${appointment.customer.fullName}")
            }
        }
    }

    private fun setCustomersList(customers: List<Customer>) {
        customersList.value = customers
    }

    private fun setAppointmentsList(appointments: List<Appointment>) {
        appointmentsList.value = appointments
    }

    fun checkAppointmentsList() {
        if (appointmentsList.value?.isNotEmpty() == true) {
            val currentDateAppointments = appointmentsList.value?.filter {
                it.date == selectedAppointmentDate.value
            }

            if (currentDateAppointments?.isNotEmpty() == true) {
                val currentTimeAppointments = currentDateAppointments.filter {
                    it.startTime == selectedAppointmentStartTime.value // Porównanie String
                }

                // Możesz dodać logikę lub obsługę wyników, jeśli znaleziono pasujące terminy
            }
        }
    }

    fun getsAppoiments() {
        if (appointmentsList.value?.isNotEmpty() == true) {
            val filteredAppointments = appointmentsList.value?.filter { appointment ->
                appointment.date == selectedAppointmentDate.value && appointment.employee.id == selectedEmployee.value!!.id
            }

            // Przypisz przefiltrowane wyniki do nowej zmiennej lub listy, np.:
            appointmentsList.value = filteredAppointments
        }
    }

}

