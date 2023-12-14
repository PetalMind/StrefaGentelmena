package com.strefagentelmena.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fileFuctions.fileFunctionsClients
import com.strefagentelmena.functions.fileFuctions.fileFunctionsSettings
import com.strefagentelmena.functions.fileFuctions.filesFunctionsAppoiments
import com.strefagentelmena.functions.greetingsManager
import com.strefagentelmena.models.AppoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale

class MainScreenModelView : ViewModel() {
    val messages = MutableLiveData("")
    val viewState = MutableLiveData(AppState.Idle)
    val customersLists = MutableLiveData<List<Customer>>(emptyList())
    val appointmentsLists = MutableLiveData<List<Appointment>>(emptyList())
    val appointmentsToNotify = MutableLiveData<List<Appointment>>(emptyList())
    val showNotifyDialog = MutableLiveData(false)
    val upcomingAppointment: MutableLiveData<Appointment> = MutableLiveData()
    val profilePreferences = MutableLiveData(ProfilePreferences())
    val dataLoaded = MutableLiveData(false)

    val randomGreetings = MutableLiveData(
        greetingsManager.randomGreeting(profilePreferences.value?.userName ?: "")
    )

    val displayGreetings: MutableLiveData<String> = randomGreetings

//    fun createNotification(customerList: List<Appointment>, data: String) {
//        val notification = Notification(
//            id = (notificationList.value?.size ?: 0) + 1,
//            recipient = "Strefa Gentelmena",
//            message = "Wysłano do ${customerList.size} klientów powiadomień SMS",
//            timestamp = data,
//            notificationSent = true,
//        )
//
//        val updatedList = notificationList.value.orEmpty() + notification
//        notificationList.postValue(updatedList)
//    }

    fun loadData(context: Context) {
        dataLoaded.value = loadAllData(context)
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

    fun hideNotifyDialog() {
        showNotifyDialog.value = false
    }

    fun showNotifyDialog() {
        showNotifyDialog.value = true
    }

    fun loadAllData(context: Context): Boolean {
        return try {
            loadProfile(context)
            loadCustomersList(context)
            loadApointmentsList(context)
            findNearestAppointmentToday()
            true // All operations succeeded
        } catch (e: Exception) {
            setViewState(AppState.Error)
            false // An error occurred
        }
    }

    /**
     * Load profile
     *
     * @param context
     */
    private fun loadProfile(context: Context) {
        profilePreferences.value = fileFunctionsSettings.loadSettingsFromFile(context)

        randomGreetings.value =
            greetingsManager.randomGreeting(profilePreferences.value?.userName ?: "")
    }

    /**
     * Load customers list
     *
     * @param context
     * @return
     */
    private fun loadCustomersList(context: Context): List<Customer> {
        // Sprawdź, czy funkcja loadCustomersFromFile zwraca poprawny typ
        val loadedCustomers = fileFunctionsClients.loadCustomersFromFile(context)

        customersLists.value = loadedCustomers

        // Zwróć załadowanych klientów
        return loadedCustomers
    }

    private fun loadApointmentsList(context: Context): List<Appointment> {
        // Sprawdź, czy funkcja loadCustomersFromFile zwraca poprawny typ
        val loadedAppointments = filesFunctionsAppoiments.loadAppointmentFromFile(context)

        // Dodaj warunek, aby uniknąć zwracania pustej listy, jeśli coś poszło nie tak
        if (loadedAppointments.isNotEmpty()) {
            // Zaktualizuj customersLists.value, jeśli to jest wymagane
            appointmentsLists.value = loadedAppointments
        }

        // Zwróć załadowanych klientów
        return loadedAppointments
    }


    fun sendNotificationsForUpcomingAppointments(formattedDate: String) {
        val appointmentsToSend = mutableListOf<Appointment>()
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val currentTime = LocalDateTime.now()

        appointmentsLists.value?.let { appointments ->
            appointments.filterNot { it.notificationSent }.forEach { appointment ->
                val appointmentDateTime = LocalDateTime.parse(
                    "${appointment.date} ${appointment.startTime}",
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                )

                val startTimeString = profilePreferences.value?.notificationSendStartTime
                val endTimeString = profilePreferences.value?.notificationSendEndTime

                val startTime = try {
                    LocalTime.parse(startTimeString, DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: DateTimeParseException) {
                    newMessage("Błąd parsowania czasu początkowego: $e")
                    LocalTime.of(7, 30) // Default value
                }

                val endTime = try {
                    LocalTime.parse(endTimeString, DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: DateTimeParseException) {
                    // Handle the error here. For example, you might want to log it, show a message to the user, or set a default value.
                    newMessage("Błąd parsowania czasu koncowego: $e")
                    LocalTime.of(20, 30) // Default value
                }


                val daysDifference = Period.between(
                    currentTime.toLocalDate(),
                    appointmentDateTime.toLocalDate()
                ).days

                if (daysDifference.toLong() == 1L && currentTime.toLocalTime()
                        .isAfter(startTime) && currentTime.toLocalTime().isBefore(endTime)
                ) {
                    try {
                        val appointmentDate = dateFormatter.parse(appointment.date)

                        val appointmentLocalDate = appointmentDate?.let {
                            val calendar = Calendar.getInstance()
                            calendar.time = it
                            val year = calendar.get(Calendar.YEAR)
                            val month = calendar.get(Calendar.MONTH) + 1
                            val day = calendar.get(Calendar.DAY_OF_MONTH)

                            LocalDate.of(year, month, day)
                        }

                        val formattedDateLocalDate = LocalDate.parse(
                            formattedDate,
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        )

                        appointmentLocalDate?.let {
                            if (it == formattedDateLocalDate.plusDays(1)) {
                                appointmentsToSend.add(appointment)
                            }
                        }

                        if (appointmentsToSend.isNotEmpty()) {
                            appointmentsToNotify.value = appointmentsToSend
                        }
                    } catch (e: Exception) {
                        viewState.value = AppState.Error
                    }
                }
            }
        }
    }


     fun findNearestAppointmentToday(
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): Appointment? {
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val currentDate = currentDateTime.format(dateFormatter)
        val currentTime = currentDateTime.format(timeFormatter)

        var nearestAppointment: Appointment? = null
        var minTimeDifference: Long = Long.MAX_VALUE

        for (appointment in appointmentsLists.value ?: emptyList()) {
            try {
                val appointmentDateTime = LocalDateTime.parse(
                    "${appointment.date} ${appointment.startTime}",
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                )

                // Sprawdzamy, czy wizyta jest na dzisiaj i po aktualnej godzinie
                if (appointment.date == currentDate && appointment.startTime > currentTime) {
                    val timeDifference =
                        Duration.between(currentDateTime, appointmentDateTime).toMillis()

                    if (timeDifference < minTimeDifference) {
                        minTimeDifference = timeDifference
                        nearestAppointment = appointment
                    }
                }
            } catch (e: Exception) {
                // Obsłuż błąd odpowiednio, np. wyślij log do konsoli lub zaktualizuj stan widoku
                viewState.value = AppState.Idle

            }
        }

        return nearestAppointment
    }


    fun editAppointment(
        context: Context,
        appointment: Appointment,
        notificationIsSent: Boolean = false
    ) {
        val currentAppointments = appointmentsLists.value?.toMutableList() ?: return
        val index = currentAppointments.indexOfFirst { it.id == appointment.id }
        val selectedClient = findCustomerByName(appointment.customer.fullName) ?: return
        val clientIndex = customersLists.value?.indexOf(selectedClient) ?: return

        if (index != -1 && clientIndex != -1) {
            appointment.notificationSent = notificationIsSent

            currentAppointments[index] = appointment
            selectedClient.appointment = appointment

            appointmentsLists.value = currentAppointments

            filesFunctionsAppoiments.saveAppointmentToFile(
                context,
                appointmentsLists.value ?: emptyList()
            )

            customersLists.value?.get(clientIndex)?.appointment = appointment
            fileFunctionsClients.saveCustomersToFile(context, customersLists.value ?: emptyList())

            customersLists.value = fileFunctionsClients.loadCustomersFromFile(context)
            appointmentsLists.value = filesFunctionsAppoiments.loadAppointmentFromFile(context)

        }
    }

    private fun findCustomerByName(name: String): Customer? {
        return customersLists.value?.firstOrNull { it.fullName == name }
    }

    fun setAppointmentsToNotify(emptyList: List<Appointment>) {
        appointmentsToNotify.value = emptyList
    }
}
