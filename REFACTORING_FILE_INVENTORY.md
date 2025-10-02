# KreeKt Refactoring File Inventory
**Complete list of files requiring refactoring**

---

## Files Over 500 Lines (47 total)

### CRITICAL PRIORITY (1,000+ lines) - 17 files

| # | File | Lines | Target | Reduction | Status |
|---|------|-------|--------|-----------|--------|
| 1 | `src/jsMain/kotlin/io/kreekt/physics/RapierPhysics.kt` | 1,871 → **22** | 350 | 81% | ✅ **COMPLETED** (9 modules extracted, 2,134 lines) |
| 2 | `tools/editor/desktop/src/SceneEditorCompose.kt` | 1,377 | 450 | 67% | 🔴 Not Started |
| 3 | `tools/integration/ToolIntegrationTester.kt` | 1,247 | 400 | 68% | 🔴 Not Started |
| 4 | `tools/docs/src/main/kotlin/migration/MigrationGuide.kt` | 1,237 | 350 | 72% | 🔴 Not Started |
| 5 | `src/jvmMain/kotlin/io/kreekt/physics/BulletPhysics.kt` | 1,206 → **18** | 350 | 99% | ✅ **COMPLETED** (7 modules extracted, 1,286 lines) |
| 6 | `src/commonMain/kotlin/io/kreekt/geometry/GeometryProcessor.kt` | 1,183 | 400 | 66% | 🔴 Not Started |
| 7 | `tools/profiler/src/commonMain/kotlin/gpu/GPUProfiler.kt` | 1,156 | 400 | 65% | 🔴 Not Started |
| 8 | `tests/integration/test_cicd_pipeline.kt` | 1,152 | 400 | 65% | 🔴 Not Started |
| 9 | `src/commonMain/kotlin/io/kreekt/lighting/LightProbe.kt` | 1,150 | 400 | 65% | 🔴 Not Started |
| 10 | `tools/editor/src/commonMain/kotlin/material/MaterialLibrary.kt` | 1,142 | 350 | 69% | 🔴 Not Started |
| 11 | `src/commonMain/kotlin/io/kreekt/renderer/BufferManager.kt` | 1,116 | 350 | 69% | 🔴 Not Started |
| 12 | `tools/editor/src/commonMain/kotlin/material/ShaderEditor.kt` | 1,073 | 350 | 67% | 🔴 Not Started |
| 13 | `src/commonMain/kotlin/io/kreekt/lighting/ShadowMapper.kt` | 1,043 | 400 | 62% | 🔴 Not Started |
| 14 | `tools/profiler/src/commonMain/kotlin/analysis/FrameAnalyzer.kt` | 1,037 | 350 | 66% | 🔴 Not Started |
| 15 | `tools/tests/src/commonMain/kotlin/performance/PerformanceBenchmark.kt` | 1,029 | 350 | 66% | 🔴 Not Started |
| 16 | `tools/tests/src/commonMain/kotlin/execution/TestEngine.kt` | 1,012 | 350 | 65% | 🔴 Not Started |
| 17 | `tools/editor/src/commonMain/kotlin/material/MaterialPreview.kt` | 982 | 350 | 64% | 🔴 Not Started |

**Subtotal**: 18,295 lines → 6,250 target (12,045 lines to extract)

---

### HIGH PRIORITY (700-1,000 lines) - 13 files

| # | File | Lines | Target | Reduction | Status |
|---|------|-------|--------|-----------|--------|
| 18 | `tools/editor/src/commonMain/kotlin/manipulation/ObjectManipulator.kt` | 982 | 350 | 64% | 🔴 Not Started |
| 19 | `tools/tests/src/commonMain/kotlin/visual/VisualComparator.kt` | 980 | 350 | 64% | 🔴 Not Started |
| 20 | `src/commonMain/kotlin/io/kreekt/physics/PhysicsConstraints.kt` | 973 | 350 | 64% | 🔴 Not Started |
| 21 | `tools/docs/src/main/kotlin/server/DocServer.kt` | 969 | 350 | 64% | 🔴 Not Started |
| 22 | `tools/docs/src/main/kotlin/examples/ExampleGenerator.kt` | 969 | 350 | 64% | 🔴 Not Started |
| 23 | `tools/docs/src/main/kotlin/dokka/DokkaEnhancer.kt` | 950 | 350 | 63% | 🔴 Not Started |
| 24 | `src/commonMain/kotlin/io/kreekt/geometry/PrimitiveGeometry.kt` | 945 | 400 | 58% | 🔴 Not Started |
| 25 | `src/commonMain/kotlin/io/kreekt/performance/AdaptiveRenderer.kt` | 929 | 400 | 57% | 🔴 Not Started |
| 26 | `specs/012-complete-three-js/contracts/physics-api.kt` | 898 | 400 | 55% | 🔴 Not Started |
| 27 | `tools/profiler/src/commonMain/kotlin/metrics/MetricsCollector.kt` | 888 | 350 | 61% | 🔴 Not Started |
| 28 | `tools/docs/src/main/kotlin/search/SearchIndexer.kt` | 887 | 350 | 61% | 🔴 Not Started |
| 29 | `tests/integration/test_docs_generation.kt` | 885 | 350 | 60% | 🔴 Not Started |
| 30 | `tools/profiler/src/commonMain/kotlin/ui/PerformanceUI.kt` | 877 | 350 | 60% | 🔴 Not Started |

**Subtotal**: 11,132 lines → 4,700 target (6,432 lines to extract)

---

### MEDIUM PRIORITY (500-700 lines) - 17 files

| # | File | Lines | Target | Reduction | Status |
|---|------|-------|--------|-----------|--------|
| 31 | `src/commonMain/kotlin/io/kreekt/geometry/UVGenerator.kt` | 876 | 400 | 54% | 🔴 Not Started |
| 32 | `src/commonMain/kotlin/io/kreekt/material/TextureAtlas.kt` | 873 | 350 | 60% | 🔴 Not Started |
| 33 | `specs/013-double-check-and/contracts/postprocessing-contracts.kt` | 858 | 400 | 53% | 🔴 Not Started |
| 34 | `src/commonMain/kotlin/io/kreekt/lighting/IBLProcessor.kt` | 851 | 350 | 59% | 🔴 Not Started |
| 35 | `tools/tests/src/commonMain/kotlin/runner/CrossPlatformRunner.kt` | 849 | 350 | 59% | 🔴 Not Started |
| 36 | `tools/editor/src/commonMain/kotlin/material/UniformControls.kt` | 846 | 350 | 59% | 🔴 Not Started |
| 37 | `tools/scanning/T032_AutomatedPlaceholderFix.kt` | 830 | 400 | 52% | 🔴 Not Started |
| 38 | `tools/validation/api-doc-reviewer.kt` | 825 | 400 | 52% | 🔴 Not Started |
| 39 | `tools/editor/src/commonMain/kotlin/serialization/ProjectSerializer.kt` | 815 | 350 | 57% | 🔴 Not Started |
| 40 | `src/commonMain/kotlin/io/kreekt/animation/AnimationCompressor.kt` | 804 | 350 | 56% | 🔴 Not Started |
| 41 | `src/commonMain/kotlin/io/kreekt/validation/checker/DefaultProductionReadinessChecker.kt` | 788 | 400 | 49% | 🔴 Not Started |
| 42 | `tools/cicd/performance/RegressionDetector.kt` | 781 | 350 | 55% | 🔴 Not Started |
| 43 | `src/commonMain/kotlin/io/kreekt/core/math/Matrix4.kt` | 768 | 400 | 48% | 🔴 Not Started |
| 44 | `tests/integration/test_testing_pipeline.kt` | 767 | 400 | 48% | 🔴 Not Started |
| 45 | `src/commonMain/kotlin/io/kreekt/physics/CharacterController.kt` | 766 | 400 | 48% | 🔴 Not Started |
| 46 | `src/commonMain/kotlin/io/kreekt/geometry/TextGeometry.kt` | 756 | 400 | 47% | 🔴 Not Started |
| 47 | `src/jsMain/kotlin/io/kreekt/renderer/webgl/MatrixUtils.kt` | 755 | 350 | 54% | 🔴 Not Started |

**Subtotal**: 13,212 lines → 6,400 target (6,812 lines to extract)

---

## Summary Statistics

### Current State
- **Total files requiring refactoring**: 47
- **Total current lines**: 42,639 lines
- **Total target lines**: 17,350 lines
- **Lines to extract/eliminate**: 25,289 lines (59% reduction)

### By Priority
| Priority | Files | Current Lines | Target Lines | Reduction |
|----------|-------|---------------|--------------|-----------|
| Critical | 17 | 18,295 | 6,250 | 66% |
| High | 13 | 11,132 | 4,700 | 58% |
| Medium | 17 | 13,212 | 6,400 | 52% |
| **Total** | **47** | **42,639** | **17,350** | **59%** |

### Progress Tracking
- ✅ Completed: 2 files (4%)
- 🟡 In Progress: 0 files (0%)
- 🔴 Not Started: 45 files (96%)

---

## Modules Created (Extracted Code)

### ✅ Rapier Physics Modules (from RapierPhysics.kt - COMPLETED)
1. ✅ `src/jsMain/kotlin/io/kreekt/physics/rapier/RAPIER.kt` (260 lines) - External WASM declarations
2. ✅ `src/jsMain/kotlin/io/kreekt/physics/rapier/RapierPhysicsEngine.kt` (233 lines) - Main factory
3. ✅ `src/jsMain/kotlin/io/kreekt/physics/rapier/body/RapierRigidBody.kt` (306 lines) - Rigid body implementation
4. ✅ `src/jsMain/kotlin/io/kreekt/physics/rapier/world/RapierPhysicsWorld.kt` (453 lines) - Physics world
5. ✅ `src/jsMain/kotlin/io/kreekt/physics/rapier/constraints/RapierConstraints.kt` (50 lines) - Constraints
6. ✅ `src/jsMain/kotlin/io/kreekt/physics/rapier/shapes/RapierShapes.kt` (246 lines) - Basic shapes
7. ✅ `src/jsMain/kotlin/io/kreekt/physics/rapier/shapes/RapierComplexShapes.kt` (462 lines) - Complex shapes
8. ✅ `src/jsMain/kotlin/io/kreekt/physics/rapier/utils/RapierTypeConversions.kt` (81 lines) - Type conversions
9. ✅ `src/jsMain/kotlin/io/kreekt/physics/rapier/utils/RapierColliderDescFactory.kt` (43 lines) - Collider factory

**Total extracted**: 2,134 lines across 9 focused modules
**Original file reduced**: 1,871 → 22 lines (99% reduction!)

### ✅ Bullet Physics Modules (from BulletPhysics.kt - COMPLETED)
1. ✅ `src/jvmMain/kotlin/io/kreekt/physics/bullet/shapes/BulletShapes.kt` (253 lines) - Basic shapes
2. ✅ `src/jvmMain/kotlin/io/kreekt/physics/bullet/shapes/BulletComplexShapes.kt` (393 lines) - Complex shapes
3. ✅ `src/jvmMain/kotlin/io/kreekt/physics/bullet/body/BulletRigidBody.kt` (144 lines) - Rigid body
4. ✅ `src/jvmMain/kotlin/io/kreekt/physics/bullet/world/BulletPhysicsWorld.kt` (160 lines) - Physics world
5. ✅ `src/jvmMain/kotlin/io/kreekt/physics/bullet/constraints/BulletConstraints.kt` (121 lines) - Constraints
6. ✅ `src/jvmMain/kotlin/io/kreekt/physics/bullet/character/BulletCharacterController.kt` (96 lines) - Character controller
7. ✅ `src/jvmMain/kotlin/io/kreekt/physics/bullet/BulletPhysicsEngine.kt` (119 lines) - Main engine

**Total extracted**: 1,286 lines across 7 focused modules
**Original file reduced**: 1,206 → 18 lines (99% reduction!)

### Remaining Modules (Planned)
See REFACTORING_REPORT.md for complete extraction plan (~150+ modules)

---

## Next Files to Refactor (Recommended Order)

### Batch 1: Complete Rapier Physics
1. 🟡 RapierPhysics.kt (finish extraction, 4 more modules needed)
2. BulletPhysics.kt (replicate Rapier pattern)

### Batch 2: Geometry System
3. GeometryProcessor.kt (LOD, simplification, attributes, optimization)

### Batch 3: Rendering Core
4. BufferManager.kt
5. ShadowMapper.kt
6. LightProbe.kt
7. IBLProcessor.kt

### Batch 4: Material System
8. MaterialLibrary.kt
9. MaterialPreview.kt
10. TextureAtlas.kt

### Batch 5: Tools
11. SceneEditorCompose.kt
12. PerformanceBenchmark.kt
13. TestEngine.kt

---

## Validation Commands

```bash
# Check file sizes
find src -name "*.kt" -exec wc -l {} + | awk '$1 > 500' | sort -rn

# Count total files over 500 lines
find src -name "*.kt" -exec wc -l {} + | awk '$1 > 500' | wc -l

# Track progress
find src -name "*.kt" -exec wc -l {} + | awk '$1 > 500 && $1 < 1000' | wc -l  # Medium
find src -name "*.kt" -exec wc -l {} + | awk '$1 >= 1000' | wc -l  # Critical

# Verify extracted modules exist
ls -lah src/jsMain/kotlin/io/kreekt/physics/rapier/{utils,shapes}/
```

---

**Last Updated**: 2025-10-02
**Next Update**: After completing RapierPhysics.kt refactoring
**Goal**: 0 files over 500 lines by end of refactoring initiative
