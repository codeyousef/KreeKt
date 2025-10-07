# Feature Specification: Fix VoxelCraft Rendering Performance and Visual Issues

**Feature Branch**: `017-in-voxelcraft-example`
**Created**: 2025-10-07
**Status**: Draft
**Input**: User description: "in voxelcraft example the terrain is rendering very slowly at 0 fps and seems to be upside down. make sure we are using webgpu and the example does not have custom code that should be handled by the library and fix the issues with the example"

## Execution Flow (main)
```
1. Parse user description from Input
   → Identified: Performance issue (0 FPS), rendering bug (upside down terrain), backend verification needed
2. Extract key concepts from description
   → Actors: VoxelCraft example, WebGPU renderer, WebGL fallback
   → Actions: Diagnose performance, fix visual rendering, verify backend selection
   → Data: Mesh geometry, indices, vertex attributes
   → Constraints: Must achieve 60 FPS (constitutional requirement), WebGPU preferred
3. For each unclear aspect:
   → Terrain "upside down" could mean: Y-axis inverted, winding order reversed, or matrix transformation issue
4. Fill User Scenarios & Testing section
   → Clear user flow: Load VoxelCraft → See terrain render correctly at 60 FPS
5. Generate Functional Requirements
   → Each requirement must be testable ✓
6. Identify Key Entities (if data involved)
   → BufferGeometry, WebGPURenderer, WebGLRenderer, ChunkMeshGenerator
7. Run Review Checklist
   → No [NEEDS CLARIFICATION] markers
   → No implementation details in spec
8. Return: SUCCESS (spec ready for planning)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers

---

## User Scenarios & Testing

### Primary User Story
A developer runs the VoxelCraft example to see a playable Minecraft-like voxel building game. They expect the terrain to load and render correctly with smooth performance (60 FPS target per constitutional requirements). The terrain should appear right-side up with proper depth perception, and the renderer should automatically select the best available graphics backend (WebGPU or WebGL).

### Acceptance Scenarios

1. **Given** VoxelCraft example is started in a WebGPU-capable browser, **When** the game loads, **Then** the terrain renders correctly at target framerate (30-60 FPS) using WebGPU backend

2. **Given** VoxelCraft example is started in a WebGL-only browser, **When** the game loads, **Then** the terrain renders correctly at acceptable framerate (≥30 FPS) using WebGL fallback

3. **Given** terrain is visible on screen, **When** player looks at the world, **Then** terrain appears with correct orientation (grass on top, not inverted/flipped)

4. **Given** game is running, **When** console output is checked, **Then** backend selection is clearly logged (WebGPU or WebGL) with initialization success

5. **Given** multiple chunks are generated, **When** player moves through world, **Then** all chunk meshes render consistently without visual artifacts

### Edge Cases
- What happens when WebGPU initialization fails? (Should gracefully fall back to WebGL)
- How does system handle empty chunks with no visible faces? (Should skip rendering, not produce 0 FPS)
- What happens when geometry has only vertex data without indices? (Should use non-indexed rendering path)
- How does system handle coordinate system differences between Three.js and KreeKt? (Should render consistently with library conventions)

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST render VoxelCraft terrain at minimum 30 FPS with target 60 FPS (constitutional performance requirement)

- **FR-002**: System MUST display terrain with correct visual orientation (Y-up coordinate system, grass blocks on top)

- **FR-003**: System MUST automatically select WebGPU renderer when available, falling back to WebGL when unavailable

- **FR-004**: System MUST log backend selection decision (WebGPU vs WebGL) during initialization

- **FR-005**: System MUST render chunk meshes with proper face culling and depth testing

- **FR-006**: System MUST handle both indexed and non-indexed geometry rendering correctly

- **FR-007**: System MUST display accurate performance metrics (FPS, triangle count, draw calls) in HUD

- **FR-008**: VoxelCraft example MUST NOT contain custom rendering code that duplicates library functionality

- **FR-009**: System MUST render geometry with consistent winding order across all faces

- **FR-010**: System MUST properly initialize WebGL context with depth testing, face culling, and blending settings

### Non-Functional Requirements

- **NFR-001**: Performance MUST meet constitutional 60 FPS target with 100k triangles on target hardware

- **NFR-002**: Rendering MUST be visually consistent between WebGPU and WebGL backends

- **NFR-003**: Error messages MUST clearly indicate which rendering issues occurred and in which backend

### Key Entities

- **ChunkMesh**: Represents optimized geometry for 16x16x256 block region with vertices, normals, colors, UVs, and indices

- **Renderer**: Graphics backend abstraction providing WebGPU or WebGL rendering with automatic selection

- **BufferGeometry**: Mesh data structure containing vertex attributes (position, normal, color, UV) and optional index buffer

- **RenderStats**: Performance metrics tracking draw calls, triangle count, and frame timing

---

## Root Cause Analysis (Based on Prior Session)

### Known Issues from Previous Session

1. **Performance Issue**: VoxelCraft rendering at 0 FPS despite fixes to add `drawArrays()` fallback
   - Previous fix added non-indexed rendering support to WebGLRenderer
   - Issue persists, suggesting deeper problem with geometry pipeline or rendering loop

2. **Visual Issue**: Terrain appears "upside down"
   - Could indicate Y-axis inversion, incorrect winding order, or matrix transformation issue
   - Need to verify coordinate system consistency (Three.js uses Y-up, right-handed)

3. **Backend Verification**: Unclear if WebGPU is actually being used
   - WebGPURendererFactory should select WebGPU when available
   - May be silently falling back to WebGL without proper logging

### Areas to Investigate

1. **Geometry Generation**: ChunkMeshGenerator creates indexed geometry with Float32Array indices converted to Uint16Array
   - Verify index buffer is properly created and bound
   - Check if index conversion is causing issues

2. **Coordinate Systems**: VoxelCraft uses chunk-local coordinates converted to world space
   - Verify Y-axis orientation matches library expectations
   - Check if winding order is correct for face culling

3. **Renderer Pipeline**: WebGLRenderer has separate paths for indexed vs non-indexed geometry
   - Verify correct path is taken based on geometry type
   - Check if shader program is properly bound before rendering

4. **Performance Bottlenecks**: 0 FPS suggests rendering loop may be blocked or not executing
   - Verify game loop is calling renderer.render() each frame
   - Check if any async operations are blocking rendering

---

## Success Criteria

### Performance Metrics
- VoxelCraft achieves ≥30 FPS minimum, 60 FPS target
- Terrain generation completes in <10 seconds for 1,024 chunks
- Console logs show correct backend selection (WebGPU preferred, WebGL fallback)

### Visual Quality
- Terrain renders right-side up with grass on top
- Depth perception is correct (distant chunks appear behind near chunks)
- Face culling works correctly (no back-faces visible)
- Lighting/shading appears consistent across all chunk meshes

### Code Quality
- VoxelCraft example contains no custom WebGL/WebGPU calls
- All rendering logic is handled by library Renderer interface
- No duplicate code between example and library

---

## Review & Acceptance Checklist

### Content Quality
- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked (none found)
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---

## Dependencies and Assumptions

### Dependencies
- WebGPURenderer and WebGLRenderer implementations (completed in 016-implement-production-ready)
- WebGPURendererFactory with automatic fallback (completed in 016-implement-production-ready)
- BufferGeometry with attribute and index support (already implemented)
- ChunkMeshGenerator with greedy meshing (already implemented in VoxelCraft)

### Assumptions
- Browser supports either WebGPU or WebGL 1.0+
- Target hardware can achieve 60 FPS with expected triangle count
- VoxelCraft example is being tested in a web browser (not JVM/Native)
- User has access to browser developer console for debugging output

---

## Out of Scope

- Adding new rendering features beyond fixing existing issues
- Optimizing terrain generation algorithm (focus is rendering only)
- Implementing frustum culling or LOD systems
- Adding new visual effects or post-processing
- Supporting additional platforms beyond web browser

---

## Related Documentation

- `CLAUDE.md`: Constitutional requirement for 60 FPS performance
- `specs/014-create-a-basic/spec.md`: Original VoxelCraft specification
- `specs/016-implement-production-ready/spec.md`: WebGPU/WebGL renderer implementation
- Previous session summary: Details of 0 FPS issue and fixes attempted
