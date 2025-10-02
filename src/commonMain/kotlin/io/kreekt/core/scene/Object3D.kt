package io.kreekt.core.scene

import io.kreekt.core.math.*

/**
 * Base class for all objects in the 3D scene.
 * Compatible with Three.js Object3D API.
 *
 * Object3D provides the foundation for all 3D objects (including meshes), lights, cameras, and groups.
 * It manages transformation, hierarchy, and common properties.
 *
 * Implementation is split across multiple files:
 * - Object3DCore.kt: Core functionality, layers, events
 * - Object3DHierarchy.kt: Hierarchy management (add, remove, traverse)
 * - Object3DTransform.kt: Transformation operations (rotate, translate, matrix)
 * - Object3DExtensions.kt: Extension functions for supporting classes
 */
abstract class Object3D {

    // Unique identifier
    val id: Int = Object3DIdGenerator.generateId()

    // Object name for debugging and identification
    var name: String = ""

    // Transformation properties
    val position: Vector3 = Vector3()
    val rotation: Euler = Euler()
    val scale: Vector3 = Vector3(1f, 1f, 1f)
    val quaternion: Quaternion = Quaternion()

    // Transformation matrices
    val matrix: Matrix4 = Matrix4()
    val matrixWorld: Matrix4 = Matrix4()

    // Auto-update behavior
    var matrixAutoUpdate: Boolean = true
    var matrixWorldNeedsUpdate: Boolean = false

    // Visibility and shadow properties
    var visible: Boolean = true
    var castShadow: Boolean = false
    var receiveShadow: Boolean = false

    // Hierarchy
    var parent: Object3D? = null
        internal set
    internal val _children: MutableList<Object3D> = mutableListOf()
    val children: List<Object3D> get() = _children

    // Layers for selective rendering/raycasting
    val layers: Layers = Layers()

    // Custom user data
    val userData: MutableMap<String, Any> = mutableMapOf()

    // Event callbacks
    var onBeforeRender: ((Object3D) -> Unit)? = null
    var onAfterRender: ((Object3D) -> Unit)? = null

    // Object type for identification
    open val type: String get() = "Object3D"

    init {
        // Ensure rotation and quaternion stay in sync
        rotation.onChange = { updateQuaternionFromEuler() }
        quaternion.onChange = { updateEulerFromQuaternion() }
    }

    // Hierarchy operations (delegated to Object3DHierarchy.kt)
    fun add(vararg objects: Object3D): Object3D = addChildren(*objects)
    fun remove(vararg objects: Object3D): Object3D = removeChildren(*objects)
    fun removeFromParent(): Object3D = detachFromParent()
    fun clear(): Object3D = clearChildren()
    fun attach(object3d: Object3D): Object3D = attachChild(object3d)
    fun getObjectByName(name: String): Object3D? = findObjectByName(name)
    fun getObjectById(id: Int): Object3D? = findObjectById(id)
    fun getObjectByProperty(name: String, value: Any): Object3D? = findObjectByProperty(name, value)
    fun traverse(callback: (Object3D) -> Unit) = traverseAll(callback)
    fun traverseVisible(callback: (Object3D) -> Unit) = traverseOnlyVisible(callback)
    fun traverseAncestors(callback: (Object3D) -> Unit) = traverseParents(callback)

    // Transformation operations (delegated to Object3DTransform.kt)
    fun getWorldPosition(target: Vector3 = Vector3()): Vector3 = extractWorldPosition(target)
    fun getWorldQuaternion(target: Quaternion = Quaternion()): Quaternion = extractWorldQuaternion(target)
    fun getWorldScale(target: Vector3 = Vector3()): Vector3 = extractWorldScale(target)
    open fun getWorldDirection(target: Vector3 = Vector3()): Vector3 = extractWorldDirection(target)
    fun lookAt(target: Vector3) = setLookAt(target)
    fun lookAt(x: Float, y: Float, z: Float) = setLookAt(x, y, z)
    fun rotateOnAxis(axis: Vector3, angle: Float): Object3D = applyRotationOnAxis(axis, angle)
    fun rotateOnWorldAxis(axis: Vector3, angle: Float): Object3D = applyRotationOnWorldAxis(axis, angle)
    fun rotateX(angle: Float): Object3D = applyRotationX(angle)
    fun rotateY(angle: Float): Object3D = applyRotationY(angle)
    fun rotateZ(angle: Float): Object3D = applyRotationZ(angle)
    fun translateOnAxis(axis: Vector3, distance: Float): Object3D = applyTranslationOnAxis(axis, distance)
    fun translateX(distance: Float): Object3D = applyTranslationX(distance)
    fun translateY(distance: Float): Object3D = applyTranslationY(distance)
    fun translateZ(distance: Float): Object3D = applyTranslationZ(distance)
    fun localToWorld(vector: Vector3): Vector3 = convertLocalToWorld(vector)
    fun worldToLocal(vector: Vector3): Vector3 = convertWorldToLocal(vector)
    fun applyMatrix4(matrix: Matrix4): Object3D = applyMatrixTransform(matrix)
    fun updateMatrix() = updateLocalMatrix()
    fun updateWorldMatrix(updateParents: Boolean = false, updateChildren: Boolean = false) =
        updateWorldMatrixWithOptions(updateParents, updateChildren)

    /**
     * Gets the bounding box of this object
     */
    open fun getBoundingBox(): Box3 {
        // Default implementation returns empty box
        return Box3()
    }

    /**
     * Updates the world transformation matrix
     */
    open fun updateMatrixWorld(force: Boolean = false) {
        if (matrixAutoUpdate) updateMatrix()

        var forceChildren = force
        if (matrixWorldNeedsUpdate || force) {
            if (parent == null) {
                matrixWorld.copy(matrix)
            } else {
                matrixWorld.multiplyMatrices(parent!!.matrixWorld, matrix)
            }
            matrixWorldNeedsUpdate = false
            forceChildren = true
        }

        // Update children
        for (child in children) {
            child.updateMatrixWorld(forceChildren)
        }
    }

    /**
     * Creates a copy of this object
     */
    open fun clone(recursive: Boolean = true): Object3D {
        throw NotImplementedError("Clone must be implemented by subclass")
    }

    /**
     * Copies properties from another object
     */
    open fun copy(source: Object3D, recursive: Boolean = true): Object3D {
        name = source.name

        position.copy(source.position)
        rotation.copy(source.rotation)
        scale.copy(source.scale)
        quaternion.copy(source.quaternion)

        matrix.copy(source.matrix)
        matrixWorld.copy(source.matrixWorld)

        matrixAutoUpdate = source.matrixAutoUpdate
        matrixWorldNeedsUpdate = source.matrixWorldNeedsUpdate

        visible = source.visible
        castShadow = source.castShadow
        receiveShadow = source.receiveShadow

        layers.mask = source.layers.mask

        userData.clear()
        userData.putAll(source.userData)

        if (recursive) {
            for (child in source.children) {
                add(child.clone(true))
            }
        }

        return this
    }

    internal fun dispatchEvent(event: Event) {
        // Event system placeholder - could be expanded
    }

    private fun updateQuaternionFromEuler() {
        quaternion.setFromEuler(rotation)
    }

    private fun updateEulerFromQuaternion() {
        rotation.setFromQuaternion(quaternion)
    }

    override fun toString(): String {
        return "${this::class.simpleName}(id=$id, name='$name')"
    }
}
