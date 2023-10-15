package com.strefagentelmena.models

class Notification(
    val id: Int,
    val recipient: String,
    val message: String,
    val timestamp: String,
    var notificationSent: Boolean = false,
    var viwed: Boolean = false,
)
