package io.kreekt.xr

import android.app.Activity
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * ARCore implementation for Android platform
 * Provides comprehensive AR capabilities including:
 * - Motion tracking and environmental understanding
 * - Plane detection and hit testing
 * - Augmented Images and Cloud Anchors
 * - Face tracking and augmented faces
 * - Depth API and occlusion
 * - Instant placement and persistent cloud anchors
 */
actual class XRSystemImpl : XRSystem {
    private var arSession: Session? = null
    private var activity: Activity? = null
    private val stateFlow = MutableStateFlow(XRState.NotSupported)
    private val eventChannel = Channel<XREvent>(Channel.UNLIMITED)

    // Rendering components
    private var displayRotationHelper: DisplayRotationHelper? = null
    private var cameraPermissionHelper: CameraPermissionHelper? = null
    private var depthTexture: Texture? = null

    // Feature managers
    private var cloudAnchorManager: CloudAnchorManager? = null
    private var augmentedImageDatabase: AugmentedImageDatabase? = null
    private var instantPlacementManager: InstantPlacementManager? = null

    // Tracking state
    private var isTracking = false
    private val trackedPlanes = mutableSetOf<Plane>()
    private val trackedImages = mutableSetOf<AugmentedImage>()
    private val trackedFaces = mutableSetOf<AugmentedFace>()

    override val state: StateFlow<XRState> = stateFlow.asStateFlow()
    override val events: Flow<XREvent> = eventChannel.receiveAsFlow()

    fun initialize(context: Context) {
        if (context is Activity) {
            activity = context
            displayRotationHelper = DisplayRotationHelper(context)
            cameraPermissionHelper = CameraPermissionHelper()
        }
    }

    override suspend fun checkSupport(): XRSupport {
        val availability = ArCoreApk.getInstance().checkAvailability(
            activity ?: return XRSupport.NotSupported
        )

        return when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED,
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                // Check for advanced features
                when {
                    checkDepthSupport() && checkRawDepthSupport() -> XRSupport.Full
                    checkDepthSupport() -> XRSupport.Advanced
                    else -> XRSupport.Basic
                }
            }
            else -> XRSupport.NotSupported
        }
    }

    override suspend fun requestSession(mode: XRMode): XRSession? {
        val context = activity ?: throw IllegalStateException("Activity not set")

        // Check camera permission
        if (!cameraPermissionHelper?.hasCameraPermission(context) == true) {
            cameraPermissionHelper?.requestCameraPermission(context)
            return null
        }

        // Install ARCore if needed
        try {
            when (ArCoreApk.getInstance().requestInstall(context, true)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> return null
                ArCoreApk.InstallStatus.INSTALLED -> { /* Continue */ }
            }
        } catch (e: UnavailableException) {
            handleArCoreException(e)
            return null
        }

        // Create session
        return try {
            val session = Session(context)
            arSession = session

            // Configure session based on mode
            val config = createConfig(session, mode)
            session.configure(config)

            // Resume session
            session.resume()
            isTracking = true
            stateFlow.value = XRState.Active

            ARCoreXRSession(session, context, displayRotationHelper!!)
        } catch (e: Exception) {
            Log.e("ARCore", "Failed to create session", e)
            handleArCoreException(e)
            null
        }
    }

    override suspend fun endSession() {
        isTracking = false
        arSession?.pause()
        arSession?.close()
        arSession = null
        stateFlow.value = XRState.Inactive
    }

    private fun createConfig(session: Session, mode: XRMode): Config {
        return Config(session).apply {
            when (mode) {
                is XRMode.AR.World -> configureWorldTracking(this, session, mode)
                is XRMode.AR.Face -> configureFaceTracking(this, session)
                else -> { /* Default config */ }
            }

            // Common configuration
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE

            // Enable depth if supported
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                depthMode = Config.DepthMode.AUTOMATIC
            }

            // Enable instant placement
            instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        }
    }

    private fun configureWorldTracking(config: Config, session: Session, mode: XRMode.AR.World) {
        config.apply {
            // Plane detection
            if (mode.planeDetection) {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            }

            // Image tracking
            if (mode.imageTracking) {
                augmentedImageDatabase = createImageDatabase(session)
                this.augmentedImageDatabase = augmentedImageDatabase
                augmentedImageMode = Config.AugmentedImageMode.ENABLED
            }

            // Cloud anchors
            if (mode.cloudAnchors) {
                cloudAnchorMode = Config.CloudAnchorMode.ENABLED
                cloudAnchorManager = CloudAnchorManager(session)
            }

            // Focus mode
            focusMode = Config.FocusMode.AUTO

            // Semantic mode for scene understanding
            if (session.isSemanticModeSupported(Config.SemanticMode.ENABLED)) {
                semanticMode = Config.SemanticMode.ENABLED
            }

            // Geospatial API if available
            if (mode.geoTracking && session.isGeospatialModeSupported(
                    Config.GeospatialMode.ENABLED)) {
                geospatialMode = Config.GeospatialMode.ENABLED
            }
        }
    }

    private fun configureFaceTracking(config: Config, session: Session) {
        config.apply {
            augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
            focusMode = Config.FocusMode.FIXED
        }
    }

    private fun createImageDatabase(session: Session): AugmentedImageDatabase {
        return AugmentedImageDatabase(session).apply {
            // Add reference images here
            // Example: addImage("image_name", bitmap, widthInMeters)
        }
    }

    private fun checkDepthSupport(): Boolean {
        return try {
            arSession?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRawDepthSupport(): Boolean {
        return try {
            arSession?.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY) ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun handleArCoreException(e: Exception) {
        val errorMessage = when (e) {
            is UnavailableArcoreNotInstalledException -> "ARCore not installed"
            is UnavailableApkTooOldException -> "ARCore APK too old"
            is UnavailableSdkTooOldException -> "SDK too old"
            is UnavailableDeviceNotCompatibleException -> "Device not compatible"
            is CameraNotAvailableException -> "Camera not available"
            else -> e.message ?: "Unknown error"
        }
        stateFlow.value = XRState.Error(errorMessage)
    }
}

/**
 * ARCore XR Session implementation
 */
class ARCoreXRSession(
    private val arSession: Session,
    private val context: Context,
    private val displayRotationHelper: DisplayRotationHelper
) : XRSession {
    override val state = MutableStateFlow(XRSessionState.Active)
    override val renderState = XRRenderState()

    private val anchors = mutableMapOf<String, Anchor>()
    private val cloudAnchors = mutableMapOf<String, Anchor>()
    private var currentFrame: Frame? = null

    // Hit test sources
    private val hitTestSources = mutableListOf<ARCoreHitTestSource>()
    private var hitTestSourceId = 0

    override suspend fun requestReferenceSpace(type: XRReferenceSpaceType): XRReferenceSpace {
        return when (type) {
            XRReferenceSpaceType.Local -> ARCoreLocalSpace(arSession)
            XRReferenceSpaceType.LocalFloor -> ARCoreLocalFloorSpace(arSession)
            XRReferenceSpaceType.BoundedFloor -> ARCoreBoundedFloorSpace(arSession)
            XRReferenceSpaceType.Unbounded -> ARCoreUnboundedSpace(arSession)
            XRReferenceSpaceType.Viewer -> ARCoreViewerSpace(arSession)
        }
    }

    override suspend fun requestAnimationFrame(callback: (XRFrame) -> Unit) {
        // Update frame
        currentFrame = arSession.update()
        currentFrame?.let { frame ->
            displayRotationHelper.updateSessionIfNeeded(arSession)
            callback(ARCoreXRFrame(frame, this, arSession))
        }
    }

    override fun updateRenderState(state: XRRenderState) {
        renderState.apply {
            depthNear = state.depthNear
            depthFar = state.depthFar
            inlineVerticalFieldOfView = state.inlineVerticalFieldOfView
            baseLayer = state.baseLayer
        }
    }

    override suspend fun end() {
        state.value = XRSessionState.Ended
        arSession.pause()
        arSession.close()
    }

    override suspend fun createAnchor(pose: XRPose, space: XRReferenceSpace): XRAnchor {
        val arPose = pose.toArCorePose()
        val anchor = arSession.createAnchor(arPose)
        val id = generateAnchorId()
        anchors[id] = anchor
        return ARCoreAnchor(id, anchor, space)
    }

    override suspend fun deleteAnchor(anchor: XRAnchor) {
        if (anchor is ARCoreAnchor) {
            anchors[anchor.id]?.detach()
            anchors.remove(anchor.id)
        }
    }

    override suspend fun getInputSources(): List<XRInputSource> {
        return listOf(ARCoreTouchInputSource())
    }

    override suspend fun requestHitTest(
        ray: XRRay,
        referenceSpace: XRReferenceSpace
    ): List<XRHitTestResult> {
        val results = mutableListOf<XRHitTestResult>()

        currentFrame?.let { frame ->
            val camera = frame.camera
            if (camera.trackingState == TrackingState.TRACKING) {
                // Perform hit test at screen center or ray origin
                val hitResults = frame.hitTest(
                    frame.screenCenter(),
                    setOf(
                        HitResult.TrackableType.PLANE,
                        HitResult.TrackableType.POINT,
                        HitResult.TrackableType.DEPTH_POINT,
                        HitResult.TrackableType.INSTANT_PLACEMENT_POINT
                    )
                )

                hitResults.forEach { hit ->
                    results.add(ARCoreHitTestResult(hit))
                }
            }
        }

        return results
    }

    suspend fun createCloudAnchor(anchor: Anchor): String? {
        return try {
            val cloudAnchor = arSession.hostCloudAnchorWithTtl(anchor, 24 * 60 * 60) // 24 hours TTL
            val cloudId = cloudAnchor.cloudAnchorId
            cloudAnchors[cloudId] = cloudAnchor
            cloudId
        } catch (e: Exception) {
            Log.e("ARCore", "Failed to create cloud anchor", e)
            null
        }
    }

    suspend fun resolveCloudAnchor(cloudId: String): Anchor? {
        return try {
            val cloudAnchor = arSession.resolveCloudAnchor(cloudId)
            cloudAnchors[cloudId] = cloudAnchor
            cloudAnchor
        } catch (e: Exception) {
            Log.e("ARCore", "Failed to resolve cloud anchor", e)
            null
        }
    }

    private fun generateAnchorId(): String {
        return "anchor_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}_${anchors.size}"
    }

    fun createHitTestSource(options: XRHitTestSourceOptions): ARCoreHitTestSource {
        val source = ARCoreHitTestSource(++hitTestSourceId, options)
        hitTestSources.add(source)
        return source
    }

    fun cancelHitTestSource(source: ARCoreHitTestSource) {
        hitTestSources.remove(source)
    }
}

/**
 * ARCore XR Frame implementation
 */
class ARCoreXRFrame(
    private val arFrame: Frame,
    override val session: XRSession,
    private val arSession: Session
) : XRFrame {
    override val predictedDisplayTime: Double = arFrame.timestamp.toDouble()

    override fun getPose(space: XRSpace, baseSpace: XRSpace): XRPose? {
        val camera = arFrame.camera
        if (camera.trackingState != TrackingState.TRACKING) return null

        val pose = camera.pose
        return pose.toXRPose()
    }

    override fun getViewerPose(referenceSpace: XRReferenceSpace): XRViewerPose? {
        val camera = arFrame.camera
        if (camera.trackingState != TrackingState.TRACKING) return null

        val pose = camera.pose
        val projectionMatrix = camera.getProjectionMatrix(FloatArray(16), 0, 0.1f, 100.0f)

        return XRViewerPose(
            transform = XRRigidTransform(
                position = Vector3(pose.tx(), pose.ty(), pose.tz()),
                orientation = Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw())
            ),
            views = listOf(
                XRView(
                    eye = XREye.None,
                    projectionMatrix = Matrix4.fromArray(projectionMatrix),
                    transform = XRRigidTransform(
                        position = Vector3(pose.tx(), pose.ty(), pose.tz()),
                        orientation = Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw())
                    ),
                    recommendedViewportScale = 1.0f
                )
            ),
            emulatedPosition = false
        )
    }

    override fun getHitTestResults(source: XRHitTestSource): List<XRHitTestResult> {
        if (source !is ARCoreHitTestSource) return emptyList()

        val results = mutableListOf<XRHitTestResult>()
        val camera = arFrame.camera

        if (camera.trackingState == TrackingState.TRACKING) {
            // Perform hit test based on source options
            val hitResults = when (source.options.entityTypes) {
                setOf(XREntityType.Plane) -> {
                    arFrame.hitTest(arFrame.screenCenter()).filter {
                        it.trackable is Plane
                    }
                }
                setOf(XREntityType.Point) -> {
                    arFrame.hitTest(arFrame.screenCenter()).filter {
                        it.trackable is Point
                    }
                }
                else -> arFrame.hitTest(arFrame.screenCenter())
            }

            hitResults.forEach { hit ->
                results.add(ARCoreHitTestResult(hit))
            }
        }

        return results
    }

    override fun getHitTestResultsForTransientInput(
        source: XRTransientInputHitTestSource
    ): List<XRTransientInputHitTestResult> {
        // Handle transient input hit tests (touch events)
        return emptyList()
    }

    override fun getJointPose(joint: XRJoint, baseSpace: XRSpace): XRPose? {
        // Hand tracking not available in standard ARCore
        return null
    }

    override fun fillPoses(joints: List<XRJoint>, baseSpace: XRSpace, output: FloatArray): Boolean {
        // Hand tracking not available in standard ARCore
        return false
    }

    override fun getLightProbe(): XRLightProbe? {
        val lightEstimate = arFrame.lightEstimate
        return if (lightEstimate.state == LightEstimate.State.VALID) {
            ARCoreLightProbe(lightEstimate)
        } else null
    }

    override fun getDepthInformation(view: XRView): XRDepthInformation? {
        try {
            if (arSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                arFrame.acquireDepthImage16Bits()?.use { depthImage ->
                    return ARCoreDepthInformation(depthImage)
                }
            }
        } catch (e: NotYetAvailableException) {
            // Depth not available yet
        }
        return null
    }

    fun getTrackedPlanes(): List<Plane> {
        return arSession.getAllTrackables(Plane::class.java).toList()
    }

    fun getUpdatedTrackables(type: Class<out Trackable>): Collection<Trackable> {
        return arFrame.getUpdatedTrackables(type)
    }

    fun getSemanticImage(): Image? {
        return try {
            arFrame.acquireSemanticImage()
        } catch (e: NotYetAvailableException) {
            null
        }
    }

    fun getSemanticConfidenceImage(): Image? {
        return try {
            arFrame.acquireSemanticConfidenceImage()
        } catch (e: NotYetAvailableException) {
            null
        }
    }
}

/**
 * ARCore implementations of XR types
 */
class ARCoreAnchor(
    val id: String,
    private val anchor: Anchor,
    override val anchorSpace: XRSpace
) : XRAnchor {
    override fun delete() {
        anchor.detach()
    }

    fun getCloudAnchorId(): String? {
        return if (anchor.cloudAnchorState == Anchor.CloudAnchorState.SUCCESS) {
            anchor.cloudAnchorId
        } else null
    }

    fun getPose(): Pose = anchor.pose
    fun getTrackingState(): TrackingState = anchor.trackingState
}

/**
 * ARCore reference space implementations
 */
abstract class ARCoreReferenceSpace(protected val arSession: Session) : XRReferenceSpace {
    override fun getOffsetReferenceSpace(transform: XRRigidTransform): XRReferenceSpace {
        return ARCoreOffsetSpace(this, transform)
    }
}

class ARCoreLocalSpace(arSession: Session) : ARCoreReferenceSpace(arSession)
class ARCoreLocalFloorSpace(arSession: Session) : ARCoreReferenceSpace(arSession)
class ARCoreBoundedFloorSpace(arSession: Session) : ARCoreReferenceSpace(arSession) {
    override val boundsGeometry: List<Vector3>
        get() = emptyList() // ARCore doesn't have bounded spaces
}
class ARCoreUnboundedSpace(arSession: Session) : ARCoreReferenceSpace(arSession)
class ARCoreViewerSpace(arSession: Session) : ARCoreReferenceSpace(arSession)
class ARCoreOffsetSpace(
    private val baseSpace: XRReferenceSpace,
    private val offset: XRRigidTransform
) : XRReferenceSpace {
    override fun getOffsetReferenceSpace(transform: XRRigidTransform): XRReferenceSpace {
        return ARCoreOffsetSpace(this, transform)
    }
}

/**
 * Touch input source for AR interaction
 */
class ARCoreTouchInputSource : XRInputSource {
    override val handedness: XRHandedness = XRHandedness.None
    override val targetRayMode: XRTargetRayMode = XRTargetRayMode.Screen
    override val targetRaySpace: XRSpace = object : XRSpace {}
    override val gripSpace: XRSpace? = null
    override val gamepad: XRGamepad? = null
    override val hand: XRHand? = null
    override val profiles: List<String> = listOf("touchscreen", "generic-touchscreen")
}

/**
 * Hit test result implementation
 */
class ARCoreHitTestResult(
    private val hitResult: HitResult
) : XRHitTestResult {
    override fun getPose(baseSpace: XRSpace): XRPose? {
        return hitResult.hitPose.toXRPose()
    }

    override fun createAnchor(): XRAnchor? {
        val anchor = hitResult.createAnchor()
        return ARCoreAnchor(
            "hit_anchor_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
            anchor,
            object : XRSpace {}
        )
    }

    fun getDistance(): Float = hitResult.distance
    fun getTrackable(): Trackable = hitResult.trackable
}

/**
 * Hit test source for continuous hit testing
 */
class ARCoreHitTestSource(
    val id: Int,
    val options: XRHitTestSourceOptions
) : XRHitTestSource {
    fun cancel() {
        // Cancellation handled by session
    }
}

/**
 * Light estimation wrapper
 */
class ARCoreLightProbe(
    private val lightEstimate: LightEstimate
) : XRLightProbe {
    override val probeSpace: XRSpace = object : XRSpace {}
    override val indirectIrradiance: Float
        get() = lightEstimate.ambientIntensity

    fun getEnvironmentalHdrMainLightDirection(): FloatArray {
        return lightEstimate.environmentalHdrMainLightDirection
    }

    fun getEnvironmentalHdrMainLightIntensity(): FloatArray {
        return lightEstimate.environmentalHdrMainLightIntensity
    }

    fun getEnvironmentalHdrAmbientSphericalHarmonics(): FloatArray {
        return lightEstimate.environmentalHdrAmbientSphericalHarmonics
    }

    fun getColorCorrection(): FloatArray {
        return lightEstimate.colorCorrection
    }
}

/**
 * Depth information from ARCore
 */
class ARCoreDepthInformation(
    private val depthImage: Image
) : XRDepthInformation {
    override val width: Int = depthImage.width
    override val height: Int = depthImage.height
    override val normDepthBufferFromNormView: Matrix4 = Matrix4.identity()
    override val rawValueToMeters: Float = 0.001f // millimeters to meters

    override fun getDepthInMeters(x: Float, y: Float): Float {
        val xPixel = (x * width).toInt().coerceIn(0, width - 1)
        val yPixel = (y * height).toInt().coerceIn(0, height - 1)

        val plane = depthImage.planes[0]
        val buffer = plane.buffer.asShortBuffer()
        val index = yPixel * plane.rowStride / 2 + xPixel
        val depthSample = buffer[index]

        return depthSample * rawValueToMeters
    }

    fun getDepthTexture(): Int {
        // Return OpenGL texture ID if needed
        return 0
    }
}

/**
 * Helper classes
 */
class DisplayRotationHelper(private val context: Context) {
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var viewportChanged = false

    fun updateSessionIfNeeded(session: Session) {
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
        val displayRotation = display.rotation

        if (viewportChanged) {
            val displayWidth = if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180) {
                viewportWidth
            } else {
                viewportHeight
            }
            val displayHeight = if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180) {
                viewportHeight
            } else {
                viewportWidth
            }
            session.setDisplayGeometry(displayRotation, displayWidth, displayHeight)
            viewportChanged = false
        }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        viewportChanged = true
    }
}

class CameraPermissionHelper {
    private val CAMERA_PERMISSION = android.Manifest.permission.CAMERA
    private val CAMERA_PERMISSION_CODE = 0

    fun hasCameraPermission(activity: Activity): Boolean {
        return activity.checkSelfPermission(CAMERA_PERMISSION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission(activity: Activity) {
        activity.requestPermissions(arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE)
    }

    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return activity.shouldShowRequestPermissionRationale(CAMERA_PERMISSION)
    }
}

class CloudAnchorManager(private val session: Session) {
    private val pendingAnchors = mutableMapOf<String, Anchor>()

    suspend fun hostAnchor(anchor: Anchor, ttlDays: Int = 1): String? {
        val ttlSeconds = ttlDays * 24 * 60 * 60
        val cloudAnchor = session.hostCloudAnchorWithTtl(anchor, ttlSeconds)
        pendingAnchors[cloudAnchor.cloudAnchorId] = cloudAnchor
        return cloudAnchor.cloudAnchorId
    }

    suspend fun resolveAnchor(cloudId: String): Anchor? {
        val cloudAnchor = session.resolveCloudAnchor(cloudId)
        pendingAnchors[cloudId] = cloudAnchor
        return cloudAnchor
    }

    fun checkAnchorState(cloudId: String): Anchor.CloudAnchorState? {
        return pendingAnchors[cloudId]?.cloudAnchorState
    }

    fun clearPendingAnchors() {
        pendingAnchors.forEach { (_, anchor) ->
            anchor.detach()
        }
        pendingAnchors.clear()
    }
}

class InstantPlacementManager {
    private val instantPlacements = mutableListOf<InstantPlacementPoint>()

    fun addInstantPlacement(point: InstantPlacementPoint) {
        instantPlacements.add(point)
    }

    fun updateInstantPlacements() {
        instantPlacements.removeAll { point ->
            point.trackingMethod == InstantPlacementPoint.TrackingMethod.FULL_TRACKING
        }
    }

    fun clear() {
        instantPlacements.clear()
    }
}

// Extension functions for type conversion
private fun XRPose.toArCorePose(): Pose {
    val translation = floatArrayOf(position.x, position.y, position.z)
    val rotation = floatArrayOf(
        orientation.x,
        orientation.y,
        orientation.z,
        orientation.w
    )
    return Pose(translation, rotation)
}

private fun Pose.toXRPose(): XRPose {
    return XRPose(
        position = Vector3(tx(), ty(), tz()),
        orientation = Quaternion(qx(), qy(), qz(), qw())
    )
}

private fun Frame.screenCenter(): Float {
    // Return center of screen for hit testing
    return 0.5f
}

// Additional ARCore-specific features
data class XRHitTestSourceOptions(
    val entityTypes: Set<XREntityType> = setOf(XREntityType.Plane),
    val offsetRay: XRRay? = null
)

enum class XREntityType {
    Plane, Point, Mesh, Face
}