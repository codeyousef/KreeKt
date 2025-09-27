/**
 * Integration tests for skeletal animation system with IK
 * Tests interaction between AnimationSystem, Skeleton, IKSolver, StateMachine, and animation pipeline
 */
package io.kreekt.integration

import io.kreekt.core.math.*
import io.kreekt.animation.*
import io.kreekt.scene.*
import kotlin.test.*

class AnimationIntegrationTest {

    @Test
    fun testSkeletalAnimationWithIK() {
        // This test will fail until we implement the complete skeletal animation system
        val animationSystem = TODO("AnimationSystem implementation not available yet") as AnimationSystem
        val skeleton = TODO("Skeleton implementation not available yet") as Skeleton
        val ikSolver = TODO("IKSolver implementation not available yet") as IKSolver
        val mesh = TODO("SkinnedMesh implementation not available yet") as SkinnedMesh

        // 1. Setup skeleton hierarchy
        val rootBone = Bone("Root", Matrix4.IDENTITY)
        val spineBone = Bone("Spine", Matrix4.translation(0f, 1f, 0f))
        val leftArmBone = Bone("LeftArm", Matrix4.translation(-1f, 2f, 0f))
        val leftForearmBone = Bone("LeftForearm", Matrix4.translation(-1.5f, 2f, 0f))
        val leftHandBone = Bone("LeftHand", Matrix4.translation(-2f, 2f, 0f))

        // Build bone hierarchy
        rootBone.add(spineBone)
        spineBone.add(leftArmBone)
        leftArmBone.add(leftForearmBone)
        leftForearmBone.add(leftHandBone)

        skeleton.rootBone = rootBone
        mesh.skeleton = skeleton

        // 2. Create animation clip
        val animationClip = AnimationClip(
            name = "WaveAnimation",
            duration = 3.0f,
            tracks = listOf(
                KeyframeTrack(
                    boneName = "LeftArm",
                    property = AnimationProperty.ROTATION,
                    times = floatArrayOf(0f, 1.5f, 3f),
                    values = arrayOf(
                        Quaternion.IDENTITY,
                        Quaternion.fromAxisAngle(Vector3.FORWARD, Math.PI.toFloat() / 4f),
                        Quaternion.IDENTITY
                    ),
                    interpolation = InterpolationType.LINEAR
                ),
                KeyframeTrack(
                    boneName = "LeftForearm",
                    property = AnimationProperty.ROTATION,
                    times = floatArrayOf(0f, 0.75f, 2.25f, 3f),
                    values = arrayOf(
                        Quaternion.IDENTITY,
                        Quaternion.fromAxisAngle(Vector3.FORWARD, Math.PI.toFloat() / 3f),
                        Quaternion.fromAxisAngle(Vector3.FORWARD, Math.PI.toFloat() / 3f),
                        Quaternion.IDENTITY
                    ),
                    interpolation = InterpolationType.CUBIC_BEZIER
                )
            )
        )

        // 3. Setup animation mixer
        val animationMixer = AnimationMixer(mesh)
        val animationAction = animationMixer.clipAction(animationClip)

        animationAction.play()
        assertTrue(animationAction.isRunning, "Animation action should be running")

        // 4. Setup IK constraint for hand
        val ikChain = IKChain(
            bones = listOf(leftArmBone, leftForearmBone, leftHandBone),
            target = Vector3(-3f, 3f, 1f), // Target position for hand
            poleTarget = Vector3(-1f, 3f, 1f), // Elbow pole position
            chainLength = 3
        )

        val ikConstraint = IKConstraint(
            chain = ikChain,
            solver = IKSolverType.FABRIK,
            weight = 1.0f,
            iterations = 10,
            tolerance = 0.01f
        )

        skeleton.addIKConstraint(ikConstraint)

        // 5. Update animation and IK
        val deltaTime = 0.016f // 60 FPS
        for (frame in 0..180) { // 3 seconds at 60 FPS
            val currentTime = frame * deltaTime

            // Update keyframe animation
            animationMixer.update(deltaTime)

            // Apply IK solving
            val ikResult = ikSolver.solve(ikConstraint)

            when (ikResult) {
                is IKResult.Success -> {
                    val ikSolution = ikResult.value

                    // Verify IK solution is valid
                    assertTrue(ikSolution.converged, "IK should converge within iteration limit")
                    assertTrue(ikSolution.error < ikConstraint.tolerance, "IK error should be within tolerance")

                    // Verify hand reaches target
                    val handWorldPosition = leftHandBone.worldMatrix.getTranslation()
                    val distanceToTarget = handWorldPosition.distanceTo(ikChain.target)
                    assertTrue(distanceToTarget < 0.1f, "Hand should reach IK target")
                }
                is IKResult.Error -> fail("IK solving should not fail: ${ikResult.exception.message}")
            }

            // Update skeleton matrices
            skeleton.update()

            // Verify bone transformations are valid
            skeleton.bones.forEach { bone ->
                assertFalse(bone.worldMatrix.isNaN(), "Bone world matrix should not contain NaN")
                assertFalse(bone.worldMatrix.isInfinite(), "Bone world matrix should not contain infinite values")
            }
        }
    }

    @Test
    fun testAnimationStateMachine() {
        val animationSystem = TODO("AnimationSystem implementation not available yet") as AnimationSystem
        val stateMachine = TODO("StateMachine implementation not available yet") as StateMachine
        val mesh = TODO("SkinnedMesh implementation not available yet") as SkinnedMesh

        // 1. Define animation clips
        val idleClip = AnimationClip("Idle", 2f, emptyList())
        val walkClip = AnimationClip("Walk", 1f, emptyList())
        val runClip = AnimationClip("Run", 0.8f, emptyList())
        val jumpClip = AnimationClip("Jump", 1.2f, emptyList())

        // 2. Setup animation states
        val idleState = AnimationState(
            name = "Idle",
            clip = idleClip,
            loop = true,
            speed = 1.0f
        )

        val walkState = AnimationState(
            name = "Walk",
            clip = walkClip,
            loop = true,
            speed = 1.0f
        )

        val runState = AnimationState(
            name = "Run",
            clip = runClip,
            loop = true,
            speed = 1.2f
        )

        val jumpState = AnimationState(
            name = "Jump",
            clip = jumpClip,
            loop = false,
            speed = 1.0f
        )

        // 3. Define state transitions
        val transitions = listOf(
            StateTransition(
                from = "Idle",
                to = "Walk",
                condition = ParameterCondition("speed", ComparisonType.GREATER_THAN, 0.1f),
                duration = 0.2f,
                exitTime = 0.8f
            ),
            StateTransition(
                from = "Walk",
                to = "Run",
                condition = ParameterCondition("speed", ComparisonType.GREATER_THAN, 5.0f),
                duration = 0.3f,
                exitTime = 0.5f
            ),
            StateTransition(
                from = "Walk",
                to = "Idle",
                condition = ParameterCondition("speed", ComparisonType.LESS_THAN, 0.1f),
                duration = 0.25f,
                exitTime = 0.7f
            ),
            StateTransition(
                from = "Run",
                to = "Walk",
                condition = ParameterCondition("speed", ComparisonType.LESS_THAN, 5.0f),
                duration = 0.2f,
                exitTime = 0.3f
            ),
            StateTransition(
                from = "Idle",
                to = "Jump",
                condition = TriggerCondition("jump"),
                duration = 0.1f,
                exitTime = 0f
            ),
            StateTransition(
                from = "Walk",
                to = "Jump",
                condition = TriggerCondition("jump"),
                duration = 0.1f,
                exitTime = 0f
            ),
            StateTransition(
                from = "Jump",
                to = "Idle",
                condition = FinishedCondition(),
                duration = 0.2f,
                exitTime = 1.0f
            )
        )

        // 4. Initialize state machine
        stateMachine.addState(idleState)
        stateMachine.addState(walkState)
        stateMachine.addState(runState)
        stateMachine.addState(jumpState)

        transitions.forEach { stateMachine.addTransition(it) }

        stateMachine.setInitialState("Idle")
        assertEquals("Idle", stateMachine.currentState?.name, "Should start in idle state")

        // 5. Test state transitions
        val deltaTime = 0.016f

        // Test idle to walk
        stateMachine.setFloat("speed", 2.0f)
        repeat(20) { stateMachine.update(deltaTime) } // Allow transition time

        assertEquals("Walk", stateMachine.currentState?.name, "Should transition to walk state")

        // Test walk to run
        stateMachine.setFloat("speed", 8.0f)
        repeat(25) { stateMachine.update(deltaTime) } // Allow transition time

        assertEquals("Run", stateMachine.currentState?.name, "Should transition to run state")

        // Test jump trigger
        stateMachine.setTrigger("jump")
        repeat(10) { stateMachine.update(deltaTime) } // Allow transition time

        assertEquals("Jump", stateMachine.currentState?.name, "Should transition to jump state")

        // Test jump completion
        repeat(100) { stateMachine.update(deltaTime) } // Allow jump to complete

        assertEquals("Idle", stateMachine.currentState?.name, "Should return to idle after jump")
    }

    @Test
    fun testMorphTargetAnimation() {
        val animationSystem = TODO("AnimationSystem implementation not available yet") as AnimationSystem
        val morphAnimator = TODO("MorphTargetAnimator implementation not available yet") as MorphTargetAnimator
        val mesh = TODO("Mesh implementation not available yet") as Mesh

        // 1. Setup morph targets
        val morphTargets = mapOf(
            "smile" to MorphTarget(
                name = "smile",
                vertices = FloatArray(mesh.geometry.getAttribute("position")!!.array.size) { /* modified vertices */ }
            ),
            "frown" to MorphTarget(
                name = "frown",
                vertices = FloatArray(mesh.geometry.getAttribute("position")!!.array.size) { /* modified vertices */ }
            ),
            "blink_left" to MorphTarget(
                name = "blink_left",
                vertices = FloatArray(mesh.geometry.getAttribute("position")!!.array.size) { /* modified vertices */ }
            ),
            "blink_right" to MorphTarget(
                name = "blink_right",
                vertices = FloatArray(mesh.geometry.getAttribute("position")!!.array.size) { /* modified vertices */ }
            )
        )

        mesh.morphTargets = morphTargets

        // 2. Create facial animation clip
        val facialAnimationClip = AnimationClip(
            name = "FacialExpression",
            duration = 4f,
            tracks = listOf(
                MorphTrack(
                    targetName = "smile",
                    times = floatArrayOf(0f, 1f, 3f, 4f),
                    values = floatArrayOf(0f, 1f, 1f, 0f),
                    interpolation = InterpolationType.SMOOTH_STEP
                ),
                MorphTrack(
                    targetName = "blink_left",
                    times = floatArrayOf(0.5f, 0.7f, 0.9f),
                    values = floatArrayOf(0f, 1f, 0f),
                    interpolation = InterpolationType.LINEAR
                ),
                MorphTrack(
                    targetName = "blink_right",
                    times = floatArrayOf(0.6f, 0.8f, 1.0f),
                    values = floatArrayOf(0f, 1f, 0f),
                    interpolation = InterpolationType.LINEAR
                )
            )
        )

        // 3. Setup morph animator
        morphAnimator.setMesh(mesh)
        val morphAction = morphAnimator.clipAction(facialAnimationClip)

        morphAction.play()
        assertTrue(morphAction.isRunning, "Morph animation should be running")

        // 4. Update animation and verify morph weights
        val deltaTime = 0.016f
        for (frame in 0..240) { // 4 seconds at 60 FPS
            val currentTime = frame * deltaTime

            morphAnimator.update(deltaTime)

            // Get current morph weights
            val smileWeight = morphAnimator.getMorphWeight("smile")
            val blinkLeftWeight = morphAnimator.getMorphWeight("blink_left")
            val blinkRightWeight = morphAnimator.getMorphWeight("blink_right")

            // Verify weights are in valid range
            assertTrue(smileWeight >= 0f && smileWeight <= 1f, "Smile weight should be between 0 and 1")
            assertTrue(blinkLeftWeight >= 0f && blinkLeftWeight <= 1f, "Blink left weight should be between 0 and 1")
            assertTrue(blinkRightWeight >= 0f && blinkRightWeight <= 1f, "Blink right weight should be between 0 and 1")

            // Test specific timing
            when {
                currentTime >= 1f && currentTime <= 3f -> {
                    assertTrue(smileWeight > 0.8f, "Should be smiling at time $currentTime")
                }
                currentTime >= 0.6f && currentTime <= 0.8f -> {
                    assertTrue(blinkLeftWeight > 0.5f, "Left eye should be blinking at time $currentTime")
                }
                currentTime >= 0.7f && currentTime <= 0.9f -> {
                    assertTrue(blinkRightWeight > 0.5f, "Right eye should be blinking at time $currentTime")
                }
            }
        }

        // 5. Test manual morph control
        morphAnimator.setMorphWeight("smile", 0.5f)
        morphAnimator.setMorphWeight("frown", 0.3f)

        val resultSmile = morphAnimator.getMorphWeight("smile")
        val resultFrown = morphAnimator.getMorphWeight("frown")

        assertEquals(0.5f, resultSmile, 0.01f, "Manual smile weight should be set")
        assertEquals(0.3f, resultFrown, 0.01f, "Manual frown weight should be set")
    }

    @Test
    fun testAnimationBlending() {
        val animationSystem = TODO("AnimationSystem implementation not available yet") as AnimationSystem
        val animationMixer = TODO("AnimationMixer implementation not available yet") as AnimationMixer
        val mesh = TODO("SkinnedMesh implementation not available yet") as SkinnedMesh

        // 1. Create multiple animation clips
        val upperBodyClip = AnimationClip("UpperBodyWave", 2f, emptyList())
        val lowerBodyClip = AnimationClip("LowerBodyWalk", 1f, emptyList())
        val fullBodyClip = AnimationClip("FullBodyDance", 3f, emptyList())

        // 2. Setup animation layers
        val baseLayer = AnimationLayer(
            name = "BaseLayer",
            weight = 1.0f,
            blendMode = BlendMode.OVERRIDE
        )

        val upperBodyLayer = AnimationLayer(
            name = "UpperBodyLayer",
            weight = 0.8f,
            blendMode = BlendMode.ADDITIVE,
            boneMask = BoneMask.createUpperBodyMask(mesh.skeleton)
        )

        animationMixer.addLayer(baseLayer)
        animationMixer.addLayer(upperBodyLayer)

        // 3. Setup animation actions on different layers
        val walkAction = animationMixer.clipAction(lowerBodyClip, baseLayer)
        val waveAction = animationMixer.clipAction(upperBodyClip, upperBodyLayer)

        walkAction.play()
        waveAction.play()

        assertTrue(walkAction.isRunning, "Walk action should be running")
        assertTrue(waveAction.isRunning, "Wave action should be running")

        // 4. Test weight blending
        val deltaTime = 0.016f
        for (frame in 0..120) { // 2 seconds
            val currentTime = frame * deltaTime

            // Animate layer weights
            val cycleTime = currentTime % 2f
            val upperBodyWeight = (Math.sin(cycleTime * Math.PI) * 0.5 + 0.5).toFloat()

            upperBodyLayer.weight = upperBodyWeight
            animationMixer.update(deltaTime)

            // Verify blending is applied correctly
            assertTrue(upperBodyLayer.weight >= 0f && upperBodyLayer.weight <= 1f,
                "Upper body weight should be valid")

            // Check that bone transformations are blended
            mesh.skeleton.bones.forEach { bone ->
                if (upperBodyLayer.boneMask?.includes(bone) == true) {
                    // Upper body bones should be influenced by both layers
                    assertTrue(bone.animationInfluence > 0f, "Upper body bone should have animation influence")
                }
            }
        }

        // 5. Test crossfade between animations
        val danceAction = animationMixer.clipAction(fullBodyClip, baseLayer)

        val crossfadeResult = animationMixer.crossfade(
            fromAction = walkAction,
            toAction = danceAction,
            duration = 1.0f,
            warp = true
        )

        assertTrue(crossfadeResult is CrossfadeResult.Success, "Crossfade should succeed")

        // Update during crossfade
        repeat(60) { // 1 second crossfade
            animationMixer.update(deltaTime)

            val walkWeight = walkAction.weight
            val danceWeight = danceAction.weight

            // Verify weights sum to 1 during crossfade
            val totalWeight = walkWeight + danceWeight
            assertEquals(1.0f, totalWeight, 0.1f, "Total weight should be approximately 1 during crossfade")

            assertTrue(danceWeight >= 0f && danceWeight <= 1f, "Dance weight should be valid")
            assertTrue(walkWeight >= 0f && walkWeight <= 1f, "Walk weight should be valid")
        }

        // After crossfade, dance should be dominant
        assertTrue(danceAction.weight > 0.8f, "Dance action should be dominant after crossfade")
        assertTrue(walkAction.weight < 0.2f, "Walk action should be faded out")
    }

    @Test
    fun testAnimationCompression() {
        val animationSystem = TODO("AnimationSystem implementation not available yet") as AnimationSystem
        val animationCompressor = TODO("AnimationCompressor implementation not available yet") as AnimationCompressor

        // 1. Create high-frequency animation data
        val highFreqTimes = FloatArray(1000) { it * 0.001f } // 1000 keyframes over 1 second
        val highFreqValues = Array(1000) { index ->
            // Complex curve with redundant keyframes
            val t = index * 0.001f
            Vector3(
                (Math.sin(t * 2 * Math.PI) * 0.1).toFloat(),
                (Math.cos(t * 4 * Math.PI) * 0.05).toFloat(),
                (Math.sin(t * Math.PI) * 0.2).toFloat()
            )
        }

        val originalTrack = KeyframeTrack(
            boneName = "TestBone",
            property = AnimationProperty.POSITION,
            times = highFreqTimes,
            values = highFreqValues,
            interpolation = InterpolationType.LINEAR
        )

        val originalClip = AnimationClip(
            name = "HighFrequencyAnimation",
            duration = 1f,
            tracks = listOf(originalTrack)
        )

        // 2. Compress animation with different settings
        val compressionSettings = listOf(
            CompressionSettings(
                positionTolerance = 0.01f,
                rotationTolerance = 0.1f,
                scaleTolerance = 0.01f,
                timeTolerance = 0.001f,
                algorithm = CompressionAlgorithm.KEYFRAME_REDUCTION
            ),
            CompressionSettings(
                positionTolerance = 0.05f,
                rotationTolerance = 0.5f,
                scaleTolerance = 0.05f,
                timeTolerance = 0.01f,
                algorithm = CompressionAlgorithm.CURVE_FITTING
            )
        )

        compressionSettings.forEach { settings ->
            val compressionResult = animationCompressor.compressClip(originalClip, settings)

            when (compressionResult) {
                is CompressionResult.Success -> {
                    val compressedClip = compressionResult.value
                    val compressionRatio = compressionResult.compressionRatio

                    // Verify compression achieved size reduction
                    assertTrue(compressionRatio > 1.5f, "Should achieve significant compression ratio")

                    val originalKeyframes = originalClip.tracks.sumOf { it.keyframeCount }
                    val compressedKeyframes = compressedClip.tracks.sumOf { it.keyframeCount }

                    assertTrue(compressedKeyframes < originalKeyframes, "Compressed clip should have fewer keyframes")

                    // Verify quality is maintained within tolerance
                    val qualityLoss = compressionResult.qualityLoss
                    assertTrue(qualityLoss < settings.positionTolerance * 2, "Quality loss should be within acceptable bounds")
                }
                is CompressionResult.Error -> fail("Animation compression should not fail")
            }
        }

        // 3. Test quantization compression
        val quantizationResult = animationCompressor.quantizeClip(
            clip = originalClip,
            positionBits = 16,
            rotationBits = 12,
            scaleBits = 14,
            timeBits = 16
        )

        when (quantizationResult) {
            is QuantizationResult.Success -> {
                val quantizedClip = quantizationResult.value
                val sizeReduction = quantizationResult.sizeReduction

                assertTrue(sizeReduction > 0.5f, "Quantization should reduce size by at least 50%")

                // Verify quantized data is still playable
                val testMixer = TODO("AnimationMixer for test not available yet") as AnimationMixer
                val quantizedAction = testMixer.clipAction(quantizedClip)

                quantizedAction.play()
                assertTrue(quantizedAction.isRunning, "Quantized animation should be playable")
            }
            is QuantizationResult.Error -> fail("Animation quantization should not fail")
        }
    }

    @Test
    fun testPerformanceOptimization() {
        val animationSystem = TODO("AnimationSystem implementation not available yet") as AnimationSystem
        val performanceMonitor = TODO("PerformanceMonitor implementation not available yet") as PerformanceMonitor

        // 1. Create complex animation scenario
        val meshCount = 100
        val meshes = (0 until meshCount).map { index ->
            val mesh = TODO("SkinnedMesh creation not available yet") as SkinnedMesh
            val mixer = AnimationMixer(mesh)

            // Add multiple animations per mesh
            val clips = listOf(
                AnimationClip("Idle$index", 2f, emptyList()),
                AnimationClip("Walk$index", 1f, emptyList()),
                AnimationClip("Run$index", 0.8f, emptyList())
            )

            clips.forEach { clip ->
                val action = mixer.clipAction(clip)
                if (index % 3 == 0) action.play() // Only play some animations
            }

            mesh to mixer
        }

        // 2. Measure baseline performance
        val baselineMetrics = performanceMonitor.startMeasurement("animation_baseline")

        val deltaTime = 0.016f
        repeat(300) { // 5 seconds of animation
            meshes.forEach { (_, mixer) ->
                mixer.update(deltaTime)
            }
        }

        val baselineResult = performanceMonitor.endMeasurement(baselineMetrics)
        val baselineFrameTime = baselineResult.averageFrameTime

        // 3. Enable animation optimizations
        val optimizations = AnimationOptimizations(
            enableLOD = true,
            enableCulling = true,
            enableBatching = true,
            lodDistances = listOf(10f, 25f, 50f),
            maxActiveAnimations = 50
        )

        animationSystem.setOptimizations(optimizations)

        // 4. Measure optimized performance
        val optimizedMetrics = performanceMonitor.startMeasurement("animation_optimized")

        repeat(300) { // 5 seconds of animation with optimizations
            animationSystem.updateAllAnimations(meshes.map { it.second }, deltaTime)
        }

        val optimizedResult = performanceMonitor.endMeasurement(optimizedMetrics)
        val optimizedFrameTime = optimizedResult.averageFrameTime

        // 5. Verify performance improvement
        val performanceImprovement = (baselineFrameTime - optimizedFrameTime) / baselineFrameTime

        assertTrue(performanceImprovement > 0.2f, "Optimizations should improve performance by at least 20%")
        assertTrue(optimizedFrameTime < 16.67f, "Optimized animation should maintain 60 FPS")

        // 6. Verify LOD system is working
        val lodMetrics = animationSystem.getLODMetrics()

        assertTrue(lodMetrics.highDetailCount < meshCount, "Not all meshes should be at high detail")
        assertTrue(lodMetrics.totalActiveAnimations <= optimizations.maxActiveAnimations,
            "Should respect max active animations limit")

        // 7. Verify culling is working
        val cullingMetrics = animationSystem.getCullingMetrics()

        assertTrue(cullingMetrics.culledAnimations > 0, "Some animations should be culled")
        assertTrue(cullingMetrics.activeAnimations < meshCount * 3, "Culling should reduce active animations")
    }
}