# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

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

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]  
**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]  
**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]  
**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]  
**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]
**Project Type**: [single/web/mobile - determines source structure]  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

[Gates determined based on constitution file]

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

<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

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

The /tasks command will generate implementation tasks following TDD principles and dependency order:

### 1. Contract Test Tasks (TDD Red Phase)

Generate one test task per contract API:

- **T001**: Write BufferManager contract tests (10 tests: createVertexBuffer, createIndexBuffer, createUniformBuffer,
  updateUniformBuffer, destroyBuffer) [P]
- **T002**: Write RenderPassManager contract tests (7 tests: beginRenderPass, bindPipeline, bindBuffers, drawIndexed,
  endRenderPass) [P]
- **T003**: Write SwapchainManager contract tests (4 tests: acquireNextImage, presentImage, recreateSwapchain,
  getExtent) [P]

### 2. Platform Implementation Tasks (TDD Green Phase)

Generate implementation tasks for each platform (JVM/Vulkan, JS/WebGPU):

**JVM/Vulkan**:

- **T004**: Implement VulkanBufferManager (createVertexBuffer, createIndexBuffer, createUniformBuffer) [Depends: T001]
- **T005**: Implement VulkanBufferManager (updateUniformBuffer, destroyBuffer) [Depends: T004]
- **T006**: Implement VulkanRenderPassManager (beginRenderPass, endRenderPass) [Depends: T001]
- **T007**: Implement VulkanRenderPassManager (bind* methods, drawIndexed) [Depends: T006]
- **T008**: Implement VulkanSwapchain (create, acquireNextImage, presentImage) [Depends: T002]
- **T009**: Implement VulkanSwapchain (recreateSwapchain, resize handling) [Depends: T008]

**JS/WebGPU**:

- **T010**: Implement WebGPUBufferManager (createVertexBuffer, createIndexBuffer,
  createUniformBuffer) [P] [Depends: T001]
- **T011**: Implement WebGPUBufferManager (updateUniformBuffer, destroyBuffer) [Depends: T010]
- **T012**: Implement WebGPURenderPassManager (beginRenderPass, endRenderPass) [P] [Depends: T001]
- **T013**: Implement WebGPURenderPassManager (bind* methods, drawIndexed) [Depends: T012]
- **T014**: Implement WebGPUSwapchain (configure, getCurrentTexture, present) [P] [Depends: T002]
- **T015**: Implement WebGPUSwapchain (recreate, resize handling) [Depends: T014]

### 3. Integration Tasks (Renderer.render() Implementation)

Update VulkanRenderer and WebGPURenderer to use new APIs:

- **T016**: Update VulkanRenderer.render() to use BufferManager and RenderPassManager [Depends: T009]
- **T017**: Update WebGPURenderer.render() to use BufferManager and RenderPassManager [Depends: T015]
- **T018**: Update VulkanRenderer.resize() to recreate swapchain [Depends: T016]
- **T019**: Update WebGPURenderer.resize() to recreate swapchain [Depends: T017]

### 4. VoxelCraft Integration Tasks

Migrate VoxelCraft from OpenGL/WebGL to Vulkan/WebGPU:

- **T020**: Update VoxelWorld to upload chunks using BufferManager [Depends: T016, T017] [P]
- **T021**: Update VoxelCraft JS Main.kt to use WebGPURenderer.render() [Depends: T020]
- **T022**: Update VoxelCraft JVM MainJvm.kt to use VulkanRenderer.render() [Depends: T020]
- **T023**: Remove OpenGL/WebGL API calls from VoxelCraft [Depends: T021, T022]

### 5. Performance Validation Tasks

Run performance benchmarks from Feature 019:

- **T024**: Run PerformanceBenchmarkTest (100k triangles, FPS validation) [Depends: T023]
- **T025**: Run visual regression tests (SSIM comparison) [Depends: T023]
- **T026**: Validate memory usage < 500MB [Depends: T024]

### 6. Test Validation Tasks (TDD Green Confirmation)

Verify all Feature 019 contract tests pass:

- **T027**: Run BackendDetectionTest [Depends: T023]
- **T028**: Run RendererLifecycleTest [Depends: T023]
- **T029**: Run ErrorHandlingTest [Depends: T023]
- **T030**: Run CapabilityDetectionTest [Depends: T023]

### 7. Production Readiness Tasks

Final cleanup and validation:

- **T031**: Remove all TODO/FIXME/STUB from rendering pipeline [Depends: T030]
- **T032**: Add comprehensive logging to all rendering paths [Depends: T031]
- **T033**: Validate resource cleanup in Renderer.dispose() [Depends: T032]
- **T034**: Update CLAUDE.md with Feature 020 implementation notes [Depends: T033]

**Ordering Strategy**:

1. **TDD Order**: Tests (T001-T003) → Implementation (T004-T015) → Integration (T016-T019)
2. **Dependency Order**: Buffers → Render passes → Integration → VoxelCraft
3. **Parallel Execution**: Tasks marked [P] can run concurrently (independent files)

**Estimated Task Count**: 34 tasks (17 core implementation + 7 integration + 7 validation + 3 production readiness)

**Estimated Timeline**:

- Phase 2 (Task generation): 1 hour (/tasks command)
- Phase 3 (Contract tests): 4-6 hours (T001-T003)
- Phase 3 (Implementation): 16-20 hours (T004-T015)
- Phase 3 (Integration): 8-10 hours (T016-T023)
- Phase 3 (Validation): 4-6 hours (T024-T030)
- Phase 3 (Production): 2-3 hours (T031-T034)
- **Total**: 35-45 hours (5-6 working days)

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

- [x] Phase 0: Research complete (/plan command) ✅ research.md created
- [x] Phase 1: Design complete (/plan command) ✅ data-model.md, contracts/, quickstart.md, CLAUDE.md updated
- [x] Phase 2: Task planning complete (/plan command - describe approach only) ✅ 34 tasks planned
- [x] Phase 3: Tasks generated (/tasks command) ✅ tasks.md created with 35 tasks
- [ ] Phase 4: Implementation complete - Ready to execute tasks
- [ ] Phase 5: Validation passed

**Gate Status**:

- [x] Initial Constitution Check: PASS ✅ No violations
- [x] Post-Design Constitution Check: PASS ✅ No new violations
- [x] All NEEDS CLARIFICATION resolved ✅ All research complete
- [x] Complexity deviations documented ✅ None required

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
