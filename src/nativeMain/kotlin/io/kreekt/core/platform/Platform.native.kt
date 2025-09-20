package io.kreekt.core.platform

import kotlinx.datetime.Clock

/**
 * Native implementation of platform abstractions
 */
actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

actual fun nanoTime(): Long = Clock.System.now().nanosecondsOfSecond.toLong()

actual fun performanceNow(): Double = Clock.System.now().toEpochMilliseconds().toDouble()

actual fun FloatArray.platformClone(): FloatArray {
    val result = FloatArray(this.size)
    for (i in this.indices) {
        result[i] = this[i]
    }
    return result
}

actual fun IntArray.platformClone(): IntArray {
    val result = IntArray(this.size)
    for (i in this.indices) {
        result[i] = this[i]
    }
    return result
}

actual fun DoubleArray.platformClone(): DoubleArray {
    val result = DoubleArray(this.size)
    for (i in this.indices) {
        result[i] = this[i]
    }
    return result
}

actual fun <T> Array<T>.platformClone(): Array<T> {
    @Suppress("UNCHECKED_CAST")
    return Array(this.size) { i -> this[i] } as Array<T>
}