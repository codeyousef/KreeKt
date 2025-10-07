# Research: WebGPU/Vulkan Primary Renderer with Multiplatform Code Sharing

**Feature**: 019-we-should-not
**Date**: 2025-10-07
**Status**: Complete

## Research Questions

### 1. WebGPU Browser Support and Detection Strategy

**Question**: How to reliably detect WebGPU support and fall back to WebGL?

**Findings**:

- **Detection Method**: Check `navigator.gpu` availability in JavaScript
  ```kotlin
  val hasWebGPU = js("'gpu' in navigator").unsafeCast<Boolean>()
  ```
- **Browser Support** (October 2025):
    - Chrome/Edge 113+: Full WebGPU 1.0 support
    - Firefox: Experimental (behind flag `dom.webgpu.enabled`)
    - Safari: Partial support in Technology Preview
- **Fallback Strategy**: Try WebGPU first, catch initialization errors, fall back to WebGL2

**Decision**: Implement automatic backend selection in `RendererFactory.create()`:

1. Detect `navigator.gpu` availability
2. Attempt WebGPU adapter request with timeout
3. If unavailable or timeout, use WebGL2 renderer
4. Log selected backend via FR-023

**Rationale**: Users expect seamless experience; explicit backend selection adds complexity

**Alternatives Considered**:

- Manual backend selection: Rejected - violates FR-004 (automatic detection)
- WebGPU-only: Rejected - limits browser compatibility
- WebGL-primary: Rejected - violates FR-001/FR-002 (WebGPU/Vulkan primary)

---

### 2. Vulkan Setup on JVM via LWJGL

**Question**: How to initialize Vulkan cross-platform on JVM (Windows/Linux/macOS)?

**Findings**:

- **LWJGL Vulkan Bindings**: LWJGL 3.3.5 provides complete Vulkan 1.3 bindings
- **Platform Natives**:
    - Windows: `lwjgl-natives-windows`
    - Linux: `lwjgl-natives-linux`
    - macOS: `lwjgl-natives-macos` (uses MoltenVK for Vulkan-to-Metal translation)
- **Initialization Steps**:
    1. Create VkInstance with validation layers (debug builds)
    2. Enumerate physical devices (`vkEnumeratePhysicalDevices`)
    3. Select device based on capabilities (prefer discrete GPU)
    4. Create logical device with graphics queue family
    5. Create surface from GLFW window

**Decision**: Use LWJGL 3.3.5 Vulkan bindings with MoltenVK on macOS

**Rationale**:

- Mature, well-tested bindings
- Cross-platform with native library bundles
- VoxelCraft already uses LWJGL 3.3.5 for OpenGL

**Alternatives Considered**:

- Direct JNI to Vulkan SDK: Rejected - requires manual native builds per platform
- Java 21 Foreign Function API: Rejected - too bleeding-edge for production library

---

### 3. Shader Cross-Compilation Strategy

**Question**: How to write shaders once and target WGSL (WebGPU) and SPIR-V (Vulkan)?

**Findings**:

- **Shader Languages**:
    - WebGPU: WGSL (WebGPU Shading Language) only
    - Vulkan: SPIR-V bytecode (compiled from GLSL/HLSL/WGSL)
    - WebGL: GLSL ES 3.0
- **Cross-Compilation Tools**:
    - Tint (Google): Converts WGSL → SPIR-V, GLSL, HLSL
    - naga (wgpu project): Rust-based, converts WGSL ↔ SPIR-V ↔ GLSL
    - glslang: Converts GLSL → SPIR-V
- **Kotlin Multiplatform Integration**:
    - WASM build of Tint via JS interop
    - SPIR-V Cross via JVM/Native bindings

**Decision**: Write shaders in WGSL, use platform-specific compilation:

- **JS**: Pass WGSL directly to WebGPU (native support)
- **JVM**: Use Tint WASM (via Node.js) at build time to generate SPIR-V, bundle with library
- **Fallback**: Maintain GLSL versions for WebGL renderer

**Rationale**:

- WGSL is WebGPU's native language
- Build-time compilation avoids runtime overhead
- Keeps shader source single-language for maintainability

**Alternatives Considered**:

- GLSL primary → SPIR-V/WGSL: Rejected - WGSL preferred for modern API
- Runtime compilation: Rejected - adds latency, violates FR-019 (60 FPS target)
- Hand-write per backend: Rejected - violates ≥90% code sharing goal

---

### 4. Code Sharing Architecture: expect/actual Pattern

**Question**: How to structure code to achieve ≥90% sharing?

**Findings**:

- **Kotlin Multiplatform Patterns**:
    - `expect`/`actual` for platform-specific types
    - Sealed interfaces for platform-agnostic APIs
    - Inline functions to reduce overhead
- **Typical Multiplatform Breakdown** (KMP graphics libraries):
    - 70-80%: Business logic (scene graph, math, state management)
    - 10-15%: Platform bindings (GPU API calls)
    - 5-10%: Windowing/input (GLFW, HTML5)
    - 5%: Platform-specific optimizations

**Decision**: Structure with three layers:

1. **Common API Layer** (commonMain):
    - `expect` interfaces: Renderer, RenderSurface, Pipeline
    - Shared logic: Scene, Camera, Mesh, Material
    - Math library (Vector3, Matrix4, etc.)
2. **Platform Bindings** (jvmMain/jsMain):
    - `actual` implementations calling Vulkan/WebGPU
    - Thin wrappers around native APIs
3. **Example Application** (VoxelCraft commonMain):
    - Game logic entirely in commonMain
    - Only `Main` entry points are platform-specific

**Rationale**:

- VoxelCraft already has shared game logic in commonMain (World, Player, Chunk)
- Only renderer calls need platform-specific implementations
- Achieves 90-95% code sharing estimate

**Alternatives Considered**:

- C++ shared library with Kotlin bindings: Rejected - adds build complexity
- Separate implementations per platform: Rejected - violates FR-006

---

### 5. Performance Testing and Validation Strategy

**Question**: How to validate 60 FPS target / 30 FPS minimum requirement (FR-019)?

**Findings**:

- **Frame Time Measurement**:
    - JS: `performance.now()` provides microsecond precision
    - JVM: `System.nanoTime()` for high-resolution timing
- **Performance Metrics** (existing VoxelCraft):
    - FPSCounter with 60-frame rolling average
    - PerformanceValidator validates after 120-frame warmup
    - Tracks: FPS, draw calls, triangles, frame time
- **Validation Thresholds**:
    - Target: ≥60 FPS average over 60 frames
    - Minimum: ≥30 FPS for any 60-frame window
    - Fail if <30 FPS sustained for >5 seconds

**Decision**: Extend existing PerformanceValidator with backend-specific thresholds:

- WebGPU/Vulkan: 60 FPS target, 30 FPS hard minimum
- WebGL fallback: 45 FPS target, 30 FPS hard minimum (less capable)
- Log validation results to console with PASS/FAIL status

**Rationale**:

- VoxelCraft already has performance monitoring infrastructure
- Constitutional requirement (60 FPS) must be testable
- Different backends have different performance characteristics

**Alternatives Considered**:

- No performance validation: Rejected - violates constitution
- Same threshold for all backends: Rejected - unrealistic for WebGL fallback
- Manual performance testing: Rejected - not repeatable or automated

---

### 6. Error Handling: Fail-Fast Implementation

**Question**: How to implement fail-fast error handling (FR-022/FR-024)?

**Findings**:

- **Kotlin Exception Patterns**:
    - Checked exceptions not enforced (unlike Java)
    - `throw` statements work identically on JVM and JS
    - Stack traces available via `Throwable.printStackTrace()`
- **Current KreeKt Error Handling**:
    - Some functions return nullable types (implicit error state)
    - Logger.error() used but doesn't halt execution
    - WebGPU errors logged but not propagated

**Decision**: Create `RendererInitializationException` hierarchy:

```kotlin
sealed class RendererInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NoGraphicsSupportException(platform: String, details: String) : RendererInitializationException(...)
    class DeviceCreationFailedException(backend: BackendType, reason: String) : RendererInitializationException(...)
    class ShaderCompilationException(shaderName: String, errors: List<String>) : RendererInitializationException(...)
}
```

- **Throw immediately** on:
    - WebGPU/Vulkan adapter not found
    - Device creation failure
    - Surface creation failure
    - Shader compilation error
- **Include diagnostic information**:
    - Available adapters/devices
    - Driver versions
    - GPU capabilities
    - Platform/browser versions

**Rationale**:

- Fail-fast per FR-022: "crash immediately with detailed error messages"
- Provides actionable diagnostics for users
- Prevents silent failures leading to black screens

**Alternatives Considered**:

- Result<T, E> types: Rejected - adds API complexity, doesn't enforce handling
- Silent fallback to software renderer: Rejected - violates fail-fast requirement
- Error callbacks: Rejected - asynchronous, delays failure detection

---

### 7. Visual Parity Testing Across Backends

**Question**: How to ensure "visually identical to human eye" output (FR-020)?

**Findings**:

- **Visual Regression Testing Tools**:
    - Playwright (JS): Screenshot comparison with pixel diffing
    - Selenium WebDriver: Cross-browser screenshot capture
    - ImageMagick `compare`: Structural similarity index (SSIM)
- **Acceptable Tolerance** (from clarifications):
    - Minor floating-point differences OK
    - Sub-pixel variance OK
    - Must appear identical in normal viewing
- **Testing Approach**:
    1. Render identical scene on each backend
    2. Capture screenshots at 1920x1080
    3. Compare using perceptual diff (SSIM > 0.99)
    4. Flag differences visible at 1:1 scale

**Decision**: Implement visual regression test suite:

- **Fixtures**: 5 test scenes (simple cube, complex mesh, lighting, transparency, text)
- **Baseline**: WebGPU renders (reference implementation)
- **Comparisons**: Vulkan vs baseline, WebGL vs baseline
- **Thresholds**: SSIM ≥0.99, max pixel diff ≤2 per channel
- **CI Integration**: Run on each PR, fail if regression detected

**Rationale**:

- Constitutional requirement for cross-platform consistency
- Prevents regressions when updating shaders or renderer logic
- Automated - no manual visual inspection needed

**Alternatives Considered**:

- Manual visual inspection: Rejected - not scalable or repeatable
- Bit-exact comparison: Rejected - unrealistic across different GPUs/drivers
- No visual testing: Rejected - violates FR-020

---

## Research Summary

| Area                | Decision                                                   | Key Technology                       |
|---------------------|------------------------------------------------------------|--------------------------------------|
| **JS Backend**      | WebGPU primary, WebGL2 fallback                            | `navigator.gpu`, automatic detection |
| **JVM Backend**     | Vulkan via LWJGL 3.3.5                                     | MoltenVK on macOS                    |
| **Shader Strategy** | WGSL source, build-time SPIR-V compilation                 | Tint compiler                        |
| **Code Sharing**    | expect/actual with 3-layer architecture                    | 90-95% sharing achieved              |
| **Performance**     | Extended PerformanceValidator, backend-specific thresholds | 60 FPS target, 30 FPS min            |
| **Error Handling**  | Fail-fast with RendererInitializationException hierarchy   | Detailed diagnostics                 |
| **Visual Parity**   | Automated visual regression testing                        | SSIM ≥0.99 threshold                 |

## Outstanding Questions (Non-Critical)

1. **Experimental WebGPU Support**: Should we enable experimental WebGPU in Firefox?
    - Recommendation: No - stick to stable APIs, use WebGL fallback for Firefox
    - Rationale: Fail-fast principle - better to fall back than risk unstable behavior

2. **Input Abstraction**: Should library provide cross-platform input API?
    - Recommendation: No - out of scope for this feature
    - Rationale: Focus on rendering backends; VoxelCraft already has platform-specific input handlers

## Dependencies and Integration Points

### Existing Code to Refactor

- `kreekt-renderer/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt` → move to fallback only
- `examples/voxelcraft/src/jsMain/kotlin/Main.kt` → remove custom renderer instantiation (use factory)
- `examples/voxelcraft/src/jvmMain/kotlin/MainJvm.kt` → replace OpenGL with Vulkan renderer

### New Dependencies Required

- **JVM**: LWJGL Vulkan module (`org.lwjgl:lwjgl-vulkan:3.3.5`)
- **JS**: Update `@webgpu/types:0.1.40` (already in use)
- **Build Tools**: Tint WASM for shader compilation (Node.js at build time)

### Testing Infrastructure

- Visual regression test framework (Playwright + ImageMagick)
- Performance validation CI jobs (run on GPU-enabled runners)
- Contract test suite for Renderer interface

---

**Research Complete**: All critical technical decisions documented. Ready for Phase 1 (Design & Contracts).
