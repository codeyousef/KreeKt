# Tasks: WebGPU/Vulkan Primary Renderer with Multiplatform Code Sharing

**Feature**: 019-we-should-not
**Branch**: `019-we-should-not`
**Input**: Design documents from `/mnt/d/Projects/KMP/KreeKt/specs/019-we-should-not/`
**Prerequisites**: plan.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

## Execution Flow (main)

```
1. Load plan.md from feature directory ✅
   → Tech stack: Kotlin 1.9+ Multiplatform, LWJGL 3.3.5, @webgpu/types 0.1.40
   → Structure: commonMain, jvmMain, jsMain source sets
2. Load design documents ✅
   → data-model.md: 8 entities (Renderer, BackendType, RendererCapabilities, etc.)
   → contracts/: renderer-contract.kt (19 test methods)
   → research.md: 7 technical decisions
   → quickstart.md: 7 validation scenarios
3. Generate tasks by category ✅
   → 37 tasks across 9 phases
   → TDD approach: Tests → Common APIs → Platform implementations → Integration
4. Apply task rules ✅
   → Different files = [P] for parallel
   → Same file/dependent = sequential
   → Tests before implementation
5. Number tasks T001-T037
6. Dependency graph generated below
7. Parallel execution examples included
8. Validation complete ✅
   → All contracts have tests ✅
   → All entities have model tasks ✅
   → Tests before implementation ✅
9. Return: SUCCESS (tasks ready for execution)
```

## Path Conventions

This is a Kotlin Multiplatform library following the structure:

- **Common code**: `kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/`
- **JVM code**: `kreekt-renderer/src/jvmMain/kotlin/io/kreekt/renderer/`
- **JS code**: `kreekt-renderer/src/jsMain/kotlin/io/kreekt/renderer/`
- **Common tests**: `kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/`
- **Example**: `examples/voxelcraft/src/`

---

## Phase 3.1: Setup & Dependencies

### T001 ✅ Add LWJGL Vulkan dependencies to build.gradle.kts

**File**: `kreekt-renderer/build.gradle.kts`
**Description**: Add LWJGL 3.3.5 Vulkan module and platform-specific natives
**Details**:

```kotlin
jvm {
    dependencies {
        implementation("org.lwjgl:lwjgl-vulkan:3.3.5")
        val lwjglNatives = when (osName) {
            "windows" -> "natives-windows"
            "linux" -> "natives-linux"
            "macos" -> "natives-macos"
        }
        runtimeOnly("org.lwjgl:lwjgl:3.3.5:$lwjglNatives")
        runtimeOnly("org.lwjgl:lwjgl-vulkan:3.3.5:$lwjglNatives")
    }
}
```

**Success Criteria**: Build succeeds with Vulkan dependencies available

---

### T002 ✅ [P] Update @webgpu/types to 0.1.40 in package.json

**File**: `package.json` (if exists) or document in `build.gradle.kts`
**Description**: Ensure WebGPU type definitions are up to date
**Details**: Add or update WebGPU types for TypeScript/Kotlin JS interop
**Success Criteria**: WebGPU APIs available in jsMain source set

---

### T003 ✅ [P] Configure Tint WASM for shader compilation

**File**: `buildSrc/` or `build.gradle.kts` custom task
**Description**: Set up Tint WASM compiler for WGSL → SPIR-V at build time
**Details**:

- Download Tint WASM binary
- Create Gradle task `compileShaders`
- Input: `kreekt-renderer/src/commonMain/resources/shaders/*.wgsl`
- Output: `kreekt-renderer/src/jvmMain/resources/shaders/*.spv`
  **Success Criteria**: WGSL shaders compile to SPIR-V during build

---

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3

**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### T004 ✅ [P] Contract test: Backend detection (FR-004)

**File**: `kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/BackendDetectionTest.kt`
**From**: `contracts/renderer-contract.kt` lines 42-60
**Description**: Test `RendererFactory.detectAvailableBackends()` returns correct backends for platform
**Test Cases**:

- JS platform: Must have at least WebGL fallback
- JVM platform: Must have Vulkan
- List contains valid BackendType values
  **Success Criteria**: Test compiles, fails with "RendererFactory not implemented"

---

### T005 ✅ [P] Contract test: Primary backend selection (FR-001, FR-002)

**File**: `kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/PrimaryBackendTest.kt`
**From**: `contracts/renderer-contract.kt` lines 62-82
**Description**: Test `RendererFactory.create()` selects primary backend by default
**Test Cases**:

- JS: WebGPU if available, else WebGL
- JVM: Vulkan (not OpenGL)
- Log backend selection (FR-023)
  **Success Criteria**: Test compiles, fails with "RendererFactory.create() not implemented"

---

### T006 ✅ [P] Contract test: Renderer lifecycle (initialize, render, dispose)

**File**: `kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/RendererLifecycleTest.kt`
**From**: `contracts/renderer-contract.kt` lines 84-117
**Description**: Test Renderer interface methods (initialize, render, resize, dispose)
**Test Cases**:

- `initialize()` succeeds with valid config
- `render()` accepts Scene and Camera
- `dispose()` cleans up resources
- Operations after dispose fail
  **Success Criteria**: Test compiles, fails with "Renderer interface not defined"

---

### T007 ✅ [P] Contract test: Performance validation (FR-019)

**File**: `kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/PerformanceValidationTest.kt`
**From**: `contracts/renderer-contract.kt` lines 136-160
**Description**: Test renderer maintains 30 FPS minimum, targets 60 FPS
**Test Cases**:

- Warmup 120 frames
- Measure 60 frames
- Average FPS ≥30 (minimum per FR-019)
- Log performance metrics
  **Success Criteria**: Test compiles, fails with "Renderer.render() not implemented"

---

### T008 ✅ [P] Contract test: Error handling (FR-022, FR-024)

**File**: `kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/ErrorHandlingTest.kt`
**From**: `contracts/renderer-contract.kt` lines 97-112
**Description**: Test fail-fast behavior with descriptive exceptions
**Test Cases**:

- Invalid surface → throws `RendererInitializationException`
- Exception message contains diagnostic keywords
- Exception message length >50 characters
- Includes platform, backend, troubleshooting info
  **Success Criteria**: Test compiles, fails with "RendererInitializationException not defined"

---

### T009 ✅ [P] Contract test: Visual consistency (FR-020)

**File**: `kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/VisualConsistencyTest.kt`
**From**: `contracts/renderer-contract.kt` lines 183-201
**Description**: Test identical scenes produce consistent output
**Test Cases**:

- Render same scene twice
- Triangle count identical
- Draw call count identical
  **Success Criteria**: Test compiles, fails with "Renderer.getStats() not implemented"

---

### T010 ✅ [P] Contract test: Capability detection (FR-024)

**File**: `kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/CapabilityDetectionTest.kt`
**From**: `contracts/renderer-contract.kt` lines 217-234
**Description**: Test renderer detects and validates backend capabilities
**Test Cases**:

- `maxTextureSize` ≥2048
- `maxVertexAttributes` ≥16
- `deviceName` and `driverVersion` reported
  **Success Criteria**: Test compiles, fails with "RendererCapabilities not defined"

---

## Phase 3.3: Common API Implementation (TDD Green Phase)

**ONLY after Phase 3.2 tests are written and failing**

### T011 [P] Create BackendType enum in commonMain

**File**: `kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/BackendType.kt`
**From**: data-model.md "BackendType (Enum)"
**Description**: Define enum with WEBGPU, VULKAN, WEBGL values
**Details**:

```kotlin
enum class BackendType {
    WEBGPU,   // Primary for JS
    VULKAN,   // Primary for JVM
    WEBGL;    // Fallback only

    val isPrimary: Boolean get() = this == WEBGPU || this == VULKAN
    val isFallback: Boolean get() = this == WEBGL
}
```

**Success Criteria**: Enum compiles, used in tests

---

### T012 [P] Create RendererCapabilities data class in commonMain

**File**: `kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/RendererCapabilities.kt`
**From**: data-model.md "RendererCapabilities"
**Description**: Define capabilities data class
**Details**:

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

**Success Criteria**: Data class compiles, T010 test can instantiate it

---

### T013 [P] Create RendererConfig and RenderStats data classes in commonMain

**File**: `kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/RendererConfig.kt`
**File**: `kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/RenderStats.kt`
**From**: data-model.md "RendererConfig" and "RenderStats"
**Description**: Define configuration and stats data classes
**Details**: Include all fields from data-model.md (config, stats, power preference)
**Success Criteria**: Both classes compile, used in Renderer interface

---

### T014 [P] Define expect Renderer interface in commonMain

**File**: `kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/Renderer.kt`
**From**: data-model.md "Renderer (Interface)"
**Description**: Define expect interface with all methods
**Details**:

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

**Dependencies**: T011 (BackendType), T012 (RendererCapabilities), T013 (RenderStats)
**Success Criteria**: Interface compiles, T006 test uses it

---

### T015 [P] Define expect RenderSurface interface in commonMain

**File**: `kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/RenderSurface.kt`
**From**: data-model.md "RenderSurface (Interface)"
**Description**: Define expect interface for platform-specific surfaces
**Details**:

```kotlin
expect interface RenderSurface {
    val width: Int
    val height: Int
    fun getHandle(): Any
}
```

**Success Criteria**: Interface compiles, used in RendererFactory

---

### T016 [P] Create RendererInitializationException hierarchy in commonMain

**File**: `kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/RendererInitializationException.kt`
**From**: data-model.md "RendererInitializationException (Sealed Class)"
**Description**: Define sealed class hierarchy with all subtypes
**Details**: Include NoGraphicsSupportException, AdapterRequestFailedException, DeviceCreationFailedException,
SurfaceCreationFailedException, ShaderCompilationException (all from data-model.md)
**Success Criteria**: All exception types compile, T008 test can catch them

---

### T017 Define expect RendererFactory object in commonMain

**File**: `kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/RendererFactory.kt`
**From**: data-model.md "RendererFactory"
**Description**: Define expect object with create() and detectAvailableBackends()
**Details**:

```kotlin
expect object RendererFactory {
    suspend fun create(
        surface: RenderSurface,
        config: RendererConfig = RendererConfig()
    ): Result<Renderer, RendererInitializationException>

    fun detectAvailableBackends(): List<BackendType>
}
```

**Dependencies**: T011 (BackendType), T013 (RendererConfig), T014 (Renderer), T015 (RenderSurface), T016 (Exception)
**Success Criteria**: Object compiles, T004 and T005 tests can call methods (will still fail - no actual implementation)

---

## Phase 3.4: Platform Implementation (JVM/Vulkan)

**Sequential tasks within JVM, parallel with Phase 3.5 (JS)**

### T018 Create VulkanRenderer actual implementation in jvmMain

**File**: `kreekt-renderer/src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanRenderer.kt`
**From**: data-model.md "Renderer" actual implementation
**Description**: Implement Renderer interface using LWJGL Vulkan bindings
**Details**:

- Initialize VkInstance with validation layers
- Select physical device (prefer discrete GPU)
- Create logical device with graphics queue
- Implement render() with vkCmdDraw
- Track RenderStats (FPS, draw calls, triangles)
- Implement dispose() to clean up Vulkan resources
  **Dependencies**: T001 (LWJGL Vulkan), T014 (Renderer interface)
  **Success Criteria**: VulkanRenderer compiles, implements all Renderer methods

---

### T019 Implement VulkanSurface with GLFW integration in jvmMain

**File**: `kreekt-renderer/src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanSurface.kt`
**From**: data-model.md "RenderSurface" JVM actual
**Description**: Implement RenderSurface for GLFW window handles
**Details**:

- Wrap GLFW window handle (Long)
- Create VkSurfaceKHR from window
- Return width/height from glfwGetWindowSize
  **Dependencies**: T015 (RenderSurface interface), T018 (VulkanRenderer)
  **Success Criteria**: VulkanSurface compiles, integrates with VulkanRenderer

---

### T020 Implement VulkanPipeline for shader management in jvmMain

**File**: `kreekt-renderer/src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanPipeline.kt`
**Description**: Load SPIR-V shaders and create Vulkan graphics pipeline
**Details**:

- Load compiled SPIR-V from `src/jvmMain/resources/shaders/*.spv`
- Create VkShaderModule for vertex and fragment shaders
- Create VkPipelineLayout with descriptor sets
- Create VkGraphicsPipeline with blending, depth test, etc.
  **Dependencies**: T003 (Tint compiler), T018 (VulkanRenderer)
  **Success Criteria**: VulkanPipeline compiles, shaders load successfully

---

### T021 Implement RendererFactory.create() for JVM in jvmMain

**File**: `kreekt-renderer/src/jvmMain/kotlin/io/kreekt/renderer/RendererFactory.kt`
**From**: data-model.md "RendererFactory" JVM actual
**Description**: Implement actual RendererFactory for JVM (Vulkan only)
**Details**:

```kotlin
actual object RendererFactory {
    actual suspend fun create(...): Result<Renderer, ...> {
        // Always use Vulkan on JVM (FR-002)
        return try {
            val renderer = VulkanRenderer(surface, config)
            renderer.initialize()
            Logger.info("Selected backend: Vulkan")
            Result.success(renderer)
        } catch (e: Exception) {
            Result.failure(NoGraphicsSupportException("JVM", emptyList(), listOf("Vulkan 1.1+")))
        }
    }

    actual fun detectAvailableBackends(): List<BackendType> {
        return if (VulkanLoader.isVulkanAvailable()) {
            listOf(BackendType.VULKAN)
        } else {
            emptyList()
        }
    }
}
```

**Dependencies**: T017 (RendererFactory interface), T018 (VulkanRenderer), T019 (VulkanSurface)
**Success Criteria**: JVM contract tests (T004, T005, T006) pass

---

## Phase 3.5: Platform Implementation (JS/WebGPU)

**Sequential tasks within JS, parallel with Phase 3.4 (JVM)**

### T022 Create WebGPURenderer actual implementation in jsMain

**File**: `kreekt-renderer/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt`
**From**: data-model.md "Renderer" JS actual
**Description**: Implement Renderer interface using WebGPU APIs
**Details**:

- Request GPUAdapter via `navigator.gpu.requestAdapter()`
- Request GPUDevice from adapter
- Create GPUCommandEncoder for rendering
- Implement render() with device.queue.submit()
- Track RenderStats (FPS, draw calls, triangles)
- Implement dispose() to destroy GPU resources
  **Dependencies**: T002 (@webgpu/types), T014 (Renderer interface)
  **Success Criteria**: WebGPURenderer compiles, implements all Renderer methods

---

### T023 Create WebGPUSurface with HTMLCanvasElement in jsMain

**File**: `kreekt-renderer/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUSurface.kt`
**From**: data-model.md "RenderSurface" JS actual
**Description**: Implement RenderSurface for HTML5 canvas
**Details**:

- Wrap HTMLCanvasElement
- Get GPUCanvasContext via `canvas.getContext("webgpu")`
- Return canvas.width, canvas.height
  **Dependencies**: T015 (RenderSurface interface), T022 (WebGPURenderer)
  **Success Criteria**: WebGPUSurface compiles, integrates with WebGPURenderer

---

### T024 Implement WebGPUPipeline for shader management in jsMain

**File**: `kreekt-renderer/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUPipeline.kt`
**Description**: Load WGSL shaders and create WebGPU render pipeline
**Details**:

- Load WGSL from `src/commonMain/resources/shaders/*.wgsl`
- Create GPUShaderModule with WGSL source
- Create GPURenderPipeline with vertex/fragment stages
- Configure bind group layout (fix for existing bug from issue 018)
  **Dependencies**: T022 (WebGPURenderer)
  **Success Criteria**: WebGPUPipeline compiles, shaders compile successfully

---

### T025 Refactor WebGLRenderer as fallback in jsMain

**File**: `kreekt-renderer/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`
**Description**: Move existing WebGLRenderer to fallback-only role
**Details**:

- Keep existing WebGLRenderer implementation
- Ensure it implements updated Renderer interface from T014
- Mark as "fallback only" in logging
- No custom instantiation in examples (enforce FR-007)
  **Dependencies**: T014 (Renderer interface)
  **Success Criteria**: WebGLRenderer compiles, can be used as fallback

---

### T026 Implement RendererFactory.create() with WebGPU detection in jsMain

**File**: `kreekt-renderer/src/jsMain/kotlin/io/kreekt/renderer/RendererFactory.kt`
**From**: data-model.md "RendererFactory" JS actual
**Description**: Implement actual RendererFactory with WebGPU → WebGL fallback
**Details**:

```kotlin
actual object RendererFactory {
    actual suspend fun create(...): Result<Renderer, ...> {
        // 1. Try WebGPU (FR-001)
        val hasWebGPU = js("'gpu' in navigator").unsafeCast<Boolean>()
        if (hasWebGPU && config.preferredBackend != BackendType.WEBGL) {
            try {
                val renderer = WebGPURenderer(surface, config)
                renderer.initialize()
                Logger.info("Selected backend: WebGPU")
                return Result.success(renderer)
            } catch (e: Exception) {
                Logger.warn("WebGPU init failed: ${e.message}, falling back to WebGL")
            }
        }

        // 2. Fallback to WebGL2 (FR-003)
        val renderer = WebGLRenderer(surface, config)
        renderer.initialize()
        Logger.info("Selected backend: WebGL2 (fallback)")
        return Result.success(renderer)
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

**Dependencies**: T017 (RendererFactory interface), T022 (WebGPURenderer), T023 (WebGPUSurface), T025 (WebGLRenderer
fallback)
**Success Criteria**: JS contract tests (T004, T005, T006) pass

---

## Phase 3.6: VoxelCraft Refactoring

**Sequential, requires Phase 3.4 and 3.5 complete**

### T027 Remove custom WebGLRenderer instantiation from VoxelCraft Main.kt

**File**: `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`
**From**: quickstart.md "Before/After" example
**Description**: Replace direct `WebGLRenderer(canvas)` with `RendererFactory.create()`
**Details**:

```kotlin
// OLD (line ~173):
val renderer = WebGLRenderer(canvas)

// NEW:
val surface = WebGPURenderSurface(canvas)
val renderer = RendererFactory.create(surface).getOrElse { exception ->
    Logger.error("Renderer init failed", exception)
    exitProcess(1)
}
Logger.info("Using backend: ${renderer.backend}")
```

**Dependencies**: T026 (RendererFactory JS), T023 (WebGPUSurface)
**Success Criteria**: VoxelCraft JS runs, logs "Selected backend: WebGPU" or "WebGL2 (fallback)"

---

### T028 Update VoxelCraft MainJvm.kt to use RendererFactory

**File**: `examples/voxelcraft/src/jvmMain/kotlin/io/kreekt/examples/voxelcraft/MainJvm.kt`
**Description**: Replace OpenGL rendering with Vulkan via RendererFactory
**Details**:

```kotlin
// Remove OpenGL initialization (GL.createCapabilities())
// Add Vulkan renderer:
val window = glfwCreateWindow(800, 600, "VoxelCraft JVM", ...)
val surface = VulkanRenderSurface(window)
val renderer = RendererFactory.create(surface).getOrElse { exception ->
    Logger.error("Renderer init failed", exception)
    glfwTerminate()
    exitProcess(1)
}
Logger.info("Using backend: ${renderer.backend}")
```

**Dependencies**: T021 (RendererFactory JVM), T019 (VulkanSurface)
**Success Criteria**: VoxelCraft JVM runs, logs "Selected backend: Vulkan"

---

### T029 Move remaining VoxelCraft game logic to commonMain

**File**: Review all files in `examples/voxelcraft/src/`
**Description**: Ensure ≥90% of game logic is in commonMain (FR-006)
**Details**:

- Verify VoxelWorld, Player, Chunk, ChunkMeshGenerator, TerrainGenerator, BlockInteraction are in commonMain
- Only Main.kt/MainJvm.kt (entry points) and input handlers should be platform-specific
- Calculate code sharing percentage
  **Success Criteria**: ≥90% of VoxelCraft code (by line count) is in commonMain

---

### T030 Add error handling with RendererInitializationException to VoxelCraft

**File**: `examples/voxelcraft/src/commonMain/kotlin/io/kreekt/examples/voxelcraft/GameInit.kt` (new file)
**Description**: Centralize renderer initialization with proper error handling
**Details**:

```kotlin
suspend fun initializeRenderer(surface: RenderSurface): Renderer {
    return try {
        RendererFactory.create(surface).getOrThrow()
    } catch (e: RendererInitializationException.NoGraphicsSupportException) {
        Logger.error("Graphics not supported: ${e.message}")
        throw e
    } catch (e: RendererInitializationException) {
        Logger.error("Renderer init failed: ${e.message}")
        throw e
    }
}
```

**Dependencies**: T016 (RendererInitializationException), T027 (VoxelCraft JS refactor), T028 (VoxelCraft JVM refactor)
**Success Criteria**: Error messages match data-model.md format, include diagnostics

---

## Phase 3.7: Shader Pipeline

**Parallel creation, sequential integration**

### T031 [P] Write WGSL shaders for basic rendering

**File**: `kreekt-renderer/src/commonMain/resources/shaders/basic.wgsl`
**From**: research.md "Shader Cross-Compilation Strategy"
**Description**: Write WGSL vertex and fragment shaders for basic 3D rendering
**Details**:

- Vertex shader: Transform vertices with MVP matrix
- Fragment shader: Output per-vertex colors
- Compatible with both WebGPU and Vulkan (via SPIR-V)
  **Success Criteria**: WGSL shaders validate with Tint

---

### T032 [P] Add Tint WASM compiler build task

**File**: `build.gradle.kts` or `buildSrc/`
**From**: research.md "Shader Cross-Compilation Strategy"
**Description**: Create Gradle task to compile WGSL → SPIR-V at build time
**Details**:

- Download Tint WASM binary to `buildSrc/tint/`
- Create `compileShaders` task
- Input: `src/commonMain/resources/shaders/*.wgsl`
- Output: `src/jvmMain/resources/shaders/*.spv`
- Run before `jvmProcessResources`
  **Dependencies**: T003 (Tint configuration)
  **Success Criteria**: `./gradlew compileShaders` generates .spv files

---

### T033 Update platform renderers to use compiled shaders

**File**: `kreekt-renderer/src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanPipeline.kt`
**File**: `kreekt-renderer/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUPipeline.kt`
**Description**: Load and use shaders from T031 in both renderers
**Details**:

- JVM: Load SPIR-V from `src/jvmMain/resources/shaders/basic.spv`
- JS: Load WGSL from `src/commonMain/resources/shaders/basic.wgsl`
- Both use same shader logic (FR-020: visual parity)
  **Dependencies**: T031 (WGSL shaders), T032 (SPIR-V compilation), T020 (VulkanPipeline), T024 (WebGPUPipeline)
  **Success Criteria**: VoxelCraft renders identically on JVM and JS

---

## Phase 3.8: Visual Regression Testing

**Requires Phase 3.4, 3.5, 3.6 complete**

### T034 [P] Create 5 test scene fixtures

**File**: `kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/fixtures/TestScenes.kt`
**From**: research.md "Visual Parity Testing"
**Description**: Create deterministic test scenes for visual regression
**Test Scenes**:

1. Simple cube (1,000 triangles)
2. Complex mesh (10,000 triangles)
3. Lighting test (directional + point lights)
4. Transparency test (alpha blending)
5. VoxelCraft terrain (chunk of 16x16x256 blocks)
   **Success Criteria**: All scenes generate consistent geometry

---

### T035 Implement screenshot capture for JVM and JS

**File**: `kreekt-renderer/src/jvmMain/kotlin/io/kreekt/renderer/testing/ScreenshotCapture.kt`
**File**: `kreekt-renderer/src/jsMain/kotlin/io/kreekt/renderer/testing/ScreenshotCapture.kt`
**Description**: Capture framebuffer as PNG for visual regression
**Details**:

- JVM: Use LWJGL `glReadPixels()` + `BufferedImage`
- JS: Use `canvas.toDataURL()` or `canvas.toBlob()`
- Save to `build/visual-regression/[backend]-[scene].png`
  **Dependencies**: T034 (test scenes), T018 (VulkanRenderer), T022 (WebGPURenderer)
  **Success Criteria**: Screenshots saved for all test scenes

---

### T036 Create visual regression test suite with SSIM comparison

**File**: `kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/VisualRegressionTest.kt`
**From**: research.md "Visual Parity Testing", quickstart.md "Visual Regression Test"
**Description**: Compare screenshots across backends with SSIM ≥0.99 threshold
**Details**:

- Render all 5 test scenes on each backend
- Capture screenshots
- Compare WebGPU vs Vulkan (baseline)
- Compare WebGL vs WebGPU
- Use ImageMagick `compare` with SSIM metric
- Assert SSIM ≥0.99 for all scenes (FR-020)
  **Dependencies**: T034 (test scenes), T035 (screenshot capture)
  **Success Criteria**: All SSIM scores ≥0.99, tests pass

---

## Phase 3.9: Performance Validation

**Requires Phase 3.4, 3.5 complete**

### T037 Update PerformanceValidator with backend-specific thresholds

**File**: `examples/voxelcraft/src/commonMain/kotlin/io/kreekt/examples/voxelcraft/PerformanceValidator.kt`
**From**: research.md "Performance Testing and Validation"
**Description**: Extend existing PerformanceValidator with backend-specific thresholds
**Details**:

```kotlin
fun validate(metrics: PerformanceMetrics): ValidationResult {
    val threshold = when (metrics.backendType) {
        BackendType.WEBGPU, BackendType.VULKAN -> 60.0 // Primary backends
        BackendType.WEBGL -> 45.0 // Fallback has lower target
    }
    val minimumFps = 30.0 // Hard minimum per FR-019

    return ValidationResult(
        passed = metrics.fps >= minimumFps,
        meetsTarget = metrics.fps >= threshold,
        fps = metrics.fps,
        backend = metrics.backendType
    )
}
```

**Dependencies**: T018 (VulkanRenderer), T022 (WebGPURenderer), T025 (WebGLRenderer)
**Success Criteria**: Validator distinguishes primary/fallback backends

---

### T038 Add warmup logic (120 frames) before validation

**File**: `examples/voxelcraft/src/commonMain/kotlin/io/kreekt/examples/voxelcraft/PerformanceValidator.kt`
**From**: research.md "Performance Testing and Validation"
**Description**: Skip first 120 frames before performance measurement
**Details**:

- Track frame count
- Skip validation until frame 120
- After warmup: validate every 60 frames
- Log validation result (PASS/FAIL per FR-019)
  **Dependencies**: T037 (PerformanceValidator)
  **Success Criteria**: First validation occurs at frame 120

---

### T039 Implement CI integration for performance benchmarks

**File**: `.github/workflows/performance-benchmark.yml` (new file)
**From**: quickstart.md "Performance Validation"
**Description**: Create CI workflow to run performance benchmarks on GPU runners
**Details**:

- Run VoxelCraft JVM with Vulkan
- Run VoxelCraft JS with WebGPU (headless Chrome)
- Capture performance metrics (FPS, frame time)
- Assert 60 FPS target (primary backends)
- Assert 30 FPS minimum (all backends)
- Fail CI if performance regression detected
  **Dependencies**: T038 (warmup logic), T037 (performance validator)
  **Success Criteria**: CI runs benchmarks, fails on regression

---

## Phase 3.10: Documentation & Cleanup

**Can run in parallel with testing**

### T040 [P] Update CLAUDE.md with new renderer architecture

**File**: `CLAUDE.md`
**Description**: Document new renderer architecture in project instructions
**Details**:

- Add "Recent Changes" entry for feature 019
- Update architecture section with WebGPU/Vulkan primary
- Document RendererFactory usage pattern
- Update VoxelCraft example notes (no custom renderer)
  **Success Criteria**: CLAUDE.md reflects new architecture

---

### T041 [P] Add error handling troubleshooting docs

**File**: `docs/troubleshooting.md` (new file)
**From**: quickstart.md "Troubleshooting" section
**Description**: Document common errors and solutions
**Details**:

- JVM: "No Vulkan support detected" → update drivers, install SDK
- JS: Black screen → check WebGPU support, update drivers
- Performance below 30 FPS → GPU selection, scene complexity
- Copy troubleshooting section from quickstart.md
  **Success Criteria**: Troubleshooting doc covers all quickstart scenarios

---

### T042 Run full test suite and validate all FRs

**File**: N/A (meta-task)
**Description**: Execute all tests and validate all 24 functional requirements
**Details**:

```bash
# Run all tests
./gradlew :kreekt-renderer:allTests
./gradlew :examples:voxelcraft:allTests

# Run VoxelCraft on both platforms
./gradlew :examples:voxelcraft:runJvm
./gradlew :examples:voxelcraft:runJs

# Run visual regression
./gradlew :examples:voxelcraft:visualRegressionTest

# Run performance benchmark
./gradlew :examples:voxelcraft:performanceBenchmark
```

**Validation Checklist**: Use quickstart.md success criteria table
**Dependencies**: All previous tasks
**Success Criteria**: All 24 functional requirements validated ✅

---

## Dependencies Graph

```
Setup Phase (T001-T003)
  ↓
Tests Phase (T004-T010) [All Parallel - TDD Red]
  ↓
Common API Phase (T011-T017)
  T011, T012, T013 [Parallel]
  T014 (depends: T011, T012, T013)
  T015 [Parallel with T014]
  T016 [Parallel with T014, T015]
  T017 (depends: T011, T013, T014, T015, T016)
  ↓
Platform Implementation (T018-T026) [JVM and JS in parallel]
  JVM Branch:
    T018 (depends: T001, T014)
    T019 (depends: T015, T018)
    T020 (depends: T003, T018)
    T021 (depends: T017, T018, T019)
  JS Branch (parallel with JVM):
    T022 (depends: T002, T014)
    T023 (depends: T015, T022)
    T024 (depends: T022)
    T025 (depends: T014)
    T026 (depends: T017, T022, T023, T025)
  ↓
VoxelCraft Refactoring (T027-T030) [Sequential]
  T027 (depends: T026, T023)
  T028 (depends: T021, T019)
  T029 (depends: T027, T028)
  T030 (depends: T016, T027, T028)
  ↓
Shader Pipeline (T031-T033)
  T031, T032 [Parallel]
  T033 (depends: T031, T032, T020, T024)
  ↓
Visual Regression (T034-T036)
  T034 [Parallel - can start early]
  T035 (depends: T034, T018, T022)
  T036 (depends: T034, T035)
  ↓
Performance Validation (T037-T039) [Sequential]
  T037 (depends: T018, T022, T025)
  T038 (depends: T037)
  T039 (depends: T038)
  ↓
Documentation (T040-T042) [Parallel until T042]
  T040, T041 [Parallel]
  T042 (depends: ALL previous tasks)
```

---

## Parallel Execution Examples

### Example 1: Contract Tests (Phase 3.2)

All contract tests can run in parallel - they create different files:

```bash
# Launch T004-T010 together:
./gradlew test --tests "BackendDetectionTest" &
./gradlew test --tests "PrimaryBackendTest" &
./gradlew test --tests "RendererLifecycleTest" &
./gradlew test --tests "PerformanceValidationTest" &
./gradlew test --tests "ErrorHandlingTest" &
./gradlew test --tests "VisualConsistencyTest" &
./gradlew test --tests "CapabilityDetectionTest" &
wait
```

---

### Example 2: Common API (Phase 3.3)

Data classes and enums can be created in parallel:

```bash
# Launch T011-T013 together (different files):
# Edit BackendType.kt
# Edit RendererCapabilities.kt (parallel)
# Edit RendererConfig.kt + RenderStats.kt (parallel)
```

---

### Example 3: Platform Implementations (Phase 3.4 + 3.5)

JVM and JS implementations can proceed in parallel:

```bash
# JVM team works on T018-T021
# JS team works on T022-T026 (parallel)
# Both branches independent until VoxelCraft integration
```

---

### Example 4: Documentation (Phase 3.10)

Documentation tasks are independent:

```bash
# Launch T040-T041 together:
# Edit CLAUDE.md
# Edit docs/troubleshooting.md (parallel)
```

---

## Validation Checklist

*GATE: All must pass before marking feature complete*

- [x] All contracts have corresponding tests (T004-T010) ✅
- [x] All entities have model tasks (T011-T016) ✅
- [x] All tests come before implementation (Phase 3.2 before 3.3) ✅
- [x] Parallel tasks truly independent (verified above) ✅
- [x] Each task specifies exact file path ✅
- [x] No task modifies same file as another [P] task ✅
- [ ] All 42 tasks completed
- [ ] All contract tests pass (FR-001, FR-002, FR-004, FR-011, FR-019, FR-020, FR-022, FR-024)
- [ ] VoxelCraft runs on JVM with Vulkan (FR-002, FR-007)
- [ ] VoxelCraft runs on JS with WebGPU (FR-001, FR-007)
- [ ] WebGL fallback works (FR-003, FR-005)
- [ ] Visual regression SSIM ≥0.99 (FR-020)
- [ ] Performance: 60 FPS target, 30 FPS minimum (FR-019)
- [ ] Code sharing ≥90% (FR-006)
- [ ] All 24 functional requirements validated (T042)

---

## Notes

- **[P] tasks** = different files, no dependencies, safe to parallelize
- **Sequential tasks** = same file or explicit dependencies
- **TDD approach**: Phase 3.2 tests MUST fail before Phase 3.3 implementation
- **Commit after each task** for incremental progress
- **Avoid**: Modifying same file in parallel tasks, skipping tests

---

## Task Execution Strategy

### Week 1: Foundations

- Day 1-2: T001-T003 (Setup), T004-T010 (Contract tests)
- Day 3-4: T011-T017 (Common API)
- Day 5: Verify all tests fail, common code compiles

### Week 2: Platform Implementations

- Day 1-3: T018-T021 (JVM/Vulkan) + T022-T026 (JS/WebGPU) in parallel
- Day 4: T027-T030 (VoxelCraft refactoring)
- Day 5: Integration testing, verify VoxelCraft runs

### Week 3: Shaders & Testing

- Day 1-2: T031-T033 (Shader pipeline)
- Day 3: T034-T036 (Visual regression)
- Day 4: T037-T039 (Performance validation)
- Day 5: T040-T042 (Documentation, final validation)

**Total Estimated Effort**: 15-20 engineering days (3 weeks)

---

**Tasks Generated**: 42 tasks across 10 phases
**TDD Enforced**: Tests (T004-T010) before implementation (T011+)
**Parallel Opportunities**: 18 tasks marked [P]
**Critical Path**: Setup → Tests → Common → Platform → Integration → Validation

**Ready for execution** ✅
