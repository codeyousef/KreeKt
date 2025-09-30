package io.kreekt.validation.contract

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import io.kreekt.validation.services.SecurityValidator
import io.kreekt.validation.models.Severity

/**
 * Contract test for validateSecurity endpoint.
 * Tests security vulnerability scanning.
 *
 * This test will fail until SecurityValidator is implemented.
 */
class SecurityTest {

    @Test
    fun `validateSecurity should scan dependencies for vulnerabilities`() = runTest {
        // Arrange
        val validator = SecurityValidator()

        // Act
        val result = validator.validateSecurity(
            projectPath = ".",
            scanDependencies = true
        )

        // Assert
        assertNotNull(result)
        assertNotNull(result.vulnerabilities)
        // List can be empty if no vulnerabilities found
        assertTrue(result.vulnerabilities is List)
    }

    @Test
    fun `validateSecurity should categorize vulnerabilities by severity`() = runTest {
        // Arrange
        val validator = SecurityValidator()

        // Act
        val result = validator.validateSecurity(
            projectPath = ".",
            scanDependencies = true
        )

        // Assert
        assertNotNull(result)

        result.vulnerabilities.forEach { vulnerability ->
            assertNotNull(vulnerability.severity)
            assertTrue(
                vulnerability.severity.uppercase() in listOf(
                    "CRITICAL",
                    "HIGH",
                    "MEDIUM",
                    "LOW"
                )
            )
        }
    }

    @Test
    fun `validateSecurity should include CVE information when available`() = runTest {
        // Arrange
        val validator = SecurityValidator()

        // Act
        val result = validator.validateSecurity(
            projectPath = ".",
            scanDependencies = true
        )

        // Assert
        assertNotNull(result)

        result.vulnerabilities.forEach { vulnerability ->
            // CVE may be null for some vulnerabilities
            if (vulnerability.cve != null) {
                assertTrue(vulnerability.cve.startsWith("CVE-"))
            }
            assertNotNull(vulnerability.dependency)
            assertNotNull(vulnerability.description)
        }
    }

    @Test
    fun `validateSecurity should scan code for security patterns`() = runTest {
        // Arrange
        val validator = SecurityValidator()

        // Act
        val result = validator.validateSecurity(
            projectPath = ".",
            scanCode = true
        )

        // Assert
        assertNotNull(result)
        assertNotNull(result.codeIssues)

        result.codeIssues.forEach { issue ->
            assertNotNull(issue.type)
            assertNotNull(issue.location)
            assertNotNull(issue.message)
        }
    }

    @Test
    fun `validateSecurity should respect scan configuration`() = runTest {
        // Arrange
        val validator = SecurityValidator()

        // Act - Only scan dependencies, not code
        val result = validator.validateSecurity(
            projectPath = ".",
            scanDependencies = true,
            scanCode = false
        )

        // Assert
        assertNotNull(result)
        assertNotNull(result.vulnerabilities)
        assertTrue(result.codeIssues?.isEmpty() ?: true)
    }

    @Test
    fun `validateSecurity should handle missing dependencies gracefully`() = runTest {
        // Arrange
        val validator = SecurityValidator()

        // Act
        val result = validator.validateSecurity(
            projectPath = "/empty/project",
            scanDependencies = true
        )

        // Assert
        assertNotNull(result)
        // Should return empty list or skip, not throw
        assertTrue(result.vulnerabilities.isEmpty() || result.skipped == true)
    }
}
