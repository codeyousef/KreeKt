/**
 * JVM implementations for XR input platform functions
 * These are placeholder implementations since XR is not available on JVM
 */
package io.kreekt.xr.input

import io.kreekt.xr.EyeTrackingData
import io.kreekt.xr.XRHandJoint
import io.kreekt.xr.XRJointPose

// JVM implementation for XR input functions (OpenXR simulation)
// Note: getPlatformHandJointPoses with (inputSource, hand, baseSpace) is in ARPlatform.jvm.kt (io.kreekt.xr package)

internal actual suspend fun getPlatformHandJointPoses(
    hand: DefaultXRHand
): Map<XRHandJoint, XRJointPose> = emptyMap()

internal actual suspend fun getPlatformEyeTrackingData(): EyeTrackingData? = null