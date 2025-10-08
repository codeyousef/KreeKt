package io.kreekt.renderer.webgl

import io.kreekt.camera.Camera
import io.kreekt.core.scene.Scene
import io.kreekt.renderer.*
import org.w3c.dom.HTMLCanvasElement

/**
 * WebGL Renderer stub - WebGL support deferred to Phase 3
 *
 * For Phase 2 Feature 020, focus is on WebGPU (primary) and Vulkan (JVM).
 * WebGL will be implemented as a fallback in Phase 3.
 */
class WebGLRenderer(
    private val canvas: HTMLCanvasElement
) : Renderer {

    override val backend: BackendType = BackendType.WEBGL

    override val capabilities: RendererCapabilities = RendererCapabilities(
        backend = BackendType.WEBGL,
        maxTextureSize = 0,
        maxVertexAttributes = 0,
        maxVertexUniforms = 0,
        maxFragmentUniforms = 0,
        maxVertexTextures = 0,
        maxFragmentTextures = 0,
        maxCombinedTextures = 0,
        maxColorAttachments = 0,
        maxSamples = 0,
        maxAnisotropy = 0f
    )

    override val stats: RenderStats = RenderStats(
        fps = 0.0,
        frameTime = 0.0,
        triangles = 0,
        drawCalls = 0,
        textureMemory = 0,
        bufferMemory = 0
    )

    override suspend fun initialize(config: RendererConfig): io.kreekt.core.Result<Unit> {
        return io.kreekt.core.Result.Error(
            "WebGL renderer not yet implemented - use WebGPU backend",
            NotImplementedError("WebGL support deferred to Phase 3")
        )
    }

    override fun render(scene: Scene, camera: Camera) {
        throw NotImplementedError("WebGL renderer not yet implemented - use WebGPU backend")
    }

    override fun resize(width: Int, height: Int) {
        throw NotImplementedError("WebGL renderer not yet implemented - use WebGPU backend")
    }

    override fun dispose() {
        // No-op for stub
    }
}
