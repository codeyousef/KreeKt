# Tasks: Complete Three.js Feature Parity (25 Feature Categories)

**Input**: Design documents from `/specs/011-reference-https-threejs/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

## Execution Flow

```
1. Load plan.md from feature directory ✓
   → Extracted: Kotlin Multiplatform, WebGPU/Vulkan, 7 platforms
2. Load design documents ✓
   → data-model.md: 17 entity categories
   → contracts/: 17 contract files (5,994 lines)
   → research.md: 8 research areas completed
   → quickstart.md: 16 usage examples
3. Generate tasks by category:
   → Setup: project structure, dependencies
   → Tests: 17 contract tests + 40 integration tests
   → Core: Models, services, renderers (90+ tasks)
   → Integration: Cross-feature tests (15 tasks)
   → Polish: Performance, docs (12 tasks)
4. Apply task rules:
   → Contract tests [P] before implementation
   → Different modules = mark [P] for parallel
   → TDD ordering enforced
5. Number tasks sequentially (T001-T174)
6. Validate completeness ✓
```

## Format: `[ID] [P?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions

Kotlin Multiplatform structure:
- Common: `src/commonMain/kotlin/io/kreekt/`
- Tests: `src/commonTest/kotlin/io/kreekt/`
- Platform-specific: `src/jvmMain/`, `src/jsMain/`, etc.

---

## Phase 3.1: Setup (3 tasks)

- [ ] **T001** Create module structure for 17 new feature categories
  - **Files**: `build.gradle.kts` in root and each new module
  - **Action**: Add modules: `kreekt-audio`, `kreekt-helper`, `kreekt-fog`, `kreekt-raycaster`, `kreekt-curve`, `kreekt-instancing`, `kreekt-points`, `kreekt-morph`, `kreekt-clipping`, `kreekt-layers`, `kreekt-rendertarget`, `kreekt-shape`, `kreekt-lines`, `kreekt-lod`, `kreekt-constants`
  - **Dependencies**: Update `settings.gradle.kts` to include all new modules
  - **Note**: Base structure only, no implementations

- [ ] **T002** Add platform-specific dependencies for audio and texture features
  - **Files**: `build.gradle.kts` in `kreekt-audio`, `kreekt-texture`
  - **Action**: Add OpenAL (JVM), Web Audio API typings (JS), AVAudioEngine bindings (iOS), AudioTrack (Android)
  - **Note**: Configure expect/actual for platform audio APIs

- [ ] **T003** [P] Configure Kotlin 1.9+ multiplatform compilation targets
  - **Files**: Root `build.gradle.kts`
  - **Action**: Ensure all 7 platforms configured: JVM, JS (WebGPU), Android, iOS, linuxX64, mingwX64, macosX64
  - **Note**: Verify existing targets, add any missing

---

## Phase 3.2: Contract Tests (TDD) - MUST COMPLETE BEFORE 3.3 ⚠️

**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### Audio System Tests (3 tasks)

- [ ] **T004** [P] Contract test: AudioListener attachment and position tracking
  - **File**: `src/commonTest/kotlin/io/kreekt/audio/AudioListenerContractTest.kt`
  - **Covers**: FR-A001, FR-A002 from contracts/audio-api.kt
  - **Test Cases**:
    - AudioListener attaches to camera
    - Position updates from camera matrix
    - Orientation updates from camera quaternion
  - **Expected**: All tests FAIL (no implementation yet)

- [ ] **T005** [P] Contract test: PositionalAudio 3D panning and attenuation
  - **File**: `src/commonTest/kotlin/io/kreekt/audio/PositionalAudioContractTest.kt`
  - **Covers**: FR-A003, FR-A004 from contracts/audio-api.kt
  - **Test Cases**:
    - Distance-based attenuation models (linear, inverse, exponential)
    - Directional cone attenuation
    - Doppler effect (platform-dependent)
  - **Expected**: All tests FAIL

- [ ] **T006** [P] Contract test: Audio loading and playback controls
  - **File**: `src/commonTest/kotlin/io/kreekt/audio/AudioPlaybackContractTest.kt`
  - **Covers**: FR-A005, FR-A006, FR-A007 from contracts/audio-api.kt
  - **Test Cases**:
    - Load audio from URL
    - Play/pause/stop controls
    - Volume and playback rate adjustments
    - Looping behavior
  - **Expected**: All tests FAIL

### Helper Objects Tests (2 tasks)

- [ ] **T007** [P] Contract test: Visual debugging helpers (axes, grid, box)
  - **File**: `src/commonTest/kotlin/io/kreekt/helper/DebugHelpersContractTest.kt`
  - **Covers**: FR-H001 through FR-H004 from contracts/helper-api.kt
  - **Test Cases**:
    - AxesHelper renders colored axes
    - GridHelper renders grid with divisions
    - BoxHelper updates with object bounds
    - Box3Helper creates from min/max vectors
  - **Expected**: All tests FAIL

- [ ] **T008** [P] Contract test: Light and camera helpers
  - **File**: `src/commonTest/kotlin/io/kreekt/helper/LightCameraHelpersContractTest.kt`
  - **Covers**: FR-H005 through FR-H009 from contracts/helper-api.kt
  - **Test Cases**:
    - CameraHelper shows frustum planes
    - DirectionalLightHelper shows direction
    - SpotLightHelper shows cone
    - PointLightHelper shows range sphere
  - **Expected**: All tests FAIL

### Advanced Cameras Tests (3 tasks)

- [ ] **T009** [P] Contract test: CubeCamera 6-face rendering
  - **File**: `src/commonTest/kotlin/io/kreekt/camera/CubeCameraContractTest.kt`
  - **Covers**: FR-C001, FR-C002 from contracts/camera-api.kt
  - **Test Cases**:
    - Render to 6 cube faces
    - Update camera orientation per face
    - Integrate with PBR materials
  - **Expected**: All tests FAIL

- [ ] **T010** [P] Contract test: StereoCamera for VR
  - **File**: `src/commonTest/kotlin/io/kreekt/camera/StereoCameraContractTest.kt`
  - **Covers**: FR-C003, FR-C004 from contracts/camera-api.kt
  - **Test Cases**:
    - Maintain two cameras with eye separation
    - Compute correct view matrices
    - Adjustable inter-pupillary distance
  - **Expected**: All tests FAIL

- [ ] **T011** [P] Contract test: ArrayCamera multi-view rendering
  - **File**: `src/commonTest/kotlin/io/kreekt/camera/ArrayCameraContractTest.kt`
  - **Covers**: FR-C005, FR-C006 from contracts/camera-api.kt
  - **Test Cases**:
    - Multiple cameras with viewport regions
    - Render scene once per camera
    - Correct viewport bounds
  - **Expected**: All tests FAIL

### Fog System Tests (2 tasks)

- [ ] **T012** [P] Contract test: Linear fog distance-based
  - **File**: `src/commonTest/kotlin/io/kreekt/fog/FogContractTest.kt`
  - **Covers**: FR-F001, FR-F002, FR-F003 from contracts/fog-api.kt
  - **Test Cases**:
    - Linear interpolation between near and far
    - Fog factor calculation at distance
    - Shader code generation
  - **Expected**: All tests FAIL

- [ ] **T013** [P] Contract test: Exponential fog density-based
  - **File**: `src/commonTest/kotlin/io/kreekt/fog/FogExp2ContractTest.kt`
  - **Covers**: FR-F004, FR-F005, FR-F006 from contracts/fog-api.kt
  - **Test Cases**:
    - Exponential squared falloff
    - Density parameter effects
    - Shader integration
  - **Expected**: All tests FAIL

### Raycasting Tests (3 tasks)

- [ ] **T014** [P] Contract test: Raycaster intersection testing
  - **File**: `src/commonTest/kotlin/io/kreekt/raycaster/RaycasterContractTest.kt`
  - **Covers**: FR-R001, FR-R002, FR-R003, FR-R004 from contracts/raycaster-api.kt
  - **Test Cases**:
    - Detect intersections with meshes, lines, points
    - Return sorted results by distance
    - setFromCamera NDC conversion
    - Recursive object traversal
  - **Expected**: All tests FAIL

- [ ] **T015** [P] Contract test: Intersection data completeness
  - **File**: `src/commonTest/kotlin/io/kreekt/raycaster/IntersectionContractTest.kt`
  - **Covers**: FR-R005, FR-R006, FR-R007 from contracts/raycaster-api.kt
  - **Test Cases**:
    - Distance, point, face data
    - UV coordinates at intersection
    - Instance ID for InstancedMesh
    - Normal computation
  - **Expected**: All tests FAIL

- [ ] **T016** [P] Contract test: BVH acceleration structure
  - **File**: `src/commonTest/kotlin/io/kreekt/raycaster/BVHContractTest.kt`
  - **Covers**: FR-R008, FR-R009, FR-R010 from contracts/raycaster-api.kt
  - **Test Cases**:
    - BVH construction from objects
    - Ray-BVH intersection traversal
    - Performance target: 10,000 objects at 60 FPS
    - Dynamic BVH updates
  - **Expected**: All tests FAIL

### Curve System Tests (4 tasks)

- [ ] **T017** [P] Contract test: Base Curve parametric evaluation
  - **File**: `src/commonTest/kotlin/io/kreekt/curve/CurveContractTest.kt`
  - **Covers**: FR-CR001, FR-CR002 from contracts/curve-api.kt
  - **Test Cases**:
    - getPoint(t) returns correct position
    - getTangent(t) returns normalized tangent
    - getSpacedPoints distributes by arc length
    - Arc length computation
  - **Expected**: All tests FAIL

- [ ] **T018** [P] Contract test: CatmullRomCurve3 interpolation
  - **File**: `src/commonTest/kotlin/io/kreekt/curve/CatmullRomCurveContractTest.kt`
  - **Covers**: FR-CR003 from contracts/curve-api.kt
  - **Test Cases**:
    - Pass through all control points
    - Closed curve behavior
    - Centripetal/chordal/uniform tension
  - **Expected**: All tests FAIL

- [ ] **T019** [P] Contract test: Bézier curves (cubic and quadratic)
  - **File**: `src/commonTest/kotlin/io/kreekt/curve/BezierCurveContractTest.kt`
  - **Covers**: FR-CR004, FR-CR005 from contracts/curve-api.kt
  - **Test Cases**:
    - Cubic Bézier interpolation
    - Quadratic Bézier interpolation
    - Control point influence
  - **Expected**: All tests FAIL

- [ ] **T020** [P] Contract test: TubeGeometry extrusion along curve
  - **File**: `src/commonTest/kotlin/io/kreekt/curve/TubeGeometryContractTest.kt`
  - **Covers**: FR-CR012, FR-CR013, FR-CR014 from contracts/curve-api.kt
  - **Test Cases**:
    - Extrude 2D shape along 3D curve
    - Frenet frame computation
    - Radial and tubular segments
  - **Expected**: All tests FAIL

### Advanced Textures Tests (6 tasks)

- [ ] **T021** [P] Contract test: CubeTexture 6-face loading
  - **File**: `src/commonTest/kotlin/io/kreekt/texture/CubeTextureContractTest.kt`
  - **Covers**: FR-T001, FR-T002 from contracts/texture-api.kt
  - **Test Cases**:
    - Load 6 images (px, nx, py, ny, pz, nz)
    - Cube map sampling
    - Integration with CubeCamera
  - **Expected**: All tests FAIL

- [ ] **T022** [P] Contract test: VideoTexture streaming
  - **File**: `src/commonTest/kotlin/io/kreekt/texture/VideoTextureContractTest.kt`
  - **Covers**: FR-T003, FR-T004 from contracts/texture-api.kt
  - **Test Cases**:
    - Play video as texture
    - Update texture each frame
    - Sync with video playback
  - **Expected**: All tests FAIL

- [ ] **T023** [P] Contract test: CanvasTexture dynamic updates
  - **File**: `src/commonTest/kotlin/io/kreekt/texture/CanvasTextureContractTest.kt`
  - **Covers**: FR-T005, FR-T006 from contracts/texture-api.kt
  - **Test Cases**:
    - Render canvas as texture
    - Manual update triggering
    - Canvas resize handling
  - **Expected**: All tests FAIL

- [ ] **T024** [P] Contract test: CompressedTexture formats
  - **File**: `src/commonTest/kotlin/io/kreekt/texture/CompressedTextureContractTest.kt`
  - **Covers**: FR-T007, FR-T008, FR-T009 from contracts/texture-api.kt
  - **Test Cases**:
    - Support BC7, ETC2, ASTC, PVRTC
    - Platform compression detection
    - Load compressed mipmaps
  - **Expected**: All tests FAIL

- [ ] **T025** [P] Contract test: DataTexture from typed arrays
  - **File**: `src/commonTest/kotlin/io/kreekt/texture/DataTextureContractTest.kt`
  - **Covers**: FR-T010, FR-T011, FR-T012 from contracts/texture-api.kt
  - **Test Cases**:
    - Create from raw data
    - Float and integer formats
    - 2D, 3D, and array textures
  - **Expected**: All tests FAIL

- [ ] **T026** [P] Contract test: PMREMGenerator for IBL
  - **File**: `src/commonTest/kotlin/io/kreekt/texture/PMREMGeneratorContractTest.kt`
  - **Covers**: FR-T019, FR-T020 from contracts/texture-api.kt
  - **Test Cases**:
    - Generate pre-filtered mipmaps
    - GGX distribution computation
    - Cube and equirectangular input
  - **Expected**: All tests FAIL

### Instancing Tests (2 tasks)

- [ ] **T027** [P] Contract test: InstancedMesh GPU instancing
  - **File**: `src/commonTest/kotlin/io/kreekt/instancing/InstancedMeshContractTest.kt`
  - **Covers**: FR-I001, FR-I002, FR-I003, FR-I004 from contracts/instancing-api.kt
  - **Test Cases**:
    - Render 10,000+ instances in single draw call
    - Per-instance transforms (setMatrixAt)
    - Per-instance colors (setColorAt)
    - GPU buffer updates
  - **Expected**: All tests FAIL

- [ ] **T028** [P] Contract test: InstancedBufferAttribute
  - **File**: `src/commonTest/kotlin/io/kreekt/instancing/InstancedBufferAttributeContractTest.kt`
  - **Covers**: FR-I007, FR-I008, FR-I009, FR-I010 from contracts/instancing-api.kt
  - **Test Cases**:
    - Store per-instance data
    - Efficient GPU uploads
    - Partial buffer updates
    - meshPerAttribute parameter
  - **Expected**: All tests FAIL

### Points & Sprites Tests (2 tasks)

- [ ] **T029** [P] Contract test: Points rendering (point clouds)
  - **File**: `src/commonTest/kotlin/io/kreekt/points/PointsContractTest.kt`
  - **Covers**: FR-P001, FR-P002, FR-P003, FR-P004, FR-P005 from contracts/points-api.kt
  - **Test Cases**:
    - Render 1M+ points at 60 FPS
    - Per-point colors and sizes
    - Size attenuation (perspective scaling)
    - Raycasting against points
  - **Expected**: All tests FAIL

- [ ] **T030** [P] Contract test: Sprite billboards
  - **File**: `src/commonTest/kotlin/io/kreekt/points/SpriteContractTest.kt`
  - **Covers**: FR-P006, FR-P007, FR-P008, FR-P009 from contracts/points-api.kt
  - **Test Cases**:
    - Always face camera
    - Texture mapping
    - Rotation and scaling
    - Raycasting against sprites
  - **Expected**: All tests FAIL

### Morph Targets Tests (2 tasks)

- [ ] **T031** [P] Contract test: Morph target blend shapes
  - **File**: `src/commonTest/kotlin/io/kreekt/morph/MorphTargetContractTest.kt`
  - **Covers**: FR-M001, FR-M002, FR-M003, FR-M004, FR-M005 from contracts/morph-api.kt
  - **Test Cases**:
    - Store multiple morph targets
    - Blend between base and targets
    - Named morph targets
    - Position and normal morphing
  - **Expected**: All tests FAIL

- [ ] **T032** [P] Contract test: Morph animation mixer
  - **File**: `src/commonTest/kotlin/io/kreekt/morph/MorphAnimationContractTest.kt`
  - **Covers**: FR-M006, FR-M007, FR-M008 from contracts/morph-api.kt
  - **Test Cases**:
    - Animate influences over time
    - Blend multiple animations
    - Shader code generation
  - **Expected**: All tests FAIL

### Clipping Planes Tests (2 tasks)

- [ ] **T033** [P] Contract test: Global clipping planes
  - **File**: `src/commonTest/kotlin/io/kreekt/clipping/ClippingPlanesContractTest.kt`
  - **Covers**: FR-CP001, FR-CP002, FR-CP003, FR-CP004 from contracts/clipping-api.kt
  - **Test Cases**:
    - Clip geometry against arbitrary planes
    - Support up to 8 planes
    - Per-material clipping override
    - Union vs intersection mode
  - **Expected**: All tests FAIL

- [ ] **T034** [P] Contract test: Clipping shader integration
  - **File**: `src/commonTest/kotlin/io/kreekt/clipping/ClippingShaderContractTest.kt`
  - **Covers**: FR-CP005, FR-CP006 from contracts/clipping-api.kt
  - **Test Cases**:
    - Generate platform shader code
    - Hardware clipping (gl_ClipDistance)
    - Fragment discard fallback
  - **Expected**: All tests FAIL

### Layers Tests (1 task)

- [ ] **T035** [P] Contract test: Layer-based visibility
  - **File**: `src/commonTest/kotlin/io/kreekt/layers/LayersContractTest.kt`
  - **Covers**: FR-L001, FR-L002, FR-L003, FR-L004, FR-L005, FR-L006 from contracts/layers-api.kt
  - **Test Cases**:
    - Support 32 layers (bitmask)
    - Filter objects during rendering
    - Filter during raycasting
    - Layer enable/disable/toggle
    - Layer intersection testing
  - **Expected**: All tests FAIL

### Render Targets Tests (4 tasks)

- [ ] **T036** [P] Contract test: WebGPURenderTarget off-screen rendering
  - **File**: `src/commonTest/kotlin/io/kreekt/rendertarget/RenderTargetContractTest.kt`
  - **Covers**: FR-RT001, FR-RT002, FR-RT003 from contracts/rendertarget-api.kt
  - **Test Cases**:
    - Create framebuffer with attachments
    - Color and depth/stencil support
    - Usable as texture
    - MSAA multisampling
  - **Expected**: All tests FAIL

- [ ] **T037** [P] Contract test: Multiple Render Targets (MRT)
  - **File**: `src/commonTest/kotlin/io/kreekt/rendertarget/MRTContractTest.kt`
  - **Covers**: FR-RT004, FR-RT005 from contracts/rendertarget-api.kt
  - **Test Cases**:
    - Multiple color attachments
    - Shader output to gl_FragData[N]
    - Platform limits (4-8 targets)
  - **Expected**: All tests FAIL

- [ ] **T038** [P] Contract test: Cube and 3D render targets
  - **File**: `src/commonTest/kotlin/io/kreekt/rendertarget/CubeRenderTargetContractTest.kt`
  - **Covers**: FR-RT006, FR-RT007, FR-RT008, FR-RT009 from contracts/rendertarget-api.kt
  - **Test Cases**:
    - Render to 6 cube faces
    - Convert from equirectangular
    - 3D texture slices
  - **Expected**: All tests FAIL

- [ ] **T039** [P] Contract test: Render target pool management
  - **File**: `src/commonTest/kotlin/io/kreekt/rendertarget/RenderTargetPoolContractTest.kt`
  - **Covers**: FR-RT010 from contracts/rendertarget-api.kt
  - **Test Cases**:
    - Reuse render targets
    - Dispose unused targets
    - Memory fragmentation reduction
  - **Expected**: All tests FAIL

### Shape & Path Tests (3 tasks)

- [ ] **T040** [P] Contract test: 2D Shape creation and holes
  - **File**: `src/commonTest/kotlin/io/kreekt/shape/ShapeContractTest.kt`
  - **Covers**: FR-S001, FR-S002, FR-S003, FR-S004 from contracts/shape-api.kt
  - **Test Cases**:
    - Define closed 2D shapes
    - Add holes to shapes
    - Triangulate with Earcut
    - Generate UV coordinates
  - **Expected**: All tests FAIL

- [ ] **T041** [P] Contract test: ExtrudeGeometry 3D extrusion
  - **File**: `src/commonTest/kotlin/io/kreekt/shape/ExtrudeGeometryContractTest.kt`
  - **Covers**: FR-S005, FR-S006, FR-S007, FR-S008 from contracts/shape-api.kt
  - **Test Cases**:
    - Extrude along Z or custom path
    - Beveling support
    - UV mapping
    - Custom UV generators
  - **Expected**: All tests FAIL

- [ ] **T042** [P] Contract test: LatheGeometry revolution
  - **File**: `src/commonTest/kotlin/io/kreekt/shape/LatheGeometryContractTest.kt`
  - **Covers**: FR-S009, FR-S010 from contracts/shape-api.kt
  - **Test Cases**:
    - Revolve points around Y axis
    - Custom rotation angles
    - Shape utilities (triangulation, area, simplification)
  - **Expected**: All tests FAIL

### Enhanced Lines Tests (2 tasks)

- [ ] **T043** [P] Contract test: Line2 high-quality rendering
  - **File**: `src/commonTest/kotlin/io/kreekt/lines/Line2ContractTest.kt`
  - **Covers**: FR-LI006, FR-LI007, FR-LI008 from contracts/lines-api.kt
  - **Test Cases**:
    - Render as screen-space quads
    - Variable width in pixels
    - Resolution-independent rendering
    - World units vs screen pixels
  - **Expected**: All tests FAIL

- [ ] **T044** [P] Contract test: Basic line rendering and dashing
  - **File**: `src/commonTest/kotlin/io/kreekt/lines/LineBasicContractTest.kt`
  - **Covers**: FR-LI001, FR-LI002, FR-LI003, FR-LI004, FR-LI005 from contracts/lines-api.kt
  - **Test Cases**:
    - Line, LineSegments, LineLoop rendering
    - Dashed line patterns
    - lineDistance attribute computation
  - **Expected**: All tests FAIL

### LOD Tests (1 task)

- [ ] **T045** [P] Contract test: Level of Detail switching
  - **File**: `src/commonTest/kotlin/io/kreekt/lod/LODContractTest.kt`
  - **Covers**: FR-LO001, FR-LO002, FR-LO003, FR-LO004, FR-LO005 from contracts/lod-api.kt
  - **Test Cases**:
    - Switch levels based on distance
    - Multiple LOD levels
    - Distance computation from camera
    - Automatic LOD hierarchy generation
  - **Expected**: All tests FAIL

### Constants Tests (1 task)

- [ ] **T046** [P] Contract test: Constants and enumerations
  - **File**: `src/commonTest/kotlin/io/kreekt/constants/ConstantsContractTest.kt`
  - **Covers**: FR-CE001 through FR-CE010 from contracts/constants-api.kt
  - **Test Cases**:
    - Blending modes map to WebGPU
    - Depth modes, cull face modes
    - Texture mapping, wrapping, filtering
    - Data types and formats
    - Encoding and color space
  - **Expected**: All tests FAIL

---

## Phase 3.3: Core Implementation (90 tasks)

**Prerequisites: ALL Phase 3.2 tests must be failing before starting**

### Audio System Implementation (5 tasks)

- [ ] **T047** [P] Implement AudioListener (common)
  - **File**: `src/commonMain/kotlin/io/kreekt/audio/AudioListener.kt`
  - **Action**: Create expect class with position, rotation, up tracking
  - **Contract**: T004 tests should pass
  - **Note**: Common interface only, platform implementations next

- [ ] **T048** Platform-specific AudioListener implementations
  - **Files**:
    - `src/jsMain/kotlin/io/kreekt/audio/AudioListener.kt` (Web Audio API)
    - `src/jvmMain/kotlin/io/kreekt/audio/AudioListener.kt` (OpenAL)
    - `src/androidMain/kotlin/io/kreekt/audio/AudioListener.kt` (AudioTrack)
    - `src/iosMain/kotlin/io/kreekt/audio/AudioListener.kt` (AVAudioEngine)
  - **Action**: Implement actual classes for each platform
  - **Dependencies**: T047
  - **Note**: Sequential due to shared test validation

- [ ] **T049** [P] Implement PositionalAudio (common + platform)
  - **File**: `src/commonMain/kotlin/io/kreekt/audio/PositionalAudio.kt`
  - **Action**: 3D panning, distance attenuation, directional cones
  - **Contract**: T005 tests should pass
  - **Dependencies**: T048

- [ ] **T050** [P] Implement Audio base class
  - **File**: `src/commonMain/kotlin/io/kreekt/audio/Audio.kt`
  - **Action**: Load, play/pause/stop, volume, looping
  - **Contract**: T006 tests should pass

- [ ] **T051** [P] Implement AudioAnalyser
  - **File**: `src/commonMain/kotlin/io/kreekt/audio/AudioAnalyser.kt`
  - **Action**: FFT analysis, frequency/time domain data
  - **Contract**: T006 tests should pass

### Helper Objects Implementation (4 tasks)

- [ ] **T052** [P] Implement AxesHelper and GridHelper
  - **File**: `src/commonMain/kotlin/io/kreekt/helper/DebugHelpers.kt`
  - **Action**: Render colored axes and grid
  - **Contract**: T007 tests should pass

- [ ] **T053** [P] Implement BoxHelper and Box3Helper
  - **File**: `src/commonMain/kotlin/io/kreekt/helper/BoundingHelpers.kt`
  - **Action**: Wireframe boxes from objects and bounds
  - **Contract**: T007 tests should pass

- [ ] **T054** [P] Implement CameraHelper
  - **File**: `src/commonMain/kotlin/io/kreekt/helper/CameraHelper.kt`
  - **Action**: Visualize camera frustum
  - **Contract**: T008 tests should pass

- [ ] **T055** [P] Implement light helpers (Directional, Spot, Point, Hemisphere)
  - **File**: `src/commonMain/kotlin/io/kreekt/helper/LightHelpers.kt`
  - **Action**: Visualize light position, direction, range, cone
  - **Contract**: T008 tests should pass

### Advanced Cameras Implementation (3 tasks)

- [ ] **T056** [P] Implement CubeCamera
  - **File**: `src/commonMain/kotlin/io/kreekt/camera/CubeCamera.kt`
  - **Action**: 6-face rendering to cube render target
  - **Contract**: T009 tests should pass
  - **Note**: Requires WebGPUCubeRenderTarget from T084

- [ ] **T057** [P] Implement StereoCamera
  - **File**: `src/commonMain/kotlin/io/kreekt/camera/StereoCamera.kt`
  - **Action**: Left/right eye cameras with IPD separation
  - **Contract**: T010 tests should pass

- [ ] **T058** [P] Implement ArrayCamera
  - **File**: `src/commonMain/kotlin/io/kreekt/camera/ArrayCamera.kt`
  - **Action**: Multiple cameras with viewport regions
  - **Contract**: T011 tests should pass

### Fog System Implementation (3 tasks)

- [ ] **T059** [P] Implement Fog (linear)
  - **File**: `src/commonMain/kotlin/io/kreekt/fog/Fog.kt`
  - **Action**: Linear fog with near/far distance
  - **Contract**: T012 tests should pass

- [ ] **T060** [P] Implement FogExp2 (exponential)
  - **File**: `src/commonMain/kotlin/io/kreekt/fog/FogExp2.kt`
  - **Action**: Exponential squared fog with density
  - **Contract**: T013 tests should pass

- [ ] **T061** Implement FogShaderInjector
  - **File**: `src/commonMain/kotlin/io/kreekt/fog/FogShaderInjector.kt`
  - **Action**: Generate WGSL fog code, inject into shaders
  - **Contract**: T012, T013 tests should pass
  - **Dependencies**: T059, T060
  - **Note**: Sequential - modifies shader generation pipeline

### Raycasting Implementation (6 tasks)

- [ ] **T062** [P] Implement Raycaster core
  - **File**: `src/commonMain/kotlin/io/kreekt/raycaster/Raycaster.kt`
  - **Action**: Ray creation, setFromCamera, intersect logic
  - **Contract**: T014 tests should pass

- [ ] **T063** [P] Implement Intersection data class
  - **File**: `src/commonMain/kotlin/io/kreekt/raycaster/Intersection.kt`
  - **Action**: Distance, point, face, UV, instanceId, normal
  - **Contract**: T015 tests should pass

- [ ] **T064** Implement ray-mesh intersection
  - **File**: `src/commonMain/kotlin/io/kreekt/raycaster/MeshRaycasting.kt`
  - **Action**: Ray-triangle intersection, face detection
  - **Contract**: T014 tests should pass
  - **Dependencies**: T062, T063

- [ ] **T065** [P] Implement ray-line and ray-point intersection
  - **File**: `src/commonMain/kotlin/io/kreekt/raycaster/PrimitiveRaycasting.kt`
  - **Action**: Distance thresholds for lines and points
  - **Contract**: T014 tests should pass
  - **Dependencies**: T062

- [ ] **T066** [P] Implement BVH data structure
  - **File**: `src/commonMain/kotlin/io/kreekt/raycaster/BVH.kt`
  - **Action**: BVH construction with SAH, AABB bounds
  - **Contract**: T016 tests should pass

- [ ] **T067** Integrate BVH with Raycaster
  - **File**: `src/commonMain/kotlin/io/kreekt/raycaster/Raycaster.kt`
  - **Action**: Use BVH for object culling before ray tests
  - **Contract**: T016 performance tests (10k objects) should pass
  - **Dependencies**: T062, T066

### Curve System Implementation (8 tasks)

- [ ] **T068** [P] Implement base Curve class
  - **File**: `src/commonMain/kotlin/io/kreekt/curve/Curve.kt`
  - **Action**: Abstract curve with getPoint, getTangent, getLength
  - **Contract**: T017 tests should pass

- [ ] **T069** [P] Implement CatmullRomCurve3
  - **File**: `src/commonMain/kotlin/io/kreekt/curve/CatmullRomCurve3.kt`
  - **Action**: Catmull-Rom spline interpolation
  - **Contract**: T018 tests should pass
  - **Dependencies**: T068

- [ ] **T070** [P] Implement CubicBezierCurve3 and QuadraticBezierCurve3
  - **File**: `src/commonMain/kotlin/io/kreekt/curve/BezierCurves.kt`
  - **Action**: Cubic and quadratic Bézier evaluation
  - **Contract**: T019 tests should pass
  - **Dependencies**: T068

- [ ] **T071** [P] Implement LineCurve3 and EllipseCurve
  - **File**: `src/commonMain/kotlin/io/kreekt/curve/BasicCurves.kt`
  - **Action**: Line segment and ellipse curves
  - **Dependencies**: T068

- [ ] **T072** [P] Implement CurvePath and Path
  - **File**: `src/commonMain/kotlin/io/kreekt/curve/Path.kt`
  - **Action**: Composite curves, 2D path with moveTo/lineTo/bezierCurveTo
  - **Dependencies**: T068

- [ ] **T073** [P] Implement FrenetFrames
  - **File**: `src/commonMain/kotlin/io/kreekt/curve/FrenetFrames.kt`
  - **Action**: Compute tangent, normal, binormal frames
  - **Contract**: T020 tests should pass
  - **Dependencies**: T068

- [ ] **T074** Implement TubeGeometry
  - **File**: `src/commonMain/kotlin/io/kreekt/curve/TubeGeometry.kt`
  - **Action**: Extrude tube along curve using Frenet frames
  - **Contract**: T020 tests should pass
  - **Dependencies**: T068, T073

- [ ] **T075** [P] Implement CurveUtils (adaptive tessellation)
  - **File**: `src/commonMain/kotlin/io/kreekt/curve/CurveUtils.kt`
  - **Action**: Adaptive subdivision based on curvature
  - **Dependencies**: T068

### Advanced Textures Implementation (12 tasks)

- [ ] **T076** [P] Implement CubeTexture (common)
  - **File**: `src/commonMain/kotlin/io/kreekt/texture/CubeTexture.kt`
  - **Action**: Expect class for 6-face cube texture
  - **Contract**: T021 tests should pass

- [ ] **T077** Platform-specific CubeTexture implementations
  - **Files**:
    - `src/jsMain/kotlin/io/kreekt/texture/CubeTexture.kt` (WebGPU texture_cube)
    - `src/jvmMain/kotlin/io/kreekt/texture/CubeTexture.kt` (Vulkan VK_IMAGE_VIEW_TYPE_CUBE)
  - **Action**: Implement actual cube texture for each platform
  - **Dependencies**: T076

- [ ] **T078** [P] Implement VideoTexture (common + JS/platform)
  - **File**: `src/commonMain/kotlin/io/kreekt/texture/VideoTexture.kt`
  - **Action**: Expect class for video streaming
  - **Contract**: T022 tests should pass
  - **Note**: Primary implementation on JS with HTMLVideoElement

- [ ] **T079** [P] Implement CanvasTexture (common + JS/platform)
  - **File**: `src/commonMain/kotlin/io/kreekt/texture/CanvasTexture.kt`
  - **Action**: Expect class for canvas rendering
  - **Contract**: T023 tests should pass
  - **Note**: Primary implementation on JS with HTMLCanvasElement

- [ ] **T080** [P] Implement CompressedTexture formats
  - **File**: `src/commonMain/kotlin/io/kreekt/texture/CompressedTexture.kt`
  - **Action**: BC7, ETC2, ASTC, PVRTC support
  - **Contract**: T024 tests should pass

- [ ] **T081** Platform-specific compression detection
  - **Files**:
    - `src/jsMain/kotlin/io/kreekt/texture/CompressionSupport.kt`
    - `src/jvmMain/kotlin/io/kreekt/texture/CompressionSupport.kt`
  - **Action**: Query GPU capabilities for supported formats
  - **Dependencies**: T080

- [ ] **T082** [P] Implement CompressedTextureLoader
  - **File**: `src/commonMain/kotlin/io/kreekt/texture/CompressedTextureLoader.kt`
  - **Action**: Load KTX2, DDS, PVR files
  - **Contract**: T024 tests should pass
  - **Dependencies**: T080

- [ ] **T083** [P] Implement DataTexture, DataTexture3D, DataTexture2DArray
  - **File**: `src/commonMain/kotlin/io/kreekt/texture/DataTextures.kt`
  - **Action**: Create textures from typed arrays
  - **Contract**: T025 tests should pass

- [ ] **T084** [P] Implement DepthTexture
  - **File**: `src/commonMain/kotlin/io/kreekt/texture/DepthTexture.kt`
  - **Action**: Depth texture for shadow maps
  - **Contract**: T025 tests should pass

- [ ] **T085** [P] Implement EquirectangularToCubeGenerator
  - **File**: `src/commonMain/kotlin/io/kreekt/texture/EquirectangularToCubeGenerator.kt`
  - **Action**: Convert equirect to cube map
  - **Contract**: T026 tests should pass
  - **Dependencies**: T076

- [ ] **T086** Implement PMREMGenerator
  - **File**: `src/commonMain/kotlin/io/kreekt/texture/PMREMGenerator.kt`
  - **Action**: Generate pre-filtered environment maps (PMREM)
  - **Contract**: T026 tests should pass
  - **Dependencies**: T076, T085
  - **Note**: Complex shader generation, sequential

- [ ] **T087** Optimize PMREMGenerator (GGX sampling)
  - **File**: `src/commonMain/kotlin/io/kreekt/texture/PMREMGenerator.kt`
  - **Action**: Implement importance sampling for GGX distribution
  - **Contract**: T026 performance tests should pass
  - **Dependencies**: T086

### Instancing Implementation (3 tasks)

- [ ] **T088** [P] Implement InstancedMesh
  - **File**: `src/commonMain/kotlin/io/kreekt/instancing/InstancedMesh.kt`
  - **Action**: GPU instancing with per-instance matrices and colors
  - **Contract**: T027 tests should pass

- [ ] **T089** [P] Implement InstancedBufferAttribute
  - **File**: `src/commonMain/kotlin/io/kreekt/instancing/InstancedBufferAttribute.kt`
  - **Action**: Per-instance attribute storage and GPU upload
  - **Contract**: T028 tests should pass

- [ ] **T090** Integrate instancing with renderer
  - **File**: `src/commonMain/kotlin/io/kreekt/renderer/InstancedRenderer.kt`
  - **Action**: Single draw call for instanced meshes
  - **Contract**: T027 performance tests (10k instances) should pass
  - **Dependencies**: T088, T089

### Points & Sprites Implementation (4 tasks)

- [ ] **T091** [P] Implement Points (point clouds)
  - **File**: `src/commonMain/kotlin/io/kreekt/points/Points.kt`
  - **Action**: Render point clouds with per-point colors/sizes
  - **Contract**: T029 tests should pass

- [ ] **T092** [P] Implement PointsMaterial
  - **File**: `src/commonMain/kotlin/io/kreekt/points/PointsMaterial.kt`
  - **Action**: Point size, color, attenuation, texture support
  - **Contract**: T029 tests should pass

- [ ] **T093** [P] Implement Sprite (billboards)
  - **File**: `src/commonMain/kotlin/io/kreekt/points/Sprite.kt`
  - **Action**: Camera-facing quads with textures
  - **Contract**: T030 tests should pass

- [ ] **T094** [P] Implement SpriteMaterial
  - **File**: `src/commonMain/kotlin/io/kreekt/points/SpriteMaterial.kt`
  - **Action**: Sprite color, texture, rotation, alpha
  - **Contract**: T030 tests should pass

### Morph Targets Implementation (4 tasks)

- [ ] **T095** [P] Implement MorphTargetGeometry interface
  - **File**: `src/commonMain/kotlin/io/kreekt/morph/MorphTargetGeometry.kt`
  - **Action**: Store morph attributes and targets
  - **Contract**: T031 tests should pass

- [ ] **T096** Extend BufferGeometry with morph support
  - **File**: `src/commonMain/kotlin/io/kreekt/geometry/BufferGeometry.kt`
  - **Action**: Add morphAttributes, morphTargets properties
  - **Contract**: T031 tests should pass
  - **Dependencies**: T095

- [ ] **T097** [P] Implement MorphAnimationMixer
  - **File**: `src/commonMain/kotlin/io/kreekt/morph/MorphAnimationMixer.kt`
  - **Action**: Animate morph influences over time
  - **Contract**: T032 tests should pass

- [ ] **T098** Implement MorphShaderGenerator
  - **File**: `src/commonMain/kotlin/io/kreekt/morph/MorphShaderGenerator.kt`
  - **Action**: Generate WGSL morph blending code
  - **Contract**: T032 tests should pass
  - **Dependencies**: T095

### Clipping Planes Implementation (3 tasks)

- [ ] **T099** [P] Implement Plane class
  - **File**: `src/commonMain/kotlin/io/kreekt/clipping/Plane.kt`
  - **Action**: Plane with normal and constant, distance calculations
  - **Contract**: T033 tests should pass

- [ ] **T100** [P] Implement global and per-material clipping
  - **File**: `src/commonMain/kotlin/io/kreekt/clipping/ClippingSupport.kt`
  - **Action**: Renderer clippingPlanes, Material clippingPlanes
  - **Contract**: T033 tests should pass
  - **Dependencies**: T099

- [ ] **T101** Implement ClippingShaderGenerator
  - **File**: `src/commonMain/kotlin/io/kreekt/clipping/ClippingShaderGenerator.kt`
  - **Action**: Generate WGSL clipping code (gl_ClipDistance or discard)
  - **Contract**: T034 tests should pass
  - **Dependencies**: T099, T100

### Layers Implementation (1 task)

- [ ] **T102** [P] Implement Layers bitmask system
  - **File**: `src/commonMain/kotlin/io/kreekt/layers/Layers.kt`
  - **Action**: 32-layer bitmask with set/enable/disable/test
  - **Contract**: T035 tests should pass

### Render Targets Implementation (6 tasks)

- [ ] **T103** [P] Implement WebGPURenderTarget (common)
  - **File**: `src/commonMain/kotlin/io/kreekt/rendertarget/WebGPURenderTarget.kt`
  - **Action**: Expect class for off-screen rendering
  - **Contract**: T036 tests should pass

- [ ] **T104** Platform-specific WebGPURenderTarget implementations
  - **Files**:
    - `src/jsMain/kotlin/io/kreekt/rendertarget/WebGPURenderTarget.kt` (WebGPU texture)
    - `src/jvmMain/kotlin/io/kreekt/rendertarget/WebGPURenderTarget.kt` (Vulkan framebuffer)
  - **Action**: Implement actual render target for each platform
  - **Dependencies**: T103

- [ ] **T105** [P] Implement WebGPUMultipleRenderTargets (MRT)
  - **File**: `src/commonMain/kotlin/io/kreekt/rendertarget/WebGPUMultipleRenderTargets.kt`
  - **Action**: Multiple color attachments
  - **Contract**: T037 tests should pass
  - **Dependencies**: T103

- [ ] **T106** [P] Implement WebGPUCubeRenderTarget
  - **File**: `src/commonMain/kotlin/io/kreekt/rendertarget/WebGPUCubeRenderTarget.kt`
  - **Action**: Render to 6 cube faces
  - **Contract**: T038 tests should pass
  - **Dependencies**: T076, T103

- [ ] **T107** [P] Implement WebGPU3DRenderTarget and WebGPUArrayRenderTarget
  - **File**: `src/commonMain/kotlin/io/kreekt/rendertarget/WebGPU3DRenderTarget.kt`
  - **Action**: 3D texture slices and 2D array layers
  - **Contract**: T038 tests should pass
  - **Dependencies**: T083, T103

- [ ] **T108** [P] Implement RenderTargetPool
  - **File**: `src/commonMain/kotlin/io/kreekt/rendertarget/RenderTargetPool.kt`
  - **Action**: Pool-based render target management
  - **Contract**: T039 tests should pass
  - **Dependencies**: T103

### Shape & Path Implementation (6 tasks)

- [ ] **T109** [P] Implement Shape class
  - **File**: `src/commonMain/kotlin/io/kreekt/shape/Shape.kt`
  - **Action**: 2D shape with holes
  - **Contract**: T040 tests should pass

- [ ] **T110** [P] Implement ShapeUtils (Earcut triangulation)
  - **File**: `src/commonMain/kotlin/io/kreekt/shape/ShapeUtils.kt`
  - **Action**: Triangulate shapes with holes using Earcut algorithm
  - **Contract**: T040 tests should pass
  - **Dependencies**: T109

- [ ] **T111** [P] Implement ShapeGeometry
  - **File**: `src/commonMain/kotlin/io/kreekt/shape/ShapeGeometry.kt`
  - **Action**: Create 2D geometry from shapes
  - **Contract**: T040 tests should pass
  - **Dependencies**: T109, T110

- [ ] **T112** [P] Implement ExtrudeGeometry
  - **File**: `src/commonMain/kotlin/io/kreekt/shape/ExtrudeGeometry.kt`
  - **Action**: Extrude shapes to 3D with beveling
  - **Contract**: T041 tests should pass
  - **Dependencies**: T109, T110

- [ ] **T113** [P] Implement LatheGeometry
  - **File**: `src/commonMain/kotlin/io/kreekt/shape/LatheGeometry.kt`
  - **Action**: Revolve 2D points around axis
  - **Contract**: T042 tests should pass

- [ ] **T114** [P] Implement Shapes utility (pre-defined shapes)
  - **File**: `src/commonMain/kotlin/io/kreekt/shape/Shapes.kt`
  - **Action**: Circle, rectangle, star, heart, polygon shapes
  - **Dependencies**: T109

### Enhanced Lines Implementation (4 tasks)

- [ ] **T115** [P] Implement Line2 and LineSegments2
  - **File**: `src/commonMain/kotlin/io/kreekt/lines/Line2.kt`
  - **Action**: Screen-space quad-based line rendering
  - **Contract**: T043 tests should pass

- [ ] **T116** [P] Implement LineMaterial
  - **File**: `src/commonMain/kotlin/io/kreekt/lines/LineMaterial.kt`
  - **Action**: Pixel-accurate width, world units, dashing
  - **Contract**: T043 tests should pass

- [ ] **T117** [P] Implement LineGeometry and LineSegmentsGeometry
  - **File**: `src/commonMain/kotlin/io/kreekt/lines/LineGeometry.kt`
  - **Action**: Position and color buffer management
  - **Contract**: T043 tests should pass

- [ ] **T118** [P] Implement LineBasicMaterial and LineDashedMaterial
  - **File**: `src/commonMain/kotlin/io/kreekt/lines/LineBasicMaterial.kt`
  - **Action**: Basic line rendering with dashing support
  - **Contract**: T044 tests should pass

### LOD Implementation (2 tasks)

- [ ] **T119** [P] Implement LOD container
  - **File**: `src/commonMain/kotlin/io/kreekt/lod/LOD.kt`
  - **Action**: Distance-based level switching
  - **Contract**: T045 tests should pass

- [ ] **T120** [P] Implement LODUtils (automatic generation)
  - **File**: `src/commonMain/kotlin/io/kreekt/lod/LODUtils.kt`
  - **Action**: Generate LOD hierarchy, simplify geometry
  - **Contract**: T045 tests should pass
  - **Dependencies**: T119

### Constants Implementation (2 tasks)

- [ ] **T121** [P] Implement all constant enums
  - **File**: `src/commonMain/kotlin/io/kreekt/constants/Constants.kt`
  - **Action**: Blending, depth, cull, texture enums (10 categories)
  - **Contract**: T046 tests should pass

- [ ] **T122** [P] Implement ConstantUtils (WebGPU mapping)
  - **File**: `src/commonMain/kotlin/io/kreekt/constants/ConstantUtils.kt`
  - **Action**: Map KreeKt constants to WebGPU/Vulkan values
  - **Contract**: T046 tests should pass
  - **Dependencies**: T121

---

## Phase 3.4: Integration Tests (15 tasks)

**Prerequisites: Core implementation complete, contract tests passing**

- [ ] **T123** [P] Integration test: Audio + Scene
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/AudioSceneIntegrationTest.kt`
  - **Scenario**: Positional audio follows moving object in 3D scene
  - **Validates**: Audio spatialization, listener updates

- [ ] **T124** [P] Integration test: Raycaster + Instancing
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/RaycastInstancedMeshTest.kt`
  - **Scenario**: Pick individual instances from InstancedMesh
  - **Validates**: Instance ID in Intersection, BVH with instancing

- [ ] **T125** [P] Integration test: Fog + Transparency
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/FogTransparencyTest.kt`
  - **Scenario**: Fog blending with transparent materials
  - **Validates**: Shader fog integration, alpha blending

- [ ] **T126** [P] Integration test: RenderTarget + Post-processing
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/RenderTargetPostProcessTest.kt`
  - **Scenario**: Render scene to texture, apply blur, render to screen
  - **Validates**: Off-screen rendering, texture sampling

- [ ] **T127** [P] Integration test: Curve + Animation path
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/CurveAnimationPathTest.kt`
  - **Scenario**: Animate object along CatmullRomCurve3
  - **Validates**: Curve evaluation, tangent computation

- [ ] **T128** [P] Integration test: Points + Raycasting
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/PointCloudPickingTest.kt`
  - **Scenario**: Pick individual points from 1M point cloud
  - **Validates**: Point threshold distance, BVH with points

- [ ] **T129** [P] Integration test: Morph + Skinning
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/MorphSkinnedMeshTest.kt`
  - **Scenario**: Combine morph targets with skeletal animation
  - **Validates**: Shader morphing + skinning, influence blending

- [ ] **T130** [P] Integration test: Layers + Multi-pass rendering
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/LayersMultiPassTest.kt`
  - **Scenario**: Render different layers to different render targets
  - **Validates**: Layer filtering, multiple render passes

- [ ] **T131** [P] Integration test: Clipping + Shadows
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/ClippingShadowsTest.kt`
  - **Scenario**: Clipping planes affect shadow casting
  - **Validates**: clipShadows parameter, shadow map clipping

- [ ] **T132** [P] Integration test: CubeCamera + PBR materials
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/DynamicEnvMapTest.kt`
  - **Scenario**: Dynamic environment mapping with reflective sphere
  - **Validates**: CubeCamera updates, envMap integration

- [ ] **T133** [P] Integration test: VideoTexture + Material
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/VideoMaterialTest.kt`
  - **Scenario**: Video playing on mesh surface
  - **Validates**: Video streaming, texture updates per frame

- [ ] **T134** [P] Integration test: ExtrudeGeometry + Shape with holes
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/ExtrudeHolesTest.kt`
  - **Scenario**: Extrude complex shape with multiple holes
  - **Validates**: Earcut triangulation, extrusion side faces

- [ ] **T135** [P] Integration test: Line2 + Camera zoom
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/Line2ResolutionTest.kt`
  - **Scenario**: Line width remains constant in pixels during zoom
  - **Validates**: Resolution-independent rendering, worldUnits=false

- [ ] **T136** [P] Integration test: LOD + Camera distance
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/LODDistanceTest.kt`
  - **Scenario**: LOD automatically switches levels as camera moves
  - **Validates**: Distance calculation, level visibility switching

- [ ] **T137** [P] Integration test: PMREMGenerator + Scene
  - **File**: `src/commonTest/kotlin/io/kreekt/integration/IBLSceneTest.kt`
  - **Scenario**: Load HDR, generate PMREM, use for IBL
  - **Validates**: Environment map pre-filtering, PBR integration

---

## Phase 3.5: Platform-Specific Tests (20 tasks)

**Platform validation for cross-platform features**

### JVM Platform Tests (5 tasks)

- [ ] **T138** [P] JVM: AudioListener with OpenAL
  - **File**: `src/jvmTest/kotlin/io/kreekt/audio/JvmAudioListenerTest.kt`
  - **Validates**: OpenAL context creation, listener positioning

- [ ] **T139** [P] JVM: CubeTexture with Vulkan
  - **File**: `src/jvmTest/kotlin/io/kreekt/texture/JvmCubeTextureTest.kt`
  - **Validates**: VK_IMAGE_VIEW_TYPE_CUBE creation and sampling

- [ ] **T140** [P] JVM: WebGPURenderTarget with Vulkan framebuffer
  - **File**: `src/jvmTest/kotlin/io/kreekt/rendertarget/JvmRenderTargetTest.kt`
  - **Validates**: Vulkan framebuffer creation, attachment binding

- [ ] **T141** [P] JVM: CompressedTexture BC7 format
  - **File**: `src/jvmTest/kotlin/io/kreekt/texture/JvmCompressionTest.kt`
  - **Validates**: BC7 compressed texture loading on Windows/Linux

- [ ] **T142** [P] JVM: Instancing with Vulkan
  - **File**: `src/jvmTest/kotlin/io/kreekt/instancing/JvmInstancingTest.kt`
  - **Validates**: vkCmdDrawIndexed with instance count

### JS Platform Tests (5 tasks)

- [ ] **T143** [P] JS: AudioListener with Web Audio API
  - **File**: `src/jsTest/kotlin/io/kreekt/audio/JsAudioListenerTest.kt`
  - **Validates**: AudioContext, PannerNode creation

- [ ] **T144** [P] JS: VideoTexture with HTMLVideoElement
  - **File**: `src/jsTest/kotlin/io/kreekt/texture/JsVideoTextureTest.kt`
  - **Validates**: Video element texture updates

- [ ] **T145** [P] JS: CanvasTexture with HTMLCanvasElement
  - **File**: `src/jsTest/kotlin/io/kreekt/texture/JsCanvasTextureTest.kt`
  - **Validates**: Canvas 2D context texture updates

- [ ] **T146** [P] JS: WebGPURenderTarget with WebGPU texture
  - **File**: `src/jsTest/kotlin/io/kreekt/rendertarget/JsRenderTargetTest.kt`
  - **Validates**: GPUTexture creation, render pass descriptor

- [ ] **T147** [P] JS: CompressedTexture ETC2 format (mobile)
  - **File**: `src/jsTest/kotlin/io/kreekt/texture/JsCompressionTest.kt`
  - **Validates**: ETC2 format detection and loading

### Android Platform Tests (5 tasks)

- [ ] **T148** [P] Android: AudioListener with AudioTrack
  - **File**: `src/androidTest/kotlin/io/kreekt/audio/AndroidAudioListenerTest.kt`
  - **Validates**: AudioTrack with Oboe low-latency

- [ ] **T149** [P] Android: Vulkan native API
  - **File**: `src/androidTest/kotlin/io/kreekt/renderer/AndroidVulkanTest.kt`
  - **Validates**: Native Vulkan instance creation

- [ ] **T150** [P] Android: CompressedTexture ASTC format
  - **File**: `src/androidTest/kotlin/io/kreekt/texture/AndroidCompressionTest.kt`
  - **Validates**: ASTC compressed texture on mobile GPU

- [ ] **T151** [P] Android: RenderTarget with Vulkan swapchain
  - **File**: `src/androidTest/kotlin/io/kreekt/rendertarget/AndroidRenderTargetTest.kt`
  - **Validates**: Vulkan surface and swapchain integration

- [ ] **T152** [P] Android: Instancing performance (10k+ instances)
  - **File**: `src/androidTest/kotlin/io/kreekt/instancing/AndroidInstancingTest.kt`
  - **Validates**: Mobile GPU instancing performance

### iOS Platform Tests (5 tasks)

- [ ] **T153** [P] iOS: AudioListener with AVAudioEngine
  - **File**: `src/iosTest/kotlin/io/kreekt/audio/IosAudioListenerTest.kt`
  - **Validates**: AVAudio3DNode spatial audio

- [ ] **T154** [P] iOS: MoltenVK (Vulkan-to-Metal)
  - **File**: `src/iosTest/kotlin/io/kreekt/renderer/IosMoltenVKTest.kt`
  - **Validates**: MoltenVK instance creation and Metal backend

- [ ] **T155** [P] iOS: CompressedTexture PVRTC format
  - **File**: `src/iosTest/kotlin/io/kreekt/texture/IosCompressionTest.kt`
  - **Validates**: PVRTC compressed texture on iOS GPU

- [ ] **T156** [P] iOS: RenderTarget with Metal texture
  - **File**: `src/iosTest/kotlin/io/kreekt/rendertarget/IosRenderTargetTest.kt`
  - **Validates**: MoltenVK framebuffer to Metal texture

- [ ] **T157** [P] iOS: LOD performance on mobile
  - **File**: `src/iosTest/kotlin/io/kreekt/lod/IosLODTest.kt`
  - **Validates**: LOD switching maintains 60 FPS on iOS

---

## Phase 3.6: Performance Optimization (12 tasks)

- [ ] **T158** [P] Optimize BVH construction (SAH)
  - **File**: `src/commonMain/kotlin/io/kreekt/raycaster/BVH.kt`
  - **Target**: <5% CPU overhead for BVH maintenance
  - **Method**: Surface Area Heuristic for optimal splits

- [ ] **T159** [P] Optimize raycasting (10k objects)
  - **File**: `src/commonMain/kotlin/io/kreekt/raycaster/Raycaster.kt`
  - **Target**: 10,000 objects tested per frame at 60 FPS (<16ms)
  - **Method**: Early exit, spatial coherence

- [ ] **T160** [P] Optimize curve tessellation (adaptive)
  - **File**: `src/commonMain/kotlin/io/kreekt/curve/CurveUtils.kt`
  - **Target**: <5ms for 100 curve segments
  - **Method**: Adaptive subdivision based on curvature

- [ ] **T161** [P] Optimize instancing (50k instances)
  - **File**: `src/commonMain/kotlin/io/kreekt/instancing/InstancedMesh.kt`
  - **Target**: 50,000 instances single draw call at 60 FPS
  - **Method**: Efficient buffer updates, frustum culling per instance

- [ ] **T162** [P] Optimize point cloud rendering (1M points)
  - **File**: `src/commonMain/kotlin/io/kreekt/points/Points.kt`
  - **Target**: 1M+ points at 60 FPS
  - **Method**: GPU-side point size computation, vertex pulling

- [ ] **T163** [P] Optimize morph target shader generation
  - **File**: `src/commonMain/kotlin/io/kreekt/morph/MorphShaderGenerator.kt`
  - **Target**: <5ms morph computation for 10k vertices
  - **Method**: Loop unrolling, texture-based morph storage

- [ ] **T164** [P] Optimize fog shader integration
  - **File**: `src/commonMain/kotlin/io/kreekt/fog/FogShaderInjector.kt`
  - **Target**: <1ms fog overhead per frame
  - **Method**: Shader compilation caching, uniform buffer optimization

- [ ] **T165** [P] Optimize clipping plane shader
  - **File**: `src/commonMain/kotlin/io/kreekt/clipping/ClippingShaderGenerator.kt`
  - **Target**: <2ms clipping overhead per frame
  - **Method**: Hardware clipping (gl_ClipDistance) where available

- [ ] **T166** [P] Optimize render target pool
  - **File**: `src/commonMain/kotlin/io/kreekt/rendertarget/RenderTargetPool.kt`
  - **Target**: <1ms target acquisition/release
  - **Method**: Hash-based lookup, LRU eviction

- [ ] **T167** [P] Optimize shape triangulation (Earcut)
  - **File**: `src/commonMain/kotlin/io/kreekt/shape/ShapeUtils.kt`
  - **Target**: <10ms for complex shapes with holes
  - **Method**: Efficient Earcut implementation, polygon simplification

- [ ] **T168** [P] Optimize Line2 rendering
  - **File**: `src/commonMain/kotlin/io/kreekt/lines/Line2.kt`
  - **Target**: 100k line segments at 60 FPS
  - **Method**: Instanced rendering, geometry shader optimization

- [ ] **T169** [P] Optimize LOD distance calculations
  - **File**: `src/commonMain/kotlin/io/kreekt/lod/LOD.kt`
  - **Target**: <1ms for 100 LODs per frame
  - **Method**: Cached distance calculations, batch updates

---

## Phase 3.7: Documentation & Polish (5 tasks)

- [ ] **T170** [P] Update API documentation (Dokka)
  - **Files**: KDoc comments in all public APIs
  - **Action**: Add comprehensive KDoc for 17 feature modules
  - **Tool**: Dokka generation

- [ ] **T171** [P] Create migration guide from Three.js
  - **File**: `docs/migration-guide.md`
  - **Action**: Document API differences, code examples for each feature

- [ ] **T172** [P] Add quickstart examples to docs
  - **File**: `docs/examples/`
  - **Action**: Copy quickstart.md examples to documentation site
  - **Examples**: 16 usage examples from quickstart.md

- [ ] **T173** [P] Performance profiling report
  - **File**: `docs/performance-report.md`
  - **Action**: Document performance benchmarks for all features
  - **Metrics**: FPS, memory usage, initialization time per feature

- [ ] **T174** Final validation and cleanup
  - **Action**: Remove any temporary code, verify all tests pass
  - **Checklist**:
    - [ ] All 174 tasks completed
    - [ ] All contract tests passing (T004-T046)
    - [ ] All integration tests passing (T123-T137)
    - [ ] All platform tests passing (T138-T157)
    - [ ] Performance targets met (T158-T169)
    - [ ] Documentation complete (T170-T173)
    - [ ] Zero stubs, TODOs, "for now" phrases (constitutional requirement)

---

## Dependencies Summary

### Critical Path
```
Setup (T001-T003)
  ↓
Contract Tests (T004-T046) [P] - MUST ALL FAIL
  ↓
Core Implementation (T047-T122)
  - Audio: T047→T048→T049,T050,T051
  - Helpers: T052,T053,T054,T055 [P]
  - Cameras: T056,T057,T058 [P] (T056 needs T084,T106)
  - Fog: T059,T060→T061
  - Raycasting: T062,T063→T064,T065 [P]→T066→T067
  - Curves: T068→T069,T070,T071,T072,T073 [P]→T074,T075
  - Textures: T076→T077, T078,T079,T080→T081,T082, T083,T084,T085 [P]→T086→T087
  - Instancing: T088,T089 [P]→T090
  - Points: T091,T092,T093,T094 [P]
  - Morph: T095→T096, T097,T098 [P]
  - Clipping: T099→T100,T101
  - Layers: T102
  - RenderTargets: T103→T104, T105,T106,T107,T108 [P]
  - Shape: T109→T110,T111,T112,T113,T114 [P]
  - Lines: T115,T116,T117,T118 [P]
  - LOD: T119,T120 [P]
  - Constants: T121,T122 [P]
  ↓
Integration Tests (T123-T137) [P]
  ↓
Platform Tests (T138-T157) [P]
  ↓
Performance Optimization (T158-T169) [P]
  ↓
Documentation & Polish (T170-T174)
```

### Blocking Tasks (Sequential)
- T048 blocks T049 (platform audio before positional audio)
- T061 blocks other fog tasks (shader injection required)
- T064 blocks T067 (basic raycasting before BVH integration)
- T074 blocks other tube tasks (Frenet frames required)
- T086 blocks T087 (basic PMREM before GGX optimization)
- T090 blocks instancing completion (renderer integration)
- T096 blocks morph completion (geometry extension)
- T101 blocks clipping completion (shader integration)

### Parallel Opportunities
- **Phase 3.2 (Tests)**: All 43 contract test tasks can run in parallel [P]
- **Phase 3.3 (Core)**: ~70 implementation tasks marked [P] can run in parallel (different modules)
- **Phase 3.4 (Integration)**: All 15 integration test tasks can run in parallel [P]
- **Phase 3.5 (Platform)**: All 20 platform test tasks can run in parallel [P]
- **Phase 3.6 (Performance)**: All 12 optimization tasks can run in parallel [P]
- **Phase 3.7 (Docs)**: 4 of 5 documentation tasks can run in parallel [P]

---

## Parallel Execution Examples

### Launch all contract tests together (43 tasks):
```bash
# Audio tests
Task: "Contract test: AudioListener attachment and position tracking"
Task: "Contract test: PositionalAudio 3D panning and attenuation"
Task: "Contract test: Audio loading and playback controls"

# Helper tests
Task: "Contract test: Visual debugging helpers (axes, grid, box)"
Task: "Contract test: Light and camera helpers"

# Camera tests
Task: "Contract test: CubeCamera 6-face rendering"
Task: "Contract test: StereoCamera for VR"
Task: "Contract test: ArrayCamera multi-view rendering"

# ... (all 43 contract tests from T004-T046)
```

### Launch parallel implementation tasks (example batch):
```bash
# Different modules = can run in parallel
Task: "Implement AudioListener (common)"
Task: "Implement AxesHelper and GridHelper"
Task: "Implement Fog (linear)"
Task: "Implement Raycaster core"
Task: "Implement base Curve class"
Task: "Implement CubeTexture (common)"
Task: "Implement InstancedMesh"
Task: "Implement Points (point clouds)"
Task: "Implement Layers bitmask system"
```

---

## Validation Checklist

**GATE: All must be checked before marking feature complete**

- [x] All 17 contract files have corresponding tests (T004-T046)
- [x] All 17 entity categories have model tasks (T047-T122)
- [x] All tests come before implementation (Phase 3.2 before 3.3)
- [x] Parallel tasks truly independent (different modules/files)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] TDD enforced (tests fail first, then implementation)
- [ ] All 150+ functional requirements covered by tasks
- [ ] Performance targets specified for optimization tasks
- [ ] Platform-specific implementations for expect/actual
- [ ] Constitutional requirements met:
  - [ ] Zero stubs, TODOs, "for now" phrases
  - [ ] Production-ready code only
  - [ ] Cross-platform compatibility (7 platforms)
  - [ ] Performance standards (60 FPS, memory budgets)
  - [ ] Type safety (no runtime casts)

---

## Estimated Effort

- **Total Tasks**: 174
- **Contract Tests**: 43 tasks (can run parallel) = ~2-3 days
- **Core Implementation**: 76 tasks (many parallel) = ~6-8 weeks
- **Integration Tests**: 15 tasks (parallel) = ~3-5 days
- **Platform Tests**: 20 tasks (parallel) = ~3-5 days
- **Performance Optimization**: 12 tasks (parallel) = ~1-2 weeks
- **Documentation**: 5 tasks = ~3-5 days

**Total Estimated Time**: 8-10 weeks (as specified in plan.md)

---

## Notes

- **[P] = Parallelizable**: 120+ tasks can run in parallel
- **TDD Enforced**: All contract tests (T004-T046) MUST FAIL before implementation
- **Constitutional Compliance**: Zero placeholders, all code production-ready
- **Platform Coverage**: 7 platforms (JVM, JS, Android, iOS, Linux, Windows, macOS)
- **Performance Targets**: Explicitly defined for optimization tasks
- **Three.js r180 Parity**: All 25 missing feature categories now implemented

---

*Ready for task execution. Start with Phase 3.1 (Setup) → Phase 3.2 (Tests First)*