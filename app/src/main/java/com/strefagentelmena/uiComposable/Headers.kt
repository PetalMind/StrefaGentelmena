package com.strefagentelmena.uiComposable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strefagentelmena.R
import com.strefagentelmena.ui.theme.SalonBg
import com.strefagentelmena.ui.theme.SalonBg3
import com.strefagentelmena.ui.theme.SalonBorderElevated
import com.strefagentelmena.ui.theme.SalonGearSurface
import com.strefagentelmena.ui.theme.SalonGold
import com.strefagentelmena.ui.theme.SalonMuted
import com.strefagentelmena.ui.theme.SalonMuted2
import com.strefagentelmena.functions.appFunctions
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.viewModel.ScheduleModelView
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale

val headersUI = Headers()

@OptIn(ExperimentalMaterial3Api::class)
class Headers {
    @Composable
    fun LogoHeader() {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(200.dp)
        )
    }


    @Composable
    fun NotificationHeader(notificationCount: Int, onClick: () -> Unit) {
        BadgedBox(
            badge = {
                Badge {
                    Text(
                        notificationCount.toString(),
                        modifier = Modifier.semantics {
                            contentDescription = "$notificationCount new notifications"
                        }
                    )
                }
            }) {
            Icon(
                imageVector = if (notificationCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                contentDescription = "Favorite",
                modifier = Modifier.size(32.dp),
            )
        }
    }

    @Composable
    fun AppBarWithBackArrow(
        title: String,
        onBackPressed: () -> Unit,
        modifier: Modifier = Modifier,
        compose: @Composable () -> Unit = {},
        onClick: () -> Unit = {},
    ) {
        Surface(shadowElevation = 3.dp) {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    compose()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorsUI.teaGreen
                ),
                modifier = modifier
            )
        }
    }

    @Composable
    fun DashboardHeaderGreetings(
        greeting: String,
        subtitle: String,
        onSettingsClick: () -> Unit,
        compact: Boolean = false,
    ) {
        val randomGreeting = remember { mutableStateOf(greeting) }
        val currentTimeString = remember {
            mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
        }

        val currentStringDate = remember {
            mutableStateOf(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
            )
        }

        val currentDayAndMonth = remember {
            mutableStateOf(
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault()))
            )
        }

        LaunchedEffect(greeting) {
            randomGreeting.value = greeting
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(60_000L)
                currentTimeString.value =
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                currentStringDate.value =
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
                currentDayAndMonth.value =
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault()))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SalonBg)
                .padding(
                    horizontal = if (compact) 16.dp else 24.dp,
                    vertical = if (compact) 10.dp else 22.dp,
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${currentStringDate.value}, ${currentDayAndMonth.value} · ${currentTimeString.value}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = if (compact) 10.sp else 12.sp,
                        letterSpacing = 0.5.sp,
                    ),
                    color = SalonMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, SalonBorderElevated, CircleShape)
                        .background(SalonGearSurface)
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Ustawienia",
                        modifier = Modifier.size(16.dp),
                        tint = SalonMuted2,
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 8.dp else 20.dp))

            Text(
                text = "Strefa Gentlemana",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = if (compact) 22.sp else 26.sp,
                    lineHeight = if (compact) 22.sp else 26.sp,
                    letterSpacing = 0.2.sp,
                ),
                color = SalonGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 1.8.sp,
                    ),
                    color = SalonMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Spacer(modifier = Modifier.height(if (compact) 8.dp else 16.dp))

            AnimatedContent(
                targetState = randomGreeting.value,
                label = "dashboard_greeting",
                transitionSpec = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(300, easing = LinearEasing)
                    ).togetherWith(
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(300, easing = LinearEasing)
                        )
                    )
                },
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = if (compact) 11.sp else 12.sp,
                    ),
                    color = SalonMuted2,
                    maxLines = if (compact) 1 else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Divider(
                modifier = Modifier.padding(top = if (compact) 10.dp else 24.dp),
                thickness = 0.5.dp,
                color = SalonBg3,
            )
        }
    }

    @Composable
    fun CalendarHeaderView(viewModel: ScheduleModelView) {
        val selectedDateText by viewModel.selectedAppointmentDate.observeAsState()
        val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("pl-PL"))
        val apoimentsList by viewModel.appointmentsList.observeAsState(emptyList())

        var selectedDate: LocalDate? = try {
            LocalDate.parse(selectedDateText, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        } catch (e: DateTimeParseException) {
            null
        }

        LaunchedEffect(selectedDateText) {
            selectedDate = try {
                LocalDate.parse(selectedDateText, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            } catch (e: DateTimeParseException) {
                null
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colorsUI.papaya)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {

                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                selectedDate?.let {
                    Text(
                        text = it.format(dateFormatter),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
            }
        }
    }



    @Composable
    fun IconAndNumber(
        iconResourceId: Int,
        number: String,
        text: String,
        tooltipText: String,
        modifier: Modifier = Modifier,
    ) {
        PlainTooltipBox(
            tooltip = { Text(tooltipText) },
            content = {
                Row(modifier = modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = iconResourceId),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Crop, // Fill the bounds of the parent layout
                    )
                    Column {
                        Text(
                            text = number.toString(),
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = text,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        )
    }
}

