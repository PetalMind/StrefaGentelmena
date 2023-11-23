package com.strefagentelmena.screens

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.strefagentelmena.R
import com.strefagentelmena.appViewStates
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.smsManager
import com.strefagentelmena.ui.theme.StrefaGentelmenaTheme
import com.strefagentelmena.uiComposable.PopUpDialogs
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.footerUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.viewModel.DashboardModelView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val screenDashboard = Dashboard()

class Dashboard {
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun PermissionAwareComponent() {
        val smsPermissionState = rememberPermissionState(Manifest.permission.SEND_SMS)

        if (!smsPermissionState.hasPermission && !smsPermissionState.shouldShowRationale) {
            // Wyświetl pytanie o uprawnienia, nawet jeśli wcześniej zostały one odmówione
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                buttonsUI.CustomTextButton(
                    text = "Udziel uprawnień do SMS",
                    onClick = { smsPermissionState.launchPermissionRequest() },
                    width = 250.dp
                )
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun DashboardView(
        navController: NavController,
        viewModel: DashboardModelView,
    ) {
        val context = LocalContext.current
        val viewState by viewModel.viewState.observeAsState(AppState.Idle)
        val clientsToNotify by viewModel.appointmentsToNotify.observeAsState(emptyList())

        when (viewState) {
            AppState.Idle -> {
                viewModel.loadAllData(context = context)
            }

            AppState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    appViewStates.LoadingView()
                }
            }

            AppState.Error -> {

            }

            AppState.Success -> {
                DashboardSuccessView(navController, viewModel)
            }

            else -> {
                appViewStates.LoadingView()
            }
        }


//        try {
//            scheduleViewModel.loadAppointmentFromFile(context)
//            viewModel.loadCustomersFromFile(context = context)
//            dashboardViewModel.clearMessage()
//        } catch (e: Exception) {
//            dashboardViewModel.newMessage("Błąd podczas wczytywania plików!")
//        }

        // Efekt wyzwalany, gdy wartość 'messages' się zmienia
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun DashboardSuccessView(
        navController: NavController,
        viewModel: DashboardModelView,
    ) {
        val messages by viewModel.messages.observeAsState("")
        val showNotifyDialog by viewModel.showNotifyDialog.observeAsState(false)
        val clientsToNotify by viewModel.appointmentsToNotify.observeAsState(emptyList())
        val greetingRandom by viewModel.displayGreetings.observeAsState("")

        val currentDay = remember {
            mutableStateOf(
                LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault()))
            )
        }

        val context = LocalContext.current

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
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }


        LaunchedEffect(messages) {
            if (messages.isNotEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar(messages, duration = SnackbarDuration.Short)
                    viewModel.clearMessage()
                }
            }
        }

        LaunchedEffect(clientsToNotify, Unit) {
            viewModel.loadAllData(context = context)

            if (clientsToNotify.isNotEmpty()) {
                viewModel.showNotifyDialog.value = true
            }
        }

        // Aktualizacja aktualnego czasu co 30 sekund
        LaunchedEffect(currentTimeString) {
            while (true) {
                delay(15000L)

                currentTimeString.value =
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

                viewModel.sendNotificationsForUpcomingAppointments(
                    currentDay.value
                )
            }
        }

        StrefaGentelmenaTheme(dynamicColor = false, darkTheme = false) {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
                                )
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            DashboardHeaderGreetings(
                                currentTimeString = currentTimeString,
                                currentStringDate = currentStringDate,
                                currentDayAndMonth = currentDayAndMonth,
                                greeting = greetingRandom,
                            )
                        }

                        headersUI.LogoHeader()

                        PermissionAwareComponent()

                        Row {
                            cardUI.DashboardSmallCard(iconId = R.drawable.ic_clients,
                                labelText = viewModel.customersLists.value?.size.toString(),
                                nameText = "Klienci Salonu",
                                onClick = { navController.navigate("AddCustomer") })

                            cardUI.DashboardSmallCard(
                                iconId = R.drawable.ic_events, labelText = "", onClick = {
                                    navController.navigate("schedule")
                                }, nameText = "Plan Dnia"
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        footerUI.AppFooter(context = context)
                    }
                }
                if (showNotifyDialog) {
                    PopUpDialogs().NotifyDialog(
                        onClick = {
                            clientsToNotify.forEach {
                                smsManager.sendNotification(
                                    it,
                                )

                                viewModel.editAppointment(context, it, true)
                            }
                            viewModel.hideNotifyDialog()

                        },
                        onDismissRequest = {
                            viewModel.hideNotifyDialog()
                        },
                        clientCountString = clientsToNotify.size.toString(),
                        appoiments = clientsToNotify
                    )
                }
            }
        }
    }


    @Composable
    fun DashboardHeaderGreetings(
        greeting: String,
        currentTimeString: MutableState<String>,
        currentStringDate: MutableState<String>,
        currentDayAndMonth: MutableState<String>,
    ) {
        val randomGreeting = remember { mutableStateOf(greeting) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorsUI.teaGreen,
                    shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
                )
        ) {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeString.value,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = colorsUI.fontGrey
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = currentStringDate.value,
                        style = MaterialTheme.typography.titleLarge,
                        color = colorsUI.fontGrey
                    )
                    Text(
                        text = ", ${currentDayAndMonth.value}",
                        style = MaterialTheme.typography.titleLarge,
                        color = colorsUI.fontGrey
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = randomGreeting.value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorsUI.fontGrey
                )
            }
        }
    }
}
