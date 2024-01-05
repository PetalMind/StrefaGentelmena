package com.strefagentelmena.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.strefagentelmena.functions.fileFuctions.fileFunctionsClients
import com.strefagentelmena.functions.fileFuctions.filesFunctionsAppoiments
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.CustomerIdGenerator

class CustomersModelView : ViewModel() {
    val customersLists = MutableLiveData<List<Customer>>(emptyList())
    val searchedCustomersLists = MutableLiveData<List<Customer>>(emptyList())
    var selectedCustomer = MutableLiveData<Customer?>(null)
    val messages = MutableLiveData<String>("")
    val searchState = MutableLiveData<Boolean>(false)
    private val selectedCustomerNote = MutableLiveData<String>("")

    //form Errors
    val firstNameError = MutableLiveData<String>()
    val lastNameError = MutableLiveData<String>()
    val phoneNumberError = MutableLiveData<String>()

    private val idGenerator = CustomerIdGenerator()

    val clientDialogState = MutableLiveData<Boolean>(false)
    val deleteDialogState = MutableLiveData<Boolean>(false)

    //Customer data
    private val customerName = MutableLiveData<String>("")
    private val customerLastName = MutableLiveData<String>("")
    private val customerPhoneNumber = MutableLiveData<String>("")

    fun closeAllDialogs() {
        clientDialogState.value = false
        deleteDialogState.value = false
        searchState.value = false
        selectedCustomer.value = null
    }

    fun setSelectedCustomerNote(note: String) {
        selectedCustomerNote.value = note
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
        customerName.value = name.filterNot { it.isWhitespace() }
    }

    fun setCustomerLastName(lastname: String) {
        customerLastName.value = lastname.filterNot { it.isWhitespace() }
    }

    fun setCustomerPhoneNumber(phoneNumber: String) {
        customerPhoneNumber.value = phoneNumber.filterNot { it.isWhitespace() }
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
            noted = selectedCustomerNote.value ?: ""
        )
    }


    private fun clearSelectedClientAndData() {
        selectedCustomer.value = Customer()
        customerPhoneNumber.value = ""
        customerLastName.value = ""
        customerName.value = ""
        selectedCustomerNote.value = ""
    }

    /**
     * Załadowanie danych klienta
     */
    fun setSelectedCustomerData() {
        customerName.value = selectedCustomer.value?.firstName ?: ""
        customerLastName.value = selectedCustomer.value?.lastName ?: ""
        customerPhoneNumber.value = selectedCustomer.value?.phoneNumber ?: ""
        selectedCustomerNote.value = selectedCustomer.value?.noted ?: ""
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
        val currentList = customersLists.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == customer.id }
        var scheduledAppointments = filesFunctionsAppoiments.loadAppointmentFromFile(context)

        if (index != -1) {
            currentList.removeAt(index)
            scheduledAppointments = scheduledAppointments.filter { it.customer.id != customer.id }
            setCustomersList(currentList)
            setSearchedCustomersList(currentList)

            setMessage("Klient ${customer.fullName} został usunięty")

            fileFunctionsClients.saveCustomersToFile(context, currentList)
            filesFunctionsAppoiments.saveAppointmentToFile(context, scheduledAppointments)

            closeDeleteDialog()
        }
    }

    private fun setSearchedCustomersList(currentList: MutableList<Customer>) {
        searchedCustomersLists.value = currentList
    }

    private fun setCustomersList(currentList: MutableList<Customer>) {
        customersLists.value = currentList
    }


    fun validateFirstName(firstName: String) {
        val namePattern = Regex("^[a-zA-Z]+$")

        if (firstName.isEmpty()) {
            firstNameError.postValue("Imię nie może być puste")
        } else if (!namePattern.matches(firstName)) {
            firstNameError.postValue("Imię może zawierać tylko litery")
        } else {
            firstNameError.postValue("")
        }
    }

    fun validateLastName(lastName: String) {
        val namePattern = Regex("^[a-zA-Z]+$")

        if (lastName.isEmpty()) {
            lastNameError.postValue("Nazwisko nie może być puste")
        } else if (!namePattern.matches(lastName)) {
            lastNameError.postValue("Nazwisko może zawierać tylko litery")
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


    fun validateAllFields(
    ): Boolean {
        var isValid = true

        if (customerName.value?.isEmpty() == true) {
            firstNameError.postValue("Imię nie może być puste")
            isValid = false
        } else {
            firstNameError.postValue("")
        }

        if (customerLastName.value?.isEmpty() == true) {
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
        // Załaduj listę wizyt z pliku
        val appointmentsList = filesFunctionsAppoiments.loadAppointmentFromFile(context)

        // Znajdź wizyty przypisane do wybranego klienta
        val appointmentsToEdit =
            appointmentsList.filter { it.customer.id == selectedCustomer.value?.id }

        // Edytuj wizyty (przykładowa operacja, zastąp ją właściwą logiką edycji)
        appointmentsToEdit.forEach { appointment ->
            // Przykładowa edycja: Zmiana czasu rozpoczęcia na 12:00
            appointment.customer = updatedCustomer
            // Możesz dodać więcej operacji edycji w zależności od potrzeb
        }

        // Zapisz zaktualizowaną listę wizyt z powrotem do pliku
        filesFunctionsAppoiments.saveAppointmentToFile(context, appointmentsList)
    }

    fun editCustomer(context: Context) {
        val customersList = customersLists.value ?: return
        val customerToEditIndex = customersList.indexOfFirst { it.id == selectedCustomer.value?.id }
        if (customerToEditIndex == -1) return

        val updatedCustomer = customersList[customerToEditIndex].copy(
            firstName = customerName.value ?: return,
            lastName = customerLastName.value ?: return,
            phoneNumber = customerPhoneNumber.value ?: return,
            noted = selectedCustomerNote.value ?: return
        )

        customersLists.value = customersList.toMutableList().apply {
            this[customerToEditIndex] = updatedCustomer
        }

        loadAndEditAppointments(context, updatedCustomer)
        setMessage("Klient ${updatedCustomer.fullName} został zaktualizowany")
        fileFunctionsClients.saveCustomersToFile(context, customersLists.value ?: return)
        closeCustomerDialog()
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
