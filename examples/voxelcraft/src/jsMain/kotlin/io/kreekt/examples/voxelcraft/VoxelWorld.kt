package io.kreekt.examples.voxelcraft

import io.kreekt.core.scene.Scene
import kotlinx.coroutines.delay

/**
 * VoxelWorld - Main world entity managing chunks and player
 *
 * Uses KreeKt Scene to manage chunk meshes for rendering.
 *
 * Data model: data-model.md Section 4
 */
class VoxelWorld(val seed: Long) {
    val chunks = mutableMapOf<ChunkPosition, Chunk>()
    val player = Player(this)
    private val generator = TerrainGenerator(seed)
    val scene = Scene()

    val chunkCount: Int get() = chunks.size
    var isGenerated = false
        private set

    suspend fun generateTerrain(onProgress: ((Int, Int) -> Unit)? = null) {
        val positions = ChunkPosition.allChunks()
        val total = positions.size
        val batchSize = 32  // Process 32 chunks at a time (~3% progress per update)

        positions.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            batch.forEachIndexed { indexInBatch, pos ->
                val chunk = Chunk(pos, this)
                generator.generate(chunk)
                chunks[pos] = chunk

                val currentIndex = batchIndex * batchSize + indexInBatch + 1
                onProgress?.invoke(currentIndex, total)
            }

            // Yield to JavaScript event loop to allow DOM updates and rendering
            // Using delay(0) ensures we yield to the event loop properly
            delay(0)
        }

        // Spawn player above terrain
        player.position.set(0.0f, 100.0f, 0.0f)
        isGenerated = true
    }

    fun getBlock(x: Int, y: Int, z: Int): BlockType? {
        if (x !in -256..255 || y !in 0..255 || z !in -256..255) return null

        val chunkPos = ChunkPosition.fromWorldCoordinates(x, z)
        val chunk = chunks[chunkPos] ?: return null

        val (localX, localZ) = chunkPos.toLocalCoordinates(x, z)
        return chunk.getBlock(localX, y, localZ)
    }

    fun setBlock(x: Int, y: Int, z: Int, type: BlockType): Boolean {
        if (x !in -256..255 || y !in 0..255 || z !in -256..255) return false

        val chunkPos = ChunkPosition.fromWorldCoordinates(x, z)
        val chunk = chunks[chunkPos] ?: return false

        val (localX, localZ) = chunkPos.toLocalCoordinates(x, z)
        chunk.setBlock(localX, y, localZ, type)
        return true
    }

    fun getChunk(position: ChunkPosition): Chunk? {
        return chunks[position]
    }

    fun update(deltaTime: Float) {
        // Update dirty chunks (regenerate meshes and add to scene)
        chunks.values.filter { it.isDirty }.forEach { chunk ->
            // Remove old mesh from scene if it exists
            chunk.mesh?.let { scene.remove(it) }

            // Regenerate mesh
            chunk.regenerateMesh()

            // Add new mesh to scene
            chunk.mesh?.let { scene.add(it) }
        }

        // Update player
        player.update(deltaTime)
    }

    fun dispose() {
        // Remove all chunks from scene and cleanup
        chunks.values.forEach { chunk ->
            chunk.mesh?.let { scene.remove(it) }
            chunk.dispose()
        }
        chunks.clear()
        scene.clear()
    }
}
