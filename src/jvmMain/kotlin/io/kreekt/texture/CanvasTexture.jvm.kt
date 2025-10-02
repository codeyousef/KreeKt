package io.kreekt.texture

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

/**
 * JVM implementation of CanvasTexture using BufferedImage
 */
actual class CanvasTexture actual constructor(
    width: Int,
    height: Int
) : CanvasTextureBase() {

    actual override val width: Int = width
    actual override val height: Int = height

    private val bufferedImage: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    private val graphics: Graphics2D = bufferedImage.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    }

    init {
        // Initialize AFTER all properties are created
        initCanvasTexture("CanvasTexture")
    }

    /**
     * Clear canvas to solid color
     */
    actual fun clear(r: Float, g: Float, b: Float, a: Float) {
        graphics.background = Color(r, g, b, a)
        graphics.clearRect(0, 0, width, height)
        markNeedsUpdate()
    }

    /**
     * Get the Graphics2D context
     */
    actual fun getContext(): Any = graphics

    /**
     * Update texture from canvas content
     */
    actual fun update() {
        // Mark texture as needing GPU upload
        needsUpdate = true
        version++
    }

    /**
     * Get the BufferedImage
     */
    fun getImage(): BufferedImage = bufferedImage

    /**
     * Get image data as ByteBuffer for GPU upload
     */
    fun getImageData(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(width * height * 4)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val argb = bufferedImage.getRGB(x, y)
                buffer.put(((argb shr 16) and 0xFF).toByte())  // R
                buffer.put(((argb shr 8) and 0xFF).toByte())   // G
                buffer.put((argb and 0xFF).toByte())           // B
                buffer.put(((argb shr 24) and 0xFF).toByte())  // A
            }
        }

        buffer.flip()
        return buffer
    }

    /**
     * Draw text on the canvas
     */
    fun drawText(text: String, x: Float, y: Float, color: Color = Color.WHITE) {
        graphics.color = color
        graphics.drawString(text, x, y)
        markNeedsUpdate()
    }

    /**
     * Draw a rectangle on the canvas
     */
    fun drawRect(x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE, fill: Boolean = true) {
        graphics.color = color
        if (fill) {
            graphics.fillRect(x, y, width, height)
        } else {
            graphics.drawRect(x, y, width, height)
        }
        markNeedsUpdate()
    }

    /**
     * Draw a circle on the canvas
     */
    fun drawCircle(x: Int, y: Int, radius: Int, color: Color = Color.WHITE, fill: Boolean = true) {
        graphics.color = color
        val diameter = radius * 2
        if (fill) {
            graphics.fillOval(x - radius, y - radius, diameter, diameter)
        } else {
            graphics.drawOval(x - radius, y - radius, diameter, diameter)
        }
        markNeedsUpdate()
    }

    override fun dispose() {
        super.dispose()
        graphics.dispose()
    }

    actual override fun clone(): Texture {
        val cloned = CanvasTexture(width, height)
        cloned.copy(this)

        // Copy image content
        val g = cloned.graphics
        g.drawImage(bufferedImage, 0, 0, null)

        return cloned
    }
}