package com.strefagentelmena.uiComposable.settingsUI

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.app
import com.strefagentelmena.functions.fireBase.storageFireBase
import com.strefagentelmena.models.settngsModel.BackupSettings
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
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
                },
                isError = profileName.isEmpty(),
                supportText = "Nazwa użytkownika nie może być pusta"

            )
            buttonsUI.ButtonsRow(
                onClick = {
                    viewModel.saveAllData(context = context)
                    viewModel.setProfileViewState()
                },
                onDismiss = { viewModel.setProfileViewState() },
                containerColor = colorsUI.mintGreen,
                buttonEnabled = profileName.isNotEmpty()
            )
        }
    }

    @Composable
    fun NotificationView(viewModel: SettingsModelView) {
        val notificationSendStartTime by viewModel.notificationSendStartTime.observeAsState("")
        val notificationSendEndTime by viewModel.notificationSendEndTime.observeAsState("")
        val notificationSendAutomatic by viewModel.notificationSendAutomatic.observeAsState(false)

        val context = LocalContext.current

        Column(
            Modifier
                .fillMaxSize()
        ) {
            textModernTextFieldUI.TimeOutlinedTextField(
                value = notificationSendStartTime,
                onValueChange = { it -> viewModel.setNotificationSendStartTime(it) },
                modifier = Modifier.padding(10.dp),
                label = "Godzina rozpoczęcia wysyłania powiadomienia",
            )

            textModernTextFieldUI.TimeOutlinedTextField(
                value = notificationSendEndTime,
                onValueChange = { it -> viewModel.setNotificationSendEndTime(it) },
                modifier = Modifier.padding(10.dp),
                label = "Godzina zakonczenia wysyłania powiadomienia",
            )

            settingsUiElements.CustomSwitch(
                checked = notificationSendAutomatic,
                onCheckedChange = {
                    viewModel.setAutomaticNotificationViewState(it)
                },
                text = "Automatycznie wysyłaj powiadomienia",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )


            buttonsUI.ButtonsRow(
                onClick = {
                    viewModel.saveAllData(context = context)
                    viewModel.setNotificationViewState()
                },
                onDismiss = { viewModel.setNotificationViewState() },
                containerColor = colorsUI.mintGreen
            )
        }
    }

    @Composable
    fun BackupView(viewModel: SettingsModelView) {
        val context = LocalContext.current
        val backupSettings by viewModel.backupPrefecences.observeAsState(BackupSettings())
        val isBackupCreated by viewModel.isBackupCreated.observeAsState(false)
        val customBackupPreferences by viewModel.backupCustom.observeAsState(false)
        val backupCustomers by viewModel.backupCustomers.observeAsState(false)
        val backupAppoiments by viewModel.backupAppoiments.observeAsState(false)
        val backupAutomatic by viewModel.backupAutomatic.observeAsState(false)
        val hasOnlineCopy by viewModel.hasOnlineCopy.observeAsState(false)
        val isBackupOnline by viewModel.isBackupOnline.observeAsState(false)
        val onlineBackupLists by viewModel.onlineBackupLists.observeAsState(
            mutableListOf()
        )

        LaunchedEffect(isBackupOnline) {
            if (isBackupOnline) {
                viewModel.downloadBackupFiles()
            }
        }


        Column(
            Modifier
                .fillMaxSize()
        ) {
            Column {
                Text(
                    "Utwórz kopie zapasową",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                /*
                                if (FirebaseApp.getApps(context).isNotEmpty()) {
                                    settingsUiElements.CustomSwitch(
                                        checked = hasOnlineCopy,
                                        onCheckedChange = {
                                            viewModel.setHasOnlineCopy(it)
                                        },
                                        text = "Pozwól na kopię online",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    )
                                }
                */
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    buttonsUI.PrimaryButton(
                        text = "Utwórz kopie",
                        onClick = {
                            viewModel.createBackup(context = context)
                        },
                        containerColor =
                        colorsUI.mintGreen,
                    )
                }
            }

            Column {
                Text(
                    "Odtwórz kopie",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                /*
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    settingsUiElements.CustomSwitch(
                        checked = isBackupOnline,
                        onCheckedChange = {
                            viewModel.setIsBackupOnline(it)
                        },
                        text = "Odwtórz kopię online",
                        modifier =F Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
*/
                Row(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Ostatnia utworzona kopie zapasowa: ",
                    )

                    Text(
                        text = backupSettings.latestBackupDate.ifBlank { "Brak" },
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    buttonsUI.PrimaryButton(
                        text = "Odtwórz kopie",
                        onClick = {
                            if (isBackupOnline) {
                                viewModel.setBackupOnlineDialog(true)
                            } else {
                                viewModel.loadBackup(context)

                            }
                        },
                        containerColor = colorsUI.headersBlue
                    )
                }
            }
        }
    }

    @Composable
    fun UpgradeView(viewModel: SettingsModelView) {
        Column(Modifier.fillMaxSize()) {

        }
    }
}