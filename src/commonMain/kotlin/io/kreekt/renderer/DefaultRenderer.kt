package io.kreekt.renderer

import io.kreekt.core.math.*
import io.kreekt.core.scene.*
import io.kreekt.camera.Camera
import io.kreekt.camera.Viewport

/**
 * Default renderer implementation for cross-platform compatibility
 * Provides a software-like fallback renderer that satisfies the interface
 */
class DefaultRenderer(
    private val config: RendererConfig = RendererConfig()
) : Renderer {

    override val capabilities: RendererCapabilities = CapabilitiesUtils.getCompatibilityCapabilities().copy(
        vendor = "KreeKt",
        renderer = "Default Software Renderer",
        version = "1.0",
        shadingLanguageVersion = "WGSL 1.0"
    )

    override var renderTarget: RenderTarget? = null
    override var autoClear: Boolean = true
    override var autoClearColor: Boolean = true
    override var autoClearDepth: Boolean = true
    override var autoClearStencil: Boolean = true
    override var clearColor: Color = Color.BLACK
    override var clearAlpha: Float = 1f
    override var shadowMap: ShadowMapSettings = ShadowMapSettings()
    override var toneMapping: ToneMapping = ToneMapping.NONE
    override var toneMappingExposure: Float = 1f
    override var outputColorSpace: ColorSpace = ColorSpace.sRGB
    override var physicallyCorrectLights: Boolean = false

    private var surface: RenderSurface? = null
    private var isInitialized = false
    private var isDisposed = false
    private var contextLost = false

    private var currentViewport = Viewport(0, 0, 800, 600)
    private var scissorTest = false
    private var scissorArea = Viewport(0, 0, 0, 0)
    private var pixelRatio = 1f

    private val stats = RenderStatsTracker()

    override suspend fun initialize(surface: RenderSurface): RendererResult<Unit> {
        if (isDisposed) return RendererResult.Error(RendererException.InvalidState("Renderer is disposed"))

        try {
            this.surface = surface
            currentViewport = Viewport(0, 0, surface.width, surface.height)
            isInitialized = true
            contextLost = false

            return RendererResult.Success(Unit)
        } catch (e: Exception) {
            return RendererResult.Error(RendererException.InitializationFailed("Failed to initialize renderer", e))
        }
    }

    override fun render(scene: Scene, camera: Camera): RendererResult<Unit> {
        if (!isInitialized) return RendererResult.Error(RendererException.InvalidState("Renderer not initialized"))
        if (isDisposed) return RendererResult.Error(RendererException.InvalidState("Renderer is disposed"))
        if (contextLost) return RendererResult.Error(RendererException.ContextLost())

        try {
            stats.frameStart()

            // Auto-clear if enabled
            if (autoClear) {
                clear(autoClearColor, autoClearDepth, autoClearStencil)
            }

            // Basic software rendering simulation
            // In a real implementation, this would:
            // 1. Update camera matrices
            // 2. Frustum culling
            // 3. Depth sorting
            // 4. Shader compilation/binding
            // 5. Draw calls for each object
            // 6. Post-processing

            // Simulate rendering work
            renderSceneObjects(scene, camera)

            stats.frameEnd()

            // Present the frame
            surface?.present()

            return RendererResult.Success(Unit)
        } catch (e: Exception) {
            return RendererResult.Error(RendererException.RenderingFailed("Rendering failed", e))
        }
    }

    override fun setSize(width: Int, height: Int, updateStyle: Boolean): RendererResult<Unit> {
        if (width <= 0 || height <= 0) {
            return RendererResult.Error(RendererException.InvalidState("Invalid dimensions: ${width}x${height}"))
        }

        try {
            surface?.resize(width, height)
            currentViewport = Viewport(0, 0, width, height)
            return RendererResult.Success(Unit)
        } catch (e: Exception) {
            return RendererResult.Error(RendererException.RenderingFailed("Failed to resize", e))
        }
    }

    override fun setPixelRatio(pixelRatio: Float): RendererResult<Unit> {
        if (pixelRatio <= 0f) {
            return RendererResult.Error(RendererException.InvalidState("Invalid pixel ratio: $pixelRatio"))
        }

        this.pixelRatio = pixelRatio
        return RendererResult.Success(Unit)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit> {
        if (width <= 0 || height <= 0) {
            return RendererResult.Error(RendererException.InvalidState("Invalid viewport dimensions"))
        }

        currentViewport = Viewport(x, y, width, height)
        return RendererResult.Success(Unit)
    }

    override fun getViewport(): Viewport = currentViewport

    override fun setScissorTest(enable: Boolean): RendererResult<Unit> {
        scissorTest = enable
        return RendererResult.Success(Unit)
    }

    override fun setScissor(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit> {
        scissorArea = Viewport(x, y, width, height)
        return RendererResult.Success(Unit)
    }

    override fun clear(color: Boolean, depth: Boolean, stencil: Boolean): RendererResult<Unit> {
        try {
            // Simulate clearing buffers
            if (color) clearColorBuffer()
            if (depth) clearDepth()
            if (stencil) clearStencil()

            return RendererResult.Success(Unit)
        } catch (e: Exception) {
            return RendererResult.Error(RendererException.RenderingFailed("Clear failed", e))
        }
    }

    override fun clearColorBuffer(): RendererResult<Unit> {
        // Simulate color buffer clear
        return RendererResult.Success(Unit)
    }

    override fun clearDepth(): RendererResult<Unit> {
        // Simulate depth buffer clear
        return RendererResult.Success(Unit)
    }

    override fun clearStencil(): RendererResult<Unit> {
        // Simulate stencil buffer clear
        return RendererResult.Success(Unit)
    }

    override fun resetState(): RendererResult<Unit> {
        try {
            // Reset renderer state to defaults
            scissorTest = false
            renderTarget = null
            return RendererResult.Success(Unit)
        } catch (e: Exception) {
            return RendererResult.Error(RendererException.RenderingFailed("State reset failed", e))
        }
    }

    override fun compile(scene: Scene, camera: Camera): RendererResult<Unit> {
        try {
            // Simulate shader compilation for all materials in scene
            compileSceneMaterials(scene)
            return RendererResult.Success(Unit)
        } catch (e: Exception) {
            return RendererResult.Error(RendererException.ResourceCreationFailed("Compilation failed", e))
        }
    }

    override fun dispose(): RendererResult<Unit> {
        if (isDisposed) return RendererResult.Success(Unit)

        try {
            surface?.dispose()
            surface = null
            isInitialized = false
            isDisposed = true
            return RendererResult.Success(Unit)
        } catch (e: Exception) {
            return RendererResult.Error(RendererException.RenderingFailed("Disposal failed", e))
        }
    }

    override fun forceContextLoss(): RendererResult<Unit> {
        contextLost = true
        return RendererResult.Success(Unit)
    }

    override fun isContextLost(): Boolean = contextLost

    override fun getStats(): RenderStats = stats.getStats()

    override fun resetStats() = stats.reset()

    // Private helper methods
    private fun renderSceneObjects(scene: Scene, camera: Camera) {
        // Simulate rendering each object in the scene
        var triangleCount = 0
        var drawCalls = 0

        // Traverse scene graph and "render" objects
        scene.traverse { obj ->
            if (obj is Mesh) {
                drawCalls++
                // Simulate triangle counting
                triangleCount += estimateTriangleCount(obj)
            }
        }

        stats.addDrawCall(drawCalls)
        stats.addTriangles(triangleCount)
    }

    private fun estimateTriangleCount(mesh: Mesh): Int {
        // Simple estimation - in real implementation would get from geometry
        return 100 // Default triangle count estimate
    }

    private fun compileSceneMaterials(scene: Scene) {
        // Simulate material compilation
        scene.traverse { obj ->
            if (obj is Mesh) {
                // Simulate shader compilation for this material
                stats.addShader()
            }
        }
    }
}

/**
 * Default render target implementation
 */
class DefaultRenderTarget(
    override val width: Int,
    override val height: Int,
    override val texture: io.kreekt.renderer.Texture? = null,
    override val depthTexture: io.kreekt.renderer.Texture? = null,
    override val stencilBuffer: Boolean = false,
    override val depthBuffer: Boolean = true
) : RenderTarget

/**
 * Default renderer factory implementation
 */
class DefaultRendererFactory : RendererFactory {

    override suspend fun createRenderer(config: RendererConfig): RendererResult<Renderer> {
        return try {
            val renderer = DefaultRenderer(config)
            RendererResult.Success(renderer)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.InitializationFailed("Failed to create renderer", e))
        }
    }

    override fun isSupported(): Boolean = true

    override suspend fun getCapabilities(): RendererCapabilities {
        return CapabilitiesUtils.getCompatibilityCapabilities()
    }
}

/**
 * Statistics tracking for renderer performance
 */
private class RenderStatsTracker {
    private var frameCount = 0
    private var drawCalls = 0
    private var triangles = 0
    private var points = 0
    private var lines = 0
    private var geometries = 0
    private var textures = 0
    private var shaders = 0
    private var programs = 0

    fun frameStart() {
        // Reset per-frame counters
        drawCalls = 0
        triangles = 0
        points = 0
        lines = 0
    }

    fun frameEnd() {
        frameCount++
    }

    fun addDrawCall(count: Int = 1) {
        drawCalls += count
    }

    fun addTriangles(count: Int) {
        triangles += count
    }

    fun addShader() {
        shaders++
    }

    fun getStats(): RenderStats {
        return RenderStats(
            frame = frameCount,
            calls = drawCalls,
            triangles = triangles,
            points = points,
            lines = lines,
            geometries = geometries,
            textures = textures,
            shaders = shaders,
            programs = programs,
            memory = MemoryStats(
                geometries = geometries * 1024L, // Estimated memory per geometry
                textures = textures * 4096L,    // Estimated memory per texture
                programs = programs * 512L      // Estimated memory per program
            )
        )
    }

    fun reset() {
        frameCount = 0
        drawCalls = 0
        triangles = 0
        points = 0
        lines = 0
        geometries = 0
        textures = 0
        shaders = 0
        programs = 0
    }
}

/**
 * Global renderer creation function for easy access
 */
suspend fun createRenderer(config: RendererConfig = RendererConfig()): RendererResult<Renderer> {
    return DefaultRendererFactory().createRenderer(config)
}

/**
 * Create a simple render target
 */
fun createRenderTarget(
    width: Int,
    height: Int,
    hasDepth: Boolean = true,
    hasStencil: Boolean = false
): RenderTarget {
    return DefaultRenderTarget(
        width = width,
        height = height,
        depthBuffer = hasDepth,
        stencilBuffer = hasStencil
    )
}