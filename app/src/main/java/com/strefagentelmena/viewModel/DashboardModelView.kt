package com.strefagentelmena.viewModel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fileFuctions.ClientsFilesFuctions
import com.strefagentelmena.functions.fileFuctions.fileFunctionsClients
import com.strefagentelmena.functions.fileFuctions.filesFunctionsAppoiments
import com.strefagentelmena.functions.greetingsManager
import com.strefagentelmena.models.Appointment
import com.strefagentelmena.models.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DashboardModelView : ViewModel() {
    val messages = MutableLiveData<String>("")
    val viewState = MutableLiveData<AppState>(AppState.Idle)
    val customersLists = MutableLiveData<List<Customer>>(emptyList())
    val appointmentsLists = MutableLiveData<List<Appointment>>(emptyList())
    val isDataLoaded = MutableLiveData<Boolean>(false)
    val appointmentsToNotify = MutableLiveData<List<Appointment>>(emptyList())
    val showNotifyDialog = MutableLiveData<Boolean>(false)

    private val _displayGreetings = MutableLiveData(greetingsManager.randomGreeting())
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


    @RequiresApi(Build.VERSION_CODES.O)
    fun loadAllData(context: Context) {
        setViewState(AppState.Loading)

        try {
            loadCustomersFromFile(context)
            loadAppointmentFromFile(context)
            setViewState(AppState.Success)
        } catch (e: Exception) {
            setViewState(AppState.Error)
        }

    }

    /**
     * Load Customers From File.
     *
     * @param context
     */
    fun loadCustomersFromFile(context: Context) {
        val file = File(context.filesDir, "customers.json")

        if (file.exists()) {
            FileReader(file).use {
                val type = object : TypeToken<List<Customer>>() {}.type
                val loadedCustomers: List<Customer> = Gson().fromJson(it, type)
                customersLists.value = loadedCustomers
            }
        }
    }

    /**
     * Load Appointment From File.
     *
     * @param context
     */
    fun loadAppointmentFromFile(context: Context) {
        val file = File(context.filesDir, "appointment.json")

        if (file.exists()) {
            FileReader(file).use {
                val type = object : TypeToken<List<Appointment>>() {}.type
                val loadedAppointments: List<Appointment> = Gson().fromJson(it, type)

                appointmentsLists.value = loadedAppointments
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendNotificationsForUpcomingAppointments(
        context: Context,
    ) {
        val appointmentsToSend = mutableListOf<Appointment>()

        if (appointmentsLists.value?.isNotEmpty() == true) {
            appointmentsLists.value?.forEach { appointment ->
                try {
                    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
                    val formattedDate = LocalDate.now().format(formatter)

                    if (appointment.notificationSent) {
                        return@forEach
                    }
                    if (appointment.date <= formattedDate) {
                        return@forEach
                    } else {
                        appointmentsToSend.add(appointment)
                    }
                } catch (e: Exception) {
                    viewState.value = AppState.Error
                }
            }

            appointmentsToNotify.value = appointmentsToSend

            if (appointmentsToNotify.value?.isNotEmpty() == true) {
                showNotifyDialog.value = true
            }
            try {
                loadAllData(context)
            } catch (e: Exception) {
                // Możesz dodać tutaj logowanie błędu lub inną formę informacji dla dewelopera lub użytkownika
            }
        }
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

        appointment.notificationSent = notificationIsSent



        if (index != -1 && clientIndex != -1) {
            currentAppointments[index] = appointment
            selectedClient.appointment = appointment

            appointmentsLists.value = currentAppointments

            filesFunctionsAppoiments.saveAppointmentToFile(context, appointmentsLists.value)
            appointmentsLists.value = filesFunctionsAppoiments.loadAppointmentFromFile(context)

            customersLists.value?.get(clientIndex)?.appointment = appointment
            fileFunctionsClients.saveCustomersToFile(context, customersLists.value)
        }
    }

    fun findCustomerByName(name: String): Customer? {
        return customersLists.value?.firstOrNull { it.fullName == name }
    }
}
