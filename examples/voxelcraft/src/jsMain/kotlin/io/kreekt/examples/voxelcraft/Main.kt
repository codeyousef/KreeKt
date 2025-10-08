package io.kreekt.examples.voxelcraft

import io.kreekt.camera.PerspectiveCamera
import io.kreekt.renderer.FPSCounter
import io.kreekt.renderer.RendererFactory
import io.kreekt.renderer.RendererInitializationException
import io.kreekt.renderer.webgpu.WebGPUSurface
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

    // Always reset to proper spawn for debugging
    world.player.position.set(0.0f, 100.0f, 0.0f)
    world.player.rotation.set(0.0f, 0.0f, 0.0f)
    world.player.isFlying = true
    Logger.info("📍 Reset player to spawn: (0, 100, 0), flight mode: ON")

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

            window.setTimeout({
                hideLoadingScreen()
                Logger.info("?? Game loop started (terrain generating in background)!")
                Logger.info("?? Controls: WASD=Move, Mouse=Look, F=Flight, Space/Shift=Up/Down")
            }, 100)

            world.generateTerrain { current, total ->
                val percent = (current * 100) / total
                if (percent % 10 == 0) {
                    Logger.info("? Generating terrain... $percent% ($current/$total chunks)")
                }
            }

            if (savedState != null) {
                savedState.applyModifications(world)
                Logger.info("? Applied ${savedState.chunks.size} saved chunk modifications")
            }

            val generationTime = js("Date.now()") as Double - startTime
            updateLoadingProgress("Terrain ready in ${generationTime.toInt()}ms")
            Logger.info("? Terrain generation complete in ${generationTime.toInt()}ms")
            Logger.info("?? Chunks: ${world.chunkCount}")
        } catch (e: Throwable) {
            Logger.error("? Generation failed: ${e.message}", e)
            console.error(e)
        }
    }
}

suspend fun continueInitialization(world: VoxelWorld, canvas: HTMLCanvasElement) {
    val storage = WorldStorage()

    updateLoadingProgress("Initializing renderer...")

    // T027: Create render surface using Feature 019 API
    val surface = WebGPUSurface(canvas)

    // T027: Initialize renderer with RendererFactory (automatic backend detection)
    Logger.info("🔧 Initializing renderer backend for VoxelCraft...")
    Logger.info("📊 Backend Negotiation:")
    Logger.info("  Detecting capabilities...")

    // Detect available backends
    val availableBackends = RendererFactory.detectAvailableBackends()
    Logger.info("  Available backends: ${availableBackends.joinToString(", ")}")

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
    )

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

        // T002: Temporary diagnostic logging
        if (frameCount < 10 || frameCount % 60 == 0) {
            console.log("📷 Frame $frameCount: Camera pos: (${camera.position.x}, ${camera.position.y}, ${camera.position.z})")
            console.log("📷 Frame $frameCount: Camera rot: (${camera.rotation.x}, ${camera.rotation.y}, ${camera.rotation.z})")
            console.log("🧱 Frame $frameCount: Scene children: ${world.scene.children.size}")
            console.log("👤 Frame $frameCount: Player pos: (${world.player.position.x}, ${world.player.position.y}, ${world.player.position.z})")
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
        camera.rotation.set(
            world.player.rotation.x.toFloat(),
            world.player.rotation.y.toFloat(),
            0.0f
        )

        // Update camera matrices
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

    // Start game loop (loading screen hidden by generateTerrainAsync)
    gameLoop()
}

fun updateLoadingProgress(message: String) {
    val progressElement = document.getElementById("loading-progress")
    progressElement?.textContent = message
}

fun hideLoadingScreen() {
    val loading = document.getElementById("loading")
    loading?.setAttribute("class", "loading hidden")

    // Request pointer lock to start gameplay
    val canvas = document.getElementById("kreekt-canvas") as? HTMLCanvasElement
    canvas?.let {
        js("canvas.requestPointerLock = canvas.requestPointerLock || canvas.mozRequestPointerLock || canvas.webkitRequestPointerLock")
        js("if (canvas.requestPointerLock) canvas.requestPointerLock()")
    }
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



