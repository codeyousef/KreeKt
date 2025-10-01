/**
 * Helper Objects API Contract
 * Maps FR-H001 through FR-H015
 *
 * Constitutional Requirements:
 * - Helpers for debugging and visualization only
 * - Minimal performance impact (<1ms per helper per frame)
 * - Not included in production builds by default
 */

package io.kreekt.helper

import io.kreekt.core.Object3D
import io.kreekt.math.Vector3
import io.kreekt.math.Color
import io.kreekt.camera.Camera
import io.kreekt.light.Light
import io.kreekt.light.SpotLight
import io.kreekt.light.DirectionalLight
import io.kreekt.geometry.BufferGeometry
import io.kreekt.material.Material

/**
 * FR-H001: Display world axes (X=red, Y=green, Z=blue)
 *
 * Test Contract:
 * - MUST render three colored lines for axes
 * - MUST be visible in scene
 * - MUST have configurable size
 */
class AxesHelper(size: Float = 1f) : Object3D() {
    var size: Float

    fun setColors(xColor: Color, yColor: Color, zColor: Color)
    fun update()
}

/**
 * FR-H002: Display grid on a plane
 *
 * Test Contract:
 * - MUST render grid with configurable divisions
 * - MUST support custom colors
 * - MUST align to world plane (XZ by default)
 */
class GridHelper(
    size: Float = 10f,
    divisions: Int = 10,
    colorCenterLine: Color = Color(0x444444),
    colorGrid: Color = Color(0x888888)
) : Object3D() {
    var size: Float
    var divisions: Int
    var colorCenterLine: Color
    var colorGrid: Color

    fun update()
}

/**
 * FR-H003: Display wireframe box around object
 *
 * Test Contract:
 * - MUST compute bounding box from object
 * - MUST update when object transforms
 * - MUST render as wireframe lines
 */
class BoxHelper(
    obj: Object3D,
    color: Color = Color(0xffff00)
) : Object3D() {
    var color: Color
    val obj: Object3D

    fun update()
    fun setFromObject(obj: Object3D): BoxHelper
}

/**
 * FR-H004: Display wireframe box from explicit bounds
 *
 * Test Contract:
 * - MUST create box from min/max vectors
 * - MUST be independent of scene objects
 */
class Box3Helper(
    box: Box3,
    color: Color = Color(0xffff00)
) : Object3D() {
    var box: Box3
    var color: Color

    fun update()
}

/**
 * FR-H005: Display camera frustum
 *
 * Test Contract:
 * - MUST show near/far planes
 * - MUST show frustum edges
 * - MUST update with camera changes
 */
class CameraHelper(camera: Camera) : Object3D() {
    val camera: Camera
    var pointMap: Map<String, List<Int>>

    fun update()
}

/**
 * FR-H006: Display directional light direction
 *
 * Test Contract:
 * - MUST show light position and target
 * - MUST render direction line
 * - MUST update with light changes
 */
class DirectionalLightHelper(
    light: DirectionalLight,
    size: Float = 1f,
    color: Color? = null
) : Object3D() {
    val light: DirectionalLight
    var size: Float
    var color: Color?

    fun update()
}

/**
 * FR-H007: Display spotlight cone
 *
 * Test Contract:
 * - MUST show cone shape based on angle
 * - MUST show light position and target
 * - MUST update with light changes
 */
class SpotLightHelper(light: SpotLight, color: Color? = null) : Object3D() {
    val light: SpotLight
    var color: Color?

    fun update()
}

/**
 * FR-H008: Display point light range
 *
 * Test Contract:
 * - MUST show spherical range indicator
 * - MUST update with light changes
 */
class PointLightHelper(
    light: Light,
    sphereSize: Float = 1f,
    color: Color? = null
) : Object3D() {
    val light: Light
    var sphereSize: Float
    var color: Color?

    fun update()
}

/**
 * FR-H009: Display hemisphere light colors
 *
 * Test Contract:
 * - MUST show sky and ground colors
 * - MUST render as split sphere
 */
class HemisphereLightHelper(
    light: Light,
    size: Float = 1f
) : Object3D() {
    val light: Light
    var size: Float

    fun update()
}

/**
 * FR-H010: Display skeleton bones
 *
 * Test Contract:
 * - MUST show bone hierarchy
 * - MUST update with skeleton animation
 */
class SkeletonHelper(obj: Object3D) : Object3D() {
    val bones: List<Object3D>
    val root: Object3D

    fun update()
    fun getBoneList(obj: Object3D): List<Object3D>
}

/**
 * FR-H011: Display arrow from origin to direction
 *
 * Test Contract:
 * - MUST render line and cone arrowhead
 * - MUST support custom colors and sizes
 */
class ArrowHelper(
    dir: Vector3,
    origin: Vector3 = Vector3(0f, 0f, 0f),
    length: Float = 1f,
    color: Color = Color(0xffff00),
    headLength: Float = length * 0.2f,
    headWidth: Float = headLength * 0.2f
) : Object3D() {
    var direction: Vector3
    var origin: Vector3
    var length: Float
    var color: Color

    fun setDirection(dir: Vector3)
    fun setLength(length: Float, headLength: Float? = null, headWidth: Float? = null)
    fun setColor(color: Color)
}

/**
 * FR-H012: Display plane helper
 *
 * Test Contract:
 * - MUST show plane position and normal
 * - MUST be visible from both sides
 */
class PlaneHelper(
    plane: Plane,
    size: Float = 1f,
    color: Color = Color(0xffff00)
) : Object3D() {
    var plane: Plane
    var size: Float
    var color: Color

    fun update()
}

/**
 * FR-H013: Display vertex normals
 *
 * Test Contract:
 * - MUST render arrows for each vertex normal
 * - MUST update with geometry changes
 */
class VertexNormalsHelper(
    obj: Object3D,
    size: Float = 1f,
    color: Color = Color(0xff0000)
) : Object3D() {
    val obj: Object3D
    var size: Float
    var color: Color

    fun update()
}

/**
 * FR-H014: Display face normals
 *
 * Test Contract:
 * - MUST render arrows for each face normal
 * - MUST compute from face vertices
 */
class FaceNormalsHelper(
    obj: Object3D,
    size: Float = 1f,
    color: Color = Color(0x0000ff)
) : Object3D() {
    val obj: Object3D
    var size: Float
    var color: Color

    fun update()
}

/**
 * FR-H015: Display polar grid
 *
 * Test Contract:
 * - MUST render radial and circular divisions
 * - MUST support custom colors
 */
class PolarGridHelper(
    radius: Float = 10f,
    radialDivisions: Int = 16,
    circularDivisions: Int = 8,
    divisions: Int = 64,
    color1: Color = Color(0x444444),
    color2: Color = Color(0x888888)
) : Object3D() {
    var radius: Float
    var radialDivisions: Int
    var circularDivisions: Int

    fun update()
}

// Supporting types
data class Box3(
    var min: Vector3 = Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    var max: Vector3 = Vector3(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
)

data class Plane(
    var normal: Vector3 = Vector3(1f, 0f, 0f),
    var constant: Float = 0f
)