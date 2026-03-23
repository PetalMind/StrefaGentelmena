package com.strefagentelmena.uiComposable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.strefagentelmena.R
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.ui.theme.SalonGold

class PopUpDialogs {

    @Composable
    fun IconHeader() {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_clock),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_bomb),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_fire),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
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
        var showClientList by remember { mutableStateOf(true) }
        val canSend = appoiments.isNotEmpty()

        val clientText = when (clientCountString) {
            "1" -> "Przypomnij 1 klientowi o nadchodzącej wizycie."
            "0" -> "Nie masz klientów do powiadomienia."
            else -> "Przypomnij $clientCountString klientom o nadchodzących wizytach."
        }

        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false,
                usePlatformDefaultWidth = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StrefaModalPanel {
                        StrefaModalIconFrame(
                            variant = StrefaModalIconVariant.Warning,
                            icon = Icons.Outlined.Notifications,
                            iconTint = SalonGold,
                        )
                        StrefaModalTitleText(
                            text = "Powiadom swoich klientów!",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        StrefaModalBodyText(
                            text = clientText,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )

                        if (appoiments.isNotEmpty()) {
                            StrefaDialogButton(
                                text = if (showClientList) "Ukryj listę klientów" else "Pokaż listę klientów",
                                onClick = { showClientList = !showClientList },
                                modifier = Modifier.fillMaxWidth(),
                                style = StrefaDialogButtonStyle.Ghost,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        AnimatedVisibility(visible = showClientList && appoiments.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                appoiments.forEach { appointment ->
                                    StrefaDialogReadOnlyField(
                                        text = "${appointment.customer.fullName}\n${appointment.date}, ${appointment.startTime}",
                                        modifier = Modifier.padding(bottom = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        StrefaDialogButtonRow(
                            first = { m ->
                                StrefaDialogButton(
                                    text = "Pomiń",
                                    onClick = onDismissRequest,
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Ghost,
                                )
                            },
                            second = { m ->
                                StrefaDialogButton(
                                    text = "Wyślij powiadomienia",
                                    onClick = onClick,
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Gold,
                                    enabled = canSend,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
