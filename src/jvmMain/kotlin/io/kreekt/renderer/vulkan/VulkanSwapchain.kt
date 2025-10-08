/**
 * T009-T010: VulkanSwapchain Implementation
 * Feature: 020-go-from-mvp
 *
 * Vulkan swapchain management for presenting rendered frames.
 */

package io.kreekt.renderer.vulkan

import io.kreekt.renderer.feature020.SwapchainException
import io.kreekt.renderer.feature020.SwapchainImage
import io.kreekt.renderer.feature020.SwapchainManager
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK12.*

/**
 * Vulkan swapchain manager implementation.
 *
 * Manages swapchain lifecycle for presenting rendered frames to screen.
 *
 * @property device Vulkan logical device
 * @property physicalDevice Vulkan physical device
 * @property surface Vulkan surface (window)
 */
class VulkanSwapchain(
    private val device: VkDevice,
    private val physicalDevice: VkPhysicalDevice,
    private val surface: Long // VkSurfaceKHR
) : SwapchainManager {

    private var swapchain: Long = VK_NULL_HANDLE
    private var swapchainImages: List<Long> = emptyList()
    private var currentWidth: Int = 800
    private var currentHeight: Int = 600
    private var currentImageIndex: Int = -1

    // Synchronization primitives
    private var imageAvailableSemaphore: Long = VK_NULL_HANDLE
    private var renderFinishedSemaphore: Long = VK_NULL_HANDLE

    init {
        // Create synchronization objects
        createSyncObjects()
        // Create initial swapchain
        create(currentWidth, currentHeight)
    }

    /**
     * Create swapchain with specified dimensions.
     *
     * @param width Swapchain width in pixels
     * @param height Swapchain height in pixels
     */
    private fun create(width: Int, height: Int) {
        try {
            MemoryStack.stackPush().use { stack ->
                // Query surface capabilities
                val capabilities = VkSurfaceCapabilitiesKHR.malloc(stack)
                vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, capabilities)

                // Choose image count (prefer triple buffering)
                val imageCount = capabilities.minImageCount() + 1
                val maxImageCount = capabilities.maxImageCount()
                val finalImageCount = if (maxImageCount > 0 && imageCount > maxImageCount) {
                    maxImageCount
                } else {
                    imageCount
                }

                // Choose surface format (prefer B8G8R8A8_UNORM)
                val surfaceFormat = chooseSurfaceFormat()

                // Choose present mode (use FIFO for vsync)
                val presentMode = VK_PRESENT_MODE_FIFO_KHR

                // Create swapchain
                val swapchainInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface)
                    .minImageCount(finalImageCount)
                    .imageFormat(surfaceFormat.format)
                    .imageColorSpace(surfaceFormat.colorSpace)
                    .imageExtent { extent ->
                        extent.width(width)
                        extent.height(height)
                    }
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .preTransform(capabilities.currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(swapchain) // For recreation

                val pSwapchain = stack.mallocLong(1)
                val result = vkCreateSwapchainKHR(device, swapchainInfo, null, pSwapchain)
                if (result != VK_SUCCESS) {
                    throw SwapchainException("Failed to create swapchain: VkResult=$result")
                }

                // Destroy old swapchain if recreating
                if (swapchain != VK_NULL_HANDLE) {
                    vkDestroySwapchainKHR(device, swapchain, null)
                }

                swapchain = pSwapchain.get(0)
                currentWidth = width
                currentHeight = height

                // Get swapchain images
                val pImageCount = stack.mallocInt(1)
                vkGetSwapchainImagesKHR(device, swapchain, pImageCount, null)
                val actualImageCount = pImageCount.get(0)

                val pImages = stack.mallocLong(actualImageCount)
                vkGetSwapchainImagesKHR(device, swapchain, pImageCount, pImages)

                swapchainImages = (0 until actualImageCount).map { pImages.get(it) }
            }
        } catch (e: SwapchainException) {
            throw e
        } catch (e: Exception) {
            throw SwapchainException("Failed to create swapchain: ${e.message}")
        }
    }

    /**
     * Choose surface format (prefer B8G8R8A8_UNORM with SRGB_NONLINEAR).
     */
    private fun chooseSurfaceFormat(): SurfaceFormatData {
        MemoryStack.stackPush().use { stack ->
            val pFormatCount = stack.mallocInt(1)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pFormatCount, null)
            val formatCount = pFormatCount.get(0)

            if (formatCount == 0) {
                // Fallback to default
                return SurfaceFormatData(VK_FORMAT_B8G8R8A8_UNORM, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
            }

            val formats = VkSurfaceFormatKHR.malloc(formatCount, stack)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pFormatCount, formats)

            // Look for preferred format
            for (i in 0 until formatCount) {
                val format = formats.get(i)
                if (format.format() == VK_FORMAT_B8G8R8A8_UNORM &&
                    format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
                ) {
                    return SurfaceFormatData(format.format(), format.colorSpace())
                }
            }

            // Return first available format
            val firstFormat = formats.get(0)
            return SurfaceFormatData(firstFormat.format(), firstFormat.colorSpace())
        }
    }

    /**
     * Create synchronization objects (semaphores).
     */
    private fun createSyncObjects() {
        MemoryStack.stackPush().use { stack ->
            val semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)

            val pSemaphore = stack.mallocLong(1)

            // Image available semaphore
            var result = vkCreateSemaphore(device, semaphoreInfo, null, pSemaphore)
            if (result != VK_SUCCESS) {
                throw SwapchainException("Failed to create imageAvailable semaphore: VkResult=$result")
            }
            imageAvailableSemaphore = pSemaphore.get(0)

            // Render finished semaphore
            result = vkCreateSemaphore(device, semaphoreInfo, null, pSemaphore)
            if (result != VK_SUCCESS) {
                throw SwapchainException("Failed to create renderFinished semaphore: VkResult=$result")
            }
            renderFinishedSemaphore = pSemaphore.get(0)
        }
    }

    /**
     * Acquire next swapchain image for rendering.
     *
     * @return Swapchain image ready for rendering
     * @throws SwapchainException if acquire fails
     */
    override fun acquireNextImage(): SwapchainImage {
        if (swapchain == VK_NULL_HANDLE) {
            throw SwapchainException("Swapchain not created")
        }

        try {
            MemoryStack.stackPush().use { stack ->
                val pImageIndex = stack.mallocInt(1)
                val result = vkAcquireNextImageKHR(
                    device,
                    swapchain,
                    Long.MAX_VALUE, // Timeout (infinite)
                    imageAvailableSemaphore,
                    VK_NULL_HANDLE,
                    pImageIndex
                )

                when (result) {
                    VK_SUCCESS, VK_SUBOPTIMAL_KHR -> {
                        currentImageIndex = pImageIndex.get(0)
                        return SwapchainImage(
                            handle = swapchainImages[currentImageIndex],
                            index = currentImageIndex,
                            ready = true
                        )
                    }

                    VK_ERROR_OUT_OF_DATE_KHR -> {
                        throw SwapchainException("Swapchain out of date (needs recreation)")
                    }

                    else -> {
                        throw SwapchainException("Failed to acquire swapchain image: VkResult=$result")
                    }
                }
            }
        } catch (e: SwapchainException) {
            throw e
        } catch (e: Exception) {
            throw SwapchainException("Failed to acquire swapchain image: ${e.message}")
        }
    }

    /**
     * Present rendered image to screen.
     *
     * @param image Swapchain image to present
     * @throws SwapchainException if present fails
     */
    override fun presentImage(image: SwapchainImage) {
        if (!image.isReady()) {
            throw SwapchainException("Swapchain image not ready for presentation")
        }

        // Note: Presentation requires vkQueuePresentKHR with VkQueue and synchronization.
        // This is deferred to renderer integration where the graphics queue is available.
        // The swapchain image has been acquired successfully; presentation is handled
        // by the VulkanRenderer which has access to the graphics queue and semaphores.
    }

    /**
     * Recreate swapchain on window resize.
     *
     * @param width New width in pixels (> 0)
     * @param height New height in pixels (> 0)
     * @throws IllegalArgumentException if width or height <= 0
     */
    override fun recreateSwapchain(width: Int, height: Int) {
        if (width <= 0) {
            throw IllegalArgumentException("width must be > 0, got $width")
        }
        if (height <= 0) {
            throw IllegalArgumentException("height must be > 0, got $height")
        }

        // Wait for device to finish
        vkDeviceWaitIdle(device)

        // Recreate swapchain
        create(width, height)
    }

    /**
     * Get current swapchain extent.
     *
     * @return Pair of (width, height) in pixels
     */
    override fun getExtent(): Pair<Int, Int> {
        return Pair(currentWidth, currentHeight)
    }

    /**
     * Cleanup swapchain resources.
     */
    fun dispose() {
        if (swapchain != VK_NULL_HANDLE) {
            vkDestroySwapchainKHR(device, swapchain, null)
            swapchain = VK_NULL_HANDLE
        }

        if (imageAvailableSemaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(device, imageAvailableSemaphore, null)
            imageAvailableSemaphore = VK_NULL_HANDLE
        }

        if (renderFinishedSemaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(device, renderFinishedSemaphore, null)
            renderFinishedSemaphore = VK_NULL_HANDLE
        }
    }
}

/**
 * Surface format data.
 */
private data class SurfaceFormatData(
    val format: Int,
    val colorSpace: Int
)
