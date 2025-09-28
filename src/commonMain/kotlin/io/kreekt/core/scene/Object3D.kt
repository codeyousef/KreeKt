package io.kreekt.core.scene

import io.kreekt.core.math.*

/**
 * Base class for all objects in the 3D scene.
 * Compatible with Three.js Object3D API.
 *
 * Object3D provides the foundation for all 3D objects (including * meshes), lights, cameras, and groups. It manages transformation,
 * hierarchy, and common properties.
 */
abstract class Object3D {

    companion object {
        private var nextId = 1
        private fun generateId(): Int = nextId++
    }

    // Unique identifier
    val id: Int = generateId()

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
        private set
    private val _children: MutableList<Object3D> = mutableListOf()
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

    /**
     * Adds a child object to this object
     */
    fun add(vararg objects: Object3D): Object3D {
        for (obj in objects) {
            if (obj === this) {
                throw IllegalArgumentException("Object cannot be added as a child of itself")
            }

            if (obj.parent !== null) {
                obj.parent?.remove(obj)
            }

            obj.parent = this
            _children.add(obj)
            obj.dispatchEvent(Event.Added(obj))
        }
        return this
    }

    /**
     * Removes a child object from this object
     */
    fun remove(vararg objects: Object3D): Object3D {
        for (obj in objects) {
            val index = _children.indexOf(obj)
            if (index >= 0) {
                _children.removeAt(index)
                obj.parent = null
                obj.dispatchEvent(Event.Removed(obj))
            }
        }
        return this
    }

    /**
     * Removes this object from its parent
     */
    fun removeFromParent(): Object3D {
        parent?.remove(this)
        return this
    }

    /**
     * Removes all children from this object
     */
    fun clear(): Object3D {
        val childrenCopy = _children.toList()
        _children.clear()
        for (child in childrenCopy) {
            child.parent = null
            child.dispatchEvent(Event.Removed(child))
        }
        return this
    }

    /**
     * Attaches this object to another object
     */
    fun attach(object3d: Object3D): Object3D {
        // Calculate world position and apply inverse transform
        updateMatrixWorld()
        object3d.updateMatrixWorld()

        val targetMatrix = Matrix4().copy(matrixWorld).invert()
        object3d.matrix.premultiply(targetMatrix)
        object3d.matrix.decompose(object3d.position, object3d.quaternion, object3d.scale)

        add(object3d)
        return this
    }

    /**
     * Gets a child by name
     */
    fun getObjectByName(name: String): Object3D? {
        if (this.name == name) return this

        for (child in children) {
            val result = child.getObjectByName(name)
            if (result != null) return result
        }

        return null
    }

    /**
     * Gets a child by ID
     */
    fun getObjectById(id: Int): Object3D? {
        if (this.id == id) return this

        for (child in children) {
            val result = child.getObjectById(id)
            if (result != null) return result
        }

        return null
    }

    /**
     * Gets a child by property
     */
    fun getObjectByProperty(name: String, value: Any): Object3D? {
        when (name) {
            "id" -> if (id == value) return this
            "name" -> if (this.name == value) return this
            "visible" -> if (visible == value) return this
            "castShadow" -> if (castShadow == value) return this
            "receiveShadow" -> if (receiveShadow == value) return this
        }

        for (child in children) {
            val result = child.getObjectByProperty(name, value)
            if (result != null) return result
        }

        return null
    }

    /**
     * Gets the world position of this object
     */
    fun getWorldPosition(target: Vector3 = Vector3()): Vector3 {
        updateMatrixWorld()
        return target.setFromMatrixPosition(matrixWorld)
    }

    /**
     * Gets the world quaternion of this object
     */
    fun getWorldQuaternion(target: Quaternion = Quaternion()): Quaternion {
        updateMatrixWorld()
        matrixWorld.decompose(Vector3(), target, Vector3())
        return target
    }

    /**
     * Gets the world scale of this object
     */
    fun getWorldScale(target: Vector3 = Vector3()): Vector3 {
        updateMatrixWorld()
        matrixWorld.decompose(Vector3(), Quaternion(), target)
        return target
    }

    /**
     * Gets the world direction vector for this object
     */
    open fun getWorldDirection(target: Vector3 = Vector3()): Vector3 {
        updateMatrixWorld()
        val te = matrixWorld.elements
        return target.set(-te[8], -te[9], -te[10]).normalize()
    }

    /**
     * Rotates this object to face a target position
     */
    fun lookAt(target: Vector3) {
        lookAt(target.x, target.y, target.z)
    }

    /**
     * Rotates this object to face a target position
     */
    fun lookAt(x: Float, y: Float, z: Float) {
        val target = Vector3(x, y, z)
        val position = getWorldPosition()

        val m1 = Matrix4()
        m1.lookAt(position, target, Vector3(0f, 1f, 0f))
        quaternion.setFromRotationMatrix(m1)

        if (parent != null) {
            m1.extractRotation(parent!!.matrixWorld)
            val q1 = Quaternion().setFromRotationMatrix(m1)
            quaternion.premultiply(q1.invert())
        }
    }

    /**
     * Rotates around an axis by an angle
     */
    fun rotateOnAxis(axis: Vector3, angle: Float): Object3D {
        val q1 = Quaternion().setFromAxisAngle(axis, angle)
        quaternion.multiply(q1)
        return this
    }

    /**
     * Rotates around world X axis
     */
    fun rotateOnWorldAxis(axis: Vector3, angle: Float): Object3D {
        val q1 = Quaternion().setFromAxisAngle(axis, angle)
        quaternion.premultiply(q1)
        return this
    }

    /**
     * Rotates around X axis
     */
    fun rotateX(angle: Float): Object3D {
        return rotateOnAxis(Vector3(1f, 0f, 0f), angle)
    }

    /**
     * Rotates around Y axis
     */
    fun rotateY(angle: Float): Object3D {
        return rotateOnAxis(Vector3(0f, 1f, 0f), angle)
    }

    /**
     * Rotates around Z axis
     */
    fun rotateZ(angle: Float): Object3D {
        return rotateOnAxis(Vector3(0f, 0f, 1f), angle)
    }

    /**
     * Translates along an axis
     */
    fun translateOnAxis(axis: Vector3, distance: Float): Object3D {
        val v1 = Vector3().copy(axis).applyQuaternion(quaternion)
        position.add(v1.multiplyScalar(distance))
        return this
    }

    /**
     * Translates along X axis
     */
    fun translateX(distance: Float): Object3D {
        return translateOnAxis(Vector3(1f, 0f, 0f), distance)
    }

    /**
     * Translates along Y axis
     */
    fun translateY(distance: Float): Object3D {
        return translateOnAxis(Vector3(0f, 1f, 0f), distance)
    }

    /**
     * Translates along Z axis
     */
    fun translateZ(distance: Float): Object3D {
        return translateOnAxis(Vector3(0f, 0f, 1f), distance)
    }

    /**
     * Converts local coordinates to world coordinates
     */
    fun localToWorld(vector: Vector3): Vector3 {
        return vector.applyMatrix4(matrixWorld)
    }

    /**
     * Converts world coordinates to local coordinates
     */
    fun worldToLocal(vector: Vector3): Vector3 {
        return vector.applyMatrix4(Matrix4().copy(matrixWorld).invert())
    }

    /**
     * Gets the bounding box of this object
     */
    open fun getBoundingBox(): Box3 {
        // Default implementation returns empty box
        return Box3()
    }

    /**
     * Applies a matrix transformation to this object
     */
    fun applyMatrix4(matrix: Matrix4): Object3D {
        if (matrixAutoUpdate) updateMatrix()
        this.matrix.premultiply(matrix)
        this.matrix.decompose(position, quaternion, scale)
        return this
    }

    /**
     * Updates the local transformation matrix
     */
    fun updateMatrix() {
        matrix.compose(position, quaternion, scale)
        matrixWorldNeedsUpdate = true
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
     * Updates the world matrix and all children
     */
    fun updateWorldMatrix(updateParents: Boolean = false, updateChildren: Boolean = false) {
        var parent = this.parent

        if (updateParents && parent != null) {
            parent.updateWorldMatrix(true, false)
        }

        if (matrixAutoUpdate) updateMatrix()

        if (this.parent == null) {
            matrixWorld.copy(matrix)
        } else {
            matrixWorld.multiplyMatrices(this.parent!!.matrixWorld, matrix)
        }

        if (updateChildren) {
            for (child in children) {
                child.updateWorldMatrix(false, true)
            }
        }
    }

    /**
     * Traverses the object and all its children
     */
    fun traverse(callback: (Object3D) -> Unit) {
        callback(this)
        for (child in children) {
            child.traverse(callback)
        }
    }

    /**
     * Traverses only visible objects
     */
    fun traverseVisible(callback: (Object3D) -> Unit) {
        if (!visible) return
        callback(this)
        for (child in children) {
            child.traverseVisible(callback)
        }
    }

    /**
     * Traverses ancestors (parents)
     */
    fun traverseAncestors(callback: (Object3D) -> Unit) {
        parent?.let { parent ->
            callback(parent)
            parent.traverseAncestors(callback)
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

    private fun updateQuaternionFromEuler() {
        quaternion.setFromEuler(rotation)
    }

    private fun updateEulerFromQuaternion() {
        rotation.setFromQuaternion(quaternion)
    }

    private fun dispatchEvent(event: Event) {
        // Event system placeholder - could be expanded
    }

    override fun toString(): String {
        return "${this::class.simpleName}(id=$id, name='$name')"
    }
}

/**
 * Layer management for selective rendering
 */
data class Layers(var mask: Int = 1) {

    /**
     * Sets which layer this object belongs to
     */
    fun set(layer: Int) {
        mask = 1 shl layer
    }

    /**
     * Enables a layer
     */
    fun enable(layer: Int) {
        mask = mask or (1 shl layer)
    }

    /**
     * Disables a layer
     */
    fun disable(layer: Int) {
        mask = mask and (1 shl layer).inv()
    }

    /**
     * Toggles a layer
     */
    fun toggle(layer: Int) {
        mask = mask xor (1 shl layer)
    }

    /**
     * Tests if a layer is enabled
     */
    fun test(layers: Layers): Boolean {
        return (mask and layers.mask) != 0
    }

    /**
     * Tests if a specific layer is enabled
     */
    fun isEnabled(layer: Int): Boolean {
        return (mask and (1 shl layer)) != 0
    }

    /**
     * Enables all layers
     */
    fun enableAll() {
        mask = 0xffffffff.toInt()
    }

    /**
     * Disables all layers
     */
    fun disableAll() {
        mask = 0
    }
}

/**
 * Event system for Object3D
 */
sealed class Event {
    data class Added(val target: Object3D) : Event()
    data class Removed(val target: Object3D) : Event()
}

/**
 * Extension functions for common operations
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