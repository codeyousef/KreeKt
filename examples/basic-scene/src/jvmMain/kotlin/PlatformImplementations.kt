import io.kreekt.camera.Camera
import io.kreekt.camera.Viewport
import io.kreekt.core.math.Color
import io.kreekt.core.scene.Scene
import io.kreekt.renderer.*

// Minimal actual implementations for compilation
actual fun createRenderer(): io.kreekt.renderer.RendererResult<Renderer> {
    return io.kreekt.renderer.RendererResult.Success(MockRenderer())
}

actual class InputState {
    actual fun isKeyPressed(key: String): Boolean = false
    actual val isMousePressed: Boolean = false
    actual val mouseDeltaX: Float = 0f
    actual val mouseDeltaY: Float = 0f
}

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun getCurrentInput(): InputState = InputState()

// Mock renderer for compilation
private class MockRenderer : Renderer {
    override suspend fun initialize(surface: RenderSurface): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun setSize(width: Int, height: Int, updateStyle: Boolean): RendererResult<Unit> =
        RendererResult.Success(Unit)

    override fun render(scene: Scene, camera: Camera): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun dispose(): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun setPixelRatio(pixelRatio: Float): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun setViewport(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit> =
        RendererResult.Success(Unit)

    override fun getViewport(): Viewport = Viewport(0, 0, 1920, 1080)
    override fun setScissorTest(enable: Boolean): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun setScissor(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit> =
        RendererResult.Success(Unit)

    override fun clear(color: Boolean, depth: Boolean, stencil: Boolean): RendererResult<Unit> =
        RendererResult.Success(Unit)

    override fun clearColorBuffer(): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun clearDepth(): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun clearStencil(): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun resetState(): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun compile(scene: Scene, camera: Camera): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun forceContextLoss(): RendererResult<Unit> = RendererResult.Success(Unit)
    override fun isContextLost(): Boolean = false
    override fun getStats(): RenderStats = RenderStats(0, 0, 0)
    override fun resetStats() {}
    override val capabilities: RendererCapabilities = RendererCapabilities()
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
}