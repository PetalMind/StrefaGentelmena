package com.strefagentelmena.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.strefagentelmena.screens.screenCustomerView
import com.strefagentelmena.screens.mainScreen
import com.strefagentelmena.screens.screenSchedule
import com.strefagentelmena.screens.settingsScreen
import com.strefagentelmena.screens.statisticsScreen
import com.strefagentelmena.viewModel.CustomersModelView
import com.strefagentelmena.viewModel.MainScreenModelView
import com.strefagentelmena.viewModel.ScheduleModelView
import com.strefagentelmena.viewModel.SettingsModelView

val navigation = Navigation()

class Navigation {
    @Composable
    fun AppNavigation(
        isDarkTheme: Boolean,
        onThemeChange: (Boolean) -> Unit,
    ) {
        val navController = rememberNavController()

        NavHost(navController, startDestination = Screen.MainScreen.route) {
            composable(Screen.MainScreen.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(700)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(700)
                    )
                }
            ) { entry ->
                val dashboardModelView: MainScreenModelView = viewModel(entry)
                mainScreen.DashboardView(
                    navController,
                    dashboardModelView,
                )
            }
            composable(Screen.CustomersScreen.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            300, easing = LinearEasing
                        )
                    ) + slideIntoContainer(
                        animationSpec = tween(300, easing = EaseIn),
                        towards = AnimatedContentTransitionScope.SlideDirection.Start
                    )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            300, easing = LinearEasing
                        )
                    ) + slideOutOfContainer(
                        animationSpec = tween(300, easing = EaseOut),
                        towards = AnimatedContentTransitionScope.SlideDirection.End
                    )
                }
            ) { entry ->
                val customersModelView: CustomersModelView = viewModel(entry)
                screenCustomerView.CustomersOverview(
                    customersModelView, navController
                )
            }
            composable(Screen.ScheduleScreen.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(700)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(700)
                    )
                }
            ) { entry ->
                val scheduleModelView: ScheduleModelView = viewModel(entry)
                screenSchedule.ScheduleView(
                    viewModel = scheduleModelView,
                    navController = navController,
                )
            }

            composable(Screen.SettingsScreen.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(700)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(700)
                    )
                }
            ) { entry ->
                val settingsModelView: SettingsModelView = viewModel(entry)
                settingsScreen.SettingsView(
                    navController = navController,
                    viewModel = settingsModelView,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                )
            }

            composable(
                Screen.StatisticsScreen.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(280)) + slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(400, easing = EaseIn),
                    )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(220)) + slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(350, easing = EaseOut),
                    )
                },
            ) { entry ->
                val parentEntry = remember(entry) {
                    navController.getBackStackEntry(Screen.MainScreen.route)
                }
                val dashboardModelView: MainScreenModelView = viewModel(parentEntry)
                statisticsScreen.StatisticsView(
                    navController = navController,
                    viewModel = dashboardModelView,
                )
            }
        }
    }
}
