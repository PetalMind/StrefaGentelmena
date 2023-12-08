package com.strefagentelmena.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
            ) {
                val dashboardModelView = MainScreenModelView()

                mainScreen.DashboardView(
                    navController,
                    dashboardModelView
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
            ) {
                screenCustomerView.CustomerListView(
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
            ) {
                screenSchedule.ScheduleView(
                    viewModel = schuduleModelView,
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
            ) {
                settingsScreen.SettingsView(
                    navController = navController,
                    viewModel = settingsModelView
                )
            }
        }
    }
}
