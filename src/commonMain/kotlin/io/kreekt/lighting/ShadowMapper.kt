package io.kreekt.lighting

import io.kreekt.camera.Camera
import io.kreekt.camera.OrthographicCamera
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.core.math.Matrix4
import io.kreekt.core.math.Vector2
import io.kreekt.core.math.Vector3
import io.kreekt.core.scene.Material
import io.kreekt.core.scene.Object3D
import io.kreekt.core.scene.Scene
import io.kreekt.lighting.shadow.*
import io.kreekt.renderer.Texture
import kotlin.math.PI

/**
 * High-quality shadow mapping implementation
 * This file serves as the public API entry point.
 * Implementation is in io.kreekt.lighting.shadow package.
 */
class ShadowMapperImpl : ShadowMapper {

    private val frustumCalculator = FrustumCalculator()
    private val shadowGenerator = ShadowGenerator(frustumCalculator)
    private val shadowSampler = ShadowSampler()

    private var shadowNormalBias: Float = 0.01f
    private var cullingDistance: Float = 100.0f
    private var lodBias: Float = 0.0f
    private var shadowLODEnabled: Boolean = true

    private var maxShadowMaps: Int = 8
    private var shadowMapPool: MutableList<io.kreekt.lighting.ShadowMap> = mutableListOf()
    private var cascadedShadowMapPool: MutableList<io.kreekt.lighting.CascadedShadowMap> = mutableListOf()
    private var cubeShadowMapPool: MutableList<io.kreekt.lighting.CubeShadowMap> = mutableListOf()

    override suspend fun renderShadowMap(
        light: Light,
        scene: Scene,
        camera: Camera,
        objects: List<Object3D>
    ): ShadowResult<Texture> {
        return try {
            when (light) {
                is DirectionalLight -> {
                    val result = shadowGenerator.generateDirectionalShadowMap(light, scene)
                    when (result) {
                        is ShadowResult.Success -> ShadowResult.Success(result.data.texture)
                        is ShadowResult.Error -> result
                    }
                }
                is SpotLight -> {
                    val result = shadowGenerator.generateSpotShadowMap(light, scene)
                    when (result) {
                        is ShadowResult.Success -> ShadowResult.Success(result.data.texture)
                        is ShadowResult.Error -> result
                    }
                }
                else -> ShadowResult.Error(UnsupportedShadowType("Unsupported light type for shadow mapping"))
            }
        } catch (e: Exception) {
            ShadowResult.Error(ShadowMapGenerationFailed("Failed to render shadow map: ${e.message}"))
        }
    }

    override fun getShadowMatrix(light: Light, camera: Camera): Matrix4 {
        return when (light) {
            is DirectionalLight -> {
                val frustum = frustumCalculator.calculateDirectionalLightFrustumFromCamera(light, camera)
                val lightCamera = OrthographicCamera(
                    left = frustum.left,
                    right = frustum.right,
                    top = frustum.top,
                    bottom = frustum.bottom,
                    near = frustum.near,
                    far = frustum.far
                )
                lightCamera.position.copy(light.position)
                lightCamera.lookAt(light.position + light.direction)
                lightCamera.updateMatrixWorld()

                val biasMatrix = Matrix4().set(
                    0.5f, 0.0f, 0.0f, 0.5f,
                    0.0f, 0.5f, 0.0f, 0.5f,
                    0.0f, 0.0f, 0.5f, 0.5f,
                    0.0f, 0.0f, 0.0f, 1.0f
                )
                biasMatrix.multiply(lightCamera.projectionMatrix).multiply(lightCamera.matrixWorldInverse)
            }

            is SpotLight -> {
                val lightCamera = PerspectiveCamera(
                    fov = light.angle * 2f * 180f / PI.toFloat(),
                    aspect = 1f,
                    near = 0.1f,
                    far = light.distance
                )
                lightCamera.position.copy(light.position)
                lightCamera.lookAt(light.position + light.direction)
                lightCamera.updateMatrixWorld()

                val biasMatrix = Matrix4().set(
                    0.5f, 0.0f, 0.0f, 0.5f,
                    0.0f, 0.5f, 0.0f, 0.5f,
                    0.0f, 0.0f, 0.5f, 0.5f,
                    0.0f, 0.0f, 0.0f, 1.0f
                )
                biasMatrix.multiply(lightCamera.projectionMatrix).multiply(lightCamera.matrixWorldInverse)
            }

            else -> Matrix4.identity()
        }
    }

    override fun updateShadowUniforms(light: Light, material: Material) {
        val shadowMatrix = getShadowMatrix(light, PerspectiveCamera())

        material.setUniform("shadowMatrix", shadowMatrix)
        material.setUniform("shadowBias", shadowSampler)
        material.setUniform("shadowNormalBias", shadowNormalBias)
        material.setUniform("shadowRadius", shadowSampler)
        material.setUniform(
            "shadowMapSize", Vector2(
                shadowMapPool.firstOrNull()?.texture?.width?.toFloat() ?: 1024f,
                shadowMapPool.firstOrNull()?.texture?.height?.toFloat() ?: 1024f
            )
        )

        material.setUniform("shadowFilter", 0)
        material.setUniform("shadowSoftness", 1.0f)

        when (light) {
            is DirectionalLight -> {
                material.setUniform("shadowCascadeCount", 4)
                material.setUniform("shadowCascadeSplits", frustumCalculator.calculateCascadeSplits(PerspectiveCamera(), 4))
            }
            is SpotLight -> {
                material.setUniform("shadowLightPosition", light.position)
                material.setUniform("shadowLightDirection", light.direction)
            }
        }
    }

    suspend fun generateShadowMap(light: Light, scene: Scene): ShadowResult<io.kreekt.lighting.ShadowMap> {
        return try {
            when (light) {
                is DirectionalLight -> shadowGenerator.generateDirectionalShadowMap(light, scene)
                is SpotLight -> shadowGenerator.generateSpotShadowMap(light, scene)
                else -> ShadowResult.Error(UnsupportedShadowType("Unsupported light type"))
            }
        } catch (e: Exception) {
            ShadowResult.Error(ShadowMapGenerationFailed("Failed to generate shadow map: ${e.message}"))
        }
    }

    suspend fun generateCascadedShadowMap(
        light: DirectionalLight,
        scene: Scene,
        camera: Camera,
        cascadeCount: Int
    ): ShadowResult<io.kreekt.lighting.CascadedShadowMap> {
        return shadowGenerator.generateCascadedShadowMap(light, scene, camera, cascadeCount)
    }

    suspend fun generateOmnidirectionalShadowMap(
        light: PointLight,
        scene: Scene
    ): ShadowResult<io.kreekt.lighting.CubeShadowMap> {
        return shadowGenerator.generateOmnidirectionalShadowMap(light, scene)
    }

    fun setShadowFilter(filter: ShadowFilter) {
        shadowSampler.setShadowFilter(filter)
    }

    fun setShadowBias(bias: Float) {
        shadowSampler.setShadowBias(bias)
        shadowGenerator.setShadowBias(bias)
    }

    fun setShadowRadius(radius: Float) {
        shadowSampler.setShadowRadius(radius)
    }

    fun setShadowNormalBias(bias: Float) {
        this.shadowNormalBias = bias
    }

    fun setCullingDistance(distance: Float) {
        this.cullingDistance = distance
    }

    fun setLODBias(bias: Float) {
        this.lodBias = bias
    }

    fun enableShadowLOD(enabled: Boolean) {
        this.shadowLODEnabled = enabled
    }

    fun sampleShadowMap(shadowMap: io.kreekt.lighting.ShadowMap, worldPosition: Vector3, normal: Vector3): Float {
        return shadowSampler.sampleShadowMap(shadowMap, worldPosition, normal)
    }

    fun sampleShadowPoisson(shadowMap: io.kreekt.lighting.ShadowMap, coords: Vector3): Float {
        return shadowSampler.sampleShadowPoisson(shadowMap, coords)
    }
}

/**
 * Extension properties for lights
 */
var Light.shadowQuality: ShadowQuality
    get() = ShadowQuality.MEDIUM
    set(value) { /* Store in light properties */ }

/**
 * Extension function for Material to set uniforms
 */
fun Material.setUniform(name: String, value: Any) {
    // Placeholder implementation
}

// Re-export shadow quality
typealias ShadowQuality = io.kreekt.lighting.shadow.ShadowQuality
