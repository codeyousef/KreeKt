package io.kreekt.validation.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import io.kreekt.validation.api.ProductionReadinessChecker
import io.kreekt.validation.models.ValidationConfiguration
import io.kreekt.validation.models.ValidationStatus
import io.kreekt.validation.models.ProductionReadinessReport

/**
 * Contract test for validateProductionReadiness endpoint.
 * Tests the complete production readiness validation flow.
 *
 * This test will fail until ProductionReadinessChecker is implemented.
 */
class ProductionReadinessTest {

    @Test
    fun `validateProductionReadiness should return complete report`() = runTest {
        // Arrange
        val checker = ProductionReadinessChecker()
        val config = ValidationConfiguration(
            enabledCategories = setOf("compilation", "testing", "performance", "security", "constitutional"),
            coverageThreshold = 95.0f,
            generateHtmlReport = true
        )

        // Act
        val result = checker.validateProductionReadiness(
            projectPath = ".",
            configuration = config
        )

        // Assert - Contract validation
        assertNotNull(result)
        assertNotNull(result.timestamp)
        assertNotNull(result.overallStatus)
        assertNotNull(result.overallScore)
        assertTrue(result.overallScore in 0.0f..1.0f)
        assertNotNull(result.categories)
        assertTrue(result.categories.isNotEmpty())
    }

    @Test
    fun `validateProductionReadiness should handle missing configuration`() = runTest {
        // Arrange
        val checker = ProductionReadinessChecker()

        // Act
        val result = checker.validateProductionReadiness(
            projectPath = "."
        )

        // Assert
        assertNotNull(result)
        assertEquals(ValidationStatus.PASSED, result.overallStatus) // Or expected default behavior
    }

    @Test
    fun `validateProductionReadiness should validate request schema`() = runTest {
        // Arrange
        val checker = ProductionReadinessChecker()
        val config = ValidationConfiguration(
            coverageThreshold = 95.0f,
            maxArtifactSize = 2_097_152L,
            failFast = false
        )

        // Act & Assert - Should not throw
        val result = checker.validateProductionReadiness(
            projectPath = "/path/to/project",
            configuration = config
        )

        assertNotNull(result)
    }

    @Test
    fun `validateProductionReadiness should include remediation actions on failure`() = runTest {
        // Arrange
        val checker = ProductionReadinessChecker()
        val config = ValidationConfiguration(
            coverageThreshold = 100.0f // Impossibly high to force failure
        )

        // Act
        val result = checker.validateProductionReadiness(
            projectPath = ".",
            configuration = config
        )

        // Assert
        assertNotNull(result.remediationActions)
        assertTrue(result.remediationActions.isNotEmpty())
        result.remediationActions.forEach { action ->
            assertNotNull(action.criterionId)
            assertNotNull(action.title)
            assertNotNull(action.steps)
            assertTrue(action.steps.isNotEmpty())
        }
    }

    @Test
    fun `validateProductionReadiness should respect enabled categories`() = runTest {
        // Arrange
        val checker = ProductionReadinessChecker()
        val config = ValidationConfiguration(
            enabledCategories = setOf("compilation", "testing") // Only these two
        )

        // Act
        val result = checker.validateProductionReadiness(
            projectPath = ".",
            configuration = config
        )

        // Assert
        val categoryNames = result.categories.map { it.name }
        assertTrue("compilation" in categoryNames || "Compilation" in categoryNames)
        assertTrue("testing" in categoryNames || "Testing" in categoryNames)
        assertTrue("security" !in categoryNames && "Security" !in categoryNames)
    }
}
