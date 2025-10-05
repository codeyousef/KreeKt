# QuickStart: VoxelCraft

**Feature**: Basic Minecraft Clone
**Purpose**: End-to-end test scenario validating all core features
**Status**: Test specification (implementation pending)

## Prerequisites

- Kotlin 1.9+ with Multiplatform plugin
- KreeKt library installed
- Web browser with WebGL2 support (Chrome 56+, Firefox 51+, Safari 15+)

## Test Scenario

This quickstart validates the primary user story from `spec.md`:

> A player enters a procedurally generated voxel world where they can freely explore a 3D environment made of different
> block types. The player can move around using standard first-person controls, look in any direction, break blocks by
> clicking on them, and place blocks from their inventory. The player can modify the landscape creatively and fly through
> the world.

### Step 1: Generate New World

```kotlin
// Initialize VoxelCraft game
val game = VoxelCraft(seed = 12345L)

// Verify world generated
assertEquals(1024, game.world.chunkCount)
assertTrue(game.world.player.position.y >= 64.0) // Player above terrain
```

**Expected Result**: World with 1,024 chunks (32x32 grid) generated using Simplex noise

**Acceptance Criteria** (FR-001, FR-003):

- World dimensions: 512x512 horizontal, 256 vertical
- Chunks: 32×32 = 1,024 total
- Terrain includes hills, valleys, caves, trees

### Step 2: Spawn Player at Valid Position

```kotlin
// Verify player spawned on solid ground
val blockBelow = game.world.getBlock(
    game.world.player.position.x.toInt(),
    (game.world.player.position.y - 1).toInt(),
    game.world.player.position.z.toInt()
)
assertNotNull(blockBelow)
assertTrue(blockBelow != BlockType.Air)
```

**Expected Result**: Player standing on GRASS or DIRT block

**Acceptance Criteria** (FR-007, FR-009):

- Player not falling through world
- Gravity applies correctly

### Step 3: Move Player with WASD

```kotlin
val initialPos = game.world.player.position.clone()

// Simulate W key press (move forward)
game.playerController.handleKeyDown("w")
game.update(deltaTime = 0.016f) // 60 FPS frame
game.playerController.handleKeyUp("w")

val newPos = game.world.player.position
assertTrue(newPos.z < initialPos.z) // Moved forward (negative Z in default facing)
```

**Expected Result**: Player moves in direction they're facing

**Acceptance Criteria** (FR-007, FR-010):

- WASD controls work correctly
- Movement respects camera rotation
- Collision detection prevents walking through walls

### Step 4: Rotate Camera with Mouse

```kotlin
val initialRotation = game.world.player.rotation.clone()

// Simulate mouse movement (right and down)
game.cameraController.handleMouseMove(movementX = 100.0, movementY = 50.0)

val newRotation = game.world.player.rotation
assertTrue(newRotation.y > initialRotation.y) // Turned right (yaw increased)
assertTrue(newRotation.x > initialRotation.x) // Looked down (pitch increased)
assertTrue(newRotation.x <= Math.PI / 2) // Pitch clamped to 90°
```

**Expected Result**: Camera rotates smoothly, pitch clamped to ±90°

**Acceptance Criteria** (FR-010, FR-013):

- Mouse controls camera direction
- First-person perspective maintained
- Pitch clamps prevent over-rotation

### Step 5: Break Block (Left Click)

```kotlin
val targetPos = Vector3(0, 65, 0)
game.world.setBlock(0, 65, 0, BlockType.Stone) // Place test block

// Simulate aiming at block and left-click
game.world.player.position.set(0.0, 66.0, 2.0)
game.world.player.rotation.set(0.0, Math.PI, 0.0) // Face block
game.blockInteraction.handleLeftClick()

// Verify block broken
val block = game.world.getBlock(0, 65, 0)
assertEquals(BlockType.Air, block)

// Verify block added to inventory
assertTrue(game.world.player.inventory.hasBlock(BlockType.Stone))
```

**Expected Result**: Block disappears, added to inventory

**Acceptance Criteria** (FR-014, FR-015, FR-016, FR-020):

- Raycasting identifies target block
- Block breaks instantly (creative mode)
- Inventory updated correctly
- Visual feedback (block highlight)

### Step 6: Place Block (Right Click)

```kotlin
val placementPos = Vector3(0, 66, 0)

// Ensure player has blocks in inventory
game.world.player.inventory.add(BlockType.Dirt, 64)
game.world.player.inventory.selectBlock(BlockType.Dirt)

// Simulate aiming at adjacent block and right-click
game.world.player.position.set(0.0, 67.0, 2.0)
game.blockInteraction.handleRightClick()

// Verify block placed
val placedBlock = game.world.getBlock(0, 66, 0)
assertEquals(BlockType.Dirt, placedBlock)
```

**Expected Result**: Dirt block placed adjacent to targeted surface

**Acceptance Criteria** (FR-017, FR-018, FR-019):

- Block appears immediately
- Cannot place inside player position
- Cannot place inside existing blocks

### Step 7: Toggle Flight Mode

```kotlin
assertFalse(game.world.player.isFlying)

// Press F key to toggle flight
game.playerController.handleKeyDown("f")
assertTrue(game.world.player.isFlying)

// Move upward while flying
game.playerController.handleKeyDown(" ") // Spacebar
game.update(deltaTime = 0.016f)
val yBefore = game.world.player.position.y

game.update(deltaTime = 0.016f)
val yAfter = game.world.player.position.y
assertTrue(yAfter > yBefore) // Moved up

// Toggle off
game.playerController.handleKeyDown("f")
assertFalse(game.world.player.isFlying)
```

**Expected Result**: Flight mode enables vertical movement, disables gravity

**Acceptance Criteria** (FR-034, FR-035, FR-036, FR-037):

- F key toggles flight on/off
- Spacebar moves up when flying
- Shift moves down when flying
- Gravity disabled during flight

### Step 8: Save World State

```kotlin
// Make some changes to world
game.world.setBlock(10, 70, 10, BlockType.Wood)
game.world.player.position.set(5.0, 75.0, 5.0)

// Save to localStorage
val saveResult = game.worldStorage.save(game.world)
assertTrue(saveResult.success)
assertTrue(saveResult.sizeBytes > 0)
assertTrue(saveResult.sizeBytes < 10_000_000) // <10MB
```

**Expected Result**: World saved to localStorage, size under 10MB

**Acceptance Criteria** (FR-032, FR-038):

- Auto-save every 30 seconds
- Compression reduces size 90%+
- Quota errors handled gracefully

### Step 9: Reload and Verify State Persisted

```kotlin
// Simulate page reload (destroy and recreate game)
val savedState = game.worldStorage.load()
assertNotNull(savedState)

val restoredGame = VoxelCraft.fromSavedState(savedState!!)

// Verify world state restored
assertEquals(12345L, restoredGame.world.seed)
assertEquals(Vector3(5.0, 75.0, 5.0), restoredGame.world.player.position)
assertEquals(BlockType.Wood, restoredGame.world.getBlock(10, 70, 10))
```

**Expected Result**: Game state fully restored from localStorage

**Acceptance Criteria** (FR-033):

- Player position restored
- Block modifications persisted
- Flight state restored

## Performance Validation

```kotlin
// Run game loop for 5 seconds, measure FPS
val frameCount = 300 // 5 seconds at 60 FPS
val frameTimes = mutableListOf<Double>()

for (frame in 1..frameCount) {
    val startTime = performance.now()

    game.update(deltaTime = 0.016f)
    game.render()

    val frameTime = performance.now() - startTime
    frameTimes.add(frameTime)
}

val avgFrameTime = frameTimes.average()
val avgFPS = 1000.0 / avgFrameTime
val minFPS = 1000.0 / frameTimes.maxOrNull()!!

// Constitutional requirements
assertTrue(avgFPS >= 60.0, "Average FPS: $avgFPS (target: 60)")  // FR-028
assertTrue(minFPS >= 30.0, "Minimum FPS: $minFPS (minimum: 30)") // FR-029
```

**Expected Result**: 60 FPS average, 30 FPS minimum

**Acceptance Criteria** (FR-028, FR-029):

- Modern hardware: ≥60 FPS
- Average hardware: ≥30 FPS
- No stuttering or frame drops

## Success Criteria

**All tests must pass for feature acceptance**:

- ✅ World generation (1,024 chunks in <3 seconds)
- ✅ Player spawns on solid ground
- ✅ WASD movement with collision detection
- ✅ Mouse camera control with pitch clamping
- ✅ Break blocks (raycast + instant break + inventory update)
- ✅ Place blocks (raycast + placement + collision check)
- ✅ Flight toggle (F key + vertical movement)
- ✅ Save world (<10MB, successful localStorage write)
- ✅ Load world (state fully restored)
- ✅ Performance (60 FPS target, 30 FPS minimum)

## Running the Quickstart

```bash
# Build and run VoxelCraft example
./gradlew :examples:voxelcraft:runJs

# Or run tests
./gradlew :examples:voxelcraft:jsTest
```

**Browser Compatibility**:

- Chrome 56+ ✅
- Firefox 51+ ✅
- Safari 15+ ✅
- Edge 79+ ✅

**Known Limitations (v1)**:

- Single world only (no multiple save slots)
- No day/night cycle
- No sound effects
- First-person view only (no third-person)
- Desktop/laptop only (mobile support in v2)
