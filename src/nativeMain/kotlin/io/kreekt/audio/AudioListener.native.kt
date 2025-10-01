package io.kreekt.audio

import io.kreekt.camera.Camera
import io.kreekt.core.math.Vector3
import io.kreekt.core.scene.Object3D

actual class AudioListener actual constructor(camera: Camera?) : Object3D() {
    private val _camera = camera
    private val _up = Vector3(0f, 1f, 0f)

    actual val up: Vector3
        get() = _up

    actual override fun updateMatrixWorld(force: Boolean) {
        super.updateMatrixWorld(force)
        // Update audio listener position/orientation based on matrix
    }
}
