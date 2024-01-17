package com.strefagentelmena.models.settngsModel

class BackupSettings(
    val isBackupCreated: Boolean = false,
    val latestBackupDate: String = "",
    val isAutomaticBackupEnabled: Boolean = false,
    val hasOnlineCopy: Boolean = false,
    val isBackupOnline: Boolean = false
)