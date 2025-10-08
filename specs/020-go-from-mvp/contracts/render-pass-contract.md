# Render Pass Manager Contract

**API**: `io.kreekt.renderer.RenderPassManager`
**Type**: expect interface
**Platforms**: JVM (Vulkan), JS (WebGPU)

## Interface Definition

```kotlin
package io.kreekt.renderer

/**
 * Render pass manager for recording drawing commands.
 *
 * Provides cross-platform render pass recording for clearing framebuffer
 * and executing draw calls.
 */
expect interface RenderPassManager {
    /**
     * Begin render pass with clear color.
     *
     * @param clearColor Framebuffer clear color (RGBA, 0.0-1.0)
     * @param framebuffer Platform-specific framebuffer handle
     * @throws RenderPassException if render pass already active
     */
    fun beginRenderPass(clearColor: Color, framebuffer: FramebufferHandle)

    /**
     * Bind graphics pipeline.
     *
     * @param pipeline Platform-specific pipeline handle
     * @throws IllegalStateException if no active render pass
     */
    fun bindPipeline(pipeline: PipelineHandle)

    /**
     * Bind vertex buffer to slot.
     *
     * @param buffer Vertex buffer handle
     * @param slot Binding slot (default 0)
     * @throws InvalidBufferException if buffer invalid
     */
    fun bindVertexBuffer(buffer: BufferHandle, slot: Int = 0)

    /**
     * Bind index buffer.
     *
     * @param buffer Index buffer handle
     * @throws InvalidBufferException if buffer invalid
     */
    fun bindIndexBuffer(buffer: BufferHandle)

    /**
     * Bind uniform buffer to group and binding.
     *
     * @param buffer Uniform buffer handle
     * @param group Binding group (default 0)
     * @param binding Binding index (default 0)
     * @throws InvalidBufferException if buffer invalid
     */
    fun bindUniformBuffer(buffer: BufferHandle, group: Int = 0, binding: Int = 0)

    /**
     * Draw indexed primitives.
     *
     * @param indexCount Number of indices to draw
     * @param firstIndex First index to start drawing from
     * @param instanceCount Number of instances (1 for non-instanced)
     * @throws IllegalStateException if no pipeline or buffers bound
     */
    fun drawIndexed(indexCount: Int, firstIndex: Int = 0, instanceCount: Int = 1)

    /**
     * End render pass.
     *
     * @throws IllegalStateException if no active render pass
     */
    fun endRenderPass()
}
```

## Contract Tests

### Test 1: Render Pass Lifecycle

```kotlin
@Test
fun testRenderPassLifecycle_validUsage_succeeds() {
    // GIVEN: Valid clear color and framebuffer
    val clearColor = Color(0.53f, 0.81f, 0.92f, 1.0f) // Sky blue

    // WHEN: Begin render pass
    renderPassManager.beginRenderPass(clearColor, framebuffer)

    // THEN: No exception thrown

    // WHEN: End render pass
    renderPassManager.endRenderPass()

    // THEN: No exception thrown
}
```

### Test 2: Begin Without End

```kotlin
@Test
fun testBeginRenderPass_alreadyActive_throwsException() {
    // GIVEN: Active render pass
    renderPassManager.beginRenderPass(Color(0f, 0f, 0f, 1f), framebuffer)

    // WHEN/THEN: Beginning again throws RenderPassException
    assertFailsWith<RenderPassException> {
        renderPassManager.beginRenderPass(Color(0f, 0f, 0f, 1f), framebuffer)
    }
}
```

### Test 3: Draw Call Complete Flow

```kotlin
@Test
fun testDrawIndexed_completeFlow_succeeds() {
    // GIVEN: Active render pass with bound resources
    renderPassManager.beginRenderPass(Color(0.53f, 0.81f, 0.92f, 1.0f), framebuffer)
    renderPassManager.bindPipeline(pipeline)
    renderPassManager.bindVertexBuffer(vertexBuffer)
    renderPassManager.bindIndexBuffer(indexBuffer)
    renderPassManager.bindUniformBuffer(uniformBuffer)

    // WHEN: Draw indexed
    renderPassManager.drawIndexed(indexCount = 3, firstIndex = 0, instanceCount = 1)

    // THEN: No exception thrown
    renderPassManager.endRenderPass()
}
```

## Error Handling

- `RenderPassException`: Render pass already active or not active
- `IllegalStateException`: Invalid render pass state (no pipeline/buffers bound)
- `InvalidBufferException`: Buffer handle invalid

## Performance Requirements

- **beginRenderPass**: < 1ms
- **bindPipeline**: < 0.5ms
- **bindVertexBuffer**: < 0.5ms
- **drawIndexed**: < 0.1ms per call
- **endRenderPass**: < 1ms

---

**Contract Version**: 1.0
**Status**: Ready for implementation
