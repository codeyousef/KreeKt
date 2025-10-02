/**
 * Asset Loader System Contracts
 *
 * Feature: 013-double-check-and (Three.js r180 Feature Parity)
 * Category: Asset Loading and Management
 *
 * These contracts define the API surface for loading various 3D model formats,
 * textures, and other assets across all platforms using Kotlin coroutines.
 *
 * Requirements covered: FR-017 through FR-028
 */

package io.kreekt.loader

import io.kreekt.core.Result
import io.kreekt.core.scene.Scene
import io.kreekt.renderer.Texture
import io.kreekt.texture.CompressedTexture
import io.kreekt.texture.DataTexture
import kotlinx.coroutines.flow.StateFlow

/**
 * Base interface for all asset loaders.
 *
 * FR-027: All loaders MUST support async/await patterns using Kotlin coroutines
 * FR-028: All loaders MUST handle platform-specific file system access patterns
 *
 * @param T The type of asset this loader produces
 */
interface AssetLoader<T> {
    /**
     * Loads an asset from a URL or file path.
     *
     * @param url The URL or file path to load from
     * @param onProgress Optional callback for loading progress
     * @return Result containing the loaded asset or an error
     */
    suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)? = null
    ): Result<T>

    /**
     * Loads an asset from a byte array.
     *
     * @param bytes The raw asset data
     * @param onProgress Optional callback for loading progress
     * @return Result containing the loaded asset or an error
     */
    suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)? = null
    ): Result<T>

    /**
     * Parses already-loaded data into an asset.
     *
     * @param data The data to parse (format depends on loader)
     * @return Result containing the parsed asset or an error
     */
    suspend fun parse(data: Any): Result<T>
}

/**
 * Loading progress information.
 */
data class LoadingProgress(
    /**
     * Number of bytes loaded.
     */
    val loaded: Long,

    /**
     * Total number of bytes (0 if unknown).
     */
    val total: Long
) {
    /**
     * Progress as a percentage (0.0 to 1.0).
     */
    val percentage: Float
        get() = if (total > 0) loaded.toFloat() / total.toFloat() else 0f
}

/**
 * Centralized asset loading manager with progress tracking.
 *
 * FR-026: System MUST provide LoadingManager for centralized asset loading management with progress tracking
 *
 * @sample
 * ```kotlin
 * val manager = LoadingManager()
 * manager.onLoad = { println("All assets loaded!") }
 * manager.onProgress = { url, loaded, total ->
 *     println("Loading $url: $loaded / $total")
 * }
 * manager.onError = { url, error ->
 *     println("Failed to load $url: ${error.message}")
 * }
 *
 * val scene = manager.load("model.gltf").getOrThrow()
 * ```
 */
class LoadingManager {
    /**
     * Called when loading begins.
     */
    var onStart: ((url: String, loaded: Int, total: Int) -> Unit)? = null

    /**
     * Called when all items finish loading.
     */
    var onLoad: (() -> Unit)? = null

    /**
     * Called during loading progress.
     */
    var onProgress: ((url: String, loaded: Int, total: Int) -> Unit)? = null

    /**
     * Called when a load error occurs.
     */
    var onError: ((url: String, error: Throwable) -> Unit)? = null

    /**
     * Number of items currently loaded.
     */
    val itemsLoaded: StateFlow<Int>

    /**
     * Total number of items to load.
     */
    val itemsTotal: StateFlow<Int>

    /**
     * Whether loading is currently in progress.
     */
    val isLoading: StateFlow<Boolean>

    /**
     * Registers a loader for a file extension.
     *
     * @param extension File extension (without dot, e.g. "gltf")
     * @param loader The loader instance
     */
    fun registerLoader(extension: String, loader: AssetLoader<*>)

    /**
     * Gets the registered loader for an extension.
     *
     * @param extension File extension
     * @return The loader or null if not registered
     */
    fun getLoader(extension: String): AssetLoader<*>?

    /**
     * Loads an asset using the appropriate loader based on file extension.
     *
     * @param url URL or file path
     * @return Result containing the loaded asset
     */
    suspend fun load(url: String): Result<Any>

    /**
     * Adds an item to the cache.
     *
     * @param url The URL key
     * @param data The data to cache
     */
    fun addToCache(url: String, data: Any)

    /**
     * Retrieves an item from the cache.
     *
     * @param url The URL key
     * @return The cached data or null
     */
    fun getFromCache(url: String): Any?

    /**
     * Clears the entire cache.
     */
    fun clearCache()
}

/**
 * Loads FBX format models.
 *
 * FR-017: System MUST provide FBXLoader for FBX format models
 *
 * @sample
 * ```kotlin
 * val loader = FBXLoader()
 * val scene = loader.load("model.fbx") { progress ->
 *     println("Loading: ${progress.percentage * 100}%")
 * }.getOrThrow()
 *
 * sceneRoot.add(scene)
 * ```
 */
class FBXLoader : AssetLoader<Scene> {
    /**
     * Optional loading manager for coordinated loading.
     */
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Scene>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Scene>

    override suspend fun parse(data: Any): Result<Scene>
}

/**
 * Loads OBJ/MTL format models.
 *
 * FR-018: System MUST provide OBJLoader for OBJ/MTL format models
 *
 * @sample
 * ```kotlin
 * val loader = OBJLoader()
 * loader.setMaterialLoader(MTLLoader())
 * val scene = loader.load("model.obj").getOrThrow()
 * ```
 */
class OBJLoader : AssetLoader<Scene> {
    var manager: LoadingManager? = null

    /**
     * Sets the MTL loader for loading materials.
     *
     * @param mtlLoader The MTL loader instance
     */
    fun setMaterialLoader(mtlLoader: MTLLoader)

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Scene>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Scene>

    override suspend fun parse(data: Any): Result<Scene>
}

/**
 * Loads MTL material library files.
 */
class MTLLoader : AssetLoader<Map<String, io.kreekt.material.Material>> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Map<String, io.kreekt.material.Material>>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Map<String, io.kreekt.material.Material>>

    override suspend fun parse(data: Any): Result<Map<String, io.kreekt.material.Material>>
}

/**
 * Loads COLLADA (DAE) format models.
 *
 * FR-019: System MUST provide ColladaLoader for COLLADA (DAE) format models
 *
 * @sample
 * ```kotlin
 * val loader = ColladaLoader()
 * val result = loader.load("model.dae").getOrThrow()
 * val scene = result.scene
 * val animations = result.animations
 * ```
 */
class ColladaLoader : AssetLoader<ColladaLoaderResult> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<ColladaLoaderResult>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<ColladaLoaderResult>

    override suspend fun parse(data: Any): Result<ColladaLoaderResult>
}

/**
 * Result of loading a COLLADA file.
 */
data class ColladaLoaderResult(
    /**
     * The loaded scene hierarchy.
     */
    val scene: Scene,

    /**
     * Animation clips (if any).
     */
    val animations: List<io.kreekt.animation.AnimationClip>,

    /**
     * Kinematics information.
     */
    val kinematics: Map<String, Any>
)

/**
 * Loads STL format models (3D printing).
 *
 * FR-020: System MUST provide STLLoader for STL format (3D printing) models
 *
 * @sample
 * ```kotlin
 * val loader = STLLoader()
 * val geometry = loader.load("model.stl").getOrThrow()
 * val mesh = Mesh(geometry, MeshStandardMaterial())
 * ```
 */
class STLLoader : AssetLoader<io.kreekt.geometry.BufferGeometry> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<io.kreekt.geometry.BufferGeometry>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<io.kreekt.geometry.BufferGeometry>

    override suspend fun parse(data: Any): Result<io.kreekt.geometry.BufferGeometry>
}

/**
 * Loads PLY format models.
 *
 * FR-021: System MUST provide PLYLoader for PLY format models
 *
 * @sample
 * ```kotlin
 * val loader = PLYLoader()
 * val geometry = loader.load("model.ply").getOrThrow()
 * val mesh = Mesh(geometry, MeshStandardMaterial())
 * ```
 */
class PLYLoader : AssetLoader<io.kreekt.geometry.BufferGeometry> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<io.kreekt.geometry.BufferGeometry>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<io.kreekt.geometry.BufferGeometry>

    override suspend fun parse(data: Any): Result<io.kreekt.geometry.BufferGeometry>
}

/**
 * Loads 3DS format models.
 *
 * FR-022: System MUST provide 3DMLoader for 3DS format models
 */
class ThreeDSLoader : AssetLoader<Scene> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Scene>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Scene>

    override suspend fun parse(data: Any): Result<Scene>
}

/**
 * Loads USDZ format models (Apple AR).
 *
 * FR-023: System MUST provide USDZLoader for USDZ (Apple AR) format models
 *
 * @sample
 * ```kotlin
 * val loader = USDZLoader()
 * val scene = loader.load("model.usdz").getOrThrow()
 * // Compatible with AR Quick Look on iOS
 * ```
 */
class USDZLoader : AssetLoader<Scene> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Scene>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Scene>

    override suspend fun parse(data: Any): Result<Scene>
}

/**
 * Loads EXR high dynamic range textures.
 *
 * FR-024: System MUST provide texture format loaders (EXR, RGBE, TGA, KTX2, Basis, DDS, PVR)
 */
class EXRLoader : AssetLoader<DataTexture> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<DataTexture>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<DataTexture>

    override suspend fun parse(data: Any): Result<DataTexture>
}

/**
 * Loads RGBE (Radiance HDR) format textures.
 *
 * FR-024: System MUST provide texture format loaders (EXR, RGBE, TGA, KTX2, Basis, DDS, PVR)
 */
class RGBELoader : AssetLoader<DataTexture> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<DataTexture>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<DataTexture>

    override suspend fun parse(data: Any): Result<DataTexture>
}

/**
 * Loads TGA format textures.
 *
 * FR-024: System MUST provide texture format loaders (EXR, RGBE, TGA, KTX2, Basis, DDS, PVR)
 */
class TGALoader : AssetLoader<Texture> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Texture>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Texture>

    override suspend fun parse(data: Any): Result<Texture>
}

/**
 * Loads KTX2 format compressed textures.
 *
 * FR-024: System MUST provide texture format loaders (EXR, RGBE, TGA, KTX2, Basis, DDS, PVR)
 */
class KTX2Loader : AssetLoader<CompressedTexture> {
    var manager: LoadingManager? = null

    /**
     * Sets the transcoder for Basis Universal supercompression.
     *
     * @param transcoder The transcoder instance
     */
    fun setTranscoder(transcoder: BasisUniversalTranscoder)

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<CompressedTexture>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<CompressedTexture>

    override suspend fun parse(data: Any): Result<CompressedTexture>
}

/**
 * Loads Basis Universal format textures.
 *
 * FR-024: System MUST provide texture format loaders (EXR, RGBE, TGA, KTX2, Basis, DDS, PVR)
 */
class BasisTextureLoader : AssetLoader<CompressedTexture> {
    var manager: LoadingManager? = null

    /**
     * Sets the transcoder instance.
     */
    fun setTranscoder(transcoder: BasisUniversalTranscoder)

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<CompressedTexture>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<CompressedTexture>

    override suspend fun parse(data: Any): Result<CompressedTexture>
}

/**
 * Transcoder for Basis Universal compressed textures.
 */
expect class BasisUniversalTranscoder {
    /**
     * Initializes the transcoder (loads WASM module on Web, JNI on JVM).
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Transcodes Basis data to a platform-appropriate format.
     *
     * @param basisData The Basis Universal data
     * @param targetFormat The target texture format
     * @param mipLevel Mipmap level to transcode
     * @return Transcoded texture data
     */
    suspend fun transcode(
        basisData: ByteArray,
        targetFormat: io.kreekt.renderer.TextureFormat,
        mipLevel: Int = 0
    ): Result<ByteArray>

    /**
     * Gets information about a Basis texture without transcoding.
     *
     * @param basisData The Basis Universal data
     * @return Image information
     */
    fun getImageInfo(basisData: ByteArray): Result<BasisImageInfo>

    /**
     * Gets the supported transcode formats for this platform.
     */
    fun getSupportedTranscodeFormats(): Set<io.kreekt.renderer.TextureFormat>
}

/**
 * Information about a Basis Universal texture.
 */
data class BasisImageInfo(
    val width: Int,
    val height: Int,
    val hasAlpha: Boolean,
    val mipLevels: Int,
    val imageCount: Int,
    val isCompressed: Boolean
)

/**
 * Loads DDS format compressed textures.
 *
 * FR-024: System MUST provide texture format loaders (EXR, RGBE, TGA, KTX2, Basis, DDS, PVR)
 */
class DDSLoader : AssetLoader<CompressedTexture> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<CompressedTexture>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<CompressedTexture>

    override suspend fun parse(data: Any): Result<CompressedTexture>
}

/**
 * Loads PVR format compressed textures.
 *
 * FR-024: System MUST provide texture format loaders (EXR, RGBE, TGA, KTX2, Basis, DDS, PVR)
 */
class PVRLoader : AssetLoader<CompressedTexture> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<CompressedTexture>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<CompressedTexture>

    override suspend fun parse(data: Any): Result<CompressedTexture>
}

/**
 * Loads fonts for TextGeometry.
 *
 * FR-025: System MUST provide FontLoader for loading fonts for TextGeometry
 *
 * @sample
 * ```kotlin
 * val loader = FontLoader()
 * val font = loader.load("helvetiker_regular.typeface.json").getOrThrow()
 * val textGeometry = TextGeometry("Hello", TextGeometryOptions(font = font))
 * ```
 */
class FontLoader : AssetLoader<Font> {
    var manager: LoadingManager? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Font>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Font>

    override suspend fun parse(data: Any): Result<Font>
}

/**
 * Represents a loaded font for text geometry.
 */
data class Font(
    /**
     * Font data structure.
     */
    val data: FontData,

    /**
     * Generates shape paths for text.
     *
     * @param text The text to generate
     * @param size Font size
     * @param curveSegments Number of segments per curve
     * @return List of shape paths
     */
    fun generateShapes: (text: String, size: Float, curveSegments: Int) -> List<io.kreekt.geometry.Shape>
)

/**
 * Font data structure.
 */
data class FontData(
    val glyphs: Map<Char, Glyph>,
    val resolution: Int,
    val boundingBox: BoundingBox
)

/**
 * Glyph information.
 */
data class Glyph(
    val ha: Float, // horizontal advance
    val x_min: Float,
    val x_max: Float,
    val o: String // outline path commands
)

/**
 * Bounding box for font.
 */
data class BoundingBox(
    val yMin: Float,
    val yMax: Float
)
