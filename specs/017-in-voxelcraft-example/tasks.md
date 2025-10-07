# Tasks: Fix VoxelCraft Rendering Performance and Visual Issues

**Input**: Design documents from `/home/yousef/Projects/kmp/KreeKt/specs/017-in-voxelcraft-example/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/renderer-contract.md, quickstart.md
**Branch**: `017-in-voxelcraft-example`

## Execution Flow (main)
```
1. Load plan.md from feature directory ✓
   → Tech stack: Kotlin 1.9+ Multiplatform, kotlinx-coroutines-core:1.8.0
   → Structure: Single project with examples/ subdirectory
2. Load optional design documents ✓
   → research.md: Root cause analysis (scene traversal failure most likely)
   → data-model.md: 7 entities (BufferGeometry, Mesh, Scene, RenderStats, etc.)
   → contracts/renderer-contract.md: 8 contracts covering rendering requirements
   → quickstart.md: 4 manual test scenarios
3. Generate tasks by category ✓
   → Diagnostic: Add logging to identify root cause
   → Tests: Contract tests for renderer behavior (TDD)
   → Implementation: Fix based on diagnostic findings
   → Validation: End-to-end testing and performance validation
4. Apply task rules ✓
   → Different files = mark [P] for parallel execution
   → Same file = sequential (no [P])
   → Tests before implementation (TDD approach)
5. Number tasks sequentially (T001-T024) ✓
6. Generate dependency graph ✓
7. Create parallel execution examples ✓
8. Validate task completeness ✓
   → All contracts have tests ✓
   → All diagnostic steps covered ✓
   → All fixes prioritized by research.md ✓
9. Return: SUCCESS (tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions
- **[SERIAL]**: Must run in specific order

## Path Conventions
- **Project root**: `/home/yousef/Projects/kmp/KreeKt/`
- **VoxelCraft example**: `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/`
- **Library renderer**: `src/jsMain/kotlin/io/kreekt/renderer/`
- **Tests**: Tests will be created in appropriate test directories

---

## Phase 3.1: Diagnostic Investigation (SERIAL - Build Understanding) ✅ COMPLETE
**Goal**: Identify exact root cause of 0 FPS rendering issue

### T001 [SERIAL] [X] [X] Add scene traversal diagnostic logging to WebGLRenderer
**File**: `src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`
**Priority**: CRITICAL
**Description**: Add diagnostic logging to `renderScene()` method to count meshes found during scene traversal.

**Implementation**:
```kotlin
// In renderScene() method (line ~174)
var meshCount = 0
scene.traverse { obj ->
    if (obj is Mesh) {
        meshCount++
        console.log("🎨 Rendering mesh: ${obj.name ?: "unnamed"}, geometry hash: ${obj.geometry.hashCode()}")
        renderMesh(obj, gl, program, uModelMatrix)
    }
}
console.log("✅ Scene traversal complete: $meshCount meshes found")
if (meshCount == 0) {
    console.warn("⚠️ Scene traversal found 0 meshes - check scene.add() calls")
}
```

**Expected Output**: Console should show mesh count and names
**Success Criteria**: Logging added, compiles successfully

---

### T002 [SERIAL] [X] Add camera position diagnostic logging to Main.kt
**File**: `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`
**Priority**: CRITICAL
**Description**: Add diagnostic logging to game loop to track camera position and scene state.

**Implementation**:
```kotlin
// In gameLoop() function (add after line 204)
if (frameCount < 10) {
    console.log("📷 Camera pos: (${camera.position.x}, ${camera.position.y}, ${camera.position.z})")
    console.log("📷 Camera rot: (${camera.rotation.x}, ${camera.rotation.y}, ${camera.rotation.z})")
    console.log("🧱 Scene children count: ${world.scene.children.size}")
    console.log("🧱 Scene type: ${world.scene::class.simpleName}")
}
```

**Expected Output**: Camera position and scene stats for first 10 frames
**Success Criteria**: Logging added, compiles successfully

---

### T003 [SERIAL] [X] Add shader compilation validation logging to WebGLRenderer
**File**: `src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`
**Priority**: CRITICAL
**Description**: Add validation logging to `renderScene()` to check shader program state.

**Implementation**:
```kotlin
// In renderScene() method (line ~174, BEFORE useProgram)
val program = basicShaderProgram
if (program == null) {
    console.error("🔴 basicShaderProgram is null - shader compilation failed!")
    console.error("🔴 Renderer initialization may have failed silently")
    return
}
console.log("✅ Using shader program: ${program}")
gl.useProgram(program)
```

**Expected Output**: Shader program validation or error message
**Success Criteria**: Logging added, compiles successfully

---

### T004 [SERIAL] [X] Add geometry statistics logging to ChunkMeshGenerator
**File**: `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ChunkMeshGenerator.kt`
**Priority**: HIGH
**Description**: Enhance existing debug logging to show more geometry details.

**Implementation**:
```kotlin
// In generate() method (replace line 54)
console.log("""
    Chunk mesh generated:
      Vertices: ${vertices.size/3} (${vertices.size} floats)
      Triangles: ${indices.size/3} (${indices.size} indices)
      Has normals: ${normals.isNotEmpty()}
      Has colors: ${colors.isNotEmpty()}
      Geometry ID: ${geometry.hashCode()}
""".trimIndent())
```

**Expected Output**: Detailed geometry statistics
**Success Criteria**: Enhanced logging added, compiles successfully

---

### T005 [SERIAL] [X] Run VoxelCraft and capture diagnostic console output
**File**: N/A (manual testing task)
**Priority**: CRITICAL
**Description**: Build and run VoxelCraft with diagnostic logging, capture full console output.

**Steps**:
1. Build VoxelCraft: `./gradlew :examples:voxelcraft:jsBrowserDevelopmentRun`
2. Open browser DevTools console (F12)
3. Click "Start Game" button
4. Wait for loading screen to disappear
5. Capture all console output (copy/paste to text file)
6. Look for patterns:
   - "⚠️ Scene traversal found 0 meshes" → Root cause = Scene graph issue (Fix 1)
   - "🔴 basicShaderProgram is null" → Root cause = Shader compilation failure (Fix 2)
   - "📷 Camera pos: (0, 0, 0)" with "✅ Scene traversal complete: 0 meshes" → Both issues
   - "✅ Rendered N meshes" with "Chunk mesh: X vertices" → Geometry generated but not visible

**Expected Output**: Console output matching one of the patterns above
**Success Criteria**: Console output captured, root cause identified

---

## Phase 3.2: Contract Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**Goal**: Write failing tests that define expected behavior
**CRITICAL**: These tests MUST be written and MUST FAIL before ANY implementation

### T006 [P] [X] Write WebGLRenderer performance contract test
**File**: `src/jsTest/kotlin/io/kreekt/renderer/webgl/WebGLRendererPerformanceTest.kt`
**Priority**: HIGH
**Description**: Create contract test validating FPS ≥30 requirement from contracts/renderer-contract.md Contract 1.

**Implementation**:
```kotlin
package io.kreekt.renderer.webgl

import io.kreekt.camera.PerspectiveCamera
import io.kreekt.core.math.Vector3
import io.kreekt.core.scene.Mesh
import io.kreekt.core.scene.Scene
import io.kreekt.geometry.BufferAttribute
import io.kreekt.geometry.BufferGeometry
import io.kreekt.material.MeshBasicMaterial
import io.kreekt.renderer.RendererConfig
import io.kreekt.renderer.WebGPURenderSurface
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import kotlin.js.Date
import kotlin.test.Test
import kotlin.test.assertTrue

class WebGLRendererPerformanceTest {

    @Test
    fun testRendererMeetsPerformanceTarget() {
        // Setup
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = 800
        canvas.height = 600

        val renderer = WebGLRenderer(canvas, RendererConfig(antialias = true))
        val surface = WebGPURenderSurface(canvas)
        renderer.initialize(surface)

        // Create test scene with 32 chunks worth of geometry
        val scene = createTestScene(chunkCount = 32, trianglesPerChunk = 4000)
        val camera = createTestCamera()

        // Warm-up frames
        repeat(10) {
            renderer.render(scene, camera)
        }

        // Measure performance
        val startTime = Date.now()
        val targetFrames = 60
        repeat(targetFrames) {
            renderer.render(scene, camera)
        }
        val endTime = Date.now()

        val avgFrameTime = (endTime - startTime) / targetFrames
        val fps = 1000.0 / avgFrameTime

        // Assert constitutional requirement
        assertTrue(fps >= 30.0, "Expected FPS >= 30 (constitutional minimum), got $fps")

        console.log("✅ Performance test passed: ${fps.toInt()} FPS (target: 60 FPS)")
    }

    private fun createTestScene(chunkCount: Int, trianglesPerChunk: Int): Scene {
        val scene = Scene()
        repeat(chunkCount) { i ->
            val mesh = createTestMesh("chunk_$i", trianglesPerChunk)
            mesh.position.set((i % 8) * 16f, 0f, (i / 8) * 16f)
            scene.add(mesh)
        }
        return scene
    }

    private fun createTestMesh(name: String, triangleCount: Int): Mesh {
        val vertexCount = triangleCount * 3
        val positions = FloatArray(vertexCount * 3) { it.toFloat() }
        val geometry = BufferGeometry()
        geometry.setAttribute("position", BufferAttribute(positions, 3))

        val material = MeshBasicMaterial()
        return Mesh(geometry, material).apply { this.name = name }
    }

    private fun createTestCamera(): PerspectiveCamera {
        val camera = PerspectiveCamera(fov = 75f, aspect = 1.333f, near = 0.1f, far = 1000f)
        camera.position.set(0f, 64f, 0f)
        camera.updateMatrixWorld()
        camera.updateProjectionMatrix()
        return camera
    }
}
```

**Expected Result**: Test MUST FAIL initially (FPS = 0)
**Success Criteria**: Test file created, compiles, fails as expected

---

### T007 [P] [X] Write WebGLRenderer indexed geometry contract test
**File**: `src/jsTest/kotlin/io/kreekt/renderer/webgl/WebGLRendererIndexedGeometryTest.kt`
**Priority**: HIGH
**Description**: Create contract test validating indexed geometry rendering from contracts/renderer-contract.md Contract 2.

**Implementation**: See contracts/renderer-contract.md Contract 2 for full test implementation
**Expected Result**: Test MUST FAIL initially
**Success Criteria**: Test file created, compiles, fails as expected

---

### T008 [P] [X] Write WebGLRenderer non-indexed geometry contract test
**File**: `src/jsTest/kotlin/io/kreekt/renderer/webgl/WebGLRendererNonIndexedGeometryTest.kt`
**Priority**: HIGH
**Description**: Create contract test validating non-indexed geometry rendering from contracts/renderer-contract.md Contract 3.

**Implementation**: See contracts/renderer-contract.md Contract 3 for full test implementation
**Expected Result**: Test MUST FAIL initially
**Success Criteria**: Test file created, compiles, fails as expected

---

### T009 [P] [X] Write WebGPURendererFactory backend selection test
**File**: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/WebGPURendererFactoryTest.kt`
**Priority**: MEDIUM
**Description**: Create contract test validating backend selection logging from contracts/renderer-contract.md Contract 4.

**Implementation**: See contracts/renderer-contract.md Contract 4 for full test implementation
**Expected Result**: Test should PASS (backend logging already implemented)
**Success Criteria**: Test file created, compiles, passes

---

### T010 [P] [X] Write ChunkMeshGenerator winding order test
**File**: `examples/voxelcraft/src/jsTest/kotlin/io/kreekt/examples/voxelcraft/ChunkMeshWindingTest.kt`
**Priority**: MEDIUM
**Description**: Create contract test validating CCW winding order from contracts/renderer-contract.md Contract 5.

**Implementation**: See contracts/renderer-contract.md Contract 5 for full test implementation
**Expected Result**: Test should PASS (winding order validated correct in research.md)
**Success Criteria**: Test file created, compiles, passes

---

## Phase 3.3: Core Implementation (ONLY after tests are failing)
**Goal**: Fix rendering issues based on diagnostic output from Phase 3.1

### FIX PATH 1: Scene Traversal Failure (If T005 showed "0 meshes found")

#### T011 [SERIAL] Verify chunk meshes are added to scene in VoxelWorld
**File**: `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/VoxelWorld.kt`
**Priority**: CRITICAL
**Condition**: Execute ONLY IF T005 output shows "Scene traversal found 0 meshes"
**Description**: Verify chunk meshes are properly added to scene graph via scene.add().

**Investigation**:
```kotlin
// In VoxelWorld.kt, find where chunks are added to scene
// Look for: scene.add(chunkMesh) or similar
// Verify chunk.mesh is not null before adding
// Verify parent relationship is established
```

**Fix** (if missing):
```kotlin
// After chunk mesh generation
val chunkMesh = ChunkMeshGenerator.generate(chunk)
chunk.mesh = Mesh(chunkMesh, chunkMaterial)
chunk.mesh!!.position.set(chunk.position.toWorldX().toFloat(), 0f, chunk.position.toWorldZ().toFloat())
scene.add(chunk.mesh!!)  // CRITICAL: Must call scene.add()
console.log("Added chunk mesh to scene: ${chunk.position}")
```

**Success Criteria**: Chunks added to scene, T001 logging shows >0 meshes

---

#### T012 [SERIAL] Fix Scene.add() implementation if broken
**File**: `src/commonMain/kotlin/io/kreekt/core/scene/Scene.kt`
**Priority**: CRITICAL
**Condition**: Execute ONLY IF T011 found scene.add() calls but T001 still shows 0 meshes
**Description**: Verify Scene.add() properly adds children and sets parent relationship.

**Investigation**:
```kotlin
// In Scene.kt, check add() method implementation
// Verify: children.add(object)
// Verify: object.parent = this
// Verify: no early returns that skip adding
```

**Fix** (if broken):
```kotlin
override fun add(vararg objects: Object3D) {
    for (obj in objects) {
        if (obj === this) {
            console.error("Object can't be added as a child of itself")
            continue
        }
        if (obj.parent != null) {
            obj.parent?.remove(obj)
        }
        obj.parent = this
        children.add(obj)
        console.log("Added object to scene: ${obj.name ?: obj::class.simpleName}")
    }
}
```

**Success Criteria**: Scene.add() fixed, T001 logging shows >0 meshes

---

#### T013 [SERIAL] Fix Scene.traverse() implementation if broken
**File**: `src/commonMain/kotlin/io/kreekt/core/scene/Object3D.kt`
**Priority**: CRITICAL
**Condition**: Execute ONLY IF T012 fixed add() but traverse still finds 0 meshes
**Description**: Verify Scene.traverse() recursively visits all children.

**Investigation**:
```kotlin
// In Object3D.kt, check traverse() method
// Verify: callback is called on each object
// Verify: children are recursively traversed
```

**Fix** (if broken):
```kotlin
fun traverse(callback: (Object3D) -> Unit) {
    callback(this)  // Visit this object
    for (child in children) {
        child.traverse(callback)  // Recursively visit children
    }
}
```

**Success Criteria**: Scene.traverse() fixed, T001 logging shows >0 meshes

---

### FIX PATH 2: Shader Compilation Failure (If T005 showed "basicShaderProgram is null")

#### T014 [SERIAL] Debug shader compilation in WebGLRenderer.createBasicShaderProgram()
**File**: `src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`
**Priority**: CRITICAL
**Condition**: Execute ONLY IF T005 output shows "basicShaderProgram is null"
**Description**: Add detailed logging to shader compilation to identify failure point.

**Implementation**:
```kotlin
// In createBasicShaderProgram() method (line ~345)
private fun createBasicShaderProgram() {
    val gl = this.gl
    if (gl == null) {
        console.error("🔴 Cannot create shader: gl context is null")
        return
    }

    console.log("🔧 Compiling basic shader program...")

    val vertexShaderSource = """..."""  // existing source
    val fragmentShaderSource = """..."""  // existing source

    console.log("🔧 Vertex shader source length: ${vertexShaderSource.length}")
    console.log("🔧 Fragment shader source length: ${fragmentShaderSource.length}")

    val program = compileShaderProgram(gl, vertexShaderSource, fragmentShaderSource)

    if (program == null) {
        console.error("🔴 Shader compilation failed - program is null")
        console.error("🔴 Check compileShaderProgram() for detailed errors")
        return
    }

    basicShaderProgram = program
    shaderPrograms["basic"] = program
    console.log("✅ Basic shader program compiled successfully")
}
```

**Success Criteria**: Detailed shader logging added, root cause identified

---

#### T015 [SERIAL] Fix shader compilation errors
**File**: `src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`
**Priority**: CRITICAL
**Condition**: Execute ONLY IF T014 identified shader compilation error
**Description**: Fix shader source or compilation logic based on T014 findings.

**Possible Fixes**:
1. If vertex shader fails: Check attribute names match geometry attributes
2. If fragment shader fails: Check syntax errors in GLSL
3. If linking fails: Check varying variables match between shaders

**Success Criteria**: Shaders compile successfully, T003 logging shows valid program

---

### FIX PATH 3: Camera/Visibility Issues (If T005 showed "N meshes rendered" but still 0 FPS)

#### T016 [SERIAL] [X] Fix camera position or look direction
**File**: `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`
**Priority**: HIGH
**Condition**: Execute ONLY IF T005 shows meshes rendered but nothing visible
**Description**: Adjust camera to ensure it's looking at terrain.

**Investigation**: Check T002 output for camera position
- If camera Y = 0: Terrain might be at same level, camera inside blocks
- If camera looking away: Need to adjust rotation

**Fix**:
```kotlin
// In continueInitialization() (line ~166)
val camera = PerspectiveCamera(
    fov = 75.0f,
    aspect = canvas.width.toFloat() / canvas.height.toFloat(),
    near = 0.1f,
    far = 1000.0f
)

// Force camera to known good position and orientation
camera.position.set(0f, 80f, 0f)  // High up, looking down
camera.rotation.set(-0.5f, 0f, 0f)  // Look slightly down
camera.updateMatrixWorld(true)
camera.updateProjectionMatrix()

console.log("📷 Camera forced to known good position: (0, 80, 0)")
```

**Success Criteria**: Terrain becomes visible, FPS >0

---

### FIX PATH 4: Index Buffer Overflow (If chunks have >65,535 vertices)

#### T017 [SERIAL] Switch index buffer from Uint16 to Uint32 if needed
**File**: `src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`
**Priority**: LOW (unlikely based on research.md analysis)
**Condition**: Execute ONLY IF T004 shows vertex count >65,535
**Description**: Change index buffer type to support larger meshes.

**Fix**: See research.md Section 3.3 for Uint32Array implementation details

**Success Criteria**: Large chunks render correctly

---

## Phase 3.4: Integration & Polish
**Goal**: Remove debug code, optimize, validate fixes

### T018 [SERIAL] Run all contract tests and verify PASS
**File**: N/A (test execution task)
**Priority**: HIGH
**Description**: Execute all contract tests from Phase 3.2 and verify they pass.

**Steps**:
```bash
./gradlew :kreekt-renderer:jsTest
./gradlew :examples:voxelcraft:jsTest
```

**Expected Result**: All tests pass, including T006 performance test (FPS ≥30)
**Success Criteria**: Test suite passes, no failures

---

### T019 [SERIAL] Execute manual test scenarios from quickstart.md
**File**: N/A (manual testing task)
**Priority**: HIGH
**Description**: Follow all 4 test scenarios in quickstart.md and verify acceptance criteria.

**Scenarios**:
1. Performance Validation (30-60 FPS) → quickstart.md Scenario 1
2. Visual Correctness (terrain right-side up) → quickstart.md Scenario 2
3. Backend Selection Verification → quickstart.md Scenario 3
4. Debug Diagnostics (if issues persist) → quickstart.md Scenario 4

**Expected Result**: All acceptance criteria pass
**Success Criteria**: FPS ≥30, terrain right-side up, backend logged

---

### T020 [P] [X] Remove diagnostic logging from WebGLRenderer
**File**: `src/jsMain/kotlin/io/kreekt/renderer/webgl/WebGLRenderer.kt`
**Priority**: MEDIUM
**Description**: Remove or comment out diagnostic console.log statements added in T001 and T003.

**Implementation**:
- Keep error logging (console.error)
- Remove verbose console.log for mesh rendering
- Reduce scene traversal logging to errors only

**Success Criteria**: Production code clean, no excessive logging

---

### T021 [P] [X] Remove diagnostic logging from Main.kt
**File**: `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`
**Priority**: MEDIUM
**Description**: Remove diagnostic camera/scene logging added in T002.

**Success Criteria**: Example code clean

---

### T022 [P] [X] Remove excessive logging from ChunkMeshGenerator
**File**: `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ChunkMeshGenerator.kt`
**Priority**: LOW
**Description**: Reduce geometry statistics logging to summary only (keep 1 line per chunk).

**Success Criteria**: Logging concise but informative

---

### T023 [SERIAL] Performance validation - verify 60 FPS target approached
**File**: N/A (validation task)
**Priority**: MEDIUM
**Description**: Measure actual FPS with production code (no debug logging) and compare to constitutional target.

**Steps**:
1. Build release version: `./gradlew :examples:voxelcraft:jsBrowserProductionRun`
2. Run VoxelCraft
3. Observe FPS counter for 30 seconds
4. Record min/max/average FPS
5. Compare to constitutional target (60 FPS) and minimum (30 FPS)

**Expected Result**:
- FPS ≥ 30 (minimum, MUST pass)
- FPS → 60 (target, should approach)
- No console errors

**Success Criteria**: FPS meets or exceeds 30, approaches 60

---

### T024 [SERIAL] [X] Update CLAUDE.md with fix details
**File**: `CLAUDE.md`
**Priority**: LOW
**Description**: Document the VoxelCraft rendering fix in Recent Changes section.

**Implementation**:
```markdown
## Recent Changes
- 017-in-voxelcraft-example: Fixed VoxelCraft rendering issues (0 FPS black screen)
  - Root cause: [Scene traversal failure | Shader compilation | Camera position]
  - Fix: [Description of fix applied]
  - Result: Restored 30-60 FPS performance, terrain renders correctly
- 017-in-voxelcraft-example: Added Kotlin 1.9+ with Multiplatform plugin, targeting JS/Browser
...
```

**Success Criteria**: CLAUDE.md updated with fix summary

---

## Dependencies

### Phase 3.1 (Diagnostic) - SERIAL
```
T001 → T002 → T003 → T004 → T005
(Must run in order to build understanding)
```

### Phase 3.2 (Tests) - PARALLEL
```
T006 [P] ─┐
T007 [P] ─┤
T008 [P] ─┼─ Can run in parallel (different files)
T009 [P] ─┤
T010 [P] ─┘
```

### Phase 3.3 (Implementation) - CONDITIONAL
```
T005 diagnostic output → determines which fix path to take:

Path 1 (Scene): T011 → T012 → T013
Path 2 (Shader): T014 → T015
Path 3 (Camera): T016
Path 4 (Index): T017

Only execute path matching root cause from T005
```

### Phase 3.4 (Polish) - MIX
```
T018 (run tests) → blocks T019 (must pass first)
T019 (manual test) → blocks T023 (must pass first)

T020 [P] ─┐
T021 [P] ─┼─ Can run in parallel (different files)
T022 [P] ─┘

T023 → T024 (update docs last)
```

## Parallel Execution Examples

### Run all contract tests together (Phase 3.2):
```kotlin
// Launch T006-T010 in parallel
Task("Write performance test", "tests/.../WebGLRendererPerformanceTest.kt")
Task("Write indexed geometry test", "tests/.../WebGLRendererIndexedGeometryTest.kt")
Task("Write non-indexed geometry test", "tests/.../WebGLRendererNonIndexedGeometryTest.kt")
Task("Write backend selection test", "tests/.../WebGPURendererFactoryTest.kt")
Task("Write winding order test", "tests/.../ChunkMeshWindingTest.kt")
```

### Clean up logging together (Phase 3.4):
```kotlin
// Launch T020-T022 in parallel
Task("Remove diagnostic logging from WebGLRenderer", "src/.../WebGLRenderer.kt")
Task("Remove diagnostic logging from Main.kt", "examples/.../Main.kt")
Task("Remove excessive logging from ChunkMeshGenerator", "examples/.../ChunkMeshGenerator.kt")
```

## Task Execution Strategy

### Approach 1: Diagnostic-First (Recommended)
1. Execute Phase 3.1 serially (T001-T005) to identify root cause
2. Based on T005 output, execute ONLY the relevant fix path from Phase 3.3
3. Execute Phase 3.2 tests in parallel (T006-T010) to verify fix
4. Execute Phase 3.4 polish tasks

**Advantages**:
- Faster diagnosis (skip irrelevant fixes)
- More targeted implementation
- Less wasted effort

### Approach 2: TDD-First (Rigorous)
1. Execute Phase 3.2 tests in parallel (T006-T010) - watch them fail
2. Execute Phase 3.1 diagnostics serially (T001-T005)
3. Execute relevant fix path from Phase 3.3
4. Re-run Phase 3.2 tests - watch them pass
5. Execute Phase 3.4 polish tasks

**Advantages**:
- Pure TDD approach
- Better test coverage
- More confidence in fixes

## Notes

### Critical Success Factors
- **T005 output is CRITICAL**: Determines entire fix path
- **Tests must fail first**: Don't implement before seeing failures (TDD)
- **One fix path only**: Don't apply multiple fixes blindly

### Common Pitfalls to Avoid
- ❌ Implementing fixes before running diagnostics (T001-T005)
- ❌ Applying all fixes instead of targeted fix based on T005
- ❌ Removing debug logging before validating fixes work (T018-T019)
- ❌ Skipping contract tests (T006-T010) to "save time"

### Debugging Tips
- If FPS still 0 after fix: Re-run T001-T005 diagnostics
- If terrain upside down: Check T010 winding order test
- If performance <30 FPS: Profile with browser DevTools
- If WebGPU not used: Check T009 backend selection test

## Validation Checklist
*GATE: Must pass before marking feature complete*

- [ ] All diagnostic tasks (T001-T005) executed
- [ ] Root cause identified from T005 output
- [ ] Relevant fix path from T011-T017 executed
- [ ] All contract tests (T006-T010) pass
- [ ] Manual tests (T019) pass all scenarios
- [ ] FPS ≥ 30 validated (T023)
- [ ] Debug logging removed (T020-T022)
- [ ] Documentation updated (T024)

## Estimated Time

- **Phase 3.1** (Diagnostic): 30 minutes
- **Phase 3.2** (Tests): 2 hours (writing 5 tests)
- **Phase 3.3** (Implementation): 1-3 hours (depends on root cause)
- **Phase 3.4** (Polish): 1 hour
- **Total**: 4-6 hours

## Success Confirmation

Feature is complete when:
1. ✅ VoxelCraft renders at ≥30 FPS (constitutional minimum)
2. ✅ Terrain appears right-side up (grass on top)
3. ✅ Backend selection logged correctly
4. ✅ All contract tests pass
5. ✅ Manual test scenarios pass
6. ✅ Debug code removed
7. ✅ Documentation updated

---

*Generated from design documents in `/home/yousef/Projects/kmp/KreeKt/specs/017-in-voxelcraft-example/`*
*Based on research.md root cause analysis and contracts/renderer-contract.md specifications*
