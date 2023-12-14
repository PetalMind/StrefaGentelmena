package com.strefagentelmena.uiComposable

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.strefagentelmena.models.AppoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.viewModel.ScheduleModelView
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
        val currentSelectedAppoinmentsDate by viewModel.currentSelectedAppoinmentsDate.observeAsState()
        val isNewAppointment by viewModel.isNewAppointment.observeAsState(false)
        val selectedClient by viewModel.selectedClient.observeAsState(Customer())

        val context = LocalContext.current

        val customerIdError by remember { mutableStateOf(false) }
        val dateError by remember { mutableStateOf(false) }
        val startTimeError by remember { mutableStateOf(false) }
        val startTime by viewModel.selectedAppointmentTime.observeAsState("")

        LaunchedEffect(Unit) {
            if (isNewAppointment) {
                viewModel.clearDate()

                val currentTime = LocalTime.now()

                viewModel.setNewTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .toString()
            } else {
                viewModel.selectedClient.value =
                    viewModel.findCustomerByName(selectedAppointment?.customer?.fullName ?: "")
            }

            viewModel.selectedAppointmentDate.value = currentSelectedAppoinmentsDate
            viewModel.loadCustomersList(context = context)
        }

        Column {
            selectorsUI.ClientSelector(viewModel = viewModel)

            if (customerIdError) {
                Text("Wymagany klient", color = Color.Red)
            }

            Spacer(modifier = Modifier.height(10.dp))

            textModernTextFieldUI.DateOutlinedTextField(
                value = selectedDate.toString(),
                onValueChange = { viewModel.setNewDataAppointment(it) },
                onFocusLost = {}
            )

            if (dateError) {
                Text("Niepoprawna data", color = Color.Red)
            }

            Spacer(modifier = Modifier.height(10.dp))

            textModernTextFieldUI.TimeOutlinedTextField(
                value = startTime,
                onValueChange = { viewModel.setNewTime(it) },
                onFocusLost = {},
                label = "Godzina rozpoczęcia"
            )

            if (startTimeError) {
                Text("Niepoprawna godzina rozpoczęcia", color = Color.Red)
            }

            if (selectedClient?.noted?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Notatka: ${selectedClient?.noted}",
                    style = MaterialTheme.typography.titleMedium
                )
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
