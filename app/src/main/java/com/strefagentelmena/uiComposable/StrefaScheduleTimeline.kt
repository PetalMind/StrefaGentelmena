package com.strefagentelmena.uiComposable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.computeTimelineGridMinuteBounds
import com.strefagentelmena.models.appoimentsModel.scheduleTimelineAltStripe
import com.strefagentelmena.models.appoimentsModel.timelineBlockOffsetAndDurationMinutesOrNull
import com.strefagentelmena.models.appoimentsModel.timelineClientDisplayName
import com.strefagentelmena.models.appoimentsModel.timelineServiceLineOrBlank
import com.strefagentelmena.models.appoimentsModel.timelineTimeRangeLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

/** Kolory z makiet `strefa_harmonogram_timeline.html`. */
object StrefaHarmonogramColors {
    val Bg = Color(0xFF0E0E0E)
    val Border = Color(0xFF262626)
    val Gold = Color(0xFFC9A84C)
    val GoldDim = Color(0x21C9A84C)
    /** Nieprzezroczyste tło karty — nakładające się wizyty czytelne obok siebie. */
    val CardFillSolid = Color(0xFF221E18)
    val GoldBorder = Color(0x4DC9A84C)
    val Text = Color(0xFFE8E0CC)
    val Muted = Color(0xFF555555)
    val Muted2 = Color(0xFF888888)
    val HalfLine = Color(0xFF1C1C1C)
    val AltStripeLeft = Color(0xFF8A7A50)
    val AltTime = Color(0xFFA09060)
    val AltFillSolid = Color(0xFF1E1C16)
    val AltBorder = Color(0x408A7A50)
}

private val plLocale: Locale = Locale.forLanguageTag("pl-PL")

private fun polishDayShort(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Pn"
    DayOfWeek.TUESDAY -> "Wt"
    DayOfWeek.WEDNESDAY -> "Śr"
    DayOfWeek.THURSDAY -> "Cz"
    DayOfWeek.FRIDAY -> "Pt"
    DayOfWeek.SATURDAY -> "Sb"
    DayOfWeek.SUNDAY -> "Nd"
}

private data class TimelineApptLayout(
    val appointment: Appointment,
    val offsetMin: Int,
    val durationMin: Int,
    /** Oś czasu w pionie: przy nakładających się godzinach karty dzielą szerokość (pas 0, 1, …). */
    val lane: Int,
    val totalLanes: Int,
)

/** Przypisanie pasów przy pionowej osi: czas w dół (wysokość = długość wizyty), nakładki obok siebie. */
private fun layoutTimelineAppointments(
    appointments: List<Appointment>,
    gridStartMinuteOfDay: Int,
): List<TimelineApptLayout> {
    val base = gridStartMinuteOfDay
    val items = appointments.mapNotNull { appt ->
        val block = appt.timelineBlockOffsetAndDurationMinutesOrNull(gridStartMinuteOfDay)
            ?: return@mapNotNull null
        val (offsetMin, dur) = block
        val effectiveDur = dur.coerceAtLeast(1)
        val startDayMin = offsetMin + base
        val endExclusive = startDayMin + effectiveDur
        Triple(appt, startDayMin, endExclusive) to (offsetMin to effectiveDur)
    }.sortedBy { it.first.second }

    val laneEnds = mutableListOf<Int>()
    val laneOf = mutableMapOf<Appointment, Int>()
    val durOf = mutableMapOf<Appointment, Pair<Int, Int>>()

    for ((triple, offDur) in items) {
        val (appt, startDayMin, endExclusive) = triple
        val (offsetMin, effectiveDur) = offDur
        durOf[appt] = offsetMin to effectiveDur
        val lane = laneEnds.indexOfFirst { lastEnd -> lastEnd <= startDayMin }
        if (lane >= 0) {
            laneEnds[lane] = endExclusive
            laneOf[appt] = lane
        } else {
            laneEnds.add(endExclusive)
            laneOf[appt] = laneEnds.lastIndex
        }
    }

    val totalLanes = laneEnds.size.coerceAtLeast(1)
    return laneOf.keys.map { appt ->
        val (offsetMin, effectiveDur) = durOf.getValue(appt)
        TimelineApptLayout(
            appointment = appt,
            offsetMin = offsetMin,
            durationMin = effectiveDur,
            lane = laneOf.getValue(appt),
            totalLanes = totalLanes,
        )
    }.sortedWith(compareBy({ it.offsetMin }, { it.appointment.startTime }))
}

private fun formatTopMeta(date: LocalDate): String {
    val raw = date.format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", plLocale),
    )
    return raw.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(plLocale) else ch.toString()
    }
}

@Composable
fun StrefaHarmonogramTimelineContent(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    dayStripDates: List<LocalDate>,
    daysWithAppointments: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
    onAddClick: () -> Unit,
    addButtonLabel: String = "",
    appointments: List<Appointment>,
    isViewingToday: Boolean,
    onAppointmentClick: (Appointment) -> Unit,
    onNotificationClick: (Appointment) -> Unit,
) {
    val (gridStartMin, gridEndMin) = remember(appointments) {
        computeTimelineGridMinuteBounds(appointments)
    }
    val spanMinutes = gridEndMin - gridStartMin
    val timelineHeight: Dp = spanMinutes.dp
    val dayListState = rememberLazyListState()

    LaunchedEffect(selectedDate, dayStripDates) {
        val idx = dayStripDates.indexOf(selectedDate)
        if (idx >= 0) {
            dayListState.animateScrollToItem(idx.coerceAtMost(dayStripDates.lastIndex))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StrefaHarmonogramColors.Bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 12.dp),
        ) {
            Text(
                text = formatTopMeta(selectedDate),
                color = StrefaHarmonogramColors.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Harmonogram",
                    color = StrefaHarmonogramColors.Gold,
                    fontSize = 22.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    modifier = Modifier.clickable(onClick = onAddClick),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (addButtonLabel.isNotBlank()) {
                        Text(
                            text = addButtonLabel,
                            color = StrefaHarmonogramColors.Gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StrefaHarmonogramColors.GoldDim)
                            .border(0.5.dp, StrefaHarmonogramColors.GoldBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Dodaj wizytę",
                            tint = StrefaHarmonogramColors.Gold,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(StrefaHarmonogramColors.Border),
        )

        HarmonogramDayStrip(
            dates = dayStripDates,
            selected = selectedDate,
            daysWithAppointments = daysWithAppointments,
            onSelect = onSelectDate,
            listState = dayListState,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(StrefaHarmonogramColors.Border),
        )

        // Oś czasu pionowa: góra = początek siatki (≥ 08:00, rozszerzane gdy wizyty zaczynają wcześniej), 1 dp ≈ 1 min.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(timelineHeight)
                    .padding(top = 8.dp)
                    .padding(end = 16.dp),
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxHeight(),
                    ) {
                        HourLabels(
                            spanMinutes = spanMinutes,
                            gridStartMinuteOfDay = gridStartMin,
                            gridEndMinuteOfDay = gridEndMin,
                        )
                    }
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(0.dp)),
                    ) {
                        val layouts = remember(appointments, gridStartMin) {
                            layoutTimelineAppointments(appointments, gridStartMin)
                        }
                        val gap = 2.dp
                        val startPad = 4.dp
                        val innerW = maxWidth - startPad
                        TimelineGrid(
                            spanMinutes = spanMinutes,
                            gridStartMinuteOfDay = gridStartMin,
                            gridEndMinuteOfDay = gridEndMin,
                        )
                        layouts.forEachIndexed { index, layout ->
                            val lc = layout.totalLanes.coerceAtLeast(1)
                            val totalGaps = gap * (lc - 1).coerceAtLeast(0)
                            val colW = (innerW - totalGaps) / lc
                            val xOff = startPad + colW * layout.lane + gap * layout.lane
                            TimelineAppointmentBlock(
                                layout = layout,
                                columnWidth = colW,
                                offsetXDp = xOff,
                                useAltStripe = scheduleTimelineAltStripe(index),
                                zBase = index,
                                onClick = { onAppointmentClick(layout.appointment) },
                                onNotificationClick = { onNotificationClick(layout.appointment) },
                            )
                        }
                        if (isViewingToday) {
                            NowIndicatorLine(
                                spanMinutes = spanMinutes,
                                gridStartMinuteOfDay = gridStartMin,
                            )
                        }
                    }
                }
                if (appointments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Brak zaplanowanych wizyt",
                            color = StrefaHarmonogramColors.Muted2,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HarmonogramDayStrip(
    dates: List<LocalDate>,
    selected: LocalDate,
    daysWithAppointments: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
    listState: LazyListState,
) {
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(dates, key = { _, d -> d.toString() }) { _, date ->
            val isSelected = date == selected
            val hasDot = daysWithAppointments.contains(date)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 0.5.dp,
                        color = if (isSelected) StrefaHarmonogramColors.Gold else StrefaHarmonogramColors.Border,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .background(
                        if (isSelected) StrefaHarmonogramColors.Gold else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(date) }
                    .padding(vertical = 7.dp, horizontal = 10.dp),
            ) {
                Text(
                    text = polishDayShort(date.dayOfWeek),
                    fontSize = 9.sp,
                    letterSpacing = 0.3.sp,
                    color = if (isSelected) StrefaHarmonogramColors.Bg else StrefaHarmonogramColors.Muted2,
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) StrefaHarmonogramColors.Bg else StrefaHarmonogramColors.Text,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (hasDot) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0x66000000) else StrefaHarmonogramColors.Gold,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HourLabels(
    spanMinutes: Int,
    gridStartMinuteOfDay: Int,
    gridEndMinuteOfDay: Int,
) {
    val startHour = gridStartMinuteOfDay / 60
    val endHour = gridEndMinuteOfDay / 60
    for (h in startHour..endHour) {
        val minuteFromStart = h * 60 - gridStartMinuteOfDay
        if (minuteFromStart in 0..spanMinutes) {
            Text(
                text = "%02d:00".format(h),
                fontSize = 10.sp,
                color = StrefaHarmonogramColors.Muted,
                fontWeight = FontWeight.Light,
                modifier = Modifier.offset(y = minuteFromStart.dp - 1.dp),
            )
        }
    }
}

@Composable
private fun TimelineGrid(
    spanMinutes: Int,
    gridStartMinuteOfDay: Int,
    gridEndMinuteOfDay: Int,
) {
    val startHour = gridStartMinuteOfDay / 60
    val endHourInclusive = gridEndMinuteOfDay / 60
    for (h in startHour..endHourInclusive) {
        val minuteFromStart = h * 60 - gridStartMinuteOfDay
        if (minuteFromStart in 0..spanMinutes) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .offset(y = minuteFromStart.dp)
                    .background(StrefaHarmonogramColors.Border),
            )
        }
        val half = h * 60 + 30 - gridStartMinuteOfDay
        if (half in 0 until spanMinutes) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .offset(y = half.dp)
                    .background(StrefaHarmonogramColors.HalfLine),
            )
        }
    }
}

@Composable
private fun TimelineAppointmentBlock(
    layout: TimelineApptLayout,
    columnWidth: Dp,
    offsetXDp: Dp,
    useAltStripe: Boolean,
    zBase: Int,
    onClick: () -> Unit,
    onNotificationClick: () -> Unit,
) {
    val appointment = layout.appointment
    val offsetMin = layout.offsetMin
    val durationMin = layout.durationMin
    val minHForTap = 24
    val heightDp = max(durationMin, minHForTap).dp
    val topDp = offsetMin.dp
    val leftGold = if (useAltStripe) StrefaHarmonogramColors.AltStripeLeft else StrefaHarmonogramColors.Gold
    val timeColor = if (useAltStripe) StrefaHarmonogramColors.AltTime else StrefaHarmonogramColors.Gold
    val fill = if (useAltStripe) StrefaHarmonogramColors.AltFillSolid else StrefaHarmonogramColors.CardFillSolid
    val stroke = if (useAltStripe) StrefaHarmonogramColors.AltBorder else StrefaHarmonogramColors.GoldBorder

    val innerWidth = (columnWidth - 2.dp).coerceAtLeast(0.dp)
    val narrow = columnWidth < 72.dp
    val veryNarrow = columnWidth < 52.dp
    val padH = when {
        veryNarrow -> 2.dp
        narrow -> 4.dp
        else -> 6.dp
    }
    val padV = when {
        veryNarrow -> 2.dp
        narrow -> 3.dp
        else -> 4.dp
    }
    val timeSp = when {
        veryNarrow -> 7.sp
        narrow -> 8.sp
        else -> 10.sp
    }
    val nameSp = when {
        veryNarrow -> 9.sp
        narrow -> 10.sp
        else -> 12.sp
    }
    val serviceSp = when {
        veryNarrow -> 7.sp
        narrow -> 8.sp
        else -> 9.sp
    }
    val bellSide = (innerWidth - padH - 2.dp).coerceIn(24.dp, 40.dp)
        .coerceAtMost((innerWidth - 4.dp).coerceAtLeast(22.dp))

    Box(
        modifier = Modifier
            .width(columnWidth.coerceAtLeast(1.dp))
            .height(heightDp)
            .offset(x = offsetXDp, y = topDp)
            .zIndex(1f + zBase * 0.01f)
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, stroke, RoundedCornerShape(10.dp))
            .background(fill, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 1.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(leftGold),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = padH, vertical = padV),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Text(
                        text = appointment.timelineTimeRangeLabel(),
                        fontSize = timeSp,
                        lineHeight = timeSp,
                        color = timeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = appointment.timelineClientDisplayName().ifBlank { "—" },
                        fontSize = nameSp,
                        lineHeight = nameSp,
                        color = StrefaHarmonogramColors.Text,
                        maxLines = if (veryNarrow) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                    val service = appointment.timelineServiceLineOrBlank()
                    if (service.isNotEmpty() && !veryNarrow) {
                        Text(
                            text = service,
                            fontSize = serviceSp,
                            lineHeight = serviceSp,
                            color = StrefaHarmonogramColors.Muted2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier.align(Alignment.Top),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    animationElements.NotificationIcon(
                        notificationSent = appointment.notificationSent,
                        compact = true,
                        compactTouchSize = bellSide,
                        onClick = {
                            if (!appointment.notificationSent) onNotificationClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NowIndicatorLine(spanMinutes: Int, gridStartMinuteOfDay: Int) {
    val now = LocalTime.now()
    val nowMin = now.hour * 60 + now.minute
    val offset = nowMin - gridStartMinuteOfDay
    if (offset !in 0 until spanMinutes) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offset.dp)
            .zIndex(2f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
                .height(1.dp)
                .background(StrefaHarmonogramColors.Gold),
        )
        Box(
            modifier = Modifier
                .offset(x = (-5).dp, y = (-4).dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(StrefaHarmonogramColors.Gold),
        )
    }
}
