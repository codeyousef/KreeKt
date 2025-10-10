package io.kreekt.examples.voxelcraft

import io.kreekt.camera.PerspectiveCamera
import io.kreekt.renderer.FPSCounter
import io.kreekt.renderer.RendererFactory
import io.kreekt.renderer.RendererInitializationException
import io.kreekt.renderer.SurfaceFactory
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import org.w3c.dom.HTMLCanvasElement

private val gameScope = MainScope()
private var initJob: Job? = null

/**
 * VoxelCraft entry point
 *
 * Demonstrates:
 * - World generation with Simplex noise (1,024 chunks)
 * - Player controls (WASD movement, mouse camera)
 * - Flight mode (F key toggle)
 * - WebGPU rendering with KreeKt (Feature 019/020)
 * - Persistence (save/load to localStorage)
 * - Game loop with delta time
 */

fun main() {
    console.log("🎮 VoxelCraft Starting...")

    window.addEventListener("load", {
        console.log("📦 Page loaded, waiting for user to click Start...")

        // Expose startGameFromButton to JavaScript using window.asDynamic()
        window.asDynamic().startGameFromButton = ::startGameFromButton
    })

    window.addEventListener("beforeunload", {
        gameScope.cancel()
    })
}

fun startGameFromButton() {
    console.log("🎮 Starting game from button click...")
    initJob?.cancel()
    initJob = gameScope.launch {
        initGame()
    }
}

suspend fun initGame() = coroutineScope {
    Logger.info("🌍 Initializing VoxelCraft...")

    val canvas = document.getElementById("kreekt-canvas") as? HTMLCanvasElement
    if (canvas == null) {
        Logger.error("❌ Canvas element not found!")
        return@coroutineScope
    }

    canvas.width = 800
    canvas.height = 600

    val storage = WorldStorage()
    val savedState = storage.load()
    val startTime = js("Date.now()") as Double

    val world = savedState?.restore(this) ?: VoxelWorld(seed = 12345L, parentScope = this)

    // T016: Fix camera/player position - ensure player is at proper spawn height
    Logger.info("🔍 Player position after load: (${world.player.position.x}, ${world.player.position.y}, ${world.player.position.z})")
    Logger.info("🔍 Player rotation: (${world.player.rotation.x}, ${world.player.rotation.y}, ${world.player.rotation.z})")
    Logger.info("🔍 Flight mode: ${world.player.isFlying}")

    // T021: Move camera OUTSIDE and ABOVE terrain to see it clearly
    // Position at (8, 150, 8) - center of first chunk but much higher up
    world.player.position.set(8.0f, 150.0f, 8.0f)
    // Look down and forward: pitch down more (-0.5 ≈ -28°), no yaw
    world.player.rotation.set(-0.5f, 0.0f, 0.0f)
    world.player.isFlying = true
    Logger.info("📍 T021: Reset player to high spawn: (8, 150, 8), rotation: (-0.5, 0, 0), flight mode: ON")
    Logger.info("📍 T021: Camera looking DOWN at terrain from above")
    Logger.info("📍 T021: Player rotation (radians): x=${world.player.rotation.x}, y=${world.player.rotation.y}, z=${world.player.rotation.z}")

    generateTerrainAsync(
        scope = this,
        world = world,
        startTime = startTime,
        canvas = canvas,
        savedState = savedState
    )
}


fun generateTerrainAsync(
    scope: CoroutineScope,
    world: VoxelWorld,
    startTime: Double,
    canvas: HTMLCanvasElement,
    savedState: WorldState?
) {
    Logger.info("?? Generating world...")
    updateLoadingProgress("Starting terrain generation...")

    scope.launch {
        continueInitialization(world, canvas)
    }

    scope.launch {
        try {
            Logger.info("?? About to generate ${ChunkPosition.TOTAL_CHUNKS} chunks...")

            // T021: Generate terrain data first (Phase 1)
            world.generateTerrain { current, total ->
                val percent = (current * 100) / total
                if (percent % 10 == 0) {
                    Logger.info("? Generating terrain... $percent% ($current/$total chunks)")
                    updateLoadingProgress("Generating terrain: $percent% ($current/$total chunks)")
                }
            }

            if (savedState != null) {
                savedState.applyModifications(world)
                Logger.info("? Applied ${savedState.chunks.size} saved chunk modifications")
            }

            val generationTime = js("Date.now()") as Double - startTime
            Logger.info("? Terrain generation complete in ${generationTime.toInt()}ms")
            updateLoadingProgress("Terrain ready, generating meshes...")
            
            // T021: Phase 2 - Wait for initial mesh generation to complete
            val initialChunkCount = 81  // 9×9 grid (INITIAL_GENERATION_RADIUS=4)
            world.setInitialMeshTarget(initialChunkCount)
            
            // Poll mesh generation progress
            var lastPercent = 0
            Logger.info("?? Waiting for $initialChunkCount initial meshes to generate...")
            while (!world.isInitialMeshGenerationComplete) {
                delay(100)  // Check every 100ms
                val progress = world.initialMeshGenerationProgress
                val percent = (progress * 100).toInt()
                if (percent > lastPercent && percent % 5 == 0) {
                    val meshCount = (initialChunkCount * progress).toInt()
                    Logger.info("? Generating meshes... $percent% ($meshCount/$initialChunkCount)")
                    updateLoadingProgress("Generating meshes: $meshCount/$initialChunkCount ($percent%)")
                    lastPercent = percent
                }
            }
            
            // T021: Phase 3 - All meshes ready, wait for user click to start
            val totalTime = js("Date.now()") as Double - startTime
            Logger.info("? All $initialChunkCount initial meshes generated in ${totalTime.toInt()}ms total")
            updateLoadingProgress("World ready!")  // Shows "Click on canvas to start"
            setupStartOnClick(world)  // Wait for click, then hide loading screen
            Logger.info("?? World ready with ${world.scene.children.size} meshes!")
            Logger.info("?? Controls: WASD=Move, Mouse=Look, F=Flight, Space/Shift=Up/Down")
        } catch (e: Throwable) {
            Logger.error("? Generation failed: ${e.message}", e)
            console.error(e)
        }
    }
}

suspend fun continueInitialization(world: VoxelWorld, canvas: HTMLCanvasElement) {
    val storage = WorldStorage()

    updateLoadingProgress("Initializing renderer...")

    // Create render surface using platform-agnostic SurfaceFactory
    val surface = SurfaceFactory.create(canvas)

    // Initialize renderer with RendererFactory (automatic backend detection)
    Logger.info("🔧 Initializing renderer backend for VoxelCraft...")
    Logger.info("📊 Backend Negotiation:")
    Logger.info("  Detecting capabilities...")

    // Detect available backends
    val availableBackends = RendererFactory.detectAvailableBackends()
    Logger.info("  Available backends: ${availableBackends.joinToString(", ")}")

    // Check if WebGPU is available
    if (!availableBackends.contains(io.kreekt.renderer.BackendType.WEBGPU)) {
        val errorMsg = """
            ❌ WebGPU Not Available

            VoxelCraft requires WebGPU support.

            WebGPU is available in:
            • Chrome/Edge 113+ (enabled by default)
            • Firefox Nightly (enable dom.webgpu.enabled in about:config)
            • Safari Technology Preview

            Your browser detected: ${availableBackends.joinToString(", ")}

            Please update your browser or enable WebGPU.
        """.trimIndent()

        Logger.error(errorMsg)
        updateLoadingProgress(errorMsg.replace("\n", "<br>"))
        throw RuntimeException("WebGPU not available. Please use a WebGPU-enabled browser.")
    }

    // Create renderer with automatic backend selection (WebGPU → WebGL fallback)
    val renderer = try {
        when (val result = RendererFactory.create(surface)) {
            is io.kreekt.core.Result.Success -> result.value
            is io.kreekt.core.Result.Error -> {
                val exception = result.exception as? RendererInitializationException
                when (exception) {
                    is RendererInitializationException.NoGraphicsSupportException -> {
                        Logger.error("❌ Graphics not supported: ${result.message}")
                        Logger.error("   Platform: ${exception.platform}")
                        Logger.error("   Available: ${exception.availableBackends}")
                        Logger.error("   Required: ${exception.requiredFeatures}")
                        throw exception
                    }

                    else -> {
                        Logger.error("❌ Renderer initialization failed: ${result.message}")
                        throw exception ?: RuntimeException(result.message)
                    }
                }
            }
        }
    } catch (e: Throwable) {
        Logger.error("❌ Failed to create renderer: ${e.message}")
        updateLoadingProgress("Error: ${e.message}")
        throw e
    }

    // Log selected backend
    Logger.info("✅ Renderer initialized!")
    Logger.info("  Backend: ${renderer.backend}")
    Logger.info("  Device: ${renderer.capabilities.deviceName}")
    Logger.info("  Features:")
    Logger.info("    COMPUTE: ${renderer.capabilities.supportsCompute}")
    Logger.info("    RAY_TRACING: ${renderer.capabilities.supportsRayTracing}")
    Logger.info("    MSAA: ${renderer.capabilities.supportsMultisampling} (max ${renderer.capabilities.maxSamples}x)")

    // Create camera
    val camera = PerspectiveCamera(
        fov = 75.0f,
        aspect = canvas.width.toFloat() / canvas.height.toFloat(),
        near = 0.1f,
        far = 1000.0f
    ).apply {
        name = "MainCamera"  // T021: Add name for logging
    }

    updateLoadingProgress("World ready!")

    // Initialize block interaction
    val blockInteraction = BlockInteraction(world, world.player)

    // Initialize controllers
    val playerController = PlayerController(world.player, blockInteraction)
    val cameraController = CameraController(world.player, canvas)

    // Auto-save every 30 seconds
    window.setInterval({
        val result = storage.save(world)
        if (!result.success) {
            Logger.warn("⚠️ Auto-save failed: ${result.error}")
        }
    }, 30000)

    // Save on page close
    window.addEventListener("beforeunload", {
        storage.save(world)
    })

    // T014: FPS counter with rolling average
    val fpsCounter = FPSCounter(windowSize = 60)
    var lastTime = js("Date.now()") as Double
    var frameCount = 0

    fun gameLoop() {
        val currentTime = js("performance.now()") as Double
        val deltaTime = ((currentTime - lastTime) / 1000.0).toFloat()
        lastTime = currentTime

        // T002: Temporary diagnostic logging - MORE FREQUENT to catch mesh additions
        if (frameCount < 100 || frameCount % 60 == 0) {
            console.log("📷 Frame $frameCount: Camera pos: (${camera.position.x}, ${camera.position.y}, ${camera.position.z})")
            console.log("🧱 Frame $frameCount: Scene children: ${world.scene.children.size}")
            if (frameCount % 10 == 0) {
                console.log("👤 Frame $frameCount: Player pos: (${world.player.position.x}, ${world.player.position.y}, ${world.player.position.z})")
            }
        }

        // Update controllers
        playerController.update(deltaTime)

        // Update world
        world.update(deltaTime)

        // Sync camera with player
        camera.position.set(
            world.player.position.x.toFloat(),
            world.player.position.y.toFloat(),
            world.player.position.z.toFloat()
        )

        // Set rotation - Player has Vector3, Camera has Euler
        // IMPORTANT: Must use set() method to properly update Euler
        camera.rotation.set(
            world.player.rotation.x,
            world.player.rotation.y,
            world.player.rotation.z
        )

        // Manually update quaternion from Euler angles since onChange callbacks aren't implemented
        camera.quaternion.setFromEuler(camera.rotation)

        // Manually update matrices since onChange callbacks aren't implemented
        camera.updateMatrix()  // Updates local matrix from position/quaternion/scale
        camera.updateMatrixWorld(true)

        camera.updateProjectionMatrix()

        // Render scene
        renderer.render(world.scene, camera)

        // Update HUD (every frame)
        updateHUD(world)

        // T014: Update FPS counter with rolling average (every frame)
        val fps = fpsCounter.update(currentTime)
        val stats = renderer.stats
        updateFPS(fps.toInt(), stats.triangles, stats.drawCalls)

        // T020: Performance validation after warmup (frame 120 = ~2 seconds)
        val validationResult = PerformanceValidator.validateAfterWarmup(
            frameCount = frameCount,
            metrics = PerformanceValidator.PerformanceMetrics(
                fps = fps,
                drawCalls = stats.drawCalls,
                triangles = stats.triangles,
                backendType = renderer.backend.name,
                frameTime = fpsCounter.getAverageFrameTime()
            )
        )
        validationResult?.let { result ->
            PerformanceValidator.logResult(result)
        }

        frameCount++

        // Request next frame
        window.requestAnimationFrame { gameLoop() }
    }

    // T021: Sync camera with player BEFORE starting game loop to fix initial rotation
    camera.position.set(
        world.player.position.x.toFloat(),
        world.player.position.y.toFloat(),
        world.player.position.z.toFloat()
    )
    camera.rotation.set(
        world.player.rotation.x,
        world.player.rotation.y,
        world.player.rotation.z
    )
    camera.quaternion.setFromEuler(camera.rotation)
    camera.updateMatrix()
    camera.updateMatrixWorld(true)

    // Start game loop (loading screen hidden by generateTerrainAsync)
    gameLoop()
}

fun updateLoadingProgress(message: String) {
    val progressElement = document.getElementById("loading-progress")
    if (message == "World ready!") {
        // T021: Show click instruction after loading completes
        progressElement?.innerHTML = "$message<br><br>🖱️ <strong>Click on the canvas to start playing!</strong>"
    } else {
        progressElement?.textContent = message
    }
}

/**
 * T021: Setup click handler to start the game.
 * Loading screen stays visible until user clicks, ensuring pointer lock request
 * happens with a valid user gesture.
 */
fun setupStartOnClick(world: VoxelWorld) {
    val loading = document.getElementById("loading")
    val canvas = document.getElementById("kreekt-canvas") as? HTMLCanvasElement
    
    // Make loading screen clickable
    loading?.addEventListener("click", {
        // Hide loading screen
        loading.setAttribute("class", "loading hidden")
        
        // Request pointer lock (requires user gesture)
        canvas?.let { c ->
            js("c.requestPointerLock = c.requestPointerLock || c.mozRequestPointerLock || c.webkitRequestPointerLock")
            js("if (c.requestPointerLock) c.requestPointerLock()")
        }
        
        console.log("🎮 Game started with ${world.scene.children.size} meshes!")
    })
    
    // Also allow clicking directly on canvas
    canvas?.addEventListener("click", {
        // Hide loading screen if still visible
        loading?.setAttribute("class", "loading hidden")
        
        // Request pointer lock
        js("canvas.requestPointerLock = canvas.requestPointerLock || canvas.mozRequestPointerLock || canvas.webkitRequestPointerLock")
        js("if (canvas.requestPointerLock) canvas.requestPointerLock()")
    })
    
    console.log("💡 Click anywhere to start playing!")
}

fun hideLoadingScreen() {
    val loading = document.getElementById("loading")
    loading?.setAttribute("class", "loading hidden")
}

fun updateHUD(world: VoxelWorld) {
    // Update position
    val posElement = document.getElementById("position")
    val pos = world.player.position
    posElement?.textContent = "X: ${pos.x.toInt()}, Y: ${pos.y.toInt()}, Z: ${pos.z.toInt()}"

    // Update flight status
    val flightElement = document.getElementById("flight-status")
    flightElement?.textContent = if (world.player.isFlying) "ON" else "OFF"

    // Update chunks
    val chunksElement = document.getElementById("chunks")
    chunksElement?.textContent = "${world.chunkCount} / 1024"
}

fun updateFPS(fps: Int, triangles: Int = 0, drawCalls: Int = 0) {
    val fpsElement = document.getElementById("fps")
    fpsElement?.textContent = "$fps FPS | ${triangles}▲ | ${drawCalls}DC"
}

/**
 * VoxelCraft game facade - Simplified for vertical slice
 */
class VoxelCraft(val seed: Long, private val scope: CoroutineScope = gameScope) {
    val world = VoxelWorld(seed, scope)

    fun update(deltaTime: Float) {
        world.update(deltaTime)
    }

    fun dispose() {
        world.dispose()
    }

    companion object {
        fun fromSavedState(state: Any, scope: CoroutineScope = gameScope): VoxelCraft {
            val worldState = state as WorldState
            return VoxelCraft(worldState.seed, scope)
        }
    }
}



