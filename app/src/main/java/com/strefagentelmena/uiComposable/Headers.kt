package com.strefagentelmena.uiComposable

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strefagentelmena.R
import com.strefagentelmena.functions.appFunctions
import com.strefagentelmena.models.Appointment
import com.strefagentelmena.viewModel.ScheduleModelView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
        Surface(shadowElevation = 3.dp,) {
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun getVisitTimes(appointments: List<Appointment>?, selectedDate: LocalDate): String {
        val filteredAppointments = appointments?.filterNot { it.date.isEmpty() }?.filter {
            LocalDate.parse(it.date, DateTimeFormatter.ofPattern("dd.MM.yyyy")) == selectedDate
        } ?: emptyList()
        val sortedAppointments = filteredAppointments.sortedBy { it.startTime }
        val firstVisitTime = sortedAppointments.firstOrNull()?.startTime
        val lastVisitTime =
            sortedAppointments.lastOrNull()?.let { Appointment.calculateEndTime(it.startTime) }
        return if (firstVisitTime != null && lastVisitTime != null) {
            "$firstVisitTime - $lastVisitTime"
        } else {
            "Brak wizyt"
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun getCustomerCount(appointments: List<Appointment>?, selectedDate: LocalDate): Int {
        return appointments?.filterNot { it.date.isEmpty() }?.filter {
            LocalDate.parse(it.date, DateTimeFormatter.ofPattern("dd.MM.yyyy")) == selectedDate
        }?.size ?: 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun CalendarHeaderView(viewModel: ScheduleModelView) {
        val context = LocalContext.current
        val selectedDate by viewModel.currentSelectedAppoinmentsDate.observeAsState()
        val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("pl"))

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)  // Increased padding
                    .clickable {  // Made the whole row clickable

                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))  // Spacer added here
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Calendar Icon",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                selectedDate?.let {
                    Text(
                        text = it.format(formatter),
                        style = MaterialTheme.typography.headlineLarge,  // Changed to h6 for better readability
                    )
                }
                Spacer(modifier = Modifier.weight(1f))  // Spacer remains here
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorsUI.grey),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
//                val customerCount = getCustomerCount(viewModel.appointments.value, selectedDate)
//                val visitTimes = getVisitTimes(viewModel.appointments.value, selectedDate)
//
//                IconAndNumber(
//                    iconResourceId = R.drawable.ic_clients,
//                    number = customerCount.toString(),
//                    text = "Klienci",
//                    tooltipText = "Klienci z dnia dzisiejszego"
//                )
//
//                IconAndNumber(
//                    iconResourceId = R.drawable.ic_clock,
//                    number = visitTimes,
//                    text = "Dzień Pracy",
//                    tooltipText = "Godziny pracy w dniu dzisiejszym",
//                )
            }
        }
    }


    @Composable
    fun CalendarHeader(
        currentDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
        onDaySelected: (String) -> Unit,
    ) {
        val selectedDay = remember { mutableIntStateOf(currentDay) }
        val daysInCurrentWeek = remember {
            mutableStateOf(appFunctions.getCurrentWeekDays(selectedDay.value.toString()))
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

        LaunchedEffect(onDaySelected) {
            daysInCurrentWeek.value = appFunctions.getCurrentWeekDays(selectedDay.value.toString())
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = colorsUI.headersBlue)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorsUI.grey, shape = RoundedCornerShape(4.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "PON.",
                            "WT.",
                            "ŚR.",
                            "CZW.",
                            "PT.",
                            "SOB.",
                            "NIEDZ."
                        ).forEach { day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Black,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        daysInCurrentWeek.value.forEach { dayNumber ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (dayNumber.toInt() == selectedDay.intValue) colorsUI.yellow else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = if (dayNumber.toInt() == selectedDay.intValue) MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ) else MaterialTheme.typography.bodyLarge,
                                    color = if (dayNumber.toInt() == selectedDay.intValue) Color.Black else colorsUI.darkGrey,
                                    modifier = Modifier
                                        .clickable {
                                            selectedDay.intValue = dayNumber.toInt()
                                            val formattedDate = String.format(
                                                "%02d.%02d.%04d",
                                                dayNumber,
                                                currentMonth,
                                                currentYear
                                            )
                                            onDaySelected(formattedDate)
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
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
