package io.kreekt.core.platform

/**
 * Android implementation of platform abstractions (same as JVM)
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun nanoTime(): Long = System.nanoTime()

actual fun performanceNow(): Double = System.nanoTime() / 1_000_000.0

actual fun FloatArray.platformClone(): FloatArray = this.clone()
actual fun IntArray.platformClone(): IntArray = this.clone()
actual fun DoubleArray.platformClone(): DoubleArray = this.clone()

@Suppress("UNCHECKED_CAST")
actual fun <T> Array<T>.platformClone(): Array<T> = this.clone() as Array<T>