package io.kreekt.core.math

import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * 2D vector implementation
 * T026 - Vector2 class
 */
@Serializable
data class Vector2(
    var x: Float = 0f,
    var y: Float = 0f
) {
    constructor(scalar: Float) : this(scalar, scalar)
    constructor(other: Vector2) : this(other.x, other.y)

    // Basic operations
    fun set(x: Float, y: Float): Vector2 {
        this.x = x
        this.y = y
        return this
    }

    fun set(scalar: Float): Vector2 = set(scalar, scalar)
    fun set(other: Vector2): Vector2 = set(other.x, other.y)

    fun copy(other: Vector2): Vector2 = set(other)
    fun clone(): Vector2 = Vector2(x, y)

    // Arithmetic operations
    fun add(other: Vector2): Vector2 {
        x += other.x
        y += other.y
        return this
    }

    fun add(scalar: Float): Vector2 {
        x += scalar
        y += scalar
        return this
    }

    fun subtract(other: Vector2): Vector2 {
        x -= other.x
        y -= other.y
        return this
    }

    fun subtract(scalar: Float): Vector2 {
        x -= scalar
        y -= scalar
        return this
    }

    fun multiply(other: Vector2): Vector2 {
        x *= other.x
        y *= other.y
        return this
    }

    fun multiply(scalar: Float): Vector2 {
        x *= scalar
        y *= scalar
        return this
    }

    fun divide(other: Vector2): Vector2 {
        x /= other.x
        y /= other.y
        return this
    }

    fun divide(scalar: Float): Vector2 {
        x /= scalar
        y /= scalar
        return this
    }

    // Vector operations
    fun dot(other: Vector2): Float = x * other.x + y * other.y

    fun cross(other: Vector2): Float = x * other.y - y * other.x

    fun length(): Float = sqrt(x * x + y * y)
    fun lengthSquared(): Float = x * x + y * y

    fun normalize(): Vector2 {
        val len = length()
        return if (len > 0) divide(len) else this
    }

    fun distance(other: Vector2): Float = sqrt(distanceSquared(other))
    fun distanceSquared(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }

    fun angle(): Float = atan2(y, x)
    fun angleTo(other: Vector2): Float = atan2(cross(other), dot(other))

    fun lerp(other: Vector2, t: Float): Vector2 {
        x += (other.x - x) * t
        y += (other.y - y) * t
        return this
    }

    fun negate(): Vector2 {
        x = -x
        y = -y
        return this
    }

    fun floor(): Vector2 {
        x = floor(x)
        y = floor(y)
        return this
    }

    fun ceil(): Vector2 {
        x = ceil(x)
        y = ceil(y)
        return this
    }

    fun round(): Vector2 {
        x = round(x)
        y = round(y)
        return this
    }

    // Utility
    fun equals(other: Vector2, epsilon: Float = 0.000001f): Boolean {
        return abs(x - other.x) < epsilon && abs(y - other.y) < epsilon
    }

    fun isZero(): Boolean = x == 0f && y == 0f

    override fun toString(): String = "Vector2($x, $y)"

    companion object {
        val ZERO = Vector2(0f, 0f)
        val ONE = Vector2(1f, 1f)
        val UNIT_X = Vector2(1f, 0f)
        val UNIT_Y = Vector2(0f, 1f)
    }
}