package com.strefagentelmena.uiComposable

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.strefagentelmena.viewModel.ScheduleModelView


val formUI = Form()

class Form {

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun AppointmentForm(
        viewModel: ScheduleModelView,
        isNew: Boolean = true,
    ) {
        val selectedAppointment by viewModel.selectedAppointment.observeAsState(null)
        val selectedDate by viewModel.selectedAppointmentDate.observeAsState()
        val currentSelectedAppoinmentsDate by viewModel.currentSelectedAppoinmentsDate.observeAsState()

        val startTime by viewModel.selectedAppointmentTime.observeAsState(
            if (isNew) "" else selectedAppointment?.startTime ?: ""
        )


        val context = LocalContext.current

        val customerIdError by remember { mutableStateOf(false) }
        val dateError by remember { mutableStateOf(false) }
        val startTimeError by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (isNew) viewModel.clearDate()
            if (isNew) viewModel.selectedClient.value = null

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
                onValueChange = { viewModel.setNewTime(it) }
            ) {}

            if (startTimeError) {
                Text("Niepoprawna godzina rozpoczęcia", color = Color.Red)
            }
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
