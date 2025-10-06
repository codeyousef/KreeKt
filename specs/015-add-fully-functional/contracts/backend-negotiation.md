# Contract: Backend Negotiation API

## Overview
Defines the shared API that discovers device capabilities, selects an appropriate backend (WebGPU or Vulkan), initializes it, and handles fail-fast behavior when no backend qualifies.

## Interface Sketch (Kotlin)
```kotlin
interface BackendNegotiator {
    suspend fun detectCapabilities(request: CapabilityRequest): DeviceCapabilityReport
    suspend fun selectBackend(report: DeviceCapabilityReport, profiles: List<RenderingBackendProfile>): BackendSelection
    suspend fun initializeBackend(selection: BackendSelection, surface: SurfaceConfig): BackendHandle
}

data class BackendSelection(
    val backendId: BackendId,
    val reason: SelectionReason,
    val parityMatrix: FeatureParityMatrix
)

data class BackendHandle(
    val backendId: BackendId,
    val surfaceDescriptor: RenderSurfaceDescriptor,
    val diagnosticsLogId: String
)
```

## Behavioral Requirements
1. `detectCapabilities` MUST populate all telemetry fields (device/vendor/driver/OS/feature flags) before returning.
2. `selectBackend` MUST prefer the highest-priority profile with all mandatory features (`COMPUTE`, `RAY_TRACING`, `XR_SURFACE`) supported.
3. If no backend qualifies, `selectBackend` MUST emit a `DENIED` telemetry event and return a `BackendSelection` flagged as `FAILED`.
4. `initializeBackend` MUST complete within the `performanceBudget.initBudgetMs` target; otherwise emit `PERFORMANCE_DEGRADED` and fail initialization.
5. Any failure in initialization MUST trigger fail-fast behavior with actionable messaging exposed to the caller.

## Contract Test Scenarios (to implement)
1. **Happy Path (WebGPU)**: Given a report with WebGPU features supported, when selecting backends, the negotiator chooses WebGPU and initialization produces a surface descriptor matching request dimensions.
2. **Fallback Denied**: Given no backend meets parity requirements, the negotiator emits a `DENIED` telemetry event and surfaces a fail-fast error without attempting reduced-fidelity fallbacks.
3. **Initialization Timeout**: Given backend initialization exceeds `initBudgetMs`, the negotiator emits `PERFORMANCE_DEGRADED` and returns failure.
4. **Device Loss Recovery**: When `initializeBackend` returns a handle and a subsequent device loss occurs, the negotiator attempts a single reinitialization before failing fast with diagnostics.

Tests should live under `src/commonTest/.../BackendNegotiationContractTest.kt` and fail until the backend work is implemented.
