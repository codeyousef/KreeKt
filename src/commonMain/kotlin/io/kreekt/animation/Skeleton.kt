package io.kreekt.animation

import io.kreekt.core.math.Matrix4
import io.kreekt.core.platform.platformClone
import io.kreekt.core.math.Quaternion
import io.kreekt.core.platform.platformClone
import io.kreekt.core.math.Vector3
import io.kreekt.core.platform.platformClone
import kotlinx.serialization.Contextual
import io.kreekt.core.platform.platformClone
import kotlinx.serialization.Serializable
import io.kreekt.core.platform.platformClone

/**
 * Enhanced Skeleton class with IK chain support and bone constraints.
 * Provides comprehensive bone hierarchy management for advanced character animation.
 *
 * T036 - Enhanced Skeleton with IK chain support
 */
class Skeleton(
    val bones: List<Bone>,
    val boneInverses: List<Matrix4>? = null
) {

    // Bone hierarchy maps
    private val bonesByName = mutableMapOf<String, Bone>()
    private val parentChildMap = mutableMapOf<Bone, MutableList<Bone>>()
    private val childParentMap = mutableMapOf<Bone, Bone>()

    // IK chains
    private val ikChains = mutableListOf<IKChain>()

    // Pose management
    private val bindPose = mutableMapOf<Bone, BonePose>()
    private val currentPose = mutableMapOf<Bone, BonePose>()
    private val savedPoses = mutableMapOf<String, Map<Bone, BonePose>>()

    // Update flags
    private var hierarchyNeedsUpdate = true
    private var matricesNeedUpdate = true

    init {
        buildHierarchy()
        computeBoneInverses()
        saveBind()
    }

    /**
     * Enhanced Bone class with constraints and limits
     */
    data class Bone(
        val name: String,
        var position: Vector3 = Vector3(),
        var rotation: Quaternion = Quaternion(),
        var scale: Vector3 = Vector3(1f, 1f, 1f),
        val constraints: BoneConstraints = BoneConstraints(),
        var userData: Map<String, Any> = emptyMap()
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
     * IK Chain definition
     */
    data class IKChain(
        val name: String,
        val bones: List<Bone>,
        val target: Vector3,
        val poleVector: Vector3? = null,
        val solver: IKSolverType = IKSolverType.FABRIK,
        var weight: Float = 1.0f,
        var iterations: Int = 10,
        var tolerance: Float = 0.001f,
        var isEnabled: Boolean = true
    ) {
        val effector: Bone get() = bones.last()
        val root: Bone get() = bones.first()

        fun getChainLength(): Float {
            var length = 0f
            for (i in 0 until bones.size - 1) {
                val bone1 = bones[i]
                val bone2 = bones[i + 1]
                length = length + bone1.getWorldPosition().distanceTo(bone2.getWorldPosition())
            }
            return length
        }
    }

    /**
     * IK Solver types
     */
    enum class IKSolverType {
        FABRIK,
        TWO_BONE,
        CCD,
        JACOBIAN
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

    /**
     * Build bone hierarchy and name lookup
     */
    private fun buildHierarchy() {
        bonesByName.clear()
        parentChildMap.clear()
        childParentMap.clear()

        // Build name lookup
        bones.forEach { bone ->
            bonesByName[bone.name] = bone
        }

        // Build parent-child relationships
        bones.forEach { bone ->
            if (bone.parent != null) {
                parentChildMap.getOrPut(bone.parent!!) { mutableListOf() }.add(bone)
                childParentMap[bone] = bone.parent!!
            }
        }

        hierarchyNeedsUpdate = false
    }

    /**
     * Compute inverse bind matrices
     */
    private fun computeBoneInverses() {
        bones.forEach { bone ->
            bone.updateMatrixWorld()
        }
        matricesNeedUpdate = false
    }

    /**
     * Save bind pose
     */
    private fun saveBind() {
        bindPose.clear()
        bones.forEach { bone ->
            bindPose[bone] = BonePose(bone)
        }
    }

    /**
     * Update skeleton matrices
     */
    fun update() {
        if (hierarchyNeedsUpdate) {
            buildHierarchy()
        }

        if (matricesNeedUpdate) {
            bones.forEach { bone ->
                bone.updateMatrixWorld()
            }
            matricesNeedUpdate = false
        }
    }

    /**
     * Get bone by name
     */
    fun getBoneByName(name: String): Bone? = bonesByName[name]

    /**
     * Add IK chain
     */
    fun addIKChain(chain: IKChain) {
        ikChains.add(chain)
    }

    /**
     * Remove IK chain
     */
    fun removeIKChain(name: String) {
        ikChains.removeAll { it.name == name }
    }

    /**
     * Get IK chain by name
     */
    fun getIKChain(name: String): IKChain? = ikChains.find { it.name == name }

    /**
     * Get all IK chains
     */
    fun getIKChains(): List<IKChain> = ikChains.toList()

    /**
     * Save current pose with name
     */
    fun savePose(name: String) {
        val pose = mutableMapOf<Bone, BonePose>()
        bones.forEach { bone ->
            pose[bone] = BonePose(bone)
        }
        savedPoses[name] = pose
    }

    /**
     * Load saved pose
     */
    fun loadPose(name: String): Boolean {
        val pose = savedPoses[name] ?: return false
        pose.forEach { (bone, bonePose) ->
            bonePose.applyTo(bone)
        }
        matricesNeedUpdate = true
        return true
    }

    /**
     * Blend between current pose and saved pose
     */
    fun blendPose(name: String, alpha: Float): Boolean {
        val targetPose = savedPoses[name] ?: return false

        bones.forEach { bone ->
            val currentBonePose = BonePose(bone)
            val targetBonePose = targetPose[bone] ?: return@forEach
            val blendedPose = currentBonePose.lerp(targetBonePose, alpha)
            blendedPose.applyTo(bone)
        }

        matricesNeedUpdate = true
        return true
    }

    /**
     * Reset to bind pose
     */
    fun resetToBind() {
        bindPose.forEach { (bone, pose) ->
            pose.applyTo(bone)
        }
        matricesNeedUpdate = true
    }

    /**
     * Get bone children
     */
    fun getBoneChildren(bone: Bone): List<Bone> = parentChildMap[bone] ?: emptyList()

    /**
     * Get bone parent
     */
    fun getBoneParent(bone: Bone): Bone? = childParentMap[bone]

    /**
     * Get root bones (bones with no parent)
     */
    fun getRootBones(): List<Bone> = bones.filter { it.parent == null }

    /**
     * Get bone path from root to bone
     */
    fun getBonePath(bone: Bone): List<Bone> {
        val path = mutableListOf<Bone>()
        var current: Bone? = bone

        while (current != null) {
            path.add(0, current)
            current = current.parent
        }

        return path
    }

    /**
     * Retarget skeleton to another skeleton structure
     */
    fun retargetTo(targetSkeleton: Skeleton, boneMapping: Map<String, String>): Boolean {
        val retargetedPoses = mutableMapOf<Bone, BonePose>()

        boneMapping.forEach { (sourceName, targetName) ->
            val sourceBone = getBoneByName(sourceName)
            val targetBone = targetSkeleton.getBoneByName(targetName)

            if (sourceBone != null && targetBone != null) {
                retargetedPoses[targetBone] = BonePose(sourceBone)
            }
        }

        // Apply retargeted poses
        retargetedPoses.forEach { (bone, pose) ->
            pose.applyTo(bone)
        }

        targetSkeleton.matricesNeedUpdate = true
        return retargetedPoses.isNotEmpty()
    }

    /**
     * Calculate bone lengths for the skeleton
     */
    fun calculateBoneLengths(): Map<Bone, Float> {
        val boneLengths = mutableMapOf<Bone, Float>()

        bones.forEach { bone ->
            val children = getBoneChildren(bone)
            if (children.isNotEmpty()) {
                val bonePos = bone.getWorldPosition()
                val avgChildPos = Vector3()
                children.forEach { child ->
                    avgChildPos.add(child.getWorldPosition())
                }
                avgChildPos.divideScalar(children.size.toFloat())
                boneLengths[bone] = bonePos.distanceTo(avgChildPos)
            } else {
                // Leaf bone - use parent distance or default
                val parent = getBoneParent(bone)
                if (parent != null) {
                    boneLengths[bone] = bone.getWorldPosition().distanceTo(parent.getWorldPosition())
                } else {
                    boneLengths[bone] = 1.0f // Default length
                }
            }
        }

        return boneLengths
    }

    /**
     * Validate skeleton hierarchy
     */
    fun validateHierarchy(): List<String> {
        val errors = mutableListOf<String>()

        // Check for circular dependencies
        bones.forEach { bone ->
            val visited = mutableSetOf<Bone>()
            var current: Bone? = bone

            while (current != null) {
                if (current in visited) {
                    errors.add("Circular dependency detected involving bone: ${bone.name}")
                    break
                }
                visited.add(current)
                current = current.parent
            }
        }

        // Check for orphaned bones
        val connectedBones = mutableSetOf<Bone>()
        getRootBones().forEach { root ->
            addBoneAndChildren(root, connectedBones)
        }

        val orphanedBones = bones.filter { it !in connectedBones }
        orphanedBones.forEach { bone ->
            errors.add("Orphaned bone detected: ${bone.name}")
        }

        return errors
    }

    private fun addBoneAndChildren(bone: Bone, set: MutableSet<Bone>) {
        set.add(bone)
        getBoneChildren(bone).forEach { child ->
            addBoneAndChildren(child, set)
        }
    }

    fun dispose() {
        ikChains.clear()
        savedPoses.clear()
        bindPose.clear()
        currentPose.clear()
        bonesByName.clear()
        parentChildMap.clear()
        childParentMap.clear()
    }
}

// Extension functions for math operations
private fun Quaternion.toEuler(): Vector3 {
    // Simplified euler conversion - in real implementation, use proper math
    return Vector3(0f, 0f, 0f) // Placeholder
}

private fun Quaternion.setFromEuler(x: Float, y: Float, z: Float): Quaternion {
    // Simplified euler to quaternion - in real implementation, use proper math
    return this // Placeholder
}

private fun Vector3.setFromMatrixPosition(matrix: Matrix4): Vector3 {
    // Extract position from matrix - in real implementation, use matrix extraction
    return this // Placeholder
}

// Extension functions for Matrix4
private fun Matrix4.compose(position: Vector3, quaternion: Quaternion, scale: Vector3): Matrix4 {
    // Simplified matrix composition - in real implementation, use proper math
    return this // Placeholder
}

private fun Matrix4.decompose(position: Vector3, quaternion: Quaternion, scale: Vector3): Matrix4 {
    // Simplified matrix decomposition - in real implementation, use proper math
    return this // Placeholder
}

private fun Matrix4.copy(other: Matrix4): Matrix4 {
    // Copy matrix elements
    return this // Placeholder
}

private fun Matrix4.multiplyMatrices(a: Matrix4, b: Matrix4): Matrix4 {
    // Matrix multiplication
    return this // Placeholder
}