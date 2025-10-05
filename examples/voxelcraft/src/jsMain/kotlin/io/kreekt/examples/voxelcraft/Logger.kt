package io.kreekt.examples.voxelcraft

/**
 * Logger for VoxelCraft
 *
 * Provides structured logging with different severity levels.
 * Uses browser console for output with appropriate styling.
 */
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

object Logger {
    var level = LogLevel.INFO

    fun debug(message: String) {
        if (level <= LogLevel.DEBUG) console.log("[DEBUG] $message")
    }

    fun info(message: String) {
        if (level <= LogLevel.INFO) console.log("[INFO] $message")
    }

    fun warn(message: String) {
        if (level <= LogLevel.WARN) console.warn("[WARN] $message")
    }

    fun error(message: String) {
        if (level <= LogLevel.ERROR) console.error("[ERROR] $message")
    }
}