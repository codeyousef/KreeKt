import io.kreekt.renderer.Renderer
import io.kreekt.renderer.RendererResult
import kotlin.js.Date

actual fun createRenderer(): RendererResult<Renderer> {
    return RendererResult.Error(
        io.kreekt.renderer.RendererException.UnsupportedFeature("WebGL renderer not yet implemented")
    )
}

actual class InputState {
    actual fun isKeyPressed(key: String): Boolean = false
    actual val isMousePressed: Boolean = false
    actual val mouseDeltaX: Float = 0f
    actual val mouseDeltaY: Float = 0f
}

actual fun getCurrentTimeMillis(): Long = Date.now().toLong()
actual fun getCurrentInput(): InputState = InputState()