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
    val userName = MutableLiveData("")
    val notificationSendStartTime = MutableLiveData("")
    val notificationSendEndTime = MutableLiveData("")
    val greetingsLists = MutableLiveData<MutableList<String>>(mutableListOf())
    val notificationMessage = MutableLiveData("")


    fun setViewState(state: AppState) {
        viewState.value = state
    }

    fun setMessages(message: String) {
        messages.value = message
    }

    fun setUserName(userName: String) {
        this.userName.value = userName
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


    fun loadAllData(context: Context) {
        setViewState(AppState.Loading)

        fileFunctionsSettings.loadSettingsFromFile(context = context).let {
            userName.value = it.userName
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
            userName = userName.value ?: "",
            notificationSendStartTime = notificationSendStartTime.value ?: "",
            notificationSendEndTime = notificationSendEndTime.value ?: "",
            greetingsLists = greetingsLists.value ?: mutableListOf(),
            notificationMessage = notificationMessage.value ?: ""
        )
        fileFunctionsSettings.saveSettingsToFile(context = context, preferences = preferences)
        setViewState(AppState.Success)
    }


}