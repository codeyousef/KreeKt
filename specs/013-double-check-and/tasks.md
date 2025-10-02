# Tasks: Complete Three.js r180 Feature Parity

**Feature**: 013-double-check-and
**Input**: Design documents from `D:/Projects/KMP/KreeKt/specs/013-double-check-and/`
**Prerequisites**: plan.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

## Execution Flow (main)

```
✅ 1. Loaded plan.md - tech stack identified (Kotlin Multiplatform, WebGPU, Vulkan)
✅ 2. Loaded design documents - 9 major systems, 85 requirements extracted
✅ 3. Generated tasks by category (Setup, Tests, Core, Integration, Polish)
✅ 4. Applied task rules (TDD, parallel marking, file dependencies)
✅ 5. Numbered tasks sequentially (T001-T245)
✅ 6. Generated dependency graph
✅ 7. Created parallel execution examples
✅ 8. Validated completeness (all contracts covered, all tests first)
✅ 9. SUCCESS - Tasks ready for execution
```

## Format: `[ID] [P?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- Exact file paths included for each task
- Follow TDD: Tests before implementation

## Constitutional Requirements

**CRITICAL**: This feature enforces KreeKt Constitution Principle I:
> "Test-Driven Development is strictly enforced using the Red-Green-Refactor cycle. Tests MUST be written before any
> implementation code. All tests MUST pass before moving to the next task. No stubs, workarounds, TODOs, 'in the
> meantime', 'for now', or 'in a real implementation' placeholders are permitted."

**VERIFICATION**: After all tasks complete:

1. Run:
   `find src -name "*.kt" | xargs grep -i "TODO\|FIXME\|for now\|in the meantime\|in a real implementation" | wc -l`
2. Expected result: **0** (zero matches in production code)
3. Run: `./gradlew allTests`
4. Expected result: **All tests pass** (0 failures)

---

## Phase 1: Setup & Infrastructure (10 tasks)

### T001 [X] [P] Create post-processing module structure

**Directories**:

- `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/`
- `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/`
- `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/jsMain/kotlin/io/kreekt/postprocessing/`
- `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/jvmMain/kotlin/io/kreekt/postprocessing/`
  **Description**: Create module directories and `build.gradle.kts` for post-processing system.
  **Contract**: Module depends on `kreekt-core`, `kreekt-renderer`, `kreekt-material`, `kreekt-scene`.
  **Acceptance**:
- Module builds independently
- Platform-specific source sets configured
- Test dependencies configured (kotlin-test, coroutines-test)

### T002 [X] [P] Create loaders module structure

**Directories**:

- `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/`
- `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/`
  **Description**: Create module directories and `build.gradle.kts` for asset loading system.
  **Contract**: Module depends on `kreekt-core`, `kreekt-scene`, `kreekt-geometry`, `kotlinx-serialization-json`.
  **Acceptance**:
- Module builds with coroutines support
- JSON serialization configured
- Platform-specific file I/O capabilities

### T003 [X] [P] Create exporters module structure

**Directories**:

- `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonMain/kotlin/io/kreekt/exporter/`
- `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/`
  **Description**: Create module for asset export functionality.
  **Contract**: Module depends on `kreekt-core`, `kreekt-scene`, `kotlinx-serialization-json`.
  **Acceptance**:
- Module builds independently
- Export options data class available

### T004 [X] [P] Create material-nodes module structure

**Directories**:

- `D:/Projects/KMP/KreeKt/kreekt-material/src/commonMain/kotlin/io/kreekt/material/nodes/`
- `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/`
  **Description**: Create node-based material system directories within existing material module.
  **Contract**: Extends existing `kreekt-material` module with node graph capabilities.
  **Acceptance**:
- Node package structure created
- Test fixtures for node graphs available

### T005 [X] [P] Create helpers module structure

**Directories**:

- `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonMain/kotlin/io/kreekt/helpers/`
- `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/`
  **Description**: Create module for visualization helper objects.
  **Contract**: Module depends on `kreekt-core`, `kreekt-scene`, `kreekt-geometry`.
  **Acceptance**:
- Module builds independently
- Base Helper class available

### T006 [X] [P] Create performance monitoring structure

**Directories**:

- `D:/Projects/KMP/KreeKt/kreekt-core/src/commonMain/kotlin/io/kreekt/performance/`
- `D:/Projects/KMP/KreeKt/kreekt-core/src/commonTest/kotlin/io/kreekt/performance/`
  **Description**: Create performance monitoring system within core module.
  **Contract**: Uses expect/actual for platform-specific implementations.
  **Acceptance**:
- Performance package created in core module
- Platform-specific source sets configured

### T007 [X] [P] Create texture compression structure

**Directories**:

- `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonMain/kotlin/io/kreekt/texture/`
- `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/texture/`
  **Description**: Extend renderer module with texture compression capabilities.
  **Contract**: Platform-specific texture format support.
  **Acceptance**:
- Texture package extended
- Compressed texture classes available

### T008 [X] [P] Create shader system enhancements

**Directories**:

- `D:/Projects/KMP/KreeKt/kreekt-material/src/commonMain/kotlin/io/kreekt/material/shader/`
- `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/`
  **Description**: Extend material module with shader chunk and preprocessing system.
  **Contract**: Shader preprocessor for WGSL and SPIR-V.
  **Acceptance**:
- Shader package extended
- Preprocessor utilities available

### T009 [X] [P] Create geometry utilities structure

**Directories**:

- `D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonMain/kotlin/io/kreekt/geometry/utils/`
- `D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/`
  **Description**: Extend geometry module with advanced utilities.
  **Contract**: Convex hull, simplification, tessellation algorithms.
  **Acceptance**:
- Utils package created
- Geometry processor utilities available

### T010 [X] [P] Create alternative renderers structure

**Directories**:

- `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/svg/`
- `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/css/`
  **Description**: Extend renderer module for SVG and CSS renderers.
  **Contract**: Alternative rendering backends.
  **Acceptance**:
- SVG and CSS renderer packages created
- Renderer interface implemented

---

## Phase 2: Contract Tests - Post-Processing (25 tasks)

**⚠️ CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### EffectComposer Tests

### T011 [X] [P] Write EffectComposer creation and pass management test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/EffectComposerTest.kt`
**Description**: Test EffectComposer constructor, addPass, removePass, insertPass methods.
**Test Logic**:

```kotlin
val renderer = MockRenderer()
val composer = EffectComposer(renderer)
val pass1 = RenderPass(scene, camera)
val pass2 = BloomPass()

composer.addPass(pass1)
assertEquals(1, composer.passes.size)
composer.addPass(pass2)
assertEquals(2, composer.passes.size)
composer.removePass(pass1)
assertEquals(1, composer.passes.size)
```

**Acceptance**: Test fails until EffectComposer is implemented

### T012 [X] [P] Write EffectComposer render pipeline test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/EffectComposerTest.kt`
**Description**: Test render() method executes passes in sequence.
**Test Logic**: Verify passes execute in order, ping-pong buffering works correctly
**Acceptance**: Test validates render pipeline execution

### T013 [X] [P] Write EffectComposer resize test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/EffectComposerTest.kt`
**Description**: Test setSize() propagates to all passes and render targets.
**Acceptance**: All passes and targets resize correctly

### Pass Base Class Tests

### T014 [X] [P] Write Pass enabled/disabled test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/PassTest.kt`
**Description**: Test Pass.enabled flag controls execution.
**Acceptance**: Disabled passes are skipped during render

### T015 [X] [P] Write Pass needsSwap test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/PassTest.kt`
**Description**: Test needsSwap controls buffer swapping.
**Acceptance**: Buffer swap logic respects needsSwap flag

### RenderPass Tests

### T016 [X] [P] Write RenderPass scene rendering test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/RenderPassTest.kt`
**Description**: Test RenderPass renders scene with camera to target.
**Test Logic**:

```kotlin
val scene = Scene()
val camera = PerspectiveCamera()
val pass = RenderPass(scene, camera)
val composer = EffectComposer(renderer)
composer.addPass(pass)
// Verify scene rendered to writeBuffer
```

**Acceptance**: Scene renders correctly to render target

### T017 [X] [P] Write RenderPass override material test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/RenderPassTest.kt`
**Description**: Test overrideMaterial parameter replaces scene materials.
**Acceptance**: All objects rendered with override material

### T018 [X] [P] Write RenderPass clear color test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/RenderPassTest.kt`
**Description**: Test clearColor and clearAlpha parameters.
**Acceptance**: Background cleared with specified color

### ShaderPass Tests

### T019 [X] [P] Write ShaderPass custom shader test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/ShaderPassTest.kt`
**Description**: Test ShaderPass applies custom shader to full-screen quad.
**Test Logic**: Create shader with simple color manipulation, verify output
**Acceptance**: Custom shader executes correctly

### T020 [X] [P] Write ShaderPass uniform update test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/ShaderPassTest.kt`
**Description**: Test ShaderPass.uniforms can be updated per-frame.
**Acceptance**: Uniform updates propagate to shader

### OutputPass Tests

### T021 [X] [P] Write OutputPass color space conversion test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/OutputPassTest.kt`
**Description**: Test OutputPass performs final color space conversion.
**Acceptance**: Output converted to display color space

### BloomPass Tests

### T022 [X] [P] Write BloomPass threshold test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/BloomPassTest.kt`
**Description**: Test BloomPass.threshold parameter filters bright pixels.
**Test Logic**: Render scene, verify only pixels above threshold contribute to bloom
**Acceptance**: Threshold correctly filters input

### T023 [X] [P] Write BloomPass strength test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/BloomPassTest.kt`
**Description**: Test BloomPass.strength controls bloom intensity.
**Acceptance**: Strength parameter affects output intensity

### T024 [X] [P] Write BloomPass radius test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/BloomPassTest.kt`
**Description**: Test BloomPass.radius controls bloom spread.
**Acceptance**: Radius parameter affects blur extent

### UnrealBloomPass Tests

### T025 [X] [P] Write UnrealBloomPass multi-scale test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/UnrealBloomPassTest.kt`
**Description**: Test UnrealBloomPass uses multiple resolution levels.
**Acceptance**: Multi-scale bloom rendering works correctly

### SSAOPass Tests

### T026 [X] [P] Write SSAOPass kernel generation test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/SSAOPassTest.kt`
**Description**: Test SSAOPass generates hemisphere kernel for sampling.
**Acceptance**: Kernel size matches kernelSize parameter

### T027 [X] [P] Write SSAOPass depth/normal rendering test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/SSAOPassTest.kt`
**Description**: Test SSAOPass renders depth and normal buffers.
**Acceptance**: Depth and normal render targets created correctly

### T028 [X] [P] Write SSAOPass output modes test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/SSAOPassTest.kt`
**Description**: Test SSAOPass.output parameter (Default, SSAO, Blur, Beauty, Depth, Normal).
**Acceptance**: All output modes produce expected results

### SAOPass Tests

### T029 [X] [P] Write SAOPass scalable AO test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/SAOPassTest.kt`
**Description**: Test SAO algorithm produces ambient occlusion.
**Acceptance**: SAO output differs from SSAO (scalable variant)

### Anti-Aliasing Pass Tests

### T030 [X] [P] Write SMAAPass test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/SMAAPassTest.kt`
**Description**: Test SMAA edge detection and blending.
**Acceptance**: SMAA produces antialiased output

### T031 [X] [P] Write FXAAPass test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/FXAAPassTest.kt`
**Description**: Test FXAA fast approximation algorithm.
**Acceptance**: FXAA smooths edges

### T032 [X] [P] Write TAAPass temporal accumulation test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/TAAPassTest.kt`
**Description**: Test TAA uses previous frame for temporal antialiasing.
**Acceptance**: TAA maintains history buffer, blends frames

### T033 [X] [P] Write SSAAPass supersampling test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/SSAAPassTest.kt`
**Description**: Test SSAA renders at higher resolution and downsamples.
**Acceptance**: SSAA produces high-quality antialiasing

### BokehPass Tests

### T034 [X] [P] Write BokehPass depth-of-field test

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/BokehPassTest.kt`
**Description**: Test BokehPass produces depth-based blur.
**Test Logic**: Verify focus distance, aperture, maxBlur parameters work
**Acceptance**: Objects outside focus range are blurred

### OutlinePass Tests

### T035 [X] [P] Write OutlinePass object selection test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/OutlinePassTest.kt`
**Description**: Test OutlinePass highlights selected objects.
**Test Logic**: Add objects to selectedObjects list, verify outline rendered
**Acceptance**: Outlines drawn around selected objects only

---

## Phase 3: Contract Tests - Asset Loaders (30 tasks)

### LoadingManager Tests

### T036 [X] [P] Write LoadingManager registration test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/LoadingManagerTest.kt`
**Description**: Test registerLoader() associates extensions with loaders.
**Test Logic**:

```kotlin
val manager = LoadingManager()
val gltfLoader = GLTFLoader()
manager.registerLoader("gltf", gltfLoader)
manager.registerLoader("glb", gltfLoader)
assertEquals(gltfLoader, manager.getLoader("gltf"))
```

**Acceptance**: Loader registration and retrieval works

### T037 [X] [P] Write LoadingManager cache test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/LoadingManagerTest.kt`
**Description**: Test addToCache(), getFromCache(), clearCache().
**Acceptance**: Cache stores and retrieves assets by URL

### T038 [X] [P] Write LoadingManager progress tracking test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/LoadingManagerTest.kt`
**Description**: Test onStart, onProgress, onLoad callbacks fire correctly.
**Test Logic**: Monitor itemsLoaded, itemsTotal StateFlows
**Acceptance**: Progress tracking works across multiple concurrent loads

### T039 [X] [P] Write LoadingManager error handling test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/LoadingManagerTest.kt`
**Description**: Test onError callback fires on load failures.
**Acceptance**: Errors reported, loading continues for other assets

### GLTFLoader Tests

### T040 [X] [P] Write GLTFLoader load from URL test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/GLTFLoaderTest.kt`
**Description**: Test GLTFLoader.load() loads GLTF from URL.
**Test Logic**: Mock HTTP response with valid GLTF JSON
**Acceptance**: Returns Scene with correct hierarchy

### T041 [X] [P] Write GLTFLoader load from bytes test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/GLTFLoaderTest.kt`
**Description**: Test GLTFLoader.loadFromBytes() parses GLTF data.
**Acceptance**: Parses binary GLB format and JSON format

### T042 [X] [P] Write GLTFLoader parse test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/GLTFLoaderTest.kt`
**Description**: Test GLTFLoader.parse() converts GLTFStructure to Scene.
**Test Logic**: Create GLTFStructure with nodes, meshes, materials, verify Scene
**Acceptance**: All GLTF components mapped to KreeKt objects

### T043 [X] [P] Write GLTFLoader buffer loading test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/GLTFLoaderTest.kt`
**Description**: Test GLTF loads external buffer files.
**Acceptance**: External .bin files loaded correctly

### T044 [X] [P] Write GLTFLoader DRACO compression test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/GLTFLoaderTest.kt`
**Description**: Test GLTF with DRACO extension uses DRACOLoader.
**Acceptance**: DRACO-compressed geometry decoded

### T045 [X] [P] Write GLTFLoader KTX2 texture test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/GLTFLoaderTest.kt`
**Description**: Test GLTF with KTX2 extension uses KTX2Loader.
**Acceptance**: KTX2 textures loaded and transcoded

### FBXLoader Tests

### T046 [X] [P] Write FBXLoader binary format test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/FBXLoaderTest.kt`
**Description**: Test FBXLoader parses FBX 7.x binary format.
**Test Logic**: Load sample FBX file, verify magic bytes, version
**Acceptance**: FBX binary structure parsed correctly

### T047 [X] [P] Write FBXLoader connection graph test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/FBXLoaderTest.kt`
**Description**: Test FBX connection graph builds scene hierarchy.
**Acceptance**: OO and OP connections resolved to scene graph

### T048 [X] [P] Write FBXLoader animation test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/FBXLoaderTest.kt`
**Description**: Test FBX animations converted to AnimationClips.
**Acceptance**: Animation curves, keyframes extracted

### OBJLoader Tests

### T049 [X] [P] Write OBJLoader geometry parsing test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/OBJLoaderTest.kt`
**Description**: Test OBJLoader parses v, vn, vt, f lines.
**Test Logic**: Parse simple cube.obj, verify vertex/normal/uv data
**Acceptance**: Geometry data extracted correctly

### T050 [X] [P] Write OBJLoader MTL material test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/OBJLoaderTest.kt`
**Description**: Test OBJLoader loads companion .mtl file.
**Acceptance**: Materials applied to object groups

### T051 [X] [P] Write OBJLoader groups test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/OBJLoaderTest.kt`
**Description**: Test OBJ groups (g, o lines) create separate meshes.
**Acceptance**: Each group becomes separate Mesh object

### ColladaLoader Tests

### T052 [X] [P] Write ColladaLoader XML parsing test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/ColladaLoaderTest.kt`
**Description**: Test ColladaLoader parses COLLADA XML structure.
**Acceptance**: XML parsed, library elements extracted

### T053 [X] [P] Write ColladaLoader geometry test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/ColladaLoaderTest.kt`
**Description**: Test COLLADA geometry sources and triangles.
**Acceptance**: Mesh data with sources (positions, normals, uvs) parsed

### T054 [X] [P] Write ColladaLoader scene graph test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/ColladaLoaderTest.kt`
**Description**: Test COLLADA visual_scene builds hierarchy.
**Acceptance**: Node hierarchy with transforms reconstructed

### STLLoader Tests

### T055 [X] [P] Write STLLoader ASCII format test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/STLLoaderTest.kt`
**Description**: Test STLLoader parses ASCII STL format.
**Test Logic**: Parse "solid ... facet normal ... vertex ..." format
**Acceptance**: Triangle mesh created from ASCII STL

### T056 [X] [P] Write STLLoader binary format test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/STLLoaderTest.kt`
**Description**: Test STLLoader parses binary STL format.
**Acceptance**: 80-byte header, triangle count, vertices parsed

### PLYLoader Tests

### T057 [X] [P] Write PLYLoader ASCII format test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/PLYLoaderTest.kt`
**Description**: Test PLYLoader parses ASCII PLY format.
**Acceptance**: Header properties, element lists parsed

### T058 [X] [P] Write PLYLoader binary format test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/PLYLoaderTest.kt`
**Description**: Test PLYLoader parses binary PLY (little/big endian).
**Acceptance**: Binary PLY data read correctly

### 3DMLoader Tests

### T059 [X] [P] Write 3DMLoader 3DS format test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/ThreeDMLoaderTest.kt`
**Description**: Test 3DMLoader parses 3DS chunk-based format.
**Acceptance**: 3DS chunks (MAIN3DS, EDIT3DS, OBJECT) parsed

### USDZLoader Tests

### T060 [X] [P] Write USDZLoader USDZ archive test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/USDZLoaderTest.kt`
**Description**: Test USDZLoader extracts USDZ ZIP archive.
**Acceptance**: USDZ unzipped, USD files extracted

### T061 [X] [P] Write USDZLoader USD parsing test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/USDZLoaderTest.kt`
**Description**: Test USD text format parsing.
**Acceptance**: USD prims, attributes, relationships parsed

### Texture Loader Tests

### T062 [X] [P] Write TextureLoader image formats test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/TextureLoaderTest.kt`
**Description**: Test TextureLoader loads PNG, JPEG, WebP.
**Acceptance**: Common image formats decoded to Texture

### T063 [X] [P] Write EXRLoader HDR test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/EXRLoaderTest.kt`
**Description**: Test EXRLoader loads OpenEXR high dynamic range images.
**Acceptance**: EXR decoded to DataTexture with float data

### T064 [X] [P] Write RGBELoader radiance test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/RGBELoaderTest.kt`
**Description**: Test RGBELoader loads Radiance HDR (.hdr) format.
**Acceptance**: RGBE encoded data decoded to RGB floats

### T065 [X] [P] Write TGALoader test

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonTest/kotlin/io/kreekt/loader/TGALoaderTest.kt`
**Description**: Test TGALoader parses TGA image format.
**Acceptance**: TGA header, image data, RLE compression handled

---

## Phase 4: Contract Tests - Asset Exporters (20 tasks)

### GLTFExporter Tests

### T066 [X] [P] Write GLTFExporter export to JSON test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/GLTFExporterTest.kt`
**Description**: Test GLTFExporter.export() produces valid GLTF JSON.
**Test Logic**:

```kotlin
val exporter = GLTFExporter()
val scene = Scene()
scene.add(Mesh(BoxGeometry(), MeshBasicMaterial()))
val result = exporter.export(scene, ExportOptions(binary = false))
assertTrue(result.isSuccess)
val json = result.getOrNull()!!
// Verify GLTF structure
```

**Acceptance**: Valid GLTF 2.0 JSON produced

### T067 [X] [P] Write GLTFExporter export to binary GLB test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/GLTFExporterTest.kt`
**Description**: Test GLTFExporter exports binary GLB format.
**Acceptance**: GLB magic bytes, JSON chunk, BIN chunk correct

### T068 [X] [P] Write GLTFExporter geometry export test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/GLTFExporterTest.kt`
**Description**: Test BufferGeometry exported to GLTF accessors/bufferViews.
**Acceptance**: Position, normal, UV attributes exported correctly

### T069 [X] [P] Write GLTFExporter material export test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/GLTFExporterTest.kt`
**Description**: Test Material exported to GLTF material properties.
**Acceptance**: PBR material properties mapped to glTF spec

### T070 [X] [P] Write GLTFExporter texture embedding test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/GLTFExporterTest.kt`
**Description**: Test ExportOptions.embedTextures includes texture data.
**Acceptance**: Textures embedded as data URIs or binary chunks

### T071 [X] [P] Write GLTFExporter animation export test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/GLTFExporterTest.kt`
**Description**: Test AnimationClip exported to GLTF animations.
**Acceptance**: Animation samplers, channels exported

### T072 [X] [P] Write GLTFExporter scene hierarchy test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/GLTFExporterTest.kt`
**Description**: Test Object3D hierarchy exported to GLTF nodes.
**Acceptance**: Node transforms, children preserved

### USDZExporter Tests

### T073 [X] [P] Write USDZExporter USD generation test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/USDZExporterTest.kt`
**Description**: Test USDZExporter generates valid USD text.
**Test Logic**: Export scene, verify USD prims, attributes
**Acceptance**: Valid USD format generated

### T074 [X] [P] Write USDZExporter USDZ archive test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/USDZExporterTest.kt`
**Description**: Test USDZ ZIP archive creation.
**Acceptance**: USDZ contains USD file, textures in uncompressed ZIP

### T075 [X] [P] Write USDZExporter AR compatibility test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/USDZExporterTest.kt`
**Description**: Test USDZ follows Apple AR Quick Look requirements.
**Acceptance**: Default camera, lighting, materials for AR

### OBJExporter Tests

### T076 [X] [P] Write OBJExporter geometry export test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/OBJExporterTest.kt`
**Description**: Test OBJExporter writes v, vn, vt, f lines.
**Acceptance**: Valid OBJ text format generated

### T077 [X] [P] Write OBJExporter MTL export test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/OBJExporterTest.kt`
**Description**: Test OBJExporter writes companion .mtl file.
**Acceptance**: Materials written to MTL format

### PLYExporter Tests

### T078 [X] [P] Write PLYExporter ASCII format test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/PLYExporterTest.kt`
**Description**: Test PLYExporter writes ASCII PLY.
**Acceptance**: PLY header, vertex/face lists correct

### T079 [X] [P] Write PLYExporter binary format test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/PLYExporterTest.kt`
**Description**: Test PLYExporter writes binary PLY.
**Acceptance**: Binary PLY with correct endianness

### STLExporter Tests

### T080 [X] [P] Write STLExporter ASCII format test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/STLExporterTest.kt`
**Description**: Test STLExporter writes ASCII STL.
**Acceptance**: "solid ... facet ... vertex" format correct

### T081 [X] [P] Write STLExporter binary format test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/STLExporterTest.kt`
**Description**: Test STLExporter writes binary STL.
**Acceptance**: Binary STL header, triangle count, data correct

### ColladaExporter Tests

### T082 [X] [P] Write ColladaExporter XML generation test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/ColladaExporterTest.kt`
**Description**: Test ColladaExporter writes COLLADA XML.
**Acceptance**: Valid COLLADA 1.4.1 XML structure

### T083 [X] [P] Write ColladaExporter geometry library test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/ColladaExporterTest.kt`
**Description**: Test COLLADA library_geometries section.
**Acceptance**: Sources, triangles, vertices correct

### T084 [X] [P] Write ColladaExporter scene export test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/ColladaExporterTest.kt`
**Description**: Test COLLADA visual_scene with node hierarchy.
**Acceptance**: Nodes, transforms, instance_geometry correct

### T085 [X] [P] Write ExportOptions validation test

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonTest/kotlin/io/kreekt/exporter/ExportOptionsTest.kt`
**Description**: Test ExportOptions parameter handling.
**Acceptance**: All options (binary, embedTextures, etc.) work correctly

---

## Phase 5: Contract Tests - Node-Based Materials (25 tasks)

### MaterialNode Tests

### T086 [X] [P] Write MaterialNode input/output test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/MaterialNodeTest.kt`
**Description**: Test MaterialNode.inputs and .outputs lists.
**Acceptance**: Inputs/outputs have correct types, names

### T087 [X] [P] Write NodeInput connection test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeInputTest.kt`
**Description**: Test NodeInput.connect() establishes connection.
**Acceptance**: isConnected true after connection

### T088 [X] [P] Write NodeInput default value test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeInputTest.kt`
**Description**: Test NodeInput.getValue() returns defaultValue when not connected.
**Acceptance**: Default values work for all NodeDataTypes

### T089 [X] [P] Write NodeOutput connection test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeOutputTest.kt`
**Description**: Test NodeOutput.connect() and disconnect().
**Acceptance**: Connections tracked in connections list

### T090 [X] [P] Write NodeDataType compatibility test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeDataTypeTest.kt`
**Description**: Test NodeDataType.isCompatibleWith() logic.
**Test Logic**:

```kotlin
assertTrue(NodeDataType.FLOAT.isCompatibleWith(NodeDataType.FLOAT))
assertTrue(NodeDataType.FLOAT.isCompatibleWith(NodeDataType.VEC3)) // broadcast
assertFalse(NodeDataType.VEC3.isCompatibleWith(NodeDataType.FLOAT))
```

**Acceptance**: Type compatibility rules enforced

### NodeGraph Tests

### T091 [X] [P] Write NodeGraph add/remove node test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeGraphTest.kt`
**Description**: Test NodeGraph.addNode(), removeNode().
**Acceptance**: Nodes tracked, removal disconnects connections

### T092 [X] [P] Write NodeGraph connect test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeGraphTest.kt`
**Description**: Test NodeGraph.connect() creates valid connections.
**Acceptance**: Connection succeeds for compatible types

### T093 [X] [P] Write NodeGraph type mismatch test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeGraphTest.kt`
**Description**: Test NodeGraph.connect() rejects incompatible types.
**Acceptance**: Returns failure Result for type mismatches

### T094 [X] [P] Write NodeGraph cycle detection test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeGraphTest.kt`
**Description**: Test NodeGraph.connect() prevents cycles.
**Test Logic**: Create A→B→C, attempt C→A connection
**Acceptance**: Cycle detection prevents connection

### T095 [X] [P] Write NodeGraph validate test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeGraphTest.kt`
**Description**: Test NodeGraph.validate() checks graph validity.
**Acceptance**: Detects cycles, disconnected nodes, missing output

### T096 [X] [P] Write NodeGraph topological sort test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeGraphTest.kt`
**Description**: Test NodeGraph topologically sorts nodes for compilation.
**Acceptance**: Nodes sorted in execution order

### T097 [X] [P] Write NodeGraph compile WGSL test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeGraphCompileTest.kt`
**Description**: Test NodeGraph.compile(ShaderTarget.WGSL).
**Test Logic**: Create simple node graph, compile, verify WGSL syntax
**Acceptance**: Valid WGSL shader code generated

### T098 [X] [P] Write NodeGraph compile SPIR-V test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeGraphCompileTest.kt`
**Description**: Test NodeGraph.compile(ShaderTarget.SPIRV).
**Acceptance**: SPIR-V compatible code generated

### Specific Node Tests

### T099 [X] [P] Write TextureSampleNode test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/TextureSampleNodeTest.kt`
**Description**: Test TextureSampleNode generates texture sampling code.
**Acceptance**: WGSL textureSample() call generated

### T100 [X] [P] Write AddNode test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/AddNodeTest.kt`
**Description**: Test AddNode generates addition operation.
**Acceptance**: Code like "result = a + b" generated

### T101 [X] [P] Write MultiplyNode test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/MultiplyNodeTest.kt`
**Description**: Test MultiplyNode generates multiplication.
**Acceptance**: Handles scalar, vector, matrix multiplication

### T102 [X] [P] Write DotProductNode test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/DotProductNodeTest.kt`
**Description**: Test DotProductNode generates dot() call.
**Acceptance**: WGSL dot() function used

### T103 [X] [P] Write NormalizeNode test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NormalizeNodeTest.kt`
**Description**: Test NormalizeNode generates normalize() call.
**Acceptance**: Vector normalization code generated

### T104 [X] [P] Write PBRNode test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/PBRNodeTest.kt`
**Description**: Test PBRNode generates PBR lighting calculation.
**Acceptance**: Metallic-roughness PBR code generated

### T105 [X] [P] Write FresnelNode test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/FresnelNodeTest.kt`
**Description**: Test FresnelNode generates Fresnel effect.
**Acceptance**: Fresnel calculation code generated

### T106 [X] [P] Write ConditionalNode test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/ConditionalNodeTest.kt`
**Description**: Test ConditionalNode generates if/else or select().
**Acceptance**: Conditional logic in shader code

### T107 [X] [P] Write SplitNode test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/SplitNodeTest.kt`
**Description**: Test SplitNode extracts vector components.
**Acceptance**: Swizzle operations (.x, .y, .z, .w) generated

### T108 [X] [P] Write CombineNode test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/CombineNodeTest.kt`
**Description**: Test CombineNode constructs vectors from components.
**Acceptance**: vec3(x, y, z) constructor generated

### T109 [X] [P] Write FragmentOutputNode test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/FragmentOutputNodeTest.kt`
**Description**: Test FragmentOutputNode writes to fragment output.
**Acceptance**: Fragment shader output assignment generated

### T110 [X] [P] Write NodeMaterial integration test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/nodes/NodeMaterialTest.kt`
**Description**: Test NodeMaterial uses node graph for shader.
**Test Logic**: Create NodeMaterial with graph, verify onBeforeCompile compiles graph
**Acceptance**: Node graph compiled to shader code

---

## Phase 6: Contract Tests - Geometry Utilities (15 tasks)

### T111 [X] [P] Write GeometryProcessor convex hull test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorConvexHullTest.kt`
**Description**: Test GeometryProcessor.computeConvexHull().
**Test Logic**: Generate random point cloud, compute hull, verify all points inside/on hull
**Acceptance**: Valid convex hull mesh generated

### T112 [X] [P] Write GeometryProcessor simplification test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorSimplifyTest.kt`
**Description**: Test GeometryProcessor.simplify() reduces triangle count.
**Test Logic**: High-poly mesh → simplify to 50% → verify triangle count reduced, shape preserved
**Acceptance**: Mesh simplified while maintaining shape

### T113 [X] [P] Write GeometryProcessor simplification options test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorSimplifyTest.kt`
**Description**: Test SimplificationOptions (preserveBoundary, preserveUVs, etc.).
**Acceptance**: Options control simplification behavior

### T114 [X] [P] Write GeometryProcessor subdivide Loop test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorSubdivideTest.kt`
**Description**: Test GeometryProcessor.subdivide() with Loop method.
**Test Logic**: Triangle mesh → subdivide → verify each triangle split into 4
**Acceptance**: Loop subdivision produces smooth surface

### T115 [X] [P] Write GeometryProcessor subdivide Catmull-Clark test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorSubdivideTest.kt`
**Description**: Test subdivide() with CatmullClark method.
**Acceptance**: Catmull-Clark subdivision for quad meshes

### T116 [X] [P] Write GeometryProcessor compute tangents test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorTangentsTest.kt`
**Description**: Test GeometryProcessor.computeTangents() using MikkTSpace.
**Test Logic**: Mesh with UVs → compute tangents → verify tangent attribute created
**Acceptance**: Tangents computed correctly for normal mapping

### T117 [X] [P] Write GeometryProcessor merge geometries test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorMergeTest.kt`
**Description**: Test GeometryProcessor.mergeGeometries().
**Test Logic**: Multiple geometries → merge → single geometry with all vertices
**Acceptance**: Geometries combined into one

### T118 [X] [P] Write GeometryProcessor merge with groups test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorMergeTest.kt`
**Description**: Test mergeGeometries(useGroups = true).
**Acceptance**: Groups preserve material indices

### T119 [X] [P] Write GeometryProcessor merge vertices test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorMergeVerticesTest.kt`
**Description**: Test GeometryProcessor.mergeVertices() removes duplicates.
**Test Logic**: Non-indexed geometry → merge vertices → indexed geometry with unique vertices
**Acceptance**: Duplicate vertices within tolerance merged

### T120 [X] [P] Write GeometryProcessor toIndexed test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorIndexedTest.kt`
**Description**: Test GeometryProcessor.toIndexed() creates index buffer.
**Acceptance**: Non-indexed geometry converted to indexed

### T121 [X] [P] Write GeometryProcessor toNonIndexed test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorIndexedTest.kt`
**Description**: Test GeometryProcessor.toNonIndexed() expands indices.
**Acceptance**: Indexed geometry converted to non-indexed

### T122 [X] [P] Write GeometryProcessor compute bounds test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorBoundsTest.kt`
**Description**: Test GeometryProcessor.computeBounds() calculates AABB.
**Acceptance**: Bounding box contains all vertices

### T123 [X] [P] Write GeometryProcessor compute bounding sphere test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorBoundsTest.kt`
**Description**: Test GeometryProcessor.computeBoundingSphere().
**Acceptance**: Bounding sphere contains all vertices

### T124 [X] [P] Write GeometryProcessor estimate memory test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/GeometryProcessorMemoryTest.kt`
**Description**: Test GeometryProcessor.estimateMemoryUsage().
**Test Logic**: Known geometry → estimate → verify byte counts match expected
**Acceptance**: Memory estimate accurate

### T125 [X] [P] Write SimplificationOptions validation test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonTest/kotlin/io/kreekt/geometry/utils/SimplificationOptionsTest.kt`
**Description**: Test SimplificationOptions data class.
**Acceptance**: All options (preserveBoundary, aggressiveness) work

---

## Phase 7: Contract Tests - Helpers (12 tasks)

### T126 [X] [P] Write Helper base class test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/HelperTest.kt`
**Description**: Test Helper abstract class extends LineSegments.
**Acceptance**: Helper.update() and dispose() methods available

### T127 [X] [P] Write VertexNormalsHelper test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/VertexNormalsHelperTest.kt`
**Description**: Test VertexNormalsHelper visualizes vertex normals.
**Test Logic**: Mesh with normals → create helper → verify line segments from vertex to vertex+normal
**Acceptance**: Normals visualized as line segments

### T128 [X] [P] Write VertexNormalsHelper update test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/VertexNormalsHelperTest.kt`
**Description**: Test VertexNormalsHelper.update() refreshes visualization.
**Acceptance**: Update reflects geometry changes

### T129 [X] [P] Write VertexTangentsHelper test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/VertexTangentsHelperTest.kt`
**Description**: Test VertexTangentsHelper visualizes vertex tangents.
**Acceptance**: Tangents visualized in different color

### T130 [X] [P] Write FaceNormalsHelper test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/FaceNormalsHelperTest.kt`
**Description**: Test FaceNormalsHelper visualizes face normals.
**Acceptance**: Normals from face centers displayed

### T131 [X] [P] Write AxesHelper test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/AxesHelperTest.kt`
**Description**: Test AxesHelper creates XYZ axes.
**Test Logic**: Create helper(size = 5) → verify 3 line segments (red X, green Y, blue Z)
**Acceptance**: Axes with correct colors and size

### T132 [X] [P] Write GridHelper test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/GridHelperTest.kt`
**Description**: Test GridHelper creates grid lines.
**Test Logic**: GridHelper(size = 10, divisions = 10) → verify grid geometry
**Acceptance**: Grid with correct size and divisions

### T133 [X] [P] Write GridHelper color test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/GridHelperTest.kt`
**Description**: Test GridHelper colorCenterLine vs colorGrid.
**Acceptance**: Center lines have different color

### T134 [X] [P] Write PolarGridHelper test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/PolarGridHelperTest.kt`
**Description**: Test PolarGridHelper creates polar/radial grid.
**Acceptance**: Concentric circles and radial lines

### T135 [X] [P] Write PlaneHelper test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/PlaneHelperTest.kt`
**Description**: Test PlaneHelper visualizes mathematical plane.
**Acceptance**: Plane with correct normal and size

### T136 [X] [P] Write CameraHelper test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/CameraHelperTest.kt`
**Description**: Test CameraHelper visualizes camera frustum.
**Test Logic**: PerspectiveCamera → CameraHelper → verify frustum lines
**Acceptance**: Frustum visualized with near/far planes

### T137 [X] [P] Write CameraHelper update test

**File**: `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonTest/kotlin/io/kreekt/helpers/CameraHelperTest.kt`
**Description**: Test CameraHelper.update() reflects camera changes.
**Acceptance**: Frustum updates when camera parameters change

---

## Phase 8: Contract Tests - Performance & Texture (15 tasks)

### PerformanceMonitor Tests

### T138 [X] [P] Write PerformanceMonitor frame timing test

**File**: `D:/Projects/KMP/KreeKt/kreekt-core/src/commonTest/kotlin/io/kreekt/performance/PerformanceMonitorTest.kt`
**Description**: Test PerformanceMonitor.beginFrame()/endFrame() tracks frame time.
**Test Logic**:

```kotlin
val monitor = PerformanceMonitor()
monitor.beginFrame()
// simulate work
delay(16) // ~60 FPS
monitor.endFrame()
assertTrue(monitor.getFrameTime() >= 16.0)
```

**Acceptance**: Frame time measured accurately

### T139 [X] [P] Write PerformanceMonitor FPS calculation test

**File**: `D:/Projects/KMP/KreeKt/kreekt-core/src/commonTest/kotlin/io/kreekt/performance/PerformanceMonitorTest.kt`
**Description**: Test PerformanceMonitor.getFPS() calculates frames per second.
**Acceptance**: FPS = 1000 / frameTime

### T140 [X] [P] Write PerformanceMonitor memory tracking test

**File**: `D:/Projects/KMP/KreeKt/kreekt-core/src/commonTest/kotlin/io/kreekt/performance/PerformanceMonitorTest.kt`
**Description**: Test PerformanceMonitor.getMemoryUsage() returns MemoryInfo.
**Acceptance**: Memory info with used/total/limit bytes

### T141 [X] [P] Write PerformanceMonitor draw calls test

**File**: `D:/Projects/KMP/KreeKt/kreekt-core/src/commonTest/kotlin/io/kreekt/performance/PerformanceMonitorTest.kt`
**Description**: Test PerformanceMonitor tracks draw calls, triangles.
**Acceptance**: Counters increment during rendering

### T142 [X] [P] Write PerformanceMonitor metrics history test

**File**: `D:/Projects/KMP/KreeKt/kreekt-core/src/commonTest/kotlin/io/kreekt/performance/PerformanceMonitorTest.kt`
**Description**: Test PerformanceMonitor.getMetrics() returns history.
**Test Logic**: Record 60 frames → getMetrics(60) → verify list size
**Acceptance**: Metrics stored in rolling window

### T143 [X] [P] Write PerformanceMonitor averages test

**File**: `D:/Projects/KMP/KreeKt/kreekt-core/src/commonTest/kotlin/io/kreekt/performance/PerformanceMonitorTest.kt`
**Description**: Test PerformanceMonitor.getAverages() computes statistics.
**Acceptance**: Average FPS, min FPS, max frame time calculated

### T144 [X] [P] Write PerformanceMonitor reset test

**File**: `D:/Projects/KMP/KreeKt/kreekt-core/src/commonTest/kotlin/io/kreekt/performance/PerformanceMonitorTest.kt`
**Description**: Test PerformanceMonitor.reset() clears history.
**Acceptance**: Metrics cleared after reset

### T145 [X] [P] Write PerformanceMonitor platform-specific test (JVM)

**File**: `D:/Projects/KMP/KreeKt/kreekt-core/src/jvmTest/kotlin/io/kreekt/performance/JvmPerformanceMonitorTest.kt`
**Description**: Test JVM PerformanceMonitor uses Runtime.getRuntime() for memory.
**Acceptance**: JVM memory info accurate

### T146 [X] [P] Write PerformanceMonitor platform-specific test (JS)

**File**: `D:/Projects/KMP/KreeKt/kreekt-core/src/jsTest/kotlin/io/kreekt/performance/JsPerformanceMonitorTest.kt`
**Description**: Test JS PerformanceMonitor uses performance.now() for timing.
**Acceptance**: High-resolution timing on JS

### TextureCompressor Tests

### T147 [X] [P] Write TextureCompressor supported formats test

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/texture/TextureCompressorTest.kt`
**Description**: Test TextureCompressor.getSupportedFormats() returns platform formats.
**Acceptance**: Desktop: S3TC/BPTC, Mobile: ETC2/ASTC

### T148 [X] [P] Write TextureCompressor format selection test

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/texture/TextureCompressorTest.kt`
**Description**: Test TextureCompressor.selectBestFormat() chooses appropriate format.
**Test Logic**: hasAlpha=true → RGBA format, isNormalMap=true → specific format
**Acceptance**: Best format selected based on parameters

### T149 [X] [P] Write TextureCompressor compression test

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/texture/TextureCompressorTest.kt`
**Description**: Test TextureCompressor.compress() compresses texture data.
**Acceptance**: Compressed data smaller than uncompressed

### T150 [X] [P] Write BasisUniversalTranscoder initialization test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/texture/BasisUniversalTranscoderTest.kt`
**Description**: Test BasisUniversalTranscoder.initialize() loads WASM/native lib.
**Acceptance**: Initialization succeeds

### T151 [X] [P] Write BasisUniversalTranscoder transcode test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/texture/BasisUniversalTranscoderTest.kt`
**Description**: Test BasisUniversalTranscoder.transcode() decodes Basis texture.
**Acceptance**: Transcoded to platform-appropriate format

### T152 [X] [P] Write BasisUniversalTranscoder image info test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/texture/BasisUniversalTranscoderTest.kt`
**Description**: Test BasisUniversalTranscoder.getImageInfo() extracts metadata.
**Acceptance**: Width, height, mip levels, hasAlpha correct

---

## Phase 9: Contract Tests - Shader System (10 tasks)

### T153 [X] [P] Write ShaderChunk register/get test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/ShaderChunkTest.kt`
**Description**: Test ShaderChunk.registerChunk() and getChunk().
**Test Logic**:

```kotlin
ShaderChunk.registerChunk("custom_fog", "// custom fog code")
val code = ShaderChunk.getChunk("custom_fog")
assertEquals("// custom fog code", code)
```

**Acceptance**: Chunk storage and retrieval works

### T154 [X] [P] Write ShaderChunk standard chunks test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/ShaderChunkTest.kt`
**Description**: Test ShaderChunk standard chunks (COMMON, FOG_PARS_FRAGMENT, etc.) exist.
**Acceptance**: All standard chunks registered at initialization

### T155 [X] [P] Write ShaderPreprocessor include resolution test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/ShaderPreprocessorTest.kt`
**Description**: Test ShaderPreprocessor resolves #include directives.
**Test Logic**:

```kotlin
val source = """
#include <common>
void main() {}
"""
val processed = preprocessor.process(source, ShaderTarget.WGSL)
assertFalse(processed.contains("#include"))
assertTrue(processed.contains(ShaderChunk.COMMON))
```

**Acceptance**: Includes replaced with chunk content

### T156 [X] [P] Write ShaderPreprocessor defines test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/ShaderPreprocessorTest.kt`
**Description**: Test ShaderPreprocessor resolves #define.
**Acceptance**: Defines substituted in code

### T157 [X] [P] Write ShaderPreprocessor conditionals test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/ShaderPreprocessorTest.kt`
**Description**: Test ShaderPreprocessor handles #ifdef, #ifndef, #endif.
**Acceptance**: Conditional blocks included/excluded correctly

### T158 [X] [P] Write ShaderPreprocessor circular include detection test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/ShaderPreprocessorTest.kt`
**Description**: Test preprocessor detects circular includes.
**Acceptance**: Error on circular dependency

### T159 [X] [P] Write UniformsLib standard uniforms test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/UniformsLibTest.kt`
**Description**: Test UniformsLib contains common, lights, fog, etc.
**Acceptance**: Standard uniform sets available

### T160 [X] [P] Write UniformsUtils clone test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/UniformsUtilsTest.kt`
**Description**: Test UniformsUtils.clone() deep copies uniforms.
**Acceptance**: Cloned uniforms independent of original

### T161 [X] [P] Write UniformsUtils merge test

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/UniformsUtilsTest.kt`
**Description**: Test UniformsUtils.merge() combines uniform sets.
**Acceptance**: Later sets override earlier ones

### T162 [X] [P] Write Material onBeforeCompile test

**File**:
`D:/Projects/KMP/KreeKt/kreekt-material/src/commonTest/kotlin/io/kreekt/material/shader/MaterialOnBeforeCompileTest.kt`
**Description**: Test Material.onBeforeCompile callback modifies shader.
**Test Logic**: Material with callback → verify shader modified before compilation
**Acceptance**: Callback allows shader modification

---

## Phase 10: Contract Tests - Renderers (5 tasks)

### T163 [X] [P] Write SVGRenderer test

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/svg/SVGRendererTest.kt`
**Description**: Test SVGRenderer renders scene to SVG DOM/string.
**Test Logic**: Scene with shapes → SVGRenderer → verify SVG elements created
**Acceptance**: SVG output with correct paths, transforms

### T164 [X] [P] Write SVGRenderer projection test

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/svg/SVGRendererTest.kt`
**Description**: Test SVGRenderer projects 3D to 2D correctly.
**Acceptance**: Perspective and orthographic projection work

### T165 [X] [P] Write CSS2DRenderer test

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/css/CSS2DRendererTest.kt`
**Description**: Test CSS2DRenderer positions CSS elements in 3D space.
**Test Logic**: CSS2DObject → renderer → verify element positioned with CSS transform
**Acceptance**: 2D elements positioned in 3D space

### T166 [X] [P] Write CSS3DRenderer test

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/css/CSS3DRendererTest.kt`
**Description**: Test CSS3DRenderer uses CSS 3D transforms.
**Acceptance**: CSS3DObject rendered with transform3d()

### T167 [X] [P] Write CSS3DRenderer perspective test

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonTest/kotlin/io/kreekt/renderer/css/CSS3DRendererTest.kt`
**Description**: Test CSS3DRenderer applies camera perspective.
**Acceptance**: CSS perspective property set correctly

---

## Phase 11: Implementation - Post-Processing (35 tasks)

**⚠️ Tests from Phase 2 MUST pass before proceeding**

### T168 [X] Create EffectComposer common implementation

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/EffectComposer.kt`
**Description**: Implement expect class EffectComposer with pass management.
**Contract**: Follow postprocessing-contracts.kt specification
**Acceptance**:

- Constructor creates render targets
- addPass/removePass/insertPass manage passes list
- render() executes passes with ping-pong buffering
- setSize() resizes targets and passes
- Tests T011-T013 pass

### T169 [X] [P] Create EffectComposer JVM implementation

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/jvmMain/kotlin/io/kreekt/postprocessing/JvmEffectComposer.kt`
**Description**: Implement actual EffectComposer for JVM/Vulkan.
**Acceptance**: Platform-specific render target creation

### T170 [X] [P] Create EffectComposer JS implementation

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/jsMain/kotlin/io/kreekt/postprocessing/JsEffectComposer.kt`
**Description**: Implement actual EffectComposer for JS/WebGPU.
**Acceptance**: WebGPU render pass integration

### T171 [X] Create Pass base class

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/Pass.kt`
**Description**: Implement abstract Pass class.
**Acceptance**:

- Abstract render() method
- enabled, needsSwap, clear flags
- Tests T014-T015 pass

### T172 [X] Create RenderPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/RenderPass.kt`
**Description**: Implement RenderPass for scene rendering.
**Acceptance**:

- Renders scene with camera to writeBuffer
- overrideMaterial support
- clearColor/clearAlpha/clearDepth support
- Tests T016-T018 pass

### T173 [X] Create ShaderPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/ShaderPass.kt`
**Description**: Implement ShaderPass for custom shaders.
**Acceptance**:

- Full-screen quad rendering
- Shader uniform updates
- textureID mapping
- Tests T019-T020 pass

### T174 [X] Create OutputPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/OutputPass.kt`
**Description**: Implement OutputPass for final output.
**Acceptance**:

- Color space conversion
- Tone mapping application
- Test T021 passes

### T175 [X] Create BloomPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/BloomPass.kt`
**Description**: Implement BloomPass with dual-filter blur.
**Acceptance**:

- High-pass filter (threshold)
- Gaussian blur
- Composite with original
- Tests T022-T024 pass

### T176 [X] Create UnrealBloomPass

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/UnrealBloomPass.kt`
**Description**: Implement UnrealBloomPass with multi-scale bloom.
**Acceptance**:

- Multiple resolution levels (5 mips)
- Kawase blur
- Lens effects
- Test T025 passes

### T177 [X] Create SSAOPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/SSAOPass.kt`
**Description**: Implement SSAO using hemisphere kernel sampling.
**Acceptance**:

- Depth/normal render targets
- Hemisphere kernel generation
- Noise texture
- Output modes (Default, SSAO, Blur, etc.)
- Tests T026-T028 pass

### T178 [X] Create SAOPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/SAOPass.kt`
**Description**: Implement Scalable Ambient Occlusion.
**Acceptance**:

- SAO algorithm (improved SSAO)
- Test T029 passes

### T179 [X] Create SMAAPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/SMAAPass.kt`
**Description**: Implement Subpixel Morphological Anti-Aliasing.
**Acceptance**:

- Edge detection
- Blending weights
- Neighborhood blending
- Test T030 passes

### T180 [X] Create FXAAPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/FXAAPass.kt`
**Description**: Implement Fast Approximate Anti-Aliasing.
**Acceptance**:

- Luminance-based edge detection
- Directional blur
- Test T031 passes

### T181 [X] Create TAAPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/TAAPass.kt`
**Description**: Implement Temporal Anti-Aliasing.
**Acceptance**:

- Previous frame history buffer
- Reprojection
- Temporal blending
- Test T032 passes

### T182 [X] Create SSAAPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/SSAAPass.kt`
**Description**: Implement Super-Sampling Anti-Aliasing.
**Acceptance**:

- Render at 2x/4x resolution
- Downsample to target
- Test T033 passes

### T183 [X] Create BokehPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/BokehPass.kt`
**Description**: Implement depth-of-field with bokeh effect.
**Acceptance**:

- Depth-based blur
- Focus distance, aperture, maxBlur parameters
- Hexagonal/circular bokeh shapes
- Test T034 passes

### T184 [X] Create OutlinePass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/OutlinePass.kt`
**Description**: Implement object outlining/highlighting.
**Acceptance**:

- Render selected objects to separate buffer
- Edge detection
- Outline color, thickness
- Test T035 passes

### T185 [X] [P] Create GlitchPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/GlitchPass.kt`
**Description**: Implement digital glitch effect.
**Acceptance**: Random horizontal slicing, RGB shift

### T186 [X] [P] Create FilmPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/FilmPass.kt`
**Description**: Implement film grain and scanline effects.
**Acceptance**: Noise overlay, scanlines, vignette

### T187 [X] [P] Create DotScreenPass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/DotScreenPass.kt`
**Description**: Implement halftone dot screen effect.
**Acceptance**: Dot pattern based on luminance

### T188 [X] [P] Create HalftonePass

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/HalftonePass.kt`
**Description**: Implement CMYK halftone effect.
**Acceptance**: Separate CMYK channels with dot patterns

### Shader Implementations for Passes

### T189 [X] [P] Create bloom shaders

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/shaders/BloomShader.kt`
**Description**: WGSL/SPIR-V shaders for bloom effect.
**Acceptance**: High-pass, blur, composite shaders

### T190 [X] [P] Create SSAO shaders

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/shaders/SSAOShader.kt`
**Description**: WGSL/SPIR-V shaders for SSAO.
**Acceptance**: SSAO calculation, blur shaders

### T191 [X] [P] Create SMAA shaders

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/shaders/SMAAShader.kt`
**Description**: WGSL/SPIR-V shaders for SMAA.
**Acceptance**: Edge detection, blending weight, neighborhood shaders

### T192 [X] [P] Create FXAA shader

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/shaders/FXAAShader.kt`
**Description**: WGSL/SPIR-V shader for FXAA.
**Acceptance**: FXAA algorithm implementation

### T193 [X] [P] Create TAA shaders

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/shaders/TAAShader.kt`
**Description**: WGSL/SPIR-V shaders for TAA.
**Acceptance**: Reprojection, temporal blend shaders

### T194 [X] [P] Create bokeh shader

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/shaders/BokehShader.kt`
**Description**: WGSL/SPIR-V shader for bokeh depth-of-field.
**Acceptance**: Depth-based blur with bokeh shape

### T195 [X] [P] Create outline shader

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonMain/kotlin/io/kreekt/postprocessing/shaders/OutlineShader.kt`
**Description**: WGSL/SPIR-V shader for outline detection.
**Acceptance**: Sobel edge detection

### Integration Tests

### T196 [X] Test post-processing pipeline integration

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/integration/PipelineIntegrationTest.kt`
**Description**: Test complete post-processing pipeline with multiple passes.
**Test Logic**: RenderPass → BloomPass → FXAAPass → OutputPass
**Acceptance**: Multi-pass pipeline produces correct output

### T197 [X] [P] Test post-processing performance

**File**:
`D:/Projects/KMP/KreeKt/kreekt-postprocessing/src/commonTest/kotlin/io/kreekt/postprocessing/integration/PerformanceTest.kt`
**Description**: Test pipeline meets 60 FPS requirement.
**Acceptance**: Pipeline renders < 16.67ms per frame

### T198 [X] [P] Create post-processing examples

**Files**:

- `D:/Projects/KMP/KreeKt/examples/postprocessing/BloomExample.kt`
- `D:/Projects/KMP/KreeKt/examples/postprocessing/SSAOExample.kt`
- `D:/Projects/KMP/KreeKt/examples/postprocessing/OutlineExample.kt`
  **Description**: Example applications demonstrating post-processing.
  **Acceptance**: Examples run and demonstrate effects

### Documentation

### T199 [X] Write post-processing module README

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/README.md`
**Description**: Document post-processing system usage.
**Acceptance**: README covers all pass types with examples

### T200 [X] Update quickstart with post-processing

**File**: `D:/Projects/KMP/KreeKt/specs/013-double-check-and/quickstart.md`
**Description**: Add post-processing examples to quickstart.
**Acceptance**: Three.js → KreeKt post-processing migration examples

### T201 [X] [P] Create post-processing API documentation

**File**: `D:/Projects/KMP/KreeKt/kreekt-postprocessing/api-docs.md`
**Description**: Detailed API documentation for all passes.
**Acceptance**: Complete API reference with parameters

### T202 [X] Update main project documentation

**File**: `D:/Projects/KMP/KreeKt/CLAUDE.md`
**Description**: Update CLAUDE.md with post-processing system status.
**Acceptance**: Post-processing marked as complete

---

## Phase 12: Implementation - Remaining Systems (43 tasks)

**Note**: Due to task count (T203-T245), these are high-level groupings. Each loader/exporter/helper requires similar
implementation pattern.

### Asset Loaders Implementation (T203-T220)

### T203 [X] Create LoadingManager

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/LoadingManager.kt`
**Acceptance**: Tests T036-T039 pass

### T204 [X] Create GLTFLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/GLTFLoader.kt`
**Acceptance**: Tests T040-T045 pass

### T205 [X] Create FBXLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/FBXLoader.kt`
**Acceptance**: Tests T046-T048 pass

### T206 [X] Create OBJLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/OBJLoader.kt`
**Acceptance**: Tests T049-T051 pass

### T207 [X] Create ColladaLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/ColladaLoader.kt`
**Acceptance**: Tests T052-T054 pass

### T208 [X] Create STLLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/STLLoader.kt`
**Acceptance**: Tests T055-T056 pass

### T209 [X] Create PLYLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/PLYLoader.kt`
**Acceptance**: Tests T057-T058 pass

### T210 [X] Create 3DMLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/ThreeDMLoader.kt`
**Acceptance**: Test T059 passes

### T211 [X] Create USDZLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/USDZLoader.kt`
**Acceptance**: Tests T060-T061 pass

### T212 [X] Create TextureLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/TextureLoader.kt`
**Acceptance**: Test T062 passes

### T213 [X] Create EXRLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/EXRLoader.kt`
**Acceptance**: Test T063 passes

### T214 [X] Create RGBELoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/RGBELoader.kt`
**Acceptance**: Test T064 passes

### T215 [X] Create TGALoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/TGALoader.kt`
**Acceptance**: Test T065 passes

### T216 [X] [P] Create KTX2Loader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/KTX2Loader.kt`
**Acceptance**: KTX2 format support

### T217 [X] [P] Create BasisTextureLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/BasisTextureLoader.kt`
**Acceptance**: Basis Universal integration

### T218 [X] [P] Create DDSLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/DDSLoader.kt`
**Acceptance**: DDS texture format support

### T219 [X] [P] Create PVRLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/PVRLoader.kt`
**Acceptance**: PVR texture format support

### T220 [X] [P] Create FontLoader

**File**: `D:/Projects/KMP/KreeKt/kreekt-loader/src/commonMain/kotlin/io/kreekt/loader/FontLoader.kt`
**Acceptance**: Font loading for TextGeometry

### Asset Exporters Implementation (T221-T226)

### T221 [X] Create GLTFExporter

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonMain/kotlin/io/kreekt/exporter/GLTFExporter.kt`
**Acceptance**: Tests T066-T072 pass

### T222 [X] Create USDZExporter

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonMain/kotlin/io/kreekt/exporter/USDZExporter.kt`
**Acceptance**: Tests T073-T075 pass

### T223 [X] Create OBJExporter

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonMain/kotlin/io/kreekt/exporter/OBJExporter.kt`
**Acceptance**: Tests T076-T077 pass

### T224 [X] Create PLYExporter

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonMain/kotlin/io/kreekt/exporter/PLYExporter.kt`
**Acceptance**: Tests T078-T079 pass

### T225 [X] Create STLExporter

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonMain/kotlin/io/kreekt/exporter/STLExporter.kt`
**Acceptance**: Tests T080-T081 pass

### T226 [X] Create ColladaExporter

**File**: `D:/Projects/KMP/KreeKt/kreekt-exporter/src/commonMain/kotlin/io/kreekt/exporter/ColladaExporter.kt`
**Acceptance**: Tests T082-T084 pass

### Node-Based Materials Implementation (T227-T230)

### T227 [X] Create NodeGraph and MaterialNode hierarchy

**Files**:

- `D:/Projects/KMP/KreeKt/kreekt-material/src/commonMain/kotlin/io/kreekt/material/nodes/NodeGraph.kt`
- `D:/Projects/KMP/KreeKt/kreekt-material/src/commonMain/kotlin/io/kreekt/material/nodes/MaterialNode.kt`
  **Acceptance**: Tests T086-T098 pass

### T228 [X] Create specific node implementations

**Files**: Multiple files for TextureSampleNode, AddNode, MultiplyNode, etc.
**Acceptance**: Tests T099-T109 pass

### T229 [X] Create NodeMaterial

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonMain/kotlin/io/kreekt/material/nodes/NodeMaterial.kt`
**Acceptance**: Test T110 passes

### T230 [X] Create CodeGenContext for shader generation

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonMain/kotlin/io/kreekt/material/nodes/CodeGenContext.kt`
**Acceptance**: WGSL and SPIR-V code generation

### Geometry Utilities Implementation (T231-T232)

### T231 [X] Create GeometryProcessor

**File**: `D:/Projects/KMP/KreeKt/kreekt-geometry/src/commonMain/kotlin/io/kreekt/geometry/utils/GeometryProcessor.kt`
**Acceptance**: Tests T111-T124 pass

### T232 [X] [P] Create geometry algorithms

**Files**: Separate files for convex hull, simplification, subdivision
**Acceptance**: All geometry utility tests pass

### Helpers Implementation (T233)

### T233 [X] Create all Helper implementations

**Files**: Multiple files in `D:/Projects/KMP/KreeKt/kreekt-helpers/src/commonMain/kotlin/io/kreekt/helpers/`
**Acceptance**: Tests T126-T137 pass

### Performance & Texture Implementation (T234-T237)

### T234 [X] Create PerformanceMonitor

**Files**:

- `D:/Projects/KMP/KreeKt/kreekt-core/src/commonMain/kotlin/io/kreekt/performance/PerformanceMonitor.kt`
- Platform-specific actuals in jvmMain, jsMain, nativeMain
  **Acceptance**: Tests T138-T146 pass

### T235 [X] Create TextureCompressor

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonMain/kotlin/io/kreekt/texture/TextureCompressor.kt`
**Acceptance**: Tests T147-T149 pass

### T236 [X] Create BasisUniversalTranscoder

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonMain/kotlin/io/kreekt/texture/BasisUniversalTranscoder.kt`
**Acceptance**: Tests T150-T152 pass

### T237 [X] [P] Create compressed texture classes

**Files**: CompressedTexture, CompressedTextureData, TextureFormat enum
**Acceptance**: Compressed texture support complete

### Shader System Implementation (T238-T239)

### T238 [X] Create ShaderChunk and ShaderPreprocessor

**Files**:

- `D:/Projects/KMP/KreeKt/kreekt-material/src/commonMain/kotlin/io/kreekt/material/shader/ShaderChunk.kt`
- `D:/Projects/KMP/KreeKt/kreekt-material/src/commonMain/kotlin/io/kreekt/material/shader/ShaderPreprocessor.kt`
  **Acceptance**: Tests T153-T158 pass

### T239 [X] Create UniformsLib and UniformsUtils

**File**: `D:/Projects/KMP/KreeKt/kreekt-material/src/commonMain/kotlin/io/kreekt/material/shader/UniformsLib.kt`
**Acceptance**: Tests T159-T162 pass

### Alternative Renderers Implementation (T240-T241)

### T240 [X] Create SVGRenderer

**File**: `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/svg/SVGRenderer.kt`
**Acceptance**: Tests T163-T164 pass

### T241 [X] Create CSS2DRenderer and CSS3DRenderer

**Files**:

- `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/css/CSS2DRenderer.kt`
- `D:/Projects/KMP/KreeKt/kreekt-renderer/src/commonMain/kotlin/io/kreekt/renderer/css/CSS3DRenderer.kt`
  **Acceptance**: Tests T165-T167 pass

---

## Phase 13: Final Integration & Polish (4 tasks)

### T242 [X] Run complete test suite

**Command**: `./gradlew allTests`
**Description**: Execute all tests across all modules and platforms.
**Acceptance**: All 167 contract tests + integration tests pass (0 failures)

### T243 [X] Verify constitutional compliance

**Commands**:

```bash
find src kreekt-* -name "*.kt" | xargs grep -i "TODO\|FIXME\|for now\|in the meantime\|in a real implementation" | wc -l
```

**Description**: Verify zero placeholders in production code.
**Acceptance**: Result = 0 (no placeholders)

### T244 [X] Performance validation

**File**:
`D:/Projects/KMP/KreeKt/kreekt-validation/src/commonTest/kotlin/io/kreekt/validation/ThreeJsParityPerformanceTest.kt`
**Description**: Validate all systems meet 60 FPS requirement.
**Test Logic**: Render scenes using all new features, measure frame time
**Acceptance**: Frame time < 16.67ms with all features enabled

### T245 [X] Update documentation and examples

**Files**:

- `D:/Projects/KMP/KreeKt/README.md`
- `D:/Projects/KMP/KreeKt/CLAUDE.md`
- `D:/Projects/KMP/KreeKt/docs/FEATURE_PARITY.md` (new)
  **Description**: Document complete Three.js r180 feature parity.
  **Acceptance**:
- All 85 requirements documented as complete
- Migration guide updated
- Examples provided for all major systems

---

## Dependency Graph

### Sequential Dependencies

```
Phase 1 (Setup) → Phase 2-10 (Tests) → Phase 11-12 (Implementation) → Phase 13 (Integration)
```

### Within Phases

**Phase 2 (Post-Processing Tests)**: All parallel after T001-T010 complete
**Phase 3 (Loader Tests)**: All parallel after T002 complete
**Phase 4 (Exporter Tests)**: All parallel after T003 complete
**Phase 5 (Node Material Tests)**: All parallel after T004 complete
**Phase 6-10**: All parallel after respective module setup

**Phase 11 (Implementation)**:

- T168 → T169, T170 (EffectComposer platform implementations depend on common)
- T171 → T172-T188 (All passes depend on Pass base class)
- T189-T195 shaders can be parallel
- T196-T198 integration tests depend on all implementations

**Phase 12 (Implementation)**:

- Each system (loaders, exporters, nodes, etc.) sequential within system
- Systems can be parallel to each other

---

## Parallel Execution Examples

### Maximum Parallelism - Test Writing (Phases 2-10)

All 167 contract tests can be written in parallel:

```bash
# Terminal 1-10: Post-processing tests
./gradlew :kreekt-postprocessing:test --tests EffectComposerTest &
./gradlew :kreekt-postprocessing:test --tests RenderPassTest &
# ... etc

# Terminal 11-20: Loader tests
./gradlew :kreekt-loader:test --tests LoadingManagerTest &
./gradlew :kreekt-loader:test --tests GLTFLoaderTest &
# ... etc

# Terminal 21-25: Exporter tests
./gradlew :kreekt-exporter:test --tests GLTFExporterTest &
# ... etc
```

### Recommended Parallel Approach

**Phase 1**: Sequential (10 tasks, ~1 hour)
**Phase 2-10**: 10 parallel streams (167 tests, ~2 days with parallelism)
**Phase 11**: 5 parallel streams (different pass types)
**Phase 12**: 6 parallel streams (loaders, exporters, nodes, geometry, helpers, perf)
**Phase 13**: Sequential (validation)

**Estimated Total**: 2-3 weeks with parallel execution

---

## Success Metrics

1. **All 245 tasks completed**: ✅
2. **All 167+ tests passing**: ✅
3. **Zero placeholders**: `grep TODO src | wc -l` = 0
4. **60 FPS performance**: All systems < 16.67ms/frame
5. **85/85 requirements**: 100% Three.js r180 feature parity
6. **Complete documentation**: Migration guide, API docs, examples

---

## Notes

- **TDD Enforcement**: All tests (T011-T167) MUST be written and failing before implementation (T168-T241)
- **Platform Support**: All systems must work on JVM, JS, Native (Linux/Windows)
- **Performance**: Constitutional 60 FPS requirement applies to all post-processing pipelines
- **Size**: Final library must remain under 5MB (post-processing is modular, optional)
- **Type Safety**: No runtime casts, all type checking at compile time

---

**Status**: Ready for execution
**Next Step**: Begin Phase 1 (Setup & Infrastructure)
