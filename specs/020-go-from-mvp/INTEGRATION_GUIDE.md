# Feature 020: Integration Guide

**Feature**: Production-Ready Renderer with Full VoxelCraft Integration
**Date**: 2025-10-07
**Purpose**: Step-by-step guide for integrating Feature 020 managers into existing renderers

---

## Overview

This guide provides detailed instructions for completing T017-T035, integrating the Feature 020 buffer, render pass, and
swapchain managers into the existing VulkanRenderer and WebGPURenderer implementations.

---

## Phase 3.4: Renderer Integration (T017-T020)

### T017: Integrate VulkanRenderer with Managers

**File**: `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanRenderer.kt`

#### Step 1: Add Manager Fields

Add these fields to the VulkanRenderer class:

```kotlin
import io.kreekt.renderer.vulkan.VulkanBufferManager
import io.kreekt.renderer.vulkan.VulkanRenderPassManager
import io.kreekt.renderer.vulkan.VulkanSwapchain
import io.kreekt.renderer.feature020.*

class VulkanRenderer(
    private val surface: RenderSurface,
    private val config: RendererConfig
) : Renderer {

    // Existing fields...
    private var device: VkDevice? = null
    private var physicalDevice: VkPhysicalDevice? = null

    // NEW: Feature 020 managers
    private var bufferManager: VulkanBufferManager? = null
    private var renderPassManager: VulkanRenderPassManager? = null
    private var swapchainManager: VulkanSwapchain? = null
    private var renderPass: Long = VK_NULL_HANDLE // VkRenderPass
    private var framebuffers: List<Long> = emptyList() // VkFramebuffer list
```

#### Step 2: Initialize Managers in initialize()

In the `initialize()` method, after device creation:

```kotlin
override suspend fun initialize(config: RendererConfig): Result<Unit, RendererError> {
    // ... existing initialization code ...

    // Create render pass (after device creation)
    renderPass = createRenderPass()

    // Initialize Feature 020 managers
    val vkSurface = (surface as VulkanSurface).surfaceHandle

    bufferManager = VulkanBufferManager(
        device = device!!,
        physicalDevice = physicalDevice!!
    )

    swapchainManager = VulkanSwapchain(
        device = device!!,
        physicalDevice = physicalDevice!!,
        surface = vkSurface
    )

    // Create framebuffers for swapchain images
    framebuffers = createFramebuffers(swapchainManager!!)

    renderPassManager = VulkanRenderPassManager(
        device = device!!,
        commandBuffer = commandBuffer!!,
        renderPass = renderPass
    )

    initialized = true
    Result.success(Unit)
}
```

#### Step 3: Create Helper Methods

Add these helper methods to VulkanRenderer:

```kotlin
/**
 * Create VkRenderPass for color attachment.
 */
private fun createRenderPass(): Long {
    MemoryStack.stackPush().use { stack ->
        // Color attachment
        val attachment = VkAttachmentDescription.calloc(1, stack)
            .format(VK_FORMAT_B8G8R8A8_UNORM)
            .samples(VK_SAMPLE_COUNT_1_BIT)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)

        // Attachment reference
        val colorRef = VkAttachmentReference.calloc(1, stack)
            .attachment(0)
            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

        // Subpass
        val subpass = VkSubpassDescription.calloc(1, stack)
            .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            .colorAttachmentCount(1)
            .pColorAttachments(colorRef)

        // Render pass create info
        val renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
            .pAttachments(attachment)
            .pSubpasses(subpass)

        val pRenderPass = stack.mallocLong(1)
        val result = vkCreateRenderPass(device, renderPassInfo, null, pRenderPass)

        if (result != VK_SUCCESS) {
            throw RendererInitializationException.DeviceCreationFailedException(
                BackendType.VULKAN,
                "Unknown",
                "Failed to create render pass: VkResult=$result"
            )
        }

        return pRenderPass.get(0)
    }
}

/**
 * Create framebuffers for each swapchain image.
 */
private fun createFramebuffers(swapchain: VulkanSwapchain): List<Long> {
    val framebuffers = mutableListOf<Long>()
    val extent = swapchain.getExtent()

    // Get swapchain images (you'll need to expose this from VulkanSwapchain)
    val swapchainImages = getSwapchainImages(swapchain)

    MemoryStack.stackPush().use { stack ->
        swapchainImages.forEach { imageView ->
            val attachments = stack.mallocLong(1).put(0, imageView)

            val framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .renderPass(renderPass)
                .attachmentCount(1)
                .pAttachments(attachments)
                .width(extent.first)
                .height(extent.second)
                .layers(1)

            val pFramebuffer = stack.mallocLong(1)
            val result = vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer)

            if (result != VK_SUCCESS) {
                throw RendererInitializationException.DeviceCreationFailedException(
                    BackendType.VULKAN,
                    "Unknown",
                    "Failed to create framebuffer: VkResult=$result"
                )
            }

            framebuffers.add(pFramebuffer.get(0))
        }
    }

    return framebuffers
}

/**
 * Get swapchain image views.
 * NOTE: You'll need to expose swapchainImages from VulkanSwapchain
 * or create image views here from the swapchain images.
 */
private fun getSwapchainImages(swapchain: VulkanSwapchain): List<Long> {
    // This requires VulkanSwapchain to expose its image list
    // or create image views from VkImage handles
    // For now, return empty list as placeholder
    return emptyList()
}
```

#### Step 4: Implement render() Method

Replace the stub render() implementation:

```kotlin
override fun render(scene: Scene, camera: Camera) {
    if (!initialized) {
        throw IllegalStateException("Renderer not initialized. Call initialize() first.")
    }

    val frameTime = measureTimeMillis {
        // 1. Acquire swapchain image
        val swapchainImage = swapchainManager!!.acquireNextImage()
        val framebuffer = framebuffers[swapchainImage.index]

        // 2. Begin command buffer recording
        MemoryStack.stackPush().use { stack ->
            val beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

            vkBeginCommandBuffer(commandBuffer, beginInfo)
        }

        // 3. Begin render pass
        val clearColor = Color(
            r = clearColor.r,
            g = clearColor.g,
            b = clearColor.b,
            a = clearAlpha
        )
        val framebufferHandle = FramebufferHandle(framebuffer)
        renderPassManager!!.beginRenderPass(clearColor, framebufferHandle)

        // 4. Bind pipeline (you'll need to have a pipeline created)
        // val pipelineHandle = PipelineHandle(basicPipeline)
        // renderPassManager!!.bindPipeline(pipelineHandle)

        // 5. Render scene objects
        scene.traverseVisible { obj ->
            if (obj is Mesh) {
                renderMesh(obj, camera)
            }
        }

        // 6. End render pass
        renderPassManager!!.endRenderPass()

        // 7. End command buffer
        vkEndCommandBuffer(commandBuffer)

        // 8. Submit command buffer
        MemoryStack.stackPush().use { stack ->
            val submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(stack.pointers(commandBuffer))

            vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE)
            vkQueueWaitIdle(graphicsQueue) // TODO: Use fences instead
        }

        // 9. Present swapchain
        swapchainManager!!.presentImage(swapchainImage)

        frameCount++
    }

    updateStats(frameTime)
}

/**
 * Render a single mesh.
 */
private fun renderMesh(mesh: Mesh, camera: Camera) {
    // Get or create buffers for this mesh
    val geometry = mesh.geometry as? BufferGeometry ?: return

    // Create buffers if not cached
    if (!meshBuffers.containsKey(mesh.uuid)) {
        val vertices = geometry.getAttribute("position")?.array as? FloatArray ?: return
        val indices = geometry.index?.array as? IntArray ?: return

        val vertexBuffer = bufferManager!!.createVertexBuffer(vertices)
        val indexBuffer = bufferManager!!.createIndexBuffer(indices)
        val uniformBuffer = bufferManager!!.createUniformBuffer(64) // mat4x4

        meshBuffers[mesh.uuid] = MeshBuffers(vertexBuffer, indexBuffer, uniformBuffer)
    }

    val buffers = meshBuffers[mesh.uuid]!!

    // Update uniform buffer with MVP matrix
    val mvp = calculateMVP(mesh, camera)
    val mvpBytes = mvpToByteArray(mvp)
    bufferManager!!.updateUniformBuffer(buffers.uniformBuffer, mvpBytes)

    // Bind buffers
    renderPassManager!!.bindVertexBuffer(buffers.vertexBuffer, slot = 0)
    renderPassManager!!.bindIndexBuffer(buffers.indexBuffer)
    renderPassManager!!.bindUniformBuffer(buffers.uniformBuffer, group = 0, binding = 0)

    // Draw
    val indexCount = (geometry.index?.array as? IntArray)?.size ?: 0
    renderPassManager!!.drawIndexed(indexCount)
}

// Helper data class
private data class MeshBuffers(
    val vertexBuffer: BufferHandle,
    val indexBuffer: BufferHandle,
    val uniformBuffer: BufferHandle
)

// Cache for mesh buffers
private val meshBuffers = mutableMapOf<String, MeshBuffers>()
```

#### Step 5: Update dispose()

Clean up managers in dispose():

```kotlin
override fun dispose() {
    if (!initialized) return

    // Wait for device to finish
    device?.let { vkDeviceWaitIdle(it) }

    // Destroy mesh buffers
    meshBuffers.values.forEach { buffers ->
        bufferManager?.destroyBuffer(buffers.vertexBuffer)
        bufferManager?.destroyBuffer(buffers.indexBuffer)
        bufferManager?.destroyBuffer(buffers.uniformBuffer)
    }
    meshBuffers.clear()

    // Dispose managers
    swapchainManager?.dispose()

    // Destroy framebuffers
    framebuffers.forEach { framebuffer ->
        vkDestroyFramebuffer(device, framebuffer, null)
    }
    framebuffers = emptyList()

    // Destroy render pass
    if (renderPass != VK_NULL_HANDLE) {
        vkDestroyRenderPass(device, renderPass, null)
        renderPass = VK_NULL_HANDLE
    }

    // ... existing dispose code ...
}
```

### T018: Integrate WebGPURenderer with Managers

**File**: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt`

Follow a similar pattern to VulkanRenderer:

#### Step 1: Add Manager Fields

```kotlin
import io.kreekt.renderer.webgpu.WebGPUBufferManager
import io.kreekt.renderer.webgpu.WebGPURenderPassManager
import io.kreekt.renderer.webgpu.WebGPUSwapchain
import io.kreekt.renderer.feature020.*

class WebGPURenderer(private val canvas: HTMLCanvasElement) : Renderer {

    // Existing fields...
    private var device: GPUDevice? = null
    private var context: GPUCanvasContext? = null

    // NEW: Feature 020 managers
    private var bufferManager: WebGPUBufferManager? = null
    private var renderPassManager: WebGPURenderPassManager? = null
    private var swapchainManager: WebGPUSwapchain? = null
```

#### Step 2: Initialize Managers

```kotlin
override suspend fun initialize(config: RendererConfig): Result<Unit, RendererError> {
    // ... existing initialization ...

    // Initialize Feature 020 managers
    bufferManager = WebGPUBufferManager(device = device!!)

    swapchainManager = WebGPUSwapchain(
        context = context!!,
        device = device!!
    )

    // Note: renderPassManager is created per-frame with command encoder

    isInitialized = true
    Result.success(Unit)
}
```

#### Step 3: Implement render() Method

```kotlin
override fun render(scene: Scene, camera: Camera) {
    if (!isInitialized) {
        throw IllegalStateException("Renderer not initialized")
    }

    // 1. Acquire swapchain image
    val swapchainImage = swapchainManager!!.acquireNextImage()

    // 2. Create command encoder
    val commandEncoder = device!!.createCommandEncoder()

    // 3. Create render pass manager
    renderPassManager = WebGPURenderPassManager(commandEncoder = commandEncoder)

    // 4. Begin render pass
    val clearColor = Color(
        r = clearColor.r.toFloat(),
        g = clearColor.g.toFloat(),
        b = clearColor.b.toFloat(),
        a = clearAlpha
    )
    val framebufferHandle = FramebufferHandle(swapchainImage.handle)
    renderPassManager!!.beginRenderPass(clearColor, framebufferHandle)

    // 5. Bind pipeline (if you have one)
    // renderPassManager!!.bindPipeline(pipelineHandle)

    // 6. Render scene objects
    scene.traverse { obj ->
        if (obj is Mesh) {
            renderMesh(obj, camera)
        }
    }

    // 7. End render pass
    renderPassManager!!.endRenderPass()

    // 8. Submit command buffer (auto-presents)
    val commandBuffer = commandEncoder.finish()
    device!!.queue.submit(arrayOf(commandBuffer))

    // 9. Present (implicit in WebGPU)
    swapchainManager!!.presentImage(swapchainImage)

    frameCount++
}

private fun renderMesh(mesh: Mesh, camera: Camera) {
    // Similar to Vulkan implementation
    // Get/create buffers, update uniforms, bind, draw
}
```

### T019: Implement VulkanRenderer.resize()

**File**: `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanRenderer.kt`

```kotlin
override fun resize(width: Int, height: Int) {
    if (!initialized) return

    // Wait for device to finish
    device?.let { vkDeviceWaitIdle(it) }

    // Destroy old framebuffers
    framebuffers.forEach { framebuffer ->
        vkDestroyFramebuffer(device, framebuffer, null)
    }

    // Recreate swapchain
    swapchainManager?.recreateSwapchain(width, height)

    // Recreate framebuffers
    framebuffers = createFramebuffers(swapchainManager!!)

    // Update viewport (handled by render pass manager)
}
```

### T020: Implement WebGPURenderer.resize()

**File**: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt`

```kotlin
override fun resize(width: Int, height: Int) {
    if (!isInitialized) return

    // Update canvas size
    canvas.width = width
    canvas.height = height

    // Recreate swapchain (reconfigures context)
    swapchainManager?.recreateSwapchain(width, height)

    // Update viewport
    viewport = Viewport(0, 0, width, height)
}
```

---

## Phase 3.5: VoxelCraft Migration (T021-T024)

### T021: Update VoxelWorld to Use BufferManager

**File**: `examples/voxelcraft/src/commonMain/kotlin/io/kreekt/examples/voxelcraft/VoxelWorld.kt`

#### Add BufferManager Parameter

```kotlin
import io.kreekt.renderer.feature020.BufferManager
import io.kreekt.renderer.feature020.BufferHandle

class VoxelWorld(
    private val bufferManager: BufferManager, // NEW
    private val logger: Logger
) {
    // Chunk storage
    private val renderableChunks = mutableMapOf<ChunkPosition, RenderableChunk>()

    // NEW: Renderable chunk data class
    data class RenderableChunk(
        val vertexBuffer: BufferHandle,
        val indexBuffer: BufferHandle,
        val uniformBuffer: BufferHandle,
        val triangleCount: Int
    )
```

#### Update uploadChunk() Method

```kotlin
fun uploadChunk(chunk: Chunk) {
    // 1. Generate mesh
    val meshData = ChunkMeshGenerator.generateMesh(chunk)

    if (meshData.vertices.isEmpty() || meshData.indices.isEmpty()) {
        // Empty chunk, remove if exists
        renderableChunks[chunk.position]?.let { renderable ->
            bufferManager.destroyBuffer(renderable.vertexBuffer)
            bufferManager.destroyBuffer(renderable.indexBuffer)
            bufferManager.destroyBuffer(renderable.uniformBuffer)
        }
        renderableChunks.remove(chunk.position)
        return
    }

    // 2. Create buffers
    val vertexBuffer = bufferManager.createVertexBuffer(meshData.vertices)
    val indexBuffer = bufferManager.createIndexBuffer(meshData.indices)
    val uniformBuffer = bufferManager.createUniformBuffer(64) // mat4x4

    // 3. Store renderable chunk
    val triangleCount = meshData.indices.size / 3
    val renderable = RenderableChunk(vertexBuffer, indexBuffer, uniformBuffer, triangleCount)

    // Destroy old buffers if exists
    renderableChunks[chunk.position]?.let { old ->
        bufferManager.destroyBuffer(old.vertexBuffer)
        bufferManager.destroyBuffer(old.indexBuffer)
        bufferManager.destroyBuffer(old.uniformBuffer)
    }

    renderableChunks[chunk.position] = renderable

    logger.debug("Uploaded chunk ${chunk.position}: $triangleCount triangles")
}
```

#### Add renderChunks() Method

```kotlin
fun renderChunks(
    renderPassManager: RenderPassManager,
    camera: Camera,
    chunkPositions: List<ChunkPosition>
) {
    chunkPositions.forEach { position ->
        val renderable = renderableChunks[position] ?: return@forEach

        // Update model matrix for this chunk
        val modelMatrix = calculateChunkModelMatrix(position)
        val mvpMatrix = camera.projectionMatrix * camera.viewMatrix * modelMatrix
        val mvpBytes = mvpMatrix.toByteArray()

        bufferManager.updateUniformBuffer(renderable.uniformBuffer, mvpBytes)

        // Bind and draw
        renderPassManager.bindVertexBuffer(renderable.vertexBuffer, slot = 0)
        renderPassManager.bindIndexBuffer(renderable.indexBuffer)
        renderPassManager.bindUniformBuffer(renderable.uniformBuffer, group = 0, binding = 0)
        renderPassManager.drawIndexed(renderable.triangleCount * 3)
    }
}
```

### T022: Migrate JS Main.kt from WebGL to WebGPU

**File**: `examples/voxelcraft/src/jsMain/kotlin/Main.kt`

```kotlin
import io.kreekt.renderer.webgpu.WebGPURenderer
import io.kreekt.renderer.webgpu.WebGPUBufferManager
import io.kreekt.examples.voxelcraft.VoxelWorld

suspend fun main() {
    println("[VoxelCraft] Starting JS version with WebGPU...")

    // Get canvas
    val canvas = document.getElementById("canvas") as HTMLCanvasElement

    // Create WebGPU renderer
    val renderer = WebGPURenderer(canvas)
    val result = renderer.initialize(RendererConfig(
        enableValidation = true,
        preferredBackend = BackendType.WEBGPU // Force WebGPU
    ))

    if (result is Result.Error) {
        console.error("Failed to initialize WebGPU renderer: ${result.error}")
        return
    }

    println("[VoxelCraft] WebGPU renderer initialized")

    // Create buffer manager from renderer's device
    val device = renderer.device // You'll need to expose this
    val bufferManager = WebGPUBufferManager(device)

    // Create voxel world
    val logger = ConsoleLogger()
    val world = VoxelWorld(bufferManager, logger)

    // Generate initial chunks
    world.generateTerrain()

    // Game loop
    var lastTime = Date.now()
    fun gameLoop() {
        val currentTime = Date.now()
        val deltaTime = (currentTime - lastTime) / 1000.0
        lastTime = currentTime

        // Update game logic
        world.update(deltaTime)

        // Render
        renderer.render(world.scene, world.camera)

        window.requestAnimationFrame { gameLoop() }
    }

    gameLoop()
}
```

### T023: Migrate JVM MainJvm.kt from OpenGL to Vulkan

**File**: `examples/voxelcraft/src/jvmMain/kotlin/MainJvm.kt`

```kotlin
import io.kreekt.renderer.vulkan.VulkanRenderer
import io.kreekt.renderer.vulkan.VulkanBufferManager
import io.kreekt.renderer.vulkan.VulkanSurface
import io.kreekt.examples.voxelcraft.VoxelWorld

fun main() {
    println("[VoxelCraft] Starting JVM version with Vulkan...")

    // Initialize GLFW
    if (!glfwInit()) {
        throw RuntimeException("Failed to initialize GLFW")
    }

    // Create window with no client API (Vulkan)
    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

    val window = glfwCreateWindow(800, 600, "VoxelCraft - Vulkan", NULL, NULL)
    if (window == NULL) {
        throw RuntimeException("Failed to create GLFW window")
    }

    // Create Vulkan surface
    val surface = VulkanSurface(window)

    // Create Vulkan renderer
    val renderer = VulkanRenderer(surface, RendererConfig(
        enableValidation = true
    ))

    runBlocking {
        val result = renderer.initialize(RendererConfig())
        if (result is Result.Error) {
            throw RuntimeException("Failed to initialize Vulkan renderer: ${result.error}")
        }
    }

    println("[VoxelCraft] Vulkan renderer initialized")

    // Create buffer manager
    val device = renderer.device // You'll need to expose this
    val physicalDevice = renderer.physicalDevice // You'll need to expose this
    val bufferManager = VulkanBufferManager(device, physicalDevice)

    // Create voxel world
    val logger = StdoutLogger()
    val world = VoxelWorld(bufferManager, logger)

    // Generate initial chunks
    world.generateTerrain()

    // Game loop
    var lastTime = System.currentTimeMillis()
    while (!glfwWindowShouldClose(window)) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastTime) / 1000.0
        lastTime = currentTime

        // Update game logic
        world.update(deltaTime)

        // Render
        renderer.render(world.scene, world.camera)

        // Poll events
        glfwPollEvents()
    }

    // Cleanup
    renderer.dispose()
    glfwDestroyWindow(window)
    glfwTerminate()
}
```

### T024: Remove Legacy OpenGL/WebGL API Calls

Search and remove all legacy API calls:

**Search Patterns**:

- OpenGL: `glGen*`, `glBind*`, `glBuffer*`, `glDraw*`, `glDelete*`
- WebGL: `gl.createBuffer`, `gl.bindBuffer`, `gl.bufferData`, `gl.drawElements`

**Files to Check**:

- `examples/voxelcraft/src/jsMain/kotlin/Main.kt`
- `examples/voxelcraft/src/jvmMain/kotlin/MainJvm.kt`
- `src/jsMain/kotlin/io/kreekt/renderer/webgl/**/*.kt` (fallback code)
- `src/jvmMain/kotlin/io/kreekt/renderer/opengl/**/*.kt` (legacy code)

**Validation**: After removal, run:

```bash
grep -r "gl\." examples/voxelcraft/
grep -r "glBind" examples/voxelcraft/
```

Should return no matches in rendering code.

---

## Phase 3.6: Performance Validation (T025-T027)

### T025: Run Performance Benchmarks

**Command**:

```bash
./gradlew :kreekt-renderer:test --tests PerformanceBenchmarkTest
```

**Expected Results**:

- 60 FPS @ 100k triangles (target)
- 30 FPS @ 50k triangles (minimum)
- Frame time: ≤ 16.67ms (60 FPS)

**Validation**:

```kotlin
@Test
fun testPerformance_100kTriangles_achieves60FPS() {
    val triangleCount = 100_000
    val scene = generateScene(triangleCount)

    val frameTime = measureTimeMillis {
        renderer.render(scene, camera)
    }

    val fps = 1000.0 / frameTime
    assertTrue(fps >= 60.0, "Expected >= 60 FPS, got $fps")
}
```

### T026: Run Visual Regression Tests

**Command**:

```bash
./gradlew :kreekt-renderer:test --tests VisualRegressionTest
```

**Expected Results**:

- SSIM >= 0.95 between Vulkan and WebGPU
- No visual artifacts
- Correct colors and geometry

**Validation**:

```kotlin
@Test
fun testVisualParity_VulkanVsWebGPU_achieves95PercentSSIM() {
    val scene = createTestScene()

    // Render with Vulkan
    val vulkanImage = vulkanRenderer.renderToImage(scene, camera)

    // Render with WebGPU
    val webgpuImage = webgpuRenderer.renderToImage(scene, camera)

    // Compare
    val ssim = calculateSSIM(vulkanImage, webgpuImage)
    assertTrue(ssim >= 0.95, "Expected SSIM >= 0.95, got $ssim")
}
```

### T027: Validate Memory Usage

**Command**:

```bash
./gradlew :examples:voxelcraft:runJvm -Xmx512m
./gradlew :examples:voxelcraft:runJs
```

**Monitor**:

- JVM: Use `-XX:+PrintGCDetails` to track heap usage
- JS: Use Chrome DevTools Memory Profiler

**Expected**:

- Typical: < 250MB
- Maximum: < 500MB (constitutional limit)

**Validation Points**:

- Startup memory after terrain generation
- Memory after 5 minutes of gameplay
- Memory after chunk loading/unloading cycles

---

## Phase 3.7: Test Validation (T028-T031)

### T028-T031: Run Contract Tests

**Commands**:

```bash
# Backend detection
./gradlew :kreekt-renderer:test --tests BackendDetectionTest

# Renderer lifecycle
./gradlew :kreekt-renderer:test --tests RendererLifecycleTest

# Error handling
./gradlew :kreekt-renderer:test --tests ErrorHandlingTest

# Capability detection
./gradlew :kreekt-renderer:test --tests CapabilityDetectionTest
```

**Expected**: All tests pass

**If Tests Fail**: Review error messages and update implementations to match contract expectations.

---

## Phase 3.8: Production Readiness (T032-T035)

### T032: Remove Placeholders

**Command**:

```bash
# Find all placeholders
grep -r "TODO\|FIXME\|STUB" src/*/kotlin/io/kreekt/renderer/feature020/
grep -r "TODO\|FIXME\|STUB" src/jvmMain/kotlin/io/kreekt/renderer/vulkan/Vulkan*Manager.kt
grep -r "TODO\|FIXME\|STUB" src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPU*Manager.kt
```

**Action**: Replace all TODO/FIXME/STUB with actual implementations or remove if no longer needed.

**Validation**: Grep should return 0 matches in feature020 namespace.

### T033: Add Comprehensive Logging

Add logging to all manager methods:

```kotlin
class VulkanBufferManager(...) : BufferManager {

    private val logger = Logger.get("VulkanBufferManager")

    actual override fun createVertexBuffer(data: FloatArray): BufferHandle {
        logger.debug("Creating vertex buffer: ${data.size} floats (${data.size * 4} bytes)")

        // ... implementation ...

        logger.info("Created vertex buffer: handle=${buffer}, size=${sizeBytes}")
        return BufferHandle(...)
    }

    actual override fun destroyBuffer(handle: BufferHandle) {
        logger.debug("Destroying buffer: handle=${handle.handle}")

        // ... implementation ...

        logger.info("Destroyed buffer successfully")
    }
}
```

**Log Levels**:

- **DEBUG**: Detailed operation logs (parameters, intermediate steps)
- **INFO**: Lifecycle events (create, destroy)
- **WARN**: Recoverable errors, performance warnings
- **ERROR**: Fatal errors, exceptions

### T034: Validate Resource Cleanup

Add cleanup validation to dispose methods:

```kotlin
override fun dispose() {
    val startTime = System.currentTimeMillis()
    var buffersDestroyed = 0

    // Destroy all mesh buffers
    meshBuffers.values.forEach { buffers ->
        try {
            bufferManager?.destroyBuffer(buffers.vertexBuffer)
            bufferManager?.destroyBuffer(buffers.indexBuffer)
            bufferManager?.destroyBuffer(buffers.uniformBuffer)
            buffersDestroyed += 3
        } catch (e: Exception) {
            logger.error("Failed to destroy buffer: ${e.message}")
        }
    }

    logger.info("Destroyed $buffersDestroyed buffers in ${System.currentTimeMillis() - startTime}ms")

    // ... rest of disposal ...

    // Verify all resources released
    assert(meshBuffers.isEmpty()) { "Mesh buffers not cleared" }
    assert(swapchainManager == null || swapchain disposed) { "Swapchain not disposed" }
}
```

### T035: Update CLAUDE.md

Add Feature 020 section to `/mnt/d/Projects/KMP/KreeKt/CLAUDE.md`:

```markdown
## Recent Changes

- **2025-10-07**: Implemented Feature 020 - Production-Ready Renderer
  - Complete Vulkan implementation for JVM (VulkanBufferManager, VulkanRenderPassManager, VulkanSwapchain)
  - Complete WebGPU implementation for JS (WebGPUBufferManager, WebGPURenderPassManager, WebGPUSwapchain)
  - 21 contract tests validating all functionality
  - 1,614 lines of production rendering code
  - Cross-platform buffer management with proper lifecycle
  - State-tracked render pass recording
  - Swapchain presentation with resize support
  - VoxelCraft now runs on Vulkan (JVM) and WebGPU (JS)

## Feature 020: Production-Ready Renderer

### Buffer Management

Use Feature 020 managers for all GPU buffer operations:

```kotlin
// Create buffer manager
val bufferManager = VulkanBufferManager(device, physicalDevice) // or WebGPUBufferManager(device)

// Create buffers
val vertexBuffer = bufferManager.createVertexBuffer(floatArrayOf(
    // position (xyz) + color (rgb) interleaved
    0f, 0f, 0f,  1f, 0f, 0f,  // vertex 0: red
    1f, 0f, 0f,  0f, 1f, 0f,  // vertex 1: green
    0f, 1f, 0f,  0f, 0f, 1f   // vertex 2: blue
))

val indexBuffer = bufferManager.createIndexBuffer(intArrayOf(0, 1, 2))

val uniformBuffer = bufferManager.createUniformBuffer(64) // mat4x4

// Update uniform buffer
val mvpMatrix = camera.projectionMatrix * camera.viewMatrix * model.modelMatrix
bufferManager.updateUniformBuffer(uniformBuffer, mvpMatrix.toByteArray())

// Cleanup
bufferManager.destroyBuffer(vertexBuffer)
bufferManager.destroyBuffer(indexBuffer)
bufferManager.destroyBuffer(uniformBuffer)
```

### Render Pass Recording

```kotlin
// Create render pass manager
val renderPassManager = VulkanRenderPassManager(device, commandBuffer, renderPass)

// Record render pass
renderPassManager.beginRenderPass(Color(0.53f, 0.81f, 0.92f, 1.0f), framebufferHandle)
renderPassManager.bindPipeline(pipelineHandle)
renderPassManager.bindVertexBuffer(vertexBuffer, slot = 0)
renderPassManager.bindIndexBuffer(indexBuffer)
renderPassManager.bindUniformBuffer(uniformBuffer, group = 0, binding = 0)
renderPassManager.drawIndexed(indexCount = 3)
renderPassManager.endRenderPass()
```

### Swapchain Management

```kotlin
// Create swapchain
val swapchain = VulkanSwapchain(device, physicalDevice, surface)

// Render loop
val image = swapchain.acquireNextImage()
// ... render to image ...
swapchain.presentImage(image)

// Handle resize
swapchain.recreateSwapchain(1024, 768)
val (width, height) = swapchain.getExtent()
```

### Error Handling

All managers throw consistent exceptions:

- `IllegalArgumentException` - Invalid parameters (empty data, wrong size, misalignment)
- `InvalidBufferException` - Invalid or destroyed buffer handle
- `OutOfMemoryException` - GPU memory allocation failed
- `RenderPassException` - Invalid render pass state
- `SwapchainException` - Swapchain acquire/present failure

### Performance

- Buffer creation: < 5ms for 10k vertices
- Uniform update: < 1ms for 64-byte matrix
- Draw call: < 0.1ms
- Target: 60 FPS @ 100k triangles

```

---

## Summary

This integration guide provides complete step-by-step instructions for:

1. **T017-T020**: Integrating managers into VulkanRenderer and WebGPURenderer
2. **T021-T024**: Migrating VoxelCraft from OpenGL/WebGL to Vulkan/WebGPU
3. **T025-T027**: Validating performance targets (60 FPS, 95% SSIM, < 500MB)
4. **T028-T031**: Running contract tests to verify functionality
5. **T032-T035**: Production cleanup (remove TODOs, add logging, validate cleanup, update docs)

Each task includes:
- Exact file paths
- Code snippets with implementation details
- Validation criteria
- Expected results

**Estimated Time**: 19-26 hours remaining for complete integration and validation.

---

**Guide Version**: 1.0
**Last Updated**: 2025-10-07
