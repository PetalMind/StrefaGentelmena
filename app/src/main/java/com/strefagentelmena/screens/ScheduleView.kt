package com.strefagentelmena.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.strefagentelmena.dataSource.pernamentFormats
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.SMSManager
import com.strefagentelmena.functions.smsManager
import com.strefagentelmena.models.Appointment
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.viewModel.ScheduleModelView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

val screenSchedule = Schedule()

class Schedule {

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun ScheduleView(
        navController: NavController,
        viewModel: ScheduleModelView,
    ) {
        val appointments by viewModel.appointmentsList.observeAsState(emptyList())
        val showApoimentDialog by viewModel.showAppointmentDialog.observeAsState(false)
        val message by viewModel.messages.observeAsState("")
        val context = LocalContext.current
        val viewState by viewModel.viewState.observeAsState(AppState.Idle)


        when (viewState) {
            AppState.Idle -> {
                LaunchedEffect(Unit) {
                    viewModel.loadAllData(context = context)
                    viewModel.loadCustomersList(context = context)
                }
            }

            AppState.Loading -> {

            }

            AppState.Error -> {

            }

            AppState.Success -> {
                AppointmentSuccesConent(
                    navController = navController,
                    viewModel = viewModel,
                )
            }
        }

    }

    /**
     * Appointment Succes Conent.
     *
     * @param customersModelView
     * @param navController
     * @param viewModel
     * @param dashboardModelView
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun AppointmentSuccesConent(
        navController: NavController,
        viewModel: ScheduleModelView,
    ) {
        val showApoimentDialog by viewModel.showAppointmentDialog.observeAsState(false)
        val message by viewModel.messages.observeAsState("")
        val context = LocalContext.current
        val currentSelectedAppoinmentsDate by viewModel.currentSelectedAppoinmentsDate.observeAsState(
            ""
        )

        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        val currentSelectedDay = remember {
            mutableIntStateOf(currentSelectedAppoinmentsDate?.let {
                sdf.parse(it)?.date ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            } ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
        }

        LaunchedEffect(currentSelectedAppoinmentsDate) {
            currentSelectedDay.intValue = currentSelectedAppoinmentsDate?.let {
                sdf.parse(it)?.date ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            } ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        }

        // Inicjalizacja stanu Scaffold
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }


        // Efekt wyzwalany, gdy wartość 'message' się zmienia
        LaunchedEffect(message) {
            if (message?.isNotEmpty() == true && message != "") {
                scope.launch {
                    message?.let {
                        snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                    }
                    viewModel.clearMessages()
                }
            }
        }

        LaunchedEffect(Unit) {
            viewModel.clearMessages()
            viewModel.loadAllData(context = context)
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                headersUI.AppBarWithBackArrow(
                    title = "Plan Dnia",
                    onBackPressed = {
                        navController.navigate("dashboard")
                    },
                    compose = {
                        IconButton(onClick = {
                            dialogsUI.showDatePickerDialog(context, dateSetListener = {
                                viewModel.setNewAppoimentsDate(it)
                            }, viewModel = viewModel)
                        }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    onClick = {
                    },
                )
            },
            floatingActionButton = {
                buttonsUI.ExtendedFab(text = "Dodaj wizytę", icon = Icons.Default.Add) {
                    viewModel.setAppoimentState(true)
                    viewModel.selectedClient.value = null
                    viewModel.showApoimentDialog()
                }
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                headersUI.CalendarHeaderView(viewModel)

                headersUI.CalendarHeader(
                    onDaySelected = {
                        viewModel.setNewAppoimentsDate(it)
                    },
                    currentDayFormatter = currentSelectedAppoinmentsDate,
                    currentDay = currentSelectedDay.intValue,
                )

                AppointmentsList(viewModel) { selectedAppointment ->
                    viewModel.selectAppointmentAndClient(selectedAppointment)
                    viewModel.setAppoimentState(false)

                    viewModel.showApoimentDialog()
                }
            }
        }

        if (showApoimentDialog) {
            dialogsUI.OnAddOrEditSchedule(
                viewModel = viewModel,
            )
        }
    }

    /**
     * Time line with appointments
     *
     * @param appointments
     * @param onClick
     * @receiver
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun TimeLineWithAppointments(
        appointments: List<Appointment>,
        onClick: (Appointment) -> Unit,
        viewModel: ScheduleModelView
    ) {
        val notificationDialogState by viewModel.onNotificationClickState.observeAsState(false)
        val selectedAppointment by viewModel.selectedAppointment.observeAsState(null)
        val context = LocalContext.current
        // Helper function to generate time intervals
        LazyColumn {
            itemsIndexed(appointments) { index, appointment ->
                val startTime =
                    LocalTime.parse(
                        appointment.startTime, DateTimeFormatter.ofPattern(
                            pernamentFormats.TIME_FORMAT_PATTERN
                        )
                    )
                val endTime = startTime.plusHours(1) // Dodaj 1 godzinę do czasu rozpoczęcia

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Draw hours, vertical time line, and circle
                    Column {
                        // Add additional 30 minute time intervals if not the last appointment
                        Row(
                            modifier = Modifier
                                .width(90.dp)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically  // Vertically center the contents
                        ) {

                            if (index == 0 || appointments[index - 1].startTime != appointment.startTime) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row {
                                        Canvas(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .align(Alignment.CenterVertically)  // Vertically center align the circle
                                                .background(
                                                    Color.Gray,
                                                    CircleShape
                                                )  // Set background color and shape
                                        ) {
                                            drawCircle(
                                                color = colorsUI.mintGreen,
                                                radius = size.width / 2,
                                                center = Offset(
                                                    size.width / 2,
                                                    size.height / 2
                                                )  // Center the circle
                                            )
                                        }
                                        Text(
                                            text = String.format(
                                                "%02d:%02d",
                                                startTime.hour,
                                                startTime.minute
                                            ),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                //     .align(Alignment.CenterVertically)  // Vertically center align the text
                                                .padding(start = 8.dp)  // Add some padding to separate the text from the line and circle
                                        )
                                    }

                                    if (index < appointments.size - 1) {
                                        val nextAppointment = appointments[index + 1]
                                        val endTimesAppointment = LocalTime.parse(
                                            nextAppointment.startTime,
                                            DateTimeFormatter.ofPattern("HH:mm")
                                        )

                                        val intervals =
                                            generateTimeIntervals(startTime, endTimesAppointment)
                                        intervals.forEach { interval ->
                                            Row(
                                                modifier = Modifier
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.Start,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (interval == endTime) {
                                                    Row {
                                                        Text(
                                                            text = String.format(
                                                                "%02d:%02d",
                                                                interval.hour,
                                                                interval.minute
                                                            ),
                                                            fontSize = 14.sp,
                                                            color = colorsUI.fontGrey,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier
                                                                .padding(
                                                                    start = 12.dp,
                                                                    bottom = 4.dp
                                                                )  // Adjust padding as needed
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = String.format(
                                                            "%02d:%02d",
                                                            interval.hour,
                                                            interval.minute
                                                        ),
                                                        fontSize = 10.sp,
                                                        color = Color.Gray,
                                                        fontWeight = FontWeight.Normal,
                                                        modifier = Modifier
                                                            .padding(
                                                                start = 12.dp,
                                                                bottom = 4.dp
                                                            )  // Adjust padding as needed
                                                    )
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                    // Draw appointment card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        cardUI.CustomerAppoimentListCard(
                            appointment,
                            onClick = { onClick(appointment) },
                            onNotificationClick = {
                               // viewModel.showNotificationState()
                            }
                        )
                    }
                }
                if (notificationDialogState) {
                    dialogsUI.SendNotificationDialog(
                        objectName = selectedAppointment?.customer?.fullName ?: "",
                        onConfirm = {
                            viewModel.sendNotificationForAppointment(context = context)
                            viewModel.hideNotificationState()
                            viewModel.hideApoimentDialog()
                        },
                        onDismiss = { viewModel.hideNotificationState() })

                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateTimeIntervals(start: LocalTime, end: LocalTime): List<LocalTime> {
        val intervals = mutableListOf<LocalTime>()
        var current = start.plusMinutes(15)  // Start at the first 30 minute interval
        while (current.isBefore(end)) {
            intervals.add(current)
            current = current.plusMinutes(15)  // Add 30 minutes to the current time
        }
        return intervals
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun AppointmentsList(viewModel: ScheduleModelView, onClick: (Appointment) -> Unit) {
        val appointmentsList by viewModel.appointmentsList.observeAsState(emptyList())
        val selectedDate by viewModel.currentSelectedAppoinmentsDate.observeAsState(LocalDate.now())

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        val filteredAppointments = appointmentsList.filter {
            it.date == selectedDate
        }.sortedBy {
            LocalTime.parse(it.startTime, DateTimeFormatter.ofPattern("HH:mm"))
        }

        if (filteredAppointments.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Brak umówionych wizyt na ten dzień", fontSize = 24.sp)
            }
        } else {
            TimeLineWithAppointments(
                appointments = filteredAppointments,
                onClick = onClick,
                viewModel = viewModel
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun TimeSlotsView(viewModel: ScheduleModelView) {
        val appointmentsList by viewModel.appointmentsList.observeAsState(emptyList())
        // Zakładamy, że mamy już funkcję, która filtruje wizyty na wybrany dzień
        val filteredAppointments = listOf("1", 2, 3, 4)

        LazyColumn {
            for (hour in 8 until 22) {  // Od 8 do 21
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "$hour",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.End
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("00", style = MaterialTheme.typography.bodyLarge)
                            Text("15", style = MaterialTheme.typography.bodyLarge)
                            Text("30", style = MaterialTheme.typography.bodyLarge)
                            Text("45", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        Box {
            // Iteruj przez `filteredAppointments` i dodaj komponenty wizualne dla każdej wizyty
        }
    }

}
