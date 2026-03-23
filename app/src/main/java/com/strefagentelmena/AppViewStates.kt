package com.strefagentelmena

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.strefagentelmena.uiComposable.StrefaLoading

val appViewStates = AppViewStates()

class AppViewStates {

    @Composable
    fun LoadingView(
        modifier: Modifier = Modifier,
        onRetry: (() -> Unit)? = null,
        message: String = "Ładowanie... sprawdź połączenie z internetem",
    ) {
        StrefaLoading(
            modifier = modifier,
            message = message,
            onRetry = onRetry,
        )
    }

}
