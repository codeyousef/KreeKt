package io.kreekt.examples.voxelcraft

import io.kreekt.geometry.BufferAttribute
import io.kreekt.geometry.BufferGeometry
import kotlinx.coroutines.yield

/**
 * ChunkMeshGenerator generates optimized meshes for chunks using greedy meshing and face culling.
 *
 * Implements the greedy meshing algorithm from research.md to reduce triangle count by 10-100x.
 * Only renders faces that are visible (not adjacent to solid blocks).
 *
 * Output: KreeKt BufferGeometry with position, normal, uv, and color attributes.
 */
object ChunkMeshGenerator {

    /**
     * Generate mesh for chunk using greedy meshing algorithm
     *
     * @param chunk The chunk to generate mesh for
     * @return BufferGeometry ready for rendering with KreeKt
     */
    suspend fun generate(chunk: Chunk): BufferGeometry {
        var operations = 0
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        var vertexOffset = 0

        // Generate faces for each of the 6 directions
        for (direction in FaceDirection.values()) {
            operations = generateFacesForDirection(
                chunk, direction, vertices, normals, uvs, colors, indices, vertexOffset, operations
            )
            vertexOffset = vertices.size / 3
        }

        // Create BufferGeometry and set attributes
        val geometry = BufferGeometry()

        geometry.setAttribute("position", BufferAttribute(vertices.toFloatArray(), 3))
        geometry.setAttribute("normal", BufferAttribute(normals.toFloatArray(), 3))
        geometry.setAttribute("uv", BufferAttribute(uvs.toFloatArray(), 2))
        geometry.setAttribute("color", BufferAttribute(colors.toFloatArray(), 3))

        // Convert indices to FloatArray for BufferAttribute
        val indexArray = indices.map { it.toFloat() }.toFloatArray()
        geometry.setIndex(BufferAttribute(indexArray, 1))

        // Compute bounding volumes for frustum culling
        geometry.computeBoundingBox()
        geometry.computeBoundingSphere()

        return geometry
    }

    /**
     * Generate all faces for a specific direction using greedy meshing
     */
    private suspend fun generateFacesForDirection(
        chunk: Chunk,
        direction: FaceDirection,
        vertices: MutableList<Float>,
        normals: MutableList<Float>,
        uvs: MutableList<Float>,
        colors: MutableList<Float>,
        indices: MutableList<Int>,
        vertexOffset: Int,
        operationsIn: Int
    ): Int {
        var operations = operationsIn
        // Determine axis-aligned dimensions
        val (uAxis, vAxis, wAxis) = DirectionHelper.getAxes(direction)

        // Sweep through slices perpendicular to direction
        for (w in 0..15) {
            // Build mask for this slice
            val mask = Array(16) { Array(256) { MaskEntry(BlockType.Air, false) } }

            for (u in 0..15) {
                for (v in 0..255) {
                    val pos = DirectionHelper.getPosition(direction, u, v, w)
                    val neighborPos = DirectionHelper.getPosition(direction, u, v, w + 1)

                    val block = chunk.getBlockSafe(pos[0], pos[1], pos[2])
                    val neighbor = chunk.getNeighborBlock(neighborPos[0], neighborPos[1], neighborPos[2])

                    // Should we render this face?
                    if (shouldRenderFace(block, neighbor)) {
                        mask[u][v] = MaskEntry(block, true)
                    }
                }
            }

            // Greedily merge adjacent faces in mask
            for (u in 0..15) {
                for (v in 0..255) {
                    if (!mask[u][v].render || mask[u][v].blockType == BlockType.Air) continue

                    val blockType = mask[u][v].blockType

                    // Find width (along u axis)
                    var width = 1
                    while (u + width <= 15 && mask[u + width][v].render &&
                        mask[u + width][v].blockType == blockType
                    ) {
                        width++
                    }

                    // Find height (along v axis)
                    var height = 1
                    var canExtend = true
                    while (v + height <= 255 && canExtend) {
                        for (du in 0 until width) {
                            if (!mask[u + du][v + height].render ||
                                mask[u + du][v + height].blockType != blockType
                            ) {
                                canExtend = false
                                break
                            }
                        }
                        if (canExtend) height++
                    }

                    // Generate quad for this merged face
                    addQuad(
                        vertices, normals, uvs, colors, indices,
                        u, v, w, width, height, direction, blockType, vertexOffset
                    )
                    operations += width * height
                    if (operations % 4096 == 0) {
                        yield()
                    }

                    // Clear mask for merged area
                    for (du in 0 until width) {
                        for (dv in 0 until height) {
                            mask[u + du][v + dv] = MaskEntry(BlockType.Air, false)
                        }
                    }
                }
            }
        }
        return operations
    }

    /**
     * Check if face should be rendered (culling hidden faces)
     */
    private fun shouldRenderFace(block: BlockType, neighbor: BlockType?): Boolean {
        if (block == BlockType.Air) return false
        if (neighbor == null) return true // Chunk boundary
        if (neighbor == BlockType.Air) return true
        if (neighbor.isTransparent) return true
        return false
    }

    /**
     * Add a quad (2 triangles) for a merged face
     */
    private fun addQuad(
        vertices: MutableList<Float>,
        normals: MutableList<Float>,
        uvs: MutableList<Float>,
        colors: MutableList<Float>,
        indices: MutableList<Int>,
        u: Int,
        v: Int,
        w: Int,
        width: Int,
        height: Int,
        direction: FaceDirection,
        blockType: BlockType,
        vertexOffset: Int
    ) {
        val currentVertexCount = vertices.size / 3

        // Get world positions for quad corners
        val corners = DirectionHelper.getQuadCorners(direction, u, v, w, width, height)

        // Add 4 vertices
        for (corner in corners) {
            vertices.add(corner[0].toFloat())
            vertices.add(corner[1].toFloat())
            vertices.add(corner[2].toFloat())
        }

        // Add normals (same for all 4 vertices)
        val normal = DirectionHelper.getNormal(direction)
        for (i in 0..3) {
            normals.add(normal[0])
            normals.add(normal[1])
            normals.add(normal[2])
        }

        // Add UVs (tiled based on quad size)
        val (u0, v0, u1, v1) = TextureAtlas.getUVForBlockFace(blockType, direction)
        uvs.add(u0)
        uvs.add(v0)
        uvs.add(u1)
        uvs.add(v0)
        uvs.add(u1)
        uvs.add(v1)
        uvs.add(u0)
        uvs.add(v1)

        // Add colors (face brightness)
        val brightness = DirectionHelper.getBrightness(direction)
        for (i in 0..3) {
            colors.add(brightness)
            colors.add(brightness)
            colors.add(brightness)
        }

        // Add indices for 2 triangles (quad)
        indices.add(currentVertexCount + 0)
        indices.add(currentVertexCount + 1)
        indices.add(currentVertexCount + 2)

        indices.add(currentVertexCount + 0)
        indices.add(currentVertexCount + 2)
        indices.add(currentVertexCount + 3)
    }

    /**
     * Get block safely with bounds checking
     */
    private fun Chunk.getBlockSafe(x: Int, y: Int, z: Int): BlockType {
        if (x !in 0..15 || y !in 0..255 || z !in 0..15) return BlockType.Air
        return getBlock(x, y, z)
    }

    /**
     * Get neighboring block (may be in adjacent chunk)
     */
    private fun Chunk.getNeighborBlock(x: Int, y: Int, z: Int): BlockType? {
        // If within chunk bounds, get block directly
        if (x in 0..15 && y in 0..255 && z in 0..15) {
            return getBlock(x, y, z)
        }

        // Convert to world coordinates
        val worldX = position.toWorldX() + x
        val worldY = y
        val worldZ = position.toWorldZ() + z

        // Get from world (may be in adjacent chunk)
        return world.getBlock(worldX, worldY, worldZ)
    }
}

/**
 * Face direction enum for mesh generation
 */
enum class FaceDirection(
    val dx: Int,
    val dy: Int,
    val dz: Int
) {
    UP(0, 1, 0),
    DOWN(0, -1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    EAST(1, 0, 0),
    WEST(-1, 0, 0)
}

/**
 * Direction helper functions
 */
object DirectionHelper {

    fun getNormal(direction: FaceDirection): FloatArray = when (direction) {
        FaceDirection.UP -> floatArrayOf(0f, 1f, 0f)
        FaceDirection.DOWN -> floatArrayOf(0f, -1f, 0f)
        FaceDirection.NORTH -> floatArrayOf(0f, 0f, -1f)
        FaceDirection.SOUTH -> floatArrayOf(0f, 0f, 1f)
        FaceDirection.EAST -> floatArrayOf(1f, 0f, 0f)
        FaceDirection.WEST -> floatArrayOf(-1f, 0f, 0f)
    }

    fun getBrightness(direction: FaceDirection): Float = when (direction) {
        FaceDirection.UP -> 1.0f
        FaceDirection.DOWN -> 0.5f
        FaceDirection.NORTH, FaceDirection.SOUTH -> 0.8f
        FaceDirection.EAST, FaceDirection.WEST -> 0.6f
    }

    fun getAxes(direction: FaceDirection): Triple<Int, Int, Int> = when (direction) {
        FaceDirection.UP, FaceDirection.DOWN -> Triple(0, 2, 1)
        FaceDirection.NORTH, FaceDirection.SOUTH -> Triple(0, 1, 2)
        FaceDirection.EAST, FaceDirection.WEST -> Triple(2, 1, 0)
    }

    fun getPosition(direction: FaceDirection, u: Int, v: Int, w: Int): IntArray = when (direction) {
        FaceDirection.UP, FaceDirection.DOWN -> intArrayOf(u, w, v)
        FaceDirection.NORTH, FaceDirection.SOUTH -> intArrayOf(u, v, w)
        FaceDirection.EAST, FaceDirection.WEST -> intArrayOf(w, v, u)
    }

    fun getQuadCorners(direction: FaceDirection, u: Int, v: Int, w: Int, width: Int, height: Int): Array<IntArray> {
        return when (direction) {
            FaceDirection.UP -> arrayOf(
                intArrayOf(u, w + 1, v),
                intArrayOf(u + width, w + 1, v),
                intArrayOf(u + width, w + 1, v + height),
                intArrayOf(u, w + 1, v + height)
            )

            FaceDirection.DOWN -> arrayOf(
                intArrayOf(u, w, v),
                intArrayOf(u, w, v + height),
                intArrayOf(u + width, w, v + height),
                intArrayOf(u + width, w, v)
            )

            FaceDirection.NORTH -> arrayOf(
                intArrayOf(u, v, w),
                intArrayOf(u, v + height, w),
                intArrayOf(u + width, v + height, w),
                intArrayOf(u + width, v, w)
            )

            FaceDirection.SOUTH -> arrayOf(
                intArrayOf(u, v, w + 1),
                intArrayOf(u + width, v, w + 1),
                intArrayOf(u + width, v + height, w + 1),
                intArrayOf(u, v + height, w + 1)
            )

            FaceDirection.EAST -> arrayOf(
                intArrayOf(w + 1, v, u),
                intArrayOf(w + 1, v, u + width),
                intArrayOf(w + 1, v + height, u + width),
                intArrayOf(w + 1, v + height, u)
            )

            FaceDirection.WEST -> arrayOf(
                intArrayOf(w, v, u),
                intArrayOf(w, v + height, u),
                intArrayOf(w, v + height, u + width),
                intArrayOf(w, v, u + width)
            )
        }
    }
}

/**
 * Mask entry for greedy meshing
 */
internal data class MaskEntry(
    val blockType: BlockType,
    val render: Boolean
)
