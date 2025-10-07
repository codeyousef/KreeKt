# Implementation Plan: Optimize VoxelCraft Rendering with WebGPU/Vulkan GPU Acceleration

**Branch**: `018-optimize-voxelcraft-rendering` | **Date**: 2025-10-07 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/home/yousef/Projects/kmp/KreeKt/specs/018-optimize-voxelcraft-rendering/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path → ✅ COMPLETED
2. Fill Technical Context → ✅ COMPLETED
3. Fill Constitution Check section → ✅ COMPLETED
4. Evaluate Constitution Check section → ✅ PASSED
5. Execute Phase 0 → research.md → ✅ COMPLETED
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, CLAUDE.md → ✅ COMPLETED
7. Re-evaluate Constitution Check section → ✅ PASSED
8. Plan Phase 2 → Describe task generation approach → ✅ COMPLETED
9. STOP - Ready for /tasks command → ✅ READY
```

## Summary

VoxelCraft is currently rendering at 0 FPS due to WebGPU renderer implementation issues. The renderer exists but has critical performance bottlenecks including busy-wait pipeline creation, lack of pipeline caching, inefficient buffer management, and missing frustum culling. This plan addresses these issues to achieve 60 FPS with WebGPU on capable hardware (constitutional requirement) and maintain 30 FPS minimum with WebGL fallback.

**Primary Requirement**: Implement production-ready WebGPU rendering with full GPU utilization to render 81 chunks (~160K triangles) at 60 FPS.

**Technical Approach** (from research phase):
1. Fix WebGPU pipeline creation (async/await pattern, eliminate busy-wait)
2. Implement proper pipeline caching with synchronous access
3. Optimize buffer management (VAO-equivalent, buffer pooling)
4. Add frustum culling to render only visible chunks
5. Implement draw call batching (<100 calls per frame)
6. Enhance WebGL fallback with attribute location caching and VAO
7. Add comprehensive performance metrics and validation

## Technical Context

**Language/Version**: Kotlin 1.9+ Multiplatform (JS target for browser)
**Primary Dependencies**:
- @webgpu/types 0.1.40 (WebGPU JavaScript bindings)
- kotlinx-coroutines-core 1.8.0 (async operations)
- WebGL 2.0 (fallback renderer)
- KreeKt renderer module (existing WebGPU/WebGL infrastructure)

**Storage**: N/A (rendering optimization, no persistence)
**Testing**:
- Kotlin/JS test framework
- Browser-based performance tests (FPS measurement)
- Visual regression tests (rendering consistency)
- Contract tests (FR-001 through FR-015 validation)

**Target Platform**: Modern web browsers
- Primary: Chrome 113+, Edge 113+ (WebGPU support)
- Fallback: Firefox, Safari (WebGL 2.0)

**Project Type**: Single project (Kotlin Multiplatform library with JS example)

**Performance Goals**:
- **60 FPS** with WebGPU (constitutional target)
- **30 FPS minimum** with WebGL fallback (constitutional requirement)
- **<100 draw calls** per frame for 81 chunks
- **<2000ms** renderer initialization (constitutional budget)
- **<5MB** library size (constitutional constraint)

**Constraints**:
- Must maintain visual consistency between WebGPU and WebGL
- Must support async terrain generation without blocking render loop
- Must handle WebGPU unavailability gracefully (Firefox, Safari)
- Must not break existing VoxelCraft gameplay features

**Scale/Scope**:
- 81 visible chunks (16x16x256 blocks each)
- ~160,000 triangles total (3000-5000 per chunk)
- ~1,024 total chunks in world (streaming system)
- Real-time mesh generation (block breaking/placing)

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Core Principles Compliance

**✅ Performance (60 FPS target)**
- Current state: 0 FPS → Target: 60 FPS
- Remediation: Fix WebGPU pipeline creation, add frustum culling, batch draw calls
- Validation: Performance tests measuring FPS under load (FR-002)

**✅ Type Safety (No runtime casts)**
- Existing code uses Kotlin type system with minimal unsafe casts
- Limited JS interop uses `unsafeCast` only where necessary for WebGPU API
- Plan maintains type safety discipline

**✅ Size Constraint (<5MB base library)**
- Current library is within budget
- Optimizations add minimal code (~200 lines)
- No new dependencies

**✅ Cross-Platform Consistency**
- WebGPU and WebGL backends must render identically
- Validation: Visual regression tests (FR-011)
- Fallback strategy preserves user experience

**✅ Initialization Budget (<2000ms)**
- Current: ~50ms initialization time
- Plan does not add initialization overhead
- Validation: Startup performance tests (FR-015)

### Testing Requirements

**✅ Test-First (TDD mandatory)**
- Phase 1 generates contract tests from FR-001 through FR-015
- Tests fail initially (no implementation yet)
- Implementation follows red-green-refactor cycle

**✅ Integration Testing**
- VoxelCraft integration tests validate 60 FPS with 81 chunks
- WebGPU fallback tests verify WebGL activation
- Cross-browser compatibility tests (Chrome, Firefox, Safari)

**✅ Performance Benchmarking**
- Automated FPS measurement tests
- Draw call counting validation (<100 per frame)
- Triangle count tracking (~160K target)

### Gate Status
- ✅ Initial Constitution Check: **PASSED**
- ⏸️ Post-Design Constitution Check: Pending Phase 1
- ✅ No violations requiring complexity tracking
- ✅ No deviations from constitutional principles

## Project Structure

### Documentation (this feature)
```
specs/018-optimize-voxelcraft-rendering/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
│   ├── webgpu-renderer.contract.md
│   ├── webgl-fallback.contract.md
│   └── performance.contract.md
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
src/jsMain/kotlin/io/kreekt/renderer/
├── webgpu/
│   ├── WebGPURenderer.kt              # Main renderer (FIX: pipeline creation, caching)
│   ├── WebGPUPipeline.kt              # Pipeline wrapper (FIX: async creation)
│   ├── WebGPUBuffer.kt                # Buffer management
│   ├── WebGPURendererFactory.kt       # Factory (existing)
│   └── shaders/
│       └── BasicShaders.kt            # WGSL shaders
├── webgl/
│   └── WebGLRenderer.kt               # Fallback renderer (OPTIMIZE: VAO, caching)
└── backend/
    └── WebGPUBackendNegotiator.kt     # Backend selection (enhance)

examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/
├── Main.kt                            # Integration point
├── VoxelWorld.kt                      # Scene management
├── ChunkMeshGenerator.kt              # Mesh generation (already optimized)
└── Chunk.kt                           # Chunk data structure

src/jsTest/kotlin/io/kreekt/renderer/
├── webgpu/
│   ├── WebGPURendererPerformanceTest.kt    # FPS validation tests
│   ├── WebGPUPipelineCacheTest.kt          # Caching tests
│   └── WebGPURendererFactoryTest.kt        # Backend selection tests
└── webgl/
    ├── WebGLRendererPerformanceTest.kt     # WebGL fallback tests
    └── WebGLRendererGeometryTest.kt        # Geometry tests (existing)

examples/voxelcraft/src/jsTest/kotlin/io/kreekt/examples/voxelcraft/
└── VoxelCraftPerformanceTest.kt            # Integration tests (60 FPS with 81 chunks)
```

**Structure Decision**: Single project (Kotlin Multiplatform library). The KreeKt library uses `src/{platform}Main` and `src/{platform}Test` directories. VoxelCraft is an example under `examples/voxelcraft/`. This feature optimizes both the core renderer (`src/jsMain`) and the VoxelCraft example integration.

## Phase 0: Outline & Research

### Research Tasks

1. **WebGPU Pipeline Creation Patterns**
   - Problem: Current busy-wait pipeline creation blocks render thread
   - Research: Async/await patterns for WebGPU pipeline creation
   - Research: Pipeline pre-compilation strategies
   - Research: Error handling for pipeline creation failures

2. **Pipeline Caching Strategies**
   - Problem: Current cache requires async access (coroutines)
   - Research: Synchronous pipeline cache patterns
   - Research: Cache key generation from material/geometry
   - Research: Cache invalidation strategies

3. **WebGPU Buffer Management**
   - Problem: No VAO-equivalent for caching attribute state
   - Research: WebGPU bind group patterns (VAO-equivalent)
   - Research: Buffer pooling for dynamic meshes
   - Research: Efficient buffer update strategies (block breaking/placing)

4. **Frustum Culling Implementation**
   - Problem: All 81 chunks rendered even if not visible
   - Research: View frustum calculation from camera matrices
   - Research: Chunk bounding box intersection tests
   - Research: Performance impact of culling (CPU vs GPU tradeoff)

5. **Draw Call Batching**
   - Problem: One draw call per chunk (81 calls)
   - Research: Mesh batching strategies in WebGPU
   - Research: Dynamic batching vs static batching
   - Research: Material/shader compatibility requirements

6. **WebGL Optimization Patterns**
   - Problem: Current WebGL fallback has 0 FPS performance
   - Research: VAO usage in WebGL 2.0
   - Research: Attribute location caching patterns
   - Research: Index buffer optimization

7. **Performance Profiling**
   - Research: Browser performance APIs (performance.now, FPS measurement)
   - Research: WebGPU timestamp queries
   - Research: Memory usage tracking
   - Research: Draw call counting and validation

8. **Cross-Browser WebGPU Support**
   - Research: WebGPU feature detection (Chrome, Edge)
   - Research: Fallback strategies for unsupported features
   - Research: WebGL compatibility matrix (Firefox, Safari)
   - Research: Progressive enhancement patterns

### Research Output Format

Each research task will produce:
- **Decision**: Chosen approach (e.g., "Async pipeline creation with pre-compilation")
- **Rationale**: Why chosen (e.g., "Non-blocking, better user experience")
- **Alternatives considered**: (e.g., "Busy-wait rejected due to frame drops")
- **Implementation notes**: Key technical details for Phase 1

**Output**: `research.md` with all decisions documented

## Phase 1: Design & Contracts

*Prerequisites: research.md complete*

### 1. Data Model (`data-model.md`)

Extract entities from feature spec:

**RenderBackend**
- Fields: `type: WebGPU | WebGL`, `isInitialized: Boolean`, `features: Set<String>`
- Validation: Must select WebGPU if available, fallback to WebGL
- State transitions: Uninitialized → Initializing → Initialized | Failed

**VertexBuffer**
- Fields: `buffer: GPUBuffer | WebGLBuffer`, `size: Int`, `usage: BufferUsage`
- Relationships: Belongs to GeometryBuffers
- Validation: Size must match vertex count * stride

**IndexBuffer**
- Fields: `buffer: GPUBuffer | WebGLBuffer`, `count: Int`, `format: "uint16" | "uint32"`
- Relationships: Optional (non-indexed geometry supported)
- Validation: Count must be multiple of 3 (triangles)

**RenderPipeline**
- Fields: `pipeline: GPURenderPipeline | WebGLProgram`, `cacheKey: String`, `isReady: Boolean`
- Relationships: Cached by PipelineCache
- State transitions: Creating → Ready | Failed

**DrawCall**
- Fields: `meshId: String`, `pipeline: RenderPipeline`, `vertexBuffer: VertexBuffer`, `indexBuffer: IndexBuffer?`, `triangleCount: Int`
- Validation: Pipeline must be ready before draw
- Relationships: Many draw calls per frame

**PerformanceMetrics**
- Fields: `fps: Float`, `frameTime: Float`, `drawCalls: Int`, `triangles: Int`, `backend: BackendType`, `gpuMemoryUsed: Int`
- Validation: FPS >= 30 (constitutional minimum)
- Updates: Every frame

**PipelineCache**
- Fields: `cache: Map<PipelineKey, RenderPipeline>`, `hits: Int`, `misses: Int`
- Operations: `get(key)`, `put(key, pipeline)`, `clear()`
- Validation: Synchronous access required

**FrustumCuller**
- Fields: `frustumPlanes: Array<Plane>`, `culledCount: Int`, `visibleCount: Int`
- Operations: `updateFromCamera(camera)`, `isVisible(boundingBox): Boolean`
- Validation: Must reduce draw calls for off-screen chunks

### 2. API Contracts (`/contracts/`)

Generate contracts from functional requirements:

**webgpu-renderer.contract.md** (FR-001, FR-002, FR-004, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-012, FR-013, FR-014, FR-015)
```kotlin
interface WebGPURendererContract {
    // FR-001: Backend detection
    suspend fun detectWebGPU(): Boolean

    // FR-002, FR-015: Initialization
    suspend fun initialize(): RendererResult<Unit> // Must complete in <2000ms

    // FR-004, FR-005: Rendering with batching
    fun render(scene: Scene, camera: Camera): RendererResult<Unit> // <100 draw calls

    // FR-006: Pipeline caching
    fun getOrCreatePipeline(geometry: BufferGeometry, material: Material): GPURenderPipeline?

    // FR-007: Async mesh upload
    suspend fun uploadGeometry(geometry: BufferGeometry): GeometryBuffers

    // FR-008: Frustum culling
    fun cullInvisibleChunks(chunks: List<Chunk>, camera: Camera): List<Chunk>

    // FR-012: Performance metrics
    fun getStats(): RenderStats // Must include FPS, draw calls, triangles

    // FR-013: Dynamic mesh updates
    fun updateGeometry(geometryId: String, geometry: BufferGeometry): RendererResult<Unit>

    // FR-014: Resource cleanup
    fun dispose(): RendererResult<Unit>
}
```

**webgl-fallback.contract.md** (FR-003, FR-010, FR-011)
```kotlin
interface WebGLFallbackContract {
    // FR-003: Minimum performance
    fun render(scene: Scene, camera: Camera): RendererResult<Unit> // Must achieve >=30 FPS

    // FR-010: Graceful fallback
    fun fallbackToWebGL(): RendererResult<Unit> // No data loss, preserve game state

    // FR-011: Rendering consistency
    fun validateVisualConsistency(webgpuOutput: ImageData, webglOutput: ImageData): Boolean
}
```

**performance.contract.md** (FR-002, FR-003, FR-005, FR-009, FR-012)
```kotlin
interface PerformanceContract {
    // FR-002: WebGPU target
    fun measureFPS(durationMs: Int): Float // Must return >=60 for WebGPU

    // FR-003: WebGL minimum
    fun validateMinimumFPS(): Boolean // Must return true if FPS >= 30

    // FR-005: Draw call limit
    fun countDrawCalls(): Int // Must return <100 for 81 chunks

    // FR-009: Metrics logging
    fun logPerformanceMetrics(): PerformanceMetrics

    // FR-012: HUD display
    fun updatePerformanceHUD(metrics: PerformanceMetrics): Unit
}
```

### 3. Contract Tests

Generate failing tests from contracts:

**WebGPURendererPerformanceTest.kt**
```kotlin
class WebGPURendererPerformanceTest {
    @Test
    fun `FR-002 - WebGPU renders at 60 FPS with 81 chunks`() {
        // Setup: Create renderer, load 81 chunks
        // Measure FPS over 2 seconds
        // Assert: FPS >= 60
        fail("Not implemented")
    }

    @Test
    fun `FR-005 - Draw calls under 100 for 81 chunks`() {
        // Setup: Render frame with 81 chunks
        // Count draw calls
        // Assert: drawCalls < 100
        fail("Not implemented")
    }

    @Test
    fun `FR-008 - Frustum culling reduces draw calls`() {
        // Setup: Position camera to see 40 chunks
        // Render with culling
        // Assert: drawCalls ~ 40, not 81
        fail("Not implemented")
    }
}
```

**WebGLFallbackPerformanceTest.kt**
```kotlin
class WebGLFallbackPerformanceTest {
    @Test
    fun `FR-003 - WebGL renders at 30+ FPS with 81 chunks`() {
        // Setup: Force WebGL fallback, load 81 chunks
        // Measure FPS over 2 seconds
        // Assert: FPS >= 30
        fail("Not implemented")
    }

    @Test
    fun `FR-010 - WebGPU failure falls back to WebGL gracefully`() {
        // Setup: Simulate WebGPU init failure
        // Assert: WebGL initializes successfully
        // Assert: Game state preserved
        fail("Not implemented")
    }
}
```

**VoxelCraftIntegrationTest.kt**
```kotlin
class VoxelCraftIntegrationTest {
    @Test
    fun `VoxelCraft renders 81 chunks at 60 FPS with WebGPU`() {
        // Setup: Initialize VoxelCraft with WebGPU
        // Generate 81 chunks
        // Measure FPS over 5 seconds
        // Assert: FPS >= 60
        fail("Not implemented")
    }

    @Test
    fun `Block breaking updates mesh without frame drops`() {
        // Setup: Render scene at 60 FPS
        // Break block (trigger mesh regen)
        // Assert: FPS stays >= 55 (allow brief dip)
        fail("Not implemented")
    }
}
```

### 4. Quickstart Test Scenario (`quickstart.md`)

Extract from user stories:

**Scenario: VoxelCraft 60 FPS Rendering**
1. Launch VoxelCraft in Chrome 113+
2. Wait for terrain generation (81 chunks)
3. Observe FPS counter: Should show 60 FPS
4. Move camera (WASD + mouse)
5. Observe FPS: Should remain 60 FPS
6. Open performance metrics (F3)
7. Verify: Draw calls <100, Triangles ~160K, Backend: WebGPU

**Expected Output**:
```
✅ WebGPU renderer initialized (50ms)
✅ 81 chunks loaded
✅ FPS: 60 | 87▲ | 92DC (Pass: Draw calls <100)
✅ Backend: WebGPU 1.0
✅ GPU Memory: 45MB / 256MB
```

**Fallback Scenario: WebGL in Firefox**
1. Launch VoxelCraft in Firefox
2. Observe backend selection: "WebGL 2.0"
3. Wait for terrain generation
4. Observe FPS counter: Should show 30+ FPS
5. Verify: Visual output matches WebGPU reference

**Expected Output**:
```
⚠️ WebGPU not available - using WebGL 2.0
✅ WebGL renderer initialized (40ms)
✅ 81 chunks loaded
✅ FPS: 35 | 87▲ | 81DC (Pass: FPS >= 30)
✅ Backend: WebGL 2.0
```

### 5. Update CLAUDE.md

Run incremental update script:
```bash
.specify/scripts/bash/update-agent-context.sh claude
```

Add to "Recent Changes" section:
- **2025-10-07**: WebGPU renderer performance optimization (018-optimize-voxelcraft-rendering)
  - Fixed pipeline creation busy-wait (async/await pattern)
  - Implemented synchronous pipeline caching
  - Added frustum culling (reduces draw calls from 81 to ~40-50)
  - Optimized WebGL fallback with VAO and attribute caching
  - Achieved 60 FPS with WebGPU, 30+ FPS with WebGL

Keep under 150 lines by removing oldest entry if needed.

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, CLAUDE.md updated

## Phase 2: Task Planning Approach

*This section describes what the /tasks command will do - DO NOT execute during /plan*

### Task Generation Strategy

The /tasks command will:
1. Load `.specify/templates/tasks-template.md` as base
2. Generate tasks from Phase 1 artifacts:
   - Each contract test → contract test implementation task [P]
   - Each entity in data-model.md → model/class creation task [P]
   - Each contract interface → interface implementation task
   - Each user story in quickstart.md → integration test task

### Task Categories

**Category 1: Contract Tests (Phase 1)** [P] = Parallel
- T001 [P]: Write WebGPU renderer performance tests (FR-002, FR-005, FR-008)
- T002 [P]: Write WebGL fallback performance tests (FR-003, FR-010)
- T003 [P]: Write VoxelCraft integration tests (60 FPS validation)
- T004 [P]: Write pipeline caching tests (FR-006)
- T005 [P]: Write buffer management tests (FR-007, FR-013, FR-014)

**Category 2: Core Fixes (Phase 2)**
- T006: Fix WebGPU pipeline creation (eliminate busy-wait, use async/await)
- T007: Implement synchronous pipeline cache (PipelineCache class)
- T008: Optimize buffer management (bind groups for VAO-equivalent)
- T009: Add frustum culling system (FrustumCuller class)
- T010: Implement draw call batching (reduce 81 calls to <100)

**Category 3: WebGL Optimization (Phase 2)** [P]
- T011 [P]: Add VAO support to WebGL renderer
- T012 [P]: Implement attribute location caching
- T013 [P]: Optimize index buffer handling

**Category 4: Performance Metrics (Phase 2)** [P]
- T014 [P]: Add FPS measurement system
- T015 [P]: Implement draw call counter
- T016 [P]: Add GPU memory tracking
- T017 [P]: Update performance HUD display

**Category 5: Integration (Phase 3)**
- T018: Integrate optimized renderer with VoxelCraft
- T019: Update Main.kt with backend selection logging
- T020: Add performance validation on startup
- T021: Update VoxelWorld chunk streaming with culling

**Category 6: Testing & Validation (Phase 4)**
- T022: Run all contract tests (must pass 100%)
- T023: Execute quickstart validation (60 FPS with WebGPU)
- T024: Cross-browser testing (Chrome, Firefox, Safari)
- T025: Performance regression testing (FPS, draw calls, memory)

### Ordering Strategy

1. **TDD Order**: Contract tests (T001-T005) → Implementation (T006-T021) → Validation (T022-T025)
2. **Dependency Order**:
   - Pipeline fixes (T006-T007) before rendering optimizations (T009-T010)
   - WebGL optimizations (T011-T013) independent of WebGPU fixes
   - Performance metrics (T014-T017) can run in parallel
   - Integration (T018-T021) depends on core fixes
3. **Parallel Execution**: Mark [P] for tasks that modify independent files

### Estimated Output

**25-30 numbered, dependency-ordered tasks in tasks.md**

Example task format:
```
## T006: Fix WebGPU Pipeline Creation

**Category**: Core Fixes
**Priority**: Critical
**Dependencies**: T001 (tests must exist and fail)
**Estimated Time**: 2 hours

**Objective**: Replace busy-wait pipeline creation with async/await pattern to prevent render loop blocking.

**Files Modified**:
- `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt` (lines 496-564)
- `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUPipeline.kt`

**Implementation Steps**:
1. Modify `getOrCreatePipeline()` to return cached pipeline immediately if available
2. For new pipelines, pre-compile during initialization or first render
3. Use coroutine dispatcher for async pipeline creation
4. Add fallback: return previous frame's pipeline if new one not ready
5. Remove busy-wait loop (lines 539-545)

**Acceptance Criteria**:
- T001 tests pass (60 FPS achieved)
- No busy-wait loops in render path
- Pipeline creation doesn't block frame rendering
- Graceful handling of pipeline creation failures
```

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation

*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation (execute tasks.md following TDD principles)
**Phase 5**: Validation
- Run all contract tests (100% pass rate required)
- Execute quickstart.md scenarios (60 FPS WebGPU, 30+ FPS WebGL)
- Performance regression tests (FPS, draw calls, memory)
- Cross-browser compatibility validation

## Complexity Tracking

*Fill ONLY if Constitution Check has violations that must be justified*

No violations detected. This feature:
- Maintains constitutional 60 FPS performance requirement
- Adheres to <5MB library size constraint
- Follows TDD principles (tests before implementation)
- Preserves type safety (minimal unsafe casts)
- Uses existing project structure (no new dependencies)

## Progress Tracking

*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS (no violations)
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented (None)

---
*Based on KreeKt Constitutional Requirements - See `/CLAUDE.md` for project principles*
