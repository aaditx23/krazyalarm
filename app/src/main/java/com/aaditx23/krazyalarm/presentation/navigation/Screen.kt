package com.aaditx23.krazyalarm.presentation.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen {
    abstract val route: String
    open val arguments: List<NamedNavArgument> = emptyList()

    object Permissions : Screen() {
        override val route = "permissions"
    }

    object AlarmList : Screen() {
        override val route = "alarm_list"
    }

    data class AlarmEdit(val alarmId: Long = -1L) : Screen() {
        override val route = "alarm_edit"
        override val arguments = listOf(
            navArgument("alarmId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    }

    data class AlarmRinging(val alarmId: Long) : Screen() {
        override val route = "alarm_ringing"
        override val arguments = listOf(
            navArgument("alarmId") {
                type = NavType.LongType
            }
        )
    }

    object Settings : Screen() {
        override val route = "settings"
    }

    object LEDPatterns : Screen() {
        override val route = "led_patterns"
    }

    object VibrationPatterns : Screen() {
        override val route = "vibration_patterns"
    }

    object AlarmScreenCustomization : Screen() {
        override val route = "alarm_screen_customization"
    }
}
