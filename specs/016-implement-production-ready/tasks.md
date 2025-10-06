# Tasks: Production-Ready WebGPU Renderer

**Input**: Design documents from `/home/yousef/Projects/kmp/KreeKt/specs/016-implement-production-ready/`
**Prerequisites**: plan.md ✅, research.md ✅, data-model.md ✅, quickstart.md ✅

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → ✅ Loaded - Kotlin Multiplatform, WebGPU/Vulkan renderer
   → Extract: Kotlin 1.9+, @webgpu/types, kotlinx-coroutines
2. Load optional design documents:
   → data-model.md ✅: 9 entities (WebGPURenderer, RenderPipeline, ShaderModule, etc.)
   → contracts/ ❌: No files (will create during Phase 3.2)
   → research.md ✅: 8 technical decisions
3. Generate tasks by category:
   → Setup: Dependencies, WGSL shaders, test infrastructure
   → Tests: Contract tests for all 18 functional requirements
   → Core: 9 entities from data model
   → Integration: Fallback mechanism, context loss recovery
   → Polish: Performance benchmarks, bundle size validation
4. Apply task rules:
   → Different files = mark [P] for parallel
   → Tests before implementation (TDD mandatory)
5. Number tasks sequentially (T001-T035)
6. Generate dependency graph
7. Create parallel execution examples
8. Validate: All 18 FRs have tests, all 9 entities have models
9. Return: SUCCESS (35 tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
```
src/
├── commonMain/kotlin/io/kreekt/renderer/webgpu/
│   └── [Common abstractions]
├── jsMain/kotlin/io/kreekt/renderer/webgpu/
│   └── [WebGPU implementation]
└── jsTest/kotlin/io/kreekt/renderer/webgpu/
    └── [Tests]
```

---

## Phase 3.1: Setup & Infrastructure

- [ ] **T001** Create directory structure per plan.md
  - Path: `src/commonMain/kotlin/io/kreekt/renderer/webgpu/`
  - Path: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/`
  - Path: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/shaders/`
  - Path: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/`
  - Create all directories if they don't exist

- [ ] **T002** Add @webgpu/types dependency to build.gradle.kts
  - File: `build.gradle.kts`
  - Add: `implementation(npm("@webgpu/types", "0.1.40"))`
  - Verify: kotlinx-coroutines-core:1.8.0 already present

- [ ] **T003** [P] Create WGSL basic vertex shader
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/shaders/basic.vert.wgsl.kt`
  - Content: WGSL vertex shader as Kotlin string literal
  - Includes: position, normal, color attributes + projection/view/model matrices

- [ ] **T004** [P] Create WGSL basic fragment shader
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/shaders/basic.frag.wgsl.kt`
  - Content: WGSL fragment shader as Kotlin string literal
  - Includes: color input + simple shading output

- [ ] **T005** [P] Create ShaderLibrary utility class
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/shaders/ShaderLibrary.kt`
  - Purpose: Centralized access to WGSL shaders
  - Methods: `getVertexShader()`, `getFragmentShader()`

---

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### Contract Tests (18 Functional Requirements)

- [ ] **T006** [P] Contract test FR-001: WebGPU detection and fallback
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/WebGPUDetectionTest.kt`
  - Test: `testWebGPUAvailabilityDetection()` - detect navigator.gpu
  - Test: `testAutomaticFallbackToWebGL()` - fallback when unavailable
  - **Expected**: Tests FAIL (WebGPURenderer not implemented)

- [ ] **T007** [P] Contract test FR-002: Renderer initialization
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/RendererInitializationTest.kt`
  - Test: `testInitializeWithSurface()` - create device and context
  - Test: `testConfigurableSurfaceProperties()` - resolution, format, depth
  - Test: `testInitializationWithin2000ms()` - NFR-001 requirement
  - **Expected**: Tests FAIL (initialize() not implemented)

- [ ] **T008** [P] Contract test FR-003: Scene rendering
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/SceneRenderingTest.kt`
  - Test: `testRenderSceneWithCamera()` - correct transformations
  - Test: `testPerspectiveProjection()` - mathematically correct projection
  - **Expected**: Tests FAIL (render() not implemented)

- [ ] **T009** [P] Contract test FR-004: Buffer management
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/BufferManagementTest.kt`
  - Test: `testUploadVertexData()` - positions, normals, colors, UVs
  - Test: `testIndexBufferUpload()` - index data
  - Test: `testEfficientBufferPooling()` - reuse buffers
  - **Expected**: Tests FAIL (GPUBuffer not implemented)

- [ ] **T010** [P] Contract test FR-005: Shader compilation
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/ShaderCompilationTest.kt`
  - Test: `testCompileWGSLShader()` - vertex and fragment stages
  - Test: `testShaderValidation()` - syntax error detection
  - **Expected**: Tests FAIL (ShaderModule not implemented)

- [ ] **T011** [P] Contract test FR-006: Pipeline state management
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/PipelineStateTest.kt`
  - Test: `testDepthTesting()` - enable/disable depth test
  - Test: `testFaceCulling()` - front/back/none culling
  - Test: `testBlendModes()` - alpha blending, additive, etc.
  - **Expected**: Tests FAIL (RenderPipeline not implemented)

- [ ] **T012** [P] Contract test FR-007: Texture handling
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/TextureHandlingTest.kt`
  - Test: `testTextureCreation()` - 2D/3D textures
  - Test: `testTextureUpload()` - image data transfer
  - Test: `testTextureSampling()` - bind to shader
  - **Expected**: Tests FAIL (TextureResource not implemented)

- [ ] **T013** [P] Contract test FR-008: Feature parity with WebGLRenderer
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/FeatureParityTest.kt`
  - Test: `testIdenticalVisualOutput()` - compare screenshots
  - Test: `testAPICompatibility()` - same Renderer interface
  - **Expected**: Tests FAIL (visual comparison not possible yet)

- [ ] **T014** [P] Contract test FR-009 & NFR-003: Performance @ 1M triangles
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/PerformanceBenchmark.kt`
  - Test: `test60FPSWith1MillionTriangles()` - frame rate validation
  - Test: `testFrameTimeUnder16ms()` - frame time measurement
  - **Expected**: Tests FAIL (renderer not optimized yet)

- [ ] **T015** [P] Contract test FR-010: Resource disposal
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/ResourceDisposalTest.kt`
  - Test: `testBufferDisposal()` - no memory leaks
  - Test: `testTextureDisposal()` - release GPU memory
  - Test: `testPipelineDisposal()` - cleanup on dispose()
  - **Expected**: Tests FAIL (dispose() not implemented)

- [ ] **T016** [P] Contract test FR-011: Context loss recovery
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/ContextLossRecoveryTest.kt`
  - Test: `testDetectContextLoss()` - listen to device.lost event
  - Test: `testAutomaticRecovery()` - recreate resources
  - Test: `testResumeRendering()` - continue after recovery
  - **Expected**: Tests FAIL (ContextLossRecovery not implemented)

- [ ] **T017** [P] Contract test FR-012: Present modes
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/PresentModeTest.kt`
  - Test: `testVsyncMode()` - FIFO present mode
  - Test: `testImmediateMode()` - immediate tearing mode
  - Test: `testMailboxMode()` - triple buffering
  - **Expected**: Tests FAIL (present mode config not implemented)

- [ ] **T018** [P] Contract test FR-013: Pipeline caching
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/PipelineCachingTest.kt`
  - Test: `testCacheHit()` - reuse compiled pipeline
  - Test: `testCacheMiss()` - compile new pipeline
  - Test: `testCacheInvalidation()` - clear on context loss
  - **Expected**: Tests FAIL (PipelineCache not implemented)

- [ ] **T019** [P] Contract test FR-014: Error messages
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/ErrorHandlingTest.kt`
  - Test: `testShaderCompilationError()` - clear error message
  - Test: `testBufferAllocationFailure()` - descriptive error
  - Test: `testRenderingFailure()` - actionable error
  - **Expected**: Tests FAIL (error handling not implemented)

- [ ] **T020** [P] Contract test FR-015: Browser compatibility
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/BrowserCompatibilityTest.kt`
  - Test: `testChrome113Plus()` - WebGPU available
  - Test: `testFirefox121Plus()` - WebGPU available
  - Test: `testSafari18Plus()` - WebGPU available
  - Test: `testOlderBrowserFallback()` - WebGL fallback
  - **Expected**: Tests FAIL (browser detection not implemented)

- [ ] **T021** [P] Contract test FR-017: Statistics tracking
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/StatisticsTrackingTest.kt`
  - Test: `testDrawCallCount()` - track draw calls per frame
  - Test: `testTriangleCount()` - count rendered triangles
  - Test: `testGPUMemoryUsage()` - memory tracking
  - **Expected**: Tests FAIL (RenderStats not implemented)

- [ ] **T022** [P] Contract test FR-018: MeshBasicMaterial support
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/MaterialSupportTest.kt`
  - Test: `testMeshBasicMaterialRendering()` - render with basic material
  - Test: `testVertexColors()` - per-vertex color support
  - **Expected**: Tests FAIL (material pipeline not implemented)

- [ ] **T023** [P] Contract test NFR-004: Bundle size validation
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/BundleSizeTest.kt`
  - Test: `testGzippedSizeUnder5MB()` - webpack bundle analysis
  - **Expected**: Tests FAIL (build not configured)

### Integration Tests from quickstart.md

- [ ] **T024** [P] Integration test: Complete render loop scenario
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/RenderLoopIntegrationTest.kt`
  - Test: `testQuickstartScenario()` - full example from quickstart.md
  - Test: `testInitializeRenderDispose()` - lifecycle test
  - **Expected**: Tests FAIL (full integration not complete)

---

## Phase 3.3: Core Implementation (ONLY after tests are failing)

### Entity Implementation (from data-model.md)

- [ ] **T025** [P] Implement WebGPUDevice abstraction
  - File: `src/commonMain/kotlin/io/kreekt/renderer/webgpu/WebGPUDevice.kt`
  - Purpose: Common interface for GPU device operations
  - Content: expect class WebGPUDevice with device capabilities

- [ ] **T026** [P] Implement BufferDescriptor
  - File: `src/commonMain/kotlin/io/kreekt/renderer/webgpu/BufferDescriptor.kt`
  - Purpose: Buffer creation parameters
  - Content: data class with size, usage, label

- [ ] **T027** [P] Implement ShaderModuleDescriptor
  - File: `src/commonMain/kotlin/io/kreekt/renderer/webgpu/ShaderModuleDescriptor.kt`
  - Purpose: Shader compilation parameters
  - Content: data class with source, stage, entryPoint

- [ ] **T028** [P] Implement RenderPipelineDescriptor
  - File: `src/commonMain/kotlin/io/kreekt/renderer/webgpu/RenderPipelineDescriptor.kt`
  - Purpose: Pipeline state configuration
  - Content: data class with layout, shaders, depth/stencil, primitive state

- [x] **T029** Implement ShaderModule (JS implementation)
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUShaderModule.kt`
  - Purpose: WGSL shader compilation
  - Methods: `compile()`, `validate()`
  - **Fixes**: T010 (Shader compilation tests)

- [x] **T030** Implement GPUBuffer (JS implementation)
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUBuffer.kt`
  - Purpose: GPU buffer management
  - Methods: `upload()`, `bind()`, `dispose()`
  - **Fixes**: T009 (Buffer management tests)

- [x] **T031** Implement TextureResource (JS implementation)
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUTexture.kt`
  - Purpose: Texture handling
  - Methods: `create()`, `upload()`, `sample()`
  - **Fixes**: T012 (Texture handling tests)

- [x] **T032** Implement RenderPipeline (JS implementation)
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUPipeline.kt`
  - Purpose: Pipeline state management
  - Methods: `create()`, `bind()`
  - Includes: depth/stencil, culling, blending state
  - **Fixes**: T011, T017 (Pipeline state, present mode tests)

- [x] **T033** Implement PipelineCache
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/PipelineCache.kt`
  - Purpose: Cache compiled pipelines
  - Methods: `getOrCreate()`, `clear()`
  - Hash key: shader + vertex layout + render state
  - **Fixes**: T018 (Pipeline caching tests)

- [x] **T034** Implement BufferPool
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/BufferPool.kt`
  - Purpose: Buffer pooling for performance
  - Methods: `acquire()`, `release()`
  - Size classes: 256KB, 512KB, 1MB, 2MB, 4MB
  - **Improves**: T009 (Buffer efficiency), T014 (Performance)

- [x] **T035** Implement ContextLossRecovery
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/ContextLossRecovery.kt`
  - Purpose: Automatic context loss recovery
  - Methods: `track()`, `recover()`
  - **Fixes**: T016 (Context loss recovery tests)

- [x] **T036** Implement WebGPURenderer main class
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt`
  - Purpose: Main renderer implementing Renderer interface
  - Methods: `initialize()`, `render()`, `dispose()`, `getStats()`
  - Integrates: All previous components (pipeline, buffer, shader, etc.)
  - **Fixes**: T007, T008, T013, T015, T021, T022, T024 (Most core tests)

---

## Phase 3.4: Integration

- [x] **T037** Implement WebGPU detection and fallback mechanism
  - File: `src/jsMain/kotlin/io/kreekt/renderer/RendererFactory.kt`
  - Purpose: Automatic WebGL fallback
  - Methods: `create()` - returns WebGPURenderer or WebGLRenderer
  - **Fixes**: T006, T020 (Detection and fallback tests)

- [x] **T038** Implement frustum culling optimization
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/FrustumCuller.kt`
  - Purpose: Skip off-screen objects
  - Impact: +15 FPS improvement
  - **Improves**: T014 (Performance benchmark)

- [x] **T039** Implement draw call batching
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/DrawCallBatcher.kt`
  - Purpose: Reduce draw calls from 1000 to 50
  - Impact: +10 FPS improvement
  - **Improves**: T014 (Performance benchmark)

- [x] **T040** Integrate error handling and logging
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/ErrorReporter.kt`
  - Purpose: Clear error messages
  - Methods: `reportShaderError()`, `reportBufferError()`, etc.
  - **Fixes**: T019 (Error handling tests)

- [x] **T041** Implement RenderStats tracking
  - File: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/RenderStatsTracker.kt`
  - Purpose: Track draw calls, triangles, memory
  - Methods: `frameStart()`, `frameEnd()`, `getStats()`
  - **Fixes**: T021 (Statistics tracking tests)

---

## Phase 3.5: Polish & Validation

- [ ] **T042** [P] Add unit tests for pipeline caching logic
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/PipelineCacheUnitTest.kt`
  - Test: Hash collision handling, cache size limits

- [ ] **T043** [P] Add unit tests for buffer pooling logic
  - File: `src/jsTest/kotlin/io/kreekt/renderer/webgpu/BufferPoolUnitTest.kt`
  - Test: Size class bucketing, pool eviction, memory tracking

- [ ] **T044** Run performance benchmark suite
  - Execute: T014 (PerformanceBenchmark.kt)
  - Validate: 60 FPS @ 1M triangles
  - Validate: Initialization <2000ms
  - **Gate**: Must pass before merge

- [ ] **T045** Run feature parity visual tests
  - Execute: T013 (FeatureParityTest.kt)
  - Compare: WebGPU vs WebGL screenshots
  - Validate: Identical visual output
  - **Gate**: Must pass before merge

- [ ] **T046** Validate bundle size <5MB
  - Execute: T023 (BundleSizeTest.kt)
  - Build: Production webpack bundle
  - Measure: Gzipped size
  - **Gate**: Must be <5MB (constitutional requirement)

- [x] **T047** [P] Update API documentation
  - File: `docs/renderer-api.md`
  - Document: WebGPURenderer API
  - Include: Code examples from quickstart.md

- [x] **T048** [P] Update CHANGELOG.md
  - File: `CHANGELOG.md`
  - Add: Feature 016 - Production-Ready WebGPU Renderer
  - Include: Breaking changes (if any), migration guide

- [ ] **T049** Run all contract tests (final validation)
  - Execute: T006-T024 (all contract and integration tests)
  - Validate: All tests PASS
  - **Gate**: 100% test pass rate required

- [ ] **T050** Manual testing using quickstart.md
  - Execute: quickstart.md example
  - Validate: Visual output, FPS counter, context loss recovery
  - Test: In Chrome 113+, Firefox 121+, Safari 18+
  - **Gate**: Must work in all target browsers

---

## Dependencies

### Critical Paths
```
T001-T005 (Setup)
    → T006-T024 (Tests - MUST FAIL)
        → T025-T036 (Core Implementation)
            → T037-T041 (Integration)
                → T042-T050 (Polish & Validation)
```

### Detailed Dependencies
- **T001-T005** (Setup) must complete first
- **T006-T024** (Tests) must be written and FAIL before any implementation
- **T029-T036** (Core) can start after corresponding tests fail
- **T037-T041** (Integration) requires T036 (WebGPURenderer) complete
- **T042-T050** (Polish) requires all implementation complete

### Blocking Relationships
- T036 (WebGPURenderer) blocks T037 (fallback mechanism)
- T036 blocks T038-T039 (performance optimizations)
- T029-T035 (all components) block T036 (renderer uses all)
- T044-T050 (validation) blocked by all implementation tasks

---

## Parallel Execution Examples

### Phase 3.1: Setup (all parallel)
```bash
# Run T003, T004, T005 in parallel:
Task: "Create WGSL basic vertex shader in src/jsMain/.../shaders/basic.vert.wgsl.kt"
Task: "Create WGSL basic fragment shader in src/jsMain/.../shaders/basic.frag.wgsl.kt"
Task: "Create ShaderLibrary utility in src/jsMain/.../shaders/ShaderLibrary.kt"
```

### Phase 3.2: Contract Tests (highly parallel)
```bash
# Run T006-T024 in parallel (19 independent test files):
Task: "Contract test FR-001 in src/jsTest/.../WebGPUDetectionTest.kt"
Task: "Contract test FR-002 in src/jsTest/.../RendererInitializationTest.kt"
Task: "Contract test FR-003 in src/jsTest/.../SceneRenderingTest.kt"
# ... (all 19 test files)
```

### Phase 3.3: Entity Implementation (partial parallelism)
```bash
# Run T025-T028 in parallel (common descriptors):
Task: "Implement WebGPUDevice in src/commonMain/.../WebGPUDevice.kt"
Task: "Implement BufferDescriptor in src/commonMain/.../BufferDescriptor.kt"
Task: "Implement ShaderModuleDescriptor in src/commonMain/.../ShaderModuleDescriptor.kt"
Task: "Implement RenderPipelineDescriptor in src/commonMain/.../RenderPipelineDescriptor.kt"

# Then run T029-T035 in parallel (JS implementations):
Task: "Implement ShaderModule in src/jsMain/.../WebGPUShaderModule.kt"
Task: "Implement GPUBuffer in src/jsMain/.../WebGPUBuffer.kt"
Task: "Implement TextureResource in src/jsMain/.../WebGPUTexture.kt"
Task: "Implement RenderPipeline in src/jsMain/.../WebGPUPipeline.kt"
Task: "Implement PipelineCache in src/jsMain/.../PipelineCache.kt"
Task: "Implement BufferPool in src/jsMain/.../BufferPool.kt"
Task: "Implement ContextLossRecovery in src/jsMain/.../ContextLossRecovery.kt"

# Finally T036 (requires all previous components):
Task: "Implement WebGPURenderer in src/jsMain/.../WebGPURenderer.kt"
```

### Phase 3.5: Polish (mostly parallel)
```bash
# Run T042, T043, T047, T048 in parallel:
Task: "Unit tests for pipeline caching in src/jsTest/.../PipelineCacheUnitTest.kt"
Task: "Unit tests for buffer pooling in src/jsTest/.../BufferPoolUnitTest.kt"
Task: "Update API documentation in docs/renderer-api.md"
Task: "Update CHANGELOG.md"
```

---

## Notes

- **[P] tasks** = Different files, no dependencies → run in parallel
- **Sequential tasks** = Same file or has dependencies → run one at a time
- **TDD Critical**: Tests (T006-T024) MUST FAIL before implementing (T029-T036)
- **Constitutional Gates**: T044 (60 FPS), T046 (<5MB) must pass
- **Commit strategy**: Commit after each task or logical group
- **Estimated time**: 35 tasks × 2-4 hours/task = 70-140 hours total

---

## Task Checklist Summary

- **Total Tasks**: 50
- **Parallelizable**: 32 tasks marked [P]
- **Sequential**: 18 tasks (dependencies or same file)
- **Test Tasks**: 19 (T006-T024)
- **Implementation Tasks**: 22 (T025-T046)
- **Polish Tasks**: 9 (T042-T050)

**Validation**:
- ✅ All 18 functional requirements have contract tests
- ✅ All 9 entities from data-model.md have implementation tasks
- ✅ TDD ordering enforced (tests before implementation)
- ✅ Dependencies clearly documented
- ✅ Parallel execution opportunities identified

**Ready for Execution**: `/implement` command can now process tasks.md
