/**
 * XR (VR/AR) API Contract
 *
 * This file defines the complete API surface for the XR subsystem,
 * ensuring Three.js r180 compatibility with Kotlin idioms.
 *
 * Contract Design Principles:
 * - Type-safe method signatures
 * - Data classes for immutable configuration
 * - Default parameters for common use cases
 * - Sealed classes for type hierarchies
 * - Extension functions for DSL builders
 */

package io.kreekt.contracts.xr

import io.kreekt.core.math.*
import io.kreekt.xr.*
import io.kreekt.scene.*
import io.kreekt.camera.*
import io.kreekt.geometry.*

// ============================================================================
// Core XR API
// ============================================================================

/**
 * WebXRManager: WebXR session management
 * Three.js equivalent: THREE.WebXRManager
 */
interface WebXRManagerAPI {
    var enabled: Boolean
    var isPresenting: Boolean
    val cameraAutoUpdate: Boolean

    fun getSession(): XRSession?
    fun setSession(session: XRSession?)
    fun getCamera(): ArrayCamera
    fun setFramebufferScaleFactor(scale: Float)
    fun setReferenceSpaceType(type: XRReferenceSpaceType)
    fun getReferenceSpace(): XRReferenceSpace?
    fun setReferenceSpace(space: XRReferenceSpace)
    fun getBaseLayer(): XRWebGLLayer?
    fun getBinding(): XRWebGLBinding?
    fun getFrame(): XRFrame?
    fun getController(index: Int): XRController
    fun getControllerGrip(index: Int): XRGrip
    fun getHand(index: Int): XRHand
    fun setAnimationLoop(callback: ((Float, XRFrame?) -> Unit)?)
    fun dispose()
}

/**
 * XRController: VR controller representation
 * Three.js equivalent: THREE.Group (with XR controller data)
 */
interface XRControllerAPI : Object3DAPI {
    val targetRaySpace: Object3D
    val gripSpace: Object3D
    val hand: XRHand?

    fun getTargetRaySpace(): Object3D
    fun getGripSpace(): Object3D
    fun getHand(): XRHand?
    fun dispatchEvent(event: XRInputSourceEvent)
    fun disconnect(inputSource: XRInputSource)
    fun update(inputSource: XRInputSource, frame: XRFrame, referenceSpace: XRReferenceSpace)
}

/**
 * XRGrip: Controller grip space
 */
interface XRGripAPI : Object3DAPI {
    val inputSource: XRInputSource?
    fun update(inputSource: XRInputSource, frame: XRFrame, referenceSpace: XRReferenceSpace)
}

/**
 * XRHand: Hand tracking
 * Three.js equivalent: THREE.Group (with hand joints)
 */
interface XRHandAPI : Object3DAPI {
    val joints: Map<XRHandJoint, Object3D>
    val inputSource: XRInputSource?

    fun update(inputSource: XRInputSource, frame: XRFrame, referenceSpace: XRReferenceSpace)
}

// ============================================================================
// XR Session and Spaces
// ============================================================================

/**
 * XRSession: WebXR session interface
 */
interface XRSession {
    val environmentBlendMode: XREnvironmentBlendMode
    val interactionMode: XRInteractionMode
    val visibilityState: XRVisibilityState
    val frameRate: Float?
    val supportedFrameRates: FloatArray?
    val renderState: XRRenderState
    val inputSources: List<XRInputSource>
    val enabledFeatures: List<String>

    fun updateRenderState(state: XRRenderStateInit)
    fun requestReferenceSpace(type: XRReferenceSpaceType): XRReferenceSpace
    fun requestAnimationFrame(callback: (Float, XRFrame) -> Unit): Int
    fun cancelAnimationFrame(handle: Int)
    fun end(): Unit
    fun updateTargetFrameRate(rate: Float)

    fun addEventListener(type: String, listener: (Any) -> Unit)
    fun removeEventListener(type: String, listener: (Any) -> Unit)
}

interface XRRenderState {
    val baseLayer: XRWebGLLayer?
    val depthFar: Float
    val depthNear: Float
    val inlineVerticalFieldOfView: Float?
    val layers: List<XRLayer>?
}

data class XRRenderStateInit(
    val baseLayer: XRWebGLLayer? = null,
    val depthFar: Float = 1000f,
    val depthNear: Float = 0.1f,
    val inlineVerticalFieldOfView: Float? = null,
    val layers: List<XRLayer>? = null
)

/**
 * XRReferenceSpace: Spatial tracking reference
 */
interface XRReferenceSpace {
    fun getOffsetReferenceSpace(originOffset: XRRigidTransform): XRReferenceSpace
    fun addEventListener(type: String, listener: (Any) -> Unit)
    fun removeEventListener(type: String, listener: (Any) -> Unit)
}

data class XRRigidTransform(
    val position: Vector3 = Vector3(0f, 0f, 0f),
    val orientation: Quaternion = Quaternion(0f, 0f, 0f, 1f)
) {
    val matrix: Matrix4
        get() = Matrix4().compose(position, orientation, Vector3(1f, 1f, 1f))

    val inverse: XRRigidTransform
        get() {
            val invMatrix = Matrix4().copy(matrix).invert()
            val pos = Vector3()
            val quat = Quaternion()
            val scale = Vector3()
            invMatrix.decompose(pos, quat, scale)
            return XRRigidTransform(pos, quat)
        }
}

// ============================================================================
// XR Frame and Views
// ============================================================================

/**
 * XRFrame: Single frame of XR session
 */
interface XRFrame {
    val session: XRSession
    val predictedDisplayTime: Float

    fun getViewerPose(referenceSpace: XRReferenceSpace): XRViewerPose?
    fun getPose(space: XRSpace, baseSpace: XRSpace): XRPose?
    fun getHitTestResults(source: XRHitTestSource): List<XRHitTestResult>
    fun getHitTestResultsForTransientInput(source: XRTransientInputHitTestSource): List<XRTransientInputHitTestResult>
    fun getLightEstimate(lightProbe: XRLightProbe): XRLightEstimate?
    fun getDepthInformation(view: XRView): XRCPUDepthInformation?
    fun getJointPose(joint: XRJointSpace, baseSpace: XRSpace): XRJointPose?
}

interface XRViewerPose {
    val transform: XRRigidTransform
    val views: List<XRView>
}

interface XRView {
    val eye: XREye
    val projectionMatrix: Matrix4
    val transform: XRRigidTransform
    val recommendedViewportScale: Float?

    fun requestViewportScale(scale: Float?)
}

interface XRPose {
    val transform: XRRigidTransform
    val linearVelocity: Vector3?
    val angularVelocity: Vector3?
    val emulatedPosition: Boolean
}

interface XRJointPose : XRPose {
    val radius: Float
}

// ============================================================================
// Input Sources
// ============================================================================

/**
 * XRInputSource: VR/AR input device
 */
interface XRInputSource {
    val handedness: XRHandedness
    val targetRayMode: XRTargetRayMode
    val targetRaySpace: XRSpace
    val gripSpace: XRSpace?
    val gamepad: Gamepad?
    val profiles: List<String>
    val hand: XRHandInput?
}

interface XRHandInput {
    val size: Int
    operator fun get(key: XRHandJoint): XRJointSpace
    fun entries(): Iterator<Pair<XRHandJoint, XRJointSpace>>
    fun keys(): Iterator<XRHandJoint>
    fun values(): Iterator<XRJointSpace>
}

interface XRSpace {
    // Marker interface for XR spaces
}

interface XRJointSpace : XRSpace {
    val jointName: XRHandJoint
}

interface Gamepad {
    val id: String
    val index: Int
    val connected: Boolean
    val timestamp: Double
    val mapping: String
    val axes: FloatArray
    val buttons: List<GamepadButton>
    val hapticActuators: List<GamepadHapticActuator>?
}

data class GamepadButton(
    val pressed: Boolean,
    val touched: Boolean,
    val value: Float
)

interface GamepadHapticActuator {
    val type: String
    fun pulse(value: Float, duration: Float): Boolean
    fun playEffect(type: String, params: GamepadEffectParameters): Boolean
}

data class GamepadEffectParameters(
    val duration: Float = 0f,
    val startDelay: Float = 0f,
    val strongMagnitude: Float = 0f,
    val weakMagnitude: Float = 0f
)

// ============================================================================
// Hit Testing
// ============================================================================

/**
 * XRHitTestSource: Ray-based hit testing
 */
interface XRHitTestSource {
    fun cancel()
}

interface XRTransientInputHitTestSource {
    fun cancel()
}

interface XRHitTestResult {
    fun getPose(baseSpace: XRSpace): XRPose?
    fun createAnchor(): XRAnchor?
}

interface XRTransientInputHitTestResult {
    val inputSource: XRInputSource
    val results: List<XRHitTestResult>
}

/**
 * XRAnchor: Persistent spatial anchor
 */
interface XRAnchor {
    val anchorSpace: XRSpace
    fun delete()
}

// ============================================================================
// Plane Detection
// ============================================================================

/**
 * XRPlane: Detected planar surface
 */
interface XRPlane {
    val orientation: XRPlaneOrientation
    val planeSpace: XRSpace
    val polygon: List<Vector3>
    val lastChangedTime: Float
}

enum class XRPlaneOrientation {
    Horizontal,
    Vertical
}

// ============================================================================
// Image Tracking
// ============================================================================

/**
 * XRImageTrackingResult: Tracked image
 */
interface XRImageTrackingResult {
    val imageSpace: XRSpace
    val index: Int
    val trackingState: XRImageTrackingState
    val measuredWidthInMeters: Float
}

enum class XRImageTrackingState {
    Tracked,
    Emulated
}

// ============================================================================
// Light Estimation
// ============================================================================

/**
 * XRLightProbe: Environmental light probe
 */
interface XRLightProbe {
    val probeSpace: XRSpace
}

interface XRLightEstimate {
    val sphericalHarmonicsCoefficients: FloatArray
    val primaryLightDirection: Vector3
    val primaryLightIntensity: Color
}

/**
 * XRCPUDepthInformation: Depth data
 */
interface XRCPUDepthInformation {
    val width: Int
    val height: Int
    val normDepthBufferFromNormView: XRRigidTransform
    val rawValueToMeters: Float
    val data: ByteArray

    fun getDepthInMeters(x: Float, y: Float): Float
}

// ============================================================================
// WebGL Integration
// ============================================================================

interface XRWebGLLayer : XRLayer {
    val antialias: Boolean
    val ignoreDepthValues: Boolean
    val framebuffer: Any
    val framebufferWidth: Int
    val framebufferHeight: Int

    fun getViewport(view: XRView): XRViewport
}

interface XRWebGLBinding {
    fun getSubImage(layer: XRLayer, frame: XRFrame, eye: XREye = XREye.None): XRWebGLSubImage
    fun getViewSubImage(layer: XRLayer, view: XRView): XRWebGLSubImage
    fun getDepthInformation(view: XRView): XRWebGLDepthInformation?
    fun getReflectionCubeMap(lightProbe: XRLightProbe): CubeTexture?
}

interface XRWebGLSubImage {
    val colorTexture: Texture
    val depthStencilTexture: Texture?
    val imageIndex: Int?
    val colorTextureWidth: Int
    val colorTextureHeight: Int
    val viewport: XRViewport?
}

interface XRWebGLDepthInformation {
    val texture: Texture
    val width: Int
    val height: Int
    val normDepthBufferFromNormView: XRRigidTransform
    val rawValueToMeters: Float
}

interface XRLayer {
    // Marker interface for XR layers
}

data class XRViewport(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

// ============================================================================
// Events
// ============================================================================

data class XRInputSourceEvent(
    val frame: XRFrame,
    val inputSource: XRInputSource
)

data class XRSessionEvent(
    val session: XRSession
)

data class XRReferenceSpaceEvent(
    val referenceSpace: XRReferenceSpace,
    val transform: XRRigidTransform?
)

// ============================================================================
// Enums and Constants
// ============================================================================

enum class XRReferenceSpaceType {
    Viewer,
    Local,
    LocalFloor,
    BoundedFloor,
    Unbounded
}

enum class XREnvironmentBlendMode {
    Opaque,
    Additive,
    AlphaBlend
}

enum class XRInteractionMode {
    ScreenSpace,
    WorldSpace
}

enum class XRVisibilityState {
    Visible,
    VisibleBlurred,
    Hidden
}

enum class XRHandedness {
    None,
    Left,
    Right
}

enum class XRTargetRayMode {
    Gaze,
    TrackedPointer,
    Screen
}

enum class XRHandJoint {
    Wrist,
    ThumbMetacarpal, ThumbPhalanxProximal, ThumbPhalanxDistal, ThumbTip,
    IndexFingerMetacarpal, IndexFingerPhalanxProximal, IndexFingerPhalanxIntermediate, IndexFingerPhalanxDistal, IndexFingerTip,
    MiddleFingerMetacarpal, MiddleFingerPhalanxProximal, MiddleFingerPhalanxIntermediate, MiddleFingerPhalanxDistal, MiddleFingerTip,
    RingFingerMetacarpal, RingFingerPhalanxProximal, RingFingerPhalanxIntermediate, RingFingerPhalanxDistal, RingFingerTip,
    PinkyMetacarpal, PinkyPhalanxProximal, PinkyPhalanxIntermediate, PinkyPhalanxDistal, PinkyTip
}

enum class XREye {
    None,
    Left,
    Right
}

// ============================================================================
// Helper Classes
// ============================================================================

/**
 * XRControllerModelFactory: Load controller models
 */
interface XRControllerModelFactoryAPI {
    fun createControllerModel(controller: XRController): Object3D
}

/**
 * XRHandModelFactory: Load hand models
 */
interface XRHandModelFactoryAPI {
    fun createHandModel(hand: XRHand, profile: String = "spheres"): Object3D
}

/**
 * XRButton: UI button for entering XR
 */
interface XRButtonAPI {
    companion object {
        fun createButton(
            renderer: Renderer,
            sessionInit: XRSessionInit = XRSessionInit(),
            onSessionStarted: ((XRSession) -> Unit)? = null,
            onSessionEnded: (() -> Unit)? = null
        ): Any  // Returns HTML button element
    }
}

data class XRSessionInit(
    val requiredFeatures: List<String> = emptyList(),
    val optionalFeatures: List<String> = emptyList(),
    val domOverlay: DomOverlay? = null
)

data class DomOverlay(
    val root: Any  // HTML element
)

// ============================================================================
// Factory Functions (Kotlin Idiom)
// ============================================================================

/**
 * Extension function to enable XR on renderer
 */
fun Renderer.enableXR(): WebXRManager {
    val xrManager = WebXRManager(this)
    xrManager.enabled = true
    return xrManager
}

/**
 * Extension function to configure VR session
 */
fun WebXRManager.configureVR(
    referenceSpaceType: XRReferenceSpaceType = XRReferenceSpaceType.LocalFloor,
    framebufferScale: Float = 1.0f
) {
    setReferenceSpaceType(referenceSpaceType)
    setFramebufferScaleFactor(framebufferScale)
}

/**
 * Extension function for haptic feedback
 */
fun XRController.vibrate(intensity: Float, duration: Float) {
    val inputSource = getTargetRaySpace().userData["inputSource"] as? XRInputSource
    inputSource?.gamepad?.hapticActuators?.firstOrNull()?.pulse(intensity, duration)
}

// ============================================================================
// Usage Examples
// ============================================================================

/**
 * Example: Setup basic VR scene
 */
fun exampleVRSetup(renderer: Renderer, scene: Scene, camera: Camera): WebXRManager {
    val xrManager = renderer.enableXR()

    // Configure XR
    xrManager.apply {
        setReferenceSpaceType(XRReferenceSpaceType.LocalFloor)
        setFramebufferScaleFactor(1.0f)
        enabled = true
    }

    // Add controllers
    val controller1 = xrManager.getController(0)
    val controller2 = xrManager.getController(1)
    scene.add(controller1)
    scene.add(controller2)

    // Add controller grips
    val controllerGrip1 = xrManager.getControllerGrip(0)
    val controllerGrip2 = xrManager.getControllerGrip(1)
    scene.add(controllerGrip1)
    scene.add(controllerGrip2)

    return xrManager
}

/**
 * Example: Hand tracking
 */
fun exampleHandTracking(xrManager: WebXRManager, scene: Scene) {
    val hand1 = xrManager.getHand(0)
    val hand2 = xrManager.getHand(1)

    scene.add(hand1)
    scene.add(hand2)

    // Visualize hand joints
    val handModelFactory = XRHandModelFactory()
    hand1.add(handModelFactory.createHandModel(hand1, "spheres"))
    hand2.add(handModelFactory.createHandModel(hand2, "spheres"))
}

/**
 * Example: Controller interaction
 */
fun exampleControllerInteraction(controller: XRController) {
    controller.addEventListener("selectstart") { event ->
        println("Controller select started")
        controller.vibrate(0.5f, 100f)
    }

    controller.addEventListener("selectend") { event ->
        println("Controller select ended")
    }

    controller.addEventListener("squeeze") { event ->
        println("Controller squeezed")
        controller.vibrate(1.0f, 50f)
    }
}

/**
 * Example: Hit testing for AR placement
 */
suspend fun exampleARHitTest(
    session: XRSession,
    frame: XRFrame,
    referenceSpace: XRReferenceSpace
) {
    // Request hit test source
    val hitTestSource = session.requestHitTestSource(
        XRHitTestOptionsInit(
            space = referenceSpace,
            offsetRay = XRRay()
        )
    )

    // Get hit test results
    val hitTestResults = frame.getHitTestResults(hitTestSource)

    hitTestResults.firstOrNull()?.let { result ->
        val pose = result.getPose(referenceSpace)
        pose?.let {
            // Place object at hit location
            val position = it.transform.position
            println("Hit at: $position")

            // Create persistent anchor
            result.createAnchor()?.let { anchor ->
                println("Anchor created: ${anchor.anchorSpace}")
            }
        }
    }
}

/**
 * Example: Plane detection for AR
 */
fun examplePlaneDetection(
    session: XRSession,
    onPlaneDetected: (XRPlane) -> Unit
) {
    session.addEventListener("planesdetected") { event ->
        val planes = event as? List<XRPlane> ?: return@addEventListener
        planes.forEach { plane ->
            onPlaneDetected(plane)
            println("Plane detected: ${plane.orientation}, vertices: ${plane.polygon.size}")
        }
    }
}

/**
 * Example: Light estimation for AR
 */
fun exampleLightEstimation(
    session: XRSession,
    frame: XRFrame,
    lightProbe: XRLightProbe
) {
    val lightEstimate = frame.getLightEstimate(lightProbe)
    lightEstimate?.let {
        println("Light direction: ${it.primaryLightDirection}")
        println("Light intensity: ${it.primaryLightIntensity}")

        // Apply to scene lighting
        val directionalLight = DirectionalLight(it.primaryLightIntensity, 1f)
        directionalLight.position.copy(it.primaryLightDirection)
    }
}

/**
 * Example: Create XR session button
 */
fun exampleCreateXRButton(renderer: Renderer): Any {
    return XRButton.createButton(
        renderer = renderer,
        sessionInit = XRSessionInit(
            requiredFeatures = listOf("local-floor"),
            optionalFeatures = listOf("hand-tracking", "hit-test", "anchors")
        ),
        onSessionStarted = { session ->
            println("XR session started")
        },
        onSessionEnded = {
            println("XR session ended")
        }
    )
}

// Supporting types for hit testing
data class XRHitTestOptionsInit(
    val space: XRSpace,
    val offsetRay: XRRay? = null,
    val entityTypes: List<XRHitTestTrackableType> = listOf(XRHitTestTrackableType.Plane)
)

data class XRRay(
    val origin: Vector3 = Vector3(0f, 0f, 0f),
    val direction: Vector3 = Vector3(0f, 0f, -1f)
)

enum class XRHitTestTrackableType {
    Point,
    Plane,
    Mesh
}

suspend fun XRSession.requestHitTestSource(options: XRHitTestOptionsInit): XRHitTestSource {
    // Platform-specific implementation
    TODO("Platform-specific hit test source creation")
}
