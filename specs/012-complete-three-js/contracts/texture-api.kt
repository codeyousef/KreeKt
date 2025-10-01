/**
 * Texture API Contract
 *
 * This file defines the complete API surface for the texture subsystem,
 * ensuring Three.js r180 compatibility with Kotlin idioms.
 *
 * Contract Design Principles:
 * - Type-safe method signatures
 * - Data classes for immutable configuration
 * - Default parameters for common use cases
 * - Sealed classes for type hierarchies
 * - Extension functions for DSL builders
 */

package io.kreekt.contracts.texture

import io.kreekt.core.math.*
import io.kreekt.texture.*

// ============================================================================
// Core Texture API
// ============================================================================

/**
 * Texture: Base texture interface
 * Three.js equivalent: THREE.Texture
 */
interface TextureAPI {
    // Identity
    val id: Int
    val uuid: String
    var name: String
    val isTexture: Boolean

    // Image source
    var image: Any?  // HTMLImageElement, Canvas, Video, Data, etc.
    val mipmaps: List<Any>

    // Mapping
    var mapping: TextureMapping

    // Wrapping
    var wrapS: TextureWrapping
    var wrapT: TextureWrapping

    // Filtering
    var magFilter: TextureFilter
    var minFilter: TextureFilter
    var anisotropy: Int

    // Format and type
    var format: TextureFormat
    var internalFormat: String?
    var type: TextureDataType

    // Offset and repeat
    val offset: Vector2
    val repeat: Vector2
    val center: Vector2
    var rotation: Float

    // Matrix transform
    var matrixAutoUpdate: Boolean
    val matrix: Matrix3

    // Mipmap generation
    var generateMipmaps: Boolean
    var premultiplyAlpha: Boolean
    var flipY: Boolean
    var unpackAlignment: Int

    // Color space
    var colorSpace: ColorSpace

    // User data
    val userData: MutableMap<String, Any>
    var version: Int
    var needsUpdate: Boolean

    // Methods
    fun updateMatrix()
    fun clone(): Texture
    fun copy(source: Texture): Texture
    fun toJSON(meta: Any? = null): String
    fun dispose()
    fun transformUv(uv: Vector2): Vector2
}

// ============================================================================
// Texture Types
// ============================================================================

/**
 * CubeTexture: Cubemap texture for environment mapping
 * Three.js equivalent: THREE.CubeTexture
 */
interface CubeTextureAPI : TextureAPI {
    val isCubeTexture: Boolean
    override var image: Array<Any>  // 6 images for +X, -X, +Y, -Y, +Z, -Z
    override var mapping: CubeTextureMapping

    override fun clone(): CubeTexture
    override fun copy(source: Texture): CubeTexture
}

/**
 * CanvasTexture: Texture from HTML canvas
 * Three.js equivalent: THREE.CanvasTexture
 */
interface CanvasTextureAPI : TextureAPI {
    val isCanvasTexture: Boolean
    override var image: Any  // HTMLCanvasElement or OffscreenCanvas
    var needsUpdate: Boolean
}

/**
 * VideoTexture: Texture from HTML video element
 * Three.js equivalent: THREE.VideoTexture
 */
interface VideoTextureAPI : TextureAPI {
    val isVideoTexture: Boolean
    override var image: Any  // HTMLVideoElement
    fun update()
}

/**
 * DataTexture: Texture from raw pixel data
 * Three.js equivalent: THREE.DataTexture
 */
interface DataTextureAPI : TextureAPI {
    val isDataTexture: Boolean
    override var image: ImageData
}

data class ImageData(
    val data: ByteArray,
    val width: Int,
    val height: Int
)

/**
 * DataArrayTexture: 2D texture array
 * Three.js equivalent: THREE.DataArrayTexture
 */
interface DataArrayTextureAPI : TextureAPI {
    val isDataArrayTexture: Boolean
    override var image: ImageData3D
    var depth: Int

    fun setSize(width: Int, height: Int, depth: Int)
}

/**
 * Data3DTexture: 3D texture
 * Three.js equivalent: THREE.Data3DTexture
 */
interface Data3DTextureAPI : TextureAPI {
    val isData3DTexture: Boolean
    override var image: ImageData3D
    var depth: Int

    fun setSize(width: Int, height: Int, depth: Int)
}

data class ImageData3D(
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val depth: Int
)

/**
 * CompressedTexture: GPU-compressed texture
 * Three.js equivalent: THREE.CompressedTexture
 */
interface CompressedTextureAPI : TextureAPI {
    val isCompressedTexture: Boolean
    override var image: CompressedMipmap
    override val mipmaps: List<CompressedMipmap>
    var compressionFormat: CompressedTextureFormat
}

data class CompressedMipmap(
    val data: ByteArray,
    val width: Int,
    val height: Int
)

/**
 * CompressedArrayTexture: Compressed 2D texture array
 * Three.js equivalent: THREE.CompressedArrayTexture
 */
interface CompressedArrayTextureAPI : CompressedTextureAPI {
    val isCompressedArrayTexture: Boolean
    var depth: Int
}

/**
 * CompressedCubeTexture: Compressed cubemap texture
 * Three.js equivalent: THREE.CompressedCubeTexture
 */
interface CompressedCubeTextureAPI : CompressedTextureAPI {
    val isCompressedCubeTexture: Boolean
    val isCubeTexture: Boolean
    override var image: Array<Any>
}

/**
 * DepthTexture: Depth buffer texture
 * Three.js equivalent: THREE.DepthTexture
 */
interface DepthTextureAPI : TextureAPI {
    val isDepthTexture: Boolean
    override var image: ImageData
    override var format: DepthTextureFormat
    override var type: DepthTextureDataType
    var compareFunction: CompareFunction?
}

/**
 * FramebufferTexture: Texture from framebuffer
 * Three.js equivalent: THREE.FramebufferTexture
 */
interface FramebufferTextureAPI : TextureAPI {
    val isFramebufferTexture: Boolean
    var needsUpdate: Boolean
}

// ============================================================================
// Texture Sources
// ============================================================================

/**
 * Source: Texture data source abstraction
 * Three.js equivalent: THREE.Source
 */
interface SourceAPI {
    val data: Any
    var needsUpdate: Boolean
    val uuid: String

    fun toJSON(meta: Any? = null): String
}

// ============================================================================
// Texture Enums and Constants
// ============================================================================

enum class TextureMapping {
    UVMapping,
    CubeReflectionMapping,
    CubeRefractionMapping,
    EquirectangularReflectionMapping,
    EquirectangularRefractionMapping,
    CubeUVReflectionMapping
}

typealias CubeTextureMapping = TextureMapping

enum class TextureWrapping {
    RepeatWrapping,
    ClampToEdgeWrapping,
    MirroredRepeatWrapping
}

enum class TextureFilter {
    NearestFilter,
    NearestMipmapNearestFilter,
    NearestMipmapLinearFilter,
    LinearFilter,
    LinearMipmapNearestFilter,
    LinearMipmapLinearFilter
}

enum class TextureFormat {
    AlphaFormat,
    RedFormat,
    RedIntegerFormat,
    RGFormat,
    RGIntegerFormat,
    RGBFormat,
    RGBIntegerFormat,
    RGBAFormat,
    RGBAIntegerFormat,
    LuminanceFormat,
    LuminanceAlphaFormat,
    DepthFormat,
    DepthStencilFormat
}

typealias DepthTextureFormat = TextureFormat

enum class TextureDataType {
    UnsignedByteType,
    ByteType,
    ShortType,
    UnsignedShortType,
    IntType,
    UnsignedIntType,
    FloatType,
    HalfFloatType,
    UnsignedShort4444Type,
    UnsignedShort5551Type,
    UnsignedInt248Type,
    UnsignedInt5999Type
}

typealias DepthTextureDataType = TextureDataType

enum class CompressedTextureFormat {
    // S3TC/DXT
    RGB_S3TC_DXT1_Format,
    RGBA_S3TC_DXT1_Format,
    RGBA_S3TC_DXT3_Format,
    RGBA_S3TC_DXT5_Format,

    // PVRTC
    RGB_PVRTC_4BPPV1_Format,
    RGB_PVRTC_2BPPV1_Format,
    RGBA_PVRTC_4BPPV1_Format,
    RGBA_PVRTC_2BPPV1_Format,

    // ETC
    RGB_ETC1_Format,
    RGB_ETC2_Format,
    RGBA_ETC2_EAC_Format,

    // ASTC
    RGBA_ASTC_4x4_Format,
    RGBA_ASTC_5x4_Format,
    RGBA_ASTC_5x5_Format,
    RGBA_ASTC_6x5_Format,
    RGBA_ASTC_6x6_Format,
    RGBA_ASTC_8x5_Format,
    RGBA_ASTC_8x6_Format,
    RGBA_ASTC_8x8_Format,
    RGBA_ASTC_10x5_Format,
    RGBA_ASTC_10x6_Format,
    RGBA_ASTC_10x8_Format,
    RGBA_ASTC_10x10_Format,
    RGBA_ASTC_12x10_Format,
    RGBA_ASTC_12x12_Format,

    // BPTC
    RGBA_BPTC_Format,
    RGB_BPTC_SIGNED_Format,
    RGB_BPTC_UNSIGNED_Format,

    // RGTC
    RED_RGTC1_Format,
    SIGNED_RED_RGTC1_Format,
    RED_GREEN_RGTC2_Format,
    SIGNED_RED_GREEN_RGTC2_Format
}

enum class ColorSpace {
    NoColorSpace,
    SRGBColorSpace,
    LinearSRGBColorSpace,
    DisplayP3ColorSpace
}

enum class CompareFunction {
    NeverCompare,
    LessCompare,
    EqualCompare,
    LessEqualCompare,
    GreaterCompare,
    NotEqualCompare,
    GreaterEqualCompare,
    AlwaysCompare
}

// ============================================================================
// Render Targets
// ============================================================================

/**
 * RenderTarget: Offscreen rendering target
 * Three.js equivalent: THREE.WebGLRenderTarget (as WebGPURenderTarget)
 */
interface RenderTargetAPI {
    val width: Int
    val height: Int
    val depth: Int
    val scissor: Vector4
    var scissorTest: Boolean
    val viewport: Vector4
    val texture: Texture
    val depthBuffer: Boolean
    val stencilBuffer: Boolean
    val depthTexture: DepthTexture?
    var samples: Int

    fun setSize(width: Int, height: Int, depth: Int = 1)
    fun clone(): RenderTarget
    fun copy(source: RenderTarget): RenderTarget
    fun dispose()
}

/**
 * CubeRenderTarget: Cubemap render target
 * Three.js equivalent: THREE.WebGLCubeRenderTarget
 */
interface CubeRenderTargetAPI : RenderTargetAPI {
    override val texture: CubeTexture
    fun fromEquirectangularTexture(renderer: Renderer, texture: Texture): CubeRenderTarget
}

/**
 * ArrayRenderTarget: 2D array render target
 */
interface ArrayRenderTargetAPI : RenderTargetAPI {
    override var depth: Int
    override val texture: DataArrayTexture
}

/**
 * 3DRenderTarget: 3D volume render target
 */
interface ThreeDRenderTargetAPI : RenderTargetAPI {
    override var depth: Int
    override val texture: Data3DTexture
}

// ============================================================================
// Factory Functions (Kotlin Idiom)
// ============================================================================

/**
 * DSL builder for Texture
 */
fun texture(
    image: Any? = null,
    mapping: TextureMapping = TextureMapping.UVMapping,
    init: Texture.() -> Unit = {}
): Texture {
    val tex = Texture(image, mapping)
    tex.init()
    return tex
}

/**
 * DSL builder for CubeTexture
 */
fun cubeTexture(
    images: Array<Any> = emptyArray(),
    mapping: CubeTextureMapping = TextureMapping.CubeReflectionMapping,
    init: CubeTexture.() -> Unit = {}
): CubeTexture {
    val tex = CubeTexture(images, mapping)
    tex.init()
    return tex
}

/**
 * DSL builder for DataTexture
 */
fun dataTexture(
    data: ByteArray,
    width: Int,
    height: Int,
    format: TextureFormat = TextureFormat.RGBAFormat,
    type: TextureDataType = TextureDataType.UnsignedByteType,
    init: DataTexture.() -> Unit = {}
): DataTexture {
    val tex = DataTexture(ImageData(data, width, height), format, type)
    tex.init()
    return tex
}

/**
 * DSL builder for RenderTarget
 */
fun renderTarget(
    width: Int,
    height: Int,
    init: RenderTarget.() -> Unit = {}
): RenderTarget {
    val rt = RenderTarget(width, height)
    rt.init()
    return rt
}

/**
 * Extension function to configure texture for normal maps
 */
fun Texture.asNormalMap(): Texture {
    colorSpace = ColorSpace.LinearSRGBColorSpace
    format = TextureFormat.RGBAFormat
    return this
}

/**
 * Extension function to configure texture for data storage
 */
fun Texture.asDataTexture(): Texture {
    colorSpace = ColorSpace.LinearSRGBColorSpace
    minFilter = TextureFilter.NearestFilter
    magFilter = TextureFilter.NearestFilter
    generateMipmaps = false
    return this
}

// ============================================================================
// Usage Examples
// ============================================================================

/**
 * Example: Create texture with repeat
 */
fun exampleRepeatingTexture(image: Any): Texture {
    return texture(image) {
        wrapS = TextureWrapping.RepeatWrapping
        wrapT = TextureWrapping.RepeatWrapping
        repeat.set(4f, 4f)
    }
}

/**
 * Example: Create environment cubemap
 */
fun exampleEnvironmentCubemap(
    px: Any, nx: Any,
    py: Any, ny: Any,
    pz: Any, nz: Any
): CubeTexture {
    return cubeTexture(arrayOf(px, nx, py, ny, pz, nz)) {
        mapping = TextureMapping.CubeReflectionMapping
        colorSpace = ColorSpace.SRGBColorSpace
    }
}

/**
 * Example: Create procedural noise texture
 */
fun exampleNoiseTexture(width: Int, height: Int): DataTexture {
    val size = width * height
    val data = ByteArray(size * 4)

    for (i in 0 until size) {
        val stride = i * 4
        val noise = (Math.random() * 255).toInt().toByte()
        data[stride] = noise
        data[stride + 1] = noise
        data[stride + 2] = noise
        data[stride + 3] = 255.toByte()
    }

    return dataTexture(data, width, height) {
        minFilter = TextureFilter.LinearFilter
        magFilter = TextureFilter.LinearFilter
        wrapS = TextureWrapping.RepeatWrapping
        wrapT = TextureWrapping.RepeatWrapping
    }
}

/**
 * Example: Create render target for post-processing
 */
fun examplePostProcessingTarget(width: Int, height: Int): RenderTarget {
    return renderTarget(width, height) {
        texture.apply {
            minFilter = TextureFilter.LinearFilter
            magFilter = TextureFilter.LinearFilter
            format = TextureFormat.RGBAFormat
            colorSpace = ColorSpace.LinearSRGBColorSpace
        }
        samples = 4  // MSAA
    }
}

/**
 * Example: Create depth texture for shadow mapping
 */
fun exampleDepthTexture(width: Int, height: Int): DepthTexture {
    return DepthTexture(width, height).apply {
        format = TextureFormat.DepthFormat
        type = TextureDataType.UnsignedIntType
        minFilter = TextureFilter.NearestFilter
        magFilter = TextureFilter.NearestFilter
    }
}

/**
 * Example: Create video texture
 */
fun exampleVideoTexture(video: Any): VideoTexture {
    return VideoTexture(video).apply {
        minFilter = TextureFilter.LinearFilter
        magFilter = TextureFilter.LinearFilter
        format = TextureFormat.RGBAFormat
        colorSpace = ColorSpace.SRGBColorSpace
    }
}

/**
 * Example: Create compressed texture (for mobile)
 */
fun exampleCompressedTexture(
    mipmaps: List<CompressedMipmap>,
    width: Int,
    height: Int,
    format: CompressedTextureFormat
): CompressedTexture {
    return CompressedTexture(mipmaps, width, height, format).apply {
        minFilter = TextureFilter.LinearMipmapLinearFilter
        magFilter = TextureFilter.LinearFilter
        generateMipmaps = false
        needsUpdate = true
    }
}
