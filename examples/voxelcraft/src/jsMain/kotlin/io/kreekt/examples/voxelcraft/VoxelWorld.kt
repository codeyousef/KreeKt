package io.kreekt.examples.voxelcraft

import io.kreekt.core.scene.Scene

/**
 * VoxelWorld - Main world entity managing chunks and player
 *
 * Uses KreeKt Scene to manage chunk meshes for rendering.
 *
 * Data model: data-model.md Section 4
 */
class VoxelWorld(val seed: Long) {
    private val chunks = mutableMapOf<ChunkPosition, Chunk>()
    val player = Player(this)
    private val generator = TerrainGenerator(seed)
    val scene = Scene()

    val chunkCount: Int get() = chunks.size
    var isGenerated = false
        private set

    fun generateTerrain(onProgress: ((Int, Int) -> Unit)? = null) {
        val positions = ChunkPosition.allChunks()
        val total = positions.size

        positions.forEachIndexed { index, pos ->
            val chunk = Chunk(pos, this)
            generator.generate(chunk)
            chunks[pos] = chunk

            // Report progress
            onProgress?.invoke(index + 1, total)
        }

        // Spawn player above terrain
        player.position.set(0.0, 100.0, 0.0)
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
}
