# Swapchain Manager Contract

**API**: `io.kreekt.renderer.SwapchainManager`
**Type**: expect interface
**Platforms**: JVM (Vulkan), JS (WebGPU)

## Interface Definition

```kotlin
package io.kreekt.renderer

/**
 * Swapchain manager for presenting rendered frames.
 *
 * Provides cross-platform swapchain management for acquiring images,
 * presenting to screen, and handling window resize.
 */
expect interface SwapchainManager {
    /**
     * Acquire next swapchain image for rendering.
     *
     * Blocks until image available (vsync).
     *
     * @return Swapchain image ready for rendering
     * @throws SwapchainException if acquire fails
     */
    fun acquireNextImage(): SwapchainImage

    /**
     * Present rendered image to screen.
     *
     * @param image Swapchain image to present
     * @throws SwapchainException if present fails
     */
    fun presentImage(image: SwapchainImage)

    /**
     * Recreate swapchain on window resize.
     *
     * @param width New width in pixels (> 0)
     * @param height New height in pixels (> 0)
     * @throws IllegalArgumentException if width or height <= 0
     */
    fun recreateSwapchain(width: Int, height: Int)

    /**
     * Get current swapchain extent.
     *
     * @return Pair of (width, height) in pixels
     */
    fun getExtent(): Pair<Int, Int>
}

/**
 * Swapchain image for rendering.
 */
data class SwapchainImage(
    val handle: Any,
    val index: Int,
    val ready: Boolean
) {
    fun isReady(): Boolean = ready
}
```

## Contract Tests

### Test 1: Acquire and Present

```kotlin
@Test
fun testAcquirePresent_normalFlow_succeeds() {
    // GIVEN: Initialized swapchain
    val swapchain = createSwapchain()

    // WHEN: Acquire next image
    val image = swapchain.acquireNextImage()

    // THEN: Image is valid and ready
    assertNotNull(image)
    assertTrue(image.isReady())

    // WHEN: Present image
    swapchain.presentImage(image)

    // THEN: No exception thrown
}
```

### Test 2: Recreate Swapchain

```kotlin
@Test
fun testRecreateSwapchain_validDimensions_succeeds() {
    // GIVEN: Initialized swapchain (800×600)
    val swapchain = createSwapchain()

    // WHEN: Recreate with new dimensions (1024×768)
    swapchain.recreateSwapchain(1024, 768)

    // THEN: Extent updated
    val (width, height) = swapchain.getExtent()
    assertEquals(1024, width)
    assertEquals(768, height)
}
```

### Test 3: Invalid Dimensions

```kotlin
@Test
fun testRecreateSwapchain_invalidDimensions_throwsException() {
    // GIVEN: Initialized swapchain
    val swapchain = createSwapchain()

    // WHEN/THEN: Recreate with invalid dimensions throws
    assertFailsWith<IllegalArgumentException> {
        swapchain.recreateSwapchain(0, 600)
    }
}
```

## Error Handling

- `SwapchainException`: Swapchain acquire/present failure
- `IllegalArgumentException`: Invalid dimensions (width/height <= 0)

## Performance Requirements

- **acquireNextImage**: < 16ms (vsync timeout)
- **presentImage**: < 5ms
- **recreateSwapchain**: < 50ms (includes GPU wait)
- **getExtent**: < 0.1ms (cached value)

---

**Contract Version**: 1.0
**Status**: Ready for implementation
