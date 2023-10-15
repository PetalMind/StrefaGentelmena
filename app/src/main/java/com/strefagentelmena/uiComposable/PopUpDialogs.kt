package com.strefagentelmena.uiComposable

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.strefagentelmena.R

class PopUpDialogs {
    @Composable
    fun CustomPopup(
        clientCountString: String = "3",
        onDismissRequest: () -> Unit,
        onClick: () -> Unit,
    ) {
        Dialog(
            onDismissRequest = { /*TODO*/ },
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
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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

                        Text(
                            text = "Zostaw to mnie, Kingo! Chcesz wysłać powiadomienia?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        Text(
                            text = "Niech każdy z $clientCountString klientów będzie przygotowany na jutro!",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { onDismissRequest() }) {
                                Text("Następnym razem")
                            }

                            Button(onClick = { onClick() }) {
                                Text(
                                    "Wyślij",
                                    fontSize = 18.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
