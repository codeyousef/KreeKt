/**
 * Light Probe System
 * Provides light probe placement, baking, and runtime interpolation for global illumination
 */
package io.kreekt.lighting
import io.kreekt.core.math.Box3
import io.kreekt.renderer.Texture

import io.kreekt.core.math.*
import io.kreekt.core.platform.currentTimeMillis
import io.kreekt.core.scene.Scene
import io.kreekt.core.scene.Object3D
import io.kreekt.core.scene.Mesh
import io.kreekt.renderer.*
import io.kreekt.renderer.CubeTexture
import io.kreekt.renderer.CubeTextureImpl
import io.kreekt.camera.Camera
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.camera.Viewport
import kotlinx.coroutines.*
import kotlin.math.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import io.kreekt.lighting.SHOrder.SH_L1
import io.kreekt.lighting.ProbeCompressionFormat.TETRAHEDRAL

/**
 * Light probe implementation with spherical harmonics and cubemap capture
 */
class LightProbeImpl(
    override val position: Vector3,
    override val distance: Float = 10.0f,
    override val intensity: Float = 1.0f
) : LightProbe {

    override var irradianceMap: CubeTexture? = null
    override var sh: SphericalHarmonics? = null

    // Probe configuration
    var resolution: Int = 256
    var nearPlane: Float = 0.1f
    var farPlane: Float = 100.0f
    var updateFrequency: Float = 60.0f // Updates per second
    var autoUpdate: Boolean = false

    // Influence calculation
    var falloffType: ProbeFalloff = ProbeFalloff.SMOOTH
    var falloffStrength: Float = 1.0f
    var influenceBounds: Box3? = null

    // Lightmap baking
    var lightmapResolution: Int = 512
    var lightmapUVScale: Float = 1.0f
    var lightmapPadding: Int = 2

    // Quality settings
    var compressionFormat: ProbeCompressionFormat = ProbeCompressionFormat.NONE
    var compressionQuality: Float = 0.8f

    // Runtime state
    private var lastUpdateTime: Float = 0f
    private var captureInProgress: Boolean = false
    private var validData: Boolean = false

    override suspend fun capture(scene: Scene, renderer: Renderer, camera: Camera): ProbeResult<CubeTexture> {
        if (captureInProgress) {
            return ProbeResult.Error(ProbeException("Capture already in progress"))
        }

        return try {
            captureInProgress = true

            // Create 6 cameras for cubemap faces
            val cameras = createCubemapCameras()

            // Capture each face
            val faceData = Array(6) { FloatArray(resolution * (resolution * 4)) }

            for (face in 0 until 6) {
                val camera = cameras[face]
                // Render scene from this camera's perspective
                // This creates a placeholder capture - in production,
                // this would use the renderer's render-to-texture capability
                faceData[face] = captureFace(scene, camera, renderer)
            }

            // Create cubemap from captured data
            irradianceMap = createCubemapFromFaces(faceData)

            // Generate spherical harmonics
            irradianceMap?.let { cubemap ->
                sh = generateSphericalHarmonics(cubemap)
            }

            validData = true
            lastUpdateTime = currentTimeMillis().toFloat() / 1000f

            // Create a placeholder cubemap for now
            val cubemap = CubeTextureImpl(
                size = 256,
                format = TextureFormat.RGBA32F,
                filter = TextureFilter.LINEAR
            )
            ProbeResult.Success(cubemap)
        } catch (e: Exception) {
            ProbeResult.Error(ProbeException("Capture failed: ${e.message}"))
        } finally {
            captureInProgress = false
        }
    }

    override fun getInfluence(position: Vector3): Float {
        if (!validData) return 0f

        val distanceToProbe = this.position.distanceTo(position)
        if (distanceToProbe > distance) return 0f

        // Check influence bounds if defined
        influenceBounds?.let { bounds ->
            if (!bounds.containsPoint(position)) return 0f
        }

        // Calculate influence based on falloff type
        val normalizedDistance = distanceToProbe / distance

        return when (falloffType) {
            ProbeFalloff.LINEAR -> (1f - normalizedDistance).coerceIn(0f, 1f)
            ProbeFalloff.SMOOTH -> {
                val t = 1f - normalizedDistance
                (t * t * (3f - (2f * t))).coerceIn(0f, 1f)
            }
            ProbeFalloff.EXPONENTIAL -> {
                exp(-(normalizedDistance * falloffStrength)).coerceIn(0f, 1f)
            }
            ProbeFalloff.CUSTOM -> {
                // Custom falloff can be implemented by extending this class
                1f - normalizedDistance
            }
        }
    }

    /**
     * Capture a single face of the cubemap
     */
    private fun captureFace(scene: Scene, camera: Camera, renderer: Renderer): FloatArray {
        // Create render target for this face
        val data = FloatArray(resolution * resolution * 4)

        // In a real implementation, this would:
        // 1. Create a render target texture
        // 2. Render the scene from the camera's viewpoint
        // 3. Read back the pixel data
        // For now, generate a simple gradient based on camera direction
        val direction = camera.getWorldDirection(Vector3())

        for (y in 0 until resolution) {
            for (x in 0 until resolution) {
                val u = (x.toFloat() / resolution) * 2f - 1f
                val v = (y.toFloat() / resolution) * 2f - 1f

                // Simple color based on direction
                val idx = (y * resolution + x) * 4
                data[idx] = (direction.x + 1f) * 0.5f     // R
                data[idx + 1] = (direction.y + 1f) * 0.5f // G
                data[idx + 2] = (direction.z + 1f) * 0.5f // B
                data[idx + 3] = 1f                        // A
            }
        }

        return data
    }

    /**
     * Get lighting contribution at a surface point
     */
    fun getLightingContribution(
        surfacePosition: Vector3,
        surfaceNormal: Vector3,
        viewDirection: Vector3
    ): Color {
        val influence = getInfluence(surfacePosition)
        if (influence <= 0f) return Color.BLACK

        return when {
            sh != null -> {
                // Use spherical harmonics for fast approximation
                val shResult = sh!!.evaluate(surfaceNormal)
                val scaled = shResult * (influence * intensity)
                Color(scaled.x.coerceIn(0f, 1f), scaled.y.coerceIn(0f, 1f), scaled.z.coerceIn(0f, 1f))
            }
            irradianceMap != null -> {
                // Sample irradiance map directly
                val irradianceColor = sampleIrradianceMap(irradianceMap!!, surfaceNormal)
                Color(
                    irradianceColor.r * influence * intensity,
                    irradianceColor.g * influence * intensity,
                    irradianceColor.b * influence * intensity
                )
            }
            else -> Color.BLACK
        }
    }

    /**
     * Create cameras for cubemap capture
     */
    private fun createCubemapCameras(): Array<PerspectiveCamera> {
        val directions = arrayOf(
            Vector3(1f, 0f, 0f),   // +X
            Vector3(-1f, 0f, 0f),  // -X
            Vector3(0f, 1f, 0f),   // +Y
            Vector3(0f, -1f, 0f),  // -Y
            Vector3(0f, 0f, 1f),   // +Z
            Vector3(0f, 0f, -1f)   // -Z
        )

        val ups = arrayOf(
            Vector3(0f, -1f, 0f),  // +X
            Vector3(0f, -1f, 0f),  // -X
            Vector3(0f, 0f, 1f),   // +Y
            Vector3(0f, 0f, -1f),  // -Y
            Vector3(0f, -1f, 0f),  // +Z
            Vector3(0f, -1f, 0f)   // -Z
        )

        return Array(6) { face ->
            PerspectiveCamera(
                fov = 90f,
                aspect = 1f,
                near = nearPlane,
                far = farPlane
            ).apply {
                position.copy(this@LightProbeImpl.position)
                lookAt(this@LightProbeImpl.position.clone().add(directions[face]))
                updateMatrixWorld()
            }
        }
    }

    /**
     * Create cubemap from face data
     */
    private fun createCubemapFromFaces(faceData: Array<FloatArray>): CubeTexture {
        // Platform-specific cubemap creation
        val cubemap = CubeTextureImpl(
            size = resolution,
            format = TextureFormat.RGBA32F,
            filter = TextureFilter.LINEAR
        )

        // Set face data
        for (face in 0 until 6) {
            cubemap.setFaceDataByIndex(face, faceData[face])
        }

        return cubemap
    }

    /**
     * Generate spherical harmonics from cubemap
     */
    private suspend fun generateSphericalHarmonics(cubemap: CubeTexture): SphericalHarmonics = withContext(Dispatchers.Default) {
        val coefficients = Array(9) { Vector3.ZERO }

        // Sample the cubemap to generate SH coefficients
        val samples = 32 // Samples per face edge
        val deltaU = 2f / samples
        val deltaV = 2f / samples

        for (face in 0 until 6) {
            for (i in 0 until samples) {
                for (j in 0 until samples) {
                    val u = -1f + (i + 0.5f) * deltaU
                    val v = -1f + (j + 0.5f) * deltaV

                    val direction = cubeFaceUVToDirection(face, u, v)
                    val color = sampleCubemap(cubemap, direction)

                    // Solid angle weight
                    val weight = calculateSolidAngle(u, v, deltaU, deltaV)

                    // Evaluate SH basis and accumulate
                    val shBasis = evaluateSphericalHarmonics(direction)
                    for (k in 0 until 9) {
                        coefficients[k] = coefficients[k].add(color.toVector3().multiplyScalar(shBasis[k] * weight))
                    }
                }
            }
        }

        IBLSphericalHarmonics(coefficients)
    }

    /**
     * Sample irradiance map
     */
    private fun sampleIrradianceMap(irradianceMap: CubeTexture, normal: Vector3): Color {
        // Platform-specific cubemap sampling
        return sampleCubemap(irradianceMap, normal)
    }

    /**
     * Sample cubemap at direction
     */
    private fun sampleCubemap(cubemap: CubeTexture, direction: Vector3): Color {
        // Determine which face to sample based on the direction
        val absX = abs(direction.x)
        val absY = abs(direction.y)
        val absZ = abs(direction.z)

        val (faceIndex, u, v) = when {
            absX >= absY && absX >= absZ -> {
                // X face
                if (direction.x > 0) {
                    // +X face
                    Triple(0, -direction.z / absX, -direction.y / absX)
                } else {
                    // -X face
                    Triple(1, direction.z / absX, -direction.y / absX)
                }
            }

            absY >= absX && absY >= absZ -> {
                // Y face
                if (direction.y > 0) {
                    // +Y face
                    Triple(2, direction.x / absY, direction.z / absY)
                } else {
                    // -Y face
                    Triple(3, direction.x / absY, -direction.z / absY)
                }
            }

            else -> {
                // Z face
                if (direction.z > 0) {
                    // +Z face
                    Triple(4, direction.x / absZ, -direction.y / absZ)
                } else {
                    // -Z face
                    Triple(5, -direction.x / absZ, -direction.y / absZ)
                }
            }
        }

        // Convert from [-1, 1] to [0, 1]
        val s = (u + 1f) * 0.5f
        val t = (v + 1f) * 0.5f

        // Sample the texture at the calculated coordinates
        return (cubemap as? io.kreekt.texture.CubeTexture)?.sampleFace(faceIndex, s, t)?.let {
            Color(it.x, it.y, it.z)
        } ?: Color.WHITE
    }

    /**
     * Convert cube face UV to world direction
     */
    private fun cubeFaceUVToDirection(face: Int, u: Float, v: Float): Vector3 {
        return when (face) {
            0 -> Vector3(1f, -v, -u)     // +X
            1 -> Vector3(-1f, -v, u)     // -X
            2 -> Vector3(u, 1f, v)       // +Y
            3 -> Vector3(u, -1f, -v)     // -Y
            4 -> Vector3(u, -v, 1f)      // +Z
            5 -> Vector3(-u, -v, -1f)    // -Z
            else -> Vector3(0f, 0f, 1f)
        }.normalized
    }

    /**
     * Calculate solid angle for cubemap sampling
     */
    private fun calculateSolidAngle(u: Float, v: Float, deltaU: Float, deltaV: Float): Float {
        val x0 = u - deltaU * 0.5f
        val x1 = u + deltaU * 0.5f
        val y0 = v - deltaV * 0.5f
        val y1 = v + deltaV * 0.5f

        return areaSolidAngle(x0, y0) - areaSolidAngle(x0, y1) - areaSolidAngle(x1, y0) + areaSolidAngle(x1, y1)
    }

    private fun areaSolidAngle(x: Float, y: Float): Float {
        return atan2((x * y), sqrt(x * x + y * y + 1f))
    }

    /**
     * Evaluate spherical harmonics basis functions
     */
    private fun evaluateSphericalHarmonics(direction: Vector3): FloatArray {
        val x = direction.x
        val y = direction.y
        val z = direction.z

        return floatArrayOf(
            0.282095f,                           // Y₀⁰
            0.488603f * y,                       // Y₁⁻¹
            0.488603f * z,                       // Y₁⁰
            0.488603f * x,                       // Y₁¹
            1.092548f * (x * y),                   // Y₂⁻²
            1.092548f * (y * z),                   // Y₂⁻¹
            0.315392f * (3f * z * z - 1f),       // Y₂⁰
            1.092548f * (x * z),                   // Y₂¹
            0.546274f * (x * x - (y * y))          // Y₂²
        )
    }
}

/**
 * Light probe baking system
 */
class LightProbeBakerImpl : LightProbeBaker {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Baking configuration
    var bounceCount: Int = 3
    var sampleCount: Int = 1024
    var filterSize: Float = 1.0f
    var denoisingEnabled: Boolean = true
    var progressiveRefinement: Boolean = true

    // Performance settings
    var maxConcurrentBakes: Int = 4
    var tileSize: Int = 64
    var adaptiveQuality: Boolean = true

    private fun calculateSceneBounds(scene: Scene): Box3 {
        val bounds = Box3()

        // Iterate through all objects in the scene hierarchy
        scene.traverse { obj ->
            // Skip lights and non-renderable objects
            if (obj.type == "Mesh" || obj.type == "Group") {
                // Get object bounds in world space
                val objBounds = obj.getBoundingBox()
                if (objBounds != null) {
                    // Transform bounds to world space
                    objBounds.applyMatrix4(obj.matrixWorld)
                    bounds.union(objBounds)
                }
            }
        }

        // If no bounds found, use default
        if (bounds.isEmpty()) {
            bounds.min.set(-10f, -10f, -10f)
            bounds.max.set(10f, 10f, 10f)
        }

        return bounds
    }

    override suspend fun autoPlaceProbes(scene: Scene, density: Float): List<LightProbe> {
        val sceneBounds = calculateSceneBounds(scene)
        val size = sceneBounds.getSize()
        val volume = size.x * size.y * size.z
        val spacing = (volume / density).pow(1f / 3f)

        return placeProbesOnGrid(sceneBounds, Vector3(spacing, spacing, spacing))
    }

    override suspend fun placeProbesOnGrid(bounds: Box3, spacing: Vector3): List<LightProbe> {
        val probes = mutableListOf<LightProbe>()

        val start = bounds.min
        val end = bounds.max

        var x = start.x
        while (x <= end.x) {
            var y = start.y
            while (y <= end.y) {
                var z = start.z
                while (z <= end.z) {
                    val position = Vector3(x, y, z)
                    val distance = min(spacing.x, min(spacing.y, spacing.z)) * 1.5f

                    probes.add(LightProbeImpl(position, distance))

                    z = z + spacing.z
                }
                y = y + spacing.y
            }
            x = x + spacing.x
        }

        return probes
    }

    override suspend fun placeProbesManual(positions: List<Vector3>): List<LightProbe> {
        return positions.map { position ->
            LightProbeImpl(position, 10.0f) // Default distance
        }
    }

    override suspend fun bakeProbe(probe: LightProbe, scene: Scene): BakeResult<Unit> = withContext(Dispatchers.Default) {
        try {
            // Create render context for baking
            val renderer = createBakeRenderer()
            val camera = PerspectiveCamera(fov = 90f, aspect = 1f, near = 0.1f, far = 100f)
            camera.position.copy(probe.position)

            // Capture environment from probe position
            val captureResult = probe.capture(scene, renderer, camera)
            when (captureResult) {
                is ProbeResult.Success -> BakeResult.Success(Unit)
                is ProbeResult.Error -> BakeResult.Error(
                    BakingFailed("Probe capture failed: ${captureResult.exception.message}")
                )
            }
        } catch (e: Exception) {
            BakeResult.Error(BakingFailed("Probe baking failed: ${e.message}"))
        }
    }

    override suspend fun bakeAllProbes(probes: List<LightProbe>, scene: Scene): BakeResult<Unit> = withContext(Dispatchers.Default) {
        try {
            val semaphore = Semaphore(maxConcurrentBakes)
            val jobs = probes.map { probe ->
                async<BakeResult<Unit>> {
                    semaphore.withPermit {
                        bakeProbe(probe, scene)
                    }
                }
            }

            val results = jobs.awaitAll()
            val errors = results.filterIsInstance<BakeResult.Error>()

            if (errors.isNotEmpty()) {
                BakeResult.Error(BakingFailed("${errors.size} probes failed to bake"))
            } else {
                BakeResult.Success(Unit)
            }
        } catch (e: Exception) {
            BakeResult.Error(BakingFailed("Batch baking failed: ${e.message}"))
        }
    }

    suspend fun bakeLightmaps(scene: Scene, resolution: Int): BakeResult<List<Texture2D>> = withContext(Dispatchers.Default) {
        try {
            val lightmaps = mutableListOf<Texture2D>()

            // Process each object that needs lightmaps
            val objects = scene.getObjectsWithLightmapUVs()

            for (obj in objects) {
                val lightmap = bakeLightmapForObject(obj, scene, resolution)
                lightmaps.add(lightmap)
            }

            BakeResult.Success(lightmaps)
        } catch (e: Exception) {
            BakeResult.Error(BakingFailed("Lightmap baking failed: ${e.message}"))
        }
    }

    fun optimizeProbeNetwork(probes: List<LightProbe>): List<LightProbe> {
        val optimizedProbes = mutableListOf<LightProbe>()
        val processed = mutableSetOf<LightProbe>()

        for (probe in probes) {
            if (probe in processed) continue

            // Find nearby probes
            val nearby = probes.filter { other ->
                other != probe && probe.position.distanceTo(other.position) < probe.distance * 0.5f
            }

            if (nearby.isNotEmpty()) {
                // Merge nearby probes
                val mergedPosition = (probe.position + nearby.fold(Vector3.ZERO) { acc, p -> acc + p.position }) / (nearby.size + 1f)
                val mergedDistance = (probe.distance + nearby.sumOf { it.distance.toDouble() }) / (nearby.size + 1f)

                optimizedProbes.add(LightProbeImpl(mergedPosition, mergedDistance.toFloat()))
                processed.add(probe)
                processed.addAll(nearby)
            } else {
                optimizedProbes.add(probe)
                processed.add(probe)
            }
        }

        return optimizedProbes
    }

    fun generateProbeVolume(probes: List<LightProbe>): ProbeVolume {
        // Calculate bounds of probe network
        val bounds = calculateProbeBounds(probes)

        // Determine grid resolution based on probe density
        val averageSpacing = calculateAverageSpacing(probes)
        val gridSize = Vector3(
            ceil((bounds.max.x - bounds.min.x) / averageSpacing.x),
            ceil((bounds.max.y - bounds.min.y) / averageSpacing.y),
            ceil((bounds.max.z - bounds.min.z) / averageSpacing.z)
        )

        // Create 3D grid of probes
        val grid = Array(gridSize.x.toInt()) { x ->
            Array(gridSize.y.toInt()) { y ->
                Array(gridSize.z.toInt()) { z ->
                    findNearestProbe(probes, bounds.min + Vector3(
                        x * averageSpacing.x,
                        y * averageSpacing.y,
                        z * averageSpacing.z
                    ))
                }
            }
        }

        // Convert grid to flat list for ProbeVolume
        val flatProbes = mutableListOf<LightProbe>()
        for (x in grid.indices) {
            for (y in grid[x].indices) {
                for (z in grid[x][y].indices) {
                    flatProbes.add(grid[x][y][z])
                }
            }
        }

        return ProbeVolume(bounds, flatProbes, averageSpacing)
    }

    fun compressProbeData(probes: List<LightProbe>): CompressedProbeData {
        return when (val compressionFormat = ProbeCompressionFormat.NONE) {
            ProbeCompressionFormat.NONE -> {
                // No compression - store full irradiance maps
                val data = serializeIrradianceMaps(probes)
                CompressedProbeData(
                    data = data,
                    metadata = ProbeMetadata(
                        format = compressionFormat,
                        originalSize = data.size,
                        compressedSize = data.size
                    )
                )
            }
            ProbeCompressionFormat.SH_L1 -> {
                // L1 spherical harmonics (4 coefficients)
                val shData = compressToSphericalHarmonics(probes, 4)
                CompressedProbeData(
                    data = shData,
                    metadata = ProbeMetadata(
                        format = compressionFormat,
                        originalSize = probes.size * 256,  // Estimated original size
                        compressedSize = shData.size
                    )
                )
            }
            ProbeCompressionFormat.NONE -> {
                // L2 spherical harmonics (9 coefficients)
                val shData = compressToSphericalHarmonics(probes, 9)
                CompressedProbeData(
                    data = shData,
                    metadata = ProbeMetadata(
                        format = compressionFormat,
                        originalSize = probes.size * 256,  // Estimated original size
                        compressedSize = shData.size
                    )
                )
            }
            ProbeCompressionFormat.TETRAHEDRAL -> {
                // Tetrahedral encoding
                val tetraData = compressToTetrahedral(probes)
                CompressedProbeData(
                    data = tetraData,
                    metadata = ProbeMetadata(
                        format = compressionFormat,
                        originalSize = probes.size * 256,  // Estimated original size
                        compressedSize = tetraData.size
                    )
                )
            }
            ProbeCompressionFormat.RGBM -> {
                // RGBM encoding
                val data = serializeIrradianceMaps(probes)
                CompressedProbeData(
                    data = data,
                    metadata = ProbeMetadata(
                        format = compressionFormat,
                        originalSize = data.size,
                        compressedSize = data.size
                    )
                )
            }
            ProbeCompressionFormat.RGBE -> {
                // RGBE encoding
                val data = serializeIrradianceMaps(probes)
                CompressedProbeData(
                    data = data,
                    metadata = ProbeMetadata(
                        format = compressionFormat,
                        originalSize = data.size,
                        compressedSize = data.size
                    )
                )
            }
            ProbeCompressionFormat.LOGLUV -> {
                // LogLuv encoding
                val data = serializeIrradianceMaps(probes)
                CompressedProbeData(
                    data = data,
                    metadata = ProbeMetadata(
                        format = compressionFormat,
                        originalSize = data.size,
                        compressedSize = data.size
                    )
                )
            }
        }
    }

    /**
     * Bake lightmap for a specific object
     */
    private suspend fun bakeLightmapForObject(obj: Any, scene: Scene, resolution: Int): Texture2D = withContext(Dispatchers.Default) {
        // Create lightmap texture
        val lightmap = createEmptyTexture2D(resolution, resolution)
        val data = FloatArray(resolution * (resolution * 4)) // TextureFormat.RGBA8

        // Render lighting for each texel
        for (y in 0 until resolution) {
            for (x in 0 until resolution) {
                val u = (x + 0.5f) / resolution
                val v = (y + 0.5f) / resolution

                // Get world position and normal from UV coordinates
                val worldPos = getWorldPositionFromUV(obj, u, v)
                val normal = getNormalFromUV(obj, u, v)

                // Calculate lighting at this point
                val lighting = calculateLightingAtPoint(worldPos, normal, scene)

                val index = (y * resolution + x) * 4
                data[index] = lighting.r
                data[index + 1] = lighting.g
                data[index + 2] = lighting.b
                data[index + 3] = 1.0f
            }
        }

        lightmap.setData(data)
        lightmap
    }

    /**
     * Calculate lighting at a world position
     */
    private fun calculateLightingAtPoint(position: Vector3, normal: Vector3, scene: Scene): Color {
        var totalLighting = Color.BLACK

        // Direct lighting from lights in the scene
        val lights = scene.children.filterIsInstance<Light>()
        for (light in lights) {
            when (light) {
                is DirectionalLight -> {
                    val lightDir = light.direction.normalized * -1f
                    val nDotL = max(0f, normal.dot(lightDir))
                    totalLighting = totalLighting + light.color * (light.intensity * nDotL)
                }
                is PointLight -> {
                    // Get light position from its world transform
                    val lightPos = light.position
                    val lightDir = (lightPos - position).normalized
                    val distance = lightPos.distanceTo(position)
                    val attenuation = 1f / (1f + 0.1f * distance + 0.01f * (distance * distance))
                    val nDotL = max(0f, normal.dot(lightDir))
                    totalLighting = totalLighting + light.color * light.intensity * (nDotL * attenuation)
                }
            }
        }

        // Indirect lighting from light probes
        val probes = scene.lightProbes
        for (probe in probes) {
            val contribution = probe.getLightingContribution(position, normal, Vector3.UP)
            totalLighting = totalLighting + contribution
        }

        return totalLighting
    }

    /**
     * Helper functions
     */
    private fun calculateProbeBounds(probes: List<LightProbe>): Box3 {
        if (probes.isEmpty()) return Box3()

        var min = probes.first().position.copy()
        var max = probes.first().position.copy()

        for (probe in probes) {
            min = Vector3(
                kotlin.math.min(min.x, probe.position.x),
                kotlin.math.min(min.y, probe.position.y),
                kotlin.math.min(min.z, probe.position.z)
            )
            max = Vector3(
                kotlin.math.max(max.x, probe.position.x),
                kotlin.math.max(max.y, probe.position.y),
                kotlin.math.max(max.z, probe.position.z)
            )
        }

        return Box3(min, max)
    }

    private fun calculateAverageSpacing(probes: List<LightProbe>): Vector3 {
        if (probes.size < 2) return Vector3.ONE

        var totalSpacing = Vector3.ZERO
        var count = 0

        for (i in probes.indices) {
            for (j in i + 1 until probes.size) {
                val spacing = probes[j].position - probes[i].position
                totalSpacing = totalSpacing + Vector3(abs(spacing.x), abs(spacing.y), abs(spacing.z))
                count++
            }
        }

        return totalSpacing / count.toFloat()
    }

    private fun findNearestProbe(probes: List<LightProbe>, position: Vector3): LightProbe {
        return probes.minByOrNull { it.position.distanceTo(position) } ?: probes.first()
    }

    // Platform-specific implementations with working defaults
    private fun createBakeRenderer(): Renderer {
        // Create a simple renderer for baking
        // In a real implementation, this would create a platform-specific renderer
        return object : Renderer {
            override val capabilities: RendererCapabilities = RendererCapabilities(
                maxTextureSize = 1024,
                maxCubeMapSize = 1024,
                maxVertexAttributes = 16,
                maxVertexUniforms = 1024,
                maxFragmentUniforms = 1024,
                maxVertexTextures = 8,
                maxFragmentTextures = 8,
                maxCombinedTextures = 16,
                maxSamples = 4,
                maxAnisotropy = 1f,
                floatTextures = true,
                depthTextures = true,
                instancedRendering = true,
                multipleRenderTargets = true,
                vertexArrayObjects = true,
                standardDerivatives = true,
                shadowMaps = true,
                vendor = "KreeKt",
                renderer = "Mock Renderer",
                version = "1.0",
                shadingLanguageVersion = "WGSL 1.0"
            )
            override var renderTarget: RenderTarget? = null
            override var autoClear: Boolean = true
            override var autoClearColor: Boolean = true
            override var autoClearDepth: Boolean = true
            override var autoClearStencil: Boolean = true
            override var clearColor: Color = Color.BLACK
            override var clearAlpha: Float = 1f
            override var shadowMap: ShadowMapSettings = ShadowMapSettings()
            override var toneMapping: ToneMapping = ToneMapping.NONE
            override var toneMappingExposure: Float = 1f
            override var outputColorSpace: ColorSpace = ColorSpace.sRGB
            override var physicallyCorrectLights: Boolean = false

            override suspend fun initialize(surface: RenderSurface): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun render(scene: Scene, camera: Camera): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun setSize(width: Int, height: Int, updateStyle: Boolean): RendererResult<Unit> =
                RendererResult.Success(Unit)

            override fun setPixelRatio(pixelRatio: Float): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun setViewport(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit> =
                RendererResult.Success(Unit)

            override fun getViewport(): Viewport = Viewport(0, 0, 1024, 1024)
            override fun setScissorTest(enable: Boolean): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun setScissor(x: Int, y: Int, width: Int, height: Int): RendererResult<Unit> =
                RendererResult.Success(Unit)

            override fun clear(color: Boolean, depth: Boolean, stencil: Boolean): RendererResult<Unit> =
                RendererResult.Success(Unit)

            override fun clearColorBuffer(): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun clearDepth(): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun clearStencil(): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun resetState(): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun compile(scene: Scene, camera: Camera): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun dispose(): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun forceContextLoss(): RendererResult<Unit> = RendererResult.Success(Unit)
            override fun isContextLost(): Boolean = false
            override fun getStats(): RenderStats = RenderStats()
            override fun resetStats() {}
        }
    }

    private fun createEmptyTexture2D(width: Int, height: Int): Texture2D {
        return Texture2D(
            width = width,
            height = height,
            textureName = "Lightmap_${width}x${height}"
        )
    }

    private fun getWorldPositionFromUV(obj: Any, u: Float, v: Float): Vector3 {
        // In a real implementation, this would:
        // 1. Access the object's geometry
        // 2. Use the lightmap UV coordinates to find the corresponding triangle
        // 3. Interpolate the world position based on barycentric coordinates
        // For now, return a position based on UV
        return Vector3(u * 10f - 5f, 0f, v * 10f - 5f)
    }

    private fun getNormalFromUV(obj: Any, u: Float, v: Float): Vector3 {
        // In a real implementation, this would:
        // 1. Access the object's geometry
        // 2. Use the lightmap UV coordinates to find the corresponding triangle
        // 3. Interpolate the normal based on barycentric coordinates
        // For now, return up vector with slight variation
        val angle = u * PI.toFloat() * 2f
        return Vector3(sin(angle) * 0.1f, 1f, cos(angle) * 0.1f).normalized
    }

    private fun serializeIrradianceMaps(probes: List<LightProbe>): ByteArray {
        // Serialize irradiance maps to byte array
        val buffer = mutableListOf<Byte>()

        // Header: number of probes
        buffer.addAll(probes.size.toByteArray())

        for (probe in probes) {
            // Serialize probe position
            buffer.addAll(probe.position.x.toByteArray())
            buffer.addAll(probe.position.y.toByteArray())
            buffer.addAll(probe.position.z.toByteArray())

            // Serialize probe distance and intensity
            buffer.addAll(probe.distance.toByteArray())
            buffer.addAll(probe.intensity.toByteArray())

            // Serialize spherical harmonics if available
            probe.sh?.let { sh ->
                buffer.add(1) // Has SH flag
                for (coeff in sh.coefficients) {
                    buffer.addAll(coeff.x.toByteArray())
                    buffer.addAll(coeff.y.toByteArray())
                    buffer.addAll(coeff.z.toByteArray())
                }
            } ?: buffer.add(0) // No SH flag
        }

        return buffer.toByteArray()
    }

    private fun compressToSphericalHarmonics(probes: List<LightProbe>, coeffCount: Int): ByteArray {
        val buffer = mutableListOf<Byte>()

        // Header
        buffer.addAll(probes.size.toByteArray())
        buffer.addAll(coeffCount.toByteArray())

        for (probe in probes) {
            // Position
            buffer.addAll(probe.position.x.toByteArray())
            buffer.addAll(probe.position.y.toByteArray())
            buffer.addAll(probe.position.z.toByteArray())

            // SH coefficients (up to coeffCount)
            val coeffs = probe.sh?.coefficients ?: Array(9) { Vector3.ZERO }
            for (i in 0 until coeffCount.coerceAtMost(coeffs.size)) {
                buffer.addAll(coeffs[i].x.toByteArray())
                buffer.addAll(coeffs[i].y.toByteArray())
                buffer.addAll(coeffs[i].z.toByteArray())
            }
        }

        return buffer.toByteArray()
    }

    private fun compressToTetrahedral(probes: List<LightProbe>): ByteArray {
        // Tetrahedral compression encodes irradiance using 4 vertices
        val buffer = mutableListOf<Byte>()

        buffer.addAll(probes.size.toByteArray())

        for (probe in probes) {
            // Position
            buffer.addAll(probe.position.x.toByteArray())
            buffer.addAll(probe.position.y.toByteArray())
            buffer.addAll(probe.position.z.toByteArray())

            // Encode irradiance as tetrahedral (4 vertices)
            // This is a simplified version - real implementation would
            // properly encode directional irradiance
            val tetrahedralVertices = Array(4) { Vector3.ONE }
            for (vertex in tetrahedralVertices) {
                buffer.addAll(vertex.x.toByteArray())
                buffer.addAll(vertex.y.toByteArray())
                buffer.addAll(vertex.z.toByteArray())
            }
        }

        return buffer.toByteArray()
    }

    // Extension functions for byte array conversion
    private fun Int.toByteArray(): List<Byte> {
        return listOf(
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte()
        )
    }

    private fun Float.toByteArray(): List<Byte> {
        val bits = this.toBits()
        return listOf(
            (bits shr 24).toByte(),
            (bits shr 16).toByte(),
            (bits shr 8).toByte(),
            bits.toByte()
        )
    }
}

/**
 * Probe volume implementation
 */
private class ProbeVolumeImpl(
    val bounds: Box3,
    val gridProbes: Array<Array<Array<LightProbe>>>,
    val spacing: Vector3
) {

    fun sampleAt(position: Vector3): ProbeInfluence {
        // Convert world position to grid coordinates
        val localPos = position - bounds.min
        val gridPos = localPos / spacing

        val x = gridPos.x.toInt().coerceIn(0, gridProbes.size - 2)
        val y = gridPos.y.toInt().coerceIn(0, gridProbes[0].size - 2)
        val z = gridPos.z.toInt().coerceIn(0, gridProbes[0][0].size - 2)

        // Trilinear interpolation weights
        val fx = gridPos.x - x
        val fy = gridPos.y - y
        val fz = gridPos.z - z

        // Get 8 corner probes
        val cornerProbes = listOf(
            gridProbes[x][y][z],
            gridProbes[x + 1][y][z],
            gridProbes[x][y + 1][z],
            gridProbes[x + 1][y + 1][z],
            gridProbes[x][y][z + 1],
            gridProbes[x + 1][y][z + 1],
            gridProbes[x][y + 1][z + 1],
            gridProbes[x + 1][y + 1][z + 1]
        )

        // Calculate trilinear weights
        val weights = floatArrayOf(
            (1f - fx) * (1f - fy) * (1f - fz),
            fx * (1f - fy) * (1f - fz),
            (1f - fx) * fy * (1f - fz),
            fx * fy * (1f - fz),
            (1f - fx) * (1f - fy) * fz,
            fx * (1f - fy) * fz,
            (1f - fx) * (fy * fz),
            fx * (fy * fz)
        )

        // Find the probe with the highest weight for trilinear interpolation
        var maxWeight = 0f
        var bestProbe = cornerProbes[0]
        var totalWeight = 0f

        for (i in cornerProbes.indices) {
            totalWeight += weights[i]
            if (weights[i] > maxWeight) {
                maxWeight = weights[i]
                bestProbe = cornerProbes[i]
            }
        }

        return ProbeInfluence(bestProbe, totalWeight / cornerProbes.size)
    }
}


// ProbeFalloff enum is defined in LightingTypes.kt

/**
 * Extension functions
 */
private fun Color.toVector3(): Vector3 = Vector3(r, g, b)

private val Scene.lightProbes: List<LightProbe>
    get() {
        // Access light probes from scene's children
        val probes = mutableListOf<LightProbe>()
        traverse { obj ->
            if (obj is LightProbe) {
                probes.add(obj)
            }
        }
        return probes
    }

private fun Scene.getObjectsWithLightmapUVs(): List<Any> {
    // Find all objects in scene that have lightmap UV coordinates
    val objects = mutableListOf<Any>()
    traverse { obj ->
        if (obj.type == "Mesh") {
            // Check if geometry has lightmap UVs (usually UV channel 2)
            val geometry = (obj as? Mesh)?.geometry
            if (geometry?.getAttribute("uv2") != null) {
                objects.add(obj)
            }
        }
    }
    return objects
}

private fun LightProbe.getLightingContribution(position: Vector3, normal: Vector3, up: Vector3): Color {
    // Calculate lighting contribution from this probe
    val influence = getInfluence(position)
    if (influence <= 0f) return Color.BLACK

    // Use spherical harmonics if available
    sh?.let { harmonics ->
        val shResult = harmonics.evaluate(normal)
        return Color(
            (shResult.x * influence * intensity).coerceIn(0f, 1f),
            (shResult.y * influence * intensity).coerceIn(0f, 1f),
            (shResult.z * influence * intensity).coerceIn(0f, 1f)
        )
    }

    // Fallback to simple ambient contribution
    return Color(
        (0.3f * influence * intensity).coerceIn(0f, 1f),
        (0.3f * influence * intensity).coerceIn(0f, 1f),
        (0.3f * influence * intensity).coerceIn(0f, 1f)
    )
}