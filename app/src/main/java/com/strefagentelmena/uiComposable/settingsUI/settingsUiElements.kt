package com.strefagentelmena.uiComposable.settingsUI

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.strefagentelmena.uiComposable.colorsUI

val settingsUiElements = SettingsUiElements()

class SettingsUiElements {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsItem(
        icon: Int,
        text: String,
        onClick: () -> Unit,
        expandedComposable: @Composable () -> Unit,
        expandedState: Boolean
    ) {
        // Dynamiczna wysokość karty z animacją
        val cardHeight by animateDpAsState(
            targetValue = if (expandedState) 500.dp else 60.dp, // Dopasuj wysokość
            animationSpec = tween(
                durationMillis = 300,
                easing = LinearOutSlowInEasing
            )
        )

        Card(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing
                    )
                )
                .fillMaxWidth()
                .height(cardHeight) // Ustaw dynamiczną wysokość
                .padding(4.dp),
            onClick = {
                onClick()
            },
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp), // Dopasuj padding dla zawartości
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = CenterVertically
                ) {
                    Row(verticalAlignment = CenterVertically) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = null
                        )
                        Text(
                            text = text,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null
                    )
                }

                if (expandedState) {
                    expandedComposable()
                }
            }
        }
    }

    @Composable
    fun CustomSwitch(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        contentDescription: String = "Switch",
        text: String,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier.padding(10.dp),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorsUI.mintGreen,
                    checkedTrackColor = colorsUI.teaGreen,
                    uncheckedThumbColor = colorsUI.murrey,
                    uncheckedTrackColor = colorsUI.grey
                ),
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = contentDescription,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                }
            )

            Text(
                text = text,
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }


    @Composable
    fun ExpandedContent(composable: @Composable () -> Unit) {
        Box(
            Modifier
                .padding(8.dp)
                .fillMaxSize()
        ) {
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                shape = MaterialTheme.shapes.medium,
            ) {
                Box(Modifier.padding(8.dp)) {
                    composable()
                }
            }
        }
    }
}