package com.strefagentelmena.models.SettngsModel

class Preferences(
    var userName: String = "Kinga",
    var notificationSendStartTime: String = "10:00",
    var notificationSendEndTime: String = "22:30",
    var greetingsLists: MutableList<String> = mutableListOf(),
    var notificationMessage: String = "Dzien dobry!",
)