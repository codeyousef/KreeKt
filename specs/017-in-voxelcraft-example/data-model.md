# Data Model: VoxelCraft Rendering System

## Overview
This document defines the data structures and their relationships for fixing VoxelCraft rendering issues. The model focuses on renderer state, geometry representation, and performance metrics.

## Core Entities

### 1. BufferGeometry
**Purpose**: Represents mesh data with vertex attributes and optional index buffer

**Attributes**:
- `attributes: Map<String, BufferAttribute>` - Vertex attributes (position, normal, color, uv)
- `index: BufferAttribute?` - Optional index buffer for indexed rendering
- `drawRange: DrawRange` - Subset of geometry to render
- `groups: List<GeometryGroup>` - Multiple materials/draw calls

**Key Methods**:
- `getAttribute(name: String): BufferAttribute?` - Type-safe attribute access
- `setAttribute(name: String, attribute: BufferAttribute)` - Add/update attribute
- `setIndex(index: BufferAttribute)` - Set index buffer
- `computeBoundingBox()` - Calculate AABB for frustum culling
- `computeBoundingSphere()` - Calculate bounding sphere

**Validation Rules**:
- Position attribute is mandatory (vec3)
- All attributes must have same vertex count
- Index buffer (if present) must reference valid vertex indices
- Index count must be multiple of 3 (triangles)

**State Transitions**:
```
Created → Attributes Added → Indices Set (optional) → Bounds Computed → Ready
```

### 2. BufferAttribute
**Purpose**: Typed array wrapper for GPU buffer data

**Attributes**:
- `array: FloatArray | Uint16Array | Uint32Array` - Underlying data
- `itemSize: Int` - Components per vertex (1-4)
- `count: Int` - Number of elements (array.size / itemSize)
- `normalized: Boolean` - GPU normalization flag

**Validation Rules**:
- array.size must be multiple of itemSize
- count = array.size / itemSize
- For indices: array must be Uint16Array or Uint32Array

**Examples**:
```kotlin
// Position attribute: 3 floats per vertex
BufferAttribute(floatArrayOf(x1, y1, z1, x2, y2, z2, ...), itemSize = 3)

// Color attribute: 3 floats per vertex (RGB)
BufferAttribute(floatArrayOf(r1, g1, b1, r2, g2, b2, ...), itemSize = 3)

// Index attribute: Uint16 indices
BufferAttribute(uint16ArrayOf(0, 1, 2, 0, 2, 3, ...), itemSize = 1)
```

### 3. Mesh
**Purpose**: Renderable object combining geometry, material, and transform

**Attributes**:
- `geometry: BufferGeometry` - Mesh geometry data
- `material: Material` - Rendering material (MeshBasicMaterial for VoxelCraft)
- `position: Vector3` - World position
- `rotation: Euler` - World rotation
- `scale: Vector3` - World scale
- `matrix: Matrix4` - Local transform matrix
- `matrixWorld: Matrix4` - World transform matrix
- `parent: Object3D?` - Parent in scene graph
- `children: List<Object3D>` - Child objects

**Key Methods**:
- `updateMatrix()` - Recompute local matrix from position/rotation/scale
- `updateMatrixWorld(force: Boolean)` - Propagate transforms down scene graph

**Validation Rules**:
- geometry must not be null
- material must not be null
- matrixWorld updated before rendering

**Relationships**:
- 1 Mesh → 1 BufferGeometry
- 1 Mesh → 1 Material
- 1 Mesh → N children (via Object3D hierarchy)

### 4. Scene
**Purpose**: Root container for renderable objects

**Attributes**:
- `children: MutableList<Object3D>` - Top-level scene objects
- `autoUpdate: Boolean` - Auto-update matrices each frame

**Key Methods**:
- `add(object: Object3D)` - Add child to scene
- `remove(object: Object3D)` - Remove child from scene
- `traverse(callback: (Object3D) -> Unit)` - Depth-first traversal
- `updateMatrixWorld(force: Boolean)` - Update all object transforms

**Validation Rules**:
- Must call updateMatrixWorld() before rendering
- traverse() must visit all descendant objects

**Critical for Bug Fix**:
- Verify meshes are properly added via scene.add()
- Verify traverse() correctly visits all Mesh objects

### 5. RenderStats
**Purpose**: Performance metrics tracking

**Attributes**:
- `frame: Int` - Frame counter
- `calls: Int` - Draw calls this frame
- `triangles: Int` - Triangles rendered this frame
- `fps: Float` - Frames per second (derived)

**Key Methods**:
- `frameStart()` - Reset per-frame counters
- `frameEnd()` - Increment frame counter
- `addDrawCall()` - Increment draw call counter
- `addTriangles(count: Int)` - Increment triangle counter
- `reset()` - Reset all counters

**Validation Rules**:
- calls ≥ 0
- triangles ≥ 0
- fps should be 30-60 for VoxelCraft

**Success Criteria**:
- FPS ≥ 30 (minimum acceptable)
- FPS → 60 (constitutional target)
- calls matches expected mesh count
- triangles matches visible geometry

### 6. RendererState
**Purpose**: Internal renderer state tracking

**Attributes**:
- `gl: WebGLRenderingContext` - WebGL context
- `isInitialized: Boolean` - Initialization status
- `isDisposed: Boolean` - Disposal status
- `contextLost: Boolean` - Context loss detection
- `currentViewport: Viewport` - Active viewport
- `basicShaderProgram: WebGLProgram?` - Compiled shader program
- `geometryCache: Map<Int, GeometryBuffers>` - Cached GPU buffers

**State Transitions**:
```
Created → initialize() → Initialized → [render loops] → dispose() → Disposed
                    ↓
              Context Lost ← WebGL context loss event
```

**Critical for Bug Fix**:
- Verify isInitialized = true before rendering
- Verify basicShaderProgram != null (shader compilation succeeded)
- Verify gl.getError() returns NO_ERROR

### 7. GeometryBuffers (Internal)
**Purpose**: GPU-side buffer objects for geometry

**Attributes**:
- `positionBuffer: WebGLBuffer` - Position attribute GPU buffer
- `normalBuffer: WebGLBuffer?` - Normal attribute GPU buffer
- `colorBuffer: WebGLBuffer?` - Color attribute GPU buffer
- `indexBuffer: WebGLBuffer?` - Index GPU buffer
- `indexCount: Int` - Number of indices

**Validation Rules**:
- positionBuffer must not be null
- If indexBuffer != null, use drawElements()
- If indexBuffer == null, use drawArrays()

**Critical for Bug Fix**:
- Verify correct rendering path selected (indexed vs non-indexed)
- Verify buffers bound before draw call

## Relationships Diagram

```
Scene (root)
  └─> children: List<Object3D>
       └─> Mesh (renderable)
            ├─> geometry: BufferGeometry
            │    ├─> attributes: Map<String, BufferAttribute>
            │    │    ├─> "position": BufferAttribute (vec3, mandatory)
            │    │    ├─> "normal": BufferAttribute (vec3, optional)
            │    │    ├─> "color": BufferAttribute (vec3, optional)
            │    │    └─> "uv": BufferAttribute (vec2, optional)
            │    └─> index: BufferAttribute? (Uint16/Uint32)
            │
            ├─> material: Material
            └─> matrixWorld: Matrix4

Renderer
  ├─> gl: WebGLRenderingContext
  ├─> basicShaderProgram: WebGLProgram
  ├─> geometryCache: Map<Int, GeometryBuffers>
  │    └─> GeometryBuffers
  │         ├─> positionBuffer: WebGLBuffer
  │         ├─> normalBuffer: WebGLBuffer?
  │         ├─> colorBuffer: WebGLBuffer?
  │         └─> indexBuffer: WebGLBuffer?
  └─> stats: RenderStats
```

## Data Flow: Render Pipeline

```
1. Game Loop (Main.kt)
   └─> renderer.render(scene, camera)

2. Renderer.render()
   ├─> stats.frameStart()
   ├─> camera.updateMatrixWorld()
   └─> renderScene(scene, camera)

3. Renderer.renderScene()
   ├─> gl.useProgram(basicShaderProgram) [CRITICAL: Must not be null]
   ├─> gl.uniformMatrix4fv(projectionMatrix)
   ├─> gl.uniformMatrix4fv(viewMatrix)
   └─> scene.traverse { obj ->
        if (obj is Mesh) renderMesh(obj) [CRITICAL: Must find meshes]
      }

4. Renderer.renderMesh()
   ├─> mesh.updateMatrixWorld()
   ├─> gl.uniformMatrix4fv(modelMatrix)
   ├─> buffers = geometryCache[geometry.id] ?: setupGeometry(geometry)
   └─> renderGeometry(buffers, geometry)

5. Renderer.setupGeometry()
   ├─> Create WebGLBuffer for position attribute
   ├─> Create WebGLBuffer for normal attribute (if present)
   ├─> Create WebGLBuffer for color attribute (if present)
   ├─> Create WebGLBuffer for index (if present)
   └─> Store in geometryCache

6. Renderer.renderGeometry()
   ├─> Bind position attribute → gl.vertexAttribPointer()
   ├─> Bind normal attribute → gl.vertexAttribPointer()
   ├─> Bind color attribute → gl.vertexAttribPointer()
   └─> Draw:
       ├─> If indexBuffer != null: gl.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)
       └─> Else: gl.drawArrays(TRIANGLES, 0, vertexCount)
```

## Bug Fix Data Validation Points

### Point 1: Scene Graph Integrity
```kotlin
// Before rendering
assert(world.scene.children.isNotEmpty()) { "Scene has no children" }
assert(world.scene.children.any { it is Mesh }) { "Scene has no meshes" }
```

### Point 2: Shader Compilation
```kotlin
// In WebGLRenderer.createBasicShaderProgram()
val program = compileShaderProgram(gl, vertexSource, fragmentSource)
assert(program != null) { "Shader compilation failed" }
basicShaderProgram = program
```

### Point 3: Geometry Validity
```kotlin
// In ChunkMeshGenerator.generate()
val geometry = BufferGeometry()
geometry.setAttribute("position", BufferAttribute(vertices, 3))
assert(vertices.size % 3 == 0) { "Invalid vertex count" }
assert(indices.isEmpty() || indices.size % 3 == 0) { "Invalid index count" }
```

### Point 4: Render Path Selection
```kotlin
// In WebGLRenderer.renderGeometry()
if (buffers.indexBuffer != null) {
    // Indexed rendering
    gl.drawElements(TRIANGLES, buffers.indexCount, UNSIGNED_SHORT, 0)
} else {
    // Non-indexed rendering
    val vertexCount = geometry.getAttribute("position")?.count ?: 0
    assert(vertexCount > 0) { "Zero vertices in non-indexed geometry" }
    gl.drawArrays(TRIANGLES, 0, vertexCount)
}
```

## Performance Constraints

### Memory
- **BufferGeometry**: ~8KB per chunk (8,000 vertices × 3 floats × 4 bytes)
- **GeometryBuffers**: 4× GPU memory (position, normal, color, index buffers)
- **Total**: ~32KB GPU memory per chunk
- **1,024 chunks**: ~32MB GPU memory (acceptable)

### Timing
- **Geometry generation**: <10ms per chunk (greedy meshing)
- **Buffer upload**: <5ms per chunk (GPU transfer)
- **Render**: <16ms per frame (60 FPS target)
- **Frame budget**: 16.67ms for 60 FPS, 33.33ms for 30 FPS

### Draw Calls
- **Target**: 1 draw call per chunk
- **Maximum**: 1,024 draw calls (all chunks visible)
- **Typical**: 32-64 draw calls (frustum culling reduces visible chunks)

## Test Data

### Minimal Test Case
```kotlin
// Single quad (2 triangles)
val positions = floatArrayOf(
    0f, 0f, 0f,  // vertex 0
    1f, 0f, 0f,  // vertex 1
    1f, 1f, 0f,  // vertex 2
    0f, 1f, 0f   // vertex 3
)
val indices = floatArrayOf(0f, 1f, 2f, 0f, 2f, 3f) // Converted to Uint16

val geometry = BufferGeometry()
geometry.setAttribute("position", BufferAttribute(positions, 3))
geometry.setIndex(BufferAttribute(indices, 1))

// Expected result: 1 draw call, 2 triangles, visible on screen
```

### Typical Chunk
```kotlin
// From ChunkMeshGenerator output
vertices.size = 24,702 (8,234 vertices)
indices.size = 24,702 (4,117 triangles)

// Expected result: 1 draw call, 4,117 triangles, ~0.25ms render time
```

## Related Files
- `src/jsMain/kotlin/io/kreekt/geometry/BufferGeometry.kt` - BufferGeometry implementation
- `src/jsMain/kotlin/io/kreekt/geometry/BufferAttribute.kt` - BufferAttribute implementation
- `src/jsMain/kotlin/io/kreekt/core/scene/Mesh.kt` - Mesh implementation
- `src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt` - Renderer implementation
- `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ChunkMeshGenerator.kt` - Geometry generation
