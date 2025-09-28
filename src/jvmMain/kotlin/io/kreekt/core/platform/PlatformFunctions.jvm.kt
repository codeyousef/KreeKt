/**
 * JVM implementations for platform-specific functions
 */
package io.kreekt.core.platform

/**
 * JVM implementation of platform array copy for FloatArray
 */
actual fun platformArrayCopy(
    src: FloatArray,
    srcPos: Int,
    dest: FloatArray,
    destPos: Int,
    length: Int
) {
    System.arraycopy(src, srcPos, dest, destPos, length)
}

/**
 * JVM implementation of platform array copy for IntArray
 */
actual fun platformArrayCopy(
    src: IntArray,
    srcPos: Int,
    dest: IntArray,
    destPos: Int,
    length: Int
) {
    System.arraycopy(src, srcPos, dest, destPos, length)
}

/**
 * JVM implementation of platform-specific clone function
 */
actual fun <T> platformClone(obj: T): T {
    @Suppress("UNCHECKED_CAST")
    return when (obj) {
        is FloatArray -> obj.clone() as T
        is IntArray -> obj.clone() as T
        is DoubleArray -> obj.clone() as T
        is Array<*> -> obj.clone() as T
        else -> obj // For immutable objects, return as-is
    }
}