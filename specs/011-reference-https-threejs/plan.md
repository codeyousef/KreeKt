# Implementation Plan: Complete Three.js Feature Parity

**Branch**: `011-reference-https-threejs` | **Date**: 2025-09-30 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/011-reference-https-threejs/spec.md`

## Execution Flow (/plan command scope)

```
1. Load feature spec from Input path ✓
   → Spec loaded: 25 feature categories, 150+ functional requirements
2. Fill Technical Context (scan for NEEDS CLARIFICATION) ✓
   → Detect Project Type: Kotlin Multiplatform library
   → Set Structure Decision: Multiplatform module structure
3. Fill the Constitution Check section ✓
4. Evaluate Constitution Check section ✓
   → No violations: All constitutional requirements met
   → Update Progress Tracking: Initial Constitution Check ✓
5. Execute Phase 0 → research.md ✓
   → No NEEDS CLARIFICATION remain
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, CLAUDE.md ✓
7. Re-evaluate Constitution Check section ✓
   → No new violations
   → Update Progress Tracking: Post-Design Constitution Check ✓
8. Plan Phase 2 → Describe task generation approach ✓
9. STOP - Ready for /tasks command ✓
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:

- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary

This plan implements complete Three.js r180 feature parity for KreeKt, a Kotlin Multiplatform 3D graphics library. The implementation adds 25 major feature categories currently missing from KreeKt but essential for Three.js developers migrating to native platforms. High-priority features include Raycasting (object picking/interaction), Fog System (atmospheric effects), RenderTarget (post-processing), Helper Objects (visual debugging), and Advanced Textures (cube maps, video, canvas). The technical approach leverages KreeKt's existing WebGPU/Vulkan renderer abstraction with platform-specific implementations using expect/actual patterns.

## Technical Context

**Language/Version**: Kotlin 1.9+ with Multiplatform plugin
**Primary Dependencies**:
  - LWJGL 3.3.3 (JVM - Vulkan bindings)
  - @webgpu/types 0.1.40 (JS - WebGPU)
  - kotlinx-coroutines-core 1.8.0
  - kotlinx-serialization-json 1.6.0
  - kotlin-math 0.5.0

**Storage**: N/A (graphics library, no persistence)
**Testing**: Kotlin Test multiplatform framework, platform-specific test runners
**Target Platform**: JVM, JS (WebGPU/WebGL2), Android, iOS, Linux x64, Windows x64, macOS
**Project Type**: Kotlin Multiplatform library
**Performance Goals**:
  - 60 FPS with 100k+ triangles
  - <200ms initialization time
  - <5MB base library size (modular architecture)
  - 1M+ points rendering at 60 FPS
  - Real-time audio spatialization with <10ms latency

**Constraints**:
  - Must maintain Three.js API compatibility
  - Platform-specific rendering backends (WebGPU, Vulkan, Metal via MoltenVK)
  - Type-safe API with no runtime casts
  - Memory budgets: Mobile 256MB, Standard 1GB, High 2GB, Ultra 4GB+
  - Cross-platform shader compilation (WGSL → SPIR-V)

**Scale/Scope**:
  - 25 new feature categories
  - 150+ functional requirements
  - 30+ new entity/class definitions
  - Estimated 15,000-20,000 lines of new code
  - Support for 7 target platforms

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Test-Driven Development ✅
- **Status**: PASS
- **Compliance**: All features will be implemented using TDD with tests written first
- **Verification**: Each functional requirement maps to contract tests before implementation
- **No placeholders**: Constitution mandates zero stubs, TODOs, "for now" phrases

### II. Production-Ready Code Only ✅
- **Status**: PASS
- **Compliance**: All 150+ requirements specify complete, production-ready implementations
- **Quality Gates**: Code reviews verify production readiness per constitutional standards

### III. Cross-Platform Compatibility ✅
- **Status**: PASS
- **Compliance**: All features use expect/actual pattern for 7 target platforms
- **Platform Support**: JVM, JS, Android, iOS, Linux x64, Windows x64, macOS
- **Verification**: Platform-specific tests for each feature per platform

### IV. Performance Standards ✅
- **Status**: PASS
- **Target**: 60 FPS with 100k+ triangles maintained
- **Memory**: Within constitutional budgets (Mobile 256MB, Standard 1GB, High 2GB, Ultra 4GB+)
- **Benchmarks**: Performance tests for raycasting, instancing, point cloud rendering
- **Specific Targets**:
  - Raycasting: 10,000 objects tested per frame at 60 FPS
  - Instancing: 50,000 instances single draw call at 60 FPS
  - Points: 1M+ points rendered at 60 FPS
  - Audio: <10ms spatialization latency

### V. Type Safety and API Design ✅
- **Status**: PASS
- **Type System**: Sealed classes for hierarchies, data classes for immutable structures
- **No Runtime Casts**: Compile-time type validation throughout
- **Three.js Compatibility**: Familiar API patterns while maintaining Kotlin idioms
- **Inline Classes**: Performance-critical math operations

**Constitution Check Result**: ✅ **PASS** - All constitutional requirements met

## Project Structure

### Documentation (this feature)

```
specs/011-reference-https-threejs/
├── spec.md              # Feature specification
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
│   ├── audio-api.kt
│   ├── helper-api.kt
│   ├── camera-api.kt
│   ├── fog-api.kt
│   ├── raycaster-api.kt
│   ├── curve-api.kt
│   ├── texture-api.kt
│   ├── instancing-api.kt
│   ├── points-api.kt
│   ├── morph-api.kt
│   ├── clipping-api.kt
│   ├── layers-api.kt
│   ├── rendertarget-api.kt
│   └── shape-api.kt
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)

```
src/commonMain/kotlin/io/kreekt/
├── audio/                          # NEW: Audio system
│   ├── AudioListener.kt
│   ├── PositionalAudio.kt
│   ├── Audio.kt
│   ├── AudioLoader.kt
│   └── AudioAnalyser.kt
├── helper/                         # NEW: Helper objects
│   ├── Helper.kt (base)
│   ├── AxesHelper.kt
│   ├── GridHelper.kt
│   ├── BoxHelper.kt
│   ├── ArrowHelper.kt
│   ├── LightHelpers.kt
│   ├── CameraHelper.kt
│   └── SkeletonHelper.kt
├── camera/                         # EXTEND: Advanced cameras
│   ├── Camera.kt (existing)
│   ├── PerspectiveCamera.kt (existing)
│   ├── OrthographicCamera.kt (existing)
│   ├── CubeCamera.kt              # NEW
│   ├── StereoCamera.kt            # NEW
│   └── ArrayCamera.kt             # NEW
├── fog/                            # NEW: Fog system
│   ├── Fog.kt
│   └── FogExp2.kt
├── raycaster/                      # NEW: Raycasting
│   ├── Raycaster.kt
│   ├── Intersection.kt
│   └── RaycastUtils.kt
├── curve/                          # NEW: Curve system
│   ├── Curve.kt (base)
│   ├── LineCurve.kt
│   ├── QuadraticBezierCurve.kt
│   ├── CubicBezierCurve.kt
│   ├── SplineCurve.kt
│   ├── CatmullRomCurve3.kt
│   ├── EllipseCurve.kt
│   ├── ArcCurve.kt
│   ├── CurvePath.kt
│   ├── Shape.kt
│   └── Path.kt
├── texture/                        # EXTEND: Advanced textures
│   ├── Texture.kt (existing)
│   ├── CubeTexture.kt             # NEW
│   ├── VideoTexture.kt            # NEW
│   ├── CanvasTexture.kt           # NEW
│   ├── DataTexture.kt             # NEW
│   ├── Data3DTexture.kt           # NEW
│   ├── CompressedTexture.kt       # NEW
│   ├── DepthTexture.kt            # NEW
│   └── PMREMGenerator.kt          # NEW
├── instancing/                     # NEW: Instanced rendering
│   ├── InstancedMesh.kt
│   └── InstancedBufferAttribute.kt
├── points/                         # NEW: Point cloud & sprites
│   ├── Points.kt
│   ├── PointsMaterial.kt
│   ├── Sprite.kt
│   └── SpriteMaterial.kt
├── morph/                          # NEW: Morph targets
│   ├── MorphTargets.kt
│   └── MorphInfluences.kt
├── clipping/                       # NEW: Clipping planes
│   └── ClippingPlane.kt
├── layers/                         # NEW: Rendering layers
│   └── Layers.kt
├── rendertarget/                   # NEW: Render targets
│   ├── WebGLRenderTarget.kt
│   ├── WebGLCubeRenderTarget.kt
│   └── WebGLMultipleRenderTargets.kt
├── shape/                          # NEW: Shape & path system
│   ├── Shape.kt
│   ├── Path.kt
│   ├── ShapeGeometry.kt
│   ├── ExtrudeGeometry.kt (enhance existing)
│   └── LatheGeometry.kt
├── line/                           # NEW: Enhanced line rendering
│   ├── Line2.kt
│   └── LineSegments2.kt
├── lod/                            # NEW: Level of detail
│   └── LOD.kt
└── constants/                      # NEW: Material & rendering constants
    ├── BlendingMode.kt
    ├── TextureConstants.kt
    ├── MaterialConstants.kt
    └── RenderConstants.kt

tests/
├── commonTest/kotlin/io/kreekt/
│   ├── audio/                     # Audio system tests
│   ├── helper/                    # Helper object tests
│   ├── camera/                    # Advanced camera tests
│   ├── fog/                       # Fog system tests
│   ├── raycaster/                 # Raycasting tests
│   ├── curve/                     # Curve system tests
│   ├── texture/                   # Advanced texture tests
│   ├── instancing/                # Instancing tests
│   ├── points/                    # Point cloud & sprite tests
│   ├── morph/                     # Morph target tests
│   ├── clipping/                  # Clipping plane tests
│   ├── layers/                    # Layer system tests
│   ├── rendertarget/              # Render target tests
│   ├── shape/                     # Shape & path tests
│   ├── line/                      # Enhanced line tests
│   ├── lod/                       # LOD tests
│   └── integration/               # Cross-feature integration tests
├── jvmTest/kotlin/io/kreekt/      # JVM platform-specific tests
├── jsTest/kotlin/io/kreekt/       # JS platform-specific tests
└── nativeTest/kotlin/io/kreekt/   # Native platform-specific tests
```

**Structure Decision**: This is a Kotlin Multiplatform library project with shared common code and platform-specific implementations. The structure follows KreeKt's existing modular architecture with new modules for each major feature category. Each module has corresponding test directories following the project's TDD approach. Platform-specific implementations use Kotlin's expect/actual mechanism for cross-platform compatibility.

## Phase 0: Outline & Research

### Unknowns Identified

All technical context is well-defined with no NEEDS CLARIFICATION markers. The following research areas support implementation decisions:

### Research Tasks

1. **Audio System Platform APIs**
   - Web Audio API for JS platform
   - OpenAL for JVM/Native platforms
   - Android AudioTrack for Android
   - AVAudioEngine for iOS
   - Research cross-platform audio abstraction patterns

2. **Raycasting Optimization Strategies**
   - Bounding volume hierarchies (BVH) for large scenes
   - Spatial partitioning (octree, k-d tree)
   - GPU-accelerated picking techniques
   - Instanced mesh raycasting optimizations

3. **Curve Tessellation Algorithms**
   - Adaptive subdivision techniques
   - Arc length parametrization
   - Efficient Bezier evaluation
   - Catmull-Rom spline interpolation

4. **Texture Compression Formats**
   - Platform-specific formats (DXT, ETC2, ASTC, BC7)
   - Compression quality vs size tradeoffs
   - Runtime decompression performance
   - Format selection strategies

5. **GPU Instancing Best Practices**
   - Instance buffer management
   - Dynamic instance updates
   - Frustum culling per instance
   - Platform-specific instancing APIs (WebGPU vs Vulkan)

6. **Morph Target Shader Optimization**
   - Shader code generation for variable target counts
   - GPU memory layout for morph attributes
   - Combined skinning + morph target shaders
   - Performance benchmarking

7. **Clipping Plane Implementation**
   - Clip distance shader outputs
   - Platform shader compatibility (WGSL, SPIR-V)
   - Multi-plane intersection handling
   - Performance impact on fragment shaders

8. **Render Target Management**
   - Framebuffer object lifecycle
   - Multiple render target (MRT) setup
   - Depth/stencil attachment handling
   - Memory pooling strategies

**Output**: research.md with consolidated findings (see Phase 0 execution below)

## Phase 1: Design & Contracts

*Prerequisites: research.md complete*

### 1. Data Model Generation

Extract entities from feature spec → `data-model.md`:

**Core Entities** (25 categories):
- Audio: AudioListener, PositionalAudio, Audio, AudioAnalyser
- Helper: Helper (base), 12 specific helper types
- Camera: CubeCamera, StereoCamera, ArrayCamera
- Fog: Fog, FogExp2
- Raycaster: Raycaster, Intersection
- Curve: Curve (base), 9 curve types, Shape, Path
- Texture: CubeTexture, VideoTexture, CanvasTexture, DataTexture, Data3DTexture, CompressedTexture, DepthTexture, PMREMGenerator
- Instancing: InstancedMesh, InstancedBufferAttribute
- Points: Points, PointsMaterial, Sprite, SpriteMaterial
- Morph: MorphTargets, MorphInfluences
- Clipping: ClippingPlane
- Layers: Layers
- RenderTarget: WebGLRenderTarget, WebGLCubeRenderTarget, WebGLMultipleRenderTargets
- Shape: Shape, Path, ShapeGeometry, LatheGeometry
- Line: Line2, LineSegments2
- LOD: LOD
- Constants: BlendingMode, TextureConstants, MaterialConstants, RenderConstants

### 2. API Contracts Generation

Generate contracts from 150+ functional requirements:

**Contract Files** (in `/contracts/`):
- `audio-api.kt` - FR-A001 through FR-A010
- `helper-api.kt` - FR-H001 through FR-H015
- `camera-api.kt` - FR-C001 through FR-C006
- `fog-api.kt` - FR-F001 through FR-F006
- `raycaster-api.kt` - FR-R001 through FR-R010
- `curve-api.kt` - FR-CR001 through FR-CR015
- `texture-api.kt` - FR-T001 through FR-T020
- `instancing-api.kt` - FR-I001 through FR-I010
- `points-api.kt` - FR-P001 through FR-P010
- `morph-api.kt` - FR-M001 through FR-M008
- `clipping-api.kt` - FR-CP001 through FR-CP006
- `layers-api.kt` - FR-L001 through FR-L006
- `rendertarget-api.kt` - FR-RT001 through FR-RT010
- `shape-api.kt` - FR-S001 through FR-S010

### 3. Contract Test Generation

One test file per contract:
- Tests assert API signatures match contracts
- Tests verify functional requirements
- Tests must fail initially (no implementation)
- Tests follow TDD red-green-refactor cycle

### 4. Test Scenarios from User Stories

Extract from spec's 15 acceptance scenario categories:
- Audio System scenarios (2)
- Helper Objects scenarios (3)
- Advanced Cameras scenarios (3)
- Fog System scenarios (2)
- Raycasting scenarios (3)
- Curve System scenarios (3)
- Advanced Textures scenarios (4)
- Instancing scenarios (2)
- Points & Sprites scenarios (2)
- Morph Targets scenarios (2)
- Clipping Planes scenarios (2)
- Layers scenarios (2)
- RenderTarget scenarios (2)
- Curves & Paths scenarios (2)
- Shape & Path System scenarios (2)

### 5. Agent Context Update

Run: `.specify/scripts/bash/update-agent-context.sh claude`
- Add new feature categories to CLAUDE.md
- Document new modules and their purposes
- Update implementation status
- Keep file under 150 lines

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, CLAUDE.md

## Phase 2: Task Planning Approach

*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:

The /tasks command will:
1. Load `.specify/templates/tasks-template.md` as base
2. Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
3. Follow TDD ordering: contracts → tests → implementation
4. Organize by feature priority (High, Medium, Lower from spec notes)

**Task Categories**:

1. **Contract Test Tasks** (25 tasks, one per feature category) [P]
   - Write contract tests for Audio API
   - Write contract tests for Helper API
   - Write contract tests for Camera API
   - ... (one per contract file)

2. **Model Creation Tasks** (25 tasks, one per feature category) [P]
   - Implement Audio system models (AudioListener, PositionalAudio, etc.)
   - Implement Helper system models (AxesHelper, GridHelper, etc.)
   - ... (one per feature category)

3. **Core Implementation Tasks** (grouped by priority):

   **High Priority** (30-35 tasks):
   - Raycaster core implementation
   - BVH/spatial optimization for raycasting
   - Fog shader integration
   - RenderTarget framebuffer management
   - Helper rendering pipeline
   - CubeTexture loading and sampling
   - VideoTexture streaming
   - CanvasTexture dynamic updates

   **Medium Priority** (35-40 tasks):
   - Curve parametric evaluation
   - Curve tessellation
   - TubeGeometry along curves
   - Points rendering pipeline
   - Sprite billboard rendering
   - InstancedMesh GPU instancing
   - Instance buffer management
   - Morph target shader generation
   - LOD distance-based switching

   **Lower Priority** (25-30 tasks):
   - Audio spatialization
   - Audio Doppler effects
   - CubeCamera 6-face rendering
   - StereoCamera VR rendering
   - Clipping plane shader integration
   - Shape triangulation
   - ExtrudeGeometry enhancements
   - Line2 thick rendering

4. **Integration Test Tasks** (15-20 tasks):
   - Audio + Scene integration test
   - Raycaster + Instancing test
   - Fog + Transparency test
   - RenderTarget + Post-processing test
   - Curve + Animation path test
   - Points + Raycasting test
   - Morph + Skinning test
   - Layers + Multi-pass rendering test

5. **Platform-Specific Tasks** (20-25 tasks):
   - Audio platform implementations (Web Audio API, OpenAL)
   - Texture compression format handling
   - Shader compilation (WGSL, SPIR-V)
   - RenderTarget platform framebuffers

6. **Performance Optimization Tasks** (10-15 tasks):
   - Raycasting BVH optimization
   - Instancing frustum culling
   - Point cloud LOD system
   - Shader caching

7. **Documentation Tasks** (5-10 tasks):
   - API documentation
   - Migration guide from Three.js
   - Example applications
   - Performance best practices

**Ordering Strategy**:

- **TDD order**: Contract tests → Unit tests → Implementation
- **Dependency order**:
  - Math/Core types first (already exist in KreeKt)
  - Data models before services
  - Services before renderers
  - Renderers before integration
- **Mark [P] for parallel execution**: Contract tests, model creation tasks
- **Sequential for integration**: Cross-feature tests after core implementations

**Estimated Output**: 150-180 numbered, ordered tasks in tasks.md

**Task Duration Estimates**:
- Contract test: 0.5-1 hour
- Model creation: 1-2 hours
- Core implementation: 2-4 hours
- Integration test: 1-2 hours
- Platform-specific: 2-3 hours
- Optimization: 2-4 hours

**Total Estimated Effort**: 300-400 hours (8-10 weeks with 1 developer)

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation

*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation (execute tasks.md following constitutional principles)
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

### Phase 4 Implementation Notes

Implementation will follow constitutional TDD cycle:
1. Write failing contract tests
2. Implement minimal code to pass tests
3. Refactor while maintaining green tests
4. Verify cross-platform compilation
5. Run performance benchmarks

### Phase 5 Validation Criteria

Success measured by:
- **Test Coverage**: 90%+ line coverage, 85%+ branch coverage
- **Performance**: 60 FPS with 100k triangles maintained
- **API Coverage**: 95%+ of Three.js r180 APIs available
- **Platform Support**: All 7 platforms compile and pass tests
- **Migration**: Three.js examples run with minimal changes

## Complexity Tracking

*Fill ONLY if Constitution Check has violations that must be justified*

No constitutional violations identified. All requirements align with constitutional principles:
- TDD enforced
- Production-ready code
- Cross-platform compatibility
- Performance standards met
- Type safety maintained

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
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented (none)

---
*Based on Constitution v1.0.0 - See `.specify/memory/constitution.md`*