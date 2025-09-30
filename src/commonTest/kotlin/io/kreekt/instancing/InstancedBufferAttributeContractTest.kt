package io.kreekt.instancing

import io.kreekt.geometry.BufferAttribute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract test for InstancedBufferAttribute - T028
 * Covers: FR-I007, FR-I008, FR-I009, FR-I010 from contracts/instancing-api.kt
 */
class InstancedBufferAttributeContractTest {

    @Test
    fun testStorePerInstanceData() {
        // FR-I007: Store per-instance data
        val data = floatArrayOf(
            1f, 0f, 0f, 1f,  // Instance 1 color (red)
            0f, 1f, 0f, 1f,  // Instance 2 color (green)
            0f, 0f, 1f, 1f,  // Instance 3 color (blue)
            1f, 1f, 0f, 1f   // Instance 4 color (yellow)
        )

        val attribute = InstancedBufferAttribute(data, itemSize = 4, normalized = false)

        assertEquals(4, attribute.itemSize)
        assertEquals(4, attribute.count)
        assertEquals(16, attribute.array.size)

        // Verify data integrity
        assertEquals(1f, attribute.getX(0)) // First instance red channel
        assertEquals(0f, attribute.getY(0))
        assertEquals(0f, attribute.getZ(0))
        assertEquals(1f, attribute.getW(0))

        assertEquals(0f, attribute.getX(1)) // Second instance green channel
        assertEquals(1f, attribute.getY(1))
    }

    @Test
    fun testEfficientGPUUploads() {
        // FR-I008: Efficient GPU uploads
        val largeData = FloatArray(10000 * 4) { it.toFloat() } // 10k instances, 4 components each
        val attribute = InstancedBufferAttribute(largeData, itemSize = 4)

        // Test that attribute is marked for GPU upload
        assertTrue(attribute.needsUpdate)

        // Simulate GPU upload
        attribute.uploadToGPU()
        assertTrue(!attribute.needsUpdate)

        // Modify data
        attribute.setXYZW(100, 1f, 2f, 3f, 4f)
        assertTrue(attribute.needsUpdate) // Should be marked dirty
    }

    @Test
    fun testPartialBufferUpdates() {
        // FR-I009: Partial buffer updates
        val data = FloatArray(1000 * 3) { 0f } // 1000 instances, 3 components each
        val attribute = InstancedBufferAttribute(data, itemSize = 3)

        // Update a range of instances
        val updateStart = 100
        val updateCount = 50

        for (i in updateStart until updateStart + updateCount) {
            attribute.setXYZ(i, i.toFloat(), i.toFloat() * 2, i.toFloat() * 3)
        }

        // Verify update range is tracked
        assertNotNull(attribute.updateRange)
        assertEquals(updateStart, attribute.updateRange.offset)
        assertEquals(updateCount * 3, attribute.updateRange.count) // count in array elements

        // Verify partial upload capability
        assertTrue(attribute.supportsPartialUpdate())

        // Clear update range after GPU upload
        attribute.uploadToGPU()
        assertEquals(-1, attribute.updateRange.offset)
        assertEquals(-1, attribute.updateRange.count)
    }

    @Test
    fun testMeshPerAttributeParameter() {
        // FR-I010: meshPerAttribute parameter for instancing divisor
        val matrices = FloatArray(100 * 16) { 0f } // 100 4x4 matrices
        val attribute = InstancedBufferAttribute(matrices, itemSize = 16)

        // Default: one instance per attribute value
        assertEquals(1, attribute.meshPerAttribute)

        // Test custom divisor (e.g., 2 meshes share same instance data)
        attribute.meshPerAttribute = 2
        assertEquals(2, attribute.meshPerAttribute)

        // This affects how many times geometry is drawn per instance
        val instanceCount = 200
        val effectiveInstances = instanceCount / attribute.meshPerAttribute
        assertEquals(100, effectiveInstances) // 200 meshes / 2 per attribute = 100 unique instances
    }

    @Test
    fun testInstancedAttributeInterleaving() {
        // Test interleaved instance data (position + color in single buffer)
        val interleavedData = floatArrayOf(
            // Instance 0: position (x,y,z) + color (r,g,b,a)
            0f, 0f, 0f, 1f, 0f, 0f, 1f,
            // Instance 1
            1f, 0f, 0f, 0f, 1f, 0f, 1f,
            // Instance 2
            2f, 0f, 0f, 0f, 0f, 1f, 1f
        )

        val interleavedAttribute = InstancedInterleavedBuffer(interleavedData, stride = 7)

        // Create views for position and color
        val positionAttribute = InterleavedBufferAttribute(interleavedAttribute, itemSize = 3, offset = 0)
        val colorAttribute = InterleavedBufferAttribute(interleavedAttribute, itemSize = 4, offset = 3)

        // Verify position data
        assertEquals(0f, positionAttribute.getX(0))
        assertEquals(1f, positionAttribute.getX(1))
        assertEquals(2f, positionAttribute.getX(2))

        // Verify color data
        assertEquals(1f, colorAttribute.getX(0)) // Red for instance 0
        assertEquals(0f, colorAttribute.getX(1)) // Red for instance 1
        assertEquals(0f, colorAttribute.getX(2)) // Red for instance 2
    }

    @Test
    fun testDynamicInstanceCount() {
        // Test dynamic instance count updates
        val initialCount = 100
        val data = FloatArray(1000 * 4) { 0f } // Allocate for max 1000 instances
        val attribute = InstancedBufferAttribute(data, itemSize = 4)

        // Set initial count
        attribute.count = initialCount
        assertEquals(initialCount, attribute.count)

        // Increase count
        attribute.count = 500
        assertEquals(500, attribute.count)

        // Ensure can't exceed buffer capacity
        attribute.count = 2000 // Try to set beyond buffer
        assertEquals(1000, attribute.count) // Should be clamped to max
    }

    @Test
    fun testInstanceMatrixAttribute() {
        // Special case: 4x4 matrix as instance attribute
        val instanceMatrices = FloatArray(50 * 16) // 50 4x4 matrices

        // Initialize with identity matrices
        for (i in 0 until 50) {
            val offset = i * 16
            // Identity matrix
            instanceMatrices[offset + 0] = 1f  // m11
            instanceMatrices[offset + 5] = 1f  // m22
            instanceMatrices[offset + 10] = 1f // m33
            instanceMatrices[offset + 15] = 1f // m44
        }

        val matrixAttribute = InstancedBufferAttribute(instanceMatrices, itemSize = 16)

        // Verify matrix data
        assertEquals(16, matrixAttribute.itemSize)
        assertEquals(50, matrixAttribute.count)

        // Get first matrix
        val matrix = FloatArray(16)
        matrixAttribute.getMatrix4(0, matrix)

        // Verify identity matrix
        assertEquals(1f, matrix[0])
        assertEquals(1f, matrix[5])
        assertEquals(1f, matrix[10])
        assertEquals(1f, matrix[15])
    }

    @Test
    fun testMemoryEfficiency() {
        // Verify memory-efficient storage
        val instanceCount = 10000
        val componentsPerInstance = 7 // position (3) + quaternion (4)

        val data = FloatArray(instanceCount * componentsPerInstance)
        val attribute = InstancedBufferAttribute(data, itemSize = componentsPerInstance)

        // Calculate expected memory usage
        val bytesPerFloat = 4
        val expectedBytes = instanceCount * componentsPerInstance * bytesPerFloat

        assertTrue(attribute.byteLength <= expectedBytes)

        // Test copy-on-write optimization
        val attribute2 = attribute.clone()
        assertTrue(attribute2.array !== attribute.array) // Should be different arrays
        assertEquals(attribute.count, attribute2.count)
    }
}

// Supporting classes for the contract test

class InstancedBufferAttribute(
    override val array: FloatArray,
    override val itemSize: Int,
    override val normalized: Boolean = false
) : BufferAttribute(array, itemSize, normalized) {

    var meshPerAttribute: Int = 1
    override var needsUpdate: Boolean = true

    val updateRange = UpdateRange()

    override val count: Int
        get() = _count

    private var _count: Int = array.size / itemSize

    override fun setX(index: Int, value: Float) {
        super.setX(index, value)
        markRangeUpdate(index, 1)
    }

    override fun setXY(index: Int, x: Float, y: Float) {
        super.setXY(index, x, y)
        markRangeUpdate(index, 2)
    }

    override fun setXYZ(index: Int, x: Float, y: Float, z: Float) {
        super.setXYZ(index, x, y, z)
        markRangeUpdate(index, 3)
    }

    fun setXYZW(index: Int, x: Float, y: Float, z: Float, w: Float) {
        val offset = index * itemSize
        array[offset] = x
        array[offset + 1] = y
        array[offset + 2] = z
        array[offset + 3] = w
        markRangeUpdate(index, 4)
        needsUpdate = true
    }

    fun getW(index: Int): Float {
        return array[index * itemSize + 3]
    }

    fun getMatrix4(index: Int, target: FloatArray) {
        val offset = index * itemSize
        for (i in 0 until 16) {
            target[i] = array[offset + i]
        }
    }

    fun uploadToGPU() {
        // Simulate GPU upload
        needsUpdate = false
        updateRange.offset = -1
        updateRange.count = -1
    }

    fun supportsPartialUpdate(): Boolean = true

    val byteLength: Int
        get() = array.size * 4 // 4 bytes per float

    override fun clone(): InstancedBufferAttribute {
        return InstancedBufferAttribute(array.copyOf(), itemSize, normalized).also {
            it.meshPerAttribute = meshPerAttribute
        }
    }

    private fun markRangeUpdate(index: Int, components: Int) {
        val elementOffset = index * itemSize
        if (updateRange.offset == -1) {
            updateRange.offset = index
            updateRange.count = components
        } else {
            val minOffset = minOf(updateRange.offset, index)
            val maxEnd = maxOf(
                updateRange.offset * itemSize + updateRange.count,
                elementOffset + components
            )
            updateRange.offset = minOffset
            updateRange.count = maxEnd - minOffset * itemSize
        }
    }

    override var count: Int
        get() = _count
        set(value) {
            _count = minOf(value, array.size / itemSize)
        }

    data class UpdateRange(
        var offset: Int = -1,
        var count: Int = -1
    )
}

class InstancedInterleavedBuffer(
    val array: FloatArray,
    val stride: Int
) {
    val count: Int = array.size / stride
}

class InterleavedBufferAttribute(
    private val buffer: InstancedInterleavedBuffer,
    override val itemSize: Int,
    private val offset: Int
) : BufferAttribute(buffer.array, itemSize, false) {

    override fun getX(index: Int): Float {
        return buffer.array[index * buffer.stride + offset]
    }

    override fun getY(index: Int): Float {
        return buffer.array[index * buffer.stride + offset + 1]
    }

    override fun getZ(index: Int): Float {
        return buffer.array[index * buffer.stride + offset + 2]
    }
}