package io.kreekt.audio

import io.kreekt.camera.Camera
import io.kreekt.core.math.Vector3
import io.kreekt.core.scene.Object3D

/**
 * Audio listener for 3D spatial audio
 * Typically attached to camera for first-person audio perspective
 */
expect class AudioListener(camera: Camera?) : Object3D {
    val up: Vector3
    override fun updateMatrixWorld(force: Boolean)
}