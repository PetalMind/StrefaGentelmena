package com.strefagentelmena.uiComposable

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.childrenOfParent
import com.strefagentelmena.models.familyRootAccounts
import com.strefagentelmena.models.settngsModel.isOnVacationOn
import com.strefagentelmena.viewModel.ScheduleModelView

val selectorsUI = Selectors()

class Selectors {
    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun FamilyAppointmentClientSelectors(viewModel: ScheduleModelView) {
        val customersList by viewModel.customersList.observeAsState(emptyList())
        val selectedClient by viewModel.selectedClient.observeAsState(Customer())
        val familyRoot by viewModel.selectedFamilyRootCustomer.observeAsState(Customer())
        val isNewAppointment by viewModel.isNewAppointment.observeAsState(false)
        val appointmentDialog by viewModel.appointmentDialog.observeAsState(false)

        val openRootDialog = remember { mutableStateOf(false) }
        val openSubjectDialog = remember { mutableStateOf(false) }

        val rootAccounts = remember(customersList) { customersList.familyRootAccounts() }

        val rootLabel = remember {
            mutableStateOf(
                familyRoot.fullName.takeIf { it.isNotBlank() } ?: "Wybierz rodzica / konto",
            )
        }

        LaunchedEffect(familyRoot.id, familyRoot.fullName, appointmentDialog) {
            rootLabel.value = familyRoot.fullName.takeIf { it.isNotBlank() }
                ?: "Wybierz rodzica / konto"
        }

        val subjectOptions: List<Customer> = remember(familyRoot.id, customersList) {
            if (familyRoot.id <= 0) emptyList()
            else buildList {
                add(familyRoot)
                addAll(customersList.childrenOfParent(familyRoot.id))
            }
        }

        val subjectRows: List<Pair<String, Customer>> = remember(subjectOptions, familyRoot.id) {
            val multi = subjectOptions.size > 1
            subjectOptions.map { c ->
                val line = if (multi && c.id == familyRoot.id) {
                    "Rodzic — ${c.fullName}"
                } else {
                    c.fullName
                }
                line to c
            }
        }

        val subjectLabel = remember {
            mutableStateOf(
                selectedClient.fullName.takeIf { it.isNotBlank() } ?: "Kto ma wizytę?",
            )
        }

        LaunchedEffect(selectedClient.id, selectedClient.fullName, subjectRows) {
            val row = subjectRows.firstOrNull { it.second.id == selectedClient.id }
            subjectLabel.value = row?.first
                ?: selectedClient.fullName.takeIf { it.isNotBlank() }
                ?: "Kto ma wizytę?"
        }

        LaunchedEffect(familyRoot.id, customersList, appointmentDialog, isNewAppointment) {
            if (!appointmentDialog) return@LaunchedEffect
            if (familyRoot.id <= 0) return@LaunchedEffect
            val ids = subjectOptions.map { it.id }
            if (ids.isEmpty()) return@LaunchedEffect
            if (selectedClient.id !in ids) {
                viewModel.setAppointmentSubjectForFamily(familyRoot)
            }
        }

        LaunchedEffect(isNewAppointment, appointmentDialog, selectedClient) {
            if (!isNewAppointment && appointmentDialog && selectedClient.id > 0) {
                val findClient = viewModel.findCustomerByName(selectedClient.fullName)
                if (findClient != null && findClient.id == selectedClient.id) {
                    viewModel.setSelectedClient(findClient)
                }
            }
        }

        val rootLabelText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                append("Rodzic / konto")
            }
        }
        val subjectLabelText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                append("Kto ma wizytę?")
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
        dialogsUI.FullScreenLogisticDialogSelector(
            labelText = rootLabelText,
            selectedItem = rootLabel.value,
            onItemChange = { rootLabel.value = it },
            isEditable = true,
            items = rootAccounts
                .filterNot { it.id == familyRoot.id }
                .map { it.fullName },
            onItemSelected = { name ->
                val r = rootAccounts.firstOrNull { it.fullName == name }
                if (r != null) {
                    viewModel.setSelectedFamilyRootForAppointment(r)
                    viewModel.checkAppointmentsList()
                    rootLabel.value = r.fullName
                }
            },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Person, contentDescription = "Person")
            },
            dialogShouldOpen = openRootDialog,
        )

        if (subjectOptions.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            dialogsUI.FullScreenLogisticDialogSelector(
                labelText = subjectLabelText,
                selectedItem = subjectLabel.value,
                onItemChange = { subjectLabel.value = it },
                isEditable = true,
                items = subjectRows.map { it.first }.filterNot { it == subjectLabel.value },
                onItemSelected = { line ->
                    val c = subjectRows.firstOrNull { it.first == line }?.second
                    if (c != null) {
                        viewModel.setAppointmentSubjectForFamily(c)
                        viewModel.checkAppointmentsList()
                        subjectLabel.value = line
                    }
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = "Person")
                },
                dialogShouldOpen = openSubjectDialog,
            )
        }
        }
    }

    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun ClientSelector(
        viewModel: ScheduleModelView,
    ) {
        FamilyAppointmentClientSelectors(viewModel = viewModel)
    }

    @Composable
    fun WorkerSelector(
        viewModel: ScheduleModelView,
    ) {
        val employeesList by viewModel.employeeList.observeAsState(emptyList())
        val selectedWorker by viewModel.selectedEmployee.observeAsState()
        val isNewAppointment by viewModel.isNewAppointment.observeAsState(false)
        val appoiment by viewModel.selectedAppointment.observeAsState()
        val appointmentDialog by viewModel.appointmentDialog.observeAsState()
        val appointmentDate by viewModel.selectedAppointmentDate.observeAsState("")

        val shouldOpen = remember { mutableStateOf(false) }

        val labelText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                append("Pracownik")
            }
        }

        val selectedWorkerLabel = selectedWorker?.takeIf { it.displayName.isNotEmpty() }?.displayName
            ?: "Wybierz pracownika"

        LaunchedEffect(isNewAppointment, appointmentDialog, appoiment) {
            if (appointmentDialog == true && !isNewAppointment) {
                viewModel.setEmpolyee(appoiment?.employee ?: return@LaunchedEffect)
            }
        }


        dialogsUI.FullScreenLogisticDialogSelector(
            labelText = labelText,
            selectedItem = selectedWorkerLabel,
            onItemChange = { },
            isEditable = true,
            items = employeesList.orEmpty()
                .filter { emp ->
                    val onVacation = emp.isOnVacationOn(appointmentDate)
                    !onVacation || emp.id != null && emp.id == selectedWorker?.id
                }
                .filterNot { it.id == selectedWorker?.id }
                .map { it.displayName }
                .filter { it.isNotEmpty() },
            onItemSelected =
            { label ->
                val employee = viewModel.findWorkerByDisplayName(label)

                viewModel.setEmpolyee(employee ?: return@FullScreenLogisticDialogSelector)
                viewModel.getsAppoiments()
            },
            leadingIcon =
            {
                Icon(imageVector = Icons.Outlined.Person, contentDescription = "Person")
            },
            dialogShouldOpen = shouldOpen,
        )
    }

}
