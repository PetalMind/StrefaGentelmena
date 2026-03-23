package com.strefagentelmena.viewModel

import com.strefagentelmena.functions.fireBase.addNewCustomerToFirebase
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.functions.fireBase.FirebaseFunctionsAppointments
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.CustomerNote
import com.strefagentelmena.models.notesOrderedNewestFirst
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.effectiveCustomerId
import com.strefagentelmena.models.lastVisitEpochDayOrNull
import com.strefagentelmena.models.mergeCustomersWithVisitStats
import com.strefagentelmena.functions.fireBase.allocateNextCustomerId
import com.strefagentelmena.functions.fireBase.editCustomerInFirebase
import com.strefagentelmena.functions.fireBase.getAllCustomersFromFirebase
import com.strefagentelmena.functions.fireBase.removeCustomerAndTheirAppointmentsFromFirebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.Locale

class CustomersModelView : ViewModel() {

    companion object {
        private val appointmentDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val appointmentTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val nameCollator: Collator =
            Collator.getInstance(Locale.forLanguageTag("pl-PL")).apply { strength = Collator.PRIMARY }
    }

    /** Ostatnie zapytanie wyszukiwania — żeby po sortowaniu odświeżyć widoczną listę przy aktywnym searchu. */
    private var lastCustomerSearchQuery: String = ""
    val customersLists = MutableLiveData<List<Customer>>(emptyList())
    /** Po pierwszym (lub kolejnym) zakończeniu [loadClients] — pod intencje nawigacji z dashboardu. */
    val customersCatalogReady = MutableLiveData(false)
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
    val addNewChildDialogVisible = MutableLiveData(false)

    val newChildFirstName = MutableLiveData("")
    val newChildAgeYears = MutableLiveData("")
    val newChildBirthDate = MutableLiveData("")
    val newChildFirstNameError = MutableLiveData("")

    //Customer data
    val customerName = MutableLiveData<String>("")
    val customerLastName = MutableLiveData<String>("")
    val customerPhoneNumber = MutableLiveData<String>("")
    val customerEmail = MutableLiveData<String>("")
    val customerNote = MutableLiveData<String>("")
    /** Notatki w otwartym dialogu (najnowsza pierwsza). */
    val customerNoteHistory = MutableLiveData<List<CustomerNote>>(emptyList())
    /** Notatka edytowana w polu tekstowym (oryginał z listy — identyfikacja po treści i [CustomerNote.addedAtMillis]). */
    val customerNoteEditTarget = MutableLiveData<CustomerNote?>(null)

    val customerVisitHistory = MutableLiveData<List<Appointment>>(emptyList())
    val customerVisitHistoryLoading = MutableLiveData(false)

    fun closeAllDialogs() {
        clientDialogState.value = false
        deleteDialogState.value = false
        searchState.value = false
        addNewChildDialogVisible.value = false
        resetNewChildForm()
        selectedCustomer.value = null
    }

    fun setSelectedCustomerNote(note: String) {
        customerNote.value = note
    }

    private fun customerNotesSameIdentity(a: CustomerNote, b: CustomerNote): Boolean =
        a.addedAtMillis == b.addedAtMillis && a.text == b.text

    fun appendCustomerNote() {
        if (customerNoteEditTarget.value != null) return
        val text = customerNote.value?.trim().orEmpty()
        if (text.isEmpty()) return
        val entry = CustomerNote(text, System.currentTimeMillis())
        val current = customerNoteHistory.value.orEmpty()
        customerNoteHistory.value = listOf(entry) + current
        customerNote.value = ""
    }

    fun beginEditCustomerNote(note: CustomerNote) {
        customerNoteEditTarget.value = CustomerNote(note.text, note.addedAtMillis)
        customerNote.value = note.text
    }

    fun cancelCustomerNoteEdit() {
        customerNoteEditTarget.value = null
        customerNote.value = ""
    }

    fun applyCustomerNoteEdit() {
        val target = customerNoteEditTarget.value ?: return
        val newText = customerNote.value?.trim().orEmpty()
        if (newText.isEmpty()) return
        val list = customerNoteHistory.value.orEmpty().toMutableList()
        val idx = list.indexOfFirst { customerNotesSameIdentity(it, target) }
        if (idx < 0) {
            cancelCustomerNoteEdit()
            return
        }
        list[idx] = CustomerNote(newText, target.addedAtMillis)
        customerNoteHistory.value = list
        cancelCustomerNoteEdit()
    }

    fun removeCustomerNote(note: CustomerNote) {
        val target = customerNoteEditTarget.value
        if (target != null && customerNotesSameIdentity(target, note)) {
            cancelCustomerNoteEdit()
        }
        customerNoteHistory.value = customerNoteHistory.value.orEmpty()
            .filterNot { customerNotesSameIdentity(it, note) }
    }

    private fun notesArrayListForSave(): ArrayList<CustomerNote>? {
        val draft = customerNote.value?.trim().orEmpty()
        val editTarget = customerNoteEditTarget.value
        val fromHistory = customerNoteHistory.value.orEmpty()
        val historyForCombine = if (editTarget != null) {
            fromHistory.filterNot { customerNotesSameIdentity(it, editTarget) }
        } else {
            fromHistory
        }
        val combined = buildList {
            if (draft.isNotEmpty()) {
                val millis = editTarget?.addedAtMillis ?: System.currentTimeMillis()
                add(CustomerNote(draft, millis))
            }
            addAll(historyForCombine)
        }
        if (combined.isEmpty()) return null
        val sorted = combined.sortedByDescending { it.addedAtMillis }
        return ArrayList(sorted)
    }

    private fun latestNotedFromNotes(): String =
        notesArrayListForSave()?.firstOrNull()?.text?.trim().orEmpty()

    fun loadClients(firebaseDatabase: FirebaseDatabase) {
        viewModelScope.launch {
            try {
                customersCatalogReady.value = false
                val customers = withContext(Dispatchers.IO) {
                    getAllCustomersFromFirebase(database = firebaseDatabase)
                }
                val appointments = withContext(Dispatchers.IO) {
                    FirebaseFunctionsAppointments().loadAppointmentsFromFirebase(firebaseDatabase)
                }
                val merged = mergeCustomersWithVisitStats(customers, appointments)
                customersLists.value = customersSortedByName(merged)
                searchCustomers(lastCustomerSearchQuery)
            } catch (e: Exception) {
                Log.e("ViewModel", "Error loading customers: ${e.message}")
            } finally {
                customersCatalogReady.value = true
            }
        }
    }


    fun searchCustomers(query: String) {
        lastCustomerSearchQuery = query.trim()
        val customersToSearch = customersLists.value ?: emptyList()
        val matchingCustomers = if (query.isNotEmpty()) {
            customersToSearch.filter { customer ->
                customer.fullName.contains(
                    query, ignoreCase = true
                ) || customer.phoneNumber.contains(query, ignoreCase = true)
                    || customer.email.contains(query, ignoreCase = true)
            }
        } else {
            customersToSearch
        }
        searchedCustomersLists.value = matchingCustomers
    }

    fun changeDeleteDialogState() {
        deleteDialogState.value = !(deleteDialogState.value ?: false)
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
        val digitsOnly = phoneNumber.filter { it.isDigit() }.take(9)
        customerPhoneNumber.value = digitsOnly
    }

    fun setCustomerEmail(email: String) {
        customerEmail.value = email.trim()
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
        addNewChildDialogVisible.value = false
        resetNewChildForm()
    }

    fun openAddNewChildDialog() {
        resetNewChildForm()
        addNewChildDialogVisible.value = true
    }

    fun dismissAddNewChildDialog() {
        addNewChildDialogVisible.value = false
        resetNewChildForm()
    }

    private fun resetNewChildForm() {
        newChildFirstName.value = ""
        newChildAgeYears.value = ""
        newChildBirthDate.value = ""
        newChildFirstNameError.value = ""
    }

    fun setNewChildFirstName(name: String) {
        var n = name.filterNot { it.isWhitespace() }
        n = n.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        newChildFirstName.value = n
        if (n.isNotBlank()) newChildFirstNameError.value = ""
    }

    fun setNewChildAgeYears(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(3)
        newChildAgeYears.value = digits
    }

    fun setNewChildBirthDate(formattedDdMmYyyy: String) {
        newChildBirthDate.value = formattedDdMmYyyy
        newChildAgeYears.value = ""
    }

    fun clearNewChildBirthDate() {
        newChildBirthDate.value = ""
    }

    private fun resolveNewChildBirthDateForSave(): Pair<String, String?> {
        val bdTrim = newChildBirthDate.value?.trim().orEmpty()
        val ageTrim = newChildAgeYears.value?.trim().orEmpty()
        if (bdTrim.isNotEmpty()) {
            val d = runCatching { LocalDate.parse(bdTrim, appointmentDateFormatter) }.getOrNull()
                ?: return "" to "Niepoprawna data urodzenia"
            if (d.isAfter(LocalDate.now())) return "" to "Data urodzenia nie może być w przyszłości"
            if (d.year < 1900) return "" to "Data zbyt wczesna"
            return d.format(appointmentDateFormatter) to null
        }
        if (ageTrim.isNotEmpty()) {
            val age = ageTrim.toIntOrNull()
                ?: return "" to "Podaj poprawny wiek"
            if (age < 0 || age > 120) return "" to "Wiek od 0 do 120"
            val approx = LocalDate.of(LocalDate.now().year - age, 1, 1)
            return approx.format(appointmentDateFormatter) to null
        }
        return "" to null
    }

    fun submitNewChildForParent(firebaseDatabase: FirebaseDatabase) {
        val parent = selectedCustomer.value ?: run {
            setMessage("Brak wybranego rodzica")
            return
        }
        if (parent.id <= 0 || parent.parentCustomerId != 0) {
            setMessage("Ten profil nie jest kontem rodzica")
            return
        }
        val first = newChildFirstName.value?.trim().orEmpty()
        if (first.isEmpty()) {
            newChildFirstNameError.value = "Podaj imię"
            return
        }
        newChildFirstNameError.value = ""
        if (parent.lastName.isBlank()) {
            setMessage("Uzupełnij nazwisko rodzica w profilu")
            return
        }
        val (birthStr, err) = resolveNewChildBirthDateForSave()
        if (err != null) {
            setMessage(err)
            return
        }
        val localMax = customersLists.value?.maxOfOrNull { it.id } ?: 0
        allocateNextCustomerId(firebaseDatabase, localMax) { allocatedId ->
            if (allocatedId == null) {
                setMessage("Błąd dodawania dziecka (ID)")
                return@allocateNextCustomerId
            }
            val child = Customer(
                id = allocatedId,
                firstName = first,
                lastName = parent.lastName,
                phoneNumber = parent.phoneNumber,
                email = "",
                parentCustomerId = parent.id,
                birthDate = birthStr,
            )
            addNewCustomerToFirebase(firebaseDatabase, child) { success ->
                if (success) {
                    val cur = customersLists.value?.toMutableList() ?: mutableListOf()
                    cur.add(child)
                    customersLists.value = customersSortedByName(cur)
                    searchCustomers(lastCustomerSearchQuery)
                    setMessage("Dodano dziecko: ${child.fullName}")
                    dismissAddNewChildDialog()
                } else {
                    setMessage("Błąd zapisu dziecka")
                }
            }
        }
    }

    private fun buildNewCustomer(id: Int): Customer = Customer(
        id = id,
        firstName = customerName.value ?: "",
        lastName = customerLastName.value ?: "",
        phoneNumber = customerPhoneNumber.value ?: "",
        email = customerEmail.value ?: "",
        noted = latestNotedFromNotes(),
        notes = notesArrayListForSave(),
        parentCustomerId = 0,
        birthDate = "",
    )

    fun eligibleToAssignAsChild(parentId: Int): List<Customer> {
        val all = customersLists.value.orEmpty()
        return all.filter { candidate ->
            candidate.id != parentId &&
                candidate.parentCustomerId == 0 &&
                all.none { it.parentCustomerId == candidate.id }
        }.sortedBy { it.fullName }
    }

    fun linkChildToParent(database: FirebaseDatabase, parentId: Int, childId: Int) {
        val list = customersLists.value?.toMutableList() ?: run {
            setMessage("Brak listy klientów")
            return
        }
        if (!list.any { it.id == parentId && it.parentCustomerId == 0 }) {
            setMessage("Nieprawidłowy profil rodzica")
            return
        }
        val childIdx = list.indexOfFirst { it.id == childId }
        if (childIdx < 0) {
            setMessage("Nie znaleziono klienta")
            return
        }
        if (eligibleToAssignAsChild(parentId).none { it.id == childId }) {
            setMessage("Tego klienta nie można przypisać jako dziecko")
            return
        }
        val updated = list[childIdx].copy(parentCustomerId = parentId)
        editCustomerInFirebase(database, updated) { success ->
            if (success) {
                list[childIdx] = updated
                customersLists.value = list
                searchCustomers(lastCustomerSearchQuery)
                setMessage("${updated.fullName} przypisano do profilu rodzica")
            } else {
                setMessage("Błąd zapisu powiązania")
            }
        }
    }

    fun unlinkChildParent(database: FirebaseDatabase, childId: Int) {
        val list = customersLists.value?.toMutableList() ?: return
        val idx = list.indexOfFirst { it.id == childId }
        if (idx < 0) return
        val child = list[idx]
        if (child.parentCustomerId == 0) return
        val updated = child.copy(parentCustomerId = 0)
        editCustomerInFirebase(database, updated) { success ->
            if (success) {
                list[idx] = updated
                customersLists.value = list
                if (selectedCustomer.value?.id == childId) {
                    selectedCustomer.value = updated
                }
                searchCustomers(lastCustomerSearchQuery)
                setMessage("Usunięto powiązanie z rodzicem")
            } else {
                setMessage("Błąd zapisu")
            }
        }
    }



    fun clearSelectedClientAndData() {
        selectedCustomer.value = null
        customerPhoneNumber.value = ""
        customerEmail.value = ""
        customerLastName.value = ""
        customerName.value = ""
        customerNote.value = ""
        customerNoteHistory.value = emptyList()
        customerNoteEditTarget.value = null
        clearCustomerVisitHistory()
    }

    fun clearCustomerVisitHistory() {
        customerVisitHistory.value = emptyList()
        customerVisitHistoryLoading.value = false
    }

    fun loadCustomerVisitHistory(firebaseDatabase: FirebaseDatabase, customerId: Int) {
        viewModelScope.launch {
            customerVisitHistoryLoading.value = true
            try {
                val all = withContext(Dispatchers.IO) {
                    FirebaseFunctionsAppointments().loadAppointmentsFromFirebase(firebaseDatabase)
                }
                customerVisitHistory.value = all
                    .filter { it.effectiveCustomerId() == customerId }
                    .sortedWith { a, b -> compareAppointmentsNewestFirst(a, b) }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error loading visit history: ${e.message}")
                customerVisitHistory.value = emptyList()
            } finally {
                customerVisitHistoryLoading.value = false
            }
        }
    }

    private fun compareAppointmentsNewestFirst(a: Appointment, b: Appointment): Int {
        val dateA = runCatching { LocalDate.parse(a.date, appointmentDateFormatter) }.getOrNull()
        val dateB = runCatching { LocalDate.parse(b.date, appointmentDateFormatter) }.getOrNull()
        when {
            dateA != null && dateB != null -> {
                val dateCompare = dateB.compareTo(dateA)
                if (dateCompare != 0) return dateCompare
            }

            dateA != null -> return -1
            dateB != null -> return 1
        }
        val timeA = runCatching { LocalTime.parse(a.startTime, appointmentTimeFormatter) }.getOrNull()
        val timeB = runCatching { LocalTime.parse(b.startTime, appointmentTimeFormatter) }.getOrNull()
        return when {
            timeA != null && timeB != null -> timeB.compareTo(timeA)
            else -> b.startTime.compareTo(a.startTime)
        }
    }

    /**
     * Załadowanie danych klienta
     */
    fun setSelectedCustomerData() {
        customerName.value = selectedCustomer.value?.firstName ?: ""
        customerLastName.value = selectedCustomer.value?.lastName ?: ""
        customerPhoneNumber.value = selectedCustomer.value?.phoneNumber ?: ""
        customerEmail.value = selectedCustomer.value?.email ?: ""
        customerNote.value = ""
        customerNoteEditTarget.value = null
        customerNoteHistory.value = selectedCustomer.value?.notesOrderedNewestFirst().orEmpty()
    }

    /**
     * Add Customer.
     *
     */
    fun addCustomer(firebaseDatabase: FirebaseDatabase) {
        val localMax = customersLists.value?.maxOfOrNull { it.id } ?: 0
        allocateNextCustomerId(firebaseDatabase, localMax) { allocatedId ->
            if (allocatedId == null) {
                setMessage("Błąd dodawania klienta (ID)")
                return@allocateNextCustomerId
            }
            val newClient = buildNewCustomer(allocatedId)
            addNewCustomerToFirebase(firebaseDatabase, newClient) { success ->
                if (success) {
                    val currentList = customersLists.value?.toMutableList() ?: mutableListOf()
                    currentList.add(newClient)
                    setCustomersList(currentList)
                    searchCustomers(lastCustomerSearchQuery)
                    setMessage("Klient ${newClient.fullName} został dodany")
                    closeCustomerDialog()
                    clearSelectedClientAndData()
                } else {
                    setMessage("Błąd dodawania klienta")
                }
            }
        }
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

        val phonePattern = "^[0-9]{9}$".toRegex()
        if (!phonePattern.matches(customerPhoneNumber.value.orEmpty())) {
            phoneNumberError.postValue("Numer telefonu musi składać się z 9 cyfr")
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
    fun editCustomer(database: FirebaseDatabase) {
        val customersList = customersLists.value ?: return
        val selectedCustomer = selectedCustomer.value ?: return

        val customerToEditIndex = customersList.indexOfFirst { it.id == selectedCustomer.id }

        if (customerToEditIndex == -1) {
            setMessage("Klient nie został znaleziony")
            return
        }

        val updatedCustomer = updateCustomerDetails(customersList[customerToEditIndex])

        editCustomerInFirebase(database, updatedCustomer) { success ->
            if (success) {
                customersLists.value = customersList.toMutableList().apply {
                    this[customerToEditIndex] = updatedCustomer
                }
                searchCustomers(lastCustomerSearchQuery)
                setMessage("Klient ${updatedCustomer.fullName} został zaktualizowany")
                closeCustomerDialog()
            } else {
                setMessage("Błąd edycji klienta")
            }
        }
    }


    private fun updateCustomerDetails(customer: Customer): Customer {
        return customer.copy(
            firstName = customerName.value
                ?: throw IllegalArgumentException("FirstName cannot be null"),
            lastName = customerLastName.value
                ?: throw IllegalArgumentException("LastName cannot be null"),
            phoneNumber = customerPhoneNumber.value
                ?: throw IllegalArgumentException("PhoneNumber cannot be null"),
            email = customerEmail.value ?: "",
            noted = latestNotedFromNotes(),
            notes = notesArrayListForSave(),
            appointment = customer.appointment,
            lastVisit = customer.lastVisit,
            visitCount = customer.visitCount,
            avgWeeksBetweenVisits = customer.avgWeeksBetweenVisits,
            parentCustomerId = customer.parentCustomerId,
            birthDate = customer.birthDate,
        )
    }

    fun deleteCustomer(database: FirebaseDatabase) {
        val customersList = customersLists.value ?: return
        val toRemove = selectedCustomer.value ?: return

        val customerToDeleteIndex = customersList.indexOfFirst { it.id == toRemove.id }

        if (customerToDeleteIndex == -1) {
            setMessage("Klient nie został znaleziony")
            return
        }

        viewModelScope.launch {
            val appointments = withContext(Dispatchers.IO) {
                FirebaseFunctionsAppointments().loadAppointmentsFromFirebase(database)
            }
            val appointmentIds = appointments
                .filter { it.effectiveCustomerId() == toRemove.id }
                .map { it.id }
                .filter { it > 0 }

            val childIdsToOrphan = customersList
                .filter { it.parentCustomerId == toRemove.id }
                .map { it.id }
                .filter { it > 0 }

            removeCustomerAndTheirAppointmentsFromFirebase(
                database,
                toRemove.id,
                appointmentIds,
                childIdsToOrphan,
            ) { success ->
                if (success) {
                    val fresh = customersLists.value ?: return@removeCustomerAndTheirAppointmentsFromFirebase
                    val mutable = fresh.toMutableList()
                    val idx = mutable.indexOfFirst { it.id == toRemove.id }
                    if (idx >= 0) {
                        mutable.removeAt(idx)
                    }
                    for (i in mutable.indices) {
                        if (mutable[i].id in childIdsToOrphan) {
                            mutable[i] = mutable[i].copy(parentCustomerId = 0)
                        }
                    }
                    customersLists.value = mutable
                    searchCustomers(lastCustomerSearchQuery)
                    setMessage("Klient ${toRemove.fullName} został usunięty")
                    selectedCustomer.value = null
                } else {
                    setMessage("Błąd usuwania klienta")
                }
            }
        }
    }

    private fun customersSortedByName(list: List<Customer>): List<Customer> =
        list.sortedWith(compareBy(nameCollator) { it.fullName })

    private fun applySortedOrder(sorted: List<Customer>) {
        customersLists.value = sorted
        searchCustomers(lastCustomerSearchQuery)
    }

    fun sortClientsByName() {
        val list = customersLists.value ?: return
        applySortedOrder(customersSortedByName(list))
    }

    fun sortClientsByDate() {
        val list = customersLists.value ?: return
        applySortedOrder(
            list.sortedWith(
                compareBy<Customer> { it.lastVisitEpochDayOrNull() == null }
                    .thenByDescending { it.lastVisitEpochDayOrNull() ?: Long.MIN_VALUE },
            ),
        )
    }

    suspend fun sortClientsNormal(database: FirebaseDatabase) {
        val customers = getAllCustomersFromFirebase(database)
        val appointments =
            FirebaseFunctionsAppointments().loadAppointmentsFromFirebase(database)
        customersLists.value = mergeCustomersWithVisitStats(customers, appointments)
    }

    fun sortClientsByDateDesc() {
        val list = customersLists.value ?: return
        applySortedOrder(
            list.sortedWith(
                compareBy<Customer> { it.lastVisitEpochDayOrNull() == null }
                    .thenBy { it.lastVisitEpochDayOrNull() ?: Long.MAX_VALUE },
            ),
        )
    }

    fun setSelectedCustomer(customer: Customer?) {
        selectedCustomer.value = customer
    }
}
