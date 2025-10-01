package io.kreekt.material

import io.kreekt.core.math.Color

/**
 * Basic mesh material for simple rendering
 * Three.js compatible MeshBasicMaterial
 */
class MeshBasicMaterial(
    var color: Color = Color.WHITE,
    var wireframe: Boolean = false,
    var wireframeLinewidth: Float = 1f,
    transparent: Boolean = false,
    opacity: Float = 1f
) : Material() {

    override val type: String = "MeshBasicMaterial"

    init {
        this.transparent = transparent
        this.opacity = opacity
    }

    override fun clone(): MeshBasicMaterial {
        val material = MeshBasicMaterial(
            color = color.clone(),
            wireframe = wireframe,
            wireframeLinewidth = wireframeLinewidth,
            transparent = transparent,
            opacity = opacity
        )
        material.copy(this)
        return material
    }

    override fun copy(source: Material): Material {
        super.copy(source)
        if (source is MeshBasicMaterial) {
            color = source.color.clone()
            wireframe = source.wireframe
            wireframeLinewidth = source.wireframeLinewidth
        }
        return this
    }
}
