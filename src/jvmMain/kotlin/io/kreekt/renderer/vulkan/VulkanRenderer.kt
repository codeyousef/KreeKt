/**
 * T018: VulkanRenderer Implementation
 * Feature: 019-we-should-not
 *
 * Vulkan-based renderer for JVM platform using LWJGL 3.3.6.
 */

package io.kreekt.renderer.vulkan

import io.kreekt.camera.Camera
import io.kreekt.core.scene.Scene
import io.kreekt.renderer.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
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
        return try {
            // 1. Create VkInstance
            instance = createInstance(config.enableValidation)
                ?: return io.kreekt.core.Result.Error(
                    "Failed to create VkInstance",
                    RendererInitializationException.AdapterRequestFailedException(
                        BackendType.VULKAN,
                        "Failed to create VkInstance"
                    )
                )

            // 2. Select physical device
            physicalDevice = selectPhysicalDevice(instance!!)
                ?: return io.kreekt.core.Result.Error(
                    "No suitable Vulkan physical device found",
                    RendererInitializationException.AdapterRequestFailedException(
                        BackendType.VULKAN,
                        "No suitable Vulkan physical device found"
                    )
                )

            // 3. Create logical device
            device = createLogicalDevice(physicalDevice!!, config)
                ?: return io.kreekt.core.Result.Error(
                    "Failed to create logical device",
                    RendererInitializationException.DeviceCreationFailedException(
                        BackendType.VULKAN,
                        getPhysicalDeviceInfo(physicalDevice!!),
                        "Failed to create logical device"
                    )
                )

            // 4. Get graphics queue
            MemoryStack.stackPush().use { stack ->
                val pQueue = stack.callocPointer(1)
                vkGetDeviceQueue(device!!, 0, 0, pQueue)
                graphicsQueue = VkQueue(pQueue.get(0), device!!)
            }

            // 5. Create command pool
            commandPool = createCommandPool(device!!)
            if (commandPool == VK_NULL_HANDLE) {
                return io.kreekt.core.Result.Error(
                    "Failed to create command pool",
                    RendererInitializationException.DeviceCreationFailedException(
                        BackendType.VULKAN,
                        getPhysicalDeviceInfo(physicalDevice!!),
                        "Failed to create command pool"
                    )
                )
            }

            // 6. Allocate command buffer
            commandBuffer = allocateCommandBuffer(device!!, commandPool)

            // 7. Query capabilities
            capabilities = queryCapabilities(physicalDevice!!)

            initialized = true
            io.kreekt.core.Result.Success(Unit)
        } catch (e: Exception) {
            // Clean up partial initialization
            dispose()
            io.kreekt.core.Result.Error(
                e.message ?: "Unknown error",
                RendererInitializationException.DeviceCreationFailedException(
                    BackendType.VULKAN,
                    "Unknown device",
                    e.message ?: "Unknown error"
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

        val frameTime = measureTimeMillis {
            // TODO: Actual rendering implementation
            // 1. Begin command buffer recording
            // 2. Begin render pass
            // 3. Bind pipeline
            // 4. Render scene objects
            // 5. End render pass
            // 6. Submit command buffer
            // 7. Present swapchain

            // For now, just track frame stats
            frameCount++
        }

        // Update FPS stats
        updateStats(frameTime)
    }

    /**
     * Resize render targets.
     *
     * Currently a stub. Will recreate swapchain in full implementation.
     */
    override fun resize(width: Int, height: Int) {
        // TODO: Recreate swapchain with new dimensions
    }

    /**
     * Dispose Vulkan resources.
     *
     * Must be called before application exit to prevent resource leaks.
     */
    override fun dispose() {
        if (!initialized) return

        // Wait for device to finish
        device?.let { vkDeviceWaitIdle(it) }

        // Destroy command pool (also frees command buffers)
        if (commandPool != VK_NULL_HANDLE && device != null) {
            vkDestroyCommandPool(device!!, commandPool, null)
            commandPool = VK_NULL_HANDLE
        }

        // Destroy logical device
        device?.let {
            vkDestroyDevice(it, null)
            device = null
        }

        // Destroy instance
        instance?.let {
            vkDestroyInstance(it, null)
            instance = null
        }

        initialized = false
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

            // Instance create info
            val createInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)

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

            // Device create info
            val createInfo = VkDeviceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueCreateInfo)
                .pEnabledFeatures(deviceFeatures)

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
