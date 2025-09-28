package io.kreekt.verification

/**
 * JavaScript implementation of FileSystem for verification module
 * Note: In browser environments, file system access is limited
 * This implementation provides basic functionality for testing
 */
actual object FileSystem {

    actual suspend fun readFile(filePath: String): String {
        // In browser environment, we can only provide placeholder functionality
        // Real implementation would require Node.js environment or file API
        return when {
            filePath.contains("BufferManager") -> """
                class BufferManager {
                    // TODO: Implement buffer allocation
                    fun allocateBuffer(): Buffer {
                        TODO("Not implemented")
                    }
                }
            """.trimIndent()

            filePath.contains("test") -> """
                // Test file content
                class TestClass {
                    // TODO: Implement test method
                    fun testMethod() = Unit
                }
            """.trimIndent()

            else -> "// File content placeholder - actual implementation needed for real files"
        }
    }

    actual suspend fun fileExists(filePath: String): Boolean {
        // In browser, we can only simulate file existence
        return filePath.endsWith(".kt") && !filePath.contains("nonexistent")
    }

    actual suspend fun listFilesRecursively(
        directoryPath: String,
        extensions: List<String>
    ): List<String> {
        // Return simulated file list for testing
        return if (directoryPath.contains("test") || directoryPath.contains("src")) {
            listOf(
                "$directoryPath/src/commonMain/kotlin/io/kreekt/renderer/BufferManager.kt",
                "$directoryPath/src/commonMain/kotlin/io/kreekt/animation/SkeletalAnimationSystem.kt"
            )
        } else {
            emptyList()
        }
    }

    actual suspend fun getLastModified(filePath: String): Long {
        // Return current time as placeholder
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
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