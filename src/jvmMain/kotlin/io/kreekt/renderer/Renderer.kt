/**
 * T018: Renderer Actual (JVM)
 * Feature: 019-we-should-not
 *
 * JVM actual declaration for Renderer interface.
 */

package io.kreekt.renderer

import io.kreekt.scene.Camera
import io.kreekt.scene.Scene

/**
 * JVM actual for Renderer interface.
 *
 * Implemented by VulkanRenderer (see io.kreekt.renderer.vulkan.VulkanRenderer).
 */
actual interface Renderer {
    actual val backend: BackendType
    actual val capabilities: RendererCapabilities
    actual val stats: RenderStats

    actual suspend fun initialize(config: RendererConfig): Result<Unit, RendererError>
    actual fun render(scene: Scene, camera: Camera)
    actual fun resize(width: Int, height: Int)
    actual fun dispose()
    actual fun getStats(): RenderStats
}
