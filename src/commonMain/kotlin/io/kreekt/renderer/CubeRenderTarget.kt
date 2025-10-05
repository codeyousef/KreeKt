package io.kreekt.renderer

import io.kreekt.texture.CubeTexture
import io.kreekt.texture.Texture

/**
 * Render target for cube texture rendering
 * Used by CubeCamera for environment mapping and reflections
 *
 * @property width Width of each cube face
 * @property height Height of each cube face
 * @property cubeTexture The cube texture for 6-face rendering
 */
class CubeRenderTarget(
    override val width: Int,
    override val height: Int,
    override val depthBuffer: Boolean = true,
    override val stencilBuffer: Boolean = false
) : RenderTarget {

    /**
     * The cube texture containing all 6 faces
     */
    val cubeTexture: CubeTexture = CubeTexture(size = width)

    /**
     * Main texture reference (returns cube texture)
     */
    override val texture: Texture? = cubeTexture

    /**
     * Depth texture (optional)
     */
    override val depthTexture: Texture? = null

    /**
     * Dispose of render target resources
     */
    fun dispose() {
        cubeTexture.dispose()
    }
}
