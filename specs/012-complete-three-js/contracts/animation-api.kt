/**
 * Animation API Contract
 *
 * This file defines the complete API surface for the animation subsystem,
 * ensuring Three.js r180 compatibility with Kotlin idioms.
 *
 * Contract Design Principles:
 * - Type-safe method signatures
 * - Data classes for immutable configuration
 * - Default parameters for common use cases
 * - Sealed classes for type hierarchies
 * - Extension functions for DSL builders
 */

package io.kreekt.contracts.animation

import io.kreekt.core.math.*
import io.kreekt.animation.*
import io.kreekt.scene.*

// ============================================================================
// Core Animation API
// ============================================================================

/**
 * AnimationClip: Reusable set of keyframe tracks
 * Three.js equivalent: THREE.AnimationClip
 */
interface AnimationClipAPI {
    var name: String
    val duration: Float
    val tracks: List<KeyframeTrack>
    val uuid: String
    var blendMode: AnimationBlendMode

    // Static utilities
    companion object {
        fun findByName(objectOrClips: Any, name: String): AnimationClip?
        fun CreateFromMorphTargetSequence(
            name: String,
            morphTargetSequence: List<MorphTarget>,
            fps: Float,
            noLoop: Boolean = false
        ): AnimationClip

        fun CreateClipsFromMorphTargetSequences(
            morphTargets: List<MorphTarget>,
            fps: Float,
            noLoop: Boolean = false
        ): List<AnimationClip>

        fun parse(json: String): AnimationClip
        fun parseAnimation(animation: Any, bones: List<Bone>): AnimationClip
        fun toJSON(clip: AnimationClip): String
    }

    // Instance methods
    fun resetDuration(): AnimationClip
    fun trim(): AnimationClip
    fun validate(): Boolean
    fun optimize(): AnimationClip
    fun clone(): AnimationClip
}

/**
 * AnimationMixer: Animation player for a specific root object
 * Three.js equivalent: THREE.AnimationMixer
 */
interface AnimationMixerAPI {
    val time: Float
    val timeScale: Float
    val root: Object3D

    // Clip management
    fun clipAction(
        clip: AnimationClip,
        optionalRoot: Object3D? = null
    ): AnimationAction

    fun existingAction(
        clip: AnimationClip,
        optionalRoot: Object3D? = null
    ): AnimationAction?

    fun stopAllAction(): AnimationMixer
    fun update(deltaTime: Float): AnimationMixer
    fun setTime(timeInSeconds: Float): AnimationMixer
    fun getRoot(): Object3D

    // Event handling
    fun addEventListener(type: String, listener: (Event) -> Unit)
    fun hasEventListener(type: String, listener: (Event) -> Unit): Boolean
    fun removeEventListener(type: String, listener: (Event) -> Unit)
    fun dispatchEvent(event: Event)

    // Cleanup
    fun uncacheClip(clip: AnimationClip)
    fun uncacheRoot(root: Object3D)
    fun uncacheAction(clip: AnimationClip, optionalRoot: Object3D? = null)
}

/**
 * AnimationAction: Scheduled animation clip instance
 * Three.js equivalent: THREE.AnimationAction
 */
interface AnimationActionAPI {
    // Playback control
    var loop: AnimationActionLoopStyles
    var time: Float
    var timeScale: Float
    var weight: Float
    var repetitions: Int
    var paused: Boolean
    var enabled: Boolean
    var clampWhenFinished: Boolean
    var zeroSlopeAtStart: Boolean
    var zeroSlopeAtEnd: Boolean

    // State
    val isRunning: Boolean
    val isScheduled: Boolean

    // Playback methods
    fun play(): AnimationAction
    fun stop(): AnimationAction
    fun reset(): AnimationAction
    fun isRunning(): Boolean
    fun isScheduled(): Boolean
    fun startAt(time: Float): AnimationAction
    fun setLoop(mode: AnimationActionLoopStyles, repetitions: Int = Int.MAX_VALUE): AnimationAction
    fun setEffectiveWeight(weight: Float): AnimationAction
    fun getEffectiveWeight(): Float
    fun fadeIn(duration: Float): AnimationAction
    fun fadeOut(duration: Float): AnimationAction
    fun crossFadeFrom(fadeOutAction: AnimationAction, duration: Float, warp: Boolean = false): AnimationAction
    fun crossFadeTo(fadeInAction: AnimationAction, duration: Float, warp: Boolean = false): AnimationAction
    fun stopFading(): AnimationAction
    fun setEffectiveTimeScale(timeScale: Float): AnimationAction
    fun getEffectiveTimeScale(): Float
    fun setDuration(duration: Float): AnimationAction
    fun syncWith(action: AnimationAction): AnimationAction
    fun halt(duration: Float): AnimationAction
    fun warp(startTimeScale: Float, endTimeScale: Float, duration: Float): AnimationAction
    fun stopWarping(): AnimationAction
    fun getMixer(): AnimationMixer
    fun getClip(): AnimationClip
    fun getRoot(): Object3D
}

/**
 * AnimationObjectGroup: Group of objects sharing animations
 * Three.js equivalent: THREE.AnimationObjectGroup
 */
interface AnimationObjectGroupAPI {
    val uuid: String
    val stats: AnimationObjectGroupStats

    fun add(vararg objects: Object3D)
    fun remove(vararg objects: Object3D)
    fun uncache(vararg objects: Object3D)
}

data class AnimationObjectGroupStats(
    val bindingsPerObject: Int,
    val objects: AnimationObjectGroupStatsObjects
)

data class AnimationObjectGroupStatsObjects(
    val total: Int,
    val inUse: Int
)

// ============================================================================
// Keyframe Tracks
// ============================================================================

/**
 * KeyframeTrack: Base class for all keyframe tracks
 * Three.js equivalent: THREE.KeyframeTrack
 */
interface KeyframeTrackAPI {
    val name: String
    val times: FloatArray
    val values: FloatArray
    val valueSize: Int
    val interpolation: InterpolationMode
    val timeScale: Float
    val valueType: String

    fun InterpolantFactoryMethodDiscrete(result: FloatArray): Interpolant
    fun InterpolantFactoryMethodLinear(result: FloatArray): Interpolant
    fun InterpolantFactoryMethodSmooth(result: FloatArray): Interpolant
    fun setInterpolation(interpolation: InterpolationMode): KeyframeTrack
    fun getInterpolation(): InterpolationMode
    fun getValueSize(): Int
    fun shift(timeOffset: Float): KeyframeTrack
    fun scale(timeScale: Float): KeyframeTrack
    fun trim(startTime: Float, endTime: Float): KeyframeTrack
    fun validate(): Boolean
    fun optimize(): KeyframeTrack
    fun clone(): KeyframeTrack

    companion object {
        fun toJSON(track: KeyframeTrack): String
        fun parse(json: String): KeyframeTrack
    }
}

/**
 * VectorKeyframeTrack: Vector3 keyframes (position, scale)
 * Three.js equivalent: THREE.VectorKeyframeTrack
 */
interface VectorKeyframeTrackAPI : KeyframeTrackAPI {
    // Inherits all from KeyframeTrack, valueSize = 3
}

/**
 * QuaternionKeyframeTrack: Quaternion keyframes (rotation)
 * Three.js equivalent: THREE.QuaternionKeyframeTrack
 */
interface QuaternionKeyframeTrackAPI : KeyframeTrackAPI {
    // Inherits all from KeyframeTrack, valueSize = 4
    // Uses spherical linear interpolation (SLERP)
}

/**
 * NumberKeyframeTrack: Scalar keyframes (opacity, morphTargetInfluences)
 * Three.js equivalent: THREE.NumberKeyframeTrack
 */
interface NumberKeyframeTrackAPI : KeyframeTrackAPI {
    // Inherits all from KeyframeTrack, valueSize = 1
}

/**
 * BooleanKeyframeTrack: Boolean keyframes (visibility)
 * Three.js equivalent: THREE.BooleanKeyframeTrack
 */
interface BooleanKeyframeTrackAPI : KeyframeTrackAPI {
    // Inherits all from KeyframeTrack, valueSize = 1
    // Uses discrete interpolation only
}

/**
 * StringKeyframeTrack: String keyframes (morph target names)
 * Three.js equivalent: THREE.StringKeyframeTrack
 */
interface StringKeyframeTrackAPI : KeyframeTrackAPI {
    // Inherits all from KeyframeTrack
    // Uses discrete interpolation only
}

/**
 * ColorKeyframeTrack: Color keyframes
 * Three.js equivalent: THREE.ColorKeyframeTrack
 */
interface ColorKeyframeTrackAPI : KeyframeTrackAPI {
    // Inherits all from KeyframeTrack, valueSize = 3 (RGB)
}

// ============================================================================
// Property Binding and Mixing
// ============================================================================

/**
 * PropertyBinding: Binds animation track to object property
 * Three.js equivalent: THREE.PropertyBinding
 */
interface PropertyBindingAPI {
    val path: String
    val parsedPath: ParsedPath
    val node: Any
    val rootNode: Any

    fun getValue(targetArray: FloatArray, offset: Int): FloatArray
    fun setValue(sourceArray: FloatArray, offset: Int)
    fun bind()
    fun unbind()

    companion object {
        fun create(root: Any, path: String, parsedPath: ParsedPath? = null): PropertyBinding
        fun sanitizeNodeName(name: String): String
        fun parseTrackName(trackName: String): ParsedPath
        fun findNode(root: Any, nodeName: String): Any?
    }
}

data class ParsedPath(
    val nodeName: String,
    val objectName: String,
    val objectIndex: String,
    val propertyName: String,
    val propertyIndex: String
)

/**
 * PropertyMixer: Mixes multiple property values
 * Three.js equivalent: THREE.PropertyMixer
 */
interface PropertyMixerAPI {
    val binding: PropertyBinding
    val buffer: FloatArray
    val cumulativeWeight: Float
    val cumulativeWeightAdditive: Float
    val useCount: Int
    val referenceCount: Int

    fun accumulate(accuIndex: Int, weight: Float)
    fun accumulateAdditive(weight: Float)
    fun apply(accuIndex: Int)
    fun saveOriginalState()
    fun restoreOriginalState()
}

// ============================================================================
// Interpolants
// ============================================================================

/**
 * Interpolant: Base interpolation interface
 * Three.js equivalent: THREE.Interpolant
 */
interface Interpolant {
    val parameterPositions: FloatArray
    val sampleValues: FloatArray
    val valueSize: Int
    val resultBuffer: FloatArray

    fun evaluate(t: Float): FloatArray
    fun copySampleValue_(index: Int): FloatArray
    fun interpolate_(i1: Int, t0: Float, t: Float, t1: Float): FloatArray
}

/**
 * DiscreteInterpolant: No interpolation (stepped)
 * Three.js equivalent: THREE.DiscreteInterpolant
 */
interface DiscreteInterpolant : Interpolant {
    // Step function - returns value at nearest keyframe
}

/**
 * LinearInterpolant: Linear interpolation
 * Three.js equivalent: THREE.LinearInterpolant
 */
interface LinearInterpolant : Interpolant {
    // Linear interpolation between keyframes
}

/**
 * CubicInterpolant: Cubic interpolation with tangents
 * Three.js equivalent: THREE.CubicInterpolant
 */
interface CubicInterpolant : Interpolant {
    // Cubic hermite interpolation
}

/**
 * QuaternionLinearInterpolant: SLERP for quaternions
 * Three.js equivalent: THREE.QuaternionLinearInterpolant
 */
interface QuaternionLinearInterpolant : Interpolant {
    // Spherical linear interpolation
}

// ============================================================================
// Supporting Types
// ============================================================================

enum class InterpolationMode {
    InterpolateDiscrete,
    InterpolateLinear,
    InterpolateSmooth
}

enum class AnimationActionLoopStyles {
    LoopOnce,
    LoopRepeat,
    LoopPingPong
}

enum class AnimationBlendMode {
    NormalAnimationBlendMode,
    AdditiveAnimationBlendMode
}

data class MorphTarget(
    val name: String,
    val vertices: List<Vector3>
)

data class Bone(
    val name: String,
    val parent: Int,
    val pos: Vector3,
    val rotq: Quaternion,
    val scl: Vector3
)

data class Event(
    val type: String,
    val action: AnimationAction? = null,
    val loopDelta: Int? = null
)

// ============================================================================
// Factory Functions (Kotlin Idiom)
// ============================================================================

/**
 * DSL builder for AnimationClip
 */
fun animationClip(
    name: String,
    duration: Float = -1f,
    tracks: List<KeyframeTrack> = emptyList(),
    init: AnimationClip.() -> Unit = {}
): AnimationClip {
    val clip = AnimationClip(name, duration, tracks)
    clip.init()
    return clip
}

/**
 * DSL builder for VectorKeyframeTrack
 */
fun vectorTrack(
    name: String,
    times: FloatArray,
    values: FloatArray,
    interpolation: InterpolationMode = InterpolationMode.InterpolateLinear
): VectorKeyframeTrack {
    return VectorKeyframeTrack(name, times, values, interpolation)
}

/**
 * DSL builder for QuaternionKeyframeTrack
 */
fun quaternionTrack(
    name: String,
    times: FloatArray,
    values: FloatArray,
    interpolation: InterpolationMode = InterpolationMode.InterpolateLinear
): QuaternionKeyframeTrack {
    return QuaternionKeyframeTrack(name, times, values, interpolation)
}

/**
 * DSL builder for NumberKeyframeTrack
 */
fun numberTrack(
    name: String,
    times: FloatArray,
    values: FloatArray,
    interpolation: InterpolationMode = InterpolationMode.InterpolateLinear
): NumberKeyframeTrack {
    return NumberKeyframeTrack(name, times, values, interpolation)
}

/**
 * Extension functions for AnimationMixer
 */
fun AnimationMixer.playClip(clip: AnimationClip): AnimationAction {
    return clipAction(clip).play()
}

fun AnimationMixer.stopClip(clip: AnimationClip) {
    existingAction(clip)?.stop()
}

fun AnimationMixer.crossFade(
    fromClip: AnimationClip,
    toClip: AnimationClip,
    duration: Float
) {
    val fromAction = existingAction(fromClip)
    val toAction = clipAction(toClip)
    fromAction?.crossFadeTo(toAction, duration)
}

// ============================================================================
// Usage Examples
// ============================================================================

/**
 * Example: Create position animation
 */
fun examplePositionAnimation(): AnimationClip {
    val times = floatArrayOf(0f, 1f, 2f)
    val values = floatArrayOf(
        0f, 0f, 0f,  // t=0: position (0,0,0)
        1f, 1f, 0f,  // t=1: position (1,1,0)
        0f, 0f, 0f   // t=2: position (0,0,0)
    )

    return animationClip(
        name = "bounce",
        duration = 2f,
        tracks = listOf(
            vectorTrack(".position", times, values)
        )
    )
}

/**
 * Example: Create rotation animation
 */
fun exampleRotationAnimation(): AnimationClip {
    val times = floatArrayOf(0f, 1f, 2f)

    // Quaternions for 0, 90, 180 degree rotations around Y
    val q1 = Quaternion().setFromAxisAngle(Vector3(0f, 1f, 0f), 0f)
    val q2 = Quaternion().setFromAxisAngle(Vector3(0f, 1f, 0f), Math.PI.toFloat() / 2f)
    val q3 = Quaternion().setFromAxisAngle(Vector3(0f, 1f, 0f), Math.PI.toFloat())

    val values = floatArrayOf(
        q1.x, q1.y, q1.z, q1.w,
        q2.x, q2.y, q2.z, q2.w,
        q3.x, q3.y, q3.z, q3.w
    )

    return animationClip(
        name = "rotate",
        duration = 2f,
        tracks = listOf(
            quaternionTrack(".quaternion", times, values)
        )
    )
}

/**
 * Example: Setup animation mixer
 */
fun exampleAnimationSetup(mesh: Object3D, clip: AnimationClip): AnimationMixer {
    val mixer = AnimationMixer(mesh)
    val action = mixer.clipAction(clip)

    action.apply {
        loop = AnimationActionLoopStyles.LoopRepeat
        timeScale = 1f
        weight = 1f
        play()
    }

    return mixer
}

/**
 * Example: Crossfade between animations
 */
fun exampleCrossfade(
    mixer: AnimationMixer,
    currentClip: AnimationClip,
    nextClip: AnimationClip
) {
    val currentAction = mixer.existingAction(currentClip)!!
    val nextAction = mixer.clipAction(nextClip)

    nextAction.apply {
        enabled = true
        time = 0f
        setEffectiveWeight(1f)
        setEffectiveTimeScale(1f)
    }

    currentAction.crossFadeTo(nextAction, duration = 0.5f, warp = false)
}

/**
 * Example: Update animation in render loop
 */
fun exampleAnimationLoop(mixer: AnimationMixer, deltaTime: Float) {
    mixer.update(deltaTime)
}
