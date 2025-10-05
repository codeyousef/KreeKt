# Implementation Plan: Basic Minecraft Clone (VoxelCraft)

**Branch**: `014-create-a-basic` | **Date**: 2025-10-04 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/014-create-a-basic/spec.md`

## Execution Flow (/plan command scope)

```
1. Load feature spec from Input path ✅
2. Fill Technical Context ✅
3. Fill Constitution Check section ✅
4. Evaluate Constitution Check → PASS ✅
5. Execute Phase 0 → research.md ✅
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, CLAUDE.md ✅
7. Re-evaluate Constitution Check → PASS ✅
8. Plan Phase 2 → Describe task generation approach ✅
9. STOP - Ready for /tasks command ✅
```

## Summary

A web-based creative mode voxel building game in the style of Minecraft, built using the KreeKt 3D graphics library.
Players can explore a procedurally generated 512x512x256 block world, break and place different block types, and fly
freely through the environment. The game runs entirely in the browser using WebGL2, with automatic save/restore via
localStorage. Core features include first-person camera controls, chunk-based rendering with face culling optimization,
simple terrain generation with natural features (trees, caves), and a creative mode inventory system with unlimited
blocks.

## Technical Context

**Language/Version**: Kotlin 1.9+ Multiplatform (JavaScript target only for v1)
**Primary Dependencies**:

- KreeKt library (WebGL2 renderer, scene graph, cameras, geometries, materials)
- kotlinx-coroutines-core 1.8.0
- kotlinx-serialization-json 1.6.0 (for localStorage persistence)

**Storage**: Browser localStorage (world state, player position, inventory)
**Testing**: Kotlin Test (multiplatform), Mocha/Karma for JS runtime tests
**Target Platform**: Web browsers with WebGL2 support (Chrome 56+, Firefox 51+, Safari 15+, Edge 79+)
**Project Type**: Single (JavaScript-only web application)
**Performance Goals**:

- Target: 60 FPS on modern hardware (2020+ laptop/desktop)
- Minimum acceptable: 30 FPS on average hardware (2017+ laptop)
- Render distance: 8-12 chunks (128-192 blocks)
- World generation: <3 seconds for full 512x512x256 world

**Constraints**:

- Total world size: 512x512 blocks horizontal, 256 blocks vertical (fixed boundaries)
- localStorage limit: ~5-10MB for world data (requires chunk compression)
- WebGL2 drawcall budget: <200 draw calls per frame (chunk batching required)
- Memory budget: <512MB total for world data + rendering

**Scale/Scope**:

- 1,024 chunks (32x32 grid of 16x16x256 chunks)
- ~67 million blocks total (512×512×256)
- 7-10 distinct block types in v1 (grass, dirt, stone, wood, leaves, sand, water)
- Single player only (no networking)
- Creative mode only (no health/hunger/damage systems)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Test-Driven Development (NON-NEGOTIABLE)

**Status**: ✅ PASS

- Plan includes contract tests in Phase 1 before implementation
- TDD red-green-refactor cycle will be enforced in tasks.md
- No placeholders or TODO markers permitted

### II. Production-Ready Code Only

**Status**: ✅ PASS

- All code will be fully implemented, no stubs
- localStorage error handling included
- Performance targets measurable via FPS counter

### III. Cross-Platform Compatibility

**Status**: ⚠️ PARTIAL (Justified deviation)

- **Deviation**: JavaScript-only target for v1 (excludes JVM, Native)
- **Justification**: Feature spec clarification #2 explicitly scoped to "JavaScript only (web browser)" to reduce
  initial complexity and validate core gameplay before platform expansion
- **Future path**: Architecture will use KreeKt's multiplatform APIs to enable later JVM/Native ports
- **Compliance**: Will use expect/actual patterns where applicable for future expansion

### IV. Performance Standards

**Status**: ✅ PASS

- 60 FPS target, 30 FPS minimum (FR-028, FR-029)
- Chunk-based rendering with face culling (FR-005)
- Performance benchmarks required in tests
- Memory budget defined (<512MB)

### V. Type Safety and API Design

**Status**: ✅ PASS

- Kotlin's type system for compile-time validation
- Using KreeKt API (Three.js-compatible patterns)
- Sealed classes for BlockType hierarchy
- Data classes for immutable structures (Vector3, ChunkPosition, etc.)

**Overall Assessment**: PASS with documented cross-platform deviation

## Project Structure

### Documentation (this feature)

```
specs/014-create-a-basic/
├── spec.md              # Feature specification
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
│   ├── world-api.yaml   # World generation and block manipulation contracts
│   ├── player-api.yaml  # Player movement, camera, and flight contracts
│   └── storage-api.yaml # Persistence contracts
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)

```
examples/voxelcraft/          # New example project (similar to basic-scene structure)
├── src/
│   ├── jsMain/
│   │   ├── kotlin/
│   │   │   └── io/kreekt/examples/voxelcraft/
│   │   │       ├── Main.kt                    # Entry point, game loop
│   │   │       ├── VoxelWorld.kt             # World entity, chunk management
│   │   │       ├── Chunk.kt                  # Chunk entity, mesh generation
│   │   │       ├── BlockType.kt              # Block type definitions (sealed class)
│   │   │       ├── TerrainGenerator.kt       # Procedural world generation
│   │   │       ├── Player.kt                 # Player entity (position, camera, inventory)
│   │   │       ├── PlayerController.kt       # Input handling, movement, flight
│   │   │       ├── BlockInteraction.kt       # Raycasting, break/place logic
│   │   │       ├── Inventory.kt              # Block inventory management
│   │   │       ├── WorldStorage.kt           # localStorage save/load
│   │   │       └── ui/
│   │   │           ├── Crosshair.kt          # Crosshair rendering
│   │   │           ├── HUD.kt                # Position/FPS display
│   │   │           └── InventoryUI.kt        # Block selection UI
│   │   └── resources/
│   │       ├── index.html                    # Game HTML container
│   │       └── textures/                     # Block textures (7-10 types)
│   └── commonTest/
│       └── kotlin/io/kreekt/examples/voxelcraft/
│           ├── contract/
│           │   ├── WorldContractTest.kt      # World API contracts
│           │   ├── PlayerContractTest.kt     # Player API contracts
│           │   └── StorageContractTest.kt    # Storage API contracts
│           ├── unit/
│           │   ├── ChunkTest.kt              # Chunk mesh generation tests
│           │   ├── TerrainGeneratorTest.kt   # World generation tests
│           │   ├── BlockInteractionTest.kt   # Raycasting tests
│           │   └── WorldStorageTest.kt       # Serialization tests
│           └── integration/
│               ├── GameLoopTest.kt           # Full game loop test
│               └── PerformanceTest.kt        # FPS benchmarks
└── build.gradle.kts                          # Example project build config
```

**Structure Decision**: Single project structure (JavaScript-only example). Using KreeKt's established `examples/`
pattern (similar to `examples/basic-scene/`). The voxelcraft example will be a standalone runnable game demonstrating
KreeKt's capabilities for voxel rendering, chunked worlds, and player interaction systems.

## Phase 0: Outline & Research

**Unknowns to Research**:

1. **Terrain Generation Algorithm**
    - Research: Simplex/Perlin noise for terrain height maps in JavaScript
    - Research: Cave generation using 3D noise or cellular automata
    - Research: Tree placement algorithms (random distribution with spacing)

2. **Chunk Rendering Optimization**
    - Research: Greedy meshing algorithms for voxel face reduction
    - Research: Frustum culling for chunk visibility
    - Research: Chunk mesh update strategies (rebuild vs. partial update)

3. **LocalStorage Compression**
    - Research: Run-length encoding for block data
    - Research: JavaScript compression libraries (pako.js, fflate)
    - Research: Chunk delta compression for incremental saves

4. **Block Textures & Rendering**
    - Research: Texture atlas packing for block types
    - Research: Ambient occlusion techniques for voxels
    - Research: Simple lighting model (per-vertex or per-block)

5. **Input Handling Best Practices**
    - Research: Pointer Lock API for mouse camera control
    - Research: Keyboard event handling for WASD + flight
    - Research: Touch input support for mobile browsers (future)

6. **Performance Profiling**
    - Research: Browser performance profiling tools (DevTools, about:tracing)
    - Research: WebGL performance counters
    - Research: FPS measurement techniques

**Output**: research.md with consolidated findings and technology decisions

## Phase 1: Design & Contracts

*Prerequisites: research.md complete*

### 1. Data Model (`data-model.md`)

Entities to extract from spec:

- **World**: 512x512x256 dimensions, chunk grid (32x32), seed, block storage
- **Chunk**: Position (x, z), 16x16x256 block array, mesh data, dirty flag
- **BlockType**: Enum/sealed class (Air, Grass, Dirt, Stone, Wood, Leaves, Sand, Water)
- **Block**: Type reference, position in chunk (x, y, z)
- **Player**: Position (Vector3), rotation (Euler/Quaternion), inventory, flight state
- **Inventory**: Block type → count map (unlimited capacity)
- **Camera**: First-person PerspectiveCamera from KreeKt
- **WorldState**: Serializable snapshot for localStorage

### 2. API Contracts (`contracts/`)

**world-api.yaml** (OpenAPI 3.0):

- `POST /world/generate` - Generate world from seed
- `GET /world/block/{x}/{y}/{z}` - Get block at position
- `PUT /world/block/{x}/{y}/{z}` - Set block at position
- `GET /world/chunk/{chunkX}/{chunkZ}` - Get chunk data
- `GET /world/bounds` - Get world boundaries

**player-api.yaml**:

- `GET /player/state` - Get player position, rotation, flight status
- `PUT /player/move` - Update player position with collision
- `PUT /player/rotate` - Update camera rotation
- `PUT /player/flight/toggle` - Toggle flight mode
- `GET /player/inventory` - Get inventory contents
- `POST /player/inventory/add` - Add block to inventory

**storage-api.yaml**:

- `POST /storage/save` - Save world state to localStorage
- `GET /storage/load` - Load world state from localStorage
- `DELETE /storage/clear` - Clear saved data
- `GET /storage/size` - Get current storage usage

### 3. Contract Tests

Generate failing tests for each endpoint:

- `WorldContractTest.kt` - Assert world API responses match schema
- `PlayerContractTest.kt` - Assert player API responses match schema
- `StorageContractTest.kt` - Assert storage API responses match schema

### 4. Quickstart Test (`quickstart.md`)

Extract test scenario from primary user story:

1. Generate new world
2. Spawn player at valid position
3. Move player with WASD
4. Rotate camera with mouse
5. Break block (left click)
6. Place block (right click)
7. Toggle flight mode
8. Save world state
9. Reload and verify state persisted

### 5. Update CLAUDE.md

Run: `.specify/scripts/bash/update-agent-context.sh claude`

- Add: KreeKt scene graph, WebGL2 renderer, Kotlin/JS
- Add: Voxel rendering, chunk meshing, localStorage
- Add: Recent change: "Added VoxelCraft Minecraft clone example"

**Output**: data-model.md, contracts/*.yaml, failing contract tests, quickstart.md, CLAUDE.md

## Phase 2: Task Planning Approach

*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:

1. Load `.specify/templates/tasks-template.md` as base
2. Generate tasks from Phase 1 artifacts:
    - **Contract Tests** (from contracts/*.yaml): 15-20 tasks
        - World API contract tests [P]
        - Player API contract tests [P]
        - Storage API contract tests [P]
    - **Entity Models** (from data-model.md): 8-10 tasks
        - BlockType sealed class [P]
        - Chunk entity with mesh generation
        - VoxelWorld entity with chunk grid
        - Player entity with state
        - Inventory entity
        - WorldState serialization
    - **Core Systems** (from requirements): 20-25 tasks
        - Terrain generation (noise, caves, trees)
        - Chunk rendering (greedy meshing, face culling)
        - Player controller (movement, collision, flight)
        - Block interaction (raycasting, break/place)
        - World storage (compression, save/load)
        - UI components (crosshair, HUD, inventory)
        - Game loop integration
    - **Integration Tests** (from quickstart.md): 5-8 tasks
        - Full game loop test
        - Performance benchmarks (60/30 FPS validation)
        - Storage persistence test

**Ordering Strategy**:

1. TDD order: Tests before implementation
2. Dependency order:
    - BlockType enum → Chunk → VoxelWorld
    - Player → PlayerController
    - WorldState → WorldStorage
3. Parallel markers [P] for independent files/tests

**Estimated Output**: 50-60 numbered, dependency-ordered tasks in tasks.md

**Task Size**: ~2-4 hours per task (TDD red-green-refactor cycle)

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation

*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation (execute tasks.md following TDD)
**Phase 5**: Validation (run tests, execute quickstart.md, verify 60 FPS)

## Complexity Tracking

*No constitutional violations requiring justification*

**Cross-platform Deviation Rationale**:

- Single platform (JavaScript) explicitly scoped by clarification session
- Reduces v1 complexity while validating core gameplay mechanics
- KreeKt API usage enables future JVM/Native ports with minimal refactoring
- No additional complexity introduced beyond feature requirements

## Progress Tracking

*This checklist is updated during execution flow*

**Phase Status**:

- [x] Phase 0: Research complete (/plan command) - research.md created
- [x] Phase 1: Design complete (/plan command) - data-model.md, contracts/*.yaml, quickstart.md, CLAUDE.md created
- [x] Phase 2: Task planning complete (/plan command - describe approach only) - Strategy documented above
- [ ] Phase 3: Tasks generated (/tasks command) - **READY TO EXECUTE**
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:

- [x] Initial Constitution Check: PASS (with documented cross-platform deviation)
- [x] Post-Design Constitution Check: PASS (no new violations introduced)
- [x] All NEEDS CLARIFICATION resolved (via /clarify session)
- [x] Complexity deviations documented (JavaScript-only targeting justified)

---
*Based on Constitution v1.0.0 - See `.specify/memory/constitution.md`*
