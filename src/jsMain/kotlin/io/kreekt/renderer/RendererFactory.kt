/**
 * T026: RendererFactory Actual (JS)
 * Feature: 019-we-should-not
 *
 * JS implementation of RendererFactory with WebGPU → WebGL fallback.
 */

package io.kreekt.renderer

import io.kreekt.renderer.webgpu.WebGPURenderer
import io.kreekt.renderer.webgpu.WebGPUSurface
import org.w3c.dom.HTMLCanvasElement

/**
 * JS actual for RendererFactory.
 *
 * Creates WebGPURenderer (primary) with WebGLRenderer fallback for JS platform.
 *
 * Feature Requirements:
 * - FR-001: WebGPU primary for JavaScript/browser
 * - FR-003: WebGL 2.0 fallback only
 */
actual object RendererFactory {

    /**
     * Create renderer for JS platform with WebGPU → WebGL fallback.
     *
     * Process:
     * 1. Detect available backends
     * 2. Try WebGPU first (if available and not explicitly disabled)
     * 3. Fallback to WebGL if WebGPU unavailable or init fails
     * 4. Return error if both fail
     *
     * @param surface WebGPUSurface wrapping HTMLCanvasElement
     * @param config Renderer configuration
     * @return Result with Renderer or RendererInitializationException on failure
     */
    actual suspend fun create(
        surface: RenderSurface,
        config: RendererConfig
    ): Result<Renderer, RendererInitializationException> {
        // 1. Check if surface is valid
        if (surface !is WebGPUSurface) {
            return Result.failure(
                RendererInitializationException.SurfaceCreationFailedException(
                    BackendType.WEBGPU,
                    "Expected WebGPUSurface, got ${surface::class.simpleName}"
                )
            )
        }

        val canvas = surface.getCanvasElement()

        // 2. Detect available backends
        val availableBackends = detectAvailableBackends()

        // 3. Try WebGPU first (FR-001: primary for JS)
        val preferWebGPU = config.preferredBackend != BackendType.WEBGL &&
                BackendType.WEBGPU in availableBackends

        if (preferWebGPU) {
            try {
                // Create WebGPURenderer
                val renderer = createWebGPURenderer(canvas, config)

                // Initialize renderer
                val initResult = renderer.initialize(config)
                if (initResult.isSuccess) {
                    console.log("[KreeKt] Selected backend: WebGPU")
                    return Result.success(renderer)
                } else {
                    console.warn("[KreeKt] WebGPU initialization failed: ${initResult.exceptionOrNull()?.message}")
                }
            } catch (e: Throwable) {
                console.warn("[KreeKt] WebGPU creation failed: ${e.message}, falling back to WebGL")
            }
        }

        // 4. Fallback to WebGL (FR-003: WebGL fallback only)
        if (BackendType.WEBGL in availableBackends) {
            try {
                val renderer = createWebGLRenderer(canvas, config)
                val initResult = renderer.initialize(config)
                if (initResult.isSuccess) {
                    console.log("[KreeKt] Selected backend: WebGL 2.0 (fallback)")
                    return Result.success(renderer)
                } else {
                    return Result.failure(initResult.exceptionOrNull() as RendererInitializationException)
                }
            } catch (e: Throwable) {
                return Result.failure(
                    RendererInitializationException.DeviceCreationFailedException(
                        BackendType.WEBGL,
                        "WebGL context",
                        e.message ?: "Unknown error"
                    )
                )
            }
        }

        // 5. No backend available
        return Result.failure(
            RendererInitializationException.NoGraphicsSupportException(
                platform = "JS",
                availableBackends = availableBackends,
                requiredFeatures = listOf("WebGPU or WebGL 2.0")
            )
        )
    }

    /**
     * Detect available graphics backends on JS.
     *
     * Returns:
     * - [WEBGPU, WEBGL] on modern browsers
     * - [WEBGL] on older browsers
     * - [] on ancient browsers (very rare)
     *
     * @return List of available BackendType
     */
    actual fun detectAvailableBackends(): List<BackendType> {
        val backends = mutableListOf<BackendType>()

        // Check WebGPU support
        if (isWebGPUAvailable()) {
            backends.add(BackendType.WEBGPU)
        }

        // Check WebGL support
        if (isWebGLAvailable()) {
            backends.add(BackendType.WEBGL)
        }

        return backends
    }

    /**
     * Check if WebGPU is available in this browser.
     *
     * @return true if navigator.gpu is defined
     */
    private fun isWebGPUAvailable(): Boolean {
        return try {
            js("'gpu' in navigator").unsafeCast<Boolean>()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Check if WebGL is available in this browser.
     *
     * @return true if WebGL2 or WebGL context can be created
     */
    private fun isWebGLAvailable(): Boolean {
        return try {
            // Try to create a temporary canvas and get WebGL context
            val testCanvas = js("document.createElement('canvas')") as HTMLCanvasElement
            val gl = testCanvas.getContext("webgl2") ?: testCanvas.getContext("webgl")
            gl != null
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Create WebGPURenderer instance.
     *
     * @param canvas HTMLCanvasElement
     * @param config Renderer configuration
     * @return WebGPURenderer instance
     */
    private fun createWebGPURenderer(canvas: HTMLCanvasElement, config: RendererConfig): Renderer {
        // Use existing WebGPURenderer implementation
        val renderer = WebGPURenderer(canvas)
        return object : Renderer {
            override val backend: BackendType = BackendType.WEBGPU
            override val capabilities: RendererCapabilities = renderer.capabilities
            override var stats: RenderStats = RenderStats(0.0, 0.0, 0, 0)

            override suspend fun initialize(config: RendererConfig): Result<Unit, RendererError> {
                return try {
                    renderer.initialize(WebGPUSurface(canvas))
                    Result.success(Unit)
                } catch (e: Throwable) {
                    Result.failure(
                        RendererInitializationException.DeviceCreationFailedException(
                            BackendType.WEBGPU,
                            "Unknown adapter",
                            e.message ?: "Unknown error"
                        )
                    )
                }
            }

            override fun render(scene: io.kreekt.scene.Scene, camera: io.kreekt.scene.Camera) {
                // Convert Scene/Camera to core.scene.Scene/camera.Camera if needed
                // For now, delegate directly
                val coreScene = scene as? io.kreekt.core.scene.Scene
                    ?: throw IllegalArgumentException("Scene must be io.kreekt.core.scene.Scene")
                val coreCamera = camera as? io.kreekt.camera.Camera
                    ?: throw IllegalArgumentException("Camera must be io.kreekt.camera.Camera")

                renderer.render(coreScene, coreCamera).getOrThrow()
                stats = renderer.getStats()
            }

            override fun resize(width: Int, height: Int) {
                renderer.setSize(width, height)
            }

            override fun dispose() {
                renderer.dispose()
            }

            override fun getStats(): RenderStats = stats
        }
    }

    /**
     * Create WebGLRenderer instance.
     *
     * @param canvas HTMLCanvasElement
     * @param config Renderer configuration
     * @return WebGLRenderer instance (stub for now)
     */
    private fun createWebGLRenderer(canvas: HTMLCanvasElement, config: RendererConfig): Renderer {
        // Use existing WebGLRenderer if available
        // For now, return a stub that throws (WebGL implementation deferred)
        return object : Renderer {
            override val backend: BackendType = BackendType.WEBGL
            override val capabilities: RendererCapabilities = RendererCapabilities(
                backend = BackendType.WEBGL,
                deviceName = "WebGL 2.0",
                driverVersion = "Unknown"
            )
            override var stats: RenderStats = RenderStats(0.0, 0.0, 0, 0)

            override suspend fun initialize(config: RendererConfig): Result<Unit, RendererError> {
                // TODO: Implement WebGL renderer initialization
                return Result.failure(
                    RendererInitializationException.DeviceCreationFailedException(
                        BackendType.WEBGL,
                        "WebGL context",
                        "WebGL renderer not yet implemented in Feature 019"
                    )
                )
            }

            override fun render(scene: io.kreekt.scene.Scene, camera: io.kreekt.scene.Camera) {
                throw UnsupportedOperationException("WebGL renderer not yet implemented")
            }

            override fun resize(width: Int, height: Int) {}
            override fun dispose() {}
            override fun getStats(): RenderStats = stats
        }
    }
}
