/**
 * Performance Monitoring Contracts
 *
 * Feature: 013-double-check-and (Three.js r180 Feature Parity)
 * Category: Performance Monitoring and Debugging
 *
 * Requirements covered: FR-057 through FR-061
 */

package io.kreekt.performance

/**
 * Cross-platform performance monitor.
 *
 * FR-057: System MUST provide Stats-equivalent performance monitoring (FPS, frame time, memory)
 * FR-058: System MUST provide Timer utilities for high-resolution timing
 * FR-061: Performance monitoring MUST work across all platforms (JVM, JS, Native, Android, iOS)
 *
 * @sample
 * ```kotlin
 * val monitor = PerformanceMonitor()
 * monitor.enabled = true
 *
 * // In render loop
 * monitor.beginFrame()
 * // ... rendering code ...
 * monitor.endFrame()
 *
 * // Get metrics
 * println("FPS: ${monitor.getFPS()}")
 * println("Frame time: ${monitor.getFrameTime()}ms")
 * println("Memory: ${monitor.getMemoryUsage().usedMB}MB")
 * ```
 */
expect class PerformanceMonitor {
    /**
     * Whether monitoring is enabled.
     */
    var enabled: Boolean

    /**
     * How often to sample (every N frames).
     */
    var samplingInterval: Int

    /**
     * Marks the beginning of a frame.
     */
    fun beginFrame()

    /**
     * Marks the end of a frame.
     */
    fun endFrame()

    /**
     * Gets the most recent frame time in milliseconds.
     */
    fun getFrameTime(): Double

    /**
     * Gets the current frames per second.
     */
    fun getFPS(): Double

    /**
     * Gets current memory usage.
     */
    fun getMemoryUsage(): MemoryInfo

    /**
     * Gets draw call count for last frame.
     */
    fun getDrawCalls(): Int

    /**
     * Gets triangle count for last frame.
     */
    fun getTriangles(): Int

    /**
     * Gets active texture count.
     */
    fun getTextures(): Int

    /**
     * Gets active geometry count.
     */
    fun getGeometries(): Int

    /**
     * Gets number of compiled shader programs.
     */
    fun getProgramsCompiled(): Int

    /**
     * Gets recent metrics history.
     *
     * @param count Number of recent frames to return
     */
    fun getMetrics(count: Int = 60): List<FrameMetrics>

    /**
     * Gets average metrics over a window.
     *
     * @param windowSize Number of frames to average over
     */
    fun getAverages(windowSize: Int = 60): AverageMetrics

    /**
     * Resets all counters and metrics.
     */
    fun reset()
}

/**
 * Memory usage information.
 */
data class MemoryInfo(
    /**
     * Memory currently used in bytes.
     */
    val used: Long,

    /**
     * Total allocated memory in bytes.
     */
    val total: Long,

    /**
     * Memory limit in bytes.
     */
    val limit: Long
) {
    val usedMB: Float
        get() = used / (1024f * 1024f)

    val totalMB: Float
        get() = total / (1024f * 1024f)

    val percentage: Float
        get() = if (total > 0) used.toFloat() / total.toFloat() else 0f
}

/**
 * Frame metrics snapshot.
 */
data class FrameMetrics(
    /**
     * Frame number.
     */
    val frameNumber: Long,

    /**
     * Timestamp when frame was captured.
     */
    val timestamp: Double,

    /**
     * Frame time in milliseconds.
     */
    val frameTime: Double,

    /**
     * Frames per second.
     */
    val fps: Double,

    /**
     * Number of draw calls.
     */
    val drawCalls: Int,

    /**
     * Number of triangles rendered.
     */
    val triangles: Int,

    /**
     * Memory usage snapshot.
     */
    val memory: MemoryInfo
)

/**
 * Average metrics over a time window.
 */
data class AverageMetrics(
    val avgFPS: Double,
    val avgFrameTime: Double,
    val minFPS: Double,
    val maxFrameTime: Double,
    val avgDrawCalls: Double,
    val avgTriangles: Double,
    val avgMemoryUsed: Long
)

/**
 * High-resolution timer utility.
 *
 * FR-058: System MUST provide Timer utilities for high-resolution timing
 */
expect class Timer {
    /**
     * Gets current time in milliseconds with high precision.
     */
    fun now(): Double

    /**
     * Starts the timer.
     */
    fun start()

    /**
     * Gets elapsed time since start in milliseconds.
     */
    fun getElapsed(): Double

    /**
     * Resets the timer.
     */
    fun reset()
}

/**
 * Geometry validator.
 *
 * FR-059: System MUST provide geometry validation utilities to detect invalid geometry
 */
object GeometryValidator {
    /**
     * Validates a geometry for common issues.
     */
    fun validate(geometry: io.kreekt.geometry.BufferGeometry): GeometryValidationResult

    /**
     * Checks for degenerate triangles.
     */
    fun checkDegenerateTriangles(geometry: io.kreekt.geometry.BufferGeometry): List<Int>

    /**
     * Checks for non-manifold edges.
     */
    fun checkNonManifoldEdges(geometry: io.kreekt.geometry.BufferGeometry): List<Int>

    /**
     * Checks for missing or invalid UVs.
     */
    fun checkUVs(geometry: io.kreekt.geometry.BufferGeometry): UVValidationResult
}

/**
 * Geometry validation result.
 */
data class GeometryValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * UV validation result.
 */
data class UVValidationResult(
    val hasUVs: Boolean,
    val outOfBoundsUVs: List<Int>,
    val missingUVs: List<Int>
)

/**
 * Material validator.
 *
 * FR-060: System MUST provide material validation utilities to detect shader errors
 */
object MaterialValidator {
    /**
     * Validates a material's shaders.
     */
    fun validate(material: io.kreekt.material.Material): MaterialValidationResult

    /**
     * Validates shader source code.
     */
    fun validateShader(
        source: String,
        target: io.kreekt.material.ShaderTarget
    ): ShaderValidationResult
}

/**
 * Material validation result.
 */
data class MaterialValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Shader validation result.
 */
data class ShaderValidationResult(
    val isValid: Boolean,
    val errors: List<ShaderError>,
    val warnings: List<String>
)

/**
 * Shader compilation error.
 */
data class ShaderError(
    val line: Int,
    val column: Int,
    val message: String
)
