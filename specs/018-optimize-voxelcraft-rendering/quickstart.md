# Quickstart: VoxelCraft 60 FPS Rendering

**Feature**: 018-optimize-voxelcraft-rendering
**Date**: 2025-10-07
**Time Required**: 5 minutes

## Prerequisites

- Modern web browser with WebGPU support (Chrome 113+, Edge 113+) OR WebGL 2.0 support (Firefox, Safari)
- VoxelCraft example built and served locally
- Terminal access for running Gradle commands

## Scenario 1: WebGPU 60 FPS Validation (Chrome/Edge)

### Steps

1. **Build and start VoxelCraft**:
   ```bash
   cd /home/yousef/Projects/kmp/KreeKt
   ./gradlew :examples:voxelcraft:jsRun
   ```

2. **Open in Chrome 113+ or Edge 113+**:
   - Navigate to: `http://localhost:8080`
   - Wait for "Start" button to appear

3. **Start the game**:
   - Click "Start" button
   - Observe loading progress: "Generating terrain... 0% → 100%"
   - Wait for "Terrain ready" message

4. **Verify WebGPU backend**:
   - Check browser console for: `"✅ WebGPU available - using WebGPU renderer"`
   - Check for: `"WebGPU renderer initialized in ~50ms"`

5. **Observe FPS counter** (top-left HUD):
   - Should display: `60 FPS | ~160K▲ | ~85DC`
   - FPS = Frames per second (target: 60)
   - ▲ = Triangles rendered (~160K for 81 chunks)
   - DC = Draw calls (target: <100)

6. **Test camera movement**:
   - Move: WASD keys
   - Look: Mouse movement (pointer locked)
   - Fly up/down: Space/Shift
   - FPS should remain at 60 with smooth movement

7. **Test frustum culling** (optional):
   - Press F3 to open debug overlay
   - Observe "Visible chunks" count
   - Rotate camera 180°
   - Count should change (~40-50 visible vs 81 total)
   - Draw calls should match visible chunk count

8. **Test dynamic mesh updates**:
   - Left-click to break a block
   - Observe FPS: May dip briefly to 55-58, should recover to 60 within 1 second
   - Right-click to place a block
   - FPS should remain stable (>58)

### Expected Output

**Console Log**:
```
🎮 VoxelCraft Starting...
📦 Page loaded, waiting for user to click Start...
🎮 Starting game from button click...
🌍 Initializing VoxelCraft...
🔧 Initializing renderer backend for VoxelCraft...
📊 Backend Negotiation:
  Detecting capabilities...
  Available backends: WebGPU 1.0
  Selected: WebGPU
✅ WebGPU renderer initialized in 48ms
✅ Renderer initialized!
  Init Time: ~50ms
  Within Budget: true (2000ms limit)
?? Generating world...
? Generating terrain... 10% (102/1024 chunks)
? Generating terrain... 20% (204/1024 chunks)
...
? Terrain generation complete in 1247ms
?? Chunks: 81
?? Game loop started!
```

**HUD Display**:
```
FPS: 60 | 162,354▲ | 87DC
Backend: WebGPU 1.0
Position: X: 0, Y: 100, Z: 0
Flight: ON
Chunks: 81 / 1024
```

**Performance Metrics** (F3 debug overlay):
```
Backend: WebGPU 1.0
FPS: 60.2 (16.6ms)
Draw Calls: 87 / 81 chunks
Triangles: 162,354
Visible Chunks: 81 / 81
Culled Chunks: 0
Pipeline Cache: 2 pipelines, 98.5% hit rate
GPU Memory: ~18 MB
```

### Success Criteria

✅ FPS >= 60 (sustained over 60 frames)
✅ Draw calls < 100 for 81 chunks
✅ Backend: WebGPU 1.0
✅ Initialization < 2000ms (typically 50-100ms)
✅ Smooth camera movement (no stuttering)
✅ Dynamic mesh updates don't cause frame drops (<5 FPS dip)

---

## Scenario 2: WebGL Fallback 30+ FPS Validation (Firefox/Safari)

### Steps

1. **Build and start VoxelCraft**:
   ```bash
   cd /home/yousef/Projects/kmp/KreeKt
   ./gradlew :examples:voxelcraft:jsRun
   ```

2. **Open in Firefox or Safari**:
   - Navigate to: `http://localhost:8080`
   - Wait for "Start" button

3. **Start the game**:
   - Click "Start" button
   - Observe loading progress

4. **Verify WebGL fallback**:
   - Check console for: `"ℹ️ WebGPU not available - using WebGL 2.0 renderer"`
   - Check for: `"WebGL renderer initialized in ~40ms"`

5. **Observe FPS counter**:
   - Should display: `30-40 FPS | ~160K▲ | ~81DC`
   - FPS should be >= 30 (constitutional minimum)

6. **Test camera movement**:
   - Move: WASD keys
   - Look: Mouse movement
   - FPS should remain >= 30 with acceptable smoothness

7. **Verify visual consistency**:
   - Compare with WebGPU rendering (Chrome screenshot)
   - Colors, geometry, lighting should match
   - Allowable difference: <1% pixels

### Expected Output

**Console Log**:
```
🎮 VoxelCraft Starting...
📦 Page loaded, waiting for user to click Start...
🔧 Initializing renderer backend for VoxelCraft...
📊 Backend Negotiation:
  Detecting capabilities...
  Available backends: WebGL 2.0
  Selected: WebGL2
ℹ️ WebGPU not available - using WebGL 2.0 renderer
✅ WebGL renderer initialized in 42ms
✅ Renderer initialized!
?? Generating world...
? Terrain generation complete in 1389ms
?? Game loop started!
```

**HUD Display**:
```
FPS: 35 | 162,354▲ | 81DC
Backend: WebGL 2.0
Position: X: 0, Y: 100, Z: 0
Flight: ON
Chunks: 81 / 1024
```

### Success Criteria

✅ FPS >= 30 (sustained)
✅ Backend: WebGL 2.0
✅ No errors during fallback
✅ Visual output matches WebGPU
✅ Smooth camera movement (acceptable for 30 FPS)

---

## Scenario 3: Frustum Culling Validation

### Steps

1. **Start VoxelCraft** (either Chrome with WebGPU or Firefox with WebGL)

2. **Enable debug overlay**:
   - Press F3 key
   - Observe "Visible chunks" and "Culled chunks" counters

3. **Position camera to see all chunks**:
   - Fly up (Space) to Y=150
   - Look down at 45° angle
   - Should see most/all 81 chunks
   - Visible chunks: ~75-81
   - Culled chunks: ~0-6

4. **Rotate camera 180°**:
   - Turn around (mouse movement)
   - Now facing away from terrain
   - Visible chunks: ~40-50
   - Culled chunks: ~30-40

5. **Observe draw call reduction**:
   - HUD shows: `45 FPS | ~80K▲ | ~45DC` (approximately half)
   - Culling reduced draw calls from 81 to ~45
   - Culling efficiency: ~44% (30-50% typical)

### Expected Output

**Camera Facing Terrain** (all chunks visible):
```
FPS: 60
Draw Calls: 81
Visible Chunks: 81 / 81
Culled Chunks: 0
```

**Camera Rotated 180°** (half chunks visible):
```
FPS: 60 (improved headroom)
Draw Calls: 45
Visible Chunks: 45 / 81
Culled Chunks: 36
```

### Success Criteria

✅ Culling reduces draw calls by 30-50%
✅ Visible chunk count matches draw calls (approximately)
✅ No visual artifacts (chunks popping in/out at frustum edges)

---

## Troubleshooting

### Issue: FPS stuck at 0

**Symptoms**: Game loads, terrain generates, but FPS counter shows 0-2 FPS, screen is frozen

**Diagnosis**: Pipeline busy-wait loop blocking render thread

**Solution**: This indicates feature 018 fixes not applied. Check:
1. WebGPURenderer.kt: Pipeline creation should be async (no busy-wait loop)
2. Pipeline cache should have entries after first 10 frames
3. Console should not show "Waiting for pipeline... attempt N"

**Verification**:
```kotlin
// Should NOT see this in WebGPURenderer.kt
while (!pipelineReady && attempts < 10000) { ... }

// Should see this instead
val cached = pipelineCacheMap[key]
if (cached != null) return cached.getPipeline()
```

### Issue: Draw calls > 100

**Symptoms**: HUD shows >100 draw calls for 81 chunks

**Diagnosis**: Multiple pipelines per chunk (cache miss) or no batching

**Solution**: Check pipeline cache hit rate
- F3 debug overlay: "Pipeline Cache: X pipelines, Y% hit rate"
- Hit rate should be >95% after first 100 frames
- If <50%, pipeline key generation is broken

### Issue: WebGPU not detected in Chrome 113+

**Symptoms**: Chrome 113+ falls back to WebGL

**Diagnosis**: WebGPU disabled or system incompatibility

**Solution**:
1. Check chrome://gpu → "WebGPU Status"
2. Enable: chrome://flags/#enable-unsafe-webgpu
3. Update GPU drivers
4. Try different GPU (integrated vs dedicated)

### Issue: Visual differences between WebGPU and WebGL

**Symptoms**: Colors or geometry don't match between backends

**Diagnosis**: Shader math differences or buffer format mismatch

**Solution**: Run visual regression test
```bash
./gradlew :examples:voxelcraft:jsTest --tests "VoxelCraftVisualTest"
```

Expected: <1% pixel difference

---

## Next Steps

After completing quickstart validation:

1. **Run automated tests**:
   ```bash
   ./gradlew :src:jsTest:test --tests "WebGPURendererPerformanceTest"
   ./gradlew :src:jsTest:test --tests "WebGLFallbackPerformanceTest"
   ```

2. **Performance profiling**:
   - Open Chrome DevTools → Performance tab
   - Record 5 seconds of gameplay
   - Check for 60 FPS consistency (no dropped frames)
   - Validate frame time <16.67ms

3. **Cross-browser testing**:
   - Repeat scenarios in Edge, Firefox, Safari
   - Document FPS and draw call results
   - Report any visual inconsistencies

4. **Stress testing**:
   - Increase chunk count (modify INITIAL_GENERATION_RADIUS)
   - Measure FPS with 200+ chunks
   - Validate frustum culling effectiveness

---

**Quickstart Complete**: Feature 018 rendering optimization validated ✅
