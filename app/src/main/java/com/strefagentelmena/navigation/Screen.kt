package com.strefagentelmena.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AddCustomer : Screen("addCustomer")
    data object Schedule : Screen("schedule")
}
