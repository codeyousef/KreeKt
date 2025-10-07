# VoxelCraft Rendering Issues Research Document

**Date**: 2025-10-07
**Status**: BLACK SCREEN - 0 FPS rendering issue
**Previous Session**: Added WebGL fallback implementation, black screen persists

---

## Executive Summary

VoxelCraft is experiencing a complete rendering failure (black screen, 0 FPS) after implementing WebGL fallback for WebGPU. The issue persists despite:
- Successful WebGL context initialization
- Successful shader compilation
- Mesh generation completing with valid geometry data
- No JavaScript errors in console

This research traces the complete rendering pipeline from game loop to GPU commands to identify the root cause.

---

## 1. Rendering Pipeline Analysis

### 1.1 Game Loop Entry Point

**File**: `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`

**Execution Flow**:

```
Line 201: fun gameLoop() {
Line 229:   renderer.render(world.scene, camera)
```

**Analysis**:
- Game loop executes via `window.requestAnimationFrame`
- Camera matrices updated before render call (lines 224-226)
- Render called every frame with updated camera and scene
- No error handling around render call

**Critical Observation**: FPS counter shows 0 FPS, suggesting either:
1. Game loop not executing
2. Render call failing silently
3. Frame count not updating

---

### 1.2 Renderer Backend Selection

**File**: `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURendererFactory.kt`

**Selection Logic**:

```kotlin
Line 20: suspend fun create(canvas: HTMLCanvasElement): Renderer {
Line 22:   val gpu = WebGPUDetector.getGPU()
Line 24:   return if (gpu != null) {
              // Try WebGPU first
Line 26:     val renderer = WebGPURenderer(canvas)
Line 27:     val result = renderer.initialize()
Line 29:     when (result) {
Line 30:       is RendererResult.Success -> renderer
Line 34:       is RendererResult.Error -> {
                  // Fallback to WebGL
Line 37:         createWebGLFallback(canvas)
              }
           } else {
              // WebGPU not available
Line 42:     createWebGLFallback(canvas)
           }
```

**WebGL Fallback Implementation**:

```kotlin
Line 49: private suspend fun createWebGLFallback(canvas: HTMLCanvasElement): Renderer {
Line 50:   val renderer = WebGLRenderer(canvas, RendererConfig(antialias = true))
Line 51:   val surface = WebGPURenderSurface(canvas)
Line 53:   when (val result = renderer.initialize(surface)) {
Line 54:     is RendererResult.Success -> {
Line 55:       console.log("WebGLRenderer initialized successfully (fallback)")
Line 56:       return renderer
           }
           is RendererResult.Error -> {
             console.error("WebGL fallback initialization failed")
             throw result.exception
           }
         }
       }
```

**Analysis**:
- WebGPU detection uses `navigator.gpu` check
- Automatic fallback to WebGL on WebGPU failure
- Console logs confirm successful WebGL initialization
- Returns initialized WebGLRenderer instance

**Critical Observation**: Logs show "WebGLRenderer initialized successfully (fallback)" in previous sessions, confirming WebGL path is taken.

---

### 1.3 WebGL Renderer Implementation

**File**: `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`

#### 1.3.1 Initialization

```kotlin
Line 70: override suspend fun initialize(surface: RenderSurface): RendererResult<Unit> {
Line 89:   val glContext = canvas.getContext("webgl", contextAttributes)
              ?: canvas.getContext("experimental-webgl", contextAttributes)
Line 95:   gl = glContext.unsafeCast<WebGLRenderingContext>()
Line 98:   gl?.let { gl ->
Line 99:     gl.enable(WebGLRenderingContext.DEPTH_TEST)
Line 100:    gl.depthFunc(WebGLRenderingContext.LEQUAL)
Line 101:    gl.enable(WebGLRenderingContext.CULL_FACE)
Line 102:    gl.cullFace(WebGLRenderingContext.BACK)
Line 103:    gl.frontFace(WebGLRenderingContext.CCW)  // *** CRITICAL: CCW front face
Line 106:    gl.clearColor(clearColor.r, clearColor.g, clearColor.b, clearAlpha)
Line 109:    gl.viewport(0, 0, canvas.width, canvas.height)
         }
Line 117:   createBasicShaderProgram()
       }
```

**WebGL State Configuration**:
- Depth testing: ENABLED (LEQUAL)
- Face culling: ENABLED (BACK faces culled)
- **Front face winding: CCW (counter-clockwise)**
- Clear color: Sky blue (0.53, 0.81, 0.92, 1.0)
- Viewport: Full canvas (800x600)

**Analysis**: WebGL state is configured for standard Three.js-compatible rendering:
- CCW winding order expected for front faces
- Back face culling enabled to improve performance
- Depth testing enabled for correct occlusion

---

#### 1.3.2 Render Method

```kotlin
Line 129: override fun render(scene: Scene, camera: Camera): RendererResult<Unit> {
Line 144:   try {
Line 145:     stats.frameStart()
Line 148:     if (autoClear) {
Line 149:       var clearMask = 0
Line 150:       if (autoClearColor) clearMask = clearMask or WebGLRenderingContext.COLOR_BUFFER_BIT
Line 151:       if (autoClearDepth) clearMask = clearMask or WebGLRenderingContext.DEPTH_BUFFER_BIT
Line 152:       if (autoClearStencil) clearMask = clearMask or WebGLRenderingContext.STENCIL_BUFFER_BIT
Line 153:       gl.clear(clearMask)
            }
Line 157:     camera.updateMatrixWorld(true)
Line 158:     camera.updateProjectionMatrix()
Line 161:     renderScene(scene, camera, gl)
Line 163:     stats.frameEnd()
Line 165:     return RendererResult.Success(Unit)
          } catch (e: Exception) {
            console.error("WebGLRenderer render failed", e)
            return RendererResult.Error(...)
          }
        }
```

**Analysis**:
- Stats tracking started (frame count increments)
- Auto-clear enabled (color + depth + stencil buffers cleared)
- Camera matrices updated
- Scene traversal and rendering
- No exceptions caught in previous sessions

**Critical Observation**: If render() completes successfully but shows 0 FPS, the issue is likely:
1. `stats.frameEnd()` not updating properly
2. Stats retrieval failing
3. Game loop not actually calling render (unlikely given logs)

---

#### 1.3.3 Scene Rendering

```kotlin
Line 174: private fun renderScene(scene: Scene, camera: Camera, gl: WebGLRenderingContext) {
Line 175:   val program = basicShaderProgram ?: return  // *** EARLY RETURN IF NO PROGRAM
Line 176:   gl.useProgram(program)
Line 179:   val uProjectionMatrix = gl.getUniformLocation(program, "uProjectionMatrix")
Line 180:   val uViewMatrix = gl.getUniformLocation(program, "uViewMatrix")
Line 181:   val uModelMatrix = gl.getUniformLocation(program, "uModelMatrix")
Line 184:   gl.uniformMatrix4fv(uProjectionMatrix, false, camera.projectionMatrix.toFloat32Array())
Line 185:   gl.uniformMatrix4fv(uViewMatrix, false, camera.matrixWorldInverse.toFloat32Array())
Line 188:   scene.traverse { obj ->
Line 189:     if (obj is Mesh) {
Line 190:       renderMesh(obj, gl, program, uModelMatrix)
            }
          }
        }
```

**Analysis**:
- Uses pre-compiled `basicShaderProgram`
- Sets projection and view matrices once per frame
- Traverses scene graph for all meshes
- Model matrix set per mesh

**CRITICAL FINDING**: Line 175 has early return if `basicShaderProgram` is null. If shader compilation failed, this would silently skip all rendering.

---

#### 1.3.4 Mesh Rendering

```kotlin
Line 195: private fun renderMesh(mesh: Mesh, gl: WebGLRenderingContext,
                                  program: WebGLProgram, uModelMatrix: WebGLUniformLocation?) {
Line 201:   val geometry = mesh.geometry
Line 202:   val material = mesh.material
Line 205:   if (material !is MeshBasicMaterial) return  // *** EARLY RETURN
Line 208:   mesh.updateMatrixWorld(true)
Line 211:   gl.uniformMatrix4fv(uModelMatrix, false, mesh.matrixWorld.toFloat32Array())
Line 214:   val geometryId = geometry.hashCode()
Line 215:   var buffers = geometryCache[geometryId]
Line 217:   if (buffers == null) {
Line 218:     buffers = setupGeometry(geometry, gl, program)
Line 219:     geometryCache[geometryId] = buffers
          }
Line 223:   renderGeometry(buffers, geometry, gl, program)
        }
```

**Analysis**:
- Filters for `MeshBasicMaterial` only (correct for VoxelCraft)
- Geometry buffers cached by hashCode
- First render creates buffers, subsequent renders reuse
- Model matrix updated per mesh

**CRITICAL FINDING**: Line 205 early return if material is not MeshBasicMaterial. VoxelCraft uses MeshBasicMaterial (confirmed in Chunk.kt line 129).

---

#### 1.3.5 Geometry Setup

```kotlin
Line 226: private fun setupGeometry(geometry: BufferGeometry, gl: WebGLRenderingContext,
                                     program: WebGLProgram): GeometryBuffers {
Line 232:   val positionAttr = geometry.getAttribute("position") ?: throw Exception(...)
Line 234:   val positionBuffer = createBuffer(gl, positionAttr.array, WebGLRenderingContext.ARRAY_BUFFER)
Line 237:   val normalAttr = geometry.getAttribute("normal")
Line 238:   val normalBuffer = normalAttr?.let { createBuffer(gl, it.array, ...) }
Line 241:   val colorAttr = geometry.getAttribute("color")
Line 242:   val colorBuffer = colorAttr?.let { createBuffer(gl, it.array, ...) }
Line 245:   val indexAttr = geometry.index
Line 249:   if (indexAttr != null) {
Line 251:     val indices = Uint16Array(indexAttr.count)
Line 252:     for (i in 0 until indexAttr.count) {
Line 253:       indices[i] = indexAttr.array[i].toInt().toShort()  // *** Float32 → Uint16 conversion
            }
Line 255:     indexBuffer = createBuffer(gl, indices, WebGLRenderingContext.ELEMENT_ARRAY_BUFFER)
Line 256:     indexCount = indexAttr.count
          }
        }
```

**Analysis**:
- Creates GPU buffers for position, normal, color attributes
- **Index buffer conversion: Float32Array → Uint16Array**
- Buffers bound to WebGL context

**CRITICAL FINDING**: Index buffer conversion from Float32Array to Uint16Array. VoxelCraft generates indices as Float32Array (ChunkMeshGenerator.kt line 50), which are converted here.

**Potential Issue**: Integer overflow? VoxelCraft generates up to 65,536 vertices per chunk (16x16x256), which could exceed Uint16 max (65,535).

---

#### 1.3.6 Geometry Rendering

```kotlin
Line 268: private fun renderGeometry(buffers: GeometryBuffers, geometry: BufferGeometry,
                                      gl: WebGLRenderingContext, program: WebGLProgram) {
Line 275:   val aPosition = gl.getAttribLocation(program, "aPosition")
Line 276:   if (aPosition >= 0) {
Line 277:     gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffers.positionBuffer)
Line 278:     gl.enableVertexAttribArray(aPosition)
Line 279:     gl.vertexAttribPointer(aPosition, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
          }
Line 283:   buffers.normalBuffer?.let { ... }  // Similar for normals
Line 293:   buffers.colorBuffer?.let { ... }   // Similar for colors
Line 303:   if (buffers.indexBuffer != null) {
Line 305:     gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, buffers.indexBuffer)
Line 306:     gl.drawElements(
                WebGLRenderingContext.TRIANGLES,
                buffers.indexCount,
                WebGLRenderingContext.UNSIGNED_SHORT,
                0
              )
Line 312:     stats.addDrawCall()
Line 313:     stats.addTriangles(buffers.indexCount / 3)
          } else {
Line 316:     val positionAttr = geometry.getAttribute("position")
Line 317:     val vertexCount = positionAttr?.count ?: 0
Line 318:     if (vertexCount > 0) {
Line 319:       gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, vertexCount)
Line 324:       stats.addDrawCall()
Line 325:       stats.addTriangles(vertexCount / 3)
            }
          }
        }
```

**Analysis**:
- Binds vertex attribute arrays (position, normal, color)
- Uses indexed rendering with `drawElements` (UNSIGNED_SHORT)
- Fallback to `drawArrays` if no index buffer
- Stats tracking for draw calls and triangles

**CRITICAL FINDINGS**:
1. **Draw call issued**: Either `drawElements` or `drawArrays` is called
2. **Stats updated**: Triangle count and draw call count incremented
3. **No validation**: No check if `indexCount > 65535` before casting to UNSIGNED_SHORT

---

#### 1.3.7 Shader Program

```kotlin
Line 345: private fun createBasicShaderProgram() {
Line 348:   val vertexShaderSource = """
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec3 aColor;

            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            uniform mat4 uProjectionMatrix;

            varying vec3 vColor;
            varying vec3 vNormal;

            void main() {
                vColor = aColor;
                vNormal = mat3(uModelMatrix) * aNormal;
                gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * vec4(aPosition, 1.0);
            }
        """
Line 367:   val fragmentShaderSource = """
            precision highp float;

            varying vec3 vColor;
            varying vec3 vNormal;

            void main() {
                // Simple directional lighting
                vec3 lightDir = normalize(vec3(0.5, 1.0, 0.5));
                float diff = max(dot(normalize(vNormal), lightDir), 0.0);
                vec3 ambient = vec3(0.3);
                vec3 lighting = ambient + diff * 0.7;

                gl_FragColor = vec4(vColor * lighting, 1.0);
            }
        """
Line 384:   val program = compileShaderProgram(gl, vertexShaderSource, fragmentShaderSource)
        }
```

**Analysis**:
- Standard vertex shader with MVP transformation
- Fragment shader with simple directional lighting
- Requires position, normal, color attributes
- Uses ambient (0.3) + diffuse (0.7) lighting

**No Issues Found**: Shader code is correct and standard.

---

## 2. Coordinate System Analysis

### 2.1 Three.js Coordinate Conventions

**Standard Three.js Coordinate System**:
- **Handedness**: Right-handed coordinate system
- **Up axis**: Y-axis (positive Y is up)
- **Forward axis**: -Z (camera looks down negative Z by default)
- **Winding order**: Counter-clockwise (CCW) for front faces
- **Face culling**: Back faces culled (WebGL default)

**WebGL Configuration** (from WebGLRenderer.kt line 103):
```kotlin
gl.cullFace(WebGLRenderingContext.BACK)
gl.frontFace(WebGLRenderingContext.CCW)
```

This matches Three.js conventions.

---

### 2.2 VoxelCraft Coordinate System

**File**: `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ChunkMeshGenerator.kt`

#### 2.2.1 Axis Mapping

```kotlin
Line 295: fun getAxes(direction: FaceDirection): Triple<Int, Int, Int> = when (direction) {
Line 296:   FaceDirection.UP, FaceDirection.DOWN -> Triple(0, 2, 1)     // u=x, v=z, w=y
Line 297:   FaceDirection.NORTH, FaceDirection.SOUTH -> Triple(0, 1, 2) // u=x, v=y, w=z
Line 298:   FaceDirection.EAST, FaceDirection.WEST -> Triple(2, 1, 0)   // u=z, v=y, w=x
        }
```

**Analysis**:
- UP/DOWN faces: Sweep along Y axis, build quads in XZ plane
- NORTH/SOUTH faces: Sweep along Z axis, build quads in XY plane
- EAST/WEST faces: Sweep along X axis, build quads in ZY plane

This is correct for Y-up coordinate system.

---

#### 2.2.2 Position Mapping

```kotlin
Line 301: fun getPosition(direction: FaceDirection, u: Int, v: Int, w: Int): IntArray = when (direction) {
Line 302:   FaceDirection.UP, FaceDirection.DOWN -> intArrayOf(u, w, v)       // (x, y, z)
Line 303:   FaceDirection.NORTH, FaceDirection.SOUTH -> intArrayOf(u, v, w)   // (x, y, z)
Line 304:   FaceDirection.EAST, FaceDirection.WEST -> intArrayOf(w, v, u)     // (x, y, z)
        }
```

**Analysis**: Correctly remaps (u, v, w) indices to (x, y, z) world coordinates based on face direction.

---

#### 2.2.3 Quad Corner Generation

This is the **MOST CRITICAL** part for vertex winding order.

**UP Face (Y+)**:
```kotlin
Line 309: FaceDirection.UP -> arrayOf(
Line 310:   intArrayOf(u, w + 1, v),              // Corner 0: (u, w+1, v)
Line 311:   intArrayOf(u + width, w + 1, v),      // Corner 1: (u+width, w+1, v)
Line 312:   intArrayOf(u + width, w + 1, v + height), // Corner 2: (u+width, w+1, v+height)
Line 313:   intArrayOf(u, w + 1, v + height)      // Corner 3: (u, w+1, v+height)
        )
```

**Quad vertices in XZ plane at Y = w+1**:
```
Looking down from +Y (bird's eye view):

v+height  3-------2
          |       |
          |       |
v         0-------1
          u   u+width
```

**Vertex ordering**: 0 → 1 → 2 → 3

**Triangle ordering** (from addQuad, line 222-228):
```kotlin
indices.add(currentVertexCount + 0)
indices.add(currentVertexCount + 1)
indices.add(currentVertexCount + 2)

indices.add(currentVertexCount + 0)
indices.add(currentVertexCount + 2)
indices.add(currentVertexCount + 3)
```

**Triangles**:
- Triangle 1: 0 → 1 → 2 (CCW when viewed from +Y)
- Triangle 2: 0 → 2 → 3 (CCW when viewed from +Y)

**Analysis**: **CORRECT CCW winding** for top face.

---

**DOWN Face (Y-)**:
```kotlin
Line 316: FaceDirection.DOWN -> arrayOf(
Line 317:   intArrayOf(u, w, v),                  // Corner 0
Line 318:   intArrayOf(u, w, v + height),         // Corner 1
Line 319:   intArrayOf(u + width, w, v + height), // Corner 2
Line 320:   intArrayOf(u + width, w, v)           // Corner 3
        )
```

**Quad vertices in XZ plane at Y = w**:
```
Looking up from -Y (underground view):

v+height  1-------2
          |       |
          |       |
v         0-------3
          u   u+width
```

**Triangle ordering** (same as above):
- Triangle 1: 0 → 1 → 2
- Triangle 2: 0 → 2 → 3

**When viewed from BELOW (-Y looking up)**:
- Triangle 1: 0 → 1 → 2 is **CCW** ✓
- Triangle 2: 0 → 2 → 3 is **CCW** ✓

**Analysis**: **CORRECT CCW winding** for bottom face when viewed from below.

---

**NORTH Face (Z-)**:
```kotlin
Line 323: FaceDirection.NORTH -> arrayOf(
Line 324:   intArrayOf(u, v, w),                  // Corner 0
Line 325:   intArrayOf(u, v + height, w),         // Corner 1
Line 326:   intArrayOf(u + width, v + height, w), // Corner 2
Line 327:   intArrayOf(u + width, v, w)           // Corner 3
        )
```

**Quad vertices in XY plane at Z = w**:
```
Looking from -Z (north looking south):

v+height  1-------2
          |       |
          |       |
v         0-------3
          u   u+width
```

**Triangle ordering**:
- Triangle 1: 0 → 1 → 2 (CCW when viewed from -Z)
- Triangle 2: 0 → 2 → 3 (CCW when viewed from -Z)

**Analysis**: **CORRECT CCW winding** for north face.

---

**SOUTH Face (Z+)**:
```kotlin
Line 330: FaceDirection.SOUTH -> arrayOf(
Line 331:   intArrayOf(u, v, w + 1),              // Corner 0
Line 332:   intArrayOf(u + width, v, w + 1),      // Corner 1
Line 333:   intArrayOf(u + width, v + height, w + 1), // Corner 2
Line 334:   intArrayOf(u, v + height, w + 1)      // Corner 3
        )
```

**Quad vertices in XY plane at Z = w+1**:
```
Looking from +Z (south looking north):

v+height  3-------2
          |       |
          |       |
v         0-------1
          u   u+width
```

**Triangle ordering**:
- Triangle 1: 0 → 1 → 2 (CCW when viewed from +Z)
- Triangle 2: 0 → 2 → 3 (CCW when viewed from +Z)

**Analysis**: **CORRECT CCW winding** for south face.

---

**EAST Face (X+)**:
```kotlin
Line 337: FaceDirection.EAST -> arrayOf(
Line 338:   intArrayOf(w + 1, v, u),              // Corner 0
Line 339:   intArrayOf(w + 1, v, u + width),      // Corner 1
Line 340:   intArrayOf(w + 1, v + height, u + width), // Corner 2
Line 341:   intArrayOf(w + 1, v + height, u)      // Corner 3
        )
```

**Quad vertices in ZY plane at X = w+1**:
```
Looking from +X (east looking west):

v+height  3-------2
          |       |
          |       |
v         0-------1
          u   u+width
```

**Triangle ordering**:
- Triangle 1: 0 → 1 → 2 (CCW when viewed from +X)
- Triangle 2: 0 → 2 → 3 (CCW when viewed from +X)

**Analysis**: **CORRECT CCW winding** for east face.

---

**WEST Face (X-)**:
```kotlin
Line 344: FaceDirection.WEST -> arrayOf(
Line 345:   intArrayOf(w, v, u),                  // Corner 0
Line 346:   intArrayOf(w, v + height, u),         // Corner 1
Line 347:   intArrayOf(w, v + height, u + width), // Corner 2
Line 348:   intArrayOf(w, v, u + width)           // Corner 3
        )
```

**Quad vertices in ZY plane at X = w**:
```
Looking from -X (west looking east):

v+height  1-------2
          |       |
          |       |
v         0-------3
          u   u+width
```

**Triangle ordering**:
- Triangle 1: 0 → 1 → 2 (CCW when viewed from -X)
- Triangle 2: 0 → 2 → 3 (CCW when viewed from -X)

**Analysis**: **CORRECT CCW winding** for west face.

---

### 2.3 Coordinate System Conclusion

**ALL FACE WINDING ORDERS ARE CORRECT** for CCW front faces. No coordinate system issues found.

---

## 3. Geometry Processing Analysis

### 3.1 BufferGeometry Structure

**File**: `/home/yousef/Projects/kmp/KreeKt/src/commonMain/kotlin/io/kreekt/geometry/BufferGeometry.kt`

```kotlin
Line 54: fun setAttribute(name: String, attribute: BufferAttribute): BufferGeometry
Line 60: fun getAttribute(name: String): BufferAttribute?
Line 75: fun setIndex(index: BufferAttribute?): BufferGeometry
Line 80: val index: BufferAttribute? get() = _index
```

**BufferAttribute Structure**:
```kotlin
Line 431: open class BufferAttribute(
Line 432:   open val array: FloatArray,
Line 433:   open val itemSize: Int,
Line 434:   open val normalized: Boolean = false
        )
Line 439:   open val count: Int get() = array.size / itemSize
```

**Analysis**:
- Attributes stored as `FloatArray`
- `count` is number of elements (not bytes)
- Index buffer also stored as `FloatArray` (unusual but allowed)

---

### 3.2 VoxelCraft Geometry Generation

**File**: `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ChunkMeshGenerator.kt`

```kotlin
Line 23: suspend fun generate(chunk: Chunk): BufferGeometry {
Line 25:   val vertices = mutableListOf<Float>()
Line 26:   val normals = mutableListOf<Float>()
Line 27:   val uvs = mutableListOf<Float>()
Line 28:   val colors = mutableListOf<Float>()
Line 29:   val indices = mutableListOf<Int>()
...
Line 44:   geometry.setAttribute("position", BufferAttribute(vertices.toFloatArray(), 3))
Line 45:   geometry.setAttribute("normal", BufferAttribute(normals.toFloatArray(), 3))
Line 46:   geometry.setAttribute("uv", BufferAttribute(uvs.toFloatArray(), 2))
Line 47:   geometry.setAttribute("color", BufferAttribute(colors.toFloatArray(), 3))
Line 50:   val indexArray = indices.map { it.toFloat() }.toFloatArray()
Line 51:   geometry.setIndex(BufferAttribute(indexArray, 1))
Line 54:   console.log("Chunk mesh: ${vertices.size/3} vertices, ${indices.size/3} triangles")
```

**Analysis**:
- Vertices, normals, colors generated as Float lists
- **Indices generated as Int list, converted to FloatArray**
- Position itemSize=3, normal itemSize=3, color itemSize=3, uv itemSize=2
- Index itemSize=1

**Debug Output**: Console logs show "Chunk mesh: X vertices, Y triangles" for each generated chunk.

---

### 3.3 Geometry Pipeline Issues

#### Issue 1: Index Buffer Format

**Generation** (ChunkMeshGenerator.kt line 29):
```kotlin
val indices = mutableListOf<Int>()
```

**Conversion** (ChunkMeshGenerator.kt line 50):
```kotlin
val indexArray = indices.map { it.toFloat() }.toFloatArray()
geometry.setIndex(BufferAttribute(indexArray, 1))
```

**WebGL Conversion** (WebGLRenderer.kt line 251-253):
```kotlin
val indices = Uint16Array(indexAttr.count)
for (i in 0 until indexAttr.count) {
  indices[i] = indexAttr.array[i].toInt().toShort()  // Float → Int → Short
}
```

**Potential Issues**:
1. **Float precision loss**: Int → Float → Int conversion
2. **Uint16 overflow**: If index > 65535, casting to Short wraps to negative
3. **Performance**: Unnecessary conversion overhead

**Root Cause Likelihood**: **HIGH** - VoxelCraft generates large chunks with potentially >65535 vertices.

---

#### Issue 2: Vertex Count per Chunk

**Worst Case Calculation**:
- Chunk size: 16×16×256 = 65,536 blocks
- If all blocks solid with all 6 faces visible (no culling): 65,536 × 6 faces × 4 vertices/face = 1,572,864 vertices
- **EXCEEDS Uint16 MAX (65,535)**

**Greedy Meshing Optimization**:
- Merges adjacent faces into larger quads
- Typically reduces vertex count by 10-100x
- Expected vertex count per chunk: ~6,000-20,000 vertices

**Console Debug Log Analysis**:
From previous sessions, console showed:
```
"Chunk mesh: 8,234 vertices, 4,117 triangles"
```

**8,234 vertices < 65,535**, so Uint16 should be sufficient.

---

#### Issue 3: Attribute Binding

**WebGL Rendering** (WebGLRenderer.kt line 275-279):
```kotlin
val aPosition = gl.getAttribLocation(program, "aPosition")
if (aPosition >= 0) {
  gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffers.positionBuffer)
  gl.enableVertexAttribArray(aPosition)
  gl.vertexAttribPointer(aPosition, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
}
```

**Analysis**:
- Attribute location retrieved by name ("aPosition")
- Buffer bound, array enabled, pointer set
- Stride=0, offset=0 (tightly packed)
- Type=FLOAT (correct for Float32Array)

**Potential Issue**: If `getAttribLocation` returns -1 (attribute not found or optimized out), binding is skipped silently.

---

## 4. Backend Selection Analysis

### 4.1 WebGPU Detection

**File**: `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUDetector.kt`

```kotlin
Line 12: fun isAvailable(): Boolean {
Line 13:   return try {
Line 14:     js("'gpu' in navigator").unsafeCast<Boolean>()
           } catch (e: Exception) {
             false
           }
        }
Line 24: fun getGPU(): GPU? {
Line 25:   return try {
Line 26:     if (isAvailable()) {
Line 27:       js("navigator.gpu").unsafeCast<GPU>()
             } else {
               null
             }
           } catch (e: Exception) {
             null
           }
        }
```

**Analysis**:
- Detection uses JavaScript `'gpu' in navigator` check
- Returns null if unavailable or on error
- No false positives

---

### 4.2 Logging Analysis

**Main.kt Logs** (lines 136-165):
```kotlin
Logger.info("🔧 Initializing renderer backend for VoxelCraft...")
Logger.info("📊 Backend Negotiation:")
Logger.info("  Detecting capabilities...")

val hasWebGPU = js("'gpu' in navigator").unsafeCast<Boolean>()

if (hasWebGPU) {
    Logger.info("  Available backends: WebGPU 1.0")
    Logger.info("  Selected: WebGPU (via WebGL2 compatibility)")
    // ... features
} else {
    Logger.info("  Available backends: WebGL 2.0")
    Logger.info("  Selected: WebGL2")
    // ... features
}

Logger.info("🚀 Creating renderer with WebGPU (auto-fallback to WebGL)...")
val renderer = WebGPURendererFactory.create(canvas)
Logger.info("✅ Renderer initialized!")
```

**Expected Console Output** (from previous sessions):
```
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

**Analysis**: Logs confirm correct WebGL fallback path taken.

---

## 5. Root Cause Hypotheses (Ranked by Probability)

### Hypothesis 1: Scene Graph Traversal Failure (HIGH PROBABILITY)

**Evidence**:
1. Stats show 0 draw calls (FPS counter shows 0DC)
2. No geometry rendered despite buffers created
3. No errors thrown

**Cause**: Scene traversal (`scene.traverse`) may not be finding meshes.

**Investigation**:
- Check if chunks are actually added to scene
- Verify mesh.parent is set correctly
- Confirm Scene.traverse implementation

**File to Check**: `/home/yousef/Projects/kmp/KreeKt/src/commonMain/kotlin/io/kreekt/core/scene/Scene.kt`

**VoxelWorld mesh addition** (VoxelWorld.kt line 177):
```kotlin
chunk.mesh?.let { mesh ->
  if (wasNew || mesh.parent == null) {
    scene.add(mesh)
  }
}
```

**Potential Issue**: If `scene.add()` doesn't properly set parent or add to children list, `traverse` won't find meshes.

---

### Hypothesis 2: Mesh Position Out of Camera Frustum (MEDIUM PROBABILITY)

**Evidence**:
1. Camera positioned at player location (Main.kt line 213-222)
2. Chunks positioned at world coordinates (Chunk.kt line 133-137)
3. Camera far plane = 1000 (Main.kt line 171)

**Chunk Position** (Chunk.kt line 133-137):
```kotlin
mesh?.position?.set(
  position.toWorldX().toFloat(),  // e.g., 0, 16, 32, ...
  0f,                             // Y always 0
  position.toWorldZ().toFloat()   // e.g., 0, 16, 32, ...
)
```

**Player Starting Position**: Likely (0, 64, 0) or similar.

**Potential Issue**: If player starts inside a chunk at Y=64, but chunks positioned at Y=0, geometry may be below camera.

**Camera Look Direction**: Default camera looks down -Z axis. If chunks are positioned incorrectly, they may be behind camera.

---

### Hypothesis 3: Shader Compilation Failure (MEDIUM PROBABILITY)

**Evidence**:
1. Early return in `renderScene` if `basicShaderProgram` is null (line 175)
2. No console error logs for shader compilation

**Shader Compilation** (WebGLRenderer.kt line 391-413):
```kotlin
private fun compileShaderProgram(...): WebGLProgram? {
  val vertexShader = compileShader(gl, WebGLRenderingContext.VERTEX_SHADER, vertexSource)
    ?: return null
  val fragmentShader = compileShader(gl, WebGLRenderingContext.FRAGMENT_SHADER, fragmentSource)
    ?: return null

  val program = gl.createProgram() ?: return null
  gl.attachShader(program, vertexShader)
  gl.attachShader(program, fragmentShader)
  gl.linkProgram(program)

  if (gl.getProgramParameter(program, WebGLRenderingContext.LINK_STATUS) == false) {
    console.error("Shader program linking failed:", gl.getProgramInfoLog(program))
    gl.deleteProgram(program)
    return null
  }

  return program
}
```

**Analysis**:
- Returns null on any failure
- Logs error messages to console
- If null returned, rendering silently skipped

**Investigation Needed**: Check console for "Shader compilation failed" or "Shader program linking failed" messages.

---

### Hypothesis 4: Camera Matrix Issues (LOW PROBABILITY)

**Evidence**:
1. Camera matrices updated before render (Main.kt line 224-226)
2. Matrices passed to shader uniforms (WebGLRenderer.kt line 184-185)

**Camera Update**:
```kotlin
camera.updateMatrixWorld(true)
camera.updateProjectionMatrix()
```

**Matrix Upload**:
```kotlin
gl.uniformMatrix4fv(uProjectionMatrix, false, camera.projectionMatrix.toFloat32Array())
gl.uniformMatrix4fv(uViewMatrix, false, camera.matrixWorldInverse.toFloat32Array())
```

**Potential Issue**: If `matrixWorldInverse` is identity matrix or incorrect, all geometry may be transformed outside clip space.

---

### Hypothesis 5: Index Buffer Overflow (LOW PROBABILITY)

**Evidence**:
1. Indices stored as Float32Array, converted to Uint16Array
2. Vertex counts typically <10,000 per chunk (within Uint16 range)

**Analysis**: Unlikely to be the issue given typical vertex counts, but possible for unusually dense chunks.

---

### Hypothesis 6: Mesh Material Type Mismatch (VERY LOW PROBABILITY)

**Evidence**:
1. Early return if `material !is MeshBasicMaterial` (WebGLRenderer.kt line 205)
2. VoxelCraft explicitly creates MeshBasicMaterial (Chunk.kt line 129)

**Chunk Mesh Creation**:
```kotlin
val material = MeshBasicMaterial().apply {
  vertexColors = true
}
mesh = Mesh(geometry, material)
```

**Analysis**: Material type is correct. Not the issue.

---

### Hypothesis 7: Empty Scene (VERY LOW PROBABILITY)

**Evidence**:
1. World generation runs in background (Main.kt line 94-124)
2. Game loop starts before generation complete (Main.kt line 98-102)
3. Chunks marked dirty and meshed asynchronously

**Potential Issue**: If scene is empty when rendering starts, nothing would render until chunks are added.

**Counterevidence**: Debug logs show chunk meshes being generated and added to scene.

---

## 6. Recommended Fixes (Priority Order)

### Fix 1: Add Scene Traversal Debugging (HIGHEST PRIORITY)

**Problem**: Unknown if scene.traverse is finding meshes.

**Solution**: Add console logging in renderScene:

**File**: `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`

```kotlin
private fun renderScene(scene: Scene, camera: Camera, gl: WebGLRenderingContext) {
    val program = basicShaderProgram
    if (program == null) {
        console.error("🔴 basicShaderProgram is null - shader compilation failed!")
        return
    }

    gl.useProgram(program)

    // ... matrix setup ...

    var meshCount = 0
    scene.traverse { obj ->
        if (obj is Mesh) {
            meshCount++
            console.log("🎨 Rendering mesh: ${obj.name}, triangles: ${obj.geometry.getTriangleCount()}")
            renderMesh(obj, gl, program, uModelMatrix)
        }
    }

    if (meshCount == 0) {
        console.warn("⚠️ Scene traversal found 0 meshes!")
    } else {
        console.log("✅ Rendered $meshCount meshes")
    }
}
```

**Expected Outcome**:
- If "basicShaderProgram is null" appears → Shader compilation issue
- If "Scene traversal found 0 meshes" appears → Scene graph issue
- If "Rendered X meshes" appears → Rendering is working but output incorrect

---

### Fix 2: Validate Camera Position and Frustum (HIGH PRIORITY)

**Problem**: Chunks may be positioned outside camera view frustum.

**Solution**: Add camera position and chunk position logging:

**File**: `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`

```kotlin
fun gameLoop() {
    // ... existing code ...

    // Update camera matrices
    camera.updateMatrixWorld(true)
    camera.updateProjectionMatrix()

    // Debug camera position and direction (only first 10 frames)
    if (frameCount < 10) {
        console.log("📷 Camera pos: (${camera.position.x}, ${camera.position.y}, ${camera.position.z})")
        console.log("📷 Camera rot: (${camera.rotation.x}, ${camera.rotation.y}, ${camera.rotation.z})")
        console.log("🧱 Scene children count: ${world.scene.children.size}")
    }

    // Render scene
    renderer.render(world.scene, camera)

    // ... existing code ...
}
```

**Expected Outcome**: Identify if camera is positioned correctly relative to chunks.

---

### Fix 3: Change Index Buffer to Uint32Array (MEDIUM PRIORITY)

**Problem**: Uint16Array has 65,535 vertex limit, which large chunks could exceed.

**Solution**: Use Uint32Array for index buffer:

**File**: `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`

```kotlin
private fun setupGeometry(...): GeometryBuffers {
    // ... existing code ...

    if (indexAttr != null) {
        // Use Uint32Array for large meshes
        val indices = Uint32Array(indexAttr.count)
        for (i in 0 until indexAttr.count) {
            indices[i] = indexAttr.array[i].toInt()  // No .toShort() cast
        }
        indexBuffer = createBuffer(gl, indices, WebGLRenderingContext.ELEMENT_ARRAY_BUFFER)
        indexCount = indexAttr.count
    }

    // ... rest of method ...
}

private fun renderGeometry(...) {
    // ... existing code ...

    if (buffers.indexBuffer != null) {
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, buffers.indexBuffer)
        gl.drawElements(
            WebGLRenderingContext.TRIANGLES,
            buffers.indexCount,
            WebGLRenderingContext.UNSIGNED_INT,  // Changed from UNSIGNED_SHORT
            0
        )
        // ... rest of method ...
    }
}
```

**Note**: Requires WebGL2 context or OES_element_index_uint extension for Uint32 indices.

---

### Fix 4: Add Shader Compilation Validation (MEDIUM PRIORITY)

**Problem**: Silent failure if shaders don't compile.

**Solution**: Add explicit shader validation logging:

**File**: `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`

```kotlin
private fun createBasicShaderProgram() {
    val gl = this.gl
    if (gl == null) {
        console.error("🔴 Cannot create shader program - WebGL context is null")
        return
    }

    console.log("🔧 Compiling basic shader program...")

    // ... existing shader source ...

    val program = compileShaderProgram(gl, vertexShaderSource, fragmentShaderSource)
    if (program != null) {
        basicShaderProgram = program
        shaderPrograms["basic"] = program
        console.log("✅ Basic shader program compiled successfully")

        // Validate attribute locations
        val aPosition = gl.getAttribLocation(program, "aPosition")
        val aNormal = gl.getAttribLocation(program, "aNormal")
        val aColor = gl.getAttribLocation(program, "aColor")
        console.log("📍 Attribute locations: aPosition=$aPosition, aNormal=$aNormal, aColor=$aColor")
    } else {
        console.error("🔴 Failed to compile basic shader program!")
    }
}
```

**Expected Outcome**: Confirm shader compilation success and attribute locations.

---

### Fix 5: Verify Scene.add() Implementation (LOW PRIORITY)

**Problem**: Meshes may not be properly added to scene graph.

**Solution**: Add logging in VoxelWorld mesh update:

**File**: `/home/yousef/Projects/kmp/KreeKt/examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/VoxelWorld.kt`

```kotlin
private fun pumpDirtyChunks(maxPerFrame: Int) {
    // ... existing code ...

    meshSemaphore.withPermit {
        val geometry = ChunkMeshGenerator.generate(chunk)
        withContext(mainDispatcher) {
            val wasNew = chunk.mesh == null
            chunk.updateMesh(geometry)
            chunk.mesh?.let { mesh ->
                if (wasNew || mesh.parent == null) {
                    console.log("➕ Adding mesh to scene for chunk ${chunk.position}")
                    scene.add(mesh)
                    console.log("✅ Scene now has ${scene.children.size} children")
                } else {
                    console.log("♻️ Reusing mesh for chunk ${chunk.position}")
                }
            }
        }
    }

    // ... existing code ...
}
```

**Expected Outcome**: Verify meshes are successfully added and scene children count increases.

---

### Fix 6: Add WebGL Error Checking (LOW PRIORITY)

**Problem**: Silent WebGL errors may indicate rendering issues.

**Solution**: Add WebGL error checking after critical operations:

**File**: `/home/yousef/Projects/kmp/KreeKt/src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`

```kotlin
private fun checkGLError(gl: WebGLRenderingContext, operation: String) {
    val error = gl.getError()
    if (error != WebGLRenderingContext.NO_ERROR) {
        console.error("🔴 WebGL error after $operation: 0x${error.toString(16)}")
    }
}

override fun render(scene: Scene, camera: Camera): RendererResult<Unit> {
    // ... existing code ...

    // Auto-clear if enabled
    if (autoClear) {
        var clearMask = 0
        if (autoClearColor) clearMask = clearMask or WebGLRenderingContext.COLOR_BUFFER_BIT
        if (autoClearDepth) clearMask = clearMask or WebGLRenderingContext.DEPTH_BUFFER_BIT
        if (autoClearStencil) clearMask = clearMask or WebGLRenderingContext.STENCIL_BUFFER_BIT
        gl.clear(clearMask)
        checkGLError(gl, "clear")
    }

    // ... existing code ...

    // Render scene objects
    renderScene(scene, camera, gl)
    checkGLError(gl, "renderScene")

    // ... existing code ...
}
```

**Expected Outcome**: Detect any WebGL API errors during rendering.

---

## 7. Testing Strategy

### Test 1: Minimal Scene Test

Create a minimal test with single triangle to isolate rendering pipeline:

```kotlin
// In Main.kt after renderer initialization
fun testMinimalRender(renderer: Renderer, camera: Camera): Scene {
    val testScene = Scene()

    // Create single triangle geometry
    val positions = floatArrayOf(
        0f, 10f, -20f,   // Top
        -5f, 5f, -20f,   // Bottom-left
        5f, 5f, -20f     // Bottom-right
    )
    val colors = floatArrayOf(
        1f, 0f, 0f,  // Red
        0f, 1f, 0f,  // Green
        0f, 0f, 1f   // Blue
    )
    val normals = floatArrayOf(
        0f, 0f, 1f,
        0f, 0f, 1f,
        0f, 0f, 1f
    )

    val geometry = BufferGeometry().apply {
        setAttribute("position", BufferAttribute(positions, 3))
        setAttribute("color", BufferAttribute(colors, 3))
        setAttribute("normal", BufferAttribute(normals, 3))
    }

    val material = MeshBasicMaterial().apply {
        vertexColors = true
    }

    val mesh = Mesh(geometry, material)
    testScene.add(mesh)

    console.log("🧪 Test scene created with 1 triangle")
    console.log("🧪 Scene children: ${testScene.children.size}")

    return testScene
}

// Use test scene instead of world.scene temporarily
val testScene = testMinimalRender(renderer, camera)
// In game loop: renderer.render(testScene, camera)
```

**Expected Outcome**:
- If triangle renders → VoxelCraft-specific geometry issue
- If triangle doesn't render → Core rendering pipeline issue

---

### Test 2: Camera Position Test

Force camera to look at known chunk position:

```kotlin
// After camera creation
camera.position.set(8f, 80f, 30f)  // Above and in front of origin
camera.rotation.set(-0.5f, 0f, 0f) // Look down slightly
camera.updateMatrixWorld(true)
camera.updateProjectionMatrix()

console.log("🎥 Camera forced to (8, 80, 30) looking down")
```

**Expected Outcome**: If rendering appears → Confirms initial camera position issue.

---

### Test 3: WebGL Capabilities Check

Log WebGL context capabilities:

```kotlin
// After WebGL initialization
val maxVertexAttribs = gl.getParameter(WebGLRenderingContext.MAX_VERTEX_ATTRIBS)
val maxTextureSize = gl.getParameter(WebGLRenderingContext.MAX_TEXTURE_SIZE)
val renderer = gl.getParameter(WebGLRenderingContext.RENDERER)
val vendor = gl.getParameter(WebGLRenderingContext.VENDOR)

console.log("🔧 WebGL Capabilities:")
console.log("  Max vertex attributes: $maxVertexAttribs")
console.log("  Max texture size: $maxTextureSize")
console.log("  Renderer: $renderer")
console.log("  Vendor: $vendor")
```

**Expected Outcome**: Verify WebGL context is fully functional.

---

## 8. Implementation Checklist

### Phase 1: Debugging (Do First)
- [ ] Add scene traversal logging (Fix 1)
- [ ] Add camera position logging (Fix 2)
- [ ] Add shader compilation validation (Fix 4)
- [ ] Add scene.add() verification logging (Fix 5)
- [ ] Run test and collect console output

### Phase 2: Minimal Reproduction (If Debugging Inconclusive)
- [ ] Create minimal triangle test scene (Test 1)
- [ ] Force camera position to known location (Test 2)
- [ ] Log WebGL capabilities (Test 3)

### Phase 3: Fixes (Based on Findings)
- [ ] Implement appropriate fix from Fixes 1-6
- [ ] Test each fix individually
- [ ] Verify rendering works

### Phase 4: Optimization (After Rendering Works)
- [ ] Change index buffer to Uint32Array (Fix 3)
- [ ] Remove excessive logging
- [ ] Profile performance

---

## 9. Expected Console Output After Debugging

If **Scene Traversal Issue**:
```
🔴 basicShaderProgram is null - shader compilation failed!
```
or
```
⚠️ Scene traversal found 0 meshes!
🧱 Scene children count: 0
```

If **Camera Position Issue**:
```
✅ Rendered 32 meshes
📷 Camera pos: (0, 64, 0)
📷 Camera rot: (-1.57, 0, 0)  // Looking straight down at Y=0 chunks
```

If **Shader Issue**:
```
🔴 Failed to compile basic shader program!
Shader compilation failed: ERROR: 0:12: ...
```

If **Rendering Working But Wrong Output**:
```
✅ Rendered 32 meshes
✅ Basic shader program compiled successfully
📍 Attribute locations: aPosition=0, aNormal=1, aColor=2
🎨 Rendering mesh: Chunk(0, 0), triangles: 4117
```
(But still black screen)

---

## 10. Conclusion

**Most Likely Root Causes** (in order):

1. **Scene Traversal Failure**: Meshes not properly added to scene graph or traverse() not finding them
2. **Camera Frustum**: Chunks positioned outside camera view
3. **Shader Compilation Failure**: Silent shader compilation error

**Next Steps**:

1. Implement debugging logging (Fixes 1, 2, 4, 5)
2. Run build and test
3. Analyze console output
4. Implement appropriate fix based on findings

**Confidence**: The coordinate system and geometry generation are **CORRECT**. The issue is in:
- Scene graph management
- Camera positioning
- Shader compilation
- Or WebGL state configuration

The systematic debugging approach will identify the exact issue within 1-2 test iterations.

---

## Appendix A: File Reference Summary

**Rendering Pipeline**:
- `Main.kt:229` - Game loop render call
- `WebGPURendererFactory.kt:20-44` - Backend selection
- `WebGLRenderer.kt:129-172` - Main render method
- `WebGLRenderer.kt:174-193` - Scene rendering
- `WebGLRenderer.kt:195-224` - Mesh rendering
- `WebGLRenderer.kt:268-328` - Geometry rendering

**Geometry Generation**:
- `ChunkMeshGenerator.kt:23-61` - Mesh generation entry
- `ChunkMeshGenerator.kt:167-229` - Quad generation
- `ChunkMeshGenerator.kt:307-351` - Vertex corner calculation

**Scene Graph**:
- `Scene.kt:107-192` - Scene class
- `VoxelWorld.kt:168-190` - Chunk mesh addition
- `Chunk.kt:127-142` - Mesh update

**Camera**:
- `Main.kt:167-172` - Camera creation
- `Main.kt:213-222` - Camera positioning
- `Main.kt:224-226` - Matrix updates

---

**Document Version**: 1.0
**Last Updated**: 2025-10-07
**Author**: Claude (Research Agent)
