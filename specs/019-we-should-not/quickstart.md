# Quickstart: WebGPU/Vulkan Primary Renderer

**Feature**: 019-we-should-not
**Audience**: Developers testing the new renderer implementation
**Estimated Time**: 10 minutes

## Prerequisites

- Kotlin 1.9+ installed
- JDK 17+ for JVM target
- Modern browser (Chrome 113+, Edge 113+) for JS target
- GPU with Vulkan 1.1+ support (JVM) or WebGPU support (browser)
- Graphics drivers up to date

## Quick Start

### 1. Clone and Build

```bash
cd /mnt/d/Projects/KMP/KreeKt

# Build library
./gradlew :kreekt-renderer:build

# Run tests (validates FR-011, FR-019, FR-020)
./gradlew :kreekt-renderer:allTests
```

**Expected Output**:

```
> Task :kreekt-renderer:jvmTest
RendererContractTest > RendererFactory detects Vulkan PASSED
RendererContractTest > Renderer maintains 60 FPS target PASSED
✅ Performance: 62.3 FPS, 16.0ms/frame

> Task :kreekt-renderer:jsTest
RendererContractTest > RendererFactory detects WebGPU PASSED
RendererContractTest > Renderer maintains 60 FPS target PASSED
✅ Performance: 60.1 FPS, 16.6ms/frame

BUILD SUCCESSFUL
```

---

### 2. Run VoxelCraft Example (JVM)

```bash
./gradlew :examples:voxelcraft:runJvm
```

**Expected Behavior**:

- Window opens with title "VoxelCraft JVM"
- Console shows backend selection log:
  ```
  [INFO] 🔧 Initializing renderer backend for VoxelCraft...
  [INFO] 📊 Backend Negotiation:
  [INFO]   Detecting capabilities...
  [INFO]   Available backends: Vulkan 1.3
  [INFO]   Selected: Vulkan
  [INFO]   Features:
  [INFO]     COMPUTE: Available
  [INFO]     RAY_TRACING: Available (hardware-dependent)
  [INFO] ✅ Renderer initialized!
  [INFO]   Init Time: ~150ms
  [INFO]   Backend: Vulkan
  ```
- 3D voxel terrain renders in window
- Performance HUD shows:
  ```
  60 FPS | 87234▲ | 89DC
  Position: X: 0, Y: 100, Z: 0
  Flight: ON
  Chunks: 81 / 1024
  ```

**Validation**:

- ✅ **FR-002**: Vulkan selected as primary backend
- ✅ **FR-004**: Automatic backend detection (no manual selection)
- ✅ **FR-007**: VoxelCraft uses library renderer (no custom WebGLRenderer code)
- ✅ **FR-019**: 60 FPS target achieved (check HUD)
- ✅ **FR-023**: Backend logged at initialization

---

### 3. Run VoxelCraft Example (JavaScript)

```bash
./gradlew :examples:voxelcraft:runJs
```

**Browser Opens**: `http://localhost:8080`

**Click "Start Game" Button**

**Expected Console Output**:

```
🎮 VoxelCraft Starting...
📦 Page loaded, waiting for user to click Start...
🎮 Starting game from button click...
🔧 Initializing renderer backend for VoxelCraft...
📊 Backend Negotiation:
  Detecting capabilities...
  Available backends: WebGPU 1.0
  Selected: WebGPU
  Features:
    COMPUTE: Available
    RAY_TRACING: Not available
✅ Renderer initialized!
  Init Time: ~80ms
  Backend: WebGPU
🌍 Generating terrain... 100% (81/81 chunks)
✅ Terrain generation complete in 1234ms
📦 Chunks: 81
```

**Expected Behavior**:

- 3D voxel terrain renders in browser canvas
- Mouse controls camera (WASD to move)
- Performance HUD shows 60 FPS
- No WebGL fallback message (WebGPU used)

**Validation**:

- ✅ **FR-001**: WebGPU selected as primary backend
- ✅ **FR-004**: Automatic backend detection
- ✅ **FR-005**: WebGL NOT used (only fallback if WebGPU unavailable)
- ✅ **FR-007**: VoxelCraft uses library renderer
- ✅ **FR-019**: 60 FPS target achieved
- ✅ **FR-023**: Backend logged at initialization

---

### 4. Test WebGL Fallback (JavaScript)

**Simulate WebGPU unavailable** by disabling it in browser:

**Chrome/Edge**:

```
chrome://flags/#enable-unsafe-webgpu
→ Set to "Disabled"
→ Restart browser
```

**Run VoxelCraft JS again**:

```bash
./gradlew :examples:voxelcraft:runJs
```

**Expected Console Output**:

```
🔧 Initializing renderer backend for VoxelCraft...
📊 Backend Negotiation:
  Detecting capabilities...
  Available backends: WebGL 2.0
  Selected: WebGL2 (WebGPU unavailable)
  Features:
    COMPUTE: Not available
    RAY_TRACING: Not available
✅ Renderer initialized!
  Init Time: ~50ms
  Backend: WebGL
```

**Validation**:

- ✅ **FR-003**: WebGL fallback when WebGPU unavailable
- ✅ **FR-005**: WebGL used ONLY as fallback (not primary)
- ✅ **FR-020**: Visual output identical to WebGPU (compare screenshots)

---

### 5. Test Error Handling (Fail-Fast)

**Simulate graphics driver failure** (JVM):

```bash
# Temporarily hide Vulkan library (Linux example)
export VK_ICD_FILENAMES=/nonexistent/path
./gradlew :examples:voxelcraft:runJvm
```

**Expected Output**:

```
[ERROR] ❌ Renderer initialization failed!
io.kreekt.renderer.RendererInitializationException$NoGraphicsSupportException:
  No supported graphics API found on JVM.
  Available backends: []
  Required features: [Vulkan 1.1+]

  Troubleshooting:
  - Update graphics drivers to latest version
  - Ensure GPU supports Vulkan 1.1+
  - Check Vulkan SDK installation

  at io.kreekt.renderer.RendererFactory.create(RendererFactory.kt:42)
  ...

Process finished with exit code 1
```

**Validation**:

- ✅ **FR-022**: Crash immediately with detailed error message (fail-fast)
- ✅ **FR-024**: Detect backend capability mismatch before rendering
- Error message contains:
    - Platform (JVM)
    - Available backends ([])
    - Required features (Vulkan 1.1+)
    - Troubleshooting steps

---

### 6. Visual Regression Test (Cross-Backend)

**Compare visual output across backends**:

```bash
# Generate reference screenshots
./gradlew :examples:voxelcraft:generateVisualBaseline

# Run visual regression tests
./gradlew :examples:voxelcraft:visualRegressionTest
```

**Expected Output**:

```
> Task :examples:voxelcraft:visualRegressionTest
Comparing WebGPU vs Vulkan:
  Scene 1 (simple cube): SSIM = 0.997 ✅ PASS
  Scene 2 (complex mesh): SSIM = 0.996 ✅ PASS
  Scene 3 (lighting): SSIM = 0.995 ✅ PASS
  Scene 4 (transparency): SSIM = 0.994 ✅ PASS
  Scene 5 (VoxelCraft terrain): SSIM = 0.992 ✅ PASS

Comparing WebGL vs WebGPU:
  Scene 1 (simple cube): SSIM = 0.996 ✅ PASS
  Scene 2 (complex mesh): SSIM = 0.994 ✅ PASS
  ...

All visual tests PASSED (threshold: 0.99)
```

**Validation**:

- ✅ **FR-020**: Visual output visually identical to human eye across backends
- SSIM ≥0.99 = sub-pixel variance acceptable
- All 5 test scenes render consistently

---

### 7. Performance Validation

**Run performance benchmark**:

```bash
./gradlew :examples:voxelcraft:performanceBenchmark
```

**Expected Output**:

```
=== Performance Benchmark: VoxelCraft ===

Backend: Vulkan (JVM)
  Warmup: 120 frames (discarded)
  Measurement: 600 frames (10 seconds)

Results:
  Average FPS: 61.8
  Minimum FPS: 58.2
  Frame Time (avg): 16.2ms
  Frame Time (p95): 17.1ms
  Frame Time (p99): 18.5ms

  Draw Calls (avg): 89
  Triangles (avg): 87,234

Validation:
  ✅ PASS: 60 FPS target met (FR-019)
  ✅ PASS: 30 FPS minimum never violated (FR-019)
  ✅ PASS: Frame time <16.67ms average (60 FPS)

---

Backend: WebGPU (JS)
  Average FPS: 59.4
  Minimum FPS: 54.1
  Frame Time (avg): 16.8ms

Validation:
  ✅ PASS: 60 FPS target met (FR-019)
  ✅ PASS: 30 FPS minimum never violated (FR-019)

---

Backend: WebGL (JS fallback)
  Average FPS: 47.2
  Minimum FPS: 41.3
  Frame Time (avg): 21.2ms

Validation:
  ⚠️  WARN: 60 FPS target not met (expected for fallback)
  ✅ PASS: 30 FPS minimum maintained (FR-019)

=== OVERALL: PASS ===
Primary backends (WebGPU/Vulkan) meet 60 FPS target.
Fallback backend (WebGL) meets 30 FPS minimum.
```

**Validation**:

- ✅ **FR-019**: Primary backends achieve 60 FPS target
- ✅ **FR-019**: All backends maintain 30 FPS minimum
- ✅ Performance scales with backend capability (WebGPU > WebGL)

---

## Success Criteria

All tests must pass:

| Requirement                            | Test                                  | Status |
|----------------------------------------|---------------------------------------|--------|
| **FR-001: WebGPU primary (JS)**        | VoxelCraft JS logs WebGPU selected    | ✅      |
| **FR-002: Vulkan primary (JVM)**       | VoxelCraft JVM logs Vulkan selected   | ✅      |
| **FR-003: WebGL fallback**             | Disable WebGPU → WebGL selected       | ✅      |
| **FR-004: Automatic detection**        | No manual backend selection in code   | ✅      |
| **FR-005: WebGL only as fallback**     | WebGPU used when available            | ✅      |
| **FR-006: ≥90% code sharing**          | VoxelCraft commonMain has game logic  | ✅      |
| **FR-007: Library-only APIs**          | No custom renderer code in VoxelCraft | ✅      |
| **FR-011: Unified renderer interface** | Contract tests pass                   | ✅      |
| **FR-019: 60 FPS target**              | Performance benchmark passes          | ✅      |
| **FR-020: Visual parity**              | Visual regression SSIM ≥0.99          | ✅      |
| **FR-022: Fail-fast errors**           | NoGraphicsSupportException thrown     | ✅      |
| **FR-023: Log backend**                | Console shows "Selected: [backend]"   | ✅      |
| **FR-024: Detect mismatches**          | Renderer checks capabilities at init  | ✅      |

---

## Troubleshooting

### JVM: "No Vulkan support detected"

**Symptoms**: `NoGraphicsSupportException` on JVM

**Solutions**:

1. Update graphics drivers:
    - NVIDIA: https://www.nvidia.com/download/index.aspx
    - AMD: https://www.amd.com/en/support
    - Intel: https://www.intel.com/content/www/us/en/download-center/home.html
2. Install Vulkan SDK: https://vulkan.lunarg.com/
3. Verify Vulkan: `vulkaninfo` (Linux/macOS) or `vulkaninfoSDK.exe` (Windows)

### JS: Black screen in browser

**Symptoms**: Canvas shows black screen, no errors in console

**Solutions**:

1. Check browser compatibility:
    - Chrome/Edge 113+ for WebGPU
    - Firefox 115+ with `dom.webgpu.enabled` flag
2. Update graphics drivers (WebGPU requires up-to-date drivers)
3. Try incognito mode (disable extensions)
4. Check console for fallback message: "Selected: WebGL2"

### Performance below 30 FPS

**Symptoms**: HUD shows <30 FPS sustained

**Solutions**:

1. Check GPU selection (Windows): Force high-performance GPU in Settings
2. Reduce scene complexity: Lower chunk count in VoxelCraft config
3. Enable V-sync: Prevent frame pacing issues
4. Check background processes: GPU-intensive apps (games, video encoding)

---

## Code Examples

### Using the New Renderer API

**Before (018 - Custom Renderer)**:

```kotlin
// examples/voxelcraft/src/jsMain/kotlin/Main.kt (OLD)
val renderer = WebGLRenderer(canvas)  // ❌ Custom instantiation
```

**After (019 - Library Factory)**:

```kotlin
// examples/voxelcraft/src/commonMain/kotlin/GameInit.kt (NEW)
val surface = createRenderSurface(canvas)  // Platform-specific
val renderer = RendererFactory.create(surface).getOrElse { exception ->
    Logger.error("Renderer init failed", exception)
    exitProcess(1)
}

// Renderer automatically selects WebGPU (JS) or Vulkan (JVM)
Logger.info("Using backend: ${renderer.backend}")
```

### Error Handling

**Recommended Pattern**:

```kotlin
try {
    val renderer = RendererFactory.create(surface).getOrThrow()
    gameLoop(renderer)
} catch (e: RendererInitializationException.NoGraphicsSupportException) {
    showErrorDialog(
        title = "Graphics Not Supported",
        message = e.message ?: "Unknown error",
        details = e.stackTraceToString()
    )
    exitProcess(1)
} catch (e: RendererInitializationException) {
    Logger.error("Renderer init failed", e)
    exitProcess(1)
}
```

---

## Next Steps

After validating quickstart:

1. **Run Contract Tests**: `./gradlew :kreekt-renderer:allTests`
2. **Run Visual Regression**: `./gradlew :examples:voxelcraft:visualRegressionTest`
3. **Run Performance Benchmark**: `./gradlew :examples:voxelcraft:performanceBenchmark`
4. **Inspect Code Sharing**: Check VoxelCraft `commonMain` has ≥90% of game logic
5. **Review Error Messages**: Simulate failures, validate diagnostic quality

**Quickstart Complete** ✅

Expected Time: 10 minutes
Actual Time: _________
All Tests Passed: ☐ Yes ☐ No (list failures: _________________)
