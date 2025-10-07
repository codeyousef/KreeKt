# Renderer API Contract

## Overview
This contract defines the expected behavior of the WebGL/WebGPU renderer implementations for the VoxelCraft rendering fix. All implementations must satisfy these contracts.

## Contract 1: Performance Requirement

### Description
Renderer must achieve minimum 30 FPS (target 60 FPS) with typical VoxelCraft geometry load.

### Preconditions
- Renderer initialized successfully
- Scene contains 32-64 visible chunks (typical case)
- Each chunk has ~8,000 vertices, ~4,000 triangles
- Total visible geometry: ~256,000 vertices, ~128,000 triangles

### Postconditions
- Frame time ≤ 33.33ms (30 FPS minimum)
- Frame time ≤ 16.67ms (60 FPS target)
- stats.fps ≥ 30

### Test
```kotlin
@Test
fun testRendererPerformance() {
    val renderer = WebGLRenderer(canvas, RendererConfig(antialias = true))
    val scene = createTestScene(chunkCount = 32)
    val camera = createTestCamera()

    // Warm-up frames
    repeat(10) {
        renderer.render(scene, camera)
    }

    // Measure performance
    val startTime = now()
    repeat(60) {
        renderer.render(scene, camera)
    }
    val endTime = now()

    val avgFrameTime = (endTime - startTime) / 60.0
    val fps = 1000.0 / avgFrameTime

    assertTrue(fps >= 30.0, "Expected FPS >= 30, got $fps")
    // Assert constitutional requirement
    // assertTrue(fps >= 60.0, "Constitutional target FPS >= 60, got $fps")
}
```

### Acceptance Criteria
- ✅ PASS: FPS ≥ 30 for typical geometry load
- ✅ TARGET: FPS ≥ 60 for typical geometry load (constitutional requirement)
- ❌ FAIL: FPS < 30 or renderer hangs

---

## Contract 2: Indexed Geometry Rendering

### Description
Renderer must correctly render BufferGeometry with index buffer using `drawElements()` code path.

### Preconditions
- Renderer initialized
- BufferGeometry has:
  - position attribute (vec3, required)
  - index attribute (Uint16Array, required)
- Index count is multiple of 3

### Postconditions
- `gl.drawElements()` called with correct parameters:
  - mode = TRIANGLES
  - count = indexCount
  - type = UNSIGNED_SHORT
  - offset = 0
- stats.calls incremented by 1
- stats.triangles incremented by indexCount / 3
- Geometry visible on screen

### Test
```kotlin
@Test
fun testIndexedGeometryRendering() {
    val renderer = WebGLRenderer(canvas, RendererConfig())

    // Create indexed geometry (simple quad)
    val positions = floatArrayOf(
        0f, 0f, 0f,
        1f, 0f, 0f,
        1f, 1f, 0f,
        0f, 1f, 0f
    )
    val indices = uint16ArrayOf(0, 1, 2, 0, 2, 3)

    val geometry = BufferGeometry()
    geometry.setAttribute("position", BufferAttribute(positions, 3))
    geometry.setIndex(BufferAttribute(indices.toFloatArray(), 1))

    val mesh = Mesh(geometry, MeshBasicMaterial())
    val scene = Scene().apply { add(mesh) }
    val camera = createTestCamera()

    renderer.render(scene, camera)

    val stats = renderer.getStats()
    assertEquals(1, stats.calls, "Expected 1 draw call")
    assertEquals(2, stats.triangles, "Expected 2 triangles")

    // Visual validation: Capture framebuffer and check non-black pixels
    val pixels = captureFramebuffer(renderer)
    assertTrue(pixels.any { it != 0 }, "Expected visible geometry on screen")
}
```

### Acceptance Criteria
- ✅ PASS: drawElements() called, stats correct, geometry visible
- ❌ FAIL: Wrong draw path, incorrect stats, or black screen

---

## Contract 3: Non-Indexed Geometry Rendering

### Description
Renderer must correctly render BufferGeometry without index buffer using `drawArrays()` code path.

### Preconditions
- Renderer initialized
- BufferGeometry has:
  - position attribute (vec3, required)
  - NO index attribute
- Vertex count is multiple of 3

### Postconditions
- `gl.drawArrays()` called with correct parameters:
  - mode = TRIANGLES
  - first = 0
  - count = vertexCount
- stats.calls incremented by 1
- stats.triangles incremented by vertexCount / 3
- Geometry visible on screen

### Test
```kotlin
@Test
fun testNonIndexedGeometryRendering() {
    val renderer = WebGLRenderer(canvas, RendererConfig())

    // Create non-indexed geometry (2 triangles, 6 vertices)
    val positions = floatArrayOf(
        0f, 0f, 0f,  // triangle 1
        1f, 0f, 0f,
        1f, 1f, 0f,
        0f, 0f, 0f,  // triangle 2
        1f, 1f, 0f,
        0f, 1f, 0f
    )

    val geometry = BufferGeometry()
    geometry.setAttribute("position", BufferAttribute(positions, 3))
    // NO index buffer

    val mesh = Mesh(geometry, MeshBasicMaterial())
    val scene = Scene().apply { add(mesh) }
    val camera = createTestCamera()

    renderer.render(scene, camera)

    val stats = renderer.getStats()
    assertEquals(1, stats.calls, "Expected 1 draw call")
    assertEquals(2, stats.triangles, "Expected 2 triangles")

    // Visual validation
    val pixels = captureFramebuffer(renderer)
    assertTrue(pixels.any { it != 0 }, "Expected visible geometry on screen")
}
```

### Acceptance Criteria
- ✅ PASS: drawArrays() called, stats correct, geometry visible
- ❌ FAIL: Wrong draw path, incorrect stats, or black screen

---

## Contract 4: Backend Selection Logging

### Description
WebGPURendererFactory must log backend selection decision (WebGPU vs WebGL) during initialization.

### Preconditions
- Browser environment with either WebGPU or WebGL support

### Postconditions
- If WebGPU available:
  - Console log: "WebGPU available - creating WebGPURenderer"
  - Console log: "WebGPURenderer initialized successfully"
- If WebGPU unavailable:
  - Console log: "WebGPU not available - using WebGL renderer"
  - Console log: "WebGLRenderer initialized successfully (fallback)"
- Renderer instance returned is functional

### Test
```kotlin
@Test
fun testBackendSelectionLogging() {
    val consoleLogs = mutableListOf<String>()

    // Mock console.log
    val originalLog = console.log
    console.log = { message: String ->
        consoleLogs.add(message)
        originalLog(message)
    }

    val renderer = WebGPURendererFactory.create(canvas)

    // Restore console.log
    console.log = originalLog

    // Verify logging
    assertTrue(
        consoleLogs.any { it.contains("WebGPU") || it.contains("WebGL") },
        "Expected backend selection logging"
    )
    assertTrue(
        consoleLogs.any { it.contains("initialized successfully") },
        "Expected initialization success logging"
    )

    // Verify renderer is functional
    assertNotNull(renderer)
    val result = renderer.initialize(WebGPURenderSurface(canvas))
    assertTrue(result is RendererResult.Success)
}
```

### Acceptance Criteria
- ✅ PASS: Backend selection logged, initialization logged, renderer functional
- ❌ FAIL: No logging or renderer non-functional

---

## Contract 5: Counter-Clockwise Winding Order

### Description
ChunkMeshGenerator must produce geometry with CCW winding order for all face directions, compatible with Three.js/WebGL conventions.

### Preconditions
- Chunk has solid blocks requiring face rendering
- Face culling enabled: `gl.cullFace(BACK)`, `gl.frontFace(CCW)`

### Postconditions
- For each face direction (UP, DOWN, NORTH, SOUTH, EAST, WEST):
  - Vertices ordered counter-clockwise when viewed from face normal direction
  - Triangle indices: 0→1→2, 0→2→3 (CCW quad split)
- No back-faces visible from intended viewing angle
- Terrain appears right-side up (grass on top, Y-up coordinate system)

### Test
```kotlin
@Test
fun testChunkMeshWindingOrder() {
    // Create test chunk with single block at origin
    val chunk = Chunk(ChunkPosition(0, 0), world)
    chunk.setBlock(0, 0, 0, BlockType.Grass)

    val geometry = ChunkMeshGenerator.generate(chunk)
    val positions = geometry.getAttribute("position")!!.array
    val indices = geometry.index!!.array

    // Test UP face winding (should be CCW when viewed from +Y)
    val upFaceIndices = findFaceIndices(positions, indices, y = 1.0f)
    val windingOrder = calculateWindingOrder(positions, upFaceIndices)
    assertEquals(WindingOrder.CCW, windingOrder, "UP face should be CCW")

    // Test DOWN face winding (should be CCW when viewed from -Y)
    val downFaceIndices = findFaceIndices(positions, indices, y = 0.0f)
    val downWindingOrder = calculateWindingOrder(positions, downFaceIndices, viewFrom = Vector3(0f, -1f, 0f))
    assertEquals(WindingOrder.CCW, downWindingOrder, "DOWN face should be CCW")

    // Repeat for NORTH, SOUTH, EAST, WEST faces...
}

@Test
fun testTerrainOrientation() {
    val world = VoxelWorld(seed = 12345L)
    world.generateTerrain()

    // Find a grass block
    val grassBlock = world.findFirstBlock(BlockType.Grass)
    assertNotNull(grassBlock, "Expected grass block in generated world")

    // Verify grass is on top (Y > 0)
    assertTrue(grassBlock.y > 0, "Grass should be above Y=0")

    // Render and check visual orientation
    val renderer = WebGLRenderer(canvas)
    val camera = createTestCamera(lookAt = Vector3(grassBlock.x, grassBlock.y, grassBlock.z))
    renderer.render(world.scene, camera)

    // Capture framebuffer and check grass appears at expected screen position (top of terrain)
    val pixels = captureFramebuffer(renderer)
    val grassScreenPos = findGrassColorInFramebuffer(pixels)
    assertTrue(grassScreenPos.y < canvas.height / 2, "Grass should appear in upper half of screen")
}
```

### Acceptance Criteria
- ✅ PASS: All faces CCW, terrain right-side up, no back-face artifacts
- ❌ FAIL: CW winding, inverted terrain, or visible back-faces

---

## Contract 6: Scene Graph Traversal

### Description
Renderer must find and render all Mesh objects in scene graph via `scene.traverse()`.

### Preconditions
- Scene contains one or more Mesh objects
- Meshes properly added via `scene.add(mesh)` or parented to scene children
- Scene.updateMatrixWorld() called before rendering

### Postconditions
- `scene.traverse()` visits all descendant objects
- All Mesh objects are rendered
- stats.calls equals number of meshes found
- No meshes skipped silently

### Test
```kotlin
@Test
fun testSceneTraversal() {
    val renderer = WebGLRenderer(canvas)
    val scene = Scene()

    // Add multiple meshes at different hierarchy levels
    val mesh1 = createTestMesh(name = "mesh1")
    val mesh2 = createTestMesh(name = "mesh2")
    val mesh3 = createTestMesh(name = "mesh3")

    scene.add(mesh1)
    scene.add(mesh2)
    mesh2.add(mesh3)  // Nested mesh

    val camera = createTestCamera()
    renderer.render(scene, camera)

    val stats = renderer.getStats()
    assertEquals(3, stats.calls, "Expected 3 draw calls for 3 meshes")
    assertTrue(stats.triangles > 0, "Expected non-zero triangles rendered")
}

@Test
fun testEmptySceneHandling() {
    val renderer = WebGLRenderer(canvas)
    val scene = Scene()  // No meshes
    val camera = createTestCamera()

    // Should not crash
    val result = renderer.render(scene, camera)
    assertTrue(result is RendererResult.Success)

    val stats = renderer.getStats()
    assertEquals(0, stats.calls, "Expected 0 draw calls for empty scene")
    assertEquals(0, stats.triangles, "Expected 0 triangles for empty scene")
}
```

### Acceptance Criteria
- ✅ PASS: All meshes found and rendered, correct stats
- ❌ FAIL: Meshes skipped, incorrect stats, or crash

---

## Contract 7: Shader Compilation Validation

### Description
Renderer must validate shader compilation and fail gracefully on error.

### Preconditions
- WebGL context created
- Vertex and fragment shader source code available

### Postconditions
- Shaders compiled successfully → `basicShaderProgram` != null
- Shaders failed → Error logged, initialization returns error result
- No silent failures causing black screen

### Test
```kotlin
@Test
fun testShaderCompilationSuccess() {
    val renderer = WebGLRenderer(canvas)
    val surface = WebGPURenderSurface(canvas)

    val result = renderer.initialize(surface)
    assertTrue(result is RendererResult.Success)

    // Access internal shader program (via reflection or test hook)
    val program = renderer.getShaderProgram("basic")
    assertNotNull(program, "Expected basic shader program to be compiled")
}

@Test
fun testShaderCompilationFailure() {
    // Inject invalid shader source
    val renderer = WebGLRendererWithBrokenShader(canvas)
    val surface = WebGPURenderSurface(canvas)

    val result = renderer.initialize(surface)

    // Should return error, not crash
    assertTrue(result is RendererResult.Error)
    assertTrue((result as RendererResult.Error).exception.message!!.contains("shader"))
}
```

### Acceptance Criteria
- ✅ PASS: Valid shaders compile, invalid shaders error gracefully
- ❌ FAIL: Silent shader failure causing black screen

---

## Contract 8: Geometry Attribute Binding

### Description
Renderer must correctly bind all geometry attributes to shader locations before drawing.

### Preconditions
- BufferGeometry with position, normal, color attributes
- Shader program with corresponding attribute locations (aPosition, aNormal, aColor)

### Postconditions
- All present attributes bound via `gl.vertexAttribPointer()`
- Attribute arrays enabled via `gl.enableVertexAttribArray()`
- Missing attributes handled gracefully (not bound)

### Test
```kotlin
@Test
fun testAttributeBinding() {
    val renderer = WebGLRenderer(canvas)

    // Geometry with all attributes
    val geometry = BufferGeometry()
    geometry.setAttribute("position", BufferAttribute(floatArrayOf(0f, 0f, 0f), 3))
    geometry.setAttribute("normal", BufferAttribute(floatArrayOf(0f, 1f, 0f), 3))
    geometry.setAttribute("color", BufferAttribute(floatArrayOf(1f, 0f, 0f), 3))

    val mesh = Mesh(geometry, MeshBasicMaterial())
    val scene = Scene().apply { add(mesh) }

    // Spy on WebGL calls
    val boundAttributes = mutableSetOf<String>()
    mockGLCalls { location, name ->
        boundAttributes.add(name)
    }

    renderer.render(scene, createTestCamera())

    assertTrue("aPosition" in boundAttributes)
    assertTrue("aNormal" in boundAttributes)
    assertTrue("aColor" in boundAttributes)
}

@Test
fun testMissingAttributeHandling() {
    val renderer = WebGLRenderer(canvas)

    // Geometry with only position (no normal, color)
    val geometry = BufferGeometry()
    geometry.setAttribute("position", BufferAttribute(floatArrayOf(0f, 0f, 0f), 3))

    val mesh = Mesh(geometry, MeshBasicMaterial())
    val scene = Scene().apply { add(mesh) }

    // Should not crash
    val result = renderer.render(scene, createTestCamera())
    assertTrue(result is RendererResult.Success)
}
```

### Acceptance Criteria
- ✅ PASS: All attributes bound correctly, missing attributes handled
- ❌ FAIL: Attribute binding errors or crashes

---

## Integration Contract

### Description
All contracts must pass together in a full VoxelCraft rendering scenario.

### Test
```kotlin
@Test
fun testVoxelCraftFullRendering() {
    // Setup
    val world = VoxelWorld(seed = 12345L)
    world.generateTerrain()

    val canvas = createTestCanvas(800, 600)
    val renderer = WebGPURendererFactory.create(canvas)
    val camera = createTestCamera(position = Vector3(0f, 64f, 0f))

    // Render multiple frames
    repeat(60) {
        renderer.render(world.scene, camera)
    }

    // Validate
    val stats = renderer.getStats()
    assertTrue(stats.calls > 0, "Expected draw calls")
    assertTrue(stats.triangles > 0, "Expected triangles rendered")

    // Performance
    val fps = measureFPS(renderer, world.scene, camera, frames = 60)
    assertTrue(fps >= 30.0, "Expected FPS >= 30, got $fps")

    // Visual
    val pixels = captureFramebuffer(renderer)
    assertTrue(pixels.any { it != 0 }, "Expected non-black screen")
}
```

### Acceptance Criteria
- ✅ PASS: All sub-contracts pass, VoxelCraft renders correctly at ≥30 FPS
- ❌ FAIL: Any contract fails or VoxelCraft shows black screen

---

## Test Helpers

```kotlin
fun createTestCamera(position: Vector3 = Vector3(0f, 0f, 5f), lookAt: Vector3 = Vector3.ZERO): PerspectiveCamera {
    val camera = PerspectiveCamera(fov = 75f, aspect = 1.333f, near = 0.1f, far = 1000f)
    camera.position.copy(position)
    camera.lookAt(lookAt)
    camera.updateMatrixWorld()
    camera.updateProjectionMatrix()
    return camera
}

fun createTestMesh(name: String = "test", triangles: Int = 2): Mesh {
    val positions = floatArrayOf(
        0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f,
        0f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 0f
    )
    val geometry = BufferGeometry()
    geometry.setAttribute("position", BufferAttribute(positions, 3))
    return Mesh(geometry, MeshBasicMaterial()).apply { this.name = name }
}

fun captureFramebuffer(renderer: WebGLRenderer): IntArray {
    // Read pixels from canvas
    val pixels = IntArray(renderer.getViewport().width * renderer.getViewport().height)
    renderer.gl.readPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
    return pixels
}

fun measureFPS(renderer: Renderer, scene: Scene, camera: Camera, frames: Int = 60): Double {
    val startTime = now()
    repeat(frames) {
        renderer.render(scene, camera)
    }
    val endTime = now()
    return frames * 1000.0 / (endTime - startTime)
}
```

## Contract Verification Checklist

- [ ] Contract 1: Performance (≥30 FPS)
- [ ] Contract 2: Indexed geometry rendering
- [ ] Contract 3: Non-indexed geometry rendering
- [ ] Contract 4: Backend selection logging
- [ ] Contract 5: CCW winding order
- [ ] Contract 6: Scene graph traversal
- [ ] Contract 7: Shader compilation validation
- [ ] Contract 8: Geometry attribute binding
- [ ] Integration contract: Full VoxelCraft rendering

All contracts must pass for feature completion.
