package com.strefagentelmena.uiComposable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strefagentelmena.AgentDebugLog
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.DEFAULT_APPOINTMENT_DURATION_MINUTES
import com.strefagentelmena.models.appoimentsModel.parseAppointmentTimeString
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.viewModel.ScheduleModelView
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


val formUI = Form()

class Form {
    private val noteAddedAtFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    private fun formatNoteAddedAt(millis: Long): String {
        if (millis <= 0L) return "wcześniej"
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(noteAddedAtFormat)
    }

    @Composable
    fun AppointmentForm(
        viewModel: ScheduleModelView,
    ) {
        val selectedAppointment by viewModel.selectedAppointment.observeAsState(Appointment())
        val selectedDate by viewModel.selectedAppointmentDate.observeAsState()
        val currentSelectedAppoinmentsDate by viewModel.selectedAppointmentDate.observeAsState()
        val selectedClient by viewModel.selectedClient.observeAsState(Customer())
        val appointmentError by viewModel.appointmentError.observeAsState("")
        val appointmentScheduleNotice by viewModel.appointmentScheduleNotice.observeAsState("")
        val startTime by viewModel.selectedAppointmentStartTime.observeAsState("")
        val endTime by viewModel.selectedAppointmentEndTime.observeAsState("")
        val note by viewModel.selectedAppointmentNote.observeAsState("")
        val noteHistory by viewModel.selectedAppointmentNoteHistory.observeAsState(emptyList())
        val selectedEmployee by viewModel.selectedEmployee.observeAsState(Employee())
        val isNewAppointment by viewModel.isNewAppointment.observeAsState(false)

        val customerIdError by remember { mutableStateOf(false) }
        val dateError by remember { mutableStateOf(false) }
        val startTimeError by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.prepareAppointmentDetails()
        }

        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

        // Nowa wizyta: po zmianie startu domyślnie koniec = start + domyślna długość. Edycja: zachowaj zapisany koniec.
        LaunchedEffect(startTime, selectedClient, isNewAppointment) {
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H2",
                location = "Form.AppointmentForm.LaunchedEffect",
                message = "startTime_side_effects",
                data = mapOf(
                    "startTimeLen" to startTime.length.toString(),
                    "isNew" to isNewAppointment.toString(),
                    "clientId" to selectedClient.id.toString(),
                ),
            )
            // #endregion
            if (startTime != "") {
                viewModel.apply {
                    selectedAppointmentDate.value = currentSelectedAppoinmentsDate
                    selectedAppointmentStartTime.value = startTime
                    if (isNewAppointment) {
                        parseAppointmentTimeString(startTime)?.let { startTimeLocal ->
                            selectedAppointmentEndTime.value =
                                startTimeLocal.plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES).format(timeFmt)
                        }
                    }
                    checkAppointmentsList()
                }
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
                    viewModel.setNewTime(it)
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
                    val endParsed = parseAppointmentTimeString(it) ?: return@TimeOutlinedTextField
                    viewModel.setAppointmentEndTime(endParsed)

                    viewModel.checkAppointmentsList()
                },
                label = "Godzina Zakończenia",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Notatki",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorsUI.fontGrey,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (noteHistory.isEmpty()) {
                Text(
                    text = "Brak zapisanych notatek. Dodaj pierwszą poniżej.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorsUI.darkGrey,
                )
            } else {
                noteHistory.forEach { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = colorsUI.raisinBlack),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = formatNoteAddedAt(entry.addedAtMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorsUI.darkGrey,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorsUI.fontGrey,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            textModernTextFieldUI.ModernTextField(
                value = note,
                onValueChange = { viewModel.setAppoimentNote(it) },
                label = "Nowa notatka",
                isError = false,
                supportText = null,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AccountBox,
                        contentDescription = "Person",
                    )
                },
                modifier = Modifier.height(120.dp),
            )
            TextButton(
                onClick = { viewModel.appendSelectedAppointmentNote() },
                enabled = note.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Dodaj notatkę",
                    color = if (note.isNotBlank()) colorsUI.jade else colorsUI.darkGrey,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val scheduleBanner = strefaScheduleErrorBannerOrNull(appointmentError)
            if (scheduleBanner != null) {
                StrefaBanner(
                    variant = scheduleBanner.variant,
                    title = scheduleBanner.title,
                    description = scheduleBanner.description,
                    actionLabel = if (scheduleBanner.variant == StrefaBannerVariant.Warning) {
                        "Zmień godziny"
                    } else {
                        null
                    },
                    onActionClick = if (scheduleBanner.variant == StrefaBannerVariant.Warning) {
                        { viewModel.setAppoimentError("") }
                    } else {
                        null
                    },
                    onDismiss = { viewModel.setAppoimentError("") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            val noticeBanner = strefaScheduleNoticeBannerOrNull(appointmentScheduleNotice)
            if (noticeBanner != null) {
                StrefaBanner(
                    variant = noticeBanner.variant,
                    title = noticeBanner.title,
                    description = noticeBanner.description,
                    onDismiss = { viewModel.clearAppointmentScheduleNotice() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
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
