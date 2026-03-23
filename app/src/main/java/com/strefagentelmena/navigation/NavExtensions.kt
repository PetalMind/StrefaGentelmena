package com.strefagentelmena.navigation

import androidx.navigation.NavController

/**
 * Cofa o jeden ekran; jeśli stos jest pusty lub nie da się cofnąć — przechodzi na główny.
 */
fun NavController.popBackStackOrNavigateToMain() {
    if (!popBackStack()) {
        navigate(Screen.MainScreen.route) {
            launchSingleTop = true
        }
    }
}
