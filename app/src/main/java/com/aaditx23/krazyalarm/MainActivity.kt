package com.aaditx23.krazyalarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aaditx23.krazyalarm.presentation.navigation.AppNavigation
import com.aaditx23.krazyalarm.presentation.theme.KrazyAlarmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KrazyAlarmTheme {
                AppNavigation()
            }
        }
    }
}
