# Buffer Manager Contract

**API**: `io.kreekt.renderer.BufferManager`
**Type**: expect interface
**Platforms**: JVM (Vulkan), JS (WebGPU)

## Interface Definition

```kotlin
package io.kreekt.renderer

/**
 * Buffer manager for creating and managing GPU buffers.
 *
 * Provides cross-platform buffer creation for vertex data, index data,
 * and uniform data (transformation matrices).
 */
expect interface BufferManager {
    /**
     * Create vertex buffer from float array (interleaved position + color).
     *
     * Data layout: [x, y, z, r, g, b, x, y, z, r, g, b, ...]
     * Stride: 24 bytes (6 floats × 4 bytes)
     *
     * @param data Vertex data (position + color, interleaved)
     * @return Buffer handle for GPU buffer
     * @throws IllegalArgumentException if data is empty or invalid stride
     */
    fun createVertexBuffer(data: FloatArray): BufferHandle

    /**
     * Create index buffer from int array.
     *
     * @param data Triangle indices (must be multiple of 3)
     * @return Buffer handle for GPU buffer
     * @throws IllegalArgumentException if data is empty or not triangles
     */
    fun createIndexBuffer(data: IntArray): BufferHandle

    /**
     * Create uniform buffer with fixed size.
     *
     * Size must be at least 64 bytes (1× mat4x4) for MVP matrix.
     *
     * @param sizeBytes Buffer size in bytes (minimum 64)
     * @return Buffer handle for GPU buffer
     * @throws IllegalArgumentException if sizeBytes < 64
     */
    fun createUniformBuffer(sizeBytes: Int): BufferHandle

    /**
     * Update uniform buffer data (transformation matrices).
     *
     * @param handle Buffer handle from createUniformBuffer()
     * @param data Matrix data as byte array (64 bytes for mat4x4)
     * @param offset Write offset in bytes (must be 16-byte aligned)
     * @throws InvalidBufferException if handle is invalid
     * @throws IllegalArgumentException if offset not aligned or data too large
     */
    fun updateUniformBuffer(handle: BufferHandle, data: ByteArray, offset: Int = 0)

    /**
     * Destroy buffer and release GPU memory.
     *
     * @param handle Buffer handle to destroy
     * @throws InvalidBufferException if handle already destroyed
     */
    fun destroyBuffer(handle: BufferHandle)
}
```

## Data Types

```kotlin
/**
 * Platform-agnostic GPU buffer handle.
 *
 * JVM: Wraps VkBuffer (Long)
 * JS: Wraps GPUBuffer (dynamic)
 */
data class BufferHandle(
    val handle: Any,
    val size: Int,
    val usage: BufferUsage
) {
    fun isValid(): Boolean = handle != null && size > 0
}

/**
 * Buffer usage flags.
 */
enum class BufferUsage {
    VERTEX,   // Vertex buffer (position + attributes)
    INDEX,    // Index buffer (triangle indices)
    UNIFORM   // Uniform buffer (shader constants)
}
```

## Contract Tests

**File**: `src/commonTest/kotlin/io/kreekt/renderer/BufferManagerTest.kt`

### Test 1: Create Vertex Buffer (Happy Path)

```kotlin
@Test
fun testCreateVertexBuffer_validData_returnsHandle() {
    // GIVEN: Valid vertex data (3 vertices with position + color)
    val vertexData = floatArrayOf(
        // Vertex 0: position (0,0,0), color (1,0,0) red
        0f, 0f, 0f,  1f, 0f, 0f,
        // Vertex 1: position (1,0,0), color (0,1,0) green
        1f, 0f, 0f,  0f, 1f, 0f,
        // Vertex 2: position (0,1,0), color (0,0,1) blue
        0f, 1f, 0f,  0f, 0f, 1f
    )

    // WHEN: Create vertex buffer
    val handle = bufferManager.createVertexBuffer(vertexData)

    // THEN: Handle is valid
    assertNotNull(handle)
    assertTrue(handle.isValid())
    assertEquals(BufferUsage.VERTEX, handle.usage)
    assertEquals(vertexData.size * 4, handle.size) // 18 floats × 4 bytes = 72 bytes
}
```

### Test 2: Create Vertex Buffer (Empty Data)

```kotlin
@Test
fun testCreateVertexBuffer_emptyData_throwsException() {
    // GIVEN: Empty vertex data
    val vertexData = floatArrayOf()

    // WHEN/THEN: Creating buffer throws IllegalArgumentException
    assertFailsWith<IllegalArgumentException> {
        bufferManager.createVertexBuffer(vertexData)
    }
}
```

### Test 3: Create Index Buffer (Happy Path)

```kotlin
@Test
fun testCreateIndexBuffer_validData_returnsHandle() {
    // GIVEN: Valid index data (2 triangles = 6 indices)
    val indexData = intArrayOf(0, 1, 2,  0, 2, 3)

    // WHEN: Create index buffer
    val handle = bufferManager.createIndexBuffer(indexData)

    // THEN: Handle is valid
    assertNotNull(handle)
    assertTrue(handle.isValid())
    assertEquals(BufferUsage.INDEX, handle.usage)
    assertEquals(indexData.size * 4, handle.size) // 6 ints × 4 bytes = 24 bytes
}
```

### Test 4: Create Index Buffer (Invalid Triangle Count)

```kotlin
@Test
fun testCreateIndexBuffer_notTriangles_throwsException() {
    // GIVEN: Invalid index data (not multiple of 3)
    val indexData = intArrayOf(0, 1)

    // WHEN/THEN: Creating buffer throws IllegalArgumentException
    assertFailsWith<IllegalArgumentException> {
        bufferManager.createIndexBuffer(indexData)
    }
}
```

### Test 5: Create Uniform Buffer (Happy Path)

```kotlin
@Test
fun testCreateUniformBuffer_validSize_returnsHandle() {
    // GIVEN: Valid uniform buffer size (64 bytes for mat4x4)
    val sizeBytes = 64

    // WHEN: Create uniform buffer
    val handle = bufferManager.createUniformBuffer(sizeBytes)

    // THEN: Handle is valid
    assertNotNull(handle)
    assertTrue(handle.isValid())
    assertEquals(BufferUsage.UNIFORM, handle.usage)
    assertEquals(sizeBytes, handle.size)
}
```

### Test 6: Create Uniform Buffer (Size Too Small)

```kotlin
@Test
fun testCreateUniformBuffer_sizeTooSmall_throwsException() {
    // GIVEN: Uniform buffer size too small (< 64 bytes)
    val sizeBytes = 32

    // WHEN/THEN: Creating buffer throws IllegalArgumentException
    assertFailsWith<IllegalArgumentException> {
        bufferManager.createUniformBuffer(sizeBytes)
    }
}
```

### Test 7: Update Uniform Buffer (Happy Path)

```kotlin
@Test
fun testUpdateUniformBuffer_validData_succeeds() {
    // GIVEN: Created uniform buffer
    val handle = bufferManager.createUniformBuffer(64)

    // AND: Valid matrix data (identity matrix)
    val matrixData = ByteArray(64).apply {
        // Identity matrix: diagonal = 1.0
        putFloat(0, 1.0f)   // m00
        putFloat(20, 1.0f)  // m11
        putFloat(40, 1.0f)  // m22
        putFloat(60, 1.0f)  // m33
    }

    // WHEN: Update uniform buffer
    bufferManager.updateUniformBuffer(handle, matrixData, offset = 0)

    // THEN: No exception thrown
}
```

### Test 8: Update Uniform Buffer (Invalid Handle)

```kotlin
@Test
fun testUpdateUniformBuffer_invalidHandle_throwsException() {
    // GIVEN: Invalid buffer handle
    val invalidHandle = BufferHandle(null, 0, BufferUsage.UNIFORM)
    val data = ByteArray(64)

    // WHEN/THEN: Updating buffer throws InvalidBufferException
    assertFailsWith<InvalidBufferException> {
        bufferManager.updateUniformBuffer(invalidHandle, data)
    }
}
```

### Test 9: Destroy Buffer (Happy Path)

```kotlin
@Test
fun testDestroyBuffer_validHandle_succeeds() {
    // GIVEN: Created buffer
    val handle = bufferManager.createVertexBuffer(floatArrayOf(0f, 0f, 0f,  1f, 0f, 0f))

    // WHEN: Destroy buffer
    bufferManager.destroyBuffer(handle)

    // THEN: No exception thrown
    // AND: Buffer no longer valid
    assertFalse(handle.isValid())
}
```

### Test 10: Destroy Buffer (Already Destroyed)

```kotlin
@Test
fun testDestroyBuffer_alreadyDestroyed_throwsException() {
    // GIVEN: Created and destroyed buffer
    val handle = bufferManager.createVertexBuffer(floatArrayOf(0f, 0f, 0f,  1f, 0f, 0f))
    bufferManager.destroyBuffer(handle)

    // WHEN/THEN: Destroying again throws InvalidBufferException
    assertFailsWith<InvalidBufferException> {
        bufferManager.destroyBuffer(handle)
    }
}
```

## Platform Implementations

### Vulkan (JVM)

```kotlin
// src/jvmMain/kotlin/io/kreekt/renderer/vulkan/VulkanBufferManager.kt
actual class VulkanBufferManager(
    private val device: VkDevice,
    private val physicalDevice: VkPhysicalDevice
) : BufferManager {

    actual override fun createVertexBuffer(data: FloatArray): BufferHandle {
        // 1. Create VkBuffer with VERTEX_BUFFER_BIT
        // 2. Allocate VkDeviceMemory (HOST_VISIBLE | HOST_COHERENT)
        // 3. Map, copy data, unmap
        // 4. Return BufferHandle with VkBuffer handle
    }

    // ... other implementations
}
```

### WebGPU (JS)

```kotlin
// src/jsMain/kotlin/io/kreekt/renderer/webgpu/WebGPUBufferManager.kt
actual class WebGPUBufferManager(
    private val device: dynamic
) : BufferManager {

    actual override fun createVertexBuffer(data: FloatArray): BufferHandle {
        // 1. Convert FloatArray to Float32Array
        // 2. Create GPUBuffer with usage: VERTEX | COPY_DST
        // 3. Write data via device.queue.writeBuffer()
        // 4. Return BufferHandle with GPUBuffer handle
    }

    // ... other implementations
}
```

## Error Handling

### Exceptions

- `IllegalArgumentException`: Invalid input parameters (empty data, size too small, misalignment)
- `InvalidBufferException`: Invalid buffer handle (destroyed or corrupted)
- `OutOfMemoryException`: GPU memory allocation failed
- `RendererInitializationException`: Renderer not initialized

### Error Messages

All error messages MUST include:

- Parameter name and invalid value
- Expected constraints
- Platform-specific error codes (if available)

Example:

```
IllegalArgumentException: vertexData.size must be multiple of 6 (position + color), got 5
```

## Performance Requirements

- **createVertexBuffer**: < 5ms for 10,000 vertices (typical chunk mesh)
- **createIndexBuffer**: < 3ms for 15,000 indices (typical chunk mesh)
- **createUniformBuffer**: < 1ms (small allocation)
- **updateUniformBuffer**: < 1ms for 64-byte matrix (per-frame update)
- **destroyBuffer**: < 1ms (deferred cleanup acceptable)

## Integration Example

```kotlin
// VoxelCraft chunk upload
fun uploadChunkMesh(meshData: ChunkMeshData) {
    // Create vertex buffer
    val vertexBuffer = bufferManager.createVertexBuffer(meshData.vertices)

    // Create index buffer
    val indexBuffer = bufferManager.createIndexBuffer(meshData.indices)

    // Create uniform buffer for MVP matrix
    val uniformBuffer = bufferManager.createUniformBuffer(64)

    // Update MVP matrix
    val mvpMatrix = camera.projectionMatrix * camera.viewMatrix * chunk.modelMatrix
    bufferManager.updateUniformBuffer(uniformBuffer, mvpMatrix.toByteArray())

    // Store handles for rendering
    return RenderableChunk(vertexBuffer, indexBuffer, uniformBuffer, meshData.triangleCount)
}
```

---

**Contract Version**: 1.0
**Last Updated**: 2025-10-07
**Status**: Ready for implementation
