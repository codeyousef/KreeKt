/**
 * Matrix extensions for physics calculations
 * Provides missing transformation methods for Matrix4 and Matrix3
 */
package io.kreekt.physics

import io.kreekt.core.math.*
import kotlin.math.*

/**
 * Matrix4 extensions for physics
 */

/**
 * Creates a matrix from translation, rotation, and scale
 */
fun Matrix4.Companion.fromTranslationRotationScale(
    translation: Vector3,
    rotation: Quaternion,
    scale: Vector3
): Matrix4 {
    val matrix = Matrix4()

    // Apply scale
    val sx = scale.x
    val sy = scale.y
    val sz = scale.z

    // Apply rotation (quaternion to matrix)
    val x = rotation.x
    val y = rotation.y
    val z = rotation.z
    val w = rotation.w

    val x2 = x + x
    val y2 = y + y
    val z2 = z + z
    val xx = x * x2
    val xy = x * y2
    val xz = x * z2
    val yy = y * y2
    val yz = y * z2
    val zz = z * z2
    val wx = w * x2
    val wy = w * y2
    val wz = w * z2

    // Set matrix elements
    matrix.elements[0] = (1f - (yy + zz)) * sx
    matrix.elements[1] = (xy + wz) * sx
    matrix.elements[2] = (xz - wy) * sx
    matrix.elements[3] = 0f

    matrix.elements[4] = (xy - wz) * sy
    matrix.elements[5] = (1f - (xx + zz)) * sy
    matrix.elements[6] = (yz + wx) * sy
    matrix.elements[7] = 0f

    matrix.elements[8] = (xz + wy) * sz
    matrix.elements[9] = (yz - wx) * sz
    matrix.elements[10] = (1f - (xx + yy)) * sz
    matrix.elements[11] = 0f

    // Apply translation
    matrix.elements[12] = translation.x
    matrix.elements[13] = translation.y
    matrix.elements[14] = translation.z
    matrix.elements[15] = 1f

    return matrix
}

/**
 * Creates a matrix from translation and rotation
 */
fun Matrix4.Companion.fromTranslationRotation(
    translation: Vector3,
    rotation: Quaternion
): Matrix4 = fromTranslationRotationScale(translation, rotation, Vector3.ONE)

/**
 * Creates a matrix from translation only
 */
fun Matrix4.Companion.fromTranslation(translation: Vector3): Matrix4 =
    fromTranslationRotationScale(translation, Quaternion.IDENTITY, Vector3.ONE)

/**
 * Gets the translation component from a transformation matrix
 */
fun Matrix4.getTranslation(): Vector3 = getPosition()

/**
 * Gets the rotation component from a transformation matrix
 */
fun Matrix4.getRotation(): Quaternion {
    val scale = getScale()

    // Normalize the matrix to remove scale
    val m11 = elements[0] / scale.x
    val m12 = elements[4] / scale.y
    val m13 = elements[8] / scale.z
    val m21 = elements[1] / scale.x
    val m22 = elements[5] / scale.y
    val m23 = elements[9] / scale.z
    val m31 = elements[2] / scale.x
    val m32 = elements[6] / scale.y
    val m33 = elements[10] / scale.z

    // Convert matrix to quaternion
    val trace = m11 + m22 + m33

    return if (trace > 0f) {
        val s = sqrt(trace + 1f) * 2f // s = 4 * qw
        val w = 0.25f * s
        val x = (m32 - m23) / s
        val y = (m13 - m31) / s
        val z = (m21 - m12) / s
        Quaternion(x, y, z, w)
    } else if (m11 > m22 && m11 > m33) {
        val s = sqrt(1f + m11 - m22 - m33) * 2f // s = 4 * qx
        val w = (m32 - m23) / s
        val x = 0.25f * s
        val y = (m12 + m21) / s
        val z = (m13 + m31) / s
        Quaternion(x, y, z, w)
    } else if (m22 > m33) {
        val s = sqrt(1f + m22 - m11 - m33) * 2f // s = 4 * qy
        val w = (m13 - m31) / s
        val x = (m12 + m21) / s
        val y = 0.25f * s
        val z = (m23 + m32) / s
        Quaternion(x, y, z, w)
    } else {
        val s = sqrt(1f + m33 - m11 - m22) * 2f // s = 4 * qz
        val w = (m21 - m12) / s
        val x = (m13 + m31) / s
        val y = (m23 + m32) / s
        val z = 0.25f * s
        Quaternion(x, y, z, w)
    }
}

/**
 * Matrix3 companion and extensions
 */

/**
 * Creates a Matrix3 identity
 */
val Matrix3.Companion.IDENTITY: Matrix3
    get() = Matrix3(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )

/**
 * Creates a Matrix3 zero matrix
 */
val Matrix3.Companion.ZERO: Matrix3
    get() = Matrix3(
        0f, 0f, 0f,
        0f, 0f, 0f,
        0f, 0f, 0f
    )

/**
 * Matrix3 inverse calculation
 */
fun Matrix3.inverse(): Matrix3 {
    val det = determinant()
    if (abs(det) < 0.000001f) {
        return Matrix3.ZERO // Singular matrix
    }

    val invDet = 1f / det

    return Matrix3(
        (m11 * m22 - m12 * m21) * invDet,
        (m02 * m21 - m01 * m22) * invDet,
        (m01 * m12 - m02 * m11) * invDet,

        (m12 * m20 - m10 * m22) * invDet,
        (m00 * m22 - m02 * m20) * invDet,
        (m02 * m10 - m00 * m12) * invDet,

        (m10 * m21 - m11 * m20) * invDet,
        (m01 * m20 - m00 * m21) * invDet,
        (m00 * m11 - m01 * m10) * invDet
    )
}

/**
 * Matrix3 determinant calculation
 */
fun Matrix3.determinant(): Float {
    return m00 * (m11 * m22 - m12 * m21) -
           m01 * (m10 * m22 - m12 * m20) +
           m02 * (m10 * m21 - m11 * m20)
}

/**
 * Matrix3 transpose
 */
fun Matrix3.transpose(): Matrix3 {
    return Matrix3(
        m00, m10, m20,
        m01, m11, m21,
        m02, m12, m22
    )
}

/**
 * Matrix3 multiplication with Vector3
 */
operator fun Matrix3.times(vector: Vector3): Vector3 {
    return Vector3(
        m00 * vector.x + m01 * vector.y + m02 * vector.z,
        m10 * vector.x + m11 * vector.y + m12 * vector.z,
        m20 * vector.x + m21 * vector.y + m22 * vector.z
    )
}

/**
 * Matrix3 addition
 */
operator fun Matrix3.plus(other: Matrix3): Matrix3 {
    return Matrix3(
        m00 + other.m00, m01 + other.m01, m02 + other.m02,
        m10 + other.m10, m11 + other.m11, m12 + other.m12,
        m20 + other.m20, m21 + other.m21, m22 + other.m22
    )
}

/**
 * Matrix3 multiplication
 */
operator fun Matrix3.times(other: Matrix3): Matrix3 {
    return Matrix3(
        m00 * other.m00 + m01 * other.m10 + m02 * other.m20,
        m00 * other.m01 + m01 * other.m11 + m02 * other.m21,
        m00 * other.m02 + m01 * other.m12 + m02 * other.m22,

        m10 * other.m00 + m11 * other.m10 + m12 * other.m20,
        m10 * other.m01 + m11 * other.m11 + m12 * other.m21,
        m10 * other.m02 + m11 * other.m12 + m12 * other.m22,

        m20 * other.m00 + m21 * other.m10 + m22 * other.m20,
        m20 * other.m01 + m21 * other.m11 + m22 * other.m21,
        m20 * other.m02 + m21 * other.m12 + m22 * other.m22
    )
}

/**
 * Quaternion extensions for physics
 */

/**
 * Quaternion identity constant
 */
val Quaternion.Companion.IDENTITY: Quaternion
    get() = Quaternion(0f, 0f, 0f, 1f)

/**
 * Create a quaternion from axis and angle
 */
fun Quaternion.Companion.fromAxisAngle(axis: Vector3, angle: Float): Quaternion {
    val halfAngle = angle * 0.5f
    val s = sin(halfAngle)
    val normalizedAxis = axis.normalized()

    return Quaternion(
        normalizedAxis.x * s,
        normalizedAxis.y * s,
        normalizedAxis.z * s,
        cos(halfAngle)
    )
}

/**
 * Normalize a quaternion
 */
fun Quaternion.normalized(): Quaternion {
    val length = sqrt(x * x + y * y + z * z + w * w)
    return if (length > 0.001f) {
        Quaternion(x / length, y / length, z / length, w / length)
    } else {
        Quaternion.IDENTITY
    }
}

/**
 * Quaternion inverse
 */
fun Quaternion.inverse(): Quaternion {
    val lengthSq = x * x + y * y + z * z + w * w
    return if (lengthSq > 0.001f) {
        Quaternion(-x / lengthSq, -y / lengthSq, -z / lengthSq, w / lengthSq)
    } else {
        Quaternion.IDENTITY
    }
}

/**
 * Quaternion multiplication
 */
operator fun Quaternion.times(other: Quaternion): Quaternion {
    return Quaternion(
        w * other.x + x * other.w + y * other.z - z * other.y,
        w * other.y - x * other.z + y * other.w + z * other.x,
        w * other.z + x * other.y - y * other.x + z * other.w,
        w * other.w - x * other.x - y * other.y - z * other.z
    )
}

/**
 * Additional Vector3 extensions for physics
 */

/**
 * Set vector components from another vector
 */
fun Vector3.set(other: Vector3): Vector3 {
    this.x = other.x
    this.y = other.y
    this.z = other.z
    return this
}

/**
 * Get vector component by index
 */
fun Vector3.componentAt(index: Int): Float = when (index) {
    0 -> x
    1 -> y
    2 -> z
    else -> throw IndexOutOfBoundsException("Vector3 component index must be 0, 1, or 2")
}

/**
 * Set vector component by index
 */
fun Vector3.setComponent(index: Int, value: Float) {
    when (index) {
        0 -> x = value
        1 -> y = value
        2 -> z = value
        else -> throw IndexOutOfBoundsException("Vector3 component index must be 0, 1, or 2")
    }
}

/**
 * Find the maximum component value
 */
fun Vector3.maxComponent(): Float = maxOf(x, maxOf(y, z))

/**
 * Find the minimum component value
 */
fun Vector3.minComponent(): Float = minOf(x, minOf(y, z))

/**
 * Constrain vector length to a range
 */
fun Vector3.coerceLength(minLength: Float, maxLength: Float): Vector3 {
    val currentLength = length()
    return when {
        currentLength < minLength -> if (currentLength > 0f) normalized() * minLength else Vector3.ZERO
        currentLength > maxLength -> normalized() * maxLength
        else -> this
    }
}

/**
 * Check if vector is nearly zero
 */
fun Vector3.isNearlyZero(epsilon: Float = 0.001f): Boolean {
    return abs(x) < epsilon && abs(y) < epsilon && abs(z) < epsilon
}

/**
 * Check if vector is unit length
 */
fun Vector3.isNormalized(epsilon: Float = 0.001f): Boolean {
    return abs(length() - 1f) < epsilon
}

/**
 * Get a vector perpendicular to this one
 */
fun Vector3.getPerpendicularVector(): Vector3 {
    val absX = abs(x)
    val absY = abs(y)
    val absZ = abs(z)

    return when {
        absX < absY && absX < absZ -> Vector3.UNIT_X.cross(this).normalized()
        absY < absZ -> Vector3.UNIT_Y.cross(this).normalized()
        else -> Vector3.UNIT_Z.cross(this).normalized()
    }
}

/**
 * Box3 extensions for physics
 */

/**
 * Check if Box3 is empty
 */
fun Box3.isEmpty(): Boolean {
    return max.x < min.x || max.y < min.y || max.z < min.z
}

/**
 * Expand box to include a point
 */
fun Box3.expandByPoint(point: Vector3) {
    if (isEmpty()) {
        min.set(point)
        max.set(point)
    } else {
        min.x = minOf(min.x, point.x)
        min.y = minOf(min.y, point.y)
        min.z = minOf(min.z, point.z)
        max.x = maxOf(max.x, point.x)
        max.y = maxOf(max.y, point.y)
        max.z = maxOf(max.z, point.z)
    }
}

/**
 * Check if box intersects another box
 */
fun Box3.intersectsBox(other: Box3): Boolean {
    return (max.x >= other.min.x && min.x <= other.max.x) &&
           (max.y >= other.min.y && min.y <= other.max.y) &&
           (max.z >= other.min.z && min.z <= other.max.z)
}