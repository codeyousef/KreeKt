package io.kreekt.examples.voxelcraft

import io.kreekt.util.KreektLogger

/**
 * Thin wrapper around the shared KreeKt logger so the example uses the
 * project-wide logging pipeline (formatting, filtering, custom handlers).
 */
object Logger {
    private const val TAG = "VoxelCraft"

    init {
        if (KreektLogger.customLogHandler == null) {
            KreektLogger.customLogHandler = { level, tag, message, throwable ->
                val formatted = "[${level.name}][$tag] $message"
                when (level) {
                    KreektLogger.LogLevel.DEBUG -> console.log(formatted)
                    KreektLogger.LogLevel.INFO -> console.info(formatted)
                    KreektLogger.LogLevel.WARN -> console.warn(formatted)
                    KreektLogger.LogLevel.ERROR -> console.error(formatted)
                    KreektLogger.LogLevel.NONE -> Unit
                }
                throwable?.let { console.error(it) }
            }
        }
    }

    var level: KreektLogger.LogLevel
        get() = KreektLogger.level
        set(value) {
            KreektLogger.level = value
        }

    fun debug(message: String) {
        KreektLogger.debug(TAG, message)
    }

    fun info(message: String) {
        KreektLogger.info(TAG, message)
    }

    fun warn(message: String) {
        KreektLogger.warn(TAG, message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        KreektLogger.error(TAG, message, throwable)
    }
}
