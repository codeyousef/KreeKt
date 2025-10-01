/**
 * Fog System API Contract
 * Maps FR-F001 through FR-F006
 *
 * Constitutional Requirements:
 * - Shader integration for fog calculations
 * - Type-safe fog parameters
 * - Performance: <1ms fog computation per frame
 */

package io.kreekt.fog

import io.kreekt.math.Color

/**
 * Base fog interface
 *
 * Test Contract:
 * - MUST be attachable to Scene
 * - MUST integrate with material shaders
 * - MUST support serialization
 */
interface IFog {
    val name: String
    val color: Color

    /**
     * Generate fog shader code for insertion into fragment shader
     *
     * @return WGSL/GLSL fog calculation code
     */
    fun generateShaderCode(): String

    /**
     * Clone fog instance
     */
    fun clone(): IFog
}

/**
 * FR-F001, FR-F002, FR-F003: Linear fog (distance-based)
 *
 * Test Contract:
 * - MUST interpolate linearly between near and far
 * - MUST apply fog color based on depth
 * - MUST integrate with depth buffer
 * - Formula: factor = (far - depth) / (far - near)
 */
class Fog(
    color: Color = Color(0xffffff),
    near: Float = 1f,
    far: Float = 1000f
) : IFog {
    override val name: String
    override var color: Color

    /**
     * Distance at which fog starts
     */
    var near: Float

    /**
     * Distance at which fog is maximum
     */
    var far: Float

    override fun generateShaderCode(): String
    override fun clone(): Fog

    /**
     * Get fog factor at given depth
     *
     * Test Contract:
     * - MUST return 1.0 at near distance (no fog)
     * - MUST return 0.0 at far distance (full fog)
     * - MUST clamp to [0, 1]
     */
    fun getFogFactor(depth: Float): Float
}

/**
 * FR-F004, FR-F005, FR-F006: Exponential fog (density-based)
 *
 * Test Contract:
 * - MUST apply exponential falloff
 * - MUST support squared exponential mode
 * - MUST use density parameter
 * - Formula: factor = exp(-density * depth) or exp(-(density * depth)^2)
 */
class FogExp2(
    color: Color = Color(0xffffff),
    density: Float = 0.00025f
) : IFog {
    override val name: String
    override var color: Color

    /**
     * Fog density coefficient
     * Higher values = thicker fog
     */
    var density: Float

    override fun generateShaderCode(): String
    override fun clone(): FogExp2

    /**
     * Get fog factor at given depth
     *
     * Test Contract:
     * - MUST return value in [0, 1]
     * - MUST use exponential squared formula
     * - Higher density = more fog at same distance
     */
    fun getFogFactor(depth: Float): Float
}

/**
 * Fog shader integration utilities
 *
 * Test Contract:
 * - MUST generate platform-appropriate shader code
 * - MUST inject fog uniforms
 * - MUST modify fragment shader output
 */
object FogShaderInjector {
    /**
     * Generate fog uniform declarations
     *
     * @param fog Fog instance
     * @return WGSL uniform declaration code
     */
    fun generateUniforms(fog: IFog): String

    /**
     * Generate fog calculation code for fragment shader
     *
     * @param fog Fog instance
     * @return WGSL fog calculation code
     *
     * Test Contract:
     * - MUST read from depth buffer
     * - MUST compute fog factor
     * - MUST blend with fog color
     */
    fun generateFragmentCode(fog: IFog): String

    /**
     * Inject fog into existing shader code
     *
     * @param shaderCode Original fragment shader
     * @param fog Fog instance
     * @return Modified shader with fog
     *
     * Test Contract:
     * - MUST preserve existing shader logic
     * - MUST insert fog before final output
     * - MUST handle alpha blending correctly
     */
    fun injectFog(shaderCode: String, fog: IFog): String
}

/**
 * Scene fog attachment
 *
 * Extension for Scene class to support fog
 */
interface SceneFogSupport {
    /**
     * Fog applied to scene
     * null = no fog
     */
    var fog: IFog?

    /**
     * Check if scene has fog enabled
     */
    fun hasFog(): Boolean = fog != null
}