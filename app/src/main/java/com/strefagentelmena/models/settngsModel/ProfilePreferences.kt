package com.strefagentelmena.models.settngsModel

class ProfilePreferences(
    var userName: String = "",
    var notificationSendStartTime: String = "08:00",
    var notificationSendEndTime: String = "21:00",
    var greetingsLists: MutableList<String> = mutableListOf(),
    var notificationSendAutomatic: Boolean = false,
)