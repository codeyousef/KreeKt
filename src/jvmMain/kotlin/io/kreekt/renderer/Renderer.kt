/**
 * T018: Renderer Actual (JVM)
 * Feature: 019-we-should-not
 *
 * JVM actual declaration for Renderer interface.
 */

package io.kreekt.renderer

import io.kreekt.camera.Camera
import io.kreekt.core.scene.Scene

/**
 * JVM actual for Renderer interface.
 *
 * Implemented by VulkanRenderer (see io.kreekt.renderer.vulkan.VulkanRenderer).
 */
actual interface Renderer {
    actual val backend: BackendType
    actual val capabilities: RendererCapabilities
    actual val stats: RenderStats

    actual suspend fun initialize(config: RendererConfig): io.kreekt.core.Result<Unit>
    actual fun render(scene: Scene, camera: Camera)
    actual fun resize(width: Int, height: Int)
    actual fun dispose()
}
