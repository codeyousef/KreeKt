package io.kreekt.xr.input

import io.kreekt.xr.EyeTrackingData
import io.kreekt.xr.XRHandJoint
import io.kreekt.xr.XRJointPose

/**
 * Native implementation of XR input functions
 */
// Note: getPlatformHandJointPoses with (inputSource, hand, baseSpace) is in ARPlatform.native.kt (io.kreekt.xr package)

internal actual suspend fun getPlatformHandJointPoses(
    hand: DefaultXRHand
): Map<XRHandJoint, XRJointPose> = emptyMap()

internal actual suspend fun getPlatformEyeTrackingData(): EyeTrackingData? = null