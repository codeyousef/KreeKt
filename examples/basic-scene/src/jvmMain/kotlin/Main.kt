import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    println("🚀 KreeKt Basic Scene Example (LWJGL)")
    println("======================================")

    // Initialize GLFW
    GLFWErrorCallback.createPrint(System.err).set()
    if (!glfwInit()) {
        throw IllegalStateException("Unable to initialize GLFW")
    }

    // Configure GLFW - use compatibility profile for immediate mode
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
    // Don't request core profile so we can use immediate mode for demo

    // Create the window
    val window = glfwCreateWindow(1280, 720, "KreeKt 3D Engine - Basic Scene Example", NULL, NULL)
    if (window == NULL) {
        glfwTerminate()
        throw RuntimeException("Failed to create the GLFW window")
    }

    // Setup key callback
    glfwSetKeyCallback(window) { windowHandle, key, scancode, action, mods ->
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            glfwSetWindowShouldClose(windowHandle, true)
        }
    }

    // Make the OpenGL context current BEFORE any GL calls
    glfwMakeContextCurrent(window)

    // This line is critical for LWJGL's interoperation with GLFW's OpenGL context
    GL.createCapabilities()

    glfwSwapInterval(1) // Enable v-sync

    // Center the window
    val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
    glfwSetWindowPos(
        window,
        (vidmode.width() - 1280) / 2,
        (vidmode.height() - 720) / 2
    )

    // Show the window
    glfwShowWindow(window)

    // Set clear color
    glClearColor(0.05f, 0.05f, 0.1f, 1.0f)

    // Enable depth testing
    glEnable(GL_DEPTH_TEST)
    glDepthFunc(GL_LESS)

    // Run the example in a coroutine scope
    runBlocking {
        try {
            val example = BasicSceneExample()
            println("Initializing scene...")
            example.initialize()
            example.printSceneInfo()

            println("\n🎮 Controls:")
            println("  ESC - Exit")
            println("  WASD - Move camera")
            println("  Mouse - Look around")
            println("\n🎬 Starting render loop...")

            var lastTime = System.currentTimeMillis() / 1000.0
            var frameCount = 0
            var fpsTimer = 0.0

            // Run the rendering loop until the user has attempted to close the window
            while (!glfwWindowShouldClose(window)) {
                val currentTime = System.currentTimeMillis() / 1000.0
                val deltaTime = (currentTime - lastTime).toFloat()
                lastTime = currentTime

                // Clear the framebuffer
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

                // Get window size for viewport
                val width = IntArray(1)
                val height = IntArray(1)
                glfwGetFramebufferSize(window, width, height)
                glViewport(0, 0, width[0], height[0])

                // Setup basic projection matrix
                val aspect = width[0].toFloat() / height[0].toFloat()
                setupPerspectiveProjection(75.0f, aspect, 0.1f, 100.0f)

                // Draw a simple animated scene
                drawSimpleScene(currentTime.toFloat())

                // Handle input
                val input = getCurrentInput(window)
                example.handleInput(input)

                // Render the example (even though it just prints to console for now)
                example.render(deltaTime)

                // Swap buffers and poll events
                glfwSwapBuffers(window)
                glfwPollEvents()

                // Update FPS counter
                frameCount++
                fpsTimer += deltaTime.toDouble()
                if (fpsTimer >= 1.0) {
                    val fps = frameCount / fpsTimer
                    glfwSetWindowTitle(window, "KreeKt 3D Engine - FPS: ${fps.toInt()}")
                    frameCount = 0
                    fpsTimer = 0.0
                }
        }

            println("\n✅ Example completed successfully!")

        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }

    // Clean up
    glfwDestroyWindow(window)
    glfwTerminate()
}

fun setupPerspectiveProjection(fov: Float, aspect: Float, near: Float, far: Float) {
    // Simple perspective projection using deprecated immediate mode for demo
    glMatrixMode(GL_PROJECTION)
    glLoadIdentity()
    val fH = kotlin.math.tan(Math.toRadians(fov / 2.0)).toFloat() * near
    val fW = fH * aspect
    glFrustum(-fW.toDouble(), fW.toDouble(), -fH.toDouble(), fH.toDouble(), near.toDouble(), far.toDouble())
    glMatrixMode(GL_MODELVIEW)
}

fun drawSimpleScene(time: Float) {
    glLoadIdentity()
    glTranslatef(0f, 0f, -10f)

    // Draw a rotating cube
    glPushMatrix()
    glRotatef(time * 30f, 1f, 1f, 0f)
    drawCube()
    glPopMatrix()

    // Draw orbiting spheres
    for (i in 0..2) {
        glPushMatrix()
        val angle = time + i * 2.0f * Math.PI.toFloat() / 3
        glTranslatef(cos(angle) * 3f, sin(angle) * 2f, 0f)
        glScalef(0.5f, 0.5f, 0.5f)
        drawCube() // Using cube as placeholder for sphere
        glPopMatrix()
    }

    // Draw ground plane
    glPushMatrix()
    glTranslatef(0f, -2f, 0f)
    glScalef(10f, 0.1f, 10f)
    drawCube()
    glPopMatrix()
}

fun drawCube() {
    // Draw a simple colored cube using immediate mode (for demo purposes)
    glBegin(GL_QUADS)

    // Front face (red)
    glColor3f(0.8f, 0.3f, 0.2f)
    glVertex3f(-1f, -1f, 1f)
    glVertex3f(1f, -1f, 1f)
    glVertex3f(1f, 1f, 1f)
    glVertex3f(-1f, 1f, 1f)

    // Back face (green)
    glColor3f(0.3f, 0.8f, 0.3f)
    glVertex3f(-1f, -1f, -1f)
    glVertex3f(-1f, 1f, -1f)
    glVertex3f(1f, 1f, -1f)
    glVertex3f(1f, -1f, -1f)

    // Top face (blue)
    glColor3f(0.3f, 0.3f, 0.8f)
    glVertex3f(-1f, 1f, -1f)
    glVertex3f(-1f, 1f, 1f)
    glVertex3f(1f, 1f, 1f)
    glVertex3f(1f, 1f, -1f)

    // Bottom face (yellow)
    glColor3f(0.8f, 0.8f, 0.3f)
    glVertex3f(-1f, -1f, -1f)
    glVertex3f(1f, -1f, -1f)
    glVertex3f(1f, -1f, 1f)
    glVertex3f(-1f, -1f, 1f)

    // Right face (magenta)
    glColor3f(0.8f, 0.3f, 0.8f)
    glVertex3f(1f, -1f, -1f)
    glVertex3f(1f, 1f, -1f)
    glVertex3f(1f, 1f, 1f)
    glVertex3f(1f, -1f, 1f)

    // Left face (cyan)
    glColor3f(0.3f, 0.8f, 0.8f)
    glVertex3f(-1f, -1f, -1f)
    glVertex3f(-1f, -1f, 1f)
    glVertex3f(-1f, 1f, 1f)
    glVertex3f(-1f, 1f, -1f)

    glEnd()
}