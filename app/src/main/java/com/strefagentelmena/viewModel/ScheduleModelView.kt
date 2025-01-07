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
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ScheduleModelView : ViewModel() {
    val customersList = MutableLiveData<List<Customer>>()
    val appointmentsList = MutableLiveData<List<Appointment>>()
    private val profilePreferences = MutableLiveData<ProfilePreferences>()
    val selectedEmployee = MutableLiveData<Employee?>()
    val employeeList = MutableLiveData<List<Employee>>()
    private val currentBaseAppointmentsList = MutableLiveData<List<Appointment>>()

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val currentDate: String = LocalDate.now().format(dateFormatter)
    private val currentTime: String = LocalTime.now().format(timeFormatter)

    val selectedClient = MutableLiveData<Customer>()
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


    fun setNewTime(time: String) {
        try {
            LocalTime.parse(time, timeFormatter)
            selectedAppointmentStartTime.value = time
        } catch (e: Exception) {
            appointmentError.value = "Niepoprawny format godziny"
        }
    }

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

    fun changeNotificationDialogState() {
        onNotificationClickState.value = !onNotificationClickState.value!!
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
        selectedAppointmentStartTime.value = appointment.startTime.format(timeFormatter)
        selectedAppointmentDate.value = appointment.date.format(dateFormatter)

        setSelectedClient(appointment.customer)
    }

    /**
     * Show Apoiment Dialog.
     *
     */
    fun changeAppointmentDialogState() {
        appointmentDialog.value = !appointmentDialog.value!!
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
    ) {
        val id = if (isNew) appointmentsList.value?.size else selectedAppointment.value?.id!!

        // Formatuj czasy jako String przed przekazaniem do konstruktora Appointment
        val startTime = selectedAppointmentStartTime.value?.let {
            LocalTime.parse(it, timeFormatter).format(timeFormatter)
        } ?: return

        val endTime = selectedAppointmentEndTime.value?.let {
            LocalTime.parse(it, timeFormatter).format(timeFormatter)
        } ?: return

        val new = Appointment(
            id = id ?: (appointmentsList.value!!.size + 1),
            customer = selectedClient.value ?: Customer(),
            date = selectedAppointmentDate.value ?: return,
            startTime = startTime, // Przechowywany jako String
            notificationSent = false,
            endTime = endTime, // Przechowywany jako String
            employee = selectedEmployee.value ?: return,
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

                    val appointmentWithId = newAppointment.copy(id = nextAppointmentId)

                    // Dodaj wizytę do lokalnej listy
                    currentAppointments.add(appointmentWithId)

                    // Ustaw zaktualizowaną listę
                    setAppointmentsList(currentAppointments)
                    currentBaseAppointmentsList.value = currentAppointments

                    // Dodaj wizytę do Firebase
                    FirebaseFunctionsAppointments().addNewAppointmentToFirebase(
                        firebaseDatabase, appointmentWithId
                    ) { success ->
                        if (success) {
                            setMessages("${appointmentWithId.customer.fullName} już niedługo na twoim fotelu")
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
    fun removeAppointment(id: Int) {
        val currentAppointments = currentBaseAppointmentsList.value?.toMutableList() ?: return
        currentAppointments.removeAll { it.id == id }

        setAppointmentsList(currentAppointments)
        currentBaseAppointmentsList.value = currentAppointments

        FirebaseFunctionsAppointments().deleteAppointmentFromFirebase(FirebaseDatabase.getInstance(),
            selectedAppointment.value?.id ?: return,
            completion = { success ->
                if (success) {
                    setMessages("Wizyta została usunięta")
                } else {
                    setMessages("Błąd usuwania wizyty")
                }

            })
        setMessages("Jedna wizyta mniej do zrobienia")
    }

    /**
     * Edit Appointment.
     *
     */
    fun editAppointment(
        firebaseDatabase: FirebaseDatabase, notificationIsSent: Boolean = false
    ) {
        // Pobieranie danych
        val appointmentsList = currentBaseAppointmentsList.value?.toMutableList() ?: return
        val selectedClient = getSelectedClient() ?: return logError("Selected client is null")
        val customers = customersList.value?.toMutableList() ?: return

        // Znalezienie wizyty i klienta
        val appointmentIndex =
            appointmentsList.indexOfFirst { it.id == selectedAppointment.value?.id }
        val clientIndex = customers.indexOf(selectedClient)

        // Weryfikacja indeksów
        if (appointmentIndex == -1 || clientIndex == -1) return logError("Appointment or client not found.")

        // Aktualizacja notatki
        selectedClient.noted = selectedAppointmentNote.value.orEmpty()


        onNotificationClickState.value = notificationIsSent

        // Walidacja czasu i daty
        val startTime =
            validateAndParseTime(selectedAppointmentStartTime.value, "Start time") ?: return
        val endTime = validateAndParseTime(selectedAppointmentEndTime.value, "End time") ?: return
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val appointmentDate = selectedAppointmentDate.value?.takeIf { it.isNotEmpty() }
            ?: return logError("Invalid appointment date")
        if (LocalDate.parse(appointmentDate, dateFormatter)
                .isBefore(LocalDate.now())
        ) return logError("Appointment date is in the past.")

        // Tworzenie nowego obiektu wizyty
        val updatedAppointment = selectedAppointment.value?.copy(
            notificationSent = onNotificationClickState.value ?: notificationIsSent,
            date = appointmentDate,
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            customer = selectedClient,
            employee = selectedEmployee.value ?: Employee()
        ) ?: return logError("Selected appointment is null")

        // Aktualizacja listy spotkań i klientów
        appointmentsList[appointmentIndex] = updatedAppointment
        customers[clientIndex] = selectedClient.copy(appointment = updatedAppointment)

        // Aktualizacja stanów
        updateState(appointmentsList, customers)

        // Zaktualizowanie Firebase
        try {
            saveAppointmentToFirebase(firebaseDatabase, updatedAppointment)
            Log.d(
                "EditAppointment",
                "Appointment updated successfully with ID: ${updatedAppointment.id}"
            )
        } catch (e: Exception) {
            return logError("Error saving appointment to Firebase: ${e.message}")
        }

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
        setAppointmentsList(getAppointmentsForSelectedWorker(appointmentsList))
    }


    // Funkcja walidująca i parsująca czas
    private fun validateAndParseTime(time: String?, fieldName: String): LocalTime? {
        if (time.isNullOrEmpty()) {
            Log.e("EditAppointment", "$fieldName is missing.")
            return null
        }
        return try {
            LocalTime.parse(time, timeFormatter)
        } catch (e: DateTimeParseException) {
            Log.e("EditAppointment", "Invalid $fieldName format: $time")
            null
        }
    }

    // Funkcja zapisująca dane do Firebase
    private fun saveAppointmentToFirebase(
        firebaseDatabase: FirebaseDatabase, updatedAppointment: Appointment
    ) {
        FirebaseFunctionsAppointments().editAppointmentInFirebase(
            firebaseDatabase, updatedAppointment
        ) { success ->
            if (success) {
                setMessages("Wizyta ${updatedAppointment.customer.fullName} została zaktualizowana.")
            } else {
                setMessages("Błąd edycji wizyty.")
            }
        }
    }


    fun loadAllData() {
        viewModelScope.launch {

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
                setCustomersList(customersDeferred.await() ?: emptyList())

                val employee = employeeDeferred.await()
                if (employee != null) {
                    selectedEmployee.value = employee
                } else {
                    Log.w("LoadAllData", "Pracownik nie został załadowany")
                }

                currentBaseAppointmentsList.value = appointmentsDeferred.await() ?: emptyList()
                employeeList.value = employeeListDeferred.await() ?: emptyList()
                profilePreferences.value = profileDeferred.await() ?: ProfilePreferences()

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
            smsManager.sendNotification(appointment, profile)

        } catch (e: Exception) {
            setMessages("Błąd podczas wysyłania powiadomienia: ${e.message}")
            Log.e("SendNotification", "Error sending notification: ${e.message}", e)
            return
        }

        if (notificationSent) {
            setMessages("Powiadomienie wysłane do ${appointment.customer.fullName}")
            editAppointment(FirebaseDatabase.getInstance(), true)
        } else {
            // Trim i parsowanie godziny
            val endTimeString = profile.notificationSendEndTime.trim()
            val endTime = try {
                LocalTime.parse(endTimeString, DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: DateTimeParseException) {
                setMessages("Nieprawidłowy format godziny w ustawieniach: '$endTimeString'")
                Log.e("SendNotification", "Invalid time format: ${e.message}", e)
                return
            }

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
        // Sprawdzamy, czy lista wizyt nie jest pusta oraz czy wybrany pracownik i data są prawidłowe
        if (currentBaseAppointmentsList.value?.isNotEmpty() == true && selectedAppointmentDate.value != null && selectedEmployee.value?.id != null) {

            val filteredAppointments = currentBaseAppointmentsList.value?.filter { appointment ->
                // Filtrujemy wizyty na podstawie daty i pracownika
                appointment.date == selectedAppointmentDate.value && appointment.employee.id == selectedEmployee.value!!.id
            }

            // Przypisujemy przefiltrowane wyniki, jeśli są
            appointmentsList.value = filteredAppointments ?: emptyList()
        } else {
            // Jeśli dane są niewłaściwe (np. brak daty lub pracownika), przypisujemy pustą listę
            appointmentsList.value = emptyList()
        }
    }


}

