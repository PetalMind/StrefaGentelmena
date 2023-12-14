package com.strefagentelmena.models.AppoimentsModel

import android.os.Build
import androidx.annotation.RequiresApi
import com.strefagentelmena.models.Customer
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Appointment (
    var id: Int = 0,
    var customer: Customer = Customer(),
    var date: String = "",
    var startTime: String = "",
    var notificationSent: Boolean = false,
) {
    companion object {
        fun calculateEndTime(startTime: String): String {
            val start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            val end = start.plusHours(1)
            return end.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }
}
