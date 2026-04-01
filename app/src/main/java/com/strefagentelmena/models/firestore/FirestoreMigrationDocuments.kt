package com.strefagentelmena.models.firestore

import com.google.firebase.firestore.FieldValue
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.VisitSummary
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.effectiveEmployeeId
import com.strefagentelmena.models.appoimentsModel.timelineWorkerDisplayName
import com.strefagentelmena.models.effectiveCustomerId
import com.strefagentelmena.models.normalizedAfterFirebaseLoad
import com.strefagentelmena.models.normalizedForFirebaseWrite
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Docelowe kształty dokumentów po migracji RTDB → Firestore (`schemaVersion` = [SCHEMA_VERSION]).
 *
 * **Zalecane indeksy złożone** (konsola Firebase → Firestore → Indeksy):
 * - `appointments`: `employeeId` ↑, `dateSortKey` ↑, `startMinuteOfDay` ↑
 * - `appointments`: `customerId` ↑, `dateSortKey` ↑, `startMinuteOfDay` ↑
 * - `customers`: `sortKeyLastFirst` ↑
 * - `employees`: `sortKey` ↑
 *
 * Pola pomocnicze:
 * - [appointmentToFirestoreMap] — bez pełnego zagnieżdżonego klienta; tylko `customerId` + denormalizacja nazw.
 * - [customerToFirestoreMap] — bez legacy [Customer.appointment]; ostatnia wizyta w mapie `lastVisit`.
 * - `dateSortKey` — ISO `yyyy-MM-dd` do sortowania i zakresów (obok `dateDisplay` z UI).
 */
const val SCHEMA_VERSION = 1L

private val DATE_DISPLAY_FORMATS = listOf(
    DateTimeFormatter.ofPattern("dd.MM.uuuu"),
    DateTimeFormatter.ofPattern("d.M.uuuu"),
)

/** Konwersja `dd.MM.yyyy` / `d.M.yyyy` → `yyyy-MM-dd` dla zapytań; null gdy nie da się sparsować. */
fun displayDateToSortKey(display: String): String? {
    val s = display.trim()
    if (s.isEmpty()) return null
    for (fmt in DATE_DISPLAY_FORMATS) {
        try {
            return LocalDate.parse(s, fmt).format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: DateTimeParseException) {
            // next pattern
        }
    }
    return null
}

fun timeDisplayToMinuteOfDay(time: String): Int? {
    val parts = time.trim().split(':')
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..47 || m !in 0..59) return null
    return h * 60 + m
}

private fun visitSummaryToMap(v: VisitSummary): Map<String, Any> = buildMap {
    put("appointmentId", v.appointmentId.toLong())
    put("date", v.date)
    put("startTime", v.startTime)
    put("endTime", v.endTime)
    put("employeeId", (v.employeeId ?: 0).toLong())
    put("employeeName", v.employeeName)
    put("employeeSurname", v.employeeSurname)
}

fun customerToFirestoreMap(customer: Customer): Map<String, Any> {
    val c = customer.normalizedAfterFirebaseLoad()
    return buildMap {
        put("schemaVersion", SCHEMA_VERSION)
        put("migratedAt", FieldValue.serverTimestamp())
        put("id", c.id.toLong())
        put("firstName", c.firstName)
        put("lastName", c.lastName)
        put("phoneNumber", c.phoneNumber)
        put("email", c.email)
        put("noted", c.noted)
        put("visitCount", c.visitCount.toLong())
        put("avgWeeksBetweenVisits", c.avgWeeksBetweenVisits)
        val full = "${c.firstName} ${c.lastName}".trim()
        put("fullNameSearch", full.lowercase())
        put(
            "sortKeyLastFirst",
            "${c.lastName.trim().lowercase()} ${c.firstName.trim().lowercase()}".trim(),
        )
        c.lastVisit?.let { put("lastVisit", visitSummaryToMap(it)) }
    }
}

fun appointmentToFirestoreMap(appointment: Appointment): Map<String, Any> {
    val a = appointment.normalizedAfterFirebaseLoad().normalizedForFirebaseWrite()
    val customerId = a.effectiveCustomerId().toLong()
    val empId = a.effectiveEmployeeId()
    return buildMap {
        put("schemaVersion", SCHEMA_VERSION)
        put("migratedAt", FieldValue.serverTimestamp())
        put("id", a.id.toLong())
        put("customerId", customerId)
        put("dateDisplay", a.date)
        displayDateToSortKey(a.date)?.let { put("dateSortKey", it) }
        put("startTime", a.startTime)
        put("endTime", a.endTime)
        timeDisplayToMinuteOfDay(a.startTime)?.let { put("startMinuteOfDay", it.toLong()) }
        timeDisplayToMinuteOfDay(a.endTime)?.let { put("endMinuteOfDay", it.toLong()) }
        put("serviceDescription", a.serviceDescription)
        put("notificationSent", a.notificationSent)
        if (empId != null) put("employeeId", empId.toLong())
        put("customerDisplayName", a.customer.fullName.trim())
        put("employeeDisplayName", a.timelineWorkerDisplayName())
    }
}

fun employeeToFirestoreMap(employee: Employee): Map<String, Any> {
    val id = employee.id?.toLong() ?: 0L
    return buildMap {
        put("schemaVersion", SCHEMA_VERSION)
        put("migratedAt", FieldValue.serverTimestamp())
        put("id", id)
        put("name", employee.name)
        put("surname", employee.surname)
        put("displayName", "${employee.name} ${employee.surname}".trim())
        put(
            "sortKey",
            "${employee.surname.trim().lowercase()} ${employee.name.trim().lowercase()}".trim(),
        )
        put("vacationFrom", employee.vacationFrom)
        put("vacationTo", employee.vacationTo)
        put("vacationTimeFrom", employee.vacationTimeFrom)
        put("vacationTimeTo", employee.vacationTimeTo)
    }
}

fun profilePreferencesToFirestoreMap(prefs: ProfilePreferences): Map<String, Any> =
    mapOf(
        "schemaVersion" to SCHEMA_VERSION,
        "migratedAt" to FieldValue.serverTimestamp(),
        "userName" to prefs.userName,
        "notificationSendStartTime" to prefs.notificationSendStartTime,
        "notificationSendEndTime" to prefs.notificationSendEndTime,
        "greetingsLists" to prefs.greetingsLists.toList(),
        "notificationSendAutomatic" to prefs.notificationSendAutomatic,
    )
