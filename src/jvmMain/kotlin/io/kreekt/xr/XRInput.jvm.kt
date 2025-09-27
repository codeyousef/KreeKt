/**
 * JVM implementations for XR input platform functions
 * These are placeholder implementations since XR is not available on JVM
 */
package io.kreekt.xr

// Placeholder implementations for XR input functions on JVM
internal actual suspend fun getPlatformHandJointPoses(
    hand: DefaultXRHand
): Map<XRHandJoint, XRJointPose> = emptyMap()

internal actual suspend fun getPlatformEyeTrackingData(): EyeTrackingData? = null