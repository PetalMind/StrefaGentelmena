package com.strefagentelmena.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fireBase.FirebaseEmployeeFunctions
import com.strefagentelmena.functions.fireBase.FirebaseProfilePreferences
import com.strefagentelmena.models.settngsModel.BackupSettings
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import kotlinx.coroutines.launch

class SettingsModelView : ViewModel() {
    val viewState = MutableLiveData(AppState.Idle)
    val messages = MutableLiveData("")
    val profilePreferences = MutableLiveData(ProfilePreferences())
    val profileName = MutableLiveData("")
    private val selectedEmployee = MutableLiveData(Employee())
    val newEmployee = MutableLiveData(Employee())
    val empolyeeName = MutableLiveData("")
    val empolyeeSurname = MutableLiveData("")
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

    fun clearNewEmpolyee() {
        newEmployee.value = Employee()
        empolyeeName.value = ""
        empolyeeSurname.value = ""
    }

    fun setNewProfileName(value: String) {
        profileName.value = value
    }


    /**
     * Set selected empolyee
     */
    fun setSlectedEmpolyee(value: Employee) {
        selectedEmployee.value = value
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

    fun setNewEmployee(context: Context) {
        val name = empolyeeName.value
        val surname = empolyeeSurname.value

        if (name.isNullOrEmpty() || surname.isNullOrEmpty()) {
            // Obsługa przypadku, gdy dane są niekompletne
            return
        }

        // Generujemy ID dla nowego pracownika
        val newEmployee = Employee().apply {
            this.id = (empoleesList.value?.maxOfOrNull { it.id ?: 0 } ?: 0) + 1
            this.name = name
            this.surname = surname
        }

        empoleesList.value?.add(newEmployee)

        FirebaseEmployeeFunctions().addEmployeeToFirebase(
            firebaseDatabase = FirebaseDatabase.getInstance(),
            employee = newEmployee
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
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to load ProfilePreferences", e)
                viewState.value = AppState.Error
            }
        }
    }


    fun deleteEmpolyee(value: Employee) {
        empoleesList.value?.remove(value)
        value.id?.let {
            FirebaseEmployeeFunctions().deleteEmployeeFromFirebase(firebaseDatabase = FirebaseDatabase.getInstance(),
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


    fun setAutomaticNotificationViewState(value: Boolean) {
        notificationSendAutomatic.value = value
    }

    fun setNotificationViewState() {
        notificationViewState.value = !notificationViewState.value!!
    }

    fun setBackButtonViewState() {
        backButtonViewState.value = !backButtonViewState.value!!
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
    }


    fun loadEmployeesListFromFirebase() {
        try {
            viewModelScope.launch {
                val employees =
                    FirebaseEmployeeFunctions().loadEmployeesFromFirebase(FirebaseDatabase.getInstance())
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


}