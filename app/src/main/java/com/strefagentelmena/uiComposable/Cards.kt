package com.strefagentelmena.uiComposable

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.PlainTooltipState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.strefagentelmena.R
import com.strefagentelmena.functions.appFunctions
import com.strefagentelmena.models.Appointment
import com.strefagentelmena.models.Customer
import kotlinx.coroutines.launch

val cardUI = Cards()

class Cards {
    @Composable
    fun CustomerCard(customer: Customer) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Imię: ${customer.firstName}")
            Text("Nazwisko: ${customer.lastName}")
            Text("Data wizyty: ${customer.appointment}")
            Text("Telefon: ${customer.phoneNumber}")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UpcomingClientCard(appointment: Appointment) {
        val context = LocalContext.current
        val tooltipState = remember { PlainTooltipState() }
        val scope = rememberCoroutineScope()

        Card(
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Kto następny na twoim fotelu?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    PlainTooltipBox(
                        tooltip = {
                            Text("Kto jest następny?\n Pokazuje klienta spotykającego się w ciągu godziny.")
                        },
                        tooltipState = tooltipState
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    tooltipState.show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Szczegóły")
                        }
                    }
                }

                Text(
                    text = "${appointment.customer.fullName}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(painterResource(id = R.drawable.ic_clock), contentDescription = "Czas")
                    Text(
                        text = "Godzina:", style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = appointment.startTime,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    buttonsUI.PrimaryButton(
                        onClick = {
                            appFunctions.dialPhoneNumber(
                                context = context,
                                phoneNumber = appointment.customer.phoneNumber
                                    ?: return@PrimaryButton
                            )
                        },
                        text = "Nie zwlekaj, dzwoń!",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    @Composable
    fun DashboardSmallCard(
        @DrawableRes iconId: Int,
        labelText: String,
        nameText: String,
        onClick: () -> Unit = {},
    ) {
        Card(
            modifier = Modifier
                .size(180.dp, 180.dp)
                .clickable(onClick = onClick)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = labelText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = nameText,
                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    @Composable
    fun CustomerListCard(
        customer: Customer,
        onClick: () -> Unit,
    ) {
        Card(shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = customer.fullName ?: "Brak imienia",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Tel: ${customer.phoneNumber}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (customer.appointment != null) {
                        Text(
                            text = "Ostatnia wizyta: ${customer.appointment?.date ?: ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun CustomerAppoimentListCard(
        appointment: Appointment,
        onClick: () -> Unit,
    ) {
        Card(
            shape = RoundedCornerShape(12.dp), // Zmieniłem kształt narożników na nieco mniej zaokrąglone
            elevation = CardDefaults.cardElevation(6.dp), // Podniosłem nieco elewację, aby dodać karcie głębi
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp) // Zwiększyłem nieco wysokość karty
                .clickable { onClick() }
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = appointment.customer.fullName ?: "Brak imienia",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

}
