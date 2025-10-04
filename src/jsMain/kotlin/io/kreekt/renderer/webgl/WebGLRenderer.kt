package io.kreekt.renderer.webgl

import io.kreekt.camera.Camera
import io.kreekt.camera.Viewport
import io.kreekt.core.math.Color
import io.kreekt.core.math.Matrix4
import io.kreekt.core.scene.Mesh
import io.kreekt.core.scene.Scene
import io.kreekt.geometry.BufferGeometry
import io.kreekt.material.MeshBasicMaterial
import io.kreekt.renderer.*
import org.khronos.webgl.*
import org.w3c.dom.HTMLCanvasElement

/**
 * WebGL Renderer implementation for KreeKt
 *
 * Provides hardware-accelerated rendering using WebGL API.
 * Supports BufferGeometry with vertex attributes and MeshBasicMaterial.
 */
class WebGLRenderer(
    private val canvas: HTMLCanvasElement,
    private val config: RendererConfig = RendererConfig()
) : Renderer {

    private var gl: WebGLRenderingContext? = null
    private var isInitialized = false
    private var isDisposed = false
    private var contextLost = false

    override val capabilities: RendererCapabilities by lazy {
        createCapabilities()
    }

    override var renderTarget: RenderTarget? = null
    override var autoClear: Boolean = true
    override var autoClearColor: Boolean = true
    override var autoClearDepth: Boolean = true
    override var autoClearStencil: Boolean = true
    override var clearColor: Color = Color(0.53f, 0.81f, 0.92f, 1f) // Sky blue
    override var clearAlpha: Float = 1f
    override var shadowMap: ShadowMapSettings = ShadowMapSettings()
    override var toneMapping: ToneMapping = ToneMapping.NONE
    override var toneMappingExposure: Float = 1f
    override var outputColorSpace: ColorSpace = ColorSpace.sRGB
    override var physicallyCorrectLights: Boolean = false

    private var currentViewport = Viewport(0, 0, canvas.width, canvas.height)
    private var pixelRatio = 1f

    private val stats = RenderStatsTracker()

    // Shader program cache
    private val shaderPrograms = mutableMapOf<String, WebGLProgram>()
    private val buffers = mutableMapOf<Int, WebGLBuffer>()
    private val geometryCache = mutableMapOf<Int, GeometryBuffers>()

    private var nextBufferId = 0
    private var basicShaderProgram: WebGLProgram? = null

    data class GeometryBuffers(
        val positionBuffer: WebGLBuffer,
        val normalBuffer: WebGLBuffer?,
        val colorBuffer: WebGLBuffer?,
        val indexBuffer: WebGLBuffer?,
        val indexCount: Int
    )

    override suspend fun initialize(surface: RenderSurface): RendererResult<Unit> {
        if (isDisposed) return RendererResult.Error(
            RendererException.InvalidState("Renderer is disposed")
        )

        try {
            // Get WebGL context
            val contextAttributes = js(
                """{
                alpha: false,
                depth: true,
                stencil: true,
                antialias: true,
                premultipliedAlpha: true,
                preserveDrawingBuffer: false,
                powerPreference: "high-performance"
            }"""
            )

            val glContext = canvas.getContext("webgl", contextAttributes)
                ?: canvas.getContext("experimental-webgl", contextAttributes)
                ?: return RendererResult.Error(
                    RendererException.InitializationFailed("WebGL not supported")
                )

            gl = glContext.unsafeCast<WebGLRenderingContext>()

            // Setup WebGL state
            gl?.let { gl ->
                gl.enable(WebGLRenderingContext.DEPTH_TEST)
                gl.depthFunc(WebGLRenderingContext.LEQUAL)
                gl.enable(WebGLRenderingContext.CULL_FACE)
                gl.cullFace(WebGLRenderingContext.BACK)
                gl.frontFace(WebGLRenderingContext.CCW)

                // Set clear color
                gl.clearColor(clearColor.r, clearColor.g, clearColor.b, clearAlpha)

                // Set viewport
                gl.viewport(0, 0, canvas.width, canvas.height)
            }

            currentViewport = Viewport(0, 0, canvas.width, canvas.height)
            isInitialized = true
            contextLost = false

            // Create default shader program
            createBasicShaderProgram()

            println("✅ WebGL Renderer initialized")
            return RendererResult.Success(Unit)
        } catch (e: Exception) {
            console.error("WebGLRenderer initialization failed", e)
            return RendererResult.Error(
                RendererException.InitializationFailed("Failed to initialize WebGL renderer", e)
            )
        }
    }

    override fun render(scene: Scene, camera: Camera): RendererResult<Unit> {
        if (!isInitialized) return RendererResult.Error(
            RendererException.InvalidState("Renderer not initialized")
        )
        if (isDisposed) return RendererResult.Error(
            RendererException.InvalidState("Renderer is disposed")
        )
        if (contextLost) return RendererResult.Error(
            RendererException.ContextLost()
        )

        val gl = this.gl ?: return RendererResult.Error(
            RendererException.InvalidState("WebGL context is null")
        )

        try {
            stats.frameStart()

            // Auto-clear if enabled
            if (autoClear) {
                var clearMask = 0
                if (autoClearColor) clearMask = clearMask or WebGLRenderingContext.COLOR_BUFFER_BIT
                if (autoClearDepth) clearMask = clearMask or WebGLRenderingContext.DEPTH_BUFFER_BIT
                if (autoClearStencil) clearMask = clearMask or WebGLRenderingContext.STENCIL_BUFFER_BIT
                gl.clear(clearMask)
            }

            // Update camera matrices
            camera.updateMatrixWorld(true)
            camera.updateProjectionMatrix()

            // Render scene objects
            renderScene(scene, camera, gl)

            stats.frameEnd()

            return RendererResult.Success(Unit)
        } catch (e: Exception) {
            console.error("WebGLRenderer render failed", e)
            return RendererResult.Error(
                RendererException.RenderingFailed("Rendering failed", e)
            )
        }
    }

    private fun renderScene(scene: Scene, camera: Camera, gl: WebGLRenderingContext) {
        val program = basicShaderProgram ?: return
        gl.useProgram(program)

        // Get uniform locations
        val uProjectionMatrix = gl.getUniformLocation(program, "uProjectionMatrix")
        val uViewMatrix = gl.getUniformLocation(program, "uViewMatrix")
        val uModelMatrix = gl.getUniformLocation(program, "uModelMatrix")

        // Set view and projection matrices
        gl.uniformMatrix4fv(uProjectionMatrix, false, camera.projectionMatrix.toFloat32Array())
        gl.uniformMatrix4fv(uViewMatrix, false, camera.matrixWorldInverse.toFloat32Array())

        // Traverse scene and render meshes
        scene.traverse { obj ->
            if (obj is Mesh) {
                renderMesh(obj, gl, program, uModelMatrix)
            }
        }
    }

    private fun renderMesh(
        mesh: Mesh,
        gl: WebGLRenderingContext,
        program: WebGLProgram,
        uModelMatrix: WebGLUniformLocation?
    ) {
        val geometry = mesh.geometry
        val material = mesh.material

        // Only render MeshBasicMaterial for now
        if (material !is MeshBasicMaterial) return

        // Update mesh world matrix
        mesh.updateMatrixWorld(true)

        // Set model matrix
        gl.uniformMatrix4fv(uModelMatrix, false, mesh.matrixWorld.toFloat32Array())

        // Get or create geometry buffers
        val geometryId = geometry.hashCode()
        var buffers = geometryCache[geometryId]

        if (buffers == null) {
            buffers = setupGeometry(geometry, gl, program)
            geometryCache[geometryId] = buffers
        }

        // Bind and render buffers
        renderGeometry(buffers, gl, program)
    }

    private fun setupGeometry(
        geometry: BufferGeometry,
        gl: WebGLRenderingContext,
        program: WebGLProgram
    ): GeometryBuffers {
        // Setup position buffer
        val positionAttr = geometry.getAttribute("position")
            ?: throw Exception("Geometry missing position attribute")
        val positionBuffer = createBuffer(gl, positionAttr.array, WebGLRenderingContext.ARRAY_BUFFER)

        // Setup normal buffer
        val normalAttr = geometry.getAttribute("normal")
        val normalBuffer = normalAttr?.let { createBuffer(gl, it.array, WebGLRenderingContext.ARRAY_BUFFER) }

        // Setup color buffer
        val colorAttr = geometry.getAttribute("color")
        val colorBuffer = colorAttr?.let { createBuffer(gl, it.array, WebGLRenderingContext.ARRAY_BUFFER) }

        // Setup index buffer
        val indexAttr = geometry.index
        var indexBuffer: WebGLBuffer? = null
        var indexCount = 0

        if (indexAttr != null) {
            // Convert Float32Array indices to Uint16Array
            val indices = Uint16Array(indexAttr.count)
            for (i in 0 until indexAttr.count) {
                indices[i] = indexAttr.array[i].toInt().toShort()
            }
            indexBuffer = createBuffer(gl, indices, WebGLRenderingContext.ELEMENT_ARRAY_BUFFER)
            indexCount = indexAttr.count
        }

        return GeometryBuffers(
            positionBuffer = positionBuffer,
            normalBuffer = normalBuffer,
            colorBuffer = colorBuffer,
            indexBuffer = indexBuffer,
            indexCount = indexCount
        )
    }

    private fun renderGeometry(
        buffers: GeometryBuffers,
        gl: WebGLRenderingContext,
        program: WebGLProgram
    ) {
        // Bind position attribute
        val aPosition = gl.getAttribLocation(program, "aPosition")
        if (aPosition >= 0) {
            gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffers.positionBuffer)
            gl.enableVertexAttribArray(aPosition)
            gl.vertexAttribPointer(aPosition, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
        }

        // Bind normal attribute
        buffers.normalBuffer?.let { buffer ->
            val aNormal = gl.getAttribLocation(program, "aNormal")
            if (aNormal >= 0) {
                gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer)
                gl.enableVertexAttribArray(aNormal)
                gl.vertexAttribPointer(aNormal, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
            }
        }

        // Bind color attribute
        buffers.colorBuffer?.let { buffer ->
            val aColor = gl.getAttribLocation(program, "aColor")
            if (aColor >= 0) {
                gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer)
                gl.enableVertexAttribArray(aColor)
                gl.vertexAttribPointer(aColor, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
            }
        }

        // Draw
        if (buffers.indexBuffer != null) {
            gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, buffers.indexBuffer)
            gl.drawElements(
                WebGLRenderingContext.TRIANGLES,
                buffers.indexCount,
                WebGLRenderingContext.UNSIGNED_SHORT,
                0
            )
            stats.addDrawCall()
            stats.addTriangles(buffers.indexCount / 3)
        }
    }

    private fun createBuffer(
        gl: WebGLRenderingContext,
        data: dynamic,
        target: Int
    ): WebGLBuffer {
        val buffer = gl.createBuffer() ?: throw Exception("Failed to create buffer")
        buffers[nextBufferId++] = buffer
        gl.bindBuffer(target, buffer)
        gl.bufferData(target, data as ArrayBufferView, WebGLRenderingContext.STATIC_DRAW)
        return buffer
    }

    private fun createBasicShaderProgram() {
        val gl = this.gl ?: return

        val vertexShaderSource = """
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec3 aColor;

            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            uniform mat4 uProjectionMatrix;

            varying vec3 vColor;
            varying vec3 vNormal;

            void main() {
                vColor = aColor;
                vNormal = mat3(uModelMatrix) * aNormal;
                gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * vec4(aPosition, 1.0);
            }
        """.trimIndent()

        val fragmentShaderSource = """
            precision highp float;

            varying vec3 vColor;
            varying vec3 vNormal;

            void main() {
                // Simple directional lighting
                vec3 lightDir = normalize(vec3(0.5, 1.0, 0.5));
                float diff = max(dot(normalize(vNormal), lightDir), 0.0);
                vec3 ambient = vec3(0.3);
                vec3 lighting = ambient + diff * 0.7;

                gl_FragColor = vec4(vColor * lighting, 1.0);
            }
        """.trimIndent()

        val program = compileShaderProgram(gl, vertexShaderSource, fragmentShaderSource)
        if (program != null) {
            basicShaderProgram = program
            shaderPrograms["basic"] = program
        }
    }

    private fun compileShaderProgram(
        gl: WebGLRenderingContext,
        vertexSource: String,
        fragmentSource: String
    ): WebGLProgram? {
        val vertexShader = compileShader(gl, WebGLRenderingContext.VERTEX_SHADER, vertexSource)
            ?: return null
        val fragmentShader = compileShader(gl, WebGLRenderingContext.FRAGMENT_SHADER, fragmentSource)
            ?: return null

        val program = gl.createProgram() ?: return null
        gl.attachShader(program, vertexShader)
        gl.attachShader(program, fragmentShader)
        gl.linkProgram(program)

        if (gl.getProgramParameter(program, WebGLRenderingContext.LINK_STATUS) == false) {
            console.error("Shader program linking failed:", gl.getProgramInfoLog(program))
            gl.deleteProgram(program)
            return null
        }

        return program
    }

    private fun compileShader(
        gl: WebGLRenderingContext,
        type: Int,
        source: String
    ): WebGLShader? {
        val shader = gl.createShader(type) ?: return null
        gl.shaderSource(shader, source)
        gl.compileShader(shader)

        if (gl.getShaderParameter(shader, WebGLRenderingContext.COMPILE_STATUS) == false) {
            console.error("Shader compilation failed:", gl.getShaderInfoLog(shader))
            gl.deleteShader(shader)
            return null
        }

        return shader
    }

    override fun setSize(width: Int, height: Int, updateStyle: Boolean): RendererResult<Unit> {
        if (width <= 0 || height <= 0) {
            return RendererResult.Error(
                RendererException.InvalidState("Invalid dimensions: ${width}x${height}")
            )
        }

        canvas.width = width
        canvas.height = height

        if (updateStyle) {
            canvas.style.width = "${width}px"
            canvas.style.height = "${height}px"
        }

        gl?.viewport(0, 0, width, height)
        currentViewport = Viewport(0, 0, width, height)

        return RendererResult.Success(Unit)
    }

    override fun setPixelRatio(pixelRatio: Float): RendererResult<Unit> {
        this.pixelRatio = pixelRatio
        return RendererResult.Success(Unit)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit> {
        gl?.viewport(x, y, width, height)
        currentViewport = Viewport(x, y, width, height)
        return RendererResult.Success(Unit)
    }

    override fun getViewport(): Viewport = currentViewport

    override fun setScissorTest(enable: Boolean): RendererResult<Unit> {
        if (enable) {
            gl?.enable(WebGLRenderingContext.SCISSOR_TEST)
        } else {
            gl?.disable(WebGLRenderingContext.SCISSOR_TEST)
        }
        return RendererResult.Success(Unit)
    }

    override fun setScissor(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit> {
        gl?.scissor(x, y, width, height)
        return RendererResult.Success(Unit)
    }

    override fun clear(color: Boolean, depth: Boolean, stencil: Boolean): RendererResult<Unit> {
        var mask = 0
        if (color) mask = mask or WebGLRenderingContext.COLOR_BUFFER_BIT
        if (depth) mask = mask or WebGLRenderingContext.DEPTH_BUFFER_BIT
        if (stencil) mask = mask or WebGLRenderingContext.STENCIL_BUFFER_BIT
        gl?.clear(mask)
        return RendererResult.Success(Unit)
    }

    override fun clearColorBuffer(): RendererResult<Unit> {
        gl?.clear(WebGLRenderingContext.COLOR_BUFFER_BIT)
        return RendererResult.Success(Unit)
    }

    override fun clearDepth(): RendererResult<Unit> {
        gl?.clear(WebGLRenderingContext.DEPTH_BUFFER_BIT)
        return RendererResult.Success(Unit)
    }

    override fun clearStencil(): RendererResult<Unit> {
        gl?.clear(WebGLRenderingContext.STENCIL_BUFFER_BIT)
        return RendererResult.Success(Unit)
    }

    override fun resetState(): RendererResult<Unit> {
        gl?.let { gl ->
            gl.enable(WebGLRenderingContext.DEPTH_TEST)
            gl.enable(WebGLRenderingContext.CULL_FACE)
            gl.disable(WebGLRenderingContext.SCISSOR_TEST)
        }
        return RendererResult.Success(Unit)
    }

    override fun compile(scene: Scene, camera: Camera): RendererResult<Unit> {
        // Pre-compile shaders for all materials
        return RendererResult.Success(Unit)
    }

    override fun dispose(): RendererResult<Unit> {
        gl?.let { gl ->
            // Delete all buffers
            buffers.values.forEach { buffer ->
                gl.deleteBuffer(buffer)
            }
            buffers.clear()

            // Delete all shader programs
            shaderPrograms.values.forEach { program ->
                gl.deleteProgram(program)
            }
            shaderPrograms.clear()

            geometryCache.clear()
        }

        gl = null
        isInitialized = false
        isDisposed = true
        return RendererResult.Success(Unit)
    }

    override fun forceContextLoss(): RendererResult<Unit> {
        contextLost = true
        return RendererResult.Success(Unit)
    }

    override fun isContextLost(): Boolean = contextLost

    override fun getStats(): RenderStats = stats.getStats()

    override fun resetStats() = stats.reset()

    private fun createCapabilities(): RendererCapabilities {
        val gl = this.gl
        return if (gl != null) {
            RendererCapabilities(
                maxTextureSize = gl.getParameter(WebGLRenderingContext.MAX_TEXTURE_SIZE) as Int,
                maxCubeMapSize = gl.getParameter(WebGLRenderingContext.MAX_CUBE_MAP_TEXTURE_SIZE) as Int,
                maxVertexAttributes = gl.getParameter(WebGLRenderingContext.MAX_VERTEX_ATTRIBS) as Int,
                maxVertexUniforms = gl.getParameter(WebGLRenderingContext.MAX_VERTEX_UNIFORM_VECTORS) as Int,
                maxFragmentUniforms = gl.getParameter(WebGLRenderingContext.MAX_FRAGMENT_UNIFORM_VECTORS) as Int,
                maxFragmentTextures = gl.getParameter(WebGLRenderingContext.MAX_TEXTURE_IMAGE_UNITS) as Int,
                maxSamples = 4,
                maxAnisotropy = 16f,
                vendor = "WebGL",
                renderer = "WebGLRenderer",
                version = "WebGL 1.0",
                shadingLanguageVersion = "GLSL ES 1.00"
            )
        } else {
            CapabilitiesUtils.getCompatibilityCapabilities()
        }
    }
}

/**
 * Extension to convert Matrix4 to Float32Array for WebGL
 */
private fun Matrix4.toFloat32Array(): Float32Array {
    val array = Float32Array(16)
    val elements = this.elements
    for (i in 0 until 16) {
        array[i] = elements[i].toFloat()
    }
    return array
}

/**
 * Statistics tracking for renderer performance
 */
private class RenderStatsTracker {
    private var frameCount = 0
    private var drawCalls = 0
    private var triangles = 0

    fun frameStart() {
        drawCalls = 0
        triangles = 0
    }

    fun frameEnd() {
        frameCount++
    }

    fun addDrawCall() {
        drawCalls++
    }

    fun addTriangles(count: Int) {
        triangles += count
    }

    fun getStats(): RenderStats {
        return RenderStats(
            frame = frameCount,
            calls = drawCalls,
            triangles = triangles
        )
    }

    fun reset() {
        frameCount = 0
        drawCalls = 0
        triangles = 0
    }
}
