/**
 * XR Types - Main Export File
 * Re-exports all XR types from modularized files
 *
 * This file serves as the main entry point for XR types.
 * Actual implementations are in:
 * - XRCoreTypes.kt: Core XR system, sessions, spaces, poses
 * - XRInputTypes.kt: Input sources, controllers, hands, gamepads
 * - XRARTypes.kt: AR-specific features (hit testing, anchors, planes)
 * - XREnums.kt: All XR enumerations
 */
package io.kreekt.xr

/**
 * Placeholder interfaces that need to be defined elsewhere in the codebase
 * These are referenced by XRARTypes.kt but not part of this refactoring
 */

/**
 * XR Session interface (placeholder - implement elsewhere)
 */
interface XRSession {
    // To be implemented by platform-specific code
}

/**
 * XR Frame interface (placeholder - implement elsewhere)
 */
interface XRFrame {
    // To be implemented by platform-specific code
}

/**
 * XR Light Probe interface (placeholder - implement elsewhere)
 */
interface XRLightProbe {
    val probeSpace: XRSpace
    fun addEventListener(type: String, listener: (Any) -> Unit)
    fun removeEventListener(type: String, listener: (Any) -> Unit)
}

// Note: All other XR types are defined in the modularized files:
// - XRCoreTypes.kt
// - XRInputTypes.kt
// - XRARTypes.kt
// - XREnums.kt
