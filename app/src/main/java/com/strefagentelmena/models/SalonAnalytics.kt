package com.strefagentelmena.models

import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.parseAppointmentTimeString
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private val apptDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

data class SalonKpiSummary(
    val visitsThisMonth: Int,
    val customersWithVisits: Int,
    val returningCustomers: Int,
    val workedMinutes: Int,
)

data class LoyalCustomerInsight(
    val customer: Customer,
    val visitCount: Int,
    /** Tekst jak „średnio co 4 tyg.” */
    val rhythmLabel: String,
)

data class AtRiskCustomerInsight(
    val customer: Customer,
    val daysSinceLastVisit: Long,
    val detailLabel: String,
)

data class SalonAnalyticsUiState(
    val kpi: SalonKpiSummary,
    /** 0–100 lub -1 gdy brak klientów z wizytami */
    val retentionPercent: Int,
    val sparklineDailyCounts: List<Int>,
    val heatmapYearMonth: YearMonth,
    /** indeks 0 = pierwszy dzień miesiąca */
    val heatmapDayCounts: List<Int>,
    val topLoyal: List<LoyalCustomerInsight>,
    val atRisk: List<AtRiskCustomerInsight>,
)

enum class AnalyticsRange {
    Day,
    Week,
    Month,
    Year,
    Custom,
}

private fun Customer.lastVisitDateOrNull(): LocalDate? {
    val raw = lastVisit?.date?.trim()?.takeIf { it.isNotBlank() }
        ?: appointment?.date?.trim()?.takeIf { it.isNotBlank() }
        ?: return null
    return runCatching { LocalDate.parse(raw, apptDateFmt) }.getOrNull()
}

private fun rhythmLabel(avgWeeks: Double): String {
    if (avgWeeks <= 0.05) return "rytm po 2+ wizytach"
    val w = avgWeeks.roundToInt().coerceAtLeast(1)
    return when {
        w == 1 -> "średnio co tydzień"
        w in 2..4 -> "średnio co $w tyg."
        else -> "średnio co ${w} tyg."
    }
}

private fun atRiskThresholdDays(c: Customer): Long? {
    val visitCount = c.visitCount
    if (visitCount < 1) return null
    val avgW = c.avgWeeksBetweenVisits
    return when {
        visitCount >= 2 && avgW > 0.05 -> {
            val base = (avgW * 7.0 * 1.4).roundToInt().toLong()
            base.coerceIn(21L, 120L)
        }
        visitCount >= 2 -> 40L
        else -> 50L
    }
}

/**
 * Analityka salonu z list klientów (po [mergeCustomersWithVisitStats]) i wizyt z harmonogramu.
 */
fun computeSalonAnalytics(
    customers: List<Customer>,
    appointments: List<Appointment>,
    today: LocalDate = LocalDate.now(),
    range: AnalyticsRange = AnalyticsRange.Month,
    customFrom: LocalDate? = null,
    customTo: LocalDate? = null,
): SalonAnalyticsUiState {
    val real = appointments.filter { it.id != 0 }

    fun parseApptDate(a: Appointment): LocalDate? =
        runCatching { LocalDate.parse(a.date.trim(), apptDateFmt) }.getOrNull()

    val customStart = minOf(
        customFrom ?: today.minusDays(29),
        customTo ?: today,
    )
    val customEnd = maxOf(
        customFrom ?: today.minusDays(29),
        customTo ?: today,
    )
    val referenceDate = when (range) {
        AnalyticsRange.Custom -> customEnd
        else -> today
    }

    fun inRange(d: LocalDate): Boolean = when (range) {
        AnalyticsRange.Day -> d == today
        AnalyticsRange.Week -> !d.isBefore(today.minusDays(6)) && !d.isAfter(today)
        AnalyticsRange.Month -> YearMonth.from(d) == YearMonth.from(today)
        AnalyticsRange.Year -> d.year == today.year
        AnalyticsRange.Custom -> !d.isBefore(customStart) && !d.isAfter(customEnd)
    }

    val scoped = real.filter { appt ->
        val d = parseApptDate(appt) ?: return@filter false
        inRange(d)
    }

    val ym = YearMonth.from(referenceDate)
    val monthStart = ym.atDay(1)
    val visitsThisMonth = scoped.size
    val workedMinutes = scoped.sumOf { appt ->
        val start = parseAppointmentTimeString(appt.startTime) ?: return@sumOf 0
        val end = parseAppointmentTimeString(appt.endTime) ?: return@sumOf 0
        val startMin = start.hour * 60 + start.minute
        var endMin = end.hour * 60 + end.minute
        if (endMin <= startMin) endMin += 24 * 60
        (endMin - startMin).coerceAtLeast(0)
    }
    val groupedScoped = scoped.groupBy { it.effectiveCustomerId() }.filterKeys { it > 0 }
    val withVisits = groupedScoped.size
    val returning = groupedScoped.values.count { it.size >= 2 }
    val retentionPct = when {
        withVisits <= 0 -> -1
        else -> ((100.0 * returning) / withVisits).roundToInt().coerceIn(0, 100)
    }

    val sparkline = when (range) {
        AnalyticsRange.Day -> List(24) { h ->
            scoped.count { appt ->
                val t = parseAppointmentTimeString(appt.startTime) ?: return@count false
                t.hour == h
            }
        }
        AnalyticsRange.Week -> List(7) { idx ->
            val d = today.minusDays(6L - idx.toLong())
            scoped.count { parseApptDate(it) == d }
        }
        AnalyticsRange.Month -> List(30) { idx ->
            val d = today.minusDays(29L - idx.toLong())
            scoped.count { parseApptDate(it) == d }
        }
        AnalyticsRange.Year -> List(12) { idx ->
            val m = idx + 1
            scoped.count {
                val d = parseApptDate(it) ?: return@count false
                d.year == today.year && d.monthValue == m
            }
        }
        AnalyticsRange.Custom -> List(30) { idx ->
            val d = customEnd.minusDays(29L - idx.toLong())
            scoped.count { parseApptDate(it) == d }
        }
    }

    val dim = ym.lengthOfMonth()
    val byDay = scoped.mapNotNull { a -> parseApptDate(a)?.let { it to a } }
        .groupingBy { it.first }
        .eachCount()
    val heatmapDayCounts = List(dim) { i ->
        val d = monthStart.plusDays(i.toLong())
        byDay[d] ?: 0
    }

    val topLoyal = groupedScoped.entries
        .asSequence()
        .filter { it.value.size >= 2 }
        .sortedWith(
            compareByDescending<Map.Entry<Int, List<Appointment>>> { it.value.size }
                .thenBy { entry ->
                    customers.firstOrNull { it.id == entry.key }?.fullName?.lowercase().orEmpty()
                },
        )
        .take(15)
        .mapNotNull { entry ->
            val customer = customers.firstOrNull { it.id == entry.key } ?: return@mapNotNull null
            val sorted = entry.value.sortedBy { parseApptDate(it)?.toEpochDay() ?: Long.MIN_VALUE }
            val avgW = if (sorted.size < 2) 0.0 else sorted.zipWithNext { a, b ->
                val d0 = parseApptDate(a) ?: return@zipWithNext 0.0
                val d1 = parseApptDate(b) ?: return@zipWithNext 0.0
                ChronoUnit.WEEKS.between(d0, d1).toDouble()
            }.average()
            LoyalCustomerInsight(
                customer = customer,
                visitCount = entry.value.size,
                rhythmLabel = rhythmLabel(avgW),
            )
        }
        .toList()

    val atRisk = customers.mapNotNull { c ->
        val threshold = atRiskThresholdDays(c) ?: return@mapNotNull null
        val lastD = c.lastVisitDateOrNull() ?: return@mapNotNull null
        val days = ChronoUnit.DAYS.between(lastD, referenceDate)
        if (days <= threshold) return@mapNotNull null
        val detail = when {
            c.avgWeeksBetweenVisits > 0.05 ->
                "Ostatnia wizyta $days dni temu — zwykle wraca co ~${c.avgWeeksBetweenVisits.roundToInt()} tyg."
            else ->
                "Ostatnia wizyta $days dni temu"
        }
        AtRiskCustomerInsight(customer = c, daysSinceLastVisit = days, detailLabel = detail)
    }
        .sortedByDescending { it.daysSinceLastVisit }
        .take(25)

    return SalonAnalyticsUiState(
        kpi = SalonKpiSummary(
            visitsThisMonth = visitsThisMonth,
            customersWithVisits = withVisits,
            returningCustomers = returning,
            workedMinutes = workedMinutes,
        ),
        retentionPercent = retentionPct,
        sparklineDailyCounts = sparkline,
        heatmapYearMonth = ym,
        heatmapDayCounts = heatmapDayCounts,
        topLoyal = topLoyal,
        atRisk = atRisk,
    )
}
