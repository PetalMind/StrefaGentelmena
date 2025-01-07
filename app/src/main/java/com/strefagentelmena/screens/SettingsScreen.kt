package com.strefagentelmena.screens

import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.strefagentelmena.R
import com.strefagentelmena.appViewStates
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.uiComposable.settingsUI.settingsViews
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.uiComposable.settingsUI.settingsUiElements
import com.strefagentelmena.viewModel.SettingsModelView
import kotlinx.coroutines.launch

val settingsScreen = SettingsScreen()

class SettingsScreen {
    @Composable
    fun SettingsView(viewModel: SettingsModelView, navController: NavController) {
        val viewState by viewModel.viewState.observeAsState(AppState.Idle)
        val profilePreferences by viewModel.profilePreferences.observeAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.closeAllStates()
            viewModel.viewState.value = AppState.Idle
        }


        when (viewState) {
            AppState.Idle -> {
                viewModel.loadAllData()
            }

            AppState.Loading -> {
                appViewStates.LoadingView(onRetry = {
                    viewModel.loadAllData()
                })
            }

            AppState.Error -> {

            }

            AppState.Success -> {
                SettingsSuccessView(viewModel, navController)
            }
        }
    }

    @Composable
    fun SettingsSuccessView(viewModel: SettingsModelView, navController: NavController) {
        val snackbarHostState = SnackbarHostState()
        val profilePreferences by viewModel.profilePreferences.observeAsState()
        val profileViewState by viewModel.profileViewState.observeAsState(false)
        val notificationViewState by viewModel.notificationViewState.observeAsState(false)
        val empolyeeViewState by viewModel.empolyeeViewState.observeAsState(false)
        val backButtonViewState by viewModel.backButtonViewState.observeAsState(false)
        val updateViewState by viewModel.updateViewState.observeAsState(false)
        val message by viewModel.messages.observeAsState("")
        val scope = rememberCoroutineScope()

        LaunchedEffect(message) {
            if (message.isNotEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                    viewModel.clearMessages()
                }
            }
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                headersUI.AppBarWithBackArrow(
                    title = "Ustawienia",
                    onBackPressed = {
                        navController.navigate("mainScreen")
                    },
                    compose = {
                    },
                    onClick = {
                    },
                )
            },

            ) { it ->
            Surface(
                modifier = Modifier
                    .padding(it)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()) // Dodanie scrollowania
                ) {
                    settingsUiElements.SettingsItem(
                        icon = R.drawable.ic_person,
                        text = "Użytkownik",
                        onClick = {
                            viewModel.setProfileViewState()
                        },
                        expandedComposable = {
                            settingsViews.ProfileView(viewModel = viewModel)
                        },
                        expandedState = profileViewState
                    )

                    settingsUiElements.SettingsItem(
                        icon = R.drawable.ic_notification,
                        text = "Powiadomienia",
                        onClick = {
                            viewModel.setNotificationViewState()
                        },
                        expandedComposable = {
                            settingsViews.NotificationView(viewModel = viewModel)
                        },
                        expandedState = notificationViewState
                    )

                    settingsUiElements.SettingsItem(
                        icon = R.drawable.ic_person,
                        text = "Pracownicy",
                        onClick = {
                            viewModel.setEmpolyeeViewState()
                        },
                        expandedComposable = {
                            settingsViews.EmployeeView(viewModel = viewModel)
                        },
                        expandedState = empolyeeViewState
                    )
                }
            }
        }
    }
}