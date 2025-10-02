package io.kreekt.texture

import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlinx.browser.document

/**
 * JavaScript implementation of CanvasTexture using HTMLCanvasElement
 */
actual class CanvasTexture actual constructor(
    width: Int,
    height: Int
) : CanvasTextureBase() {

    actual override val width: Int = width
    actual override val height: Int = height

    private val canvas: HTMLCanvasElement = (document.createElement("canvas") as HTMLCanvasElement).apply {
        this.width = width
        this.height = height
    }

    private val context: CanvasRenderingContext2D = canvas.getContext("2d") as CanvasRenderingContext2D

    init {
        // Initialize AFTER all properties are created
        initCanvasTexture("CanvasTexture")
        // Set default canvas settings
        context.imageSmoothingEnabled = true
    }

    /**
     * Clear canvas to solid color
     */
    actual fun clear(r: Float, g: Float, b: Float, a: Float) {
        context.save()
        context.globalCompositeOperation = "source-over"
        context.fillStyle = "rgba(${(r * 255).toInt()}, ${(g * 255).toInt()}, ${(b * 255).toInt()}, $a)"
        context.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
        context.restore()
        markNeedsUpdate()
    }

    /**
     * Get the 2D rendering context
     */
    actual fun getContext(): Any = context

    /**
     * Update texture from canvas content
     */
    actual fun update() {
        // In WebGL, this would trigger texture upload from canvas
        needsUpdate = true
        version++
    }

    /**
     * Get the HTML canvas element
     */
    fun getCanvas(): HTMLCanvasElement = canvas

    /**
     * Draw text on the canvas
     */
    fun drawText(text: String, x: Double, y: Double, font: String = "16px Arial", color: String = "white") {
        context.font = font
        context.fillStyle = color
        context.fillText(text, x, y)
        markNeedsUpdate()
    }

    /**
     * Draw a rectangle on the canvas
     */
    fun drawRect(x: Double, y: Double, width: Double, height: Double, color: String = "white", fill: Boolean = true) {
        if (fill) {
            context.fillStyle = color
            context.fillRect(x, y, width, height)
        } else {
            context.strokeStyle = color
            context.strokeRect(x, y, width, height)
        }
        markNeedsUpdate()
    }

    /**
     * Draw a circle on the canvas
     */
    fun drawCircle(x: Double, y: Double, radius: Double, color: String = "white", fill: Boolean = true) {
        context.beginPath()
        context.arc(x, y, radius, 0.0, 2 * kotlin.math.PI)
        if (fill) {
            context.fillStyle = color
            context.fill()
        } else {
            context.strokeStyle = color
            context.stroke()
        }
        markNeedsUpdate()
    }

    override fun dispose() {
        super.dispose()
        // Canvas will be garbage collected
    }

    actual override fun clone(): Texture {
        val cloned = CanvasTexture(width, height)
        cloned.copy(this)

        // Copy canvas content
        val ctx = cloned.context
        ctx.drawImage(canvas, 0.0, 0.0)

        return cloned
    }
}