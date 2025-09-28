/**
 * JVM implementations for XR input platform functions
 * These are placeholder implementations since XR is not available on JVM
 */
package io.kreekt.xr

// JVM implementations for XR input functions (OpenXR simulation)
internal actual suspend fun getPlatformHandJointPoses(
    hand: DefaultXRHand
): Map<XRHandJoint, XRJointPose> = emptyMap()

internal actual suspend fun getPlatformEyeTrackingData(): EyeTrackingData? = null