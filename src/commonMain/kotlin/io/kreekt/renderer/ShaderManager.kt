package io.kreekt.renderer

import io.kreekt.core.scene.Material
import io.kreekt.material.MeshStandardMaterial

/**
 * Advanced shader management system
 * T117 - Shader compilation, caching, and program management
 */

/**
 * Shader types
 */
enum class ShaderType {
    VERTEX,
    FRAGMENT,
    COMPUTE,
    GEOMETRY,
    TESSELLATION_CONTROL,
    TESSELLATION_EVALUATION
}

/**
 * Shader source representation
 */
data class ShaderSource(
    val type: ShaderType,
    val source: String,
    val language: ShaderLanguage = ShaderLanguage.WGSL,
    val entryPoint: String = "main",
    val defines: Map<String, String> = emptyMap()
) {
    /**
     * Get the processed source with defines applied
     */
    fun getProcessedSource(): String {
        var processed = source
        for ((key, value) in defines) {
            processed = processed.replace("#define $key", "#define $key $value")
        }
        return processed
    }

    /**
     * Create a variant with additional defines
     */
    fun withDefines(additionalDefines: Map<String, String>): ShaderSource {
        return copy(defines = defines + additionalDefines)
    }
}

/**
 * Shader languages supported
 */
enum class ShaderLanguage {
    WGSL,    // WebGPU Shading Language
    GLSL,    // OpenGL Shading Language
    HLSL,    // High Level Shading Language
    SPIRV    // SPIR-V bytecode
}

/**
 * Compiled shader representation
 */
interface CompiledShader {
    val id: Int
    val type: ShaderType
    val source: ShaderSource
    val isValid: Boolean
    val compilationLog: String?

    fun dispose()
}

/**
 * Shader program that combines multiple shaders
 */
interface ShaderProgram {
    val id: Int
    val vertexShader: CompiledShader
    val fragmentShader: CompiledShader
    val geometryShader: CompiledShader?
    val computeShader: CompiledShader?
    val isLinked: Boolean
    val linkLog: String?

    fun use(): RendererResult<Unit>
    fun setUniform(name: String, value: Any): RendererResult<Unit>
    fun dispose()
}

/**
 * Shader compilation result
 */
sealed class ShaderCompilationResult {
    data class Success(val shader: CompiledShader) : ShaderCompilationResult()
    data class Error(val message: String, val log: String? = null) : ShaderCompilationResult()
}

/**
 * Shader program link result
 */
sealed class ShaderLinkResult {
    data class Success(val program: ShaderProgram) : ShaderLinkResult()
    data class Error(val message: String, val log: String? = null) : ShaderLinkResult()
}

/**
 * Shader manager interface
 */
interface ShaderManager {
    /**
     * Compile a shader from source
     */
    fun compileShader(source: ShaderSource): ShaderCompilationResult

    /**
     * Link shaders into a program
     */
    fun linkProgram(shaders: List<CompiledShader>): ShaderLinkResult

    /**
     * Get or create a shader program with caching
     */
    fun getProgram(vertexSource: ShaderSource, fragmentSource: ShaderSource): ShaderProgram?

    /**
     * Create a compute shader program
     */
    fun getComputeProgram(computeSource: ShaderSource): ShaderProgram?

    /**
     * Clear shader cache
     */
    fun clearCache()

    /**
     * Get compilation statistics
     */
    fun getStats(): ShaderStats

    /**
     * Dispose of all resources
     */
    fun dispose()
}

/**
 * Shader compilation and usage statistics
 */
data class ShaderStats(
    val totalCompilations: Int = 0,
    val successfulCompilations: Int = 0,
    val failedCompilations: Int = 0,
    val cacheHits: Int = 0,
    val cacheMisses: Int = 0,
    val totalPrograms: Int = 0,
    val activePrograms: Int = 0,
    val compilationTimeMs: Long = 0
)

/**
 * Default shader manager implementation
 */
class DefaultShaderManager(
    private val capabilities: RendererCapabilities
) : ShaderManager {

    private val shaderCache = mutableMapOf<String, CompiledShader>()
    private val programCache = mutableMapOf<String, ShaderProgram>()
    private val stats = ShaderStatsTracker()

    override fun compileShader(source: ShaderSource): ShaderCompilationResult {
        val startTime = 0L // System.currentTimeMillis() not available in multiplatform

        try {
            // Check cache first
            val cacheKey = generateShaderCacheKey(source)
            shaderCache[cacheKey]?.let { cached ->
                stats.cacheHit()
                return ShaderCompilationResult.Success(cached)
            }

            stats.cacheMiss()

            // Validate shader type support
            if (!isShaderTypeSupported(source.type)) {
                stats.compilationFailed()
                return ShaderCompilationResult.Error("Shader type ${source.type} not supported")
            }

            // Compile shader
            val compiledShader = compileShaderImpl(source)

            // Cache the result
            if (compiledShader.isValid) {
                shaderCache[cacheKey] = compiledShader
                stats.compilationSucceeded()
            } else {
                stats.compilationFailed()
                return ShaderCompilationResult.Error(
                    "Shader compilation failed",
                    compiledShader.compilationLog
                )
            }

            stats.recordCompilationTime(0L) // System.currentTimeMillis() not available in multiplatform
            return ShaderCompilationResult.Success(compiledShader)

        } catch (e: Exception) {
            stats.compilationFailed()
            return ShaderCompilationResult.Error("Compilation exception: ${e.message}")
        }
    }

    override fun linkProgram(shaders: List<CompiledShader>): ShaderLinkResult {
        try {
            // Validate shader combination
            val vertexShader = shaders.firstOrNull { it.type == ShaderType.VERTEX }
            val fragmentShader = shaders.firstOrNull { it.type == ShaderType.FRAGMENT }
            val computeShader = shaders.firstOrNull { it.type == ShaderType.COMPUTE }

            if (computeShader != null) {
                // Compute shader program
                if (shaders.size != 1) {
                    return ShaderLinkResult.Error("Compute shader must be standalone")
                }
            } else {
                // Graphics shader program
                if (vertexShader == null || fragmentShader == null) {
                    return ShaderLinkResult.Error("Graphics program requires vertex and fragment shaders")
                }
            }

            // Link program
            val program = linkProgramImpl(shaders)

            if (program.isLinked) {
                stats.programCreated()
                return ShaderLinkResult.Success(program)
            } else {
                return ShaderLinkResult.Error("Program linking failed", program.linkLog)
            }

        } catch (e: Exception) {
            return ShaderLinkResult.Error("Linking exception: ${e.message}")
        }
    }

    override fun getProgram(vertexSource: ShaderSource, fragmentSource: ShaderSource): ShaderProgram? {
        val cacheKey = generateProgramCacheKey(vertexSource, fragmentSource)

        programCache[cacheKey]?.let { cached ->
            stats.cacheHit()
            return cached
        }

        stats.cacheMiss()

        // Compile shaders
        val vertexResult = compileShader(vertexSource)
        val fragmentResult = compileShader(fragmentSource)

        if (vertexResult is ShaderCompilationResult.Success &&
            fragmentResult is ShaderCompilationResult.Success
        ) {

            // Link program
            val linkResult = linkProgram(listOf(vertexResult.shader, fragmentResult.shader))

            if (linkResult is ShaderLinkResult.Success) {
                programCache[cacheKey] = linkResult.program
                return linkResult.program
            }
        }

        return null
    }

    override fun getComputeProgram(computeSource: ShaderSource): ShaderProgram? {
        val cacheKey = generateComputeCacheKey(computeSource)

        programCache[cacheKey]?.let { cached ->
            stats.cacheHit()
            return cached
        }

        stats.cacheMiss()

        // Compile compute shader
        val computeResult = compileShader(computeSource)

        if (computeResult is ShaderCompilationResult.Success) {
            // Link program
            val linkResult = linkProgram(listOf(computeResult.shader))

            if (linkResult is ShaderLinkResult.Success) {
                programCache[cacheKey] = linkResult.program
                return linkResult.program
            }
        }

        return null
    }

    override fun clearCache() {
        shaderCache.values.forEach { it.dispose() }
        programCache.values.forEach { it.dispose() }
        shaderCache.clear()
        programCache.clear()
        stats.reset()
    }

    override fun getStats(): ShaderStats = stats.getStats()

    override fun dispose() {
        clearCache()
    }

    // Private implementation methods
    private fun isShaderTypeSupported(type: ShaderType): Boolean {
        return when (type) {
            ShaderType.VERTEX, ShaderType.FRAGMENT -> true
            ShaderType.COMPUTE -> capabilities.computeShaders
            ShaderType.GEOMETRY -> capabilities.geometryShaders
            ShaderType.TESSELLATION_CONTROL, ShaderType.TESSELLATION_EVALUATION -> capabilities.tessellation
        }
    }

    private fun compileShaderImpl(source: ShaderSource): CompiledShader {
        try {
            // Validate shader source structure
            val validationIssues = ShaderUtils.validateShaderSource(source)
            if (validationIssues.isNotEmpty()) {
                return DefaultCompiledShader(
                    id = generateShaderId(),
                    type = source.type,
                    source = source,
                    isValid = false,
                    compilationLog = "Validation failed: ${validationIssues.joinToString("; ")}"
                )
            }

            // Get processed source with defines applied
            val processedSource = source.getProcessedSource()

            // Validate shader language support
            if (!isShaderLanguageSupported(source.language)) {
                return DefaultCompiledShader(
                    id = generateShaderId(),
                    type = source.type,
                    source = source,
                    isValid = false,
                    compilationLog = "Shader language ${source.language} not supported"
                )
            }

            // Perform syntax validation based on shader language
            val syntaxErrors = validateShaderSyntax(processedSource, source.language, source.type)
            if (syntaxErrors.isNotEmpty()) {
                return DefaultCompiledShader(
                    id = generateShaderId(),
                    type = source.type,
                    source = source,
                    isValid = false,
                    compilationLog = "Syntax errors: ${syntaxErrors.joinToString("; ")}"
                )
            }

            // Validate shader type specific requirements
            val typeErrors = validateShaderTypeRequirements(processedSource, source.type, source.language)
            if (typeErrors.isNotEmpty()) {
                return DefaultCompiledShader(
                    id = generateShaderId(),
                    type = source.type,
                    source = source,
                    isValid = false,
                    compilationLog = "Type validation failed: ${typeErrors.joinToString("; ")}"
                )
            }

            // Platform-specific compilation would happen here
            // For now, we'll perform basic validation and assume success
            val compilationResult = performPlatformSpecificCompilation(processedSource, source)

            return DefaultCompiledShader(
                id = generateShaderId(),
                type = source.type,
                source = source,
                isValid = compilationResult.success,
                compilationLog = compilationResult.log
            )

        } catch (e: Exception) {
            return DefaultCompiledShader(
                id = generateShaderId(),
                type = source.type,
                source = source,
                isValid = false,
                compilationLog = "Compilation exception: ${e.message}"
            )
        }
    }

    private fun linkProgramImpl(shaders: List<CompiledShader>): ShaderProgram {
        try {
            // Validate all shaders are valid
            val invalidShaders = shaders.filter { !it.isValid }
            if (invalidShaders.isNotEmpty()) {
                val programId = generateProgramId()
                val firstValidShader = shaders.firstOrNull() ?: shaders.first()
                return DefaultShaderProgram(
                    id = programId,
                    vertexShader = firstValidShader,
                    fragmentShader = firstValidShader,
                    geometryShader = null,
                    computeShader = null,
                    isLinked = false,
                    linkLog = "Cannot link program with invalid shaders: ${
                        invalidShaders.map { "${it.type}(${it.id})" }.joinToString(", ")
                    }",
                    stats = stats
                )
            }

            val vertexShader = shaders.firstOrNull { it.type == ShaderType.VERTEX }
            val fragmentShader = shaders.firstOrNull { it.type == ShaderType.FRAGMENT }
            val geometryShader = shaders.firstOrNull { it.type == ShaderType.GEOMETRY }
            val computeShader = shaders.firstOrNull { it.type == ShaderType.COMPUTE }

            // Validate shader combination
            val linkingErrors = mutableListOf<String>()

            if (computeShader != null) {
                // Compute shader must be standalone
                if (shaders.size > 1) {
                    linkingErrors.add("Compute shader must be standalone, found ${shaders.size} shaders")
                }
            } else {
                // Graphics pipeline requires vertex and fragment shaders
                if (vertexShader == null) {
                    linkingErrors.add("Graphics pipeline requires vertex shader")
                }
                if (fragmentShader == null) {
                    linkingErrors.add("Graphics pipeline requires fragment shader")
                }
            }

            // Validate attribute/uniform compatibility
            if (vertexShader != null && fragmentShader != null) {
                val compatibilityErrors = validateShaderCompatibility(vertexShader, fragmentShader)
                linkingErrors.addAll(compatibilityErrors)
            }

            // Validate geometry shader compatibility
            if (geometryShader != null) {
                if (vertexShader == null) {
                    linkingErrors.add("Geometry shader requires vertex shader")
                }
                val geometryErrors = validateGeometryShaderCompatibility(vertexShader, geometryShader)
                linkingErrors.addAll(geometryErrors)
            }

            // Check if linking would succeed
            val linkingSuccess = linkingErrors.isEmpty()
            val linkLog = if (linkingErrors.isNotEmpty()) {
                "Linking failed: ${linkingErrors.joinToString("; ")}"
            } else {
                null
            }

            // Perform platform-specific linking
            val platformLinkResult = if (linkingSuccess) {
                performPlatformSpecificLinking(shaders)
            } else {
                PlatformLinkResult(false, linkLog)
            }

            val programId = generateProgramId()
            return DefaultShaderProgram(
                id = programId,
                vertexShader = vertexShader ?: computeShader ?: shaders.first(),
                fragmentShader = fragmentShader ?: computeShader ?: shaders.first(),
                geometryShader = geometryShader,
                computeShader = computeShader,
                isLinked = platformLinkResult.success,
                linkLog = platformLinkResult.log,
                stats = stats
            )

        } catch (e: Exception) {
            val programId = generateProgramId()
            val firstShader = shaders.firstOrNull() ?: shaders.first()
            return DefaultShaderProgram(
                id = programId,
                vertexShader = firstShader,
                fragmentShader = firstShader,
                geometryShader = null,
                computeShader = null,
                isLinked = false,
                linkLog = "Linking exception: ${e.message}",
                stats = stats
            )
        }
    }

    private fun generateShaderCacheKey(source: ShaderSource): String {
        return "${source.type}-${source.language}-${source.source.hashCode()}-${source.defines.hashCode()}"
    }

    private fun generateProgramCacheKey(vertexSource: ShaderSource, fragmentSource: ShaderSource): String {
        return "${generateShaderCacheKey(vertexSource)}-${generateShaderCacheKey(fragmentSource)}"
    }

    private fun generateComputeCacheKey(computeSource: ShaderSource): String {
        return "compute-${generateShaderCacheKey(computeSource)}"
    }

    private var nextShaderId = 1
    private fun generateShaderId(): Int = nextShaderId++

    private var nextProgramId = 1
    private fun generateProgramId(): Int = nextProgramId++

    // Helper methods for shader compilation and linking

    /**
     * Check if a shader language is supported
     */
    private fun isShaderLanguageSupported(language: ShaderLanguage): Boolean {
        return when (language) {
            ShaderLanguage.WGSL -> true // Primary target
            ShaderLanguage.GLSL -> capabilities.extensions.contains("GLSL")
            ShaderLanguage.HLSL -> capabilities.extensions.contains("HLSL")
            ShaderLanguage.SPIRV -> capabilities.extensions.contains("SPIRV")
        }
    }

    /**
     * Validate shader syntax based on language
     */
    private fun validateShaderSyntax(source: String, language: ShaderLanguage, type: ShaderType): List<String> {
        val errors = mutableListOf<String>()

        when (language) {
            ShaderLanguage.WGSL -> {
                // WGSL-specific syntax validation
                if (!source.contains("fn ${getExpectedEntryPoint(type)}(")) {
                    errors.add("Missing entry point function '${getExpectedEntryPoint(type)}'")
                }

                // Check for required stage annotations
                when (type) {
                    ShaderType.VERTEX -> if (!source.contains("@vertex")) errors.add("Missing @vertex annotation")
                    ShaderType.FRAGMENT -> if (!source.contains("@fragment")) errors.add("Missing @fragment annotation")
                    ShaderType.COMPUTE -> if (!source.contains("@compute")) errors.add("Missing @compute annotation")
                    else -> {} // Other types may not require annotations
                }

                // Basic bracket matching
                val openBraces = source.count { it == '{' }
                val closeBraces = source.count { it == '}' }
                if (openBraces != closeBraces) {
                    errors.add("Mismatched braces: $openBraces open, $closeBraces close")
                }
            }

            ShaderLanguage.GLSL -> {
                // GLSL-specific validation
                if (!source.contains("#version")) {
                    errors.add("Missing #version directive")
                }
                if (!source.contains("void main(")) {
                    errors.add("Missing main() function")
                }
            }

            ShaderLanguage.HLSL -> {
                // HLSL-specific validation
                val entryPoint = getExpectedEntryPoint(type)
                if (!source.contains("$entryPoint(") && !source.contains("void $entryPoint(")) {
                    errors.add("Missing entry point function '$entryPoint'")
                }
            }

            ShaderLanguage.SPIRV -> {
                // SPIR-V is binary, basic validation only
                if (source.isBlank()) {
                    errors.add("SPIR-V binary data is empty")
                }
            }
        }

        return errors
    }

    /**
     * Validate shader type specific requirements
     */
    private fun validateShaderTypeRequirements(
        source: String,
        type: ShaderType,
        language: ShaderLanguage
    ): List<String> {
        val errors = mutableListOf<String>()

        when (type) {
            ShaderType.VERTEX -> {
                if (language == ShaderLanguage.WGSL && !source.contains("@builtin(position)")) {
                    errors.add("Vertex shader must output position via @builtin(position)")
                }
            }

            ShaderType.FRAGMENT -> {
                if (language == ShaderLanguage.WGSL && !source.contains("@location(0)") && !source.contains("-> @location(0)")) {
                    errors.add("Fragment shader must output to @location(0)")
                }
            }

            ShaderType.COMPUTE -> {
                if (language == ShaderLanguage.WGSL && !source.contains("@workgroup_size(")) {
                    errors.add("Compute shader must specify @workgroup_size")
                }
            }

            ShaderType.GEOMETRY -> {
                if (!capabilities.geometryShaders) {
                    errors.add("Geometry shaders not supported on this platform")
                }
            }

            ShaderType.TESSELLATION_CONTROL, ShaderType.TESSELLATION_EVALUATION -> {
                if (!capabilities.tessellation) {
                    errors.add("Tessellation shaders not supported on this platform")
                }
            }
        }

        return errors
    }

    /**
     * Get expected entry point name for shader type
     */
    private fun getExpectedEntryPoint(type: ShaderType): String {
        return when (type) {
            ShaderType.VERTEX -> "vs_main"
            ShaderType.FRAGMENT -> "fs_main"
            ShaderType.COMPUTE -> "cs_main"
            ShaderType.GEOMETRY -> "gs_main"
            ShaderType.TESSELLATION_CONTROL -> "tcs_main"
            ShaderType.TESSELLATION_EVALUATION -> "tes_main"
        }
    }

    /**
     * Perform platform-specific compilation
     */
    private fun performPlatformSpecificCompilation(
        source: String,
        shaderSource: ShaderSource
    ): PlatformCompilationResult {
        // Platform-specific compilation would be implemented here via expect/actual
        // For now, assume compilation succeeds
        return PlatformCompilationResult(
            success = true,
            log = "Compilation successful (mock implementation)"
        )
    }

    /**
     * Validate compatibility between vertex and fragment shaders
     */
    private fun validateShaderCompatibility(
        vertexShader: CompiledShader,
        fragmentShader: CompiledShader
    ): List<String> {
        val errors = mutableListOf<String>()

        // Basic compatibility checks
        if (vertexShader.source.language != fragmentShader.source.language) {
            errors.add("Vertex and fragment shaders must use the same language")
        }

        // For WGSL, check that vertex outputs match fragment inputs
        if (vertexShader.source.language == ShaderLanguage.WGSL) {
            val vertexOutputs = extractWGSLOutputs(vertexShader.source.getProcessedSource())
            val fragmentInputs = extractWGSLInputs(fragmentShader.source.getProcessedSource())

            // Check that all fragment inputs have corresponding vertex outputs
            fragmentInputs.forEach { input ->
                if (!vertexOutputs.contains(input)) {
                    errors.add("Fragment shader input '@location($input)' has no corresponding vertex output")
                }
            }
        }

        return errors
    }

    /**
     * Validate geometry shader compatibility
     */
    private fun validateGeometryShaderCompatibility(
        vertexShader: CompiledShader?,
        geometryShader: CompiledShader
    ): List<String> {
        val errors = mutableListOf<String>()

        if (vertexShader != null && vertexShader.source.language != geometryShader.source.language) {
            errors.add("Vertex and geometry shaders must use the same language")
        }

        return errors
    }

    /**
     * Perform platform-specific program linking
     */
    private fun performPlatformSpecificLinking(shaders: List<CompiledShader>): PlatformLinkResult {
        // Platform-specific linking would be implemented here via expect/actual
        // For now, assume linking succeeds
        return PlatformLinkResult(
            success = true,
            log = "Linking successful (mock implementation)"
        )
    }

    /**
     * Extract WGSL output location indices from shader source
     */
    private fun extractWGSLOutputs(source: String): Set<Int> {
        val outputs = mutableSetOf<Int>()
        val regex = Regex("""@location\((\d+)\)""")

        // Look for outputs in struct definitions and return statements
        regex.findAll(source).forEach { match ->
            val location = match.groupValues[1].toIntOrNull()
            if (location != null) {
                outputs.add(location)
            }
        }

        return outputs
    }

    /**
     * Extract WGSL input location indices from shader source
     */
    private fun extractWGSLInputs(source: String): Set<Int> {
        val inputs = mutableSetOf<Int>()
        val regex = Regex("""@location\((\d+)\)""")

        // Look for inputs in struct definitions and function parameters
        regex.findAll(source).forEach { match ->
            val location = match.groupValues[1].toIntOrNull()
            if (location != null) {
                inputs.add(location)
            }
        }

        return inputs
    }
}

/**
 * Default compiled shader implementation
 */
private class DefaultCompiledShader(
    override val id: Int,
    override val type: ShaderType,
    override val source: ShaderSource,
    override val isValid: Boolean,
    override val compilationLog: String?
) : CompiledShader {

    private var disposed = false
    private var nativeShaderHandle: Any? = null // Platform-specific shader handle

    override fun dispose() {
        if (disposed) return

        try {
            // Platform-specific native shader resource cleanup
            nativeShaderHandle?.let { handle ->
                cleanupNativeShaderResource(handle)
            }

            // Mark as disposed to prevent double-cleanup
            disposed = true
            nativeShaderHandle = null

        } catch (e: Exception) {
            // Log disposal error but don't throw to avoid issues in cleanup scenarios
            // In a real implementation, this would go to a logger
            println("Warning: Error disposing shader ${id}: ${e.message}")
        }
    }

    /**
     * Check if shader has been disposed
     */
    fun isDisposed(): Boolean = disposed

    /**
     * Platform-specific cleanup method
     * This would be implemented differently for each platform (WebGPU, Vulkan, etc.)
     */
    private fun cleanupNativeShaderResource(handle: Any) {
        // Platform-specific implementation:
        // - WebGPU: Release shader module
        // - Vulkan: Destroy VkShaderModule
        // - OpenGL: Delete shader object
        // For now, just a placeholder
    }
}

/**
 * Default shader program implementation
 */
private class DefaultShaderProgram(
    override val id: Int,
    override val vertexShader: CompiledShader,
    override val fragmentShader: CompiledShader,
    override val geometryShader: CompiledShader?,
    override val computeShader: CompiledShader?,
    override val isLinked: Boolean,
    override val linkLog: String?,
    private val stats: ShaderStatsTracker
) : ShaderProgram {

    private var disposed = false
    private var currentlyBound = false
    private var nativeProgramHandle: Any? = null // Platform-specific program handle
    private val uniformCache = mutableMapOf<String, UniformInfo>()

    override fun use(): RendererResult<Unit> {
        if (disposed) {
            return RendererResult.Error(
                RendererException.InvalidState("Cannot use disposed shader program $id")
            )
        }

        if (!isLinked) {
            return RendererResult.Error(
                RendererException.InvalidState("Cannot use unlinked shader program $id")
            )
        }

        try {
            // Validate that all required shaders are still valid
            if (!vertexShader.isValid || !fragmentShader.isValid) {
                return RendererResult.Error(
                    RendererException.InvalidState("Shader program $id contains invalid shaders")
                )
            }

            // Check for compute vs graphics pipeline mismatch
            if (computeShader != null && (currentlyBound && !isComputePipelineActive())) {
                return RendererResult.Error(
                    RendererException.InvalidState("Cannot bind compute program during graphics rendering")
                )
            }

            // Perform platform-specific binding
            val bindResult = bindShaderProgram(nativeProgramHandle)
            if (!bindResult.success) {
                return RendererResult.Error(
                    RendererException.RenderingFailed("Failed to bind shader program $id: ${bindResult.error}")
                )
            }

            currentlyBound = true
            return RendererResult.Success(Unit)

        } catch (e: Exception) {
            return RendererResult.Error(
                RendererException.RenderingFailed("Exception binding shader program $id: ${e.message}")
            )
        }
    }

    override fun setUniform(name: String, value: Any): RendererResult<Unit> {
        if (disposed) {
            return RendererResult.Error(
                RendererException.InvalidState("Cannot set uniform on disposed shader program $id")
            )
        }

        if (!isLinked) {
            return RendererResult.Error(
                RendererException.InvalidState("Cannot set uniform on unlinked shader program $id")
            )
        }

        if (!currentlyBound) {
            return RendererResult.Error(
                RendererException.InvalidState("Shader program $id must be bound before setting uniforms")
            )
        }

        try {
            // Validate uniform name
            if (name.isBlank()) {
                return RendererResult.Error(
                    RendererException.InvalidState("Uniform name cannot be blank")
                )
            }

            // Get or cache uniform information
            val uniformInfo = uniformCache.getOrPut(name) {
                getUniformInfo(name)
            }

            if (!uniformInfo.exists) {
                return RendererResult.Error(
                    RendererException.InvalidState("Uniform '$name' not found in shader program $id")
                )
            }

            // Validate uniform type compatibility
            val typeValidationResult = validateUniformType(uniformInfo.type, value)
            if (!typeValidationResult.isValid) {
                return RendererResult.Error(
                    RendererException.InvalidState(
                        "Uniform '$name' type mismatch: expected ${uniformInfo.type}, got ${value::class.simpleName}. ${typeValidationResult.error}"
                    )
                )
            }

            // Perform platform-specific uniform setting
            val setResult = setPlatformUniform(uniformInfo, value)
            if (!setResult.success) {
                return RendererResult.Error(
                    RendererException.RenderingFailed("Failed to set uniform '$name': ${setResult.error}")
                )
            }

            return RendererResult.Success(Unit)

        } catch (e: Exception) {
            return RendererResult.Error(
                RendererException.RenderingFailed("Exception setting uniform '$name': ${e.message}")
            )
        }
    }

    override fun dispose() {
        if (disposed) return

        try {
            // Unbind if currently bound
            if (currentlyBound) {
                unbindShaderProgram()
                currentlyBound = false
            }

            // Platform-specific program cleanup
            nativeProgramHandle?.let { handle ->
                cleanupNativeProgramResource(handle)
            }

            // Clear uniform cache
            uniformCache.clear()

            // Update stats
            stats.programDestroyed()

            // Mark as disposed
            disposed = true
            nativeProgramHandle = null

        } catch (e: Exception) {
            // Log disposal error but don't throw to avoid issues in cleanup scenarios
            println("Warning: Error disposing shader program $id: ${e.message}")
        }
    }

    /**
     * Check if program has been disposed
     */
    fun isDisposed(): Boolean = disposed

    /**
     * Check if program is currently bound
     */
    fun isBound(): Boolean = currentlyBound && !disposed

    /**
     * Get uniform information from the shader program
     */
    private fun getUniformInfo(name: String): UniformInfo {
        // Platform-specific uniform introspection would go here
        // For now, assume uniform exists with basic type inference
        return UniformInfo(
            name = name,
            location = name.hashCode(), // Mock location
            type = UniformType.FLOAT, // Default type
            exists = true
        )
    }

    /**
     * Validate that the provided value matches the expected uniform type
     */
    private fun validateUniformType(expectedType: UniformType, value: Any): UniformTypeValidation {
        return when (expectedType) {
            UniformType.FLOAT -> when (value) {
                is Float, is Double -> UniformTypeValidation(true)
                is Number -> UniformTypeValidation(true, "Numeric value will be converted to Float")
                else -> UniformTypeValidation(false, "Expected Float, got ${value::class.simpleName}")
            }

            UniformType.INT -> when (value) {
                is Int, is Long, is Short, is Byte -> UniformTypeValidation(true)
                is Number -> UniformTypeValidation(true, "Numeric value will be converted to Int")
                else -> UniformTypeValidation(false, "Expected Int, got ${value::class.simpleName}")
            }

            UniformType.BOOL -> when (value) {
                is Boolean -> UniformTypeValidation(true)
                else -> UniformTypeValidation(false, "Expected Boolean, got ${value::class.simpleName}")
            }

            UniformType.VEC2 -> when (value) {
                is io.kreekt.core.math.Vector2 -> UniformTypeValidation(true)
                is FloatArray -> if (value.size >= 2) UniformTypeValidation(true) else UniformTypeValidation(
                    false,
                    "FloatArray must have at least 2 elements"
                )

                else -> UniformTypeValidation(false, "Expected Vector2 or FloatArray, got ${value::class.simpleName}")
            }

            UniformType.VEC3 -> when (value) {
                is io.kreekt.core.math.Vector3 -> UniformTypeValidation(true)
                is io.kreekt.core.math.Color -> UniformTypeValidation(true)
                is FloatArray -> if (value.size >= 3) UniformTypeValidation(true) else UniformTypeValidation(
                    false,
                    "FloatArray must have at least 3 elements"
                )

                else -> UniformTypeValidation(
                    false,
                    "Expected Vector3, Color, or FloatArray, got ${value::class.simpleName}"
                )
            }

            UniformType.VEC4 -> when (value) {
                is io.kreekt.core.math.Vector4 -> UniformTypeValidation(true)
                is io.kreekt.core.math.Color -> UniformTypeValidation(true)
                is FloatArray -> if (value.size >= 4) UniformTypeValidation(true) else UniformTypeValidation(
                    false,
                    "FloatArray must have at least 4 elements"
                )

                else -> UniformTypeValidation(
                    false,
                    "Expected Vector4, Color, or FloatArray, got ${value::class.simpleName}"
                )
            }

            UniformType.MAT3 -> when (value) {
                is io.kreekt.core.math.Matrix3 -> UniformTypeValidation(true)
                is FloatArray -> if (value.size >= 9) UniformTypeValidation(true) else UniformTypeValidation(
                    false,
                    "FloatArray must have at least 9 elements"
                )

                else -> UniformTypeValidation(false, "Expected Matrix3 or FloatArray, got ${value::class.simpleName}")
            }

            UniformType.MAT4 -> when (value) {
                is io.kreekt.core.math.Matrix4 -> UniformTypeValidation(true)
                is FloatArray -> if (value.size >= 16) UniformTypeValidation(true) else UniformTypeValidation(
                    false,
                    "FloatArray must have at least 16 elements"
                )

                else -> UniformTypeValidation(false, "Expected Matrix4 or FloatArray, got ${value::class.simpleName}")
            }

            UniformType.SAMPLER2D, UniformType.SAMPLER_CUBE -> when (value) {
                is Texture -> UniformTypeValidation(true)
                is Int -> UniformTypeValidation(true, "Texture unit index")
                else -> UniformTypeValidation(false, "Expected Texture or Int, got ${value::class.simpleName}")
            }
        }
    }

    /**
     * Platform-specific methods - these would be implemented differently for each platform
     */
    private fun isComputePipelineActive(): Boolean = false
    private fun bindShaderProgram(handle: Any?): BindResult = BindResult(true)
    private fun unbindShaderProgram() {}
    private fun cleanupNativeProgramResource(handle: Any) {}
    private fun setPlatformUniform(uniformInfo: UniformInfo, value: Any): SetUniformResult = SetUniformResult(true)
}

/**
 * Uniform information structure
 */
private data class UniformInfo(
    val name: String,
    val location: Int,
    val type: UniformType,
    val exists: Boolean
)

/**
 * Supported uniform types
 */
private enum class UniformType {
    FLOAT, INT, BOOL,
    VEC2, VEC3, VEC4,
    MAT3, MAT4,
    SAMPLER2D, SAMPLER_CUBE
}

/**
 * Uniform type validation result
 */
private data class UniformTypeValidation(
    val isValid: Boolean,
    val error: String? = null
)

/**
 * Platform operation results
 */
private data class BindResult(val success: Boolean, val error: String? = null)
private data class SetUniformResult(val success: Boolean, val error: String? = null)
private data class PlatformCompilationResult(val success: Boolean, val log: String? = null)
private data class PlatformLinkResult(val success: Boolean, val log: String? = null)

/**
 * Statistics tracker for shader operations
 */
private class ShaderStatsTracker {
    private var totalCompilations = 0
    private var successfulCompilations = 0
    private var failedCompilations = 0
    private var cacheHits = 0
    private var cacheMisses = 0
    private var totalPrograms = 0
    private var activePrograms = 0
    private var compilationTimeMs = 0L

    fun compilationSucceeded() {
        totalCompilations++
        successfulCompilations++
    }

    fun compilationFailed() {
        totalCompilations++
        failedCompilations++
    }

    fun cacheHit() {
        cacheHits++
    }

    fun cacheMiss() {
        cacheMisses++
    }

    fun programCreated() {
        totalPrograms++
        activePrograms++
    }

    fun programDestroyed() {
        activePrograms = maxOf(0, activePrograms - 1)
    }

    fun recordCompilationTime(timeMs: Long) {
        compilationTimeMs += timeMs
    }

    fun getStats(): ShaderStats {
        return ShaderStats(
            totalCompilations = totalCompilations,
            successfulCompilations = successfulCompilations,
            failedCompilations = failedCompilations,
            cacheHits = cacheHits,
            cacheMisses = cacheMisses,
            totalPrograms = totalPrograms,
            activePrograms = activePrograms,
            compilationTimeMs = compilationTimeMs
        )
    }

    fun reset() {
        totalCompilations = 0
        successfulCompilations = 0
        failedCompilations = 0
        cacheHits = 0
        cacheMisses = 0
        totalPrograms = 0
        activePrograms = 0
        compilationTimeMs = 0
    }
}

/**
 * Utility functions for common shader operations
 */
object ShaderUtils {

    /**
     * Create a basic vertex shader source
     */
    fun createBasicVertexShader(): ShaderSource {
        return ShaderSource(
            type = ShaderType.VERTEX,
            source = """
                struct VertexInput {
                    @location(0) position: vec3<f32>,
                    @location(1) normal: vec3<f32>,
                    @location(2) uv: vec2<f32>,
                }

                struct VertexOutput {
                    @builtin(position) clip_position: vec4<f32>,
                    @location(0) world_position: vec3<f32>,
                    @location(1) normal: vec3<f32>,
                    @location(2) uv: vec2<f32>,
                }

                struct Uniforms {
                    model_matrix: mat4x4<f32>,
                    view_matrix: mat4x4<f32>,
                    projection_matrix: mat4x4<f32>,
                }

                @group(0) @binding(0) var<uniform> uniforms: Uniforms;

                @vertex
                fn main(input: VertexInput) -> VertexOutput {
                    var output: VertexOutput;

                    let world_position = uniforms.model_matrix * vec4<f32>(input.position, 1.0);
                    output.clip_position = uniforms.projection_matrix * uniforms.view_matrix * world_position;
                    output.world_position = world_position.xyz;
                    output.normal = (uniforms.model_matrix * vec4<f32>(input.normal, 0.0)).xyz;
                    output.uv = input.uv;

                    return output;
                }
            """.trimIndent()
        )
    }

    /**
     * Create a basic fragment shader source
     */
    fun createBasicFragmentShader(): ShaderSource {
        return ShaderSource(
            type = ShaderType.FRAGMENT,
            source = """
                struct VertexOutput {
                    @builtin(position) clip_position: vec4<f32>,
                    @location(0) world_position: vec3<f32>,
                    @location(1) normal: vec3<f32>,
                    @location(2) uv: vec2<f32>,
                }

                struct Material {
                    color: vec4<f32>,
                    metallic: f32,
                    roughness: f32,
                    emissive: vec3<f32>,
                }

                @group(1) @binding(0) var<uniform> material: Material;
                @group(1) @binding(1) var texture_sampler: sampler;
                @group(1) @binding(2) var base_color_texture: texture_2d<f32>;

                @fragment
                fn main(input: VertexOutput) -> @location(0) vec4<f32> {
                    let base_color = textureSample(base_color_texture, texture_sampler, input.uv);
                    let final_color = material.color * base_color;

                    return vec4<f32>(final_color.rgb, final_color.a);
                }
            """.trimIndent()
        )
    }

    /**
     * Create a compute shader for texture processing
     */
    fun createTextureProcessComputeShader(): ShaderSource {
        return ShaderSource(
            type = ShaderType.COMPUTE,
            source = """
                @group(0) @binding(0) var input_texture: texture_2d<f32>;
                @group(0) @binding(1) var output_texture: texture_storage_2d<rgba8unorm, write>;

                @compute @workgroup_size(8, 8)
                fn main(@builtin(global_invocation_id) global_id: vec3<u32>) {
                    let dims = textureDimensions(input_texture);
                    if (global_id.x >= dims.x || global_id.y >= dims.y) {
                        return;
                    }

                    let color = textureLoad(input_texture, vec2<i32>(global_id.xy), 0);
                    textureStore(output_texture, vec2<i32>(global_id.xy), color);
                }
            """.trimIndent()
        )
    }

    /**
     * Generate shader defines for material properties
     */
    fun generateMaterialDefines(material: Material): Map<String, String> {
        val defines = mutableMapOf<String, String>()

        // Add material-specific defines based on material type and properties
        when (material) {
            is MeshStandardMaterial -> {
                if (material.map != null) defines["HAS_BASE_COLOR_MAP"] = "1"
                if (material.normalMap != null) defines["HAS_NORMAL_MAP"] = "1"
                if (material.metalnessMap != null) defines["HAS_METALLIC_MAP"] = "1"
                if (material.roughnessMap != null) defines["HAS_ROUGHNESS_MAP"] = "1"
                if (material.aoMap != null) defines["HAS_AO_MAP"] = "1"
                if (material.emissiveMap != null) defines["HAS_EMISSIVE_MAP"] = "1"
            }
        }

        return defines
    }

    /**
     * Validate shader source for common issues
     */
    fun validateShaderSource(source: ShaderSource): List<String> {
        val issues = mutableListOf<String>()

        if (source.source.isBlank()) {
            issues.add("Shader source is empty")
        }

        if (source.type == ShaderType.VERTEX && !source.source.contains("@vertex")) {
            issues.add("Vertex shader missing @vertex entry point")
        }

        if (source.type == ShaderType.FRAGMENT && !source.source.contains("@fragment")) {
            issues.add("Fragment shader missing @fragment entry point")
        }

        if (source.type == ShaderType.COMPUTE && !source.source.contains("@compute")) {
            issues.add("Compute shader missing @compute entry point")
        }

        return issues
    }
}