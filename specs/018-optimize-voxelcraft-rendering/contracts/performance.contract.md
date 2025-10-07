# Performance Contract

**Feature**: 018-optimize-voxelcraft-rendering
**Functional Requirements**: FR-002, FR-003, FR-005, FR-009, FR-012

## Contract Interface

```kotlin
interface PerformanceContract {
    /**
     * FR-002: WebGPU performance target
     * Measures frames per second over specified duration.
     * MUST return >= 60 FPS for WebGPU backend (constitutional target).
     * @param durationMs Duration to measure in milliseconds (minimum 1000ms)
     * @return Average FPS over the measurement period
     */
    fun measureFPS(durationMs: Int): Float

    /**
     * FR-003: WebGL minimum performance
     * Validates that WebGL fallback meets constitutional minimum.
     * MUST return true if FPS >= 30.
     * @return true if minimum FPS requirement met
     */
    fun validateMinimumFPS(): Boolean

    /**
     * FR-005: Draw call limit validation
     * Counts draw calls in the most recent frame.
     * MUST return < 100 for 81 chunks.
     * @return Number of draw calls in last frame
     */
    fun countDrawCalls(): Int

    /**
     * FR-009: Performance metrics logging
     * Returns comprehensive performance metrics for debugging and monitoring.
     * Includes FPS, draw calls, triangles, backend type, memory usage.
     * @return PerformanceMetrics with all current stats
     */
    fun logPerformanceMetrics(): PerformanceMetrics

    /**
     * FR-012: HUD display update
     * Updates on-screen performance display with current metrics.
     * Called every frame to keep HUD synchronized.
     * @param metrics The metrics to display
     */
    fun updatePerformanceHUD(metrics: PerformanceMetrics): Unit
}

/**
 * Comprehensive performance metrics structure.
 */
data class PerformanceMetrics(
    val fps: Float,                    // Frames per second (rolling average)
    val frameTime: Float,              // Frame time in milliseconds
    val drawCalls: Int,                // Draw calls in last frame
    val triangles: Int,                // Triangles rendered in last frame
    val backendType: String,           // "WebGPU 1.0" or "WebGL 2.0"
    val gpuMemory: Int = 0,            // Estimated GPU memory usage in MB
    val pipelineCacheHits: Int = 0,    // Pipeline cache hit count
    val pipelineCacheMisses: Int = 0,  // Pipeline cache miss count
    val culledChunks: Int = 0,         // Chunks culled by frustum
    val visibleChunks: Int = 0         // Chunks rendered
)
```

## Contract Test Requirements

### FPS Measurement Tests (FR-002, FR-003)
- Measure FPS with WebGPU backend over 2 seconds
- Assert average FPS >= 60
- Measure FPS with WebGL backend over 2 seconds
- Assert average FPS >= 30
- Validate rolling average calculation (no spikes/dips)

### Draw Call Tests (FR-005)
- Render frame with 81 chunks
- Count draw calls using `countDrawCalls()`
- Assert count < 100
- Validate frustum culling reduces count (test with camera showing 40 chunks)
- Assert count ~ 40

### Metrics Logging Tests (FR-009, FR-012)
- Call `logPerformanceMetrics()`
- Assert all fields populated (no null/undefined)
- Validate backend type matches active renderer
- Check triangle count matches geometry (81 chunks * ~5000 triangles = ~405K total, ~160K visible)
- Verify HUD updates without errors

## Acceptance Criteria

**FR-002**: WebGPU achieves sustained 60 FPS (rolling average over 60 frames)
**FR-003**: WebGL achieves sustained 30+ FPS (rolling average over 60 frames)
**FR-005**: Draw calls < 100 for 81 chunks (typically 81-85 without culling, 40-50 with culling)
**FR-009**: Metrics logged every frame with <0.1ms overhead
**FR-012**: HUD displays accurate, real-time metrics
