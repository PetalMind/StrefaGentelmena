package com.strefagentelmena.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fileFuctions.fileFunctionsClients
import com.strefagentelmena.functions.fileFuctions.fileFunctionsSettings
import com.strefagentelmena.functions.fileFuctions.filesFunctionsAppoiments
import com.strefagentelmena.functions.smsManager
import com.strefagentelmena.models.AppoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.CustomerIdGenerator
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleModelView : ViewModel() {
    val customersList = MutableLiveData<List<Customer>>(emptyList())
    val appointmentsList = MutableLiveData<List<Appointment>>(emptyList())
    private val profile = MutableLiveData<ProfilePreferences>(ProfilePreferences())

    val selectedClient = MutableLiveData<Customer?>(Customer())
    val isNewAppointment = MutableLiveData<Boolean>(false)


    private val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val currentDate: String = sdf.format(Date())
    val currentSelectedAppoinmentsDate = MutableLiveData<String>(currentDate)

    val messages = MutableLiveData<String?>("")
    val viewState = MutableLiveData<AppState>(AppState.Idle)

    /**
     *Dialogs states
     */
    val showAppointmentDialog = MutableLiveData(false)
    val deleteDialogState = MutableLiveData(false)
    val onNotificationClickState = MutableLiveData(false)

    /**
     * Appoiments Data
     *
     */
    val selectedAppointmentDate = MutableLiveData("")
    val selectedAppointmentTime = MutableLiveData("")
    val selectedAppointment = MutableLiveData<Appointment?>(Appointment())


    fun setAppoimentState(newValue: Boolean) {
        isNewAppointment.value = newValue
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

    fun setNewAppoimentsDate(newValue: String) {
        currentSelectedAppoinmentsDate.value = newValue
    }

    /**
     * Clear Date.
     *
     */
    fun clearDate() {
        selectedAppointmentTime.value = ""
        selectedAppointmentDate.value = ""
        selectedAppointment.value = null
    }

    fun setNewDataAppointment(newValue: String) {
        selectedAppointmentDate.value = newValue
    }

    fun setNewTime(newValue: String) {
        selectedAppointmentTime.value = newValue
    }

    fun showDeleteDialog() {
        deleteDialogState.value = true
    }

    fun changeAppoinmentsDate(newValue: String) {
        currentSelectedAppoinmentsDate.value = newValue
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
        selectedAppointmentTime.value = appointment.startTime
        selectedAppointmentDate.value = appointment.date
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
    fun createNewApointment(
        isNew: Boolean,
        selectedClient: Customer?,
        date: String,
        startTime: String,
        context: Context,
    ) {
        val id =
            if (isNew) CustomerIdGenerator().generateId() else selectedAppointment.value?.id!!

        val new = Appointment(
            id = id,
            customer = selectedClient ?: return,
            date = date,  // Zaktualizowane
            startTime = startTime,  // Zaktualizowane
            notificationSent = false
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
        appointmentsList.value = currentAppointments

        // Tu kod do wysyłania powiadomienia
        filesFunctionsAppoiments.saveAppointmentToFile(
            context,
            appointmentsList.value ?: emptyList()
        )
        setMessages("${selectedAppointment.value?.customer?.fullName} już niedługo na twoim fotelu")
    }

    private fun setSelectedAppoiment(notificationSend: Boolean) {
        selectedAppointment.value?.date = selectedAppointmentDate.value ?: return
        selectedAppointment.value?.startTime = selectedAppointmentTime.value ?: return
        selectedAppointment.value?.notificationSent = notificationSend
        selectedAppointment.value?.customer = selectedClient.value ?: return
    }

    /**
     * Remove Appointment.
     *
     * @param id
     */
    fun removeAppointment(id: Int, context: Context) {
        val currentAppointments = appointmentsList.value?.toMutableList() ?: return
        currentAppointments.removeAll { it.id == id }

        appointmentsList.value = currentAppointments

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
            )

        val clientIndex = customersList.value?.indexOf(selectedClient) ?: return

        if (index != -1 && clientIndex != -1) {
            val updatedAppointment = selectedAppointment.value?.copy(
                notificationSent = onNotificationClickState.value ?: notificationIsSent,
                date = selectedAppointmentDate.value ?: return,
                startTime = selectedAppointmentTime.value ?: return,
                customer = selectedClient ?: return
            ) ?: return

            currentAppointments[index] = updatedAppointment

            val updatedClient = selectedClient?.copy(appointment = updatedAppointment)

            customersList.value = customersList.value?.toMutableList()
            if (updatedClient != null) {
                (customersList.value as MutableList<Customer>?)?.set(clientIndex, updatedClient)
            }

            appointmentsList.value = currentAppointments

            fileFunctionsClients.saveCustomersToFile(context, customersList.value ?: return)
            filesFunctionsAppoiments.saveAppointmentToFile(context, currentAppointments)

            customersList.value = fileFunctionsClients.loadCustomersFromFile(context)
            appointmentsList.value = filesFunctionsAppoiments.loadAppointmentFromFile(context)
        }
    }


    fun loadAllData(context: Context) {
        viewState.value = AppState.Loading
        try {
            appointmentsList.value = filesFunctionsAppoiments.loadAppointmentFromFile(context)
            customersList.value = fileFunctionsClients.loadCustomersFromFile(context)
            profile.value = fileFunctionsSettings.loadSettingsFromFile(context)
        } catch (e: Exception) {
            viewState.value = AppState.Error
        }

        viewState.value = AppState.Success
    }

    fun loadCustomersList(context: Context) {
        customersList.value = fileFunctionsClients.loadCustomersFromFile(context)
    }

    fun loadProfile(context: Context) {
        profile.value = fileFunctionsSettings.loadSettingsFromFile(context)
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
}

