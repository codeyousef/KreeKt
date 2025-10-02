package io.kreekt.examples.profiling

import io.kreekt.core.math.Vector3
import io.kreekt.core.scene.Scene
import io.kreekt.core.scene.Mesh
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.geometry.primitives.BoxGeometry
import io.kreekt.geometry.primitives.SphereGeometry
import io.kreekt.material.MeshBasicMaterial
import io.kreekt.renderer.*
import io.kreekt.profiling.*

/**
 * Comprehensive profiling example demonstrating:
 * - Renderer profiling
 * - Scene graph profiling
 * - Geometry profiling
 * - Performance dashboard
 * - Report generation
 */
class ProfilingExample {

    private val dashboard = ProfilingDashboard()
    private lateinit var scene: Scene
    private lateinit var camera: PerspectiveCamera
    private lateinit var renderer: Renderer

    fun run() {
        println("KreeKt Profiling Example")
        println("=" .repeat(80))
        println()

        // 1. Enable profiling
        setupProfiling()

        // 2. Create and analyze scene
        setupScene()
        analyzeScene()

        // 3. Run profiled rendering loop
        runProfiledRenderLoop()

        // 4. Generate and display reports
        generateReports()

        // 5. Show final dashboard
        showFinalDashboard()
    }

    private fun setupProfiling() {
        println("1. Setting up profiling...")
        println("-".repeat(80))

        // Enable profiling with detailed tracking
        ProfilingHelpers.enableDevelopmentProfiling()

        // Enable dashboard
        dashboard.enable(DashboardConfig(
            showHotspots = true,
            showMemory = true,
            showRecommendations = true,
            verbosity = ProfileVerbosity.DETAILED
        ))

        println("✓ Profiling enabled")
        println("✓ Dashboard enabled")
        println()
    }

    private fun setupScene() {
        println("2. Creating scene...")
        println("-".repeat(80))

        scene = Scene()
        camera = PerspectiveCamera(
            fov = 75f,
            aspect = 16f / 9f,
            near = 0.1f,
            far = 1000f
        )
        camera.position.set(0f, 0f, 5f)

        // Profile scene creation
        val session = ProfilingHelpers.createSession("SceneCreation")

        // Add various objects with profiling
        repeat(50) { i ->
            GeometryProfiler.profilePrimitiveGeneration("Box") {
                val geometry = BoxGeometry(1f, 1f, 1f)
                val material = MeshBasicMaterial()
                val mesh = Mesh(geometry, material)

                mesh.position.set(
                    (i % 10 - 5).toFloat(),
                    ((i / 10) - 2).toFloat(),
                    0f
                )

                scene.add(mesh)
            }
        }

        // Add some spheres
        repeat(10) { i ->
            GeometryProfiler.profilePrimitiveGeneration("Sphere") {
                val geometry = SphereGeometry(0.5f, 16, 16)
                val material = MeshBasicMaterial()
                val mesh = Mesh(geometry, material)

                mesh.position.set(
                    (i - 5).toFloat(),
                    3f,
                    0f
                )

                scene.add(mesh)
            }
        }

        val summary = session.end()
        summary.printSummary()
    }

    private fun analyzeScene() {
        println("3. Analyzing scene complexity...")
        println("-".repeat(80))

        // Analyze scene complexity
        val sceneComplexity = scene.getComplexity()
        println("Scene Statistics:")
        println("  Total nodes: ${sceneComplexity.totalNodes}")
        println("  Max depth: ${sceneComplexity.maxDepth}")
        println("  Avg children per node: ${String.format("%.2f", sceneComplexity.averageChildrenPerNode)}")
        println("  Complexity score: ${String.format("%.2f", sceneComplexity.getComplexityScore())}")
        println("  Is complex: ${sceneComplexity.isComplex()}")
        println()

        // Analyze geometry for first mesh
        scene.traverse { obj ->
            if (obj is Mesh) {
                val geomComplexity = obj.geometry.analyzeComplexity()
                println("Sample Geometry Analysis:")
                println("  Vertices: ${geomComplexity.vertexCount}")
                println("  Triangles: ${geomComplexity.triangleCount}")
                println("  Attributes: ${geomComplexity.attributeCount}")
                println("  Memory: ${String.format("%.2f", geomComplexity.getMemoryUsageMB())}MB")
                println("  Mobile friendly: ${geomComplexity.isMobileFriendly()}")

                val recommendations = geomComplexity.getRecommendations()
                if (recommendations.isNotEmpty()) {
                    println("  Recommendations:")
                    recommendations.forEach { println("    - $it") }
                }
                println()

                return@traverse // Only analyze first mesh
            }
        }
    }

    private fun runProfiledRenderLoop() {
        println("4. Running profiled render loop...")
        println("-".repeat(80))

        // Create renderer (in real app, this would be platform-specific)
        // For this example, we'll use a mock renderer
        renderer = createMockRenderer()

        // Wrap with profiling
        val profiledRenderer = renderer.withProfiling()

        // Run render loop
        val frameCount = 120 // 2 seconds at 60 FPS
        repeat(frameCount) { frame ->
            // Simulate game loop timing
            val deltaTime = 1f / 60f

            ProfilingHelpers.profileGameLoop(
                deltaTime = deltaTime,
                updatePhase = {
                    // Update scene
                    updateScene(deltaTime, frame)
                },
                renderPhase = {
                    // Render scene
                    profiledRenderer.render(scene, camera)
                }
            )

            // Show dashboard every 30 frames (0.5 seconds)
            if (frame > 0 && frame % 30 == 0) {
                println("\nFrame $frame:")
                showDashboardSnapshot()
            }
        }

        println("\n✓ Render loop complete")
        println()
    }

    private fun updateScene(deltaTime: Float, frame: Int) {
        // Profile scene updates
        SceneProfiler.profileTraversal(scene) { obj ->
            if (obj is Mesh) {
                // Rotate mesh
                obj.rotation.y += deltaTime
                obj.rotation.x += deltaTime * 0.5f

                // Update matrix
                obj.updateMatrixProfiled()
            }
        }
    }

    private fun generateReports() {
        println("5. Generating performance reports...")
        println("-".repeat(80))

        // Generate text report
        val textReport = ProfilingReport.generateTextReport()
        println(textReport)

        // In a real application, you would save these to files
        println("✓ Reports generated (HTML, JSON, CSV, Chrome trace)")
        println()

        // Show example of Chrome trace export
        println("Chrome Trace Format (first 500 chars):")
        val trace = PerformanceProfiler.export(ExportFormat.CHROME_TRACE)
        println(trace.take(500) + "...")
        println()
    }

    private fun showFinalDashboard() {
        println("6. Final Performance Summary")
        println("=" .repeat(80))

        val state = dashboard.getCurrentState()

        println("\nFrame Statistics:")
        println("  Average FPS: ${String.format("%.2f", state.frameStats.averageFps)}")
        println("  Average frame time: ${state.frameStats.averageFrameTime / 1_000_000}ms")
        println("  Min frame time: ${state.frameStats.minFrameTime / 1_000_000}ms")
        println("  Max frame time: ${state.frameStats.maxFrameTime / 1_000_000}ms")
        println("  95th percentile: ${state.frameStats.percentile95 / 1_000_000}ms")
        println("  99th percentile: ${state.frameStats.percentile99 / 1_000_000}ms")
        println("  Dropped frames: ${state.frameStats.droppedFrames}")

        val meetsTarget = state.frameStats.meetsTargetFps(60)
        println("  Meets 60 FPS target: ${if (meetsTarget) "✓ YES" else "✗ NO"}")

        println("\nPerformance Grade: ${dashboard.getPerformanceGrade()}")

        if (state.hotspots.isNotEmpty()) {
            println("\nTop Hotspots:")
            state.hotspots.take(5).forEach { hotspot ->
                println("  • ${hotspot.name}: ${String.format("%.1f", hotspot.percentage)}% " +
                        "(${hotspot.callCount} calls, avg ${hotspot.averageTime / 1_000_000}ms)")
            }
        }

        if (state.recommendations.isNotEmpty()) {
            println("\nRecommendations:")
            state.recommendations.forEach { rec ->
                println("  [${rec.severity}] ${rec.message}")
                println("    → ${rec.suggestion}")
            }
        }

        println("\n" + "=" .repeat(80))
        println("Profiling example complete!")
    }

    private fun showDashboardSnapshot() {
        val stats = PerformanceProfiler.getFrameStats()
        println("  FPS: ${String.format("%.1f", stats.averageFps)}, " +
                "Frame time: ${stats.averageFrameTime / 1_000_000}ms")
    }

    // Mock renderer for example purposes
    private fun createMockRenderer(): Renderer {
        // In a real application, this would be createRenderer()
        // For this example, we use DefaultRenderer
        return DefaultRenderer(RendererConfig())
    }
}

/**
 * Entry point for the profiling example
 */
fun main() {
    val example = ProfilingExample()
    example.run()
}
