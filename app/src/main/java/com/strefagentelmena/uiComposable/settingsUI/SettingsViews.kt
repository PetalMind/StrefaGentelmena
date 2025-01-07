package com.strefagentelmena.uiComposable.settingsUI

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.textModernTextFieldUI
import com.strefagentelmena.viewModel.SettingsModelView

val settingsViews = SettingsViews()

class SettingsViews {

    @Composable
    fun ProfileView(viewModel: SettingsModelView) {
        val profilePreferences by viewModel.profilePreferences.observeAsState()
        val newProfileName by viewModel.profileName.observeAsState("")

        LaunchedEffect(profilePreferences) {
            Log.e("ProfileView", "profilePreferences: ${profilePreferences!!.userName}")
        }

        Column(Modifier.fillMaxSize()) {
            textModernTextFieldUI.ModernTextField(
                value = newProfileName,
                onValueChange = { it -> viewModel.setNewProfileName(it) },
                modifier = Modifier.padding(10.dp),
                label = "Nazwa użytkownika",
                leadingIcon = {
                    Icon(
                        Icons.Default.Person, contentDescription = null
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

        Column(Modifier.fillMaxSize()) {
            when (isAddingEmployee) {
                true -> {
                    EmpolyeeAddView(viewModel)
                }

                false -> {
                    EmpolyeeListView(viewModel)
                }

                null -> {
                    Column {
                        Text("Lista pracowników jest pusta")
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
                    fontWeight = FontWeight.Bold
                )

                buttonsUI.IconButton(
                    onClick = {
                        viewModel.setIsNewEmplyee(true)
                        viewModel.setAddNewEmployeeState()
                    },
                    icon = Icons.Outlined.Add,
                    color = colorsUI.headersBlue,
                    modifier = Modifier.padding(20.dp) // Add padding to the IconButton as well
                )
            }

            if (showAddEmployeeView) {
                EmpolyeeAddView(viewModel)
            } else {
                LazyColumn {
                    items(employeeList!!, key = { it.id!! }) { employee ->
                        cardUI.SwipeToDismissEmployeeCard(employee = employee,
                            onClick = {
                                viewModel.setIsNewEmplyee(false)
                                viewModel.setAddNewEmployeeState()
                                viewModel.setSlectedEmpolyee(employee)
                            }, onDismiss = {
                                viewModel.setSlectedEmpolyee(employee)
                                viewModel.setEmpolyeeDeleteDialog()
                            }, onEdit = {
                                viewModel.setIsNewEmplyee(false)
                                viewModel.setAddNewEmployeeState()
                                viewModel.setSlectedEmpolyee(employee)
                            })
                    }
                }
            }
        }
    }


    @Composable
    fun EmpolyeeAddView(viewModel: SettingsModelView) {
        val context = LocalContext.current
        val empolyeeName by viewModel.empolyeeName.observeAsState()
        val empolyeeSurname by viewModel.empolyeeSurname.observeAsState()
        val newEmpolyee by viewModel.newEmployee.observeAsState()
        val addNewEmpolyeeState by viewModel.addEmpolyeeState.observeAsState()
        val isNewEmplyee by viewModel.isNewEmplyee.observeAsState()
        val text = if (isNewEmplyee == true) "Dodaj nowego pracownika" else "Edytuj pracownika"

        LaunchedEffect(addNewEmpolyeeState) {
            if (isNewEmplyee == true) {
                viewModel.clearNewEmpolyee()
            }
        }

        Column {
            Text(
                text,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
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

        Column(
            Modifier.fillMaxSize()
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
        Column(Modifier.fillMaxSize()) {

        }
    }
}