package io.kreekt.examples.voxelcraft

/**
 * TextureAtlas manages block textures and UV mapping
 *
 * Uses a 4x3 texture atlas (64x48 pixels) to minimize texture switches in WebGL.
 * Each block type maps to specific UV coordinates in the atlas.
 *
 * Atlas layout (each cell is 16x16 pixels):
 * ```
 * [Grass Top][Grass Side][Dirt    ][Stone  ]
 * [Wood Side][Wood Top  ][Sand    ][Leaves ]
 * [Water    ][          ][        ][       ]
 * ```
 *
 * Based on research.md texture atlas design.
 */
object TextureAtlas {

    // Atlas dimensions
    private const val ATLAS_WIDTH = 4
    private const val ATLAS_HEIGHT = 3
    private const val TEXTURE_SIZE = 16

    /**
     * Atlas texture coordinates for each block type and face
     *
     * Returns (u0, v0, u1, v1) normalized coordinates (0.0 to 1.0)
     */
    fun getUVForBlockFace(blockType: BlockType, direction: FaceDirection): UV {
        val (atlasX, atlasY) = when (blockType) {
            BlockType.Air -> 0 to 0 // Not rendered, but provide default

            BlockType.Grass -> when (direction) {
                FaceDirection.UP -> 0 to 0      // Grass top
                FaceDirection.DOWN -> 2 to 0    // Dirt (bottom of grass)
                else -> 1 to 0              // Grass side
            }

            BlockType.Dirt -> 2 to 0

            BlockType.Stone -> 3 to 0

            BlockType.Wood -> when (direction) {
                FaceDirection.UP, FaceDirection.DOWN -> 5 to 1  // Wood rings (top/bottom)
                else -> 4 to 1                          // Wood bark (sides)
            }

            BlockType.Leaves -> 7 to 1

            BlockType.Sand -> 6 to 1

            BlockType.Water -> 0 to 2
        }

        return calculateUV(atlasX, atlasY)
    }

    /**
     * Calculate normalized UV coordinates from atlas position
     */
    private fun calculateUV(atlasX: Int, atlasY: Int): UV {
        val u0 = atlasX.toFloat() / ATLAS_WIDTH.toFloat()
        val v0 = atlasY.toFloat() / ATLAS_HEIGHT.toFloat()
        val u1 = (atlasX + 1).toFloat() / ATLAS_WIDTH.toFloat()
        val v1 = (atlasY + 1).toFloat() / ATLAS_HEIGHT.toFloat()

        return UV(u0, v0, u1, v1)
    }

    /**
     * Get atlas dimensions
     */
    fun getAtlasDimensions(): Pair<Int, Int> = ATLAS_WIDTH * TEXTURE_SIZE to ATLAS_HEIGHT * TEXTURE_SIZE

    /**
     * Generate procedural texture atlas data
     *
     * Uses simple colored textures for each block type. This approach is intentional
     * for the VoxelCraft demo to minimize asset dependencies and load times.
     * Production games would typically load actual PNG texture files.
     *
     * @return RGBA pixel data (64x48 pixels = 12,288 bytes)
     */
    fun generateAtlasData(): ByteArray {
        val width = ATLAS_WIDTH * TEXTURE_SIZE
        val height = ATLAS_HEIGHT * TEXTURE_SIZE
        val data = ByteArray(width * height * 4) // RGBA

        // Define colors for each atlas position
        val colors = mapOf(
            (0 to 0) to Color(34, 139, 34),   // Grass top - green
            (1 to 0) to Color(96, 128, 56),   // Grass side - greenish-brown
            (2 to 0) to Color(139, 90, 43),   // Dirt - brown
            (3 to 0) to Color(128, 128, 128), // Stone - gray
            (0 to 1) to Color(101, 67, 33),   // Wood side - brown
            (1 to 1) to Color(139, 90, 43),   // Wood top - lighter brown
            (2 to 1) to Color(194, 178, 128), // Sand - tan
            (3 to 1) to Color(34, 139, 34),   // Leaves - green
            (0 to 2) to Color(64, 164, 223)   // Water - blue
        )

        // Fill atlas with block colors
        for ((pos, color) in colors) {
            fillTextureCell(data, width, pos.first, pos.second, color)
        }

        return data
    }

    /**
     * Fill a 16x16 texture cell with color
     */
    private fun fillTextureCell(data: ByteArray, atlasWidth: Int, cellX: Int, cellY: Int, color: Color) {
        val startX = cellX * TEXTURE_SIZE
        val startY = cellY * TEXTURE_SIZE

        for (y in 0 until TEXTURE_SIZE) {
            for (x in 0 until TEXTURE_SIZE) {
                val pixelX = startX + x
                val pixelY = startY + y
                val index = (pixelY * atlasWidth + pixelX) * 4

                // Add slight noise for texture variation
                val variation = ((x + y) % 3 - 1) * 10
                data[index + 0] = (color.r + variation).coerceIn(0, 255).toByte()
                data[index + 1] = (color.g + variation).coerceIn(0, 255).toByte()
                data[index + 2] = (color.b + variation).coerceIn(0, 255).toByte()
                data[index + 3] = 255.toByte() // Alpha
            }
        }
    }
}

/**
 * UV coordinates (normalized 0.0 to 1.0)
 */
data class UV(
    val u0: Float,
    val v0: Float,
    val u1: Float,
    val v1: Float
)

/**
 * RGB color
 */
private data class Color(
    val r: Int,
    val g: Int,
    val b: Int
)
