package io.kreekt.profiling

import io.kreekt.renderer.Renderer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.collections.mutableMapOf
import kotlin.system.getTimeNanos

/**
 * Memory allocation tracking
 */
data class AllocationInfo(
    val type: String,
    val size: Long,
    val timestamp: Long,
    val stackTrace: List<String> = emptyList()
)

/**
 * Memory category for classification
 */
enum class MemoryCategory {
    TEXTURE,
    BUFFER,
    RENDER_TARGET,
    SHADER,
    GEOMETRY,
    MATERIAL,
    SYSTEM,
    POOL,
    OTHER
}

/**
 * GPU memory tracking entry
 */
data class GPUMemoryEntry(
    val id: String,
    val category: MemoryCategory,
    val size: Long,
    val description: String,
    val timestamp: Long = getTimeNanos()
)

/**
 * Memory usage snapshot
 */
data class MemorySnapshot(
    val timestamp: Long,
    val systemMemory: SystemMemoryInfo,
    val gpuMemory: GPUMemoryInfo,
    val allocations: Map<String, AllocationInfo>
)

/**
 * System memory information
 */
data class SystemMemoryInfo(
    val heapUsed: Long,
    val heapMax: Long,
    val nonHeapUsed: Long,
    val totalAllocated: Long,
    val objectCount: Map<String, Int>
)

/**
 * GPU memory information
 */
data class GPUMemoryInfo(
    val totalUsed: Long,
    val textureMemory: Long,
    val bufferMemory: Long,
    val renderTargetMemory: Long,
    val breakdown: Map<MemoryCategory, Long>
)

/**
 * Memory leak detection info
 */
data class PotentialLeak(
    val type: String,
    val count: Int,
    val totalSize: Long,
    val growthRate: Float,
    val samples: List<AllocationInfo>
)

/**
 * Memory profiler for tracking allocations and usage
 */
class MemoryProfiler(
    private val renderer: Renderer,
    private val trackAllocations: Boolean = true,
    private val detectLeaks: Boolean = true
) {
    private val gpuAllocations = mutableMapOf<String, GPUMemoryEntry>()
    private val systemAllocations = mutableMapOf<String, AllocationInfo>()
    private val snapshots = mutableListOf<MemorySnapshot>()
    private val leakDetector = MemoryLeakDetector()
    private val statistics = MemoryStatistics()

    private val monitoringScope = CoroutineScope(Dispatchers.Default)
    private var monitoringJob: Job? = null

    private val _memoryPressure = MutableStateFlow(0f)
    val memoryPressure: StateFlow<Float> = _memoryPressure

    private val _memoryWarnings = MutableSharedFlow<MemoryWarning>()
    val memoryWarnings: SharedFlow<MemoryWarning> = _memoryWarnings

    /**
     * Track GPU memory allocation
     */
    fun trackGPUAllocation(
        id: String,
        category: MemoryCategory,
        size: Long,
        description: String = ""
    ) {
        gpuAllocations[id] = GPUMemoryEntry(id, category, size, description)
        statistics.recordGPUAllocation(category, size)

        checkMemoryPressure()
    }

    /**
     * Track GPU memory deallocation
     */
    fun trackGPUDeallocation(id: String) {
        gpuAllocations.remove(id)?.let { entry ->
            statistics.recordGPUDeallocation(entry.category, entry.size)
        }
    }

    /**
     * Track system memory allocation
     */
    fun trackSystemAllocation(
        id: String,
        type: String,
        size: Long,
        captureStackTrace: Boolean = false
    ) {
        if (!trackAllocations) return

        val stackTrace = if (captureStackTrace) {
            Thread.currentThread().stackTrace
                .drop(2) // Skip this method and caller
                .take(10)
                .map { "${it.className}.${it.methodName}:${it.lineNumber}" }
        } else emptyList()

        systemAllocations[id] = AllocationInfo(type, size, getTimeNanos(), stackTrace)
        statistics.recordSystemAllocation(type, size)

        if (detectLeaks) {
            leakDetector.recordAllocation(type, size, stackTrace)
        }
    }

    /**
     * Track system memory deallocation
     */
    fun trackSystemDeallocation(id: String) {
        systemAllocations.remove(id)?.let { info ->
            statistics.recordSystemDeallocation(info.type, info.size)

            if (detectLeaks) {
                leakDetector.recordDeallocation(info.type, info.size)
            }
        }
    }

    /**
     * Take memory snapshot
     */
    fun takeSnapshot(): MemorySnapshot {
        val runtime = Runtime.getRuntime()

        val systemMemory = SystemMemoryInfo(
            heapUsed = runtime.totalMemory() - runtime.freeMemory(),
            heapMax = runtime.maxMemory(),
            nonHeapUsed = 0, // Platform-specific
            totalAllocated = systemAllocations.values.sumOf { it.size },
            objectCount = systemAllocations.values
                .groupBy { it.type }
                .mapValues { it.value.size }
        )

        val gpuMemoryByCategory = gpuAllocations.values
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.size } }

        val gpuMemory = GPUMemoryInfo(
            totalUsed = gpuAllocations.values.sumOf { it.size },
            textureMemory = gpuMemoryByCategory[MemoryCategory.TEXTURE] ?: 0,
            bufferMemory = gpuMemoryByCategory[MemoryCategory.BUFFER] ?: 0,
            renderTargetMemory = gpuMemoryByCategory[MemoryCategory.RENDER_TARGET] ?: 0,
            breakdown = gpuMemoryByCategory
        )

        val snapshot = MemorySnapshot(
            timestamp = getTimeNanos(),
            systemMemory = systemMemory,
            gpuMemory = gpuMemory,
            allocations = systemAllocations.toMap()
        )

        snapshots.add(snapshot)

        // Keep snapshot history limited
        if (snapshots.size > 100) {
            snapshots.removeAt(0)
        }

        return snapshot
    }

    /**
     * Start memory monitoring
     */
    fun startMonitoring(intervalMs: Long = 1000) {
        stopMonitoring()

        monitoringJob = monitoringScope.launch {
            while (isActive) {
                updateMemoryMetrics()
                checkForLeaks()
                delay(intervalMs)
            }
        }
    }

    /**
     * Stop memory monitoring
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Update memory metrics
     */
    private suspend fun updateMemoryMetrics() {
        val runtime = Runtime.getRuntime()
        val heapUsed = runtime.totalMemory() - runtime.freeMemory()
        val heapMax = runtime.maxMemory()

        _memoryPressure.value = heapUsed.toFloat() / heapMax

        // Check thresholds
        when {
            _memoryPressure.value > 0.95f -> {
                _memoryWarnings.emit(MemoryWarning.Critical(
                    "Critical memory pressure: ${(_memoryPressure.value * 100).toInt()}%"
                ))
                triggerMemoryCleanup()
            }
            _memoryPressure.value > 0.8f -> {
                _memoryWarnings.emit(MemoryWarning.High(
                    "High memory pressure: ${(_memoryPressure.value * 100).toInt()}%"
                ))
            }
        }
    }

    /**
     * Check for memory leaks
     */
    private suspend fun checkForLeaks() {
        if (!detectLeaks) return

        val leaks = leakDetector.detectLeaks()

        leaks.forEach { leak ->
            _memoryWarnings.emit(MemoryWarning.PotentialLeak(
                "Potential leak detected: ${leak.type} (${leak.count} instances, ${leak.totalSize} bytes)"
            ))
        }
    }

    /**
     * Check memory pressure
     */
    private fun checkMemoryPressure() {
        val totalGPU = gpuAllocations.values.sumOf { it.size }
        val totalSystem = systemAllocations.values.sumOf { it.size }

        // Platform-specific GPU memory limits
        val gpuLimit = getGPUMemoryLimit()

        if (totalGPU > gpuLimit * 0.9f) {
            monitoringScope.launch {
                _memoryWarnings.emit(MemoryWarning.GPUMemory(
                    "GPU memory near limit: ${totalGPU / 1024 / 1024}MB / ${gpuLimit / 1024 / 1024}MB"
                ))
            }
        }
    }

    /**
     * Trigger automatic memory cleanup
     */
    private fun triggerMemoryCleanup() {
        // Force garbage collection (use sparingly)
        System.gc()

        // Clear caches and pools
        statistics.recordCleanup()
    }

    /**
     * Get platform-specific GPU memory limit
     */
    private fun getGPUMemoryLimit(): Long {
        // Platform-specific implementation
        return 2L * 1024 * 1024 * 1024 // Default 2GB
    }

    /**
     * Get memory statistics
     */
    fun getStatistics(): MemoryStatistics = statistics

    /**
     * Get detailed memory report
     */
    fun generateReport(): MemoryReport {
        val currentSnapshot = takeSnapshot()

        return MemoryReport(
            timestamp = currentSnapshot.timestamp,
            systemMemory = currentSnapshot.systemMemory,
            gpuMemory = currentSnapshot.gpuMemory,
            topAllocations = getTopAllocations(10),
            memoryTrend = calculateMemoryTrend(),
            potentialLeaks = if (detectLeaks) leakDetector.detectLeaks() else emptyList()
        )
    }

    /**
     * Get top memory allocations
     */
    private fun getTopAllocations(count: Int): List<AllocationInfo> {
        return systemAllocations.values
            .sortedByDescending { it.size }
            .take(count)
    }

    /**
     * Calculate memory trend
     */
    private fun calculateMemoryTrend(): MemoryTrend {
        if (snapshots.size < 2) {
            return MemoryTrend.STABLE
        }

        val recent = snapshots.takeLast(10)
        val firstTotal = recent.first().let { it.systemMemory.heapUsed + it.gpuMemory.totalUsed }
        val lastTotal = recent.last().let { it.systemMemory.heapUsed + it.gpuMemory.totalUsed }

        val change = (lastTotal - firstTotal).toFloat() / firstTotal

        return when {
            change > 0.1f -> MemoryTrend.INCREASING
            change < -0.1f -> MemoryTrend.DECREASING
            else -> MemoryTrend.STABLE
        }
    }

    /**
     * Clear profiler data
     */
    fun clear() {
        gpuAllocations.clear()
        systemAllocations.clear()
        snapshots.clear()
        leakDetector.clear()
        statistics.reset()
    }
}

/**
 * Memory leak detector
 */
class MemoryLeakDetector {
    private val allocationHistory = mutableMapOf<String, MutableList<AllocationInfo>>()
    private val allocationCounts = mutableMapOf<String, Int>()

    fun recordAllocation(type: String, size: Long, stackTrace: List<String>) {
        allocationHistory.getOrPut(type) { mutableListOf() }
            .add(AllocationInfo(type, size, getTimeNanos(), stackTrace))

        allocationCounts[type] = allocationCounts.getOrDefault(type, 0) + 1

        // Keep history limited
        allocationHistory[type]?.let { history ->
            if (history.size > 100) {
                history.removeAt(0)
            }
        }
    }

    fun recordDeallocation(type: String, size: Long) {
        allocationCounts[type] = (allocationCounts[type] ?: 0) - 1
    }

    fun detectLeaks(): List<PotentialLeak> {
        val leaks = mutableListOf<PotentialLeak>()

        allocationHistory.forEach { (type, history) ->
            if (history.size < 10) return@forEach

            // Check for continuous growth
            val recent = history.takeLast(10)
            val growthRate = calculateGrowthRate(recent)

            if (growthRate > 0.1f && allocationCounts[type] ?: 0 > 100) {
                leaks.add(PotentialLeak(
                    type = type,
                    count = allocationCounts[type] ?: 0,
                    totalSize = recent.sumOf { it.size },
                    growthRate = growthRate,
                    samples = recent.take(3)
                ))
            }
        }

        return leaks
    }

    private fun calculateGrowthRate(allocations: List<AllocationInfo>): Float {
        if (allocations.size < 2) return 0f

        val first = allocations.first().timestamp
        val last = allocations.last().timestamp
        val duration = (last - first) / 1_000_000_000f // Convert to seconds

        return allocations.size / duration
    }

    fun clear() {
        allocationHistory.clear()
        allocationCounts.clear()
    }
}

/**
 * Memory statistics
 */
class MemoryStatistics {
    private val gpuAllocations = mutableMapOf<MemoryCategory, Long>()
    private val systemAllocations = mutableMapOf<String, Long>()
    private var cleanupCount = 0

    fun recordGPUAllocation(category: MemoryCategory, size: Long) {
        gpuAllocations[category] = gpuAllocations.getOrDefault(category, 0L) + size
    }

    fun recordGPUDeallocation(category: MemoryCategory, size: Long) {
        gpuAllocations[category] = (gpuAllocations[category] ?: 0L) - size
    }

    fun recordSystemAllocation(type: String, size: Long) {
        systemAllocations[type] = systemAllocations.getOrDefault(type, 0L) + size
    }

    fun recordSystemDeallocation(type: String, size: Long) {
        systemAllocations[type] = (systemAllocations[type] ?: 0L) - size
    }

    fun recordCleanup() {
        cleanupCount++
    }

    fun reset() {
        gpuAllocations.clear()
        systemAllocations.clear()
        cleanupCount = 0
    }

    fun getTotalGPUMemory(): Long = gpuAllocations.values.sum()
    fun getTotalSystemMemory(): Long = systemAllocations.values.sum()
}

/**
 * Memory warning types
 */
sealed class MemoryWarning(val message: String) {
    class High(message: String) : MemoryWarning(message)
    class Critical(message: String) : MemoryWarning(message)
    class GPUMemory(message: String) : MemoryWarning(message)
    class PotentialLeak(message: String) : MemoryWarning(message)
}

/**
 * Memory trend indicator
 */
enum class MemoryTrend {
    INCREASING,
    STABLE,
    DECREASING
}

/**
 * Comprehensive memory report
 */
data class MemoryReport(
    val timestamp: Long,
    val systemMemory: SystemMemoryInfo,
    val gpuMemory: GPUMemoryInfo,
    val topAllocations: List<AllocationInfo>,
    val memoryTrend: MemoryTrend,
    val potentialLeaks: List<PotentialLeak>
)