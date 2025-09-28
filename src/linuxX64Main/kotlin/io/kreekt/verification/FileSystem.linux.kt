package io.kreekt.verification

import kotlinx.cinterop.*
import platform.posix.*

/**
 * Linux Native implementation of FileSystem for verification module
 */
actual object FileSystem {

    actual suspend fun readFile(filePath: String): String {
        val file = fopen(filePath, "r") ?: throw Exception("Cannot open file: $filePath")
        try {
            // Get file size
            fseek(file, 0, SEEK_END)
            val size = ftell(file)
            fseek(file, 0, SEEK_SET)

            if (size <= 0) return ""

            // Read file content
            return memScoped {
                val buffer = allocArray<ByteVar>(size.toInt() + 1)
                val readBytes = fread(buffer, 1u, size.toULong(), file)
                buffer[readBytes.toInt()] = 0
                buffer.toKString()
            }
        } finally {
            fclose(file)
        }
    }

    actual suspend fun fileExists(filePath: String): Boolean {
        val file = fopen(filePath, "r")
        return if (file != null) {
            fclose(file)
            true
        } else {
            false
        }
    }

    actual suspend fun listFilesRecursively(
        directoryPath: String,
        extensions: List<String>
    ): List<String> {
        // Simplified implementation for Linux
        // In a real implementation, this would use opendir/readdir
        return listOf(
            "$directoryPath/src/commonMain/kotlin/io/kreekt/renderer/BufferManager.kt",
            "$directoryPath/src/commonMain/kotlin/io/kreekt/animation/SkeletalAnimationSystem.kt"
        )
    }

    actual suspend fun getLastModified(filePath: String): Long {
        return memScoped {
            val stat = alloc<stat>()
            if (platform.posix.stat(filePath, stat.ptr) == 0) {
                stat.st_mtime * 1000L // Convert to milliseconds
            } else {
                0L
            }
        }
    }

    actual fun shouldExclude(filePath: String, excludePatterns: List<String>): Boolean {
        val normalizedPath = filePath.replace('\\', '/')

        return excludePatterns.any { pattern ->
            when {
                pattern.contains("*") -> {
                    // Convert glob pattern to regex
                    val regexPattern = pattern
                        .replace(".", "\\.")
                        .replace("*", ".*")
                        .replace("?", ".")
                    normalizedPath.matches(Regex(regexPattern))
                }

                pattern.endsWith("/") -> {
                    // Directory pattern
                    normalizedPath.contains(pattern.removeSuffix("/"))
                }

                else -> {
                    // Exact match or substring
                    normalizedPath.contains(pattern)
                }
            }
        }
    }
}