package com.strefagentelmena.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.CustomerIdGenerator
import java.io.File
import java.io.FileReader
import java.io.FileWriter

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
    val appointmentDateError = MutableLiveData<String>()

    private val idGenerator = CustomerIdGenerator()

    val booleanAddClientDialog = MutableLiveData<Boolean>(false)

    //Customer data
    private val customerName = MutableLiveData<String>("")
    private val customerLastName = MutableLiveData<String>("")
    private val customerPhoneNumber = MutableLiveData<String>("")
    private val customerAppointmentDate = MutableLiveData<String>("")


//

    fun searchCustomers(query: String) {
        val customersToSearch = customersLists.value ?: emptyList()
        val matchingCustomers = if (query.isNotEmpty()) {
            customersToSearch.filter { customer ->
                customer.fullName?.contains(query, ignoreCase = true) == true ||
                        customer.phoneNumber?.contains(query, ignoreCase = true) == true
            }
        } else {
            customersToSearch
        }
        searchedCustomersLists.value = matchingCustomers
    }


    fun setShowSearchState(showSearchState: Boolean) {
        searchState.value = showSearchState
    }

    fun setMessage(message: String) {
        messages.value = message
    }

    fun clearMessage() {
        messages.value = ""
    }

    fun findCustomerByName(name: String): Customer? {
        return customersLists.value?.firstOrNull { it.fullName == name }
    }

    /**
     * Set Customer Appointment Date.
     *
     * @param appointmentDate
     */
    fun setCustomerAppointmentDate(appointmentDate: String) {
        customerAppointmentDate.value = appointmentDate
    }

    /**
     * Add Customer.
     *
     * @param customer
     */
    fun addCustomer(customer: Customer, context: Context) {
        val currentList = customersLists.value?.toMutableList() ?: mutableListOf()
        currentList.add(customer)
        customersLists.value = currentList.toList()
        searchedCustomersLists.value = customersLists.value

        // Aktualizuj listę klientów
        setMessage("Klient ${customer.fullName} został dodany")

        closeAddClientDialog()

        saveCustomersToFile(context)
    }


    /**
     * Show Add Customer Dialog.
     *
     */
    fun showAddCustomerDialog() {
        booleanAddClientDialog.value = true
    }

    /**
     * Close Add Client Dialog.
     *
     */
    fun closeAddClientDialog() {
        booleanAddClientDialog.value = false
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
        if (phoneNumber.length != 9) {
            phoneNumberError.postValue("Numer telefonu musi mieć 9 cyfr")
        } else {
            phoneNumberError.postValue("")
        }
    }

    fun validateAppointmentDate(appointmentDate: String) {
        if (appointmentDate.isEmpty()) {
            appointmentDateError.postValue("Data wizyty nie może być pusta")
        } else {
            appointmentDateError.postValue("")
        }
    }


    fun createNewCustomer(
        firstName: String?,
        lastName: String?,
        phoneNumber: String?,
    ): Customer {
        return Customer().apply {
            this.id = CustomerIdGenerator().generateId()
            this.firstName = firstName
            this.lastName = lastName
            this.phoneNumber = phoneNumber
            this.lastAppointmentDate = ""
        }
    }

    fun validateAllFields(
        firstName: String,
        lastName: String,
        phoneNumber: String,
    ): Boolean {
        var isValid = true

        if (firstName.isEmpty()) {
            firstNameError.postValue("Imię nie może być puste")
            isValid = false
        } else {
            firstNameError.postValue("")
        }

        if (lastName.isEmpty()) {
            lastNameError.postValue("Nazwisko nie może być puste")
            isValid = false
        } else {
            lastNameError.postValue("")
        }

        if (phoneNumber.length != 9) {
            phoneNumberError.postValue("Numer telefonu musi mieć 9 cyfr")
            isValid = false
        } else {
            phoneNumberError.postValue("")
        }


        return isValid
    }

    fun saveCustomersToFile(context: Context) {
        val gson = Gson()
        val jsonString = gson.toJson(customersLists.value)
        val file = File(context.filesDir, "customers.json")

        FileWriter(file).use {
            it.write(jsonString)
        }
    }

    fun loadCustomersFromFile(context: Context) {
        val file = File(context.filesDir, "customers.json")

        if (file.exists()) {
            FileReader(file).use {
                val type = object : TypeToken<List<Customer>>() {}.type
                val loadedCustomers: List<Customer> = Gson().fromJson(it, type)
                customersLists.value = loadedCustomers
                searchedCustomersLists.value = loadedCustomers
            }
        }
    }

    fun editCustomer(
        id: Int,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        context: Context,
    ) {
        // Pobierz listę klientów (zakładam, że masz listę klientów w ViewModel)
        val customersList = customersLists.value ?: return

        // Znajdź i edytuj klienta o danym ID
        val customerToEditIndex = customersList.indexOfFirst { it.id == id }
        if (customerToEditIndex == -1) return

        val updatedCustomer = customersList[customerToEditIndex].copy(
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phoneNumber
        )

        val updatedCustomersList = customersList.toMutableList().apply {
            this[customerToEditIndex] = updatedCustomer
        }

        // Aktualizuj listę klientów
        customersLists.value = updatedCustomersList
        setMessage("Klient ${updatedCustomer.fullName} został zaktualizowany")

        // Zapisz zmiany
        saveCustomersToFile(context)
    }


}
