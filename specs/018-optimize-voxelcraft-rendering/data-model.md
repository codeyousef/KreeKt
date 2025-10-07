# Data Model: WebGPU Rendering Optimization

**Feature**: 018-optimize-voxelcraft-rendering
**Date**: 2025-10-07

## Core Entities

### RenderBackend
Represents the selected GPU API (WebGPU or WebGL).

**Fields**:
- `type: BackendType` - enum (WEBGPU, WEBGL)
- `isInitialized: Boolean` - initialization state
- `features: Set<String>` - supported features (e.g., "timestamp-query", "depth-clip-control")
- `adapter: GPUAdapter?` - WebGPU adapter (null for WebGL)
- `device: GPUDevice?` - WebGPU device (null for WebGL)

**Validation Rules**:
- MUST select WebGPU if available (FR-001)
- MUST fallback to WebGL if WebGPU unavailable (FR-010)
- `isInitialized` = true only after successful init

**State Transitions**:
```
Uninitialized → Initializing → Initialized
Uninitialized → Initializing → Failed → Fallback(WebGL) → Initialized
```

---

### VertexBuffer
GPU memory buffer containing vertex data (position, normal, UV, color).

**Fields**:
- `buffer: GPUBuffer | WebGLBuffer` - platform-specific buffer handle
- `size: Int` - buffer size in bytes
- `usage: BufferUsage` - VERTEX | COPY_DST
- `vertexCount: Int` - number of vertices
- `stride: Int` - bytes per vertex (typically 36: 3*float pos + 3*float norm + 3*float color + 2*float uv)

**Relationships**:
- Belongs to `GeometryBuffers` (1:1)
- Referenced by `DrawCall` (many-to-one)

**Validation Rules**:
- `size` MUST equal `vertexCount * stride`
- `buffer` MUST be created before first use
- `usage` MUST include VERTEX flag

---

### IndexBuffer
GPU memory buffer containing triangle indices.

**Fields**:
- `buffer: GPUBuffer | WebGLBuffer` - platform-specific buffer handle
- `count: Int` - number of indices
- `format: IndexFormat` - UINT16 or UINT32
- `size: Int` - buffer size in bytes

**Relationships**:
- Belongs to `GeometryBuffers` (1:1 optional)
- Referenced by `DrawCall` (many-to-one optional)

**Validation Rules**:
- `count` MUST be multiple of 3 (triangles)
- `size` MUST equal `count * formatSize` (2 for UINT16, 4 for UINT32)
- Optional - non-indexed geometry supported

---

### GeometryBuffers
Cached GPU buffers for a specific geometry (chunk mesh).

**Fields**:
- `vertexBuffer: VertexBuffer` - vertex data buffer
- `indexBuffer: IndexBuffer?` - optional index buffer
- `bindGroup: GPUBindGroup?` - WebGPU bind group (VAO equivalent)
- `vao: WebGLVertexArrayObject?` - WebGL VAO
- `geometryUUID: String` - unique identifier
- `uploadTime: Double` - timestamp of upload (for cache eviction)

**Relationships**:
- Contains `VertexBuffer` (1:1)
- Contains `IndexBuffer` (1:0..1)
- Referenced by `Mesh` via UUID (many-to-one)

**Validation Rules**:
- WebGPU: `bindGroup` MUST be non-null
- WebGL: `vao` MUST be non-null
- `geometryUUID` MUST match source geometry UUID

---

### RenderPipeline
WebGPU render pipeline or WebGL program encapsulating shaders and render state.

**Fields**:
- `pipeline: GPURenderPipeline | WebGLProgram` - platform-specific handle
- `cacheKey: PipelineKey` - unique cache identifier
- `isReady: Boolean` - compilation status
- `descriptor: RenderPipelineDescriptor` - original descriptor for debugging
- `creationTime: Double` - timestamp of creation

**Relationships**:
- Cached by `PipelineCache` (many-to-one)
- Referenced by `DrawCall` (many-to-one)

**State Transitions**:
```
Creating → Ready
Creating → Failed (log error, retry with fallback shader)
```

**Validation Rules**:
- `isReady` = true before first use in render pass
- `cacheKey` MUST be deterministic from `descriptor`

---

### PipelineKey
Unique identifier for pipeline caching.

**Fields**:
- `vertexFormat: String` - "pos3_norm3_col3_uv2"
- `indexFormat: String?` - "uint32" or null
- `materialType: String` - "MeshBasicMaterial"
- `materialHash: Int` - hash of material properties

**Validation Rules**:
- MUST be deterministic (same geometry+material → same key)
- MUST implement `hashCode()` and `equals()` for Map key

**Hash Calculation**:
```kotlin
override fun hashCode(): Int {
    var result = vertexFormat.hashCode()
    result = 31 * result + (indexFormat?.hashCode() ?: 0)
    result = 31 * result + materialType.hashCode()
    result = 31 * result + materialHash
    return result
}
```

---

### PipelineCache
Synchronous cache for compiled render pipelines.

**Fields**:
- `cache: MutableMap<PipelineKey, RenderPipeline>` - pipeline storage
- `hits: Int` - cache hit counter
- `misses: Int` - cache miss counter
- `maxSize: Int` - maximum cache entries (default 100)

**Operations**:
- `get(key: PipelineKey): RenderPipeline?` - O(1) synchronous lookup
- `put(key: PipelineKey, pipeline: RenderPipeline)` - store pipeline
- `clear()` - dispose all pipelines
- `getStats(): CacheStats` - hit rate, size

**Validation Rules**:
- MUST support synchronous access (no suspend functions)
- Eviction: FIFO if `size > maxSize` (future optimization)
- Thread-safe not required (single-threaded JS)

---

### DrawCall
Represents a single GPU draw command.

**Fields**:
- `meshId: String` - unique mesh identifier
- `pipeline: RenderPipeline` - compiled pipeline
- `geometryBuffers: GeometryBuffers` - vertex/index buffers
- `uniformData: Float Array` - MVP matrices (48 floats)
- `triangleCount: Int` - triangles in this draw call
- `indexed: Boolean` - true if using index buffer

**Validation Rules**:
- `pipeline.isReady` MUST be true before draw
- `geometryBuffers` MUST have valid buffers
- `triangleCount` = `indexCount / 3` (indexed) or `vertexCount / 3` (non-indexed)

**Relationships**:
- References `RenderPipeline` (many-to-one)
- References `GeometryBuffers` (many-to-one)
- One per mesh per frame

---

### Frustum
View frustum for visibility culling.

**Fields**:
- `planes: Array<Plane>` - 6 frustum planes (left, right, top, bottom, near, far)
- `projectionViewMatrix: Matrix4` - combined matrix used to extract planes

**Operations**:
- `setFromProjectionMatrix(matrix: Matrix4)` - extract planes from matrix
- `intersectsBox(box: Box3): Boolean` - AABB intersection test

**Validation Rules**:
- `planes` MUST be normalized (plane normal is unit vector)
- MUST be updated every frame if camera moves

---

### PerformanceMetrics
Real-time rendering performance statistics.

**Fields**:
- `fps: Float` - frames per second (rolling average over 60 frames)
- `frameTime: Float` - milliseconds per frame
- `drawCalls: Int` - draw calls in last frame
- `triangles: Int` - triangles rendered in last frame
- `backendType: String` - "WebGPU 1.0" or "WebGL 2.0"
- `gpuMemory: Int` - estimated GPU memory in MB
- `pipelineCacheHits: Int` - pipeline cache hit count
- `pipelineCacheMisses: Int` - pipeline cache miss count
- `culledChunks: Int` - chunks culled by frustum
- `visibleChunks: Int` - chunks rendered

**Validation Rules**:
- `fps` >= 30 (WebGL) or >= 60 (WebGPU) - constitutional requirements
- `drawCalls` < 100 for 81 chunks (FR-005)
- Updated every frame with <0.1ms overhead

**Relationships**:
- Consumed by HUD display
- Logged for performance monitoring

---

## Entity Relationships Diagram

```
RenderBackend (1)
    ├── PipelineCache (1)
    │   └── contains → RenderPipeline (many)
    └── manages → GeometryBuffers (many)
                   ├── VertexBuffer (1)
                   ├── IndexBuffer (0..1)
                   ├── bindGroup/vao (1)
                   └── referenced by → Mesh (many)

DrawCall (many per frame)
    ├── references → RenderPipeline (1)
    ├── references → GeometryBuffers (1)
    └── generates → PerformanceMetrics (aggregated)

Frustum (1 per camera)
    └── culls → Chunk (many)
                  └── has → Mesh (0..1)
                              └── uses → GeometryBuffers (1)
```

---

## Data Flow

### Initialization Flow
```
1. Detect WebGPU availability
2. Create RenderBackend (WebGPU or WebGL)
3. Initialize device/context
4. Create PipelineCache (empty)
5. Pre-compile common pipelines (optional)
```

### Render Frame Flow
```
1. Update Frustum from camera matrices
2. Cull chunks (Frustum.intersectsBox)
3. For each visible chunk:
    a. Get/create GeometryBuffers
    b. Get/create RenderPipeline (from cache)
    c. If pipeline ready:
        - Create DrawCall
        - Bind pipeline
        - Bind buffers (via bindGroup/VAO)
        - Draw
4. Update PerformanceMetrics
5. Update HUD
```

### Dynamic Mesh Update Flow (Block Breaking)
```
1. Block broken → Chunk.setBlock()
2. Chunk marks dirty
3. VoxelWorld.pumpDirtyChunks():
    a. Generate new mesh (ChunkMeshGenerator)
    b. Upload to GPU (async)
    c. Update GeometryBuffers
    d. Old buffers disposed
4. Next frame renders updated mesh
```

---

## Performance Considerations

### Memory Management
- **VertexBuffer**: ~36 bytes/vertex * 4000 vertices/chunk * 81 chunks = ~11.7 MB
- **IndexBuffer**: ~4 bytes/index * 12000 indices/chunk * 81 chunks = ~3.9 MB
- **Total GPU memory**: ~15-20 MB for visible chunks (within budget)

### Cache Sizes
- **PipelineCache**: ~1 KB/pipeline * 10-20 unique pipelines = ~20 KB (negligible)
- **GeometryBuffers**: One per chunk, disposed when chunk unloaded

### Optimization Targets
- **Pipeline cache hit rate**: >95% after first 100 frames
- **Frustum culling effectiveness**: 30-50% fewer draw calls (40-50 visible vs 81 total)
- **Frame budget**: 16.67ms for 60 FPS, <0.5ms for culling+caching overhead
