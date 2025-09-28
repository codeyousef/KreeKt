package io.kreekt.material

import io.kreekt.core.math.Color
import io.kreekt.core.scene.Material

/**
 * Simple material implementation for basic examples
 */
class SimpleMaterial(
    var albedo: Color = Color.WHITE,
    var metallic: Float = 0f,
    var roughness: Float = 1f,
    var emissive: Color = Color.BLACK,
    var transparent: Boolean = false,
    materialName: String = "SimpleMaterial"
) : Material {
    override val id: Int = generateId()
    override val name: String = materialName
    override var needsUpdate: Boolean = true
    override var visible: Boolean = true

    private companion object {
        private var nextId = 0
        private fun generateId(): Int = ++nextId
    }

    fun clone(): SimpleMaterial = SimpleMaterial(
        albedo.clone(),
        metallic,
        roughness,
        emissive.clone(),
        transparent,
        name
    )
}