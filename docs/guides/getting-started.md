# Getting Started with KreeKt

This guide will help you get started with KreeKt, a Kotlin Multiplatform 3D graphics library.

## Prerequisites

- Kotlin 1.9+
- Gradle 8.0+
- For JVM: Java 17+ with LWJGL 3.3.3 support
- For Web: Modern browser with WebGPU support (or WebGL2 fallback)
- For Android: API level 24+ with Vulkan support
- For iOS: iOS 14+ with MoltenVK support

## Installation

### Gradle (Kotlin DSL)

Add KreeKt to your `build.gradle.kts`:

```kotlin
plugins {
    kotlin("multiplatform") version "1.9.21"
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.kreekt:kreekt-core:1.0.0")
            }
        }
    }
}
```

### Maven

```xml
<dependency>
    <groupId>io.kreekt</groupId>
    <artifactId>kreekt-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Your First Scene

Let's create a simple 3D scene with a rotating cube.

### Step 1: Create a Scene

```kotlin
import io.kreekt.core.scene.Scene
import io.kreekt.camera.PerspectiveCamera

// Create the scene (container for all 3D objects)
val scene = Scene()

// Create a camera
val camera = PerspectiveCamera(
    fov = 75f,                    // Field of view in degrees
    aspect = windowWidth / windowHeight,  // Aspect ratio
    near = 0.1f,                  // Near clipping plane
    far = 1000f                   // Far clipping plane
)
camera.position.z = 5f            // Move camera back to see the scene
```

### Step 2: Create Geometry and Material

```kotlin
import io.kreekt.geometry.BoxGeometry
import io.kreekt.material.MeshStandardMaterial
import io.kreekt.mesh.Mesh
import io.kreekt.core.math.Color

// Create a box geometry (1x1x1 cube)
val geometry = BoxGeometry(1f, 1f, 1f)

// Create a material with a green color
val material = MeshStandardMaterial().apply {
    color = Color(0x00ff00)  // Green
    metalness = 0.5f
    roughness = 0.5f
}

// Combine geometry and material into a mesh
val cube = Mesh(geometry, material)
scene.add(cube)
```

### Step 3: Add Lighting

```kotlin
import io.kreekt.light.DirectionalLight
import io.kreekt.light.AmbientLight

// Add ambient light for overall illumination
val ambientLight = AmbientLight(Color(0x404040), 0.5f)
scene.add(ambientLight)

// Add directional light for shadows
val directionalLight = DirectionalLight(Color(0xffffff), 1.0f)
directionalLight.position.set(5f, 5f, 5f)
scene.add(directionalLight)
```

### Step 4: Initialize Renderer

```kotlin
import io.kreekt.renderer.WebGPURenderer

// Create renderer
val renderer = createRenderer() // Platform-specific factory function

// Set renderer size
renderer.setSize(windowWidth, windowHeight)

// Enable shadows (optional)
renderer.shadowMap.enabled = true
```

### Step 5: Animation Loop

```kotlin
import kotlin.math.PI

var lastTime = 0.0

fun animate(currentTime: Double) {
    val deltaTime = (currentTime - lastTime) / 1000.0
    lastTime = currentTime

    // Rotate the cube
    cube.rotation.x += 0.01f
    cube.rotation.y += 0.01f

    // Render the scene
    renderer.render(scene, camera)

    // Request next frame
    window.requestAnimationFrame(::animate)
}

// Start animation
animate(0.0)
```

## Complete Example

Here's the complete code:

```kotlin
import io.kreekt.core.scene.*
import io.kreekt.core.math.*
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.geometry.BoxGeometry
import io.kreekt.material.MeshStandardMaterial
import io.kreekt.mesh.Mesh
import io.kreekt.light.*

fun main() {
    // Scene setup
    val scene = Scene()

    // Camera setup
    val camera = PerspectiveCamera(
        fov = 75f,
        aspect = 800f / 600f,
        near = 0.1f,
        far = 1000f
    )
    camera.position.z = 5f

    // Create cube
    val geometry = BoxGeometry(1f, 1f, 1f)
    val material = MeshStandardMaterial().apply {
        color = Color(0x00ff00)
        metalness = 0.5f
        roughness = 0.5f
    }
    val cube = Mesh(geometry, material)
    scene.add(cube)

    // Lighting
    val ambientLight = AmbientLight(Color(0x404040), 0.5f)
    scene.add(ambientLight)

    val directionalLight = DirectionalLight(Color(0xffffff), 1.0f)
    directionalLight.position.set(5f, 5f, 5f)
    scene.add(directionalLight)

    // Renderer
    val renderer = createRenderer()
    renderer.setSize(800, 600)

    // Animation loop
    var lastTime = 0.0
    fun animate(currentTime: Double) {
        val deltaTime = (currentTime - lastTime) / 1000.0
        lastTime = currentTime

        cube.rotation.x += 0.01f
        cube.rotation.y += 0.01f

        renderer.render(scene, camera)

        window.requestAnimationFrame(::animate)
    }

    animate(0.0)
}
```

## Platform-Specific Setup

### JVM (Desktop)

```kotlin
// build.gradle.kts
kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("io.kreekt:kreekt-jvm:1.0.0")
                implementation("org.lwjgl:lwjgl:3.3.3")
                implementation("org.lwjgl:lwjgl-vulkan:3.3.3")
            }
        }
    }
}
```

### JavaScript (Web)

```kotlin
// build.gradle.kts
kotlin {
    js(IR) {
        browser {
            webpackTask {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }
}
```

```html
<!-- index.html -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>KreeKt App</title>
</head>
<body>
    <canvas id="canvas"></canvas>
    <script src="app.js"></script>
</body>
</html>
```

### Android

```kotlin
// build.gradle.kts
android {
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
}
```

```xml
<!-- AndroidManifest.xml -->
<manifest>
    <uses-feature android:name="android.hardware.vulkan.level" android:required="true" />
    <uses-feature android:glEsVersion="0x00030000" android:required="true" />
</manifest>
```

## Next Steps

- Read the [Materials Guide](materials-guide.md) to learn about different material types
- Explore the [Animation Guide](animation-guide.md) for character animation
- Check out the [API Reference](../api-reference/README.md) for detailed documentation
- Browse [Examples](../examples/basic-usage.md) for more code samples

## Common Patterns

### Loading Models

```kotlin
import io.kreekt.loader.GLTFLoader

val loader = GLTFLoader()
loader.load("models/character.gltf") { gltf ->
    scene.add(gltf.scene)
}
```

### Camera Controls

```kotlin
import io.kreekt.controls.OrbitControls

val controls = OrbitControls(camera, renderer.domElement)
controls.enableDamping = true
controls.dampingFactor = 0.05f

// In animation loop
fun animate(time: Double) {
    controls.update()
    renderer.render(scene, camera)
}
```

### Handling Window Resize

```kotlin
window.addEventListener("resize") {
    val width = window.innerWidth
    val height = window.innerHeight

    camera.aspect = width / height
    camera.updateProjectionMatrix()

    renderer.setSize(width, height)
}
```

## Troubleshooting

### WebGPU Not Available

If WebGPU is not available in your browser, KreeKt automatically falls back to WebGL2:

```kotlin
if (renderer.capabilities.webgpu) {
    println("Using WebGPU")
} else {
    println("Using WebGL2 fallback")
}
```

### Performance Issues

For large scenes, enable optimization features:

```kotlin
// Enable frustum culling
renderer.frustumCulling = true

// Use LOD (Level of Detail)
import io.kreekt.lod.LOD

val lod = LOD()
lod.addLevel(highDetailMesh, 0f)
lod.addLevel(mediumDetailMesh, 50f)
lod.addLevel(lowDetailMesh, 100f)
scene.add(lod)

// Enable instancing for repeated objects
import io.kreekt.instancing.InstancedMesh

val instancedMesh = InstancedMesh(geometry, material, count = 1000)
scene.add(instancedMesh)
```

## Resources

- [API Documentation](../api-reference/README.md)
- [Examples Repository](https://github.com/your-org/kreekt-examples)
- [Community Discord](https://discord.gg/kreekt)
- [Three.js Migration Guide](migration-from-threejs.md)
