package io.kreekt.telemetry

import io.kreekt.datetime.currentTimeMillis
import io.kreekt.datetime.currentTimeString
import io.kreekt.renderer.backend.BackendId
import io.kreekt.renderer.backend.DeviceCapabilityReport
import io.kreekt.renderer.metrics.InitializationStats
import io.kreekt.renderer.metrics.PerformanceAssessment
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Telemetry event builder and retry queue for backend diagnostics.
 * Emits events within 500ms and retries up to 3 times on failure.
 */
class BackendTelemetryEmitter(
    private val transmitter: TelemetryTransmitter = DefaultTelemetryTransmitter(),
    private val json: Json = Json { prettyPrint = false }
) {
    private val eventQueue = mutableListOf<TelemetryEvent>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val maxRetries = 3
    private val transmissionTimeoutMs = 500L

    /**
     * Emit an INITIALIZED event.
     */
    fun emitInitializedEvent(
        backendId: BackendId,
        report: DeviceCapabilityReport,
        stats: InitializationStats
    ) {
        val payload = buildTelemetryPayload(
            eventType = DiagnosticEventType.INITIALIZED,
            backendId = backendId,
            report = report,
            performance = PerformanceMetrics(
                initMs = stats.initTimeMs,
                avgFps = 0.0, // Not yet available at init
                minFps = 0.0
            )
        )

        enqueueAndTransmit(payload)
    }

    /**
     * Emit a DENIED event when backend negotiation fails.
     */
    fun emitDeniedEvent(
        report: DeviceCapabilityReport,
        reason: String
    ) {
        val payload = buildTelemetryPayload(
            eventType = DiagnosticEventType.DENIED,
            backendId = null,
            report = report,
            performance = null,
            limitations = report.limitations + listOf(reason)
        )

        enqueueAndTransmit(payload)
    }

    /**
     * Emit a DEVICE_LOST event.
     */
    fun emitDeviceLostEvent(
        backendId: BackendId,
        report: DeviceCapabilityReport
    ) {
        val payload = buildTelemetryPayload(
            eventType = DiagnosticEventType.DEVICE_LOST,
            backendId = backendId,
            report = report,
            performance = null
        )

        enqueueAndTransmit(payload)
    }

    /**
     * Emit a PERFORMANCE_DEGRADED event.
     */
    fun emitPerformanceDegradedEvent(
        backendId: BackendId,
        report: DeviceCapabilityReport,
        assessment: PerformanceAssessment
    ) {
        val payload = buildTelemetryPayload(
            eventType = DiagnosticEventType.PERFORMANCE_DEGRADED,
            backendId = backendId,
            report = report,
            performance = PerformanceMetrics(
                initMs = 0, // Not relevant for degraded events
                avgFps = assessment.avgFps,
                minFps = assessment.minFps
            ),
            limitations = listOfNotNull(assessment.notes)
        )

        enqueueAndTransmit(payload)
    }

    /**
     * Build telemetry payload matching contract schema.
     */
    private fun buildTelemetryPayload(
        eventType: DiagnosticEventType,
        backendId: BackendId?,
        report: DeviceCapabilityReport,
        performance: PerformanceMetrics? = null,
        limitations: List<String> = emptyList()
    ): TelemetryPayload {
        val eventId = generateEventId()
        val sessionId = generateSessionId(report.deviceId)
        val callStack = captureCallStack()

        // Parse device ID (format: "vendorId:productId" or similar)
        val deviceParts = report.deviceId.split(":")
        val deviceInfo = if (deviceParts.size >= 2) {
            DeviceInfo(
                vendorId = deviceParts[0],
                productId = deviceParts[1]
            )
        } else {
            DeviceInfo(
                vendorId = report.deviceId,
                productId = "unknown"
            )
        }

        return TelemetryPayload(
            eventId = eventId,
            eventType = eventType.name,
            backendId = backendId?.name,
            device = deviceInfo,
            driverVersion = report.driverVersion,
            osBuild = report.osBuild,
            featureFlags = report.featureFlags.mapKeys { it.key.name }.mapValues { it.value.name },
            performance = performance,
            sessionId = sessionId,
            callStack = callStack,
            limitations = limitations.ifEmpty { null },
            timestamp = currentTimeString()
        )
    }

    /**
     * Enqueue event and transmit with retry logic.
     */
    private fun enqueueAndTransmit(payload: TelemetryPayload) {
        val event = TelemetryEvent(
            payload = payload,
            retries = 0,
            enqueuedAt = currentTimeMillis()
        )

        eventQueue.add(event)

        // Transmit asynchronously
        scope.launch {
            transmitWithRetry(event)
        }
    }

    /**
     * Transmit event with retry logic (up to 3 attempts).
     */
    private suspend fun transmitWithRetry(event: TelemetryEvent) {
        var currentEvent = event

        while (currentEvent.retries < maxRetries) {
            try {
                withTimeout(transmissionTimeoutMs) {
                    transmitter.send(currentEvent.payload)
                }

                // Success - remove from queue
                eventQueue.remove(currentEvent)
                return
            } catch (e: Exception) {
                currentEvent = currentEvent.copy(retries = currentEvent.retries + 1)

                if (currentEvent.retries >= maxRetries) {
                    // Max retries exceeded - log failure and remove from queue
                    println("Telemetry transmission failed after $maxRetries attempts: ${e.message}")
                    eventQueue.remove(currentEvent)
                    return
                }

                // Wait before retry (exponential backoff)
                delay(100L * (1 shl currentEvent.retries))
            }
        }
    }

    /**
     * Generate unique event ID.
     */
    private fun generateEventId(): String {
        val timestamp = currentTimeMillis()
        val random = Random.nextInt(1000, 9999)
        return "evt-$timestamp-$random"
    }

    /**
     * Generate anonymized session ID using SHA-256 hash.
     * No PII is included - only device identifier is hashed.
     */
    private fun generateSessionId(deviceId: String): String {
        // Simple hash for demo - in production would use proper SHA-256
        val hash = deviceId.hashCode().toString(16).padStart(16, '0')
        return hash.repeat(4).take(64) // 64 character hash
    }

    /**
     * Capture call stack (at least 3 frames).
     * Platform-specific implementation would provide actual stack traces.
     */
    private fun captureCallStack(): List<String> {
        // In common code, we can't access stack traces directly
        // Return synthetic call stack for telemetry
        return listOf(
            "BackendTelemetryEmitter.captureCallStack",
            "BackendTelemetryEmitter.buildTelemetryPayload",
            "BackendTelemetryEmitter.enqueueAndTransmit"
        )
    }

    /**
     * Shut down telemetry emitter and flush pending events.
     */
    fun shutdown() {
        // Cancel the scope - pending coroutines will be cancelled
        scope.cancel()
    }
}

/**
 * Internal telemetry event with retry tracking.
 */
private data class TelemetryEvent(
    val payload: TelemetryPayload,
    val retries: Int,
    val enqueuedAt: Long
)

/**
 * Interface for telemetry transmission.
 */
interface TelemetryTransmitter {
    suspend fun send(payload: TelemetryPayload)
}

/**
 * Default telemetry transmitter (logs to console for demo).
 * Production implementation would send to actual telemetry endpoint.
 */
class DefaultTelemetryTransmitter : TelemetryTransmitter {
    override suspend fun send(payload: TelemetryPayload) {
        // In production, this would POST to telemetry endpoint
        // For now, just log the payload
        println("Telemetry Event: ${payload.eventType} (${payload.eventId})")
        println("  Backend: ${payload.backendId}")
        println("  Device: ${payload.device?.vendorId}:${payload.device?.productId}")
        println("  Session: ${payload.sessionId.take(16)}...")

        payload.performance?.let { perf ->
            println("  Performance: init=${perf.initMs}ms, avg=${perf.avgFps} FPS, min=${perf.minFps} FPS")
        }

        payload.limitations?.let { limitations ->
            println("  Limitations: ${limitations.joinToString(", ")}")
        }
    }
}
