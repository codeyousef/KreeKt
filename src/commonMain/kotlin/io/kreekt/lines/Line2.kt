package io.kreekt.lines

import io.kreekt.core.math.*
import io.kreekt.core.scene.Object3D

/**
 * Line2 - High-quality line rendering using screen-space quads
 * Resolution-independent lines with pixel-accurate width
 *
 * Based on Three.js Line2 (THREE.Line2)
 */
open class Line2(
    geometry: LineGeometry,
    material: LineMaterial
) : Object3D() {

    var geometry: LineGeometry = geometry
        set(value) {
            field = value
            needsUpdate = true
        }

    var material: LineMaterial = material
        set(value) {
            field = value
            needsUpdate = true
        }

    private var needsUpdate = false

    /**
     * Compute line distances for dashing
     */
    fun computeLineDistances(): Line2 {
        geometry.computeLineDistances()
        return this
    }

    /**
     * Update resolution for screen-space rendering
     */
    fun setResolution(width: Float, height: Float) {
        material.resolution.set(width, height)
    }

    /**
     * Raycast for Line2
     * Uses threshold-based intersection testing
     */
    fun raycast(raycaster: Any, intersects: MutableList<Any>) {
        // TODO: Implement raycasting for Line2
    }
}

/**
 * LineSegments2 - Variant for rendering line segments
 */
class LineSegments2(
    geometry: LineSegmentsGeometry,
    material: LineMaterial
) : Line2(geometry, material)
