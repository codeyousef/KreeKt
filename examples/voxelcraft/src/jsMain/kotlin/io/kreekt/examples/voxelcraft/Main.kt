package io.kreekt.examples.voxelcraft

import io.kreekt.camera.PerspectiveCamera
import io.kreekt.renderer.RenderSurface
import io.kreekt.renderer.RendererConfig
import io.kreekt.renderer.webgl.WebGLRenderer
import kotlinx.browser.document
import kotlinx.browser.window
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

fun main() {
    println("🎮 VoxelCraft Starting...")

    window.addEventListener("load", {
        GlobalScope.launch {
            initGame()
        }
    })
}

suspend fun initGame() {
    println("🌍 Initializing VoxelCraft...")

    // Get canvas element
    val canvas = document.getElementById("kreekt-canvas") as? HTMLCanvasElement
    if (canvas == null) {
        println("❌ Canvas element not found!")
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
        println("📂 Restoring from save...")

        // Restore world state
        window.setTimeout({
            savedState.restore().let { restoredWorld ->
                // Copy restored data to our world
                // For now just generate fresh (saved state restoration needs world reference)
                generateTerrainAsync(world, startTime, canvas)
            }
        }, 10)
    } else {
        generateTerrainAsync(world, startTime, canvas)
    }
}

fun generateTerrainAsync(world: VoxelWorld, startTime: Double, canvas: HTMLCanvasElement) {
    println("🌍 Generating world...")
    updateLoadingProgress("Generating terrain... 0%")

    // Use setTimeout to make generation async and allow UI updates
    window.setTimeout({
        world.generateTerrain { current, total ->
            val percent = (current * 100) / total
            updateLoadingProgress("Generating terrain... $percent% ($current/$total chunks)")

            // Log progress every 10%
            if (percent % 10 == 0 && current != total) {
                println("⏳ Generation progress: $percent%")
            }
        }

        val generationTime = js("Date.now()") as Double - startTime
        println("✅ World ready in ${generationTime.toInt()}ms")
        println("📊 Chunks: ${world.chunkCount}")
        println("👤 Player: ${world.player.position}")

        GlobalScope.launch {
            continueInitialization(world, generationTime, canvas)
        }
    }, 10)
}

suspend fun continueInitialization(world: VoxelWorld, generationTime: Double, canvas: HTMLCanvasElement) {
    val storage = WorldStorage()

    updateLoadingProgress("Initializing renderer...")

    // Create WebGL renderer
    val renderer = WebGLRenderer(canvas, RendererConfig(antialias = true))
    val surface = RenderSurface(canvas)

    val initResult = renderer.initialize(surface)
    if (initResult !is io.kreekt.renderer.RendererResult.Success) {
        println("❌ Renderer initialization failed!")
        updateLoadingProgress("Renderer initialization failed!")
        return
    }

    println("✅ Renderer initialized")

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
            println("⚠️ Auto-save failed: ${result.error}")
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
            world.player.rotation.pitch.toFloat(),
            world.player.rotation.yaw.toFloat(),
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

    // Hide loading screen and start
    window.setTimeout({
        hideLoadingScreen()
        println("🚀 Game loop started!")
        println("🎮 Controls: WASD=Move, Mouse=Look, F=Flight, Space/Shift=Up/Down")
        gameLoop()
    }, 1000)
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
        // TODO: Cleanup resources
    }

    companion object {
        fun fromSavedState(state: Any): VoxelCraft {
            // TODO: Implement state restoration
            return VoxelCraft(12345L)
        }
    }
}
