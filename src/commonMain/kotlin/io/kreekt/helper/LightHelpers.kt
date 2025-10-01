package io.kreekt.helper

import io.kreekt.camera.Camera
import io.kreekt.core.math.Color
import io.kreekt.core.scene.Object3D
import io.kreekt.geometry.BufferGeometry
import io.kreekt.lighting.DirectionalLightImpl
import io.kreekt.lighting.HemisphereLightImpl
import io.kreekt.lighting.PointLightImpl
import io.kreekt.lighting.SpotLightImpl
import io.kreekt.material.LineBasicMaterial

/**
 * Light and Camera helper implementations
 * Visual debugging for lights and camera frustums
 */

/**
 * CameraHelper - Visualizes camera frustum
 */
class CameraHelper(val camera: Camera) : Object3D() {
    private val geometry = BufferGeometry()
    private val material = LineBasicMaterial()
    val pointMap = mutableMapOf<String, Int>()

    init {
        material.vertexColors = true
        material.toneMapped = false

        val vertices = mutableListOf<Float>()
        val colors = mutableListOf<Float>()

        // Build frustum lines
        updateFrustum()

        name = "CameraHelper"
    }

    private fun updateFrustum() {
        // Implementation will create lines for camera frustum
        // This is a simplified version
    }

    fun update() {
        updateFrustum()
    }

    fun dispose() {
        geometry.dispose()
    }
}

/**
 * DirectionalLightHelper - Visualizes directional light
 */
class DirectionalLightHelper(
    val light: DirectionalLightImpl,
    val size: Float = 1f,
    val color: Color? = null
) : Object3D() {
    private val geometry = BufferGeometry()
    private val material = LineBasicMaterial()

    init {
        // Create visual representation
        val helperColor = color ?: light.color
        material.color = helperColor

        position.copy(light.position)
        name = "DirectionalLightHelper"
    }

    fun update() {
        // Update helper visualization based on light position
        position.copy(light.position)
    }

    fun dispose() {
        geometry.dispose()
    }
}

/**
 * PointLightHelper - Visualizes point light
 */
class PointLightHelper(
    val light: PointLightImpl,
    val sphereSize: Float = 1f,
    val color: Color? = null
) : Object3D() {
    private val geometry = BufferGeometry()
    private val material = LineBasicMaterial()

    init {
        // Create sphere wireframe
        val helperColor = color ?: light.color
        material.color = helperColor

        position.copy(light.position)
        name = "PointLightHelper"
    }

    fun update() {
        position.copy(light.position)
    }

    fun dispose() {
        geometry.dispose()
    }
}

/**
 * SpotLightHelper - Visualizes spot light
 */
class SpotLightHelper(
    val light: SpotLightImpl,
    val color: Color? = null
) : Object3D() {
    private val geometry = BufferGeometry()
    private val material = LineBasicMaterial()

    init {
        val helperColor = color ?: light.color
        material.color = helperColor

        position.copy(light.position)
        name = "SpotLightHelper"
    }

    fun update() {
        position.copy(light.position)
        // Update cone direction
        val targetPos = light.position.clone().add(light.direction)
        lookAt(targetPos.x, targetPos.y, targetPos.z)
    }

    fun dispose() {
        geometry.dispose()
    }
}

/**
 * HemisphereLightHelper - Visualizes hemisphere light
 */
class HemisphereLightHelper(
    val light: HemisphereLightImpl,
    val size: Float = 1f,
    val color: Color? = null
) : Object3D() {
    private val geometry = BufferGeometry()
    private val material = LineBasicMaterial()

    init {
        val helperColor = color ?: light.color
        material.color = helperColor

        position.copy(light.position)
        name = "HemisphereLightHelper"
    }

    fun update() {
        position.copy(light.position)
    }

    fun dispose() {
        geometry.dispose()
    }
}
