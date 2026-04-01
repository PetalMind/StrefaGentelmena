@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.strefagentelmena.uiComposable.settingsUI

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.textModernTextFieldUI
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.viewModel.SettingsModelView
import com.strefagentelmena.work.ScheduledBackupScheduler

val settingsViews = SettingsViews()

class SettingsViews {

    companion object {
        /** Dwie zaplanowane kopie dziennie (6:00 i 22:00) — wywołaj przy starcie aplikacji. */
        fun scheduleAutomaticDatabaseBackups(context: Context) {
            ScheduledBackupScheduler.scheduleTwiceDaily(context.applicationContext)
        }
    }

    @Composable
    fun ProfileView(viewModel: SettingsModelView) {
        val profilePreferences by viewModel.profilePreferences.observeAsState()
        val newProfileName by viewModel.profileName.observeAsState("")

        LaunchedEffect(profilePreferences) {
            Log.e("ProfileView", "profilePreferences: ${profilePreferences!!.userName}")
        }

        Column(Modifier.fillMaxWidth()) {
            textModernTextFieldUI.ModernTextField(
                value = newProfileName,
                onValueChange = { it -> viewModel.setNewProfileName(it) },
                modifier = Modifier.padding(10.dp),
                label = "Nazwa użytkownika",
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
                isError = profilePreferences!!.userName.isEmpty(),
                supportText = "Nazwa użytkownika nie może być pusta"

            )

            buttonsUI.ButtonsRow(
                onClick = {
                    viewModel.saveAllData()
                    viewModel.setProfileViewState()
                },
                onDismiss = { viewModel.setProfileViewState() },
                containerColor = colorsUI.mintGreen,
                buttonEnabled = profilePreferences!!.userName.isNotEmpty()
            )
        }
    }

    @Composable
    fun EmployeeView(viewModel: SettingsModelView) {
        val employeesList by viewModel.empoleesList.observeAsState()
        val isAddingEmployee by viewModel.addEmpolyeeState.observeAsState()
        val deleteEmployeeDialog by viewModel.deleteEmployeeDialog.observeAsState(false)

        Column(Modifier.fillMaxWidth()) {
            when (isAddingEmployee) {
                true -> {
                    EmpolyeeAddView(viewModel)
                }

                false -> {
                    EmpolyeeListView(viewModel)
                }

                null -> {
                    Column {
                        Text(
                            "Lista pracowników jest pusta",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        if (deleteEmployeeDialog == true) {
            dialogsUI.DeleteDialog(
                objectName = "${viewModel.selectedEmployee.value?.name} ${viewModel.selectedEmployee.value?.surname}",
                onConfirm = {
                    viewModel.deleteEmpolyee(viewModel.selectedEmployee.value!!)

                    viewModel.setEmpolyeeDeleteDialog()
                },
                onDismiss = { viewModel.setEmpolyeeDeleteDialog() },
                labelName = "Czy chcesz usunąć pracownika "
            )
        }
    }

    @Composable
    fun EmpolyeeListView(viewModel: SettingsModelView) {
        val employeeList by viewModel.empoleesList.observeAsState()
        var showAddEmployeeView by remember { mutableStateOf(false) } // State for add view visibility
        val isNewEmplyee by viewModel.isNewEmplyee.observeAsState(false)

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lista pracowników",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                buttonsUI.IconButton(
                    onClick = {
                        viewModel.setIsNewEmplyee(true)
                        viewModel.setAddNewEmployeeState()
                    },
                    icon = Icons.Outlined.Add,
                    modifier = Modifier.padding(20.dp)
                )
            }

            if (showAddEmployeeView) {
                EmpolyeeAddView(viewModel)
            } else {
                // Column zamiast LazyColumn — rodzic ma verticalScroll; zagnieżdżone listy źle liczą wysokość.
                Column(Modifier.fillMaxWidth()) {
                    employeeList.orEmpty().forEach { employee ->
                        cardUI.SwipeToDismissEmployeeCard(
                            employee = employee,
                            onClick = {
                                viewModel.setIsNewEmplyee(false)
                                viewModel.setAddNewEmployeeState()
                                viewModel.setSlectedEmpolyee(employee)
                            },
                            onDismiss = {
                                viewModel.setSlectedEmpolyee(employee)
                                viewModel.setEmpolyeeDeleteDialog()
                            },
                            onEdit = {
                                viewModel.setIsNewEmplyee(false)
                                viewModel.setAddNewEmployeeState()
                                viewModel.setSlectedEmpolyee(employee)
                            },
                        )
                    }
                }
            }
        }
    }


    @Composable
    fun EmpolyeeAddView(viewModel: SettingsModelView) {
        val empolyeeName by viewModel.empolyeeName.observeAsState()
        val empolyeeSurname by viewModel.empolyeeSurname.observeAsState()
        val empolyeeWorkStart by viewModel.empolyeeWorkStartTime.observeAsState(Employee.DEFAULT_WORK_START)
        val empolyeeWorkEnd by viewModel.empolyeeWorkEndTime.observeAsState(Employee.DEFAULT_WORK_END)
        val empolyeeVacationFrom by viewModel.empolyeeVacationFrom.observeAsState("")
        val empolyeeVacationTo by viewModel.empolyeeVacationTo.observeAsState("")
        val vacationWholeDay by viewModel.empolyeeVacationWholeDay.observeAsState(true)
        val empolyeeVacationTimeFrom by viewModel.empolyeeVacationTimeFrom.observeAsState("")
        val empolyeeVacationTimeTo by viewModel.empolyeeVacationTimeTo.observeAsState("")
        val newEmpolyee by viewModel.newEmployee.observeAsState()
        val addNewEmpolyeeState by viewModel.addEmpolyeeState.observeAsState()
        val isNewEmplyee by viewModel.isNewEmplyee.observeAsState()
        val text = if (isNewEmplyee == true) "Dodaj nowego pracownika" else "Edytuj pracownika"

        LaunchedEffect(addNewEmpolyeeState) {
            if (isNewEmplyee == true) {
                viewModel.clearNewEmpolyee()
            }
        }

        Column(Modifier.fillMaxWidth()) {
            Text(
                text,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            textModernTextFieldUI.ModernTextField(
                value = empolyeeName ?: "",
                onValueChange = { it -> viewModel.setEmpolyeeName(it) },
                modifier = Modifier.padding(10.dp),
                label = "Imię pracowika",
            )

            textModernTextFieldUI.ModernTextField(
                value = empolyeeSurname ?: "",
                onValueChange = { it -> viewModel.setEmpolyeeSurname(it) },
                modifier = Modifier.padding(10.dp),
                label = "Nazwisko pracowika",
            )

            textModernTextFieldUI.TimeOutlinedTextField(
                value = empolyeeWorkStart ?: Employee.DEFAULT_WORK_START,
                onValueChange = { viewModel.setEmpolyeeWorkStartTime(it) },
                modifier = Modifier.padding(10.dp),
                label = "Początek pracy (harmonogram)",
            )

            textModernTextFieldUI.TimeOutlinedTextField(
                value = empolyeeWorkEnd ?: Employee.DEFAULT_WORK_END,
                onValueChange = { viewModel.setEmpolyeeWorkEndTime(it) },
                modifier = Modifier.padding(10.dp),
                label = "Koniec pracy (harmonogram)",
            )

            Text(
                text = "Urlop / wolne (blokuje zapisy w harmonogramie)",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Zakres dat — dzień początku wymagany, koniec pusty = jeden dzień. Przy „wybrane godziny” ten sam przedział obowiązuje każdego dnia w zakresie.",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            textModernTextFieldUI.DateOutlinedTextField(
                value = empolyeeVacationFrom.orEmpty(),
                onValueChange = { viewModel.setEmpolyeeVacationFrom(it) },
                modifier = Modifier.padding(10.dp),
                label = "Od dnia (dd.MM.yyyy)",
            )
            textModernTextFieldUI.DateOutlinedTextField(
                value = empolyeeVacationTo.orEmpty(),
                onValueChange = { viewModel.setEmpolyeeVacationTo(it) },
                modifier = Modifier.padding(10.dp),
                label = "Do dnia (opcjonalnie)",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = vacationWholeDay,
                    onClick = { viewModel.setEmpolyeeVacationWholeDay(true) },
                    label = { Text("Cały dzień") },
                )
                FilterChip(
                    selected = !vacationWholeDay,
                    onClick = { viewModel.setEmpolyeeVacationWholeDay(false) },
                    label = { Text("Wybrane godziny") },
                )
            }
            if (!vacationWholeDay) {
                textModernTextFieldUI.TimeOutlinedTextField(
                    value = empolyeeVacationTimeFrom.orEmpty(),
                    onValueChange = { viewModel.setEmpolyeeVacationTimeFrom(it) },
                    modifier = Modifier.padding(10.dp),
                    label = "Wolne od (HH:mm)",
                )
                textModernTextFieldUI.TimeOutlinedTextField(
                    value = empolyeeVacationTimeTo.orEmpty(),
                    onValueChange = { viewModel.setEmpolyeeVacationTimeTo(it) },
                    modifier = Modifier.padding(10.dp),
                    label = "Wolne do (HH:mm)",
                )
            }

            buttonsUI.ButtonsRow(
                onClick = {
                    if (isNewEmplyee == true) {
                        viewModel.addNewEmployee()
                    } else {
                        viewModel.editEmployee(viewModel.selectedEmployee.value!!)
                    }
                    viewModel.setAddNewEmployeeState()
                },
                onDismiss = { viewModel.setAddNewEmployeeState() },
            )
        }
    }

    @Composable
    fun NotificationView(viewModel: SettingsModelView) {
        val notificationSendStartTime by viewModel.notificationSendStartTime.observeAsState("")
        val notificationSendEndTime by viewModel.notificationSendEndTime.observeAsState("")
        val notificationSendAutomatic by viewModel.notificationSendAutomatic.observeAsState()

        Column(Modifier.fillMaxWidth()) {
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
            )/*
                        settingsUiElements.CustomSwitch(
                            checked = notificationSendAutomatic ?: false,
                            onCheckedChange = {
                                viewModel.setAutomaticNotificationViewState(it)
                            },
                            text = "Automatycznie wysyłaj powiadomienia",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
            */

            buttonsUI.ButtonsRow(
                onClick = {
                    viewModel.saveAllData()
                    viewModel.setNotificationViewState()
                },
                onDismiss = { viewModel.setNotificationViewState() },
                containerColor = colorsUI.mintGreen
            )
        }
    }


    @Composable
    fun UpgradeView(viewModel: SettingsModelView) {
        Column(Modifier.fillMaxWidth()) {

        }
    }

    /**
     * Kopia zapasowa do Storage jest planowana automatycznie dwa razy dziennie (6:00 i 22:00, czas lokalny).
     */
    @Composable
    fun BackupView(viewModel: SettingsModelView) {
        val backupInProgress by viewModel.backupInProgress.observeAsState(false)

        Column(Modifier.fillMaxWidth()) {
            Text(
                "Eksport całej bazy (Realtime Database) do pliku JSON w Storage " +
                    "(gs://strefagentlemena.appspot.com, folder backups/).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Automatyczna kopia zapasowa uruchamia się dwa razy dziennie: o 6:00 i o 22:00 (czas urządzenia), " +
                    "o ile jest połączenie z siecią.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.backupDatabaseToStorage() },
                enabled = !backupInProgress,
            ) {
                Text(if (backupInProgress) "Trwa tworzenie kopii…" else "Utwórz kopię i wyślij")
            }
        }
    }
}