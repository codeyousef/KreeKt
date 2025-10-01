package io.kreekt.lines

import io.kreekt.core.math.*
import io.kreekt.material.Material

/**
 * LineMaterial - Material for Line2 with pixel-accurate width
 * Supports world units and screen pixels, with dashing
 *
 * Based on Three.js LineMaterial
 */
class LineMaterial(
    var color: Color = Color(0xffffff),
    var linewidth: Float = 1f,
    var dashed: Boolean = false,
    var dashScale: Float = 1f,
    var dashSize: Float = 1f,
    var gapSize: Float = 1f,
    var worldUnits: Boolean = false,
    var alphaToCoverage: Boolean = true
) : Material() {

    override val type: String = "LineMaterial"

    val resolution = Vector2(1024f, 768f)

    override fun clone(): LineMaterial {
        return LineMaterial(
            color = color.clone(),
            linewidth = linewidth,
            dashed = dashed,
            dashScale = dashScale,
            dashSize = dashSize,
            gapSize = gapSize,
            worldUnits = worldUnits,
            alphaToCoverage = alphaToCoverage
        ).apply {
            resolution.copy(this@LineMaterial.resolution)
        }
    }

    /**
     * Generate shader code for line rendering
     */
    fun generateShader(): Pair<String, String> {
        val vertexShader = """
            attribute vec3 position;
            attribute vec3 previous;
            attribute vec3 next;
            attribute float side;

            uniform vec2 resolution;
            uniform float linewidth;
            uniform mat4 projectionMatrix;
            uniform mat4 modelViewMatrix;

            varying float vLineDistance;

            void main() {
                vec4 currentProjected = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
                vec4 previousProjected = projectionMatrix * modelViewMatrix * vec4(previous, 1.0);
                vec4 nextProjected = projectionMatrix * modelViewMatrix * vec4(next, 1.0);

                vec2 currentScreen = currentProjected.xy / currentProjected.w;
                vec2 previousScreen = previousProjected.xy / previousProjected.w;
                vec2 nextScreen = nextProjected.xy / nextProjected.w;

                vec2 dir = normalize(nextScreen - previousScreen);
                vec2 normal = vec2(-dir.y, dir.x);

                normal *= linewidth / resolution;
                normal.x *= resolution.x / resolution.y;

                vec4 offset = vec4(normal * side, 0.0, 0.0);
                gl_Position = currentProjected + offset;
            }
        """

        val fragmentShader = """
            uniform vec3 color;
            uniform float opacity;
            uniform bool dashed;
            uniform float dashSize;
            uniform float gapSize;

            varying float vLineDistance;

            void main() {
                if (dashed) {
                    float dashDistance = dashSize + gapSize;
                    float mod = mod(vLineDistance, dashDistance);
                    if (mod > dashSize) discard;
                }

                gl_FragColor = vec4(color, opacity);
            }
        """

        return Pair(vertexShader, fragmentShader)
    }
}

data class Color(val hex: Int) {
    val r: Float get() = ((hex shr 16) and 0xFF) / 255f
    val g: Float get() = ((hex shr 8) and 0xFF) / 255f
    val b: Float get() = (hex and 0xFF) / 255f

    fun clone(): Color = Color(hex)
}
