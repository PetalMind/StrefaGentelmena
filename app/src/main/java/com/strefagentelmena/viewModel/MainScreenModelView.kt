package com.strefagentelmena.viewModel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
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
import java.util.Calendar
import java.util.Locale

class MainScreenModelView : ViewModel() {
    val messages = MutableLiveData<String>("")
    val viewState = MutableLiveData<AppState>(AppState.Idle)
    val customersLists = MutableLiveData<List<Customer>>(emptyList())
    private val appointmentsLists = MutableLiveData<List<Appointment>>(emptyList())
    val appointmentsToNotify = MutableLiveData<List<Appointment>>(emptyList())
    val showNotifyDialog = MutableLiveData<Boolean>(false)
    val upcomingAppointment: MutableLiveData<Appointment?> = MutableLiveData()
    val profilePreferences = MutableLiveData<ProfilePreferences>()
    val dataLoaded = MutableLiveData<Boolean>(false)

    val _displayGreetings = MutableLiveData(
        greetingsManager.randomGreeting(profilePreferences.value?.userName ?: "")
    )

    val displayGreetings: MutableLiveData<String> = _displayGreetings

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
        val loadedProfile = fileFunctionsSettings.loadSettingsFromFile(context)

        profilePreferences.value = loadedProfile
        _displayGreetings.value = greetingsManager.randomGreeting(loadedProfile.userName)
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

        // Dodaj warunek, aby uniknąć zwracania pustej listy, jeśli coś poszło nie tak
        if (loadedCustomers.isNotEmpty()) {
            // Zaktualizuj customersLists.value, jeśli to jest wymagane
            customersLists.value = loadedCustomers
        }

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


    fun sendNotificationsForUpcomingAppointments(
        formattedDate: String,
    ) {
        val appointmentsToSend = mutableListOf<Appointment>()
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val currentTime = LocalDateTime.now()


        appointmentsLists.value?.let { appointments ->
            if (appointments.isNotEmpty()) {
                for (appointment in appointments) {
                    if (appointment.notificationSent) continue
                    val appointmentDateTime = LocalDateTime.parse(
                        "${appointment.date} ${appointment.startTime}",
                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    )

                    val startTime = LocalTime.parse(
                        profilePreferences.value!!.notificationSendStartTime,
                        DateTimeFormatter.ofPattern("HH:mm")
                    )
                    val endTime = LocalTime.parse(
                        profilePreferences.value!!.notificationSendEndTime,
                        DateTimeFormatter.ofPattern("HH:mm")
                    )

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
                                val month =
                                    calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH jest indeksowane od zera
                                val day = calendar.get(Calendar.DAY_OF_MONTH)

                                LocalDate.of(year, month, day)
                            }

                            val formattedDateLocalDate =
                                LocalDate.parse(
                                    formattedDate,
                                    DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                )

                            appointmentLocalDate?.let {
                                // Sprawdzamy, czy wizyta jest dokładnie jutro
                                if (it == formattedDateLocalDate.plusDays(1)) {
                                    appointmentsToSend.add(appointment)
                                }
                            }
                            appointmentsToSend.let {
                                if (it.isNotEmpty()) {
                                    appointmentsToNotify.value = appointmentsToSend
                                }
                            }
                        } catch (e: Exception) {
                            // Obsłuż błąd odpowiednio, np. wyślij log do konsoli lub zaktualizuj stan widoku
                            viewState.value = AppState.Error
                        }

                    } else {
                        return
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
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

                    upcomingAppointment.value = nearestAppointment
                }
            } catch (e: Exception) {
                // Obsłuż błąd odpowiednio, np. wyślij log do konsoli lub zaktualizuj stan widoku
                viewState.value = AppState.Error
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

            filesFunctionsAppoiments.saveAppointmentToFile(context, appointmentsLists.value)

            customersLists.value?.get(clientIndex)?.appointment = appointment
            fileFunctionsClients.saveCustomersToFile(context, customersLists.value)

            customersLists.value = fileFunctionsClients.loadCustomersFromFile(context)
            appointmentsLists.value = filesFunctionsAppoiments.loadAppointmentFromFile(context)

        }
    }

    fun findCustomerByName(name: String): Customer? {
        return customersLists.value?.firstOrNull { it.fullName == name }
    }
}
