package com.strefagentelmena.uiComposable.settingsUI

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.strefagentelmena.R
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.textModernTextFieldUI
import com.strefagentelmena.viewModel.SettingsModelView

val settingsViews = SettingsViews()

class SettingsViews {

    @Composable
    fun ProfileView(viewModel: SettingsModelView) {
        val profileName by viewModel.profileName.observeAsState("użytkownik")
        val context = LocalContext.current

        Column(Modifier.fillMaxSize()) {
            textModernTextFieldUI.ModernTextField(
                value = profileName,
                onValueChange = { it -> viewModel.setUserName(it) },
                modifier = Modifier.padding(10.dp),
                label = "Nazwa użytkownika",
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null
                    )
                }
            )
            buttonsUI.ButtonsRow(
                onClick = {
                    viewModel.saveAllData(context = context)
                    viewModel.setProfileViewState()
                },
                onDismiss = { viewModel.setProfileViewState() },
                containerColor = colorsUI.mintGreen
            )
        }
    }

    @Composable
    fun NotificationView(viewModel: SettingsModelView) {
        val notificationSendStartTime by viewModel.notificationSendStartTime.observeAsState("")
        val notificationSendEndTime by viewModel.notificationSendEndTime.observeAsState("")
        val notificationMessage by viewModel.notificationMessage.observeAsState("")
        val context = LocalContext.current

        Column(Modifier.fillMaxSize()) {
            textModernTextFieldUI.ModernTextField(
                value = notificationSendStartTime,
                onValueChange = { it -> viewModel.setNotificationSendStartTime(it) },
                modifier = Modifier.padding(10.dp),
                label = "Godzina rozpoczęcia wysyłania komunikatów",
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = null
                    )
                },
            )

            textModernTextFieldUI.ModernTextField(
                value = notificationSendEndTime,
                onValueChange = { it -> viewModel.setNotificationSendEndTime(it) },
                modifier = Modifier.padding(10.dp),
                label = "Godzina aakończenia wysyłania komunikatów",
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = null
                    )
                }
            )

            buttonsUI.ButtonsRow(
                onClick = {
                    viewModel.setNotificationSendEndTime(notificationSendEndTime)
                    viewModel.setNotificationSendStartTime(notificationSendStartTime)
                    viewModel.saveAllData(context = context)
                    viewModel.setNotificationViewState()
                },
                onDismiss = { viewModel.setNotificationViewState() },
                containerColor = colorsUI.mintGreen
            )
        }
    }

    @Composable
    fun GreetingsView(viewModel: SettingsModelView) {
        val greetingsMessage by viewModel.greetingsLists.observeAsState("")
        Column(Modifier.fillMaxSize()) {
        }
    }

    @Composable
    fun BackupView(viewModel: SettingsModelView) {
        Column(Modifier.fillMaxSize()) {
        }
    }

    @Composable
    fun UpgradeView(viewModel: SettingsModelView) {
        Column(Modifier.fillMaxSize()) {

        }
    }
}