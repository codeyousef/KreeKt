@file:OptIn(kotlin.time.ExperimentalTime::class)

package io.kreekt.verification

import io.kreekt.verification.impl.DefaultImplementationVerifier
import io.kreekt.verification.model.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Contract tests for ImplementationVerifier interface
 * These tests MUST FAIL initially to follow TDD methodology
 */
class ImplementationVerifierContractTest {

    private lateinit var verifier: ImplementationVerifier

    @BeforeTest
    fun setup() {
        verifier = DefaultImplementationVerifier()
    }

    @Test
    fun `scanCodebase should return list of implementation artifacts`() {
        runBlocking {
            // Arrange
            val testRootPath = "test/src"
            val excludePatterns = listOf("*.class", "build/")

            // Act
            val result = verifier.scanCodebase(testRootPath, excludePatterns)

            // Assert
            assertTrue(result is VerificationResult.Success, "scanCodebase should succeed")
            val artifacts = (result as VerificationResult.Success).data
            assertTrue(artifacts.isNotEmpty(), "Should find implementation artifacts")

            // Verify artifact structure
            val firstArtifact = artifacts.first()
            assertTrue(firstArtifact.filePath.isNotBlank(), "File path should not be blank")
            assertTrue(firstArtifact.placeholderCount >= 0, "Placeholder count should be non-negative")
            assertNotNull(firstArtifact.moduleType, "Module type should be assigned")
            assertNotNull(firstArtifact.implementationStatus, "Implementation status should be assigned")
        }
    }

    @Test
    fun `analyzeFile should return detailed implementation artifact`() {
        runBlocking {
            // Arrange
            val testFilePath = "src/commonMain/kotlin/io/kreekt/renderer/BufferManager.kt"

            // Act
            val result = verifier.analyzeFile(testFilePath)

            // Assert
            assertTrue(result is VerificationResult.Success, "analyzeFile should succeed")
            val artifact = (result as VerificationResult.Success).data

            assertEquals(testFilePath, artifact.filePath)
            assertTrue(artifact.placeholderCount > 0, "BufferManager should have placeholders initially")
            assertEquals(ModuleType.RENDERER, artifact.moduleType)
            assertFalse(artifact.constitutionalCompliance, "Should not be constitutionally compliant with placeholders")
        }
    }

    @Test
    fun `validateConstitutionalCompliance should identify violations`() {
        runBlocking {
            // Arrange
            val artifactsWithPlaceholders = listOf(
                createTestArtifact(placeholderCount = 5, constitutionalCompliance = false),
                createTestArtifact(placeholderCount = 0, constitutionalCompliance = true)
            )

            // Act
            val result = verifier.validateConstitutionalCompliance(artifactsWithPlaceholders)

            // Assert
            assertTrue(result is VerificationResult.Success, "Compliance validation should succeed")
            val report = (result as VerificationResult.Success).data

            assertEquals(2, report.totalArtifacts)
            assertEquals(1, report.compliantArtifacts)
            assertFalse(report.overallCompliance, "Should not be overall compliant")
            assertTrue(report.violations.isNotEmpty(), "Should have violations")

            val violation = report.violations.first()
            assertEquals("Production-Ready Code Only", violation.principle)
            assertEquals(Severity.HIGH, violation.severity, "Should have high severity")
        }
    }

    @Test
    fun `generateImplementationTasks should create ordered task list`() {
        runBlocking {
            // Arrange
            val artifacts = listOf(
                createTestArtifact(moduleType = ModuleType.RENDERER, priority = Priority.CRITICAL),
                createTestArtifact(moduleType = ModuleType.ANIMATION, priority = Priority.HIGH),
                createTestArtifact(moduleType = ModuleType.GEOMETRY, priority = Priority.MEDIUM)
            )

            // Act
            val result = verifier.generateImplementationTasks(artifacts, Priority.MEDIUM)

            // Assert
            assertTrue(result is VerificationResult.Success, "Task generation should succeed")
            val tasks = (result as VerificationResult.Success).data

            assertTrue(tasks.isNotEmpty(), "Should generate implementation tasks")

            // Verify task ordering by priority
            val priorities = tasks.map { it.priority }
            assertTrue(
                priorities.all { it.ordinal <= Priority.MEDIUM.ordinal },
                "All tasks should be medium priority or higher"
            )

            // Verify task structure
            val firstTask = tasks.first()
            assertTrue(firstTask.id.isNotBlank(), "Task should have ID")
            assertTrue(firstTask.title.isNotBlank(), "Task should have title")
            assertTrue(firstTask.description.isNotBlank(), "Task should have description")
            assertNotNull(firstTask.artifact, "Task should reference artifact")
            assertTrue(firstTask.placeholders.isNotEmpty(), "Task should have placeholders to fix")
        }
    }

    @Test
    fun `verifyImplementationComplete should check artifact completeness`() {
        runBlocking {
            // Arrange
            val incompleteArtifact = createTestArtifact(placeholderCount = 3, constitutionalCompliance = false)

            // Act
            val result = verifier.verifyImplementationComplete(incompleteArtifact)

            // Assert
            assertTrue(result is VerificationResult.Success, "Verification should succeed")
            val status = (result as VerificationResult.Success).data

            assertFalse(status.isComplete, "Artifact with placeholders should not be complete")
            assertFalse(status.constitutionalCompliance, "Should not be constitutionally compliant")
            assertTrue(status.remainingIssues.isNotEmpty(), "Should have remaining issues")
            assertTrue(status.recommendations.isNotEmpty(), "Should have recommendations")
        }
    }

    @Test
    fun `getImplementationProgress should return progress statistics`() {
        runBlocking {
            // Act
            val result = verifier.getImplementationProgress()

            // Assert
            assertTrue(result is VerificationResult.Success, "Progress retrieval should succeed")
            val progress = (result as VerificationResult.Success).data

            assertTrue(progress.totalArtifacts >= 0, "Total artifacts should be non-negative")
            assertTrue(progress.completeArtifacts >= 0, "Complete artifacts should be non-negative")
            assertTrue(progress.inProgressArtifacts >= 0, "In-progress artifacts should be non-negative")
            assertTrue(progress.remainingPlaceholders >= 0, "Remaining placeholders should be non-negative")
            assertTrue(progress.overallProgress in 0f..1f, "Overall progress should be between 0 and 1")
        }
    }

    @Test
    fun `validateQualityGates should check gate criteria`() {
        runBlocking {
            // Arrange
            val gates = listOf(
                QualityGate(
                    name = "Zero Placeholders",
                    description = "No placeholder implementations in production code",
                    criteria = listOf(
                        QualityCriteria(
                            name = "Zero Placeholders",
                            description = "All placeholders must be replaced",
                            threshold = "0",
                            measurement = "placeholder_count"
                        )
                    ),
                    required = true,
                    automatable = true,
                    constitutionalRequirement = true
                )
            )
            val artifacts = listOf(
                createTestArtifact(placeholderCount = 5, constitutionalCompliance = false)
            )

            // Act
            val result = verifier.validateQualityGates(gates, artifacts)

            // Assert
            assertTrue(result is VerificationResult.Success, "Gate validation should succeed")
            val report = (result as VerificationResult.Success).data

            assertEquals(1, report.totalGates)
            assertEquals(0, report.passedGates)
            assertFalse(report.allGatesPassed, "Gates should not pass with placeholders")
            assertTrue(report.failedGates.isNotEmpty(), "Should have failed gates")

            val failure = report.failedGates.first()
            assertEquals("Zero Placeholders", failure.gate.name)
            assertTrue(failure.affectedArtifacts.isNotEmpty(), "Should have affected artifacts")
        }
    }

    // Helper method to create test artifacts
    private fun createTestArtifact(
        filePath: String = "test/file.kt",
        moduleType: ModuleType = ModuleType.CORE_MATH,
        placeholderCount: Int = 1,
        priority: Priority = Priority.MEDIUM,
        constitutionalCompliance: Boolean = false,
        testCoverage: Float = 0.7f
    ): ImplementationArtifact {
        return ImplementationArtifact(
            filePath = filePath,
            moduleType = moduleType,
            implementationStatus = if (placeholderCount > 0) ImplementationStatus.INCOMPLETE else ImplementationStatus.COMPLETE,
            placeholderCount = placeholderCount,
            placeholderTypes = if (placeholderCount > 0) listOf(PlaceholderType.TODO) else emptyList(),
            priority = priority,
            lastModified = Instant.fromEpochMilliseconds(0),
            testCoverage = testCoverage,
            constitutionalCompliance = constitutionalCompliance
        )
    }
}