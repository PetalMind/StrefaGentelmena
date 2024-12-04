package com.strefagentelmena.uiComposable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.viewModel.ScheduleModelView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


val formUI = Form()

class Form {

    @Composable
    fun AppointmentForm(
        viewModel: ScheduleModelView,
        onSave: () -> Unit,
        onCancel: () -> Unit,
    ) {
        val selectedAppointment by viewModel.selectedAppointment.observeAsState(Appointment())
        val selectedDate by viewModel.selectedAppointmentDate.observeAsState()
        val currentSelectedAppoinmentsDate by viewModel.selectedAppointmentDate.observeAsState()
        val selectedClient by viewModel.selectedClient.observeAsState(Customer())
        val appointmentError by viewModel.appointmentError.observeAsState("")
        val startTime by viewModel.selectedAppointmentStartTime.observeAsState("")
        val endTime by viewModel.selectedAppointmentEndTime.observeAsState("")
        val note by viewModel.selectedAppointmentNote.observeAsState("")
        val selectedEmployee by viewModel.selectedEmployee.observeAsState(Employee())

        val customerIdError by remember { mutableStateOf(false) }
        val dateError by remember { mutableStateOf(false) }
        val startTimeError by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.prepareAppointmentDetails()
        }

        LaunchedEffect(selectedClient, Unit) {
            if (startTime != "") {
                if (selectedClient != null) {
                    val startTimeLocal =
                        LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
                    val endTimeLocal = startTimeLocal.plusHours(1)

                    viewModel.apply {
                        setAppoimentNote(selectedClient?.noted ?: "")
                        setAppoimentError("")
                        selectedAppointmentDate.value = currentSelectedAppoinmentsDate
                        selectedAppointmentStartTime.value = startTime
                        selectedAppointmentEndTime.value = endTime
                    }
                }
            }
        }

        LaunchedEffect(key1 = startTime) {
            if (startTime != "") {
                //   setAppointmentEndTime(selectedAppointment)
            }
        }

        Column {
            selectorsUI.WorkerSelector(viewModel = viewModel)
            selectorsUI.ClientSelector(viewModel = viewModel)
            if (customerIdError) {
                Text("Wymagany klient", color = Color.Red)
            }

            Spacer(modifier = Modifier.height(10.dp))

            textModernTextFieldUI.DateOutlinedTextField(
                value = selectedDate.toString(),
                onValueChange = {
                    val date = LocalDate.parse(it, DateTimeFormatter.ofPattern("dd.MM.yyyy"))

                    viewModel.setNewDataAppointment(date.toString())
                    viewModel.checkAppointmentsList()
                },
            )

            if (dateError) {
                Text("Niepoprawna data", color = Color.Red)
            }

            Spacer(modifier = Modifier.height(10.dp))

            textModernTextFieldUI.TimeOutlinedTextField(
                value = startTime,
                onValueChange = {
                    val startTimeLocal = LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                    viewModel.setNewTime(startTimeLocal.toString())

                    viewModel.checkAppointmentsList()
                },
                label = "Godzina rozpoczęcia",
                modifier = Modifier.fillMaxWidth()
            )

            if (startTimeError) {
                Text("Niepoprawna godzina rozpoczęcia", color = Color.Red)
            }

            Spacer(modifier = Modifier.height(10.dp))

            textModernTextFieldUI.TimeOutlinedTextField(
                value = endTime,
                onValueChange = {
                    val endTimeLocal = LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                    viewModel.setAppointmentEndTime(endTimeLocal)

                    viewModel.checkAppointmentsList()
                },
                label = "Godzina Zakończenia",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            textModernTextFieldUI.ModernTextField(
                value = note,
                onValueChange = {
                    viewModel.setAppoimentNote(it)
                },
                label = "Notatka",
                isError = false,
                supportText = null,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AccountBox,
                        contentDescription = "Person",
                    )
                },
                modifier = Modifier.height(150.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (appointmentError.isNotEmpty()) {
                Text(text = appointmentError.toString())
            }

            buttonsUI.ButtonsRow(
                onClick = { onSave() },
                onDismiss = { onCancel() },
                containerColor = colorsUI.green,
                cancelText = "Anuluj",
                confirmText = "Zapisz",
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }


    @Composable
    fun ErrorText(message: String) {
        Text(
            text = message,
            color = Color.Red,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
        )
    }
}
