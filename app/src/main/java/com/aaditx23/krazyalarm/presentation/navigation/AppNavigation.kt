package com.aaditx23.krazyalarm.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aaditx23.krazyalarm.data.util.PermissionUtils
import com.aaditx23.krazyalarm.domain.repository.SettingsRepository
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.AlarmListScreen
import com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.AlarmRingingActivity
import com.aaditx23.krazyalarm.presentation.screen.permissions.PermissionsScreen
import com.aaditx23.krazyalarm.presentation.screen.settings.SettingsScreen
import com.aaditx23.krazyalarm.presentation.screen.settings.alarmscreencustomization.AlarmScreenCustomizationScreen
import com.aaditx23.krazyalarm.presentation.screen.settings.ledpatterns.LEDPatternsScreen
import com.aaditx23.krazyalarm.presentation.screen.settings.vibrationpatterns.VibrationPatternsScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    settingsRepository: SettingsRepository,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasSeenPermissionsScreen by settingsRepository.hasSeenPermissionsScreen.collectAsState(initial = null)

    if (hasSeenPermissionsScreen == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val cameraReady = !PermissionUtils.hasCamera(context) || PermissionUtils.hasCameraPermission(context)
    val fullScreenIntentReady = PermissionUtils.canUseFullScreenIntent(context)
    val exactAlarmReady = PermissionUtils.canScheduleExactAlarms(context)
    val notificationsReady = PermissionUtils.hasNotificationPermission(context)
    val requireExplicitPermissionAck = hasSeenPermissionsScreen == false

    // Determine start destination based on permissions
    val startDestination = if (requireExplicitPermissionAck) {
        Screen.Permissions.route
    } else if (
        notificationsReady &&
        exactAlarmReady &&
        fullScreenIntentReady &&
        cameraReady
    ) {
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
                requireExplicitContinue = requireExplicitPermissionAck,
                onPermissionsGranted = {
                    scope.launch {
                        settingsRepository.setHasSeenPermissionsScreen(true)
                        navController.navigate(Screen.AlarmList.route) {
                            popUpTo(Screen.Permissions.route) { inclusive = true }
                        }
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
                onNavigateToAlarmScreenCustomization = {
                    navController.navigate(Screen.AlarmScreenCustomization.route)
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

        composable(Screen.AlarmScreenCustomization.route) {
            AlarmScreenCustomizationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTestAlarm = {
                    val intent = AlarmRingingActivity.createIntent(
                        context = context,
                        alarmId = -1L
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}
