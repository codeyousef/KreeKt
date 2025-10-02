# Data Model: Three.js r180 Feature Parity

**Feature**: 013-double-check-and
**Date**: 2025-10-01
**Status**: Complete

## Overview

This document defines the core entities and their relationships for implementing complete Three.js r180 feature parity.
All entities follow Kotlin Multiplatform patterns with expect/actual for platform-specific implementations.

---

## 1. Post-Processing System

### EffectComposer

Central orchestrator for post-processing pipeline management.

```kotlin
expect class EffectComposer {
    val renderer: Renderer
    val renderTarget1: RenderTarget
    val renderTarget2: RenderTarget
    val passes: List<Pass>

    var currentRenderTarget: RenderTarget

    fun addPass(pass: Pass)
    fun insertPass(pass: Pass, index: Int)
    fun removePass(pass: Pass)
    fun render(deltaTime: Float = 0.016f)
    fun setSize(width: Int, height: Int)
    fun setPixelRatio(pixelRatio: Float)
    fun dispose()
}
```

**Relationships**:

- **Has many**: `Pass` objects (pipeline stages)
- **Uses**: `Renderer` for actual rendering
- **Owns**: Two `RenderTarget` objects for ping-pong buffering

**Validation Rules**:

- Must have at least one pass
- Last pass must be an `OutputPass` or have `needsSwap = false`
- Width and height must be > 0

**State Transitions**:

1. Initial → Pass Added → Ready
2. Ready → Rendering → Ready
3. Ready → Disposed

---

### Pass (Abstract)

Base class for all post-processing passes.

```kotlin
abstract class Pass {
    abstract val name: String
    var enabled: Boolean = true
    var needsSwap: Boolean = true
    var clear: Boolean = false
    var renderToScreen: Boolean = false

    abstract fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    abstract fun setSize(width: Int, height: Int)
    open fun dispose() {}
}
```

**Subclasses**:

- `RenderPass`: Renders scene/camera
- `ShaderPass`: Applies custom shader
- `OutputPass`: Final output with color space
- `BloomPass`: Bloom effect
- `UnrealBloomPass`: Advanced bloom with lens effects
- `SSAOPass`: Screen-space ambient occlusion
- `SAOPass`: Scalable ambient occlusion
- `SMAAPass`: Subpixel morphological antialiasing
- `FXAAPass`: Fast approximate antialiasing
- `TAAPass`: Temporal antialiasing
- `SSAAPass`: Super-sampling antialiasing
- `BokehPass`: Depth-of-field
- `OutlinePass`: Object outlining
- `GlitchPass`: Digital glitch effect
- `FilmPass`: Film grain and scanlines
- `DotScreenPass`: Halftone dot screen
- `HalftonePass`: CMYK halftone

**Validation Rules**:

- Name must be unique within composer
- If `needsSwap = true`, must not be last pass unless followed by `OutputPass`

---

### RenderPass

Renders a scene with a camera.

```kotlin
class RenderPass(
    val scene: Scene,
    val camera: Camera,
    val overrideMaterial: Material? = null,
    val clearColor: Color? = null,
    val clearAlpha: Float = 0.0f
) : Pass() {
    override val name = "RenderPass"
    var clearDepth: Boolean = false

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
}
```

**Relationships**:

- **References**: `Scene` to render
- **References**: `Camera` for viewpoint
- **Optionally uses**: `Material` to override scene materials

---

### ShaderPass

Applies a custom shader as a full-screen effect.

```kotlin
class ShaderPass(
    val shader: Shader,
    val textureID: String = "tDiffuse"
) : Pass() {
    override val name = "ShaderPass"
    val uniforms: MutableMap<String, Uniform> = shader.uniforms

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
}
```

**Relationships**:

- **Has one**: `Shader` defining the effect
- **Has many**: `Uniform` values

---

### BloomPass

Applies bloom/glow effect using dual-filter downsampling/upsampling.

```kotlin
class BloomPass(
    var strength: Float = 1.0f,
    var radius: Float = 0.5f,
    var threshold: Float = 0.0f
) : Pass() {
    override val name = "BloomPass"

    val renderTargetsHorizontal: List<RenderTarget>
    val renderTargetsVertical: List<RenderTarget>
    val nMips: Int = 5

    var highPassUniforms: Map<String, Uniform>
    var blurUniforms: Map<String, Uniform>
    var compositeUniforms: Map<String, Uniform>

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}
```

**Validation Rules**:

- `strength` ≥ 0
- `radius` ≥ 0
- `threshold` ≥ 0

---

### SSAOPass

Screen-space ambient occlusion.

```kotlin
class SSAOPass(
    val scene: Scene,
    val camera: Camera,
    var kernelRadius: Float = 8.0f,
    var minDistance: Float = 0.005f,
    var maxDistance: Float = 0.1f
) : Pass() {
    override val name = "SSAOPass"

    var kernelSize: Int = 64
    var noiseTexture: Texture
    var output: SSAOOutput = SSAOOutput.Default

    val ssaoRenderTarget: RenderTarget
    val normalRenderTarget: RenderTarget
    val blurRenderTarget: RenderTarget

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}

enum class SSAOOutput {
    Default,    // SSAO * scene
    SSAO,       // SSAO only
    Blur,       // Blurred SSAO
    Beauty,     // Scene without SSAO
    Depth,      // Depth buffer
    Normal      // Normal buffer
}
```

**Relationships**:

- **References**: `Scene` and `Camera`
- **Owns**: Multiple `RenderTarget` objects for multi-pass rendering
- **Uses**: Noise texture for randomization

**Validation Rules**:

- `kernelRadius` > 0
- `minDistance` < `maxDistance`
- `kernelSize` must be power of 2 (16, 32, 64, 128)

---

## 2. Asset Loader System

### AssetLoader (Interface)

Base interface for all asset loaders.

```kotlin
interface AssetLoader<T> {
    suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)? = null
    ): Result<T>

    suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)? = null
    ): Result<T>

    suspend fun parse(data: Any): Result<T>
}

data class LoadingProgress(
    val loaded: Long,
    val total: Long
) {
    val percentage: Float
        get() = if (total > 0) loaded.toFloat() / total.toFloat() else 0f
}
```

**Validation Rules**:

- URL must be valid (format dependent on platform)
- Progress callbacks must be invoked on main/UI thread

---

### LoadingManager

Centralized asset loading coordination.

```kotlin
class LoadingManager {
    private val loaders = mutableMapOf<String, AssetLoader<*>>()
    private val cache = mutableMapOf<String, Any>()

    var onStart: ((url: String, loaded: Int, total: Int) -> Unit)? = null
    var onLoad: (() -> Unit)? = null
    var onProgress: ((url: String, loaded: Int, total: Int) -> Unit)? = null
    var onError: ((url: String, error: Throwable) -> Unit)? = null

    val itemsLoaded = MutableStateFlow(0)
    val itemsTotal = MutableStateFlow(0)
    val isLoading: StateFlow<Boolean>

    fun registerLoader(extension: String, loader: AssetLoader<*>)
    fun getLoader(extension: String): AssetLoader<*>?

    suspend fun load(url: String): Result<Any>
    fun addToCache(url: String, data: Any)
    fun getFromCache(url: String): Any?
    fun clearCache()
}
```

**Relationships**:

- **Contains many**: `AssetLoader` instances mapped by file extension
- **Maintains**: Cache of loaded assets

**State Transitions**:

1. Idle → Loading (itemsTotal > itemsLoaded)
2. Loading → Idle (itemsLoaded == itemsTotal)
3. Loading → Error → Idle

---

### GLTFLoader

Loads GLTF 2.0 models (JSON + binary buffers).

```kotlin
class GLTFLoader : AssetLoader<Scene> {
    var dracoLoader: DRACOLoader? = null
    var ktx2Loader: KTX2Loader? = null
    var meshOptDecoder: MeshOptDecoder? = null

    override suspend fun load(
        url: String,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Scene>

    override suspend fun loadFromBytes(
        bytes: ByteArray,
        onProgress: ((LoadingProgress) -> Unit)?
    ): Result<Scene>

    override suspend fun parse(data: Any): Result<Scene>

    private suspend fun parseGLTF(json: JsonObject, buffers: List<ByteArray>): Scene
    private suspend fun loadBuffer(uri: String): ByteArray
    private fun buildScene(gltf: GLTFStructure): Scene
}

data class GLTFStructure(
    val asset: GLTFAsset,
    val scenes: List<GLTFScene>,
    val nodes: List<GLTFNode>,
    val meshes: List<GLTFMesh>,
    val materials: List<GLTFMaterial>,
    val textures: List<GLTFTexture>,
    val images: List<GLTFImage>,
    val accessors: List<GLTFAccessor>,
    val bufferViews: List<GLTFBufferView>,
    val buffers: List<GLTFBuffer>,
    val animations: List<GLTFAnimation>?,
    val skins: List<GLTFSkin>?
)
```

**Validation Rules**:

- GLTF version must be 2.0
- All buffer references must be valid
- Required extensions must be supported

---

### FBXLoader

Loads FBX 7.x binary format.

```kotlin
class FBXLoader : AssetLoader<Scene> {
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

    private fun parseBinary(buffer: ByteArray): FBXTree
    private fun parseConnections(tree: FBXTree): FBXConnections
    private fun buildScene(tree: FBXTree, connections: FBXConnections): Scene
}

data class FBXTree(
    val version: Int,
    val objects: Map<Long, FBXNode>,
    val connections: List<FBXConnection>
)

data class FBXConnection(
    val type: String, // "OO" (object-object) or "OP" (object-property)
    val childId: Long,
    val parentId: Long,
    val propertyName: String?
)
```

**Validation Rules**:

- FBX version ≥ 7000
- Binary format (magic bytes: "Kaydara FBX Binary")
- All object references must resolve

---

### TextureLoader

Loads various texture formats.

```kotlin
class TextureLoader : AssetLoader<Texture> {
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

// Specialized texture loaders
class EXRLoader : AssetLoader<DataTexture>
class RGBELoader : AssetLoader<DataTexture>
class TGALoader : AssetLoader<Texture>
class KTX2Loader : AssetLoader<CompressedTexture>
class BasisTextureLoader : AssetLoader<CompressedTexture>
class DDSLoader : AssetLoader<CompressedTexture>
class PVRLoader : AssetLoader<CompressedTexture>
```

---

## 3. Asset Exporter System

### AssetExporter (Interface)

Base interface for exporters.

```kotlin
interface AssetExporter {
    suspend fun export(
        scene: Scene,
        options: ExportOptions = ExportOptions()
    ): Result<ByteArray>

    suspend fun exportToFile(
        scene: Scene,
        path: String,
        options: ExportOptions = ExportOptions()
    ): Result<Unit>
}

data class ExportOptions(
    val binary: Boolean = false,
    val embedTextures: Boolean = false,
    val includeAnimations: Boolean = true,
    val includeCameras: Boolean = true,
    val includeLights: Boolean = true,
    val maxTextureSize: Int = 4096,
    val compressionLevel: Int = 5,
    val onlyVisible: Boolean = false,
    val truncateDrawRange: Boolean = true
)
```

---

### GLTFExporter

Exports to GLTF 2.0 format.

```kotlin
class GLTFExporter : AssetExporter {
    override suspend fun export(
        scene: Scene,
        options: ExportOptions
    ): Result<ByteArray>

    override suspend fun exportToFile(
        scene: Scene,
        path: String,
        options: ExportOptions
    ): Result<Unit>

    private fun processScene(scene: Scene, options: ExportOptions): GLTFStructure
    private fun processNode(node: Object3D): GLTFNode
    private fun processMesh(mesh: Mesh): GLTFMesh
    private fun processMaterial(material: Material): GLTFMaterial
    private fun processTexture(texture: Texture): GLTFTexture
    private fun processAnimation(animation: AnimationClip): GLTFAnimation
    private fun serializeGLTF(gltf: GLTFStructure, binary: Boolean): ByteArray
}
```

---

### USDZExporter

Exports to USDZ format (Apple AR).

```kotlin
class USDZExporter : AssetExporter {
    override suspend fun export(
        scene: Scene,
        options: ExportOptions
    ): Result<ByteArray>

    override suspend fun exportToFile(
        scene: Scene,
        path: String,
        options: ExportOptions
    ): Result<Unit>

    private fun generateUSDFile(scene: Scene): String
    private fun generateUSDZ(usdContent: String, textures: Map<String, ByteArray>): ByteArray
}
```

---

## 4. Node-Based Material System

### MaterialNode (Abstract)

Base class for material graph nodes.

```kotlin
abstract class MaterialNode {
    abstract val id: String
    abstract val name: String
    abstract val inputs: List<NodeInput>
    abstract val outputs: List<NodeOutput>

    var position: Vector2 = Vector2.zero()

    abstract fun generateCode(target: ShaderTarget, context: CodeGenContext): String
    abstract fun validate(): ValidationResult

    open fun clone(): MaterialNode = throw NotImplementedError()
}
```

**Subclasses**:

- Input nodes: `TextureSampleNode`, `AttributeNode`, `UniformNode`, `TimeNode`
- Math nodes: `AddNode`, `MultiplyNode`, `DotProductNode`, `NormalizeNode`
- Lighting nodes: `PBRNode`, `LambertNode`, `PhongNode`, `FresnelNode`
- Utility nodes: `SplitNode`, `CombineNode`, `ConditionalNode`, `FunctionNode`
- Output nodes: `FragmentOutputNode`, `VertexOutputNode`

---

### NodeInput

Represents an input socket on a node.

```kotlin
data class NodeInput(
    val name: String,
    val type: NodeDataType,
    val node: MaterialNode,
    var connection: NodeOutput? = null,
    var defaultValue: Any? = null
) {
    val isConnected: Boolean
        get() = connection != null

    fun getValue(): Any {
        return connection?.getValue() ?: defaultValue ?: getTypeDefault(type)
    }
}
```

**Validation Rules**:

- Cannot connect to output of same node (prevent self-loops)
- Type must match connected output (or be compatible)
- If not connected, must have default value

---

### NodeOutput

Represents an output socket on a node.

```kotlin
data class NodeOutput(
    val name: String,
    val type: NodeDataType,
    val node: MaterialNode,
    val connections: MutableList<NodeInput> = mutableListOf()
) {
    fun getValue(): Any {
        return node.computeOutput(name)
    }

    fun connect(input: NodeInput) {
        connections.add(input)
        input.connection = this
    }

    fun disconnect(input: NodeInput) {
        connections.remove(input)
        input.connection = null
    }
}
```

---

### NodeDataType

Type system for node connections.

```kotlin
enum class NodeDataType(val size: Int, val glslType: String, val wgslType: String) {
    FLOAT(1, "float", "f32"),
    VEC2(2, "vec2", "vec2<f32>"),
    VEC3(3, "vec3", "vec3<f32>"),
    VEC4(4, "vec4", "vec4<f32>"),
    MAT3(9, "mat3", "mat3x3<f32>"),
    MAT4(16, "mat4", "mat4x4<f32>"),
    TEXTURE_2D(0, "sampler2D", "texture_2d<f32>"),
    TEXTURE_CUBE(0, "samplerCube", "texture_cube<f32>"),
    SAMPLER(0, "sampler", "sampler"),
    BOOL(1, "bool", "bool"),
    INT(1, "int", "i32");

    fun isCompatibleWith(other: NodeDataType): Boolean {
        return when {
            this == other -> true
            this == FLOAT && other in listOf(VEC2, VEC3, VEC4) -> true // Can broadcast
            else -> false
        }
    }
}
```

---

### NodeGraph

Container for material node graph.

```kotlin
class NodeGraph {
    private val nodes = mutableMapOf<String, MaterialNode>()
    private val connections = mutableListOf<Pair<NodeOutput, NodeInput>>()

    var name: String = "NodeGraph"
    var outputNode: FragmentOutputNode? = null

    fun addNode(node: MaterialNode) {
        nodes[node.id] = node
    }

    fun removeNode(nodeId: String) {
        val node = nodes[nodeId] ?: return
        // Disconnect all connections
        node.inputs.forEach { it.connection?.disconnect(it) }
        node.outputs.forEach { output ->
            output.connections.toList().forEach { output.disconnect(it) }
        }
        nodes.remove(nodeId)
    }

    fun connect(from: NodeOutput, to: NodeInput): Result<Unit> {
        // Validate connection
        if (!from.type.isCompatibleWith(to.type)) {
            return Result.failure(Exception("Incompatible types"))
        }

        // Check for cycles
        if (wouldCreateCycle(from.node, to.node)) {
            return Result.failure(Exception("Connection would create cycle"))
        }

        // Make connection
        from.connect(to)
        connections.add(from to to)
        return Result.success(Unit)
    }

    fun disconnect(from: NodeOutput, to: NodeInput) {
        from.disconnect(to)
        connections.remove(from to to)
    }

    fun compile(target: ShaderTarget): Result<CompiledShader> {
        // Topological sort
        val sorted = topologicalSort()

        // Generate code for each node
        val context = CodeGenContext(target)
        for (node in sorted) {
            val code = node.generateCode(target, context)
            context.addNodeCode(node.id, code)
        }

        // Generate final shader
        return context.finalize()
    }

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Check for cycles
        if (hasCycles()) {
            errors.add("Graph contains cycles")
        }

        // Check for disconnected nodes
        val disconnected = findDisconnectedNodes()
        if (disconnected.isNotEmpty()) {
            errors.add("Disconnected nodes: ${disconnected.joinToString()}")
        }

        // Check output node
        if (outputNode == null) {
            errors.add("No output node")
        }

        // Validate each node
        nodes.values.forEach { node ->
            val result = node.validate()
            if (!result.isValid) {
                errors.addAll(result.errors)
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun topologicalSort(): List<MaterialNode>
    private fun wouldCreateCycle(from: MaterialNode, to: MaterialNode): Boolean
    private fun hasCycles(): Boolean
    private fun findDisconnectedNodes(): List<String>
}
```

**Validation Rules**:

- Graph must be acyclic (DAG)
- Must have exactly one output node
- All input nodes must have values (connected or default)
- No disconnected subgraphs (unless intentional for debugging)

---

### NodeMaterial

Material that uses a node graph.

```kotlin
class NodeMaterial : Material() {
    val nodeGraph: NodeGraph = NodeGraph()
    var autoCompile: Boolean = true

    private var compiledShader: CompiledShader? = null

    override fun onBeforeCompile(shader: Shader, renderer: Renderer) {
        if (autoCompile || compiledShader == null) {
            val result = nodeGraph.compile(shader.target)
            result.onSuccess { compiled ->
                compiledShader = compiled
                shader.vertexShader = compiled.vertexShader
                shader.fragmentShader = compiled.fragmentShader
                shader.uniforms.putAll(compiled.uniforms)
            }
        }
    }

    fun recompile() {
        compiledShader = null
    }
}
```

---

## 5. Geometry Utilities

### GeometryProcessor

Utility class for geometry operations.

```kotlin
object GeometryProcessor {
    fun computeConvexHull(points: List<Vector3>): BufferGeometry

    fun simplify(
        geometry: BufferGeometry,
        targetRatio: Float,
        options: SimplificationOptions = SimplificationOptions()
    ): BufferGeometry

    fun subdivide(
        geometry: BufferGeometry,
        iterations: Int = 1,
        method: SubdivisionMethod = SubdivisionMethod.Loop
    ): BufferGeometry

    fun computeTangents(
        geometry: BufferGeometry,
        uvChannel: Int = 0
    )

    fun mergeGeometries(
        geometries: List<BufferGeometry>,
        useGroups: Boolean = false
    ): BufferGeometry

    fun mergeVertices(
        geometry: BufferGeometry,
        tolerance: Float = 1e-4f
    ): BufferGeometry

    fun toIndexed(geometry: BufferGeometry): BufferGeometry

    fun toNonIndexed(geometry: BufferGeometry): BufferGeometry

    fun computeBounds(geometry: BufferGeometry): Box3

    fun computeBoundingSphere(geometry: BufferGeometry): Sphere

    fun estimateMemoryUsage(geometry: BufferGeometry): GeometryMemoryInfo
}

data class SimplificationOptions(
    val preserveBoundary: Boolean = true,
    val preserveUVs: Boolean = true,
    val preserveNormals: Boolean = true,
    val aggressiveness: Float = 7.0f
)

enum class SubdivisionMethod {
    Loop,        // For triangle meshes
    CatmullClark // For quad meshes
}

data class GeometryMemoryInfo(
    val vertexCount: Int,
    val triangleCount: Int,
    val attributeBytes: Long,
    val indexBytes: Long,
    val totalBytes: Long
)
```

---

## 6. Performance Monitoring

### PerformanceMonitor

Cross-platform performance monitoring.

```kotlin
expect class PerformanceMonitor {
    var enabled: Boolean
    var samplingInterval: Int

    fun beginFrame()
    fun endFrame()

    fun getFrameTime(): Double // milliseconds
    fun getFPS(): Double
    fun getMemoryUsage(): MemoryInfo
    fun getDrawCalls(): Int
    fun getTriangles(): Int
    fun getTextures(): Int
    fun getGeometries(): Int
    fun getProgramsCompiled(): Int

    fun getMetrics(count: Int = 60): List<FrameMetrics>
    fun getAverages(windowSize: Int = 60): AverageMetrics

    fun reset()
}

data class MemoryInfo(
    val used: Long,
    val total: Long,
    val limit: Long
) {
    val usedMB: Float
        get() = used / (1024f * 1024f)
    val totalMB: Float
        get() = total / (1024f * 1024f)
    val percentage: Float
        get() = if (total > 0) used.toFloat() / total.toFloat() else 0f
}

data class FrameMetrics(
    val frameNumber: Long,
    val timestamp: Double,
    val frameTime: Double,
    val fps: Double,
    val drawCalls: Int,
    val triangles: Int,
    val memory: MemoryInfo
)

data class AverageMetrics(
    val avgFPS: Double,
    val avgFrameTime: Double,
    val minFPS: Double,
    val maxFrameTime: Double,
    val avgDrawCalls: Double,
    val avgTriangles: Double,
    val avgMemoryUsed: Long
)
```

---

## 7. Texture Compression

### TextureCompressor

Platform-appropriate texture compression.

```kotlin
interface TextureCompressor {
    fun getSupportedFormats(): Set<TextureFormat>

    fun selectBestFormat(
        hasAlpha: Boolean,
        isNormalMap: Boolean = false,
        quality: TextureQuality = TextureQuality.HIGH
    ): TextureFormat

    suspend fun compress(
        data: ByteArray,
        width: Int,
        height: Int,
        targetFormat: TextureFormat,
        options: CompressionOptions = CompressionOptions()
    ): Result<CompressedTextureData>
}

data class CompressedTextureData(
    val format: TextureFormat,
    val width: Int,
    val height: Int,
    val data: ByteArray,
    val mipmaps: List<ByteArray> = emptyList()
)

data class CompressionOptions(
    val generateMipmaps: Boolean = true,
    val quality: TextureQuality = TextureQuality.HIGH,
    val compressionLevel: Int = 5 // 0-10
)

enum class TextureQuality {
    LOW,    // Favor size
    MEDIUM, // Balanced
    HIGH,   // Favor quality
    ULTRA   // Best quality
}
```

---

### BasisUniversalTranscoder

Basis Universal integration.

```kotlin
expect class BasisUniversalTranscoder {
    suspend fun initialize(): Result<Unit>

    suspend fun transcode(
        basisData: ByteArray,
        targetFormat: TextureFormat,
        mipLevel: Int = 0
    ): Result<ByteArray>

    fun getImageInfo(basisData: ByteArray): Result<BasisImageInfo>

    fun getSupportedTranscodeFormats(): Set<TextureFormat>
}

data class BasisImageInfo(
    val width: Int,
    val height: Int,
    val hasAlpha: Boolean,
    val mipLevels: Int,
    val imageCount: Int,
    val isCompressed: Boolean
)
```

---

## 8. Shader System

### ShaderChunk

Library of reusable shader code.

```kotlin
object ShaderChunk {
    private val chunks = mutableMapOf<String, String>()

    fun registerChunk(name: String, code: String)
    fun getChunk(name: String): String?
    fun hasChunk(name: String): Boolean
    fun getAllChunks(): Map<String, String>
    fun clear()

    // Standard chunks (registered at initialization)
    val COMMON: String
    val FOG_PARS_FRAGMENT: String
    val FOG_FRAGMENT: String
    val LIGHTS_PARS_FRAGMENT: String
    val LIGHTS_FRAGMENT: String
    val NORMAL_PARS_FRAGMENT: String
    val NORMAL_FRAGMENT: String
    // ... more standard chunks
}
```

---

### ShaderPreprocessor

Preprocesses shader source code.

```kotlin
class ShaderPreprocessor {
    fun process(
        source: String,
        target: ShaderTarget,
        defines: Map<String, String> = emptyMap()
    ): Result<String>

    private fun resolveIncludes(source: String, visited: Set<String> = emptySet()): String
    private fun resolveDefines(source: String, defines: Map<String, String>): String
    private fun resolveConditionals(source: String, defines: Set<String>): String
}
```

---

### Shader

Shader definition with uniforms.

```kotlin
data class Shader(
    var vertexShader: String,
    var fragmentShader: String,
    val uniforms: MutableMap<String, Uniform> = mutableMapOf(),
    val defines: MutableMap<String, String> = mutableMapOf(),
    val target: ShaderTarget = ShaderTarget.WGSL
)

enum class ShaderTarget {
    WGSL,   // WebGPU
    SPIRV,  // Vulkan
    GLSL    // Legacy (not used)
}
```

---

### UniformsLib

Standard uniform libraries.

```kotlin
object UniformsLib {
    val common: Map<String, Uniform>
    val lights: Map<String, Uniform>
    val fog: Map<String, Uniform>
    val shadowmap: Map<String, Uniform>
    val envmap: Map<String, Uniform>
}

object UniformsUtils {
    fun clone(uniforms: Map<String, Uniform>): MutableMap<String, Uniform>
    fun merge(vararg uniformSets: Map<String, Uniform>): MutableMap<String, Uniform>
}
```

---

## 9. Helper Objects

### Helper (Abstract)

Base class for visualization helpers.

```kotlin
abstract class Helper : LineSegments() {
    abstract fun update()
    open fun dispose() {}
}
```

---

### VertexNormalsHelper

Visualizes vertex normals.

```kotlin
class VertexNormalsHelper(
    val targetObject: Object3D,
    var size: Float = 1.0f,
    var color: Color = Color(0xff0000)
) : Helper() {
    override fun update() {
        // Extract vertex normals from geometry
        // Create line segments from vertex to vertex+normal*size
    }
}
```

---

### VertexTangentsHelper

Visualizes vertex tangents.

```kotlin
class VertexTangentsHelper(
    val targetObject: Object3D,
    var size: Float = 1.0f,
    var color: Color = Color(0x00ff00)
) : Helper() {
    override fun update() {
        // Extract vertex tangents from geometry
        // Create line segments from vertex to vertex+tangent*size
    }
}
```

---

### AxesHelper

Visualizes XYZ axes.

```kotlin
class AxesHelper(
    var size: Float = 1.0f
) : LineSegments() {
    init {
        // Create three line segments:
        // X axis (red): origin to (size, 0, 0)
        // Y axis (green): origin to (0, size, 0)
        // Z axis (blue): origin to (0, 0, size)
    }
}
```

---

### GridHelper

Visualizes a grid.

```kotlin
class GridHelper(
    var size: Float = 10.0f,
    var divisions: Int = 10,
    var colorCenterLine: Color = Color(0x444444),
    var colorGrid: Color = Color(0x888888)
) : LineSegments() {
    init {
        // Create grid lines
    }
}
```

---

## Entity Relationship Diagram

```
┌─────────────────┐
│ EffectComposer  │
├─────────────────┤       ┌──────────┐
│ + passes        │───────┤   Pass   │
│ + renderer      │       └──────────┘
│ + renderTargets │            ▲
└─────────────────┘            │
                               ├── RenderPass
                               ├── ShaderPass
                               ├── BloomPass
                               └── SSAOPass (etc.)

┌─────────────────┐       ┌──────────────┐
│ LoadingManager  │───────┤ AssetLoader  │
├─────────────────┤       └──────────────┘
│ + loaders       │            ▲
│ + cache         │            │
└─────────────────┘            ├── GLTFLoader
                               ├── FBXLoader
                               └── TextureLoader (etc.)

┌─────────────────┐       ┌──────────────┐
│   NodeGraph     │───────┤ MaterialNode │
├─────────────────┤       └──────────────┘
│ + nodes         │            ▲
│ + connections   │            │
│ + outputNode    │            ├── TextureSampleNode
└─────────────────┘            ├── AddNode
                               └── PBRNode (etc.)

┌─────────────────┐
│ MaterialNode    │
├─────────────────┤       ┌─────────────┐
│ + inputs        │───────┤  NodeInput  │
│ + outputs       │       └─────────────┘
└─────────────────┘       ┌─────────────┐
         │────────────────┤ NodeOutput  │
                          └─────────────┘

┌──────────────────────┐
│ PerformanceMonitor   │
├──────────────────────┤
│ + getFrameTime()     │
│ + getFPS()           │
│ + getMemoryUsage()   │
│ + getMetrics()       │
└──────────────────────┘

┌──────────────────────┐       ┌────────────────────┐
│ TextureCompressor    │───────┤ BasisTranscoder    │
├──────────────────────┤       └────────────────────┘
│ + compress()         │
│ + selectBestFormat() │
└──────────────────────┘

┌──────────────────────┐       ┌────────────────────┐
│ ShaderPreprocessor   │───────┤   ShaderChunk      │
├──────────────────────┤       └────────────────────┘
│ + process()          │
│ + resolveIncludes()  │
└──────────────────────┘

┌──────────────────────┐
│ GeometryProcessor    │
├──────────────────────┤
│ + computeConvexHull()│
│ + simplify()         │
│ + subdivide()        │
│ + computeTangents()  │
└──────────────────────┘
```

---

## Summary

This data model defines 9 core systems with their entities, relationships, validation rules, and state transitions:

1. **Post-Processing**: EffectComposer + Pass hierarchy
2. **Asset Loaders**: LoadingManager + loader implementations
3. **Asset Exporters**: Exporter interface + implementations
4. **Node Materials**: NodeGraph + MaterialNode hierarchy
5. **Geometry Utils**: GeometryProcessor utilities
6. **Performance**: PerformanceMonitor
7. **Texture Compression**: TextureCompressor + BasisTranscoder
8. **Shader System**: ShaderPreprocessor + ShaderChunk
9. **Helpers**: Helper base class + implementations

All entities follow:

- Kotlin Multiplatform patterns (expect/actual)
- TDD principles (testable interfaces)
- Production-ready design (no placeholders)
- Three.js API compatibility
