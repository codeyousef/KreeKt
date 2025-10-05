# Tasks: VoxelCraft - Basic Minecraft Clone

**Input**: Design documents from `/specs/014-create-a-basic/`
**Prerequisites**: plan.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

## Execution Flow (main)

```
1. Load plan.md from feature directory ✅
   → Tech stack: Kotlin/JS, KreeKt, WebGL2
   → Libraries: kotlinx-coroutines, kotlinx-serialization
   → Structure: examples/voxelcraft/ (JavaScript-only example)
2. Load design documents ✅
   → data-model.md: 8 entities (BlockType, Chunk, VoxelWorld, Player, Inventory, etc.)
   → contracts/: 3 API contracts (world, player, storage)
   → research.md: 6 technical decisions (terrain gen, rendering, compression, etc.)
   → quickstart.md: 9-step integration test scenario
3. Generate tasks by category ✅
   → Setup: 3 tasks
   → Tests: 19 tasks (contract + integration)
   → Core: 24 tasks (entities, systems, UI)
   → Integration: 8 tasks (rendering, input, storage)
   → Polish: 6 tasks (performance, docs)
4. Apply task rules ✅
   → Parallel [P]: Different files, no dependencies
   → Sequential: Shared files, dependencies
   → TDD: Tests before implementation
5. Total: 60 numbered tasks ✅
```

## Format: `[ID] [P?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions

**Project Structure**: `examples/voxelcraft/` (JavaScript-only KreeKt example)

```
examples/voxelcraft/
├── src/jsMain/kotlin/io/kreekt/examples/voxelcraft/  # Source code
├── src/jsMain/resources/                              # HTML, textures
└── src/commonTest/kotlin/io/kreekt/examples/voxelcraft/  # Tests
    ├── contract/       # Contract tests
    ├── unit/           # Unit tests
    └── integration/    # Integration tests
```

---

## Phase 3.1: Setup (3 tasks)

- [x] **T001** Create `examples/voxelcraft/` directory structure per plan.md
    - Create `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/`
    - Create `src/jsMain/resources/` for HTML and textures
    - Create `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/{contract,unit,integration}/`
    - **Files**: Directory structure only
    - **Dependencies**: None

- [x] **T002** Initialize VoxelCraft Gradle project with dependencies
    - Create `examples/voxelcraft/build.gradle.kts`
    - Add Kotlin Multiplatform plugin (JS target only)
    - Add dependencies: kotlinx-coroutines-core 1.8.0, kotlinx-serialization-json 1.6.0
    - Add KreeKt library dependency
    - Add Kotlin Test framework
    - Configure `runJs` and `jsTest` tasks
    - **Files**: `examples/voxelcraft/build.gradle.kts`
    - **Dependencies**: T001

- [x] **T003** [P] Configure linting and create HTML entry point
    - Create `src/jsMain/resources/index.html` with canvas element
    - Add fullscreen CSS, pointer lock request on click
    - Configure ktlint or detekt for code quality
    - **Files**: `src/jsMain/resources/index.html`, linting config
    - **Dependencies**: T001

---

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3

**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### Contract Tests (9 tasks - All Parallel)

- [ ] **T004** [P] World API contract test: POST /world/generate
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/contract/WorldContractTest.kt`
    - Test: `testGenerateWorld()` - Verify 1,024 chunks generated with correct seed
    - Assert: Response matches world-api.yaml schema
    - **Must fail initially** (no VoxelWorld class yet)
    - **Dependencies**: T002

- [ ] **T005** [P] World API contract test: GET /world/block/{x}/{y}/{z}
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/contract/WorldContractTest.kt`
    - Test: `testGetBlock()` - Verify block retrieval at valid coordinates
    - Assert: Returns BlockType, handles out-of-bounds (404)
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T006** [P] World API contract test: PUT /world/block/{x}/{y}/{z}
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/contract/WorldContractTest.kt`
    - Test: `testSetBlock()` - Verify block placement at valid coordinates
    - Assert: Block updated, rejects out-of-bounds (403)
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T007** [P] Player API contract test: GET /player/state
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/contract/PlayerContractTest.kt`
    - Test: `testGetPlayerState()` - Verify player state retrieval
    - Assert: Returns position, rotation, isFlying, velocity per player-api.yaml
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T008** [P] Player API contract test: PUT /player/move
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/contract/PlayerContractTest.kt`
    - Test: `testMovePlayer()` - Verify movement with collision detection
    - Assert: Returns newPosition, collided flag
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T009** [P] Player API contract test: PUT /player/rotate
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/contract/PlayerContractTest.kt`
    - Test: `testRotateCamera()` - Verify pitch clamping to ±π/2
    - Assert: Rotation matches player-api.yaml schema
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T010** [P] Player API contract test: POST /player/flight/toggle
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/contract/PlayerContractTest.kt`
    - Test: `testToggleFlight()` - Verify flight mode toggle
    - Assert: Returns isFlying boolean
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T011** [P] Storage API contract test: POST /storage/save
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/contract/StorageContractTest.kt`
    - Test: `testSaveWorldState()` - Verify localStorage save with compression
    - Assert: Returns success, sizeBytes < 10MB, handles quota errors (507)
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T012** [P] Storage API contract test: GET /storage/load
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/contract/StorageContractTest.kt`
    - Test: `testLoadWorldState()` - Verify state restoration
    - Assert: Returns WorldState matching storage-api.yaml, handles missing data (404)
    - **Must fail initially**
    - **Dependencies**: T002

### Integration Tests (10 tasks - All Parallel)

- [ ] **T013** [P] Integration test: Generate world and spawn player (Quickstart Step 1-2)
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/integration/GameLoopTest.kt`
    - Test: `testWorldGenerationAndSpawn()` - Full scenario from quickstart.md steps 1-2
    - Assert: 1,024 chunks, player on solid ground, position Y >= 64
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T014** [P] Integration test: Player movement with WASD (Quickstart Step 3)
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/integration/GameLoopTest.kt`
    - Test: `testPlayerMovement()` - Simulate W key, verify position change
    - Assert: Player moves in camera-relative direction
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T015** [P] Integration test: Camera rotation with mouse (Quickstart Step 4)
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/integration/GameLoopTest.kt`
    - Test: `testCameraRotation()` - Simulate mouse movement, verify rotation
    - Assert: Pitch clamped to ±90°, yaw unclamped
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T016** [P] Integration test: Break block (Quickstart Step 5)
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/integration/GameLoopTest.kt`
    - Test: `testBreakBlock()` - Raycast, left-click, verify block removed
    - Assert: Block becomes AIR, added to inventory
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T017** [P] Integration test: Place block (Quickstart Step 6)
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/integration/GameLoopTest.kt`
    - Test: `testPlaceBlock()` - Raycast, right-click, verify block placed
    - Assert: Block appears, inventory count decremented
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T018** [P] Integration test: Toggle flight mode (Quickstart Step 7)
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/integration/GameLoopTest.kt`
    - Test: `testFlightMode()` - F key, vertical movement, gravity toggle
    - Assert: Flight enables vertical movement, disables gravity
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T019** [P] Integration test: Save world state (Quickstart Step 8)
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/integration/StoragePersistenceTest.kt`
    - Test: `testSaveWorld()` - Modify world, save, verify size < 10MB
    - Assert: Save succeeds, compressed data in localStorage
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T020** [P] Integration test: Load world state (Quickstart Step 9)
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/integration/StoragePersistenceTest.kt`
    - Test: `testLoadWorld()` - Reload, verify state restored
    - Assert: Player position, block changes, flight state persisted
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T021** [P] Performance test: 60 FPS target (Quickstart Performance)
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/integration/PerformanceTest.kt`
    - Test: `testAverageFPS()` - Run 300 frames, measure average FPS
    - Assert: avgFPS >= 60.0 (FR-028 constitutional requirement)
    - **Must fail initially**
    - **Dependencies**: T002

- [ ] **T022** [P] Performance test: 30 FPS minimum (Quickstart Performance)
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/integration/PerformanceTest.kt`
    - Test: `testMinimumFPS()` - Run 300 frames, measure min FPS
    - Assert: minFPS >= 30.0 (FR-029 constitutional requirement)
    - **Must fail initially**
    - **Dependencies**: T002

---

## Phase 3.3: Core Implementation (ONLY after tests are failing)

### Entity Models (8 tasks - Some Parallel)

- [ ] **T023** [P] BlockType sealed class with 8 block types
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/BlockType.kt`
    - Implement: Sealed class with Air, Grass, Dirt, Stone, Wood, Leaves, Sand, Water
    - Include: id (Byte), name (String), isTransparent (Boolean)
    - Add: fromId() companion function
    - **Verifies**: T004-T006 tests start passing (block types exist)
    - **Dependencies**: T004-T022 (tests must fail first)

- [ ] **T024** [P] ChunkPosition data class
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ChunkPosition.kt`
    - Implement: Data class with chunkX, chunkZ (Int, range -16 to 15)
    - Include: init block with range validation
    - Add: toWorldX(), toWorldZ() conversion methods
    - **Verifies**: T004 tests progress (chunk positions valid)
    - **Dependencies**: T004-T022

- [ ] **T025** Chunk class with 16x16x256 block storage
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Chunk.kt`
    - Implement: Chunk entity per data-model.md
    - Storage: ByteArray[65536] for blocks
    - Methods: getBlock(), setBlock(), serialize(), deserialize()
    - Properties: position, isDirty flag, mesh (nullable BufferGeometry)
    - **Verifies**: T004-T006 tests progress (chunks work)
    - **Dependencies**: T023, T024

- [ ] **T026** Inventory class with unlimited capacity
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Inventory.kt`
    - Implement: Map<BlockType, Int> for block counts
    - Methods: add(), remove(), getCount(), hasBlock(), selectBlock()
    - Property: selectedBlock (current block to place)
    - **Verifies**: T007, T016, T017 tests progress (inventory works)
    - **Dependencies**: T023

- [ ] **T027** Player class with position, rotation, inventory
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Player.kt`
    - Implement: Player entity per data-model.md
    - Properties: position (Vector3), rotation (Euler), velocity, isFlying, inventory, boundingBox
    - Methods: move(), rotate(), toggleFlight(), checkCollision(), isOnGround()
    - **Verifies**: T007-T010, T014, T015, T018 tests progress
    - **Dependencies**: T023, T026

- [ ] **T028** VoxelWorld class with chunk grid management
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/VoxelWorld.kt`
    - Implement: VoxelWorld entity per data-model.md
    - Storage: Map<ChunkPosition, Chunk> (1,024 chunks)
    - Properties: seed, scene, player, camera
    - Methods: getBlock(), setBlock(), generateTerrain(), update()
    - **Verifies**: T004-T006, T013 tests progress (world works)
    - **Dependencies**: T024, T025, T027

- [ ] **T029** [P] WorldState serialization model
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/WorldState.kt`
    - Implement: @Serializable data class per data-model.md
    - Include: SerializableVector3, SerializableRotation, SerializedChunk
    - Methods: from(world), restore()
    - **Verifies**: T011, T012, T019, T020 tests progress (persistence works)
    - **Dependencies**: T023, T024

- [ ] **T030** [P] RunLengthEncoding compression utility
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/util/RunLengthEncoding.kt`
    - Implement: encode(ByteArray) → ByteArray, decode(ByteArray) → ByteArray
    - RLE algorithm: [blockID, count, blockID, count, ...]
    - Target: 90%+ compression ratio for chunk data
    - **Verifies**: T011, T019 tests progress (compression works)
    - **Dependencies**: T004-T022

### Terrain Generation (2 tasks)

- [ ] **T031** SimplexNoise implementation for terrain generation
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/SimplexNoise.kt`
    - Implement: SimplexNoise class per research.md (2D and 3D noise)
    - Methods: eval(x, y), eval(x, y, z)
    - Use: Stefan Gustavson's algorithm
    - **Verifies**: T013 tests progress (terrain generates)
    - **Dependencies**: T004-T022

- [ ] **T032** TerrainGenerator with height maps and caves
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/TerrainGenerator.kt`
    - Implement: generate(chunk) using Simplex noise per research.md
    - 2D noise: Terrain height (Y = 64-124)
    - 3D noise: Cave carving (threshold >0.6)
    - Features: Trees (random placement on grass), basic biomes
    - **Verifies**: T013 tests pass (world generates correctly)
    - **Dependencies**: T023, T025, T028, T031

### Chunk Rendering (2 tasks)

- [x] **T033** ChunkMeshGenerator with greedy meshing algorithm
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ChunkMeshGenerator.kt`
    - Implement: generate(chunk) → MeshData per research.md
    - Face culling: Check adjacent blocks, skip if both solid
    - Greedy meshing: Merge adjacent faces into quads
    - Output: Vertex positions, normals, UVs, indices
    - **Verifies**: T013 tests progress (chunks render)
    - **Dependencies**: T023, T025

- [x] **T034** Texture atlas and UV mapping
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/TextureAtlas.kt`
    - Create: 4x3 texture atlas (64x48 pixels) per research.md
    - Implement: getUVForBlockFace(type, face) → UV coordinates
    - Lighting: getFaceBrightness(face) → Float (0.5 to 1.0)
    - **Verifies**: T013 tests progress (textures work)
    - **Dependencies**: T023

### Player Controller (2 tasks)

- [ ] **T035** PlayerController for keyboard and mouse input
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/PlayerController.kt`
    - Implement: Keyboard event handling (WASD, Space, Shift, F)
    - Implement: update(deltaTime) for movement logic
    - Collision detection: Call player.move() with collision check
    - Flight controls: Spacebar up, Shift down when flying
    - **Verifies**: T014, T018 tests pass (movement works)
    - **Dependencies**: T027

- [ ] **T036** CameraController for Pointer Lock API
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/CameraController.kt`
    - Implement: Pointer Lock request on canvas click
    - Implement: mousemove event handler with sensitivity
    - Pitch clamping: -π/2 to +π/2
    - Yaw: Unclamped rotation
    - **Verifies**: T015 tests pass (camera rotation works)
    - **Dependencies**: T027

### Block Interaction (1 task)

- [ ] **T037** BlockInteraction with raycasting for break/place
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/BlockInteraction.kt`
    - Implement: raycast() using KreeKt Raycaster
    - Implement: handleLeftClick() - Break block, add to inventory
    - Implement: handleRightClick() - Place block from inventory
    - Range: 5 blocks max distance
    - **Verifies**: T016, T017 tests pass (block interaction works)
    - **Dependencies**: T026, T027, T028

### Storage Persistence (1 task)

- [ ] **T038** WorldStorage with localStorage save/load
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/WorldStorage.kt`
    - Implement: save(world) → localStorage with gzip compression (pako.js)
    - Implement: load() → WorldState, handle quota errors
    - Auto-save: Every 30 seconds
    - Target: <10MB compressed size
    - **Verifies**: T011, T012, T019, T020 tests pass (persistence works)
    - **Dependencies**: T028, T029, T030

### UI Components (3 tasks - All Parallel)

- [ ] **T039** [P] Crosshair rendering
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ui/Crosshair.kt`
    - Implement: Draw 2D crosshair at screen center
    - Use: HTML canvas 2D or WebGL lines
    - Style: Simple white cross, 20x20 pixels
    - **Verifies**: No specific test, visual feature
    - **Dependencies**: T002

- [ ] **T040** [P] HUD for position, FPS, coordinates
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ui/HUD.kt`
    - Implement: Display player position (X, Y, Z)
    - Implement: FPS counter updated every second
    - Optional: Block facing direction, chunk position
    - **Verifies**: T021, T022 tests use FPS counter
    - **Dependencies**: T027

- [ ] **T041** [P] InventoryUI for block selection
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ui/InventoryUI.kt`
    - Implement: Horizontal hotbar with 9 slots
    - Display: Selected block type highlighted
    - Input: Number keys 1-9 to select blocks
    - **Verifies**: T016, T017 tests progress (inventory selection)
    - **Dependencies**: T026

### Main Entry Point (1 task)

- [ ] **T042** Main.kt game loop and initialization
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`
    - Implement: main() entry point
    - Initialize: VoxelWorld, PlayerController, CameraController, UI components
    - Game loop: requestAnimationFrame with deltaTime calculation
    - Sequence: world.update() → render() → UI.update()
    - **Verifies**: T013-T022 integration tests pass (full game works)
    - **Dependencies**: T028, T035, T036, T037, T038, T039, T040, T041

---

## Phase 3.4: Integration (8 tasks)

- [ ] **T043** Integrate ChunkMeshGenerator with VoxelWorld update loop
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/VoxelWorld.kt`
    - Add: Chunk mesh regeneration in update() when isDirty = true
    - Optimize: Only regenerate dirty chunks, limit to 5 chunks per frame
    - **Verifies**: T013 tests pass (chunks update correctly)
    - **Dependencies**: T028, T033

- [ ] **T044** Integrate BlockInteraction with PlayerController input
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/PlayerController.kt`
    - Add: Mouse button event handlers (left = break, right = place)
    - Call: BlockInteraction.handleLeftClick(), handleRightClick()
    - **Verifies**: T016, T017 tests pass (interaction triggers correctly)
    - **Dependencies**: T035, T037

- [ ] **T045** Integrate WorldStorage auto-save with game loop
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`
    - Add: setInterval(30000) for auto-save every 30 seconds
    - Add: window.onbeforeunload to save on page close
    - Error handling: Display message if save fails (quota exceeded)
    - **Verifies**: T019 tests pass (auto-save works)
    - **Dependencies**: T038, T042

- [ ] **T046** Integrate FPS counter with performance metrics
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ui/HUD.kt`
    - Add: FPSCounter class per research.md
    - Display: Update FPS every second in HUD
    - **Verifies**: T021, T022 tests can measure FPS
    - **Dependencies**: T040

- [ ] **T047** Integrate InventoryUI with block selection input
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/ui/InventoryUI.kt`
    - Add: Number key event handlers (1-9)
    - Update: Inventory.selectBlock() on key press
    - **Verifies**: T016, T017 tests progress (block selection works)
    - **Dependencies**: T041

- [ ] **T048** Integrate frustum culling for chunk visibility
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/VoxelWorld.kt`
    - Add: Camera frustum check in update()
    - Only render: Chunks intersecting camera frustum
    - Target: 100-200 visible chunks max
    - **Verifies**: Performance improvement for T021, T022
    - **Dependencies**: T028, T043

- [ ] **T049** Add world boundary collision detection
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Player.kt`
    - Add: Boundary checks in move() method
    - Clamp: Position to X/Z = -256 to 255, Y = 0 to 255
    - **Verifies**: Edge case for T014 tests
    - **Dependencies**: T027

- [ ] **T050** Error handling for localStorage quota exceeded
    - File: `src/jsMain/kotlin/io/kreekt/examples/voxelcraft/WorldStorage.kt`
    - Add: try-catch around localStorage.setItem()
    - Display: Error message in UI when quota exceeded
    - **Verifies**: T011, T019 tests handle quota errors correctly
    - **Dependencies**: T038

---

## Phase 3.5: Polish (10 tasks)

### Unit Tests (4 tasks - All Parallel)

- [ ] **T051** [P] Unit tests for ChunkMeshGenerator greedy meshing
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/unit/ChunkTest.kt`
    - Test: Face culling removes hidden faces
    - Test: Greedy meshing merges adjacent faces
    - Test: Correct vertex count for various chunk configurations
    - **Dependencies**: T033

- [ ] **T052** [P] Unit tests for TerrainGenerator
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/unit/TerrainGeneratorTest.kt`
    - Test: Same seed produces same terrain
    - Test: Height range 64-124
    - Test: Caves generate below surface
    - Test: Trees place on grass blocks only
    - **Dependencies**: T032

- [ ] **T053** [P] Unit tests for BlockInteraction raycasting
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/unit/BlockInteractionTest.kt`
    - Test: Raycast identifies correct target block
    - Test: Max distance 5 blocks enforced
    - Test: Adjacent placement validation
    - **Dependencies**: T037

- [ ] **T054** [P] Unit tests for WorldStorage serialization
    - File: `src/commonTest/kotlin/io/kreekt/examples/voxelcraft/unit/WorldStorageTest.kt`
    - Test: RLE compression reduces size by 90%+
    - Test: Serialization round-trip preserves data
    - Test: Handles missing/corrupted localStorage data
    - **Dependencies**: T038

### Performance Optimization (2 tasks)

- [ ] **T055** Profile rendering performance and optimize bottlenecks
    - Use: Chrome DevTools Performance tab per research.md
    - Target: Identify if avg FPS < 60
    - Optimize: Reduce draw calls, optimize mesh generation
    - **Verifies**: T021 tests pass (60 FPS achieved)
    - **Dependencies**: T042

- [ ] **T056** Optimize memory usage for world storage
    - Profile: Check memory usage < 512MB
    - Optimize: Chunk compression, mesh vertex deduplication
    - **Verifies**: T019 tests pass (size < 10MB)
    - **Dependencies**: T038

### Documentation (2 tasks - All Parallel)

- [ ] **T057** [P] Create README.md for VoxelCraft example
    - File: `examples/voxelcraft/README.md`
    - Include: Feature description, controls, build instructions
    - Add: `./gradlew :examples:voxelcraft:runJs` command
    - List: Browser compatibility (Chrome 56+, Firefox 51+, Safari 15+)
    - **Dependencies**: T042

- [ ] **T058** [P] Add inline code documentation (KDoc)
    - Add: KDoc comments to all public classes and methods
    - Document: Block types, chunk generation, player controls
    - **Dependencies**: T042

### Code Quality (2 tasks - All Parallel)

- [ ] **T059** [P] Remove code duplication and refactor
    - Identify: Duplicate mesh generation code
    - Extract: Common utilities (Vector3 helpers, collision detection)
    - Simplify: Complex methods (> 50 lines)
    - **Dependencies**: T042

- [ ] **T060** [P] Run quickstart.md manual testing
    - Execute: All 9 steps from quickstart.md manually in browser
    - Verify: Gameplay feels responsive, no visual glitches
    - Confirm: Save/load works across browser sessions
    - **Dependencies**: T042

---

## Dependencies

**Critical Path**:

```
T001 → T002 → [T004-T022 tests] → [T023-T042 implementation] → T042 → [T043-T050 integration] → [T051-T060 polish]
```

**Test-First (TDD)**:

- All tests T004-T022 MUST fail before starting T023-T042
- Tests verify implementation as it progresses

**Parallel Batches**:

- **Batch 1**: T004-T012 (contract tests - 9 parallel)
- **Batch 2**: T013-T022 (integration/performance tests - 10 parallel)
- **Batch 3**: T023, T024, T029, T030 (simple entities - 4 parallel)
- **Batch 4**: T039, T040, T041 (UI components - 3 parallel)
- **Batch 5**: T051-T054 (unit tests - 4 parallel)
- **Batch 6**: T057, T058, T059, T060 (polish - 4 parallel)

**Blocking Relationships**:

- T023 (BlockType) blocks: T025, T026, T028, T029
- T025 (Chunk) blocks: T028, T033
- T027 (Player) blocks: T028, T035, T036, T037
- T028 (VoxelWorld) blocks: T032, T038, T042, T043
- T042 (Main) blocks: T045, T055, T060

---

## Parallel Execution Examples

### Launch Contract Tests Together (Batch 1)

```bash
# All 9 contract tests can run in parallel (different test methods):
./gradlew :examples:voxelcraft:jsTest --tests "*WorldContractTest*"
./gradlew :examples:voxelcraft:jsTest --tests "*PlayerContractTest*"
./gradlew :examples:voxelcraft:jsTest --tests "*StorageContractTest*"
```

### Launch Entity Models Together (Batch 3)

```kotlin
// T023, T024, T029, T030 - Different files, no dependencies
Task: "Implement BlockType sealed class in BlockType.kt"
Task: "Implement ChunkPosition data class in ChunkPosition.kt"
Task: "Implement WorldState serialization in WorldState.kt"
Task: "Implement RunLengthEncoding in util/RunLengthEncoding.kt"
```

### Launch UI Components Together (Batch 4)

```kotlin
// T039, T040, T041 - Different files in ui/ package
Task: "Implement Crosshair in ui/Crosshair.kt"
Task: "Implement HUD in ui/HUD.kt"
Task: "Implement InventoryUI in ui/InventoryUI.kt"
```

---

## Validation

**Task Completeness**:

- ✅ All 3 contract files have tests (world, player, storage)
- ✅ All 8 entities have implementation tasks
- ✅ All 9 quickstart steps have integration tests
- ✅ All core systems covered (terrain, rendering, input, storage, UI)
- ✅ Performance targets validated (60 FPS, 30 FPS min)

**TDD Compliance**:

- ✅ 19 tests written before implementation (T004-T022)
- ✅ Tests verify functional requirements
- ✅ No implementation without failing tests first

**Constitutional Compliance**:

- ✅ Test-driven development enforced
- ✅ No placeholders or TODOs
- ✅ Performance targets measurable (T021, T022)
- ✅ Type safety via Kotlin sealed classes and data classes

---

**Total Tasks**: 60
**Estimated Time**: ~120-150 hours (2-3 hours per task average)
**Ready for Execution**: ✅

Next: Begin Phase 3.1 with T001-T003 (Setup)
