package io.kreekt.examples.voxelcraft

/**
 * JVM implementation of Logger using System.out/err
 */
actual object Logger {
    actual fun debug(message: String) {
        println("[DEBUG] $message")
    }

    actual fun info(message: String) {
        println("[INFO] $message")
    }

    actual fun warn(message: String) {
        println("[WARN] $message")
    }

    actual fun error(message: String, throwable: Throwable?) {
        System.err.println("[ERROR] $message")
        throwable?.printStackTrace()
    }
}
