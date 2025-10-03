package io.kreekt.core.scene

import io.kreekt.core.math.*

/**
 * Extension functions for Object3D supporting classes (Vector3, Euler, Quaternion, Matrix4, Color)
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

// Note: Matrix4.compose() member function exists - this extension is shadowed and removed

/**
 * Extracts rotation from another matrix (removes scale)
 * This extension function does NOT exist as a member function in Matrix4
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
 * Converts Color to array with offset
 * Extension function for Color.toArray(array, offset) - the no-param version exists, but not this one
 */
fun Color.toArray(array: FloatArray, offset: Int = 0): FloatArray {
    array[offset] = r
    array[offset + 1] = g
    array[offset + 2] = b
    return array
}

// Note: Matrix4.toArray(array, offset) member function exists - this extension is shadowed and removed
