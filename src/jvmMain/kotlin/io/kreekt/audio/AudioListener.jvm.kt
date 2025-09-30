package io.kreekt.audio

import io.kreekt.camera.Camera
import io.kreekt.core.scene.Object3D
import io.kreekt.core.math.Vector3
import io.kreekt.core.math.Quaternion

actual class AudioListener actual constructor(camera: Camera?) : Object3D() {
    private val _camera = camera

    actual val up: Vector3 = Vector3(0f, 1f, 0f)

    actual override fun updateMatrixWorld(force: Boolean) {
        _camera?.let {
            position.copy(it.position)
            quaternion.copy(it.quaternion)
        }
        super.updateMatrixWorld(force)
    }
}