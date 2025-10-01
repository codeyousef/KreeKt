# Research: Complete Three.js r180 Feature Parity

**Feature**: Complete Three.js r180 Feature Parity
**Branch**: `012-complete-three-js`
**Date**: 2025-10-01
**Status**: Research Complete

## Executive Summary

This research document provides comprehensive technical analysis for implementing complete Three.js r180 API compatibility in KreeKt, a Kotlin Multiplatform 3D graphics library. The goal is 100% feature parity with Three.js r180 while maintaining cross-platform support (JVM, JavaScript, Native, Android, iOS) and adhering to KreeKt's constitutional requirements (60 FPS, <5MB library size, no runtime casts, >80% test coverage).

**Key Finding**: Three.js r180 provides a mature, battle-tested 3D graphics API with ~150 classes and ~500 core methods. The primary technical challenge is adapting browser-centric JavaScript patterns to Kotlin Multiplatform idioms while abstracting platform-specific graphics APIs (WebGL/WebGPU/Vulkan/Metal) behind a unified interface.

**Scope**: 90 functional requirements across 15 major subsystems (geometry, materials, animation, lighting, textures, loaders, post-processing, XR, audio, controls, helpers, raycasting, render targets, shaders, physics integration).

---

## 1. Three.js r180 API Surface Analysis

### Decision

Maintain 1:1 API compatibility with Three.js r180 wherever possible, translating JavaScript patterns to Kotlin idioms (sealed classes, data classes, inline classes, coroutines, nullable types).

### Rationale

- **Migration Path**: Developers familiar with Three.js can transition to KreeKt with minimal learning curve
- **Documentation Leverage**: Existing Three.js documentation, tutorials, and examples translate directly
- **Proven Design**: Three.js API has evolved over 10+ years with extensive real-world validation
- **Ecosystem Integration**: Compatibility enables tooling, asset pipelines, and workflows to transfer

### Three.js r180 Core API Structure

#### Module Categories (15 Major Systems)

1. **Animation**: AnimationClip, AnimationMixer, AnimationAction, KeyframeTrack variants
2. **Audio**: Audio, PositionalAudio, AudioListener, AudioAnalyser
3. **Cameras**: Perspective, Orthographic, Array, Cube, Stereo
4. **Constants**: Texture formats, blending modes, side constants, depth modes
5. **Core**: Object3D, BufferGeometry, BufferAttribute, Clock, EventDispatcher, Raycaster
6. **Extras**: Curves (Line, Bezier, CatmullRom, etc.), Shapes, Paths, Interpolants
7. **Geometries**: Box, Sphere, Plane, Cylinder, Torus, Extrude, Lathe, Text, etc.
8. **Helpers**: Axes, Grid, Box, Arrow, Light helpers, Skeleton helpers
9. **Lights**: Ambient, Directional, Point, Spot, Hemisphere, RectArea, LightProbe
10. **Loaders**: Texture, Cube texture, GLTFLoader, FBXLoader, OBJLoader, FontLoader
11. **Materials**: Basic, Lambert, Phong, Standard, Physical, Toon, Shader, Raw
12. **Math**: Vector2/3/4, Matrix3/4, Quaternion, Euler, Color, Box2/3, Sphere, Ray, Plane, Frustum, Triangle
13. **Objects**: Mesh, SkinnedMesh, InstancedMesh, Line, LineSegments, Points, Sprite, Group, LOD, Bone
14. **Renderers**: WebGLRenderer, WebGPURenderer (experimental), render targets, info, state management
15. **Scenes**: Scene, Fog, FogExp2

#### API Compatibility Matrix

| Three.js Feature Category | Classes | Methods (avg) | KreeKt Status | Adaptation Strategy |
|---------------------------|---------|---------------|---------------|---------------------|
| Math Primitives | 15 | 30 | ✅ Implemented | Direct port with Kotlin operators |
| Core Scene Graph | 8 | 25 | ✅ Implemented | Object3D hierarchy complete |
| Basic Geometries | 15 | 10 | ⚠️ Partial | Add advanced geometries (Extrude, Lathe, Text) |
| Materials | 17 | 15 | ⚠️ Partial | Add PBR materials, shader materials |
| Textures | 12 | 20 | ⚠️ Partial | Add compressed, video, cube textures |
| Animation | 10 | 20 | ⚠️ Partial | Complete AnimationMixer, morph targets, skinning |
| Lighting | 8 | 12 | ⚠️ Partial | Add area lights, light probes, shadow mapping |
| Cameras | 5 | 15 | ✅ Implemented | Complete with ArrayCamera, CubeCamera |
| Loaders | 10 | 8 | ❌ Missing | Implement GLTF, FBX, OBJ, texture loaders |
| Post-Processing | 20 | 5 | ❌ Missing | EffectComposer, passes, built-in effects |
| Helpers | 15 | 8 | ⚠️ Partial | Visual debugging tools complete |
| Audio | 4 | 15 | ❌ Missing | 3D positional audio system |
| XR (VR/AR) | 6 | 20 | ❌ Missing | WebXR, ARKit, ARCore integration |
| Controls | 5 | 12 | ❌ Missing | Orbit, First-Person, Transform controls |
| Raycaster | 1 | 10 | ✅ Implemented | Intersection testing complete |

**Coverage Assessment**:
- **Complete**: ~30% (math, core scene graph, basic cameras, raycaster)
- **Partial**: ~40% (geometries, materials, textures, animation, lighting)
- **Missing**: ~30% (loaders, post-processing, audio, XR, controls)

### Migration Patterns from Three.js to KreeKt

#### JavaScript to Kotlin Idiom Translation

**1. Constructor Patterns**

```javascript
// Three.js
const material = new THREE.MeshStandardMaterial({
    color: 0xff0000,
    roughness: 0.5,
    metalness: 0.8
});
```

```kotlin
// KreeKt - Named parameters
val material = MeshStandardMaterial(
    color = Color(0xff0000),
    roughness = 0.5f,
    metalness = 0.8f
)

// KreeKt - Builder DSL (alternative)
val material = meshStandardMaterial {
    color = Color.red
    roughness = 0.5f
    metalness = 0.8f
}
```

**2. Dynamic Properties**

```javascript
// Three.js - Dynamic object modification
mesh.position.set(1, 2, 3);
mesh.rotation.y = Math.PI / 2;
mesh.userData.customData = { id: 123 };
```

```kotlin
// KreeKt - Type-safe properties
mesh.position.set(1f, 2f, 3f)
mesh.rotation.y = PI.toFloat() / 2f
mesh.userData["customData"] = mapOf("id" to 123)
```

**3. Async Operations**

```javascript
// Three.js - Promises and callbacks
const loader = new THREE.GLTFLoader();
loader.load('model.gltf',
    (gltf) => scene.add(gltf.scene),
    (progress) => console.log(progress),
    (error) => console.error(error)
);
```

```kotlin
// KreeKt - Coroutines
val loader = GLTFLoader()
try {
    val gltf = loader.load("model.gltf") { progress ->
        println("Loading: ${progress.loaded}/${progress.total}")
    }
    scene.add(gltf.scene)
} catch (e: LoaderException) {
    println("Error: ${e.message}")
}
```

**4. Event Handling**

```javascript
// Three.js - EventDispatcher
renderer.addEventListener('webglcontextlost', (event) => {
    event.preventDefault();
    console.log('Context lost');
});
```

```kotlin
// KreeKt - Kotlin Flow or callbacks
renderer.contextLost.collect { event ->
    event.preventDefault()
    println("Context lost")
}

// Alternative: callback style
renderer.onContextLost = { event ->
    event.preventDefault()
    println("Context lost")
}
```

### Implementation Notes

**Phase 1 Priorities** (Highest Impact):
1. Complete geometry system (primitives + advanced geometries)
2. PBR material system (Standard, Physical materials)
3. Texture system (all texture types, compression)
4. Asset loading (GLTF primary, OBJ/FBX secondary)

**Phase 2 Priorities** (Feature Completeness):
1. Animation system (complete AnimationMixer implementation)
2. Advanced lighting (IBL, area lights, shadows)
3. Post-processing pipeline
4. Helper/debugging tools

**Phase 3 Priorities** (Advanced Features):
1. XR support (WebXR, native AR/VR)
2. Audio system (3D positional audio)
3. Camera controls (Orbit, FirstPerson, Transform)
4. Physics integration helpers

### Alternatives Considered

**Option A: Kotlin-First API Design**
- Create idiomatic Kotlin API from scratch
- ❌ Rejected: High migration friction, reinventing proven patterns
- ❌ Rejected: Cannot leverage Three.js documentation ecosystem

**Option B: Thin Wrapper Over Three.js (JS only)**
- Direct bindings to Three.js on JavaScript platform
- ❌ Rejected: Doesn't work for JVM/Native platforms
- ❌ Rejected: Loses type safety and Kotlin idioms

**Option C: Selected Approach - API Parity with Kotlin Idioms**
- Maintain Three.js API structure and method names
- Apply Kotlin language features (sealed classes, data classes, nullability, coroutines)
- ✅ Best migration path, maintains Three.js familiarity
- ✅ Full multiplatform support with platform-specific backends

---

## 2. Graphics API Abstraction Strategy

### Decision

Implement unified `Renderer` interface with platform-specific backends (WebGL2/WebGPU for JS, Vulkan for JVM/Native/Android, Metal for iOS/macOS), using expect/actual pattern for graphics primitives.

### Rationale

- **Platform Independence**: Application code remains platform-agnostic
- **Performance**: Native graphics APIs provide optimal performance on each platform
- **Future-Proof**: Easy to add new backends (DirectX 12, Metal 3) without API changes
- **Three.js Compatibility**: Maintains familiar WebGLRenderer API surface

### Unified Renderer Architecture

```kotlin
// Common API (expect/actual)
expect interface Renderer {
    val domElement: RenderCanvas  // Platform-specific canvas/surface
    val capabilities: RendererCapabilities

    fun setSize(width: Int, height: Int, updateStyle: Boolean = true)
    fun render(scene: Scene, camera: Camera)
    fun renderAsync(scene: Scene, camera: Camera): Deferred<Unit>

    fun setRenderTarget(target: RenderTarget?)
    fun getRenderTarget(): RenderTarget?

    fun setClearColor(color: Color, alpha: Float = 1.0f)
    fun clear(color: Boolean = true, depth: Boolean = true, stencil: Boolean = true)

    fun dispose()
}

// Platform-specific implementations
actual class JvmVulkanRenderer : Renderer { /* LWJGL 3 + Vulkan */ }
actual class JsWebGPURenderer : Renderer { /* WebGPU API */ }
actual class JsWebGLRenderer : Renderer { /* WebGL 2 fallback */ }
actual class NativeVulkanRenderer : Renderer { /* Native Vulkan bindings */ }
actual class IosMetalRenderer : Renderer { /* Metal via MoltenVK */ }
```

### Graphics API Comparison

| Feature | WebGL 2 | WebGPU | Vulkan | Metal | Implementation Strategy |
|---------|---------|--------|--------|-------|------------------------|
| Shader Language | GLSL ES 3.0 | WGSL | SPIR-V | MSL | WGSL source → SPIR-V Cross translation |
| Buffer Management | Typed arrays | GPUBuffer | VkBuffer | MTLBuffer | Abstract as `GraphicsBuffer` |
| Texture Formats | Limited | Extensive | Extensive | Extensive | Runtime capability detection |
| Compute Shaders | ❌ | ✅ | ✅ | ✅ | Optional feature flag |
| Ray Tracing | ❌ | ❌ (future) | ✅ (extension) | ✅ | Optional feature flag |
| Async Operations | Sync only | Native async | Native async | Native async | Coroutines abstraction |
| Error Handling | WebGL errors | Promise rejections | VkResult codes | NSError | Unified Result type |

### Shader Translation Pipeline

**Decision**: Use WGSL (WebGPU Shading Language) as primary shader source, translate to SPIR-V for Vulkan, compile to MSL for Metal.

**Shader Translation Flow**:
```
WGSL Source (Common)
    ↓
┌───────────────┬─────────────────┬─────────────────┐
│               │                 │                 │
WebGPU (JS)   SPIR-V (Vulkan)   MSL (Metal)    GLSL ES (WebGL fallback)
    ↓               ↓                 ↓                 ↓
Native execution  Native execution  Native execution  Native execution
```

**Tools**:
- **Tint**: Google's WGSL compiler (WGSL → SPIR-V, MSL, GLSL)
- **SPIRV-Cross**: SPIR-V → GLSL/MSL/HLSL
- **glslang**: GLSL → SPIR-V (for legacy shaders)

**Shader Abstraction**:
```kotlin
expect class Shader {
    fun compile(source: String, type: ShaderType): Result<CompiledShader>
    fun link(vertex: CompiledShader, fragment: CompiledShader): Result<ShaderProgram>
}

data class ShaderProgram(
    val id: ShaderId,
    val uniforms: Map<String, UniformLocation>,
    val attributes: Map<String, AttributeLocation>
)

// Platform-specific compilation
actual fun Shader.compile(source: String, type: ShaderType): Result<CompiledShader> {
    return when (Platform.current) {
        Platform.JS -> compileWGSL(source, type)
        Platform.JVM -> compileToSPIRV(source, type)
        Platform.IOS -> compileToMSL(source, type)
    }
}
```

### Resource Management Patterns

**Graphics Buffer Abstraction**:
```kotlin
expect class GraphicsBuffer {
    val size: Long
    val usage: BufferUsage

    fun write(data: ByteArray, offset: Long = 0)
    fun read(size: Int, offset: Long = 0): ByteArray
    fun dispose()
}

// Platform implementations
actual class WebGPUBuffer(val gpuBuffer: GPUBuffer) : GraphicsBuffer
actual class VulkanBuffer(val vkBuffer: VkBuffer, val memory: VkDeviceMemory) : GraphicsBuffer
actual class MetalBuffer(val mtlBuffer: MTLBuffer) : GraphicsBuffer
```

**Texture Abstraction**:
```kotlin
expect class GraphicsTexture {
    val width: Int
    val height: Int
    val format: TextureFormat

    fun upload(data: ByteArray, mipLevel: Int = 0)
    fun generateMipmaps()
    fun dispose()
}

// Platform implementations map to native texture types
actual class WebGPUTexture(val gpuTexture: GPUTexture)
actual class VulkanTexture(val vkImage: VkImage, val vkImageView: VkImageView)
actual class MetalTexture(val mtlTexture: MTLTexture)
```

### Render Pipeline State

**Decision**: Abstract pipeline state (blend modes, depth testing, culling) as immutable configuration objects, create pipelines on-demand with caching.

```kotlin
data class RenderPipelineState(
    val shader: ShaderProgram,
    val blendMode: BlendMode,
    val depthTest: DepthTest,
    val culling: CullMode,
    val stencil: StencilOp,
    val topology: PrimitiveTopology
)

interface RenderPipeline {
    val state: RenderPipelineState
    fun bind(encoder: CommandEncoder)
    fun draw(vertexCount: Int, instanceCount: Int = 1)
}

// Platform-specific pipeline creation
expect fun createRenderPipeline(state: RenderPipelineState): RenderPipeline
```

**Pipeline Caching Strategy**:
- Hash pipeline state to generate cache key
- Cache compiled pipelines per state configuration
- Invalidate on shader recompilation or device change
- LRU eviction for memory management

### Performance Targets

| Operation | Target Latency | Rationale |
|-----------|---------------|-----------|
| Frame render | <16.67ms | 60 FPS requirement |
| Buffer upload | <1ms | Streaming updates |
| Texture upload | <5ms | 2K texture |
| Shader compilation | <100ms | Amortize with caching |
| Pipeline creation | <10ms | Cached reuse |

### Alternatives Considered

**Option A: WebGL-Only (Emscripten for Native)**
- Use WebGL everywhere via Emscripten
- ❌ Rejected: Poor performance on native, no modern GPU features

**Option B: OpenGL Everywhere**
- Use OpenGL 4.5 / OpenGL ES 3.2
- ❌ Rejected: OpenGL deprecated on macOS, no WebGPU parity

**Option C: Per-Platform Native APIs** (Selected)
- WebGPU/WebGL2 (JS), Vulkan (JVM/Linux/Windows/Android), Metal (iOS/macOS)
- ✅ Best performance per platform
- ✅ Modern GPU features available
- ✅ Future-proof architecture

---

## 3. Cross-Platform Patterns

### Decision

Use Kotlin Multiplatform expect/actual pattern for platform-specific graphics primitives, with common business logic in `commonMain`.

### Rationale

- **Code Reuse**: Math, scene graph, animation logic shared across platforms (70-80% of codebase)
- **Type Safety**: Compile-time validation of platform implementations
- **Gradual Refinement**: Implement common logic first, add platform specifics incrementally
- **Testing**: Contract tests ensure platform consistency

### Multiplatform Source Set Structure

```
src/
├── commonMain/kotlin/io/kreekt/
│   ├── core/              # Scene graph, Object3D, math
│   ├── geometry/          # Geometry generation algorithms
│   ├── material/          # Material definitions
│   ├── animation/         # Animation system
│   ├── renderer/          # Renderer interface (expect)
│   └── ... (other modules)
│
├── commonTest/kotlin/io/kreekt/
│   └── ...Contract...Test.kt   # Platform-agnostic tests
│
├── jvmMain/kotlin/io/kreekt/
│   └── renderer/
│       ├── VulkanRenderer.kt    # actual Renderer
│       ├── VulkanBuffer.kt      # actual GraphicsBuffer
│       └── VulkanTexture.kt     # actual GraphicsTexture
│
├── jsMain/kotlin/io/kreekt/
│   └── renderer/
│       ├── WebGPURenderer.kt    # actual Renderer
│       ├── WebGLRenderer.kt     # actual Renderer (fallback)
│       └── WebGPUBuffer.kt      # actual GraphicsBuffer
│
├── nativeMain/kotlin/io/kreekt/
│   └── renderer/
│       └── VulkanRenderer.kt    # Shared native Vulkan
│
├── iosMain/kotlin/io/kreekt/
│   └── renderer/
│       └── MetalRenderer.kt     # iOS Metal (via MoltenVK)
│
└── androidNativeArm64Main/kotlin/io/kreekt/
    └── renderer/
        └── VulkanRenderer.kt    # Android Vulkan
```

### Expect/Actual for Graphics Primitives

**Buffer Interface**:
```kotlin
// commonMain - expect declaration
expect class GraphicsBuffer {
    val size: Long
    val usage: BufferUsage

    fun write(data: ByteArray, offset: Long = 0)
    fun read(size: Int, offset: Long = 0): ByteArray
    fun dispose()
}

expect fun createBuffer(size: Long, usage: BufferUsage): GraphicsBuffer

// jvmMain - Vulkan implementation
actual class GraphicsBuffer(
    private val vkBuffer: Long,  // VkBuffer handle
    private val memory: Long     // VkDeviceMemory handle
) {
    actual val size: Long = ...
    actual val usage: BufferUsage = ...

    actual fun write(data: ByteArray, offset: Long) {
        // VkMapMemory, memcpy, VkUnmapMemory
    }

    actual fun read(size: Int, offset: Long): ByteArray {
        // VkMapMemory, memcpy, VkUnmapMemory, return data
    }

    actual fun dispose() {
        vkDestroyBuffer(...)
        vkFreeMemory(...)
    }
}

// jsMain - WebGPU implementation
actual class GraphicsBuffer(
    private val gpuBuffer: GPUBuffer
) {
    actual val size: Long = gpuBuffer.size.toLong()
    actual val usage: BufferUsage = ...

    actual fun write(data: ByteArray, offset: Long) {
        gpuBuffer.getMappedRange(offset, data.size.toLong())
            .set(data.toTypedArray())
        gpuBuffer.unmap()
    }

    actual fun read(size: Int, offset: Long): ByteArray = runBlocking {
        // Read back via staging buffer
    }

    actual fun dispose() {
        gpuBuffer.destroy()
    }
}
```

**Texture Interface**:
```kotlin
// commonMain
expect class GraphicsTexture {
    val width: Int
    val height: Int
    val format: TextureFormat

    fun upload(data: ByteArray, mipLevel: Int = 0)
    fun generateMipmaps()
    fun dispose()
}

// Platform-specific implementations follow same pattern as GraphicsBuffer
```

**Shader Interface**:
```kotlin
// commonMain
expect class ShaderProgram {
    val id: ShaderId
    val uniforms: Map<String, UniformLocation>
    val attributes: Map<String, AttributeLocation>

    fun bind()
    fun setUniform(name: String, value: Any)
    fun dispose()
}

expect fun compileShader(vertexSource: String, fragmentSource: String): Result<ShaderProgram>
```

### Platform-Specific Initialization

**Renderer Creation Pattern**:
```kotlin
// commonMain - expect factory function
expect fun createRenderer(config: RendererConfiguration): Result<Renderer>

// jvmMain - Vulkan initialization
actual fun createRenderer(config: RendererConfiguration): Result<Renderer> {
    return try {
        // Initialize GLFW
        if (!GLFW.glfwInit()) {
            return Result.failure(RendererException("GLFW initialization failed"))
        }

        // Create Vulkan instance
        val instance = createVulkanInstance()

        // Select physical device
        val physicalDevice = selectPhysicalDevice(instance, config)

        // Create logical device
        val device = createLogicalDevice(physicalDevice, config)

        Result.success(JvmVulkanRenderer(instance, device, physicalDevice))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// jsMain - WebGPU initialization
actual fun createRenderer(config: RendererConfiguration): Result<Renderer> = runBlocking {
    try {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        val context = canvas.getContext("webgpu") as GPUCanvasContext

        // Request adapter and device
        val adapter = window.navigator.gpu.requestAdapter().await()
        val device = adapter.requestDevice().await()

        // Configure canvas
        context.configure(object {
            val device = device
            val format = "bgra8unorm"
        })

        Result.success(JsWebGPURenderer(canvas, context, device))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Resource Management Across Platforms

**Memory Lifetime Management**:
```kotlin
interface Disposable {
    fun dispose()
}

abstract class GraphicsResource : Disposable {
    private var disposed = false

    protected abstract fun doDispose()

    final override fun dispose() {
        if (!disposed) {
            doDispose()
            disposed = true
        }
    }

    protected fun finalize() {
        if (!disposed) {
            println("Warning: Resource not disposed properly")
            dispose()
        }
    }
}

// Usage
class Mesh(
    val geometry: BufferGeometry,
    val material: Material
) : Object3D(), Disposable {
    override fun dispose() {
        geometry.dispose()
        material.dispose()
    }
}
```

### Performance Targets by Platform

| Platform | Target FPS | Triangle Budget | Memory Budget |
|----------|-----------|-----------------|---------------|
| Desktop (JVM) | 60 FPS | 1M+ triangles | 2-4 GB VRAM |
| Web (WebGPU) | 60 FPS | 500K triangles | 512 MB - 1 GB |
| Web (WebGL2) | 30-60 FPS | 100K triangles | 256 MB |
| Mobile (iOS) | 60 FPS | 300K triangles | 512 MB |
| Mobile (Android) | 60 FPS | 200K triangles | 256-512 MB |

### Alternatives Considered

**Option A: Abstract Everything**
- Create abstractions for every platform difference
- ❌ Rejected: Over-abstraction, performance overhead

**Option B: Platform-Specific Codebases**
- Separate codebases per platform
- ❌ Rejected: Code duplication, maintenance burden

**Option C: Expect/Actual with Common Logic** (Selected)
- Share business logic, isolate platform specifics
- ✅ Optimal code reuse (70-80%)
- ✅ Type-safe platform implementations
- ✅ Flexible per-platform optimization

---

## 4. Asset Loading Architecture

### Decision

Use Okio for cross-platform IO, platform-specific decoders for formats (GLTF, FBX, OBJ, images), coroutines for async loading with progress tracking.

### Rationale

- **Okio**: Mature multiplatform IO library with efficient buffered streams
- **Platform Decoders**: Leverage native image decoders (ImageIO, Canvas, Skia) for performance
- **Coroutines**: Natural async/await patterns, cancellation support, progress reporting
- **Progressive Loading**: Stream large assets without blocking main thread

### Asset Loading System Architecture

```kotlin
// Loader base interface
interface AssetLoader<T> {
    suspend fun load(url: String, onProgress: (ProgressEvent) -> Unit = {}): Result<T>
    fun loadSync(url: String): Result<T>  // Blocking version for simple cases
}

data class ProgressEvent(
    val loaded: Long,
    val total: Long,
    val percentage: Float = (loaded.toFloat() / total.toFloat()) * 100f
)

// Platform-agnostic loading manager
class LoadingManager {
    private val loaders = mutableMapOf<String, AssetLoader<*>>()
    private val cache = mutableMapOf<String, Any>()

    fun <T> registerLoader(extension: String, loader: AssetLoader<T>) {
        loaders[extension] = loader
    }

    suspend fun <T> load(url: String, onProgress: (ProgressEvent) -> Unit = {}): Result<T> {
        // Check cache first
        cache[url]?.let { return Result.success(it as T) }

        // Determine loader by extension
        val extension = url.substringAfterLast('.')
        val loader = loaders[extension] as? AssetLoader<T>
            ?: return Result.failure(UnsupportedFormatException(extension))

        // Load asset
        return loader.load(url, onProgress).onSuccess { asset ->
            cache[url] = asset as Any
        }
    }
}
```

### Cross-Platform IO with Okio

**File Reading Pattern**:
```kotlin
// Common loading logic
suspend fun loadFile(path: String): ByteArray = withContext(Dispatchers.IO) {
    val source = when (Platform.current) {
        Platform.JVM -> FileSystem.SYSTEM.source(Path(path))
        Platform.JS -> fetchSource(path)  // Fetch API
        Platform.NATIVE -> FileSystem.SYSTEM.source(Path(path))
    }

    source.buffer().use { bufferedSource ->
        bufferedSource.readByteArray()
    }
}

// expect/actual for platform-specific details
expect suspend fun fetchSource(url: String): Source

// jsMain - Fetch API
actual suspend fun fetchSource(url: String): Source {
    val response = window.fetch(url).await()
    val arrayBuffer = response.arrayBuffer().await()
    return Buffer().write(arrayBuffer.toByteArray())
}

// jvmMain - File system
actual suspend fun fetchSource(url: String): Source {
    return FileSystem.SYSTEM.source(Path(url))
}
```

### Format Support Matrix

| Format | Use Case | Decoder | Platforms | Priority |
|--------|----------|---------|-----------|----------|
| **GLTF 2.0** | Primary 3D models | kotlinx.serialization + JSON | All | High |
| **GLB** | Binary GLTF | Custom binary parser | All | High |
| **FBX** | Game assets | FBX SDK bindings (JVM), JS library | JVM, JS | Medium |
| **OBJ** | Simple models | Pure Kotlin parser | All | Medium |
| **PNG** | Textures | Platform decoders | All | High |
| **JPEG** | Textures | Platform decoders | All | High |
| **WebP** | Efficient textures | Platform decoders | All | Medium |
| **KTX2** | Compressed textures | Basis Universal | All | High |
| **EXR** | HDR images | OpenEXR bindings | JVM, Native | Low |
| **HDR (RGBE)** | HDR images | Pure Kotlin decoder | All | Medium |
| **TTF/OTF** | Fonts for TextGeometry | FreeType bindings | All | Medium |

### GLTF Loader Implementation

**GLTF Loading Pipeline**:
```kotlin
class GLTFLoader : AssetLoader<GLTF> {
    override suspend fun load(
        url: String,
        onProgress: (ProgressEvent) -> Unit
    ): Result<GLTF> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Load JSON
            val json = loadFile(url).decodeToString()
            val gltfData = Json.decodeFromString<GLTFData>(json)

            // Step 2: Load buffers
            val baseUrl = url.substringBeforeLast('/')
            val buffers = gltfData.buffers.mapIndexed { index, bufferInfo ->
                val bufferUrl = "$baseUrl/${bufferInfo.uri}"
                loadFile(bufferUrl).also { bytes ->
                    onProgress(ProgressEvent(
                        loaded = (index + 1).toLong() * bufferInfo.byteLength,
                        total = gltfData.buffers.sumOf { it.byteLength }.toLong()
                    ))
                }
            }

            // Step 3: Parse geometry
            val geometries = parseGeometries(gltfData, buffers)

            // Step 4: Load textures
            val textures = loadTextures(gltfData, baseUrl, onProgress)

            // Step 5: Parse materials
            val materials = parseMaterials(gltfData, textures)

            // Step 6: Build scene graph
            val scenes = buildScenes(gltfData, geometries, materials)

            // Step 7: Load animations
            val animations = parseAnimations(gltfData, buffers)

            Result.success(GLTF(
                scenes = scenes,
                animations = animations,
                cameras = parseCameras(gltfData)
            ))
        } catch (e: Exception) {
            Result.failure(LoaderException("GLTF loading failed", e))
        }
    }

    private fun parseGeometries(gltf: GLTFData, buffers: List<ByteArray>): List<BufferGeometry> {
        return gltf.meshes.flatMap { mesh ->
            mesh.primitives.map { primitive ->
                val geometry = BufferGeometry()

                // Parse positions
                val posAccessor = gltf.accessors[primitive.attributes.POSITION]
                val positions = parseAccessor(posAccessor, buffers, gltf.bufferViews)
                geometry.setAttribute("position", BufferAttribute(positions, 3))

                // Parse normals
                primitive.attributes.NORMAL?.let { normalIdx ->
                    val normals = parseAccessor(gltf.accessors[normalIdx], buffers, gltf.bufferViews)
                    geometry.setAttribute("normal", BufferAttribute(normals, 3))
                }

                // Parse UVs
                primitive.attributes.TEXCOORD_0?.let { uvIdx ->
                    val uvs = parseAccessor(gltf.accessors[uvIdx], buffers, gltf.bufferViews)
                    geometry.setAttribute("uv", BufferAttribute(uvs, 2))
                }

                // Parse indices
                primitive.indices?.let { indicesIdx ->
                    val indices = parseAccessor(gltf.accessors[indicesIdx], buffers, gltf.bufferViews)
                    geometry.setIndex(BufferAttribute(indices, 1))
                }

                geometry
            }
        }
    }
}
```

### Texture Loading with Platform Decoders

```kotlin
// Common texture loading interface
expect suspend fun decodeImage(data: ByteArray): ImageData

data class ImageData(
    val width: Int,
    val height: Int,
    val pixels: ByteArray,  // RGBA8
    val format: TextureFormat = TextureFormat.RGBA8
)

// jvmMain - ImageIO
actual suspend fun decodeImage(data: ByteArray): ImageData = withContext(Dispatchers.IO) {
    val inputStream = ByteArrayInputStream(data)
    val bufferedImage = ImageIO.read(inputStream)

    val width = bufferedImage.width
    val height = bufferedImage.height
    val pixels = ByteArray(width * height * 4)

    // Extract RGBA data
    for (y in 0 until height) {
        for (x in 0 until width) {
            val rgb = bufferedImage.getRGB(x, y)
            val offset = (y * width + x) * 4
            pixels[offset + 0] = ((rgb shr 16) and 0xFF).toByte()  // R
            pixels[offset + 1] = ((rgb shr 8) and 0xFF).toByte()   // G
            pixels[offset + 2] = (rgb and 0xFF).toByte()           // B
            pixels[offset + 3] = ((rgb shr 24) and 0xFF).toByte()  // A
        }
    }

    ImageData(width, height, pixels)
}

// jsMain - Canvas ImageData
actual suspend fun decodeImage(data: ByteArray): ImageData = suspendCoroutine { cont ->
    val blob = Blob(arrayOf(data), BlobPropertyBag(type = "image/png"))
    val img = Image()

    img.onload = {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = img.width
        canvas.height = img.height

        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        ctx.drawImage(img, 0.0, 0.0)

        val imageData = ctx.getImageData(0.0, 0.0, img.width.toDouble(), img.height.toDouble())
        cont.resume(ImageData(
            width = img.width,
            height = img.height,
            pixels = imageData.data.unsafeCast<ByteArray>()
        ))
    }

    img.onerror = { _, _, _, _, _ ->
        cont.resumeWithException(LoaderException("Image decode failed"))
    }

    img.src = URL.createObjectURL(blob)
}
```

### Async Loading Patterns

**Parallel Loading**:
```kotlin
suspend fun loadMultipleAssets(urls: List<String>): List<Any> = coroutineScope {
    urls.map { url ->
        async { loadingManager.load<Any>(url) }
    }.awaitAll().mapNotNull { it.getOrNull() }
}

// Usage
val (model, texture1, texture2) = loadMultipleAssets(listOf(
    "model.gltf",
    "texture1.png",
    "texture2.png"
))
```

**Streaming Large Assets**:
```kotlin
class StreamingTextureLoader : AssetLoader<Texture> {
    override suspend fun load(
        url: String,
        onProgress: (ProgressEvent) -> Unit
    ): Result<Texture> = withContext(Dispatchers.IO) {
        val texture = Texture()

        // Load progressively at multiple resolutions
        val lowRes = loadFile("$url?size=256").let { decodeImage(it) }
        texture.image = lowRes
        texture.needsUpdate = true
        onProgress(ProgressEvent(256L * 256L * 4L, 2048L * 2048L * 4L))

        val fullRes = loadFile(url).let { decodeImage(it) }
        texture.image = fullRes
        texture.needsUpdate = true
        onProgress(ProgressEvent(2048L * 2048L * 4L, 2048L * 2048L * 4L))

        Result.success(texture)
    }
}
```

### Performance Targets

| Operation | Target Time | Notes |
|-----------|-------------|-------|
| GLTF parse | <500ms | 10MB file |
| Texture decode | <100ms | 2K PNG |
| Buffer upload | <50ms | 10MB buffer |
| Total load time | <3s | Typical scene |

### Alternatives Considered

**Option A: Pure Kotlin Parsers**
- Implement all format parsers in pure Kotlin
- ❌ Rejected: Reinventing wheel, poor performance for complex formats (FBX, EXR)

**Option B: Native Library Bindings Everywhere**
- Use Assimp, FBX SDK, OpenEXR via FFI
- ❌ Rejected: Platform compatibility issues, large binary size

**Option C: Hybrid Approach** (Selected)
- Simple formats (OBJ, GLTF) in pure Kotlin
- Complex formats use platform libraries where available
- ✅ Balance between portability and performance
- ✅ Smallest binary size

---

## 5. Animation System Design

### Decision

Port Three.js AnimationMixer architecture to Kotlin with coroutines for time management, supporting keyframe tracks, morph targets, skeletal animation, and animation blending.

### Rationale

- **Proven Design**: Three.js AnimationMixer has 10+ years of refinement
- **Feature Complete**: Supports all common animation types (transform, morph, skeletal)
- **Flexible**: Allows blending, crossfading, time warping, weight control
- **Performant**: Update loop optimized for thousands of animated objects

### Animation System Architecture

```kotlin
// Animation data structures
data class AnimationClip(
    val name: String,
    val duration: Float,  // seconds
    val tracks: List<KeyframeTrack>
)

sealed class KeyframeTrack {
    abstract val name: String  // Property path (e.g., ".position[x]")
    abstract val times: FloatArray
    abstract val values: FloatArray
    abstract val interpolation: InterpolationMode

    data class VectorTrack(
        override val name: String,
        override val times: FloatArray,
        override val values: FloatArray,  // Flattened [x,y,z, x,y,z, ...]
        override val interpolation: InterpolationMode = InterpolationMode.LINEAR
    ) : KeyframeTrack()

    data class QuaternionTrack(
        override val name: String,
        override val times: FloatArray,
        override val values: FloatArray,  // Flattened [x,y,z,w, x,y,z,w, ...]
        override val interpolation: InterpolationMode = InterpolationMode.SLERP
    ) : KeyframeTrack()

    data class NumberTrack(
        override val name: String,
        override val times: FloatArray,
        override val values: FloatArray,
        override val interpolation: InterpolationMode = InterpolationMode.LINEAR
    ) : KeyframeTrack()

    data class ColorTrack(
        override val name: String,
        override val times: FloatArray,
        override val values: FloatArray,  // Flattened [r,g,b, r,g,b, ...]
        override val interpolation: InterpolationMode = InterpolationMode.LINEAR
    ) : KeyframeTrack()

    data class BooleanTrack(
        override val name: String,
        override val times: FloatArray,
        override val values: FloatArray,  // 0.0 = false, 1.0 = true
        override val interpolation: InterpolationMode = InterpolationMode.DISCRETE
    ) : KeyframeTrack()
}

enum class InterpolationMode {
    LINEAR,       // Linear interpolation
    DISCRETE,     // Step interpolation (no smoothing)
    CUBIC,        // Cubic spline interpolation
    SLERP         // Spherical linear interpolation (quaternions)
}

// Animation mixer
class AnimationMixer(val root: Object3D) {
    private val actions = mutableMapOf<String, AnimationAction>()
    private var time: Float = 0f

    fun clipAction(clip: AnimationClip): AnimationAction {
        return actions.getOrPut(clip.name) {
            AnimationAction(this, clip, root)
        }
    }

    fun update(deltaTime: Float) {
        time += deltaTime
        actions.values.forEach { action ->
            if (action.enabled) {
                action.update(deltaTime)
            }
        }
    }

    fun stopAllAction() {
        actions.values.forEach { it.stop() }
    }
}

// Animation action (playback control)
class AnimationAction(
    private val mixer: AnimationMixer,
    val clip: AnimationClip,
    private val root: Object3D
) {
    var enabled: Boolean = true
    var paused: Boolean = false
    var loop: LoopMode = LoopMode.LOOP
    var repetitions: Int = Int.MAX_VALUE
    var timeScale: Float = 1.0f
    var weight: Float = 1.0f
    var time: Float = 0f

    private var blendAlpha: Float = 1.0f
    private var fadeDirection: Int = 0
    private var fadeDuration: Float = 0f
    private var fadeStartTime: Float = 0f

    fun play(): AnimationAction {
        enabled = true
        paused = false
        time = 0f
        return this
    }

    fun stop(): AnimationAction {
        enabled = false
        time = 0f
        return this
    }

    fun pause(): AnimationAction {
        paused = true
        return this
    }

    fun fadeIn(duration: Float): AnimationAction {
        fadeDuration = duration
        fadeDirection = 1
        fadeStartTime = time
        return this
    }

    fun fadeOut(duration: Float): AnimationAction {
        fadeDuration = duration
        fadeDirection = -1
        fadeStartTime = time
        return this
    }

    fun crossFadeTo(action: AnimationAction, duration: Float): AnimationAction {
        fadeOut(duration)
        action.fadeIn(duration).play()
        return this
    }

    fun update(deltaTime: Float) {
        if (paused) return

        // Update time
        time += deltaTime * timeScale

        // Handle looping
        when (loop) {
            LoopMode.ONCE -> {
                if (time >= clip.duration) {
                    time = clip.duration
                    enabled = false
                }
            }
            LoopMode.LOOP -> {
                time %= clip.duration
            }
            LoopMode.PING_PONG -> {
                val cycles = (time / clip.duration).toInt()
                if (cycles % 2 == 0) {
                    time %= clip.duration
                } else {
                    time = clip.duration - (time % clip.duration)
                }
            }
        }

        // Update fade
        if (fadeDirection != 0) {
            val fadeTime = time - fadeStartTime
            blendAlpha = (fadeTime / fadeDuration).coerceIn(0f, 1f)
            if (fadeDirection < 0) blendAlpha = 1f - blendAlpha
            if (fadeTime >= fadeDuration) fadeDirection = 0
        }

        // Apply animation tracks
        clip.tracks.forEach { track ->
            applyTrack(track, time, blendAlpha * weight)
        }
    }

    private fun applyTrack(track: KeyframeTrack, time: Float, weight: Float) {
        // Find keyframe interval
        val keyIndex = track.times.binarySearch(time).let {
            if (it >= 0) it else -(it + 1) - 1
        }.coerceIn(0, track.times.size - 2)

        val t0 = track.times[keyIndex]
        val t1 = track.times[keyIndex + 1]
        val alpha = ((time - t0) / (t1 - t0)).coerceIn(0f, 1f)

        // Interpolate value
        val value = when (track) {
            is KeyframeTrack.VectorTrack -> {
                interpolateVector(track.values, keyIndex, alpha, track.interpolation)
            }
            is KeyframeTrack.QuaternionTrack -> {
                interpolateQuaternion(track.values, keyIndex, alpha)
            }
            is KeyframeTrack.NumberTrack -> {
                interpolateNumber(track.values, keyIndex, alpha, track.interpolation)
            }
            is KeyframeTrack.ColorTrack -> {
                interpolateVector(track.values, keyIndex, alpha, track.interpolation)
            }
            is KeyframeTrack.BooleanTrack -> {
                track.values[keyIndex]  // Discrete
            }
        }

        // Apply to target object
        applyValueToObject(root, track.name, value, weight)
    }

    private fun interpolateVector(values: FloatArray, index: Int, alpha: Float, mode: InterpolationMode): Vector3 {
        val i0 = index * 3
        val i1 = (index + 1) * 3

        return when (mode) {
            InterpolationMode.LINEAR -> {
                Vector3(
                    lerp(values[i0], values[i1], alpha),
                    lerp(values[i0 + 1], values[i1 + 1], alpha),
                    lerp(values[i0 + 2], values[i1 + 2], alpha)
                )
            }
            InterpolationMode.DISCRETE -> {
                Vector3(values[i0], values[i0 + 1], values[i0 + 2])
            }
            InterpolationMode.CUBIC -> {
                // Catmull-Rom spline interpolation
                cubicInterpolate(values, index, alpha)
            }
            else -> throw UnsupportedOperationException("Interpolation mode $mode not supported for vectors")
        }
    }

    private fun interpolateQuaternion(values: FloatArray, index: Int, alpha: Float): Quaternion {
        val i0 = index * 4
        val i1 = (index + 1) * 4

        val q0 = Quaternion(values[i0], values[i0 + 1], values[i0 + 2], values[i0 + 3])
        val q1 = Quaternion(values[i1], values[i1 + 1], values[i1 + 2], values[i1 + 3])

        return q0.slerp(q1, alpha)
    }
}

enum class LoopMode {
    ONCE,
    LOOP,
    PING_PONG
}
```

### Skeletal Animation

**Skeleton System**:
```kotlin
data class Skeleton(
    val bones: List<Bone>,
    val boneInverses: List<Matrix4>  // Bind pose inverse matrices
) {
    val boneMatrices: FloatArray = FloatArray(bones.size * 16)

    fun update() {
        bones.forEachIndexed { index, bone ->
            // Calculate bone matrix: boneMatrix = bone.matrixWorld * boneInverse
            val boneMatrix = bone.matrixWorld.clone().multiply(boneInverses[index])

            // Copy to flat array for GPU upload
            boneMatrix.toArray().copyInto(boneMatrices, index * 16)
        }
    }
}

class Bone(name: String) : Object3D(name) {
    // Bones are just Object3D nodes in the skeleton hierarchy
}

class SkinnedMesh(
    geometry: BufferGeometry,
    material: Material,
    val skeleton: Skeleton
) : Mesh(geometry, material) {
    init {
        // Ensure geometry has skinning attributes
        require(geometry.attributes.containsKey("skinIndex"))
        require(geometry.attributes.containsKey("skinWeight"))
    }

    override fun updateMatrixWorld(force: Boolean) {
        super.updateMatrixWorld(force)
        skeleton.update()
    }
}
```

**Skinning Shader**:
```wgsl
// Vertex shader for skinned mesh
struct VertexInput {
    @location(0) position: vec3f,
    @location(1) normal: vec3f,
    @location(2) uv: vec2f,
    @location(3) skinIndex: vec4u,   // Bone indices (4 bones per vertex)
    @location(4) skinWeight: vec4f,  // Bone weights (sum = 1.0)
}

@group(0) @binding(0) var<uniform> modelViewProjection: mat4x4f;
@group(0) @binding(1) var<storage, read> boneMatrices: array<mat4x4f>;

@vertex
fn vertexMain(input: VertexInput) -> VertexOutput {
    // Calculate skinned position
    var skinnedPosition = vec4f(0.0, 0.0, 0.0, 0.0);
    var skinnedNormal = vec3f(0.0, 0.0, 0.0);

    for (var i = 0u; i < 4u; i = i + 1u) {
        let boneIndex = input.skinIndex[i];
        let boneWeight = input.skinWeight[i];
        let boneMatrix = boneMatrices[boneIndex];

        skinnedPosition += boneMatrix * vec4f(input.position, 1.0) * boneWeight;
        skinnedNormal += (boneMatrix * vec4f(input.normal, 0.0)).xyz * boneWeight;
    }

    // Transform skinned position to clip space
    var output: VertexOutput;
    output.position = modelViewProjection * skinnedPosition;
    output.normal = normalize(skinnedNormal);
    output.uv = input.uv;
    return output;
}
```

### Morph Target Animation

```kotlin
class MorphTargetGeometry(
    val baseGeometry: BufferGeometry,
    val morphTargets: List<MorphTarget>
) {
    data class MorphTarget(
        val name: String,
        val positions: FloatArray,
        val normals: FloatArray? = null
    )

    fun applyMorphTargets(influences: FloatArray): BufferGeometry {
        require(influences.size == morphTargets.size)

        val basePositions = baseGeometry.attributes["position"]!!.array
        val morphedPositions = basePositions.copyOf()

        morphTargets.forEachIndexed { index, target ->
            val influence = influences[index]
            if (influence > 0.001f) {
                for (i in target.positions.indices) {
                    morphedPositions[i] += target.positions[i] * influence
                }
            }
        }

        val morphedGeometry = baseGeometry.clone()
        morphedGeometry.setAttribute("position", BufferAttribute(morphedPositions, 3))
        return morphedGeometry
    }
}

// Usage with AnimationMixer
val clip = AnimationClip(
    name = "FacialExpression",
    duration = 2.0f,
    tracks = listOf(
        KeyframeTrack.NumberTrack(
            name = ".morphTargetInfluences[0]",  // "smile" morph target
            times = floatArrayOf(0f, 1f, 2f),
            values = floatArrayOf(0f, 1f, 0f),
            interpolation = InterpolationMode.LINEAR
        )
    )
)
```

### Performance Optimization

**Animation Update Batching**:
```kotlin
class AnimationSystem {
    private val mixers = mutableListOf<AnimationMixer>()

    fun register(mixer: AnimationMixer) {
        mixers.add(mixer)
    }

    fun updateAll(deltaTime: Float) {
        // Batch updates to improve cache coherency
        mixers.forEach { it.update(deltaTime) }
    }
}
```

**Keyframe Binary Search Optimization**:
- Cache last keyframe index per track
- Most animations access sequential keyframes
- Binary search only when time jumps

**SIMD Interpolation** (platform-specific):
```kotlin
// jvmMain - use SIMD intrinsics where available
expect fun interpolateVectorsSIMD(
    v0: FloatArray,
    v1: FloatArray,
    alpha: Float,
    out: FloatArray
)

// jvmMain implementation
actual fun interpolateVectorsSIMD(...) {
    // Use jdk.incubator.vector if available
}
```

### Performance Targets

| Operation | Target Time | Notes |
|-----------|-------------|-------|
| AnimationMixer.update() | <1ms | 100 actions |
| Skeleton.update() | <0.5ms | 50 bones |
| Morph target blend | <2ms | 8 targets, 10K vertices |

### Alternatives Considered

**Option A: Custom Timeline System**
- Design animation system from scratch
- ❌ Rejected: Reinventing proven design, more complexity

**Option B: Use Platform Animation APIs**
- CoreAnimation (iOS), Animator (Android), CSS animations (Web)
- ❌ Rejected: Inconsistent APIs, limited control, not for 3D

**Option C: Three.js AnimationMixer Port** (Selected)
- Direct port of battle-tested system
- ✅ Feature complete, proven performance
- ✅ Familiar to Three.js developers

---

## 6. Performance Optimization Strategies

### Decision

Implement multi-level optimization strategy: instance rendering, LOD system, frustum culling, batching, memory pooling, and adaptive quality settings.

### Rationale

- **60 FPS Target**: Constitutional requirement across all platforms
- **100K+ Triangles**: Performance budget for desktop, scaled for mobile
- **Memory Efficiency**: Minimize allocations, reuse objects
- **Adaptive Quality**: Maintain frame rate by adjusting quality

### Instance Rendering

**GPU Instancing for Duplicate Objects**:
```kotlin
class InstancedMesh(
    geometry: BufferGeometry,
    material: Material,
    val count: Int
) : Mesh(geometry, material) {
    val instanceMatrix: InstancedBufferAttribute = InstancedBufferAttribute(FloatArray(count * 16), 16)
    val instanceColor: InstancedBufferAttribute? = null

    private val _matrix = Matrix4()
    private val _color = Color()

    fun setMatrixAt(index: Int, matrix: Matrix4) {
        matrix.toArray().copyInto(instanceMatrix.array, index * 16)
        instanceMatrix.needsUpdate = true
    }

    fun getMatrixAt(index: Int, matrix: Matrix4) {
        matrix.fromArray(instanceMatrix.array, index * 16)
    }

    fun setColorAt(index: Int, color: Color) {
        if (instanceColor == null) {
            throw IllegalStateException("Instance colors not initialized")
        }
        color.toArray().copyInto(instanceColor.array, index * 3)
        instanceColor.needsUpdate = true
    }
}

// Shader support for instancing
// Vertex shader automatically gets instance matrix via @builtin(instance_index)
```

**Frustum Culling with Instancing**:
```kotlin
class InstancedMeshCuller(val mesh: InstancedMesh) {
    private val boundingSpheres = Array(mesh.count) { Sphere() }
    private val visibilityMask = BooleanArray(mesh.count)

    fun updateBounds() {
        for (i in 0 until mesh.count) {
            val matrix = Matrix4()
            mesh.getMatrixAt(i, matrix)

            val position = Vector3()
            matrix.decompose(position, null, null)

            // Calculate bounding sphere from geometry + instance transform
            boundingSpheres[i].center.copy(position)
            boundingSpheres[i].radius = mesh.geometry.boundingSphere.radius
        }
    }

    fun cull(frustum: Frustum): Int {
        var visibleCount = 0
        for (i in 0 until mesh.count) {
            visibilityMask[i] = frustum.intersectsSphere(boundingSpheres[i])
            if (visibilityMask[i]) visibleCount++
        }
        return visibleCount
    }
}
```

### LOD (Level of Detail) System

```kotlin
class LOD : Object3D() {
    private val levels = mutableListOf<LODLevel>()

    data class LODLevel(
        val object: Object3D,
        val distance: Float
    )

    fun addLevel(object: Object3D, distance: Float) {
        levels.add(LODLevel(object, distance))
        levels.sortBy { it.distance }
        add(object)
    }

    fun update(camera: Camera) {
        val distance = position.distanceTo(camera.position)

        // Hide all levels
        levels.forEach { it.object.visible = false }

        // Show appropriate level
        val activeLevel = levels.lastOrNull { it.distance <= distance } ?: levels.firstOrNull()
        activeLevel?.object?.visible = true
    }
}

// Automatic LOD generation
fun generateLODLevels(geometry: BufferGeometry, levels: Int): List<BufferGeometry> {
    return (0 until levels).map { level ->
        val decimationRatio = 1.0f / (1 shl level)  // 1.0, 0.5, 0.25, 0.125, ...
        decimateGeometry(geometry, decimationRatio)
    }
}

fun decimateGeometry(geometry: BufferGeometry, ratio: Float): BufferGeometry {
    // Quadric error metrics mesh simplification
    // Port of Three.js SimplifyModifier or use platform-specific library
    TODO("Implement mesh decimation")
}
```

### Frustum Culling

```kotlin
class FrustumCuller {
    fun cullScene(scene: Scene, camera: Camera): List<Object3D> {
        val frustum = Frustum().setFromProjectionMatrix(
            camera.projectionMatrix.clone().multiply(camera.matrixWorldInverse)
        )

        val visibleObjects = mutableListOf<Object3D>()

        scene.traverse { obj ->
            if (!obj.visible) return@traverse

            // Update world matrix if needed
            if (obj.matrixWorldNeedsUpdate) {
                obj.updateMatrixWorld()
            }

            // Test bounding sphere against frustum
            obj.geometry?.let { geometry ->
                if (geometry.boundingSphere == null) {
                    geometry.computeBoundingSphere()
                }

                val sphere = geometry.boundingSphere!!.clone()
                sphere.applyMatrix4(obj.matrixWorld)

                if (frustum.intersectsSphere(sphere)) {
                    visibleObjects.add(obj)
                }
            }
        }

        return visibleObjects
    }
}
```

### Geometry Batching

```kotlin
class GeometryBatcher {
    fun batchGeometries(meshes: List<Mesh>): Mesh {
        // Merge multiple geometries into one for reduced draw calls
        val mergedGeometry = BufferGeometry()
        val positionArrays = mutableListOf<FloatArray>()
        val normalArrays = mutableListOf<FloatArray>()
        val uvArrays = mutableListOf<FloatArray>()

        meshes.forEach { mesh ->
            val geometry = mesh.geometry
            val matrix = mesh.matrixWorld

            // Transform vertices to world space
            val positions = geometry.attributes["position"]!!.array
            val transformedPositions = FloatArray(positions.size)
            for (i in positions.indices step 3) {
                val v = Vector3(positions[i], positions[i + 1], positions[i + 2])
                v.applyMatrix4(matrix)
                transformedPositions[i] = v.x
                transformedPositions[i + 1] = v.y
                transformedPositions[i + 2] = v.z
            }
            positionArrays.add(transformedPositions)

            // Transform normals
            val normals = geometry.attributes["normal"]?.array
            if (normals != null) {
                val normalMatrix = Matrix3().getNormalMatrix(matrix)
                val transformedNormals = FloatArray(normals.size)
                for (i in normals.indices step 3) {
                    val n = Vector3(normals[i], normals[i + 1], normals[i + 2])
                    n.applyMatrix3(normalMatrix).normalize()
                    transformedNormals[i] = n.x
                    transformedNormals[i + 1] = n.y
                    transformedNormals[i + 2] = n.z
                }
                normalArrays.add(transformedNormals)
            }

            // Copy UVs
            geometry.attributes["uv"]?.array?.let { uvArrays.add(it) }
        }

        // Concatenate all arrays
        val allPositions = positionArrays.fold(floatArrayOf()) { acc, arr -> acc + arr }
        val allNormals = normalArrays.fold(floatArrayOf()) { acc, arr -> acc + arr }
        val allUVs = uvArrays.fold(floatArrayOf()) { acc, arr -> acc + arr }

        mergedGeometry.setAttribute("position", BufferAttribute(allPositions, 3))
        if (allNormals.isNotEmpty()) {
            mergedGeometry.setAttribute("normal", BufferAttribute(allNormals, 3))
        }
        if (allUVs.isNotEmpty()) {
            mergedGeometry.setAttribute("uv", BufferAttribute(allUVs, 2))
        }

        return Mesh(mergedGeometry, meshes.first().material)
    }
}
```

### Object Pooling

```kotlin
class ObjectPool<T>(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
    initialSize: Int = 10
) {
    private val available = ArrayDeque<T>(initialSize)
    private val inUse = mutableSetOf<T>()

    init {
        repeat(initialSize) {
            available.add(factory())
        }
    }

    fun acquire(): T {
        val obj = if (available.isEmpty()) {
            factory()
        } else {
            available.removeFirst()
        }
        inUse.add(obj)
        return obj
    }

    fun release(obj: T) {
        if (inUse.remove(obj)) {
            reset(obj)
            available.add(obj)
        }
    }

    fun releaseAll() {
        inUse.forEach { obj ->
            reset(obj)
            available.add(obj)
        }
        inUse.clear()
    }
}

// Usage for frequently allocated objects
val vector3Pool = ObjectPool(
    factory = { Vector3() },
    reset = { it.set(0f, 0f, 0f) },
    initialSize = 100
)

val matrix4Pool = ObjectPool(
    factory = { Matrix4() },
    reset = { it.identity() },
    initialSize = 50
)
```

### Adaptive Quality Settings

```kotlin
class AdaptiveQualityManager(private val renderer: Renderer) {
    private var currentQuality = QualityPreset.HIGH
    private var frameTimeHistory = FloatArray(60)
    private var historyIndex = 0

    enum class QualityPreset(
        val targetFPS: Int,
        val maxTriangles: Int,
        val shadowMapSize: Int,
        val enablePostProcessing: Boolean,
        val lodBias: Float
    ) {
        LOW(30, 50_000, 512, false, 2.0f),
        MEDIUM(60, 100_000, 1024, false, 1.0f),
        HIGH(60, 500_000, 2048, true, 0.5f),
        ULTRA(60, 1_000_000, 4096, true, 0.0f)
    }

    fun update(deltaTime: Float) {
        frameTimeHistory[historyIndex] = deltaTime
        historyIndex = (historyIndex + 1) % frameTimeHistory.size

        // Calculate average frame time
        val avgFrameTime = frameTimeHistory.average().toFloat()
        val avgFPS = 1f / avgFrameTime

        // Adjust quality if performance dips
        when {
            avgFPS < currentQuality.targetFPS * 0.8f && currentQuality.ordinal > 0 -> {
                currentQuality = QualityPreset.values()[currentQuality.ordinal - 1]
                applyQualitySettings()
                println("Quality reduced to $currentQuality")
            }
            avgFPS > currentQuality.targetFPS * 1.2f && currentQuality.ordinal < QualityPreset.values().size - 1 -> {
                currentQuality = QualityPreset.values()[currentQuality.ordinal + 1]
                applyQualitySettings()
                println("Quality increased to $currentQuality")
            }
        }
    }

    private fun applyQualitySettings() {
        // Adjust renderer settings
        renderer.shadowMap.size = currentQuality.shadowMapSize
        renderer.postProcessing.enabled = currentQuality.enablePostProcessing

        // Adjust LOD bias
        renderer.lodBias = currentQuality.lodBias
    }
}
```

### Performance Metrics Tracking

```kotlin
class PerformanceMonitor {
    private var frameCount = 0
    private var fps = 0f
    private var lastTime = 0L

    data class FrameMetrics(
        val fps: Float,
        val frameTime: Float,
        val triangleCount: Int,
        val drawCalls: Int,
        val textureMemory: Long,
        val bufferMemory: Long
    )

    fun update(): FrameMetrics {
        frameCount++
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastTime >= 1000) {
            fps = frameCount.toFloat() / ((currentTime - lastTime) / 1000f)
            frameCount = 0
            lastTime = currentTime
        }

        return FrameMetrics(
            fps = fps,
            frameTime = 1000f / fps,
            triangleCount = /* query from renderer */,
            drawCalls = /* query from renderer */,
            textureMemory = /* query from renderer */,
            bufferMemory = /* query from renderer */
        )
    }
}
```

### Performance Targets by Platform

| Platform | Min FPS | Target FPS | Max Triangles | Draw Calls | Memory Budget |
|----------|---------|------------|---------------|------------|---------------|
| Desktop (High-end) | 60 | 144 | 1M+ | 500 | 4 GB VRAM |
| Desktop (Mid-range) | 60 | 60 | 500K | 300 | 2 GB VRAM |
| Web (WebGPU) | 60 | 60 | 300K | 200 | 1 GB |
| Web (WebGL2) | 30 | 60 | 100K | 100 | 512 MB |
| Mobile (High-end) | 60 | 60 | 200K | 150 | 512 MB |
| Mobile (Mid-range) | 30 | 60 | 50K | 50 | 256 MB |

### Alternatives Considered

**Option A: No Optimization**
- Implement features without performance considerations
- ❌ Rejected: Violates 60 FPS constitutional requirement

**Option B: Defer Optimization**
- Optimize after implementation complete
- ❌ Rejected: Performance issues difficult to fix retroactively

**Option C: Built-in Optimization** (Selected)
- Design performance into architecture from start
- ✅ Meets constitutional requirements
- ✅ Adaptive quality maintains UX
- ✅ Instrumentation enables profiling

---

## 7. Testing Strategy

### Decision

Multi-layered testing approach: contract tests for API compliance, visual regression tests for rendering accuracy, performance benchmarks for constitutional validation, cross-platform consistency tests.

### Rationale

- **API Compliance**: Contract tests ensure Three.js compatibility
- **Visual Accuracy**: Pixel-perfect rendering across platforms
- **Performance**: Automated validation of 60 FPS requirement
- **Platform Parity**: Consistent behavior on all targets

### Testing Architecture

**Contract Testing Pattern**:
```kotlin
// commonTest - platform-agnostic API tests
abstract class GeometryContractTest {
    @Test
    fun `BoxGeometry should create correct vertex count`() {
        val geometry = BoxGeometry(1f, 1f, 1f)
        assertEquals(24, geometry.attributes["position"]!!.count)
    }

    @Test
    fun `SphereGeometry should support custom segments`() {
        val geometry = SphereGeometry(
            radius = 1f,
            widthSegments = 16,
            heightSegments = 8
        )
        val expectedVertices = (16 + 1) * (8 + 1)
        assertEquals(expectedVertices, geometry.attributes["position"]!!.count)
    }

    @Test
    fun `BufferGeometry should compute bounding sphere`() {
        val geometry = BoxGeometry(2f, 2f, 2f)
        geometry.computeBoundingSphere()

        assertNotNull(geometry.boundingSphere)
        assertEquals(Vector3(0f, 0f, 0f), geometry.boundingSphere!!.center)
        assertApproximately(sqrt(3f), geometry.boundingSphere!!.radius, 0.01f)
    }
}

// Platform-specific implementations run same tests
class JvmGeometryContractTest : GeometryContractTest()
class JsGeometryContractTest : GeometryContractTest()
class NativeGeometryContractTest : GeometryContractTest()
```

**Visual Regression Testing**:
```kotlin
class VisualRegressionTest {
    @Test
    fun `render basic scene matches reference`() = runTest {
        // Setup scene
        val scene = Scene()
        val camera = PerspectiveCamera(75f, 1.0f, 0.1f, 1000f)
        camera.position.z = 5f

        val geometry = BoxGeometry(1f, 1f, 1f)
        val material = MeshBasicMaterial(color = Color(0xff0000))
        val mesh = Mesh(geometry, material)
        scene.add(mesh)

        // Render
        val renderer = createRenderer(RendererConfiguration(
            width = 800,
            height = 600
        )).getOrThrow()

        renderer.render(scene, camera)
        val screenshot = renderer.screenshot()

        // Compare to reference image
        val reference = loadReferenceImage("basic-scene.png")
        val diff = compareImages(screenshot, reference)

        assertTrue(diff.pixelDifferencePercentage < 0.1f,
            "Visual difference too high: ${diff.pixelDifferencePercentage}%")
    }

    private suspend fun compareImages(img1: ImageData, img2: ImageData): ImageDiff {
        require(img1.width == img2.width && img1.height == img2.height)

        var diffPixels = 0
        val totalPixels = img1.width * img1.height

        for (i in img1.pixels.indices step 4) {
            val r1 = img1.pixels[i].toInt() and 0xFF
            val g1 = img1.pixels[i + 1].toInt() and 0xFF
            val b1 = img1.pixels[i + 2].toInt() and 0xFF

            val r2 = img2.pixels[i].toInt() and 0xFF
            val g2 = img2.pixels[i + 1].toInt() and 0xFF
            val b2 = img2.pixels[i + 2].toInt() and 0xFF

            val diff = abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
            if (diff > 10) diffPixels++  // Threshold for pixel difference
        }

        return ImageDiff(
            pixelDifferencePercentage = (diffPixels.toFloat() / totalPixels) * 100f,
            diffPixelCount = diffPixels
        )
    }
}

data class ImageDiff(
    val pixelDifferencePercentage: Float,
    val diffPixelCount: Int
)
```

**Performance Benchmarking**:
```kotlin
class PerformanceBenchmark {
    @Test
    fun `rendering 100k triangles should maintain 60 FPS`() = runTest {
        val scene = createComplexScene(triangleCount = 100_000)
        val camera = PerspectiveCamera(75f, 16f / 9f, 0.1f, 1000f)
        val renderer = createRenderer(RendererConfiguration(
            width = 1920,
            height = 1080
        )).getOrThrow()

        val monitor = PerformanceMonitor()
        val frameTimes = mutableListOf<Float>()

        // Render 600 frames (10 seconds at 60 FPS)
        repeat(600) {
            val startTime = System.nanoTime()
            renderer.render(scene, camera)
            val endTime = System.nanoTime()

            val frameTime = (endTime - startTime) / 1_000_000f  // Convert to ms
            frameTimes.add(frameTime)
        }

        // Calculate statistics
        val avgFrameTime = frameTimes.average().toFloat()
        val avgFPS = 1000f / avgFrameTime
        val minFPS = 1000f / frameTimes.maxOrNull()!!

        println("Avg FPS: $avgFPS, Min FPS: $minFPS, Avg Frame Time: ${avgFrameTime}ms")

        // Constitutional requirement: 60 FPS
        assertTrue(avgFPS >= 60f, "Average FPS $avgFPS below target 60 FPS")
        assertTrue(minFPS >= 30f, "Minimum FPS $minFPS below acceptable threshold")
    }

    @Test
    fun `library size should be under 5MB`() {
        val jarSize = File("build/libs/kreekt.jar").length()
        val sizeMB = jarSize / (1024f * 1024f)

        println("Library size: $sizeMB MB")
        assertTrue(sizeMB < 5f, "Library size $sizeMB MB exceeds 5MB limit")
    }

    @Test
    fun `memory usage should stay within budget`() = runTest {
        val scene = createComplexScene(triangleCount = 500_000)
        val camera = PerspectiveCamera()
        val renderer = createRenderer(RendererConfiguration()).getOrThrow()

        val runtime = Runtime.getRuntime()
        runtime.gc()  // Force GC before measurement

        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Render 100 frames
        repeat(100) {
            renderer.render(scene, camera)
        }

        runtime.gc()
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncreaseMB = (finalMemory - initialMemory) / (1024f * 1024f)

        println("Memory increase after 100 frames: $memoryIncreaseMB MB")
        assertTrue(memoryIncreaseMB < 100f, "Memory leak detected: $memoryIncreaseMB MB increase")
    }
}
```

**Cross-Platform Consistency Testing**:
```kotlin
class CrossPlatformConsistencyTest {
    @Test
    fun `Matrix4 multiplication should produce identical results across platforms`() {
        val m1 = Matrix4().makeRotationX(PI.toFloat() / 4f)
        val m2 = Matrix4().makeTranslation(1f, 2f, 3f)
        val result = m1.clone().multiply(m2)

        // Test against reference values (computed once, validated everywhere)
        val expected = floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.707f, 0.707f, 0.0f,
            0.0f, -0.707f, 0.707f, 0.0f,
            1.0f, 2.0f, 3.0f, 1.0f
        )

        for (i in expected.indices) {
            assertApproximately(expected[i], result.elements[i], 0.001f,
                "Matrix element $i differs across platforms")
        }
    }

    @Test
    fun `Raycaster should produce identical intersections across platforms`() {
        val scene = Scene()
        val geometry = BoxGeometry(1f, 1f, 1f)
        val mesh = Mesh(geometry, MeshBasicMaterial())
        scene.add(mesh)

        val raycaster = Raycaster(
            origin = Vector3(0f, 0f, 5f),
            direction = Vector3(0f, 0f, -1f)
        )

        val intersections = raycaster.intersectObjects(listOf(mesh))

        assertEquals(1, intersections.size, "Should find exactly one intersection")
        assertApproximately(4.5f, intersections[0].distance, 0.001f)
        assertApproximately(Vector3(0f, 0f, 0.5f), intersections[0].point, 0.001f)
    }
}
```

### Test Coverage Requirements

| Category | Target Coverage | Validation Method |
|----------|----------------|-------------------|
| Unit Tests | >80% | JaCoCo/Kover |
| Integration Tests | >60% | Manual review |
| Visual Regression | 100% core features | Automated screenshots |
| Performance | 100% benchmarks pass | CI/CD gates |
| Cross-Platform | 100% contract tests | Multi-platform CI |

### CI/CD Integration

```yaml
# GitHub Actions workflow
name: KreeKt CI/CD

on: [push, pull_request]

jobs:
  test-jvm:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run JVM tests
        run: ./gradlew jvmTest
      - name: Coverage report
        run: ./gradlew koverJvmReport
      - name: Upload coverage
        uses: codecov/codecov-action@v3

  test-js:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Run JS tests
        run: ./gradlew jsTest

  test-native:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v3
      - name: Run native tests
        run: ./gradlew nativeTest

  performance-benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run performance benchmarks
        run: ./gradlew performanceTest
      - name: Validate 60 FPS requirement
        run: ./gradlew validatePerformance

  visual-regression:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run visual tests
        run: ./gradlew visualRegressionTest
      - name: Upload screenshots
        uses: actions/upload-artifact@v3
        if: failure()
        with:
          name: visual-diff
          path: build/visual-regression/
```

### Alternatives Considered

**Option A: Manual Testing Only**
- Rely on human testers for validation
- ❌ Rejected: Not scalable, error-prone, slow feedback

**Option B: Unit Tests Only**
- Focus solely on isolated unit tests
- ❌ Rejected: Doesn't validate integration, rendering, or performance

**Option C: Comprehensive Multi-Layer Testing** (Selected)
- Contract, visual, performance, cross-platform tests
- ✅ Ensures API compliance and rendering accuracy
- ✅ Validates constitutional requirements
- ✅ Catches regressions early

---

## Summary of Technical Decisions

| Area | Decision | Key Rationale |
|------|----------|---------------|
| **API Design** | 1:1 Three.js compatibility with Kotlin idioms | Migration path, documentation leverage, proven design |
| **Graphics Abstraction** | Unified Renderer with platform-specific backends | Platform independence, optimal performance, future-proof |
| **Shader Pipeline** | WGSL source → SPIR-V Cross translation | Cross-platform shader support, modern GPU features |
| **Multiplatform** | expect/actual for graphics primitives, common business logic | 70-80% code reuse, type-safe platform implementations |
| **Asset Loading** | Okio + platform decoders + coroutines | Cross-platform IO, native performance, async patterns |
| **Animation** | Three.js AnimationMixer port | Proven design, feature complete, familiar API |
| **Performance** | Instancing, LOD, culling, batching, adaptive quality | 60 FPS constitutional requirement, scalable to platforms |
| **Testing** | Multi-layer: contract, visual, performance, cross-platform | API compliance, rendering accuracy, constitutional validation |

---

## Implementation Roadmap

### Phase 1: Core API Completion (Weeks 1-4)
1. Complete geometry system (all Three.js geometry types)
2. Complete material system (PBR materials, shader materials)
3. Complete texture system (all texture types, compression)
4. Implement asset loaders (GLTF primary, OBJ/FBX secondary)

### Phase 2: Advanced Features (Weeks 5-8)
1. Complete animation system (AnimationMixer, morph targets, skinning)
2. Advanced lighting (IBL, area lights, shadow mapping)
3. Post-processing pipeline (EffectComposer, standard effects)
4. Helper/debugging tools (complete visual helpers)

### Phase 3: Platform-Specific Features (Weeks 9-12)
1. XR support (WebXR, ARKit, ARCore)
2. Audio system (3D positional audio)
3. Camera controls (Orbit, FirstPerson, Transform)
4. Physics integration helpers

### Phase 4: Optimization & Polish (Weeks 13-16)
1. Performance optimization (GPU-driven culling, compute shaders)
2. Memory optimization (streaming, compression)
3. Platform-specific tuning
4. Documentation and examples

---

## Risk Assessment & Mitigation

### High-Risk Areas

1. **Shader Translation Complexity**
   - Risk: WGSL → SPIR-V/MSL translation issues
   - Mitigation: Extensive testing, fallback shaders, incremental rollout

2. **Platform Performance Variance**
   - Risk: 60 FPS achievable on desktop but not mobile
   - Mitigation: Adaptive quality, platform-specific budgets, profiling

3. **Asset Loading Reliability**
   - Risk: GLTF/FBX parsing bugs cause crashes
   - Mitigation: Fuzzing, extensive test assets, error handling

4. **Cross-Platform Consistency**
   - Risk: Subtle rendering differences between platforms
   - Mitigation: Visual regression tests, reference renders, strict tolerances

### Medium-Risk Areas

1. **Animation System Complexity**: Many moving parts (mixer, blending, skinning)
2. **Memory Management**: Potential leaks in long-running applications
3. **API Surface Size**: Large API increases maintenance burden

---

## References

### Three.js Documentation
- Three.js r180 Documentation: https://threejs.org/docs/
- Three.js Examples: https://threejs.org/examples/
- Three.js GitHub: https://github.com/mrdoob/three.js/tree/r180

### Graphics API Documentation
- WebGPU Specification: https://www.w3.org/TR/webgpu/
- Vulkan 1.3 Specification: https://registry.khronos.org/vulkan/
- Metal Programming Guide: https://developer.apple.com/metal/
- WebGL 2.0 Specification: https://www.khronos.org/registry/webgl/specs/latest/2.0/

### Technical References
- Real-Time Rendering 4th Edition (Akenine-Möller et al.)
- Game Engine Architecture 3rd Edition (Jason Gregory)
- Physically Based Rendering (Pharr, Jakob, Humphreys)
- GPU Gems series (NVIDIA)

### Libraries & Tools
- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html
- LWJGL 3: https://www.lwjgl.org/
- Okio: https://square.github.io/okio/
- kotlinx.serialization: https://github.com/Kotlin/kotlinx.serialization
- kotlinx.coroutines: https://github.com/Kotlin/kotlinx.coroutines

---

**Research Status**: COMPLETE
**Ready for Phase 1**: ✅ YES
**Next Steps**: Generate data-model.md, contracts/, quickstart.md
