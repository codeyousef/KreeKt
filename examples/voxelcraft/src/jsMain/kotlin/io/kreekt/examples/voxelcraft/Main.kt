package io.kreekt.examples.voxelcraft

import io.kreekt.camera.PerspectiveCamera
import io.kreekt.renderer.RenderSurface
import io.kreekt.renderer.RendererConfig
import io.kreekt.renderer.webgl.WebGLRenderer
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement

/**
 * VoxelCraft entry point
 *
 * Demonstrates:
 * - World generation with Simplex noise (1,024 chunks)
 * - Player controls (WASD movement, mouse camera)
 * - Flight mode (F key toggle)
 * - WebGL2 rendering with KreeKt
 * - Persistence (save/load to localStorage)
 * - Game loop with delta time
 */

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    console.log("🎮 VoxelCraft Starting...")

    window.addEventListener("load", {
        console.log("📦 Page loaded, waiting for user to click Start...")

        // Expose startGameFromButton to JavaScript using window.asDynamic()
        window.asDynamic().startGameFromButton = ::startGameFromButton
    })
}

@OptIn(DelicateCoroutinesApi::class)
fun startGameFromButton() {
    console.log("🎮 Starting game from button click...")
    GlobalScope.launch {
        initGame()
    }
}

suspend fun initGame() {
    Logger.info("🌍 Initializing VoxelCraft...")

    // Get canvas element
    val canvas = document.getElementById("kreekt-canvas") as? HTMLCanvasElement
    if (canvas == null) {
        Logger.error("❌ Canvas element not found!")
        return
    }

    // Setup canvas size
    canvas.width = 800
    canvas.height = 600

    // Try to load saved world first
    val storage = WorldStorage()
    val savedState = storage.load()

    // Create world (without terrain generation yet)
    val world = VoxelWorld(seed = 12345L)

    // Generate terrain with progress tracking
    val startTime = js("Date.now()") as Double

    if (savedState != null) {
        updateLoadingProgress("Loading saved world...")
        Logger.info("📂 Restoring from save...")

        // Restore world state
        val restoredWorld = savedState.restore()

        // Generate terrain first (all chunks need to exist)
        generateTerrainAsync(restoredWorld, startTime, canvas, savedState)
    } else {
        generateTerrainAsync(world, startTime, canvas, null)
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun generateTerrainAsync(world: VoxelWorld, startTime: Double, canvas: HTMLCanvasElement, savedState: WorldState?) {
    Logger.info("🌍 Generating world...")
    updateLoadingProgress("Starting terrain generation...")

    // Start game loop immediately in a coroutine, terrain generates in background
    GlobalScope.launch {
        continueInitialization(world, 0.0, canvas)
    }

    // Launch coroutine for async terrain generation with progress updates
    GlobalScope.launch {
        try {
            Logger.info("📊 About to generate ${ChunkPosition.TOTAL_CHUNKS} chunks...")

            // Hide loading screen and start game immediately
            window.setTimeout({
                hideLoadingScreen()
                Logger.info("🚀 Game loop started (terrain generating in background)!")
                Logger.info("🎮 Controls: WASD=Move, Mouse=Look, F=Flight, Space/Shift=Up/Down")
            }, 100)

            world.generateTerrain { current, total ->
                val percent = (current * 100) / total
                // Only log every 10% to reduce spam
                if (percent % 10 == 0) {
                    Logger.info("⏳ Generating terrain... $percent% ($current/$total chunks)")
                }
            }

            // Apply saved modifications if loading from save
            if (savedState != null) {
                savedState.applyModifications(world)
                Logger.info("✅ Applied ${savedState.chunks.size} saved chunk modifications")
            }

            val generationTime = js("Date.now()") as Double - startTime
            Logger.info("✅ Terrain generation complete in ${generationTime.toInt()}ms")
            Logger.info("📊 Chunks: ${world.chunkCount}")
        } catch (e: Throwable) {
            Logger.error("❌ Generation failed: ${e.message}")
            console.error(e)
        }
    }
}

suspend fun continueInitialization(world: VoxelWorld, generationTime: Double, canvas: HTMLCanvasElement) {
    val storage = WorldStorage()

    updateLoadingProgress("Initializing renderer...")

    // Create WebGL renderer
    val renderer = WebGLRenderer(canvas, RendererConfig(antialias = true))
    val surface = RenderSurface(canvas)

    val initResult = renderer.initialize(surface)
    if (initResult !is io.kreekt.renderer.RendererResult.Success) {
        Logger.error("❌ Renderer initialization failed!")
        updateLoadingProgress("Renderer initialization failed!")
        return
    }

    Logger.info("✅ Renderer initialized")

    // Create camera
    val camera = PerspectiveCamera(
        fov = 75.0f,
        aspect = canvas.width.toFloat() / canvas.height.toFloat(),
        near = 0.1f,
        far = 1000.0f
    )

    updateLoadingProgress("World ready! (${generationTime.toInt()}ms)")

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

    // Start game loop
    var lastTime = js("Date.now()") as Double
    var frameCount = 0
    var fpsUpdateTime = lastTime

    fun gameLoop() {
        val currentTime = js("Date.now()") as Double
        val deltaTime = ((currentTime - lastTime) / 1000.0).toFloat()
        lastTime = currentTime

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

        // Update FPS counter (every second)
        frameCount++
        if (currentTime - fpsUpdateTime >= 1000) {
            val fps = (frameCount * 1000.0 / (currentTime - fpsUpdateTime)).toInt()
            val stats = renderer.getStats()
            updateFPS(fps, stats.triangles, stats.calls)
            frameCount = 0
            fpsUpdateTime = currentTime
        }

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
class VoxelCraft(val seed: Long) {
    val world = VoxelWorld(seed)

    fun update(deltaTime: Float) {
        world.update(deltaTime)
    }

    fun dispose() {
        world.dispose()
    }

    companion object {
        fun fromSavedState(state: Any): VoxelCraft {
            val worldState = state as WorldState
            return VoxelCraft(worldState.seed).apply {
                // World is already restored in WorldState.restore()
            }
        }
    }
}
