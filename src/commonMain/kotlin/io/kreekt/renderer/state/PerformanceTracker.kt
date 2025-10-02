package io.kreekt.renderer.state

import io.kreekt.core.platform.currentTimeMillis
import io.kreekt.renderer.*

/**
 * Performance tracking and monitoring for GPU operations.
 * Tracks frame timing, memory usage, and resource pooling.
 */

/**
 * Performance monitor for GPU operations
 */
class GPUPerformanceMonitor {
    private var frameStartTime = 0L
    private var lastFrameTime = 0L
    private var frameCount = 0L
    private var averageFrameTime = 0f
    private var minFrameTime = Float.MAX_VALUE
    private var maxFrameTime = 0f

    private val frameTimeHistory = ArrayDeque<Float>(60) // Last 60 frames

    /**
     * Start frame timing
     */
    fun startFrame() {
        frameStartTime = getCurrentTime()
    }

    /**
     * End frame timing and update statistics
     */
    fun endFrame() {
        val frameTime = getCurrentTime() - frameStartTime
        lastFrameTime = frameTime

        updateFrameStats(frameTime.toFloat())
        frameCount++
    }

    private fun updateFrameStats(frameTime: Float) {
        // Update min/max
        minFrameTime = kotlin.math.min(minFrameTime, frameTime)
        maxFrameTime = kotlin.math.max(maxFrameTime, frameTime)

        // Add to history
        frameTimeHistory.addLast(frameTime)
        if (frameTimeHistory.size > 60) {
            frameTimeHistory.removeFirst()
        }

        // Calculate average
        averageFrameTime = frameTimeHistory.average().toFloat()
    }

    /**
     * Get current FPS
     */
    fun getFPS(): Float {
        return if (averageFrameTime > 0) 1000f / averageFrameTime else 0f
    }

    /**
     * Get frame time statistics
     */
    fun getFrameStats(): FrameStats {
        return FrameStats(
            currentFrameTime = lastFrameTime.toFloat(),
            averageFrameTime = averageFrameTime,
            minFrameTime = if (minFrameTime == Float.MAX_VALUE) 0f else minFrameTime,
            maxFrameTime = maxFrameTime,
            fps = getFPS(),
            frameCount = frameCount
        )
    }

    /**
     * Reset statistics
     */
    fun reset() {
        frameCount = 0
        averageFrameTime = 0f
        minFrameTime = Float.MAX_VALUE
        maxFrameTime = 0f
        frameTimeHistory.clear()
    }

    private fun getCurrentTime(): Long {
        return currentTimeMillis()
    }
}

/**
 * Frame timing statistics
 */
data class FrameStats(
    val currentFrameTime: Float,
    val averageFrameTime: Float,
    val minFrameTime: Float,
    val maxFrameTime: Float,
    val fps: Float,
    val frameCount: Long
)

/**
 * GPU memory tracker
 */
class GPUMemoryTracker {
    private var allocatedBuffers = 0L
    private var allocatedTextures = 0L
    private var allocatedPrograms = 0L
    private var peakBufferMemory = 0L
    private var peakTextureMemory = 0L

    fun trackBufferAllocation(size: Long) {
        allocatedBuffers += size
        peakBufferMemory = kotlin.math.max(peakBufferMemory, allocatedBuffers)
    }

    fun trackBufferDeallocation(size: Long) {
        allocatedBuffers = kotlin.math.max(0, allocatedBuffers - size)
    }

    fun trackTextureAllocation(size: Long) {
        allocatedTextures += size
        peakTextureMemory = kotlin.math.max(peakTextureMemory, allocatedTextures)
    }

    fun trackTextureDeallocation(size: Long) {
        allocatedTextures = kotlin.math.max(0, allocatedTextures - size)
    }

    fun getMemoryStats(): GPUMemoryStats {
        return GPUMemoryStats(
            allocatedBuffers = allocatedBuffers,
            allocatedTextures = allocatedTextures,
            allocatedPrograms = allocatedPrograms,
            peakBufferMemory = peakBufferMemory,
            peakTextureMemory = peakTextureMemory,
            totalAllocated = allocatedBuffers + allocatedTextures + allocatedPrograms
        )
    }

    fun reset() {
        allocatedBuffers = 0
        allocatedTextures = 0
        allocatedPrograms = 0
        peakBufferMemory = 0
        peakTextureMemory = 0
    }
}

/**
 * GPU memory statistics
 */
data class GPUMemoryStats(
    val allocatedBuffers: Long,
    val allocatedTextures: Long,
    val allocatedPrograms: Long,
    val peakBufferMemory: Long,
    val peakTextureMemory: Long,
    val totalAllocated: Long
) {
    fun getFormattedSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "${(bytes / 1_073_741_824.0 * 100).toInt() / 100.0} GB"
            bytes >= 1_048_576 -> "${(bytes / 1_048_576.0 * 100).toInt() / 100.0} MB"
            bytes >= 1024 -> "${(bytes / 1024.0 * 100).toInt() / 100.0} KB"
            else -> "$bytes B"
        }
    }
}

/**
 * GPU resource pool for efficient resource reuse
 */
class GPUResourcePool {
    private val bufferPools = mutableMapOf<BufferPoolKey, MutableList<Buffer>>()
    private val texturePool = mutableListOf<Texture>()
    private val shaderProgramPool = mutableListOf<ShaderProgram>()

    /**
     * Get or create a buffer from the pool
     */
    fun getBuffer(type: BufferType, size: Long, usage: BufferUsage): Buffer? {
        val key = BufferPoolKey(type, size, usage)
        val pool = bufferPools[key] ?: return null

        return pool.removeFirstOrNull()
    }

    /**
     * Return a buffer to the pool
     */
    fun returnBuffer(buffer: Buffer) {
        val key = BufferPoolKey(buffer.type, buffer.size, buffer.usage)
        val pool = bufferPools.getOrPut(key) { mutableListOf() }

        // Reset buffer state
        buffer.needsUpdate = false
        pool.add(buffer)
    }

    /**
     * Get or create a texture from the pool
     */
    fun getTexture(width: Int, height: Int): Texture? {
        val texture = texturePool.find { it.width == width && it.height == height }
        if (texture != null) {
            texturePool.remove(texture)
        }
        return texture
    }

    /**
     * Return a texture to the pool
     */
    fun returnTexture(texture: Texture) {
        texture.needsUpdate = false
        texturePool.add(texture)
    }

    /**
     * Clear all pools
     */
    fun clear() {
        bufferPools.values.forEach { pool ->
            pool.forEach { it.dispose() }
            pool.clear()
        }
        bufferPools.clear()

        texturePool.forEach { it.dispose() }
        texturePool.clear()

        shaderProgramPool.forEach { it.dispose() }
        shaderProgramPool.clear()
    }

    /**
     * Get pool statistics
     */
    fun getStats(): ResourcePoolStats {
        val totalBuffers = bufferPools.values.sumOf { it.size }
        val totalTextures = texturePool.size
        val totalPrograms = shaderProgramPool.size

        return ResourcePoolStats(
            totalBuffers = totalBuffers,
            totalTextures = totalTextures,
            totalPrograms = totalPrograms,
            bufferPools = bufferPools.size
        )
    }

    private data class BufferPoolKey(
        val type: BufferType,
        val size: Long,
        val usage: BufferUsage
    )
}

/**
 * Resource pool statistics
 */
data class ResourcePoolStats(
    val totalBuffers: Int,
    val totalTextures: Int,
    val totalPrograms: Int,
    val bufferPools: Int
)
