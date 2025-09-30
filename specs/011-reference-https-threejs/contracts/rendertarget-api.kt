/**
 * Render Target API Contract
 * Maps FR-RT001 through FR-RT010
 *
 * Constitutional Requirements:
 * - Efficient off-screen rendering
 * - Type-safe render target management
 * - Performance: <5ms render target switch
 */

package io.kreekt.rendertarget

import io.kreekt.texture.Texture
import io.kreekt.texture.CubeTexture
import io.kreekt.texture.TextureFormat
import io.kreekt.texture.TextureDataType
import io.kreekt.texture.TextureFilter
import io.kreekt.texture.TextureWrapping
import io.kreekt.math.Vector2
import io.kreekt.math.Vector4

/**
 * FR-RT001, FR-RT002: Render target for off-screen rendering
 *
 * Test Contract:
 * - MUST create framebuffer with attachments
 * - MUST support color and depth/stencil attachments
 * - MUST be usable as texture
 * - MUST support multisampling (MSAA)
 */
expect open class WebGPURenderTarget(
    width: Int = 256,
    height: Int = 256,
    options: RenderTargetOptions = RenderTargetOptions()
) {
    var width: Int
    var height: Int

    /**
     * Rendered texture output
     */
    val texture: Texture

    /**
     * Depth/stencil attachment texture
     */
    var depthTexture: DepthTexture?

    /**
     * Depth buffer enabled
     */
    var depthBuffer: Boolean

    /**
     * Stencil buffer enabled
     */
    var stencilBuffer: Boolean

    /**
     * Number of MSAA samples (1 = no MSAA, 4 = 4x MSAA)
     */
    var samples: Int

    /**
     * Scissor test region
     */
    var scissor: Vector4
    var scissorTest: Boolean

    /**
     * Viewport region
     */
    var viewport: Vector4

    /**
     * FR-RT003: Resize render target
     *
     * @param width New width
     * @param height New height
     *
     * Test Contract:
     * - MUST recreate framebuffer
     * - MUST resize attachments
     * - MUST preserve options
     * - MUST dispose old resources
     */
    fun setSize(width: Int, height: Int)

    /**
     * Clone render target
     */
    fun clone(): WebGPURenderTarget

    /**
     * Dispose render target resources
     *
     * Test Contract:
     * - MUST release GPU resources
     * - MUST dispose textures
     */
    fun dispose()
}

/**
 * FR-RT004, FR-RT005: Multi-target rendering (MRT)
 *
 * Test Contract:
 * - MUST support multiple color attachments
 * - MUST allow shader output to gl_FragData[N]
 * - MUST respect platform limits (usually 4-8 targets)
 */
expect class WebGPUMultipleRenderTargets(
    width: Int = 256,
    height: Int = 256,
    count: Int = 2,
    options: RenderTargetOptions = RenderTargetOptions()
) : WebGPURenderTarget {
    /**
     * Array of output textures
     */
    val texture: Array<Texture>

    /**
     * Get texture at index
     */
    fun getTexture(index: Int): Texture

    /**
     * Set texture at index
     */
    fun setTexture(index: Int, texture: Texture)
}

/**
 * FR-RT006, FR-RT007: Cube render target for environment maps
 *
 * Test Contract:
 * - MUST render to 6 cube faces
 * - MUST be usable as CubeTexture
 * - MUST support dynamic environment capture
 */
expect class WebGPUCubeRenderTarget(
    size: Int = 256,
    options: RenderTargetOptions = RenderTargetOptions()
) : WebGPURenderTarget {
    /**
     * Cube texture output
     */
    override val texture: CubeTexture

    /**
     * FR-RT008: Convert from equirectangular texture
     *
     * @param renderer Renderer to use
     * @param texture Equirectangular source
     * @return Self for chaining
     *
     * Test Contract:
     * - MUST render equirect to 6 cube faces
     * - MUST preserve HDR data
     * - MUST generate mipmaps if requested
     */
    fun fromEquirectangularTexture(
        renderer: WebGPURenderer,
        texture: Texture
    ): WebGPUCubeRenderTarget

    /**
     * Clear all cube faces
     */
    fun clear(
        renderer: WebGPURenderer,
        color: Boolean = true,
        depth: Boolean = true,
        stencil: Boolean = true
    )
}

/**
 * FR-RT009: 3D render target for volumetric rendering
 *
 * Test Contract:
 * - MUST support 3D texture output
 * - MUST render to slices
 * - MUST be usable for volumetric effects
 */
expect class WebGPU3DRenderTarget(
    width: Int = 256,
    height: Int = 256,
    depth: Int = 256,
    options: RenderTargetOptions = RenderTargetOptions()
) : WebGPURenderTarget {
    var depth: Int

    /**
     * 3D texture output
     */
    override val texture: DataTexture3D

    /**
     * Render to specific depth slice
     */
    fun setDepthSlice(slice: Int)
}

/**
 * FR-RT010: Array render target for texture arrays
 *
 * Test Contract:
 * - MUST support 2D texture array output
 * - MUST render to layers
 * - MUST be indexed in shaders
 */
expect class WebGPUArrayRenderTarget(
    width: Int = 256,
    height: Int = 256,
    depth: Int = 1,
    options: RenderTargetOptions = RenderTargetOptions()
) : WebGPURenderTarget {
    var depth: Int

    /**
     * 2D array texture output
     */
    override val texture: DataTexture2DArray

    /**
     * Render to specific array layer
     */
    fun setArrayLayer(layer: Int)
}

/**
 * Render target configuration options
 */
data class RenderTargetOptions(
    /**
     * Texture format (RGBA, RGB, etc.)
     */
    val format: TextureFormat = TextureFormat.RGBA,

    /**
     * Texture data type (UNSIGNED_BYTE, FLOAT, etc.)
     */
    val type: TextureDataType = TextureDataType.UNSIGNED_BYTE,

    /**
     * Anisotropic filtering level
     */
    val anisotropy: Int = 1,

    /**
     * Generate mipmaps
     */
    val generateMipmaps: Boolean = false,

    /**
     * Minification filter
     */
    val minFilter: TextureFilter = TextureFilter.LINEAR,

    /**
     * Magnification filter
     */
    val magFilter: TextureFilter = TextureFilter.LINEAR,

    /**
     * S-axis (U) wrapping
     */
    val wrapS: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,

    /**
     * T-axis (V) wrapping
     */
    val wrapT: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,

    /**
     * Enable depth buffer
     */
    val depthBuffer: Boolean = true,

    /**
     * Enable stencil buffer
     */
    val stencilBuffer: Boolean = false,

    /**
     * Depth texture format (for shadow mapping)
     */
    val depthFormat: TextureFormat? = null,

    /**
     * MSAA sample count (1, 2, 4, 8, 16)
     */
    val samples: Int = 1
)

/**
 * Render target utilities
 */
object RenderTargetUtils {
    /**
     * Create render target for shadow mapping
     *
     * Test Contract:
     * - MUST use depth texture
     * - MUST disable color attachment
     * - MUST use appropriate filtering
     */
    fun createShadowRenderTarget(
        width: Int,
        height: Int
    ): WebGPURenderTarget

    /**
     * Create render target for HDR rendering
     *
     * Test Contract:
     * - MUST use float texture format
     * - MUST support values > 1.0
     */
    fun createHDRRenderTarget(
        width: Int,
        height: Int
    ): WebGPURenderTarget

    /**
     * Create render target for post-processing
     *
     * Test Contract:
     * - MUST match screen dimensions
     * - MUST use appropriate format
     */
    fun createPostProcessRenderTarget(
        width: Int,
        height: Int,
        useHDR: Boolean = false
    ): WebGPURenderTarget

    /**
     * Create ping-pong render targets for iterative effects
     *
     * @return Pair of identical render targets for swapping
     *
     * Test Contract:
     * - MUST create two identical render targets
     * - MUST be usable for blur, bloom, etc.
     */
    fun createPingPongTargets(
        width: Int,
        height: Int,
        options: RenderTargetOptions = RenderTargetOptions()
    ): Pair<WebGPURenderTarget, WebGPURenderTarget>

    /**
     * Resize render target with aspect ratio preservation
     */
    fun resizeWithAspectRatio(
        target: WebGPURenderTarget,
        containerWidth: Int,
        containerHeight: Int,
        pixelRatio: Float = 1f
    )

    /**
     * Copy render target contents
     *
     * Test Contract:
     * - MUST copy pixels from source to destination
     * - MUST handle size mismatch (scaling)
     */
    fun copy(
        renderer: WebGPURenderer,
        source: WebGPURenderTarget,
        destination: WebGPURenderTarget
    )

    /**
     * Get platform render target limits
     */
    fun getPlatformLimits(renderer: WebGPURenderer): RenderTargetLimits
}

/**
 * Platform render target capabilities
 */
data class RenderTargetLimits(
    val maxSize: Int,
    val maxColorAttachments: Int,
    val maxSamples: Int,
    val supportsFloatTextures: Boolean,
    val supportsDepthTexture: Boolean,
    val supports3DTextures: Boolean
)

/**
 * Render target pool for efficient resource management
 *
 * Test Contract:
 * - MUST reuse render targets when possible
 * - MUST dispose unused targets
 * - MUST reduce GPU memory fragmentation
 */
class RenderTargetPool {
    private val pool: MutableMap<String, MutableList<WebGPURenderTarget>> = mutableMapOf()

    /**
     * Acquire render target from pool
     *
     * @return Existing target if available, or new target
     */
    fun acquire(
        width: Int,
        height: Int,
        options: RenderTargetOptions = RenderTargetOptions()
    ): WebGPURenderTarget

    /**
     * Release render target back to pool
     *
     * Test Contract:
     * - MUST add target to pool for reuse
     * - MUST NOT dispose target
     */
    fun release(target: WebGPURenderTarget)

    /**
     * Dispose all pooled render targets
     */
    fun dispose()

    /**
     * Get pool statistics
     */
    fun getStats(): PoolStats

    private fun generateKey(
        width: Int,
        height: Int,
        options: RenderTargetOptions
    ): String
}

data class PoolStats(
    val totalTargets: Int,
    val inUse: Int,
    val available: Int,
    val memoryUsageMB: Float
)

// Forward declarations
expect class DepthTexture
expect class DataTexture3D
expect class DataTexture2DArray
expect class WebGPURenderer