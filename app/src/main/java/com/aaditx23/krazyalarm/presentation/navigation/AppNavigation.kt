package com.aaditx23.krazyalarm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.aaditx23.krazyalarm.presentation.screen.alarm_list.AlarmListScreen
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.AlarmListViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.AlarmList.route
    ) {
        composable(Screen.AlarmList.route) {
            val viewModel: AlarmListViewModel = koinViewModel()
            AlarmListScreen(
                viewModel = viewModel
            )
        }


    }
}
