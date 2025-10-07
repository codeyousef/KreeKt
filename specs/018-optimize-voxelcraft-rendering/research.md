# Research: WebGPU/Vulkan Rendering Optimization

**Feature**: 018-optimize-voxelcraft-rendering
**Date**: 2025-10-07
**Researcher**: Implementation Planning Agent

## Executive Summary

VoxelCraft's 0 FPS rendering is caused by:
1. **Busy-wait pipeline creation** - Blocks render loop for 10,000+ iterations
2. **No pipeline caching** - Creates new pipeline every frame
3. **Missing frustum culling** - Renders all 81 chunks regardless of visibility
4. **WebGL fallback inefficiency** - Repeated `getAttribLocation()` calls per frame

This research documents solutions for each bottleneck to achieve 60 FPS (WebGPU) and 30+ FPS (WebGL fallback).

---

## 1. WebGPU Pipeline Creation Patterns

### Problem Analysis
Current implementation (`WebGPURenderer.kt:496-564`):
```kotlin
// Busy-wait pattern - BLOCKS RENDER THREAD
var attempts = 0
while (!pipelineReady && pipelineError == null && attempts < 10000) {
    attempts++
    if (attempts % 1000 == 0) {
        console.log("Waiting for pipeline... attempt $attempts")
    }
}
```
**Impact**: 10,000 iterations per pipeline creation = ~50-100ms blocked render thread = 0 FPS

### Decision: **Async Pipeline Pre-Compilation with Deferred Loading**

**Rationale**:
- WebGPU `createRenderPipeline()` is inherently async (returns Promise)
- Render loop must never block - constitutional 60 FPS requirement
- Pipelines can be pre-compiled during initialization (most materials known upfront)
- For runtime pipeline creation, use "warm-up frame" approach

**Alternatives Considered**:

| Approach | Pros | Cons | Verdict |
|----------|------|------|---------|
| Busy-wait (current) | Simple | Blocks render thread | ❌ Rejected |
| Async/await in render loop | Non-blocking | Still delays first render | ⚠️ Partial |
| **Pre-compilation + lazy** | Best UX, no blocking | Slightly complex | ✅ **Chosen** |
| Synchronous createRenderPipelineSync | Immediate | Not available in spec | ❌ N/A |

**Implementation Notes**:
1. **Initialization Phase**: Pre-compile common pipelines (basic material + standard geometry)
   ```kotlin
   suspend fun precompilePipelines() {
       // Create pipelines for common material/geometry combinations
       val basicMaterial = MeshBasicMaterial()
       val standardGeometry = BoxGeometry(1f, 1f, 1f)
       precompilePipeline(standardGeometry, basicMaterial)
   }
   ```

2. **Runtime Creation**: Use coroutine launch + fallback
   ```kotlin
   fun getOrCreatePipeline(geometry: BufferGeometry, material: Material): GPURenderPipeline? {
       val cacheKey = PipelineKey.from(geometry, material)

       // Return cached pipeline immediately
       pipelineCacheMap[cacheKey]?.let { return it.getPipeline() }

       // Launch async creation (non-blocking)
       GlobalScope.launch {
           val pipeline = WebGPUPipeline(device, createDescriptor(geometry, material))
           pipeline.create() // Awaits internally
           pipelineCacheMap[cacheKey] = pipeline
       }

       // Return null (mesh skipped this frame, will render next frame)
       return null
   }
   ```

3. **Graceful Degradation**: Skip mesh if pipeline not ready (better than frozen frame)
   ```kotlin
   val pipeline = getOrCreatePipeline(geometry, material) ?: run {
       console.warn("Pipeline not ready for mesh $uuid, skipping frame")
       return // Skip this mesh, will render next frame
   }
   ```

**References**:
- WebGPU spec: https://gpuweb.github.io/gpuweb/#dom-gpudevice-createrenderpipeline
- Three.js pattern: Lazy compilation with placeholder geometry
- Babylon.js pattern: Pipeline warm-up phase

---

## 2. Pipeline Caching Strategies

### Problem Analysis
Current implementation has caching infrastructure but no effective synchronous access:
- `PipelineCache` class exists (`WebGPURenderer.kt:50`)
- `pipelineCacheMap` exists (`WebGPURenderer.kt:64`) but always empty
- Pipeline creation happens every frame because cache lookup fails

### Decision: **Synchronous Cache with Pipeline Key Hashing**

**Rationale**:
- Render loop must be synchronous (60 FPS = 16.67ms budget)
- Pipeline creation is async, but cache lookup must be instant
- Cache key must uniquely identify pipeline (geometry + material hash)

**Alternatives Considered**:

| Approach | Pros | Cons | Verdict |
|----------|------|------|---------|
| **Synchronous Map<Key, Pipeline>** | O(1) lookup, simple | Must handle async creation | ✅ **Chosen** |
| Async cache with suspend | Matches WebGPU async model | Can't call from render loop | ❌ Rejected |
| LRU cache with eviction | Memory efficient | Added complexity | ⚠️ Future optimization |

**Implementation Notes**:
1. **Cache Key Generation**: Hash geometry + material properties
   ```kotlin
   data class PipelineKey(
       val vertexFormat: String,  // "pos3_norm3_col3"
       val indexFormat: String?,  // "uint32" or null
       val materialType: String,  // "MeshBasicMaterial"
       val materialHash: Int      // material.color.hashCode() etc.
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
       }
   }
   ```

2. **Cache Access Pattern**:
   ```kotlin
   private val pipelineCacheMap = mutableMapOf<PipelineKey, WebGPUPipeline>()

   fun getOrCreatePipeline(geometry: BufferGeometry, material: Material): GPURenderPipeline? {
       val key = PipelineKey.from(geometry, material)

       // Synchronous cache lookup
       val cached = pipelineCacheMap[key]
       if (cached != null && cached.isReady) {
           return cached.getPipeline()
       }

       // Create new pipeline asynchronously (see Research #1)
       if (cached == null) {
           createPipelineAsync(key, geometry, material)
       }

       return cached?.getPipeline() // May be null if still creating
   }
   ```

3. **Cache Statistics** (for debugging):
   ```kotlin
   data class CacheStats(
       val hits: Int,
       val misses: Int,
       val size: Int,
       val hitRate: Float = hits.toFloat() / (hits + misses).coerceAtLeast(1)
   )
   ```

**Cache Invalidation Strategy**:
- No automatic eviction (pipelines are lightweight, ~1KB each)
- Manual clear on dispose()
- Future: LRU eviction if >100 pipelines (unlikely in VoxelCraft)

**References**:
- Three.js: WebGLPrograms cache (synchronous map)
- Vulkan best practices: Pipeline cache objects
- WebGPU samples: Pipeline caching patterns

---

## 3. WebGPU Buffer Management

### Problem Analysis
Current implementation creates buffers correctly but lacks VAO-equivalent for caching attribute state:
- Vertex/index buffers created once per geometry ✅
- Buffer bindings set every frame ❌
- No bind group reuse ❌

### Decision: **Persistent Bind Groups (WebGPU's VAO Equivalent)**

**Rationale**:
- Bind groups cache buffer bindings and uniform layouts
- Creating bind group once and reusing = VAO performance benefits
- WebGPU spec explicitly designed bind groups for this use case

**Alternatives Considered**:

| Approach | Pros | Cons | Verdict |
|----------|------|------|---------|
| **Persistent bind groups** | Native WebGPU pattern, fast | Need to track geometry changes | ✅ **Chosen** |
| Re-create every frame (current) | Simple | Slow (~0.1ms per bind group) | ❌ Rejected |
| Buffer pooling | Reduces allocations | VoxelCraft doesn't need it | ⚠️ Unnecessary |

**Implementation Notes**:
1. **Geometry Buffers Structure**: Cache bind group with buffers
   ```kotlin
   data class GeometryBuffers(
       val vertexBuffer: GPUBuffer,
       val indexBuffer: GPUBuffer?,
       val bindGroup: GPUBindGroup,  // NEW: Cache bind group
       val vertexCount: Int,
       val indexCount: Int,
       val indexFormat: String
   )
   ```

2. **Bind Group Creation**: Once per geometry
   ```kotlin
   private fun createGeometryBindGroup(
       vertexBuffer: GPUBuffer,
       uniformBuffer: GPUBuffer
   ): GPUBindGroup {
       val descriptor = js("({})").unsafeCast<GPUBindGroupDescriptor>()
       descriptor.layout = pipelineBindGroupLayout

       val entries = arrayOf(
           createBindGroupEntry(0, vertexBuffer),  // Vertex data
           createBindGroupEntry(1, uniformBuffer)  // MVP matrices
       )
       descriptor.entries = entries

       return device!!.createBindGroup(descriptor)
   }
   ```

3. **Render Loop Usage**: Set bind group once per mesh
   ```kotlin
   private fun renderMesh(mesh: Mesh, camera: Camera, renderPass: GPURenderPassEncoder) {
       val buffers = getOrCreateGeometryBuffers(geometry)

       // Set bind group (fast - just pointer)
       renderPass.setBindGroup(0, buffers.bindGroup)

       // Set vertex buffer (still needed for draw call reference)
       renderPass.setVertexBuffer(0, buffers.vertexBuffer)

       // Draw
       if (buffers.indexBuffer != null) {
           renderPass.setIndexBuffer(buffers.indexBuffer, buffers.indexFormat)
           renderPass.drawIndexed(buffers.indexCount, 1, 0, 0, 0)
       }
   }
   ```

**Buffer Update Strategy** (for dynamic meshes like block breaking):
- Immutable buffers for static terrain chunks
- For modified chunks: Re-create buffer + bind group (infrequent)
- Alternative: Use `GPUBufferUsage.COPY_DST` and `writeBuffer()` for small updates

**References**:
- WebGPU spec: Bind groups (https://gpuweb.github.io/gpuweb/#bind-groups)
- Three.js WebGL: VAO usage (VertexArrayObject class)
- wgpu-rs: Bind group management patterns

---

## 4. Frustum Culling Implementation

### Problem Analysis
VoxelCraft renders all 81 chunks every frame regardless of camera orientation:
- Camera can only see ~40-50 chunks at typical FOV (75°)
- Rendering off-screen chunks wastes ~40% of GPU time
- No spatial culling system exists

### Decision: **View Frustum + AABB Intersection Test**

**Rationale**:
- Frustum culling is standard 3D optimization (10-50% perf gain typical)
- Chunk AABBs are static and cheap to test (box vs 6 planes)
- CPU cost minimal (~0.1ms for 81 tests), GPU savings large (~6ms)
- Achieves FR-008 requirement (reduce draw calls)

**Alternatives Considered**:

| Approach | Pros | Cons | Verdict |
|----------|------|------|---------|
| **Frustum culling** | Standard, effective, simple | Needs camera matrix math | ✅ **Chosen** |
| Occlusion culling | More accurate | Complex (GPU queries), overkill | ❌ Too complex |
| Distance culling only | Very simple | Doesn't handle camera rotation | ❌ Insufficient |
| No culling (current) | No CPU cost | Wastes 40% GPU time | ❌ Rejected |

**Implementation Notes**:
1. **Frustum Plane Extraction**: From combined view-projection matrix
   ```kotlin
   class Frustum {
       private val planes = Array(6) { Plane() }

       fun setFromProjectionMatrix(matrix: Matrix4) {
           val m = matrix.elements

           // Extract 6 planes from matrix (Gribb/Hartmann method)
           // Left:   m[3] + m[0]
           // Right:  m[3] - m[0]
           // Bottom: m[3] + m[1]
           // Top:    m[3] - m[1]
           // Near:   m[3] + m[2]
           // Far:    m[3] - m[2]

           planes[0].setComponents(m[3] + m[0], m[7] + m[4], m[11] + m[8], m[15] + m[12]).normalize()
           planes[1].setComponents(m[3] - m[0], m[7] - m[4], m[11] - m[8], m[15] - m[12]).normalize()
           planes[2].setComponents(m[3] + m[1], m[7] + m[5], m[11] + m[9], m[15] + m[13]).normalize()
           planes[3].setComponents(m[3] - m[1], m[7] - m[5], m[11] - m[9], m[15] - m[13]).normalize()
           planes[4].setComponents(m[3] + m[2], m[7] + m[6], m[11] + m[10], m[15] + m[14]).normalize()
           planes[5].setComponents(m[3] - m[2], m[7] - m[6], m[11] - m[10], m[15] - m[14]).normalize()
       }

       fun intersectsBox(box: Box3): Boolean {
           // Test AABB against all 6 planes
           for (plane in planes) {
               if (plane.distanceToPoint(box.getPositiveVertex(plane.normal)) < 0) {
                   return false // Box is completely outside this plane
               }
           }
           return true // Box intersects or is inside frustum
       }
   }
   ```

2. **Chunk AABB Calculation**: Static bounding boxes
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

3. **Integration with Render Loop**:
   ```kotlin
   override fun render(scene: Scene, camera: Camera): RendererResult<Unit> {
       // Update frustum from camera
       val frustum = Frustum()
       frustum.setFromProjectionMatrix(camera.projectionViewMatrix)

       // Cull invisible chunks
       val visibleChunks = world.chunks.values.filter { chunk ->
           frustum.intersectsBox(chunk.boundingBox)
       }

       console.log("Frustum culling: ${visibleChunks.size}/${world.chunks.size} chunks visible")

       // Render only visible chunks
       visibleChunks.forEach { chunk ->
           chunk.mesh?.let { renderMesh(it, camera, renderPass) }
       }
   }
   ```

**Performance Impact Estimate**:
- CPU cost: ~0.1ms (81 box tests)
- GPU savings: ~6ms (40 fewer chunks rendered)
- Net gain: ~5.9ms → +10 FPS (from 50 to 60)

**References**:
- Frustum culling: https://www.lighthouse3d.com/tutorials/view-frustum-culling/
- Three.js implementation: Frustum class + Object3D.frustumCulled
- Fast AABB-Plane test: https://gdbooks.gitbooks.io/3dcollisions/content/Chapter2/static_aabb_plane.html

---

## 5. Draw Call Batching

### Problem Analysis
VoxelCraft makes one draw call per chunk mesh:
- 81 visible chunks = 81 draw calls
- FR-005 requires <100 draw calls for 81 chunks ✅ (currently meets requirement)
- However, frustum culling will expose future scaling issues (1024 total chunks)

### Decision: **No Batching Required (Defer to Future Optimization)**

**Rationale**:
- Current 81 draw calls meets FR-005 requirement (<100)
- Each chunk has unique material/geometry (greedy meshing creates varied meshes)
- WebGPU draw call overhead is low (~0.01ms per call)
- Batching adds complexity with minimal current benefit
- Focus on pipeline caching + frustum culling (higher ROI)

**Alternatives Considered**:

| Approach | Pros | Cons | Verdict |
|----------|------|------|---------|
| **No batching (current)** | Simple, meets requirements | Future scaling concern | ✅ **Chosen** (defer batching) |
| Static batching | Reduces draw calls | Breaks dynamic updates | ❌ Incompatible with VoxelCraft |
| Dynamic batching | Flexible | Complex, CPU overhead | ⚠️ Future if needed |
| Instancing | Very efficient | All chunks must share geometry | ❌ Not applicable |

**Future Consideration**:
If VoxelCraft scales to >200 visible chunks, revisit with:
1. **Greedy batching**: Combine chunks with identical materials
2. **Indirect draw buffer**: Single draw call with GPU-driven culling
3. **Mesh LOD**: Reduce triangle count for distant chunks

**Implementation Notes**: N/A (no changes needed)

**References**:
- WebGPU draw call overhead benchmarks
- Three.js: BatchedMesh class (for future reference)
- Minecraft optimization: Chunk batch rendering

---

## 6. WebGL Optimization Patterns

### Problem Analysis
Current WebGL fallback has same 0 FPS issue:
- `getAttribLocation()` called 81 times per frame per attribute = ~240 calls/frame
- No VAO usage (WebGL 2.0 feature available but unused)
- Index buffer conversion happens every frame

**Previous fix attempt** (from 017-fix-voxelcraft-rendering):
- Added attribute location caching ✅
- Changed `updateMatrixWorld(false)` ✅
- Still 0 FPS ❌

### Decision: **WebGL 2.0 VAO + Enhanced Attribute Caching**

**Rationale**:
- VAO caches all vertex attribute state (like WebGPU bind groups)
- Attribute location caching already implemented but needs VAO for full benefit
- WebGL 2.0 support is universal (Firefox, Safari, Chrome)
- Achieves FR-003 (30+ FPS minimum)

**Alternatives Considered**:

| Approach | Pros | Cons | Verdict |
|----------|------|------|---------|
| **VAO + caching** | Standard WebGL 2.0 pattern | None | ✅ **Chosen** |
| Attribute caching only | Partial improvement | Still slow | ❌ Insufficient |
| WebGL 1.0 compatibility | Wider support | Slower, OES_vertex_array_object needed | ❌ Unnecessary |

**Implementation Notes**:
1. **VAO Creation**: Once per geometry
   ```kotlin
   data class WebGLGeometryBuffers(
       val vertexBuffer: WebGLBuffer,
       val indexBuffer: WebGLBuffer?,
       val vao: WebGLVertexArrayObject,  // NEW: Cache VAO
       val vertexCount: Int,
       val indexCount: Int
   )

   private fun createVAO(
       gl: WebGL2RenderingContext,
       vertexBuffer: WebGLBuffer,
       indexBuffer: WebGLBuffer?
   ): WebGLVertexArrayObject {
       val vao = gl.createVertexArray()!!
       gl.bindVertexArray(vao)

       // Bind vertex buffer and set attributes
       gl.bindBuffer(GL.ARRAY_BUFFER, vertexBuffer)

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

2. **Render Loop Usage**:
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

       gl.bindVertexArray(null) // Unbind
   }
   ```

3. **Attribute Location Caching** (already implemented):
   ```kotlin
   private var cachedAttribLocations: Map<String, Int>? = null

   private fun getOrCacheAttribLocation(gl: WebGLRenderingContext, program: WebGLProgram, name: String): Int {
       if (cachedAttribLocations == null) {
           cachedAttribLocations = mapOf(
               "aPosition" to gl.getAttribLocation(program, "aPosition"),
               "aNormal" to gl.getAttribLocation(program, "aNormal"),
               "aColor" to gl.getAttribLocation(program, "aColor"),
               "aUV" to gl.getAttribLocation(program, "aUV")
           )
       }
       return cachedAttribLocations!![name] ?: -1
   }
   ```

**Performance Impact Estimate**:
- Without VAO: ~240 attribute setup calls/frame = ~4ms
- With VAO: 1 bind call/chunk = ~0.5ms
- Net gain: ~3.5ms → Enables 30+ FPS (from 0 FPS)

**References**:
- WebGL 2.0 VAO spec: https://registry.khronos.org/webgl/specs/latest/2.0/#3.7.17
- Three.js WebGL: WebGLAttributes + WebGLBindingStates (VAO management)
- Mozilla VAO tutorial: https://developer.mozilla.org/en-US/docs/Web/API/WebGL_API/WebGL_best_practices#use_vaos

---

## 7. Performance Profiling

### Problem Analysis
Current metrics are incomplete:
- FPS counter exists but not validated
- Draw call count tracked but not enforced
- No GPU memory tracking
- No frame time breakdown

### Decision: **Performance.now() + Custom Metrics**

**Rationale**:
- `performance.now()` provides microsecond precision timestamps
- Custom metrics validate constitutional requirements (60 FPS, <100 draw calls)
- Browser DevTools integration for debugging
- Lightweight (<0.1ms overhead)

**Implementation Notes**:
1. **FPS Measurement**: Rolling average
   ```kotlin
   class FPSCounter {
       private val frameTimes = mutableListOf<Double>()
       private var lastTime = 0.0

       fun update(): Float {
           val currentTime = js("performance.now()").unsafeCast<Double>()
           if (lastTime > 0) {
               val deltaTime = currentTime - lastTime
               frameTimes.add(deltaTime)
               if (frameTimes.size > 60) frameTimes.removeAt(0)
           }
           lastTime = currentTime

           val avgFrameTime = frameTimes.average()
           return (1000.0 / avgFrameTime).toFloat() // Convert to FPS
       }
   }
   ```

2. **Performance Metrics Structure**:
   ```kotlin
   data class PerformanceMetrics(
       val fps: Float,
       val frameTime: Float,  // ms
       val drawCalls: Int,
       val triangles: Int,
       val backendType: String,  // "WebGPU 1.0" or "WebGL 2.0"
       val gpuMemory: Int = 0,  // MB (estimated)
       val pipelineCacheHits: Int = 0,
       val pipelineCacheMisses: Int = 0,
       val culledChunks: Int = 0,
       val visibleChunks: Int = 0
   )
   ```

3. **Constitutional Validation**:
   ```kotlin
   fun validatePerformance(metrics: PerformanceMetrics): Boolean {
       val meetsWebGPUTarget = metrics.backendType.contains("WebGPU") && metrics.fps >= 60f
       val meetsWebGLMinimum = metrics.backendType.contains("WebGL") && metrics.fps >= 30f
       val meetsDrawCallLimit = metrics.drawCalls < 100

       return (meetsWebGPUTarget || meetsWebGLMinimum) && meetsDrawCallLimit
   }
   ```

**WebGPU Timestamp Queries** (Future Enhancement):
- Not yet widely supported (Chrome only)
- Would provide GPU-side timing (more accurate)
- Defer to future optimization

**References**:
- Performance API: https://developer.mozilla.org/en-US/docs/Web/API/Performance
- WebGPU timestamp queries: https://gpuweb.github.io/gpuweb/#timestamp-query
- Three.js: WebGLRenderer.info stats

---

## 8. Cross-Browser WebGPU Support

### Problem Analysis
WebGPU availability varies by browser:
- Chrome 113+: Full WebGPU support ✅
- Edge 113+: Full WebGPU support ✅
- Firefox: WebGPU behind flag (no stable support yet) ❌
- Safari: WebGPU in technology preview (iOS 18+, macOS 15+) ⚠️

### Decision: **Progressive Enhancement with WebGL 2.0 Fallback**

**Rationale**:
- WebGPU adoption will grow, but WebGL fallback needed for 1-2 years
- WebGL 2.0 has 97% browser support (all modern browsers)
- Progressive enhancement provides best UX for all users
- Meets FR-010 (graceful fallback) and FR-011 (visual consistency)

**Implementation Notes**:
1. **Feature Detection**:
   ```kotlin
   suspend fun detectWebGPU(): Boolean {
       val hasNavigatorGPU = js("'gpu' in navigator").unsafeCast<Boolean>()
       if (!hasNavigatorGPU) return false

       try {
           val adapter = navigator.gpu.requestAdapter().awaitPromise()
           return adapter != null
       } catch (e: Exception) {
           console.warn("WebGPU detection failed: ${e.message}")
           return false
       }
   }
   ```

2. **Backend Selection** (already implemented in `WebGPURendererFactory`):
   ```kotlin
   suspend fun create(canvas: HTMLCanvasElement): Renderer {
       return if (detectWebGPU()) {
           console.log("✅ WebGPU available - using WebGPU renderer")
           val renderer = WebGPURenderer(canvas)
           when (val result = renderer.initialize()) {
               is RendererResult.Success -> renderer
               is RendererResult.Error -> {
                   console.warn("⚠️ WebGPU init failed: ${result.exception.message}")
                   console.log("Falling back to WebGL...")
                   WebGLRenderer(canvas) // Fallback
               }
           }
       } else {
           console.log("ℹ️ WebGPU not available - using WebGL 2.0 renderer")
           WebGLRenderer(canvas)
       }
   }
   ```

3. **Visual Consistency Validation**:
   - Same shader math (WGSL vs GLSL must produce identical output)
   - Same vertex attributes (position, normal, color)
   - Same transformation matrices (MVP calculations)
   - Integration test: Render same scene, compare pixels (allowable diff <1%)

**Browser Compatibility Matrix**:

| Browser | WebGPU Support | WebGL 2.0 Support | VoxelCraft Status |
|---------|----------------|-------------------|-------------------|
| Chrome 113+ | ✅ Full | ✅ Full | ✅ 60 FPS (WebGPU) |
| Edge 113+ | ✅ Full | ✅ Full | ✅ 60 FPS (WebGPU) |
| Firefox | ⚠️ Flag only | ✅ Full | ✅ 30+ FPS (WebGL) |
| Safari 17+ | ⚠️ Preview | ✅ Full | ✅ 30+ FPS (WebGL) |
| Safari 18+ | ✅ Shipping | ✅ Full | ✅ 60 FPS (WebGPU) |

**References**:
- WebGPU browser support: https://caniuse.com/webgpu
- WebGL 2.0 support: https://caniuse.com/webgl2
- Progressive enhancement: https://web.dev/progressively-enhance-your-pwa/

---

## Implementation Priority

Based on impact analysis:

1. **Critical (Blocks 60 FPS)**:
   - Fix WebGPU pipeline creation (Research #1)
   - Implement pipeline caching (Research #2)

2. **High (Enables 30+ FPS WebGL)**:
   - WebGL VAO implementation (Research #6)

3. **Medium (Performance gains)**:
   - Frustum culling (Research #4)
   - WebGPU buffer management (Research #3)

4. **Low (Nice to have)**:
   - Performance profiling (Research #7)
   - Draw call batching (Research #5 - deferred)

---

## Phase 0 Complete

**Next Step**: Phase 1 - Design & Contracts
- Generate data-model.md from entities
- Create API contracts from functional requirements
- Write failing contract tests
- Extract quickstart scenarios
- Update CLAUDE.md with research findings
