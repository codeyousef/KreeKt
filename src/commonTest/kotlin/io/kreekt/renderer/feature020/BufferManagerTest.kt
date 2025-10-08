package io.kreekt.renderer.feature020

import kotlin.test.*

/**
 * Contract tests for BufferManager interface.
 * Feature 020 - Production-Ready Renderer
 *
 * TDD Red Phase: All tests must fail (no implementation yet)
 */
class BufferManagerTest {

    // TDD Red Phase: Tests expect NotImplementedError until implementation complete
    // Will be replaced with actual initialization in T005-T016

    /**
     * Test 1: Create Vertex Buffer (Happy Path)
     * Contract: specs/020-go-from-mvp/contracts/buffer-manager-contract.md
     */
    @Test
    fun testCreateVertexBuffer_validData_returnsHandle() {
        // GIVEN: Valid vertex data (3 vertices with position + color)
        val vertexData = floatArrayOf(
            // Vertex 0: position (0,0,0), color (1,0,0) red
            0f, 0f, 0f, 1f, 0f, 0f,
            // Vertex 1: position (1,0,0), color (0,1,0) green
            1f, 0f, 0f, 0f, 1f, 0f,
            // Vertex 2: position (0,1,0), color (0,0,1) blue
            0f, 1f, 0f, 0f, 0f, 1f
        )

        // WHEN: Create vertex buffer
        val handle = bufferManager.createVertexBuffer(vertexData)

        // THEN: Handle is valid
        assertNotNull(handle)
        assertTrue(handle.isValid())
        assertEquals(BufferUsage.VERTEX, handle.usage)
        assertEquals(vertexData.size * 4, handle.size) // 18 floats × 4 bytes = 72 bytes
    }

    /**
     * Test 2: Create Vertex Buffer (Empty Data)
     * Contract: specs/020-go-from-mvp/contracts/buffer-manager-contract.md
     */
    @Test
    fun testCreateVertexBuffer_emptyData_throwsException() {
        // GIVEN: Empty vertex data
        val vertexData = floatArrayOf()

        // WHEN/THEN: Creating buffer throws IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            bufferManager.createVertexBuffer(vertexData)
        }
    }

    /**
     * Test 3: Create Index Buffer (Happy Path)
     * Contract: specs/020-go-from-mvp/contracts/buffer-manager-contract.md
     */
    @Test
    fun testCreateIndexBuffer_validData_returnsHandle() {
        // GIVEN: Valid index data (2 triangles = 6 indices)
        val indexData = intArrayOf(0, 1, 2, 0, 2, 3)

        // WHEN: Create index buffer
        val handle = bufferManager.createIndexBuffer(indexData)

        // THEN: Handle is valid
        assertNotNull(handle)
        assertTrue(handle.isValid())
        assertEquals(BufferUsage.INDEX, handle.usage)
        assertEquals(indexData.size * 4, handle.size) // 6 ints × 4 bytes = 24 bytes
    }

    /**
     * Test 4: Create Index Buffer (Invalid Triangle Count)
     * Contract: specs/020-go-from-mvp/contracts/buffer-manager-contract.md
     */
    @Test
    fun testCreateIndexBuffer_notTriangles_throwsException() {
        // GIVEN: Invalid index data (not multiple of 3)
        val indexData = intArrayOf(0, 1)

        // WHEN/THEN: Creating buffer throws IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            bufferManager.createIndexBuffer(indexData)
        }
    }

    /**
     * Test 5: Create Uniform Buffer (Happy Path)
     * Contract: specs/020-go-from-mvp/contracts/buffer-manager-contract.md
     */
    @Test
    fun testCreateUniformBuffer_validSize_returnsHandle() {
        // GIVEN: Valid uniform buffer size (64 bytes for mat4x4)
        val sizeBytes = 64

        // WHEN: Create uniform buffer
        val handle = bufferManager.createUniformBuffer(sizeBytes)

        // THEN: Handle is valid
        assertNotNull(handle)
        assertTrue(handle.isValid())
        assertEquals(BufferUsage.UNIFORM, handle.usage)
        assertEquals(sizeBytes, handle.size)
    }

    /**
     * Test 6: Create Uniform Buffer (Size Too Small)
     * Contract: specs/020-go-from-mvp/contracts/buffer-manager-contract.md
     */
    @Test
    fun testCreateUniformBuffer_sizeTooSmall_throwsException() {
        // GIVEN: Uniform buffer size too small (< 64 bytes)
        val sizeBytes = 32

        // WHEN/THEN: Creating buffer throws IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            bufferManager.createUniformBuffer(sizeBytes)
        }
    }

    /**
     * Test 7: Update Uniform Buffer (Happy Path)
     * Contract: specs/020-go-from-mvp/contracts/buffer-manager-contract.md
     */
    @Test
    fun testUpdateUniformBuffer_validData_succeeds() {
        // GIVEN: Created uniform buffer
        val handle = bufferManager.createUniformBuffer(64)

        // AND: Valid matrix data (identity matrix)
        val matrixData = ByteArray(64).apply {
            // Identity matrix: diagonal = 1.0
            putFloat(0, 1.0f)   // m00
            putFloat(20, 1.0f)  // m11
            putFloat(40, 1.0f)  // m22
            putFloat(60, 1.0f)  // m33
        }

        // WHEN: Update uniform buffer
        bufferManager.updateUniformBuffer(handle, matrixData, offset = 0)

        // THEN: No exception thrown
    }

    /**
     * Test 8: Update Uniform Buffer (Invalid Handle)
     * Contract: specs/020-go-from-mvp/contracts/buffer-manager-contract.md
     */
    @Test
    fun testUpdateUniformBuffer_invalidHandle_throwsException() {
        // GIVEN: Invalid buffer handle
        val invalidHandle = BufferHandle(null, 0, BufferUsage.UNIFORM)
        val data = ByteArray(64)

        // WHEN/THEN: Updating buffer throws InvalidBufferException
        assertFailsWith<InvalidBufferException> {
            bufferManager.updateUniformBuffer(invalidHandle, data)
        }
    }

    /**
     * Test 9: Destroy Buffer (Happy Path)
     * Contract: specs/020-go-from-mvp/contracts/buffer-manager-contract.md
     */
    @Test
    fun testDestroyBuffer_validHandle_succeeds() {
        // GIVEN: Created buffer
        val handle = bufferManager.createVertexBuffer(floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f))

        // WHEN: Destroy buffer
        bufferManager.destroyBuffer(handle)

        // THEN: No exception thrown
        // Note: Contract states buffer should no longer be valid after destruction,
        // but BufferHandle is immutable, so validation happens on next use
    }

    /**
     * Test 10: Destroy Buffer (Already Destroyed)
     * Contract: specs/020-go-from-mvp/contracts/buffer-manager-contract.md
     */
    @Test
    fun testDestroyBuffer_alreadyDestroyed_throwsException() {
        // GIVEN: Created and destroyed buffer
        val handle = bufferManager.createVertexBuffer(floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f))
        bufferManager.destroyBuffer(handle)

        // WHEN/THEN: Destroying again throws InvalidBufferException
        assertFailsWith<InvalidBufferException> {
            bufferManager.destroyBuffer(handle)
        }
    }
}
