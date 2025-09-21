package io.kreekt.renderer

import io.kreekt.core.math.*
import io.kreekt.core.scene.*
import io.kreekt.camera.Camera
import io.kreekt.camera.Viewport
import io.kreekt.renderer.TextureFormat
import kotlin.math.PI

/**
 * Core renderer interface for all platform-specific implementations.
 * Compatible with Three.js WebGLRenderer API patterns.
 *
 * Provides a unified interface for rendering 3D scenes (across * WebGPU), Vulkan, and other graphics APIs.
 */
interface Renderer {

    /**
     * Renderer capabilities and limits
     */
    val capabilities: RendererCapabilities

    /**
     * Current render target (null for default framebuffer)
     */
    var renderTarget: RenderTarget?

    /**
     * Automatically clear the frame before rendering
     */
    var autoClear: Boolean

    /**
     * Automatically clear color buffer
     */
    var autoClearColor: Boolean

    /**
     * Automatically clear depth buffer
     */
    var autoClearDepth: Boolean

    /**
     * Automatically clear stencil buffer
     */
    var autoClearStencil: Boolean

    /**
     * Clear color for color buffer
     */
    var clearColor: Color

    /**
     * Clear alpha value for color buffer
     */
    var clearAlpha: Float

    /**
     * Enable/disable shadow mapping
     */
    var shadowMap: ShadowMapSettings

    /**
     * Tone mapping type
     */
    var toneMapping: ToneMapping

    /**
     * Tone mapping exposure
     */
    var toneMappingExposure: Float

    /**
     * Current output color space
     */
    var outputColorSpace: ColorSpace

    /**
     * Enable/disable physically correct lighting
     */
    var physicallyCorrectLights: Boolean

    /**
     * Initialize the renderer with the given surface
     */
    suspend fun initialize(surface: RenderSurface): RendererResult<Unit>

    /**
     * Render a scene from a camera's perspective
     */
    fun render(scene: Scene, camera: Camera): RendererResult<Unit>

    /**
     * Set the size of the rendering area
     */
    fun setSize(width: Int, height: Int, updateStyle: Boolean = true): RendererResult<Unit>

    /**
     * Set the device pixel ratio
     */
    fun setPixelRatio(pixelRatio: Float): RendererResult<Unit>

    /**
     * Set the viewport within the render target
     */
    fun setViewport(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit>

    /**
     * Get the current viewport
     */
    fun getViewport(): Viewport

    /**
     * Set the scissor test area
     */
    fun setScissorTest(enable: Boolean): RendererResult<Unit>

    /**
     * Set the scissor area
     */
    fun setScissor(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit>

    /**
     * Clear the current render target
     */
    fun clear(color: Boolean = true, depth: Boolean = true, stencil: Boolean = true): RendererResult<Unit>

    /**
     * Clear the color buffer
     */
    fun clearColorBuffer(): RendererResult<Unit>

    /**
     * Clear the depth buffer
     */
    fun clearDepth(): RendererResult<Unit>

    /**
     * Clear the stencil buffer
     */
    fun clearStencil(): RendererResult<Unit>

    /**
     * Force a specific state update
     */
    fun resetState(): RendererResult<Unit>

    /**
     * Compile all materials in the scene for better performance
     */
    fun compile(scene: Scene, camera: Camera): RendererResult<Unit>

    /**
     * Dispose of renderer resources
     */
    fun dispose(): RendererResult<Unit>

    /**
     * Force context loss (for testing/debugging)
     */
    fun forceContextLoss(): RendererResult<Unit>

    /**
     * Check if context is lost
     */
    fun isContextLost(): Boolean

    /**
     * Get rendering statistics
     */
    fun getStats(): RenderStats

    /**
     * Reset rendering statistics
     */
    fun resetStats()
}

/**
 * Render target for off-screen rendering
 */
interface RenderTarget {
    val width: Int
    val height: Int
    val texture: Texture?
    val depthTexture: Texture?
    val stencilBuffer: Boolean
    val depthBuffer: Boolean
}

/**
 * Shadow mapping settings
 */
data class ShadowMapSettings(
    var enabled: Boolean = false,
    var type: ShadowMapType = ShadowMapType.PCF,
    var autoUpdate: Boolean = true,
    var needsUpdate: Boolean = false
)

/**
 * Shadow map types
 */
enum class ShadowMapType {
    BASIC,
    PCF,
    PCF_SOFT,
    VSM
}

/**
 * Tone mapping types
 */
enum class ToneMapping {
    NONE,
    LINEAR,
    REINHARD,
    CINEON,
    ACES_FILMIC
}

/**
 * Color spaces
 */
enum class ColorSpace {
    sRGB,
    LINEAR_sRGB,
    REC_2020,
    P3,
    sRGB_LINEAR
}

/**
 * Rendering statistics
 */
data class RenderStats(
    val frame: Int = 0,
    val calls: Int = 0,
    val triangles: Int = 0,
    val points: Int = 0,
    val lines: Int = 0,
    val geometries: Int = 0,
    val textures: Int = 0,
    val shaders: Int = 0,
    val programs: Int = 0,
    val memory: MemoryStats = MemoryStats()
)

/**
 * Memory usage statistics
 */
data class MemoryStats(
    val geometries: Long = 0,
    val textures: Long = 0,
    val programs: Long = 0
)

/**
 * Renderer result type for error handling
 */
sealed class RendererResult<T> {
    data class Success<T>(val value: T) : RendererResult<T>()
    data class Error<T>(val exception: RendererException) : RendererResult<T>()

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Error -> throw exception
    }

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Error -> null
    }

    fun onSuccess(action: (T) -> Unit): RendererResult<T> {
        if (this is Success) action(value)
        return this
    }

    fun onError(action: (RendererException) -> Unit): RendererResult<T> {
        if (this is Error) action(exception)
        return this
    }

    fun <R> map(transform: (T) -> R): RendererResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Error -> Error(exception)
    }
}

/**
 * Renderer-specific exceptions
 */
sealed class RendererException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InitializationFailed(message: String, cause: Throwable? = null) : RendererException(message, cause)
    class RenderingFailed(message: String, cause: Throwable? = null) : RendererException(message, cause)
    class ResourceCreationFailed(message: String, cause: Throwable? = null) : RendererException(message, cause)
    class InvalidState(message: String) : RendererException(message)
    class UnsupportedFeature(feature: String) : RendererException("Unsupported feature: $feature")
    class ContextLost(message: String = "Rendering context lost") : RendererException(message)
    class OutOfMemory(message: String) : RendererException(message)
}

/**
 * Renderer configuration options
 */
data class RendererConfig(
    val antialias: Boolean = false,
    val alpha: Boolean = false,
    val premultipliedAlpha: Boolean = true,
    val preserveDrawingBuffer: Boolean = false,
    val powerPreference: PowerPreference = PowerPreference.DEFAULT,
    val stencil: Boolean = true,
    val depth: Boolean = true,
    val logarithmicDepthBuffer: Boolean = false,
    val precision: Precision = Precision.HIGHP,
    val debug: Boolean = false
)

/**
 * Power preference for GPU selection
 */
enum class PowerPreference {
    DEFAULT,
    HIGH_PERFORMANCE,
    LOW_POWER
}

/**
 * Precision settings
 */
enum class Precision {
    LOWP,
    MEDIUMP,
    HIGHP
}

/**
 * Render pass information
 */
data class RenderPass(
    val name: String,
    val enabled: Boolean = true,
    val clear: Boolean = true,
    val renderToScreen: Boolean = false,
    val needsSwap: Boolean = true
)

/**
 * Renderer factory interface
 */
interface RendererFactory {
    /**
     * Creates a renderer for the current platform
     */
    suspend fun createRenderer(config: RendererConfig = RendererConfig()): RendererResult<Renderer>

    /**
     * Checks if renderer is supported on current platform
     */
    fun isSupported(): Boolean

    /**
     * Gets renderer capabilities before creation
     */
    suspend fun getCapabilities(): RendererCapabilities?
}

/**
 * Global renderer utilities
 */
object RendererUtils {

    /**
     * Calculates optimal render scale for performance
     */
    fun getOptimalRenderScale(devicePixelRatio: Float, capabilities: RendererCapabilities): Float {
        return when {
            capabilities.maxTextureSize < 2048 -> 0.5f
            capabilities.maxTextureSize < 4096 -> 0.75f
            devicePixelRatio > 2f -> 0.8f
            else -> 1f
        }.coerceAtMost(devicePixelRatio)
    }

    /**
     * Estimates memory usage for a render target
     */
    fun estimateRenderTargetMemory(width: Int, height: Int, hasDepth: Boolean, samples: Int = 1): Long {
        val colorBuffer = width * height * 4L * samples // TextureFormat.RGBA8
        val depthBuffer = if (hasDepth) width * height * 4L * samples else 0L // 32-bit depth
        return colorBuffer + depthBuffer
    }

    /**
     * Validates render target dimensions
     */
    fun validateRenderTargetSize(width: Int, height: Int, capabilities: RendererCapabilities): Boolean {
        return width > 0 && height > 0 &&
               width <= capabilities.maxTextureSize &&
               height <= capabilities.maxTextureSize
    }

    /**
     * Gets recommended MSAA samples
     */
    fun getRecommendedMSAA(capabilities: RendererCapabilities): Int {
        return when {
            capabilities.maxSamples >= 8 -> 4
            capabilities.maxSamples >= 4 -> 4
            capabilities.maxSamples >= 2 -> 2
            else -> 1
        }
    }
}

/**
 * Extension functions for Renderer
 */

/**
 * Render with automatic error handling
 */
fun Renderer.renderSafe(scene: Scene, camera: Camera, onError: (RendererException) -> Unit = {}): Boolean {
    return render(scene, camera).onError(onError) is RendererResult.Success
}

/**
 * Set clear color from hex
 */
fun Renderer.setClearColor(hex: Int, alpha: Float = 1f) {
    clearColor.setHex(hex)
    clearAlpha = alpha
}

/**
 * Set clear color from RGB
 */
fun Renderer.setClearColor(r: Float, g: Float, b: Float, alpha: Float = 1f) {
    clearColor.set(r, g, b)
    clearAlpha = alpha
}

/**
 * Enable shadows with default settings
 */
fun Renderer.enableShadows(type: ShadowMapType = ShadowMapType.PCF) {
    shadowMap = shadowMap.copy(enabled = true, type = type)
}

/**
 * Disable shadows
 */
fun Renderer.disableShadows() {
    shadowMap = shadowMap.copy(enabled = false)
}

/**
 * Check if feature is supported
 */
fun Renderer.supports(feature: String): Boolean {
    return capabilities.supports(feature)
}