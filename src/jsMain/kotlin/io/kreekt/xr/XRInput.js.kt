package io.kreekt.xr

/**
 * JavaScript implementation of XR Input internal functions
 */

// Platform input functions for WebXR
internal actual suspend fun getPlatformHandJointPoses(
    hand: DefaultXRHand
): Map<XRHandJoint, DefaultXRJointPose> = emptyMap()

internal actual suspend fun getPlatformEyeTrackingData(): EyeTrackingData? = null