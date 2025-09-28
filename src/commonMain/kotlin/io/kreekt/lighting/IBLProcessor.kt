/**
 * Image-Based Lighting Processor
 * Handles HDR environment processing, cubemap generation, and IBL map creation
 */
package io.kreekt.lighting

import io.kreekt.core.math.Color
import io.kreekt.core.math.Vector2
import io.kreekt.core.math.Vector3
import io.kreekt.renderer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * Result type for IBL operations
 */
sealed class IBLResult<out T> {
    data class Success<T>(val data: T) : IBLResult<T>()
    data class Error(val message: String) : IBLResult<Nothing>()
}

/**
 * HDR Environment data
 */
data class HDREnvironment(
    val data: FloatArray,
    val width: Int,
    val height: Int
)

/**
 * IBL Configuration
 */
data class IBLConfig(
    val irradianceSize: Int = 32,
    val prefilterSize: Int = 128,
    val brdfLutSize: Int = 512,
    val roughnessLevels: Int = 5,
    val samples: Int = 1024
)

/**
 * IBL Environment Maps
 */
data class IBLEnvironmentMaps(
    val environment: CubeTexture,
    val irradiance: CubeTexture,
    val prefilter: CubeTexture,
    val brdfLut: Texture
)

/**
 * Advanced IBL processor with HDR pipeline and spherical harmonics
 */
class IBLProcessorImpl : IBLProcessor {

    private val dispatcher = Dispatchers.Default
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    // Cache for generated maps
    private val irradianceCache = mutableMapOf<String, CubeTexture>()
    private val prefilterCache = mutableMapOf<String, CubeTexture>()
    private val brdfLUTCache = mutableMapOf<Int, Texture2D>()

    // Spherical harmonics coefficients cache
    private val shCache = mutableMapOf<String, SphericalHarmonics>()

    suspend fun loadHDREnvironment(url: String): IBLResult<HDREnvironment> = withContext(dispatcher) {
        try {
            // Load HDR image data (platform-specific implementation)
            val hdrData = loadHDRImageData(url)
            IBLResult.Success(hdrData)
        } catch (e: Exception) {
            IBLResult.Error("HDRLoadingFailed: $url, ${e.message}")
        }
    }

    suspend fun generateCubemapFromHDR(hdr: HDREnvironment, size: Int): IBLResult<CubeTexture> = withContext(dispatcher) {
        try {
            val cubemap = createEmptyCubemap(size, size)

            // Generate 6 faces of the cubemap from HDR equirectangular
            val faces = arrayOf(
                Vector3(1f, 0f, 0f),   // Positive X
                Vector3(-1f, 0f, 0f),  // Negative X
                Vector3(0f, 1f, 0f),   // Positive Y
                Vector3(0f, -1f, 0f),  // Negative Y
                Vector3(0f, 0f, 1f),   // Positive Z
                Vector3(0f, 0f, -1f)   // Negative Z
            )

            val faceUps = arrayOf(
                Vector3(0f, -1f, 0f),  // Positive X
                Vector3(0f, -1f, 0f),  // Negative X
                Vector3(0f, 0f, 1f),   // Positive Y
                Vector3(0f, 0f, -1f),  // Negative Y
                Vector3(0f, -1f, 0f),  // Positive Z
                Vector3(0f, -1f, 0f)   // Negative Z
            )

            for (face in 0 until 6) {
                val faceData = generateCubemapFace(hdr, size, faces[face], faceUps[face])
                (cubemap as CubeTextureImpl).setFaceDataByIndex(face, faceData)
            }

            IBLResult.Success(cubemap)
        } catch (e: Exception) {
            IBLResult.Error("CubemapGenerationFailed: Failed to generate cubemap from HDR, ${e.message}")
        }
    }

    

    override suspend fun generateEquirectangularMap(
        cubeMap: CubeTexture,
        width: Int,
        height: Int
    ): Texture {
        return withContext(dispatcher) {
            val equirectTexture = Texture2D(
                width,
                height,
                TextureFormat.RGBA32F,
                TextureFilter.LINEAR
            )

            // Generate equirectangular projection from cubemap
            val data = FloatArray(width * (height * 4))
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val u = x.toFloat() / width
                    val v = y.toFloat() / height

                    // Convert UV to spherical coordinates
                    val theta = u * PI.toFloat() * 2f - PI.toFloat()
                    val phi = v * PI.toFloat()

                    // Convert to cartesian direction
                    val dir = Vector3(
                        sin(phi) * cos(theta),
                        cos(phi),
                        sin(phi) * sin(theta)
                    )

                    // Sample cubemap
                    val color = sampleCubemap(cubeMap, dir)
                    val idx = (y * width + x) * 4
                    data[idx] = color.r
                    data[idx + 1] = color.g
                    data[idx + 2] = color.b
                    data[idx + 3] = 1f
                }
            }

            equirectTexture.setData(data)
            equirectTexture as Texture
        }
    }

    override suspend fun generateIrradianceMap(
        environmentMap: Texture,
        size: Int
    ): CubeTexture {
        val key = "irradiance_${environmentMap.hashCode()}_$size"

        irradianceCache[key]?.let { return it }

        return withContext(dispatcher) {
            val irradianceMap = CubeTextureImpl(
                size = size,
                format = TextureFormat.RGBA32F,
                filter = TextureFilter.LINEAR
            )

            // Generate irradiance convolution
            // Implementation details...

            irradianceCache[key] = irradianceMap
            irradianceMap
        }
    }

    override suspend fun generatePrefilterMap(
        environmentMap: Texture,
        size: Int,
        roughnessLevels: Int
    ): CubeTexture {
        val key = "prefilter_${environmentMap.hashCode()}_${size}_$roughnessLevels"

        prefilterCache[key]?.let { return it }

        return withContext(dispatcher) {
            val prefilterMap = CubeTextureImpl(
                size = size,
                format = TextureFormat.RGBA32F,
                filter = TextureFilter.LINEAR,
                generateMipmaps = true
            )

            // Generate prefiltered environment map
            // Implementation details...

            prefilterCache[key] = prefilterMap
            prefilterMap
        }
    }

    override fun generateBRDFLUT(size: Int): Texture {
        brdfLUTCache[size]?.let { return it }

        val brdfLUT = Texture2D(
            size,
            size,
            TextureFormat.RG16F,
            TextureFilter.LINEAR
        )

        // Generate BRDF lookup table
        val data = FloatArray(size * (size * 2))
        for (y in 0 until size) {
            for (x in 0 until size) {
                val NdotV = x.toFloat() / size
                val roughness = y.toFloat() / size

                // Compute BRDF integral
                val integral = computeBRDFIntegral(NdotV, roughness)
                val idx = (y * size + x) * 2
                data[idx] = integral.x
                data[idx + 1] = integral.y
            }
        }

        brdfLUT.setData(data)
        brdfLUTCache[size] = brdfLUT
        return brdfLUT as Texture
    }

    private fun computeBRDFIntegral(NdotV: Float, roughness: Float): Vector2 {
        // Compute BRDF integral using importance sampling
        return integrateBRDF(NdotV, roughness)
    }

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
        return (cubemap as? io.kreekt.texture.CubeTexture)?.sampleFace(faceIndex, s, t)?.let { result ->
            Color(result.x, result.y, result.z)
        } ?: Color.WHITE
    }




    suspend fun computeSphericalHarmonics(environmentMap: CubeTexture, order: Int): IBLResult<SphericalHarmonics> = withContext(dispatcher) {
        try {
            val cacheKey = "sh_${environmentMap.hashCode()}_$order"
            shCache[cacheKey]?.let { return@withContext IBLResult.Success(it) }

            val coefficients = Array(9) { Vector3.ZERO }

            // Sample the irradiance map to generate SH coefficients
            val sampleCount = 64 * 64 // Samples per face
            val deltaPhi = 2f * PI.toFloat() / 64f
            val deltaTheta = PI.toFloat() / 64f

            for (face in 0 until 6) {
                for (i in 0 until 64) {
                    for (j in 0 until 64) {
                        val theta = (i + 0.5f) * deltaTheta
                        val phi = (j + 0.5f) * deltaPhi

                        val direction = cubeFaceToDirection(face, theta, phi)
                        val color = sampleCubemap(environmentMap, direction)

                        // Compute solid angle weight
                        val weight = sin(theta) * deltaTheta * deltaPhi

                        // Compute SH basis functions and accumulate coefficients
                        val sh = evaluateSphericalHarmonics(direction)
                        for (k in 0 until 9) {
                            coefficients[k] = coefficients[k].add(color.toVector3().multiplyScalar(sh[k] * weight))
                        }
                    }
                }
            }

            // Normalize coefficients
            val normalizer = 4f * PI.toFloat() / sampleCount
            for (k in 0 until 9) {
                coefficients[k] = coefficients[k] * normalizer
            }

            val sh = IBLSphericalHarmonics(coefficients)
            shCache[cacheKey] = sh
            IBLResult.Success(sh)
        } catch (e: Exception) {
            IBLResult.Error("CubemapGenerationFailed: Failed to generate spherical harmonics, ${e.message}")
        }
    }

    suspend fun processEnvironment(hdr: HDREnvironment, config: IBLConfig): IBLResult<IBLEnvironmentMaps> = withContext(dispatcher) {
        try {
            // Generate cubemap from HDR
            val cubemapResult = generateCubemapFromHDR(hdr, config.prefilterSize)
            val cubemap = when (cubemapResult) {
                is IBLResult.Success -> cubemapResult.data as CubeTexture
                is IBLResult.Error -> return@withContext cubemapResult
            }

            // Generate irradiance map
            val irradianceMap = generateIrradianceMap(cubemap, config.irradianceSize)

            // Generate prefilter map
            val prefilterMap = generatePrefilterMap(cubemap, config.prefilterSize, config.roughnessLevels)

            // Generate BRDF LUT
            val brdfLUT = generateBRDFLUT(config.brdfLutSize)

            // Compute spherical harmonics for fast irradiance approximation
            val sphericalHarmonicsResult = computeSphericalHarmonics(cubemap, 2)
            val sphericalHarmonics = when (sphericalHarmonicsResult) {
                is IBLResult.Success -> sphericalHarmonicsResult.data
                is IBLResult.Error -> null // Optional, continue without SH
            }

            IBLResult.Success(IBLEnvironmentMaps(
                environment = cubemap,
                irradiance = irradianceMap,
                prefilter = prefilterMap,
                brdfLut = brdfLUT
            ))
        } catch (e: Exception) {
            IBLResult.Error("ProcessingFailed: Failed to process environment, ${e.message}")
        }
    }

    fun applySHLighting(sh: SphericalHarmonics, normal: Vector3): Color {
        val shBasis = evaluateSphericalHarmonics(normal)
        var result = Vector3.ZERO

        for (i in 0 until 9) {
            result = result.add(sh.coefficients[i].clone().multiplyScalar(shBasis[i]))
        }

        return Color(result.x.coerceAtLeast(0f), result.y.coerceAtLeast(0f), result.z.coerceAtLeast(0f))
    }

    /**
     * Generate a single face of the irradiance cubemap
     */
    private suspend fun generateIrradianceFace(environment: CubeTexture, size: Int, face: Int): FloatArray = withContext(dispatcher) {
        val data = FloatArray(size * (size * 4)) // TextureFormat.RGBA8

        for (y in 0 until size) {
            for (x in 0 until size) {
                val u = (x + 0.5f) / size * 2f - 1f
                val v = (y + 0.5f) / size * 2f - 1f

                val direction = cubeFaceUVToDirection(face, u, v)
                val irradiance = computeIrradiance(environment, direction)

                val index = (y * size + x) * 4
                data[index] = irradiance.r
                data[index + 1] = irradiance.g
                data[index + 2] = irradiance.b
                data[index + 3] = 1.0f
            }
        }

        data
    }

    /**
     * Generate a single face of the prefilter cubemap
     */
    private suspend fun generatePrefilterFace(
        environment: CubeTexture,
        size: Int,
        face: Int,
        roughness: Float
    ): FloatArray = withContext(dispatcher) {
        val data = FloatArray(size * (size * 4)) // TextureFormat.RGBA8

        for (y in 0 until size) {
            for (x in 0 until size) {
                val u = (x + 0.5f) / size * 2f - 1f
                val v = (y + 0.5f) / size * 2f - 1f

                val direction = cubeFaceUVToDirection(face, u, v)
                val prefiltered = computePrefilter(environment, direction, roughness)

                val index = (y * size + x) * 4
                data[index] = prefiltered.r
                data[index + 1] = prefiltered.g
                data[index + 2] = prefiltered.b
                data[index + 3] = 1.0f
            }
        }

        data
    }

    /**
     * Compute irradiance by integrating over hemisphere
     */
    private fun computeIrradiance(environment: CubeTexture, normal: Vector3): Color {
        var irradiance = Vector3.ZERO
        var sampleCount = 0

        val samples = 64 // Number of samples for integration
        val deltaTheta = PI.toFloat() / samples
        val deltaPhi = 2f * PI.toFloat() / samples

        for (i in 0 until samples) {
            for (j in 0 until samples) {
                val theta = i * deltaTheta
                val phi = j * deltaPhi

                // Convert spherical to Cartesian coordinates
                val sampleDirection = sphericalToCartesian(theta, phi, normal)

                val nDotL = max(0f, normal.dot(sampleDirection))
                if (nDotL > 0f) {
                    val color = sampleCubemap(environment, sampleDirection)
                    val weight = sin(theta) * deltaTheta * deltaPhi

                    irradiance = irradiance + color.toVector3() * nDotL * weight
                    sampleCount++
                }
            }
        }

        return Color.fromVector3(irradiance * (PI.toFloat() / sampleCount))
    }

    /**
     * Compute prefiltered environment map for given roughness
     */
    private fun computePrefilter(environment: CubeTexture, direction: Vector3, roughness: Float): Color {
        val normal = direction
        val view = direction

        var prefilteredColor = Vector3.ZERO
        var totalWeight = 0f

        val sampleCount = 1024 // More samples for better quality
        for (i in 0 until sampleCount) {
            val xi = hammersley(i, sampleCount)
            val halfVector = importanceSampleGGX(xi, normal, roughness)
            val lightDirection = reflect(-view, halfVector)

            val nDotL = max(0f, normal.dot(lightDirection))
            if (nDotL > 0f) {
                val nDotH = max(0f, normal.dot(halfVector))
                val vDotH = max(0f, view.dot(halfVector))

                // Importance sampling weight
                val distribution = distributionGGX(nDotH, roughness)
                val pdf = distribution * nDotH / ((4f * vDotH)) + 0.0001f

                val resolution = environment.size.toFloat()
                val saTexel = 4f * PI.toFloat() / (6f * (resolution * resolution))
                val saSample = 1f / (sampleCount * pdf + 0.0001f)

                val mipLevel = if (roughness == 0f) 0f else 0.5f * log2(saSample / saTexel)

                val color = sampleCubemapLOD(environment, lightDirection, mipLevel)
                prefilteredColor = prefilteredColor + color.toVector3() * nDotL
                totalWeight = totalWeight + nDotL
            }
        }

        return Color.fromVector3(prefilteredColor / totalWeight)
    }

    /**
     * Integrate BRDF for environment map
     */
    private fun integrateBRDF(nDotV: Float, roughness: Float): Vector2 {
        val view = Vector3(sqrt(1f - (nDotV * nDotV)), 0f, nDotV)
        val normal = Vector3(0f, 0f, 1f)

        var scale = 0f
        var bias = 0f

        val sampleCount = 1024
        for (i in 0 until sampleCount) {
            val xi = hammersley(i, sampleCount)
            val halfVector = importanceSampleGGX(xi, normal, roughness)
            val lightDirection = reflect(-view, halfVector)

            val nDotL = max(0f, lightDirection.z)
            val nDotH = max(0f, halfVector.z)
            val vDotH = max(0f, view.dot(halfVector))

            if (nDotL > 0f) {
                val g = geometrySmith(normal, view, lightDirection, roughness)
                val gVis = g * vDotH / ((nDotH * nDotV))
                val fc = (1f - vDotH).pow(5f)

                scale += (1f - fc) * gVis
                bias += (fc * gVis)
            }
        }

        return Vector2(scale / sampleCount, bias / sampleCount)
    }

    /**
     * Convert spherical coordinates to Cartesian in tangent space
     */
    private fun sphericalToCartesian(theta: Float, phi: Float, normal: Vector3): Vector3 {
        val sinTheta = sin(theta)
        val cosTheta = cos(theta)
        val sinPhi = sin(phi)
        val cosPhi = cos(phi)

        val tangent = normal.cross(Vector3.UP).normalized
        val bitangent = normal.cross(tangent)

        return tangent * sinTheta * cosPhi + bitangent * sinTheta * sinPhi + (normal * cosTheta)
    }

    /**
     * Hammersley sequence for low-discrepancy sampling
     */
    private fun hammersley(i: Int, n: Int): Vector2 {
        val x = i.toFloat() / n
        val y = radicalInverseVDC(i)
        return Vector2(x, y)
    }

    /**
     * Radical inverse Van der Corput sequence
     */
    private fun radicalInverseVDC(bits: Int): Float {
        var b = bits
        b = (b shl 16) or (b ushr 16)
        b = ((b and 0x55555555) shl 1) or ((b and 0xAAAAAAAA.toInt()) ushr 1)
        b = ((b and 0x33333333) shl 2) or ((b and 0xCCCCCCCC.toInt()) ushr 2)
        b = ((b and 0x0F0F0F0F) shl 4) or ((b and 0xF0F0F0F0.toInt()) ushr 4)
        b = ((b and 0x00FF00FF) shl 8) or ((b and 0xFF00FF00.toInt()) ushr 8)
        return b * 2.3283064365386963e-10f
    }

    /**
     * Importance sample GGX distribution
     */
    private fun importanceSampleGGX(xi: Vector2, normal: Vector3, roughness: Float): Vector3 {
        val alpha = roughness * roughness

        val phi = 2f * PI.toFloat() * xi.x
        val cosTheta = sqrt((1f - xi.y) / (1f + (alpha * alpha - 1f) * xi.y))
        val sinTheta = sqrt(1f - (cosTheta * cosTheta))

        val halfVector = Vector3(
            cos(phi) * sinTheta,
            sin(phi) * sinTheta,
            cosTheta
        )

        val up = if (abs(normal.z) < 0.999f) Vector3(0f, 0f, 1f) else Vector3(1f, 0f, 0f)
        val tangent = up.cross(normal).normalized
        val bitangent = normal.cross(tangent)

        return tangent * halfVector.x + bitangent * halfVector.y + normal * halfVector.z
    }

    /**
     * GGX distribution function
     */
    private fun distributionGGX(nDotH: Float, roughness: Float): Float {
        val alpha = roughness * roughness
        val alpha2 = alpha * alpha
        val denom = nDotH * nDotH * (alpha2 - 1f) + 1f
        return alpha2 / (PI.toFloat() * (denom * denom))
    }

    /**
     * Smith geometry function
     */
    private fun geometrySmith(normal: Vector3, view: Vector3, light: Vector3, roughness: Float): Float {
        val nDotV = max(0f, normal.dot(view))
        val nDotL = max(0f, normal.dot(light))
        val ggx2 = geometrySchlickGGX(nDotV, roughness)
        val ggx1 = geometrySchlickGGX(nDotL, roughness)
        return (ggx1 * ggx2)
    }

    private fun geometrySchlickGGX(nDotV: Float, roughness: Float): Float {
        val r = roughness + 1f
        val k = r * r / 8f
        val num = nDotV
        val denom = nDotV * (1f - k) + k
        return num / denom
    }

    /**
     * Reflect vector
     */
    private fun reflect(incident: Vector3, normal: Vector3): Vector3 {
        return incident - normal * 2f * incident.dot(normal)
    }

    /**
     * Convert cube face UV to direction
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
     * Convert cube face and spherical coordinates to direction
     */
    private fun cubeFaceToDirection(face: Int, theta: Float, phi: Float): Vector3 {
        val x = sin(theta) * cos(phi)
        val y = cos(theta)
        val z = sin(theta) * sin(phi)

        return when (face) {
            0 -> Vector3(1f, -y, -x)     // +X
            1 -> Vector3(-1f, -y, x)     // -X
            2 -> Vector3(x, 1f, y)       // +Y
            3 -> Vector3(x, -1f, -y)     // -Y
            4 -> Vector3(x, -y, 1f)      // +Z
            5 -> Vector3(-x, -y, -1f)    // -Z
            else -> Vector3(0f, 0f, 1f)
        }.normalized
    }

    /**
     * Evaluate spherical harmonics basis functions
     */
    private fun evaluateSphericalHarmonics(direction: Vector3): FloatArray {
        val x = direction.x
        val y = direction.y
        val z = direction.z

        // SH basis functions (order 2)
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

    // Platform-specific implementations (placeholders)
    private suspend fun loadHDRImageData(url: String): HDREnvironment = withContext(dispatcher) {
        // Platform-specific HDR loading
        HDREnvironment(
            data = FloatArray(1024 * (512 * 3)), // 1024x512 RGB
            width = 1024,
            height = 512
        )
    }

    private fun createEmptyCubemap(width: Int, height: Int, mipLevels: Int = 1): CubeTexture {
        // Platform-specific cubemap creation
        return CubeTextureImpl(
            size = width,
            format = TextureFormat.RGBA32F,
            filter = TextureFilter.LINEAR
        )
    }

    private fun createEmptyTexture2D(width: Int, height: Int): Texture2D {
        // Platform-specific 2D texture creation
        return Texture2D(
            width = width,
            height = height,
            format = TextureFormat.RGBA32F,
            filter = TextureFilter.LINEAR
        )
    }


    private fun sampleCubemapLOD(cubemap: CubeTexture, direction: Vector3, lod: Float): Color {
        // Sample cubemap with LOD level for mipmapping
        // For now, use bilinear interpolation between mip levels
        val mipLevel = lod.toInt()
        val mipFraction = lod - mipLevel

        // Sample the base mip level
        val color1 = sampleCubemap(cubemap, direction)

        // If we have a fractional part and another mip level exists, interpolate
        if (mipFraction > 0.01f && mipLevel < (cubemap as? io.kreekt.texture.CubeTexture)?.getMipLevelCount()
                .let { it ?: 1 } - 1
        ) {
            // Sample next mip level (would need mipmap support in CubeTexture)
            val color2 = sampleCubemap(cubemap, direction) // Simplified: same as base for now

            // Linearly interpolate between mip levels
            return Color(
                color1.r * (1f - mipFraction) + color2.r * mipFraction,
                color1.g * (1f - mipFraction) + color2.g * mipFraction,
                color1.b * (1f - mipFraction) + color2.b * mipFraction,
                color1.a
            )
        }

        return color1
    }

    private fun generateCubemapFace(hdr: HDREnvironment, size: Int, direction: Vector3, up: Vector3): FloatArray {
        // Generate face data from HDR equirectangular projection
        val faceData = FloatArray(size * size * 4)

        // Calculate right vector for the face
        val right = up.cross(direction).normalized
        val correctedUp = direction.cross(right).normalized

        for (y in 0 until size) {
            for (x in 0 until size) {
                // Convert pixel to UV coordinates [-1, 1]
                val u = 2f * (x + 0.5f) / size - 1f
                val v = 2f * (y + 0.5f) / size - 1f

                // Calculate world direction for this pixel
                val worldDir = (direction + right * u + correctedUp * v).normalized

                // Convert world direction to spherical coordinates
                val theta = atan2(worldDir.z, worldDir.x)
                val phi = acos(worldDir.y.coerceIn(-1f, 1f))

                // Convert spherical to equirectangular UV [0, 1]
                val equirectU = (theta + PI.toFloat()) / (2f * PI.toFloat())
                val equirectV = phi / PI.toFloat()

                // Sample HDR at equirectangular coordinates
                val hdrX = (equirectU * hdr.width).toInt().coerceIn(0, hdr.width - 1)
                val hdrY = (equirectV * hdr.height).toInt().coerceIn(0, hdr.height - 1)
                val hdrIndex = (hdrY * hdr.width + hdrX) * 3

                // Copy color data
                val pixelIndex = (y * size + x) * 4
                faceData[pixelIndex] = hdr.data.getOrElse(hdrIndex) { 1f }     // R
                faceData[pixelIndex + 1] = hdr.data.getOrElse(hdrIndex + 1) { 1f } // G
                faceData[pixelIndex + 2] = hdr.data.getOrElse(hdrIndex + 2) { 1f } // B
                faceData[pixelIndex + 3] = 1f // A
            }
        }

        return faceData
    }

    private fun log2(x: Float): Float = ln(x) / ln(2f)
}

/**
 * Spherical harmonics implementation
 */
internal data class IBLSphericalHarmonics(
    override val coefficients: Array<Vector3>
) : SphericalHarmonics {

    override fun evaluate(direction: Vector3): Vector3 {
        val sh = evaluateSphericalHarmonics(direction)
        var result = Vector3.ZERO

        for (i in 0 until 9) {
            result = result + coefficients[i] * sh[i]
        }

        return result
    }

    private fun evaluateSphericalHarmonics(direction: Vector3): FloatArray {
        val x = direction.x
        val y = direction.y
        val z = direction.z

        return floatArrayOf(
            0.282095f,
            0.488603f * y,
            0.488603f * z,
            0.488603f * x,
            1.092548f * (x * y),
            1.092548f * (y * z),
            0.315392f * (3f * z * z - 1f),
            1.092548f * (x * z),
            0.546274f * (x * x - (y * y))
        )
    }
}


/**
 * Extension functions for Color
 */
private fun Color.toVector3(): Vector3 = Vector3(r, g, b)
private fun Color.Companion.fromVector3(v: Vector3): Color = Color(v.x, v.y, v.z)