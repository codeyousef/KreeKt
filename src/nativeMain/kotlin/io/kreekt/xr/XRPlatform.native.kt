package io.kreekt.xr

import io.kreekt.core.math.Vector3
import io.kreekt.core.math.Vector2
import io.kreekt.core.math.Matrix4
import io.kreekt.xr.PermissionState

/**
 * Native implementation of XRDepthInformation
 */
actual interface XRDepthInformation {
    actual val width: Int
    actual val height: Int
    actual val rawValueToMeters: Float
    actual fun getDepthInMeters(x: Float, y: Float): Float
}

/**
 * Native implementation of XR platform functions
 */

actual fun isPlatformXRSupported(): Boolean = false

actual fun isWebXRSupported(): Boolean = false

actual fun isImmersiveVRSupported(): Boolean = false

actual fun isImmersiveARSupported(): Boolean = false

actual fun getPlatformXRDevices(): List<XRDevice> = emptyList()

actual fun checkCameraPermission(): PermissionState = PermissionState.DENIED

actual fun checkEyeTrackingPermission(): PermissionState = PermissionState.DENIED

actual fun checkHandTrackingPermission(): PermissionState = PermissionState.DENIED

actual suspend fun getPlatformViewerPose(referenceSpace: XRReferenceSpace): XRViewerPose? = null

actual suspend fun getPlatformInputPose(inputSource: XRInputSource, referenceSpace: XRReferenceSpace): XRPose? = null

actual suspend fun getPlatformJointPose(joint: XRJointSpace, baseSpace: XRSpace): XRJointPose? = null

actual fun detectPlatformInputSources(): List<XRInputSource> = emptyList()

actual fun getBoundsFromPlatform(): List<Vector2> = emptyList()

actual fun calculateRelativePose(space: XRSpace, baseSpace: XRSpace): XRPose? = null

actual fun getPlatformTransientHitTestResults(source: XRTransientInputHitTestSource): List<XRTransientInputHitTestResult> = emptyList()

actual fun getPlatformLightEstimate(lightProbe: XRLightProbe): XRLightEstimate? = null

actual fun combineTransforms(first: XRPose, second: XRPose): XRPose = first