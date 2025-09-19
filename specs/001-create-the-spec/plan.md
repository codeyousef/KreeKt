# Implementation Plan: KreeKt WebGPU/Vulkan Multiplatform 3D Library

**Branch**: `001-create-the-spec` | **Date**: 2025-09-19 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-create-the-spec/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → Success: Spec loaded from /specs/001-create-the-spec/spec.md
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detected Project Type: Library (multiplatform graphics library)
   → Structure Decision: Option 1 (Single project structure) with KMP modifications
3. Fill the Constitution Check section based on the content of the constitution document.
   → Using template-based constitution (no specific constraints defined yet)
4. Evaluate Constitution Check section below
   → No violations identified - library-first approach
   → Update Progress Tracking: Initial Constitution Check PASS
5. Execute Phase 0 → research.md
   → Research needed for WebGPU fallback, physics engines, distribution
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, CLAUDE.md
7. Re-evaluate Constitution Check section
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
A type-safe Kotlin Multiplatform library providing Three.js-equivalent 3D graphics capabilities using WebGPU/Vulkan rendering backends across all major platforms (JVM, Web, Android, iOS, Native), enabling developers to write 3D applications once and deploy everywhere.

## Technical Context
**Language/Version**: Kotlin 1.9+ with Kotlin Multiplatform
**Primary Dependencies**:
  - Common: kotlinx-coroutines, kotlinx-serialization, kotlin-math
  - JVM: LWJGL 3.3.3 (Vulkan bindings)
  - JS: @webgpu/types NPM package
  - Native: Direct Vulkan bindings
  - iOS: MoltenVK (Vulkan-to-Metal)
**Storage**: N/A (Library project)
**Testing**: Kotlin Test multiplatform framework
**Target Platform**: JVM/Desktop, Web (WebGPU), Android (Vulkan), iOS (MoltenVK), Native (Vulkan)
**Project Type**: Library (Kotlin Multiplatform)
**Performance Goals**: 60 FPS with 100,000 triangles, <100ms initialization
**Constraints**: <5MB base library size, type-safe API with no runtime casts
**Scale/Scope**: 13 implementation phases over 52 weeks, Three.js API parity

### Unresolved Clarifications from Spec:
- **WebGPU Fallback Strategy**: [NEEDS CLARIFICATION: WebGL2, software renderer, or error?]
- **Physics Engine Choice**: [NEEDS CLARIFICATION: Bullet, Jolt, Rapier, or all?]
- **Documentation Platform**: [NEEDS CLARIFICATION: hosting platform and update frequency]
- **Asset Size Limits**: [NEEDS CLARIFICATION: maximum progressive loading size]
- **Debug Metrics**: [NEEDS CLARIFICATION: GPU usage, draw calls, memory tracking?]
- **Distribution Channels**: [NEEDS CLARIFICATION: Maven Central, NPM, or custom?]

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Since the constitution is currently a template, we'll apply general best practices:
- ✅ **Library-First Approach**: This is a standalone library project
- ✅ **Clear Purpose**: Multiplatform 3D graphics library with defined scope
- ✅ **Testing Strategy**: TDD approach with multiplatform test framework
- ✅ **Simplicity**: Phased implementation starting with core math/rendering

## Project Structure

### Documentation (this feature)
```
specs/001-create-the-spec/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (Kotlin Multiplatform structure)
```
kreekt/
├── core/               # Math, utilities (commonMain)
│   ├── commonMain/
│   ├── jvmMain/
│   ├── jsMain/
│   └── nativeMain/
├── renderer/          # WebGPU/Vulkan abstraction
│   ├── commonMain/
│   └── [platform-specific]
├── scene/             # Scene graph
├── geometry/          # Geometry classes
├── material/          # Materials and shaders
├── light/            # Lighting system
├── animation/        # Animation system
├── loader/           # Asset loaders
├── controls/         # Camera controls
├── physics/          # Physics integration
├── xr/              # VR/AR support
├── postprocess/     # Post-processing
└── tools/           # Editor and debug tools

tests/
├── commonTest/
├── jvmTest/
├── jsTest/
└── nativeTest/
```

**Structure Decision**: Kotlin Multiplatform module structure with platform-specific implementations

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - WebGPU fallback strategy for unsupported browsers
   - Physics engine selection criteria and integration approach
   - Documentation generation and hosting strategy
   - Asset loading size limits and streaming approach
   - Debug/profiling metrics to expose
   - Library distribution channels per platform

2. **Research tasks to execute**:
   - Research WebGPU browser support matrix and fallback options
   - Evaluate physics engines (Bullet vs Jolt vs Rapier) for KMP compatibility
   - Research KMP library distribution best practices
   - Investigate WebGPU/Vulkan shader cross-compilation strategies
   - Research progressive asset loading patterns for 3D models
   - Evaluate documentation tools for KMP projects

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]

**Output**: research.md with all NEEDS CLARIFICATION resolved

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Scene graph hierarchy (Object3D, Scene, Group)
   - Geometry system (BufferGeometry, attributes)
   - Material system (Material hierarchy, shader uniforms)
   - Light types and properties
   - Camera types and projections
   - Texture formats and types
   - Animation clips and tracks

2. **Generate API contracts** from functional requirements:
   - Scene creation and manipulation API
   - Geometry builder DSL
   - Material configuration API
   - Asset loader interfaces
   - Animation playback API
   - Post-processing pipeline API
   - XR session management API

3. **Generate platform-specific contracts**:
   - WebGPU initialization contract (JS)
   - Vulkan device creation contract (JVM/Native)
   - Surface creation per platform
   - Input event handling per platform

4. **Extract test scenarios** from user stories:
   - Basic scene rendering test
   - Three.js API compatibility test
   - Cross-platform rendering consistency test
   - VR mode activation test
   - Model loading with animations test

5. **Update CLAUDE.md** incrementally:
   - Add Kotlin Multiplatform setup
   - Add WebGPU/Vulkan context
   - Add Three.js API mapping notes

**Output**: data-model.md, /contracts/*, quickstart.md, CLAUDE.md

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Phase-based task generation following the 13 phases from spec
- Each phase generates 15-20 implementation tasks
- Priority on Phase 1 (Foundation) and Phase 2 (Scene Graph)
- Platform-specific tasks marked for parallel execution

**Task Categories**:
1. **Math Library Tasks** (Phase 1.1):
   - Vector/Matrix implementations [P]
   - Quaternion operations [P]
   - Intersection algorithms [P]

2. **Renderer Abstraction Tasks** (Phase 1.2):
   - Device initialization per platform
   - Command buffer abstraction
   - Shader module system

3. **Scene Graph Tasks** (Phase 2):
   - Object3D hierarchy
   - Transform propagation
   - Camera implementations

**Ordering Strategy**:
- Math library first (no dependencies)
- Renderer abstraction second (depends on math)
- Scene graph third (depends on renderer)
- Features in dependency order thereafter

**Estimated Output**: 40-50 numbered tasks for first 2 phases in tasks.md

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation (execute tasks.md following 13-phase roadmap)
**Phase 5**: Validation (cross-platform testing, performance benchmarks)

## Complexity Tracking
*No violations identified - library follows single-purpose principle*

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
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved (completed in research.md)
- [x] Complexity deviations documented (none required)

**Generated Artifacts**:
- ✅ research.md: All technical decisions documented
- ✅ data-model.md: Complete entity definitions and relationships
- ✅ contracts/: API interfaces for renderer, scene, and animation
- ✅ quickstart.md: Cross-platform usage examples and validation tests
- ✅ CLAUDE.md: Development context and guidelines

---
*Based on Constitution template - Project-specific constitution to be defined*