/**
 * T017-T019: VulkanRenderer Implementation with Feature 020 Managers
 * Feature: 020-go-from-mvp
 *
 * Vulkan-based renderer for JVM platform using LWJGL 3.3.6.
 * Integrates BufferManager, RenderPassManager, and SwapchainManager.
 */

package io.kreekt.renderer.vulkan

import io.kreekt.camera.Camera
import io.kreekt.core.scene.Scene
import io.kreekt.renderer.*
import io.kreekt.renderer.feature020.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.KHRSwapchain.*
import kotlin.system.measureTimeMillis

/**
 * Vulkan renderer implementation for JVM platform.
 *
 * Uses LWJGL 3.3.6 Vulkan bindings to implement the Renderer interface.
 * Targets Vulkan 1.1+ for broad compatibility.
 *
 * Lifecycle:
 * 1. Create VulkanRenderer instance
 * 2. Call initialize(config) - creates VkInstance, device, pipeline
 * 3. Call render(scene, camera) repeatedly
 * 4. Call dispose() on shutdown
 *
 * @property surface VulkanSurface wrapping GLFW window
 * @property config Renderer configuration
 */
class VulkanRenderer(
    private val surface: RenderSurface,
    private val config: RendererConfig
) : Renderer {

    // Vulkan handles
    private var instance: VkInstance? = null
    private var physicalDevice: VkPhysicalDevice? = null
    private var device: VkDevice? = null
    private var graphicsQueue: VkQueue? = null
    private var commandPool: Long = VK_NULL_HANDLE
    private var commandBuffer: VkCommandBuffer? = null
    private var renderPass: Long = VK_NULL_HANDLE

    // Feature 020 Managers (T017-T019)
    private var bufferManager: VulkanBufferManager? = null
    private var renderPassManager: VulkanRenderPassManager? = null
    private var swapchainManager: VulkanSwapchain? = null

    // Renderer state
    override var backend: BackendType = BackendType.VULKAN
        private set

    override var capabilities: RendererCapabilities = RendererCapabilities()
        private set

    override var stats: RenderStats = RenderStats(0.0, 0.0, 0, 0)
        private set

    private var initialized = false
    private var frameCount = 0
    private var lastFrameTime = System.currentTimeMillis()
    private var fpsAccumulator = 0.0
    private var fpsFrameCount = 0

    // T033: Debug flag for verbose frame logging (default off to avoid spam)
    var enableFrameLogging: Boolean = false

    /**
     * Initialize Vulkan renderer.
     *
     * Process:
     * 1. Create VkInstance with validation layers (if config.enableValidation)
     * 2. Select physical device (prefer discrete GPU)
     * 3. Create logical device with graphics queue
     * 4. Create command pool and buffers
     * 5. Query capabilities
     *
     * @param config Renderer configuration
     * @return Success or RendererError on failure
     */
    override suspend fun initialize(config: RendererConfig): io.kreekt.core.Result<Unit> {
        println("T033: Starting Vulkan renderer initialization...")
        val startTime = System.currentTimeMillis()
        return try {
                // 1. Create VkInstance
                println("T033: Creating VkInstance (validation=${config.enableValidation})...")
                instance = createInstance(config.enableValidation)
                    ?: return io.kreekt.core.Result.Error(
                        "Failed to create VkInstance",
                        io.kreekt.renderer.RendererInitializationException.AdapterRequestFailedException(
                            BackendType.VULKAN,
                            "Failed to create VkInstance"
                        )
                    )
                println("T033: VkInstance created successfully")

            // 1.5. Create surface (T019: Required before swapchain creation)
            println("T033: Creating Vulkan surface...")
            if (surface is VulkanSurface) {
                surface.createSurface(instance!!)
            }
            println("T033: Surface created successfully")

            // 2. Select physical device
            println("T033: Selecting physical device...")
            physicalDevice = selectPhysicalDevice(instance!!)
                ?: return io.kreekt.core.Result.Error(
                    "No suitable Vulkan physical device found",
                    io.kreekt.renderer.RendererInitializationException.AdapterRequestFailedException(
                        BackendType.VULKAN,
                        "No suitable Vulkan physical device found"
                    )
                )
            val deviceInfo = getPhysicalDeviceInfo(physicalDevice!!)
            println("T033: Selected physical device: $deviceInfo")

            // 3. Create logical device
            println("T033: Creating logical device...")
            device = createLogicalDevice(physicalDevice!!, config)
                ?: return io.kreekt.core.Result.Error(
                    "Failed to create logical device",
                    io.kreekt.renderer.RendererInitializationException.DeviceCreationFailedException(
                        BackendType.VULKAN,
                        getPhysicalDeviceInfo(physicalDevice!!),
                        "Failed to create logical device"
                    )
                )
            println("T033: Logical device created successfully")

            // 4. Get graphics queue
            println("T033: Getting graphics queue...")
            MemoryStack.stackPush().use { stack ->
                val pQueue = stack.callocPointer(1)
                vkGetDeviceQueue(device!!, 0, 0, pQueue)
                graphicsQueue = VkQueue(pQueue.get(0), device!!)
            }
            println("T033: Graphics queue obtained")

            // 5. Create command pool
            println("T033: Creating command pool...")
            commandPool = createCommandPool(device!!)
            if (commandPool == VK_NULL_HANDLE) {
                return io.kreekt.core.Result.Error(
                    "Failed to create command pool",
                    io.kreekt.renderer.RendererInitializationException.DeviceCreationFailedException(
                        BackendType.VULKAN,
                        getPhysicalDeviceInfo(physicalDevice!!),
                        "Failed to create command pool"
                    )
                )
            }
            println("T033: Command pool created successfully")

            // 6. Allocate command buffer
            println("T033: Allocating command buffer...")
            commandBuffer = allocateCommandBuffer(device!!, commandPool)
            println("T033: Command buffer allocated successfully")

            // 7. Create render pass
            println("T033: Creating render pass...")
            renderPass = createRenderPass(device!!)
            if (renderPass == VK_NULL_HANDLE) {
                return io.kreekt.core.Result.Error(
                    "Failed to create render pass",
                    io.kreekt.renderer.RendererInitializationException.DeviceCreationFailedException(
                        BackendType.VULKAN,
                        getPhysicalDeviceInfo(physicalDevice!!),
                        "Failed to create render pass"
                    )
                )
            }
            println("T033: Render pass created successfully")

            // 8. Initialize Feature 020 Managers (T017-T019)

            // T017: Initialize BufferManager
            println("T033: Initializing BufferManager...")
            bufferManager = VulkanBufferManager(device!!, physicalDevice!!)
            println("T033: BufferManager initialized successfully")

            // T019: Initialize SwapchainManager
            println("T033: Initializing SwapchainManager...")
            val vulkanSurface = extractVulkanSurface(surface)
            swapchainManager = VulkanSwapchain(device!!, physicalDevice!!, vulkanSurface)
            println("T033: SwapchainManager initialized successfully")

            // T018: Initialize RenderPassManager
            println("T033: Initializing RenderPassManager...")
            renderPassManager = VulkanRenderPassManager(device!!, commandBuffer!!, renderPass)
            println("T033: RenderPassManager initialized successfully")

            // 9. Query capabilities
            println("T033: Querying device capabilities...")
            capabilities = queryCapabilities(physicalDevice!!)
            println("T033: Capabilities detected: maxTextureSize=${capabilities.maxTextureSize}, maxSamples=${capabilities.maxSamples}")

            initialized = true
            val totalInitTime = System.currentTimeMillis() - startTime
            println("T033: Vulkan renderer initialization completed in ${totalInitTime}ms")
            io.kreekt.core.Result.Success(Unit)
        } catch (e: Exception) {
            // Clean up partial initialization
            println("T033: ERROR during initialization: ${e.message}")
            println("T033: Stack trace: ${e.stackTraceToString()}")
            dispose()
            io.kreekt.core.Result.Error(
                "Initialization failed at stage: ${e.message}",
                io.kreekt.renderer.RendererInitializationException.DeviceCreationFailedException(
                    BackendType.VULKAN,
                    physicalDevice?.let { getPhysicalDeviceInfo(it) } ?: "Unknown device",
                    "Initialization failed: ${e.message}"
                )
            )
        }
    }

    /**
     * Render scene from camera perspective.
     *
     * Currently a stub implementation that tracks stats.
     * Full rendering will be implemented in later phases.
     */
    override fun render(scene: Scene, camera: Camera) {
        if (!initialized) {
            throw IllegalStateException("Renderer not initialized. Call initialize() first.")
        }

        if (enableFrameLogging) {
            println("T033: [Frame $frameCount] Starting render...")
        }

        val frameTime = measureTimeMillis {
            // TODO: Actual rendering implementation
            // 1. Begin command buffer recording
            if (enableFrameLogging) println("T033: [Frame $frameCount] - Begin command buffer recording")

            // 2. Begin render pass
            if (enableFrameLogging) println("T033: [Frame $frameCount] - Begin render pass")

            // 3. Bind pipeline
            if (enableFrameLogging) println("T033: [Frame $frameCount] - Bind pipeline")

            // 4. Render scene objects
            if (enableFrameLogging) println("T033: [Frame $frameCount] - Render ${scene.children.size} scene objects")

            // 5. End render pass
            if (enableFrameLogging) println("T033: [Frame $frameCount] - End render pass")

            // 6. Submit command buffer
            if (enableFrameLogging) println("T033: [Frame $frameCount] - Submit command buffer")

            // 7. Present swapchain
            if (enableFrameLogging) println("T033: [Frame $frameCount] - Present swapchain")

            // For now, just track frame stats
            frameCount++
        }

        if (enableFrameLogging) {
            println("T033: [Frame ${frameCount-1}] Render completed in ${frameTime}ms")
        }

        // Update FPS stats
        updateStats(frameTime)
    }

    /**
     * Resize render targets.
     *
     * T019: Use SwapchainManager to recreate swapchain with new dimensions.
     */
    override fun resize(width: Int, height: Int) {
        if (!initialized) {
            throw IllegalStateException("Renderer not initialized. Call initialize() first.")
        }

        swapchainManager?.recreateSwapchain(width, height)
    }

    /**
     * Dispose Vulkan resources.
     *
     * Must be called before application exit to prevent resource leaks.
     * T017-T019: Clean up all managers.
     */
    override fun dispose() {
        if (!initialized) {
            println("T033: Dispose called but renderer not initialized, skipping")
            return
        }

        println("T033: Starting Vulkan renderer disposal...")

        // Wait for device to finish
        println("T033: Waiting for device idle...")
        device?.let { vkDeviceWaitIdle(it) }
        println("T033: Device idle")

        // T019: Dispose SwapchainManager
        println("T033: Disposing SwapchainManager...")
        swapchainManager?.dispose()
        swapchainManager = null
        println("T033: SwapchainManager disposed")

        // T018: RenderPassManager doesn't need explicit disposal (uses command buffer)
        println("T033: Cleaning up RenderPassManager...")
        renderPassManager = null

        // T017: BufferManager doesn't need explicit disposal (buffers disposed individually)
        println("T033: Cleaning up BufferManager...")
        bufferManager = null

        // Destroy render pass
        if (renderPass != VK_NULL_HANDLE && device != null) {
            println("T033: Destroying render pass...")
            vkDestroyRenderPass(device!!, renderPass, null)
            renderPass = VK_NULL_HANDLE
            println("T033: Render pass destroyed")
        }

        // Destroy command pool (also frees command buffers)
        if (commandPool != VK_NULL_HANDLE && device != null) {
            println("T033: Destroying command pool...")
            vkDestroyCommandPool(device!!, commandPool, null)
            commandPool = VK_NULL_HANDLE
            println("T033: Command pool destroyed")
        }

        // Destroy logical device
        device?.let {
            println("T033: Destroying logical device...")
            vkDestroyDevice(it, null)
            device = null
            println("T033: Logical device destroyed")
        }

        // T019: Destroy surface
        if (surface is VulkanSurface) {
            println("T033: Destroying surface...")
            surface.destroySurface()
            println("T033: Surface destroyed")
        }

        // Destroy instance
        instance?.let {
            println("T033: Destroying VkInstance...")
            vkDestroyInstance(it, null)
            instance = null
            println("T033: VkInstance destroyed")
        }

        initialized = false
        println("T033: Vulkan renderer disposal completed")
    }

    // Note: getStats() removed - use 'stats' property directly to avoid JVM signature clash

    // === Private Helper Methods ===

    private fun createInstance(enableValidation: Boolean): VkInstance? {
        return MemoryStack.stackPush().use { stack ->
            // Application info
            val appInfo = VkApplicationInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.UTF8("KreeKt Application"))
                .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                .pEngineName(stack.UTF8("KreeKt"))
                .engineVersion(VK_MAKE_VERSION(1, 0, 0))
                .apiVersion(VK_API_VERSION_1_1)

            // Get required extensions from GLFW (T019: Required for surface creation)
            val glfwExtensions = org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions()
                ?: return null

            // Instance create info
            val createInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(glfwExtensions)

            // Validation layers (if enabled)
            if (enableValidation) {
                val layers = stack.callocPointer(1)
                layers.put(0, stack.UTF8("VK_LAYER_KHRONOS_validation"))
                createInfo.ppEnabledLayerNames(layers)
            }

            // Create instance
            val pInstance = stack.callocPointer(1)
            val result = vkCreateInstance(createInfo, null, pInstance)
            if (result != VK_SUCCESS) {
                null
            } else {
                VkInstance(pInstance.get(0), createInfo)
            }
        }
    }

    private fun selectPhysicalDevice(instance: VkInstance): VkPhysicalDevice? {
        return MemoryStack.stackPush().use { stack ->
            // Enumerate physical devices
            val pPhysicalDeviceCount = stack.callocInt(1)
            vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, null)
            val deviceCount = pPhysicalDeviceCount.get(0)
            if (deviceCount == 0) return null

            val pPhysicalDevices = stack.callocPointer(deviceCount)
            vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, pPhysicalDevices)

            // Prefer discrete GPU, fallback to first device
            var selectedDevice: VkPhysicalDevice? = null
            for (i in 0 until deviceCount) {
                val device = VkPhysicalDevice(pPhysicalDevices.get(i), instance)
                val properties = VkPhysicalDeviceProperties.calloc(stack)
                vkGetPhysicalDeviceProperties(device, properties)

                if (properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                    selectedDevice = device
                    break
                }
                if (selectedDevice == null) {
                    selectedDevice = device
                }
            }
            selectedDevice
        }
    }

    private fun createLogicalDevice(physicalDevice: VkPhysicalDevice, config: RendererConfig): VkDevice? {
        return MemoryStack.stackPush().use { stack ->
            // Queue create info (graphics queue family 0)
            val queuePriority = stack.callocFloat(1).put(0, 1.0f)
            val queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(0)
                .pQueuePriorities(queuePriority)

            // Device features
            val deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack)

            // T019: Enable swapchain extension for presentation
            val extensions = stack.callocPointer(1)
            extensions.put(0, stack.UTF8(org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME))

            // Device create info
            val createInfo = VkDeviceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueCreateInfo)
                .pEnabledFeatures(deviceFeatures)
                .ppEnabledExtensionNames(extensions)

            // Create device
            val pDevice = stack.callocPointer(1)
            val result = vkCreateDevice(physicalDevice, createInfo, null, pDevice)
            if (result != VK_SUCCESS) {
                null
            } else {
                VkDevice(pDevice.get(0), physicalDevice, createInfo)
            }
        }
    }

    private fun createCommandPool(device: VkDevice): Long {
        return MemoryStack.stackPush().use { stack ->
            val createInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(0)

            val pCommandPool = stack.callocLong(1)
            val result = vkCreateCommandPool(device, createInfo, null, pCommandPool)
            if (result != VK_SUCCESS) VK_NULL_HANDLE else pCommandPool.get(0)
        }
    }

    private fun allocateCommandBuffer(device: VkDevice, commandPool: Long): VkCommandBuffer? {
        return MemoryStack.stackPush().use { stack ->
            val allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1)

            val pCommandBuffer = stack.callocPointer(1)
            val result = vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer)
            if (result != VK_SUCCESS) null else VkCommandBuffer(pCommandBuffer.get(0), device)
        }
    }

    private fun queryCapabilities(physicalDevice: VkPhysicalDevice): RendererCapabilities {
        return MemoryStack.stackPush().use { stack ->
            val properties = VkPhysicalDeviceProperties.calloc(stack)
            vkGetPhysicalDeviceProperties(physicalDevice, properties)

            val deviceNameBytes = ByteArray(VK_MAX_PHYSICAL_DEVICE_NAME_SIZE)
            properties.deviceName().get(deviceNameBytes)
            val deviceName = String(deviceNameBytes).trim('\u0000')

            val limits = properties.limits()

            RendererCapabilities(
                backend = BackendType.VULKAN,
                deviceName = deviceName,
                driverVersion = properties.driverVersion().toString(),
                supportsCompute = true,
                supportsRayTracing = false, // Would need to query extensions
                supportsMultisampling = true,
                maxTextureSize = limits.maxImageDimension2D(),
                maxSamples = getSampleCountFromVulkanSampleCount(limits.framebufferColorSampleCounts()),
                maxUniformBufferBindings = limits.maxPerStageDescriptorUniformBuffers(),
                depthTextures = true,
                floatTextures = true,
                instancedRendering = true,
                extensions = emptySet() // TODO: Query actual extensions
            )
        }
    }

    private fun getSampleCountFromVulkanSampleCount(sampleCountFlags: Int): Int {
        return when {
            (sampleCountFlags and VK_SAMPLE_COUNT_64_BIT) != 0 -> 64
            (sampleCountFlags and VK_SAMPLE_COUNT_32_BIT) != 0 -> 32
            (sampleCountFlags and VK_SAMPLE_COUNT_16_BIT) != 0 -> 16
            (sampleCountFlags and VK_SAMPLE_COUNT_8_BIT) != 0 -> 8
            (sampleCountFlags and VK_SAMPLE_COUNT_4_BIT) != 0 -> 4
            (sampleCountFlags and VK_SAMPLE_COUNT_2_BIT) != 0 -> 2
            else -> 1
        }
    }

    private fun getPhysicalDeviceInfo(physicalDevice: VkPhysicalDevice): String {
        return MemoryStack.stackPush().use { stack ->
            val properties = VkPhysicalDeviceProperties.calloc(stack)
            vkGetPhysicalDeviceProperties(physicalDevice, properties)

            val deviceNameBytes = ByteArray(VK_MAX_PHYSICAL_DEVICE_NAME_SIZE)
            properties.deviceName().get(deviceNameBytes)
            val deviceName = String(deviceNameBytes).trim('\u0000')

            "$deviceName (Vulkan ${VK_VERSION_MAJOR(properties.apiVersion())}.${VK_VERSION_MINOR(properties.apiVersion())}.${
                VK_VERSION_PATCH(
                    properties.apiVersion()
                )
            })"
        }
    }

    /**
     * Create render pass for rendering.
     * T018: Used by RenderPassManager.
     */
    private fun createRenderPass(device: VkDevice): Long {
        return MemoryStack.stackPush().use { stack ->
            // Color attachment description
            val colorAttachment = VkAttachmentDescription.calloc(1, stack)
                .format(VK_FORMAT_B8G8R8A8_UNORM)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)

            // Color attachment reference
            val colorAttachmentRef = VkAttachmentReference.calloc(1, stack)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

            // Subpass description
            val subpass = VkSubpassDescription.calloc(1, stack)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .pColorAttachments(colorAttachmentRef)

            // Render pass create info
            val renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(colorAttachment)
                .pSubpasses(subpass)

            val pRenderPass = stack.mallocLong(1)
            val result = vkCreateRenderPass(device, renderPassInfo, null, pRenderPass)
            if (result != VK_SUCCESS) VK_NULL_HANDLE else pRenderPass.get(0)
        }
    }

    /**
     * Extract VkSurfaceKHR from RenderSurface.
     * T019: Used for SwapchainManager initialization.
     */
    private fun extractVulkanSurface(surface: RenderSurface): Long {
        // VulkanSurface should have a method to get the VkSurfaceKHR handle
        // For now, we'll use reflection or expect a specific type
        return when (surface) {
            is VulkanSurface -> surface.getSurfaceHandle()
            else -> throw IllegalArgumentException("RenderSurface must be VulkanSurface for Vulkan renderer")
        }
    }

    private fun updateStats(frameTimeMs: Long) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastFrameTime
        lastFrameTime = currentTime

        // Calculate instantaneous FPS
        val instantFps = if (frameTimeMs > 0) 1000.0 / frameTimeMs else 0.0

        // Accumulate for smoothed FPS
        fpsAccumulator += instantFps
        fpsFrameCount++

        // Update stats every 60 frames (roughly 1 second at 60 FPS)
        if (fpsFrameCount >= 60) {
            val averageFps = fpsAccumulator / fpsFrameCount
            stats = RenderStats(
                fps = averageFps,
                frameTime = frameTimeMs.toDouble(),
                triangles = 0, // TODO: Track actual triangle count
                drawCalls = 0, // TODO: Track actual draw calls
                textureMemory = 0L, // TODO: Track texture memory
                bufferMemory = 0L, // TODO: Track buffer memory
                timestamp = currentTime
            )
            fpsAccumulator = 0.0
            fpsFrameCount = 0
        }
    }
}
