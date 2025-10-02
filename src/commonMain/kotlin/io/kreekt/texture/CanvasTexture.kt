package io.kreekt.texture

import io.kreekt.renderer.TextureFormat
import io.kreekt.texture.Texture

/**
 * CanvasTexture - Texture from HTML Canvas or similar drawing surface
 * T079 - Canvas rendering as texture source
 *
 * Uses canvas drawing surface as texture source, enabling dynamic texture creation
 * through 2D drawing operations.
 */
expect class CanvasTexture(
    width: Int = 256,
    height: Int = 256
) : Texture {

    override val width: Int
    override val height: Int

    /**
     * Clear the canvas to a solid color
     */
    fun clear(r: Float = 0f, g: Float = 0f, b: Float = 0f, a: Float = 1f)

    /**
     * Get the drawing context for this canvas
     * Platform-specific return type
     */
    fun getContext(): Any

    /**
     * Update texture from current canvas content
     */
    fun update()

    /**
     * Clone this canvas texture
     */
    override fun clone(): Texture
}

/**
 * Common CanvasTexture functionality
 */
abstract class CanvasTextureBase : Texture() {

    /**
     * Initialize canvas texture properties
     * MUST be called from subclass constructor AFTER super() completes
     */
    protected fun initCanvasTexture(textureName: String = "CanvasTexture") {
        // Use explicit setter method to avoid Kotlin bytecode generation issues
        setTextureName(textureName)
        format = TextureFormat.RGBA8
        generateMipmaps = false  // Canvas textures typically don't use mipmaps
        needsUpdate = true
    }

    /**
     * Mark for update
     */
    fun markNeedsUpdate() {
        needsUpdate = true
        version++
    }
}