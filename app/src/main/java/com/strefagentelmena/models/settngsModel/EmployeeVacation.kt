package com.strefagentelmena.models.settngsModel

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val DISPLAY_DATE_FORMATS = listOf(
    DateTimeFormatter.ofPattern("dd.MM.uuuu"),
    DateTimeFormatter.ofPattern("d.M.uuuu"),
)

fun parseEmployeeDisplayDate(display: String): LocalDate? {
    val s = display.trim()
    if (s.isEmpty()) return null
    for (fmt in DISPLAY_DATE_FORMATS) {
        try {
            return LocalDate.parse(s, fmt)
        } catch (_: DateTimeParseException) {
            // next
        }
    }
    return null
}

/**
 * Zakres urlopu włącznie; pusty początek = brak urlopu.
 * Pusty koniec przy ustawionym początku = jeden dzień.
 */
fun Employee.vacationRangeOrNull(): ClosedRange<LocalDate>? {
    val fromStr = vacationFrom.trim()
    if (fromStr.isEmpty()) return null
    val from = parseEmployeeDisplayDate(fromStr) ?: return null
    val toStr = vacationTo.trim()
    val to = if (toStr.isEmpty()) from else (parseEmployeeDisplayDate(toStr) ?: from)
    return if (to.isBefore(from)) null else from..to
}

fun Employee.isOnVacationOn(appointmentDateDisplay: String): Boolean {
    val range = vacationRangeOrNull() ?: return false
    val day = parseEmployeeDisplayDate(appointmentDateDisplay) ?: return false
    return day in range
}

fun Employee.vacationRangeLabel(): String {
    val range = vacationRangeOrNull() ?: return ""
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val a = range.start.format(fmt)
    val b = range.endInclusive.format(fmt)
    return if (a == b) a else "$a – $b"
}
