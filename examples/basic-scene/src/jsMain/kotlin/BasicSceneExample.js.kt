/**
 * Web/JavaScript implementation of the Basic Scene Example
 * Uses WebGPU/WebGL for rendering in the browser
 */

import io.kreekt.renderer.Renderer
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent

actual fun createRenderer(): io.kreekt.renderer.RendererResult<Renderer> {
    return try {
        io.kreekt.renderer.RendererResult.Success(WebGPURenderer()) // With WebGL fallback
    } catch (e: Exception) {
        io.kreekt.renderer.RendererResult.Error(
            io.kreekt.renderer.RendererException.InitializationFailed(
                "Failed to create WebGPU renderer",
                e
            )
        )
    }
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
    body.style.asDynamic().overflow = "hidden"
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
    private var viewport = io.kreekt.camera.Viewport(0, 0, canvas.width, canvas.height)

    // Renderer interface properties
    override val capabilities: io.kreekt.renderer.RendererCapabilities = io.kreekt.renderer.RendererCapabilities(
        maxTextureSize = 4096,
        vendor = "WebGPU",
        renderer = "Browser WebGPU",
        version = "WebGPU 1.0"
    )

    override var renderTarget: io.kreekt.renderer.RenderTarget? = null
    override var autoClear: Boolean = true
    override var autoClearColor: Boolean = true
    override var autoClearDepth: Boolean = true
    override var autoClearStencil: Boolean = false
    override var clearColor: io.kreekt.core.math.Color = io.kreekt.core.math.Color(0.05f, 0.05f, 0.1f)
    override var clearAlpha: Float = 1.0f
    override var shadowMap: io.kreekt.renderer.ShadowMapSettings = io.kreekt.renderer.ShadowMapSettings()
    override var toneMapping: io.kreekt.renderer.ToneMapping = io.kreekt.renderer.ToneMapping.NONE
    override var toneMappingExposure: Float = 1.0f
    override var outputColorSpace: io.kreekt.renderer.ColorSpace = io.kreekt.renderer.ColorSpace.sRGB
    override var physicallyCorrectLights: Boolean = false

    override suspend fun initialize(surface: io.kreekt.renderer.RenderSurface): io.kreekt.renderer.RendererResult<Unit> {
        return try {
            // WebGPU initialization would go here
            io.kreekt.renderer.RendererResult.Success(Unit)
        } catch (e: Exception) {
            io.kreekt.renderer.RendererResult.Error(
                io.kreekt.renderer.RendererException.InitializationFailed(
                    "WebGPU init failed",
                    e
                )
            )
        }
    }

    override fun render(
        scene: io.kreekt.core.scene.Scene,
        camera: io.kreekt.camera.Camera
    ): io.kreekt.renderer.RendererResult<Unit> {
        return try {
            // Basic WebGL/WebGPU rendering - replace with actual implementation
            val context = canvas.getContext("webgl2") ?: canvas.getContext("webgl")
            if (context != null) {
                val gl = context.asDynamic()

                // Clear screen
                gl.clearColor(clearColor.r, clearColor.g, clearColor.b, clearAlpha)
                gl.clear(gl.COLOR_BUFFER_BIT or gl.DEPTH_BUFFER_BIT)
                gl.enable(gl.DEPTH_TEST)

                // Render scene objects (placeholder)
                scene.traverse { obj ->
                    renderObject(obj, camera, gl)
                }
            }

            // Update FPS counter
            updateFPS()
            io.kreekt.renderer.RendererResult.Success(Unit)
        } catch (e: Exception) {
            io.kreekt.renderer.RendererResult.Error(
                io.kreekt.renderer.RendererException.RenderingFailed(
                    "Render failed",
                    e
                )
            )
        }
    }

    override fun setSize(width: Int, height: Int, updateStyle: Boolean): io.kreekt.renderer.RendererResult<Unit> {
        return try {
            canvas.width = width
            canvas.height = height
            viewport = io.kreekt.camera.Viewport(0, 0, width, height)
            if (updateStyle) {
                canvas.style.width = "${width}px"
                canvas.style.height = "${height}px"
            }
            io.kreekt.renderer.RendererResult.Success(Unit)
        } catch (e: Exception) {
            io.kreekt.renderer.RendererResult.Error(io.kreekt.renderer.RendererException.InvalidState("Failed to set size"))
        }
    }

    override fun setPixelRatio(pixelRatio: Float): io.kreekt.renderer.RendererResult<Unit> {
        return io.kreekt.renderer.RendererResult.Success(Unit)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int): io.kreekt.renderer.RendererResult<Unit> {
        viewport = io.kreekt.camera.Viewport(x, y, width, height)
        return io.kreekt.renderer.RendererResult.Success(Unit)
    }

    override fun getViewport(): io.kreekt.camera.Viewport = viewport

    override fun setScissorTest(enable: Boolean): io.kreekt.renderer.RendererResult<Unit> {
        return io.kreekt.renderer.RendererResult.Success(Unit)
    }

    override fun setScissor(x: Int, y: Int, width: Int, height: Int): io.kreekt.renderer.RendererResult<Unit> {
        return io.kreekt.renderer.RendererResult.Success(Unit)
    }

    override fun clear(color: Boolean, depth: Boolean, stencil: Boolean): io.kreekt.renderer.RendererResult<Unit> {
        return try {
            val context = canvas.getContext("webgl2") ?: canvas.getContext("webgl")
            if (context != null) {
                val gl = context.asDynamic()
                var mask = 0
                if (color) mask = mask or gl.COLOR_BUFFER_BIT
                if (depth) mask = mask or gl.DEPTH_BUFFER_BIT
                if (stencil) mask = mask or gl.STENCIL_BUFFER_BIT
                gl.clear(mask)
            }
            io.kreekt.renderer.RendererResult.Success(Unit)
        } catch (e: Exception) {
            io.kreekt.renderer.RendererResult.Error(
                io.kreekt.renderer.RendererException.RenderingFailed(
                    "Clear failed",
                    e
                )
            )
        }
    }

    override fun clearColorBuffer(): io.kreekt.renderer.RendererResult<Unit> =
        clear(color = true, depth = false, stencil = false)

    override fun clearDepth(): io.kreekt.renderer.RendererResult<Unit> =
        clear(color = false, depth = true, stencil = false)

    override fun clearStencil(): io.kreekt.renderer.RendererResult<Unit> =
        clear(color = false, depth = false, stencil = true)

    override fun resetState(): io.kreekt.renderer.RendererResult<Unit> {
        return io.kreekt.renderer.RendererResult.Success(Unit)
    }

    override fun compile(
        scene: io.kreekt.core.scene.Scene,
        camera: io.kreekt.camera.Camera
    ): io.kreekt.renderer.RendererResult<Unit> {
        return io.kreekt.renderer.RendererResult.Success(Unit)
    }

    override fun dispose(): io.kreekt.renderer.RendererResult<Unit> {
        return try {
            // Cleanup WebGPU/WebGL resources
            io.kreekt.renderer.RendererResult.Success(Unit)
        } catch (e: Exception) {
            io.kreekt.renderer.RendererResult.Error(
                io.kreekt.renderer.RendererException.ResourceCreationFailed(
                    "Dispose failed",
                    e
                )
            )
        }
    }

    override fun forceContextLoss(): io.kreekt.renderer.RendererResult<Unit> {
        return io.kreekt.renderer.RendererResult.Success(Unit)
    }

    override fun isContextLost(): Boolean = false

    override fun getStats(): io.kreekt.renderer.RenderStats {
        return io.kreekt.renderer.RenderStats(
            frame = frameCount,
            calls = 1,
            triangles = 0,
            points = 0,
            lines = 0
        )
    }

    override fun resetStats() {
        frameCount = 0
    }

    private fun renderObject(obj: io.kreekt.core.scene.Object3D, camera: io.kreekt.camera.Camera, gl: dynamic) {
        // Basic object rendering
        // In production, this would use WebGPU compute/render pipelines
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
}

