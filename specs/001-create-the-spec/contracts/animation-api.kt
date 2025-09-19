/**
 * Animation System API Contract
 * Defines interfaces for 3D animation and interpolation
 */
package kreekt.animation

import kreekt.core.*
import kreekt.scene.*

/**
 * Animation mixer for managing multiple animations on objects
 */
interface AnimationMixer {
    val root: Object3D
    val time: Float
    val timeScale: Float

    /**
     * Create an action for the given clip
     */
    fun clipAction(clip: AnimationClip): AnimationAction

    /**
     * Update all animations by delta time
     */
    fun update(deltaTime: Float)

    /**
     * Stop all animations
     */
    fun stopAllAction()

    /**
     * Get all active actions
     */
    fun getActions(): List<AnimationAction>

    /**
     * Remove an existing action
     */
    fun uncacheClip(clip: AnimationClip)
    fun uncacheRoot(root: Object3D)
    fun uncacheAction(clip: AnimationClip, root: Object3D)
}

/**
 * Animation action controls playback of a single clip
 */
interface AnimationAction {
    val clip: AnimationClip
    val mixer: AnimationMixer
    var enabled: Boolean
    var paused: Boolean
    var loop: LoopMode
    var time: Float
    var timeScale: Float
    var weight: Float
    var repetitions: Float
    var clampWhenFinished: Boolean
    var zeroSlopeAtStart: Boolean
    var zeroSlopeAtEnd: Boolean

    /**
     * Play the animation
     */
    fun play(): AnimationAction

    /**
     * Stop the animation
     */
    fun stop(): AnimationAction

    /**
     * Pause the animation
     */
    fun pause(): AnimationAction

    /**
     * Reset the animation to the beginning
     */
    fun reset(): AnimationAction

    /**
     * Set animation to a specific time
     */
    fun setTime(time: Float): AnimationAction

    /**
     * Fade in over duration
     */
    fun fadeIn(duration: Float): AnimationAction

    /**
     * Fade out over duration
     */
    fun fadeOut(duration: Float): AnimationAction

    /**
     * Cross-fade to another action
     */
    fun crossFadeTo(action: AnimationAction, duration: Float, warp: Boolean = false): AnimationAction

    /**
     * Cross-fade from another action
     */
    fun crossFadeFrom(action: AnimationAction, duration: Float, warp: Boolean = false): AnimationAction

    /**
     * Set effective weight
     */
    fun setEffectiveWeight(weight: Float): AnimationAction

    /**
     * Set effective time scale
     */
    fun setEffectiveTimeScale(timeScale: Float): AnimationAction

    /**
     * Get effective weight (considers fading)
     */
    fun getEffectiveWeight(): Float

    /**
     * Get effective time scale
     */
    fun getEffectiveTimeScale(): Float

    /**
     * Check if action is running
     */
    fun isRunning(): Boolean

    /**
     * Check if action is scheduled to start
     */
    fun isScheduled(): Boolean
}

/**
 * Animation loop modes
 */
enum class LoopMode {
    ONCE,           // Play once and stop
    REPEAT,         // Loop continuously
    PING_PONG       // Play forward, then backward, repeat
}

/**
 * Animation clip data structure
 */
data class AnimationClip(
    val name: String,
    val duration: Float,
    val tracks: List<KeyframeTrack>,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val uuid: String = generateUUID()
) {
    /**
     * Optimize the clip by removing redundant keyframes
     */
    fun optimize(): AnimationClip

    /**
     * Reset the clip duration based on tracks
     */
    fun resetDuration(): AnimationClip

    /**
     * Trim the clip to the specified time range
     */
    fun trim(startTime: Float, endTime: Float): AnimationClip

    /**
     * Create a copy of this clip
     */
    fun clone(): AnimationClip
}

/**
 * Blend modes for animation mixing
 */
enum class BlendMode {
    NORMAL,
    ADDITIVE
}

/**
 * Base class for keyframe tracks
 */
abstract class KeyframeTrack(
    val name: String,
    val times: FloatArray,
    val values: FloatArray,
    val interpolation: InterpolationMode = InterpolationMode.LINEAR
) {
    val valueSize: Int
        get() = values.size / times.size

    /**
     * Interpolate value at given time
     */
    abstract fun interpolate(time: Float, result: FloatArray): FloatArray

    /**
     * Get value at specific time index
     */
    abstract fun getValue(index: Int, result: FloatArray): FloatArray

    /**
     * Optimize track by removing redundant keyframes
     */
    abstract fun optimize(): KeyframeTrack

    /**
     * Create a copy of this track
     */
    abstract fun clone(): KeyframeTrack

    /**
     * Shift all keyframes by offset
     */
    fun shift(timeOffset: Float): KeyframeTrack

    /**
     * Scale all keyframes by factor
     */
    fun scale(timeScale: Float): KeyframeTrack

    /**
     * Trim track to time range
     */
    fun trim(startTime: Float, endTime: Float): KeyframeTrack
}

/**
 * Interpolation modes for keyframes
 */
enum class InterpolationMode {
    DISCRETE,       // No interpolation (step)
    LINEAR,         // Linear interpolation
    CUBIC           // Cubic spline interpolation
}

/**
 * Vector keyframe track for position, scale, etc.
 */
class VectorKeyframeTrack(
    name: String,
    times: FloatArray,
    values: FloatArray,
    interpolation: InterpolationMode = InterpolationMode.LINEAR
) : KeyframeTrack(name, times, values, interpolation) {

    override fun interpolate(time: Float, result: FloatArray): FloatArray {
        // Implementation handles Vector3 interpolation
        return result
    }

    override fun getValue(index: Int, result: FloatArray): FloatArray {
        val offset = index * 3
        result[0] = values[offset]
        result[1] = values[offset + 1]
        result[2] = values[offset + 2]
        return result
    }

    override fun optimize(): VectorKeyframeTrack {
        // Remove consecutive duplicate keyframes
        return this
    }

    override fun clone(): VectorKeyframeTrack {
        return VectorKeyframeTrack(name, times.clone(), values.clone(), interpolation)
    }
}

/**
 * Quaternion keyframe track for rotations
 */
class QuaternionKeyframeTrack(
    name: String,
    times: FloatArray,
    values: FloatArray,
    interpolation: InterpolationMode = InterpolationMode.LINEAR
) : KeyframeTrack(name, times, values, interpolation) {

    override fun interpolate(time: Float, result: FloatArray): FloatArray {
        // Implementation handles Quaternion SLERP
        return result
    }

    override fun getValue(index: Int, result: FloatArray): FloatArray {
        val offset = index * 4
        result[0] = values[offset]     // x
        result[1] = values[offset + 1] // y
        result[2] = values[offset + 2] // z
        result[3] = values[offset + 3] // w
        return result
    }

    override fun optimize(): QuaternionKeyframeTrack {
        return this
    }

    override fun clone(): QuaternionKeyframeTrack {
        return QuaternionKeyframeTrack(name, times.clone(), values.clone(), interpolation)
    }
}

/**
 * Color keyframe track for color animations
 */
class ColorKeyframeTrack(
    name: String,
    times: FloatArray,
    values: FloatArray,
    interpolation: InterpolationMode = InterpolationMode.LINEAR
) : KeyframeTrack(name, times, values, interpolation) {

    override fun interpolate(time: Float, result: FloatArray): FloatArray {
        // Implementation handles Color interpolation
        return result
    }

    override fun getValue(index: Int, result: FloatArray): FloatArray {
        val offset = index * 3
        result[0] = values[offset]     // r
        result[1] = values[offset + 1] // g
        result[2] = values[offset + 2] // b
        return result
    }

    override fun optimize(): ColorKeyframeTrack {
        return this
    }

    override fun clone(): ColorKeyframeTrack {
        return ColorKeyframeTrack(name, times.clone(), values.clone(), interpolation)
    }
}

/**
 * Number keyframe track for scalar values
 */
class NumberKeyframeTrack(
    name: String,
    times: FloatArray,
    values: FloatArray,
    interpolation: InterpolationMode = InterpolationMode.LINEAR
) : KeyframeTrack(name, times, values, interpolation) {

    override fun interpolate(time: Float, result: FloatArray): FloatArray {
        // Implementation handles scalar interpolation
        return result
    }

    override fun getValue(index: Int, result: FloatArray): FloatArray {
        result[0] = values[index]
        return result
    }

    override fun optimize(): NumberKeyframeTrack {
        return this
    }

    override fun clone(): NumberKeyframeTrack {
        return NumberKeyframeTrack(name, times.clone(), values.clone(), interpolation)
    }
}

/**
 * Animation utilities and helpers
 */
object AnimationUtils {
    /**
     * Convert array to specific track type
     */
    fun arrayToTrack(
        name: String,
        times: FloatArray,
        values: FloatArray,
        interpolation: InterpolationMode = InterpolationMode.LINEAR
    ): KeyframeTrack {
        val valueSize = values.size / times.size
        return when (valueSize) {
            1 -> NumberKeyframeTrack(name, times, values, interpolation)
            3 -> when {
                name.contains("rotation") -> {
                    // Convert Euler to Quaternion
                    VectorKeyframeTrack(name, times, values, interpolation)
                }
                name.contains("color") -> ColorKeyframeTrack(name, times, values, interpolation)
                else -> VectorKeyframeTrack(name, times, values, interpolation)
            }
            4 -> QuaternionKeyframeTrack(name, times, values, interpolation)
            else -> throw IllegalArgumentException("Unsupported value size: $valueSize")
        }
    }

    /**
     * Find keyframe index for given time
     */
    fun findKeyframeIndex(times: FloatArray, time: Float): Int {
        // Binary search implementation
        var left = 0
        var right = times.size - 1

        while (left <= right) {
            val mid = (left + right) / 2
            when {
                times[mid] < time -> left = mid + 1
                times[mid] > time -> right = mid - 1
                else -> return mid
            }
        }

        return right.coerceAtLeast(0)
    }

    /**
     * Linear interpolation between two values
     */
    fun lerp(a: Float, b: Float, t: Float): Float {
        return a + t * (b - a)
    }

    /**
     * Smooth step interpolation
     */
    fun smoothstep(a: Float, b: Float, t: Float): Float {
        val x = ((t - a) / (b - a)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    /**
     * Smoothest step interpolation
     */
    fun smootherstep(a: Float, b: Float, t: Float): Float {
        val x = ((t - a) / (b - a)).coerceIn(0f, 1f)
        return x * x * x * (x * (x * 6f - 15f) + 10f)
    }
}