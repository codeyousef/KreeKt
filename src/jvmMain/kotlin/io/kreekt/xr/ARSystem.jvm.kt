/**
 * JVM implementations for AR system platform functions
 * These are placeholder implementations since AR is not available on JVM
 */
package io.kreekt.xr

import io.kreekt.core.math.Vector3
import io.kreekt.core.math.Matrix4
import io.kreekt.core.math.Color
import io.kreekt.renderer.Texture

// Placeholder implementations for AR system functions on JVM
internal actual fun isPeopleOcclusionSupported(): Boolean = false

internal actual fun enablePlatformPlaneDetection(enabled: Boolean) {
    // No-op on JVM
}

internal actual suspend fun getPlatformDetectedPlanes(): List<PlaneData> = emptyList()

internal actual suspend fun registerImageTargetWithPlatform(target: XRImageTarget): Boolean = false

internal actual fun unregisterImageTargetFromPlatform(target: XRImageTarget) {
    // No-op on JVM
}

internal actual suspend fun trackImageWithPlatform(target: XRImageTarget): ImageTrackingResult? = null

internal actual suspend fun registerObjectTargetWithPlatform(target: XRObjectTarget): Boolean = false

internal actual fun unregisterObjectTargetFromPlatform(target: XRObjectTarget) {
    // No-op on JVM
}

internal actual suspend fun trackObjectWithPlatform(target: XRObjectTarget): ObjectTrackingResult? = null

internal actual fun enablePlatformEnvironmentProbes(enabled: Boolean) {
    // No-op on JVM
}

internal actual suspend fun getPlatformEnvironmentProbes(): List<EnvironmentProbeData> = emptyList()

internal actual suspend fun getPlatformLightEstimation(): LightEstimationData? = null

internal actual fun enablePlatformOcclusion(enabled: Boolean) {
    // No-op on JVM
}

internal actual suspend fun getPlatformOcclusionTexture(): Texture? = null

internal actual fun enablePlatformPeopleOcclusion(enabled: Boolean) {
    // No-op on JVM
}

internal actual suspend fun cancelPlatformHitTestSource(source: DefaultXRHitTestSource) {
    // No-op on JVM
}

internal actual suspend fun cancelPlatformTransientHitTestSource(source: DefaultXRTransientInputHitTestSource) {
    // No-op on JVM
}

internal actual suspend fun performPlatformHitTest(
    origin: Vector3,
    direction: Vector3,
    hitTestType: XRHitTestType
): List<XRHitTestResult> = emptyList()

internal actual suspend fun performPlatformTransientHitTest(
    inputSource: XRInputSource,
    hitTestType: XRHitTestType
): List<XRTransientInputHitTestResult> = emptyList()

internal actual fun calculateSphericalHarmonics(probe: XREnvironmentProbe): FloatArray? = null

internal actual fun estimatePrimaryLightDirection(probe: XREnvironmentProbe): Vector3? = null

internal actual fun estimatePrimaryLightIntensity(probe: XREnvironmentProbe): Color? = null

internal actual fun performCoordinateTransform(
    pose: XRPose,
    fromSpace: XRReferenceSpace,
    toSpace: XRReferenceSpace,
    transform: Matrix4
): XRPose = pose

internal actual suspend fun getPlatformHandJointPoses(
    inputSource: XRInputSource,
    hand: XRHand,
    baseSpace: XRReferenceSpace
): List<XRJointPose> = emptyList()