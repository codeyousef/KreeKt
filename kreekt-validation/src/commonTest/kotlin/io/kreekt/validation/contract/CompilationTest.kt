package io.kreekt.validation.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import io.kreekt.validation.services.CompilationValidator
import io.kreekt.validation.models.Platform
import io.kreekt.validation.models.ValidationStatus

/**
 * Contract test for validateCompilation endpoint.
 * Tests compilation validation across platforms.
 *
 * This test will fail until CompilationValidator is implemented.
 */
class CompilationTest {

    @Test
    fun `validateCompilation should check all platforms`() = runTest {
        // Arrange
        val validator = CompilationValidator()
        val platforms = listOf(Platform.JVM, Platform.JS, Platform.NATIVE_LINUX_X64)

        // Act
        val result = validator.validateCompilation(
            projectPath = ".",
            platforms = platforms
        )

        // Assert
        assertNotNull(result)
        assertNotNull(result.status)
        assertNotNull(result.platformResults)
        assertEquals(platforms.size, result.platformResults.size)
    }

    @Test
    fun `validateCompilation should report compilation errors`() = runTest {
        // Arrange
        val validator = CompilationValidator()

        // Act
        val result = validator.validateCompilation(
            projectPath = "/invalid/path",
            platforms = listOf(Platform.JVM)
        )

        // Assert
        assertNotNull(result)
        assertEquals(ValidationStatus.FAILED, result.status)
        val jvmResult = result.platformResults["jvm"]
        assertTrue(jvmResult?.errorMessages?.isNotEmpty() == true)
    }

    @Test
    fun `validateCompilation should handle platform unavailability gracefully`() = runTest {
        // Arrange
        val validator = CompilationValidator()

        // Act
        val result = validator.validateCompilation(
            projectPath = ".",
            platforms = listOf(Platform.IOS) // May not be available on all systems
        )

        // Assert
        assertNotNull(result)
        // Should skip or warn, not fail entirely
        assertTrue(
            result.status == ValidationStatus.SKIPPED ||
                    result.status == ValidationStatus.WARNING
        )
    }

    @Test
    fun `validateCompilation should validate response schema`() = runTest {
        // Arrange
        val validator = CompilationValidator()

        // Act
        val result = validator.validateCompilation(
            projectPath = ".",
            platforms = listOf(Platform.JVM)
        )

        // Assert - Response schema validation
        assertNotNull(result)
        assertNotNull(result.status)
        assertNotNull(result.platformResults)

        result.platformResults.forEach { (platform, platformResult) ->
            assertNotNull(platform)
            assertNotNull(platformResult.success)
            if (!platformResult.success) {
                assertNotNull(platformResult.errorMessages)
            }
        }
    }

    @Test
    fun `validateCompilation should complete within timeout`() = runTest {
        // Arrange
        val validator = CompilationValidator()
        val startTime = Clock.System.now().toEpochMilliseconds()

        // Act
        val result = validator.validateCompilation(
            projectPath = ".",
            platforms = Platform.values().toList(),
            timeoutMillis = 300_000L // 5 minutes
        )

        // Assert
        val duration = Clock.System.now().toEpochMilliseconds() - startTime
        assertTrue(duration < 300_000L)
        assertNotNull(result)
    }
}
