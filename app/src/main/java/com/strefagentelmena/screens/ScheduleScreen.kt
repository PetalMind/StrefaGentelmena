package com.strefagentelmena.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.strefagentelmena.AgentDebugLog
import com.strefagentelmena.navigation.NavPendingActions
import com.strefagentelmena.navigation.popBackStackOrNavigateToMain
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.appViewStates
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.fireBase.FirebaseEmployeeFunctions
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.effectiveEmployeeId
import com.strefagentelmena.models.appoimentsModel.findFirstSchedulingOverlapPair
import com.strefagentelmena.models.settngsModel.isOnVacationOn
import com.strefagentelmena.uiComposable.StrefaBanner
import com.strefagentelmena.uiComposable.StrefaBannerDensity
import com.strefagentelmena.uiComposable.StrefaBannerVariant
import com.strefagentelmena.models.appoimentsModel.formatOverlapPairBannerDescription
import com.strefagentelmena.uiComposable.StrefaDialogButton
import com.strefagentelmena.uiComposable.StrefaDialogButtonRow
import com.strefagentelmena.uiComposable.StrefaDialogButtonStyle
import com.strefagentelmena.uiComposable.StrefaDialogFloatingBar
import com.strefagentelmena.uiComposable.StrefaDialogNotifyButton
import com.strefagentelmena.uiComposable.StrefaHarmonogramColors
import com.strefagentelmena.uiComposable.StrefaHarmonogramTimelineContent
import com.strefagentelmena.uiComposable.StrefaModalIconFrame
import com.strefagentelmena.uiComposable.StrefaModalIconVariant
import com.strefagentelmena.uiComposable.StrefaModalPanel
import com.strefagentelmena.uiComposable.StrefaModalTitleText
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.uiComposable.selectorsUI
import com.strefagentelmena.ui.theme.SalonGold
import com.strefagentelmena.ui.theme.SalonText
import com.strefagentelmena.viewModel.ScheduleModelView
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
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
        val viewState by viewModel.viewState.observeAsState(AppState.Idle)


        LaunchedEffect(Unit) {
            viewModel.loadAllData()
        }

        when (viewState) {
            AppState.Idle, AppState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    appViewStates.LoadingView(onRetry = { viewModel.loadAllData() })
                }
            }

            AppState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Nie udało się załadować harmonogramu.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = { viewModel.loadAllData() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Spróbuj ponownie")
                        }
                    }
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
        val customersList by viewModel.customersList.observeAsState(emptyList())
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val selectedWorker by viewModel.selectedEmployee.observeAsState()

        // Obecny dzień w miesiącu
        val currentSelectedDay =
            remember { mutableIntStateOf(getCurrentDay(appointmentDateSelection)) }

        LaunchedEffect(appointmentDateSelection) {
            currentSelectedDay.intValue = getCurrentDay(appointmentDateSelection)
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

        LaunchedEffect(customersList) {
            val p = NavPendingActions.takeSchedulePrefillWhenReady(customersList.orEmpty())
                ?: return@LaunchedEffect
            val c = customersList.firstOrNull { it.id == p.customerId && p.customerId > 0 }
                ?: p.fallbackCustomer
            if (c.fullName.isNotBlank() || c.phoneNumber.isNotBlank()) {
                viewModel.openNewAppointmentWithCustomer(c, p.followUpFromAppointment)
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = { ReservationTopBar(navController, viewModel, context) },
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
            onBackPressed = { navController.popBackStackOrNavigateToMain() },
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


    private fun getCurrentDay(dateSelection: String?): Int {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateSelection?.takeIf { it.isNotEmpty() }?.let {
            try {
                sdf.parse(it)?.date ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            } catch (e: ParseException) {
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            }
        } ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    @Composable
    private fun ReservationContent(
        innerPadding: PaddingValues,
        viewModel: ScheduleModelView,
    ) {
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(StrefaHarmonogramColors.Bg),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    ChooseWorker(viewModel)
                }
            }

            AppointmentsList(viewModel) { selectedAppointment ->
                viewModel.selectAppointmentAndClient(selectedAppointment)
                viewModel.setAppoimentState(false)
                viewModel.changeAppointmentDialogState()
            }
        }
    }


    @Composable
    fun ChooseWorker(viewModel: ScheduleModelView) {
        val selectedEmpolyee by viewModel.selectedEmployee.observeAsState()
        val employeeList by viewModel.employeeList.observeAsState(emptyList())
        val appointmentDate by viewModel.selectedAppointmentDate.observeAsState()

        LaunchedEffect(employeeList, appointmentDate) {
            when (employeeList.size) {
                0 -> FirebaseEmployeeFunctions().loadEmployeesFromFirebase(firebaseDatabase = FirebaseDatabase.getInstance())

                else -> {
                    val date = appointmentDate.orEmpty()
                    val selected = selectedEmpolyee
                    if (selected?.id == null) {
                        val firstOk = employeeList.firstOrNull { !it.isOnVacationOn(date) }
                            ?: employeeList.first()
                        viewModel.setEmpolyee(firstOk)
                    } else {
                        viewModel.ensureSelectedEmployeeAvailableForCurrentDate()
                        viewModel.getsAppoiments()
                    }
                }
            }
        }

        selectorsUI.WorkerSelector(viewModel = viewModel)
    }


    @Composable
    fun AppointmentsList(viewModel: ScheduleModelView, onClick: (Appointment) -> Unit) {
        val appointmentsList by viewModel.appointmentsList.observeAsState(emptyList())
        val allUnfiltered by viewModel.allAppointmentsUnfiltered.observeAsState(emptyList())
        val selectedWorker by viewModel.selectedEmployee.observeAsState()
        val todayFormatted = remember {
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }
        val selectedDateString by viewModel.selectedAppointmentDate.observeAsState(todayFormatted)
        val notificationDialogState by viewModel.onNotificationClickState.observeAsState(false)
        val resendAfterEdit by viewModel.resendNotificationAfterEdit.observeAsState(null)
        val selectedAppointment by viewModel.selectedAppointment.observeAsState(null)
        val appointmentDialog by viewModel.appointmentDialog.observeAsState(false)

        val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

        val sortedAppointments = remember(appointmentsList) {
            appointmentsList.orEmpty().sortedBy { it.startTime }
        }

        val selectedLocalDate = remember(selectedDateString) {
            runCatching { LocalDate.parse(selectedDateString, dateFormatter) }.getOrElse { LocalDate.now() }
        }

        val dayStripDates = remember(selectedLocalDate) {
            (-5..10).map { selectedLocalDate.plusDays(it.toLong()) }
        }

        val daysWithAppointments = remember(allUnfiltered, selectedWorker?.id) {
            val wid = selectedWorker?.id ?: return@remember emptySet()
            allUnfiltered
                .filter { it.effectiveEmployeeId() == wid }
                .mapNotNull { ap ->
                    runCatching { LocalDate.parse(ap.date.trim(), dateFormatter) }.getOrNull()
                }
                .toSet()
        }

        LaunchedEffect(selectedDateString) {
            viewModel.getsAppoiments()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            val overlapPair = sortedAppointments.findFirstSchedulingOverlapPair()
            if (overlapPair != null) {
                val (a, b) = overlapPair
                StrefaBanner(
                    variant = StrefaBannerVariant.Warning,
                    title = "Wizyty się nakładają",
                    description = formatOverlapPairBannerDescription(a, b),
                    density = StrefaBannerDensity.Compact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 4.dp),
                )
            }
            StrefaHarmonogramTimelineContent(
                modifier = Modifier.weight(1f),
                selectedDate = selectedLocalDate,
                dayStripDates = dayStripDates,
                daysWithAppointments = daysWithAppointments,
                onSelectDate = { viewModel.setNewAppoimentsDate(it) },
                onAddClick = {
                    viewModel.setAppoimentState(true)
                    viewModel.clearDate()
                    viewModel.changeAppointmentDialogState()
                },
                addButtonLabel = "Dodaj wizyte",
                appointments = sortedAppointments,
                isViewingToday = selectedLocalDate == LocalDate.now(),
                onAppointmentClick = onClick,
                onNotificationClick = { appointment ->
                    viewModel.selectAppointmentAndClient(appointment)
                    viewModel.openNotificationDialog()
                },
            )
        }

        if (notificationDialogState) {
            SendSmsConfirmationStrefaDialog(
                clientName = selectedAppointment?.customer?.fullName ?: "",
                onConfirm = {
                    viewModel.sendNotificationForAppointment()
                    viewModel.dismissNotificationDialog()
                    if (appointmentDialog) {
                        viewModel.changeAppointmentDialogState()
                    }
                },
                onDismiss = { viewModel.dismissNotificationDialog() },
            )
        }

        resendAfterEdit?.let { edited ->
            SendSmsConfirmationStrefaDialog(
                clientName = edited.customer.fullName,
                labelText = "Czy wysłać ponownie powiadomienie SMS z zaktualizowanymi danymi wizyty?",
                onConfirm = { viewModel.confirmResendNotificationAfterEdit() },
                onDismiss = { viewModel.dismissResendNotificationAfterEdit() },
            )
        }
    }

    @Composable
    private fun SendSmsConfirmationStrefaDialog(
        clientName: String,
        labelText: String = "Czy chcesz wyslac SMS?",
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StrefaModalPanel {
                        StrefaModalIconFrame(
                            variant = StrefaModalIconVariant.Warning,
                            icon = Icons.Outlined.Notifications,
                            iconTint = SalonGold,
                        )
                        StrefaModalTitleText(
                            text = labelText,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = clientName,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = SalonText,
                            textAlign = TextAlign.Center,
                        )
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        StrefaDialogButtonRow(
                            first = { m ->
                                StrefaDialogButton(
                                    text = "Nie",
                                    onClick = onDismiss,
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Ghost,
                                )
                            },
                            second = { m ->
                                StrefaDialogNotifyButton(
                                    text = "Tak",
                                    onClick = onConfirm,
                                    modifier = m,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

}
