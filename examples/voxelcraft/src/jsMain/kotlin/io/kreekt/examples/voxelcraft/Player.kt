package io.kreekt.examples.voxelcraft

import kotlin.math.PI

/**
 * Player entity - minimal implementation for vertical slice
 *
 * Full implementation in future: collision detection, physics, bounding box
 * Data model: data-model.md Section 5
 */
class Player(val world: VoxelWorld) {
    // Position stored as simple object for now (TODO: use KreeKt Vector3)
    var position = Position(0.0, 100.0, 0.0)
    var rotation = Rotation(0.0, 0.0)
    var velocity = Position(0.0, 0.0, 0.0)
    var isFlying = false
    val inventory = Inventory()

    fun move(direction: Position) {
        position.x = (position.x + direction.x).coerceIn(-256.0, 255.0)
        position.y = (position.y + direction.y).coerceIn(0.0, 255.0)
        position.z = (position.z + direction.z).coerceIn(-256.0, 255.0)
    }

    fun rotate(deltaPitch: Double, deltaYaw: Double) {
        rotation.pitch += deltaPitch
        rotation.yaw += deltaYaw
        // Clamp pitch to ±90°
        rotation.pitch = rotation.pitch.coerceIn(-PI / 2, PI / 2)
    }

    fun toggleFlight() {
        isFlying = !isFlying
        if (isFlying) velocity.y = 0.0
    }

    fun update(deltaTime: Float) {
        // TODO: Implement gravity, collision, ground detection
    }
}

data class Position(var x: Double, var y: Double, var z: Double) {
    fun clone() = Position(x, y, z)
    fun set(newX: Double, newY: Double, newZ: Double) {
        x = newX; y = newY; z = newZ
    }
}

data class Rotation(var pitch: Double, var yaw: Double) {
    fun clone() = Rotation(pitch, yaw)
    fun set(newPitch: Double, newYaw: Double) {
        pitch = newPitch; yaw = newYaw
    }
}
