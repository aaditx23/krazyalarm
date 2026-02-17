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
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val buttonWidthPx = with(density) { 140.dp.toPx() }
    val buttonHeightPx = with(density) { 48.dp.toPx() }

    val maxX = screenWidthPx - buttonWidthPx
    val maxY = screenHeightPx - buttonHeightPx

    // Create physics bodies for both buttons
    val dismissBody = remember {
        PhysicsBody(
            x = 50f,
            y = screenHeightPx - buttonHeightPx - 200f,
            width = buttonWidthPx,
            height = buttonHeightPx,
            vx = randomVelocity(),
            vy = randomVelocity()
        )
    }

    val snoozeBody = remember {
        PhysicsBody(
            x = screenWidthPx - buttonWidthPx - 50f,
            y = screenHeightPx - buttonHeightPx - 400f,
            width = buttonWidthPx,
            height = buttonHeightPx,
            vx = randomVelocity(),
            vy = randomVelocity()
        )
    }

    // State for UI recomposition
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(maxX, maxY) {
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

            // Speed limits
            dismissBody.clampSpeed(8f)
            snoozeBody.clampSpeed(8f)

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

private fun randomVelocity(): Float {
    val speed = (2..4).random().toFloat()
    return if (kotlin.random.Random.nextBoolean()) speed else -speed
}

