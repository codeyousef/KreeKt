/**
 * Contract Test: Renderer Interface
 * Feature: 019-we-should-not
 *
 * Validates that Renderer implementations (WebGPU, Vulkan, WebGL) conform to
 * the unified interface contract and behave identically across platforms.
 *
 * Requirements Tested:
 * - FR-001: WebGPU primary for JS
 * - FR-002: Vulkan primary for JVM
 * - FR-004: Automatic backend detection
 * - FR-011: Unified renderer interface
 * - FR-019: 60 FPS target performance
 * - FR-020: Visually identical output across backends
 * - FR-022: Fail-fast with detailed errors
 */

package io.kreekt.renderer.contract

import io.kreekt.renderer.*
import io.kreekt.scene.Scene
import io.kreekt.camera.PerspectiveCamera
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class RendererContractTest {

    private lateinit var renderer: Renderer
    private lateinit var surface: RenderSurface
    private lateinit var scene: Scene
    private lateinit var camera: PerspectiveCamera

    @BeforeTest
    fun setup() {
        surface = createTestSurface(width = 800, height = 600)
        scene = Scene()
        camera = PerspectiveCamera(fov = 75.0f, aspect = 800f / 600f, near = 0.1f, far = 1000f)
    }

    @AfterTest
    fun teardown() {
        if (::renderer.isInitialized) {
            renderer.dispose()
        }
    }

    // FR-004: Automatic backend detection
    @Test
    fun `RendererFactory detects available backends for current platform`() {
        val backends = RendererFactory.detectAvailableBackends()

        // Platform-specific assertions
        when (getPlatform()) {
            Platform.JS -> {
                assertTrue(backends.isNotEmpty(), "JS platform must have at least WebGL fallback")
                assertTrue(
                    backends.contains(BackendType.WEBGPU) || backends.contains(BackendType.WEBGL),
                    "JS must support WebGPU or WebGL"
                )
            }

            Platform.JVM -> {
                assertTrue(
                    backends.contains(BackendType.VULKAN),
                    "JVM platform must support Vulkan (FR-002)"
                )
            }

            Platform.NATIVE -> {
                assertTrue(
                    backends.contains(BackendType.VULKAN),
                    "Native platform must support Vulkan"
                )
            }
        }
    }

    // FR-001, FR-002: Primary backend selection
    @Test
    fun `RendererFactory creates primary backend renderer by default`() = runTest {
        val result = RendererFactory.create(surface)
        assertTrue(result.isSuccess, "Renderer creation should succeed on supported platform")

        renderer = result.getOrThrow()

        when (getPlatform()) {
            Platform.JS -> {
                // Should prefer WebGPU if available, otherwise WebGL
                assertTrue(renderer.backend == BackendType.WEBGPU || renderer.backend == BackendType.WEBGL)
                if (renderer.backend == BackendType.WEBGL) {
                    println("WebGL fallback active (WebGPU unavailable)")
                }
            }

            Platform.JVM, Platform.NATIVE -> {
                assertEquals(BackendType.VULKAN, renderer.backend, "JVM/Native must use Vulkan (FR-002)")
            }
        }
    }

    // FR-011: Unified interface - initialize()
    @Test
    fun `Renderer initialize must succeed with valid config`() = runTest {
        val result = RendererFactory.create(surface)
        renderer = result.getOrThrow()

        // Renderer should be in READY state after creation
        assertNotNull(renderer.capabilities, "Capabilities must be available after init")
        assertTrue(
            renderer.capabilities.maxTextureSize >= 2048,
            "Max texture size must be at least 2048 (FR-024)"
        )
    }

    // FR-022: Fail-fast with detailed errors
    @Test
    fun `RendererFactory throws descriptive exception when no graphics support`() = runTest {
        // Simulate platform with no graphics support
        val invalidSurface = createInvalidSurface()

        val result = RendererFactory.create(invalidSurface)

        assertTrue(result.isFailure, "Should fail with invalid surface")
        val exception = result.exceptionOrNull()
        assertTrue(
            exception is RendererInitializationException,
            "Must throw RendererInitializationException (FR-022)"
        )

        // Exception must contain diagnostic information
        val message = exception?.message ?: ""
        assertTrue(
            message.contains("graphics") || message.contains("support") || message.contains("driver"),
            "Error message must contain diagnostic keywords"
        )
        assertTrue(message.length > 50, "Error message must be detailed (FR-022)")
    }

    // FR-011: Unified interface - render()
    @Test
    fun `Renderer render accepts Scene and Camera`() = runTest {
        renderer = RendererFactory.create(surface).getOrThrow()

        // Should not throw
        assertDoesNotThrow {
            renderer.render(scene, camera)
        }
    }

    // FR-011: Unified interface - getStats()
    @Test
    fun `Renderer getStats returns valid RenderStats`() = runTest {
        renderer = RendererFactory.create(surface).getOrThrow()

        // Render one frame
        renderer.render(scene, camera)

        val stats = renderer.getStats()
        assertNotNull(stats, "Stats must not be null")
        assertTrue(stats.fps >= 0, "FPS must be non-negative")
        assertTrue(stats.frameTime >= 0, "Frame time must be non-negative")
        assertTrue(stats.drawCalls >= 0, "Draw calls must be non-negative")
        assertTrue(stats.triangles >= 0, "Triangles must be non-negative")
    }

    // FR-019: Performance target (60 FPS) - warmup test
    @Test
    fun `Renderer maintains target frame rate with simple scene`() = runTest {
        renderer = RendererFactory.create(surface).getOrThrow()

        // Add simple test geometry
        val testCube = createTestCube()
        scene.add(testCube)

        // Warmup: 120 frames
        repeat(120) {
            renderer.render(scene, camera)
        }

        // Measure: 60 frames
        val startTime = currentTimeMillis()
        repeat(60) {
            renderer.render(scene, camera)
        }
        val elapsed = currentTimeMillis() - startTime

        val averageFps = 60000.0 / elapsed
        val stats = renderer.getStats()

        // Assert 30 FPS minimum (FR-019: 30 FPS minimum acceptable)
        assertTrue(
            averageFps >= 30.0,
            "Renderer must maintain at least 30 FPS (got ${averageFps.format(1)})"
        )

        // Target 60 FPS (may not always achieve, but should be close)
        if (averageFps < 50.0) {
            println("⚠️ Performance warning: ${averageFps.format(1)} FPS (target: 60 FPS)")
        }

        println("✅ Performance: ${averageFps.format(1)} FPS, ${stats.frameTime.format(2)}ms/frame")
    }

    // FR-011: Unified interface - resize()
    @Test
    fun `Renderer resize updates surface dimensions`() = runTest {
        renderer = RendererFactory.create(surface).getOrThrow()

        renderer.resize(1024, 768)

        // Should not throw, and next render should work
        assertDoesNotThrow {
            renderer.render(scene, camera)
        }
    }

    // FR-011: Unified interface - dispose()
    @Test
    fun `Renderer dispose cleans up resources`() = runTest {
        renderer = RendererFactory.create(surface).getOrThrow()

        // Should not throw
        assertDoesNotThrow {
            renderer.dispose()
        }

        // Subsequent operations should fail
        assertFails {
            renderer.render(scene, camera)
        }
    }

    // FR-020: Visually identical output (basic sanity check)
    @Test
    fun `Renderer produces consistent output for identical scenes`() = runTest {
        renderer = RendererFactory.create(surface).getOrThrow()

        val testMesh = createTestCube()
        scene.add(testMesh)

        // Render same scene twice
        renderer.render(scene, camera)
        val stats1 = renderer.getStats()

        renderer.render(scene, camera)
        val stats2 = renderer.getStats()

        // Triangle count should be identical for same scene
        assertEquals(
            stats1.triangles, stats2.triangles,
            "Triangle count must be consistent for identical scenes"
        )

        // Draw calls should be identical
        assertEquals(
            stats1.drawCalls, stats2.drawCalls,
            "Draw calls must be consistent for identical scenes"
        )
    }

    // FR-023: Log selected backend
    @Test
    fun `RendererFactory logs selected backend at initialization`() = runTest {
        // Capture logs (platform-specific implementation)
        val logCapture = captureLog {
            renderer = RendererFactory.create(surface).getOrThrow()
        }

        val backendName = renderer.backend.name
        assertTrue(
            logCapture.contains(backendName),
            "Selected backend ($backendName) must be logged (FR-023)"
        )
    }

    // FR-024: Detect capability mismatches
    @Test
    fun `Renderer fails fast if backend lacks required capabilities`() = runTest {
        renderer = RendererFactory.create(surface).getOrThrow()

        val caps = renderer.capabilities

        // Required minimum capabilities (FR-024)
        assertTrue(
            caps.maxTextureSize >= 2048,
            "maxTextureSize must be at least 2048 (got ${caps.maxTextureSize})"
        )
        assertTrue(
            caps.maxVertexAttributes >= 16,
            "maxVertexAttributes must be at least 16 (got ${caps.maxVertexAttributes})"
        )

        // These checks would have failed at init if not met
        assertNotNull(caps.deviceName, "Device name must be reported")
        assertNotNull(caps.driverVersion, "Driver version must be reported")
    }
}

// Test helpers (platform-specific implementations in actual sourcesets)

expect enum class Platform {
    JS, JVM, NATIVE
}

expect fun getPlatform(): Platform

expect fun createTestSurface(width: Int, height: Int): RenderSurface

expect fun createInvalidSurface(): RenderSurface

expect fun createTestCube(): io.kreekt.scene.Mesh

expect fun currentTimeMillis(): Long

expect fun captureLog(block: () -> Unit): String

expect fun Double.format(decimals: Int): String

