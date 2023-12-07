package com.strefagentelmena.models.AppoimentsModel

import android.os.Build
import androidx.annotation.RequiresApi
import com.strefagentelmena.models.Customer
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Appointment @RequiresApi(Build.VERSION_CODES.O) constructor(
    var id: Int,
    var customer: Customer,
    var date: String,
    var startTime: String,
    var notificationSent: Boolean,
) {
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun calculateEndTime(startTime: String): String {
            val start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            val end = start.plusHours(1)
            return end.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }
}
