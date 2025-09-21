package io.kreekt.animation

import io.kreekt.core.math.Matrix4
import io.kreekt.core.math.Vector3
import io.kreekt.core.math.Quaternion

/**
 * Skeletal animation system interface for bone-based animations
 * Handles bone transformations, skinning, and inverse kinematics
 */
interface SkeletalAnimationSystem {
    /**
     * The skeleton being animated
     */
    val skeleton: Skeleton

    /**
     * Whether this system has been disposed
     */
    val isDisposed: Boolean

    /**
     * Plays a skeletal animation clip
     * @param clip The animation clip to play
     * @param weight Weight of this animation in blending
     * @param loop Whether to loop the animation
     * @return Animation action for controlling playback
     */
    fun playAnimation(clip: AnimationClip, weight: Float = 1f, loop: Boolean = true): AnimationAction

    /**
     * Stops all skeletal animations
     */
    fun stopAllAnimations()

    /**
     * Sets bone transform directly
     * @param boneIndex Index of the bone
     * @param transform Transform matrix to apply
     */
    fun setBoneTransform(boneIndex: Int, transform: Matrix4)

    /**
     * Gets current bone transform
     * @param boneIndex Index of the bone
     * @return Current transform matrix of the bone
     */
    fun getBoneTransform(boneIndex: Int): Matrix4

    /**
     * Updates the skeletal animation system
     * @param deltaTime Time elapsed since last update in seconds
     */
    fun update(deltaTime: Float)

    /**
     * Disposes of system resources
     */
    fun dispose()
}

/**
 * Default implementation of SkeletalAnimationSystem
 */
class DefaultSkeletalAnimationSystem(
    override val skeleton: Skeleton
) : SkeletalAnimationSystem {

    private var _isDisposed = false
    private val activeAnimations = mutableListOf<SkeletalAnimationAction>()
    private val boneTransforms = Array(skeleton.bones.size) { Matrix4.identity() }
    private val finalBoneMatrices = Array(skeleton.bones.size) { Matrix4.identity() }

    override val isDisposed: Boolean get() = _isDisposed

    override fun playAnimation(clip: AnimationClip, weight: Float, loop: Boolean): AnimationAction {
        val action = SkeletalAnimationAction(clip, this, weight, loop)
        activeAnimations.add(action)
        action.play()
        return action
    }

    override fun stopAllAnimations() {
        activeAnimations.forEach { it.stop() }
        activeAnimations.clear()
    }

    override fun setBoneTransform(boneIndex: Int, transform: Matrix4) {
        if (boneIndex in boneTransforms.indices) {
            boneTransforms[boneIndex] = transform.copy()
        }
    }

    override fun getBoneTransform(boneIndex: Int): Matrix4 {
        return if (boneIndex in boneTransforms.indices) {
            boneTransforms[boneIndex].copy()
        } else {
            Matrix4.identity()
        }
    }

    override fun update(deltaTime: Float) {
        if (_isDisposed) return

        // Reset bone transforms to bind pose
        resetToBindPose()

        // Update and apply all active animations
        activeAnimations.removeAll { animation ->
            if (!animation.isRunning) {
                true // Remove finished animations
            } else {
                animation.update(deltaTime)
                applyAnimationToBones(animation)
                false
            }
        }

        // Calculate final bone matrices for rendering
        calculateFinalBoneMatrices()
    }

    private fun resetToBindPose() {
        skeleton.bones.forEachIndexed { index, bone ->
            boneTransforms[index] = bone.bindTransform.copy()
        }
    }

    private fun applyAnimationToBones(animation: SkeletalAnimationAction) {
        val weight = animation.weight
        if (weight <= 0f) return

        animation.clip.tracks.forEach { track ->
            val boneIndex = findBoneIndex(track.name)
            if (boneIndex >= 0) {
                val animatedValue = interpolateTrack(track, animation.time)
                val animatedTransform = createTransformFromValues(animatedValue)

                // Blend with existing transform
                boneTransforms[boneIndex] = blendTransforms(
                    boneTransforms[boneIndex],
                    animatedTransform,
                    weight
                )
            }
        }
    }

    private fun findBoneIndex(boneName: String): Int {
        return skeleton.bones.indexOfFirst { it.name == boneName }
    }

    private fun interpolateTrack(track: KeyframeTrack, time: Float): FloatArray {
        // Similar to AnimationAction interpolation but simplified
        val times = track.times
        val values = track.values
        val valuesPerKey = values.size / times.size

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
            val startIdx = index * valuesPerKey
            return values.sliceArray(startIdx until startIdx + valuesPerKey)
        }

        val t1 = times[index]
        val t2 = times[nextIndex]
        val alpha = ((time - t1) / (t2 - t1)).coerceIn(0f, 1f)

        val startIdx1 = index * valuesPerKey
        val startIdx2 = nextIndex * valuesPerKey

        val result = FloatArray(valuesPerKey)
        for (i in 0 until valuesPerKey) {
            val v1 = values[startIdx1 + i]
            val v2 = values[startIdx2 + i]
            result[i] = v1 + (v2 - v1) * alpha
        }

        return result
    }

    private fun createTransformFromValues(values: FloatArray): Matrix4 {
        // Assumes values contain [px, py, pz, qx, qy, qz, qw, sx, sy, sz]
        // Position, Quaternion rotation, Scale
        when (values.size) {
            10 -> {
                val position = Vector3(values[0], values[1], values[2])
                val rotation = Quaternion(values[3], values[4], values[5], values[6])
                val scale = Vector3(values[7], values[8], values[9])
                return Matrix4().compose(position, rotation, scale)
            }
            7 -> {
                // Position + Quaternion only
                val position = Vector3(values[0], values[1], values[2])
                val rotation = Quaternion(values[3], values[4], values[5], values[6])
                return Matrix4().compose(position, rotation, Vector3.ONE)
            }
            3 -> {
                // Position only
                val position = Vector3(values[0], values[1], values[2])
                return Matrix4().compose(position, Quaternion.IDENTITY, Vector3.ONE)
            }
            else -> return Matrix4.identity()
        }
    }

    private fun blendTransforms(transform1: Matrix4, transform2: Matrix4, weight: Float): Matrix4 {
        if (weight <= 0f) return transform1
        if (weight >= 1f) return transform2

        // Decompose both transforms
        val pos1 = transform1.getTranslation()
        val rot1 = transform1.getRotation()
        val scale1 = transform1.getScale()

        val pos2 = transform2.getTranslation()
        val rot2 = transform2.getRotation()
        val scale2 = transform2.getScale()

        // Interpolate components
        val blendedPos = pos1.lerp(pos2, weight)
        val blendedRot = rot1.slerp(rot2, weight)
        val blendedScale = scale1.lerp(scale2, weight)

        return Matrix4().compose(blendedPos, blendedRot, blendedScale)
    }

    private fun calculateFinalBoneMatrices() {
        skeleton.bones.forEachIndexed { index, bone ->
            val worldMatrix = calculateBoneWorldMatrix(index)
            finalBoneMatrices[index] = worldMatrix * bone.inverseBindMatrix
        }
    }

    private fun calculateBoneWorldMatrix(boneIndex: Int): Matrix4 {
        val bone = skeleton.bones[boneIndex]
        val localMatrix = boneTransforms[boneIndex]

        return if (bone.parentIndex >= 0) {
            calculateBoneWorldMatrix(bone.parentIndex) * localMatrix
        } else {
            localMatrix
        }
    }

    /**
     * Gets the final bone matrices for rendering (bone space to world space)
     */
    fun getFinalBoneMatrices(): Array<Matrix4> {
        return finalBoneMatrices.copyOf()
    }

    override fun dispose() {
        if (_isDisposed) return

        stopAllAnimations()
        _isDisposed = true
    }
}

/**
 * Skeletal animation action for controlling bone-based animation playback
 */
private class SkeletalAnimationAction(
    override val clip: AnimationClip,
    private val system: DefaultSkeletalAnimationSystem,
    initialWeight: Float,
    initialLoop: Boolean
) : AnimationAction {

    override var time: Float = 0f
    override var timeScale: Float = 1f
    override var weight: Float = initialWeight
    override var loop: Boolean = initialLoop

    private var _isRunning = false
    private var _isPaused = false

    override val isRunning: Boolean get() = _isRunning && !_isPaused
    override val isPaused: Boolean get() = _isPaused

    override fun play(): AnimationAction {
        _isRunning = true
        _isPaused = false
        return this
    }

    override fun stop(): AnimationAction {
        _isRunning = false
        _isPaused = false
        time = 0f
        return this
    }

    override fun pause(): AnimationAction {
        _isPaused = true
        return this
    }

    override fun resume(): AnimationAction {
        _isPaused = false
        return this
    }

    override fun fadeIn(duration: Float): AnimationAction {
        // TODO: Implement fading
        weight = 1f
        return play()
    }

    override fun fadeOut(duration: Float): AnimationAction {
        // TODO: Implement fading
        weight = 0f
        return this
    }

    override fun crossFadeTo(toAction: AnimationAction, duration: Float): AnimationAction {
        // TODO: Implement cross-fading
        fadeOut(duration)
        toAction.fadeIn(duration)
        return this
    }

    override fun update(deltaTime: Float) {
        if (!_isRunning || _isPaused) return

        time += deltaTime * timeScale

        if (loop && time > clip.duration) {
            time = time % clip.duration
        } else if (!loop && time > clip.duration) {
            time = clip.duration
            stop()
        }
    }

    override fun dispose() {
        stop()
    }
}