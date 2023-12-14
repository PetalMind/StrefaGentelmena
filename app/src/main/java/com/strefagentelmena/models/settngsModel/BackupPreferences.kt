package com.strefagentelmena.models.settngsModel

class BackupPreferences(
    val isBackupCreated: Boolean = false,
    val lastestBackupDate: String = "",
    val backupCustom: Boolean = false,
    val backupAutomatic: Boolean = false,
    val backupCustomers: Boolean = false,
    val backupAppoiments: Boolean = false,
)