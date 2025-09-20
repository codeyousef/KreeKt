/**
 * PBRMaterial with clearcoat, transmission, iridescence
 * T026 - Advanced physically-based rendering material with complete PBR feature set
 *
 * Features:
 * - Standard PBR workflow (metallic/roughness)
 * - Clearcoat layer for automotive/glossy surfaces
 * - Transmission for glass and transparent materials
 * - Iridescence for soap bubbles, oil films, etc.
 * - Subsurface scattering for organic materials
 * - Anisotropic reflections for brushed metals
 * - Sheen for fabric materials
 * - Advanced texture mapping and UV animation
 */
package io.kreekt.material
import io.kreekt.renderer.Texture3D
import io.kreekt.renderer.Texture2D
import io.kreekt.renderer.CubeTexture
import io.kreekt.core.scene.Material
import io.kreekt.lighting.IBLResult
import io.kreekt.core.math.*
import io.kreekt.core.platform.platformClone
import io.kreekt.core.platform.currentTimeMillis
import kotlinx.serialization.Serializable
import kotlin.math.*

// Missing type definitions for PBR features
data class ClearcoatSettings(
    var intensity: Float = 0f,
    var roughness: Float = 0f,
    var normal: Float = 1f,
    var texture: Texture2D? = null,
    var roughnessTexture: Texture2D? = null,
    var normalTexture: Texture2D? = null
)

data class TransmissionSettings(
    var intensity: Float = 0f,
    var thickness: Float = 0f,
    var attenuationDistance: Float = Float.POSITIVE_INFINITY,
    var attenuationColor: Color = Color.WHITE,
    var texture: Texture2D? = null,
    var thicknessTexture: Texture2D? = null
)

data class IridescenceSettings(
    var intensity: Float = 0f,
    var ior: Float = 1.3f,
    var thicknessMin: Float = 100f,
    var thicknessMax: Float = 400f,
    var texture: Texture2D? = null,
    var thicknessTexture: Texture2D? = null
)

data class SubsurfaceSettings(
    var intensity: Float = 0f,
    var radius: Vector3 = Vector3(1f, 0.2f, 0.1f),
    var color: Color = Color.WHITE,
    var texture: Texture2D? = null,
    var radiusTexture: Texture2D? = null
)

data class AnisotropySettings(
    var intensity: Float = 0f,
    var direction: Vector2 = Vector2(1f, 0f),
    var texture: Texture2D? = null
)

data class SheenSettings(
    var intensity: Float = 0f,
    var color: Color = Color.WHITE,
    var roughness: Float = 0f,
    var texture: Texture2D? = null,
    var roughnessTexture: Texture2D? = null
)

typealias MaterialAnimator = Any
typealias PBRShaderGenerator = Any
typealias MaterialLighting = Any

/**
 * Advanced PBR material with physically-based lighting model
 * Implements the latest glTF 2.0 PBR extensions and real-time rendering techniques
class PBRMaterial(
    // Core PBR parameters
    var albedo: Color = Color.WHITE,
    var metallic: Float = 0f,
    var roughness: Float = 1f,
    var normal: Float = 1f,
    var emissive: Color = Color.BLACK,
    var emissiveIntensity: Float = 1f,
    var ao: Float = 1f, // Ambient occlusion
    // Advanced PBR features
    var clearcoat: ClearcoatSettings = ClearcoatSettings(),
    var transmission: TransmissionSettings = TransmissionSettings(),
    var iridescence: IridescenceSettings = IridescenceSettings(),
    var subsurface: SubsurfaceSettings = SubsurfaceSettings(),
    var anisotropy: AnisotropySettings = AnisotropySettings(),
    var sheen: SheenSettings = SheenSettings(),
    // Environment and lighting
    var envMapIntensity: Float = 1f,
    var reflectance: Float = 0.04f, // F0 for dielectrics
    // Material properties
    var transparent: Boolean = false,
    var alphaMode: AlphaMode = AlphaMode.OPAQUE,
    var alphaCutoff: Float = 0.5f,
    var doubleSided: Boolean = false,
    var shadowSide: MaterialSide = MaterialSide.FRONT,
    materialName: String = "PBRMaterial"
) : Material {
    override val id: Int = generateId()
    override val name: String = materialName
    override var needsUpdate: Boolean = true
    override var visible: Boolean = true

    private companion object {
        private var nextId = 0
        private fun generateId(): Int = ++nextId
    }
    // Texture maps for all PBR channels
    var albedoMap: Texture? = null
    var metallicRoughnessMap: Texture? = null
    var normalMap: Texture? = null
    var emissiveMap: Texture? = null
    var aoMap: Texture? = null
    var envMap: CubeTexture? = null
    // Advanced texture maps
    var clearcoatMap: Texture? = null
    var clearcoatRoughnessMap: Texture? = null
    var clearcoatNormalMap: Texture? = null
    var transmissionMap: Texture? = null
    var thicknessMap: Texture? = null
    var iridescenceMap: Texture? = null
    var iridescenceThicknessMap: Texture? = null
    var sheenColorMap: Texture? = null
    var sheenRoughnessMap: Texture? = null
    var specularMap: Texture? = null
    var specularColorMap: Texture? = null
    // UV transformation and animation
    var uvTransform: UVTransform = UVTransform()
    var uvAnimation: UVAnimation? = null
    // Quality and optimization settings
    var quality: MaterialQuality = MaterialQuality.HIGH
    var optimizations: MaterialOptimizations = MaterialOptimizations()
    /**
     * Calculate the effective albedo including texture influence
     */
    fun getEffectiveAlbedo(uv: Vector2): Color {
        val baseColor = albedo.clone()
        albedoMap?.let { texture ->
            val texelColor = texture.sample(uv)
            baseColor.multiply(texelColor)
        }
        return baseColor
    }
     * Calculate metallic-roughness values from texture
    fun getMetallicRoughness(uv: Vector2): Vector2 {
        metallicRoughnessMap?.let { texture ->
            val texelValue = texture.sample(uv)
            // OpenGL convention: G=roughness, B=metallic
            return Vector2(texelValue.(b * metallic), texelValue.(g * roughness))
        return Vector2(metallic, roughness)
     * Calculate normal vector from normal map
    fun getNormal(uv: Vector2, tangent: Vector3, bitangent: Vector3, normal: Vector3): Vector3 {
        normalMap?.let { texture ->
            val normalSample = texture.sample(uv)
            // Convert from [0,1] to [-1,1] range
            val normalTangent = Vector3(
                normalSample.r * 2f - 1f,
                normalSample.g * 2f - 1f,
                normalSample.b * 2f - 1f
            )
            // Transform from tangent space to world space
            val worldNormal = Vector3()
            worldNormal.add(tangent.clone().multiplyScalar(normalTangent.x))
            worldNormal.add(bitangent.clone().multiplyScalar(normalTangent.y))
            worldNormal.add(normal.clone().multiplyScalar(normalTangent.z))
            return worldNormal.normalize()
        return normal.clone()
     * Calculate emissive contribution
    fun getEmissive(uv: Vector2): Color {
        val emissiveColor = emissive.clone().multiplyScalar(emissiveIntensity)
        emissiveMap?.let { texture ->
            emissiveColor.multiply(texelColor)
        return emissiveColor
     * Calculate clearcoat layer contribution
    fun getClearcoatContribution(uv: Vector2): ClearcoatContribution {
        if (!clearcoat.enabled) {
            return ClearcoatContribution()
        var intensity = clearcoat.intensity
        var roughness = clearcoat.roughness
        var normal = Vector3(0f, 0f, 1f) // Default tangent space normal
        clearcoatMap?.let { texture ->
            intensity = intensity * texture.sample(uv).r
        clearcoatRoughnessMap?.let { texture ->
            roughness = roughness * texture.sample(uv).g
        clearcoatNormalMap?.let { texture ->
            normal.set(
            ).normalize()
        return ClearcoatContribution(
            intensity = intensity,
            roughness = roughness,
            normal = normal,
            ior = clearcoat.ior
        )
     * Calculate transmission properties for glass-like materials
    fun getTransmissionContribution(uv: Vector2): TransmissionContribution {
        if (!transmission.enabled) {
            return TransmissionContribution()
        var factor = transmission.factor
        var thickness = transmission.thickness
        var attenuationDistance = transmission.attenuationDistance
        var attenuationColor = transmission.attenuationColor.clone()
        transmissionMap?.let { texture ->
            factor = factor * texture.sample(uv).r
        thicknessMap?.let { texture ->
            thickness = thickness * texture.sample(uv).g
        return TransmissionContribution(
            factor = factor,
            thickness = thickness,
            ior = transmission.ior,
            attenuationDistance = attenuationDistance,
            attenuationColor = attenuationColor
     * Calculate iridescence effects for thin film interference
    fun getIridescenceContribution(uv: Vector2): IridescenceContribution {
        if (!iridescence.enabled) {
            return IridescenceContribution()
        var factor = iridescence.factor
        var thickness = iridescence.thickness
        iridescenceMap?.let { texture ->
        iridescenceThicknessMap?.let { texture ->
        // Calculate iridescence color based on thin film interference
        val iridescenceColor = calculateIridescenceColor(thickness, iridescence.ior)
        return IridescenceContribution(
            ior = iridescence.ior,
            color = iridescenceColor
     * Calculate subsurface scattering for organic materials
    fun getSubsurfaceContribution(uv: Vector2): SubsurfaceContribution {
        if (!subsurface.enabled) {
            return SubsurfaceContribution()
        return SubsurfaceContribution(
            factor = subsurface.factor,
            color = subsurface.color.clone(),
            distanceScale = subsurface.distanceScale,
            thickness = subsurface.thickness
     * Calculate anisotropic reflection properties
    fun getAnisotropyContribution(uv: Vector2): AnisotropyContribution {
        if (!anisotropy.enabled) {
            return AnisotropyContribution()
        return AnisotropyContribution(
            strength = anisotropy.strength,
            direction = anisotropy.direction.clone(),
            aspectRatio = anisotropy.aspectRatio
     * Calculate sheen for fabric-like materials
    fun getSheenContribution(uv: Vector2): SheenContribution {
        if (!sheen.enabled) {
            return SheenContribution()
        var color = sheen.color.clone()
        var roughness = sheen.roughness
        sheenColorMap?.let { texture ->
            color.multiply(texture.sample(uv))
        sheenRoughnessMap?.let { texture ->
            roughness = roughness * texture.sample(uv).a
        return SheenContribution(
            color = color,
            intensity = sheen.intensity
     * Get transformed UV coordinates with animation
    fun getTransformedUV(baseUV: Vector2, time: Float = 0f): Vector2 {
        var uv = baseUV.clone()
        // Apply static UV transform
        uv = applyUVTransform(uv, uvTransform)
        // Apply UV animation if present
        uvAnimation?.let { anim ->
            val animTransform = anim.getTransformAtTime(time)
            uv = applyUVTransform(uv, animTransform)
        return uv
     * Validate material parameters for rendering
    fun validate(): MaterialValidationResult {
        val issues = mutableListOf<String>()
        // Validate basic PBR parameters
        if (metallic < 0f || metallic > 1f) {
            issues.add("Metallic value should be in range [0,1], found: $metallic")
        if (roughness < 0f || roughness > 1f) {
            issues.add("Roughness value should be in range [0,1], found: $roughness")
        if (reflectance < 0f || reflectance > 1f) {
            issues.add("Reflectance value should be in range [0,1], found: $reflectance")
        // Validate clearcoat parameters
        if (clearcoat.enabled) {
            if (clearcoat.intensity < 0f || clearcoat.intensity > 1f) {
                issues.add("Clearcoat intensity should be in range [0,1]")
            }
            if (clearcoat.ior < 1f) {
                issues.add("Clearcoat IOR should be >= 1.0")
        // Validate transmission parameters
        if (transmission.enabled) {
            if (transmission.factor < 0f || transmission.factor > 1f) {
                issues.add("Transmission factor should be in range [0,1]")
            if (transmission.ior < 1f) {
                issues.add("Transmission IOR should be >= 1.0")
        // Validate texture compatibility
        validateTextureCompatibility(issues)
        return MaterialValidationResult(
            isValid = issues.isEmpty(),
            issues = issues
     * Optimize material for target quality level
    fun optimize(targetQuality: MaterialQuality): PBRMaterial {
        val optimized = clone()
        optimized.quality = targetQuality
        when (targetQuality) {
            MaterialQuality.LOW -> {
                // Disable expensive features
                optimized.clearcoat.enabled = false
                optimized.transmission.enabled = false
                optimized.iridescence.enabled = false
                optimized.subsurface.enabled = false
                optimized.anisotropy.enabled = false
                optimized.sheen.enabled = false
                // Reduce texture resolution hints
                optimized.optimizations.maxTextureSize = 512
                optimized.optimizations.useCompression = true
            MaterialQuality.MEDIUM -> {
                // Enable selective features
                optimized.clearcoat.enabled = clearcoat.enabled && clearcoat.intensity > 0.1f
                optimized.transmission.enabled = transmission.enabled && transmission.factor > 0.1f
                optimized.iridescence.enabled = false // Expensive
                optimized.subsurface.enabled = false // Expensive
                optimized.optimizations.maxTextureSize = 1024
            MaterialQuality.HIGH,
            MaterialQuality.ULTRA -> {
                // Keep all features enabled
                optimized.optimizations.maxTextureSize = if (targetQuality == MaterialQuality.ULTRA) 4096 else 2048
                optimized.optimizations.useCompression = false
        return optimized
     * Clone the material with all properties
    fun clone(): PBRMaterial {
        return PBRMaterial(
            albedo = albedo.clone(),
            metallic = metallic,
            emissive = emissive.clone(),
            emissiveIntensity = emissiveIntensity,
            ao = ao,
            clearcoat = clearcoat.copy(),
            transmission = transmission.copy(),
            iridescence = iridescence.copy(),
            subsurface = subsurface.copy(),
            anisotropy = anisotropy.copy(),
            sheen = sheen.copy(),
            envMapIntensity = envMapIntensity,
            reflectance = reflectance,
            transparent = transparent,
            alphaMode = alphaMode,
            alphaCutoff = alphaCutoff,
            doubleSided = doubleSided,
            shadowSide = shadowSide,
            name = name
        ).apply {
            // Copy texture references
            albedoMap = this@PBRMaterial.albedoMap
            metallicRoughnessMap = this@PBRMaterial.metallicRoughnessMap
            normalMap = this@PBRMaterial.normalMap
            emissiveMap = this@PBRMaterial.emissiveMap
            aoMap = this@PBRMaterial.aoMap
            envMap = this@PBRMaterial.envMap
            clearcoatMap = this@PBRMaterial.clearcoatMap
            clearcoatRoughnessMap = this@PBRMaterial.clearcoatRoughnessMap
            clearcoatNormalMap = this@PBRMaterial.clearcoatNormalMap
            transmissionMap = this@PBRMaterial.transmissionMap
            thicknessMap = this@PBRMaterial.thicknessMap
            iridescenceMap = this@PBRMaterial.iridescenceMap
            iridescenceThicknessMap = this@PBRMaterial.iridescenceThicknessMap
            sheenColorMap = this@PBRMaterial.sheenColorMap
            sheenRoughnessMap = this@PBRMaterial.sheenRoughnessMap
            specularMap = this@PBRMaterial.specularMap
            specularColorMap = this@PBRMaterial.specularColorMap
            // Copy transforms and animations
            uvTransform = this@PBRMaterial.uvTransform.copy()
            uvAnimation = this@PBRMaterial.uvAnimation?.copy()
            // Copy optimization settings
            quality = this@PBRMaterial.quality
            optimizations = this@PBRMaterial.optimizations.copy()
    // Private helper methods
    private fun calculateIridescenceColor(thickness: Float, ior: Float): Color {
        // Simplified thin film interference calculation
        // Real implementation would use full spectral calculation
        val phase = (thickness * ior * 2 * PI.toFloat()) % (2 * PI.toFloat())
        val r = 0.5f + 0.5f * cos(phase)
        val g = 0.5f + 0.5f * cos(phase + 2 * PI.toFloat() / 3)
        val b = 0.5f + 0.5f * cos(phase + 4 * PI.toFloat() / 3)
        return Color(r, g, b, 1f)
    private fun applyUVTransform(uv: Vector2, transform: UVTransform): Vector2 {
        val result = uv.clone()
        // Apply offset
        result.add(transform.offset)
        // Apply scale
        result.multiply(transform.scale)
        // Apply rotation around center
        if (transform.rotation != 0f) {
            val center = Vector2(0.5f, 0.5f)
            result.subtract(center)
            val cos = cos(transform.rotation)
            val sin = sin(transform.rotation)
            val x = result.x * cos - result.y * sin
            val y = result.x * sin + result.y * cos
            result.set(x, y).add(center)
        return result
    private fun validateTextureCompatibility(issues: MutableList<String>) {
        // Check if texture formats and sizes are compatible
        // Implementation would validate actual texture properties
}
 * Base material class
abstract class Material(
    var name: String = "Material",
    var id: String = generateId(),
    var version: Int = 0
) {
    var visible: Boolean = true
    var needsUpdate: Boolean = false
    var userData: MutableMap<String, Any> = mutableMapOf()
    companion object {
        private fun generateId(): String {
            return "material-${currentTimeMillis()}-${(Math.random() * 1000000).toInt()}"
 * Clearcoat settings for automotive and glossy surfaces
@Serializable
data class ClearcoatSettings(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var roughness: Float = 0.1f,
    var ior: Float = 1.5f
)
 * Transmission settings for glass and transparent materials
data class TransmissionSettings(
    var factor: Float = 0f,
    var thickness: Float = 0f,
    var ior: Float = 1.5f,
    var attenuationDistance: Float = Float.POSITIVE_INFINITY,
    var attenuationColor: Color = Color.WHITE
 * Iridescence settings for thin film interference effects
data class IridescenceSettings(
    var thickness: Float = 400f, // nanometers
    var ior: Float = 1.3f
 * Subsurface scattering settings
data class SubsurfaceSettings(
    var color: Color = Color.WHITE,
    var distanceScale: Float = 1f,
    var thickness: Float = 1f
 * Anisotropic reflection settings
data class AnisotropySettings(
    var strength: Float = 0f,
    var direction: Vector2 = Vector2(1f, 0f),
    var aspectRatio: Float = 1f
 * Sheen settings for fabric materials
data class SheenSettings(
    var roughness: Float = 0f,
    var intensity: Float = 0f
 * UV transformation parameters
data class UVTransform(
    var offset: Vector2 = Vector2(),
    var scale: Vector2 = Vector2(1f, 1f),
    var rotation: Float = 0f
 * UV animation for moving textures
data class UVAnimation(
    var duration: Float = 1f,
    var loop: Boolean = true,
    var offsetAnimation: Vector2Animation? = null,
    var scaleAnimation: Vector2Animation? = null,
    var rotationAnimation: FloatAnimation? = null
    fun getTransformAtTime(time: Float): UVTransform {
        val normalizedTime = if (loop && duration > 0f) {
            (time % duration) / duration
        } else {
            (time / duration).coerceIn(0f, 1f)
        return UVTransform(
            offset = offsetAnimation?.getValueAtTime(normalizedTime) ?: Vector2(),
            scale = scaleAnimation?.getValueAtTime(normalizedTime) ?: Vector2(1f, 1f),
            rotation = rotationAnimation?.getValueAtTime(normalizedTime) ?: 0f
    fun copy(): UVAnimation {
        return UVAnimation(
            duration = duration,
            loop = loop,
            offsetAnimation = offsetAnimation?.copy(),
            scaleAnimation = scaleAnimation?.copy(),
            rotationAnimation = rotationAnimation?.copy()
 * Material quality levels for performance optimization
enum class MaterialQuality {
    LOW, MEDIUM, HIGH, ULTRA
 * Material optimization settings
data class MaterialOptimizations(
    var maxTextureSize: Int = 2048,
    var useCompression: Boolean = false,
    var mipmapGeneration: Boolean = true,
    var anisotropicFiltering: Boolean = true,
    var maxAnisotropy: Float = 16f
 * Alpha blending modes
enum class AlphaMode {
    OPAQUE, MASK, BLEND
 * Material face culling sides
enum class MaterialSide {
    FRONT, BACK, DOUBLE
// Contribution data classes for PBR features
data class ClearcoatContribution(
    val intensity: Float = 0f,
    val roughness: Float = 0f,
    val normal: Vector3 = Vector3(0f, 0f, 1f),
    val ior: Float = 1.5f
data class TransmissionContribution(
    val factor: Float = 0f,
    val thickness: Float = 0f,
    val ior: Float = 1.5f,
    val attenuationDistance: Float = Float.POSITIVE_INFINITY,
    val attenuationColor: Color = Color.WHITE
data class IridescenceContribution(
    val thickness: Float = 400f,
    val ior: Float = 1.3f,
    val color: Color = Color.WHITE
data class SubsurfaceContribution(
    val color: Color = Color.WHITE,
    val distanceScale: Float = 1f,
    val thickness: Float = 1f
data class AnisotropyContribution(
    val strength: Float = 0f,
    val direction: Vector2 = Vector2(1f, 0f),
    val aspectRatio: Float = 1f
data class SheenContribution(
    val intensity: Float = 0f
// Animation data classes
data class Vector2Animation(
    val keyframes: List<Vector2Keyframe>
    fun getValueAtTime(time: Float): Vector2 {
        if (keyframes.isEmpty()) return Vector2()
        if (keyframes.size == 1) return keyframes[0].data.clone()
        // Find surrounding keyframes
        var lowerIndex = 0
        var upperIndex = keyframes.size - 1
        for (i in keyframes.indices) {
            if (keyframes[i].time <= time) {
                lowerIndex = i
            } else {
                upperIndex = i
                break
        if (lowerIndex == upperIndex) {
            return keyframes[lowerIndex].data.clone()
        val lower = keyframes[lowerIndex]
        val upper = keyframes[upperIndex]
        val t = (time - lower.time) / (upper.time - lower.time)
        return (lower as? Result.Success<*>)?.data.clone().lerp((upper as? Result.Success<*>)?.data, t)
    fun copy(): Vector2Animation {
        return Vector2Animation(keyframes.map { it.copy() })
data class FloatAnimation(
    val keyframes: List<FloatKeyframe>
    fun getValueAtTime(time: Float): Float {
        if (keyframes.isEmpty()) return 0f
        if (keyframes.size == 1) return keyframes[0].data
            return keyframes[lowerIndex].data
        return (lower as? Result.Success<*>)?.data + ((upper as? Result.Success<*>)?.data - (lower as? Result.Success<*>)?.data) * t
    fun copy(): FloatAnimation {
        return FloatAnimation(keyframes.map { it.copy() })
data class Vector2Keyframe(
    val time: Float,
    val value: Vector2
    fun copy(): Vector2Keyframe {
        return Vector2Keyframe(time, value.clone())
data class FloatKeyframe(
    val value: Float
    fun copy(): FloatKeyframe {
        return FloatKeyframe(time, value)
// Placeholder classes for texture types
 * Base texture class
abstract class Texture {
    abstract fun sample(uv: Vector2): Color
 * Cube texture for environment mapping
abstract class CubeTexture : Texture()
 * 2D texture for standard texture mapping
abstract class Texture2D : Texture() {
    abstract fun sample2D(uv: Vector2): Color
 * 3D texture for volumetric effects
abstract class Texture3D : Texture() {
    abstract fun sample3D(uvw: Vector3): Color
 * Extension functions for Vector2 operations
fun Vector2.multiply(other: Vector2): Vector2 {
    x = x * other.x
    y = y * other.y
    return this
fun Vector2.subtract(other: Vector2): Vector2 {
    x = x - other.x
    y = y - other.y
fun Vector2.add(other: Vector2): Vector2 {
    x = x + other.x
    y = y + other.y
fun Vector2.lerp(target: Vector2, alpha: Float): Vector2 {
    x += (target.x - x) * alpha
    y += (target.y - y) * alpha
fun Vector2.clone(): Vector2 {
    return Vector2(x, y)
fun Vector2.set(x: Float, y: Float): Vector2 {
    this.x = x
    this.y = y
    return this
}
*/
