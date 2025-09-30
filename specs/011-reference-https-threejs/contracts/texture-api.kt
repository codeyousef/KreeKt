/**
 * Advanced Texture API Contract
 * Maps FR-T001 through FR-T020
 *
 * Constitutional Requirements:
 * - Platform-specific texture compression support
 * - Type-safe texture parameters
 * - Performance: <50ms texture upload, <200ms compression
 */

package io.kreekt.texture

import io.kreekt.math.Vector2
import io.kreekt.math.Matrix3
import io.kreekt.core.EventDispatcher

/**
 * FR-T001, FR-T002: Cube texture for environment mapping
 *
 * Test Contract:
 * - MUST load 6 images (px, nx, py, ny, pz, nz)
 * - MUST support cube map sampling
 * - MUST integrate with CubeCamera
 */
expect class CubeTexture(
    images: Array<TextureSource> = emptyArray(),
    mapping: CubeTextureMapping = CubeTextureMapping.CUBE_REFLECTION,
    wrapS: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    wrapT: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    magFilter: TextureFilter = TextureFilter.LINEAR,
    minFilter: TextureFilter = TextureFilter.LINEAR_MIPMAP_LINEAR,
    format: TextureFormat = TextureFormat.RGBA,
    type: TextureDataType = TextureDataType.UNSIGNED_BYTE,
    anisotropy: Int = 1
) : Texture {
    val images: Array<TextureSource>

    /**
     * Load cube texture from URLs
     */
    fun load(urls: Array<String>): CubeTexture

    /**
     * Set image for specific face
     *
     * @param index Face index (0=px, 1=nx, 2=py, 3=ny, 4=pz, 5=nz)
     */
    fun setFaceImage(index: Int, image: TextureSource)
}

/**
 * FR-T003, FR-T004: Video texture for dynamic content
 *
 * Test Contract:
 * - MUST play video as texture
 * - MUST update texture each frame
 * - MUST sync with video playback
 */
expect class VideoTexture(
    video: VideoElement,
    mapping: TextureMapping = TextureMapping.UV,
    wrapS: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    wrapT: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    magFilter: TextureFilter = TextureFilter.LINEAR,
    minFilter: TextureFilter = TextureFilter.LINEAR,
    format: TextureFormat = TextureFormat.RGBA,
    type: TextureDataType = TextureDataType.UNSIGNED_BYTE,
    anisotropy: Int = 1
) : Texture {
    val video: VideoElement

    /**
     * Update texture from current video frame
     *
     * Test Contract:
     * - MUST be called each frame
     * - MUST only update if video is playing
     * - MUST handle video seek
     */
    fun update()
}

/**
 * FR-T005, FR-T006: Canvas texture for procedural content
 *
 * Test Contract:
 * - MUST render canvas as texture
 * - MUST support manual updates
 * - MUST handle canvas resize
 */
expect class CanvasTexture(
    canvas: CanvasElement,
    mapping: TextureMapping = TextureMapping.UV,
    wrapS: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    wrapT: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    magFilter: TextureFilter = TextureFilter.LINEAR,
    minFilter: TextureFilter = TextureFilter.LINEAR_MIPMAP_LINEAR,
    format: TextureFormat = TextureFormat.RGBA,
    type: TextureDataType = TextureDataType.UNSIGNED_BYTE,
    anisotropy: Int = 1
) : Texture {
    val canvas: CanvasElement

    /**
     * Update texture from current canvas content
     */
    fun update()
}

/**
 * FR-T007, FR-T008, FR-T009: Compressed texture formats
 *
 * Test Contract:
 * - MUST support BC7, ETC2, ASTC, PVRTC
 * - MUST detect platform compression support
 * - MUST load compressed mipmaps
 * - MUST validate format compatibility
 */
expect class CompressedTexture(
    mipmaps: List<CompressedMipmap> = emptyList(),
    width: Int,
    height: Int,
    format: CompressedTextureFormat,
    type: TextureDataType = TextureDataType.UNSIGNED_BYTE,
    mapping: TextureMapping = TextureMapping.UV,
    wrapS: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    wrapT: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    magFilter: TextureFilter = TextureFilter.LINEAR,
    minFilter: TextureFilter = TextureFilter.LINEAR_MIPMAP_LINEAR,
    anisotropy: Int = 1
) : Texture {
    val mipmaps: List<CompressedMipmap>
    val format: CompressedTextureFormat

    /**
     * Add mipmap level
     */
    fun addMipmap(data: ByteArray, width: Int, height: Int)
}

/**
 * Compressed mipmap level
 */
data class CompressedMipmap(
    val data: ByteArray,
    val width: Int,
    val height: Int
)

/**
 * FR-T010: Data texture for typed arrays
 *
 * Test Contract:
 * - MUST create texture from raw data
 * - MUST support float and integer formats
 * - MUST support 1D, 2D, 3D textures
 */
expect class DataTexture(
    data: TypedArray? = null,
    width: Int = 1,
    height: Int = 1,
    format: TextureFormat = TextureFormat.RGBA,
    type: TextureDataType = TextureDataType.UNSIGNED_BYTE,
    mapping: TextureMapping = TextureMapping.UV,
    wrapS: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    wrapT: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    magFilter: TextureFilter = TextureFilter.NEAREST,
    minFilter: TextureFilter = TextureFilter.NEAREST,
    anisotropy: Int = 1
) : Texture {
    var data: TypedArray?

    /**
     * Update texture data
     */
    fun setData(data: TypedArray)
}

/**
 * FR-T011: 3D data texture
 *
 * Test Contract:
 * - MUST support volumetric data
 * - MUST support 3D texture sampling
 */
expect class DataTexture3D(
    data: TypedArray? = null,
    width: Int = 1,
    height: Int = 1,
    depth: Int = 1,
    format: TextureFormat = TextureFormat.RGBA,
    type: TextureDataType = TextureDataType.UNSIGNED_BYTE
) : Texture {
    var depth: Int
    var data: TypedArray?
}

/**
 * FR-T012: 2D texture array
 *
 * Test Contract:
 * - MUST support array of 2D textures
 * - MUST support indexed access in shaders
 */
expect class DataTexture2DArray(
    data: TypedArray? = null,
    width: Int = 1,
    height: Int = 1,
    depth: Int = 1,
    format: TextureFormat = TextureFormat.RGBA,
    type: TextureDataType = TextureDataType.UNSIGNED_BYTE
) : Texture {
    var depth: Int
    var data: TypedArray?
}

/**
 * FR-T013, FR-T014: Depth texture for shadow maps
 *
 * Test Contract:
 * - MUST store depth values
 * - MUST support shadow comparison
 * - MUST integrate with shadow mapping
 */
expect class DepthTexture(
    width: Int,
    height: Int,
    type: TextureDataType = TextureDataType.UNSIGNED_INT,
    mapping: TextureMapping = TextureMapping.UV,
    wrapS: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    wrapT: TextureWrapping = TextureWrapping.CLAMP_TO_EDGE,
    magFilter: TextureFilter = TextureFilter.NEAREST,
    minFilter: TextureFilter = TextureFilter.NEAREST,
    anisotropy: Int = 1
) : Texture {
    var compareFunction: CompareFunction?
}

/**
 * FR-T015: Texture loader
 *
 * Test Contract:
 * - MUST load textures asynchronously
 * - MUST decode image data
 * - MUST handle errors gracefully
 */
expect class TextureLoader {
    fun load(
        url: String,
        onLoad: ((Texture) -> Unit)? = null,
        onProgress: ((Float) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): Texture

    suspend fun loadAsync(url: String): Result<Texture>
}

/**
 * FR-T016: Compressed texture loader
 *
 * Test Contract:
 * - MUST load KTX2, DDS, PVR formats
 * - MUST detect compression format
 * - MUST load mipmaps
 */
expect class CompressedTextureLoader {
    fun load(
        url: String,
        onLoad: ((CompressedTexture) -> Unit)? = null,
        onProgress: ((Float) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): CompressedTexture

    suspend fun loadAsync(url: String): Result<CompressedTexture>
}

/**
 * FR-T017: Cube texture loader
 */
expect class CubeTextureLoader {
    fun load(
        urls: Array<String>,
        onLoad: ((CubeTexture) -> Unit)? = null,
        onProgress: ((Float) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): CubeTexture

    suspend fun loadAsync(urls: Array<String>): Result<CubeTexture>
}

/**
 * FR-T018: Equirectangular to cube map conversion
 *
 * Test Contract:
 * - MUST convert equirectangular image to cube map
 * - MUST preserve HDR data
 * - MUST generate mipmaps
 */
class EquirectangularToCubeGenerator(renderer: WebGPURenderer) {
    /**
     * Convert equirectangular texture to cube map
     */
    fun fromEquirectangular(
        texture: Texture,
        cubeSize: Int = 512
    ): CubeTexture
}

/**
 * FR-T019, FR-T020: Pre-filtered environment map generator (PMREM)
 *
 * Test Contract:
 * - MUST generate pre-filtered mipmaps for IBL
 * - MUST compute GGX distribution
 * - MUST support both cube and equirectangular input
 */
class PMREMGenerator(renderer: WebGPURenderer) {
    /**
     * Generate PMREM from cube texture
     */
    fun fromCubemap(cubemap: CubeTexture, samples: Int = 1024): CubeTexture

    /**
     * Generate PMREM from equirectangular texture
     */
    fun fromEquirectangular(texture: Texture, samples: Int = 1024): CubeTexture

    /**
     * Generate PMREM from scene
     */
    fun fromScene(scene: Scene, sigma: Float = 0f, near: Float = 0.1f, far: Float = 100f): CubeTexture

    /**
     * Compile PMREM shaders
     */
    fun compileCubemapShader()

    /**
     * Clean up resources
     */
    fun dispose()
}

// Enums and supporting types

enum class CubeTextureMapping {
    CUBE_REFLECTION,
    CUBE_REFRACTION,
    CUBE_UV
}

enum class TextureMapping {
    UV,
    CUBE_REFLECTION,
    CUBE_REFRACTION,
    EQUIRECTANGULAR,
    CUBE_UV
}

enum class TextureWrapping {
    REPEAT,
    CLAMP_TO_EDGE,
    MIRRORED_REPEAT
}

enum class TextureFilter {
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_NEAREST,
    LINEAR_MIPMAP_LINEAR
}

enum class TextureFormat {
    ALPHA,
    RGB,
    RGBA,
    LUMINANCE,
    LUMINANCE_ALPHA,
    DEPTH,
    DEPTH_STENCIL
}

enum class TextureDataType {
    UNSIGNED_BYTE,
    BYTE,
    SHORT,
    UNSIGNED_SHORT,
    INT,
    UNSIGNED_INT,
    FLOAT,
    HALF_FLOAT
}

enum class CompressedTextureFormat {
    // BC formats (DirectX/Vulkan)
    BC1_RGB,
    BC1_RGBA,
    BC2_RGBA,
    BC3_RGBA,
    BC4_R,
    BC5_RG,
    BC6H_RGB,
    BC7_RGBA,

    // ETC formats (OpenGL ES)
    ETC1_RGB,
    ETC2_RGB,
    ETC2_RGBA,

    // ASTC formats (Mobile)
    ASTC_4x4_RGBA,
    ASTC_5x5_RGBA,
    ASTC_6x6_RGBA,
    ASTC_8x8_RGBA,

    // PVRTC formats (iOS)
    PVRTC_RGB_4BPPV1,
    PVRTC_RGBA_4BPPV1
}

enum class CompareFunction {
    NEVER,
    LESS,
    EQUAL,
    LEQUAL,
    GREATER,
    NOTEQUAL,
    GEQUAL,
    ALWAYS
}

// Platform-specific types
expect class TextureSource
expect class VideoElement
expect class CanvasElement
expect class TypedArray
expect class WebGPURenderer
expect class Scene

/**
 * Base texture class (should already exist in kreekt-core)
 */
expect abstract class Texture : EventDispatcher {
    var id: Int
    var uuid: String
    var name: String

    var image: TextureSource?
    var mipmaps: List<Any>

    var mapping: TextureMapping
    var wrapS: TextureWrapping
    var wrapT: TextureWrapping

    var magFilter: TextureFilter
    var minFilter: TextureFilter

    var anisotropy: Int
    var format: TextureFormat
    var type: TextureDataType

    var offset: Vector2
    var repeat: Vector2
    var center: Vector2
    var rotation: Float

    var matrixAutoUpdate: Boolean
    var matrix: Matrix3

    var generateMipmaps: Boolean
    var premultiplyAlpha: Boolean
    var flipY: Boolean
    var unpackAlignment: Int

    var needsUpdate: Boolean

    fun updateMatrix()
    fun clone(): Texture
    fun dispose()
}