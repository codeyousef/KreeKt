/**
 * XR Controller Implementation
 * Provides controller input handling, haptic feedback, and pose tracking
 */
package io.kreekt.xr

import io.kreekt.core.math.*
import io.kreekt.scene.Object3D
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Default implementation of XRController interface
 * Manages controller state, input, and haptic feedback
 */
class DefaultXRController(
    override val inputSource: XRInputSource,
    private val session: XRSession
) : XRController {
    override var connected: Boolean = false
        private set

    override val grip: Object3D? by lazy { createGripObject() }
    override val pointer: Object3D? by lazy { createPointerObject() }

    private val connectedCallbacks = mutableListOf<() -> Unit>()
    private val disconnectedCallbacks = mutableListOf<() -> Unit>()
    private val buttonChangeCallbacks = mutableListOf<(Int, XRGamepadButton) -> Unit>()

    private var lastButtonStates: List<XRGamepadButton>? = null
    private var connectionMonitorJob: Job? = null

    override val hapticActuators: List<XRHapticActuator>
        get() = inputSource.gamepad?.hapticActuators ?: emptyList()

    init {
        startConnectionMonitoring()
    }

    override fun getButton(index: Int): XRGamepadButton? {
        return inputSource.gamepad?.buttons?.getOrNull(index)
    }

    override fun getAxis(index: Int): Float {
        return inputSource.gamepad?.axes?.getOrNull(index) ?: 0f
    }

    override fun isButtonPressed(index: Int): Boolean {
        return getButton(index)?.pressed ?: false
    }

    override fun getButtonValue(index: Int): Float {
        return getButton(index)?.value ?: 0f
    }

    override suspend fun pulse(intensity: Float, duration: Float): XRResult<Unit> {
        if (!connected) {
            return XRResult.Error(
                XRException.InvalidState("Controller not connected")
            )
        }

        val actuator = hapticActuators.firstOrNull()
            ?: return XRResult.Error(
                XRException.FeatureNotAvailable(XRFeature.HAND_TRACKING)
            )

        return try {
            val success = actuator.pulse(intensity.coerceIn(0f, 1f), duration)
            if (success is XRResult.Success && success.value) {
                XRResult.Success(Unit)
            } else {
                XRResult.Error(
                    XRException.InvalidState("Haptic pulse failed")
                )
            }
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Haptic pulse error: ${e.message}")
            )
        }
    }

    override suspend fun playEffect(effect: HapticEffect): XRResult<Unit> {
        if (!connected) {
            return XRResult.Error(
                XRException.InvalidState("Controller not connected")
            )
        }

        val actuator = hapticActuators.firstOrNull()
            ?: return XRResult.Error(
                XRException.FeatureNotAvailable(XRFeature.HAND_TRACKING)
            )

        return try {
            when (effect.type) {
                HapticEffectType.PULSE -> {
                    pulse(effect.intensity, effect.duration)
                }
                HapticEffectType.PATTERN -> {
                    playHapticPattern(effect.pattern ?: emptyList())
                }
                HapticEffectType.CONTINUOUS -> {
                    playContinuousHaptic(effect.intensity, effect.duration)
                }
            }
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Haptic effect error: ${e.message}")
            )
        }
    }

    override fun onConnected(callback: () -> Unit) {
        connectedCallbacks.add(callback)
    }

    override fun onDisconnected(callback: () -> Unit) {
        disconnectedCallbacks.add(callback)
    }

    override fun onButtonChanged(callback: (Int, XRGamepadButton) -> Unit) {
        buttonChangeCallbacks.add(callback)
    }

    private fun startConnectionMonitoring() {
        connectionMonitorJob = GlobalScope.launch {
            while (true) {
                val wasConnected = connected
                connected = checkControllerConnection()

                if (!wasConnected && connected) {
                    handleConnection()
                } else if (wasConnected && !connected) {
                    handleDisconnection()
                }

                if (connected) {
                    checkButtonChanges()
                }

                delay(16) // ~60Hz polling rate
            }
        }
    }

    private fun handleConnection() {
        connectedCallbacks.forEach { it() }
        updateControllerPose()
    }

    private fun handleDisconnection() {
        disconnectedCallbacks.forEach { it() }
        lastButtonStates = null
    }

    private fun checkButtonChanges() {
        val currentButtons = inputSource.gamepad?.buttons
        if (currentButtons != null && lastButtonStates != null) {
            currentButtons.forEachIndexed { index, button ->
                val lastButton = lastButtonStates?.getOrNull(index)
                if (lastButton != null && hasButtonChanged(button, lastButton)) {
                    buttonChangeCallbacks.forEach { it(index, button) }
                }
            }
        }
        lastButtonStates = currentButtons?.toList()
    }

    private fun hasButtonChanged(current: XRGamepadButton, previous: XRGamepadButton): Boolean {
        return current.pressed != previous.pressed ||
                current.touched != previous.touched ||
                abs(current.value - previous.value) > 0.01f
    }

    private suspend fun playHapticPattern(pattern: List<HapticPulse>): XRResult<Unit> {
        return try {
            pattern.forEach { pulse ->
                if (pulse.delay > 0) {
                    delay(pulse.delay.toLong())
                }
                pulse(pulse.intensity, pulse.duration)
            }
            XRResult.Success(Unit)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Pattern playback failed: ${e.message}")
            )
        }
    }

    private suspend fun playContinuousHaptic(
        intensity: Float,
        duration: Float
    ): XRResult<Unit> {
        return try {
            val pulseInterval = 16f // 16ms pulses for continuous effect
            val numPulses = (duration / pulseInterval).toInt()

            repeat(numPulses) {
                pulse(intensity, pulseInterval)
                delay(pulseInterval.toLong())
            }

            XRResult.Success(Unit)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Continuous haptic failed: ${e.message}")
            )
        }
    }

    private fun updateControllerPose() {
        grip?.let { gripObject ->
            inputSource.gripSpace?.let { space ->
                runBlocking {
                    val pose = session.getInputPose(inputSource, session.referenceSpace)
                    pose?.let {
                        gripObject.position.copy(it.position)
                        gripObject.quaternion.copy(it.orientation)
                    }
                }
            }
        }

        pointer?.let { pointerObject ->
            runBlocking {
                val pose = session.getInputPose(inputSource, session.referenceSpace)
                pose?.let {
                    pointerObject.position.copy(it.position)

                    // Apply pointer offset for ray direction
                    val pointerOffset = when (inputSource.targetRayMode) {
                        XRTargetRayMode.TRACKED_POINTER -> Vector3(0f, -0.05f, -0.1f)
                        XRTargetRayMode.GAZE -> Vector3(0f, 0f, -1f)
                        XRTargetRayMode.SCREEN -> Vector3(0f, 0f, -1f)
                    }

                    pointerObject.position.add(pointerOffset.applyQuaternion(it.orientation))
                    pointerObject.quaternion.copy(it.orientation)
                }
            }
        }
    }

    private fun createGripObject(): Object3D? {
        if (inputSource.gripSpace == null) return null

        return Object3D().apply {
            name = "controller_grip_${inputSource.handedness}"
            // Add visual representation based on controller profile
            addControllerModel(this)
        }
    }

    private fun createPointerObject(): Object3D? {
        return Object3D().apply {
            name = "controller_pointer_${inputSource.handedness}"
            // Add ray visualization
            addPointerRay(this)
        }
    }

    private fun addControllerModel(parent: Object3D) {
        // Load controller model based on profile
        val profile = inputSource.profiles.firstOrNull() ?: "generic-trigger"

        // Platform-specific model loading
        loadControllerModel(profile)?.let {
            parent.add(it)
        }
    }

    private fun addPointerRay(parent: Object3D) {
        // Create a simple ray visualization
        // This would typically create a line or cylinder mesh
        val ray = createRayVisualization()
        parent.add(ray)
    }

    fun dispose() {
        connectionMonitorJob?.cancel()
        connectedCallbacks.clear()
        disconnectedCallbacks.clear()
        buttonChangeCallbacks.clear()
    }
}

/**
 * Default implementation of XRInputSource
 */
class DefaultXRInputSource(
    override val handedness: XRHandedness,
    override val targetRayMode: XRTargetRayMode,
    override val profiles: List<String> = listOf("generic-trigger")
) : XRInputSource {
    override val targetRaySpace: XRSpace = DefaultXRSpace()
    override val gripSpace: XRSpace? = if (targetRayMode == XRTargetRayMode.TRACKED_POINTER) {
        DefaultXRSpace()
    } else null

    private var _gamepad: XRGamepad? = null
    override val gamepad: XRGamepad?
        get() = _gamepad

    private var _hand: XRHand? = null
    override val hand: XRHand?
        get() = _hand

    private var _gaze: XRGaze? = null
    override val gaze: XRGaze?
        get() = _gaze

    fun updateGamepad(gamepad: XRGamepad?) {
        _gamepad = gamepad
    }

    fun updateHand(hand: XRHand?) {
        _hand = hand
    }

    fun updateGaze(gaze: XRGaze?) {
        _gaze = gaze
    }
}

/**
 * Default implementation of XRGamepad
 */
class DefaultXRGamepad(
    override val buttons: List<XRGamepadButton>,
    override val axes: FloatArray,
    override val hapticActuators: List<XRHapticActuator> = emptyList()
) : XRGamepad

/**
 * Default implementation of XRGamepadButton
 */
data class DefaultXRGamepadButton(
    override val pressed: Boolean,
    override val touched: Boolean,
    override val value: Float
) : XRGamepadButton

/**
 * Default implementation of XRHapticActuator
 */
class DefaultXRHapticActuator(
    override val type: XRHapticActuatorType
) : XRHapticActuator {
    private var isVibrating = false
    private var vibrationJob: Job? = null

    override suspend fun pulse(value: Float, duration: Float): XRResult<Boolean> {
        return try {
            // Cancel any ongoing vibration
            vibrationJob?.cancel()

            isVibrating = true

            // Perform platform-specific haptic pulse
            val success = performPlatformHapticPulse(value, duration)

            vibrationJob = GlobalScope.launch {
                delay(duration.toLong())
                isVibrating = false
            }

            XRResult.Success(success)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Haptic pulse failed: ${e.message}")
            )
        }
    }

    override suspend fun playEffect(effect: HapticEffect): XRResult<Boolean> {
        return when (effect.type) {
            HapticEffectType.PULSE -> {
                pulse(effect.intensity, effect.duration)
            }
            HapticEffectType.PATTERN -> {
                playPattern(effect.pattern ?: emptyList())
            }
            HapticEffectType.CONTINUOUS -> {
                playContinuous(effect.intensity, effect.duration)
            }
        }
    }

    private suspend fun playPattern(pattern: List<HapticPulse>): XRResult<Boolean> {
        return try {
            vibrationJob?.cancel()

            vibrationJob = GlobalScope.launch {
                pattern.forEach { pulse ->
                    if (pulse.delay > 0) {
                        delay(pulse.delay.toLong())
                    }
                    performPlatformHapticPulse(pulse.intensity, pulse.duration)
                    delay(pulse.duration.toLong())
                }
            }

            XRResult.Success(true)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Pattern playback failed: ${e.message}")
            )
        }
    }

    private suspend fun playContinuous(
        intensity: Float,
        duration: Float
    ): XRResult<Boolean> {
        return try {
            vibrationJob?.cancel()

            vibrationJob = GlobalScope.launch {
                val endTime = System.currentTimeMillis() + duration.toLong()

                while (System.currentTimeMillis() < endTime) {
                    performPlatformHapticPulse(intensity, 16f) // 16ms pulses
                    delay(16)
                }
            }

            XRResult.Success(true)
        } catch (e: Exception) {
            XRResult.Error(
                XRException.InvalidState("Continuous haptic failed: ${e.message}")
            )
        }
    }

    fun dispose() {
        vibrationJob?.cancel()
        isVibrating = false
    }
}

/**
 * Default implementation of XRSpace
 */
class DefaultXRSpace : XRSpace {
    override val id: String = generateSpaceId()
}

/**
 * Controller profile definitions
 */
object ControllerProfiles {
    val OCULUS_TOUCH = "oculus-touch"
    val OCULUS_TOUCH_V2 = "oculus-touch-v2"
    val OCULUS_TOUCH_V3 = "oculus-touch-v3"
    val VALVE_INDEX = "valve-index"
    val HTC_VIVE = "htc-vive"
    val WINDOWS_MIXED_REALITY = "windows-mixed-reality"
    val SAMSUNG_ODYSSEY = "samsung-odyssey"
    val SAMSUNG_GEAR_VR = "samsung-gear-vr"
    val GOOGLE_DAYDREAM = "google-daydream"
    val MAGIC_LEAP_ONE = "magic-leap-one"
    val GENERIC_TRIGGER = "generic-trigger"
    val GENERIC_TOUCHPAD = "generic-touchpad"
    val GENERIC_BUTTON = "generic-button"
}

/**
 * Standard gamepad button mappings
 */
object GamepadButtonMapping {
    const val TRIGGER = 0
    const val GRIP = 1
    const val TOUCHPAD = 2
    const val THUMBSTICK = 3
    const val BUTTON_A = 4
    const val BUTTON_B = 5
    const val BUTTON_X = 6
    const val BUTTON_Y = 7
    const val MENU = 8
    const val SYSTEM = 9
}

/**
 * Standard gamepad axis mappings
 */
object GamepadAxisMapping {
    const val TOUCHPAD_X = 0
    const val TOUCHPAD_Y = 1
    const val THUMBSTICK_X = 2
    const val THUMBSTICK_Y = 3
}

/**
 * Haptic effect presets
 */
object HapticPresets {
    val CLICK = HapticEffect(
        type = HapticEffectType.PULSE,
        intensity = 0.8f,
        duration = 10f
    )

    val DOUBLE_CLICK = HapticEffect(
        type = HapticEffectType.PATTERN,
        intensity = 0.8f,
        duration = 100f,
        pattern = listOf(
            HapticPulse(0.8f, 10f, 0f),
            HapticPulse(0.8f, 10f, 50f)
        )
    )

    val SOFT_VIBRATION = HapticEffect(
        type = HapticEffectType.CONTINUOUS,
        intensity = 0.3f,
        duration = 200f
    )

    val STRONG_VIBRATION = HapticEffect(
        type = HapticEffectType.CONTINUOUS,
        intensity = 1.0f,
        duration = 300f
    )

    val RAMP_UP = HapticEffect(
        type = HapticEffectType.PATTERN,
        intensity = 1.0f,
        duration = 500f,
        pattern = List(10) { i ->
            HapticPulse(
                intensity = (i + 1) / 10f,
                duration = 50f,
                delay = 0f
            )
        }
    )

    val HEARTBEAT = HapticEffect(
        type = HapticEffectType.PATTERN,
        intensity = 1.0f,
        duration = 1000f,
        pattern = listOf(
            HapticPulse(1.0f, 100f, 0f),
            HapticPulse(0.7f, 80f, 150f),
            HapticPulse(1.0f, 100f, 500f),
            HapticPulse(0.7f, 80f, 150f)
        )
    )
}

// Platform-specific functions (will be implemented via expect/actual)
internal expect fun checkControllerConnection(): Boolean
internal expect fun loadControllerModel(profile: String): Object3D?
internal expect fun createRayVisualization(): Object3D
internal expect suspend fun performPlatformHapticPulse(intensity: Float, duration: Float): Boolean

// Utility function
private fun generateSpaceId(): String = "xrsp_${System.currentTimeMillis()}"