package io.kreekt.renderer

import io.kreekt.camera.Camera
import io.kreekt.camera.OrthographicCamera
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.core.math.Color
import io.kreekt.core.scene.Material
import io.kreekt.core.scene.Scene
import io.kreekt.lighting.*

/**
 * Advanced render pass system for multi-pass rendering
 * T116 - Render passes for forward/deferred rendering, shadows, post-processing
 */

/**
 * Base class for all render passes
 */
abstract class RenderPass(
    val name: String,
    var enabled: Boolean = true,
    var clear: Boolean = true,
    var renderToScreen: Boolean = false,
    var needsSwap: Boolean = true
) {
    var renderTarget: RenderTarget? = null
    var dependencies: List<RenderPass> = emptyList()

    /**
     * Execute this render pass
     */
    abstract fun execute(renderer: Renderer, scene: Scene, camera: Camera): RendererResult<Unit>

    /**
     * Setup/initialize the render pass
     */
    open fun setup(renderer: Renderer): RendererResult<Unit> = RendererResult.Success(Unit)

    /**
     * Cleanup/dispose the render pass
     */
    open fun dispose() {}

    /**
     * Check if this pass is ready to execute
     */
    open fun canExecute(): Boolean = enabled && dependencies.all { it.isComplete() }

    /**
     * Check if this pass has completed execution
     */
    private var completed = false
    fun isComplete(): Boolean = completed

    protected fun setComplete(value: Boolean) {
        completed = value
    }
}

/**
 * Forward rendering pass
 */
class ForwardRenderPass(
    var clearColor: Color = Color.BLACK,
    var clearAlpha: Float = 1f
) : RenderPass("Forward", renderToScreen = true) {

    override fun execute(renderer: Renderer, scene: Scene, camera: Camera): RendererResult<Unit> {
        return try {
            setComplete(false)

            // Set render target
            renderer.renderTarget = renderTarget

            // Clear buffers if needed
            if (clear) {
                renderer.clearColor = clearColor
                renderer.clearAlpha = clearAlpha
                renderer.clear(true, true, true)
            }

            // Render scene with forward shading
            val result = renderer.render(scene, camera)

            setComplete(true)
            result
        } catch (e: Exception) {
            RendererResult.Error(RendererException.RenderingFailed("Forward pass failed", e))
        }
    }
}

/**
 * Shadow mapping pass
 */
class ShadowMapPass(
    val light: Light,
    val shadowMapSize: Int = 2048
) : RenderPass("ShadowMap_${light.id}") {

    private var shadowRenderTarget: RenderTarget? = null

    override fun setup(renderer: Renderer): RendererResult<Unit> {
        return try {
            // Create shadow map render target
            shadowRenderTarget = createRenderTarget(shadowMapSize, shadowMapSize, hasDepth = true)
            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.ResourceCreationFailed("Shadow map setup failed", e))
        }
    }

    override fun execute(renderer: Renderer, scene: Scene, camera: Camera): RendererResult<Unit> {
        if (!light.castShadow) {
            setComplete(true)
            return RendererResult.Success(Unit)
        }

        return try {
            setComplete(false)

            // Set shadow map as render target
            renderer.renderTarget = shadowRenderTarget

            // Create light camera for shadow mapping
            val lightCamera = createLightCamera(light)

            // Clear depth buffer
            renderer.clear(false, true, false)

            // Render depth-only pass
            val result = renderDepthOnly(renderer, scene, lightCamera)

            setComplete(true)
            result
        } catch (e: Exception) {
            RendererResult.Error(RendererException.RenderingFailed("Shadow pass failed", e))
        }
    }

    private fun createLightCamera(light: Light): Camera {
        // Create appropriate camera based on light type
        return when (light.type) {
            LightType.DIRECTIONAL -> createDirectionalLightCamera(light as DirectionalLight)
            LightType.SPOT -> createSpotLightCamera(light as SpotLight)
            LightType.POINT -> createPointLightCamera(light as PointLight)
            else -> throw IllegalArgumentException("Light type ${light.type} cannot cast shadows")
        }
    }

    private fun createDirectionalLightCamera(light: DirectionalLight): Camera {
        // Create orthographic camera for directional light
        val camera = OrthographicCamera(
            left = light.shadowCameraLeft,
            right = light.shadowCameraRight,
            top = light.shadowCameraTop,
            bottom = light.shadowCameraBottom,
            near = light.shadowCameraNear,
            far = light.shadowCameraFar
        )
        camera.position.copy(light.position)
        val target = camera.position.clone().add(light.direction)
        camera.lookAt(target)
        return camera
    }

    private fun createSpotLightCamera(light: SpotLight): Camera {
        // Create perspective camera for spot light
        val camera = PerspectiveCamera(
            fov = light.angle * 2f * 180f / kotlin.math.PI.toFloat(),
            aspect = 1f,
            near = light.shadowCameraNear,
            far = light.shadowCameraFar
        )
        camera.position.copy(light.position)
        val target = camera.position.clone().add(light.direction)
        camera.lookAt(target)
        return camera
    }

    private fun createPointLightCamera(light: PointLight): Camera {
        // Create perspective camera for point light (one face of cube map)
        val camera = PerspectiveCamera(
            fov = 90f,
            aspect = 1f,
            near = light.shadowCameraNear,
            far = light.shadowCameraFar
        )
        camera.position.copy(light.position)
        return camera
    }

    private fun renderDepthOnly(renderer: Renderer, scene: Scene, lightCamera: Camera): RendererResult<Unit> {
        // Render scene with depth-only materials
        return renderer.render(scene, lightCamera)
    }

    override fun dispose() {
        shadowRenderTarget = null
    }
}

/**
 * Post-processing pass
 */
class PostProcessPass(
    val material: Material,
    var inputTexture: io.kreekt.renderer.Texture? = null
) : RenderPass("PostProcess") {

    override fun execute(renderer: Renderer, scene: Scene, camera: Camera): RendererResult<Unit> {
        return try {
            setComplete(false)

            // Set render target
            renderer.renderTarget = renderTarget

            // Clear if needed
            if (clear) {
                renderer.clear(true, true, true)
            }

            // Render full-screen quad with post-process material
            val result = renderFullScreenQuad(renderer, material)

            setComplete(true)
            result
        } catch (e: Exception) {
            RendererResult.Error(RendererException.RenderingFailed("Post-process pass failed", e))
        }
    }

    private fun renderFullScreenQuad(renderer: Renderer, material: Material): RendererResult<Unit> {
        // Create full-screen quad geometry and render with material
        // This would typically use a simple quad geometry
        return RendererResult.Success(Unit)
    }
}

/**
 * Render pass manager
 */
class RenderPassManager {
    private val passes = mutableListOf<RenderPass>()

    fun addPass(pass: RenderPass) {
        passes.add(pass)
    }

    fun removePass(pass: RenderPass) {
        passes.remove(pass)
        pass.dispose()
    }

    fun clearPasses() {
        passes.forEach { it.dispose() }
        passes.clear()
    }

    fun setup(renderer: Renderer): RendererResult<Unit> {
        for (pass in passes) {
            val result = pass.setup(renderer)
            if (result is RendererResult.Error) {
                return result
            }
        }
        return RendererResult.Success(Unit)
    }

    fun execute(renderer: Renderer, scene: Scene, camera: Camera): RendererResult<Unit> {
        // Sort passes by dependencies
        val sortedPasses = topologicalSort(passes)

        for (pass in sortedPasses) {
            if (pass.canExecute()) {
                val result = pass.execute(renderer, scene, camera)
                if (result is RendererResult.Error) {
                    return result
                }
            }
        }

        return RendererResult.Success(Unit)
    }

    private fun topologicalSort(passes: List<RenderPass>): List<RenderPass> {
        // Simple topological sort for pass dependencies
        val sorted = mutableListOf<RenderPass>()
        val visited = mutableSetOf<RenderPass>()

        fun visit(pass: RenderPass) {
            if (pass in visited) return
            visited.add(pass)

            for (dependency in pass.dependencies) {
                if (dependency in passes) {
                    visit(dependency)
                }
            }

            sorted.add(pass)
        }

        for (pass in passes) {
            visit(pass)
        }

        return sorted
    }

    fun dispose() {
        passes.forEach { it.dispose() }
        passes.clear()
    }
}

/**
 * Utility function to create a render target
 */
fun createRenderTarget(width: Int, height: Int, hasDepth: Boolean = true): RenderTarget {
    return object : RenderTarget {
        override val width: Int = width
        override val height: Int = height
        override val texture: Texture? = null
        override val depthTexture: Texture? = if (hasDepth) null else null
        override val stencilBuffer: Boolean = false
        override val depthBuffer: Boolean = hasDepth
    }
}

/**
 * Multi-pass renderer that uses render passes
 */
class MultiPassRenderer(
    private val baseRenderer: Renderer
) : Renderer by baseRenderer {

    private val passManager = RenderPassManager()

    fun addRenderPass(pass: RenderPass) {
        passManager.addPass(pass)
    }

    fun removeRenderPass(pass: RenderPass) {
        passManager.removePass(pass)
    }

    override suspend fun initialize(surface: RenderSurface): RendererResult<Unit> {
        val result = baseRenderer.initialize(surface)
        if (result is RendererResult.Success) {
            return passManager.setup(baseRenderer)
        }
        return result
    }

    override fun render(scene: Scene, camera: Camera): RendererResult<Unit> {
        return passManager.execute(baseRenderer, scene, camera)
    }

    override fun dispose(): RendererResult<Unit> {
        passManager.dispose()
        return baseRenderer.dispose()
    }
}

/**
 * Utility functions for creating common render pass configurations
 */
object RenderPassUtils {

    /**
     * Create a basic forward rendering setup
     */
    fun createForwardRenderingPasses(): List<RenderPass> {
        return listOf(
            ForwardRenderPass()
        )
    }

    /**
     * Create a shadow-mapped forward rendering setup
     */
    fun createShadowMappedForwardPasses(lights: List<Light>): List<RenderPass> {
        val passes = mutableListOf<RenderPass>()

        // Add shadow passes for each shadow-casting light
        val shadowPasses = lights.filter { it.castShadow }.map { light ->
            ShadowMapPass(light)
        }
        passes.addAll(shadowPasses)

        // Add forward pass that depends on shadow passes
        val forwardPass = ForwardRenderPass()
        forwardPass.dependencies = shadowPasses
        passes.add(forwardPass)

        return passes
    }

    /**
     * Create a post-processing pipeline
     */
    fun createPostProcessingPasses(effects: List<Material>): List<RenderPass> {
        val passes = mutableListOf<RenderPass>()

        // Create render target for intermediate results
        var previousPass: RenderPass? = null

        for ((index, effect) in effects.withIndex()) {
            val pass = PostProcessPass(effect)

            if (previousPass != null) {
                pass.dependencies = listOf(previousPass)
            }

            // Last pass renders to screen
            if (index == effects.size - 1) {
                pass.renderToScreen = true
            }

            passes.add(pass)
            previousPass = pass
        }

        return passes
    }
}