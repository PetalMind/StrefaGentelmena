package com.strefagentelmena.uiComposable

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.PlainTooltipState
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strefagentelmena.functions.appFunctions
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.settngsModel.Employee
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val cardUI = Cards()

class Cards {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UpcomingClientCard(appointment: Appointment) {
        val context = LocalContext.current
        val tooltipState = remember { PlainTooltipState() }
        val scope = rememberCoroutineScope()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorsUI.papaya,
            ),
            elevation = CardDefaults.cardElevation(4.dp),
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
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    PlainTooltipBox(
                        tooltip = {
                            Text("Kto jest następny?\nPokazuje klienta spotykającego się w ciągu godziny.")
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

                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = appointment.customer.fullName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${appointment.startTime} - ${appointment.endTime}",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        buttonsUI.IconButton(
                            color = Color.Black,
                            icon = Icons.Default.Call,
                            onClick = {
                                appFunctions.dialPhoneNumber(
                                    context = context,
                                    phoneNumber = appointment.customer.phoneNumber,
                                )
                            },
                        )
                    }
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
            colors = CardDefaults.cardColors(
                containerColor = colorsUI.papaya,
            ),
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
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp
                    ),
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SwipeToDismissCustomerCard(
        customer: Customer,
        onClick: () -> Unit,
        onDismiss: (Customer) -> Unit,
        onEdit: (Customer) -> Unit
    ) {
        val dismissState = rememberDismissState(initialValue = DismissValue.Default)
        val visible = rememberSaveable { mutableStateOf(true) }
        val scale by animateFloatAsState(
            targetValue = if (dismissState.currentValue != DismissValue.Default) 1.2f else 1f,
            label = ""
        )

        AnimatedVisibility(visible = visible.value) {
            SwipeToDismiss(
                state = dismissState,
                background = {
                    val color = when (dismissState.dismissDirection) {
                        DismissDirection.StartToEnd -> colorsUI.mintGreen
                        DismissDirection.EndToStart -> colorsUI.amaranthPurple
                        null -> Color.Transparent
                    }

                    val direction = dismissState.dismissDirection

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(color)
                    ) {
                        if (direction == DismissDirection.StartToEnd) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .animateContentSize()
                                    .background(color)
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .scale(scale)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = "Edytuj", fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .animateContentSize()
                                    .background(color)
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .scale(scale)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Spacer(modifier = Modifier.heightIn(5.dp))

                                    Text(
                                        text = "Usuń",
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                },
                dismissContent = {
                    CustomerListCard(
                        customer = customer,
                        onClick = onClick
                    )
                },
                directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
            )
        }

        LaunchedEffect(dismissState.targetValue) {
            if (dismissState.targetValue == DismissValue.DismissedToEnd) {
                delay(300)
                dismissState.snapTo(DismissValue.Default)
                onEdit(customer)
            } else if (dismissState.targetValue == DismissValue.DismissedToStart) {
                delay(300)
                onDismiss(customer)
            }
        }


        LaunchedEffect(dismissState.currentValue) {
            if (dismissState.currentValue == DismissValue.DismissedToEnd || dismissState.currentValue == DismissValue.DismissedToStart) {
                delay(300)
                dismissState.snapTo(DismissValue.Default)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SwipeToDismissEmployeeCard(
        employee: Employee,
        onClick: () -> Unit,
        onDismiss: (Employee) -> Unit,
        onEdit: (Employee) -> Unit
    ) {
        val dismissState = rememberDismissState(initialValue = DismissValue.Default)
        val visible = rememberSaveable { mutableStateOf(true) }
        val scale by animateFloatAsState(
            targetValue = if (dismissState.currentValue != DismissValue.Default) 1.2f else 1f,
            label = ""
        )

        AnimatedVisibility(visible = visible.value) {
            SwipeToDismiss(
                state = dismissState,
                background = {
                    val color = when (dismissState.dismissDirection) {
                        DismissDirection.StartToEnd -> colorsUI.mintGreen
                        DismissDirection.EndToStart -> colorsUI.amaranthPurple
                        null -> Color.Transparent
                    }

                    val direction = dismissState.dismissDirection

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(color)
                    ) {
                        if (direction == DismissDirection.StartToEnd) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .animateContentSize()
                                    .background(color)
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .scale(scale)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = "Edytuj", fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .animateContentSize()
                                    .background(color)
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .scale(scale)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Spacer(modifier = Modifier.heightIn(5.dp))

                                    Text(
                                        text = "Usuń",
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                },
                dismissContent = {
                    EmployeeListCard(
                        employee = employee,
                        onClick = onClick
                    )
                },
                directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
            )
        }

        LaunchedEffect(dismissState.targetValue) {
            if (dismissState.targetValue == DismissValue.DismissedToEnd) {
                delay(300)
                dismissState.snapTo(DismissValue.Default)
                onEdit(employee)
            } else if (dismissState.targetValue == DismissValue.DismissedToStart) {
                delay(300)
                onDismiss(employee)
            }
        }


        LaunchedEffect(dismissState.currentValue) {
            if (dismissState.currentValue == DismissValue.DismissedToEnd || dismissState.currentValue == DismissValue.DismissedToStart) {
                delay(300)
                dismissState.snapTo(DismissValue.Default)
            }
        }
    }

    @Composable
    fun EmployeeListCard(
        employee: Employee,
        onClick: () -> Unit,
    ) {
        Card(shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorsUI.papaya,
            ),
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
                        .size(56.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(colorsUI.jade)
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
                        text = (employee.name + " " + employee.surname),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
            colors = CardDefaults.cardColors(
                containerColor = colorsUI.papaya,
            ),
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
                        .size(56.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(colorsUI.jade)
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
                        text = customer.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tel: ${customer.phoneNumber}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    if (customer.appointment?.customer?.id != (0 ?: return@Card)) {
                        Row {
                            Text(
                                text = "Ostatnia wizyta: ",
                                style = MaterialTheme.typography.bodyLarge,
                            )

                            Text(
                                text = customer.appointment?.date.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CustomerAppoimentListCard(
        appointment: Appointment,
        onClick: () -> Unit,
        onNotificationClick: () -> Unit,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = colorsUI.papaya
            ),
            shape = RoundedCornerShape(12.dp), // Zmieniłem kształt narożników na nieco mniej zaokrąglone
            elevation = CardDefaults.cardElevation(2.dp), // Podniosłem nieco elewację, aby dodać karcie głębi
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp) // Zwiększyłem nieco wysokość karty
                .clickable { onClick() }
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = appointment.customer.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)  // Rozciąganie, aby zajmować dostępną przestrzeń
                    )

                    Spacer(modifier = Modifier.width(10.dp))  // Dodatkowy odstęp

                    animationElements.NotificationIcon(
                        notificationSent = appointment.notificationSent,
                        onClick = {
                            if (!appointment.notificationSent) onNotificationClick()
                        }
                    )
                }
            }
        }
    }
}

