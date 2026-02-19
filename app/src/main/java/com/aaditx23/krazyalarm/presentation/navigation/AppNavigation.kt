package com.aaditx23.krazyalarm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aaditx23.krazyalarm.data.util.PermissionUtils
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.AlarmListScreen
import com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.AlarmRingingActivity
import com.aaditx23.krazyalarm.presentation.screen.permissions.PermissionsScreen
import com.aaditx23.krazyalarm.presentation.screen.settings.SettingsScreen
import com.aaditx23.krazyalarm.presentation.screen.settings.ledpatterns.LEDPatternsScreen
import com.aaditx23.krazyalarm.presentation.screen.settings.vibrationpatterns.VibrationPatternsScreen

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
            AlarmListScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLEDPatterns = {
                    navController.navigate(Screen.LEDPatterns.route)
                },
                onNavigateToVibrationPatterns = {
                    navController.navigate(Screen.VibrationPatterns.route)
                },
                onTestAlarm = {
                    // Start the AlarmRingingActivity for testing
                    val intent = AlarmRingingActivity.createIntent(
                        context = context,
                        alarmId = -1L // -1 indicates a test alarm
                    )
                    context.startActivity(intent)
                }
            )
        }

        composable(Screen.LEDPatterns.route) {
            LEDPatternsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.VibrationPatterns.route) {
            VibrationPatternsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
