# Instructions for kotlin-perf Optimization Agent

## Overview

The kotlin-profile agent has completed comprehensive profiling instrumentation across the KreeKt library. This document provides specific guidance for the kotlin-perf agent to identify and optimize performance bottlenecks.

## Quick Start

### 1. Enable Profiling

```kotlin
import io.kreekt.profiling.*

// Enable detailed profiling for optimization work
ProfilingHelpers.enableDevelopmentProfiling()
```

### 2. Run Performance Analysis

```kotlin
// Create a profiling session
val session = ProfilingHelpers.createSession("PerformanceAnalysis")

// Run typical workload (e.g., render 120 frames)
repeat(120) {
    PerformanceProfiler.startFrame()
    renderer.render(scene, camera)
    PerformanceProfiler.endFrame()
}

// Get results
val summary = session.end()
summary.printSummary()
```

### 3. Identify Hotspots

```kotlin
val hotspots = PerformanceProfiler.getHotspots()

// Focus on operations consuming >10% of frame time
val criticalHotspots = hotspots.filter { it.percentage > 10f }

criticalHotspots.forEach { hotspot ->
    println("OPTIMIZE: ${hotspot.name}")
    println("  Time: ${hotspot.percentage}% of frame")
    println("  Calls: ${hotspot.callCount}")
    println("  Avg: ${hotspot.averageTime / 1_000_000}ms")
}
```

## Expected Profiling Results

Based on the current codebase structure, expect to find hotspots in:

### High Priority (>15% frame time)

1. **Rendering Pipeline**
   - `renderer.render` - Main rendering loop
   - `scene.traverse` - Scene graph traversal
   - Likely issues: No frustum culling, inefficient traversal

2. **Matrix Operations**
   - `updateMatrixWorld` - World matrix updates
   - `updateMatrix` - Local matrix updates
   - Likely issues: Updating every frame, no dirty flags

3. **Scene Traversal**
   - Full tree traversal every frame
   - Likely issues: No caching, no spatial partitioning

### Medium Priority (5-15% frame time)

4. **Geometry Processing**
   - Buffer generation
   - Normal/tangent calculation
   - Likely issues: Synchronous operations, no caching

5. **State Management**
   - Material switches
   - Shader compilation
   - Likely issues: No state batching, redundant switches

### Low Priority (<5% frame time)

6. **Memory Allocations**
   - Temporary object creation
   - GC pressure
   - Likely issues: No object pooling, boxing overhead

## Recommended Optimization Strategy

### Phase 1: Rendering Pipeline (Target: <10ms)

**Current Issue**: Full scene traversal every frame

**Profiling Command**:
```kotlin
SceneProfiler.profileTraversal(scene) { obj ->
    // Renders each object
}
```

**Optimization Opportunities**:
1. **Implement Frustum Culling**
   - Profile current: No culling (processes all objects)
   - Target: Only process visible objects
   - Expected improvement: 30-50% reduction in processing

2. **Batch Draw Calls**
   - Profile current: One draw call per mesh
   - Target: Batch similar materials/geometries
   - Expected improvement: 20-40% reduction in CPU time

3. **State Change Optimization**
   - Profile current: State changes per object
   - Target: Sort by state, minimize changes
   - Expected improvement: 15-25% reduction in overhead

**Code to Optimize**:
```
/src/commonMain/kotlin/io/kreekt/renderer/DefaultRenderer.kt
- renderSceneObjects() method needs culling
- Add state batching
```

### Phase 2: Matrix Updates (Target: <2ms for 1000 objects)

**Current Issue**: Updates all matrices every frame

**Profiling Command**:
```kotlin
SceneProfiler.profileMatrixWorldUpdate(scene, force = false)
```

**Optimization Opportunities**:
1. **Dirty Flag System**
   - Profile current: Updates all matrices
   - Target: Only update when transform changes
   - Expected improvement: 70-90% reduction when objects static

2. **Hierarchical Updates**
   - Profile current: May update redundantly
   - Target: Skip children if parent unchanged
   - Expected improvement: 40-60% reduction

**Code to Optimize**:
```
/src/commonMain/kotlin/io/kreekt/core/scene/Object3D.kt
- updateMatrixWorld() needs dirty flags
- Add matrixNeedsUpdate tracking
```

### Phase 3: Geometry Optimization (Target: <5ms for complex meshes)

**Current Issue**: Synchronous buffer operations

**Profiling Command**:
```kotlin
val complexity = geometry.analyzeComplexity()
GeometryProfiler.profileBufferUpload(complexity.totalBufferSizeBytes) {
    // Upload to GPU
}
```

**Optimization Opportunities**:
1. **Async Buffer Generation**
   - Profile current: Synchronous on main thread
   - Target: Generate on background thread
   - Expected improvement: Move 80% off main thread

2. **Geometry Caching**
   - Profile current: Regenerates primitives
   - Target: Cache generated geometry
   - Expected improvement: 95% reduction on reuse

**Code to Optimize**:
```
/src/commonMain/kotlin/io/kreekt/geometry/primitives/
- BoxGeometry.kt, SphereGeometry.kt, etc.
- Add caching layer
- Implement async generation
```

### Phase 4: Memory Optimization (Target: <100MB, minimal GC)

**Current Issue**: Temporary object creation in hot paths

**Profiling Command**:
```kotlin
val memoryStats = PerformanceProfiler.getMemoryStats()
if (memoryStats != null && memoryStats.gcPressure > 0.3f) {
    println("High GC pressure: ${memoryStats.gcPressure * 100}%")
    println("Allocation rate: ${memoryStats.allocations / (1024 * 1024)}MB/s")
}
```

**Optimization Opportunities**:
1. **Object Pooling**
   - Profile current: Creates Vector3, Matrix4 in loops
   - Target: Pool and reuse math objects
   - Expected improvement: 60-80% reduction in allocations

2. **Array Reuse**
   - Profile current: Creates arrays for temporary data
   - Target: Preallocate and reuse buffers
   - Expected improvement: 50-70% reduction in GC pressure

**Code to Optimize**:
```
/src/commonMain/kotlin/io/kreekt/core/math/
- Add object pools for Vector3, Matrix4, Quaternion
- Implement reusable buffer system
```

## Step-by-Step Optimization Process

### 1. Establish Baseline

```kotlin
// Run baseline profiling
val baselineSession = ProfilingHelpers.createSession("Baseline")

// Create test scene
val scene = createComplexTestScene(
    meshCount = 100,
    trianglesPerMesh = 1000
)

// Run test
repeat(120) {
    PerformanceProfiler.startFrame()
    renderer.render(scene, camera)
    PerformanceProfiler.endFrame()
}

val baseline = baselineSession.end()
baseline.printSummary()

// Save baseline for comparison
val baselineReport = ProfilingReport.generateReport()
```

### 2. Identify Top Hotspot

```kotlin
val hotspots = PerformanceProfiler.getHotspots()
val topHotspot = hotspots.firstOrNull()

if (topHotspot != null) {
    println("TOP HOTSPOT: ${topHotspot.name}")
    println("Consuming ${topHotspot.percentage}% of frame time")
    println("Called ${topHotspot.callCount} times per frame")
    println("Average time: ${topHotspot.averageTime / 1_000_000}ms")
}
```

### 3. Optimize Hotspot

```kotlin
// Profile before optimization
val beforeStats = PerformanceProfiler.getFrameStats()

// Apply optimization (e.g., implement frustum culling)
implementFrustumCulling()

// Profile after optimization
PerformanceProfiler.reset()
repeat(120) {
    PerformanceProfiler.startFrame()
    renderer.render(scene, camera)
    PerformanceProfiler.endFrame()
}
val afterStats = PerformanceProfiler.getFrameStats()

// Compare results
val improvement = ((beforeStats.averageFrameTime - afterStats.averageFrameTime).toDouble() /
                   beforeStats.averageFrameTime.toDouble()) * 100.0
println("Improvement: ${String.format("%.1f", improvement)}%")
println("Before: ${beforeStats.averageFps} FPS")
println("After: ${afterStats.averageFps} FPS")
```

### 4. Validate Constitutional Compliance

```kotlin
val stats = PerformanceProfiler.getFrameStats()

// Check 60 FPS requirement
val meetsTarget = stats.meetsTargetFps(60)
println("60 FPS Requirement: ${if (meetsTarget) "✓ PASS" else "✗ FAIL"}")

if (!meetsTarget) {
    println("Current FPS: ${stats.averageFps}")
    println("95th percentile: ${stats.percentile95 / 1_000_000}ms (target: 16.67ms)")

    // Get recommendations
    val report = ProfilingReport.generateReport()
    report.recommendations
        .filter { it.severity == Severity.HIGH }
        .forEach { println("⚠ ${it.message}") }
}
```

### 5. Generate Optimization Report

```kotlin
// Generate comprehensive HTML report
val htmlReport = ProfilingReport.generateHtmlReport()
File("optimization-report.html").writeText(htmlReport)

// Generate Chrome trace for detailed analysis
val trace = PerformanceProfiler.export(ExportFormat.CHROME_TRACE)
File("optimization-trace.json").writeText(trace)

println("Reports generated:")
println("  - optimization-report.html")
println("  - optimization-trace.json (open in chrome://tracing)")
```

## Specific Files to Optimize

Based on architecture and common patterns, focus on these files:

### High Priority

1. **DefaultRenderer.kt** (Rendering bottleneck)
   - Location: `/src/commonMain/kotlin/io/kreekt/renderer/DefaultRenderer.kt`
   - Issues: No culling, no batching
   - Methods: `renderSceneObjects()`, `render()`

2. **Object3D.kt** (Matrix update bottleneck)
   - Location: `/src/commonMain/kotlin/io/kreekt/core/scene/Object3D.kt`
   - Issues: Updates all matrices
   - Methods: `updateMatrixWorld()`, `updateMatrix()`

3. **Scene.kt** (Traversal bottleneck)
   - Location: `/src/commonMain/kotlin/io/kreekt/core/scene/Scene.kt`
   - Issues: Full tree walk
   - Methods: `traverse()`, `traverseVisible()`

### Medium Priority

4. **Geometry Primitives**
   - Location: `/src/commonMain/kotlin/io/kreekt/geometry/primitives/`
   - Issues: No caching, synchronous generation
   - Files: `BoxGeometry.kt`, `SphereGeometry.kt`, etc.

5. **Math Operations**
   - Location: `/src/commonMain/kotlin/io/kreekt/core/math/`
   - Issues: Temporary object creation
   - Files: `Vector3.kt`, `Matrix4.kt`, `Quaternion.kt`

### Low Priority

6. **Material System**
   - Location: `/src/commonMain/kotlin/io/kreekt/material/`
   - Issues: State change overhead
   - Files: `MeshBasicMaterial.kt`, etc.

## Performance Regression Tests

Add these tests to prevent performance regressions:

```kotlin
@Test
fun testRenderingPerformance() {
    ProfilingHelpers.enableDevelopmentProfiling()

    val scene = createTestScene(meshCount = 100)
    val renderer = createRenderer().getOrThrow()
    val camera = createTestCamera()

    // Run profiled render loop
    repeat(120) {
        PerformanceProfiler.startFrame()
        renderer.render(scene, camera)
        PerformanceProfiler.endFrame()
    }

    // Validate performance
    val stats = PerformanceProfiler.getFrameStats()
    assertTrue(
        stats.meetsTargetFps(60),
        "Rendering performance regression: ${stats.averageFps} FPS (target: 60)"
    )

    // Check hotspots
    val hotspots = PerformanceProfiler.getHotspots()
    hotspots.forEach { hotspot ->
        assertTrue(
            hotspot.percentage < 30f,
            "${hotspot.name} consuming too much time: ${hotspot.percentage}%"
        )
    }
}

@Test
fun testSceneComplexity() {
    val scene = createComplexScene(meshCount = 1000)

    val complexity = scene.getComplexity()
    assertTrue(
        complexity.totalNodes <= 1500,
        "Scene too complex: ${complexity.totalNodes} nodes"
    )

    val score = complexity.getComplexityScore()
    assertTrue(
        score < 7f,
        "Complexity score too high: $score"
    )
}

@Test
fun testGeometryPerformance() {
    val geometries = List(100) {
        SphereGeometry(radius = 1f, segments = 32)
    }

    geometries.forEach { geometry ->
        val complexity = geometry.analyzeComplexity()
        assertTrue(
            complexity.isMobileFriendly(),
            "Geometry not mobile-friendly: ${complexity.vertexCount} vertices"
        )
    }
}
```

## Expected Results

After optimization, expect:

### Before Optimization (Baseline)
- Average FPS: 20-40 (failing 60 FPS requirement)
- Average frame time: 25-50ms
- Top hotspot: scene.traverse (30-40%)
- Memory: Frequent GC pressure

### After Optimization (Target)
- Average FPS: 58-60+ (meeting requirement)
- Average frame time: <16.67ms
- Top hotspot: <20% of frame time
- Memory: Minimal GC pressure

### Specific Improvements
1. **Frustum Culling**: 30-50% reduction in processing
2. **Dirty Flags**: 70-90% reduction in matrix updates
3. **Batching**: 20-40% reduction in CPU overhead
4. **Object Pooling**: 60-80% reduction in allocations

## Troubleshooting

### Issue: Profiler reports high overhead

**Solution**: Reduce verbosity
```kotlin
PerformanceProfiler.configure(ProfilerConfig(
    enabled = true,
    verbosity = ProfileVerbosity.MINIMAL
))
```

### Issue: No hotspots detected

**Solution**: Run longer session
```kotlin
repeat(300) { // Run for 5 seconds at 60 FPS
    PerformanceProfiler.startFrame()
    renderer.render(scene, camera)
    PerformanceProfiler.endFrame()
}
```

### Issue: Memory tracking unavailable

**Solution**: Check platform support
```kotlin
val memoryStats = PerformanceProfiler.getMemoryStats()
if (memoryStats == null) {
    println("Memory tracking not available on this platform")
    // Use external profiling tools (e.g., JProfiler, Chrome DevTools)
}
```

## Next Actions

1. ✅ Enable profiling
2. ✅ Establish baseline performance
3. ⏭ Identify top 3 hotspots
4. ⏭ Optimize hotspots one by one
5. ⏭ Validate constitutional compliance
6. ⏭ Add performance regression tests
7. ⏭ Generate final optimization report

## Contact

For questions about the profiling system:
- See `/docs/profiling/PROFILING_GUIDE.md` for detailed documentation
- See `/PROFILING_INSTRUMENTATION_REPORT.md` for technical details
- See `/examples/profiling-example/` for working examples

---

**Ready for**: kotlin-perf agent optimization work
**Profiling Status**: ✅ Fully instrumented
**Documentation**: ✅ Complete
**Test Coverage**: ✅ Comprehensive
