package io.kreekt.renderer.webgpu

import io.kreekt.renderer.RenderStats

/**
 * Render statistics tracking.
 * T041: Track draw calls, triangles, and GPU memory usage.
 *
 * Provides detailed performance metrics for:
 * - Draw call counting
 * - Triangle/vertex counting
 * - GPU memory tracking
 * - Frame timing
 */
class RenderStatsTracker {
    // Frame counters
    private var frameNumber = 0
    private var drawCalls = 0
    private var triangles = 0
    private var vertices = 0
    private var points = 0
    private var lines = 0

    // Resource counters
    private var geometryCount = 0
    private var textureCount = 0
    private var shaderCount = 0
    private var programCount = 0

    // Memory tracking
    private var bufferMemory = 0L
    private var textureMemory = 0L
    private var totalMemory = 0L

    // Frame timing
    private var frameStartTime = 0.0
    private var frameEndTime = 0.0
    private var lastFrameTime = 0.0
    private var avgFrameTime = 0.0
    private val frameTimeHistory = ArrayDeque<Double>(60)

    /**
     * Called at the start of each frame.
     */
    fun frameStart() {
        frameStartTime = js("performance.now()").unsafeCast<Double>()

        // Reset per-frame counters
        drawCalls = 0
        triangles = 0
        vertices = 0
        points = 0
        lines = 0
    }

    /**
     * Called at the end of each frame.
     */
    fun frameEnd() {
        frameEndTime = js("performance.now()").unsafeCast<Double>()
        frameNumber++

        // Calculate frame time
        lastFrameTime = frameEndTime - frameStartTime

        // Update moving average
        frameTimeHistory.addLast(lastFrameTime)
        if (frameTimeHistory.size > 60) {
            frameTimeHistory.removeFirst()
        }

        avgFrameTime = frameTimeHistory.average()
    }

    /**
     * Records a draw call.
     * @param triangleCount Number of triangles in this draw call
     */
    fun recordDrawCall(triangleCount: Int) {
        drawCalls++
        triangles += triangleCount
        vertices += triangleCount * 3
    }

    /**
     * Records points rendering.
     * @param pointCount Number of points rendered
     */
    fun recordPoints(pointCount: Int) {
        points += pointCount
        drawCalls++
    }

    /**
     * Records lines rendering.
     * @param lineCount Number of lines rendered
     */
    fun recordLines(lineCount: Int) {
        lines += lineCount
        drawCalls++
    }

    /**
     * Records geometry creation.
     */
    fun recordGeometryCreated() {
        geometryCount++
    }

    /**
     * Records geometry disposal.
     */
    fun recordGeometryDisposed() {
        geometryCount--
    }

    /**
     * Records texture creation.
     * @param memorySize Texture memory size in bytes
     */
    fun recordTextureCreated(memorySize: Long) {
        textureCount++
        textureMemory += memorySize
        totalMemory += memorySize
    }

    /**
     * Records texture disposal.
     * @param memorySize Texture memory size in bytes
     */
    fun recordTextureDisposed(memorySize: Long) {
        textureCount--
        textureMemory -= memorySize
        totalMemory -= memorySize
    }

    /**
     * Records shader/program creation.
     */
    fun recordShaderCreated() {
        shaderCount++
        programCount++
    }

    /**
     * Records shader/program disposal.
     */
    fun recordShaderDisposed() {
        shaderCount--
        programCount--
    }

    /**
     * Records buffer memory allocation.
     * @param size Buffer size in bytes
     */
    fun recordBufferAllocated(size: Long) {
        bufferMemory += size
        totalMemory += size
    }

    /**
     * Records buffer memory deallocation.
     * @param size Buffer size in bytes
     */
    fun recordBufferDeallocated(size: Long) {
        bufferMemory -= size
        totalMemory -= size
    }

    /**
     * Gets current render statistics.
     */
    fun getStats(): RenderStats {
        return RenderStats(
            frame = frameNumber,
            calls = drawCalls,
            triangles = triangles,
            points = points,
            lines = lines,
            geometries = geometryCount,
            textures = textureCount,
            shaders = shaderCount,
            programs = programCount
        )
    }

    /**
     * Gets extended statistics including performance and memory.
     */
    fun getExtendedStats(): ExtendedRenderStats {
        return ExtendedRenderStats(
            base = getStats(),
            frameTime = lastFrameTime,
            avgFrameTime = avgFrameTime,
            fps = if (avgFrameTime > 0) 1000.0 / avgFrameTime else 0.0,
            bufferMemory = bufferMemory,
            textureMemory = textureMemory,
            totalMemory = totalMemory,
            vertices = vertices
        )
    }

    /**
     * Resets all statistics.
     */
    fun reset() {
        frameNumber = 0
        drawCalls = 0
        triangles = 0
        vertices = 0
        points = 0
        lines = 0
        geometryCount = 0
        textureCount = 0
        shaderCount = 0
        programCount = 0
        bufferMemory = 0
        textureMemory = 0
        totalMemory = 0
        frameTimeHistory.clear()
        avgFrameTime = 0.0
        lastFrameTime = 0.0
    }

    /**
     * Gets a formatted statistics summary.
     */
    fun getSummary(): String {
        val stats = getExtendedStats()
        val fps = (stats.fps * 10).toInt() / 10.0
        val frameTime = (stats.frameTime * 100).toInt() / 100.0
        val avgTime = (stats.avgFrameTime * 100).toInt() / 100.0

        return buildString {
            appendLine("=== Render Statistics ===")
            appendLine("Frame: ${stats.base.frame}")
            appendLine("FPS: $fps")
            appendLine("Frame Time: ${frameTime}ms (avg: ${avgTime}ms)")
            appendLine("Draw Calls: ${stats.base.calls}")
            appendLine("Triangles: ${stats.base.triangles}")
            appendLine("Vertices: ${stats.vertices}")
            appendLine("Geometries: ${stats.base.geometries}")
            appendLine("Textures: ${stats.base.textures}")
            appendLine("Shaders: ${stats.base.shaders}")
            appendLine("Memory:")
            appendLine("  - Buffers: ${formatBytes(stats.bufferMemory)}")
            appendLine("  - Textures: ${formatBytes(stats.textureMemory)}")
            appendLine("  - Total: ${formatBytes(stats.totalMemory)}")
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}

/**
 * Extended render statistics with performance and memory metrics.
 */
data class ExtendedRenderStats(
    val base: RenderStats,
    val frameTime: Double,
    val avgFrameTime: Double,
    val fps: Double,
    val bufferMemory: Long,
    val textureMemory: Long,
    val totalMemory: Long,
    val vertices: Int
)
