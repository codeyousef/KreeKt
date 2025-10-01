/**
 * Lighting API Contract
 *
 * This file defines the complete API surface for the lighting subsystem,
 * ensuring Three.js r180 compatibility with Kotlin idioms.
 *
 * Contract Design Principles:
 * - Type-safe method signatures
 * - Data classes for immutable configuration
 * - Default parameters for common use cases
 * - Sealed classes for type hierarchies
 * - Extension functions for DSL builders
 */

package io.kreekt.contracts.lighting

import io.kreekt.core.math.*
import io.kreekt.light.*
import io.kreekt.scene.*
import io.kreekt.camera.*
import io.kreekt.texture.*

// ============================================================================
// Core Light API
// ============================================================================

/**
 * Light: Base interface for all lights
 * Three.js equivalent: THREE.Light
 */
interface LightAPI : Object3DAPI {
    var color: Color
    var intensity: Float
    val isLight: Boolean

    fun dispose()
    fun copy(source: Light, recursive: Boolean = true): Light
}

// ============================================================================
// Basic Lights
// ============================================================================

/**
 * AmbientLight: Uniform lighting from all directions
 * Three.js equivalent: THREE.AmbientLight
 */
interface AmbientLightAPI : LightAPI {
    // Color and intensity inherited from Light
    val isAmbientLight: Boolean
}

/**
 * DirectionalLight: Parallel light rays (sun)
 * Three.js equivalent: THREE.DirectionalLight
 */
interface DirectionalLightAPI : LightAPI {
    val isDirectionalLight: Boolean
    val target: Object3D
    var shadow: DirectionalLightShadow
    val castShadow: Boolean

    fun dispose()
}

/**
 * PointLight: Omnidirectional light from single point
 * Three.js equivalent: THREE.PointLight
 */
interface PointLightAPI : LightAPI {
    val isPointLight: Boolean
    var distance: Float
    var decay: Float
    var shadow: PointLightShadow
    val castShadow: Boolean
    var power: Float  // Luminous power in lumens

    fun dispose()
}

/**
 * SpotLight: Cone-shaped light
 * Three.js equivalent: THREE.SpotLight
 */
interface SpotLightAPI : LightAPI {
    val isSpotLight: Boolean
    val target: Object3D
    var distance: Float
    var angle: Float  // Cone angle in radians
    var penumbra: Float  // Percent of spotlight cone attenuated due to penumbra
    var decay: Float
    var shadow: SpotLightShadow
    val castShadow: Boolean
    var power: Float  // Luminous power in lumens

    fun dispose()
}

/**
 * HemisphereLight: Gradient light between sky and ground colors
 * Three.js equivalent: THREE.HemisphereLight
 */
interface HemisphereLightAPI : LightAPI {
    val isHemisphereLight: Boolean
    var groundColor: Color
    val position: Vector3  // Direction from ground to sky
}

/**
 * RectAreaLight: Rectangular area light (no shadows)
 * Three.js equivalent: THREE.RectAreaLight
 */
interface RectAreaLightAPI : LightAPI {
    val isRectAreaLight: Boolean
    var width: Float
    var height: Float
    var power: Float  // Luminous power in lumens
}

/**
 * LightProbe: Light probe for image-based lighting
 * Three.js equivalent: THREE.LightProbe
 */
interface LightProbeAPI : LightAPI {
    val isLightProbe: Boolean
    val sh: SphericalHarmonics3  // Spherical harmonics coefficients

    companion object {
        fun fromCubeTexture(cubeTexture: CubeTexture): LightProbe
        fun fromCubeRenderTarget(
            renderer: WebGPURenderer,
            cubeRenderTarget: WebGPUCubeRenderTarget
        ): LightProbe
    }
}

// ============================================================================
// Shadow System
// ============================================================================

/**
 * LightShadow: Base shadow configuration
 * Three.js equivalent: THREE.LightShadow
 */
interface LightShadowAPI {
    val camera: Camera
    var bias: Float
    var normalBias: Float
    var radius: Float
    var blurSamples: Int
    val map: RenderTarget?
    val mapSize: Vector2
    val matrix: Matrix4
    var autoUpdate: Boolean
    var needsUpdate: Boolean

    fun updateMatrices(light: Light)
    fun getFrustum(): Frustum
    fun getViewportCount(): Int
    fun getFrameExtents(): Vector2
    fun dispose()
    fun copy(source: LightShadow): LightShadow
    fun clone(): LightShadow
}

/**
 * DirectionalLightShadow: Shadow for directional light
 * Three.js equivalent: THREE.DirectionalLightShadow
 */
interface DirectionalLightShadowAPI : LightShadowAPI {
    val isDirectionalLightShadow: Boolean
    override val camera: OrthographicCamera
}

/**
 * PointLightShadow: Shadow for point light (cubemap)
 * Three.js equivalent: THREE.PointLightShadow
 */
interface PointLightShadowAPI : LightShadowAPI {
    val isPointLightShadow: Boolean
    override val camera: PerspectiveCamera
}

/**
 * SpotLightShadow: Shadow for spot light
 * Three.js equivalent: THREE.SpotLightShadow
 */
interface SpotLightShadowAPI : LightShadowAPI {
    val isSpotLightShadow: Boolean
    override val camera: PerspectiveCamera
    var focus: Float  // Shadow camera focus for penumbra
}

// ============================================================================
// Image-Based Lighting
// ============================================================================

/**
 * SphericalHarmonics3: 3rd order spherical harmonics
 * Three.js equivalent: THREE.SphericalHarmonics3
 */
interface SphericalHarmonics3API {
    val coefficients: Array<Vector3>  // 9 coefficients (l=0,1,2)

    fun set(coefficients: Array<Vector3>): SphericalHarmonics3
    fun zero(): SphericalHarmonics3
    fun add(sh: SphericalHarmonics3): SphericalHarmonics3
    fun addScaledSH(sh: SphericalHarmonics3, s: Float): SphericalHarmonics3
    fun scale(s: Float): SphericalHarmonics3
    fun lerp(sh: SphericalHarmonics3, alpha: Float): SphericalHarmonics3
    fun equals(sh: SphericalHarmonics3): Boolean
    fun copy(sh: SphericalHarmonics3): SphericalHarmonics3
    fun clone(): SphericalHarmonics3
    fun fromArray(array: FloatArray, offset: Int = 0): SphericalHarmonics3
    fun toArray(array: FloatArray, offset: Int = 0): FloatArray

    // Utility methods
    fun getAt(normal: Vector3, target: Vector3): Vector3
    fun getIrradianceAt(normal: Vector3, target: Vector3): Vector3

    companion object {
        fun getBasisAt(normal: Vector3, shBasis: FloatArray)
    }
}

/**
 * LightProbeGenerator: Generate light probes from environment
 * Three.js equivalent: THREE.LightProbeGenerator
 */
interface LightProbeGeneratorAPI {
    companion object {
        fun fromCubeTexture(cubeTexture: CubeTexture): LightProbe
        fun fromCubeRenderTarget(
            renderer: WebGPURenderer,
            cubeRenderTarget: WebGPUCubeRenderTarget
        ): LightProbe
    }
}

// ============================================================================
// Light Helpers
// ============================================================================

/**
 * DirectionalLightHelper: Visualize directional light
 * Three.js equivalent: THREE.DirectionalLightHelper
 */
interface DirectionalLightHelperAPI : Object3DAPI {
    val light: DirectionalLight
    var color: Color?
    val lightPlane: Line
    val targetLine: Line

    fun update()
    fun dispose()
}

/**
 * PointLightHelper: Visualize point light
 * Three.js equivalent: THREE.PointLightHelper
 */
interface PointLightHelperAPI : Object3DAPI {
    val light: PointLight
    var color: Color?
    val size: Float

    fun update()
    fun dispose()
}

/**
 * SpotLightHelper: Visualize spot light
 * Three.js equivalent: THREE.SpotLightHelper
 */
interface SpotLightHelperAPI : Object3DAPI {
    val light: SpotLight
    var color: Color?
    val cone: LineSegments

    fun update()
    fun dispose()
}

/**
 * HemisphereLightHelper: Visualize hemisphere light
 * Three.js equivalent: THREE.HemisphereLightHelper
 */
interface HemisphereLightHelperAPI : Object3DAPI {
    val light: HemisphereLight
    val size: Float
    var color: Color?

    fun update()
    fun dispose()
}

/**
 * RectAreaLightHelper: Visualize rect area light
 * Three.js equivalent: THREE.RectAreaLightHelper
 */
interface RectAreaLightHelperAPI : Object3DAPI {
    val light: RectAreaLight
    var color: Color?

    fun update()
    fun dispose()
}

// ============================================================================
// Advanced Shadow Features
// ============================================================================

/**
 * CascadedShadowMap: Cascaded shadow mapping for large scenes
 */
interface CascadedShadowMapAPI {
    val light: DirectionalLight
    val cascades: Int
    val maxDistance: Float
    val cascadeDistribution: Float
    val shadowMapSize: Int
    val cameras: List<OrthographicCamera>
    val shadowMaps: List<RenderTarget>

    fun update(camera: Camera)
    fun getShadowMap(cascade: Int): RenderTarget
    fun getCamera(cascade: Int): OrthographicCamera
    fun dispose()
}

/**
 * VSM (Variance Shadow Maps) Configuration
 */
data class VSMShadowMapConfig(
    val enabled: Boolean = false,
    val minVariance: Float = 0.00001f,
    val lightBleedingReduction: Float = 0.0f
)

/**
 * PCF (Percentage Closer Filtering) Configuration
 */
data class PCFShadowMapConfig(
    val enabled: Boolean = true,
    val radius: Float = 1f,
    val samples: Int = 16
)

/**
 * PCSS (Percentage Closer Soft Shadows) Configuration
 */
data class PCSSShadowMapConfig(
    val enabled: Boolean = false,
    val lightRadius: Float = 1f,
    val searchRadius: Float = 5f,
    val pcfSamples: Int = 16
)

// ============================================================================
// Supporting Types
// ============================================================================

data class Frustum(
    val planes: Array<Plane>
) {
    fun setFromProjectionMatrix(m: Matrix4): Frustum
    fun intersectsObject(obj: Object3D): Boolean
    fun intersectsSphere(sphere: Sphere): Boolean
    fun intersectsBox(box: Box3): Boolean
}

// ============================================================================
// Factory Functions (Kotlin Idiom)
// ============================================================================

/**
 * DSL builder for DirectionalLight
 */
fun directionalLight(
    color: Color = Color(0xffffff),
    intensity: Float = 1f,
    init: DirectionalLight.() -> Unit = {}
): DirectionalLight {
    val light = DirectionalLight(color, intensity)
    light.init()
    return light
}

/**
 * DSL builder for PointLight
 */
fun pointLight(
    color: Color = Color(0xffffff),
    intensity: Float = 1f,
    distance: Float = 0f,
    decay: Float = 2f,
    init: PointLight.() -> Unit = {}
): PointLight {
    val light = PointLight(color, intensity, distance, decay)
    light.init()
    return light
}

/**
 * DSL builder for SpotLight
 */
fun spotLight(
    color: Color = Color(0xffffff),
    intensity: Float = 1f,
    distance: Float = 0f,
    angle: Float = Math.PI.toFloat() / 3f,
    penumbra: Float = 0f,
    decay: Float = 2f,
    init: SpotLight.() -> Unit = {}
): SpotLight {
    val light = SpotLight(color, intensity, distance, angle, penumbra, decay)
    light.init()
    return light
}

/**
 * DSL builder for AmbientLight
 */
fun ambientLight(
    color: Color = Color(0xffffff),
    intensity: Float = 1f,
    init: AmbientLight.() -> Unit = {}
): AmbientLight {
    val light = AmbientLight(color, intensity)
    light.init()
    return light
}

/**
 * DSL builder for HemisphereLight
 */
fun hemisphereLight(
    skyColor: Color = Color(0xffffff),
    groundColor: Color = Color(0x444444),
    intensity: Float = 1f,
    init: HemisphereLight.() -> Unit = {}
): HemisphereLight {
    val light = HemisphereLight(skyColor, groundColor, intensity)
    light.init()
    return light
}

/**
 * Extension function to enable shadows
 */
fun Light.enableShadows(
    mapSize: Int = 1024,
    bias: Float = 0f,
    normalBias: Float = 0f,
    radius: Float = 1f
): Light {
    when (this) {
        is DirectionalLight -> {
            castShadow = true
            shadow.mapSize.set(mapSize.toFloat(), mapSize.toFloat())
            shadow.bias = bias
            shadow.normalBias = normalBias
            shadow.radius = radius
        }
        is PointLight -> {
            castShadow = true
            shadow.mapSize.set(mapSize.toFloat(), mapSize.toFloat())
            shadow.bias = bias
            shadow.normalBias = normalBias
            shadow.radius = radius
        }
        is SpotLight -> {
            castShadow = true
            shadow.mapSize.set(mapSize.toFloat(), mapSize.toFloat())
            shadow.bias = bias
            shadow.normalBias = normalBias
            shadow.radius = radius
        }
    }
    return this
}

// ============================================================================
// Usage Examples
// ============================================================================

/**
 * Example: Create sun-like directional light with shadows
 */
fun exampleSunLight(): DirectionalLight {
    return directionalLight(
        color = Color(0xffffff),
        intensity = 1.0f
    ) {
        position.set(10f, 10f, 10f)
        target.position.set(0f, 0f, 0f)

        enableShadows(
            mapSize = 2048,
            bias = -0.0001f,
            normalBias = 0.02f
        )

        // Configure shadow camera frustum
        shadow.camera.apply {
            left = -10f
            right = 10f
            top = 10f
            bottom = -10f
            near = 0.5f
            far = 50f
        }
    }
}

/**
 * Example: Create three-point lighting setup
 */
fun exampleThreePointLighting(scene: Scene, target: Vector3 = Vector3(0f, 0f, 0f)) {
    // Key light (main light)
    val keyLight = directionalLight(
        color = Color(0xffffff),
        intensity = 1.0f
    ) {
        position.set(5f, 5f, 5f)
        this.target.position.copy(target)
    }
    scene.add(keyLight)

    // Fill light (soften shadows)
    val fillLight = directionalLight(
        color = Color(0xffffff),
        intensity = 0.4f
    ) {
        position.set(-5f, 0f, 5f)
        this.target.position.copy(target)
    }
    scene.add(fillLight)

    // Back light (rim light)
    val backLight = directionalLight(
        color = Color(0xffffff),
        intensity = 0.6f
    ) {
        position.set(0f, 5f, -5f)
        this.target.position.copy(target)
    }
    scene.add(backLight)
}

/**
 * Example: Create environment lighting with light probe
 */
fun exampleEnvironmentLighting(envMap: CubeTexture): LightProbe {
    return LightProbe.fromCubeTexture(envMap).apply {
        intensity = 1.0f
    }
}

/**
 * Example: Create cascaded shadow map for large outdoor scene
 */
fun exampleCascadedShadows(
    light: DirectionalLight,
    camera: Camera
): CascadedShadowMap {
    return CascadedShadowMap(
        light = light,
        cascades = 4,
        maxDistance = 100f,
        cascadeDistribution = 0.5f,
        shadowMapSize = 2048
    ).apply {
        update(camera)
    }
}

/**
 * Example: Create realistic indoor lighting
 */
fun exampleIndoorLighting(scene: Scene) {
    // Ambient light for base illumination
    scene.add(ambientLight(Color(0x404040), 0.3f))

    // Window light (directional)
    val windowLight = directionalLight(
        color = Color(0xffffee),
        intensity = 0.8f
    ) {
        position.set(10f, 10f, 5f)
        enableShadows(mapSize = 2048)
    }
    scene.add(windowLight)

    // Ceiling lights (point lights)
    for (i in -2..2) {
        val ceilingLight = pointLight(
            color = Color(0xffffff),
            intensity = 0.5f,
            distance = 10f,
            decay = 2f
        ) {
            position.set(i * 3f, 3f, 0f)
        }
        scene.add(ceilingLight)
    }
}
