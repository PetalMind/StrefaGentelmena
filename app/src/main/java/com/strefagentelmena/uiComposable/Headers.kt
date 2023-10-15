package com.strefagentelmena.uiComposable

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strefagentelmena.R
import com.strefagentelmena.models.Appointment
import com.strefagentelmena.viewModel.ScheduleModelView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val headersUI = Headers()

@OptIn(ExperimentalMaterial3Api::class)
class Headers {
    @Composable
    fun DashboardHeader() {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(240.dp)
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
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = modifier
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getVisitTimes(appointments: List<Appointment>?, selectedDate: LocalDate): String {
        val filteredAppointments = appointments?.filterNot { it.date.isNullOrEmpty() }?.filter {
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
        return appointments?.filterNot { it.date.isNullOrEmpty() }?.filter {
            LocalDate.parse(it.date, DateTimeFormatter.ofPattern("dd.MM.yyyy")) == selectedDate
        }?.size ?: 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun CalendarHeaderView(viewModel: ScheduleModelView) {
        val context = LocalContext.current
        val selectedDate by viewModel.selectedDate.observeAsState(LocalDate.now())
        val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("pl"))

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)  // Increased padding
                    .clickable {  // Made the whole row clickable
                        showDatePickerDialog(context) { year, month, day ->
                            viewModel.changeDate(LocalDate.of(year, month + 1, day))
                        }
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
                Text(
                    text = selectedDate.format(formatter),
                    style = MaterialTheme.typography.headlineLarge,  // Changed to h6 for better readability
                )
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
                val customerCount = getCustomerCount(viewModel.appointments.value, selectedDate)
                val visitTimes = getVisitTimes(viewModel.appointments.value, selectedDate)

                IconAndNumber(
                    iconResourceId = R.drawable.ic_clients,
                    number = customerCount.toString(),
                    text = "Klienci",
                    tooltipText = "Klienci z dnia dzisiejszego"
                )

                IconAndNumber(
                    iconResourceId = R.drawable.ic_clock,
                    number = visitTimes,
                    text = "Dzień Pracy",
                    tooltipText = "Godziny pracy w dniu dzisiejszym",
                )
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    fun showDatePickerDialog(context: Context, dateSetListener: (Int, Int, Int) -> Unit) {
        val current = LocalDate.now()
        val datePickerDialog = android.app.DatePickerDialog(context, { _, year, month, dayOfMonth ->
            dateSetListener(year, month, dayOfMonth)
        }, current.year, current.monthValue - 1, current.dayOfMonth)

        datePickerDialog.show()
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
