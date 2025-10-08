/**
 * T030: GameInit - Centralized Renderer Initialization
 * Feature: 019-we-should-not
 *
 * Provides centralized renderer initialization with comprehensive error handling.
 */

package io.kreekt.examples.voxelcraft

import io.kreekt.renderer.*

/**
 * Initialize renderer with comprehensive error handling.
 *
 * Handles all RendererInitializationException subtypes with appropriate error messages.
 *
 * Usage:
 * ```kotlin
 * val surface = WebGPUSurface(canvas) // or VulkanSurface(window)
 * val renderer = try {
 *     initializeRenderer(surface)
 * } catch (e: RendererInitializationException) {
 *     showErrorDialog(e.message)
 *     exitProcess(1)
 * }
 * ```
 *
 * @param surface Platform-specific render surface
 * @param config Optional renderer configuration
 * @return Initialized Renderer instance
 * @throws RendererInitializationException if initialization fails
 */
suspend fun initializeRenderer(
    surface: RenderSurface,
    config: RendererConfig = RendererConfig()
): Renderer {
    return try {
        // Attempt to create renderer via RendererFactory
        when (val result = RendererFactory.create(surface, config)) {
            is io.kreekt.core.Result.Success -> result.value
            is io.kreekt.core.Result.Error -> {
                val exception = result.exception as? RendererInitializationException
                // Handle specific exception types with detailed logging
                when (exception) {
                is RendererInitializationException.NoGraphicsSupportException -> {
                    Logger.error("❌ Graphics Not Supported")
                    Logger.error("   Platform: ${exception.platform}")
                    Logger.error("   Available backends: ${exception.availableBackends}")
                    Logger.error("   Required features: ${exception.requiredFeatures}")
                    Logger.error("")
                    Logger.error("Troubleshooting:")
                    Logger.error("  1. Ensure your GPU supports Vulkan 1.1+ (JVM) or WebGPU/WebGL 2.0 (Browser)")
                    Logger.error("  2. Update your GPU drivers to the latest version")
                    Logger.error("  3. Check if Vulkan/WebGPU is enabled in your system")
                    throw exception
                }

                is RendererInitializationException.AdapterRequestFailedException -> {
                    Logger.error("❌ Failed to Request GPU Adapter")
                    Logger.error("   Backend: ${exception.backend}")
                    Logger.error("   Reason: ${exception.reason}")
                    Logger.error("")
                    Logger.error("Troubleshooting:")
                    Logger.error("  1. Check if GPU drivers are installed")
                    Logger.error("  2. Verify GPU is not in use by another application")
                    Logger.error("  3. Try restarting your system")
                    throw exception
                }

                is RendererInitializationException.DeviceCreationFailedException -> {
                    Logger.error("❌ Failed to Create GPU Device")
                    Logger.error("   Backend: ${exception.backend}")
                    Logger.error("   Adapter: ${exception.adapterInfo}")
                    Logger.error("   Reason: ${exception.reason}")
                    Logger.error("")
                    Logger.error("Troubleshooting:")
                    Logger.error("  1. Update GPU drivers")
                    Logger.error("  2. Check if GPU supports required features")
                    Logger.error("  3. Try closing other GPU-intensive applications")
                    throw exception
                }

                is RendererInitializationException.SurfaceCreationFailedException -> {
                    Logger.error("❌ Failed to Create Render Surface")
                    Logger.error("   Backend: ${exception.backend}")
                    Logger.error("   Surface type: ${exception.surfaceType}")
                    Logger.error("")
                    Logger.error("Troubleshooting:")
                    Logger.error("  1. Check if window/canvas is valid")
                    Logger.error("  2. Verify surface dimensions are within GPU limits")
                    Logger.error("  3. Try resizing the window")
                    throw exception
                }

                is RendererInitializationException.ShaderCompilationException -> {
                    Logger.error("❌ Shader Compilation Failed")
                    Logger.error("   Shader: ${exception.shaderName}")
                    Logger.error("   Errors:")
                    exception.errors.forEach { error ->
                        Logger.error("     - $error")
                    }
                    Logger.error("")
                    Logger.error("This is likely a bug in KreeKt. Please report it with:")
                    Logger.error("  - GPU model and driver version")
                    Logger.error("  - Operating system")
                    Logger.error("  - Full error log")
                    throw exception
                }
                    else -> {
                        // Unknown exception type
                        Logger.error("❌ Renderer initialization failed: ${result.message}")
                        throw exception ?: RuntimeException(result.message)
                    }
                }
            }
        }
    } catch (e: RendererInitializationException) {
        // Re-throw RendererInitializationException
        throw e
    } catch (e: Throwable) {
        // Catch any unexpected errors
        Logger.error("❌ Unexpected error during renderer initialization: ${e.message}")
        throw RendererInitializationException.DeviceCreationFailedException(
            backend = io.kreekt.renderer.BackendType.VULKAN, // Default, may not be accurate
            adapterInfo = "Unknown",
            reason = e.message ?: "Unknown error: ${e::class.simpleName}"
        )
    }
}

/**
 * Detect available graphics backends on current platform.
 *
 * @return List of available BackendType
 */
fun detectAvailableBackends() = RendererFactory.detectAvailableBackends()
