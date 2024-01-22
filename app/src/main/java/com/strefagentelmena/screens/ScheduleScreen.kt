package com.strefagentelmena.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.calendarHeader.callendarHeaderUI
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.uiComposable.reusableScreen
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
    @Composable
    fun AppointmentSuccesConent(
        navController: NavController,
        viewModel: ScheduleModelView,
    ) {
        val showApoimentDialog by viewModel.showAppointmentDialog.observeAsState(false)
        val message by viewModel.messages.observeAsState("")
        val context = LocalContext.current
        val currentSelectedAppoinmentsDate by viewModel.selectedAppointmentDate.observeAsState(
            LocalDate.now().format(
                DateTimeFormatter.ofPattern(
                    "dd.MM.yyyy"
                )
            )
        )
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        val currentSelectedDay = remember {
            mutableIntStateOf(currentSelectedAppoinmentsDate?.let {
                if (it.isNotEmpty()) sdf.parse(it)?.date ?: Calendar.getInstance()
                    .get(Calendar.DAY_OF_MONTH)
                else Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            } ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
        }


        LaunchedEffect(currentSelectedAppoinmentsDate) {
            currentSelectedDay.intValue = currentSelectedAppoinmentsDate?.let {
                if (it.isNotEmpty()) sdf.parse(it)?.date ?: Calendar.getInstance()
                    .get(Calendar.DAY_OF_MONTH)
                else Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            } ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
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
                    title = "Harmonogram",
                    onBackPressed = {
                        navController.navigate("mainScreen")
                    },
                    compose = {
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(colorsUI.headersBlue, RoundedCornerShape(15.dp))
                                    .clip(RoundedCornerShape(15.dp))
                                    .padding(10.dp)
                                    .clickable {
                                        dialogsUI.showDatePickerDialog(
                                            context = context,
                                            dateSetListener = { it ->
                                                val formatter =
                                                    DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                                val date = LocalDate.parse(it, formatter)
                                                viewModel.setNewAppoimentsDate(date)
                                            },
                                        )
                                    }
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    onClick = {
                    },
                )
            },
            floatingActionButton = {
                buttonsUI.LargeFloatingActionButton(icon = Icons.Default.Add) {
                    viewModel.setAppoimentState(true)
                    viewModel.clearDate()
                    viewModel.showApoimentDialog()
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                callendarHeaderUI.CalendarApp(
                    modifier = Modifier
                        .background(color = colorsUI.babyBlue.copy(0.7f))
                        .fillMaxWidth()
                        .height(130.dp),
                    viewModel = viewModel
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
    @Composable
    fun TimeLineWithAppointments(
        appointments: List<Appointment>,
        onClick: (Appointment) -> Unit,
    ) {
        LazyColumn {
            itemsIndexed(appointments) { index, appointment ->
                val startTime = appointment.startTime
                val endTime = appointment.endTime

                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .width(90.dp)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (index == 0 || appointments[index - 1].startTime != appointment.startTime) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row {
                                    Canvas(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .align(Alignment.CenterVertically)
                                            .background(Color.Gray, CircleShape)
                                    ) {
                                        drawCircle(
                                            color = colorsUI.mintGreen,
                                            radius = size.width / 2,
                                            center = Offset(size.width / 2, size.height / 2)
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
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                val nextAppointment =
                                    if (index < appointments.size - 1) appointments[index + 1] else null
                                val endTimesAppointment = nextAppointment?.startTime

                                val intervals =
                                    generateTimeIntervals(startTime, endTimesAppointment ?: endTime)
                                intervals.forEach { interval ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
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
                                            color = if (interval == endTime) Color.Black else Color.Gray,
                                            fontWeight = if (interval == endTime) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(
                                                start = 12.dp,
                                                bottom = 4.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier) {
                        cardUI.CustomerAppoimentListCard(
                            appointment,
                            onClick = { onClick(appointment) },
                            onNotificationClick = {}
                        )
                    }
                }
            }
        }
    }

    private fun generateTimeIntervals(start: LocalTime, end: LocalTime): List<LocalTime> {
        val intervals = mutableListOf<LocalTime>()
        var current = start.plusMinutes(15) // Start at the first 15 minute interval
        while (current.isBefore(end) || current.equals(end)) {
            intervals.add(current)
            current = current.plusMinutes(15) // Add 15 minutes to the current time
        }
        return intervals
    }


    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun AppointmentsList(viewModel: ScheduleModelView, onClick: (Appointment) -> Unit) {
        val appointmentsList by viewModel.appointmentsList.observeAsState(emptyList())
        val selectedDate by viewModel.selectedAppointmentDate.observeAsState(LocalDate.now())
        val notificationDialogState by viewModel.onNotificationClickState.observeAsState(false)
        val selectedAppointment by viewModel.selectedAppointment.observeAsState(null)
        val context = LocalContext.current

        val filteredAppointments = appointmentsList.filter {
            it.date == selectedDate
        }.sortedBy {
            it.startTime
        }
        AnimatedContent(targetState = filteredAppointments, label = "", transitionSpec = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(300, easing = LinearEasing)
            ).togetherWith(
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(300, easing = LinearEasing)
                )
            )
        }) {
            if (filteredAppointments.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    reusableScreen.EmptyScreen()
                }
            } else {
                TimeLineWithAppointments(
                    appointments = it,
                    onClick = onClick,
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
                onDismiss = { viewModel.hideNotificationState() }
            )
        }
    }

}
