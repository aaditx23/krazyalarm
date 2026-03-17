package com.aaditx23.krazyalarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aaditx23.krazyalarm.domain.repository.SettingsRepository
import com.aaditx23.krazyalarm.presentation.navigation.AppNavigation
import com.aaditx23.krazyalarm.presentation.theme.KrazyAlarmTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkMode by settingsRepository.darkMode.collectAsState(initial = "system")
            val isDarkTheme = when (darkMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            KrazyAlarmTheme(darkTheme = isDarkTheme) {
                AppNavigation(settingsRepository = settingsRepository)
            }
        }
    }
}


