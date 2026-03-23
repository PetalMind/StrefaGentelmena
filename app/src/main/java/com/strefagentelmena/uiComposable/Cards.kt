package com.strefagentelmena.uiComposable

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.PlainTooltipState
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.parseAppointmentTimeString
import com.strefagentelmena.models.appoimentsModel.timelineWorkerDisplayName
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.ageShortLabel
import com.strefagentelmena.models.lastVisitSortKey
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.models.settngsModel.vacationRangeLabel
import com.strefagentelmena.ui.theme.SalonBg2
import com.strefagentelmena.ui.theme.SalonBg3
import com.strefagentelmena.ui.theme.SalonBorder
import com.strefagentelmena.ui.theme.SalonGold
import com.strefagentelmena.ui.theme.SalonGoldBorder
import com.strefagentelmena.ui.theme.SalonGoldDim
import com.strefagentelmena.ui.theme.SalonIconBoxBorder
import com.strefagentelmena.ui.theme.SalonMuted
import com.strefagentelmena.ui.theme.SalonMuted2
import com.strefagentelmena.ui.theme.SalonText
import com.strefagentelmena.ui.theme.SalonWarmSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private fun minutesUntilAppointmentStart(appointment: Appointment): Long? {
    return try {
        val df = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val start = LocalDateTime.parse("${appointment.date} ${appointment.startTime}", df)
        val m = Duration.between(LocalDateTime.now(), start).toMinutes()
        if (m >= 0) m else null
    } catch (_: Exception) {
        null
    }
}

private fun formatVisitCountdown(minutes: Long): String {
    return when {
        minutes <= 0L -> "teraz"
        minutes < 60L -> "za $minutes min"
        else -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "za $h godz." else "za $h godz. $m min"
        }
    }
}

private fun minutesSinceVisitEnded(appointment: Appointment): Long? {
    return try {
        val dfDate = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val d = LocalDate.parse(appointment.date, dfDate)
        val startT = parseAppointmentTimeString(appointment.startTime) ?: return null
        val endT = parseAppointmentTimeString(appointment.endTime) ?: return null
        var endDt = LocalDateTime.of(d, endT)
        val startDt = LocalDateTime.of(d, startT)
        if (!endDt.isAfter(startDt)) {
            endDt = endDt.plusDays(1)
        }
        val m = Duration.between(endDt, LocalDateTime.now()).toMinutes()
        if (m >= 0) m else null
    } catch (_: Exception) {
        null
    }
}

private fun formatEndedAgo(minutes: Long): String {
    return when {
        minutes < 1L -> "przed chwilą"
        minutes < 60L -> "$minutes min temu"
        else -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "$h godz. temu" else "$h godz. $m min temu"
        }
    }
}

val cardUI = Cards()

class Cards {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UpcomingClientCard(appointment: Appointment) {
        val tooltipState = remember { PlainTooltipState() }
        val scope = rememberCoroutineScope()
        var timeTick by remember { mutableIntStateOf(0) }
        LaunchedEffect(appointment.id) {
            while (true) {
                delay(30_000)
                timeTick++
            }
        }
        val minutesUntil = remember(appointment, timeTick) {
            minutesUntilAppointmentStart(appointment)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(0.5.dp, SalonBorder, RoundedCornerShape(16.dp))
                .background(SalonBg2)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Następna wizyta",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        color = SalonMuted,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    PlainTooltipBox(
                        tooltip = {
                            Text(
                                "Kto jest następny?\nKlient z wizytą w ciągu najbliższej godziny.",
                                color = SalonText,
                            )
                        },
                        tooltipState = tooltipState,
                    ) {
                        IconButton(
                            onClick = { scope.launch { tooltipState.show() } },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Szczegóły",
                                tint = SalonMuted2,
                            )
                        }
                    }
                }
                Text(
                    text = "${appointment.customer.fullName} · ${appointment.startTime}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    color = SalonWarmSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (minutesUntil != null) {
                    Text(
                        text = formatVisitCountdown(minutesUntil),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SalonGold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(0.5.dp, SalonGoldBorder, RoundedCornerShape(20.dp))
                            .background(SalonGoldDim)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    @Composable
    fun CurrentVisitExtendCard(
        appointment: Appointment,
        extendBusy: Boolean,
        onExtendMinutes: (Int) -> Unit,
        onAddFollowUpVisit: (() -> Unit)? = null,
    ) {
        val extendOptions = listOf(15, 30, 45, 60)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(0.5.dp, SalonBorder, RoundedCornerShape(16.dp))
                .background(SalonBg2)
                .padding(20.dp),
        ) {
                Text(
                    text = "Trwa wizyta",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = SalonMuted,
                )
                Text(
                    text = appointment.customer.fullName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = SalonText,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "${appointment.startTime} – ${appointment.endTime}"
                        + if (appointment.timelineWorkerDisplayName().isNotBlank())
                            " · ${appointment.timelineWorkerDisplayName()}"
                        else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SalonWarmSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "Przedłuż wizytę o…",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SalonMuted2,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    extendOptions.forEach { min ->
                        FilledTonalButton(
                            onClick = { onExtendMinutes(min) },
                            enabled = !extendBusy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SalonBg3,
                                contentColor = SalonGold,
                                disabledContainerColor = SalonBg3.copy(alpha = 0.5f),
                                disabledContentColor = SalonMuted2,
                            ),
                        ) {
                            Text(
                                text = "${min}m",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                if (extendBusy) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = SalonGold,
                        )
                    }
                }
                if (onAddFollowUpVisit != null) {
                    FilledTonalButton(
                        onClick = onAddFollowUpVisit,
                        enabled = !extendBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SalonBg3,
                            contentColor = SalonGold,
                            disabledContainerColor = SalonBg3.copy(alpha = 0.5f),
                            disabledContentColor = SalonMuted2,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dodaj kolejną wizytę",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
        }
    }

    @Composable
    fun RecentPastVisitCard(
        appointment: Appointment,
        onClick: () -> Unit,
    ) {
        var timeTick by remember { mutableIntStateOf(0) }
        LaunchedEffect(appointment.id) {
            while (true) {
                delay(60_000)
                timeTick++
            }
        }
        val minutesSince = remember(appointment, timeTick) {
            minutesSinceVisitEnded(appointment)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .border(0.5.dp, SalonBorder, RoundedCornerShape(16.dp))
                .background(SalonBg2)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.customer.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                    ),
                    color = SalonText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${appointment.date} · ${appointment.startTime}–${appointment.endTime}"
                        + if (appointment.timelineWorkerDisplayName().isNotBlank())
                            " · ${appointment.timelineWorkerDisplayName()}"
                        else "",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    color = SalonWarmSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (minutesSince != null) {
                Text(
                    text = formatEndedAgo(minutesSince),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = SalonMuted2,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(0.5.dp, SalonBorder, RoundedCornerShape(20.dp))
                        .background(SalonBg3)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }

    @Composable
    fun DashboardBrandStatCard(
        @DrawableRes iconId: Int,
        bigNumber: String,
        labelText: String,
        onClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 78.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(0.5.dp, SalonBorder, RoundedCornerShape(16.dp))
                .background(SalonBg2)
                .clickable(onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = bigNumber,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 38.sp,
                        lineHeight = 38.sp,
                    ),
                    color = SalonText,
                )
                Text(
                    text = labelText.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = SalonMuted,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .border(0.5.dp, SalonIconBoxBorder, RoundedCornerShape(11.dp))
                    .background(SalonBg3),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SalonGold,
                )
            }
        }
    }

    @Composable
    fun DashboardSmallCard(
        @DrawableRes iconId: Int,
        labelText: String,
        nameText: String,
        onClick: () -> Unit = {},
    ) {
        Card(
            modifier = Modifier
                .size(180.dp, 180.dp)
                .clickable(onClick = onClick)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorsUI.papaya,
            ),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = labelText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = nameText,
                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SwipeToDismissCustomerCard(
        customer: Customer,
        onClick: () -> Unit,
        onDismiss: (Customer) -> Unit,
        onEdit: (Customer) -> Unit
    ) {
        val dismissState = rememberDismissState(initialValue = DismissValue.Default)
        val visible = rememberSaveable { mutableStateOf(true) }
        val scale by animateFloatAsState(
            targetValue = if (dismissState.currentValue != DismissValue.Default) 1.2f else 1f,
            label = ""
        )

        AnimatedVisibility(visible = visible.value) {
            SwipeToDismiss(
                state = dismissState,
                background = {
                    val color = when (dismissState.dismissDirection) {
                        DismissDirection.StartToEnd -> colorsUI.mintGreen
                        DismissDirection.EndToStart -> colorsUI.amaranthPurple
                        null -> Color.Transparent
                    }

                    val direction = dismissState.dismissDirection

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(color)
                    ) {
                        if (direction == DismissDirection.StartToEnd) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .animateContentSize()
                                    .background(color)
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .scale(scale)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = "Edytuj", fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .animateContentSize()
                                    .background(color)
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .scale(scale)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Spacer(modifier = Modifier.heightIn(5.dp))

                                    Text(
                                        text = "Usuń",
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                },
                dismissContent = {
                    CustomerListCard(
                        customer = customer,
                        onClick = onClick
                    )
                },
                directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
            )
        }

        LaunchedEffect(dismissState.targetValue) {
            if (dismissState.targetValue == DismissValue.DismissedToEnd) {
                delay(300)
                dismissState.snapTo(DismissValue.Default)
                onEdit(customer)
            } else if (dismissState.targetValue == DismissValue.DismissedToStart) {
                delay(300)
                onDismiss(customer)
            }
        }


        LaunchedEffect(dismissState.currentValue) {
            if (dismissState.currentValue == DismissValue.DismissedToEnd || dismissState.currentValue == DismissValue.DismissedToStart) {
                delay(300)
                dismissState.snapTo(DismissValue.Default)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SwipeToDismissEmployeeCard(
        employee: Employee,
        onClick: () -> Unit,
        onDismiss: (Employee) -> Unit,
        onEdit: (Employee) -> Unit
    ) {
        val dismissState = rememberDismissState(initialValue = DismissValue.Default)
        val visible = rememberSaveable { mutableStateOf(true) }
        val scale by animateFloatAsState(
            targetValue = if (dismissState.currentValue != DismissValue.Default) 1.2f else 1f,
            label = ""
        )

        AnimatedVisibility(visible = visible.value) {
            SwipeToDismiss(
                state = dismissState,
                background = {
                    val color = when (dismissState.dismissDirection) {
                        DismissDirection.StartToEnd -> colorsUI.mintGreen
                        DismissDirection.EndToStart -> colorsUI.amaranthPurple
                        null -> Color.Transparent
                    }

                    val direction = dismissState.dismissDirection

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(color)
                    ) {
                        if (direction == DismissDirection.StartToEnd) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .animateContentSize()
                                    .background(color)
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .scale(scale)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = "Edytuj", fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .animateContentSize()
                                    .background(color)
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .scale(scale)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Spacer(modifier = Modifier.heightIn(5.dp))

                                    Text(
                                        text = "Usuń",
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                },
                dismissContent = {
                    EmployeeListCard(
                        employee = employee,
                        onClick = onClick
                    )
                },
                directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
            )
        }

        LaunchedEffect(dismissState.targetValue) {
            if (dismissState.targetValue == DismissValue.DismissedToEnd) {
                delay(300)
                dismissState.snapTo(DismissValue.Default)
                onEdit(employee)
            } else if (dismissState.targetValue == DismissValue.DismissedToStart) {
                delay(300)
                onDismiss(employee)
            }
        }


        LaunchedEffect(dismissState.currentValue) {
            if (dismissState.currentValue == DismissValue.DismissedToEnd || dismissState.currentValue == DismissValue.DismissedToStart) {
                delay(300)
                dismissState.snapTo(DismissValue.Default)
            }
        }
    }

    @Composable
    fun EmployeeListCard(
        employee: Employee,
        onClick: () -> Unit,
    ) {
        Card(shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorsUI.papaya,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(colorsUI.jade)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = (employee.name + " " + employee.surname),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    val vacLabel = employee.vacationRangeLabel()
                    if (vacLabel.isNotEmpty()) {
                        Text(
                            text = "Urlop: $vacLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }


    @Composable
    fun CustomerListCard(
        customer: Customer,
        onClick: () -> Unit,
    ) {
        val initials = buildString {
            customer.firstName.firstOrNull()?.uppercaseChar()?.let { append(it) }
            customer.lastName.firstOrNull()?.uppercaseChar()?.let { append(it) }
        }.ifBlank { "?" }

        val subtitle = buildList {
            if (customer.parentCustomerId > 0) {
                add("Profil dziecka")
                customer.ageShortLabel()?.let { add(it) }
            }
            if (customer.lastVisitSortKey().isNotBlank()) {
                add("Ostatnia wizyta: ${customer.lastVisitSortKey()}")
            }
            if (customer.visitCount > 0) {
                add("Wizyt: ${customer.visitCount}")
            }
        }.joinToString(" · ").ifBlank {
            customer.phoneNumber.takeIf { it.isNotBlank() }.orEmpty()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorsUI.papaya)
                .border(0.5.dp, colorsUI.border, RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colorsUI.raisinBlack)
                    .border(0.5.dp, colorsUI.sunset, CircleShape),
            ) {
                Text(
                    text = initials,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorsUI.rusticBrown,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = customer.fullName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorsUI.fontGrey,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = colorsUI.darkGrey,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = colorsUI.murrey,
                modifier = Modifier.size(14.dp),
            )
        }
    }

    @Composable
    fun CustomerAppoimentListCard(
        appointment: Appointment,
        onClick: () -> Unit,
        onNotificationClick: () -> Unit,
        height: Dp = 80.dp,
    ) {
        val showTimeRange = height >= 112.dp
        val contentAlign =
            if (height >= 120.dp) Alignment.TopCenter else Alignment.Center
        Card(
            colors = CardDefaults.cardColors(
                containerColor = colorsUI.papaya
            ),
            shape = RoundedCornerShape(12.dp), // Zmieniłem kształt narożników na nieco mniej zaokrąglone
            elevation = CardDefaults.cardElevation(2.dp), // Podniosłem nieco elewację, aby dodać karcie głębi
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clickable { onClick() }
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = if (showTimeRange) 10.dp else 0.dp),
                contentAlignment = contentAlign
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = if (showTimeRange) Alignment.Top else Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = appointment.customer.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = if (showTimeRange) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                        )
                        if (showTimeRange) {
                            Text(
                                text = "${appointment.startTime} – ${appointment.endTime}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        val serviceLine = appointment.serviceDescription.trim()
                        if (serviceLine.isNotEmpty()) {
                            Text(
                                text = serviceLine,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = if (showTimeRange) 2.dp else 4.dp),
                                maxLines = if (showTimeRange) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    animationElements.NotificationIcon(
                        notificationSent = appointment.notificationSent,
                        onClick = {
                            if (!appointment.notificationSent) onNotificationClick()
                        }
                    )
                }
            }
        }
    }
}

