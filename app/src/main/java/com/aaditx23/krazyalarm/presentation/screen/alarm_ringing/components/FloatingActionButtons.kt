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

    // Convert button dimensions to pixels for calculation
    val buttonWidthPx = with(density) { 140.dp.toPx() }
    val buttonHeightPx = with(density) { 48.dp.toPx() }

    val maxX = screenWidthPx - buttonWidthPx
    val maxY = screenHeightPx - buttonHeightPx

    // Initialize button positions - start at different safe positions
    var dismissButtonX by remember { mutableFloatStateOf(50f) }
    var dismissButtonY by remember { mutableFloatStateOf(screenHeightPx - buttonHeightPx - 200f) }
    var snoozeButtonX by remember { mutableFloatStateOf(screenWidthPx - buttonWidthPx - 50f) }
    var snoozeButtonY by remember { mutableFloatStateOf(screenHeightPx - buttonHeightPx - 400f) }

    // Random initial velocities - different each time the screen is shown
    var dismissVelocityX by remember {
        mutableFloatStateOf(if (kotlin.random.Random.nextBoolean())
            kotlin.random.Random.nextFloat() * 2f + 2f  // 2.0 to 4.0
        else
            -(kotlin.random.Random.nextFloat() * 2f + 2f)) // -2.0 to -4.0
    }
    var dismissVelocityY by remember {
        mutableFloatStateOf(if (kotlin.random.Random.nextBoolean())
            kotlin.random.Random.nextFloat() * 2f + 2f  // 2.0 to 4.0
        else
            -(kotlin.random.Random.nextFloat() * 2f + 2f)) // -2.0 to -4.0
    }
    var snoozeVelocityX by remember {
        mutableFloatStateOf(if (kotlin.random.Random.nextBoolean())
            kotlin.random.Random.nextFloat() * 2f + 2f  // 2.0 to 4.0
        else
            -(kotlin.random.Random.nextFloat() * 2f + 2f)) // -2.0 to -4.0
    }
    var snoozeVelocityY by remember {
        mutableFloatStateOf(if (kotlin.random.Random.nextBoolean())
            kotlin.random.Random.nextFloat() * 2f + 2f  // 2.0 to 4.0
        else
            -(kotlin.random.Random.nextFloat() * 2f + 2f)) // -2.0 to -4.0
    }

    // Animation loop for bouncing buttons with collision detection
    LaunchedEffect(maxX, maxY) {
        while (true) {
            delay(16L) // ~60fps

            // Update positions
            dismissButtonX += dismissVelocityX
            dismissButtonY += dismissVelocityY
            snoozeButtonX += snoozeVelocityX
            snoozeButtonY += snoozeVelocityY

            // Check collision between buttons (AABB collision)
            val collisionX = dismissButtonX < snoozeButtonX + buttonWidthPx &&
                    dismissButtonX + buttonWidthPx > snoozeButtonX
            val collisionY = dismissButtonY < snoozeButtonY + buttonHeightPx &&
                    dismissButtonY + buttonHeightPx > snoozeButtonY

            if (collisionX && collisionY) {
                // Calculate centers of both buttons
                val dismissCenterX = dismissButtonX + buttonWidthPx / 2
                val dismissCenterY = dismissButtonY + buttonHeightPx / 2
                val snoozeCenterX = snoozeButtonX + buttonWidthPx / 2
                val snoozeCenterY = snoozeButtonY + buttonHeightPx / 2

                // Calculate collision normal (direction from dismiss to snooze)
                val dx = snoozeCenterX - dismissCenterX
                val dy = snoozeCenterY - dismissCenterY
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                if (distance > 0) {
                    // Normalize the collision normal
                    val nx = dx / distance
                    val ny = dy / distance

                    // Calculate relative velocity
                    val relVelX = dismissVelocityX - snoozeVelocityX
                    val relVelY = dismissVelocityY - snoozeVelocityY

                    // Calculate relative velocity along collision normal
                    val relVelAlongNormal = relVelX * nx + relVelY * ny

                    // Only resolve if objects are moving towards each other
                    if (relVelAlongNormal > 0) {
                        // Elastic collision - swap velocity components along normal
                        dismissVelocityX -= relVelAlongNormal * nx
                        dismissVelocityY -= relVelAlongNormal * ny
                        snoozeVelocityX += relVelAlongNormal * nx
                        snoozeVelocityY += relVelAlongNormal * ny
                    }

                    // Separate buttons to prevent overlap
                    val overlap = (buttonWidthPx / 2 + buttonWidthPx / 2) - distance + 2f
                    if (overlap > 0) {
                        dismissButtonX -= nx * overlap / 2
                        dismissButtonY -= ny * overlap / 2
                        snoozeButtonX += nx * overlap / 2
                        snoozeButtonY += ny * overlap / 2
                    }
                }
            }

            // Bounce dismiss button off walls
            if (dismissButtonX <= 0f || dismissButtonX >= maxX) {
                dismissVelocityX = -dismissVelocityX
                dismissButtonX = dismissButtonX.coerceIn(0f, maxX)
            }
            if (dismissButtonY <= 0f || dismissButtonY >= maxY) {
                dismissVelocityY = -dismissVelocityY
                dismissButtonY = dismissButtonY.coerceIn(0f, maxY)
            }

            // Bounce snooze button off walls
            if (snoozeButtonX <= 0f || snoozeButtonX >= maxX) {
                snoozeVelocityX = -snoozeVelocityX
                snoozeButtonX = snoozeButtonX.coerceIn(0f, maxX)
            }
            if (snoozeButtonY <= 0f || snoozeButtonY >= maxY) {
                snoozeVelocityY = -snoozeVelocityY
                snoozeButtonY = snoozeButtonY.coerceIn(0f, maxY)
            }
        }
    }

    Box(modifier = modifier) {
        // Floating Snooze button
        Button(
            onClick = onSnooze,
            modifier = Modifier
                .width(140.dp)
                .height(48.dp)
                .offset(
                    x = with(density) { snoozeButtonX.toDp() },
                    y = with(density) { snoozeButtonY.toDp() }
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Snooze,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SNOOZE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Floating Dismiss button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .width(140.dp)
                .height(48.dp)
                .offset(
                    x = with(density) { dismissButtonX.toDp() },
                    y = with(density) { dismissButtonY.toDp() }
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AlarmOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "DISMISS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

    }
}
