package io.kreekt.core.math

import kotlin.math.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A 4x4 matrix stored in column-major order (OpenGL/WebGL convention).
 * Compatible with Three.js Matrix4 API.
 *
 * Matrix layout:
 * | m[0] m[4] m[8]  m[12] |   | m11 m12 m13 m14 |
 * | m[1] m[5] m[9]  m[13] |   | m21 m22 m23 m24 |
 * | m[2] m[6] m[10] m[14] | = | m31 m32 m33 m34 |
 * | m[3] m[7] m[11] m[15] |   | m41 m42 m43 m44 |
 */
data class Matrix4(
    val elements: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f,  // column 0
        0f, 1f, 0f, 0f,  // column 1
        0f, 0f, 1f, 0f,  // column 2
        0f, 0f, 0f, 1f   // column 3
    )
) {
    // Matrix element accessors (row-column notation)
    val m00: Float get() = elements[0]
    val m10: Float get() = elements[1]
    val m20: Float get() = elements[2]
    val m30: Float get() = elements[3]

    val m01: Float get() = elements[4]
    val m11: Float get() = elements[5]
    val m21: Float get() = elements[6]
    val m31: Float get() = elements[7]

    val m02: Float get() = elements[8]
    val m12: Float get() = elements[9]
    val m22: Float get() = elements[10]
    val m32: Float get() = elements[11]

    val m03: Float get() = elements[12]
    val m13: Float get() = elements[13]
    val m23: Float get() = elements[14]
    val m33: Float get() = elements[15]

    companion object {
        val IDENTITY: Matrix4
            get() = Matrix4()

        /**
         * Creates an identity matrix
         */
        fun identity(): Matrix4 = Matrix4()

        /**
         * Creates a translation matrix
         */
        fun translation(x: Float, y: Float, z: Float): Matrix4 =
            Matrix4().makeTranslation(x, y, z)

        /**
         * Creates a scale matrix
         */
        fun scale(x: Float, y: Float, z: Float): Matrix4 =
            Matrix4().makeScale(x, y, z)

        /**
         * Creates a rotation matrix around X axis
         */
        fun rotationX(theta: Float): Matrix4 =
            Matrix4().makeRotationX(theta)

        /**
         * Creates a rotation matrix around Y axis
         */
        fun rotationY(theta: Float): Matrix4 =
            Matrix4().makeRotationY(theta)

        /**
         * Creates a rotation matrix around Z axis
         */
        fun rotationZ(theta: Float): Matrix4 =
            Matrix4().makeRotationZ(theta)
    }

    /**
     * Sets this matrix to identity
     */
    fun identity(): Matrix4 {
        elements[0] = 1f; elements[4] = 0f; elements[8]  = 0f; elements[12] = 0f
        elements[1] = 0f; elements[5] = 1f; elements[9]  = 0f; elements[13] = 0f
        elements[2] = 0f; elements[6] = 0f; elements[10] = 1f; elements[14] = 0f
        elements[3] = 0f; elements[7] = 0f; elements[11] = 0f; elements[15] = 1f
        return this
    }

    /**
     * Checks if this matrix is the identity matrix
     */
    fun isIdentity(): Boolean {
        return elements[0] == 1f && elements[4] == 0f && elements[8]  == 0f && elements[12] == 0f &&
               elements[1] == 0f && elements[5] == 1f && elements[9]  == 0f && elements[13] == 0f &&
               elements[2] == 0f && elements[6] == 0f && elements[10] == 1f && elements[14] == 0f &&
               elements[3] == 0f && elements[7] == 0f && elements[11] == 0f && elements[15] == 1f
    }

    /**
     * Creates a copy of this matrix
     */
    fun clone(): Matrix4 {
        return Matrix4(elements.copyOf())
    }

    /**
     * Copies values from another matrix
     */
    fun copy(matrix: Matrix4): Matrix4 {
        matrix.elements.copyInto(elements)
        return this
    }

    /**
     * Sets matrix elements from individual values
     */
    fun set(
        m11: Float, m12: Float, m13: Float, m14: Float,
        m21: Float, m22: Float, m23: Float, m24: Float,
        m31: Float, m32: Float, m33: Float, m34: Float,
        m41: Float, m42: Float, m43: Float, m44: Float
    ): Matrix4 {
        elements[0] = m11; elements[4] = m12; elements[8]  = m13; elements[12] = m14
        elements[1] = m21; elements[5] = m22; elements[9]  = m23; elements[13] = m24
        elements[2] = m31; elements[6] = m32; elements[10] = m33; elements[14] = m34
        elements[3] = m41; elements[7] = m42; elements[11] = m43; elements[15] = m44
        return this
    }

    /**
     * Makes this matrix a translation matrix
     */
    fun makeTranslation(x: Float, y: Float, z: Float): Matrix4 {
        identity()
        elements[12] = x
        elements[13] = y
        elements[14] = z
        return this
    }

    /**
     * Makes this matrix a scale matrix
     */
    fun makeScale(x: Float, y: Float, z: Float): Matrix4 {
        identity()
        elements[0] = x
        elements[5] = y
        elements[10] = z
        return this
    }

    /**
     * Makes this matrix a rotation around X axis
     */
    fun makeRotationX(theta: Float): Matrix4 {
        val c = cos(theta)
        val s = sin(theta)
        identity()
        elements[5] = c; elements[9] = -s
        elements[6] = s; elements[10] = c
        return this
    }

    /**
     * Makes this matrix a rotation around Y axis
     */
    fun makeRotationY(theta: Float): Matrix4 {
        val c = cos(theta)
        val s = sin(theta)
        identity()
        elements[0] = c; elements[8] = s
        elements[2] = -s; elements[10] = c
        return this
    }

    /**
     * Makes this matrix a rotation around Z axis
     */
    fun makeRotationZ(theta: Float): Matrix4 {
        val c = cos(theta)
        val s = sin(theta)
        identity()
        elements[0] = c; elements[4] = -s
        elements[1] = s; elements[5] = c
        return this
    }

    /**
     * Multiplies this matrix by another matrix
     */
    fun multiply(matrix: Matrix4): Matrix4 {
        return multiplyMatrices(this, matrix)
    }

    /**
     * Multiplies another matrix by this matrix (order matters!)
     */
    fun premultiply(matrix: Matrix4): Matrix4 {
        return multiplyMatrices(matrix, this)
    }

    /**
     * Multiplies two matrices and stores result in this matrix
     */
    fun multiplyMatrices(a: Matrix4, b: Matrix4): Matrix4 {
        val ae = a.elements
        val be = b.elements
        val te = elements

        val a11 = ae[0]; val a12 = ae[4]; val a13 = ae[8]; val a14 = ae[12]
        val a21 = ae[1]; val a22 = ae[5]; val a23 = ae[9]; val a24 = ae[13]
        val a31 = ae[2]; val a32 = ae[6]; val a33 = ae[10]; val a34 = ae[14]
        val a41 = ae[3]; val a42 = ae[7]; val a43 = ae[11]; val a44 = ae[15]

        val b11 = be[0]; val b12 = be[4]; val b13 = be[8]; val b14 = be[12]
        val b21 = be[1]; val b22 = be[5]; val b23 = be[9]; val b24 = be[13]
        val b31 = be[2]; val b32 = be[6]; val b33 = be[10]; val b34 = be[14]
        val b41 = be[3]; val b42 = be[7]; val b43 = be[11]; val b44 = be[15]

        te[0] = a11 * b11 + a12 * b21 + a13 * b31 + a14 * b41
        te[4] = a11 * b12 + a12 * b22 + a13 * b32 + a14 * b42
        te[8] = a11 * b13 + a12 * b23 + a13 * b33 + a14 * b43
        te[12] = a11 * b14 + a12 * b24 + a13 * b34 + a14 * b44

        te[1] = a21 * b11 + a22 * b21 + a23 * b31 + a24 * b41
        te[5] = a21 * b12 + a22 * b22 + a23 * b32 + a24 * b42
        te[9] = a21 * b13 + a22 * b23 + a23 * b33 + a24 * b43
        te[13] = a21 * b14 + a22 * b24 + a23 * b34 + a24 * b44

        te[2] = a31 * b11 + a32 * b21 + a33 * b31 + a34 * b41
        te[6] = a31 * b12 + a32 * b22 + a33 * b32 + a34 * b42
        te[10] = a31 * b13 + a32 * b23 + a33 * b33 + a34 * b43
        te[14] = a31 * b14 + a32 * b24 + a33 * b34 + a34 * b44

        te[3] = a41 * b11 + a42 * b21 + a43 * b31 + a44 * b41
        te[7] = a41 * b12 + a42 * b22 + a43 * b32 + a44 * b42
        te[11] = a41 * b13 + a42 * b23 + a43 * b33 + a44 * b43
        te[15] = a41 * b14 + a42 * b24 + a43 * b34 + a44 * b44

        return this
    }

    /**
     * Calculates the determinant of this matrix
     */
    fun determinant(): Float {
        val n11 = elements[0]; val n12 = elements[4]; val n13 = elements[8]; val n14 = elements[12]
        val n21 = elements[1]; val n22 = elements[5]; val n23 = elements[9]; val n24 = elements[13]
        val n31 = elements[2]; val n32 = elements[6]; val n33 = elements[10]; val n34 = elements[14]
        val n41 = elements[3]; val n42 = elements[7]; val n43 = elements[11]; val n44 = elements[15]

        return (
            n41 * (
                +n14 * n23 * n32
                - n13 * n24 * n32
                - n14 * n22 * n33
                + n12 * n24 * n33
                + n13 * n22 * n34
                - n12 * (n23 * n34)
            ) +
            n42 * (
                +n11 * n23 * n34
                - n11 * n24 * n33
                + n14 * n21 * n33
                - n13 * n21 * n34
                + n13 * n24 * n31
                - n14 * (n23 * n31)
            ) +
            n43 * (
                +n11 * n24 * n32
                - n11 * n22 * n34
                - n14 * n21 * n32
                + n12 * n21 * n34
                + n14 * n22 * n31
                - n12 * (n24 * n31)
            ) +
            n44 * (
                -n13 * n22 * n31
                - n11 * n23 * n32
                + n11 * n22 * n33
                + n13 * n21 * n32
                - n12 * n21 * n33
                + n12 * (n23 * n31)
            )
        )
    }

    /**
     * Inverts this matrix
     */
    fun invert(): Matrix4 {
        val n11 = elements[0]; val n21 = elements[1]; val n31 = elements[2]; val n41 = elements[3]
        val n12 = elements[4]; val n22 = elements[5]; val n32 = elements[6]; val n42 = elements[7]
        val n13 = elements[8]; val n23 = elements[9]; val n33 = elements[10]; val n43 = elements[11]
        val n14 = elements[12]; val n24 = elements[13]; val n34 = elements[14]; val n44 = elements[15]

        val t11 = n23 * n34 * n42 - n24 * n33 * n42 + n24 * n32 * n43 - n22 * n34 * n43 - n23 * n32 * n44 + n22 * n33 * n44
        val t12 = n14 * n33 * n42 - n13 * n34 * n42 - n14 * n32 * n43 + n12 * n34 * n43 + n13 * n32 * n44 - n12 * n33 * n44
        val t13 = n13 * n24 * n42 - n14 * n23 * n42 + n14 * n22 * n43 - n12 * n24 * n43 - n13 * n22 * n44 + n12 * n23 * n44
        val t14 = n14 * n23 * n32 - n13 * n24 * n32 - n14 * n22 * n33 + n12 * n24 * n33 + n13 * n22 * n34 - n12 * n23 * n34

        val det = n11 * t11 + n21 * t12 + n31 * t13 + n41 * t14

        if (det == 0f) throw IllegalArgumentException("Cannot invert matrix with determinant 0")

        val detInv = 1f / det

        elements[0] = t11 * detInv
        elements[1] = (n24 * n33 * n41 - n23 * n34 * n41 - n24 * n31 * n43 + n21 * n34 * n43 + n23 * n31 * n44 - n21 * (n33 * n44)) * detInv
        elements[2] = (n22 * n34 * n41 - n24 * n32 * n41 + n24 * n31 * n42 - n21 * n34 * n42 - n22 * n31 * n44 + n21 * (n32 * n44)) * detInv
        elements[3] = (n23 * n32 * n41 - n22 * n33 * n41 - n23 * n31 * n42 + n21 * n33 * n42 + n22 * n31 * n43 - n21 * (n32 * n43)) * detInv

        elements[4] = t12 * detInv
        elements[5] = (n13 * n34 * n41 - n14 * n33 * n41 + n14 * n31 * n43 - n11 * n34 * n43 - n13 * n31 * n44 + n11 * (n33 * n44)) * detInv
        elements[6] = (n14 * n32 * n41 - n12 * n34 * n41 - n14 * n31 * n42 + n11 * n34 * n42 + n12 * n31 * n44 - n11 * (n32 * n44)) * detInv
        elements[7] = (n12 * n33 * n41 - n13 * n32 * n41 + n13 * n31 * n42 - n11 * n33 * n42 - n12 * n31 * n43 + n11 * (n32 * n43)) * detInv

        elements[8] = t13 * detInv
        elements[9] = (n14 * n23 * n41 - n13 * n24 * n41 - n14 * n21 * n43 + n11 * n24 * n43 + n13 * n21 * n44 - n11 * (n23 * n44)) * detInv
        elements[10] = (n12 * n24 * n41 - n14 * n22 * n41 + n14 * n21 * n42 - n11 * n24 * n42 - n12 * n21 * n44 + n11 * (n22 * n44)) * detInv
        elements[11] = (n13 * n22 * n41 - n12 * n23 * n41 - n13 * n21 * n42 + n11 * n23 * n42 + n12 * n21 * n43 - n11 * (n22 * n43)) * detInv

        elements[12] = t14 * detInv
        elements[13] = (n13 * n24 * n31 - n14 * n23 * n31 + n14 * n21 * n33 - n11 * n24 * n33 - n13 * n21 * n34 + n11 * (n23 * n34)) * detInv
        elements[14] = (n14 * n22 * n31 - n12 * n24 * n31 - n14 * n21 * n32 + n11 * n24 * n32 + n12 * n21 * n34 - n11 * (n22 * n34)) * detInv
        elements[15] = (n12 * n23 * n31 - n13 * n22 * n31 + n13 * n21 * n32 - n11 * n23 * n32 - n12 * n21 * n33 + n11 * (n22 * n33)) * detInv

        return this
    }

    /**
     * Transposes this matrix
     */
    fun transpose(): Matrix4 {
        var tmp = elements[1]; elements[1] = elements[4]; elements[4] = tmp
        tmp = elements[2]; elements[2] = elements[8]; elements[8] = tmp
        tmp = elements[6]; elements[6] = elements[9]; elements[9] = tmp
        tmp = elements[3]; elements[3] = elements[12]; elements[12] = tmp
        tmp = elements[7]; elements[7] = elements[13]; elements[13] = tmp
        tmp = elements[11]; elements[11] = elements[14]; elements[14] = tmp
        return this
    }

    /**
     * Extracts position from transformation matrix
     */
    fun getPosition(): Vector3 {
        return Vector3(elements[12], elements[13], elements[14])
    }

    /**
     * Sets position in transformation matrix
     */
    fun setPosition(x: Float, y: Float, z: Float): Matrix4 {
        elements[12] = x
        elements[13] = y
        elements[14] = z
        return this
    }

    /**
     * Sets position in transformation matrix
     */
    fun setPosition(v: Vector3): Matrix4 {
        return setPosition(v.x, v.y, v.z)
    }

    /**
     * Extracts scale from transformation matrix
     */
    fun getScale(): Vector3 {
        val sx = Vector3(elements[0], elements[1], elements[2]).length()
        val sy = Vector3(elements[4], elements[5], elements[6]).length()
        val sz = Vector3(elements[8], elements[9], elements[10]).length()

        // Check for negative determinant (reflection)
        if (determinant() < 0) {
            return Vector3(-sx, sy, sz)
        }

        return Vector3(sx, sy, sz)
    }

    /**
     * Creates a perspective projection matrix
     */
    fun makePerspective(left: Float, right: Float, top: Float, bottom: Float, near: Float, far: Float): Matrix4 {
        val x = 2f * near / (right - left)
        val y = 2f * near / (top - bottom)
        val a = (right + left) / (right - left)
        val b = (top + bottom) / (top - bottom)
        val c = -(far + near) / (far - near)
        val d = -2f * far * near / (far - near)

        elements[0] = x; elements[4] = 0f; elements[8] = a; elements[12] = 0f
        elements[1] = 0f; elements[5] = y; elements[9] = b; elements[13] = 0f
        elements[2] = 0f; elements[6] = 0f; elements[10] = c; elements[14] = d
        elements[3] = 0f; elements[7] = 0f; elements[11] = -1f; elements[15] = 0f

        return this
    }

    /**
     * Creates an orthographic projection matrix
     */
    fun makeOrthographic(left: Float, right: Float, top: Float, bottom: Float, near: Float, far: Float): Matrix4 {
        val w = 1f / (right - left)
        val h = 1f / (top - bottom)
        val p = 1f / (far - near)

        val x = (right + left) * w
        val y = (top + bottom) * h
        val z = (far + near) * p

        elements[0] = (2f * w); elements[4] = 0f; elements[8] = 0f; elements[12] = -x
        elements[1] = 0f; elements[5] = (2f * h); elements[9] = 0f; elements[13] = -y
        elements[2] = 0f; elements[6] = 0f; elements[10] = -(2f * p); elements[14] = -z
        elements[3] = 0f; elements[7] = 0f; elements[11] = 0f; elements[15] = 1f

        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Matrix4) return false
        return elements.contentEquals(other.elements)
    }

    override fun hashCode(): Int {
        return elements.contentHashCode()
    }

    override fun toString(): String {
        return "Matrix4(\n" +
               "  ${elements[0]}, ${elements[4]}, ${elements[8]}, ${elements[12]}\n" +
               "  ${elements[1]}, ${elements[5]}, ${elements[9]}, ${elements[13]}\n" +
               "  ${elements[2]}, ${elements[6]}, ${elements[10]}, ${elements[14]}\n" +
               "  ${elements[3]}, ${elements[7]}, ${elements[11]}, ${elements[15]}\n" +
               ")"
    }

    /**
     * Transforms a 3D point by this matrix
     */
    fun multiplyPoint3(point: Vector3): Vector3 {
        val x = point.x
        val y = point.y
        val z = point.z
        val e = elements

        val w = 1f / (e[3] * x + e[7] * y + e[11] * z + e[15])

        return Vector3(
            (e[0] * x + e[4] * y + e[8] * z + e[12]) * w,
            (e[1] * x + e[5] * y + e[9] * z + e[13]) * w,
            (e[2] * x + e[6] * y + e[10] * z + e[14]) * w
        )
    }

    /**
     * Extracts the translation component from this matrix
     */
    fun extractTranslation(target: Vector3 = Vector3()): Vector3 {
        target.x = elements[12]
        target.y = elements[13]
        target.z = elements[14]
        return target
    }

    /**
     * Gets the view matrix (inverse of this matrix)
     */
    val viewMatrix: Matrix4
        get() = clone().invert()

    /**
     * Returns a new inverted matrix (doesn't modify this matrix)
     */
    fun inverse(): Matrix4 = clone().invert()

    /**
     * Returns the matrix elements as an array
     */
    fun toArray(): FloatArray = elements.copyOf()

    /**
     * Copy matrix elements to an array at the specified offset
     */
    fun toArray(array: FloatArray, offset: Int = 0) {
        elements.copyInto(array, offset, 0, 16)
    }

    /**
     * Set matrix elements from an array at the specified offset
     */
    fun fromArray(array: FloatArray, offset: Int = 0): Matrix4 {
        array.copyInto(elements, 0, offset, offset + 16)
        return this
    }

    /**
     * Get translation component as Vector3
     */
    fun getTranslation(): Vector3 = Vector3(m03, m13, m23)

    /**
     * Get rotation component as Quaternion
     */
    fun getRotation(): Quaternion {
        val trace = m00 + m11 + m22
        return when {
            trace > 0f -> {
                val s = sqrt(trace + 1f) * 2f
                Quaternion(
                    (m21 - m12) / s,
                    (m02 - m20) / s,
                    (m10 - m01) / s,
                    0.25f * s
                )
            }
            m00 > m11 && m00 > m22 -> {
                val s = sqrt(1f + m00 - m11 - m22) * 2f
                Quaternion(
                    0.25f * s,
                    (m01 + m10) / s,
                    (m02 + m20) / s,
                    (m21 - m12) / s
                )
            }
            m11 > m22 -> {
                val s = sqrt(1f + m11 - m00 - m22) * 2f
                Quaternion(
                    (m01 + m10) / s,
                    0.25f * s,
                    (m12 + m21) / s,
                    (m02 - m20) / s
                )
            }
            else -> {
                val s = sqrt(1f + m22 - m00 - m11) * 2f
                Quaternion(
                    (m02 + m20) / s,
                    (m12 + m21) / s,
                    0.25f * s,
                    (m10 - m01) / s
                )
            }
        }
    }

    /**
     * Transform a point (with translation)
     */
    fun transformPoint(point: Vector3): Vector3 {
        val x = point.x * m00 + point.y * m01 + point.z * m02 + m03
        val y = point.x * m10 + point.y * m11 + point.z * m12 + m13
        val z = point.x * m20 + point.y * m21 + point.z * m22 + m23
        return Vector3(x, y, z)
    }

    /**
     * Transform a direction (without translation)
     */
    fun transformDirection(direction: Vector3): Vector3 {
        val x = direction.x * m00 + direction.y * m01 + direction.z * m02
        val y = direction.x * m10 + direction.y * m11 + direction.z * m12
        val z = direction.x * m20 + direction.y * m21 + direction.z * m22
        return Vector3(x, y, z)
    }

    /**
     * Apply translation to this matrix
     */
    fun translate(offset: Vector3): Matrix4 {
        return this.multiply(Matrix4.translation(offset.x, offset.y, offset.z))
    }

    /**
     * Apply rotation to this matrix
     */
    fun rotate(rotation: Quaternion): Matrix4 {
        val rotMatrix = Matrix4().makeRotationFromQuaternion(rotation)
        return this.multiply(rotMatrix)
    }

    /**
     * Create rotation matrix from quaternion
     */
    fun makeRotationFromQuaternion(q: Quaternion): Matrix4 {
        val x = q.x; val y = q.y; val z = q.z; val w = q.w
        val x2 = x + x; val y2 = y + y; val z2 = z + z
        val xx = x * x2; val xy = x * y2; val xz = x * z2
        val yy = y * y2; val yz = y * z2; val zz = z * z2
        val wx = w * x2; val wy = w * y2; val wz = w * z2

        elements[0] = 1f - (yy + zz)
        elements[4] = xy - wz
        elements[8] = xz + wy

        elements[1] = xy + wz
        elements[5] = 1f - (xx + zz)
        elements[9] = yz - wx

        elements[2] = xz - wy
        elements[6] = yz + wx
        elements[10] = 1f - (xx + yy)

        // Last row
        elements[3] = 0f
        elements[7] = 0f
        elements[11] = 0f

        // Last column
        elements[12] = 0f
        elements[13] = 0f
        elements[14] = 0f
        elements[15] = 1f

        return this
    }

    /**
     * Multiply operator for matrices
     */
    operator fun times(other: Matrix4): Matrix4 = clone().multiply(other)

    /**
     * Scales this matrix by the given vector
     */
    fun scale(scale: Vector3): Matrix4 {
        elements[0] *= scale.x
        elements[1] *= scale.x
        elements[2] *= scale.x
        elements[3] *= scale.x

        elements[4] *= scale.y
        elements[5] *= scale.y
        elements[6] *= scale.y
        elements[7] *= scale.y

        elements[8] *= scale.z
        elements[9] *= scale.z
        elements[10] *= scale.z
        elements[11] *= scale.z

        return this
    }

    /**
     * Compose this matrix from position, quaternion, and scale
     */
    fun compose(position: Vector3, quaternion: Quaternion, scale: Vector3): Matrix4 {
        makeRotationFromQuaternion(quaternion)
        this.scale(scale)
        setPosition(position)
        return this
    }

    /**
     * Decompose this matrix into position, quaternion, and scale
     */
    fun decompose(position: Vector3, quaternion: Quaternion, scale: Vector3): Matrix4 {
        // Extract position
        position.x = elements[12]
        position.y = elements[13]
        position.z = elements[14]

        // Extract scale
        val sx = Vector3(elements[0], elements[1], elements[2]).length()
        val sy = Vector3(elements[4], elements[5], elements[6]).length()
        val sz = Vector3(elements[8], elements[9], elements[10]).length()

        scale.x = sx
        scale.y = sy
        scale.z = sz

        // Remove scale from the matrix
        val invSx = 1f / sx
        val invSy = 1f / sy
        val invSz = 1f / sz

        val m11 = elements[0] * invSx
        val m12 = elements[4] * invSy
        val m13 = elements[8] * invSz
        val m21 = elements[1] * invSx
        val m22 = elements[5] * invSy
        val m23 = elements[9] * invSz
        val m31 = elements[2] * invSx
        val m32 = elements[6] * invSy
        val m33 = elements[10] * invSz

        // Extract quaternion from rotation matrix
        val trace = m11 + m22 + m33

        if (trace > 0) {
            val s = 0.5f / sqrt(trace + 1.0f)
            quaternion.w = 0.25f / s
            quaternion.x = (m32 - m23) * s
            quaternion.y = (m13 - m31) * s
            quaternion.z = (m21 - m12) * s
        } else if ((m11 > m22) && (m11 > m33)) {
            val s = 2.0f * sqrt(1.0f + m11 - m22 - m33)
            quaternion.w = (m32 - m23) / s
            quaternion.x = 0.25f * s
            quaternion.y = (m12 + m21) / s
            quaternion.z = (m13 + m31) / s
        } else if (m22 > m33) {
            val s = 2.0f * sqrt(1.0f + m22 - m11 - m33)
            quaternion.w = (m13 - m31) / s
            quaternion.x = (m12 + m21) / s
            quaternion.y = 0.25f * s
            quaternion.z = (m23 + m32) / s
        } else {
            val s = 2.0f * sqrt(1.0f + m33 - m11 - m22)
            quaternion.w = (m21 - m12) / s
            quaternion.x = (m13 + m31) / s
            quaternion.y = (m23 + m32) / s
            quaternion.z = 0.25f * s
        }

        return this
    }

    /**
     * Set this matrix to look from eye position to target position
     * @param eye The position of the camera
     * @param target The position to look at
     * @param up The up direction (usually (0, 1, 0))
     */
    fun lookAt(eye: Vector3, target: Vector3, up: Vector3): Matrix4 {
        val z = eye.clone().sub(target).normalize()

        if (z.lengthSq() == 0f) {
            // eye and target are in the same position
            z.z = 1f
        }

        val x = up.clone().cross(z).normalize()

        if (x.lengthSq() == 0f) {
            // up and z are parallel
            if (kotlin.math.abs(up.z) == 1f) {
                z.x += 0.0001f
            } else {
                z.z += 0.0001f
            }
            z.normalize()
            x.copy(up).cross(z).normalize()
        }

        val y = z.clone().cross(x)

        elements[0] = x.x; elements[4] = y.x; elements[8] = z.x
        elements[1] = x.y; elements[5] = y.y; elements[9] = z.y
        elements[2] = x.z; elements[6] = y.z; elements[10] = z.z

        return this
    }

}