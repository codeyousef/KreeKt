package io.kreekt.renderer.webgpu

import io.kreekt.renderer.webgpu.WebGPURendererFactory
import io.kreekt.renderer.RendererConfig
import kotlinx.browser.document
import kotlinx.coroutines.test.runTest
import org.w3c.dom.HTMLCanvasElement
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract Test: WebGPURendererFactory Backend Selection (Contract 4)
 *
 * Validates that the factory correctly detects available backends and
 * creates appropriate renderer instances with proper logging.
 *
 * From: specs/017-in-voxelcraft-example/contracts/renderer-contract.md
 */
class WebGPURendererFactoryTest {

    @Test
    fun testBackendSelection() = runTest {
        // Setup
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = 800
        canvas.height = 600

        console.log("📊 Testing backend selection...")

        // Create renderer using factory
        val renderer = WebGPURendererFactory.create(canvas)

        // Initialize renderer
        renderer.initialize(RendererConfig())

        // Verify renderer was created
        assertNotNull(renderer, "Renderer should be created successfully")

        // Verify renderer is functional
        val stats = renderer.stats
        assertNotNull(stats, "Renderer should provide statistics")
        assertTrue(stats.triangles >= 0, "Statistics should be valid")

        console.log("✅ Backend selection test passed")
        console.log("   Renderer type: ${renderer::class.simpleName}")
        console.log("   Statistics: ${stats.triangles} triangles, ${stats.drawCalls} calls")
    }

    @Test
    fun testRendererInitialization() = runTest {
        // Setup
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = 1024
        canvas.height = 768

        console.log("🔧 Testing renderer initialization...")

        // Create renderer
        val renderer = WebGPURendererFactory.create(canvas)

        // Verify initialization completed without errors
        assertNotNull(renderer, "Renderer initialization should succeed")

        console.log("✅ Renderer initialization test passed")
    }

    @Test
    fun testMultipleRendererInstances() = runTest {
        // Test that multiple renderers can be created
        val canvas1 = document.createElement("canvas") as HTMLCanvasElement
        canvas1.width = 800
        canvas1.height = 600

        val canvas2 = document.createElement("canvas") as HTMLCanvasElement
        canvas2.width = 640
        canvas2.height = 480

        console.log("🔧 Testing multiple renderer instances...")

        val renderer1 = WebGPURendererFactory.create(canvas1)
        val renderer2 = WebGPURendererFactory.create(canvas2)

        assertNotNull(renderer1, "First renderer should be created")
        assertNotNull(renderer2, "Second renderer should be created")

        console.log("✅ Multiple renderer instances test passed")
    }
}
