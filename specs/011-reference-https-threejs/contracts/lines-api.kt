/**
 * Enhanced Line Rendering API Contract
 * Maps FR-LI001 through FR-LI008
 *
 * Constitutional Requirements:
 * - High-quality line rendering with variable width
 * - Type-safe line materials
 * - Performance: 100k line segments at 60 FPS
 */

package io.kreekt.lines

import io.kreekt.core.Object3D
import io.kreekt.geometry.BufferGeometry
import io.kreekt.material.Material
import io.kreekt.math.Color
import io.kreekt.math.Vector3
import io.kreekt.texture.Texture

/**
 * FR-LI001: Basic line rendering
 *
 * Test Contract:
 * - MUST render lines between vertices
 * - MUST support line materials
 * - MUST support vertex colors
 */
open class Line(
    geometry: BufferGeometry = BufferGeometry(),
    material: LineBasicMaterial = LineBasicMaterial()
) : Object3D() {
    var geometry: BufferGeometry
    var material: Material

    /**
     * Compute line distances for dashed lines
     *
     * Test Contract:
     * - MUST compute cumulative distance along line
     * - MUST store in lineDistance attribute
     */
    fun computeLineDistances()

    /**
     * Raycast against line
     */
    fun raycast(raycaster: Raycaster, intersects: MutableList<Intersection>)

    fun clone(): Line
    fun dispose()
}

/**
 * FR-LI002: Line segments (disconnected)
 *
 * Test Contract:
 * - MUST render pairs of vertices as segments
 * - MUST NOT connect segments
 */
class LineSegments(
    geometry: BufferGeometry = BufferGeometry(),
    material: LineBasicMaterial = LineBasicMaterial()
) : Line(geometry, material)

/**
 * FR-LI003: Line loop (connected back to start)
 *
 * Test Contract:
 * - MUST connect last vertex to first
 * - MUST render as closed loop
 */
class LineLoop(
    geometry: BufferGeometry = BufferGeometry(),
    material: LineBasicMaterial = LineBasicMaterial()
) : Line(geometry, material)

/**
 * FR-LI004: Basic line material
 *
 * Test Contract:
 * - MUST render lines with constant width (1px)
 * - MUST support vertex colors
 * - MUST support dashed lines
 */
class LineBasicMaterial(
    parameters: LineBasicMaterialParameters = LineBasicMaterialParameters()
) : Material() {
    var color: Color
    var linewidth: Float  // Note: often ignored by WebGL/WebGPU
    var linecap: String
    var linejoin: String
    var vertexColors: Boolean

    // Dashed line properties
    var dashSize: Float
    var gapSize: Float
    var dashScale: Float
    var dashOffset: Float

    override fun clone(): LineBasicMaterial
}

data class LineBasicMaterialParameters(
    val color: Color = Color(0xffffff),
    val linewidth: Float = 1f,
    val linecap: String = "round",
    val linejoin: String = "round",
    val vertexColors: Boolean = false,
    val dashSize: Float = 3f,
    val gapSize: Float = 1f,
    val dashScale: Float = 1f,
    val dashOffset: Float = 0f,
    val fog: Boolean = true,
    val transparent: Boolean = false,
    val opacity: Float = 1f
)

/**
 * FR-LI005: Dashed line material
 *
 * Test Contract:
 * - MUST render dashed line pattern
 * - MUST use lineDistance attribute
 * - MUST support custom dash/gap sizes
 */
class LineDashedMaterial(
    parameters: LineDashedMaterialParameters = LineDashedMaterialParameters()
) : LineBasicMaterial(
    LineBasicMaterialParameters(
        color = parameters.color,
        linewidth = parameters.linewidth,
        vertexColors = parameters.vertexColors,
        dashSize = parameters.dashSize,
        gapSize = parameters.gapSize,
        dashScale = parameters.scale,
        dashOffset = parameters.dashOffset
    )
) {
    var scale: Float
}

data class LineDashedMaterialParameters(
    val color: Color = Color(0xffffff),
    val linewidth: Float = 1f,
    val vertexColors: Boolean = false,
    val dashSize: Float = 3f,
    val gapSize: Float = 1f,
    val scale: Float = 1f,
    val dashOffset: Float = 0f
)

/**
 * FR-LI006, FR-LI007: Line2 for high-quality lines with variable width
 *
 * Test Contract:
 * - MUST render lines as screen-space quads
 * - MUST support variable width in pixels
 * - MUST support resolution-independent rendering
 * - MUST support dashing
 */
class Line2(
    geometry: LineGeometry = LineGeometry(),
    material: LineMaterial = LineMaterial()
) : Object3D() {
    var geometry: LineGeometry
    var material: LineMaterial

    /**
     * Compute line distances for dashed lines
     */
    fun computeLineDistances()

    fun raycast(raycaster: Raycaster, intersects: MutableList<Intersection>)
    fun clone(): Line2
    fun dispose()
}

/**
 * Line segments variant of Line2
 */
class LineSegments2(
    geometry: LineSegmentsGeometry = LineSegmentsGeometry(),
    material: LineMaterial = LineMaterial()
) : Line2(geometry, material)

/**
 * FR-LI008: Advanced line material
 *
 * Test Contract:
 * - MUST support pixel-accurate width
 * - MUST support world units or screen pixels
 * - MUST support gradient colors along line
 * - MUST support texture mapping
 */
class LineMaterial(
    parameters: LineMaterialParameters = LineMaterialParameters()
) : Material() {
    var color: Color

    /**
     * Line width in pixels (worldUnits=false) or world units (worldUnits=true)
     */
    var linewidth: Float

    /**
     * Interpret linewidth as world units
     * true = world space width
     * false = screen pixel width (resolution independent)
     */
    var worldUnits: Boolean

    /**
     * Render resolution (for screen-space calculations)
     */
    var resolution: Vector2

    /**
     * Use vertex colors
     */
    var vertexColors: Boolean

    /**
     * Alpha map texture
     */
    var alphaMap: Texture?

    // Dashing
    var dashed: Boolean
    var dashScale: Float
    var dashSize: Float
    var gapSize: Float
    var dashOffset: Float

    override fun clone(): LineMaterial
}

data class LineMaterialParameters(
    val color: Color = Color(0xffffff),
    val linewidth: Float = 1f,
    val worldUnits: Boolean = false,
    val resolution: Vector2 = Vector2(1f, 1f),
    val vertexColors: Boolean = false,
    val alphaMap: Texture? = null,
    val dashed: Boolean = false,
    val dashScale: Float = 1f,
    val dashSize: Float = 1f,
    val gapSize: Float = 1f,
    val dashOffset: Float = 0f,
    val fog: Boolean = true,
    val transparent: Boolean = false,
    val opacity: Float = 1f
)

/**
 * Geometry for Line2
 *
 * Test Contract:
 * - MUST store line positions in specific format
 * - MUST compute quad geometry from line segments
 */
class LineGeometry : BufferGeometry() {
    /**
     * Set line positions from array
     *
     * @param array Flat array of [x0, y0, z0, x1, y1, z1, ...]
     */
    fun setPositions(array: FloatArray): LineGeometry

    /**
     * Set line colors from array
     *
     * @param array Flat array of [r0, g0, b0, r1, g1, b1, ...]
     */
    fun setColors(array: FloatArray): LineGeometry

    /**
     * Get positions array
     */
    fun getPositions(): FloatArray

    /**
     * Get colors array
     */
    fun getColors(): FloatArray
}

/**
 * Geometry for LineSegments2
 */
class LineSegmentsGeometry : LineGeometry() {
    /**
     * Create from connected line
     */
    fun fromLine(line: Line): LineSegmentsGeometry

    /**
     * Create from mesh edges
     */
    fun fromMeshEdges(mesh: Mesh): LineSegmentsGeometry

    /**
     * Create from wireframe
     */
    fun fromWireframe(geometry: BufferGeometry): LineSegmentsGeometry
}

/**
 * Line utilities
 */
object LineUtils {
    /**
     * Create line from points
     */
    fun fromPoints(
        points: List<Vector3>,
        material: Material = LineBasicMaterial()
    ): Line

    /**
     * Create Line2 from points with advanced rendering
     */
    fun fromPoints2(
        points: List<Vector3>,
        material: LineMaterial = LineMaterial()
    ): Line2

    /**
     * Create wireframe from geometry
     */
    fun createWireframe(
        geometry: BufferGeometry,
        material: Material = LineBasicMaterial()
    ): LineSegments

    /**
     * Create edge lines (only unique edges)
     */
    fun createEdges(
        geometry: BufferGeometry,
        thresholdAngle: Float = 1f
    ): LineSegments

    /**
     * Smooth line by inserting intermediate points
     *
     * Test Contract:
     * - MUST add points between existing vertices
     * - MUST use Catmull-Rom interpolation
     */
    fun smoothLine(
        points: List<Vector3>,
        divisions: Int = 1
    ): List<Vector3>

    /**
     * Simplify line using Ramer-Douglas-Peucker algorithm
     *
     * Test Contract:
     * - MUST reduce point count
     * - MUST preserve shape within tolerance
     */
    fun simplifyLine(
        points: List<Vector3>,
        tolerance: Float = 0.1f
    ): List<Vector3>

    /**
     * Compute total length of line
     */
    fun computeLength(points: List<Vector3>): Float

    /**
     * Sample point at distance along line
     */
    fun getPointAtDistance(
        points: List<Vector3>,
        distance: Float
    ): Vector3
}

// Forward declarations
expect class Vector2
expect class Raycaster
expect class Intersection
expect class Mesh