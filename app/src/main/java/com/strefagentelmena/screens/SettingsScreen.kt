package com.strefagentelmena.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.strefagentelmena.appViewStates
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.viewModel.SettingsModelView

val settingsScreen = SettingsScreen()

class SettingsScreen {
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun SettingsView(viewModel: SettingsModelView, navController: NavController) {
        val viewState by viewModel.viewState.observeAsState(AppState.Idle)
        val context = LocalContext.current


        when (viewState) {
            AppState.Idle -> {
                viewModel.loadAllData(context = context)
            }

            AppState.Loading -> {
                appViewStates.LoadingView()
            }

            AppState.Error -> {}
            AppState.Success -> {
                SettingsSuccessView(viewModel, navController)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun SettingsSuccessView(viewModel: SettingsModelView, navController: NavController) {
        val snackbarHostState = SnackbarHostState()

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                headersUI.AppBarWithBackArrow(
                    title = "Ustawienia",
                    onBackPressed = {
                        navController.navigate("mainScreen")
                    },
                    compose = {
                    },
                    onClick = {
                    },
                )
            },

            ) { it ->
            Surface(
                modifier = Modifier
                    .padding(it)
            ) {

            }
        }
    }
}