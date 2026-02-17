package com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.components

import kotlin.math.abs
import kotlin.math.min

/**
 * Physics body representing a rectangular object in 2D space.
 * Similar to game engine/OpenGL approach.
 */
class PhysicsBody(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
) {
    // Collision cooldown to prevent repeated collision detection
    private var collisionCooldown: Int = 0

    // Update position based on velocity
    fun update() {
        x += vx
        y += vy
        // Decrease cooldown
        if (collisionCooldown > 0) collisionCooldown--
    }

    // Check if in collision cooldown
    fun isInCooldown(): Boolean = collisionCooldown > 0

    // Set collision cooldown
    fun setCooldown(frames: Int) {
        collisionCooldown = frames
    }

    // Check AABB collision with another body
    fun overlaps(other: PhysicsBody): Boolean {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y
    }

    // Handle wall collision with pixel-perfect bounce
    fun bounceOffWalls(maxX: Float, maxY: Float) {
        // Left wall
        if (x < 0f) {
            x = 0f
            vx = abs(vx)
        }
        // Right wall
        else if (x > maxX) {
            x = maxX
            vx = -abs(vx)
        }

        // Top wall
        if (y < 0f) {
            y = 0f
            vy = abs(vy)
        }
        // Bottom wall
        else if (y > maxY) {
            y = maxY
            vy = -abs(vy)
        }
    }

    // Clamp velocity to max speed
    fun clampSpeed(maxSpeed: Float) {
        vx = vx.coerceIn(-maxSpeed, maxSpeed)
        vy = vy.coerceIn(-maxSpeed, maxSpeed)
    }

    companion object {
        /**
         * Resolve collision between two physics bodies with pixel-perfect separation.
         * Returns true if collision was resolved.
         */
        fun resolveCollision(a: PhysicsBody, b: PhysicsBody): Boolean {
            // Skip if either body is in cooldown
            if (a.isInCooldown() || b.isInCooldown()) return false
            if (!a.overlaps(b)) return false

            // Calculate overlap on each axis
            val overlapLeft = (a.x + a.width) - b.x
            val overlapRight = (b.x + b.width) - a.x
            val overlapTop = (a.y + a.height) - b.y
            val overlapBottom = (b.y + b.height) - a.y

            val minOverlapX = min(overlapLeft, overlapRight)
            val minOverlapY = min(overlapTop, overlapBottom)

            if (minOverlapX < minOverlapY) {
                // Horizontal collision - separate exactly by half the overlap each
                val separation = minOverlapX / 2f
                if (overlapLeft < overlapRight) {
                    // a is on left, b is on right
                    a.x -= separation
                    b.x += separation
                } else {
                    // a is on right, b is on left
                    a.x += separation
                    b.x -= separation
                }
                // Swap horizontal velocities (elastic collision)
                val tempVx = a.vx
                a.vx = b.vx
                b.vx = tempVx
            } else {
                // Vertical collision - separate exactly by half the overlap each
                val separation = minOverlapY / 2f
                if (overlapTop < overlapBottom) {
                    // a is on top, b is on bottom
                    a.y -= separation
                    b.y += separation
                } else {
                    // a is on bottom, b is on top
                    a.y += separation
                    b.y -= separation
                }
                // Swap vertical velocities (elastic collision)
                val tempVy = a.vy
                a.vy = b.vy
                b.vy = tempVy
            }

            // Set cooldown to prevent immediate re-collision
            a.setCooldown(8)
            b.setCooldown(8)

            return true
        }
    }
}
