/**
 * Advanced Shadow Mapping System
 * Provides cascaded shadow maps, omnidirectional shadows, and various filtering techniques
 */
package io.kreekt.lighting

import io.kreekt.core.math.*
import io.kreekt.core.math.Box3
import io.kreekt.core.math.Vector2
import io.kreekt.core.scene.Scene
import io.kreekt.core.scene.Object3D
import io.kreekt.core.scene.Material
import io.kreekt.renderer.*
import io.kreekt.camera.Camera
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.camera.OrthographicCamera
import kotlin.math.*
import io.kreekt.renderer.TextureFilter
import io.kreekt.renderer.TextureFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-quality shadow mapping implementation with multiple techniques
 */
class ShadowMapperImpl : ShadowMapper {

    private var shadowFilter: ShadowFilter = ShadowFilter.PCF
    private var shadowBias: Float = 0.0005f
    private var shadowRadius: Float = 1.0f
    private var shadowNormalBias: Float = 0.01f
    private var cullingDistance: Float = 100.0f
    private var lodBias: Float = 0.0f
    private var shadowLODEnabled: Boolean = true

    // Performance optimization settings
    private var maxShadowMaps: Int = 8
    private var shadowMapPool: MutableList<ShadowMapImpl> = mutableListOf()
    private var cascadedShadowMapPool: MutableList<CascadedShadowMapImpl> = mutableListOf()
    private var cubeShadowMapPool: MutableList<CubeShadowMapImpl> = mutableListOf()

    override suspend fun renderShadowMap(light: Light, scene: Scene, camera: Camera, objects: List<Object3D>): ShadowResult<Texture> {
        return try {
            when (light) {
                is DirectionalLight -> {
                    val result = generateDirectionalShadowMap(light, scene)
                    when (result) {
                        is ShadowResult.Success -> ShadowResult.Success(result.data.texture)
                        is ShadowResult.Error -> result
                    }
                }
                is SpotLight -> {
                    val result = generateSpotShadowMap(light, scene)
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
        // Calculate shadow matrix based on light type
        return when (light) {
            is DirectionalLight -> {
                // For directional lights, create orthographic projection
                // Use scene-based frustum calculation (we'll need a default scene)
                val frustum = calculateDirectionalLightFrustumFromCamera(light, camera)
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

                // Shadow matrix = bias * projection * view
                val biasMatrix = Matrix4().set(
                    0.5f, 0.0f, 0.0f, 0.5f,
                    0.0f, 0.5f, 0.0f, 0.5f,
                    0.0f, 0.0f, 0.5f, 0.5f,
                    0.0f, 0.0f, 0.0f, 1.0f
                )
                biasMatrix.multiply(lightCamera.projectionMatrix).multiply(lightCamera.matrixWorldInverse)
            }

            is SpotLight -> {
                // For spot lights, create perspective projection
                val lightCamera = PerspectiveCamera(
                    fov = light.angle * 2f * 180f / PI.toFloat(),
                    aspect = 1f,
                    near = 0.1f,
                    far = light.distance
                )
                lightCamera.position.copy(light.position)
                lightCamera.lookAt(light.position + light.direction)
                lightCamera.updateMatrixWorld()

                // Shadow matrix = bias * projection * view
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
        // Update material uniforms for shadow mapping
        val shadowMatrix = getShadowMatrix(light, PerspectiveCamera()) // Default camera for uniform calculation

        // Set shadow uniforms on the material
        material.setUniform("shadowMatrix", shadowMatrix)
        material.setUniform("shadowBias", shadowBias)
        material.setUniform("shadowNormalBias", shadowNormalBias)
        material.setUniform("shadowRadius", shadowRadius)
        material.setUniform(
            "shadowMapSize", Vector2(
                shadowMapPool.firstOrNull()?.texture?.width?.toFloat() ?: 1024f,
                shadowMapPool.firstOrNull()?.texture?.height?.toFloat() ?: 1024f
            )
        )

        // Set shadow filtering parameters
        material.setUniform("shadowFilter", shadowFilter.ordinal)
        material.setUniform("shadowSoftness", shadowRadius)

        // Set light-specific shadow parameters
        when (light) {
            is DirectionalLight -> {
                material.setUniform("shadowCascadeCount", 4)
                material.setUniform("shadowCascadeSplits", calculateCascadeSplits(PerspectiveCamera(), 4))
            }

            is SpotLight -> {
                material.setUniform("shadowLightPosition", light.position)
                material.setUniform("shadowLightDirection", light.direction)
            }
        }
    }

    suspend fun generateShadowMap(light: Light, scene: Scene): ShadowResult<ShadowMap> {
        return try {
            when (light) {
                is DirectionalLight -> generateDirectionalShadowMap(light, scene)
                is SpotLight -> generateSpotShadowMap(light, scene)
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
    ): ShadowResult<CascadedShadowMap> {
        return try {

            val cascades = calculateCascadeSplits(camera, cascadeCount)
            val shadowMaps = mutableListOf<ShadowCascade>()

            for (i in 0 until cascadeCount) {
                val cascade = generateCascade(light, scene, camera, cascades[i], cascades[i + 1])
                shadowMaps.add(cascade)
            }

            val cascadedMap = CascadedShadowMapImpl(
                cascades = shadowMaps,
                splitDistances = cascades,
                texture = createCascadedTexture(shadowMaps),
                lightSpaceMatrix = Matrix4.identity(),
                near = camera.near,
                far = camera.far,
                bias = shadowBias
            )

            ShadowResult.Success(cascadedMap)
        } catch (e: Exception) {
            ShadowResult.Error(ShadowMapGenerationFailed("Failed to generate cascaded shadow map: ${e.message}"))
        }
    }

    suspend fun generateOmnidirectionalShadowMap(
        light: PointLight,
        scene: Scene
    ): ShadowResult<CubeShadowMap> {
        return try {
            val shadowMapSize = 1024 // Configurable
            val near = 0.1f
            val far = light.distance

            // Generate 6 shadow maps for cube faces
            val faceTextures = Array(6) { createShadowTexture(shadowMapSize, shadowMapSize) }

            val cubeDirections = arrayOf(
                Vector3(1f, 0f, 0f),   // Positive X
                Vector3(-1f, 0f, 0f),  // Negative X
                Vector3(0f, 1f, 0f),   // Positive Y
                Vector3(0f, -1f, 0f),  // Negative Y
                Vector3(0f, 0f, 1f),   // Positive Z
                Vector3(0f, 0f, -1f)   // Negative Z
            )

            val cubeUps = arrayOf(
                Vector3(0f, -1f, 0f),  // Positive X
                Vector3(0f, -1f, 0f),  // Negative X
                Vector3(0f, 0f, 1f),   // Positive Y
                Vector3(0f, 0f, -1f),  // Negative Y
                Vector3(0f, -1f, 0f),  // Positive Z
                Vector3(0f, -1f, 0f)   // Negative Z
            )

            // Get light position from its world transform
            val lightPos = light.position

            for (face in 0 until 6) {
                val camera = PerspectiveCamera(
                    fov = 90f,
                    aspect = 1f,
                    near = near,
                    far = far
                )

                camera.position.copy(lightPos)
                camera.lookAt(lightPos + cubeDirections[face])
                camera.updateMatrixWorld()

                // Render depth from this face
                renderDepthToTexture(scene, camera, faceTextures[face])
            }

            val cubeMap = CubeShadowMapImpl(
                textures = faceTextures,
                lightPosition = lightPos,
                near = near,
                far = far
            )

            ShadowResult.Success(cubeMap)
        } catch (e: Exception) {
            ShadowResult.Error(ShadowMapGenerationFailed("Failed to generate omnidirectional shadow map: ${e.message}"))
        }
    }

    fun setShadowFilter(filter: ShadowFilter) {
        this.shadowFilter = filter
    }

    fun setShadowBias(bias: Float) {
        this.shadowBias = bias
    }

    fun setShadowRadius(radius: Float) {
        this.shadowRadius = radius
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

    /**
     * Generate shadow map for directional light
     */
    private suspend fun generateDirectionalShadowMap(
        light: DirectionalLight,
        scene: Scene
    ): ShadowResult<ShadowMap> {
        val shadowMapSize = calculateShadowMapSize(light, scene)
        val frustum = calculateDirectionalLightFrustum(light, scene)

        val lightCamera = OrthographicCamera(
            left = frustum.left,
            right = frustum.right,
            top = frustum.top,
            bottom = frustum.bottom,
            near = frustum.near,
            far = frustum.far
        )

        // Position camera at light position looking in light direction
        val lightPos = light.position
        lightCamera.position.copy(lightPos)
        lightCamera.lookAt(lightPos + light.direction)
        lightCamera.updateMatrixWorld()

        val texture = createShadowTexture(shadowMapSize, shadowMapSize)
        renderDepthToTexture(scene, lightCamera, texture)

        val shadowMap = ShadowMapImpl(
            texture = texture,
            lightSpaceMatrix = lightCamera.projectionMatrix.multiply(lightCamera.matrixWorldInverse),
            near = frustum.near,
            far = frustum.far,
            bias = shadowBias
        )

        return ShadowResult.Success(shadowMap)
    }

    /**
     * Generate shadow map for spot light
     */
    private suspend fun generateSpotShadowMap(
        light: SpotLight,
        scene: Scene
    ): ShadowResult<ShadowMap> {
        val shadowMapSize = calculateShadowMapSize(light, scene)

        val lightCamera = PerspectiveCamera(
            fov = light.angle * 2f * 180f / PI.toFloat(),
            aspect = 1f,
            near = 0.1f,
            far = light.distance
        )

        // Get light position from its world transform
        val lightPos = light.position
        lightCamera.position.copy(lightPos)
        lightCamera.lookAt(lightPos + light.direction)
        lightCamera.updateMatrixWorld()

        val texture = createShadowTexture(shadowMapSize, shadowMapSize)
        renderDepthToTexture(scene, lightCamera, texture)

        val shadowMap = ShadowMapImpl(
            texture = texture,
            lightSpaceMatrix = lightCamera.projectionMatrix.multiply(lightCamera.matrixWorldInverse),
            near = 0.1f,
            far = light.distance,
            bias = shadowBias
        )

        return ShadowResult.Success(shadowMap)
    }

    /**
     * Calculate cascade splits for CSM
     */
    private fun calculateCascadeSplits(camera: Camera, cascadeCount: Int): FloatArray {
        val splits = FloatArray(cascadeCount + 1)
        splits[0] = camera.near
        splits[cascadeCount] = camera.far

        val range = camera.far - camera.near
        val ratio = camera.far / camera.near

        for (i in 1 until cascadeCount) {
            val p = i.toFloat() / cascadeCount

            // Logarithmic split scheme
            val logSplit = camera.near * ratio.pow(p)

            // Linear split scheme
            val linearSplit = camera.near + range * p

            // Blend between logarithmic and linear (typically 0.5)
            val lambda = 0.5f
            splits[i] = linearSplit * lambda + logSplit * (1f - lambda)
        }

        return splits
    }

    /**
     * Generate a single cascade for CSM
     */
    private suspend fun generateCascade(
        light: DirectionalLight,
        scene: Scene,
        camera: Camera,
        nearPlane: Float,
        farPlane: Float
    ): ShadowCascade {
        // Calculate frustum for this cascade
        val frustum = calculateCascadeFrustum(camera, nearPlane, farPlane)

        // Fit orthographic camera to frustum
        val lightCamera = OrthographicCamera(
            left = frustum.left,
            right = frustum.right,
            top = frustum.top,
            bottom = frustum.bottom,
            near = frustum.near,
            far = frustum.far
        )

        // Get light position from its world transform
        val lightPos = light.position
        lightCamera.position.copy(lightPos)
        lightCamera.lookAt(lightPos + light.direction)
        lightCamera.updateMatrixWorld()

        val shadowMapSize = calculateCascadeShadowMapSize(nearPlane, farPlane)
        val texture = createShadowTexture(shadowMapSize, shadowMapSize)
        renderDepthToTexture(scene, lightCamera, texture)

        return ShadowCascadeImpl(
            texture = texture,
            projectionViewMatrix = lightCamera.projectionMatrix.multiply(lightCamera.matrixWorldInverse),
            splitDistance = farPlane
        )
    }

    /**
     * Calculate frustum for directional light
     */
    private fun calculateDirectionalLightFrustum(light: DirectionalLight, scene: Scene): LightFrustum {
        // Calculate scene bounds by traversing all objects
        val sceneBounds = Box3()
        scene.traverse { obj ->
            if (obj is io.kreekt.core.scene.Mesh) {
                val objBounds = obj.geometry.boundingBox ?: Box3()
                // Transform bounds to world space using object's world matrix
                val worldMin = objBounds.min.copy().applyMatrix4(obj.matrixWorld)
                val worldMax = objBounds.max.copy().applyMatrix4(obj.matrixWorld)
                sceneBounds.expandByPoint(worldMin)
                sceneBounds.expandByPoint(worldMax)
            }
        }

        // Expand bounds slightly to avoid edge clipping
        sceneBounds.expandByScalar(1.0f)

        // Create light view matrix
        val lightPos = light.position
        val target = lightPos + light.direction
        val lightViewMatrix = createLookAtMatrix(lightPos, target, Vector3.UP)

        // Transform scene bounds to light space
        val corners = arrayOf(
            Vector3(sceneBounds.min.x, sceneBounds.min.y, sceneBounds.min.z),
            Vector3(sceneBounds.max.x, sceneBounds.min.y, sceneBounds.min.z),
            Vector3(sceneBounds.min.x, sceneBounds.max.y, sceneBounds.min.z),
            Vector3(sceneBounds.max.x, sceneBounds.max.y, sceneBounds.min.z),
            Vector3(sceneBounds.min.x, sceneBounds.min.y, sceneBounds.max.z),
            Vector3(sceneBounds.max.x, sceneBounds.min.y, sceneBounds.max.z),
            Vector3(sceneBounds.min.x, sceneBounds.max.y, sceneBounds.max.z),
            Vector3(sceneBounds.max.x, sceneBounds.max.y, sceneBounds.max.z)
        )

        // Transform corners to light space
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        for (corner in corners) {
            val transformed = corner.copy().applyMatrix4(lightViewMatrix)
            minX = min(minX, transformed.x)
            maxX = max(maxX, transformed.x)
            minY = min(minY, transformed.y)
            maxY = max(maxY, transformed.y)
            minZ = min(minZ, transformed.z)
            maxZ = max(maxZ, transformed.z)
        }

        return LightFrustum(minX, maxX, minY, maxY, minZ, maxZ)
    }

    /**
     * Create a look-at matrix for light space transformation
     */
    private fun createLookAtMatrix(position: Vector3, target: Vector3, up: Vector3): Matrix4 {
        val zAxis = (position - target).normalized
        val xAxis = up.cross(zAxis).normalized
        val yAxis = zAxis.cross(xAxis)

        return Matrix4().set(
            xAxis.x, yAxis.x, zAxis.x, position.x,
            xAxis.y, yAxis.y, zAxis.y, position.y,
            xAxis.z, yAxis.z, zAxis.z, position.z,
            0f, 0f, 0f, 1f
        ).invert()
    }

    /**
     * Calculate frustum for directional light from camera view
     */
    private fun calculateDirectionalLightFrustumFromCamera(light: DirectionalLight, camera: Camera): LightFrustum {
        // Get camera frustum corners in world space
        val frustumCorners = getCameraFrustumCorners(camera)

        // Create light view matrix
        val lightPos = light.position
        val target = lightPos + light.direction
        val lightViewMatrix = createLookAtMatrix(lightPos, target, Vector3.UP)

        // Transform corners to light space and find bounds
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        for (corner in frustumCorners) {
            val transformed = corner.copy().applyMatrix4(lightViewMatrix)
            minX = min(minX, transformed.x)
            maxX = max(maxX, transformed.x)
            minY = min(minY, transformed.y)
            maxY = max(maxY, transformed.y)
            minZ = min(minZ, transformed.z)
            maxZ = max(maxZ, transformed.z)
        }

        // Add some padding to avoid shadow clipping
        val padding = 10f
        minZ -= padding
        maxZ += padding

        return LightFrustum(minX, maxX, minY, maxY, minZ, maxZ)
    }

    /**
     * Get camera frustum corners in world space
     */
    private fun getCameraFrustumCorners(camera: Camera): Array<Vector3> {
        val corners = arrayOf(
            Vector3(-1f, -1f, -1f), Vector3(1f, -1f, -1f),
            Vector3(1f, 1f, -1f), Vector3(-1f, 1f, -1f),
            Vector3(-1f, -1f, 1f), Vector3(1f, -1f, 1f),
            Vector3(1f, 1f, 1f), Vector3(-1f, 1f, 1f)
        )

        // Transform from NDC to world space
        val invProjView = camera.projectionMatrix.clone().multiply(camera.matrixWorldInverse).invert()
        for (i in corners.indices) {
            corners[i] = corners[i].applyMatrix4(invProjView)
        }

        return corners
    }

    /**
     * Calculate frustum for a cascade
     */
    private fun calculateCascadeFrustum(camera: Camera, near: Float, far: Float): LightFrustum {
        val frustumCorners = arrayOf(
            Vector3(-1f, -1f, -1f), Vector3(1f, -1f, -1f),
            Vector3(1f, 1f, -1f), Vector3(-1f, 1f, -1f),
            Vector3(-1f, -1f, 1f), Vector3(1f, -1f, 1f),
            Vector3(1f, 1f, 1f), Vector3(-1f, 1f, 1f)
        )

        // Transform to world space
        val invProjView = camera.projectionMatrix.multiply(camera.matrixWorldInverse.clone().invert())
        for (i in frustumCorners.indices) {
            frustumCorners[i] = frustumCorners[i].applyMatrix4(invProjView)
        }

        // Calculate bounds
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        for (corner in frustumCorners) {
            minX = min(minX, corner.x)
            maxX = max(maxX, corner.x)
            minY = min(minY, corner.y)
            maxY = max(maxY, corner.y)
            minZ = min(minZ, corner.z)
            maxZ = max(maxZ, corner.z)
        }

        return LightFrustum(minX, maxX, minY, maxY, minZ, maxZ)
    }

    /**
     * Calculate appropriate shadow map size based on distance and quality
     */
    private fun calculateShadowMapSize(light: Light, scene: Scene): Int {
        val baseSize = 1024
        val qualityMultiplier = when (light.shadowQuality) {
            ShadowQuality.LOW -> 0.5f
            ShadowQuality.MEDIUM -> 1.0f
            ShadowQuality.HIGH -> 2.0f
            ShadowQuality.ULTRA -> 4.0f
        }

        return ((baseSize * qualityMultiplier)).toInt().coerceIn(256, 4096)
    }

    /**
     * Calculate shadow map size for cascade
     */
    private fun calculateCascadeShadowMapSize(near: Float, far: Float): Int {
        val cascadeSize = far - near
        val sizeFactor = when {
            cascadeSize < 10f -> 2048
            cascadeSize < 50f -> 1024
            cascadeSize < 200f -> 512
            else -> 256
        }
        return sizeFactor
    }

    /**
     * Create shadow texture with appropriate format
     */
    private fun createShadowTexture(width: Int, height: Int): Texture {
        // Platform-specific implementation would create depth texture
        return object : Texture {
            override val id: Int = generateTextureId()
            val name: String = "ShadowTexture"
            override var needsUpdate: Boolean = true

            override val width: Int = width
            override val height: Int = height

            override fun dispose() {
                // Shadow texture disposal
            }
        }
    }

    private fun generateTextureId(): Int = (kotlin.random.Random.nextFloat() * 10000).toInt()

    /**
     * Create cascaded texture array
     */
    private fun createCascadedTexture(cascades: List<ShadowCascade>): Texture {
        // Platform-specific implementation would create texture array
        return object : Texture {
            override val id: Int = generateTextureId()
            val name: String = "CascadedShadowTexture"
            override var needsUpdate: Boolean = true
            override val width: Int = 1024 // Default size for cascaded texture
            override val height: Int = 1024

            override fun dispose() {
                // Cascaded shadow texture disposal
            }
        }
    }

    /**
     * Render scene depth to texture
     */
    private suspend fun renderDepthToTexture(scene: Scene, camera: Camera, texture: Texture) {
        // Platform-specific depth rendering implementation
        // This would:
        // 1. Set up depth-only render pass
        // 2. Cull objects outside shadow frustum
        // 3. Render with depth-only shader
        // 4. Apply shadow bias and filtering
    }

    /**
     * Sample shadow map with filtering
     */
    fun sampleShadowMap(
        shadowMap: ShadowMap,
        worldPosition: Vector3,
        normal: Vector3
    ): Float {
        // Transform world position to light space
        val lightSpacePos = worldPosition.applyMatrix4(shadowMap.lightSpaceMatrix)

        // Convert to shadow map coordinates
        val shadowCoords = Vector3(
            (lightSpacePos.x + 1f) * 0.5f,
            (lightSpacePos.y + 1f) * 0.5f,
            lightSpacePos.z
        )

        // Check if position is outside shadow map
        if (shadowCoords.x < 0f || shadowCoords.x > 1f ||
            shadowCoords.y < 0f || shadowCoords.y > 1f) {
            return 1.0f // Not in shadow
        }

        return when (shadowFilter) {
            ShadowFilter.NONE -> sampleShadowBasic(shadowMap, shadowCoords)
            ShadowFilter.PCF -> sampleShadowPCF(shadowMap, shadowCoords, 3)
            ShadowFilter.PCF_SOFT -> sampleShadowPCF(shadowMap, shadowCoords, 7)
            ShadowFilter.PCSS -> sampleShadowPCSS(shadowMap, shadowCoords)
            ShadowFilter.VSM -> sampleShadowVSM(shadowMap, shadowCoords)
            ShadowFilter.ESM -> sampleShadowESM(shadowMap, shadowCoords)
            ShadowFilter.CONTACT -> sampleShadowContact(shadowMap, shadowCoords, normal)
        }
    }

    /**
     * Basic shadow sampling
     */
    private fun sampleShadowBasic(shadowMap: ShadowMap, coords: Vector3): Float {
        val depth = sampleDepthTexture(shadowMap.texture, coords.x, coords.y)
        val bias = shadowBias
        return if (coords.z - bias > depth) 0.0f else 1.0f
    }

    /**
     * Linear filtered shadow sampling
     */
    private fun sampleShadowLinear(shadowMap: ShadowMap, coords: Vector3): Float {
        val texelSize = 1.0f / shadowMap.texture.width // Use width as approximation for size

        var shadow = 0f
        for (y in -1..1) {
            for (x in -1..1) {
                val sampleX = coords.x + x * texelSize
                val sampleY = coords.y + y * texelSize
                val depth = sampleDepthTexture(shadowMap.texture, sampleX, sampleY)
                shadow = shadow + if (coords.z - shadowBias > depth) 0.0f else 1.0f
            }
        }
        return shadow / 9.0f
    }

    /**
     * PCF (Percentage Closer Filtering) shadow sampling
     */
    private fun sampleShadowPCF(shadowMap: ShadowMap, coords: Vector3, kernelSize: Int): Float {
        val texelSize = 1.0f / shadowMap.texture.width // Use width as approximation for size
        val halfKernel = kernelSize / 2

        var shadow = 0f
        var samples = 0

        for (y in -halfKernel..halfKernel) {
            for (x in -halfKernel..halfKernel) {
                val sampleX = coords.x + x * texelSize * shadowRadius
                val sampleY = coords.y + y * texelSize * shadowRadius
                val depth = sampleDepthTexture(shadowMap.texture, sampleX, sampleY)
                shadow = shadow + if (coords.z - shadowBias > depth) 0.0f else 1.0f
                samples++
            }
        }
        return shadow / samples
    }

    /**
     * PCSS (Percentage Closer Soft Shadows) sampling
     */
    private fun sampleShadowPCSS(shadowMap: ShadowMap, coords: Vector3): Float {
        // PCSS implementation would require:
        // 1. Blocker search
        // 2. Penumbra estimation
        // 3. Variable filter size PCF

        // Simplified version using variable PCF
        val blockerDistance = findAverageBlockerDistance(shadowMap, coords)
        val penumbraSize = (coords.z - blockerDistance) / blockerDistance
        val filterSize = max(1, ((penumbraSize * 10)).toInt())

        return sampleShadowPCF(shadowMap, coords, filterSize)
    }

    /**
     * Poisson disk shadow sampling
     */
    private fun sampleShadowPoisson(shadowMap: ShadowMap, coords: Vector3): Float {
        val poissonDisk = arrayOf(
            Vector2(-0.94201624f, -0.39906216f),
            Vector2(0.94558609f, -0.76890725f),
            Vector2(-0.094184101f, -0.92938870f),
            Vector2(0.34495938f, 0.29387760f),
            Vector2(-0.91588581f, 0.45771432f),
            Vector2(-0.81544232f, -0.87912464f),
            Vector2(-0.38277543f, 0.27676845f),
            Vector2(0.97484398f, 0.75648379f),
            Vector2(0.44323325f, -0.97511554f),
            Vector2(0.53742981f, -0.47373420f),
            Vector2(-0.26496911f, -0.41893023f),
            Vector2(0.79197514f, 0.19090188f),
            Vector2(-0.24188840f, 0.99706507f),
            Vector2(-0.81409955f, 0.91437590f),
            Vector2(0.19984126f, 0.78641367f),
            Vector2(0.14383161f, -0.14100790f)
        )

        val texelSize = 1.0f / shadowMap.texture.width // Use width as approximation for size
        var shadow = 0f

        for (offset in poissonDisk) {
            val sampleX = coords.x + offset.x * texelSize * shadowRadius
            val sampleY = coords.y + offset.y * texelSize * shadowRadius
            val depth = sampleDepthTexture(shadowMap.texture, sampleX, sampleY)
            shadow = shadow + if (coords.z - shadowBias > depth) 0.0f else 1.0f
        }

        return shadow / poissonDisk.size
    }

    /**
     * Find average blocker distance for PCSS
     */
    private fun findAverageBlockerDistance(shadowMap: ShadowMap, coords: Vector3): Float {
        val searchRadius = 0.05f
        val texelSize = 1.0f / shadowMap.texture.width // Use width as approximation for size
        val samples = 16

        var blockerSum = 0f
        var blockerCount = 0

        for (i in 0 until samples) {
            val angle = (i * 2 * PI / samples).toFloat()
            val sampleX = coords.x + cos(angle) * searchRadius * texelSize
            val sampleY = coords.y + sin(angle) * searchRadius * texelSize
            val depth = sampleDepthTexture(shadowMap.texture, sampleX, sampleY)

            if (depth < coords.z - shadowBias) {
                blockerSum = blockerSum + depth
                blockerCount++
            }
        }

        return if (blockerCount > 0) blockerSum / blockerCount else coords.z
    }

    /**
     * VSM (Variance Shadow Maps) sampling
     */
    private fun sampleShadowVSM(shadowMap: ShadowMap, coords: Vector3): Float {
        // Sample first two moments from shadow map
        val moments = sampleMomentsTexture(shadowMap.texture, coords.x, coords.y)

        // The fragment is in shadow if its depth is greater than the mean depth
        if (coords.z <= moments.x) {
            return 1.0f
        }

        // Calculate variance
        val variance = moments.y - (moments.x * moments.x)
        val varianceMin = 0.00002f
        val adjustedVariance = max(variance, varianceMin)

        // Calculate probability using Chebyshev's inequality
        val d = coords.z - moments.x
        val pMax = adjustedVariance / (adjustedVariance + d * d)

        // Reduce light bleeding
        val lightBleedingReduction = 0.2f
        return max((pMax - lightBleedingReduction) / (1.0f - lightBleedingReduction), 0.0f)
    }

    /**
     * ESM (Exponential Shadow Maps) sampling
     */
    private fun sampleShadowESM(shadowMap: ShadowMap, coords: Vector3): Float {
        val c = 80.0f // Exponential warp factor
        val occluderDepth = sampleDepthTexture(shadowMap.texture, coords.x, coords.y)

        // Apply exponential function
        val receiver = exp(c * coords.z)
        val occluder = exp(c * occluderDepth)

        return min(1.0f, occluder / receiver)
    }

    /**
     * Contact shadows sampling
     */
    private fun sampleShadowContact(shadowMap: ShadowMap, coords: Vector3, normal: Vector3): Float {
        // Ray march from surface towards light to find occluders
        val numSteps = 16
        val stepSize = 0.1f / numSteps
        var shadow = 1.0f

        for (i in 0 until numSteps) {
            val t = i.toFloat() / numSteps
            val sampleDepth = coords.z - t * 0.1f // March towards light
            val sampleCoords = Vector3(
                coords.x + normal.x * t * stepSize,
                coords.y + normal.y * t * stepSize,
                sampleDepth
            )

            val depth = sampleDepthTexture(shadowMap.texture, sampleCoords.x, sampleCoords.y)
            if (sampleDepth - shadowBias > depth) {
                // Found contact shadow
                shadow *= (1.0f - (1.0f - t) * 0.8f)
            }
        }

        return shadow
    }

    /**
     * Sample moments texture for VSM
     */
    private fun sampleMomentsTexture(texture: Texture, x: Float, y: Float): Vector2 {
        // Sample variance shadow map which stores depth and depth²
        // In practice, this would sample an RG32F texture
        val depth = sampleDepthTexture(texture, x, y)
        return Vector2(depth, depth * depth)
    }

    /**
     * Sample depth texture (platform-specific implementation needed)
     */
    private fun sampleDepthTexture(texture: Texture, x: Float, y: Float): Float {
        // Platform-specific depth texture sampling
        // This is a simplified implementation - actual implementation would
        // interface with the GPU texture sampling

        // Clamp coordinates to [0,1]
        val u = x.coerceIn(0f, 1f)
        val v = y.coerceIn(0f, 1f)

        // Convert to texture coordinates
        val texX = (u * texture.width).toInt().coerceIn(0, texture.width - 1)
        val texY = (v * texture.height).toInt().coerceIn(0, texture.height - 1)

        // In a real implementation, this would sample the actual texture data
        // For now, return a depth value based on texture coordinates
        // This simulates a gradient depth map for testing
        return (u + v) * 0.5f + 0.3f
    }
}

/**
 * Shadow map implementation
 */
private data class ShadowMapImpl(
    override val texture: Texture,
    override val lightSpaceMatrix: Matrix4,
    override val near: Float,
    override val far: Float,
    override val bias: Float
) : ShadowMap

private data class ShadowCascadeImpl(
    override val texture: Texture,
    override val projectionViewMatrix: Matrix4,
    override val splitDistance: Float
) : ShadowCascade

/**
 * Cascaded shadow map implementation
 */
private data class CascadedShadowMapImpl(
    override val cascades: List<ShadowCascade>,
    val splitDistances: FloatArray,
    val texture: Texture,
    val lightSpaceMatrix: Matrix4,
    val near: Float,
    val far: Float,
    val bias: Float
) : CascadedShadowMap {

    fun getCascadeForDepth(depth: Float): Int {
        for (i in 0 until splitDistances.size - 1) {
            if (depth >= splitDistances[i] && depth < splitDistances[i + 1]) {
                return i
            }
        }
        return splitDistances.size - 2 // Last cascade
    }

    fun getCascadeMatrix(index: Int): Matrix4 {
        return if (index < cascades.size) cascades[index].projectionViewMatrix else Matrix4.identity()
    }
}

/**
 * Cube shadow map implementation
 */
private data class CubeShadowMapImpl(
    val textures: Array<Texture>,
    val lightPosition: Vector3,
    override val near: Float,
    override val far: Float
) : CubeShadowMap {
    override val cubeTexture: CubeTexture = CubeTextureImpl(
        size = 256,
        format = TextureFormat.RGBA8, // Using basic format for now
        filter = TextureFilter.LINEAR
    )

    override val matrices: Array<Matrix4> = Array(6) { Matrix4.identity() }
}

/**
 * Light frustum for shadow calculations
 */
private data class LightFrustum(
    val left: Float,
    val right: Float,
    val bottom: Float,
    val top: Float,
    val near: Float,
    val far: Float
)

/**
 * Shadow quality levels
 */
enum class ShadowQuality {
    LOW, MEDIUM, HIGH, ULTRA
}

/**
 * Extension properties for lights
 */
var Light.shadowQuality: ShadowQuality
    get() = ShadowQuality.MEDIUM // Default
    set(value) { /* Store in light properties */ }

/**
 * Extension function for Material to set uniforms
 * This is a simplified implementation - in a real system, materials would
 * store uniforms in a map or similar structure
 */
fun Material.setUniform(name: String, value: Any) {
    // In a real implementation, this would set the uniform value
    // on the material's shader program
    // For now, this is a placeholder that could be expanded
    // when the material system is fully implemented
}