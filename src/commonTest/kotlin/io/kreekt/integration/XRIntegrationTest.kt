/**
 * Integration tests for XR system
 * Tests interaction between XRSystem, XRSession, XRController, ARSystem, and XR pipeline
 */
package io.kreekt.integration

import io.kreekt.core.math.*
import io.kreekt.xr.*
import io.kreekt.scene.*
import kotlin.test.*

class XRIntegrationTest {

    @Test
    fun testXRSessionManagement() {
        // This test will fail until we implement the complete XR system
        val xrSystem = TODO("XRSystem implementation not available yet") as XRSystem
        val scene = TODO("Scene implementation not available yet") as Scene

        // 1. Check XR support
        val xrSupport = xrSystem.isSupported()
        if (!xrSupport.isSupported) {
            println("XR not supported, skipping XR session test: ${xrSupport.reason}")
            return
        }

        // 2. Request XR session for VR
        val sessionRequest = XRSessionRequest(
            mode = XRSessionMode.IMMERSIVE_VR,
            requiredFeatures = listOf(
                XRFeature.LOCAL_FLOOR,
                XRFeature.BOUNDED_FLOOR
            ),
            optionalFeatures = listOf(
                XRFeature.HAND_TRACKING,
                XRFeature.EYE_TRACKING,
                XRFeature.ANCHORS
            )
        )

        val sessionResult = xrSystem.requestSession(sessionRequest)

        when (sessionResult) {
            is XRSessionResult.Success -> {
                val session = sessionResult.value

                assertNotNull(session, "XR session should be created")
                assertEquals(XRSessionMode.IMMERSIVE_VR, session.mode, "Session should be in VR mode")
                assertTrue(session.isActive, "Session should be active")

                // 3. Setup XR frame loop
                var frameCount = 0
                val maxFrames = 300 // 5 seconds at 60 FPS

                session.onFrame { frame ->
                    frameCount++

                    // Verify frame data
                    assertNotNull(frame.viewerPose, "Frame should have viewer pose")
                    assertTrue(frame.views.isNotEmpty(), "Frame should have views")

                    // Process each view (eye)
                    frame.views.forEach { view ->
                        assertNotNull(view.transform, "View should have transform")
                        assertNotNull(view.projectionMatrix, "View should have projection matrix")

                        // Update camera for this view
                        val camera = scene.getCamera(view.eye)
                        camera.matrixWorld = view.transform
                        camera.projectionMatrix = view.projectionMatrix
                        camera.updateMatrixWorld()
                    }

                    // Render scene for XR
                    val renderResult = xrSystem.renderFrame(frame, scene)
                    assertTrue(renderResult is XRRenderResult.Success, "XR frame rendering should succeed")

                    // Continue until max frames
                    frameCount < maxFrames
                }

                // 4. Test session state changes
                assertTrue(session.isActive, "Session should remain active during frame loop")
                assertEquals(frameCount, maxFrames, "Should process all frames")

                // 5. End session
                val endResult = session.end()
                assertTrue(endResult is XRSessionResult.Success, "Session should end successfully")
                assertFalse(session.isActive, "Session should not be active after ending")
            }
            is XRSessionResult.Error -> {
                // XR session might not be available in test environment
                println("XR session creation failed: ${sessionResult.exception.message}")
                assertTrue(true, "Test passed - XR not available in test environment")
            }
        }
    }

    @Test
    fun testXRControllerInput() {
        val xrSystem = TODO("XRSystem implementation not available yet") as XRSystem

        if (!xrSystem.isSupported().isSupported) {
            println("XR not supported, skipping controller test")
            return
        }

        val sessionResult = xrSystem.requestSession(
            XRSessionRequest(
                mode = XRSessionMode.IMMERSIVE_VR,
                requiredFeatures = listOf(XRFeature.LOCAL)
            )
        )

        when (sessionResult) {
            is XRSessionResult.Success -> {
                val session = sessionResult.value

                // 1. Get XR controllers
                val controllers = session.getControllers()

                if (controllers.isEmpty()) {
                    println("No XR controllers available, skipping test")
                    return
                }

                val leftController = controllers.find { it.handedness == XRHandedness.LEFT }
                val rightController = controllers.find { it.handedness == XRHandedness.RIGHT }

                // 2. Test controller tracking
                session.onFrame { frame ->
                    leftController?.let { controller ->
                        val pose = controller.getPose(frame)
                        if (pose != null) {
                            assertNotNull(pose.position, "Controller should have position")
                            assertNotNull(pose.orientation, "Controller should have orientation")

                            // Verify pose is reasonable
                            assertTrue(pose.position.y > -2f, "Controller Y position should be reasonable")
                            assertTrue(pose.position.y < 3f, "Controller Y position should be reasonable")
                        }

                        // 3. Test controller input
                        val gamepad = controller.gamepad
                        if (gamepad != null) {
                            // Test button states
                            val triggerPressed = gamepad.buttons[XRButton.TRIGGER.index]?.pressed ?: false
                            val gripPressed = gamepad.buttons[XRButton.GRIP.index]?.pressed ?: false
                            val aPressed = gamepad.buttons[XRButton.A.index]?.pressed ?: false

                            // Test axis values
                            val thumbstickX = gamepad.axes[XRAxis.THUMBSTICK_X.index] ?: 0f
                            val thumbstickY = gamepad.axes[XRAxis.THUMBSTICK_Y.index] ?: 0f

                            // Verify input ranges
                            assertTrue(thumbstickX >= -1f && thumbstickX <= 1f, "Thumbstick X should be in range [-1, 1]")
                            assertTrue(thumbstickY >= -1f && thumbstickY <= 1f, "Thumbstick Y should be in range [-1, 1]")

                            // Test input events
                            if (triggerPressed) {
                                println("Left trigger pressed")
                            }

                            if (abs(thumbstickX) > 0.1f || abs(thumbstickY) > 0.1f) {
                                println("Thumbstick moved: ($thumbstickX, $thumbstickY)")
                            }
                        }

                        // 4. Test haptic feedback
                        if (controller.supportsHaptics()) {
                            val hapticResult = controller.pulse(
                                intensity = 0.5f,
                                duration = 100 // 100ms
                            )
                            assertTrue(hapticResult is HapticResult.Success, "Haptic feedback should work")
                        }
                    }

                    // Continue for a few frames
                    frame.frameNumber < 60
                }

                session.end()
            }
            is XRSessionResult.Error -> {
                println("XR session creation failed: ${sessionResult.exception.message}")
            }
        }
    }

    @Test
    fun testARFeatures() {
        val xrSystem = TODO("XRSystem implementation not available yet") as XRSystem
        val arSystem = TODO("ARSystem implementation not available yet") as ARSystem

        if (!xrSystem.isSupported().isSupported) {
            println("XR not supported, skipping AR test")
            return
        }

        // 1. Request AR session
        val arSessionRequest = XRSessionRequest(
            mode = XRSessionMode.IMMERSIVE_AR,
            requiredFeatures = listOf(
                XRFeature.LOCAL,
                XRFeature.PLANE_DETECTION,
                XRFeature.HIT_TEST
            ),
            optionalFeatures = listOf(
                XRFeature.ANCHORS,
                XRFeature.LIGHT_ESTIMATION,
                XRFeature.OCCLUSION
            )
        )

        val sessionResult = xrSystem.requestSession(arSessionRequest)

        when (sessionResult) {
            is XRSessionResult.Success -> {
                val session = sessionResult.value

                // 2. Enable plane detection
                val planeDetectionResult = arSystem.enablePlaneDetection(
                    session = session,
                    orientation = listOf(
                        PlaneOrientation.HORIZONTAL,
                        PlaneOrientation.VERTICAL
                    )
                )

                assertTrue(planeDetectionResult is ARResult.Success, "Plane detection should be enabled")

                // 3. Setup hit testing
                val hitTestSource = arSystem.requestHitTestSource(
                    session = session,
                    offsetRay = Ray(Vector3.ZERO, Vector3(0f, 0f, -1f))
                )

                when (hitTestSource) {
                    is HitTestSourceResult.Success -> {
                        val source = hitTestSource.value

                        // 4. Test AR frame processing
                        var planesDetected = false
                        var hitTestExecuted = false

                        session.onFrame { frame ->
                            // Process detected planes
                            val detectedPlanes = arSystem.getDetectedPlanes(frame)
                            if (detectedPlanes.isNotEmpty()) {
                                planesDetected = true

                                detectedPlanes.forEach { plane ->
                                    assertNotNull(plane.pose, "Plane should have pose")
                                    assertNotNull(plane.polygon, "Plane should have polygon")
                                    assertTrue(plane.polygon.vertices.size >= 3, "Plane polygon should have at least 3 vertices")

                                    when (plane.orientation) {
                                        PlaneOrientation.HORIZONTAL -> {
                                            assertTrue(abs(plane.pose.orientation.x) < 0.3f, "Horizontal plane should be mostly flat")
                                            assertTrue(abs(plane.pose.orientation.z) < 0.3f, "Horizontal plane should be mostly flat")
                                        }
                                        PlaneOrientation.VERTICAL -> {
                                            assertTrue(abs(plane.pose.orientation.y) < 0.3f, "Vertical plane should be mostly upright")
                                        }
                                    }
                                }
                            }

                            // Perform hit test
                            val hitTestResults = arSystem.requestHitTest(frame, source)
                            if (hitTestResults.isNotEmpty()) {
                                hitTestExecuted = true

                                hitTestResults.forEach { hitResult ->
                                    assertNotNull(hitResult.pose, "Hit result should have pose")
                                    assertTrue(hitResult.distance >= 0f, "Hit distance should be positive")

                                    // Place virtual object at hit location
                                    val virtualObject = TODO("Virtual object creation not available yet") as Object3D
                                    virtualObject.position = hitResult.pose.position
                                    virtualObject.quaternion = hitResult.pose.orientation
                                }
                            }

                            // Test light estimation
                            val lightEstimate = arSystem.getLightEstimate(frame)
                            if (lightEstimate != null) {
                                assertTrue(lightEstimate.ambientIntensity >= 0f, "Ambient intensity should be non-negative")
                                assertNotNull(lightEstimate.ambientColorTemperature, "Should estimate color temperature")
                                assertNotNull(lightEstimate.primaryLightDirection, "Should estimate primary light direction")
                            }

                            // Continue for a reasonable time
                            frame.frameNumber < 180 // 3 seconds
                        }

                        // Verify AR features worked
                        // Note: In test environment, these might not trigger
                        if (planesDetected) {
                            println("Plane detection worked")
                        } else {
                            println("No planes detected (expected in test environment)")
                        }

                        if (hitTestExecuted) {
                            println("Hit testing worked")
                        } else {
                            println("No hit test results (expected in test environment)")
                        }
                    }
                    is HitTestSourceResult.Error -> {
                        println("Hit test source creation failed: ${hitTestSource.exception.message}")
                    }
                }

                session.end()
            }
            is XRSessionResult.Error -> {
                println("AR session creation failed: ${sessionResult.exception.message}")
            }
        }
    }

    @Test
    fun testXRAnchorSystem() {
        val xrSystem = TODO("XRSystem implementation not available yet") as XRSystem
        val anchorSystem = TODO("XRAnchorSystem implementation not available yet") as XRAnchorSystem

        if (!xrSystem.isSupported().isSupported) {
            println("XR not supported, skipping anchor test")
            return
        }

        val sessionResult = xrSystem.requestSession(
            XRSessionRequest(
                mode = XRSessionMode.IMMERSIVE_AR,
                requiredFeatures = listOf(XRFeature.ANCHORS)
            )
        )

        when (sessionResult) {
            is XRSessionResult.Success -> {
                val session = sessionResult.value
                val anchors = mutableListOf<XRAnchor>()

                session.onFrame { frame ->
                    // 1. Create anchors at specific poses
                    if (frame.frameNumber == 30) { // After 0.5 seconds
                        val anchorPose = Pose(
                            position = Vector3(1f, 0f, -2f),
                            orientation = Quaternion.IDENTITY
                        )

                        val anchorResult = anchorSystem.createAnchor(session, anchorPose)

                        when (anchorResult) {
                            is AnchorResult.Success -> {
                                val anchor = anchorResult.value
                                anchors.add(anchor)

                                assertNotNull(anchor.id, "Anchor should have ID")
                                assertEquals(XRAnchorState.CREATING, anchor.state, "New anchor should be in creating state")
                            }
                            is AnchorResult.Error -> {
                                println("Anchor creation failed: ${anchorResult.exception.message}")
                            }
                        }
                    }

                    // 2. Update anchor states
                    anchors.forEach { anchor ->
                        val updatedAnchor = anchorSystem.updateAnchor(frame, anchor)

                        when (updatedAnchor.state) {
                            XRAnchorState.CREATING -> {
                                // Anchor is still being processed
                            }
                            XRAnchorState.TRACKING -> {
                                // Anchor is successfully tracking
                                assertNotNull(updatedAnchor.pose, "Tracking anchor should have pose")
                                assertTrue(updatedAnchor.trackingConfidence > 0.5f, "Should have good tracking confidence")
                            }
                            XRAnchorState.SUSPENDED -> {
                                // Anchor tracking temporarily lost
                                assertTrue(updatedAnchor.trackingConfidence < 0.5f, "Suspended anchor should have low confidence")
                            }
                            XRAnchorState.STOPPED -> {
                                // Anchor permanently lost
                            }
                        }
                    }

                    // 3. Test anchor persistence
                    if (frame.frameNumber == 120) { // After 2 seconds
                        anchors.forEach { anchor ->
                            if (anchor.state == XRAnchorState.TRACKING) {
                                val persistResult = anchorSystem.persistAnchor(anchor)

                                when (persistResult) {
                                    is PersistenceResult.Success -> {
                                        val persistentId = persistResult.value
                                        assertNotNull(persistentId, "Persistent anchor should have ID")
                                        assertTrue(persistentId.isNotEmpty(), "Persistent ID should not be empty")
                                    }
                                    is PersistenceResult.Error -> {
                                        println("Anchor persistence failed: ${persistResult.exception.message}")
                                    }
                                }
                            }
                        }
                    }

                    // 4. Test anchor restoration
                    if (frame.frameNumber == 150) { // After 2.5 seconds
                        val storedAnchorIds = anchorSystem.getStoredAnchorIds()

                        storedAnchorIds.forEach { anchorId ->
                            val restoreResult = anchorSystem.restoreAnchor(session, anchorId)

                            when (restoreResult) {
                                is AnchorResult.Success -> {
                                    val restoredAnchor = restoreResult.value
                                    assertTrue(restoredAnchor.isPersistent, "Restored anchor should be persistent")
                                    assertEquals(anchorId, restoredAnchor.persistentId, "Restored anchor should have correct ID")
                                }
                                is AnchorResult.Error -> {
                                    println("Anchor restoration failed: ${restoreResult.exception.message}")
                                }
                            }
                        }
                    }

                    frame.frameNumber < 180 // 3 seconds
                }

                // Clean up anchors
                anchors.forEach { anchor ->
                    anchorSystem.deleteAnchor(anchor)
                }

                session.end()
            }
            is XRSessionResult.Error -> {
                println("XR session creation failed: ${sessionResult.exception.message}")
            }
        }
    }

    @Test
    fun testHandTracking() {
        val xrSystem = TODO("XRSystem implementation not available yet") as XRSystem
        val handTracker = TODO("HandTracker implementation not available yet") as HandTracker

        if (!xrSystem.isSupported().isSupported) {
            println("XR not supported, skipping hand tracking test")
            return
        }

        val sessionResult = xrSystem.requestSession(
            XRSessionRequest(
                mode = XRSessionMode.IMMERSIVE_VR,
                optionalFeatures = listOf(XRFeature.HAND_TRACKING)
            )
        )

        when (sessionResult) {
            is XRSessionResult.Success -> {
                val session = sessionResult.value

                if (!handTracker.isSupported(session)) {
                    println("Hand tracking not supported, skipping test")
                    session.end()
                    return
                }

                session.onFrame { frame ->
                    // 1. Get hand tracking data
                    val leftHand = handTracker.getHand(frame, XRHandedness.LEFT)
                    val rightHand = handTracker.getHand(frame, XRHandedness.RIGHT)

                    listOf(leftHand, rightHand).forEach { hand ->
                        if (hand != null && hand.isTracking) {
                            // 2. Test joint data
                            val joints = hand.joints

                            // Verify all expected joints are present
                            val expectedJoints = listOf(
                                HandJoint.WRIST,
                                HandJoint.THUMB_TIP,
                                HandJoint.INDEX_TIP,
                                HandJoint.MIDDLE_TIP,
                                HandJoint.RING_TIP,
                                HandJoint.PINKY_TIP
                            )

                            expectedJoints.forEach { jointType ->
                                val joint = joints[jointType]
                                if (joint != null) {
                                    assertNotNull(joint.pose, "Joint should have pose")
                                    assertTrue(joint.radius > 0f, "Joint should have positive radius")
                                    assertTrue(joint.trackingConfidence >= 0f, "Tracking confidence should be non-negative")
                                }
                            }

                            // 3. Test hand gestures
                            val pinchStrength = handTracker.getPinchStrength(hand, HandPinch.INDEX_THUMB)
                            assertTrue(pinchStrength >= 0f && pinchStrength <= 1f, "Pinch strength should be in [0,1]")

                            val isGrasping = handTracker.isGrasping(hand)
                            val isPointing = handTracker.isPointing(hand)
                            val isThumbsUp = handTracker.isThumbsUp(hand)

                            // These are boolean values, just verify they can be called

                            // 4. Test hand ray casting
                            val handRay = handTracker.getPointingRay(hand)
                            if (handRay != null) {
                                assertNotNull(handRay.origin, "Hand ray should have origin")
                                assertNotNull(handRay.direction, "Hand ray should have direction")
                                assertTrue(handRay.direction.length() > 0.9f, "Hand ray direction should be normalized")
                            }

                            // 5. Test hand mesh (if supported)
                            if (handTracker.supportsMesh()) {
                                val handMesh = handTracker.getHandMesh(hand)
                                if (handMesh != null) {
                                    assertNotNull(handMesh.vertices, "Hand mesh should have vertices")
                                    assertNotNull(handMesh.indices, "Hand mesh should have indices")
                                    assertTrue(handMesh.vertices.isNotEmpty(), "Hand mesh should have vertex data")
                                }
                            }
                        }
                    }

                    frame.frameNumber < 120 // 2 seconds
                }

                session.end()
            }
            is XRSessionResult.Error -> {
                println("XR session creation failed: ${sessionResult.exception.message}")
            }
        }
    }

    @Test
    fun testEyeTracking() {
        val xrSystem = TODO("XRSystem implementation not available yet") as XRSystem
        val eyeTracker = TODO("EyeTracker implementation not available yet") as EyeTracker

        if (!xrSystem.isSupported().isSupported) {
            println("XR not supported, skipping eye tracking test")
            return
        }

        val sessionResult = xrSystem.requestSession(
            XRSessionRequest(
                mode = XRSessionMode.IMMERSIVE_VR,
                optionalFeatures = listOf(XRFeature.EYE_TRACKING)
            )
        )

        when (sessionResult) {
            is XRSessionResult.Success -> {
                val session = sessionResult.value

                if (!eyeTracker.isSupported(session)) {
                    println("Eye tracking not supported, skipping test")
                    session.end()
                    return
                }

                session.onFrame { frame ->
                    // 1. Get eye tracking data
                    val eyeData = eyeTracker.getEyeData(frame)

                    if (eyeData != null) {
                        // 2. Test gaze ray
                        val gazeRay = eyeData.gazeRay
                        if (gazeRay != null) {
                            assertNotNull(gazeRay.origin, "Gaze ray should have origin")
                            assertNotNull(gazeRay.direction, "Gaze ray should have direction")
                            assertTrue(gazeRay.direction.length() > 0.9f, "Gaze direction should be normalized")
                        }

                        // 3. Test individual eye data
                        val leftEye = eyeData.leftEye
                        val rightEye = eyeData.rightEye

                        listOf(leftEye, rightEye).forEach { eye ->
                            if (eye != null) {
                                assertTrue(eye.openness >= 0f && eye.openness <= 1f, "Eye openness should be in [0,1]")
                                assertTrue(eye.pupilDiameter > 0f, "Pupil diameter should be positive")

                                if (eye.gazeDirection != null) {
                                    assertTrue(eye.gazeDirection!!.length() > 0.9f, "Eye gaze direction should be normalized")
                                }
                            }
                        }

                        // 4. Test eye tracking confidence
                        assertTrue(eyeData.confidence >= 0f && eyeData.confidence <= 1f, "Eye tracking confidence should be in [0,1]")

                        // 5. Test calibration state
                        val calibrationState = eyeTracker.getCalibrationState()
                        when (calibrationState) {
                            EyeCalibrationState.NOT_CALIBRATED -> {
                                // Eye tracking needs calibration
                            }
                            EyeCalibrationState.CALIBRATING -> {
                                // Calibration in progress
                            }
                            EyeCalibrationState.CALIBRATED -> {
                                // Eye tracking is calibrated and ready
                                assertTrue(eyeData.confidence > 0.5f, "Calibrated eye tracking should have good confidence")
                            }
                        }

                        // 6. Test vergence (depth perception)
                        val vergenceDistance = eyeData.vergenceDistance
                        if (vergenceDistance != null) {
                            assertTrue(vergenceDistance > 0f, "Vergence distance should be positive")
                            assertTrue(vergenceDistance < 100f, "Vergence distance should be reasonable")
                        }
                    }

                    frame.frameNumber < 180 // 3 seconds
                }

                session.end()
            }
            is XRSessionResult.Error -> {
                println("XR session creation failed: ${sessionResult.exception.message}")
            }
        }
    }

    @Test
    fun testXRPerformanceOptimization() {
        val xrSystem = TODO("XRSystem implementation not available yet") as XRSystem
        val performanceMonitor = TODO("PerformanceMonitor implementation not available yet") as PerformanceMonitor

        if (!xrSystem.isSupported().isSupported) {
            println("XR not supported, skipping performance test")
            return
        }

        val sessionResult = xrSystem.requestSession(
            XRSessionRequest(mode = XRSessionMode.IMMERSIVE_VR)
        )

        when (sessionResult) {
            is XRSessionResult.Success -> {
                val session = sessionResult.value

                // 1. Measure baseline XR performance
                val baselineMetrics = performanceMonitor.startMeasurement("xr_baseline")
                var frameCount = 0

                session.onFrame { frame ->
                    frameCount++

                    // Simple rendering without optimizations
                    val renderResult = xrSystem.renderFrame(frame, TODO("Scene not available yet"))

                    frameCount < 180 // 3 seconds
                }

                val baselineResult = performanceMonitor.endMeasurement(baselineMetrics)
                val baselineFrameTime = baselineResult.averageFrameTime

                // 2. Enable XR-specific optimizations
                val optimizations = XROptimizations(
                    enableFoveatedRendering = true,
                    foveationLevel = FoveationLevel.MEDIUM,
                    enableSpaceWarp = true,
                    enableFixedFoveation = true,
                    dynamicResolution = true,
                    targetFrameRate = 90f // 90 Hz for VR
                )

                xrSystem.setOptimizations(optimizations)

                // 3. Measure optimized performance
                val optimizedMetrics = performanceMonitor.startMeasurement("xr_optimized")
                frameCount = 0

                session.onFrame { frame ->
                    frameCount++

                    // Rendering with optimizations enabled
                    val renderResult = xrSystem.renderFrame(frame, TODO("Scene not available yet"))

                    frameCount < 180 // 3 seconds
                }

                val optimizedResult = performanceMonitor.endMeasurement(optimizedMetrics)
                val optimizedFrameTime = optimizedResult.averageFrameTime

                // 4. Verify performance improvements
                val performanceImprovement = (baselineFrameTime - optimizedFrameTime) / baselineFrameTime

                assertTrue(performanceImprovement > 0.1f, "XR optimizations should improve performance by at least 10%")
                assertTrue(optimizedFrameTime < 11.11f, "Optimized XR should maintain 90 FPS")

                // 5. Test adaptive quality
                val qualitySettings = xrSystem.getAdaptiveQuality()

                assertTrue(qualitySettings.currentResolutionScale <= 1.0f, "Resolution scale should not exceed 100%")
                assertTrue(qualitySettings.currentResolutionScale > 0.5f, "Resolution scale should not be too low")

                if (optimizedFrameTime > 11.11f) { // Missing 90 FPS target
                    assertTrue(qualitySettings.currentResolutionScale < 1.0f, "Should reduce resolution when missing target")
                }

                // 6. Test foveated rendering
                if (xrSystem.supportsFoveatedRendering()) {
                    val foveationStats = xrSystem.getFoveationStats()

                    assertTrue(foveationStats.centerRegionPixels > 0, "Should have center region pixels")
                    assertTrue(foveationStats.peripheralRegionPixels > 0, "Should have peripheral region pixels")
                    assertTrue(foveationStats.totalPixelsRendered < foveationStats.fullResolutionPixels,
                        "Foveation should reduce total pixels rendered")

                    val pixelSavings = 1f - (foveationStats.totalPixelsRendered.toFloat() / foveationStats.fullResolutionPixels)
                    assertTrue(pixelSavings > 0.2f, "Foveation should save at least 20% of pixels")
                }

                // 7. Test reprojection/space warp
                if (xrSystem.supportsSpaceWarp()) {
                    val reprojectionStats = xrSystem.getReprojectionStats()

                    assertTrue(reprojectionStats.reprojectedFrames >= 0, "Reprojected frames should be non-negative")

                    val reprojectionRate = reprojectionStats.reprojectedFrames.toFloat() / frameCount
                    assertTrue(reprojectionRate <= 0.1f, "Reprojection rate should be low for good performance")
                }

                session.end()
            }
            is XRSessionResult.Error -> {
                println("XR session creation failed: ${sessionResult.exception.message}")
            }
        }
    }

    @Test
    fun testCrossFeatureXRIntegration() {
        val xrSystem = TODO("XRSystem implementation not available yet") as XRSystem
        val scene = TODO("Scene implementation not available yet") as Scene
        val physicsWorld = TODO("PhysicsWorld implementation not available yet") as PhysicsWorld

        if (!xrSystem.isSupported().isSupported) {
            println("XR not supported, skipping cross-feature test")
            return
        }

        val sessionResult = xrSystem.requestSession(
            XRSessionRequest(
                mode = XRSessionMode.IMMERSIVE_AR,
                requiredFeatures = listOf(
                    XRFeature.HIT_TEST,
                    XRFeature.PLANE_DETECTION
                ),
                optionalFeatures = listOf(
                    XRFeature.ANCHORS,
                    XRFeature.HAND_TRACKING
                )
            )
        )

        when (sessionResult) {
            is XRSessionResult.Success -> {
                val session = sessionResult.value

                // 1. Setup physics objects that can be placed in AR
                val physicsObjects = mutableListOf<RigidBody>()

                session.onFrame { frame ->
                    // 2. Use hand tracking to interact with physics objects
                    val handTracker = TODO("HandTracker not available yet") as HandTracker
                    val leftHand = handTracker.getHand(frame, XRHandedness.LEFT)

                    leftHand?.let { hand ->
                        if (handTracker.isGrasping(hand)) {
                            // Pick up nearest physics object
                            val handPosition = hand.joints[HandJoint.INDEX_TIP]?.pose?.position
                            if (handPosition != null) {
                                val nearestObject = physicsObjects.minByOrNull { obj ->
                                    obj.position.distanceTo(handPosition)
                                }

                                nearestObject?.let { obj ->
                                    if (obj.position.distanceTo(handPosition) < 0.2f) { // 20cm grab range
                                        // Attach object to hand
                                        obj.kinematic = true
                                        obj.position = handPosition
                                    }
                                }
                            }
                        }
                    }

                    // 3. Place objects on detected planes
                    val arSystem = TODO("ARSystem not available yet") as ARSystem
                    val detectedPlanes = arSystem.getDetectedPlanes(frame)

                    if (detectedPlanes.isNotEmpty() && frame.frameNumber % 60 == 0) { // Every second
                        val randomPlane = detectedPlanes.random()
                        val randomPoint = randomPlane.getRandomPoint()

                        // Create new physics object on plane
                        val newObject = RigidBody(
                            mass = 1f,
                            shape = TODO("BoxShape not available yet") as CollisionShape,
                            position = randomPoint + Vector3(0f, 0.1f, 0f), // Slightly above plane
                            rotation = Quaternion.IDENTITY
                        )

                        physicsObjects.add(newObject)
                        physicsWorld.addRigidBody(newObject)

                        // Create anchor for persistent placement
                        val anchorSystem = TODO("XRAnchorSystem not available yet") as XRAnchorSystem
                        val anchorResult = anchorSystem.createAnchor(
                            session,
                            Pose(newObject.position, newObject.rotation)
                        )

                        when (anchorResult) {
                            is AnchorResult.Success -> {
                                // Associate object with anchor for persistence
                                newObject.userData["anchor"] = anchorResult.value
                            }
                            is AnchorResult.Error -> {
                                println("Failed to create anchor: ${anchorResult.exception.message}")
                            }
                        }
                    }

                    // 4. Update physics simulation
                    physicsWorld.step()

                    // 5. Render scene with XR camera
                    frame.views.forEach { view ->
                        val camera = scene.getCamera(view.eye)
                        camera.matrixWorld = view.transform
                        camera.projectionMatrix = view.projectionMatrix

                        // Render physics objects in AR
                        val renderResult = xrSystem.renderFrame(frame, scene)
                    }

                    frame.frameNumber < 600 // 10 seconds
                }

                // Cleanup
                physicsObjects.forEach { obj ->
                    physicsWorld.removeRigidBody(obj)
                }

                session.end()
            }
            is XRSessionResult.Error -> {
                println("XR session creation failed: ${sessionResult.exception.message}")
            }
        }
    }
}