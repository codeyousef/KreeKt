package io.kreekt.validation.platform

/**
 * JVM implementation of the file scanner factory.
 */
actual object FileScannerFactory {
    actual fun createFileScanner(): FileScanner {
        return JvmFileScanner()
    }
}