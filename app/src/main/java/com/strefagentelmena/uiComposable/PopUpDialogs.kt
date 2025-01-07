package com.strefagentelmena.uiComposable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.strefagentelmena.R
import com.strefagentelmena.models.appoimentsModel.Appointment

class PopUpDialogs {

    @Composable
    fun IconHeader() {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_clock),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_bomb),
                contentDescription = null,
                modifier = Modifier.size(32.dp)

            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_fire),
                contentDescription = null,
                modifier = Modifier.size(32.dp)

            )

        }
    }

    @Composable
    fun NotifyDialog(
        clientCountString: String = "0",
        appoiments: List<Appointment> = emptyList(),
        onDismissRequest: () -> Unit,
        onClick: () -> Unit,
    ) {
        val showClientList = remember { mutableStateOf(true) }

        Dialog(
            onDismissRequest = { onDismissRequest() },
            properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Powiadom swoich klientów!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp),
                            textAlign = TextAlign.Center
                        )

                        val clientText = when (clientCountString) {
                            "1" -> "Przypomnij 1 klientowi o nadchodzącej wizycie."
                            "0" -> "Nie masz klientów do powiadomienia."
                            else -> "Przypomnij $clientCountString klientom o nadchodzących wizytach."
                        }

                        Text(
                            text = clientText,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            if (appoiments.isNotEmpty()) {
                                TextButton(onClick = { showClientList.value = !showClientList.value }) {
                                    Text(
                                        text = if (showClientList.value) "Ukryj listę klientów" else "Pokaż listę klientów",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = showClientList.value) {
                        if (showClientList.value) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 4.dp
                                )
                            ) {
                                appoiments.forEach { appointment ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Text(
                                            text = appointment.customer.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "${appointment.date}, ${appointment.startTime}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    buttonsUI.ButtonsRow(
                        onClick = { onClick() },
                        onDismiss = { onDismissRequest() },
                        cancelText = "Pomiń",
                        confirmText = "Wyślij powiadomienia",
                    )
                }
            }
        }
    }

}

