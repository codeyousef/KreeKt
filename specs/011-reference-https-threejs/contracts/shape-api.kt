/**
 * Shape & Path API Contract
 * Maps FR-S001 through FR-S010
 *
 * Constitutional Requirements:
 * - Efficient 2D vector graphics to 3D geometry conversion
 * - Type-safe shape and path operations
 * - Performance: <20ms for complex shape extrusion
 */

package io.kreekt.shape

import io.kreekt.curve.Path
import io.kreekt.curve.Curve
import io.kreekt.math.Vector2
import io.kreekt.math.Vector3
import io.kreekt.geometry.BufferGeometry

/**
 * FR-S001, FR-S002: 2D shape with holes
 *
 * Test Contract:
 * - MUST define closed 2D shape
 * - MUST support arbitrary holes
 * - MUST be extrudable to 3D geometry
 */
class Shape(points: List<Vector2> = emptyList()) : Path(points) {
    /**
     * Holes in shape (inner boundaries)
     */
    val holes: MutableList<Path> = mutableListOf()

    /**
     * UUID for identification
     */
    val uuid: String = generateUUID()

    /**
     * Add hole to shape
     *
     * @param hole Path defining hole boundary
     *
     * Test Contract:
     * - MUST be closed path
     * - MUST be inside shape boundary
     * - MUST support multiple holes
     */
    fun addHole(hole: Path) {
        holes.add(hole)
    }

    /**
     * Remove hole from shape
     */
    fun removeHole(hole: Path): Boolean {
        return holes.remove(hole)
    }

    /**
     * Get points for shape and all holes
     *
     * Test Contract:
     * - MUST return main shape points
     * - MUST return hole points separately
     */
    fun extractPoints(divisions: Int = 12): ShapePoints

    /**
     * Clone shape with holes
     */
    fun clone(): Shape
}

/**
 * Shape and hole points
 */
data class ShapePoints(
    val shape: List<Vector2>,
    val holes: List<List<Vector2>>
)

/**
 * FR-S003, FR-S004: Shape geometry from 2D paths
 *
 * Test Contract:
 * - MUST triangulate shape with holes
 * - MUST generate UV coordinates
 * - MUST handle concave shapes
 * - MUST use Earcut or similar algorithm
 */
class ShapeGeometry(
    shapes: List<Shape>,
    curveSegments: Int = 12
) : BufferGeometry() {
    val parameters: ShapeGeometryParameters

    data class ShapeGeometryParameters(
        val shapes: List<Shape>,
        val curveSegments: Int
    )
}

/**
 * Convenience constructor for single shape
 */
fun ShapeGeometry(
    shape: Shape,
    curveSegments: Int = 12
): ShapeGeometry = ShapeGeometry(listOf(shape), curveSegments)

/**
 * FR-S005, FR-S006: Extrude geometry from 2D shape
 *
 * Test Contract:
 * - MUST extrude shape along Z axis or custom path
 * - MUST generate side faces
 * - MUST support beveling
 * - MUST support UV mapping
 */
class ExtrudeGeometry(
    shapes: List<Shape>,
    options: ExtrudeGeometryOptions = ExtrudeGeometryOptions()
) : BufferGeometry() {
    val parameters: ExtrudeGeometryParameters

    data class ExtrudeGeometryParameters(
        val shapes: List<Shape>,
        val options: ExtrudeGeometryOptions
    )
}

/**
 * Convenience constructor for single shape
 */
fun ExtrudeGeometry(
    shape: Shape,
    options: ExtrudeGeometryOptions = ExtrudeGeometryOptions()
): ExtrudeGeometry = ExtrudeGeometry(listOf(shape), options)

/**
 * FR-S007: Extrude options
 */
data class ExtrudeGeometryOptions(
    /**
     * Extrusion depth (Z direction)
     */
    val depth: Float = 1f,

    /**
     * Number of segments along extrusion path
     */
    val steps: Int = 1,

    /**
     * Bevel thickness on Z axis
     */
    val bevelThickness: Float = 0.2f,

    /**
     * Bevel size (offset from shape outline)
     */
    val bevelSize: Float = 0.1f,

    /**
     * Number of bevel segments
     */
    val bevelSegments: Int = 3,

    /**
     * Enable beveling
     */
    val bevelEnabled: Boolean = false,

    /**
     * Extrude along custom 3D path instead of Z axis
     */
    val extrudePath: Curve? = null,

    /**
     * Number of points on curves
     */
    val curveSegments: Int = 12,

    /**
     * UV generator for custom UV mapping
     */
    val UVGenerator: UVGenerator? = null
)

/**
 * FR-S008: UV generation for extruded shapes
 */
interface UVGenerator {
    /**
     * Generate UVs for front/back faces
     *
     * @param vertices Vertices of face
     * @return UV coordinates
     */
    fun generateTopUV(
        geometry: BufferGeometry,
        vertices: List<Vector3>,
        indexA: Int,
        indexB: Int,
        indexC: Int
    ): List<Vector2>

    /**
     * Generate UVs for side faces
     */
    fun generateSideWallUV(
        geometry: BufferGeometry,
        vertices: List<Vector3>,
        indexA: Int,
        indexB: Int,
        indexC: Int,
        indexD: Int
    ): List<Vector2>
}

/**
 * Default UV generator for extruded geometry
 */
object WorldUVGenerator : UVGenerator {
    override fun generateTopUV(
        geometry: BufferGeometry,
        vertices: List<Vector3>,
        indexA: Int,
        indexB: Int,
        indexC: Int
    ): List<Vector2>

    override fun generateSideWallUV(
        geometry: BufferGeometry,
        vertices: List<Vector3>,
        indexA: Int,
        indexB: Int,
        indexC: Int,
        indexD: Int
    ): List<Vector2>
}

/**
 * FR-S009: Lathe geometry by rotating 2D shape
 *
 * Test Contract:
 * - MUST revolve points around Y axis
 * - MUST generate smooth surface
 * - MUST support custom rotation angles
 */
class LatheGeometry(
    points: List<Vector2>,
    segments: Int = 12,
    phiStart: Float = 0f,
    phiLength: Float = Math.PI.toFloat() * 2f
) : BufferGeometry() {
    val parameters: LatheGeometryParameters

    data class LatheGeometryParameters(
        val points: List<Vector2>,
        val segments: Int,
        val phiStart: Float,
        val phiLength: Float
    )
}

/**
 * FR-S010: Shape utilities
 */
object ShapeUtils {
    /**
     * Triangulate 2D shape with holes using Earcut algorithm
     *
     * @param contour Main shape boundary
     * @param holes Hole boundaries
     * @return Triangle indices
     *
     * Test Contract:
     * - MUST handle concave shapes
     * - MUST handle multiple holes
     * - MUST produce valid triangulation
     * - MUST use Earcut algorithm
     */
    fun triangulateShape(
        contour: List<Vector2>,
        holes: List<List<Vector2>>
    ): List<List<Int>>

    /**
     * Compute shape area (signed)
     *
     * Test Contract:
     * - MUST return positive for CCW winding
     * - MUST return negative for CW winding
     */
    fun area(contour: List<Vector2>): Float

    /**
     * Check if point is inside shape
     *
     * Test Contract:
     * - MUST use ray casting algorithm
     * - MUST handle edge cases
     */
    fun isPointInsideShape(point: Vector2, shape: List<Vector2>): Boolean

    /**
     * Simplify shape by removing collinear points
     *
     * Test Contract:
     * - MUST preserve shape outline
     * - MUST reduce point count
     */
    fun simplifyShape(
        points: List<Vector2>,
        tolerance: Float = 0.01f
    ): List<Vector2>

    /**
     * Compute bounding box of shape
     */
    fun computeBounds(points: List<Vector2>): ShapeBounds

    /**
     * Offset shape outline (inward or outward)
     *
     * @param distance Offset distance (positive = outward, negative = inward)
     *
     * Test Contract:
     * - MUST handle self-intersections
     * - MUST preserve topology where possible
     */
    fun offsetShape(
        shape: List<Vector2>,
        distance: Float
    ): List<Vector2>
}

data class ShapeBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
)

/**
 * Pre-defined shapes for convenience
 */
object Shapes {
    /**
     * Create circle shape
     */
    fun circle(radius: Float = 1f, segments: Int = 32): Shape

    /**
     * Create rectangle shape
     */
    fun rectangle(width: Float = 1f, height: Float = 1f): Shape

    /**
     * Create rounded rectangle shape
     */
    fun roundedRectangle(
        width: Float = 1f,
        height: Float = 1f,
        radius: Float = 0.1f
    ): Shape

    /**
     * Create polygon shape
     */
    fun polygon(sides: Int = 6, radius: Float = 1f): Shape

    /**
     * Create star shape
     */
    fun star(
        points: Int = 5,
        outerRadius: Float = 1f,
        innerRadius: Float = 0.5f
    ): Shape

    /**
     * Create heart shape
     */
    fun heart(scale: Float = 1f): Shape

    /**
     * Create text shape from font
     *
     * Test Contract:
     * - MUST load font definition
     * - MUST generate shape paths from glyphs
     * - MUST support kerning
     */
    fun text(
        text: String,
        font: Font,
        size: Float = 1f
    ): List<Shape>
}

/**
 * Font definition for text shapes
 */
interface Font {
    val familyName: String
    val styleName: String
    val unitsPerEm: Int

    /**
     * Get glyph path for character
     */
    fun getGlyphPath(char: Char, size: Float): Path?

    /**
     * Get kerning between two characters
     */
    fun getKerning(leftChar: Char, rightChar: Char, size: Float): Float
}

/**
 * Font loader for loading TrueType/OpenType fonts
 */
expect class FontLoader {
    suspend fun load(url: String): Result<Font>
}

// Helper function
private fun generateUUID(): String {
    // Platform-specific UUID generation
    return java.util.UUID.randomUUID().toString()
}