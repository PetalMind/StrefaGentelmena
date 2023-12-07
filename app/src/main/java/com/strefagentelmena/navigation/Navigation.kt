package com.strefagentelmena.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.strefagentelmena.screens.screenCustomerView
import com.strefagentelmena.screens.mainScreen
import com.strefagentelmena.screens.screenSchedule
import com.strefagentelmena.screens.settingsScreen
import com.strefagentelmena.viewModel.CustomersModelView
import com.strefagentelmena.viewModel.MainScreenModelView
import com.strefagentelmena.viewModel.ScheduleModelView
import com.strefagentelmena.viewModel.SettingsModelView

val navigation = Navigation()

class Navigation {
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        val customersModelView: CustomersModelView = CustomersModelView()
        val schuduleModelView: ScheduleModelView = ScheduleModelView()
        val settingsModelView = SettingsModelView()

        NavHost(navController, startDestination = Screen.MainScreen.route) {
            composable(Screen.MainScreen.route) {
                val dashboardModelView = MainScreenModelView()

                mainScreen.DashboardView(
                    navController,
                    dashboardModelView
                )
            }
            composable(Screen.CustomersScreen.route) {
                screenCustomerView.CustomerListView(
                    customersModelView, navController
                )
            }
            composable(Screen.ScheduleScreen.route) {
                screenSchedule.ScheduleView(
                    viewModel = schuduleModelView,
                    navController = navController,
                )
            }

            composable(Screen.SettingsScreen.route) {
                settingsScreen.SettingsView(
                    navController = navController,
                    viewModel = settingsModelView
                )
            }
        }
    }
}
