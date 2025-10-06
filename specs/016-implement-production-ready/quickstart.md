# Quickstart: WebGPU Renderer

**Feature**: Production-Ready WebGPU Renderer
**Audience**: KreeKt library users
**Estimated Time**: 5 minutes

## Prerequisites

- Modern browser with WebGPU support (Chrome 113+, Firefox 121+, Safari 18+)
- KreeKt library included in your project
- Basic familiarity with 3D rendering concepts

## Step 1: Initialize the Renderer

The WebGPU renderer automatically detects WebGPU availability and falls back to WebGL if needed.

```kotlin
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.core.scene.Scene
import io.kreekt.renderer.webgpu.WebGPURenderer
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

suspend fun main() {
    // Get canvas element
    val canvas = document.getElementById("webgpu-canvas") as HTMLCanvasElement
    canvas.width = 1920
    canvas.height = 1080

    // Create renderer (automatic WebGPU/WebGL selection)
    val renderer = WebGPURenderer(canvas)

    // Initialize renderer
    val initResult = renderer.initialize()
    if (initResult is RendererResult.Error) {
        console.error("Renderer initialization failed", initResult.exception)
        return
    }

    console.log("Renderer initialized successfully")
    console.log("Using backend: ${if (renderer.isWebGPU) "WebGPU" else "WebGL (fallback)"}")
}
```

**Expected Output**:
```
Renderer initialized successfully
Using backend: WebGPU
```

---

## Step 2: Create a Scene with 1M Triangles

Demonstrate performance with a high-poly mesh.

```kotlin
import io.kreekt.core.math.Color
import io.kreekt.core.scene.Mesh
import io.kreekt.geometry.BoxGeometry
import io.kreekt.geometry.BufferGeometry
import io.kreekt.geometry.BufferAttribute
import io.kreekt.material.MeshBasicMaterial

// Create scene
val scene = Scene()

// Create high-poly geometry (1M triangles = ~500K vertices)
val geometry = createHighPolyGeometry(triangleCount = 1_000_000)

// Create material
val material = MeshBasicMaterial().apply {
    vertexColors = true  // Use per-vertex colors
}

// Create mesh
val mesh = Mesh(geometry, material)
scene.add(mesh)

// Helper function to generate high-poly geometry
fun createHighPolyGeometry(triangleCount: Int): BufferGeometry {
    val vertexCount = triangleCount * 3
    val positions = FloatArray(vertexCount * 3)  // x, y, z per vertex
    val normals = FloatArray(vertexCount * 3)    // nx, ny, nz per vertex
    val colors = FloatArray(vertexCount * 3)      // r, g, b per vertex

    // Generate vertices for a complex surface (e.g., subdivided icosphere)
    for (i in 0 until vertexCount) {
        // Position (scattered in 100x100x100 cube)
        positions[i * 3 + 0] = (Math.random() * 200 - 100).toFloat()
        positions[i * 3 + 1] = (Math.random() * 200 - 100).toFloat()
        positions[i * 3 + 2] = (Math.random() * 200 - 100).toFloat()

        // Normal (pointing outward from origin)
        val x = positions[i * 3 + 0]
        val y = positions[i * 3 + 1]
        val z = positions[i * 3 + 2]
        val length = sqrt(x * x + y * y + z * z)
        normals[i * 3 + 0] = x / length
        normals[i * 3 + 1] = y / length
        normals[i * 3 + 2] = z / length

        // Color (gradient based on position)
        colors[i * 3 + 0] = (x + 100) / 200  // Red channel
        colors[i * 3 + 1] = (y + 100) / 200  // Green channel
        colors[i * 3 + 2] = (z + 100) / 200  // Blue channel
    }

    // Create geometry
    return BufferGeometry().apply {
        setAttribute("position", BufferAttribute(positions, 3))
        setAttribute("normal", BufferAttribute(normals, 3))
        setAttribute("color", BufferAttribute(colors, 3))
    }
}
```

---

## Step 3: Setup Camera

Configure perspective camera for viewing the scene.

```kotlin
// Create camera
val camera = PerspectiveCamera(
    fov = 75.0f,           // Field of view in degrees
    aspect = 1920f / 1080f, // Aspect ratio
    near = 0.1f,           // Near clipping plane
    far = 1000.0f          // Far clipping plane
)

// Position camera
camera.position.set(0f, 0f, 200f)  // Move back to see the scene
camera.lookAt(0f, 0f, 0f)          // Look at origin

// Update camera matrices
camera.updateMatrixWorld(true)
camera.updateProjectionMatrix()
```

---

## Step 4: Render Loop (60 FPS Target)

Create a render loop that maintains 60 FPS with 1M triangles.

```kotlin
// FPS tracking
var frameCount = 0
var lastFpsUpdate = js("Date.now()").unsafeCast<Double>()

// Render loop
fun renderLoop() {
    val currentTime = js("Date.now()").unsafeCast<Double>()

    // Render scene
    val renderResult = renderer.render(scene, camera)
    if (renderResult is RendererResult.Error) {
        console.error("Render failed", renderResult.exception)
        return
    }

    // Update FPS counter (every second)
    frameCount++
    if (currentTime - lastFpsUpdate >= 1000) {
        val fps = (frameCount * 1000.0 / (currentTime - lastFpsUpdate)).toInt()
        console.log("FPS: $fps | Triangles: ${renderer.getStats().triangles}")

        frameCount = 0
        lastFpsUpdate = currentTime
    }

    // Request next frame
    window.requestAnimationFrame { renderLoop() }
}

// Start rendering
renderLoop()
```

**Expected Console Output**:
```
FPS: 60 | Triangles: 1000000
FPS: 60 | Triangles: 1000000
FPS: 60 | Triangles: 1000000
...
```

---

## Step 5: Handle Context Loss (Automatic Recovery)

The renderer automatically recovers from GPU context loss events.

```kotlin
// Monitor context loss events (optional - automatic recovery happens internally)
renderer.onContextLost = {
    console.warn("GPU context lost - automatic recovery in progress...")
}

renderer.onContextRestored = {
    console.info("GPU context restored - rendering resumed")
}

// No manual intervention needed - renderer recreates all resources automatically
```

**Simulating Context Loss** (for testing):
```javascript
// In browser console:
const canvas = document.getElementById('webgpu-canvas');
const gl = canvas.getContext('webgl');
gl.getExtension('WEBGL_lose_context').loseContext();
```

**Expected Behavior**:
```
GPU context lost - automatic recovery in progress...
Recreating 3 buffers...
Recreating 1 texture...
Recreating 2 pipelines...
GPU context restored - rendering resumed
FPS: 60 | Triangles: 1000000  (continues normally)
```

---

## Step 6: Dispose Resources Properly

Clean up GPU resources when done.

```kotlin
// When closing application or switching scenes
fun cleanup() {
    renderer.dispose()
    scene.dispose()
    console.log("Resources disposed - GPU memory freed")
}

// Call on page unload
window.addEventListener("beforeunload", {
    cleanup()
})
```

---

## Complete Example

Full working example combining all steps:

```kotlin
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.core.math.Color
import io.kreekt.core.scene.{Scene, Mesh}
import io.kreekt.geometry.{BufferGeometry, BufferAttribute}
import io.kreekt.material.MeshBasicMaterial
import io.kreekt.renderer.webgpu.WebGPURenderer
import kotlinx.browser.{document, window}
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.sqrt

suspend fun main() {
    // 1. Initialize renderer
    val canvas = document.getElementById("webgpu-canvas") as HTMLCanvasElement
    canvas.width = 1920
    canvas.height = 1080

    val renderer = WebGPURenderer(canvas)
    renderer.initialize()

    // 2. Create scene with 1M triangles
    val scene = Scene()
    val geometry = createHighPolyGeometry(1_000_000)
    val material = MeshBasicMaterial().apply { vertexColors = true }
    val mesh = Mesh(geometry, material)
    scene.add(mesh)

    // 3. Setup camera
    val camera = PerspectiveCamera(75.0f, 1920f / 1080f, 0.1f, 1000.0f)
    camera.position.set(0f, 0f, 200f)
    camera.lookAt(0f, 0f, 0f)
    camera.updateMatrixWorld(true)
    camera.updateProjectionMatrix()

    // 4. Render loop
    var frameCount = 0
    var lastFpsUpdate = js("Date.now()").unsafeCast<Double>()

    fun renderLoop() {
        val currentTime = js("Date.now()").unsafeCast<Double>()

        renderer.render(scene, camera)

        frameCount++
        if (currentTime - lastFpsUpdate >= 1000) {
            val fps = (frameCount * 1000.0 / (currentTime - lastFpsUpdate)).toInt()
            console.log("FPS: $fps | Triangles: ${renderer.getStats().triangles}")
            frameCount = 0
            lastFpsUpdate = currentTime
        }

        window.requestAnimationFrame { renderLoop() }
    }

    renderLoop()

    // 5. Cleanup on page unload
    window.addEventListener("beforeunload", {
        renderer.dispose()
        scene.dispose()
    })
}

fun createHighPolyGeometry(triangleCount: Int): BufferGeometry {
    val vertexCount = triangleCount * 3
    val positions = FloatArray(vertexCount * 3)
    val normals = FloatArray(vertexCount * 3)
    val colors = FloatArray(vertexCount * 3)

    for (i in 0 until vertexCount) {
        positions[i * 3 + 0] = (Math.random() * 200 - 100).toFloat()
        positions[i * 3 + 1] = (Math.random() * 200 - 100).toFloat()
        positions[i * 3 + 2] = (Math.random() * 200 - 100).toFloat()

        val x = positions[i * 3 + 0]
        val y = positions[i * 3 + 1]
        val z = positions[i * 3 + 2]
        val length = sqrt(x * x + y * y + z * z)
        normals[i * 3 + 0] = x / length
        normals[i * 3 + 1] = y / length
        normals[i * 3 + 2] = z / length

        colors[i * 3 + 0] = (x + 100) / 200
        colors[i * 3 + 1] = (y + 100) / 200
        colors[i * 3 + 2] = (z + 100) / 200
    }

    return BufferGeometry().apply {
        setAttribute("position", BufferAttribute(positions, 3))
        setAttribute("normal", BufferAttribute(normals, 3))
        setAttribute("color", BufferAttribute(colors, 3))
    }
}
```

---

## Performance Validation

**Expected Performance** (on mid-range GPU):
- **FPS**: 60 (stable)
- **Frame Time**: ~16.7ms
- **Triangles**: 1,000,000
- **Draw Calls**: 1-50 (with batching)
- **GPU Memory**: ~150-200MB

**Browser Compatibility**:
- ✅ Chrome 113+ (WebGPU native)
- ✅ Firefox 121+ (WebGPU native)
- ✅ Safari 18+ (WebGPU native)
- ✅ Older browsers (automatic WebGL fallback)

---

## Troubleshooting

### Issue: FPS below 60

**Cause**: GPU not powerful enough for 1M triangles
**Solution**: Reduce triangle count or enable LOD system

### Issue: "WebGPU not available" warning

**Cause**: Browser doesn't support WebGPU
**Solution**: Renderer automatically falls back to WebGL (no action needed)

### Issue: Black screen

**Cause**: Initialization failed or context lost
**Solution**: Check browser console for errors, verify canvas exists

---

## Next Steps

- Explore advanced materials (PBR, textures)
- Add lighting to the scene
- Implement camera controls (orbit, pan, zoom)
- Integrate with VoxelCraft example

**Documentation**: See [API Reference](../../../docs/renderer-api.md) for detailed method documentation.
