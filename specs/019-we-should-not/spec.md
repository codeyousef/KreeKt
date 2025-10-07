# Feature Specification: WebGPU/Vulkan Primary Renderer with Multiplatform Code Sharing

**Feature Branch**: `019-we-should-not`
**Created**: October 7, 2025
**Status**: Draft
**Input**: User description: "we should not be using webgl at all except as a fallback. the library should be using
webgpu/vulkan. make sure the voxelcraft example is using the library exclusively and not using any custom code that
should be handled by the library. also, make sure all the targets (jvm/js) are sharing the majority, preferably all the
code since this is a kotlin multiplatform library. make any updates to the library itself if needed"

## Execution Flow (main)

```
1. Parse user description from Input
   → User wants: WebGPU/Vulkan primary, WebGL fallback only, library-based rendering, maximum code sharing
2. Extract key concepts from description
   → Actors: Library developers, Example developers, End users
   → Actions: Render 3D graphics, share code across platforms, use library APIs
   → Data: 3D meshes, textures, shaders, scene graphs
   → Constraints: WebGPU/Vulkan primary, WebGL fallback only, multiplatform code reuse
3. For each unclear aspect:
   → [NEEDS CLARIFICATION: What percentage of code sharing is "majority"?]
   → [NEEDS CLARIFICATION: Which rendering features must work on all platforms?]
   → [NEEDS CLARIFICATION: What level of API abstraction for the library?]
4. Fill User Scenarios & Testing section
   → Primary user flow: Developer creates 3D app using library, deploys to JVM/JS
5. Generate Functional Requirements
   → Each requirement testable via example apps and platform tests
6. Identify Key Entities
   → Renderer, Scene, Mesh, Material, Camera (library abstractions)
7. Run Review Checklist
   → Has clarifications marked
   → No implementation tech details leaked through
8. Return: SUCCESS (spec ready for planning)
```

---

## ⚡ Quick Guidelines

- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers

---

## Clarifications

### Session 2025-10-07

- Q: What minimum percentage of application code (excluding platform-specific windowing/input) should be shareable in
  common code? → A: ≥90% - Maximum code reuse; platform code only for native windowing/input initialization
- Q: Should the system require strict 60 FPS minimum, target 60 FPS average, use tiered requirements (60 FPS target/30
  FPS minimum), or let developers set FPS targets? → A: Tiered requirement - 60 FPS target with 30 FPS minimum
  acceptable in complex scenes
- Q: How strict should visual consistency be between WebGPU, Vulkan, and WebGL backends? → A: Visually identical to
  human eye - allow minor floating-point differences and sub-pixel variance, but must appear identical in normal viewing
  conditions
- Q: What minimum scene complexity (triangle count) should the renderer handle at target performance? → A: No minimum
  guarantee - performance scales naturally with hardware; library should provide profiling tools and best practices for
  optimization
- Q: When renderer initialization fails completely, how should the system respond? → A: Fail-fast with detailed error
  message - crash immediately with diagnostic information about missing graphics support, driver issues, or
  configuration problems

---

## User Scenarios & Testing *(mandatory)*

### Primary User Story

A developer building a 3D application with KreeKt wants to write their application code once and have it work across
both browser (JavaScript) and desktop (JVM) targets. The application should automatically use the best available
graphics backend (WebGPU in browsers, Vulkan on desktop) without the developer needing platform-specific code. If a
modern backend is unavailable, the system should gracefully fall back to WebGL to maintain compatibility.

### Acceptance Scenarios

1. **Given** a 3D voxel game example application, **When** the developer writes the game logic in common Kotlin code, *
   *Then** the same code compiles and runs correctly on both browser and desktop targets without platform-specific
   modifications

2. **Given** a modern browser with WebGPU support, **When** the application launches in the browser, **Then** the
   application uses WebGPU for rendering (not WebGL)

3. **Given** a desktop computer with Vulkan support, **When** the application launches on the JVM, **Then** the
   application uses Vulkan for rendering

4. **Given** an older browser without WebGPU support, **When** the application launches, **Then** the system
   automatically falls back to WebGL while maintaining visual parity

5. **Given** the VoxelCraft example application, **When** examining the codebase, **Then** no custom rendering code
   exists outside the library - all rendering is handled through library APIs

6. **Given** common game logic (terrain generation, player movement, chunk management), **When** compiled for different
   targets, **Then** the same code executes correctly on all platforms without duplicated logic

7. **Given** a developer initializing a renderer, **When** they request a renderer instance, **Then** the library
   automatically selects the appropriate backend based on platform capabilities without requiring manual backend
   selection

### Edge Cases

- What happens when a platform supports neither WebGPU/Vulkan nor WebGL?
    - System must crash immediately with detailed error message explaining missing graphics support, required driver
      versions, and platform requirements
- How does system handle partial WebGPU support (browser with experimental features)?
    - [NEEDS CLARIFICATION: Should system use experimental WebGPU or fall back to stable WebGL?]
- What happens when renderer initialization fails mid-application?
    - System must crash immediately with diagnostic information about the failure cause (fail-fast approach)
- How does the example handle platform-specific input differences (keyboard vs touch)?
    - [NEEDS CLARIFICATION: Should library provide input abstraction or is this example-specific?]

## Requirements *(mandatory)*

### Functional Requirements

**Rendering Backend Selection**

- **FR-001**: System MUST use WebGPU as the primary rendering backend for browser (JavaScript) targets
- **FR-002**: System MUST use Vulkan as the primary rendering backend for desktop (JVM) targets
- **FR-003**: System MUST fall back to WebGL when WebGPU is unavailable in browser environments
- **FR-004**: System MUST detect available graphics backends at runtime without requiring developer configuration
- **FR-005**: System MUST NOT use WebGL unless WebGPU/Vulkan are unavailable or initialization fails

**Code Sharing and Multiplatform**

- **FR-006**: System MUST allow at least 90% of application code to be shared across JVM and JavaScript targets
- **FR-007**: Example applications MUST NOT contain custom rendering logic that should be provided by the library
- **FR-008**: Scene graph operations (adding meshes, updating transforms, camera control) MUST work identically across
  all platforms
- **FR-009**: Mesh generation and geometry operations MUST execute in common (shared) code without platform-specific
  implementations
- **FR-010**: Material and shader definitions MUST be portable across platforms through library abstractions

**Library Responsibilities**

- **FR-011**: Library MUST provide a unified renderer interface that works across all target platforms
- **FR-012**: Library MUST handle shader compilation and pipeline creation internally without exposing platform-specific
  APIs to applications
- **FR-013**: Library MUST manage graphics resource lifecycle (buffers, textures, pipelines) transparently across
  backends
- **FR-014**: Library MUST provide scene graph, camera, and mesh abstractions that work identically on all platforms

**VoxelCraft Example Requirements**

- **FR-015**: VoxelCraft example MUST use only library-provided APIs for all rendering operations
- **FR-016**: VoxelCraft example MUST share all game logic (terrain generation, player movement, chunk meshing) in
  common code
- **FR-017**: VoxelCraft example MUST only contain platform-specific code for native window/input handling where
  unavoidable
- **FR-018**: VoxelCraft example MUST demonstrate successful rendering with the primary backend (WebGPU/Vulkan) on
  capable systems

**Performance and Quality**

- **FR-019**: System MUST target 60 FPS consistently across all backends, with 30 FPS minimum acceptable in complex
  scenes
- **FR-020**: Visual output MUST be visually identical to the human eye across WebGPU, Vulkan, and WebGL backends (minor
  floating-point differences and sub-pixel variance acceptable)
- **FR-021**: System MUST provide profiling tools and performance best practices to help developers optimize for their
  target hardware (no specific polygon count guarantee)

**Error Handling and Diagnostics**

- **FR-022**: System MUST crash immediately with detailed error messages when graphics initialization fails (fail-fast
  approach with diagnostic information)
- **FR-023**: System MUST log which rendering backend was selected at initialization
- **FR-024**: System MUST detect and report backend capability mismatches before rendering attempts, crashing with
  actionable error messages if requirements are not met

### Key Entities *(feature involves graphics abstractions)*

- **Renderer**: The main graphics system interface that abstracts WebGPU/Vulkan/WebGL differences. Developers interact
  with this to render scenes. Provides uniform API regardless of underlying backend.

- **Scene**: Container for all renderable objects. Maintains hierarchy of meshes, lights, and cameras. Platform-agnostic
  representation of what should be rendered.

- **Mesh**: Represents a 3D object with geometry and material. Contains vertex data, indices, and material properties.
  Must work identically whether rendered via WebGPU or Vulkan.

- **Material**: Defines surface appearance properties. Abstracts shader compilation and uniform binding across backends.
  Developers define materials once for all platforms.

- **Camera**: Defines view perspective and projection. Controls what portion of the scene is visible. Must produce
  consistent results across all rendering backends.

- **Geometry**: Defines the shape of a mesh through vertices, normals, colors, and indices. Generated in common code and
  consumed by platform-specific renderers transparently.

---

## Review & Acceptance Checklist

*GATE: Automated checks run during main() execution*

### Content Quality

- [x] No implementation details (languages, frameworks, APIs) - specification avoids mentioning specific WebGPU/Vulkan
  APIs
- [x] Focused on user value and business needs - emphasizes developer experience and code reuse
- [x] Written for non-technical stakeholders - uses plain language for graphics concepts
- [x] All mandatory sections completed - User Scenarios, Requirements, and Entities provided

### Requirement Completeness

- [ ] No [NEEDS CLARIFICATION] markers remain - 2 non-critical clarifications remain (experimental WebGPU, input
  abstraction)
- [x] Requirements are testable and unambiguous - Core requirements now have concrete thresholds (90% code sharing,
  60/30 FPS, visual parity)
- [x] Success criteria are measurable - FPS targets, code sharing percentage, visual consistency defined
- [x] Scope is clearly bounded - Scoped to rendering backend and code sharing
- [x] Dependencies and assumptions identified - Assumes platform graphics support exists

### Clarifications Needed (Session 2025-10-07)

**Resolved** (5 priority questions answered):

1. ✅ **Code Sharing Target**: ≥90% of application code shared across platforms
2. ✅ **Performance Target**: 60 FPS target, 30 FPS minimum acceptable
3. ✅ **Visual Tolerance**: Visually identical to human eye (sub-pixel variance acceptable)
4. ✅ **Polygon Budget**: No minimum guarantee - hardware-dependent with profiling tools
5. ✅ **Fallback Strategy**: Fail-fast with detailed error messages

**Remaining** (2 non-critical questions):

1. **Experimental WebGPU**: Use experimental browser features or fall back to stable WebGL?
2. **Input Abstraction**: Should library provide cross-platform input handling or is this example-specific?

---

## Execution Status

*Updated by main() during processing*

- [x] User description parsed - Extracted WebGPU/Vulkan primary, code sharing, library-only APIs
- [x] Key concepts extracted - Identified rendering backends, multiplatform constraints, example refactoring
- [x] Ambiguities marked - 8 clarification points identified, 5 priority resolved
- [x] User scenarios defined - 7 acceptance scenarios, 4 edge cases documented
- [x] Requirements generated - 24 functional requirements across 5 categories
- [x] Entities identified - 6 key abstractions documented
- [x] Clarification phase completed - 5/5 priority questions answered (2 non-critical remain)
- [x] Review checklist passed - Specification ready for planning phase

---

## Next Steps

**For Clarification Phase (`/clarify`)**:

1. Determine acceptable code sharing percentage target
2. Define performance requirements (FPS, polygon count)
3. Establish visual parity tolerance between backends
4. Decide experimental feature handling strategy
5. Define input abstraction scope
6. Specify error handling and fallback behaviors

**For Planning Phase (`/plan`)**:
Once clarifications are resolved, planning will need to address:

- Library API redesign for unified renderer interface
- VoxelCraft refactoring to use library APIs exclusively
- Common code extraction for game logic
- Backend selection and fallback mechanism
- Shader abstraction and pipeline management

**Constitution Alignment**:
This feature directly supports constitutional requirements:

- **60 FPS Performance**: Backend optimization for performance targets
- **Cross-platform consistency**: Ensuring identical behavior across platforms
- **Type Safety**: Library abstractions maintain compile-time guarantees
- **5MB Size Limit**: Efficient backend selection without code duplication
