# Data Model: Feature 020

**Feature**: Production-Ready Renderer with Full VoxelCraft Integration
**Date**: 2025-10-07
**Status**: Complete

## Overview

Feature 020 introduces GPU resource entities and rendering pipeline entities needed for full rendering implementation.
All entities use expect/actual pattern for cross-platform compatibility.

## Entity Hierarchy

```
GPU Resource Entities
├── Buffer (abstract)
│   ├── VertexBuffer (position + color data)
│   ├── IndexBuffer (triangle indices)
│   └── UniformBuffer (transformation matrices)
├── RenderPass (clear + draw operations)
└── Swapchain (presentable images)

Rendering Pipeline Entities
├── DrawCommand (buffer bindings + draw call)
├── FrameContext (per-frame rendering state)
└── PipelineState (shader + vertex layout)

VoxelCraft Integration Entities
├── ChunkMeshData (generated mesh from greedy meshing)
└── RenderableChunk (GPU-uploaded chunk mesh)
```

---

## GPU Resource Entities

### 1. BufferHandle

**Purpose**: Platform-agnostic handle to GPU buffer

**Fields**:

- `handle: Any` - Platform-specific buffer handle (VkBuffer on JVM, GPUBuffer on JS)
- `size: Int` - Buffer size in bytes
- `usage: BufferUsage` - Buffer usage flags (VERTEX, INDEX, UNIFORM)

**Validation Rules**:

- `size > 0`
- `handle` must be non-null
- `usage` must be valid enum value

**Platform Implementations**:

```kotlin
// JVM: VkBuffer (Long)
data class BufferHandle(val handle: Long, val memory: Long, val size: Int, val usage: BufferUsage)

// JS: GPUBuffer (dynamic)
data class BufferHandle(val handle: dynamic, val size: Int, val usage: BufferUsage)
```

**Lifecycle**:

1. Create → Allocate GPU memory
2. Bind → Attach to rendering pipeline
3. Update (optional) → Write new data
4. Destroy → Release GPU memory

**Relationships**:

- Referenced by `DrawCommand` for rendering
- Owned by `BufferManager` for lifecycle management

---

### 2. VertexBuffer

**Purpose**: GPU buffer containing vertex position and color data

**Fields**:

- `handle: BufferHandle` - GPU buffer handle
- `vertexCount: Int` - Number of vertices
- `stride: Int` - Bytes per vertex (24 bytes: vec3 position + vec3 color)
- `data: FloatArray` - Vertex data (interleaved)

**Validation Rules**:

- `vertexCount > 0`
- `stride == 24` (6 floats × 4 bytes = 24 bytes)
- `data.size == vertexCount * 6` (position + color)
- All position values finite (not NaN/Infinity)
- All color values in range [0.0, 1.0]

**Data Layout**:

```
Vertex 0: [x, y, z, r, g, b]  // 24 bytes
Vertex 1: [x, y, z, r, g, b]  // 24 bytes
...
```

**Example**:

```kotlin
// Triangle with red, green, blue vertices
val vertexData = floatArrayOf(
    // Position (x, y, z), Color (r, g, b)
    0f, 0f, 0f,  1f, 0f, 0f,  // Vertex 0: origin, red
    1f, 0f, 0f,  0f, 1f, 0f,  // Vertex 1: right, green
    0f, 1f, 0f,  0f, 0f, 1f   // Vertex 2: up, blue
)
val vertexBuffer = VertexBuffer(handle, 3, 24, vertexData)
```

**Lifecycle**:

1. Generate mesh data via `ChunkMeshGenerator`
2. Create vertex buffer via `BufferManager.createVertexBuffer(data)`
3. Bind via `RenderPassManager.bindVertexBuffer(buffer)`
4. Draw via `RenderPassManager.drawIndexed()`
5. Destroy via `BufferManager.destroyBuffer(handle)`

---

### 3. IndexBuffer

**Purpose**: GPU buffer containing triangle indices for indexed rendering

**Fields**:

- `handle: BufferHandle` - GPU buffer handle
- `indexCount: Int` - Number of indices
- `indexType: IndexType` - U16 or U32
- `data: IntArray` - Index data

**Validation Rules**:

- `indexCount > 0`
- `indexCount % 3 == 0` (triangles)
- All indices `< vertexCount` (valid vertex references)
- All indices `>= 0`

**Index Types**:

- `U16` - 16-bit indices (max 65,535 vertices)
- `U32` - 32-bit indices (max 4,294,967,295 vertices)

**Example**:

```kotlin
// Triangle with vertices 0, 1, 2
val indexData = intArrayOf(0, 1, 2)
val indexBuffer = IndexBuffer(handle, 3, IndexType.U32, indexData)
```

**VoxelCraft Usage**:

- Each chunk mesh typically has 1,000-5,000 triangles (3,000-15,000 indices)
- U16 sufficient for single chunks (<65k vertices)
- U32 needed if batching multiple chunks

---

### 4. UniformBuffer

**Purpose**: GPU buffer containing transformation matrices (model, view, projection)

**Fields**:

- `handle: BufferHandle` - GPU buffer handle
- `size: Int` - Buffer size in bytes (minimum 192 bytes for 3× mat4x4)
- `offset: Int` - Current write offset
- `alignment: Int` - Alignment requirement (16 bytes for mat4x4)

**Validation Rules**:

- `size >= 192` (3 matrices × 64 bytes)
- `offset % alignment == 0` (aligned writes)
- `size % alignment == 0` (aligned size)

**Data Layout**:

```
Offset   0: mat4x4 model matrix      (64 bytes)
Offset  64: mat4x4 view matrix       (64 bytes)
Offset 128: mat4x4 projection matrix (64 bytes)
Total: 192 bytes
```

**MVP Approach** (Simplified for Feature 020):

```
Offset 0: mat4x4 modelViewProjection (64 bytes)
Total: 64 bytes (matches basic.wgsl shader)
```

**Update Frequency**:

- **Model Matrix**: Per draw call (different for each chunk)
- **View Matrix**: Per frame (camera moves)
- **Projection Matrix**: On resize (viewport changes)

**Example**:

```kotlin
// Create uniform buffer
val uniformBuffer = bufferManager.createUniformBuffer(64)

// Update MVP matrix per frame
val mvp = projection * view * model
bufferManager.updateUniformBuffer(uniformBuffer, mvp.toByteArray(), offset = 0)
```

---

### 5. RenderPass

**Purpose**: Describes framebuffer clearing and drawing operations

**Fields**:

- `clearColor: Color` - Framebuffer clear color (sky blue for VoxelCraft)
- `depthStencilFormat: DepthFormat?` - Optional depth buffer format (null for MVP)
- `colorAttachments: List<ColorAttachment>` - Color attachments (swapchain images)
- `loadOp: LoadOp` - Load operation (CLEAR or LOAD)
- `storeOp: StoreOp` - Store operation (STORE or DISCARD)

**Validation Rules**:

- `clearColor` valid RGBA (all components in [0.0, 1.0])
- `colorAttachments` non-empty
- `loadOp == CLEAR` requires valid `clearColor`

**Load/Store Operations**:

- **LOAD**: Preserve previous framebuffer contents
- **CLEAR**: Clear framebuffer to `clearColor`
- **STORE**: Write render pass output to attachment
- **DISCARD**: Discard render pass output (don't write)

**VoxelCraft Configuration**:

```kotlin
val renderPass = RenderPass(
    clearColor = Color(0.53f, 0.81f, 0.92f, 1.0f), // Sky blue
    depthStencilFormat = null, // No depth buffer for MVP
    colorAttachments = listOf(swapchainImageAttachment),
    loadOp = LoadOp.CLEAR,
    storeOp = StoreOp.STORE
)
```

**Lifecycle**:

1. Begin → `RenderPassManager.beginRenderPass(clearColor, framebuffer)`
2. Record → Bind pipelines, buffers, draw calls
3. End → `RenderPassManager.endRenderPass()`

---

### 6. Swapchain

**Purpose**: Manages presentable images for displaying rendered frames

**Fields**:

- `imageCount: Int` - Number of swapchain images (2-3)
- `format: ImageFormat` - Swapchain image format (BGRA8Unorm)
- `presentMode: PresentMode` - Presentation mode (FIFO for vsync)
- `extent: Pair<Int, Int>` - Swapchain dimensions (width, height)
- `images: List<SwapchainImage>` - Swapchain images

**Validation Rules**:

- `imageCount >= 2` (double buffering minimum)
- `extent.first > 0 && extent.second > 0`
- `format` supported by GPU
- `presentMode` supported by GPU

**Present Modes**:

- **IMMEDIATE**: No vsync, tearing possible, lowest latency
- **FIFO**: Vsync, no tearing, stable frame rate (default)
- **MAILBOX**: Vsync, no tearing, lowest latency (if supported)

**VoxelCraft Configuration**:

```kotlin
val swapchain = Swapchain(
    imageCount = 2,  // Double buffering
    format = ImageFormat.BGRA8Unorm,
    presentMode = PresentMode.FIFO,  // Vsync (matches RendererConfig default)
    extent = 800 to 600
)
```

**Resize Handling**:

```kotlin
// On window resize
swapchain.recreate(newWidth, newHeight)
// Recreates swapchain with new extent
// Framebuffers also need recreation
```

---

## Rendering Pipeline Entities

### 7. DrawCommand

**Purpose**: Encapsulates buffer bindings and draw call parameters

**Fields**:

- `vertexBuffer: BufferHandle` - Vertex buffer to bind
- `indexBuffer: BufferHandle` - Index buffer to bind
- `uniformBuffer: BufferHandle` - Uniform buffer to bind
- `indexCount: Int` - Number of indices to draw
- `firstIndex: Int` - First index to start drawing from
- `instanceCount: Int` - Number of instances (1 for non-instanced)

**Validation Rules**:

- All buffer handles valid
- `indexCount > 0`
- `indexCount % 3 == 0` (triangles)
- `firstIndex >= 0`
- `instanceCount > 0`

**Execution**:

```kotlin
fun execute(renderPassManager: RenderPassManager) {
    renderPassManager.bindVertexBuffer(vertexBuffer)
    renderPassManager.bindIndexBuffer(indexBuffer)
    renderPassManager.bindUniformBuffer(uniformBuffer)
    renderPassManager.drawIndexed(indexCount, firstIndex, instanceCount)
}
```

**VoxelCraft Usage**:

```kotlin
// Draw single chunk
val drawCommand = DrawCommand(
    vertexBuffer = chunk.vertexBufferHandle,
    indexBuffer = chunk.indexBufferHandle,
    uniformBuffer = cameraUniformBuffer,
    indexCount = chunk.triangleCount * 3,
    firstIndex = 0,
    instanceCount = 1
)
drawCommand.execute(renderPassManager)
```

---

### 8. FrameContext

**Purpose**: Tracks per-frame rendering state

**Fields**:

- `frameNumber: Long` - Current frame number (for FPS tracking)
- `swapchainImage: SwapchainImage` - Current swapchain image
- `commandBuffer: CommandBufferHandle` - Platform-specific command buffer
- `renderPass: RenderPassHandle` - Active render pass

**Validation Rules**:

- `frameNumber >= 0`
- All handles valid
- `swapchainImage` not null during rendering

**Lifecycle**:

```kotlin
// Begin frame
val frameContext = FrameContext(
    frameNumber = currentFrame,
    swapchainImage = swapchain.acquireNextImage(),
    commandBuffer = commandPool.allocate(),
    renderPass = activeRenderPass
)

// Record rendering
frameContext.commandBuffer.begin()
renderPassManager.beginRenderPass(...)
// ... draw calls ...
renderPassManager.endRenderPass()
frameContext.commandBuffer.end()

// Submit and present
graphicsQueue.submit(frameContext.commandBuffer)
swapchain.presentImage(frameContext.swapchainImage)
```

---

### 9. PipelineState

**Purpose**: Complete graphics pipeline state (shaders, vertex layout, etc.)

**Fields**:

- `shaderModule: ShaderModuleHandle` - Compiled shader (WGSL/SPIR-V)
- `vertexLayout: VertexLayout` - Vertex attribute descriptions
- `topology: PrimitiveTopology` - Primitive type (TRIANGLE_LIST)
- `cullMode: CullMode` - Face culling mode (BACK or NONE)
- `frontFace: FrontFace` - Front-facing orientation (CCW or CW)

**Validation Rules**:

- `shaderModule` valid
- `vertexLayout` matches shader vertex inputs
- `topology` supported by GPU

**Vertex Layout** (matches basic.wgsl):

```kotlin
val vertexLayout = VertexLayout(
    stride = 24, // 6 floats × 4 bytes
    attributes = listOf(
        VertexAttribute(location = 0, format = Format.FLOAT3, offset = 0),  // position
        VertexAttribute(location = 1, format = Format.FLOAT3, offset = 12)  // color
    )
)
```

**VoxelCraft Configuration**:

```kotlin
val pipelineState = PipelineState(
    shaderModule = loadBasicShader(),
    vertexLayout = vertexLayout,
    topology = PrimitiveTopology.TRIANGLE_LIST,
    cullMode = CullMode.BACK,  // Cull back faces (performance optimization)
    frontFace = FrontFace.CCW  // Counter-clockwise front face
)
```

---

## VoxelCraft Integration Entities

### 10. ChunkMeshData

**Purpose**: Generated mesh data from greedy meshing algorithm (Feature 017)

**Fields**:

- `vertices: FloatArray` - Vertex data (position + color, interleaved)
- `indices: IntArray` - Triangle indices
- `triangleCount: Int` - Number of triangles
- `blockTypes: Map<BlockType, Int>` - Block type triangle counts

**Source**: `ChunkMeshGenerator.kt` (already implemented in Feature 017)

**Validation Rules**:

- `vertices.size % 6 == 0` (6 floats per vertex)
- `indices.size == triangleCount * 3`
- All indices `< vertices.size / 6`

**Lifecycle**:

1. Generate → `ChunkMeshGenerator.generateMesh(chunk)`
2. Upload → `bufferManager.createVertexBuffer(meshData.vertices)`
3. Render → `drawCommand.execute(renderPassManager)`

---

### 11. RenderableChunk

**Purpose**: GPU-uploaded chunk mesh ready for rendering

**Fields**:

- `chunkPosition: Vector3<Int>` - Chunk position in world
- `vertexBuffer: BufferHandle` - Uploaded vertex data
- `indexBuffer: BufferHandle` - Uploaded index data
- `triangleCount: Int` - Number of triangles
- `visible: Boolean` - Frustum culling flag

**Validation Rules**:

- Both buffer handles valid
- `triangleCount > 0` (non-empty chunk)
- `visible` updated per frame (frustum culling)

**Creation**:

```kotlin
fun uploadChunk(chunk: Chunk, meshData: ChunkMeshData): RenderableChunk {
    val vertexBuffer = bufferManager.createVertexBuffer(meshData.vertices)
    val indexBuffer = bufferManager.createIndexBuffer(meshData.indices)
    return RenderableChunk(
        chunkPosition = chunk.position,
        vertexBuffer = vertexBuffer,
        indexBuffer = indexBuffer,
        triangleCount = meshData.triangleCount,
        visible = true
    )
}
```

**Rendering**:

```kotlin
fun renderWorld(chunks: List<RenderableChunk>) {
    for (chunk in chunks) {
        if (!chunk.visible) continue  // Frustum culling

        val drawCommand = DrawCommand(
            vertexBuffer = chunk.vertexBuffer,
            indexBuffer = chunk.indexBuffer,
            uniformBuffer = cameraUniformBuffer,
            indexCount = chunk.triangleCount * 3,
            firstIndex = 0,
            instanceCount = 1
        )
        drawCommand.execute(renderPassManager)
    }
}
```

---

## Entity Relationships

```
VoxelWorld
    ├── contains many Chunks
    │   └── generates ChunkMeshData (via ChunkMeshGenerator)
    │       └── uploads to RenderableChunk
    │           ├── references VertexBuffer (GPU)
    │           └── references IndexBuffer (GPU)
    │
    └── renders with Camera
        └── provides UniformBuffer (MVP matrix)

Renderer
    ├── manages BufferManager
    │   ├── creates VertexBuffer
    │   ├── creates IndexBuffer
    │   └── creates UniformBuffer
    │
    ├── manages RenderPassManager
    │   ├── uses RenderPass
    │   └── executes DrawCommands
    │
    └── manages SwapchainManager
        └── presents SwapchainImages

FrameContext
    ├── references SwapchainImage (from Swapchain)
    ├── references CommandBuffer (platform-specific)
    └── references RenderPass (from RenderPassManager)
```

---

## State Transitions

### Buffer Lifecycle

```
[UNALLOCATED] --create()--> [ALLOCATED] --bind()--> [BOUND] --destroy()--> [DESTROYED]
                                 |
                                 +--update()--> [ALLOCATED] (data changed)
```

### Render Pass Lifecycle

```
[IDLE] --beginRenderPass()--> [RECORDING] --draw()--> [RECORDING] --endRenderPass()--> [COMPLETE]
                                   |
                                   +--bind*()--> [RECORDING] (bind resources)
```

### Swapchain Lifecycle

```
[CREATED] --acquireImage()--> [IMAGE_ACQUIRED] --present()--> [PRESENTED] --acquireImage()--> ...
    |
    +--resize()--> [RECREATING] ---> [CREATED] (new swapchain)
```

---

## Performance Considerations

### Buffer Uploads

- **Vertex/Index Buffers**: Upload once per chunk generation, immutable
- **Uniform Buffers**: Update per frame (view/projection) or per draw call (model)
- **Optimization**: Batch uniform updates, minimize GPU memory copies

### Draw Calls

- **VoxelCraft**: 1 draw call per visible chunk (~50-100 chunks in view frustum)
- **Optimization**: Instance rendering (deferred to Phase 2-13)

### Memory Usage

- **Per Chunk**: ~50KB vertices + ~30KB indices = ~80KB
- **1,024 Chunks**: ~80MB total GPU memory
- **Well within 500MB constitutional limit**

---

## Platform-Specific Notes

### Vulkan (JVM)

- **Buffers**: VkBuffer + VkDeviceMemory (manual memory management)
- **Command Buffers**: VkCommandBuffer (explicit recording)
- **Synchronization**: VkFence + VkSemaphore (explicit)

### WebGPU (JS)

- **Buffers**: GPUBuffer (automatic memory management)
- **Command Buffers**: GPUCommandEncoder (implicit submission)
- **Synchronization**: Promises (implicit)

---

**Data Model Complete**: ✅ Ready for contract generation
