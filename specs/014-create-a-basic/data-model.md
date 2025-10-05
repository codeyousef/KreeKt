# Data Model: VoxelCraft

**Feature**: Basic Minecraft Clone
**Date**: 2025-10-04
**Status**: Complete

## Entity Definitions

### 1. BlockType (Sealed Class)

```kotlin
sealed class BlockType(val id: Byte, val name: String, val isTransparent: Boolean) {
    object Air : BlockType(0, "Air", true)
    object Grass : BlockType(1, "Grass", false)
    object Dirt : BlockType(2, "Dirt", false)
    object Stone : BlockType(3, "Stone", false)
    object Wood : BlockType(4, "Wood", false)
    object Leaves : BlockType(5, "Leaves", true)
    object Sand : BlockType(6, "Sand", false)
    object Water : BlockType(7, "Water", true)

    companion object {
        fun fromId(id: Byte): BlockType = when (id) {
            0.toByte() -> Air
            1.toByte() -> Grass
            2.toByte() -> Dirt
            3.toByte() -> Stone
            4.toByte() -> Wood
            5.toByte() -> Leaves
            6.toByte() -> Sand
            7.toByte() -> Water
            else -> Air
        }
    }
}
```

**Validation Rules**:

- ID must be 0-7 (8 block types total)
- Name must be unique
- Transparent blocks (Air, Leaves, Water) don't render adjacent faces

---

### 2. ChunkPosition (Data Class)

```kotlin
data class ChunkPosition(
    val chunkX: Int, // Range: -16 to 15 (32 chunks wide)
    val chunkZ: Int  // Range: -16 to 15 (32 chunks deep)
) {
    init {
        require(chunkX in -16..15) { "chunkX must be in range -16..15" }
        require(chunkZ in -16..15) { "chunkZ must be in range -16..15" }
    }

    fun toWorldX(): Int = chunkX * 16
    fun toWorldZ(): Int = chunkZ * 16
}
```

**Relationships**:

- Maps to 16x16 block region in world coordinates
- Used as key in World's chunk grid map

---

### 3. Chunk (Entity)

```kotlin
class Chunk(
    val position: ChunkPosition,
    val world: VoxelWorld
) {
    // 16x16x256 = 65,536 blocks, stored as flat ByteArray
    private val blocks = ByteArray(16 * 16 * 256) { BlockType.Air.id }

    var mesh: BufferGeometry? = null
        private set

    var isDirty: Boolean = true
        private set

    fun getBlock(x: Int, y: Int, z: Int): BlockType {
        require(x in 0..15 && y in 0..255 && z in 0..15) { "Block position out of chunk bounds" }
        val index = x + z * 16 + y * 16 * 16
        return BlockType.fromId(blocks[index])
    }

    fun setBlock(x: Int, y: Int, z: Int, type: BlockType) {
        require(x in 0..15 && y in 0..255 && z in 0..15) { "Block position out of chunk bounds" }
        val index = x + z * 16 + y * 16 * 16
        blocks[index] = type.id
        isDirty = true
    }

    fun regenerateMesh() {
        mesh = ChunkMeshGenerator.generate(this)
        isDirty = false
    }

    fun serialize(): ByteArray {
        // RLE compression: [blockID, count, blockID, count, ...]
        return RunLengthEncoding.encode(blocks)
    }

    fun deserialize(data: ByteArray) {
        val decoded = RunLengthEncoding.decode(data)
        require(decoded.size == blocks.size) { "Invalid chunk data size" }
        decoded.copyInto(blocks)
        isDirty = true
    }
}
```

**State Transitions**:

- `isDirty = false` → `isDirty = true`: When block is modified
- `isDirty = true` → `isDirty = false`: After mesh regeneration

**Validation Rules**:

- Block coordinates must be 0-15 (X/Z) and 0-255 (Y)
- Mesh regeneration only when dirty flag is true

---

### 4. VoxelWorld (Entity)

```kotlin
class VoxelWorld(
    val seed: Long,
    val scene: Scene
) {
    private val chunks = mutableMapOf<ChunkPosition, Chunk>()

    val player: Player = Player(this)
    val camera: PerspectiveCamera = PerspectiveCamera(
        fov = 75.0,
        aspect = window.innerWidth / window.innerHeight,
        near = 0.1,
        far = 1000.0
    )

    init {
        generateTerrain()
    }

    fun generateTerrain() {
        val generator = TerrainGenerator(seed)
        for (chunkX in -16..15) {
            for (chunkZ in -16..15) {
                val pos = ChunkPosition(chunkX, chunkZ)
                val chunk = Chunk(pos, this)
                generator.generate(chunk)
                chunks[pos] = chunk
                chunk.regenerateMesh()
                scene.add(chunk.mesh!!)
            }
        }
    }

    fun getBlock(x: Int, y: Int, z: Int): BlockType? {
        if (x !in -256..255 || y !in 0..255 || z !in -256..255) return null

        val chunkX = Math.floorDiv(x, 16)
        val chunkZ = Math.floorDiv(z, 16)
        val chunk = chunks[ChunkPosition(chunkX, chunkZ)] ?: return null

        val localX = Math.floorMod(x, 16)
        val localZ = Math.floorMod(z, 16)
        return chunk.getBlock(localX, y, localZ)
    }

    fun setBlock(x: Int, y: Int, z: Int, type: BlockType): Boolean {
        if (x !in -256..255 || y !in 0..255 || z !in -256..255) return false

        val chunkX = Math.floorDiv(x, 16)
        val chunkZ = Math.floorDiv(z, 16)
        val chunk = chunks[ChunkPosition(chunkX, chunkZ)] ?: return false

        val localX = Math.floorMod(x, 16)
        val localZ = Math.floorMod(z, 16)
        chunk.setBlock(localX, y, localZ, type)
        return true
    }

    fun update(deltaTime: Float) {
        // Update dirty chunks
        chunks.values.filter { it.isDirty }.forEach { it.regenerateMesh() }

        // Update player
        player.update(deltaTime)

        // Update camera position to follow player
        camera.position.copy(player.position)
        camera.rotation.copy(player.rotation)
    }
}
```

**Relationships**:

- Contains 1,024 chunks (32x32 grid)
- Owns player entity
- Owns scene camera

**Validation Rules**:

- World bounds: X/Z = -256 to 255, Y = 0 to 255
- Chunk positions must be -16 to 15 (both X and Z)

---

### 5. Player (Entity)

```kotlin
class Player(
    val world: VoxelWorld
) {
    var position = Vector3(0.0, 100.0, 0.0)  // Spawn at Y=100
    var rotation = Euler(0.0, 0.0, 0.0)      // Pitch, Yaw, Roll
    var velocity = Vector3()
    var isFlying = false

    val inventory = Inventory()
    val boundingBox = Box3(
        Vector3(-0.3, 0.0, -0.3),  // Player width: 0.6 blocks
        Vector3(0.3, 1.8, 0.3)     // Player height: 1.8 blocks
    )

    fun move(direction: Vector3) {
        val targetPos = position.clone().add(direction)

        // Collision detection
        val collides = checkCollision(targetPos)
        if (!collides) {
            position.copy(targetPos)
        }

        // Apply gravity if not flying
        if (!isFlying) {
            velocity.y -= 9.8 * 0.016 // Gravity (9.8 m/s²)
            position.y += velocity.y

            // Ground collision
            if (isOnGround()) {
                velocity.y = 0.0
                position.y = Math.floor(position.y) + 0.01
            }
        }
    }

    fun rotate(deltaPitch: Double, deltaYaw: Double) {
        rotation.x += deltaPitch
        rotation.y += deltaYaw

        // Clamp pitch to ±90°
        rotation.x = rotation.x.coerceIn(-Math.PI / 2, Math.PI / 2)
    }

    fun toggleFlight() {
        isFlying = !isFlying
        if (isFlying) {
            velocity.y = 0.0  // Stop falling
        }
    }

    private fun checkCollision(pos: Vector3): Boolean {
        val playerBox = boundingBox.clone().translate(pos)

        // Check blocks in player bounding box
        for (y in (pos.y - 0.1).toInt()..(pos.y + 1.9).toInt()) {
            for (x in (pos.x - 0.4).toInt()..(pos.x + 0.4).toInt()) {
                for (z in (pos.z - 0.4).toInt()..(pos.z + 0.4).toInt()) {
                    val block = world.getBlock(x, y, z) ?: continue
                    if (!block.isTransparent) return true
                }
            }
        }
        return false
    }

    private fun isOnGround(): Boolean {
        val groundY = (position.y - 0.1).toInt()
        for (x in (position.x - 0.3).toInt()..(position.x + 0.3).toInt()) {
            for (z in (position.z - 0.3).toInt()..(position.z + 0.3).toInt()) {
                val block = world.getBlock(x, groundY, z) ?: continue
                if (!block.isTransparent) return true
            }
        }
        return false
    }

    fun update(deltaTime: Float) {
        // Player logic updated in PlayerController
    }
}
```

**State Transitions**:

- `isFlying = false` → `isFlying = true`: Toggle flight (F key)
- `isFlying = true` → `isFlying = false`: Toggle flight (F key)
- `velocity.y < 0` → `velocity.y = 0`: When landing on ground

**Validation Rules**:

- Position must be within world bounds (-256 to 255 XZ, 0 to 255 Y)
- Pitch rotation clamped to ±π/2 radians
- Bounding box must not intersect solid blocks

---

### 6. Inventory (Entity)

```kotlin
class Inventory {
    private val blocks = mutableMapOf<BlockType, Int>()
    var selectedBlock: BlockType = BlockType.Grass

    fun add(type: BlockType, count: Int = 1) {
        require(count > 0) { "Count must be positive" }
        blocks[type] = (blocks[type] ?: 0) + count
    }

    fun remove(type: BlockType, count: Int = 1): Boolean {
        val current = blocks[type] ?: 0
        if (current < count) return false

        blocks[type] = current - count
        if (blocks[type] == 0) blocks.remove(type)
        return true
    }

    fun getCount(type: BlockType): Int = blocks[type] ?: 0

    fun hasBlock(type: BlockType): Boolean = (blocks[type] ?: 0) > 0

    fun selectBlock(type: BlockType) {
        selectedBlock = type
    }
}
```

**Validation Rules**:

- Block counts must be non-negative
- Unlimited capacity (creative mode)
- Selected block must be a placeable type (not AIR)

---

### 7. WorldState (Serialization Model)

```kotlin
@Serializable
data class WorldState(
    val seed: Long,
    val playerPosition: SerializableVector3,
    val playerRotation: SerializableRotation,
    val isFlying: Boolean,
    val chunks: List<SerializedChunk>
) {
    companion object {
        fun from(world: VoxelWorld): WorldState {
            return WorldState(
                seed = world.seed,
                playerPosition = SerializableVector3(world.player.position),
                playerRotation = SerializableRotation(world.player.rotation),
                isFlying = world.player.isFlying,
                chunks = world.chunks.map { SerializedChunk.from(it) }
            )
        }
    }

    fun restore(): VoxelWorld {
        val world = VoxelWorld(seed, Scene())
        world.player.position.set(playerPosition.x, playerPosition.y, playerPosition.z)
        world.player.rotation.set(playerRotation.pitch, playerRotation.yaw, 0.0)
        world.player.isFlying = isFlying

        chunks.forEach { serializedChunk ->
            val chunk = world.chunks[serializedChunk.position]
            chunk?.deserialize(serializedChunk.compressedBlocks)
        }

        return world
    }
}

@Serializable
data class SerializableVector3(val x: Double, val y: Double, val z: Double) {
    constructor(v: Vector3) : this(v.x, v.y, v.z)
}

@Serializable
data class SerializableRotation(val pitch: Double, val yaw: Double) {
    constructor(e: Euler) : this(e.x, e.y)
}

@Serializable
data class SerializedChunk(
    val position: ChunkPosition,
    val compressedBlocks: ByteArray // RLE-encoded
) {
    companion object {
        fun from(chunk: Chunk): SerializedChunk {
            return SerializedChunk(
                position = chunk.position,
                compressedBlocks = chunk.serialize()
            )
        }
    }
}
```

**Validation Rules**:

- Seed must be valid Long
- Player position must be within world bounds
- Chunk count must be 1-1024
- Compressed blocks must decompress to exactly 65,536 bytes

---

## Entity Relationship Diagram

```
VoxelWorld (1)
├── chunks: Map<ChunkPosition, Chunk> (1,024)
│   └── Chunk
│       ├── position: ChunkPosition
│       ├── blocks: ByteArray[65536]
│       └── mesh: BufferGeometry?
├── player: Player (1)
│   ├── position: Vector3
│   ├── rotation: Euler
│   ├── inventory: Inventory (1)
│   │   └── blocks: Map<BlockType, Int>
│   └── boundingBox: Box3
├── camera: PerspectiveCamera (1)
└── scene: Scene (1)

BlockType (sealed class - 8 instances)
└── Air, Grass, Dirt, Stone, Wood, Leaves, Sand, Water

WorldState (serialization)
├── seed: Long
├── playerPosition: Vector3
├── playerRotation: Rotation
├── isFlying: Boolean
└── chunks: List<SerializedChunk> (1-1024)
```

---

## Data Flow

### World Generation Flow

```
1. VoxelWorld.init() → TerrainGenerator
2. TerrainGenerator.generate() → Chunk.setBlock() (65,536 times per chunk)
3. Chunk.setBlock() → isDirty = true
4. Chunk.regenerateMesh() → BufferGeometry
5. Scene.add(mesh)
```

### Block Interaction Flow

```
1. Input (mouse click) → BlockInteraction.raycast()
2. Raycast result → World coordinate (x, y, z)
3. VoxelWorld.setBlock(x, y, z, type)
4. Chunk.setBlock(localX, localY, localZ, type) → isDirty = true
5. Next frame: Chunk.regenerateMesh() → Update scene
```

### Save/Load Flow

```
Save:
1. WorldState.from(world) → Serialize entities
2. Chunk.serialize() → RLE compression
3. WorldStorage.save() → localStorage.setItem()

Load:
1. localStorage.getItem() → WorldStorage.load()
2. WorldState.restore() → Deserialize entities
3. Chunk.deserialize() → Decompress RLE → blocks ByteArray
4. Chunk.regenerateMesh() → Render world
```

---

## Performance Considerations

1. **Memory Usage**:
    - 1,024 chunks × 65,536 bytes = ~67MB uncompressed
    - RLE compression: ~5-10KB per chunk = 5-10MB total
    - Meshes: Variable (~100-500KB per chunk depending on complexity)

2. **Dirty Flag Optimization**:
    - Only regenerate mesh when chunk is modified
    - Batch multiple block changes before regeneration

3. **Chunk Culling**:
    - Frustum culling: Only render visible chunks
    - Target: 100-200 chunks visible at once

4. **Serialization**:
    - RLE compression reduces storage by 90%+
    - gzip further reduces by 50-70%
    - Target: <5MB total in localStorage
