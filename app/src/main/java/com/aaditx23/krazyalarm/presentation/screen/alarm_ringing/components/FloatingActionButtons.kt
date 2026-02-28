package com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FloatingActionButtons(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    screenWidthPx: Float,
    screenHeightPx: Float,
    buttonMotionSpeed: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val buttonWidthPx = with(density) { 140.dp.toPx() }
    val buttonHeightPx = with(density) { 48.dp.toPx() }

    val maxX = screenWidthPx - buttonWidthPx
    val maxY = screenHeightPx - buttonHeightPx

    // Convert speed setting (0-8) to actual velocity
    val actualSpeed = buttonMotionSpeed.toFloat()

    // Both buttons share the same initial Y so they are aligned when speed = 0
    val buttonGap = with(density) { 16.dp.toPx() }
    val totalButtonsWidth = buttonWidthPx * 2 + buttonGap
    val startX = (screenWidthPx - totalButtonsWidth) / 2f
    val sharedY = screenHeightPx - buttonHeightPx - with(density) { 120.dp.toPx() }

    // Create physics bodies for both buttons.
    // Horizontal direction is randomised independently; vertical directions are forced opposite
    // so the buttons immediately diverge instead of drifting together.
    val dismissBody = remember(buttonMotionSpeed) {
        val randomVx = if (kotlin.random.Random.nextBoolean()) actualSpeed else -actualSpeed
        PhysicsBody(
            x = startX,
            y = sharedY,
            width = buttonWidthPx,
            height = buttonHeightPx,
            vx = if (buttonMotionSpeed > 0) randomVx else 0f,
            vy = if (buttonMotionSpeed > 0) -actualSpeed else 0f  // always starts going UP
        )
    }

    val snoozeBody = remember(buttonMotionSpeed) {
        val randomVx = if (kotlin.random.Random.nextBoolean()) actualSpeed else -actualSpeed
        PhysicsBody(
            x = startX + buttonWidthPx + buttonGap,
            y = sharedY,
            width = buttonWidthPx,
            height = buttonHeightPx,
            vx = if (buttonMotionSpeed > 0) randomVx else 0f,
            vy = if (buttonMotionSpeed > 0) actualSpeed else 0f   // always starts going DOWN
        )
    }

    // State for UI recomposition
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(maxX, maxY, buttonMotionSpeed) {
        // Skip animation if speed is 0
        if (buttonMotionSpeed == 0) return@LaunchedEffect

        while (true) {
            delay(16L) // ~60 FPS

            // Update physics
            dismissBody.update()
            snoozeBody.update()

            // Wall collisions
            dismissBody.bounceOffWalls(maxX, maxY)
            snoozeBody.bounceOffWalls(maxX, maxY)

            // Body-to-body collision
            PhysicsBody.resolveCollision(dismissBody, snoozeBody)

            // Speed limits - scale with button motion speed
            val maxSpeed = actualSpeed * 2f
            dismissBody.clampSpeed(maxSpeed)
            snoozeBody.clampSpeed(maxSpeed)

            // Trigger recomposition
            tick++
        }
    }

    // Force read tick to trigger recomposition
    tick.let {
        Box(modifier = modifier) {
            // Snooze button
            Button(
                onClick = onSnooze,
                modifier = Modifier
                    .width(140.dp)
                    .height(48.dp)
                    .offset(
                        x = with(density) { snoozeBody.x.toDp() },
                        y = with(density) { snoozeBody.y.toDp() }
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Snooze, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("SNOOZE", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Dismiss button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .width(140.dp)
                    .height(48.dp)
                    .offset(
                        x = with(density) { dismissBody.x.toDp() },
                        y = with(density) { dismissBody.y.toDp() }
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.AlarmOff, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("DISMISS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

