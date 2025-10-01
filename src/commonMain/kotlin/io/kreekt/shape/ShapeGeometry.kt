package io.kreekt.shape

import io.kreekt.core.math.*
import io.kreekt.geometry.BufferAttribute
import io.kreekt.geometry.BufferGeometry

/**
 * ShapeGeometry - Creates 2D planar geometry from Shape objects
 * Triangulates shapes with holes and generates proper UVs
 *
 * Based on Three.js ShapeGeometry
 */
class ShapeGeometry(
    shapes: List<Shape>,
    curveSegments: Int = 12
) : BufferGeometry() {

    constructor(shape: Shape, curveSegments: Int = 12) : this(listOf(shape), curveSegments)

    init {
        require(shapes.isNotEmpty()) { "At least one shape must be provided" }
        require(curveSegments > 0) { "Curve segments must be positive" }

        generate(shapes, curveSegments)
    }

    private fun generate(shapes: List<Shape>, curveSegments: Int) {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        var vertexOffset = 0

        for (shape in shapes) {
            val shapePoints = shape.getPoints(curveSegments)
            val holes = shape.holes.map { it.getPoints(curveSegments) }

            // Triangulate the shape
            val faces = ShapeUtils.triangulateShape(shapePoints, holes)

            // Calculate bounding box for UV generation
            val boundingBox = calculateBoundingBox(shapePoints)

            // Add vertices for shape
            for (point in shapePoints) {
                vertices.add(point.x)
                vertices.add(point.y)
                vertices.add(0f) // Z = 0 for planar shape

                // Normal pointing up for planar shape
                normals.add(0f)
                normals.add(0f)
                normals.add(1f)

                // Generate UVs based on bounding box
                val u = (point.x - boundingBox.min.x) / (boundingBox.max.x - boundingBox.min.x)
                val v = (point.y - boundingBox.min.y) / (boundingBox.max.y - boundingBox.min.y)
                uvs.add(u)
                uvs.add(v)
            }

            // Add hole vertices
            for (hole in holes) {
                for (point in hole) {
                    vertices.add(point.x)
                    vertices.add(point.y)
                    vertices.add(0f)

                    normals.add(0f)
                    normals.add(0f)
                    normals.add(1f)

                    val u = (point.x - boundingBox.min.x) / (boundingBox.max.x - boundingBox.min.x)
                    val v = (point.y - boundingBox.min.y) / (boundingBox.max.y - boundingBox.min.y)
                    uvs.add(u)
                    uvs.add(v)
                }
            }

            // Add indices
            for (face in faces) {
                indices.add(vertexOffset + face[0])
                indices.add(vertexOffset + face[1])
                indices.add(vertexOffset + face[2])
            }

            vertexOffset = vertices.size / 3
        }

        // Set geometry attributes
        setAttribute("position", BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", BufferAttribute(uvs.toFloatArray(), 2))
        setIndex(BufferAttribute(indices.map { it.toFloat() }.toFloatArray(), 1))

        computeBoundingSphere()
    }

    private fun calculateBoundingBox(points: List<Vector2>): BoundingBox2D {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        for (point in points) {
            minX = minOf(minX, point.x)
            minY = minOf(minY, point.y)
            maxX = maxOf(maxX, point.x)
            maxY = maxOf(maxY, point.y)
        }

        return BoundingBox2D(Vector2(minX, minY), Vector2(maxX, maxY))
    }

    private data class BoundingBox2D(val min: Vector2, val max: Vector2)
}