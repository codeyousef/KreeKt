/**
 * Helper Objects Contracts
 *
 * Feature: 013-double-check-and (Three.js r180 Feature Parity)
 * Category: Visualization Helpers
 *
 * Requirements covered: FR-043 through FR-050
 */

package io.kreekt.helpers

import io.kreekt.core.math.Color
import io.kreekt.core.math.Vector2
import io.kreekt.core.scene.Object3D
import io.kreekt.geometry.LineSegments

/**
 * Base class for visualization helpers.
 */
abstract class Helper : LineSegments() {
    abstract fun update()
    open fun dispose() {}
}

/**
 * Visualizes vertex normals.
 *
 * FR-043: System MUST provide VertexNormalsHelper for visualizing vertex normals
 *
 * @sample
 * ```kotlin
 * val helper = VertexNormalsHelper(mesh, size = 1.0f, color = Color(0xff0000))
 * scene.add(helper)
 * ```
 */
class VertexNormalsHelper(
    val targetObject: Object3D,
    var size: Float = 1.0f,
    var color: Color = Color(0xff0000)
) : Helper() {
    override fun update()
}

/**
 * Visualizes vertex tangents.
 *
 * FR-044: System MUST provide VertexTangentsHelper for visualizing vertex tangents
 */
class VertexTangentsHelper(
    val targetObject: Object3D,
    var size: Float = 1.0f,
    var color: Color = Color(0x00ff00)
) : Helper() {
    override fun update()
}

/**
 * Visualizes face normals.
 *
 * FR-045: System MUST provide FaceNormalsHelper for visualizing face normals
 */
class FaceNormalsHelper(
    val targetObject: Object3D,
    var size: Float = 1.0f,
    var color: Color = Color(0xffaa00)
) : Helper() {
    override fun update()
}

/**
 * Visualizes coordinate axes (XYZ).
 *
 * FR-046: System MUST provide AxesHelper for visualizing coordinate axes
 *
 * @sample
 * ```kotlin
 * val axes = AxesHelper(size = 5.0f)
 * scene.add(axes)
 * ```
 */
class AxesHelper(
    var size: Float = 1.0f
) : LineSegments() {
    // X axis: red, Y axis: green, Z axis: blue
}

/**
 * Visualizes a grid in the XZ plane.
 *
 * FR-047: System MUST provide GridHelper and PolarGridHelper for reference grids
 *
 * @sample
 * ```kotlin
 * val grid = GridHelper(
 *     size = 10.0f,
 *     divisions = 10,
 *     colorCenterLine = Color(0x444444),
 *     colorGrid = Color(0x888888)
 * )
 * scene.add(grid)
 * ```
 */
class GridHelper(
    var size: Float = 10.0f,
    var divisions: Int = 10,
    var colorCenterLine: Color = Color(0x444444),
    var colorGrid: Color = Color(0x888888)
) : LineSegments()

/**
 * Visualizes a polar grid.
 *
 * FR-047: System MUST provide GridHelper and PolarGridHelper for reference grids
 */
class PolarGridHelper(
    var radius: Float = 10.0f,
    var radials: Int = 16,
    var circles: Int = 8,
    var divisions: Int = 64,
    var color1: Color = Color(0x444444),
    var color2: Color = Color(0x888888)
) : LineSegments()

/**
 * Visualizes a mathematical plane.
 *
 * FR-048: System MUST provide PlaneHelper for visualizing mathematical planes
 */
class PlaneHelper(
    val plane: io.kreekt.core.math.Plane,
    var size: Float = 1.0f,
    var color: Color = Color(0xffff00)
) : LineSegments() {
    fun update()
}

/**
 * Visualizes a camera frustum.
 *
 * FR-050: System MUST provide CameraHelper for visualizing camera frustums
 *
 * @sample
 * ```kotlin
 * val cameraHelper = CameraHelper(perspectiveCamera)
 * scene.add(cameraHelper)
 * ```
 */
class CameraHelper(
    val camera: io.kreekt.camera.Camera
) : LineSegments() {
    fun update()
}

// Light helpers (FR-049)

/**
 * Visualizes a directional light.
 */
class DirectionalLightHelper(
    val light: io.kreekt.lighting.DirectionalLight,
    var size: Float = 1.0f,
    var color: Color? = null
) : Object3D() {
    fun update()
    fun dispose()
}

/**
 * Visualizes a point light.
 */
class PointLightHelper(
    val light: io.kreekt.lighting.PointLight,
    var sphereSize: Float = 1.0f,
    var color: Color? = null
) : Object3D() {
    fun update()
    fun dispose()
}

/**
 * Visualizes a spot light.
 */
class SpotLightHelper(
    val light: io.kreekt.lighting.SpotLight,
    var color: Color? = null
) : Object3D() {
    fun update()
    fun dispose()
}

/**
 * Visualizes a hemisphere light.
 */
class HemisphereLightHelper(
    val light: io.kreekt.lighting.HemisphereLight,
    var size: Float = 1.0f,
    var color: Color? = null
) : Object3D() {
    fun update()
    fun dispose()
}
