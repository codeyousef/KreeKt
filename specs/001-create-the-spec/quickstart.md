# KreeKt Library Quickstart Guide

**Branch**: `001-create-the-spec` | **Date**: 2025-09-19

## Overview
This quickstart guide demonstrates how to get up and running with the KreeKt library across different platforms. The examples show the core API usage and validate that all major features work as expected.

## Prerequisites

### All Platforms
- Kotlin 1.9.0 or later
- Gradle 8.0 or later

### Platform-Specific Requirements
- **JVM**: JDK 11+ with OpenGL 4.1+ or Vulkan support
- **Web**: Modern browser with WebGPU support (Chrome 113+, Edge 113+)
- **Android**: API level 24+ with Vulkan support
- **iOS**: iOS 14+ with Metal support
- **Native**: Vulkan 1.1+ drivers

## Installation

### Gradle Setup (build.gradle.kts)
```kotlin
plugins {
    kotlin("multiplatform") version "1.9.0"
}

kotlin {
    // Configure your targets
    jvm()
    js(IR) {
        browser()
    }
    androidTarget()

    // Apple targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("kreekt:core:1.0.0")
                implementation("kreekt:renderer:1.0.0")
                implementation("kreekt:scene:1.0.0")
                implementation("kreekt:geometry:1.0.0")
                implementation("kreekt:material:1.0.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("kreekt:renderer-vulkan:1.0.0")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation("kreekt:renderer-webgpu:1.0.0")
            }
        }
    }
}
```

## Basic Example: Spinning Cube

### Common Code (src/commonMain/kotlin/BasicScene.kt)
```kotlin
import kreekt.core.*
import kreekt.scene.*
import kreekt.geometry.*
import kreekt.material.*
import kreekt.renderer.*

/**
 * Basic scene with a spinning cube
 * This example validates:
 * - Scene creation and management
 * - Basic geometry (BoxGeometry)
 * - Standard PBR material
 * - Camera setup and projection
 * - Lighting (DirectionalLight)
 * - Animation loop
 */
class BasicScene {
    private lateinit var renderer: Renderer
    private lateinit var scene: Scene
    private lateinit var camera: PerspectiveCamera
    private lateinit var cube: Mesh

    suspend fun initialize(surface: RenderSurface): Boolean {
        return try {
            // Initialize renderer
            renderer = createRenderer()
            val result = renderer.initialize(surface)
            if (result is RendererResult.Error) {
                println("Renderer initialization failed: ${result.exception.message}")
                return false
            }

            // Create scene using DSL
            scene = scene {
                // Add cube mesh
                mesh {
                    boxGeometry(width = 2f, height = 2f, depth = 2f)
                    standardMaterial {
                        color = Color.BLUE
                        metalness = 0.1f
                        roughness = 0.3f
                    }
                    position.set(0f, 0f, 0f)
                    castShadow = true
                }.also { cube = it }

                // Add lighting
                directionalLight {
                    color(Color.WHITE)
                    intensity(1f)
                    position(5f, 5f, 5f)
                    castShadow(true)
                }

                // Add ambient light
                ambientLight {
                    color(Color(0.404f, 0.404f, 0.404f))
                    intensity(0.6f)
                }
            }

            // Setup camera
            camera = PerspectiveCamera(
                fov = 75f,
                aspect = 16f / 9f,
                near = 0.1f,
                far = 1000f
            ).apply {
                position.set(0f, 0f, 5f)
            }

            // Configure renderer
            renderer.setSize(800, 600)
            renderer.setPixelRatio(1f)

            true
        } catch (e: Exception) {
            println("Scene initialization failed: ${e.message}")
            false
        }
    }

    suspend fun render(deltaTime: Float) {
        // Animate cube rotation
        cube.rotation.x += deltaTime
        cube.rotation.y += deltaTime * 0.5f

        // Render scene
        when (val result = renderer.render(scene, camera)) {
            is RendererResult.Error -> {
                println("Render failed: ${result.exception.message}")
            }
            is RendererResult.Success -> {
                // Rendering successful
            }
        }
    }

    fun setSize(width: Int, height: Int) {
        camera.aspect = width.toFloat() / height.toFloat()
        camera.updateProjectionMatrix()
        renderer.setSize(width, height)
    }

    fun dispose() {
        renderer.dispose()
    }

    // Get rendering statistics for validation
    fun getStats(): RendererInfo = renderer.info
}
```

### Platform-Specific Entry Points

#### JVM Application (src/jvmMain/kotlin/JvmApp.kt)
```kotlin
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class JvmApp {
    suspend fun run() {
        val surface = createJvmSurface(800, 600, "KreeKt Demo")
        val basicScene = BasicScene()

        if (!basicScene.initialize(surface)) {
            println("Failed to initialize scene")
            return
        }

        var lastTime = System.currentTimeMillis()

        // Main render loop
        while (!surface.shouldClose()) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastTime) / 1000f
            lastTime = currentTime

            // Process input events
            surface.pollEvents()

            // Render frame
            basicScene.render(deltaTime)

            // Swap buffers
            surface.swapBuffers()

            // Limit to ~60 FPS
            delay(16.milliseconds)
        }

        basicScene.dispose()
        surface.dispose()
    }
}

fun main() = runBlocking {
    JvmApp().run()
}
```

#### Web Application (src/jsMain/kotlin/WebApp.kt)
```kotlin
import kotlinx.browser.*
import kotlinx.coroutines.*
import org.w3c.dom.*

class WebApp {
    private var animationId: Int? = null
    private lateinit var basicScene: BasicScene
    private var lastTime = 0.0

    suspend fun start() {
        val canvas = document.getElementById("canvas") as HTMLCanvasElement
        val surface = createWebSurface(canvas)

        basicScene = BasicScene()

        if (!basicScene.initialize(surface)) {
            console.log("Failed to initialize scene")
            return
        }

        // Handle window resize
        window.addEventListener("resize", {
            val width = window.innerWidth
            val height = window.innerHeight
            canvas.width = width
            canvas.height = height
            basicScene.setSize(width, height)
        })

        // Start render loop
        startRenderLoop()
    }

    private fun startRenderLoop() {
        fun render(time: Double) {
            val deltaTime = ((time - lastTime) / 1000.0).toFloat()
            lastTime = time

            GlobalScope.launch {
                basicScene.render(deltaTime)

                // Continue loop
                animationId = window.requestAnimationFrame(::render)
            }
        }

        // Start the loop
        animationId = window.requestAnimationFrame(::render)
    }

    fun stop() {
        animationId?.let { window.cancelAnimationFrame(it) }
        basicScene.dispose()
    }
}

fun main() {
    GlobalScope.launch {
        WebApp().start()
    }
}
```

#### Android Activity (src/androidMain/kotlin/AndroidActivity.kt)
```kotlin
import android.app.Activity
import android.os.Bundle
import android.view.SurfaceView
import kotlinx.coroutines.*

class MainActivity : Activity() {
    private lateinit var surfaceView: SurfaceView
    private lateinit var basicScene: BasicScene
    private var renderJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        // Initialize scene when surface is ready
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                renderJob = GlobalScope.launch {
                    val surface = createAndroidSurface(holder.surface)
                    basicScene = BasicScene()

                    if (basicScene.initialize(surface)) {
                        startRenderLoop()
                    }
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                basicScene.setSize(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                renderJob?.cancel()
                basicScene.dispose()
            }
        })
    }

    private suspend fun startRenderLoop() {
        var lastTime = System.nanoTime()

        while (isActive) {
            val currentTime = System.nanoTime()
            val deltaTime = (currentTime - lastTime) / 1_000_000_000f
            lastTime = currentTime

            basicScene.render(deltaTime)

            // Target 60 FPS
            delay(16)
        }
    }
}
```

## Validation Tests

### Performance Test
```kotlin
class PerformanceTest {
    @Test
    fun testRenderingPerformance() = runBlocking {
        val surface = createTestSurface(1920, 1080)
        val scene = BasicScene()

        scene.initialize(surface)

        // Measure 100 frames
        val startTime = System.currentTimeMillis()
        repeat(100) {
            scene.render(0.016f) // 60 FPS delta
        }
        val endTime = System.currentTimeMillis()

        val avgFrameTime = (endTime - startTime) / 100f

        // Should maintain 60 FPS (16.67ms per frame)
        assertTrue("Frame time too high: ${avgFrameTime}ms", avgFrameTime < 20f)

        // Check triangle count
        val stats = scene.getStats()
        assertTrue("Not enough triangles rendered", stats.render.triangles > 10)

        scene.dispose()
    }
}
```

### Cross-Platform Consistency Test
```kotlin
class ConsistencyTest {
    @Test
    fun testRenderingConsistency() = runBlocking {
        val platforms = listOf("webgpu", "vulkan", "metal")
        val results = mutableMapOf<String, RendererInfo>()

        platforms.forEach { platform ->
            val surface = createPlatformSurface(platform, 800, 600)
            val scene = BasicScene()

            scene.initialize(surface)
            scene.render(0.016f)

            results[platform] = scene.getStats()
            scene.dispose()
        }

        // All platforms should render same triangle count
        val triangleCounts = results.values.map { it.render.triangles }.toSet()
        assertEquals("Triangle counts differ across platforms", 1, triangleCounts.size)
    }
}
```

### Three.js Compatibility Test
```kotlin
class CompatibilityTest {
    @Test
    fun testThreeJsApiCompatibility() {
        // Test scene creation matches Three.js patterns
        val scene = scene {
            mesh {
                boxGeometry(1f, 1f, 1f)
                standardMaterial {
                    color = Color.RED
                    metalness = 0.5f
                    roughness = 0.5f
                }
            }
        }

        // Should have same structure as Three.js scene
        assertEquals(1, scene.children.size)
        assertTrue(scene.children[0] is Mesh)

        val mesh = scene.children[0] as Mesh
        assertTrue(mesh.geometry is BoxGeometry)
        assertTrue(mesh.material is MeshStandardMaterial)
    }
}
```

## Expected Output

### Success Criteria
1. **Scene renders successfully** on all platforms
2. **Cube is visible** and rotating smoothly
3. **Frame rate maintains** 60 FPS on modern hardware
4. **Lighting appears correct** with shadows
5. **Resize handling** works properly
6. **Memory usage** remains stable over time

### Platform-Specific Validation
- **Web**: Check WebGPU/WebGL2 fallback works
- **JVM**: Verify Vulkan initialization succeeds
- **Android**: Confirm surface lifecycle handling
- **iOS**: Test Metal renderer integration

### Debug Information
```kotlin
// Print renderer capabilities
println("Renderer: ${renderer.capabilities}")
println("Max texture size: ${renderer.capabilities.maxTextureSize}")
println("Supports instancing: ${renderer.capabilities.supportsInstancedArrays}")

// Print rendering stats each frame
val stats = scene.getStats()
println("Triangles: ${stats.render.triangles}, Calls: ${stats.render.calls}")
println("Geometries: ${stats.memory.geometries}, Textures: ${stats.memory.textures}")
```

## Troubleshooting

### Common Issues
1. **WebGPU not supported**: Falls back to WebGL2 automatically
2. **Vulkan initialization fails**: Check driver updates
3. **Black screen**: Verify camera position and lighting
4. **Low performance**: Check GPU capabilities and reduce geometry complexity

### Platform-Specific Issues
- **Web**: Enable experimental web platform features in browser
- **JVM**: Ensure graphics drivers are up to date
- **Android**: Verify Vulkan support in device specifications
- **iOS**: Check Metal feature set compatibility

---
*This quickstart validates all core library functionality and serves as the foundation for more complex applications*