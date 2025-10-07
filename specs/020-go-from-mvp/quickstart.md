# VoxelCraft Rendering Quickstart

**Feature**: Production-Ready Renderer with Full VoxelCraft Integration
**Date**: 2025-10-07
**Prerequisites**: Feature 019 MVP foundation, JVM 17+ or Chrome 113+

## Quick Start Guide

### 1. Run VoxelCraft on Web (WebGPU)

```bash
# From repository root
./gradlew :examples:voxelcraft:runJs
```

**Expected Output**:

```
> Task :examples:voxelcraft:jsBrowserDevelopmentRun
Starting development server at http://localhost:8080
BUILD SUCCESSFUL
```

**Browser Output** (Console):

```
[GameInit] Initializing renderer...
[RendererFactory] Detecting available backends...
[RendererFactory] ✓ WebGPU detected
[WebGPURenderer] Creating GPU adapter...
[WebGPURenderer] ✓ GPU device created
[WebGPURenderer] Initializing rendering pipeline...
[BufferManager] Creating vertex buffers...
[RenderPassManager] Configuring render pass...
[SwapchainManager] Configuring swapchain (800×600, BGRA8Unorm, FIFO)
[VoxelWorld] Generating terrain...
[ChunkMeshGenerator] Generated 1,024 chunks (3.2M triangles)
[BufferManager] Uploaded 1,024 vertex buffers (85MB)
[GameInit] ✓ Renderer initialized in 1,847ms
[GameLoop] FPS: 62.3, Frame time: 16.0ms, Triangles: 3,247,891
```

**Visual Validation**:

- ✅ Voxel terrain renders with correct colors (grass=green, dirt=brown, stone=gray)
- ✅ Camera movement smooth (WASD controls)
- ✅ FPS counter shows 60+ FPS
- ✅ No visual artifacts or gaps between chunks
- ✅ Block breaking/placing updates mesh immediately

---

### 2. Run VoxelCraft on Desktop (Vulkan)

```bash
# From repository root
./gradlew :examples:voxelcraft:runJvm
```

**Expected Output**:

```
> Task :examples:voxelcraft:jvmRun
[GameInit] Initializing renderer...
[RendererFactory] Detecting available backends...
[RendererFactory] ✓ Vulkan 1.3.204 detected
[VulkanRenderer] Creating Vulkan instance...
[VulkanRenderer] Selecting physical device...
[VulkanRenderer] ✓ Selected: NVIDIA GeForce RTX 3080 (Discrete GPU)
[VulkanRenderer] Creating logical device...
[VulkanRenderer] ✓ Graphics queue family: 0
[BufferManager] Allocating VkDeviceMemory for buffers...
[RenderPassManager] Creating VkRenderPass...
[SwapchainManager] Creating VkSwapchainKHR (800×600, B8G8R8A8_UNORM, FIFO)
[VoxelWorld] Generating terrain...
[ChunkMeshGenerator] Generated 1,024 chunks (3.2M triangles)
[BufferManager] Uploaded 1,024 vertex buffers (85MB)
[GameInit] ✓ Renderer initialized in 1,612ms
[GameLoop] FPS: 64.1, Frame time: 15.6ms, Triangles: 3,247,891
```

**Visual Validation**:

- ✅ GLFW window opens (800×600)
- ✅ Voxel terrain renders identically to web version (95%+ SSIM)
- ✅ Camera controls work (WASD + mouse look)
- ✅ FPS counter shows 60+ FPS
- ✅ Window resize works without crashes

---

## Validation Checklist

### ✅ Backend Detection

- [ ] **JS**: WebGPU detected (not WebGL fallback)
    - Check console: `[RendererFactory] ✓ WebGPU detected`
    - If WebGL fallback: Upgrade to Chrome 113+ or Edge 113+
- [ ] **JVM**: Vulkan detected (not OpenGL)
    - Check console: `[RendererFactory] ✓ Vulkan X.X.X detected`
    - If Vulkan not found: Install GPU drivers, run `vulkaninfo` to verify

### ✅ Initialization Performance

- [ ] Renderer initializes within 2 seconds
    - Check console: `[GameInit] ✓ Renderer initialized in XXXXms`
    - Target: < 2000ms
    - If slower: Check GPU capabilities, reduce chunk count

### ✅ Runtime Performance

- [ ] FPS >= 60 during normal gameplay
    - Check console: `[GameLoop] FPS: XX.X`
    - Target: >= 60 FPS
    - Minimum: >= 30 FPS
- [ ] Frame time <= 16.67ms (60 FPS target)
    - Check console: `[GameLoop] Frame time: XX.Xms`
- [ ] No frame drops below 30 FPS
    - Monitor FPS during camera movement and block updates

### ✅ Visual Quality

- [ ] Terrain colors correct
    - Grass blocks: Green (#00FF00 variants)
    - Dirt blocks: Brown (#8B4513 variants)
    - Stone blocks: Gray (#808080 variants)
- [ ] No visual artifacts
    - No gaps between chunks
    - No z-fighting or flickering
    - No color banding
- [ ] Chunk boundaries seamless
    - Look at chunk edges (16-block boundaries)
    - Should be invisible

### ✅ Interaction

- [ ] Camera movement smooth (WASD keys)
- [ ] Mouse look works (first-person view)
- [ ] Block breaking updates mesh immediately (JS only currently)
- [ ] Block placing updates mesh immediately (JS only currently)
- [ ] Window/canvas resize works without crashes

### ✅ Memory Usage

- [ ] Memory usage < 500MB
    - Check Task Manager / Activity Monitor
    - VoxelCraft typical: 100-200MB
    - Maximum constitutional limit: 500MB

---

## Troubleshooting

### Issue: WebGPU not available (JS)

**Symptoms**:

```
[RendererFactory] ⚠ WebGPU not available, falling back to WebGL
```

**Solutions**:

1. Upgrade browser to Chrome 113+ or Edge 113+
2. Check `chrome://flags#enable-unsafe-webgpu` (set to Enabled)
3. Verify `chrome://gpu` shows WebGPU support

**Workaround**: WebGL fallback should work (expect 30-45 FPS instead of 60+)

---

### Issue: Vulkan not found (JVM)

**Symptoms**:

```
[RendererFactory] ❌ Vulkan not found
NoGraphicsSupportException: Vulkan 1.1+ required but not available
```

**Solutions**:

1. Install/update GPU drivers:
    - NVIDIA: https://www.nvidia.com/Download/index.aspx
    - AMD: https://www.amd.com/en/support
    - Intel: https://www.intel.com/content/www/us/en/download-center/home.html
2. Verify Vulkan installation:
   ```bash
   vulkaninfo  # Should show Vulkan instance and devices
   ```
3. Check GLFW Vulkan support:
   ```bash
   glfwinfo  # Should show Vulkan loader available
   ```

**No Workaround**: JVM has no OpenGL fallback per Feature 019 FR-002

---

### Issue: FPS < 30 (Performance)

**Symptoms**:

```
[GameLoop] FPS: 25.3, Frame time: 39.7ms
⚠ Performance below minimum requirement (30 FPS)
```

**Solutions**:

1. Check GPU capabilities:
    - JVM: Check console for GPU name (prefer discrete GPU, not integrated)
    - JS: Check `chrome://gpu` for hardware acceleration
2. Reduce render distance (edit VoxelWorld.kt):
   ```kotlin
   // Reduce from 16 to 8 chunks radius
   val renderDistance = 8  // instead of 16
   ```
3. Disable MSAA (edit RendererConfig):
   ```kotlin
   val config = RendererConfig(msaaSamples = 1)  // Disable antialiasing
   ```
4. Check for background processes consuming GPU

---

### Issue: Visual artifacts (Flickering, gaps, z-fighting)

**Symptoms**:

- Flickering triangles
- Gaps between chunks
- Overlapping geometry (z-fighting)

**Solutions**:

1. Run visual regression tests:
   ```bash
   ./gradlew :kreekt-renderer:test --tests VisualRegressionTest
   ```
2. Check SSIM similarity score (should be >= 0.95):
   ```
   [VisualRegressionTest] SSIM: 0.97 (PASS)
   ```
3. If SSIM < 0.95: File bug report with screenshot

---

### Issue: Initialization timeout (> 2 seconds)

**Symptoms**:

```
[GameInit] ⚠ Renderer initialized in 3,421ms (exceeds 2s target)
```

**Solutions**:

1. Check GPU memory availability:
    - VoxelCraft needs ~100MB GPU memory
    - Close other GPU-intensive applications
2. Reduce initial chunk count (edit VoxelWorld.kt):
   ```kotlin
   // Load fewer chunks on startup
   val initialChunks = 512  // instead of 1024
   ```
3. Check disk I/O (shader compilation caches)

---

### Issue: Memory usage > 500MB

**Symptoms**:

```
[GameLoop] Memory: 542MB (exceeds 500MB constitutional limit)
```

**Solutions**:

1. Reduce loaded chunks (edit VoxelWorld.kt):
   ```kotlin
   val maxLoadedChunks = 768  // instead of 1024
   ```
2. Implement chunk unloading (future feature)
3. Check for memory leaks:
   ```bash
   ./gradlew :kreekt-renderer:test --tests MemoryLeakTest
   ```

---

### Issue: Window resize crash (JVM)

**Symptoms**:

- Window resize triggers exception
- Swapchain recreation fails

**Solutions**:

1. Check Vulkan swapchain recreation:
   ```
   [SwapchainManager] Recreating swapchain (1024×768)
   ```
2. Ensure VkDeviceWaitIdle() called before recreation
3. Run resize test:
   ```bash
   ./gradlew :kreekt-renderer:test --tests SwapchainTest.testRecreateSwapchain
   ```

---

## Performance Metrics

### Expected Performance (Target Hardware)

**Target Hardware**:

- **GPU**: NVIDIA GTX 1660 or equivalent (discrete GPU)
- **CPU**: Intel i5-8400 or equivalent (6 cores)
- **RAM**: 8GB
- **OS**: Windows 10/11, Ubuntu 20.04+, macOS 11+

**Performance Targets**:
| Metric | Target | Minimum | VoxelCraft Typical |
|--------|--------|---------|-------------------|
| FPS | 60+ | 30+ | 62-64 FPS |
| Frame Time | ≤16.67ms | ≤33.33ms | 15-16ms |
| Initialization | ≤2s | ≤3s | 1.6-1.8s |
| Memory Usage | ≤250MB | ≤500MB | 150-200MB |
| Triangle Count | 100k @ 60 FPS | 50k @ 30 FPS | 3.2M @ 60+ FPS |

---

## Visual Regression Testing

### Automated Tests

Run visual regression tests to validate cross-platform consistency:

```bash
# Run all visual regression tests
./gradlew :kreekt-renderer:test --tests VisualRegressionTest

# Expected output:
# ✓ testVisualParity_SimpleCube_VulkanVsWebGPU (SSIM: 0.98)
# ✓ testVisualParity_ComplexMesh_AllBackends (SSIM: 0.96)
# ✓ testVisualParity_VoxelTerrain_AllBackends (SSIM: 0.97)
```

### Manual Comparison

1. Launch VoxelCraft on both platforms
2. Navigate to same world position (e.g., spawn at x=0, y=100, z=0)
3. Take screenshots:
    - JS: Press F12 → Console → `ScreenshotCapture.capture(canvas, "webgpu-voxelcraft.png")`
    - JVM: Screenshots saved to `build/visual-regression/vulkan-voxelcraft.png`
4. Compare screenshots visually:
    - Colors should match exactly
    - Chunk boundaries identical
    - No artifacts in either version

---

## Next Steps

### After Successful Quickstart

1. **Explore VoxelCraft**:
    - WASD to move
    - Mouse to look around
    - Left click to break blocks (JS only)
    - Right click to place blocks (JS only)

2. **Run Full Test Suite**:
   ```bash
   ./gradlew :kreekt-renderer:test
   ```

3. **Run Performance Benchmarks**:
   ```bash
   ./gradlew :kreekt-renderer:test --tests PerformanceBenchmarkTest
   ```

4. **Check Implementation Status**:
   ```bash
   # View tasks.md for remaining work
   cat specs/020-go-from-mvp/tasks.md
   ```

---

## Getting Help

### Documentation

- **Feature 020 Spec**: `specs/020-go-from-mvp/spec.md`
- **Implementation Plan**: `specs/020-go-from-mvp/plan.md`
- **Data Model**: `specs/020-go-from-mvp/data-model.md`
- **API Contracts**: `specs/020-go-from-mvp/contracts/`

### Debug Logging

Enable verbose logging for troubleshooting:

```kotlin
// Add to Main.kt or MainJvm.kt
Logger.setLevel(LogLevel.DEBUG)
```

**Output**:

```
[DEBUG][BufferManager] Creating vertex buffer (size: 72 bytes, vertices: 3)
[DEBUG][RenderPassManager] Begin render pass (clearColor: (0.53, 0.81, 0.92, 1.0))
[DEBUG][SwapchainManager] Acquire swapchain image (index: 0)
[DEBUG][RenderPassManager] Bind pipeline (handle: 0x7f8a4c000000)
[DEBUG][RenderPassManager] Draw indexed (indexCount: 3, firstIndex: 0)
[DEBUG][SwapchainManager] Present swapchain image (index: 0)
```

### Report Issues

If you encounter issues not covered in this guide:

1. Check existing issues: https://github.com/your-org/kreekt/issues
2. Run diagnostics:
   ```bash
   # Collect system info
   vulkaninfo > vulkan-info.txt  # JVM
   chrome://gpu > gpu-info.txt   # JS
   ```
3. File bug report with:
    - Console output
    - System info (GPU, driver version, OS)
    - Screenshots (if visual issue)
    - Steps to reproduce

---

**Quickstart Complete**: ✅ VoxelCraft running with WebGPU/Vulkan rendering
