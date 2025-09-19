/**
 * Advanced Light Implementations
 * Provides AreaLight and VolumetricLight with physically-based lighting calculations
 */
package io.kreekt.lighting

import io.kreekt.core.math.*
import io.kreekt.renderer.*
import io.kreekt.material.Texture
import io.kreekt.material.Texture3D
import kotlin.math.*

/**
 * Area light implementation with different shapes and physical light units
 * Supports rectangle, disk, sphere, and tube shapes with proper energy distribution
 */
class AreaLightImpl(
    override var width: Float = 1.0f,
    override var height: Float = 1.0f,
    override var shape: AreaLightShape = AreaLightShape.RECTANGLE,
    override var texture: Texture? = null,
    override var doubleSided: Boolean = false
) : AreaLight {

    override var intensity: Float = 1.0f
    override var color: Color = Color.WHITE
    override var position: Vector3 = Vector3.ZERO
    override var rotation: Quaternion = Quaternion.IDENTITY
    override var castShadow: Boolean = true
    override var visible: Boolean = true

    // Physical light properties
    var luminousFlux: Float = 1000.0f // lumens
    var luminousIntensity: Float = 100.0f // candela
    var colorTemperature: Float = 6500.0f // Kelvin
    var efficacy: Float = 683.0f // lm/W (maximum luminous efficacy)

    // Attenuation properties
    var constantAttenuation: Float = 1.0f
    var linearAttenuation: Float = 0.0f
    var quadraticAttenuation: Float = 1.0f
    var maxDistance: Float = Float.POSITIVE_INFINITY

    // Shape-specific properties
    var radius: Float = 0.5f // for disk and sphere shapes
    var tubeLength: Float = 1.0f // for tube shape

    /**
     * Calculate light intensity at a given distance and surface point
     */
    fun calculateIntensity(distance: Float, surfacePoint: Vector3, surfaceNormal: Vector3): Float {
        if (distance > maxDistance) return 0.0f

        // Calculate attenuation
        val attenuation = constantAttenuation +
                         linearAttenuation * distance +
                         quadraticAttenuation * distance * distance

        // Shape-specific intensity calculation
        val shapeIntensity = when (shape) {
            AreaLightShape.RECTANGLE -> calculateRectangleIntensity(surfacePoint, surfaceNormal)
            AreaLightShape.DISK -> calculateDiskIntensity(surfacePoint, surfaceNormal)
            AreaLightShape.SPHERE -> calculateSphereIntensity(surfacePoint, surfaceNormal)
            AreaLightShape.TUBE -> calculateTubeIntensity(surfacePoint, surfaceNormal)
        }

        return (intensity * shapeIntensity) / attenuation
    }

    /**
     * Calculate color temperature in RGB
     */
    fun calculateColorFromTemperature(): Color {
        val temp = colorTemperature / 100.0f

        val red = when {
            temp <= 66 -> 255f
            else -> 329.698727446f * (temp - 60).pow(-0.1332047592f)
        }.coerceIn(0f, 255f)

        val green = when {
            temp <= 66 -> 99.4708025861f * ln(temp) - 161.1195681661f
            else -> 288.1221695283f * (temp - 60).pow(-0.0755148492f)
        }.coerceIn(0f, 255f)

        val blue = when {
            temp >= 66 -> 255f
            temp <= 19 -> 0f
            else -> 138.5177312231f * ln(temp - 10) - 305.0447927307f
        }.coerceIn(0f, 255f)

        return Color(red / 255f, green / 255f, blue / 255f, 1f)
    }

    /**
     * Get the effective area of the light source
     */
    fun getEffectiveArea(): Float = when (shape) {
        AreaLightShape.RECTANGLE -> width * height
        AreaLightShape.DISK -> PI.toFloat() * radius * radius
        AreaLightShape.SPHERE -> 4f * PI.toFloat() * radius * radius
        AreaLightShape.TUBE -> 2f * PI.toFloat() * radius * tubeLength
    }

    /**
     * Convert luminous flux to intensity for the given shape
     */
    fun fluxToIntensity(): Float {
        val area = getEffectiveArea()
        return luminousFlux / (PI.toFloat() * area)
    }

    /**
     * Calculate lighting contribution for PBR materials
     */
    fun calculatePBRContribution(
        surfacePoint: Vector3,
        surfaceNormal: Vector3,
        viewDirection: Vector3,
        roughness: Float,
        metallic: Float,
        albedo: Color
    ): Color {
        val lightDir = (position - surfacePoint).normalized()
        val distance = position.distanceTo(surfacePoint)
        val lightIntensity = calculateIntensity(distance, surfacePoint, surfaceNormal)

        // Cook-Torrance BRDF calculation
        val halfVector = (lightDir + viewDirection).normalized()
        val nDotL = max(0f, surfaceNormal.dot(lightDir))
        val nDotV = max(0f, surfaceNormal.dot(viewDirection))
        val nDotH = max(0f, surfaceNormal.dot(halfVector))
        val vDotH = max(0f, viewDirection.dot(halfVector))

        // Fresnel term (Schlick approximation)
        val f0 = Color.mix(Color(0.04f, 0.04f, 0.04f), albedo, metallic)
        val fresnel = f0 + (Color.WHITE - f0) * (1f - vDotH).pow(5f)

        // Distribution term (GGX/Trowbridge-Reitz)
        val alpha = roughness * roughness
        val alpha2 = alpha * alpha
        val denom = nDotH * nDotH * (alpha2 - 1f) + 1f
        val distribution = alpha2 / (PI.toFloat() * denom * denom)

        // Geometry term (Smith G-function)
        val k = (roughness + 1f) * (roughness + 1f) / 8f
        val g1L = nDotL / (nDotL * (1f - k) + k)
        val g1V = nDotV / (nDotV * (1f - k) + k)
        val geometry = g1L * g1V

        // Final BRDF
        val specular = (distribution * geometry * fresnel) / (4f * nDotL * nDotV + 0.001f)
        val diffuse = albedo / PI.toFloat() * (Color.WHITE - fresnel) * (1f - metallic)

        return (diffuse + specular) * color * lightIntensity * nDotL
    }

    private fun calculateRectangleIntensity(surfacePoint: Vector3, surfaceNormal: Vector3): Float {
        // Simplified rectangular area light calculation
        // In practice, would use more sophisticated integration methods
        val lightToSurface = (surfacePoint - position).normalized()
        val lightNormal = Vector3(0f, 0f, 1f).transformByQuaternion(rotation)
        val cosTheta = max(0f, -lightNormal.dot(lightToSurface))

        if (!doubleSided && cosTheta <= 0f) return 0f

        return if (doubleSided) abs(cosTheta) else cosTheta
    }

    private fun calculateDiskIntensity(surfacePoint: Vector3, surfaceNormal: Vector3): Float {
        val lightToSurface = (surfacePoint - position).normalized()
        val lightNormal = Vector3(0f, 0f, 1f).transformByQuaternion(rotation)
        val cosTheta = max(0f, -lightNormal.dot(lightToSurface))

        if (!doubleSided && cosTheta <= 0f) return 0f

        return if (doubleSided) abs(cosTheta) else cosTheta
    }

    private fun calculateSphereIntensity(surfacePoint: Vector3, surfaceNormal: Vector3): Float {
        // Spherical lights emit uniformly in all directions
        return 1.0f
    }

    private fun calculateTubeIntensity(surfacePoint: Vector3, surfaceNormal: Vector3): Float {
        // Simplified tube light calculation
        // Would need line-to-point distance calculation for accuracy
        val lightToSurface = (surfacePoint - position).normalized()
        val tubeAxis = Vector3(1f, 0f, 0f).transformByQuaternion(rotation)
        val perpendicular = lightToSurface - (lightToSurface.dot(tubeAxis) * tubeAxis)
        val distance = perpendicular.magnitude()

        return 1.0f / (1.0f + distance)
    }
}

/**
 * Volumetric light implementation with scattering and phase functions
 * Supports atmospheric and participating media effects
 */
class VolumetricLightImpl(
    override var volumeTexture: Texture3D? = null,
    override var scattering: Float = 0.1f,
    override var extinction: Float = 0.1f,
    override var phase: Float = 0.0f,
    override var steps: Int = 32
) : VolumetricLight {

    override var intensity: Float = 1.0f
    override var color: Color = Color.WHITE
    override var position: Vector3 = Vector3.ZERO
    override var rotation: Quaternion = Quaternion.IDENTITY
    override var castShadow: Boolean = false // Volumetric lights typically don't cast sharp shadows
    override var visible: Boolean = true

    // Volumetric properties
    var density: Float = 1.0f
    var anisotropy: Float = 0.0f // -1 (backward) to 1 (forward) scattering
    var fogColor: Color = Color(0.8f, 0.9f, 1.0f) // Atmospheric blue
    var fogDensity: Float = 0.01f
    var heightFalloff: Float = 0.0f
    var windDirection: Vector3 = Vector3(1f, 0f, 0f)
    var windStrength: Float = 0.0f

    // Noise properties for dynamic effects
    var noiseScale: Float = 1.0f
    var noiseSpeed: Float = 1.0f
    var noiseStrength: Float = 0.1f

    /**
     * Calculate volumetric scattering using ray marching
     */
    fun calculateVolumetricScattering(
        rayStart: Vector3,
        rayEnd: Vector3,
        cameraPosition: Vector3,
        time: Float = 0f
    ): Color {
        val rayDirection = (rayEnd - rayStart).normalized()
        val rayLength = rayStart.distanceTo(rayEnd)
        val stepSize = rayLength / steps

        var accumColor = Color.BLACK
        var transmittance = 1.0f

        for (i in 0 until steps) {
            val t = (i + 0.5f) * stepSize
            val currentPos = rayStart + rayDirection * t

            // Sample density at current position
            val localDensity = sampleDensity(currentPos, time)
            if (localDensity <= 0f) continue

            // Calculate lighting at this point
            val lightDir = (position - currentPos).normalized()
            val viewDir = (cameraPosition - currentPos).normalized()

            // Phase function (Henyey-Greenstein)
            val phaseValue = calculatePhaseFunction(lightDir, viewDir, anisotropy)

            // In-scattering
            val scatteringCoeff = scattering * localDensity
            val extinctionCoeff = extinction * localDensity

            val lightAttenuation = calculateLightAttenuation(currentPos)
            val inscattering = scatteringCoeff * phaseValue * lightAttenuation * intensity

            // Volumetric contribution
            val sampleContribution = inscattering * color * transmittance * stepSize
            accumColor += sampleContribution

            // Update transmittance
            transmittance *= exp(-extinctionCoeff * stepSize)

            if (transmittance < 0.01f) break // Early termination
        }

        return accumColor
    }

    /**
     * Sample density at a given position with noise
     */
    fun sampleDensity(position: Vector3, time: Float): Float {
        var baseDensity = density

        // Height-based falloff
        if (heightFalloff > 0f) {
            val height = position.y - this.position.y
            baseDensity *= exp(-height * heightFalloff)
        }

        // Wind effect
        if (windStrength > 0f) {
            val windOffset = windDirection * windStrength * time
            val windPosition = position + windOffset
            baseDensity *= sampleWindNoise(windPosition)
        }

        // 3D texture sampling
        volumeTexture?.let { texture ->
            val uvw = worldToTextureCoordinates(position)
            baseDensity *= sampleTexture3D(texture, uvw)
        }

        // Procedural noise
        if (noiseStrength > 0f) {
            val noisePos = position * noiseScale + Vector3.ONE * time * noiseSpeed
            val noise = simplexNoise3D(noisePos)
            baseDensity *= (1f + noise * noiseStrength)
        }

        return max(0f, baseDensity)
    }

    /**
     * Henyey-Greenstein phase function
     */
    private fun calculatePhaseFunction(lightDir: Vector3, viewDir: Vector3, g: Float): Float {
        val cosTheta = lightDir.dot(viewDir)
        val g2 = g * g
        val denom = 1f + g2 - 2f * g * cosTheta
        return (1f - g2) / (4f * PI.toFloat() * denom * sqrt(denom))
    }

    /**
     * Calculate light attenuation based on distance and medium properties
     */
    private fun calculateLightAttenuation(samplePos: Vector3): Float {
        val distance = position.distanceTo(samplePos)

        // Inverse square law with minimum distance
        val minDistance = 0.1f
        val attenuationDistance = max(distance, minDistance)

        // Atmospheric absorption
        val atmosphericExtinction = exp(-distance * fogDensity)

        return atmosphericExtinction / (attenuationDistance * attenuationDistance)
    }

    /**
     * Convert world position to 3D texture coordinates
     */
    private fun worldToTextureCoordinates(worldPos: Vector3): Vector3 {
        // Simple box mapping - could be extended for more complex shapes
        val localPos = worldPos - position
        val size = Vector3(10f, 10f, 10f) // Volume size
        return (localPos + size * 0.5f) / size
    }

    /**
     * Sample 3D texture (placeholder - would use actual texture sampling)
     */
    private fun sampleTexture3D(texture: Texture3D, uvw: Vector3): Float {
        // Placeholder implementation
        // In practice, this would sample the actual 3D texture
        return 1.0f
    }

    /**
     * Generate wind-based noise
     */
    private fun sampleWindNoise(position: Vector3): Float {
        // Simple turbulence model
        val octaves = 3
        var value = 0f
        var amplitude = 1f
        var frequency = 1f

        for (i in 0 until octaves) {
            value += amplitude * simplexNoise3D(position * frequency)
            amplitude *= 0.5f
            frequency *= 2f
        }

        return (value + 1f) * 0.5f // Normalize to [0,1]
    }

    /**
     * 3D Simplex noise (simplified implementation)
     */
    private fun simplexNoise3D(p: Vector3): Float {
        // Simplified 3D noise - in practice would use a proper noise library
        val x = p.x * 0.1f
        val y = p.y * 0.1f
        val z = p.z * 0.1f

        return sin(x) * cos(y) * sin(z) * 0.5f
    }

    /**
     * Calculate fog contribution for atmospheric effects
     */
    fun calculateFogContribution(
        cameraPosition: Vector3,
        fragmentPosition: Vector3,
        fragmentColor: Color
    ): Color {
        val distance = cameraPosition.distanceTo(fragmentPosition)
        val fogFactor = 1f - exp(-distance * fogDensity)

        return Color.mix(fragmentColor, fogColor, fogFactor.coerceIn(0f, 1f))
    }

    /**
     * Calculate god rays effect
     */
    fun calculateGodRays(
        rayStart: Vector3,
        rayEnd: Vector3,
        samples: Int = 16
    ): Float {
        val rayDirection = (rayEnd - rayStart).normalized()
        val rayLength = rayStart.distanceTo(rayEnd)
        val stepSize = rayLength / samples

        var rayStrength = 0f

        for (i in 0 until samples) {
            val t = i * stepSize
            val samplePos = rayStart + rayDirection * t

            // Check if sample point is within light volume
            val distanceToLight = position.distanceTo(samplePos)
            val lightContribution = 1f / (1f + distanceToLight)

            rayStrength += lightContribution * stepSize
        }

        return rayStrength / rayLength
    }
}

/**
 * Light intensity calculation utilities
 */
object LightUtils {
    /**
     * Convert between different light units
     */
    fun lumensToWatts(lumens: Float, efficacy: Float = 683f): Float = lumens / efficacy
    fun wattsToLumens(watts: Float, efficacy: Float = 683f): Float = watts * efficacy
    fun candelaToLumens(candela: Float, solidAngle: Float = 4f * PI.toFloat()): Float = candela * solidAngle

    /**
     * Calculate luminance from luminous intensity
     */
    fun calculateLuminance(intensity: Float, area: Float): Float = intensity / area

    /**
     * Convert color temperature to RGB
     */
    fun kelvinToRGB(kelvin: Float): Color {
        val temp = kelvin / 100f

        val red = when {
            temp <= 66 -> 255f
            else -> 329.698727446f * (temp - 60).pow(-0.1332047592f)
        }.coerceIn(0f, 255f)

        val green = when {
            temp <= 66 -> 99.4708025861f * ln(temp) - 161.1195681661f
            else -> 288.1221695283f * (temp - 60).pow(-0.0755148492f)
        }.coerceIn(0f, 255f)

        val blue = when {
            temp >= 66 -> 255f
            temp <= 19 -> 0f
            else -> 138.5177312231f * ln(temp - 10) - 305.0447927307f
        }.coerceIn(0f, 255f)

        return Color(red / 255f, green / 255f, blue / 255f, 1f)
    }
}