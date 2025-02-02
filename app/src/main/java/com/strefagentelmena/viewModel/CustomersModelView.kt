package com.strefagentelmena.viewModel

import com.strefagentelmena.functions.fireBase.addNewCustomerToFirebase
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.functions.fireBase.FirebaseFunctionsAppointments
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.functions.fireBase.editCustomerInFirebase
import com.strefagentelmena.functions.fireBase.getAllCustomersFromFirebase
import com.strefagentelmena.functions.fireBase.removeCustomerFromFirebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun loadClients(firebaseDatabase: FirebaseDatabase) {
        viewModelScope.launch {
            try {
                val customers = withContext(Dispatchers.IO) {
                    getAllCustomersFromFirebase(database = firebaseDatabase)
                }
                customersLists.value = customers
                searchedCustomersLists.value = customers
            } catch (e: Exception) {
                Log.e("ViewModel", "Error loading customers: ${e.message}")
            }
        }
    }


    fun searchCustomers(query: String) {
        val customersToSearch = customersLists.value ?: emptyList()
        val matchingCustomers = if (query.isNotEmpty()) {
            customersToSearch.filter { customer ->
                customer.fullName.contains(
                    query, ignoreCase = true
                ) || customer.phoneNumber.contains(query, ignoreCase = true)
            }
        } else {
            customersToSearch
        }
        searchedCustomersLists.value = matchingCustomers
    }

    fun changeDeleteDialogState() {
        deleteDialogState.value = !deleteDialogState.value!!
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
        // Znajdź najwyższe istniejące ID w liście klientów
        val maxId = customersLists.value?.maxOfOrNull { it.id } ?: 0

        return Customer(
            id = maxId + 1, // Ustaw nowe ID jako największe istniejące + 1
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
    fun addCustomer(firebaseDatabase: FirebaseDatabase) {
        val currentList = customersLists.value?.toMutableList() ?: mutableListOf()
        val newClient = createNewCustomer()

        // Dodanie nowego klienta do lokalnej listy
        currentList.add(newClient)

        // Ustawienie zaktualizowanej listy klientów
        setCustomersList(currentList)
        setSearchedCustomersList(currentList)

        // Dodanie klienta do Firebase
        addNewCustomerToFirebase(firebaseDatabase, newClient) { success ->
            if (success) {
                setMessage("Klient ${newClient.fullName} został dodany")
            } else {
                setMessage("Błąd dodawania klienta")
            }
        }

        // Zamknięcie dialogu i czyszczenie danych
        closeCustomerDialog()
        clearSelectedClientAndData()
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

    /**
     * Edit customer
     *
     * @param context
     */
    fun editCustomer(context: Context, database: FirebaseDatabase) {
        val customersList = customersLists.value ?: return
        val selectedCustomer = selectedCustomer.value ?: return // Upewnij się, że dane są dostępne

        val customerToEditIndex = customersList.indexOfFirst { it.id == selectedCustomer.id }

        if (customerToEditIndex == -1) {
            setMessage("Klient nie został znaleziony") // Obsługa sytuacji, gdy klient nie istnieje
            return
        }

        val updatedCustomer = updateCustomerDetails(customersList[customerToEditIndex])

        editCustomerInFirebase(database, updatedCustomer) { success ->
            if (success) {
                // Aktualizacja listy klientów po udanej edycji w Firebase
                customersLists.value = customersList.toMutableList().apply {
                    this[customerToEditIndex] = updatedCustomer
                }

                // Przeładuj powiązane wizyty

                setMessage("Klient ${updatedCustomer.fullName} został zaktualizowany")
            } else {
                setMessage("Błąd edycji klienta")
            }
        }

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
            ?: throw IllegalArgumentException("Note cannot be null"),
            appointment = selectedCustomer.value?.appointment ?: Appointment()

        )
    }

    fun deleteCustomer(database: FirebaseDatabase) {
        val customersList = customersLists.value ?: return
        val selectedCustomer = selectedCustomer.value ?: return

        val customerToDeleteIndex = customersList.indexOfFirst { it.id == selectedCustomer.id }

        if (customerToDeleteIndex == -1) {
            setMessage("Klient nie został znaleziony")
            return
        }

        customersLists.value = customersList.toMutableList().apply {
            removeAt(customerToDeleteIndex)
            removeCustomerFromFirebase(
                database,
                selectedCustomer.id.toString(),
                completion = { success ->
                    if (success) {
                        setMessage("Klient ${selectedCustomer.fullName} został usunięty")
                    } else {
                        setMessage("Błąd usuwania klienta")
                    }
                })
        }
    }

    fun findAndDeleteAppoiments() {
        viewModelScope.launch {
            val appoiments =
                FirebaseFunctionsAppointments().loadAppointmentsFromFirebase(firebaseDatabase = FirebaseDatabase.getInstance())

            val findAndDelete = appoiments.filter {
                it.customer.id == selectedCustomer.value?.id

            }

        }
    }

    fun sortClientsByName() {
        customersLists.value = customersLists.value?.sortedBy { it.fullName }
    }

    fun sortClientsByDate() {
        customersLists.value = customersLists.value?.sortedByDescending { it.appointment?.date }
    }

    suspend fun sortClientsNormal(database: FirebaseDatabase) {
        customersLists.value = getAllCustomersFromFirebase(database)
    }

    fun sortClientsByDateDesc() {
        customersLists.value = customersLists.value?.sortedBy { it.appointment?.date }
    }

    fun setSelectedCustomer(customer: Customer?) {
        selectedCustomer.value = customer
    }
}
