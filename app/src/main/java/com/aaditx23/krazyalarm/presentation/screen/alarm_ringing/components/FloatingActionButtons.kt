package com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    buttonFlickerIntervalMs: Int = 0,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val buttonWidthPx  = with(density) { 140.dp.toPx() }
    val buttonHeightPx = with(density) { 48.dp.toPx() }

    val maxX = screenWidthPx - buttonWidthPx
    val maxY = screenHeightPx - buttonHeightPx

    val actualSpeed = buttonMotionSpeed.toFloat()

    val buttonGap         = with(density) { 16.dp.toPx() }
    val totalButtonsWidth = buttonWidthPx * 2 + buttonGap
    val startX            = (screenWidthPx - totalButtonsWidth) / 2f
    val sharedY           = screenHeightPx - buttonHeightPx - with(density) { 120.dp.toPx() }

    val dismissBody = remember(buttonMotionSpeed) {
        val randomVx = if (kotlin.random.Random.nextBoolean()) actualSpeed else -actualSpeed
        PhysicsBody(
            x = startX, y = sharedY,
            width = buttonWidthPx, height = buttonHeightPx,
            vx = if (buttonMotionSpeed > 0) randomVx else 0f,
            vy = if (buttonMotionSpeed > 0) -actualSpeed else 0f
        )
    }

    val snoozeBody = remember(buttonMotionSpeed) {
        val randomVx = if (kotlin.random.Random.nextBoolean()) actualSpeed else -actualSpeed
        PhysicsBody(
            x = startX + buttonWidthPx + buttonGap, y = sharedY,
            width = buttonWidthPx, height = buttonHeightPx,
            vx = if (buttonMotionSpeed > 0) randomVx else 0f,
            vy = if (buttonMotionSpeed > 0) actualSpeed else 0f
        )
    }

    // Physics tick
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(maxX, maxY, buttonMotionSpeed) {
        if (buttonMotionSpeed == 0) return@LaunchedEffect
        while (true) {
            delay(16L)
            dismissBody.update()
            snoozeBody.update()
            dismissBody.bounceOffWalls(maxX, maxY)
            snoozeBody.bounceOffWalls(maxX, maxY)
            PhysicsBody.resolveCollision(dismissBody, snoozeBody)
            val maxSpeed = actualSpeed * 2f
            dismissBody.clampSpeed(maxSpeed)
            snoozeBody.clampSpeed(maxSpeed)
            tick++
        }
    }

    // Flicker state: one button is "active" (full alpha, enabled) at a time.
    // The inactive button is faded to near-invisible AND disabled so ghost taps are impossible.
    // Starting state is random so it differs every alarm fire.
    var dismissActive by remember(buttonFlickerIntervalMs) {
        mutableStateOf(if (buttonFlickerIntervalMs > 0) kotlin.random.Random.nextBoolean() else true)
    }
    var snoozeActive by remember(buttonFlickerIntervalMs) {
        mutableStateOf(if (buttonFlickerIntervalMs > 0) !dismissActive else true)
    }

    LaunchedEffect(buttonFlickerIntervalMs) {
        if (buttonFlickerIntervalMs <= 0) {
            dismissActive = true
            snoozeActive  = true
            return@LaunchedEffect
        }
        while (true) {
            delay(buttonFlickerIntervalMs.toLong())
            val next = !dismissActive
            dismissActive = next
            snoozeActive  = !next
        }
    }

    val dismissAlpha = if (dismissActive) 1f else 0f
    val snoozeAlpha  = if (snoozeActive)  1f else 0f

    tick.let {
        Box(modifier = modifier) {
            // Snooze button
            Button(
                onClick = onSnooze,
                enabled = snoozeActive,
                modifier = Modifier
                    .width(140.dp)
                    .height(48.dp)
                    .offset(
                        x = with(density) { snoozeBody.x.toDp() },
                        y = with(density) { snoozeBody.y.toDp() }
                    )
                    .alpha(snoozeAlpha),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Snooze, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("SNOOZE", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Dismiss button
            Button(
                onClick = onDismiss,
                enabled = dismissActive,
                modifier = Modifier
                    .width(140.dp)
                    .height(48.dp)
                    .offset(
                        x = with(density) { dismissBody.x.toDp() },
                        y = with(density) { dismissBody.y.toDp() }
                    )
                    .alpha(dismissAlpha),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.AlarmOff, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("DISMISS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
