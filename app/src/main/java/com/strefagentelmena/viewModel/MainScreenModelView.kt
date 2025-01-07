package com.strefagentelmena.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.Greetings
import com.strefagentelmena.functions.fireBase.FirebaseFunctionsAppointments
import com.strefagentelmena.functions.fireBase.FirebaseProfilePreferences
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainScreenModelView : ViewModel() {
    val messages = MutableLiveData("")
    val viewState = MutableLiveData(AppState.Idle)
    val customersLists = MutableLiveData<List<Customer>>(emptyList())
    private val appointmentsLists = MutableLiveData<List<Appointment>>(listOf(Appointment()))
    val appointmentsToNotify = MutableLiveData<List<Appointment>>(emptyList())
    val showNotifyDialog = MutableLiveData(false)
    val upcomingAppointment: MutableLiveData<Appointment> = MutableLiveData(Appointment())
    val profilePreferences = MutableLiveData(ProfilePreferences())
    val dataLoaded = MutableLiveData(false)
    val displayGreetings: MutableLiveData<String> = MutableLiveData(
        Greetings().getSeasonalAndPartOfDayGreeting(
            profilePreferences.value?.userName ?: "Użytkownik"
        )
    )
    private val _deferNotificationUntil = MutableLiveData<LocalDateTime?>()

    fun deferNotifyDialog(minutes: Long) {
        _deferNotificationUntil.value = LocalDateTime.now().plusMinutes(minutes)
    }

    fun shouldShowNotifyDialog(): Boolean {
        val deferUntil = _deferNotificationUntil.value
        return deferUntil == null || LocalDateTime.now().isAfter(deferUntil)
    }

    fun setViewState(viewState: AppState) {
        this.viewState.value = viewState
    }

    fun newMessage(message: String) {
        messages.value = message
    }

    fun clearMessage() {
        messages.value = ""
    }

    fun setViewNotifyDialog() {
        showNotifyDialog.value = !showNotifyDialog.value!!

    }

    private fun setAppointmentsList(value: List<Appointment>) {
        appointmentsLists.value = value
    }

    private fun setCustomersList(customerList: List<Customer>) {
        customersLists.value = customerList
    }

    private fun findCustomerByName(name: String): Customer? {
        return customersLists.value?.firstOrNull { it.fullName == name }
    }

    private fun setUpcomingAppointment(appointment: Appointment) {
        upcomingAppointment.value = appointment
    }

    private fun setAppointmentsToNotify(emptyList: List<Appointment>) {
        appointmentsToNotify.value = emptyList
    }

    private fun setGreetingRandom(greeting: String) {
        viewModelScope.launch(Dispatchers.Main) {
            displayGreetings.value = greeting
        }
    }

    fun setDataLoaded(dataLoadedValue: Boolean) {
        dataLoaded.value = dataLoadedValue
    }


    /*
        private fun addFirstEmployee(context: Context) {
         //   val employees = fileFunctionsSettings.loadSettingsFromFile(context)

            val newEmployeeList = employees.employeeMutableList.toMutableList()
            if (employees.employeeMutableList.isEmpty()) {
                newEmployeeList.add(Employee(1, "Kinga", "Kloss"))
                profilePreferences.value?.employeeMutableList = newEmployeeList

                profilePreferences.value?.let {
                    fileFunctionsSettings.saveSettingsToFile(
                        context,
                        preferences = it
                    )
                }

            }

        }

    */


    /**
     * load Customer from firebase
     * @param database FirebaseDatabase
     * @return List of customers
     */
    private suspend fun loadCustomersList(database: FirebaseDatabase): List<Customer> {
        return suspendCoroutine { continuation ->
            val customersRef = database.getReference("Customers")
            customersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val customers =
                        snapshot.children.mapNotNull { it.getValue(Customer::class.java) }
                    continuation.resume(customers) // Wznawia coroutine z wynikiem
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException()) // Wznawia coroutine z wyjątkiem
                }
            })
        }
    }


    private suspend fun loadApointmentsList(): List<Appointment> {
        // Zwróć załadowanych klientów
        return FirebaseFunctionsAppointments().loadAppointmentsFromFirebase(
            FirebaseDatabase.getInstance()
        )
    }


    private fun sendNotificationsForUpcomingAppointments() {
        val currentTime = LocalDateTime.now()
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val nextDay = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        val appointmentsToSend = appointmentsLists.value
            ?.filter { appointment ->
                !appointment.notificationSent && shouldSendNotificationSafe(
                    appointment,
                    today,
                    nextDay,
                    currentTime
                )
            }
            .orEmpty() // Gwarantujemy, że wynik nie będzie `null`

        if (appointmentsToSend.isNotEmpty()) {
            // Ustawienie listy spotkań do powiadomienia
            setAppointmentsToNotify(appointmentsToSend)

            messages.value = "Znaleziono ${appointmentsToSend.size} spotkań do powiadomienia"
            // Opcjonalnie: komunikat o znalezionych spotkaniach
            Log.d("Notifications", "Znaleziono ${appointmentsToSend.size} spotkań do powiadomienia")
        }
    }

    private fun shouldSendNotificationSafe(
        appointment: Appointment,
        today: String,
        nextDay: String,
        currentTime: LocalDateTime
    ): Boolean {
        return try {
            shouldSendNotificationForAppointment(appointment, today, nextDay, currentTime)
        } catch (e: Exception) {
            Log.e(
                "Notification Error",
                "Error parsing appointment: ${appointment.date} ${appointment.startTime}",
                e
            )
            messages.value = "Błąd parsowania daty spotkania: ${appointment.date}"
            false
        }
    }


    private fun shouldSendNotificationForAppointment(
        appointment: Appointment,
        today: String,
        nextDay: String,
        currentTime: LocalDateTime
    ): Boolean {
        if (appointment.notificationSent) return false

        val appointmentDateTime = LocalDateTime.parse(
            "${appointment.date} ${appointment.startTime}",
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        )

        val isToday = appointment.date == today &&
                currentTime.toLocalTime().isBefore(LocalTime.parse(appointment.startTime))

        val isTomorrow = appointment.date == nextDay &&
                shouldSendNotification(
                    Period.between(
                        currentTime.toLocalDate(),
                        appointmentDateTime.toLocalDate()
                    ).days,
                    currentTime
                )

        return isToday || isTomorrow
    }


    private fun shouldSendNotification(daysDifference: Int, currentTime: LocalDateTime): Boolean {
        val startTime = profilePreferences.value?.notificationSendStartTime
        val endTime = profilePreferences.value?.notificationSendEndTime

        // Upewnij się, że wartości czasowe są dostępne
        if (startTime.isNullOrEmpty() || endTime.isNullOrEmpty()) {
            Log.e("Notification Error", "Notification time range is not set in preferences.")
            return false
        }

        return try {
            val startLocalTime = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            val endLocalTime = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))

            // Powiadomienie może być wysłane w dniu wizyty (daysDifference == 0) lub dzień przed (daysDifference == 1)
            (daysDifference == 1 || daysDifference == 0) &&
                    currentTime.toLocalTime().isAfter(startLocalTime) &&
                    currentTime.toLocalTime().isBefore(endLocalTime)
        } catch (e: Exception) {
            Log.e(
                "Notification Error",
                "Error parsing notification time range: $startTime - $endTime",
                e
            )
            false
        }
    }


    /**
     * Find nearest appointment today
     *
     * @param currentDateTime
     * @return
     */

    private fun findNearestAppointmentToday(
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): Appointment? {
        val oneHourLater = currentDateTime.plusHours(1)

        return try {
            appointmentsLists.value?.firstOrNull {
                val appointmentDateTime = LocalDateTime.parse(
                    "${it.date} ${it.startTime}",
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                )

                appointmentDateTime.isAfter(currentDateTime) &&
                        appointmentDateTime.isBefore(oneHourLater)
            } ?: run {
                // Poprawka: użyj postValue zamiast setValue
                viewModelScope.launch(Dispatchers.Main) {
                    setViewState(AppState.Error)
                }
                null
            }
        } catch (e: Exception) {
            Log.e("Appointment Error", "Error finding the nearest appointment: ${e.message}", e)

            // Upewnij się, że zmiana stanu odbywa się na głównym wątku
            viewModelScope.launch(Dispatchers.Main) {
                setViewState(AppState.Error)
            }
            null
        }
    }


    private fun loadProfilePreferencesFromFirebase(firebaseDatabase: FirebaseDatabase) {
        viewModelScope.launch {
            try {
                val profile =
                    FirebaseProfilePreferences().loadProfilePreferencesFromFirebase(firebaseDatabase)
                setProfilePreferences(profile)
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to load ProfilePreferences", e)
                viewState.value = AppState.Error
            }
        }
    }

    private fun setProfilePreferences(profile: ProfilePreferences) {
        profilePreferences.value = profile
    }

    fun startLoadingData() {
        setViewState(AppState.Loading)
        viewModelScope.launch {
            try {
                clearMessage()
                val database = FirebaseDatabase.getInstance()
                val customers = withContext(Dispatchers.IO) {
                    loadCustomersList(database)
                }

                loadProfilePreferencesFromFirebase(database)
                setCustomersList(customers)

                setViewState(AppState.Success)


                Log.i("Coroutine", "Loaded ${customers.size} customers.")
            } catch (e: Exception) {
                setViewState(AppState.Error)
                Log.e("Coroutine", "Error loading customers: ${e.message}", e)
            }
        }
    }

    fun checkAppointments() {
        viewModelScope.launch {
            try {
                // Pobranie listy spotkań w tle
                val appointments = withContext(Dispatchers.IO) {
                    loadApointmentsList()
                }

                // Aktualizacja listy spotkań na głównym wątku
                setAppointmentsList(appointments)

                // Wysłanie powiadomień o nadchodzących spotkaniach
                sendNotificationsForUpcomingAppointments()
            } catch (e: Exception) {
                // Obsługa błędów
                Log.e("CheckAppointments", "Error while checking appointments: ${e.message}", e)

                // Ustawienie stanu błędu w widoku
                setViewState(AppState.Error)
                messages.value = "Wystąpił błąd podczas sprawdzania spotkań."
            }
        }
    }


}
