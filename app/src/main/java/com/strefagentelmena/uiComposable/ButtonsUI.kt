package com.strefagentelmena.uiComposable

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.strefagentelmena.navigation.Screen

val buttonsUI = ButtonsUI()

class ButtonsUI {
    @Composable
    fun DashboardButtons(navController: NavController) {
        val buttons = listOf(
            "Dodaj klienta" to { navController.navigate(Screen.CustomersScreen.route) },
            "Zobacz listę klientów" to { /* Akcja */ },
            "Ustawienia" to { /* Akcja */ }
        )

        buttons.forEach { (text, action) ->
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = action,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    @Composable
    fun PrimaryButton(
        text: String,
        onClick: () -> Unit,
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
        padding: Dp = 12.dp,
        fontSize: TextUnit = 18.sp,
        height: Dp = 50.dp,
        width: Dp = 220.dp,
        fontWeight: FontWeight = FontWeight.Normal,
        modifier: Modifier = Modifier,
        buttonEnabled: Boolean = true
    ) {
        val pressed = remember { mutableStateOf(false) }

        val elevation by animateDpAsState(
            targetValue = if (pressed.value) 8.dp else 0.dp,
            animationSpec = tween(durationMillis = 300)
        )

        val size by animateDpAsState(
            targetValue = if (pressed.value) 100.dp else 50.dp,
            animationSpec = tween(durationMillis = 300)
        )

        Button(
            onClick = {
                pressed.value = !pressed.value
                onClick()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            enabled = buttonEnabled,
            modifier = modifier
                .padding(padding)
                .shadow(elevation = elevation)
                .size(width, height),
        ) {
            Text(text, style = TextStyle(fontSize = fontSize, fontWeight = fontWeight))
        }
    }

    @Composable
    fun CustomTextButton(
        text: String,
        onClick: () -> Unit,
        padding: Dp = 12.dp,
        fontSize: TextUnit = 18.sp,
        height: Dp = 50.dp,
        width: Dp = 100.dp,
        modifier: Modifier = Modifier,
    ) {

        TextButton(
            onClick = onClick,
            modifier = modifier
                .padding(padding)
                .size(width, height),
        ) {
            Text(
                text,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = colorsUI.fontGrey
                )
            )
        }

    }


    @Composable
    fun ExtendedFab(
        text: String,
        icon: ImageVector,
        onClick: () -> Unit,
    ) {
        ExtendedFloatingActionButton(
            text = { Text(text) },
            icon = { Icon(icon, contentDescription = null) },
            onClick = onClick,
            containerColor = colorsUI.jade,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    }

    @Composable
    fun HeaderIconButton(
        icon: Int, // ID ikony, np. R.drawable.icon
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        containerColor: Color = colorsUI.buttonsGreen,
        iconSize: Dp = 24.dp,
        iconColor: Color = Color.Black
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
                .padding(8.dp)
        ) {
            // Jeśli ikona jest obrazkiem
            if (icon != 0) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                // Jeśli chcesz użyć ikony z Material Design
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }

    @Composable
    fun ButtonsRow(
        onClick: () -> Unit,
        onDismiss: () -> Unit,
        cancelText: String = "Anuluj",
        confirmText: String = "Zapisz",
        containerColor: Color = colorsUI.amaranthPurple,
        modifier: Modifier = Modifier,
        buttonEnabled: Boolean = true
    ) {
        Row(
            modifier = modifier
        ) {
            buttonsUI.CustomTextButton(text = cancelText, onClick = { onDismiss() })
            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = confirmText,
                onClick = { onClick() },
                containerColor = containerColor,
                buttonEnabled = buttonEnabled
            )
        }
    }
}
