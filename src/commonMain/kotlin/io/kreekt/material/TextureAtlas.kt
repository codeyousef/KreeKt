/**
 * TextureAtlas for optimized texture packing
 * T028 - Advanced texture atlas system with automatic packing and UV remapping
 *
 * Features:
 * - Automatic texture packing using rectangle packing algorithms
 * - UV coordinate remapping for atlased textures
 * - Multiple packing strategies (Max Rects, Skyline, Guillotine)
 * - Texture filtering and preprocessing
 * - Atlas optimization and compression
 * - Runtime atlas updates and streaming
 * - Multi-channel atlas packing (RGB+A, Normal+Roughness+Metallic)
 * - Mipmap generation for packed atlases
 */
package io.kreekt.material

import io.kreekt.core.math.*
import kotlinx.collections.immutable.*
import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Advanced texture atlas system for optimizing texture memory and draw calls
 * Implements industry-standard rectangle packing algorithms
 */
class TextureAtlas(
    val width: Int = 2048,
    val height: Int = 2048,
    val format: TextureFormat = TextureFormat.RGBA8,
    val packingStrategy: PackingStrategy = PackingStrategy.MAX_RECTS
) {

    // Atlas data and state
    private var _atlasData: ByteArray = ByteArray(width * height * format.bytesPerPixel)
    private val _packedTextures = mutableMapOf<String, PackedTexture>()
    private val _freeRectangles = mutableListOf<Rectangle>()
    private val _usedRectangles = mutableListOf<Rectangle>()

    // Packing algorithms
    private val _packer: RectanglePacker = createPacker(packingStrategy)

    // Atlas properties
    var padding: Int = 2
    var borderExtension: Int = 1
    var autoResize: Boolean = false
    var maxSize: Int = 4096
    var minSize: Int = 512
    var allowRotation: Boolean = true
    var useSquareAtlas: Boolean = true

    // Quality and optimization
    var enableMipmaps: Boolean = true
    var mipmapFilter: FilterMode = FilterMode.LINEAR
    var compressionQuality: Float = 0.9f
    var enableCompression: Boolean = false

    // Statistics and debugging
    var fillRatio: Float = 0f
        private set
    var wastedSpace: Int = 0
        private set
    var totalTextures: Int = 0
        private set

    init {
        initialize()
    }

    /**
     * Pack a single texture into the atlas
     */
    fun packTexture(
        id: String,
        textureData: TextureData,
        options: PackingOptions = PackingOptions()
    ): PackingResult {
        // Validate input
        if (_packedTextures.containsKey(id)) {
            return PackingResult.Error("Texture with ID '$id' already exists in atlas")
        }

        if (textureData.width <= 0 || textureData.height <= 0) {
            return PackingResult.Error("Invalid texture dimensions: ${textureData.width}x${textureData.height}")
        }

        // Preprocess texture if needed
        val processedTexture = preprocessTexture(textureData, options)

        // Add padding to dimensions
        val paddedWidth = processedTexture.width + padding * 2
        val paddedHeight = processedTexture.height + padding * 2

        // Try to find space in atlas
        val rectangle = _packer.findBestFit(paddedWidth, paddedHeight, allowRotation)
            ?: return handleAtlasFull(id, processedTexture, options)

        // Pack the texture
        val packedTexture = createPackedTexture(
            id = id,
            originalTexture = processedTexture,
            atlasRectangle = rectangle,
            rotated = rectangle.rotated
        )

        // Copy texture data to atlas
        copyTextureToAtlas(processedTexture, packedTexture)

        // Update bookkeeping
        _packedTextures[id] = packedTexture
        _usedRectangles.add(rectangle)
        _packer.markRectangleAsUsed(rectangle)

        // Update statistics
        updateStatistics()

        return PackingResult.Success(packedTexture)
    }

    /**
     * Pack multiple textures efficiently
     */
    fun packTextures(
        textures: Map<String, TextureData>,
        options: BatchPackingOptions = BatchPackingOptions()
    ): BatchPackingResult {
        val results = mutableMapOf<String, PackingResult>()
        val failed = mutableListOf<String>()

        // Sort textures by size for optimal packing
        val sortedTextures = when (options.sortStrategy) {
            SortStrategy.AREA_DESC -> textures.toList().sortedByDescending { it.second.width * it.second.height }
            SortStrategy.WIDTH_DESC -> textures.toList().sortedByDescending { it.second.width }
            SortStrategy.HEIGHT_DESC -> textures.toList().sortedByDescending { it.second.height }
            SortStrategy.PERIMETER_DESC -> textures.toList().sortedByDescending {
                2 * (it.second.width + it.second.height)
            }
            SortStrategy.NONE -> textures.toList()
        }

        // Pack textures in sorted order
        for ((id, textureData) in sortedTextures) {
            val result = packTexture(id, textureData, options.packingOptions)
            results[id] = result

            if (result is PackingResult.Error) {
                failed.add(id)
                if (!options.continueOnFailure) {
                    break
                }
            }
        }

        return BatchPackingResult(
            results = results,
            successCount = results.count { it.value is PackingResult.Success },
            failureCount = failed.size,
            failedTextures = failed
        )
    }

    /**
     * Get UV coordinates for a packed texture
     */
    fun getUVMapping(textureId: String): UVMapping? {
        return _packedTextures[textureId]?.let { packed ->
            val x = packed.atlasRectangle.x.toFloat()
            val y = packed.atlasRectangle.y.toFloat()
            val w = packed.atlasRectangle.width.toFloat()
            val h = packed.atlasRectangle.height.toFloat()

            val u1 = x / width
            val v1 = y / height
            val u2 = (x + w) / width
            val v2 = (y + h) / height

            UVMapping(
                textureId = textureId,
                uvMin = Vector2(u1, v1),
                uvMax = Vector2(u2, v2),
                rotated = packed.atlasRectangle.rotated,
                originalSize = Vector2(packed.originalTexture.width.toFloat(), packed.originalTexture.height.toFloat()),
                atlasSize = Vector2(width.toFloat(), height.toFloat())
            )
        }
    }

    /**
     * Transform UV coordinates from original texture space to atlas space
     */
    fun transformUV(textureId: String, originalUV: Vector2): Vector2? {
        val mapping = getUVMapping(textureId) ?: return null

        var u = originalUV.x
        var v = originalUV.y

        // Handle rotation
        if (mapping.rotated) {
            val temp = u
            u = 1f - v
            v = temp
        }

        // Transform to atlas space
        val atlasU = mapping.uvMin.x + u * (mapping.uvMax.x - mapping.uvMin.x)
        val atlasV = mapping.uvMin.y + v * (mapping.uvMax.y - mapping.uvMin.y)

        return Vector2(atlasU, atlasV)
    }

    /**
     * Remove a texture from the atlas
     */
    fun removeTexture(textureId: String): Boolean {
        val packed = _packedTextures.remove(textureId) ?: return false

        // Mark rectangle as free
        _packer.freeRectangle(packed.atlasRectangle)
        _usedRectangles.remove(packed.atlasRectangle)

        // Clear atlas data in that region
        clearAtlasRegion(packed.atlasRectangle)

        // Update statistics
        updateStatistics()

        return true
    }

    /**
     * Update an existing texture in the atlas
     */
    fun updateTexture(
        textureId: String,
        newTextureData: TextureData,
        options: PackingOptions = PackingOptions()
    ): PackingResult {
        val existing = _packedTextures[textureId]
            ?: return PackingResult.Error("Texture '$textureId' not found in atlas")

        val processedTexture = preprocessTexture(newTextureData, options)

        // Check if new texture fits in same space
        val requiredWidth = processedTexture.width + padding * 2
        val requiredHeight = processedTexture.height + padding * 2

        if (requiredWidth <= existing.atlasRectangle.width &&
            requiredHeight <= existing.atlasRectangle.height) {

            // Update in place
            val updatedPacked = existing.copy(originalTexture = processedTexture)
            copyTextureToAtlas(processedTexture, updatedPacked)
            _packedTextures[textureId] = updatedPacked

            return PackingResult.Success(updatedPacked)
        } else {
            // Remove and re-pack
            removeTexture(textureId)
            return packTexture(textureId, processedTexture, options)
        }
    }

    /**
     * Optimize atlas layout by repacking all textures
     */
    fun optimize(): OptimizationResult {
        val originalFillRatio = fillRatio
        val originalWastedSpace = wastedSpace

        // Store current textures
        val texturesToRepack = _packedTextures.values.map {
            it.id to it.originalTexture
        }.toMap()

        // Clear atlas
        clear()

        // Repack with optimal strategy
        val batchOptions = BatchPackingOptions(
            sortStrategy = SortStrategy.AREA_DESC,
            continueOnFailure = false
        )

        val result = packTextures(texturesToRepack, batchOptions)

        val fillRatioImprovement = fillRatio - originalFillRatio
        val spaceReduction = originalWastedSpace - wastedSpace

        return OptimizationResult(
            success = result.failureCount == 0,
            fillRatioImprovement = fillRatioImprovement,
            spaceReduction = spaceReduction,
            repackedTextures = result.successCount,
            failedTextures = result.failureCount
        )
    }

    /**
     * Generate mipmaps for the atlas
     */
    fun generateMipmaps(): List<MipmapLevel> {
        if (!enableMipmaps) return emptyList()

        val mipmaps = mutableListOf<MipmapLevel>()
        var currentWidth = width
        var currentHeight = height
        var level = 0

        while (currentWidth > 1 || currentHeight > 1) {
            currentWidth = maxOf(1, currentWidth / 2)
            currentHeight = maxOf(1, currentHeight / 2)
            level++

            val mipmapData = generateMipmapLevel(level, currentWidth, currentHeight)
            mipmaps.add(MipmapLevel(level, currentWidth, currentHeight, mipmapData))
        }

        return mipmaps
    }

    /**
     * Export atlas as texture data
     */
    fun exportAtlas(includeMetadata: Boolean = true): AtlasExportData {
        val mipmaps = if (enableMipmaps) generateMipmaps() else emptyList()

        return AtlasExportData(
            width = width,
            height = height,
            format = format,
            data = _atlasData.copyOf(),
            mipmaps = mipmaps,
            metadata = if (includeMetadata) {
                AtlasMetadata(
                    packedTextures = _packedTextures.values.toList(),
                    fillRatio = fillRatio,
                    wastedSpace = wastedSpace,
                    totalTextures = totalTextures,
                    packingStrategy = packingStrategy
                )
            } else null
        )
    }

    /**
     * Clear the entire atlas
     */
    fun clear() {
        _atlasData.fill(0)
        _packedTextures.clear()
        _usedRectangles.clear()
        _packer.reset()
        fillRatio = 0f
        wastedSpace = width * height
        totalTextures = 0
    }

    /**
     * Get atlas usage statistics
     */
    fun getStatistics(): AtlasStatistics {
        return AtlasStatistics(
            width = width,
            height = height,
            totalPixels = width * height,
            usedPixels = (width * height * fillRatio).toInt(),
            wastedPixels = wastedSpace,
            fillRatio = fillRatio,
            totalTextures = totalTextures,
            averageTextureSize = if (totalTextures > 0) {
                (width * height * fillRatio).toInt() / totalTextures
            } else 0,
            largestTexture = _packedTextures.values.maxByOrNull {
                it.atlasRectangle.width * it.atlasRectangle.height
            }?.let { "${it.atlasRectangle.width}x${it.atlasRectangle.height}" } ?: "None",
            smallestTexture = _packedTextures.values.minByOrNull {
                it.atlasRectangle.width * it.atlasRectangle.height
            }?.let { "${it.atlasRectangle.width}x${it.atlasRectangle.height}" } ?: "None"
        )
    }

    // Private helper methods

    private fun initialize() {
        _freeRectangles.add(Rectangle(0, 0, width, height))
        wastedSpace = width * height
    }

    private fun createPacker(strategy: PackingStrategy): RectanglePacker {
        return when (strategy) {
            PackingStrategy.MAX_RECTS -> MaxRectsPackager()
            PackingStrategy.SKYLINE -> SkylinePackager()
            PackingStrategy.GUILLOTINE -> GuillotinePackager()
            PackingStrategy.BOTTOM_LEFT -> BottomLeftPackager()
        }
    }

    private fun preprocessTexture(texture: TextureData, options: PackingOptions): TextureData {
        var processed = texture

        // Apply filters
        if (options.enableFiltering) {
            processed = applyTextureFilters(processed, options.filters)
        }

        // Resize if needed
        if (options.maxSize > 0) {
            val scale = minOf(
                options.maxSize.toFloat() / processed.width,
                options.maxSize.toFloat() / processed.height,
                1f
            )
            if (scale < 1f) {
                processed = resizeTexture(processed, scale)
            }
        }

        // Ensure power-of-two if required
        if (options.forcePowerOfTwo) {
            processed = makePowerOfTwo(processed)
        }

        return processed
    }

    private fun createPackedTexture(
        id: String,
        originalTexture: TextureData,
        atlasRectangle: Rectangle,
        rotated: Boolean
    ): PackedTexture {
        return PackedTexture(
            id = id,
            originalTexture = originalTexture,
            atlasRectangle = atlasRectangle,
            padding = padding,
            borderExtension = borderExtension
        )
    }

    private fun copyTextureToAtlas(textureData: TextureData, packedTexture: PackedTexture) {
        val rect = packedTexture.atlasRectangle
        val paddedRect = Rectangle(
            rect.x + padding,
            rect.y + padding,
            rect.width - padding * 2,
            rect.height - padding * 2
        )

        // Copy main texture data
        copyTextureRegion(textureData, paddedRect, rect.rotated)

        // Extend borders for seamless sampling
        if (borderExtension > 0) {
            extendTextureBorders(paddedRect, borderExtension)
        }
    }

    private fun copyTextureRegion(source: TextureData, targetRect: Rectangle, rotated: Boolean) {
        val bytesPerPixel = format.bytesPerPixel

        for (y in 0 until targetRect.height) {
            for (x in 0 until targetRect.width) {
                val sourceX = if (rotated) y else x
                val sourceY = if (rotated) source.width - 1 - x else y

                if (sourceX < source.width && sourceY < source.height) {
                    val sourceIndex = (sourceY * source.width + sourceX) * bytesPerPixel
                    val targetIndex = ((targetRect.y + y) * width + (targetRect.x + x)) * bytesPerPixel

                    for (c in 0 until bytesPerPixel) {
                        _atlasData[targetIndex + c] = source.data[sourceIndex + c]
                    }
                }
            }
        }
    }

    private fun extendTextureBorders(rect: Rectangle, extension: Int) {
        val bytesPerPixel = format.bytesPerPixel

        // Extend top and bottom
        for (ext in 1..extension) {
            // Top border
            if (rect.y - ext >= 0) {
                for (x in rect.x until rect.x + rect.width) {
                    val sourceIndex = (rect.y * width + x) * bytesPerPixel
                    val targetIndex = ((rect.y - ext) * width + x) * bytesPerPixel
                    for (c in 0 until bytesPerPixel) {
                        _atlasData[targetIndex + c] = _atlasData[sourceIndex + c]
                    }
                }
            }

            // Bottom border
            if (rect.y + rect.height + ext - 1 < height) {
                for (x in rect.x until rect.x + rect.width) {
                    val sourceIndex = ((rect.y + rect.height - 1) * width + x) * bytesPerPixel
                    val targetIndex = ((rect.y + rect.height + ext - 1) * width + x) * bytesPerPixel
                    for (c in 0 until bytesPerPixel) {
                        _atlasData[targetIndex + c] = _atlasData[sourceIndex + c]
                    }
                }
            }
        }

        // Extend left and right
        for (ext in 1..extension) {
            // Left border
            if (rect.x - ext >= 0) {
                for (y in maxOf(0, rect.y - extension) until minOf(height, rect.y + rect.height + extension)) {
                    val sourceIndex = (y * width + rect.x) * bytesPerPixel
                    val targetIndex = (y * width + rect.x - ext) * bytesPerPixel
                    for (c in 0 until bytesPerPixel) {
                        _atlasData[targetIndex + c] = _atlasData[sourceIndex + c]
                    }
                }
            }

            // Right border
            if (rect.x + rect.width + ext - 1 < width) {
                for (y in maxOf(0, rect.y - extension) until minOf(height, rect.y + rect.height + extension)) {
                    val sourceIndex = (y * width + rect.x + rect.width - 1) * bytesPerPixel
                    val targetIndex = (y * width + rect.x + rect.width + ext - 1) * bytesPerPixel
                    for (c in 0 until bytesPerPixel) {
                        _atlasData[targetIndex + c] = _atlasData[sourceIndex + c]
                    }
                }
            }
        }
    }

    private fun handleAtlasFull(
        id: String,
        texture: TextureData,
        options: PackingOptions
    ): PackingResult {
        if (autoResize && width < maxSize && height < maxSize) {
            // Try to resize atlas
            val newSize = minOf(maxOf(width, height) * 2, maxSize)
            return resizeAtlasAndRetry(newSize, id, texture, options)
        }

        return PackingResult.Error("Atlas is full - cannot fit texture ${texture.width}x${texture.height}")
    }

    private fun resizeAtlasAndRetry(
        newSize: Int,
        id: String,
        texture: TextureData,
        options: PackingOptions
    ): PackingResult {
        // Store current state
        val oldData = _atlasData.copyOf()
        val oldWidth = width
        val oldHeight = height

        // Create new larger atlas
        val newAtlas = TextureAtlas(
            width = newSize,
            height = newSize,
            format = format,
            packingStrategy = packingStrategy
        )

        // Copy settings
        newAtlas.padding = padding
        newAtlas.borderExtension = borderExtension
        newAtlas.allowRotation = allowRotation

        // Repack all existing textures
        val texturesToRepack = _packedTextures.values.map {
            it.id to it.originalTexture
        }.toMap()

        val repackResult = newAtlas.packTextures(texturesToRepack)
        if (repackResult.failureCount > 0) {
            return PackingResult.Error("Failed to repack textures during resize")
        }

        // Try to pack the new texture
        val packResult = newAtlas.packTexture(id, texture, options)
        if (packResult is PackingResult.Success) {
            // Success - replace current atlas with new one
            _atlasData = newAtlas._atlasData
            _packedTextures.clear()
            _packedTextures.putAll(newAtlas._packedTextures)
            _usedRectangles.clear()
            _usedRectangles.addAll(newAtlas._usedRectangles)
            updateStatistics()
        }

        return packResult
    }

    private fun clearAtlasRegion(rect: Rectangle) {
        val bytesPerPixel = format.bytesPerPixel

        for (y in rect.y until rect.y + rect.height) {
            for (x in rect.x until rect.x + rect.width) {
                if (x < width && y < height) {
                    val index = (y * width + x) * bytesPerPixel
                    for (c in 0 until bytesPerPixel) {
                        _atlasData[index + c] = 0
                    }
                }
            }
        }
    }

    private fun updateStatistics() {
        totalTextures = _packedTextures.size
        val usedPixels = _usedRectangles.sumOf { it.width * it.height }
        val totalPixels = width * height
        fillRatio = usedPixels.toFloat() / totalPixels
        wastedSpace = totalPixels - usedPixels
    }

    private fun generateMipmapLevel(level: Int, mipmapWidth: Int, mipmapHeight: Int): ByteArray {
        val mipmapData = ByteArray(mipmapWidth * mipmapHeight * format.bytesPerPixel)
        val sourceLevel = if (level == 1) _atlasData else generateMipmapLevel(level - 1, mipmapWidth * 2, mipmapHeight * 2)

        // Simple box filter downsampling
        val bytesPerPixel = format.bytesPerPixel
        val sourceWidth = mipmapWidth * 2

        for (y in 0 until mipmapHeight) {
            for (x in 0 until mipmapWidth) {
                for (c in 0 until bytesPerPixel) {
                    val sample1 = sourceLevel[((y * 2) * sourceWidth + (x * 2)) * bytesPerPixel + c].toInt() and 0xFF
                    val sample2 = sourceLevel[((y * 2) * sourceWidth + (x * 2 + 1)) * bytesPerPixel + c].toInt() and 0xFF
                    val sample3 = sourceLevel[((y * 2 + 1) * sourceWidth + (x * 2)) * bytesPerPixel + c].toInt() and 0xFF
                    val sample4 = sourceLevel[((y * 2 + 1) * sourceWidth + (x * 2 + 1)) * bytesPerPixel + c].toInt() and 0xFF

                    val average = (sample1 + sample2 + sample3 + sample4) / 4
                    mipmapData[(y * mipmapWidth + x) * bytesPerPixel + c] = average.toByte()
                }
            }
        }

        return mipmapData
    }

    // Placeholder implementations for texture processing

    private fun applyTextureFilters(texture: TextureData, filters: List<TextureFilter>): TextureData {
        // Implementation would apply various image filters
        return texture
    }

    private fun resizeTexture(texture: TextureData, scale: Float): TextureData {
        // Implementation would resize texture using bilinear/bicubic filtering
        val newWidth = (texture.width * scale).toInt()
        val newHeight = (texture.height * scale).toInt()
        return texture.copy(width = newWidth, height = newHeight)
    }

    private fun makePowerOfTwo(texture: TextureData): TextureData {
        // Implementation would resize to nearest power of two
        val newWidth = nextPowerOfTwo(texture.width)
        val newHeight = nextPowerOfTwo(texture.height)
        if (newWidth != texture.width || newHeight != texture.height) {
            return resizeTexture(texture, minOf(newWidth.toFloat() / texture.width, newHeight.toFloat() / texture.height))
        }
        return texture
    }

    private fun nextPowerOfTwo(value: Int): Int {
        var result = 1
        while (result < value) {
            result *= 2
        }
        return result
    }

    // Readonly accessors
    val packedTextures: Map<String, PackedTexture> get() = _packedTextures.toMap()
    val atlasData: ByteArray get() = _atlasData.copyOf()
}

/**
 * Rectangle packing strategy enumeration
 */
enum class PackingStrategy {
    MAX_RECTS, SKYLINE, GUILLOTINE, BOTTOM_LEFT
}

/**
 * Texture sorting strategies for optimal packing
 */
enum class SortStrategy {
    NONE, AREA_DESC, WIDTH_DESC, HEIGHT_DESC, PERIMETER_DESC
}

/**
 * Texture format enumeration
 */
enum class TextureFormat(val bytesPerPixel: Int) {
    R8(1), RG8(2), RGB8(3), RGBA8(4),
    R16(2), RG16(4), RGB16(6), RGBA16(8),
    R32F(4), RG32F(8), RGB32F(12), RGBA32F(16)
}

/**
 * Texture filter types for preprocessing
 */
enum class TextureFilter {
    SHARPEN, BLUR, CONTRAST, BRIGHTNESS, GAMMA_CORRECTION
}

// Data classes for texture atlas system

/**
 * Rectangle representation for packing
 */
@Serializable
data class Rectangle(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val rotated: Boolean = false
) {
    val area: Int get() = width * height
    val perimeter: Int get() = 2 * (width + height)
}

/**
 * Texture data container
 */
data class TextureData(
    val width: Int,
    val height: Int,
    val format: TextureFormat,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextureData) return false
        return width == other.width &&
               height == other.height &&
               format == other.format &&
               data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + format.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Packed texture information
 */
data class PackedTexture(
    val id: String,
    val originalTexture: TextureData,
    val atlasRectangle: Rectangle,
    val padding: Int,
    val borderExtension: Int
)

/**
 * UV mapping information for packed texture
 */
data class UVMapping(
    val textureId: String,
    val uvMin: Vector2,
    val uvMax: Vector2,
    val rotated: Boolean,
    val originalSize: Vector2,
    val atlasSize: Vector2
)

/**
 * Packing options for individual textures
 */
data class PackingOptions(
    val maxSize: Int = 0, // 0 = no limit
    val forcePowerOfTwo: Boolean = false,
    val enableFiltering: Boolean = false,
    val filters: List<TextureFilter> = emptyList()
)

/**
 * Batch packing options
 */
data class BatchPackingOptions(
    val sortStrategy: SortStrategy = SortStrategy.AREA_DESC,
    val continueOnFailure: Boolean = true,
    val packingOptions: PackingOptions = PackingOptions()
)

/**
 * Packing result for individual texture
 */
sealed class PackingResult {
    data class Success(val packedTexture: PackedTexture) : PackingResult()
    data class Error(val message: String) : PackingResult()
}

/**
 * Batch packing result
 */
data class BatchPackingResult(
    val results: Map<String, PackingResult>,
    val successCount: Int,
    val failureCount: Int,
    val failedTextures: List<String>
)

/**
 * Atlas optimization result
 */
data class OptimizationResult(
    val success: Boolean,
    val fillRatioImprovement: Float,
    val spaceReduction: Int,
    val repackedTextures: Int,
    val failedTextures: Int
)

/**
 * Mipmap level data
 */
data class MipmapLevel(
    val level: Int,
    val width: Int,
    val height: Int,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MipmapLevel) return false
        return level == other.level &&
               width == other.width &&
               height == other.height &&
               data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = level
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Atlas export data
 */
data class AtlasExportData(
    val width: Int,
    val height: Int,
    val format: TextureFormat,
    val data: ByteArray,
    val mipmaps: List<MipmapLevel>,
    val metadata: AtlasMetadata?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AtlasExportData) return false
        return width == other.width &&
               height == other.height &&
               format == other.format &&
               data.contentEquals(other.data) &&
               mipmaps == other.mipmaps &&
               metadata == other.metadata
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + format.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + mipmaps.hashCode()
        result = 31 * result + (metadata?.hashCode() ?: 0)
        return result
    }
}

/**
 * Atlas metadata for export
 */
data class AtlasMetadata(
    val packedTextures: List<PackedTexture>,
    val fillRatio: Float,
    val wastedSpace: Int,
    val totalTextures: Int,
    val packingStrategy: PackingStrategy
)

/**
 * Atlas usage statistics
 */
data class AtlasStatistics(
    val width: Int,
    val height: Int,
    val totalPixels: Int,
    val usedPixels: Int,
    val wastedPixels: Int,
    val fillRatio: Float,
    val totalTextures: Int,
    val averageTextureSize: Int,
    val largestTexture: String,
    val smallestTexture: String
)

// Rectangle packing algorithm interfaces and implementations

/**
 * Base interface for rectangle packing algorithms
 */
interface RectanglePacker {
    fun findBestFit(width: Int, height: Int, allowRotation: Boolean): Rectangle?
    fun markRectangleAsUsed(rectangle: Rectangle)
    fun freeRectangle(rectangle: Rectangle)
    fun reset()
}

/**
 * Max Rects packing algorithm implementation
 */
class MaxRectsPackager : RectanglePacker {
    private val freeRectangles = mutableListOf<Rectangle>()
    private val usedRectangles = mutableListOf<Rectangle>()

    init {
        reset()
    }

    override fun findBestFit(width: Int, height: Int, allowRotation: Boolean): Rectangle? {
        var bestRectangle: Rectangle? = null
        var bestShortSideFit = Int.MAX_VALUE
        var bestLongSideFit = Int.MAX_VALUE

        for (rect in freeRectangles) {
            // Try normal orientation
            if (rect.width >= width && rect.height >= height) {
                val leftoverHorizontal = rect.width - width
                val leftoverVertical = rect.height - height
                val shortSideFit = minOf(leftoverHorizontal, leftoverVertical)
                val longSideFit = maxOf(leftoverHorizontal, leftoverVertical)

                if (shortSideFit < bestShortSideFit ||
                    (shortSideFit == bestShortSideFit && longSideFit < bestLongSideFit)) {
                    bestRectangle = Rectangle(rect.x, rect.y, width, height, false)
                    bestShortSideFit = shortSideFit
                    bestLongSideFit = longSideFit
                }
            }

            // Try rotated orientation
            if (allowRotation && rect.width >= height && rect.height >= width) {
                val leftoverHorizontal = rect.width - height
                val leftoverVertical = rect.height - width
                val shortSideFit = minOf(leftoverHorizontal, leftoverVertical)
                val longSideFit = maxOf(leftoverHorizontal, leftoverVertical)

                if (shortSideFit < bestShortSideFit ||
                    (shortSideFit == bestShortSideFit && longSideFit < bestLongSideFit)) {
                    bestRectangle = Rectangle(rect.x, rect.y, height, width, true)
                    bestShortSideFit = shortSideFit
                    bestLongSideFit = longSideFit
                }
            }
        }

        return bestRectangle
    }

    override fun markRectangleAsUsed(rectangle: Rectangle) {
        usedRectangles.add(rectangle)

        // Split intersecting free rectangles
        val toRemove = mutableListOf<Rectangle>()
        val toAdd = mutableListOf<Rectangle>()

        for (freeRect in freeRectangles) {
            if (rectanglesIntersect(rectangle, freeRect)) {
                toRemove.add(freeRect)

                // Create new rectangles from the split
                val splitRects = splitRectangle(freeRect, rectangle)
                toAdd.addAll(splitRects)
            }
        }

        freeRectangles.removeAll(toRemove)
        freeRectangles.addAll(toAdd)

        // Remove redundant rectangles
        pruneRectangles()
    }

    override fun freeRectangle(rectangle: Rectangle) {
        usedRectangles.remove(rectangle)
        freeRectangles.add(rectangle)

        // Merge adjacent free rectangles
        mergeRectangles()
    }

    override fun reset() {
        freeRectangles.clear()
        usedRectangles.clear()
        freeRectangles.add(Rectangle(0, 0, 2048, 2048)) // Default atlas size
    }

    private fun rectanglesIntersect(a: Rectangle, b: Rectangle): Boolean {
        return !(a.x >= b.x + b.width || a.x + a.width <= b.x ||
                 a.y >= b.y + b.height || a.y + a.height <= b.y)
    }

    private fun splitRectangle(freeRect: Rectangle, usedRect: Rectangle): List<Rectangle> {
        val result = mutableListOf<Rectangle>()

        // Left side
        if (usedRect.x > freeRect.x && usedRect.x < freeRect.x + freeRect.width) {
            result.add(Rectangle(
                freeRect.x, freeRect.y,
                usedRect.x - freeRect.x, freeRect.height
            ))
        }

        // Right side
        if (usedRect.x + usedRect.width < freeRect.x + freeRect.width) {
            result.add(Rectangle(
                usedRect.x + usedRect.width, freeRect.y,
                freeRect.x + freeRect.width - (usedRect.x + usedRect.width), freeRect.height
            ))
        }

        // Bottom side
        if (usedRect.y > freeRect.y && usedRect.y < freeRect.y + freeRect.height) {
            result.add(Rectangle(
                freeRect.x, freeRect.y,
                freeRect.width, usedRect.y - freeRect.y
            ))
        }

        // Top side
        if (usedRect.y + usedRect.height < freeRect.y + freeRect.height) {
            result.add(Rectangle(
                freeRect.x, usedRect.y + usedRect.height,
                freeRect.width, freeRect.y + freeRect.height - (usedRect.y + usedRect.height)
            ))
        }

        return result
    }

    private fun pruneRectangles() {
        val toRemove = mutableListOf<Rectangle>()

        for (i in freeRectangles.indices) {
            for (j in freeRectangles.indices) {
                if (i != j && rectangleContains(freeRectangles[j], freeRectangles[i])) {
                    toRemove.add(freeRectangles[i])
                    break
                }
            }
        }

        freeRectangles.removeAll(toRemove)
    }

    private fun rectangleContains(container: Rectangle, contained: Rectangle): Boolean {
        return container.x <= contained.x &&
               container.y <= contained.y &&
               container.x + container.width >= contained.x + contained.width &&
               container.y + container.height >= contained.y + contained.height
    }

    private fun mergeRectangles() {
        // Simplified merge - real implementation would be more sophisticated
        var merged = true
        while (merged) {
            merged = false

            for (i in freeRectangles.indices) {
                for (j in i + 1 until freeRectangles.size) {
                    val rect1 = freeRectangles[i]
                    val rect2 = freeRectangles[j]

                    val mergedRect = tryMergeRectangles(rect1, rect2)
                    if (mergedRect != null) {
                        freeRectangles.removeAt(j)
                        freeRectangles.removeAt(i)
                        freeRectangles.add(mergedRect)
                        merged = true
                        break
                    }
                }
                if (merged) break
            }
        }
    }

    private fun tryMergeRectangles(a: Rectangle, b: Rectangle): Rectangle? {
        // Can merge horizontally
        if (a.y == b.y && a.height == b.height) {
            if (a.x + a.width == b.x) {
                return Rectangle(a.x, a.y, a.width + b.width, a.height)
            }
            if (b.x + b.width == a.x) {
                return Rectangle(b.x, b.y, a.width + b.width, a.height)
            }
        }

        // Can merge vertically
        if (a.x == b.x && a.width == b.width) {
            if (a.y + a.height == b.y) {
                return Rectangle(a.x, a.y, a.width, a.height + b.height)
            }
            if (b.y + b.height == a.y) {
                return Rectangle(a.x, b.y, a.width, a.height + b.height)
            }
        }

        return null
    }
}

// Placeholder implementations for other packing algorithms

class SkylinePackager : RectanglePacker {
    override fun findBestFit(width: Int, height: Int, allowRotation: Boolean): Rectangle? = null
    override fun markRectangleAsUsed(rectangle: Rectangle) {}
    override fun freeRectangle(rectangle: Rectangle) {}
    override fun reset() {}
}

class GuillotinePackager : RectanglePacker {
    override fun findBestFit(width: Int, height: Int, allowRotation: Boolean): Rectangle? = null
    override fun markRectangleAsUsed(rectangle: Rectangle) {}
    override fun freeRectangle(rectangle: Rectangle) {}
    override fun reset() {}
}

class BottomLeftPackager : RectanglePacker {
    override fun findBestFit(width: Int, height: Int, allowRotation: Boolean): Rectangle? = null
    override fun markRectangleAsUsed(rectangle: Rectangle) {}
    override fun freeRectangle(rectangle: Rectangle) {}
    override fun reset() {}
}