package io.kreekt.verification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

/**
 * JVM implementation of FileSystem for verification module
 */
actual object FileSystem {

    actual suspend fun readFile(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $filePath")
        }
        if (!file.canRead()) {
            throw SecurityException("Cannot read file: $filePath")
        }
        file.readText()
    }

    actual suspend fun fileExists(filePath: String): Boolean = withContext(Dispatchers.IO) {
        File(filePath).exists()
    }

    actual suspend fun listFilesRecursively(
        directoryPath: String,
        extensions: List<String>
    ): List<String> = withContext(Dispatchers.IO) {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            return@withContext emptyList()
        }

        val result = mutableListOf<String>()
        directory.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                extensions.isEmpty() || extensions.any { ext ->
                    file.name.endsWith(".$ext", ignoreCase = true)
                }
            }
            .forEach { file ->
                result.add(file.absolutePath)
            }
        result
    }

    actual suspend fun getLastModified(filePath: String): Long = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.lastModified()
        } else {
            0L
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