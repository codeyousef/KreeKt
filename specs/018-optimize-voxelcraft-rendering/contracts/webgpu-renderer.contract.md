# WebGPU Renderer Contract

**Feature**: 018-optimize-voxelcraft-rendering
**Functional Requirements**: FR-001, FR-002, FR-004, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-012, FR-013, FR-014, FR-015

## Contract Interface

```kotlin
interface WebGPURendererContract {
    /**
     * FR-001: Backend detection
     * Detects if WebGPU is available in the current browser.
     * @return true if WebGPU is supported and available
     */
    suspend fun detectWebGPU(): Boolean

    /**
     * FR-002, FR-015: Renderer initialization
     * Initializes the WebGPU renderer with GPU device and canvas context.
     * MUST complete within 2000ms (constitutional requirement).
     * @return RendererResult.Success or RendererResult.Error
     */
    suspend fun initialize(): RendererResult<Unit>

    /**
     * FR-004, FR-005: Scene rendering with draw call batching
     * Renders the scene using WebGPU with <100 draw calls for 81 chunks.
     * MUST achieve 60 FPS with capable hardware (constitutional requirement).
     * @param scene The scene graph to render
     * @param camera The camera for view/projection matrices
     * @return RendererResult.Success or RendererResult.Error
     */
    fun render(scene: Scene, camera: Camera): RendererResult<Unit>

    /**
     * FR-006: Pipeline caching
     * Gets an existing pipeline from cache or creates a new one asynchronously.
     * MUST return cached pipeline synchronously if available.
     * @param geometry The geometry defining vertex layout
     * @param material The material defining shader and render state
     * @return GPURenderPipeline if ready, null if still compiling
     */
    fun getOrCreatePipeline(geometry: BufferGeometry, material: Material): GPURenderPipeline?

    /**
     * FR-007: Asynchronous geometry upload
     * Uploads geometry data to GPU buffers without blocking render loop.
     * @param geometry The geometry to upload
     * @return GeometryBuffers containing GPU buffers and metadata
     */
    suspend fun uploadGeometry(geometry: BufferGeometry): GeometryBuffers

    /**
     * FR-008: Frustum culling
     * Filters chunk list to only visible chunks based on camera frustum.
     * MUST reduce draw calls for off-screen chunks.
     * @param chunks All chunks in the world
     * @param camera The camera defining the view frustum
     * @return Filtered list of visible chunks
     */
    fun cullInvisibleChunks(chunks: List<Chunk>, camera: Camera): List<Chunk>

    /**
     * FR-012: Performance metrics
     * Returns current rendering statistics for performance monitoring.
     * MUST include FPS, draw calls, triangle count, and backend type.
     * @return RenderStats with current metrics
     */
    fun getStats(): RenderStats

    /**
     * FR-013: Dynamic mesh updates
     * Updates geometry buffers for a specific geometry (e.g., block breaking).
     * MUST not block render loop or cause frame drops.
     * @param geometryId The unique identifier of the geometry to update
     * @param geometry The new geometry data
     * @return RendererResult.Success or RendererResult.Error
     */
    fun updateGeometry(geometryId: String, geometry: BufferGeometry): RendererResult<Unit>

    /**
     * FR-014: Resource cleanup
     * Disposes all GPU resources (buffers, pipelines, textures).
     * MUST prevent memory leaks.
     * @return RendererResult.Success or RendererResult.Error
     */
    fun dispose(): RendererResult<Unit>
}
```

## Contract Test Requirements

### Performance Tests (FR-002, FR-005, FR-008)
- Measure FPS over 2+ seconds with 81 chunks loaded
- Assert FPS >= 60 for WebGPU backend
- Assert draw calls < 100 for 81 chunks
- Assert frustum culling reduces draw calls by ~40% when camera sees half the world

### Initialization Tests (FR-001, FR-015)
- Validate WebGPU detection on Chrome 113+
- Measure initialization time, assert < 2000ms
- Verify graceful handling of WebGPU unavailable

### Pipeline Caching Tests (FR-006)
- Create same geometry+material twice, assert cache hit
- Measure cache lookup time, assert < 1ms
- Validate pipeline ready state before rendering

### Buffer Management Tests (FR-007, FR-013, FR-014)
- Upload geometry, verify buffers created
- Update geometry (block breaking), verify no frame drops
- Dispose renderer, verify no memory leaks (check buffer counts)

## Acceptance Criteria

**FR-002**: WebGPU renders at 60 FPS with 81 chunks and ~160K triangles
**FR-005**: Draw call count < 100 for 81 chunks
**FR-008**: Frustum culling reduces draw calls by 30-50% depending on camera orientation
**FR-015**: Renderer initialization completes in <2000ms (typically 50-100ms)
