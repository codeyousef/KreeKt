package io.kreekt.audio

import io.kreekt.camera.Camera
import io.kreekt.core.math.Vector3
import io.kreekt.core.scene.Object3D

actual class AudioListener actual constructor(camera: Camera?) : Object3D() {
    private val _camera = camera

    actual val up: Vector3 = Vector3(0f, 1f, 0f)

    actual override fun updateMatrixWorld(force: Boolean) {
        _camera?.let {
            // Copy position from camera
            position.copy(it.position)
            // Sync quaternion from camera's rotation (in case rotation was set directly)
            quaternion.setFromEuler(it.rotation)
        }
        super.updateMatrixWorld(force)
    }
}