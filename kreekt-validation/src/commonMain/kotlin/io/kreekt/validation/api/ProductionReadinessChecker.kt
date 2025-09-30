package io.kreekt.validation.api

import io.kreekt.validation.models.*
import io.kreekt.validation.services.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Main orchestration API for production readiness validation.
 *
 * This checker coordinates all validators and aggregates their results into
 * a comprehensive production readiness report. It implements a flexible
 * validation pipeline that can be configured for different validation modes.
 *
 * ## Usage Example
 * ```kotlin
 * val checker = ProductionReadinessChecker()
 * val report = checker.validateProductionReadiness(
 *     projectPath = "/path/to/project",
 *     configuration = ValidationConfiguration.strict()
 * )
 *
 * println("Production ready: ${report.isProductionReady}")
 * println("Overall score: ${report.overallScore}")
 * ```
 */
class ProductionReadinessChecker {

    private val validators = mutableListOf<Validator<*>>()

    init {
        // Register default validators
        registerDefaultValidators()
    }

    /**
     * Main entry point for production readiness validation.
     *
     * Executes all applicable validators and returns a comprehensive report.
     *
     * @param projectPath The root path of the project to validate
     * @param configuration The validation configuration to use
     * @param platforms Optional list of platforms to validate (null = all platforms)
     * @return A comprehensive production readiness report
     */
    suspend fun validateProductionReadiness(
        projectPath: String,
        configuration: ValidationConfiguration = ValidationConfiguration.strict(),
        platforms: List<Platform>? = null
    ): ProductionReadinessReport = coroutineScope {

        val startTime = Clock.System.now()

        val context = ValidationContext(
            projectPath = projectPath,
            platforms = platforms?.map { it.name },
            configuration = mapOf(
                "coverageThreshold" to configuration.coverageThreshold,
                "maxArtifactSize" to configuration.maxArtifactSize,
                "failFast" to configuration.failFast,
                "performanceRequirements" to configuration.performanceRequirements
            )
        )

        // Execute validators in parallel where possible
        val validationJobs = validators.mapNotNull { validator ->
            if (validator.isApplicable(context)) {
                async {
                    try {
                        ValidatorResult(
                            name = validator.name,
                            result = validator.validate(context),
                            error = null
                        )
                    } catch (e: Exception) {
                        ValidatorResult(
                            name = validator.name,
                            result = null,
                            error = e
                        )
                    }
                }
            } else {
                null
            }
        }

        val results = validationJobs.awaitAll()

        val endTime = Clock.System.now()
        val executionTime = endTime - startTime

        // Aggregate results
        aggregateResults(results, configuration, context, executionTime)
    }

    /**
     * Validates performance metrics specifically.
     *
     * @param projectPath The project path to validate
     * @param configuration Optional configuration overrides
     * @return Performance validation metrics
     */
    suspend fun validatePerformance(
        projectPath: String,
        configuration: Map<String, Any> = emptyMap()
    ): PerformanceMetrics {
        val validator = PerformanceValidator()
        val context = ValidationContext(
            projectPath = projectPath,
            configuration = configuration
        )
        return validator.validate(context)
    }

    /**
     * Validates compilation across platforms.
     *
     * @param projectPath The project path to validate
     * @param platforms Platforms to compile (null = all)
     * @return Compilation validation results
     */
    suspend fun validateCompilation(
        projectPath: String,
        platforms: List<String>? = null
    ): CompilationResult {
        val validator = CompilationValidator()
        val context = ValidationContext(
            projectPath = projectPath,
            platforms = platforms
        )
        return validator.validate(context)
    }

    /**
     * Validates test coverage and test suite health.
     *
     * @param projectPath The project path to validate
     * @return Test coverage validation results
     */
    suspend fun validateTestCoverage(
        projectPath: String
    ): TestResults {
        val validator = TestCoverageValidator()
        val context = ValidationContext(projectPath = projectPath)
        return validator.validate(context)
    }

    /**
     * Validates constitutional compliance (TDD, code quality, etc).
     *
     * @param projectPath The project path to validate
     * @return Constitutional compliance results
     */
    suspend fun validateConstitutionalCompliance(
        projectPath: String
    ): ConstitutionalCompliance {
        val validator = ConstitutionalValidator()
        val context = ValidationContext(projectPath = projectPath)
        return validator.validate(context)
    }

    /**
     * Validates security vulnerabilities and best practices.
     *
     * @param projectPath The project path to validate
     * @return Security validation results
     */
    suspend fun validateSecurity(
        projectPath: String
    ): SecurityValidationResult {
        val validator = SecurityValidator()
        val context = ValidationContext(projectPath = projectPath)
        return validator.validate(context)
    }

    /**
     * Adds a custom validator to the validation pipeline.
     *
     * @param validator The validator to add
     */
    fun registerValidator(validator: Validator<*>) {
        validators.add(validator)
    }

    /**
     * Removes a validator from the validation pipeline.
     *
     * @param validatorName The name of the validator to remove
     * @return true if removed, false if not found
     */
    fun unregisterValidator(validatorName: String): Boolean {
        return validators.removeAll { it.name == validatorName }
    }

    /**
     * Gets the list of registered validator names.
     */
    fun getRegisteredValidators(): List<String> {
        return validators.map { it.name }
    }

    /**
     * Generates actionable recommendations based on validation results.
     *
     * @param report The production readiness report
     * @return List of prioritized recommendations
     */
    fun generateRecommendations(report: ProductionReadinessReport): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // Generate recommendations from failed categories
        report.categories.forEach { category ->
            if (category.status == ValidationStatus.FAILED) {
                category.failedCriteria.forEach { criterion ->
                    recommendations.add(
                        Recommendation(
                            priority = when (criterion.severity) {
                                Severity.CRITICAL -> Priority.CRITICAL
                                Severity.HIGH -> Priority.HIGH
                                Severity.MEDIUM -> Priority.MEDIUM
                                Severity.LOW -> Priority.LOW
                            },
                            category = category.name,
                            issue = criterion.description,
                            action = "Fix ${criterion.name}",
                            estimatedEffort = EffortLevel.MEDIUM
                        )
                    )
                }
            }
        }

        // Generate recommendations from remediation actions
        report.remediationActions.forEach { action ->
            recommendations.add(
                Recommendation(
                    priority = when (action.priority) {
                        1 -> Priority.CRITICAL
                        2 -> Priority.HIGH
                        3 -> Priority.MEDIUM
                        else -> Priority.LOW
                    },
                    category = action.criterionId,
                    issue = action.description,
                    action = action.title,
                    estimatedEffort = estimateEffortFromTime(action.estimatedEffort)
                )
            )
        }

        // Sort by priority
        return recommendations.sortedBy { it.priority.ordinal }
    }

    /**
     * Estimates effort level from time string.
     */
    private fun estimateEffortFromTime(timeStr: String): EffortLevel {
        return when {
            "hour" in timeStr.lowercase() -> {
                val hours = timeStr.filter { it.isDigit() }.toIntOrNull() ?: 1
                if (hours <= 1) EffortLevel.LOW else if (hours <= 4) EffortLevel.MEDIUM else EffortLevel.HIGH
            }

            "day" in timeStr.lowercase() || "week" in timeStr.lowercase() -> EffortLevel.HIGH
            "minute" in timeStr.lowercase() -> EffortLevel.LOW
            else -> EffortLevel.MEDIUM
        }
    }

    /**
     * Registers the default set of validators.
     */
    private fun registerDefaultValidators() {
        validators.apply {
            add(CompilationValidator())
            add(TestCoverageValidator())
            add(PerformanceValidator())
            add(ConstitutionalValidator())
            add(SecurityValidator())
        }
    }

    /**
     * Aggregates individual validation results into a comprehensive report.
     */
    private fun aggregateResults(
        results: List<ValidatorResult>,
        configuration: ValidationConfiguration,
        context: ValidationContext,
        executionTime: Duration
    ): ProductionReadinessReport {

        val categories = mutableListOf<io.kreekt.validation.models.ValidationCategory>()
        val remediationActions = mutableListOf<io.kreekt.validation.models.RemediationAction>()

        // Get git information (stub for now)
        val branchName = "main" // TODO: Get from git
        val commitHash = "unknown" // TODO: Get from git

        // Process each validation result
        results.forEach { validatorResult ->
            val result = validatorResult.result

            if (result != null) {
                // Convert to ValidationCategory
                val categoryName = getCategoryNameForValidator(validatorResult.name)
                val criteria = extractCriteria(result, validatorResult.name)
                val categoryScore = result.score
                val categoryStatus = result.status

                categories.add(
                    io.kreekt.validation.models.ValidationCategory(
                        name = categoryName,
                        status = categoryStatus,
                        score = categoryScore,
                        weight = getCategoryWeight(categoryName),
                        criteria = criteria
                    )
                )

                // Extract remediation actions from failures
                if (result.status == ValidationStatus.FAILED) {
                    remediationActions.addAll(generateRemediationActions(result, categoryName))
                }
            } else if (validatorResult.error != null) {
                // Handle validation errors
                val errorCategory = io.kreekt.validation.models.ValidationCategory(
                    name = validatorResult.name,
                    status = ValidationStatus.ERROR,
                    score = 0.0f,
                    weight = 0.1f,
                    criteria = listOf(
                        ValidationCriterion(
                            id = "${validatorResult.name}-error",
                            name = "Validator Execution",
                            description = "Validator failed to execute: ${validatorResult.error.message}",
                            requirement = "Success",
                            actual = "Error: ${validatorResult.error.message}",
                            status = ValidationStatus.ERROR,
                            severity = Severity.HIGH,
                            details = emptyMap()
                        )
                    )
                )
                categories.add(errorCategory)
            }
        }

        // Calculate overall score
        val overallScore = calculateOverallScore(categories)

        // Determine overall status
        val overallStatus = determineOverallStatus(categories)

        return ProductionReadinessReport(
            timestamp = Clock.System.now(),
            branchName = branchName,
            commitHash = commitHash,
            overallStatus = overallStatus,
            overallScore = overallScore,
            categories = categories,
            remediationActions = remediationActions,
            executionTime = executionTime
        )
    }

    /**
     * Gets the category name for a validator.
     */
    private fun getCategoryNameForValidator(validatorName: String): String {
        return when {
            validatorName.contains(
                "Compilation",
                ignoreCase = true
            ) -> io.kreekt.validation.models.ValidationCategory.COMPILATION

            validatorName.contains("Test", ignoreCase = true) -> io.kreekt.validation.models.ValidationCategory.TESTING
            validatorName.contains(
                "Performance",
                ignoreCase = true
            ) -> io.kreekt.validation.models.ValidationCategory.PERFORMANCE

            validatorName.contains(
                "Constitutional",
                ignoreCase = true
            ) -> io.kreekt.validation.models.ValidationCategory.CONSTITUTIONAL

            validatorName.contains(
                "Security",
                ignoreCase = true
            ) -> io.kreekt.validation.models.ValidationCategory.SECURITY

            else -> "Infrastructure"
        }
    }

    /**
     * Gets the weight for a category.
     */
    private fun getCategoryWeight(categoryName: String): Float {
        return when (categoryName) {
            io.kreekt.validation.models.ValidationCategory.COMPILATION -> 0.25f
            io.kreekt.validation.models.ValidationCategory.TESTING -> 0.20f
            io.kreekt.validation.models.ValidationCategory.PERFORMANCE -> 0.20f
            io.kreekt.validation.models.ValidationCategory.CONSTITUTIONAL -> 0.15f
            io.kreekt.validation.models.ValidationCategory.SECURITY -> 0.15f
            else -> 0.05f
        }
    }

    /**
     * Extracts validation criteria from a result.
     */
    private fun extractCriteria(result: ValidationResult, validatorName: String): List<ValidationCriterion> {
        val criteria = mutableListOf<ValidationCriterion>()

        when (result) {
            is CompilationResult -> {
                criteria.add(
                    ValidationCriterion(
                        id = "$validatorName-compilation",
                        name = "Compilation Success",
                        description = "All platforms compile successfully",
                        severity = Severity.CRITICAL,
                        status = result.status,
                        requirement = "All platforms pass",
                        actual = "${result.platformResults.count { it.value.success }}/${result.platformResults.size} platforms pass",
                        details = emptyMap()
                    )
                )
            }

            is TestResults -> {
                criteria.add(
                    ValidationCriterion(
                        id = "$validatorName-tests",
                        name = "Test Success Rate",
                        description = "All tests pass",
                        severity = Severity.HIGH,
                        status = result.status,
                        requirement = "100% pass rate",
                        actual = "${result.passedTests}/${result.totalTests} tests pass",
                        details = emptyMap()
                    )
                )
                criteria.add(
                    ValidationCriterion(
                        id = "$validatorName-coverage",
                        name = "Test Coverage",
                        description = "Adequate test coverage",
                        severity = Severity.MEDIUM,
                        status = if (result.lineCoverage >= 80f) ValidationStatus.PASSED else ValidationStatus.FAILED,
                        requirement = "≥ 80%",
                        actual = "${result.lineCoverage}%",
                        details = emptyMap()
                    )
                )
            }

            is PerformanceMetrics -> {
                criteria.add(
                    ValidationCriterion(
                        id = "$validatorName-fps",
                        name = "Frame Rate",
                        description = "Meets 60 FPS requirement",
                        severity = Severity.CRITICAL,
                        status = if (result.minFps >= 60f) ValidationStatus.PASSED else ValidationStatus.FAILED,
                        requirement = "≥ 60 FPS",
                        actual = "${result.minFps} FPS",
                        details = emptyMap()
                    )
                )
            }

            is ConstitutionalCompliance -> {
                criteria.add(
                    ValidationCriterion(
                        id = "$validatorName-placeholders",
                        name = "No Placeholder Code",
                        description = "All code is implemented",
                        severity = Severity.HIGH,
                        status = if (result.placeholderCodeCount == 0) ValidationStatus.PASSED else ValidationStatus.FAILED,
                        requirement = "0 placeholders",
                        actual = "${result.placeholderCodeCount} placeholders",
                        details = emptyMap()
                    )
                )
                criteria.add(
                    ValidationCriterion(
                        id = "$validatorName-tdd",
                        name = "TDD Compliance",
                        description = "Follows TDD practices",
                        severity = Severity.MEDIUM,
                        status = if (result.tddCompliance.isCompliant) ValidationStatus.PASSED else ValidationStatus.WARNING,
                        requirement = "TDD compliant",
                        actual = if (result.tddCompliance.isCompliant) "Compliant" else "Non-compliant",
                        details = emptyMap()
                    )
                )
            }

            is SecurityValidationResult -> {
                criteria.add(
                    ValidationCriterion(
                        id = "$validatorName-vulnerabilities",
                        name = "No Security Vulnerabilities",
                        description = "No security issues found",
                        severity = Severity.CRITICAL,
                        status = if (result.vulnerabilities.isEmpty()) ValidationStatus.PASSED else ValidationStatus.FAILED,
                        requirement = "0 vulnerabilities",
                        actual = "${result.vulnerabilities.size} vulnerabilities",
                        details = emptyMap()
                    )
                )
            }
        }

        // Generic criterion if none were added
        if (criteria.isEmpty()) {
            criteria.add(
                ValidationCriterion(
                    id = "$validatorName-general",
                    name = validatorName,
                    description = result.message,
                    severity = Severity.MEDIUM,
                    status = result.status,
                    requirement = "Passed",
                    actual = result.status.name,
                    details = emptyMap()
                )
            )
        }

        return criteria
    }

    /**
     * Generates remediation actions from a failed result.
     */
    private fun generateRemediationActions(
        result: ValidationResult,
        categoryName: String
    ): List<io.kreekt.validation.models.RemediationAction> {
        val actions = mutableListOf<io.kreekt.validation.models.RemediationAction>()

        when (result) {
            is CompilationResult -> {
                result.errors.forEach { error ->
                    actions.add(
                        io.kreekt.validation.models.RemediationAction(
                            criterionId = "compilation-error",
                            title = "Fix compilation error in ${error.file}",
                            description = error.message,
                            steps = listOf("Review error at ${error.file}:${error.line}", "Fix the compilation issue"),
                            estimatedEffort = "15 minutes",
                            priority = 1,
                            automatable = false
                        )
                    )
                }
            }

            is TestResults -> {
                if (result.failedTests > 0) {
                    actions.add(
                        io.kreekt.validation.models.RemediationAction(
                            criterionId = "test-failures",
                            title = "Fix ${result.failedTests} failing tests",
                            description = "Tests are failing and need to be fixed",
                            steps = listOf("Review failed tests", "Debug and fix issues", "Verify all tests pass"),
                            estimatedEffort = "${result.failedTests * 20} minutes",
                            priority = 2,
                            automatable = false
                        )
                    )
                }
                if (result.lineCoverage < 80f) {
                    actions.add(
                        io.kreekt.validation.models.RemediationAction(
                            criterionId = "test-coverage",
                            title = "Increase test coverage to 80%",
                            description = "Test coverage is below the required threshold",
                            steps = listOf(
                                "Identify uncovered code",
                                "Write tests for uncovered paths",
                                "Verify coverage threshold"
                            ),
                            estimatedEffort = "2 hours",
                            priority = 3,
                            automatable = false
                        )
                    )
                }
            }

            is ConstitutionalCompliance -> {
                if (result.placeholderCodeCount > 0) {
                    actions.add(
                        io.kreekt.validation.models.RemediationAction(
                            criterionId = "placeholder-code",
                            title = "Replace ${result.placeholderCodeCount} placeholders with implementations",
                            description = "Placeholder code needs to be implemented",
                            steps = listOf(
                                "Review each placeholder",
                                "Implement proper functionality",
                                "Remove TODO/FIXME markers"
                            ),
                            estimatedEffort = "${result.placeholderCodeCount * 30} minutes",
                            priority = 2,
                            automatable = false
                        )
                    )
                }
            }
        }

        return actions
    }

    /**
     * Calculates the overall validation score.
     */
    private fun calculateOverallScore(categories: List<io.kreekt.validation.models.ValidationCategory>): Float {
        if (categories.isEmpty()) return 0.0f

        var totalWeight = 0.0f
        var weightedSum = 0.0f

        categories.forEach { category ->
            weightedSum += category.score * category.weight
            totalWeight += category.weight
        }

        return if (totalWeight > 0) (weightedSum / totalWeight) else 0.0f
    }

    /**
     * Determines overall validation status.
     */
    private fun determineOverallStatus(categories: List<io.kreekt.validation.models.ValidationCategory>): ValidationStatus {
        if (categories.isEmpty()) return ValidationStatus.SKIPPED

        val hasCriticalFailures = categories.any { it.hasCriticalFailures }
        if (hasCriticalFailures) return ValidationStatus.FAILED

        val hasFailures = categories.any { it.status == ValidationStatus.FAILED }
        if (hasFailures) return ValidationStatus.FAILED

        val hasErrors = categories.any { it.status == ValidationStatus.ERROR }
        if (hasErrors) return ValidationStatus.ERROR

        val hasWarnings = categories.any { it.status == ValidationStatus.WARNING }
        if (hasWarnings) return ValidationStatus.WARNING

        return ValidationStatus.PASSED
    }

    /**
     * Internal class to hold validator results.
     */
    private data class ValidatorResult(
        val name: String,
        val result: ValidationResult?,
        val error: Throwable?
    )
}

/**
 * Recommendation for improving production readiness.
 */
data class Recommendation(
    val priority: Priority,
    val category: String,
    val issue: String,
    val action: String,
    val estimatedEffort: EffortLevel
)

enum class Priority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

enum class EffortLevel {
    LOW,    // < 1 hour
    MEDIUM, // 1-4 hours
    HIGH    // > 4 hours
}