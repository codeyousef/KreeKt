/**
 * Renderer API Contract
 * Defines the interface for cross-platform 3D rendering
 */
package kreekt.renderer

import kreekt.core.*
import kreekt.scene.*

/**
 * Main renderer interface for 3D scene rendering
 * Platform implementations: WebGPURenderer (JS), VulkanRenderer (JVM/Native)
 */
interface Renderer {
    /**
     * Initialize the renderer with the given canvas/surface
     */
    suspend fun initialize(surface: RenderSurface): RendererResult<Unit>

    /**
     * Render a scene from the perspective of a camera
     */
    suspend fun render(scene: Scene, camera: Camera): RendererResult<Unit>

    /**
     * Set the viewport size
     */
    fun setSize(width: Int, height: Int, updateStyle: Boolean = true)

    /**
     * Set the device pixel ratio for high-DPI displays
     */
    fun setPixelRatio(ratio: Float)

    /**
     * Get renderer capabilities and limits
     */
    val capabilities: RendererCapabilities

    /**
     * Get current rendering statistics
     */
    val info: RendererInfo

    /**
     * Dispose of all GPU resources
     */
    fun dispose()
}

/**
 * Platform-specific surface abstraction
 */
expect class RenderSurface

/**
 * Renderer capabilities query interface
 */
interface RendererCapabilities {
    val maxTextureSize: Int
    val maxCubeMapSize: Int
    val maxVertexUniforms: Int
    val maxFragmentUniforms: Int
    val maxVertexTextures: Int
    val maxFragmentTextures: Int
    val supportsInstancedArrays: Boolean
    val supportsVertexArrayObjects: Boolean
    val supportsFloatTextures: Boolean
    val supportsHalfFloatTextures: Boolean
    val supportsAnisotropicFiltering: Boolean
    val maxAnisotropy: Int
}

/**
 * Rendering statistics
 */
data class RendererInfo(
    val render: RenderStats,
    val memory: MemoryStats,
    val programs: ProgramStats
)

data class RenderStats(
    val frame: Int,
    val calls: Int,
    val triangles: Int,
    val points: Int,
    val lines: Int
)

data class MemoryStats(
    val geometries: Int,
    val textures: Int
)

data class ProgramStats(
    val programs: Int
)

/**
 * Result wrapper for async operations
 */
sealed class RendererResult<T> {
    data class Success<T>(val value: T) : RendererResult<T>()
    data class Error<T>(val exception: RendererException) : RendererResult<T>()
}

/**
 * Renderer-specific exceptions
 */
sealed class RendererException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InitializationFailed(message: String, cause: Throwable? = null) : RendererException(message, cause)
    class RenderingFailed(message: String, cause: Throwable? = null) : RendererException(message, cause)
    class UnsupportedOperation(message: String) : RendererException(message)
    class InvalidState(message: String) : RendererException(message)
}

/**
 * Render target abstraction for off-screen rendering
 */
interface RenderTarget {
    val width: Int
    val height: Int
    val texture: Texture
    val depthTexture: Texture?
    fun setSize(width: Int, height: Int)
    fun dispose()
}

/**
 * Post-processing effects pipeline
 */
interface EffectComposer {
    fun addPass(pass: Pass)
    fun removePass(pass: Pass)
    fun render(deltaTime: Float)
    fun setSize(width: Int, height: Int)
    fun dispose()
}

/**
 * Base class for rendering passes
 */
abstract class Pass {
    var enabled: Boolean = true
    var needsSwap: Boolean = true
    var clear: Boolean = false
    var renderToScreen: Boolean = false

    abstract fun render(
        renderer: Renderer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float,
        maskActive: Boolean
    )

    open fun setSize(width: Int, height: Int) {}
    open fun dispose() {}
}