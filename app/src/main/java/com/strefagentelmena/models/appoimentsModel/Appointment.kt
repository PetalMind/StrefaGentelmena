package com.strefagentelmena.models.appoimentsModel

import com.strefagentelmena.models.Customer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Appointment(
    var id: Int = 0,
    var customer: Customer = Customer(),
    var date: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
    var startTime: LocalTime = LocalTime.now(),
    var endTime: LocalTime = LocalTime.now().plusHours(1), // or whatever your default end time is
    var notificationSent: Boolean = false,
) {
    private fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        return date.format(formatter)
    }

    fun copy(
        id: Int = this.id,
        customer: Customer = this.customer,
        date: String = this.getFormattedDate(),
        startTime: LocalTime = this.startTime,
        endTime: LocalTime = this.endTime,
        notificationSent: Boolean = this.notificationSent
    ): Appointment {
        return Appointment(
            id = id,
            customer = customer,
            date = date,
            startTime = startTime,
            endTime = endTime,
            notificationSent = notificationSent
        )
    }
}

