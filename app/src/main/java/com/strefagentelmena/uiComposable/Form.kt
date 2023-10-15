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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.strefagentelmena.viewModel.CustomersModelView
import com.strefagentelmena.viewModel.DashboardModelView
import com.strefagentelmena.viewModel.ScheduleModelView
import java.time.LocalDate


val formUI = Form()

class Form {

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun AppointmentForm(
        customerViewModel: CustomersModelView,
        scheduleModelView: ScheduleModelView,
        dashboardModelView: DashboardModelView,
        isNew: Boolean = true,
    ) {
        val selectedAppointment by scheduleModelView.selectedAppointment.observeAsState(null)


        val startTime by scheduleModelView.selectedAppointmentTime.observeAsState(
            if (isNew) "" else selectedAppointment?.startTime ?: ""
        )

        val selectedDate by scheduleModelView.selectedAppointmentDate.observeAsState(LocalDate.now())


        val customerIdError by remember { mutableStateOf(false) }
        val dateError by remember { mutableStateOf(false) }
        val startTimeError by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (isNew) scheduleModelView.clearDate()
        }

        Column {
            selectorsUI.ClientSelector(customersModelView = customerViewModel, scheduleModelView =  scheduleModelView, viewModel = dashboardModelView)

            if (customerIdError) {
                Text("Wymagany klient", color = Color.Red)
            }

            Spacer(modifier = Modifier.height(10.dp))

            textModernTextFieldUI.DateOutlinedTextField(
                value = selectedDate.toString(),
                onValueChange = { scheduleModelView.setNewDataAppointment(it) }
            ) {}

            if (dateError) {
                Text("Niepoprawna data", color = Color.Red)
            }

            Spacer(modifier = Modifier.height(10.dp))

            textModernTextFieldUI.TimeOutlinedTextField(
                value = startTime,
                onValueChange = { scheduleModelView.setNewTime(it) }
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
