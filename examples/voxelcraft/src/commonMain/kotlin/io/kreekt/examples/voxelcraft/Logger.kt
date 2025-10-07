package io.kreekt.examples.voxelcraft

/**
 * Simple multiplatform logger for VoxelCraft
 *
 * Provides basic logging levels: debug, info, warn, error
 * Platform-specific implementations handle the actual output.
 */
expect object Logger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, throwable: Throwable? = null)
}
