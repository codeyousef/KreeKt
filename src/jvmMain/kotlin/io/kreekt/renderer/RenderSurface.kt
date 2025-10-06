package io.kreekt.renderer

/**
 * Vulkan render surface for desktop platforms (GLFW window)
 */
class VulkanRenderSurface(
    val windowHandle: Long,
    private var surfaceWidth: Int,
    private var surfaceHeight: Int
) : RenderSurface {
    override val width: Int
        get() = surfaceWidth

    override val height: Int
        get() = surfaceHeight

    override val devicePixelRatio: Float
        get() = 1.0f // Can be queried from GLFW if needed

    override val isValid: Boolean
        get() = windowHandle != 0L

    override fun resize(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
    }

    override fun present() {
        // Vulkan present is handled by swapchain
    }

    override fun dispose() {
        // Cleanup Vulkan surface resources
    }
}
