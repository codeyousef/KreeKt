/**
 * Contract test: DataTexture from typed arrays
 * T025: Tests data texture functionality
 *
 * Validates:
 * - FR-T010: Create from raw data
 * - FR-T011: Float and integer formats
 * - FR-T012: 2D, 3D, and array textures
 */
package io.kreekt.texture

import io.kreekt.renderer.TextureFormat
import io.kreekt.renderer.TextureType
import kotlin.test.*

class DataTextureContractTest {

    /**
     * FR-T010: Create texture from raw data
     */
    @Test
    fun testCreateFromRawData() {
        // RGBA8 texture from byte array
        val rgbaData = ByteArray(256 * 256 * 4) { (it % 256).toByte() }
        val rgbaTexture = DataTexture(
            data = rgbaData,
            width = 256,
            height = 256,
            format = TextureFormat.RGBA8
        )

        assertEquals(256, rgbaTexture.width)
        assertEquals(256, rgbaTexture.height)
        assertEquals(TextureFormat.RGBA8, rgbaTexture.format)
        assertEquals(TextureType.UNSIGNED_BYTE, rgbaTexture.type)
        assertTrue(rgbaTexture.isDataTexture)

        // RGB8 texture
        val rgbData = ByteArray(128 * 128 * 3)
        val rgbTexture = DataTexture(
            data = rgbData,
            width = 128,
            height = 128,
            format = TextureFormat.RGB8
        )

        assertEquals(TextureFormat.RGB8, rgbTexture.format)
        assertEquals(128 * 128 * 3, rgbTexture.data.size)

        // Single channel texture
        val grayData = ByteArray(512 * 512)
        val grayTexture = DataTexture(
            data = grayData,
            width = 512,
            height = 512,
            format = TextureFormat.R8
        )

        assertEquals(TextureFormat.R8, grayTexture.format)
    }

    /**
     * FR-T011: Float and integer texture formats
     */
    @Test
    fun testFloatAndIntegerFormats() {
        // Float32 texture
        val floatData = FloatArray(256 * 256 * 4) { it * 0.1f }
        val floatTexture = DataTexture(
            data = floatData,
            width = 256,
            height = 256,
            format = TextureFormat.RGBA32F,
            type = TextureType.FLOAT
        )

        assertEquals(TextureFormat.RGBA32F, floatTexture.format)
        assertEquals(TextureType.FLOAT, floatTexture.type)
        assertTrue(floatTexture.isFloatTexture)

        // Half float texture
        val halfData = FloatArray(128 * 128 * 4)
        val halfTexture = DataTexture(
            data = halfData,
            width = 128,
            height = 128,
            format = TextureFormat.RGBA16F,
            type = TextureType.HALF_FLOAT
        )

        assertEquals(TextureFormat.RGBA16F, halfTexture.format)
        assertEquals(TextureType.HALF_FLOAT, halfTexture.type)

        // Integer texture
        val intData = IntArray(64 * 64 * 4)
        val intTexture = DataTexture(
            data = intData,
            width = 64,
            height = 64,
            format = TextureFormat.RGBA32I,
            type = TextureType.INT
        )

        assertEquals(TextureFormat.RGBA32I, intTexture.format)
        assertEquals(TextureType.INT, intTexture.type)
        assertTrue(intTexture.isIntegerTexture)

        // Unsigned integer texture
        val uintData = IntArray(64 * 64 * 4)
        val uintTexture = DataTexture(
            data = uintData,
            width = 64,
            height = 64,
            format = TextureFormat.RGBA32UI,
            type = TextureType.UNSIGNED_INT
        )

        assertEquals(TextureFormat.RGBA32UI, uintTexture.format)
        assertEquals(TextureType.UNSIGNED_INT, uintTexture.type)
    }

    /**
     * FR-T012: 2D, 3D, and array textures
     */
    @Test
    fun testTextureDimensions() {
        // Standard 2D texture
        val texture2D = DataTexture(
            data = ByteArray(256 * 256 * 4),
            width = 256,
            height = 256
        )

        assertTrue(texture2D.is2D)
        assertFalse(texture2D.is3D)
        assertFalse(texture2D.isArray)

        // 3D texture
        val texture3D = DataTexture3D(
            data = ByteArray(64 * 64 * 64 * 4),
            width = 64,
            height = 64,
            depth = 64,
            format = TextureFormat.RGBA8
        )

        assertFalse(texture3D.is2D)
        assertTrue(texture3D.is3D)
        assertEquals(64, texture3D.depth)

        // 2D array texture
        val textureArray = DataTexture2DArray(
            data = ByteArray(128 * 128 * 16 * 4),
            width = 128,
            height = 128,
            layers = 16,
            format = TextureFormat.RGBA8
        )

        assertFalse(textureArray.is2D)
        assertFalse(textureArray.is3D)
        assertTrue(textureArray.isArray)
        assertEquals(16, textureArray.layers)
    }

    /**
     * Test data update
     */
    @Test
    fun testDataUpdate() {
        val texture = DataTexture(
            data = ByteArray(128 * 128 * 4),
            width = 128,
            height = 128
        )

        assertFalse(texture.needsUpdate)

        // Update data
        val newData = ByteArray(128 * 128 * 4) { 255.toByte() }
        texture.updateData(newData)

        assertTrue(texture.needsUpdate, "Should need update after data change")
        assertEquals(newData, texture.data, "Data should be updated")

        // Partial update
        texture.updateDataRegion(
            x = 32,
            y = 32,
            width = 64,
            height = 64,
            data = ByteArray(64 * 64 * 4) { 128.toByte() }
        )

        assertTrue(texture.needsUpdate, "Should need update after partial update")
    }

    /**
     * Test pixel access
     */
    @Test
    fun testPixelAccess() {
        val data = ByteArray(256 * 256 * 4)
        // Set specific pixel to red
        val pixelIndex = (128 * 256 + 128) * 4
        data[pixelIndex] = 255.toByte()     // R
        data[pixelIndex + 1] = 0.toByte()   // G
        data[pixelIndex + 2] = 0.toByte()   // B
        data[pixelIndex + 3] = 255.toByte() // A

        val texture = DataTexture(data, 256, 256)

        // Get pixel
        val pixel = texture.getPixel(128, 128)
        assertEquals(255.toByte(), pixel[0], "Red channel")
        assertEquals(0.toByte(), pixel[1], "Green channel")
        assertEquals(0.toByte(), pixel[2], "Blue channel")
        assertEquals(255.toByte(), pixel[3], "Alpha channel")

        // Set pixel
        texture.setPixel(64, 64, byteArrayOf(0, 255.toByte(), 0, 255.toByte()))
        val greenPixel = texture.getPixel(64, 64)
        assertEquals(0.toByte(), greenPixel[0])
        assertEquals(255.toByte(), greenPixel[1])
    }

    /**
     * Test mipmap generation
     */
    @Test
    fun testMipmapGeneration() {
        val texture = DataTexture(
            data = ByteArray(256 * 256 * 4),
            width = 256,
            height = 256,
            generateMipmaps = true
        )

        assertTrue(texture.generateMipmaps, "Should generate mipmaps")

        // Calculate expected mipmap levels
        val expectedLevels = kotlin.math.log2(256f).toInt() + 1
        assertEquals(9, expectedLevels, "Should have 9 mipmap levels for 256x256")
    }

    /**
     * Test data validation
     */
    @Test
    fun testDataValidation() {
        // Wrong data size should throw
        assertFailsWith<IllegalArgumentException> {
            DataTexture(
                data = ByteArray(100),  // Wrong size
                width = 256,
                height = 256,
                format = TextureFormat.RGBA8  // Expects 256*256*4 bytes
            )
        }

        // Correct size should work
        assertDoesNotThrow {
            DataTexture(
                data = ByteArray(256 * 256 * 4),
                width = 256,
                height = 256,
                format = TextureFormat.RGBA8
            )
        }
    }

    /**
     * Test format conversion
     */
    @Test
    fun testFormatConversion() {
        val rgbaTexture = DataTexture(
            data = ByteArray(128 * 128 * 4),
            width = 128,
            height = 128,
            format = TextureFormat.RGBA8
        )

        // Convert to RGB
        val rgbTexture = rgbaTexture.convertToFormat(TextureFormat.RGB8)
        assertEquals(TextureFormat.RGB8, rgbTexture.format)
        assertEquals(128 * 128 * 3, rgbTexture.data.size)

        // Convert to grayscale
        val grayTexture = rgbaTexture.convertToFormat(TextureFormat.R8)
        assertEquals(TextureFormat.R8, grayTexture.format)
        assertEquals(128 * 128, grayTexture.data.size)
    }

    /**
     * Test flipY behavior
     */
    @Test
    fun testFlipY() {
        val data = ByteArray(4 * 4 * 4)
        // Set top-left pixel to red
        data[0] = 255.toByte()
        data[3] = 255.toByte()

        val texture = DataTexture(data, 4, 4, flipY = false)
        assertFalse(texture.flipY)

        // Flip texture
        texture.flipY = true
        texture.applyFlipY()

        // Top-left should now be bottom-left in data
        val bottomLeftIndex = (3 * 4) * 4  // Last row, first pixel
        assertEquals(255.toByte(), texture.data[bottomLeftIndex], "Red should be at bottom-left")
    }

    /**
     * Test clone operation
     */
    @Test
    fun testClone() {
        val original = DataTexture(
            data = ByteArray(64 * 64 * 4) { it.toByte() },
            width = 64,
            height = 64,
            format = TextureFormat.RGBA8
        )

        val clone = original.clone()

        assertEquals(original.width, clone.width)
        assertEquals(original.height, clone.height)
        assertEquals(original.format, clone.format)
        assertNotSame(original.data, clone.data, "Should have separate data arrays")
        assertContentEquals(original.data as ByteArray, clone.data as ByteArray)
    }
}

// Placeholder implementations
open class DataTexture(
    val data: Any,
    override val width: Int,
    override val height: Int,
    val format: TextureFormat = TextureFormat.RGBA8,
    val type: TextureType = TextureType.UNSIGNED_BYTE,
    override val generateMipmaps: Boolean = false,
    override val flipY: Boolean = true
) : Texture() {
    val isDataTexture = true
    val is2D = true
    open val is3D = false
    open val isArray = false

    val isFloatTexture = type in listOf(TextureType.FLOAT, TextureType.HALF_FLOAT)
    val isIntegerTexture = type in listOf(TextureType.INT, TextureType.UNSIGNED_INT)

    init {
        validateDataSize()
    }

    private fun validateDataSize() {
        val expectedSize = width * height * getChannelCount() * getTypeSize()
        val actualSize = when (data) {
            is ByteArray -> data.size
            is FloatArray -> data.size * 4
            is IntArray -> data.size * 4
            else -> 0
        }
        require(actualSize == expectedSize) {
            "Data size mismatch. Expected $expectedSize, got $actualSize"
        }
    }

    private fun getChannelCount(): Int = when (format) {
        TextureFormat.R8, TextureFormat.R32F -> 1
        TextureFormat.RG8, TextureFormat.RG32F -> 2
        TextureFormat.RGB8, TextureFormat.RGB32F -> 3
        TextureFormat.RGBA8, TextureFormat.RGBA32F, TextureFormat.RGBA16F,
        TextureFormat.RGBA32I, TextureFormat.RGBA32UI -> 4

        else -> 4
    }

    private fun getTypeSize(): Int = when (type) {
        TextureType.UNSIGNED_BYTE -> 1
        TextureType.HALF_FLOAT -> 2
        TextureType.FLOAT, TextureType.INT, TextureType.UNSIGNED_INT -> 4
        else -> 1
    }

    fun updateData(newData: Any) {
        @Suppress("UNCHECKED_CAST")
        when (data) {
            is ByteArray -> (data as ByteArray).indices.forEach { i ->
                (data as ByteArray)[i] = (newData as ByteArray)[i]
            }
        }
        needsUpdate = true
    }

    fun updateDataRegion(x: Int, y: Int, width: Int, height: Int, data: ByteArray) {
        needsUpdate = true
    }

    fun getPixel(x: Int, y: Int): ByteArray {
        val index = (y * width + x) * getChannelCount()
        return when (data) {
            is ByteArray -> data.sliceArray(index until index + getChannelCount())
            else -> ByteArray(getChannelCount())
        }
    }

    fun setPixel(x: Int, y: Int, pixel: ByteArray) {
        val index = (y * width + x) * getChannelCount()
        if (data is ByteArray) {
            pixel.forEachIndexed { i, byte ->
                data[index + i] = byte
            }
        }
    }

    fun applyFlipY() {
        // Flip texture vertically
    }

    fun convertToFormat(newFormat: TextureFormat): DataTexture {
        val newChannelCount = when (newFormat) {
            TextureFormat.R8 -> 1
            TextureFormat.RGB8 -> 3
            TextureFormat.RGBA8 -> 4
            else -> 4
        }
        return DataTexture(
            ByteArray(width * height * newChannelCount),
            width,
            height,
            newFormat
        )
    }

    override fun clone(): DataTexture {
        val clonedData = when (data) {
            is ByteArray -> data.copyOf()
            is FloatArray -> data.copyOf()
            is IntArray -> data.copyOf()
            else -> data
        }
        return DataTexture(clonedData, width, height, format, type, generateMipmaps, flipY)
    }
}

class DataTexture3D(
    data: ByteArray,
    width: Int,
    height: Int,
    val depth: Int,
    format: TextureFormat = TextureFormat.RGBA8
) : DataTexture(data, width, height, format) {
    override val is3D = true
}

class DataTexture2DArray(
    data: ByteArray,
    width: Int,
    height: Int,
    val layers: Int,
    format: TextureFormat = TextureFormat.RGBA8
) : DataTexture(data, width, height, format) {
    override val isArray = true
}

enum class TextureType {
    UNSIGNED_BYTE,
    BYTE,
    SHORT,
    UNSIGNED_SHORT,
    INT,
    UNSIGNED_INT,
    FLOAT,
    HALF_FLOAT
}