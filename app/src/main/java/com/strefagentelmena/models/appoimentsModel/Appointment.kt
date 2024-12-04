package com.strefagentelmena.models.appoimentsModel

import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.settngsModel.Employee
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Appointment(
    var id: Int = 0,
    var customer: Customer = Customer(),
    var date: String = "", // Data jako String w formacie "dd.MM.yyyy"
    var startTime: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), // Czas jako String
    var endTime: String = LocalTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")), // Czas jako String
    var notificationSent: Boolean = false,
    var employee: Employee = Employee()
) {
    fun getStartTimeAsLocalTime(): LocalTime {
        return LocalTime.parse(startTime, TIME_FORMATTER)
    }

    fun getEndTimeAsLocalTime(): LocalTime {
        return LocalTime.parse(endTime, TIME_FORMATTER)
    }

    fun copy(
        id: Int = this.id,
        customer: Customer = this.customer,
        date: String = this.date,
        startTime: String = this.startTime,
        endTime: String = this.endTime,
        notificationSent: Boolean = this.notificationSent,
        employee: Employee = this.employee
    ): Appointment {
        return Appointment(
            id = id,
            customer = customer,
            date = date,
            startTime = startTime,
            endTime = endTime,
            notificationSent = notificationSent,
            employee = employee
        )
    }
    companion object {
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
