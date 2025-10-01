/**
 * Loader API Contract
 *
 * This file defines the complete API surface for the asset loading subsystem,
 * ensuring Three.js r180 compatibility with Kotlin idioms.
 *
 * Contract Design Principles:
 * - Type-safe method signatures
 * - Data classes for immutable configuration
 * - Default parameters for common use cases
 * - Sealed classes for type hierarchies
 * - Extension functions for DSL builders
 */

package io.kreekt.contracts.loader

import io.kreekt.core.math.*
import io.kreekt.loader.*
import io.kreekt.scene.*
import io.kreekt.geometry.*
import io.kreekt.material.*
import io.kreekt.texture.*
import io.kreekt.animation.*

// ============================================================================
// Core Loader API
// ============================================================================

/**
 * Loader: Base loader interface
 * Three.js equivalent: THREE.Loader
 */
interface LoaderAPI {
    var crossOrigin: String
    var withCredentials: Boolean
    var path: String
    var resourcePath: String
    var requestHeader: Map<String, String>
    val manager: LoadingManager

    fun load(
        url: String,
        onLoad: ((Any) -> Unit)? = null,
        onProgress: ((ProgressEvent) -> Unit)? = null,
        onError: ((Error) -> Unit)? = null
    )

    fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)? = null): Any

    fun setCrossOrigin(crossOrigin: String): Loader
    fun setWithCredentials(value: Boolean): Loader
    fun setPath(path: String): Loader
    fun setResourcePath(resourcePath: String): Loader
    fun setRequestHeader(requestHeader: Map<String, String>): Loader
}

/**
 * LoadingManager: Coordinates loading of multiple assets
 * Three.js equivalent: THREE.LoadingManager
 */
interface LoadingManagerAPI {
    var onStart: ((url: String, itemsLoaded: Int, itemsTotal: Int) -> Unit)?
    var onLoad: (() -> Unit)?
    var onProgress: ((url: String, itemsLoaded: Int, itemsTotal: Int) -> Unit)?
    var onError: ((url: String) -> Unit)?

    fun itemStart(url: String)
    fun itemEnd(url: String)
    fun itemError(url: String)
    fun resolveURL(url: String): String
    fun setURLModifier(callback: ((url: String) -> String)?): LoadingManager
    fun addHandler(regex: Regex, loader: Loader): LoadingManager
    fun removeHandler(regex: Regex): LoadingManager
    fun getHandler(file: String): Loader?
}

/**
 * FileLoader: Low-level file loading
 * Three.js equivalent: THREE.FileLoader
 */
interface FileLoaderAPI : LoaderAPI {
    var mimeType: String?
    var responseType: String

    fun setMimeType(mimeType: String): FileLoader
    fun setResponseType(responseType: String): FileLoader
}

// ============================================================================
// Texture Loaders
// ============================================================================

/**
 * TextureLoader: Load image textures
 * Three.js equivalent: THREE.TextureLoader
 */
interface TextureLoaderAPI : LoaderAPI {
    override fun load(
        url: String,
        onLoad: ((Texture) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): Texture
}

/**
 * CubeTextureLoader: Load cubemap textures
 * Three.js equivalent: THREE.CubeTextureLoader
 */
interface CubeTextureLoaderAPI : LoaderAPI {
    fun load(
        urls: Array<String>,
        onLoad: ((CubeTexture) -> Unit)? = null,
        onProgress: ((ProgressEvent) -> Unit)? = null,
        onError: ((Error) -> Unit)? = null
    )

    fun loadAsync(urls: Array<String>, onProgress: ((ProgressEvent) -> Unit)?): CubeTexture
}

/**
 * CompressedTextureLoader: Load compressed textures
 * Three.js equivalent: THREE.CompressedTextureLoader
 */
interface CompressedTextureLoaderAPI : LoaderAPI {
    override fun load(
        url: String,
        onLoad: ((CompressedTexture) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): CompressedTexture
}

/**
 * DataTextureLoader: Load raw texture data
 * Three.js equivalent: THREE.DataTextureLoader
 */
interface DataTextureLoaderAPI : LoaderAPI {
    override fun load(
        url: String,
        onLoad: ((DataTexture) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): DataTexture
}

// ============================================================================
// 3D Model Loaders
// ============================================================================

/**
 * GLTFLoader: Load glTF 2.0 models
 * Three.js equivalent: THREE.GLTFLoader
 */
interface GLTFLoaderAPI : LoaderAPI {
    var dracoLoader: DRACOLoader?
    var ktx2Loader: KTX2Loader?
    var meshoptDecoder: Any?

    fun setDRACOLoader(dracoLoader: DRACOLoader): GLTFLoader
    fun setKTX2Loader(ktx2Loader: KTX2Loader): GLTFLoader
    fun setMeshoptDecoder(meshoptDecoder: Any): GLTFLoader

    override fun load(
        url: String,
        onLoad: ((GLTF) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): GLTF

    fun parse(
        data: Any,
        path: String,
        onLoad: (GLTF) -> Unit,
        onError: ((Error) -> Unit)? = null
    )

    fun parseAsync(data: Any, path: String): GLTF
}

data class GLTF(
    val scene: Scene,
    val scenes: List<Scene>,
    val cameras: List<Camera>,
    val animations: List<AnimationClip>,
    val asset: GLTFAsset,
    val parser: GLTFParser,
    val userData: MutableMap<String, Any>
)

data class GLTFAsset(
    val version: String,
    val generator: String?,
    val copyright: String?,
    val minVersion: String?
)

interface GLTFParser {
    val json: Any
    val extensions: Map<String, Any>
    val options: GLTFLoaderOptions
    fun getDependency(type: String, index: Int): Any
    fun getDependencies(type: String): List<Any>
}

data class GLTFLoaderOptions(
    val path: String = "",
    val crossOrigin: String = "anonymous",
    val requestHeader: Map<String, String> = emptyMap(),
    val manager: LoadingManager? = null
)

/**
 * FBXLoader: Load Autodesk FBX models
 * Three.js equivalent: THREE.FBXLoader
 */
interface FBXLoaderAPI : LoaderAPI {
    override fun load(
        url: String,
        onLoad: ((Group) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): Group

    fun parse(fbxData: Any, path: String): Group
}

/**
 * OBJLoader: Load Wavefront OBJ models
 * Three.js equivalent: THREE.OBJLoader
 */
interface OBJLoaderAPI : LoaderAPI {
    var materials: MaterialLibrary?

    fun setMaterials(materials: MaterialLibrary): OBJLoader

    override fun load(
        url: String,
        onLoad: ((Group) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): Group

    fun parse(text: String): Group
}

/**
 * MTLLoader: Load OBJ material library
 * Three.js equivalent: THREE.MTLLoader
 */
interface MTLLoaderAPI : LoaderAPI {
    var materialOptions: MaterialOptions

    override fun load(
        url: String,
        onLoad: ((MaterialLibrary) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): MaterialLibrary

    fun parse(text: String): MaterialLibrary
    fun setMaterialOptions(options: MaterialOptions): MTLLoader
}

data class MaterialOptions(
    val side: Side = Side.FrontSide,
    val wrap: TextureWrapping = TextureWrapping.RepeatWrapping,
    val normalizeRGB: Boolean = false,
    val ignoreZeroRGBs: Boolean = false,
    val invertTrProperty: Boolean = false
)

interface MaterialLibrary {
    val materials: Map<String, Material>
    fun preload()
    fun getAsArray(): List<Material>
    fun create(materialName: String): Material
}

/**
 * ColladaLoader: Load COLLADA DAE models
 * Three.js equivalent: THREE.ColladaLoader
 */
interface ColladaLoaderAPI : LoaderAPI {
    override fun load(
        url: String,
        onLoad: ((Collada) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): Collada

    fun parse(text: String, path: String): Collada
}

data class Collada(
    val scene: Scene,
    val scenes: List<Scene>,
    val cameras: List<Camera>,
    val lights: List<Light>,
    val animations: List<AnimationClip>,
    val library: ColladaLibrary
)

data class ColladaLibrary(
    val images: Map<String, Any>,
    val effects: Map<String, Any>,
    val materials: Map<String, Material>,
    val geometries: Map<String, BufferGeometry>,
    val controllers: Map<String, Any>,
    val nodes: Map<String, Object3D>
)

/**
 * STLLoader: Load STL models (3D printing)
 * Three.js equivalent: THREE.STLLoader
 */
interface STLLoaderAPI : LoaderAPI {
    override fun load(
        url: String,
        onLoad: ((BufferGeometry) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): BufferGeometry

    fun parse(data: Any): BufferGeometry
}

/**
 * PLYLoader: Load PLY models (point clouds)
 * Three.js equivalent: THREE.PLYLoader
 */
interface PLYLoaderAPI : LoaderAPI {
    var propertyNameMapping: Map<String, String>

    fun setPropertyNameMapping(mapping: Map<String, String>): PLYLoader

    override fun load(
        url: String,
        onLoad: ((BufferGeometry) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): BufferGeometry

    fun parse(data: Any): BufferGeometry
}

// ============================================================================
// Compression Loaders
// ============================================================================

/**
 * DRACOLoader: Load Draco compressed geometry
 * Three.js equivalent: THREE.DRACOLoader
 */
interface DRACOLoaderAPI : LoaderAPI {
    var decoderPath: String
    var decoderConfig: DRACODecoderConfig
    var workerLimit: Int

    fun setDecoderPath(path: String): DRACOLoader
    fun setDecoderConfig(config: DRACODecoderConfig): DRACOLoader
    fun setWorkerLimit(workerLimit: Int): DRACOLoader
    fun preload(): DRACOLoader
    fun dispose(): DRACOLoader

    override fun load(
        url: String,
        onLoad: ((BufferGeometry) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): BufferGeometry

    fun decodeGeometry(
        buffer: ByteArray,
        taskConfig: DRACOTaskConfig
    ): BufferGeometry
}

data class DRACODecoderConfig(
    val type: String = "js"  // "js" or "wasm"
)

data class DRACOTaskConfig(
    val attributeIDs: Map<String, Int> = emptyMap(),
    val attributeTypes: Map<String, String> = emptyMap(),
    val useUniqueIDs: Boolean = false
)

/**
 * KTX2Loader: Load KTX2 compressed textures
 * Three.js equivalent: THREE.KTX2Loader
 */
interface KTX2LoaderAPI : LoaderAPI {
    var transcoderPath: String
    var transcoderBinary: ByteArray?
    var transcoderPending: Any?
    var workerLimit: Int
    var workerPool: Any
    var workerNextTaskID: Int
    var workerConfig: KTX2WorkerConfig?

    fun setTranscoderPath(path: String): KTX2Loader
    fun setWorkerLimit(limit: Int): KTX2Loader
    fun detectSupport(renderer: Renderer): KTX2Loader
    fun dispose(): KTX2Loader

    override fun load(
        url: String,
        onLoad: ((CompressedTexture) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): CompressedTexture
}

data class KTX2WorkerConfig(
    val astcSupported: Boolean = false,
    val etc1Supported: Boolean = false,
    val etc2Supported: Boolean = false,
    val dxtSupported: Boolean = false,
    val bptcSupported: Boolean = false,
    val pvrtcSupported: Boolean = false
)

// ============================================================================
// Font Loaders
// ============================================================================

/**
 * FontLoader: Load typeface.js fonts for TextGeometry
 * Three.js equivalent: THREE.FontLoader
 */
interface FontLoaderAPI : LoaderAPI {
    override fun load(
        url: String,
        onLoad: ((Font) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): Font

    fun parse(json: String): Font
}

interface Font {
    val type: String
    val data: String
    fun generateShapes(text: String, size: Float): List<Shape>
}

// ============================================================================
// Animation Loaders
// ============================================================================

/**
 * AnimationLoader: Load animation clips
 * Three.js equivalent: THREE.AnimationLoader
 */
interface AnimationLoaderAPI : LoaderAPI {
    override fun load(
        url: String,
        onLoad: ((List<AnimationClip>) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((Error) -> Unit)?
    )

    override fun loadAsync(url: String, onProgress: ((ProgressEvent) -> Unit)?): List<AnimationClip>

    fun parse(json: String): List<AnimationClip>
}

// ============================================================================
// Supporting Types
// ============================================================================

data class ProgressEvent(
    val loaded: Long,
    val total: Long,
    val lengthComputable: Boolean = true
) {
    val progress: Float
        get() = if (total > 0) loaded.toFloat() / total.toFloat() else 0f
}

data class Error(
    val message: String,
    val url: String? = null,
    val lineNumber: Int? = null,
    val columnNumber: Int? = null,
    val error: Throwable? = null
)

// ============================================================================
// Cache
// ============================================================================

/**
 * Cache: Global cache for loaded assets
 * Three.js equivalent: THREE.Cache
 */
object CacheAPI {
    var enabled: Boolean
    val files: MutableMap<String, Any>

    fun add(key: String, file: Any)
    fun get(key: String): Any?
    fun remove(key: String)
    fun clear()
}

// ============================================================================
// Factory Functions (Kotlin Idiom)
// ============================================================================

/**
 * DSL builder for LoadingManager
 */
fun loadingManager(init: LoadingManager.() -> Unit = {}): LoadingManager {
    val manager = LoadingManager()
    manager.init()
    return manager
}

/**
 * Extension function for progress tracking
 */
fun LoadingManager.trackProgress(
    onUpdate: (loaded: Int, total: Int, progress: Float) -> Unit
) {
    onProgress = { url, loaded, total ->
        val progress = if (total > 0) loaded.toFloat() / total.toFloat() else 0f
        onUpdate(loaded, total, progress)
    }
}

/**
 * Extension function for error handling
 */
fun LoadingManager.handleErrors(onError: (String) -> Unit) {
    this.onError = { url -> onError(url) }
}

// ============================================================================
// Usage Examples
// ============================================================================

/**
 * Example: Load texture with progress
 */
fun exampleLoadTexture(url: String, onProgress: (Float) -> Unit): Texture {
    val loader = TextureLoader()
    var texture: Texture? = null

    loader.load(
        url = url,
        onLoad = { tex ->
            texture = tex
            println("Texture loaded: ${tex.uuid}")
        },
        onProgress = { event ->
            onProgress(event.progress)
        },
        onError = { error ->
            println("Error loading texture: ${error.message}")
        }
    )

    return texture!!
}

/**
 * Example: Load GLTF model with Draco compression
 */
fun exampleLoadGLTF(url: String): GLTF {
    val dracoLoader = DRACOLoader().apply {
        setDecoderPath("/draco/")
        setDecoderConfig(DRACODecoderConfig(type = "wasm"))
    }

    val loader = GLTFLoader().apply {
        setDRACOLoader(dracoLoader)
    }

    var model: GLTF? = null

    loader.load(
        url = url,
        onLoad = { gltf ->
            model = gltf
            println("Model loaded: ${gltf.scene.children.size} children")
        },
        onError = { error ->
            println("Error loading model: ${error.message}")
        }
    )

    return model!!
}

/**
 * Example: Setup loading manager with progress bar
 */
fun exampleLoadingManager(
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onProgress: (Float) -> Unit,
    onError: (String) -> Unit
): LoadingManager {
    return loadingManager {
        this.onStart = { _, _, _ ->
            onStart()
        }

        this.onLoad = {
            onComplete()
        }

        trackProgress { loaded, total, progress ->
            onProgress(progress)
        }

        handleErrors { url ->
            onError("Failed to load: $url")
        }
    }
}

/**
 * Example: Load multiple textures
 */
fun exampleLoadMultipleTextures(
    urls: List<String>,
    manager: LoadingManager
): List<Texture> {
    val loader = TextureLoader(manager)
    val textures = mutableListOf<Texture>()

    urls.forEach { url ->
        loader.load(url) { texture ->
            textures.add(texture)
        }
    }

    return textures
}

/**
 * Example: Async loading with coroutines
 */
suspend fun exampleAsyncLoading(url: String): GLTF {
    val loader = GLTFLoader()
    return loader.loadAsync(url)
}

/**
 * Example: Load OBJ with MTL materials
 */
fun exampleLoadOBJWithMaterials(
    objUrl: String,
    mtlUrl: String
): Group {
    val mtlLoader = MTLLoader()
    var materials: MaterialLibrary? = null

    mtlLoader.load(mtlUrl) { mtl ->
        mtl.preload()
        materials = mtl
    }

    val objLoader = OBJLoader().apply {
        setMaterials(materials!!)
    }

    var model: Group? = null
    objLoader.load(objUrl) { group ->
        model = group
    }

    return model!!
}

/**
 * Example: Load compressed texture (KTX2)
 */
fun exampleLoadKTX2(
    url: String,
    renderer: Renderer
): CompressedTexture {
    val loader = KTX2Loader().apply {
        setTranscoderPath("/basis/")
        detectSupport(renderer)
    }

    var texture: CompressedTexture? = null
    loader.load(url) { tex ->
        texture = tex
    }

    return texture!!
}
