package io.kreekt.examples.voxelcraft

import io.kreekt.camera.PerspectiveCamera
import kotlinx.coroutines.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil
import kotlin.system.measureTimeMillis

class VoxelCraftJVM {
    private var window: Long = 0
    private lateinit var world: VoxelWorld
    private lateinit var camera: PerspectiveCamera
    private val gameScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Input state
    private val keysPressed = mutableSetOf<Int>()
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var firstMouse = true

    fun run() {
        init()
        gameLoop()
        cleanup()
    }

    private fun init() {
        Logger.info("🎮 VoxelCraft JVM Starting...")
        Logger.info("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
        Logger.info("Java: ${System.getProperty("java.version")}")

        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW
        if (!glfwInit()) {
            throw RuntimeException("Failed to initialize GLFW")
        }

        // Configure GLFW - try Core Profile first, fallback to Any if needed
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        // Create window
        window = glfwCreateWindow(800, 600, "VoxelCraft JVM", MemoryUtil.NULL, MemoryUtil.NULL)

        // If core profile fails, try compatibility profile
        if (window == MemoryUtil.NULL) {
            Logger.warn("Failed to create Core Profile context, trying compatibility profile...")
            glfwDefaultWindowHints()
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE)
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
            glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

            window = glfwCreateWindow(800, 600, "VoxelCraft JVM", MemoryUtil.NULL, MemoryUtil.NULL)
        }

        if (window == MemoryUtil.NULL) {
            glfwTerminate()
            throw RuntimeException("Failed to create GLFW window - please check your GPU drivers")
        }

        // Make OpenGL context current BEFORE any other GL operations
        glfwMakeContextCurrent(window)

        // Setup input callbacks
        setupInputHandlers()

        // Enable V-Sync
        glfwSwapInterval(1)

        // Initialize OpenGL capabilities - wrap in try-catch for better error reporting
        try {
            GL.createCapabilities()
        } catch (e: Exception) {
            Logger.error("Failed to initialize OpenGL capabilities", e)
            glfwDestroyWindow(window)
            glfwTerminate()
            throw RuntimeException("Failed to initialize OpenGL - please update your GPU drivers", e)
        }

        Logger.info("✅ OpenGL ${glGetString(GL_VERSION)}")
        Logger.info("✅ Renderer: ${glGetString(GL_RENDERER)}")
        Logger.info("✅ Vendor: ${glGetString(GL_VENDOR)}")

        // Enable depth testing
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)

        // Enable backface culling
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)

        // Set clear color (sky blue)
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f)

        // Show window
        glfwShowWindow(window)

        // Initialize camera
        camera = PerspectiveCamera(
            fov = 75.0f,
            aspect = 800f / 600f,
            near = 0.1f,
            far = 1000.0f
        )

        // Initialize world and generate terrain
        Logger.info("🌍 Creating world...")
        world = VoxelWorld(seed = 12345L, parentScope = gameScope)
        world.player.position.set(0.0f, 100.0f, 0.0f)
        world.player.isFlying = true

        // Generate terrain asynchronously
        runBlocking {
            val generationTime = measureTimeMillis {
                world.generateTerrain { current, total ->
                    val percent = (current * 100) / total
                    if (percent % 10 == 0) {
                        Logger.info("🌍 Generating terrain... $percent% ($current/$total chunks)")
                    }
                }
            }
            Logger.info("✅ Terrain generation complete in ${generationTime}ms")
            Logger.info("📦 Chunks: ${world.chunkCount}")
        }
    }

    private fun setupInputHandlers() {
        // Keyboard callback
        glfwSetKeyCallback(window) { _, key, _, action, _ ->
            when (action) {
                GLFW_PRESS -> {
                    keysPressed.add(key)
                    // Toggle flight mode with F key
                    if (key == GLFW_KEY_F) {
                        world.player.toggleFlight()
                        Logger.info("🦅 Flight mode: ${if (world.player.isFlying) "ON" else "OFF"}")
                    }
                    // Close window with ESC
                    if (key == GLFW_KEY_ESCAPE) {
                        glfwSetWindowShouldClose(window, true)
                    }
                }
                GLFW_RELEASE -> keysPressed.remove(key)
            }
        }

        // Mouse movement callback
        glfwSetCursorPosCallback(window) { _, xpos, ypos ->
            if (firstMouse) {
                lastMouseX = xpos
                lastMouseY = ypos
                firstMouse = false
            }

            val deltaX = xpos - lastMouseX
            val deltaY = lastMouseY - ypos // Reversed since y-coordinates go from bottom to top

            lastMouseX = xpos
            lastMouseY = ypos

            // Mouse sensitivity
            val sensitivity = 0.002
            world.player.rotate(deltaY * sensitivity, deltaX * sensitivity)
        }

        // Capture mouse cursor
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
    }

    private fun processInput(deltaTime: Float) {
        val moveSpeed = if (world.player.isFlying) 20.0f else 5.0f
        val moveAmount = moveSpeed * deltaTime

        // Calculate forward and right vectors from rotation
        val yaw = world.player.rotation.y
        val pitch = world.player.rotation.x

        val forward = io.kreekt.core.math.Vector3(
            kotlin.math.sin(yaw) * kotlin.math.cos(pitch),
            0f,
            kotlin.math.cos(yaw) * kotlin.math.cos(pitch)
        )

        val right = io.kreekt.core.math.Vector3(
            kotlin.math.sin(yaw + Math.PI.toFloat() / 2f),
            0f,
            kotlin.math.cos(yaw + Math.PI.toFloat() / 2f)
        )

        // WASD movement
        if (GLFW_KEY_W in keysPressed) {
            world.player.move(forward.multiplyScalar(moveAmount))
        }
        if (GLFW_KEY_S in keysPressed) {
            world.player.move(forward.multiplyScalar(-moveAmount))
        }
        if (GLFW_KEY_A in keysPressed) {
            world.player.move(right.multiplyScalar(-moveAmount))
        }
        if (GLFW_KEY_D in keysPressed) {
            world.player.move(right.multiplyScalar(moveAmount))
        }

        // Vertical movement in flight mode
        if (world.player.isFlying) {
            if (GLFW_KEY_SPACE in keysPressed) {
                world.player.move(io.kreekt.core.math.Vector3(0f, moveAmount, 0f))
            }
            if (GLFW_KEY_LEFT_SHIFT in keysPressed) {
                world.player.move(io.kreekt.core.math.Vector3(0f, -moveAmount, 0f))
            }
        } else {
            // Jump when on ground
            if (GLFW_KEY_SPACE in keysPressed && world.player.isOnGround) {
                world.player.jump()
            }
        }
    }

    private fun gameLoop() {
        var lastTime = System.nanoTime()
        var frameCount = 0

        Logger.info("🎮 Game loop starting...")
        Logger.info("🎮 Controls: WASD=Move, Mouse=Look, F=Flight, Space/Shift=Up/Down, ESC=Quit")

        while (!glfwWindowShouldClose(window)) {
            val currentTime = System.nanoTime()
            val deltaTime = ((currentTime - lastTime) / 1_000_000_000.0).toFloat()
            lastTime = currentTime

            // Process input
            processInput(deltaTime)

            // Update world
            world.update(deltaTime)

            // Sync camera with player
            camera.position.set(
                world.player.position.x,
                world.player.position.y,
                world.player.position.z
            )
            camera.rotation.set(
                world.player.rotation.x,
                world.player.rotation.y,
                0.0f
            )

            // Update camera matrices
            camera.updateMatrixWorld(true)
            camera.updateProjectionMatrix()

            // Render
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            // TODO: Render scene using KreeKt renderer
            // For now, just clear the screen

            // Log stats every 60 frames
            if (frameCount % 60 == 0) {
                val fps = (1.0f / deltaTime).toInt()
                Logger.info("📊 FPS: $fps | Player: (${world.player.position.x.toInt()}, ${world.player.position.y.toInt()}, ${world.player.position.z.toInt()}) | Chunks: ${world.chunkCount}")
            }

            frameCount++

            // Swap buffers and poll events
            glfwSwapBuffers(window)
            glfwPollEvents()
        }
    }

    private fun cleanup() {
        Logger.info("🔚 Shutting down...")

        // Dispose world
        world.dispose()

        // Cancel coroutine scope
        gameScope.cancel()

        // Destroy window
        glfwDestroyWindow(window)

        // Terminate GLFW
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()

        Logger.info("✅ Cleanup complete")
    }
}

fun main() {
    try {
        VoxelCraftJVM().run()
    } catch (e: Exception) {
        Logger.error("❌ Fatal error: ${e.message}", e)
        throw e
    }
}
