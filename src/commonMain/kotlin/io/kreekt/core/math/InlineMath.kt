package io.kreekt.core.math

import kotlin.jvm.JvmInline

/**
 * Inline value classes for zero-allocation math operations
 * These classes are optimized for hot paths and avoid object allocation overhead
 */

/**
 * Inline wrapper for angle values to avoid Float boxing
 */
@JvmInline
value class Angle(val radians: Float) {
    val degrees: Float get() = radians * 57.29578f // 180/PI

    companion object {
        fun fromDegrees(degrees: Float) = Angle(degrees * 0.017453292f) // PI/180
        fun fromRadians(radians: Float) = Angle(radians)
    }
}

/**
 * Inline wrapper for distance values
 */
@JvmInline
value class Distance(val value: Float) {
    operator fun compareTo(other: Distance): Int = value.compareTo(other.value)
    operator fun plus(other: Distance): Distance = Distance(value + other.value)
    operator fun minus(other: Distance): Distance = Distance(value - other.value)
}

/**
 * Fast math utilities for hot paths
 */
object FastMath {
    /**
     * Fast approximation of 1/sqrt(x) using inverse square root
     */
    inline fun invSqrt(x: Float): Float {
        // Note: On modern JVMs, kotlin.math.sqrt is already optimized
        return 1f / kotlin.math.sqrt(x)
    }

    /**
     * Fast vector length squared (avoids sqrt)
     */
    inline fun lengthSquared(x: Float, y: Float, z: Float): Float {
        return x * x + y * y + z * z
    }

    /**
     * Fast vector length
     */
    inline fun length(x: Float, y: Float, z: Float): Float {
        return kotlin.math.sqrt(x * x + y * y + z * z)
    }

    /**
     * Fast dot product
     */
    inline fun dot(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float
    ): Float {
        return x1 * x2 + y1 * y2 + z1 * z2
    }

    /**
     * Fast distance squared
     */
    inline fun distanceSquared(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        return dx * dx + dy * dy + dz * dz
    }

    /**
     * Fast lerp without allocation
     */
    inline fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    /**
     * Clamp value without branching
     */
    inline fun clamp(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }
}

/**
 * Extension functions for inline math operations
 */
inline fun Vector3.setFast(x: Float, y: Float, z: Float) {
    this.x = x
    this.y = y
    this.z = z
}

inline fun Vector3.addFast(x: Float, y: Float, z: Float) {
    this.x += x
    this.y += y
    this.z += z
}

inline fun Vector3.multiplyScalarFast(scalar: Float) {
    this.x *= scalar
    this.y *= scalar
    this.z *= scalar
}

inline fun Vector3.normalizeFast(): Vector3 {
    val len = FastMath.length(x, y, z)
    if (len > 0f) {
        val invLen = 1f / len
        x *= invLen
        y *= invLen
        z *= invLen
    }
    return this
}

/**
 * Batch operations for arrays of vectors (SIMD-friendly)
 */
object VectorBatch {
    /**
     * Normalize array of vectors in place
     */
    fun normalizeArray(vectors: Array<Vector3>) {
        for (v in vectors) {
            v.normalizeFast()
        }
    }

    /**
     * Scale array of vectors by scalar
     */
    fun scaleArray(vectors: Array<Vector3>, scalar: Float) {
        for (v in vectors) {
            v.multiplyScalarFast(scalar)
        }
    }

    /**
     * Transform array of vectors by matrix
     */
    fun transformArray(vectors: Array<Vector3>, matrix: Matrix4) {
        for (v in vectors) {
            v.applyMatrix4(matrix)
        }
    }
}
