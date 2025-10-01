# KreeKt Quickstart Guide

**Feature**: Complete Three.js r180 Feature Parity
**Branch**: `012-complete-three-js`
**Date**: 2025-10-01

## Introduction

This quickstart guide demonstrates core KreeKt functionality through four complete, runnable examples. Each example showcases Three.js API compatibility while leveraging Kotlin Multiplatform idioms.

**Prerequisites**:
- Kotlin 1.9+
- Gradle 8.0+
- Target platform SDK (JVM 17+, Node.js 18+, or native toolchain)

**Installation**:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.kreekt:kreekt-core:1.0.0")
    implementation("io.kreekt:kreekt-renderer:1.0.0")
    implementation("io.kreekt:kreekt-loader:1.0.0")
}
```

---

## Example 1: Hello Cube (Minimal Working Scene)

This example creates a basic 3D scene with a rotating cube, demonstrating the fundamental KreeKt workflow.

### Complete Code

```kotlin
package io.kreekt.examples

import io.kreekt.core.math.*
import io.kreekt.core.scene.*
import io.kreekt.geometry.*
import io.kreekt.material.*
import io.kreekt.camera.*
import io.kreekt.renderer.*
import kotlinx.coroutines.*

/**
 * Hello Cube: Minimal KreeKt example
 *
 * Creates a scene with a rotating red cube, demonstrating:
 * - Scene setup
 * - Camera configuration
 * - Geometry and material creation
 * - Render loop with animation
 */
suspend fun main() = coroutineScope {
    // 1. Create renderer
    val renderer = createRenderer(
        RendererConfiguration(
            width = 800,
            height = 600,
            title = "KreeKt Hello Cube"
        )
    ).getOrThrow()

    // 2. Create scene
    val scene = Scene().apply {
        background = SceneBackground.SolidColor(Color(0x1a1a1a))
    }

    // 3. Create camera
    val camera = PerspectiveCamera(
        fov = 75f,
        aspect = 800f / 600f,
        near = 0.1f,
        far = 1000f
    ).apply {
        position.z = 5f
    }

    // 4. Create cube geometry
    val geometry = BoxGeometry(
        width = 1f,
        height = 1f,
        depth = 1f
    )

    // 5. Create material
    val material = MeshBasicMaterial(
        color = Color(0xff0000)  // Red color
    )

    // 6. Create mesh
    val cube = Mesh(geometry, material)
    scene.add(cube)

    // 7. Animation loop
    val clock = Clock()

    launch {
        while (isActive) {
            val deltaTime = clock.getDelta()

            // Rotate cube
            cube.rotation.x += deltaTime * 0.5f
            cube.rotation.y += deltaTime * 0.3f

            // Render scene
            renderer.render(scene, camera)

            // Wait for next frame (60 FPS target)
            delay(16L)
        }
    }

    // 8. Cleanup on exit
    renderer.onDispose = {
        geometry.dispose()
        material.dispose()
        println("Resources disposed")
    }
}
```

### Key Concepts

#### 1. Renderer Creation
```kotlin
val renderer = createRenderer(
    RendererConfiguration(width = 800, height = 600)
).getOrThrow()
```
- Platform-specific renderer (Vulkan/WebGPU/Metal)
- Returns `Result<Renderer>` for error handling
- Configuration includes size, vsync, MSAA

#### 2. Scene Graph
```kotlin
val scene = Scene()
scene.add(cube)
```
- Scene is root container for 3D objects
- Hierarchical structure (parent-child)
- Background can be color, texture, or cube map

#### 3. Camera Setup
```kotlin
val camera = PerspectiveCamera(
    fov = 75f,      // Field of view in degrees
    aspect = 4/3f,  // Aspect ratio
    near = 0.1f,    // Near clipping plane
    far = 1000f     // Far clipping plane
)
```
- Defines viewing frustum
- Position via `camera.position.set(x, y, z)`

#### 4. Geometry and Material
```kotlin
val geometry = BoxGeometry(1f, 1f, 1f)
val material = MeshBasicMaterial(color = Color(0xff0000))
val mesh = Mesh(geometry, material)
```
- Geometry: Vertex data (positions, normals, UVs)
- Material: Surface appearance
- Mesh: Combines geometry + material

#### 5. Animation Loop
```kotlin
launch {
    while (isActive) {
        val deltaTime = clock.getDelta()
        cube.rotation.y += deltaTime * 0.3f
        renderer.render(scene, camera)
        delay(16L)  // 60 FPS
    }
}
```
- Coroutine-based render loop
- Delta time for frame-rate independent animation
- Automatic cleanup on scope cancellation

### Expected Output

Running this example displays:
- 800x600 window with dark gray background
- Red cube rotating smoothly
- 60 FPS performance

### Three.js Comparison

**Three.js (JavaScript)**:
```javascript
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, 4/3, 0.1, 1000);
const renderer = new THREE.WebGLRenderer();
renderer.setSize(800, 600);

const geometry = new THREE.BoxGeometry(1, 1, 1);
const material = new THREE.MeshBasicMaterial({ color: 0xff0000 });
const cube = new THREE.Mesh(geometry, material);
scene.add(cube);

camera.position.z = 5;

function animate() {
    requestAnimationFrame(animate);
    cube.rotation.x += 0.01;
    cube.rotation.y += 0.01;
    renderer.render(scene, camera);
}
animate();
```

**KreeKt (Kotlin)**:
```kotlin
val scene = Scene()
val camera = PerspectiveCamera(75f, 4/3f, 0.1f, 1000f)
val renderer = createRenderer().getOrThrow()

val geometry = BoxGeometry(1f, 1f, 1f)
val material = MeshBasicMaterial(color = Color(0xff0000))
val cube = Mesh(geometry, material)
scene.add(cube)

camera.position.z = 5f

launch {
    while (isActive) {
        cube.rotation.x += 0.01f
        cube.rotation.y += 0.01f
        renderer.render(scene, camera)
        delay(16L)
    }
}
```

**Key Differences**:
- Kotlin: Type-safe floats (`1f`), coroutines for async
- Three.js: Dynamic types, `requestAnimationFrame`
- API structure nearly identical

---

## Example 2: Asset Loading (GLTF Model with Progress)

This example loads a 3D model from a GLTF file with progress tracking, demonstrating KreeKt's asset loading system.

### Complete Code

```kotlin
package io.kreekt.examples

import io.kreekt.core.math.*
import io.kreekt.core.scene.*
import io.kreekt.camera.*
import io.kreekt.renderer.*
import io.kreekt.loader.*
import io.kreekt.light.*
import kotlinx.coroutines.*

/**
 * Asset Loading Example
 *
 * Demonstrates:
 * - GLTF model loading with progress tracking
 * - Lighting setup for PBR materials
 * - Model inspection and manipulation
 * - Error handling for asset loading
 */
suspend fun main() = coroutineScope {
    // 1. Setup renderer and scene
    val renderer = createRenderer(
        RendererConfiguration(
            width = 1280,
            height = 720,
            title = "KreeKt Asset Loading"
        )
    ).getOrThrow()

    val scene = Scene().apply {
        background = SceneBackground.SolidColor(Color(0x202020))
    }

    // 2. Setup camera
    val camera = PerspectiveCamera(
        fov = 45f,
        aspect = 1280f / 720f,
        near = 0.1f,
        far = 1000f
    ).apply {
        position.set(0f, 2f, 5f)
        lookAt(Vector3(0f, 0f, 0f))
    }

    // 3. Add lighting for PBR materials
    val ambientLight = AmbientLight(
        color = Color(0xffffff),
        intensity = 0.5f
    )
    scene.add(ambientLight)

    val directionalLight = DirectionalLight(
        color = Color(0xffffff),
        intensity = 1f
    ).apply {
        position.set(5f, 10f, 7.5f)
        castShadow = true
    }
    scene.add(directionalLight)

    // 4. Setup loading manager with progress tracking
    val loadingManager = LoadingManager().apply {
        onStart = { url, itemsLoaded, itemsTotal ->
            println("Started loading: $url")
        }

        onProgress = { url, itemsLoaded, itemsTotal ->
            val progress = (itemsLoaded.toFloat() / itemsTotal.toFloat()) * 100f
            println("Loading: ${progress.toInt()}% ($itemsLoaded/$itemsTotal)")
        }

        onLoad = {
            println("All assets loaded successfully!")
        }

        onError = { url, error ->
            println("Error loading $url: ${error.message}")
        }
    }

    // 5. Create GLTF loader
    val gltfLoader = GLTFLoader(loadingManager)

    // 6. Load model asynchronously
    println("Loading model...")
    val result = gltfLoader.load(
        url = "models/robot.gltf",
        onProgress = { event ->
            val progress = (event.loaded.toFloat() / event.total.toFloat()) * 100f
            println("Model: ${progress.toInt()}% (${event.loaded}/${event.total} bytes)")
        }
    )

    // 7. Handle loading result
    val gltf = when {
        result.isSuccess -> result.getOrThrow()
        else -> {
            println("Failed to load model: ${result.exceptionOrNull()?.message}")
            return@coroutineScope
        }
    }

    // 8. Add model to scene
    val model = gltf.scene
    scene.add(model)

    // 9. Inspect model structure
    println("\nModel structure:")
    model.traverse { object3D ->
        println("  ${object3D.type}: ${object3D.name}")
    }

    // 10. Scale and position model
    model.scale.set(0.5f, 0.5f, 0.5f)
    model.position.y = -1f

    // 11. Play first animation if available
    val mixer = if (gltf.animations.isNotEmpty()) {
        AnimationMixer(model).apply {
            val clip = gltf.animations[0]
            println("\nPlaying animation: ${clip.name} (${clip.duration}s)")
            clipAction(clip).play()
        }
    } else {
        println("\nNo animations found in model")
        null
    }

    // 12. Render loop with animation
    val clock = Clock()

    launch {
        while (isActive) {
            val deltaTime = clock.getDelta()

            // Update animations
            mixer?.update(deltaTime)

            // Rotate model slowly
            model.rotation.y += deltaTime * 0.2f

            // Render
            renderer.render(scene, camera)
            delay(16L)
        }
    }

    // 13. Cleanup
    renderer.onDispose = {
        gltf.dispose()
        println("Model resources disposed")
    }
}

/**
 * Extension: Dispose GLTF and all its resources
 */
fun GLTF.dispose() {
    scene.traverse { object3D ->
        when (object3D) {
            is Mesh -> {
                object3D.geometry.dispose()
                object3D.material.dispose()
            }
        }
    }
}
```

### Key Concepts

#### 1. Loading Manager
```kotlin
val loadingManager = LoadingManager().apply {
    onStart = { url, itemsLoaded, itemsTotal -> }
    onProgress = { url, itemsLoaded, itemsTotal -> }
    onLoad = { }
    onError = { url, error -> }
}
```
- Tracks loading progress across multiple assets
- Callbacks for start, progress, completion, errors
- Centralizes asset management

#### 2. GLTF Loader
```kotlin
val gltfLoader = GLTFLoader(loadingManager)
val result = gltfLoader.load("models/robot.gltf") { event ->
    println("Progress: ${event.percentage}%")
}
```
- Asynchronous loading with coroutines
- Returns `Result<GLTF>` for error handling
- Supports embedded textures and animations

#### 3. GLTF Structure
```kotlin
data class GLTF(
    val scene: Scene,              // Default scene
    val scenes: List<Scene>,       // All scenes
    val animations: List<AnimationClip>,
    val cameras: List<Camera>,
    val asset: GLTFAsset
)
```
- Complete scene graph
- Embedded animations
- Material and texture references

#### 4. Animation Playback
```kotlin
val mixer = AnimationMixer(model)
mixer.clipAction(gltf.animations[0]).play()

// In render loop
mixer.update(deltaTime)
```
- AnimationMixer manages playback
- Supports blending, crossfading, weight control

### Expected Output

Running this example:
1. Prints loading progress: "Loading: 25% (2/8)"
2. Displays model structure: meshes, bones, materials
3. Shows animated 3D model rotating
4. Reports animation playback

### Error Handling

```kotlin
val result = gltfLoader.load("model.gltf")

when {
    result.isSuccess -> {
        val gltf = result.getOrThrow()
        scene.add(gltf.scene)
    }
    else -> {
        val error = result.exceptionOrNull()
        println("Failed to load: ${error?.message}")
        // Fallback: load placeholder model or show error UI
    }
}
```

---

## Example 3: Animation (Rotating Object with AnimationMixer)

This example demonstrates keyframe animation using AnimationMixer, showing how to create and control animations programmatically.

### Complete Code

```kotlin
package io.kreekt.examples

import io.kreekt.core.math.*
import io.kreekt.core.scene.*
import io.kreekt.geometry.*
import io.kreekt.material.*
import io.kreekt.camera.*
import io.kreekt.renderer.*
import io.kreekt.animation.*
import kotlinx.coroutines.*

/**
 * Animation Example
 *
 * Demonstrates:
 * - Creating animation clips programmatically
 * - AnimationMixer and AnimationAction
 * - Keyframe tracks (position, rotation, scale)
 * - Animation blending and crossfading
 */
suspend fun main() = coroutineScope {
    // 1. Setup
    val renderer = createRenderer(
        RendererConfiguration(width = 1280, height = 720)
    ).getOrThrow()

    val scene = Scene().apply {
        background = SceneBackground.SolidColor(Color(0x1a1a2e))
    }

    val camera = PerspectiveCamera(60f, 1280f / 720f, 0.1f, 1000f).apply {
        position.set(0f, 5f, 10f)
        lookAt(Vector3(0f, 0f, 0f))
    }

    // 2. Create animated object
    val geometry = TorusKnotGeometry(
        radius = 1f,
        tube = 0.4f,
        tubularSegments = 64,
        radialSegments = 8
    )

    val material = MeshStandardMaterial(
        color = Color(0x00ff88),
        roughness = 0.3f,
        metalness = 0.7f
    )

    val mesh = Mesh(geometry, material)
    scene.add(mesh)

    // 3. Add lighting
    val ambientLight = AmbientLight(Color(0xffffff), 0.4f)
    scene.add(ambientLight)

    val directionalLight = DirectionalLight(Color(0xffffff), 0.8f).apply {
        position.set(5f, 10f, 5f)
    }
    scene.add(directionalLight)

    // 4. Create animation clip - Rotation animation
    val rotationClip = AnimationClip(
        name = "rotation",
        duration = 4f,  // 4 seconds
        tracks = listOf(
            QuaternionKeyframeTrack(
                name = ".quaternion",
                times = floatArrayOf(0f, 2f, 4f),
                values = floatArrayOf(
                    // Start: no rotation
                    0f, 0f, 0f, 1f,
                    // Middle: 180 degrees around Y
                    0f, 0.707f, 0f, 0.707f,
                    // End: 360 degrees around Y (full rotation)
                    0f, 1f, 0f, 0f
                ),
                interpolation = InterpolationMode.Linear
            )
        )
    )

    // 5. Create animation clip - Scale pulse
    val scaleClip = AnimationClip(
        name = "scale_pulse",
        duration = 2f,
        tracks = listOf(
            VectorKeyframeTrack(
                name = ".scale",
                times = floatArrayOf(0f, 1f, 2f),
                values = floatArrayOf(
                    // Start: normal scale
                    1f, 1f, 1f,
                    // Middle: enlarged
                    1.5f, 1.5f, 1.5f,
                    // End: back to normal
                    1f, 1f, 1f
                ),
                interpolation = InterpolationMode.Linear
            )
        )
    )

    // 6. Create animation clip - Position orbit
    val positionClip = AnimationClip(
        name = "orbit",
        duration = 8f,
        tracks = listOf(
            VectorKeyframeTrack(
                name = ".position",
                times = floatArrayOf(0f, 2f, 4f, 6f, 8f),
                values = floatArrayOf(
                    // Start
                    0f, 0f, 0f,
                    // Right
                    3f, 0f, 0f,
                    // Back
                    0f, 0f, -3f,
                    // Left
                    -3f, 0f, 0f,
                    // Return to start
                    0f, 0f, 0f
                ),
                interpolation = InterpolationMode.Cubic
            )
        )
    )

    // 7. Setup animation mixer
    val mixer = AnimationMixer(mesh)

    // 8. Create actions and configure playback
    val rotationAction = mixer.clipAction(rotationClip).apply {
        loop = LoopMode.Repeat
        play()
    }

    val scaleAction = mixer.clipAction(scaleClip).apply {
        loop = LoopMode.Repeat
        timeScale = 2f  // Play twice as fast
        weight = 0f     // Start disabled
    }

    val orbitAction = mixer.clipAction(positionClip).apply {
        loop = LoopMode.Repeat
        weight = 0f     // Start disabled
    }

    // 9. UI state for animation control
    var currentAnimation = 0  // 0: rotation, 1: scale, 2: orbit
    val animationNames = listOf("Rotation", "Scale Pulse", "Orbit")

    println("\nAnimation Controls:")
    println("Current: ${animationNames[currentAnimation]}")
    println("(Simulated - in real app, use keyboard/UI)")

    // 10. Animation switching logic (simulated with timer)
    launch {
        var timeCounter = 0f
        while (isActive) {
            delay(5000)  // Switch every 5 seconds
            timeCounter += 5f

            // Crossfade to next animation
            val nextAnimation = ((currentAnimation + 1) % 3)
            println("\nCrossfading to: ${animationNames[nextAnimation]}")

            when (currentAnimation) {
                0 -> rotationAction.crossFadeTo(
                    when (nextAnimation) {
                        1 -> scaleAction
                        2 -> orbitAction
                        else -> rotationAction
                    },
                    duration = 1f,
                    warp = true
                )
                1 -> scaleAction.crossFadeTo(
                    when (nextAnimation) {
                        0 -> rotationAction
                        2 -> orbitAction
                        else -> scaleAction
                    },
                    duration = 1f,
                    warp = true
                )
                2 -> orbitAction.crossFadeTo(
                    when (nextAnimation) {
                        0 -> rotationAction
                        1 -> scaleAction
                        else -> orbitAction
                    },
                    duration = 1f,
                    warp = true
                )
            }

            currentAnimation = nextAnimation
        }
    }

    // 11. Render loop
    val clock = Clock()

    launch {
        while (isActive) {
            val deltaTime = clock.getDelta()

            // Update animations
            mixer.update(deltaTime)

            // Render
            renderer.render(scene, camera)
            delay(16L)
        }
    }

    // 12. Cleanup
    renderer.onDispose = {
        geometry.dispose()
        material.dispose()
    }
}
```

### Key Concepts

#### 1. Animation Clip Creation
```kotlin
val clip = AnimationClip(
    name = "rotation",
    duration = 4f,
    tracks = listOf(
        QuaternionKeyframeTrack(
            name = ".quaternion",
            times = floatArrayOf(0f, 2f, 4f),
            values = floatArrayOf(/* quaternion values */),
            interpolation = InterpolationMode.Linear
        )
    )
)
```
- Keyframe tracks define animated properties
- Multiple tracks can animate different properties
- Interpolation modes: Linear, Cubic, Discrete, Slerp

#### 2. Animation Mixer
```kotlin
val mixer = AnimationMixer(mesh)
val action = mixer.clipAction(clip)
action.play()

// In render loop
mixer.update(deltaTime)
```
- Manages multiple animation clips
- Blends animations with weights
- Time-scaled playback

#### 3. Animation Action Control
```kotlin
action.apply {
    loop = LoopMode.Repeat  // Once, Repeat, PingPong
    timeScale = 2f          // Playback speed multiplier
    weight = 1f             // Blend weight (0-1)
    play()
}
```
- Play, pause, stop, reset controls
- Fade in/out for smooth transitions
- Crossfading between animations

#### 4. Crossfading
```kotlin
action1.crossFadeTo(action2, duration = 1f, warp = true)
```
- Smooth transition between animations
- Warp adjusts time scales during transition
- Automatic weight management

### Expected Output

Running this example shows:
- Torus knot geometry with metallic material
- Smooth rotation animation (4 seconds loop)
- Automatic crossfade to scale pulse (every 5 seconds)
- Then crossfade to orbital motion
- Console logs showing animation switches

---

## Example 4: Post-Processing (Bloom Effect)

This example demonstrates post-processing effects using EffectComposer, adding a bloom effect to enhance the scene's appearance.

### Complete Code

```kotlin
package io.kreekt.examples

import io.kreekt.core.math.*
import io.kreekt.core.scene.*
import io.kreekt.geometry.*
import io.kreekt.material.*
import io.kreekt.camera.*
import io.kreekt.renderer.*
import io.kreekt.light.*
import io.kreekt.postprocessing.*
import kotlinx.coroutines.*

/**
 * Post-Processing Example
 *
 * Demonstrates:
 * - EffectComposer setup
 * - UnrealBloom pass for glow effects
 * - Render pass configuration
 * - Multiple passes with different parameters
 */
suspend fun main() = coroutineScope {
    // 1. Setup renderer
    val renderer = createRenderer(
        RendererConfiguration(
            width = 1920,
            height = 1080,
            title = "KreeKt Post-Processing",
            antialias = false  // Post-processing AA is better
        )
    ).getOrThrow()

    val scene = Scene().apply {
        background = SceneBackground.SolidColor(Color(0x000000))
    }

    val camera = PerspectiveCamera(75f, 1920f / 1080f, 0.1f, 1000f).apply {
        position.z = 10f
    }

    // 2. Create multiple emissive objects for bloom effect
    val geometries = listOf(
        SphereGeometry(radius = 1f),
        BoxGeometry(1.5f, 1.5f, 1.5f),
        TorusGeometry(radius = 1f, tube = 0.4f)
    )

    val materials = listOf(
        MeshStandardMaterial(
            color = Color(0x0000ff),
            emissive = Color(0x0000ff),
            emissiveIntensity = 2f
        ),
        MeshStandardMaterial(
            color = Color(0xff0000),
            emissive = Color(0xff0000),
            emissiveIntensity = 2f
        ),
        MeshStandardMaterial(
            color = Color(0x00ff00),
            emissive = Color(0x00ff00),
            emissiveIntensity = 2f
        )
    )

    val meshes = geometries.zip(materials).mapIndexed { index, (geom, mat) ->
        Mesh(geom, mat).apply {
            position.x = (index - 1) * 4f
            name = "emissive_mesh_$index"
        }
    }

    meshes.forEach { scene.add(it) }

    // 3. Add subtle ambient light
    val ambientLight = AmbientLight(Color(0xffffff), 0.1f)
    scene.add(ambientLight)

    // 4. Setup post-processing
    val composer = EffectComposer(renderer)

    // 5. Add render pass (renders scene to texture)
    val renderPass = RenderPass(scene, camera)
    composer.addPass(renderPass)

    // 6. Add unreal bloom pass
    val bloomPass = UnrealBloomPass(
        resolution = Vector2(1920f, 1080f),
        strength = 1.5f,      // Bloom intensity
        radius = 0.4f,        // Bloom spread
        threshold = 0.0f      // Luminance threshold (0 = bloom everything)
    )
    composer.addPass(bloomPass)

    // 7. Optional: Add FXAA antialiasing pass
    val fxaaPass = FXAAPass()
    composer.addPass(fxaaPass)

    // 8. Optional: Add output pass (gamma correction)
    val outputPass = OutputPass()
    composer.addPass(outputPass)

    // 9. Animation state
    var bloomStrength = 1.5f
    var increasing = true

    println("\nPost-Processing Setup:")
    println("  - UnrealBloom: strength=${bloomPass.strength}, radius=${bloomPass.radius}")
    println("  - FXAA antialiasing enabled")
    println("  - Bloom strength will pulse between 0.5 and 3.0")

    // 10. Render loop with dynamic bloom
    val clock = Clock()

    launch {
        while (isActive) {
            val deltaTime = clock.getDelta()

            // Animate meshes
            meshes.forEachIndexed { index, mesh ->
                mesh.rotation.y += deltaTime * (0.5f + index * 0.2f)
                mesh.rotation.x += deltaTime * 0.3f
            }

            // Pulse bloom strength
            if (increasing) {
                bloomStrength += deltaTime * 0.5f
                if (bloomStrength >= 3.0f) {
                    bloomStrength = 3.0f
                    increasing = false
                }
            } else {
                bloomStrength -= deltaTime * 0.5f
                if (bloomStrength <= 0.5f) {
                    bloomStrength = 0.5f
                    increasing = true
                }
            }
            bloomPass.strength = bloomStrength

            // Render with post-processing
            composer.render(deltaTime)

            delay(16L)
        }
    }

    // 11. Cleanup
    renderer.onDispose = {
        meshes.forEach { mesh ->
            mesh.geometry.dispose()
            mesh.material.dispose()
        }
        composer.dispose()
        println("Post-processing resources disposed")
    }
}
```

### Key Concepts

#### 1. EffectComposer
```kotlin
val composer = EffectComposer(renderer)
composer.addPass(renderPass)
composer.addPass(bloomPass)
composer.addPass(outputPass)
```
- Manages multi-pass rendering pipeline
- Each pass processes previous pass's output
- Final pass renders to screen

#### 2. Render Pass
```kotlin
val renderPass = RenderPass(scene, camera)
```
- First pass: renders scene to texture
- Outputs color buffer for subsequent passes

#### 3. UnrealBloom Pass
```kotlin
val bloomPass = UnrealBloomPass(
    resolution = Vector2(1920f, 1080f),
    strength = 1.5f,    // Glow intensity
    radius = 0.4f,      // Blur radius
    threshold = 0.0f    // Luminance cutoff
)
```
- High-quality bloom effect
- Blurs bright areas
- Threshold controls which pixels bloom

#### 4. FXAA Pass
```kotlin
val fxaaPass = FXAAPass()
```
- Fast approximate antialiasing
- Post-process edge smoothing
- Lower performance cost than MSAA

#### 5. Dynamic Pass Parameters
```kotlin
bloomPass.strength = 2.0f  // Update in render loop
bloomPass.threshold = 0.5f // Only bloom bright pixels
```
- Pass parameters can be animated
- Enables reactive effects

### Expected Output

Running this example shows:
- Three rotating shapes (sphere, cube, torus)
- Bright emissive colors (blue, red, green)
- Glowing bloom effect around each object
- Bloom strength pulsing smoothly
- Antialiased edges

### Performance Notes

Post-processing adds render cost:
- UnrealBloom: ~3-5ms (1080p)
- FXAA: ~1-2ms
- Total: ~5-7ms per frame

Optimizations:
- Reduce resolution for bloom pass
- Adjust radius (smaller = faster)
- Disable on low-end hardware

---

## Common Patterns

### 1. Resource Management

```kotlin
// Automatic disposal with use
fun renderScene() {
    val geometry = BoxGeometry(1f, 1f, 1f).use { geom ->
        val material = MeshStandardMaterial().use { mat ->
            val mesh = Mesh(geom, mat)
            // ... use mesh ...
        }
    }
}  // Resources disposed automatically

// Manual disposal
val geometry = BoxGeometry()
try {
    // ... use geometry ...
} finally {
    geometry.dispose()
}
```

### 2. Error Handling

```kotlin
// Result type for operations
val result = loader.load("model.gltf")
when {
    result.isSuccess -> {
        val model = result.getOrThrow()
        scene.add(model)
    }
    result.isFailure -> {
        val error = result.exceptionOrNull()
        logger.error("Load failed: ${error?.message}")
        // Handle error (show placeholder, retry, etc.)
    }
}
```

### 3. Coroutine Patterns

```kotlin
// Parallel asset loading
suspend fun loadAssets(): Assets = coroutineScope {
    val modelDeferred = async { loader.loadModel("model.gltf") }
    val textureDeferred = async { loader.loadTexture("texture.png") }

    Assets(
        model = modelDeferred.await().getOrThrow(),
        texture = textureDeferred.await().getOrThrow()
    )
}

// Render loop with cancellation
val renderJob = launch {
    while (isActive) {
        renderer.render(scene, camera)
        delay(16L)
    }
}

// Cancel on window close
window.onClose = { renderJob.cancel() }
```

### 4. DSL Builders

```kotlin
// Scene DSL
val scene = scene {
    background = color(0x1a1a1a)

    mesh {
        geometry = boxGeometry(1f, 1f, 1f)
        material = meshStandardMaterial {
            color = color(0xff0000)
            roughness = 0.5f
        }
        position.set(0f, 1f, 0f)
    }

    directionalLight {
        intensity = 0.8f
        position.set(5f, 10f, 5f)
        castShadow = true
    }
}
```

---

## Next Steps

### Learning Resources

1. **API Documentation**: Browse Dokka-generated API docs at `docs/api/`
2. **Examples**: Explore more examples in `examples/` directory
3. **Tests**: Review contract tests in `src/commonTest/` for API usage patterns

### Advanced Topics

- **Skeletal Animation**: Character rigging and inverse kinematics
- **Custom Shaders**: WGSL shader development
- **XR Development**: VR/AR scene creation
- **Physics Integration**: Rigid body dynamics with Rapier
- **Optimization**: LOD, instancing, frustum culling

### Platform-Specific Setup

#### JVM (Desktop)
```kotlin
// build.gradle.kts
kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }
}

dependencies {
    implementation("org.lwjgl:lwjgl:3.3.3")
    implementation("org.lwjgl:lwjgl-vulkan:3.3.3")
}
```

#### JavaScript (Web)
```kotlin
// build.gradle.kts
kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport { enabled.set(true) }
            }
        }
        binaries.executable()
    }
}
```

#### Native (Linux/macOS/Windows)
```kotlin
// build.gradle.kts
kotlin {
    linuxX64()
    mingwX64()
    macosX64()
    macosArm64()
}
```

---

## Troubleshooting

### Common Issues

**Issue**: Black screen / nothing renders
```kotlin
// Solution: Check camera position
camera.position.z = 5f  // Must be outside near/far planes

// Check object is in view
println("Camera: ${camera.position}")
println("Object: ${mesh.position}")
```

**Issue**: Model doesn't load
```kotlin
// Solution: Check file path and format
try {
    val gltf = loader.load("models/model.gltf").getOrThrow()
} catch (e: Exception) {
    println("Load error: ${e.message}")
    // Check: File exists? Correct format? Network accessible?
}
```

**Issue**: Poor performance
```kotlin
// Solution: Enable performance monitoring
val monitor = PerformanceMonitor()
renderer.onBeforeRender = {
    monitor.begin()
}
renderer.onAfterRender = {
    monitor.end()
    if (monitor.fps < 60f) {
        println("Low FPS: ${monitor.fps}, Frame time: ${monitor.frameTime}ms")
    }
}
```

---

## Summary

These four examples demonstrate core KreeKt functionality:

1. **Hello Cube**: Basic scene setup and rendering
2. **Asset Loading**: GLTF model loading with progress tracking
3. **Animation**: Keyframe animation with AnimationMixer
4. **Post-Processing**: Bloom effect with EffectComposer

**Key Takeaways**:
- KreeKt API mirrors Three.js for easy migration
- Kotlin idioms (coroutines, DSLs, type safety) enhance developer experience
- Platform abstraction enables cross-platform 3D graphics
- Performance targets (60 FPS) are achievable with proper resource management

**Next**: Explore contract files in `specs/012-complete-three-js/contracts/` for complete API reference.

---

**Generated**: 2025-10-01
**Branch**: `012-complete-three-js`
**Specification**: `/home/yousef/Projects/kmp/KreeKt/specs/012-complete-three-js/spec.md`
