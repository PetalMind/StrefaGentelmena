package com.strefagentelmena.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
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
import com.google.accompanist.permissions.shouldShowRationale
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.R
import com.strefagentelmena.appViewStates
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.Greetings
import com.strefagentelmena.functions.fireBase.FirebaseFunctionsAppointments
import com.strefagentelmena.functions.smsManager
import com.strefagentelmena.models.Customer
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
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_EXTERNAL_STORAGE // Ogranicz do API 29+
            )
        )

        DisposableEffect(key1 = lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
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
        val dataLoaded by viewModel.dataLoaded.observeAsState(false)

        LaunchedEffect(dataLoaded) {
            if (dataLoaded) {
                viewModel.setViewState(AppState.Success)
            } else {
                viewModel.setViewState(AppState.Error)
            }
        }

        LaunchedEffect(Unit) {
            viewModel.setViewState(AppState.Idle)
            viewModel.setDataLoaded(false)
        }



        when (viewState) {
            AppState.Idle -> {
                viewModel.startLoadingData()
                viewModel.clearMessage()
            }

            AppState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    appViewStates.LoadingView(onRetry = {
                        viewModel.startLoadingData()
                    })
                }

            }

            AppState.Error -> {
                viewModel.setViewState(AppState.Loading)
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
        val upcomingAppointment by viewModel.upcomingAppointment.observeAsState(null)
        val profilePreference by viewModel.profilePreferences.observeAsState(null)
        val customersList by viewModel.customersLists.observeAsState(emptyList())

        val greetingRandom by viewModel.displayGreetings.observeAsState(
            Greetings().getSeasonalAndPartOfDayGreeting(profilePreference?.userName ?: "Użytkownik")
        )

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        FirebaseApp.initializeApp(context)


        HandleMessages(messages, snackbarHostState, viewModel)
        HandleNotifyDialog(viewModel, clientsToNotify)

        UpdateGreetingOnProfileChange(profilePreference, viewModel)

        StrefaGentelmenaTheme(dynamicColor = false, darkTheme = false) {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
                DashboardContent(
                    navController = navController,
                    paddingValues = paddingValues,
                    greetingRandom = greetingRandom,
                    customersList = customersList,
                    upcomingAppointment = upcomingAppointment
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
    }

    @Composable
    private fun HandleMessages(
        messages: String,
        snackbarHostState: SnackbarHostState,
        viewModel: MainScreenModelView
    ) {
        val scope = rememberCoroutineScope()
        LaunchedEffect(messages) {
            if (messages.isNotEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar(messages, duration = SnackbarDuration.Short)
                    viewModel.clearMessage()
                }
            }
        }
    }

    @Composable
    private fun HandleNotifyDialog(
        viewModel: MainScreenModelView,
        clientsToNotify: List<Appointment>
    ) {
        LaunchedEffect(clientsToNotify) {
            if (clientsToNotify.isNotEmpty()) {
                viewModel.setViewNotifyDialog()
            } else {
                viewModel.checkAppointments()
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
        customersList: List<Customer>,
        upcomingAppointment: Appointment?
    ) {
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
                        onSettingsClick = { navController.navigate("settingsScreen") }
                    )
                }

                headersUI.LogoHeader()
                PermissionAwareComponents()

                if (upcomingAppointment?.id != 0) {
                    upcomingAppointment?.let { cardUI.UpcomingClientCard(appointment = it) }
                }

                Row {
                    cardUI.DashboardSmallCard(
                        iconId = R.drawable.ic_clients,
                        labelText = customersList.size.toString(),
                        nameText = "Klienci Salonu",
                        onClick = { navController.navigate("customersScreen") }
                    )

                    cardUI.DashboardSmallCard(
                        iconId = R.drawable.ic_events,
                        labelText = "",
                        onClick = { navController.navigate("scheduleScreen") },
                        nameText = "Harmonogram"
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
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
            // Automatyczne wysyłanie powiadomień
            clientsToNotify.forEach { appointment ->
                try {
                    // Wysyłanie powiadomienia SMS
                    profilePreference.let { profile ->
                        smsManager.sendNotification(appointment, profile)
                    }

                    // Aktualizacja statusu powiadomienia
                    val updatedAppointment = appointment.copy(notificationSent = true)

                    FirebaseFunctionsAppointments().editAppointmentInFirebase(
                        firebaseDatabase = FirebaseDatabase.getInstance(),
                        updatedAppointment = updatedAppointment
                    ) { success ->
                        if (!success) {
                            val message = "Nie udało się zaktualizować powiadomienia dla klienta ${appointment.customer.fullName}"
                            viewModel.newMessage(message)
                        }
                    }
                } catch (e: Exception) {
                    val errorMessage = "Błąd wysyłania SMS do ${appointment.customer.fullName}: ${e.message}"
                    viewModel.newMessage(errorMessage)
                }
            }
        } else {
            // Ręczne potwierdzenie za pomocą dialogu
            PopUpDialogs().NotifyDialog(
                onClick = {
                    clientsToNotify.forEach { appointment ->
                        try {
                            // Wysyłanie powiadomienia SMS
                            profilePreference?.let { profile ->
                                smsManager.sendNotification(appointment, profile)
                            }

                            // Aktualizacja statusu powiadomienia
                            val updatedAppointment = appointment.copy(notificationSent = true)
                            FirebaseFunctionsAppointments().editAppointmentInFirebase(
                                firebaseDatabase = FirebaseDatabase.getInstance(),
                                updatedAppointment = updatedAppointment
                            ) { success ->
                                if (!success) {
                                    val message = "Nie udało się zaktualizować powiadomienia dla klienta ${appointment.customer.fullName}"
                                    viewModel.newMessage(message)
                                }
                            }
                        } catch (e: Exception) {
                            val errorMessage = "Błąd wysyłania SMS do ${appointment.customer.fullName}: ${e.message}"
                            viewModel.newMessage(errorMessage)
                        }
                    }

                    // Powiadomienie o zakończeniu procesu
                    viewModel.newMessage("Wysłano powiadomienia do ${clientsToNotify.size} klientów")
                    viewModel.setViewNotifyDialog() // Zakończenie dialogu po wysłaniu
                },
                onDismissRequest = {
                    // Odroczenie na 5 minut
                    viewModel.deferNotifyDialog(5)
                    viewModel.setViewNotifyDialog()
                },
                clientCountString = clientsToNotify.size.toString(),
                appoiments = clientsToNotify
            )
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorsUI.fontGrey
                        )
                    }
                }
                Box(modifier = Modifier.padding(end = 25.dp)) {
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
