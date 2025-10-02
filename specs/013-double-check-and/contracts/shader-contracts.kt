/**
 * Shader System Contracts
 *
 * Feature: 013-double-check-and (Three.js r180 Feature Parity)
 * Category: Shader Composition and Management
 *
 * Requirements covered: FR-067 through FR-071
 */

package io.kreekt.material.shader

import io.kreekt.core.Result
import io.kreekt.material.Uniform

/**
 * Shader chunk library for modular composition.
 *
 * FR-067: System MUST support shader chunks system for modular shader composition
 * FR-069: System MUST support custom shader includes (#include directives)
 *
 * @sample
 * ```kotlin
 * // Register custom chunk
 * ShaderChunk.registerChunk("my_custom_lighting", """
 *     fn calculateCustomLighting(normal: vec3<f32>) -> vec3<f32> {
 *         // Custom lighting calculation
 *         return normal * 0.5 + 0.5;
 *     }
 * """)
 *
 * // Use in shader
 * val fragmentShader = """
 *     #include <common>
 *     #include <my_custom_lighting>
 *
 *     @fragment
 *     fn main() {
 *         let lighting = calculateCustomLighting(normal);
 *         // ...
 *     }
 * """
 * ```
 */
object ShaderChunk {
    /**
     * Registers a shader chunk.
     *
     * @param name Chunk identifier
     * @param code Shader code
     */
    fun registerChunk(name: String, code: String)

    /**
     * Gets a shader chunk by name.
     *
     * @param name Chunk identifier
     * @return Shader code or null if not found
     */
    fun getChunk(name: String): String?

    /**
     * Checks if a chunk exists.
     */
    fun hasChunk(name: String): Boolean

    /**
     * Gets all registered chunks.
     */
    fun getAllChunks(): Map<String, String>

    /**
     * Clears all chunks (for testing).
     */
    fun clear()

    // Standard shader chunks
    val COMMON: String
    val FOG_PARS_FRAGMENT: String
    val FOG_FRAGMENT: String
    val LIGHTS_PARS_FRAGMENT: String
    val LIGHTS_FRAGMENT: String
    val NORMAL_PARS_FRAGMENT: String
    val NORMAL_FRAGMENT: String
    val SHADOWMAP_PARS_FRAGMENT: String
    val SHADOWMAP_FRAGMENT: String
}

/**
 * Shader preprocessor for #include and #define directives.
 *
 * FR-069: System MUST support custom shader includes (#include directives)
 */
class ShaderPreprocessor {
    /**
     * Processes shader source code.
     *
     * @param source Shader source
     * @param target Target platform (WGSL or SPIR-V)
     * @param defines Preprocessor defines
     * @return Processed shader code
     */
    fun process(
        source: String,
        target: io.kreekt.material.ShaderTarget,
        defines: Map<String, String> = emptyMap()
    ): Result<String>
}

/**
 * Shader definition with uniforms.
 */
data class Shader(
    var vertexShader: String,
    var fragmentShader: String,
    val uniforms: MutableMap<String, Uniform> = mutableMapOf(),
    val defines: MutableMap<String, String> = mutableMapOf(),
    val target: io.kreekt.material.ShaderTarget = io.kreekt.material.ShaderTarget.WGSL
)

/**
 * Standard uniforms library.
 *
 * FR-070: System MUST provide UniformsLib standard uniforms library
 *
 * @sample
 * ```kotlin
 * val uniforms = UniformsUtils.merge(
 *     UniformsLib.common,
 *     UniformsLib.lights,
 *     UniformsLib.fog
 * )
 * ```
 */
object UniformsLib {
    /**
     * Common uniforms (matrices, etc.).
     */
    val common: Map<String, Uniform>

    /**
     * Lighting uniforms.
     */
    val lights: Map<String, Uniform>

    /**
     * Fog uniforms.
     */
    val fog: Map<String, Uniform>

    /**
     * Shadow map uniforms.
     */
    val shadowmap: Map<String, Uniform>

    /**
     * Environment map uniforms.
     */
    val envmap: Map<String, Uniform>
}

/**
 * Uniform manipulation utilities.
 *
 * FR-071: System MUST provide UniformsUtils for uniform manipulation
 */
object UniformsUtils {
    /**
     * Deep clones a uniform map.
     */
    fun clone(uniforms: Map<String, Uniform>): MutableMap<String, Uniform>

    /**
     * Merges multiple uniform maps.
     */
    fun merge(vararg uniformSets: Map<String, Uniform>): MutableMap<String, Uniform>
}

/**
 * Material callback for runtime shader modification.
 *
 * FR-068: System MUST support onBeforeCompile material callback for shader modification
 *
 * Example usage in Material subclass:
 * ```kotlin
 * override fun onBeforeCompile(shader: Shader, renderer: Renderer) {
 *     // Add custom uniforms
 *     shader.uniforms["time"] = Uniform(0.0f)
 *
 *     // Modify vertex shader
 *     shader.vertexShader = shader.vertexShader.replace(
 *         "// INSERT_CUSTOM_VERTEX",
 *         "position.y += sin(position.x + time) * 0.1;"
 *     )
 *
 *     // Modify fragment shader
 *     shader.fragmentShader = shader.fragmentShader.replace(
 *         "// INSERT_CUSTOM_FRAGMENT",
 *         "color *= vec3(time);"
 *     )
 * }
 * ```
 */
interface ShaderModifiable {
    /**
     * Called before shader compilation, allowing runtime modification.
     *
     * @param shader The shader to modify
     * @param renderer The renderer (for capability queries)
     */
    fun onBeforeCompile(shader: Shader, renderer: io.kreekt.renderer.Renderer)
}
