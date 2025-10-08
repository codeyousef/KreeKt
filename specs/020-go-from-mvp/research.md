# Technical Research: Feature 020

**Feature**: Production-Ready Renderer with Full VoxelCraft Integration
**Date**: 2025-10-07
**Status**: Complete

## Overview

Feature 020 builds on the solid foundation established by Feature 019 MVP. All major architectural decisions were made
in Feature 019 (graphics APIs, shader language, multiplatform pattern). This research document focuses on the remaining
technical decisions needed for full rendering implementation.

## Research Findings

### R1: Vulkan Buffer Management

**Decision**: Use VkBuffer with manual memory management via LWJGL

**Rationale**:

- VMA (Vulkan Memory Allocator) adds external dependency (not included in LWJGL)
- Manual VkDeviceMemory management is sufficient for VoxelCraft's simple use case
- Explicit control over memory allocation teaches better understanding of Vulkan
- LWJGL 3.3.6 provides all necessary Vulkan buffer APIs

**Implementation Strategy**:

```kotlin
// VulkanBufferManager.kt
class VulkanBufferManager(private val device: VkDevice, private val physicalDevice: VkPhysicalDevice) {
    fun createVertexBuffer(data: FloatArray): Long {
        // 1. Create VkBuffer with VERTEX_BUFFER_BIT usage
        // 2. Get memory requirements (vkGetBufferMemoryRequirements)
        // 3. Find memory type (HOST_VISIBLE | HOST_COHERENT)
        // 4. Allocate VkDeviceMemory (vkAllocateMemory)
        // 5. Bind buffer to memory (vkBindBufferMemory)
        // 6. Map memory, copy data, unmap (vkMapMemory, memcpy, vkUnmapMemory)
        // 7. Return buffer handle
    }
}
```

**Alternatives Considered**:

- **VMA**: Rejected due to external dependency, overkill for simple use case
- **Staging Buffers**: Deferred to post-MVP optimization (direct upload sufficient for VoxelCraft)

**References**:

- LWJGL Vulkan Tutorial: https://github.com/LWJGL/lwjgl3-wiki/wiki/2.6.1.-Vulkan-Tutorial
- Vulkan Specification: https://registry.khronos.org/vulkan/specs/1.3/html/

---

### R2: WebGPU Buffer Management

**Decision**: Use GPUDevice.createBuffer() with appropriate usage flags

**Rationale**:

- WebGPU's buffer API is higher-level than Vulkan (no manual memory management)
- Browser handles memory allocation, defragmentation automatically
- Usage flags (VERTEX, INDEX, UNIFORM, COPY_DST) sufficient for all VoxelCraft needs
- writeBuffer() provides simple data upload

**Implementation Strategy**:

```kotlin
// WebGPUBufferManager.kt
class WebGPUBufferManager(private val device: dynamic) {
    fun createVertexBuffer(data: Float32Array): dynamic {
        // 1. Create GPUBuffer with usage: VERTEX | COPY_DST
        // 2. Write data via device.queue.writeBuffer()
        // 3. Return GPUBuffer
    }
}
```

**Alternatives Considered**:

- **getMappedRange()**: Rejected for initial upload (writeBuffer simpler, same performance)
- **Usage Flags**: COPY_DST required for writeBuffer(), VERTEX/INDEX/UNIFORM for binding

**References**:

- WebGPU Specification: https://gpuweb.github.io/gpuweb/
- MDN WebGPU: https://developer.mozilla.org/en-US/docs/Web/API/WebGPU_API

---

### R3: Render Pass Design

**Decision**: Single render pass per frame with load op CLEAR, store op STORE

**Rationale**:

- VoxelCraft uses opaque geometry only (no transparency, no multi-pass effects)
- Single render pass minimizes GPU overhead
- Clear to sky blue (0.53, 0.81, 0.92, 1.0) matches VoxelCraft aesthetic
- No depth buffer needed for MVP (can add later for transparency)

**Vulkan Implementation**:

```kotlin
// VulkanRenderPass.kt
class VulkanRenderPass(private val device: VkDevice) {
    fun create(swapchainFormat: Int): Long {
        // 1. Create VkAttachmentDescription (color attachment)
        //    - format: swapchainFormat
        //    - loadOp: VK_ATTACHMENT_LOAD_OP_CLEAR
        //    - storeOp: VK_ATTACHMENT_STORE_OP_STORE
        //    - initialLayout: VK_IMAGE_LAYOUT_UNDEFINED
        //    - finalLayout: VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
        // 2. Create VkSubpassDescription (graphics subpass)
        // 3. Create VkRenderPass (vkCreateRenderPass)
        // 4. Return render pass handle
    }
}
```

**WebGPU Implementation**:

```kotlin
// WebGPURenderPass.kt
fun beginRenderPass(encoder: dynamic, view: dynamic, clearColor: Color) {
    val renderPassDescriptor = js("{}")
    renderPassDescriptor.colorAttachments = js("([{
        view: view,
        clearValue: { r: ${clearColor.r}, g: ${clearColor.g}, b: ${clearColor.b}, a: ${clearColor.a} },
        loadOp: 'clear',
        storeOp: 'store'
    }])")
    return encoder.beginRenderPass(renderPassDescriptor)
}
```

**Alternatives Considered**:

- **Depth Buffer**: Deferred to Phase 2-13 (not needed for opaque voxels)
- **Multi-Pass**: Deferred to Phase 2-13 (no transparency or post-processing)

---

### R4: Swapchain Management

**Decision**: Double/triple buffering with FIFO present mode (vsync)

**Rationale**:

- FIFO (vsync) prevents tearing, matches RendererConfig.vsync default from Feature 019
- Double buffering sufficient for 60 FPS, triple buffering adds latency tolerance
- Match Feature 019 constitutional requirement: smooth presentation, no tearing

**Vulkan Implementation**:

```kotlin
// VulkanSwapchain.kt
class VulkanSwapchain(private val device: VkDevice, private val surface: Long) {
    fun create(width: Int, height: Int): Long {
        // 1. Query surface capabilities (vkGetPhysicalDeviceSurfaceCapabilitiesKHR)
        // 2. Choose image count (min 2, max 3)
        // 3. Choose surface format (prefer B8G8R8A8_UNORM or B8G8R8A8_SRGB)
        // 4. Choose present mode (prefer VK_PRESENT_MODE_FIFO_KHR for vsync)
        // 5. Create VkSwapchainKHR (vkCreateSwapchainKHR)
        // 6. Get swapchain images (vkGetSwapchainImagesKHR)
        // 7. Create image views for each swapchain image
        // 8. Return swapchain handle
    }

    fun recreate(width: Int, height: Int) {
        // 1. Wait for device idle (vkDeviceWaitIdle)
        // 2. Destroy old swapchain (vkDestroySwapchainKHR)
        // 3. Create new swapchain with updated extent
    }
}
```

**WebGPU Implementation**:

```kotlin
// WebGPUSwapchain.kt (simplified - canvas context handles swapchain)
class WebGPUSwapchain(private val canvas: HTMLCanvasElement, private val context: dynamic) {
    fun configure(device: dynamic, format: String) {
        context.configure(js("{
            device: device,
            format: format,
            usage: GPUTextureUsage.RENDER_ATTACHMENT,
            alphaMode: 'opaque'
        }"))
    }

    fun getCurrentTexture(): dynamic {
        return context.getCurrentTexture()
    }

    fun recreate(width: Int, height: Int) {
        // WebGPU automatically handles canvas resize
        // Just update canvas.width and canvas.height
        canvas.width = width
        canvas.height = height
    }
}
```

**Alternatives Considered**:

- **MAILBOX Present Mode**: Rejected for MVP (FIFO sufficient, less GPU load)
- **Manual Vsync**: Rejected (platform-provided vsync more reliable)

---

### R5: Uniform Buffer Updates

**Decision**: Single uniform buffer with 3× mat4x4 (model, view, projection) = 192 bytes

**Rationale**:

- VoxelCraft needs model matrix per chunk, view/projection per frame
- 192 bytes fits in single uniform buffer (all GPUs support ≥256 byte uniforms)
- Update view/projection once per frame, model matrix per draw call
- Alignment: mat4x4 requires 16-byte alignment (satisfied by 64 bytes per matrix)

**Implementation Strategy**:

```kotlin
// Uniform buffer layout (WGSL/SPIR-V):
struct Uniforms {
    modelViewProjection: mat4x4<f32>,  // 64 bytes, offset 0
    // Future: separate model, view, projection for more flexibility
}

// Vulkan update:
fun updateUniformBuffer(buffer: Long, mvpMatrix: Matrix4) {
    vkMapMemory(device, bufferMemory, 0, 64, 0, pData)
    memPutFloat(pData, 0, mvpMatrix.m00)  // Copy all 16 floats
    // ... (16 × 4 bytes = 64 bytes)
    vkUnmapMemory(device, bufferMemory)
}

// WebGPU update:
fun updateUniformBuffer(buffer: dynamic, mvpMatrix: Matrix4) {
    val data = Float32Array(16)
    data[0] = mvpMatrix.m00
    // ... (all 16 elements)
    device.queue.writeBuffer(buffer, 0, data)
}
```

**Alternatives Considered**:

- **Push Constants**: Vulkan supports, but WebGPU doesn't (use uniform buffers for cross-platform)
- **Separate MVP Matrices**: Deferred to optimization (single MVP sufficient for VoxelCraft)

---

### R6: VoxelCraft Integration Strategy

**Decision**: Replace OpenGL/WebGL rendering with Vulkan/WebGPU using new BufferManager APIs

**Rationale**:

- Feature 019 established RendererFactory initialization (already integrated)
- Need to replace `glGenBuffers`, `glBufferData`, `glDrawElements` with new APIs
- VoxelCraft already generates chunk meshes (ChunkMeshGenerator) - just need GPU upload

**Implementation Steps**:

1. Update `VoxelWorld.kt` to use `BufferManager.createVertexBuffer()` instead of OpenGL
2. Update rendering loop to use `RenderPassManager.draw Indexed()` instead of `glDrawElements`
3. Update camera matrices to use `BufferManager.updateUniformBuffer()` for MVP
4. Remove all OpenGL/WebGL API calls from JVM/JS Main files

**Before (OpenGL)**:

```kotlin
// MainJvm.kt (OLD)
val vbo = glGenBuffers()
glBindBuffer(GL_ARRAY_BUFFER, vbo)
glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)
glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0)
```

**After (Vulkan)**:

```kotlin
// MainJvm.kt (NEW)
val vertexBuffer = bufferManager.createVertexBuffer(vertexData)
val indexBuffer = bufferManager.createIndexBuffer(indexData)
renderPassManager.bindVertexBuffer(vertexBuffer)
renderPassManager.bindIndexBuffer(indexBuffer)
renderPassManager.drawIndexed(indexCount)
```

---

## Research Summary

All technical decisions for Feature 020 are now resolved:

| Research Area          | Decision                 | Confidence |
|------------------------|--------------------------|------------|
| Vulkan Buffers         | Manual VkDeviceMemory    | ✅ High     |
| WebGPU Buffers         | GPUDevice.createBuffer() | ✅ High     |
| Render Pass            | Single pass, clear+draw  | ✅ High     |
| Swapchain              | FIFO vsync, 2-3 images   | ✅ High     |
| Uniform Buffers        | Single 192-byte buffer   | ✅ High     |
| VoxelCraft Integration | Replace GL with new APIs | ✅ High     |

**No remaining unknowns or NEEDS CLARIFICATION.**

## References

1. **Vulkan Specification**: https://registry.khronos.org/vulkan/specs/1.3/html/
2. **WebGPU Specification**: https://gpuweb.github.io/gpuweb/
3. **LWJGL Documentation**: https://www.lwjgl.org/guide
4. **Feature 019 Implementation**: `/mnt/d/Projects/KMP/KreeKt/specs/019-we-should-not/`
5. **VoxelCraft Source**: `/mnt/d/Projects/KMP/KreeKt/examples/voxelcraft/`
6. **WGSL Shader**: `/mnt/d/Projects/KMP/KreeKt/src/commonMain/resources/shaders/basic.wgsl`

---

**Research Complete**: ✅ Ready for Phase 1 (Design & Contracts)
