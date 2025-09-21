package io.kreekt.core.platform

/**
 * Platform abstraction for time measurement
 */
expect fun currentTimeMillis(): Long

/**
 * Platform abstraction for high-resolution time measurement (nanoseconds)
 */
expect fun nanoTime(): Long

/**
 * Platform abstraction for performance measurement
 */
expect fun performanceNow(): Double

/**
 * Platform abstraction for array cloning
 */
expect fun FloatArray.platformClone(): FloatArray
expect fun IntArray.platformClone(): IntArray
expect fun DoubleArray.platformClone(): DoubleArray

/**
 * Platform abstraction for generic array cloning
 */
expect fun <T> Array<T>.platformClone(): Array<T>