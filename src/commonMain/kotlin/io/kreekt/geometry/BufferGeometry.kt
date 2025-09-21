/**
 * Enhanced BufferGeometry implementation with advanced 3D features
 * Extends the basic geometry system with morph targets, instancing, and LOD support
 */
package io.kreekt.geometry
import io.kreekt.core.math.Box3

import io.kreekt.core.math.*
import io.kreekt.core.platform.platformClone
import io.kreekt.core.platform.currentTimeMillis
import kotlinx.collections.immutable.*
import kotlinx.serialization.Serializable

/**
 * Advanced buffer geometry with morph targets and instancing support
 * Compatible with Three.js BufferGeometry patterns
 */
open class BufferGeometry {
    // Core attributes
    private val _attributes = mutableMapOf<String, BufferAttribute>()
    private var _index: BufferAttribute? = null

    // Morph targets
    private val _morphAttributes = mutableMapOf<String, Array<BufferAttribute>>()
    var morphTargetsRelative: Boolean = false

    // Instancing
    private val _instancedAttributes = mutableMapOf<String, BufferAttribute>()
    private var _instanceCount: Int = 0

    // Geometry groups for multi-material support
    private val _groups = mutableListOf<GeometryGroup>()

    // Bounding volumes (lazy computed)
    private var _boundingBox: Box3? = null
    private var _boundingSphere: Sphere? = null
    private var _boundingBoxNeedsUpdate = true
    private var _boundingSphereNeedsUpdate = true

    // LOD support
    private val _lodLevels = mutableListOf<LodLevel>()
    private var _activeLodLevel: Int = 0

    // Metadata and state
    var uuid: String = "geometry-${currentTimeMillis()}-${(kotlin.random.Random.nextDouble() * 1000000).toInt()}"
        private set
    var name: String = ""

    // Events
    private val _onDisposeCallbacks = mutableListOf<() -> Unit>()

    /**
     * Core attribute management
     */
    fun setAttribute(name: String, attribute: BufferAttribute): BufferGeometry {
        _attributes[name] = attribute
        _markBoundingVolumesNeedUpdate()
        return this
    }

    fun getAttribute(name: String): BufferAttribute? = _attributes[name]

    fun deleteAttribute(name: String): BufferGeometry {
        _attributes.remove(name)
        _markBoundingVolumesNeedUpdate()
        return this
    }

    fun hasAttribute(name: String): Boolean = _attributes.containsKey(name)

    val attributes: Map<String, BufferAttribute> get() = _attributes.toMap()

    /**
     * Index buffer management
     */
    fun setIndex(index: BufferAttribute?): BufferGeometry {
        _index = index
        return this
    }

    fun getIndex(): BufferAttribute? = _index

    val index: BufferAttribute? get() = _index

    /**
     * Morph target management
     */
    fun setMorphAttribute(name: String, targets: Array<BufferAttribute>): BufferGeometry {
        _morphAttributes[name] = targets
        return this
    }

    fun getMorphAttribute(name: String): Array<BufferAttribute>? = _morphAttributes[name]

    fun deleteMorphAttribute(name: String): BufferGeometry {
        _morphAttributes.remove(name)
        return this
    }

    val morphAttributes: Map<String, Array<BufferAttribute>> get() = _morphAttributes.toMap()

    /**
     * Morph targets for backward compatibility
     * Returns the position morph attributes if they exist
     */
    val morphTargets: Array<BufferAttribute>?
        get() = _morphAttributes["position"]

    /**
     * Instancing support
     */
    fun setInstancedAttribute(name: String, attribute: BufferAttribute): BufferGeometry {
        _instancedAttributes[name] = attribute
        return this
    }

    fun getInstancedAttribute(name: String): BufferAttribute? = _instancedAttributes[name]

    fun deleteInstancedAttribute(name: String): BufferGeometry {
        _instancedAttributes.remove(name)
        return this
    }

    var instanceCount: Int
        get() = _instanceCount
        set(value) {
            _instanceCount = maxOf(0, value)
        }

    val instancedAttributes: Map<String, BufferAttribute> get() = _instancedAttributes.toMap()

    val isInstanced: Boolean get() = _instanceCount > 0

    /**
     * Geometry groups for multi-material rendering
     */
    fun addGroup(start: Int, count: Int, materialIndex: Int = 0): BufferGeometry {
        _groups.add(GeometryGroup(start, count, materialIndex))
        return this
    }

    fun clearGroups(): BufferGeometry {
        _groups.clear()
        return this
    }

    val groups: List<GeometryGroup> get() = _groups.toList()

    /**
     * Bounding volumes with lazy computation
     */
    fun computeBoundingBox(): Box3 {
        if (_boundingBox == null || _boundingBoxNeedsUpdate) {
            val positionAttribute = getAttribute("position")
            if (positionAttribute != null) {
                _boundingBox = Box3.fromBufferAttribute(positionAttribute)
                _boundingBoxNeedsUpdate = false
            } else {
                _boundingBox = Box3()
            }
        }
        return _boundingBox!!
    }

    fun computeBoundingSphere(): Sphere {
        if (_boundingSphere == null || _boundingSphereNeedsUpdate) {
            val positionAttribute = getAttribute("position")
            if (positionAttribute != null) {
                _boundingSphere = Sphere.fromBufferAttribute(positionAttribute)
                _boundingSphereNeedsUpdate = false
            } else {
                _boundingSphere = Sphere()
            }
        }
        return _boundingSphere!!
    }

    val boundingBox: Box3? get() = if (_boundingBoxNeedsUpdate) null else _boundingBox
    val boundingSphere: Sphere? get() = if (_boundingSphereNeedsUpdate) null else _boundingSphere

    /**
     * LOD (Level of Detail) management
     */
    fun addLodLevel(distance: Float, geometry: BufferGeometry): BufferGeometry {
        val triangleCount = geometry.getTriangleCount()
        _lodLevels.add(LodLevel(distance, geometry, triangleCount))
        _lodLevels.sortBy { it.distance }
        return this
    }

    fun getLodLevel(distance: Float): BufferGeometry? {
        for (level in _lodLevels) {
            if (distance <= level.distance) {
                return level.geometry
            }
        }
        return _lodLevels.lastOrNull()?.geometry
    }

    var activeLodLevel: Int
        get() = _activeLodLevel
        set(value) {
            _activeLodLevel = value.coerceIn(0, _lodLevels.size - 1)
        }

    val lodLevels: List<LodLevel> get() = _lodLevels.toList()

    /**
     * Utility methods
     */
    fun getTriangleCount(): Int {
        val index = _index
        return if (index != null) {
            index.count / 3
        } else {
            val position = getAttribute("position")
            if (position != null) {
                position.count / 3
            } else {
                0
            }
        }
    }

    fun getVertexCount(): Int {
        val position = getAttribute("position") ?: return 0
        return position.count / position.itemSize
    }

    fun isEmpty(): Boolean = getVertexCount() == 0

    /**
     * Geometry operations
     */
    fun translate(x: Float, y: Float, z: Float): BufferGeometry {
        val position = getAttribute("position")
        if (position != null) {
            for (i in 0 until position.count step 3) {
                position.setX(i / 3, position.getX(i / 3) + x)
                position.setY(i / 3, position.getY(i / 3) + y)
                position.setZ(i / 3, position.getZ(i / 3) + z)
            }
            position.needsUpdate = true
            _markBoundingVolumesNeedUpdate()
        }
        return this
    }

    fun scale(x: Float, y: Float, z: Float): BufferGeometry {
        val position = getAttribute("position")
        if (position != null) {
            for (i in 0 until position.count step 3) {
                position.setX(i / 3, position.getX(i / 3) * x)
                position.setY(i / 3, position.getY(i / 3) * y)
                position.setZ(i / 3, position.getZ(i / 3) * z)
            }
            position.needsUpdate = true
            _markBoundingVolumesNeedUpdate()
        }
        return this
    }

    fun rotateX(angle: Float): BufferGeometry {
        val matrix = Matrix4.rotationX(angle)
        return applyMatrix4(matrix)
    }

    fun rotateY(angle: Float): BufferGeometry {
        val matrix = Matrix4.rotationY(angle)
        return applyMatrix4(matrix)
    }

    fun rotateZ(angle: Float): BufferGeometry {
        val matrix = Matrix4.rotationZ(angle)
        return applyMatrix4(matrix)
    }

    fun applyMatrix4(matrix: Matrix4): BufferGeometry {
        val position = getAttribute("position")
        if (position != null) {
            position.applyMatrix4(matrix)
            _markBoundingVolumesNeedUpdate()
        }

        val normal = getAttribute("normal")
        if (normal != null) {
            val normalMatrix = Matrix3.normalMatrix(matrix)
            normal.applyNormalMatrix(normalMatrix)
        }

        return this
    }

    /**
     * Serialization and cloning
     */
    fun clone(): BufferGeometry {
        val cloned = BufferGeometry()

        // Clone attributes
        _attributes.forEach { (name, attribute) ->
            cloned.setAttribute(name, attribute.clone())
        }

        // Clone index
        _index?.let { cloned.setIndex(it.clone()) }

        // Clone morph attributes
        _morphAttributes.forEach { (name, targets) ->
            cloned.setMorphAttribute(name, targets.map { it.clone() }.toTypedArray())
        }

        // Clone instanced attributes
        _instancedAttributes.forEach { (name, attribute) ->
            cloned.setInstancedAttribute(name, attribute.clone())
        }

        // Copy properties
        cloned.morphTargetsRelative = morphTargetsRelative
        cloned.instanceCount = instanceCount
        cloned.name = name

        // Clone groups
        _groups.forEach { group ->
            cloned.addGroup(group.start, group.count, group.materialIndex)
        }

        // Clone LOD levels
        _lodLevels.forEach { level ->
            cloned.addLodLevel(level.distance, level.geometry.clone())
        }

        return cloned
    }

    /**
     * Resource disposal
     */
    fun dispose() {
        _onDisposeCallbacks.forEach { it() }
        _onDisposeCallbacks.clear()
    }

    fun onDispose(callback: () -> Unit) {
        _onDisposeCallbacks.add(callback)
    }

    private fun _markBoundingVolumesNeedUpdate() {
        _boundingBoxNeedsUpdate = true
        _boundingSphereNeedsUpdate = true
    }
}

/**
 * Geometry group for multi-material support
 */
@Serializable
data class GeometryGroup(
    val start: Int,
    val count: Int,
    val materialIndex: Int = 0
)

/**
 * LOD level definition
 */
data class LodLevel(
    val distance: Float,
    val geometry: BufferGeometry,
    val triangleCount: Int
)

/**
 * Buffer attribute for storing vertex data
 */
class BufferAttribute(
    val array: FloatArray,
    val itemSize: Int,
    val normalized: Boolean = false
) {
    var needsUpdate: Boolean = false
    var updateRange: IntRange = IntRange.EMPTY

    val count: Int get() = array.size / itemSize

    fun getX(index: Int): Float = array[(index * itemSize)]
    fun getY(index: Int): Float = array[index * itemSize + 1]
    fun getZ(index: Int): Float = array[index * itemSize + 2]
    fun getW(index: Int): Float = array[index * itemSize + 3]

    fun setX(index: Int, value: Float) { array[(index * itemSize)] = value }
    fun setY(index: Int, value: Float) { array[index * itemSize + 1] = value }
    fun setZ(index: Int, value: Float) { array[index * itemSize + 2] = value }
    fun setW(index: Int, value: Float) { array[index * itemSize + 3] = value }

    fun setXYZ(index: Int, x: Float, y: Float, z: Float) {
        val offset = index * itemSize
        array[offset] = x
        array[offset + 1] = y
        array[offset + 2] = z
    }

    fun setXYZW(index: Int, x: Float, y: Float, z: Float, w: Float) {
        val offset = index * itemSize
        array[offset] = x
        array[offset + 1] = y
        array[offset + 2] = z
        array[offset + 3] = w
    }

    fun clone(): BufferAttribute {
        return BufferAttribute(array.copyOf(), itemSize, normalized).apply {
            needsUpdate = this@BufferAttribute.needsUpdate
            updateRange = this@BufferAttribute.updateRange
        }
    }

    fun applyMatrix4(matrix: Matrix4) {
        if (itemSize == 3) {
            for (i in 0 until count) {
                val vector = Vector3(getX(i), getY(i), getZ(i))
                vector.applyMatrix4(matrix)
                setXYZ(i, vector.x, vector.y, vector.z)
            }
        }
        needsUpdate = true
    }

    fun applyNormalMatrix(matrix: Matrix3) {
        if (itemSize == 3) {
            for (i in 0 until count) {
                val vector = Vector3(getX(i), getY(i), getZ(i))
                vector.applyMatrix3(matrix).normalize()
                setXYZ(i, vector.x, vector.y, vector.z)
            }
        }
        needsUpdate = true
    }
}

/**
 * Extension functions for Box3 and Sphere to compute from BufferAttribute
 */
fun Box3.Companion.fromBufferAttribute(attribute: BufferAttribute): Box3 {
    val box = Box3()

    for (i in 0 until attribute.count) {
        val x = attribute.getX(i)
        val y = attribute.getY(i)
        val z = attribute.getZ(i)
        box.expandByPoint(Vector3(x, y, z))
    }

    return box
}

fun Sphere.Companion.fromBufferAttribute(attribute: BufferAttribute): Sphere {
    val sphere = Sphere()
    val box = Box3.fromBufferAttribute(attribute)
    box.getBoundingSphere(sphere)
    return sphere
}