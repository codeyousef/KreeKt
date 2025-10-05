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

    var isGeneratingTerrain = false
        private set

    suspend fun generateTerrain(onProgress: ((Int, Int) -> Unit)? = null) {
        isGeneratingTerrain = true
        val positions = ChunkPosition.allChunks()
        val total = positions.size
        val batchSize = 1  // Process 1 chunk at a time to maintain 60 FPS

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
        isGeneratingTerrain = false
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
        // Skip mesh generation during terrain generation for 60 FPS
        if (!isGeneratingTerrain) {
            // Process chunks progressively to avoid blocking the main thread
            // Mesh generation takes ~100ms per chunk, so process 1 at a time
            val chunksPerFrame = 1

            val dirtyChunks = chunks.values.filter { it.isDirty }
            val processedChunks = dirtyChunks.take(chunksPerFrame)

            if (processedChunks.isNotEmpty()) {
                console.log("Processing ${processedChunks.size} dirty chunks (${dirtyChunks.size} total dirty, ${scene.children.size} in scene)")
            }

            processedChunks.forEach { chunk ->
                // Remove old mesh from scene if it exists
                chunk.mesh?.let {
                    console.log("Removing old mesh for chunk ${chunk.position}")
                    scene.remove(it)
                }

                // Regenerate mesh
                chunk.regenerateMesh()

                // Add new mesh to scene
                chunk.mesh?.let {
                    console.log("Adding mesh for chunk ${chunk.position} to scene")
                    scene.add(it)
                } ?: console.warn("Chunk ${chunk.position} has no mesh after regeneration!")
            }
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
