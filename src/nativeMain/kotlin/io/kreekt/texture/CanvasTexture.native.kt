package io.kreekt.texture

/**
 * Native implementation of CanvasTexture (stub for Native platforms)
 */
actual class CanvasTexture actual constructor(
    width: Int,
    height: Int
) : CanvasTextureBase() {

    actual override val width: Int = width
    actual override val height: Int = height

    private val imageData = ByteArray(width * height * 4)

    init {
        // Initialize AFTER all properties are created
        initCanvasTexture("CanvasTexture")
    }

    /**
     * Clear canvas to solid color
     */
    actual fun clear(r: Float, g: Float, b: Float, a: Float) {
        val rByte = (r * 255).toInt().toByte()
        val gByte = (g * 255).toInt().toByte()
        val bByte = (b * 255).toInt().toByte()
        val aByte = (a * 255).toInt().toByte()

        for (i in imageData.indices step 4) {
            imageData[i] = rByte
            imageData[i + 1] = gByte
            imageData[i + 2] = bByte
            imageData[i + 3] = aByte
        }
        markNeedsUpdate()
    }

    /**
     * Get the drawing context - returns ByteArray on Native
     */
    actual fun getContext(): Any = imageData

    /**
     * Update texture from canvas content
     */
    actual fun update() {
        needsUpdate = true
        version++
    }

    /**
     * Get image data for GPU upload
     */
    fun getImageData(): ByteArray = imageData

    actual override fun clone(): Texture {
        val cloned = CanvasTexture(width, height)
        cloned.copy(this)
        imageData.copyInto(cloned.imageData)
        return cloned
    }
}
