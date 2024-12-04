package com.strefagentelmena.uiComposable

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.strefagentelmena.viewModel.ScheduleModelView

val selectorsUI = Selectors()

class Selectors {
    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun ClientSelector(
        viewModel: ScheduleModelView,
    ) {
        val customersList by viewModel.customersList.observeAsState(emptyList())
        val selectedClient by viewModel.selectedClient.observeAsState(null)
        val isNewAppointment by viewModel.isNewAppointment.observeAsState(false)
        val isNew = remember { mutableStateOf(isNewAppointment) }

        val selectedClientName =
            remember { mutableStateOf(selectedClient?.fullName ?: "Wybierz klienta") }

        val labelText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Black)) {
                append("Klient")
            }
        }

        LaunchedEffect(isNewAppointment) {
            isNew.value = isNewAppointment

            if (!isNewAppointment) {
                val findClient = viewModel.findCustomerByName(selectedClient?.fullName ?: "")

                if (findClient != null) {
                    viewModel.setSelectedClient(findClient)

                    selectedClientName.value = findClient.fullName
                }
            }
        }

        dialogsUI.FullScreenLogisticDialogSelector(
            labelText = labelText,
            selectedItem = selectedClientName.value,
            onItemChange = {
                selectedClientName.value = it
            },
            isEditable = true,
            items = customersList?.filterNot { it.id == selectedClient?.id }
                ?.map { it.fullName } ?: emptyList(),
            onItemSelected = { client ->
                val findClient = viewModel.findCustomerByName(client)

                if (findClient != null) {
                    viewModel.setSelectedClient(findClient)
                    viewModel.checkAppointmentsList()
                    selectedClientName.value = findClient.fullName
                }
            },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Person, contentDescription = "Person")
            },
            dialogShouldOpen = isNew,
        )
    }

    @Composable
    fun WorkerSelector(
        viewModel: ScheduleModelView,
    ) {
        val employeesList by viewModel.employeeList.observeAsState(emptyList())
        val selectedWorker by viewModel.selectedEmployee.observeAsState()
        val isNewAppointment by viewModel.isNewAppointment.observeAsState(false)
        val isNew = remember { mutableStateOf(isNewAppointment) }
        val appoiment by viewModel.selectedAppointment.observeAsState()
        val appointmentDialog by viewModel.appointmentDialog.observeAsState()

        val selectedWorkerName =
            remember { mutableStateOf(selectedWorker?.name ?: "Wybierz Pracownika") }

        val shouldOpen = remember { mutableStateOf(false) }

        val labelText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Black)) {
                append("Pracownik")
            }
        }

        LaunchedEffect(isNewAppointment) {
            isNew.value = isNewAppointment

            if (appointmentDialog == true) {
                if (!isNewAppointment) {
                    viewModel.setEmpolyee(appoiment?.employee ?: return@LaunchedEffect)

                    selectedWorkerName.value = appoiment!!.employee.name
                }
            }
        }

        LaunchedEffect(selectedWorker)
        {
            if (selectedWorker!!.id != null) {
                shouldOpen.value = false
            }
        }

        dialogsUI.FullScreenLogisticDialogSelector(
            labelText = labelText,
            selectedItem = selectedWorker?.name.toString(),
            onItemChange =
            {
                selectedWorkerName.value = it
            },
            isEditable = true,
            items = employeesList?.filterNot
            { it.id == selectedWorker?.id }
                ?.map
                { it.name } ?: emptyList(),
            onItemSelected =
            { client ->
                val employee = viewModel.findWorkerByName(client)

                viewModel.setEmpolyee(employee ?: return@FullScreenLogisticDialogSelector)
            },
            leadingIcon =
            {
                Icon(imageVector = Icons.Outlined.Person, contentDescription = "Person")
            },
            dialogShouldOpen = shouldOpen,
        )
    }

}
