/**
 * Curve System API Contract
 * Maps FR-CR001 through FR-CR015
 *
 * Constitutional Requirements:
 * - Type-safe curve implementations
 * - Efficient tessellation algorithms
 * - Performance: <5ms for 100 curve segments
 */

package io.kreekt.curve

import io.kreekt.math.Vector2
import io.kreekt.math.Vector3
import io.kreekt.math.Matrix4

/**
 * FR-CR001, FR-CR002: Base curve class
 *
 * Test Contract:
 * - MUST provide parametric evaluation (t ∈ [0, 1])
 * - MUST compute tangents at any point
 * - MUST compute accurate arc length
 * - MUST provide uniform point distribution
 */
abstract class Curve {
    var arcLengthDivisions: Int = 200

    /**
     * Get point on curve at parameter t
     *
     * @param t Parameter in [0, 1]
     * @return Point on curve
     *
     * Test Contract:
     * - MUST return start point at t=0
     * - MUST return end point at t=1
     * - MUST interpolate smoothly
     */
    abstract fun getPoint(t: Float): Vector3

    /**
     * Get tangent vector at parameter t
     *
     * @param t Parameter in [0, 1]
     * @return Normalized tangent vector
     */
    open fun getTangent(t: Float): Vector3 {
        val delta = 0.0001f
        val t1 = (t - delta).coerceIn(0f, 1f)
        val t2 = (t + delta).coerceIn(0f, 1f)

        val pt1 = getPoint(t1)
        val pt2 = getPoint(t2)

        return (pt2 - pt1).normalize()
    }

    /**
     * Get evenly spaced points along curve
     *
     * @param divisions Number of divisions
     * @return List of points
     *
     * Test Contract:
     * - MUST distribute points by arc length (not parameter)
     * - MUST include start and end points
     */
    fun getSpacedPoints(divisions: Int = 5): List<Vector3>

    /**
     * Get points by uniform parameter distribution
     *
     * Test Contract:
     * - MUST distribute by parameter (not arc length)
     */
    fun getPoints(divisions: Int = 5): List<Vector3>

    /**
     * Get total arc length of curve
     */
    fun getLength(): Float

    /**
     * Get cumulative arc lengths at divisions
     */
    fun getLengths(divisions: Int = this.arcLengthDivisions): List<Float>

    /**
     * Update cached arc length computations
     */
    fun updateArcLengths()

    /**
     * Convert from arc length u to parameter t
     */
    fun getUtoTmapping(u: Float, distance: Float): Float

    /**
     * Get point at specific arc length distance
     */
    fun getPointAt(u: Float): Vector3

    /**
     * Get tangent at specific arc length distance
     */
    fun getTangentAt(u: Float): Vector3
}

/**
 * FR-CR003: 3D Catmull-Rom spline
 *
 * Test Contract:
 * - MUST pass through all control points
 * - MUST support closed curves
 * - MUST support tension parameter (centripetal/chordal/uniform)
 */
class CatmullRomCurve3(
    points: List<Vector3> = emptyList(),
    closed: Boolean = false,
    curveType: CurveType = CurveType.CENTRIPETAL,
    tension: Float = 0.5f
) : Curve() {
    var points: List<Vector3>
    var closed: Boolean
    var curveType: CurveType
    var tension: Float

    override fun getPoint(t: Float): Vector3
}

/**
 * FR-CR004: Cubic Bézier curve
 *
 * Test Contract:
 * - MUST interpolate from v0 to v3
 * - MUST use v1 and v2 as control points
 * - MUST be smooth and continuous
 */
class CubicBezierCurve3(
    v0: Vector3,
    v1: Vector3,
    v2: Vector3,
    v3: Vector3
) : Curve() {
    var v0: Vector3
    var v1: Vector3
    var v2: Vector3
    var v3: Vector3

    override fun getPoint(t: Float): Vector3
}

/**
 * FR-CR005: Quadratic Bézier curve
 *
 * Test Contract:
 * - MUST interpolate from v0 to v2
 * - MUST use v1 as control point
 */
class QuadraticBezierCurve3(
    v0: Vector3,
    v1: Vector3,
    v2: Vector3
) : Curve() {
    var v0: Vector3
    var v1: Vector3
    var v2: Vector3

    override fun getPoint(t: Float): Vector3
}

/**
 * FR-CR006: Straight line segment
 *
 * Test Contract:
 * - MUST interpolate linearly between v1 and v2
 */
class LineCurve3(v1: Vector3, v2: Vector3) : Curve() {
    var v1: Vector3
    var v2: Vector3

    override fun getPoint(t: Float): Vector3
}

/**
 * FR-CR007: Ellipse curve (2D)
 *
 * Test Contract:
 * - MUST trace ellipse from aStartAngle to aEndAngle
 * - MUST support clockwise and counter-clockwise
 * - MUST support full ellipse (not just circular arc)
 */
class EllipseCurve(
    aX: Float = 0f,
    aY: Float = 0f,
    xRadius: Float = 1f,
    yRadius: Float = 1f,
    aStartAngle: Float = 0f,
    aEndAngle: Float = Math.PI.toFloat() * 2f,
    aClockwise: Boolean = false,
    aRotation: Float = 0f
) : Curve() {
    var aX: Float
    var aY: Float
    var xRadius: Float
    var yRadius: Float
    var aStartAngle: Float
    var aEndAngle: Float
    var aClockwise: Boolean
    var aRotation: Float

    override fun getPoint(t: Float): Vector3
}

/**
 * FR-CR008: Spline curve through points
 *
 * Test Contract:
 * - MUST create smooth curve through all points
 * - MUST support closed splines
 */
class SplineCurve(points: List<Vector2> = emptyList()) : Curve() {
    var points: List<Vector2>

    override fun getPoint(t: Float): Vector3
}

/**
 * FR-CR009, FR-CR010: Composite curve (CurvePath)
 *
 * Test Contract:
 * - MUST combine multiple curves
 * - MUST provide continuous path
 * - MUST handle arc length correctly across curves
 */
open class CurvePath : Curve() {
    val curves: MutableList<Curve> = mutableListOf()
    var autoClose: Boolean = false

    /**
     * Add curve to path
     */
    fun add(curve: Curve)

    /**
     * Close path with line segment
     */
    fun closePath()

    override fun getPoint(t: Float): Vector3

    /**
     * Get curve divisions as lines
     */
    fun getCurveLengths(): List<Float>
}

/**
 * FR-CR011: 2D path with lines and curves
 *
 * Test Contract:
 * - MUST support moveTo, lineTo, bezierCurveTo, quadraticCurveTo, arc
 * - MUST track current point
 * - MUST support closed paths
 */
class Path(points: List<Vector2> = emptyList()) : CurvePath() {
    var currentPoint: Vector2

    /**
     * Move to point without drawing
     */
    fun moveTo(x: Float, y: Float): Path

    /**
     * Draw line to point
     */
    fun lineTo(x: Float, y: Float): Path

    /**
     * Draw quadratic Bézier curve
     */
    fun quadraticCurveTo(aCPx: Float, aCPy: Float, aX: Float, aY: Float): Path

    /**
     * Draw cubic Bézier curve
     */
    fun bezierCurveTo(
        aCP1x: Float, aCP1y: Float,
        aCP2x: Float, aCP2y: Float,
        aX: Float, aY: Float
    ): Path

    /**
     * Draw arc
     */
    fun arc(
        aX: Float, aY: Float,
        aRadius: Float,
        aStartAngle: Float, aEndAngle: Float,
        aClockwise: Boolean = false
    ): Path

    /**
     * Draw full circle
     */
    fun absarc(
        aX: Float, aY: Float,
        aRadius: Float,
        aStartAngle: Float, aEndAngle: Float,
        aClockwise: Boolean = false
    ): Path

    /**
     * Draw ellipse
     */
    fun ellipse(
        aX: Float, aY: Float,
        xRadius: Float, yRadius: Float,
        aStartAngle: Float, aEndAngle: Float,
        aClockwise: Boolean = false,
        aRotation: Float = 0f
    ): Path

    /**
     * Get points as 2D vectors
     */
    fun getPoints2D(divisions: Int = 12): List<Vector2>
}

/**
 * FR-CR012, FR-CR013: 3D tube geometry from curve
 *
 * Test Contract:
 * - MUST extrude 2D shape along 3D curve
 * - MUST compute Frenet frames for orientation
 * - MUST support custom radial segments and tube segments
 */
class TubeGeometry(
    path: Curve,
    tubularSegments: Int = 64,
    radius: Float = 1f,
    radialSegments: Int = 8,
    closed: Boolean = false
) {
    val parameters: TubeGeometryParameters
    val tangents: List<Vector3>
    val normals: List<Vector3>
    val binormals: List<Vector3>

    data class TubeGeometryParameters(
        val path: Curve,
        val tubularSegments: Int,
        val radius: Float,
        val radialSegments: Int,
        val closed: Boolean
    )
}

/**
 * FR-CR014: Frenet frame computation
 *
 * Test Contract:
 * - MUST compute tangent, normal, binormal frames
 * - MUST minimize twist along curve
 * - MUST handle curve discontinuities
 */
class FrenetFrames(
    curve: Curve,
    segments: Int,
    closed: Boolean
) {
    val tangents: List<Vector3>
    val normals: List<Vector3>
    val binormals: List<Vector3>

    /**
     * Compute frames at parameter t
     */
    fun computeFrenetFrames(): FrenetFrames
}

/**
 * FR-CR015: Curve interpolation types
 */
enum class CurveType {
    /**
     * Centripetal parameterization (default)
     * Best for general use, avoids loops and self-intersections
     */
    CENTRIPETAL,

    /**
     * Chordal parameterization
     * Similar to centripetal, slightly different tension
     */
    CHORDAL,

    /**
     * Uniform parameterization
     * Equal spacing between points, can produce loops
     */
    CATMULLROM
}

/**
 * Curve utilities
 */
object CurveUtils {
    /**
     * Interpolate Catmull-Rom spline
     */
    fun interpolate(
        p0: Float, p1: Float, p2: Float, p3: Float,
        t: Float, type: CurveType
    ): Float

    /**
     * Compute curve tangent using finite differences
     */
    fun computeTangent(curve: Curve, t: Float, delta: Float = 0.0001f): Vector3

    /**
     * Compute curve normal (perpendicular to tangent)
     */
    fun computeNormal(curve: Curve, t: Float): Vector3

    /**
     * Adaptive tessellation based on curvature
     *
     * Test Contract:
     * - MUST add more points in high-curvature regions
     * - MUST use fewer points in straight sections
     * - MUST meet curvature tolerance threshold
     */
    fun adaptiveTessellate(
        curve: Curve,
        curvatureTolerance: Float = 0.01f,
        minSegments: Int = 4,
        maxSegments: Int = 256
    ): List<Vector3>
}