/**
 * WebXR Implementation for Web Platform
 * Provides comprehensive XR support using WebXR API for browsers
 */
package io.kreekt.xr

import io.kreekt.core.math.*
import io.kreekt.renderer.*
import io.kreekt.core.platform.currentTimeMillis
import kotlinx.coroutines.*
import kotlinx.browser.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.khronos.webgl.*
import kotlin.js.*
import kotlin.js.Promise

/**
 * External WebXR API declarations
 */
external interface Navigator {
    val xr: XRSystemNative?
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
external interface XRSystemNative {
    fun isSessionSupported(mode: String): Promise<Boolean>
    fun requestSession(mode: String, options: dynamic = definedExternally): Promise<XRSessionNative>
    val ondevicechange: ((Event) -> Unit)?
}

external interface XRSessionNative {
    val inputSources: XRInputSourceArray
    val renderState: XRRenderState
    val visibilityState: String
    val frameRate: Float?
    val supportedFrameRates: Array<Float>?

    fun requestReferenceSpace(type: String): Promise<XRReferenceSpaceNative>
    fun updateRenderState(state: dynamic)
    fun requestAnimationFrame(callback: (Double, XRFrameNative) -> Unit): Int
    fun cancelAnimationFrame(handle: Int)
    fun end(): Promise<Unit>

    fun requestHitTestSource(options: dynamic): Promise<XRHitTestSourceNative>
    fun requestHitTestSourceForTransientInput(options: dynamic): Promise<XRTransientInputHitTestSourceNative>

    val onend: ((Event) -> Unit)?
    val oninputsourceschange: ((XRInputSourcesChangeEvent) -> Unit)?
    val onselect: ((XRInputSourceEvent) -> Unit)?
    val onselectstart: ((XRInputSourceEvent) -> Unit)?
    val onselectend: ((XRInputSourceEvent) -> Unit)?
    val onsqueeze: ((XRInputSourceEvent) -> Unit)?
    val onsqueezestart: ((XRInputSourceEvent) -> Unit)?
    val onsqueezeend: ((XRInputSourceEvent) -> Unit)?
    val onvisibilitychange: ((Event) -> Unit)?
}

external interface XRRenderState {
    val depthNear: Float
    val depthFar: Float
    val inlineVerticalFieldOfView: Float?
    val baseLayer: XRWebGLLayerNative?
}

external interface XRFrameNative {
    val session: XRSessionNative
    val predictedDisplayTime: Double

    fun getViewerPose(referenceSpace: XRReferenceSpaceNative): XRViewerPoseNative?
    fun getPose(space: XRSpaceNative, baseSpace: XRSpaceNative): XRPoseNative?
    fun getJointPose(joint: XRJointSpaceNative, baseSpace: XRSpaceNative): XRJointPoseNative?
    fun getHitTestResults(source: XRHitTestSourceNative): Array<XRHitTestResultNative>
    fun getHitTestResultsForTransientInput(source: XRTransientInputHitTestSourceNative): Array<XRTransientInputHitTestResultNative>
    fun getLightEstimate(lightProbe: XRLightProbeNative): XRLightEstimateNative?
    fun getDepthInformation(view: XRViewNative): XRDepthInformationNative?
    fun createAnchor(transform: XRRigidTransform, space: XRSpaceNative): Promise<XRAnchorNative>
}

external interface XRSpaceNative

external interface XRReferenceSpaceNative : XRSpaceNative {
    fun getOffsetReferenceSpace(originOffset: XRRigidTransform): XRReferenceSpaceNative
}

external interface XRBoundedReferenceSpaceNative : XRReferenceSpaceNative {
    val boundsGeometry: Array<DOMPointReadOnly>
}

external interface XRViewNative {
    val eye: String
    val projectionMatrix: Float32Array
    val transform: XRRigidTransform
    val recommendedViewportScale: Float?

    fun requestViewportScale(scale: Float?)
}

external interface XRViewerPoseNative : XRPoseNative {
    val views: Array<XRViewNative>
}

external interface XRPoseNative {
    val transform: XRRigidTransform
    val linearVelocity: DOMPointReadOnly?
    val angularVelocity: DOMPointReadOnly?
    val emulatedPosition: Boolean
}

external class XRRigidTransform {
    val position: DOMPointReadOnly
    val orientation: DOMPointReadOnly
    val matrix: Float32Array
    val inverse: XRRigidTransform

    constructor(position: dynamic = definedExternally, orientation: dynamic = definedExternally)
}

external interface DOMPointReadOnly {
    val x: Float
    val y: Float
    val z: Float
    val w: Float
}

external interface XRInputSourceArray {
    val length: Int
    fun item(index: Int): XRInputSourceNative
}

external interface XRInputSourceNative {
    val handedness: String
    val targetRayMode: String
    val targetRaySpace: XRSpaceNative
    val gripSpace: XRSpaceNative?
    val gamepad: Gamepad?
    val profiles: Array<String>
    val hand: XRHandNative?
}

external interface XRHandNative {
    val size: Int
    fun get(joint: String): XRJointSpaceNative?
}

external interface XRJointSpaceNative : XRSpaceNative {
    val jointName: String
}

external interface XRJointPoseNative : XRPoseNative {
    val radius: Float
}

external interface XRInputSourcesChangeEvent {
    val session: XRSessionNative
    val added: Array<XRInputSourceNative>
    val removed: Array<XRInputSourceNative>
}

external interface XRInputSourceEvent {
    val frame: XRFrameNative
    val inputSource: XRInputSourceNative
}

external class XRWebGLLayerNative {
    val antialias: Boolean
    val ignoreDepthValues: Boolean
    val framebuffer: WebGLFramebuffer?
    val framebufferWidth: Int
    val framebufferHeight: Int

    constructor(session: XRSessionNative, context: WebGLRenderingContext, options: dynamic = definedExternally)
}

external interface XRHitTestSourceNative {
    fun cancel()
}

external interface XRTransientInputHitTestSourceNative {
    fun cancel()
}

external interface XRHitTestResultNative {
    fun getPose(baseSpace: XRSpaceNative): XRPoseNative?
    fun createAnchor(): Promise<XRAnchorNative>
}

external interface XRTransientInputHitTestResultNative {
    val inputSource: XRInputSourceNative
    val results: Array<XRHitTestResultNative>
}

external interface XRAnchorNative {
    val anchorSpace: XRSpaceNative
    fun delete()
}

external interface XRLightProbeNative {
    val probeSpace: XRSpaceNative
    val onreflectionchange: (() -> Unit)?
}

external interface XRLightEstimateNative {
    val sphericalHarmonicsCoefficients: Float32Array
    val primaryLightDirection: DOMPointReadOnly
    val primaryLightIntensity: DOMPointReadOnly
}

external interface XRDepthInformationNative {
    val width: Int
    val height: Int
    val normDepthBufferFromNormView: XRRigidTransform
    val rawValueToMeters: Float

    fun getDepthInMeters(x: Float, y: Float): Float
}

actual typealias XRDepthInformation = XRDepthInformationNative

external interface Gamepad {
    val id: String
    val index: Int
    val connected: Boolean
    val timestamp: Double
    val mapping: String
    val axes: Array<Float>
    val buttons: Array<GamepadButton>
    val hapticActuators: Array<GamepadHapticActuator>?
}

external interface GamepadButton {
    val pressed: Boolean
    val touched: Boolean
    val value: Float
}

external interface GamepadHapticActuator {
    val type: String
    fun pulse(value: Float, duration: Float): Promise<Boolean>
}

/**
 * WebXR implementation of XRSystem expect functions
 */
@JsName("isPlatformXRSupported")
actual fun isPlatformXRSupported(): Boolean {
    val nav = window.navigator.asDynamic()
    return nav.xr != undefined
}

@JsName("isWebXRSupported")
actual fun isWebXRSupported(): Boolean {
    return isPlatformXRSupported()
}

@JsName("isImmersiveVRSupported")
actual fun isImmersiveVRSupported(): Boolean {
    // Note: WebXR support check is async, returning false for now
    // Should be replaced with suspend function or cached result
    return false
}

@JsName("isImmersiveARSupported")
actual fun isImmersiveARSupported(): Boolean {
    // Note: WebXR support check is async, returning false for now
    // Should be replaced with suspend function or cached result
    return false
}

@JsName("getPlatformXRDevices")
actual fun getPlatformXRDevices(): List<XRDevice> {
    // WebXR doesn't expose device enumeration directly
    // We create virtual devices based on supported session modes
    val devices = mutableListOf<XRDevice>()

    if (isImmersiveVRSupported()) {
        devices.add(XRDevice(
            id = "webxr-vr",
            name = "WebXR VR Device",
            capabilities = XRDeviceCapabilities(
                supportedModes = setOf(XRSessionMode.IMMERSIVE_VR, XRSessionMode.INLINE),
                supportsHandTracking = checkHandTrackingSupport(),
                supportsEyeTracking = checkEyeTrackingSupport(),
                supportedControllers = 2,
                hasPositionalTracking = true,
                hasRotationalTracking = true,
                fieldOfView = XRFieldOfView(90f, 90f, 90f, 90f)
            )
        ))
    }

    if (isImmersiveARSupported()) {
        devices.add(XRDevice(
            id = "webxr-ar",
            name = "WebXR AR Device",
            capabilities = XRDeviceCapabilities(
                supportedModes = setOf(XRSessionMode.IMMERSIVE_AR, XRSessionMode.INLINE),
                supportsHandTracking = checkHandTrackingSupport(),
                supportsEyeTracking = false,
                supportedControllers = 1,
                hasPositionalTracking = true,
                hasRotationalTracking = true,
                fieldOfView = XRFieldOfView(60f, 60f, 60f, 60f)
            )
        ))
    }

    // Always add inline device
    devices.add(XRDevice(
        id = "webxr-inline",
        name = "WebXR Inline Device",
        capabilities = XRDeviceCapabilities(
            supportedModes = setOf(XRSessionMode.INLINE),
            supportsHandTracking = false,
            supportsEyeTracking = false,
            supportedControllers = 0,
            hasPositionalTracking = false,
            hasRotationalTracking = false,
            fieldOfView = null
        )
    ))

    return devices
}

private fun checkHandTrackingSupport(): Boolean {
    // Check if hand tracking is available
    return js("'XRHand' in window") as Boolean
}

private fun checkEyeTrackingSupport(): Boolean {
    // Eye tracking requires specific permission and feature
    return false // Not widely supported yet
}

@JsName("checkCameraPermission")
actual fun checkCameraPermission(): PermissionState {
    return try {
        val permission = window.navigator.asDynamic().permissions
        if (permission != undefined) {
            // Web permissions API - would need async handling in real implementation
            // For now, return GRANTED to allow XR to work
            PermissionState.GRANTED
        } else {
            PermissionState.GRANTED
        }
    } catch (e: Throwable) {
        PermissionState.GRANTED
    }
}

@JsName("checkEyeTrackingPermission")
actual fun checkEyeTrackingPermission(): PermissionState {
    // Eye tracking requires specific permission
    return PermissionState.PROMPT
}

@JsName("checkHandTrackingPermission")
actual fun checkHandTrackingPermission(): PermissionState {
    // Hand tracking doesn't require specific permission in WebXR
    return if (checkHandTrackingSupport()) {
        PermissionState.GRANTED
    } else {
        PermissionState.DENIED
    }
}

/**
 * WebXR implementation for getting viewer pose
 */
@JsName("getPlatformViewerPose")
actual suspend fun getPlatformViewerPose(referenceSpace: XRReferenceSpace): XRViewerPose? {
    val webXRSpace = (referenceSpace as? WebXRReferenceSpace)?.nativeSpace ?: return null
    val session = WebXRSessionManager.currentSession ?: return null
    val frame = WebXRSessionManager.currentFrame ?: return null

    val nativePose = frame.getViewerPose(webXRSpace) ?: return null

    return convertNativeViewerPose(nativePose)
}

/**
 * WebXR implementation for getting input pose
 */
@JsName("getPlatformInputPose")
actual suspend fun getPlatformInputPose(
    inputSource: XRInputSource,
    referenceSpace: XRReferenceSpace
): XRPose? {
    val webXRSpace = (referenceSpace as? WebXRReferenceSpace)?.nativeSpace ?: return null
    val webXRInput = (inputSource as? WebXRInputSource) ?: return null
    val frame = WebXRSessionManager.currentFrame ?: return null

    val targetRayPose = frame.getPose(webXRInput.nativeSource.targetRaySpace, webXRSpace)

    return targetRayPose?.let { convertNativePose(it) }
}

/**
 * WebXR implementation for getting joint pose
 */
@JsName("getPlatformJointPose")
actual suspend fun getPlatformJointPose(joint: XRJointSpace, baseSpace: XRSpace): XRJointPose? {
    val webXRJoint = (joint as? WebXRJointSpace)?.nativeJoint ?: return null
    val webXRSpace = (baseSpace as? WebXRSpace)?.let { space ->
        when (space) {
            is WebXRReferenceSpace -> space.nativeSpace
            is WebXRJointSpace -> space.nativeJoint
            else -> null
        }
    } ?: return null

    val frame = WebXRSessionManager.currentFrame ?: return null
    val nativePose = frame.getJointPose(webXRJoint, webXRSpace as XRSpaceNative) ?: return null

    return convertNativeJointPose(nativePose)
}

/**
 * WebXR implementation for detecting input sources
 */
@JsName("detectPlatformInputSources")
actual fun detectPlatformInputSources(): List<XRInputSource> {
    val session = WebXRSessionManager.currentSession ?: return emptyList()
    val sources = mutableListOf<XRInputSource>()

    for (i in 0 until session.inputSources.length) {
        val nativeSource = session.inputSources.item(i)
        sources.add(WebXRInputSource(nativeSource))
    }

    return sources
}

/**
 * WebXR implementation for getting bounds
 */
@JsName("getBoundsFromPlatform")
actual fun getBoundsFromPlatform(): List<Vector2> {
    // Get bounds from bounded reference space if available
    val boundedSpace = WebXRSessionManager.boundedReferenceSpace
    return boundedSpace?.boundsGeometry?.map { point ->
        Vector2(point.x, point.z)
    } ?: emptyList()
}

/**
 * WebXR implementation for calculating relative pose
 */
@JsName("calculateRelativePose")
actual fun calculateRelativePose(space: XRSpace, baseSpace: XRSpace): XRPose? {
    val frame = WebXRSessionManager.currentFrame ?: return null

    val nativeSpace = when (space) {
        is WebXRReferenceSpace -> space.nativeSpace
        is WebXRJointSpace -> space.nativeJoint
        else -> return null
    }

    val nativeBaseSpace = when (baseSpace) {
        is WebXRReferenceSpace -> baseSpace.nativeSpace
        is WebXRJointSpace -> baseSpace.nativeJoint
        else -> return null
    }

    val pose = frame.getPose(nativeSpace, nativeBaseSpace) ?: return null
    return convertNativePose(pose)
}

/**
 * WebXR implementation for transient hit test results
 */
@JsName("getPlatformTransientHitTestResults")
actual fun getPlatformTransientHitTestResults(
    source: XRTransientInputHitTestSource
): List<XRTransientInputHitTestResult> {
    val webXRSource = (source as? WebXRTransientInputHitTestSource) ?: return emptyList()
    val frame = WebXRSessionManager.currentFrame ?: return emptyList()

    val nativeResults = frame.getHitTestResultsForTransientInput(webXRSource.nativeSource)

    return nativeResults.map { nativeResult ->
        WebXRTransientInputHitTestResult(nativeResult)
    }
}

/**
 * WebXR implementation for light estimation
 */
@JsName("getPlatformLightEstimate")
actual fun getPlatformLightEstimate(lightProbe: XRLightProbe): XRLightEstimate? {
    val webXRProbe = (lightProbe as? WebXRLightProbe) ?: return null
    val frame = WebXRSessionManager.currentFrame ?: return null

    val nativeEstimate = frame.getLightEstimate(webXRProbe.nativeProbe) ?: return null

    return WebXRLightEstimate(nativeEstimate)
}

/**
 * WebXR implementation for combining transforms
 */
@JsName("combineTransforms")
actual fun combineTransforms(first: XRPose, second: XRPose): XRPose {
    val combinedMatrix = first.transform.multiply(second.transform)
    val position = Vector3(combinedMatrix.m03, combinedMatrix.m13, combinedMatrix.m23)
    val rotation = combinedMatrix.extractRotation()

    return XRPose(
        transform = combinedMatrix,
        linearVelocity = first.linearVelocity?.add(second.linearVelocity ?: Vector3.ZERO),
        angularVelocity = first.angularVelocity?.add(second.angularVelocity ?: Vector3.ZERO)
    )
}

/**
 * WebXR Session Manager
 */
object WebXRSessionManager {
    var currentSession: XRSessionNative? = null
    var currentFrame: XRFrameNative? = null
    var boundedReferenceSpace: XRBoundedReferenceSpaceNative? = null
    private val referenceSpaces = mutableMapOf<XRReferenceSpaceType, XRReferenceSpaceNative>()

    suspend fun createSession(mode: XRSessionMode, features: List<XRFeature>): XRSessionNative {
        val xr = (window.navigator.asDynamic().xr as? XRSystemNative)
            ?: throw XRException.NotSupported("WebXR not available")

        val sessionMode = when (mode) {
            XRSessionMode.INLINE -> "inline"
            XRSessionMode.IMMERSIVE_VR -> "immersive-vr"
            XRSessionMode.IMMERSIVE_AR -> "immersive-ar"
        }

        val requiredFeatures = mutableListOf<String>()
        val optionalFeatures = mutableListOf<String>()

        features.forEach { feature ->
            val featureName = mapFeatureToWebXR(feature)
            if (isRequiredFeature(feature)) {
                requiredFeatures.add(featureName)
            } else {
                optionalFeatures.add(featureName)
            }
        }

        val options = js("{}")
        options.requiredFeatures = requiredFeatures.toTypedArray()
        options.optionalFeatures = optionalFeatures.toTypedArray()

        currentSession = xr.requestSession(sessionMode, options).await()
        return currentSession!!
    }

    suspend fun requestReferenceSpace(type: XRReferenceSpaceType): XRReferenceSpaceNative {
        val session = currentSession ?: throw XRException.SessionEnded("No active session")

        val spaceType = when (type) {
            XRReferenceSpaceType.VIEWER -> "viewer"
            XRReferenceSpaceType.LOCAL -> "local"
            XRReferenceSpaceType.LOCAL_FLOOR -> "local-floor"
            XRReferenceSpaceType.BOUNDED_FLOOR -> "bounded-floor"
            XRReferenceSpaceType.UNBOUNDED -> "unbounded"
        }

        val space = session.requestReferenceSpace(spaceType).await()
        referenceSpaces[type] = space

        if (space.asDynamic().boundsGeometry != undefined) {
            boundedReferenceSpace = space as XRBoundedReferenceSpaceNative
        }

        return space
    }

    fun updateFrame(frame: XRFrameNative) {
        currentFrame = frame
    }

    fun getReferenceSpace(type: XRReferenceSpaceType): XRReferenceSpaceNative? {
        return referenceSpaces[type]
    }

    fun clear() {
        currentSession = null
        currentFrame = null
        boundedReferenceSpace = null
        referenceSpaces.clear()
    }

    private fun mapFeatureToWebXR(feature: XRFeature): String {
        return when (feature) {
            XRFeature.VIEWER -> "viewer"
            XRFeature.LOCAL -> "local"
            XRFeature.LOCAL_FLOOR -> "local-floor"
            XRFeature.BOUNDED_FLOOR -> "bounded-floor"
            XRFeature.UNBOUNDED -> "unbounded"
            XRFeature.HIT_TEST -> "hit-test"
            XRFeature.ANCHORS -> "anchors"
            XRFeature.HAND_TRACKING -> "hand-tracking"
            XRFeature.EYE_TRACKING -> "eye-tracking"
            XRFeature.PLANE_DETECTION -> "plane-detection"
            XRFeature.MESH_DETECTION -> "mesh-detection"
            XRFeature.FACE_TRACKING -> "face-tracking"
            XRFeature.IMAGE_TRACKING -> "image-tracking"
            XRFeature.OBJECT_TRACKING -> "object-tracking"
            XRFeature.LIGHT_ESTIMATION -> "lighting-estimation"
            XRFeature.DEPTH_SENSING -> "depth-sensing"
            XRFeature.CAMERA_ACCESS -> "camera-access"
            XRFeature.DOM_OVERLAY -> "dom-overlay"
            XRFeature.LAYERS -> "layers"
        }
    }

    private fun isRequiredFeature(feature: XRFeature): Boolean {
        return when (feature) {
            XRFeature.VIEWER, XRFeature.LOCAL -> true
            else -> false
        }
    }
}


/**
 * WebXR Reference Space implementation
 */
class WebXRReferenceSpace(
    override val type: XRReferenceSpaceType,
    val nativeSpace: XRReferenceSpaceNative
) : XRReferenceSpace {
    override val spaceId = "webxr_space_${currentTimeMillis()}"

    override fun getOffsetReferenceSpace(originOffset: Matrix4): XRReferenceSpace {
        val position = originOffset.getTranslation()
        val rotation = originOffset.getRotation()

        // Create a dynamic object for position and orientation
        val positionObj = js("{}")
        positionObj.x = position.x
        positionObj.y = position.y
        positionObj.z = position.z

        val orientationObj = js("{}")
        orientationObj.x = rotation.x
        orientationObj.y = rotation.y
        orientationObj.z = rotation.z
        orientationObj.w = rotation.w

        val rigidTransform = XRRigidTransform(positionObj, orientationObj)

        val offsetSpace = nativeSpace.getOffsetReferenceSpace(rigidTransform)
        return WebXRReferenceSpace(type, offsetSpace)
    }
}

/**
 * WebXR Input Source implementation
 */
class WebXRInputSource(
    val nativeSource: XRInputSourceNative
) : XRInputSource {
    override val handedness: XRHandedness = when (nativeSource.handedness) {
        "left" -> XRHandedness.LEFT
        "right" -> XRHandedness.RIGHT
        else -> XRHandedness.NONE
    }

    override val targetRayMode: XRTargetRayMode = when (nativeSource.targetRayMode) {
        "gaze" -> XRTargetRayMode.GAZE
        "tracked-pointer" -> XRTargetRayMode.TRACKED_POINTER
        "screen" -> XRTargetRayMode.SCREEN
        else -> XRTargetRayMode.GAZE
    }

    override val targetRaySpace = WebXRSpace(nativeSource.targetRaySpace)
    override val gripSpace: XRSpace? = nativeSource.gripSpace?.let { WebXRSpace(it) }
    override val profiles = nativeSource.profiles.toList()

    override val gamepad: XRGamepad? = nativeSource.gamepad?.let { WebXRGamepad(it) }
    override val hand: XRHand? = nativeSource.hand?.let { WebXRHand(it) }

    fun vibrate(duration: Float, intensity: Float): Boolean {
        // WebXR doesn't directly support vibration on input sources
        // Would need to use Gamepad API if available
        return false
    }
}

/**
 * WebXR Space implementation
 */
class WebXRSpace(
    val nativeSpace: XRSpaceNative
) : XRSpace {
    override val spaceId = "webxr_space_${currentTimeMillis()}"
}

/**
 * WebXR Joint Space implementation
 */
class WebXRJointSpace(
    val nativeJoint: XRJointSpaceNative,
    override val joint: XRHandJoint
) : XRJointSpace {
    override val spaceId = "webxr_joint_${joint.name}"
}

/**
 * WebXR Gamepad implementation
 */
class WebXRGamepad(
    private val nativeGamepad: Gamepad
) : XRGamepad {
    override val id = nativeGamepad.id
    override val index = nativeGamepad.index
    override val connected = nativeGamepad.connected
    override val mapping = nativeGamepad.mapping
    override val axes = nativeGamepad.axes.toList()

    override val buttons: List<XRGamepadButton> = nativeGamepad.buttons.map { button ->
        WebXRGamepadButton(button)
    }

    override val hapticActuators: List<XRHapticActuator> = nativeGamepad.hapticActuators?.map { actuator ->
        WebXRHapticActuator(actuator)
    } ?: emptyList()
}

/**
 * WebXR Gamepad Button implementation
 */
class WebXRGamepadButton(
    private val nativeButton: GamepadButton
) : XRGamepadButton {
    override val pressed = nativeButton.pressed
    override val touched = nativeButton.touched
    override val value = nativeButton.value
}

/**
 * WebXR Haptic Actuator implementation
 */
class WebXRHapticActuator(
    private val nativeActuator: GamepadHapticActuator
) : XRHapticActuator {
    override fun playHapticEffect(type: String, params: Map<String, Any>) {
        // WebXR uses pulse for haptic feedback
        val intensity = (params["intensity"] as? Number)?.toFloat() ?: 0.5f
        val duration = (params["duration"] as? Number)?.toFloat() ?: 100f
        nativeActuator.pulse(intensity, duration)
    }

    override fun stopHaptics() {
        // WebXR doesn't have a direct stop method
        // Pulse with 0 intensity to stop
        nativeActuator.pulse(0.0f, 0.0f)
    }
}

/**
 * WebXR Hand implementation
 */
class WebXRHand(
    private val nativeHand: XRHandNative
) : XRHand {
    override val joints: Map<XRHandJoint, XRJointSpace>

    init {
        // Map all hand joints
        val jointsMap = mutableMapOf<XRHandJoint, XRJointSpace>()
        XRHandJoint.values().forEach { joint ->
            val jointName = getWebXRJointName(joint)
            nativeHand.get(jointName)?.let { nativeJoint ->
                jointsMap[joint] = WebXRJointSpace(nativeJoint, joint)
            }
        }
        joints = jointsMap
    }

    private fun getWebXRJointName(joint: XRHandJoint): String {
        return joint.name.lowercase().replace('_', '-')
    }
}

/**
 * WebXR Transient Input Hit Test Source
 */
class WebXRTransientInputHitTestSource(
    val nativeSource: XRTransientInputHitTestSourceNative
) : XRTransientInputHitTestSource {
    override fun cancel() {
        nativeSource.cancel()
    }
}

/**
 * WebXR Transient Input Hit Test Result
 */
class WebXRTransientInputHitTestResult(
    private val nativeResult: XRTransientInputHitTestResultNative
) : XRTransientInputHitTestResult {
    override val inputSource = WebXRInputSource(nativeResult.inputSource)
    override val results: List<XRHitTestResult> = nativeResult.results.map { result ->
        WebXRHitTestResult(result)
    }
}

/**
 * WebXR Hit Test Result
 */
class WebXRHitTestResult(
    private val nativeResult: XRHitTestResultNative
) : XRHitTestResult {
    override fun getPose(baseSpace: XRSpace): XRPose? {
        val nativeSpace = when (baseSpace) {
            is WebXRReferenceSpace -> baseSpace.nativeSpace
            is WebXRSpace -> baseSpace.nativeSpace
            else -> return null
        }

        val nativePose = nativeResult.getPose(nativeSpace) ?: return null
        return convertNativePose(nativePose)
    }

    override suspend fun createAnchor(): XRAnchor? {
        return try {
            val nativeAnchor = nativeResult.createAnchor().await()
            WebXRAnchor(nativeAnchor)
        } catch (e: Throwable) {
            null
        }
    }
}

/**
 * WebXR Anchor
 */
class WebXRAnchor(
    private val nativeAnchor: XRAnchorNative
) : XRAnchor {
    override val anchorId = "webxr_anchor_${currentTimeMillis()}"
    override val anchorSpace = WebXRSpace(nativeAnchor.anchorSpace)

    override fun delete() {
        nativeAnchor.delete()
    }
}

/**
 * WebXR Light Probe
 */
class WebXRLightProbe(
    val nativeProbe: XRLightProbeNative
) : XRLightProbe {
    override val probeSpace = WebXRSpace(nativeProbe.probeSpace)

    private val listeners = mutableMapOf<String, MutableList<(Any) -> Unit>>()

    override fun addEventListener(type: String, listener: (Any) -> Unit) {
        listeners.getOrPut(type) { mutableListOf() }.add(listener)
        // WebXR specific event handling
        when (type) {
            "reflectionchange" -> {
                // Can't reassign onreflectionchange directly
                // Would need to use addEventListener on native object
            }
        }
    }

    override fun removeEventListener(type: String, listener: (Any) -> Unit) {
        listeners[type]?.remove(listener)
    }
}

/**
 * WebXR Light Estimate
 */
class WebXRLightEstimate(
    private val nativeEstimate: XRLightEstimateNative
) : XRLightEstimate {
    override val sphericalHarmonicsCoefficients: List<Vector3> = run {
        val coeffs = nativeEstimate.sphericalHarmonicsCoefficients.unsafeCast<FloatArray>()
        val vectors = mutableListOf<Vector3>()
        // Convert float array to Vector3 list (groups of 3)
        for (i in coeffs.indices step 3) {
            if (i + 2 < coeffs.size) {
                vectors.add(Vector3(coeffs[i], coeffs[i + 1], coeffs[i + 2]))
            }
        }
        vectors
    }

    override val primaryLightDirection = Vector3(
        nativeEstimate.primaryLightDirection.x,
        nativeEstimate.primaryLightDirection.y,
        nativeEstimate.primaryLightDirection.z
    )

    override val primaryLightIntensity: Float =
        // Average the intensity components or use magnitude
        (nativeEstimate.primaryLightIntensity.x +
         nativeEstimate.primaryLightIntensity.y +
         nativeEstimate.primaryLightIntensity.z) / 3.0f
}

// Conversion helper functions
private fun convertNativePose(nativePose: XRPoseNative): XRPose {
    val transform = convertRigidTransform(nativePose.transform)
    val position = Vector3(
        nativePose.transform.position.x,
        nativePose.transform.position.y,
        nativePose.transform.position.z
    )
    val orientation = Quaternion(
        nativePose.transform.orientation.x,
        nativePose.transform.orientation.y,
        nativePose.transform.orientation.z,
        nativePose.transform.orientation.w
    )

    val linearVelocity = nativePose.linearVelocity?.let {
        Vector3(it.x, it.y, it.z)
    }

    val angularVelocity = nativePose.angularVelocity?.let {
        Vector3(it.x, it.y, it.z)
    }

    return XRPose(transform, linearVelocity = linearVelocity, angularVelocity = angularVelocity)
}

private fun convertNativeViewerPose(nativePose: XRViewerPoseNative): XRViewerPose {
    val transform = convertRigidTransform(nativePose.transform)
    val views = nativePose.views.map { view ->
        convertNativeView(view)
    }

    // Check if position is emulated (not from tracking)
    val emulatedPosition = false // WebXR doesn't provide this info directly

    return XRViewerPose(transform, emulatedPosition, views)
}

private fun convertNativeJointPose(nativePose: XRJointPoseNative): XRJointPose {
    val pose = convertNativePose(nativePose)
    return XRJointPose(
        transform = pose.transform,
        radius = nativePose.radius,
        emulatedPosition = pose.emulatedPosition,
        linearVelocity = pose.linearVelocity,
        angularVelocity = pose.angularVelocity
    )
}

private fun convertNativeView(nativeView: XRViewNative): XRView {
    val eye = when (nativeView.eye) {
        "left" -> XREye.LEFT
        "right" -> XREye.RIGHT
        else -> XREye.NONE
    }

    val projectionMatrix = Matrix4(floatArrayOf(
        nativeView.projectionMatrix[0], nativeView.projectionMatrix[1], nativeView.projectionMatrix[2], nativeView.projectionMatrix[3],
        nativeView.projectionMatrix[4], nativeView.projectionMatrix[5], nativeView.projectionMatrix[6], nativeView.projectionMatrix[7],
        nativeView.projectionMatrix[8], nativeView.projectionMatrix[9], nativeView.projectionMatrix[10], nativeView.projectionMatrix[11],
        nativeView.projectionMatrix[12], nativeView.projectionMatrix[13], nativeView.projectionMatrix[14], nativeView.projectionMatrix[15]
    ))

    val viewMatrix = convertRigidTransform(nativeView.transform)

    return DefaultXRView(eye, projectionMatrix, viewMatrix, nativeView.recommendedViewportScale)
}

private fun convertRigidTransform(rigidTransform: XRRigidTransform): Matrix4 {
    return Matrix4(floatArrayOf(
        rigidTransform.matrix[0], rigidTransform.matrix[1], rigidTransform.matrix[2], rigidTransform.matrix[3],
        rigidTransform.matrix[4], rigidTransform.matrix[5], rigidTransform.matrix[6], rigidTransform.matrix[7],
        rigidTransform.matrix[8], rigidTransform.matrix[9], rigidTransform.matrix[10], rigidTransform.matrix[11],
        rigidTransform.matrix[12], rigidTransform.matrix[13], rigidTransform.matrix[14], rigidTransform.matrix[15]
    ))
}

// Default implementations for XR types
data class DefaultXRView(
    override val eye: XREye,
    override val projectionMatrix: Matrix4,
    override val viewMatrix: Matrix4,
    override val recommendedViewportScale: Float?
) : XRView

// Extension function for Matrix4
private fun Matrix4.extractRotation(): Quaternion {
    val trace = m00 + m11 + m22

    return when {
        trace > 0 -> {
            val s = 0.5f / kotlin.math.sqrt(trace + 1f)
            Quaternion(
                (m21 - m12) * s,
                (m02 - m20) * s,
                (m10 - m01) * s,
                0.25f / s
            )
        }
        m00 > m11 && m00 > m22 -> {
            val s = 2f * kotlin.math.sqrt(1f + m00 - m11 - m22)
            Quaternion(
                0.25f * s,
                (m01 + m10) / s,
                (m02 + m20) / s,
                (m21 - m12) / s
            )
        }
        m11 > m22 -> {
            val s = 2f * kotlin.math.sqrt(1f + m11 - m00 - m22)
            Quaternion(
                (m01 + m10) / s,
                0.25f * s,
                (m12 + m21) / s,
                (m02 - m20) / s
            )
        }
        else -> {
            val s = 2f * kotlin.math.sqrt(1f + m22 - m00 - m11)
            Quaternion(
                (m02 + m20) / s,
                (m12 + m21) / s,
                0.25f * s,
                (m10 - m01) / s
            )
        }
    }
}