# Data Model: WebGPU Renderer

**Feature**: Production-Ready WebGPU Renderer
**Date**: 2025-10-06
**Status**: Complete

## Core Entities

### 1. WebGPURenderer

**Purpose**: Main renderer class implementing the Renderer interface with WebGPU backend.

**Fields**:
- `device: GPUDevice` - WebGPU device handle
- `context: GPUCanvasContext` - Canvas rendering context
- `pipelineCache: PipelineCache` - Compiled pipeline cache
- `bufferPool: BufferPool` - Reusable buffer pool
- `textureCache: TextureCache` - Texture resource cache
- `stats: RenderStats` - Frame statistics tracker
- `isInitialized: Boolean` - Initialization state flag
- `contextLossRecovery: ContextLossRecovery` - Recovery manager

**Relationships**:
- Owns one `GPUDevice` instance
- Manages multiple `RenderPipeline` instances via cache
- References one `RenderSurface` (canvas)
- Composes `ContextLossRecovery` for automatic recovery

**Lifecycle**:
1. **Created** → Uninitialized state
2. **initialize(surface)** → Request GPU device, configure context
3. **render(scene, camera)** loop → Frame rendering
4. **dispose()** → Release all GPU resources

**Validation Rules**:
- Must call `initialize()` before first `render()`
- Cannot render after `dispose()` called
- Must handle context loss events gracefully

**State Transitions**:
```
Uninitialized → Initializing → Ready → Rendering → Ready
                                    ↓
                                Disposed (terminal)
                                    ↑
                         ContextLost → Recovering → Ready
```

---

### 2. RenderPipeline

**Purpose**: Encapsulates compiled shader programs and rendering state configuration.

**Fields**:
- `pipeline: GPURenderPipeline` - WebGPU pipeline handle
- `key: PipelineKey` - Unique identifier (shader + state hash)
- `vertexLayout: VertexBufferLayout` - Vertex attribute configuration
- `shaderModules: Pair<ShaderModule, ShaderModule>` - (vertex, fragment)
- `depthStencilState: DepthStencilState` - Depth/stencil configuration
- `primitiveState: PrimitiveState` - Topology, culling, winding
- `blendState: BlendState` - Color blending configuration

**Relationships**:
- Owned by `PipelineCache`
- References two `ShaderModule` instances (vertex + fragment)
- Matches `BufferGeometry` vertex attribute layout

**Lifecycle**:
1. **Created** → Compile shaders, create pipeline
2. **Cached** → Stored in `PipelineCache` (immutable)
3. **Bound** → Active during render pass
4. **Disposed** → Released on context loss or cache clear

**Validation Rules**:
- Vertex layout must match geometry attributes
- Shader stages must be compatible (matching in/out varyings)
- Depth format must match render target

**State**: Immutable after creation (functional/declarative design)

---

### 3. ShaderModule

**Purpose**: Compiled WGSL shader code for vertex or fragment processing.

**Fields**:
- `module: GPUShaderModule` - WebGPU shader module handle
- `source: String` - Original WGSL source code
- `stage: ShaderStage` - VERTEX or FRAGMENT
- `entryPoint: String` - Shader function name (default "main")

**Relationships**:
- Owned by `RenderPipeline`
- Created by `ShaderCompiler`

**Lifecycle**:
1. **Compiled** → WGSL → GPU bytecode
2. **Validated** → Check for syntax errors
3. **Linked** → Attached to pipeline
4. **Disposed** → Released on pipeline disposal

**Validation Rules**:
- Must be valid WGSL syntax
- Entry point must exist in source
- Stage-appropriate inputs/outputs

**State**: Immutable after compilation

---

### 4. GPUBuffer

**Purpose**: Vertex, index, or uniform data storage in GPU-accessible memory.

**Fields**:
- `buffer: GPUBuffer` - WebGPU buffer handle
- `size: Int` - Buffer size in bytes
- `usage: GPUBufferUsage` - VERTEX, INDEX, or UNIFORM
- `descriptor: BufferDescriptor` - Original creation parameters (for recovery)
- `refCount: Int` - Reference counter for automatic disposal

**Relationships**:
- Referenced by `RenderPipeline` during draw calls
- Pooled by `BufferPool`
- Created for each `BufferGeometry` attribute

**Lifecycle**:
1. **Created** → Allocate GPU memory
2. **Uploaded** → Copy data from CPU to GPU
3. **Bound** → Attached to pipeline slot
4. **Released** → Returned to pool or disposed

**State Transitions**:
```
Created → Uploaded → Bound → Unbound → (Bound → Unbound)* → Released
```

**Validation Rules**:
- Size must be non-zero and aligned (4-byte alignment)
- Usage flags must match binding points
- Data type must match shader attribute type

---

### 5. TextureResource

**Purpose**: 2D/3D image data on GPU for material sampling.

**Fields**:
- `texture: GPUTexture` - WebGPU texture handle
- `format: GPUTextureFormat` - Pixel format (RGBA8, RGBA16F, etc.)
- `dimensions: Pair<Int, Int>` - Width × height
- `mipLevels: Int` - Number of mipmap levels
- `usage: GPUTextureUsage` - TEXTURE_BINDING, RENDER_ATTACHMENT
- `descriptor: TextureDescriptor` - Original creation parameters

**Relationships**:
- Referenced by `Material` instances
- Bound to samplers in shader
- Cached by `TextureCache`

**Lifecycle**:
1. **Created** → Allocate GPU memory
2. **Uploaded** → Copy image data
3. **Sampled** → Bound to shader sampler
4. **Disposed** → Release GPU memory

**Validation Rules**:
- Dimensions must be power-of-2 for mipmapping
- Format must match shader sampler type
- Mip levels must be log2(max(width, height))

---

### 6. CommandEncoder

**Purpose**: Records rendering commands for a single frame.

**Fields**:
- `encoder: GPUCommandEncoder` - WebGPU command encoder handle
- `renderPass: GPURenderPassEncoder` - Active render pass
- `clearColor: Color` - Background clear color
- `depthClearValue: Float` - Depth buffer clear value

**Relationships**:
- Created by `WebGPURenderer` per frame
- Records draw commands for all meshes
- Submits to `GPUQueue` at frame end

**Lifecycle** (ephemeral per-frame):
1. **begin()** → Create encoder, start render pass
2. **recordCommands()** → Set pipeline, bind buffers, draw
3. **finish()** → End render pass
4. **submit()** → Submit to GPU queue (encoder destroyed)

**Validation Rules**:
- Must begin render pass before draw commands
- Must finish render pass before submit
- Cannot reuse after submit

---

### 7. PipelineCache

**Purpose**: Stores compiled pipelines to avoid redundant compilation.

**Fields**:
- `cache: Map<PipelineKey, RenderPipeline>` - Hash map of pipelines
- `hitCount: Int` - Cache hit statistics
- `missCount: Int` - Cache miss statistics

**Relationships**:
- Owned by `WebGPURenderer`
- Stores `RenderPipeline` instances

**Lifecycle**:
1. **Created** → Empty cache
2. **getOrCreate(key)** → Lookup or compile new pipeline
3. **clear()** → Invalidate all on context loss

**Validation Rules**:
- Keys must be unique (hash collision handling)
- Cache size typically <50 pipelines

---

### 8. BufferPool

**Purpose**: Reuses GPU buffers to reduce allocation overhead.

**Fields**:
- `pools: Map<BufferSize, Queue<GPUBuffer>>` - Size-class queues
- `acquiredBuffers: Set<GPUBuffer>` - Currently in-use buffers
- `totalAllocated: Int` - Total memory allocated

**Relationships**:
- Owned by `WebGPURenderer`
- Manages `GPUBuffer` lifetimes

**Lifecycle**:
1. **acquire(size, usage)** → Get buffer from pool or create new
2. **release(buffer)** → Return buffer to pool
3. **clear()** → Dispose all pooled buffers

**Validation Rules**:
- Buffer size classes: 256KB, 512KB, 1MB, 2MB, 4MB
- Maximum pool size per class: 10 buffers
- Total memory limit: Device-dependent (query via adapter)

---

### 9. ContextLossRecovery

**Purpose**: Manages automatic recovery from GPU context loss events.

**Fields**:
- `resourceDescriptors: List<ResourceDescriptor>` - Tracked resources
- `isRecovering: Boolean` - Recovery in-progress flag
- `lossCount: Int` - Context loss event counter

**Relationships**:
- Owned by `WebGPURenderer`
- Tracks all `GPUBuffer`, `GPUTexture`, `RenderPipeline` descriptors

**Lifecycle**:
1. **track(descriptor)** → Add resource to recovery list
2. **onContextLoss()** → Trigger recovery process
3. **recover(device)** → Recreate all tracked resources

**Validation Rules**:
- Must track descriptor before GPU resource creation
- Recovery must complete before resuming rendering
- Failed recovery triggers fallback to WebGL

---

## Entity Relationships Diagram

```
WebGPURenderer
├── owns GPUDevice
├── owns PipelineCache
│   └── stores RenderPipeline[]
│       └── owns ShaderModule[] (vertex + fragment)
├── owns BufferPool
│   └── manages GPUBuffer[]
├── owns TextureCache
│   └── stores TextureResource[]
├── owns ContextLossRecovery
│   └── tracks ResourceDescriptor[]
└── creates CommandEncoder (per frame, ephemeral)
    └── records draw commands for Scene
```

---

## Data Flow Sequence

### Rendering a Frame (Happy Path)

```
1. WebGPURenderer.render(scene, camera)
2. → Create CommandEncoder
3. → Begin render pass (clear color/depth)
4. → For each Mesh in scene:
   a. → Get or create RenderPipeline (cache lookup by material + geometry)
   b. → Bind pipeline
   c. → Acquire/upload vertex buffers from pool
   d. → Bind buffers to pipeline
   e. → Record draw command (indexed or non-indexed)
5. → End render pass
6. → Submit command buffer to GPU queue
7. → Release buffers back to pool
8. → Update render statistics
```

### Context Loss Recovery

```
1. GPUDevice.lost promise resolves
2. → ContextLossRecovery.onContextLoss()
3. → WebGPURenderer pauses rendering
4. → Request new GPUDevice
5. → ContextLossRecovery.recover(newDevice)
   a. → Recreate all buffers from descriptors
   b. → Recreate all textures from descriptors
   c. → Recreate all pipelines from descriptors
6. → Resume rendering with new device
7. → Log recovery completion
```

---

## Memory Management

**Buffer Lifecycle**:
- Buffers acquired from pool → bound to pipeline → released to pool
- Pool size limited to 10 buffers per size class
- Automatic disposal when pool full (FIFO eviction)

**Texture Lifecycle**:
- Textures created on-demand → cached by Material → disposed when Material releases
- Reference counting prevents premature disposal
- Cache eviction when GPU memory exceeds 80% capacity

**Pipeline Lifecycle**:
- Pipelines compiled on first use → cached indefinitely
- Cache cleared only on context loss
- Typical scene uses 5-15 unique pipelines

**Total Memory Budget**:
- Buffers: ~50-200MB (1M triangles ≈ 150MB vertex + index data)
- Textures: ~50-500MB (depends on scene complexity)
- Pipelines: ~1-5MB (shader bytecode)
- **Total**: ~100-700MB (utilizes available GPU memory per clarification)

---

## Validation & Constraints

**Type Safety**:
- All GPU handles wrapped in Kotlin classes (no raw external objects exposed)
- Enum types for usage flags, formats, stages
- No runtime type casting required

**Performance Constraints**:
- Pipeline cache hit rate >95% required for 60 FPS
- Buffer pool acquisition <0.1ms per buffer
- Command encoding <5ms per frame (1000 meshes)

**Constitutional Compliance**:
- Memory management prevents leaks (FR-010)
- Reference counting enables automatic disposal
- Buffer pooling reduces allocation overhead
- All entities support context loss recovery

---

**Next Phase**: Generate API contracts and failing tests based on this data model.
