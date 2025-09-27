/**
 * Android implementation of XR system
 * Provides stub implementation without ARCore dependencies
 */
package io.kreekt.xr

import io.kreekt.core.math.*

/**
 * Android-specific XR system implementation
 * This is a stub implementation that doesn't require ARCore
 */
actual class XRSystemImpl : DefaultARSystem() {
    override fun isSupported(): Boolean = false

    override fun getSupportedSessionModes(): List<XRSessionMode> = emptyList()

    override fun getSupportedFeatures(mode: XRSessionMode): List<XRFeature> = emptyList()

    override fun checkPermissions(features: List<XRFeature>): Map<XRFeature, PermissionState> =
        features.associateWith { PermissionState.DENIED }

    override suspend fun requestSession(
        mode: XRSessionMode,
        features: List<XRFeature>
    ): XRResult<XRSession> = XRResult.Error(XRException.NotSupported("XR not supported on Android without ARCore"))
}

/**
 * Android-specific implementations for XR platform functions
 */
internal actual suspend fun getPlatformHandJointPoses(
    hand: DefaultXRHand
): Map<XRHandJoint, XRJointPose> = emptyMap()

internal actual suspend fun getPlatformEyeTrackingData(): EyeTrackingData? = null