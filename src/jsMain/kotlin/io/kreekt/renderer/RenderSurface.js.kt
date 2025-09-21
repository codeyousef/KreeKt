package io.kreekt.renderer

import org.w3c.dom.HTMLCanvasElement

/**
 * JS implementation of RenderSurface using HTML Canvas
 */
actual class RenderSurface(
    private val canvas: HTMLCanvasElement
) {
    actual val width: Int
        get() = canvas.width

    actual val height: Int
        get() = canvas.height

    actual val devicePixelRatio: Float
        get() = js("window.devicePixelRatio || 1").unsafeCast<Float>()

    actual val isValid: Boolean
        get() = canvas.width > 0 && canvas.height > 0

    actual fun resize(width: Int, height: Int) {
        canvas.width = width
        canvas.height = height
    }

    actual fun present() {
        // For WebGPU/WebGL, presentation is handled by the context
        // This might trigger a frame present in some implementations
    }

    actual fun dispose() {
        // Clean up canvas if needed
    }
}

/**
 * JS implementation of RenderSurfaceFactory
 */
actual object RenderSurfaceFactory {

    actual fun createSurface(target: Any, config: SurfaceConfig): RenderSurface {
        return when (target) {
            is HTMLCanvasElement -> RenderSurface(target)
            else -> throw IllegalArgumentException("Unsupported surface target type. Expected HTMLCanvasElement.")
        }
    }

    actual fun getSurfaceCapabilities(target: Any): SurfaceCapabilities? {
        if (!isValidTarget(target)) return null

        return SurfaceCapabilities(
            formats = listOf(
                SurfaceFormat.BGRA8_UNORM,
                SurfaceFormat.RGBA8_UNORM,
                SurfaceFormat.BGRA8_UNORM_SRGB,
                SurfaceFormat.RGBA8_UNORM_SRGB,
                SurfaceFormat.RGBA16_FLOAT
            ),
            presentModes = listOf(
                PresentMode.FIFO, // Default for browsers
                PresentMode.IMMEDIATE
            ),
            alphaeModes = listOf(
                AlphaMode.OPAQUE,
                AlphaMode.PREMULTIPLIED
            ),
            usage = setOf(
                SurfaceUsage.RENDER_ATTACHMENT,
                SurfaceUsage.COPY_SRC,
                SurfaceUsage.COPY_DST
            ),
            currentExtent = null,
            minImageExtent = Pair(1, 1),
            maxImageExtent = Pair(8192, 8192), // Common WebGL limit
            maxImageCount = 2, // Double buffering
            supportedTransforms = setOf(SurfaceTransform.IDENTITY)
        )
    }

    actual fun isValidTarget(target: Any): Boolean {
        return target is HTMLCanvasElement
    }
}