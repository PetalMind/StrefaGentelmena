package com.strefagentelmena.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fileFuctions.backupFilesFunctions
import com.strefagentelmena.functions.fileFuctions.fileFunctionsSettings
import com.strefagentelmena.models.settngsModel.BackupPreferences
import com.strefagentelmena.models.settngsModel.ProfilePreferences

class SettingsModelView : ViewModel() {
    val viewState = MutableLiveData<AppState>(AppState.Idle)
    val messages = MutableLiveData("")
    val backupPrefecences = MutableLiveData(BackupPreferences())
    val profilePreferences = MutableLiveData(ProfilePreferences())
    val profileName = MutableLiveData("")
    val notificationSendStartTime = MutableLiveData("")
    val notificationSendEndTime = MutableLiveData("")
    val notificationSendAutomatic = MutableLiveData(false)
    val isBackupCreated = MutableLiveData(false)
    val backupCustom = MutableLiveData(false)
    val backupAutomatic = MutableLiveData(false)
    val backupCustomers = MutableLiveData(false)
    val backupAppoiments = MutableLiveData(false)
    val backupDate = MutableLiveData("")

    private val greetingsLists = MutableLiveData<MutableList<String>>(mutableListOf())

    private val notificationMessage =
        MutableLiveData("Przypominamy o wizycie w dniu {data wizyty} o godzinie {godzina ropozczęcia} w Strefie Gentlemana Kinga Kloss, adres: Łaska 4, Zduńska Wola.")

    val profileViewState = MutableLiveData<Boolean>(false)
    val notificationViewState = MutableLiveData<Boolean>(false)
    val greetingsViewState = MutableLiveData(false)
    val backButtonViewState = MutableLiveData<Boolean>(false)
    val updateViewState = MutableLiveData<Boolean>(false)

    fun setProfileViewState() {
        profileViewState.value = !profileViewState.value!!
    }

    private fun setProfilePreferences(): ProfilePreferences {
        // Sprawdź, czy wartości nie są null, zanim utworzysz obiekt ProfilePreferences
        val userName = profileName.value ?: "Kinga"
        val startTime = notificationSendStartTime.value
            ?: "7:30"
        val endTime = notificationSendEndTime.value
            ?: "21:00"
        val greetings = greetingsLists.value ?: mutableListOf()
        val sendAutomatic = notificationSendAutomatic.value
            ?: false
        val backupPreferences = backupPrefecences.value
            ?: BackupPreferences()

        // Utwórz i zwróć obiekt ProfilePreferences z wartościami
        val loadedProfilePreferences = ProfilePreferences(
            userName = userName,
            notificationSendStartTime = startTime,
            notificationSendEndTime = endTime,
            greetingsLists = greetings,
            notificationSendAutomatic = sendAutomatic,
            backupPreferences = backupPreferences
        )

        // Zaktualizuj MutableState, jeśli to konieczne
        profilePreferences.value = loadedProfilePreferences

        return loadedProfilePreferences
    }


    fun setCustomBackupViewState(value: Boolean) {
        backupCustom.value = value
    }

    fun setBackupCreatedViewState(value: Boolean) {
        isBackupCreated.value = value
    }

    fun setAutomaticBackupViewState(value: Boolean) {
        backupAutomatic.value = value
    }

    fun setBackupDate(backupDate: String) {
        this.backupDate.value = backupDate
    }

    fun setCustomersBackupViewState(value: Boolean) {
        backupCustomers.value = value
    }

    fun setAppoimentsBackupViewState(value: Boolean) {
        backupAppoiments.value = value
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

    fun setUpdateViewState() {
        updateViewState.value = !updateViewState.value!!
    }

    private fun setViewState(state: AppState) {
        viewState.value = state
    }

    private fun setMessages(message: String) {
        messages.value = message
    }

    fun setUserName(userName: String) {
        this.profileName.value = userName
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
    }


    fun loadAllData(context: Context) {
        setViewState(AppState.Loading)

        fileFunctionsSettings.loadSettingsFromFile(context = context).let {
            profileName.value = it.userName
            notificationSendStartTime.value = it.notificationSendStartTime
            notificationSendEndTime.value = it.notificationSendEndTime
            greetingsLists.value = it.greetingsLists
            notificationSendAutomatic.value = it.notificationSendAutomatic
            backupPrefecences.value = it.backupPreferences
        }
        profilePreferences.value = setProfilePreferences()
        setViewState(AppState.Success)
    }

    fun saveAllData(context: Context) {
        setViewState(AppState.Loading)

        val preferences = ProfilePreferences(
            userName = profileName.value ?: "",
            notificationSendStartTime = notificationSendStartTime.value ?: "",
            notificationSendEndTime = notificationSendEndTime.value ?: "",
            greetingsLists = greetingsLists.value ?: mutableListOf(),
            notificationSendAutomatic = notificationSendAutomatic.value ?: false,
            backupPreferences = backupPrefecences.value ?: BackupPreferences()
        )

        fileFunctionsSettings.saveSettingsToFile(context = context, preferences = preferences)
        profilePreferences.value = fileFunctionsSettings.loadSettingsFromFile(context = context)

        setMessages("Zapisano zmiany")
        setViewState(AppState.Success)
    }

    fun createBackup(context: Context) {
        setViewState(AppState.Loading)

        val preferences = BackupPreferences(
            isBackupCreated = isBackupCreated.value ?: false,
            backupCustom = backupCustom.value ?: false,
            backupAutomatic = backupAutomatic.value ?: false,
            backupCustomers = backupCustomers.value ?: false,
            backupAppoiments = backupAppoiments.value ?: false
        )

        backupPrefecences.value = preferences
        profilePreferences.value?.backupPreferences = preferences

        if (backupFilesFunctions.createBackupFile(context = context)) {
            setMessages("Utworzono kopę zapasowa")
        } else {
            setMessages("Nie udało się utworzenie kopii zapasowej")
        }

        setViewState(AppState.Idle)
    }

    fun loadBackup(context: Context) {
        setViewState(AppState.Loading)
        if (backupFilesFunctions.readBackupFile(context = context)) {
            setMessages("Przywrócono kopię zapasową")
        } else {
            setMessages("Nie znaleziono kopii zapasowej")
        }

        setViewState(AppState.Idle)
    }

    fun clearMessages() {
        messages.value = ""
    }

}