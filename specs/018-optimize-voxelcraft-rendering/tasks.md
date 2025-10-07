# Tasks: Optimize VoxelCraft Rendering with WebGPU/Vulkan GPU Acceleration

**Feature**: 018-optimize-voxelcraft-rendering
**Branch**: `018-optimize-voxelcraft-rendering`
**Input**: Design documents from `/home/yousef/Projects/kmp/KreeKt/specs/018-optimize-voxelcraft-rendering/`

## Execution Flow (main)
```
1. Load plan.md → ✅ Loaded (Kotlin Multiplatform, WebGPU/WebGL, 60 FPS target)
2. Load design documents → ✅ Loaded (research.md, data-model.md, contracts/, quickstart.md)
3. Generate tasks by category → ✅ 27 tasks across 6 categories
4. Apply task rules → ✅ TDD order, parallel [P] where applicable
5. Number tasks sequentially → ✅ T001-T027
6. Validate completeness → ✅ All contracts, entities, tests covered
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- All file paths are absolute from repository root: `/home/yousef/Projects/kmp/KreeKt/`

## Constitutional Requirements
- ✅ 60 FPS with WebGPU (constitutional target)
- ✅ 30 FPS minimum with WebGL (constitutional requirement)
- ✅ <100 draw calls for 81 chunks
- ✅ <2000ms renderer initialization
- ✅ TDD: Tests written before implementation

---

## Phase 1: Contract Tests (TDD) ⚠️ CRITICAL

**MUST COMPLETE BEFORE PHASE 2 IMPLEMENTATION**

These tests validate functional requirements and MUST FAIL initially.

### T001 [P]: Write WebGPU Renderer Performance Tests

**Category**: Contract Tests
**Priority**: Critical
**Dependencies**: None
**Estimated Time**: 3 hours
**Files Created**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsTest/kotlin/io/kreekt/renderer/webgpu/WebGPURendererPerformanceTest.kt`

**Objective**: Write contract tests validating FR-002 (60 FPS), FR-005 (<100 draw calls), FR-008 (frustum culling).

**Test Cases**:
1. `test_FR002_webgpu_renders_at_60fps_with_81_chunks()`
   - Setup: Initialize WebGPU renderer, load 81 chunks
   - Measure FPS over 2 seconds (120 frames minimum)
   - Assert: avgFPS >= 60.0f

2. `test_FR005_draw_calls_under_100_for_81_chunks()`
   - Setup: Render frame with 81 chunks
   - Count draw calls from renderer stats
   - Assert: drawCalls < 100

3. `test_FR008_frustum_culling_reduces_draw_calls()`
   - Setup: Position camera to see 40 chunks (half visible)
   - Render with frustum culling enabled
   - Assert: drawCalls approximately 40-50 (not 81)
   - Assert: culledChunks > 30

**Acceptance Criteria**:
- All 3 tests written
- Tests compile successfully
- Tests FAIL with current implementation (0 FPS)
- Clear assertion failure messages

---

### T002 [P]: Write WebGL Fallback Performance Tests

**Category**: Contract Tests
**Priority**: Critical
**Dependencies**: None
**Estimated Time**: 2 hours
**Files Created**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsTest/kotlin/io/kreekt/renderer/webgl/WebGLFallbackPerformanceTest.kt`

**Objective**: Write contract tests validating FR-003 (30 FPS minimum), FR-010 (graceful fallback).

**Test Cases**:
1. `test_FR003_webgl_renders_at_30plus_fps_with_81_chunks()`
   - Setup: Force WebGL renderer (disable WebGPU detection)
   - Load 81 chunks
   - Measure FPS over 2 seconds
   - Assert: avgFPS >= 30.0f

2. `test_FR010_webgpu_failure_falls_back_to_webgl()`
   - Setup: Simulate WebGPU init failure
   - Assert: WebGL renderer initializes successfully
   - Assert: No errors logged
   - Verify: Game state preserved (player position, chunks)

**Acceptance Criteria**:
- Both tests written
- Tests compile
- Tests FAIL (WebGL also at 0 FPS currently)
- Fallback test validates graceful degradation

---

### T003 [P]: Write VoxelCraft Integration Tests

**Category**: Integration Tests
**Priority**: High
**Dependencies**: None
**Estimated Time**: 3 hours
**Files Created**:
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsTest/kotlin/io/kreekt/examples/voxelcraft/VoxelCraftPerformanceTest.kt`

**Objective**: Write end-to-end integration tests validating VoxelCraft 60 FPS rendering.

**Test Cases**:
1. `test_voxelcraft_renders_81_chunks_at_60fps_with_webgpu()`
   - Setup: Initialize VoxelCraft with WebGPU
   - Generate 81 chunks (full terrain)
   - Run game loop for 5 seconds
   - Measure FPS every frame
   - Assert: avgFPS >= 60.0f
   - Assert: minFPS >= 55.0f (allow brief dips)

2. `test_block_breaking_updates_mesh_without_frame_drops()`
   - Setup: Render scene at 60 FPS baseline
   - Break block (trigger mesh regeneration)
   - Measure FPS during and after update
   - Assert: FPS stays >= 55.0f (5 FPS tolerance)
   - Assert: Mesh updates within 1 frame

**Acceptance Criteria**:
- Integration tests run full VoxelCraft initialization
- Tests measure real-world performance
- Tests FAIL (0 FPS)
- Clear performance metrics logged

---

### T004 [P]: Write Pipeline Caching Tests

**Category**: Contract Tests
**Priority**: High
**Dependencies**: None
**Estimated Time**: 2 hours
**Files Created**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsTest/kotlin/io/kreekt/renderer/webgpu/WebGPUPipelineCacheTest.kt`

**Objective**: Write tests validating FR-006 (pipeline caching).

**Test Cases**:
1. `test_FR006_pipeline_cache_returns_existing_pipeline()`
   - Setup: Create renderer, render mesh twice with same geometry+material
   - First call: Pipeline created (cache miss)
   - Second call: Pipeline retrieved from cache (cache hit)
   - Assert: Same pipeline instance returned
   - Assert: Cache hit count incremented

2. `test_pipeline_cache_synchronous_access()`
   - Setup: Create pipeline, add to cache
   - Measure: `getOrCreatePipeline()` execution time
   - Assert: Lookup time < 1ms (synchronous)
   - Assert: No await/suspend in render loop

3. `test_pipeline_cache_hit_rate_over_95_percent()`
   - Setup: Render 100 frames with same meshes
   - Track: Cache hits and misses
   - Assert: Hit rate > 0.95 (95%)

**Acceptance Criteria**:
- Tests validate caching behavior
- Tests FAIL (no caching currently)
- Performance assertions included

---

### T005 [P]: Write Buffer Management Tests

**Category**: Contract Tests
**Priority**: High
**Dependencies**: None
**Estimated Time**: 2 hours
**Files Created**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsTest/kotlin/io/kreekt/renderer/webgpu/WebGPUBufferManagementTest.kt`

**Objective**: Write tests validating FR-007 (async upload), FR-013 (dynamic updates), FR-014 (cleanup).

**Test Cases**:
1. `test_FR007_geometry_upload_async_non_blocking()`
   - Setup: Upload large geometry (10K vertices)
   - Measure: Render loop continues during upload
   - Assert: FPS >= 60 during upload
   - Assert: Upload completes within 100ms

2. `test_FR013_update_geometry_without_rebuild()`
   - Setup: Render mesh, update single block
   - Assert: Only affected chunk buffers updated
   - Assert: FPS stays >= 55
   - Assert: No full scene rebuild

3. `test_FR014_dispose_cleans_up_buffers()`
   - Setup: Create 100 geometry buffers
   - Call: `renderer.dispose()`
   - Assert: All GPU buffers released
   - Assert: No memory leaks (check buffer count)

**Acceptance Criteria**:
- Buffer lifecycle tests written
- Tests FAIL (no buffer reuse currently)
- Memory cleanup validated

---

## Phase 2: Core WebGPU Fixes

**ONLY START AFTER PHASE 1 TESTS FAIL**

### T006: Fix WebGPU Pipeline Creation ✅ COMPLETED

**Category**: Core Fixes
**Priority**: Critical
**Dependencies**: T001, T004 (tests must exist and fail)
**Estimated Time**: 3 hours
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt` (lines 496-564)

**Objective**: Replace busy-wait pipeline creation with async/await pattern to prevent render loop blocking.

**Implementation Steps**:
1. Remove busy-wait loop (lines 539-545 in WebGPURenderer.kt)
2. Modify `getOrCreatePipeline()` to return cached pipeline immediately if available
3. For cache miss, launch async pipeline creation in background coroutine
4. Return null if pipeline not ready (skip mesh this frame, will render next frame)
5. Add pipeline pre-compilation during renderer initialization for common materials
6. Update WebGPUPipeline.create() to properly await pipeline creation

**Code Changes**:
```kotlin
// WebGPURenderer.kt:496-564
fun getOrCreatePipeline(geometry: BufferGeometry, material: Material): GPURenderPipeline? {
    val key = PipelineKey.from(geometry, material)

    // Synchronous cache lookup
    pipelineCacheMap[key]?.let {
        if (it.isReady) return it.getPipeline()
    }

    // Launch async creation if not exists
    if (!pipelineCacheMap.containsKey(key)) {
        val pipeline = WebGPUPipeline(device!!, createDescriptor(geometry, material))
        pipelineCacheMap[key] = pipeline

        GlobalScope.launch {
            when (val result = pipeline.create()) {
                is RendererResult.Success -> {
                    console.log("Pipeline ready for key: $key")
                }
                is RendererResult.Error -> {
                    console.error("Pipeline creation failed: ${result.exception.message}")
                    pipelineCacheMap.remove(key)
                }
            }
        }
    }

    // Return null if not ready (mesh skipped this frame)
    return pipelineCacheMap[key]?.takeIf { it.isReady }?.getPipeline()
}
```

**Acceptance Criteria**:
- T001 test `test_FR002_webgpu_renders_at_60fps` PASSES
- No busy-wait loops in render path
- Pipeline creation doesn't block frame rendering
- Graceful handling of pipeline creation failures
- Console logs show pipelines compiling in background

---

### T007: Implement Synchronous Pipeline Cache

**Category**: Core Fixes
**Priority**: Critical
**Dependencies**: T006 (pipeline creation must be async-compatible)
**Estimated Time**: 2 hours
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt` (lines 50, 64)

**Objective**: Implement proper synchronous pipeline cache with PipelineKey hashing.

**Implementation Steps**:
1. Enhance `PipelineKey` data class with proper `hashCode()` and `equals()`:
   ```kotlin
   data class PipelineKey(
       val vertexFormat: String,   // "pos3_norm3_col3_uv2"
       val indexFormat: String?,   // "uint32" or null
       val materialType: String,   // "MeshBasicMaterial"
       val materialHash: Int       // material.hashCode()
   ) {
       companion object {
           fun from(geometry: BufferGeometry, material: Material): PipelineKey {
               val vertexFormat = buildVertexFormat(geometry)
               val indexFormat = geometry.index?.let { "uint32" }
               return PipelineKey(
                   vertexFormat,
                   indexFormat,
                   material::class.simpleName ?: "unknown",
                   material.hashCode()
               )
           }

           private fun buildVertexFormat(geometry: BufferGeometry): String {
               val attrs = mutableListOf<String>()
               if (geometry.hasAttribute("position")) attrs.add("pos3")
               if (geometry.hasAttribute("normal")) attrs.add("norm3")
               if (geometry.hasAttribute("color")) attrs.add("col3")
               if (geometry.hasAttribute("uv")) attrs.add("uv2")
               return attrs.joinToString("_")
           }
       }
   }
   ```

2. Update `pipelineCacheMap` to use PipelineKey properly
3. Add cache statistics tracking (hits, misses, size)
4. Implement `getCacheStats()` method for debugging

**Acceptance Criteria**:
- T004 test `test_FR006_pipeline_cache_returns_existing_pipeline` PASSES
- T004 test `test_pipeline_cache_hit_rate_over_95_percent` PASSES after 100 frames
- Cache lookup is O(1) synchronous
- Cache statistics logged on demand

---

### T008: Optimize Buffer Management with Bind Groups

**Category**: Core Fixes
**Priority**: High
**Dependencies**: T006 (renderer must be functional)
**Estimated Time**: 3 hours
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt` (lines 388-492, 720-727)

**Objective**: Implement persistent bind groups (WebGPU's VAO equivalent) for efficient buffer binding.

**Implementation Steps**:
1. Update `GeometryBuffers` data class to include bind group:
   ```kotlin
   private data class GeometryBuffers(
       val vertexBuffer: GPUBuffer,
       val indexBuffer: GPUBuffer?,
       val bindGroup: GPUBindGroup,  // NEW: Cache bind group
       val vertexCount: Int,
       val indexCount: Int,
       val indexFormat: String
   )
   ```

2. Create bind group during geometry buffer creation:
   ```kotlin
   private fun createGeometryBindGroup(vertexBuffer: GPUBuffer, uniformBuffer: GPUBuffer): GPUBindGroup {
       val descriptor = js("({})").unsafeCast<GPUBindGroupDescriptor>()
       descriptor.layout = pipelineBindGroupLayout

       val entries = arrayOf(
           createBindGroupEntry(0, vertexBuffer),
           createBindGroupEntry(1, uniformBuffer)
       )
       descriptor.entries = entries

       return device!!.createBindGroup(descriptor)
   }
   ```

3. Update `renderMesh()` to use bind group:
   ```kotlin
   renderPass.setBindGroup(0, buffers.bindGroup)
   renderPass.setVertexBuffer(0, buffers.vertexBuffer)
   ```

4. Ensure bind groups are disposed when geometry is removed

**Acceptance Criteria**:
- T005 test `test_FR007_geometry_upload_async_non_blocking` PASSES
- Bind groups created once per geometry
- Render loop uses `setBindGroup()` instead of setting attributes individually
- Performance improvement measurable (3-5ms per frame)

---

### T009: Add Frustum Culling System

**Category**: Core Fixes
**Priority**: High
**Dependencies**: T006 (renderer must achieve 60 FPS first)
**Estimated Time**: 4 hours
**Files Created**:
- `/home/yousef/Projects/kmp/KreeKt/src/commonMain/kotlin/io/kreekt/renderer/Frustum.kt`
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt` (render method)
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Chunk.kt`

**Objective**: Implement view frustum culling to reduce draw calls for off-screen chunks.

**Implementation Steps**:
1. Create `Frustum` class in commonMain:
   ```kotlin
   class Frustum {
       private val planes = Array(6) { Plane() }

       fun setFromProjectionMatrix(matrix: Matrix4) {
           val m = matrix.elements
           // Extract 6 planes using Gribb/Hartmann method
           planes[0].setComponents(m[3] + m[0], m[7] + m[4], m[11] + m[8], m[15] + m[12]).normalize()
           planes[1].setComponents(m[3] - m[0], m[7] - m[4], m[11] - m[8], m[15] - m[12]).normalize()
           planes[2].setComponents(m[3] + m[1], m[7] + m[5], m[11] + m[9], m[15] + m[13]).normalize()
           planes[3].setComponents(m[3] - m[1], m[7] - m[5], m[11] - m[9], m[15] - m[13]).normalize()
           planes[4].setComponents(m[3] + m[2], m[7] + m[6], m[11] + m[10], m[15] + m[14]).normalize()
           planes[5].setComponents(m[3] - m[2], m[7] - m[6], m[11] - m[10], m[15] - m[14]).normalize()
       }

       fun intersectsBox(box: Box3): Boolean {
           for (plane in planes) {
               if (plane.distanceToPoint(box.getPositiveVertex(plane.normal)) < 0) {
                   return false
               }
           }
           return true
       }
   }
   ```

2. Add `boundingBox` property to Chunk:
   ```kotlin
   data class Chunk(
       val position: ChunkPosition,
       val boundingBox: Box3 = calculateBoundingBox(position)
   ) {
       companion object {
           fun calculateBoundingBox(pos: ChunkPosition): Box3 {
               val worldX = pos.toWorldX().toFloat()
               val worldZ = pos.toWorldZ().toFloat()
               return Box3(
                   min = Vector3(worldX, 0f, worldZ),
                   max = Vector3(worldX + 16f, 256f, worldZ + 16f)
               )
           }
       }
   }
   ```

3. Update WebGPURenderer.render() to cull chunks:
   ```kotlin
   override fun render(scene: Scene, camera: Camera): RendererResult<Unit> {
       val frustum = Frustum()
       frustum.setFromProjectionMatrix(camera.projectionViewMatrix)

       var culledCount = 0
       var visibleCount = 0

       scene.traverse { obj ->
           if (obj is Mesh) {
               val chunk = obj.userData["chunk"] as? Chunk
               if (chunk != null && !frustum.intersectsBox(chunk.boundingBox)) {
                   culledCount++
                   return@traverse // Skip this mesh
               }
               visibleCount++
               renderMesh(obj, camera, renderPass)
           }
       }

       console.log("Frustum culling: $visibleCount visible, $culledCount culled")
   }
   ```

**Acceptance Criteria**:
- T001 test `test_FR008_frustum_culling_reduces_draw_calls` PASSES
- Culling reduces draw calls by 30-50% when camera sees half the world
- CPU overhead < 0.2ms (frustum update + 81 AABB tests)
- Console logs show visible/culled chunk counts

---

### T010: Optimize Draw Call Tracking (No Batching)

**Category**: Core Fixes
**Priority**: Low
**Dependencies**: T009 (culling must work first)
**Estimated Time**: 1 hour
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt` (getStats method)

**Objective**: Ensure draw call tracking is accurate and meets FR-005 (<100 calls for 81 chunks).

**Implementation Steps**:
1. Verify `drawCallCount` increments correctly in `renderMesh()`
2. Add validation that drawCallCount < 100 in debug builds
3. Log warning if drawCallCount exceeds 100
4. Update performance metrics to include draw call info

**Note**: Based on research.md, draw call batching is NOT needed yet. Current 81 draw calls meets FR-005 (<100). Frustum culling will reduce this further. Defer batching to future optimization.

**Acceptance Criteria**:
- T001 test `test_FR005_draw_calls_under_100_for_81_chunks` PASSES
- Draw call count accurately reflects actual GPU draw calls
- With frustum culling, typical draw calls: 40-50 (well under 100 limit)

---

## Phase 3: WebGL Optimization (Parallel with Phase 2)

**CAN RUN IN PARALLEL WITH T006-T010**

### T011 [P]: Add VAO Support to WebGL Renderer

**Category**: WebGL Optimization
**Priority**: High
**Dependencies**: T002 (test must exist and fail)
**Estimated Time**: 3 hours
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt` (setupGeometry method, GeometryBuffers data class)

**Objective**: Implement WebGL 2.0 VAO for efficient vertex attribute setup.

**Implementation Steps**:
1. Update `WebGLGeometryBuffers` data class to include VAO:
   ```kotlin
   private data class WebGLGeometryBuffers(
       val vertexBuffer: WebGLBuffer,
       val indexBuffer: WebGLBuffer?,
       val vao: WebGLVertexArrayObject,  // NEW: Cache VAO
       val vertexCount: Int,
       val indexCount: Int
   )
   ```

2. Create VAO during geometry setup:
   ```kotlin
   private fun createVAO(
       gl: WebGL2RenderingContext,
       vertexBuffer: WebGLBuffer,
       indexBuffer: WebGLBuffer?,
       program: WebGLProgram
   ): WebGLVertexArrayObject {
       val vao = gl.createVertexArray()!!
       gl.bindVertexArray(vao)

       // Bind vertex buffer
       gl.bindBuffer(GL.ARRAY_BUFFER, vertexBuffer)

       // Setup attributes (using cached locations)
       val posLoc = getOrCacheAttribLocation(gl, program, "aPosition")
       gl.enableVertexAttribArray(posLoc)
       gl.vertexAttribPointer(posLoc, 3, GL.FLOAT, false, 36, 0)

       val normLoc = getOrCacheAttribLocation(gl, program, "aNormal")
       gl.enableVertexAttribArray(normLoc)
       gl.vertexAttribPointer(normLoc, 3, GL.FLOAT, false, 36, 12)

       val colorLoc = getOrCacheAttribLocation(gl, program, "aColor")
       gl.enableVertexAttribArray(colorLoc)
       gl.vertexAttribPointer(colorLoc, 3, GL.FLOAT, false, 36, 24)

       // Bind index buffer (part of VAO state)
       if (indexBuffer != null) {
           gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)
       }

       gl.bindVertexArray(null) // Unbind
       return vao
   }
   ```

3. Update render loop to use VAO:
   ```kotlin
   private fun renderMesh(mesh: Mesh, camera: Camera, gl: WebGL2RenderingContext) {
       val buffers = getOrCreateGeometryBuffers(geometry)

       // Bind VAO (sets all vertex attributes + index buffer)
       gl.bindVertexArray(buffers.vao)

       // Update uniforms
       updateUniforms(mesh, camera, gl)

       // Draw
       if (buffers.indexBuffer != null) {
           gl.drawElements(GL.TRIANGLES, buffers.indexCount, GL.UNSIGNED_INT, 0)
       } else {
           gl.drawArrays(GL.TRIANGLES, 0, buffers.vertexCount)
       }

       gl.bindVertexArray(null)
   }
   ```

**Acceptance Criteria**:
- T002 test `test_FR003_webgl_renders_at_30plus_fps` PASSES
- VAO created once per geometry
- Render loop uses `bindVertexArray()` instead of setting attributes per frame
- Performance improvement: 0 FPS → 30+ FPS

---

### T012 [P]: Enhance Attribute Location Caching

**Category**: WebGL Optimization
**Priority**: Medium
**Dependencies**: T011 (VAO must exist)
**Estimated Time**: 1 hour
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt` (getOrCacheAttribLocation method)

**Objective**: Verify and enhance existing attribute location caching implementation.

**Implementation Steps**:
1. Review existing `getOrCacheAttribLocation()` method (lines 282-297)
2. Ensure it's called only during VAO creation, not during render loop
3. Add cache statistics logging
4. Validate no `getAttribLocation()` calls during render loop

**Verification**:
```kotlin
// Should be called ONLY during VAO creation
private fun getOrCacheAttribLocation(gl: WebGLRenderingContext, program: WebGLProgram, name: String): Int {
    if (cachedAttribLocations == null) {
        cachedAttribLocations = mapOf(
            "aPosition" to gl.getAttribLocation(program, "aPosition"),
            "aNormal" to gl.getAttribLocation(program, "aNormal"),
            "aColor" to gl.getAttribLocation(program, "aColor"),
            "aUV" to gl.getAttribLocation(program, "aUV")
        )
        console.log("✅ Cached attribute locations: ${cachedAttribLocations}")
    }
    return cachedAttribLocations!![name] ?: -1
}
```

**Acceptance Criteria**:
- Attribute locations cached once per program
- No `getAttribLocation()` calls in render loop
- Cache hit rate logged (should be 100% after first frame)

---

### T013 [P]: Optimize Index Buffer Handling

**Category**: WebGL Optimization
**Priority**: Low
**Dependencies**: T011 (VAO must exist)
**Estimated Time**: 1 hour
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt` (setupGeometry method)

**Objective**: Ensure index buffers are created efficiently without per-frame conversion.

**Implementation Steps**:
1. Verify index buffer creation happens once during geometry setup
2. Ensure no index data conversion in render loop
3. Validate index buffer is bound as part of VAO state
4. Add debug logging for index buffer creation

**Acceptance Criteria**:
- Index buffers created once per geometry
- No per-frame index data processing
- Index buffer binding via VAO (not manual)

---

## Phase 4: Performance Metrics (Parallel with Phase 2-3)

**CAN RUN IN PARALLEL WITH T006-T013**

### T014 [P]: Add FPS Measurement System

**Category**: Performance Metrics
**Priority**: High
**Dependencies**: None
**Estimated Time**: 2 hours
**Files Created**:
- `/home/yousef/Projects/kmp/KreeKt/src/commonMain/kotlin/io/kreekt/renderer/FPSCounter.kt`
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt` (gameLoop function)

**Objective**: Implement accurate FPS measurement using rolling average.

**Implementation Steps**:
1. Create FPSCounter class:
   ```kotlin
   class FPSCounter(private val windowSize: Int = 60) {
       private val frameTimes = mutableListOf<Double>()
       private var lastTime = 0.0

       fun update(currentTime: Double): Float {
           if (lastTime > 0) {
               val deltaTime = currentTime - lastTime
               frameTimes.add(deltaTime)
               if (frameTimes.size > windowSize) {
                   frameTimes.removeAt(0)
               }
           }
           lastTime = currentTime

           if (frameTimes.isEmpty()) return 0f
           val avgFrameTime = frameTimes.average()
           return (1000.0 / avgFrameTime).toFloat()
       }
   }
   ```

2. Integrate into VoxelCraft game loop:
   ```kotlin
   val fpsCounter = FPSCounter()

   fun gameLoop() {
       val currentTime = js("performance.now()").unsafeCast<Double>()
       val fps = fpsCounter.update(currentTime)

       // Existing game loop code...

       updateFPS(fps.toInt(), stats.triangles, stats.calls)
   }
   ```

**Acceptance Criteria**:
- FPS measured with rolling 60-frame average
- Accurate measurement (validated against browser DevTools)
- FPS displayed in HUD every frame

---

### T015 [P]: Implement Draw Call Counter

**Category**: Performance Metrics
**Priority**: Medium
**Dependencies**: None
**Estimated Time**: 1 hour
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt` (renderMesh, getStats)
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt` (renderMesh, getStats)

**Objective**: Track draw calls accurately for both WebGPU and WebGL renderers.

**Implementation Steps**:
1. Ensure `drawCallCount` is incremented in `renderMesh()` for both renderers
2. Reset `drawCallCount` at start of each frame
3. Include draw call count in `RenderStats`
4. Validate count matches actual GPU draw calls

**Acceptance Criteria**:
- Draw calls tracked per frame
- Accurate count (81 for 81 chunks, ~40-50 with culling)
- Displayed in HUD

---

### T016 [P]: Add GPU Memory Tracking

**Category**: Performance Metrics
**Priority**: Low
**Dependencies**: None
**Estimated Time**: 2 hours
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt` (getOrCreateGeometryBuffers, dispose)
- `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt` (same)

**Objective**: Estimate GPU memory usage for debugging and profiling.

**Implementation Steps**:
1. Track buffer sizes when created:
   ```kotlin
   private var totalGPUMemory: Int = 0 // bytes

   private fun getOrCreateGeometryBuffers(geometry: BufferGeometry): GeometryBuffers? {
       // ... existing code ...

       val bufferSize = vertexData.size * 4 + (indexData?.size ?: 0) * 4
       totalGPUMemory += bufferSize

       // ... return buffers ...
   }
   ```

2. Decrement when buffers disposed:
   ```kotlin
   override fun dispose(): RendererResult<Unit> {
       geometryBuffers.values.forEach { buffers ->
           val bufferSize = /* calculate from vertex/index counts */
           totalGPUMemory -= bufferSize
       }
       // ... existing dispose code ...
   }
   ```

3. Include in performance metrics:
   ```kotlin
   val gpuMemoryMB = (totalGPUMemory / (1024 * 1024))
   ```

**Acceptance Criteria**:
- Memory usage tracked (estimated)
- Displayed in HUD (e.g., "GPU Memory: 18 MB")
- Helps identify memory leaks

---

### T017 [P]: Update Performance HUD Display

**Category**: Performance Metrics
**Priority**: Medium
**Dependencies**: T014, T015, T016
**Estimated Time**: 1 hour
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt` (updateFPS, updateHUD)
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/resources/index.html`

**Objective**: Enhance HUD to display comprehensive performance metrics.

**Implementation Steps**:
1. Update HTML to include more metrics:
   ```html
   <div id="performance-hud">
       <div id="fps">60 FPS | 160K▲ | 85DC</div>
       <div id="backend">Backend: WebGPU 1.0</div>
       <div id="memory">GPU Memory: 18 MB</div>
       <div id="culling">Visible: 45 / 81 chunks</div>
   </div>
   ```

2. Update Kotlin functions to populate all fields:
   ```kotlin
   fun updatePerformanceHUD(metrics: PerformanceMetrics) {
       document.getElementById("fps")?.textContent =
           "${metrics.fps.toInt()} FPS | ${metrics.triangles/1000}K▲ | ${metrics.drawCalls}DC"
       document.getElementById("backend")?.textContent =
           "Backend: ${metrics.backendType}"
       document.getElementById("memory")?.textContent =
           "GPU Memory: ${metrics.gpuMemory} MB"
       document.getElementById("culling")?.textContent =
           "Visible: ${metrics.visibleChunks} / 81 chunks"
   }
   ```

**Acceptance Criteria**:
- HUD shows FPS, draw calls, triangles, backend, memory, culling stats
- Updated every frame (<0.1ms overhead)
- Readable and non-intrusive

---

## Phase 5: Integration

**DEPENDS ON PHASES 2-4 COMPLETION**

### T018: Integrate Optimized Renderer with VoxelCraft

**Category**: Integration
**Priority**: Critical
**Dependencies**: T006-T010 (core fixes must be complete)
**Estimated Time**: 2 hours
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt` (continueInitialization)
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/VoxelWorld.kt`

**Objective**: Integrate all renderer optimizations with VoxelCraft game loop.

**Implementation Steps**:
1. Verify WebGPURendererFactory.create() returns optimized renderer
2. Ensure chunk meshes have userData["chunk"] for culling
3. Update game loop to pass performance metrics to HUD
4. Test full integration (terrain generation → rendering → block breaking)

**Acceptance Criteria**:
- T003 test `test_voxelcraft_renders_81_chunks_at_60fps` PASSES
- Game runs at 60 FPS with all optimizations enabled
- No regressions (player movement, block interaction work)

---

### T019: Update Main.kt with Backend Selection Logging

**Category**: Integration
**Priority**: Low
**Dependencies**: T018
**Estimated Time**: 30 minutes
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt` (continueInitialization)

**Objective**: Add detailed logging for backend selection and initialization.

**Implementation Steps**:
1. Enhance existing backend negotiation logging (lines 147-176)
2. Add feature detection details (WebGPU capabilities, WebGL version)
3. Log initialization time with validation against 2000ms budget
4. Add warning if initialization exceeds budget

**Acceptance Criteria**:
- Clear console logs showing backend selection rationale
- Initialization time logged and validated
- User-friendly messages for debugging

---

### T020: Add Performance Validation on Startup

**Category**: Integration
**Priority**: Medium
**Dependencies**: T018, T014
**Estimated Time**: 1 hour
**Files Created**:
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/PerformanceValidator.kt`
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt` (gameLoop)

**Objective**: Validate constitutional performance requirements on startup.

**Implementation Steps**:
1. Create PerformanceValidator:
   ```kotlin
   object PerformanceValidator {
       fun validate(metrics: PerformanceMetrics): Boolean {
           val meetsWebGPUTarget = metrics.backendType.contains("WebGPU") && metrics.fps >= 60f
           val meetsWebGLMinimum = metrics.backendType.contains("WebGL") && metrics.fps >= 30f
           val meetsDrawCallLimit = metrics.drawCalls < 100

           val passed = (meetsWebGPUTarget || meetsWebGLMinimum) && meetsDrawCallLimit

           if (!passed) {
               console.warn("⚠️ Performance validation FAILED:")
               console.warn("  FPS: ${metrics.fps} (Target: 60 WebGPU, 30 WebGL)")
               console.warn("  Draw Calls: ${metrics.drawCalls} (Limit: <100)")
           } else {
               console.log("✅ Performance validation PASSED")
           }

           return passed
       }
   }
   ```

2. Run validation after 5 seconds of gameplay (allow warmup)
3. Log results to console

**Acceptance Criteria**:
- Validation runs automatically after 5 seconds
- Clear pass/fail messaging
- Helps catch regressions

---

### T021: Update VoxelWorld Chunk Streaming with Culling

**Category**: Integration
**Priority**: Low
**Dependencies**: T009 (frustum culling must exist)
**Estimated Time**: 1 hour
**Files Modified**:
- `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/VoxelWorld.kt` (update method)

**Objective**: Optimize chunk streaming to prioritize visible chunks.

**Implementation Steps**:
1. Track visible chunks from last frame's culling
2. Prioritize visible chunks for mesh generation in `pumpDirtyChunks()`
3. Defer mesh generation for culled chunks
4. Ensure smooth streaming without popping

**Acceptance Criteria**:
- Visible chunks rendered first
- Culled chunks deferred (but still eventually rendered)
- No visual popping or delays

---

## Phase 6: Testing & Validation

**MUST RUN AFTER PHASES 2-5 COMPLETE**

### T022: Run All Contract Tests

**Category**: Validation
**Priority**: Critical
**Dependencies**: T001-T021 (all implementation complete)
**Estimated Time**: 1 hour
**Command**:
```bash
cd /home/yousef/Projects/kmp/KreeKt
./gradlew :src:jsTest:test --tests "WebGPURendererPerformanceTest"
./gradlew :src:jsTest:test --tests "WebGLFallbackPerformanceTest"
./gradlew :src:jsTest:test --tests "WebGPUPipelineCacheTest"
./gradlew :src:jsTest:test --tests "WebGPUBufferManagementTest"
./gradlew :examples:voxelcraft:jsTest --tests "VoxelCraftPerformanceTest"
```

**Objective**: Validate all contract tests pass with 100% success rate.

**Acceptance Criteria**:
- All tests from T001-T005 PASS
- No test failures
- Performance assertions met (60 FPS WebGPU, 30 FPS WebGL)

---

### T023: Execute Quickstart Validation

**Category**: Validation
**Priority**: Critical
**Dependencies**: T022 (tests must pass)
**Estimated Time**: 30 minutes
**Command**:
```bash
cd /home/yousef/Projects/kmp/KreeKt
./gradlew :examples:voxelcraft:jsRun
# Follow steps in quickstart.md Scenario 1 and Scenario 2
```

**Objective**: Manually validate VoxelCraft rendering follows quickstart.md scenarios.

**Validation Steps**:
1. Open Chrome 113+ → Verify WebGPU backend selected
2. Observe FPS: Should be 60 FPS
3. Move camera → FPS stays 60
4. Break block → FPS dips to 55-58, recovers to 60
5. Open Firefox → Verify WebGL backend selected
6. Observe FPS: Should be 30+ FPS
7. Verify visual consistency (same rendering)

**Acceptance Criteria**:
- Scenario 1 (WebGPU 60 FPS) passes
- Scenario 2 (WebGL 30+ FPS) passes
- All quickstart steps completed successfully

---

### T024: Cross-Browser Testing

**Category**: Validation
**Priority**: High
**Dependencies**: T023 (quickstart must pass)
**Estimated Time**: 1 hour
**Browsers**:
- Chrome 113+ (WebGPU)
- Edge 113+ (WebGPU)
- Firefox (WebGL)
- Safari (WebGL)

**Objective**: Validate rendering consistency and performance across browsers.

**Test Matrix**:

| Browser | Backend | Expected FPS | Draw Calls | Visual Check |
|---------|---------|--------------|------------|--------------|
| Chrome 113+ | WebGPU | 60 | <100 | ✓ |
| Edge 113+ | WebGPU | 60 | <100 | ✓ |
| Firefox | WebGL | 30+ | <100 | ✓ |
| Safari | WebGL | 30+ | <100 | ✓ |

**Acceptance Criteria**:
- All browsers tested
- FPS meets minimums
- No visual artifacts or differences
- Graceful fallback works

---

### T025: Performance Regression Testing

**Category**: Validation
**Priority**: High
**Dependencies**: T024
**Estimated Time**: 2 hours
**Files Created**:
- `/home/yousef/Projects/kmp/KreeKt/specs/018-optimize-voxelcraft-rendering/performance-baseline.md`

**Objective**: Establish performance baseline and validate no regressions.

**Metrics to Capture**:
1. **FPS**: Min, max, average over 60 seconds
2. **Frame time**: 1st percentile, 50th, 95th, 99th
3. **Draw calls**: Per frame (with and without culling)
4. **GPU memory**: Peak usage
5. **Initialization time**: Renderer init
6. **Pipeline cache**: Hit rate after 100 frames

**Baseline**:
```markdown
# Performance Baseline: 018-optimize-voxelcraft-rendering

**Date**: 2025-10-07
**Hardware**: (capture GPU model)
**Browser**: Chrome 113+ / Firefox

## WebGPU (Chrome 113+)
- FPS: Min 58, Avg 60.2, Max 62
- Frame time: p50=16.5ms, p95=17.2ms, p99=18.1ms
- Draw calls: 45 (with culling), 81 (without)
- GPU memory: 18 MB
- Init time: 52ms
- Pipeline cache hit rate: 98.5%

## WebGL (Firefox)
- FPS: Min 32, Avg 35.1, Max 38
- Frame time: p50=28.5ms, p95=31.2ms, p99=33.1ms
- Draw calls: 45 (with culling), 81 (without)
- GPU memory: 18 MB
- Init time: 43ms
```

**Acceptance Criteria**:
- Baseline documented
- All metrics meet constitutional requirements
- Performance regression tests added to CI/CD (future)

---

## Parallel Execution Examples

### Example 1: Contract Tests (T001-T005)
All contract tests can run in parallel (different files):
```bash
# Launch all 5 tests concurrently
gradle :src:jsTest:test --tests "WebGPURendererPerformanceTest" &
gradle :src:jsTest:test --tests "WebGLFallbackPerformanceTest" &
gradle :src:jsTest:test --tests "VoxelCraftPerformanceTest" &
gradle :src:jsTest:test --tests "WebGPUPipelineCacheTest" &
gradle :src:jsTest:test --tests "WebGPUBufferManagementTest" &
wait
```

### Example 2: WebGL Optimizations (T011-T013)
Independent WebGL optimizations:
```bash
# T011, T012, T013 modify different aspects of WebGLRenderer.kt
# Can work on simultaneously but need coordination for same file
# Alternative: Branch per task, merge sequentially
```

### Example 3: Performance Metrics (T014-T017)
All metrics can be developed in parallel:
```bash
# T014: FPSCounter.kt (new file)
# T015: Draw call counter (WebGPURenderer.kt, WebGLRenderer.kt - different methods)
# T016: GPU memory tracking (same, different methods)
# T017: HUD display (Main.kt, index.html - different files)
```

---

## Dependencies Graph

```
Setup: None

Tests (Phase 1): [ALL PARALLEL]
├─ T001 [P] WebGPU performance tests
├─ T002 [P] WebGL fallback tests
├─ T003 [P] VoxelCraft integration tests
├─ T004 [P] Pipeline caching tests
└─ T005 [P] Buffer management tests

Core WebGPU (Phase 2): [SEQUENTIAL]
T006 (pipeline fix) → T007 (cache) → T008 (buffers) → T009 (culling) → T010 (draw calls)

WebGL (Phase 3): [PARALLEL with Phase 2]
├─ T011 [P] VAO support
├─ T012 [P] Attribute caching
└─ T013 [P] Index buffer optimization

Metrics (Phase 4): [PARALLEL with Phase 2-3]
├─ T014 [P] FPS measurement
├─ T015 [P] Draw call counter
├─ T016 [P] GPU memory tracking
└─ T017 [P] HUD display

Integration (Phase 5): [SEQUENTIAL, AFTER Phase 2-4]
T018 (renderer integration) → T019 (logging) → T020 (validation) → T021 (streaming)

Validation (Phase 6): [SEQUENTIAL, AFTER Phase 5]
T022 (run tests) → T023 (quickstart) → T024 (cross-browser) → T025 (regression)
```

---

## Notes

- **TDD Discipline**: T001-T005 MUST be written and failing before T006-T021 implementation
- **Parallel [P] Tasks**: Can run simultaneously (different files, no dependencies)
- **Commit Strategy**: Commit after each task completion with descriptive message
- **Test Validation**: After each implementation task, run corresponding test to verify it now passes
- **Constitutional Validation**: T020, T025 validate 60 FPS, 30 FPS, <100 draw calls requirements

---

## Task Execution Template

When implementing a task, follow this pattern:

```
1. Read task description and acceptance criteria
2. Read referenced files (use Read tool)
3. If test task: Write failing test, verify it fails
4. If implementation task: Run corresponding test first, verify it fails
5. Implement changes as specified in task
6. Run test, verify it now passes
7. Commit with message: "T###: [task description]"
8. Move to next task
```

---

**Total Tasks**: 27
**Estimated Time**: ~40-50 hours
**Parallel Opportunities**: 15 tasks marked [P]
**Critical Path**: T001 → T006 → T007 → T008 → T009 → T018 → T022 → T023
**Expected Outcome**: 60 FPS WebGPU, 30+ FPS WebGL, <100 draw calls, all tests passing ✅
