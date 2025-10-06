package io.kreekt.validation

import io.kreekt.renderer.backend.BackendInitializationResult
import io.kreekt.telemetry.BackendTelemetryEmitter

/**
 * Renderer validation hooks that connect to telemetry pipeline.
 * Provides compliance validation for backend initialization and rendering.
 */
class RendererValidation(
    private val telemetry: BackendTelemetryEmitter = BackendTelemetryEmitter()
) {

    /**
     * Validate backend initialization result and emit telemetry.
     */
    fun validateBackendInitialization(result: BackendInitializationResult): RendererValidationResult {
        return when (result) {
            is BackendInitializationResult.Success -> {
                // Check if initialization met constitutional requirements
                val meetsRequirements = result.initializationStats.withinBudget &&
                                      result.parityReport.meetsMinimumRequirements()

                if (meetsRequirements) {
                    RendererValidationResult.Passed(
                        message = "Backend initialization successful and meets requirements"
                    )
                } else {
                    RendererValidationResult.Warning(
                        message = "Backend initialized but performance/parity below optimal",
                        details = buildList {
                            if (!result.initializationStats.withinBudget) {
                                add("Initialization time exceeded budget: ${result.initializationStats.initTimeMs}ms > ${result.initializationStats.budgetMs}ms")
                            }
                            if (!result.parityReport.meetsMinimumRequirements()) {
                                add("Feature parity below minimum: score=${result.parityReport.parityScore}")
                            }
                        }
                    )
                }
            }

            is BackendInitializationResult.Denied -> {
                RendererValidationResult.Failed(
                    message = "Backend initialization denied: ${result.message}",
                    error = null
                )
            }

            is BackendInitializationResult.InitializationFailed -> {
                RendererValidationResult.Failed(
                    message = "Backend initialization failed: ${result.message}",
                    error = result.cause
                )
            }
        }
    }

    /**
     * Validate frame performance against constitutional requirements.
     */
    fun validateFramePerformance(avgFps: Double, minFps: Double): RendererValidationResult {
        val meetsTargetFps = avgFps >= 60.0
        val meetsMinimumFps = minFps >= 30.0

        return when {
            meetsTargetFps && meetsMinimumFps -> RendererValidationResult.Passed(
                message = "Frame performance meets constitutional requirements"
            )
            meetsMinimumFps -> RendererValidationResult.Warning(
                message = "Frame performance below target (avg=${avgFps} FPS) but above minimum"
            )
            else -> RendererValidationResult.Failed(
                message = "Frame performance below minimum (min=${minFps} FPS < 30 FPS)"
            )
        }
    }
}

/**
 * Renderer validation result (distinct from production readiness validation).
 */
sealed class RendererValidationResult {
    data class Passed(val message: String) : RendererValidationResult()
    data class Warning(val message: String, val details: List<String> = emptyList()) : RendererValidationResult()
    data class Failed(val message: String, val error: Throwable? = null) : RendererValidationResult()

    fun isSuccess(): Boolean = this is Passed
    fun isWarning(): Boolean = this is Warning
    fun isFailure(): Boolean = this is Failed
}
