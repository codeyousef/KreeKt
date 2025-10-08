package io.kreekt.renderer.webgpu

import io.kreekt.camera.Camera
import io.kreekt.camera.Viewport
import io.kreekt.core.math.Color
import io.kreekt.core.math.Matrix4
import io.kreekt.core.scene.Mesh
import io.kreekt.core.scene.Scene
import io.kreekt.geometry.BufferGeometry
import io.kreekt.material.MeshBasicMaterial
import io.kreekt.optimization.BoundingBox
import io.kreekt.optimization.Frustum
import io.kreekt.renderer.*
import io.kreekt.renderer.webgpu.shaders.BasicShaders
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLCanvasElement
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

/**
 * Await a JavaScript Promise in a suspend function.
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

    // Feature 020 Managers (T020)
    private var bufferManager: WebGPUBufferManager? = null
    private var renderPassManager: WebGPURenderPassManager? = null

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

    // Bind groups cached per pipeline (since each pipeline has its own layout when using "auto")
    private val bindGroupCache = mutableMapOf<GPURenderPipeline, GPUBindGroup>()

    // Capabilities
    private var rendererCapabilities: RendererCapabilities? = null

    // Viewport
    private var viewport = Viewport(0, 0, canvas.width, canvas.height)

    // T033: Debug flag for verbose frame logging (default off to avoid spam)
    var enableFrameLogging: Boolean = false

    // Renderer interface properties
    override val backend: BackendType = BackendType.WEBGPU

    override val capabilities: RendererCapabilities
        get() = rendererCapabilities ?: createDefaultCapabilities()

    override val stats: RenderStats
        get() = RenderStats(
            fps = 0.0, // TODO: Calculate actual FPS
            frameTime = 0.0, // TODO: Calculate actual frame time
            triangles = triangleCount,
            drawCalls = drawCallCount,
            textureMemory = 0, // TODO: Track texture memory
            bufferMemory = 0 // TODO: Track buffer memory
        )

    // Old Three.js-style properties removed - not part of Feature 020 Renderer interface
    // These will be restored in advanced features phase (Phase 2-13)
    var clearColor: Color = Color(0x000000)
    var clearAlpha: Float = 1f

    var isInitialized: Boolean = false
        private set

    val isWebGPU: Boolean = true


    override suspend fun initialize(config: RendererConfig): io.kreekt.core.Result<Unit> {
        // For WebGPU, surface is the canvas - already provided in constructor
        return initializeInternal()
    }

    private suspend fun initializeInternal(): io.kreekt.core.Result<Unit> {
        return try {
            console.log("T033: Starting WebGPU renderer initialization...")
            val startTime = js("performance.now()").unsafeCast<Double>()

            // Get GPU
            console.log("T033: Getting GPU interface...")
            val gpu = WebGPUDetector.getGPU()
            if (gpu == null) {
                console.error("T033: WebGPU not available in this browser")
                return io.kreekt.core.Result.Error(
                    "WebGPU not available",
                    RuntimeException("WebGPU not available")
                )
            }
            console.log("T033: GPU interface obtained")

            // Request adapter
            console.log("T033: Requesting GPU adapter (powerPreference=high-performance)...")
            val adapterOptions = js("({})").unsafeCast<GPURequestAdapterOptions>()
            adapterOptions.powerPreference = "high-performance"

            val adapterPromise = gpu.requestAdapter(adapterOptions).unsafeCast<Promise<GPUAdapter?>>()
            val adapterResult = adapterPromise.awaitPromise()
            if (adapterResult == null) {
                console.error("T033: Failed to request GPU adapter")
                return io.kreekt.core.Result.Error(
                    "Failed to request GPU adapter",
                    RuntimeException("Failed to request GPU adapter")
                )
            }
            adapter = adapterResult
            console.log("T033: GPU adapter obtained")

            // Request device
            console.log("T033: Requesting GPU device...")
            val deviceDescriptor = js("({})").unsafeCast<GPUDeviceDescriptor>()
            deviceDescriptor.label = "KreeKt WebGPU Device"

            val devicePromise = adapter!!.requestDevice(deviceDescriptor).unsafeCast<Promise<GPUDevice>>()
            device = devicePromise.awaitPromise()
            console.log("T033: GPU device created successfully")

            // Monitor device loss
            console.log("T033: Setting up device loss monitoring...")
            device!!.lost.then { info ->
                try {
                    console.warn("T033: WebGPU device lost: ${info}")
                    contextLossRecovery.handleContextLoss()
                } catch (e: Exception) {
                    console.error("T033: Error monitoring device loss: ${e.message}")
                }
            }

            // Configure canvas context
            console.log("T033: Configuring canvas context...")
            context = canvas.getContext("webgpu").unsafeCast<GPUCanvasContext?>()
                ?: return io.kreekt.core.Result.Error(
                    "Failed to get WebGPU context from canvas",
                    RuntimeException("Failed to get WebGPU context")
                )

            val contextConfig = js("({})").unsafeCast<GPUCanvasConfiguration>()
            contextConfig.device = device!!
            contextConfig.format = "bgra8unorm"
            contextConfig.alphaMode = "opaque"
            context!!.configure(contextConfig)
            console.log("T033: Canvas context configured (format=bgra8unorm, alphaMode=opaque)")

            // Create buffer pool
            console.log("T033: Creating buffer pool...")
            bufferPool = BufferPool(device!!)
            console.log("T033: Buffer pool created")

            // T020: Initialize Feature 020 Managers
            console.log("T033: Initializing BufferManager...")
            bufferManager = WebGPUBufferManager(device!!)
            console.log("T033: BufferManager initialized")
            // Note: RenderPassManager is initialized per-frame with command encoder

            // Create capabilities
            console.log("T033: Querying device capabilities...")
            rendererCapabilities = createCapabilities(adapter!!)
            console.log("T033: Capabilities detected: maxTextureSize=${rendererCapabilities!!.maxTextureSize}, maxVertexAttributes=${rendererCapabilities!!.maxVertexAttributes}")

            isInitialized = true

            val initTime = js("performance.now()").unsafeCast<Double>() - startTime
            console.log("T033: WebGPU renderer initialization completed in ${initTime}ms")

            io.kreekt.core.Result.Success(Unit)
        } catch (e: Exception) {
            console.error("T033: ERROR during initialization: ${e.message}")
            console.error("T033: Stack trace: ${e.stackTraceToString()}")
            io.kreekt.core.Result.Error(
                "Renderer initialization failed at stage: ${e.message}",
                e
            )
        }
    }

    override fun resize(width: Int, height: Int) {
        setSize(width, height, false)
    }

    override fun render(scene: Scene, camera: Camera) {
        if (!isInitialized || device == null || context == null) {
            console.error("T033: Renderer not initialized, cannot render")
            return
        }

        if (enableFrameLogging) {
            console.log("T033: [Frame $frameCount] Starting render...")
        }

        try {
            triangleCount = 0
            drawCallCount = 0

            // T009: Create frustum for culling
            if (enableFrameLogging) console.log("T033: [Frame $frameCount] - Updating camera matrices...")
            camera.updateMatrixWorld()
            camera.updateProjectionMatrix()
            val projectionViewMatrix = Matrix4()
                .copy(camera.projectionMatrix)
                .multiply(camera.matrixWorldInverse)

            if (enableFrameLogging) console.log("T033: [Frame $frameCount] - Creating frustum for culling...")
            val frustum = Frustum()
            frustum.setFromMatrix(projectionViewMatrix)

            var culledCount = 0
            var visibleCount = 0

            // Get current texture from swap chain
            if (enableFrameLogging) console.log("T033: [Frame $frameCount] - Getting current texture from swap chain...")
            val currentTexture = context!!.getCurrentTexture()
            val textureView = currentTexture.createView()

            // Create command encoder
            if (enableFrameLogging) console.log("T033: [Frame $frameCount] - Creating command encoder...")
            val commandEncoder = device!!.createCommandEncoder()

            // T020: Initialize RenderPassManager for this frame
            if (enableFrameLogging) console.log("T033: [Frame $frameCount] - Initializing RenderPassManager...")
            renderPassManager = WebGPURenderPassManager(commandEncoder)

            // T020: Begin render pass using manager
            if (enableFrameLogging) console.log("T033: [Frame $frameCount] - Beginning render pass (clearColor=[${clearColor.r}, ${clearColor.g}, ${clearColor.b}])...")
            val framebufferHandle = io.kreekt.renderer.feature020.FramebufferHandle(textureView)
            val clearColorFeature020 = io.kreekt.renderer.feature020.Color(
                clearColor.r,
                clearColor.g,
                clearColor.b,
                clearAlpha
            )
            renderPassManager!!.beginRenderPass(clearColorFeature020, framebufferHandle)

            // Get the internal render pass encoder for legacy rendering code
            val renderPass = (renderPassManager as WebGPURenderPassManager).getPassEncoder().unsafeCast<GPURenderPassEncoder>()

            // T009: Render scene with frustum culling
            if (enableFrameLogging) console.log("T033: [Frame $frameCount] - Traversing scene graph and rendering meshes...")
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

            // T020: End render pass using manager
            if (enableFrameLogging) console.log("T033: [Frame $frameCount] - Ending render pass...")
            renderPassManager!!.endRenderPass()

            // Submit commands
            if (enableFrameLogging) console.log("T033: [Frame $frameCount] - Finishing command encoder...")
            val commandBuffer = commandEncoder.finish()
            val commandBuffers = js("[]").unsafeCast<Array<GPUCommandBuffer>>()
            js("commandBuffers.push(commandBuffer)")
            if (enableFrameLogging) console.log("T033: [Frame $frameCount] - Submitting command buffer to GPU...")
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

            if (enableFrameLogging) {
                console.log("T033: [Frame $frameCount] Render completed successfully")
            }

            frameCount++
        } catch (e: Exception) {
            console.error("T033: ERROR during rendering frame $frameCount: ${e.message}")
            console.error("T033: Stack trace: ${e.stackTraceToString()}")
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

        // Bind uniform buffer (bind group 0) - get bind group for this pipeline
        val bindGroup = getOrCreateBindGroup(pipeline)
        renderPass.setBindGroup(0, bindGroup)

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
        // T021: Temporarily disable backface culling to debug black screen
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
            ),
            cullMode = CullMode.NONE  // T021: Disable culling to test winding order issue
        )
    }

    /**
     * Internal method to resize the canvas.
     * Called by RendererFactory's resize() implementation.
     */
    fun setSize(width: Int, height: Int, updateStyle: Boolean) {
        canvas.width = width
        canvas.height = height
        viewport = Viewport(0, 0, width, height)
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

                    // T021: Diagnostic - log first vertex color to verify values
                    if (i == 0) {
                        console.log("T021 First vertex color: (${vertexData[offset + 6]}, ${vertexData[offset + 7]}, ${vertexData[offset + 8]})")
                    }
                } else {
                    vertexData[offset + 6] = 1f
                    vertexData[offset + 7] = 1f
                    vertexData[offset + 8] = 1f
                    console.warn("T021 No color attribute - using white default")
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

        // Create pipeline synchronously if not exists
        if (!pipelineCacheMap.containsKey(cacheKey)) {
            console.log("🆕 Creating new pipeline for key: $cacheKey")
            val pipeline = WebGPUPipeline(device!!, pipelineDescriptor)
            pipelineCacheMap[cacheKey] = pipeline

            try {
                val result = pipeline.create()
                when (result) {
                    is io.kreekt.core.Result.Success -> {
                        console.log("✅ Pipeline ready for key: $cacheKey, isReady=${pipeline.isReady}, pipeline=${pipeline.getPipeline()}")
                    }

                    is io.kreekt.core.Result.Error -> {
                        console.error("❌ Pipeline creation failed: ${result.message}")
                        pipelineCacheMap.remove(cacheKey) // Remove failed pipeline
                        return null
                    }
                }
            } catch (e: Exception) {
                console.error("❌ Pipeline creation exception: ${e.message}")
                pipelineCacheMap.remove(cacheKey)
                return null
            }
        }

        // Return pipeline
        return pipelineCacheMap[cacheKey]?.getPipeline()
    }

    /**
     * Update uniform buffer with MVP matrices.
     */
    private fun updateUniforms(mesh: Mesh, camera: Camera) {
        // T021: Always log for first few calls to diagnose issue
        if (frameCount < 50 && drawCallCount == 0) {
            console.log("T021 Frame $frameCount: updateUniforms called, drawCallCount=$drawCallCount")
        }

        if (uniformBuffer == null) {
            createUniformBuffer()
        }

        // Create MVP matrices (3 x mat4x4<f32> = 3 x 64 bytes = 192 bytes)
        val projMatrix = camera.projectionMatrix.elements
        val viewMatrix = camera.matrixWorldInverse.elements
        val modelMatrix = mesh.matrixWorld.elements

        // T021: Diagnostic - log matrices for first mesh of first frame only
        if (frameCount == 46 && drawCallCount == 0) {
            console.log("T021 Projection matrix[0..3]: [${projMatrix[0]}, ${projMatrix[1]}, ${projMatrix[2]}, ${projMatrix[3]}]")
            console.log("T021 View matrix[0..3]: [${viewMatrix[0]}, ${viewMatrix[1]}, ${viewMatrix[2]}, ${viewMatrix[3]}]")
            console.log("T021 Model matrix[0..3]: [${modelMatrix[0]}, ${modelMatrix[1]}, ${modelMatrix[2]}, ${modelMatrix[3]}]")
            console.log("T021 Camera position: (${camera.position.x}, ${camera.position.y}, ${camera.position.z})")
            console.log("T021 Camera rotation: (${camera.rotation.x}, ${camera.rotation.y}, ${camera.rotation.z})")
        }

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
     * Create uniform buffer.
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
    }

    /**
     * Get or create a bind group for the given pipeline.
     * Each pipeline created with "auto" layout has its own bind group layout,
     * so we need to create a compatible bind group for each pipeline.
     */
    private fun getOrCreateBindGroup(pipeline: GPURenderPipeline): GPUBindGroup {
        // Return cached bind group if exists
        bindGroupCache[pipeline]?.let { return it }

        // Create new bind group using the pipeline's bind group layout
        val bindGroupLayout = pipeline.asDynamic().getBindGroupLayout(0)

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
        val bindGroup = device!!.createBindGroup(bindGroupDescriptor)

        // Cache and return
        bindGroupCache[pipeline] = bindGroup
        return bindGroup
    }

    override fun dispose() {
        if (!isInitialized) {
            console.log("T033: Dispose called but renderer not initialized, skipping")
            return
        }

        console.log("T033: Starting WebGPU renderer disposal...")

        console.log("T033: Clearing pipeline cache...")
        pipelineCache.clear()
        console.log("T033: Pipeline cache cleared")

        console.log("T033: Disposing buffer pool...")
        bufferPool.dispose()
        console.log("T033: Buffer pool disposed")

        console.log("T033: Clearing context loss recovery...")
        contextLossRecovery.clear()
        console.log("T033: Context loss recovery cleared")

        // T020: Clean up Feature 020 managers
        console.log("T033: Cleaning up BufferManager...")
        // Note: BufferManager doesn't have a dispose method - buffers are cleaned up when device is destroyed
        bufferManager = null
        console.log("T033: BufferManager cleaned up")

        console.log("T033: Cleaning up RenderPassManager...")
        renderPassManager = null
        console.log("T033: RenderPassManager cleaned up")

        if (device != null) {
            console.log("T033: Destroying GPU device...")
            device?.asDynamic()?.destroy()
            device = null
            console.log("T033: GPU device destroyed")
        }

        console.log("T033: Releasing canvas context...")
        context = null
        console.log("T033: Canvas context released")

        isInitialized = false
        console.log("T033: WebGPU renderer disposal completed")
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

    // ========================================================================
    // T020: Feature 020 BufferManager Integration Helper Methods
    // ========================================================================

    /**
     * Create vertex buffer using Feature 020 BufferManager.
     *
     * This method demonstrates how to use the new BufferManager API
     * for simple vertex data (position + color, 6 floats per vertex).
     *
     * The existing getOrCreateGeometryBuffers() uses a more complex format
     * (position + normal + color, 9 floats per vertex) and will be migrated
     * to use BufferManager in a future refactoring.
     *
     * @param vertices Interleaved vertex data [x, y, z, r, g, b, ...]
     * @return BufferHandle for the created vertex buffer
     */
    private fun createVertexBufferViaManager(vertices: FloatArray): io.kreekt.renderer.feature020.BufferHandle {
        return bufferManager!!.createVertexBuffer(vertices)
    }

    /**
     * Create index buffer using Feature 020 BufferManager.
     *
     * @param indices Triangle indices (must be multiple of 3)
     * @return BufferHandle for the created index buffer
     */
    private fun createIndexBufferViaManager(indices: IntArray): io.kreekt.renderer.feature020.BufferHandle {
        return bufferManager!!.createIndexBuffer(indices)
    }

    /**
     * Create uniform buffer using Feature 020 BufferManager.
     *
     * @param sizeBytes Buffer size in bytes (minimum 64 for mat4x4)
     * @return BufferHandle for the created uniform buffer
     */
    private fun createUniformBufferViaManager(sizeBytes: Int): io.kreekt.renderer.feature020.BufferHandle {
        return bufferManager!!.createUniformBuffer(sizeBytes)
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

