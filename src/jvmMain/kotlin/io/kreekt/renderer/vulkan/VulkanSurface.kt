/**
 * T019: VulkanSurface Implementation
 * Feature: 019-we-should-not
 *
 * Vulkan surface wrapper for GLFW windows.
 */

package io.kreekt.renderer.vulkan

import io.kreekt.renderer.RenderSurface
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack

/**
 * Vulkan surface implementation wrapping a GLFW window.
 *
 * Usage:
 * ```kotlin
 * // Create GLFW window
 * val window = glfwCreateWindow(800, 600, "My App", NULL, NULL)
 *
 * // Wrap in VulkanSurface
 * val surface = VulkanSurface(window)
 *
 * // Use with RendererFactory
 * val renderer = RendererFactory.create(surface).getOrThrow()
 * ```
 *
 * @property windowHandle GLFW window handle (Long pointer)
 */
class VulkanSurface(
    private val windowHandle: Long
) : RenderSurface {

    /**
     * Surface width in pixels.
     */
    override val width: Int
        get() = MemoryStack.stackPush().use { stack ->
            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            glfwGetWindowSize(windowHandle, pWidth, pHeight)
            pWidth.get(0)
        }

    /**
     * Surface height in pixels.
     */
    override val height: Int
        get() = MemoryStack.stackPush().use { stack ->
            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            glfwGetWindowSize(windowHandle, pWidth, pHeight)
            pHeight.get(0)
        }

    /**
     * Get GLFW window handle.
     *
     * @return GLFW window handle as Long
     */
    override fun getHandle(): Any = windowHandle

    /**
     * Get framebuffer size (respects DPI scaling).
     *
     * @return Pair of (width, height) in pixels
     */
    fun getFramebufferSize(): Pair<Int, Int> {
        return MemoryStack.stackPush().use { stack ->
            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            glfwGetFramebufferSize(windowHandle, pWidth, pHeight)
            Pair(pWidth.get(0), pHeight.get(0))
        }
    }

    /**
     * Check if window should close.
     *
     * @return true if window close requested
     */
    fun shouldClose(): Boolean {
        return glfwWindowShouldClose(windowHandle)
    }

    /**
     * Poll GLFW events.
     *
     * Should be called once per frame.
     */
    fun pollEvents() {
        glfwPollEvents()
    }
}
