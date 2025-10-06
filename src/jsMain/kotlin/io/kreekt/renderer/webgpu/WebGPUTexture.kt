package io.kreekt.renderer.webgpu

import io.kreekt.renderer.RendererException
import io.kreekt.renderer.RendererResult

/**
 * Texture descriptor for creation.
 */
data class TextureDescriptor(
    val label: String? = null,
    val width: Int,
    val height: Int,
    val depth: Int = 1,
    val format: TextureFormat = TextureFormat.RGBA8_UNORM,
    val usage: Int = GPUTextureUsage.TEXTURE_BINDING or GPUTextureUsage.COPY_DST,
    val mipLevelCount: Int = 1,
    val sampleCount: Int = 1
)

/**
 * WebGPU texture implementation.
 * T031: 2D/3D texture handling and sampling.
 */
class WebGPUTexture(
    private val device: GPUDevice,
    private val descriptor: TextureDescriptor
) {
    private var texture: GPUTexture? = null
    private var view: GPUTextureView? = null

    /**
     * Creates the GPU texture.
     */
    fun create(): RendererResult<Unit> {
        return try {
            val textureDescriptor = js("({})").unsafeCast<GPUTextureDescriptor>()
            descriptor.label?.let { textureDescriptor.label = it }

            // Set size
            val size = js("({ width: descriptor.width, height: descriptor.height, depthOrArrayLayers: descriptor.depth })")
            textureDescriptor.size = size

            textureDescriptor.mipLevelCount = descriptor.mipLevelCount
            textureDescriptor.sampleCount = descriptor.sampleCount
            textureDescriptor.dimension = "2d"
            textureDescriptor.format = when (descriptor.format) {
                TextureFormat.RGBA8_UNORM -> "rgba8unorm"
                TextureFormat.RGBA8_SRGB -> "rgba8unorm-srgb"
                TextureFormat.BGRA8_UNORM -> "bgra8unorm"
                TextureFormat.BGRA8_SRGB -> "bgra8unorm-srgb"
                TextureFormat.DEPTH24_PLUS -> "depth24plus"
                TextureFormat.DEPTH32_FLOAT -> "depth32float"
            }
            textureDescriptor.usage = descriptor.usage

            texture = device.createTexture(textureDescriptor)

            // Create default view
            view = texture?.createView()

            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.ResourceCreationFailed("Texture creation failed", e))
        }
    }

    /**
     * Uploads image data to the texture.
     * @param data Image data
     * @param width Width in pixels
     * @param height Height in pixels
     */
    fun upload(data: ByteArray, width: Int, height: Int): RendererResult<Unit> {
        return try {
            texture?.let { tex ->
                // Create destination for writeTexture
                val destination = js("({})").unsafeCast<dynamic>()
                destination.texture = tex
                destination.mipLevel = 0
                destination.origin = js("({ x: 0, y: 0, z: 0 })")

                // Create data layout
                val dataLayout = js("({})").unsafeCast<dynamic>()
                dataLayout.offset = 0
                dataLayout.bytesPerRow = width * 4 // RGBA = 4 bytes per pixel
                dataLayout.rowsPerImage = height

                // Create size
                val size = js("({ width: width, height: height, depthOrArrayLayers: 1 })")

                // Convert ByteArray to Uint8Array
                val uint8Array = js("new Uint8Array(data.size)")
                for (i in data.indices) {
                    js("uint8Array[i] = data[i]")
                }

                device.queue.writeTexture(destination, uint8Array, dataLayout, size)
                RendererResult.Success(Unit)
            } ?: RendererResult.Error(RendererException.InvalidState("Texture not created"))
        } catch (e: Exception) {
            RendererResult.Error(RendererException.ResourceCreationFailed("Texture upload failed", e))
        }
    }

    /**
     * Gets the GPU texture handle.
     */
    fun getTexture(): GPUTexture? = texture

    /**
     * Gets the texture view for sampling.
     */
    fun getView(): GPUTextureView? = view

    /**
     * Creates a custom texture view.
     */
    fun createView(viewDescriptor: GPUTextureViewDescriptor? = null): GPUTextureView? {
        return if (viewDescriptor != null) {
            texture?.createView(viewDescriptor)
        } else {
            texture?.createView()
        }
    }

    /**
     * Gets texture dimensions.
     */
    fun getDimensions(): Triple<Int, Int, Int> {
        return Triple(descriptor.width, descriptor.height, descriptor.depth)
    }

    /**
     * Gets texture format.
     */
    fun getFormat(): TextureFormat = descriptor.format

    /**
     * Disposes the texture and releases GPU memory.
     */
    fun dispose() {
        texture?.destroy()
        texture = null
        view = null
    }
}
