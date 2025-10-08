package io.kreekt.renderer.webgpu

import io.kreekt.renderer.Renderer
import io.kreekt.renderer.RendererConfig
import io.kreekt.renderer.webgl.WebGLRenderer
import org.w3c.dom.HTMLCanvasElement

/**
 * Factory for creating WebGPU or WebGL renderer with automatic fallback.
 * NOTE: This factory is deprecated - use io.kreekt.renderer.RendererFactory instead.
 */
@Deprecated("Use io.kreekt.renderer.RendererFactory instead", ReplaceWith("RendererFactory.create(surface, config)"))
object WebGPURendererFactory {
    /**
     * Creates a renderer, preferring WebGPU but falling back to WebGL if unavailable.
     * @param canvas HTML canvas element to render to
     * @return Renderer instance (WebGPURenderer or WebGLRenderer)
     */
    suspend fun create(canvas: HTMLCanvasElement): Renderer {
        // Check if WebGPU is available
        val gpu = WebGPUDetector.getGPU()

        return if (gpu != null) {
            console.log("WebGPU available - creating WebGPURenderer")
            val renderer = WebGPURenderer(canvas)
            val config = RendererConfig()
            val result = renderer.initialize(config)

            when (result) {
                is io.kreekt.core.Result.Success -> {
                    console.log("WebGPURenderer initialized successfully")
                    renderer
                }

                is io.kreekt.core.Result.Error -> {
                    console.warn("WebGPU initialization failed: ${result.message}")
                    console.warn("Falling back to WebGL renderer")
                    createWebGLFallback(canvas)
                }
            }
        } else {
            console.log("WebGPU not available - using WebGL renderer")
            createWebGLFallback(canvas)
        }
    }

    /**
     * Creates a WebGL renderer as fallback.
     */
    private suspend fun createWebGLFallback(canvas: HTMLCanvasElement): Renderer {
        val renderer = WebGLRenderer(canvas)
        val config = RendererConfig()

        when (val result = renderer.initialize(config)) {
            is io.kreekt.core.Result.Success -> {
                console.log("WebGLRenderer initialized successfully (fallback)")
                return renderer
            }

            is io.kreekt.core.Result.Error -> {
                console.error("WebGL fallback initialization failed: ${result.message}")
                throw result.exception ?: RuntimeException(result.message)
            }
        }
    }
}
