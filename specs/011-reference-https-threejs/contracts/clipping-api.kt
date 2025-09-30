/**
 * Clipping Planes API Contract
 * Maps FR-CP001 through FR-CP006
 *
 * Constitutional Requirements:
 * - Efficient clipping plane implementation
 * - Type-safe plane management
 * - Performance: <2ms clipping overhead per frame
 */

package io.kreekt.clipping

import io.kreekt.math.Plane
import io.kreekt.math.Vector3
import io.kreekt.math.Vector4
import io.kreekt.material.Material
import io.kreekt.renderer.WebGPURenderer

/**
 * FR-CP001, FR-CP002: Global clipping planes
 *
 * Test Contract:
 * - MUST clip geometry against arbitrary planes
 * - MUST support up to 8 simultaneous planes (platform limit)
 * - MUST work with all material types
 * - MUST preserve fragment depth
 */
interface ClippingSupport {
    /**
     * Global clipping planes applied to all objects
     */
    var clippingPlanes: List<Plane>?

    /**
     * Enable local clipping planes (per material)
     */
    var localClippingEnabled: Boolean

    /**
     * Get maximum number of clipping planes supported
     *
     * Test Contract:
     * - MUST query platform capabilities
     * - MUST return conservative limit
     */
    fun getMaxClippingPlanes(): Int
}

/**
 * FR-CP003, FR-CP004: Per-material clipping planes
 *
 * Test Contract:
 * - MUST override global clipping planes
 * - MUST support clip union and intersection
 * - MUST integrate with material shader
 */
interface MaterialClipping {
    /**
     * Clipping planes specific to this material
     * Overrides global clipping planes if set
     */
    var clippingPlanes: List<Plane>?

    /**
     * Clip to union or intersection of planes
     * true = union (clip if outside ANY plane)
     * false = intersection (clip if outside ALL planes)
     */
    var clipIntersection: Boolean

    /**
     * Enable shadow clipping
     * true = clip shadows using same planes
     */
    var clipShadows: Boolean
}

/**
 * FR-CP005: Plane representation
 *
 * Test Contract:
 * - MUST define plane as normal + distance
 * - MUST provide point-plane distance calculation
 * - MUST support plane transformations
 */
data class Plane(
    var normal: Vector3 = Vector3(1f, 0f, 0f),
    var constant: Float = 0f
) {
    /**
     * Set plane from normal and coplanar point
     *
     * Test Contract:
     * - MUST normalize normal vector
     * - MUST compute constant from point
     */
    fun setFromNormalAndCoplanarPoint(normal: Vector3, point: Vector3): Plane

    /**
     * Set plane from three points
     *
     * Test Contract:
     * - MUST compute plane from point triangle
     * - MUST use right-hand rule for normal
     */
    fun setFromCoplanarPoints(a: Vector3, b: Vector3, c: Vector3): Plane

    /**
     * Compute signed distance from point to plane
     *
     * @return Positive = point in front, Negative = point behind
     *
     * Test Contract:
     * - MUST return dot(normal, point) + constant
     */
    fun distanceToPoint(point: Vector3): Float

    /**
     * Project point onto plane
     *
     * Test Contract:
     * - MUST return closest point on plane
     */
    fun projectPoint(point: Vector3, target: Vector3): Vector3

    /**
     * Negate plane (flip direction)
     */
    fun negate(): Plane

    /**
     * Normalize plane equation
     *
     * Test Contract:
     * - MUST ensure normal is unit length
     * - MUST scale constant accordingly
     */
    fun normalize(): Plane

    /**
     * Transform plane by matrix
     *
     * Test Contract:
     * - MUST apply rotation to normal
     * - MUST apply translation to constant
     */
    fun applyMatrix4(matrix: Matrix4, optionalNormalMatrix: Matrix3? = null): Plane

    /**
     * Check if point is on plane
     */
    fun containsPoint(point: Vector3, epsilon: Float = 0.0001f): Boolean

    /**
     * Clone plane
     */
    fun clone(): Plane
}

/**
 * FR-CP006: Clipping shader integration
 *
 * Test Contract:
 * - MUST generate platform-appropriate shader code
 * - MUST inject clipping uniforms
 * - MUST use hardware clipping where available
 */
object ClippingShaderGenerator {
    /**
     * Generate clipping uniform declarations
     *
     * @param numPlanes Number of clipping planes
     * @return WGSL uniform declarations
     *
     * Test Contract:
     * - MUST declare clipping plane array
     * - MUST include plane count uniform
     */
    fun generateUniforms(numPlanes: Int): String

    /**
     * Generate vertex shader clipping code
     *
     * Uses gl_ClipDistance for hardware clipping
     *
     * @param numPlanes Number of clipping planes
     * @return WGSL vertex shader code
     *
     * Test Contract:
     * - MUST compute clip distances in vertex shader
     * - MUST write to gl_ClipDistance array
     * - MUST handle numPlanes=0 (no clipping)
     */
    fun generateVertexCode(numPlanes: Int): String

    /**
     * Generate fragment shader clipping code
     *
     * Fallback for platforms without gl_ClipDistance
     *
     * @param numPlanes Number of clipping planes
     * @param clipIntersection Union or intersection mode
     * @return WGSL fragment shader code
     *
     * Test Contract:
     * - MUST compute point-plane distances
     * - MUST discard fragments based on clipIntersection mode
     * - Union: discard if ANY distance < 0
     * - Intersection: discard if ALL distances < 0
     */
    fun generateFragmentCode(
        numPlanes: Int,
        clipIntersection: Boolean = false
    ): String

    /**
     * Inject clipping code into shaders
     *
     * Test Contract:
     * - MUST preserve existing shader logic
     * - MUST insert vertex code before transformation
     * - MUST insert fragment code after material computation
     */
    fun injectClippingCode(
        vertexShader: String,
        fragmentShader: String,
        numPlanes: Int,
        useHardwareClipping: Boolean,
        clipIntersection: Boolean = false
    ): Pair<String, String>

    /**
     * Check if hardware clipping is available
     *
     * Test Contract:
     * - MUST query WebGPU/Vulkan capabilities
     * - MUST return conservative result
     */
    fun isHardwareClippingAvailable(renderer: WebGPURenderer): Boolean
}

/**
 * Clipping utilities
 */
object ClippingUtils {
    /**
     * Create plane from equation coefficients
     *
     * @param coefficients [A, B, C, D] for Ax + By + Cz + D = 0
     *
     * Test Contract:
     * - MUST set normal = (A, B, C)
     * - MUST set constant = D
     * - MUST normalize plane
     */
    fun fromEquation(coefficients: FloatArray): Plane

    /**
     * Create plane from point and normal
     */
    fun fromPointNormal(point: Vector3, normal: Vector3): Plane

    /**
     * Transform planes by matrix
     *
     * Test Contract:
     * - MUST apply transformation to all planes
     * - MUST compute normal matrix once
     */
    fun transformPlanes(
        planes: List<Plane>,
        matrix: Matrix4
    ): List<Plane>

    /**
     * Check if box is clipped by planes
     *
     * @return true if box should be clipped (outside all planes in intersection mode)
     *
     * Test Contract:
     * - MUST test box against all planes
     * - MUST respect clipIntersection mode
     * - MUST be used for frustum culling optimization
     */
    fun isBoxClipped(
        boxMin: Vector3,
        boxMax: Vector3,
        planes: List<Plane>,
        clipIntersection: Boolean
    ): Boolean

    /**
     * Check if sphere is clipped by planes
     */
    fun isSphereClipped(
        center: Vector3,
        radius: Float,
        planes: List<Plane>,
        clipIntersection: Boolean
    ): Boolean

    /**
     * Compute clipping plane from view-space parameters
     *
     * Useful for creating planes in camera space
     *
     * Test Contract:
     * - MUST transform to world space
     * - MUST account for camera projection
     */
    fun fromViewSpace(
        normal: Vector3,
        distance: Float,
        camera: Camera,
        target: Plane = Plane()
    ): Plane
}

/**
 * Clipping plane helpers for visualization
 */
class ClippingPlaneHelper(
    plane: Plane,
    size: Float = 1f,
    color: Color = Color(0xffff00)
) : Object3D() {
    var plane: Plane
    var size: Float
    var color: Color

    /**
     * Update helper visualization
     *
     * Test Contract:
     * - MUST show plane position and orientation
     * - MUST show clipping direction (normal)
     */
    fun update()
}

// Forward declarations
expect class Matrix4
expect class Matrix3
expect class Camera
expect class Object3D
expect class Color