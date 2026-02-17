package com.aaditx23.krazyalarm.presentation.screen.alarm_ringing

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.components.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlarmRingingScreen(
    viewModel: AlarmRingingViewModel,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Pulsing animation for the alarm icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Live time update
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = getCurrentTime()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
    ) {
        when (val state = uiState) {
            is AlarmRingingUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            is AlarmRingingUiState.Ringing -> {
                AlarmRingingContent(
                    alarm = state.alarm,
                    currentTime = currentTime,
                    scale = scale,
                    onDismiss = onDismiss,
                    onSnooze = onSnooze,
                    viewModel = viewModel,
                    buttonMotionSpeed = state.buttonMotionSpeed
                )
            }
            is AlarmRingingUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRingingContent(
    alarm: com.aaditx23.krazyalarm.domain.models.Alarm,
    currentTime: String,
    scale: Float,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    viewModel: AlarmRingingViewModel,
    buttonMotionSpeed: Int
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Time Display
            AlarmTimeDisplay(
                dateString = viewModel.getCurrentDateString(),
                currentTime = currentTime
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Pulsing Alarm Icon
            PulsingAlarmIcon(scale = scale)

            Spacer(modifier = Modifier.height(32.dp))

            // Alarm Info
            AlarmInfo(
                label = alarm.label,
                hour = alarm.hour,
                minute = alarm.minute
            )
        }

        // Floating Action Buttons
        FloatingActionButtons(
            onDismiss = onDismiss,
            onSnooze = onSnooze,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            buttonMotionSpeed = buttonMotionSpeed,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun getCurrentTime(): String {
    val calendar = Calendar.getInstance()
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return timeFormat.format(calendar.time)
}

