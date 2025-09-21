package io.kreekt.renderer

/**
 * Native implementation of RenderSurface
 */
actual class RenderSurface {
    actual val width: Int = 800
    actual val height: Int = 600
    actual val devicePixelRatio: Float = 1.0f
    actual val isValid: Boolean = true

    actual fun resize(width: Int, height: Int) {
        // No-op for Native
    }

    actual fun present() {
        // No-op for Native
    }

    actual fun dispose() {
        // No-op for Native
    }
}

/**
 * Native implementation of RenderSurfaceFactory
 */
actual object RenderSurfaceFactory {
    actual fun createSurface(target: Any, config: SurfaceConfig): RenderSurface {
        return RenderSurface()
    }

    actual fun getSurfaceCapabilities(target: Any): SurfaceCapabilities? = null

    actual fun isValidTarget(target: Any): Boolean = false
}