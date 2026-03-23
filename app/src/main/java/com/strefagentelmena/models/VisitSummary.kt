package com.strefagentelmena.models

import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.effectiveEmployeeId

/**
 * Lekki zapis ostatniej wizyty przy kliencie (bez zagnieżdżonego [Customer] w [Appointment]).
 * Stare wpisy w Firebase mogą go nie mieć — wtedy uzupełniamy z legacy [Customer.appointment].
 */
class VisitSummary(
    var appointmentId: Int = 0,
    var date: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var employeeId: Int? = null,
    var employeeName: String = "",
    var employeeSurname: String = "",
) {
    companion object {
        fun fromAppointment(appointment: Appointment): VisitSummary = VisitSummary(
            appointmentId = appointment.id,
            date = appointment.date,
            startTime = appointment.startTime,
            endTime = appointment.endTime,
            employeeId = appointment.effectiveEmployeeId(),
            employeeName = appointment.employee.name,
            employeeSurname = appointment.employee.surname,
        )
    }
}
