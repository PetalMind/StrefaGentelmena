package com.strefagentelmena.models.appoimentsModel

import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.settngsModel.Employee
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.max
import kotlin.math.min

/** Domyślna długość nowej wizyty przy tworzeniu (minuty od startu do sugerowanego końca). */
const val DEFAULT_APPOINTMENT_DURATION_MINUTES: Long = 30L

private val appointmentTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")

/**
 * Godziny z Firebase / starych wpisów: `09:00`, `9:00`, z sekundami, ISO.
 * Ścisłe `HH:mm` rzuca przy `9:00` lub `14:30:00` — stąd wcześniejsze crashe UI przy edycji.
 */
fun parseAppointmentTimeString(raw: String?): LocalTime? {
    val t = raw?.trim().orEmpty()
    if (t.isEmpty()) return null
    val formatters = listOf(
        appointmentTimeFormatter,
        DateTimeFormatter.ofPattern("H:mm"),
        DateTimeFormatter.ofPattern("HH:mm:ss"),
        DateTimeFormatter.ofPattern("H:mm:ss"),
    )
    for (f in formatters) {
        try {
            return LocalTime.parse(t, f)
        } catch (_: DateTimeParseException) {
        }
    }
    return try {
        LocalTime.parse(t)
    } catch (_: DateTimeParseException) {
        null
    }
}

/**
 * Półotwarty przedział [startMin, endExclusiveMin) w minutach od północy.
 * - Koniec wizyty o **08:00** = pierwsza wolna minuta 08:00 → kolejna wizyta może zaczynać o 08:00 (brak kolizji).
 * - Przejście przez północ: gdy godzina końca < godziny startu (np. 23:00–01:00), [end] jest następnego dnia.
 * - Gdy start == end **bez** przejścia przez północ — traktujemy jako brak okienka (nie rozciągamy na 24 h).
 */
internal fun appointmentIntervalEndExclusiveMinutes(start: LocalTime, end: LocalTime): Pair<Int, Int> {
    val startMin = start.hour * 60 + start.minute
    var endMin = end.hour * 60 + end.minute
    when {
        endMin == startMin -> return startMin to startMin
        endMin < startMin -> endMin += 24 * 60
    }
    return startMin to endMin
}

/**
 * Czy [start1, endExclusive1) i [start2, endExclusive2) mają **wspólny** fragment czasu.
 * Styk (koniec A == początek B, np. 7:00–8:00 i 8:00–9:00) **nie** jest kolizją.
 */
internal fun minutesIntervalsOverlap(
    start1: Int,
    endExclusive1: Int,
    start2: Int,
    endExclusive2: Int,
): Boolean {
    if (endExclusive1 <= start1 || endExclusive2 <= start2) return false
    return endExclusive1 > start2 && endExclusive2 > start1
}

fun Appointment.parsedStartEndTimesOrNull(): Pair<LocalTime, LocalTime>? {
    val s = parseAppointmentTimeString(startTime) ?: return null
    val e = parseAppointmentTimeString(endTime) ?: return null
    return s to e
}

fun Appointment.effectiveEmployeeId(): Int? {
    val fromEmbedded = employee.id?.takeIf { it != 0 }
    if (fromEmbedded != null) return fromEmbedded
    return employeeId.takeIf { it != 0 }
}

fun Appointment.sameEmployeeAndDateAs(other: Appointment): Boolean {
    if (date != other.date) return false
    val a = effectiveEmployeeId()
    val b = other.effectiveEmployeeId()
    return a != null && a == b
}

/** Czy z perspektywy harmonogramu przedziały czasu się przecinają (ten sam dzień i pracownik). */
fun Appointment.schedulingTimeOverlaps(other: Appointment): Boolean {
    if (!sameEmployeeAndDateAs(other)) return false
    val t1 = parsedStartEndTimesOrNull() ?: return false
    val t2 = other.parsedStartEndTimesOrNull() ?: return false
    val (a0, a1) = appointmentIntervalEndExclusiveMinutes(t1.first, t1.second)
    val (b0, b1) = appointmentIntervalEndExclusiveMinutes(t2.first, t2.second)
    return minutesIntervalsOverlap(a0, a1, b0, b1)
}

/**
 * Pierwsza wizyta z listy kolidująca z tą (ta sama data, ten sam pracownik, nakładające się godziny).
 * [excludeAppointmentId] — np. edytowana wizyta (pomijamy samą siebie).
 */
fun Appointment.findFirstSchedulingConflict(
    candidates: Iterable<Appointment>,
    excludeAppointmentId: Int?,
): Appointment? = candidates.firstOrNull { existing ->
    if (excludeAppointmentId != null && existing.id == excludeAppointmentId) return@firstOrNull false
    schedulingTimeOverlaps(existing)
}

/** Komunikat jak przy nakładaniu terminów — osobno identyczna godzina startu i ogólne nachodzenie. */
fun formatSchedulingConflictMessage(proposed: Appointment, conflicting: Appointment): String {
    val sameStart = proposed.startTime.trim() == conflicting.startTime.trim()
    return if (sameStart) {
        "Pracownik ma już wizytę o godzinie ${conflicting.startTime}."
    } else {
        "Wizyty nachodzą na siebie — koliduje z wizytą ${conflicting.startTime}–${conflicting.endTime}."
    }
}

/** Pierwsza para wizyt z listy (ten sam dzień i pracownik w danych) z nakładającymi się godzinami. */
fun List<Appointment>.findFirstSchedulingOverlapPair(): Pair<Appointment, Appointment>? {
    for (i in indices) {
        for (j in i + 1 until size) {
            val a = this[i]
            val b = this[j]
            if (a.schedulingTimeOverlaps(b)) return a to b
        }
    }
    return null
}

/** Tekst pod baner ostrzegawczy na liście dnia (makieta `strefa_banner_component.html`). */
fun formatOverlapPairBannerDescription(a: Appointment, b: Appointment): String {
    val n1 = a.timelineClientDisplayName().ifBlank { "Klient" }
    val n2 = b.timelineClientDisplayName().ifBlank { "Klient" }
    return "${a.timelineTimeRangeLabel()} ($n1) i ${b.timelineTimeRangeLabel()} ($n2) — terminy nachodzą na siebie."
}

class Appointment(
    var id: Int = 0,
    var customer: Customer = Customer(),
    /** Powiązanie z klientem (stare wpisy: 0 — wtedy używamy [customer.id]). */
    var customerId: Int = 0,
    var date: String = "", // Data jako String w formacie "dd.MM.yyyy"
    var startTime: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), // Czas jako String
    var endTime: String = LocalTime.now().plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES)
        .format(DateTimeFormatter.ofPattern("HH:mm")), // Czas jako String
    /** Krótki opis usług na karcie osi czasu (makieta: `.appt-service`). */
    var serviceDescription: String = "",
    var notificationSent: Boolean = false,
    /** Powiązanie z pracownikiem (stare wpisy: 0 — użyj [effectiveEmployeeId] / [employee.id]). */
    var employeeId: Int = 0,
    var employee: Employee = Employee(),
    /**
     * Wizyta u dziecka: ID rodzica do SMS (numer z profilu rodzica). 0 = użyj [customer.phoneNumber].
     */
    var smsContactCustomerId: Int = 0,
) {
    fun copy(
        id: Int = this.id,
        customer: Customer = this.customer,
        customerId: Int = this.customerId,
        date: String = this.date,
        startTime: String = this.startTime,
        endTime: String = this.endTime,
        serviceDescription: String = this.serviceDescription,
        notificationSent: Boolean = this.notificationSent,
        employeeId: Int = this.employeeId,
        employee: Employee = this.employee,
        smsContactCustomerId: Int = this.smsContactCustomerId,
    ): Appointment {
        return Appointment(
            id = id,
            customer = customer,
            customerId = customerId,
            date = date,
            startTime = startTime,
            endTime = endTime,
            serviceDescription = serviceDescription,
            notificationSent = notificationSent,
            employeeId = employeeId,
            employee = employee,
            smsContactCustomerId = smsContactCustomerId,
        )
    }
}

/**
 * Oś czasu harmonogramu — zgodnie z makietą `strefa_harmonogram_timeline.html`
 * (siatka 08:00–20:00; w HTML 1 px pionowo = 1 minuta).
 */
object AppointmentScheduleTimeline {
    const val VISIBLE_DAY_START_MINUTE_OF_DAY: Int = 8 * 60
    const val VISIBLE_DAY_END_MINUTE_OF_DAY: Int = 20 * 60
    /** Długość osi w minutach (08:00–20:00), zgodnie z makietą HTML. */
    const val VISIBLE_TIMELINE_SPAN_MINUTES: Int =
        VISIBLE_DAY_END_MINUTE_OF_DAY - VISIBLE_DAY_START_MINUTE_OF_DAY
}

/** Etykieta jak `.appt-time` w makiecie: `HH:mm – HH:mm`. */
fun Appointment.timelineTimeRangeLabel(): String = "${startTime.trim()} – ${endTime.trim()}"

/** Tekst jak `.appt-name` — pełne imię i nazwisko z osadzonego klienta. */
fun Appointment.timelineClientDisplayName(): String = customer.fullName.trim()

/** Etykieta pracownika (harmonogram, nagłówki) — z osadzonego [employee]. */
fun Appointment.timelineWorkerDisplayName(): String = employee.displayName.ifBlank { "Pracownik" }

/** Tekst jak `.appt-service` — krótki opis usług (osobno od notatki profilu klienta). */
fun Appointment.timelineServiceLineOrBlank(): String = serviceDescription.trim()

/**
 * Granice siatki osi (minuta dnia): domyślnie 08:00–20:00, rozszerzane w dół/górę,
 * gdy któraś wizyta zaczyna się wcześniej lub kończy później (pełne godziny).
 */
fun computeTimelineGridMinuteBounds(appointments: List<Appointment>): Pair<Int, Int> {
    val defStart = AppointmentScheduleTimeline.VISIBLE_DAY_START_MINUTE_OF_DAY
    val defEnd = AppointmentScheduleTimeline.VISIBLE_DAY_END_MINUTE_OF_DAY
    var start = defStart
    var end = defEnd
    for (a in appointments) {
        val t = a.parsedStartEndTimesOrNull() ?: continue
        val (s, eex) = appointmentIntervalEndExclusiveMinutes(t.first, t.second)
        start = min(start, s)
        end = max(end, eex)
    }
    start = (start / 60) * 60
    end = ((end + 59) / 60) * 60
    if (end <= start) {
        end = start + (defEnd - defStart)
    }
    return start to end
}

/**
 * Przesunięcie względem [gridStartMinuteOfDay] oraz długość w minutach (pozycja bloku na osi).
 */
fun Appointment.timelineBlockOffsetAndDurationMinutesOrNull(
    gridStartMinuteOfDay: Int = AppointmentScheduleTimeline.VISIBLE_DAY_START_MINUTE_OF_DAY,
): Pair<Int, Int>? {
    val (start, end) = parsedStartEndTimesOrNull() ?: return null
    val (startMin, endExclusive) = appointmentIntervalEndExclusiveMinutes(start, end)
    val duration = endExclusive - startMin
    val offset = startMin - gridStartMinuteOfDay
    return offset to duration
}

/** Naprzemienny wariant stylu karty (`.appt` / `.appt.alt` w makiecie) wg indeksu na liście danego dnia. */
fun scheduleTimelineAltStripe(dayOrderedIndex: Int): Boolean = dayOrderedIndex % 2 == 1
