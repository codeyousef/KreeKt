package io.kreekt.validation.services

import io.kreekt.validation.api.ValidationContext
import io.kreekt.validation.api.Validator
import io.kreekt.validation.models.*

/**
 * JS-specific implementation of the CompilationValidator.
 *
 * Currently a stub implementation as browser-based compilation validation
 * is not feasible. This would need to be run in a Node.js environment
 * with access to the Kotlin/JS compiler.
 */
actual class CompilationValidator actual constructor() : Validator<CompilationResult> {

    actual override val name: String = "JS Compilation Validator (Stub)"

    /**
     * Convenience method to validate compilation for a given project path.
     *
     * @param projectPath The path to the project to validate
     * @param platforms The list of platforms to validate (null means all)
     * @param timeoutMillis Optional timeout in milliseconds
     * @return CompilationResult containing the validation results
     */
    suspend fun validateCompilation(
        projectPath: String,
        platforms: List<io.kreekt.validation.models.Platform>? = null,
        timeoutMillis: Long? = null
    ): CompilationResult {
        val platformStrings = platforms?.map { it.name.lowercase() }
        val config = if (timeoutMillis != null) {
            mapOf("timeoutMillis" to timeoutMillis)
        } else {
            emptyMap()
        }

        val context = ValidationContext(
            projectPath = projectPath,
            platforms = platformStrings,
            configuration = config
        )
        return validate(context)
    }

    actual override suspend fun validate(context: ValidationContext): CompilationResult {
        // Stub implementation - JS validation would require Node.js environment
        return CompilationResult(
            status = ValidationStatus.SKIPPED,
            score = 1.0f,
            message = "JS compilation validation not implemented (requires Node.js environment)",
            platformResults = emptyMap(),
            errors = emptyList(),
            warnings = emptyList(),
            compilationTime = 0L
        )
    }
}