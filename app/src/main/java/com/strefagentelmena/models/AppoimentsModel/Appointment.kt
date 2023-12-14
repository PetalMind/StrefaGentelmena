package com.strefagentelmena.models.AppoimentsModel

import android.os.Build
import androidx.annotation.RequiresApi
import com.strefagentelmena.models.Customer
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Appointment(
    var id: Int = 0,
    var customer: Customer = Customer(),
    var date: String = "",
    var startTime: String = "",
    var endTime: String = calculateEndTime(startTime),
    var notificationSent: Boolean = false,
) {
    fun copy(
        id: Int = this.id,
        customer: Customer = this.customer,
        date: String = this.date,
        startTime: String = this.startTime,
        endTime: String = this.endTime,
        notificationSent: Boolean = this.notificationSent
    ): Appointment {
        return Appointment(id, customer, date, startTime, endTime, notificationSent)
    }

    companion object {
        fun calculateEndTime(startTime: String): String {
            if (startTime.isEmpty()) {
                return ""
            } else {
                val start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
                val end = start.plusHours(1)
                return end.format(DateTimeFormatter.ofPattern("HH:mm"))
            }
        }
    }
}

