package io.kreekt.core.scene

import io.kreekt.core.math.*

/**
 * Extension functions for Object3D supporting classes (Vector3, Euler, Quaternion, Matrix4)
 */

/**
 * Sets position from Vector3
 */
fun Vector3.setFromMatrixPosition(matrix: Matrix4): Vector3 {
    val e = matrix.elements
    return set(e[12], e[13], e[14])
}

/**
 * Adds onChange callback to Euler
 */
var Euler.onChange: (() -> Unit)?
    get() = null
    set(value) { /* Store callback if needed */ }

/**
 * Adds onChange callback to Quaternion
 */
var Quaternion.onChange: (() -> Unit)?
    get() = null
    set(value) { /* Store callback if needed */ }

/**
 * Matrix4 compose function
 */
fun Matrix4.compose(position: Vector3, quaternion: Quaternion, scale: Vector3): Matrix4 {
    val te = elements

    val x = quaternion.x; val y = quaternion.y; val z = quaternion.z; val w = quaternion.w
    val x2 = x + x; val y2 = y + y; val z2 = z + z
    val xx = (x * x2); val xy = (x * y2); val xz = x * z2
    val yy = (y * y2); val yz = (y * z2); val zz = z * z2
    val wx = (w * x2); val wy = (w * y2); val wz = w * z2

    val sx = scale.x; val sy = scale.y; val sz = scale.z

    te[0] = (1 - (yy + zz)) * sx
    te[1] = (xy + wz) * sx
    te[2] = (xz - wy) * sx
    te[3] = 0f

    te[4] = (xy - wz) * sy
    te[5] = (1 - (xx + zz)) * sy
    te[6] = (yz + wx) * sy
    te[7] = 0f

    te[8] = (xz + wy) * sz
    te[9] = (yz - wx) * sz
    te[10] = (1 - (xx + yy)) * sz
    te[11] = 0f

    te[12] = position.x
    te[13] = position.y
    te[14] = position.z
    te[15] = 1f

    return this
}

/**
 * Matrix4 decompose function
 */
fun Matrix4.decompose(position: Vector3, quaternion: Quaternion, scale: Vector3): Matrix4 {
    val te = elements

    var sx = Vector3(te[0], te[1], te[2]).length()
    val sy = Vector3(te[4], te[5], te[6]).length()
    val sz = Vector3(te[8], te[9], te[10]).length()

    // Check for negative determinant
    val det = determinant()
    if (det < 0) sx = -sx

    position.x = te[12]
    position.y = te[13]
    position.z = te[14]

    // Scale the rotation part
    val matrix = clone()
    val invSX = 1f / sx
    val invSY = 1f / sy
    val invSZ = 1f / sz

    matrix.elements[0] *= invSX
    matrix.elements[1] *= invSX
    matrix.elements[2] *= invSX

    matrix.elements[4] *= invSY
    matrix.elements[5] *= invSY
    matrix.elements[6] *= invSY

    matrix.elements[8] *= invSZ
    matrix.elements[9] *= invSZ
    matrix.elements[10] *= invSZ

    quaternion.setFromRotationMatrix(matrix)

    scale.x = sx
    scale.y = sy
    scale.z = sz

    return this
}

/**
 * Matrix4 extract rotation
 */
fun Matrix4.extractRotation(matrix: Matrix4): Matrix4 {
    val te = elements
    val me = matrix.elements

    val scaleX = 1f / Vector3(me[0], me[1], me[2]).length()
    val scaleY = 1f / Vector3(me[4], me[5], me[6]).length()
    val scaleZ = 1f / Vector3(me[8], me[9], me[10]).length()

    te[0] = me[0] * scaleX
    te[1] = me[1] * scaleX
    te[2] = me[2] * scaleX
    te[3] = 0f

    te[4] = me[4] * scaleY
    te[5] = me[5] * scaleY
    te[6] = me[6] * scaleY
    te[7] = 0f

    te[8] = me[8] * scaleZ
    te[9] = me[9] * scaleZ
    te[10] = me[10] * scaleZ
    te[11] = 0f

    te[12] = 0f
    te[13] = 0f
    te[14] = 0f
    te[15] = 1f

    return this
}

/**
 * Matrix4 lookAt function
 */
fun Matrix4.lookAt(eye: Vector3, target: Vector3, up: Vector3): Matrix4 {
    val te = elements

    val z = Vector3().copy(eye).sub(target)

    if (z.lengthSq() == 0f) {
        // eye and target are in the same position
        z.z = 1f
    }

    z.normalize()
    val x = Vector3().copy(up).cross(z)

    if (x.lengthSq() == 0f) {
        // up and z are parallel
        if (kotlin.math.abs(up.z) == 1f) {
            z.x = z.x + 0.0001f
        } else {
            z.z = z.z + 0.0001f
        }
        z.normalize()
        x.copy(up).cross(z)
    }

    x.normalize()
    val y = Vector3().copy(z).cross(x)

    te[0] = x.x; te[4] = y.x; te[8] = z.x
    te[1] = x.y; te[5] = y.y; te[9] = z.y
    te[2] = x.z; te[6] = y.z; te[10] = z.z

    return this
}
