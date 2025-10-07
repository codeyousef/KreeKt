/**
 * T019: RenderSurface Actual (JVM)
 * Feature: 019-we-should-not
 *
 * JVM actual declaration for RenderSurface interface.
 */

package io.kreekt.renderer

/**
 * JVM actual for RenderSurface interface.
 *
 * Implemented by VulkanSurface (see io.kreekt.renderer.vulkan.VulkanSurface).
 */
actual interface RenderSurface {
    actual val width: Int
    actual val height: Int
    actual fun getHandle(): Any
}

/*
 * NOTE: Previous VulkanRenderSurface class replaced with Feature 019 expect/actual pattern.
 * See git history for old implementation.
 * New implementation: io.kreekt.renderer.vulkan.VulkanSurface
 */
