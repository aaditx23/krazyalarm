package com.aaditx23.krazyalarm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aaditx23.krazyalarm.data.util.PermissionUtils
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.AlarmListScreen
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.AlarmListViewModel
import com.aaditx23.krazyalarm.presentation.screen.permissions.PermissionsScreen
import com.aaditx23.krazyalarm.presentation.screen.settings.SettingsScreen
import com.aaditx23.krazyalarm.presentation.screen.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    // Determine start destination based on permissions
    val startDestination = if (PermissionUtils.hasNotificationPermission(context) &&
                                PermissionUtils.canScheduleExactAlarms(context)) {
        Screen.AlarmList.route
    } else {
        Screen.Permissions.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Permissions.route) {
            PermissionsScreen(
                onPermissionsGranted = {
                    navController.navigate(Screen.AlarmList.route) {
                        popUpTo(Screen.Permissions.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AlarmList.route) {
            val viewModel: AlarmListViewModel = koinViewModel()
            AlarmListScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = koinViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLEDPatterns = {
                    navController.navigate(Screen.LEDPatterns.route)
                },
                onNavigateToVibrationPatterns = {
                    navController.navigate(Screen.VibrationPatterns.route)
                }
            )
        }

        composable(Screen.LEDPatterns.route) {
            val viewModel: SettingsViewModel = koinViewModel()
            com.aaditx23.krazyalarm.presentation.screen.settings.ledpatterns.LEDPatternsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.VibrationPatterns.route) {
            val viewModel: SettingsViewModel = koinViewModel()
            com.aaditx23.krazyalarm.presentation.screen.settings.vibrationpatterns.VibrationPatternsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
