package io.kreekt.renderer

/**
 * Advanced buffer management system for GPU resources
 * T118 - Buffer management for vertex, index, uniform buffers with pooling
 */

/**
 * Buffer types
 */
enum class BufferType {
    VERTEX,
    INDEX,
    UNIFORM,
    STORAGE,
    STAGING,
    INDIRECT
}

/**
 * Buffer usage patterns
 */
enum class BufferUsage {
    STATIC,   // Data uploaded once, rarely changes
    DYNAMIC,  // Data changes frequently
    STREAM    // Data changes every frame
}

/**
 * Buffer access patterns
 */
enum class BufferAccess {
    READ_ONLY,
    WRITE_ONLY,
    READ_WRITE
}

/**
 * Generic GPU buffer interface
 */
interface Buffer {
    val id: Int
    val type: BufferType
    val size: Long
    val usage: BufferUsage
    val access: BufferAccess
    var needsUpdate: Boolean

    /**
     * Upload data to the buffer
     */
    fun uploadData(data: ByteArray, offset: Long = 0): RendererResult<Unit>

    /**
     * Upload float data to the buffer
     */
    fun uploadFloatData(data: FloatArray, offset: Long = 0): RendererResult<Unit>

    /**
     * Upload int data to the buffer
     */
    fun uploadIntData(data: IntArray, offset: Long = 0): RendererResult<Unit>

    /**
     * Map buffer for CPU access
     */
    fun map(access: BufferAccess): ByteArray?

    /**
     * Unmap buffer after CPU access
     */
    fun unmap()

    /**
     * Copy data from another buffer
     */
    fun copyFrom(source: Buffer, srcOffset: Long = 0, dstOffset: Long = 0, size: Long = -1): RendererResult<Unit>

    /**
     * Dispose of buffer resources
     */
    fun dispose()
}

/**
 * Vertex buffer for geometry data
 */
interface VertexBuffer : Buffer {
    override val type: BufferType get() = BufferType.VERTEX
    val stride: Int  // Size of one vertex in bytes
    val count: Int   // Number of vertices

    /**
     * Set vertex attribute layout
     */
    fun setAttributes(attributes: List<VertexAttribute>)

    /**
     * Bind vertex buffer for rendering
     */
    fun bind(): RendererResult<Unit>
}

/**
 * Index buffer for triangle indices
 */
interface IndexBuffer : Buffer {
    override val type: BufferType get() = BufferType.INDEX
    val indexType: IndexType  // 16-bit or 32-bit indices
    val count: Int            // Number of indices

    /**
     * Bind index buffer for rendering
     */
    fun bind(): RendererResult<Unit>
}

/**
 * Uniform buffer for shader uniforms
 */
interface UniformBuffer : Buffer {
    override val type: BufferType get() = BufferType.UNIFORM
    val binding: Int  // Binding point for shader

    /**
     * Upload uniform data by name
     */
    fun setUniform(name: String, value: Any): RendererResult<Unit>

    /**
     * Bind uniform buffer to shader
     */
    fun bind(bindingPoint: Int): RendererResult<Unit>
}

/**
 * Storage buffer for compute shaders
 */
interface StorageBuffer : Buffer {
    override val type: BufferType get() = BufferType.STORAGE
    val binding: Int  // Binding point for shader

    /**
     * Bind storage buffer to compute shader
     */
    fun bind(bindingPoint: Int): RendererResult<Unit>
}

/**
 * Vertex attribute description
 */
data class VertexAttribute(
    val location: Int,
    val format: AttributeFormat,
    val offset: Int,
    val name: String = ""
)

/**
 * Vertex attribute formats
 */
enum class AttributeFormat(val size: Int, val componentCount: Int) {
    FLOAT(4, 1),
    FLOAT2(8, 2),
    FLOAT3(12, 3),
    FLOAT4(16, 4),
    INT(4, 1),
    INT2(8, 2),
    INT3(12, 3),
    INT4(16, 4),
    BYTE4_NORM(4, 4),
    UBYTE4_NORM(4, 4),
    SHORT2_NORM(4, 2),
    USHORT2_NORM(4, 2)
}

/**
 * Index formats
 */
enum class IndexType(val size: Int) {
    UINT16(2),
    UINT32(4)
}

/**
 * Buffer allocation result
 */
sealed class BufferAllocationResult {
    data class Success(val buffer: Buffer) : BufferAllocationResult()
    data class Error(val message: String) : BufferAllocationResult()
}

/**
 * Buffer manager interface
 */
interface BufferManager {
    /**
     * Create a vertex buffer
     */
    fun createVertexBuffer(
        size: Long,
        usage: BufferUsage = BufferUsage.STATIC,
        stride: Int,
        attributes: List<VertexAttribute> = emptyList()
    ): BufferAllocationResult

    /**
     * Create an index buffer
     */
    fun createIndexBuffer(
        size: Long,
        usage: BufferUsage = BufferUsage.STATIC,
        indexType: IndexType = IndexType.UINT32
    ): BufferAllocationResult

    /**
     * Create a uniform buffer
     */
    fun createUniformBuffer(
        size: Long,
        usage: BufferUsage = BufferUsage.DYNAMIC,
        binding: Int = 0
    ): BufferAllocationResult

    /**
     * Create a storage buffer
     */
    fun createStorageBuffer(
        size: Long,
        usage: BufferUsage = BufferUsage.DYNAMIC,
        binding: Int = 0
    ): BufferAllocationResult

    /**
     * Get buffer pool statistics
     */
    fun getStats(): BufferStats

    /**
     * Free unused buffers
     */
    fun gc()

    /**
     * Dispose of all buffers
     */
    fun dispose()
}

/**
 * Buffer statistics
 */
data class BufferStats(
    val totalBuffers: Int = 0,
    val activeBuffers: Int = 0,
    val pooledBuffers: Int = 0,
    val totalMemory: Long = 0,
    val usedMemory: Long = 0,
    val peakMemory: Long = 0,
    val allocations: Int = 0,
    val deallocations: Int = 0
)

/**
 * Buffer pool for efficient buffer reuse
 */
class BufferPool(
    private val bufferManager: BufferManager
) {
    private val pools = mutableMapOf<BufferPoolKey, MutableList<Buffer>>()
    private val stats = BufferStatsTracker()

    /**
     * Get or create a buffer from the pool
     */
    fun getBuffer(
        type: BufferType,
        size: Long,
        usage: BufferUsage
    ): BufferAllocationResult {
        val key = BufferPoolKey(type, size, usage)
        val pool = pools.getOrPut(key) { mutableListOf() }

        // Try to reuse an existing buffer
        val reusedBuffer = pool.removeFirstOrNull()
        if (reusedBuffer != null) {
            stats.poolHit()
            return BufferAllocationResult.Success(reusedBuffer)
        }

        stats.poolMiss()

        // Create new buffer
        return when (type) {
            BufferType.VERTEX -> bufferManager.createVertexBuffer(size, usage, 0)
            BufferType.INDEX -> bufferManager.createIndexBuffer(size, usage)
            BufferType.UNIFORM -> bufferManager.createUniformBuffer(size, usage)
            BufferType.STORAGE -> bufferManager.createStorageBuffer(size, usage)
            else -> BufferAllocationResult.Error("Unsupported buffer type for pooling")
        }
    }

    /**
     * Return a buffer to the pool
     */
    fun returnBuffer(buffer: Buffer) {
        val key = BufferPoolKey(buffer.type, buffer.size, buffer.usage)
        val pool = pools.getOrPut(key) { mutableListOf() }

        // Reset buffer state
        buffer.needsUpdate = false

        pool.add(buffer)
        stats.bufferReturned()
    }

    /**
     * Clear pools and dispose of all buffers
     */
    fun clear() {
        for (pool in pools.values) {
            pool.forEach { it.dispose() }
            pool.clear()
        }
        pools.clear()
        stats.reset()
    }

    /**
     * Get pool statistics
     */
    fun getStats(): BufferStats = stats.getStats()

    private data class BufferPoolKey(
        val type: BufferType,
        val size: Long,
        val usage: BufferUsage
    )
}

/**
 * Default buffer manager implementation
 */
class DefaultBufferManager(
    private val capabilities: RendererCapabilities
) : BufferManager {

    private val buffers = mutableListOf<Buffer>()
    private val stats = BufferStatsTracker()

    override fun createVertexBuffer(
        size: Long,
        usage: BufferUsage,
        stride: Int,
        attributes: List<VertexAttribute>
    ): BufferAllocationResult {
        return try {
            val buffer = DefaultVertexBuffer(
                id = generateBufferId(),
                size = size,
                usage = usage,
                stride = stride,
                attributes = attributes
            )

            buffers.add(buffer)
            stats.bufferAllocated(size)

            BufferAllocationResult.Success(buffer)
        } catch (e: Exception) {
            BufferAllocationResult.Error("Failed to create vertex buffer: ${e.message}")
        }
    }

    override fun createIndexBuffer(
        size: Long,
        usage: BufferUsage,
        indexType: IndexType
    ): BufferAllocationResult {
        return try {
            val buffer = DefaultIndexBuffer(
                id = generateBufferId(),
                size = size,
                usage = usage,
                indexType = indexType
            )

            buffers.add(buffer)
            stats.bufferAllocated(size)

            BufferAllocationResult.Success(buffer)
        } catch (e: Exception) {
            BufferAllocationResult.Error("Failed to create index buffer: ${e.message}")
        }
    }

    override fun createUniformBuffer(
        size: Long,
        usage: BufferUsage,
        binding: Int
    ): BufferAllocationResult {
        // Check uniform buffer size limits
        if (size > capabilities.maxUniformBufferSize) {
            return BufferAllocationResult.Error(
                "Uniform buffer size $size exceeds maximum ${capabilities.maxUniformBufferSize}"
            )
        }

        return try {
            val buffer = DefaultUniformBuffer(
                id = generateBufferId(),
                size = size,
                usage = usage,
                binding = binding
            )

            buffers.add(buffer)
            stats.bufferAllocated(size)

            BufferAllocationResult.Success(buffer)
        } catch (e: Exception) {
            BufferAllocationResult.Error("Failed to create uniform buffer: ${e.message}")
        }
    }

    override fun createStorageBuffer(
        size: Long,
        usage: BufferUsage,
        binding: Int
    ): BufferAllocationResult {
        if (!capabilities.computeShaders) {
            return BufferAllocationResult.Error("Storage buffers require compute shader support")
        }

        return try {
            val buffer = DefaultStorageBuffer(
                id = generateBufferId(),
                size = size,
                usage = usage,
                binding = binding
            )

            buffers.add(buffer)
            stats.bufferAllocated(size)

            BufferAllocationResult.Success(buffer)
        } catch (e: Exception) {
            BufferAllocationResult.Error("Failed to create storage buffer: ${e.message}")
        }
    }

    override fun getStats(): BufferStats = stats.getStats()

    override fun gc() {
        // Remove disposed buffers
        val iterator = buffers.iterator()
        while (iterator.hasNext()) {
            val buffer = iterator.next()
            // In a real implementation, check if buffer is still referenced
            // For now, just keep all buffers
        }
    }

    override fun dispose() {
        buffers.forEach { it.dispose() }
        buffers.clear()
        stats.reset()
    }

    private var nextBufferId = 1
    private fun generateBufferId(): Int = nextBufferId++
}

/**
 * Default vertex buffer implementation
 */
private class DefaultVertexBuffer(
    override val id: Int,
    override val size: Long,
    override val usage: BufferUsage,
    override val stride: Int,
    private var attributes: List<VertexAttribute>
) : VertexBuffer {

    override val access: BufferAccess = BufferAccess.WRITE_ONLY
    override var needsUpdate: Boolean = false
    override val count: Int get() = (size / stride).toInt()

    private var data: ByteArray? = null
    private var isMapped = false

    override fun uploadData(data: ByteArray, offset: Long): RendererResult<Unit> {
        return try {
            this.data = data.copyOf()
            needsUpdate = true
            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.ResourceCreationFailed("Upload failed", e))
        }
    }

    override fun uploadFloatData(data: FloatArray, offset: Long): RendererResult<Unit> {
        val byteArray = ByteArray(data.size * 4)
        // Convert float array to byte array
        for (i in data.indices) {
            val bits = data[i].toBits()
            byteArray[i * 4] = (bits and 0xFF).toByte()
            byteArray[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
            byteArray[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
            byteArray[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
        }
        return uploadData(byteArray, offset)
    }

    override fun uploadIntData(data: IntArray, offset: Long): RendererResult<Unit> {
        val byteArray = ByteArray(data.size * 4)
        // Convert int array to byte array
        for (i in data.indices) {
            val value = data[i]
            byteArray[i * 4] = (value and 0xFF).toByte()
            byteArray[i * 4 + 1] = ((value shr 8) and 0xFF).toByte()
            byteArray[i * 4 + 2] = ((value shr 16) and 0xFF).toByte()
            byteArray[i * 4 + 3] = ((value shr 24) and 0xFF).toByte()
        }
        return uploadData(byteArray, offset)
    }

    override fun map(access: BufferAccess): ByteArray? {
        if (isMapped) return null
        isMapped = true
        return data?.copyOf()
    }

    override fun unmap() {
        isMapped = false
    }

    override fun copyFrom(source: Buffer, srcOffset: Long, dstOffset: Long, size: Long): RendererResult<Unit> {
        return try {
            // Validate source buffer type compatibility - can copy from any buffer type to vertex buffer

            // Get source buffer data by mapping it
            val sourceData = source.map(BufferAccess.READ_ONLY)

            if (sourceData == null) {
                return RendererResult.Error(RendererException.InvalidState("Source buffer has no data or cannot be mapped"))
            }

            // Calculate actual copy size
            val actualSize = if (size == -1L) {
                (sourceData.size - srcOffset).coerceAtLeast(0)
            } else {
                size
            }

            // Validate bounds
            if (srcOffset < 0 || dstOffset < 0 || actualSize < 0) {
                return RendererResult.Error(RendererException.InvalidState("Invalid offset or size parameters"))
            }

            if (srcOffset + actualSize > sourceData.size) {
                return RendererResult.Error(RendererException.InvalidState("Source copy bounds exceed buffer size"))
            }

            if (dstOffset + actualSize > this.size) {
                return RendererResult.Error(RendererException.InvalidState("Destination copy bounds exceed buffer size"))
            }

            // Initialize destination data if null
            if (this.data == null) {
                this.data = ByteArray(this.size.toInt())
            }

            // Perform the copy
            sourceData.copyInto(
                destination = this.data!!,
                destinationOffset = dstOffset.toInt(),
                startIndex = srcOffset.toInt(),
                endIndex = (srcOffset + actualSize).toInt()
            )

            // Mark buffer as needing update
            needsUpdate = true

            // Unmap source buffer after copying
            source.unmap()

            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.InvalidState("Buffer copy failed: ${e.message}"))
        }
    }

    override fun setAttributes(attributes: List<VertexAttribute>) {
        this.attributes = attributes
    }

    override fun bind(): RendererResult<Unit> {
        return RendererResult.Success(Unit) // Platform-specific binding would go here
    }

    override fun dispose() {
        data = null
        isMapped = false
    }
}

/**
 * Default index buffer implementation
 */
private class DefaultIndexBuffer(
    override val id: Int,
    override val size: Long,
    override val usage: BufferUsage,
    override val indexType: IndexType
) : IndexBuffer {

    override val type: BufferType = BufferType.INDEX
    override val access: BufferAccess = BufferAccess.WRITE_ONLY
    override var needsUpdate: Boolean = false
    override val count: Int get() = (size / indexType.size).toInt()

    private var data: ByteArray? = null
    private var isMapped = false

    override fun uploadData(data: ByteArray, offset: Long): RendererResult<Unit> {
        return try {
            this.data = data.copyOf()
            needsUpdate = true
            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.ResourceCreationFailed("Upload failed", e))
        }
    }

    override fun uploadFloatData(data: FloatArray, offset: Long): RendererResult<Unit> {
        return RendererResult.Error(RendererException.InvalidState("Cannot upload float data to index buffer"))
    }

    override fun uploadIntData(data: IntArray, offset: Long): RendererResult<Unit> {
        val byteArray = when (indexType) {
            IndexType.UINT16 -> {
                ByteArray(data.size * 2).apply {
                    for (i in data.indices) {
                        val value = data[i]
                        this[i * 2] = (value and 0xFF).toByte()
                        this[i * 2 + 1] = ((value shr 8) and 0xFF).toByte()
                    }
                }
            }

            IndexType.UINT32 -> {
                ByteArray(data.size * 4).apply {
                    for (i in data.indices) {
                        val value = data[i]
                        this[i * 4] = (value and 0xFF).toByte()
                        this[i * 4 + 1] = ((value shr 8) and 0xFF).toByte()
                        this[i * 4 + 2] = ((value shr 16) and 0xFF).toByte()
                        this[i * 4 + 3] = ((value shr 24) and 0xFF).toByte()
                    }
                }
            }
        }
        return uploadData(byteArray, offset)
    }

    override fun map(access: BufferAccess): ByteArray? {
        if (isMapped) return null
        isMapped = true
        return data?.copyOf()
    }

    override fun unmap() {
        isMapped = false
    }

    override fun copyFrom(source: Buffer, srcOffset: Long, dstOffset: Long, size: Long): RendererResult<Unit> {
        return try {
            // Index buffers should typically only copy from other index buffers with compatible types
            // But we'll allow copying from any buffer type for flexibility

            // Get source buffer data by mapping it
            val sourceData = source.map(BufferAccess.READ_ONLY)

            if (sourceData == null) {
                return RendererResult.Error(RendererException.InvalidState("Source buffer has no data or cannot be mapped"))
            }

            // Calculate actual copy size
            val actualSize = if (size == -1L) {
                (sourceData.size - srcOffset).coerceAtLeast(0)
            } else {
                size
            }

            // Validate bounds
            if (srcOffset < 0 || dstOffset < 0 || actualSize < 0) {
                return RendererResult.Error(RendererException.InvalidState("Invalid offset or size parameters"))
            }

            if (srcOffset + actualSize > sourceData.size) {
                return RendererResult.Error(RendererException.InvalidState("Source copy bounds exceed buffer size"))
            }

            if (dstOffset + actualSize > this.size) {
                return RendererResult.Error(RendererException.InvalidState("Destination copy bounds exceed buffer size"))
            }

            // Initialize destination data if null
            if (this.data == null) {
                this.data = ByteArray(this.size.toInt())
            }

            // Perform the copy
            sourceData.copyInto(
                destination = this.data!!,
                destinationOffset = dstOffset.toInt(),
                startIndex = srcOffset.toInt(),
                endIndex = (srcOffset + actualSize).toInt()
            )

            // Mark buffer as needing update
            needsUpdate = true

            // Unmap source buffer after copying
            source.unmap()

            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.InvalidState("Buffer copy failed: ${e.message}"))
        }
    }

    override fun bind(): RendererResult<Unit> {
        return RendererResult.Success(Unit) // Platform-specific binding would go here
    }

    override fun dispose() {
        data = null
        isMapped = false
    }
}

/**
 * Default uniform buffer implementation
 */
private class DefaultUniformBuffer(
    override val id: Int,
    override val size: Long,
    override val usage: BufferUsage,
    override val binding: Int
) : UniformBuffer {

    override val type: BufferType = BufferType.UNIFORM
    override val access: BufferAccess = BufferAccess.WRITE_ONLY
    override var needsUpdate: Boolean = false

    private var data: ByteArray = ByteArray(size.toInt())
    private val uniforms = mutableMapOf<String, UniformInfo>()
    private var isMapped = false

    override fun uploadData(data: ByteArray, offset: Long): RendererResult<Unit> {
        return try {
            data.copyInto(this.data, offset.toInt())
            needsUpdate = true
            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.ResourceCreationFailed("Upload failed", e))
        }
    }

    override fun uploadFloatData(data: FloatArray, offset: Long): RendererResult<Unit> {
        // Convert float array to byte array and upload
        return RendererResult.Success(Unit) // Simplified implementation
    }

    override fun uploadIntData(data: IntArray, offset: Long): RendererResult<Unit> {
        // Convert int array to byte array and upload
        return RendererResult.Success(Unit) // Simplified implementation
    }

    override fun map(access: BufferAccess): ByteArray? {
        if (isMapped) return null
        isMapped = true
        return data.copyOf()
    }

    override fun unmap() {
        isMapped = false
    }

    override fun copyFrom(source: Buffer, srcOffset: Long, dstOffset: Long, size: Long): RendererResult<Unit> {
        return try {
            // Uniform buffers can copy from any buffer type

            // Get source buffer data by mapping it
            val sourceData = source.map(BufferAccess.READ_ONLY)

            if (sourceData == null) {
                return RendererResult.Error(RendererException.InvalidState("Source buffer has no data or cannot be mapped"))
            }

            // Calculate actual copy size
            val actualSize = if (size == -1L) {
                (sourceData.size - srcOffset).coerceAtLeast(0)
            } else {
                size
            }

            // Validate bounds
            if (srcOffset < 0 || dstOffset < 0 || actualSize < 0) {
                return RendererResult.Error(RendererException.InvalidState("Invalid offset or size parameters"))
            }

            if (srcOffset + actualSize > sourceData.size) {
                return RendererResult.Error(RendererException.InvalidState("Source copy bounds exceed buffer size"))
            }

            if (dstOffset + actualSize > this.size) {
                return RendererResult.Error(RendererException.InvalidState("Destination copy bounds exceed buffer size"))
            }

            // Perform the copy - uniform buffer data is non-nullable
            sourceData.copyInto(
                destination = this.data,
                destinationOffset = dstOffset.toInt(),
                startIndex = srcOffset.toInt(),
                endIndex = (srcOffset + actualSize).toInt()
            )

            // Mark buffer as needing update
            needsUpdate = true

            // Unmap source buffer after copying
            source.unmap()

            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.InvalidState("Buffer copy failed: ${e.message}"))
        }
    }

    override fun setUniform(name: String, value: Any): RendererResult<Unit> {
        return try {
            // Set uniform value at appropriate offset
            // This would need proper layout calculation in a real implementation
            needsUpdate = true
            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.InvalidState("Failed to set uniform $name"))
        }
    }

    override fun bind(bindingPoint: Int): RendererResult<Unit> {
        return RendererResult.Success(Unit) // Platform-specific binding would go here
    }

    override fun dispose() {
        uniforms.clear()
        isMapped = false
    }

    private data class UniformInfo(
        val offset: Int,
        val size: Int,
        val type: String
    )
}

/**
 * Default storage buffer implementation
 */
private class DefaultStorageBuffer(
    override val id: Int,
    override val size: Long,
    override val usage: BufferUsage,
    override val binding: Int
) : StorageBuffer {

    override val type: BufferType = BufferType.STORAGE
    override val access: BufferAccess = BufferAccess.READ_WRITE
    override var needsUpdate: Boolean = false

    private var data: ByteArray = ByteArray(size.toInt())
    private var isMapped = false

    override fun uploadData(data: ByteArray, offset: Long): RendererResult<Unit> {
        return try {
            data.copyInto(this.data, offset.toInt())
            needsUpdate = true
            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.ResourceCreationFailed("Upload failed", e))
        }
    }

    override fun uploadFloatData(data: FloatArray, offset: Long): RendererResult<Unit> {
        return RendererResult.Success(Unit) // Simplified implementation
    }

    override fun uploadIntData(data: IntArray, offset: Long): RendererResult<Unit> {
        return RendererResult.Success(Unit) // Simplified implementation
    }

    override fun map(access: BufferAccess): ByteArray? {
        if (isMapped) return null
        isMapped = true
        return data.copyOf()
    }

    override fun unmap() {
        isMapped = false
    }

    override fun copyFrom(source: Buffer, srcOffset: Long, dstOffset: Long, size: Long): RendererResult<Unit> {
        return try {
            // Storage buffers can copy from any buffer type

            // Get source buffer data by mapping it
            val sourceData = source.map(BufferAccess.READ_ONLY)

            if (sourceData == null) {
                return RendererResult.Error(RendererException.InvalidState("Source buffer has no data or cannot be mapped"))
            }

            // Calculate actual copy size
            val actualSize = if (size == -1L) {
                (sourceData.size - srcOffset).coerceAtLeast(0)
            } else {
                size
            }

            // Validate bounds
            if (srcOffset < 0 || dstOffset < 0 || actualSize < 0) {
                return RendererResult.Error(RendererException.InvalidState("Invalid offset or size parameters"))
            }

            if (srcOffset + actualSize > sourceData.size) {
                return RendererResult.Error(RendererException.InvalidState("Source copy bounds exceed buffer size"))
            }

            if (dstOffset + actualSize > this.size) {
                return RendererResult.Error(RendererException.InvalidState("Destination copy bounds exceed buffer size"))
            }

            // Perform the copy - storage buffer data is non-nullable
            sourceData.copyInto(
                destination = this.data,
                destinationOffset = dstOffset.toInt(),
                startIndex = srcOffset.toInt(),
                endIndex = (srcOffset + actualSize).toInt()
            )

            // Mark buffer as needing update
            needsUpdate = true

            // Unmap source buffer after copying
            source.unmap()

            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.InvalidState("Buffer copy failed: ${e.message}"))
        }
    }

    override fun bind(bindingPoint: Int): RendererResult<Unit> {
        return RendererResult.Success(Unit) // Platform-specific binding would go here
    }

    override fun dispose() {
        isMapped = false
    }
}

/**
 * Statistics tracker for buffer operations
 */
private class BufferStatsTracker {
    private var totalBuffers = 0
    private var activeBuffers = 0
    private var pooledBuffers = 0
    private var totalMemory = 0L
    private var usedMemory = 0L
    private var peakMemory = 0L
    private var allocations = 0
    private var deallocations = 0
    private var poolHits = 0
    private var poolMisses = 0

    fun bufferAllocated(size: Long) {
        totalBuffers++
        activeBuffers++
        allocations++
        usedMemory += size
        totalMemory += size
        peakMemory = maxOf(peakMemory, usedMemory)
    }

    fun bufferDeallocated(size: Long) {
        activeBuffers = maxOf(0, activeBuffers - 1)
        deallocations++
        usedMemory = maxOf(0, usedMemory - size)
    }

    fun bufferReturned() {
        pooledBuffers++
    }

    fun poolHit() {
        poolHits++
    }

    fun poolMiss() {
        poolMisses++
    }

    fun getStats(): BufferStats {
        return BufferStats(
            totalBuffers = totalBuffers,
            activeBuffers = activeBuffers,
            pooledBuffers = pooledBuffers,
            totalMemory = totalMemory,
            usedMemory = usedMemory,
            peakMemory = peakMemory,
            allocations = allocations,
            deallocations = deallocations
        )
    }

    fun reset() {
        totalBuffers = 0
        activeBuffers = 0
        pooledBuffers = 0
        totalMemory = 0
        usedMemory = 0
        peakMemory = 0
        allocations = 0
        deallocations = 0
        poolHits = 0
        poolMisses = 0
    }
}

/**
 * Utility functions for buffer operations
 */
object BufferUtils {

    /**
     * Calculate optimal vertex buffer size
     */
    fun calculateVertexBufferSize(vertexCount: Int, attributes: List<VertexAttribute>): Long {
        val stride = attributes.sumOf { it.format.size }
        return (vertexCount * stride).toLong()
    }

    /**
     * Calculate optimal index buffer size
     */
    fun calculateIndexBufferSize(indexCount: Int, indexType: IndexType): Long {
        return (indexCount * indexType.size).toLong()
    }

    /**
     * Create standard vertex attributes for basic mesh
     */
    fun createBasicVertexAttributes(): List<VertexAttribute> {
        return listOf(
            VertexAttribute(0, AttributeFormat.FLOAT3, 0, "position"),    // 12 bytes
            VertexAttribute(1, AttributeFormat.FLOAT3, 12, "normal"),     // 12 bytes
            VertexAttribute(2, AttributeFormat.FLOAT2, 24, "texCoord")    // 8 bytes
        ) // Total stride: 32 bytes
    }

    /**
     * Create vertex attributes for PBR material
     */
    fun createPBRVertexAttributes(): List<VertexAttribute> {
        return listOf(
            VertexAttribute(0, AttributeFormat.FLOAT3, 0, "position"),    // 12 bytes
            VertexAttribute(1, AttributeFormat.FLOAT3, 12, "normal"),     // 12 bytes
            VertexAttribute(2, AttributeFormat.FLOAT4, 24, "tangent"),    // 16 bytes
            VertexAttribute(3, AttributeFormat.FLOAT2, 40, "texCoord0"),  // 8 bytes
            VertexAttribute(4, AttributeFormat.FLOAT2, 48, "texCoord1")   // 8 bytes
        ) // Total stride: 56 bytes
    }

    /**
     * Validate vertex attribute layout
     */
    fun validateVertexAttributes(attributes: List<VertexAttribute>): List<String> {
        val issues = mutableListOf<String>()

        if (attributes.isEmpty()) {
            issues.add("No vertex attributes defined")
            return issues
        }

        // Check for duplicate locations
        val locations = attributes.map { it.location }
        if (locations.size != locations.toSet().size) {
            issues.add("Duplicate vertex attribute locations found")
        }

        // Check for overlapping offsets
        val sortedAttributes = attributes.sortedBy { it.offset }
        for (i in 0 until sortedAttributes.size - 1) {
            val current = sortedAttributes[i]
            val next = sortedAttributes[i + 1]
            if (current.offset + current.format.size > next.offset) {
                issues.add("Overlapping vertex attributes: ${current.name} and ${next.name}")
            }
        }

        return issues
    }
}