package io.kreekt.renderer.webgl

import io.kreekt.camera.Camera
import io.kreekt.camera.Viewport
import io.kreekt.core.math.Color
import io.kreekt.core.math.Matrix4
import io.kreekt.core.scene.Mesh
import io.kreekt.core.scene.Scene
import io.kreekt.geometry.BufferGeometry
import io.kreekt.material.MeshBasicMaterial
import io.kreekt.optimization.Frustum
import io.kreekt.optimization.BoundingBox
import io.kreekt.renderer.*
import io.kreekt.util.KreektLogger
import org.khronos.webgl.*
import org.w3c.dom.HTMLCanvasElement

// T011: WebGL VAO extension types
external interface OES_vertex_array_object {
    fun createVertexArrayOES(): WebGLVertexArrayObjectOES?
    fun deleteVertexArrayOES(arrayObject: WebGLVertexArrayObjectOES?)
    fun isVertexArrayOES(arrayObject: WebGLVertexArrayObjectOES?): Boolean
    fun bindVertexArrayOES(arrayObject: WebGLVertexArrayObjectOES?)
}

external interface WebGLVertexArrayObjectOES

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
    private var vaoExt: OES_vertex_array_object? = null  // T011: VAO extension
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
    private val geometryCache = mutableMapOf<Int, GeometryBuffers>()  // T025: Use mesh.id for stable caching

    // T023: Buffer creation throttling to prevent lag when many meshes load at once
    private var bufferCreationsThisFrame = 0
    private val maxBufferCreationsPerFrame = 2  // Limit to 2 new geometries per frame (aggressive throttling)

    private var nextBufferId = 0
    private var basicShaderProgram: WebGLProgram? = null

    data class GeometryBuffers(
        val positionBuffer: WebGLBuffer,
        val normalBuffer: WebGLBuffer?,
        val colorBuffer: WebGLBuffer?,
        val indexBuffer: WebGLBuffer?,
        val indexCount: Int,
        val vao: WebGLVertexArrayObjectOES? = null  // T011: VAO for efficient attribute setup
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

            // T011: Get VAO extension for efficient vertex attribute setup
            gl?.let { gl ->
                vaoExt = gl.getExtension("OES_vertex_array_object")?.unsafeCast<OES_vertex_array_object>()
                if (vaoExt != null) {
                    console.log("✅ VAO extension available")
                } else {
                    console.warn("⚠️ VAO extension not available - performance will be reduced")
                }
            }

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

            KreektLogger.info("WebGLRenderer", "WebGL Renderer initialized successfully")
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

    private var frameCount = 0
    private var lastFrameTime = 0.0

    private fun renderScene(scene: Scene, camera: Camera, gl: WebGLRenderingContext) {
        val frameStartTime = js("performance.now()") as Double
        frameCount++
        val startTime = if (frameCount <= 10) js("performance.now()") as Double else 0.0

        // T023: Reset buffer creation counter at the start of each frame
        bufferCreationsThisFrame = 0

        // T003: Shader compilation validation (keep error checking, remove verbose logging)
        val program = basicShaderProgram
        if (program == null) {
            console.error("🔴 basicShaderProgram is null - shader compilation failed!")
            console.error("🔴 Renderer initialization may have failed silently")
            return
        }
        gl.useProgram(program)

        // Get uniform locations
        val uProjectionMatrix = gl.getUniformLocation(program, "uProjectionMatrix")
        val uViewMatrix = gl.getUniformLocation(program, "uViewMatrix")
        val uModelMatrix = gl.getUniformLocation(program, "uModelMatrix")

        // Set view and projection matrices
        gl.uniformMatrix4fv(uProjectionMatrix, false, camera.projectionMatrix.toFloat32Array())
        gl.uniformMatrix4fv(uViewMatrix, false, camera.matrixWorldInverse.toFloat32Array())

        // T009: Create frustum for culling
        val projectionViewMatrix = Matrix4()
            .copy(camera.projectionMatrix)
            .multiply(camera.matrixWorldInverse)

        val frustum = Frustum()
        frustum.setFromMatrix(projectionViewMatrix)

        var culledCount = 0
        var visibleCount = 0

        val traverseStart = if (frameCount <= 10) js("performance.now()") as Double else 0.0

        // T024: Measure rendering time
        var renderMeshTime = 0.0

        // T001/T020: Scene traversal with frustum culling
        var meshCount = 0
        scene.traverse { obj ->
            if (obj is Mesh) {
                // T009: Check if mesh has chunk data for frustum culling
                val chunk = obj.userData["chunk"]
                if (chunk != null) {
                    // VoxelCraft chunk - use frustum culling
                    try {
                        val boundingBox = js("chunk.boundingBox")
                        if (boundingBox != null) {
                            val isVisible = frustum.intersectsBox(boundingBox.unsafeCast<BoundingBox>())
                            if (!isVisible) {
                                culledCount++
                                return@traverse // Skip this mesh
                            }
                        }
                    } catch (e: Exception) {
                        // If anything fails, render the mesh anyway
                        console.warn("Frustum culling error: ${e.message}")
                    }
                }
                visibleCount++
                meshCount++

                // T024: Time the render call
                val meshStart = js("performance.now()") as Double
                renderMesh(obj, gl, program, uModelMatrix)
                renderMeshTime += js("performance.now()") as Double - meshStart
            }
        }

        if (frameCount <= 10) {
            val traverseTime = js("performance.now()") as Double - traverseStart
            val totalTime = js("performance.now()") as Double - startTime
            console.log("⏱️ Frame $frameCount: Total=${totalTime.toInt()}ms, Traverse=${traverseTime.toInt()}ms, Meshes=$meshCount")
        }

        // T009: Log frustum culling statistics
        if (culledCount > 0 || visibleCount > 0) {
            console.log("T009 WebGL Frustum culling: $visibleCount visible, $culledCount culled (${culledCount + visibleCount} total)")
        }

        // T023: Log buffer creation throttling statistics
        if (bufferCreationsThisFrame > 0) {
            console.log("T023 Buffer creation: $bufferCreationsThisFrame new geometries this frame (max $maxBufferCreationsPerFrame)")
        }

        if (meshCount == 0) {
            console.warn("⚠️ Scene traversal found 0 meshes - check scene.add() calls")
        }

        // T024: Measure total frame time and log slow frames
        val totalFrameTime = js("performance.now()") as Double - frameStartTime
        val fps = if (totalFrameTime > 0) 1000.0 / totalFrameTime else 0.0

        if (totalFrameTime > 33) {  // Slower than 30 FPS
            val avgPerMesh = if (meshCount > 0) renderMeshTime / meshCount else 0.0
            console.error("🐌 T024 SLOW FRAME: ${totalFrameTime.toInt()}ms (${fps.toInt()} FPS)")
            console.error("   Meshes: $meshCount, renderMesh total: ${renderMeshTime.toInt()}ms, avg: ${avgPerMesh.toInt()}ms/mesh")
        }

        lastFrameTime = totalFrameTime
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

        // Update mesh world matrix (only if needed, don't force)
        mesh.updateMatrixWorld(false)  // Changed from true to false - don't force recompute

        // Set model matrix
        gl.uniformMatrix4fv(uModelMatrix, false, mesh.matrixWorld.toFloat32Array())

        // T025: Get or create geometry buffers (use mesh.id for stable caching)
        var buffers = geometryCache[mesh.id]

        if (buffers == null) {
            // T023: Throttle buffer creation to prevent lag when many meshes load at once
            if (bufferCreationsThisFrame >= maxBufferCreationsPerFrame) {
                // Reached limit for this frame - skip this mesh, it will be rendered next frame
                return
            }

            buffers = setupGeometry(geometry, gl, program)
            geometryCache[mesh.id] = buffers
            bufferCreationsThisFrame++
        }

        // Bind and render buffers
        renderGeometry(buffers, geometry, gl, program)
    }

    private fun setupGeometry(
        geometry: BufferGeometry,
        gl: WebGLRenderingContext,
        program: WebGLProgram
    ): GeometryBuffers {
        val startTime = js("performance.now()") as Double

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

        // T011: Create VAO and setup all vertex attributes once
        var vao: WebGLVertexArrayObjectOES? = null
        vaoExt?.let { ext ->
            vao = ext.createVertexArrayOES()
            ext.bindVertexArrayOES(vao)

            // Setup all vertex attributes inside the VAO
            val aPosition = getOrCacheAttribLocation(gl, program, "aPosition")
            if (aPosition >= 0) {
                gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, positionBuffer)
                gl.enableVertexAttribArray(aPosition)
                gl.vertexAttribPointer(aPosition, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
            }

            normalBuffer?.let { buffer ->
                val aNormal = getOrCacheAttribLocation(gl, program, "aNormal")
                if (aNormal >= 0) {
                    gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer)
                    gl.enableVertexAttribArray(aNormal)
                    gl.vertexAttribPointer(aNormal, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
                }
            }

            colorBuffer?.let { buffer ->
                val aColor = getOrCacheAttribLocation(gl, program, "aColor")
                if (aColor >= 0) {
                    gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer)
                    gl.enableVertexAttribArray(aColor)
                    gl.vertexAttribPointer(aColor, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
                }
            }

            // Bind index buffer (stored in VAO)
            indexBuffer?.let {
                gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, it)
            }

            // Unbind VAO
            ext.bindVertexArrayOES(null)
        }

        val totalTime = js("performance.now()") as Double - startTime
        if (totalTime > 10) {
            console.warn("⚠️ setupGeometry took ${totalTime.toInt()}ms for ${indexCount/3} triangles")
        }

        return GeometryBuffers(
            positionBuffer = positionBuffer,
            normalBuffer = normalBuffer,
            colorBuffer = colorBuffer,
            indexBuffer = indexBuffer,
            indexCount = indexCount,
            vao = vao
        )
    }

    // Cache attribute locations (CRITICAL for performance)
    private var cachedAttribLocations: Map<String, Int>? = null

    private fun getOrCacheAttribLocation(gl: WebGLRenderingContext, program: WebGLProgram, name: String): Int {
        if (cachedAttribLocations == null) {
            // Cache all attribute locations once
            cachedAttribLocations = mapOf(
                "aPosition" to gl.getAttribLocation(program, "aPosition"),
                "aNormal" to gl.getAttribLocation(program, "aNormal"),
                "aColor" to gl.getAttribLocation(program, "aColor"),
                "aUV" to gl.getAttribLocation(program, "aUV")
            )
            console.log("✅ Cached attribute locations: ${cachedAttribLocations}")
        }
        return cachedAttribLocations!![name] ?: -1
    }

    private fun renderGeometry(
        buffers: GeometryBuffers,
        geometry: BufferGeometry,
        gl: WebGLRenderingContext,
        program: WebGLProgram
    ) {
        // T011: Use VAO if available (much faster - single bind instead of 3-4 attribute setups)
        val ext = vaoExt
        if (buffers.vao != null && ext != null) {
            // Fast path: Just bind the VAO
            ext.bindVertexArrayOES(buffers.vao)
        } else {
            // Fallback path: Set up vertex attributes manually
            val aPosition = getOrCacheAttribLocation(gl, program, "aPosition")
            if (aPosition >= 0) {
                gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffers.positionBuffer)
                gl.enableVertexAttribArray(aPosition)
                gl.vertexAttribPointer(aPosition, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
            }

            buffers.normalBuffer?.let { buffer ->
                val aNormal = getOrCacheAttribLocation(gl, program, "aNormal")
                if (aNormal >= 0) {
                    gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer)
                    gl.enableVertexAttribArray(aNormal)
                    gl.vertexAttribPointer(aNormal, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
                }
            }

            buffers.colorBuffer?.let { buffer ->
                val aColor = getOrCacheAttribLocation(gl, program, "aColor")
                if (aColor >= 0) {
                    gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer)
                    gl.enableVertexAttribArray(aColor)
                    gl.vertexAttribPointer(aColor, 3, WebGLRenderingContext.FLOAT, false, 0, 0)
                }
            }

            if (buffers.indexBuffer != null) {
                gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, buffers.indexBuffer)
            }
        }

        // Draw
        if (buffers.indexBuffer != null) {
            // Indexed rendering
            gl.drawElements(
                WebGLRenderingContext.TRIANGLES,
                buffers.indexCount,
                WebGLRenderingContext.UNSIGNED_SHORT,
                0
            )
            stats.addDrawCall()
            stats.addTriangles(buffers.indexCount / 3)
        } else {
            // Non-indexed rendering fallback
            val positionAttr = geometry.getAttribute("position")
            val vertexCount = positionAttr?.count ?: 0
            if (vertexCount > 0) {
                gl.drawArrays(
                    WebGLRenderingContext.TRIANGLES,
                    0,
                    vertexCount
                )
                stats.addDrawCall()
                stats.addTriangles(vertexCount / 3)
            }
        }

        // T011: Unbind VAO after drawing
        if (buffers.vao != null && ext != null) {
            ext.bindVertexArrayOES(null)
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
        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        val arrayBufferView = data as? ArrayBufferView
            ?: throw IllegalArgumentException("Data must be ArrayBufferView")
        gl.bufferData(target, arrayBufferView, WebGLRenderingContext.STATIC_DRAW)
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
                maxTextureSize = (gl.getParameter(WebGLRenderingContext.MAX_TEXTURE_SIZE) as? Int) ?: 2048,
                maxCubeMapSize = (gl.getParameter(WebGLRenderingContext.MAX_CUBE_MAP_TEXTURE_SIZE) as? Int) ?: 2048,
                maxVertexAttributes = (gl.getParameter(WebGLRenderingContext.MAX_VERTEX_ATTRIBS) as? Int) ?: 16,
                maxVertexUniforms = (gl.getParameter(WebGLRenderingContext.MAX_VERTEX_UNIFORM_VECTORS) as? Int) ?: 256,
                maxFragmentUniforms = (gl.getParameter(WebGLRenderingContext.MAX_FRAGMENT_UNIFORM_VECTORS) as? Int) ?: 256,
                maxFragmentTextures = (gl.getParameter(WebGLRenderingContext.MAX_TEXTURE_IMAGE_UNITS) as? Int) ?: 16,
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
