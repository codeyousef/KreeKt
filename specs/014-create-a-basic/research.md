# Research & Technology Decisions

**Feature**: VoxelCraft - Basic Minecraft Clone
**Date**: 2025-10-04
**Status**: Complete

## Research Areas

### 1. Terrain Generation Algorithm

**Decision**: Simplex Noise with 2D height map + 3D cave noise

**Rationale**:

- Simplex noise is faster and has fewer directional artifacts than Perlin noise
- 2D noise for terrain height creates natural-looking hills and valleys
- 3D noise for caves provides organic cave systems
- Well-suited for JavaScript performance (no complex fractal calculations)

**Implementation Approach**:

```kotlin
// Pseudo-code
fun generateTerrain(chunkX: Int, chunkZ: Int, seed: Long): ChunkData {
    val noise2D = SimplexNoise(seed)
    val noise3D = SimplexNoise(seed + 1)

    for (x in 0..15) {
        for (z in 0..15) {
            val worldX = chunkX * 16 + x
            val worldZ = chunkZ * 16 + z

            // Height map (0-1 normalized)
            val height = noise2D.eval(worldX * 0.01, worldZ * 0.01)
            val terrainHeight = (height * 60 + 64).toInt() // Range: 64-124

            for (y in 0..255) {
                val caveNoise = noise3D.eval(worldX * 0.05, y * 0.05, worldZ * 0.05)
                val isCave = caveNoise > 0.6 && y < terrainHeight - 5

                when {
                    y > terrainHeight -> setBlock(AIR)
                    isCave -> setBlock(AIR)
                    y == terrainHeight -> setBlock(GRASS)
                    y > terrainHeight - 4 -> setBlock(DIRT)
                    else -> setBlock(STONE)
                }
            }
        }
    }
}
```

**Libraries Considered**:

- Custom implementation using Stefan Gustavson's Simplex Noise algorithm (chosen)
- OpenSimplex2 (more complex, unnecessary for basic terrain)
- FastNoise Lite (C++ port, harder to integrate with Kotlin/JS)

**Alternatives Considered**:

- **Diamond-Square Algorithm**: Too blocky for Minecraft-style terrain
- **Voronoi Diagrams**: Good for biomes but overkill for basic version
- **Value Noise**: Inferior quality to Simplex noise

**Tree Placement**:

- Random distribution using world seed
- Minimum spacing: 8 blocks between trees
- Only place on GRASS blocks at terrain height
- Simple 5-block tall tree: Wood trunk + Leaves cross pattern

**Cave Generation**:

- 3D Simplex noise threshold (>0.6 = air)
- Only carve caves below surface (y < terrainHeight - 5)
- Prevents surface holes and floating grass

---

### 2. Chunk Rendering Optimization

**Decision**: Greedy Meshing + Face Culling

**Rationale**:

- Reduces draw calls dramatically (10-100x fewer triangles)
- Culls hidden internal faces (blocks surrounded by other blocks)
- Greedy meshing combines adjacent faces into larger quads
- Critical for 60 FPS with 1,024 chunks

**Implementation Approach**:

1. **Face Culling**:

```kotlin
fun shouldRenderFace(x: Int, y: Int, z: Int, direction: Direction): Boolean {
    val neighborBlock = getBlock(x + direction.dx, y + direction.dy, z + direction.dz)
    return neighborBlock == null || neighborBlock.type == AIR || neighborBlock.type.isTransparent
}
```

2. **Greedy Meshing Algorithm**:

```kotlin
fun generateChunkMesh(chunk: Chunk): BufferGeometry {
    val vertices = mutableListOf<Float>()
    val indices = mutableListOf<Int>()

    // For each axis (X, Y, Z)
    for (axis in Axis.values()) {
        // Sweep through chunk in slices perpendicular to axis
        for (sliceIndex in 0..15) {
            val mask = Array(16) { BooleanArray(256) }

            // Build mask of visible faces in this slice
            for (x in 0..15) {
                for (y in 0..255) {
                    val blockType = getBlock(x, y, sliceIndex)
                    val neighborType = getBlock(x + axis.dx, y + axis.dy, sliceIndex + axis.dz)
                    mask[x][y] = shouldRenderFace(blockType, neighborType)
                }
            }

            // Greedily merge adjacent faces
            for (x in 0..15) {
                for (y in 0..255) {
                    if (!mask[x][y]) continue

                    // Find width (along X)
                    var width = 1
                    while (x + width <= 15 && mask[x + width][y]) {
                        width++
                    }

                    // Find height (along Y)
                    var height = 1
                    while (y + height <= 255 && allTrue(mask, x, y + height, width)) {
                        height++
                    }

                    // Generate quad for merged faces
                    addQuad(vertices, indices, x, y, width, height, axis, sliceIndex)

                    // Clear mask for merged area
                    clearMask(mask, x, y, width, height)
                }
            }
        }
    }

    return BufferGeometry().apply {
        setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3))
        setIndex(indices.toIntArray())
    }
}
```

**Frustum Culling**:

- Use KreeKt's built-in camera frustum
- Only render chunks intersecting frustum
- Check bounding box of each chunk (16x256x16)

**Chunk Update Strategy**:

- **Full rebuild**: When multiple blocks change (>10)
- **Partial update**: When 1-10 blocks change, rebuild only affected sections
- **Dirty flag**: Mark chunks for rebuild on next frame
- **Worker thread**: Generate meshes in background (future optimization)

**Alternatives Considered**:

- **Naive rendering** (all block faces): 67M blocks × 6 faces = 402M triangles (too slow)
- **Simple face culling only**: Still ~10-20M triangles visible
- **Marching cubes**: Smooth terrain (not Minecraft-style)

---

### 3. LocalStorage Compression

**Decision**: Run-Length Encoding (RLE) + gzip compression via pako.js

**Rationale**:

- Voxel data is highly compressible (large runs of identical blocks)
- RLE reduces 16x16x256 = 65,536 bytes to ~5,000-10,000 bytes (90% reduction)
- gzip further reduces by 50-70%
- Final size: ~2-5KB per chunk, ~2-5MB for full world (within localStorage limits)

**Implementation Approach**:

1. **Chunk Serialization**:

```kotlin
data class SerializedChunk(
    val chunkX: Int,
    val chunkZ: Int,
    val blocks: ByteArray // RLE-encoded block IDs
)

fun Chunk.serialize(): ByteArray {
    val rle = mutableListOf<Pair<Byte, Int>>() // (blockID, count)
    var currentType = blocks[0]
    var count = 1

    for (i in 1 until blocks.size) {
        if (blocks[i] == currentType) {
            count++
        } else {
            rle.add(currentType to count)
            currentType = blocks[i]
            count = 1
        }
    }
    rle.add(currentType to count) // Last run

    // Encode as bytes: [blockID, count, blockID, count, ...]
    return rle.flatMap { (id, cnt) -> listOf(id, cnt.toByte()) }.toByteArray()
}
```

2. **LocalStorage Save**:

```kotlin
fun WorldStorage.save(world: VoxelWorld) {
    val chunkData = world.chunks.map { it.serialize() }
    val json = Json.encodeToString(SerializedWorld(
        seed = world.seed,
        playerPos = world.player.position,
        playerRot = world.player.rotation,
        chunks = chunkData
    ))

    val compressed = pako.gzip(json.encodeToByteArray())
    localStorage.setItem("voxelcraft_world", compressed.toBase64())
}
```

**Libraries**:

- **pako.js**: Pure JavaScript gzip implementation (fast, no dependencies)
- Integration via Kotlin/JS `external` declarations

**Error Handling**:

- Try-catch around localStorage.setItem (quota exceeded)
- Display error message to player if save fails
- Periodic auto-save every 30 seconds

**Alternatives Considered**:

- **No compression**: 67MB for full world (exceeds 10MB localStorage limit)
- **Delta compression**: Complex, overkill for single-player creative mode
- **IndexedDB**: More complex API, unnecessary for 5MB data

---

### 4. Block Textures & Rendering

**Decision**: Texture Atlas + Simple Per-Face Lighting

**Rationale**:

- Texture atlas minimizes WebGL texture switches (better performance)
- Per-face lighting (ambient occlusion) adds depth without complexity
- Simple approach: 4 brightness levels based on face direction

**Texture Atlas Layout**:

```
[Grass Top][Grass Side][Dirt][Stone]
[Wood Side][Wood Top  ][Sand][Leaves]
[Water    ][         ][     ][       ]
```

- 4x3 grid = 12 texture slots (expandable to 16x16 for more block types)
- Each texture: 16x16 pixels
- Full atlas: 64x48 pixels (tiny, fast to load)

**UV Mapping**:

```kotlin
fun getUVForBlockFace(blockType: BlockType, face: Face): UV {
    val (u, v) = when (blockType) {
        GRASS -> when (face) {
            Face.TOP -> 0 to 0      // Grass top texture
            Face.BOTTOM -> 2 to 0   // Dirt texture
            else -> 1 to 0          // Grass side texture
        }
        DIRT -> 2 to 0
        STONE -> 3 to 0
        WOOD -> when (face) {
            Face.TOP, Face.BOTTOM -> 5 to 0  // Wood rings
            else -> 4 to 0                    // Wood bark
        }
        SAND -> 6 to 0
        LEAVES -> 7 to 0
        WATER -> 8 to 0
        else -> 0 to 0
    }

    // Convert to normalized UV coordinates (0-1)
    val atlasWidth = 4.0
    val atlasHeight = 3.0
    return UV(
        u0 = u / atlasWidth, v0 = v / atlasHeight,
        u1 = (u + 1) / atlasWidth, v1 = (v + 1) / atlasHeight
    )
}
```

**Lighting Model**:

```kotlin
fun getFaceBrightness(face: Face): Float = when (face) {
    Face.TOP -> 1.0      // Brightest (sunlight)
    Face.BOTTOM -> 0.5   // Darkest (shadow)
    Face.NORTH, Face.SOUTH -> 0.8
    Face.EAST, Face.WEST -> 0.6
}
```

**Ambient Occlusion**:

- Optional enhancement: Check adjacent blocks
- Darken vertex if 2+ adjacent blocks are solid
- Adds depth to block edges

**Alternatives Considered**:

- **Individual textures**: 50+ texture switches per frame (slow)
- **Procedural textures**: Harder to achieve Minecraft aesthetic
- **Advanced lighting (shadows)**: Too complex for v1

---

### 5. Input Handling Best Practices

**Decision**: Pointer Lock API + Keyboard Events + Touch fallback

**Rationale**:

- Pointer Lock API essential for FPS camera control
- Standard keyboard events for WASD movement
- Touch support ensures mobile compatibility (future)

**Implementation Approach**:

1. **Pointer Lock for Mouse Look**:

```kotlin
external interface PointerLockAPI {
    fun requestPointerLock()
    fun exitPointerLock()
    val pointerLockElement: Element?
}

class CameraController(val camera: PerspectiveCamera) {
    private val mouseSensitivity = 0.002

    init {
        canvas.addEventListener("click") {
            canvas.requestPointerLock()
        }

        document.addEventListener("pointerlockchange") {
            if (document.pointerLockElement == canvas) {
                document.addEventListener("mousemove", ::onMouseMove)
            } else {
                document.removeEventListener("mousemove", ::onMouseMove)
            }
        }
    }

    private fun onMouseMove(event: MouseEvent) {
        val dx = event.movementX * mouseSensitivity
        val dy = event.movementY * mouseSensitivity

        camera.rotation.y -= dx  // Yaw (left/right)
        camera.rotation.x -= dy  // Pitch (up/down)
        camera.rotation.x = camera.rotation.x.coerceIn(-PI/2, PI/2) // Clamp pitch
    }
}
```

2. **Keyboard for Movement**:

```kotlin
class PlayerController(val player: Player) {
    private val keys = mutableSetOf<String>()

    init {
        document.addEventListener("keydown") { event ->
            keys.add(event.key.toLowerCase())
        }
        document.addEventListener("keyup") { event ->
            keys.remove(event.key.toLowerCase())
        }
    }

    fun update(deltaTime: Float) {
        val moveSpeed = if (player.isFlying) 10.0f else 4.3f // Minecraft speeds
        val direction = Vector3()

        if ("w" in keys) direction.z -= 1
        if ("s" in keys) direction.z += 1
        if ("a" in keys) direction.x -= 1
        if ("d" in keys) direction.x += 1
        if (player.isFlying) {
            if (" " in keys) direction.y += 1  // Spacebar = up
            if ("shift" in keys) direction.y -= 1  // Shift = down
        }

        direction.normalize().multiplyScalar(moveSpeed * deltaTime)
        direction.applyQuaternion(camera.quaternion) // Rotate by camera direction

        player.move(direction)
    }
}
```

3. **Touch Support (Future)**:

- Virtual joystick for movement (left side)
- Touch-drag for camera (right side)
- Tap to break/place blocks

**Alternatives Considered**:

- **Mouse delta without Pointer Lock**: Cursor hits screen edges
- **Canvas-relative mouse**: Requires click-and-drag (awkward)
- **Gamepad API**: Nice-to-have but not essential for v1

---

### 6. Performance Profiling

**Decision**: Chrome DevTools + Custom FPS Counter + WebGL Stats

**Rationale**:

- Chrome DevTools Performance tab shows frame timing
- Custom FPS counter visible to player (validates FR-028/FR-029)
- WebGL stats extension shows draw calls and triangles

**FPS Counter Implementation**:

```kotlin
class FPSCounter {
    private var lastTime = Date.now()
    private var frames = 0
    private var fps = 60.0

    fun update() {
        frames++
        val currentTime = Date.now()
        val elapsed = currentTime - lastTime

        if (elapsed >= 1000) { // Update every second
            fps = (frames * 1000.0 / elapsed)
            frames = 0
            lastTime = currentTime

            // Update HUD
            document.getElementById("fps")?.textContent = "FPS: ${fps.toInt()}"
        }
    }
}
```

**Performance Benchmarks**:

```kotlin
@Test
fun testPerformance() {
    val world = VoxelWorld(seed = 12345)
    val renderer = WebGL2Renderer(canvas)

    val frameT imes = mutableListOf<Double>()
    for (frame in 1..300) { // 5 seconds at 60 FPS
        val startTime = performance.now()

        renderer.render(world.scene, world.camera)

        val frameTime = performance.now() - startTime
        frameTimes.add(frameTime)
    }

    val avgFPS = 1000.0 / frameTimes.average()
    val minFPS = 1000.0 / frameTimes.maxOrNull()!!

    // Constitutional requirement: 60 FPS target
    assertTrue(avgFPS >= 60.0, "Average FPS below target: $avgFPS")

    // Constitutional requirement: 30 FPS minimum
    assertTrue(minFPS >= 30.0, "Minimum FPS below acceptable: $minFPS")
}
```

**Profiling Tools**:

- **Chrome DevTools Performance**: Identify bottlenecks (rendering, GC, etc.)
- **chrome://tracing**: Low-level GPU profiling
- **WebGL Inspector**: Texture/shader analysis
- **about:tracing** (Firefox): Similar to Chrome tracing

**Performance Targets**:

- **Draw calls**: <200 per frame (chunk batching)
- **Triangles**: <500K per frame (greedy meshing)
- **Memory**: <512MB total (world + rendering)
- **Frame time**: <16.67ms for 60 FPS, <33.33ms for 30 FPS

**Alternatives Considered**:

- **stats.js library**: Adds dependency, easy to implement custom
- **Browser console.time**: Less precise than performance.now()

---

## Summary of Decisions

| Area      | Decision                             | Primary Benefit                                   |
|-----------|--------------------------------------|---------------------------------------------------|
| Terrain   | Simplex Noise (2D height + 3D caves) | Fast, natural-looking, JS-compatible              |
| Rendering | Greedy Meshing + Face Culling        | 10-100x fewer triangles, 60 FPS achievable        |
| Storage   | RLE + gzip (pako.js)                 | 90%+ compression, fits in localStorage            |
| Textures  | Texture Atlas (64x48)                | Minimal texture switches, fast loading            |
| Input     | Pointer Lock + Keyboard              | Standard FPS controls, good UX                    |
| Profiling | DevTools + Custom FPS Counter        | Validates constitutional performance requirements |

## Open Questions (Deferred to Implementation)

1. **Day/Night Cycle**: Not in FR-006 clarification → Defer to v2
2. **Unbreakable Blocks (Bedrock)**: Not clarified → Use bedrock at Y=0
3. **Multiple Worlds**: Not clarified → Single world for v1
4. **Inventory UI Layout**: Not clarified → Simple horizontal hotbar
5. **Third-Person View**: Not clarified → First-person only

All critical technical decisions are resolved. Ready for Phase 1 (Design & Contracts).
