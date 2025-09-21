/**
 * JVM implementations for XR input platform functions
 * These are placeholder implementations since XR is not available on JVM
 */
package io.kreekt.xr

// Placeholder implementations for XR input functions on JVM
internal actual suspend fun getPlatformHandJointPoses(
    inputSource: XRInputSource,
    hand: XRHand,
    baseSpace: XRReferenceSpace
): List<XRJointPose> = emptyList()

internal actual suspend fun getPlatformEyeTrackingData(): EyeTrackingData? = null