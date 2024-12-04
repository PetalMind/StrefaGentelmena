package com.strefagentelmena.uiComposable.navigationUI

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

val navActionButtonsUI = ActionsNavButtons()

class ActionsNavButtons {
    @Composable
    fun BackArrow(onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Wróć")
        }
    }
}
