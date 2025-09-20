/**
 * JavaScript platform-specific function implementations
 */
package io.kreekt.core.platform

import kotlin.js.Date

/**
 * JS implementation - uses Date.now()
 */
actual fun currentTimeMillis(): Long {
    return Date.now().toLong()
}

/**
 * JS implementation - uses performance.now() or Date.now()
 */
actual fun nanoTime(): Long {
    return (js("performance.now()") as Double * 1_000_000).toLong()
}

/**
 * JS implementation - uses performance.now()
 */
actual fun performanceNow(): Double {
    return js("performance.now()") as Double
}

/**
 * Platform-specific array cloning
 */
actual fun FloatArray.platformClone(): FloatArray = this.copyOf()
actual fun IntArray.platformClone(): IntArray = this.copyOf()
actual fun DoubleArray.platformClone(): DoubleArray = this.copyOf()

/**
 * Platform-specific generic array cloning
 */
actual fun <T> Array<T>.platformClone(): Array<T> = this.copyOf()

/**
 * JS implementation of array copy for FloatArray
 */
actual fun platformArrayCopy(
    src: FloatArray,
    srcPos: Int,
    dest: FloatArray,
    destPos: Int,
    length: Int
) {
    for (i in 0 until length) {
        dest[destPos + i] = src[srcPos + i]
    }
}

/**
 * JS implementation of array copy for IntArray
 */
actual fun platformArrayCopy(
    src: IntArray,
    srcPos: Int,
    dest: IntArray,
    destPos: Int,
    length: Int
) {
    for (i in 0 until length) {
        dest[destPos + i] = src[srcPos + i]
    }
}

/**
 * JS implementation of clone - creates a shallow copy
 */
actual fun <T> platformClone(obj: T): T {
    @Suppress("UNCHECKED_CAST")
    return when (obj) {
        is FloatArray -> obj.copyOf() as T
        is IntArray -> obj.copyOf() as T
        is Array<*> -> obj.copyOf() as T
        is MutableList<*> -> (obj as MutableList<*>).toMutableList() as T
        is MutableMap<*, *> -> (obj as MutableMap<*, *>).toMutableMap() as T
        is MutableSet<*> -> (obj as MutableSet<*>).toMutableSet() as T
        else -> obj // For immutable objects, return as-is
    }
}