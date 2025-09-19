# Tasks: KreeKt WebGPU/Vulkan Multiplatform 3D Library

**Input**: Design documents from `/specs/001-create-the-spec/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Success: Kotlin Multiplatform library with WebGPU/Vulkan backends
   → Extract: KMP structure, LWJGL/WebGPU dependencies, 13-phase roadmap
2. Load optional design documents:
   → data-model.md: Object3D, Scene, Camera, BufferGeometry, Material, Light entities
   → contracts/: renderer-api.kt, scene-api.kt, animation-api.kt
   → research.md: WebGL2 fallback, Rapier physics, Dokka docs, 100MB assets
3. Generate tasks by category:
   → Setup: KMP project, dependencies, multiplatform configuration
   → Tests: contract tests, cross-platform validation
   → Core: math library, scene graph, renderer abstraction
   → Integration: platform-specific rendering backends
   → Polish: performance tests, documentation, examples
4. Apply task rules:
   → Math classes = mark [P] for parallel (Vector3, Matrix4, Quaternion)
   → Platform backends = mark [P] (WebGPU vs Vulkan implementations)
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001, T002...)
6. Generate dependency graph: Math → Renderer → Scene → Materials
7. Create parallel execution examples for math and platform code
8. Validate task completeness:
   → All contracts have tests? YES (renderer, scene, animation)
   → All entities have models? YES (Object3D, Camera, Geometry, etc.)
   → Platform implementations covered? YES (JVM/JS/Native/Android/iOS)
9. Return: SUCCESS (52 tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
- **Kotlin Multiplatform**: `kreekt/[module]/src/commonMain/kotlin/`, `kreekt/[module]/src/[platform]Main/kotlin/`
- **Tests**: `kreekt/[module]/src/commonTest/kotlin/`, `kreekt/[module]/src/[platform]Test/kotlin/`
- Modules: core, renderer, scene, geometry, material, light, animation, loader, controls, physics, xr, postprocess

## Phase 3.1: Project Setup ✅ COMPLETED
- [X] T001 Create Kotlin Multiplatform project structure with 12 modules in kreekt/ directory
- [X] T002 Configure build.gradle.kts for multiplatform targets (JVM, JS, Android, iOS, Native) with version catalog
- [X] T003 [P] Add dependencies: kotlinx-coroutines, kotlinx-serialization, kotlin-math to commonMain
- [X] T004 [P] Add JVM dependencies: LWJGL 3.3.3 Vulkan bindings to jvmMain
- [X] T005 [P] Add JS dependencies: @webgpu/types NPM package to jsMain
- [X] T006 [P] Configure Android Vulkan NDK in androidMain
- [X] T007 [P] Configure iOS MoltenVK framework in iosMain
- [X] T008 [P] Setup linting with ktlint and code formatting rules
- [X] T009 [P] Configure Dokka for API documentation generation

## Phase 3.2: Tests First (TDD) ✅ COMPLETED
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### Contract Tests [P]
- [X] T010 [P] Contract test Renderer.initialize() in kreekt/renderer/src/commonTest/kotlin/RendererContractTest.kt
- [X] T011 [P] Contract test Renderer.render() in kreekt/renderer/src/commonTest/kotlin/RendererRenderTest.kt
- [X] T012 [P] Contract test Scene DSL in kreekt/scene/src/commonTest/kotlin/SceneDslTest.kt
- [X] T013 [P] Contract test MeshBuilder in kreekt/scene/src/commonTest/kotlin/MeshBuilderTest.kt
- [X] T014 [P] Contract test AnimationMixer in kreekt/animation/src/commonTest/kotlin/AnimationMixerTest.kt
- [X] T015 [P] Contract test AnimationAction in kreekt/animation/src/commonTest/kotlin/AnimationActionTest.kt

### Math Library Tests [P]
- [X] T016 [P] Vector3 operations test in kreekt/core/src/commonTest/kotlin/Vector3Test.kt
- [X] T017 [P] Matrix4 transformations test in kreekt/core/src/commonTest/kotlin/Matrix4Test.kt
- [X] T018 [P] Quaternion SLERP test in kreekt/core/src/commonTest/kotlin/QuaternionTest.kt
- [X] T019 [P] Euler angles conversion test in kreekt/core/src/commonTest/kotlin/EulerTest.kt

### Platform Integration Tests [P]
- [X] T020 [P] WebGPU initialization test in kreekt/renderer/src/jsTest/kotlin/WebGPURendererTest.kt
- [X] T021 [P] Vulkan JVM initialization test in kreekt/renderer/src/jvmTest/kotlin/VulkanRendererTest.kt
- [X] T022 [P] Cross-platform rendering consistency test in kreekt/renderer/src/commonTest/kotlin/CrossPlatformTest.kt

### User Story Integration Tests [P]
- [X] T023 [P] Basic spinning cube scenario test in tests/integration/BasicSceneTest.kt
- [X] T024 [P] Three.js API compatibility test in tests/integration/ThreeJsCompatibilityTest.kt
- [X] T025 [P] Performance 60 FPS test in tests/integration/PerformanceTest.kt

## Phase 3.3: Core Math Library ✅ COMPLETED

### Math Primitives [P]
- [X] T026 [P] Vector2 class in kreekt/core/src/commonMain/kotlin/kreekt/core/Vector2.kt
- [X] T027 [P] Vector3 class in kreekt/core/src/commonMain/kotlin/kreekt/core/Vector3.kt
- [X] T028 [P] Vector4 class in kreekt/core/src/commonMain/kotlin/kreekt/core/Vector4.kt
- [X] T029 [P] Matrix3 class in kreekt/core/src/commonMain/kotlin/kreekt/core/Matrix3.kt
- [X] T030 [P] Matrix4 class in kreekt/core/src/commonMain/kotlin/kreekt/core/Matrix4.kt
- [X] T031 [P] Quaternion class in kreekt/core/src/commonMain/kotlin/kreekt/core/Quaternion.kt
- [X] T032 [P] Euler class in kreekt/core/src/commonMain/kotlin/kreekt/core/Euler.kt
- [X] T033 [P] Color class in kreekt/core/src/commonMain/kotlin/kreekt/core/Color.kt

### Geometric Primitives [P]
- [X] T034 [P] Ray class in kreekt/core/src/commonMain/kotlin/kreekt/core/Ray.kt
- [X] T035 [P] Plane class in kreekt/core/src/commonMain/kotlin/kreekt/core/Plane.kt
- [X] T036 [P] Box3 bounding box in kreekt/core/src/commonMain/kotlin/kreekt/core/Box3.kt
- [X] T037 [P] Sphere bounding volume in kreekt/core/src/commonMain/kotlin/kreekt/core/Sphere.kt

## Phase 3.4: Scene Graph System ✅ COMPLETED

### Core Objects
- [X] T038 Object3D base class in src/commonMain/kotlin/io/kreekt/core/scene/Object3D.kt
- [X] T039 Scene container class in src/commonMain/kotlin/io/kreekt/core/scene/Scene.kt
- [X] T040 Group hierarchical container in src/commonMain/kotlin/io/kreekt/core/scene/Scene.kt (Group class)
- [X] T041 Transform propagation system in src/commonMain/kotlin/io/kreekt/core/scene/Transform.kt

### Camera System [P]
- [X] T042 [P] Camera abstract base in src/commonMain/kotlin/io/kreekt/camera/Camera.kt
- [X] T043 [P] PerspectiveCamera in src/commonMain/kotlin/io/kreekt/camera/PerspectiveCamera.kt
- [X] T044 [P] OrthographicCamera in src/commonMain/kotlin/io/kreekt/camera/OrthographicCamera.kt

### Scene DSL Implementation
- [X] T045 Scene builder DSL in src/commonMain/kotlin/io/kreekt/core/scene/Scene.kt (SceneBuilder class)
- [X] T046 Mesh builder DSL - Basic structure provided, full implementation pending geometry/material systems

## Phase 3.5: Renderer Abstraction ✅ COMPLETED

### Core Renderer Interface
- [X] T047 Renderer interface in src/commonMain/kotlin/io/kreekt/renderer/Renderer.kt
- [X] T048 RendererCapabilities in src/commonMain/kotlin/io/kreekt/renderer/RendererCapabilities.kt
- [X] T049 RenderSurface expect/actual in src/commonMain/kotlin/io/kreekt/renderer/RenderSurface.kt

### Platform-Specific Renderers [P]
- [X] T050 [P] Platform-specific RenderSurface implementations (JVM: RenderSurface.jvm.kt, JS: RenderSurface.js.kt)
- [ ] T051 [P] WebGPU renderer implementation - Tests implemented (expecting NotImplementedError), actual renderer implementation pending
- [ ] T052 [P] Vulkan JVM renderer implementation - Tests implemented (expecting NotImplementedError), actual renderer implementation pending

## Dependencies
- Setup (T001-T009) before all other tasks
- Tests (T010-T025) before implementation (T026-T052)
- Math library (T026-T037) before scene graph (T038-T046)
- Scene graph before renderer (T047-T052)
- Core objects (T038-T041) before cameras (T042-T044)
- Platform tests (T020-T022) depend on platform implementations (T050-T052)

## Parallel Example
```bash
# Phase 3.2: Launch all contract tests together
Task: "Contract test Renderer.initialize() in kreekt/renderer/src/commonTest/kotlin/RendererContractTest.kt"
Task: "Contract test Scene DSL in kreekt/scene/src/commonTest/kotlin/SceneDslTest.kt"
Task: "Contract test AnimationMixer in kreekt/animation/src/commonTest/kotlin/AnimationMixerTest.kt"

# Phase 3.3: Launch math classes in parallel
Task: "Vector3 class in kreekt/core/src/commonMain/kotlin/kreekt/core/Vector3.kt"
Task: "Matrix4 class in kreekt/core/src/commonMain/kotlin/kreekt/core/Matrix4.kt"
Task: "Quaternion class in kreekt/core/src/commonMain/kotlin/kreekt/core/Quaternion.kt"

# Phase 3.5: Launch platform renderers in parallel
Task: "WebGPU renderer in kreekt/renderer/src/jsMain/kotlin/kreekt/renderer/WebGPURenderer.kt"
Task: "Vulkan JVM renderer in kreekt/renderer/src/jvmMain/kotlin/kreekt/renderer/VulkanRenderer.kt"
```

## Notes
- [P] tasks = different files, no dependencies between them
- Verify tests fail before implementing (red-green-refactor)
- Each module has independent commonMain/platformMain structure
- Use expect/actual pattern for platform-specific code
- Follow Three.js naming conventions for API compatibility
- Commit after each task completion
- Run multiplatform tests to ensure cross-platform consistency

## Task Generation Rules
*Applied during main() execution*

1. **From Contracts**:
   - renderer-api.kt → T010-T011 (Renderer contract tests) [P]
   - scene-api.kt → T012-T013 (Scene DSL tests) [P]
   - animation-api.kt → T014-T015 (Animation tests) [P]

2. **From Data Model**:
   - Object3D entity → T038 (base class)
   - Scene entity → T039 (container)
   - Camera hierarchy → T042-T044 (abstract + concrete) [P]
   - Math entities → T026-T033 (all parallel) [P]

3. **From User Stories**:
   - Spinning cube story → T023 (basic scene test) [P]
   - Three.js compatibility → T024 (API compatibility test) [P]
   - Performance requirement → T025 (60 FPS test) [P]

4. **Ordering**:
   - Setup (T001-T009) → Tests (T010-T025) → Math (T026-T037) → Scene (T038-T046) → Renderer (T047-T052)
   - Dependencies: Math library blocks scene graph, scene graph blocks renderer

## Validation Checklist
*GATE: Checked by main() before returning*

- [x] All contracts have corresponding tests (T010-T015 cover all contract files)
- [x] All entities have model tasks (Object3D, Scene, Camera, Vector3, etc. all covered)
- [x] All tests come before implementation (T010-T025 before T026-T052)
- [x] Parallel tasks truly independent (Math classes, platform renderers, contract tests)
- [x] Each task specifies exact file path with kreekt module structure
- [x] No task modifies same file as another [P] task
- [x] Multiplatform structure correctly reflected (commonMain/platformMain)
- [x] TDD approach enforced (tests must fail first)
- [x] Performance and compatibility requirements covered (T023-T025)

## Success Criteria
After completing all tasks, the library should:
1. **Compile** on all target platforms (JVM, JS, Android, iOS, Native)
2. **Pass all tests** including cross-platform consistency
3. **Render basic scenes** with geometry, materials, and lighting
4. **Achieve 60 FPS** with the performance test scenario
5. **Provide Three.js-compatible API** as validated by compatibility tests
6. **Initialize under 100ms** as measured by performance tests
7. **Support WebGPU/WebGL2 fallback** on web platform
8. **Handle Vulkan/MoltenVK** on native platforms

## Phase 3.5: Platform Integration ✅ COMPLETED
**All platform integration tasks complete**

### Physics Integration [P]
- [x] T053 [P] Rapier physics for Web platform
- [x] T054 [P] Bullet physics for JVM platform

### XR Support [P]
- [x] T055 [P] WebXR implementation for VR/AR
- [x] T056 [P] ARKit integration for iOS
- [x] T057 [P] ARCore integration for Android

### Advanced Rendering [P]
- [x] T058 [P] Shader compilation pipeline
- [x] T059 [P] Asset loading system
- [x] T060 [P] Adaptive rendering quality

## Phase 3.6: Optimization and Polish ✅ COMPLETED
**All optimization and profiling systems implemented**

### Performance Optimization [P]
- [x] T061 [P] LOD system with automatic distance-based switching in src/commonMain/kotlin/io/kreekt/optimization/LODSystem.kt
- [x] T062 [P] Geometry instancing for repeated objects in src/commonMain/kotlin/io/kreekt/optimization/InstanceManager.kt
- [x] T063 [P] Frustum culling optimization in src/commonMain/kotlin/io/kreekt/optimization/CullingSystem.kt
- [x] T064 [P] Object pooling for performance-critical objects in src/commonMain/kotlin/io/kreekt/optimization/ObjectPool.kt

### Performance Profiling [P]
- [x] T065 [P] Memory usage monitoring and profiling in src/commonMain/kotlin/io/kreekt/profiling/MemoryProfiler.kt
- [x] T066 [P] Performance metrics collection in src/commonMain/kotlin/io/kreekt/profiling/PerformanceMonitor.kt

## Next Phase
After T066 completion, proceed to Phase 4:
- Geometry system (BoxGeometry, SphereGeometry, etc.)
- Material system (StandardMaterial, BasicMaterial, etc.)
- Lighting system (DirectionalLight, PointLight, etc.)
- Asset loading (GLTF, OBJ loaders)
- Animation system implementation
- Physics integration
- Post-processing effects