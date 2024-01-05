package com.strefagentelmena.uiComposable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strefagentelmena.R
import com.strefagentelmena.functions.appFunctions
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.viewModel.ScheduleModelView
import java.time.LocalDate
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

