/**
 * Platform-specific functions for Kotlin Multiplatform
 */
package io.kreekt.core.platform

// currentTimeMillis is already declared in Platform.kt

/**
 * Platform-specific array copy function
 * Replaces System.arraycopy for multiplatform compatibility
 */
expect fun platformArrayCopy(
    src: FloatArray,
    srcPos: Int,
    dest: FloatArray,
    destPos: Int,
    length: Int
)

/**
 * Platform-specific array copy function for IntArray
 */
expect fun platformArrayCopy(
    src: IntArray,
    srcPos: Int,
    dest: IntArray,
    destPos: Int,
    length: Int
)

/**
 * Platform-specific clone function
 */
expect fun <T> platformClone(obj: T): T