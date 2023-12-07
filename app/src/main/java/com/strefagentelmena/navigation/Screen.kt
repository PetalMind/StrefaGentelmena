package com.strefagentelmena.navigation

sealed class Screen(val route: String) {
    data object MainScreen : Screen("mainScreen")
    data object CustomersScreen : Screen("customersScreen")
    data object ScheduleScreen : Screen("scheduleScreen")

    data object SettingsScreen : Screen("settingsScreen")
}
