# Research: Three.js r180 Feature Parity - Technical Decisions

**Feature**: 013-double-check-and
**Date**: 2025-10-01
**Status**: Complete

## Overview

This document outlines technical decisions for implementing complete Three.js r180 feature parity in KreeKt. All
decisions prioritize WebGPU/Vulkan backends (not OpenGL), cross-platform compatibility, and production-ready
implementation following TDD principles.

---

## 1. Post-Processing System

### Decision

Implement a unified post-processing pipeline using:

- **WebGPU (JS)**: Compute and render passes with `GPURenderPassDescriptor` and `GPUComputePassDescriptor`
- **Vulkan (JVM/Native)**: `VkRenderPass` with multiple subpasses and framebuffer attachments
- **Architecture**: Effect chaining through intermediate framebuffers with ping-pong technique for temporal effects

### Rationale

- **WebGPU Render Passes**: Native support for multiple render targets, efficient GPU state management, and modern
  graphics pipeline
- **Vulkan Subpasses**: Enables efficient tile-based rendering on mobile, reduces memory bandwidth through on-chip
  memory
- **Ping-Pong Buffers**: Essential for temporal effects (TAA, motion blur) and iterative effects (bloom)
- **Cross-Platform**: Both APIs support similar concepts (render targets, passes, pipelines)

### Alternatives Considered

1. **OpenGL Framebuffer Objects**: Rejected - project explicitly uses WebGPU/Vulkan, not OpenGL
2. **Single-Pass Effects**: Rejected - limits effect complexity and temporal effects
3. **CPU-Based Post-Processing**: Rejected - too slow for 60 FPS target
4. **Metal-Specific API**: Considered but Vulkan via MoltenVK provides better cross-platform support

### Implementation Notes

**Core Classes**:

```kotlin
// Common interface
expect class EffectComposer {
    fun addPass(pass: Pass)
    fun render(delta: Float)
    fun setSize(width: Int, height: Int)
}

// Platform-specific implementations
actual class EffectComposer // WebGPU: GPURenderPassEncoder management
actual class EffectComposer // Vulkan: VkCommandBuffer with render passes

sealed class Pass {
    abstract fun render(composer: EffectComposer, writeBuffer: RenderTarget, readBuffer: RenderTarget)
    var enabled: Boolean = true
    var needsSwap: Boolean = true
}
```

**Pass Types**:

- `RenderPass`: Renders scene to texture
- `ShaderPass`: Custom WGSL/SPIR-V shader
- `OutputPass`: Final color space conversion
- `BloomPass`: Dual-filter downsampling/upsampling
- `UnrealBloomPass`: Lens dirt, threshold, radius
- `SSAOPass`: Screen-space ambient occlusion
- `TAA`: Temporal anti-aliasing with jitter
- `BokehPass`: Hexagonal/circular bokeh shapes
- `OutlinePass`: Stencil-based object highlighting

**Platform Specifics**:

- **JS**: Use `@webgpu/types` for TypeScript definitions
- **JVM**: LWJGL 3.3.3 Vulkan bindings
- **Native**: Platform-specific Vulkan libraries
- **Texture Formats**: `RGBA16Float` for HDR, `RGBA8Unorm` for LDR

**Performance Targets**:

- Post-processing overhead: <5ms per frame at 1080p
- Memory: 2-4 render targets max (ping-pong)
- Pipeline state caching to avoid runtime compilation

---

## 2. Asset Loaders

### Decision

Implement format-specific loaders using:

- **Parsing**: Kotlin Multiplatform libraries (kotlinx.serialization for JSON-based formats)
- **File I/O**: Platform-specific expect/actual with `Path` abstraction
- **Binary Parsing**: Kotlin's `ByteArray` with `ByteBuffer` wrapper for structured reading
- **Async Loading**: Kotlin coroutines with `Flow` for progress tracking

### Rationale

- **kotlinx.serialization**: Zero-copy deserialization, multiplatform, type-safe
- **Coroutines**: Natural async/await pattern, cancellation support, back-pressure handling
- **Platform File I/O**: Necessary due to different file system APIs (Web File API, JVM NIO, Native POSIX)
- **Streaming**: Large models (>100MB) require progressive loading

### Alternatives Considered

1. **JVM-Only Libraries**: Rejected - not multiplatform compatible
2. **Callback-Based Loading**: Rejected - coroutines provide better composition
3. **Synchronous Loading**: Rejected - blocks main thread, poor UX
4. **Native Parsers (C/C++)**: Considered for performance but Kotlin parsers sufficient

### Implementation Notes

**Core Loader Interface**:

```kotlin
interface AssetLoader<T> {
    suspend fun load(url: String, onProgress: ((Float) -> Unit)? = null): Result<T>
    suspend fun loadFromBytes(bytes: ByteArray): Result<T>
}

class LoadingManager {
    private val loaders = mutableMapOf<String, AssetLoader<*>>()

    fun registerLoader(extension: String, loader: AssetLoader<*>)
    suspend fun load(url: String): Result<Any>

    val progress: StateFlow<LoadingProgress>
}
```

**Format-Specific Loaders**:

1. **FBXLoader** (Binary format):
    - Parse FBX 7.x binary structure
    - Extract geometry, materials, animations, skeletons
    - Convert FBX coordinate system to KreeKt (Y-up, right-handed)
    - Libraries: Custom binary parser

2. **GLTFLoader** (JSON + Binary):
    - kotlinx.serialization for JSON parsing
    - Binary buffer handling for .bin files
    - Draco compression support (via WASM module on Web, native on JVM)
    - KTX2 texture support with Basis Universal transcoding

3. **OBJLoader** (Text format):
    - Line-by-line parsing of .obj files
    - MTL material library parsing
    - Automatic smoothing group handling
    - Simple text parsing, no external dependencies

4. **ColladaLoader** (XML format):
    - XML parsing via kotlinx.serialization-xml or platform XML parser
    - COLLADA 1.4/1.5 support
    - Animation and skinning import
    - Libraries: kotlinx.serialization or platform XML

5. **STLLoader** (Binary/ASCII):
    - Binary STL: 80-byte header + triangles
    - ASCII STL: Text parsing
    - Auto-detection of format
    - No materials, simple triangle soup

6. **PLYLoader** (Binary/ASCII):
    - PLY header parsing
    - Binary/ASCII data section
    - Vertex colors, normals, UVs
    - Libraries: Custom parser

7. **3DMLoader** (3DS format):
    - Chunk-based binary format
    - Legacy format, limited features
    - Libraries: Custom parser

8. **USDZLoader** (ZIP archive):
    - USDZ = USD + textures in ZIP
    - Use platform ZIP libraries
    - USD parsing (subset for geometry/materials)
    - Libraries: Platform ZIP, custom USD parser

**Texture Loaders**:

- **EXR**: OpenEXR via JNI wrapper (JVM), WASM module (Web)
- **RGBE**: Custom HDR parser (Radiance format)
- **TGA**: Simple RLE/uncompressed parser
- **KTX2**: Basis Universal transcoder (WASM/Native)
- **DDS**: DirectX texture parser
- **PVR**: PowerVR texture parser

**Platform File I/O**:

```kotlin
expect class FileSystem {
    suspend fun readBytes(path: String): Result<ByteArray>
    suspend fun readText(path: String): Result<String>
    fun exists(path: String): Boolean
}

// JS: fetch API or FileReader
// JVM: java.nio.file.Files
// Native: POSIX file APIs
```

**Performance Targets**:

- GLTF: <1s for 10MB model
- FBX: <2s for 20MB model
- Texture: <500ms for 4K texture
- Progress callbacks every 100ms

---

## 3. Asset Exporters

### Decision

Implement format-specific exporters using:

- **Serialization**: kotlinx.serialization for JSON formats (GLTF)
- **Binary Writing**: Platform-specific binary file writers
- **Scene Graph Traversal**: Visitor pattern for scene export
- **Texture Encoding**: Platform-specific image encoders

### Rationale

- **Serialization**: Type-safe, automatic JSON generation, multiplatform
- **Visitor Pattern**: Clean separation of export logic from scene graph
- **Platform Encoders**: Leverage native image encoding libraries
- **Incremental Export**: Support large scenes without memory spikes

### Alternatives Considered

1. **String Concatenation**: Rejected - error-prone, no type safety
2. **DOM-Based XML**: Considered but kotlinx.serialization-xml preferred
3. **Synchronous Export**: Rejected - blocks UI during large exports
4. **Single Monolithic Exporter**: Rejected - violates single responsibility

### Implementation Notes

**Core Exporter Interface**:

```kotlin
interface AssetExporter {
    suspend fun export(scene: Scene, options: ExportOptions): Result<ByteArray>
    suspend fun exportToFile(scene: Scene, path: String, options: ExportOptions): Result<Unit>
}

data class ExportOptions(
    val embedTextures: Boolean = false,
    val binary: Boolean = false,
    val includeAnimations: Boolean = true,
    val maxTextureSize: Int = 4096,
    val compressionLevel: Int = 5
)
```

**Exporter Implementations**:

1. **GLTFExporter**:
    - Serialize scene to GLTF 2.0 JSON
    - Extract buffers, images, accessors
    - Support .gltf (text + files) and .glb (binary)
    - Embed or reference textures
    - Export animations and skinning
    - Libraries: kotlinx.serialization-json

2. **USDZExporter**:
    - Export to USD text format
    - Package USD + textures in ZIP
    - Apple AR Quick Look compatible
    - Support PBR materials (UsdPreviewSurface)
    - Libraries: Platform ZIP, USD generation

3. **OBJExporter**:
    - Export geometry to .obj text
    - Generate .mtl material file
    - Export texture coordinates
    - No animations/skinning (format limitation)
    - Simple text generation

4. **PLYExporter**:
    - Binary or ASCII PLY format
    - Vertex positions, normals, colors, UVs
    - No materials (format limitation)
    - Fast export for point clouds

5. **STLExporter**:
    - Binary or ASCII STL
    - Triangle mesh only
    - 3D printing compatible
    - No colors/materials (format limitation)

6. **ColladaExporter**:
    - XML-based COLLADA 1.4/1.5
    - Full scene hierarchy
    - Animations, skinning, physics
    - Libraries: kotlinx.serialization-xml

**Platform File Writing**:

```kotlin
expect class FileWriter {
    suspend fun writeBytes(path: String, bytes: ByteArray): Result<Unit>
    suspend fun writeText(path: String, text: String): Result<Unit>
}

// JS: Blob + download or File System Access API
// JVM: java.nio.file.Files
// Native: POSIX file APIs
```

**Performance Targets**:

- GLTF export: <2s for 50k triangle scene
- Binary formats: <1s for same scene
- Memory: streaming export, no full scene copy
- Texture compression: parallel encoding

---

## 4. Node-Based Materials (TSL Equivalent)

### Decision

Implement node-based material system using:

- **Node Graph**: Directed acyclic graph (DAG) with typed edges
- **Code Generation**: Convert node graph to WGSL (WebGPU) and SPIR-V (Vulkan)
- **Compilation**: Runtime shader compilation with caching
- **Integration**: Hook into existing Material system via `onBeforeCompile` pattern

### Rationale

- **DAG Representation**: Natural for shader dataflow, prevents cycles
- **Typed Edges**: Compile-time validation, prevents type errors
- **Code Generation**: Enables platform-specific optimization
- **Runtime Compilation**: Supports dynamic material creation
- **Caching**: Avoids recompilation, critical for performance

### Alternatives Considered

1. **Interpreting Nodes at Runtime**: Rejected - too slow, doesn't leverage GPU
2. **Fixed Material Types**: Rejected - inflexible, not node-based
3. **String-Based Shader Concatenation**: Rejected - error-prone, no type safety
4. **Precompiled Node Combinations**: Considered but limits flexibility

### Implementation Notes

**Node Graph Architecture**:

```kotlin
sealed class MaterialNode {
    abstract val id: String
    abstract val inputs: List<NodeInput>
    abstract val outputs: List<NodeOutput>

    abstract fun generateCode(target: ShaderTarget): String
}

data class NodeInput(
    val name: String,
    val type: NodeDataType,
    val connection: NodeOutput? = null,
    val defaultValue: Any? = null
)

data class NodeOutput(
    val name: String,
    val type: NodeDataType,
    val node: MaterialNode
)

enum class NodeDataType {
    FLOAT, VEC2, VEC3, VEC4,
    MAT3, MAT4,
    TEXTURE_2D, TEXTURE_CUBE,
    SAMPLER
}

class NodeGraph {
    private val nodes = mutableListOf<MaterialNode>()
    private val connections = mutableMapOf<NodeInput, NodeOutput>()

    fun addNode(node: MaterialNode)
    fun connect(from: NodeOutput, to: NodeInput)
    fun compile(target: ShaderTarget): CompiledShader
}
```

**Node Categories**:

1. **Input Nodes**:
    - `TextureSampleNode`: Sample 2D/cube textures
    - `AttributeNode`: Access vertex attributes (position, normal, uv)
    - `UniformNode`: Access uniform values
    - `TimeNode`: Access time for animations

2. **Math Nodes**:
    - `AddNode`, `SubtractNode`, `MultiplyNode`, `DivideNode`
    - `DotProductNode`, `CrossProductNode`
    - `NormalizeNode`, `LengthNode`
    - `ClampNode`, `MixNode`, `StepNode`, `SmoothStepNode`

3. **Lighting Nodes**:
    - `PBRNode`: Physical-based lighting calculation
    - `LambertNode`: Diffuse lighting
    - `PhongNode`: Specular lighting
    - `FresnelNode`: Fresnel effect

4. **Utility Nodes**:
    - `SplitNode`: Split vector into components
    - `CombineNode`: Combine components into vector
    - `ConditionalNode`: If/else branching
    - `FunctionNode`: Custom WGSL/SPIR-V code

5. **Output Nodes**:
    - `FragmentOutputNode`: Final fragment color
    - `VertexOutputNode`: Vertex position/normal

**Code Generation**:

```kotlin
interface ShaderCodeGenerator {
    fun generate(graph: NodeGraph): ShaderCode
}

class WGSLCodeGenerator : ShaderCodeGenerator {
    override fun generate(graph: NodeGraph): ShaderCode {
        // Topological sort of nodes
        // Generate WGSL function for each node
        // Connect outputs to inputs
        // Return complete shader
    }
}

class SPIRVCodeGenerator : ShaderCodeGenerator {
    override fun generate(graph: NodeGraph): ShaderCode {
        // Generate SPIR-V assembly
        // Use SPIRV-Cross for validation
        // Return binary SPIR-V
    }
}
```

**Integration with Material System**:

```kotlin
class NodeMaterial : Material() {
    val nodeGraph = NodeGraph()

    override fun onBeforeCompile(shader: Shader) {
        // Generate code from node graph
        val generated = nodeGraph.compile(shader.target)

        // Inject into shader
        shader.fragmentShader = generated.fragmentShader
        shader.vertexShader = generated.vertexShader
    }
}
```

**Performance Targets**:

- Node graph compilation: <100ms for 50 nodes
- Shader caching: Hash-based, <10ms lookup
- Runtime overhead: <1ms per material update
- Memory: <10MB for node graph editor

---

## 5. Geometry Utilities

### Decision

Implement geometry processing utilities using:

- **ConvexHull**: QuickHull algorithm (O(n log n) expected)
- **Simplification**: Quadric error metrics (QEM) for LOD generation
- **Tessellation**: Loop subdivision and Catmull-Clark subdivision
- **Tangent Calculation**: MikkTSpace algorithm for consistent tangents

### Rationale

- **QuickHull**: Industry standard, fast for typical meshes
- **QEM**: Preserves shape better than edge collapse alone
- **MikkTSpace**: De facto standard, ensures tangent consistency across tools
- **Subdivision**: Standard algorithms with proven quality
- **Pure Kotlin**: No native dependencies, fully cross-platform

### Alternatives Considered

1. **Gift Wrapping**: Rejected - O(n²) too slow
2. **Random Edge Collapse**: Rejected - poor quality
3. **Custom Tangent Calc**: Rejected - MikkTSpace is industry standard
4. **Native Libraries**: Considered but Kotlin implementation sufficient

### Implementation Notes

**Geometry Utility Classes**:

1. **ConvexHull**:

```kotlin
object ConvexHullGenerator {
    fun generate(points: List<Vector3>): BufferGeometry {
        // QuickHull algorithm
        // 1. Find initial tetrahedron (4 extreme points)
        // 2. Partition remaining points into outside sets
        // 3. Recursively add points to hull
        // 4. Remove interior faces
        // Returns: Convex hull as indexed triangle mesh
    }
}
```

2. **Mesh Simplification**:

```kotlin
object MeshSimplifier {
    fun simplify(
        geometry: BufferGeometry,
        targetTriangles: Int,
        preserveUVs: Boolean = true,
        preserveNormals: Boolean = true
    ): BufferGeometry {
        // Quadric Error Metrics algorithm
        // 1. Compute quadric matrix for each vertex
        // 2. Compute edge collapse cost for all edges
        // 3. Collapse edge with minimum cost
        // 4. Update quadrics and costs
        // 5. Repeat until target reached
    }

    fun generateLODs(
        geometry: BufferGeometry,
        levels: List<Float> // [1.0, 0.5, 0.25, 0.1]
    ): List<BufferGeometry>
}
```

3. **Tessellation**:

```kotlin
object Tessellator {
    fun subdivideLoop(
        geometry: BufferGeometry,
        iterations: Int = 1
    ): BufferGeometry {
        // Loop subdivision
        // 1. Split each edge at midpoint
        // 2. Update vertex positions with weights
        // 3. Connect new vertices
    }

    fun subdivideCatmullClark(
        geometry: BufferGeometry,
        iterations: Int = 1
    ): BufferGeometry {
        // Catmull-Clark subdivision
        // For quad meshes
    }
}
```

4. **Tangent Generation**:

```kotlin
object TangentGenerator {
    fun computeTangents(
        geometry: BufferGeometry,
        uvChannel: Int = 0
    ) {
        // MikkTSpace algorithm
        // 1. For each vertex, find adjacent triangles
        // 2. Compute tangent and bitangent per triangle
        // 3. Average tangents for shared vertices
        // 4. Compute handedness (w component)
        // Output: vec4 tangents (xyz=tangent, w=handedness)
    }
}
```

5. **Geometry Merging**:

```kotlin
object GeometryMerger {
    fun merge(geometries: List<BufferGeometry>): BufferGeometry {
        // Concatenate vertex buffers
        // Offset indices appropriately
        // Preserve attributes
    }

    fun mergeWithTransforms(
        geometries: List<Pair<BufferGeometry, Matrix4>>
    ): BufferGeometry {
        // Apply transform before merging
    }
}
```

6. **Geometry Analysis**:

```kotlin
object GeometryAnalyzer {
    fun estimateMemoryUsage(geometry: BufferGeometry): Long

    fun validate(geometry: BufferGeometry): ValidationResult

    fun computeBounds(geometry: BufferGeometry): Box3

    fun computeBoundingSphere(geometry: BufferGeometry): Sphere
}
```

7. **Index Conversion**:

```kotlin
object GeometryConverter {
    fun toIndexed(geometry: BufferGeometry): BufferGeometry

    fun toNonIndexed(geometry: BufferGeometry): BufferGeometry
}
```

**Performance Targets**:

- ConvexHull: <100ms for 10k points
- Simplification: <500ms for 100k triangles to 10k
- Tessellation: <200ms per subdivision iteration (10k triangles)
- Tangent calculation: <50ms for 50k vertices
- Memory: In-place operations where possible

---

## 6. Performance Monitoring

### Decision

Implement performance monitoring using:

- **JS**: `performance.now()` for timing, `performance.memory` for memory (Chrome)
- **JVM**: `System.nanoTime()` for timing, `Runtime` memory APIs
- **Native**: Platform-specific high-resolution timers (`clock_gettime`, `QueryPerformanceCounter`)
- **Architecture**: Circular buffer for metrics, async reporting to avoid overhead

### Rationale

- **High-Resolution Timers**: Required for accurate frame time measurement (<1ms precision)
- **Platform APIs**: Each platform has specific APIs, need expect/actual
- **Circular Buffer**: Fixed memory usage, O(1) insert
- **Async Reporting**: Prevents monitoring from affecting measurements
- **60 FPS Target**: Need <16.67ms frame time monitoring

### Alternatives Considered

1. **Date.now()/currentTimeMillis()**: Rejected - insufficient precision
2. **Synchronous Reporting**: Rejected - adds overhead to measurements
3. **Fixed-Size Arrays**: Considered, circular buffer more efficient
4. **External Profiling Tools**: Complement but don't replace built-in monitoring

### Implementation Notes

**Performance Monitor Interface**:

```kotlin
interface PerformanceMonitor {
    // Frame timing
    fun beginFrame()
    fun endFrame()
    fun getFrameTime(): Double // milliseconds
    fun getFPS(): Double

    // Memory
    fun getMemoryUsage(): MemoryInfo

    // GPU
    fun getDrawCalls(): Int
    fun getTriangles(): Int
    fun getTextures(): Int
    fun getGeometries(): Int

    // Metrics history
    fun getMetrics(count: Int = 60): List<FrameMetrics>

    // Configuration
    var enabled: Boolean
    var samplingInterval: Int // frames between samples
}

data class FrameMetrics(
    val frameNumber: Long,
    val frameTime: Double,
    val fps: Double,
    val drawCalls: Int,
    val triangles: Int,
    val memory: MemoryInfo
)

data class MemoryInfo(
    val used: Long, // bytes
    val total: Long,
    val limit: Long
)
```

**Platform Implementations**:

1. **JavaScript**:

```kotlin
actual class PerformanceMonitorImpl : PerformanceMonitor {
    private external fun performanceNow(): Double
    private external fun performanceMemory(): dynamic

    override fun beginFrame() {
        frameStartTime = performanceNow()
    }

    override fun endFrame() {
        val frameTime = performanceNow() - frameStartTime
        recordMetric(frameTime)
    }

    override fun getMemoryUsage(): MemoryInfo {
        val mem = performanceMemory()
        return MemoryInfo(
            used = mem.usedJSHeapSize.toLong(),
            total = mem.totalJSHeapSize.toLong(),
            limit = mem.jsHeapSizeLimit.toLong()
        )
    }
}
```

2. **JVM**:

```kotlin
actual class PerformanceMonitorImpl : PerformanceMonitor {
    private val runtime = Runtime.getRuntime()

    override fun beginFrame() {
        frameStartTime = System.nanoTime()
    }

    override fun endFrame() {
        val frameTime = (System.nanoTime() - frameStartTime) / 1_000_000.0
        recordMetric(frameTime)
    }

    override fun getMemoryUsage(): MemoryInfo {
        return MemoryInfo(
            used = runtime.totalMemory() - runtime.freeMemory(),
            total = runtime.totalMemory(),
            limit = runtime.maxMemory()
        )
    }
}
```

3. **Native (Linux)**:

```kotlin
actual class PerformanceMonitorImpl : PerformanceMonitor {
    @CName("clock_gettime")
    external fun clockGetTime(clockId: Int, timespec: CPointer<timespec>): Int

    override fun beginFrame() {
        frameStartTime = getHighResTime()
    }

    private fun getHighResTime(): Double {
        // Use clock_gettime with CLOCK_MONOTONIC
        // Return time in milliseconds
    }
}
```

**Metrics Collection**:

```kotlin
class MetricsCollector {
    private val buffer = CircularBuffer<FrameMetrics>(capacity = 300) // 5 seconds at 60fps

    fun addMetric(metric: FrameMetrics) {
        buffer.add(metric)
    }

    fun getRecent(count: Int): List<FrameMetrics> {
        return buffer.takeLast(count)
    }

    fun getAverages(windowSize: Int = 60): AverageMetrics {
        val recent = buffer.takeLast(windowSize)
        return AverageMetrics(
            avgFPS = recent.map { it.fps }.average(),
            avgFrameTime = recent.map { it.frameTime }.average(),
            minFPS = recent.minOf { it.fps },
            maxFrameTime = recent.maxOf { it.frameTime }
        )
    }
}
```

**Stats Display** (Optional UI):

```kotlin
class StatsPanel {
    fun render(metrics: List<FrameMetrics>) {
        // Draw FPS graph
        // Draw frame time graph
        // Draw memory usage
        // Draw draw calls
    }
}
```

**Performance Targets**:

- Timer overhead: <0.1ms per frame
- Memory overhead: <1MB for metrics buffer
- Sampling: Every frame for accurate measurements
- Reporting: Async, <1ms impact

---

## 7. Texture Compression

### Decision

Implement platform-appropriate texture compression using:

- **Format Selection**: Runtime detection based on GPU capabilities
- **Desktop (NVIDIA/AMD)**: BC7/BC1 (S3TC/DXT)
- **Mobile (ARM)**: ASTC (iOS/Android), ETC2 (Android fallback)
- **Web**: Basis Universal transcoding to platform format
- **Tools**: Basis Universal for compression, runtime transcoding

### Rationale

- **Platform Differences**: Each GPU architecture has optimal formats
- **Basis Universal**: Single compressed format transcodes to all platforms
- **Runtime Detection**: Query GPU capabilities via WebGPU/Vulkan
- **Memory Savings**: 4:1 to 8:1 compression vs uncompressed
- **Quality**: BC7/ASTC provide near-lossless compression

### Alternatives Considered

1. **Single Format**: Rejected - no format supported everywhere
2. **Uncompressed Textures**: Rejected - memory usage too high
3. **Static Format Selection**: Rejected - doesn't handle GPU variations
4. **JPEG/PNG**: Rejected - must decompress on CPU, defeats purpose

### Implementation Notes

**Texture Compression Architecture**:

```kotlin
enum class TextureFormat {
    // Uncompressed
    RGBA8_UNORM,
    RGBA16_FLOAT,

    // Desktop (BC/S3TC/DXT)
    BC1_RGBA_UNORM,  // DXT1 - 4:1 ratio
    BC3_RGBA_UNORM,  // DXT5 - 4:1 ratio
    BC7_RGBA_UNORM,  // Best quality - 4:1 ratio

    // Mobile (ASTC)
    ASTC_4x4_UNORM,  // 8:1 ratio, high quality
    ASTC_6x6_UNORM,  // 16:1 ratio, medium quality
    ASTC_8x8_UNORM,  // 32:1 ratio, low quality

    // Mobile (ETC)
    ETC2_RGB8_UNORM,  // 6:1 ratio
    ETC2_RGBA8_UNORM, // 4:1 ratio

    // Mobile (PVRTC)
    PVRTC_RGB_4BPP,   // iOS legacy
    PVRTC_RGBA_4BPP,  // iOS legacy

    // Basis Universal (runtime transcoding)
    BASIS_UNIVERSAL
}

interface TextureCompressor {
    fun getSupportedFormats(): Set<TextureFormat>
    fun selectBestFormat(hasAlpha: Boolean, qualityLevel: TextureQuality): TextureFormat
    fun compress(data: ByteArray, format: TextureFormat): CompressedTexture
}

enum class TextureQuality {
    LOW,    // Prefer size
    MEDIUM, // Balance
    HIGH,   // Prefer quality
    ULTRA   // Best quality
}
```

**Platform Format Selection**:

```kotlin
class TextureFormatSelector {
    fun selectFormat(
        capabilities: GPUCapabilities,
        hasAlpha: Boolean,
        quality: TextureQuality
    ): TextureFormat {
        return when {
            // Desktop path
            capabilities.supportsBCFormats -> {
                when (quality) {
                    TextureQuality.ULTRA, TextureQuality.HIGH ->
                        if (hasAlpha) TextureFormat.BC7_RGBA_UNORM
                        else TextureFormat.BC7_RGBA_UNORM
                    else ->
                        if (hasAlpha) TextureFormat.BC3_RGBA_UNORM
                        else TextureFormat.BC1_RGBA_UNORM
                }
            }

            // Mobile path
            capabilities.supportsASTCFormats -> {
                when (quality) {
                    TextureQuality.ULTRA, TextureQuality.HIGH -> TextureFormat.ASTC_4x4_UNORM
                    TextureQuality.MEDIUM -> TextureFormat.ASTC_6x6_UNORM
                    TextureQuality.LOW -> TextureFormat.ASTC_8x8_UNORM
                }
            }

            capabilities.supportsETC2Formats -> {
                if (hasAlpha) TextureFormat.ETC2_RGBA8_UNORM
                else TextureFormat.ETC2_RGB8_UNORM
            }

            // Fallback
            else -> TextureFormat.RGBA8_UNORM
        }
    }
}
```

**Basis Universal Integration**:

```kotlin
class BasisUniversalTranscoder {
    // Load Basis Universal library
    // Web: WASM module
    // JVM: JNI wrapper
    // Native: C library

    suspend fun transcode(
        basisData: ByteArray,
        targetFormat: TextureFormat
    ): ByteArray {
        // Initialize transcoder
        // Transcode to target format
        // Return compressed data
    }

    fun getInfo(basisData: ByteArray): BasisTextureInfo
}

data class BasisTextureInfo(
    val width: Int,
    val height: Int,
    val hasAlpha: Boolean,
    val mipLevels: Int
)
```

**Texture Loading Pipeline**:

```kotlin
class CompressedTextureLoader {
    private val transcoder = BasisUniversalTranscoder()
    private val formatSelector = TextureFormatSelector()

    suspend fun load(
        url: String,
        options: TextureLoadOptions = TextureLoadOptions()
    ): Texture {
        // Load texture file
        val data = fetchBytes(url)

        // Detect format
        val format = detectFormat(data)

        // If Basis Universal, transcode
        if (format == TextureFormat.BASIS_UNIVERSAL) {
            val targetFormat = formatSelector.selectFormat(
                capabilities = renderer.capabilities,
                hasAlpha = transcoder.getInfo(data).hasAlpha,
                quality = options.quality
            )

            val transcoded = transcoder.transcode(data, targetFormat)
            return createTexture(transcoded, targetFormat)
        }

        // Otherwise use as-is
        return createTexture(data, format)
    }
}
```

**Performance Targets**:

- Transcoding: <100ms for 2K texture
- Runtime overhead: <5ms per texture
- Memory savings: 4x-8x vs uncompressed
- Quality: PSNR >40dB for BC7/ASTC 4x4

---

## 8. Shader System

### Decision

Implement shader system with:

- **Chunk System**: Modular shader code libraries (#include preprocessing)
- **onBeforeCompile**: Material callback for runtime shader modification
- **Uniforms Library**: Standard uniforms (lights, fog, matrices)
- **Preprocessing**: Custom preprocessor for #include, #define, #if directives

### Rationale

- **Modularity**: Reuse common shader code (lighting, fog, etc.)
- **Flexibility**: onBeforeCompile allows per-material customization
- **Compatibility**: Matches Three.js shader modification patterns
- **Performance**: Compile-time preprocessing, runtime caching

### Alternatives Considered

1. **String Concatenation**: Rejected - no structure, hard to maintain
2. **Runtime String Replace**: Rejected - fragile, error-prone
3. **Single Monolithic Shaders**: Rejected - no reuse, massive duplication
4. **Template System**: Considered but #include more familiar

### Implementation Notes

**Shader Chunk System**:

```kotlin
object ShaderChunk {
    private val chunks = mutableMapOf<String, String>()

    init {
        // Register standard chunks
        registerChunk("common", """
            struct VertexInput {
                @location(0) position: vec3<f32>,
                @location(1) normal: vec3<f32>,
                @location(2) uv: vec2<f32>
            };

            struct VertexOutput {
                @builtin(position) position: vec4<f32>,
                @location(0) vNormal: vec3<f32>,
                @location(1) vUV: vec2<f32>
            };
        """)

        registerChunk("fog_pars_fragment", """
            struct FogUniforms {
                color: vec3<f32>,
                near: f32,
                far: f32
            };
            @group(1) @binding(3) var<uniform> fog: FogUniforms;
        """)

        registerChunk("fog_fragment", """
            fn applyFog(color: vec3<f32>, depth: f32) -> vec3<f32> {
                let fogFactor = clamp((depth - fog.near) / (fog.far - fog.near), 0.0, 1.0);
                return mix(color, fog.color, fogFactor);
            }
        """)

        // Lighting chunks
        registerChunk("lights_pars_fragment", """
            struct DirectionalLight {
                direction: vec3<f32>,
                color: vec3<f32>,
                intensity: f32
            };
        """)

        registerChunk("lights_fragment", """
            fn calculateLighting(normal: vec3<f32>, viewDir: vec3<f32>) -> vec3<f32> {
                // PBR lighting calculation
            }
        """)
    }

    fun registerChunk(name: String, code: String) {
        chunks[name] = code
    }

    fun getChunk(name: String): String? = chunks[name]
}
```

**Shader Preprocessor**:

```kotlin
class ShaderPreprocessor {
    fun process(source: String, target: ShaderTarget): String {
        var processed = source

        // Handle #include directives
        val includeRegex = """#include\s+<([^>]+)>""".toRegex()
        processed = includeRegex.replace(processed) { match ->
            val chunkName = match.groupValues[1]
            ShaderChunk.getChunk(chunkName)
                ?: throw ShaderError("Chunk not found: $chunkName")
        }

        // Handle #define directives
        val defines = mutableMapOf<String, String>()
        val defineRegex = """#define\s+(\w+)\s+(.*)""".toRegex()
        processed = defineRegex.replace(processed) { match ->
            defines[match.groupValues[1]] = match.groupValues[2]
            ""
        }

        // Replace defined symbols
        for ((symbol, value) in defines) {
            processed = processed.replace(Regex("""\b$symbol\b"""), value)
        }

        // Handle #if directives (simplified)
        processed = processConditionals(processed, defines.keys)

        return processed
    }

    private fun processConditionals(source: String, definedSymbols: Set<String>): String {
        // Handle #if defined(...), #ifdef, #ifndef, #else, #endif
        // Stack-based parsing
        // Return processed code with conditionals resolved
    }
}
```

**onBeforeCompile Pattern**:

```kotlin
abstract class Material {
    // Shader modification hook
    open fun onBeforeCompile(shader: Shader, renderer: Renderer) {
        // Override in subclass to modify shader
    }
}

class CustomMaterial : Material() {
    override fun onBeforeCompile(shader: Shader, renderer: Renderer) {
        // Add custom uniforms
        shader.uniforms["customValue"] = Uniform(1.0f)

        // Modify vertex shader
        shader.vertexShader = shader.vertexShader.replace(
            "// INSERT_CUSTOM_VERTEX",
            "vPosition = position + customValue;"
        )

        // Modify fragment shader
        shader.fragmentShader = shader.fragmentShader.replace(
            "// INSERT_CUSTOM_FRAGMENT",
            "color *= customValue;"
        )
    }
}
```

**Uniforms Library**:

```kotlin
object UniformsLib {
    val common = mapOf(
        "modelMatrix" to Uniform(Matrix4.identity()),
        "modelViewMatrix" to Uniform(Matrix4.identity()),
        "projectionMatrix" to Uniform(Matrix4.identity()),
        "normalMatrix" to Uniform(Matrix3.identity())
    )

    val lights = mapOf(
        "ambientLight" to Uniform(Color(0.1f, 0.1f, 0.1f)),
        "directionalLights" to UniformArray<DirectionalLightUniform>(),
        "pointLights" to UniformArray<PointLightUniform>(),
        "spotLights" to UniformArray<SpotLightUniform>()
    )

    val fog = mapOf(
        "fogColor" to Uniform(Color(0.5f, 0.5f, 0.5f)),
        "fogNear" to Uniform(1.0f),
        "fogFar" to Uniform(1000.0f)
    )
}

object UniformsUtils {
    fun clone(uniforms: Map<String, Uniform>): MutableMap<String, Uniform> {
        // Deep clone uniforms
    }

    fun merge(vararg uniformSets: Map<String, Uniform>): Map<String, Uniform> {
        // Merge multiple uniform sets
    }
}
```

**Shader Class**:

```kotlin
data class Shader(
    var vertexShader: String,
    var fragmentShader: String,
    val uniforms: MutableMap<String, Uniform>,
    val defines: MutableMap<String, String> = mutableMapOf()
) {
    val target: ShaderTarget
        get() = when (Platform.current) {
            Platform.JS -> ShaderTarget.WGSL
            else -> ShaderTarget.SPIRV
        }
}

enum class ShaderTarget {
    WGSL,   // WebGPU Shading Language
    SPIRV,  // Vulkan SPIR-V
    GLSL    // Fallback (not used in this project)
}
```

**Shader Compilation Pipeline**:

```kotlin
class ShaderCompiler {
    private val preprocessor = ShaderPreprocessor()
    private val cache = mutableMapOf<String, CompiledShader>()

    fun compile(shader: Shader): CompiledShader {
        // Generate cache key
        val key = generateCacheKey(shader)

        // Check cache
        cache[key]?.let { return it }

        // Preprocess
        val processedVertex = preprocessor.process(shader.vertexShader, shader.target)
        val processedFragment = preprocessor.process(shader.fragmentShader, shader.target)

        // Compile for target platform
        val compiled = when (shader.target) {
            ShaderTarget.WGSL -> compileWGSL(processedVertex, processedFragment)
            ShaderTarget.SPIRV -> compileSPIRV(processedVertex, processedFragment)
            else -> throw UnsupportedOperationException()
        }

        // Cache and return
        cache[key] = compiled
        return compiled
    }
}
```

**Performance Targets**:

- Preprocessing: <10ms per shader
- Compilation: <50ms per shader (with caching)
- Cache hit rate: >90% in typical applications
- Memory: <100KB per cached shader

---

## 9. Alternative Renderers

### Decision

Implement alternative renderers for 2D/vector output:

- **SVGRenderer**: Generate SVG elements for vector output
- **CSS2DRenderer**: Position DOM elements in 3D space (billboards)
- **CSS3DRenderer**: Transform DOM elements with CSS3D
- **Architecture**: Separate render pipelines, no WebGPU/Vulkan involvement

### Rationale

- **Use Cases**:
    - SVG: Print-quality vector graphics, logos, technical drawings
    - CSS2D: Labels, UI overlays, tooltips in 3D space
    - CSS3D: 3D-transformed HTML content (iframes, videos, complex UI)
- **DOM Integration**: Direct DOM manipulation, no canvas/GPU required
- **Compatibility**: Works where WebGPU unavailable (old browsers, print)
- **Performance**: Limited to low triangle counts (<1000), not for real-time

### Alternatives Considered

1. **Canvas2D Rendering**: Considered but SVG better for vectors
2. **GPU-Based Label Rendering**: Rejected - CSS2D more flexible
3. **All-in-One Renderer**: Rejected - different use cases, separate concerns
4. **WebGL-Based Renderers**: Rejected - project uses WebGPU/Vulkan

### Implementation Notes

**SVGRenderer**:

```kotlin
class SVGRenderer(
    val container: HTMLElement // JS only
) : Renderer {
    private val svg: SVGElement = createSVGElement()
    private val projector = Projector()

    override fun render(scene: Scene, camera: Camera) {
        // Clear previous frame
        svg.clear()

        // Project scene objects to 2D
        val renderList = projector.projectScene(scene, camera)

        // Sort by Z-order
        renderList.sortBy { it.z }

        // Render each object as SVG
        for (item in renderList) {
            when (item.object) {
                is Mesh -> renderMesh(item.object as Mesh)
                is Line -> renderLine(item.object as Line)
                is Points -> renderPoints(item.object as Points)
            }
        }
    }

    private fun renderMesh(mesh: Mesh) {
        // Project triangles to 2D
        // Create SVG <path> elements
        // Apply fill/stroke from material
    }

    private fun renderLine(line: Line) {
        // Create SVG <polyline> or <path>
    }

    private fun renderPoints(points: Points) {
        // Create SVG <circle> elements
    }

    override fun setSize(width: Int, height: Int) {
        svg.setAttribute("width", width.toString())
        svg.setAttribute("height", height.toString())
    }
}
```

**CSS2DRenderer**:

```kotlin
class CSS2DRenderer(
    val container: HTMLElement // JS only
) {
    private val cache = mutableMapOf<Object3D, HTMLElement>()

    fun render(scene: Scene, camera: Camera) {
        // Update camera matrices
        camera.updateMatrixWorld()

        // Traverse scene for CSS2DObjects
        scene.traverse { obj ->
            if (obj is CSS2DObject) {
                // Project 3D position to screen space
                val screenPos = projectToScreen(obj.position, camera)

                // Update DOM element position
                val element = cache.getOrPut(obj) { obj.element }
                element.style.left = "${screenPos.x}px"
                element.style.top = "${screenPos.y}px"

                // Handle depth sorting
                element.style.zIndex = calculateZIndex(obj, camera).toString()
            }
        }
    }

    private fun projectToScreen(position: Vector3, camera: Camera): Vector2 {
        val projected = position.clone()
            .project(camera)

        return Vector2(
            (projected.x * 0.5 + 0.5) * container.clientWidth,
            (-projected.y * 0.5 + 0.5) * container.clientHeight
        )
    }
}

class CSS2DObject(
    val element: HTMLElement
) : Object3D() {
    init {
        element.style.position = "absolute"
    }
}
```

**CSS3DRenderer**:

```kotlin
class CSS3DRenderer(
    val container: HTMLElement // JS only
) {
    private val cache = mutableMapOf<Object3D, HTMLElement>()
    private val cameraElement: HTMLElement = createCameraElement()

    fun render(scene: Scene, camera: Camera) {
        // Get camera matrix
        val cameraMatrix = camera.matrixWorldInverse

        // Apply perspective to container
        val fov = (camera as PerspectiveCamera).fov
        val perspective = "perspective(${0.5 / Math.tan(fov * 0.5 * Math.PI / 180)}px)"
        container.style.transform = perspective

        // Apply camera transform
        val cameraTransform = matrixToCSS3D(cameraMatrix)
        cameraElement.style.transform = cameraTransform

        // Traverse scene for CSS3DObjects
        scene.traverse { obj ->
            if (obj is CSS3DObject) {
                val element = cache.getOrPut(obj) { obj.element }

                // Apply object transform
                val objectTransform = matrixToCSS3D(obj.matrixWorld)
                element.style.transform = objectTransform
            }
        }
    }

    private fun matrixToCSS3D(matrix: Matrix4): String {
        val elements = matrix.elements
        return "matrix3d(" +
            "${elements[0]},${elements[1]},${elements[2]},${elements[3]}," +
            "${elements[4]},${elements[5]},${elements[6]},${elements[7]}," +
            "${elements[8]},${elements[9]},${elements[10]},${elements[11]}," +
            "${elements[12]},${elements[13]},${elements[14]},${elements[15]})"
    }
}

class CSS3DObject(
    val element: HTMLElement
) : Object3D() {
    init {
        element.style.position = "absolute"
        element.style.transformStyle = "preserve-3d"
    }
}
```

**Platform Limitations**:

- **JS Only**: All three renderers require DOM, only available on web
- **Performance**: Not suitable for high triangle counts
- **Recommended Limits**:
    - SVGRenderer: <1000 triangles
    - CSS2DRenderer: <100 objects
    - CSS3DRenderer: <50 objects

**Performance Targets**:

- SVGRenderer: 30 FPS with 500 triangles
- CSS2DRenderer: 60 FPS with 50 labels
- CSS3DRenderer: 30 FPS with 20 transformed elements
- Memory: Minimal, reuse DOM elements

---

## Cross-Cutting Concerns

### Error Handling

All systems use Kotlin `Result<T>` for error propagation:

```kotlin
sealed class KreeKtError {
    data class LoadError(val message: String, val cause: Throwable? = null) : KreeKtError()
    data class CompilationError(val message: String, val shader: String) : KreeKtError()
    data class ValidationError(val message: String, val details: List<String>) : KreeKtError()
    data class PlatformError(val message: String, val platform: Platform) : KreeKtError()
}

// Usage
suspend fun loadModel(url: String): Result<Scene> {
    return try {
        val loader = GLTFLoader()
        loader.load(url)
    } catch (e: Exception) {
        Result.failure(KreeKtError.LoadError("Failed to load: $url", e))
    }
}
```

### Testing Strategy

All implementations follow TDD:

1. **Contract Tests**: API compliance tests
2. **Unit Tests**: Individual component tests
3. **Integration Tests**: Full pipeline tests
4. **Platform Tests**: Platform-specific behavior tests
5. **Performance Tests**: Benchmark against targets

Example test structure:

```kotlin
class PostProcessingTest {
    @Test
    fun `EffectComposer should chain passes correctly`() {
        val composer = EffectComposer(renderer, renderTarget)
        val renderPass = RenderPass(scene, camera)
        val bloomPass = BloomPass()

        composer.addPass(renderPass)
        composer.addPass(bloomPass)

        composer.render(0.016f)

        // Verify pass execution order
        // Verify framebuffer swapping
        // Verify output
    }
}
```

### Documentation

All public APIs require KDoc:

```kotlin
/**
 * Loads a GLTF 2.0 model from the specified URL.
 *
 * @param url The URL or path to the GLTF file (.gltf or .glb)
 * @param onProgress Optional progress callback receiving a value between 0.0 and 1.0
 * @return Result containing the loaded Scene or an error
 *
 * @sample
 * ```kotlin
 * val loader = GLTFLoader()
 * val result = loader.load("model.gltf") { progress ->
 *     println("Loading: ${(progress * 100).toInt()}%")
 * }
 * result.onSuccess { scene ->
 *     sceneRoot.add(scene)
 * }
 * ```

*/
suspend fun load(url: String, onProgress: ((Float) -> Unit)? = null): Result<Scene>

```

---

## Dependencies Summary

### Common Dependencies
- `kotlinx-coroutines-core:1.8.0` - Async operations
- `kotlinx-serialization-json:1.6.0` - JSON parsing (GLTF, etc.)
- `kotlin-math:0.5.0` - Math primitives

### Platform-Specific Dependencies
- **JS**: `@webgpu/types:0.1.40`, Basis Universal WASM
- **JVM**: `LWJGL:3.3.3`, Basis Universal JNI
- **Native**: Platform Vulkan libraries, Basis Universal C library

### Optional Dependencies
- `kotlinx-serialization-xml` - For COLLADA, USD export
- `kotlinx-io` - For streaming file operations
- `kotlinx-atomicfu` - For performance monitoring

---

## Timeline Estimates

Based on complexity and TDD requirements:

1. **Post-Processing**: 3-4 weeks (complex, many passes)
2. **Asset Loaders**: 4-5 weeks (many formats, platform I/O)
3. **Asset Exporters**: 2-3 weeks (fewer formats than loaders)
4. **Node-Based Materials**: 3-4 weeks (complex, code generation)
5. **Geometry Utilities**: 2-3 weeks (algorithms, optimization)
6. **Performance Monitoring**: 1-2 weeks (platform APIs)
7. **Texture Compression**: 2-3 weeks (Basis Universal integration)
8. **Shader System**: 2-3 weeks (preprocessor, chunks)
9. **Alternative Renderers**: 1-2 weeks (JS only, simpler)

**Total**: 20-29 weeks (5-7 months)

**Parallelization**: Some systems can be developed in parallel (e.g., loaders and exporters, performance monitoring and texture compression).

---

## Risk Assessment

### High Risk
- **Basis Universal Integration**: Complex WASM/JNI/C interop
- **Shader Code Generation**: Correctness critical, hard to debug
- **Cross-Platform File I/O**: Different APIs, encoding issues

### Medium Risk
- **Post-Processing Performance**: Must hit 60 FPS target
- **Loader Format Support**: Many formats, edge cases
- **Platform-Specific Bugs**: Different Vulkan drivers

### Low Risk
- **Performance Monitoring**: Well-defined platform APIs
- **Geometry Utilities**: Standard algorithms
- **Alternative Renderers**: JS only, optional feature

### Mitigation Strategies
- **Extensive Testing**: TDD with edge cases
- **Reference Implementations**: Study Three.js and other libraries
- **Progressive Development**: Start with core features, add advanced features incrementally
- **Platform Testing**: Test on all platforms early and often

---

## Conclusion

This research provides concrete technical decisions for implementing complete Three.js r180 feature parity in KreeKt. All decisions prioritize:

1. **Cross-Platform Compatibility**: WebGPU/Vulkan backends
2. **Production Readiness**: No placeholders, TDD approach
3. **Performance**: 60 FPS target maintained
4. **API Familiarity**: Three.js compatibility
5. **Type Safety**: Kotlin type system leveraged

Next steps: Proceed to Phase 1 (Design & Contracts) to define data models and API contracts for each system.
