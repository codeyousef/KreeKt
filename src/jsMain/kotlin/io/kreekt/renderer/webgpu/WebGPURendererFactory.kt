package io.kreekt.renderer.webgpu

import io.kreekt.renderer.Renderer
import io.kreekt.renderer.RendererConfig
import io.kreekt.renderer.RendererResult
import io.kreekt.renderer.WebGPURenderSurface
import io.kreekt.renderer.webgl.WebGLRenderer
import org.w3c.dom.HTMLCanvasElement

/**
 * Factory for creating WebGPU or WebGL renderer with automatic fallback.
 * FR-001: Automatic fallback to WebGL when WebGPU unavailable.
 */
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
            val result = renderer.initialize()

            when (result) {
                is RendererResult.Success -> {
                    console.log("WebGPURenderer initialized successfully")
                    renderer
                }
                is RendererResult.Error -> {
                    console.warn("WebGPU initialization failed: ${result.exception.message}")
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
        val renderer = WebGLRenderer(canvas, RendererConfig(antialias = true))
        val surface = WebGPURenderSurface(canvas)

        when (val result = renderer.initialize(surface)) {
            is RendererResult.Success -> {
                console.log("WebGLRenderer initialized successfully (fallback)")
                return renderer
            }
            is RendererResult.Error -> {
                console.error("WebGL fallback initialization failed: ${result.exception.message}")
                throw result.exception
            }
        }
    }
}
