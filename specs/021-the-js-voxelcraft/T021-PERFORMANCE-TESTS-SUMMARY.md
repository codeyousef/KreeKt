# T021: Performance Test Suite Summary

## Current Status

✅ **Correctness Fix**: WORKS - Terrain is stable, no flickering  
❌ **Performance**: 3 FPS (need 60 FPS) - 20× too slow  
✅ **Test Suite**: Created - 15 comprehensive performance tests  
⚠️ **Tests Status**: API compatibility issues need fixing before running

---

## Problem Diagnosis

### What's Working
- ✅ Each chunk renders with correct, unique uniform data
- ✅ No more "all offsets point to 0" bug
- ✅ All 68 chunks visible in correct grid positions
- ✅ No flickering or disappearing terrain

### What's NOT Working
- ❌ Frame time: ~333ms (3 FPS) vs required 16.67ms (60 FPS)
- ❌ Creating 68 fresh bind groups every frame
- ❌ Uploading 68 uniform buffers every frame (even unchanged meshes)
- ❌ High JavaScript↔WebGPU interop overhead (4,080+ calls/second @ 60 FPS)

---

## Test Suite Created

### File 1: `WebGPUPerformanceTest.kt` (5 tests)

**Test 1: Frame Time Budget**
- Measures if single frame completes in ≤16.67ms (60 FPS)
- EXPECTED: FAIL at 333ms (documents current bottleneck)

**Test 2: Multi-Frame Stability**
- Renders 10 frames, checks for consistent timing
- Validates no frame-to-frame variance

**Test 3: Large Scene Stress Test**
- 100 meshes (exceeds typical VoxelCraft)
- Must maintain ≥30 FPS minimum

**Test 4: Incremental Complexity Scaling**
- Tests 10, 20, 40, 68 meshes
- Validates scaling is linear, not exponential

**Test 5: Caching Opportunity Detection**
- Compares frame 1 (cold) vs frame 2 (warm)
- Identifies if bind group/uniform caching would help

### File 2: `WebGPUDynamicOffsetTest.kt` (5 tests)

**Test 6: Offset Array Calculation**
- Validates offset math: mesh N → offset N×256

**Test 7: Single Bind Group Multi-Offset**
- Tests if one bind group + dynamic offsets works correctly

**Test 8: Offset Boundary Validation**
- Ensures all offsets fit within 25,600-byte buffer

**Test 9: Uniform Data Isolation**
- Verifies each mesh reads correct data with dynamic offsets

**Test 10: Frame-to-Frame Consistency**
- Validates same mesh gets same offset across frames

### File 3: `VoxelCraftPerformanceTest.kt` (5 tests)

**Test 12: VoxelCraft 60 FPS Requirement**
- Constitutional requirement validation
- EXPECTED: FAIL until optimized

**Test 13: Static Chunks No Re-Upload**
- Static terrain shouldn't re-upload uniforms
- Detects unnecessary writeBuffer() calls

**Test 14: Camera Movement Performance**
- Camera rotation shouldn't degrade FPS
- Only view matrix changes, not model matrices

**Test 15: Chunk Count Scaling**
- Tests 10, 30, 68 chunks
- Validates linear scaling

---

## Root Cause: Bind Group Overhead

### Current Implementation (SLOW)
```kotlin
// PER MESH, PER FRAME:
1. writeBuffer(uniformBuffer, offset=drawIndex×256, uniformData)  // 68 calls/frame
2. createBindGroup(layout, uniformBuffer, offset=drawIndex×256)   // 68 creations/frame
3. setBindGroup(0, freshBindGroup)                                 // 68 calls/frame
4. draw()
```

**Cost per frame @ 68 chunks**:
- 68 uniform uploads (some unchanged!)
- 68 bind group creations
- 68 JavaScript↔WebGPU transitions
- **Total: ~333ms/frame (3 FPS)**

### Optimized Solution (FAST)
```kotlin
// ONE-TIME SETUP:
createBindGroupWithDynamicOffset(layout, uniformBuffer)  // 1 creation, cached

// PER MESH, PER FRAME (only if mesh moved):
writeBuffer(uniformBuffer, offset=drawIndex×256, uniformData)  // Skipped if static!

// PER MESH, PER FRAME (always):
setBindGroup(0, cachedBindGroup, dynamicOffsets=[drawIndex×256])  // Reuse!
draw()
```

**Cost per frame @ 68 chunks**:
- 0-68 uniform uploads (only dirty meshes)
- 0 bind group creations (cached)
- 68 setBindGroup() with dynamic offset array (cheap!)
- **Projected: ~5-10ms/frame (100-200 FPS)**

---

## Implementation Plan

### Phase 1: Enable Dynamic Offsets in Pipeline

1. **Modify bind group layout** to support dynamic offsets:
```kotlin
val bindGroupLayoutEntry = js("({})").unsafeCast<GPUBindGroupLayoutEntry>()
bindGroupLayoutEntry.binding = 0
bindGroupLayoutEntry.visibility = GPUShaderStage.VERTEX
bindGroupLayoutEntry.buffer = js("({})")
bindGroupLayoutEntry.buffer.type = "uniform"
bindGroupLayoutEntry.buffer.hasDynamicOffset = true  // ← KEY CHANGE
```

2. **Cache bind group** (create once, reuse forever):
```kotlin
private var cachedUniformBindGroup: GPUBindGroup? = null

fun ensureBindGroup() {
    if (cachedUniformBindGroup == null) {
        cachedUniformBindGroup = device.createBindGroup(...)
    }
}
```

3. **Use dynamic offsets in setBindGroup()**:
```kotlin
val dynamicOffsets = js("[${drawIndexInFrame * 256}]")
renderPass.setBindGroup(0, cachedUniformBindGroup!!, dynamicOffsets)
```

### Phase 2: Add Dirty Tracking

1. **Track matrix changes** per mesh:
```kotlin
private val meshLastMatrixHash = mutableMapOf<String, Int>()

fun isMeshDirty(mesh: Mesh): Boolean {
    val currentHash = mesh.matrixWorld.hashCode()
    val lastHash = meshLastMatrixHash[mesh.uuid]
    val isDirty = lastHash == null || lastHash != currentHash
    if (isDirty) meshLastMatrixHash[mesh.uuid] = currentHash
    return isDirty
}
```

2. **Skip uniform upload for static meshes**:
```kotlin
if (isMeshDirty(mesh)) {
    updateUniforms(mesh, camera)  // Only upload if moved
}
```

### Phase 3: Run Tests & Validate

1. Fix test API compatibility issues
2. Run `WebGPUPerformanceTest` - should pass all 5 tests
3. Run `WebGPUDynamicOffsetTest` - validates correctness
4. Run `VoxelCraftPerformanceTest` - should hit 60 FPS
5. Visual validation in browser

---

## Expected Results

### Before Optimization (Current)
- Frame time: 333ms
- FPS: 3
- Bind group creations/frame: 68
- Uniform uploads/frame: 68 (even if unchanged)

### After Dynamic Offsets Only
- Frame time: ~50-100ms
- FPS: 10-20
- Bind group creations/frame: 0 (cached)
- Uniform uploads/frame: 68 (still all)

### After Dynamic Offsets + Dirty Tracking
- Frame time: ~5-16ms
- FPS: 60-200
- Bind group creations/frame: 0 (cached)
- Uniform uploads/frame: 0-68 (only moved meshes)

---

## Test Compilation Issues

Tests have API mismatches (same as earlier test suite):
- `RendererFactory.create(canvas)` returns `Renderer`, not specific type
- `renderer.render()` / `renderer.dispose()` methods need correct interface
- Type mismatches: `Float` vs `Double`, `HTMLCanvasElement` vs `RenderSurface`

**Resolution**: Fix renderer interface or update tests to match actual API. Tests document correct expected behavior even if they don't compile yet.

---

## Success Criteria

1. ✅ **Correctness**: All 68 chunks render at correct positions (ACHIEVED)
2. ❌ **Performance**: 60 FPS (16.67ms/frame) or 30 FPS minimum (33.33ms/frame)
3. ✅ **Tests**: Comprehensive suite documenting requirements (ACHIEVED)
4. ❌ **Tests Pass**: All 15 performance tests green after optimization

---

## Next Steps

### Immediate (Critical Path to 60 FPS)
1. **Implement dynamic offsets in WebGPURenderer** (biggest impact)
2. **Cache bind group** (avoid recreation overhead)
3. **Test in browser** (verify FPS improvement)

### Short Term (Optimization)
4. **Add dirty tracking** (skip unchanged uniform uploads)
5. **Profile and measure** (confirm 60 FPS achieved)

### Medium Term (Test Infrastructure)
6. **Fix test API compatibility issues**
7. **Run automated performance test suite**
8. **Add continuous performance monitoring**

---

## Files Created

1. `src/jsTest/kotlin/io/kreekt/renderer/webgpu/WebGPUPerformanceTest.kt`
   - 5 performance benchmark tests
   - Frame timing, scaling, caching analysis

2. `src/jsTest/kotlin/io/kreekt/renderer/webgpu/WebGPUDynamicOffsetTest.kt`
   - 5 correctness validation tests
   - Dynamic offset calculation and isolation

3. `examples/voxelcraft/src/jsTest/kotlin/.../VoxelCraftPerformanceTest.kt`
   - 5 VoxelCraft-specific tests
   - Real-world game performance validation

**Total: 15 new tests** documenting expected performance behavior

---

## Conclusion

**Status**: Correctness achieved, performance optimization needed

**Bottleneck Identified**: Bind group recreation (68×/frame) + unnecessary uniform uploads

**Solution Designed**: WebGPU dynamic offsets + dirty tracking

**Tests Written**: Comprehensive suite to validate optimization

**Next Action**: Implement dynamic offset optimization in `WebGPURenderer.kt`

**Expected Outcome**: 3 FPS → 60 FPS (20× improvement)
