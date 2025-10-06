package io.kreekt.renderer

import org.w3c.dom.HTMLCanvasElement

/**
 * WebGPU render surface backed by an HTML canvas element
 */
class WebGPURenderSurface(val canvas: HTMLCanvasElement) : RenderSurface {
    override val width: Int
        get() = canvas.width

    override val height: Int
        get() = canvas.height

    override val devicePixelRatio: Float
        get() = js("window.devicePixelRatio").unsafeCast<Float>()

    override val isValid: Boolean
        get() = true

    override fun resize(width: Int, height: Int) {
        canvas.width = width
        canvas.height = height
    }

    override fun present() {
        // WebGPU handles presentation automatically
    }

    override fun dispose() {
        // Cleanup WebGPU resources
    }
}
