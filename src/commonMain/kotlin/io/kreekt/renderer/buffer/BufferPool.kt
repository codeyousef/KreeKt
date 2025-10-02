package io.kreekt.renderer.buffer

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

        val reusedBuffer = pool.removeFirstOrNull()
        if (reusedBuffer != null) {
            stats.poolHit()
            return BufferAllocationResult.Success(reusedBuffer)
        }

        stats.poolMiss()

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
