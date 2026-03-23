package com.strefagentelmena.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.strefagentelmena.navigation.popBackStackOrNavigateToMain
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
    fun SettingsView(
        viewModel: SettingsModelView,
        navController: NavController,
        isDarkTheme: Boolean,
        onThemeChange: (Boolean) -> Unit,
    ) {
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
                SettingsSuccessView(
                    viewModel = viewModel,
                    navController = navController,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                )
            }
        }
    }

    @Composable
    fun SettingsSuccessView(
        viewModel: SettingsModelView,
        navController: NavController,
        isDarkTheme: Boolean,
        onThemeChange: (Boolean) -> Unit,
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val profilePreferences by viewModel.profilePreferences.observeAsState()
        val profileViewState by viewModel.profileViewState.observeAsState(false)
        val notificationViewState by viewModel.notificationViewState.observeAsState(false)
        val empolyeeViewState by viewModel.empolyeeViewState.observeAsState(false)
        val backButtonViewState by viewModel.backButtonViewState.observeAsState(false)
        val updateViewState by viewModel.updateViewState.observeAsState(false)
        val backupViewState by viewModel.backupViewState.observeAsState(false)
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
                        navController.popBackStackOrNavigateToMain()
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
                    .fillMaxSize()
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
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

                    settingsUiElements.SettingsItem(
                        icon = R.drawable.ic_backup,
                        text = "Kopia zapasowa",
                        onClick = {
                            viewModel.setBackupViewState()
                        },
                        expandedComposable = {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                settingsViews.BackupView(viewModel = viewModel)
                            }
                        },
                        expandedState = backupViewState
                    )

                    settingsUiElements.SettingsItem(
                        icon = R.drawable.ic_upgrade,
                        text = "Wygląd",
                        onClick = {
                            viewModel.setUpdateViewState()
                        },
                        expandedComposable = {
                            settingsUiElements.CustomSwitch(
                                checked = isDarkTheme,
                                onCheckedChange = onThemeChange,
                                text = "Tryb ciemny",
                            )
                        },
                        expandedState = updateViewState
                    )
                }
            }
        }
    }
}