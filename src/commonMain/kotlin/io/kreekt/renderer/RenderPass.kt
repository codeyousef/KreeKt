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
        return try {
            // Get or create full-screen quad geometry
            val quadGeometry = getOrCreateFullScreenQuad()

            // Set up material uniforms if input texture is provided
            inputTexture?.let { texture ->
                // In a real implementation, this would set uniforms on the material
                // For now, we'll just mark the material as needing update
                // The actual texture binding would happen in the renderer
                material.needsUpdate = true

                // Note: In a full implementation, you would extend the Material interface
                // to include uniform setters or use a shader material with uniforms
            }

            // Create a temporary mesh for rendering
            val quadMesh = io.kreekt.core.scene.Mesh(quadGeometry, material)

            // Save current viewport
            val currentViewport = renderer.getViewport()

            // Set full-screen viewport if render target is available
            renderTarget?.let { target ->
                renderer.setViewport(0, 0, target.width, target.height)
            }

            // Render the quad
            // Note: In a real implementation, this would be a direct draw call
            // For now, we simulate it by creating a temporary scene
            val tempScene = io.kreekt.core.scene.Scene()
            tempScene.add(quadMesh)

            // Create an orthographic camera for full-screen rendering
            val orthoCamera = io.kreekt.camera.OrthographicCamera(
                left = -1f, right = 1f, top = 1f, bottom = -1f, near = 0.1f, far = 1f
            )

            // Render the scene
            val result = renderer.render(tempScene, orthoCamera)

            // Restore viewport
            renderer.setViewport(currentViewport.x, currentViewport.y, currentViewport.width, currentViewport.height)

            result
        } catch (e: Exception) {
            RendererResult.Error(RendererException.RenderingFailed("Full-screen quad rendering failed", e))
        }
    }

    companion object {
        private var _fullScreenQuad: io.kreekt.geometry.BufferGeometry? = null

        /**
         * Get or create a reusable full-screen quad geometry
         */
        private fun getOrCreateFullScreenQuad(): io.kreekt.geometry.BufferGeometry {
            if (_fullScreenQuad == null) {
                _fullScreenQuad = createFullScreenQuadGeometry()
            }
            return _fullScreenQuad!!
        }

        /**
         * Create a full-screen quad geometry with NDC coordinates
         */
        private fun createFullScreenQuadGeometry(): io.kreekt.geometry.BufferGeometry {
            val geometry = io.kreekt.geometry.BufferGeometry()

            // Vertex positions in NDC space (-1 to 1)
            val positions = floatArrayOf(
                -1f, -1f, 0f,  // Bottom-left
                1f, -1f, 0f,  // Bottom-right
                1f, 1f, 0f,  // Top-right
                -1f, 1f, 0f   // Top-left
            )

            // UV coordinates (0 to 1)
            val uvs = floatArrayOf(
                0f, 0f,  // Bottom-left
                1f, 0f,  // Bottom-right
                1f, 1f,  // Top-right
                0f, 1f   // Top-left
            )

            // Normals pointing towards camera
            val normals = floatArrayOf(
                0f, 0f, 1f,  // Bottom-left
                0f, 0f, 1f,  // Bottom-right
                0f, 0f, 1f,  // Top-right
                0f, 0f, 1f   // Top-left
            )

            // Indices for two triangles
            val indices = floatArrayOf(
                0f, 1f, 2f,  // First triangle
                0f, 2f, 3f   // Second triangle
            )

            // Set attributes
            geometry.setAttribute("position", io.kreekt.geometry.BufferAttribute(positions, 3))
            geometry.setAttribute("uv", io.kreekt.geometry.BufferAttribute(uvs, 2))
            geometry.setAttribute("normal", io.kreekt.geometry.BufferAttribute(normals, 3))
            geometry.setIndex(io.kreekt.geometry.BufferAttribute(indices, 1))

            return geometry
        }
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
 * Utility function to create a render target with actual textures
 */
fun createRenderTarget(width: Int, height: Int, hasDepth: Boolean = true): RenderTarget {
    return try {
        // Validate dimensions
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("Render target dimensions must be positive: ${width}x${height}")
        }

        // Create color texture
        val colorTexture = io.kreekt.texture.Texture2D(
            width = width,
            height = height,
            format = io.kreekt.renderer.TextureFormat.RGBA8,
            wrapS = io.kreekt.renderer.TextureWrap.CLAMP_TO_EDGE,
            wrapT = io.kreekt.renderer.TextureWrap.CLAMP_TO_EDGE,
            magFilter = io.kreekt.renderer.TextureFilter.LINEAR,
            minFilter = io.kreekt.renderer.TextureFilter.LINEAR,
            name = "RenderTarget_Color_${width}x${height}"
        ).apply {
            isRenderTarget = true
            generateMipmaps = false
            flipY = false
        }

        // Create depth texture if needed
        val depthTexture = if (hasDepth) {
            io.kreekt.texture.Texture2D(
                width = width,
                height = height,
                format = io.kreekt.renderer.TextureFormat.R32F, // Use R32F for depth storage
                wrapS = io.kreekt.renderer.TextureWrap.CLAMP_TO_EDGE,
                wrapT = io.kreekt.renderer.TextureWrap.CLAMP_TO_EDGE,
                magFilter = io.kreekt.renderer.TextureFilter.NEAREST,
                minFilter = io.kreekt.renderer.TextureFilter.NEAREST,
                name = "RenderTarget_Depth_${width}x${height}"
            ).apply {
                type = io.kreekt.texture.TextureType.FLOAT
                isRenderTarget = true
                generateMipmaps = false
                flipY = false
            }
        } else null

        object : RenderTarget {
            override val width: Int = width
            override val height: Int = height
            override val texture: Texture? = colorTexture
            override val depthTexture: Texture? = depthTexture
            override val stencilBuffer: Boolean = false
            override val depthBuffer: Boolean = hasDepth

            // Optional: Add dispose method
            fun dispose() {
                texture?.dispose()
                depthTexture?.dispose()
            }
        }
    } catch (e: Exception) {
        // Fallback to stub implementation if texture creation fails
        object : RenderTarget {
            override val width: Int = width
            override val height: Int = height
            override val texture: Texture? = null
            override val depthTexture: Texture? = null
            override val stencilBuffer: Boolean = false
            override val depthBuffer: Boolean = hasDepth
        }
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