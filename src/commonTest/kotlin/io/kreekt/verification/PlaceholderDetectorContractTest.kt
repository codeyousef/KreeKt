@file:OptIn(kotlin.time.ExperimentalTime::class)

package io.kreekt.verification

import io.kreekt.verification.impl.DefaultPlaceholderDetector
import io.kreekt.verification.model.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Contract tests for PlaceholderDetector interface
 * These tests MUST FAIL initially to follow TDD methodology
 */
class PlaceholderDetectorContractTest {

    private lateinit var detector: PlaceholderDetector

    @BeforeTest
    fun setup() {
        detector = DefaultPlaceholderDetector()
    }

    @Test
    fun `detectPlaceholders should find TODO patterns in code`() {
        runBlocking {
            // Arrange
            val testContent = """
                class TestClass {
                    // TODO: Implement this method
                    fun someMethod() {
                        TODO("Not yet implemented")
                    }
                }
            """.trimIndent()

            // Act
            val result = detector.detectPlaceholders("test.kt", testContent)

            // Assert
            assertTrue(result is DetectionResult.Success, "Detection should succeed")
            val placeholders = (result as DetectionResult.Success).data

            assertTrue(placeholders.isNotEmpty(), "Should find TODO placeholders")
            assertTrue(placeholders.any { it.type == PlaceholderType.TODO }, "Should detect TODO type")

            val todoPlaceholder = placeholders.first { it.type == PlaceholderType.TODO }
            assertEquals("test.kt", todoPlaceholder.location.filePath)
            assertTrue(todoPlaceholder.location.lineNumber > 0, "Should have valid line number")
            assertTrue(todoPlaceholder.content.contains("TODO", ignoreCase = true), "Content should contain TODO")
            assertNotNull(todoPlaceholder.severity, "Should have severity assigned")
        }
    }

    @Test
    fun `detectPlaceholders should find FIXME patterns`() {
        runBlocking {
            // Arrange
            val testContent = """
                class BuggyClass {
                    // FIXME: This method has a critical bug
                    fun problematicMethod() {
                        // Known issue that needs fixing
                    }
                }
            """.trimIndent()

            // Act
            val result = detector.detectPlaceholders("buggy.kt", testContent)

            // Assert
            assertTrue(result is DetectionResult.Success, "Detection should succeed")
            val placeholders = (result as DetectionResult.Success).data

            assertTrue(placeholders.any { it.type == PlaceholderType.FIXME }, "Should detect FIXME type")

            val fixmePlaceholder = placeholders.first { it.type == PlaceholderType.FIXME }
            assertEquals("buggy.kt", fixmePlaceholder.location.filePath)
            assertEquals(Severity.CRITICAL, fixmePlaceholder.severity, "FIXME should have critical severity")
        }
    }

    @Test
    fun `scanFiles should process multiple files`() {
        runBlocking {
            // Arrange
            val filePaths = listOf(
                "file1.kt",
                "file2.kt",
                "excluded.class"
            )

            // Act
            val result = detector.scanFiles(filePaths)

            // Assert
            assertTrue(result is DetectionResult.Success, "Scan should succeed")
            val fileResults = (result as DetectionResult.Success).data

            // Should exclude .class files
            assertFalse(fileResults.containsKey("excluded.class"), "Should exclude .class files")
        }
    }

    @Test
    fun `configureDetection should update detector settings`() {
        // Arrange
        val customPatterns = listOf(
            DetectionPattern(
                type = PlaceholderType.PLACEHOLDER,
                regex = "CUSTOM_PATTERN",
                description = "Custom test pattern",
                severity = Severity.HIGH
            )
        )
        val exclusions = listOf("*.test")

        // Act
        val config = detector.configureDetection(customPatterns, exclusions)

        // Assert
        assertEquals(customPatterns, config.patterns, "Should use custom patterns")
        assertTrue(config.exclusions.contains("*.test"), "Should include exclusions")
    }

    @Test
    fun `validateProductionReady should identify violations`() {
        runBlocking {
            // Arrange
            val codeWithPlaceholders = """
                class ProductionClass {
                    // TODO: Remove this before release
                    fun criticalMethod() {
                        throw NotImplementedError("Not yet implemented")
                    }
                }
            """.trimIndent()

            // Create a temporary file for testing
            val testFile = "production.kt"

            // Act
            val result = detector.validateProductionReady(testFile)

            // Assert
            assertTrue(result is DetectionResult.Success, "Validation should succeed")
            val status = (result as DetectionResult.Success).data

            assertFalse(status.isProductionReady, "Code with placeholders should not be production ready")
            assertTrue(status.placeholderCount > 0, "Should count placeholders")
            assertTrue(status.violations.isNotEmpty(), "Should have violations")
            assertTrue(status.recommendations.isNotEmpty(), "Should have recommendations")
        }
    }

    @Test
    fun `categorizePlaceholders should group by severity and type`() {
        // Arrange
        val placeholders = listOf(
            createTestPlaceholder(PlaceholderType.TODO, Severity.HIGH),
            createTestPlaceholder(PlaceholderType.FIXME, Severity.CRITICAL),
            createTestPlaceholder(PlaceholderType.PLACEHOLDER, Severity.MEDIUM),
            createTestPlaceholder(PlaceholderType.TODO, Severity.LOW)
        )

        // Act
        val categorized = detector.categorizePlaceholders(placeholders)

        // Assert
        assertEquals(1, categorized.critical.size, "Should have 1 critical placeholder")
        assertEquals(1, categorized.high.size, "Should have 1 high priority placeholder")
        assertEquals(1, categorized.medium.size, "Should have 1 medium priority placeholder")
        assertEquals(1, categorized.low.size, "Should have 1 low priority placeholder")

        assertTrue(categorized.byType.containsKey(PlaceholderType.TODO), "Should group by TODO type")
        assertEquals(2, categorized.byType[PlaceholderType.TODO]?.size, "Should have 2 TODO placeholders")
    }

    @Test
    fun `estimateImplementationEffort should calculate time estimates`() {
        runBlocking {
            // Arrange
            val placeholders = listOf(
                createTestPlaceholder(PlaceholderType.TODO, Severity.HIGH),
                createTestPlaceholder(PlaceholderType.FIXME, Severity.CRITICAL),
                createTestPlaceholder(PlaceholderType.STUB, Severity.MEDIUM)
            )

            // Act
            val result = detector.estimateImplementationEffort(placeholders)

            // Assert
            assertTrue(result is DetectionResult.Success, "Effort estimation should succeed")
            val estimates = (result as DetectionResult.Success).data

            assertEquals(placeholders.size, estimates.size, "Should have estimate for each placeholder")

            placeholders.forEach { placeholder ->
                assertTrue(estimates.containsKey(placeholder), "Should have estimate for placeholder")
                val effort = estimates[placeholder]!!
                assertTrue(effort.amount > 0, "Effort should be positive")
                assertNotNull(effort.unit, "Should have time unit")
            }
        }
    }

    @Test
    fun `estimateImplementationEffort should use historical data when provided`() {
        runBlocking {
            // Arrange
            val placeholders = listOf(
                createTestPlaceholder(PlaceholderType.TODO, Severity.HIGH)
            )
            val historicalData = mapOf(
                PlaceholderType.TODO to io.kreekt.verification.model.Duration(
                    8,
                    io.kreekt.verification.model.Duration.TimeUnit.HOURS
                )
            )

            // Act
            val result = detector.estimateImplementationEffort(placeholders, historicalData)

            // Assert
            assertTrue(result is DetectionResult.Success, "Effort estimation should succeed")
            val estimates = (result as DetectionResult.Success).data

            val todoEstimate = estimates[placeholders.first()]!!
            assertEquals(8L, todoEstimate.amount, "Should use historical data")
            assertEquals(io.kreekt.verification.model.Duration.TimeUnit.HOURS, todoEstimate.unit)
        }
    }

    // Helper method to create test placeholders
    private fun createTestPlaceholder(
        type: PlaceholderType,
        severity: Severity,
        filePath: String = "test.kt",
        lineNumber: Int = 1
    ): PlaceholderPattern {
        return PlaceholderPattern(
            id = "test-${type.name}-$lineNumber",
            type = type,
            location = CodeLocation(filePath, lineNumber, 1, 10),
            content = "${type.name} placeholder content",
            context = "Test context",
            severity = severity,
            estimatedEffort = io.kreekt.verification.model.Duration(
                2,
                io.kreekt.verification.model.Duration.TimeUnit.HOURS
            ),
            dependencies = emptyList()
        )
    }
}