package io.kreekt.morph

import io.kreekt.geometry.BufferGeometry
import io.kreekt.geometry.BufferAttribute
import io.kreekt.animation.AnimationClip
import io.kreekt.animation.KeyframeTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Contract test for Morph animation mixer - T032
 * Covers: FR-M006, FR-M007, FR-M008 from contracts/morph-api.kt
 */
class MorphAnimationContractTest {

    @Test
    fun testAnimateInfluencesOverTime() {
        // FR-M006: Animate influences over time
        val geometry = createAnimatedMorphGeometry()
        val mixer = MorphAnimationMixer(geometry)

        // Create animation clip for morph influences
        val clip = AnimationClip(
            name = "morphAnimation",
            duration = 2.0f,
            tracks = listOf(
                // Animate first morph target from 0 to 1 over 1 second
                MorphInfluenceTrack(
                    targetIndex = 0,
                    times = floatArrayOf(0f, 1f),
                    values = floatArrayOf(0f, 1f)
                ),
                // Animate second morph target from 0 to 1 to 0 over 2 seconds
                MorphInfluenceTrack(
                    targetIndex = 1,
                    times = floatArrayOf(0f, 1f, 2f),
                    values = floatArrayOf(0f, 1f, 0f)
                )
            )
        )

        // Play animation
        val action = mixer.clipAction(clip)
        action.play()

        // Test at t=0
        mixer.update(0f)
        assertEquals(0f, mixer.getInfluence(0), 0.01f)
        assertEquals(0f, mixer.getInfluence(1), 0.01f)

        // Test at t=0.5 (halfway through first second)
        mixer.update(0.5f)
        assertEquals(0.5f, mixer.getInfluence(0), 0.01f)
        assertEquals(0.5f, mixer.getInfluence(1), 0.01f)

        // Test at t=1.0
        mixer.update(0.5f) // Total time = 1.0
        assertEquals(1f, mixer.getInfluence(0), 0.01f)
        assertEquals(1f, mixer.getInfluence(1), 0.01f)

        // Test at t=1.5
        mixer.update(0.5f) // Total time = 1.5
        assertEquals(1f, mixer.getInfluence(0), 0.01f) // Stays at 1
        assertEquals(0.5f, mixer.getInfluence(1), 0.01f) // Animating back

        // Test at t=2.0
        mixer.update(0.5f) // Total time = 2.0
        assertEquals(1f, mixer.getInfluence(0), 0.01f)
        assertEquals(0f, mixer.getInfluence(1), 0.01f) // Back to 0
    }

    @Test
    fun testBlendMultipleAnimations() {
        // FR-M007: Blend multiple animations
        val geometry = createAnimatedMorphGeometry()
        val mixer = MorphAnimationMixer(geometry)

        // Animation 1: Smile
        val smileClip = AnimationClip(
            name = "smile",
            duration = 1.0f,
            tracks = listOf(
                MorphInfluenceTrack(
                    targetIndex = 0, // "smile" morph target
                    times = floatArrayOf(0f, 1f),
                    values = floatArrayOf(0f, 1f)
                )
            )
        )

        // Animation 2: Frown
        val frownClip = AnimationClip(
            name = "frown",
            duration = 1.0f,
            tracks = listOf(
                MorphInfluenceTrack(
                    targetIndex = 1, // "frown" morph target
                    times = floatArrayOf(0f, 1f),
                    values = floatArrayOf(0f, 1f)
                )
            )
        )

        // Play both animations with different weights
        val smileAction = mixer.clipAction(smileClip)
        val frownAction = mixer.clipAction(frownClip)

        smileAction.weight = 0.7f
        frownAction.weight = 0.3f

        smileAction.play()
        frownAction.play()

        // Update to halfway point
        mixer.update(0.5f)

        // Both should be partially active, weighted
        val smileInfluence = mixer.getInfluence(0)
        val frownInfluence = mixer.getInfluence(1)

        // Smile should be stronger (0.5 * 0.7 = 0.35)
        assertTrue(smileInfluence > 0.3f && smileInfluence < 0.4f)
        // Frown should be weaker (0.5 * 0.3 = 0.15)
        assertTrue(frownInfluence > 0.1f && frownInfluence < 0.2f)

        // Test crossfading between animations
        mixer.crossFade(smileAction, frownAction, duration = 1.0f)
        mixer.update(0.5f) // Halfway through crossfade

        // Weights should be transitioning
        assertTrue(smileAction.weight < 0.7f)
        assertTrue(frownAction.weight > 0.3f)

        mixer.update(0.5f) // Complete crossfade
        assertEquals(0f, smileAction.weight, 0.01f)
        assertEquals(1f, frownAction.weight, 0.01f)
    }

    @Test
    fun testShaderCodeGeneration() {
        // FR-M008: Shader code generation
        val geometry = createAnimatedMorphGeometry()
        val shaderGenerator = MorphShaderGenerator(geometry)

        // Generate vertex shader code for morph targets
        val vertexShaderCode = shaderGenerator.generateVertexShader()

        // Verify shader includes morph target uniforms
        assertTrue(vertexShaderCode.contains("morphTargetInfluences"))
        assertTrue(vertexShaderCode.contains("morphTarget0"))
        assertTrue(vertexShaderCode.contains("morphTarget1"))

        // Verify shader includes blending logic
        assertTrue(vertexShaderCode.contains("position += morphTarget"))
        assertTrue(vertexShaderCode.contains("* morphTargetInfluences"))

        // Test with normal morphing
        val normalMorphShader = shaderGenerator.generateVertexShader(includeNormals = true)
        assertTrue(normalMorphShader.contains("normal +="))
        assertTrue(normalMorphShader.contains("normalize"))

        // Test optimized shader for GPU
        val optimizedShader = shaderGenerator.generateOptimizedShader()
        assertTrue(optimizedShader.contains("texture2D")) // Using texture for morph data
        assertTrue(optimizedShader.contains("sampler2D morphTexture"))
    }

    @Test
    fun testAnimationLooping() {
        // Test loop modes for morph animations
        val geometry = createAnimatedMorphGeometry()
        val mixer = MorphAnimationMixer(geometry)

        val clip = AnimationClip(
            name = "loop",
            duration = 1.0f,
            tracks = listOf(
                MorphInfluenceTrack(
                    targetIndex = 0,
                    times = floatArrayOf(0f, 0.5f, 1f),
                    values = floatArrayOf(0f, 1f, 0f)
                )
            )
        )

        val action = mixer.clipAction(clip)

        // Test loop once
        action.loop = LoopMode.ONCE
        action.play()

        mixer.update(1.5f) // Past duration
        assertTrue(!action.isRunning)
        assertEquals(0f, mixer.getInfluence(0)) // Should stay at last value

        // Test loop repeat
        action.reset()
        action.loop = LoopMode.REPEAT
        action.play()

        mixer.update(2.5f) // 2.5 seconds = 2.5 loops
        assertTrue(action.isRunning)
        // Should be at 0.5 seconds into third loop
        assertEquals(1f, mixer.getInfluence(0), 0.01f)

        // Test ping pong
        action.reset()
        action.loop = LoopMode.PING_PONG
        action.play()

        mixer.update(0.5f) // Forward to peak
        assertEquals(1f, mixer.getInfluence(0), 0.01f)

        mixer.update(0.5f) // Back to start
        assertEquals(0f, mixer.getInfluence(0), 0.01f)

        mixer.update(0.5f) // Forward again
        assertEquals(1f, mixer.getInfluence(0), 0.01f)
    }

    @Test
    fun testAnimationEvents() {
        // Test animation event callbacks
        val geometry = createAnimatedMorphGeometry()
        val mixer = MorphAnimationMixer(geometry)

        val clip = AnimationClip(
            name = "events",
            duration = 1.0f,
            tracks = listOf(
                MorphInfluenceTrack(
                    targetIndex = 0,
                    times = floatArrayOf(0f, 1f),
                    values = floatArrayOf(0f, 1f)
                )
            )
        )

        val action = mixer.clipAction(clip)

        var startCalled = false
        var finishCalled = false
        var loopCalled = false

        action.onStart = { startCalled = true }
        action.onFinish = { finishCalled = true }
        action.onLoop = { loopCalled = true }

        // Test start event
        action.play()
        assertTrue(startCalled)

        // Test finish event
        action.loop = LoopMode.ONCE
        mixer.update(1.1f)
        assertTrue(finishCalled)

        // Test loop event
        action.reset()
        action.loop = LoopMode.REPEAT
        action.play()
        mixer.update(1.1f) // Trigger loop
        assertTrue(loopCalled)
    }

    @Test
    fun testMorphTargetLimits() {
        // Test GPU limits for morph targets
        val maxTargets = MorphShaderGenerator.getMaxMorphTargets()
        assertTrue(maxTargets >= 8) // Minimum WebGPU requirement

        val geometry = BufferGeometry()
        val positions = FloatArray(300) // 100 vertices
        geometry.setAttribute("position", BufferAttribute(positions, 3))

        // Create maximum number of morph targets
        val morphTargets = mutableListOf<MorphTarget>()
        for (i in 0 until maxTargets) {
            morphTargets.add(
                MorphTarget(
                    name = "target_$i",
                    position = BufferAttribute(FloatArray(300), 3)
                )
            )
        }
        geometry.morphTargets = morphTargets

        val shaderGen = MorphShaderGenerator(geometry)
        val shader = shaderGen.generateVertexShader()

        // Verify all targets are included
        for (i in 0 until maxTargets) {
            assertTrue(shader.contains("morphTarget$i"))
        }

        // Test exceeding limit
        morphTargets.add(
            MorphTarget(
                name = "extra",
                position = BufferAttribute(FloatArray(300), 3)
            )
        )
        geometry.morphTargets = morphTargets

        val limitedShader = shaderGen.generateVertexShader()
        // Should only include up to max targets
        assertTrue(!limitedShader.contains("morphTarget$maxTargets"))
    }

    @Test
    fun testMorphInfluenceOptimization() {
        // Test optimization of morph influence calculations
        val geometry = createAnimatedMorphGeometry()
        val mixer = MorphAnimationMixer(geometry)

        // Create animation with sparse keyframes
        val clip = AnimationClip(
            name = "sparse",
            duration = 10.0f,
            tracks = listOf(
                MorphInfluenceTrack(
                    targetIndex = 0,
                    times = floatArrayOf(0f, 5f, 10f),
                    values = floatArrayOf(0f, 1f, 0f),
                    interpolation = InterpolationMode.CUBIC
                )
            )
        )

        val action = mixer.clipAction(clip)
        action.play()

        // Test cubic interpolation smoothness
        val samples = 100
        var previousInfluence = 0f

        for (i in 0..samples) {
            val time = i * 10f / samples
            mixer.setTime(time)

            val influence = mixer.getInfluence(0)

            // Verify smooth interpolation
            if (i > 0) {
                val delta = kotlin.math.abs(influence - previousInfluence)
                assertTrue(delta < 0.2f) // No sudden jumps
            }

            previousInfluence = influence
        }
    }

    @Test
    fun testMultiMorphMixerPerformance() {
        // Test performance with multiple simultaneous morph animations
        val geometry = createComplexMorphGeometry(morphCount = 20)
        val mixer = MorphAnimationMixer(geometry)

        // Create 10 overlapping animations
        val clips = mutableListOf<AnimationClip>()
        for (i in 0 until 10) {
            val tracks = listOf(
                MorphInfluenceTrack(
                    targetIndex = i * 2,
                    times = floatArrayOf(0f, 1f + i * 0.1f),
                    values = floatArrayOf(0f, 1f)
                ),
                MorphInfluenceTrack(
                    targetIndex = i * 2 + 1,
                    times = floatArrayOf(0f, 0.5f + i * 0.1f, 1f + i * 0.1f),
                    values = floatArrayOf(0f, 1f, 0f)
                )
            )
            clips.add(AnimationClip("clip_$i", 2.0f, tracks))
        }

        // Play all animations
        val actions = clips.map { clip ->
            mixer.clipAction(clip).apply {
                weight = Math.random().toFloat()
                play()
            }
        }

        // Measure update performance
        val startTime = System.currentTimeMillis()
        val frames = 1000

        for (frame in 0 until frames) {
            mixer.update(1f / 60f) // 60 FPS timestep
        }

        val duration = System.currentTimeMillis() - startTime
        val avgFrameTime = duration / frames.toFloat()

        // Should handle complex mixing in real-time
        assertTrue(avgFrameTime < 16f, "Mixer update should be <16ms, was ${avgFrameTime}ms")
    }

    // Helper functions

    private fun createAnimatedMorphGeometry(): BufferGeometry {
        val geometry = BufferGeometry()

        val basePositions = floatArrayOf(
            -1f, -1f, 0f,
            1f, -1f, 0f,
            1f, 1f, 0f,
            -1f, 1f, 0f
        )
        geometry.setAttribute("position", BufferAttribute(basePositions, 3))

        geometry.morphTargets = listOf(
            MorphTarget(
                name = "smile",
                position = BufferAttribute(
                    floatArrayOf(
                        -0.8f, -0.8f, 0f,
                        0.8f, -0.8f, 0f,
                        1.2f, 1.2f, 0f,
                        -1.2f, 1.2f, 0f
                    ), 3
                )
            ),
            MorphTarget(
                name = "frown",
                position = BufferAttribute(
                    floatArrayOf(
                        -1.2f, -1.2f, 0f,
                        1.2f, -1.2f, 0f,
                        0.8f, 0.8f, 0f,
                        -0.8f, 0.8f, 0f
                    ), 3
                )
            )
        )

        return geometry
    }

    private fun createComplexMorphGeometry(morphCount: Int): BufferGeometry {
        val geometry = BufferGeometry()
        val vertexCount = 1000

        val basePositions = FloatArray(vertexCount * 3)
        geometry.setAttribute("position", BufferAttribute(basePositions, 3))

        val morphTargets = mutableListOf<MorphTarget>()
        for (i in 0 until morphCount) {
            morphTargets.add(
                MorphTarget(
                    name = "morph_$i",
                    position = BufferAttribute(FloatArray(vertexCount * 3) {
                        Math.random().toFloat()
                    }, 3)
                )
            )
        }
        geometry.morphTargets = morphTargets

        return geometry
    }
}

// Supporting classes for the contract test

class MorphAnimationMixer(private val geometry: BufferGeometry) {
    private val influences = FloatArray(geometry.morphTargets.size)
    private val actions = mutableListOf<AnimationAction>()
    private var currentTime = 0f

    fun clipAction(clip: AnimationClip): AnimationAction {
        val action = AnimationAction(clip, this)
        actions.add(action)
        return action
    }

    fun update(deltaTime: Float) {
        currentTime += deltaTime

        // Reset influences
        influences.fill(0f)

        // Apply all active actions
        for (action in actions) {
            if (action.isRunning) {
                action.update(deltaTime)
                action.applyInfluences(influences)
            }
        }
    }

    fun setTime(time: Float) {
        currentTime = time
        update(0f)
    }

    fun getInfluence(index: Int): Float {
        return influences[index]
    }

    fun crossFade(from: AnimationAction, to: AnimationAction, duration: Float) {
        from.fadeOut(duration)
        to.fadeIn(duration)
    }
}

class AnimationAction(
    private val clip: AnimationClip,
    private val mixer: MorphAnimationMixer
) {
    var weight = 1f
    var loop = LoopMode.ONCE
    var isRunning = false
    private var localTime = 0f
    private var fadeStartTime = -1f
    private var fadeDuration = 0f
    private var fadeDirection = 0 // 0=none, 1=in, -1=out

    var onStart: (() -> Unit)? = null
    var onFinish: (() -> Unit)? = null
    var onLoop: (() -> Unit)? = null

    fun play() {
        isRunning = true
        localTime = 0f
        onStart?.invoke()
    }

    fun stop() {
        isRunning = false
        onFinish?.invoke()
    }

    fun reset() {
        localTime = 0f
        isRunning = false
    }

    fun fadeIn(duration: Float) {
        fadeStartTime = localTime
        fadeDuration = duration
        fadeDirection = 1
        weight = 0f
        play()
    }

    fun fadeOut(duration: Float) {
        fadeStartTime = localTime
        fadeDuration = duration
        fadeDirection = -1
    }

    fun update(deltaTime: Float) {
        if (!isRunning) return

        localTime += deltaTime

        // Handle fading
        if (fadeDirection != 0 && fadeDuration > 0) {
            val fadeProgress = (localTime - fadeStartTime) / fadeDuration
            when (fadeDirection) {
                1 -> weight = fadeProgress.coerceIn(0f, 1f)
                -1 -> {
                    weight = 1f - fadeProgress.coerceIn(0f, 1f)
                    if (weight <= 0f) stop()
                }
            }
        }

        // Handle looping
        when (loop) {
            LoopMode.ONCE -> {
                if (localTime >= clip.duration) {
                    localTime = clip.duration
                    stop()
                }
            }

            LoopMode.REPEAT -> {
                if (localTime >= clip.duration) {
                    localTime %= clip.duration
                    onLoop?.invoke()
                }
            }

            LoopMode.PING_PONG -> {
                val cycle = (localTime / clip.duration).toInt()
                val cycleTime = localTime % clip.duration
                if (cycle % 2 == 1) {
                    localTime = clip.duration - cycleTime
                } else {
                    localTime = cycleTime
                }
                if (cycleTime < deltaTime) {
                    onLoop?.invoke()
                }
            }
        }
    }

    fun applyInfluences(targetInfluences: FloatArray) {
        for (track in clip.tracks) {
            if (track is MorphInfluenceTrack) {
                val value = track.evaluate(localTime)
                targetInfluences[track.targetIndex] += value * weight
            }
        }
    }
}

class MorphInfluenceTrack(
    val targetIndex: Int,
    val times: FloatArray,
    val values: FloatArray,
    val interpolation: InterpolationMode = InterpolationMode.LINEAR
) : KeyframeTrack {

    fun evaluate(time: Float): Float {
        // Find surrounding keyframes
        var i = 0
        while (i < times.size - 1 && times[i + 1] < time) {
            i++
        }

        if (i >= times.size - 1) {
            return values.last()
        }

        val t0 = times[i]
        val t1 = times[i + 1]
        val v0 = values[i]
        val v1 = values[i + 1]

        val alpha = (time - t0) / (t1 - t0)

        return when (interpolation) {
            InterpolationMode.LINEAR -> v0 + (v1 - v0) * alpha
            InterpolationMode.CUBIC -> {
                // Simplified cubic interpolation
                val alpha2 = alpha * alpha
                val alpha3 = alpha2 * alpha
                v0 + (v1 - v0) * (3 * alpha2 - 2 * alpha3)
            }

            InterpolationMode.STEP -> if (alpha < 1f) v0 else v1
        }
    }
}

enum class LoopMode {
    ONCE,
    REPEAT,
    PING_PONG
}

enum class InterpolationMode {
    LINEAR,
    CUBIC,
    STEP
}

class MorphShaderGenerator(private val geometry: BufferGeometry) {
    companion object {
        fun getMaxMorphTargets(): Int = 8 // WebGPU minimum
    }

    fun generateVertexShader(includeNormals: Boolean = false): String {
        val targetCount = minOf(geometry.morphTargets.size, getMaxMorphTargets())

        val shader = StringBuilder()

        // Uniforms
        shader.appendLine("uniform float morphTargetInfluences[$targetCount];")
        for (i in 0 until targetCount) {
            shader.appendLine("attribute vec3 morphTarget$i;")
            if (includeNormals) {
                shader.appendLine("attribute vec3 morphNormal$i;")
            }
        }

        // Vertex shader main
        shader.appendLine("void main() {")
        shader.appendLine("  vec3 position = position;")

        if (includeNormals) {
            shader.appendLine("  vec3 normal = normal;")
        }

        // Apply morph targets
        for (i in 0 until targetCount) {
            shader.appendLine("  position += morphTarget$i * morphTargetInfluences[$i];")
            if (includeNormals) {
                shader.appendLine("  normal += morphNormal$i * morphTargetInfluences[$i];")
            }
        }

        if (includeNormals) {
            shader.appendLine("  normal = normalize(normal);")
        }

        shader.appendLine("  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);")
        shader.appendLine("}")

        return shader.toString()
    }

    fun generateOptimizedShader(): String {
        // Use texture for morph data storage
        return """
            uniform sampler2D morphTexture;
            uniform float morphTargetInfluences[${getMaxMorphTargets()}];

            vec3 getMorphPosition(int vertexId, int targetId) {
                vec2 uv = vec2(float(vertexId), float(targetId)) / textureSize(morphTexture, 0);
                return texture2D(morphTexture, uv).xyz;
            }

            void main() {
                vec3 position = position;
                int vid = gl_VertexID;

                for (int i = 0; i < ${geometry.morphTargets.size}; i++) {
                    position += getMorphPosition(vid, i) * morphTargetInfluences[i];
                }

                gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
        """.trimIndent()
    }
}