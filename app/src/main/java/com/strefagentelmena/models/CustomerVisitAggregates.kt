package com.strefagentelmena.models

import com.strefagentelmena.models.appoimentsModel.Appointment
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val appointmentDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy")

/**
 * Agregaty wizyt klienta liczone z węzła [Appointments] (źródło prawdy).
 * Zapisywane też denormalizacyjnie przy kliencie pod listę / profil.
 */
data class CustomerVisitAggregates(
    val visitCount: Int,
    val avgWeeksBetweenVisits: Double,
    val lastVisit: VisitSummary?,
    /** Ostatnia wizyta w formie „płaskiej” pod legacy [Customer.appointment]. */
    val latestSlimAppointment: Appointment?,
)

fun computeCustomerVisitAggregates(
    appointments: List<Appointment>,
    customerId: Int,
): CustomerVisitAggregates {
    val mine = appointments
        .filter { it.effectiveCustomerId() == customerId }
        .sortedWith(::compareAppointmentsChronological)

    if (mine.isEmpty()) {
        return CustomerVisitAggregates(
            visitCount = 0,
            avgWeeksBetweenVisits = 0.0,
            lastVisit = null,
            latestSlimAppointment = null,
        )
    }

    val avgWeeks = averageWeeksBetweenSortedVisits(mine)

    val latest = mine.last()
    val summary = VisitSummary.fromAppointment(latest)
    val slim = latest
        .copy(customerId = customerId, customer = latest.customer.toEmbeddedInAppointment())
        .normalizedForFirebaseWrite()

    return CustomerVisitAggregates(
        visitCount = mine.size,
        avgWeeksBetweenVisits = avgWeeks,
        lastVisit = summary,
        latestSlimAppointment = slim,
    )
}

/** Uzupełnia pola wizytowe klienta na podstawie harmonogramu (bez zapisu do Firebase). */
fun Customer.withVisitAggregatesFromAppointments(appointments: List<Appointment>): Customer {
    val agg = computeCustomerVisitAggregates(appointments, id)
    return copy(
        visitCount = agg.visitCount,
        avgWeeksBetweenVisits = agg.avgWeeksBetweenVisits,
        lastVisit = agg.lastVisit,
        appointment = agg.latestSlimAppointment,
    )
}

fun mergeCustomersWithVisitStats(
    customers: List<Customer>,
    appointments: List<Appointment>,
): List<Customer> = customers.map { it.withVisitAggregatesFromAppointments(appointments) }

private fun compareAppointmentsChronological(a: Appointment, b: Appointment): Int {
    val dateA = runCatching { LocalDate.parse(a.date, appointmentDateFormatter) }.getOrNull()
    val dateB = runCatching { LocalDate.parse(b.date, appointmentDateFormatter) }.getOrNull()
    when {
        dateA != null && dateB != null -> {
            val c = dateA.compareTo(dateB)
            if (c != 0) return c
        }

        dateA != null -> return -1
        dateB != null -> return 1
    }
    val timeCmp = a.startTime.compareTo(b.startTime)
    if (timeCmp != 0) return timeCmp
    return a.id.compareTo(b.id)
}

private fun averageWeeksBetweenSortedVisits(sortedAsc: List<Appointment>): Double {
    if (sortedAsc.size < 2) return 0.0
    val weeks = sortedAsc.zipWithNext { prev, next ->
        val d0 = runCatching { LocalDate.parse(prev.date, appointmentDateFormatter) }.getOrNull()
        val d1 = runCatching { LocalDate.parse(next.date, appointmentDateFormatter) }.getOrNull()
        if (d0 != null && d1 != null) ChronoUnit.WEEKS.between(d0, d1).toDouble()
        else 0.0
    }
    return weeks.average()
}
