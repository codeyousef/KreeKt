package io.kreekt.renderer.webgpu

import io.kreekt.camera.PerspectiveCamera
import io.kreekt.core.geometry.BoxGeometry
import io.kreekt.core.scene.Mesh
import io.kreekt.core.scene.Scene
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import kotlin.test.*

/**
 * T021 Performance Profiling Tests
 * 
 * These tests diagnose the actual bottleneck in the rendering pipeline by measuring
 * real frame times with 81 meshes (matching VoxelCraft chunk count).
 * 
 * Expected to confirm that frame times are ~333ms (3 FPS) due to multiple writeBuffer calls.
 */
class WebGPUFrameProfilingTest {

    private lateinit var renderer: WebGPURenderer
    private lateinit var scene: Scene
    private lateinit var camera: PerspectiveCamera
    private val testMeshes = mutableListOf<Mesh>()

    @BeforeTest
    fun setup() = kotlinx.coroutines.test.runTest {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = 800
        canvas.height = 600
        
        renderer = WebGPURenderer(canvas)
        renderer.initialize()
        
        camera = PerspectiveCamera(75.0, 800.0 / 600.0, 0.1, 1000.0)
        camera.position.set(0.0, 0.0, 5.0)
        
        scene = Scene()
        
        // Create 81 test meshes (matching VoxelCraft chunk count)
        val geometry = BoxGeometry(1.0, 1.0, 1.0)
        repeat(81) { i ->
            val mesh = Mesh(geometry)
            mesh.position.set((i % 9).toDouble(), 0.0, (i / 9).toDouble())
            scene.add(mesh)
            testMeshes.add(mesh)
        }
    }

    // ========================================
    // Suite 1: Real-World Frame Time Tests
    // ========================================

    @Test
    fun testActualFrameTimeWith81Meshes() {
        console.log("🧪 Test 1: Measuring frame time with 81 meshes (VoxelCraft simulation)...")
        console.log("📊 This test measures real-world performance matching VoxelCraft's 81 chunks")
        
        val frameTimes = mutableListOf<Double>()
        
        // Warm up (first few frames may be slower)
        repeat(10) {
            renderer.render(scene, camera)
        }
        
        // Measure 50 frames
        repeat(50) {
            val frameStart = window.performance.now()
            renderer.render(scene, camera)
            val frameTime = window.performance.now() - frameStart
            frameTimes.add(frameTime)
        }
        
        val avgFrameTime = frameTimes.average()
        val minFrameTime = frameTimes.minOrNull() ?: 0.0
        val maxFrameTime = frameTimes.maxOrNull() ?: 0.0
        val fps = 1000.0 / avgFrameTime
        
        console.log("📊 Frame Time Statistics:")
        console.log("   Average: ${avgFrameTime.toInt()}ms")
        console.log("   Min: ${minFrameTime.toInt()}ms")
        console.log("   Max: ${maxFrameTime.toInt()}ms")
        console.log("   FPS: ${fps.toInt()}")
        
        if (avgFrameTime > 100.0) {
            console.log("🔴 BOTTLENECK CONFIRMED: Frame time ${avgFrameTime.toInt()}ms (${fps.toInt()} FPS)")
            console.log("🔴 Expected <16.67ms for 60 FPS, got ${avgFrameTime.toInt()}ms")
            console.log("🔴 This confirms the 3 FPS issue in VoxelCraft")
        } else if (avgFrameTime > 33.0) {
            console.log("⚠️ WARNING: Frame time ${avgFrameTime.toInt()}ms (${fps.toInt()} FPS)")
            console.log("⚠️ Below 60 FPS target, but above constitutional 30 FPS minimum")
        } else if (avgFrameTime < 16.67) {
            console.log("✅ EXCELLENT: Frame time ${avgFrameTime.toInt()}ms (${fps.toInt()} FPS)")
            console.log("✅ Exceeds 60 FPS constitutional requirement!")
        } else {
            console.log("✅ GOOD: Frame time ${avgFrameTime.toInt()}ms (${fps.toInt()} FPS)")
            console.log("✅ Meets 60 FPS constitutional requirement")
        }
        
        // Log to help diagnose the issue
        console.log("")
        console.log("💡 Diagnostic Information:")
        console.log("   - Scene has ${scene.children.size} meshes")
        console.log("   - Current implementation calls updateUniforms() ${scene.children.size} times per frame")
        console.log("   - Each updateUniforms() does a separate device.queue.writeBuffer() call")
        console.log("   - Solution: Batch all uniform updates into ONE writeBuffer() call")
    }

    @Test
    fun testFrameTimeGoal() {
        console.log("🧪 Test 2: Measuring target frame time (60 FPS = 16.67ms)")
        console.log("📊 Constitutional requirement: 60 FPS with 100k triangles minimum")
        
        val frameTimes = mutableListOf<Double>()
        
        // Warm up
        repeat(5) {
            renderer.render(scene, camera)
        }
        
        // Measure 30 frames
        repeat(30) {
            val frameStart = window.performance.now()
            renderer.render(scene, camera)
            val frameTime = window.performance.now() - frameStart
            frameTimes.add(frameTime)
        }
        
        val avgFrameTime = frameTimes.average()
        val fps = 1000.0 / avgFrameTime
        val targetFrameTime = 16.67 // 60 FPS
        val actualSlowdown = avgFrameTime / targetFrameTime
        
        console.log("📊 Results:")
        console.log("   Target frame time: ${targetFrameTime}ms (60 FPS)")
        console.log("   Actual frame time: ${avgFrameTime.toInt()}ms (${fps.toInt()} FPS)")
        console.log("   Performance ratio: ${actualSlowdown.toInt()}x slower than target")
        
        if (actualSlowdown > 15.0) {
            console.log("🔴 CRITICAL: ${actualSlowdown.toInt()}x slower than 60 FPS target")
            console.log("🔴 Need to optimize: Expected improvement from batching: ~20x faster")
        } else if (actualSlowdown > 2.0) {
            console.log("⚠️ WARNING: ${actualSlowdown.toInt()}x slower than 60 FPS target")
        } else {
            console.log("✅ PASS: Within acceptable range of 60 FPS target")
        }
    }

    @Test
    fun testPerformanceConsistency() {
        console.log("🧪 Test 3: Measuring frame time consistency (checking for variance)")
        
        val frameTimes = mutableListOf<Double>()
        
        // Measure 100 frames to detect variance
        repeat(100) {
            val frameStart = window.performance.now()
            renderer.render(scene, camera)
            val frameTime = window.performance.now() - frameStart
            frameTimes.add(frameTime)
        }
        
        val avgFrameTime = frameTimes.average()
        val variance = frameTimes.map { (it - avgFrameTime).let { diff -> diff * diff } }.average()
        val stdDev = kotlin.math.sqrt(variance)
        val coefficientOfVariation = (stdDev / avgFrameTime) * 100
        
        console.log("📊 Consistency Analysis:")
        console.log("   Average: ${avgFrameTime.toInt()}ms")
        console.log("   Std Dev: ${stdDev.toInt()}ms")
        console.log("   Variation: ${coefficientOfVariation.toInt()}%")
        
        if (coefficientOfVariation < 10.0) {
            console.log("✅ CONSISTENT: Frame times are stable (${coefficientOfVariation.toInt()}% variation)")
        } else if (coefficientOfVariation < 25.0) {
            console.log("⚠️ MODERATE: Frame times have some variance (${coefficientOfVariation.toInt()}% variation)")
        } else {
            console.log("🔴 INCONSISTENT: Frame times are unstable (${coefficientOfVariation.toInt()}% variation)")
        }
    }

    // ========================================
    // Helper Functions
    // ========================================

    private fun measureTime(block: () -> Unit): Double {
        val start = window.performance.now()
        block()
        return window.performance.now() - start
    }

    @AfterTest
    fun cleanup() {
        renderer.dispose()
    }
}
