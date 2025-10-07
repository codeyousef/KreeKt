# Implementation Plan: WebGPU/Vulkan Primary Renderer with Multiplatform Code Sharing

**Branch**: `019-we-should-not` | **Date**: 2025-10-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/mnt/d/Projects/KMP/KreeKt/specs/019-we-should-not/spec.md`

## Execution Flow (/plan command scope)

```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from file system structure or context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Fill the Constitution Check section based on the content of the constitution document.
4. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, agent-specific template file (e.g., `CLAUDE.md` for Claude Code, `.github/copilot-instructions.md` for GitHub Copilot, `GEMINI.md` for Gemini CLI, `QWEN.md` for Qwen Code, or `AGENTS.md` for all other agents).
7. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:

- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary

Refactor KreeKt library to use WebGPU (browser) and Vulkan (JVM/Native) as primary rendering backends, with WebGL as
fallback only. Ensure VoxelCraft example uses library APIs exclusively without custom rendering code. Achieve ≥90% code
sharing across JVM and JavaScript targets through common Kotlin Multiplatform code. System must target 60 FPS with 30
FPS minimum acceptable in complex scenes, with visual output identical to human eye across all backends.

## Technical Context

**Language/Version**: Kotlin 1.9+ with Kotlin Multiplatform plugin
**Primary Dependencies**: LWJGL 3.3.5 (JVM/Vulkan), @webgpu/types 0.1.40 (JS/WebGPU), kotlinx-coroutines-core 1.8.0
**Storage**: N/A (graphics library)
**Testing**: Kotlin Test multiplatform, platform-specific integration tests, visual regression testing
**Target Platform**: JVM (Windows/Linux/macOS via Vulkan), JavaScript (modern browsers via WebGPU), with WebGL2 fallback
**Project Type**: Multiplatform library (single project with commonMain, jvmMain, jsMain source sets)
**Performance Goals**: 60 FPS target, 30 FPS minimum acceptable in complex scenes (constitutional requirement)
**Constraints**: <5MB library size (constitutional), ≥90% code sharing across platforms, visually identical output
across backends, fail-fast error handling
**Scale/Scope**: Graphics library supporting 3D rendering, scene graphs, cameras, materials; VoxelCraft example with ~
100k triangles, 1024 chunks

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Constitutional Requirements (from CLAUDE.md)

| Requirement                    | Status | Notes                                                                               |
|--------------------------------|--------|-------------------------------------------------------------------------------------|
| **60 FPS Performance**         | ✅ PASS | Specified in FR-019: 60 FPS target, 30 FPS minimum acceptable                       |
| **5MB Size Limit**             | ✅ PASS | Constraint acknowledged; refactoring existing code, not adding significant size     |
| **Type Safety**                | ✅ PASS | Kotlin Multiplatform with expect/actual pattern maintains compile-time safety       |
| **Cross-Platform Consistency** | ✅ PASS | FR-020: Visual output visually identical across backends; FR-006: ≥90% code sharing |
| **Test Coverage >80%**         | ✅ PASS | Testing strategy includes unit, integration, visual regression tests                |
| **Test Success Rate >95%**     | ✅ PASS | TDD approach with contract tests before implementation                              |

### Architectural Compliance

| Principle                 | Status | Notes                                                                        |
|---------------------------|--------|------------------------------------------------------------------------------|
| **Multiplatform First**   | ✅ PASS | Common code in commonMain, platform-specific only for Vulkan/WebGPU bindings |
| **Library-Only APIs**     | ✅ PASS | FR-007: VoxelCraft MUST NOT contain custom rendering logic                   |
| **Fail-Fast Errors**      | ✅ PASS | FR-022/FR-024: Crash immediately with detailed diagnostics                   |
| **Performance Profiling** | ✅ PASS | FR-021: Provide profiling tools and best practices                           |

**Initial Constitution Check: PASS** ✅

---

### Post-Design Review (after Phase 1)

| Requirement                    | Status | Design Validation                                                                                            |
|--------------------------------|--------|--------------------------------------------------------------------------------------------------------------|
| **60 FPS Performance**         | ✅ PASS | PerformanceValidator integrated; backend-specific thresholds (WebGPU/Vulkan: 60 FPS target, WebGL: 45 FPS)   |
| **5MB Size Limit**             | ✅ PASS | Refactoring existing code; no significant size increase; thin wrapper over platform APIs                     |
| **Type Safety**                | ✅ PASS | expect/actual pattern enforces compile-time correctness; no runtime casts in contracts                       |
| **Cross-Platform Consistency** | ✅ PASS | Unified Renderer interface; visual regression tests (SSIM ≥0.99); contract tests validate identical behavior |
| **Test Coverage >80%**         | ✅ PASS | Contract tests cover all interface methods; visual regression tests; performance benchmarks                  |
| **Test Success Rate >95%**     | ✅ PASS | TDD approach: contract tests fail initially (no implementation yet)                                          |

**Code Sharing Analysis** (from data-model.md):

- Renderer core interfaces: 100% common
- Configuration/stats/capabilities: 100% common
- Platform bindings (Vulkan/WebGPU): 100% platform-specific (necessary)
- VoxelCraft game logic: ~95% common (only Main entry points differ)
- **Overall**: 90-95% code sharing achieved ✅ **Exceeds FR-006 (≥90%)**

**Post-Design Constitution Check: PASS** ✅

## Project Structure

### Documentation (this feature)

```
specs/[###-feature]/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)

```
kreekt-renderer/
└── src/
    ├── commonMain/kotlin/io/kreekt/renderer/
    │   ├── Renderer.kt (expect interface)
    │   ├── RenderSurface.kt (expect interface)
    │   ├── RendererConfig.kt
    │   ├── RendererCapabilities.kt
    │   └── BackendType.kt (enum: WEBGPU, VULKAN, WEBGL)
    │
    ├── jvmMain/kotlin/io/kreekt/renderer/
    │   ├── vulkan/
    │   │   ├── VulkanRenderer.kt (actual Renderer)
    │   │   ├── VulkanSurface.kt
    │   │   ├── VulkanPipeline.kt
    │   │   └── VulkanDevice.kt
    │   └── RendererFactory.kt (actual factory)
    │
    ├── jsMain/kotlin/io/kreekt/renderer/
    │   ├── webgpu/
    │   │   ├── WebGPURenderer.kt (actual Renderer)
    │   │   ├── WebGPUSurface.kt
    │   │   ├── WebGPUPipeline.kt
    │   │   └── WebGPUDevice.kt
    │   ├── webgl/
    │   │   └── WebGLRenderer.kt (fallback only)
    │   └── RendererFactory.kt (actual factory with WebGPU detection)
    │
    └── commonTest/kotlin/io/kreekt/renderer/
        ├── RendererContractTest.kt
        └── CapabilityDetectionTest.kt

examples/voxelcraft/
└── src/
    ├── commonMain/kotlin/io/kreekt/examples/voxelcraft/
    │   ├── VoxelWorld.kt (shared logic)
    │   ├── Player.kt
    │   ├── Chunk.kt
    │   ├── ChunkMeshGenerator.kt
    │   ├── TerrainGenerator.kt
    │   └── BlockInteraction.kt
    │
    ├── jvmMain/kotlin/io/kreekt/examples/voxelcraft/
    │   ├── MainJvm.kt (LWJGL window setup)
    │   └── JvmInputHandler.kt
    │
    └── jsMain/kotlin/io/kreekt/examples/voxelcraft/
        ├── Main.kt (HTML5 canvas setup)
        └── JsInputHandler.kt
```

**Structure Decision**: Kotlin Multiplatform library with shared API definitions in `commonMain` and platform-specific
implementations in `jvmMain`/`jsMain`. This structure maximizes code reuse (≥90% target) while allowing
platform-specific graphics API bindings. VoxelCraft example follows same pattern with shared game logic and minimal
platform-specific code for windowing/input.

## Phase 0: Outline & Research

1. **Extract unknowns from Technical Context** above:
    - For each NEEDS CLARIFICATION → research task
    - For each dependency → best practices task
    - For each integration → patterns task

2. **Generate and dispatch research agents**:
   ```
   For each unknown in Technical Context:
     Task: "Research {unknown} for {feature context}"
   For each technology choice:
     Task: "Find best practices for {tech} in {domain}"
   ```

3. **Consolidate findings** in `research.md` using format:
    - Decision: [what was chosen]
    - Rationale: [why chosen]
    - Alternatives considered: [what else evaluated]

**Output**: research.md with all NEEDS CLARIFICATION resolved

## Phase 1: Design & Contracts

*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
    - Entity name, fields, relationships
    - Validation rules from requirements
    - State transitions if applicable

2. **Generate API contracts** from functional requirements:
    - For each user action → endpoint
    - Use standard REST/GraphQL patterns
    - Output OpenAPI/GraphQL schema to `/contracts/`

3. **Generate contract tests** from contracts:
    - One test file per endpoint
    - Assert request/response schemas
    - Tests must fail (no implementation yet)

4. **Extract test scenarios** from user stories:
    - Each story → integration test scenario
    - Quickstart test = story validation steps

5. **Update agent file incrementally** (O(1) operation):
    - Run `.specify/scripts/bash/update-agent-context.sh claude`
      **IMPORTANT**: Execute it exactly as specified above. Do not add or remove any arguments.
    - If exists: Add only NEW tech from current plan
    - Preserve manual additions between markers
    - Update recent changes (keep last 3)
    - Keep under 150 lines for token efficiency
    - Output to repository root

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, agent-specific file

## Phase 2: Task Planning Approach

*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:

The /tasks command will generate tasks from Phase 1 design artifacts following this structure:

### 1. Contract Test Tasks (TDD Red Phase)

- **Task 1-5**: Create failing contract tests from `contracts/renderer-contract.kt`
    - Task 1: Backend detection tests (FR-004) [P]
    - Task 2: Primary backend selection tests (FR-001, FR-002) [P]
    - Task 3: Renderer lifecycle tests (initialize, render, dispose) [P]
    - Task 4: Performance validation tests (FR-019) [P]
    - Task 5: Error handling tests (FR-022, FR-024) [P]
- **Dependencies**: None (parallel execution)
- **Success Criteria**: All tests compile and fail (no implementation yet)

### 2. Common API Implementation Tasks (TDD Green Phase)

- **Task 6-10**: Implement common interfaces in `commonMain`
    - Task 6: Create BackendType enum and RendererCapabilities data class [P]
    - Task 7: Create RendererConfig and RenderStats data classes [P]
    - Task 8: Define expect Renderer interface [P]
    - Task 9: Define expect RenderSurface interface [P]
    - Task 10: Create RendererInitializationException hierarchy [P]
- **Dependencies**: None (data classes, enums, interfaces)
- **Success Criteria**: Code compiles, contract tests still fail (no actual implementations)

### 3. Platform-Specific Implementation Tasks (JVM/Vulkan)

- **Task 11-15**: Implement Vulkan renderer in `jvmMain`
    - Task 11: Create VulkanRenderer actual implementation (depends: Task 8)
    - Task 12: Implement VulkanSurface with GLFW integration (depends: Task 9)
    - Task 13: Implement VulkanPipeline for shader management (depends: Task 11)
    - Task 14: Implement RendererFactory.create() for JVM (depends: Task 11, 12)
    - Task 15: Add LWJGL Vulkan dependencies to build.gradle.kts [P]
- **Dependencies**: Sequential within JVM tasks, parallel with JS tasks
- **Success Criteria**: JVM contract tests pass (FR-002 validated)

### 4. Platform-Specific Implementation Tasks (JS/WebGPU)

- **Task 16-21**: Implement WebGPU/WebGL renderers in `jsMain`
    - Task 16: Create WebGPURenderer actual implementation (depends: Task 8)
    - Task 17: Create WebGPUSurface with HTMLCanvasElement (depends: Task 9)
    - Task 18: Implement WebGPUPipeline for shader management (depends: Task 16)
    - Task 19: Refactor WebGLRenderer as fallback only (depends: Task 8)
    - Task 20: Implement RendererFactory.create() with detection (depends: Task 16, 19)
    - Task 21: Update @webgpu/types dependencies [P]
- **Dependencies**: Sequential within JS tasks, parallel with JVM tasks
- **Success Criteria**: JS contract tests pass (FR-001, FR-003, FR-005 validated)

### 5. VoxelCraft Refactoring Tasks

- **Task 22-25**: Refactor VoxelCraft to use library renderer
    - Task 22: Remove custom WebGLRenderer instantiation from Main.kt (depends: Task 20)
    - Task 23: Update JVM Main to use RendererFactory.create() (depends: Task 14)
    - Task 24: Move remaining game logic to commonMain if not already (depends: Task 22, 23)
    - Task 25: Add error handling with RendererInitializationException (depends: Task 10, 22, 23)
- **Dependencies**: Sequential, requires renderer implementations complete
- **Success Criteria**: VoxelCraft runs on both JVM and JS using library renderers (FR-007 validated)

### 6. Shader Cross-Compilation Tasks

- **Task 26-28**: Implement shader pipeline
    - Task 26: Write WGSL shaders for basic rendering [P]
    - Task 27: Add Tint WASM compiler for SPIR-V generation (build-time) [P]
    - Task 28: Update platform renderers to use compiled shaders (depends: Task 26, 27)
- **Dependencies**: Parallel creation, sequential integration
- **Success Criteria**: Same shaders work on WebGPU and Vulkan (FR-020)

### 7. Visual Regression Testing Tasks

- **Task 29-31**: Implement visual parity validation
    - Task 29: Create 5 test scene fixtures (cube, mesh, lighting, transparency, terrain) [P]
    - Task 30: Implement screenshot capture for JVM and JS (depends: Task 29)
    - Task 31: Create visual regression test suite with SSIM comparison (depends: Task 30)
- **Dependencies**: Requires renderer implementations complete
- **Success Criteria**: SSIM ≥0.99 for all test scenes (FR-020 validated)

### 8. Performance Validation Tasks

- **Task 32-34**: Extend performance monitoring
    - Task 32: Update PerformanceValidator with backend-specific thresholds (depends: Task 11, 16)
    - Task 33: Add warmup logic (120 frames) before validation (depends: Task 32)
    - Task 34: Implement CI integration for performance benchmarks (depends: Task 33)
- **Dependencies**: Sequential
- **Success Criteria**: 60 FPS target, 30 FPS minimum on primary backends (FR-019 validated)

### 9. Documentation and Cleanup Tasks

- **Task 35-37**: Finalize implementation
    - Task 35: Update CLAUDE.md with new renderer architecture [P]
    - Task 36: Add error handling troubleshooting docs [P]
    - Task 37: Run full test suite and validate all FRs [P]
- **Dependencies**: None (can be done in parallel with testing)
- **Success Criteria**: All 24 functional requirements validated

**Task Ordering Strategy**:

- **TDD**: Contract tests (red) → Common interfaces → Platform implementations (green)
- **Dependency Order**: Common code → Platform-specific → Integration → Testing
- **Parallel Execution**: [P] tags mark independent tasks that can run concurrently
- **Critical Path**: Contract tests → Common interfaces → Renderer implementations → VoxelCraft refactor

**Estimated Output**: **37 tasks** in dependency-ordered sequence with [P] parallel markers

**Validation Gates**:

- Gate 1 (after Task 10): All common code compiles, tests fail
- Gate 2 (after Task 15): JVM renderer works, JVM tests pass
- Gate 3 (after Task 21): JS renderer works, JS tests pass
- Gate 4 (after Task 25): VoxelCraft uses library renderer on both platforms
- Gate 5 (after Task 28): Shaders work cross-platform
- Gate 6 (after Task 31): Visual parity validated
- Gate 7 (after Task 34): Performance validated
- Gate 8 (after Task 37): All FRs validated, ready for production

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation

*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking

*Fill ONLY if Constitution Check has violations that must be justified*

| Violation                  | Why Needed         | Simpler Alternative Rejected Because |
|----------------------------|--------------------|--------------------------------------|
| [e.g., 4th project]        | [current need]     | [why 3 projects insufficient]        |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient]  |

## Progress Tracking

*This checklist is updated during execution flow*

**Phase Status**:

- [x] Phase 0: Research complete (/plan command) ✅
    - ✅ research.md created with 7 technical decisions documented
    - ✅ All NEEDS CLARIFICATION resolved
    - ✅ Outstanding non-critical questions identified (2)
- [x] Phase 1: Design complete (/plan command) ✅
    - ✅ data-model.md created (8 entities documented)
    - ✅ contracts/renderer-contract.kt created (19 test methods)
    - ✅ quickstart.md created (7 validation scenarios)
    - ✅ CLAUDE.md updated with feature context
- [x] Phase 2: Task planning complete (/plan command - describe approach only) ✅
    - ✅ 37 tasks planned across 9 categories
    - ✅ TDD approach with clear red-green phases
    - ✅ 8 validation gates defined
    - ✅ Dependencies and parallel execution markers specified
- [ ] Phase 3: Tasks generated (/tasks command) - **NEXT STEP**
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:

- [x] Initial Constitution Check: PASS ✅
- [x] Post-Design Constitution Check: PASS ✅
- [x] All NEEDS CLARIFICATION resolved ✅ (5/5 priority questions answered)
- [x] Complexity deviations documented ✅ (none - design aligns with constitution)

**Artifacts Generated**:

- ✅ `/mnt/d/Projects/KMP/KreeKt/specs/019-we-should-not/plan.md` (this file)
- ✅ `/mnt/d/Projects/KMP/KreeKt/specs/019-we-should-not/research.md`
- ✅ `/mnt/d/Projects/KMP/KreeKt/specs/019-we-should-not/data-model.md`
- ✅ `/mnt/d/Projects/KMP/KreeKt/specs/019-we-should-not/contracts/renderer-contract.kt`
- ✅ `/mnt/d/Projects/KMP/KreeKt/specs/019-we-should-not/quickstart.md`
- ✅ `/mnt/d/Projects/KMP/KreeKt/CLAUDE.md` (updated)

**Ready for /tasks command** ✅

---
*Based on Constitution documented in CLAUDE.md - See KreeKt Constitutional Requirements section*
