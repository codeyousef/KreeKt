package io.kreekt.lines

import io.kreekt.core.math.*
import io.kreekt.geometry.BufferAttribute
import io.kreekt.geometry.BufferGeometry

/**
 * LineGeometry - Geometry for Line2 rendering
 * Stores positions and generates previous/next vertex data for quad expansion
 *
 * Based on Three.js LineGeometry
 */
open class LineGeometry : BufferGeometry() {

    private var positions = floatArrayOf()
    private var colors = floatArrayOf()

    /**
     * Set positions from array of Vector3
     */
    fun setPositions(positions: FloatArray): LineGeometry {
        this.positions = positions
        updateAttributes()
        return this
    }

    /**
     * Set colors for each vertex
     */
    fun setColors(colors: FloatArray): LineGeometry {
        this.colors = colors
        updateColors()
        return this
    }

    /**
     * Compute line distances for dashing
     */
    fun computeLineDistances(): LineGeometry {
        val distances = mutableListOf<Float>()
        var totalDistance = 0f

        for (i in 0 until positions.size / 3 - 1) {
            val x1 = positions[i * 3]
            val y1 = positions[i * 3 + 1]
            val z1 = positions[i * 3 + 2]
            val x2 = positions[(i + 1) * 3]
            val y2 = positions[(i + 1) * 3 + 1]
            val z2 = positions[(i + 1) * 3 + 2]

            val dx = x2 - x1
            val dy = y2 - y1
            val dz = z2 - z1
            val distance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)

            distances.add(totalDistance)
            totalDistance += distance
        }
        distances.add(totalDistance)

        setAttribute("lineDistance", BufferAttribute(distances.toFloatArray(), 1))
        return this
    }

    private fun updateAttributes() {
        val numPoints = positions.size / 3
        val instanceStart = mutableListOf<Float>()
        val instanceEnd = mutableListOf<Float>()

        for (i in 0 until numPoints - 1) {
            instanceStart.add(positions[i * 3])
            instanceStart.add(positions[i * 3 + 1])
            instanceStart.add(positions[i * 3 + 2])

            instanceEnd.add(positions[(i + 1) * 3])
            instanceEnd.add(positions[(i + 1) * 3 + 1])
            instanceEnd.add(positions[(i + 1) * 3 + 2])
        }

        setAttribute("instanceStart", BufferAttribute(instanceStart.toFloatArray(), 3))
        setAttribute("instanceEnd", BufferAttribute(instanceEnd.toFloatArray(), 3))
    }

    private fun updateColors() {
        if (colors.isNotEmpty()) {
            setAttribute("instanceColorStart", BufferAttribute(colors, 3))
        }
    }
}

/**
 * LineSegmentsGeometry - Geometry for rendering disconnected line segments
 */
class LineSegmentsGeometry : LineGeometry()
