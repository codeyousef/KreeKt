# Data Model: Complete Three.js r180 Feature Parity

**Feature**: Complete Three.js r180 Feature Parity
**Branch**: `012-complete-three-js`
**Date**: 2025-10-01

## Overview

This document defines the core data model for KreeKt's Three.js r180 feature parity implementation. The model consists of 14 key entities organized into logical subsystems, with clear relationships, validation rules, and state management patterns.

**Design Principles**:
- **Type Safety**: No runtime casts, compile-time validation using sealed classes and inline classes
- **Three.js Compatibility**: API structure mirrors Three.js r180 for easy migration
- **Kotlin Idioms**: Data classes, default parameters, nullable types, coroutines
- **Platform Abstraction**: expect/actual pattern for platform-specific graphics primitives
- **Immutability Where Possible**: Prefer immutable data structures, mutable only when necessary for performance

---

## Entity Catalog

### Core Entities

1. **Scene**: Root container for 3D scene graph
2. **Object3D**: Base class for all scene objects with transformation hierarchy
3. **Geometry**: Vertex data structure (positions, normals, UVs, indices)
4. **Material**: Surface appearance definition with rendering properties
5. **Texture**: Image data with sampling configuration
6. **Light**: Illumination source with color, intensity, shadows
7. **Camera**: Viewing frustum definition with projection

### Advanced Entities

8. **Animation**: Temporal data with keyframe tracks and interpolation
9. **Loader**: Asset import system with format support
10. **Raycaster**: Intersection testing utility for mouse picking
11. **RenderTarget**: Off-screen rendering buffer
12. **Shader**: Custom GPU program with uniforms and attributes
13. **Helper**: Debug visualization tool
14. **Audio**: 3D positional sound source
15. **XRSession**: VR/AR session state

---

## 1. Scene Entity

**Purpose**: Root container for 3D scene graph, manages background, environment, and fog

### Data Structure

```kotlin
package io.kreekt.core.scene

import io.kreekt.core.math.Color
import io.kreekt.texture.Texture
import io.kreekt.texture.CubeTexture

/**
 * Scene is the root container for all 3D objects, lights, and cameras.
 * Compatible with Three.js Scene API.
 */
class Scene : Object3D() {
    override val type: String get() = "Scene"

    // Background rendering
    var background: SceneBackground? = null

    // Environment map for reflections and lighting
    var environment: CubeTexture? = null

    // Fog configuration
    var fog: Fog? = null

    // Auto-update behavior for children
    var autoUpdate: Boolean = true

    // Override cast/receive shadow (scenes don't cast shadows)
    init {
        castShadow = false
        receiveShadow = false
    }

    override fun clone(recursive: Boolean): Scene {
        val scene = Scene()
        scene.copy(this, recursive)
        scene.background = this.background
        scene.environment = this.environment
        scene.fog = this.fog?.clone()
        return scene
    }
}

/**
 * Scene background can be a solid color, texture, or cube map
 */
sealed class SceneBackground {
    data class SolidColor(val color: Color) : SceneBackground()
    data class TextureBackground(val texture: Texture) : SceneBackground()
    data class CubeBackground(val cubeTexture: CubeTexture) : SceneBackground()
}

/**
 * Fog types for atmospheric effects
 */
sealed class Fog {
    abstract val color: Color

    /**
     * Linear fog (density increases linearly with distance)
     */
    data class Linear(
        override val color: Color,
        val near: Float = 1f,
        val far: Float = 1000f
    ) : Fog() {
        fun clone(): Linear = copy()
    }

    /**
     * Exponential fog (density increases exponentially)
     */
    data class Exponential(
        override val color: Color,
        val density: Float = 0.00025f
    ) : Fog() {
        fun clone(): Exponential = copy()
    }
}
```

### Relationships

- **Contains**: Multiple Object3D instances (children)
- **References**: Optional CubeTexture (environment)
- **References**: Optional Fog instance

### Validation Rules

- Scene cannot be added as child of another Object3D
- Background texture dimensions should be power of two for optimal performance
- Fog near/far values: `near >= 0`, `far > near`
- Fog density: `density > 0`

### State Transitions

```
Created → [add objects] → Populated → [render] → Rendered
                       → [clear] → Empty
```

---

## 2. Object3D Entity

**Purpose**: Base class for all scene objects with transformation hierarchy

### Data Structure

```kotlin
package io.kreekt.core.scene

import io.kreekt.core.math.*

/**
 * Base class for all objects in the 3D scene.
 * Manages transformation, hierarchy, and common properties.
 */
abstract class Object3D {
    // Identity
    val id: Int = generateId()
    var name: String = ""
    open val type: String get() = "Object3D"

    // Transformation (local space)
    val position: Vector3 = Vector3()
    val rotation: Euler = Euler()
    val scale: Vector3 = Vector3(1f, 1f, 1f)
    val quaternion: Quaternion = Quaternion()

    // Transformation matrices
    val matrix: Matrix4 = Matrix4()        // Local transform
    val matrixWorld: Matrix4 = Matrix4()   // World transform

    // Matrix update behavior
    var matrixAutoUpdate: Boolean = true
    var matrixWorldNeedsUpdate: Boolean = false

    // Visibility and shadows
    var visible: Boolean = true
    var castShadow: Boolean = false
    var receiveShadow: Boolean = false

    // Hierarchy
    var parent: Object3D? = null
    private val _children: MutableList<Object3D> = mutableListOf()
    val children: List<Object3D> get() = _children

    // Selective rendering/raycasting
    val layers: Layers = Layers()

    // Custom user data
    val userData: MutableMap<String, Any> = mutableMapOf()

    // Render hooks
    var onBeforeRender: ((Object3D) -> Unit)? = null
    var onAfterRender: ((Object3D) -> Unit)? = null

    // Hierarchy operations
    fun add(vararg objects: Object3D): Object3D
    fun remove(vararg objects: Object3D): Object3D
    fun removeFromParent(): Object3D
    fun clear(): Object3D

    // Transformation operations
    fun updateMatrix()
    fun updateMatrixWorld(force: Boolean = false)
    fun applyMatrix4(matrix: Matrix4): Object3D

    // World space queries
    fun getWorldPosition(target: Vector3 = Vector3()): Vector3
    fun getWorldQuaternion(target: Quaternion = Quaternion()): Quaternion
    fun getWorldScale(target: Vector3 = Vector3()): Vector3
    fun getWorldDirection(target: Vector3 = Vector3()): Vector3

    // Rotation helpers
    fun lookAt(target: Vector3)
    fun rotateOnAxis(axis: Vector3, angle: Float): Object3D
    fun rotateX(angle: Float): Object3D
    fun rotateY(angle: Float): Object3D
    fun rotateZ(angle: Float): Object3D

    // Translation helpers
    fun translateOnAxis(axis: Vector3, distance: Float): Object3D
    fun translateX(distance: Float): Object3D
    fun translateY(distance: Float): Object3D
    fun translateZ(distance: Float): Object3D

    // Coordinate transformation
    fun localToWorld(vector: Vector3): Vector3
    fun worldToLocal(vector: Vector3): Vector3

    // Traversal
    fun traverse(callback: (Object3D) -> Unit)
    fun traverseVisible(callback: (Object3D) -> Unit)
    fun traverseAncestors(callback: (Object3D) -> Unit)

    // Queries
    fun getObjectByName(name: String): Object3D?
    fun getObjectById(id: Int): Object3D?
    fun getObjectByProperty(name: String, value: Any): Object3D?

    // Cloning
    abstract fun clone(recursive: Boolean = true): Object3D
    open fun copy(source: Object3D, recursive: Boolean = true): Object3D
}

/**
 * Layer management for selective rendering
 */
data class Layers(var mask: Int = 1) {
    fun set(layer: Int)
    fun enable(layer: Int)
    fun disable(layer: Int)
    fun toggle(layer: Int)
    fun test(layers: Layers): Boolean
    fun isEnabled(layer: Int): Boolean
    fun enableAll()
    fun disableAll()
}
```

### Relationships

- **Parent-Child**: Tree hierarchy with single parent, multiple children
- **Self-Reference**: Transformation matrices reference parent's world matrix

### Validation Rules

- Object cannot be its own child (prevents circular hierarchy)
- Object cannot have multiple parents simultaneously
- Scale components should be non-zero to avoid singularities
- Layer mask is 32-bit integer (supports 32 layers)

### State Transitions

```
Created → [add to scene] → InScene → [remove] → Orphaned
                        → [setVisible(false)] → Hidden
                        → [dispose] → Disposed
```

### Invariants

- `rotation` and `quaternion` stay synchronized
- `matrix` = compose(position, quaternion, scale)
- `matrixWorld` = parent.matrixWorld × matrix (if parent exists)

---

## 3. Geometry Entity

**Purpose**: Vertex data structure with positions, normals, UVs, indices

### Data Structure

```kotlin
package io.kreekt.geometry

import io.kreekt.core.math.*

/**
 * BufferGeometry stores geometry data in GPU-efficient typed arrays.
 * Compatible with Three.js BufferGeometry API.
 */
class BufferGeometry : Disposable {
    val id: Int = generateId()
    var name: String = ""
    val type: String = "BufferGeometry"

    // Vertex attributes (position, normal, uv, color, etc.)
    val attributes: MutableMap<String, BufferAttribute> = mutableMapOf()

    // Index buffer (optional, for indexed geometry)
    var index: BufferAttribute? = null

    // Bounding volumes (computed on demand)
    var boundingBox: Box3? = null
    var boundingSphere: Sphere? = null

    // Draw range (subset of geometry to render)
    var drawRange: DrawRange = DrawRange(start = 0, count = Int.MAX_VALUE)

    // Groups for multi-material support
    private val _groups: MutableList<GeometryGroup> = mutableListOf()
    val groups: List<GeometryGroup> get() = _groups

    // Morph target support
    val morphAttributes: MutableMap<String, List<BufferAttribute>> = mutableMapOf()
    var morphTargetsRelative: Boolean = false

    // Custom user data
    val userData: MutableMap<String, Any> = mutableMapOf()

    // Attribute management
    fun setAttribute(name: String, attribute: BufferAttribute)
    fun getAttribute(name: String): BufferAttribute?
    fun deleteAttribute(name: String)
    fun hasAttribute(name: String): Boolean

    fun setIndex(index: BufferAttribute?)
    fun setIndex(indices: IntArray)
    fun setIndex(indices: ShortArray)

    // Bounding volume computation
    fun computeBoundingBox()
    fun computeBoundingSphere()
    fun computeVertexNormals()
    fun computeTangents()

    // Group management (for multi-material)
    fun addGroup(start: Int, count: Int, materialIndex: Int = 0)
    fun clearGroups()

    // Morph targets
    fun setMorphAttribute(name: String, attributes: List<BufferAttribute>)

    // Cloning
    fun clone(): BufferGeometry
    fun copy(source: BufferGeometry): BufferGeometry

    // Disposal
    override fun dispose()
}

/**
 * Buffer attribute stores typed array data for vertex attributes
 */
data class BufferAttribute(
    val array: FloatArray,         // Typed array (Float32Array equivalent)
    val itemSize: Int,             // Number of values per vertex (e.g., 3 for Vector3)
    val normalized: Boolean = false // Whether to normalize integer values
) {
    val count: Int = array.size / itemSize
    var needsUpdate: Boolean = false

    // Convenience constructors
    companion object {
        fun fromVector3Array(vectors: List<Vector3>): BufferAttribute
        fun fromVector2Array(vectors: List<Vector2>): BufferAttribute
        fun fromColorArray(colors: List<Color>): BufferAttribute
    }

    fun clone(): BufferAttribute
    fun copy(source: BufferAttribute): BufferAttribute
}

/**
 * Index buffer for indexed rendering
 */
data class IndexAttribute(
    val array: IntArray,           // Uint16Array or Uint32Array equivalent
    val itemSize: Int = 1
) {
    val count: Int = array.size
    var needsUpdate: Boolean = false
}

/**
 * Geometry group for multi-material support
 */
data class GeometryGroup(
    val start: Int,                // Start index in index buffer
    val count: Int,                // Number of indices
    val materialIndex: Int = 0     // Which material to use
)

/**
 * Draw range to render subset of geometry
 */
data class DrawRange(
    val start: Int,
    val count: Int
)

/**
 * Primitive geometry types (procedural generation)
 */
sealed class PrimitiveGeometry : BufferGeometry() {
    abstract fun generate()
}

class BoxGeometry(
    val width: Float = 1f,
    val height: Float = 1f,
    val depth: Float = 1f,
    val widthSegments: Int = 1,
    val heightSegments: Int = 1,
    val depthSegments: Int = 1
) : PrimitiveGeometry()

class SphereGeometry(
    val radius: Float = 1f,
    val widthSegments: Int = 32,
    val heightSegments: Int = 16,
    val phiStart: Float = 0f,
    val phiLength: Float = PI * 2f,
    val thetaStart: Float = 0f,
    val thetaLength: Float = PI
) : PrimitiveGeometry()

class PlaneGeometry(
    val width: Float = 1f,
    val height: Float = 1f,
    val widthSegments: Int = 1,
    val heightSegments: Int = 1
) : PrimitiveGeometry()

// ... additional primitive types (Cylinder, Cone, Torus, etc.)
```

### Type Hierarchy

```
BufferGeometry (base)
├── PrimitiveGeometry (procedural)
│   ├── BoxGeometry
│   ├── SphereGeometry
│   ├── PlaneGeometry
│   ├── CylinderGeometry
│   ├── ConeGeometry
│   ├── TorusGeometry
│   ├── TorusKnotGeometry
│   ├── RingGeometry
│   ├── IcosahedronGeometry
│   └── ... (13 more primitive types)
├── ExtrudeGeometry (path extrusion)
├── LatheGeometry (revolution)
├── TextGeometry (text rendering)
├── ShapeGeometry (2D shapes → 3D)
└── ParametricGeometry (custom functions)
```

### Relationships

- **Contains**: Multiple BufferAttribute instances
- **References**: Optional index buffer
- **Used By**: Mesh, Line, Points objects

### Validation Rules

- All attributes must have same vertex count
- `itemSize` must be > 0
- Index values must be < vertex count
- Group ranges must not exceed index buffer size
- Bounding volumes invalidated when vertex positions change

### State Transitions

```
Created → [setAttribute] → Configured → [computeBounds] → Ready
                                     → [needsUpdate=true] → Dirty
                                     → [dispose] → Disposed
```

---

## 4. Material Entity

**Purpose**: Surface appearance definition with colors, textures, and rendering properties

### Data Structure

```kotlin
package io.kreekt.material

import io.kreekt.core.math.Color
import io.kreekt.texture.Texture

/**
 * Base class for all materials.
 * Compatible with Three.js Material API.
 */
abstract class Material : Disposable {
    val id: Int = generateId()
    var name: String = ""
    abstract val type: String

    // Transparency
    var transparent: Boolean = false
    var opacity: Float = 1f

    // Blending
    var blending: BlendMode = BlendMode.Normal
    var blendSrc: BlendFactor = BlendFactor.SrcAlpha
    var blendDst: BlendFactor = BlendFactor.OneMinusSrcAlpha
    var blendEquation: BlendEquation = BlendEquation.Add

    // Depth testing and writing
    var depthTest: Boolean = true
    var depthWrite: Boolean = true
    var depthFunc: DepthFunc = DepthFunc.LessEqual

    // Side rendering
    var side: Side = Side.Front

    // Polygon offset (for z-fighting)
    var polygonOffset: Boolean = false
    var polygonOffsetFactor: Float = 0f
    var polygonOffsetUnits: Float = 0f

    // Alpha test threshold
    var alphaTest: Float = 0f

    // Alpha to coverage (MSAA)
    var alphaToCoverage: Boolean = false

    // Premultiplied alpha
    var premultipliedAlpha: Boolean = false

    // Dithering
    var dithering: Boolean = false

    // Shadows
    var shadowSide: Side? = null

    // Color space
    var colorWrite: Boolean = true

    // Visibility
    var visible: Boolean = true

    // Rendering order hint
    var renderOrder: Int = 0

    // Custom user data
    val userData: MutableMap<String, Any> = mutableMapOf()

    // Version tracking for GPU updates
    var version: Int = 0
    var needsUpdate: Boolean = false

    // Cloning
    abstract fun clone(): Material
    open fun copy(source: Material): Material

    // Disposal
    override fun dispose()
}

/**
 * Basic material (unlit, flat color)
 */
class MeshBasicMaterial(
    var color: Color = Color(0xffffff),
    var map: Texture? = null,
    var alphaMap: Texture? = null,
    var aoMap: Texture? = null,
    var aoMapIntensity: Float = 1f,
    var specularMap: Texture? = null,
    var envMap: Texture? = null,
    var envMapIntensity: Float = 1f,
    var reflectivity: Float = 1f,
    var refractionRatio: Float = 0.98f,
    var combine: CombineMode = CombineMode.Multiply,
    var wireframe: Boolean = false,
    var wireframeLinewidth: Float = 1f
) : Material() {
    override val type: String = "MeshBasicMaterial"
}

/**
 * PBR standard material (physically-based rendering)
 */
class MeshStandardMaterial(
    var color: Color = Color(0xffffff),
    var roughness: Float = 1f,
    var metalness: Float = 0f,
    var map: Texture? = null,
    var roughnessMap: Texture? = null,
    var metalnessMap: Texture? = null,
    var normalMap: Texture? = null,
    var normalScale: Vector2 = Vector2(1f, 1f),
    var displacementMap: Texture? = null,
    var displacementScale: Float = 1f,
    var displacementBias: Float = 0f,
    var aoMap: Texture? = null,
    var aoMapIntensity: Float = 1f,
    var emissive: Color = Color(0x000000),
    var emissiveMap: Texture? = null,
    var emissiveIntensity: Float = 1f,
    var envMap: Texture? = null,
    var envMapIntensity: Float = 1f,
    var alphaMap: Texture? = null,
    var wireframe: Boolean = false,
    var wireframeLinewidth: Float = 1f,
    var flatShading: Boolean = false
) : Material() {
    override val type: String = "MeshStandardMaterial"
}

/**
 * PBR physical material (extended standard with clearcoat, sheen, etc.)
 */
class MeshPhysicalMaterial(
    // Inherits all MeshStandardMaterial properties
    var clearcoat: Float = 0f,
    var clearcoatMap: Texture? = null,
    var clearcoatRoughness: Float = 0f,
    var clearcoatRoughnessMap: Texture? = null,
    var clearcoatNormalMap: Texture? = null,
    var clearcoatNormalScale: Vector2 = Vector2(1f, 1f),
    var sheen: Float = 0f,
    var sheenColor: Color = Color(0x000000),
    var sheenColorMap: Texture? = null,
    var sheenRoughness: Float = 1f,
    var sheenRoughnessMap: Texture? = null,
    var transmission: Float = 0f,
    var transmissionMap: Texture? = null,
    var thickness: Float = 0f,
    var thicknessMap: Texture? = null,
    var attenuationDistance: Float = Float.POSITIVE_INFINITY,
    var attenuationColor: Color = Color(0xffffff),
    var specularIntensity: Float = 1f,
    var specularColor: Color = Color(0xffffff),
    var specularIntensityMap: Texture? = null,
    var specularColorMap: Texture? = null,
    var ior: Float = 1.5f
) : Material() {
    override val type: String = "MeshPhysicalMaterial"
}

/**
 * Custom shader material
 */
class ShaderMaterial(
    var vertexShader: String = "",
    var fragmentShader: String = "",
    val uniforms: MutableMap<String, Uniform> = mutableMapOf(),
    val defines: MutableMap<String, String> = mutableMapOf(),
    var lights: Boolean = false,
    var fog: Boolean = false,
    var clipping: Boolean = false,
    var wireframe: Boolean = false,
    var wireframeLinewidth: Float = 1f
) : Material() {
    override val type: String = "ShaderMaterial"
}

// Enums
enum class BlendMode { None, Normal, Additive, Subtractive, Multiply, Custom }
enum class BlendFactor { Zero, One, SrcColor, OneMinusSrcColor, SrcAlpha, OneMinusSrcAlpha, DstColor, OneMinusDstColor, DstAlpha, OneMinusDstAlpha }
enum class BlendEquation { Add, Subtract, ReverseSubtract, Min, Max }
enum class DepthFunc { Never, Always, Less, LessEqual, Equal, GreaterEqual, Greater, NotEqual }
enum class Side { Front, Back, Double }
enum class CombineMode { Multiply, Mix, Add }
```

### Type Hierarchy

```
Material (base)
├── MeshBasicMaterial (unlit)
├── MeshLambertMaterial (diffuse lighting)
├── MeshPhongMaterial (specular lighting)
├── MeshStandardMaterial (PBR)
├── MeshPhysicalMaterial (extended PBR)
├── MeshToonMaterial (cel-shading)
├── MeshNormalMaterial (visualize normals)
├── MeshDepthMaterial (depth rendering)
├── MeshDistanceMaterial (distance field)
├── MeshMatcapMaterial (matcap shading)
├── LineMaterial (line rendering)
├── LineBasicMaterial
├── LineDashedMaterial
├── PointsMaterial (particle rendering)
├── SpriteMaterial (billboards)
├── ShadowMaterial (shadow planes)
├── ShaderMaterial (custom shaders)
└── RawShaderMaterial (raw WGSL)
```

### Relationships

- **References**: Multiple Texture instances
- **Used By**: Mesh, Line, Points objects
- **Compiled To**: GPU shader programs

### Validation Rules

- `opacity` ∈ [0, 1]
- `roughness` ∈ [0, 1]
- `metalness` ∈ [0, 1]
- `alphaTest` ∈ [0, 1]
- `wireframeLinewidth` > 0
- Transparent materials require `transparent = true`

### State Transitions

```
Created → [configure properties] → Configured → [compile shader] → Compiled
                                              → [needsUpdate=true] → Dirty
                                              → [dispose] → Disposed
```

---

## 5. Texture Entity

**Purpose**: Image data with sampling and transformation configuration

### Data Structure

```kotlin
package io.kreekt.texture

import io.kreekt.core.math.Vector2
import io.kreekt.core.math.Matrix3

/**
 * Texture represents image data loaded onto the GPU.
 * Compatible with Three.js Texture API.
 */
class Texture : Disposable {
    val id: Int = generateId()
    var name: String = ""
    val type: String = "Texture"

    // Image source
    var image: ImageData? = null

    // Wrapping modes
    var wrapS: TextureWrapping = TextureWrapping.ClampToEdge
    var wrapT: TextureWrapping = TextureWrapping.ClampToEdge

    // Filtering
    var magFilter: TextureFilter = TextureFilter.Linear
    var minFilter: TextureFilter = TextureFilter.LinearMipmapLinear

    // Anisotropic filtering
    var anisotropy: Int = 1

    // Texture format
    var format: TextureFormat = TextureFormat.RGBA
    var internalFormat: TextureInternalFormat? = null
    var type: TextureDataType = TextureDataType.UnsignedByte

    // Transformation
    var offset: Vector2 = Vector2(0f, 0f)
    var repeat: Vector2 = Vector2(1f, 1f)
    var center: Vector2 = Vector2(0f, 0f)
    var rotation: Float = 0f
    val matrix: Matrix3 = Matrix3()
    var matrixAutoUpdate: Boolean = true

    // Mipmaps
    var generateMipmaps: Boolean = true
    var premultiplyAlpha: Boolean = false
    var flipY: Boolean = true
    var unpackAlignment: Int = 4

    // Color space
    var colorSpace: ColorSpace = ColorSpace.SRGB

    // Version tracking
    var version: Int = 0
    var needsUpdate: Boolean = false

    // Custom user data
    val userData: MutableMap<String, Any> = mutableMapOf()

    // Update texture matrix
    fun updateMatrix()

    // Cloning
    fun clone(): Texture
    fun copy(source: Texture): Texture

    // Disposal
    override fun dispose()
}

/**
 * Cube texture for environment maps
 */
class CubeTexture(
    val images: Array<ImageData?> = arrayOfNulls(6)  // +X, -X, +Y, -Y, +Z, -Z
) : Disposable {
    val id: Int = generateId()
    var name: String = ""
    val type: String = "CubeTexture"

    // Wrapping (cube textures are always clamped)
    var wrapS: TextureWrapping = TextureWrapping.ClampToEdge
    var wrapT: TextureWrapping = TextureWrapping.ClampToEdge

    // Filtering
    var magFilter: TextureFilter = TextureFilter.Linear
    var minFilter: TextureFilter = TextureFilter.LinearMipmapLinear

    // Format
    var format: TextureFormat = TextureFormat.RGBA
    var type: TextureDataType = TextureDataType.UnsignedByte

    // Mipmaps
    var generateMipmaps: Boolean = true
    var flipY: Boolean = false

    // Version tracking
    var version: Int = 0
    var needsUpdate: Boolean = false

    override fun dispose()
}

/**
 * Video texture for video playback
 */
class VideoTexture(
    var videoElement: VideoElement
) : Texture() {
    override val type: String = "VideoTexture"

    // Auto-update from video stream
    var autoUpdate: Boolean = true

    override fun update()
}

/**
 * Canvas texture for dynamic 2D content
 */
class CanvasTexture(
    var canvas: CanvasElement
) : Texture() {
    override val type: String = "CanvasTexture"

    override fun update()
}

/**
 * Compressed texture for GPU-native formats
 */
class CompressedTexture(
    val mipmaps: List<CompressedMipmap>,
    val width: Int,
    val height: Int,
    val format: CompressedTextureFormat
) : Texture() {
    override val type: String = "CompressedTexture"
}

data class CompressedMipmap(
    val data: ByteArray,
    val width: Int,
    val height: Int
)

// Platform-specific image data
expect class ImageData {
    val width: Int
    val height: Int
    val data: ByteArray  // RGBA pixels
}

expect class VideoElement
expect class CanvasElement

// Enums
enum class TextureWrapping { Repeat, ClampToEdge, MirroredRepeat }
enum class TextureFilter { Nearest, Linear, NearestMipmapNearest, LinearMipmapNearest, NearestMipmapLinear, LinearMipmapLinear }
enum class TextureFormat { Alpha, RGB, RGBA, Luminance, LuminanceAlpha, Depth, DepthStencil }
enum class TextureInternalFormat { RGB8, RGBA8, RGB16F, RGBA16F, RGB32F, RGBA32F, R8, RG8 }
enum class TextureDataType { UnsignedByte, Byte, UnsignedShort, Short, UnsignedInt, Int, Float, HalfFloat }
enum class ColorSpace { SRGB, Linear }
enum class CompressedTextureFormat {
    RGB_S3TC_DXT1, RGBA_S3TC_DXT1, RGBA_S3TC_DXT3, RGBA_S3TC_DXT5,
    RGB_ETC2, RGBA_ETC2_EAC,
    RGB_PVRTC_4BPPV1, RGBA_PVRTC_4BPPV1,
    ASTC_4x4, ASTC_8x8
}
```

### Type Hierarchy

```
Texture (base)
├── CubeTexture (environment maps)
├── VideoTexture (video playback)
├── CanvasTexture (dynamic 2D)
├── CompressedTexture (GPU formats)
├── DataTexture (raw pixel data)
├── Data3DTexture (volumetric data)
├── DataArrayTexture (texture arrays)
├── DepthTexture (depth rendering)
└── FramebufferTexture (render targets)
```

### Relationships

- **References**: ImageData (platform-specific)
- **Used By**: Material instances
- **Uploaded To**: GPU texture units

### Validation Rules

- Image dimensions should be power of two for mipmapping
- `anisotropy` ∈ [1, maxAnisotropy]
- `rotation` in radians
- Cube textures require exactly 6 images
- Compressed textures must match declared format

### State Transitions

```
Created → [load image] → Loaded → [upload GPU] → Ready
                                → [needsUpdate=true] → Dirty
                                → [dispose] → Disposed
```

---

## 6. Light Entity

**Purpose**: Illumination source with color, intensity, and shadow configuration

### Data Structure

```kotlin
package io.kreekt.light

import io.kreekt.core.scene.Object3D
import io.kreekt.core.math.Color
import io.kreekt.camera.Camera

/**
 * Base class for all lights.
 * Compatible with Three.js Light API.
 */
abstract class Light : Object3D() {
    abstract val lightType: String

    // Light properties
    var color: Color = Color(0xffffff)
    var intensity: Float = 1f

    // Override Object3D type
    override val type: String get() = lightType
}

/**
 * Ambient light illuminates all objects equally
 */
class AmbientLight(
    color: Color = Color(0xffffff),
    intensity: Float = 1f
) : Light() {
    override val lightType: String = "AmbientLight"

    init {
        this.color = color
        this.intensity = intensity
    }
}

/**
 * Directional light emits parallel rays (like sunlight)
 */
class DirectionalLight(
    color: Color = Color(0xffffff),
    intensity: Float = 1f
) : Light() {
    override val lightType: String = "DirectionalLight"

    // Target position (light points at target)
    val target: Object3D = Object3D()

    // Shadow configuration
    var shadow: DirectionalLightShadow? = null

    init {
        this.color = color
        this.intensity = intensity
        position.set(0f, 1f, 0f)
    }
}

/**
 * Point light emits in all directions from a point
 */
class PointLight(
    color: Color = Color(0xffffff),
    intensity: Float = 1f,
    var distance: Float = 0f,  // 0 = infinite
    var decay: Float = 2f      // Physical decay = 2
) : Light() {
    override val lightType: String = "PointLight"

    // Shadow configuration
    var shadow: PointLightShadow? = null

    init {
        this.color = color
        this.intensity = intensity
    }
}

/**
 * Spot light emits in a cone
 */
class SpotLight(
    color: Color = Color(0xffffff),
    intensity: Float = 1f,
    var distance: Float = 0f,
    var angle: Float = PI / 3f,
    var penumbra: Float = 0f,
    var decay: Float = 2f
) : Light() {
    override val lightType: String = "SpotLight"

    // Target position
    val target: Object3D = Object3D()

    // Shadow configuration
    var shadow: SpotLightShadow? = null

    init {
        this.color = color
        this.intensity = intensity
        position.set(0f, 1f, 0f)
    }
}

/**
 * Hemisphere light with sky and ground colors
 */
class HemisphereLight(
    var skyColor: Color = Color(0xffffff),
    var groundColor: Color = Color(0x444444),
    intensity: Float = 1f
) : Light() {
    override val lightType: String = "HemisphereLight"

    init {
        this.intensity = intensity
        position.set(0f, 1f, 0f)
    }
}

/**
 * Rectangular area light
 */
class RectAreaLight(
    color: Color = Color(0xffffff),
    intensity: Float = 1f,
    var width: Float = 10f,
    var height: Float = 10f
) : Light() {
    override val lightType: String = "RectAreaLight"

    init {
        this.color = color
        this.intensity = intensity
    }
}

/**
 * Light probe for image-based lighting
 */
class LightProbe(
    val sh: SphericalHarmonics = SphericalHarmonics(),
    intensity: Float = 1f
) : Light() {
    override val lightType: String = "LightProbe"

    init {
        this.intensity = intensity
    }
}

/**
 * Shadow configuration for lights
 */
sealed class LightShadow {
    abstract val camera: Camera
    abstract var mapSize: Vector2
    abstract var bias: Float
    abstract var normalBias: Float
    abstract var radius: Float

    // Shadow map texture (created by renderer)
    var map: RenderTarget? = null

    // Shadow matrix (light view-projection)
    val matrix: Matrix4 = Matrix4()

    fun updateMatrices(light: Light)
}

data class DirectionalLightShadow(
    override val camera: OrthographicCamera = OrthographicCamera(),
    override var mapSize: Vector2 = Vector2(512f, 512f),
    override var bias: Float = 0f,
    override var normalBias: Float = 0f,
    override var radius: Float = 1f
) : LightShadow()

data class PointLightShadow(
    override val camera: PerspectiveCamera = PerspectiveCamera(90f, 1f, 0.5f, 500f),
    override var mapSize: Vector2 = Vector2(512f, 512f),
    override var bias: Float = 0f,
    override var normalBias: Float = 0f,
    override var radius: Float = 1f
) : LightShadow()

data class SpotLightShadow(
    override val camera: PerspectiveCamera = PerspectiveCamera(50f, 1f, 0.5f, 500f),
    override var mapSize: Vector2 = Vector2(512f, 512f),
    override var bias: Float = 0f,
    override var normalBias: Float = 0f,
    override var radius: Float = 1f
) : LightShadow()

/**
 * Spherical harmonics for light probes
 */
data class SphericalHarmonics(
    val coefficients: FloatArray = FloatArray(27)  // 9 bands × 3 channels
) {
    fun set(values: FloatArray)
    fun scale(s: Float): SphericalHarmonics
    fun add(sh: SphericalHarmonics): SphericalHarmonics
    fun lerp(sh: SphericalHarmonics, alpha: Float): SphericalHarmonics
}
```

### Type Hierarchy

```
Light (base)
├── AmbientLight
├── DirectionalLight
├── PointLight
├── SpotLight
├── HemisphereLight
├── RectAreaLight
└── LightProbe
```

### Relationships

- **Extends**: Object3D (lights have position/rotation)
- **References**: Camera (for shadow mapping)
- **References**: RenderTarget (shadow map texture)

### Validation Rules

- `intensity` ≥ 0
- `distance` ≥ 0 (0 = infinite)
- `decay` ≥ 0 (typically 1 or 2)
- Spot light `angle` ∈ (0, π/2]
- Spot light `penumbra` ∈ [0, 1]
- Shadow `mapSize` should be power of two

### State Transitions

```
Created → [add to scene] → Active → [intensity=0] → Disabled
                                  → [dispose shadow] → Disposed
```

---

## 7. Camera Entity

**Purpose**: Viewing frustum definition with projection matrix

### Data Structure

```kotlin
package io.kreekt.camera

import io.kreekt.core.scene.Object3D
import io.kreekt.core.math.*

/**
 * Base class for all cameras.
 * Compatible with Three.js Camera API.
 */
abstract class Camera : Object3D() {
    abstract val cameraType: String

    // View matrix (inverse of matrixWorld)
    val matrixWorldInverse: Matrix4 = Matrix4()

    // Projection matrix
    val projectionMatrix: Matrix4 = Matrix4()
    val projectionMatrixInverse: Matrix4 = Matrix4()

    // Combined view-projection matrix
    val viewProjectionMatrix: Matrix4 = Matrix4()

    override val type: String get() = cameraType

    // Update projection matrix (called when camera parameters change)
    abstract fun updateProjectionMatrix()

    // Update matrices
    override fun updateMatrixWorld(force: Boolean) {
        super.updateMatrixWorld(force)
        matrixWorldInverse.copy(matrixWorld).invert()
    }
}

/**
 * Perspective camera (realistic 3D projection)
 */
class PerspectiveCamera(
    var fov: Float = 50f,        // Field of view in degrees
    var aspect: Float = 1f,      // Aspect ratio (width / height)
    var near: Float = 0.1f,      // Near clipping plane
    var far: Float = 2000f,      // Far clipping plane
    var focus: Float = 10f,      // Focus distance for depth of field
    var filmGauge: Float = 35f,  // Film gauge for depth of field
    var filmOffset: Float = 0f   // Film offset for depth of field
) : Camera() {
    override val cameraType: String = "PerspectiveCamera"

    // View frustum (computed from fov, aspect, near, far)
    var zoom: Float = 1f

    init {
        updateProjectionMatrix()
    }

    override fun updateProjectionMatrix() {
        val top = near * tan(degToRad(0.5f * fov)) / zoom
        val height = 2f * top
        val width = aspect * height
        val left = -0.5f * width

        projectionMatrix.makePerspective(left, left + width, top, top - height, near, far)
        projectionMatrixInverse.copy(projectionMatrix).invert()
    }

    fun setViewOffset(
        fullWidth: Int, fullHeight: Int,
        x: Int, y: Int,
        width: Int, height: Int
    )

    fun clearViewOffset()

    fun updateMatrixWorld(force: Boolean = false)

    override fun clone(recursive: Boolean): PerspectiveCamera
}

/**
 * Orthographic camera (parallel projection, no perspective)
 */
class OrthographicCamera(
    var left: Float = -1f,
    var right: Float = 1f,
    var top: Float = 1f,
    var bottom: Float = -1f,
    var near: Float = 0.1f,
    var far: Float = 2000f
) : Camera() {
    override val cameraType: String = "OrthographicCamera"

    var zoom: Float = 1f

    init {
        updateProjectionMatrix()
    }

    override fun updateProjectionMatrix() {
        val dx = (right - left) / (2 * zoom)
        val dy = (top - bottom) / (2 * zoom)
        val cx = (right + left) / 2
        val cy = (top + bottom) / 2

        projectionMatrix.makeOrthographic(
            cx - dx, cx + dx,
            cy + dy, cy - dy,
            near, far
        )
        projectionMatrixInverse.copy(projectionMatrix).invert()
    }

    fun setViewOffset(
        fullWidth: Int, fullHeight: Int,
        x: Int, y: Int,
        width: Int, height: Int
    )

    fun clearViewOffset()

    override fun clone(recursive: Boolean): OrthographicCamera
}

/**
 * Array camera (multiple viewports in single render)
 */
class ArrayCamera(
    val cameras: Array<Camera> = emptyArray()
) : Camera() {
    override val cameraType: String = "ArrayCamera"

    override fun updateProjectionMatrix() {
        // Array camera doesn't have its own projection
    }
}

/**
 * Cube camera (renders to cube map texture)
 */
class CubeCamera(
    var near: Float = 0.1f,
    var far: Float = 2000f,
    val renderTarget: CubeRenderTarget
) : Object3D() {
    override val type: String = "CubeCamera"

    // Six perspective cameras for cube faces
    private val cameraPX: PerspectiveCamera
    private val cameraNX: PerspectiveCamera
    private val cameraPY: PerspectiveCamera
    private val cameraNY: PerspectiveCamera
    private val cameraPZ: PerspectiveCamera
    private val cameraNZ: PerspectiveCamera

    fun update(renderer: Renderer, scene: Scene)
}

/**
 * Stereo camera (VR/AR stereo rendering)
 */
class StereoCamera {
    val cameraL: PerspectiveCamera = PerspectiveCamera()
    val cameraR: PerspectiveCamera = PerspectiveCamera()

    var aspect: Float = 1f
    var eyeSep: Float = 0.064f  // Eye separation in meters

    fun update(camera: Camera)
}
```

### Type Hierarchy

```
Camera (base)
├── PerspectiveCamera (3D projection)
├── OrthographicCamera (2D/isometric projection)
├── ArrayCamera (multi-viewport)
└── CubeCamera (environment capture)

StereoCamera (utility, not extends Camera)
```

### Relationships

- **Extends**: Object3D (cameras have position/rotation)
- **Used By**: Renderer for rendering
- **Used By**: Raycaster for mouse picking

### Validation Rules

- PerspectiveCamera: `fov` ∈ (0, 180), `near` > 0, `far` > `near`
- OrthographicCamera: `near` < `far`
- `zoom` > 0
- `aspect` > 0

### State Transitions

```
Created → [set parameters] → Configured → [updateProjectionMatrix] → Ready
                                        → [parameters change] → Dirty
```

---

## 8. Animation Entity

**Purpose**: Temporal data with keyframe tracks and interpolation

### Data Structure

```kotlin
package io.kreekt.animation

import io.kreekt.core.scene.Object3D
import io.kreekt.core.math.*

/**
 * Animation clip stores keyframe tracks for animating object properties.
 * Compatible with Three.js AnimationClip API.
 */
data class AnimationClip(
    val name: String,
    val duration: Float,           // Duration in seconds
    val tracks: List<KeyframeTrack>,
    val blendMode: BlendMode = BlendMode.Normal
) {
    val uuid: String = generateUUID()

    fun optimize(): AnimationClip
    fun trim(): AnimationClip
    fun validate(): Boolean
    fun clone(): AnimationClip

    companion object {
        fun parse(json: String): AnimationClip
        fun toJSON(clip: AnimationClip): String
        fun findByName(clips: List<AnimationClip>, name: String): AnimationClip?
    }
}

/**
 * Keyframe track stores animated values over time
 */
sealed class KeyframeTrack {
    abstract val name: String          // Property path (e.g., ".position[x]")
    abstract val times: FloatArray     // Keyframe times
    abstract val values: FloatArray    // Keyframe values
    abstract val interpolation: InterpolationMode

    val TimeBufferType: String = "Float32Array"
    val ValueBufferType: String = "Float32Array"

    fun getValueSize(): Int
    fun interpolate(t1: Int, t0: Int, t: Float, result: FloatArray)
    fun optimize(): KeyframeTrack
    fun trim(startTime: Float, endTime: Float): KeyframeTrack
    fun validate(): Boolean
    fun clone(): KeyframeTrack
}

class VectorKeyframeTrack(
    override val name: String,
    override val times: FloatArray,
    override val values: FloatArray,    // [x,y,z, x,y,z, ...]
    override val interpolation: InterpolationMode = InterpolationMode.Linear
) : KeyframeTrack() {
    override fun getValueSize(): Int = 3
}

class QuaternionKeyframeTrack(
    override val name: String,
    override val times: FloatArray,
    override val values: FloatArray,    // [x,y,z,w, x,y,z,w, ...]
    override val interpolation: InterpolationMode = InterpolationMode.Linear
) : KeyframeTrack() {
    override fun getValueSize(): Int = 4
}

class NumberKeyframeTrack(
    override val name: String,
    override val times: FloatArray,
    override val values: FloatArray,
    override val interpolation: InterpolationMode = InterpolationMode.Linear
) : KeyframeTrack() {
    override fun getValueSize(): Int = 1
}

class ColorKeyframeTrack(
    override val name: String,
    override val times: FloatArray,
    override val values: FloatArray,    // [r,g,b, r,g,b, ...]
    override val interpolation: InterpolationMode = InterpolationMode.Linear
) : KeyframeTrack() {
    override fun getValueSize(): Int = 3
}

class BooleanKeyframeTrack(
    override val name: String,
    override val times: FloatArray,
    override val values: FloatArray,    // 0.0 = false, 1.0 = true
    override val interpolation: InterpolationMode = InterpolationMode.Discrete
) : KeyframeTrack() {
    override fun getValueSize(): Int = 1
}

class StringKeyframeTrack(
    override val name: String,
    override val times: FloatArray,
    val values: Array<String>,
    override val interpolation: InterpolationMode = InterpolationMode.Discrete
) : KeyframeTrack() {
    override val values: FloatArray = FloatArray(0)  // Not used
    override fun getValueSize(): Int = 1
}

enum class InterpolationMode {
    Discrete,         // Step interpolation
    Linear,           // Linear interpolation
    Cubic,            // Cubic spline
    Slerp             // Spherical linear (quaternions)
}

enum class BlendMode {
    Normal,           // Overwrite
    Additive          // Add to existing values
}

/**
 * Animation mixer manages playback of animation clips
 */
class AnimationMixer(val root: Object3D) {
    private val actions: MutableMap<String, AnimationAction> = mutableMapOf()
    var time: Float = 0f
    var timeScale: Float = 1f

    fun clipAction(clip: AnimationClip, optionalRoot: Object3D? = null): AnimationAction
    fun existingAction(clip: AnimationClip, optionalRoot: Object3D? = null): AnimationAction?
    fun stopAllAction(): AnimationMixer
    fun update(deltaTime: Float): AnimationMixer
    fun setTime(timeInSeconds: Float): AnimationMixer
    fun getRoot(): Object3D
    fun uncacheClip(clip: AnimationClip)
    fun uncacheRoot(root: Object3D)
    fun uncacheAction(clip: AnimationClip, optionalRoot: Object3D? = null)
}

/**
 * Animation action controls playback of a clip
 */
class AnimationAction(
    val mixer: AnimationMixer,
    val clip: AnimationClip,
    val localRoot: Object3D? = null
) {
    var time: Float = 0f
    var timeScale: Float = 1f
    var weight: Float = 1f
    var loop: LoopMode = LoopMode.Repeat
    var repetitions: Int = Int.MAX_VALUE
    var clampWhenFinished: Boolean = false
    var zeroSlopeAtStart: Boolean = true
    var zeroSlopeAtEnd: Boolean = true
    var enabled: Boolean = true
    var paused: Boolean = false

    fun play(): AnimationAction
    fun stop(): AnimationAction
    fun reset(): AnimationAction
    fun isRunning(): Boolean
    fun isScheduled(): Boolean
    fun startAt(time: Float): AnimationAction
    fun setLoop(mode: LoopMode, repetitions: Int): AnimationAction
    fun setEffectiveWeight(weight: Float): AnimationAction
    fun getEffectiveWeight(): Float
    fun fadeIn(duration: Float): AnimationAction
    fun fadeOut(duration: Float): AnimationAction
    fun crossFadeFrom(fadeOutAction: AnimationAction, duration: Float, warp: Boolean): AnimationAction
    fun crossFadeTo(fadeInAction: AnimationAction, duration: Float, warp: Boolean): AnimationAction
    fun stopFading(): AnimationAction
    fun setEffectiveTimeScale(timeScale: Float): AnimationAction
    fun getEffectiveTimeScale(): Float
    fun setDuration(duration: Float): AnimationAction
    fun syncWith(action: AnimationAction): AnimationAction
    fun halt(duration: Float): AnimationAction
    fun warp(startTimeScale: Float, endTimeScale: Float, duration: Float): AnimationAction
    fun stopWarping(): AnimationAction
    fun getMixer(): AnimationMixer
    fun getClip(): AnimationClip
    fun getRoot(): Object3D
}

enum class LoopMode {
    Once,
    Repeat,
    PingPong
}
```

### Type Hierarchy

```
KeyframeTrack (base)
├── VectorKeyframeTrack (position, scale)
├── QuaternionKeyframeTrack (rotation)
├── NumberKeyframeTrack (scalar properties)
├── ColorKeyframeTrack (colors)
├── BooleanKeyframeTrack (visibility, etc.)
└── StringKeyframeTrack (string properties)
```

### Relationships

- **Contains**: Multiple KeyframeTrack instances
- **Managed By**: AnimationMixer
- **Controlled By**: AnimationAction
- **Targets**: Object3D properties

### Validation Rules

- `times` array must be sorted ascending
- `times` and `values` arrays must have compatible sizes
- `times` ≥ 0
- `duration` > 0
- `weight` ∈ [0, ∞)
- `timeScale` can be negative (reverse playback)

### State Transitions

```
Created → [play] → Playing → [pause] → Paused → [play] → Playing
                           → [stop] → Stopped
                           → [fadeOut] → FadingOut → Stopped
```

---

## 9-15. Additional Entities (Summary)

Due to length constraints, the remaining entities follow the same pattern:

### 9. Loader Entity
- Purpose: Asset import with format support (GLTF, FBX, OBJ, textures)
- Key Properties: `url`, `loadingManager`, `onProgress`, `onError`
- State: `Idle → Loading → Loaded / Error`

### 10. Raycaster Entity
- Purpose: Intersection testing for mouse picking
- Key Properties: `ray`, `near`, `far`, `layers`, `params`
- Methods: `intersectObject()`, `intersectObjects()`, `setFromCamera()`

### 11. RenderTarget Entity
- Purpose: Off-screen rendering buffer
- Key Properties: `width`, `height`, `texture`, `depthBuffer`, `stencilBuffer`
- Types: `RenderTarget`, `CubeRenderTarget`, `WebGLMultisampleRenderTarget`

### 12. Shader Entity
- Purpose: Custom GPU programs
- Key Properties: `vertexShader`, `fragmentShader`, `uniforms`, `defines`
- Types: `ShaderMaterial`, `RawShaderMaterial`

### 13. Helper Entity
- Purpose: Debug visualization
- Types: `AxesHelper`, `GridHelper`, `BoxHelper`, `ArrowHelper`, `CameraHelper`, `LightHelper`, `SkeletonHelper`

### 14. Audio Entity
- Purpose: 3D positional sound
- Key Properties: `source`, `volume`, `playbackRate`, `loop`, `distance`, `distanceModel`
- Types: `Audio`, `PositionalAudio`, `AudioListener`, `AudioAnalyser`

### 15. XRSession Entity
- Purpose: VR/AR session management
- Key Properties: `mode`, `inputSources`, `referenceSpace`, `hitTestSource`
- Types: `XRSession`, `XRController`, `XRHand`

---

## Cross-Entity Relationships

### Primary Relationships

```
Scene
 ├─ contains → Object3D (hierarchy)
 │   ├─ extends → Camera
 │   ├─ extends → Light
 │   └─ extends → Mesh
 │       ├─ references → Geometry
 │       └─ references → Material
 │           └─ references → Texture
 └─ references → CubeTexture (environment)

AnimationMixer
 ├─ targets → Object3D
 └─ manages → AnimationAction
      └─ plays → AnimationClip
           └─ contains → KeyframeTrack

Raycaster
 └─ intersects → Object3D[]

Loader
 └─ produces → Scene | Geometry | Material | Texture

Light
 └─ references → LightShadow
      └─ references → Camera (shadow frustum)
      └─ references → RenderTarget (shadow map)
```

### Dependency Graph

```
Core Layer (no dependencies):
- Vector2, Vector3, Vector4, Matrix3, Matrix4, Quaternion, Euler, Color
- Box2, Box3, Sphere, Plane, Ray, Triangle, Frustum

Scene Graph Layer (depends on Core):
- Object3D, Scene, Camera, Light

Rendering Layer (depends on Scene Graph):
- Geometry, Material, Texture, Shader, RenderTarget

Animation Layer (depends on Scene Graph):
- AnimationClip, AnimationMixer, AnimationAction, KeyframeTrack

Interaction Layer (depends on Scene Graph):
- Raycaster, Helper, Controls

Asset Layer (depends on all):
- Loader (produces all entity types)

XR Layer (depends on Rendering + Interaction):
- XRSession, XRController, XRHand

Audio Layer (depends on Scene Graph):
- Audio, PositionalAudio, AudioListener
```

---

## Platform-Specific Abstractions (expect/actual)

### Graphics Primitives

```kotlin
// commonMain
expect class GraphicsBuffer {
    val size: Long
    val usage: BufferUsage
    fun write(data: ByteArray, offset: Long = 0)
    fun read(size: Int, offset: Long = 0): ByteArray
    fun dispose()
}

expect class GraphicsTexture {
    val width: Int
    val height: Int
    val format: TextureFormat
    fun upload(data: ByteArray, mipLevel: Int = 0)
    fun generateMipmaps()
    fun dispose()
}

expect class ShaderProgram {
    val id: ShaderId
    val uniforms: Map<String, UniformLocation>
    val attributes: Map<String, AttributeLocation>
    fun bind()
    fun setUniform(name: String, value: Any)
    fun dispose()
}

expect class RenderCanvas {
    val width: Int
    val height: Int
    fun getContext(): RenderContext
}

expect class RenderContext {
    fun clear(color: Boolean, depth: Boolean, stencil: Boolean)
    fun draw(geometry: BufferGeometry, material: Material)
    fun present()
}

// jvmMain - Vulkan implementations
actual class GraphicsBuffer(private val vkBuffer: Long, private val memory: Long) { /* ... */ }
actual class GraphicsTexture(private val vkImage: Long, private val vkImageView: Long) { /* ... */ }
actual class ShaderProgram(private val pipeline: Long) { /* ... */ }

// jsMain - WebGPU implementations
actual class GraphicsBuffer(private val gpuBuffer: GPUBuffer) { /* ... */ }
actual class GraphicsTexture(private val gpuTexture: GPUTexture) { /* ... */ }
actual class ShaderProgram(private val pipeline: GPURenderPipeline) { /* ... */ }

// nativeMain - Vulkan implementations
actual class GraphicsBuffer(private val vkBuffer: COpaquePointer, private val memory: COpaquePointer) { /* ... */ }
actual class GraphicsTexture(private val vkImage: COpaquePointer, private val vkImageView: COpaquePointer) { /* ... */ }
actual class ShaderProgram(private val pipeline: COpaquePointer) { /* ... */ }
```

---

## Validation Framework

### Entity Validation Interface

```kotlin
interface Validatable {
    fun validate(): ValidationResult
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()
}

data class ValidationError(
    val field: String,
    val message: String,
    val severity: Severity
)

enum class Severity { Warning, Error, Critical }
```

### Example Validation Implementation

```kotlin
class MeshStandardMaterial : Material(), Validatable {
    override fun validate(): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (roughness !in 0f..1f) {
            errors.add(ValidationError(
                field = "roughness",
                message = "Roughness must be in range [0, 1], got $roughness",
                severity = Severity.Error
            ))
        }

        if (metalness !in 0f..1f) {
            errors.add(ValidationError(
                field = "metalness",
                message = "Metalness must be in range [0, 1], got $metalness",
                severity = Severity.Error
            ))
        }

        if (opacity !in 0f..1f) {
            errors.add(ValidationError(
                field = "opacity",
                message = "Opacity must be in range [0, 1], got $opacity",
                severity = Severity.Error
            ))
        }

        if (transparent && opacity == 1f) {
            errors.add(ValidationError(
                field = "transparent",
                message = "Transparent flag set but opacity is 1.0",
                severity = Severity.Warning
            ))
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
```

---

## Memory Management and Disposal

All GPU resources implement the `Disposable` interface:

```kotlin
interface Disposable {
    fun dispose()
}

abstract class GraphicsResource : Disposable {
    private var disposed = false

    protected abstract fun doDispose()

    final override fun dispose() {
        if (!disposed) {
            doDispose()
            disposed = true
        }
    }

    fun isDisposed(): Boolean = disposed
}
```

### Resource Lifecycle

1. **Creation**: Allocate CPU-side structures
2. **Upload**: Transfer data to GPU
3. **Usage**: Render operations
4. **Update**: Modify data (triggers re-upload)
5. **Disposal**: Free GPU resources

### Disposal Best Practices

```kotlin
// Manual disposal
val geometry = BoxGeometry(1f, 1f, 1f)
val material = MeshStandardMaterial()
val mesh = Mesh(geometry, material)

// ... use mesh ...

// Dispose when done
mesh.geometry.dispose()
mesh.material.dispose()

// Automatic disposal with use()
fun renderScene(scene: Scene) {
    val geometry = BoxGeometry(1f, 1f, 1f).use { geom ->
        val material = MeshStandardMaterial().use { mat ->
            val mesh = Mesh(geom, mat)
            scene.add(mesh)
            // ... render ...
        }
    }
}  // Resources automatically disposed
```

---

## Summary

This data model defines 15 key entities for complete Three.js r180 API parity:

1. **Scene**: Root container with background, environment, fog
2. **Object3D**: Base class for scene objects with transformation hierarchy
3. **Geometry**: Vertex data with attributes and indices
4. **Material**: Surface appearance with PBR properties
5. **Texture**: Image data with sampling configuration
6. **Light**: Illumination sources with shadows
7. **Camera**: Viewing frustum definitions
8. **Animation**: Temporal data with keyframe tracks
9. **Loader**: Asset import system
10. **Raycaster**: Intersection testing
11. **RenderTarget**: Off-screen rendering
12. **Shader**: Custom GPU programs
13. **Helper**: Debug visualization
14. **Audio**: 3D positional sound
15. **XRSession**: VR/AR support

**Design Highlights**:
- Type-safe sealed class hierarchies
- Platform abstraction via expect/actual
- Validation framework for runtime checks
- Disposal pattern for resource management
- Three.js API compatibility with Kotlin idioms

**Next Steps**:
- Phase 1: Generate contract tests for each entity
- Phase 2: Implement platform-specific graphics primitives
- Phase 3: Build rendering pipeline integrating all entities
