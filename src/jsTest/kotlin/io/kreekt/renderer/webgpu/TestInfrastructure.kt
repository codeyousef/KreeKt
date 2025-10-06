package io.kreekt.renderer.webgpu

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import kotlin.test.BeforeTest

/**
 * Test infrastructure for WebGPU renderer tests.
 * Provides canvas mocking and WebGPU detection.
 */
abstract class WebGPUTestBase {
    protected lateinit var canvas: HTMLCanvasElement
    protected var isWebGPUAvailable: Boolean = false

    @BeforeTest
    fun setupCanvas() {
        // Create a test canvas element
        canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = 800
        canvas.height = 600
        canvas.id = "test-canvas-${js("Date.now()")}"

        // Append to document body for context creation
        document.body?.appendChild(canvas)

        // Check WebGPU availability
        isWebGPUAvailable = checkWebGPUAvailability()
    }

    private fun checkWebGPUAvailability(): Boolean {
        return try {
            js("'gpu' in navigator").unsafeCast<Boolean>()
        } catch (e: Exception) {
            false
        }
    }

    protected fun skipIfWebGPUUnavailable() {
        if (!isWebGPUAvailable) {
            console.warn("WebGPU not available, skipping test")
            // Note: Kotlin/JS test framework doesn't have a built-in skip mechanism
            // Tests will simply return early if WebGPU is unavailable
        }
    }

    protected fun cleanup() {
        // Remove canvas from document
        canvas.parentNode?.removeChild(canvas)
    }

    protected fun createTestFloatArray(size: Int): dynamic {
        return js("new Float32Array(size)")
    }

    protected fun createTestUint16Array(size: Int): dynamic {
        return js("new Uint16Array(size)")
    }

    protected fun setFloatArrayValue(array: dynamic, index: Int, value: Float) {
        js("array[index] = value")
    }

    protected fun getFloatArrayValue(array: dynamic, index: Int): Float {
        return js("array[index]").unsafeCast<Float>()
    }
}

/**
 * Mock GPU navigator for testing.
 * Used when WebGPU is unavailable.
 */
object MockGPU {
    fun createMockAdapter(): dynamic {
        return js("""({
            features: new Set(),
            limits: {
                maxTextureDimension1D: 8192,
                maxTextureDimension2D: 8192,
                maxTextureDimension3D: 2048,
                maxTextureArrayLayers: 256,
                maxBindGroups: 4,
                maxDynamicUniformBuffersPerPipelineLayout: 8,
                maxDynamicStorageBuffersPerPipelineLayout: 4,
                maxSampledTexturesPerShaderStage: 16,
                maxSamplersPerShaderStage: 16,
                maxStorageBuffersPerShaderStage: 8,
                maxStorageTexturesPerShaderStage: 4,
                maxUniformBuffersPerShaderStage: 12,
                maxUniformBufferBindingSize: 65536,
                maxStorageBufferBindingSize: 134217728,
                maxVertexBuffers: 8,
                maxVertexAttributes: 16,
                maxVertexBufferArrayStride: 2048,
                maxInterStageShaderComponents: 60,
                maxComputeWorkgroupStorageSize: 16384,
                maxComputeInvocationsPerWorkgroup: 256,
                maxComputeWorkgroupSizeX: 256,
                maxComputeWorkgroupSizeY: 256,
                maxComputeWorkgroupSizeZ: 64,
                maxComputeWorkgroupsPerDimension: 65535
            },
            isFallbackAdapter: false,
            requestDevice: function() {
                return Promise.reject(new Error('Mock adapter - device creation not supported'));
            }
        })""")
    }
}

/**
 * Test utilities for WebGPU renderer.
 */
object WebGPUTestUtils {
    /**
     * Creates a simple test geometry with positions, normals, and colors.
     * Returns dynamic object with Float32Array properties
     */
    fun createTestGeometry(): dynamic {
        val positions = js("new Float32Array(9)")
        // Triangle vertices
        js("positions[0] = -1.0; positions[1] = -1.0; positions[2] = 0.0")
        js("positions[3] =  1.0; positions[4] = -1.0; positions[5] = 0.0")
        js("positions[6] =  0.0; positions[7] =  1.0; positions[8] = 0.0")

        val normals = js("new Float32Array(9)")
        // All pointing forward
        for (i in 0..2) {
            js("normals[i * 3 + 0] = 0.0")
            js("normals[i * 3 + 1] = 0.0")
            js("normals[i * 3 + 2] = 1.0")
        }

        val colors = js("new Float32Array(9)")
        // RGB colors
        js("colors[0] = 1.0; colors[1] = 0.0; colors[2] = 0.0")  // Red
        js("colors[3] = 0.0; colors[4] = 1.0; colors[5] = 0.0")  // Green
        js("colors[6] = 0.0; colors[7] = 0.0; colors[8] = 1.0")  // Blue

        val indices = js("new Uint16Array([0, 1, 2])")

        return js("({ positions: positions, normals: normals, colors: colors, indices: indices })")
    }

    /**
     * Creates an identity matrix (mat4x4).
     */
    fun createIdentityMatrix(): FloatArray {
        return floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    }

    /**
     * Waits for next animation frame (for async rendering tests).
     */
    suspend fun waitForFrame(): Unit = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        window.requestAnimationFrame {
            continuation.resumeWith(Result.success(Unit))
        }
    }

    /**
     * Waits for specified milliseconds.
     */
    suspend fun delay(ms: Int): Unit = kotlinx.coroutines.delay(ms.toLong())
}
