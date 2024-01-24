package com.strefagentelmena.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.strefagentelmena.functions.fileFuctions.fileFunctionsClients
import com.strefagentelmena.functions.fileFuctions.filesFunctionsAppoiments
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.CustomerIdGenerator
import com.strefagentelmena.models.appoimentsModel.Appointment
import java.util.Locale

class CustomersModelView : ViewModel() {
    val customersLists = MutableLiveData<List<Customer>>(emptyList())
    val searchedCustomersLists = MutableLiveData<List<Customer>>(emptyList())
    var selectedCustomer = MutableLiveData<Customer?>(null)
    val messages = MutableLiveData<String>("")
    val searchState = MutableLiveData<Boolean>(false)

    //form Errors
    val firstNameError = MutableLiveData<String>()
    val lastNameError = MutableLiveData<String>()
    val phoneNumberError = MutableLiveData<String>()

    private val idGenerator = CustomerIdGenerator()

    val clientDialogState = MutableLiveData<Boolean>(false)
    val deleteDialogState = MutableLiveData<Boolean>(false)

    //Customer data
    val customerName = MutableLiveData<String>("")
    val customerLastName = MutableLiveData<String>("")
    val customerPhoneNumber = MutableLiveData<String>("")
    val customerNote = MutableLiveData<String>("")

    fun closeAllDialogs() {
        clientDialogState.value = false
        deleteDialogState.value = false
        searchState.value = false
        selectedCustomer.value = null
    }

    fun setSelectedCustomerNote(note: String) {
        customerNote.value = note
    }

    fun loadClients(context: Context) {
        customersLists.value = fileFunctionsClients.loadCustomersFromFile(context)
        searchedCustomersLists.value = customersLists.value
    }

    fun searchCustomers(query: String) {
        val customersToSearch = customersLists.value ?: emptyList()
        val matchingCustomers = if (query.isNotEmpty()) {
            customersToSearch.filter { customer ->
                customer.fullName.contains(
                    query,
                    ignoreCase = true
                ) || customer.phoneNumber.contains(query, ignoreCase = true)
            }
        } else {
            customersToSearch
        }
        searchedCustomersLists.value = matchingCustomers
    }

    fun showDeleteDialog() {
        deleteDialogState.value = true
    }

    fun closeDeleteDialog() {
        deleteDialogState.value = false
    }

    fun setCustomerName(name: String) {
        var newName = name.filterNot { it.isWhitespace() }

        newName =
            newName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        customerName.value = newName
    }


    fun setCustomerLastName(lastname: String) {
        var newLastName = lastname.filterNot { it.isWhitespace() }

        newLastName =
            newLastName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        customerLastName.value = newLastName
    }

    fun setCustomerPhoneNumber(phoneNumber: String) {
        val newPhoneNumber = phoneNumber.filterNot { it.isWhitespace() }

        if (newPhoneNumber.length <= 9) {
            customerPhoneNumber.value = newPhoneNumber
        }
    }

    fun setShowSearchState(showSearchState: Boolean) {
        searchState.value = showSearchState
    }

    private fun setMessage(message: String) {
        messages.value = message
    }

    fun clearMessage() {
        messages.value = ""
    }


    /**
     * Show Add Customer Dialog.
     *
     */
    fun showAddCustomerDialog() {
        clientDialogState.value = true
    }

    /**
     * Close Add Client Dialog.
     *
     */
    fun closeCustomerDialog() {
        clientDialogState.value = false
    }

    private fun createNewCustomer(): Customer {
        return Customer(
            id = idGenerator.generateId(),
            firstName = customerName.value ?: "",
            lastName = customerLastName.value ?: "",
            phoneNumber = customerPhoneNumber.value ?: "",
            noted = customerNote.value ?: ""
        )
    }


    fun clearSelectedClientAndData() {
        selectedCustomer.value = null
        customerPhoneNumber.value = ""
        customerLastName.value = ""
        customerName.value = ""
        customerNote.value = ""
    }

    /**
     * Załadowanie danych klienta
     */
    fun setSelectedCustomerData() {
        customerName.value = selectedCustomer.value?.firstName ?: ""
        customerLastName.value = selectedCustomer.value?.lastName ?: ""
        customerPhoneNumber.value = selectedCustomer.value?.phoneNumber ?: ""
        customerNote.value = selectedCustomer.value?.noted ?: ""
    }

    /**
     * Add Customer.
     *
     */
    fun addCustomer(context: Context) {
        val currentList = customersLists.value?.toMutableList() ?: mutableListOf()
        val newClient = createNewCustomer()

        currentList.add(newClient)

        setCustomersList(currentList)
        setSearchedCustomersList(currentList)

        // Aktualizuj listę klientów
        setMessage("Klient ${newClient.fullName} został dodany")
        fileFunctionsClients.saveCustomersToFile(context, customersLists.value ?: emptyList())

        closeCustomerDialog()
        clearSelectedClientAndData()
    }

    fun deleteCustomer(context: Context, customer: Customer) {
        val currentList = customersLists.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == customer.id }
        val scheduledAppointments = filesFunctionsAppoiments.loadAppointmentFromFile(context)

        if (index != -1) {
            removeCustomerAndUpdateLists(
                currentList,
                index,
                scheduledAppointments,
                context,
                customer
            )
        }
    }

    private fun removeCustomerAndUpdateLists(
        currentList: MutableList<Customer>,
        index: Int,
        scheduledAppointments: List<Appointment>,
        context: Context,
        customer: Customer
    ) {
        currentList.removeAt(index)
        val updatedScheduledAppointments =
            scheduledAppointments.filter { it.customer.id != customer.id }

        setCustomersList(currentList)
        setSearchedCustomersList(currentList)

        setMessage("Klient ${customer.fullName} został usunięty")

        fileFunctionsClients.saveCustomersToFile(context, currentList)
        filesFunctionsAppoiments.saveAppointmentToFile(context, updatedScheduledAppointments)

        closeDeleteDialog()
    }


    private fun setSearchedCustomersList(currentList: MutableList<Customer>) {
        searchedCustomersLists.value = currentList
    }

    private fun setCustomersList(currentList: MutableList<Customer>) {
        customersLists.value = currentList
    }


    fun validateFirstName(firstName: String) {
        if (firstName.isEmpty()) {
            firstNameError.postValue("Imię nie może być puste")
        } else {
            firstNameError.postValue("")
        }
    }

    fun validateLastName(lastName: String) {
        if (lastName.isEmpty()) {
            lastNameError.postValue("Nazwisko nie może być puste")
        } else {
            lastNameError.postValue("")
        }
    }

    fun validatePhoneNumber(phoneNumber: String) {
        val phoneNumberPattern = "^[0-9]{9}$".toRegex()

        if (!phoneNumberPattern.matches(phoneNumber)) {
            phoneNumberError.value = "Numer telefonu musi składać się tylko z 9 cyfr"
        } else {
            phoneNumberError.value = ""
        }
    }


    fun checkFormValidity(): Boolean {
        var isValid = true

        if (customerName.value?.trim()?.isEmpty() == true) {
            firstNameError.postValue("Imię nie może być puste")
            isValid = false
        } else {
            firstNameError.postValue("")
        }

        if (customerLastName.value?.trim()?.isEmpty() == true) {
            lastNameError.postValue("Nazwisko nie może być puste")
            isValid = false
        } else {
            lastNameError.postValue("")
        }

        if (customerPhoneNumber.value?.length != 9) {
            phoneNumberError.postValue("Numer telefonu musi mieć 9 cyfr")
            isValid = false
        } else {
            phoneNumberError.postValue("")
        }

        return isValid
    }


    private fun loadAndEditAppointments(context: Context, updatedCustomer: Customer) {
        val appointmentsList = filesFunctionsAppoiments.loadAppointmentFromFile(context)

        val appointmentsToEdit =
            appointmentsList.filter { it.customer.id == selectedCustomer.value?.id }

        appointmentsToEdit.forEach { appointment ->
            appointment.customer = updatedCustomer
        }

        filesFunctionsAppoiments.saveAppointmentToFile(context, appointmentsList)
    }

    /**
     * Edit customer
     *
     * @param context
     */
    fun editCustomer(context: Context) {
        val customersList = customersLists.value ?: return

        val customerToEditIndex = customersList.indexOfFirst { it.id == selectedCustomer.value?.id }

        if (customerToEditIndex == -1) return

        val updatedCustomer = updateCustomerDetails(customersList[customerToEditIndex])

        customersLists.value = customersList.toMutableList().apply {
            this[customerToEditIndex] = updatedCustomer
        }

        loadAndEditAppointments(context, updatedCustomer)

        setMessage("Klient ${updatedCustomer.fullName} został zaktualizowany")

        fileFunctionsClients.saveCustomersToFile(context, customersLists.value ?: return)

        closeCustomerDialog()
    }

    private fun updateCustomerDetails(customer: Customer): Customer {
        return customer.copy(
            firstName = customerName.value
                ?: throw IllegalArgumentException("FirstName cannot be null"),
            lastName = customerLastName.value
                ?: throw IllegalArgumentException("LastName cannot be null"),
            phoneNumber = customerPhoneNumber.value
                ?: throw IllegalArgumentException("PhoneNumber cannot be null"),
            noted = customerNote.value ?: ""
        )
    }


    fun sortClientsByName() {
        customersLists.value = customersLists.value?.sortedBy { it.fullName }
    }

    fun sortClientsByDate() {
        customersLists.value = customersLists.value?.sortedByDescending { it.appointment?.date }
    }

    fun sortClientsNormal(context: Context) {
        customersLists.value = fileFunctionsClients.loadCustomersFromFile(context)
    }

    fun sortClientsByDateDesc() {
        customersLists.value = customersLists.value?.sortedBy { it.appointment?.date }
    }

    fun setSelectedCustomer(customer: Customer?) {
        selectedCustomer.value = customer
    }
}
