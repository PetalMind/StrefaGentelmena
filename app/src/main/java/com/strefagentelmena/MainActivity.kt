package com.strefagentelmena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import com.strefagentelmena.functions.smsManager
import com.strefagentelmena.navigation.navigation
import com.strefagentelmena.ui.theme.StrefaGentelmenaTheme
import com.strefagentelmena.uiComposable.settingsUI.SettingsViews

class MainActivity : ComponentActivity() {
    companion object {
        private const val THEME_PREFS_NAME = "strefa_theme_preferences"
        private const val THEME_DARK_KEY = "is_dark_theme"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        smsManager.attachApplicationContext(applicationContext)
        FirebaseApp.initializeApp(this)
        SettingsViews.scheduleAutomaticDatabaseBackups(this)
        val prefs = getSharedPreferences(THEME_PREFS_NAME, MODE_PRIVATE)
        val initialDarkTheme = prefs.getBoolean(THEME_DARK_KEY, true)

        setContent {
            var darkTheme by remember { mutableStateOf(initialDarkTheme) }

            StrefaGentelmenaTheme(darkTheme = darkTheme) {
                navigation.AppNavigation(
                    isDarkTheme = darkTheme,
                    onThemeChange = { enabled ->
                        darkTheme = enabled
                        prefs.edit().putBoolean(THEME_DARK_KEY, enabled).apply()
                    }
                )
            }
        }
    }
}
