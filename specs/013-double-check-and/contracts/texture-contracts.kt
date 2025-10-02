/**
 * Texture Compression Contracts
 *
 * Feature: 013-double-check-and (Three.js r180 Feature Parity)
 * Category: Texture Encoding and Compression
 *
 * Requirements covered: FR-062 through FR-066
 */

package io.kreekt.texture

import io.kreekt.core.Result
import io.kreekt.renderer.TextureFormat

/**
 * Texture compressor interface.
 *
 * FR-062: System MUST support all compressed texture formats (S3TC/DXT, PVRTC, ETC1/ETC2, ASTC, BPTC)
 * FR-064: System MUST provide platform-appropriate texture format selection
 */
interface TextureCompressor {
    /**
     * Gets supported texture formats for this platform.
     */
    fun getSupportedFormats(): Set<TextureFormat>

    /**
     * Selects the best texture format for the given criteria.
     *
     * @param hasAlpha Whether the texture has alpha channel
     * @param isNormalMap Whether this is a normal map
     * @param quality Desired quality level
     * @return The recommended format
     */
    fun selectBestFormat(
        hasAlpha: Boolean,
        isNormalMap: Boolean = false,
        quality: TextureQuality = TextureQuality.HIGH
    ): TextureFormat

    /**
     * Compresses texture data.
     *
     * @param data Uncompressed RGBA data
     * @param width Texture width
     * @param height Texture height
     * @param targetFormat Target compression format
     * @param options Compression options
     * @return Compressed texture data
     */
    suspend fun compress(
        data: ByteArray,
        width: Int,
        height: Int,
        targetFormat: TextureFormat,
        options: CompressionOptions = CompressionOptions()
    ): Result<CompressedTextureData>
}

/**
 * Compressed texture data.
 */
data class CompressedTextureData(
    val format: TextureFormat,
    val width: Int,
    val height: Int,
    val data: ByteArray,
    val mipmaps: List<ByteArray> = emptyList()
)

/**
 * Compression options.
 */
data class CompressionOptions(
    val generateMipmaps: Boolean = true,
    val quality: TextureQuality = TextureQuality.HIGH,
    val compressionLevel: Int = 5
)

/**
 * Texture quality levels.
 *
 * FR-064: System MUST provide platform-appropriate texture format selection
 */
enum class TextureQuality {
    LOW,    // Favor size
    MEDIUM, // Balanced
    HIGH,   // Favor quality
    ULTRA   // Best quality
}

/**
 * Texture wrapping modes.
 *
 * FR-065: System MUST support all texture wrapping modes (repeat, clamp, mirror)
 */
enum class TextureWrapping {
    RepeatWrapping,
    ClampToEdgeWrapping,
    MirroredRepeatWrapping
}

/**
 * Texture filtering modes.
 *
 * FR-066: System MUST support all texture filtering modes (nearest, linear, mipmap variants)
 */
enum class TextureFilter {
    NearestFilter,
    LinearFilter,
    NearestMipmapNearestFilter,
    NearestMipmapLinearFilter,
    LinearMipmapNearestFilter,
    LinearMipmapLinearFilter
}

/**
 * HDR texture encoding formats.
 *
 * FR-063: System MUST support HDR texture formats (RGBE, RGBM, RGBD)
 */
enum class HDREncoding {
    RGBE,  // RGB + shared exponent
    RGBM,  // RGB * M
    RGBD,  // RGB / D
    Linear // No encoding
}
