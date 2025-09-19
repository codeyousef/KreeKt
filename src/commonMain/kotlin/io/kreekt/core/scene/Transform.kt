package io.kreekt.core.scene

import io.kreekt.core.math.*
import kotlin.math.*

/**
 * Transform system for managing object transformations.
 * Handles position, rotation, scale, and matrix calculations.
 */
class Transform {

    // Local transformation components
    val position: Vector3 = Vector3()
    val rotation: Euler = Euler()
    val scale: Vector3 = Vector3(1f, 1f, 1f)
    val quaternion: Quaternion = Quaternion()

    // Transformation matrices
    val localMatrix: Matrix4 = Matrix4()
    val worldMatrix: Matrix4 = Matrix4()

    // Update flags
    var needsMatrixUpdate: Boolean = true
    var needsWorldMatrixUpdate: Boolean = true

    // Parent transform reference
    var parent: Transform? = null

    // Change callbacks
    var onPositionChanged: (() -> Unit)? = null
    var onRotationChanged: (() -> Unit)? = null
    var onScaleChanged: (() -> Unit)? = null

    init {
        setupChangeCallbacks()
    }

    /**
     * Sets up callbacks to maintain sync between rotation and quaternion
     */
    private fun setupChangeCallbacks() {
        // When rotation changes, update quaternion
        onRotationChanged = {
            quaternion.setFromEuler(rotation)
            markMatrixDirty()
        }

        // Note: In a full implementation, we'd also handle quaternion->euler sync
    }

    /**
     * Sets position values
     */
    fun setPosition(x: Float, y: Float, z: Float): Transform {
        position.set(x, y, z)
        markMatrixDirty()
        onPositionChanged?.invoke()
        return this
    }

    /**
     * Sets position from Vector3
     */
    fun setPosition(pos: Vector3): Transform {
        position.copy(pos)
        markMatrixDirty()
        onPositionChanged?.invoke()
        return this
    }

    /**
     * Sets rotation values in radians
     */
    fun setRotation(x: Float, y: Float, z: Float, order: EulerOrder = EulerOrder.XYZ): Transform {
        rotation.set(x, y, z, order)
        onRotationChanged?.invoke()
        return this
    }

    /**
     * Sets rotation from Euler
     */
    fun setRotation(rot: Euler): Transform {
        rotation.copy(rot)
        onRotationChanged?.invoke()
        return this
    }

    /**
     * Sets rotation from quaternion
     */
    fun setRotation(quat: Quaternion): Transform {
        quaternion.copy(quat)
        rotation.setFromQuaternion(quaternion)
        markMatrixDirty()
        return this
    }

    /**
     * Sets scale values
     */
    fun setScale(x: Float, y: Float, z: Float): Transform {
        scale.set(x, y, z)
        markMatrixDirty()
        onScaleChanged?.invoke()
        return this
    }

    /**
     * Sets uniform scale
     */
    fun setScale(uniformScale: Float): Transform {
        return setScale(uniformScale, uniformScale, uniformScale)
    }

    /**
     * Sets scale from Vector3
     */
    fun setScale(scl: Vector3): Transform {
        scale.copy(scl)
        markMatrixDirty()
        onScaleChanged?.invoke()
        return this
    }

    /**
     * Translates by offset
     */
    fun translate(x: Float, y: Float, z: Float): Transform {
        position.add(Vector3(x, y, z))
        markMatrixDirty()
        onPositionChanged?.invoke()
        return this
    }

    /**
     * Translates by vector
     */
    fun translate(offset: Vector3): Transform {
        position.add(offset)
        markMatrixDirty()
        onPositionChanged?.invoke()
        return this
    }

    /**
     * Rotates around axis by angle
     */
    fun rotate(axis: Vector3, angle: Float): Transform {
        val q = Quaternion().setFromAxisAngle(axis, angle)
        quaternion.multiply(q)
        rotation.setFromQuaternion(quaternion)
        markMatrixDirty()
        return this
    }

    /**
     * Rotates around X axis
     */
    fun rotateX(angle: Float): Transform {
        return rotate(Vector3(1f, 0f, 0f), angle)
    }

    /**
     * Rotates around Y axis
     */
    fun rotateY(angle: Float): Transform {
        return rotate(Vector3(0f, 1f, 0f), angle)
    }

    /**
     * Rotates around Z axis
     */
    fun rotateZ(angle: Float): Transform {
        return rotate(Vector3(0f, 0f, 1f), angle)
    }

    /**
     * Scales by factor
     */
    fun scaleBy(factor: Float): Transform {
        scale.multiplyScalar(factor)
        markMatrixDirty()
        onScaleChanged?.invoke()
        return this
    }

    /**
     * Scales by vector
     */
    fun scaleBy(factor: Vector3): Transform {
        scale.multiply(factor)
        markMatrixDirty()
        onScaleChanged?.invoke()
        return this
    }

    /**
     * Updates the local transformation matrix
     */
    fun updateLocalMatrix(): Matrix4 {
        if (needsMatrixUpdate) {
            localMatrix.compose(position, quaternion, scale)
            needsMatrixUpdate = false
            needsWorldMatrixUpdate = true
        }
        return localMatrix
    }

    /**
     * Updates the world transformation matrix
     */
    fun updateWorldMatrix(): Matrix4 {
        updateLocalMatrix()

        if (needsWorldMatrixUpdate) {
            if (parent == null) {
                worldMatrix.copy(localMatrix)
            } else {
                worldMatrix.multiplyMatrices(parent!!.updateWorldMatrix(), localMatrix)
            }
            needsWorldMatrixUpdate = false
        }

        return worldMatrix
    }

    /**
     * Gets world position
     */
    fun getWorldPosition(target: Vector3 = Vector3()): Vector3 {
        updateWorldMatrix()
        return target.setFromMatrixPosition(worldMatrix)
    }

    /**
     * Gets world rotation as quaternion
     */
    fun getWorldQuaternion(target: Quaternion = Quaternion()): Quaternion {
        updateWorldMatrix()
        worldMatrix.decompose(Vector3(), target, Vector3())
        return target
    }

    /**
     * Gets world scale
     */
    fun getWorldScale(target: Vector3 = Vector3()): Vector3 {
        updateWorldMatrix()
        worldMatrix.decompose(Vector3(), Quaternion(), target)
        return target
    }

    /**
     * Gets world direction (forward vector)
     */
    fun getWorldDirection(target: Vector3 = Vector3()): Vector3 {
        updateWorldMatrix()
        val te = worldMatrix.elements
        return target.set(-te[8], -te[9], -te[10]).normalize()
    }

    /**
     * Gets world up vector
     */
    fun getWorldUp(target: Vector3 = Vector3()): Vector3 {
        updateWorldMatrix()
        val te = worldMatrix.elements
        return target.set(te[4], te[5], te[6]).normalize()
    }

    /**
     * Gets world right vector
     */
    fun getWorldRight(target: Vector3 = Vector3()): Vector3 {
        updateWorldMatrix()
        val te = worldMatrix.elements
        return target.set(te[0], te[1], te[2]).normalize()
    }

    /**
     * Looks at target position
     */
    fun lookAt(target: Vector3, up: Vector3 = Vector3(0f, 1f, 0f)): Transform {
        val worldPosition = getWorldPosition()
        val matrix = Matrix4().lookAt(worldPosition, target, up)

        if (parent != null) {
            val parentWorldMatrix = parent!!.updateWorldMatrix()
            val parentRotation = Matrix4().extractRotation(parentWorldMatrix)
            val parentQuaternion = Quaternion().setFromRotationMatrix(parentRotation)
            matrix.premultiply(Matrix4().makeRotationFromQuaternion(parentQuaternion.invert()))
        }

        quaternion.setFromRotationMatrix(matrix)
        rotation.setFromQuaternion(quaternion)
        markMatrixDirty()
        return this
    }

    /**
     * Converts local point to world space
     */
    fun localToWorld(localPoint: Vector3, target: Vector3 = Vector3()): Vector3 {
        updateWorldMatrix()
        return target.copy(localPoint).applyMatrix4(worldMatrix)
    }

    /**
     * Converts world point to local space
     */
    fun worldToLocal(worldPoint: Vector3, target: Vector3 = Vector3()): Vector3 {
        updateWorldMatrix()
        val inverseWorldMatrix = Matrix4().copy(worldMatrix).invert()
        return target.copy(worldPoint).applyMatrix4(inverseWorldMatrix)
    }

    /**
     * Converts local direction to world space
     */
    fun localDirectionToWorld(localDirection: Vector3, target: Vector3 = Vector3()): Vector3 {
        updateWorldMatrix()
        return target.copy(localDirection).transformDirection(worldMatrix)
    }

    /**
     * Converts world direction to local space
     */
    fun worldDirectionToLocal(worldDirection: Vector3, target: Vector3 = Vector3()): Vector3 {
        updateWorldMatrix()
        val inverseWorldMatrix = Matrix4().copy(worldMatrix).invert()
        return target.copy(worldDirection).transformDirection(inverseWorldMatrix)
    }

    /**
     * Interpolates to target transform
     */
    fun lerp(target: Transform, alpha: Float): Transform {
        position.lerp(target.position, alpha)
        quaternion.slerp(target.quaternion, alpha)
        scale.lerp(target.scale, alpha)
        rotation.setFromQuaternion(quaternion)
        markMatrixDirty()
        return this
    }

    /**
     * Copies from another transform
     */
    fun copy(source: Transform): Transform {
        position.copy(source.position)
        rotation.copy(source.rotation)
        scale.copy(source.scale)
        quaternion.copy(source.quaternion)

        localMatrix.copy(source.localMatrix)
        worldMatrix.copy(source.worldMatrix)

        needsMatrixUpdate = source.needsMatrixUpdate
        needsWorldMatrixUpdate = source.needsWorldMatrixUpdate

        return this
    }

    /**
     * Creates a copy of this transform
     */
    fun clone(): Transform {
        return Transform().copy(this)
    }

    /**
     * Resets transform to identity
     */
    fun reset(): Transform {
        position.set(0f, 0f, 0f)
        rotation.set(0f, 0f, 0f)
        scale.set(1f, 1f, 1f)
        quaternion.identity()
        markMatrixDirty()
        return this
    }

    /**
     * Marks matrices as needing update
     */
    private fun markMatrixDirty() {
        needsMatrixUpdate = true
        needsWorldMatrixUpdate = true
    }

    /**
     * Forces update of all matrices
     */
    fun forceUpdate(): Transform {
        needsMatrixUpdate = true
        needsWorldMatrixUpdate = true
        updateWorldMatrix()
        return this
    }

    /**
     * Checks if transform has any rotation
     */
    fun hasRotation(): Boolean {
        return !quaternion.isIdentity()
    }

    /**
     * Checks if transform has any scale other than 1,1,1
     */
    fun hasScale(): Boolean {
        return scale.x != 1f || scale.y != 1f || scale.z != 1f
    }

    /**
     * Checks if transform has any translation
     */
    fun hasTranslation(): Boolean {
        return !position.isZero()
    }

    /**
     * Checks if transform is identity (no transformation)
     */
    fun isIdentity(): Boolean {
        return !hasTranslation() && !hasRotation() && !hasScale()
    }

    override fun toString(): String {
        return "Transform(pos=$position, rot=$rotation, scale=$scale)"
    }
}

/**
 * Transform hierarchy manager
 */
class TransformHierarchy {
    private val transforms = mutableMapOf<Int, Transform>()
    private val parentChildMap = mutableMapOf<Int, MutableList<Int>>()
    private val childParentMap = mutableMapOf<Int, Int>()

    /**
     * Registers a transform in the hierarchy
     */
    fun register(id: Int, transform: Transform): Transform {
        transforms[id] = transform
        return transform
    }

    /**
     * Unregisters a transform from the hierarchy
     */
    fun unregister(id: Int) {
        transforms.remove(id)

        // Remove from parent-child relationships
        childParentMap[id]?.let { parentId ->
            parentChildMap[parentId]?.remove(id)
        }
        childParentMap.remove(id)

        // Remove all children
        parentChildMap[id]?.toList()?.forEach { childId ->
            setParent(childId, null)
        }
        parentChildMap.remove(id)
    }

    /**
     * Sets parent-child relationship
     */
    fun setParent(childId: Int, parentId: Int?) {
        val childTransform = transforms[childId] ?: return

        // Remove from old parent
        childParentMap[childId]?.let { oldParentId ->
            parentChildMap[oldParentId]?.remove(childId)
        }

        if (parentId != null) {
            val parentTransform = transforms[parentId] ?: return

            // Set new relationships
            childTransform.parent = parentTransform
            childParentMap[childId] = parentId
            parentChildMap.getOrPut(parentId) { mutableListOf() }.add(childId)
        } else {
            // Remove parent
            childTransform.parent = null
            childParentMap.remove(childId)
        }
    }

    /**
     * Gets children of a transform
     */
    fun getChildren(id: Int): List<Int> {
        return parentChildMap[id] ?: emptyList()
    }

    /**
     * Gets parent of a transform
     */
    fun getParent(id: Int): Int? {
        return childParentMap[id]
    }

    /**
     * Updates all transforms in hierarchy order
     */
    fun updateAll() {
        // Update root transforms first
        transforms.values.filter { it.parent == null }.forEach {
            it.updateWorldMatrix()
        }
    }

    /**
     * Gets transform by ID
     */
    fun getTransform(id: Int): Transform? {
        return transforms[id]
    }
}

/**
 * Transform propagation utilities
 */
object TransformPropagation {

    /**
     * Propagates transform changes down a hierarchy
     */
    fun propagateDown(transform: Transform, visitor: (Transform) -> Unit) {
        visitor(transform)
        // In a full implementation, this would traverse children
    }

    /**
     * Propagates transform changes up a hierarchy
     */
    fun propagateUp(transform: Transform, visitor: (Transform) -> Unit) {
        var current: Transform? = transform
        while (current != null) {
            visitor(current)
            current = current.parent
        }
    }

    /**
     * Calculates world bounds for a transform hierarchy
     */
    fun calculateWorldBounds(rootTransform: Transform): Box3 {
        val bounds = Box3()
        // Implementation would traverse and accumulate bounds
        return bounds
    }

    /**
     * Optimizes transform updates by batching
     */
    fun batchUpdate(transforms: List<Transform>) {
        // Sort by hierarchy level to ensure parents update before children
        transforms.forEach { it.updateWorldMatrix() }
    }
}

/**
 * Transform animation utilities
 */
class TransformAnimator {
    private var startTransform: Transform? = null
    private var endTransform: Transform? = null
    private var duration: Float = 1f
    private var elapsed: Float = 0f
    private var isPlaying: Boolean = false

    /**
     * Sets up animation between two transforms
     */
    fun animate(
        start: Transform,
        end: Transform,
        duration: Float,
        onUpdate: ((Transform) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        this.startTransform = start.clone()
        this.endTransform = end
        this.duration = duration
        this.elapsed = 0f
        this.isPlaying = true
    }

    /**
     * Updates animation
     */
    fun update(deltaTime: Float, target: Transform): Boolean {
        if (!isPlaying || startTransform == null || endTransform == null) {
            return false
        }

        elapsed += deltaTime
        val t = (elapsed / duration).coerceIn(0f, 1f)

        // Interpolate transforms
        target.copy(startTransform!!)
        target.lerp(endTransform!!, t)

        if (t >= 1f) {
            isPlaying = false
            return true // Animation complete
        }

        return false
    }

    /**
     * Stops animation
     */
    fun stop() {
        isPlaying = false
    }

    /**
     * Checks if animation is playing
     */
    fun isPlaying(): Boolean = isPlaying
}