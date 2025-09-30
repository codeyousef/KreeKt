/**
 * Morph Targets API Contract
 * Maps FR-M001 through FR-M008
 *
 * Constitutional Requirements:
 * - Efficient blend shape animation
 * - Type-safe morph target management
 * - Performance: <5ms morph computation per frame for 10k vertices
 */

package io.kreekt.morph

import io.kreekt.geometry.BufferGeometry
import io.kreekt.core.Mesh
import io.kreekt.material.Material
import io.kreekt.math.Vector3

/**
 * FR-M001, FR-M002: Morph target support in geometry
 *
 * Test Contract:
 * - MUST store multiple morph target shapes
 * - MUST blend between base and targets
 * - MUST support named morph targets
 * - MUST support position and normal morphing
 */
interface MorphTargetGeometry {
    /**
     * List of morph target attributes
     * Each target contains position (and optionally normal) deltas
     */
    val morphAttributes: MorphAttributes

    /**
     * Named morph targets for animation
     */
    val morphTargets: List<MorphTarget>

    /**
     * Check if geometry has morph targets
     */
    fun hasMorphTargets(): Boolean = morphTargets.isNotEmpty()

    /**
     * Get number of morph targets
     */
    fun getMorphTargetCount(): Int = morphTargets.size

    /**
     * Add morph target to geometry
     *
     * @param name Target name
     * @param positions Position deltas (same count as base geometry)
     * @param normals Normal deltas (optional)
     *
     * Test Contract:
     * - MUST validate attribute counts match base geometry
     * - MUST store as GPU-uploadable format
     */
    fun addMorphTarget(
        name: String,
        positions: FloatArray,
        normals: FloatArray? = null
    )

    /**
     * Remove morph target by name
     */
    fun removeMorphTarget(name: String)

    /**
     * Get morph target by name
     */
    fun getMorphTarget(name: String): MorphTarget?
}

/**
 * FR-M003: Morph target data structure
 */
data class MorphTarget(
    val name: String,
    val index: Int
)

/**
 * Morph attributes container
 */
data class MorphAttributes(
    val position: List<BufferAttribute>? = null,
    val normal: List<BufferAttribute>? = null,
    val color: List<BufferAttribute>? = null,
    val uv: List<BufferAttribute>? = null
)

/**
 * FR-M004, FR-M005: Mesh with morph target animation
 *
 * Test Contract:
 * - MUST blend morph targets in shader
 * - MUST support multiple simultaneous morphs
 * - MUST provide influence weights [0, 1]
 */
interface MorphTargetMesh {
    /**
     * Influence weight for each morph target [0, 1]
     * 0 = no influence, 1 = full influence
     */
    val morphTargetInfluences: FloatArray?

    /**
     * Dictionary mapping morph target names to indices
     */
    val morphTargetDictionary: Map<String, Int>?

    /**
     * Update morph target influences
     *
     * @param influences New influence values
     *
     * Test Contract:
     * - MUST clamp influences to [0, 1]
     * - MUST mark uniforms for GPU update
     * - MUST handle null influences (all zero)
     */
    fun updateMorphTargets(influences: FloatArray)

    /**
     * Set influence by morph target name
     *
     * @param name Morph target name
     * @param influence Influence value [0, 1]
     *
     * Test Contract:
     * - MUST look up target by name
     * - MUST validate name exists
     * - MUST clamp influence
     */
    fun setMorphTargetInfluence(name: String, influence: Float)

    /**
     * Get influence by morph target name
     */
    fun getMorphTargetInfluence(name: String): Float?

    /**
     * Get morph target index by name
     */
    fun getMorphTargetIndexByName(name: String): Int?
}

/**
 * FR-M006: Morph animation mixer
 *
 * Test Contract:
 * - MUST animate morph target influences over time
 * - MUST blend multiple morph animations
 * - MUST support looping and clamping
 */
class MorphAnimationMixer(mesh: Mesh) {
    val mesh: Mesh
    private val activeAnimations: MutableList<MorphAnimation> = mutableListOf()

    /**
     * Play morph animation
     *
     * @param animation Morph animation clip
     * @param blendWeight Weight for blending [0, 1]
     *
     * Test Contract:
     * - MUST add to active animations
     * - MUST blend with existing animations
     */
    fun play(animation: MorphAnimation, blendWeight: Float = 1f)

    /**
     * Stop morph animation
     */
    fun stop(animation: MorphAnimation)

    /**
     * Update all active animations
     *
     * @param deltaTime Time since last update (seconds)
     *
     * Test Contract:
     * - MUST update animation times
     * - MUST blend influences from all active animations
     * - MUST handle animation completion
     */
    fun update(deltaTime: Float)

    /**
     * Stop all animations
     */
    fun stopAll()
}

/**
 * FR-M007: Morph animation clip
 *
 * Test Contract:
 * - MUST define morph target influence keyframes
 * - MUST support multiple morph targets
 * - MUST interpolate between keyframes
 */
data class MorphAnimation(
    val name: String,
    val duration: Float,
    val tracks: List<MorphTrack>,
    val loop: Boolean = true
) {
    /**
     * Sample animation at time
     *
     * @param time Time in seconds
     * @return Map of morph target names to influences
     *
     * Test Contract:
     * - MUST interpolate between keyframes
     * - MUST handle time wrapping for loops
     * - MUST clamp influences to [0, 1]
     */
    fun sample(time: Float): Map<String, Float>
}

/**
 * Morph target animation track
 */
data class MorphTrack(
    val morphTargetName: String,
    val times: FloatArray,
    val values: FloatArray
) {
    init {
        require(times.size == values.size) {
            "Times and values must have same length"
        }
    }

    /**
     * Interpolate track value at time
     *
     * Test Contract:
     * - MUST find surrounding keyframes
     * - MUST interpolate linearly
     * - MUST clamp to [0, 1]
     */
    fun interpolate(time: Float): Float
}

/**
 * FR-M008: Morph target shader generation
 *
 * Test Contract:
 * - MUST generate vertex shader with morph blending
 * - MUST inject morph uniforms
 * - MUST support platform-specific shader languages (WGSL/GLSL)
 */
object MorphShaderGenerator {
    /**
     * Generate morph uniform declarations
     *
     * @param morphCount Number of morph targets
     * @return WGSL uniform declarations
     *
     * Test Contract:
     * - MUST declare morphTargetInfluences array
     * - MUST declare morph target textures
     */
    fun generateUniforms(morphCount: Int): String

    /**
     * Generate vertex shader morph blending code
     *
     * @param morphCount Number of morph targets
     * @param morphNormals Include normal morphing
     * @return WGSL vertex shader code
     *
     * Test Contract:
     * - MUST blend positions from all targets
     * - MUST blend normals if requested
     * - MUST weight by influences
     * - Formula: finalPosition = basePosition + Σ(influence_i * delta_i)
     */
    fun generateVertexCode(
        morphCount: Int,
        morphNormals: Boolean = false
    ): String

    /**
     * Inject morph code into existing shader
     *
     * Test Contract:
     * - MUST preserve existing shader logic
     * - MUST insert morph before transformation
     */
    fun injectMorphCode(
        vertexShader: String,
        morphCount: Int,
        morphNormals: Boolean = false
    ): String
}

/**
 * Morph target utilities
 */
object MorphUtils {
    /**
     * Create morph target from position deltas
     *
     * Test Contract:
     * - MUST validate delta count matches geometry
     * - MUST create BufferAttribute
     */
    fun createMorphTarget(
        name: String,
        basePositions: FloatArray,
        targetPositions: FloatArray
    ): MorphTarget

    /**
     * Compute morph target normals from position deltas
     *
     * Test Contract:
     * - MUST recompute normals for morphed geometry
     * - MUST maintain consistency with base normals
     */
    fun computeMorphNormals(
        baseGeometry: BufferGeometry,
        morphedPositions: FloatArray
    ): FloatArray

    /**
     * Blend multiple morph targets on CPU
     *
     * @param basePositions Base geometry positions
     * @param morphTargets List of morph target position arrays
     * @param influences Influence weights
     * @return Blended positions
     *
     * Test Contract:
     * - MUST apply weighted sum of deltas
     * - MUST handle zero influences efficiently
     */
    fun blendMorphTargets(
        basePositions: FloatArray,
        morphTargets: List<FloatArray>,
        influences: FloatArray
    ): FloatArray

    /**
     * Bake morph animation to vertex animation texture (VAT)
     *
     * Test Contract:
     * - MUST sample animation at frame intervals
     * - MUST pack positions into texture
     * - MUST be usable for GPU-side playback
     */
    fun bakeToTexture(
        mesh: Mesh,
        animation: MorphAnimation,
        frameRate: Int = 30
    ): VertexAnimationTexture
}

/**
 * Vertex animation texture for baked morph animations
 */
data class VertexAnimationTexture(
    val texture: DataTexture,
    val frameCount: Int,
    val duration: Float
)

// Forward declarations
expect class BufferAttribute
expect class DataTexture