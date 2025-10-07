# Implementation Plan: Fix VoxelCraft Rendering Performance and Visual Issues

**Branch**: `017-in-voxelcraft-example` | **Date**: 2025-10-07 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/017-in-voxelcraft-example/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path ✓
   → Loaded successfully
2. Fill Technical Context ✓
   → Kotlin/JS project with WebGPU/WebGL rendering
   → Structure: Single project with examples/ subdirectory
3. Fill the Constitution Check section ✓
   → Based on CLAUDE.md constitutional requirements
4. Evaluate Constitution Check section ✓
   → No violations - fixing existing functionality
   → Update Progress Tracking: Initial Constitution Check ✓
5. Execute Phase 0 → research.md ✓
   → research.md complete with root cause analysis
6. Execute Phase 1 → contracts, data-model.md, quickstart.md ✓
   → data-model.md, contracts/renderer-contract.md, quickstart.md created
   → CLAUDE.md updated
7. Re-evaluate Constitution Check section ✓
   → No new violations introduced
   → Update Progress Tracking: Post-Design Constitution Check ✓
8. Plan Phase 2 → Describe task generation approach ✓
   → Task approach documented below
9. STOP - Ready for /tasks command ✓
```

**IMPORTANT**: The /plan command STOPS at step 8. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Fix VoxelCraft example rendering issues: 0 FPS performance problem and upside-down terrain visual bug. Verify WebGPU backend is being used correctly with proper WebGL fallback. Ensure example code doesn't duplicate library functionality. Technical approach: diagnose geometry pipeline, coordinate system consistency, renderer initialization, and game loop execution to achieve 60 FPS constitutional target.

## Technical Context
**Language/Version**: Kotlin 1.9+ with Multiplatform plugin, targeting JS/Browser
**Primary Dependencies**:
- kotlinx-coroutines-core:1.8.0
- kreekt-core (Vector3, Matrix4, math primitives)
- kreekt-renderer (WebGPU/WebGL abstraction)
- kreekt-scene (Object3D, Scene, Camera, Mesh)
- kreekt-geometry (BufferGeometry, BufferAttribute)

**Storage**: Browser LocalStorage for world state persistence (existing)
**Testing**: Kotlin/JS test framework, manual browser testing
**Target Platform**: Web browsers with WebGPU or WebGL 1.0+ support
**Project Type**: Single project (Kotlin Multiplatform library with examples)
**Performance Goals**:
- **Constitutional**: 60 FPS with 100k triangles (from CLAUDE.md Key Design Principles #3)
- **Target**: 30-60 FPS for VoxelCraft with 1,024 chunks
- Terrain generation <10 seconds

**Constraints**:
- <5MB base library size (constitutional requirement)
- Type safety: no runtime casts (constitutional requirement)
- Three.js API compatibility
- Cross-platform consistency between WebGPU and WebGL

**Scale/Scope**:
- 1,024 chunks (16x16x256 blocks each)
- ~100k triangles visible at once
- Greedy meshing optimization already implemented

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Based on CLAUDE.md constitutional requirements:

### ✅ Type Safety (Principle #1)
- **Status**: PASS - No new runtime casts introduced
- **Validation**: Fix maintains existing type-safe BufferGeometry and Renderer interfaces
- **Action**: Verify all geometry attribute access uses type-safe getAttribute() methods

### ✅ Three.js Compatibility (Principle #2)
- **Status**: PASS - Fixes enhance compatibility
- **Validation**: Ensures coordinate system matches Three.js Y-up convention
- **Action**: Verify winding order and matrix transformations match Three.js behavior

### ✅ Performance Target (Principle #3)
- **Status**: CRITICAL - Currently failing (0 FPS)
- **Constitutional Requirement**: 60 FPS with 100k triangles
- **Current State**: 0 FPS (blocking issue)
- **Target**: Restore to 30-60 FPS range
- **Action**: Diagnose and fix rendering pipeline to meet constitutional performance requirement

### ✅ Size Constraint (Principle #4)
- **Status**: PASS - No new code added to library
- **Constitutional Requirement**: <5MB base library
- **Action**: Fix involves debugging/refactoring existing code, no size impact

### ✅ Modern Graphics (Principle #5)
- **Status**: NEEDS VERIFICATION - WebGPU selection unclear
- **Constitutional Requirement**: WebGPU-first with sensible fallbacks
- **Action**: Verify WebGPURendererFactory correctly detects and uses WebGPU when available

### Summary
- **Violations**: None (fixing existing broken functionality)
- **Critical Issues**: Performance currently fails constitutional 60 FPS requirement
- **Justification**: N/A - restoration of constitutional compliance

## Project Structure

### Documentation (this feature)
```
specs/017-in-voxelcraft-example/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
│   └── renderer-contract.md  # Expected renderer behavior
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
examples/voxelcraft/
├── src/jsMain/kotlin/io/kreekt/examples/voxelcraft/
│   ├── Main.kt                      # Game entry point, renderer initialization
│   ├── ChunkMeshGenerator.kt        # Greedy meshing algorithm
│   ├── VoxelWorld.kt                # World state management
│   └── [other game logic files]
└── build/dist/js/productionExecutable/
    └── [compiled JS + HTML for testing]

src/jsMain/kotlin/io/kreekt/
├── renderer/
│   ├── webgpu/
│   │   ├── WebGPURenderer.kt        # WebGPU implementation
│   │   └── WebGPURendererFactory.kt # Backend selection logic
│   ├── webgl/
│   │   └── WebGLRenderer.kt         # WebGL fallback implementation
│   └── Renderer.kt                  # Renderer interface
├── geometry/
│   └── BufferGeometry.kt            # Geometry data structure
└── core/scene/
    └── Mesh.kt                      # Mesh object

tests/
└── [test files to be created in Phase 1]
```

**Structure Decision**: Single project structure with examples/ subdirectory. VoxelCraft is an example application demonstrating library capabilities. Fixes will target both example code (Main.kt, ChunkMeshGenerator.kt) and library renderer implementations (WebGLRenderer.kt, WebGPURenderer.kt) as needed.

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - Root cause of 0 FPS performance issue (not yet identified)
   - Cause of upside-down terrain rendering
   - WebGPU vs WebGL backend selection verification
   - Geometry pipeline issues (indexed vs non-indexed rendering)

2. **Research tasks to dispatch**:
   - **Task 1**: Analyze rendering pipeline from Main.kt gameLoop() → renderer.render() → WebGLRenderer.renderScene()
   - **Task 2**: Investigate ChunkMeshGenerator coordinate system and winding order
   - **Task 3**: Trace BufferGeometry attribute creation and binding in WebGLRenderer
   - **Task 4**: Verify WebGPURendererFactory backend detection and initialization
   - **Task 5**: Review Three.js coordinate system conventions (Y-up, right-handed, CCW winding)
   - **Task 6**: Examine console output from previous session for diagnostic clues

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was found/chosen]
   - Rationale: [why this is the root cause]
   - Alternatives considered: [other hypotheses evaluated]

**Output**: research.md with root cause analysis and fix strategies

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - **BufferGeometry**: vertices (Float32Array), normals, colors, UVs, indices (Uint16Array)
   - **ChunkMesh**: position, geometry, material, world matrix
   - **RenderStats**: FPS, triangle count, draw calls
   - **RendererState**: initialization status, context validity, shader programs

2. **Generate API contracts** from functional requirements:
   - **Contract 1**: Renderer.render(scene, camera) must achieve ≥30 FPS with typical geometry
   - **Contract 2**: BufferGeometry with indices must use drawElements() rendering path
   - **Contract 3**: BufferGeometry without indices must use drawArrays() rendering path
   - **Contract 4**: WebGPURendererFactory.create() must log backend selection decision
   - **Contract 5**: ChunkMeshGenerator must produce geometry with CCW winding order
   - Output contracts to `/contracts/renderer-contract.md`

3. **Generate contract tests** from contracts:
   - `tests/renderer/WebGLRendererPerformanceTest.kt` - Assert FPS ≥30 with test geometry
   - `tests/renderer/WebGLRendererIndexedGeometryTest.kt` - Verify indexed rendering path
   - `tests/renderer/WebGLRendererNonIndexedGeometryTest.kt` - Verify non-indexed rendering path
   - `tests/renderer/WebGPUFactoryTest.kt` - Verify backend selection logging
   - `tests/voxelcraft/ChunkMeshWindingTest.kt` - Verify CCW winding order
   - Tests must fail initially (TDD approach)

4. **Extract test scenarios** from user stories:
   - Scenario 1: Load VoxelCraft → Verify FPS counter shows ≥30 FPS
   - Scenario 2: Load VoxelCraft → Verify terrain appears right-side up (grass on top)
   - Scenario 3: Check console → Verify backend selection logged
   - Quickstart test validates all three scenarios

5. **Update CLAUDE.md incrementally** (O(1) operation):
   - Run `.specify/scripts/bash/update-agent-context.sh claude`
   - Add VoxelCraft rendering fix to Recent Changes
   - Preserve existing manual content
   - Keep under 150 lines

**Output**: data-model.md, contracts/renderer-contract.md, failing tests, quickstart.md, updated CLAUDE.md

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate diagnostic tasks first (understand before fix)
- Generate contract test tasks (TDD approach)
- Generate implementation tasks (make tests pass)
- Generate validation tasks (end-to-end testing)

**Task Categories**:

1. **Diagnostic Tasks** (T001-T005):
   - T001: Run VoxelCraft and capture console output, browser DevTools
   - T002: Add debug logging to WebGLRenderer.renderGeometry() to trace execution
   - T003: Add debug logging to ChunkMeshGenerator to verify geometry generation
   - T004: Verify WebGPURendererFactory backend selection logic
   - T005: Inspect generated geometry in browser console (vertex counts, indices)

2. **Contract Test Tasks** (T006-T010):
   - T006: Write WebGLRendererPerformanceTest [MUST FAIL initially]
   - T007: Write WebGLRendererIndexedGeometryTest [MUST FAIL initially]
   - T008: Write WebGLRendererNonIndexedGeometryTest [MUST FAIL initially]
   - T009: Write WebGPUFactoryTest [MUST FAIL initially]
   - T010: Write ChunkMeshWindingTest [MUST FAIL initially]

3. **Implementation Tasks** (T011-T020):
   - Based on research.md findings:
     - Fix geometry attribute binding in WebGLRenderer
     - Fix winding order in ChunkMeshGenerator if needed
     - Fix coordinate system transformations
     - Fix renderer initialization sequence
     - Fix game loop timing/execution
     - Remove any duplicate rendering code from VoxelCraft example

4. **Validation Tasks** (T021-T023):
   - T021: Run all contract tests → verify PASS
   - T022: Execute quickstart.md manual test → verify FPS ≥30, terrain right-side up
   - T023: Performance validation → verify 60 FPS target approached

**Ordering Strategy**:
- Diagnostic tasks first (T001-T005) [SERIAL - build understanding]
- Contract tests next (T006-T010) [P] parallel where tests are independent
- Implementation tasks (T011-T020) [SERIAL - fixes depend on diagnostics]
- Validation tasks last (T021-T023) [SERIAL - verify fixes work]

**Estimated Output**: 20-25 numbered, ordered tasks in tasks.md

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation (execute tasks.md following constitutional principles)
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

**Justification**: This feature fixes existing broken functionality to restore constitutional compliance (60 FPS performance requirement). No new complexity introduced. All changes are debugging and refactoring of existing code.

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [x] Phase 3: Tasks generated (/tasks command) - 24 tasks created
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved (none in spec)
- [x] Complexity deviations documented (none)

---
*Based on Constitution from CLAUDE.md - Key Design Principles*
