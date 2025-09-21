/**
 * ShaderMaterial with WGSL custom shader support
 * T027 - Advanced shader material with WGSL compilation and platform-specific optimization
 *
 * Features:
 * - WGSL shader source compilation
 * - Cross-platform shader compilation (WGSL to SPIR-V, GLSL, MSL)
 * - Dynamic uniform and attribute binding
 * - Shader hot-reloading for development
 * - Performance optimization and caching
 * - Built-in shader libraries and mixins
 * - Advanced debugging and profiling
 */
package io.kreekt.material
import io.kreekt.renderer.Texture
import io.kreekt.core.scene.Material
import io.kreekt.core.Result

import io.kreekt.core.math.*
import io.kreekt.core.platform.platformClone
import kotlinx.collections.immutable.*
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.math.PI

// Missing type definitions
enum class ShaderLanguage { WGSL, GLSL, HLSL, MSL }
enum class CompilationTarget { AUTO, WEB, JVM, NATIVE, MOBILE }

data class ShaderVariant(
    val name: String,
    val vertexShader: String = "",
    val fragmentShader: String = "",
    val computeShader: String = "",
    val defines: Map<String, String> = emptyMap()
)

data class ShaderUniform(
    val name: String,
    val type: UniformType,
    var value: Any? = null
)

data class ShaderAttribute(
    val name: String,
    val type: AttributeType,
    val location: Int = 0
)

data class TextureBinding(
    val texture: Texture,
    val sampler: String = "default",
    val binding: Int = 0
)

enum class UniformType { FLOAT, VEC2, VEC3, VEC4, MAT3, MAT4, INT, BOOL, TEXTURE_2D, TEXTURE_CUBE }
enum class AttributeType { FLOAT, VEC2, VEC3, VEC4, INT, UINT }

typealias ShaderDebugInfo = String
typealias ShaderProfiler = Any
typealias HotReloadHandler = () -> Unit
typealias ShaderCache = MutableMap<String, CompiledShader>
typealias ShaderMixin = String
typealias ShaderLibrary2 = Map<String, String>
typealias PreprocessorContext = Any
typealias PlatformShaderBackend = Any
typealias UniformBuffer = Any
typealias RenderPass = Any

/**
 * Custom shader material with WGSL support and cross-platform compilation
 * Provides low-level control over rendering pipeline with modern shader languages
class ShaderMaterial(
    var vertexShader: String = "",
    var fragmentShader: String = "",
    var computeShader: String = "",
    materialName: String = "ShaderMaterial"
) : Material {
    override val id: Int = generateId()
    override val name: String = materialName
    override var needsUpdate: Boolean = true

    private companion object {
        private var nextId = 0
        private fun generateId(): Int = ++nextId
    }
    // Shader compilation and variants
    var shaderLanguage: ShaderLanguage = ShaderLanguage.WGSL
    var compilationTarget: CompilationTarget = CompilationTarget.AUTO
    var shaderVariants: MutableMap<String, ShaderVariant> = mutableMapOf()
    var activeVariant: String = "default"
    // Uniforms and attributes
    private val _uniforms = mutableMapOf<String, ShaderUniform>()
    private val _attributes = mutableMapOf<String, ShaderAttribute>()
    private val _textures = mutableMapOf<String, TextureBinding>()
    private val _storageBuffers = mutableMapOf<String, StorageBuffer>()
    // Shader features and capabilities
    var features: MutableSet<ShaderFeature> = mutableSetOf()
    var defines: MutableMap<String, String> = mutableMapOf()
    var includes: MutableList<String> = mutableListOf()
    // Performance and debugging
    var enableProfiling: Boolean = false
    var debugMode: Boolean = false
    var hotReloadEnabled: Boolean = false
    var compilationCache: ShaderCache? = null
    // Advanced settings
    var blending: BlendState = BlendState.OPAQUE
    var depthTest: DepthTestState = DepthTestState.LESS
    var cullMode: CullMode = CullMode.BACK
    var primitiveTopology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST
    var wireframe: Boolean = false
    // Compute shader specific
    var workgroupSize: Vector3 = Vector3(1f, 1f, 1f)
    var dispatchSize: Vector3 = Vector3(1f, 1f, 1f)
    /**
     * Set uniform value with type safety
     */
    fun setUniform(name: String, value: Any) {
        val type = when (value) {
            is Float -> ShaderDataType.FLOAT
            is Int -> ShaderDataType.INT
            is Boolean -> ShaderDataType.BOOL
            is Vector2 -> ShaderDataType.VEC2
            is Vector3 -> ShaderDataType.VEC3
            is Vector4 -> ShaderDataType.VEC4
            is Matrix3 -> ShaderDataType.MAT3
            is Matrix4 -> ShaderDataType.MAT4
            is Color -> ShaderDataType.VEC4
            is FloatArray -> ShaderDataType.FLOAT_ARRAY
            else -> throw IllegalArgumentException("Unsupported uniform type: ${value::class}")
        }
        val uniform = ShaderUniform(
            name = name,
            type = type,
            value = value,
            location = -1 // Will be resolved during compilation
        )
        _uniforms[name] = uniform
        needsUpdate = true
    }
     * Get uniform value
    fun getUniform(name: String): Any? {
        return _uniforms[name]?.data
     * Set multiple uniforms from a map
    fun setUniforms(uniforms: Map<String, Any>) {
        uniforms.forEach { (name, value) ->
            when (value) {
                is Float -> setUniform(name, value)
                is Int -> setUniform(name, value)
                is Boolean -> setUniform(name, value)
                is Vector2 -> setUniform(name, value)
                is Vector3 -> setUniform(name, value)
                is Vector4 -> setUniform(name, value)
                is Matrix3 -> setUniform(name, value)
                is Matrix4 -> setUniform(name, value)
                is Color -> setUniform(name, value)
                is FloatArray -> setUniform(name, value)
                else -> throw IllegalArgumentException("Unsupported uniform type: ${value::class}")
            }
     * Define shader attribute binding
    fun setAttribute(name: String, location: Int, format: AttributeFormat) {
        _attributes[name] = ShaderAttribute(
            location = location,
            format = format,
            offset = 0,
            stride = format.getByteSize()
     * Bind texture to shader
    fun setTexture(name: String, texture: Texture, sampler: SamplerState = SamplerState.DEFAULT) {
        _textures[name] = TextureBinding(
            texture = texture,
            sampler = sampler,
            binding = -1 // Will be resolved during compilation
     * Set storage buffer for compute shaders
    fun setStorageBuffer(name: String, buffer: StorageBuffer) {
        _storageBuffers[name] = buffer
     * Add shader feature flag
    fun addFeature(feature: ShaderFeature) {
        features.add(feature)
     * Remove shader feature flag
    fun removeFeature(feature: ShaderFeature) {
        features.remove(feature)
     * Add preprocessor define
    fun addDefine(name: String, value: String = "1") {
        defines[name] = value
     * Remove preprocessor define
    fun removeDefine(name: String) {
        defines.remove(name)
     * Add shader include
    fun addInclude(includePath: String) {
        if (!includes.contains(includePath)) {
            includes.add(includePath)
            needsUpdate = true
     * Compile shader for target platform
    fun compile(target: CompilationTarget = compilationTarget): ShaderCompilationResult {
        val compiler = ShaderCompiler.getInstance()
        // Check compilation cache first
        val cacheKey = generateCacheKey(target)
        compilationCache?.let { cache ->
            cache.get(cacheKey)?.let { cachedResult ->
                return cachedResult
        // Prepare shader source with preprocessor
        val processedVertex = preprocessShaderSource(vertexShader, ShaderStage.VERTEX)
        val processedFragment = preprocessShaderSource(fragmentShader, ShaderStage.FRAGMENT)
        val processedCompute = if (computeShader.isNotEmpty()) {
            preprocessShaderSource(computeShader, ShaderStage.COMPUTE)
        } else null
        // Compile shaders
        val compilationRequest = ShaderCompilationRequest(
            vertexSource = processedVertex,
            fragmentSource = processedFragment,
            computeSource = processedCompute,
            sourceLanguage = shaderLanguage,
            targetLanguage = getTargetLanguageForPlatform(target),
            uniforms = _uniforms.values.toList(),
            attributes = _attributes.values.toList(),
            textureBindings = _textures.values.toList(),
            storageBuffers = _storageBuffers.values.toList(),
            features = features.toList(),
            defines = defines.toMap(),
            optimizationLevel = if (debugMode) OptimizationLevel.NONE else OptimizationLevel.HIGH
        val result = compiler.compile(compilationRequest)
        // Cache successful compilation
        if (result.success) {
            compilationCache?.put(cacheKey, result)
        return result
     * Create shader variant with different feature set
    fun createVariant(
        variantName: String,
        additionalFeatures: Set<ShaderFeature> = emptySet(),
        additionalDefines: Map<String, String> = emptyMap(),
        overrideShaders: ShaderOverrides? = null
    ): ShaderMaterial {
        val variant = clone()
        variant.features.addAll(additionalFeatures)
        variant.defines.putAll(additionalDefines)
        overrideShaders?.let { overrides ->
            overrides.vertexShader?.let { variant.vertexShader = it }
            overrides.fragmentShader?.let { variant.fragmentShader = it }
            overrides.computeShader?.let { variant.computeShader = it }
        shaderVariants[variantName] = ShaderVariant(
            name = variantName,
            material = variant,
            features = additionalFeatures,
            defines = additionalDefines
        return variant
     * Switch to a different shader variant
    fun useVariant(variantName: String): Boolean {
        return shaderVariants[variantName]?.let { variant ->
            activeVariant = variantName
            // Copy variant properties to this material
            features.clear()
            features.addAll(variant.material.features)
            defines.clear()
            defines.putAll(variant.material.defines)
            true
        } ?: false
     * Hot reload shaders during development
    fun hotReload(
        newVertexShader: String? = null,
        newFragmentShader: String? = null,
        newComputeShader: String? = null
    ): Boolean {
        if (!hotReloadEnabled) return false
        var changed = false
        newVertexShader?.let {
            if (it != vertexShader) {
                vertexShader = it
                changed = true
        newFragmentShader?.let {
            if (it != fragmentShader) {
                fragmentShader = it
        newComputeShader?.let {
            if (it != computeShader) {
                computeShader = it
        if (changed) {
            // Clear compilation cache
            compilationCache?.clear()
        return changed
     * Validate shader for compilation
    fun validate(): ShaderValidationResult {
        val issues = mutableListOf<String>()
        // Check if required shaders are present
        if (vertexShader.isEmpty() && fragmentShader.isEmpty() && computeShader.isEmpty()) {
            issues.add("At least one shader stage must be defined")
        // Validate shader syntax
        if (vertexShader.isNotEmpty()) {
            val vertexIssues = validateShaderSyntax(vertexShader, ShaderStage.VERTEX)
            issues.addAll(vertexIssues.map { "Vertex shader: $it" })
        if (fragmentShader.isNotEmpty()) {
            val fragmentIssues = validateShaderSyntax(fragmentShader, ShaderStage.FRAGMENT)
            issues.addAll(fragmentIssues.map { "Fragment shader: $it" })
        if (computeShader.isNotEmpty()) {
            val computeIssues = validateShaderSyntax(computeShader, ShaderStage.COMPUTE)
            issues.addAll(computeIssues.map { "Compute shader: $it" })
        // Validate uniform and attribute bindings
        validateBindings(issues)
        // Check feature compatibility
        validateFeatureCompatibility(issues)
        return ShaderValidationResult(
            isValid = issues.isEmpty(),
            issues = issues
     * Get performance metrics for compiled shader
    fun getPerformanceMetrics(target: CompilationTarget = compilationTarget): ShaderPerformanceMetrics {
        if (!enableProfiling) {
            return ShaderPerformanceMetrics()
        val compilationResult = compile(target)
        if (!compilationResult.success) {
        return ShaderPerformanceMetrics(
            compilationTime = compilationResult.compilationTime,
            vertexInstructions = compilationResult.vertexInstructions,
            fragmentInstructions = compilationResult.fragmentInstructions,
            uniformBufferSize = calculateUniformBufferSize(),
            textureUnits = _textures.size,
            estimatedGpuCost = estimateGpuCost(compilationResult)
     * Clone the shader material
    fun clone(): ShaderMaterial {
        return ShaderMaterial(
            vertexShader = vertexShader,
            fragmentShader = fragmentShader,
            computeShader = computeShader,
            name = name
        ).apply {
            // Copy shader properties
            shaderLanguage = this@ShaderMaterial.shaderLanguage
            compilationTarget = this@ShaderMaterial.compilationTarget
            activeVariant = this@ShaderMaterial.activeVariant
            // Copy uniforms, attributes, and textures
            _uniforms.putAll(this@ShaderMaterial._uniforms)
            _attributes.putAll(this@ShaderMaterial._attributes)
            _textures.putAll(this@ShaderMaterial._textures)
            _storageBuffers.putAll(this@ShaderMaterial._storageBuffers)
            // Copy features and settings
            features.addAll(this@ShaderMaterial.features)
            defines.putAll(this@ShaderMaterial.defines)
            includes.addAll(this@ShaderMaterial.includes)
            // Copy render state
            blending = this@ShaderMaterial.blending
            depthTest = this@ShaderMaterial.depthTest
            cullMode = this@ShaderMaterial.cullMode
            primitiveTopology = this@ShaderMaterial.primitiveTopology
            wireframe = this@ShaderMaterial.wireframe
            // Copy compute settings
            workgroupSize = this@ShaderMaterial.workgroupSize.clone()
            dispatchSize = this@ShaderMaterial.dispatchSize.clone()
            // Copy debug settings
            enableProfiling = this@ShaderMaterial.enableProfiling
            debugMode = this@ShaderMaterial.debugMode
            hotReloadEnabled = this@ShaderMaterial.hotReloadEnabled
    // Read-only accessors
    val uniforms: Map<String, ShaderUniform> get() = _uniforms.toMap()
    val attributes: Map<String, ShaderAttribute> get() = _attributes.toMap()
    val textures: Map<String, TextureBinding> get() = _textures.toMap()
    val storageBuffers: Map<String, StorageBuffer> get() = _storageBuffers.toMap()
    // Private helper methods
    private fun preprocessShaderSource(source: String, stage: ShaderStage): String {
        var processed = source
        // Add feature defines
        val featureDefines = features.joinToString("\n") { "#define ${it.featureName} 1" }
        processed = "$featureDefines\n$processed"
        // Add custom defines
        val customDefines = defines.entries.joinToString("\n") { "#define ${it.key} ${(it as? Result.Success<*>)?.data}" }
        processed = "$customDefines\n$processed"
        // Process includes
        includes.forEach { includePath ->
            val includeContent = loadIncludeContent(includePath)
            processed = processed.replace("#include \"$includePath\"", includeContent)
        // Add stage-specific defines
        processed = "#define STAGE_${stage.name} 1\n$processed"
        return processed
    private fun loadIncludeContent(includePath: String): String {
        // Implementation would load include content from file system or embedded resources
        return "// Include: $includePath\n"
    private fun generateCacheKey(target: CompilationTarget): String {
        val keyComponents = listOf(
            vertexShader.hashCode(),
            fragmentShader.hashCode(),
            computeShader.hashCode(),
            features.hashCode(),
            defines.hashCode(),
            target.hashCode()
        return keyComponents.joinToString("-")
    private fun getTargetLanguageForPlatform(target: CompilationTarget): ShaderLanguage {
        return when (target) {
            CompilationTarget.WEBGPU -> ShaderLanguage.WGSL
            CompilationTarget.VULKAN -> ShaderLanguage.SPIRV
            CompilationTarget.OPENGL -> ShaderLanguage.GLSL
            CompilationTarget.METAL -> ShaderLanguage.MSL
            CompilationTarget.DIRECT3D -> ShaderLanguage.HLSL
            CompilationTarget.AUTO -> detectPlatformShaderLanguage()
    private fun detectPlatformShaderLanguage(): ShaderLanguage {
        // Implementation would detect current platform capabilities
        return ShaderLanguage.WGSL // Default to WGSL
    private fun validateShaderSyntax(source: String, stage: ShaderStage): List<String> {
        // Basic syntax validation
        if (!source.contains("main")) {
            issues.add("Missing main function")
        // Stage-specific validation
        when (stage) {
            ShaderStage.VERTEX -> {
                if (shaderLanguage == ShaderLanguage.WGSL && !source.contains("@vertex")) {
                    issues.add("WGSL vertex shader missing @vertex attribute")
                }
            ShaderStage.FRAGMENT -> {
                if (shaderLanguage == ShaderLanguage.WGSL && !source.contains("@fragment")) {
                    issues.add("WGSL fragment shader missing @fragment attribute")
            ShaderStage.COMPUTE -> {
                if (shaderLanguage == ShaderLanguage.WGSL && !source.contains("@compute")) {
                    issues.add("WGSL compute shader missing @compute attribute")
            ShaderStage.GEOMETRY,
            ShaderStage.TESSELLATION_CONTROL,
            ShaderStage.TESSELLATION_EVALUATION -> {
                // Not commonly used in WGSL, skip validation
        return issues
    private fun validateBindings(issues: MutableList<String>) {
        // Check for binding conflicts
        val usedBindings = mutableSetOf<Int>()
        _textures.values.forEach { binding ->
            if (binding.binding >= 0) {
                if (usedBindings.contains(binding.binding)) {
                    issues.add("Binding conflict at binding ${binding.binding}")
                } else {
                    usedBindings.add(binding.binding)
        _storageBuffers.values.forEach { buffer ->
            if (buffer.binding >= 0) {
                if (usedBindings.contains(buffer.binding)) {
                    issues.add("Binding conflict at binding ${buffer.binding}")
                    usedBindings.add(buffer.binding)
    private fun validateFeatureCompatibility(issues: MutableList<String>) {
        // Check if features are compatible with each other and target platform
        if (features.contains(ShaderFeature.TESSELLATION) && computeShader.isNotEmpty()) {
            issues.add("Tessellation and compute shaders cannot be used together")
        if (features.contains(ShaderFeature.GEOMETRY_SHADER) && shaderLanguage == ShaderLanguage.WGSL) {
            issues.add("Geometry shaders are not supported in WGSL")
    private fun calculateUniformBufferSize(): Int {
        return _uniforms.values.sumOf { it.type.getByteSize() }
    private fun estimateGpuCost(compilationResult: ShaderCompilationResult): Float {
        // Simplified GPU cost estimation
        val vertexCost = compilationResult.vertexInstructions * 0.001f
        val fragmentCost = compilationResult.fragmentInstructions * 0.002f
        val textureCost = _textures.size * 0.1f
        return vertexCost + fragmentCost + textureCost
}
 * Shader language enumeration
enum class ShaderLanguage {
    WGSL, SPIRV, GLSL, HLSL, MSL
 * Compilation target platforms
enum class CompilationTarget {
    AUTO, WEBGPU, VULKAN, OPENGL, METAL, DIRECT3D
 * Shader stage enumeration
enum class ShaderStage {
    VERTEX, FRAGMENT, COMPUTE, GEOMETRY, TESSELLATION_CONTROL, TESSELLATION_EVALUATION
 * Shader data types
enum class ShaderDataType(val byteSize: Int) {
    FLOAT(4), INT(4), BOOL(4),
    VEC2(8), VEC3(12), VEC4(16),
    MAT3(36), MAT4(64),
    FLOAT_ARRAY(4), INT_ARRAY(4),
    TEXTURE_2D(0), TEXTURE_CUBE(0), SAMPLER(0);
    fun getByteSize(): Int = byteSize
 * Attribute format enumeration
enum class AttributeFormat(val components: Int, val componentSize: Int) {
    FLOAT(1, 4), VEC2(2, 4), VEC3(3, 4), VEC4(4, 4),
    INT(1, 4), IVEC2(2, 4), IVEC3(3, 4), IVEC4(4, 4),
    BYTE(1, 1), UBYTE(1, 1),
    SHORT(1, 2), USHORT(1, 2);
    fun getByteSize(): Int = (components * componentSize)
 * Shader features for conditional compilation
enum class ShaderFeature(val featureName: String) {
    INSTANCING("INSTANCING"),
    SKINNING("SKINNING"),
    MORPH_TARGETS("MORPH_TARGETS"),
    NORMAL_MAPPING("NORMAL_MAPPING"),
    PARALLAX_MAPPING("PARALLAX_MAPPING"),
    SHADOW_MAPPING("SHADOW_MAPPING"),
    ENVIRONMENT_MAPPING("ENVIRONMENT_MAPPING"),
    HDR("HDR"),
    TONE_MAPPING("TONE_MAPPING"),
    GAMMA_CORRECTION("GAMMA_CORRECTION"),
    FOG("FOG"),
    LIGHTING("LIGHTING"),
    PBR("PBR"),
    CLEARCOAT("CLEARCOAT"),
    TRANSMISSION("TRANSMISSION"),
    IRIDESCENCE("IRIDESCENCE"),
    SUBSURFACE_SCATTERING("SUBSURFACE_SCATTERING"),
    VERTEX_COLORS("VERTEX_COLORS"),
    TEXTURE_ATLAS("TEXTURE_ATLAS"),
    CLIPPING_PLANES("CLIPPING_PLANES"),
    TESSELLATION("TESSELLATION"),
    GEOMETRY_SHADER("GEOMETRY_SHADER"),
    MULTIVIEW("MULTIVIEW"),
    COMPUTE("COMPUTE");
    val featureNameValue: String get() = featureName
 * Blend state configuration
@Serializable
data class BlendState(
    val enabled: Boolean = false,
    val srcColorFactor: BlendFactor = BlendFactor.SRC_ALPHA,
    val dstColorFactor: BlendFactor = BlendFactor.ONE_MINUS_SRC_ALPHA,
    val colorOperation: BlendOperation = BlendOperation.ADD,
    val srcAlphaFactor: BlendFactor = BlendFactor.ONE,
    val dstAlphaFactor: BlendFactor = BlendFactor.ONE_MINUS_SRC_ALPHA,
    val alphaOperation: BlendOperation = BlendOperation.ADD
) {
    companion object {
        val OPAQUE = BlendState(enabled = false)
        val ALPHA_BLEND = BlendState(enabled = true)
        val ADDITIVE = BlendState(
            enabled = true,
            srcColorFactor = BlendFactor.ONE,
            dstColorFactor = BlendFactor.ONE
        val MULTIPLY = BlendState(
            srcColorFactor = BlendFactor.DST_COLOR,
            dstColorFactor = BlendFactor.ZERO
 * Depth test state configuration
data class DepthTestState(
    val enabled: Boolean = true,
    val writeEnabled: Boolean = true,
    val compareFunction: CompareFunction = CompareFunction.LESS
        val DISABLED = DepthTestState(enabled = false)
        val LESS = DepthTestState(enabled = true, compareFunction = CompareFunction.LESS)
        val LESS_EQUAL = DepthTestState(enabled = true, compareFunction = CompareFunction.LESS_EQUAL)
        val EQUAL = DepthTestState(enabled = true, compareFunction = CompareFunction.EQUAL)
        val ALWAYS = DepthTestState(enabled = true, compareFunction = CompareFunction.ALWAYS)
 * Blend factors
enum class BlendFactor {
    ZERO, ONE, SRC_COLOR, ONE_MINUS_SRC_COLOR, DST_COLOR, ONE_MINUS_DST_COLOR,
    SRC_ALPHA, ONE_MINUS_SRC_ALPHA, DST_ALPHA, ONE_MINUS_DST_ALPHA,
    CONSTANT_COLOR, ONE_MINUS_CONSTANT_COLOR, CONSTANT_ALPHA, ONE_MINUS_CONSTANT_ALPHA,
    SRC_ALPHA_SATURATE
 * Blend operations
enum class BlendOperation {
    ADD, SUBTRACT, REVERSE_SUBTRACT, MIN, MAX
 * Compare functions for depth testing
enum class CompareFunction {
    NEVER, LESS, EQUAL, LESS_EQUAL, GREATER, NOT_EQUAL, GREATER_EQUAL, ALWAYS
 * Face culling modes
enum class CullMode {
    NONE, FRONT, BACK
 * Primitive topology
enum class PrimitiveTopology {
    POINT_LIST, LINE_LIST, LINE_STRIP, TRIANGLE_LIST, TRIANGLE_STRIP
 * Optimization levels for shader compilation
enum class OptimizationLevel {
    NONE, LOW, MEDIUM, HIGH, AGGRESSIVE
 * Sampler state configuration
data class SamplerState(
    val magFilter: FilterMode = FilterMode.LINEAR,
    val minFilter: FilterMode = FilterMode.LINEAR,
    val mipmapFilter: FilterMode = FilterMode.LINEAR,
    val wrapU: WrapMode = WrapMode.REPEAT,
    val wrapV: WrapMode = WrapMode.REPEAT,
    val wrapW: WrapMode = WrapMode.REPEAT,
    val maxAnisotropy: Float = 1f,
    val compareFunction: CompareFunction? = null
        val DEFAULT = SamplerState()
        val NEAREST = SamplerState(
            magFilter = FilterMode.NEAREST,
            minFilter = FilterMode.NEAREST,
            mipmapFilter = FilterMode.NEAREST
        val CLAMP = SamplerState(
            wrapU = WrapMode.CLAMP_TO_EDGE,
            wrapV = WrapMode.CLAMP_TO_EDGE,
            wrapW = WrapMode.CLAMP_TO_EDGE
 * Texture filtering modes
enum class FilterMode {
    NEAREST, LINEAR
 * Texture wrap modes
enum class WrapMode {
    REPEAT, CLAMP_TO_EDGE, MIRRORED_REPEAT
// Data classes for shader system
 * Shader uniform descriptor
data class ShaderUniform(
    val name: String,
    val type: ShaderDataType,
    val value: Any,
    var location: Int = -1,
    var offset: Int = 0,
    var size: Int = type.getByteSize()
)
 * Shader attribute descriptor
data class ShaderAttribute(
    val location: Int,
    val format: AttributeFormat,
    val offset: Int = 0,
    val stride: Int = format.getByteSize()
 * Texture binding descriptor
data class TextureBinding(
    val texture: Texture,
    val sampler: SamplerState,
    var binding: Int = -1
 * Storage buffer for compute shaders
data class StorageBuffer(
    val data: ByteArray,
    val usage: BufferUsage = BufferUsage.STORAGE,
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StorageBuffer) return false
        return name == other.name && data.contentEquals((other as? Result.Success<*>)?.data) && usage == other.usage
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + usage.hashCode()
 * Buffer usage flags
enum class BufferUsage {
    VERTEX, INDEX, UNIFORM, STORAGE, COPY_SRC, COPY_DST
 * Shader variant definition
data class ShaderVariant(
    val material: ShaderMaterial,
    val features: Set<ShaderFeature>,
    val defines: Map<String, String>
 * Shader override for variants
data class ShaderOverrides(
    val vertexShader: String? = null,
    val fragmentShader: String? = null,
    val computeShader: String? = null
 * Shader compilation request
data class ShaderCompilationRequest(
    val vertexSource: String,
    val fragmentSource: String,
    val computeSource: String? = null,
    val sourceLanguage: ShaderLanguage,
    val targetLanguage: ShaderLanguage,
    val uniforms: List<ShaderUniform>,
    val attributes: List<ShaderAttribute>,
    val textureBindings: List<TextureBinding>,
    val storageBuffers: List<StorageBuffer>,
    val features: List<ShaderFeature>,
    val defines: Map<String, String>,
    val optimizationLevel: OptimizationLevel
 * Shader compilation result
data class ShaderCompilationResult(
    val success: Boolean,
    val vertexBytecode: ByteArray? = null,
    val fragmentBytecode: ByteArray? = null,
    val computeBytecode: ByteArray? = null,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val compilationTime: Float = 0f,
    val vertexInstructions: Int = 0,
    val fragmentInstructions: Int = 0,
    val computeInstructions: Int = 0
        if (other !is ShaderCompilationResult) return false
        return success == other.success &&
                vertexBytecode?.contentEquals(other.vertexBytecode) == true &&
                fragmentBytecode?.contentEquals(other.fragmentBytecode) == true &&
                computeBytecode?.contentEquals(other.computeBytecode) == true
        var result = success.hashCode()
        result = 31 * result + (vertexBytecode?.contentHashCode() ?: 0)
        result = 31 * result + (fragmentBytecode?.contentHashCode() ?: 0)
        result = 31 * result + (computeBytecode?.contentHashCode() ?: 0)
 * Shader validation result
data class ShaderValidationResult(
    val isValid: Boolean,
    val issues: List<String>
 * Shader performance metrics
data class ShaderPerformanceMetrics(
    val uniformBufferSize: Int = 0,
    val textureUnits: Int = 0,
    val estimatedGpuCost: Float = 0f
 * Shader compilation cache interface
interface ShaderCache {
    fun get(key: String): ShaderCompilationResult?
    fun put(key: String, result: ShaderCompilationResult)
    fun clear()
 * Shader compiler interface for platform-specific implementations
interface ShaderCompiler {
    fun compile(request: ShaderCompilationRequest): ShaderCompilationResult
        fun getInstance(): ShaderCompiler {
            // Platform-specific compiler would be returned here
            return DefaultShaderCompiler()
 * Default shader compiler implementation
class DefaultShaderCompiler : ShaderCompiler {
    override fun compile(request: ShaderCompilationRequest): ShaderCompilationResult {
        // Placeholder implementation - real compiler would be platform-specific
        return ShaderCompilationResult(
            success = true,
            compilationTime = 0.1f,
            vertexInstructions = 100,
            fragmentInstructions = 200
        )
    }
}
*/
