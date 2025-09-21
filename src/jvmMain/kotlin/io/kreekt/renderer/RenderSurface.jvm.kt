package io.kreekt.renderer

import java.awt.Canvas
import javax.swing.JPanel

/**
 * JVM implementation of RenderSurface using Java Swing/AWT
 */
actual class RenderSurface(
    private val component: java.awt.Component
) {
    actual val width: Int
        get() = component.width

    actual val height: Int
        get() = component.height

    actual val devicePixelRatio: Float
        get() = 1f // JVM doesn't have built-in high DPI scaling info

    actual val isValid: Boolean
        get() = component.isDisplayable && component.width > 0 && component.height > 0

    actual fun resize(width: Int, height: Int) {
        component.setSize(width, height)
    }

    actual fun present() {
        // For JVM, we might need to call repaint or similar
        component.repaint()
    }

    actual fun dispose() {
        // Clean up any resources if needed
    }
}

/**
 * JVM implementation of RenderSurfaceFactory
 */
actual object RenderSurfaceFactory {

    actual fun createSurface(target: Any, config: SurfaceConfig): RenderSurface {
        return when (target) {
            is java.awt.Component -> RenderSurface(target)
            is Canvas -> RenderSurface(target)
            is JPanel -> RenderSurface(target)
            else -> throw IllegalArgumentException("Unsupported surface target type: ${target::class}")
        }
    }

    actual fun getSurfaceCapabilities(target: Any): SurfaceCapabilities? {
        if (!isValidTarget(target)) return null

        return SurfaceCapabilities(
            formats = listOf(
                SurfaceFormat.BGRA8_UNORM,
                SurfaceFormat.RGBA8_UNORM,
                SurfaceFormat.BGRA8_UNORM_SRGB,
                SurfaceFormat.RGBA8_UNORM_SRGB
            ),
            presentModes = listOf(
                PresentMode.FIFO,
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
            maxImageExtent = Pair(16384, 16384),
            maxImageCount = 3,
            supportedTransforms = setOf(SurfaceTransform.IDENTITY)
        )
    }

    actual fun isValidTarget(target: Any): Boolean {
        return target is java.awt.Component
    }
}