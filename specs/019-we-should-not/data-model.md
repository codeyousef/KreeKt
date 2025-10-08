# Data Model: WebGPU/Vulkan Primary Renderer

**Feature**: 019-we-should-not
**Date**: 2025-10-07

## Core Entities

### 1. Renderer (Interface)

**Purpose**: Main graphics system interface abstracting WebGPU/Vulkan/WebGL differences

**Common Interface** (commonMain):

```kotlin
expect interface Renderer {
    val backend: BackendType
    val capabilities: RendererCapabilities
    val stats: RenderStats

    suspend fun initialize(config: RendererConfig): Result<Unit, RendererError>
    fun render(scene: Scene, camera: Camera)
    fun resize(width: Int, height: Int)
    fun dispose()
    fun getStats(): RenderStats
}
```

**Fields**:

- `backend: BackendType` - Current backend (WEBGPU, VULKAN, WEBGL)
- `capabilities: RendererCapabilities` - Backend feature detection
- `stats: RenderStats` - Performance metrics (FPS, draw calls, triangles)

**Validation Rules**:

- `initialize()` must be called before `render()`
- `render()` must be called from main thread
- `dispose()` must be called to free GPU resources

**State Transitions**:

```
UNINITIALIZED -> initialize() -> READY
READY -> render() -> READY (loop)
READY -> dispose() -> DISPOSED
UNINITIALIZED -> initialize() [fail] -> ERROR
```

**Relationships**:

- Renders Scene entity
- Uses Camera entity for view/projection
- Created by RendererFactory

---

### 2. BackendType (Enum)

**Purpose**: Identifies which graphics API is active

**Definition**:

```kotlin
enum class BackendType {
    WEBGPU,   // Primary for JS (browser WebGPU)
    VULKAN,   // Primary for JVM (desktop/mobile)
    WEBGL;    // Fallback only (browser compatibility)

    val isPrimary: Boolean get() = this == WEBGPU || this == VULKAN
    val isFallback: Boolean get() = this == WEBGL
}
```

**Usage**:

- Logged at initialization (FR-023)
- Used for performance threshold selection
- Reported in error messages

---

### 3. RendererCapabilities

**Purpose**: Reports available features for current backend

**Definition**:

```kotlin
data class RendererCapabilities(
    val backend: BackendType,
    val maxTextureSize: Int,
    val maxVertexAttributes: Int,
    val supportsCompute: Boolean,
    val supportsRayTracing: Boolean,
    val supportsMultisampling: Boolean,
    val deviceName: String,
    val driverVersion: String
)
```

**Validation Rules**:

- `maxTextureSize` must be ≥2048 (FR-024: detect mismatches)
- `maxVertexAttributes` must be ≥16 (minimum for complex meshes)

**Example Values**:

- WebGPU: `maxTextureSize=8192, supportsCompute=true`
- Vulkan: `maxTextureSize=16384, supportsRayTracing=true` (hardware-dependent)
- WebGL: `maxTextureSize=4096, supportsCompute=false`

---

### 4. RendererConfig

**Purpose**: Configuration for renderer initialization

**Definition**:

```kotlin
data class RendererConfig(
    val preferredBackend: BackendType? = null,  // null = auto-detect
    val enableValidation: Boolean = true,        // Debug/validation layers
    val vsync: Boolean = true,                   // V-sync for frame pacing
    val msaaSamples: Int = 4,                    // Multisampling anti-aliasing
    val powerPreference: PowerPreference = PowerPreference.HIGH_PERFORMANCE
)

enum class PowerPreference {
    LOW_POWER,           // Integrated GPU
    HIGH_PERFORMANCE     // Discrete GPU (prefer for 60 FPS target)
}
```

**Validation Rules**:

- `msaaSamples` must be power of 2 (1, 2, 4, 8, 16)
- If `preferredBackend` is WEBGL, log warning (violates FR-001/FR-002)

---

### 5. RenderStats

**Purpose**: Performance metrics for monitoring and validation

**Definition**:

```kotlin
data class RenderStats(
    val fps: Double,
    val frameTime: Double,           // milliseconds
    val triangles: Int,
    val drawCalls: Int,
    val textureMemory: Long,         // bytes
    val bufferMemory: Long,          // bytes
    val timestamp: Long = currentTimeMillis()
)
```

**Usage**:

- Updated every frame in `render()` call
- Consumed by PerformanceValidator (FR-019 validation)
- Displayed in debug UI (VoxelCraft HUD)

**Validation Rules**:

- `fps` should be ≥60 (target) or ≥30 (minimum) per FR-019
- `frameTime` should be ≤16.67ms (60 FPS) or ≤33.33ms (30 FPS)

---

### 6. RendererFactory

**Purpose**: Creates appropriate Renderer for current platform

**Common Interface**:

```kotlin
expect object RendererFactory {
    suspend fun create(
        surface: RenderSurface,
        config: RendererConfig = RendererConfig()
    ): Result<Renderer, RendererInitializationException>

    fun detectAvailableBackends(): List<BackendType>
}
```

**Actual Implementations**:

**JS (jsMain)**:

```kotlin
actual object RendererFactory {
    actual suspend fun create(...): Result<Renderer, ...> {
        // 1. Check navigator.gpu (WebGPU available?)
        if (hasWebGPU && config.preferredBackend != BackendType.WEBGL) {
            try {
                return WebGPURenderer(surface, config).initialize()
            } catch (e: Exception) {
                Logger.warn("WebGPU init failed: ${e.message}, falling back to WebGL")
            }
        }

        // 2. Fallback to WebGL2
        return WebGLRenderer(surface, config).initialize()
    }

    actual fun detectAvailableBackends(): List<BackendType> {
        val backends = mutableListOf<BackendType>()
        if (js("'gpu' in navigator").unsafeCast<Boolean>()) {
            backends.add(BackendType.WEBGPU)
        }
        if (hasWebGL2Support()) {
            backends.add(BackendType.WEBGL)
        }
        return backends
    }
}
```

**JVM (jvmMain)**:

```kotlin
actual object RendererFactory {
    actual suspend fun create(...): Result<Renderer, ...> {
        // Always use Vulkan on JVM (FR-002)
        return VulkanRenderer(surface, config).initialize()
    }

    actual fun detectAvailableBackends(): List<BackendType> {
        // Check Vulkan instance creation
        return if (VulkanLoader.isVulkanAvailable()) {
            listOf(BackendType.VULKAN)
        } else {
            emptyList() // Will throw in create()
        }
    }
}
```

**Validation Rules**:

- If no backends available, throw `NoGraphicsSupportException` (FR-022: fail-fast)
- Log selected backend (FR-023)

**State Transitions**:

```
detectAvailableBackends() -> [WEBGPU, WEBGL] or [VULKAN] or []
create() -> Try primary backend
  Success -> Return Renderer(WEBGPU|VULKAN)
  Failure (JS only) -> Try fallback (WEBGL)
  Failure (all) -> Throw RendererInitializationException
```

---

### 7. RenderSurface (Interface)

**Purpose**: Platform-specific surface for rendering (canvas, window)

**Common Interface**:

```kotlin
expect interface RenderSurface {
    val width: Int
    val height: Int
    fun getHandle(): Any  // Platform-specific handle
}
```

**Actual Implementations**:

**JS**:

```kotlin
actual class WebGPURenderSurface(val canvas: HTMLCanvasElement) : RenderSurface {
    override val width: Int get() = canvas.width
    override val height: Int get() = canvas.height
    override fun getHandle(): Any = canvas.getContext("webgpu")
}
```

**JVM**:

```kotlin
actual class VulkanRenderSurface(val windowHandle: Long) : RenderSurface {
    override val width: Int get() = glfwGetWindowWidth(windowHandle)
    override val height: Int get() = glfwGetWindowHeight(windowHandle)
    override fun getHandle(): Any = VkSurfaceKHR created from window
}
```

**Validation Rules**:

- `width` and `height` must be >0
- `getHandle()` must return valid platform handle or throw

---

### 8. RendererInitializationException (Sealed Class)

**Purpose**: Typed exceptions for fail-fast error handling (FR-022/FR-024)

**Hierarchy**:

```kotlin
sealed class RendererInitializationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class NoGraphicsSupportException(
        val platform: String,
        val availableBackends: List<BackendType>,
        val requiredFeatures: List<String>
    ) : RendererInitializationException(
        """
        No supported graphics API found on $platform.
        Available backends: $availableBackends
        Required features: $requiredFeatures

        Troubleshooting:
        - Update graphics drivers to latest version
        - Ensure GPU supports Vulkan 1.1+ (desktop) or WebGPU (browser)
        - Check browser compatibility: Chrome 113+, Edge 113+
        """.trimIndent()
    )

    class AdapterRequestFailedException(
        val backend: BackendType,
        val reason: String
    ) : RendererInitializationException(
        "Failed to request $backend adapter: $reason"
    )

    class DeviceCreationFailedException(
        val backend: BackendType,
        val adapterInfo: String,
        val reason: String
    ) : RendererInitializationException(
        "Failed to create $backend device.\nAdapter: $adapterInfo\nReason: $reason"
    )

    class SurfaceCreationFailedException(
        val backend: BackendType,
        val surfaceType: String
    ) : RendererInitializationException(
        "Failed to create $backend surface for $surfaceType"
    )

    class ShaderCompilationException(
        val shaderName: String,
        val errors: List<String>
    ) : RendererInitializationException(
        "Shader compilation failed: $shaderName\nErrors:\n${errors.joinToString("\n")}"
    )
}
```

**Usage**:

- Thrown by `RendererFactory.create()` and `Renderer.initialize()`
- Caught in application entry point (Main.kt/MainJvm.kt)
- Logged with full stack trace before crashing

**Example**:

```kotlin
try {
    val renderer = RendererFactory.create(surface).getOrThrow()
} catch (e: NoGraphicsSupportException) {
    Logger.error("Graphics initialization failed", e)
    showErrorDialog(e.message ?: "Unknown error")
    exitProcess(1)  // Fail-fast per FR-022
}
```

---

## Entity Relationships

```
RendererFactory
    ↓ creates
Renderer (WebGPURenderer | VulkanRenderer | WebGLRenderer)
    ↓ uses
RenderSurface (WebGPURenderSurface | VulkanRenderSurface)
    ↓ has
RendererCapabilities
    ↓ reports
BackendType

Renderer
    ↓ renders
Scene
    ↓ views through
Camera

Renderer
    ↓ produces
RenderStats
    ↓ validates
PerformanceValidator (FR-019)

RendererFactory.create()
    ↓ throws on failure
RendererInitializationException
    ↓ subtypes
[NoGraphicsSupport, AdapterRequestFailed, DeviceCreationFailed, SurfaceCreationFailed, ShaderCompilationFailed]
```

---

## Data Flow

### Initialization Flow

```
1. Application creates RenderSurface (canvas/window)
2. RendererFactory.detectAvailableBackends() -> [WEBGPU, WEBGL] or [VULKAN]
3. RendererFactory.create(surface, config)
   a. JS: Try WebGPU -> Fallback to WebGL
   b. JVM: Use Vulkan (no fallback)
4. Renderer.initialize()
   - Create device/adapter
   - Compile shaders
   - Create default pipeline
5. Return Renderer instance or throw RendererInitializationException
```

### Render Loop Flow

```
1. Game loop calls Renderer.render(scene, camera)
2. Renderer updates RenderStats (FPS, draw calls, triangles)
3. Renderer executes platform-specific rendering:
   - WebGPU: GPUCommandEncoder -> submit()
   - Vulkan: vkCmdDraw -> vkQueueSubmit()
   - WebGL: gl.drawElements()
4. PerformanceValidator.validate(stats) [every 60 frames]
5. Log validation result (PASS/FAIL) per FR-019
```

---

## Validation and Constraints

### Per-Entity Constraints

| Entity               | Constraint                      | Enforcement                             |
|----------------------|---------------------------------|-----------------------------------------|
| Renderer             | Must initialize before render   | Runtime check (IllegalStateException)   |
| RendererConfig       | msaaSamples must be power of 2  | Constructor validation                  |
| RendererCapabilities | maxTextureSize ≥2048            | Checked in initialize(), throw if fails |
| RenderStats          | fps ≥30 for sustained operation | PerformanceValidator (FR-019)           |
| BackendType          | WebGL only as fallback          | RendererFactory enforces                |

### Cross-Entity Constraints

- Renderer backend must match RenderSurface platform (compile-time via expect/actual)
- RenderStats.fps must meet FR-019 thresholds (60 FPS target, 30 FPS minimum)
- RendererFactory must prefer primary backends (WEBGPU/VULKAN) per FR-001/FR-002
- All initialization failures must throw RendererInitializationException (FR-022)

---

## Implementation Notes

### Code Sharing Achievement

| Entity                          | Common Code     | Platform-Specific      | Sharing % |
|---------------------------------|-----------------|------------------------|-----------|
| Renderer (interface)            | 100% (expect)   | Actual implementations | 20%       |
| BackendType                     | 100%            | 0%                     | 100%      |
| RendererCapabilities            | 100%            | 0%                     | 100%      |
| RendererConfig                  | 100%            | 0%                     | 100%      |
| RenderStats                     | 100%            | 0%                     | 100%      |
| RendererFactory                 | 30% (interface) | 70% (detection logic)  | 30%       |
| RenderSurface                   | 100% (expect)   | Actual implementations | 20%       |
| RendererInitializationException | 100%            | 0%                     | 100%      |

**Estimated Total**: ~75% common renderer code, ~95% application code (VoxelCraft) - **Meets FR-006 (≥90% application
code sharing)**

### Performance Considerations

- `RenderStats` updated every frame → must be lightweight (<0.1ms overhead)
- `RendererCapabilities` queried once at init → caching OK
- Backend detection should complete in <50ms (FR-019: 2000ms init budget)

---

**Data Model Complete**: All entities, relationships, and validation rules documented. Ready for contract generation.
