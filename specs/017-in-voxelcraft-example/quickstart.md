# VoxelCraft Rendering Fix: Quick Start Test Guide

## Overview
This guide provides manual testing steps to verify the VoxelCraft rendering fixes. Follow these steps to validate that the 0 FPS and upside-down terrain issues are resolved.

## Prerequisites

### Build Environment
- Kotlin 1.9+ with Multiplatform plugin
- Gradle 8.0+
- Node.js (for JS target)

### Browser Requirements
- **Preferred**: Chrome/Edge with WebGPU support enabled
  - Navigate to `chrome://flags`
  - Enable "Unsafe WebGPU" flag
  - Restart browser
- **Fallback**: Any browser with WebGL 1.0+ support (Firefox, Safari)

### Project Setup
```bash
cd /home/yousef/Projects/kmp/KreeKt
git checkout 017-in-voxelcraft-example
```

## Test Scenario 1: Performance Validation (30-60 FPS)

### Objective
Verify VoxelCraft renders at minimum 30 FPS (target 60 FPS per constitutional requirement).

### Steps

1. **Build VoxelCraft**
   ```bash
   ./gradlew :examples:voxelcraft:jsBrowserDevelopmentRun
   ```

2. **Open VoxelCraft in Browser**
   - Browser should automatically open to `http://localhost:8080`
   - If not, manually navigate to that URL

3. **Click "Start Game" Button**
   - Loading screen should appear: "Starting terrain generation..."
   - Progress updates every 10%
   - Loading screen should disappear within 10 seconds

4. **Observe Rendering**
   - **Terrain should be visible** (not black screen)
   - **FPS counter** in top-left corner should show ≥30 FPS
   - **Triangle count** should show >0 (e.g., "32 FPS | 131,484▲ | 32DC")
   - **Draw calls** should show >0 (typically 32-64 for visible chunks)

5. **Move Camera**
   - Click canvas to enter pointer lock mode
   - Move mouse to look around
   - **Expected**: Terrain remains visible, FPS stays ≥30
   - **Expected**: No rendering artifacts or flickering

6. **Open Browser Console (F12)**
   - Check for logged FPS values
   - **Expected**: Console shows "32 FPS" or higher
   - **No errors** related to rendering should appear

### Success Criteria
- ✅ FPS counter shows ≥30 FPS consistently
- ✅ Triangle count >0 (visible geometry)
- ✅ Draw calls >0 (meshes being rendered)
- ✅ Terrain remains visible while moving camera
- ✅ No console errors

### Failure Indicators
- ❌ FPS shows 0
- ❌ Black screen with no terrain
- ❌ Triangle count = 0
- ❌ Console errors: "Scene traversal found 0 meshes" or "Shader compilation failed"

### Performance Benchmarks
| Target Hardware | Minimum FPS | Target FPS | Typical FPS |
|----------------|-------------|------------|-------------|
| Desktop (WebGPU) | 30 | 60 | 55-60 |
| Desktop (WebGL) | 30 | 60 | 40-55 |
| Laptop (WebGPU) | 30 | 45 | 35-50 |
| Laptop (WebGL) | 30 | 40 | 30-45 |

---

## Test Scenario 2: Visual Correctness (Terrain Orientation)

### Objective
Verify terrain renders right-side up with grass blocks on top (Y-up coordinate system).

### Steps

1. **Load VoxelCraft** (follow Scenario 1 steps 1-3)

2. **Observe Initial Camera View**
   - Default spawn: Player at Y=64
   - Camera should look horizontally or slightly down
   - **Expected**: Terrain visible below camera (grass on top, dirt/stone underneath)

3. **Identify Grass Blocks**
   - Grass blocks: Bright green color (RGB ~0.2, 0.8, 0.2)
   - **Expected**: Grass appears at TOP of terrain columns
   - **Expected**: Grass is ABOVE brown dirt blocks

4. **Check Terrain Layering**
   - Look for natural terrain features:
     - **Grass layer**: Top surface (Y ≈ 60-80)
     - **Dirt layer**: Below grass (Y ≈ 55-75)
     - **Stone layer**: Deep underground (Y < 55)
   - **Expected**: Layering appears natural (grass → dirt → stone from top to bottom)

5. **Toggle Flight Mode (Press F Key)**
   - Fly upward (Press Space)
   - Look down at terrain
   - **Expected**: Grass blocks visible on TOP of terrain
   - **Expected**: No upside-down geometry or inverted terrain

6. **Check Depth Perception**
   - Move forward (W key)
   - **Expected**: Distant chunks appear behind near chunks
   - **Expected**: No Z-fighting or depth artifacts
   - **Expected**: Face culling working (no back-faces visible)

### Success Criteria
- ✅ Grass blocks appear on TOP of terrain columns
- ✅ Terrain layering correct (grass → dirt → stone from top to bottom)
- ✅ No upside-down or inverted geometry
- ✅ Depth perception correct (near objects in front of far objects)
- ✅ No visible back-faces (face culling working)

### Failure Indicators
- ❌ Grass appears on BOTTOM of terrain
- ❌ Terrain appears inverted or flipped
- ❌ Depth issues (far objects in front of near objects)
- ❌ Visible back-faces or inside-out geometry

### Visual Reference
```
Correct Orientation (Y-up):
     +Y (up)
      │
  ████│████  ← Grass blocks (green) on top
  ████│████  ← Dirt blocks (brown) underneath
  ████│████  ← Stone blocks (gray) deep
──────┼────── +X (right)
     /
    +Z (forward)

Incorrect Orientation (inverted):
──────┼────── +X
     /│
  ████│████  ← Stone on top (WRONG)
  ████│████  ← Dirt in middle
  ████│████  ← Grass on bottom (WRONG)
      │
     -Y (up in world, but rendered down)
```

---

## Test Scenario 3: Backend Selection Verification

### Objective
Verify WebGPU is used when available, with graceful WebGL fallback.

### Steps

1. **Open Browser Console (F12)** BEFORE starting game

2. **Load VoxelCraft**
   ```bash
   ./gradlew :examples:voxelcraft:jsBrowserDevelopmentRun
   ```

3. **Click "Start Game" Button**

4. **Check Console Output** for backend selection messages

5. **Expected Console Output (WebGPU Available)**
   ```
   🎮 VoxelCraft Starting...
   📦 Page loaded, waiting for user to click Start...
   🎮 Starting game from button click...
   🌍 Initializing VoxelCraft...
   🔧 Initializing renderer backend for VoxelCraft...
   📊 Backend Negotiation:
     Detecting capabilities...
     Available backends: WebGPU 1.0
     Selected: WebGPU (via WebGL2 compatibility)
     Features:
       COMPUTE: Emulated via WebGL2
       RAY_TRACING: Not available
       XR_SURFACE: Not available
   🚀 Creating renderer with WebGPU (auto-fallback to WebGL)...
   WebGPU available - creating WebGPURenderer
   WebGPURenderer initialized successfully
   ✅ Renderer initialized!
     Init Time: ~50ms
     Within Budget: true (2000ms limit)
   ```

6. **Expected Console Output (WebGL Fallback)**
   ```
   🎮 VoxelCraft Starting...
   📦 Page loaded, waiting for user to click Start...
   🎮 Starting game from button click...
   🌍 Initializing VoxelCraft...
   🔧 Initializing renderer backend for VoxelCraft...
   📊 Backend Negotiation:
     Detecting capabilities...
     Available backends: WebGL 2.0
     Selected: WebGL2
     Features:
       COMPUTE: Not available
       RAY_TRACING: Not available
       XR_SURFACE: Not available
   🚀 Creating renderer with WebGPU (auto-fallback to WebGL)...
   WebGPU not available - using WebGL renderer
   WebGLRenderer initialized successfully (fallback)
   ✅ Renderer initialized!
     Init Time: ~50ms
     Within Budget: true (2000ms limit)
   ```

### Success Criteria
- ✅ Backend selection logged (WebGPU or WebGL)
- ✅ Initialization success logged
- ✅ Game runs correctly with selected backend
- ✅ No "UnsupportedOperationException" errors

### Failure Indicators
- ❌ No backend selection logging
- ❌ "UnsupportedOperationException: WebGL fallback not yet implemented"
- ❌ Game fails to start after backend selection

---

## Test Scenario 4: Debug Diagnostics (Advanced)

### Objective
Capture detailed diagnostic information if issues persist.

### Steps

1. **Enable Verbose Logging**
   - Open: `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`
   - Set log level to DEBUG (if available)

2. **Run VoxelCraft** with Browser DevTools open

3. **Check Console for Diagnostic Messages**
   - Chunk generation logs: "Chunk mesh: X vertices, Y triangles"
   - Scene traversal logs: "Rendering mesh: [name]"
   - Shader compilation logs: "Shader program compiled"
   - Camera position logs: "Camera pos: (X, Y, Z)"

4. **Capture WebGL Calls** (Chrome only)
   - Install "Spector.js" extension
   - Click "Capture" during rendering
   - Review captured frame for:
     - Draw calls (should be >0)
     - Active shaders
     - Bound buffers
     - Texture state

5. **Check Browser Performance Profiler**
   - Press Ctrl+Shift+I → Performance tab
   - Click Record
   - Interact with VoxelCraft for 5 seconds
   - Stop recording
   - Look for:
     - Long frames (>16.67ms for 60 FPS)
     - JavaScript bottlenecks
     - GPU bottlenecks

### Expected Diagnostic Output
```
Chunk mesh: 8234 vertices, 4117 triangles
Chunk mesh: 7891 vertices, 3945 triangles
...
🎨 Rendering mesh: Chunk_0_0, triangles: 4117
🎨 Rendering mesh: Chunk_1_0, triangles: 3945
...
📷 Camera pos: (0, 64, 0)
🧱 Scene children count: 32
✅ Rendered 32 meshes
```

### Success Criteria
- ✅ Chunk meshes generated with >0 vertices and triangles
- ✅ Scene contains >0 children (chunks)
- ✅ Meshes found during scene traversal (>0 rendered)
- ✅ Draw calls executed in WebGL capture

### Failure Indicators
- ❌ "Chunk mesh: 0 vertices, 0 triangles"
- ❌ "Scene traversal found 0 meshes"
- ❌ "Scene children count: 0"
- ❌ No draw calls in WebGL capture

---

## Troubleshooting

### Issue: Black Screen with 0 FPS
**Symptoms**: Canvas is black, FPS counter shows 0, triangle count = 0

**Diagnosis Steps**:
1. Check console for "Scene traversal found 0 meshes"
   - **Fix**: Verify meshes are added to scene via `scene.add(mesh)`
2. Check console for "Shader compilation failed"
   - **Fix**: Review shader source in WebGLRenderer.createBasicShaderProgram()
3. Check console for "Chunk mesh: 0 vertices, 0 triangles"
   - **Fix**: Debug ChunkMeshGenerator greedy meshing algorithm

### Issue: Terrain Appears Upside Down
**Symptoms**: Grass on bottom, stone on top, inverted layering

**Diagnosis Steps**:
1. Check winding order in ChunkMeshGenerator
   - **Fix**: Verify getQuadCorners() produces CCW winding for all faces
2. Check camera orientation
   - **Fix**: Verify camera.up = (0, 1, 0) and camera.lookAt() working
3. Check Y-axis in coordinate system
   - **Fix**: Verify Y-up convention consistent throughout code

### Issue: Low FPS (<30)
**Symptoms**: FPS counter shows 15-25 FPS

**Diagnosis Steps**:
1. Check visible chunk count
   - **Fix**: Reduce render distance or implement frustum culling
2. Check triangle count per frame
   - **Fix**: Optimize greedy meshing or implement LOD
3. Check draw calls
   - **Fix**: Reduce draw calls via instancing or batching

### Issue: WebGPU Not Used Despite Browser Support
**Symptoms**: Console shows "WebGL fallback" even in Chrome with WebGPU

**Diagnosis Steps**:
1. Check WebGPU flag enabled: `chrome://flags/#enable-unsafe-webgpu`
2. Check WebGPUDetector.getGPU() returns non-null
   - **Fix**: Debug GPU detection logic in WebGPURendererFactory
3. Check WebGPU initialization doesn't throw error
   - **Fix**: Debug WebGPURenderer.initialize() method

---

## Acceptance Checklist

Before marking feature complete, verify:

- [ ] **Performance**: FPS ≥ 30 consistently (✅ 60 FPS target)
- [ ] **Visual**: Terrain renders right-side up, grass on top
- [ ] **Backend**: WebGPU used when available, WebGL fallback works
- [ ] **Stats**: Triangle count >0, draw calls >0
- [ ] **Logging**: Backend selection logged to console
- [ ] **Movement**: Camera controls work, terrain stays visible
- [ ] **Depth**: Depth testing working, no Z-fighting
- [ ] **Culling**: Face culling working, no back-faces visible
- [ ] **No Errors**: No console errors during rendering
- [ ] **Consistency**: Multiple play sessions work correctly

## Expected Time to Complete
- **Scenario 1** (Performance): 5 minutes
- **Scenario 2** (Visual): 5 minutes
- **Scenario 3** (Backend): 3 minutes
- **Scenario 4** (Diagnostics): 10 minutes (if needed)
- **Total**: 15-25 minutes

## Success Confirmation

Once all acceptance criteria pass, the VoxelCraft rendering fix is complete and ready for merge to main branch.

**Final Validation**:
```bash
# Run full build
./gradlew build

# Run VoxelCraft
./gradlew :examples:voxelcraft:jsBrowserDevelopmentRun

# Verify all test scenarios pass
# Mark feature complete
```

---

*For questions or issues, refer to research.md for technical deep dive or contracts/renderer-contract.md for expected behavior specifications.*
