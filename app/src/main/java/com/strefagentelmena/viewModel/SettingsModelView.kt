package com.strefagentelmena.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fileFuctions.fileFunctionsSettings
import com.strefagentelmena.models.SettngsModel.Preferences

class SettingsModelView : ViewModel() {
    val viewState = MutableLiveData<AppState>(AppState.Idle)
    val messages = MutableLiveData("")
    val profileName = MutableLiveData("")
    val notificationSendStartTime = MutableLiveData("")
    val notificationSendEndTime = MutableLiveData("")
    val greetingsLists = MutableLiveData<MutableList<String>>(mutableListOf())
    val notificationMessage =
        MutableLiveData("Przypominamy o wizycie w dniu {data wizyty} o godzinie {godzina ropozczęcia} w Strefie Gentlemana Kinga Kloss, adres: Łaska 4, Zduńska Wola.")

    val profileViewState = MutableLiveData<Boolean>(false)
    val notificationViewState = MutableLiveData<Boolean>(false)
    val greetingsViewState = MutableLiveData<Boolean>(false)
    val backButtonViewState = MutableLiveData<Boolean>(false)
    val updateViewState = MutableLiveData<Boolean>(false)

    fun setProfileViewState() {
        profileViewState.value = !profileViewState.value!!
    }

    fun setNotificationViewState() {
        notificationViewState.value = !notificationViewState.value!!
    }

    fun setGreetingsViewState() {
        greetingsViewState.value = !greetingsViewState.value!!
    }

    fun setBackButtonViewState() {
        backButtonViewState.value = !backButtonViewState.value!!
    }

    fun setUpdateViewState() {
        updateViewState.value = !updateViewState.value!!
    }

    fun setViewState(state: AppState) {
        viewState.value = state
    }

    fun setMessages(message: String) {
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

    fun setGreetingsLists(greetingsLists: MutableList<String>) {
        this.greetingsLists.value = greetingsLists
    }

    fun setNotificationMessage(notificationMessage: String) {
        this.notificationMessage.value = notificationMessage
    }

    fun closeAllStates(){
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
            notificationMessage.value = it.notificationMessage
        }

        setViewState(AppState.Success)
    }

    fun saveAllData(context: Context) {
        setViewState(AppState.Loading)
        val preferences = Preferences(
            userName = profileName.value ?: "",
            notificationSendStartTime = notificationSendStartTime.value ?: "",
            notificationSendEndTime = notificationSendEndTime.value ?: "",
            greetingsLists = greetingsLists.value ?: mutableListOf(),
            notificationMessage = notificationMessage.value ?: ""
        )

        fileFunctionsSettings.saveSettingsToFile(context = context, preferences = preferences)
        fileFunctionsSettings.loadSettingsFromFile(context = context)

        setViewState(AppState.Success)
    }


}