package io.kreekt.validation.services

import io.kreekt.validation.api.ValidationContext
import io.kreekt.validation.api.Validator
import io.kreekt.validation.models.*

/**
 * Native-specific implementation of the CompilationValidator.
 *
 * Currently a stub implementation. Native compilation validation would require
 * platform-specific process execution capabilities which vary across native targets.
 */
actual class CompilationValidator actual constructor() : Validator<CompilationResult> {

    actual override val name: String = "Native Compilation Validator (Stub)"

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
        // Stub implementation - Native validation would require platform-specific APIs
        return CompilationResult(
            status = ValidationStatus.SKIPPED,
            score = 1.0f,
            message = "Native compilation validation not implemented (platform-specific)",
            platformResults = emptyMap(),
            errors = emptyList(),
            warnings = emptyList(),
            compilationTime = 0L
        )
    }
}