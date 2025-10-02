/**
 * XR Input Implementation - Hand Tracking and Eye Gaze
 * Provides comprehensive hand joint tracking, eye gaze, and gesture recognition
 */
package io.kreekt.xr

import io.kreekt.xr.input.*

// Re-export all input types and classes
typealias DefaultXRHand = io.kreekt.xr.input.DefaultXRHand
typealias DefaultXRGaze = io.kreekt.xr.input.DefaultXRGaze
typealias HandGestureDetector = io.kreekt.xr.input.HandGestureDetector
typealias XRInputSourceManager = io.kreekt.xr.input.XRInputSourceManager
typealias XRInputCallback = io.kreekt.xr.input.XRInputCallback
typealias HandGesture = io.kreekt.xr.input.HandGesture
typealias Finger = io.kreekt.xr.input.Finger
typealias EyeOpenness = io.kreekt.xr.input.EyeOpenness
typealias PupilDilation = io.kreekt.xr.input.PupilDilation
typealias HandCalibrationData = io.kreekt.xr.input.HandCalibrationData
typealias EyeCalibrationData = io.kreekt.xr.input.EyeCalibrationData
typealias EyeTrackingData = io.kreekt.xr.input.EyeTrackingData

// All XR input functionality is now organized into focused modules:
// - HandTracking.kt: Hand joint tracking, finger curl calculations, pinch detection
// - EyeTracking.kt: Eye gaze tracking, convergence, openness, and pupil dilation
// - GestureRecognition.kt: Gesture detection algorithms for hands
// - InputSourceManager.kt: Unified input source management and callbacks
