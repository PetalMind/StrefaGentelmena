package com.strefagentelmena.models.settngsModel

import com.strefagentelmena.models.appoimentsModel.appointmentIntervalEndExclusiveMinutes
import com.strefagentelmena.models.appoimentsModel.minutesIntervalsOverlap
import com.strefagentelmena.models.appoimentsModel.parseAppointmentTimeString
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

/**
 * Czy w danym dniu pracownik jest **niedostępny cały dzień** (wykluczenie z listy bez ręcznego wyboru).
 * Przy urlopie z konkretnymi godzinami zwraca false — można go wybrać i planować poza tym oknem.
 */
fun Employee.isOnVacationOn(appointmentDateDisplay: String): Boolean {
    val range = vacationRangeOrNull() ?: return false
    val day = parseEmployeeDisplayDate(appointmentDateDisplay) ?: return false
    if (day !in range) return false
    return !hasPartialVacationHours()
}

/** Oba czasy urlopu ustawione i poprawne — wtedy wolne tylko w tym przedziale (każdego dnia w zakresie dat). */
fun Employee.hasPartialVacationHours(): Boolean {
    val a = vacationTimeFrom.trim()
    val b = vacationTimeTo.trim()
    if (a.isEmpty() || b.isEmpty()) return false
    val s = parseAppointmentTimeString(a) ?: return false
    val e = parseAppointmentTimeString(b) ?: return false
    val (t0, t1) = appointmentIntervalEndExclusiveMinutes(s, e)
    return t1 > t0
}

/**
 * Przedział niedostępności w minutach od północy [startMin, endExclusiveMin) dla danego dnia;
 * null jeśli ten dzień nie wpada w urlop/wolne.
 * Całodniowo: 0 .. 24h; częściowo: godziny z profilu pracownika.
 */
fun Employee.vacationAbsentIntervalMinutesOnDate(dateDisplay: String): Pair<Int, Int>? {
    val range = vacationRangeOrNull() ?: return null
    val day = parseEmployeeDisplayDate(dateDisplay) ?: return null
    if (day !in range) return null
    if (hasPartialVacationHours()) {
        val s = parseAppointmentTimeString(vacationTimeFrom.trim()) ?: return null
        val e = parseAppointmentTimeString(vacationTimeTo.trim()) ?: return null
        return appointmentIntervalEndExclusiveMinutes(s, e)
    }
    return 0 to 24 * 60
}

/** Czy wizyta nachodzi na urlop/wolne tego pracownika w podanym dniu. */
fun Employee.vacationBlocksAppointment(
    appointmentDateDisplay: String,
    appointmentStartTime: String,
    appointmentEndTime: String,
): Boolean {
    val absent = vacationAbsentIntervalMinutesOnDate(appointmentDateDisplay) ?: return false
    val s = parseAppointmentTimeString(appointmentStartTime) ?: return false
    val e = parseAppointmentTimeString(appointmentEndTime) ?: return false
    val (ap0, ap1) = appointmentIntervalEndExclusiveMinutes(s, e)
    return minutesIntervalsOverlap(ap0, ap1, absent.first, absent.second)
}

/** Krótka etykieta na oś czasu harmonogramu; null gdy brak blokady tego dnia. */
fun Employee.vacationTimelineLabelForDate(dateDisplay: String): String? {
    if (vacationAbsentIntervalMinutesOnDate(dateDisplay) == null) return null
    return if (hasPartialVacationHours()) {
        "Wolne / urlop ${vacationTimeFrom.trim()}–${vacationTimeTo.trim()}"
    } else {
        "Urlop / wolne (cały dzień)"
    }
}

fun Employee.vacationRangeLabel(): String {
    val range = vacationRangeOrNull() ?: return ""
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val a = range.start.format(fmt)
    val b = range.endInclusive.format(fmt)
    val datePart = if (a == b) a else "$a – $b"
    if (!hasPartialVacationHours()) return datePart
    val tf = DateTimeFormatter.ofPattern("HH:mm")
    val t1 = parseAppointmentTimeString(vacationTimeFrom.trim())?.format(tf) ?: return datePart
    val t2 = parseAppointmentTimeString(vacationTimeTo.trim())?.format(tf) ?: return datePart
    return "$datePart, $t1–$t2 (codziennie)"
}
