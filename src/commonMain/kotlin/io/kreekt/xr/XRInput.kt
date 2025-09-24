/**
 * XR Input Implementation - Hand Tracking and Eye Gaze
 * Provides comprehensive hand joint tracking, eye gaze, and gesture recognition
 */
package io.kreekt.xr

import io.kreekt.core.math.*
import io.kreekt.core.platform.platformClone
import io.kreekt.core.platform.currentTimeMillis
import io.kreekt.core.platform.platformClone
import kotlinx.coroutines.*
import io.kreekt.core.platform.platformClone
import io.kreekt.core.platform.currentTimeMillis
import io.kreekt.core.platform.platformClone
import kotlin.math.*
import io.kreekt.core.platform.platformClone
import io.kreekt.core.platform.currentTimeMillis
import io.kreekt.core.platform.platformClone
import kotlin.math.PI
import kotlin.math.cos

/**
 * Default implementation of XRHand interface
 * Manages hand joint tracking and gesture detection
 */
class DefaultXRHand(
    private val handedness: XRHandedness
) : XRHand {
    private val _joints: MutableMap<XRHandJoint, XRJointSpace> = mutableMapOf()
    override val joints: Map<XRHandJoint, XRJointSpace> get() = _joints
    var size: Float = 0.18f // Average hand size in meters

    private var _isTracked = false
    private val jointPoses = mutableMapOf<XRHandJoint, XRJointPose>()
    private val gestureDetector = HandGestureDetector(this)
    private var calibrationData: HandCalibrationData? = null

    init {
        initializeJoints()
    }

    fun getJoint(joint: XRHandJoint): XRJointSpace? {
        return _joints[joint]
    }

    fun getJointPose(
        joint: XRHandJoint,
        frame: XRFrame,
        referenceSpace: XRSpace
    ): XRJointPose? {
        if (!isTracked()) return null

        // Get the joint space and create a basic pose
        val jointSpace = _joints[joint] ?: return null
        return frame.getPose(jointSpace, referenceSpace)?.let { pose ->
            // Convert XRPose to XRJointPose
            XRJointPose(
                transform = pose.transform,
                radius = 0.01f, // Default joint radius
                emulatedPosition = pose.emulatedPosition,
                linearVelocity = pose.linearVelocity,
                angularVelocity = pose.angularVelocity
            )
        }
    }

    fun isTracked(): Boolean = _isTracked

    fun updateTracking(tracked: Boolean) {
        _isTracked = tracked
    }

    fun updateJointPose(joint: XRHandJoint, pose: XRJointPose) {
        jointPoses[joint] = pose

        // Update hand size based on wrist to middle finger tip distance
        if (joint == XRHandJoint.MIDDLE_FINGER_TIP) {
            jointPoses[XRHandJoint.WRIST]?.let { wristPose ->
                val distance = wristPose.transform.getTranslation().distanceTo(pose.transform.getTranslation())
                size = distance * 1.1f // Approximate full hand size
            }
        }
    }

    fun getJointPose(joint: XRHandJoint): XRJointPose? = jointPoses[joint]

    fun getAllJointPoses(): Map<XRHandJoint, XRJointPose> = jointPoses.toMap()

    fun detectGestures(): List<HandGesture> {
        return if (isTracked()) {
            gestureDetector.detectCurrentGestures()
        } else {
            emptyList()
        }
    }

    fun calibrate(calibrationData: HandCalibrationData) {
        this.calibrationData = calibrationData
        size = calibrationData.handSize
    }

    private fun initializeJoints() {
        XRHandJoint.values().forEach { joint ->
            _joints[joint] = DefaultXRJointSpace(joint)
        }
    }

    /**
     * Calculate finger curl amount (0 = straight, 1 = fully curled)
     */
    fun getFingerCurl(finger: Finger): Float {
        val joints = getFingerJoints(finger)
        if (joints.size < 4) return 0f

        val jointsList = _joints.keys.toList()
        if (jointsList.size < 4) return 0f

        val metacarpal = jointPoses[jointsList[0]]?.transform?.getTranslation() ?: return 0f
        val proximal = jointPoses[jointsList[1]]?.transform?.getTranslation() ?: return 0f
        val intermediate = jointPoses[jointsList[2]]?.transform?.getTranslation() ?: return 0f
        val distal = jointPoses[jointsList[3]]?.transform?.getTranslation() ?: return 0f

        // Calculate angles between segments
        val angle1 = calculateJointAngle(metacarpal, proximal, intermediate)
        val angle2 = calculateJointAngle(proximal, intermediate, distal)

        // Normalize to 0-1 range (assuming max curl is ~270 degrees total)
        return ((angle1 + angle2) / 270f).coerceIn(0f, 1f)
    }

    /**
     * Get the pinch strength between thumb and a finger (0 = no pinch, 1 = full pinch)
     */
    fun getPinchStrength(finger: Finger = Finger.INDEX): Float {
        val thumbTip = jointPoses[XRHandJoint.THUMB_TIP]?.transform?.getTranslation() ?: return 0f
        val fingerTip = jointPoses[getFingerTipJoint(finger)]?.transform?.getTranslation() ?: return 0f

        val distance = thumbTip.distanceTo(fingerTip)
        val normalizedDistance = (distance / size).coerceIn(0f, 1f)

        // Invert so 0 distance = 1 strength
        return 1f - normalizedDistance
    }

    private fun getFingerJoints(finger: Finger): List<XRHandJoint> {
        return when (finger) {
            Finger.THUMB -> listOf(
                XRHandJoint.THUMB_METACARPAL,
                XRHandJoint.THUMB_PHALANX_PROXIMAL,
                XRHandJoint.THUMB_PHALANX_DISTAL,
                XRHandJoint.THUMB_TIP
            )
            Finger.INDEX -> listOf(
                XRHandJoint.INDEX_FINGER_METACARPAL,
                XRHandJoint.INDEX_FINGER_PHALANX_PROXIMAL,
                XRHandJoint.INDEX_FINGER_PHALANX_INTERMEDIATE,
                XRHandJoint.INDEX_FINGER_PHALANX_DISTAL
            )
            Finger.MIDDLE -> listOf(
                XRHandJoint.MIDDLE_FINGER_METACARPAL,
                XRHandJoint.MIDDLE_FINGER_PHALANX_PROXIMAL,
                XRHandJoint.MIDDLE_FINGER_PHALANX_INTERMEDIATE,
                XRHandJoint.MIDDLE_FINGER_PHALANX_DISTAL
            )
            Finger.RING -> listOf(
                XRHandJoint.RING_FINGER_METACARPAL,
                XRHandJoint.RING_FINGER_PHALANX_PROXIMAL,
                XRHandJoint.RING_FINGER_PHALANX_INTERMEDIATE,
                XRHandJoint.RING_FINGER_PHALANX_DISTAL
            )
            Finger.PINKY -> listOf(
                XRHandJoint.PINKY_FINGER_METACARPAL,
                XRHandJoint.PINKY_FINGER_PHALANX_PROXIMAL,
                XRHandJoint.PINKY_FINGER_PHALANX_INTERMEDIATE,
                XRHandJoint.PINKY_FINGER_PHALANX_DISTAL
            )
        }
    }

    private fun getFingerTipJoint(finger: Finger): XRHandJoint {
        return when (finger) {
            Finger.THUMB -> XRHandJoint.THUMB_TIP
            Finger.INDEX -> XRHandJoint.INDEX_FINGER_TIP
            Finger.MIDDLE -> XRHandJoint.MIDDLE_FINGER_TIP
            Finger.RING -> XRHandJoint.RING_FINGER_TIP
            Finger.PINKY -> XRHandJoint.PINKY_FINGER_TIP
        }
    }

    private fun calculateJointAngle(p1: Vector3, p2: Vector3, p3: Vector3): Float {
        val v1 = p1.clone().sub(p2).normalize()
        val v2 = p3.clone().sub(p2).normalize()
        return acos(v1.dot(v2).coerceIn(-1f, 1f)) * 180f / PI.toFloat()
    }
}

/**
 * Default implementation of XRGaze interface
 * Manages eye tracking and gaze direction
 */
class DefaultXRGaze : XRGaze {
    override val eyeSpace: XRSpace = DefaultXRSpace()
    override var isTracked: Boolean = false
        private set

    private var currentPose: XRPose? = null
    private var gazeDirection: Vector3? = null
    private var convergenceDistance: Float = 1f // Distance to convergence point in meters
    private var eyeOpenness = EyeOpenness(left = 1f, right = 1f)
    private var pupilDilation = PupilDilation(left = 0.5f, right = 0.5f)
    private val calibrationData = EyeCalibrationData()

    override fun getEyePose(frame: XRFrame, referenceSpace: XRSpace): XRPose? {
        if (!isTracked) return null
        return frame.getPose(eyeSpace, referenceSpace)
    }

    override fun getGazeDirection(): Vector3 {
        // Return the cached gaze direction or a default forward direction
        return gazeDirection ?: Vector3(0f, 0f, -1f)
    }

    fun updateTracking(tracked: Boolean) {
        isTracked = tracked
    }

    fun updateGaze(pose: XRPose, direction: Vector3) {
        currentPose = pose
        gazeDirection = direction
    }

    fun updateEyeMetrics(
        openness: EyeOpenness,
        dilation: PupilDilation,
        convergence: Float
    ) {
        eyeOpenness = openness
        pupilDilation = dilation
        convergenceDistance = convergence
    }

    fun getConvergencePoint(frame: XRFrame, referenceSpace: XRSpace): Vector3? {
        val direction = getGazeDirection()
        val pose = getEyePose(frame, referenceSpace) ?: return null

        // Calculate point where eyes converge
        return pose.transform.getTranslation().clone().add(
            direction.multiplyScalar(convergenceDistance)
        )
    }

    fun calibrate(calibrationData: EyeCalibrationData) {
        this.calibrationData.update(calibrationData)
    }

    fun getEyeOpenness(): EyeOpenness = eyeOpenness

    fun getPupilDilation(): PupilDilation = pupilDilation

    fun isBlinking(): Boolean {
        return eyeOpenness.left < 0.2f && eyeOpenness.right < 0.2f
    }

    fun isWinking(): Boolean {
        return (eyeOpenness.left < 0.2f && eyeOpenness.right > 0.8f) ||
                (eyeOpenness.right < 0.2f && eyeOpenness.left > 0.8f)
    }
}

/**
 * Default implementation of XRJointSpace
 */
class DefaultXRJointSpace(
    override val joint: XRHandJoint
) : XRJointSpace {
    override val spaceId: String = "xrjs_${joint}_${currentTimeMillis()}"
}

/**
 * Helper function to create XRJointPose
 */
fun createXRJointPose(
    transform: Matrix4,
    radius: Float = 0.01f,
    emulatedPosition: Boolean = false,
    linearVelocity: Vector3? = null,
    angularVelocity: Vector3? = null
): XRJointPose = XRJointPose(
    transform = transform,
    radius = radius,
    emulatedPosition = emulatedPosition,
    linearVelocity = linearVelocity,
    angularVelocity = angularVelocity
)

/**
 * Hand gesture detector
 */
class HandGestureDetector(
    private val hand: DefaultXRHand
) {
    private val gestureHistory = mutableListOf<HandGesture>()
    private val maxHistorySize = 30 // ~0.5 seconds at 60fps

    fun detectCurrentGestures(): List<HandGesture> {
        val gestures = mutableListOf<HandGesture>()

        // Check static gestures
        if (isThumbsUp()) gestures.add(HandGesture.THUMBS_UP)
        if (isThumbsDown()) gestures.add(HandGesture.THUMBS_DOWN)
        if (isFist()) gestures.add(HandGesture.FIST)
        if (isOpenPalm()) gestures.add(HandGesture.OPEN_PALM)
        if (isPeaceSign()) gestures.add(HandGesture.PEACE)
        if (isOkSign()) gestures.add(HandGesture.OK)
        if (isPointingIndex()) gestures.add(HandGesture.POINT)
        if (isPinching()) gestures.add(HandGesture.PINCH)

        // Check dynamic gestures using history
        updateHistory(gestures)
        if (isWaving()) gestures.add(HandGesture.WAVE)
        if (isSwipingLeft()) gestures.add(HandGesture.SWIPE_LEFT)
        if (isSwipingRight()) gestures.add(HandGesture.SWIPE_RIGHT)
        if (isSwipingUp()) gestures.add(HandGesture.SWIPE_UP)
        if (isSwipingDown()) gestures.add(HandGesture.SWIPE_DOWN)

        return gestures
    }

    private fun isThumbsUp(): Boolean {
        val thumbCurl = hand.getFingerCurl(Finger.THUMB)
        val indexCurl = hand.getFingerCurl(Finger.INDEX)
        val middleCurl = hand.getFingerCurl(Finger.MIDDLE)
        val ringCurl = hand.getFingerCurl(Finger.RING)
        val pinkyCurl = hand.getFingerCurl(Finger.PINKY)

        val thumbTip = hand.getJointPose(XRHandJoint.THUMB_TIP)?.transform?.getTranslation()
        val wrist = hand.getJointPose(XRHandJoint.WRIST)?.transform?.getTranslation()

        if (thumbTip == null || wrist == null) return false

        val thumbUp = thumbTip.y > wrist.y + 0.05f

        return thumbUp && thumbCurl < 0.3f &&
                indexCurl > 0.7f && middleCurl > 0.7f &&
                ringCurl > 0.7f && pinkyCurl > 0.7f
    }

    private fun isThumbsDown(): Boolean {
        val thumbCurl = hand.getFingerCurl(Finger.THUMB)
        val indexCurl = hand.getFingerCurl(Finger.INDEX)
        val middleCurl = hand.getFingerCurl(Finger.MIDDLE)
        val ringCurl = hand.getFingerCurl(Finger.RING)
        val pinkyCurl = hand.getFingerCurl(Finger.PINKY)

        val thumbTip = hand.getJointPose(XRHandJoint.THUMB_TIP)?.transform?.getTranslation()
        val wrist = hand.getJointPose(XRHandJoint.WRIST)?.transform?.getTranslation()

        if (thumbTip == null || wrist == null) return false

        val thumbDown = thumbTip.y < wrist.y - 0.05f

        return thumbDown && thumbCurl < 0.3f &&
                indexCurl > 0.7f && middleCurl > 0.7f &&
                ringCurl > 0.7f && pinkyCurl > 0.7f
    }

    private fun isFist(): Boolean {
        return Finger.values().all { finger ->
            hand.getFingerCurl(finger) > 0.8f
        }
    }

    private fun isOpenPalm(): Boolean {
        return Finger.values().all { finger ->
            hand.getFingerCurl(finger) < 0.2f
        }
    }

    private fun isPeaceSign(): Boolean {
        val indexCurl = hand.getFingerCurl(Finger.INDEX)
        val middleCurl = hand.getFingerCurl(Finger.MIDDLE)
        val ringCurl = hand.getFingerCurl(Finger.RING)
        val pinkyCurl = hand.getFingerCurl(Finger.PINKY)

        return indexCurl < 0.3f && middleCurl < 0.3f &&
                ringCurl > 0.7f && pinkyCurl > 0.7f
    }

    private fun isOkSign(): Boolean {
        val thumbIndexPinch = hand.getPinchStrength(Finger.INDEX)
        val middleCurl = hand.getFingerCurl(Finger.MIDDLE)
        val ringCurl = hand.getFingerCurl(Finger.RING)
        val pinkyCurl = hand.getFingerCurl(Finger.PINKY)

        return thumbIndexPinch > 0.8f &&
                middleCurl < 0.3f && ringCurl < 0.3f && pinkyCurl < 0.3f
    }

    private fun isPointingIndex(): Boolean {
        val indexCurl = hand.getFingerCurl(Finger.INDEX)
        val middleCurl = hand.getFingerCurl(Finger.MIDDLE)
        val ringCurl = hand.getFingerCurl(Finger.RING)
        val pinkyCurl = hand.getFingerCurl(Finger.PINKY)

        return indexCurl < 0.3f &&
                middleCurl > 0.7f && ringCurl > 0.7f && pinkyCurl > 0.7f
    }

    private fun isPinching(): Boolean {
        return hand.getPinchStrength(Finger.INDEX) > 0.8f
    }

    private fun isWaving(): Boolean {
        if (gestureHistory.size < maxHistorySize) return false

        // Check for oscillating hand movement
        val wristPositions = gestureHistory.mapNotNull {
            hand.getJointPose(XRHandJoint.WRIST)?.transform?.getTranslation()
        }

        if (wristPositions.size < 20) return false

        // Calculate movement variance
        val xMovements = wristPositions.windowed(2).map { (p1, p2) ->
            p2.x - p1.x
        }

        val directionChanges = xMovements.windowed(2).count { (m1, m2) ->
            m1 * m2 < 0 // Sign change indicates direction change
        }

        return directionChanges >= 3 // At least 3 direction changes for waving
    }

    private fun isSwipingLeft(): Boolean = isSwipingInDirection(Vector3(-1f, 0f, 0f))
    private fun isSwipingRight(): Boolean = isSwipingInDirection(Vector3(1f, 0f, 0f))
    private fun isSwipingUp(): Boolean = isSwipingInDirection(Vector3(0f, 1f, 0f))
    private fun isSwipingDown(): Boolean = isSwipingInDirection(Vector3(0f, -1f, 0f))

    private fun isSwipingInDirection(direction: Vector3): Boolean {
        if (gestureHistory.size < 10) return false

        val recentPositions = gestureHistory.takeLast(10).mapNotNull {
            hand.getJointPose(XRHandJoint.WRIST)?.transform?.getTranslation()
        }

        if (recentPositions.size < 10) return false

        val movement = recentPositions.last().clone().sub(recentPositions.first())
        val speed = movement.length() / (10f / 60f) // Assuming 60fps

        return movement.normalize().dot(direction) > 0.7f && speed > 0.3f
    }

    private fun updateHistory(currentGestures: List<HandGesture>) {
        gestureHistory.addAll(currentGestures)
        while (gestureHistory.size > maxHistorySize) {
            gestureHistory.removeAt(0)
        }
    }
}

/**
 * Input source manager for unified input handling
 */
class XRInputSourceManager(
    private val session: XRSession
) {
    private val inputSources = mutableMapOf<String, DefaultXRInputSource>()
    private val inputCallbacks = mutableListOf<XRInputCallback>()

    private var primaryHand: DefaultXRHand? = null
    private var secondaryHand: DefaultXRHand? = null
    private var eyeGaze: DefaultXRGaze? = null

    init {
        startInputMonitoring()
    }

    fun getPrimaryHand(): XRHand? = primaryHand
    fun getSecondaryHand(): XRHand? = secondaryHand
    fun getEyeGaze(): XRGaze? = eyeGaze

    fun registerCallback(callback: XRInputCallback) {
        inputCallbacks.add(callback)
    }

    fun unregisterCallback(callback: XRInputCallback) {
        inputCallbacks.remove(callback)
    }

    private fun startInputMonitoring() {
        GlobalScope.launch {
            while (session.state == XRSessionState.ACTIVE) {
                updateInputSources()
                updateHandTracking()
                updateEyeTracking()
                delay(16) // 60Hz update rate
            }
        }
    }

    private suspend fun updateInputSources() {
        // Input sources would normally come from the XR frame or platform-specific APIs
        // For now, we'll just maintain the existing sources
        val currentSources = listOf<XRInputSource>() // Would be populated from platform

        // Check for new sources
        currentSources.forEach { source ->
            val id = getInputSourceId(source)
            if (!inputSources.containsKey(id)) {
                handleNewInputSource(source)
            }
        }

        // Check for removed sources
        val currentIds = currentSources.map { getInputSourceId(it) }.toSet()
        val removedIds = inputSources.keys - currentIds

        removedIds.forEach { id: String ->
            handleRemovedInputSource(id)
        }
    }

    private fun handleNewInputSource(source: XRInputSource) {
        val id = getInputSourceId(source)
        val defaultSource = source as? DefaultXRInputSource ?: return

        inputSources[id] = defaultSource

        // Check if it's a hand
        source.hand?.let { hand ->
            when (source.handedness) {
                XRHandedness.LEFT -> {
                    primaryHand = hand as? DefaultXRHand
                    inputCallbacks.forEach { it.onHandTrackingStarted(hand, XRHandedness.LEFT) }
                }
                XRHandedness.RIGHT -> {
                    secondaryHand = hand as? DefaultXRHand
                    inputCallbacks.forEach { it.onHandTrackingStarted(hand, XRHandedness.RIGHT) }
                }
                else -> {}
            }
        }

        // Eye gaze tracking would be handled separately from input sources
    }

    private fun handleRemovedInputSource(id: String) {
        val source = inputSources.remove(id) ?: return

        source.hand?.let { hand ->
            when (source.handedness) {
                XRHandedness.LEFT -> {
                    primaryHand = null
                    inputCallbacks.forEach { it.onHandTrackingLost(XRHandedness.LEFT) }
                }
                XRHandedness.RIGHT -> {
                    secondaryHand = null
                    inputCallbacks.forEach { it.onHandTrackingLost(XRHandedness.RIGHT) }
                }
                else -> {}
            }
        }

        // Eye gaze tracking cleanup would be handled separately
    }

    private suspend fun updateHandTracking() {
        primaryHand?.let { hand ->
            updateHandJoints(hand)
            val gestures = hand.detectGestures()
            if (gestures.isNotEmpty()) {
                inputCallbacks.forEach {
                    it.onHandGestureDetected(gestures, XRHandedness.LEFT)
                }
            }
        }

        secondaryHand?.let { hand ->
            updateHandJoints(hand)
            val gestures = hand.detectGestures()
            if (gestures.isNotEmpty()) {
                inputCallbacks.forEach {
                    it.onHandGestureDetected(gestures, XRHandedness.RIGHT)
                }
            }
        }
    }

    private suspend fun updateHandJoints(hand: DefaultXRHand) {
        // Update joint poses from platform
        val jointPoses = getPlatformHandJointPoses(hand)
        jointPoses.forEach { entry ->
            hand.updateJointPose(entry.key, entry.value)
        }
    }

    private suspend fun updateEyeTracking() {
        eyeGaze?.let { gaze ->
            val eyeData = getPlatformEyeTrackingData()
            if (eyeData != null) {
                gaze.updateTracking(true)
                gaze.updateGaze(eyeData.pose, eyeData.direction)
                gaze.updateEyeMetrics(
                    eyeData.openness,
                    eyeData.dilation,
                    eyeData.convergence
                )

                inputCallbacks.forEach {
                    it.onEyeGazeUpdated(eyeData.direction, eyeData.convergence)
                }
            } else {
                gaze.updateTracking(false)
            }
        }
    }

    private fun getInputSourceId(source: XRInputSource): String {
        return "${source.handedness}_${source.targetRayMode}_${source.hashCode()}"
    }
}

/**
 * Input callback interface
 */
interface XRInputCallback {
    fun onHandTrackingStarted(hand: XRHand, handedness: XRHandedness)
    fun onHandTrackingLost(handedness: XRHandedness)
    fun onHandGestureDetected(gestures: List<HandGesture>, handedness: XRHandedness)
    fun onEyeTrackingStarted(gaze: XRGaze)
    fun onEyeTrackingLost()
    fun onEyeGazeUpdated(direction: Vector3, convergenceDistance: Float)
}

/**
 * Hand gesture enumeration
 */
enum class HandGesture {
    FIST,
    OPEN_PALM,
    THUMBS_UP,
    THUMBS_DOWN,
    PEACE,
    OK,
    POINT,
    PINCH,
    WAVE,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,
    GRAB,
    RELEASE
}

/**
 * Finger enumeration
 */
enum class Finger {
    THUMB,
    INDEX,
    MIDDLE,
    RING,
    PINKY
}

/**
 * Eye openness data
 */
data class EyeOpenness(
    val left: Float,  // 0 = closed, 1 = fully open
    val right: Float
)

/**
 * Pupil dilation data
 */
data class PupilDilation(
    val left: Float,  // Normalized 0-1
    val right: Float
)

/**
 * Hand calibration data
 */
data class HandCalibrationData(
    val handSize: Float = 0.18f,
    val fingerLengths: Map<Finger, Float> = emptyMap(),
    val palmWidth: Float = 0.08f
)

/**
 * Eye calibration data
 */
data class EyeCalibrationData(
    var interpupillaryDistance: Float = 0.063f, // Average IPD in meters
    var dominantEye: XREye = XREye.RIGHT,
    var calibrationPoints: List<Vector3> = emptyList()
) {
    fun update(other: EyeCalibrationData) {
        interpupillaryDistance = other.interpupillaryDistance
        dominantEye = other.dominantEye
        calibrationPoints = other.calibrationPoints
    }
}

/**
 * Eye tracking data from platform
 */
data class EyeTrackingData(
    val pose: XRPose,
    val direction: Vector3,
    val openness: EyeOpenness,
    val dilation: PupilDilation,
    val convergence: Float
)

// Platform-specific functions (will be implemented via expect/actual)
internal expect suspend fun getPlatformHandJointPoses(
    hand: DefaultXRHand
): Map<XRHandJoint, XRJointPose>

internal expect suspend fun getPlatformEyeTrackingData(): EyeTrackingData?