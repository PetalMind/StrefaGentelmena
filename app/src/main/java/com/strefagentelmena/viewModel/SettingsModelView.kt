package com.strefagentelmena.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fireBase.FirebaseEmployeeFunctions
import com.strefagentelmena.functions.fireBase.FirebaseProfilePreferences
import com.strefagentelmena.functions.fireBase.FirebaseRealtimeDatabaseBackup
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import com.strefagentelmena.models.settngsModel.parseEmployeeDisplayDate
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SettingsModelView : ViewModel() {
    val isNewEmplyee = MutableLiveData(false)
    val deleteEmployeeDialog = MutableLiveData(false)
    val viewState = MutableLiveData(AppState.Idle)
    val messages = MutableLiveData("")
    val profilePreferences = MutableLiveData(ProfilePreferences())
    val profileName = MutableLiveData("")
    val selectedEmployee = MutableLiveData(Employee())
    val newEmployee = MutableLiveData(Employee())
    val empolyeeName = MutableLiveData("")
    val empolyeeSurname = MutableLiveData("")
    val empolyeeWorkStartTime = MutableLiveData(Employee.DEFAULT_WORK_START)
    val empolyeeWorkEndTime = MutableLiveData(Employee.DEFAULT_WORK_END)
    val empolyeeVacationFrom = MutableLiveData("")
    val empolyeeVacationTo = MutableLiveData("")
    val empoleesList = MutableLiveData<MutableList<Employee>>(mutableListOf())
    val addEmpolyeeState = MutableLiveData(false)
    val notificationSendStartTime = MutableLiveData("")
    val notificationSendEndTime = MutableLiveData("")
    val notificationSendAutomatic = MutableLiveData(false)

    private val greetingsLists = MutableLiveData<MutableList<String>>(mutableListOf())

    val profileViewState = MutableLiveData<Boolean>(false)
    val notificationViewState = MutableLiveData<Boolean>(false)
    val empolyeeViewState = MutableLiveData<Boolean>(false)
    val greetingsViewState = MutableLiveData(false)
    val backButtonViewState = MutableLiveData<Boolean>(false)
    val updateViewState = MutableLiveData<Boolean>(false)

    val backupViewState = MutableLiveData(false)
    val backupInProgress = MutableLiveData(false)

    fun setIsNewEmplyee(value: Boolean) {
        isNewEmplyee.value = value
    }

    fun setProfileViewState() {
        profileViewState.value = !profileViewState.value!!
    }

    fun setAddNewEmployeeState() {
        addEmpolyeeState.value = !addEmpolyeeState.value!!
    }

    fun setEmpolyeeViewState() {
        empolyeeViewState.value = !empolyeeViewState.value!!
    }

    fun setEmpolyeeName(value: String) {
        empolyeeName.value = value
    }

    fun setEmpolyeeSurname(value: String) {
        empolyeeSurname.value = value
    }

    fun setEmpolyeeDeleteDialog() {
        deleteEmployeeDialog.value = !deleteEmployeeDialog.value!!
    }

    fun clearNewEmpolyee() {
        newEmployee.value = Employee()
        empolyeeName.value = ""
        empolyeeSurname.value = ""
        empolyeeWorkStartTime.value = Employee.DEFAULT_WORK_START
        empolyeeWorkEndTime.value = Employee.DEFAULT_WORK_END
        empolyeeVacationFrom.value = ""
        empolyeeVacationTo.value = ""
    }

    fun setNewProfileName(value: String) {
        profileName.value = value
    }


    /**
     * Set selected empolyee
     */
    fun setSlectedEmpolyee(value: Employee) {
        selectedEmployee.value = value
        empolyeeName.value = value.name
        empolyeeSurname.value = value.surname
        empolyeeWorkStartTime.value =
            value.workStartTime.ifBlank { Employee.DEFAULT_WORK_START }
        empolyeeWorkEndTime.value =
            value.workEndTime.ifBlank { Employee.DEFAULT_WORK_END }
        empolyeeVacationFrom.value = value.vacationFrom
        empolyeeVacationTo.value = value.vacationTo
    }

    fun setEmpolyeeWorkStartTime(value: String) {
        empolyeeWorkStartTime.value = value
    }

    fun setEmpolyeeWorkEndTime(value: String) {
        empolyeeWorkEndTime.value = value
    }

    fun setEmpolyeeVacationFrom(value: String) {
        empolyeeVacationFrom.value = value
    }

    fun setEmpolyeeVacationTo(value: String) {
        empolyeeVacationTo.value = value
    }

    /*
    * Set notification message
       *
     */
    private fun setNotificationTimes(profile: ProfilePreferences) {
        notificationSendStartTime.value = profile.notificationSendStartTime
        notificationSendEndTime.value = profile.notificationSendEndTime
    }

    /**
     * Set new empolyee
     */

    fun addNewEmployee() {
        val name = empolyeeName.value
        val surname = empolyeeSurname.value

        if (name.isNullOrEmpty() || surname.isNullOrEmpty()) {
            // Obsługa przypadku, gdy dane są niekompletne
            return
        }

        val ws = empolyeeWorkStartTime.value?.trim().orEmpty().ifBlank { Employee.DEFAULT_WORK_START }
        val we = empolyeeWorkEndTime.value?.trim().orEmpty().ifBlank { Employee.DEFAULT_WORK_END }
        if (!isValidWorkWindow(ws, we)) {
            setMessages("Godzina zakończenia pracy musi być późniejsza niż rozpoczęcia")
            return
        }

        validateVacationOrNull()?.let {
            setMessages(it)
            return
        }
        val vf = empolyeeVacationFrom.value?.trim().orEmpty()
        val vt = empolyeeVacationTo.value?.trim().orEmpty()

        // Generujemy ID dla nowego pracownika
        val newEmployee = Employee().apply {
            this.id = (empoleesList.value?.maxOfOrNull { it.id ?: 0 } ?: 0) + 1
            this.name = name
            this.surname = surname
            this.workStartTime = ws
            this.workEndTime = we
            this.vacationFrom = vf
            this.vacationTo = vt
        }

        empoleesList.value?.add(newEmployee)

        FirebaseEmployeeFunctions().addEmployeeToFirebase(
            firebaseDatabase = FirebaseDatabase.getInstance(), employee = newEmployee
        ) { success ->
            if (success) {
                setMessages("Pracownik ${newEmployee.name} został dodany")
            } else {
                setMessages("Nie udało się dodać pracownika")
            }
        }

        saveAllData()
    }


    // Funkcja do odczytu danych z Firebase
    private fun loadProfilePreferencesFromFirebase(firebaseDatabase: FirebaseDatabase) {
        viewModelScope.launch {
            try {
                val profile =
                    FirebaseProfilePreferences().loadProfilePreferencesFromFirebase(firebaseDatabase)
                profilePreferences.value = profile
                setNotificationTimes(profile)
                setNewProfileName(profile.userName)
                setAutomaticNotificationViewState(profile.notificationSendAutomatic)
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to load ProfilePreferences", e)
                viewState.value = AppState.Error
            }
        }
    }


    fun deleteEmpolyee(value: Employee) {
        empoleesList.value?.remove(value)
        value.id?.let {
            FirebaseEmployeeFunctions().deleteEmployeeFromFirebase(
                firebaseDatabase = FirebaseDatabase.getInstance(),
                it,
                completion = { succes ->

                    if (succes) {
                        setMessages("Pracownik usunięty")
                    } else {
                        setMessages("Nie udało się usunąć pracownika")
                    }
                })
        }
        saveAllData()
    }

    fun editEmployee(value: Employee) {
        val ws = (empolyeeWorkStartTime.value ?: value.workStartTime).trim()
            .ifBlank { Employee.DEFAULT_WORK_START }
        val we = (empolyeeWorkEndTime.value ?: value.workEndTime).trim()
            .ifBlank { Employee.DEFAULT_WORK_END }
        if (!isValidWorkWindow(ws, we)) {
            setMessages("Godzina zakończenia pracy musi być późniejsza niż rozpoczęcia")
            return
        }

        validateVacationOrNull()?.let {
            setMessages(it)
            return
        }
        val vf = empolyeeVacationFrom.value?.trim().orEmpty()
        val vt = empolyeeVacationTo.value?.trim().orEmpty()

        val updatedEmployee = Employee().apply {
            this.id = value.id
            this.name = empolyeeName.value ?: value.name
            this.surname = empolyeeSurname.value ?: value.surname
            this.workStartTime = ws
            this.workEndTime = we
            this.vacationFrom = vf
            this.vacationTo = vt
        }

        FirebaseEmployeeFunctions().editEmployeeInFirebase(firebaseDatabase = FirebaseDatabase.getInstance(),
            updatedEmployee = updatedEmployee,
            completion = { success ->
                if (success) {
                    setMessages("Pracownik ${updatedEmployee.name} został edytowany")
                    loadEmployeesListFromFirebase()
                } else {
                    setMessages("Nie udało się edytować pracownika")
                }
            })
    }


    fun setAutomaticNotificationViewState(value: Boolean) {
        notificationSendAutomatic.value = value
    }

    fun setNotificationViewState() {
        notificationViewState.value = !notificationViewState.value!!
    }

    fun setBackupViewState() {
        backupViewState.value = !(backupViewState.value ?: false)
    }

    fun backupDatabaseToStorage() {
        if (backupInProgress.value == true) return
        viewModelScope.launch {
            backupInProgress.value = true
            try {
                val result = FirebaseRealtimeDatabaseBackup.exportFullDatabaseJsonToStorage()
                if (result.success) {
                    setMessages("Kopia zapisana w Storage: ${result.storagePath}")
                } else {
                    setMessages("Błąd kopii zapasowej: ${result.errorMessage ?: "nieznany"}")
                }
            } finally {
                backupInProgress.value = false
            }
        }
    }

    fun setBackButtonViewState() {
        backButtonViewState.value = !backButtonViewState.value!!
    }

    fun setUpdateViewState() {
        updateViewState.value = !(updateViewState.value ?: false)
    }

    private fun setViewState(state: AppState) {
        viewState.value = state
    }

    private fun setMessages(message: String) {
        messages.value = message
    }


    fun setNotificationSendStartTime(notificationSendStartTime: String) {
        this.notificationSendStartTime.value = notificationSendStartTime
    }

    fun setNotificationSendEndTime(notificationSendEndTime: String) {
        this.notificationSendEndTime.value = notificationSendEndTime
    }

    fun closeAllStates() {
        profileViewState.value = false
        notificationViewState.value = false
        greetingsViewState.value = false
        backButtonViewState.value = false
        updateViewState.value = false
        empolyeeViewState.value = false
        backupViewState.value = false
    }


    fun loadEmployeesListFromFirebase() {
        try {
            viewModelScope.launch {
                val employees = FirebaseEmployeeFunctions().loadEmployeesFromFirebase(
                    FirebaseDatabase.getInstance()
                )
                empoleesList.value = employees.toMutableList()
            }
        } catch (e: Exception) {
            setMessages("Błąd podczas ładowania listy pracowników: ${e.message}")
        }
    }


    fun loadAllData() {
        setViewState(AppState.Loading)
        loadProfilePreferencesFromFirebase(FirebaseDatabase.getInstance())
        loadEmployeesListFromFirebase()

        setViewState(AppState.Success)
    }

    fun saveAllData() {
        setViewState(AppState.Loading)

        val preferences = ProfilePreferences(
            userName = profileName.value ?: "",
            notificationSendStartTime = notificationSendStartTime.value ?: "",
            notificationSendEndTime = notificationSendEndTime.value ?: "",
            greetingsLists = greetingsLists.value ?: mutableListOf(),
            notificationSendAutomatic = notificationSendAutomatic.value ?: false,
        )

        //fileFunctionsSettings.saveSettingsToFile(context = context, preferences = preferences)
        // profilePreferences.value = fileFunctionsSettings.loadSettingsFromFile(context = context)

        setMessages("Zapisano zmiany")

        FirebaseProfilePreferences().saveProfilePreferencesToFirebase(
            FirebaseDatabase.getInstance(), preferences
        ) { success ->

            if (success) {
                Log.i("App", "ProfilePreferences saved!")
            } else {
                Log.e("App", "Failed to save ProfilePreferences.")
            }
        }

        Log.e("save settings empolyee", empolyeeName.toString())
        setViewState(AppState.Success)
    }


    fun clearMessages() {
        messages.value = ""
    }

    private fun isValidWorkWindow(start: String, end: String): Boolean {
        return try {
            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            val a = LocalTime.parse(start, fmt)
            val b = LocalTime.parse(end, fmt)
            b.isAfter(a)
        } catch (_: Exception) {
            false
        }
    }

    /** Komunikat błędu lub null gdy OK / brak urlopu. */
    private fun validateVacationOrNull(): String? {
        val from = empolyeeVacationFrom.value?.trim().orEmpty()
        val to = empolyeeVacationTo.value?.trim().orEmpty()
        if (from.isEmpty() && to.isEmpty()) return null
        if (from.isEmpty()) return "Uzupełnij datę początku urlopu"
        val fd = parseEmployeeDisplayDate(from) ?: return "Niepoprawna data początku urlopu (dd.MM.yyyy)"
        if (to.isEmpty()) return null
        val td = parseEmployeeDisplayDate(to) ?: return "Niepoprawna data końca urlopu (dd.MM.yyyy)"
        return if (td.isBefore(fd)) {
            "Koniec urlopu nie może być wcześniejszy niż początek"
        } else {
            null
        }
    }

}