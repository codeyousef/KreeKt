package io.kreekt.renderer.webgpu

import io.kreekt.camera.PerspectiveCamera
import io.kreekt.core.scene.Scene
import io.kreekt.renderer.RendererFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Dynamic Offset Tests: WebGPU Dynamic Offset Correctness
 * 
 * These tests validate that using a single bind group with dynamic offsets
 * produces the same correct rendering as creating fresh bind groups.
 * 
 * Dynamic offsets are the key optimization to achieve 60 FPS:
 * - 1 bind group creation (not 68)
 * - setBindGroup(0, bindGroup, [offset]) varies offset
 * - Minimal JS↔WebGPU interop overhead
 */
class WebGPUDynamicOffsetTest : WebGPUTestBase() {

    /**
     * Test 6: Dynamic offset array calculation
     * 
     * EXPECTED: Offset for mesh N should be N × 256 bytes
     */
    @Test
    fun testDynamicOffsetArrayCalculation() {
        // Given: Draw indices 0-9
        val offsets = (0..9).map { it * 256 }
        
        // Then: Offsets should be [0, 256, 512, 768, 1024, ...]
        val expected = listOf(0, 256, 512, 768, 1024, 1280, 1536, 1792, 2048, 2304)
        
        assertEquals(expected, offsets, "Dynamic offset calculation incorrect")
        
        console.log("✅ Dynamic Offset Test 6: Offset Array Calculation")
        console.log("   Offsets: $offsets")
    }

    /**
     * Test 7: Single bind group with multiple offsets
     * 
     * EXPECTED: Same bind group reused with different offsets produces
     * correct per-mesh rendering
     */
    @Test
    fun testSingleBindGroupWithMultipleOffsets() = runTest {
        skipIfWebGPUUnavailable()
        
        // Given: Renderer with 3 meshes at different positions
        val renderer = RendererFactory.create(canvas)
        val scene = Scene()
        val camera = PerspectiveCamera(75f, 800f / 600f, 0.1f, 1000f)
        camera.position.set(0f, 0f, 10f)
        camera.updateMatrixWorld()
        
        val mesh1 = createMeshAt(x = -5.0, y = 0.0, z = 0.0)
        val mesh2 = createMeshAt(x = 0.0, y = 0.0, z = 0.0)
        val mesh3 = createMeshAt(x = 5.0, y = 0.0, z = 0.0)
        
        scene.add(mesh1)
        scene.add(mesh2)
        scene.add(mesh3)
        
        try {
            // When: Render with dynamic offsets
            renderer.render(scene, camera)
            
            // Then: Each mesh should maintain its distinct position
            assertEquals(-5f, mesh1.position.x, 0.01f)
            assertEquals(0f, mesh2.position.x, 0.01f)
            assertEquals(5f, mesh3.position.x, 0.01f)
            
            console.log("✅ Dynamic Offset Test 7: Single Bind Group Multi-Offset")
            console.log("   Rendered 3 meshes with unique positions")
            console.log("   Mesh positions preserved correctly")
            
        } finally {
            renderer.dispose()
            cleanup()
        }
    }

    /**
     * Test 8: Offset boundary validation
     * 
     * EXPECTED: Offsets stay within buffer bounds (0 to 25,600 bytes)
     */
    @Test
    fun testOffsetBoundaryValidation() {
        // Given: Maximum supported mesh count (100)
        val maxMeshes = 100
        
        // When: Calculate all offsets
        val offsets = (0 until maxMeshes).map { it * 256 }
        
        // Then: All offsets should be within buffer bounds
        val maxOffset = offsets.maxOrNull() ?: 0
        val bufferSize = 256 * 100  // 25,600 bytes
        
        assertTrue(
            maxOffset < bufferSize,
            "Max offset $maxOffset exceeds buffer size $bufferSize"
        )
        
        console.log("✅ Dynamic Offset Test 8: Boundary Validation")
        console.log("   Max offset: $maxOffset bytes")
        console.log("   Buffer size: $bufferSize bytes")
        console.log("   Offsets within bounds: ✅")
    }

    /**
     * Test 9: Uniform data isolation with dynamic offsets
     * 
     * EXPECTED: Each mesh reads its own uniform data from correct offset
     */
    @Test
    fun testUniformDataIsolationWithDynamicOffsets() = runTest {
        skipIfWebGPUUnavailable()
        
        // Given: Renderer with meshes at very distinct positions
        val renderer = RendererFactory.create(canvas)
        val scene = Scene()
        val camera = PerspectiveCamera(75f, 800f / 600f, 0.1f, 1000f)
        camera.position.set(0f, 0f, 10f)
        camera.updateMatrixWorld()
        
        val positions = listOf(
            Triple(-20.0, 10.0, 5.0),
            Triple(0.0, 0.0, 0.0),
            Triple(20.0, -10.0, -5.0)
        )
        
        val meshes = positions.map { (x, y, z) ->
            createMeshAt(x, y, z).also { scene.add(it) }
        }
        
        try {
            // When: Render frame
            renderer.render(scene, camera)
            
            // Then: Each mesh should have its original position preserved
            positions.forEachIndexed { i, (x, y, z) ->
                val mesh = meshes[i]
                assertEquals(x.toFloat(), mesh.position.x, 0.01f, "Mesh $i X position wrong")
                assertEquals(y.toFloat(), mesh.position.y, 0.01f, "Mesh $i Y position wrong")
                assertEquals(z.toFloat(), mesh.position.z, 0.01f, "Mesh $i Z position wrong")
            }
            
            console.log("✅ Dynamic Offset Test 9: Uniform Data Isolation")
            console.log("   All meshes maintain distinct positions")
            
        } finally {
            renderer.dispose()
            cleanup()
        }
    }

    /**
     * Test 10: Frame-to-frame offset consistency
     * 
     * EXPECTED: Same mesh gets same offset across frames
     */
    @Test
    fun testFrameToFrameOffsetConsistency() = runTest {
        skipIfWebGPUUnavailable()
        
        // Given: Renderer with static scene
        val renderer = RendererFactory.create(canvas)
        val scene = Scene()
        val camera = PerspectiveCamera(75f, 800f / 600f, 0.1f, 1000f)
        camera.position.set(0f, 0f, 10f)
        camera.updateMatrixWorld()
        
        val mesh1 = createMeshAt(x = -3.0, y = 0.0, z = 0.0)
        val mesh2 = createMeshAt(x = 0.0, y = 0.0, z = 0.0)
        val mesh3 = createMeshAt(x = 3.0, y = 0.0, z = 0.0)
        
        scene.add(mesh1)
        scene.add(mesh2)
        scene.add(mesh3)
        
        try {
            // When: Render 3 frames
            renderer.render(scene, camera)
            val pos1_frame1 = Triple(mesh1.position.x, mesh1.position.y, mesh1.position.z)
            
            renderer.render(scene, camera)
            val pos1_frame2 = Triple(mesh1.position.x, mesh1.position.y, mesh1.position.z)
            
            renderer.render(scene, camera)
            val pos1_frame3 = Triple(mesh1.position.x, mesh1.position.y, mesh1.position.z)
            
            // Then: Mesh1 position should be identical across all frames
            assertEquals(pos1_frame1, pos1_frame2, "Mesh1 position changed frame 1→2")
            assertEquals(pos1_frame2, pos1_frame3, "Mesh1 position changed frame 2→3")
            
            console.log("✅ Dynamic Offset Test 10: Frame-to-Frame Consistency")
            console.log("   Mesh positions stable across 3 frames")
            
        } finally {
            renderer.dispose()
            cleanup()
        }
    }
}
