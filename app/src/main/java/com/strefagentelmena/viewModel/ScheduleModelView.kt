package com.strefagentelmena.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fileFuctions.fileFunctionsClients
import com.strefagentelmena.functions.fileFuctions.fileFunctionsSettings
import com.strefagentelmena.functions.fileFuctions.filesFunctionsAppoiments
import com.strefagentelmena.functions.smsManager
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.CustomerIdGenerator
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ScheduleModelView : ViewModel() {
    val customersList = MutableLiveData<List<Customer>>(emptyList())
    val appointmentsList = MutableLiveData<List<Appointment>>(emptyList())
    private val profile = MutableLiveData<ProfilePreferences>(ProfilePreferences())


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
    val showAppointmentDialog = MutableLiveData(false)
    val deleteDialogState = MutableLiveData(false)
    val onNotificationClickState = MutableLiveData(false)
    val appointmentError = MutableLiveData("")

    /**
     * Appoiments Data
     *
     */
    val selectedAppointmentDate = MutableLiveData(currentDate)
    val selectedAppointmentStartTime = MutableLiveData(currentTime)
    val selectedAppointment = MutableLiveData(Appointment())
    val selectedAppointmentEndTime =
        MutableLiveData(LocalTime.now().plusHours(1).format(timeFormatter))

    fun setAppoimentState(newValue: Boolean) {
        isNewAppointment.value = newValue
    }

    fun setAppointmentEndTime(newValue: LocalTime) {
        selectedAppointmentEndTime.value = newValue.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    fun setAppoimentError(newValue: String) {
        appointmentError.value = newValue
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

    /**
     * Clear Date.
     *
     */
    fun clearDate() {
        appointmentError.value = ""
        selectedClient.value = Customer()
        selectedAppointment.value = null
    }

    fun setNewDataAppointment(newValue: LocalDate) {
        selectedAppointmentDate.value = newValue.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }

    fun setNewTime(newValue: LocalTime) {
        selectedAppointmentStartTime.value = newValue.format(DateTimeFormatter.ofPattern("HH:mm"))
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
        showAppointmentDialog.value = true
    }

    /**
     * Hide Apoiment Dialog.
     *
     */
    fun hideApoimentDialog() {
        showAppointmentDialog.value = false
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

    /**
     * Create New Apointment.
     *
     * @param isNew
     * @param selectedClient
     * @param date
     * @param startTime
     * @param context
     */
    fun createNewAppointment(
        isNew: Boolean,
        context: Context,
    ) {
        val id = if (isNew) CustomerIdGenerator().generateId() else selectedAppointment.value?.id!!

        val new = Appointment(
            id = id,
            customer = selectedClient.value ?: return,
            date = selectedAppointmentDate.value ?: return,
            startTime = LocalTime.parse(selectedAppointmentStartTime.value, timeFormatter),
            notificationSent = false,
            endTime = LocalTime.parse(selectedAppointmentEndTime.value, timeFormatter),
        )

        selectedAppointment.value = new

        addAppointment(context)
        hideApoimentDialog()
    }


    /**
     * Add Appointment.
     *
     * @param context
     */
    private fun addAppointment(context: Context) {
        val currentAppointments = appointmentsList.value?.toMutableList() ?: mutableListOf()
        currentAppointments.add(selectedAppointment.value ?: return)
        setAppointmentsList(currentAppointments)

        // Tu kod do wysyłania powiadomienia
        filesFunctionsAppoiments.saveAppointmentToFile(
            context,
            appointmentsList.value ?: emptyList()
        )

        setMessages("${selectedAppointment.value?.customer?.fullName} już niedługo na twoim fotelu")
    }

    /**
     * Remove Appointment.
     *
     * @param id
     */
    fun removeAppointment(id: Int, context: Context) {
        val currentAppointments = appointmentsList.value?.toMutableList() ?: return
        currentAppointments.removeAll { it.id == id }

        setAppointmentsList(currentAppointments)

        filesFunctionsAppoiments.saveAppointmentToFile(
            context,
            appointmentsList.value ?: emptyList()
        )

        setMessages("Jedna wizyta mniej do zrobienia")
    }

    /**
     * Edit Appointment.
     *
     * @param context
     */
    fun editAppointment(
        context: Context,
        notificationIsSent: Boolean = false
    ) {
        val currentAppointments = appointmentsList.value?.toMutableList() ?: return
        val index = currentAppointments.indexOfFirst { it.id == selectedAppointment.value?.id }
        val selectedClient =
            findCustomerByName(selectedClient.value?.fullName ?: "") ?: findCustomerByName(
                selectedAppointment.value?.customer?.fullName ?: ""
            ) ?: return
        val clientIndex = customersList.value?.indexOf(selectedClient) ?: return

        if (index != -1 && clientIndex != -1) {
            val updatedAppointment = selectedAppointment.value?.copy(
                notificationSent = onNotificationClickState.value ?: notificationIsSent,
                date = selectedAppointmentDate.value ?: "",
                startTime = LocalTime.parse(selectedAppointmentStartTime.value, timeFormatter),
                endTime = LocalTime.parse(selectedAppointmentEndTime.value, timeFormatter),
                customer = selectedClient
            ) ?: return

            currentAppointments[index] = updatedAppointment

            (customersList.value as MutableList<Customer>?)?.set(
                clientIndex,
                selectedClient.copy(appointment = updatedAppointment)
            )
            setAppointmentsList(currentAppointments)

            fileFunctionsClients.saveCustomersToFile(context, customersList.value ?: return)
            filesFunctionsAppoiments.saveAppointmentToFile(context, currentAppointments)

            setCustomersList(fileFunctionsClients.loadCustomersFromFile(context))
            setAppointmentsList(filesFunctionsAppoiments.loadAppointmentFromFile(context))
        }
    }


    fun loadAllData(context: Context) {
        viewState.value = AppState.Loading
        try {
            setCustomersList(fileFunctionsClients.loadCustomersFromFile(context))
            setAppointmentsList(filesFunctionsAppoiments.loadAppointmentFromFile(context))

            profile.value = fileFunctionsSettings.loadSettingsFromFile(context)
        } catch (e: Exception) {
            viewState.value = AppState.Error
        }

        viewState.value = AppState.Success
    }


    fun closeAllDialog() {
        showAppointmentDialog.value = false
        deleteDialogState.value = false
    }

    fun sendNotificationForAppointment(
        context: Context
    ) {
        if (selectedAppointment.value?.notificationSent == true) return

        profile.value?.let {
            smsManager.sendNotification(
                selectedAppointment.value ?: return,
                profile = it
            )
        }
        setMessages("Powiadomienie wysłane do ${selectedAppointment.value?.customer?.fullName}")

        editAppointment(context, true)
    }

    private fun setCustomersList(customers: List<Customer>) {
        customersList.value = customers
    }

    private fun setAppointmentsList(appointments: List<Appointment>) {
        appointmentsList.value = appointments
    }

    fun checkAppointmentsList() {
        if (appointmentsList.value?.isNotEmpty() == true) {
            val currentDateAppointments =
                appointmentsList.value?.filter {
                    it.date == selectedAppointmentDate.value
                }

            if (currentDateAppointments?.isNotEmpty() == true) {
                val currentTimeAppointments =
                    currentDateAppointments.filter {
                        it.startTime == LocalTime.parse(
                            selectedAppointmentStartTime.value,
                            timeFormatter
                        )
                    }

                /*
                if (currentTimeAppointments.isNotEmpty()) {
                    setAppoimentError("Ten przedział czasowy jest już zarezerwowany dla.")
                }


                // Additional validations
                if (selectedAppointmentStartTime.value!! > selectedAppointmentEndTime.value.toString()) {
                    setAppoimentError("Czas rozpoczęcia nie może być późniejszy niż czas zakończenia.")
                }

            */
            }
        }
    }
}

