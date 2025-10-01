# Data Model: Three.js Feature Parity

**Feature**: Complete Three.js Feature Parity
**Date**: 2025-09-30
**Version**: 1.0

## Overview

This document defines the data structures and entities required for implementing complete Three.js r180 feature parity in KreeKt. All entities follow Kotlin Multiplatform patterns with expect/actual for platform-specific implementations.

## Entity Categories

### 1. Audio System

#### AudioListener
Represents the audio listener (typically attached to camera) for positional audio.

**Properties**:
- `position: Vector3` - World position of listener
- `rotation: Quaternion` - Orientation of listener
- `up: Vector3` - Up vector for listener coordinate system
- `context: AudioContext` - Platform-specific audio context

**Relationships**:
- One-to-one with Camera (typically)
- One-to-many with Audio sources

**Platform Variants**:
```kotlin
expect class AudioListener(camera: Camera?) {
    val position: Vector3
    val rotation: Quaternion
    fun updateMatrixWorld(force: Boolean)
}

// JS: Uses Web Audio API AudioListener
// JVM: Uses OpenAL listener
```

#### PositionalAudio
3D spatial audio source with distance attenuation and Doppler effects.

**Properties**:
- `position: Vector3` - World position of audio source
- `panner: AudioPanner` - Spatialization configuration
- `refDistance: Float` - Reference distance for attenuation
- `maxDistance: Float` - Maximum distance for audio
- `rolloffFactor: Float` - How quickly sound attenuates
- `coneInnerAngle: Float` - Inner cone angle (degrees)
- `coneOuterAngle: Float` - Outer cone angle (degrees)
- `coneOuterGain: Float` - Volume outside cone
- `source: AudioSource` - Platform audio source

**Relationships**:
- Many-to-one with AudioListener
- One-to-one with Object3D (parent)

#### Audio
Non-positional audio source (background music, UI sounds).

**Properties**:
- `source: AudioSource` - Platform audio source
- `gain: Float` - Volume (0.0 to 1.0)
- `playbackRate: Float` - Speed multiplier
- `loop: Boolean` - Loop playback
- `autoplay: Boolean` - Start on load

**State Machine**:
```
[STOPPED] --play()--> [PLAYING] --pause()--> [PAUSED]
[PAUSED] --play()--> [PLAYING]
[PLAYING/PAUSED] --stop()--> [STOPPED]
```

#### AudioAnalyser
Provides frequency spectrum analysis for visualization.

**Properties**:
- `fftSize: Int` - FFT size (power of 2: 32-32768)
- `frequencyBinCount: Int` - Number of frequency bins (fftSize / 2)
- `minDecibels: Float` - Minimum power (default: -100)
- `maxDecibels: Float` - Maximum power (default: -30)
- `smoothingTimeConstant: Float` - Temporal smoothing (0.0-1.0)

**Methods**:
- `getFrequencyData(): FloatArray` - Get frequency magnitudes
- `getTimeDomainData(): FloatArray` - Get waveform samples

---

### 2. Helper Objects

#### Helper (Base)
Abstract base for visual debugging helpers.

**Properties**:
- `visible: Boolean` - Render visibility
- `color: Color` - Primary color
- `renderOrder: Int` - Render priority

**Relationships**:
- Extends Object3D
- Not serialized/saved

#### AxesHelper
Shows XYZ axes as RGB lines.

**Properties**:
- `size: Float` - Length of each axis

**Rendering**:
- Red line: +X axis
- Green line: +Y axis
- Blue line: +Z axis

#### GridHelper
Shows ground plane grid.

**Properties**:
- `size: Float` - Overall grid size
- `divisions: Int` - Number of grid divisions
- `colorCenterLine: Color` - Color of center lines
- `colorGrid: Color` - Color of grid lines

#### BoxHelper
Wireframe box around object's bounding box.

**Properties**:
- `object: Object3D` - Target object
- `color: Color` - Wireframe color

**Behavior**:
- Updates automatically with object transform

#### CameraHelper
Visualizes camera frustum.

**Properties**:
- `camera: Camera` - Target camera
- `color: Color` - Frustum color

**Rendering**:
- Shows near/far planes
- Shows field of view
- Shows camera position

---

### 3. Advanced Cameras

#### CubeCamera
Renders scene from 6 directions into cube texture.

**Properties**:
- `near: Float` - Near clipping plane
- `far: Float` - Far clipping plane
- `renderTarget: WebGLCubeRenderTarget` - Output cube texture
- `cameras: Array<PerspectiveCamera>` - 6 cameras (±X, ±Y, ±Z)

**Methods**:
- `update(renderer: Renderer, scene: Scene)` - Render all 6 faces

**Use Cases**:
- Real-time reflections
- Dynamic environment maps
- Mirrors and water

#### StereoCamera
Generates left/right cameras for VR.

**Properties**:
- `aspect: Float` - Aspect ratio
- `eyeSeparation: Float` - Distance between eyes (default: 0.064m)
- `cameraL: PerspectiveCamera` - Left eye camera
- `cameraR: PerspectiveCamera` - Right eye camera

**Methods**:
- `update(camera: Camera)` - Sync with base camera position

#### ArrayCamera
Multiple sub-cameras for split-screen or multi-viewport.

**Properties**:
- `cameras: Array<Camera>` - Array of sub-cameras

**Rendering**:
- Each camera has viewport rectangle
- Single scene render with multiple views

---

### 4. Fog System

#### Fog
Linear fog with near/far distances.

**Properties**:
- `color: Color` - Fog color
- `near: Float` - Fog starts (distance from camera)
- `far: Float` - Fog fully opaque (distance from camera)

**Shader Integration**:
```wgsl
let fogFactor = clamp((distance - fogNear) / (fogFar - fogNear), 0.0, 1.0);
fragColor = mix(fragColor, fogColor, fogFactor);
```

#### FogExp2
Exponential fog with density.

**Properties**:
- `color: Color` - Fog color
- `density: Float` - Fog density (default: 0.00025)

**Shader Integration**:
```wgsl
let fogFactor = 1.0 - exp(-distance * density);
fragColor = mix(fragColor, fogColor, fogFactor);
```

---

### 5. Raycasting

#### Raycaster
Ray for intersection testing.

**Properties**:
- `ray: Ray` - Ray origin and direction
- `near: Float` - Near clipping distance
- `far: Float` - Far clipping distance
- `camera: Camera?` - Source camera (optional)
- `layers: Layers` - Layer filter mask
- `params: RaycastParams` - Type-specific parameters

**Methods**:
- `setFromCamera(coords: Vector2, camera: Camera)` - Ray from screen coords
- `intersectObject(object: Object3D, recursive: Boolean): List<Intersection>`
- `intersectObjects(objects: List<Object3D>, recursive: Boolean): List<Intersection>`

#### Intersection
Ray-object intersection result.

**Properties**:
- `distance: Float` - Distance from ray origin
- `point: Vector3` - Intersection point (world space)
- `face: Face?` - Intersected triangle (for meshes)
- `faceIndex: Int` - Triangle index
- `object: Object3D` - Intersected object
- `uv: Vector2?` - Texture coordinates at hit point
- `instanceId: Int?` - Instance index (for InstancedMesh)

**Sorting**:
- Results sorted by distance (nearest first)

#### RaycastParams
Type-specific raycasting parameters.

**Properties**:
- `Mesh.threshold: Float` - Unused (always precise)
- `Line.threshold: Float` - Max distance from line (default: 1)
- `Points.threshold: Float` - Max distance from point (default: 1)

---

### 6. Curve System

#### Curve (Abstract Base)
Parametric curve interface.

**Methods**:
- `getPoint(t: Float): Vector3` - Point at parameter t ∈ [0,1]
- `getTangent(t: Float): Vector3` - Normalized tangent at t
- `getPointAt(u: Float): Vector3` - Point at arc-length parameter u
- `getPoints(divisions: Int = 5): List<Vector3>` - Uniformly spaced points
- `getSpacedPoints(divisions: Int = 5): List<Vector3>` - Arc-length spaced points
- `getLength(): Float` - Total arc length
- `getLengths(divisions: Int = 200): List<Float>` - Cumulative length table

#### LineCurve / LineCurve3
Straight line segment (2D/3D).

**Properties**:
- `v1: Vector3` - Start point
- `v2: Vector3` - End point

**Evaluation**:
```kotlin
fun getPoint(t: Float) = v1.lerp(v2, t)
```

#### QuadraticBezierCurve / QuadraticBezierCurve3
Quadratic Bézier curve (2D/3D).

**Properties**:
- `v0: Vector3` - Start point
- `v1: Vector3` - Control point
- `v2: Vector3` - End point

**Evaluation**:
```kotlin
fun getPoint(t: Float): Vector3 {
    val t2 = t * t
    val mt = 1 - t
    val mt2 = mt * mt
    return v0 * mt2 + v1 * (2 * mt * t) + v2 * t2
}
```

#### CubicBezierCurve / CubicBezierCurve3
Cubic Bézier curve (2D/3D).

**Properties**:
- `v0: Vector3` - Start point
- `v1: Vector3` - Control point 1
- `v2: Vector3` - Control point 2
- `v3: Vector3` - End point

#### SplineCurve
2D smooth curve through points.

**Properties**:
- `points: List<Vector2>` - Control points

#### CatmullRomCurve3
3D smooth curve through points.

**Properties**:
- `points: List<Vector3>` - Control points
- `closed: Boolean` - Closed loop
- `curveType: CurveType` - CENTRIPETAL | CHORDAL | CATMULLROM
- `tension: Float` - Curve tension (0.0-1.0)

**CurveType Variants**:
- `CENTRIPETAL`: Best for most cases, prevents loops
- `CHORDAL`: Sharp turns, straight segments
- `CATMULLROM`: Classic, can create loops

#### EllipseCurve
2D elliptical arc.

**Properties**:
- `aX: Float, aY: Float` - Center
- `xRadius: Float, yRadius: Float` - Radii
- `aStartAngle: Float, aEndAngle: Float` - Arc angles (radians)
- `aClockwise: Boolean` - Direction
- `aRotation: Float` - Ellipse rotation

#### ArcCurve
2D circular arc (EllipseCurve with xRadius == yRadius).

#### CurvePath
Composite curve from multiple segments.

**Properties**:
- `curves: List<Curve>` - Curve segments
- `autoClose: Boolean` - Add closing segment

**Methods**:
- `add(curve: Curve)` - Append curve
- `closePath()` - Add line from end to start

#### Shape
Closed 2D curve with holes.

**Properties**:
- `curves: List<Curve>` - Outer boundary
- `holes: List<Path>` - Hole boundaries

**Methods**:
- `moveTo(x: Float, y: Float)` - Start new contour
- `lineTo(x: Float, y: Float)` - Add line
- `bezierCurveTo(...)` - Add cubic Bézier
- `quadraticCurveTo(...)` - Add quadratic Bézier
- `absarc(...)` - Add absolute arc
- `extractPoints(divisions: Int): ShapePoints` - Tessellate to points

#### Path
Open 2D curve.

**Properties**:
- `currentPoint: Vector2` - Current drawing position

**Methods**: Same as Shape

---

### 7. Advanced Textures

#### CubeTexture
Six-sided environment map.

**Properties**:
- `images: Array<Image>` - 6 images [+X, -X, +Y, -Y, +Z, -Z]
- `mapping: CubeReflectionMapping | CubeRefractionMapping`

**Loading**:
```kotlin
val loader = CubeTextureLoader()
val texture = loader.load(arrayOf(
    "px.jpg", "nx.jpg",
    "py.jpg", "ny.jpg",
    "pz.jpg", "nz.jpg"
))
```

#### VideoTexture
Texture from HTML video element (JS) or video file (native).

**Properties**:
- `video: VideoElement` - Source video
- `needsUpdate: Boolean` - Update flag (set true each frame)

**Platform Variants**:
- JS: HTMLVideoElement
- JVM: JavaFX MediaPlayer or LWJGL video decoder
- Android: MediaPlayer or ExoPlayer
- iOS: AVPlayer

#### CanvasTexture
Texture from 2D canvas (JS) or bitmap (native).

**Properties**:
- `canvas: CanvasElement` - Source canvas
- `needsUpdate: Boolean` - Update when canvas changes

**Use Cases**:
- Dynamic text rendering
- Procedural textures
- UI overlays

#### DataTexture
Texture from typed array data.

**Properties**:
- `data: TypedArray` - Raw pixel data (Uint8Array, Float32Array, etc.)
- `width: Int, height: Int` - Dimensions
- `format: TextureFormat` - RGBA, RGB, RED, RG, etc.
- `type: TextureType` - UNSIGNED_BYTE, FLOAT, HALF_FLOAT, etc.

**Example**:
```kotlin
val size = 512
val data = Uint8Array(size * size * 4)
// Fill data with procedural pattern
val texture = DataTexture(data, size, size, TextureFormat.RGBA, TextureType.UNSIGNED_BYTE)
```

#### Data3DTexture
3D volumetric texture.

**Properties**:
- `data: TypedArray` - Voxel data
- `width: Int, height: Int, depth: Int` - 3D dimensions

**Use Cases**:
- Volume rendering
- 3D noise functions
- Voxel data visualization

#### CompressedTexture
GPU-compressed texture data.

**Properties**:
- `mipmaps: List<CompressedMipmap>` - Mipmap levels
- `width: Int, height: Int` - Dimensions
- `format: CompressedFormat` - DXT, ETC2, ASTC, PVRTC, etc.

**CompressedMipmap**:
- `data: ByteArray` - Compressed data
- `width: Int, height: Int` - Level dimensions

#### DepthTexture
Depth buffer as texture.

**Properties**:
- `width: Int, height: Int` - Dimensions
- `format: DepthFormat` - DEPTH_COMPONENT, DEPTH_STENCIL
- `type: DepthType` - UNSIGNED_SHORT, UNSIGNED_INT, UNSIGNED_INT_24_8, FLOAT

**Use Cases**:
- Shadow mapping
- Post-process effects needing depth
- SSAO, SSR

#### PMREMGenerator
Pre-filtered Mipmapped Radiance Environment Map generator.

**Methods**:
- `fromCubemap(cubemap: CubeTexture): CubeTexture` - Generate PMREM
- `fromEquirectangular(texture: Texture): CubeTexture` - From equirect HDR
- `compileCubemapShader()` - Precompile shaders

**Output**:
- Roughness-based mipmap levels for IBL

---

### 8. Instancing

#### InstancedMesh
Mesh rendered multiple times with per-instance properties.

**Properties**:
- `geometry: BufferGeometry` - Shared geometry
- `material: Material` - Shared material
- `count: Int` - Number of instances
- `instanceMatrix: InstancedBufferAttribute` - Per-instance 4x4 transform matrix
- `instanceColor: InstancedBufferAttribute?` - Optional per-instance color

**Methods**:
- `setMatrixAt(index: Int, matrix: Matrix4)` - Set instance transform
- `getMatrixAt(index: Int, matrix: Matrix4)` - Get instance transform
- `setColorAt(index: Int, color: Color)` - Set instance color
- `getColorAt(index: Int, color: Color)` - Get instance color

**GPU Rendering**:
- Single draw call via GPU instancing
- Vertex shader receives `@builtin(instance_index)`

#### InstancedBufferAttribute
Per-instance vertex attribute.

**Properties**:
- `array: TypedArray` - Attribute data
- `itemSize: Int` - Components per instance (1-4)
- `normalized: Boolean` - Normalize to [0,1] or [-1,1]
- `meshPerAttribute: Int` - Instances per attribute (usually 1)

**Example**:
```kotlin
val colors = Float32Array(count * 3)
// Fill colors
val colorAttribute = InstancedBufferAttribute(colors, 3)
mesh.instanceColor = colorAttribute
```

---

### 9. Points & Sprites

#### Points
Point cloud object.

**Properties**:
- `geometry: BufferGeometry` - Point positions and attributes
- `material: PointsMaterial` - Point appearance

**Attributes**:
- `position: Vector3[]` - Point positions (required)
- `color: Color[]?` - Per-point colors (optional)
- `size: Float[]?` - Per-point sizes (optional)

#### PointsMaterial
Material for rendering points.

**Properties**:
- `color: Color` - Base color
- `size: Float` - Point size (pixels or world units)
- `sizeAttenuation: Boolean` - Scale points by distance
- `map: Texture?` - Point sprite texture
- `alphaTest: Float` - Alpha cutoff
- `transparent: Boolean` - Enable transparency

#### Sprite
Billboarded 2D plane (always faces camera).

**Properties**:
- `material: SpriteMaterial` - Appearance
- `center: Vector2` - Sprite center (0.5, 0.5 = centered)

**Rendering**:
- Rotation billboard (faces camera)
- Scale applies in screen space

#### SpriteMaterial
Material for sprite rendering.

**Properties**:
- `color: Color` - Tint color
- `map: Texture?` - Sprite texture
- `alphaTest: Float` - Alpha cutoff
- `rotation: Float` - Rotation angle (radians)
- `sizeAttenuation: Boolean` - Scale by distance

---

### 10. Morph Targets

#### MorphTarget
Single morph target (blend shape).

**Properties**:
- `name: String` - Target name
- `vertices: List<Vector3>` - Position deltas
- `normals: List<Vector3>?` - Normal deltas

**In BufferGeometry**:
```kotlin
geometry.morphAttributes = mapOf(
    "position" to listOf(morphTarget1Positions, morphTarget2Positions),
    "normal" to listOf(morphTarget1Normals, morphTarget2Normals)
)
```

#### Mesh with Morph Targets

**Properties**:
- `morphTargetInfluences: FloatArray?` - Blend weights (0.0-1.0)
- `morphTargetDictionary: Map<String, Int>?` - Name to index mapping

**Usage**:
```kotlin
mesh.morphTargetInfluences!![0] = 0.5  // 50% influence
mesh.morphTargetInfluences!![1] = 0.3  // 30% influence
```

**Shader**:
- Vertex shader blends base + (Σ target_i * influence_i)

---

### 11. Clipping Planes

#### ClippingPlane
Plane for geometry clipping.

**Properties**:
- `normal: Vector3` - Plane normal (normalized)
- `constant: Float` - Plane distance from origin

**Plane Equation**: `normal · point + constant = 0`

**Usage**:
```kotlin
material.clippingPlanes = listOf(
    ClippingPlane(Vector3(0, 1, 0), 0.0),  // Clip below Y=0
    ClippingPlane(Vector3(1, 0, 0), -5.0)   // Clip left of X=5
)

renderer.localClippingEnabled = true
```

**Platform Limits**:
- WebGL2/Vulkan: Up to 8 planes
- Fallback: Fragment discard (slower)

---

### 12. Layers

#### Layers
32-bit layer mask for selective rendering.

**Properties**:
- `mask: UInt` - 32-bit layer mask

**Methods**:
- `set(layer: Int)` - Set to single layer
- `enable(layer: Int)` - Enable layer bit
- `disable(layer: Int)` - Disable layer bit
- `toggle(layer: Int)` - Toggle layer bit
- `test(layers: Layers): Boolean` - Test mask intersection

**Usage**:
```kotlin
// Object on layer 1
object.layers.set(1)

// Camera renders layers 0 and 1
camera.layers.enable(0)
camera.layers.enable(1)

// Raycaster filters by layers
raycaster.layers.enable(1)
```

---

### 13. Render Targets

#### WebGLRenderTarget
Off-screen framebuffer for rendering.

**Properties**:
- `width: Int, height: Int` - Dimensions
- `texture: Texture` - Color attachment
- `depthBuffer: Boolean` - Enable depth buffer
- `stencilBuffer: Boolean` - Enable stencil buffer
- `depthTexture: DepthTexture?` - Depth as texture
- `samples: Int` - MSAA sample count

**Methods**:
- `setSize(width: Int, height: Int)` - Resize
- `dispose()` - Free GPU resources

#### WebGLCubeRenderTarget
Cube map framebuffer.

**Properties**:
- Inherits WebGLRenderTarget
- `texture: CubeTexture` - 6-face cube texture

**Usage**:
```kotlin
val cubeRenderTarget = WebGLCubeRenderTarget(512)
val cubeCamera = CubeCamera(0.1, 1000.0, cubeRenderTarget)

// Each frame:
cubeCamera.update(renderer, scene)
reflectiveMaterial.envMap = cubeRenderTarget.texture
```

#### WebGLMultipleRenderTargets
Multiple color attachments (MRT).

**Properties**:
- Inherits WebGLRenderTarget
- `textures: Array<Texture>` - Multiple color attachments

**Use Cases**:
- Deferred rendering (albedo, normal, depth)
- Multiple post-process outputs

---

### 14. Shape & Path

#### Shape (Detailed)
2D vector shape with holes.

**State**:
- `currentPath: Path` - Current contour
- `holes: List<Path>` - Hole contours

**Drawing Methods**:
- `moveTo(x, y)` - Start new point
- `lineTo(x, y)` - Line to point
- `bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y)` - Cubic Bézier
- `quadraticCurveTo(cpx, cpy, x, y)` - Quadratic Bézier
- `arc(x, y, radius, startAngle, endAngle, clockwise)` - Arc
- `absarc(x, y, radius, startAngle, endAngle, clockwise)` - Absolute arc
- `ellipse(x, y, xRadius, yRadius, startAngle, endAngle, clockwise, rotation)` - Ellipse

**Tessellation**:
- `extractPoints(divisions: Int): ShapePoints` - Convert to triangles

**ShapePoints**:
- `shape: List<Vector2>` - Outer boundary points
- `holes: List<List<Vector2>>` - Hole boundary points

---

### 15. Line Rendering

#### Line2
Thick line with proper joins and caps.

**Properties**:
- `geometry: LineGeometry` - Line positions
- `material: LineMaterial` - Line appearance

**LineGeometry**:
- `positions: Float32Array` - 3D line segments
- `colors: Float32Array?` - Per-vertex colors

**LineMaterial**:
- `color: Color` - Line color
- `linewidth: Float` - Line width (world units)
- `dashed: Boolean` - Enable dashed rendering
- `dashScale: Float, dashSize: Float, gapSize: Float` - Dash parameters

#### LineSegments2
Disconnected thick line segments.

**Properties**:
- Similar to Line2 but renders disconnected segments

---

### 16. LOD (Level of Detail)

#### LOD
Automatic mesh switching based on camera distance.

**Properties**:
- `levels: List<LODLevel>` - Detail levels
- `autoUpdate: Boolean` - Auto-update current level

**LODLevel**:
- `object: Object3D` - Mesh for this level
- `distance: Float` - Switch distance

**Methods**:
- `addLevel(object: Object3D, distance: Float)` - Add detail level
- `getCurrentLevel(): Int` - Get active level
- `update(camera: Camera)` - Update based on distance

**Example**:
```kotlin
val lod = LOD()
lod.addLevel(highDetailMesh, 0.0)     // 0-50 units
lod.addLevel(mediumDetailMesh, 50.0)  // 50-100 units
lod.addLevel(lowDetailMesh, 100.0)    // 100+ units
```

---

### 17. Constants

#### BlendingMode
```kotlin
enum class BlendingMode {
    NO_BLENDING,
    NORMAL_BLENDING,
    ADDITIVE_BLENDING,
    SUBTRACTIVE_BLENDING,
    MULTIPLY_BLENDING,
    CUSTOM_BLENDING
}
```

#### TextureWrapping
```kotlin
enum class TextureWrapping {
    REPEAT_WRAPPING,
    CLAMP_TO_EDGE_WRAPPING,
    MIRRORED_REPEAT_WRAPPING
}
```

#### TextureFilter
```kotlin
enum class TextureFilter {
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    LINEAR_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_LINEAR
}
```

#### Side
```kotlin
enum class Side {
    FRONT_SIDE,
    BACK_SIDE,
    DOUBLE_SIDE
}
```

---

## Relationships Summary

### Composition Hierarchy
```
Scene
├── AudioListener (1)
├── Audio/PositionalAudio (0..*)
├── Fog (0..1)
├── Object3D (0..*)
│   ├── Mesh
│   ├── InstancedMesh
│   ├── Points
│   ├── Sprite
│   ├── Line/Line2
│   ├── LOD
│   └── Helper (development only)
└── RenderTarget (external)
```

### Texture Hierarchy
```
Texture (base)
├── CubeTexture
├── VideoTexture
├── CanvasTexture
├── DataTexture
├── Data3DTexture
├── CompressedTexture
└── DepthTexture
```

### Curve Hierarchy
```
Curve (abstract)
├── LineCurve / LineCurve3
├── QuadraticBezierCurve / QuadraticBezierCurve3
├── CubicBezierCurve / CubicBezierCurve3
├── SplineCurve
├── CatmullRomCurve3
├── EllipseCurve
├── ArcCurve
├── CurvePath
├── Shape
└── Path
```

## Validation Rules

### Audio System
- `refDistance > 0`
- `maxDistance >= refDistance`
- `rolloffFactor >= 0`
- `0 <= coneInnerAngle <= 360`
- `0 <= coneOuterAngle <= 360`
- `0 <= coneOuterGain <= 1`

### Fog
- `near < far` (linear fog)
- `density > 0` (exponential fog)

### Raycaster
- `near < far`
- `ray.direction` must be normalized

### Curves
- Parameter `t` ∈ [0, 1]
- Arc length parameter `u` ∈ [0, 1]

### Instancing
- `count > 0`
- `instanceMatrix.array.length == count * 16` (4x4 matrix)
- `instanceColor.array.length == count * 3` (RGB color)

### Render Targets
- `width > 0 && height > 0`
- `samples` must be power of 2 (1, 2, 4, 8, 16)

### LOD
- Levels must be ordered by increasing distance
- At least one level required

---

## Type Compatibility

### Cross-Platform Types

All entities use platform-agnostic Kotlin types where possible:

| Domain | Kotlin Type | Platform Variants |
|--------|-------------|-------------------|
| Audio Context | `expect class AudioContext` | Web Audio, OpenAL, etc. |
| Audio Source | `expect class AudioSource` | Web Audio, OpenAL, etc. |
| Video Element | `expect class VideoElement` | HTMLVideo, MediaPlayer, etc. |
| Canvas Element | `expect class CanvasElement` | HTMLCanvas, Bitmap, etc. |
| Typed Arrays | `expect class TypedArray` | JS TypedArray, NIO Buffer |

### Serialization

Entities marked with `@Serializable` for saving/loading:
- Curves (all types)
- Shapes and Paths
- LOD configurations
- Material settings (clipping planes, constants)

Not serialized:
- Audio sources (runtime only)
- Render targets (GPU resources)
- Helpers (development tools)

---

**Data Model Version**: 1.0
**Next**: Generate API contracts from this data model