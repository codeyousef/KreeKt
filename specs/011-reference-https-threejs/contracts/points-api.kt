/**
 * Points & Sprites API Contract
 * Maps FR-P001 through FR-P010
 *
 * Constitutional Requirements:
 * - Efficient point cloud rendering
 * - Type-safe point materials
 * - Performance: 1M+ points at 60 FPS
 */

package io.kreekt.points

import io.kreekt.core.Object3D
import io.kreekt.geometry.BufferGeometry
import io.kreekt.material.Material
import io.kreekt.math.Color
import io.kreekt.texture.Texture
import io.kreekt.raycaster.Raycaster
import io.kreekt.raycaster.Intersection

/**
 * FR-P001, FR-P002: Points for rendering point clouds
 *
 * Test Contract:
 * - MUST render large point clouds efficiently
 * - MUST support per-point colors
 * - MUST support per-point sizes
 * - Performance target: 1M+ points at 60 FPS
 */
class Points(
    geometry: BufferGeometry = BufferGeometry(),
    material: PointsMaterial = PointsMaterial()
) : Object3D() {
    var geometry: BufferGeometry
    var material: PointsMaterial

    /**
     * FR-P003: Raycast against points
     *
     * @param raycaster Raycaster to test
     * @param intersects Output list of intersections
     *
     * Test Contract:
     * - MUST test ray against point positions
     * - MUST use threshold distance from raycaster.params
     * - MUST return sorted by distance
     */
    fun raycast(raycaster: Raycaster, intersects: MutableList<Intersection>)

    /**
     * Update bounding sphere for culling
     */
    fun computeBoundingSphere()

    /**
     * Update bounding box for culling
     */
    fun computeBoundingBox()

    /**
     * Clone points object
     */
    fun clone(): Points

    /**
     * Dispose resources
     */
    fun dispose()
}

/**
 * FR-P004, FR-P005: Point material with customization
 *
 * Test Contract:
 * - MUST support point size
 * - MUST support point color
 * - MUST support vertex colors
 * - MUST support size attenuation (perspective scaling)
 * - MUST support alpha test
 */
class PointsMaterial(
    parameters: PointsMaterialParameters = PointsMaterialParameters()
) : Material() {
    /**
     * Base point color
     */
    var color: Color

    /**
     * Point size in pixels (or world units if sizeAttenuation=false)
     */
    var size: Float

    /**
     * Apply perspective size attenuation
     * true = points get smaller with distance
     * false = constant pixel size
     */
    var sizeAttenuation: Boolean

    /**
     * Use per-vertex colors from geometry
     */
    var vertexColors: Boolean

    /**
     * Point sprite texture
     */
    var map: Texture?

    /**
     * Alpha map texture
     */
    var alphaMap: Texture?

    /**
     * Alpha test threshold (0-1)
     * Fragments with alpha below threshold are discarded
     */
    var alphaTest: Float

    /**
     * Fog affects points
     */
    var fog: Boolean

    override fun clone(): PointsMaterial
}

data class PointsMaterialParameters(
    val color: Color = Color(0xffffff),
    val size: Float = 1f,
    val sizeAttenuation: Boolean = true,
    val vertexColors: Boolean = false,
    val map: Texture? = null,
    val alphaMap: Texture? = null,
    val alphaTest: Float = 0f,
    val fog: Boolean = true,
    val transparent: Boolean = false,
    val opacity: Float = 1f,
    val blending: Blending = Blending.NORMAL,
    val depthTest: Boolean = true,
    val depthWrite: Boolean = true
)

/**
 * FR-P006, FR-P007: Sprite for billboarded quads
 *
 * Test Contract:
 * - MUST always face camera
 * - MUST support custom materials
 * - MUST raycast correctly
 */
class Sprite(
    material: SpriteMaterial = SpriteMaterial()
) : Object3D() {
    var material: SpriteMaterial

    /**
     * Raycast against sprite
     *
     * Test Contract:
     * - MUST test against billboard quad
     * - MUST account for sprite center
     * - MUST return UV coordinates
     */
    fun raycast(raycaster: Raycaster, intersects: MutableList<Intersection>)

    /**
     * Clone sprite
     */
    fun clone(): Sprite

    /**
     * Dispose resources
     */
    fun dispose()
}

/**
 * FR-P008, FR-P009: Sprite material for billboards
 *
 * Test Contract:
 * - MUST support texture mapping
 * - MUST support transparency
 * - MUST support custom rotation
 */
class SpriteMaterial(
    parameters: SpriteMaterialParameters = SpriteMaterialParameters()
) : Material() {
    /**
     * Base sprite color
     */
    var color: Color

    /**
     * Sprite texture map
     */
    var map: Texture?

    /**
     * Alpha map texture
     */
    var alphaMap: Texture?

    /**
     * Rotation around Z axis (radians)
     */
    var rotation: Float

    /**
     * Fog affects sprite
     */
    var fog: Boolean

    /**
     * Size attenuation (perspective scaling)
     */
    var sizeAttenuation: Boolean

    override fun clone(): SpriteMaterial
}

data class SpriteMaterialParameters(
    val color: Color = Color(0xffffff),
    val map: Texture? = null,
    val alphaMap: Texture? = null,
    val rotation: Float = 0f,
    val fog: Boolean = false,
    val sizeAttenuation: Boolean = true,
    val transparent: Boolean = true,
    val opacity: Float = 1f,
    val blending: Blending = Blending.NORMAL,
    val depthTest: Boolean = true,
    val depthWrite: Boolean = false
)

/**
 * FR-P010: Point cloud utilities
 */
object PointCloudUtils {
    /**
     * Create points from position array
     *
     * Test Contract:
     * - MUST create geometry with position attribute
     * - MUST set appropriate material
     */
    fun fromPositions(
        positions: FloatArray,
        material: PointsMaterial = PointsMaterial()
    ): Points

    /**
     * Add per-point colors to geometry
     *
     * Test Contract:
     * - MUST create color attribute
     * - MUST match position count
     */
    fun addColors(
        points: Points,
        colors: FloatArray
    )

    /**
     * Add per-point sizes to geometry
     *
     * Test Contract:
     * - MUST create size attribute
     * - MUST match position count
     */
    fun addSizes(
        points: Points,
        sizes: FloatArray
    )

    /**
     * Decimate point cloud for LOD
     *
     * @param points Source point cloud
     * @param targetCount Target point count
     * @param method Decimation method
     *
     * Test Contract:
     * - MUST preserve point distribution
     * - MUST reach target count approximately
     */
    fun decimate(
        points: Points,
        targetCount: Int,
        method: DecimationMethod = DecimationMethod.RANDOM
    ): Points

    /**
     * Compute point cloud bounds
     */
    fun computeBounds(points: Points): BoundingBox

    /**
     * Sort points back-to-front for transparency
     *
     * Test Contract:
     * - MUST sort by distance to camera
     * - MUST reorder position and color attributes
     */
    fun sortByDepth(points: Points, camera: Camera)

    /**
     * Create LOD hierarchy for point clouds
     *
     * Test Contract:
     * - MUST create multiple LOD levels
     * - MUST set appropriate distance thresholds
     */
    fun createLODHierarchy(
        points: Points,
        levels: List<Float>  // Target point counts per level
    ): LOD
}

enum class DecimationMethod {
    /**
     * Random sampling
     */
    RANDOM,

    /**
     * Uniform grid sampling
     */
    GRID,

    /**
     * Preserve high-curvature regions
     */
    ADAPTIVE
}

data class BoundingBox(
    val min: Vector3,
    val max: Vector3
)

enum class Blending {
    NONE,
    NORMAL,
    ADDITIVE,
    SUBTRACTIVE,
    MULTIPLY,
    CUSTOM
}

// Forward declarations
expect class Vector3
expect class Camera
expect class LOD