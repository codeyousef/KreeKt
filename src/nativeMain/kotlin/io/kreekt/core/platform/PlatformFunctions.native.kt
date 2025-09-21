package io.kreekt.core.platform

/**
 * Native implementation of platform functions
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

// Platform-specific clone functions are now extension functions in Platform.kt