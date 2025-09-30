/**
 * Audio System API Contract
 * Maps FR-A001 through FR-A010
 *
 * Constitutional Requirements:
 * - Cross-platform expect/actual for platform-specific audio APIs
 * - Type-safe audio parameter handling
 * - Performance: <5ms audio update per frame
 */

package io.kreekt.audio

import io.kreekt.camera.Camera
import io.kreekt.math.Vector3
import io.kreekt.math.Quaternion

/**
 * FR-A001, FR-A002: Positional 3D audio with distance attenuation
 *
 * Test Contract:
 * - MUST attach to camera for first-person audio
 * - MUST update position/orientation from camera matrix
 * - MUST provide global audio context
 */
expect class AudioListener(camera: Camera?) {
    val position: Vector3
    val rotation: Quaternion
    val up: Vector3

    fun updateMatrixWorld(force: Boolean = false)
    fun getWorldPosition(target: Vector3): Vector3
    fun getWorldQuaternion(target: Quaternion): Quaternion
}

/**
 * FR-A003, FR-A004: Positional audio source with 3D panning
 *
 * Test Contract:
 * - MUST support distance-based attenuation models
 * - MUST support directional cone-based attenuation
 * - MUST update 3D panning based on listener position
 * - MUST support Doppler effect (optional, platform-dependent)
 */
expect class PositionalAudio(listener: AudioListener) : Audio {
    var refDistance: Float      // Distance at which volume begins to decrease
    var maxDistance: Float       // Distance at which volume reaches minimum
    var rolloffFactor: Float     // Rate of volume decrease
    var distanceModel: DistanceModel

    // Directional cone
    var coneInnerAngle: Float    // Inner cone angle in radians
    var coneOuterAngle: Float    // Outer cone angle in radians
    var coneOuterGain: Float     // Gain outside outer cone

    fun setDirectionalCone(innerAngle: Float, outerAngle: Float, outerGain: Float)
}

/**
 * FR-A005: Base audio source
 *
 * Test Contract:
 * - MUST load audio from URL or buffer
 * - MUST support play/pause/stop controls
 * - MUST support volume and playback rate
 * - MUST support looping
 */
expect open class Audio(listener: AudioListener) {
    var volume: Float            // 0.0 to 1.0
    var playbackRate: Float      // Speed multiplier
    var loop: Boolean
    var autoplay: Boolean
    var isPlaying: Boolean
    val duration: Float

    fun load(url: String): Audio
    fun setBuffer(buffer: AudioBuffer): Audio
    fun play(delay: Float = 0f): Audio
    fun pause(): Audio
    fun stop(): Audio
    fun setVolume(value: Float): Audio
    fun setPlaybackRate(value: Float): Audio
    fun setLoop(value: Boolean): Audio

    fun onEnded(callback: () -> Unit)
}

/**
 * FR-A006: Audio buffer for decoded audio data
 *
 * Test Contract:
 * - MUST decode audio data on load
 * - MUST support multiple sample rates
 * - MUST be reusable across multiple Audio instances
 */
expect class AudioBuffer {
    val sampleRate: Float
    val length: Int
    val duration: Float
    val numberOfChannels: Int
}

/**
 * FR-A007: Audio loader for async loading
 *
 * Test Contract:
 * - MUST load audio files asynchronously
 * - MUST decode audio data
 * - MUST handle errors gracefully
 */
expect class AudioLoader {
    fun load(
        url: String,
        onLoad: (AudioBuffer) -> Unit,
        onProgress: ((Float) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    )

    suspend fun loadAsync(url: String): Result<AudioBuffer>
}

/**
 * FR-A008: Audio frequency analysis
 *
 * Test Contract:
 * - MUST provide real-time frequency data
 * - MUST provide time-domain data
 * - MUST support configurable FFT size
 */
expect class AudioAnalyser(audio: Audio, fftSize: Int = 2048) {
    var fftSize: Int
    val frequencyBinCount: Int
    var smoothingTimeConstant: Float

    fun getFrequencyData(): FloatArray
    fun getByteFrequencyData(): ByteArray
    fun getByteTimeDomainData(): ByteArray
    fun getAverageFrequency(): Float
}

/**
 * FR-A009: Distance model enumeration
 */
enum class DistanceModel {
    LINEAR,
    INVERSE,
    EXPONENTIAL
}

/**
 * FR-A010: Global audio context management
 *
 * Test Contract:
 * - MUST manage platform-specific audio context
 * - MUST handle context suspension/resume
 * - MUST provide sample rate information
 */
expect object AudioContext {
    val sampleRate: Float
    val state: AudioContextState

    suspend fun resume()
    suspend fun suspend()
    fun close()
}

enum class AudioContextState {
    SUSPENDED,
    RUNNING,
    CLOSED
}