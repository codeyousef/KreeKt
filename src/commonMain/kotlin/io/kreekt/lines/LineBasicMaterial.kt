package io.kreekt.lines

import io.kreekt.material.Material
import io.kreekt.core.math.Color

/**
 * LineBasicMaterial - Basic material for standard line rendering
 *
 * Based on Three.js LineBasicMaterial
 */
open class LineBasicMaterial(
    var color: Color = Color(0xffffff),
    var linewidth: Float = 1f,
    var linecap: String = "round",
    var linejoin: String = "round"
) : Material() {

    override val type: String = "LineBasicMaterial"

    override fun clone(): LineBasicMaterial {
        return LineBasicMaterial(
            color = color.clone(),
            linewidth = linewidth,
            linecap = linecap,
            linejoin = linejoin
        )
    }
}

/**
 * LineDashedMaterial - Material for dashed lines
 *
 * Based on Three.js LineDashedMaterial
 */
open class LineDashedMaterial(
    color: Color = Color(0xffffff),
    linewidth: Float = 1f,
    var dashSize: Float = 3f,
    var gapSize: Float = 1f,
    var scale: Float = 1f
) : LineBasicMaterial(color, linewidth) {

    override val type: String = "LineDashedMaterial"

    override fun clone(): LineDashedMaterial {
        return LineDashedMaterial(
            color = color.clone(),
            linewidth = linewidth,
            dashSize = dashSize,
            gapSize = gapSize,
            scale = scale
        )
    }

    fun generateShader(): Pair<String, String> {
        val vertexShader = """
            attribute vec3 position;
            uniform mat4 projectionMatrix;
            uniform mat4 modelViewMatrix;
            varying float vLineDistance;

            attribute float lineDistance;

            void main() {
                vLineDistance = lineDistance * scale;
                gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
        """

        val fragmentShader = """
            uniform vec3 color;
            uniform float opacity;
            uniform float dashSize;
            uniform float gapSize;
            varying float vLineDistance;

            void main() {
                float dashDistance = dashSize + gapSize;
                float mod = mod(vLineDistance, dashDistance);
                if (mod > dashSize) discard;
                gl_FragColor = vec4(color, opacity);
            }
        """

        return Pair(vertexShader, fragmentShader)
    }
}
