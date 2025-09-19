# Tasks: KreeKt Advanced 3D Features

**Input**: Design documents from `/mnt/d/Projects/KMP/KreeKt/specs/002-generate-the-spec/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/, quickstart.md

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Success: Kotlin Multiplatform library with advanced 3D graphics features
   → Extract: Kotlin 1.9+, WebGPU/Vulkan, multiplatform structure
2. Load optional design documents:
   → data-model.md: Extracted entities for geometry, materials, animation, physics, XR
   → contracts/: 6 API contract files (geometry, material, lighting, animation, physics, xr)
   → research.md: Extracted technical decisions (Rapier physics, multi-tier performance)
   → quickstart.md: Extracted test scenarios for all major feature categories
3. Generate tasks by category:
   → Setup: KMP project extensions, dependencies, performance tier setup
   → Tests: contract tests for 6 APIs, integration tests for 9 feature categories
   → Core: models for advanced entities, services for complex systems
   → Integration: physics engines, XR platforms, shader management
   → Polish: unit tests, performance validation, documentation
4. Apply task rules:
   → Different module files = mark [P] for parallel
   → Same module files = sequential (no [P])
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001, T002...)
6. Generate dependency graph for advanced 3D features
7. Create parallel execution examples
8. Validate task completeness:
   → All 6 contracts have tests? ✓
   → All major entities have models? ✓
   → All feature integrations implemented? ✓
9. Return: SUCCESS (tasks ready for advanced 3D implementation)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different modules/files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
- **Single project KMP structure**: `src/commonMain/kotlin/`, `src/jvmMain/kotlin/`, `src/jsMain/kotlin/`, etc.
- **Tests**: `src/commonTest/kotlin/`, `src/jvmTest/kotlin/`, `src/jsTest/kotlin/`
- Paths follow Kotlin Multiplatform conventions with platform-specific implementations

## Phase 3.1: Setup and Infrastructure
- [x] T001 Configure advanced feature modules in build.gradle.kts (geometry, material, lighting, animation, physics, xr)
- [x] T002 Add physics engine dependencies (Rapier WASM bindings, Bullet JNI) with platform-specific configurations
- [x] T003 [P] Setup asset loading dependencies (DRACO compression, KTX2 textures, FreeType fonts)
- [x] T004 [P] Configure adaptive performance system with QualityTier enum in src/commonMain/kotlin/io/kreekt/performance/
- [x] T005 [P] Setup XR platform dependencies (WebXR types, ARKit/ARCore bindings)

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### Contract Tests (All APIs)
- [x] T006 [P] Contract test GeometryGenerator interface in src/commonTest/kotlin/io/kreekt/geometry/GeometryGeneratorTest.kt
- [x] T007 [P] Contract test MaterialFactory interface in src/commonTest/kotlin/io/kreekt/material/MaterialFactoryTest.kt
- [x] T008 [P] Contract test LightingSystem interface in src/commonTest/kotlin/io/kreekt/lighting/LightingSystemTest.kt
- [x] T009 [P] Contract test AnimationSystem interface in src/commonTest/kotlin/io/kreekt/animation/AnimationSystemTest.kt
- [x] T010 [P] Contract test PhysicsWorld interface in src/commonTest/kotlin/io/kreekt/physics/PhysicsWorldTest.kt
- [x] T011 [P] Contract test XRSystem interface in src/commonTest/kotlin/io/kreekt/xr/XRSystemTest.kt

### Integration Tests (Feature Categories)
- [x] T012 [P] Integration test procedural geometry generation in src/commonTest/kotlin/io/kreekt/integration/GeometryIntegrationTest.kt
- [x] T013 [P] Integration test PBR material rendering in src/commonTest/kotlin/io/kreekt/integration/MaterialIntegrationTest.kt
- [x] T014 [P] Integration test IBL lighting pipeline in src/commonTest/kotlin/io/kreekt/integration/LightingIntegrationTest.kt
- [x] T015 [P] Integration test skeletal animation with IK in src/commonTest/kotlin/io/kreekt/integration/AnimationIntegrationTest.kt
- [x] T016 [P] Integration test physics simulation in src/commonTest/kotlin/io/kreekt/integration/PhysicsIntegrationTest.kt
- [x] T017 [P] Integration test XR session management in src/commonTest/kotlin/io/kreekt/integration/XRIntegrationTest.kt
- [x] T018 [P] Integration test performance optimization systems in src/commonTest/kotlin/io/kreekt/integration/PerformanceIntegrationTest.kt
- [x] T019 [P] Integration test cross-feature interoperability in src/commonTest/kotlin/io/kreekt/integration/CrossFeatureIntegrationTest.kt

## Phase 3.3: Core Implementation (ONLY after tests are failing)

### Extended Geometry System
- [x] T020 [P] Enhanced BufferGeometry with morph targets and instancing in src/commonMain/kotlin/io/kreekt/geometry/BufferGeometry.kt
- [x] T021 [P] PrimitiveGeometry base class and parameterized shapes in src/commonMain/kotlin/io/kreekt/geometry/PrimitiveGeometry.kt
- [x] T022 [P] ExtrudeGeometry implementation with beveling in src/commonMain/kotlin/io/kreekt/geometry/ExtrudeGeometry.kt
- [x] T023 [P] TextGeometry with font loading support in src/commonMain/kotlin/io/kreekt/geometry/TextGeometry.kt
- [x] T024 [P] GeometryProcessor for optimization and LOD generation in src/commonMain/kotlin/io/kreekt/geometry/GeometryProcessor.kt
- [x] T025 [P] UVGenerator for procedural texture coordinate mapping in src/commonMain/kotlin/io/kreekt/geometry/UVGenerator.kt

### Advanced Material System
- [x] T026 [P] PBRMaterial with clearcoat, transmission, iridescence in src/commonMain/kotlin/io/kreekt/material/PBRMaterial.kt
- [x] T027 [P] ShaderMaterial with WGSL custom shader support in src/commonMain/kotlin/io/kreekt/material/ShaderMaterial.kt
- [x] T028 [P] TextureAtlas for optimized texture packing in src/commonMain/kotlin/io/kreekt/material/TextureAtlas.kt
- [x] T029 [P] MaterialProcessor for validation and optimization in src/commonMain/kotlin/io/kreekt/material/MaterialProcessor.kt
- [ ] T030 ShaderManager implementation (depends on renderer platform detection)

### Lighting and Shadows
- [x] T031 [P] AreaLight and VolumetricLight implementations in src/commonMain/kotlin/io/kreekt/lighting/AdvancedLights.kt
- [x] T032 [P] ShadowMapper with cascaded and omnidirectional shadows in src/commonMain/kotlin/io/kreekt/lighting/ShadowMapper.kt
- [x] T033 [P] IBLProcessor for HDR environment processing in src/commonMain/kotlin/io/kreekt/lighting/IBLProcessor.kt
- [x] T034 [P] LightProbe and probe baking system in src/commonMain/kotlin/io/kreekt/lighting/LightProbe.kt
- [ ] T035 LightingSystem coordinator (depends on renderer integration)

### Animation System
- [x] T036 [P] Enhanced Skeleton with IK chain support in src/commonMain/kotlin/io/kreekt/animation/Skeleton.kt
- [x] T037 [P] IKSolver with FABRIK and two-bone algorithms in src/commonMain/kotlin/io/kreekt/animation/IKSolver.kt
- [x] T038 [P] MorphTargetAnimator for facial animation in src/commonMain/kotlin/io/kreekt/animation/MorphTargetAnimator.kt
- [x] T039 [P] StateMachine for animation state management in src/commonMain/kotlin/io/kreekt/animation/StateMachine.kt
- [x] T040 [P] AnimationCompressor for track optimization in src/commonMain/kotlin/io/kreekt/animation/AnimationCompressor.kt
- [ ] T041 SkeletalAnimationSystem coordinator (depends on scene graph integration)

### Physics Integration
- [ ] T042 [P] RigidBody and CollisionObject implementations in src/commonMain/kotlin/io/kreekt/physics/RigidBody.kt
- [ ] T043 [P] CollisionShape hierarchy (Box, Sphere, ConvexHull, etc.) in src/commonMain/kotlin/io/kreekt/physics/CollisionShapes.kt
- [ ] T044 [P] PhysicsConstraint implementations (Hinge, Slider, etc.) in src/commonMain/kotlin/io/kreekt/physics/PhysicsConstraints.kt
- [ ] T045 [P] CharacterController for player movement in src/commonMain/kotlin/io/kreekt/physics/CharacterController.kt
- [ ] T046 PhysicsEngine abstraction layer in src/commonMain/kotlin/io/kreekt/physics/PhysicsEngine.kt

### XR System
- [ ] T047 [P] XRSession and XRFrame management in src/commonMain/kotlin/io/kreekt/xr/XRSession.kt
- [ ] T048 [P] XRController with haptic feedback in src/commonMain/kotlin/io/kreekt/xr/XRController.kt
- [ ] T049 [P] XRAnchor and spatial tracking in src/commonMain/kotlin/io/kreekt/xr/XRAnchor.kt
- [ ] T050 [P] Hand tracking and eye gaze interfaces in src/commonMain/kotlin/io/kreekt/xr/XRInput.kt
- [ ] T051 [P] AR-specific features (plane detection, hit testing) in src/commonMain/kotlin/io/kreekt/xr/ARSystem.kt
- [ ] T052 XRSystem platform coordinator in src/commonMain/kotlin/io/kreekt/xr/XRSystem.kt

## Phase 3.4: Platform Integration
- [ ] T053 Rapier physics engine integration for Web platform in src/jsMain/kotlin/io/kreekt/physics/RapierPhysics.kt
- [ ] T054 Bullet physics engine integration for JVM platform in src/jvmMain/kotlin/io/kreekt/physics/BulletPhysics.kt
- [ ] T055 WebXR implementation for Web platform in src/jsMain/kotlin/io/kreekt/xr/WebXRImpl.kt
- [ ] T056 ARKit integration for iOS platform in src/iosMain/kotlin/io/kreekt/xr/ARKitImpl.kt
- [ ] T057 ARCore integration for Android platform in src/androidMain/kotlin/io/kreekt/xr/ARCoreImpl.kt
- [ ] T058 Platform-specific shader compilation (WGSL to SPIR-V) in src/commonMain/kotlin/io/kreekt/material/ShaderCompiler.kt
- [ ] T059 Asset loading with platform-appropriate formats in src/commonMain/kotlin/io/kreekt/loader/AdvancedAssetLoader.kt
- [ ] T060 Performance tier detection and adaptation in src/commonMain/kotlin/io/kreekt/performance/AdaptiveRenderer.kt

## Phase 3.5: Optimization and Polish
- [ ] T061 [P] LOD system with automatic distance-based switching in src/commonMain/kotlin/io/kreekt/optimization/LODSystem.kt
- [ ] T062 [P] Geometry instancing for repeated objects in src/commonMain/kotlin/io/kreekt/optimization/InstanceManager.kt
- [ ] T063 [P] Frustum culling optimization in src/commonMain/kotlin/io/kreekt/optimization/CullingSystem.kt
- [ ] T064 [P] Object pooling for performance-critical objects in src/commonMain/kotlin/io/kreekt/optimization/ObjectPool.kt
- [ ] T065 [P] Memory usage monitoring and profiling in src/commonMain/kotlin/io/kreekt/profiling/MemoryProfiler.kt
- [ ] T066 [P] Performance metrics collection in src/commonMain/kotlin/io/kreekt/profiling/PerformanceMonitor.kt

### Unit Tests for Complex Systems
- [ ] T067 [P] Unit tests for IK solver algorithms in src/commonTest/kotlin/io/kreekt/animation/IKSolverUnitTest.kt
- [ ] T068 [P] Unit tests for PBR material validation in src/commonTest/kotlin/io/kreekt/material/PBRMaterialUnitTest.kt
- [ ] T069 [P] Unit tests for physics collision detection in src/commonTest/kotlin/io/kreekt/physics/CollisionUnitTest.kt
- [ ] T070 [P] Unit tests for shader compilation in src/commonTest/kotlin/io/kreekt/material/ShaderCompilerUnitTest.kt
- [ ] T071 [P] Unit tests for geometry optimization in src/commonTest/kotlin/io/kreekt/geometry/GeometryProcessorUnitTest.kt

### Performance and Visual Validation
- [ ] T072 Performance test: 60 FPS with 100k triangles on standard hardware in src/commonTest/kotlin/io/kreekt/performance/FrameRateTest.kt
- [ ] T073 Performance test: Memory usage under platform limits in src/commonTest/kotlin/io/kreekt/performance/MemoryLimitTest.kt
- [ ] T074 Visual regression test: PBR material rendering consistency in src/commonTest/kotlin/io/kreekt/visual/PBRRenderingTest.kt
- [ ] T075 Cross-platform test: Feature availability detection in src/commonTest/kotlin/io/kreekt/platform/FeatureAvailabilityTest.kt
- [ ] T076 Execute quickstart.md validation scenarios across all platforms
- [ ] T077 [P] Update API documentation with advanced features in docs/api/
- [ ] T078 [P] Create migration guide from Three.js to KreeKt advanced features in docs/migration/

## Dependencies
**Test Dependencies:**
- T006-T019 (all tests) must complete before T020-T052 (any implementation)

**Core Implementation Dependencies:**
- T020-T025 (geometry) before T026-T030 (materials - need geometry for material testing)
- T026-T030 (materials) before T031-T035 (lighting - need materials for light testing)
- T036-T041 (animation) can run parallel to geometry/materials
- T042-T046 (physics) can run parallel to other core systems
- T047-T052 (XR) can run parallel to other core systems

**Platform Integration Dependencies:**
- T053-T060 (platform integration) require core implementations T020-T052
- T058 (shader compiler) blocks material and lighting platform features
- T059 (asset loader) blocks geometry and material loading features

**Polish Dependencies:**
- T061-T066 (optimization) require core geometry and rendering systems
- T067-T071 (unit tests) require corresponding core implementations
- T072-T078 (validation) require complete implementation

## Parallel Execution Examples

### Phase 3.2 Tests (All Parallel)
```
# Launch T006-T011 contract tests together:
Task: "Contract test GeometryGenerator interface in src/commonTest/kotlin/io/kreekt/geometry/GeometryGeneratorTest.kt"
Task: "Contract test MaterialFactory interface in src/commonTest/kotlin/io/kreekt/material/MaterialFactoryTest.kt"
Task: "Contract test LightingSystem interface in src/commonTest/kotlin/io/kreekt/lighting/LightingSystemTest.kt"
Task: "Contract test AnimationSystem interface in src/commonTest/kotlin/io/kreekt/animation/AnimationSystemTest.kt"
Task: "Contract test PhysicsWorld interface in src/commonTest/kotlin/io/kreekt/physics/PhysicsWorldTest.kt"
Task: "Contract test XRSystem interface in src/commonTest/kotlin/io/kreekt/xr/XRSystemTest.kt"

# Launch T012-T019 integration tests together:
Task: "Integration test procedural geometry generation in src/commonTest/kotlin/io/kreekt/integration/GeometryIntegrationTest.kt"
Task: "Integration test PBR material rendering in src/commonTest/kotlin/io/kreekt/integration/MaterialIntegrationTest.kt"
Task: "Integration test IBL lighting pipeline in src/commonTest/kotlin/io/kreekt/integration/LightingIntegrationTest.kt"
```

### Phase 3.3 Core Geometry (Parallel within category)
```
# Launch T020-T025 geometry implementations together:
Task: "Enhanced BufferGeometry with morph targets and instancing in src/commonMain/kotlin/io/kreekt/geometry/BufferGeometry.kt"
Task: "PrimitiveGeometry base class and parameterized shapes in src/commonMain/kotlin/io/kreekt/geometry/PrimitiveGeometry.kt"
Task: "ExtrudeGeometry implementation with beveling in src/commonMain/kotlin/io/kreekt/geometry/ExtrudeGeometry.kt"
Task: "TextGeometry with font loading support in src/commonMain/kotlin/io/kreekt/geometry/TextGeometry.kt"
Task: "GeometryProcessor for optimization and LOD generation in src/commonMain/kotlin/io/kreekt/geometry/GeometryProcessor.kt"
Task: "UVGenerator for procedural texture coordinate mapping in src/commonMain/kotlin/io/kreekt/geometry/UVGenerator.kt"
```

### Phase 3.5 Unit Tests (All Parallel)
```
# Launch T067-T071 unit tests together:
Task: "Unit tests for IK solver algorithms in src/commonTest/kotlin/io/kreekt/animation/IKSolverUnitTest.kt"
Task: "Unit tests for PBR material validation in src/commonTest/kotlin/io/kreekt/material/PBRMaterialUnitTest.kt"
Task: "Unit tests for physics collision detection in src/commonTest/kotlin/io/kreekt/physics/CollisionUnitTest.kt"
Task: "Unit tests for shader compilation in src/commonTest/kotlin/io/kreekt/material/ShaderCompilerUnitTest.kt"
Task: "Unit tests for geometry optimization in src/commonTest/kotlin/io/kreekt/geometry/GeometryProcessorUnitTest.kt"
```

## Notes
- [P] tasks = different files/modules, no dependencies
- Verify all tests fail before implementing corresponding features
- Commit after each major milestone (end of each phase)
- Focus on multiplatform compatibility throughout implementation
- Test on multiple platforms during integration phase

## Task Generation Rules
*Applied during main() execution*

1. **From Contracts** (6 files):
   - Each contract API → contract test task [P]
   - Each major interface → implementation task

2. **From Data Model** (9 entity categories):
   - Each entity type → model creation task [P]
   - Complex relationships → coordinator service tasks

3. **From Quickstart** (8 feature demonstrations):
   - Each feature demo → integration test [P]
   - Performance scenarios → validation tasks

4. **Ordering**:
   - Setup → Tests → Core → Platform Integration → Polish
   - Within categories: independent files marked [P]

## Validation Checklist
*GATE: Checked by main() before returning*

- [x] All 6 contracts have corresponding tests (T006-T011)
- [x] All major entities have model tasks (T020-T052)
- [x] All tests come before implementation (T006-T019 before T020+)
- [x] Parallel tasks target different files/modules
- [x] Each task specifies exact file path with KMP structure
- [x] No task modifies same file as another [P] task
- [x] Advanced 3D features comprehensively covered
- [x] Platform-specific implementations properly sequenced
- [x] Performance and optimization properly addressed