package com.strefagentelmena.screens

import android.Manifest
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.R
import com.strefagentelmena.appViewStates
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.Greetings
import com.strefagentelmena.functions.appFunctions
import com.strefagentelmena.functions.fireBase.FirebaseFunctionsAppointments
import com.strefagentelmena.functions.smsManager
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.effectiveCustomerId
import com.strefagentelmena.models.normalizedForFirebaseWrite
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import com.strefagentelmena.navigation.NavPendingActions
import com.strefagentelmena.navigation.Screen
import com.strefagentelmena.uiComposable.PopUpDialogs
import com.strefagentelmena.uiComposable.StrefaBanner
import com.strefagentelmena.uiComposable.StrefaBannerDensity
import com.strefagentelmena.uiComposable.StrefaBannerVariant
import com.strefagentelmena.uiComposable.StrefaDialogButton
import com.strefagentelmena.uiComposable.StrefaDialogButtonStyle
import com.strefagentelmena.uiComposable.StrefaDialogFloatingBar
import com.strefagentelmena.uiComposable.StrefaDialogOptionList
import com.strefagentelmena.uiComposable.StrefaDialogOptionRow
import com.strefagentelmena.uiComposable.StrefaModalBodyText
import com.strefagentelmena.uiComposable.StrefaModalPanel
import com.strefagentelmena.uiComposable.StrefaModalTitleText
import com.strefagentelmena.uiComposable.WeeklyVisitsBarChart
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.ui.theme.SalonBg3
import com.strefagentelmena.ui.theme.SalonGold
import com.strefagentelmena.ui.theme.SalonMuted
import com.strefagentelmena.ui.theme.SalonText
import com.strefagentelmena.ui.theme.SalonWrapBg
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.viewModel.MainScreenModelView
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

val mainScreen = MainScreen()

class MainScreen {
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun PermissionAwareComponents() {
        val lifecycleOwner = LocalLifecycleOwner.current
        val permissionState = rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_EXTERNAL_STORAGE // Ogranicz do API 29+
            )
        )

        DisposableEffect(key1 = lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && !permissionState.allPermissionsGranted) {
                    permissionState.launchMultiplePermissionRequest()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }


    @Composable
    fun DashboardView(
        navController: NavController,
        viewModel: MainScreenModelView,
    ) {
        val viewState by viewModel.viewState.observeAsState(AppState.Idle)

        LaunchedEffect(viewState) {
            if (viewState == AppState.Idle) {
                viewModel.clearMessage()
                viewModel.startLoadingData()
            }
        }

        when (viewState) {
            AppState.Idle, AppState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    appViewStates.LoadingView(onRetry = {
                        viewModel.startLoadingData()
                    })
                }
            }

            AppState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    appViewStates.LoadingView(
                        message = "Nie udało się załadować danych. Sprawdź połączenie.",
                        onRetry = { viewModel.startLoadingData() }
                    )
                }
            }

            AppState.Success -> {
                DashboardSuccessView(navController, viewModel)
            }
        }
    }

    @Composable
    fun DashboardSuccessView(
        navController: NavController,
        viewModel: MainScreenModelView,
    ) {
        val messages by viewModel.messages.observeAsState("")
        val showNotifyDialog by viewModel.showNotifyDialog.observeAsState(false)
        val clientsToNotify by viewModel.appointmentsToNotify.observeAsState(emptyList())
        val upcomingAppointment by viewModel.upcomingAppointment.observeAsState(null)
        val profilePreference by viewModel.profilePreferences.observeAsState(null)
        val customersList by viewModel.customersLists.observeAsState(emptyList())
        val ongoingVisits by viewModel.ongoingVisitsNow.observeAsState(emptyList())
        val recentVisits24h by viewModel.recentVisitsLast24h.observeAsState(emptyList())
        val extendVisitBusy by viewModel.extendVisitInProgress.observeAsState(false)
        val todayApptCount by viewModel.todayAppointmentsCount.observeAsState(0)
        val vacationReminder by viewModel.vacationDashboardReminder.observeAsState("")
        val weekVisitCounts by viewModel.weekVisitCountsByDay.observeAsState(List(7) { 0 })

        val greetingRandom by viewModel.displayGreetings.observeAsState(
            Greetings().getSeasonalAndPartOfDayGreeting(profilePreference?.userName ?: "Użytkownik")
        )

        val snackbarHostState = remember { SnackbarHostState() }

        HandleMessages(messages, snackbarHostState, viewModel)
        HandleNotifyDialog(viewModel, clientsToNotify)

        LaunchedEffect(Unit) {
            while (true) {
                delay(60_000)
                viewModel.refreshDashboardTimeSlots()
            }
        }

        UpdateGreetingOnProfileChange(profilePreference, viewModel)

        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
            DashboardContent(
                navController = navController,
                paddingValues = paddingValues,
                greetingRandom = greetingRandom,
                profileUserName = profilePreference?.userName.orEmpty().trim(),
                customersList = customersList,
                scheduleTodayCount = todayApptCount,
                weekVisitCountsByDay = weekVisitCounts,
                upcomingAppointment = upcomingAppointment,
                ongoingVisits = ongoingVisits,
                recentVisitsLast24h = recentVisits24h,
                extendVisitBusy = extendVisitBusy,
                onExtendVisitMinutes = { appt, minutes ->
                    viewModel.extendVisitByMinutes(appt, minutes)
                },
                onUserMessage = { viewModel.newMessage(it) },
                vacationReminder = vacationReminder,
            )

            if (showNotifyDialog) {
                NotifyDialogHandler(
                    profilePreference = profilePreference,
                    clientsToNotify = clientsToNotify,
                    viewModel = viewModel
                )
            }
        }
    }

    @Composable
    private fun HandleMessages(
        messages: String,
        snackbarHostState: SnackbarHostState,
        viewModel: MainScreenModelView
    ) {
        LaunchedEffect(messages) {
            if (messages.isNotEmpty()) {
                snackbarHostState.showSnackbar(messages, duration = SnackbarDuration.Short)
                viewModel.clearMessage()
            }
        }
    }

    @Composable
    private fun HandleNotifyDialog(
        viewModel: MainScreenModelView,
        clientsToNotify: List<Appointment>
    ) {
        val deferUntil by viewModel.deferNotificationUntil.observeAsState(null)

        LaunchedEffect(clientsToNotify) {
            if (clientsToNotify.isEmpty()) {
                viewModel.checkAppointments()
            }
        }

        LaunchedEffect(clientsToNotify, deferUntil) {
            if (clientsToNotify.isEmpty()) return@LaunchedEffect

            val until = deferUntil
            if (until != null) {
                val now = LocalDateTime.now()
                if (now.isBefore(until)) {
                    val millis = Duration.between(now, until).toMillis().coerceAtLeast(0L)
                    delay(millis)
                }
            }

            if (viewModel.shouldShowNotifyDialog()) {
                viewModel.setNotifyDialogVisible(true)
            }
        }
    }

    @Composable
    private fun UpdateGreetingOnProfileChange(
        profilePreference: ProfilePreferences?,
        viewModel: MainScreenModelView
    ) {
        LaunchedEffect(profilePreference) {
            viewModel.displayGreetings.value = Greetings().getSeasonalAndPartOfDayGreeting(
                profilePreference?.userName ?: "Użytkownik"
            )
        }
    }

    @Composable
    private fun DashboardContent(
        navController: NavController,
        paddingValues: PaddingValues,
        greetingRandom: String,
        profileUserName: String,
        customersList: List<Customer>,
        scheduleTodayCount: Int,
        weekVisitCountsByDay: List<Int>,
        upcomingAppointment: Appointment?,
        ongoingVisits: List<Appointment>,
        recentVisitsLast24h: List<Appointment>,
        extendVisitBusy: Boolean,
        onExtendVisitMinutes: (Appointment, Int) -> Unit,
        onUserMessage: (String) -> Unit,
        vacationReminder: String,
    ) {
        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isTabletLandscape = isLandscape && configuration.screenWidthDp >= 840
        var recentVisitMenuAppointment by remember { mutableStateOf<Appointment?>(null) }
        val todayWeekIndex = LocalDate.now().dayOfWeek.value - 1
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = SalonWrapBg
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                headersUI.DashboardHeaderGreetings(
                    greeting = greetingRandom,
                    subtitle = profileUserName,
                    onSettingsClick = { navController.navigate(Screen.SettingsScreen.route) },
                    compact = isLandscape,
                )

                val scrollState = rememberScrollState()
                var recentVisitsExpanded by rememberSaveable { mutableStateOf(false) }
                val recentPreviewLimit = 5
                val totalRecent = recentVisitsLast24h.size
                val hasMoreRecent = totalRecent > recentPreviewLimit
                val shownRecent = when {
                    !hasMoreRecent || recentVisitsExpanded -> recentVisitsLast24h
                    else -> recentVisitsLast24h.take(recentPreviewLimit)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(
                            horizontal = if (isTabletLandscape) 24.dp else 20.dp,
                            vertical = if (isTabletLandscape) 14.dp else 20.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PermissionAwareComponents()

                    if (vacationReminder.isNotBlank()) {
                        StrefaBanner(
                            variant = StrefaBannerVariant.Info,
                            title = "Urlopy pracowników",
                            description = vacationReminder,
                            density = StrefaBannerDensity.Compact,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    ongoingVisits.forEach { visit ->
                        cardUI.CurrentVisitExtendCard(
                            appointment = visit,
                            extendBusy = extendVisitBusy,
                            onExtendMinutes = { minutes -> onExtendVisitMinutes(visit, minutes) },
                            onAddFollowUpVisit = {
                                NavPendingActions.requestScheduleFollowUpVisit(visit)
                                navController.navigate(Screen.ScheduleScreen.route)
                            },
                        )
                    }

                    if (upcomingAppointment?.id != 0) {
                        upcomingAppointment?.let { cardUI.UpcomingClientCard(appointment = it) }
                    }

                    if (isTabletLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                cardUI.DashboardBrandStatCard(
                                    iconId = R.drawable.ic_clients,
                                    bigNumber = customersList.size.toString(),
                                    labelText = "Klienci salonu",
                                    onClick = { navController.navigate(Screen.CustomersScreen.route) },
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                cardUI.DashboardBrandStatCard(
                                    iconId = R.drawable.ic_events,
                                    bigNumber = scheduleTodayCount.toString(),
                                    labelText = "Harmonogram — dziś",
                                    onClick = { navController.navigate(Screen.ScheduleScreen.route) },
                                )
                            }
                        }
                    } else {
                        cardUI.DashboardBrandStatCard(
                            iconId = R.drawable.ic_clients,
                            bigNumber = customersList.size.toString(),
                            labelText = "Klienci salonu",
                            onClick = { navController.navigate(Screen.CustomersScreen.route) },
                        )

                        cardUI.DashboardBrandStatCard(
                            iconId = R.drawable.ic_events,
                            bigNumber = scheduleTodayCount.toString(),
                            labelText = "Harmonogram — dziś",
                            onClick = { navController.navigate(Screen.ScheduleScreen.route) },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { navController.navigate(Screen.StatisticsScreen.route) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Pokaż statystyki",
                            color = SalonGold,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    WeeklyVisitsBarChart(
                        counts = weekVisitCountsByDay,
                        todayDayIndex = todayWeekIndex,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ostatnie wizyty (do 24 h)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SalonText,
                    )
                    if (recentVisitsLast24h.isEmpty()) {
                        Text(
                            text = "Brak zakończonych wizyt w tym oknie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SalonMuted,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    } else {
                        if (isTabletLandscape) {
                            shownRecent.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    rowItems.forEach { appt ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            cardUI.RecentPastVisitCard(
                                                appointment = appt,
                                                onClick = { recentVisitMenuAppointment = appt },
                                            )
                                        }
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            shownRecent.forEach { appt ->
                                cardUI.RecentPastVisitCard(
                                    appointment = appt,
                                    onClick = { recentVisitMenuAppointment = appt },
                                )
                            }
                        }
                        if (hasMoreRecent) {
                            FilledTonalButton(
                                onClick = { recentVisitsExpanded = !recentVisitsExpanded },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SalonBg3,
                                    contentColor = SalonGold,
                                ),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = if (recentVisitsExpanded) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (recentVisitsExpanded) {
                                            "Zwiń listę"
                                        } else {
                                            "Pokaż więcej (+${totalRecent - recentPreviewLimit})"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        recentVisitMenuAppointment?.let { appt ->
            RecentVisitActionDialog(
                customerName = appt.customer.fullName.ifBlank { "Klient" },
                onDismiss = { recentVisitMenuAppointment = null },
                onEditClient = {
                    recentVisitMenuAppointment = null
                    val id = appt.effectiveCustomerId()
                    if (id > 0) {
                        NavPendingActions.requestOpenCustomerEditor(id)
                        navController.navigate(Screen.CustomersScreen.route)
                    } else {
                        onUserMessage("Brak przypisanego ID klienta — użyj listy klientów.")
                    }
                },
                onAddVisit = {
                    recentVisitMenuAppointment = null
                    NavPendingActions.requestScheduleNewVisit(
                        appt.effectiveCustomerId(),
                        appt.customer,
                    )
                    navController.navigate(Screen.ScheduleScreen.route)
                },
                onDial = {
                    recentVisitMenuAppointment = null
                    val phone = appt.customer.phoneNumber.trim()
                    if (phone.isEmpty()) {
                        onUserMessage("Brak numeru telefonu.")
                    } else {
                        appFunctions.dialPhoneNumber(context, phone)
                    }
                },
                onSms = {
                    recentVisitMenuAppointment = null
                    val phone = appt.customer.phoneNumber.trim()
                    if (phone.isEmpty()) {
                        onUserMessage("Brak numeru telefonu.")
                    } else {
                        appFunctions.openSmsComposer(context, phone)
                    }
                },
            )
        }
    }

    @Composable
    private fun RecentVisitActionDialog(
        customerName: String,
        onDismiss: () -> Unit,
        onEditClient: () -> Unit,
        onAddVisit: () -> Unit,
        onDial: () -> Unit,
        onSms: () -> Unit,
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
                        StrefaModalTitleText("Co chcesz zrobić?")
                        StrefaModalBodyText(text = customerName)
                        StrefaDialogOptionList {
                            StrefaDialogOptionRow(
                                title = "Edytuj klienta",
                                onClick = onEditClient,
                                leadingIcon = Icons.Outlined.Person,
                            )
                            StrefaDialogOptionRow(
                                title = "Dodaj wizytę",
                                onClick = onAddVisit,
                                leadingIcon = Icons.Default.DateRange,
                            )
                            StrefaDialogOptionRow(
                                title = "Zadzwoń",
                                onClick = onDial,
                                leadingIcon = Icons.Outlined.Phone,
                            )
                            StrefaDialogOptionRow(
                                title = "Napisz SMS",
                                onClick = onSms,
                            )
                        }
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        StrefaDialogButton(
                            text = "Anuluj",
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            style = StrefaDialogButtonStyle.Ghost,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun NotifyDialogHandler(
        profilePreference: ProfilePreferences?,
        clientsToNotify: List<Appointment>,
        viewModel: MainScreenModelView
    ) {
        if (profilePreference?.notificationSendAutomatic == true) {
            LaunchedEffect(
                clientsToNotify,
                profilePreference?.userName,
                profilePreference?.notificationSendAutomatic
            ) {
                val profile = profilePreference ?: return@LaunchedEffect
                if (!profile.notificationSendAutomatic) return@LaunchedEffect
                clientsToNotify.forEach { appointment ->
                    notifyClientAndPersistToFirebase(appointment, profile, viewModel)
                }
                viewModel.setNotifyDialogVisible(false)
                viewModel.clearAppointmentsToNotify()
            }
        } else {
            PopUpDialogs().NotifyDialog(
                onClick = {
                    profilePreference?.let { profile ->
                        clientsToNotify.forEach { appointment ->
                            notifyClientAndPersistToFirebase(appointment, profile, viewModel)
                        }
                    }
                    viewModel.newMessage("Wysłano powiadomienia do ${clientsToNotify.size} klientów")
                    viewModel.setNotifyDialogVisible(false)
                    viewModel.clearAppointmentsToNotify()
                },
                onDismissRequest = {
                    viewModel.deferNotifyDialog(5)
                    viewModel.setNotifyDialogVisible(false)
                },
                clientCountString = clientsToNotify.size.toString(),
                appoiments = clientsToNotify
            )
        }
    }

    private fun notifyClientAndPersistToFirebase(
        appointment: Appointment,
        profile: ProfilePreferences,
        viewModel: MainScreenModelView
    ) {
        val sent = try {
            smsManager.sendNotification(
                appointment,
                profile,
                viewModel.resolveSmsContactPhoneForAppointment(appointment),
            )
        } catch (e: Exception) {
            viewModel.newMessage(
                "Błąd wysyłania SMS do ${appointment.customer.fullName}: ${e.message}"
            )
            return
        }

        if (!sent) {
            viewModel.newMessage(
                "Nie wysłano SMS do ${appointment.customer.fullName} (poza oknem godzin, zły numer lub błąd operatora)."
            )
            return
        }

        val updatedAppointment = appointment.copy(notificationSent = true).normalizedForFirebaseWrite()
        FirebaseFunctionsAppointments().editAppointmentInFirebase(
            firebaseDatabase = FirebaseDatabase.getInstance(),
            updatedAppointment = updatedAppointment
        ) { success ->
            if (!success) {
                viewModel.newMessage(
                    "Nie udało się zaktualizować powiadomienia dla klienta ${appointment.customer.fullName}"
                )
            }
        }
    }
}
