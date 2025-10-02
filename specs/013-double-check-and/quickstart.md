# Quickstart: Migrating Three.js r180 Features to KreeKt

**Feature**: 013-double-check-and
**Date**: 2025-10-01
**Audience**: Developers migrating from Three.js to KreeKt

## Overview

This guide provides practical examples for migrating Three.js r180 features to KreeKt. All examples demonstrate the
Kotlin Multiplatform API while maintaining Three.js compatibility patterns.

---

## Post-Processing Pipeline

### Three.js (JavaScript)

```javascript
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/examples/jsm/postprocessing/OutputPass.js';

// Create composer
const composer = new EffectComposer(renderer);

// Add render pass
const renderPass = new RenderPass(scene, camera);
composer.addPass(renderPass);

// Add bloom effect
const bloomPass = new UnrealBloomPass(
    new THREE.Vector2(window.innerWidth, window.innerHeight),
    1.5,  // strength
    0.4,  // radius
    0.85  // threshold
);
composer.addPass(bloomPass);

// Add output pass
const outputPass = new OutputPass();
composer.addPass(outputPass);

// In render loop
composer.render(deltaTime);
```

### KreeKt (Kotlin)

```kotlin
import io.kreekt.postprocessing.*
import io.kreekt.core.math.Vector2

// Create composer
val composer = EffectComposer(renderer)

// Add render pass
val renderPass = RenderPass(scene, camera)
composer.addPass(renderPass)

// Add bloom effect
val bloomPass = UnrealBloomPass(
    strength = 1.5f,
    radius = 0.4f,
    threshold = 0.85f
)
composer.addPass(bloomPass)

// Add output pass
val outputPass = OutputPass()
composer.addPass(outputPass)

// In render loop
composer.render(deltaTime = 0.016f)
```

**Migration Notes**:

- Size is automatically inferred from renderer
- Float literals use `f` suffix in Kotlin
- Named parameters improve readability
- Works identically on JVM, JS, Native platforms

---

## Loading GLTF Models

### Three.js (JavaScript)

```javascript
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';

const loader = new GLTFLoader();

loader.load(
    'models/robot.gltf',
    // onLoad callback
    (gltf) => {
        scene.add(gltf.scene);
        gltf.animations.forEach((clip) => {
            mixer.clipAction(clip).play();
        });
    },
    // onProgress callback
    (xhr) => {
        console.log((xhr.loaded / xhr.total * 100) + '% loaded');
    },
    // onError callback
    (error) => {
        console.error('Error loading model:', error);
    }
);
```

### KreeKt (Kotlin)

```kotlin
import io.kreekt.loader.GLTFLoader
import kotlinx.coroutines.launch

// Using coroutines (recommended)
lifecycleScope.launch {
    val loader = GLTFLoader()

    val result = loader.load("models/robot.gltf") { progress ->
        println("${(progress.percentage * 100).toInt()}% loaded")
    }

    result.onSuccess { scene ->
        sceneRoot.add(scene)
        scene.animations.forEach { clip ->
            mixer.clipAction(clip).play()
        }
    }.onFailure { error ->
        println("Error loading model: ${error.message}")
    }
}
```

**Migration Notes**:

- Async operations use Kotlin coroutines instead of callbacks
- `Result<T>` provides type-safe error handling
- Progress is a data class with `percentage` property
- Works across all platforms (JVM, JS, Native)

---

## Loading and Exporting Models

### Three.js (JavaScript)

```javascript
import { FBXLoader } from 'three/examples/jsm/loaders/FBXLoader.js';
import { GLTFExporter } from 'three/examples/jsm/exporters/GLTFExporter.js';

// Load FBX
const fbxLoader = new FBXLoader();
fbxLoader.load('model.fbx', (object) => {
    scene.add(object);

    // Export to GLTF
    const exporter = new GLTFExporter();
    exporter.parse(
        scene,
        (gltf) => {
            // Save GLTF
            const blob = new Blob([JSON.stringify(gltf)], {
                type: 'application/json'
            });
            saveAs(blob, 'scene.gltf');
        },
        { binary: false }
    );
});
```

### KreeKt (Kotlin)

```kotlin
import io.kreekt.loader.FBXLoader
import io.kreekt.exporter.GLTFExporter
import io.kreekt.exporter.ExportOptions

lifecycleScope.launch {
    // Load FBX
    val fbxLoader = FBXLoader()
    val loadResult = fbxLoader.load("model.fbx")

    loadResult.onSuccess { loadedScene ->
        sceneRoot.add(loadedScene)

        // Export to GLTF
        val exporter = GLTFExporter()
        val exportResult = exporter.export(
            scene = sceneRoot,
            options = ExportOptions(
                binary = false,
                embedTextures = true,
                includeAnimations = true
            )
        )

        exportResult.onSuccess { gltfData ->
            // Save to file (platform-specific)
            exporter.exportToFile(sceneRoot, "scene.gltf")
        }
    }
}
```

**Migration Notes**:

- Both load and export are suspend functions
- `ExportOptions` is a data class with clear defaults
- File saving uses platform-appropriate mechanisms
- Type-safe `Result<T>` instead of callbacks

---

## Node-Based Materials

### Three.js (JavaScript with TSL)

```javascript
import { nodeObject, texture, uv, timerLocal } from 'three/nodes';

// Create node material
const material = new MeshBasicNodeMaterial();

// Create node graph
const textureNode = texture(textureMap);
const uvNode = uv();
const timeNode = timerLocal();

// Animate UVs
const animatedUV = uvNode.add(timeNode.mul(0.1));

// Sample texture with animated UVs
const colorNode = textureNode.sample(animatedUV);

// Set output
material.colorNode = colorNode;
```

### KreeKt (Kotlin)

```kotlin
import io.kreekt.material.nodes.*

// Create node material
val material = NodeMaterial()

// Create node graph
val graph = material.nodeGraph

// Add nodes
val textureNode = TextureSampleNode(textureMap)
val uvNode = AttributeNode("uv")
val timeNode = TimeNode()

graph.addNode(textureNode)
graph.addNode(uvNode)
graph.addNode(timeNode)

// Create animation node
val speedNode = UniformNode("speed", NodeDataType.FLOAT).apply {
    inputs[0].defaultValue = 0.1f
}
val animSpeedNode = MultiplyNode().apply {
    graph.addNode(this)
    graph.connect(timeNode.outputs[0], inputs[0])
    graph.connect(speedNode.outputs[0], inputs[1])
}

// Add UVs
val animatedUVNode = AddNode().apply {
    graph.addNode(this)
    graph.connect(uvNode.outputs[0], inputs[0])
    graph.connect(animSpeedNode.outputs[0], inputs[1])
}

// Sample texture
graph.connect(animatedUVNode.outputs[0], textureNode.inputs[0]) // UV input

// Output
val outputNode = FragmentOutputNode().apply {
    graph.addNode(this)
}
graph.connect(textureNode.outputs[0], outputNode.inputs[0])
graph.outputNode = outputNode

// Graph will auto-compile to WGSL or SPIR-V
```

**Migration Notes**:

- More explicit node creation and connection
- Type-safe connections (compile-time checks)
- Automatic code generation for WebGPU (WGSL) or Vulkan (SPIR-V)
- Same node graph runs on all platforms

---

## Geometry Processing

### Three.js (JavaScript)

```javascript
import { mergeBufferGeometries } from 'three/examples/jsm/utils/BufferGeometryUtils.js';
import { SimplifyModifier } from 'three/examples/jsm/modifiers/SimplifyModifier.js';

// Merge geometries
const merged = mergeBufferGeometries([geo1, geo2, geo3]);

// Simplify geometry
const simplifier = new SimplifyModifier();
const simplified = simplifier.modify(geometry, targetTriangles);

// Compute tangents
geometry.computeTangents();
```

### KreeKt (Kotlin)

```kotlin
import io.kreekt.geometry.utils.GeometryProcessor

// Merge geometries
val merged = GeometryProcessor.mergeGeometries(
    geometries = listOf(geo1, geo2, geo3),
    useGroups = false
)

// Simplify geometry
val simplified = GeometryProcessor.simplify(
    geometry = geometry,
    targetRatio = 0.5f, // 50% of original triangles
    options = SimplificationOptions(
        preserveUVs = true,
        preserveNormals = true
    )
)

// Compute tangents (MikkTSpace algorithm)
GeometryProcessor.computeTangents(geometry, uvChannel = 0)

// Compute convex hull
val points = listOf(
    Vector3(0f, 0f, 0f),
    Vector3(1f, 0f, 0f),
    Vector3(0f, 1f, 0f),
    Vector3(0f, 0f, 1f)
)
val hull = GeometryProcessor.computeConvexHull(points)

// Estimate memory usage
val memInfo = GeometryProcessor.estimateMemoryUsage(geometry)
println("Geometry uses ${memInfo.totalMB} MB")
```

**Migration Notes**:

- Static utility methods instead of modifier classes
- Named parameters for clarity
- More geometry utilities available (convex hull, tessellation)
- Memory estimation built-in

---

## Performance Monitoring

### Three.js (JavaScript with Stats.js)

```javascript
import Stats from 'stats.js';

const stats = new Stats();
stats.showPanel(0); // 0: fps, 1: ms, 2: mb
document.body.appendChild(stats.dom);

function animate() {
    stats.begin();

    // Rendering code
    renderer.render(scene, camera);

    stats.end();

    requestAnimationFrame(animate);
}
```

### KreeKt (Kotlin)

```kotlin
import io.kreekt.performance.PerformanceMonitor

val monitor = PerformanceMonitor()
monitor.enabled = true

fun animate() {
    monitor.beginFrame()

    // Rendering code
    renderer.render(scene, camera)

    monitor.endFrame()

    // Access metrics
    val fps = monitor.getFPS()
    val frameTime = monitor.getFrameTime()
    val memory = monitor.getMemoryUsage()

    println("FPS: $fps, Frame Time: ${frameTime}ms, Memory: ${memory.usedMB}MB")

    // Get averages
    val averages = monitor.getAverages(windowSize = 60)
    println("Avg FPS: ${averages.avgFPS}, Min FPS: ${averages.minFPS}")

    // Continue animation loop
    window.requestAnimationFrame(::animate)
}
```

**Migration Notes**:

- Built into KreeKt, no external library needed
- Cross-platform (JVM, JS, Native, Android, iOS)
- Rich metrics API (FPS, frame time, memory, draw calls, triangles)
- Historical data available via `getMetrics()`

---

## Visualization Helpers

### Three.js (JavaScript)

```javascript
import { AxesHelper, GridHelper, VertexNormalsHelper } from 'three';

// Axes helper
const axesHelper = new AxesHelper(5);
scene.add(axesHelper);

// Grid helper
const gridHelper = new GridHelper(10, 10);
scene.add(gridHelper);

// Vertex normals helper
const normalsHelper = new VertexNormalsHelper(mesh, 1, 0xff0000);
scene.add(normalsHelper);

// Update helpers
normalsHelper.update();
```

### KreeKt (Kotlin)

```kotlin
import io.kreekt.helpers.*

// Axes helper
val axesHelper = AxesHelper(size = 5.0f)
scene.add(axesHelper)

// Grid helper
val gridHelper = GridHelper(
    size = 10.0f,
    divisions = 10,
    colorCenterLine = Color(0x444444),
    colorGrid = Color(0x888888)
)
scene.add(gridHelper)

// Vertex normals helper
val normalsHelper = VertexNormalsHelper(
    targetObject = mesh,
    size = 1.0f,
    color = Color(0xff0000)
)
scene.add(normalsHelper)

// Update helpers
normalsHelper.update()

// Other helpers available:
// - VertexTangentsHelper
// - FaceNormalsHelper
// - CameraHelper
// - DirectionalLightHelper, PointLightHelper, etc.
```

**Migration Notes**:

- API nearly identical to Three.js
- Named parameters for readability
- All helpers available cross-platform

---

## Shader Chunks and Customization

### Three.js (JavaScript)

```javascript
import { ShaderChunk } from 'three';

// Register custom chunk
ShaderChunk['my_custom_fog'] = `
    vec3 applyCustomFog(vec3 color, float depth) {
        float fogFactor = exp(-depth * fogDensity);
        return mix(fogColor, color, fogFactor);
    }
`;

// Use in material
const material = new MeshStandardMaterial({
    color: 0x00ff00
});

material.onBeforeCompile = (shader) => {
    // Add custom uniform
    shader.uniforms.fogDensity = { value: 0.025 };

    // Modify fragment shader
    shader.fragmentShader = shader.fragmentShader.replace(
        '#include <fog_fragment>',
        '#include <my_custom_fog>\ncolor.rgb = applyCustomFog(color.rgb, vViewPosition.z);'
    );
};
```

### KreeKt (Kotlin)

```kotlin
import io.kreekt.material.shader.ShaderChunk
import io.kreekt.material.shader.UniformsUtils

// Register custom chunk
ShaderChunk.registerChunk("my_custom_fog", """
    fn applyCustomFog(color: vec3<f32>, depth: f32) -> vec3<f32> {
        let fogFactor = exp(-depth * fogDensity);
        return mix(fogColor, color, fogFactor);
    }
""")

// Use in material
val material = object : MeshStandardMaterial() {
    override fun onBeforeCompile(shader: Shader, renderer: Renderer) {
        // Add custom uniform
        shader.uniforms["fogDensity"] = Uniform(0.025f)

        // Modify fragment shader
        shader.fragmentShader = shader.fragmentShader.replace(
            "#include <fog_fragment>",
            "#include <my_custom_fog>\ncolor = applyCustomFog(color, vViewPosition.z);"
        )
    }
}
material.color = Color(0x00ff00)
```

**Migration Notes**:

- ShaderChunk API nearly identical
- WGSL syntax on Web, SPIR-V on native platforms
- Shader preprocessing handles #include directives
- `onBeforeCompile` works the same way

---

## Alternative Renderers (Web Only)

### Three.js (JavaScript)

```javascript
import { CSS2DRenderer, CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js';

// Create CSS2D renderer
const labelRenderer = new CSS2DRenderer();
labelRenderer.setSize(window.innerWidth, window.innerHeight);
labelRenderer.domElement.style.position = 'absolute';
labelRenderer.domElement.style.top = '0px';
document.body.appendChild(labelRenderer.domElement);

// Create label
const div = document.createElement('div');
div.className = 'label';
div.textContent = 'My Label';
const label = new CSS2DObject(div);
label.position.set(0, 1, 0);
scene.add(label);

// Render
labelRenderer.render(scene, camera);
```

### KreeKt (Kotlin - JS Platform)

```kotlin
import io.kreekt.renderer.alternative.CSS2DRenderer
import io.kreekt.renderer.alternative.CSS2DObject
import kotlinx.browser.document
import kotlinx.browser.window

// Create CSS2D renderer
val labelRenderer = CSS2DRenderer(document.body!!)
labelRenderer.setSize(window.innerWidth, window.innerHeight)

// Create label (using kotlinx.html)
val div = document.createElement("div").apply {
    className = "label"
    textContent = "My Label"
}
val label = CSS2DObject(div)
label.position.set(0f, 1f, 0f)
scene.add(label)

// Render
labelRenderer.render(scene, camera)
```

**Migration Notes**:

- CSS2D/CSS3D renderers only available on JS platform
- DOM interaction uses kotlinx.browser
- SVGRenderer also available for vector output
- API matches Three.js closely

---

## Compressed Textures

### Three.js (JavaScript)

```javascript
import { KTX2Loader } from 'three/examples/jsm/loaders/KTX2Loader.js';

const ktx2Loader = new KTX2Loader();
ktx2Loader.setTranscoderPath('basis/');
ktx2Loader.detectSupport(renderer);

ktx2Loader.load('texture.ktx2', (texture) => {
    material.map = texture;
    material.needsUpdate = true;
});
```

### KreeKt (Kotlin)

```kotlin
import io.kreekt.loader.KTX2Loader
import io.kreekt.loader.BasisUniversalTranscoder

lifecycleScope.launch {
    // Initialize transcoder
    val transcoder = BasisUniversalTranscoder()
    transcoder.initialize()

    // Create loader
    val ktx2Loader = KTX2Loader()
    ktx2Loader.setTranscoder(transcoder)

    // Load texture
    val result = ktx2Loader.load("texture.ktx2")
    result.onSuccess { texture ->
        material.map = texture
        material.needsUpdate = true
    }
}
```

**Migration Notes**:

- Transcoder must be initialized (loads WASM on Web, JNI on JVM)
- Automatic format selection based on platform capabilities
- Supports all Basis Universal formats
- Works on all platforms (Web, JVM, Native)

---

## Complete Migration Example

### Three.js Full Scene

```javascript
import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js';

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
const renderer = new THREE.WebGLRenderer();
renderer.setSize(window.innerWidth, window.innerHeight);
document.body.appendChild(renderer.domElement);

const composer = new EffectComposer(renderer);
composer.addPass(new RenderPass(scene, camera));
composer.addPass(new UnrealBloomPass(
    new THREE.Vector2(window.innerWidth, window.innerHeight),
    1.5, 0.4, 0.85
));

const loader = new GLTFLoader();
loader.load('model.gltf', (gltf) => {
    scene.add(gltf.scene);
});

camera.position.z = 5;

function animate() {
    requestAnimationFrame(animate);
    composer.render();
}
animate();
```

### KreeKt Full Scene

```kotlin
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.core.scene.Scene
import io.kreekt.loader.GLTFLoader
import io.kreekt.postprocessing.*
import io.kreekt.renderer.DefaultRenderer
import kotlinx.coroutines.launch

// Create scene components
val scene = Scene()
val camera = PerspectiveCamera(
    fov = 75f,
    aspect = window.innerWidth.toFloat() / window.innerHeight,
    near = 0.1f,
    far = 1000f
)
val renderer = DefaultRenderer()
renderer.setSize(window.innerWidth, window.innerHeight)

// Setup post-processing
val composer = EffectComposer(renderer)
composer.addPass(RenderPass(scene, camera))
composer.addPass(UnrealBloomPass(
    strength = 1.5f,
    radius = 0.4f,
    threshold = 0.85f
))

// Load model
lifecycleScope.launch {
    val loader = GLTFLoader()
    loader.load("model.gltf").onSuccess { loadedScene ->
        scene.add(loadedScene)
    }
}

camera.position.z = 5f

// Animation loop
fun animate() {
    window.requestAnimationFrame(::animate)
    composer.render(deltaTime = 0.016f)
}
animate()
```

**Migration Summary**:

- Nearly 1:1 API mapping from Three.js
- Kotlin idioms (coroutines, named parameters, null safety)
- Type-safe at compile time
- Works identically on JVM, JS, Native platforms
- WebGPU on Web, Vulkan on native platforms

---

## Testing Your Migration

### Basic Test Structure

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostProcessingTest {
    @Test
    fun testEffectComposerCreation() {
        val renderer = DefaultRenderer()
        val composer = EffectComposer(renderer)

        assertTrue(composer.passes.isEmpty())
    }

    @Test
    fun testAddPass() {
        val renderer = DefaultRenderer()
        val composer = EffectComposer(renderer)
        val scene = Scene()
        val camera = PerspectiveCamera()

        val pass = RenderPass(scene, camera)
        composer.addPass(pass)

        assertEquals(1, composer.passes.size)
        assertEquals(pass, composer.passes[0])
    }
}
```

---

## Next Steps

1. **Read the Contracts**: Review `/contracts/*.kt` for complete API definitions
2. **Explore Examples**: Check `/examples/` directory for more complex scenarios
3. **Platform-Specific Setup**: See platform guides for JVM, JS, Native, Android, iOS
4. **Performance Tuning**: Use PerformanceMonitor to optimize your scenes
5. **Advanced Features**: Explore node-based materials, physics, XR support

---

## Getting Help

- **Documentation**: https://kreekt.io/docs
- **API Reference**: https://kreekt.io/api
- **GitHub Issues**: https://github.com/kreekt/kreekt/issues
- **Discord**: https://discord.gg/kreekt
- **Three.js Migration Guide**: https://kreekt.io/docs/migration/threejs

---

## Summary of Key Differences

| Aspect             | Three.js                         | KreeKt                        |
|--------------------|----------------------------------|-------------------------------|
| **Language**       | JavaScript/TypeScript            | Kotlin Multiplatform          |
| **Async**          | Callbacks/Promises               | Coroutines/Flow               |
| **Error Handling** | try/catch                        | Result<T>                     |
| **Graphics API**   | WebGL                            | WebGPU/Vulkan                 |
| **Platforms**      | Web only                         | JVM, JS, Native, Android, iOS |
| **Type Safety**    | Runtime (JS) / Compile-time (TS) | Compile-time always           |
| **Shaders**        | GLSL                             | WGSL (Web), SPIR-V (Native)   |

**All Three.js r180 features are available in KreeKt with minimal API changes!**
