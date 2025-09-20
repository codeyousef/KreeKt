/**
 * Base class for primitive geometry generation with parameterized shapes
 * Provides standard primitive shapes with configurable parameters and efficient generation
 */
package io.kreekt.geometry

import io.kreekt.core.math.*
import kotlin.math.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Base class for all primitive geometries
 * Implements common functionality for parameterized shape generation
 */
abstract class PrimitiveGeometry : BufferGeometry() {

    /**
     * Parameters used to generate this primitive
     */
    abstract val parameters: PrimitiveParameters

    /**
     * Generate the geometry based on current parameters
     */
    abstract fun generate()

    /**
     * Update geometry if parameters have changed
     */
    fun updateIfNeeded() {
        if (parameters.hasChanged) {
            generate()
            parameters.markClean()
        }
    }

    /**
     * Get memory usage estimate for this primitive
     */
    fun getMemoryUsage(): Int {
        var usage = 0
        attributes.forEach { (_, attribute) ->
            usage = usage + attribute.array.size * when (attribute.itemSize) {
                1 -> 4  // Float32
                2 -> 8  // Vector2
                3 -> 12 // Vector3
                4 -> 16 // Vector4/Color
                else -> attribute.itemSize * 4
            }
        }
        index?.let { usage = usage + it.array.size * 4 }
        return usage
    }
}

/**
 * Base interface for primitive parameters
 */
abstract class PrimitiveParameters {
    protected var dirty = true

    val hasChanged: Boolean get() = dirty

    fun markDirty() {
        dirty = true
    }

    fun markClean() {
        dirty = false
    }
}

/**
 * Sphere geometry with configurable radius and subdivision
 */
class SphereGeometry(
    radius: Float = 1f,
    widthSegments: Int = 32,
    heightSegments: Int = 16,
    phiStart: Float = 0f,
    phiLength: Float = PI.toFloat() * 2f,
    thetaStart: Float = 0f,
    thetaLength: Float = PI.toFloat()
) : PrimitiveGeometry() {

    class SphereParameters(
        var radius: Float,
        var widthSegments: Int,
        var heightSegments: Int,
        var phiStart: Float,
        var phiLength: Float,
        var thetaStart: Float,
        var thetaLength: Float
    ) : PrimitiveParameters() {

        fun set(
            radius: Float = this.radius,
            widthSegments: Int = this.widthSegments,
            heightSegments: Int = this.heightSegments,
            phiStart: Float = this.phiStart,
            phiLength: Float = this.phiLength,
            thetaStart: Float = this.thetaStart,
            thetaLength: Float = this.thetaLength
        ) {
            if (this.radius != radius || this.widthSegments != widthSegments ||
                this.heightSegments != heightSegments || this.phiStart != phiStart ||
                this.phiLength != phiLength || this.thetaStart != thetaStart ||
                this.thetaLength != thetaLength) {

                this.radius = radius
                this.widthSegments = widthSegments
                this.heightSegments = heightSegments
                this.phiStart = phiStart
                this.phiLength = phiLength
                this.thetaStart = thetaStart
                this.thetaLength = thetaLength
                markDirty()
            }
        }
    }

    override val parameters = SphereParameters(
        radius, widthSegments, heightSegments, phiStart, phiLength, thetaStart, thetaLength
    )

    init {
        generate()
    }

    override fun generate() {
        val params = parameters

        // Validate parameters
        val widthSegs = max(3, params.widthSegments)
        val heightSegs = max(2, params.heightSegments)

        val thetaEnd = min(params.thetaStart + params.thetaLength, PI.toFloat())

        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        var index = 0
        val grid = mutableListOf<MutableList<Int>>()

        // Generate vertices, normals, and UVs
        for (iy in 0..heightSegs) {
            val verticesRow = mutableListOf<Int>()
            val v = iy.toFloat() / heightSegs.toFloat()

            // Special handling for poles
            var uOffset = 0f
            if (iy == 0 && params.thetaStart == 0f) {
                uOffset = 0.5f / widthSegs.toFloat()
            } else if (iy == heightSegs && thetaEnd == PI.toFloat()) {
                uOffset = -0.5f / widthSegs.toFloat()
            }

            for (ix in 0..widthSegs) {
                val u = ix.toFloat() / widthSegs.toFloat()

                // Vertex position
                val x = -params.radius * cos(params.phiStart + u * params.phiLength) *
                        sin(params.thetaStart + v * params.thetaLength)
                val y = params.radius * cos(params.thetaStart + v * params.thetaLength)
                val z = params.radius * sin(params.phiStart + u * params.phiLength) *
                        sin(params.thetaStart + v * params.thetaLength)

                vertices.addAll(listOf(x, y, z))

                // Normal (normalized position vector for sphere)
                val length = sqrt(x * x + y * y + (z * z))
                normals.addAll(listOf(x / length, y / length, z / length))

                // UV coordinates
                uvs.addAll(listOf(u + uOffset, 1f - v))

                verticesRow.add(index++)
            }

            grid.add(verticesRow)
        }

        // Generate indices
        for (iy in 0 until heightSegs) {
            for (ix in 0 until widthSegs) {
                val a = grid[iy][ix + 1]
                val b = grid[iy][ix]
                val c = grid[iy + 1][ix]
                val d = grid[iy + 1][ix + 1]

                if (iy != 0 || params.thetaStart > 0f) {
                    indices.addAll(listOf(a, b, d))
                }
                if (iy != heightSegs - 1 || thetaEnd < PI.toFloat()) {
                    indices.addAll(listOf(b, c, d))
                }
            }
        }

        // Set attributes
        setAttribute("position", BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", BufferAttribute(uvs.toFloatArray(), 2))
        setIndex(BufferAttribute(indices.map { it.toFloat() }.toFloatArray(), 1))

        computeBoundingSphere()
    }

    /**
     * Update sphere parameters
     */
    fun setParameters(
        radius: Float = parameters.radius,
        widthSegments: Int = parameters.widthSegments,
        heightSegments: Int = parameters.heightSegments,
        phiStart: Float = parameters.phiStart,
        phiLength: Float = parameters.phiLength,
        thetaStart: Float = parameters.thetaStart,
        thetaLength: Float = parameters.thetaLength
    ) {
        parameters.set(radius, widthSegments, heightSegments, phiStart, phiLength, thetaStart, thetaLength)
        updateIfNeeded()
    }
}

/**
 * Box geometry with configurable dimensions and subdivision
 */
class BoxGeometry(
    width: Float = 1f,
    height: Float = 1f,
    depth: Float = 1f,
    widthSegments: Int = 1,
    heightSegments: Int = 1,
    depthSegments: Int = 1
) : PrimitiveGeometry() {

    class BoxParameters(
        var width: Float,
        var height: Float,
        var depth: Float,
        var widthSegments: Int,
        var heightSegments: Int,
        var depthSegments: Int
    ) : PrimitiveParameters() {

        fun set(
            width: Float = this.width,
            height: Float = this.height,
            depth: Float = this.depth,
            widthSegments: Int = this.widthSegments,
            heightSegments: Int = this.heightSegments,
            depthSegments: Int = this.depthSegments
        ) {
            if (this.width != width || this.height != height || this.depth != depth ||
                this.widthSegments != widthSegments || this.heightSegments != heightSegments ||
                this.depthSegments != depthSegments) {

                this.width = width
                this.height = height
                this.depth = depth
                this.widthSegments = widthSegments
                this.heightSegments = heightSegments
                this.depthSegments = depthSegments
                markDirty()
            }
        }
    }

    override val parameters = BoxParameters(width, height, depth, widthSegments, heightSegments, depthSegments)

    init {
        generate()
    }

    override fun generate() {
        val params = parameters

        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        var numberOfVertices = 0

        fun buildPlane(
            u: String, v: String, w: String,
            udir: Float, vdir: Float,
            width: Float, height: Float, depth: Float,
            gridX: Int, gridY: Int
        ) {
            val segmentWidth = width / gridX
            val segmentHeight = height / gridY

            val widthHalf = width / 2
            val heightHalf = height / 2
            val depthHalf = depth / 2

            val gridX1 = gridX + 1
            val gridY1 = gridY + 1

            var vertexCounter = 0

            for (iy in 0 until gridY1) {
                val y = iy * segmentHeight - heightHalf

                for (ix in 0 until gridX1) {
                    val x = ix * segmentWidth - widthHalf

                    // Build vertex position
                    val vertex = mutableMapOf<String, Float>()
                    vertex[u] = x * udir
                    vertex[v] = y * vdir
                    vertex[w] = depthHalf

                    vertices.addAll(listOf(vertex["x"] ?: 0f, vertex["y"] ?: 0f, vertex["z"] ?: 0f))

                    // Build normal
                    val normal = mutableMapOf<String, Float>()
                    normal[u] = 0f
                    normal[v] = 0f
                    normal[w] = if (depth > 0) 1f else -1f

                    normals.addAll(listOf(normal["x"] ?: 0f, normal["y"] ?: 0f, normal["z"] ?: 0f))

                    // Build UV
                    uvs.addAll(listOf(ix.toFloat() / gridX, 1f - (iy.toFloat() / gridY)))

                    vertexCounter++
                }
            }

            // Build indices
            for (iy in 0 until gridY) {
                for (ix in 0 until gridX) {
                    val a = numberOfVertices + ix + gridX1 * iy
                    val b = numberOfVertices + ix + gridX1 * (iy + 1)
                    val c = numberOfVertices + (ix + 1) + gridX1 * (iy + 1)
                    val d = numberOfVertices + (ix + 1) + gridX1 * iy

                    indices.addAll(listOf(a, b, d))
                    indices.addAll(listOf(b, c, d))
                }
            }

            numberOfVertices = numberOfVertices + vertexCounter
        }

        // Build all six faces
        buildPlane("z", "y", "x", -1f, -1f, params.depth, params.height, params.width, params.depthSegments, params.heightSegments) // px
        buildPlane("z", "y", "x", 1f, -1f, params.depth, params.height, -params.width, params.depthSegments, params.heightSegments) // nx
        buildPlane("x", "z", "y", 1f, 1f, params.width, params.depth, params.height, params.widthSegments, params.depthSegments) // py
        buildPlane("x", "z", "y", 1f, -1f, params.width, params.depth, -params.height, params.widthSegments, params.depthSegments) // ny
        buildPlane("x", "y", "z", 1f, -1f, params.width, params.height, params.depth, params.widthSegments, params.heightSegments) // pz
        buildPlane("x", "y", "z", -1f, -1f, params.width, params.height, -params.depth, params.widthSegments, params.heightSegments) // nz

        // Set attributes
        setAttribute("position", BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", BufferAttribute(uvs.toFloatArray(), 2))
        setIndex(BufferAttribute(indices.map { it.toFloat() }.toFloatArray(), 1))

        computeBoundingSphere()
    }

    /**
     * Update box parameters
     */
    fun setParameters(
        width: Float = parameters.width,
        height: Float = parameters.height,
        depth: Float = parameters.depth,
        widthSegments: Int = parameters.widthSegments,
        heightSegments: Int = parameters.heightSegments,
        depthSegments: Int = parameters.depthSegments
    ) {
        parameters.set(width, height, depth, widthSegments, heightSegments, depthSegments)
        updateIfNeeded()
    }
}

/**
 * Cylinder geometry with configurable radii, height, and subdivision
 */
class CylinderGeometry(
    radiusTop: Float = 1f,
    radiusBottom: Float = 1f,
    height: Float = 1f,
    radialSegments: Int = 32,
    heightSegments: Int = 1,
    openEnded: Boolean = false,
    thetaStart: Float = 0f,
    thetaLength: Float = PI.toFloat() * 2f
) : PrimitiveGeometry() {

    class CylinderParameters(
        var radiusTop: Float,
        var radiusBottom: Float,
        var height: Float,
        var radialSegments: Int,
        var heightSegments: Int,
        var openEnded: Boolean,
        var thetaStart: Float,
        var thetaLength: Float
    ) : PrimitiveParameters() {

        fun set(
            radiusTop: Float = this.radiusTop,
            radiusBottom: Float = this.radiusBottom,
            height: Float = this.height,
            radialSegments: Int = this.radialSegments,
            heightSegments: Int = this.heightSegments,
            openEnded: Boolean = this.openEnded,
            thetaStart: Float = this.thetaStart,
            thetaLength: Float = this.thetaLength
        ) {
            if (this.radiusTop != radiusTop || this.radiusBottom != radiusBottom ||
                this.height != height || this.radialSegments != radialSegments ||
                this.heightSegments != heightSegments || this.openEnded != openEnded ||
                this.thetaStart != thetaStart || this.thetaLength != thetaLength) {

                this.radiusTop = radiusTop
                this.radiusBottom = radiusBottom
                this.height = height
                this.radialSegments = radialSegments
                this.heightSegments = heightSegments
                this.openEnded = openEnded
                this.thetaStart = thetaStart
                this.thetaLength = thetaLength
                markDirty()
            }
        }
    }

    override val parameters = CylinderParameters(
        radiusTop, radiusBottom, height, radialSegments, heightSegments,
        openEnded, thetaStart, thetaLength
    )

    init {
        generate()
    }

    override fun generate() {
        val params = parameters

        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        var index = 0
        val indexArray = mutableListOf<MutableList<Int>>()
        val halfHeight = params.height / 2f

        fun generateTorso() {
            val normal = Vector3()
            val vertex = Vector3()

            // Generate vertices, normals and uvs
            for (y in 0..params.heightSegments) {
                val indexRow = mutableListOf<Int>()
                val v = y.toFloat() / params.heightSegments.toFloat()

                // Calculate radius of current row
                val radius = v * (params.radiusBottom - params.radiusTop) + params.radiusTop

                for (x in 0..params.radialSegments) {
                    val u = x.toFloat() / params.radialSegments.toFloat()
                    val theta = u * params.thetaLength + params.thetaStart

                    val sinTheta = sin(theta)
                    val cosTheta = cos(theta)

                    // Vertex
                    vertex.x = radius * sinTheta
                    vertex.y = -v * params.height + halfHeight
                    vertex.z = radius * cosTheta
                    vertices.addAll(listOf(vertex.x, vertex.y, vertex.z))

                    // Normal
                    normal.set(sinTheta, (params.radiusBottom - params.radiusTop) / params.height, cosTheta)
                    normal.normalize()
                    normals.addAll(listOf(normal.x, normal.y, normal.z))

                    // UV
                    uvs.addAll(listOf(u, 1f - v))

                    indexRow.add(index++)
                }

                indexArray.add(indexRow)
            }

            // Generate indices
            for (x in 0 until params.radialSegments) {
                for (y in 0 until params.heightSegments) {
                    val a = indexArray[y][x]
                    val b = indexArray[y + 1][x]
                    val c = indexArray[y + 1][x + 1]
                    val d = indexArray[y][x + 1]

                    indices.addAll(listOf(a, b, d))
                    indices.addAll(listOf(b, c, d))
                }
            }
        }

        fun generateCap(top: Boolean) {
            val centerIndexStart = index
            val radius = if (top) params.radiusTop else params.radiusBottom
            val sign = if (top) 1f else -1f

            // Generate center vertex
            vertices.addAll(listOf(0f, (halfHeight * sign), 0f))
            normals.addAll(listOf(0f, sign, 0f))
            uvs.addAll(listOf(0.5f, 0.5f))
            index++

            // Generate surrounding vertices
            for (x in 0..params.radialSegments) {
                val u = x.toFloat() / params.radialSegments.toFloat()
                val theta = u * params.thetaLength + params.thetaStart

                val cosTheta = cos(theta)
                val sinTheta = sin(theta)

                // Vertex
                vertices.addAll(listOf((radius * sinTheta), (halfHeight * sign), (radius * cosTheta)))

                // Normal
                normals.addAll(listOf(0f, sign, 0f))

                // UV
                uvs.addAll(listOf((cosTheta * 0.5f) + 0.5f, (sinTheta * 0.5f * sign) + 0.5f))

                index++
            }

            // Generate indices
            for (x in 0 until params.radialSegments) {
                val c = centerIndexStart
                val i = centerIndexStart + 1 + x

                if (top) {
                    indices.addAll(listOf(i, i + 1, c))
                } else {
                    indices.addAll(listOf(i + 1, i, c))
                }
            }
        }

        generateTorso()

        if (!params.openEnded) {
            if (params.radiusTop > 0f) generateCap(true)
            if (params.radiusBottom > 0f) generateCap(false)
        }

        // Set attributes
        setAttribute("position", BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", BufferAttribute(uvs.toFloatArray(), 2))
        setIndex(BufferAttribute(indices.map { it.toFloat() }.toFloatArray(), 1))

        computeBoundingSphere()
    }

    /**
     * Update cylinder parameters
     */
    fun setParameters(
        radiusTop: Float = parameters.radiusTop,
        radiusBottom: Float = parameters.radiusBottom,
        height: Float = parameters.height,
        radialSegments: Int = parameters.radialSegments,
        heightSegments: Int = parameters.heightSegments,
        openEnded: Boolean = parameters.openEnded,
        thetaStart: Float = parameters.thetaStart,
        thetaLength: Float = parameters.thetaLength
    ) {
        parameters.set(radiusTop, radiusBottom, height, radialSegments, heightSegments, openEnded, thetaStart, thetaLength)
        updateIfNeeded()
    }
}

/**
 * Plane geometry with configurable dimensions and subdivision
 */
class PlaneGeometry(
    width: Float = 1f,
    height: Float = 1f,
    widthSegments: Int = 1,
    heightSegments: Int = 1
) : PrimitiveGeometry() {

    class PlaneParameters(
        var width: Float,
        var height: Float,
        var widthSegments: Int,
        var heightSegments: Int
    ) : PrimitiveParameters() {

        fun set(
            width: Float = this.width,
            height: Float = this.height,
            widthSegments: Int = this.widthSegments,
            heightSegments: Int = this.heightSegments
        ) {
            if (this.width != width || this.height != height ||
                this.widthSegments != widthSegments || this.heightSegments != heightSegments) {

                this.width = width
                this.height = height
                this.widthSegments = widthSegments
                this.heightSegments = heightSegments
                markDirty()
            }
        }
    }

    override val parameters = PlaneParameters(width, height, widthSegments, heightSegments)

    init {
        generate()
    }

    override fun generate() {
        val params = parameters

        val widthHalf = params.width / 2f
        val heightHalf = params.height / 2f

        val gridX = params.widthSegments
        val gridY = params.heightSegments

        val gridX1 = gridX + 1
        val gridY1 = gridY + 1

        val segmentWidth = params.width / gridX
        val segmentHeight = params.height / gridY

        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()

        for (iy in 0 until gridY1) {
            val y = iy * segmentHeight - heightHalf

            for (ix in 0 until gridX1) {
                val x = ix * segmentWidth - widthHalf

                vertices.addAll(listOf(x, -y, 0f))
                normals.addAll(listOf(0f, 0f, 1f))
                uvs.addAll(listOf(ix.toFloat() / gridX, 1f - (iy.toFloat() / gridY)))
            }
        }

        val indices = mutableListOf<Int>()

        for (iy in 0 until gridY) {
            for (ix in 0 until gridX) {
                val a = ix + gridX1 * iy
                val b = ix + gridX1 * (iy + 1)
                val c = (ix + 1) + gridX1 * (iy + 1)
                val d = (ix + 1) + gridX1 * iy

                indices.addAll(listOf(a, b, d))
                indices.addAll(listOf(b, c, d))
            }
        }

        // Set attributes
        setAttribute("position", BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", BufferAttribute(uvs.toFloatArray(), 2))
        setIndex(BufferAttribute(indices.map { it.toFloat() }.toFloatArray(), 1))

        computeBoundingSphere()
    }

    /**
     * Update plane parameters
     */
    fun setParameters(
        width: Float = parameters.width,
        height: Float = parameters.height,
        widthSegments: Int = parameters.widthSegments,
        heightSegments: Int = parameters.heightSegments
    ) {
        parameters.set(width, height, widthSegments, heightSegments)
        updateIfNeeded()
    }
}

/**
 * Ring geometry with configurable inner/outer radius and subdivision
 */
class RingGeometry(
    innerRadius: Float = 0.5f,
    outerRadius: Float = 1f,
    thetaSegments: Int = 32,
    phiSegments: Int = 1,
    thetaStart: Float = 0f,
    thetaLength: Float = PI.toFloat() * 2f
) : PrimitiveGeometry() {

    class RingParameters(
        var innerRadius: Float,
        var outerRadius: Float,
        var thetaSegments: Int,
        var phiSegments: Int,
        var thetaStart: Float,
        var thetaLength: Float
    ) : PrimitiveParameters() {

        fun set(
            innerRadius: Float = this.innerRadius,
            outerRadius: Float = this.outerRadius,
            thetaSegments: Int = this.thetaSegments,
            phiSegments: Int = this.phiSegments,
            thetaStart: Float = this.thetaStart,
            thetaLength: Float = this.thetaLength
        ) {
            if (this.innerRadius != innerRadius || this.outerRadius != outerRadius ||
                this.thetaSegments != thetaSegments || this.phiSegments != phiSegments ||
                this.thetaStart != thetaStart || this.thetaLength != thetaLength) {

                this.innerRadius = innerRadius
                this.outerRadius = outerRadius
                this.thetaSegments = thetaSegments
                this.phiSegments = phiSegments
                this.thetaStart = thetaStart
                this.thetaLength = thetaLength
                markDirty()
            }
        }
    }

    override val parameters = RingParameters(innerRadius, outerRadius, thetaSegments, phiSegments, thetaStart, thetaLength)

    init {
        generate()
    }

    override fun generate() {
        val params = parameters

        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        var index = 0

        // Generate vertices, normals and uvs
        for (j in 0..params.phiSegments) {
            for (i in 0..params.thetaSegments) {
                val segment = params.thetaStart + (i.toFloat() / params.thetaSegments.toFloat()) * params.thetaLength

                // Calculate radius for this row
                val radius = params.innerRadius + (j.toFloat() / params.phiSegments.toFloat()) * (params.outerRadius - params.innerRadius)

                // Vertex
                val x = radius * cos(segment)
                val y = radius * sin(segment)

                vertices.addAll(listOf(x, y, 0f))

                // Normal
                normals.addAll(listOf(0f, 0f, 1f))

                // UV
                val u = (x / params.outerRadius + 1f) / 2f
                val v = (y / params.outerRadius + 1f) / 2f
                uvs.addAll(listOf(u, v))

                index++
            }
        }

        // Generate indices
        for (j in 0 until params.phiSegments) {
            val thetaSegmentLevel = j * (params.thetaSegments + 1)

            for (i in 0 until params.thetaSegments) {
                val segment = i + thetaSegmentLevel

                val a = segment
                val b = segment + params.thetaSegments + 1
                val c = segment + params.thetaSegments + 1 + 1
                val d = segment + 1

                indices.addAll(listOf(a, b, d))
                indices.addAll(listOf(b, c, d))
            }
        }

        // Set attributes
        setAttribute("position", BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", BufferAttribute(uvs.toFloatArray(), 2))
        setIndex(BufferAttribute(indices.map { it.toFloat() }.toFloatArray(), 1))

        computeBoundingSphere()
    }

    /**
     * Update ring parameters
     */
    fun setParameters(
        innerRadius: Float = parameters.innerRadius,
        outerRadius: Float = parameters.outerRadius,
        thetaSegments: Int = parameters.thetaSegments,
        phiSegments: Int = parameters.phiSegments,
        thetaStart: Float = parameters.thetaStart,
        thetaLength: Float = parameters.thetaLength
    ) {
        parameters.set(innerRadius, outerRadius, thetaSegments, phiSegments, thetaStart, thetaLength)
        updateIfNeeded()
    }
}

/**
 * Torus geometry with configurable major/minor radius and subdivision
 */
class TorusGeometry(
    radius: Float = 1f,
    tube: Float = 0.4f,
    radialSegments: Int = 12,
    tubularSegments: Int = 48,
    arc: Float = PI.toFloat() * 2f
) : PrimitiveGeometry() {

    class TorusParameters(
        var radius: Float,
        var tube: Float,
        var radialSegments: Int,
        var tubularSegments: Int,
        var arc: Float
    ) : PrimitiveParameters() {

        fun set(
            radius: Float = this.radius,
            tube: Float = this.tube,
            radialSegments: Int = this.radialSegments,
            tubularSegments: Int = this.tubularSegments,
            arc: Float = this.arc
        ) {
            if (this.radius != radius || this.tube != tube ||
                this.radialSegments != radialSegments || this.tubularSegments != tubularSegments ||
                this.arc != arc) {

                this.radius = radius
                this.tube = tube
                this.radialSegments = radialSegments
                this.tubularSegments = tubularSegments
                this.arc = arc
                markDirty()
            }
        }
    }

    override val parameters = TorusParameters(radius, tube, radialSegments, tubularSegments, arc)

    init {
        generate()
    }

    override fun generate() {
        val params = parameters

        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        val center = Vector3()
        val vertex = Vector3()
        val normal = Vector3()

        // Generate vertices, normals and uvs
        for (j in 0..params.radialSegments) {
            for (i in 0..params.tubularSegments) {
                val u = i.toFloat() / params.tubularSegments.toFloat() * params.arc
                val v = j.toFloat() / params.radialSegments.toFloat() * PI.toFloat() * 2f

                // Vertex
                vertex.x = (params.radius + params.tube * cos(v)) * cos(u)
                vertex.y = (params.radius + params.tube * cos(v)) * sin(u)
                vertex.z = params.tube * sin(v)

                vertices.addAll(listOf(vertex.x, vertex.y, vertex.z))

                // Normal
                center.x = params.radius * cos(u)
                center.y = params.radius * sin(u)
                normal.copy(vertex).subtract(center).normalize()

                normals.addAll(listOf(normal.x, normal.y, normal.z))

                // UV
                uvs.addAll(listOf(i.toFloat() / params.tubularSegments, j.toFloat() / params.radialSegments))
            }
        }

        // Generate indices
        for (j in 1..params.radialSegments) {
            for (i in 1..params.tubularSegments) {
                val a = (params.tubularSegments + 1) * j + i - 1
                val b = (params.tubularSegments + 1) * (j - 1) + i - 1
                val c = (params.tubularSegments + 1) * (j - 1) + i
                val d = (params.tubularSegments + 1) * j + i

                indices.addAll(listOf(a, b, d))
                indices.addAll(listOf(b, c, d))
            }
        }

        // Set attributes
        setAttribute("position", BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", BufferAttribute(uvs.toFloatArray(), 2))
        setIndex(BufferAttribute(indices.map { it.toFloat() }.toFloatArray(), 1))

        computeBoundingSphere()
    }

    /**
     * Update torus parameters
     */
    fun setParameters(
        radius: Float = parameters.radius,
        tube: Float = parameters.tube,
        radialSegments: Int = parameters.radialSegments,
        tubularSegments: Int = parameters.tubularSegments,
        arc: Float = parameters.arc
    ) {
        parameters.set(radius, tube, radialSegments, tubularSegments, arc)
        updateIfNeeded()
    }
}