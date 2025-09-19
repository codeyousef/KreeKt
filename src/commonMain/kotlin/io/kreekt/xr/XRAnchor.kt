/**
 * XR Anchor and Spatial Tracking Implementation
 * Provides spatial anchor management for persistent world tracking
 */
package io.kreekt.xr

import io.kreekt.core.math.*
import kotlinx.coroutines.*
import kotlin.time.*

/**
 * Default implementation of XRAnchor interface
 * Manages spatial anchors for world-locked content
 */
class DefaultXRAnchor(
    override val id: String,
    private val initialPose: XRPose,
    private val space: XRSpace,
    override val persistent: Boolean = false
) : XRAnchor {
    override val anchorSpace: XRSpace = DefaultXRSpace()
    override var lastChangedTime: Long = System.currentTimeMillis()
        private set

    private var _trackingState: XRTrackingState = XRTrackingState.TRACKING
    private var currentPose: XRPose = initialPose
    private var deleted = false
    private var persistentHandle: String? = null

    private val trackingUpdateJob: Job = GlobalScope.launch {
        while (!deleted) {
            updateTrackingState()
            delay(100) // Update at 10Hz
        }
    }

    override suspend fun requestPersistentHandle(): XRResult<String> {
        if (!persistent) {
            return XRResult.Error(
                XRException.InvalidState("Anchor is not persistent")
            )
        }

        if (deleted) {
            return XRResult.Error(
                XRException.InvalidState("Anchor has been deleted")
            )
        }

        return try {
            val handle = persistentHandle ?: createPersistentHandle()
            persistentHandle = handle
            savePersistentAnchor(handle, currentPose)
            XRResult.Success(handle)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Failed to create persistent handle: ${e.message}")
            )
        }
    }

    override suspend fun delete(): XRResult<Unit> {
        if (deleted) {
            return XRResult.Error(
                XRException.InvalidState("Anchor already deleted")
            )
        }

        return try {
            // Clean up persistent storage if applicable
            persistentHandle?.let { handle ->
                removePersistentAnchor(handle)
            }

            // Stop tracking
            trackingUpdateJob.cancel()
            deleted = true
            _trackingState = XRTrackingState.STOPPED

            XRResult.Success(Unit)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Failed to delete anchor: ${e.message}")
            )
        }
    }

    override fun isTracked(): Boolean {
        return _trackingState == XRTrackingState.TRACKING && !deleted
    }

    override fun getTrackingState(): XRTrackingState {
        return _trackingState
    }

    fun getPose(): XRPose = currentPose

    fun updatePose(pose: XRPose) {
        if (!deleted) {
            currentPose = pose
            lastChangedTime = System.currentTimeMillis()
        }
    }

    private suspend fun updateTrackingState() {
        if (deleted) {
            _trackingState = XRTrackingState.STOPPED
            return
        }

        val newState = checkPlatformTrackingState(id)
        if (newState != _trackingState) {
            _trackingState = newState
            lastChangedTime = System.currentTimeMillis()

            // Update pose if tracking recovered
            if (newState == XRTrackingState.TRACKING) {
                val platformPose = getPlatformAnchorPose(id)
                if (platformPose != null) {
                    currentPose = platformPose
                }
            }
        }
    }

    private fun createPersistentHandle(): String {
        return "persistent_anchor_${id}_${System.currentTimeMillis()}"
    }
}

/**
 * Spatial tracking manager for world understanding
 */
class SpatialTrackingManager(
    private val session: XRSession
) {
    private val anchors = mutableMapOf<String, DefaultXRAnchor>()
    private val persistentAnchors = mutableMapOf<String, PersistentAnchorData>()
    private val trackingListeners = mutableListOf<SpatialTrackingListener>()

    private var worldTrackingState = XRTrackingState.TRACKING
    private var trackingQuality = TrackingQuality.GOOD
    private var trackingJob: Job? = null

    init {
        startTrackingUpdates()
    }

    /**
     * Create a new spatial anchor at the specified pose
     */
    fun createAnchor(
        pose: XRPose,
        space: XRSpace,
        persistent: Boolean = false
    ): XRAnchor {
        val anchor = DefaultXRAnchor(
            id = generateAnchorId(),
            initialPose = pose,
            space = space,
            persistent = persistent
        )

        anchors[anchor.id] = anchor

        // Notify listeners
        trackingListeners.forEach { it.onAnchorAdded(anchor) }

        return anchor
    }

    /**
     * Create an anchor from a hit test result
     */
    suspend fun createAnchorFromHitTest(
        hitResult: XRHitTestResult
    ): XRAnchor? {
        return hitResult.createAnchor()
    }

    /**
     * Load persistent anchors from storage
     */
    suspend fun loadPersistentAnchors(): List<XRAnchor> {
        return try {
            val loadedAnchors = loadPersistentAnchorsFromPlatform()

            loadedAnchors.forEach { anchorData ->
                val anchor = DefaultXRAnchor(
                    id = anchorData.id,
                    initialPose = anchorData.pose,
                    space = DefaultXRSpace(),
                    persistent = true
                )

                anchors[anchor.id] = anchor
                persistentAnchors[anchorData.handle] = anchorData
            }

            anchors.values.filter { it.persistent }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get all tracked anchors
     */
    fun getTrackedAnchors(): List<XRAnchor> {
        return anchors.values.filter { it.isTracked() }
    }

    /**
     * Remove an anchor
     */
    suspend fun removeAnchor(anchor: XRAnchor): XRResult<Unit> {
        val result = anchor.delete()

        if (result is XRResult.Success) {
            anchors.remove(anchor.id)
            trackingListeners.forEach { it.onAnchorRemoved(anchor) }
        }

        return result
    }

    /**
     * Update world tracking state
     */
    fun updateWorldTracking(state: XRTrackingState, quality: TrackingQuality) {
        worldTrackingState = state
        trackingQuality = quality

        trackingListeners.forEach {
            it.onTrackingStateChanged(state, quality)
        }

        // Pause anchor updates if tracking is lost
        when (state) {
            XRTrackingState.PAUSED -> pauseAnchorUpdates()
            XRTrackingState.STOPPED -> stopAnchorUpdates()
            XRTrackingState.TRACKING -> resumeAnchorUpdates()
        }
    }

    /**
     * Transform coordinates between different reference spaces
     */
    fun transformCoordinates(
        position: Vector3,
        fromSpace: XRReferenceSpace,
        toSpace: XRReferenceSpace
    ): Vector3? {
        if (!isTrackingActive()) return null

        return performCoordinateTransform(position, fromSpace, toSpace)
    }

    /**
     * Add a spatial tracking listener
     */
    fun addTrackingListener(listener: SpatialTrackingListener) {
        trackingListeners.add(listener)
    }

    /**
     * Remove a spatial tracking listener
     */
    fun removeTrackingListener(listener: SpatialTrackingListener) {
        trackingListeners.remove(listener)
    }

    /**
     * Get current tracking quality
     */
    fun getTrackingQuality(): TrackingQuality = trackingQuality

    /**
     * Check if world tracking is active
     */
    fun isTrackingActive(): Boolean {
        return worldTrackingState == XRTrackingState.TRACKING
    }

    /**
     * Reset world tracking origin
     */
    fun resetTrackingOrigin() {
        // Reset all anchor poses relative to new origin
        val originTransform = Matrix4.identity()

        anchors.values.forEach { anchor ->
            val relativePose = recalculatePoseFromOrigin(anchor.getPose(), originTransform)
            anchor.updatePose(relativePose)
        }

        trackingListeners.forEach { it.onTrackingOriginReset() }
    }

    private fun startTrackingUpdates() {
        trackingJob = GlobalScope.launch {
            while (true) {
                if (isTrackingActive()) {
                    updateAnchorPoses()
                    checkTrackingQuality()
                }
                delay(50) // Update at 20Hz
            }
        }
    }

    private suspend fun updateAnchorPoses() {
        anchors.values.forEach { anchor ->
            if (anchor.isTracked()) {
                val platformPose = getPlatformAnchorPose(anchor.id)
                if (platformPose != null) {
                    val previousPose = anchor.getPose()
                    anchor.updatePose(platformPose)

                    // Notify listeners if pose changed significantly
                    if (hasPoseChangedSignificantly(previousPose, platformPose)) {
                        trackingListeners.forEach {
                            it.onAnchorUpdated(anchor)
                        }
                    }
                }
            }
        }
    }

    private fun checkTrackingQuality() {
        val newQuality = evaluatePlatformTrackingQuality()
        if (newQuality != trackingQuality) {
            trackingQuality = newQuality
            trackingListeners.forEach {
                it.onTrackingQualityChanged(newQuality)
            }
        }
    }

    private fun pauseAnchorUpdates() {
        anchors.values.forEach { anchor ->
            // Keep last known pose but mark as paused
        }
    }

    private fun stopAnchorUpdates() {
        trackingJob?.cancel()
        trackingJob = null
    }

    private fun resumeAnchorUpdates() {
        if (trackingJob == null || !trackingJob!!.isActive) {
            startTrackingUpdates()
        }
    }

    private fun hasPoseChangedSignificantly(
        oldPose: XRPose,
        newPose: XRPose
    ): Boolean {
        val positionDelta = oldPose.position.distanceTo(newPose.position)
        val rotationDelta = oldPose.orientation.angleTo(newPose.orientation)

        return positionDelta > 0.01f || // 1cm threshold
                rotationDelta > 1f // 1 degree threshold
    }

    fun dispose() {
        trackingJob?.cancel()
        anchors.values.forEach { anchor ->
            runBlocking { anchor.delete() }
        }
        anchors.clear()
        persistentAnchors.clear()
        trackingListeners.clear()
    }
}

/**
 * Interface for spatial tracking events
 */
interface SpatialTrackingListener {
    fun onAnchorAdded(anchor: XRAnchor)
    fun onAnchorUpdated(anchor: XRAnchor)
    fun onAnchorRemoved(anchor: XRAnchor)
    fun onTrackingStateChanged(state: XRTrackingState, quality: TrackingQuality)
    fun onTrackingQualityChanged(quality: TrackingQuality)
    fun onTrackingOriginReset()
}

/**
 * Tracking quality enumeration
 */
enum class TrackingQuality {
    NONE,       // No tracking available
    LIMITED,    // Tracking with limited accuracy
    FAIR,       // Moderate tracking quality
    GOOD,       // Good tracking quality
    EXCELLENT   // Excellent tracking quality
}

/**
 * Persistent anchor data structure
 */
data class PersistentAnchorData(
    val id: String,
    val handle: String,
    val pose: XRPose,
    val createdTime: Long,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Anchor cloud service for cross-device persistence
 */
class AnchorCloudService(
    private val apiEndpoint: String
) {
    private val uploadedAnchors = mutableMapOf<String, CloudAnchor>()

    /**
     * Upload an anchor to the cloud
     */
    suspend fun uploadAnchor(
        anchor: XRAnchor,
        metadata: Map<String, Any> = emptyMap()
    ): XRResult<String> {
        if (!anchor.persistent) {
            return XRResult.Error(
                XRException.InvalidState("Only persistent anchors can be uploaded")
            )
        }

        return try {
            val cloudId = uploadAnchorToPlatformCloud(anchor, metadata)

            uploadedAnchors[cloudId] = CloudAnchor(
                cloudId = cloudId,
                localId = anchor.id,
                uploadTime = System.currentTimeMillis(),
                metadata = metadata
            )

            XRResult.Success(cloudId)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Failed to upload anchor: ${e.message}")
            )
        }
    }

    /**
     * Download an anchor from the cloud
     */
    suspend fun downloadAnchor(cloudId: String): XRResult<XRAnchor> {
        return try {
            val anchorData = downloadAnchorFromPlatformCloud(cloudId)

            val anchor = DefaultXRAnchor(
                id = generateAnchorId(),
                initialPose = anchorData.pose,
                space = DefaultXRSpace(),
                persistent = true
            )

            XRResult.Success(anchor)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Failed to download anchor: ${e.message}")
            )
        }
    }

    /**
     * List available cloud anchors
     */
    suspend fun listCloudAnchors(
        filter: Map<String, Any> = emptyMap()
    ): List<CloudAnchor> {
        return try {
            val cloudAnchors = listAnchorsFromPlatformCloud(filter)
            cloudAnchors
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Delete a cloud anchor
     */
    suspend fun deleteCloudAnchor(cloudId: String): XRResult<Unit> {
        return try {
            deleteAnchorFromPlatformCloud(cloudId)
            uploadedAnchors.remove(cloudId)
            XRResult.Success(Unit)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Failed to delete cloud anchor: ${e.message}")
            )
        }
    }
}

/**
 * Cloud anchor data structure
 */
data class CloudAnchor(
    val cloudId: String,
    val localId: String,
    val uploadTime: Long,
    val metadata: Map<String, Any>
)

/**
 * Coordinate system manager for spatial transformations
 */
object CoordinateSystemManager {
    /**
     * Transform a pose from one reference space to another
     */
    fun transformPose(
        pose: XRPose,
        fromSpace: XRReferenceSpace,
        toSpace: XRReferenceSpace
    ): XRPose {
        val transform = getTransformBetweenSpaces(fromSpace, toSpace)
        return applyTransformToPose(pose, transform)
    }

    /**
     * Convert world coordinates to screen coordinates
     */
    fun worldToScreen(
        worldPosition: Vector3,
        viewMatrix: Matrix4,
        projectionMatrix: Matrix4,
        viewport: XRViewport
    ): Vector2 {
        // Transform world position to clip space
        val clipSpace = worldPosition.clone()
            .applyMatrix4(viewMatrix)
            .applyMatrix4(projectionMatrix)

        // Perform perspective divide
        val ndcX = clipSpace.x / clipSpace.z
        val ndcY = clipSpace.y / clipSpace.z

        // Convert to screen coordinates
        val screenX = ((ndcX + 1) * 0.5f * viewport.width) + viewport.x
        val screenY = ((1 - ndcY) * 0.5f * viewport.height) + viewport.y

        return Vector2(screenX, screenY)
    }

    /**
     * Convert screen coordinates to world ray
     */
    fun screenToWorldRay(
        screenPosition: Vector2,
        viewMatrix: Matrix4,
        projectionMatrix: Matrix4,
        viewport: XRViewport
    ): Ray {
        // Convert screen to NDC
        val ndcX = ((screenPosition.x - viewport.x) / viewport.width) * 2 - 1
        val ndcY = 1 - ((screenPosition.y - viewport.y) / viewport.height) * 2

        // Create ray in view space
        val invProjection = projectionMatrix.inverse()
        val nearPoint = Vector3(ndcX, ndcY, -1f).applyMatrix4(invProjection)
        val farPoint = Vector3(ndcX, ndcY, 1f).applyMatrix4(invProjection)

        // Transform to world space
        val invView = viewMatrix.inverse()
        nearPoint.applyMatrix4(invView)
        farPoint.applyMatrix4(invView)

        val direction = farPoint.sub(nearPoint).normalize()
        return Ray(nearPoint, direction)
    }

    private fun getTransformBetweenSpaces(
        fromSpace: XRReferenceSpace,
        toSpace: XRReferenceSpace
    ): Matrix4 {
        // Platform-specific implementation
        return getPlatformSpaceTransform(fromSpace, toSpace)
    }

    private fun applyTransformToPose(pose: XRPose, transform: Matrix4): XRPose {
        val newPosition = pose.position.clone().applyMatrix4(transform)
        val rotationMatrix = Matrix4.makeRotationFromQuaternion(pose.orientation)
        rotationMatrix.multiplyMatrices(transform, rotationMatrix)
        val newOrientation = Quaternion().setFromRotationMatrix(rotationMatrix)

        return DefaultXRPose(
            transform = transform.clone().multiply(pose.transform),
            position = newPosition,
            orientation = newOrientation,
            linearVelocity = pose.linearVelocity?.clone()?.applyMatrix4(transform),
            angularVelocity = pose.angularVelocity
        )
    }
}

/**
 * Ray data structure for hit testing
 */
data class Ray(
    val origin: Vector3,
    val direction: Vector3
)

// Platform-specific functions (will be implemented via expect/actual)
internal expect suspend fun savePersistentAnchor(handle: String, pose: XRPose)
internal expect suspend fun removePersistentAnchor(handle: String)
internal expect suspend fun loadPersistentAnchorsFromPlatform(): List<PersistentAnchorData>
internal expect suspend fun checkPlatformTrackingState(anchorId: String): XRTrackingState
internal expect suspend fun getPlatformAnchorPose(anchorId: String): XRPose?
internal expect fun performCoordinateTransform(
    position: Vector3,
    fromSpace: XRReferenceSpace,
    toSpace: XRReferenceSpace
): Vector3?
internal expect fun recalculatePoseFromOrigin(pose: XRPose, originTransform: Matrix4): XRPose
internal expect fun evaluatePlatformTrackingQuality(): TrackingQuality
internal expect suspend fun uploadAnchorToPlatformCloud(
    anchor: XRAnchor,
    metadata: Map<String, Any>
): String
internal expect suspend fun downloadAnchorFromPlatformCloud(cloudId: String): PersistentAnchorData
internal expect suspend fun listAnchorsFromPlatformCloud(filter: Map<String, Any>): List<CloudAnchor>
internal expect suspend fun deleteAnchorFromPlatformCloud(cloudId: String)
internal expect fun getPlatformSpaceTransform(
    fromSpace: XRReferenceSpace,
    toSpace: XRReferenceSpace
): Matrix4

// Utility function
private fun generateAnchorId(): String = "xra_${System.currentTimeMillis()}"