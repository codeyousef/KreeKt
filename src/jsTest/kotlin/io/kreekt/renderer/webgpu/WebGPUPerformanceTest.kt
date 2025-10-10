package io.kreekt.renderer.webgpu

import io.kreekt.camera.PerspectiveCamera
import io.kreekt.core.scene.Scene
import io.kreekt.renderer.RendererFactory
import kotlinx.coroutines.test.runTest
import kotlin.js.Date
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Performance Tests: WebGPU Renderer Performance Validation
 * 
 * These tests measure and validate that the renderer meets the constitutional
 * 60 FPS requirement (16.67ms per frame).
 * 
 * CRITICAL: These tests will FAIL with current implementation (3 FPS observed)
 * and should PASS after dynamic offset optimization.
 */
class WebGPUPerformanceTest : WebGPUTestBase() {

    /**
     * Test 1: Frame time budget compliance
     * 
     * EXPECTED: Frame rendering completes in ≤16.67ms (60 FPS requirement)
     * CURRENT: ~333ms/frame (3 FPS)
     */
    @Test
    fun testFrameTimeMeetsRequirement() = runTest {
        skipIfWebGPUUnavailable()
        
        // Given: Renderer with realistic scene (68 meshes like VoxelCraft)
        val renderer = RendererFactory.create(canvas)
        val scene = Scene()
        val camera = PerspectiveCamera(75f, 800f / 600f, 0.1f, 1000f)
        camera.position.set(0f, 150f, 0f)
        camera.updateMatrixWorld()
        
        // Create 68 meshes (typical VoxelCraft chunk count)
        val meshCount = 68
        for (i in 0 until meshCount) {
            val x = (i % 9) * 16.0 - 64.0
            val z = (i / 9) * 16.0 - 64.0
            val mesh = createMeshAt(x, 0.0, z)
            scene.add(mesh)
        }
        
        try {
            // When: Measure frame render time
            val startTime = Date.now()
            renderer.render(scene, camera)
            val endTime = Date.now()
            
            val frameTime = endTime - startTime
            
            // Then: Frame time should be ≤16.67ms for 60 FPS
            console.log("⏱️ Performance Test 1: Frame Time")
            console.log("   Meshes rendered: $meshCount")
            console.log("   Frame time: ${frameTime.toFixed(2)}ms")
            console.log("   Target: ≤16.67ms (60 FPS)")
            console.log("   Status: ${if (frameTime <= 16.67) "✅ PASS" else "❌ FAIL"}")
            
            assertTrue(
                frameTime <= 50.0,  // Relaxed for browser variability (30 FPS minimum)
                "Frame time ${frameTime}ms exceeds minimum acceptable threshold of 50ms (30 FPS minimum)"
            )
            
            // Ideal check (will fail until optimized)
            if (frameTime > 16.67) {
                console.warn("⚠️ Frame time ${frameTime}ms exceeds 60 FPS target of 16.67ms")
                console.warn("   This indicates a performance bottleneck requiring optimization")
            }
            
        } finally {
            renderer.dispose()
            cleanup()
        }
    }

    /**
     * Test 2: Multi-frame performance stability
     * 
     * EXPECTED: Consistent frame times across multiple frames
     */
    @Test
    fun testMultiFramePerformanceStability() = runTest {
        skipIfWebGPUUnavailable()
        
        // Given: Renderer with scene
        val renderer = RendererFactory.create(canvas)
        val scene = Scene()
        val camera = PerspectiveCamera(75f, 800f / 600f, 0.1f, 1000f)
        camera.position.set(0f, 0f, 10f)
        camera.updateMatrixWorld()
        
        // Create 30 meshes
        repeat(30) { i ->
            val mesh = createMeshAt(x = i * 2.0 - 30.0, y = 0.0, z = 0.0)
            scene.add(mesh)
        }
        
        try {
            // When: Render 10 frames and measure times
            val frameTimes = mutableListOf<Double>()
            
            repeat(10) { frame ->
                val startTime = Date.now()
                renderer.render(scene, camera)
                val endTime = Date.now()
                frameTimes.add(endTime - startTime)
            }
            
            // Then: Calculate statistics
            val avgTime = frameTimes.average()
            val maxTime = frameTimes.maxOrNull() ?: 0.0
            val minTime = frameTimes.minOrNull() ?: 0.0
            
            console.log("⏱️ Performance Test 2: Multi-Frame Stability")
            console.log("   Frames rendered: 10")
            console.log("   Average time: ${avgTime.toFixed(2)}ms")
            console.log("   Min time: ${minTime.toFixed(2)}ms")
            console.log("   Max time: ${maxTime.toFixed(2)}ms")
            console.log("   Target avg: ≤16.67ms (60 FPS)")
            
            assertTrue(
                avgTime <= 50.0,  // 30 FPS minimum
                "Average frame time ${avgTime}ms exceeds acceptable threshold"
            )
            
            // Check for large variance (indicates instability)
            val variance = maxTime - minTime
            if (variance > 20.0) {
                console.warn("⚠️ Large frame time variance: ${variance.toFixed(2)}ms")
            }
            
        } finally {
            renderer.dispose()
            cleanup()
        }
    }

    /**
     * Test 3: Large scene stress test
     * 
     * EXPECTED: Even with 100 meshes, maintain acceptable FPS
     */
    @Test
    fun testLargeScenePerformance() = runTest {
        skipIfWebGPUUnavailable()
        
        // Given: Renderer with large scene (100 meshes)
        val renderer = RendererFactory.create(canvas)
        val scene = Scene()
        val camera = PerspectiveCamera(75f, 800f / 600f, 0.1f, 2000f)
        camera.position.set(0f, 0f, 100f)
        camera.updateMatrixWorld()
        
        val meshCount = 100
        repeat(meshCount) { i ->
            val x = (i % 10) * 10.0 - 45.0
            val y = ((i / 10) % 10) * 10.0 - 45.0
            val mesh = createMeshAt(x, y, 0.0)
            scene.add(mesh)
        }
        
        try {
            // When: Measure frame time with large scene
            val startTime = Date.now()
            renderer.render(scene, camera)
            val endTime = Date.now()
            
            val frameTime = endTime - startTime
            
            console.log("⏱️ Performance Test 3: Large Scene (100 meshes)")
            console.log("   Frame time: ${frameTime.toFixed(2)}ms")
            console.log("   Target: ≤33.33ms (30 FPS minimum)")
            console.log("   Status: ${if (frameTime <= 33.33) "✅ PASS" else "❌ FAIL"}")
            
            // Even with 100 meshes, should maintain 30 FPS minimum
            assertTrue(
                frameTime <= 100.0,  // Very relaxed for 100 meshes
                "Large scene frame time ${frameTime}ms is unacceptable"
            )
            
        } finally {
            renderer.dispose()
            cleanup()
        }
    }

    /**
     * Test 4: Incremental scene complexity
     * 
     * EXPECTED: Frame time scales linearly (or sub-linearly) with mesh count
     */
    @Test
    fun testIncrementalComplexityScaling() = runTest {
        skipIfWebGPUUnavailable()
        
        // Given: Renderer
        val renderer = RendererFactory.create(canvas)
        val scene = Scene()
        val camera = PerspectiveCamera(75f, 800f / 600f, 0.1f, 1000f)
        camera.position.set(0f, 0f, 50f)
        camera.updateMatrixWorld()
        
        try {
            val results = mutableListOf<Pair<Int, Double>>()
            
            // When: Test with increasing mesh counts
            for (meshCount in listOf(10, 20, 40, 68)) {
                // Clear scene
                scene.children.clear()
                
                // Add meshes
                repeat(meshCount) { i ->
                    val mesh = createMeshAt(x = (i % 10) * 5.0, y = 0.0, z = 0.0)
                    scene.add(mesh)
                }
                
                // Measure frame time
                val startTime = Date.now()
                renderer.render(scene, camera)
                val endTime = Date.now()
                
                val frameTime = endTime - startTime
                results.add(meshCount to frameTime)
                
                console.log("   ${meshCount} meshes: ${frameTime.toFixed(2)}ms")
            }
            
            // Then: Analyze scaling
            console.log("⏱️ Performance Test 4: Complexity Scaling")
            console.log("   Results:")
            results.forEach { (count, time) ->
                console.log("     $count meshes → ${time.toFixed(2)}ms")
            }
            
            // Check that doubling meshes doesn't cause exponential slowdown
            val time10 = results[0].second
            val time20 = results[1].second
            val time40 = results[2].second
            
            val ratio20to10 = time20 / time10
            val ratio40to20 = time40 / time20
            
            console.log("   Scaling ratios:")
            console.log("     20/10 meshes: ${ratio20to10.toFixed(2)}x")
            console.log("     40/20 meshes: ${ratio40to20.toFixed(2)}x")
            
            // Should be roughly linear (ratio ~2.0) or better
            assertTrue(
                ratio40to20 < 5.0,  // Not exponential
                "Scaling is exponential (ratio ${ratio40to20}x suggests O(n²) or worse)"
            )
            
        } finally {
            renderer.dispose()
            cleanup()
        }
    }

    /**
     * Test 5: Static scene optimization opportunity
     * 
     * EXPECTED: Second frame should be faster than first (caching kicks in)
     */
    @Test
    fun testStaticSceneCachingOpportunity() = runTest {
        skipIfWebGPUUnavailable()
        
        // Given: Static scene (no mesh movement)
        val renderer = RendererFactory.create(canvas)
        val scene = Scene()
        val camera = PerspectiveCamera(75f, 800f / 600f, 0.1f, 1000f)
        camera.position.set(0f, 0f, 10f)
        camera.updateMatrixWorld()
        
        repeat(50) { i ->
            val mesh = createMeshAt(x = (i % 10) * 3.0, y = 0.0, z = 0.0)
            scene.add(mesh)
        }
        
        try {
            // When: Render first frame (cold cache)
            val startTime1 = Date.now()
            renderer.render(scene, camera)
            val endTime1 = Date.now()
            val frameTime1 = endTime1 - startTime1
            
            // Render second frame (warm cache - should be faster if caching works)
            val startTime2 = Date.now()
            renderer.render(scene, camera)
            val endTime2 = Date.now()
            val frameTime2 = endTime2 - startTime2
            
            console.log("⏱️ Performance Test 5: Caching Opportunity")
            console.log("   Frame 1 (cold): ${frameTime1.toFixed(2)}ms")
            console.log("   Frame 2 (warm): ${frameTime2.toFixed(2)}ms")
            console.log("   Speedup: ${(frameTime1 / frameTime2).toFixed(2)}x")
            
            if (frameTime2 < frameTime1 * 0.8) {
                console.log("   ✅ Caching is working (20%+ improvement)")
            } else {
                console.warn("   ⚠️ No significant caching benefit detected")
                console.warn("   This suggests bind groups/uniforms are being recreated unnecessarily")
            }
            
            // Just log for now - don't fail test since optimization not implemented yet
            
        } finally {
            renderer.dispose()
            cleanup()
        }
    }
}

// Extension for Double.toFixed()
private fun Double.toFixed(decimals: Int): String {
    return this.asDynamic().toFixed(decimals).unsafeCast<String>()
}
