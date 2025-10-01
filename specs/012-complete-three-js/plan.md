# Implementation Plan: Complete Three.js r180 Feature Parity

**Branch**: `012-complete-three-js` | **Date**: 2025-10-01 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/home/yousef/Projects/kmp/KreeKt/specs/012-complete-three-js/spec.md`

## Execution Flow (/plan command scope)

```
1. Load feature spec from Input path ✅
   → Feature spec loaded successfully
2. Fill Technical Context ✅
   → Project Type: Kotlin Multiplatform library
   → Structure Decision: Multiplatform source sets with platform-specific implementations
3. Fill Constitution Check section ✅
   → Based on KreeKt Constitution v1.0.0
4. Evaluate Constitution Check ✅
   → No violations detected
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md ✅
   → Research completed with Three.js API mapping
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, CLAUDE.md ✅
   → Design artifacts generated
7. Re-evaluate Constitution Check ✅
   → Post-design validation passed
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Task generation approach described ✅
9. STOP - Ready for /tasks command ✅
```

**IMPORTANT**: The /plan command STOPS at step 9. Phases 2-4 are executed by other commands:

- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary

This feature ensures complete Three.js r180 API compatibility in KreeKt through systematic feature gap analysis and implementation. The approach validates 100% feature coverage across 15 major subsystems (geometry, materials, animation, lighting, post-processing, XR, etc.), adapts browser-specific Three.js patterns to Kotlin Multiplatform abstractions, and maintains performance parity (60 FPS, <5MB library size) across JVM, JavaScript, Native, and mobile platforms.

## Technical Context

**Language/Version**: Kotlin 1.9+ with Multiplatform plugin
**Primary Dependencies**:
- kotlinx-coroutines-core:1.8.0 (async operations)
- kotlinx-serialization-json:1.6.0 (data serialization)
- kotlinx-datetime (time management)
- okio (cross-platform IO)
- Platform graphics: LWJGL 3.3.3 (JVM/Vulkan), @webgpu/types (JS/WebGPU), native Vulkan/Metal bindings

**Storage**: N/A (library, not application)
**Testing**: Kotlin Test (multiplatform), JUnit 5 (JVM), Mocha (JS), platform-native test runners
**Target Platform**: JVM (desktop), JavaScript (browser+Node.js), Linux x64, Windows x64, macOS (x64+ARM), iOS, Android
**Project Type**: Kotlin Multiplatform library with commonMain + platform-specific implementations
**Performance Goals**: 60 FPS with 100k+ triangles, <5MB base library size, memory budgets (256MB mobile, 1GB standard, 2GB high, 4GB+ ultra)
**Constraints**:
- 100% Three.js r180 API coverage
- Cross-platform API consistency
- WebGL/WebGPU/Vulkan/Metal abstraction
- No runtime type casts (compile-time type safety)
- >80% code coverage, >95% test success rate

**Scale/Scope**:
- 90 functional requirements across 15 subsystems
- ~150 Three.js classes to implement/validate
- ~500 Three.js methods to support
- 14 key entity types

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Test-Driven Development (NON-NEGOTIABLE) ✅

**Status**: COMPLIANT
- All 90 functional requirements are testable against Three.js r180 API
- TDD workflow mandated: contract tests → implementation → validation tests
- Red-Green-Refactor cycle enforced for all features
- No placeholders permitted in production code

**Plan Alignment**:
- Phase 1 generates contract tests before implementation
- Each Three.js feature mapped to verifiable test case
- Visual regression tests compare KreeKt output to Three.js reference

### II. Production-Ready Code Only ✅

**Status**: COMPLIANT
- Feature gap analysis identifies all missing implementations
- Each requirement must be fully implemented (no partial features)
- Code review gates verify production readiness
- Performance validation required before completion

**Plan Alignment**:
- Explicit audit phase to identify incomplete features
- Task dependencies ensure complete implementation paths
- Quality gates in tasks.md enforce production standards

### III. Cross-Platform Compatibility ✅

**Status**: COMPLIANT
- All 90 requirements must work across JVM, JS, Native platforms
- expect/actual pattern for platform-specific graphics APIs
- API consistency validation across platforms
- Platform-specific performance validation

**Plan Alignment**:
- Data model uses expect/actual for graphics primitives
- Contract tests validate API consistency
- Platform-specific test suites ensure behavior parity
- Quickstart examples demonstrate cross-platform usage

### IV. Performance Standards ✅

**Status**: COMPLIANT
- FR-088: 60 FPS target with hardware-appropriate geometry
- FR-089: <5MB base library size constraint
- Memory budgets documented and enforced
- Benchmarks required for performance-critical code

**Plan Alignment**:
- Performance validation tasks in implementation phase
- Memory profiling required for large data structures
- Frame rate monitoring in visual tests
- Size optimization tasks for distribution

### V. Type Safety and API Design ✅

**Status**: COMPLIANT
- Three.js API patterns maintained
- Kotlin idioms applied (sealed classes, data classes, inline classes)
- No runtime casts (compile-time validation)
- Type-safe builder DSLs for scene construction

**Plan Alignment**:
- Data model defines type hierarchies
- Contracts specify type-safe APIs
- Material/Geometry hierarchies use sealed classes
- Vector/Matrix operations use inline classes

**Constitutional Compliance**: APPROVED - All five core principles satisfied

## Project Structure

### Documentation (this feature)

```
specs/012-complete-three-js/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
│   ├── geometry-api.kt     # Geometry system contracts
│   ├── material-api.kt     # Material system contracts
│   ├── animation-api.kt    # Animation system contracts
│   ├── lighting-api.kt     # Lighting system contracts
│   ├── texture-api.kt      # Texture system contracts
│   ├── loader-api.kt       # Asset loading contracts
│   ├── postfx-api.kt       # Post-processing contracts
│   ├── xr-api.kt           # VR/AR system contracts
│   ├── controls-api.kt     # Camera controls contracts
│   └── physics-api.kt      # Physics integration contracts
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)

```
KreeKt/
├── src/
│   ├── commonMain/kotlin/io/kreekt/
│   │   ├── core/
│   │   │   ├── math/              # Vector, Matrix, Quaternion, Color
│   │   │   ├── scene/             # Object3D, Scene, Group
│   │   │   └── raycaster/         # Raycaster, Intersection
│   │   ├── geometry/              # All geometry types
│   │   ├── material/              # All material types
│   │   ├── texture/               # All texture types
│   │   ├── animation/             # Animation system
│   │   ├── lighting/              # All light types
│   │   ├── camera/                # All camera types
│   │   ├── renderer/              # Renderer abstraction
│   │   ├── loader/                # Asset loaders
│   │   ├── postprocessing/        # Post-processing effects
│   │   ├── xr/                    # VR/AR support
│   │   ├── controls/              # Camera controls
│   │   ├── helper/                # Debug helpers
│   │   ├── audio/                 # 3D audio system
│   │   └── physics/               # Physics integration
│   │
│   ├── jvmMain/kotlin/io/kreekt/
│   │   ├── renderer/              # Vulkan renderer via LWJGL
│   │   ├── loader/                # JVM file loading
│   │   └── platform/              # JVM-specific utilities
│   │
│   ├── jsMain/kotlin/io/kreekt/
│   │   ├── renderer/              # WebGPU/WebGL renderer
│   │   ├── loader/                # Browser fetch APIs
│   │   └── platform/              # JS-specific utilities
│   │
│   ├── nativeMain/kotlin/io/kreekt/
│   │   ├── renderer/              # Native Vulkan/Metal
│   │   ├── loader/                # Native file loading
│   │   └── platform/              # Native utilities
│   │
│   ├── linuxX64Main/kotlin/       # Linux-specific Vulkan
│   ├── mingwX64Main/kotlin/       # Windows-specific Vulkan
│   ├── macosArm64Main/kotlin/     # macOS Metal (ARM)
│   ├── macosX64Main/kotlin/       # macOS Metal (x64)
│   ├── iosArm64Main/kotlin/       # iOS Metal
│   └── androidNativeArm64Main/    # Android Vulkan
│
├── src/commonTest/kotlin/io/kreekt/
│   ├── geometry/                  # Geometry contract tests
│   ├── material/                  # Material contract tests
│   ├── animation/                 # Animation contract tests
│   ├── integration/               # End-to-end tests
│   └── visual/                    # Visual regression tests
│
├── build.gradle.kts               # Multiplatform build configuration
└── gradle.properties              # Platform targets configuration
```

**Structure Decision**: Kotlin Multiplatform library structure with commonMain for shared code and platform-specific source sets (jvmMain, jsMain, nativeMain, etc.) for graphics API implementations. The multiplatform architecture enables 100% API consistency across platforms while allowing optimized platform-specific rendering backends.

## Phase 0: Outline & Research

**Objective**: Map Three.js r180 feature set to KreeKt architecture and resolve technical implementation approaches.

### Research Tasks Completed

1. **Three.js r180 API Surface Analysis**:
   - Decision: Maintain 1:1 API compatibility where possible
   - Rationale: Eases migration, leverages Three.js documentation
   - Alternatives considered: Kotlin-first API (rejected due to migration friction)

2. **Graphics API Abstraction Strategy**:
   - Decision: Unified renderer interface with platform-specific backends
   - Rationale: Platform independence, consistent behavior
   - Alternatives: Direct API exposure (rejected, too platform-specific)

3. **expect/actual Pattern for Graphics Primitives**:
   - Decision: Abstract shader, buffer, texture at expect level
   - Rationale: Maintains type safety across platforms
   - Alternatives: Reflection-based approach (rejected, runtime overhead)

4. **Asset Loading Architecture**:
   - Decision: Okio for cross-platform IO, platform-specific decoders
   - Rationale: Consistent API, native performance
   - Alternatives: Pure Kotlin decoders (rejected, performance concerns)

5. **Animation System Design**:
   - Decision: Port Three.js AnimationMixer architecture
   - Rationale: Proven design, familiar to Three.js users
   - Alternatives: Custom timeline system (unnecessary complexity)

**Output**: research.md with Three.js compatibility mapping and architectural decisions

## Phase 1: Design & Contracts

*Prerequisites: research.md complete*

### Deliverables Generated

1. **data-model.md**:
   - 14 key entities (Scene, Object3D, Geometry, Material, Texture, Light, Camera, Animation, Loader, Raycaster, RenderTarget, Shader, Helper, Audio, XRSession)
   - Type hierarchies (sealed classes for Material, Geometry, Light types)
   - Platform abstraction patterns (expect/actual for renderer primitives)

2. **contracts/** directory:
   - 10 contract files defining API surface for each subsystem
   - Type-safe method signatures matching Three.js r180
   - Kotlin idioms (data classes, default parameters, DSL builders)

3. **Failing contract tests**:
   - One test per major API method
   - Tests verify API exists and returns expected types
   - Currently fail (no implementations yet - TDD Red phase)

4. **quickstart.md**:
   - "Hello Cube" example (minimal working scene)
   - Asset loading example (GLTF model)
   - Animation example (rotating object)
   - Post-processing example (bloom effect)

5. **CLAUDE.md update**:
   - Added Three.js r180 compatibility context
   - Updated architecture notes with multiplatform patterns
   - Added recent change: Complete feature parity specification

**Output**: data-model.md, 10 contract files in /contracts/, failing test suite, quickstart.md, updated CLAUDE.md

### Constitution Re-Check (Post-Design) ✅

- **TDD Compliance**: Contract tests generated before implementation ✅
- **Production Ready**: No placeholders in design ✅
- **Cross-Platform**: expect/actual patterns defined ✅
- **Performance**: Inline classes for math operations ✅
- **Type Safety**: No runtime casts in API design ✅

**Result**: APPROVED - Design meets all constitutional requirements

## Phase 2: Task Planning Approach

*This section describes what the /tasks command will do - DO NOT execute during /plan*

### Task Generation Strategy

The /tasks command will generate a dependency-ordered task list following TDD principles:

1. **Feature Gap Analysis Tasks** (P - parallel execution):
   - Audit each of the 15 Three.js subsystems against current KreeKt implementation
   - Generate gap report identifying missing classes, methods, and features
   - Prioritize by usage frequency in Three.js examples

2. **Contract Test Implementation** (P):
   - One task per contract file (10 tasks)
   - Tests must fail initially (Red phase)
   - Verify API signatures match Three.js r180

3. **Data Model Implementation** (Sequential):
   - Implement base types (Vector2/3/4, Matrix3/4, Color, Quaternion)
   - Implement Object3D hierarchy
   - Implement expect/actual graphics primitives

4. **Feature Implementation by Subsystem** (Batched):
   - Geometry system (15 tasks)
   - Material system (17 tasks)
   - Texture system (12 tasks)
   - Animation system (10 tasks)
   - Lighting system (8 tasks)
   - Loader system (6 tasks)
   - Post-processing (12 tasks)
   - XR system (6 tasks)
   - Helper/Debug tools (8 tasks)
   - Audio system (4 tasks)

5. **Platform-Specific Implementations**:
   - JVM Vulkan renderer
   - JS WebGPU renderer
   - Native Vulkan/Metal renderers
   - Mobile-optimized variants

6. **Integration and Visual Tests**:
   - End-to-end rendering tests
   - Visual regression tests vs Three.js
   - Performance benchmarks
   - Cross-platform consistency validation

7. **Documentation and Examples**:
   - API documentation (KDoc)
   - Migration guide from Three.js
   - Example gallery (ports of Three.js examples)
   - Performance tuning guide

### Ordering Strategy

- **TDD order**: Tests before implementation (Red → Green → Refactor)
- **Dependency order**: Math → Object3D → Geometry/Material → Renderer → Advanced Features
- **Parallel execution**: Mark [P] for independent subsystems
- **Risk prioritization**: Core rendering before advanced features

### Estimated Output

150-200 numbered, ordered tasks in tasks.md:
- ~20 analysis and audit tasks
- ~30 contract test tasks
- ~100 implementation tasks (grouped by subsystem)
- ~20 platform-specific tasks
- ~30 integration, testing, and documentation tasks

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation

*These phases are beyond the scope of the /plan command*

- **Phase 3**: Execute tasks.md in dependency order (TDD Red-Green-Refactor)
- **Phase 4**: Integration testing and visual regression validation
- **Phase 5**: Performance optimization and platform-specific tuning
- **Phase 6**: Documentation finalization and release preparation

---

## Progress Tracking

- [x] Phase 0: Research complete (Three.js API mapping)
- [x] Phase 1: Design complete (contracts, data model, quickstart)
- [x] Constitution Check: Initial validation passed
- [x] Constitution Check: Post-design validation passed
- [ ] Phase 2: Task generation (executed by /tasks command)
- [ ] Phase 3+: Implementation (future work)

---

## Notes

This plan systematically addresses all 90 functional requirements from the feature specification through architectural research, API contract definition, and TDD-driven implementation planning. The multiplatform architecture ensures Three.js compatibility across all target platforms while maintaining the performance and type safety standards defined in the KreeKt constitution.

The generated artifacts (research.md, contracts/, data-model.md, quickstart.md) provide a complete blueprint for implementing Three.js r180 parity. The /tasks command will translate this design into concrete, ordered implementation tasks following TDD principles.
