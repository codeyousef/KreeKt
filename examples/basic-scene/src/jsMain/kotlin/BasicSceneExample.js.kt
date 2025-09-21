/**
 * Web/JavaScript implementation of the Basic Scene Example
 * Uses WebGPU/WebGL for rendering in the browser
 */

import io.kreekt.renderer.Renderer
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import org.w3c.dom.*
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent

actual fun createRenderer(): Renderer {
    return WebGPURenderer() // With WebGL fallback
}

actual class InputState {
    private val pressedKeys = mutableSetOf<String>()
    private var mousePressed = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var deltaX = 0.0f
    private var deltaY = 0.0f

    init {
        setupEventListeners()
    }

    private fun setupEventListeners() {
        // Keyboard events
        document.addEventListener("keydown", { event ->
            val e = event as KeyboardEvent
            pressedKeys.add(e.code)
        })

        document.addEventListener("keyup", { event ->
            val e = event as KeyboardEvent
            pressedKeys.remove(e.code)
        })

        // Mouse events
        document.addEventListener("mousedown", { event ->
            val e = event as MouseEvent
            if (e.button.toInt() == 0) { // Left mouse button
                mousePressed = true
                lastMouseX = e.clientX.toDouble()
                lastMouseY = e.clientY.toDouble()
            }
        })

        document.addEventListener("mouseup", { event ->
            val e = event as MouseEvent
            if (e.button.toInt() == 0) {
                mousePressed = false
            }
        })

        document.addEventListener("mousemove", { event ->
            val e = event as MouseEvent
            if (mousePressed) {
                deltaX = (e.clientX - lastMouseX).toFloat()
                deltaY = (e.clientY - lastMouseY).toFloat()
                lastMouseX = e.clientX.toDouble()
                lastMouseY = e.clientY.toDouble()
            } else {
                deltaX = 0.0f
                deltaY = 0.0f
            }
        })

        // Prevent context menu
        document.addEventListener("contextmenu", { event ->
            event.preventDefault()
        })
    }

    actual fun isKeyPressed(key: String): Boolean {
        return when (key.uppercase()) {
            "W" -> pressedKeys.contains("KeyW")
            "S" -> pressedKeys.contains("KeyS")
            "A" -> pressedKeys.contains("KeyA")
            "D" -> pressedKeys.contains("KeyD")
            "Q" -> pressedKeys.contains("KeyQ")
            "E" -> pressedKeys.contains("KeyE")
            "ESCAPE" -> pressedKeys.contains("Escape")
            else -> false
        }
    }

    actual val isMousePressed: Boolean
        get() = mousePressed

    actual val mouseDeltaX: Float
        get() = deltaX

    actual val mouseDeltaY: Float
        get() = deltaY
}

actual fun getCurrentTimeMillis(): Long = js("Date.now()") as Long

private var currentInput: InputState? = null

actual fun getCurrentInput(): InputState {
    return currentInput ?: throw IllegalStateException("Input not initialized")
}

/**
 * Web main function
 */
fun main() {
    // Setup the page
    setupPage()

    // Initialize input
    currentInput = InputState()

    // Create and run the example
    GlobalScope.launch {
        try {
            val example = BasicSceneExample()
            example.initialize()
            example.printSceneInfo()

            console.log("🎮 Web Controls:")
            console.log("WASD - Move camera")
            console.log("QE - Move up/down")
            console.log("Mouse - Look around (click and drag)")
            console.log("\n🎬 Starting render loop...")

            var lastTime = window.performance.now()

            // Main render loop using requestAnimationFrame
            fun renderLoop() {
                val currentTime = window.performance.now()
                val deltaTime = ((currentTime - lastTime) / 1000.0).toFloat()
                lastTime = currentTime

                // Handle input
                example.handleInput(currentInput!!)

                // Render frame
                example.render(deltaTime)

                // Continue loop
                window.requestAnimationFrame { renderLoop() }
            }

            // Start the render loop
            renderLoop()

        } catch (e: Exception) {
            console.error("❌ Error running example: ${e.message}")
            console.error(e)
        }
    }
}

private fun setupPage() {
    document.title = "KreeKt Basic Scene Example"

    // Create main container
    val body = document.body!!
    body.innerHTML = ""
    body.style.margin = "0"
    body.style.padding = "0"
    body.style.overflow = "hidden"
    body.style.fontFamily = "Arial, sans-serif"
    body.style.backgroundColor = "#000"

    // Create canvas
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.id = "kreekt-canvas"
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
    canvas.style.display = "block"
    body.appendChild(canvas)

    // Create info overlay
    val infoDiv = document.createElement("div") as HTMLDivElement
    infoDiv.innerHTML = """
        <div style="position: absolute; top: 10px; left: 10px; color: white; background: rgba(0,0,0,0.7); padding: 10px; border-radius: 5px; font-size: 14px;">
            <h3 style="margin: 0 0 10px 0;">🚀 KreeKt 3D Engine Demo</h3>
            <div><strong>Controls:</strong></div>
            <div>WASD - Move camera</div>
            <div>QE - Move up/down</div>
            <div>Mouse - Look around (click and drag)</div>
            <div style="margin-top: 10px; font-size: 12px; opacity: 0.8;">
                <div id="fps">FPS: --</div>
                <div id="objects">Objects: --</div>
            </div>
        </div>
    """.trimIndent()
    body.appendChild(infoDiv)

    // Handle window resize
    window.addEventListener("resize", {
        val newCanvas = document.getElementById("kreekt-canvas") as HTMLCanvasElement
        newCanvas.width = window.innerWidth
        newCanvas.height = window.innerHeight
    })

    // Add loading indicator
    val loadingDiv = document.createElement("div") as HTMLDivElement
    loadingDiv.id = "loading"
    loadingDiv.innerHTML = """
        <div style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: white; text-align: center;">
            <div style="font-size: 24px; margin-bottom: 10px;">🔄 Loading KreeKt...</div>
            <div style="font-size: 14px; opacity: 0.8;">Initializing 3D engine and scene</div>
        </div>
    """.trimIndent()
    body.appendChild(loadingDiv)

    // Remove loading indicator after a delay
    window.setTimeout({
        document.getElementById("loading")?.remove()
    }, 2000)
}

// Mock renderer for now - replace with actual WebGPU implementation
private class WebGPURenderer : Renderer {
    private val canvas: HTMLCanvasElement = document.getElementById("kreekt-canvas") as HTMLCanvasElement
    private var frameCount = 0
    private var lastFPSTime = window.performance.now()

    override fun setSize(width: Int, height: Int) {
        canvas.width = width
        canvas.height = height
    }

    override fun setClearColor(color: io.kreekt.core.math.Color) {
        // WebGPU clear color setup
    }

    override fun render(scene: io.kreekt.core.scene.Scene, camera: io.kreekt.camera.Camera) {
        // Basic WebGL/WebGPU rendering - replace with actual implementation
        val context = canvas.getContext("webgl2") ?: canvas.getContext("webgl")
        if (context != null) {
            val gl = context.asDynamic()

            // Clear screen
            gl.clearColor(0.05, 0.05, 0.1, 1.0)
            gl.clear(gl.COLOR_BUFFER_BIT or gl.DEPTH_BUFFER_BIT)
            gl.enable(gl.DEPTH_TEST)

            // Render scene objects (placeholder)
            scene.traverse { obj ->
                renderObject(obj, camera, gl)
            }
        }

        // Update FPS counter
        updateFPS()
    }

    private fun renderObject(obj: io.kreekt.core.scene.Object3D, camera: io.kreekt.camera.Camera, gl: dynamic) {
        // Placeholder object rendering
        // In real implementation, this would use WebGPU compute/render pipelines
    }

    private fun updateFPS() {
        frameCount++
        val currentTime = window.performance.now()

        if (currentTime - lastFPSTime >= 1000) {
            val fps = (frameCount * 1000 / (currentTime - lastFPSTime)).toInt()
            document.getElementById("fps")?.textContent = "FPS: $fps"
            frameCount = 0
            lastFPSTime = currentTime
        }
    }

    override fun getRenderInfo(): RenderInfo {
        return RenderInfo(0, 0, 0, 0)
    }

    override fun dispose() {
        // Cleanup WebGPU/WebGL resources
    }
}

data class RenderInfo(
    val triangles: Int,
    val drawCalls: Int,
    val textureMemory: Long,
    val bufferMemory: Long
)