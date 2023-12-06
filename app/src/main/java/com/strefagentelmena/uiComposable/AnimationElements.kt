package com.strefagentelmena.uiComposable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val animationElements = AnimationElements()

class AnimationElements {
    @Composable
    fun NotificationIcon(
        notificationSent: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        val transition = updateTransition(notificationSent, label = "")

        val iconAlpha by transition.animateFloat(label = "") {
            if (it) 1f else 0.5f
        }

        val textAlpha by transition.animateFloat(label = "") {
            if (it) 1f else 0f
        }

        val textTransition = remember {
            mutableStateOf(true)
        }

        val backgroundColor =
            if (notificationSent) colorsUI.teaGreen else colorsUI.sunset

        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            // Set up a coroutine to hide the text after 2 seconds
            val delayMillis = 2000L

            coroutineScope.launch {
                delay(delayMillis)
                textTransition.value = false
            }

            onDispose {
                coroutineScope.cancel()
            }
        }

        Surface(
            shape = CircleShape,
            color = backgroundColor,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = if (notificationSent) Icons.Default.Notifications else Icons.Outlined.Notifications,
                    contentDescription = if (notificationSent) "Notification Sent" else "Notification Not Sent",
                    tint = colorsUI.fontGrey,
                    modifier = modifier
                        .clickable {
                            if (!notificationSent) {
                                onClick()
                            }
                        }
                )

                AnimatedVisibility(
                    visible = textTransition.value,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = if (notificationSent) "Powiadomienie wysłano" else "Oczekuje na wysłanie",
                        fontSize = 16.sp,
                        color = colorsUI.fontGrey,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 4.dp)
                    )
                }
            }
        }
    }
}