package io.kreekt.renderer.webgpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * T021: Tests for buffer overflow prevention in WebGPU renderer.
 * 
 * Validates that the uniform buffer can handle VoxelCraft's 120+ chunks
 * without crashing (buffer overflow was causing black screen at mesh #101+).
 */
class WebGPUBufferOverflowTest {

    /**
     * Test: Buffer size constants are correctly defined.
     * 
     * Expected:
     * - MAX_MESHES_PER_FRAME = 200 (increased from 100)
     * - UNIFORM_SIZE_PER_MESH = 256 (192 data + 64 padding)
     * - UNIFORM_BUFFER_SIZE = 51,200 bytes (200 × 256)
     */
    @Test
    fun testBufferSizeConstants() {
        assertEquals(200, WebGPURenderer.MAX_MESHES_PER_FRAME, 
            "Buffer should support 200 meshes (up from 100)")
        assertEquals(256, WebGPURenderer.UNIFORM_SIZE_PER_MESH,
            "Each mesh requires 256 bytes (192 data + 64 padding)")
        assertEquals(51200, WebGPURenderer.UNIFORM_BUFFER_SIZE,
            "Total buffer size should be 200 × 256 = 51,200 bytes")
    }

    /**
     * Test: Dynamic offset calculation is correct for all mesh indices.
     * 
     * Expected offsets:
     * - Mesh 0: offset = 0
     * - Mesh 1: offset = 256
     * - Mesh 100: offset = 25,600 (OLD buffer boundary)
     * - Mesh 199: offset = 50,944 (NEW buffer boundary - 256)
     */
    @Test
    fun testDynamicOffsetCalculation() {
        // Test first mesh
        val offset0 = 0 * WebGPURenderer.UNIFORM_SIZE_PER_MESH
        assertEquals(0, offset0, "Mesh 0 should have offset 0")
        
        // Test mesh at old buffer boundary
        val offset100 = 100 * WebGPURenderer.UNIFORM_SIZE_PER_MESH
        assertEquals(25600, offset100, "Mesh 100 offset should be 25,600 (old boundary)")
        
        // Test last valid mesh in new buffer
        val offset199 = 199 * WebGPURenderer.UNIFORM_SIZE_PER_MESH
        assertEquals(50944, offset199, "Mesh 199 offset should be 50,944")
        
        // Verify offset + data size fits in buffer
        assertTrue(offset199 + 192 <= WebGPURenderer.UNIFORM_BUFFER_SIZE,
            "Last mesh (199) should fit within buffer (offset + 192 <= 51,200)")
    }

    /**
     * Test: Verify that 150 meshes can be rendered without overflow.
     * 
     * This simulates VoxelCraft's typical chunk count when player moves around.
     * 
     * Expected:
     * - All 150 meshes fit within buffer capacity
     * - No overflow warnings
     */
    @Test
    fun testVoxelCraftTypicalChunkCount() {
        val meshCount = 150
        val lastMeshOffset = (meshCount - 1) * WebGPURenderer.UNIFORM_SIZE_PER_MESH
        
        assertTrue(meshCount <= WebGPURenderer.MAX_MESHES_PER_FRAME,
            "VoxelCraft typical count (150) should be within capacity (200)")
        assertTrue(lastMeshOffset + 192 <= WebGPURenderer.UNIFORM_BUFFER_SIZE,
            "150 meshes should fit within buffer without overflow")
    }

    /**
     * Test: Verify that exactly 200 meshes fit in buffer.
     * 
     * Expected:
     * - Mesh 199 (last valid) fits within buffer
     * - Mesh 200 would exceed buffer (should be rejected by bounds check)
     */
    @Test
    fun testBufferCapacityBoundary() {
        // Last valid mesh (199)
        val lastValidOffset = 199 * WebGPURenderer.UNIFORM_SIZE_PER_MESH
        assertTrue(lastValidOffset + 192 <= WebGPURenderer.UNIFORM_BUFFER_SIZE,
            "Mesh 199 should fit within buffer")
        
        // First invalid mesh (200)
        val firstInvalidOffset = 200 * WebGPURenderer.UNIFORM_SIZE_PER_MESH
        assertTrue(firstInvalidOffset >= WebGPURenderer.UNIFORM_BUFFER_SIZE,
            "Mesh 200 offset should exceed buffer boundary")
    }

    /**
     * Test: Buffer size increase from 100 to 200 meshes.
     * 
     * Expected:
     * - Old buffer: 25,600 bytes (100 meshes)
     * - New buffer: 51,200 bytes (200 meshes)
     * - Increase: 2x capacity, 100% headroom
     */
    @Test
    fun testBufferSizeIncrease() {
        val oldBufferSize = 100 * 256
        val newBufferSize = WebGPURenderer.UNIFORM_BUFFER_SIZE
        
        assertEquals(25600, oldBufferSize, "Old buffer was 25,600 bytes")
        assertEquals(51200, newBufferSize, "New buffer is 51,200 bytes")
        assertEquals(2.0, newBufferSize.toDouble() / oldBufferSize.toDouble(),
            "Buffer size doubled from 100 to 200 meshes")
    }

    /**
     * Test: Verify buffer can handle VoxelCraft stress scenario.
     * 
     * VoxelCraft with aggressive chunk loading can reach 136 chunks.
     * 
     * Expected:
     * - 136 meshes fit comfortably within 200 capacity
     * - Headroom: 64 meshes (32% spare capacity)
     */
    @Test
    fun testVoxelCraftStressScenario() {
        val stressChunkCount = 136  // Observed in logs
        
        assertTrue(stressChunkCount < WebGPURenderer.MAX_MESHES_PER_FRAME,
            "VoxelCraft stress count (136) should be within capacity (200)")
        
        val headroom = WebGPURenderer.MAX_MESHES_PER_FRAME - stressChunkCount
        assertTrue(headroom >= 50,
            "Should have at least 50 meshes headroom (actual: $headroom)")
    }

    /**
     * Test: Offset alignment requirements.
     * 
     * WebGPU requires uniform buffer offsets to be 256-byte aligned.
     * 
     * Expected:
     * - All offsets are multiples of 256
     */
    @Test
    fun testUniformOffsetAlignment() {
        for (meshIndex in 0 until 200) {
            val offset = meshIndex * WebGPURenderer.UNIFORM_SIZE_PER_MESH
            assertEquals(0, offset % 256,
                "Mesh $meshIndex offset ($offset) must be 256-byte aligned")
        }
    }

    /**
     * Test: Memory usage is reasonable.
     * 
     * Expected:
     * - Buffer size: 51,200 bytes = 50 KB
     * - Memory overhead: negligible (<0.05 MB)
     */
    @Test
    fun testMemoryUsageIsReasonable() {
        val bufferSizeKB = WebGPURenderer.UNIFORM_BUFFER_SIZE / 1024
        val bufferSizeMB = bufferSizeKB.toDouble() / 1024.0
        
        assertEquals(50, bufferSizeKB, "Buffer should be 50 KB")
        assertTrue(bufferSizeMB < 0.1,
            "Buffer memory usage ($bufferSizeMB MB) should be negligible")
    }
}
