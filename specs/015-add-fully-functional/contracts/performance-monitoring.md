# Contract: Performance Monitoring API

## Purpose
Standardize how the renderer captures and evaluates performance metrics and enforces the 60/30 FPS and <3s initialization requirements on integrated GPUs.

## Interface Sketch (Kotlin)
```kotlin
interface PerformanceMonitor {
    fun beginInitializationTrace(backendId: BackendId)
    fun endInitializationTrace(backendId: BackendId): InitializationStats
    fun recordFrameMetrics(metrics: FrameMetrics)
    fun evaluateBudget(window: FrameWindow): PerformanceAssessment
}

data class FrameMetrics(
    val backendId: BackendId,
    val frameTimeMs: Double,
    val gpuTimeMs: Double,
    val cpuTimeMs: Double,
    val timestamp: Long
)

data class PerformanceAssessment(
    val backendId: BackendId,
    val avgFps: Double,
    val minFps: Double,
    val withinBudget: Boolean,
    val notes: String?
)
```

## Behavioral Requirements
1. Initialization traces MUST record wall-clock duration and persist alongside telemetry events.
2. Frame metrics MUST aggregate over rolling windows (default 120 frames) to smooth short spikes.
3. `evaluateBudget` MUST flag `withinBudget=false` when `avgFps < 60` or `minFps < 30` on the integrated GPU baseline.
4. When budget violations occur, the monitor MUST emit a `PERFORMANCE_DEGRADED` log entry referencing the associated capability report.
5. Monitoring MUST operate on all platforms (WebGPU, Vulkan Desktop, Android, iOS) with platform-specific timer sources abstracted behind expect/actual implementations.

## Contract Test Scenarios (to implement)
1. **Initialization Budget**: Simulate init trace exceeding 3000ms → expect `withinBudget=false` and `PERFORMANCE_DEGRADED` event.
2. **Frame Window Average**: Feed 120 frames averaging 16ms → expect `avgFps≈62.5`, `withinBudget=true`.
3. **Frame Window Drop**: Inject frames bringing `minFps` to 25 → expect `withinBudget=false` flagged.
4. **Cross-Platform Timer Consistency**: Validate Web and Desktop timers produce <1ms drift over 600 frames.

Tests should reside under `src/commonTest/.../PerformanceMonitorContractTest.kt` and initially fail.
