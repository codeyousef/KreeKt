/**
 * JVM/Desktop implementation of the Basic Scene Example
 * Uses LWJGL for OpenGL rendering and window management
 */

import io.kreekt.renderer.Renderer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL
import kotlinx.coroutines.*

actual fun createRenderer(): Renderer {
    return VulkanRenderer() // Or OpenGLRenderer() as fallback
}

actual class InputState(private val window: Long) {
    actual fun isKeyPressed(key: String): Boolean {
        val glfwKey = when (key.uppercase()) {
            "W" -> GLFW_KEY_W
            "S" -> GLFW_KEY_S
            "A" -> GLFW_KEY_A
            "D" -> GLFW_KEY_D
            "Q" -> GLFW_KEY_Q
            "E" -> GLFW_KEY_E
            "ESCAPE" -> GLFW_KEY_ESCAPE
            else -> return false
        }
        return glfwGetKey(window, glfwKey) == GLFW_PRESS
    }

    actual val isMousePressed: Boolean
        get() = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS

    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var currentMouseX = 0.0
    private var currentMouseY = 0.0

    actual val mouseDeltaX: Float
        get() {
            val pos = DoubleArray(1)
            glfwGetCursorPos(window, pos, null)
            currentMouseX = pos[0]
            val delta = (currentMouseX - lastMouseX).toFloat()
            lastMouseX = currentMouseX
            return delta
        }

    actual val mouseDeltaY: Float
        get() {
            val pos = DoubleArray(1)
            glfwGetCursorPos(window, null, pos)
            currentMouseY = pos[0]
            val delta = (currentMouseY - lastMouseY).toFloat()
            lastMouseY = currentMouseY
            return delta
        }
}

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

private var currentInput: InputState? = null

actual fun getCurrentInput(): InputState {
    return currentInput ?: throw IllegalStateException("Input not initialized")
}

/**
 * Desktop main function
 */
fun main(args: Array<String>) = runBlocking {
    // Initialize GLFW
    GLFWErrorCallback.createPrint(System.err).set()

    if (!glfwInit()) {
        throw IllegalStateException("Unable to initialize GLFW")
    }

    // Configure GLFW
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)

    // Create window
    val window = glfwCreateWindow(1920, 1080, "KreeKt Basic Scene Example", NULL, NULL)
    if (window == NULL) {
        throw RuntimeException("Failed to create the GLFW window")
    }

    // Setup input
    currentInput = InputState(window)

    // Center window
    val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
    glfwSetWindowPos(
        window,
        (videoMode.width() - 1920) / 2,
        (videoMode.height() - 1080) / 2
    )

    // Setup OpenGL context
    glfwMakeContextCurrent(window)
    glfwSwapInterval(1) // Enable v-sync
    glfwShowWindow(window)

    GL.createCapabilities()

    println("🖥️  OpenGL Version: ${glGetString(GL_VERSION)}")
    println("🎮 Graphics Card: ${glGetString(GL_RENDERER)}")

    // Setup resize callback
    glfwSetFramebufferSizeCallback(window) { _, width, height ->
        glViewport(0, 0, width, height)
        // Notify example of resize if needed
    }

    // Setup key callback for escape to close
    glfwSetKeyCallback(window) { windowHandle, key, _, action, _ ->
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            glfwSetWindowShouldClose(windowHandle, true)
        }
    }

    try {
        // Create and run the example
        val example = BasicSceneExample()
        example.initialize()
        example.printSceneInfo()

        println("\n🎮 Desktop Controls:")
        println("WASD - Move camera")
        println("QE - Move up/down")
        println("Mouse - Look around")
        println("ESC - Exit")
        println("\n🎬 Starting render loop...")

        var lastTime = glfwGetTime()

        // Main render loop
        while (!glfwWindowShouldClose(window)) {
            val currentTime = glfwGetTime()
            val deltaTime = (currentTime - lastTime).toFloat()
            lastTime = currentTime

            // Clear screen
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            // Handle input
            example.handleInput(currentInput!!)

            // Render frame
            example.render(deltaTime)

            // Swap buffers and poll events
            glfwSwapBuffers(window)
            glfwPollEvents()

            // Prevent busy waiting
            Thread.sleep(1)
        }

        example.dispose()

    } finally {
        // Cleanup
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }
}

// Mock renderer for now - replace with actual implementations
private class VulkanRenderer : Renderer {
    override fun setSize(width: Int, height: Int) {
        glViewport(0, 0, width, height)
    }

    override fun setClearColor(color: io.kreekt.core.math.Color) {
        glClearColor(color.r, color.g, color.b, 1.0f)
    }

    override fun render(scene: io.kreekt.core.scene.Scene, camera: io.kreekt.camera.Camera) {
        // Basic OpenGL rendering - replace with actual Vulkan implementation
        glEnable(GL_DEPTH_TEST)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Render scene objects
        scene.traverse { obj ->
            // Transform and render each object
            renderObject(obj, camera)
        }
    }

    private fun renderObject(obj: io.kreekt.core.scene.Object3D, camera: io.kreekt.camera.Camera) {
        // Basic object rendering - implement actual geometry/material rendering
        glPushMatrix()

        // Apply transform
        val pos = obj.position
        glTranslatef(pos.x, pos.y, pos.z)

        val rot = obj.rotation
        glRotatef(Math.toDegrees(rot.x.toDouble()).toFloat(), 1f, 0f, 0f)
        glRotatef(Math.toDegrees(rot.y.toDouble()).toFloat(), 0f, 1f, 0f)
        glRotatef(Math.toDegrees(rot.z.toDouble()).toFloat(), 0f, 0f, 1f)

        val scale = obj.scale
        glScalef(scale.x, scale.y, scale.z)

        // Render geometry (placeholder)
        // obj.geometry?.render()

        glPopMatrix()
    }

    override fun getRenderInfo(): RenderInfo {
        return RenderInfo(0, 0, 0, 0)
    }

    override fun dispose() {
        // Cleanup renderer resources
    }
}

data class RenderInfo(
    val triangles: Int,
    val drawCalls: Int,
    val textureMemory: Long,
    val bufferMemory: Long
)