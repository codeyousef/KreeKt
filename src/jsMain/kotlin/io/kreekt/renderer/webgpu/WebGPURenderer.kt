package io.kreekt.renderer.webgpu

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
import io.kreekt.renderer.webgpu.shaders.BasicShaders
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLCanvasElement
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

/**
 * Await a JavaScript Promise in a suspend function.
 * This is a workaround for the missing kotlinx.coroutines.await import issue.
 */
private suspend fun <T> Promise<T>.awaitPromise(): T = suspendCancellableCoroutine { cont ->
    this.then(
        onFulfilled = { value -> cont.resume(value) },
        onRejected = { error -> cont.resumeWithException(error as? Throwable ?: Exception(error.toString())) }
    )
}

/**
 * Main WebGPU renderer class implementing the Renderer interface.
 * T036: Complete WebGPU renderer implementation.
 *
 * FR-002: Canvas initialization
 * FR-003: Basic geometry rendering
 * FR-004: Buffer management
 * FR-009: Performance (60 FPS @ 1M triangles)
 * FR-011: Context loss recovery
 * FR-013: Pipeline caching
 */
class WebGPURenderer(private val canvas: HTMLCanvasElement) : Renderer {

    // Core WebGPU objects
    private var device: GPUDevice? = null
    private var context: GPUCanvasContext? = null
    private var adapter: GPUAdapter? = null

    // Component managers
    private lateinit var pipelineCache: PipelineCache
    private lateinit var bufferPool: BufferPool
    private lateinit var contextLossRecovery: ContextLossRecovery

    // Rendering state
    private var currentPipeline: WebGPUPipeline? = null
    private var frameCount = 0
    private var triangleCount = 0
    private var drawCallCount = 0

    // Geometry buffer cache (mesh.uuid -> buffers)
    private val geometryBuffers = mutableMapOf<String, GeometryBuffers>()

    // Pipeline cache map (for synchronous access)
    private val pipelineCacheMap = mutableMapOf<PipelineKey, WebGPUPipeline>()

    // Uniform buffer for MVP matrices
    private var uniformBuffer: WebGPUBuffer? = null
    private var bindGroup: GPUBindGroup? = null

    // Capabilities
    private var rendererCapabilities: RendererCapabilities? = null

    // Viewport
    private var viewport = Viewport(0, 0, canvas.width, canvas.height)

    // Renderer interface properties
    override val capabilities: RendererCapabilities
        get() = rendererCapabilities ?: createDefaultCapabilities()

    override var renderTarget: RenderTarget? = null
    override var autoClear: Boolean = true
    override var autoClearColor: Boolean = true
    override var autoClearDepth: Boolean = true
    override var autoClearStencil: Boolean = true
    override var clearColor: Color = Color(0x000000)
    override var clearAlpha: Float = 1f
    override var shadowMap: ShadowMapSettings = ShadowMapSettings()
    override var toneMapping: ToneMapping = ToneMapping.NONE
    override var toneMappingExposure: Float = 1f
    override var outputColorSpace: ColorSpace = ColorSpace.sRGB
    override var physicallyCorrectLights: Boolean = false

    var isInitialized: Boolean = false
        private set

    val isWebGPU: Boolean = true

    /**
     * Initializes the WebGPU renderer.
     * @return RendererResult.Success or RendererResult.Error
     */
    suspend fun initialize(): RendererResult<Unit> {
        return try {
            val startTime = js("performance.now()").unsafeCast<Double>()

            // Get GPU
            val gpu = WebGPUDetector.getGPU()
            if (gpu == null) {
                return RendererResult.Error(
                    RendererException.InitializationFailed("WebGPU not available")
                )
            }

            // Request adapter
            val adapterOptions = js("({})").unsafeCast<GPURequestAdapterOptions>()
            adapterOptions.powerPreference = "high-performance"

            val adapterPromise = gpu.requestAdapter(adapterOptions).unsafeCast<Promise<GPUAdapter?>>()
            val adapterResult = adapterPromise.awaitPromise()
            if (adapterResult == null) {
                return RendererResult.Error(
                    RendererException.InitializationFailed("Failed to request GPU adapter")
                )
            }
            adapter = adapterResult

            // Request device
            val deviceDescriptor = js("({})").unsafeCast<GPUDeviceDescriptor>()
            deviceDescriptor.label = "KreeKt WebGPU Device"

            val devicePromise = adapter!!.requestDevice(deviceDescriptor).unsafeCast<Promise<GPUDevice>>()
            device = devicePromise.awaitPromise()

            // Setup device error handlers
            device!!.asDynamic().addEventListener("uncapturederror", { event: dynamic ->
                console.error("WebGPU uncaptured error:", event.error)
                console.error("Error type:", event.error.constructor.name)
                console.error("Error message:", event.error.message)
            })

            // Monitor device loss
            GlobalScope.launch {
                try {
                    device!!.lost.unsafeCast<Promise<dynamic>>().awaitPromise()
                    console.error("WebGPU device lost!")
                    contextLossRecovery.handleContextLoss()
                } catch (e: Exception) {
                    console.error("Error monitoring device loss: ${e.message}")
                }
            }

            // Configure canvas context
            context = canvas.getContext("webgpu").unsafeCast<GPUCanvasContext?>()
                ?: return RendererResult.Error(
                    RendererException.InitializationFailed("Failed to get WebGPU context")
                )

            val contextConfig = js("({})").unsafeCast<GPUCanvasConfiguration>()
            contextConfig.device = device!!
            contextConfig.format = "bgra8unorm"
            contextConfig.alphaMode = "opaque"
            context!!.configure(contextConfig)

            // Initialize component managers
            pipelineCache = PipelineCache()
            bufferPool = BufferPool(device!!)
            contextLossRecovery = ContextLossRecovery()

            // Setup context loss monitoring
            contextLossRecovery.onContextLost = {
                isInitialized = false
                console.warn("WebGPU context lost - attempting recovery...")
            }
            contextLossRecovery.onContextRestored = {
                isInitialized = true
                console.info("WebGPU context restored successfully")
            }

            // Create capabilities
            rendererCapabilities = createCapabilities(adapter!!)

            isInitialized = true

            val initTime = js("performance.now()").unsafeCast<Double>() - startTime
            console.log("WebGPU renderer initialized in ${initTime}ms")

            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(
                RendererException.InitializationFailed("Renderer initialization failed", e)
            )
        }
    }

    override suspend fun initialize(surface: RenderSurface): RendererResult<Unit> {
        // For WebGPU, surface is the canvas - already provided in constructor
        return initialize()
    }

    override fun render(scene: Scene, camera: Camera): RendererResult<Unit> {
        if (!isInitialized || device == null || context == null) {
            return RendererResult.Error(RendererException.InvalidState("Renderer not initialized"))
        }

        return try {
            triangleCount = 0
            drawCallCount = 0

            // T009: Create frustum for culling
            camera.updateMatrixWorld()
            camera.updateProjectionMatrix()
            val projectionViewMatrix = Matrix4()
                .copy(camera.projectionMatrix)
                .multiply(camera.matrixWorldInverse)

            val frustum = Frustum()
            frustum.setFromMatrix(projectionViewMatrix)

            var culledCount = 0
            var visibleCount = 0

            // Get current texture from swap chain
            val currentTexture = context!!.getCurrentTexture()
            val textureView = currentTexture.createView()

            // Create command encoder
            val commandEncoder = device!!.createCommandEncoder()

            // Create render pass descriptor
            val renderPassDescriptor = js("({})").unsafeCast<GPURenderPassDescriptor>()

            // Color attachment
            val colorAttachment = js("({})").unsafeCast<GPURenderPassColorAttachment>()
            colorAttachment.view = textureView
            colorAttachment.loadOp = if (autoClearColor) "clear" else "load"
            colorAttachment.storeOp = "store"
            if (autoClearColor) {
                val clearValue = js("({})")
                clearValue.r = clearColor.r
                clearValue.g = clearColor.g
                clearValue.b = clearColor.b
                clearValue.a = clearAlpha
                colorAttachment.clearValue = clearValue
            }

            val colorAttachments = js("[]").unsafeCast<Array<GPURenderPassColorAttachment?>>()
            js("colorAttachments.push(colorAttachment)")
            renderPassDescriptor.colorAttachments = colorAttachments

            // Begin render pass
            val renderPass = commandEncoder.beginRenderPass(renderPassDescriptor)

            // T009: Render scene with frustum culling
            scene.traverse { obj ->
                if (obj is Mesh) {
                    // Check if mesh has chunk data for frustum culling
                    val chunk = obj.userData["chunk"]
                    if (chunk != null) {
                        // VoxelCraft chunk - use frustum culling
                        try {
                            // Use reflection-like approach to access boundingBox
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
                    renderMesh(obj, camera, renderPass)
                }
            }

            // End render pass
            renderPass.end()

            // Submit commands
            val commandBuffer = commandEncoder.finish()
            val commandBuffers = js("[]").unsafeCast<Array<GPUCommandBuffer>>()
            js("commandBuffers.push(commandBuffer)")
            device!!.queue.submit(commandBuffers)

            // T009: Log frustum culling statistics
            if (culledCount > 0 || visibleCount > 0) {
                console.log("T009 Frustum culling: $visibleCount visible, $culledCount culled (${culledCount + visibleCount} total)")
            }

            // T010: Validate draw call count (FR-005: <100 draw calls for 81 chunks)
            if (drawCallCount > 100) {
                console.warn("⚠️ T010: Draw call count ($drawCallCount) exceeds limit of 100 (FR-005 requirement)")
            }

            // T010: Log performance metrics
            console.log("T010 Performance: $drawCallCount draw calls, $triangleCount triangles, $visibleCount meshes")

            frameCount++
            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.RenderingFailed("Rendering failed", e))
        }
    }

    private fun renderMesh(mesh: Mesh, camera: Camera, renderPass: GPURenderPassEncoder) {
        val geometry = mesh.geometry
        val material = mesh.material as? MeshBasicMaterial ?: return

        // Get or create buffers for this geometry
        val buffers = getOrCreateGeometryBuffers(geometry)
        if (buffers == null) {
            console.warn("Failed to create buffers for mesh")
            return
        }

        // Get or create pipeline for this material
        val pipeline = getOrCreatePipeline(geometry, material)
        if (pipeline == null) {
            console.warn("Failed to create pipeline for mesh")
            return
        }

        // Update MVP uniform buffer
        updateUniforms(mesh, camera)

        // Bind pipeline
        renderPass.setPipeline(pipeline)

        // Bind vertex buffer
        renderPass.setVertexBuffer(0, buffers.vertexBuffer)

        // Bind uniform buffer (bind group 0)
        bindGroup?.let { renderPass.setBindGroup(0, it) }

        // Draw
        if (buffers.indexBuffer != null && buffers.indexCount > 0) {
            // Indexed draw
            renderPass.setIndexBuffer(buffers.indexBuffer!!, buffers.indexFormat)
            renderPass.drawIndexed(buffers.indexCount, 1, 0, 0, 0)
            triangleCount += buffers.indexCount / 3
        } else {
            // Non-indexed draw
            renderPass.draw(buffers.vertexCount, 1, 0, 0)
            triangleCount += buffers.vertexCount / 3
        }

        drawCallCount++
    }

    private fun createPipelineDescriptor(
        geometry: BufferGeometry,
        material: MeshBasicMaterial
    ): RenderPipelineDescriptor {
        // Create basic pipeline descriptor
        return RenderPipelineDescriptor(
            vertexShader = BasicShaders.vertexShader,
            fragmentShader = BasicShaders.fragmentShader,
            vertexBufferLayout = VertexBufferLayout(
                arrayStride = 36, // 3 floats * 3 attributes * 4 bytes
                attributes = listOf(
                    VertexAttribute(VertexFormat.FLOAT32X3, 0, 0),  // position
                    VertexAttribute(VertexFormat.FLOAT32X3, 12, 1), // normal
                    VertexAttribute(VertexFormat.FLOAT32X3, 24, 2)  // color
                )
            )
        )
    }

    override fun setSize(width: Int, height: Int, updateStyle: Boolean): RendererResult<Unit> {
        canvas.width = width
        canvas.height = height
        viewport = Viewport(0, 0, width, height)
        return RendererResult.Success(Unit)
    }

    override fun setPixelRatio(pixelRatio: Float): RendererResult<Unit> {
        val width = (canvas.clientWidth * pixelRatio).toInt()
        val height = (canvas.clientHeight * pixelRatio).toInt()
        return setSize(width, height, false)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit> {
        viewport = Viewport(x, y, width, height)
        return RendererResult.Success(Unit)
    }

    override fun getViewport(): Viewport = viewport

    override fun setScissorTest(enable: Boolean): RendererResult<Unit> {
        // WebGPU doesn't have scissor test in the same way
        return RendererResult.Success(Unit)
    }

    override fun setScissor(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit> {
        // Would be implemented with viewport/scissor rect
        return RendererResult.Success(Unit)
    }

    override fun clear(color: Boolean, depth: Boolean, stencil: Boolean): RendererResult<Unit> {
        autoClearColor = color
        autoClearDepth = depth
        autoClearStencil = stencil
        return RendererResult.Success(Unit)
    }

    override fun clearColorBuffer(): RendererResult<Unit> {
        return clear(true, false, false)
    }

    override fun clearDepth(): RendererResult<Unit> {
        return clear(false, true, false)
    }

    override fun clearStencil(): RendererResult<Unit> {
        return clear(false, false, true)
    }

    override fun resetState(): RendererResult<Unit> {
        currentPipeline = null
        return RendererResult.Success(Unit)
    }

    override fun compile(scene: Scene, camera: Camera): RendererResult<Unit> {
        // Pre-compile pipelines for all meshes
        return RendererResult.Success(Unit)
    }

    /**
     * Get or create GPU buffers for a geometry.
     */
    private fun getOrCreateGeometryBuffers(geometry: BufferGeometry): GeometryBuffers? {
        val uuid = geometry.uuid

        // Return cached buffers if available
        geometryBuffers[uuid]?.let { return it }

        // Create new buffers
        try {
            val positionAttr = geometry.getAttribute("position")
            if (positionAttr == null) {
                console.error("Position attribute is null for geometry ${geometry.uuid}")
                return null
            }

            val normalAttr = geometry.getAttribute("normal")
            val colorAttr = geometry.getAttribute("color")
            val indexAttr = geometry.index

            // Interleave vertex data: position(3) + normal(3) + color(3) = 9 floats per vertex
            val vertexCount = positionAttr.count as Int
            val vertexData = FloatArray(vertexCount * 9)

            for (i in 0 until vertexCount) {
                val offset = i * 9

                // Position (required)
                vertexData[offset + 0] = positionAttr.getX(i)
                vertexData[offset + 1] = positionAttr.getY(i)
                vertexData[offset + 2] = positionAttr.getZ(i)

                // Normal (optional, default to up vector)
                if (normalAttr != null) {
                    vertexData[offset + 3] = normalAttr.getX(i)
                    vertexData[offset + 4] = normalAttr.getY(i)
                    vertexData[offset + 5] = normalAttr.getZ(i)
                } else {
                    vertexData[offset + 3] = 0f
                    vertexData[offset + 4] = 1f
                    vertexData[offset + 5] = 0f
                }

                // Color (optional, default to white)
                if (colorAttr != null) {
                    vertexData[offset + 6] = colorAttr.getX(i)
                    vertexData[offset + 7] = colorAttr.getY(i)
                    vertexData[offset + 8] = colorAttr.getZ(i)
                } else {
                    vertexData[offset + 6] = 1f
                    vertexData[offset + 7] = 1f
                    vertexData[offset + 8] = 1f
                }
            }

            // Create vertex buffer
            val vertexBuffer = WebGPUBuffer(
                device!!,
                BufferDescriptor(
                    size = vertexData.size * 4, // 4 bytes per float
                    usage = GPUBufferUsage.VERTEX or GPUBufferUsage.COPY_DST,
                    label = "Vertex Buffer ${uuid}"
                )
            )
            vertexBuffer.create()
            vertexBuffer.upload(vertexData)

            // Create index buffer if available
            var indexBuffer: GPUBuffer? = null
            var indexCount = 0
            var indexFormat = "uint32"

            if (indexAttr != null) {
                indexCount = indexAttr.count as Int
                val indexData = IntArray(indexCount) { i ->
                    indexAttr.getX(i).toInt()
                }

                val idxBuffer = WebGPUBuffer(
                    device!!,
                    BufferDescriptor(
                        size = indexData.size * 4, // 4 bytes per uint32
                        usage = GPUBufferUsage.INDEX or GPUBufferUsage.COPY_DST,
                        label = "Index Buffer ${uuid}"
                    )
                )
                idxBuffer.create()
                idxBuffer.uploadIndices(indexData)
                indexBuffer = idxBuffer.getBuffer()
            }

            val buffers = GeometryBuffers(
                vertexBuffer = vertexBuffer.getBuffer()!!,
                indexBuffer = indexBuffer,
                vertexCount = vertexCount,
                indexCount = indexCount,
                indexFormat = indexFormat
            )

            geometryBuffers[uuid] = buffers
            return buffers
        } catch (e: Exception) {
            console.error("Failed to create geometry buffers: ${e.message}")
            return null
        }
    }

    /**
     * Get or create render pipeline for a material.
     * T006: Fixed - No longer blocks render thread with busy-wait.
     * Returns null if pipeline not ready (mesh skipped this frame, will render next frame).
     */
    private fun getOrCreatePipeline(geometry: BufferGeometry, material: MeshBasicMaterial): GPURenderPipeline? {
        val pipelineDescriptor = createPipelineDescriptor(geometry, material)
        val cacheKey = PipelineKey.fromDescriptor(pipelineDescriptor)

        // Synchronous cache lookup - return immediately if ready
        pipelineCacheMap[cacheKey]?.let {
            if (it.isReady) {
                return it.getPipeline()
            }
        }

        // Launch async creation if not exists (non-blocking)
        if (!pipelineCacheMap.containsKey(cacheKey)) {
            val pipeline = WebGPUPipeline(device!!, pipelineDescriptor)
            pipelineCacheMap[cacheKey] = pipeline

            GlobalScope.launch {
                try {
                    val result = pipeline.create()
                    when (result) {
                        is RendererResult.Success<*> -> {
                            console.log("✅ Pipeline ready for key: $cacheKey")
                        }
                        is RendererResult.Error<*> -> {
                            console.error("❌ Pipeline creation failed: ${result.exception.message}")
                            pipelineCacheMap.remove(cacheKey) // Remove failed pipeline
                        }
                    }
                } catch (e: Exception) {
                    console.error("❌ Pipeline creation exception: ${e.message}")
                    pipelineCacheMap.remove(cacheKey)
                }
            }
        }

        // Return pipeline if ready, null otherwise (mesh skipped this frame)
        return pipelineCacheMap[cacheKey]?.takeIf { it.isReady }?.getPipeline()
    }

    /**
     * Update uniform buffer with MVP matrices.
     */
    private fun updateUniforms(mesh: Mesh, camera: Camera) {
        if (uniformBuffer == null) {
            createUniformBuffer()
        }

        // Create MVP matrices (3 x mat4x4<f32> = 3 x 64 bytes = 192 bytes)
        val projMatrix = camera.projectionMatrix.elements
        val viewMatrix = camera.matrixWorldInverse.elements
        val modelMatrix = mesh.matrixWorld.elements

        // Flatten matrices into uniform data
        val uniformData = FloatArray(48) // 3 matrices * 16 floats each

        // Projection matrix
        for (i in 0 until 16) {
            uniformData[i] = projMatrix[i]
        }

        // View matrix
        for (i in 0 until 16) {
            uniformData[16 + i] = viewMatrix[i]
        }

        // Model matrix
        for (i in 0 until 16) {
            uniformData[32 + i] = modelMatrix[i]
        }

        // Upload to GPU
        uniformBuffer?.upload(uniformData)
    }

    /**
     * Create uniform buffer and bind group.
     */
    private fun createUniformBuffer() {
        // Create uniform buffer (3 mat4x4 = 192 bytes, must be 256-aligned)
        uniformBuffer = WebGPUBuffer(
            device!!,
            BufferDescriptor(
                size = 256, // Align to 256 bytes
                usage = GPUBufferUsage.UNIFORM or GPUBufferUsage.COPY_DST,
                label = "Uniform Buffer"
            )
        )
        uniformBuffer!!.create()

        // Create bind group layout
        val bindGroupLayoutDescriptor = js("({})").unsafeCast<GPUBindGroupLayoutDescriptor>()
        bindGroupLayoutDescriptor.label = "Uniform Bind Group Layout"

        val entries = js("[]").unsafeCast<Array<GPUBindGroupLayoutEntry>>()
        val entry = js("({})").unsafeCast<GPUBindGroupLayoutEntry>()
        entry.binding = 0
        entry.visibility = GPUShaderStage.VERTEX
        val bufferLayout = js("({})").unsafeCast<GPUBufferBindingLayout>()
        bufferLayout.type = "uniform"
        entry.buffer = bufferLayout
        js("entries.push(entry)")

        bindGroupLayoutDescriptor.entries = entries
        val bindGroupLayout = device!!.createBindGroupLayout(bindGroupLayoutDescriptor)

        // Create bind group
        val bindGroupDescriptor = js("({})").unsafeCast<GPUBindGroupDescriptor>()
        bindGroupDescriptor.label = "Uniform Bind Group"
        bindGroupDescriptor.layout = bindGroupLayout

        val bindingEntries = js("[]").unsafeCast<Array<GPUBindGroupEntry>>()
        val bindingEntry = js("({})").unsafeCast<GPUBindGroupEntry>()
        bindingEntry.binding = 0
        val bufferBinding = js("({})")
        bufferBinding.buffer = uniformBuffer!!.getBuffer()!!
        bufferBinding.offset = 0
        bufferBinding.size = 192
        bindingEntry.resource = bufferBinding
        js("bindingEntries.push(bindingEntry)")

        bindGroupDescriptor.entries = bindingEntries
        bindGroup = device!!.createBindGroup(bindGroupDescriptor)
    }

    override fun dispose(): RendererResult<Unit> {
        pipelineCache.clear()
        bufferPool.dispose()
        contextLossRecovery.clear()
        device?.destroy()
        device = null
        context = null
        isInitialized = false
        return RendererResult.Success(Unit)
    }

    override fun forceContextLoss(): RendererResult<Unit> {
        contextLossRecovery.handleContextLoss()
        return RendererResult.Success(Unit)
    }

    override fun isContextLost(): Boolean {
        return !isInitialized
    }

    override fun getStats(): RenderStats {
        return RenderStats(
            frame = frameCount,
            calls = drawCallCount,
            triangles = triangleCount,
            points = 0,
            lines = 0,
            geometries = 0,
            textures = 0,
            shaders = pipelineCache.size(),
            programs = pipelineCache.size()
        )
    }

    override fun resetStats() {
        frameCount = 0
        triangleCount = 0
        drawCallCount = 0
    }

    fun simulateContextLoss() {
        forceContextLoss()
    }

    private fun createCapabilities(adapter: GPUAdapter): RendererCapabilities {
        val limits = adapter.limits
        return RendererCapabilities(
            maxTextureSize = limits.maxTextureDimension2D,
            maxCubeMapSize = limits.maxTextureDimension2D,
            maxVertexAttributes = limits.maxVertexAttributes,
            maxVertexUniforms = limits.maxUniformBufferBindingSize / 16,
            maxFragmentUniforms = limits.maxUniformBufferBindingSize / 16
        )
    }

    private fun createDefaultCapabilities(): RendererCapabilities {
        return RendererCapabilities(
            maxTextureSize = 8192,
            maxCubeMapSize = 8192,
            maxVertexAttributes = 16,
            maxVertexUniforms = 4096,
            maxFragmentUniforms = 4096
        )
    }
}

/**
 * Cached geometry buffers for rendering.
 */
private data class GeometryBuffers(
    val vertexBuffer: GPUBuffer,
    val indexBuffer: GPUBuffer?,
    val vertexCount: Int,
    val indexCount: Int,
    val indexFormat: String
)

