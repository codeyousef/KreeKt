package io.kreekt.core.math

import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * 3D vector implementation
 * T027 - Vector3 class
 */
@Serializable
data class Vector3(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    constructor(scalar: Float) : this(scalar, scalar, scalar)
    constructor(other: Vector3) : this(other.x, other.y, other.z)
    constructor(v2: Vector2, z: Float = 0f) : this(v2.x, v2.y, z)

    // Basic operations
    fun set(x: Float, y: Float, z: Float): Vector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(scalar: Float): Vector3 = set(scalar, scalar, scalar)
    fun set(other: Vector3): Vector3 = set(other.x, other.y, other.z)

    fun copy(other: Vector3): Vector3 = set(other)
    fun clone(): Vector3 = Vector3(x, y, z)

    // Arithmetic operations
    fun add(other: Vector3): Vector3 {
        x += other.x
        y += other.y
        z += other.z
        return this
    }

    fun add(scalar: Float): Vector3 {
        x += scalar
        y += scalar
        z += scalar
        return this
    }

    fun subtract(other: Vector3): Vector3 {
        x -= other.x
        y -= other.y
        z -= other.z
        return this
    }

    fun subtract(scalar: Float): Vector3 {
        x -= scalar
        y -= scalar
        z -= scalar
        return this
    }

    fun multiply(other: Vector3): Vector3 {
        x *= other.x
        y *= other.y
        z *= other.z
        return this
    }

    fun multiply(scalar: Float): Vector3 {
        x *= scalar
        y *= scalar
        z *= scalar
        return this
    }

    fun divide(other: Vector3): Vector3 {
        x /= other.x
        y /= other.y
        z /= other.z
        return this
    }

    fun divide(scalar: Float): Vector3 {
        x /= scalar
        y /= scalar
        z /= scalar
        return this
    }

    // Vector operations
    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 {
        val cx = y * other.z - z * other.y
        val cy = z * other.x - x * other.z
        val cz = x * other.y - y * other.x
        return set(cx, cy, cz)
    }

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSquared(): Float = x * x + y * y + z * z

    fun normalize(): Vector3 {
        val len = length()
        return if (len > 0) divide(len) else this
    }

    fun distance(other: Vector3): Float = sqrt(distanceSquared(other))
    fun distanceSquared(other: Vector3): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return dx * dx + dy * dy + dz * dz
    }

    // Three.js compatibility aliases
    fun sub(other: Vector3): Vector3 = subtract(other)
    fun multiplyScalar(scalar: Float): Vector3 = multiply(scalar)
    fun divideScalar(scalar: Float): Vector3 = divide(scalar)
    fun addScalar(scalar: Float): Vector3 = add(scalar)
    fun distanceTo(other: Vector3): Float = distance(other)
    fun distanceToSquared(other: Vector3): Float = distanceSquared(other)
    fun lengthSq(): Float = lengthSquared()

    // Component-wise min/max operations
    fun min(other: Vector3): Vector3 {
        x = minOf(x, other.x)
        y = minOf(y, other.y)
        z = minOf(z, other.z)
        return this
    }

    fun max(other: Vector3): Vector3 {
        x = maxOf(x, other.x)
        y = maxOf(y, other.y)
        z = maxOf(z, other.z)
        return this
    }

    // Clamp operations
    fun clamp(min: Vector3, max: Vector3): Vector3 {
        x = x.coerceIn(min.x, max.x)
        y = y.coerceIn(min.y, max.y)
        z = z.coerceIn(min.z, max.z)
        return this
    }

    fun clampScalar(min: Float, max: Float): Vector3 {
        x = x.coerceIn(min, max)
        y = y.coerceIn(min, max)
        z = z.coerceIn(min, max)
        return this
    }

    fun lerp(other: Vector3, t: Float): Vector3 {
        x += (other.x - x) * t
        y += (other.y - y) * t
        z += (other.z - z) * t
        return this
    }

    fun negate(): Vector3 {
        x = -x
        y = -y
        z = -z
        return this
    }

    fun floor(): Vector3 {
        x = floor(x)
        y = floor(y)
        z = floor(z)
        return this
    }

    fun ceil(): Vector3 {
        x = ceil(x)
        y = ceil(y)
        z = ceil(z)
        return this
    }

    fun round(): Vector3 {
        x = round(x)
        y = round(y)
        z = round(z)
        return this
    }

    // Matrix transformations
    fun applyMatrix4(matrix: Matrix4): Vector3 {
        val e = matrix.elements
        val oldX = x
        val oldY = y
        val oldZ = z
        val w = 1f / (e[3] * oldX + e[7] * oldY + e[11] * oldZ + e[15])

        x = (e[0] * oldX + e[4] * oldY + e[8] * oldZ + e[12]) * w
        y = (e[1] * oldX + e[5] * oldY + e[9] * oldZ + e[13]) * w
        z = (e[2] * oldX + e[6] * oldY + e[10] * oldZ + e[14]) * w

        return this
    }

    fun applyMatrix3(matrix: Matrix3): Vector3 {
        val e = matrix.elements
        val oldX = x
        val oldY = y
        val oldZ = z

        x = e[0] * oldX + e[3] * oldY + e[6] * oldZ
        y = e[1] * oldX + e[4] * oldY + e[7] * oldZ
        z = e[2] * oldX + e[5] * oldY + e[8] * oldZ

        return this
    }

    fun applyQuaternion(quaternion: Quaternion): Vector3 {
        val qx = quaternion.x
        val qy = quaternion.y
        val qz = quaternion.z
        val qw = quaternion.w

        // Calculate quat * vector
        val ix = qw * x + qy * z - qz * y
        val iy = qw * y + qz * x - qx * z
        val iz = qw * z + qx * y - qy * x
        val iw = -qx * x - qy * y - qz * z

        // Calculate result * inverse quat
        x = ix * qw + iw * -qx + iy * -qz - iz * -qy
        y = iy * qw + iw * -qy + iz * -qx - ix * -qz
        z = iz * qw + iw * -qz + ix * -qy - iy * -qx

        return this
    }

    // Utility
    fun equals(other: Vector3, epsilon: Float = 0.000001f): Boolean {
        return abs(x - other.x) < epsilon &&
               abs(y - other.y) < epsilon &&
               abs(z - other.z) < epsilon
    }

    fun isZero(): Boolean = x == 0f && y == 0f && z == 0f

    override fun toString(): String = "Vector3($x, $y, $z)"

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val UNIT_X = Vector3(1f, 0f, 0f)
        val UNIT_Y = Vector3(0f, 1f, 0f)
        val UNIT_Z = Vector3(0f, 0f, 1f)
        val UP = Vector3(0f, 1f, 0f)
        val DOWN = Vector3(0f, -1f, 0f)
        val LEFT = Vector3(-1f, 0f, 0f)
        val RIGHT = Vector3(1f, 0f, 0f)
        val FORWARD = Vector3(0f, 0f, -1f)
        val BACK = Vector3(0f, 0f, 1f)
    }
}