package io.kreekt.renderer.feature020

import kotlin.test.*

/**
 * Contract tests for SwapchainManager interface.
 * Feature 020 - Production-Ready Renderer
 *
 * TDD Red Phase: All tests must fail (no implementation yet)
 */
class SwapchainManagerTest {

    private lateinit var swapchainManager: SwapchainManager

    @BeforeTest
    fun setup() {
        // TDD Red Phase: No implementation exists yet, this will fail
        // Implementation will be provided in T005-T016
        throw NotImplementedError("SwapchainManager implementation not yet available (TDD red phase)")
    }

    /**
     * Test 1: Acquire and Present (Normal Flow)
     * Contract: specs/020-go-from-mvp/contracts/swapchain-contract.md
     */
    @Test
    fun testAcquirePresent_normalFlow_succeeds() {
        // GIVEN: Initialized swapchain

        // WHEN: Acquire next image
        val image = swapchainManager.acquireNextImage()

        // THEN: Image is valid and ready
        assertNotNull(image)
        assertTrue(image.isReady())

        // WHEN: Present image
        swapchainManager.presentImage(image)

        // THEN: No exception thrown
    }

    /**
     * Test 2: Recreate Swapchain (Valid Dimensions)
     * Contract: specs/020-go-from-mvp/contracts/swapchain-contract.md
     */
    @Test
    fun testRecreateSwapchain_validDimensions_succeeds() {
        // GIVEN: Initialized swapchain (800×600)
        val originalExtent = swapchainManager.getExtent()
        assertEquals(800, originalExtent.first)
        assertEquals(600, originalExtent.second)

        // WHEN: Recreate with new dimensions (1024×768)
        swapchainManager.recreateSwapchain(1024, 768)

        // THEN: Extent updated
        val newExtent = swapchainManager.getExtent()
        assertEquals(1024, newExtent.first)
        assertEquals(768, newExtent.second)
    }

    /**
     * Test 3: Recreate Swapchain (Invalid Dimensions - Zero Width)
     * Contract: specs/020-go-from-mvp/contracts/swapchain-contract.md
     */
    @Test
    fun testRecreateSwapchain_invalidDimensions_throwsException() {
        // GIVEN: Initialized swapchain

        // WHEN/THEN: Recreate with invalid dimensions throws IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            swapchainManager.recreateSwapchain(0, 600)
        }

        // WHEN/THEN: Recreate with negative dimensions throws IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            swapchainManager.recreateSwapchain(800, -1)
        }
    }

    /**
     * Test 4: Get Extent (Returns Current Dimensions)
     * Contract: specs/020-go-from-mvp/contracts/swapchain-contract.md
     */
    @Test
    fun testGetExtent_returnsCurrentDimensions() {
        // WHEN: Get extent
        val extent = swapchainManager.getExtent()

        // THEN: Returns valid dimensions
        assertTrue(extent.first > 0)
        assertTrue(extent.second > 0)
    }
}
