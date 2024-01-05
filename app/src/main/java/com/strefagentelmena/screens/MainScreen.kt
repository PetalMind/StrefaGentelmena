package com.strefagentelmena.screens

import android.Manifest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.FirebaseApp
import com.strefagentelmena.R
import com.strefagentelmena.appViewStates
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.greetingsManager
import com.strefagentelmena.functions.smsManager
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import com.strefagentelmena.ui.theme.StrefaGentelmenaTheme
import com.strefagentelmena.uiComposable.PopUpDialogs
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.viewModel.MainScreenModelView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val mainScreen = MainScreen()

class MainScreen {
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun PermissionAwareComponents() {
        val lifecycleOwner = LocalLifecycleOwner.current
        val permissionState = rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.SEND_SMS, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        DisposableEffect(key1 = lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                       permissionState.launchMultiplePermissionRequest()
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        permissionState.permissions.forEach {
            when(it.permission) {
                Manifest.permission.SEND_SMS -> {
                }

                Manifest.permission.WRITE_EXTERNAL_STORAGE -> {

                }
            }
        }
    }


    @Composable
    fun DashboardView(
        navController: NavController,
        viewModel: MainScreenModelView,
    ) {
        val context = LocalContext.current
        val viewState by viewModel.viewState.observeAsState(AppState.Idle)
        val dataLoaded by viewModel.dataLoaded.observeAsState(false)

        LaunchedEffect(dataLoaded) {
            if (dataLoaded) {
                viewModel.setViewState(AppState.Success)
            }
        }

        LaunchedEffect(Unit) {
            viewModel.setViewState(AppState.Idle)
            viewModel.setDataLoaded(false)
        }



        when (viewState) {
            AppState.Idle -> {
                viewModel.loadData(context = context)
            }

            AppState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    appViewStates.LoadingView()
                }

            }

            AppState.Error -> {
                viewModel.setViewState(AppState.Idle)
            }

            AppState.Success -> {
                DashboardSuccessView(navController, viewModel)
            }

            else -> {
                viewModel.setViewState(AppState.Idle)
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
        val upcomingAppointment by viewModel.upcomingAppointment.observeAsState(Appointment())
        val profilePreference by viewModel.profilePreferences.observeAsState(ProfilePreferences())
        val customersList by viewModel.customersLists.observeAsState(emptyList())
        val greetingRandom by viewModel.displayGreetings.observeAsState(
            greetingsManager.randomGreeting(
                profilePreference.userName
            )
        )

        val context = LocalContext.current

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

        LaunchedEffect(clientsToNotify) {
            if (clientsToNotify.isNotEmpty()) {
                viewModel.showNotifyDialog()
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
                                greeting = greetingRandom,
                                onSettingsClick = {
                                    navController.navigate("settingsScreen")
                                }
                            )
                        }

                        headersUI.LogoHeader()

                        PermissionAwareComponents()

                        if (upcomingAppointment?.id != 0) {
                            upcomingAppointment?.let { cardUI.UpcomingClientCard(appointment = it) }
                        }

                        Row {
                            cardUI.DashboardSmallCard(iconId = R.drawable.ic_clients,
                                labelText = customersList.size.toString(),
                                nameText = "Klienci Salonu",
                                onClick = { navController.navigate("customersScreen") })

                            cardUI.DashboardSmallCard(
                                iconId = R.drawable.ic_events, labelText = "", onClick = {
                                    navController.navigate("scheduleScreen")
                                }, nameText = "Harmonogram"
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                if (showNotifyDialog) {
                    if (profilePreference.notificationSendAutomatic) {
                        clientsToNotify.forEach {
                            smsManager.sendNotification(
                                it, profile = profilePreference
                            )

                            viewModel.editAppointment(context, it, true)
                        }

                        viewModel.newMessage("Wysłano powiadomienia do ${if (clientsToNotify.size == 1) "1 klienta" else "${clientsToNotify.size} klientów"}")

                        viewModel.setAppointmentsToNotify(emptyList())
                        viewModel.hideNotifyDialog()
                    } else {
                        PopUpDialogs().NotifyDialog(
                            onClick = {
                                clientsToNotify.forEach {

                                    smsManager.sendNotification(
                                        it, profile = profilePreference
                                    )

                                    viewModel.editAppointment(context, it, true)
                                }
                                viewModel.newMessage("Wysłano powiadomienia do ${if (clientsToNotify.size == 1) "1 klienta" else "${clientsToNotify.size} klientów"}")
                                viewModel.setAppointmentsToNotify(emptyList())
                                viewModel.hideNotifyDialog()
                            },
                            onDismissRequest = {
                                viewModel.setAppointmentsToNotify(emptyList())
                                viewModel.hideNotifyDialog()
                            },
                            clientCountString = clientsToNotify.size.toString(),
                            appoiments = clientsToNotify
                        )
                    }
                }
            }
        }
    }


    @Composable
    fun DashboardHeaderGreetings(
        greeting: String,
        onSettingsClick: () -> Unit,
    ) {
        val randomGreeting = remember { mutableStateOf(greeting) }
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

        LaunchedEffect(greeting) {
            randomGreeting.value = greeting
        }

        // Aktualizacja aktualnego czasu co 5 sekund
        LaunchedEffect(currentTimeString) {
            while (true) {
                delay(5000L)

                currentTimeString.value =
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorsUI.teaGreen,
                    shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = currentTimeString.value,
                            label = "",
                            transitionSpec = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                                    animationSpec = tween(300, easing = LinearEasing)
                                ).togetherWith(
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = tween(300, easing = LinearEasing)
                                    )
                                )
                            }) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                ),
                                color = colorsUI.fontGrey
                            )
                        }

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

                    AnimatedContent(
                        targetState = randomGreeting.value,
                        label = "",
                        transitionSpec = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Down,
                                animationSpec = tween(300, easing = LinearEasing)
                            ).togetherWith(
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                                    animationSpec = tween(300, easing = LinearEasing)
                                )
                            )
                        }) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorsUI.fontGrey
                        )
                    }
                }
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    Box(
                        modifier = Modifier
                            .background(colorsUI.headersBlue, RoundedCornerShape(15.dp))
                            .clip(RoundedCornerShape(15.dp))
                            .padding(10.dp)
                            .clickable {
                                onSettingsClick()
                            }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
