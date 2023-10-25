package com.strefagentelmena.screens

import android.os.Build
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
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.models.Appointment
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.viewModel.CustomersModelView
import com.strefagentelmena.viewModel.DashboardModelView
import com.strefagentelmena.viewModel.ScheduleModelView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

val screenSchedule = Schedule()

class Schedule() {

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun scheduleView(
        customersModelView: CustomersModelView,
        navController: NavController,
        viewModel: ScheduleModelView,
        dashboardModelView: DashboardModelView,
    ) {
        val appointments by dashboardModelView.appointmentsLists.observeAsState(emptyList())
        val showApoimentDialog by viewModel.showAppointmentDialog.observeAsState(false)
        val message by viewModel.messages.observeAsState("")
        val context = LocalContext.current
        val viewState by viewModel.viewState.observeAsState(AppState.Idle)


        when (viewState) {
            AppState.Idle -> {
                LaunchedEffect(Unit) {
                    viewModel.loadAllData(context = context)
                }
            }

            AppState.Loading -> {

            }

            AppState.Error -> {

            }

            AppState.Success -> {
                AppointmentSuccesConent(
                    customersModelView = customersModelView,
                    navController = navController,
                    viewModel = viewModel,
                    dashboardModelView = dashboardModelView
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
        customersModelView: CustomersModelView,
        navController: NavController,
        viewModel: ScheduleModelView,
        dashboardModelView: DashboardModelView,
    ) {
        val appointments by dashboardModelView.appointmentsLists.observeAsState(emptyList())
        val showApoimentDialog by viewModel.showAppointmentDialog.observeAsState(false)
        val message by viewModel.messages.observeAsState("")
        val context = LocalContext.current
        val currentSelectedAppoinmentsDate by viewModel.currentSelectedAppoinmentsDate.observeAsState(
            ""
        )
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        val currentSelectedDay: Int = currentSelectedAppoinmentsDate?.let {
            sdf.parse(it)?.date ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        } ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        // Inicjalizacja stanu Scaffold
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }


        val isNew = remember {
            mutableStateOf(false)
        }

        val typeOfView = remember {
            mutableStateOf("list")
        }

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
            dashboardModelView.loadAppointmentFromFile(context = context)
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
                                viewModel.currentSelectedAppoinmentsDate.value = it
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
                    customersModelView.selectedCustomer.value = null
                    isNew.value = true
                    viewModel.showApoimentDialog()
                }
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                //  headersUI.CalendarHeaderView(viewModel)

                headersUI.CalendarHeader(
                    onDaySelected = {
                        viewModel.currentSelectedAppoinmentsDate.value = it.toString()
                    },
                    currentDay = currentSelectedDay,
                )

                AppointmentsList(viewModel) { selectedAppointment ->
                    viewModel.selectAppointment(selectedAppointment)
                    customersModelView.selectedCustomer.value = selectedAppointment.customer
                    isNew.value = false

                    viewModel.showApoimentDialog()
                }
            }
        }

        if (showApoimentDialog) {
            dialogsUI.onAddOrEditSchedule(
                viewModel = viewModel,
                customersViewModel = customersModelView,
                isNew = isNew.value,
                dashboardModelView = dashboardModelView
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun TimeLineWithAppointments(
        appointments: List<Appointment>,
        onClick: (Appointment) -> Unit,
    ) {
        // Helper function to generate time intervals
        fun generateTimeIntervals(start: LocalTime, end: LocalTime): List<LocalTime> {
            val intervals = mutableListOf<LocalTime>()
            var current = start.plusMinutes(15)  // Start at the first 30 minute interval
            while (current.isBefore(end)) {
                intervals.add(current)
                current = current.plusMinutes(15)  // Add 30 minutes to the current time
            }
            return intervals
        }

        LazyColumn {
            itemsIndexed(appointments) { index, appointment ->
                val startTime =
                    LocalTime.parse(appointment.startTime, DateTimeFormatter.ofPattern("HH:mm"))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Draw hours, vertical time line, and circle
                    Column {
                        // Add additional 30 minute time intervals if not the last appointment
                        Row(
                            modifier = Modifier
                                .width(80.dp)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically  // Vertically center the contents
                        ) {

                            if (index == 0 || appointments[index - 1].startTime != appointment.startTime) {
                                Column {
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
                                                color = colorsUI.rusticBrown,
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
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                //     .align(Alignment.CenterVertically)  // Vertically center align the text
                                                .padding(start = 8.dp)  // Add some padding to separate the text from the line and circle
                                        )
                                    }
                                    if (index < appointments.size - 1) {
                                        val nextAppointment = appointments[index + 1]
                                        val endTime = LocalTime.parse(
                                            nextAppointment.startTime,
                                            DateTimeFormatter.ofPattern("HH:mm")
                                        )
                                        val intervals = generateTimeIntervals(startTime, endTime)
                                        intervals.forEach { interval ->
                                            Row(
                                                modifier = Modifier
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.Start,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
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
                    // Draw appointment card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        cardUI.CustomerAppoimentListCard(appointment) {
                            onClick(appointment)
                        }
                    }
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
        val appointmentsList by viewModel.appointments.observeAsState(emptyList())
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
                onClick = onClick
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun TimeSlotsView(viewModel: ScheduleModelView) {
        val appointmentsList by viewModel.appointments.observeAsState(emptyList())
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

        Box() {
            // Iteruj przez `filteredAppointments` i dodaj komponenty wizualne dla każdej wizyty
        }
    }

}
