/**
 * JVM implementations for XR anchor platform functions
 * These are placeholder implementations since XR is not available on JVM
 */
package io.kreekt.xr

import io.kreekt.core.math.Matrix4
import io.kreekt.core.math.Vector3

// Placeholder implementations for XR anchor functions on JVM
internal actual suspend fun savePersistentAnchor(handle: String, pose: XRPose) {
    // No-op on JVM
}

internal actual suspend fun removePersistentAnchor(handle: String) {
    // No-op on JVM
}

internal actual suspend fun loadPersistentAnchorsFromPlatform(): List<PersistentAnchorData> = emptyList()

internal actual suspend fun checkPlatformTrackingState(anchorId: String): XRTrackingState =
    XRTrackingState.NOT_TRACKING

internal actual suspend fun getPlatformAnchorPose(anchorId: String): XRPose? = null

internal actual fun performCoordinateTransform(
    position: Vector3,
    fromSpace: XRReferenceSpace,
    toSpace: XRReferenceSpace
): Vector3? = position

internal actual fun recalculatePoseFromOrigin(pose: XRPose, originTransform: Matrix4): XRPose = pose

internal actual fun evaluatePlatformTrackingQuality(): TrackingQuality = TrackingQuality.NONE

internal actual suspend fun uploadAnchorToPlatformCloud(
    anchor: XRAnchor,
    metadata: Map<String, Any>
): String = ""

internal actual suspend fun downloadAnchorFromPlatformCloud(cloudId: String): PersistentAnchorData =
    PersistentAnchorData(
        id = "",
        handle = "",
        pose = XRPose(transform = Matrix4.identity()),
        createdTime = 0L,
        metadata = emptyMap()
    )

internal actual suspend fun listAnchorsFromPlatformCloud(filter: Map<String, Any>): List<CloudAnchor> = emptyList()

internal actual suspend fun deleteAnchorFromPlatformCloud(cloudId: String) {
    // No-op on JVM
}

internal actual fun getPlatformSpaceTransform(
    fromSpace: XRReferenceSpace,
    toSpace: XRReferenceSpace
): Matrix4 = Matrix4.identity()