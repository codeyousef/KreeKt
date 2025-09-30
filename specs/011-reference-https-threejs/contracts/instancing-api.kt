/**
 * Instancing API Contract
 * Maps FR-I001 through FR-I010
 *
 * Constitutional Requirements:
 * - GPU instancing for efficient rendering
 * - Type-safe instance attribute management
 * - Performance: 10,000+ instances at 60 FPS
 */

package io.kreekt.instancing

import io.kreekt.core.Mesh
import io.kreekt.geometry.BufferGeometry
import io.kreekt.material.Material
import io.kreekt.math.Matrix4
import io.kreekt.math.Color
import io.kreekt.math.Vector3
import io.kreekt.math.Quaternion

/**
 * FR-I001, FR-I002, FR-I003: Instanced mesh for efficient rendering
 *
 * Test Contract:
 * - MUST render thousands of instances in single draw call
 * - MUST support per-instance transforms (matrix)
 * - MUST support per-instance colors
 * - MUST update GPU buffers efficiently
 * - Performance target: 10,000+ instances at 60 FPS
 */
class InstancedMesh(
    geometry: BufferGeometry,
    material: Material,
    count: Int
) : Mesh(geometry, material) {
    /**
     * Number of instances
     */
    val count: Int

    /**
     * Per-instance transformation matrices
     *
     * Test Contract:
     * - MUST be uploaded to GPU as instance attribute
     * - MUST support dynamic updates
     */
    val instanceMatrix: InstancedBufferAttribute

    /**
     * Per-instance colors (optional)
     *
     * Test Contract:
     * - MUST blend with material color
     * - MUST be null if not used
     */
    var instanceColor: InstancedBufferAttribute?

    /**
     * FR-I004: Set transformation matrix for instance
     *
     * @param index Instance index (0 to count-1)
     * @param matrix Transformation matrix
     *
     * Test Contract:
     * - MUST validate index bounds
     * - MUST mark buffer for GPU update
     * - MUST apply matrix to instance
     */
    fun setMatrixAt(index: Int, matrix: Matrix4)

    /**
     * Get transformation matrix for instance
     */
    fun getMatrixAt(index: Int, matrix: Matrix4)

    /**
     * FR-I005: Set color for instance
     *
     * @param index Instance index
     * @param color Instance color
     *
     * Test Contract:
     * - MUST create instanceColor buffer if null
     * - MUST validate index bounds
     * - MUST mark buffer for GPU update
     */
    fun setColorAt(index: Int, color: Color)

    /**
     * Get color for instance
     */
    fun getColorAt(index: Int, color: Color)

    /**
     * FR-I006: Update instance matrix from position, rotation, scale
     *
     * Test Contract:
     * - MUST compose matrix from TRS
     * - MUST update instanceMatrix buffer
     */
    fun setPositionAt(index: Int, position: Vector3)
    fun setRotationAt(index: Int, rotation: Quaternion)
    fun setScaleAt(index: Int, scale: Vector3)

    /**
     * Get bounding box/sphere for all instances
     *
     * Test Contract:
     * - MUST compute bounds from all instance transforms
     * - MUST be used for frustum culling
     */
    fun computeBoundingBox()
    fun computeBoundingSphere()

    /**
     * Dispose instance resources
     */
    override fun dispose()
}

/**
 * FR-I007, FR-I008: Instanced buffer attribute
 *
 * Test Contract:
 * - MUST store per-instance data
 * - MUST upload to GPU efficiently
 * - MUST support partial updates
 */
class InstancedBufferAttribute(
    array: FloatArray,
    itemSize: Int,
    normalized: Boolean = false,
    meshPerAttribute: Int = 1
) : BufferAttribute(array, itemSize, normalized) {
    /**
     * Number of instances per attribute value
     * 1 = different value per instance (default)
     * N = same value for N consecutive instances
     */
    var meshPerAttribute: Int

    /**
     * Mark range for GPU update
     *
     * @param offset Start index
     * @param count Number of items to update
     *
     * Test Contract:
     * - MUST minimize GPU uploads
     * - MUST handle partial buffer updates
     */
    fun updateRange(offset: Int, count: Int)
}

/**
 * FR-I009: Instanced interleaved buffer
 *
 * Test Contract:
 * - MUST store multiple attributes interleaved
 * - MUST improve cache locality
 * - MUST support attribute views
 */
class InstancedInterleavedBuffer(
    array: FloatArray,
    stride: Int
) : InterleavedBuffer(array, stride) {
    var meshPerAttribute: Int
}

/**
 * FR-I010: Instanced buffer geometry
 *
 * Test Contract:
 * - MUST support instanced attributes
 * - MUST validate instance count consistency
 */
class InstancedBufferGeometry : BufferGeometry() {
    /**
     * Maximum number of instances
     */
    var instanceCount: Int

    /**
     * Add instanced attribute
     */
    fun addInstancedAttribute(name: String, attribute: InstancedBufferAttribute)

    /**
     * Validate instance count across all attributes
     */
    fun validateInstanceCount(): Boolean
}

// Supporting types (assume these exist in kreekt-geometry)
expect open class BufferAttribute(
    array: FloatArray,
    itemSize: Int,
    normalized: Boolean = false
) {
    val array: FloatArray
    val itemSize: Int
    val count: Int
    var normalized: Boolean
    var needsUpdate: Boolean

    fun setX(index: Int, x: Float)
    fun setY(index: Int, y: Float)
    fun setZ(index: Int, z: Float)
    fun setW(index: Int, w: Float)
    fun setXY(index: Int, x: Float, y: Float)
    fun setXYZ(index: Int, x: Float, y: Float, z: Float)
    fun setXYZW(index: Int, x: Float, y: Float, z: Float, w: Float)

    fun getX(index: Int): Float
    fun getY(index: Int): Float
    fun getZ(index: Int): Float
    fun getW(index: Int): Float

    fun copyAt(index1: Int, attribute: BufferAttribute, index2: Int)
    fun clone(): BufferAttribute
}

expect open class InterleavedBuffer(
    array: FloatArray,
    stride: Int
) {
    val array: FloatArray
    val stride: Int
    val count: Int
    var needsUpdate: Boolean

    fun setUsage(usage: Usage)
}

enum class Usage {
    STATIC_DRAW,
    DYNAMIC_DRAW,
    STREAM_DRAW,
    STATIC_READ,
    DYNAMIC_READ,
    STREAM_READ,
    STATIC_COPY,
    DYNAMIC_COPY,
    STREAM_COPY
}

/**
 * Instanced rendering utilities
 */
object InstancedRenderingUtils {
    /**
     * Compute optimal instance count based on platform
     *
     * Test Contract:
     * - MUST consider GPU memory limits
     * - MUST consider draw call overhead
     */
    fun getOptimalInstanceCount(
        geometrySize: Int,
        materialSize: Int,
        platformLimits: PlatformLimits
    ): Int

    /**
     * Batch instances for frustum culling
     *
     * Test Contract:
     * - MUST cull invisible instance groups
     * - MUST maintain instance ordering
     */
    fun frustumCullInstances(
        instancedMesh: InstancedMesh,
        camera: Camera
    ): IntRange?

    /**
     * Sort instances for transparency
     *
     * Test Contract:
     * - MUST sort back-to-front for alpha blending
     * - MUST update instanceMatrix order
     */
    fun sortInstancesForTransparency(
        instancedMesh: InstancedMesh,
        camera: Camera
    )
}

data class PlatformLimits(
    val maxInstanceCount: Int,
    val maxBufferSize: Int,
    val maxVertexAttributes: Int
)

// Forward declarations
expect class Camera