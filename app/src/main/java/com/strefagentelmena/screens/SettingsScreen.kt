package com.strefagentelmena.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.strefagentelmena.R
import com.strefagentelmena.appViewStates
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.uiComposable.SettingsUI.settingsUiElements
import com.strefagentelmena.uiComposable.SettingsUI.settingsViews
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.viewModel.SettingsModelView

val settingsScreen = SettingsScreen()

class SettingsScreen {
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun SettingsView(viewModel: SettingsModelView, navController: NavController) {
        val viewState by viewModel.viewState.observeAsState(AppState.Idle)

        val context = LocalContext.current



        when (viewState) {
            AppState.Idle -> {
                viewModel.closeAllStates()
                viewModel.loadAllData(context = context)
            }

            AppState.Loading -> {
                appViewStates.LoadingView()
            }

            AppState.Error -> {}
            AppState.Success -> {
                SettingsSuccessView(viewModel, navController)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun SettingsSuccessView(viewModel: SettingsModelView, navController: NavController) {
        val snackbarHostState = SnackbarHostState()
        val profileViewState by viewModel.profileViewState.observeAsState(false)
        val notificationViewState by viewModel.notificationViewState.observeAsState(false)
        val greetingsViewState by viewModel.greetingsViewState.observeAsState(false)
        val backButtonViewState by viewModel.backButtonViewState.observeAsState(false)
        val updateViewState by viewModel.updateViewState.observeAsState(false)


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
                Column {
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
                        icon = R.drawable.ic_forum,
                        text = "Wiadomości powitalne",
                        onClick = {
                            viewModel.setGreetingsViewState()
                        },
                        expandedComposable = {
                            settingsViews.GreetingsView(viewModel = viewModel)
                        },
                        expandedState = greetingsViewState
                    )

                    settingsUiElements.SettingsItem(
                        icon = R.drawable.ic_backup,
                        text = "Kopia zapasowa",
                        onClick = {
                            viewModel.setBackButtonViewState()
                        },
                        expandedComposable = {
                            settingsViews.BackupView(viewModel = viewModel)
                        },
                        expandedState = backButtonViewState
                    )

                    settingsUiElements.SettingsItem(
                        icon = R.drawable.ic_upgrade,
                        text = "Aktualizacja",
                        onClick = {
                            viewModel.setUpdateViewState()
                        },
                        expandedComposable = {
                            settingsViews.UpgradeView(viewModel = viewModel)
                        },
                        expandedState = updateViewState
                    )
                }
            }
        }
    }
}