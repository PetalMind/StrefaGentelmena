package com.strefagentelmena.uiComposable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
            "Dodaj klienta" to { navController.navigate(Screen.AddCustomer.route) },
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
        modifier: Modifier = Modifier,
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            modifier = modifier
                .padding(padding)
                .size(width, height),
        ) {
            Text(text, style = TextStyle(fontSize = fontSize))
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
            Text(text, style = TextStyle(fontSize = fontSize))
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
            containerColor = colorsUI.buttonsGreen,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    }
}
