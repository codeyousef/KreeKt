package io.kreekt.optimization

import io.kreekt.core.math.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass
import kotlin.system.measureNanoTime

/**
 * Generic object pool for reusable objects
 */
class ObjectPool<T : Any>(
    private val factory: () -> T,
    private val reset: (T) -> Unit = {},
    private val maxSize: Int = 100,
    private val preAllocate: Int = 0
) {
    private val available = mutableListOf<T>()
    private val inUse = mutableSetOf<T>()
    private val statistics = PoolStatistics()

    init {
        // Pre-allocate objects
        repeat(preAllocate.coerceAtMost(maxSize)) {
            available.add(factory())
        }
        statistics.totalCreated = preAllocate
    }

    /**
     * Acquire object from pool
     */
    fun acquire(): T {
        statistics.acquireCount++

        val obj = if (available.isNotEmpty()) {
            statistics.poolHits++
            available.removeAt(available.size - 1)
        } else {
            statistics.poolMisses++
            statistics.totalCreated++
            factory()
        }

        inUse.add(obj)
        statistics.currentInUse = inUse.size
        statistics.peakInUse = maxOf(statistics.peakInUse, inUse.size)

        return obj
    }

    /**
     * Release object back to pool
     */
    fun release(obj: T) {
        if (!inUse.remove(obj)) {
            return // Object not from this pool
        }

        statistics.releaseCount++
        statistics.currentInUse = inUse.size

        reset(obj)

        if (available.size < maxSize) {
            available.add(obj)
        } else {
            statistics.totalDestroyed++
            // Let GC handle it
        }
    }

    /**
     * Batch acquire multiple objects
     */
    fun acquireBatch(count: Int): List<T> {
        return List(count) { acquire() }
    }

    /**
     * Batch release multiple objects
     */
    fun releaseBatch(objects: List<T>) {
        objects.forEach { release(it) }
    }

    /**
     * Use object temporarily with automatic release
     */
    inline fun <R> use(block: (T) -> R): R {
        val obj = acquire()
        return try {
            block(obj)
        } finally {
            release(obj)
        }
    }

    /**
     * Clear the pool
     */
    fun clear() {
        available.clear()
        inUse.clear()
        statistics.reset()
    }

    /**
     * Trim pool to specified size
     */
    fun trim(targetSize: Int = maxSize / 2) {
        while (available.size > targetSize && available.isNotEmpty()) {
            available.removeAt(available.size - 1)
            statistics.totalDestroyed++
        }
    }

    /**
     * Get pool statistics
     */
    fun getStatistics(): PoolStatistics = statistics.copy()

    /**
     * Get current pool size
     */
    fun size(): Int = available.size + inUse.size
}

/**
 * Pool statistics for monitoring
 */
data class PoolStatistics(
    var totalCreated: Int = 0,
    var totalDestroyed: Int = 0,
    var acquireCount: Int = 0,
    var releaseCount: Int = 0,
    var poolHits: Int = 0,
    var poolMisses: Int = 0,
    var currentInUse: Int = 0,
    var peakInUse: Int = 0
) {
    fun hitRate(): Float = if (acquireCount > 0) {
        poolHits.toFloat() / acquireCount
    } else 0f

    fun reset() {
        totalCreated = 0
        totalDestroyed = 0
        acquireCount = 0
        releaseCount = 0
        poolHits = 0
        poolMisses = 0
        currentInUse = 0
        peakInUse = 0
    }
}

/**
 * Specialized pool for Vector3 objects
 */
class Vector3Pool(
    maxSize: Int = 1000,
    preAllocate: Int = 100
) : ObjectPool<Vector3>(
    factory = { Vector3() },
    reset = { v -> v.set(0f, 0f, 0f) },
    maxSize = maxSize,
    preAllocate = preAllocate
) {
    /**
     * Acquire with initialization
     */
    fun acquire(x: Float, y: Float, z: Float): Vector3 {
        return acquire().apply { set(x, y, z) }
    }

    /**
     * Acquire copy of existing vector
     */
    fun acquire(source: Vector3): Vector3 {
        return acquire().apply { copy(source) }
    }
}

/**
 * Specialized pool for Matrix4 objects
 */
class Matrix4Pool(
    maxSize: Int = 500,
    preAllocate: Int = 50
) : ObjectPool<Matrix4>(
    factory = { Matrix4() },
    reset = { m -> m.identity() },
    maxSize = maxSize,
    preAllocate = preAllocate
) {
    /**
     * Acquire identity matrix
     */
    fun acquireIdentity(): Matrix4 {
        return acquire().apply { identity() }
    }

    /**
     * Acquire with transformation
     */
    fun acquire(position: Vector3, rotation: Quaternion, scale: Vector3): Matrix4 {
        return acquire().apply { compose(position, rotation, scale) }
    }
}

/**
 * Specialized pool for Quaternion objects
 */
class QuaternionPool(
    maxSize: Int = 500,
    preAllocate: Int = 50
) : ObjectPool<Quaternion>(
    factory = { Quaternion() },
    reset = { q -> q.identity() },
    maxSize = maxSize,
    preAllocate = preAllocate
) {
    /**
     * Acquire identity quaternion
     */
    fun acquireIdentity(): Quaternion {
        return acquire().apply { identity() }
    }

    /**
     * Acquire from axis-angle
     */
    fun acquire(axis: Vector3, angle: Float): Quaternion {
        return acquire().apply { setFromAxisAngle(axis, angle) }
    }
}

/**
 * Pool management strategy
 */
enum class PoolStrategy {
    AGGRESSIVE,  // Keep many objects pooled
    BALANCED,    // Balance memory vs allocation
    CONSERVATIVE // Minimize memory usage
}

/**
 * Global pool manager for all object pools
 */
object PoolManager {
    private val pools = mutableMapOf<KClass<*>, ObjectPool<*>>()
    private val mathPools = MathPools()
    private var strategy = PoolStrategy.BALANCED
    private val monitoringScope = CoroutineScope(Dispatchers.Default)
    private var monitoringJob: Job? = null

    /**
     * Math object pools
     */
    class MathPools {
        val vector3 = Vector3Pool()
        val matrix4 = Matrix4Pool()
        val quaternion = QuaternionPool()
        val color = ObjectPool(
            factory = { Color() },
            reset = { c -> c.set(1f, 1f, 1f, 1f) },
            maxSize = 200,
            preAllocate = 20
        )
    }

    /**
     * Get math pools
     */
    fun math(): MathPools = mathPools

    /**
     * Register custom pool
     */
    fun <T : Any> registerPool(
        type: KClass<T>,
        pool: ObjectPool<T>
    ) {
        pools[type] = pool
    }

    /**
     * Get pool for type
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPool(type: KClass<T>): ObjectPool<T>? {
        return pools[type] as? ObjectPool<T>
    }

    /**
     * Set pool strategy
     */
    fun setStrategy(newStrategy: PoolStrategy) {
        strategy = newStrategy
        adjustPoolSizes()
    }

    /**
     * Adjust pool sizes based on strategy
     */
    private fun adjustPoolSizes() {
        val (maxSize, preAllocate) = when (strategy) {
            PoolStrategy.AGGRESSIVE -> 2000 to 200
            PoolStrategy.BALANCED -> 1000 to 100
            PoolStrategy.CONSERVATIVE -> 500 to 50
        }

        // Adjust math pools
        // Would need to expose setters for max size
    }

    /**
     * Start monitoring pools
     */
    fun startMonitoring(intervalMs: Long = 5000) {
        stopMonitoring()

        monitoringJob = monitoringScope.launch {
            while (isActive) {
                delay(intervalMs)
                analyzeAndOptimize()
            }
        }
    }

    /**
     * Stop monitoring pools
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Analyze pool usage and optimize
     */
    private fun analyzeAndOptimize() {
        // Check memory pressure
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryPressure = usedMemory.toFloat() / maxMemory

        if (memoryPressure > 0.8f) {
            // High memory pressure - trim pools
            trimAllPools()
        } else if (memoryPressure < 0.4f) {
            // Low memory pressure - can grow pools if needed
            growPoolsIfNeeded()
        }
    }

    /**
     * Trim all pools to reduce memory
     */
    private fun trimAllPools() {
        mathPools.vector3.trim()
        mathPools.matrix4.trim()
        mathPools.quaternion.trim()
        mathPools.color.trim()

        pools.values.forEach { pool ->
            // Would need to expose trim method
        }
    }

    /**
     * Grow pools if experiencing many misses
     */
    private fun growPoolsIfNeeded() {
        val stats = getAllStatistics()

        stats.forEach { (type, stat) ->
            if (stat.hitRate() < 0.7f) {
                // Poor hit rate - may need larger pool
                // Would adjust pool size here
            }
        }
    }

    /**
     * Get statistics for all pools
     */
    fun getAllStatistics(): Map<String, PoolStatistics> {
        return mapOf(
            "Vector3" to mathPools.vector3.getStatistics(),
            "Matrix4" to mathPools.matrix4.getStatistics(),
            "Quaternion" to mathPools.quaternion.getStatistics(),
            "Color" to mathPools.color.getStatistics()
        ) + pools.mapKeys { it.key.simpleName ?: "Unknown" }
            .mapValues { (it.value as ObjectPool<*>).getStatistics() }
    }

    /**
     * Reset all pools
     */
    fun resetAll() {
        mathPools.vector3.clear()
        mathPools.matrix4.clear()
        mathPools.quaternion.clear()
        mathPools.color.clear()

        pools.values.forEach { pool ->
            (pool as ObjectPool<*>).clear()
        }
    }
}

/**
 * Pooled operation utilities
 */
object PooledOperations {
    /**
     * Perform vector operation with pooled vectors
     */
    inline fun <R> withVector3(block: (Vector3) -> R): R {
        return PoolManager.math().vector3.use(block)
    }

    /**
     * Perform operation with multiple pooled vectors
     */
    inline fun <R> withVector3s(count: Int, block: (List<Vector3>) -> R): R {
        val vectors = PoolManager.math().vector3.acquireBatch(count)
        return try {
            block(vectors)
        } finally {
            PoolManager.math().vector3.releaseBatch(vectors)
        }
    }

    /**
     * Perform matrix operation with pooled matrix
     */
    inline fun <R> withMatrix4(block: (Matrix4) -> R): R {
        return PoolManager.math().matrix4.use(block)
    }

    /**
     * Perform quaternion operation with pooled quaternion
     */
    inline fun <R> withQuaternion(block: (Quaternion) -> R): R {
        return PoolManager.math().quaternion.use(block)
    }

    /**
     * Perform color operation with pooled color
     */
    inline fun <R> withColor(block: (Color) -> R): R {
        return PoolManager.math().color.use(block)
    }
}

/**
 * Performance benchmarking for pools
 */
class PoolBenchmark {
    /**
     * Benchmark pool vs allocation performance
     */
    fun benchmark(iterations: Int = 100000): BenchmarkResult {
        val pooledTime = measureNanoTime {
            repeat(iterations) {
                PooledOperations.withVector3 { v ->
                    v.set(1f, 2f, 3f)
                    v.normalize()
                }
            }
        }

        val allocTime = measureNanoTime {
            repeat(iterations) {
                val v = Vector3(1f, 2f, 3f)
                v.normalize()
            }
        }

        return BenchmarkResult(
            pooledTimeNs = pooledTime,
            allocTimeNs = allocTime,
            speedup = allocTime.toFloat() / pooledTime
        )
    }

    data class BenchmarkResult(
        val pooledTimeNs: Long,
        val allocTimeNs: Long,
        val speedup: Float
    )
}

/**
 * Memory pressure monitor
 */
class MemoryPressureMonitor {
    private val _pressure = MutableStateFlow(0f)
    val pressure: StateFlow<Float> = _pressure

    private val monitorScope = CoroutineScope(Dispatchers.Default)

    fun start() {
        monitorScope.launch {
            while (isActive) {
                val runtime = Runtime.getRuntime()
                val used = runtime.totalMemory() - runtime.freeMemory()
                val max = runtime.maxMemory()
                _pressure.value = used.toFloat() / max

                delay(1000) // Check every second

                if (_pressure.value > 0.9f) {
                    // Critical memory pressure
                    PoolManager.trimAllPools()
                }
            }
        }
    }

    fun stop() {
        monitorScope.cancel()
    }
}