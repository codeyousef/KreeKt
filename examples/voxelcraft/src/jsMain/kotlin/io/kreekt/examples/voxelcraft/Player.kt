package io.kreekt.examples.voxelcraft

import io.kreekt.core.math.Vector3
import kotlin.math.PI

/**
 * Player entity with physics, collision detection, and bounding box
 *
 * Features:
 * - AABB collision detection
 * - Gravity and jump mechanics
 * - Flight mode
 * - Ground detection via raycasting
 *
 * Data model: data-model.md Section 5
 */
class Player(val world: VoxelWorld) {
    // Position using KreeKt Vector3
    var position = Vector3(0.0f, 100.0f, 0.0f)

    // Rotation (pitch = x-axis, yaw = y-axis)
    var rotation = Vector3(0.0f, 0.0f, 0.0f)

    // Velocity for physics
    var velocity = Vector3(0.0f, 0.0f, 0.0f)

    var isFlying = false
    var isOnGround = false
    val inventory = Inventory()

    // Player dimensions (AABB bounding box)
    val width = 0.6f  // blocks
    val height = 1.8f // blocks
    val depth = 0.6f  // blocks

    // Physics constants
    val gravity = -20.0f // blocks/s² (faster than real gravity for gameplay)
    val jumpVelocity = 8.0f // blocks/s
    val terminalVelocity = -50.0f // maximum fall speed

    /**
     * Move player by delta, with collision detection
     */
    fun move(direction: Vector3) {
        val newX = position.x + direction.x
        val newY = position.y + direction.y
        val newZ = position.z + direction.z

        // Check collision in each axis separately for sliding
        if (!checkCollision(newX, position.y, position.z)) {
            position.x = newX.coerceIn(-256.0f, 255.0f)
        }

        if (!checkCollision(position.x, newY, position.z)) {
            position.y = newY.coerceIn(0.0f, 255.0f)
        } else {
            // Collision in Y - stop vertical velocity
            if (direction.y < 0) {
                isOnGround = true
            }
            velocity.y = 0.0f
        }

        if (!checkCollision(position.x, position.y, newZ)) {
            position.z = newZ.coerceIn(-256.0f, 255.0f)
        }
    }

    /**
     * Check if player's AABB would collide with solid blocks at position
     */
    private fun checkCollision(x: Float, y: Float, z: Float): Boolean {
        // Check corners of player's bounding box
        val minX = (x - width / 2).toInt()
        val maxX = (x + width / 2).toInt()
        val minY = y.toInt()
        val maxY = (y + height).toInt()
        val minZ = (z - depth / 2).toInt()
        val maxZ = (z + depth / 2).toInt()

        for (blockX in minX..maxX) {
            for (blockY in minY..maxY) {
                for (blockZ in minZ..maxZ) {
                    val block = world.getBlock(blockX, blockY, blockZ)
                    if (block != null && block != BlockType.Air && !block.isTransparent) {
                        return true // Collision detected
                    }
                }
            }
        }

        return false
    }

    /**
     * Rotate camera (pitch = up/down, yaw = left/right)
     */
    fun rotate(deltaPitch: Double, deltaYaw: Double) {
        rotation.x += deltaPitch.toFloat()
        rotation.y += deltaYaw.toFloat()
        // Clamp pitch to ±90°
        rotation.x = rotation.x.coerceIn((-PI / 2).toFloat(), (PI / 2).toFloat())
    }

    /**
     * Toggle flight mode
     */
    fun toggleFlight() {
        isFlying = !isFlying
        if (isFlying) {
            velocity.y = 0.0f
            isOnGround = false
        }
    }

    /**
     * Attempt to jump (only works when on ground)
     */
    fun jump() {
        if (isOnGround && !isFlying) {
            velocity.y = jumpVelocity
            isOnGround = false
        }
    }

    /**
     * Update physics (gravity, ground detection)
     */
    fun update(deltaTime: Float) {
        if (!isFlying) {
            // Apply gravity
            velocity.y += gravity * deltaTime

            // Terminal velocity
            if (velocity.y < terminalVelocity) {
                velocity.y = terminalVelocity
            }

            // Apply velocity
            if (velocity.y != 0.0f) {
                isOnGround = false
                move(Vector3(0.0f, velocity.y * deltaTime, 0.0f))
            }

            // Ground detection (raycast downward)
            if (checkCollision(position.x, position.y - 0.1f, position.z)) {
                isOnGround = true
                velocity.y = 0.0f
            }
        }
    }
}
