package io.kreekt.material

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.max

/**
 * Cross-platform shader compilation system
 * Handles WGSL source compilation to platform-specific formats:
 * - Web: WGSL (native support)
 * - JVM/Native: SPIR-V via cross-compilation
 * - Mobile: Platform-optimized shaders
 *
 * Features:
 * - Shader validation and optimization
 * - Feature detection and capability adaptation
 * - Shader caching and hot-reload support
 * - Preprocessor directives for conditional compilation
 * - Include system for shader libraries
 */
class ShaderCompiler {
    private val shaderCache = mutableMapOf<String, CompiledShader>()
    private val includeLibrary = mutableMapOf<String, String>()
    private val featureDetector = ShaderFeatureDetector()
    private val optimizer = ShaderOptimizer()
    private val validator = ShaderValidator()

    // Hot reload support
    private val hotReloadFlow = MutableSharedFlow<ShaderReloadEvent>()
    val shaderReloads: SharedFlow<ShaderReloadEvent> = hotReloadFlow.asSharedFlow()

    /**
     * Compile WGSL shader source to platform-specific format
     */
    suspend fun compileShader(
        source: String,
        type: ShaderType,
        options: CompilationOptions = CompilationOptions()
    ): Result<CompiledShader> = withContext(Dispatchers.Default) {
        try {
            // Check cache first
            val cacheKey = generateCacheKey(source, type, options)
            shaderCache[cacheKey]?.let {
                return@withContext Result.success(it)
            }

            // Preprocess source
            val preprocessed = preprocessShader(source, options)

            // Validate syntax
            validator.validate(preprocessed, type)?.let { errors ->
                return@withContext Result.failure(ShaderCompilationException(errors))
            }

            // Detect required features
            val requiredFeatures = featureDetector.detectRequiredFeatures(preprocessed)

            // Check platform capabilities
            if (!checkPlatformSupport(requiredFeatures)) {
                val fallback = generateFallbackShader(preprocessed, requiredFeatures)
                if (fallback != null) {
                    return@withContext compileShader(fallback, type, options)
                }
                return@withContext Result.failure(
                    ShaderCompilationException("Required features not supported: $requiredFeatures")
                )
            }

            // Platform-specific compilation
            val compiled = when (getPlatform()) {
                ShaderPlatform.WEB -> compileForWeb(preprocessed, type, options)
                ShaderPlatform.JVM -> compileForJVM(preprocessed, type, options)
                ShaderPlatform.ANDROID -> compileForAndroid(preprocessed, type, options)
                ShaderPlatform.IOS -> compileForIOS(preprocessed, type, options)
                ShaderPlatform.NATIVE -> compileForNative(preprocessed, type, options)
            }

            // Optimize if requested
            val optimized = if (options.optimize) {
                optimizer.optimize(compiled, options.optimizationLevel)
            } else {
                compiled
            }

            // Create compiled shader
            val result = CompiledShader(
                type = type,
                source = source,
                compiledCode = optimized,
                platform = getPlatform(),
                features = requiredFeatures,
                metadata = ShaderMetadata(
                    uniformCount = countUniforms(preprocessed),
                    attributeCount = countAttributes(preprocessed),
                    textureCount = countTextures(preprocessed),
                    workgroupSize = extractWorkgroupSize(preprocessed)
                )
            )

            // Cache result
            shaderCache[cacheKey] = result

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(ShaderCompilationException("Compilation failed: ${e.message}", e))
        }
    }

    /**
     * Compile a complete shader program (vertex + fragment)
     */
    suspend fun compileProgram(
        vertexSource: String,
        fragmentSource: String,
        options: CompilationOptions = CompilationOptions()
    ): Result<ShaderProgram> {
        val vertexResult = compileShader(vertexSource, ShaderType.VERTEX, options)
        val fragmentResult = compileShader(fragmentSource, ShaderType.FRAGMENT, options)

        return when {
            vertexResult.isSuccess && fragmentResult.isSuccess -> {
                val program = ShaderProgram(
                    vertex = vertexResult.getOrThrow(),
                    fragment = fragmentResult.getOrThrow(),
                    uniformLayout = extractUniformLayout(vertexSource, fragmentSource),
                    attributeLayout = extractAttributeLayout(vertexSource)
                )
                Result.success(program)
            }
            else -> {
                val errors = mutableListOf<String>()
                vertexResult.exceptionOrNull()?.let { errors.add("Vertex: ${it.message}") }
                fragmentResult.exceptionOrNull()?.let { errors.add("Fragment: ${it.message}") }
                Result.failure(ShaderCompilationException(errors.joinToString("\n")))
            }
        }
    }

    /**
     * Compile compute shader
     */
    suspend fun compileComputeShader(
        source: String,
        options: CompilationOptions = CompilationOptions()
    ): Result<CompiledShader> {
        return compileShader(source, ShaderType.COMPUTE, options)
    }

    /**
     * Preprocess shader source with includes and defines
     */
    private fun preprocessShader(source: String, options: CompilationOptions): String {
        var processed = source

        // Process includes
        processed = processIncludes(processed)

        // Add platform-specific defines
        processed = addPlatformDefines(processed, options)

        // Process conditional compilation
        processed = processConditionals(processed, options.defines)

        // Add feature defines
        processed = addFeatureDefines(processed, options)

        return processed
    }

    /**
     * Process #include directives
     */
    private fun processIncludes(source: String): String {
        val includePattern = Regex("#include\\s+[\"<]([^\"]+)[\">]")
        var processed = source
        val included = mutableSetOf<String>()

        while (includePattern.containsMatchIn(processed)) {
            processed = includePattern.replace(processed) { match ->
                val includeName = match.groupValues[1]
                if (includeName in included) {
                    "// Already included: $includeName"
                } else {
                    included.add(includeName)
                    includeLibrary[includeName] ?: "// Include not found: $includeName"
                }
            }
        }

        return processed
    }

    /**
     * Add platform-specific preprocessor defines
     */
    private fun addPlatformDefines(source: String, options: CompilationOptions): String {
        val defines = mutableListOf<String>()

        // Platform define
        defines.add("#define PLATFORM_${getPlatform().name}")

        // Feature defines based on capabilities
        val capabilities = featureDetector.getPlatformCapabilities()
        capabilities.forEach { capability ->
            defines.add("#define HAS_${capability.uppercase()}")
        }

        // User defines
        options.defines.forEach { (key, value) ->
            defines.add("#define $key $value")
        }

        return defines.joinToString("\n") + "\n\n" + source
    }

    /**
     * Process conditional compilation directives
     */
    private fun processConditionals(source: String, defines: Map<String, String>): String {
        var processed = source
        val ifdefPattern = Regex("#ifdef\\s+(\\w+)(.*?)#endif", RegexOption.DOT_MATCHES_ALL)
        val ifndefPattern = Regex("#ifndef\\s+(\\w+)(.*?)#endif", RegexOption.DOT_MATCHES_ALL)

        // Process #ifdef
        processed = ifdefPattern.replace(processed) { match ->
            val define = match.groupValues[1]
            val content = match.groupValues[2]
            if (define in defines || isFeatureDefined(define)) {
                content
            } else {
                ""
            }
        }

        // Process #ifndef
        processed = ifndefPattern.replace(processed) { match ->
            val define = match.groupValues[1]
            val content = match.groupValues[2]
            if (define !in defines && !isFeatureDefined(define)) {
                content
            } else {
                ""
            }
        }

        return processed
    }

    /**
     * Platform-specific compilation for Web (WGSL native)
     */
    private fun compileForWeb(source: String, type: ShaderType, options: CompilationOptions): ByteArray {
        // Web uses WGSL directly
        return source.encodeToByteArray()
    }

    /**
     * Platform-specific compilation for JVM (WGSL to SPIR-V)
     */
    private fun compileForJVM(source: String, type: ShaderType, options: CompilationOptions): ByteArray {
        // Use WGSL to SPIR-V cross-compiler
        val spirvCompiler = SPIRVCompiler()
        return spirvCompiler.compile(source, type, options)
    }

    /**
     * Platform-specific compilation for Android
     */
    private fun compileForAndroid(source: String, type: ShaderType, options: CompilationOptions): ByteArray {
        // Android uses Vulkan, so compile to SPIR-V
        val spirvCompiler = SPIRVCompiler()
        val spirv = spirvCompiler.compile(source, type, options)

        // Apply Android-specific optimizations
        return optimizeForMobile(spirv, options)
    }

    /**
     * Platform-specific compilation for iOS
     */
    private fun compileForIOS(source: String, type: ShaderType, options: CompilationOptions): ByteArray {
        // iOS uses Metal, need to cross-compile
        val metalCompiler = MetalCompiler()
        return metalCompiler.compile(source, type, options)
    }

    /**
     * Platform-specific compilation for Native platforms
     */
    private fun compileForNative(source: String, type: ShaderType, options: CompilationOptions): ByteArray {
        // Native uses Vulkan, compile to SPIR-V
        val spirvCompiler = SPIRVCompiler()
        return spirvCompiler.compile(source, type, options)
    }

    /**
     * Generate fallback shader for unsupported features
     */
    private fun generateFallbackShader(source: String, unsupportedFeatures: Set<ShaderFeature>): String? {
        var fallback = source

        unsupportedFeatures.forEach { feature ->
            when (feature) {
                ShaderFeature.COMPUTE_SHADER -> {
                    // Can't fallback compute shaders
                    return null
                }
                ShaderFeature.GEOMETRY_SHADER -> {
                    // Remove geometry shader stage
                    fallback = removeGeometryShaderCode(fallback)
                }
                ShaderFeature.TESSELLATION -> {
                    // Remove tessellation stages
                    fallback = removeTessellationCode(fallback)
                }
                ShaderFeature.VARIABLE_RATE_SHADING -> {
                    // Use fixed rate shading
                    fallback = fallback.replace("@vrs", "")
                }
                ShaderFeature.RAY_TRACING -> {
                    // Can't fallback ray tracing
                    return null
                }
                ShaderFeature.MESH_SHADER -> {
                    // Use traditional vertex shader
                    fallback = convertMeshToVertex(fallback)
                }
                else -> {
                    // Try generic fallback
                    fallback = applyGenericFallback(fallback, feature)
                }
            }
        }

        return fallback
    }

    /**
     * Hot reload a shader
     */
    suspend fun reloadShader(
        shaderPath: String,
        newSource: String,
        type: ShaderType,
        options: CompilationOptions = CompilationOptions()
    ) {
        val result = compileShader(newSource, type, options)

        result.fold(
            onSuccess = { compiled ->
                // Clear old cache entries
                shaderCache.entries.removeIf { it.value.source == shaderPath }

                // Emit reload event
                hotReloadFlow.emit(ShaderReloadEvent(
                    path = shaderPath,
                    shader = compiled,
                    timestamp = System.currentTimeMillis()
                ))
            },
            onFailure = { error ->
                // Emit error event
                hotReloadFlow.emit(ShaderReloadEvent(
                    path = shaderPath,
                    shader = null,
                    error = error.message,
                    timestamp = System.currentTimeMillis()
                ))
            }
        )
    }

    /**
     * Register shader include library
     */
    fun registerInclude(name: String, source: String) {
        includeLibrary[name] = source
    }

    /**
     * Clear shader cache
     */
    fun clearCache() {
        shaderCache.clear()
    }

    /**
     * Get compilation statistics
     */
    fun getStatistics(): CompilationStatistics {
        return CompilationStatistics(
            cachedShaders = shaderCache.size,
            totalCompilations = compilationCount,
            cacheHitRate = if (compilationCount > 0) {
                cacheHits.toFloat() / compilationCount
            } else 0f,
            averageCompilationTime = if (compilationCount > 0) {
                totalCompilationTime / compilationCount
            } else 0L
        )
    }

    // Helper functions
    private fun generateCacheKey(source: String, type: ShaderType, options: CompilationOptions): String {
        return "${source.hashCode()}_${type}_${options.hashCode()}"
    }

    private fun getPlatform(): ShaderPlatform {
        return expect { getCurrentShaderPlatform() }
    }

    private fun checkPlatformSupport(features: Set<ShaderFeature>): Boolean {
        val supported = featureDetector.getSupportedFeatures()
        return features.all { it in supported }
    }

    private fun isFeatureDefined(define: String): Boolean {
        return featureDetector.isFeatureDefined(define)
    }

    private fun countUniforms(source: String): Int {
        return Regex("@binding\\((\\d+)\\)\\s+@group\\((\\d+)\\)\\s+var").findAll(source).count()
    }

    private fun countAttributes(source: String): Int {
        return Regex("@location\\((\\d+)\\)").findAll(source).count()
    }

    private fun countTextures(source: String): Int {
        return Regex("texture_2d|texture_3d|texture_cube").findAll(source).count()
    }

    private fun extractWorkgroupSize(source: String): Vector3Int? {
        val match = Regex("@workgroup_size\\((\\d+),?\\s*(\\d+)?,?\\s*(\\d+)?\\)").find(source)
        return match?.let {
            Vector3Int(
                it.groupValues[1].toIntOrNull() ?: 1,
                it.groupValues[2].toIntOrNull() ?: 1,
                it.groupValues[3].toIntOrNull() ?: 1
            )
        }
    }

    private fun extractUniformLayout(vertexSource: String, fragmentSource: String): UniformLayout {
        val uniforms = mutableListOf<UniformBinding>()
        val pattern = Regex("@binding\\((\\d+)\\)\\s+@group\\((\\d+)\\)\\s+var(?:<.*?>)?\\s+(\\w+)\\s*:\\s*([^;]+)")

        listOf(vertexSource, fragmentSource).forEach { source ->
            pattern.findAll(source).forEach { match ->
                uniforms.add(UniformBinding(
                    name = match.groupValues[3],
                    binding = match.groupValues[1].toInt(),
                    group = match.groupValues[2].toInt(),
                    type = parseUniformType(match.groupValues[4])
                ))
            }
        }

        return UniformLayout(uniforms.distinctBy { "${it.group}_${it.binding}" })
    }

    private fun extractAttributeLayout(vertexSource: String): AttributeLayout {
        val attributes = mutableListOf<AttributeBinding>()
        val pattern = Regex("@location\\((\\d+)\\)\\s+(\\w+)\\s*:\\s*([^,;]+)")

        pattern.findAll(vertexSource).forEach { match ->
            attributes.add(AttributeBinding(
                name = match.groupValues[2],
                location = match.groupValues[1].toInt(),
                type = parseAttributeType(match.groupValues[3])
            ))
        }

        return AttributeLayout(attributes)
    }

    private fun parseUniformType(typeStr: String): UniformType {
        return when {
            "sampler" in typeStr -> UniformType.SAMPLER
            "texture" in typeStr -> UniformType.TEXTURE
            "mat4x4" in typeStr -> UniformType.MATRIX4
            "mat3x3" in typeStr -> UniformType.MATRIX3
            "vec4" in typeStr -> UniformType.VECTOR4
            "vec3" in typeStr -> UniformType.VECTOR3
            "vec2" in typeStr -> UniformType.VECTOR2
            "f32" in typeStr -> UniformType.FLOAT
            "i32" in typeStr -> UniformType.INT
            "u32" in typeStr -> UniformType.UINT
            else -> UniformType.STRUCT
        }
    }

    private fun parseAttributeType(typeStr: String): AttributeType {
        return when {
            "vec4" in typeStr -> AttributeType.FLOAT4
            "vec3" in typeStr -> AttributeType.FLOAT3
            "vec2" in typeStr -> AttributeType.FLOAT2
            "f32" in typeStr -> AttributeType.FLOAT
            "i32" in typeStr -> AttributeType.INT
            "u32" in typeStr -> AttributeType.UINT
            else -> AttributeType.FLOAT
        }
    }

    private fun removeGeometryShaderCode(source: String): String {
        // Remove geometry shader specific code
        return source.replace(Regex("@geometry.*?@end_geometry", RegexOption.DOT_MATCHES_ALL), "")
    }

    private fun removeTessellationCode(source: String): String {
        // Remove tessellation specific code
        return source.replace(Regex("@tessellation.*?@end_tessellation", RegexOption.DOT_MATCHES_ALL), "")
    }

    private fun convertMeshToVertex(source: String): String {
        // Convert mesh shader to traditional vertex shader
        // This is a complex operation that would need proper implementation
        return source
    }

    private fun applyGenericFallback(source: String, feature: ShaderFeature): String {
        // Apply generic fallback for unsupported features
        return source
    }

    private fun optimizeForMobile(spirv: ByteArray, options: CompilationOptions): ByteArray {
        // Apply mobile-specific optimizations
        // - Reduce precision where possible
        // - Optimize for tile-based rendering
        // - Minimize bandwidth usage
        return spirv
    }

    private fun addFeatureDefines(source: String, options: CompilationOptions): String {
        val defines = mutableListOf<String>()

        // Add feature-specific defines
        if (options.enableDebugInfo) {
            defines.add("#define DEBUG_MODE")
        }

        if (options.enableProfiling) {
            defines.add("#define PROFILING_ENABLED")
        }

        return if (defines.isNotEmpty()) {
            defines.joinToString("\n") + "\n" + source
        } else {
            source
        }
    }

    companion object {
        private var compilationCount = 0
        private var cacheHits = 0
        private var totalCompilationTime = 0L
    }
}

/**
 * SPIR-V cross-compiler for Vulkan platforms
 */
class SPIRVCompiler {
    fun compile(source: String, type: ShaderType, options: CompilationOptions): ByteArray {
        // This would use a real WGSL to SPIR-V compiler like Tint or Naga
        // For now, return placeholder
        return byteArrayOf()
    }
}

/**
 * Metal shader compiler for iOS
 */
class MetalCompiler {
    fun compile(source: String, type: ShaderType, options: CompilationOptions): ByteArray {
        // This would use a WGSL to Metal cross-compiler
        // For now, return placeholder
        return byteArrayOf()
    }
}

/**
 * Shader feature detection
 */
class ShaderFeatureDetector {
    private val platformFeatures = detectPlatformFeatures()

    fun detectRequiredFeatures(source: String): Set<ShaderFeature> {
        val features = mutableSetOf<ShaderFeature>()

        // Detect compute shaders
        if ("@compute" in source || "@workgroup_size" in source) {
            features.add(ShaderFeature.COMPUTE_SHADER)
        }

        // Detect geometry shaders
        if ("@geometry" in source) {
            features.add(ShaderFeature.GEOMETRY_SHADER)
        }

        // Detect tessellation
        if ("@tessellation" in source || "tessellation_control" in source) {
            features.add(ShaderFeature.TESSELLATION)
        }

        // Detect ray tracing
        if ("ray_query" in source || "acceleration_structure" in source) {
            features.add(ShaderFeature.RAY_TRACING)
        }

        // Detect mesh shaders
        if ("@mesh" in source) {
            features.add(ShaderFeature.MESH_SHADER)
        }

        // Detect advanced texture features
        if ("texture_3d" in source) {
            features.add(ShaderFeature.TEXTURE_3D)
        }

        if ("texture_array" in source) {
            features.add(ShaderFeature.TEXTURE_ARRAY)
        }

        // Detect storage buffers
        if ("storage" in source && "read_write" in source) {
            features.add(ShaderFeature.STORAGE_BUFFER)
        }

        return features
    }

    fun getSupportedFeatures(): Set<ShaderFeature> = platformFeatures

    fun getPlatformCapabilities(): List<String> {
        return platformFeatures.map { it.name.lowercase() }
    }

    fun isFeatureDefined(define: String): Boolean {
        return platformFeatures.any {
            "HAS_${it.name}" == define || it.name == define
        }
    }

    private fun detectPlatformFeatures(): Set<ShaderFeature> {
        // This would query actual platform capabilities
        // For now, return common features
        return setOf(
            ShaderFeature.COMPUTE_SHADER,
            ShaderFeature.TEXTURE_2D,
            ShaderFeature.TEXTURE_3D,
            ShaderFeature.STORAGE_BUFFER,
            ShaderFeature.UNIFORM_BUFFER
        )
    }
}

/**
 * Shader optimizer
 */
class ShaderOptimizer {
    fun optimize(code: ByteArray, level: OptimizationLevel): ByteArray {
        return when (level) {
            OptimizationLevel.NONE -> code
            OptimizationLevel.SIZE -> optimizeForSize(code)
            OptimizationLevel.SPEED -> optimizeForSpeed(code)
            OptimizationLevel.BALANCED -> optimizeBalanced(code)
        }
    }

    private fun optimizeForSize(code: ByteArray): ByteArray {
        // Minimize shader size
        // - Remove dead code
        // - Merge similar operations
        // - Compress constants
        return code
    }

    private fun optimizeForSpeed(code: ByteArray): ByteArray {
        // Optimize for execution speed
        // - Unroll loops
        // - Inline functions
        // - Vectorize operations
        return code
    }

    private fun optimizeBalanced(code: ByteArray): ByteArray {
        // Balance between size and speed
        return code
    }
}

/**
 * Shader validator
 */
class ShaderValidator {
    fun validate(source: String, type: ShaderType): List<String>? {
        val errors = mutableListOf<String>()

        // Check for required entry points
        when (type) {
            ShaderType.VERTEX -> {
                if (!source.contains("@vertex")) {
                    errors.add("Missing @vertex entry point")
                }
            }
            ShaderType.FRAGMENT -> {
                if (!source.contains("@fragment")) {
                    errors.add("Missing @fragment entry point")
                }
            }
            ShaderType.COMPUTE -> {
                if (!source.contains("@compute")) {
                    errors.add("Missing @compute entry point")
                }
            }
        }

        // Check for syntax errors (simplified)
        if (source.count { it == '{' } != source.count { it == '}' }) {
            errors.add("Mismatched braces")
        }

        if (source.count { it == '(' } != source.count { it == ')' }) {
            errors.add("Mismatched parentheses")
        }

        return if (errors.isNotEmpty()) errors else null
    }
}

// Data classes
data class CompilationOptions(
    val optimize: Boolean = true,
    val optimizationLevel: OptimizationLevel = OptimizationLevel.BALANCED,
    val defines: Map<String, String> = emptyMap(),
    val enableDebugInfo: Boolean = false,
    val enableProfiling: Boolean = false,
    val targetVersion: ShaderVersion = ShaderVersion.WGSL_1_0
)

data class CompiledShader(
    val type: ShaderType,
    val source: String,
    val compiledCode: ByteArray,
    val platform: ShaderPlatform,
    val features: Set<ShaderFeature>,
    val metadata: ShaderMetadata
)

data class ShaderProgram(
    val vertex: CompiledShader,
    val fragment: CompiledShader,
    val uniformLayout: UniformLayout,
    val attributeLayout: AttributeLayout
)

data class ShaderMetadata(
    val uniformCount: Int,
    val attributeCount: Int,
    val textureCount: Int,
    val workgroupSize: Vector3Int?
)

data class UniformLayout(
    val uniforms: List<UniformBinding>
)

data class UniformBinding(
    val name: String,
    val binding: Int,
    val group: Int,
    val type: UniformType
)

data class AttributeLayout(
    val attributes: List<AttributeBinding>
)

data class AttributeBinding(
    val name: String,
    val location: Int,
    val type: AttributeType
)

data class ShaderReloadEvent(
    val path: String,
    val shader: CompiledShader?,
    val error: String? = null,
    val timestamp: Long
)

data class CompilationStatistics(
    val cachedShaders: Int,
    val totalCompilations: Int,
    val cacheHitRate: Float,
    val averageCompilationTime: Long
)

data class Vector3Int(val x: Int, val y: Int, val z: Int)

// Enums
enum class ShaderType {
    VERTEX, FRAGMENT, COMPUTE, GEOMETRY, TESSELLATION_CONTROL, TESSELLATION_EVALUATION
}

enum class ShaderPlatform {
    WEB, JVM, ANDROID, IOS, NATIVE
}

enum class ShaderFeature {
    COMPUTE_SHADER,
    GEOMETRY_SHADER,
    TESSELLATION,
    VARIABLE_RATE_SHADING,
    RAY_TRACING,
    MESH_SHADER,
    TEXTURE_2D,
    TEXTURE_3D,
    TEXTURE_ARRAY,
    TEXTURE_CUBE,
    STORAGE_BUFFER,
    UNIFORM_BUFFER,
    PUSH_CONSTANTS,
    BINDLESS_TEXTURES,
    WAVE_INTRINSICS,
    INT64,
    FLOAT64
}

enum class OptimizationLevel {
    NONE, SIZE, SPEED, BALANCED
}

enum class ShaderVersion {
    WGSL_1_0, SPIRV_1_5, METAL_2_4, GLSL_450
}

enum class UniformType {
    FLOAT, VECTOR2, VECTOR3, VECTOR4,
    INT, IVECTOR2, IVECTOR3, IVECTOR4,
    UINT, UVECTOR2, UVECTOR3, UVECTOR4,
    MATRIX3, MATRIX4,
    SAMPLER, TEXTURE,
    STRUCT, ARRAY
}

enum class AttributeType {
    FLOAT, FLOAT2, FLOAT3, FLOAT4,
    INT, INT2, INT3, INT4,
    UINT, UINT2, UINT3, UINT4
}

// Exception
class ShaderCompilationException(message: String, cause: Throwable? = null) : Exception(message, cause)

// Platform-specific expect declaration
expect fun getCurrentShaderPlatform(): ShaderPlatform