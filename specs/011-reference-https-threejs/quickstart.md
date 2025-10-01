# Three.js Feature Parity - Quickstart Guide

This guide demonstrates how to use the new Three.js-equivalent features implemented in KreeKt. All 25 feature categories from Three.js r180 are now available.

## Table of Contents

1. [Audio System](#1-audio-system)
2. [Helper Objects](#2-helper-objects)
3. [Advanced Cameras](#3-advanced-cameras)
4. [Fog System](#4-fog-system)
5. [Raycasting](#5-raycasting)
6. [Curve System](#6-curve-system)
7. [Advanced Textures](#7-advanced-textures)
8. [Instancing](#8-instancing)
9. [Points & Sprites](#9-points--sprites)
10. [Morph Targets](#10-morph-targets)
11. [Clipping Planes](#11-clipping-planes)
12. [Layers](#12-layers)
13. [Render Targets](#13-render-targets)
14. [Shape & Path](#14-shape--path)
15. [Enhanced Line Rendering](#15-enhanced-line-rendering)
16. [Level of Detail (LOD)](#16-level-of-detail-lod)

---

## 1. Audio System

### Positional 3D Audio

```kotlin
import io.kreekt.audio.*
import io.kreekt.camera.PerspectiveCamera

// Create audio listener attached to camera
val camera = PerspectiveCamera()
val listener = AudioListener(camera)

// Create positional audio source
val audio = PositionalAudio(listener)
audio.load("sounds/ambient.mp3")

// Configure 3D positioning
audio.refDistance = 20f
audio.maxDistance = 100f
audio.rolloffFactor = 1f

// Configure directional cone
audio.setDirectionalCone(
    innerAngle = Math.PI.toFloat() / 4,
    outerAngle = Math.PI.toFloat() / 2,
    outerGain = 0.5f
)

// Play audio
audio.play()
```

### Audio Analysis

```kotlin
// Create audio analyser
val analyser = AudioAnalyser(audio, fftSize = 2048)

// Get frequency data in render loop
val frequencyData = analyser.getFrequencyData()
val averageFrequency = analyser.getAverageFrequency()

// Use for visualizations
val bass = frequencyData.slice(0..10).average()
val treble = frequencyData.slice(100..150).average()
```

---

## 2. Helper Objects

### Debugging Visualization

```kotlin
import io.kreekt.helper.*

// Add world axes
val axesHelper = AxesHelper(size = 5f)
scene.add(axesHelper)

// Add ground grid
val gridHelper = GridHelper(
    size = 100f,
    divisions = 100,
    colorCenterLine = Color(0x444444),
    colorGrid = Color(0x888888)
)
scene.add(gridHelper)

// Visualize object bounding box
val boxHelper = BoxHelper(mesh, color = Color(0xffff00))
scene.add(boxHelper)

// Update on object changes
boxHelper.update()

// Visualize camera frustum
val cameraHelper = CameraHelper(camera)
scene.add(cameraHelper)

// Visualize directional light
val lightHelper = DirectionalLightHelper(directionalLight, size = 5f)
scene.add(lightHelper)
```

---

## 3. Advanced Cameras

### Cube Camera for Environment Mapping

```kotlin
import io.kreekt.camera.*
import io.kreekt.renderer.WebGPUCubeRenderTarget

// Create cube render target
val cubeRenderTarget = WebGPUCubeRenderTarget(size = 512)

// Create cube camera
val cubeCamera = CubeCamera(
    near = 0.1f,
    far = 1000f,
    renderTarget = cubeRenderTarget
)
scene.add(cubeCamera)

// Update environment map
cubeCamera.update(renderer, scene)

// Use in PBR material
material.envMap = cubeRenderTarget.texture
```

### Stereo Camera for VR

```kotlin
// Create stereo camera
val stereoCamera = StereoCamera()
stereoCamera.eyeSep = 0.064f  // 64mm inter-pupillary distance

// Update from main camera
stereoCamera.update(mainCamera)

// Render left and right eye views
renderer.setViewport(0, 0, width / 2, height)
renderer.render(scene, stereoCamera.cameraL)

renderer.setViewport(width / 2, 0, width / 2, height)
renderer.render(scene, stereoCamera.cameraR)
```

### Array Camera for Multi-View

```kotlin
// Create array camera with multiple viewports
val arrayCamera = ArrayCamera()

val camera1 = PerspectiveCamera(fov = 50f)
arrayCamera.addCamera(camera1, x = 0f, y = 0f, width = 0.5f, height = 1f)

val camera2 = PerspectiveCamera(fov = 50f)
arrayCamera.addCamera(camera2, x = 0.5f, y = 0f, width = 0.5f, height = 1f)

// Render splits screen between cameras
renderer.render(scene, arrayCamera)
```

---

## 4. Fog System

### Linear Fog

```kotlin
import io.kreekt.fog.*

// Create linear fog
val fog = Fog(
    color = Color(0xffffff),
    near = 10f,
    far = 100f
)

// Attach to scene
scene.fog = fog

// Fog automatically affects materials with fog=true
```

### Exponential Fog

```kotlin
// Create exponential squared fog (denser)
val fogExp2 = FogExp2(
    color = Color(0xcccccc),
    density = 0.002f
)

scene.fog = fogExp2

// Higher density = thicker fog
fogExp2.density = 0.005f
```

---

## 5. Raycasting

### Object Picking

```kotlin
import io.kreekt.raycaster.*

// Create raycaster
val raycaster = Raycaster()

// Set ray from mouse coordinates (normalized device coordinates)
val mouse = Vector2(mouseX / width * 2 - 1, -(mouseY / height * 2 - 1))
raycaster.setFromCamera(mouse, camera)

// Intersect objects
val intersects = raycaster.intersectObjects(scene.children, recursive = true)

if (intersects.isNotEmpty()) {
    val closest = intersects.first()
    println("Hit: ${closest.`object`.name} at distance ${closest.distance}")
    println("Point: ${closest.point}")
    println("UV: ${closest.uv}")

    // Change color on hover
    closest.`object`.material.color.set(0xff0000)
}
```

### Layer Filtering

```kotlin
// Raycast only specific layers
raycaster.layers.set(0)  // Only layer 0
raycaster.layers.enable(1)  // Also layer 1

val intersects = raycaster.intersectObjects(objects, recursive = true)
// Only hits objects on layers 0 or 1
```

---

## 6. Curve System

### Catmull-Rom Spline

```kotlin
import io.kreekt.curve.*

// Define control points
val points = listOf(
    Vector3(0f, 0f, 0f),
    Vector3(10f, 5f, 0f),
    Vector3(20f, 0f, 10f),
    Vector3(30f, -5f, 0f)
)

// Create smooth curve through points
val curve = CatmullRomCurve3(
    points = points,
    closed = false,
    curveType = CurveType.CENTRIPETAL
)

// Get points along curve
val curvePoints = curve.getSpacedPoints(50)

// Get point and tangent at parameter t
val point = curve.getPoint(0.5f)  // Midpoint
val tangent = curve.getTangent(0.5f)
```

### Bézier Curves

```kotlin
// Cubic Bézier
val bezier = CubicBezierCurve3(
    v0 = Vector3(0f, 0f, 0f),
    v1 = Vector3(10f, 10f, 0f),  // Control point 1
    v2 = Vector3(20f, 10f, 0f),  // Control point 2
    v3 = Vector3(30f, 0f, 0f)
)

// Quadratic Bézier
val quadBezier = QuadraticBezierCurve3(
    v0 = Vector3(0f, 0f, 0f),
    v1 = Vector3(10f, 10f, 0f),  // Control point
    v2 = Vector3(20f, 0f, 0f)
)
```

### Tube Geometry from Curve

```kotlin
// Extrude tube along curve
val tubeGeometry = TubeGeometry(
    path = curve,
    tubularSegments = 64,
    radius = 2f,
    radialSegments = 8,
    closed = false
)

val mesh = Mesh(tubeGeometry, material)
scene.add(mesh)
```

### 2D Paths

```kotlin
// Create 2D path
val path = Path()
path.moveTo(0f, 0f)
path.lineTo(10f, 0f)
path.quadraticCurveTo(15f, 5f, 10f, 10f)
path.lineTo(0f, 10f)
path.bezierCurveTo(-5f, 5f, -5f, 5f, 0f, 0f)

// Get 2D points
val points2D = path.getPoints2D(divisions = 12)
```

---

## 7. Advanced Textures

### Cube Texture

```kotlin
import io.kreekt.texture.*

// Load cube texture from 6 images
val loader = CubeTextureLoader()
val cubeTexture = loader.load(arrayOf(
    "textures/cube/px.png",  // Positive X
    "textures/cube/nx.png",  // Negative X
    "textures/cube/py.png",  // Positive Y
    "textures/cube/ny.png",  // Negative Y
    "textures/cube/pz.png",  // Positive Z
    "textures/cube/nz.png"   // Negative Z
))

// Use as environment map
scene.background = cubeTexture
material.envMap = cubeTexture
```

### Video Texture

```kotlin
// Create video element
val video = document.getElementById("video") as VideoElement

// Create video texture
val videoTexture = VideoTexture(video)

// Update in render loop
fun render() {
    videoTexture.update()  // Updates from current video frame
    renderer.render(scene, camera)
}
```

### Canvas Texture

```kotlin
// Create canvas texture for procedural content
val canvas = document.createElement("canvas") as CanvasElement
canvas.width = 512
canvas.height = 512

val context = canvas.getContext("2d")
context.fillStyle = "rgb(255, 0, 0)"
context.fillRect(0, 0, 512, 512)

val canvasTexture = CanvasTexture(canvas)

// Update when canvas changes
context.fillStyle = "rgb(0, 255, 0)"
context.fillRect(100, 100, 300, 300)
canvasTexture.update()
```

### Compressed Textures

```kotlin
// Load compressed texture (KTX2)
val compressedLoader = CompressedTextureLoader()
val compressedTexture = compressedLoader.loadAsync("texture.ktx2").await()

material.map = compressedTexture
```

### PMREM Generator for IBL

```kotlin
// Generate pre-filtered environment map
val pmremGenerator = PMREMGenerator(renderer)

// From equirectangular HDR
val hdrTexture = TextureLoader().loadAsync("envmap.hdr").await()
val envMap = pmremGenerator.fromEquirectangular(hdrTexture, samples = 1024)

// Use for image-based lighting
material.envMap = envMap
```

---

## 8. Instancing

### Instanced Mesh

```kotlin
import io.kreekt.instancing.*

// Create instanced mesh for 10,000 cubes
val geometry = BoxGeometry(1f, 1f, 1f)
val material = MeshStandardMaterial(color = Color(0x00ff00))
val instancedMesh = InstancedMesh(geometry, material, count = 10_000)

scene.add(instancedMesh)

// Set transformation for each instance
val matrix = Matrix4()
val color = Color()

for (i in 0 until 10_000) {
    // Random position
    matrix.setPosition(
        Math.random().toFloat() * 100 - 50,
        Math.random().toFloat() * 100 - 50,
        Math.random().toFloat() * 100 - 50
    )
    instancedMesh.setMatrixAt(i, matrix)

    // Random color
    color.setHex((Math.random() * 0xffffff).toInt())
    instancedMesh.setColorAt(i, color)
}

// Mark for GPU update
instancedMesh.instanceMatrix.needsUpdate = true
instancedMesh.instanceColor?.needsUpdate = true
```

### Dynamic Instance Updates

```kotlin
// Update instances in animation loop
fun animate() {
    for (i in 0 until instancedMesh.count) {
        instancedMesh.getMatrixAt(i, matrix)

        // Rotate instance
        matrix.multiply(Matrix4().makeRotationY(0.01f))

        instancedMesh.setMatrixAt(i, matrix)
    }

    instancedMesh.instanceMatrix.needsUpdate = true
}
```

---

## 9. Points & Sprites

### Point Cloud

```kotlin
import io.kreekt.points.*

// Create point cloud from positions
val positions = FloatArray(30_000)  // 10,000 points * 3 (x,y,z)
for (i in 0 until 10_000) {
    positions[i * 3] = Math.random().toFloat() * 100 - 50
    positions[i * 3 + 1] = Math.random().toFloat() * 100 - 50
    positions[i * 3 + 2] = Math.random().toFloat() * 100 - 50
}

val geometry = BufferGeometry()
geometry.setAttribute("position", BufferAttribute(positions, itemSize = 3))

// Point material
val material = PointsMaterial(
    PointsMaterialParameters(
        color = Color(0xff0000),
        size = 0.5f,
        sizeAttenuation = true  // Perspective scaling
    )
)

val points = Points(geometry, material)
scene.add(points)
```

### Sprites (Billboards)

```kotlin
// Create sprite
val spriteMaterial = SpriteMaterial(
    SpriteMaterialParameters(
        map = TextureLoader().load("sprite.png"),
        transparent = true,
        opacity = 1f
    )
)

val sprite = Sprite(spriteMaterial)
sprite.position.set(10f, 5f, 0f)
sprite.scale.set(2f, 2f, 1f)

scene.add(sprite)

// Sprite always faces camera
```

---

## 10. Morph Targets

### Blend Shape Animation

```kotlin
import io.kreekt.morph.*

// Add morph targets to geometry
geometry.addMorphTarget(
    name = "smile",
    positions = smilePositions,
    normals = smileNormals
)

geometry.addMorphTarget(
    name = "frown",
    positions = frownPositions
)

// Set morph target influences
mesh.morphTargetInfluences = FloatArray(2)
mesh.morphTargetInfluences!![0] = 0.5f  // 50% smile
mesh.morphTargetInfluences!![1] = 0.0f  // 0% frown

// Animate influences
fun animate(time: Float) {
    mesh.morphTargetInfluences!![0] = (Math.sin(time) + 1) / 2  // Oscillate 0-1
}

// Or use morph animation mixer
val mixer = MorphAnimationMixer(mesh)
mixer.play(morphAnimation, blendWeight = 1f)
mixer.update(deltaTime)
```

---

## 11. Clipping Planes

### Global Clipping

```kotlin
import io.kreekt.clipping.*

// Create clipping plane
val plane = Plane(
    normal = Vector3(0f, 1f, 0f),  // Clip below Y=0
    constant = 0f
)

// Set global clipping planes
renderer.clippingPlanes = listOf(plane)
renderer.localClippingEnabled = true

// Plane clips all objects
```

### Per-Material Clipping

```kotlin
// Set clipping on specific material
material.clippingPlanes = listOf(
    Plane(Vector3(1f, 0f, 0f), 0f),  // Clip left of X=0
    Plane(Vector3(0f, 1f, 0f), 0f)   // Clip below Y=0
)

material.clipIntersection = false  // Union mode (outside ANY plane)
material.clipShadows = true  // Also clip shadows
```

---

## 12. Layers

### Layer-Based Visibility

```kotlin
import io.kreekt.layers.*

// Set object to layer 1
mesh.layers.set(1)

// Set camera to see layers 0 and 1
camera.layers.set(0)
camera.layers.enable(1)

// Object only visible if camera and object share layers
renderer.render(scene, camera)

// Common layer usage
mesh.layers.set(LayerUtils.CommonLayers.ENVIRONMENT)
gizmo.layers.set(LayerUtils.CommonLayers.GIZMOS)
ui.layers.set(LayerUtils.CommonLayers.UI)
```

### Multi-Layer Objects

```kotlin
// Enable multiple layers
object3D.layers.enableAll()  // Visible to all cameras
object3D.layers.disableAll()  // Invisible to all cameras

object3D.layers.enable(0)
object3D.layers.enable(1)
object3D.layers.enable(2)

// Check visibility
if (object3D.layers.test(camera.layers)) {
    // Object is visible to camera
}
```

---

## 13. Render Targets

### Off-Screen Rendering

```kotlin
import io.kreekt.rendertarget.*

// Create render target
val renderTarget = WebGPURenderTarget(
    width = 1024,
    height = 1024,
    options = RenderTargetOptions(
        format = TextureFormat.RGBA,
        type = TextureDataType.UNSIGNED_BYTE,
        minFilter = TextureFilter.LINEAR,
        magFilter = TextureFilter.LINEAR
    )
)

// Render to target
renderer.setRenderTarget(renderTarget)
renderer.render(scene, camera)

// Use rendered texture
renderer.setRenderTarget(null)  // Back to screen
planeMaterial.map = renderTarget.texture
renderer.render(screenScene, screenCamera)
```

### Multi-Target Rendering (MRT)

```kotlin
// Create MRT with 3 color attachments
val mrt = WebGPUMultipleRenderTargets(
    width = 1024,
    height = 1024,
    count = 3
)

// Shader writes to gl_FragData[0], gl_FragData[1], gl_FragData[2]

// Use each texture separately
val diffuseTexture = mrt.getTexture(0)
val normalTexture = mrt.getTexture(1)
val positionTexture = mrt.getTexture(2)
```

### Render Target Pool

```kotlin
// Use pool for efficient resource management
val pool = RenderTargetPool()

// Acquire from pool (reuses if available)
val target = pool.acquire(1024, 1024)

// Use target...

// Release back to pool
pool.release(target)

// Dispose all at end
pool.dispose()
```

---

## 14. Shape & Path

### 2D Shapes

```kotlin
import io.kreekt.shape.*

// Create circle shape
val circle = Shapes.circle(radius = 5f, segments = 32)

// Create rectangle with rounded corners
val rect = Shapes.roundedRectangle(
    width = 10f,
    height = 6f,
    radius = 0.5f
)

// Create star
val star = Shapes.star(
    points = 5,
    outerRadius = 5f,
    innerRadius = 2f
)

// Create shape with holes
val shape = Shape()
shape.moveTo(0f, 0f)
shape.lineTo(10f, 0f)
shape.lineTo(10f, 10f)
shape.lineTo(0f, 10f)

val hole = Path()
hole.absarc(5f, 5f, 2f, 0f, Math.PI.toFloat() * 2, false)
shape.addHole(hole)
```

### Shape Geometry (2D Extrusion)

```kotlin
// Create flat 2D geometry from shape
val shapeGeometry = ShapeGeometry(shape, curveSegments = 12)

val mesh = Mesh(shapeGeometry, material)
scene.add(mesh)
```

### Extrude Geometry (3D Extrusion)

```kotlin
// Extrude shape to 3D
val extrudeGeometry = ExtrudeGeometry(
    shape = shape,
    options = ExtrudeGeometryOptions(
        depth = 5f,
        bevelEnabled = true,
        bevelThickness = 0.5f,
        bevelSize = 0.3f,
        bevelSegments = 3
    )
)

val mesh = Mesh(extrudeGeometry, material)
scene.add(mesh)
```

### Text Geometry

```kotlin
// Load font
val font = FontLoader().load("fonts/helvetiker_regular.typeface.json").await()

// Create text shapes
val textShapes = Shapes.text("Hello KreeKt!", font, size = 10f)

// Extrude text
val textGeometry = ExtrudeGeometry(
    shapes = textShapes,
    options = ExtrudeGeometryOptions(depth = 2f)
)

val textMesh = Mesh(textGeometry, material)
scene.add(textMesh)
```

---

## 15. Enhanced Line Rendering

### High-Quality Lines (Line2)

```kotlin
import io.kreekt.lines.*

// Create line geometry
val positions = floatArrayOf(
    0f, 0f, 0f,
    10f, 5f, 0f,
    20f, 0f, 10f,
    30f, -5f, 0f
)

val geometry = LineGeometry()
geometry.setPositions(positions)

// Line material with pixel-accurate width
val material = LineMaterial(
    LineMaterialParameters(
        color = Color(0xff0000),
        linewidth = 5f,  // 5 pixels wide
        worldUnits = false,  // Screen-space width
        resolution = Vector2(windowWidth.toFloat(), windowHeight.toFloat())
    )
)

val line = Line2(geometry, material)
scene.add(line)

// Update resolution on window resize
material.resolution.set(newWidth.toFloat(), newHeight.toFloat())
```

### Dashed Lines

```kotlin
// Create dashed line
val dashedMaterial = LineMaterial(
    LineMaterialParameters(
        color = Color(0x0000ff),
        linewidth = 3f,
        dashed = true,
        dashSize = 10f,
        gapSize = 5f,
        dashScale = 1f
    )
)

val dashedLine = Line2(geometry, dashedMaterial)
dashedLine.computeLineDistances()  // Required for dashing
scene.add(dashedLine)
```

---

## 16. Level of Detail (LOD)

### Automatic LOD Switching

```kotlin
import io.kreekt.lod.*

// Create LOD hierarchy
val lod = LOD()

// Add levels (distance thresholds)
lod.addLevel(highDetailMesh, distance = 0f)    // 0-50 units
lod.addLevel(mediumDetailMesh, distance = 50f) // 50-100 units
lod.addLevel(lowDetailMesh, distance = 100f)   // 100+ units

scene.add(lod)

// LOD automatically switches in render loop based on camera distance
// Update is automatic if lod.autoUpdate = true
```

### Generate LOD Hierarchy

```kotlin
// Automatically generate LOD levels
val lod = LODUtils.generateLODChain(
    mesh = highDetailMesh,
    levelCount = 4,
    minDistance = 0f,
    maxDistance = 500f
)

scene.add(lod)

// LOD levels are automatically simplified and assigned distances
```

---

## Complete Example: Interactive Scene

```kotlin
import io.kreekt.*
import io.kreekt.audio.*
import io.kreekt.helper.*
import io.kreekt.fog.*
import io.kreekt.raycaster.*
import io.kreekt.instancing.*
import io.kreekt.layers.*

// Setup
val scene = Scene()
val camera = PerspectiveCamera(fov = 75f, aspect = 16f/9f)
camera.position.set(0f, 10f, 20f)

val renderer = WebGPURenderer()

// Fog
scene.fog = FogExp2(Color(0x000033), density = 0.002f)

// Helpers
scene.add(AxesHelper(5f))
scene.add(GridHelper(100f, 100))

// Instanced objects
val geometry = BoxGeometry(1f, 1f, 1f)
val material = MeshStandardMaterial(color = Color(0x00ff00))
val instancedMesh = InstancedMesh(geometry, material, 1000)

val matrix = Matrix4()
for (i in 0 until 1000) {
    matrix.setPosition(
        Math.random().toFloat() * 100 - 50,
        Math.random().toFloat() * 20,
        Math.random().toFloat() * 100 - 50
    )
    instancedMesh.setMatrixAt(i, matrix)
}
instancedMesh.instanceMatrix.needsUpdate = true
scene.add(instancedMesh)

// Audio
val listener = AudioListener(camera)
val audio = PositionalAudio(listener)
audio.load("ambient.mp3")
audio.play()

// Raycasting
val raycaster = Raycaster()
val mouse = Vector2()

canvas.addEventListener("click") { event ->
    mouse.x = (event.clientX / window.innerWidth) * 2 - 1
    mouse.y = -(event.clientY / window.innerHeight) * 2 + 1

    raycaster.setFromCamera(mouse, camera)
    val intersects = raycaster.intersectObject(instancedMesh)

    if (intersects.isNotEmpty()) {
        val instanceId = intersects[0].instanceId!!
        // Highlight clicked instance
        instancedMesh.setColorAt(instanceId, Color(0xff0000))
        instancedMesh.instanceColor?.needsUpdate = true
    }
}

// Render loop
fun animate() {
    requestAnimationFrame(::animate)

    camera.updateMatrixWorld()
    renderer.render(scene, camera)
}

animate()
```

---

## Migration from Three.js

KreeKt provides near-complete API compatibility with Three.js r180. Migration typically requires only minor syntax changes:

### Three.js → KreeKt

```javascript
// Three.js
const geometry = new THREE.BoxGeometry(1, 1, 1);
const material = new THREE.MeshStandardMaterial({ color: 0x00ff00 });
const mesh = new THREE.Mesh(geometry, material);
scene.add(mesh);
```

```kotlin
// KreeKt
val geometry = BoxGeometry(1f, 1f, 1f)
val material = MeshStandardMaterial(color = Color(0x00ff00))
val mesh = Mesh(geometry, material)
scene.add(mesh)
```

### Key Differences

1. **Constructors**: Named constructors instead of `new`
2. **Numbers**: Use `f` suffix for floats: `1f` instead of `1`
3. **Colors**: `Color(0xff0000)` instead of `0xff0000`
4. **Properties**: Direct access instead of `get()`/`set()` methods where possible
5. **Async**: Use `suspend` functions and `Result<T>` for loading

---

## Performance Tips

1. **Instancing**: Use `InstancedMesh` for 1000+ identical objects
2. **LOD**: Use LOD for distant objects to reduce triangle count
3. **Frustum Culling**: Enabled by default, objects outside view are skipped
4. **Layers**: Use layers to selectively render subsets of scene
5. **Geometry Disposal**: Call `dispose()` on unused geometries and textures
6. **Render Targets**: Use `RenderTargetPool` to reuse render targets

---

## Next Steps

- Explore the [API Reference](../../../docs/api/index.html)
- Check out [Examples](../../../examples/index.html)
- Read the [Migration Guide](migration-guide.md)
- Join the [Community](https://github.com/kreekt/KreeKt/discussions)

---

**KreeKt Three.js Feature Parity - Complete Implementation**

All 25 feature categories from Three.js r180 are now available in KreeKt with full cross-platform support!