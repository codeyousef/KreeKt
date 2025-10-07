package io.kreekt.examples.voxelcraft

/**
 * JavaScript implementation of Logger using browser console
 */
actual object Logger {
    actual fun debug(message: String) {
        console.log(message)
    }

    actual fun info(message: String) {
        console.info(message)
    }

    actual fun warn(message: String) {
        console.warn(message)
    }

    actual fun error(message: String, throwable: Throwable?) {
        console.error(message)
        throwable?.let { console.error(it) }
    }
}
