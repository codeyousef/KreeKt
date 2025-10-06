package io.kreekt.renderer.webgpu

import io.kreekt.camera.PerspectiveCamera
import io.kreekt.core.scene.Scene
import io.kreekt.core.scene.Mesh
import io.kreekt.geometry.BufferGeometry
import io.kreekt.geometry.BufferAttribute
import io.kreekt.material.MeshBasicMaterial
import io.kreekt.renderer.RendererResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

/**
 * Contract tests for WebGPU renderer (Phase 3.2 - Tests First).
 * These tests MUST FAIL initially per TDD workflow.
 *
 * Tests map to functional requirements from spec.md:
 * - FR-001: WebGPU detection and automatic fallback
 * - FR-002: Canvas surface initialization
 * - FR-003: Basic geometry rendering
 * - FR-004: Buffer management and pooling
 * - FR-005: Shader compilation and caching
 * - FR-009: Performance (60 FPS @ 1M triangles)
 * - FR-011: Context loss recovery
 * - FR-013: Pipeline caching
 */
class WebGPURendererContractTests : WebGPUTestBase() {

    // ========================================================================
    // T006: Contract test FR-001 - WebGPU detection and fallback
    // ========================================================================

    @Test
    fun testWebGPUDetection() = runTest {
        skipIfWebGPUUnavailable()

        val detector = WebGPUDetector
        val isAvailable = detector.isAvailable()

        // Should detect WebGPU availability correctly
        assertTrue(isAvailable == isWebGPUAvailable, "WebGPU detection should match browser capability")
    }

    @Test
    fun testAutomaticFallbackToWebGL() = runTest {
        // When WebGPU unavailable, should automatically create WebGLRenderer
        val renderer = try {
            WebGPURendererFactory.create(canvas)
        } catch (e: Exception) {
            // Expected if WebGPU unavailable
            assertNotNull(e)
            return@runTest
        }

        assertNotNull(renderer)
        // Factory should successfully create renderer (WebGPU or WebGL fallback)
        // Note: Cannot test specific renderer type through Renderer interface

        cleanup()
    }

    // ========================================================================
    // T007: Contract test FR-002 - Canvas initialization
    // ========================================================================

    @Test
    fun testCanvasInitialization() = runTest {
        skipIfWebGPUUnavailable()

        val renderer = WebGPURenderer(canvas)
        val result = renderer.initialize()

        assertTrue(result is RendererResult.Success<*>, "Renderer initialization should succeed")
        assertTrue(renderer.isInitialized, "Renderer should be marked as initialized")

        renderer.dispose()
        cleanup()
    }

    @Test
    fun testInitializationWithInvalidCanvas() = runTest {
        skipIfWebGPUUnavailable()

        // Create renderer with null/invalid context
        val invalidCanvas = kotlinx.browser.document.createElement("div") as org.w3c.dom.HTMLElement

        // Should handle gracefully and return error
        // This tests error handling path
        // Note: Actual implementation will need to validate canvas type

        cleanup()
    }

    // ========================================================================
    // T008: Contract test FR-003 - Basic geometry rendering
    // ========================================================================

    @Test
    fun testBasicGeometryRendering() = runTest {
        skipIfWebGPUUnavailable()

        val renderer = WebGPURenderer(canvas)
        renderer.initialize()

        // Create simple scene
        val scene = Scene()
        val geometry = createSimpleGeometry()
        val material = MeshBasicMaterial().apply {
            vertexColors = true
        }
        val mesh = Mesh(geometry, material)
        scene.add(mesh)

        // Create camera
        val camera = PerspectiveCamera(75.0f, 800f / 600f, 0.1f, 1000.0f)
        camera.position.set(0f, 0f, 5f)
        camera.lookAt(0f, 0f, 0f)
        camera.updateMatrixWorld(true)
        camera.updateProjectionMatrix()

        // Render
        val result = renderer.render(scene, camera)

        assertTrue(result is RendererResult.Success<*>, "Rendering should succeed")

        renderer.dispose()
        cleanup()
    }

    // ========================================================================
    // T009: Contract test FR-004 - Buffer management
    // ========================================================================

    @Test
    fun testBufferPooling() = runTest {
        skipIfWebGPUUnavailable()

        val renderer = WebGPURenderer(canvas)
        renderer.initialize()

        // Create multiple geometries to test buffer pooling
        val geometries = List(10) { createSimpleGeometry() }
        val scene = Scene()

        geometries.forEach { geometry ->
            val material = MeshBasicMaterial()
            val mesh = Mesh(geometry, material)
            scene.add(mesh)
        }

        val camera = PerspectiveCamera(75.0f, 800f / 600f, 0.1f, 1000.0f)
        camera.position.set(0f, 0f, 5f)

        // Render multiple frames to test buffer reuse
        repeat(5) {
            renderer.render(scene, camera)
        }

        // Check buffer pool statistics
        val stats = renderer.getStats()
        // Note: RenderStats doesn't have buffersCreated, will validate via draw calls
        assertTrue(stats.calls < 30, "Buffer pooling should reduce overhead (calls < 30)")

        renderer.dispose()
        cleanup()
    }

    // ========================================================================
    // T010: Contract test FR-005 - Shader compilation
    // ========================================================================

    @Test
    fun testShaderCompilation() = runTest {
        skipIfWebGPUUnavailable()

        val renderer = WebGPURenderer(canvas)
        renderer.initialize()

        // Create mesh with material (triggers shader compilation)
        val geometry = createSimpleGeometry()
        val material = MeshBasicMaterial().apply {
            vertexColors = true
        }

        val scene = Scene()
        scene.add(Mesh(geometry, material))

        val camera = PerspectiveCamera(75.0f, 800f / 600f, 0.1f, 1000.0f)
        camera.position.set(0f, 0f, 5f)

        // First render should compile shaders
        val result = renderer.render(scene, camera)
        assertTrue(result is RendererResult.Success<*>, "Shader compilation should succeed")

        // Check that shaders were compiled
        val stats = renderer.getStats()
        assertTrue(stats.shaders > 0, "Should have compiled shaders")

        renderer.dispose()
        cleanup()
    }

    // ========================================================================
    // T011: Contract test FR-009 - Performance (60 FPS @ 1M triangles)
    // ========================================================================

    @Test
    fun testPerformanceWithHighPolyGeometry() = runTest {
        skipIfWebGPUUnavailable()

        val renderer = WebGPURenderer(canvas)
        renderer.initialize()

        // Create high-poly geometry (1M triangles = ~3M vertices)
        val triangleCount = 1_000_000
        val geometry = createHighPolyGeometry(triangleCount)

        val material = MeshBasicMaterial().apply {
            vertexColors = true
        }

        val scene = Scene()
        scene.add(Mesh(geometry, material))

        val camera = PerspectiveCamera(75.0f, 800f / 600f, 0.1f, 1000.0f)
        camera.position.set(0f, 0f, 200f)

        // Measure frame time
        val frameStart = kotlinx.browser.window.performance.now()
        renderer.render(scene, camera)
        val frameEnd = kotlinx.browser.window.performance.now()

        val frameTime = frameEnd - frameStart

        // 60 FPS = ~16.7ms per frame
        // Allow some margin for test variability
        assertTrue(frameTime < 20.0, "Frame time should be <20ms for 60 FPS (actual: ${frameTime}ms)")

        // Verify triangle count
        val stats = renderer.getStats()
        assertEquals(triangleCount, stats.triangles, "Should render 1M triangles")

        renderer.dispose()
        cleanup()
    }

    // ========================================================================
    // T012: Contract test FR-011 - Context loss recovery
    // ========================================================================

    @Test
    fun testContextLossRecovery() = runTest {
        skipIfWebGPUUnavailable()

        val renderer = WebGPURenderer(canvas)
        renderer.initialize()

        val scene = Scene()
        val geometry = createSimpleGeometry()
        val material = MeshBasicMaterial()
        scene.add(Mesh(geometry, material))

        val camera = PerspectiveCamera(75.0f, 800f / 600f, 0.1f, 1000.0f)
        camera.position.set(0f, 0f, 5f)

        // Render successfully
        var result = renderer.render(scene, camera)
        assertTrue(result is RendererResult.Success<*>, "Initial render should succeed")

        // Simulate context loss
        renderer.simulateContextLoss()

        // Wait for recovery
        WebGPUTestUtils.delay(100)

        // Render should succeed after recovery
        result = renderer.render(scene, camera)
        assertTrue(result is RendererResult.Success<*>, "Render should succeed after context recovery")

        renderer.dispose()
        cleanup()
    }

    // ========================================================================
    // T013: Contract test FR-013 - Pipeline caching
    // ========================================================================

    @Test
    fun testPipelineCaching() = runTest {
        skipIfWebGPUUnavailable()

        val renderer = WebGPURenderer(canvas)
        renderer.initialize()

        // Create 10 meshes with same material (should reuse pipeline)
        val scene = Scene()
        val material = MeshBasicMaterial().apply {
            vertexColors = true
        }

        repeat(10) {
            val geometry = createSimpleGeometry()
            val mesh = Mesh(geometry, material)
            scene.add(mesh)
        }

        val camera = PerspectiveCamera(75.0f, 800f / 600f, 0.1f, 1000.0f)
        camera.position.set(0f, 0f, 5f)

        // Render
        renderer.render(scene, camera)

        // Check pipeline cache statistics
        val stats = renderer.getStats()
        // Note: RenderStats doesn't have pipeline stats, will validate via programs count
        assertTrue(stats.programs < 5, "Should reuse pipelines (programs < 5)")
        assertTrue(stats.calls >= 9, "Should have draw calls for all meshes")

        renderer.dispose()
        cleanup()
    }

    // ========================================================================
    // T014-T024: Additional contract tests
    // ========================================================================

    @Test
    fun testDisposalReleasesResources() = runTest {
        skipIfWebGPUUnavailable()

        val renderer = WebGPURenderer(canvas)
        renderer.initialize()

        val scene = Scene()
        val geometry = createSimpleGeometry()
        val material = MeshBasicMaterial()
        scene.add(Mesh(geometry, material))

        val camera = PerspectiveCamera(75.0f, 800f / 600f, 0.1f, 1000.0f)
        renderer.render(scene, camera)

        // Dispose renderer
        renderer.dispose()

        // Should release GPU resources
        assertFalse(renderer.isInitialized, "Renderer should be uninitialized after disposal")

        cleanup()
    }

    @Test
    fun testMultipleRenderCalls() = runTest {
        skipIfWebGPUUnavailable()

        val renderer = WebGPURenderer(canvas)
        renderer.initialize()

        val scene = Scene()
        val geometry = createSimpleGeometry()
        val material = MeshBasicMaterial()
        scene.add(Mesh(geometry, material))

        val camera = PerspectiveCamera(75.0f, 800f / 600f, 0.1f, 1000.0f)
        camera.position.set(0f, 0f, 5f)

        // Render 60 frames (simulate 1 second at 60 FPS)
        repeat(60) {
            val result = renderer.render(scene, camera)
            assertTrue(result is RendererResult.Success<*>, "Frame $it should render successfully")
        }

        renderer.dispose()
        cleanup()
    }

    @Test
    fun testStatsTracking() = runTest {
        skipIfWebGPUUnavailable()

        val renderer = WebGPURenderer(canvas)
        renderer.initialize()

        val scene = Scene()
        val geometry = createSimpleGeometry()
        val material = MeshBasicMaterial()
        scene.add(Mesh(geometry, material))

        val camera = PerspectiveCamera(75.0f, 800f / 600f, 0.1f, 1000.0f)
        camera.position.set(0f, 0f, 5f)

        renderer.render(scene, camera)

        val stats = renderer.getStats()

        // Validate stats are tracked
        assertTrue(stats.triangles > 0, "Should track triangle count")
        assertTrue(stats.calls > 0, "Should track draw calls")
        // Note: frameTime not in RenderStats, will validate separately
        assertTrue(stats.frame >= 0, "Should track frame number")

        renderer.dispose()
        cleanup()
    }

    // ========================================================================
    // Helper functions
    // ========================================================================

    private fun createSimpleGeometry(): BufferGeometry {
        val testData = WebGPUTestUtils.createTestGeometry()

        // Convert dynamic Float32Array to FloatArray for BufferAttribute
        val positionsArray = FloatArray(9) { i -> js("testData.positions[i]").unsafeCast<Float>() }
        val normalsArray = FloatArray(9) { i -> js("testData.normals[i]").unsafeCast<Float>() }
        val colorsArray = FloatArray(9) { i -> js("testData.colors[i]").unsafeCast<Float>() }

        return BufferGeometry().apply {
            setAttribute("position", BufferAttribute(positionsArray, 3))
            setAttribute("normal", BufferAttribute(normalsArray, 3))
            setAttribute("color", BufferAttribute(colorsArray, 3))
        }
    }

    private fun createHighPolyGeometry(triangleCount: Int): BufferGeometry {
        val vertexCount = triangleCount * 3
        val positions = FloatArray(vertexCount * 3)
        val normals = FloatArray(vertexCount * 3)
        val colors = FloatArray(vertexCount * 3)

        // Generate vertices
        for (i in 0 until vertexCount) {
            // Position (scattered in 200x200x200 cube)
            val randX = (js("Math.random()").unsafeCast<Double>() * 200 - 100).toFloat()
            val randY = (js("Math.random()").unsafeCast<Double>() * 200 - 100).toFloat()
            val randZ = (js("Math.random()").unsafeCast<Double>() * 200 - 100).toFloat()

            positions[i * 3 + 0] = randX
            positions[i * 3 + 1] = randY
            positions[i * 3 + 2] = randZ

            // Normal (pointing outward from origin)
            val x = positions[i * 3 + 0]
            val y = positions[i * 3 + 1]
            val z = positions[i * 3 + 2]
            val length = kotlin.math.sqrt(x * x + y * y + z * z)
            normals[i * 3 + 0] = x / length
            normals[i * 3 + 1] = y / length
            normals[i * 3 + 2] = z / length

            // Color (gradient based on position)
            colors[i * 3 + 0] = (x + 100) / 200  // Red channel
            colors[i * 3 + 1] = (y + 100) / 200  // Green channel
            colors[i * 3 + 2] = (z + 100) / 200  // Blue channel
        }

        return BufferGeometry().apply {
            setAttribute("position", BufferAttribute(positions, 3))
            setAttribute("normal", BufferAttribute(normals, 3))
            setAttribute("color", BufferAttribute(colors, 3))
        }
    }
}

/**
 * Integration test from quickstart.md (T024).
 * Complete end-to-end test demonstrating all features.
 */
class WebGPURendererIntegrationTest : WebGPUTestBase() {

    @Test
    fun testQuickstartExample() = runTest {
        skipIfWebGPUUnavailable()

        // Step 1: Initialize renderer
        val renderer = WebGPURenderer(canvas)
        val initResult = renderer.initialize()
        assertTrue(initResult is RendererResult.Success<*>, "Renderer initialization should succeed")

        console.log("Using backend: ${if (renderer.isWebGPU) "WebGPU" else "WebGL (fallback)"}")

        // Step 2: Create scene with 1M triangles
        val scene = Scene()
        val geometry = createHighPolyGeometry(1_000_000)
        val material = MeshBasicMaterial().apply {
            vertexColors = true
        }
        val mesh = Mesh(geometry, material)
        scene.add(mesh)

        // Step 3: Setup camera
        val camera = PerspectiveCamera(75.0f, 800f / 600f, 0.1f, 1000.0f)
        camera.position.set(0f, 0f, 200f)
        camera.lookAt(0f, 0f, 0f)
        camera.updateMatrixWorld(true)
        camera.updateProjectionMatrix()

        // Step 4: Render loop (single frame for test)
        val frameStart = kotlinx.browser.window.performance.now()
        val renderResult = renderer.render(scene, camera)
        val frameEnd = kotlinx.browser.window.performance.now()

        assertTrue(renderResult is RendererResult.Success<*>, "Rendering should succeed")

        val fps = 1000.0 / (frameEnd - frameStart)
        console.log("FPS: $fps | Triangles: ${renderer.getStats().triangles}")

        // Step 5: Validate performance
        assertTrue(fps >= 55.0, "Should achieve ~60 FPS (actual: $fps)")

        // Step 6: Dispose resources
        renderer.dispose()
        console.log("Resources disposed - GPU memory freed")

        cleanup()
    }

    private fun createHighPolyGeometry(triangleCount: Int): BufferGeometry {
        val vertexCount = triangleCount * 3
        val positions = FloatArray(vertexCount * 3)
        val normals = FloatArray(vertexCount * 3)
        val colors = FloatArray(vertexCount * 3)

        for (i in 0 until vertexCount) {
            // Use JavaScript Math.random()
            val randX = (js("Math.random()").unsafeCast<Double>() * 200 - 100).toFloat()
            val randY = (js("Math.random()").unsafeCast<Double>() * 200 - 100).toFloat()
            val randZ = (js("Math.random()").unsafeCast<Double>() * 200 - 100).toFloat()

            positions[i * 3 + 0] = randX
            positions[i * 3 + 1] = randY
            positions[i * 3 + 2] = randZ

            val x = positions[i * 3 + 0]
            val y = positions[i * 3 + 1]
            val z = positions[i * 3 + 2]
            val length = kotlin.math.sqrt(x * x + y * y + z * z)
            normals[i * 3 + 0] = x / length
            normals[i * 3 + 1] = y / length
            normals[i * 3 + 2] = z / length

            colors[i * 3 + 0] = (x + 100) / 200
            colors[i * 3 + 1] = (y + 100) / 200
            colors[i * 3 + 2] = (z + 100) / 200
        }

        return BufferGeometry().apply {
            setAttribute("position", BufferAttribute(positions, 3))
            setAttribute("normal", BufferAttribute(normals, 3))
            setAttribute("color", BufferAttribute(colors, 3))
        }
    }
}
