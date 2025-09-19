package io.kreekt.renderer

/**
 * Platform-agnostic render surface interface.
 * Uses expect/actual pattern for platform-specific implementations.
 *
 * Represents the surface that the renderer will draw to,
 * such as a canvas, window, or view.
 */
expect class RenderSurface {
    /**
     * Width of the render surface in pixels
     */
    val width: Int

    /**
     * Height of the render surface in pixels
     */
    val height: Int

    /**
     * Device pixel ratio for high-DPI displays
     */
    val devicePixelRatio: Float

    /**
     * Whether the surface is currently valid and can be rendered to
     */
    val isValid: Boolean

    /**
     * Resizes the render surface
     */
    fun resize(width: Int, height: Int)

    /**
     * Present/swap the rendered frame to the surface
     */
    fun present()

    /**
     * Dispose of surface resources
     */
    fun dispose()
}

/**
 * Surface configuration options
 */
data class SurfaceConfig(
    val preferredFormat: SurfaceFormat = SurfaceFormat.BGRA8_UNORM,
    val preferredUsage: Set<SurfaceUsage> = setOf(SurfaceUsage.RENDER_ATTACHMENT),
    val preferredPresentMode: PresentMode = PresentMode.FIFO,
    val alphaMode: AlphaMode = AlphaMode.OPAQUE
)

/**
 * Surface pixel formats
 */
enum class SurfaceFormat {
    BGRA8_UNORM,
    BGRA8_UNORM_SRGB,
    RGBA8_UNORM,
    RGBA8_UNORM_SRGB,
    RGBA16_FLOAT,
    RGB10A2_UNORM
}

/**
 * Surface usage flags
 */
enum class SurfaceUsage {
    RENDER_ATTACHMENT,
    COPY_SRC,
    COPY_DST,
    STORAGE_BINDING
}

/**
 * Present modes for vsync control
 */
enum class PresentMode {
    /**
     * Immediate presentation (no vsync)
     */
    IMMEDIATE,

    /**
     * Mailbox mode (replace previous frame if not presented)
     */
    MAILBOX,

    /**
     * FIFO mode (wait for vsync, traditional vsync)
     */
    FIFO,

    /**
     * FIFO relaxed (allow tearing if late)
     */
    FIFO_RELAXED
}

/**
 * Alpha blending modes
 */
enum class AlphaMode {
    /**
     * No alpha blending
     */
    OPAQUE,

    /**
     * Premultiplied alpha
     */
    PREMULTIPLIED,

    /**
     * Postmultiplied alpha
     */
    POSTMULTIPLIED,

    /**
     * Alpha is inherited from the parent surface
     */
    INHERIT
}

/**
 * Surface capabilities reported by the platform
 */
data class SurfaceCapabilities(
    val formats: List<SurfaceFormat>,
    val presentModes: List<PresentMode>,
    val alphaeModes: List<AlphaMode>,
    val usage: Set<SurfaceUsage>,
    val currentExtent: Pair<Int, Int>?,
    val minImageExtent: Pair<Int, Int>,
    val maxImageExtent: Pair<Int, Int>,
    val maxImageCount: Int,
    val supportedTransforms: Set<SurfaceTransform>
)

/**
 * Surface transformation flags
 */
enum class SurfaceTransform {
    IDENTITY,
    ROTATE_90,
    ROTATE_180,
    ROTATE_270,
    HORIZONTAL_MIRROR,
    HORIZONTAL_MIRROR_ROTATE_90,
    HORIZONTAL_MIRROR_ROTATE_180,
    HORIZONTAL_MIRROR_ROTATE_270
}

/**
 * Common surface factory interface
 */
interface SurfaceFactory {
    /**
     * Creates a render surface for the given target
     */
    fun createSurface(target: Any, config: SurfaceConfig = SurfaceConfig()): RenderSurface

    /**
     * Gets capabilities for a potential surface
     */
    fun getSurfaceCapabilities(target: Any): SurfaceCapabilities?

    /**
     * Checks if the target is valid for surface creation
     */
    fun isValidTarget(target: Any): Boolean
}

/**
 * Platform-specific surface creation functions
 */
expect object RenderSurfaceFactory {
    /**
     * Creates a render surface for the given target
     */
    fun createSurface(target: Any, config: SurfaceConfig = SurfaceConfig()): RenderSurface

    /**
     * Gets capabilities for a potential surface
     */
    fun getSurfaceCapabilities(target: Any): SurfaceCapabilities?

    /**
     * Checks if the target is valid for surface creation
     */
    fun isValidTarget(target: Any): Boolean
}

/**
 * Surface event listener interface
 */
interface SurfaceEventListener {
    /**
     * Called when the surface is resized
     */
    fun onSurfaceResized(width: Int, height: Int) {}

    /**
     * Called when the surface becomes invalid
     */
    fun onSurfaceInvalidated() {}

    /**
     * Called when the surface is recreated
     */
    fun onSurfaceRecreated() {}

    /**
     * Called when the device pixel ratio changes
     */
    fun onDevicePixelRatioChanged(newRatio: Float) {}
}

/**
 * Surface utilities
 */
object SurfaceUtils {

    /**
     * Calculates the optimal surface size for a given viewport
     */
    fun getOptimalSize(
        viewportWidth: Int,
        viewportHeight: Int,
        devicePixelRatio: Float,
        maxSize: Int = 4096
    ): Pair<Int, Int> {
        val width = (viewportWidth * devicePixelRatio).toInt().coerceAtMost(maxSize)
        val height = (viewportHeight * devicePixelRatio).toInt().coerceAtMost(maxSize)
        return Pair(width, height)
    }

    /**
     * Gets the bytes per pixel for a surface format
     */
    fun getBytesPerPixel(format: SurfaceFormat): Int {
        return when (format) {
            SurfaceFormat.BGRA8_UNORM,
            SurfaceFormat.BGRA8_UNORM_SRGB,
            SurfaceFormat.RGBA8_UNORM,
            SurfaceFormat.RGBA8_UNORM_SRGB,
            SurfaceFormat.RGB10A2_UNORM -> 4
            SurfaceFormat.RGBA16_FLOAT -> 8
        }
    }

    /**
     * Estimates memory usage for a surface
     */
    fun estimateMemoryUsage(
        width: Int,
        height: Int,
        format: SurfaceFormat,
        bufferCount: Int = 2
    ): Long {
        val bytesPerPixel = getBytesPerPixel(format)
        return width.toLong() * height.toLong() * bytesPerPixel * bufferCount
    }

    /**
     * Checks if a format supports sRGB
     */
    fun isSRGBFormat(format: SurfaceFormat): Boolean {
        return when (format) {
            SurfaceFormat.BGRA8_UNORM_SRGB,
            SurfaceFormat.RGBA8_UNORM_SRGB -> true
            else -> false
        }
    }

    /**
     * Gets the linear equivalent of an sRGB format
     */
    fun getLinearFormat(format: SurfaceFormat): SurfaceFormat {
        return when (format) {
            SurfaceFormat.BGRA8_UNORM_SRGB -> SurfaceFormat.BGRA8_UNORM
            SurfaceFormat.RGBA8_UNORM_SRGB -> SurfaceFormat.RGBA8_UNORM
            else -> format
        }
    }

    /**
     * Gets the sRGB equivalent of a linear format
     */
    fun getSRGBFormat(format: SurfaceFormat): SurfaceFormat {
        return when (format) {
            SurfaceFormat.BGRA8_UNORM -> SurfaceFormat.BGRA8_UNORM_SRGB
            SurfaceFormat.RGBA8_UNORM -> SurfaceFormat.RGBA8_UNORM_SRGB
            else -> format
        }
    }

    /**
     * Validates surface dimensions
     */
    fun validateDimensions(width: Int, height: Int, capabilities: SurfaceCapabilities): Boolean {
        val (minWidth, minHeight) = capabilities.minImageExtent
        val (maxWidth, maxHeight) = capabilities.maxImageExtent

        return width >= minWidth && width <= maxWidth &&
               height >= minHeight && height <= maxHeight
    }

    /**
     * Selects the best present mode from available options
     */
    fun selectBestPresentMode(
        available: List<PresentMode>,
        preferred: PresentMode = PresentMode.MAILBOX
    ): PresentMode {
        return when {
            preferred in available -> preferred
            PresentMode.MAILBOX in available -> PresentMode.MAILBOX
            PresentMode.FIFO in available -> PresentMode.FIFO
            available.isNotEmpty() -> available.first()
            else -> PresentMode.FIFO
        }
    }

    /**
     * Selects the best surface format from available options
     */
    fun selectBestFormat(
        available: List<SurfaceFormat>,
        preferred: SurfaceFormat = SurfaceFormat.BGRA8_UNORM_SRGB
    ): SurfaceFormat {
        return when {
            preferred in available -> preferred
            SurfaceFormat.BGRA8_UNORM_SRGB in available -> SurfaceFormat.BGRA8_UNORM_SRGB
            SurfaceFormat.RGBA8_UNORM_SRGB in available -> SurfaceFormat.RGBA8_UNORM_SRGB
            SurfaceFormat.BGRA8_UNORM in available -> SurfaceFormat.BGRA8_UNORM
            SurfaceFormat.RGBA8_UNORM in available -> SurfaceFormat.RGBA8_UNORM
            available.isNotEmpty() -> available.first()
            else -> SurfaceFormat.BGRA8_UNORM
        }
    }
}

/**
 * Extension functions for RenderSurface
 */

/**
 * Gets the aspect ratio of the surface
 */
fun RenderSurface.getAspectRatio(): Float = width.toFloat() / height.toFloat()

/**
 * Checks if the surface is landscape orientation
 */
fun RenderSurface.isLandscape(): Boolean = width > height

/**
 * Checks if the surface is portrait orientation
 */
fun RenderSurface.isPortrait(): Boolean = height > width

/**
 * Gets the total pixel count
 */
fun RenderSurface.getPixelCount(): Int = width * height

/**
 * Checks if the surface is high DPI
 */
fun RenderSurface.isHighDPI(): Boolean = devicePixelRatio > 1.5f

/**
 * Gets the logical size (actual size / device pixel ratio)
 */
fun RenderSurface.getLogicalSize(): Pair<Float, Float> {
    return Pair(width / devicePixelRatio, height / devicePixelRatio)
}