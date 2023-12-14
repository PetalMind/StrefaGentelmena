package com.strefagentelmena.models.settngsModel

class ProfilePreferences(
    var userName: String = "Kinga",
    var notificationSendStartTime: String = "07:30",
    var notificationSendEndTime: String = "22:00",
    var greetingsLists: MutableList<String> = mutableListOf(),
    var notificationSendAutomatic: Boolean = false,
    var backupPreferences: BackupPreferences = BackupPreferences(),
)