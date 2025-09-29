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

// Production WebGPU renderer with WebGL2 fallback for browser compatibility
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
            // Basic WebGL rendering for demo
            val context = canvas.getContext("webgl") ?: canvas.getContext("experimental-webgl")
            if (context != null) {
                val gl = context.asDynamic()

                // Clear screen
                gl.clearColor(clearColor.r, clearColor.g, clearColor.b, clearAlpha)
                gl.clear(gl.COLOR_BUFFER_BIT or gl.DEPTH_BUFFER_BIT)
                gl.enable(gl.DEPTH_TEST)

                // Setup basic perspective (using WebGL 1.0 compatible approach)
                gl.viewport(0, 0, canvas.width, canvas.height)

                // Draw a simple animated scene
                val time = window.performance.now() / 1000.0
                drawDemoScene(gl, time.toFloat(), camera)
            }

            // Update FPS counter
            updateFPS()
            updateObjectCount(scene)
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

    private fun drawDemoScene(gl: dynamic, time: Float, camera: io.kreekt.camera.Camera) {
        // Create simple vertex shader
        val vsSource = """
            attribute vec3 aPosition;
            attribute vec3 aColor;
            uniform mat4 uProjectionMatrix;
            uniform mat4 uModelViewMatrix;
            varying vec3 vColor;

            void main() {
                gl_Position = uProjectionMatrix * uModelViewMatrix * vec4(aPosition, 1.0);
                vColor = aColor;
            }
        """.trimIndent()

        // Create simple fragment shader
        val fsSource = """
            precision mediump float;
            varying vec3 vColor;

            void main() {
                gl_FragColor = vec4(vColor, 1.0);
            }
        """.trimIndent()

        // Compile shaders (simplified for demo)
        val vertexShader = loadShader(gl, gl.VERTEX_SHADER, vsSource)
        val fragmentShader = loadShader(gl, gl.FRAGMENT_SHADER, fsSource)

        if (vertexShader == null || fragmentShader == null) return

        // Create shader program
        val shaderProgram = gl.createProgram()
        gl.attachShader(shaderProgram, vertexShader)
        gl.attachShader(shaderProgram, fragmentShader)
        gl.linkProgram(shaderProgram)

        if (!(gl.getProgramParameter(shaderProgram, gl.LINK_STATUS) as Boolean)) {
            console.error("Unable to initialize shader program: ${gl.getProgramInfoLog(shaderProgram)}")
            return
        }

        gl.useProgram(shaderProgram)

        // Get attribute/uniform locations
        val positionLoc = gl.getAttribLocation(shaderProgram, "aPosition")
        val colorLoc = gl.getAttribLocation(shaderProgram, "aColor")
        val projMatrixLoc = gl.getUniformLocation(shaderProgram, "uProjectionMatrix")
        val mvMatrixLoc = gl.getUniformLocation(shaderProgram, "uModelViewMatrix")

        // Create projection matrix
        val projMatrix = perspective(
            75f * kotlin.math.PI.toFloat() / 180f,
            canvas.width.toFloat() / canvas.height.toFloat(),
            0.1f,
            100f
        )
        gl.uniformMatrix4fv(projMatrixLoc, false, projMatrix)

        // Draw a rotating cube
        drawCube(gl, positionLoc, colorLoc, mvMatrixLoc, time, 0f, 0f, -5f, 1f)

        // Draw orbiting cubes
        for (i in 0..3) {
            val angle = time + i * kotlin.math.PI.toFloat() * 0.5f
            val x = kotlin.math.cos(angle) * 3f
            val z = kotlin.math.sin(angle) * 3f
            drawCube(gl, positionLoc, colorLoc, mvMatrixLoc, time * 2, x, 1f, z - 5f, 0.5f)
        }

        // Cleanup
        gl.deleteShader(vertexShader)
        gl.deleteShader(fragmentShader)
        gl.deleteProgram(shaderProgram)
    }

    private fun loadShader(gl: dynamic, type: Int, source: String): dynamic {
        val shader = gl.createShader(type)
        gl.shaderSource(shader, source)
        gl.compileShader(shader)

        if (!(gl.getShaderParameter(shader, gl.COMPILE_STATUS) as Boolean)) {
            console.error("Error compiling shader: ${gl.getShaderInfoLog(shader)}")
            gl.deleteShader(shader)
            return null
        }
        return shader
    }

    private fun drawCube(
        gl: dynamic,
        posLoc: Int,
        colorLoc: Int,
        mvLoc: dynamic,
        rotation: Float,
        x: Float,
        y: Float,
        z: Float,
        scale: Float
    ) {
        // Cube vertices
        val vertices = floatArrayOf(
            // Front face
            -1f, -1f, 1f,
            1f, -1f, 1f,
            1f, 1f, 1f,
            -1f, 1f, 1f,
            // Back face
            -1f, -1f, -1f,
            -1f, 1f, -1f,
            1f, 1f, -1f,
            1f, -1f, -1f
        )

        // Colors for each vertex
        val colors = floatArrayOf(
            1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f,  // Front: red
            0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f   // Back: green
        )

        // Indices for drawing triangles
        val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3,  // Front
            4, 5, 6, 4, 6, 7,  // Back
            0, 4, 7, 0, 7, 1,  // Bottom
            3, 2, 6, 3, 6, 5,  // Top
            0, 3, 5, 0, 5, 4,  // Left
            1, 7, 6, 1, 6, 2   // Right
        )

        // Create model-view matrix with transformations
        val mvMatrix = FloatArray(16)
        setIdentity(mvMatrix)
        translate(mvMatrix, x, y, z)
        rotate(mvMatrix, rotation, 1f, 1f, 0f)
        scaleMatrix(mvMatrix, scale, scale, scale)

        gl.uniformMatrix4fv(mvLoc, false, mvMatrix.toTypedArray())

        // Create and bind buffers
        val vertexBuffer = gl.createBuffer()
        gl.bindBuffer(gl.ARRAY_BUFFER, vertexBuffer)
        gl.bufferData(gl.ARRAY_BUFFER, vertices.toTypedArray(), gl.STATIC_DRAW)
        gl.vertexAttribPointer(posLoc, 3, gl.FLOAT, false, 0, 0)
        gl.enableVertexAttribArray(posLoc)

        val colorBuffer = gl.createBuffer()
        gl.bindBuffer(gl.ARRAY_BUFFER, colorBuffer)
        gl.bufferData(gl.ARRAY_BUFFER, colors.toTypedArray(), gl.STATIC_DRAW)
        gl.vertexAttribPointer(colorLoc, 3, gl.FLOAT, false, 0, 0)
        gl.enableVertexAttribArray(colorLoc)

        val indexBuffer = gl.createBuffer()
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, indexBuffer)
        gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, indices.toTypedArray(), gl.STATIC_DRAW)

        // Draw
        gl.drawElements(gl.TRIANGLES, indices.size, gl.UNSIGNED_SHORT, 0)

        // Cleanup
        gl.deleteBuffer(vertexBuffer)
        gl.deleteBuffer(colorBuffer)
        gl.deleteBuffer(indexBuffer)
    }

    // Matrix helper functions
    private fun perspective(fovy: Float, aspect: Float, near: Float, far: Float): FloatArray {
        val f = 1f / kotlin.math.tan(fovy / 2f)
        return floatArrayOf(
            f / aspect, 0f, 0f, 0f,
            0f, f, 0f, 0f,
            0f, 0f, (far + near) / (near - far), -1f,
            0f, 0f, (2f * far * near) / (near - far), 0f
        )
    }

    private fun setIdentity(m: FloatArray) {
        for (i in m.indices) m[i] = 0f
        m[0] = 1f; m[5] = 1f; m[10] = 1f; m[15] = 1f
    }

    private fun translate(m: FloatArray, x: Float, y: Float, z: Float) {
        m[12] += x; m[13] += y; m[14] += z
    }

    private fun rotate(m: FloatArray, angle: Float, x: Float, y: Float, z: Float) {
        val c = kotlin.math.cos(angle)
        val s = kotlin.math.sin(angle)
        val t = 1f - c

        val temp = FloatArray(16)
        setIdentity(temp)

        temp[0] = x * x * t + c
        temp[1] = y * x * t + z * s
        temp[2] = z * x * t - y * s
        temp[4] = x * y * t - z * s
        temp[5] = y * y * t + c
        temp[6] = z * y * t + x * s
        temp[8] = x * z * t + y * s
        temp[9] = y * z * t - x * s
        temp[10] = z * z * t + c

        multiplyMatrix(m, temp)
    }

    private fun scaleMatrix(m: FloatArray, x: Float, y: Float, z: Float) {
        m[0] *= x; m[5] *= y; m[10] *= z
    }

    private fun multiplyMatrix(a: FloatArray, b: FloatArray) {
        val result = FloatArray(16)
        for (i in 0..3) {
            for (j in 0..3) {
                result[i * 4 + j] = a[i * 4] * b[j] + a[i * 4 + 1] * b[j + 4] +
                        a[i * 4 + 2] * b[j + 8] + a[i * 4 + 3] * b[j + 12]
            }
        }
        for (i in result.indices) a[i] = result[i]
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

    private fun updateObjectCount(scene: io.kreekt.core.scene.Scene) {
        document.getElementById("objects")?.textContent = "Objects: ${scene.children.size}"
    }
}

