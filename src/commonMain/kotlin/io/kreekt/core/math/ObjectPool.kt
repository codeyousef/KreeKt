package io.kreekt.core.math

/**
 * High-performance object pool for frequently allocated math objects
 * Reduces GC pressure by reusing Vector3, Matrix4, Quaternion instances
 */
class ObjectPool<T>(
    private val factory: () -> T,
    private val reset: (T) -> Unit = {},
    private val maxSize: Int = 100
) {
    private val pool = ArrayDeque<T>(maxSize)
    private var borrowed = 0

    /**
     * Borrow an object from the pool
     */
    fun borrow(): T {
        borrowed++
        return if (pool.isNotEmpty()) {
            pool.removeFirst()
        } else {
            factory()
        }
    }

    /**
     * Return an object to the pool
     */
    fun returnObject(obj: T) {
        if (borrowed > 0) borrowed--
        if (pool.size < maxSize) {
            reset(obj)
            pool.addLast(obj)
        }
    }

    /**
     * Clear the pool
     */
    fun clear() {
        pool.clear()
        borrowed = 0
    }

    /**
     * Get pool statistics
     */
    fun getStats(): PoolStats = PoolStats(
        available = pool.size,
        borrowed = borrowed,
        capacity = maxSize
    )
}

data class PoolStats(
    val available: Int,
    val borrowed: Int,
    val capacity: Int
)

/**
 * Thread-local pools for math objects to avoid allocations in hot paths
 */
object MathObjectPools {
    // Vector3 pool
    private val vector3Pool = ObjectPool(
        factory = { Vector3() },
        reset = { it.set(0f, 0f, 0f) },
        maxSize = 200
    )

    // Matrix4 pool
    private val matrix4Pool = ObjectPool(
        factory = { Matrix4() },
        reset = { it.identity() },
        maxSize = 50
    )

    // Quaternion pool
    private val quaternionPool = ObjectPool(
        factory = { Quaternion() },
        reset = { it.set(0f, 0f, 0f, 1f) },
        maxSize = 50
    )

    /**
     * Borrow a Vector3 from the pool
     */
    fun borrowVector3(): Vector3 = vector3Pool.borrow()

    /**
     * Return a Vector3 to the pool
     */
    fun returnVector3(v: Vector3) = vector3Pool.returnObject(v)

    /**
     * Borrow a Matrix4 from the pool
     */
    fun borrowMatrix4(): Matrix4 = matrix4Pool.borrow()

    /**
     * Return a Matrix4 to the pool
     */
    fun returnMatrix4(m: Matrix4) = matrix4Pool.returnObject(m)

    /**
     * Borrow a Quaternion from the pool
     */
    fun borrowQuaternion(): Quaternion = quaternionPool.borrow()

    /**
     * Return a Quaternion to the pool
     */
    fun returnQuaternion(q: Quaternion) = quaternionPool.returnObject(q)

    /**
     * Execute a block with a pooled Vector3
     */
    inline fun <R> withVector3(block: (Vector3) -> R): R {
        val v = borrowVector3()
        try {
            return block(v)
        } finally {
            returnVector3(v)
        }
    }

    /**
     * Execute a block with a pooled Matrix4
     */
    inline fun <R> withMatrix4(block: (Matrix4) -> R): R {
        val m = borrowMatrix4()
        try {
            return block(m)
        } finally {
            returnMatrix4(m)
        }
    }

    /**
     * Execute a block with a pooled Quaternion
     */
    inline fun <R> withQuaternion(block: (Quaternion) -> R): R {
        val q = borrowQuaternion()
        try {
            return block(q)
        } finally {
            returnQuaternion(q)
        }
    }

    /**
     * Get pool statistics
     */
    fun getStats(): Map<String, PoolStats> = mapOf(
        "Vector3" to vector3Pool.getStats(),
        "Matrix4" to matrix4Pool.getStats(),
        "Quaternion" to quaternionPool.getStats()
    )

    /**
     * Clear all pools
     */
    fun clearAll() {
        vector3Pool.clear()
        matrix4Pool.clear()
        quaternionPool.clear()
    }
}
