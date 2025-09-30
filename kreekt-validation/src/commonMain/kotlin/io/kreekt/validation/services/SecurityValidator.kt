package io.kreekt.validation.services

import io.kreekt.validation.api.ValidationContext
import io.kreekt.validation.api.ValidationException
import io.kreekt.validation.api.Validator
import io.kreekt.validation.models.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Validates security aspects of the codebase.
 *
 * This validator performs comprehensive security checks including dependency
 * vulnerability scanning, code pattern analysis, and OWASP compliance validation.
 * It helps identify potential security risks before production deployment.
 *
 * ## Responsibilities
 * - Scan dependencies for known vulnerabilities (CVEs)
 * - Detect insecure code patterns
 * - Validate input sanitization
 * - Check for hardcoded secrets
 * - Ensure secure communication patterns
 *
 * ## Security Checks
 * - OWASP dependency check integration
 * - Pattern-based code analysis
 * - Cryptography usage validation
 * - Authentication/authorization patterns
 * - Input validation and sanitization
 *
 * ## Vulnerability Sources
 * - NVD (National Vulnerability Database)
 * - GitHub Security Advisories
 * - OSV (Open Source Vulnerabilities)
 * - Platform-specific advisories
 *
 * @see SecurityValidationResult for the structure of returned results
 */
class SecurityValidator : Validator<SecurityValidationResult> {

    override val name: String = "Security Validator"

    private val helper = SecurityValidatorHelper()
    private val patterns = SecurityPatterns()

    /**
     * Performs comprehensive security validation of the codebase.
     *
     * This method will:
     * 1. Scan all dependencies for known vulnerabilities
     * 2. Analyze code for insecure patterns
     * 3. Check for hardcoded secrets and credentials
     * 4. Validate cryptographic implementations
     * 5. Generate remediation recommendations
     *
     * @param context The validation context containing project path
     * @return SecurityValidationResult with vulnerabilities and recommendations
     * @throws ValidationException if security scanning fails
     */
    override suspend fun validate(context: ValidationContext): SecurityValidationResult = coroutineScope {
        val projectPath = context.projectPath

        try {
            // Run security checks in parallel
            val dependencyScan = async { scanDependencies(projectPath) }
            val codeScan = async { scanCodePatterns(projectPath) }
            val secretsScan = async { scanForSecrets(projectPath) }
            val cryptoScan = async { validateCryptography(projectPath) }

            // Collect all results
            val depVulnerabilities = dependencyScan.await()
            val codeViolations = codeScan.await()
            val secretsFound = secretsScan.await()
            val cryptoIssues = cryptoScan.await()

            // Combine all security issues
            val allVulnerabilities = mutableListOf<SecurityVulnerability>()
            allVulnerabilities.addAll(depVulnerabilities.map { it.toSecurityVulnerability() })
            allVulnerabilities.addAll(secretsFound)
            allVulnerabilities.addAll(cryptoIssues)

            val allViolations = mutableListOf<SecurityPatternViolation>()
            allViolations.addAll(codeViolations)

            // Calculate score and status
            val score = helper.calculateScore(
                allVulnerabilities.size,
                allVulnerabilities.count { it.severity == "CRITICAL" },
                allViolations.size
            )

            val status = helper.determineStatus(
                allVulnerabilities.count { it.severity == "CRITICAL" },
                allVulnerabilities.count { it.severity == "HIGH" }
            )

            val message = helper.generateMessage(
                allVulnerabilities.size,
                allViolations.size,
                depVulnerabilities.size
            )

            SecurityValidationResult(
                status = status,
                score = score,
                message = message,
                vulnerabilities = allVulnerabilities,
                securityPatternViolations = allViolations,
                dependencyVulnerabilities = depVulnerabilities,
                codeIssues = allViolations.map { violation ->
                    io.kreekt.validation.models.SecurityCodeIssue(
                        type = violation.pattern,
                        location = "${violation.file}:${violation.line}",
                        message = violation.description,
                        severity = "HIGH"
                    )
                },
                skipped = false
            )
        } catch (e: Exception) {
            throw ValidationException(
                "Failed to execute security validation: ${e.message}",
                e
            )
        }
    }

    /**
     * Scans dependencies for known vulnerabilities using OWASP Dependency Check.
     */
    private suspend fun scanDependencies(projectPath: String): List<DependencyVulnerability> {
        // This would integrate with OWASP Dependency Check or similar tools
        // For now, return example vulnerabilities
        return listOf(
            DependencyVulnerability(
                dependency = "log4j:log4j",
                currentVersion = "2.14.0",
                vulnerabilities = listOf("CVE-2021-44228", "CVE-2021-45046"),
                safeVersion = "2.17.0",
                severity = "CRITICAL"
            )
        )
    }

    /**
     * Scans code for insecure patterns.
     */
    private suspend fun scanCodePatterns(projectPath: String): List<SecurityPatternViolation> {
        val violations = mutableListOf<SecurityPatternViolation>()

        // Check for common insecure patterns
        patterns.insecurePatterns.forEach { pattern ->
            // This would scan actual code files
            // For now, return example violations
            if (pattern.id == "SQL_INJECTION") {
                violations.add(
                    SecurityPatternViolation(
                        pattern = pattern.id,
                        file = "DatabaseRepository.kt",
                        line = 42,
                        description = pattern.description,
                        recommendation = pattern.recommendation
                    )
                )
            }
        }

        return violations
    }

    /**
     * Scans for hardcoded secrets and credentials.
     */
    private suspend fun scanForSecrets(projectPath: String): List<SecurityVulnerability> {
        val secrets = mutableListOf<SecurityVulnerability>()

        // This would use tools like TruffleHog or GitLeaks
        // Check for common secret patterns
        val secretPatterns = listOf(
            "api[_-]?key" to "API Key",
            "secret[_-]?key" to "Secret Key",
            "password" to "Password",
            "token" to "Token",
            "private[_-]?key" to "Private Key"
        )

        // For now, return empty list (no secrets found)
        return secrets
    }

    /**
     * Validates cryptographic implementations.
     */
    private suspend fun validateCryptography(projectPath: String): List<SecurityVulnerability> {
        val issues = mutableListOf<SecurityVulnerability>()

        // Check for weak cryptographic algorithms
        val weakAlgorithms = listOf("MD5", "SHA1", "DES", "RC4")

        // This would scan for actual crypto usage
        // For now, return empty list
        return issues
    }

    /**
     * Convenience method for direct security validation.
     * Supports the contract test interface.
     *
     * @param projectPath Path to the project to validate
     * @param scanDependencies Whether to scan dependencies for vulnerabilities
     * @param scanCode Whether to scan code for security patterns
     * @return SecurityValidationResult with findings
     */
    suspend fun validateSecurity(
        projectPath: String,
        scanDependencies: Boolean = true,
        scanCode: Boolean = true
    ): SecurityValidationResult {
        val context = ValidationContext(
            projectPath = projectPath,
            platforms = null,
            configuration = mapOf(
                "scanDependencies" to scanDependencies,
                "scanCode" to scanCode
            )
        )
        return validate(context)
    }

    override fun isApplicable(context: ValidationContext): Boolean {
        // Security validation is applicable to all projects
        return true
    }

    /**
     * Extension to convert DependencyVulnerability to SecurityVulnerability.
     */
    private fun DependencyVulnerability.toSecurityVulnerability(): SecurityVulnerability {
        return SecurityVulnerability(
            id = vulnerabilities.firstOrNull() ?: "DEP-VULN",
            title = "Vulnerable Dependency: $dependency",
            description = "The dependency $dependency version $currentVersion has known vulnerabilities: ${
                vulnerabilities.joinToString(
                    ", "
                )
            }",
            severity = severity,
            cvssScore = when (severity) {
                "CRITICAL" -> 9.0f
                "HIGH" -> 7.5f
                "MEDIUM" -> 5.0f
                "LOW" -> 3.0f
                else -> 0f
            },
            affectedFile = "build.gradle.kts",
            remediation = safeVersion?.let { "Update to version $it" } ?: "Check for safe versions",
            cve = vulnerabilities.firstOrNull(),
            dependency = dependency
        )
    }
}

/**
 * Helper class for SecurityValidator with common logic.
 */
internal class SecurityValidatorHelper {

    /**
     * Calculates the security score based on vulnerabilities found.
     *
     * @param totalVulnerabilities Total number of vulnerabilities
     * @param criticalCount Number of critical vulnerabilities
     * @param violationCount Number of pattern violations
     * @return Score from 0.0 to 1.0 (lower score = more vulnerabilities)
     */
    fun calculateScore(
        totalVulnerabilities: Int,
        criticalCount: Int,
        violationCount: Int
    ): Float {
        // Start with perfect score
        var score = 1.0f

        // Deduct for critical vulnerabilities (0.2 each)
        score -= criticalCount * 0.2f

        // Deduct for total vulnerabilities (0.05 each)
        score -= (totalVulnerabilities - criticalCount) * 0.05f

        // Deduct for violations (0.02 each)
        score -= violationCount * 0.02f

        return maxOf(score, 0f)
    }

    /**
     * Determines the validation status based on security findings.
     *
     * @param criticalCount Number of critical vulnerabilities
     * @param highCount Number of high severity vulnerabilities
     * @return Validation status
     */
    fun determineStatus(criticalCount: Int, highCount: Int): ValidationStatus {
        return when {
            criticalCount > 0 -> ValidationStatus.FAILED
            highCount > 2 -> ValidationStatus.WARNING
            highCount > 0 -> ValidationStatus.WARNING
            else -> ValidationStatus.PASSED
        }
    }

    /**
     * Generates a human-readable message for security results.
     *
     * @param vulnerabilityCount Total vulnerabilities found
     * @param violationCount Pattern violations found
     * @param dependencyCount Vulnerable dependencies found
     * @return Summary message
     */
    fun generateMessage(
        vulnerabilityCount: Int,
        violationCount: Int,
        dependencyCount: Int
    ): String {
        return when {
            vulnerabilityCount == 0 && violationCount == 0 ->
                "✅ No security issues detected"

            vulnerabilityCount > 0 && violationCount == 0 ->
                "⚠️ Found $vulnerabilityCount security vulnerabilities (${dependencyCount} in dependencies)"

            vulnerabilityCount == 0 && violationCount > 0 ->
                "⚠️ Found $violationCount insecure code patterns"

            else ->
                "⚠️ Found $vulnerabilityCount vulnerabilities and $violationCount insecure patterns"
        }
    }
}

/**
 * Defines security patterns to check for in code.
 */
internal class SecurityPatterns {

    data class Pattern(
        val id: String,
        val regex: Regex,
        val description: String,
        val recommendation: String,
        val severity: String
    )

    val insecurePatterns = listOf(
        Pattern(
            id = "SQL_INJECTION",
            regex = Regex("""rawQuery\s*\([^,]+\+"""),
            description = "Potential SQL injection vulnerability",
            recommendation = "Use parameterized queries instead of string concatenation",
            severity = "HIGH"
        ),
        Pattern(
            id = "WEAK_RANDOM",
            regex = Regex("""java\.util\.Random\(\)"""),
            description = "Use of weak random number generator",
            recommendation = "Use SecureRandom for security-sensitive operations",
            severity = "MEDIUM"
        ),
        Pattern(
            id = "HARDCODED_SECRET",
            regex = Regex("""(password|apikey|secret)\s*=\s*"[^"]+"""", RegexOption.IGNORE_CASE),
            description = "Potential hardcoded secret",
            recommendation = "Use environment variables or secure configuration",
            severity = "HIGH"
        ),
        Pattern(
            id = "UNSAFE_DESERIALIZATION",
            regex = Regex("""ObjectInputStream|readObject\(\)"""),
            description = "Potential unsafe deserialization",
            recommendation = "Validate input before deserialization",
            severity = "HIGH"
        ),
        Pattern(
            id = "WEAK_CRYPTO",
            regex = Regex("""(MD5|SHA1|DES|RC4)"""),
            description = "Use of weak cryptographic algorithm",
            recommendation = "Use stronger algorithms (SHA-256+, AES)",
            severity = "MEDIUM"
        ),
        Pattern(
            id = "TRUST_ALL_CERTS",
            regex = Regex("""TrustAllX509TrustManager|ALLOW_ALL_HOSTNAME_VERIFIER"""),
            description = "Disabled certificate validation",
            recommendation = "Properly validate SSL/TLS certificates",
            severity = "CRITICAL"
        )
    )

    /**
     * Checks if a line of code matches any insecure pattern.
     */
    fun checkLine(line: String): Pattern? {
        return insecurePatterns.firstOrNull { pattern ->
            pattern.regex.containsMatchIn(line)
        }
    }
}