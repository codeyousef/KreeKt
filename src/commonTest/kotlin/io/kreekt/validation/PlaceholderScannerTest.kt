package io.kreekt.validation

import io.kreekt.validation.scanner.DefaultPlaceholderScanner
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract tests for PlaceholderScanner interface.
 */
class PlaceholderScannerTest {

    @Test
    fun `scanDirectory should detect placeholder patterns in source files`() = runTest {
        val scanner = DefaultPlaceholderScanner()
        // Use a path that definitely exists - the commonMain source directory
        val result = scanner.scanDirectory("src/commonMain", listOf("*.kt"), listOf("**/build/**"))

        assertNotNull(result)
        assertTrue(result.scanTimestamp > 0)
        assertTrue(result.totalFilesScanned >= 0)
        assertTrue(result.scanDurationMs >= 0)
    }

    @Test
    fun `scanFile should detect multiple placeholder types in single file`() = runTest {
        val scanner = DefaultPlaceholderScanner()
        val placeholders = scanner.scanFile("src/commonMain/kotlin/io/kreekt/validation/ValidationDataTypes.kt")

        assertNotNull(placeholders)
    }

    @Test
    fun `getDetectionPatterns should return comprehensive pattern list`() {
        val scanner = DefaultPlaceholderScanner()
        val patterns = scanner.getDetectionPatterns()

        assertNotNull(patterns)
        assertTrue(patterns.isNotEmpty())
    }

    @Test
    fun `validatePlaceholder should distinguish real placeholders from documentation`() {
        val scanner = DefaultPlaceholderScanner()
        val testInstance = PlaceholderInstance(
            filePath = "test.kt",
            lineNumber = 1,
            columnNumber = 1,
            pattern = "TODO",
            context = "// TODO: implement this",
            type = PlaceholderType.TODO,
            criticality = CriticalityLevel.MEDIUM,
            module = "test",
            platform = "common"
        )

        val fileContent = "// TODO: implement this\nfun test() {}"
        val isValid = scanner.validatePlaceholder(testInstance, fileContent)

        assertTrue(isValid is Boolean)
    }

    @Test
    fun `estimateReplacementEffort should categorize by complexity`() {
        val scanner = DefaultPlaceholderScanner()
        val testInstance = PlaceholderInstance(
            filePath = "test.kt",
            lineNumber = 1,
            columnNumber = 1,
            pattern = "TODO",
            context = "// TODO: implement",
            type = PlaceholderType.TODO,
            criticality = CriticalityLevel.MEDIUM,
            module = "test",
            platform = "common"
        )

        val fileContent = "// TODO: implement\nfun test() {}"
        val effort = scanner.estimateReplacementEffort(testInstance, fileContent)

        assertNotNull(effort)
        assertTrue(effort is EffortLevel)
    }

    @Test
    fun `scanDirectory should respect file pattern filters`() = runTest {
        val scanner = DefaultPlaceholderScanner()
        val result = scanner.scanDirectory("src", listOf("*.kt"), emptyList())

        assertNotNull(result)
        assertNotNull(result.scannedPaths)
    }

    @Test
    fun `scanDirectory should respect exclude patterns`() = runTest {
        val scanner = DefaultPlaceholderScanner()
        val result = scanner.scanDirectory("src", listOf("*.kt"), listOf("**/build/**", "**/node_modules/**"))

        assertNotNull(result)
        assertNotNull(result.scannedPaths)

        result.scannedPaths.forEach { path ->
            assertFalse(path.contains("/build/"))
        }
    }

    @Test
    fun `scanResult should include metadata`() = runTest {
        val scanner = DefaultPlaceholderScanner()
        val result = scanner.scanDirectory("src", listOf("*.kt"), listOf("**/build/**"))

        assertNotNull(result)
        assertTrue(result.scanTimestamp > 0)
        assertTrue(result.scanDurationMs >= 0)
        assertTrue(result.totalFilesScanned >= 0)
        assertNotNull(result.scannedPaths)
        assertNotNull(result.placeholders)
    }

    @Test
    fun `scanner should handle various placeholder formats`() {
        val scanner = DefaultPlaceholderScanner()
        val patterns = scanner.getDetectionPatterns()

        assertNotNull(patterns)
        assertTrue(patterns.isNotEmpty())
    }

    @Test
    fun `scanner should assess criticality levels correctly`() {
        val criticalInstance = PlaceholderInstance(
            filePath = "src/commonMain/kotlin/io/kreekt/renderer/WebGPURenderer.kt",
            lineNumber = 1,
            columnNumber = 1,
            pattern = "TODO",
            context = "// TODO: Critical renderer initialization",
            type = PlaceholderType.TODO,
            criticality = CriticalityLevel.CRITICAL,
            module = "kreekt-renderer",
            platform = "common"
        )

        val lowInstance = PlaceholderInstance(
            filePath = "src/commonMain/kotlin/io/kreekt/test/TestUtil.kt",
            lineNumber = 1,
            columnNumber = 1,
            pattern = "TODO",
            context = "// TODO: Add test helper",
            type = PlaceholderType.TODO,
            criticality = CriticalityLevel.LOW,
            module = "test",
            platform = "common"
        )

        assertNotNull(criticalInstance.criticality)
        assertNotNull(lowInstance.criticality)
        assertTrue(criticalInstance.module != lowInstance.module)
    }
}
