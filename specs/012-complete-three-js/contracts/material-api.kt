/**
 * Material API Contract
 *
 * This file defines the complete API surface for the material subsystem,
 * ensuring Three.js r180 compatibility with Kotlin idioms.
 *
 * Contract Design Principles:
 * - Type-safe method signatures
 * - Data classes for immutable configuration
 * - Default parameters for common use cases
 * - Sealed classes for type hierarchies
 * - Extension functions for DSL builders
 */

package io.kreekt.contracts.material

import io.kreekt.core.math.*
import io.kreekt.material.*
import io.kreekt.texture.*

// ============================================================================
// Core Material API
// ============================================================================

/**
 * Material: Base interface for all materials
 * Three.js equivalent: THREE.Material
 */
interface MaterialAPI {
    // Identity
    val id: Int
    var name: String
    val type: String
    val uuid: String

    // Visibility and rendering
    var visible: Boolean
    var side: Side
    var transparent: Boolean
    var opacity: Float
    var blending: Blending
    var blendSrc: BlendingFactor
    var blendDst: BlendingFactor
    var blendEquation: BlendingEquation
    var blendSrcAlpha: BlendingFactor?
    var blendDstAlpha: BlendingFactor?
    var blendEquationAlpha: BlendingEquation?
    var premultipliedAlpha: Boolean

    // Depth testing and writing
    var depthTest: Boolean
    var depthWrite: Boolean
    var depthFunc: DepthMode

    // Stencil operations
    var stencilWrite: Boolean
    var stencilFunc: StencilFunc
    var stencilRef: Int
    var stencilWriteMask: Int
    var stencilFuncMask: Int
    var stencilFail: StencilOp
    var stencilZFail: StencilOp
    var stencilZPass: StencilOp

    // Culling and clipping
    var colorWrite: Boolean
    var polygonOffset: Boolean
    var polygonOffsetFactor: Float
    var polygonOffsetUnits: Float
    var clippingPlanes: List<Plane>?
    var clipIntersection: Boolean
    var clipShadows: Boolean

    // Alpha testing and dithering
    var alphaTest: Float
    var alphaToCoverage: Boolean
    var dithering: Boolean

    // Precision
    var precision: Precision?

    // Tone mapping
    var toneMapped: Boolean

    // User data
    val userData: MutableMap<String, Any>

    // Version tracking
    var version: Int
    var needsUpdate: Boolean

    // Methods
    fun clone(): Material
    fun copy(source: Material): Material
    fun dispose()
    fun setValues(values: Map<String, Any>)
}

// ============================================================================
// Basic Materials
// ============================================================================

/**
 * MeshBasicMaterial: Simple material with color/texture
 * Three.js equivalent: THREE.MeshBasicMaterial
 */
interface MeshBasicMaterialAPI : MaterialAPI {
    var color: Color
    var map: Texture?
    var lightMap: Texture?
    var lightMapIntensity: Float
    var aoMap: Texture?
    var aoMapIntensity: Float
    var specularMap: Texture?
    var alphaMap: Texture?
    var envMap: Texture?
    var combine: Combine
    var reflectivity: Float
    var refractionRatio: Float
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var wireframeLinecap: String
    var wireframeLinejoin: String
    var fog: Boolean
}

/**
 * MeshLambertMaterial: Lambert shading (non-specular diffuse)
 * Three.js equivalent: THREE.MeshLambertMaterial
 */
interface MeshLambertMaterialAPI : MaterialAPI {
    var color: Color
    var emissive: Color
    var emissiveIntensity: Float
    var emissiveMap: Texture?
    var map: Texture?
    var lightMap: Texture?
    var lightMapIntensity: Float
    var aoMap: Texture?
    var aoMapIntensity: Float
    var specularMap: Texture?
    var alphaMap: Texture?
    var envMap: Texture?
    var combine: Combine
    var reflectivity: Float
    var refractionRatio: Float
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var wireframeLinecap: String
    var wireframeLinejoin: String
    var fog: Boolean
}

/**
 * MeshPhongMaterial: Phong shading (specular highlights)
 * Three.js equivalent: THREE.MeshPhongMaterial
 */
interface MeshPhongMaterialAPI : MaterialAPI {
    var color: Color
    var emissive: Color
    var emissiveIntensity: Float
    var emissiveMap: Texture?
    var specular: Color
    var shininess: Float
    var map: Texture?
    var lightMap: Texture?
    var lightMapIntensity: Float
    var aoMap: Texture?
    var aoMapIntensity: Float
    var bumpMap: Texture?
    var bumpScale: Float
    var normalMap: Texture?
    var normalMapType: NormalMapType
    var normalScale: Vector2
    var displacementMap: Texture?
    var displacementScale: Float
    var displacementBias: Float
    var specularMap: Texture?
    var alphaMap: Texture?
    var envMap: Texture?
    var combine: Combine
    var reflectivity: Float
    var refractionRatio: Float
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var wireframeLinecap: String
    var wireframeLinejoin: String
    var flatShading: Boolean
    var fog: Boolean
}

// ============================================================================
// PBR Materials
// ============================================================================

/**
 * MeshStandardMaterial: PBR material with metalness/roughness
 * Three.js equivalent: THREE.MeshStandardMaterial
 */
interface MeshStandardMaterialAPI : MaterialAPI {
    var color: Color
    var roughness: Float
    var metalness: Float
    var map: Texture?
    var lightMap: Texture?
    var lightMapIntensity: Float
    var aoMap: Texture?
    var aoMapIntensity: Float
    var emissive: Color
    var emissiveIntensity: Float
    var emissiveMap: Texture?
    var bumpMap: Texture?
    var bumpScale: Float
    var normalMap: Texture?
    var normalMapType: NormalMapType
    var normalScale: Vector2
    var displacementMap: Texture?
    var displacementScale: Float
    var displacementBias: Float
    var roughnessMap: Texture?
    var metalnessMap: Texture?
    var alphaMap: Texture?
    var envMap: Texture?
    var envMapIntensity: Float
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var flatShading: Boolean
    var fog: Boolean
}

/**
 * MeshPhysicalMaterial: Extended PBR with clearcoat, transmission, etc.
 * Three.js equivalent: THREE.MeshPhysicalMaterial
 */
interface MeshPhysicalMaterialAPI : MeshStandardMaterialAPI {
    // Clearcoat
    var clearcoat: Float
    var clearcoatMap: Texture?
    var clearcoatRoughness: Float
    var clearcoatRoughnessMap: Texture?
    var clearcoatNormalMap: Texture?
    var clearcoatNormalScale: Vector2

    // Transmission
    var transmission: Float
    var transmissionMap: Texture?
    var thickness: Float
    var thicknessMap: Texture?
    var attenuationDistance: Float
    var attenuationColor: Color

    // Sheen
    var sheen: Float
    var sheenRoughness: Float
    var sheenRoughnessMap: Texture?
    var sheenColor: Color
    var sheenColorMap: Texture?

    // Iridescence
    var iridescence: Float
    var iridescenceMap: Texture?
    var iridescenceIOR: Float
    var iridescenceThicknessRange: Pair<Float, Float>
    var iridescenceThicknessMap: Texture?

    // Specular
    var specularIntensity: Float
    var specularIntensityMap: Texture?
    var specularColor: Color
    var specularColorMap: Texture?

    // IOR
    var ior: Float

    // Anisotropy
    var anisotropy: Float
    var anisotropyRotation: Float
    var anisotropyMap: Texture?
}

/**
 * MeshToonMaterial: Cel/toon shading
 * Three.js equivalent: THREE.MeshToonMaterial
 */
interface MeshToonMaterialAPI : MaterialAPI {
    var color: Color
    var map: Texture?
    var gradientMap: Texture?
    var lightMap: Texture?
    var lightMapIntensity: Float
    var aoMap: Texture?
    var aoMapIntensity: Float
    var emissive: Color
    var emissiveIntensity: Float
    var emissiveMap: Texture?
    var bumpMap: Texture?
    var bumpScale: Float
    var normalMap: Texture?
    var normalMapType: NormalMapType
    var normalScale: Vector2
    var displacementMap: Texture?
    var displacementScale: Float
    var displacementBias: Float
    var alphaMap: Texture?
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var wireframeLinecap: String
    var wireframeLinejoin: String
    var fog: Boolean
}

// ============================================================================
// Utility Materials
// ============================================================================

/**
 * MeshNormalMaterial: Color based on surface normals
 * Three.js equivalent: THREE.MeshNormalMaterial
 */
interface MeshNormalMaterialAPI : MaterialAPI {
    var bumpMap: Texture?
    var bumpScale: Float
    var normalMap: Texture?
    var normalMapType: NormalMapType
    var normalScale: Vector2
    var displacementMap: Texture?
    var displacementScale: Float
    var displacementBias: Float
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var flatShading: Boolean
    var fog: Boolean
}

/**
 * MeshDepthMaterial: Encode depth into color
 * Three.js equivalent: THREE.MeshDepthMaterial
 */
interface MeshDepthMaterialAPI : MaterialAPI {
    var depthPacking: DepthPacking
    var map: Texture?
    var alphaMap: Texture?
    var displacementMap: Texture?
    var displacementScale: Float
    var displacementBias: Float
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var fog: Boolean
}

/**
 * MeshDistanceMaterial: Distance from camera
 * Three.js equivalent: THREE.MeshDistanceMaterial
 */
interface MeshDistanceMaterialAPI : MaterialAPI {
    var referencePosition: Vector3
    var nearDistance: Float
    var farDistance: Float
    var map: Texture?
    var alphaMap: Texture?
    var displacementMap: Texture?
    var displacementScale: Float
    var displacementBias: Float
    var fog: Boolean
}

/**
 * MeshMatcapMaterial: Matcap (material capture) shading
 * Three.js equivalent: THREE.MeshMatcapMaterial
 */
interface MeshMatcapMaterialAPI : MaterialAPI {
    var color: Color
    var matcap: Texture?
    var map: Texture?
    var bumpMap: Texture?
    var bumpScale: Float
    var normalMap: Texture?
    var normalMapType: NormalMapType
    var normalScale: Vector2
    var displacementMap: Texture?
    var displacementScale: Float
    var displacementBias: Float
    var alphaMap: Texture?
    var flatShading: Boolean
    var fog: Boolean
}

/**
 * ShadowMaterial: Receive shadows only
 * Three.js equivalent: THREE.ShadowMaterial
 */
interface ShadowMaterialAPI : MaterialAPI {
    var color: Color
    var fog: Boolean
}

// ============================================================================
// Line Materials
// ============================================================================

/**
 * LineBasicMaterial: Basic line rendering
 * Three.js equivalent: THREE.LineBasicMaterial
 */
interface LineBasicMaterialAPI : MaterialAPI {
    var color: Color
    var linewidth: Float
    var linecap: String
    var linejoin: String
    var fog: Boolean
}

/**
 * LineDashedMaterial: Dashed line rendering
 * Three.js equivalent: THREE.LineDashedMaterial
 */
interface LineDashedMaterialAPI : LineBasicMaterialAPI {
    var scale: Float
    var dashSize: Float
    var gapSize: Float
}

// ============================================================================
// Point and Sprite Materials
// ============================================================================

/**
 * PointsMaterial: Point cloud rendering
 * Three.js equivalent: THREE.PointsMaterial
 */
interface PointsMaterialAPI : MaterialAPI {
    var color: Color
    var map: Texture?
    var alphaMap: Texture?
    var size: Float
    var sizeAttenuation: Boolean
    var fog: Boolean
}

/**
 * SpriteMaterial: Billboard sprite rendering
 * Three.js equivalent: THREE.SpriteMaterial
 */
interface SpriteMaterialAPI : MaterialAPI {
    var color: Color
    var map: Texture?
    var alphaMap: Texture?
    var rotation: Float
    var sizeAttenuation: Boolean
    var fog: Boolean
}

// ============================================================================
// Shader Materials
// ============================================================================

/**
 * ShaderMaterial: Custom WGSL shader material
 * Three.js equivalent: THREE.ShaderMaterial
 */
interface ShaderMaterialAPI : MaterialAPI {
    var vertexShader: String
    var fragmentShader: String
    var uniforms: MutableMap<String, Uniform>
    var defines: MutableMap<String, Any>
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var lights: Boolean
    var clipping: Boolean
    var fog: Boolean
    var flatShading: Boolean
    var extensions: ShaderExtensions
}

/**
 * RawShaderMaterial: Custom shader without built-in uniforms
 * Three.js equivalent: THREE.RawShaderMaterial
 */
interface RawShaderMaterialAPI : ShaderMaterialAPI {
    // No additional properties, just disables built-in uniforms
}

// ============================================================================
// Supporting Types
// ============================================================================

enum class Side {
    FrontSide,
    BackSide,
    DoubleSide
}

enum class Blending {
    NoBlending,
    NormalBlending,
    AdditiveBlending,
    SubtractiveBlending,
    MultiplyBlending,
    CustomBlending
}

enum class BlendingFactor {
    ZeroFactor,
    OneFactor,
    SrcColorFactor,
    OneMinusSrcColorFactor,
    SrcAlphaFactor,
    OneMinusSrcAlphaFactor,
    DstAlphaFactor,
    OneMinusDstAlphaFactor,
    DstColorFactor,
    OneMinusDstColorFactor,
    SrcAlphaSaturateFactor
}

enum class BlendingEquation {
    AddEquation,
    SubtractEquation,
    ReverseSubtractEquation,
    MinEquation,
    MaxEquation
}

enum class DepthMode {
    NeverDepth,
    AlwaysDepth,
    LessDepth,
    LessEqualDepth,
    EqualDepth,
    GreaterEqualDepth,
    GreaterDepth,
    NotEqualDepth
}

enum class StencilFunc {
    NeverStencilFunc,
    LessStencilFunc,
    EqualStencilFunc,
    LessEqualStencilFunc,
    GreaterStencilFunc,
    NotEqualStencilFunc,
    GreaterEqualStencilFunc,
    AlwaysStencilFunc
}

enum class StencilOp {
    ZeroStencilOp,
    KeepStencilOp,
    ReplaceStencilOp,
    IncrementStencilOp,
    DecrementStencilOp,
    IncrementWrapStencilOp,
    DecrementWrapStencilOp,
    InvertStencilOp
}

enum class Combine {
    MultiplyOperation,
    MixOperation,
    AddOperation
}

enum class NormalMapType {
    TangentSpaceNormalMap,
    ObjectSpaceNormalMap
}

enum class DepthPacking {
    BasicDepthPacking,
    RGBADepthPacking
}

enum class Precision {
    HighP,
    MediumP,
    LowP
}

data class Uniform(
    var value: Any,
    val type: UniformType? = null
)

enum class UniformType {
    Float,
    Int,
    Vector2,
    Vector3,
    Vector4,
    Color,
    Matrix3,
    Matrix4,
    Texture,
    CubeTexture
}

data class ShaderExtensions(
    var derivatives: Boolean = false,
    var fragDepth: Boolean = false,
    var drawBuffers: Boolean = false,
    var shaderTextureLOD: Boolean = false
)

// ============================================================================
// Factory Functions (Kotlin Idiom)
// ============================================================================

/**
 * DSL builder for MeshStandardMaterial
 */
fun meshStandardMaterial(init: MeshStandardMaterial.() -> Unit): MeshStandardMaterial {
    val material = MeshStandardMaterial()
    material.init()
    return material
}

/**
 * DSL builder for MeshPhysicalMaterial
 */
fun meshPhysicalMaterial(init: MeshPhysicalMaterial.() -> Unit): MeshPhysicalMaterial {
    val material = MeshPhysicalMaterial()
    material.init()
    return material
}

/**
 * DSL builder for MeshBasicMaterial
 */
fun meshBasicMaterial(init: MeshBasicMaterial.() -> Unit): MeshBasicMaterial {
    val material = MeshBasicMaterial()
    material.init()
    return material
}

/**
 * DSL builder for ShaderMaterial
 */
fun shaderMaterial(init: ShaderMaterial.() -> Unit): ShaderMaterial {
    val material = ShaderMaterial()
    material.init()
    return material
}

/**
 * Extension function for uniform setting
 */
fun ShaderMaterial.setUniform(name: String, value: Any, type: UniformType? = null) {
    uniforms[name] = Uniform(value, type)
}

// ============================================================================
// Usage Examples
// ============================================================================

/**
 * Example: Create PBR material with textures
 */
fun examplePBRMaterial(
    albedoMap: Texture,
    normalMap: Texture,
    roughnessMap: Texture,
    metalnessMap: Texture
): MeshStandardMaterial {
    return meshStandardMaterial {
        map = albedoMap
        this.normalMap = normalMap
        this.roughnessMap = roughnessMap
        this.metalnessMap = metalnessMap
        normalScale = Vector2(1f, 1f)
    }
}

/**
 * Example: Create glass material with transmission
 */
fun exampleGlassMaterial(): MeshPhysicalMaterial {
    return meshPhysicalMaterial {
        transmission = 1f
        thickness = 0.5f
        roughness = 0.1f
        metalness = 0f
        ior = 1.5f
        transparent = true
    }
}

/**
 * Example: Create custom shader material
 */
fun exampleCustomShader(): ShaderMaterial {
    return shaderMaterial {
        vertexShader = """
            @vertex
            fn main(@location(0) position: vec3<f32>) -> @builtin(position) vec4<f32> {
                return projectionMatrix * modelViewMatrix * vec4<f32>(position, 1.0);
            }
        """.trimIndent()

        fragmentShader = """
            @fragment
            fn main() -> @location(0) vec4<f32> {
                return vec4<f32>(color, 1.0);
            }
        """.trimIndent()

        setUniform("color", Vector3(1f, 0f, 0f), UniformType.Vector3)
    }
}

/**
 * Example: Create cel-shaded material
 */
fun exampleToonMaterial(gradientMap: Texture): MeshToonMaterial {
    return MeshToonMaterial().apply {
        color = Color(0x00ff00)
        this.gradientMap = gradientMap
    }
}
