# Tasks: Fix JS VoxelCraft Example Blank Screen

**Input**: Design documents from `/specs/021-the-js-voxelcraft/`
**Prerequisites**: plan.md, spec.md, research.md

**Tests**: The TDD approach was abandoned due to a broken test environment. The fix will be verified manually.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1)
- Includes exact file paths where known.

## Phase 1: Debugging

**Purpose**: Add logging to identify the root cause of the issue.

- [ ] T001 [US1] **Debug**: Add detailed diagnostic logging to `examples/voxelcraft/src/commonMain/kotlin/io/kreekt/examples/voxelcraft/ChunkMeshGenerator.kt` and `examples/voxelcraft/src/commonMain/kotlin/io/kreekt/examples/voxelcraft/VoxelWorld.kt`. The logs should trace the lifecycle of a chunk mesh: creation, geometry generation, and addition to the `world.scene`.

**Checkpoint**: Logging is in place to trace the mesh generation and scene population process.

---

## Phase 2: Implementation

**Goal**: Fix the blank screen bug so the VoxelCraft JS example renders the 3D scene correctly.

**Independent Test**: Manual verification using the steps in `quickstart.md`.

### Implementation for User Story 1

- [ ] T002 [US1] **Implement**: Based on the debugging information from T001, identify and fix the root cause of the issue. The fix is expected to be within `ChunkMeshGenerator.kt` or `VoxelWorld.kt`.

**Checkpoint**: The primary bug should be fixed.

---

## Phase 3: Polish & Verification

**Purpose**: Final cleanup and manual verification.

- [ ] T003 [US1] **Cleanup**: Remove the diagnostic logging added in T001 from `ChunkMeshGenerator.kt` and `VoxelWorld.kt`.
- [ ] T004 [US1] **Manual Test**: Follow the steps in `specs/021-the-js-voxelcraft/quickstart.md` to manually run the example and confirm that the scene renders correctly and is interactive.

---

## Dependencies & Execution Order

- **T001** (Debug) must be completed first.
- **T002** (Implement) depends on the findings from T001.
- **T003** and **T004** (Polish and Verify) should be done after the implementation is complete.

## Implementation Strategy

### Debug and Fix Workflow

1.  **Add Logging (T001)** to understand the flow of data.
2.  **Run the application** and analyze the logs.
3.  **Implement the fix (T002)** based on the analysis.
4.  **Clean up and manually test (T003-T004)** to ensure the fix is working and no new issues were introduced.
