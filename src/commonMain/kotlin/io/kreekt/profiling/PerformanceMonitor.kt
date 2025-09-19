package io.kreekt.profiling

import io.kreekt.renderer.Renderer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*
import kotlin.system.getTimeNanos

/**
 * Performance metric types
 */
enum class MetricType {
    FRAME_TIME,
    FPS,
    GPU_TIME,
    CPU_TIME,
    DRAW_CALLS,
    TRIANGLES,
    VERTICES,
    TEXTURES,
    SHADERS,
    STATE_CHANGES,
    MEMORY_USAGE,
    BUFFER_UPLOADS
}

/**
 * Performance metric data point
 */
data class MetricDataPoint(
    val timestamp: Long,
    val value: Float
)

/**
 * Performance metric with history
 */
class PerformanceMetric(
    val type: MetricType,
    val historySize: Int = 120 // 2 seconds at 60 FPS
) {
    private val history = mutableListOf<MetricDataPoint>()
    private var currentValue: Float = 0f
    private var minValue: Float = Float.MAX_VALUE
    private var maxValue: Float = Float.MIN_VALUE
    private var sumValue: Float = 0f
    private var countValue: Int = 0

    fun record(value: Float, timestamp: Long = getTimeNanos()) {
        currentValue = value

        history.add(MetricDataPoint(timestamp, value))
        if (history.size > historySize) {
            history.removeAt(0)
        }

        minValue = minOf(minValue, value)
        maxValue = maxOf(maxValue, value)
        sumValue += value
        countValue++
    }

    fun getCurrent(): Float = currentValue
    fun getMin(): Float = if (minValue == Float.MAX_VALUE) 0f else minValue
    fun getMax(): Float = if (maxValue == Float.MIN_VALUE) 0f else maxValue
    fun getAverage(): Float = if (countValue > 0) sumValue / countValue else 0f

    fun getHistory(): List<MetricDataPoint> = history.toList()

    fun getRecent(count: Int): List<Float> {
        return history.takeLast(count).map { it.value }
    }

    fun reset() {
        history.clear()
        currentValue = 0f
        minValue = Float.MAX_VALUE
        maxValue = Float.MIN_VALUE
        sumValue = 0f
        countValue = 0
    }
}

/**
 * Frame timing information
 */
data class FrameTiming(
    val frameNumber: Long,
    val totalTime: Float,
    val cpuTime: Float,
    val gpuTime: Float,
    val presentTime: Float
)

/**
 * Rendering statistics per frame
 */
data class RenderStats(
    val drawCalls: Int,
    val triangles: Int,
    val vertices: Int,
    val textureBinds: Int,
    val shaderSwitches: Int,
    val stateChanges: Int,
    val bufferUploads: Int
)

/**
 * Performance bottleneck analysis
 */
enum class Bottleneck {
    CPU_BOUND,
    GPU_BOUND,
    MEMORY_BOUND,
    BANDWIDTH_BOUND,
    BALANCED,
    UNKNOWN
}

/**
 * Performance monitor for real-time metrics
 */
class PerformanceMonitor(
    private val renderer: Renderer,
    private val targetFPS: Float = 60f
) {
    private val metrics = mutableMapOf<MetricType, PerformanceMetric>()
    private val frameTimings = mutableListOf<FrameTiming>()
    private var currentFrame = 0L
    private var frameStartTime = 0L
    private var lastFrameTime = 0L

    private val cpuTimer = CPUTimer()
    private val gpuTimer = GPUTimer(renderer)

    private val _performanceFlow = MutableStateFlow(PerformanceData())
    val performanceFlow: StateFlow<PerformanceData> = _performanceFlow

    private val _bottleneckFlow = MutableStateFlow(Bottleneck.UNKNOWN)
    val bottleneckFlow: StateFlow<Bottleneck> = _bottleneckFlow

    private val monitorScope = CoroutineScope(Dispatchers.Default)

    init {
        // Initialize metrics
        MetricType.values().forEach { type ->
            metrics[type] = PerformanceMetric(type)
        }
    }

    /**
     * Start frame measurement
     */
    fun beginFrame() {
        currentFrame++
        frameStartTime = getTimeNanos()
        cpuTimer.start()
        gpuTimer.start()
    }

    /**
     * End frame measurement
     */
    fun endFrame() {
        cpuTimer.stop()
        gpuTimer.stop()

        val frameEndTime = getTimeNanos()
        val frameDuration = (frameEndTime - frameStartTime) / 1_000_000f // Convert to ms

        // Calculate FPS
        val fps = if (frameDuration > 0) 1000f / frameDuration else 0f

        // Record metrics
        metrics[MetricType.FRAME_TIME]?.record(frameDuration)
        metrics[MetricType.FPS]?.record(fps)
        metrics[MetricType.CPU_TIME]?.record(cpuTimer.getElapsed())
        metrics[MetricType.GPU_TIME]?.record(gpuTimer.getElapsed())

        // Store frame timing
        val timing = FrameTiming(
            frameNumber = currentFrame,
            totalTime = frameDuration,
            cpuTime = cpuTimer.getElapsed(),
            gpuTime = gpuTimer.getElapsed(),
            presentTime = frameDuration - cpuTimer.getElapsed() - gpuTimer.getElapsed()
        )

        frameTimings.add(timing)
        if (frameTimings.size > 120) {
            frameTimings.removeAt(0)
        }

        // Update performance data
        updatePerformanceData()

        // Analyze bottleneck
        analyzeBottleneck()

        lastFrameTime = frameEndTime
    }

    /**
     * Record rendering statistics
     */
    fun recordRenderStats(stats: RenderStats) {
        metrics[MetricType.DRAW_CALLS]?.record(stats.drawCalls.toFloat())
        metrics[MetricType.TRIANGLES]?.record(stats.triangles.toFloat())
        metrics[MetricType.VERTICES]?.record(stats.vertices.toFloat())
        metrics[MetricType.TEXTURES]?.record(stats.textureBinds.toFloat())
        metrics[MetricType.SHADERS]?.record(stats.shaderSwitches.toFloat())
        metrics[MetricType.STATE_CHANGES]?.record(stats.stateChanges.toFloat())
        metrics[MetricType.BUFFER_UPLOADS]?.record(stats.bufferUploads.toFloat())
    }

    /**
     * Record memory usage
     */
    fun recordMemoryUsage(bytes: Long) {
        metrics[MetricType.MEMORY_USAGE]?.record(bytes / (1024f * 1024f)) // Convert to MB
    }

    /**
     * Update performance data flow
     */
    private fun updatePerformanceData() {
        _performanceFlow.value = PerformanceData(
            fps = metrics[MetricType.FPS]?.getCurrent() ?: 0f,
            frameTime = metrics[MetricType.FRAME_TIME]?.getCurrent() ?: 0f,
            cpuTime = metrics[MetricType.CPU_TIME]?.getCurrent() ?: 0f,
            gpuTime = metrics[MetricType.GPU_TIME]?.getCurrent() ?: 0f,
            drawCalls = metrics[MetricType.DRAW_CALLS]?.getCurrent()?.toInt() ?: 0,
            triangles = metrics[MetricType.TRIANGLES]?.getCurrent()?.toInt() ?: 0,
            memoryMB = metrics[MetricType.MEMORY_USAGE]?.getCurrent() ?: 0f
        )
    }

    /**
     * Analyze performance bottleneck
     */
    private fun analyzeBottleneck() {
        val cpuTime = metrics[MetricType.CPU_TIME]?.getCurrent() ?: 0f
        val gpuTime = metrics[MetricType.GPU_TIME]?.getCurrent() ?: 0f
        val frameTime = metrics[MetricType.FRAME_TIME]?.getCurrent() ?: 0f
        val targetFrameTime = 1000f / targetFPS

        _bottleneckFlow.value = when {
            frameTime < targetFrameTime * 0.8f -> Bottleneck.BALANCED
            cpuTime > gpuTime * 1.5f -> Bottleneck.CPU_BOUND
            gpuTime > cpuTime * 1.5f -> Bottleneck.GPU_BOUND
            metrics[MetricType.MEMORY_USAGE]?.getCurrent() ?: 0f > 1000f -> Bottleneck.MEMORY_BOUND
            metrics[MetricType.BUFFER_UPLOADS]?.getCurrent() ?: 0f > 100 -> Bottleneck.BANDWIDTH_BOUND
            else -> Bottleneck.BALANCED
        }
    }

    /**
     * Get detailed performance report
     */
    fun generateReport(): PerformanceReport {
        val allMetrics = metrics.mapValues { (_, metric) ->
            MetricSummary(
                current = metric.getCurrent(),
                min = metric.getMin(),
                max = metric.getMax(),
                average = metric.getAverage()
            )
        }

        return PerformanceReport(
            timestamp = getTimeNanos(),
            metrics = allMetrics,
            bottleneck = _bottleneckFlow.value,
            recentFrames = frameTimings.takeLast(60),
            warnings = analyzePerformanceWarnings()
        )
    }

    /**
     * Analyze performance warnings
     */
    private fun analyzePerformanceWarnings(): List<PerformanceWarning> {
        val warnings = mutableListOf<PerformanceWarning>()

        // Check FPS
        val avgFPS = metrics[MetricType.FPS]?.getAverage() ?: 0f
        if (avgFPS < targetFPS * 0.9f) {
            warnings.add(PerformanceWarning.LowFPS(avgFPS, targetFPS))
        }

        // Check frame time variance
        val frameTimeHistory = metrics[MetricType.FRAME_TIME]?.getRecent(60) ?: emptyList()
        val frameTimeVariance = calculateVariance(frameTimeHistory)
        if (frameTimeVariance > 5f) {
            warnings.add(PerformanceWarning.HighVariance(frameTimeVariance))
        }

        // Check draw calls
        val drawCalls = metrics[MetricType.DRAW_CALLS]?.getCurrent()?.toInt() ?: 0
        if (drawCalls > 1000) {
            warnings.add(PerformanceWarning.ExcessiveDrawCalls(drawCalls))
        }

        // Check GPU time
        val gpuTime = metrics[MetricType.GPU_TIME]?.getCurrent() ?: 0f
        if (gpuTime > 1000f / targetFPS) {
            warnings.add(PerformanceWarning.GPUBottleneck(gpuTime))
        }

        return warnings
    }

    /**
     * Calculate variance of values
     */
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f

        val mean = values.average().toFloat()
        val variance = values.map { (it - mean).pow(2) }.average().toFloat()
        return sqrt(variance)
    }

    /**
     * Get metric by type
     */
    fun getMetric(type: MetricType): PerformanceMetric? = metrics[type]

    /**
     * Reset all metrics
     */
    fun reset() {
        metrics.values.forEach { it.reset() }
        frameTimings.clear()
        currentFrame = 0
    }

    /**
     * Start automatic monitoring
     */
    fun startAutoMonitoring(intervalMs: Long = 100) {
        monitorScope.launch {
            while (isActive) {
                // Collect and publish metrics periodically
                delay(intervalMs)
            }
        }
    }

    /**
     * Stop monitoring
     */
    fun stop() {
        monitorScope.cancel()
    }
}

/**
 * CPU timer for precise timing
 */
class CPUTimer {
    private var startTime = 0L
    private var elapsed = 0f

    fun start() {
        startTime = getTimeNanos()
    }

    fun stop() {
        elapsed = (getTimeNanos() - startTime) / 1_000_000f // Convert to ms
    }

    fun getElapsed(): Float = elapsed
}

/**
 * GPU timer using renderer queries
 */
class GPUTimer(private val renderer: Renderer) {
    private var elapsed = 0f

    fun start() {
        // Platform-specific GPU timer query start
        // renderer.beginGPUTimer()
    }

    fun stop() {
        // Platform-specific GPU timer query end
        // elapsed = renderer.endGPUTimer()
        elapsed = 0f // Placeholder
    }

    fun getElapsed(): Float = elapsed
}

/**
 * Performance data snapshot
 */
data class PerformanceData(
    val fps: Float = 0f,
    val frameTime: Float = 0f,
    val cpuTime: Float = 0f,
    val gpuTime: Float = 0f,
    val drawCalls: Int = 0,
    val triangles: Int = 0,
    val memoryMB: Float = 0f
)

/**
 * Metric summary statistics
 */
data class MetricSummary(
    val current: Float,
    val min: Float,
    val max: Float,
    val average: Float
)

/**
 * Performance report
 */
data class PerformanceReport(
    val timestamp: Long,
    val metrics: Map<MetricType, MetricSummary>,
    val bottleneck: Bottleneck,
    val recentFrames: List<FrameTiming>,
    val warnings: List<PerformanceWarning>
)

/**
 * Performance warning types
 */
sealed class PerformanceWarning {
    data class LowFPS(val actual: Float, val target: Float) : PerformanceWarning()
    data class HighVariance(val variance: Float) : PerformanceWarning()
    data class ExcessiveDrawCalls(val count: Int) : PerformanceWarning()
    data class GPUBottleneck(val gpuTime: Float) : PerformanceWarning()
    data class MemoryPressure(val usageMB: Float) : PerformanceWarning()
}

/**
 * Performance counter for specific operations
 */
class PerformanceCounter(val name: String) {
    private var count = 0L
    private var totalTime = 0L
    private var minTime = Long.MAX_VALUE
    private var maxTime = Long.MIN_VALUE

    inline fun <T> measure(block: () -> T): T {
        val start = getTimeNanos()
        val result = block()
        val elapsed = getTimeNanos() - start

        count++
        totalTime += elapsed
        minTime = minOf(minTime, elapsed)
        maxTime = maxOf(maxTime, elapsed)

        return result
    }

    fun getStats(): CounterStats {
        return CounterStats(
            name = name,
            count = count,
            totalTimeMs = totalTime / 1_000_000f,
            avgTimeMs = if (count > 0) (totalTime / count) / 1_000_000f else 0f,
            minTimeMs = if (minTime != Long.MAX_VALUE) minTime / 1_000_000f else 0f,
            maxTimeMs = if (maxTime != Long.MIN_VALUE) maxTime / 1_000_000f else 0f
        )
    }

    fun reset() {
        count = 0
        totalTime = 0
        minTime = Long.MAX_VALUE
        maxTime = Long.MIN_VALUE
    }
}

/**
 * Counter statistics
 */
data class CounterStats(
    val name: String,
    val count: Long,
    val totalTimeMs: Float,
    val avgTimeMs: Float,
    val minTimeMs: Float,
    val maxTimeMs: Float
)

/**
 * Global performance counters
 */
object PerformanceCounters {
    private val counters = mutableMapOf<String, PerformanceCounter>()

    fun getCounter(name: String): PerformanceCounter {
        return counters.getOrPut(name) { PerformanceCounter(name) }
    }

    fun getAllStats(): List<CounterStats> {
        return counters.values.map { it.getStats() }
    }

    fun resetAll() {
        counters.values.forEach { it.reset() }
    }

    fun clear() {
        counters.clear()
    }
}

/**
 * Performance optimization suggestions
 */
class PerformanceOptimizer(private val monitor: PerformanceMonitor) {
    fun getSuggestions(): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()

        val report = monitor.generateReport()

        // Analyze metrics and provide suggestions
        report.metrics[MetricType.DRAW_CALLS]?.let { metric ->
            if (metric.current > 500) {
                suggestions.add(OptimizationSuggestion(
                    type = OptimizationType.BATCHING,
                    description = "High draw call count (${metric.current.toInt()}). Consider batching or instancing.",
                    impact = OptimizationImpact.HIGH
                ))
            }
        }

        report.metrics[MetricType.TRIANGLES]?.let { metric ->
            if (metric.current > 1_000_000) {
                suggestions.add(OptimizationSuggestion(
                    type = OptimizationType.LOD,
                    description = "High triangle count (${metric.current.toInt()}). Implement LOD system.",
                    impact = OptimizationImpact.HIGH
                ))
            }
        }

        when (report.bottleneck) {
            Bottleneck.CPU_BOUND -> {
                suggestions.add(OptimizationSuggestion(
                    type = OptimizationType.CPU,
                    description = "CPU bottleneck detected. Optimize game logic or use worker threads.",
                    impact = OptimizationImpact.HIGH
                ))
            }
            Bottleneck.GPU_BOUND -> {
                suggestions.add(OptimizationSuggestion(
                    type = OptimizationType.GPU,
                    description = "GPU bottleneck detected. Simplify shaders or reduce overdraw.",
                    impact = OptimizationImpact.HIGH
                ))
            }
            else -> {}
        }

        return suggestions
    }
}

/**
 * Optimization suggestion
 */
data class OptimizationSuggestion(
    val type: OptimizationType,
    val description: String,
    val impact: OptimizationImpact
)

enum class OptimizationType {
    BATCHING,
    LOD,
    CULLING,
    CPU,
    GPU,
    MEMORY
}

enum class OptimizationImpact {
    LOW,
    MEDIUM,
    HIGH
}