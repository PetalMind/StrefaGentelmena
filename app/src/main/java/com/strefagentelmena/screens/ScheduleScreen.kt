package com.strefagentelmena.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fireBase.FirebaseEmployeeFunctions
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.calendarHeader.callendarHeaderUI
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.uiComposable.reusableScreen
import com.strefagentelmena.uiComposable.selectorsUI
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
        val viewState by viewModel.viewState.observeAsState(AppState.Loading)


        when (viewState) {
            AppState.Idle -> {
                LaunchedEffect(Unit) {
                    viewModel.viewState.value = AppState.Success
                    viewModel.loadAllData()

                }
            }

            AppState.Loading -> {
            }

            AppState.Error -> {
                Column {
                    Text(text = "Error", fontSize = 30.sp)
                }
            }

            AppState.Success -> {
                ReservationSuccessContent(
                    navController = navController,
                    viewModel = viewModel,
                )
            }
        }

    }

    /**
     * Appointment Succes Conent.
     *
     * @param navController
     * @param viewModel
     */
    @Composable
    fun ReservationSuccessContent(
        navController: NavController,
        viewModel: ScheduleModelView,
    ) {
        val appointmentDialogState by viewModel.appointmentDialog.observeAsState(false)
        val message by viewModel.messages.observeAsState(null)
        val context = LocalContext.current
        val appointmentDateSelection by viewModel.selectedAppointmentDate.observeAsState(
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        )
        val appointments by viewModel.appointmentsList.observeAsState(emptyList())
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val selectedWorker by viewModel.selectedEmployee.observeAsState()

        // Obecny dzień w miesiącu
        val currentSelectedDay = remember {
            mutableIntStateOf(
                appointmentDateSelection?.let {
                    if (it.isNotEmpty()) sdf.parse(it)?.date
                        ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    else Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                } ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            )
        }

        LaunchedEffect(appointmentDateSelection) {
            currentSelectedDay.intValue = appointmentDateSelection?.let {
                if (it.isNotEmpty()) sdf.parse(it)?.date ?: Calendar.getInstance()
                    .get(Calendar.DAY_OF_MONTH)
                else Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            } ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        }

        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        // Obsługa wiadomości
        LaunchedEffect(message) {
            if (message?.isNotEmpty() == true) {
                scope.launch {
                    snackbarHostState.showSnackbar(message!!, duration = SnackbarDuration.Short)
                    viewModel.clearMessages()
                }
            }
        }

        LaunchedEffect(selectedWorker) {
            if (appointments?.size == (0 ?: 0)) {
                viewModel.loadAllData()
            }

        }


        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = { ReservationTopBar(navController, viewModel, context) },
            floatingActionButton = { AddAppointmentFab(viewModel) },
        ) { innerPadding ->
            ReservationContent(
                innerPadding = innerPadding,
                viewModel = viewModel,
            )
        }

        if (appointmentDialogState) {
            dialogsUI.OnAddOrEditSchedule(viewModel = viewModel)
        }
    }

    @Composable
    private fun ReservationTopBar(
        navController: NavController,
        viewModel: ScheduleModelView,
        context: Context,
    ) {
        headersUI.AppBarWithBackArrow(
            title = "Harmonogram",
            onBackPressed = { navController.navigate("mainScreen") },
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
                                    dateSetListener = {
                                        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                        val date = LocalDate.parse(it, formatter)
                                        viewModel.setNewAppoimentsDate(date)
                                    }
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
            onClick = {}
        )
    }

    @Composable
    private fun AddAppointmentFab(viewModel: ScheduleModelView) {
        buttonsUI.LargeFloatingActionButton(icon = Icons.Default.Add) {
            viewModel.setAppoimentState(true)
            viewModel.clearDate()
            viewModel.showApoimentDialog()
        }
    }

    @Composable
    private fun ReservationContent(
        innerPadding: PaddingValues,
        viewModel: ScheduleModelView,
    ) {
        Column(modifier = Modifier.padding(innerPadding)) {
            callendarHeaderUI.CalendarHeader(
                modifier = Modifier
                    .background(color = colorsUI.babyBlue.copy(0.7f))
                    .fillMaxWidth()
                    .height(130.dp),
                viewModel = viewModel
            )

            ChooseWorker(viewModel)

            AppointmentsList(viewModel) { selectedAppointment ->
                viewModel.selectAppointmentAndClient(selectedAppointment)
                viewModel.setAppoimentState(false)
                viewModel.showApoimentDialog()
            }
        }
    }


    @Composable
    fun ChooseWorker(viewModel: ScheduleModelView) {
        val selectedEmpolyee by viewModel.selectedEmployee.observeAsState()
        val employeeList by viewModel.employeeList.observeAsState(emptyList())

        LaunchedEffect(Unit) {
            if (employeeList.isEmpty()) {
                FirebaseEmployeeFunctions().loadEmployeesFromFirebase(firebaseDatabase = FirebaseDatabase.getInstance())
            } else {
                viewModel.setEmpolyee(employeeList[0])
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            selectorsUI.WorkerSelector(viewModel = viewModel)
        }

    }


    /**
     * Time line with appointments
     *
     * @param appointments
     * @param onClick
     * @param onNotificationClick
     */
    @Composable
    fun TimeLineWithAppointments(
        appointments: List<Appointment>,
        onClick: (Appointment) -> Unit,
        onNotificationClick: (Appointment) -> Unit
    ) {
        LazyColumn {
            itemsIndexed(appointments) { index, appointment ->
                // Konwersja czasu z String na LocalTime
                val startTime =
                    LocalTime.parse(appointment.startTime, DateTimeFormatter.ofPattern("HH:mm"))
                val endTime =
                    LocalTime.parse(appointment.endTime, DateTimeFormatter.ofPattern("HH:mm"))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .width(90.dp)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (index == 0 || LocalTime.parse(appointments[index - 1].startTime) != startTime) {
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
                                            Locale.getDefault(),
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
                                val endTimesAppointment = nextAppointment?.startTime?.let {
                                    LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                                }

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
                                                Locale.getDefault(),
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
                            onNotificationClick = {
                                onNotificationClick(appointment)
                            }
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


    @Composable
    fun AppointmentsList(viewModel: ScheduleModelView, onClick: (Appointment) -> Unit) {
        val appointmentsList by viewModel.appointmentsList.observeAsState(emptyList())
        val selectedDate by viewModel.selectedAppointmentDate.observeAsState(LocalDate.now())
        val notificationDialogState by viewModel.onNotificationClickState.observeAsState(false)
        val selectedAppointment by viewModel.selectedAppointment.observeAsState(null)
        val context = LocalContext.current


        val filteredAppointments = appointmentsList?.filter {
            it.date == selectedDate
        }?.sortedBy {
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
            if (filteredAppointments != null) {
                if (filteredAppointments.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        reusableScreen.EmptyScreen()
                    }
                } else {
                    if (it != null) {
                        TimeLineWithAppointments(
                            appointments = it,
                            onClick = onClick,
                            onNotificationClick = { appointment ->
                                viewModel.selectAppointmentAndClient(appointment)
                                viewModel.showNotificationState()

                            }
                        )
                    }
                }
            }
        }

        if (notificationDialogState) {
            dialogsUI.SendNotificationDialog(
                objectName = selectedAppointment?.customer?.fullName ?: "",
                onConfirm = {
                    viewModel.sendNotificationForAppointment()
                    viewModel.hideNotificationState()
                    viewModel.hideApoimentDialog()
                },
                onDismiss = { viewModel.hideNotificationState() }
            )
        }
    }

}
