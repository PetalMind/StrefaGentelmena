package com.strefagentelmena.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.strefagentelmena.screens.screenCustomerView
import com.strefagentelmena.screens.screenDashboard
import com.strefagentelmena.screens.screenSchedule
import com.strefagentelmena.viewModel.CustomersModelView
import com.strefagentelmena.viewModel.DashboardModelView
import com.strefagentelmena.viewModel.ScheduleModelView

val navigation = Navigation()

class Navigation {
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        val customersModelView: CustomersModelView = CustomersModelView()
        val schuduleModelView: ScheduleModelView = ScheduleModelView()
        val dashboardModelView = DashboardModelView()

        NavHost(navController, startDestination = Screen.Dashboard.route) {
            composable(Screen.Dashboard.route) {
                screenDashboard.DashboardView(
                    navController,
                    customersModelView,
                    schuduleModelView,
                    dashboardModelView
                )
            }
            composable(Screen.AddCustomer.route) {
                screenCustomerView.CustomerListView(
                    customersModelView, navController
                )
            }
            composable(Screen.Schedule.route) {
                screenSchedule.scheduleView(
                    viewModel = schuduleModelView,
                    navController = navController,
                    customersModelView = customersModelView,
                    dashboardModelView = dashboardModelView
                )
            }
        }
    }
}
