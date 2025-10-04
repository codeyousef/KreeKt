package io.kreekt.examples.voxelcraft

import kotlinx.serialization.Serializable

/**
 * WorldState serialization model for localStorage persistence
 *
 * Provides serialization/deserialization of the complete game state:
 * - World seed (for regenerating unmodified chunks)
 * - Player state (position, rotation, flight)
 * - Modified chunks only (optimization)
 *
 * Contract: storage-api.yaml WorldState schema
 * Data model: data-model.md Section 7
 */
@Serializable
data class WorldState(
    val seed: Long,
    val playerPosition: SerializableVector3,
    val playerRotation: SerializableRotation,
    val isFlying: Boolean,
    val chunks: List<SerializedChunk>
) {
    companion object {
        /**
         * Create WorldState from VoxelWorld
         *
         * Only serializes non-empty chunks to reduce storage size.
         *
         * @param world VoxelWorld to serialize
         * @return WorldState with compressed chunk data
         */
        fun from(world: VoxelWorld): WorldState {
            // TODO: Only save modified/non-empty chunks (optimization)
            val serializedChunks = emptyList<SerializedChunk>() // Stub for now

            return WorldState(
                seed = world.seed,
                playerPosition = SerializableVector3(world.player.position),
                playerRotation = SerializableRotation(world.player.rotation),
                isFlying = world.player.isFlying,
                chunks = serializedChunks
            )
        }
    }

    /**
     * Restore VoxelWorld from saved state
     *
     * Regenerates world from seed, then applies saved chunk modifications.
     *
     * @return Restored VoxelWorld instance
     */
    fun restore(): VoxelWorld {
        val world = VoxelWorld(seed)

        // Restore player state
        world.player.position.set(playerPosition.x, playerPosition.y, playerPosition.z)
        world.player.rotation.set(playerRotation.pitch, playerRotation.yaw)
        world.player.isFlying = isFlying

        // TODO: Apply chunk modifications from chunks list

        return world
    }
}

/**
 * Serializable Vector3 for position data
 */
@Serializable
data class SerializableVector3(
    val x: Double,
    val y: Double,
    val z: Double
) {
    constructor(pos: Position) : this(pos.x, pos.y, pos.z)
}

/**
 * Serializable Rotation for camera orientation
 */
@Serializable
data class SerializableRotation(
    val pitch: Double,
    val yaw: Double
) {
    constructor(rot: Rotation) : this(rot.pitch, rot.yaw)
}

/**
 * Serialized chunk with RLE-compressed block data
 */
@Serializable
data class SerializedChunk(
    val chunkX: Int,
    val chunkZ: Int,
    val compressedBlocks: ByteArray // RLE-encoded block data
) {
    companion object {
        /**
         * Create SerializedChunk from Chunk
         *
         * @param chunk Chunk to serialize
         * @return SerializedChunk with compressed data
         */
        fun from(chunk: Chunk): SerializedChunk {
            return SerializedChunk(
                chunkX = chunk.position.chunkX,
                chunkZ = chunk.position.chunkZ,
                compressedBlocks = chunk.serialize()
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SerializedChunk

        if (chunkX != other.chunkX) return false
        if (chunkZ != other.chunkZ) return false
        if (!compressedBlocks.contentEquals(other.compressedBlocks)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chunkX
        result = 31 * result + chunkZ
        result = 31 * result + compressedBlocks.contentHashCode()
        return result
    }
}
