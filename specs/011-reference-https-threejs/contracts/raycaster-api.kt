/**
 * Raycasting API Contract
 * Maps FR-R001 through FR-R010
 *
 * Constitutional Requirements:
 * - BVH acceleration for performance
 * - Type-safe intersection results
 * - Performance: <16ms for 10,000 objects
 */

package io.kreekt.raycaster

import io.kreekt.core.Object3D
import io.kreekt.math.Vector2
import io.kreekt.math.Vector3
import io.kreekt.math.Ray
import io.kreekt.camera.Camera
import io.kreekt.geometry.BufferGeometry
import io.kreekt.material.Material

/**
 * FR-R001, FR-R002, FR-R003: Raycaster for 3D intersection testing
 *
 * Test Contract:
 * - MUST detect intersections with meshes, lines, points
 * - MUST return sorted results by distance
 * - MUST support recursive object traversal
 * - MUST filter by layers and distance
 */
class Raycaster(
    origin: Vector3 = Vector3(),
    direction: Vector3 = Vector3(0f, 0f, -1f),
    near: Float = 0f,
    far: Float = Float.POSITIVE_INFINITY
) {
    val ray: Ray
    var near: Float
    var far: Float
    var camera: Camera?
    var layers: Layers
    var params: RaycastParams

    /**
     * FR-R004: Set ray from camera and normalized device coordinates
     *
     * @param coords Mouse/touch coordinates (-1 to 1)
     * @param camera Camera to cast from
     *
     * Test Contract:
     * - MUST convert NDC to world space
     * - MUST account for camera projection
     * - MUST set ray origin and direction correctly
     */
    fun setFromCamera(coords: Vector2, camera: Camera)

    /**
     * FR-R005: Intersect objects in scene
     *
     * @param objects Objects to test
     * @param recursive Test children recursively
     * @return Sorted intersections by distance
     *
     * Test Contract:
     * - MUST test ray against object bounds first (BVH)
     * - MUST test ray-triangle intersection for meshes
     * - MUST test ray-line intersection for lines
     * - MUST test ray-point intersection for points
     * - MUST return sorted by distance ascending
     * - MUST respect near/far clipping
     * - MUST respect layer filtering
     */
    fun intersectObjects(
        objects: List<Object3D>,
        recursive: Boolean = false
    ): List<Intersection>

    /**
     * FR-R006: Intersect single object
     *
     * Test Contract:
     * - MUST return all intersections with object
     * - MUST include child intersections if recursive
     */
    fun intersectObject(
        obj: Object3D,
        recursive: Boolean = false
    ): List<Intersection>

    /**
     * Set ray origin and direction directly
     */
    fun set(origin: Vector3, direction: Vector3)
}

/**
 * FR-R007: Intersection result data
 *
 * Test Contract:
 * - MUST include all relevant intersection data
 * - MUST be sorted by distance
 */
data class Intersection(
    /**
     * Distance from ray origin to intersection point
     */
    val distance: Float,

    /**
     * Intersection point in world space
     */
    val point: Vector3,

    /**
     * Face that was intersected (meshes only)
     */
    val face: Face?,

    /**
     * Face index in geometry (meshes only)
     */
    val faceIndex: Int?,

    /**
     * Object that was intersected
     */
    val `object`: Object3D,

    /**
     * UV coordinates at intersection (if available)
     */
    val uv: Vector2?,

    /**
     * Second UV set (if available)
     */
    val uv1: Vector2?,

    /**
     * Instance ID (for InstancedMesh)
     */
    val instanceId: Int?,

    /**
     * Normal at intersection point (if computed)
     */
    val normal: Vector3?
)

/**
 * Face representation for intersection
 */
data class Face(
    val a: Int,  // Vertex index 1
    val b: Int,  // Vertex index 2
    val c: Int,  // Vertex index 3
    val normal: Vector3,
    val materialIndex: Int = 0
)

/**
 * FR-R008: Raycasting parameters for different object types
 *
 * Test Contract:
 * - MUST provide type-specific thresholds
 * - MUST be configurable per raycaster
 */
data class RaycastParams(
    /**
     * Threshold for point intersection (world units)
     */
    var pointsThreshold: Float = 1f,

    /**
     * Threshold for line intersection (world units)
     */
    var lineThreshold: Float = 1f,

    /**
     * Maximum LOD level to test
     */
    var lodMaxLevel: Int = Int.MAX_VALUE
)

/**
 * FR-R009: Layer filtering for raycasting
 *
 * Test Contract:
 * - MUST support 32 layers (bitmask)
 * - MUST filter objects by layer
 * - MUST support layer testing
 */
class Layers {
    private var mask: Int = 1  // Default layer 0

    /**
     * Set layer mask to single layer
     */
    fun set(layer: Int) {
        require(layer in 0..31) { "Layer must be 0-31" }
        mask = 1 shl layer
    }

    /**
     * Enable a layer
     */
    fun enable(layer: Int) {
        require(layer in 0..31) { "Layer must be 0-31" }
        mask = mask or (1 shl layer)
    }

    /**
     * Disable a layer
     */
    fun disable(layer: Int) {
        require(layer in 0..31) { "Layer must be 0-31" }
        mask = mask and (1 shl layer).inv()
    }

    /**
     * Toggle a layer
     */
    fun toggle(layer: Int) {
        require(layer in 0..31) { "Layer must be 0-31" }
        mask = mask xor (1 shl layer)
    }

    /**
     * Test if layer is enabled
     */
    fun test(layers: Layers): Boolean {
        return (mask and layers.mask) != 0
    }

    /**
     * Check if specific layer is enabled
     */
    fun isEnabled(layer: Int): Boolean {
        require(layer in 0..31) { "Layer must be 0-31" }
        return (mask and (1 shl layer)) != 0
    }

    /**
     * Enable all layers
     */
    fun enableAll() {
        mask = -1  // All bits set
    }

    /**
     * Disable all layers
     */
    fun disableAll() {
        mask = 0
    }
}

/**
 * FR-R010: BVH acceleration structure (internal)
 *
 * Test Contract:
 * - MUST accelerate raycasting for large scenes
 * - MUST update when objects transform
 * - MUST use SAH for construction
 * - Target: <5% CPU overhead for BVH maintenance
 */
internal class BVH(objects: List<Object3D>) {
    private val root: BVHNode

    /**
     * Intersect ray with BVH
     *
     * @return Objects that potentially intersect ray
     */
    fun intersectRay(ray: Ray, near: Float, far: Float): List<Object3D>

    /**
     * Update BVH after object transforms
     *
     * @param objects Objects that changed
     */
    fun update(objects: List<Object3D>)

    /**
     * Rebuild BVH from scratch
     */
    fun rebuild()
}

internal sealed class BVHNode {
    abstract val bounds: AABB

    data class Leaf(
        override val bounds: AABB,
        val objects: List<Object3D>
    ) : BVHNode()

    data class Internal(
        override val bounds: AABB,
        val left: BVHNode,
        val right: BVHNode
    ) : BVHNode()
}

/**
 * Axis-aligned bounding box
 */
data class AABB(
    val min: Vector3,
    val max: Vector3
) {
    fun intersectsRay(ray: Ray, tMin: Float, tMax: Float): Boolean
    fun contains(point: Vector3): Boolean
    fun union(other: AABB): AABB
    fun surfaceArea(): Float
}