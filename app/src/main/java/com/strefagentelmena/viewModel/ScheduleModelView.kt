package com.strefagentelmena.viewModel

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.filesFunctions
import com.strefagentelmena.models.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.CustomerIdGenerator
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleModelView : ViewModel() {
    val appointments = MutableLiveData<List<Appointment>>(emptyList())
    private val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val currentDate: String = sdf.format(Date())
    val currentSelectedAppoinmentsDate = MutableLiveData<String>(currentDate)
    val currentWeekDays = mutableStateOf<List<String>>(emptyList())

    val messages = MutableLiveData<String?>(null)
    val viewState = MutableLiveData<AppState>(AppState.Idle)

    /**
     *Dialogs states
     */
    val showAppointmentDialog = MutableLiveData(false)
    val deleteDialog = MutableLiveData(false)

    /**
     * Appoiments Data
     *
     */
    val selectedAppointmentDate = MutableLiveData("")
    val selectedAppointmentTime = MutableLiveData("")
    val selectedAppointment = MutableLiveData<Appointment?>()


    fun clearMessages() {
        messages.value = null
    }

    fun setMessages(newValue: String) {
        messages.value = newValue
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun setNewDataAppointment(newValue: String) {
        selectedAppointmentDate.value = newValue
    }

    fun setNewTime(newValue: String) {
        selectedAppointmentTime.value = newValue
    }

    fun showDeleteDialog() {
        deleteDialog.value = true
    }

    fun changeAppoinmentsDate(newValue: String) {
        currentSelectedAppoinmentsDate.value = newValue
    }

    /**
     * Hide Delete Dialog.
     *
     */
    fun hideDeleteDialog() {
        deleteDialog.value = false
    }

    /**
     * Select Appointment.
     *
     * @param appointment
     */
    fun selectAppointment(appointment: Appointment) {
        selectedAppointment.value = appointment
        selectedAppointmentTime.value = appointment.startTime
        selectedAppointmentDate.value = appointment.date
    }

    /**
     * Find Upcoming Appointment.
     *
     * @return [Appointment? or null]
     */
    fun findUpcomingAppointment(): Appointment? {
        val currentAppointments = appointments.value ?: return null
        Log.d("findUpcoming", "Current appointments: $currentAppointments")

        val currentTime = System.currentTimeMillis()

        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        return currentAppointments
            .filter { !it.notificationSent }
            .also { Log.e("findUpcoming", "After filtering not sent: $it") }
            .mapNotNull { appointment ->
                val appointmentTimeStr = "${appointment.date} ${appointment.startTime}"
                val appointmentTime =
                    format.parse(appointmentTimeStr)?.time ?: return@mapNotNull null
                Pair(appointment, appointmentTime)
            }
            .also { Log.e("findUpcoming", "After mapping to time: $it") }
            .sortedBy { it.second }
            .firstOrNull { it.second > currentTime }
            ?.also { Log.e("findUpcoming", "Selected appointment: $it") }
            ?.first
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
     * Create New Apointment.
     *
     * @param isNew
     * @param selectedClient
     * @param date
     * @param startTime
     * @param context
     */
    @RequiresApi(Build.VERSION_CODES.O)
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

        hideApoimentDialog()

        selectedAppointment.value = new
        addAppointment(context)
    }

    /**
     * Add Appointment.
     *
     * @param context
     */
    private fun addAppointment(context: Context) {
        val currentAppointments = appointments.value?.toMutableList() ?: mutableListOf()
        currentAppointments.add(selectedAppointment.value ?: return)
        appointments.value = currentAppointments

        // Tu kod do wysyłania powiadomienia
        saveAppointmentToFile(context)
        setMessages("${selectedAppointment.value?.customer?.fullName} już niedługo na twoim fotelu")
    }

    /**
     * Remove Appointment.
     *
     * @param id
     */
    fun removeAppointment(id: Int, context: Context) {
        val currentAppointments = appointments.value?.toMutableList() ?: return
        currentAppointments.removeAll { it.id == id }
        appointments.value = currentAppointments
        saveAppointmentToFile(context)
        setMessages("Jedna wizyta mniej do zrobienia")
    }

    /**
     * Edit Appointment.
     *
     * @param context
     */
    fun editAppointment(context: Context) {
        val currentAppointments = appointments.value?.toMutableList() ?: return
        val index = currentAppointments.indexOfFirst { it.id == selectedAppointment.value?.id }
        selectedAppointment.value?.date = selectedAppointmentDate.value ?: return
        selectedAppointment.value?.startTime = selectedAppointmentTime.value ?: return
        selectedAppointment.value?.notificationSent = true

        if (index != -1) {
            currentAppointments[index] = selectedAppointment.value ?: return
            appointments.value = currentAppointments

            saveAppointmentToFile(context)
            setMessages("Wizyta ${selectedAppointment.value?.customer?.fullName} właśnie przeszła metamorfozę w systemie.")
            filesFunctions.loadAppointmentFromFile(context)
        }
    }

    /**
     * Save Appointment To File.
     *
     * @param context
     */
    fun saveAppointmentToFile(context: Context) {
        val gson = Gson()
        val jsonString = gson.toJson(appointments.value)
        val file = File(context.filesDir, "appointment.json")

        FileWriter(file).use {
            it.write(jsonString)
        }
    }

    fun loadAllData(context: Context) {
        viewState.value = AppState.Loading
        try {
            appointments.value = filesFunctions.loadAppointmentFromFile(context)
        } catch (e: Exception) {
            viewState.value = AppState.Error
        }

        viewState.value = AppState.Success
    }

}
