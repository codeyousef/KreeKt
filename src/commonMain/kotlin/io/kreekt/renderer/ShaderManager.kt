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
        // Platform-specific shader compilation would go here
        return DefaultCompiledShader(
            id = generateShaderId(),
            type = source.type,
            source = source,
            isValid = true, // Assume compilation success for default implementation
            compilationLog = null
        )
    }

    private fun linkProgramImpl(shaders: List<CompiledShader>): ShaderProgram {
        // Platform-specific program linking would go here
        val vertexShader = shaders.first { it.type == ShaderType.VERTEX }
        val fragmentShader = shaders.firstOrNull { it.type == ShaderType.FRAGMENT }
        val geometryShader = shaders.firstOrNull { it.type == ShaderType.GEOMETRY }
        val computeShader = shaders.firstOrNull { it.type == ShaderType.COMPUTE }

        return DefaultShaderProgram(
            id = generateProgramId(),
            vertexShader = vertexShader,
            fragmentShader = fragmentShader ?: vertexShader, // Fallback for compute
            geometryShader = geometryShader,
            computeShader = computeShader,
            isLinked = true, // Assume linking success for default implementation
            linkLog = null
        )
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

    override fun dispose() {
        // Cleanup would go here
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
    override val linkLog: String?
) : ShaderProgram {

    override fun use(): RendererResult<Unit> {
        // Platform-specific shader program binding would go here
        return RendererResult.Success(Unit)
    }

    override fun setUniform(name: String, value: Any): RendererResult<Unit> {
        // Platform-specific uniform setting would go here
        return RendererResult.Success(Unit)
    }

    override fun dispose() {
        // Cleanup would go here
    }
}

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