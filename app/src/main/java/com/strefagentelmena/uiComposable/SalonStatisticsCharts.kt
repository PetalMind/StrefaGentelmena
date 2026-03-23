package com.strefagentelmena.uiComposable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strefagentelmena.models.AnalyticsRange
import com.strefagentelmena.ui.theme.SalonBorder
import com.strefagentelmena.ui.theme.SalonGold
import com.strefagentelmena.ui.theme.SalonMuted
import com.strefagentelmena.ui.theme.SalonMuted2
import com.strefagentelmena.ui.theme.SalonText
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val ChartCardBg = Color(0xFF121212)
private val ChartMuted = Color(0xFF666666)
private val HeatEmpty = Color(0xFF1A1812)
private val HeatFull = Color(0xFF7A5E18)

private val dowLabels = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "Sb", "Nd")

@Composable
fun RetentionRingCard(
    percent: Int,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val track = Color(0xFF2A2618)
    val sweep = when {
        percent < 0 -> 0f
        else -> (percent.coerceIn(0, 100) / 100f) * 360f
    }
    val label = when {
        percent < 0 -> "—"
        else -> "$percent%"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ChartCardBg, RoundedCornerShape(18.dp))
            .border(1.dp, SalonBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Text(
            text = "RETENCJA",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.7.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = ChartMuted,
        )
        Text(
            text = "Udział klientów z powrotem (≥2 wizyty) wśród tych z ≥1 wizytą",
            style = MaterialTheme.typography.bodySmall,
            color = SalonMuted2,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(168.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 15.dp.toPx()
                    drawArc(
                        color = track,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    if (sweep > 0.5f) {
                        drawArc(
                            color = SalonGold,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                        ),
                        color = SalonGold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SalonMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleFillSparklineHeatmapCard(
    range: AnalyticsRange,
    referenceDate: LocalDate,
    sparklineDailyCounts: List<Int>,
    heatmapYearMonth: YearMonth,
    heatmapDayCounts: List<Int>,
    modifier: Modifier = Modifier,
) {
    val monthTitle = remember(heatmapYearMonth) {
        val fmt = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pl"))
        heatmapYearMonth.atDay(1).format(fmt)
    }
    val maxHeat = heatmapDayCounts.maxOrNull()?.coerceAtLeast(1) ?: 1

    var selectedSparkIndex by remember { mutableStateOf<Int?>(null) }
    var selectedHeatDay by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(range, sparklineDailyCounts, heatmapYearMonth) {
        selectedSparkIndex = null
        selectedHeatDay = null
    }

    val sparklineTitle = sparklineSectionTitle(range)
    val sparklineHint = sparklineInteractionHint(range)
    val heatmapSectionTitle = "Kalendarz — $monthTitle"
    val heatmapHint =
        "Każda komórka to dzień miesiąca. Kolor od jaśniejszego (pusto) do ciemnozłotego (dużo wizyt). Dotknij dzień, by zobaczyć liczbę wizyt."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ChartCardBg, RoundedCornerShape(18.dp))
            .border(1.dp, SalonBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = "WYPEŁNIENIE GRAFIKU",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.7.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = ChartMuted,
        )
        Text(
            text = "Obciążenie harmonogramu w wybranym zakresie: trend oraz rozkład wizyt po dniach miesiąca.",
            style = MaterialTheme.typography.bodySmall,
            color = SalonMuted2,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        Text(
            text = sparklineTitle,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.55.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = ChartMuted,
        )
        Text(
            text = sparklineHint,
            style = MaterialTheme.typography.bodySmall,
            color = SalonMuted2,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )

        val counts = sparklineDailyCounts
        if (counts.size < 2) {
            Text(
                text = "Za mało punktów do wykresu trendu w tym widoku.",
                style = MaterialTheme.typography.bodySmall,
                color = SalonMuted,
            )
        } else {
            val lastI = counts.size - 1
            val selIdx = selectedSparkIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .pointerInput(counts, range, referenceDate, lastI) {
                        detectTapGestures { offset ->
                            val wPx = size.width.toFloat()
                            val x = offset.x.coerceIn(0f, wPx)
                            val frac = if (wPx <= 0f) 0f else x / wPx
                            selectedSparkIndex = (frac * lastI).roundToInt().coerceIn(0, lastI)
                        }
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxC = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
                    val w = size.width
                    val h = size.height
                    val padY = h * 0.12f
                    val path = Path()
                    counts.forEachIndexed { i, c ->
                        val x = if (lastI <= 0) 0f else w * i / lastI
                        val t = c.toFloat() / maxC
                        val y = h - padY - (h - padY * 2) * t
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path,
                        SalonGold,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                    val highlightIdx = selIdx ?: lastI
                    val hx = if (lastI <= 0) 0f else w * highlightIdx / lastI
                    val hc = counts[highlightIdx]
                    val hy = h - padY - (h - padY * 2) * (hc.toFloat() / maxC)
                    drawCircle(SalonGold, 5.dp.toPx(), Offset(hx, hy))
                    drawCircle(Color.White.copy(alpha = 0.35f), 2.2.dp.toPx(), Offset(hx, hy))
                }
            }
            val detailIdx = selIdx ?: lastI
            Text(
                text = sparklineSelectionLine(range, referenceDate, detailIdx, counts[detailIdx]),
                style = MaterialTheme.typography.labelSmall,
                color = SalonGold,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "Dotknij wykres, aby sprawdzić inny punkt.",
                style = MaterialTheme.typography.labelSmall,
                color = SalonMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        HeatmapLegend()

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = heatmapSectionTitle.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.55.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = ChartMuted,
        )
        Text(
            text = heatmapHint,
            style = MaterialTheme.typography.bodySmall,
            color = SalonMuted2,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            dowLabels.forEach { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = ChartMuted,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        val monthStartDow0 = (heatmapYearMonth.atDay(1).dayOfWeek.value + 6) % 7
        val dim = heatmapDayCounts.size
        val rows = (monthStartDow0 + dim + 6) / 7

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (c in 0 until 7) {
                        val idx = r * 7 + c
                        val dayNum = idx - monthStartDow0 + 1
                        val inMonth = dayNum in 1..dim
                        val isSelected = inMonth && selectedHeatDay == dayNum
                        ScheduleHeatmapDayCell(
                            modifier = Modifier.weight(1f),
                            cellIndex = idx,
                            dayNum = dayNum,
                            inMonth = inMonth,
                            isSelected = isSelected,
                            visitCount = if (inMonth) heatmapDayCounts[dayNum - 1] else 0,
                            backgroundColor = heatCellColor(
                                idx,
                                monthStartDow0,
                                dim,
                                heatmapDayCounts,
                                maxHeat,
                            ),
                            onDayClick = {
                                selectedHeatDay = if (selectedHeatDay == dayNum) null else dayNum
                            },
                        )
                    }
                }
            }
        }

        val heatSel = selectedHeatDay
        if (heatSel != null && heatSel in 1..dim) {
            val cnt = heatmapDayCounts[heatSel - 1]
            val d = heatmapYearMonth.atDay(heatSel)
            val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl"))
            Text(
                text = "${d.format(dayFmt)} — ${formatVisitPhrase(cnt)}",
                style = MaterialTheme.typography.labelSmall,
                color = SalonGold,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = "Dotknij ten sam dzień ponownie, aby schować opis.",
                style = MaterialTheme.typography.labelSmall,
                color = SalonMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LegendSwatch(color = HeatEmpty, label = "Mało / brak wizyt")
        LegendSwatch(color = HeatFull, label = "Dużo wizyt")
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(3.dp))
                .border(0.5.dp, SalonBorder.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SalonMuted2,
        )
    }
}

private fun sparklineSectionTitle(range: AnalyticsRange): String = when (range) {
    AnalyticsRange.Day -> "Trend w ciągu dnia"
    AnalyticsRange.Week -> "Trend w tygodniu"
    AnalyticsRange.Month -> "Trend (ostatnie 30 dni)"
    AnalyticsRange.Year -> "Trend w roku (miesiące)"
    AnalyticsRange.Custom -> "Trend (ostatnie 30 dni z zakresu)"
}

private fun sparklineInteractionHint(range: AnalyticsRange): String = when (range) {
    AnalyticsRange.Day -> "Liczba wizyt wg godziny rozpoczęcia. Dotknij linię, by zobaczyć godzinę i liczbę wizyt."
    AnalyticsRange.Week -> "Liczba wizyt w każdym z ostatnich 7 dni (włącznie z dziś). Dotknij wykres, by wybrać dzień."
    AnalyticsRange.Month -> "Liczba wizyt w kolejnych dniach (ostatnie 30 dni względem dziś). Dotknij wykres, by wybrać dzień."
    AnalyticsRange.Year -> "Suma wizyt w każdym miesiącu bieżącego roku. Dotknij wykres, by wybrać miesiąc."
    AnalyticsRange.Custom -> "Liczba wizyt dziennie dla końcówki wybranego zakresu. Dotknij wykres, by wybrać dzień."
}

private fun sparklineSelectionLine(
    range: AnalyticsRange,
    referenceDate: LocalDate,
    index: Int,
    count: Int,
): String {
    val visits = formatVisitPhrase(count)
    return when (range) {
        AnalyticsRange.Day -> {
            val h = index.coerceIn(0, 23)
            "Godzina %02d:00–%02d:59 — %s".format(h, h, visits)
        }
        AnalyticsRange.Week -> {
            val d = referenceDate.minusDays(6L - index.toLong())
            val fmt = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.forLanguageTag("pl"))
            "${d.format(fmt)} — $visits"
        }
        AnalyticsRange.Month -> {
            val d = referenceDate.minusDays(29L - index.toLong())
            val fmt = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.forLanguageTag("pl"))
            "${d.format(fmt)} — $visits"
        }
        AnalyticsRange.Year -> {
            val ym = YearMonth.of(referenceDate.year, (index + 1).coerceIn(1, 12))
            val fmt = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pl"))
            "${ym.atDay(1).format(fmt)} — $visits"
        }
        AnalyticsRange.Custom -> {
            val d = referenceDate.minusDays(29L - index.toLong())
            val fmt = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.forLanguageTag("pl"))
            "${d.format(fmt)} — $visits"
        }
    }
}

private fun formatVisitPhrase(count: Int): String {
    val word = when {
        count == 1 -> "wizyta"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "wizyty"
        else -> "wizyt"
    }
    return "$count $word"
}

@Composable
private fun ScheduleHeatmapDayCell(
    modifier: Modifier = Modifier,
    cellIndex: Int,
    dayNum: Int,
    inMonth: Boolean,
    isSelected: Boolean,
    visitCount: Int,
    backgroundColor: Color,
    onDayClick: () -> Unit,
) {
    val interaction = remember(cellIndex) { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (inMonth) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onDayClick,
                    )
                } else {
                    Modifier
                },
            )
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) SalonGold else SalonBorder.copy(alpha = 0.35f),
                shape = RoundedCornerShape(6.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (inMonth) {
            Text(
                text = dayNum.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (visitCount > 0) SalonText else SalonMuted,
            )
        }
    }
}

private fun heatCellColor(
    cellIndex: Int,
    lead: Int,
    dim: Int,
    counts: List<Int>,
    maxCount: Int,
): Color {
    val dayNum = cellIndex - lead + 1
    if (dayNum < 1 || dayNum > dim) return Color(0xFF141414)
    val cnt = counts[dayNum - 1]
    if (cnt <= 0) return HeatEmpty
    val t = (cnt.toFloat() / maxCount).coerceIn(0f, 1f)
    return lerp(HeatEmpty, HeatFull, t)
}
