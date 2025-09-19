/**
 * AR System Implementation
 * Provides augmented reality features including plane detection, hit testing, and light estimation
 */
package io.kreekt.xr

import io.kreekt.core.math.*
import io.kreekt.renderer.Texture
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Default implementation of ARSystem interface
 * Manages AR-specific features on top of XR system
 */
class DefaultARSystem : DefaultXRSystem(), ARSystem {
    private var planeDetectionEnabled = false
    private val detectedPlanes = mutableMapOf<String, DefaultXRPlane>()
    private val trackableImages = mutableMapOf<String, XRImageTarget>()
    private val trackedImages = mutableMapOf<String, DefaultXRTrackedImage>()
    private val trackableObjects = mutableMapOf<String, XRObjectTarget>()
    private val trackedObjects = mutableMapOf<String, DefaultXRTrackedObject>()
    private val environmentProbes = mutableMapOf<String, DefaultXREnvironmentProbe>()

    private val planeAddedCallbacks = mutableListOf<(XRPlane) -> Unit>()
    private val planeUpdatedCallbacks = mutableListOf<(XRPlane) -> Unit>()
    private val planeRemovedCallbacks = mutableListOf<(XRPlane) -> Unit>()

    private var environmentProbesEnabled = false
    private var occlusionEnabled = false
    private var peopleOcclusionEnabled = false
    private var occlusionTexture: Texture? = null

    private var detectionJob: Job? = null
    private var lightEstimationJob: Job? = null

    init {
        startARProcessing()
    }

    override fun enablePlaneDetection(enabled: Boolean) {
        planeDetectionEnabled = enabled
        if (enabled) {
            startPlaneDetection()
        } else {
            stopPlaneDetection()
        }
    }

    override fun getDetectedPlanes(): List<XRPlane> {
        return detectedPlanes.values.filter { it.isTracked() }
    }

    override fun onPlaneAdded(callback: (XRPlane) -> Unit) {
        planeAddedCallbacks.add(callback)
    }

    override fun onPlaneUpdated(callback: (XRPlane) -> Unit) {
        planeUpdatedCallbacks.add(callback)
    }

    override fun onPlaneRemoved(callback: (XRPlane) -> Unit) {
        planeRemovedCallbacks.add(callback)
    }

    override suspend fun addTrackableImage(
        image: ByteArray,
        physicalWidth: Float
    ): XRResult<XRImageTarget> {
        return try {
            val target = DefaultXRImageTarget(
                id = generateImageTargetId(),
                physicalWidth = physicalWidth,
                imageData = image
            )

            // Register with platform AR system
            val registered = registerImageTargetWithPlatform(target)
            if (registered) {
                trackableImages[target.id] = target
                XRResult.Success(target)
            } else {
                XRResult.Error(
                    XRException.InvalidState("Failed to register image target")
                )
            }
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Error adding trackable image: ${e.message}")
            )
        }
    }

    override fun removeTrackableImage(target: XRImageTarget): XRResult<Unit> {
        return try {
            unregisterImageTargetFromPlatform(target)
            trackableImages.remove(target.id)
            trackedImages.remove(target.id)
            XRResult.Success(Unit)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Error removing trackable image: ${e.message}")
            )
        }
    }

    override fun getTrackedImages(): List<XRTrackedImage> {
        return trackedImages.values.filter {
            it.trackingState == XRTrackingState.TRACKING
        }
    }

    override suspend fun addTrackableObject(model: ByteArray): XRResult<XRObjectTarget> {
        return try {
            val target = DefaultXRObjectTarget(
                id = generateObjectTargetId(),
                modelData = model
            )

            val registered = registerObjectTargetWithPlatform(target)
            if (registered) {
                trackableObjects[target.id] = target
                XRResult.Success(target)
            } else {
                XRResult.Error(
                    XRException.InvalidState("Failed to register object target")
                )
            }
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Error adding trackable object: ${e.message}")
            )
        }
    }

    override fun removeTrackableObject(target: XRObjectTarget): XRResult<Unit> {
        return try {
            unregisterObjectTargetFromPlatform(target)
            trackableObjects.remove(target.id)
            trackedObjects.remove(target.id)
            XRResult.Success(Unit)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Error removing trackable object: ${e.message}")
            )
        }
    }

    override fun getTrackedObjects(): List<XRTrackedObject> {
        return trackedObjects.values.filter {
            it.trackingState == XRTrackingState.TRACKING
        }
    }

    override fun enableEnvironmentProbes(enabled: Boolean) {
        environmentProbesEnabled = enabled
        if (enabled) {
            startEnvironmentProbing()
        } else {
            stopEnvironmentProbing()
        }
    }

    override fun getEnvironmentProbes(): List<XREnvironmentProbe> {
        return environmentProbes.values.toList()
    }

    override fun estimateLighting(position: Vector3): XRLightEstimate? {
        if (!environmentProbesEnabled) return null

        // Find nearest environment probe
        val nearestProbe = environmentProbes.values.minByOrNull { probe ->
            probe.pose.position.distanceTo(position)
        }

        return nearestProbe?.let {
            createLightEstimateFromProbe(it)
        }
    }

    override fun enableOcclusion(enabled: Boolean) {
        occlusionEnabled = enabled
        if (enabled) {
            startOcclusionProcessing()
        } else {
            stopOcclusionProcessing()
        }
    }

    override fun getOcclusionTexture(): Texture? {
        return if (occlusionEnabled) occlusionTexture else null
    }

    override fun enablePeopleOcclusion(enabled: Boolean) {
        peopleOcclusionEnabled = enabled
        if (enabled && isPeopleOcclusionSupported()) {
            startPeopleOcclusionProcessing()
        } else {
            stopPeopleOcclusionProcessing()
        }
    }

    private fun startARProcessing() {
        detectionJob = GlobalScope.launch {
            while (true) {
                if (planeDetectionEnabled) {
                    updatePlaneDetection()
                }
                updateImageTracking()
                updateObjectTracking()
                if (environmentProbesEnabled) {
                    updateEnvironmentProbes()
                }
                delay(33) // ~30Hz update rate
            }
        }

        lightEstimationJob = GlobalScope.launch {
            while (true) {
                updateLightEstimation()
                delay(100) // 10Hz update rate for light estimation
            }
        }
    }

    private fun startPlaneDetection() {
        enablePlatformPlaneDetection(true)
    }

    private fun stopPlaneDetection() {
        enablePlatformPlaneDetection(false)
        detectedPlanes.clear()
    }

    private suspend fun updatePlaneDetection() {
        val platformPlanes = getPlatformDetectedPlanes()

        // Check for new planes
        platformPlanes.forEach { planeData ->
            if (!detectedPlanes.containsKey(planeData.id)) {
                val plane = DefaultXRPlane(
                    id = planeData.id,
                    planeSpace = DefaultXRSpace(),
                    polygon = planeData.polygon,
                    orientation = planeData.orientation,
                    semanticLabel = planeData.semanticLabel
                )
                detectedPlanes[planeData.id] = plane
                planeAddedCallbacks.forEach { it(plane) }
            } else {
                // Update existing plane
                val plane = detectedPlanes[planeData.id]!!
                if (plane.updateGeometry(planeData.polygon)) {
                    planeUpdatedCallbacks.forEach { it(plane) }
                }
            }
        }

        // Check for removed planes
        val currentIds = platformPlanes.map { it.id }.toSet()
        val removedIds = detectedPlanes.keys - currentIds

        removedIds.forEach { id ->
            val plane = detectedPlanes.remove(id)!!
            planeRemovedCallbacks.forEach { it(plane) }
        }
    }

    private suspend fun updateImageTracking() {
        trackableImages.values.forEach { target ->
            val trackingResult = trackImageWithPlatform(target)
            if (trackingResult != null) {
                val trackedImage = trackedImages[target.id]
                    ?: DefaultXRTrackedImage(target).also {
                        trackedImages[target.id] = it
                    }

                trackedImage.updateTracking(
                    pose = trackingResult.pose,
                    state = trackingResult.state,
                    extent = trackingResult.extent
                )
            } else {
                trackedImages[target.id]?.updateTracking(
                    pose = null,
                    state = XRTrackingState.STOPPED,
                    extent = null
                )
            }
        }
    }

    private suspend fun updateObjectTracking() {
        trackableObjects.values.forEach { target ->
            val trackingResult = trackObjectWithPlatform(target)
            if (trackingResult != null) {
                val trackedObject = trackedObjects[target.id]
                    ?: DefaultXRTrackedObject(target).also {
                        trackedObjects[target.id] = it
                    }

                trackedObject.updateTracking(
                    pose = trackingResult.pose,
                    state = trackingResult.state,
                    boundingBox = trackingResult.boundingBox
                )
            } else {
                trackedObjects[target.id]?.updateTracking(
                    pose = null,
                    state = XRTrackingState.STOPPED,
                    boundingBox = null
                )
            }
        }
    }

    private fun startEnvironmentProbing() {
        enablePlatformEnvironmentProbes(true)
    }

    private fun stopEnvironmentProbing() {
        enablePlatformEnvironmentProbes(false)
        environmentProbes.clear()
    }

    private suspend fun updateEnvironmentProbes() {
        val platformProbes = getPlatformEnvironmentProbes()

        platformProbes.forEach { probeData ->
            val probe = environmentProbes[probeData.id]
                ?: DefaultXREnvironmentProbe(
                    id = probeData.id,
                    pose = probeData.pose,
                    extent = probeData.extent
                ).also {
                    environmentProbes[probeData.id] = it
                }

            probe.updateCubeMap(probeData.cubeMap)
        }
    }

    private suspend fun updateLightEstimation() {
        if (!environmentProbesEnabled) return

        val lightData = getPlatformLightEstimation()
        if (lightData != null) {
            // Update global light estimation
            currentLightEstimate = DefaultXRLightEstimate(
                sphericalHarmonicsCoefficients = lightData.sphericalHarmonics,
                primaryLightDirection = lightData.primaryDirection,
                primaryLightIntensity = lightData.primaryIntensity,
                environmentTexture = lightData.environmentMap
            )
        }
    }

    private fun startOcclusionProcessing() {
        enablePlatformOcclusion(true)
        GlobalScope.launch {
            while (occlusionEnabled) {
                occlusionTexture = getPlatformOcclusionTexture()
                delay(16) // Update at 60fps
            }
        }
    }

    private fun stopOcclusionProcessing() {
        enablePlatformOcclusion(false)
        occlusionTexture = null
    }

    private fun startPeopleOcclusionProcessing() {
        enablePlatformPeopleOcclusion(true)
    }

    private fun stopPeopleOcclusionProcessing() {
        enablePlatformPeopleOcclusion(false)
    }

    private fun createLightEstimateFromProbe(
        probe: XREnvironmentProbe
    ): XRLightEstimate {
        return DefaultXRLightEstimate(
            sphericalHarmonicsCoefficients = calculateSphericalHarmonics(probe),
            primaryLightDirection = estimatePrimaryLightDirection(probe),
            primaryLightIntensity = estimatePrimaryLightIntensity(probe),
            environmentTexture = probe.environmentTexture
        )
    }

    fun dispose() {
        detectionJob?.cancel()
        lightEstimationJob?.cancel()
        detectedPlanes.clear()
        trackableImages.clear()
        trackedImages.clear()
        trackableObjects.clear()
        trackedObjects.clear()
        environmentProbes.clear()
    }

    companion object {
        private var currentLightEstimate: XRLightEstimate? = null
    }
}

/**
 * Default implementation of XRPlane
 */
class DefaultXRPlane(
    override val id: String,
    override val planeSpace: XRSpace,
    override var polygon: List<Vector2>,
    override val orientation: XRPlaneOrientation,
    override val semanticLabel: String? = null
) : XRPlane {
    override var lastChangedTime: Long = System.currentTimeMillis()
        private set

    private var trackingState = XRTrackingState.TRACKING
    private var centerPoint: Vector3 = calculateCenter()
    private var extent: Vector2 = calculateExtent()

    override fun getExtent(): Vector2 = extent

    override fun getCenterPoint(): Vector3 = centerPoint

    override fun isTracked(): Boolean = trackingState == XRTrackingState.TRACKING

    fun updateGeometry(newPolygon: List<Vector2>): Boolean {
        if (newPolygon != polygon) {
            polygon = newPolygon
            centerPoint = calculateCenter()
            extent = calculateExtent()
            lastChangedTime = System.currentTimeMillis()
            return true
        }
        return false
    }

    fun updateTrackingState(state: XRTrackingState) {
        trackingState = state
        lastChangedTime = System.currentTimeMillis()
    }

    private fun calculateCenter(): Vector3 {
        if (polygon.isEmpty()) return Vector3.ZERO

        val sumX = polygon.sumOf { it.x.toDouble() }.toFloat()
        val sumZ = polygon.sumOf { it.y.toDouble() }.toFloat()

        return Vector3(sumX / polygon.size, 0f, sumZ / polygon.size)
    }

    private fun calculateExtent(): Vector2 {
        if (polygon.isEmpty()) return Vector2.ZERO

        val minX = polygon.minOf { it.x }
        val maxX = polygon.maxOf { it.x }
        val minY = polygon.minOf { it.y }
        val maxY = polygon.maxOf { it.y }

        return Vector2(maxX - minX, maxY - minY)
    }
}

/**
 * Default implementation of XRHitTestSource
 */
class DefaultXRHitTestSource(
    override val id: String,
    val space: XRSpace,
    val entityTypes: Set<String>
) : XRHitTestSource {
    private var cancelled = false

    override suspend fun cancel(): XRResult<Unit> {
        return if (!cancelled) {
            cancelled = true
            cancelPlatformHitTestSource(this)
            XRResult.Success(Unit)
        } else {
            XRResult.Error(XRException.InvalidState("Hit test source already cancelled"))
        }
    }

    fun isCancelled(): Boolean = cancelled
}

/**
 * Default implementation of XRTransientInputHitTestSource
 */
class DefaultXRTransientInputHitTestSource(
    override val id: String,
    val profile: String
) : XRTransientInputHitTestSource {
    private var cancelled = false

    override suspend fun cancel(): XRResult<Unit> {
        return if (!cancelled) {
            cancelled = true
            cancelPlatformTransientHitTestSource(this)
            XRResult.Success(Unit)
        } else {
            XRResult.Error(XRException.InvalidState("Transient hit test source already cancelled"))
        }
    }

    fun isCancelled(): Boolean = cancelled
}

/**
 * Default implementation of XRHitTestResult
 */
class DefaultXRHitTestResult(
    override val pose: XRPose,
    private val space: XRSpace
) : XRHitTestResult {
    override val createAnchor: suspend () -> XRAnchor? = {
        DefaultXRAnchor(
            id = generateAnchorId(),
            initialPose = pose,
            space = space,
            persistent = false
        )
    }
}

/**
 * Default implementation of XRTransientInputHitTestResult
 */
data class DefaultXRTransientInputHitTestResult(
    override val inputSource: XRInputSource,
    override val results: List<XRHitTestResult>
) : XRTransientInputHitTestResult

/**
 * Default implementation of XRLightProbe
 */
class DefaultXRLightProbe(
    override val id: String,
    override val probeSpace: XRSpace
) : XRLightProbe

/**
 * Default implementation of XRLightEstimate
 */
data class DefaultXRLightEstimate(
    override val sphericalHarmonicsCoefficients: FloatArray? = null,
    override val primaryLightDirection: Vector3? = null,
    override val primaryLightIntensity: Color? = null,
    override val environmentTexture: XRCubeMap? = null
) : XRLightEstimate

/**
 * Default implementation of XRImageTarget
 */
class DefaultXRImageTarget(
    override val id: String,
    override val physicalWidth: Float,
    override val imageData: ByteArray
) : XRImageTarget

/**
 * Default implementation of XRTrackedImage
 */
class DefaultXRTrackedImage(
    override val target: XRImageTarget
) : XRTrackedImage {
    private var _pose: XRPose? = null
    override val pose: XRPose get() = _pose ?: DefaultXRPose(
        Matrix4.identity(),
        Vector3.ZERO,
        Quaternion.IDENTITY
    )

    override var trackingState: XRTrackingState = XRTrackingState.STOPPED
        private set

    private var _extent: Vector2? = null
    override val extent: Vector2 get() = _extent ?: Vector2.ZERO

    fun updateTracking(pose: XRPose?, state: XRTrackingState, extent: Vector2?) {
        _pose = pose
        trackingState = state
        _extent = extent
    }
}

/**
 * Default implementation of XRObjectTarget
 */
class DefaultXRObjectTarget(
    override val id: String,
    override val modelData: ByteArray
) : XRObjectTarget

/**
 * Default implementation of XRTrackedObject
 */
class DefaultXRTrackedObject(
    override val target: XRObjectTarget
) : XRTrackedObject {
    private var _pose: XRPose? = null
    override val pose: XRPose get() = _pose ?: DefaultXRPose(
        Matrix4.identity(),
        Vector3.ZERO,
        Quaternion.IDENTITY
    )

    override var trackingState: XRTrackingState = XRTrackingState.STOPPED
        private set

    private var _boundingBox: Box3? = null
    override val boundingBox: Box3 get() = _boundingBox ?: Box3(Vector3.ZERO, Vector3.ZERO)

    fun updateTracking(pose: XRPose?, state: XRTrackingState, boundingBox: Box3?) {
        _pose = pose
        trackingState = state
        _boundingBox = boundingBox
    }
}

/**
 * Default implementation of XREnvironmentProbe
 */
class DefaultXREnvironmentProbe(
    override val id: String,
    override val pose: XRPose,
    override val extent: Vector3
) : XREnvironmentProbe {
    override var environmentTexture: XRCubeMap? = null
        private set

    fun updateCubeMap(cubeMap: XRCubeMap?) {
        environmentTexture = cubeMap
    }
}

/**
 * AR hit testing manager
 */
class ARHitTestManager(
    private val session: XRSession
) {
    private val hitTestSources = mutableMapOf<String, DefaultXRHitTestSource>()
    private val transientSources = mutableMapOf<String, DefaultXRTransientInputHitTestSource>()

    suspend fun requestHitTestSource(
        space: XRSpace,
        entityTypes: Set<String> = setOf("plane")
    ): XRHitTestSource {
        val source = DefaultXRHitTestSource(
            id = generateHitTestSourceId(),
            space = space,
            entityTypes = entityTypes
        )

        hitTestSources[source.id] = source
        return source
    }

    suspend fun requestTransientInputHitTestSource(
        profile: String
    ): XRTransientInputHitTestSource {
        val source = DefaultXRTransientInputHitTestSource(
            id = generateTransientSourceId(),
            profile = profile
        )

        transientSources[source.id] = source
        return source
    }

    suspend fun performHitTest(
        source: XRHitTestSource,
        frame: XRFrame
    ): List<XRHitTestResult> {
        if (source !is DefaultXRHitTestSource || source.isCancelled()) {
            return emptyList()
        }

        return performPlatformHitTest(source, frame)
    }

    suspend fun performTransientHitTest(
        source: XRTransientInputHitTestSource
    ): List<XRTransientInputHitTestResult> {
        if (source !is DefaultXRTransientInputHitTestSource || source.isCancelled()) {
            return emptyList()
        }

        return performPlatformTransientHitTest(source)
    }

    fun cancelSource(sourceId: String) {
        hitTestSources.remove(sourceId)
        transientSources.remove(sourceId)
    }

    fun dispose() {
        hitTestSources.clear()
        transientSources.clear()
    }
}

/**
 * Platform plane data
 */
data class PlaneData(
    val id: String,
    val polygon: List<Vector2>,
    val orientation: XRPlaneOrientation,
    val semanticLabel: String? = null
)

/**
 * Image tracking result
 */
data class ImageTrackingResult(
    val pose: XRPose,
    val state: XRTrackingState,
    val extent: Vector2
)

/**
 * Object tracking result
 */
data class ObjectTrackingResult(
    val pose: XRPose,
    val state: XRTrackingState,
    val boundingBox: Box3
)

/**
 * Environment probe data
 */
data class EnvironmentProbeData(
    val id: String,
    val pose: XRPose,
    val extent: Vector3,
    val cubeMap: XRCubeMap?
)

/**
 * Light estimation data
 */
data class LightEstimationData(
    val sphericalHarmonics: FloatArray?,
    val primaryDirection: Vector3,
    val primaryIntensity: Color,
    val environmentMap: XRCubeMap?
)

// Platform-specific functions (will be implemented via expect/actual)
internal expect fun isPeopleOcclusionSupported(): Boolean
internal expect fun enablePlatformPlaneDetection(enabled: Boolean)
internal expect suspend fun getPlatformDetectedPlanes(): List<PlaneData>
internal expect suspend fun registerImageTargetWithPlatform(target: XRImageTarget): Boolean
internal expect fun unregisterImageTargetFromPlatform(target: XRImageTarget)
internal expect suspend fun trackImageWithPlatform(target: XRImageTarget): ImageTrackingResult?
internal expect suspend fun registerObjectTargetWithPlatform(target: XRObjectTarget): Boolean
internal expect fun unregisterObjectTargetFromPlatform(target: XRObjectTarget)
internal expect suspend fun trackObjectWithPlatform(target: XRObjectTarget): ObjectTrackingResult?
internal expect fun enablePlatformEnvironmentProbes(enabled: Boolean)
internal expect suspend fun getPlatformEnvironmentProbes(): List<EnvironmentProbeData>
internal expect suspend fun getPlatformLightEstimation(): LightEstimationData?
internal expect fun enablePlatformOcclusion(enabled: Boolean)
internal expect suspend fun getPlatformOcclusionTexture(): Texture?
internal expect fun enablePlatformPeopleOcclusion(enabled: Boolean)
internal expect suspend fun cancelPlatformHitTestSource(source: DefaultXRHitTestSource)
internal expect suspend fun cancelPlatformTransientHitTestSource(source: DefaultXRTransientInputHitTestSource)
internal expect suspend fun performPlatformHitTest(
    source: DefaultXRHitTestSource,
    frame: XRFrame
): List<XRHitTestResult>
internal expect suspend fun performPlatformTransientHitTest(
    source: DefaultXRTransientInputHitTestSource
): List<XRTransientInputHitTestResult>
internal expect fun calculateSphericalHarmonics(probe: XREnvironmentProbe): FloatArray?
internal expect fun estimatePrimaryLightDirection(probe: XREnvironmentProbe): Vector3?
internal expect fun estimatePrimaryLightIntensity(probe: XREnvironmentProbe): Color?

// Utility functions
private fun generateImageTargetId(): String = "xrit_${System.currentTimeMillis()}"
private fun generateObjectTargetId(): String = "xrot_${System.currentTimeMillis()}"
private fun generateHitTestSourceId(): String = "xrhts_${System.currentTimeMillis()}"
private fun generateTransientSourceId(): String = "xrthts_${System.currentTimeMillis()}"
private fun generateAnchorId(): String = "xra_${System.currentTimeMillis()}"