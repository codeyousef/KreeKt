package io.kreekt.animation

import io.kreekt.core.scene.Object3D

/**
 * Animation mixer interface for managing multiple animation clips on a single object
 * Handles blending, timing, and playback of animation sequences
 */
interface AnimationMixer {
    /**
     * The root object being animated
     */
    val root: Object3D

    /**
     * Whether this mixer has been disposed
     */
    val isDisposed: Boolean

    /**
     * Creates a clip action for the given animation clip
     * @param clip The animation clip to create an action for
     * @return ClipAction instance for controlling the clip playback
     */
    fun clipAction(clip: AnimationClip): ClipAction

    /**
     * Stops all active actions
     */
    fun stopAllAction()

    /**
     * Updates the mixer with the given delta time
     * @param deltaTime Time elapsed since last update in seconds
     */
    fun update(deltaTime: Float)

    /**
     * Disposes of mixer resources
     */
    fun dispose()
}

/**
 * Default implementation of AnimationMixer
 */
class DefaultAnimationMixer(override val root: Object3D) : AnimationMixer {
    private val actions = mutableMapOf<AnimationClip, ClipAction>()
    private var _isDisposed = false

    override val isDisposed: Boolean
        get() = _isDisposed

    override fun clipAction(clip: AnimationClip): ClipAction {
        return actions.getOrPut(clip) {
            DefaultClipAction(clip, this)
        }
    }

    override fun stopAllAction() {
        actions.values.forEach { it.stop() }
    }

    override fun update(deltaTime: Float) {
        if (_isDisposed) return

        // Update all active actions
        actions.values.forEach { action ->
            if (action.isRunning) {
                action.update(deltaTime)
            }
        }
    }

    override fun dispose() {
        if (_isDisposed) return

        stopAllAction()
        actions.values.forEach { it.dispose() }
        actions.clear()
        _isDisposed = true
    }
}

/**
 * Animation clip data structure
 * Contains keyframe data and timing information
 */
data class AnimationClip(
    val name: String,
    val duration: Float,
    val tracks: List<KeyframeTrack>
)

/**
 * Keyframe track for animating specific properties
 */
data class KeyframeTrack(
    val name: String,
    val times: FloatArray,
    val values: FloatArray,
    val interpolation: InterpolationType = InterpolationType.LINEAR
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KeyframeTrack

        if (name != other.name) return false
        if (!times.contentEquals(other.times)) return false
        if (!values.contentEquals(other.values)) return false
        if (interpolation != other.interpolation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + times.contentHashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + interpolation.hashCode()
        return result
    }
}

/**
 * Animation interpolation types
 */
enum class InterpolationType {
    LINEAR,
    STEP,
    CUBIC_SPLINE
}