package io.kreekt.animation.skeleton

import io.kreekt.core.math.*
import io.kreekt.core.scene.compose

/**
 * Skeleton type definitions including bones, constraints, and poses.
 */

/**
 * Enhanced Bone class with constraints and limits
 */
data class Bone(
    val name: String,
    var position: Vector3 = Vector3(),
    var rotation: Quaternion = Quaternion(),
    var scale: Vector3 = Vector3(1f, 1f, 1f),
    val constraints: BoneConstraints = BoneConstraints(),
    var userData: Map<String, Any> = emptyMap(),
    val bindTransform: Matrix4 = Matrix4(),
    val inverseBindMatrix: Matrix4 = Matrix4(),
    val parentIndex: Int = -1  // -1 indicates root bone
) {
    // Local and world matrices
    val matrix = Matrix4()
    val matrixWorld = Matrix4()

    // Parent-child relationships
    var parent: Bone? = null
    val children = mutableListOf<Bone>()

    fun updateMatrix() {
        matrix.compose(position, rotation, scale)
    }

    fun updateMatrixWorld(force: Boolean = false) {
        updateMatrix()

        if (parent == null) {
            matrixWorld.copy(matrix)
        } else {
            matrixWorld.multiplyMatrices(parent!!.matrixWorld, matrix)
        }

        if (force) {
            children.forEach { it.updateMatrixWorld(true) }
        }
    }

    fun add(child: Bone) {
        if (child.parent != null) {
            child.parent!!.remove(child)
        }
        child.parent = this
        children.add(child)
    }

    fun remove(child: Bone) {
        val index = children.indexOf(child)
        if (index != -1) {
            child.parent = null
            children.removeAt(index)
        }
    }

    fun getWorldPosition(target: Vector3 = Vector3()): Vector3 {
        updateMatrixWorld()
        return target.setFromMatrixPosition(matrixWorld)
    }

    fun getWorldQuaternion(target: Quaternion = Quaternion()): Quaternion {
        updateMatrixWorld()
        matrixWorld.decompose(Vector3(), target, Vector3())
        return target
    }
}

/**
 * Bone constraints for limiting rotation and translation
 */
data class BoneConstraints(
    // Rotation limits (in radians)
    val minRotationX: Float = -Float.MAX_VALUE,
    val maxRotationX: Float = Float.MAX_VALUE,
    val minRotationY: Float = -Float.MAX_VALUE,
    val maxRotationY: Float = Float.MAX_VALUE,
    val minRotationZ: Float = -Float.MAX_VALUE,
    val maxRotationZ: Float = Float.MAX_VALUE,

    // Translation limits
    val minTranslationX: Float = -Float.MAX_VALUE,
    val maxTranslationX: Float = Float.MAX_VALUE,
    val minTranslationY: Float = -Float.MAX_VALUE,
    val maxTranslationY: Float = Float.MAX_VALUE,
    val minTranslationZ: Float = -Float.MAX_VALUE,
    val maxTranslationZ: Float = Float.MAX_VALUE,

    // Constraint flags
    val lockRotationX: Boolean = false,
    val lockRotationY: Boolean = false,
    val lockRotationZ: Boolean = false,
    val lockTranslationX: Boolean = false,
    val lockTranslationY: Boolean = false,
    val lockTranslationZ: Boolean = false,

    // IK specific constraints
    val ikEnabled: Boolean = true,
    val twistAxis: Vector3? = null,
    val preferredAngle: Float = 0f
) {
    fun applyRotationConstraints(rotation: Quaternion): Quaternion {
        val euler = rotation.toEuler()

        // Apply constraints
        val constrainedX = if (lockRotationX) 0f else
            euler.x.coerceIn(minRotationX, maxRotationX)
        val constrainedY = if (lockRotationY) 0f else
            euler.y.coerceIn(minRotationY, maxRotationY)
        val constrainedZ = if (lockRotationZ) 0f else
            euler.z.coerceIn(minRotationZ, maxRotationZ)

        return Quaternion().setFromEuler(constrainedX, constrainedY, constrainedZ)
    }

    fun applyTranslationConstraints(position: Vector3): Vector3 {
        return Vector3(
            if (lockTranslationX) 0f else position.x.coerceIn(minTranslationX, maxTranslationX),
            if (lockTranslationY) 0f else position.y.coerceIn(minTranslationY, maxTranslationY),
            if (lockTranslationZ) 0f else position.z.coerceIn(minTranslationZ, maxTranslationZ)
        )
    }
}

/**
 * Bone pose for saving/loading/blending
 */
data class BonePose(
    val position: Vector3,
    val rotation: Quaternion,
    val scale: Vector3
) {
    constructor(bone: Bone) : this(
        bone.position.clone(),
        bone.rotation.clone(),
        bone.scale.clone()
    )

    fun applyTo(bone: Bone) {
        bone.position.copy(position)
        bone.rotation.copy(rotation)
        bone.scale.copy(scale)
    }

    fun clone(): BonePose = BonePose(position.clone(), rotation.clone(), scale.clone())

    fun lerp(other: BonePose, alpha: Float): BonePose {
        return BonePose(
            position.clone().lerp(other.position, alpha),
            rotation.clone().slerp(other.rotation, alpha),
            scale.clone().lerp(other.scale, alpha)
        )
    }
}

// Extension functions for math operations
fun Quaternion.toEuler(): Vector3 {
    val euler = Euler().setFromQuaternion(this)
    return Vector3(euler.x, euler.y, euler.z)
}

fun Quaternion.setFromEuler(x: Float, y: Float, z: Float): Quaternion {
    val euler = Euler(x, y, z)
    return this.setFromEuler(euler)
}

fun Vector3.setFromMatrixPosition(matrix: Matrix4): Vector3 {
    val e = matrix.elements
    this.x = e[12]
    this.y = e[13]
    this.z = e[14]
    return this
}

fun Matrix4.decompose(position: Vector3, quaternion: Quaternion, scale: Vector3): Matrix4 {
    val te = elements

    // Extract scale
    var sx = Vector3(te[0], te[1], te[2]).length()
    val sy = Vector3(te[4], te[5], te[6]).length()
    val sz = Vector3(te[8], te[9], te[10]).length()

    // Check for negative determinant
    val det = determinant()
    if (det < 0) sx = -sx

    // Extract position
    position.x = te[12]
    position.y = te[13]
    position.z = te[14]

    // Scale the rotation part
    val matrix = this.clone()
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

    // Extract rotation as quaternion
    quaternion.setFromRotationMatrix(matrix)

    // Set scale
    scale.x = sx
    scale.y = sy
    scale.z = sz

    return this
}

fun Matrix4.copy(other: Matrix4): Matrix4 {
    other.elements.copyInto(this.elements)
    return this
}

fun Matrix4.multiplyMatrices(a: Matrix4, b: Matrix4): Matrix4 {
    return this.multiplyMatrices(a, b)
}
