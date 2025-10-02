package io.kreekt.renderer

import io.kreekt.core.scene.Material
import io.kreekt.material.MeshStandardMaterial
import io.kreekt.renderer.shader.*

/**
 * Advanced shader management system
 * T117 - Shader compilation, caching, and program management
 *
 * Implementation is now split into focused modules:
 * - shader/ShaderValidation.kt: Source validation and compatibility checking
 * - shader/ShaderCompilation.kt: Compilation and linking logic
 * - shader/ShaderImplementations.kt: Concrete shader and program implementations
 * - shader/ShaderStatsTracker.kt: Performance statistics
 * - shader/ShaderUtils.kt: Common shader operations and utilities
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
    private val compiler = ShaderCompiler(capabilities)
    private val linker = ShaderLinker(capabilities, stats)

    override fun compileShader(source: ShaderSource): ShaderCompilationResult {
        val startTime = 0L

        try {
            // Check cache first
            val cacheKey = ShaderUtils.generateShaderCacheKey(source)
            shaderCache[cacheKey]?.let { cached ->
                stats.cacheHit()
                return ShaderCompilationResult.Success(cached)
            }

            stats.cacheMiss()

            // Validate shader type support
            if (!ShaderValidation.isShaderTypeSupported(source.type, capabilities)) {
                stats.compilationFailed()
                return ShaderCompilationResult.Error("Shader type ${source.type} not supported")
            }

            // Compile shader
            val compiledShader = compiler.compileShaderImpl(source)

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

            stats.recordCompilationTime(0L)
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
            val program = linker.linkProgramImpl(shaders)

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
        val cacheKey = ShaderUtils.generateProgramCacheKey(vertexSource, fragmentSource)

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
        val cacheKey = ShaderUtils.generateComputeCacheKey(computeSource)

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
}
