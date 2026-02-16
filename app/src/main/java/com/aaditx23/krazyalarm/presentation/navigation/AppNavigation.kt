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
                viewModel = viewModel
            )
        }


    }
}
