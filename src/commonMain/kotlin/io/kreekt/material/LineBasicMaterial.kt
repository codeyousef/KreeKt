package io.kreekt.material

import io.kreekt.core.math.Color
import io.kreekt.core.scene.Material

/**
 * Basic material for line rendering
 */
class LineBasicMaterial(
    var color: Color = Color(0xffffff),
    var linewidth: Float = 1f,
    var linecap: String = "round",
    var linejoin: String = "round",
    var vertexColors: Boolean = false,
    var fog: Boolean = true,
    var toneMapped: Boolean = true,
    materialName: String = "LineBasicMaterial"
) : Material {
    override val id: Int = generateId()
    override val name: String = materialName
    override var needsUpdate: Boolean = true
    override var visible: Boolean = true

    private companion object {
        private var nextId = 0
        private fun generateId(): Int = ++nextId
    }

    fun clone(): LineBasicMaterial = LineBasicMaterial(
        color.clone(),
        linewidth,
        linecap,
        linejoin,
        vertexColors,
        fog,
        toneMapped,
        name
    )
}