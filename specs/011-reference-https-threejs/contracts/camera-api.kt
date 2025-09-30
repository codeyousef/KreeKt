/**
 * Advanced Camera API Contract
 * Maps FR-C001 through FR-C006
 *
 * Constitutional Requirements:
 * - Type-safe camera implementations
 * - Cross-platform render target support
 * - Performance: <10ms per camera update
 */

package io.kreekt.camera

import io.kreekt.core.Object3D
import io.kreekt.math.Vector3
import io.kreekt.renderer.WebGPURenderTarget
import io.kreekt.scene.Scene

/**
 * FR-C001, FR-C002: Cube camera for environment mapping
 *
 * Test Contract:
 * - MUST render to 6 faces of cube map
 * - MUST support dynamic environment capture
 * - MUST integrate with PBR materials
 * - MUST handle near/far planes correctly
 */
class CubeCamera(
    near: Float = 0.1f,
    far: Float = 1000f,
    renderTarget: WebGPUCubeRenderTarget
) : Object3D() {
    val renderTarget: WebGPUCubeRenderTarget
    var near: Float
    var far: Float

    // 6 perspective cameras for each cube face
    val cameraPX: PerspectiveCamera
    val cameraNX: PerspectiveCamera
    val cameraPY: PerspectiveCamera
    val cameraNY: PerspectiveCamera
    val cameraPZ: PerspectiveCamera
    val cameraNZ: PerspectiveCamera

    /**
     * Update cube camera and render scene to all 6 faces
     *
     * Test Contract:
     * - MUST render scene 6 times (once per face)
     * - MUST update camera orientation for each face
     * - MUST bind correct render target for each face
     */
    fun update(renderer: WebGPURenderer, scene: Scene)

    /**
     * Clear render target
     */
    fun clear(renderer: WebGPURenderer, color: Boolean = true, depth: Boolean = true, stencil: Boolean = true)
}

/**
 * FR-C003, FR-C004: Stereo camera for VR/3D rendering
 *
 * Test Contract:
 * - MUST maintain two cameras with eye separation
 * - MUST compute correct view matrices for left/right eyes
 * - MUST support adjustable eye separation
 */
class StereoCamera {
    val cameraL: PerspectiveCamera
    val cameraR: PerspectiveCamera

    var eyeSep: Float  // Inter-pupillary distance (default: 0.064)

    /**
     * Update stereo cameras from base camera
     *
     * Test Contract:
     * - MUST offset left/right cameras by eyeSep/2
     * - MUST maintain convergence at specified distance
     * - MUST preserve FOV and aspect ratio
     */
    fun update(camera: PerspectiveCamera)
}

/**
 * FR-C005, FR-C006: Array of cameras for multi-view rendering
 *
 * Test Contract:
 * - MUST support multiple cameras with viewport regions
 * - MUST render scene once per camera
 * - MUST handle viewport bounds correctly
 */
class ArrayCamera(cameras: List<PerspectiveCamera> = emptyList()) : PerspectiveCamera() {
    val cameras: MutableList<PerspectiveCamera>

    /**
     * Add camera with viewport bounds
     *
     * @param camera Camera to add
     * @param x Viewport X position (0-1)
     * @param y Viewport Y position (0-1)
     * @param width Viewport width (0-1)
     * @param height Viewport height (0-1)
     */
    fun addCamera(
        camera: PerspectiveCamera,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    )

    /**
     * Remove camera from array
     */
    fun removeCamera(camera: PerspectiveCamera)

    /**
     * Get camera at index
     */
    fun getCamera(index: Int): PerspectiveCamera
}

// Supporting types (assuming these exist in kreekt-camera module)
abstract class Camera : Object3D() {
    abstract val projectionMatrix: Matrix4
    abstract val projectionMatrixInverse: Matrix4
    abstract fun updateProjectionMatrix()
}

open class PerspectiveCamera(
    fov: Float = 50f,
    aspect: Float = 1f,
    near: Float = 0.1f,
    far: Float = 2000f
) : Camera() {
    var fov: Float
    var aspect: Float
    var near: Float
    var far: Float
    var zoom: Float = 1f

    override fun updateProjectionMatrix()
    fun setViewOffset(
        fullWidth: Float,
        fullHeight: Float,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    )
    fun clearViewOffset()
}

// Forward declarations for renderer types
expect class WebGPURenderer {
    fun render(scene: Scene, camera: Camera)
    fun setRenderTarget(target: WebGPURenderTarget?)
    fun clear(color: Boolean, depth: Boolean, stencil: Boolean)
}

expect class WebGPUCubeRenderTarget(
    size: Int,
    options: RenderTargetOptions = RenderTargetOptions()
) : WebGPURenderTarget {
    val texture: CubeTexture

    fun fromEquirectangularTexture(renderer: WebGPURenderer, texture: Texture): WebGPUCubeRenderTarget
}

data class RenderTargetOptions(
    val format: TextureFormat = TextureFormat.RGBA8,
    val type: TextureType = TextureType.UNSIGNED_BYTE,
    val anisotropy: Int = 1,
    val generateMipmaps: Boolean = false,
    val minFilter: TextureFilter = TextureFilter.LINEAR,
    val magFilter: TextureFilter = TextureFilter.LINEAR
)

enum class TextureFormat {
    RGBA8,
    RGBA16F,
    RGBA32F,
    DEPTH24_STENCIL8
}

enum class TextureType {
    UNSIGNED_BYTE,
    FLOAT,
    HALF_FLOAT
}

enum class TextureFilter {
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    LINEAR_MIPMAP_LINEAR
}

// Forward declarations
expect class CubeTexture
expect class Texture
expect class Matrix4