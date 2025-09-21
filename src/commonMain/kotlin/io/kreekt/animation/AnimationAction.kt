package io.kreekt.animation

/**
 * Animation action interface for controlling individual animation playback
 * Provides play, pause, stop, and fade functionality
 */
interface AnimationAction {
    /**
     * The animation clip being played
     */
    val clip: AnimationClip

    /**
     * Whether this action is currently running
     */
    val isRunning: Boolean

    /**
     * Whether this action is currently paused
     */
    val isPaused: Boolean

    /**
     * Current playback time in seconds
     */
    var time: Float

    /**
     * Time scale multiplier for playback speed
     */
    var timeScale: Float

    /**
     * Weight of this action in blending (0.0 to 1.0)
     */
    var weight: Float

    /**
     * Whether the animation should loop
     */
    var loop: Boolean

    /**
     * Starts playing the animation
     * @return This action for chaining
     */
    fun play(): AnimationAction

    /**
     * Stops the animation and resets to beginning
     * @return This action for chaining
     */
    fun stop(): AnimationAction

    /**
     * Pauses the animation at current time
     * @return This action for chaining
     */
    fun pause(): AnimationAction

    /**
     * Resumes the animation from current time
     * @return This action for chaining
     */
    fun resume(): AnimationAction

    /**
     * Fades in the animation over the specified duration
     * @param duration Fade duration in seconds
     * @return This action for chaining
     */
    fun fadeIn(duration: Float): AnimationAction

    /**
     * Fades out the animation over the specified duration
     * @param duration Fade duration in seconds
     * @return This action for chaining
     */
    fun fadeOut(duration: Float): AnimationAction

    /**
     * Cross-fades from this action to another action
     * @param toAction The action to fade to
     * @param duration Cross-fade duration in seconds
     * @return This action for chaining
     */
    fun crossFadeTo(toAction: AnimationAction, duration: Float): AnimationAction

    /**
     * Updates the action with the given delta time
     * @param deltaTime Time elapsed since last update in seconds
     */
    fun update(deltaTime: Float)

    /**
     * Disposes of action resources
     */
    fun dispose()
}

/**
 * Clip action implementation for controlling animation clip playback
 */
interface ClipAction : AnimationAction {
    /**
     * The mixer this action belongs to
     */
    val mixer: AnimationMixer

    /**
     * Whether this action has been disposed
     */
    val isDisposed: Boolean
}

/**
 * Default implementation of ClipAction
 */
class DefaultClipAction(
    override val clip: AnimationClip,
    override val mixer: AnimationMixer
) : ClipAction {

    private var _isRunning = false
    private var _isPaused = false
    private var _isDisposed = false

    override val isRunning: Boolean get() = _isRunning && !_isPaused
    override val isPaused: Boolean get() = _isPaused
    override val isDisposed: Boolean get() = _isDisposed

    override var time: Float = 0f
    override var timeScale: Float = 1f
    override var weight: Float = 1f
    override var loop: Boolean = true

    private var fadeDirection = 0f // -1 for fade out, 1 for fade in, 0 for no fade
    private var fadeDuration = 0f
    private var fadeTime = 0f

    override fun play(): AnimationAction {
        if (_isDisposed) return this

        _isRunning = true
        _isPaused = false
        return this
    }

    override fun stop(): AnimationAction {
        if (_isDisposed) return this

        _isRunning = false
        _isPaused = false
        time = 0f
        weight = 1f
        fadeDirection = 0f
        return this
    }

    override fun pause(): AnimationAction {
        if (_isDisposed) return this

        _isPaused = true
        return this
    }

    override fun resume(): AnimationAction {
        if (_isDisposed) return this

        _isPaused = false
        return this
    }

    override fun fadeIn(duration: Float): AnimationAction {
        if (_isDisposed) return this

        startFade(1f, duration)
        play()
        return this
    }

    override fun fadeOut(duration: Float): AnimationAction {
        if (_isDisposed) return this

        startFade(-1f, duration)
        return this
    }

    override fun crossFadeTo(toAction: AnimationAction, duration: Float): AnimationAction {
        if (_isDisposed) return this

        fadeOut(duration)
        toAction.fadeIn(duration)
        return this
    }

    private fun startFade(direction: Float, duration: Float) {
        fadeDirection = direction
        fadeDuration = duration
        fadeTime = 0f

        if (direction > 0) {
            weight = 0f
        }
    }

    override fun update(deltaTime: Float) {
        if (_isDisposed || !_isRunning || _isPaused) return

        // Update animation time
        time += deltaTime * timeScale

        // Handle looping
        if (loop && time > clip.duration) {
            time = time % clip.duration
        } else if (!loop && time > clip.duration) {
            time = clip.duration
            stop()
            return
        }

        // Update fading
        if (fadeDirection != 0f) {
            fadeTime += deltaTime
            val fadeProgress = (fadeTime / fadeDuration).coerceIn(0f, 1f)

            weight = when {
                fadeDirection > 0 -> fadeProgress // Fade in
                else -> 1f - fadeProgress // Fade out
            }

            if (fadeProgress >= 1f) {
                fadeDirection = 0f
                if (weight <= 0f) {
                    stop()
                }
            }
        }

        // Apply animation to the target object
        applyAnimation()
    }

    private fun applyAnimation() {
        if (weight <= 0f) return

        // Apply animation tracks to the target object
        clip.tracks.forEach { track ->
            val value = interpolateTrack(track, time)
            applyTrackValue(track.name, value)
        }
    }

    private fun interpolateTrack(track: KeyframeTrack, time: Float): FloatArray {
        val times = track.times
        val values = track.values
        val valuesPerKey = values.size / times.size

        // Find the keyframe indices
        var index = 0
        for (i in times.indices) {
            if (time >= times[i]) {
                index = i
            } else {
                break
            }
        }

        val nextIndex = (index + 1).coerceAtMost(times.lastIndex)

        if (index == nextIndex) {
            // Return the exact value
            val startIdx = index * valuesPerKey
            return values.sliceArray(startIdx until startIdx + valuesPerKey)
        }

        // Interpolate between keyframes
        val t1 = times[index]
        val t2 = times[nextIndex]
        val alpha = ((time - t1) / (t2 - t1)).coerceIn(0f, 1f)

        val startIdx1 = index * valuesPerKey
        val startIdx2 = nextIndex * valuesPerKey

        val result = FloatArray(valuesPerKey)
        for (i in 0 until valuesPerKey) {
            val v1 = values[startIdx1 + i]
            val v2 = values[startIdx2 + i]
            result[i] = when (track.interpolation) {
                InterpolationType.LINEAR -> v1 + (v2 - v1) * alpha
                InterpolationType.STEP -> v1
                InterpolationType.CUBIC_SPLINE -> {
                    // Simplified cubic interpolation
                    v1 + (v2 - v1) * alpha * alpha * (3f - 2f * alpha)
                }
            }
        }

        return result
    }

    private fun applyTrackValue(trackName: String, value: FloatArray) {
        // Apply the animated value to the target object
        // This would be implemented based on the specific property being animated
        // For now, this is a placeholder that would need to integrate with the scene graph

        // Example implementation:
        // when (trackName) {
        //     "position" -> mixer.root.position.set(value[0], value[1], value[2])
        //     "rotation" -> mixer.root.rotation.set(value[0], value[1], value[2], value[3])
        //     "scale" -> mixer.root.scale.set(value[0], value[1], value[2])
        // }
    }

    override fun dispose() {
        if (_isDisposed) return

        stop()
        _isDisposed = true
    }
}