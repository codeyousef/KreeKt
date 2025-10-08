# Tasks: Production-Ready Renderer with Full VoxelCraft Integration

**Feature**: 020-go-from-mvp
**Input**: Design documents from `/mnt/d/Projects/KMP/KreeKt/specs/020-go-from-mvp/`
**Prerequisites**: plan.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

## Execution Flow (main)

```
1. Load plan.md from feature directory ✅
   → Tech stack: Kotlin Multiplatform, LWJGL 3.3.6 (JVM), WebGPU (JS)
   → Structure: expect/actual pattern, commonMain/jvmMain/jsMain
2. Load design documents ✅
   → data-model.md: 11 entities (BufferHandle, VertexBuffer, IndexBuffer, etc.)
   → contracts/: 3 contract files (BufferManager, RenderPassManager, SwapchainManager)
   → research.md: Vulkan/WebGPU buffer management decisions
3. Generate tasks by category ✅
   → Tests: 3 contract test files
   → Core: 15 platform implementations (6 Vulkan + 6 WebGPU + 3 common)
   → Integration: 4 renderer integration tasks
   → VoxelCraft: 4 migration tasks
   → Validation: 7 test execution tasks
   → Polish: 4 production readiness tasks
4. Apply task rules ✅
   → Different files = [P] for parallel
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001-T034) ✅
6. Generate dependency graph ✅
7. Validate task completeness ✅
8. Return: SUCCESS (34 tasks ready for execution)
```

## Format: `[ID] [P?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions

- **Kotlin Multiplatform structure**:
    - `src/commonMain/kotlin/` - Common interfaces and data models
    - `src/jvmMain/kotlin/` - JVM/Vulkan implementations
    - `src/jsMain/kotlin/` - JS/WebGPU implementations
    - `src/commonTest/kotlin/` - Cross-platform contract tests
    - `examples/voxelcraft/src/` - VoxelCraft example application

---

## Phase 3.1: Setup

- [x] **T001**: Verify Feature 019 MVP foundation complete ✅
    - Check all Feature 019 contract tests compile (`src/commonTest/kotlin/io/kreekt/renderer/`)
    - Verify expect interfaces exist (Renderer, RenderSurface, RendererFactory)
    - Confirm shader pipeline files present (`src/commonMain/resources/shaders/basic.wgsl`)
    - Validate LWJGL 3.3.6 and WebGPU dependencies in `build.gradle.kts`
    - **Prerequisites**: Feature 019 complete
    - **Output**: Validation report confirming MVP foundation ready

---

## Phase 3.2: Contract Tests (TDD Red Phase) ⚠️ MUST COMPLETE BEFORE 3.3

**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

- [x] **T002** [P]: Write BufferManager contract tests ✅
    - **File**: `src/commonTest/kotlin/io/kreekt/renderer/BufferManagerTest.kt`
    - **Tests** (10 total):
        1. `testCreateVertexBuffer_validData_returnsHandle()` - Happy path vertex buffer creation
        2. `testCreateVertexBuffer_emptyData_throwsException()` - Empty data validation
        3. `testCreateIndexBuffer_validData_returnsHandle()` - Happy path index buffer creation
        4. `testCreateIndexBuffer_notTriangles_throwsException()` - Triangle count validation
        5. `testCreateUniformBuffer_validSize_returnsHandle()` - Happy path uniform buffer (64 bytes)
        6. `testCreateUniformBuffer_sizeTooSmall_throwsException()` - Minimum size validation
        7. `testUpdateUniformBuffer_validData_succeeds()` - Matrix update
        8. `testUpdateUniformBuffer_invalidHandle_throwsException()` - Invalid handle detection
        9. `testDestroyBuffer_validHandle_succeeds()` - Buffer destruction
        10. `testDestroyBuffer_alreadyDestroyed_throwsException()` - Double-destroy detection
    - **Reference**: `specs/020-go-from-mvp/contracts/buffer-manager-contract.md`
    - **Expected**: All tests fail (no implementation yet)
    - **Dependencies**: None (parallel with T003, T004)

- [x] **T003** [P]: Write RenderPassManager contract tests ✅
    - **File**: `src/commonTest/kotlin/io/kreekt/renderer/RenderPassManagerTest.kt`
    - **Tests** (7 total):
        1. `testRenderPassLifecycle_validUsage_succeeds()` - Begin → End render pass
        2. `testBeginRenderPass_alreadyActive_throwsException()` - Duplicate begin detection
        3. `testBindPipeline_validPipeline_succeeds()` - Pipeline binding
        4. `testBindVertexBuffer_validBuffer_succeeds()` - Vertex buffer binding
        5. `testBindIndexBuffer_validBuffer_succeeds()` - Index buffer binding
        6. `testDrawIndexed_completeFlow_succeeds()` - Full draw call sequence
        7. `testDrawIndexed_noPipeline_throwsException()` - Missing pipeline detection
    - **Reference**: `specs/020-go-from-mvp/contracts/render-pass-contract.md`
    - **Expected**: All tests fail (no implementation yet)
    - **Dependencies**: None (parallel with T002, T004)

- [x] **T004** [P]: Write SwapchainManager contract tests ✅
    - **File**: `src/commonTest/kotlin/io/kreekt/renderer/SwapchainManagerTest.kt`
    - **Tests** (4 total):
        1. `testAcquirePresent_normalFlow_succeeds()` - Acquire → Present cycle
        2. `testRecreateSwapchain_validDimensions_succeeds()` - Window resize handling
        3. `testRecreateSwapchain_invalidDimensions_throwsException()` - Zero/negative dimension validation
        4. `testGetExtent_returnsCurrentDimensions()` - Extent query
    - **Reference**: `specs/020-go-from-mvp/contracts/swapchain-contract.md`
    - **Expected**: All tests fail (no implementation yet)
    - **Dependencies**: None (parallel with T002, T003)

---

## Phase 3.3: Core Implementation - JVM/Vulkan (ONLY after tests are failing)

- [x] **T005**: Implement VulkanBufferManager - Buffer creation ✅
    - **File**: `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanBufferManager.kt`
    - **Methods**:
        - `createVertexBuffer(data: FloatArray): BufferHandle` - Create VkBuffer with VERTEX_BUFFER_BIT, allocate
          VkDeviceMemory, map/copy/unmap
        - `createIndexBuffer(data: IntArray): BufferHandle` - Create VkBuffer with INDEX_BUFFER_BIT, allocate memory
        - `createUniformBuffer(sizeBytes: Int): BufferHandle` - Create VkBuffer with UNIFORM_BUFFER_BIT (minimum 64
          bytes)
    - **Implementation Details**:
        - Use `vkCreateBuffer()` with appropriate usage flags
        - Query memory requirements via `vkGetBufferMemoryRequirements()`
        - Find memory type with HOST_VISIBLE | HOST_COHERENT properties
        - Allocate via `vkAllocateMemory()`, bind via `vkBindBufferMemory()`
        - Map memory via `vkMapMemory()`, copy data, unmap via `vkUnmapMemory()`
    - **Error Handling**: IllegalArgumentException for empty data, OutOfMemoryException for allocation failures
    - **Reference**: `specs/020-go-from-mvp/research.md` (R1: Vulkan Buffer Management)
    - **Dependencies**: T002 (BufferManagerTest must fail first)
    - **Validation**: T002 tests 1, 3, 5 should pass

- [x] **T006**: Implement VulkanBufferManager - Buffer updates and cleanup ✅
    - **File**: `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanBufferManager.kt`
    - **Methods**:
        - `updateUniformBuffer(handle: BufferHandle, data: ByteArray, offset: Int)` - Map uniform buffer memory, copy
          matrix data, unmap
        - `destroyBuffer(handle: BufferHandle)` - Destroy VkBuffer, free VkDeviceMemory
    - **Implementation Details**:
        - Validate handle not destroyed (track in internal state)
        - Validate offset alignment (16-byte aligned for mat4x4)
        - Use `vkMapMemory()` → `memcpy()` → `vkUnmapMemory()` for updates
        - Call `vkDestroyBuffer()` and `vkFreeMemory()` for cleanup
    - **Error Handling**: InvalidBufferException for destroyed handles, IllegalArgumentException for misalignment
    - **Dependencies**: T005 (buffer creation must work first)
    - **Validation**: T002 tests 7, 8, 9, 10 should pass

- [x] **T007**: Implement VulkanRenderPassManager - Render pass lifecycle ✅
    - **File**: `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanRenderPassManager.kt`
    - **Methods**:
        - `beginRenderPass(clearColor: Color, framebuffer: FramebufferHandle)` - Begin VkRenderPass with clear operation
        - `endRenderPass()` - End VkRenderPass
    - **Implementation Details**:
        - Track render pass state (active/inactive)
        - Create VkRenderPassBeginInfo with clearColor converted to VkClearValue
        - Call `vkCmdBeginRenderPass()` with command buffer
        - Call `vkCmdEndRenderPass()` to finalize
    - **Error Handling**: RenderPassException if begin called twice without end
    - **Reference**: `specs/020-go-from-mvp/research.md` (R3: Render Pass Design)
    - **Dependencies**: T003 (RenderPassManagerTest must fail first)
    - **Validation**: T003 tests 1, 2 should pass

- [x] **T008**: Implement VulkanRenderPassManager - Resource binding and draw calls ✅
    - **File**: `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanRenderPassManager.kt`
    - **Methods**:
        - `bindPipeline(pipeline: PipelineHandle)` - Bind VkPipeline via `vkCmdBindPipeline()`
        - `bindVertexBuffer(buffer: BufferHandle, slot: Int)` - Bind vertex buffer via `vkCmdBindVertexBuffers()`
        - `bindIndexBuffer(buffer: BufferHandle)` - Bind index buffer via `vkCmdBindIndexBuffer()`
        - `bindUniformBuffer(buffer: BufferHandle, group: Int, binding: Int)` - Bind descriptor set for uniforms
        - `drawIndexed(indexCount: Int, firstIndex: Int, instanceCount: Int)` - Issue `vkCmdDrawIndexed()`
    - **Implementation Details**:
        - Validate render pass active before bind/draw operations
        - Track bound resources to validate draw call completeness
        - Use VkPipelineBindPoint.GRAPHICS for pipeline binding
    - **Error Handling**: IllegalStateException if no active render pass, InvalidBufferException for invalid buffers
    - **Dependencies**: T007 (render pass lifecycle must work first)
    - **Validation**: T003 tests 3, 4, 5, 6, 7 should pass

- [x] **T009**: Implement VulkanSwapchain - Creation and presentation ✅
    - **File**: `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanSwapchain.kt`
    - **Methods**:
        - `create(width: Int, height: Int): Long` - Create VkSwapchainKHR with FIFO present mode
        - `acquireNextImage(): SwapchainImage` - Acquire next image via `vkAcquireNextImageKHR()`
        - `presentImage(image: SwapchainImage)` - Present via `vkQueuePresentKHR()`
    - **Implementation Details**:
        - Query surface capabilities via `vkGetPhysicalDeviceSurfaceCapabilitiesKHR()`
        - Choose image count (2-3), format (B8G8R8A8_UNORM preferred), present mode (VK_PRESENT_MODE_FIFO_KHR)
        - Create swapchain via `vkCreateSwapchainKHR()`
        - Get swapchain images via `vkGetSwapchainImagesKHR()`, create image views
        - Use semaphores for acquire/present synchronization
    - **Error Handling**: SwapchainException for acquire/present failures
    - **Reference**: `specs/020-go-from-mvp/research.md` (R4: Swapchain Management)
    - **Dependencies**: T004 (SwapchainManagerTest must fail first)
    - **Validation**: T004 tests 1, 4 should pass

- [x] **T010**: Implement VulkanSwapchain - Resize handling ✅
    - **File**: `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanSwapchain.kt`
    - **Methods**:
        - `recreateSwapchain(width: Int, height: Int)` - Recreate swapchain with new extent
        - `getExtent(): Pair<Int, Int>` - Return current (width, height)
    - **Implementation Details**:
        - Wait for device idle via `vkDeviceWaitIdle()` before recreation
        - Destroy old swapchain via `vkDestroySwapchainKHR()`
        - Create new swapchain with updated extent
        - Recreate framebuffers for new swapchain images
    - **Error Handling**: IllegalArgumentException for width/height <= 0
    - **Dependencies**: T009 (swapchain creation must work first)
    - **Validation**: T004 tests 2, 3 should pass

---

## Phase 3.3: Core Implementation - JS/WebGPU (ONLY after tests are failing)

- [x] **T011** [P]: Implement WebGPUBufferManager - Buffer creation ✅
    - **File**: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUBufferManager.kt`
    - **Methods**:
        - `createVertexBuffer(data: FloatArray): BufferHandle` - Create GPUBuffer with usage VERTEX | COPY_DST
        - `createIndexBuffer(data: IntArray): BufferHandle` - Create GPUBuffer with usage INDEX | COPY_DST
        - `createUniformBuffer(sizeBytes: Int): BufferHandle` - Create GPUBuffer with usage UNIFORM | COPY_DST (minimum
          64 bytes)
    - **Implementation Details**:
        - Convert FloatArray to Float32Array, IntArray to Uint32Array
        - Call `device.createBuffer()` with appropriate size and usage flags
        - Write data via `device.queue.writeBuffer()`
    - **Error Handling**: Same as Vulkan (IllegalArgumentException, OutOfMemoryException)
    - **Reference**: `specs/020-go-from-mvp/research.md` (R2: WebGPU Buffer Management)
    - **Dependencies**: T002 (BufferManagerTest must fail first)
    - **Validation**: T002 tests 1, 3, 5 should pass
    - **Note**: [P] because different file than Vulkan implementation (T005-T006)

- [x] **T012** ✅: Implement WebGPUBufferManager - Buffer updates and cleanup
    - **File**: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUBufferManager.kt`
    - **Methods**:
        - `updateUniformBuffer(handle: BufferHandle, data: ByteArray, offset: Int)` - Write buffer via
          `device.queue.writeBuffer()`
        - `destroyBuffer(handle: BufferHandle)` - Destroy GPUBuffer via `buffer.destroy()`
    - **Implementation Details**:
        - Convert ByteArray to ArrayBuffer for writeBuffer()
        - Validate offset alignment (same as Vulkan)
        - Track destroyed buffers to prevent double-destroy
    - **Error Handling**: Same as Vulkan
    - **Dependencies**: T011 (buffer creation must work first)
    - **Validation**: T002 tests 7, 8, 9, 10 should pass

- [x] **T013** ✅ [P]: Implement WebGPURenderPassManager - Render pass lifecycle
    - **File**: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderPassManager.kt`
    - **Methods**:
        - `beginRenderPass(clearColor: Color, framebuffer: FramebufferHandle)` - Begin render pass with
          GPURenderPassDescriptor
        - `endRenderPass()` - End render pass
    - **Implementation Details**:
        - Create GPURenderPassDescriptor with colorAttachments[0].clearValue = clearColor
        - Set loadOp: 'clear', storeOp: 'store'
        - Call `commandEncoder.beginRenderPass(descriptor)`
        - Track render pass encoder state
    - **Error Handling**: RenderPassException if begin called twice
    - **Reference**: `specs/020-go-from-mvp/research.md` (R3: Render Pass Design)
    - **Dependencies**: T003 (RenderPassManagerTest must fail first)
    - **Validation**: T003 tests 1, 2 should pass
    - **Note**: [P] because different file than Vulkan implementation (T007-T008)

- [x] **T014** ✅: Implement WebGPURenderPassManager - Resource binding and draw calls
    - **File**: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderPassManager.kt`
    - **Methods**:
        - `bindPipeline(pipeline: PipelineHandle)` - Call `passEncoder.setPipeline()`
        - `bindVertexBuffer(buffer: BufferHandle, slot: Int)` - Call `passEncoder.setVertexBuffer(slot, buffer)`
        - `bindIndexBuffer(buffer: BufferHandle)` - Call `passEncoder.setIndexBuffer(buffer, 'uint32')`
        - `bindUniformBuffer(buffer: BufferHandle, group: Int, binding: Int)` - Bind group with
          `passEncoder.setBindGroup()`
        - `drawIndexed(indexCount: Int, firstIndex: Int, instanceCount: Int)` - Call `passEncoder.drawIndexed()`
    - **Implementation Details**:
        - Validate render pass active before operations
        - Track bound resources
    - **Error Handling**: Same as Vulkan
    - **Dependencies**: T013 (render pass lifecycle must work first)
    - **Validation**: T003 tests 3, 4, 5, 6, 7 should pass

- [x] **T015** ✅ [P]: Implement WebGPUSwapchain - Configuration and presentation
    - **File**: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUSwapchain.kt`
    - **Methods**:
        - `configure(device: dynamic, format: String)` - Configure GPUCanvasContext
        - `getCurrentTexture(): dynamic` - Get current texture via `context.getCurrentTexture()`
        - `presentImage(image: SwapchainImage)` - Implicit presentation (WebGPU auto-presents)
    - **Implementation Details**:
        - Call `context.configure()` with device, format: 'bgra8unorm', usage: RENDER_ATTACHMENT
        - Set alphaMode: 'opaque'
    - **Error Handling**: SwapchainException for configuration failures
    - **Reference**: `specs/020-go-from-mvp/research.md` (R4: Swapchain Management)
    - **Dependencies**: T004 (SwapchainManagerTest must fail first)
    - **Validation**: T004 tests 1, 4 should pass
    - **Note**: [P] because different file than Vulkan implementation (T009-T010)

- [x] **T016** ✅: Implement WebGPUSwapchain - Resize handling
    - **File**: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUSwapchain.kt`
    - **Methods**:
        - `recreateSwapchain(width: Int, height: Int)` - Update canvas size
        - `getExtent(): Pair<Int, Int>` - Return current (canvas.width, canvas.height)
    - **Implementation Details**:
        - Set `canvas.width = width`, `canvas.height = height`
        - WebGPU automatically recreates swapchain on next getCurrentTexture()
    - **Error Handling**: Same as Vulkan
    - **Dependencies**: T015 (swapchain configuration must work first)
    - **Validation**: T004 tests 2, 3 should pass

---

## Phase 3.4: Integration - Renderer.render() Implementation

- [x] **T017**: Integrate VulkanRenderer with buffer and render pass managers ✅
    - **File**: `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanRenderer.kt`
    - **Changes**:
        - Add `bufferManager: VulkanBufferManager` field
        - Add `renderPassManager: VulkanRenderPassManager` field
        - Add `swapchainManager: VulkanSwapchain` field
        - Implement `render(scene: Scene, camera: Camera)`:
            1. Acquire swapchain image via `swapchainManager.acquireNextImage()`
            2. Begin render pass via `renderPassManager.beginRenderPass(clearColor, framebuffer)`
            3. Bind pipeline via `renderPassManager.bindPipeline(basicPipeline)`
            4. For each mesh in scene:
                - Update uniform buffer with MVP matrix
                - Bind vertex/index/uniform buffers
                - Call `renderPassManager.drawIndexed(mesh.indexCount)`
            5. End render pass via `renderPassManager.endRenderPass()`
            6. Present image via `swapchainManager.presentImage(image)`
    - **Dependencies**: T006, T008, T010 (all Vulkan managers must work)
    - **Validation**: VoxelCraft should render on JVM (no visual yet, but no crashes)

- [ ] **T018**: Integrate WebGPURenderer with buffer and render pass managers
    - **File**: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt`
    - **Changes**:
        - Add `bufferManager: WebGPUBufferManager` field
        - Add `renderPassManager: WebGPURenderPassManager` field
        - Add `swapchainManager: WebGPUSwapchain` field
        - Implement `render(scene: Scene, camera: Camera)`:
            1. Get current texture via `swapchainManager.getCurrentTexture()`
            2. Begin render pass via `renderPassManager.beginRenderPass(clearColor, texture.createView())`
            3. Bind pipeline via `renderPassManager.bindPipeline(basicPipeline)`
            4. For each mesh in scene:
                - Update uniform buffer with MVP matrix
                - Bind vertex/index/uniform buffers
                - Call `renderPassManager.drawIndexed(mesh.indexCount)`
            5. End render pass via `renderPassManager.endRenderPass()`
            6. Submit command buffer and present (implicit)
    - **Dependencies**: T012, T014, T016 (all WebGPU managers must work)
    - **Validation**: VoxelCraft should render on JS (no visual yet, but no crashes)

- [x] **T019**: Implement VulkanRenderer.resize() with swapchain recreation ✅
    - **File**: `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanRenderer.kt`
    - **Changes**:
        - Update `resize(width: Int, height: Int)` method:
            1. Wait for device idle
            2. Call `swapchainManager.recreateSwapchain(width, height)`
            3. Recreate framebuffers for new swapchain images
            4. Update viewport and scissor
    - **Dependencies**: T017 (renderer integration must work first)
    - **Validation**: Window resize should work without crashes

- [ ] **T020**: Implement WebGPURenderer.resize() with swapchain recreation
    - **File**: `src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPURenderer.kt`
    - **Changes**:
        - Update `resize(width: Int, height: Int)` method:
            1. Call `swapchainManager.recreateSwapchain(width, height)`
            2. Update viewport (automatic with WebGPU)
    - **Dependencies**: T018 (renderer integration must work first)
    - **Validation**: Canvas resize should work without crashes

---

## Phase 3.5: VoxelCraft Integration

- [ ] **T021** [P]: Update VoxelWorld to upload chunks using BufferManager
    - **File**: `examples/voxelcraft/src/commonMain/kotlin/io/kreekt/examples/voxelcraft/VoxelWorld.kt`
    - **Changes**:
        - Add `bufferManager: BufferManager` parameter to VoxelWorld
        - Update `uploadChunk(chunk: Chunk)`:
            1. Generate mesh via `ChunkMeshGenerator.generateMesh(chunk)`
            2. Create vertex buffer via `bufferManager.createVertexBuffer(meshData.vertices)`
            3. Create index buffer via `bufferManager.createIndexBuffer(meshData.indices)`
            4. Store as `RenderableChunk(vertexBuffer, indexBuffer, triangleCount)`
        - Update `renderChunks(renderer: Renderer, camera: Camera)`:
            1. Update camera uniform buffer with view/projection matrices
            2. For each visible chunk:
                - Update model matrix uniform
                - Submit draw command (renderer handles actual drawing)
    - **Dependencies**: T017, T018 (both renderers must support BufferManager)
    - **Validation**: VoxelCraft chunks uploaded to GPU (no visual yet)
    - **Note**: [P] because commonMain file (no platform conflict)

- [x] **T022**: Update VoxelCraft JS Main.kt to use WebGPURenderer.render() ✅
    - **File**: `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`
    - **Changes**:
        - Remove all WebGL API calls (`gl.*` methods)
        - Update game loop to call `renderer.render(scene, camera)` instead of manual GL calls
        - Remove manual buffer binding (renderer handles internally)
        - Keep Pointer Lock API and input handling
    - **Dependencies**: T021 (VoxelWorld must upload chunks via BufferManager)
    - **Validation**: VoxelCraft JS should render terrain with WebGPU

- [x] **T023**: Update VoxelCraft JVM MainJvm.kt to use VulkanRenderer.render() ✅
    - **File**: `examples/voxelcraft/src/jvmMain/kotlin/io/kreekt/examples/voxelcraft/MainJvm.kt`
    - **Changes**:
        - Remove all OpenGL API calls (`glBindBuffer`, `glDrawElements`, etc.)
        - Update game loop to call `renderer.render(scene, camera)` instead of manual GL calls
        - Remove manual buffer binding (renderer handles internally)
        - Keep GLFW input handling
    - **Dependencies**: T021 (VoxelWorld must upload chunks via BufferManager)
    - **Validation**: VoxelCraft JVM should render terrain with Vulkan

- [x] **T024**: Remove all legacy OpenGL/WebGL code from VoxelCraft ✅
    - **Files**:
        - `examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/Main.kt`
        - `examples/voxelcraft/src/jvmMain/kotlin/io/kreekt/examples/voxelcraft/MainJvm.kt`
    - **Changes**:
        - Search for and remove all `gl.*`, `glBindBuffer`, `glDrawElements` calls
        - Remove OpenGL context initialization code (if any)
        - Remove manual shader compilation (use renderer's shader pipeline)
        - Confirm no WebGL fallback code remains in JS (only via RendererFactory)
    - **Dependencies**: T022, T023 (both platforms must use new renderers)
    - **Validation**: `grep -r "gl\." examples/voxelcraft/src/` returns no results

---

## Phase 3.6: Performance Validation

- [ ] **T025**: Run PerformanceBenchmarkTest - 100k triangles validation
    - **Command**:
      `./gradlew :kreekt-renderer:test --tests PerformanceBenchmarkTest.testPerformance_100kTriangles_60FPS`
    - **Expected**:
        - Test passes (FPS >= 60)
        - Average frame time <= 16.67ms
        - No frame drops below 30 FPS
    - **If Fails**: Profile GPU performance, check for bottlenecks, optimize draw calls
    - **Dependencies**: T024 (VoxelCraft must use new renderers)
    - **Output**: Performance report with FPS, frame time, triangle count

- [ ] **T026**: Run PerformanceBenchmarkTest - Memory usage validation
    - **Command**: `./gradlew :kreekt-renderer:test --tests PerformanceBenchmarkTest.testMemory_InitializationUsage`
    - **Expected**:
        - Memory usage < 500MB (constitutional limit)
        - No memory leaks detected
        - GPU memory properly released on dispose()
    - **If Fails**: Check for buffer leaks, validate dispose() implementation
    - **Dependencies**: T025 (performance tests must run first)
    - **Output**: Memory usage report (heap + GPU)

- [ ] **T027**: Run visual regression tests - SSIM validation
    - **Command**: `./gradlew :kreekt-renderer:test --tests VisualRegressionTest`
    - **Expected**:
        - SSIM >= 0.95 for all cross-backend comparisons (Vulkan vs WebGPU)
        - Screenshot capture works on both platforms
        - No visual artifacts detected
    - **If Fails**: Investigate rendering differences, check shader consistency, validate clear color
    - **Dependencies**: T024 (VoxelCraft must render correctly)
    - **Output**: SSIM similarity scores, screenshots in `build/visual-regression/`

---

## Phase 3.7: Test Validation (TDD Green Confirmation)

- [ ] **T028**: Run BackendDetectionTest
    - **Command**: `./gradlew :kreekt-renderer:test --tests BackendDetectionTest`
    - **Expected**: All tests pass (Vulkan detected on JVM, WebGPU detected on JS)
    - **Dependencies**: T024 (full integration complete)

- [ ] **T029**: Run RendererLifecycleTest
    - **Command**: `./gradlew :kreekt-renderer:test --tests RendererLifecycleTest`
    - **Expected**: All tests pass (initialize, render, resize, dispose)
    - **Dependencies**: T024 (full integration complete)

- [ ] **T030**: Run ErrorHandlingTest
    - **Command**: `./gradlew :kreekt-renderer:test --tests ErrorHandlingTest`
    - **Expected**: All tests pass (exceptions thrown correctly with diagnostics)
    - **Dependencies**: T024 (full integration complete)

- [ ] **T031**: Run CapabilityDetectionTest
    - **Command**: `./gradlew :kreekt-renderer:test --tests CapabilityDetectionTest`
    - **Expected**: All tests pass (GPU capabilities detected correctly)
    - **Dependencies**: T024 (full integration complete)

---

## Phase 3.8: Production Readiness

- [ ] **T032**: Remove all TODO/FIXME/STUB from rendering pipeline
    - **Files**: All files in `src/jvmMain/kotlin/io/kreekt/renderer/vulkan/` and
      `src/jsMain/kotlin/io/kreekt/renderer/webgpu/`
    - **Command**: `grep -r "TODO\|FIXME\|STUB" src/*/kotlin/io/kreekt/renderer/`
    - **Changes**:
        - Replace TODOs with full implementations or remove if obsolete
        - Document deferred features (transparency, advanced lighting) in comments
        - Ensure all placeholder code removed
    - **Expected**: grep returns no results
    - **Dependencies**: T031 (all tests must pass first)

- [ ] **T033** [P]: Add comprehensive logging to all rendering paths
    - **Files**: VulkanRenderer.kt, WebGPURenderer.kt, VulkanBufferManager.kt, WebGPUBufferManager.kt
    - **Changes**:
        - Add DEBUG-level logging for buffer creation (size, usage)
        - Add DEBUG-level logging for render pass begin/end (clearColor)
        - Add INFO-level logging for initialization (GPU device, capabilities)
        - Add ERROR-level logging for all exceptions (with diagnostics)
        - Add WARN-level logging for performance issues (FPS < 30)
    - **Reference**: FR-033 (comprehensive logging requirement)
    - **Dependencies**: T031 (all tests must pass first)
    - **Note**: [P] because different files can be logged independently

- [ ] **T034**: Validate resource cleanup in Renderer.dispose()
    - **Files**: VulkanRenderer.kt, WebGPURenderer.kt
    - **Changes**:
        - Verify all buffers destroyed via `bufferManager.destroyBuffer()`
        - Verify swapchain destroyed
        - Verify Vulkan instance/device destroyed (JVM)
        - Add dispose() logging (DEBUG level)
    - **Test**: Run `MemoryLeakTest` to verify no leaks
    - **Expected**: All GPU resources released, memory usage returns to baseline
    - **Reference**: FR-036 (resource cleanup requirement)
    - **Dependencies**: T033 (logging must be in place for debugging)

- [ ] **T035**: Update CLAUDE.md with Feature 020 implementation notes
    - **File**: `/mnt/d/Projects/KMP/KreeKt/CLAUDE.md`
    - **Changes**:
        - Add Feature 020 to "Recent Changes" section
        - Update renderer architecture description (add BufferManager, RenderPassManager, SwapchainManager)
        - Document VoxelCraft now uses Vulkan/WebGPU (not OpenGL/WebGL)
        - Add troubleshooting section for rendering issues
    - **Dependencies**: T034 (all production tasks complete)

---

## Dependencies Graph

```
Phase 3.1: Setup
T001 (Foundation verification)
  ↓
Phase 3.2: Contract Tests (TDD Red)
T002, T003, T004 (can run in parallel [P])
  ↓
Phase 3.3: Core Implementation
JVM: T005 → T006 → T007 → T008 → T009 → T010
JS:  T011 → T012 → T013 → T014 → T015 → T016 (parallel to JVM)
  ↓
Phase 3.4: Integration
T017, T018 (parallel JVM/JS integration)
  ↓
T019, T020 (resize handling)
  ↓
Phase 3.5: VoxelCraft
T021 → T022, T023 (parallel JS/JVM updates) → T024
  ↓
Phase 3.6: Performance
T025 → T026 → T027
  ↓
Phase 3.7: Validation
T028, T029, T030, T031 (can run in parallel)
  ↓
Phase 3.8: Production
T032 → T033 [P] → T034 → T035
```

---

## Parallel Execution Examples

### Contract Tests (Phase 3.2) - Run all 3 in parallel

```bash
# All tests independent, different files
Task: "Write BufferManager contract tests in src/commonTest/kotlin/io/kreekt/renderer/BufferManagerTest.kt"
Task: "Write RenderPassManager contract tests in src/commonTest/kotlin/io/kreekt/renderer/RenderPassManagerTest.kt"
Task: "Write SwapchainManager contract tests in src/commonTest/kotlin/io/kreekt/renderer/SwapchainManagerTest.kt"
```

### JVM + JS Implementation (Phase 3.3) - Run both platforms in parallel

```bash
# Vulkan and WebGPU implementations are independent
Task: "Implement VulkanBufferManager in src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanBufferManager.kt"
Task: "Implement WebGPUBufferManager in src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUBufferManager.kt"
```

### Test Validation (Phase 3.7) - Run all 4 in parallel

```bash
# All Feature 019 contract tests can run concurrently
Task: "Run BackendDetectionTest"
Task: "Run RendererLifecycleTest"
Task: "Run ErrorHandlingTest"
Task: "Run CapabilityDetectionTest"
```

---

## Notes

- **[P] tasks** = different files, no dependencies, safe for parallel execution
- **TDD Critical**: Phases 3.2 must complete before 3.3 (tests must fail before implementation)
- **Verify tests fail**: Before implementing, confirm all contract tests throw exceptions
- **Commit strategy**: Commit after each task completes and tests pass
- **Avoid**: Implementing before tests written, modifying same file in parallel

---

## Task Generation Rules Applied

1. **From Contracts**:
    - 3 contract files → 3 contract test tasks (T002-T004) [P]
    - Each API → implementation tasks (T005-T016)

2. **From Data Model**:
    - 11 entities → covered by BufferManager, RenderPassManager, SwapchainManager implementations

3. **From User Stories**:
    - VoxelCraft quickstart scenarios → VoxelCraft integration tasks (T021-T024)
    - Performance requirements → validation tasks (T025-T027)

4. **Ordering**:
    - Setup (T001) → Tests (T002-T004) → Implementation (T005-T016) → Integration (T017-T020) → VoxelCraft (T021-T024) →
      Validation (T025-T031) → Production (T032-T035)

---

## Validation Checklist

- [x] All contracts have corresponding tests (T002-T004)
- [x] All entities have implementation tasks (BufferHandle, VertexBuffer, etc.)
- [x] All tests come before implementation (Phase 3.2 before 3.3)
- [x] Parallel tasks truly independent (different files marked [P])
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] Estimated timeline: 35-45 hours (5-6 working days)

---

**Tasks Generated**: 35 tasks (T001-T035)
**Ready for Execution**: ✅ All tasks numbered, ordered, and validated
