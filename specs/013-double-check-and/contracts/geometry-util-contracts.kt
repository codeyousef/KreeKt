/**
 * Geometry Utilities Contracts
 *
 * Feature: 013-double-check-and (Three.js r180 Feature Parity)
 * Category: Geometry Processing and Utilities
 *
 * Requirements covered: FR-036 through FR-042
 */

package io.kreekt.geometry.utils

import io.kreekt.core.math.Vector3
import io.kreekt.core.math.Box3
import io.kreekt.geometry.BufferGeometry

/**
 * Geometry processing utilities.
 */
object GeometryProcessor {
    /**
     * Generates a convex hull from a set of points.
     *
     * FR-036: System MUST provide ConvexHull utility for generating convex hull geometries
     *
     * @sample
     * ```kotlin
     * val points = listOf(
     *     Vector3(0f, 0f, 0f),
     *     Vector3(1f, 0f, 0f),
     *     Vector3(0f, 1f, 0f),
     *     Vector3(0f, 0f, 1f)
     * )
     * val hull = GeometryProcessor.computeConvexHull(points)
     * ```
     */
    fun computeConvexHull(points: List<Vector3>): BufferGeometry

    /**
     * Simplifies geometry using quadric error metrics.
     *
     * FR-037: System MUST provide geometry simplification utilities for LOD generation
     *
     * @sample
     * ```kotlin
     * val simplified = GeometryProcessor.simplify(
     *     geometry = originalGeometry,
     *     targetRatio = 0.5f, // Reduce to 50% triangles
     *     options = SimplificationOptions(preserveUVs = true)
     * )
     * ```
     */
    fun simplify(
        geometry: BufferGeometry,
        targetRatio: Float,
        options: SimplificationOptions = SimplificationOptions()
    ): BufferGeometry

    /**
     * Subdivides geometry for smoothing.
     *
     * FR-038: System MUST provide tessellation utilities for mesh refinement
     */
    fun subdivide(
        geometry: BufferGeometry,
        iterations: Int = 1,
        method: SubdivisionMethod = SubdivisionMethod.Loop
    ): BufferGeometry

    /**
     * Computes tangents using MikkTSpace algorithm.
     *
     * FR-040: System MUST provide tangent computation utilities (MikkTSpace) for normal mapping
     */
    fun computeTangents(
        geometry: BufferGeometry,
        uvChannel: Int = 0
    )

    /**
     * Merges multiple geometries into one.
     *
     * FR-039: System MUST provide geometry merging utilities for combining multiple geometries
     */
    fun mergeGeometries(
        geometries: List<BufferGeometry>,
        useGroups: Boolean = false
    ): BufferGeometry

    /**
     * Merges duplicate vertices.
     */
    fun mergeVertices(
        geometry: BufferGeometry,
        tolerance: Float = 1e-4f
    ): BufferGeometry

    /**
     * Converts to indexed geometry.
     *
     * FR-041: System MUST provide utilities to convert between indexed and non-indexed geometries
     */
    fun toIndexed(geometry: BufferGeometry): BufferGeometry

    /**
     * Converts to non-indexed geometry.
     *
     * FR-041: System MUST provide utilities to convert between indexed and non-indexed geometries
     */
    fun toNonIndexed(geometry: BufferGeometry): BufferGeometry

    /**
     * Computes bounding box.
     */
    fun computeBounds(geometry: BufferGeometry): Box3

    /**
     * Computes bounding sphere.
     */
    fun computeBoundingSphere(geometry: BufferGeometry): io.kreekt.core.math.Sphere

    /**
     * Estimates memory usage.
     *
     * FR-042: System MUST provide utilities to estimate geometry memory usage
     */
    fun estimateMemoryUsage(geometry: BufferGeometry): GeometryMemoryInfo
}

/**
 * Simplification options.
 */
data class SimplificationOptions(
    val preserveBoundary: Boolean = true,
    val preserveUVs: Boolean = true,
    val preserveNormals: Boolean = true,
    val aggressiveness: Float = 7.0f
)

/**
 * Subdivision methods.
 */
enum class SubdivisionMethod {
    Loop,           // For triangle meshes
    CatmullClark    // For quad meshes
}

/**
 * Geometry memory information.
 */
data class GeometryMemoryInfo(
    val vertexCount: Int,
    val triangleCount: Int,
    val attributeBytes: Long,
    val indexBytes: Long,
    val totalBytes: Long
) {
    val totalMB: Float
        get() = totalBytes / (1024f * 1024f)
}
