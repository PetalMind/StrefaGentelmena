package com.strefagentelmena.uiComposable

import android.content.res.Configuration
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.strefagentelmena.functions.calendarUiFunctions.CalendarDataSource
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.AppointmentScheduleTimeline
import com.strefagentelmena.models.appoimentsModel.computeTimelineGridMinuteBounds
import com.strefagentelmena.models.appoimentsModel.scheduleTimelineAltStripe
import com.strefagentelmena.models.appoimentsModel.timelineBlockOffsetAndDurationMinutesOrNull
import com.strefagentelmena.models.appoimentsModel.timelineClientDisplayName
import com.strefagentelmena.models.appoimentsModel.timelineServiceLineOrBlank
import com.strefagentelmena.models.appoimentsModel.timelineTimeRangeLabel
import com.strefagentelmena.models.calendarUiModel.CalendarUiModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

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
private const val MIN_APPOINTMENT_CARD_HEIGHT_MINUTES = 40
private val timelineDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

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

private data class TimelineBreakLayout(
    val startMinOfDay: Int,
    val endExclusiveMinOfDay: Int,
)

private sealed interface TimelineStackedItem {
    data class Appt(val appointment: Appointment, val visualIndex: Int) : TimelineStackedItem
    data class Gap(val startMinOfDay: Int, val endExclusiveMinOfDay: Int) : TimelineStackedItem
}

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

private fun appointmentStartEndMinutesOrNull(appointment: Appointment): Pair<Int, Int>? {
    val block = appointment.timelineBlockOffsetAndDurationMinutesOrNull(gridStartMinuteOfDay = 0)
        ?: return null
    val start = block.first
    val endExclusive = start + block.second.coerceAtLeast(1)
    return start to endExclusive
}

private fun timelineBreaksBetweenAppointments(appointments: List<Appointment>): List<TimelineBreakLayout> {
    val intervals = appointments
        .mapNotNull { appt -> appointmentStartEndMinutesOrNull(appt) }
        .sortedBy { it.first }

    if (intervals.size < 2) return emptyList()

    val mergedBusy = mutableListOf<Pair<Int, Int>>()
    for ((start, endExclusive) in intervals) {
        val last = mergedBusy.lastOrNull()
        if (last == null || start > last.second) {
            mergedBusy.add(start to endExclusive)
        } else {
            mergedBusy[mergedBusy.lastIndex] = last.first to max(last.second, endExclusive)
        }
    }

    val gaps = mutableListOf<TimelineBreakLayout>()
    for (i in 0 until mergedBusy.lastIndex) {
        val gapStart = mergedBusy[i].second
        val gapEnd = mergedBusy[i + 1].first
        if (gapEnd > gapStart) {
            gaps.add(TimelineBreakLayout(gapStart, gapEnd))
        }
    }
    return gaps
}

private fun stackedItemsWithBreaks(appointments: List<Appointment>): List<TimelineStackedItem> {
    if (appointments.isEmpty()) return emptyList()
    val withMinutes = appointments.mapNotNull { appt ->
        val interval = appointmentStartEndMinutesOrNull(appt) ?: return@mapNotNull null
        Triple(appt, interval.first, interval.second)
    }.sortedBy { it.second }
    if (withMinutes.isEmpty()) return emptyList()

    val result = mutableListOf<TimelineStackedItem>()
    var clusterEnd = withMinutes.first().third
    var visualIndex = 0

    withMinutes.forEachIndexed { idx, (appt, _, endExclusive) ->
        result.add(TimelineStackedItem.Appt(appointment = appt, visualIndex = visualIndex++))
        clusterEnd = max(clusterEnd, endExclusive)
        val nextStart = withMinutes.getOrNull(idx + 1)?.second ?: return@forEachIndexed
        if (nextStart > clusterEnd) {
            result.add(
                TimelineStackedItem.Gap(
                    startMinOfDay = clusterEnd,
                    endExclusiveMinOfDay = nextStart,
                )
            )
        }
    }
    return result
}

private fun minuteOfDayLabel(minuteOfDay: Int): String {
    val clamped = minuteOfDay.coerceAtLeast(0)
    val h = (clamped / 60) % 24
    val m = clamped % 60
    return "%02d:%02d".format(h, m)
}

private fun breakRangeLabel(startMinOfDay: Int, endExclusiveMinOfDay: Int): String =
    "${minuteOfDayLabel(startMinOfDay)} - ${minuteOfDayLabel(endExclusiveMinOfDay)}"

private fun computeTimelineMinuteHeightScale(
    layouts: List<TimelineApptLayout>,
): Float {
    val minDuration = layouts.minOfOrNull { it.durationMin } ?: return 1f
    if (minDuration <= 0) return 1f
    return max(1f, MIN_APPOINTMENT_CARD_HEIGHT_MINUTES.toFloat() / minDuration.toFloat())
}

private fun mergeTimelineBoundsWithVacation(
    appointments: List<Appointment>,
    vacationAbsent: Pair<Int, Int>?,
): Pair<Int, Int> {
    val (a0, a1) = computeTimelineGridMinuteBounds(appointments)
    if (vacationAbsent == null) return a0 to a1
    val (v0, v1) = vacationAbsent
    var start = min(a0, v0)
    var end = max(a1, v1)
    start = (start / 60) * 60
    end = ((end + 59) / 60) * 60
    if (end <= start) end = start + 60
    return start to end
}

private fun formatTopMeta(date: LocalDate): String {
    val raw = date.format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", plLocale),
    )
    return raw.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(plLocale) else ch.toString()
    }
}

private fun buildHolidayAllDayAppointment(selectedDate: LocalDate, holidayName: String): Appointment {
    return Appointment(
        id = -selectedDate.hashCode(),
        customer = Customer(firstName = "Święto", lastName = holidayName),
        date = selectedDate.format(timelineDateFormatter),
        startTime = "%02d:00".format(AppointmentScheduleTimeline.VISIBLE_DAY_START_MINUTE_OF_DAY / 60),
        endTime = "%02d:00".format(AppointmentScheduleTimeline.VISIBLE_DAY_END_MINUTE_OF_DAY / 60),
        serviceDescription = "Dzień niepracujący",
        notificationSent = true,
    )
}

private fun isVirtualHolidayAppointment(appointment: Appointment): Boolean {
    return appointment.serviceDescription == "Dzień niepracujący" && appointment.customer.firstName == "Święto"
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
    isStacked: Boolean = false,
    onAppointmentClick: (Appointment) -> Unit,
    onNotificationClick: (Appointment) -> Unit,
    /** Urlop/wolne: minuty dnia [początek, koniec wyłącznie) — pasek na osi czasu. */
    vacationAbsentMinutes: Pair<Int, Int>? = null,
    vacationAbsentLabel: String = "",
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val calendarDataSource = remember { CalendarDataSource() }
    val dayMetaState = remember { mutableStateOf<Map<LocalDate, CalendarUiModel.Date>>(emptyMap()) }
    val selectedDayMeta = dayMetaState.value[selectedDate]
    val holidayAppointment = remember(selectedDate, selectedDayMeta?.holidayName) {
        selectedDayMeta?.holidayName?.takeIf { selectedDayMeta.isHoliday }?.let {
            buildHolidayAllDayAppointment(selectedDate, it)
        }
    }
    val timelineAppointments = remember(appointments, holidayAppointment) {
        if (holidayAppointment == null) appointments else appointments + holidayAppointment
    }

    val (gridStartMin, gridEndMin) = remember(timelineAppointments, vacationAbsentMinutes) {
        mergeTimelineBoundsWithVacation(timelineAppointments, vacationAbsentMinutes)
    }
    val spanMinutes = gridEndMin - gridStartMin
    val dayListState = rememberLazyListState()
    val rawLayouts = remember(timelineAppointments, gridStartMin) {
        layoutTimelineAppointments(timelineAppointments, gridStartMin)
    }
    val minuteHeightScale = remember(rawLayouts) {
        computeTimelineMinuteHeightScale(rawLayouts)
    }
    val timelineHeight: Dp = (spanMinutes.toFloat() * minuteHeightScale).dp
    val timelineBreaks = remember(timelineAppointments) {
        timelineBreaksBetweenAppointments(timelineAppointments)
    }
    val stackedItems = remember(timelineAppointments) {
        stackedItemsWithBreaks(timelineAppointments)
    }

    LaunchedEffect(dayStripDates, selectedDate) {
        calendarDataSource.ensurePolishHolidaysInRange(dayStripDates)
        dayMetaState.value = dayStripDates.associateWith { date ->
            calendarDataSource.toDateUiModel(date, isSelectedDate = date == selectedDate)
        }
    }

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
                .padding(
                    top = if (isLandscape) 6.dp else 18.dp, 
                    bottom = if (isLandscape) 4.dp else 12.dp
                ),
        ) {
            if (!isLandscape) {
                Text(
                    text = formatTopMeta(selectedDate),
                    color = StrefaHarmonogramColors.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Harmonogram",
                    color = StrefaHarmonogramColors.Gold,
                    fontSize = if (isLandscape) 18.sp else 22.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                )
                if (isLandscape) {
                    Text(
                        text = formatTopMeta(selectedDate),
                        color = StrefaHarmonogramColors.Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light,
                    )
                }
                Row(
                    modifier = Modifier.clickable(onClick = onAddClick),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (addButtonLabel.isNotBlank() && !isLandscape) {
                        Text(
                            text = addButtonLabel,
                            color = StrefaHarmonogramColors.Gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 24.dp else 28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StrefaHarmonogramColors.GoldDim)
                            .border(0.5.dp, StrefaHarmonogramColors.GoldBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Dodaj wizytę",
                            tint = StrefaHarmonogramColors.Gold,
                            modifier = Modifier.size(if (isLandscape) 12.dp else 14.dp),
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
            dayMetaByDate = dayMetaState.value,
            onSelect = onSelectDate,
            listState = dayListState,
            isLandscape = isLandscape
        )

        if (selectedDayMeta?.isHoliday == true && !selectedDayMeta.holidayName.isNullOrBlank()) {
            HolidayBanner(
                holidayName = selectedDayMeta.holidayName,
                isLandscape = isLandscape,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(StrefaHarmonogramColors.Border),
        )

        if (isStacked) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stackedItems.forEach { item ->
                    when (item) {
                        is TimelineStackedItem.Appt -> {
                            val appt = item.appointment
                            val dur = appt.timelineBlockOffsetAndDurationMinutesOrNull(0)?.second ?: 30
                            AppointmentCard(
                                appointment = appt,
                                heightDp = max(dur, MIN_APPOINTMENT_CARD_HEIGHT_MINUTES).dp,
                                useAltStripe = scheduleTimelineAltStripe(item.visualIndex),
                                onClick = { if (!isVirtualHolidayAppointment(appt)) onAppointmentClick(appt) },
                                onNotificationClick = { if (!isVirtualHolidayAppointment(appt)) onNotificationClick(appt) }
                            )
                        }
                        is TimelineStackedItem.Gap -> {
                            BreakCard(
                                label = "Wolne ${breakRangeLabel(item.startMinOfDay, item.endExclusiveMinOfDay)}",
                                heightDp = max(item.endExclusiveMinOfDay - item.startMinOfDay, 20).dp,
                            )
                        }
                    }
                }
                if (timelineAppointments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak zaplanowanych wizyt",
                            color = StrefaHarmonogramColors.Muted2,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        } else {
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
                                minuteHeightScale = minuteHeightScale,
                            )
                        }
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(0.dp)),
                        ) {
                            val startPad = 4.dp
                            val innerW = maxWidth - startPad
                            TimelineGrid(
                                spanMinutes = spanMinutes,
                                gridStartMinuteOfDay = gridStartMin,
                                gridEndMinuteOfDay = gridEndMin,
                                minuteHeightScale = minuteHeightScale,
                            )
                            vacationAbsentMinutes?.let { (vs, ve) ->
                                val top = max(vs, gridStartMin)
                                val bottom = min(ve, gridEndMin)
                                if (bottom > top) {
                                    val offsetMin = top - gridStartMin
                                    val durationMin = bottom - top
                                    VacationAbsenceCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset(y = (offsetMin.toFloat() * minuteHeightScale).dp)
                                            .zIndex(0.25f),
                                        label = vacationAbsentLabel.ifBlank { "Urlop / wolne" },
                                        heightDp = (durationMin.toFloat() * minuteHeightScale).dp,
                                    )
                                }
                            }
                            timelineBreaks.forEach { gap ->
                                val offsetMin = gap.startMinOfDay - gridStartMin
                                val durationMin = gap.endExclusiveMinOfDay - gap.startMinOfDay
                                if (offsetMin >= 0 && durationMin > 0) {
                                    BreakCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset(y = (offsetMin.toFloat() * minuteHeightScale).dp),
                                        label = "Wolne ${breakRangeLabel(gap.startMinOfDay, gap.endExclusiveMinOfDay)}",
                                        heightDp = (durationMin.toFloat() * minuteHeightScale).dp,
                                    )
                                }
                            }
                            rawLayouts.forEachIndexed { index, layout ->
                                val colW = innerW.coerceAtLeast(1.dp)
                                AppointmentCard(
                                    appointment = layout.appointment,
                                    modifier = Modifier
                                        .width(colW)
                                        .offset(x = startPad, y = (layout.offsetMin.toFloat() * minuteHeightScale).dp)
                                        .zIndex(1f + index * 0.01f),
                                    heightDp = (layout.durationMin.toFloat() * minuteHeightScale).dp,
                                    columnWidth = colW,
                                    useAltStripe = scheduleTimelineAltStripe(index),
                                    onClick = {
                                        if (!isVirtualHolidayAppointment(layout.appointment)) onAppointmentClick(
                                            layout.appointment
                                        )
                                    },
                                    onNotificationClick = {
                                        if (!isVirtualHolidayAppointment(layout.appointment)) onNotificationClick(
                                            layout.appointment
                                        )
                                    },
                                )
                            }
                            if (isViewingToday) {
                                NowIndicatorLine(
                                    spanMinutes = spanMinutes,
                                    gridStartMinuteOfDay = gridStartMin,
                                    minuteHeightScale = minuteHeightScale,
                                )
                            }
                        }
                    }
                    if (timelineAppointments.isEmpty()) {
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
}

@Composable
private fun VacationAbsenceCard(
    label: String,
    heightDp: Dp,
    modifier: Modifier = Modifier,
) {
    val compact = heightDp < 28.dp
    val textSize = if (compact) 9.sp else 10.sp
    Box(
        modifier = modifier
            .height(heightDp)
            .clip(RoundedCornerShape(10.dp))
            .border(0.8.dp, Color(0x66E57373), RoundedCornerShape(10.dp))
            .background(Color(0x28E57373), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color(0xFFE8C4C4),
            fontSize = textSize,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun BreakCard(
    label: String,
    heightDp: Dp,
    modifier: Modifier = Modifier,
) {
    val compact = heightDp < 28.dp
    val markerSize = if (compact) 6.dp else 7.dp
    val textSize = if (compact) 9.sp else 10.sp
    Box(
        modifier = modifier
            .height(heightDp)
            .clip(RoundedCornerShape(10.dp))
            .border(0.8.dp, StrefaHarmonogramColors.GoldBorder, RoundedCornerShape(10.dp))
            .background(Color(0x2BC9A84C), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(markerSize)
                    .clip(CircleShape)
                    .background(StrefaHarmonogramColors.Gold),
            )
            Text(
                text = label,
                color = StrefaHarmonogramColors.Text,
                fontSize = textSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    modifier: Modifier = Modifier,
    heightDp: Dp,
    columnWidth: Dp = 300.dp, // Domyślnie szeroka (np. w trybie stacked)
    useAltStripe: Boolean,
    onClick: () -> Unit,
    onNotificationClick: () -> Unit,
) {
    val leftGold = if (useAltStripe) StrefaHarmonogramColors.AltStripeLeft else StrefaHarmonogramColors.Gold
    val timeColor = if (useAltStripe) StrefaHarmonogramColors.AltTime else StrefaHarmonogramColors.Gold
    val fill = if (useAltStripe) StrefaHarmonogramColors.AltFillSolid else StrefaHarmonogramColors.CardFillSolid
    val stroke = if (useAltStripe) StrefaHarmonogramColors.AltBorder else StrefaHarmonogramColors.GoldBorder

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
    val bellSide = (columnWidth - padH * 2 - 2.dp).coerceIn(24.dp, 40.dp)
        .coerceAtMost((columnWidth - 4.dp).coerceAtLeast(22.dp))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
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
private fun HarmonogramDayStrip(
    dates: List<LocalDate>,
    selected: LocalDate,
    daysWithAppointments: Set<LocalDate>,
    dayMetaByDate: Map<LocalDate, CalendarUiModel.Date>,
    onSelect: (LocalDate) -> Unit,
    listState: LazyListState,
    isLandscape: Boolean = false
) {
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = if (isLandscape) 4.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(dates, key = { _, d -> d.toString() }) { _, date ->
            val isSelected = date == selected
            val hasDot = daysWithAppointments.contains(date)
            val dayMeta = dayMetaByDate[date]
            val marker = when {
                dayMeta?.isHoliday == true -> "SW"
                dayMeta?.isWeekend == true -> "WE"
                else -> null
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(if (isLandscape) 42.dp else 48.dp)
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
                    .padding(vertical = if (isLandscape) 4.dp else 7.dp, horizontal = 10.dp),
            ) {
                Text(
                    text = polishDayShort(date.dayOfWeek),
                    fontSize = if (isLandscape) 8.sp else 9.sp,
                    letterSpacing = 0.3.sp,
                    color = if (isSelected) StrefaHarmonogramColors.Bg else StrefaHarmonogramColors.Muted2,
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = if (isLandscape) 13.sp else 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) StrefaHarmonogramColors.Bg else StrefaHarmonogramColors.Text,
                    modifier = Modifier.padding(top = 1.dp),
                )
                if (hasDot && !isLandscape) {
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
                if (!isLandscape && marker != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = marker,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) StrefaHarmonogramColors.Bg else StrefaHarmonogramColors.Gold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HolidayBanner(
    holidayName: String,
    isLandscape: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = if (isLandscape) 4.dp else 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StrefaHarmonogramColors.GoldDim)
            .border(0.5.dp, StrefaHarmonogramColors.GoldBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = if (isLandscape) 7.dp else 9.dp),
    ) {
        Text(
            text = "Święto: $holidayName",
            color = StrefaHarmonogramColors.Gold,
            fontSize = if (isLandscape) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun HourLabels(
    spanMinutes: Int,
    gridStartMinuteOfDay: Int,
    gridEndMinuteOfDay: Int,
    minuteHeightScale: Float,
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
                modifier = Modifier.offset(y = (minuteFromStart.toFloat() * minuteHeightScale).dp - 1.dp),
            )
        }
    }
}

@Composable
private fun TimelineGrid(
    spanMinutes: Int,
    gridStartMinuteOfDay: Int,
    gridEndMinuteOfDay: Int,
    minuteHeightScale: Float,
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
                    .offset(y = (minuteFromStart.toFloat() * minuteHeightScale).dp)
                    .background(StrefaHarmonogramColors.Border),
            )
        }
        val half = h * 60 + 30 - gridStartMinuteOfDay
        if (half in 0 until spanMinutes) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .offset(y = (half.toFloat() * minuteHeightScale).dp)
                    .background(StrefaHarmonogramColors.HalfLine),
            )
        }
    }
}

@Composable
private fun NowIndicatorLine(
    spanMinutes: Int,
    gridStartMinuteOfDay: Int,
    minuteHeightScale: Float,
) {
    val now = LocalTime.now()
    val nowMin = now.hour * 60 + now.minute
    val offset = nowMin - gridStartMinuteOfDay
    if (offset !in 0 until spanMinutes) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (offset.toFloat() * minuteHeightScale).dp)
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
