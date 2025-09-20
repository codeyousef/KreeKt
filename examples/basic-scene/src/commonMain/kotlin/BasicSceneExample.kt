/**
 * Basic Scene Example - Demonstrates KreeKt 3D Library Core Functionality
 *
 * This example shows how to create a simple 3D scene with:
 * - Basic geometries (cube, sphere, plane)
 * - PBR materials with textures
 * - Dynamic lighting
 * - Camera controls
 * - Animation loop
 */

import io.kreekt.core.scene.Scene
import io.kreekt.core.scene.Object3D
import io.kreekt.core.math.*
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.renderer.Renderer
import io.kreekt.geometry.PrimitiveGeometry
import io.kreekt.material.PBRMaterial
import io.kreekt.lighting.AdvancedLights
import io.kreekt.animation.AnimationMixer
import kotlinx.coroutines.*

class BasicSceneExample {
    private lateinit var scene: Scene
    private lateinit var camera: PerspectiveCamera
    private lateinit var renderer: Renderer
    private lateinit var animationMixer: AnimationMixer

    // Scene objects
    private lateinit var rotatingCube: Object3D
    private lateinit var floatingSphere: Object3D
    private lateinit var ground: Object3D

    // Animation state
    private var time = 0.0f

    suspend fun initialize() {
        println("🚀 Initializing KreeKt Basic Scene Example...")

        // Create scene
        scene = Scene().apply {
            background = Color(0.1f, 0.1f, 0.2f) // Dark blue background
        }

        // Setup camera
        camera = PerspectiveCamera(
            fov = 75.0f,
            aspect = 16.0f / 9.0f,
            near = 0.1f,
            far = 1000.0f
        ).apply {
            position.set(5.0f, 5.0f, 5.0f)
            lookAt(Vector3.ZERO)
        }

        // Initialize renderer (platform-specific)
        renderer = createRenderer()
        renderer.setSize(1920, 1080)
        renderer.setClearColor(Color(0.05f, 0.05f, 0.1f))

        // Setup scene objects
        createSceneObjects()
        setupLighting()
        setupAnimations()

        println("✅ Scene initialized successfully!")
        println("📊 Scene stats: ${scene.children.size} objects")
    }

    private suspend fun createSceneObjects() {
        // Create rotating cube with PBR material
        rotatingCube = Object3D().apply {
            geometry = PrimitiveGeometry.createBox(2.0f, 2.0f, 2.0f)
            material = PBRMaterial().apply {
                baseColor = Color(0.8f, 0.3f, 0.2f) // Red-orange
                metallic = 0.7f
                roughness = 0.3f
                emissive = Color(0.1f, 0.05f, 0.0f) // Slight glow
            }
            position.set(0.0f, 2.0f, 0.0f)
        }
        scene.add(rotatingCube)

        // Create floating sphere with animated material
        floatingSphere = Object3D().apply {
            geometry = PrimitiveGeometry.createSphere(1.5f, 32, 16)
            material = PBRMaterial().apply {
                baseColor = Color(0.2f, 0.6f, 0.9f) // Blue
                metallic = 0.1f
                roughness = 0.1f
                transparent = true
                opacity = 0.8f
            }
            position.set(3.0f, 3.0f, 2.0f)
        }
        scene.add(floatingSphere)

        // Create ground plane
        ground = Object3D().apply {
            geometry = PrimitiveGeometry.createPlane(20.0f, 20.0f)
            material = PBRMaterial().apply {
                baseColor = Color(0.4f, 0.4f, 0.4f) // Gray
                metallic = 0.0f
                roughness = 0.8f
            }
            rotation.x = -Math.PI.toFloat() / 2 // Rotate to be horizontal
        }
        scene.add(ground)

        // Add some decorative objects
        repeat(5) { i ->
            val decorCube = Object3D().apply {
                geometry = PrimitiveGeometry.createBox(0.5f, 0.5f, 0.5f)
                material = PBRMaterial().apply {
                    baseColor = Color(
                        0.3f + (i * 0.15f),
                        0.6f,
                        0.9f - (i * 0.1f)
                    )
                    metallic = 0.5f
                    roughness = 0.4f
                }
                position.set(
                    (i - 2) * 2.0f,
                    0.25f,
                    -3.0f
                )
            }
            scene.add(decorCube)
        }
    }

    private fun setupLighting() {
        // Main directional light (sun)
        val directionalLight = AdvancedLights.createDirectionalLight(
            color = Color.WHITE,
            intensity = 3.0f
        ).apply {
            position.set(10.0f, 10.0f, 5.0f)
            lookAt(Vector3.ZERO)
            castShadow = true
        }
        scene.add(directionalLight)

        // Ambient light for overall illumination
        val ambientLight = AdvancedLights.createAmbientLight(
            color = Color(0.3f, 0.3f, 0.4f),
            intensity = 0.5f
        )
        scene.add(ambientLight)

        // Point light for local illumination
        val pointLight = AdvancedLights.createPointLight(
            color = Color(1.0f, 0.8f, 0.6f), // Warm white
            intensity = 2.0f,
            distance = 100.0f
        ).apply {
            position.set(-5.0f, 5.0f, 5.0f)
        }
        scene.add(pointLight)

        // Spot light for dramatic effect
        val spotLight = AdvancedLights.createSpotLight(
            color = Color(0.8f, 0.9f, 1.0f), // Cool white
            intensity = 5.0f,
            distance = 50.0f,
            angle = Math.PI.toFloat() / 6, // 30 degrees
            penumbra = 0.2f
        ).apply {
            position.set(8.0f, 8.0f, 8.0f)
            lookAt(rotatingCube.position)
            castShadow = true
        }
        scene.add(spotLight)
    }

    private fun setupAnimations() {
        animationMixer = AnimationMixer()

        // No pre-defined animations for this basic example
        // We'll do manual animation in the render loop
    }

    /**
     * Main render loop - call this continuously for animation
     */
    fun render(deltaTime: Float) {
        time += deltaTime

        // Animate rotating cube
        rotatingCube.rotation.apply {
            y = time * 0.5f
            x = time * 0.3f
        }

        // Animate floating sphere
        floatingSphere.position.apply {
            y = 3.0f + sin(time * 2.0f) * 0.5f
            x = 3.0f + cos(time * 1.0f) * 1.0f
        }

        // Pulse the sphere's emissive intensity
        val sphereMaterial = floatingSphere.material as PBRMaterial
        sphereMaterial.emissive = Color(
            0.1f + sin(time * 3.0f) * 0.1f,
            0.2f + cos(time * 2.0f) * 0.1f,
            0.3f
        )

        // Rotate camera around the scene
        val cameraRadius = 8.0f
        camera.position.apply {
            x = cos(time * 0.2f) * cameraRadius
            z = sin(time * 0.2f) * cameraRadius
            y = 5.0f + sin(time * 0.1f) * 2.0f
        }
        camera.lookAt(Vector3(0.0f, 1.0f, 0.0f))

        // Update animations
        animationMixer.update(deltaTime)

        // Render the scene
        renderer.render(scene, camera)
    }

    /**
     * Add interactive controls for the camera
     */
    fun handleInput(input: InputState) {
        when {
            input.isKeyPressed("W") -> camera.position.add(camera.getWorldDirection() * 0.1f)
            input.isKeyPressed("S") -> camera.position.sub(camera.getWorldDirection() * 0.1f)
            input.isKeyPressed("A") -> camera.position.add(camera.getRight() * -0.1f)
            input.isKeyPressed("D") -> camera.position.add(camera.getRight() * 0.1f)
            input.isKeyPressed("Q") -> camera.position.y += 0.1f
            input.isKeyPressed("E") -> camera.position.y -= 0.1f
        }

        // Mouse look
        if (input.isMousePressed) {
            val sensitivity = 0.002f
            camera.rotation.y -= input.mouseDeltaX * sensitivity
            camera.rotation.x -= input.mouseDeltaY * sensitivity
            camera.rotation.x = camera.rotation.x.coerceIn(-1.5f, 1.5f) // Clamp vertical look
        }
    }

    fun resize(width: Int, height: Int) {
        camera.aspect = width.toFloat() / height.toFloat()
        camera.updateProjectionMatrix()
        renderer.setSize(width, height)
    }

    fun dispose() {
        println("🧹 Cleaning up KreeKt Basic Scene Example...")
        renderer.dispose()
        // Dispose geometries and materials
        scene.traverse { obj ->
            obj.geometry?.dispose()
            obj.material?.dispose()
        }
    }

    /**
     * Print scene information for debugging
     */
    fun printSceneInfo() {
        println("\n📋 Scene Information:")
        println("Objects in scene: ${scene.children.size}")
        println("Camera position: ${camera.position}")
        println("Camera rotation: ${camera.rotation}")

        scene.children.forEachIndexed { index, obj ->
            println("  [$index] ${obj::class.simpleName} at ${obj.position}")
        }

        // Performance info
        val renderInfo = renderer.getRenderInfo()
        println("\n📊 Render Statistics:")
        println("Triangles: ${renderInfo.triangles}")
        println("Draw calls: ${renderInfo.drawCalls}")
        println("Texture memory: ${renderInfo.textureMemory / 1024 / 1024}MB")
    }
}

/**
 * Platform-specific renderer creation
 */
expect fun createRenderer(): Renderer

/**
 * Input state for handling user interaction
 */
expect class InputState {
    fun isKeyPressed(key: String): Boolean
    val isMousePressed: Boolean
    val mouseDeltaX: Float
    val mouseDeltaY: Float
}

/**
 * Example main function structure
 */
suspend fun runBasicSceneExample() {
    val example = BasicSceneExample()

    try {
        example.initialize()
        example.printSceneInfo()

        println("\n🎮 Controls:")
        println("WASD - Move camera")
        println("QE - Move up/down")
        println("Mouse - Look around")
        println("\n🎬 Starting render loop...")

        // Main application loop (implement this per platform)
        var lastTime = getCurrentTimeMillis()

        while (true) {
            val currentTime = getCurrentTimeMillis()
            val deltaTime = (currentTime - lastTime) / 1000.0f
            lastTime = currentTime

            // Handle input (implement per platform)
            val input = getCurrentInput()
            example.handleInput(input)

            // Render frame
            example.render(deltaTime)

            // Platform-specific frame limiting/vsync
            delay(16) // ~60 FPS
        }

    } catch (e: Exception) {
        println("❌ Error running example: ${e.message}")
        e.printStackTrace()
    } finally {
        example.dispose()
    }
}

expect fun getCurrentTimeMillis(): Long
expect fun getCurrentInput(): InputState