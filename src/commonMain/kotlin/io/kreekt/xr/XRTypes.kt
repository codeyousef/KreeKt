/**
 * XR Core Types and Interfaces
 * Provides the foundational types for XR functionality
 */
package io.kreekt.xr

import io.kreekt.core.math.Matrix4
import io.kreekt.core.math.Vector2
import io.kreekt.core.math.Vector3

/**
 * Represents an XR device
 */
data class XRDevice(
    val id: String,
    val name: String,
    val capabilities: Set<XRFeature>
)

/**
 * XR System interface for managing XR capabilities
 */
interface XRSystem {
    fun isSupported(): Boolean
    fun getSupportedSessionModes(): List<XRSessionMode>
    fun getSupportedFeatures(mode: XRSessionMode): List<XRFeature>
    fun checkPermissions(features: List<XRFeature>): Map<XRFeature, PermissionState>
    suspend fun requestSession(
        mode: XRSessionMode,
        features: List<XRFeature>
    ): XRResult<XRSession>
}

/**
 * XR Session interface for managing XR sessions
 */


/**
 * XR Space interface representing a coordinate system
 */
interface XRSpace {
    val spaceId: String
}

/**
 * XR Reference Space interface
 */
interface XRReferenceSpace : XRSpace {
    val type: XRReferenceSpaceType
    fun getOffsetReferenceSpace(originOffset: Matrix4): XRReferenceSpace
}

/**
 * XR Bounded Reference Space interface
 */
interface XRBoundedReferenceSpace : XRReferenceSpace {
    val boundsGeometry: List<Vector2>
}

/**
 * XR Layer interface for rendering layers
 */
interface XRLayer {
    val layerId: String
}

/**
 * XR WebGL Layer interface
 */
interface XRWebGLLayer : XRLayer {
    val antialias: Boolean
    val ignoreDepthValues: Boolean
    val framebufferWidth: Int
    val framebufferHeight: Int
}

/**
 * XR Pose interface representing position and orientation
 */
interface XRPose {
    val transform: Matrix4
    val emulatedPosition: Boolean
    val linearVelocity: Vector3?
    val angularVelocity: Vector3?
}

/**
 * XR Viewer Pose interface
 */
interface XRViewerPose : XRPose {
    val views: List<XRView>
}

/**
 * XR View interface representing a single view (eye)
 */
interface XRView {
    val eye: XREye
    val projectionMatrix: Matrix4
    val viewMatrix: Matrix4
    val recommendedViewportScale: Float?
}

/**
 * XR Input Source interface
 */
interface XRInputSource {
    val handedness: XRHandedness
    val targetRayMode: XRTargetRayMode
    val targetRaySpace: XRSpace
    val gripSpace: XRSpace?
    val profiles: List<String>
    val gamepad: XRGamepad?
    val hand: XRHand?
}

/**
 * XR Gamepad interface
 */
interface XRGamepad {
    val connected: Boolean
    val index: Int
    val id: String
    val mapping: String
    val axes: List<Float>
    val buttons: List<XRGamepadButton>
    val hapticActuators: List<XRHapticActuator>
}

/**
 * XR Gamepad Button interface
 */
interface XRGamepadButton {
    val pressed: Boolean
    val touched: Boolean
    val value: Float
}

/**
 * XR Haptic Actuator interface
 */
interface XRHapticActuator {
    fun playHapticEffect(type: String, params: Map<String, Any>)
    fun stopHaptics()
}

/**
 * XR Hand interface for hand tracking
 */
interface XRHand {
    val joints: Map<XRHandJoint, XRJointSpace>
}

/**
 * XR Eye Gaze interface
 */
interface XRGaze {
    val eyeSpace: XRSpace
    val isTracked: Boolean

    fun getEyePose(frame: XRFrame, referenceSpace: XRSpace): XRPose?
    fun getGazeDirection(frame: XRFrame, referenceSpace: XRSpace): Vector3?
}

/**
 * XR Joint Space interface
 */
interface XRJointSpace : XRSpace {
    val joint: XRHandJoint
}

/**
 * XR Joint Pose interface
 */
interface XRJointPose : XRPose {
    val radius: Float
}

/**
 * AR System interface extending XR System
 */
interface ARSystem : XRSystem {
    suspend fun requestHitTestSource(options: XRHitTestOptions): XRResult<XRHitTestSource>
    suspend fun requestHitTestSourceForTransientInput(
        options: XRTransientInputHitTestOptions
    ): XRResult<XRTransientInputHitTestSource>

    fun detectPlanes(): List<XRPlane>
    fun detectMeshes(): List<XRMesh>
    fun detectFaces(): List<XRFace>

    suspend fun createAnchor(pose: XRPose, space: XRSpace): XRResult<XRAnchor>
    fun loadAnchor(id: String): XRAnchor?
    fun getAllAnchors(): List<XRAnchor>

    fun trackImage(imageTarget: XRImageTarget): XRResult<XRTrackedImage>
    fun trackObject(objectTarget: XRObjectTarget): XRResult<XRTrackedObject>

    fun requestLightProbe(): XRLightProbe?
    fun requestEnvironmentProbe(position: Vector3): XREnvironmentProbe?

    suspend fun enableCameraAccess(): XRResult<XRCamera>
    suspend fun enableDepthSensing(options: XRDepthSensingOptions): XRResult<XRDepthSensor>
}


/**
 * XR Hit Test Options
 */
data class XRHitTestOptions(
    val space: XRSpace,
    val entityTypes: Set<XRHitTestEntityType> = setOf(XRHitTestEntityType.PLANE),
    val offsetRay: XRRay? = null
)

/**
 * XR Transient Input Hit Test Options
 */
data class XRTransientInputHitTestOptions(
    val profile: String,
    val entityTypes: Set<XRHitTestEntityType> = setOf(XRHitTestEntityType.PLANE),
    val offsetRay: XRRay? = null
)

/**
 * XR Ray for hit testing
 */
data class XRRay(
    val origin: Vector3,
    val direction: Vector3,
    val matrix: Matrix4 = Matrix4()
)

/**
 * XR Hit Test Source interface
 */
interface XRHitTestSource {
    fun cancel()
}

/**
 * XR Transient Input Hit Test Source interface
 */
interface XRTransientInputHitTestSource {
    fun cancel()
}

/**
 * XR Hit Test Result interface
 */
interface XRHitTestResult {
    fun getPose(baseSpace: XRSpace): XRPose?
    suspend fun createAnchor(): XRAnchor?
}

/**
 * XR Transient Input Hit Test Result interface
 */
interface XRTransientInputHitTestResult {
    val inputSource: XRInputSource
    val results: List<XRHitTestResult>
}

/**
 * XR Anchor interface
 */
interface XRAnchor {
    val anchorId: String
    val anchorSpace: XRSpace
    fun delete()
}

/**
 * XR Plane interface for plane detection
 */
interface XRPlane {
    val planeSpace: XRSpace
    val polygon: List<Vector3>
    val orientation: PlaneOrientation
    val semanticLabel: String?
}

/**
 * XR Mesh interface for mesh detection
 */
interface XRMesh {
    val meshSpace: XRSpace
    val vertices: List<Vector3>
    val indices: List<Int>
    val semanticLabel: String?
}

/**
 * XR Face interface for face tracking
 */
interface XRFace {
    val faceSpace: XRSpace
    val landmarks: Map<String, Vector3>
    val mesh: XRMesh?
}


/**
 * XR Light Estimate interface
 */
interface XRLightEstimate {
    val sphericalHarmonicsCoefficients: List<Vector3>
    val primaryLightDirection: Vector3
    val primaryLightIntensity: Float
}

/**
 * XR Environment Probe interface
 */
interface XREnvironmentProbe {
    val position: Vector3
    val size: Vector3
    val environmentMap: Any? // Platform-specific texture
}

/**
 * XR Image Target for image tracking
 */
interface XRImageTarget {
    val image: Any // Platform-specific image data
    val widthInMeters: Float
}

/**
 * XR Tracked Image interface
 */
interface XRTrackedImage {
    val image: XRImageTarget
    val trackingState: XRTrackingState
    val measuredWidthInMeters: Float
    val imageSpace: XRSpace
    val emulatedPosition: Boolean
}

/**
 * XR Object Target for object tracking
 */
interface XRObjectTarget {
    val targetObject: Any // Platform-specific object data
}

/**
 * XR Tracked Object interface
 */
interface XRTrackedObject {
    val target: XRObjectTarget
    val trackingState: XRTrackingState
    val objectSpace: XRSpace
}

/**
 * XR Camera interface for camera access
 */
interface XRCamera {
    val cameraImage: Any? // Platform-specific image
    val intrinsics: CameraIntrinsics?
}

/**
 * Camera Intrinsics
 */
data class CameraIntrinsics(
    val focalLength: Vector2,
    val principalPoint: Vector2,
    val imageSize: Vector2
)

/**
 * XR Depth Sensor interface
 */
interface XRDepthSensor {
    fun getDepthInformation(view: XRView): XRDepthInfo?
}

/**
 * XR Depth Information interface
 */
interface XRDepthInfo {
    val width: Int
    val height: Int
    val normDepthBufferFromNormView: Matrix4
    val rawValueToMeters: Float
    fun getDepthInMeters(x: Float, y: Float): Float
}

/**
 * XR Depth Sensing Options
 */
data class XRDepthSensingOptions(
    val usagePreference: Set<XRDepthUsage>,
    val dataFormatPreference: Set<XRDepthDataFormat>
)

/**
 * Enumerations
 */

enum class XRSessionMode {
    INLINE,
    IMMERSIVE_VR,
    IMMERSIVE_AR
}

enum class XRSessionState {
    IDLE,
    REQUESTING,
    ACTIVE,
    ENDING,
    ENDED
}

enum class XRReferenceSpaceType {
    VIEWER,
    LOCAL,
    LOCAL_FLOOR,
    BOUNDED_FLOOR,
    UNBOUNDED
}

enum class XRFeature {
    VIEWER,
    LOCAL,
    LOCAL_FLOOR,
    BOUNDED_FLOOR,
    UNBOUNDED,
    ANCHORS,
    HIT_TEST,
    PLANE_DETECTION,
    MESH_DETECTION,
    FACE_TRACKING,
    IMAGE_TRACKING,
    OBJECT_TRACKING,
    HAND_TRACKING,
    EYE_TRACKING,
    DEPTH_SENSING,
    LIGHT_ESTIMATION,
    CAMERA_ACCESS
}

enum class PermissionState {
    GRANTED,
    DENIED,
    PROMPT
}

enum class XREye {
    NONE,
    LEFT,
    RIGHT
}

enum class XRHandedness {
    NONE,
    LEFT,
    RIGHT
}

enum class XRTargetRayMode {
    GAZE,
    TRACKED_POINTER,
    SCREEN
}

enum class XRHandJoint {
    WRIST,
    THUMB_METACARPAL,
    THUMB_PHALANX_PROXIMAL,
    THUMB_PHALANX_DISTAL,
    THUMB_TIP,
    INDEX_FINGER_METACARPAL,
    INDEX_FINGER_PHALANX_PROXIMAL,
    INDEX_FINGER_PHALANX_INTERMEDIATE,
    INDEX_FINGER_PHALANX_DISTAL,
    INDEX_FINGER_TIP,
    MIDDLE_FINGER_METACARPAL,
    MIDDLE_FINGER_PHALANX_PROXIMAL,
    MIDDLE_FINGER_PHALANX_INTERMEDIATE,
    MIDDLE_FINGER_PHALANX_DISTAL,
    MIDDLE_FINGER_TIP,
    RING_FINGER_METACARPAL,
    RING_FINGER_PHALANX_PROXIMAL,
    RING_FINGER_PHALANX_INTERMEDIATE,
    RING_FINGER_PHALANX_DISTAL,
    RING_FINGER_TIP,
    PINKY_FINGER_METACARPAL,
    PINKY_FINGER_PHALANX_PROXIMAL,
    PINKY_FINGER_PHALANX_INTERMEDIATE,
    PINKY_FINGER_PHALANX_DISTAL,
    PINKY_FINGER_TIP
}

enum class XRControllerButton {
    TRIGGER,
    SQUEEZE,
    TOUCHPAD,
    THUMBSTICK,
    BUTTON_A,
    BUTTON_B,
    BUTTON_X,
    BUTTON_Y,
    MENU,
    SYSTEM
}

enum class XRControllerAxis {
    TOUCHPAD_X,
    TOUCHPAD_Y,
    THUMBSTICK_X,
    THUMBSTICK_Y
}

enum class XRHitTestEntityType {
    PLANE,
    POINT,
    MESH
}

enum class PlaneOrientation {
    HORIZONTAL_UP,
    HORIZONTAL_DOWN,
    VERTICAL,
    UNKNOWN
}

enum class XRTrackingState {
    NOT_TRACKING,
    TRACKING,
    LIMITED,
    PAUSED,
    STOPPED
}

enum class XRDepthUsage {
    CPU_OPTIMIZED,
    GPU_OPTIMIZED
}

enum class XRDepthDataFormat {
    LUMINANCE_ALPHA,
    RGBA32F
}


/**
 * Default implementation of XRSystem
 * Provides basic stub implementation for all required methods
 */
open class DefaultXRSystem : XRSystem {
    override fun isSupported(): Boolean = false

    override fun getSupportedSessionModes(): List<XRSessionMode> = emptyList()

    override fun getSupportedFeatures(mode: XRSessionMode): List<XRFeature> = emptyList()

    override fun checkPermissions(features: List<XRFeature>): Map<XRFeature, PermissionState> =
        features.associateWith { PermissionState.DENIED }

    override suspend fun requestSession(
        mode: XRSessionMode,
        features: List<XRFeature>
    ): XRResult<XRSession> = XRResult.Error(XRException.NotSupported("XR not supported in this implementation"))
}