# T021: Uniform Buffer Fix - Implementation Summary

## Problem Diagnosed

**Root Cause**: All mesh uniforms were being written to offset 0 of a single 256-byte buffer, causing WebGPU command batching to only preserve the last mesh's data when GPU executed commands.

**Symptoms**:
- 68 VoxelCraft chunks all rendering with the same (wrong) transform
- Flickering terrain as depth tests failed
- Random appearance/disappearance of rectangular terrain sections
- All chunks appeared to be in the same location or overlapping

## Solution Implemented

### Changes Made to `WebGPURenderer.kt`

1. **Added Draw Index Counter** (Line 72):
   ```kotlin
   private var drawIndexInFrame = 0
   ```
   - Tracks which mesh is being drawn in current frame
   - Reset to 0 at start of each frame
   - Incremented after each mesh draw

2. **Increased Uniform Buffer Size** (Line 678):
   ```kotlin
   size = 256 * 100  // 256 bytes per mesh × 100 meshes
   ```
   - Changed from 256 bytes (1 mesh) to 25,600 bytes (100 meshes)
   - Supports typical VoxelCraft scene (~70 chunks) with headroom

3. **Unique Offset Per Mesh** (Line 663):
   ```kotlin
   val offset = drawIndexInFrame * 256
   uniformBuffer?.upload(uniformData, offset)
   ```
   - Each mesh writes uniforms to its own 256-byte region
   - Offset calculation: mesh index × 256 bytes

4. **Bind Group Unique Offsets** (Line 708):
   ```kotlin
   bufferBinding.offset = drawIndexInFrame * 256
   ```
   - Each bind group points to unique uniform buffer region
   - GPU reads correct transform for each mesh

5. **Reset Counter Each Frame** (Line 253):
   ```kotlin
   drawIndexInFrame = 0  // Reset at start of render()
   ```

6. **Increment After Each Draw** (Line 406):
   ```kotlin
   drawIndexInFrame++  // Increment after each renderMesh()
   ```

## Expected Results

### Before Fix:
- All 68 chunks rendered with same transform (last chunk's data)
- Flickering as chunks fought for same depth values
- Terrain appeared sideways or in wrong positions
- Random visual glitches

### After Fix:
- Each chunk renders with its own correct transform
- Stable grid layout at (chunkX×16, 0, chunkZ×16)
- No flickering or position instability
- All 68 chunks visible simultaneously in correct positions

## Testing Instructions

### 1. Run VoxelCraft Example
```bash
cd D:\Projects\KMP\KreeKt
./gradlew :examples:voxelcraft:jsBrowserDevelopmentWebpack
```

### 2. Open Browser
Navigate to: `http://localhost:8080` (or port shown in console)

### 3. Verify Fix
**Expected Behavior**:
- ✅ Terrain generates in stable grid pattern
- ✅ No rectangles appearing/disappearing
- ✅ No flickering chunks
- ✅ All chunks visible at correct positions
- ✅ Camera movement smooth (WASD + mouse)
- ✅ Chunks remain stable across frames

**Inspect Console Logs**:
```
T021 Frame 0: updateUniforms called, drawCallCount=0
T021 Model matrix: 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, -16, 0, -16, 1
T021 Model matrix: 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, -16, 1
T021 Model matrix: 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 16, 0, -16, 1
```
- Each chunk should have unique translation (X, Y, Z values in last column)
- No more "all offsets point to 0" logs

### 4. Performance Check
- Frame rate: Should be stable 60 FPS (or near)
- Draw calls: ~68 (one per chunk)
- No console errors or warnings

## Test Suite Created

### Core Library Tests
**File**: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/WebGPURendererUniformTest.kt`

Tests validate:
- Multiple meshes have isolated uniforms
- Uniform buffer supports multiple meshes without overwrites
- Frame-to-frame consistency
- Large scene handling (60+ meshes)

### VoxelCraft Tests  
**File**: `examples/voxelcraft/src/jsTest/kotlin/io/kreekt/examples/voxelcraft/VoxelCraftBasicTest.kt`

Tests validate:
- Chunks created at correct positions
- No duplicate chunks
- Chunk world coordinate mapping correct
- Chunks contain block data

**Note**: Tests document expected behavior but some have API compatibility issues and need refinement to run. The visual test in browser is the primary validation method.

## Technical Details

### WebGPU Command Batching Explained

**Before Fix**:
```javascript
// Frame rendering pseudo-code
mesh1: writeBuffer(uniformBuffer, offset=0, mesh1Data)
mesh1: setBindGroup(bindGroup pointing to offset=0)
mesh1: draw()

mesh2: writeBuffer(uniformBuffer, offset=0, mesh2Data)  // OVERWRITES!
mesh2: setBindGroup(bindGroup pointing to offset=0)      // SAME LOCATION!
mesh2: draw()

...all commands queued...

queue.submit()  // GPU executes: uniform buffer contains only mesh68's data!
                // All 68 draws use the same (wrong) uniforms
```

**After Fix**:
```javascript
drawIndexInFrame = 0  // Reset each frame

mesh1: writeBuffer(uniformBuffer, offset=0, mesh1Data)
mesh1: setBindGroup(bindGroup pointing to offset=0)
mesh1: draw()
drawIndexInFrame++  // Now 1

mesh2: writeBuffer(uniformBuffer, offset=256, mesh2Data)  // UNIQUE OFFSET!
mesh2: setBindGroup(bindGroup pointing to offset=256)      // UNIQUE LOCATION!
mesh2: draw()
drawIndexInFrame++  // Now 2

queue.submit()  // GPU executes: each draw reads its own correct uniforms
```

### Memory Layout

**Uniform Buffer Structure (25,600 bytes total)**:
```
Offset 0:    Mesh 0   - Projection(64) + View(64) + Model(64) = 192 bytes, padded to 256
Offset 256:  Mesh 1   - Projection(64) + View(64) + Model(64) = 192 bytes, padded to 256
Offset 512:  Mesh 2   - ...
...
Offset 25,344: Mesh 99 - Last supported mesh
```

Each mesh gets its own 256-byte aligned region to satisfy WebGPU uniform buffer alignment requirements.

## Files Modified

- `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt`
  - Added `drawIndexInFrame` property
  - Modified `createUniformBuffer()` - increased size
  - Modified `updateUniforms()` - write to unique offset
  - Modified `createBindGroupForPipeline()` - bind to unique offset
  - Modified `render()` - reset counter
  - Modified `renderMesh()` - increment counter

## Files Created

- `src/jsTest/kotlin/io/kreekt/renderer/webgpu/WebGPUTestHelpers.kt`
- `src/jsTest/kotlin/io/kreekt/renderer/webgpu/WebGPURendererUniformTest.kt`
- `examples/voxelcraft/src/jsTest/kotlin/io/kreekt/examples/voxelcraft/VoxelCraftBasicTest.kt`

## Next Steps

1. ✅ **Build completed successfully**
2. **Visual Test Required**: Run the example and verify terrain is stable
3. **Refineme tests**: Fix API compatibility issues in test suite
4. **Run tests**: Validate with automated tests once API issues resolved
5. **Performance Profile**: Measure 60 FPS compliance
6. **Commit Changes**: If visual test passes

## Success Criteria

- [x] Code compiles successfully
- [ ] Terrain renders in stable grid
- [ ] No flickering or disappearing chunks  
- [ ] All 68 chunks visible with correct transforms
- [ ] Console logs show unique model matrices per chunk
- [ ] Frame rate stable at 60 FPS

---

**Status**: Implementation complete, awaiting visual verification  
**Date**: 2025-01-XX  
**Task**: T021 - Fix VoxelCraft JS terrain rendering issues
