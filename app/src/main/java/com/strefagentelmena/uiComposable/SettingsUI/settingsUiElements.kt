package com.strefagentelmena.uiComposable.settingsUI

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
        Card(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing
                    )
                )
                .fillMaxWidth()
                .height(60.dp)
                .padding(4.dp),
            onClick = {
                onClick()
            },
            shape = androidx.compose.material3.MaterialTheme.shapes.medium,
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp), // Dodaj ten modifier, aby wycentrować zawartość
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null
                    )
                    Text(
                        text = text,
                        modifier = Modifier.padding(start = 8.dp),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null
                )
            }
        }

        AnimatedVisibility(
            visible = expandedState,
            enter = expandIn(),
            exit = shrinkOut()
        ) {
            ExpandedContent(expandedComposable)
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
                shape = androidx.compose.material3.MaterialTheme.shapes.medium,
            ) {
                Box(Modifier.padding(8.dp)) {
                    composable()
                }
            }
        }
    }
}