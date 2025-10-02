/**
 * Concrete implementations of compiled shaders and shader programs
 */
package io.kreekt.renderer.shader

import io.kreekt.renderer.*
import io.kreekt.core.scene.Texture

/**
 * Default compiled shader implementation
 */
internal class DefaultCompiledShader(
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
            println("Warning: Error disposing shader ${id}: ${e.message}")
        }
    }

    /**
     * Check if shader has been disposed
     */
    fun isDisposed(): Boolean = disposed

    /**
     * Platform-specific cleanup method
     */
    private fun cleanupNativeShaderResource(handle: Any) {
        // Platform-specific implementation via expect/actual
    }
}

/**
 * Default shader program implementation
 */
internal class DefaultShaderProgram(
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
            // Log disposal error but don't throw
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
        // Platform-specific uniform introspection
        return UniformInfo(
            name = name,
            location = name.hashCode(),
            type = UniformType.FLOAT,
            exists = true
        )
    }

    /**
     * Validate that the provided value matches the expected uniform type
     */
    private fun validateUniformType(expectedType: UniformType, value: Any): UniformTypeValidation {
        return UniformTypeValidator.validate(expectedType, value)
    }

    // Platform-specific methods - these would be implemented differently for each platform
    private fun isComputePipelineActive(): Boolean = false
    private fun bindShaderProgram(handle: Any?): BindResult = BindResult(true)
    private fun unbindShaderProgram() {}
    private fun cleanupNativeProgramResource(handle: Any) {}
    private fun setPlatformUniform(uniformInfo: UniformInfo, value: Any): SetUniformResult = SetUniformResult(true)
}

/**
 * Uniform information structure
 */
internal data class UniformInfo(
    val name: String,
    val location: Int,
    val type: UniformType,
    val exists: Boolean
)

/**
 * Supported uniform types
 */
internal enum class UniformType {
    FLOAT, INT, BOOL,
    VEC2, VEC3, VEC4,
    MAT3, MAT4,
    SAMPLER2D, SAMPLER_CUBE
}

/**
 * Uniform type validation result
 */
internal data class UniformTypeValidation(
    val isValid: Boolean,
    val error: String? = null
)

/**
 * Platform operation results
 */
internal data class BindResult(val success: Boolean, val error: String? = null)
internal data class SetUniformResult(val success: Boolean, val error: String? = null)

/**
 * Uniform type validator
 */
internal object UniformTypeValidator {
    fun validate(expectedType: UniformType, value: Any): UniformTypeValidation {
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
}
