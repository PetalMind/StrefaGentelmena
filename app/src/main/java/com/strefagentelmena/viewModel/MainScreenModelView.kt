package com.strefagentelmena.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fileFuctions.fileFunctionsClients
import com.strefagentelmena.functions.fileFuctions.fileFunctionsSettings
import com.strefagentelmena.functions.fileFuctions.filesFunctionsAppoiments
import com.strefagentelmena.functions.greetingsManager
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter

class  MainScreenModelView : ViewModel() {
    val messages = MutableLiveData("")
    val viewState = MutableLiveData(AppState.Idle)
    val customersLists = MutableLiveData<List<Customer>>(listOf(Customer()))
    private val appointmentsLists = MutableLiveData<List<Appointment>>(listOf(Appointment()))
    val appointmentsToNotify = MutableLiveData<List<Appointment>>(emptyList())
    val showNotifyDialog = MutableLiveData(false)
    val upcomingAppointment: MutableLiveData<Appointment> = MutableLiveData(Appointment())
    val profilePreferences = MutableLiveData(ProfilePreferences())
    val dataLoaded = MutableLiveData(false)
    val displayGreetings: MutableLiveData<String> = MutableLiveData("")

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

    private fun loadAllData(context: Context): Boolean {
        return try {
            loadProfile(context)
            setCustomersList(loadCustomersList(context = context))
            setAppointmentsList(loadApointmentsList(context = context))
            setUpcomingAppointment(findNearestAppointmentToday() ?: Appointment())
            sendNotificationsForUpcomingAppointments()

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

        setGreetingRandom(greetingsManager.randomGreeting(profilePreferences.value?.userName ?: ""))
    }

    /**
     * Load customers list
     *
     * @param context
     * @return
     */
    private fun loadCustomersList(context: Context): List<Customer> {
        return fileFunctionsClients.loadCustomersFromFile(context)
    }

    private fun loadApointmentsList(context: Context): List<Appointment> {
        // Zwróć załadowanych klientów
        return filesFunctionsAppoiments.loadAppointmentFromFile(context)
    }


    private fun sendNotificationsForUpcomingAppointments() {
        val appointmentsToSend = mutableListOf<Appointment>()
        val currentTime = LocalDateTime.now()

        appointmentsLists.value?.filterNot { it.notificationSent }
            ?.forEach { appointment ->
                val appointmentDateTime = LocalDateTime.parse(
                    "${appointment.date} ${appointment.startTime}",
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                )

                val daysDifference = Period.between(
                    currentTime.toLocalDate(),
                    appointmentDateTime.toLocalDate()
                ).days

                if (daysDifference.toLong() == 1L && currentTime.toLocalTime().isAfter(
                        LocalTime.parse(
                            profilePreferences.value?.notificationSendStartTime,
                            DateTimeFormatter.ofPattern("HH:mm")
                        )
                    ) && currentTime.toLocalTime().isBefore(
                        LocalTime.parse(
                            profilePreferences.value?.notificationSendEndTime,
                            DateTimeFormatter.ofPattern("HH:mm")
                        )
                    )
                ) {
                    try {
                        val nextDay = LocalDate.parse(
                            LocalDate.now().format(
                                DateTimeFormatter.ofPattern("dd.MM.yyyy")
                            ),
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        ).plusDays(1)

                        if (appointment.date == nextDay.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        ) {
                            appointmentsToSend.add(appointment)
                        }
                    } catch (e: Exception) {
                        setViewState(AppState.Error)
                    }
                }
            }

        if (appointmentsToSend.isNotEmpty()) {
            setAppointmentsToNotify(appointmentsToSend)
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
    ): Appointment {
        val oneHourLater = currentDateTime.plusHours(1)

        return appointmentsLists.value
            ?.firstOrNull {
                val appointmentDateTime = LocalDateTime.parse(
                    "${it.date} ${it.startTime}",
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                )

                appointmentDateTime.isAfter(currentDateTime) && appointmentDateTime.isBefore(
                    oneHourLater
                )
            } ?: Appointment().also { setViewState(AppState.Error) }
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
            setAppointmentsList(currentAppointments)

            filesFunctionsAppoiments.saveAppointmentToFile(
                context,
                appointmentsLists.value ?: emptyList()
            )

            customersLists.value?.get(clientIndex)?.appointment = appointment
            fileFunctionsClients.saveCustomersToFile(context, customersLists.value ?: emptyList())

            setCustomersList(fileFunctionsClients.loadCustomersFromFile(context))
            setAppointmentsList(filesFunctionsAppoiments.loadAppointmentFromFile(context))
        }
    }

    private fun setAppointmentsList(loadAppointmentFromFile: List<Appointment>) {
        appointmentsLists.value = loadAppointmentFromFile
    }

    private fun setCustomersList(loadCustomersFromFile: List<Customer>) {
        customersLists.value = loadCustomersFromFile
    }

    private fun findCustomerByName(name: String): Customer? {
        return customersLists.value?.firstOrNull { it.fullName == name }
    }

    private fun setUpcomingAppointment(appointment: Appointment) {
        upcomingAppointment.value = appointment
    }

    fun setAppointmentsToNotify(emptyList: List<Appointment>) {
        appointmentsToNotify.value = emptyList
    }

    private fun setGreetingRandom(greeting: String) {
        displayGreetings.value = greeting
    }

    fun setDataLoaded(dataLoadedValue: Boolean) {
        dataLoaded.value = dataLoadedValue
    }
}
