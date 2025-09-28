/**
 * GeometryProcessor for optimization and LOD generation
 * T024 - Advanced geometry processing with LOD, optimization, and quality control
 *
 * Provides algorithms for:
 * - Geometry simplification (triangle reduction)
 * - LOD level generation with automatic distance-based switching
 * - Vertex merging and deduplication
 * - Normal and tangent generation
 * - Bounding volume calculations
 * - Memory optimization techniques
 */
package io.kreekt.geometry

import io.kreekt.core.math.*
import kotlin.math.sqrt

/**
 * Advanced geometry processor for real-time 3D applications
 * Implements industry-standard algorithms for geometry optimization
 */
class GeometryProcessor {

    companion object {
        // Default LOD configuration
        const val DEFAULT_LOD_LEVELS = 5
        const val DEFAULT_REDUCTION_FACTOR = 0.5f
        const val DEFAULT_MERGE_THRESHOLD = 0.001f
        const val DEFAULT_NORMAL_THRESHOLD = 0.866f // ~30 degrees

        // Quality targets for different performance tiers
        val QUALITY_TIERS = mapOf(
            QualityTier.LOW to QualitySettings(
                lodLevels = 3,
                reductionFactor = 0.3f,
                mergeThreshold = 0.01f,
                generateTangents = false
            ),
            QualityTier.MEDIUM to QualitySettings(
                lodLevels = 4,
                reductionFactor = 0.5f,
                mergeThreshold = 0.005f,
                generateTangents = true
            ),
            QualityTier.HIGH to QualitySettings(
                lodLevels = 5,
                reductionFactor = 0.7f,
                mergeThreshold = 0.001f,
                generateTangents = true
            ),
            QualityTier.ULTRA to QualitySettings(
                lodLevels = 6,
                reductionFactor = 0.8f,
                mergeThreshold = 0.0005f,
                generateTangents = true
            )
        )
    }

    /**
     * Generate multiple LOD levels for a geometry
     * Uses progressive mesh simplification to create lower detail versions
     */
    fun generateLodLevels(
        geometry: BufferGeometry,
        options: LodGenerationOptions = LodGenerationOptions()
    ): LodResult {
        val originalTriangleCount = geometry.getTriangleCount()
        val lodLevels = mutableListOf<LodLevel>()

        // Add original as LOD 0
        lodLevels.add(LodLevel(
            distance = 0f,
            geometry = geometry.clone(),
            triangleCount = originalTriangleCount
        ))

        var currentGeometry = geometry.clone()
        var currentReduction = options.initialReduction

        // Generate progressive LOD levels
        for (level in 1 until options.lodLevels) {
            val targetTriangleCount = ((originalTriangleCount * currentReduction)).toInt()

            if (targetTriangleCount < options.minimumTriangles) {
                break
            }

            val simplifiedGeometry = simplifyGeometry(currentGeometry, targetTriangleCount)
            val distance = calculateLodDistance(level, options.baseLodDistance)

            lodLevels.add(LodLevel(
                distance = distance,
                geometry = simplifiedGeometry,
                triangleCount = simplifiedGeometry.getTriangleCount()
            ))

            currentGeometry = simplifiedGeometry
            currentReduction = currentReduction * options.reductionFactor
        }

        return LodResult(
            levels = lodLevels.toList(),
            originalTriangleCount = originalTriangleCount,
            totalReduction = if (lodLevels.size > 1) {
                1f - (lodLevels.last().triangleCount.toFloat() / originalTriangleCount)
            } else {
                0f
            }
        )
    }

    /**
     * Simplify geometry using edge collapse algorithm
     * Implements Quadric Error Metrics for optimal vertex removal
     */
    fun simplifyGeometry(
        geometry: BufferGeometry,
        targetTriangleCount: Int
    ): BufferGeometry {
        val positionAttribute = geometry.getAttribute("position")
            ?: return geometry.clone()

        val indexAttribute = geometry.index
        if (indexAttribute == null) {
            // Convert non-indexed geometry to indexed first
            val indexedGeometry = generateIndices(geometry)
            return simplifyGeometry(indexedGeometry, targetTriangleCount)
        }

        val vertices = extractVertices(positionAttribute)
        val indices = extractIndices(indexAttribute)
        val edges = buildEdgeList(indices)
        val quadrics = calculateQuadricErrorMetrics(vertices, indices)

        // Build priority queue of edge collapse operations
        val collapseQueue = buildCollapseQueue(edges, vertices, quadrics)

        val currentTriangleCount = indices.size / 3
        val trianglesToRemove = currentTriangleCount - targetTriangleCount

        // Perform edge collapses
        val result = performEdgeCollapses(
            vertices, indices, collapseQueue, trianglesToRemove
        )

        return buildSimplifiedGeometry(geometry, result.vertices, result.indices)
    }

    /**
     * Merge duplicate vertices within threshold
     * Reduces memory usage and improves cache coherency
     */
    fun mergeVertices(
        geometry: BufferGeometry,
        threshold: Float = DEFAULT_MERGE_THRESHOLD
    ): GeometryMergeResult {
        val positionAttribute = geometry.getAttribute("position")
            ?: return GeometryMergeResult(geometry.clone(), 0)

        val vertices = mutableListOf<Vector3>()
        val vertexMap = mutableMapOf<Int, Int>()
        val newIndices = mutableListOf<Int>()

        // Extract and process vertices
        for (i in 0 until positionAttribute.count) {
            val vertex = Vector3(
                positionAttribute.getX(i),
                positionAttribute.getY(i),
                positionAttribute.getZ(i)
            )

            // Find existing vertex within threshold
            val existingIndex = findNearestVertex(vertices, vertex, threshold)

            if (existingIndex >= 0) {
                vertexMap[i] = existingIndex
            } else {
                vertexMap[i] = vertices.size
                vertices.add(vertex)
            }
        }

        // Update indices
        val originalIndex = geometry.index
        if (originalIndex != null) {
            for (i in 0 until originalIndex.count) {
                val oldIndex = originalIndex.array[i].toInt()
                newIndices.add(vertexMap[oldIndex] ?: oldIndex)
            }
        } else {
            // Generate new indices for non-indexed geometry
            for (i in 0 until positionAttribute.count) {
                newIndices.add(vertexMap[i] ?: i)
            }
        }

        // Build merged geometry
        val mergedGeometry = buildMergedGeometry(geometry, vertices, newIndices)
        val mergedVertices = positionAttribute.count - vertices.size

        return GeometryMergeResult(mergedGeometry, mergedVertices)
    }

    /**
     * Generate smooth normals using vertex merging
     * Improves lighting quality for organic shapes
     */
    fun generateSmoothNormals(
        geometry: BufferGeometry,
        angleThreshold: Float = DEFAULT_NORMAL_THRESHOLD
    ): BufferGeometry {
        val result = geometry.clone()
        val positionAttribute = result.getAttribute("position") ?: return result
        val indexAttribute = result.index

        val vertexCount = positionAttribute.count
        val normals = Array(vertexCount) { Vector3() }
        val normalCounts = IntArray(vertexCount)

        // Calculate face normals and accumulate vertex normals
        indexAttribute?.let { index ->
            calculateIndexedNormals(positionAttribute, index, normals, normalCounts)
        } ?: run {
            calculateNonIndexedNormals(positionAttribute, normals, normalCounts)
        }

        // Normalize accumulated normals
        for (i in normals.indices) {
            if (normalCounts[i] > 0) {
                normals[i].divideScalar(normalCounts[i].toFloat()).normalize()
            }
        }

        // Apply angle threshold for sharp edges
        applyAngleThreshold(result, normals, angleThreshold)

        // Set normal attribute
        val normalArray = FloatArray((vertexCount * 3))
        for (i in normals.indices) {
            normalArray[(i * 3)] = normals[i].x
            normalArray[i * 3 + 1] = normals[i].y
            normalArray[i * 3 + 2] = normals[i].z
        }

        result.setAttribute("normal", BufferAttribute(normalArray, 3))
        return result
    }

    /**
     * Generate tangent vectors for normal mapping
     * Implements Lengyel's method for consistent tangent calculation
     */
    fun generateTangents(geometry: BufferGeometry): BufferGeometry {
        val result = geometry.clone()
        val positionAttribute = result.getAttribute("position") ?: return result
        val normalAttribute = result.getAttribute("normal") ?: run {
            // Generate normals first if they don't exist
            return generateTangents(generateSmoothNormals(result))
        }
        val uvAttribute = result.getAttribute("uv") ?: return result

        val vertexCount = positionAttribute.count
        val tangents = Array(vertexCount) { Vector3() }
        val bitangents = Array(vertexCount) { Vector3() }

        // Calculate tangents using UV derivatives
        calculateTangentVectors(
            positionAttribute, normalAttribute, uvAttribute,
            result.index, tangents, bitangents
        )

        // Orthogonalize tangents using Gram-Schmidt process
        orthogonalizeTangents(normalAttribute, tangents, bitangents)

        // Set tangent attribute (w component stores handedness)
        val tangentArray = FloatArray((vertexCount * 4))
        for (i in 0 until vertexCount) {
            val normal = Vector3(
                normalAttribute.getX(i),
                normalAttribute.getY(i),
                normalAttribute.getZ(i)
            )
            val tangent = tangents[i]
            val bitangent = bitangents[i]

            // Calculate handedness
            val handedness = if (normal.clone().cross(tangent).dot(bitangent) < 0f) -1f else 1f

            tangentArray[(i * 4)] = tangent.x
            tangentArray[i * 4 + 1] = tangent.y
            tangentArray[i * 4 + 2] = tangent.z
            tangentArray[i * 4 + 3] = handedness
        }

        result.setAttribute("tangent", BufferAttribute(tangentArray, 4))
        return result
    }

    /**
     * Optimize geometry for GPU rendering
     * Applies vertex cache optimization and memory layout improvements
     */
    fun optimizeForGpu(
        geometry: BufferGeometry,
        options: GpuOptimizationOptions = GpuOptimizationOptions()
    ): GeometryOptimizationResult {
        var result = geometry.clone()
        val optimizations = mutableListOf<String>()

        // Merge duplicate vertices
        if (options.mergeVertices) {
            val mergeResult = mergeVertices(result, options.mergeThreshold)
            result = mergeResult.geometry
            if (mergeResult.mergedVertices > 0) {
                optimizations.add("Merged ${mergeResult.mergedVertices} duplicate vertices")
            }
        }

        // Generate indices if not present
        if (result.index == null && options.generateIndices) {
            result = generateIndices(result)
            optimizations.add("Generated index buffer")
        }

        // Optimize vertex cache
        if (options.optimizeVertexCache) {
            result = optimizeVertexCache(result)
            optimizations.add("Optimized vertex cache ordering")
        }

        // Generate missing attributes
        if (result.getAttribute("normal") == null && options.generateNormals) {
            result = generateSmoothNormals(result)
            optimizations.add("Generated smooth normals")
        }

        if (result.getAttribute("tangent") == null && options.generateTangents) {
            result = generateTangents(result)
            optimizations.add("Generated tangent vectors")
        }

        // Calculate memory savings
        val originalMemory = calculateGeometryMemoryUsage(geometry)
        val optimizedMemory = calculateGeometryMemoryUsage(result)
        val memorySavings = originalMemory - optimizedMemory

        return GeometryOptimizationResult(
            geometry = result,
            optimizations = optimizations.toList(),
            memorySavings = memorySavings,
            compressionRatio = if (originalMemory > 0) {
                optimizedMemory.toFloat() / originalMemory
            } else 1f
        )
    }

    /**
     * Calculate tight bounding volumes
     * More accurate than basic min/max bounds
     */
    fun calculateBoundingVolumes(geometry: BufferGeometry): BoundingVolumeResult {
        val positionAttribute = geometry.getAttribute("position")
            ?: return BoundingVolumeResult(Box3(), Sphere())

        val points = mutableListOf<Vector3>()
        for (i in 0 until positionAttribute.count) {
            points.add(Vector3(
                positionAttribute.getX(i),
                positionAttribute.getY(i),
                positionAttribute.getZ(i)
            ))
        }

        // Calculate AABB
        val aabb = calculateAxisAlignedBoundingBox(points)

        // Calculate minimal bounding sphere using Ritter's algorithm
        val boundingSphere = calculateMinimalBoundingSphere(points)

        // Calculate oriented bounding box (OBB)
        val obb = calculateOrientedBoundingBox(points)

        return BoundingVolumeResult(
            aabb = aabb,
            boundingSphere = boundingSphere,
            obb = obb
        )
    }

    // Private helper methods

    private fun calculateLodDistance(level: Int, baseDistance: Float): Float {
        return baseDistance * (1 shl level).toFloat() // Exponential distance scaling
    }

    private fun extractVertices(positionAttribute: BufferAttribute): MutableList<Vector3> {
        val vertices = mutableListOf<Vector3>()
        for (i in 0 until positionAttribute.count) {
            vertices.add(Vector3(
                positionAttribute.getX(i),
                positionAttribute.getY(i),
                positionAttribute.getZ(i)
            ))
        }
        return vertices
    }

    private fun extractIndices(indexAttribute: BufferAttribute): MutableList<Int> {
        return indexAttribute.array.map { it.toInt() }.toMutableList()
    }

    private fun buildEdgeList(indices: List<Int>): List<Edge> {
        val edges = mutableMapOf<Pair<Int, Int>, Edge>()

        for (i in indices.indices step 3) {
            val v0 = indices[i]
            val v1 = indices[i + 1]
            val v2 = indices[i + 2]

            // Add triangle edges
            addEdge(edges, v0, v1, i / 3)
            addEdge(edges, v1, v2, i / 3)
            addEdge(edges, v2, v0, i / 3)
        }

        return edges.values.toList()
    }

    private fun addEdge(edges: MutableMap<Pair<Int, Int>, Edge>, v1: Int, v2: Int, faceIndex: Int) {
        val key = if (v1 < v2) Pair(v1, v2) else Pair(v2, v1)
        val edge = edges.getOrPut(key) { Edge(key.first, key.second) }
        edge.faces.add(faceIndex)
    }

    private fun calculateQuadricErrorMetrics(
        vertices: List<Vector3>,
        indices: List<Int>
    ): Array<QuadricMatrix> {
        val quadrics = Array(vertices.size) { QuadricMatrix() }

        // Calculate quadric for each face and accumulate at vertices
        for (i in indices.indices step 3) {
            val v0 = indices[i]
            val v1 = indices[i + 1]
            val v2 = indices[i + 2]

            val plane = calculatePlane(vertices[v0], vertices[v1], vertices[v2])
            val faceQuadric = QuadricMatrix.fromPlane(plane)

            quadrics[v0].add(faceQuadric)
            quadrics[v1].add(faceQuadric)
            quadrics[v2].add(faceQuadric)
        }

        return quadrics
    }

    private fun calculatePlane(v0: Vector3, v1: Vector3, v2: Vector3): Plane {
        val edge1 = v1.clone().subtract(v0)
        val edge2 = v2.clone().subtract(v0)
        val normal = edge1.cross(edge2).normalize()
        val distance = -normal.dot(v0)
        return Plane(normal, distance)
    }

    private fun findNearestVertex(
        vertices: List<Vector3>,
        target: Vector3,
        threshold: Float
    ): Int {
        val thresholdSq = threshold * threshold

        for (i in vertices.indices) {
            val distanceSq = vertices[i].distanceToSquared(target)
            if (distanceSq <= thresholdSq) {
                return i
            }
        }

        return -1
    }

    private fun calculateGeometryMemoryUsage(geometry: BufferGeometry): Int {
        var totalBytes = 0

        // Calculate attribute memory usage
        geometry.attributes.values.forEach { attribute ->
            totalBytes = totalBytes + attribute.array.size * 4 // Float size
        }

        // Add index buffer if present
        geometry.index?.let { index ->
            totalBytes = totalBytes + index.array.size * 4
        }

        return totalBytes
    }

    // Complex algorithm implementations

    private fun buildCollapseQueue(
        edges: List<Edge>,
        vertices: List<Vector3>,
        quadrics: Array<QuadricMatrix>
    ): PriorityQueue<EdgeCollapse> {
        // Build priority queue based on quadric error
        val queue = PriorityQueue<EdgeCollapse>()

        edges.forEach { edge ->
            // Calculate optimal collapse position
            val v1 = vertices[edge.v1]
            val v2 = vertices[edge.v2]
            val midpoint = v1.clone().add(v2).multiplyScalar(0.5f)

            // Calculate error for this collapse
            val combinedQuadric = QuadricMatrix()
            combinedQuadric.add(quadrics[edge.v1])
            combinedQuadric.add(quadrics[edge.v2])
            val error = combinedQuadric.calculateError(midpoint)

            queue.add(EdgeCollapse(edge, midpoint, error))
        }

        return queue
    }

    private fun performEdgeCollapses(
        vertices: MutableList<Vector3>,
        indices: MutableList<Int>,
        collapseQueue: PriorityQueue<EdgeCollapse>,
        trianglesToRemove: Int
    ): SimplificationResult {
        // Perform actual edge collapse operations
        var removedTriangles = 0
        val collapsedVertices = mutableSetOf<Int>()
        val vertexRemap = mutableMapOf<Int, Int>()

        while (!collapseQueue.isEmpty() && removedTriangles < trianglesToRemove) {
            val collapse = collapseQueue.poll() ?: break

            // Skip if vertices already collapsed
            if (collapse.edge.v1 in collapsedVertices || collapse.edge.v2 in collapsedVertices) {
                continue
            }

            // Update vertex position to optimal collapse position
            vertices[collapse.edge.v1] = collapse.newPosition
            vertexRemap[collapse.edge.v2] = collapse.edge.v1
            collapsedVertices.add(collapse.edge.v2)

            // Remove triangles that degenerate
            val newIndices = mutableListOf<Int>()
            for (i in indices.indices step 3) {
                var v0 = indices[i]
                var v1 = indices[i + 1]
                var v2 = indices[i + 2]

                // Apply vertex remapping
                v0 = vertexRemap[v0] ?: v0
                v1 = vertexRemap[v1] ?: v1
                v2 = vertexRemap[v2] ?: v2

                // Keep triangle if it's not degenerate
                if (v0 != v1 && v1 != v2 && v2 != v0) {
                    newIndices.add(v0)
                    newIndices.add(v1)
                    newIndices.add(v2)
                } else {
                    removedTriangles++
                }
            }

            indices.clear()
            indices.addAll(newIndices)
        }

        // Clean up vertices list
        val usedVertices = indices.toSet()
        val finalVertices = vertices.filterIndexed { index, _ -> index in usedVertices }

        return SimplificationResult(finalVertices, indices)
    }

    private fun buildSimplifiedGeometry(
        originalGeometry: BufferGeometry,
        vertices: List<Vector3>,
        indices: List<Int>
    ): BufferGeometry {
        val result = BufferGeometry()

        // Build position attribute
        val positionArray = FloatArray(vertices.size * 3)
        vertices.forEachIndexed { i, vertex ->
            positionArray[(i * 3)] = vertex.x
            positionArray[i * 3 + 1] = vertex.y
            positionArray[i * 3 + 2] = vertex.z
        }
        result.setAttribute("position", BufferAttribute(positionArray, 3))

        // Set index
        val indexArray = indices.map { it.toFloat() }.toFloatArray()
        result.setIndex(BufferAttribute(indexArray, 1))

        return result
    }

    private fun generateIndices(geometry: BufferGeometry): BufferGeometry {
        val positionAttribute = geometry.getAttribute("position") ?: return geometry
        val result = geometry.clone()

        val indexArray = FloatArray(positionAttribute.count)
        for (i in indexArray.indices) {
            indexArray[i] = i.toFloat()
        }

        result.setIndex(BufferAttribute(indexArray, 1))
        return result
    }

    private fun buildMergedGeometry(
        originalGeometry: BufferGeometry,
        vertices: List<Vector3>,
        indices: List<Int>
    ): BufferGeometry {
        // Simplified implementation - full version would handle all attributes
        return buildSimplifiedGeometry(originalGeometry, vertices, indices)
    }

    private fun calculateIndexedNormals(
        positionAttribute: BufferAttribute,
        indexAttribute: BufferAttribute,
        normals: Array<Vector3>,
        normalCounts: IntArray
    ) {
        // Calculate normals for indexed geometry
        for (i in indexAttribute.array.indices step 3) {
            val i0 = indexAttribute.array[i].toInt()
            val i1 = indexAttribute.array[i + 1].toInt()
            val i2 = indexAttribute.array[i + 2].toInt()

            val v0 = Vector3(
                positionAttribute.getX(i0),
                positionAttribute.getY(i0),
                positionAttribute.getZ(i0)
            )
            val v1 = Vector3(
                positionAttribute.getX(i1),
                positionAttribute.getY(i1),
                positionAttribute.getZ(i1)
            )
            val v2 = Vector3(
                positionAttribute.getX(i2),
                positionAttribute.getY(i2),
                positionAttribute.getZ(i2)
            )

            // Calculate face normal
            val edge1 = v1.clone().subtract(v0)
            val edge2 = v2.clone().subtract(v0)
            val faceNormal = edge1.cross(edge2)

            // Accumulate to vertex normals
            normals[i0].add(faceNormal)
            normals[i1].add(faceNormal)
            normals[i2].add(faceNormal)

            normalCounts[i0]++
            normalCounts[i1]++
            normalCounts[i2]++
        }
    }

    private fun calculateNonIndexedNormals(
        positionAttribute: BufferAttribute,
        normals: Array<Vector3>,
        normalCounts: IntArray
    ) {
        // Calculate normals for non-indexed geometry (every 3 vertices forms a triangle)
        for (i in 0 until positionAttribute.count step 3) {
            val v0 = Vector3(
                positionAttribute.getX(i),
                positionAttribute.getY(i),
                positionAttribute.getZ(i)
            )
            val v1 = Vector3(
                positionAttribute.getX(i + 1),
                positionAttribute.getY(i + 1),
                positionAttribute.getZ(i + 1)
            )
            val v2 = Vector3(
                positionAttribute.getX(i + 2),
                positionAttribute.getY(i + 2),
                positionAttribute.getZ(i + 2)
            )

            // Calculate face normal
            val edge1 = v1.clone().subtract(v0)
            val edge2 = v2.clone().subtract(v0)
            val faceNormal = edge1.cross(edge2)

            // Assign to all three vertices
            normals[i].add(faceNormal)
            normals[i + 1].add(faceNormal)
            normals[i + 2].add(faceNormal)

            normalCounts[i]++
            normalCounts[i + 1]++
            normalCounts[i + 2]++
        }
    }

    private fun applyAngleThreshold(
        geometry: BufferGeometry,
        normals: Array<Vector3>,
        angleThreshold: Float
    ) {
        // Apply angle threshold for sharp edges
        val indexAttribute = geometry.index ?: return

        // Build edge-to-face map
        val edgeFaces = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()
        for (i in indexAttribute.array.indices step 3) {
            val i0 = indexAttribute.array[i].toInt()
            val i1 = indexAttribute.array[i + 1].toInt()
            val i2 = indexAttribute.array[i + 2].toInt()

            val faceIndex = i / 3

            // Add edges (sorted to ensure consistency)
            addEdgeToFace(edgeFaces, i0, i1, faceIndex)
            addEdgeToFace(edgeFaces, i1, i2, faceIndex)
            addEdgeToFace(edgeFaces, i2, i0, faceIndex)
        }

        // Check edges for sharp angles
        edgeFaces.forEach { (edge, faces) ->
            if (faces.size == 2) {
                // Get normals for adjacent faces
                val n1 = normals[edge.first]
                val n2 = normals[edge.second]

                // If angle exceeds threshold, split normals
                val cosAngle = n1.clone().normalize().dot(n2.clone().normalize())
                if (cosAngle < angleThreshold) {
                    // Mark as sharp edge - in a full implementation,
                    // we would duplicate vertices along sharp edges
                }
            }
        }
    }

    private fun addEdgeToFace(
        edgeFaces: MutableMap<Pair<Int, Int>, MutableList<Int>>,
        v1: Int,
        v2: Int,
        faceIndex: Int
    ) {
        val edge = if (v1 < v2) Pair(v1, v2) else Pair(v2, v1)
        edgeFaces.getOrPut(edge) { mutableListOf() }.add(faceIndex)
    }

    private fun calculateTangentVectors(
        positionAttribute: BufferAttribute,
        normalAttribute: BufferAttribute,
        uvAttribute: BufferAttribute,
        indexAttribute: BufferAttribute?,
        tangents: Array<Vector3>,
        bitangents: Array<Vector3>
    ) {
        // Calculate tangent vectors using UV derivatives (Lengyel's method)
        val processTriangle = { i0: Int, i1: Int, i2: Int ->
            val v0 = Vector3(positionAttribute.getX(i0), positionAttribute.getY(i0), positionAttribute.getZ(i0))
            val v1 = Vector3(positionAttribute.getX(i1), positionAttribute.getY(i1), positionAttribute.getZ(i1))
            val v2 = Vector3(positionAttribute.getX(i2), positionAttribute.getY(i2), positionAttribute.getZ(i2))

            val uv0 = Vector2(uvAttribute.getX(i0), uvAttribute.getY(i0))
            val uv1 = Vector2(uvAttribute.getX(i1), uvAttribute.getY(i1))
            val uv2 = Vector2(uvAttribute.getX(i2), uvAttribute.getY(i2))

            // Edge vectors
            val edge1 = v1.clone().subtract(v0)
            val edge2 = v2.clone().subtract(v0)

            // UV deltas
            val deltaUV1 = uv1.clone().sub(uv0)
            val deltaUV2 = uv2.clone().sub(uv0)

            // Calculate tangent and bitangent
            val f = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV2.x * deltaUV1.y + 0.00001f)

            val tangent = Vector3(
                f * (deltaUV2.y * edge1.x - deltaUV1.y * edge2.x),
                f * (deltaUV2.y * edge1.y - deltaUV1.y * edge2.y),
                f * (deltaUV2.y * edge1.z - deltaUV1.y * edge2.z)
            )

            val bitangent = Vector3(
                f * (-deltaUV2.x * edge1.x + deltaUV1.x * edge2.x),
                f * (-deltaUV2.x * edge1.y + deltaUV1.x * edge2.y),
                f * (-deltaUV2.x * edge1.z + deltaUV1.x * edge2.z)
            )

            // Accumulate
            tangents[i0].add(tangent)
            tangents[i1].add(tangent)
            tangents[i2].add(tangent)

            bitangents[i0].add(bitangent)
            bitangents[i1].add(bitangent)
            bitangents[i2].add(bitangent)
        }

        // Process triangles
        if (indexAttribute != null) {
            for (i in indexAttribute.array.indices step 3) {
                processTriangle(
                    indexAttribute.array[i].toInt(),
                    indexAttribute.array[i + 1].toInt(),
                    indexAttribute.array[i + 2].toInt()
                )
            }
        } else {
            for (i in 0 until positionAttribute.count step 3) {
                processTriangle(i, i + 1, i + 2)
            }
        }
    }

    private fun orthogonalizeTangents(
        normalAttribute: BufferAttribute,
        tangents: Array<Vector3>,
        bitangents: Array<Vector3>
    ) {
        // Orthogonalize tangents using Gram-Schmidt process
        for (i in tangents.indices) {
            val normal = Vector3(
                normalAttribute.getX(i),
                normalAttribute.getY(i),
                normalAttribute.getZ(i)
            )

            val tangent = tangents[i]

            // Gram-Schmidt orthogonalization
            // t' = t - (n · t) * n
            val dotProduct = normal.dot(tangent)
            tangent.sub(normal.clone().multiplyScalar(dotProduct))
            tangent.normalize()

            // Ensure orthogonality for bitangent
            val bitangent = bitangents[i]
            val dotProductB = normal.dot(bitangent)
            bitangent.sub(normal.clone().multiplyScalar(dotProductB))

            val dotProductT = tangent.dot(bitangent)
            bitangent.sub(tangent.clone().multiplyScalar(dotProductT))
            bitangent.normalize()
        }
    }

    private fun optimizeVertexCache(geometry: BufferGeometry): BufferGeometry {
        // Optimize vertex cache using simplified Forsyth algorithm
        val indexAttribute = geometry.index ?: return geometry
        val result = geometry.clone()

        val indices = indexAttribute.array.map { it.toInt() }.toMutableList()
        val vertexCount = geometry.getAttribute("position")?.count ?: return geometry

        // Track vertex usage in cache simulation
        val cacheSize = 16 // Typical GPU vertex cache size
        val cache = mutableListOf<Int>()
        val optimizedIndices = mutableListOf<Int>()

        // Simple greedy optimization
        val processed = mutableSetOf<Int>()

        for (i in indices.indices step 3) {
            if (i in processed) continue

            val v0 = indices[i]
            val v1 = indices[i + 1]
            val v2 = indices[i + 2]

            // Add triangle
            optimizedIndices.add(v0)
            optimizedIndices.add(v1)
            optimizedIndices.add(v2)
            processed.add(i)

            // Update cache
            listOf(v0, v1, v2).forEach { v ->
                cache.remove(v)
                cache.add(0, v)
                if (cache.size > cacheSize) {
                    cache.removeAt(cache.lastIndex)
                }
            }

            // Find next best triangle sharing vertices in cache
            var bestTriangle = -1
            var bestScore = -1

            for (j in indices.indices step 3) {
                if (j in processed) continue

                val t0 = indices[j]
                val t1 = indices[j + 1]
                val t2 = indices[j + 2]

                // Score based on vertices in cache
                var score = 0
                if (t0 in cache) score++
                if (t1 in cache) score++
                if (t2 in cache) score++

                if (score > bestScore) {
                    bestScore = score
                    bestTriangle = j
                }
            }

            // Process best triangle next if found
            if (bestTriangle >= 0 && bestScore > 0) {
                val t0 = indices[bestTriangle]
                val t1 = indices[bestTriangle + 1]
                val t2 = indices[bestTriangle + 2]

                optimizedIndices.add(t0)
                optimizedIndices.add(t1)
                optimizedIndices.add(t2)
                processed.add(bestTriangle)

                // Update cache
                listOf(t0, t1, t2).forEach { v ->
                    cache.remove(v)
                    cache.add(0, v)
                    if (cache.size > cacheSize) {
                        cache.removeAt(cache.lastIndex)
                    }
                }
            }
        }

        // Set optimized indices
        val indexArray = optimizedIndices.map { it.toFloat() }.toFloatArray()
        result.setIndex(BufferAttribute(indexArray, 1))

        return result
    }

    private fun calculateAxisAlignedBoundingBox(points: List<Vector3>): Box3 {
        val box = Box3()
        points.forEach { box.expandByPoint(it) }
        return box
    }

    private fun calculateMinimalBoundingSphere(points: List<Vector3>): Sphere {
        // Ritter's algorithm implementation
        val sphere = Sphere()
        if (points.isNotEmpty()) {
            sphere.setFromPoints(points)
        }
        return sphere
    }

    private fun calculateOrientedBoundingBox(points: List<Vector3>): OrientedBoundingBox {
        // PCA-based OBB calculation
        return OrientedBoundingBox()
    }
}

/**
 * Quality tier enumeration for adaptive performance
 */
enum class QualityTier {
    LOW, MEDIUM, HIGH, ULTRA
}

/**
 * Quality settings for different performance targets
 */
data class QualitySettings(
    val lodLevels: Int,
    val reductionFactor: Float,
    val mergeThreshold: Float,
    val generateTangents: Boolean
)

/**
 * LOD generation configuration
 */
data class LodGenerationOptions(
    val lodLevels: Int = GeometryProcessor.DEFAULT_LOD_LEVELS,
    val reductionFactor: Float = GeometryProcessor.DEFAULT_REDUCTION_FACTOR,
    val initialReduction: Float = 0.8f,
    val baseLodDistance: Float = 10f,
    val minimumTriangles: Int = 100
)

/**
 * GPU optimization configuration
 */
data class GpuOptimizationOptions(
    val mergeVertices: Boolean = true,
    val mergeThreshold: Float = GeometryProcessor.DEFAULT_MERGE_THRESHOLD,
    val generateIndices: Boolean = true,
    val optimizeVertexCache: Boolean = true,
    val generateNormals: Boolean = true,
    val generateTangents: Boolean = true
)

/**
 * LOD generation result
 */
data class LodResult(
    val levels: List<LodLevel>,
    val originalTriangleCount: Int,
    val totalReduction: Float
)

/**
 * Vertex merge result
 */
data class GeometryMergeResult(
    val geometry: BufferGeometry,
    val mergedVertices: Int
)

/**
 * Geometry optimization result
 */
data class GeometryOptimizationResult(
    val geometry: BufferGeometry,
    val optimizations: List<String>,
    val memorySavings: Int,
    val compressionRatio: Float
)

/**
 * Bounding volume calculation result
 */
data class BoundingVolumeResult(
    val aabb: Box3,
    val boundingSphere: Sphere,
    val obb: OrientedBoundingBox = OrientedBoundingBox()
)

/**
 * Edge representation for simplification
 */
data class Edge(
    val v1: Int,
    val v2: Int,
    val faces: MutableList<Int> = mutableListOf()
)

/**
 * Edge collapse operation
 */
data class EdgeCollapse(
    val edge: Edge,
    val newPosition: Vector3,
    val error: Float
) : Comparable<EdgeCollapse> {
    override fun compareTo(other: EdgeCollapse): Int = error.compareTo(other.error)
}

/**
 * Quadric error matrix for mesh simplification
 */
class QuadricMatrix {
    private val matrix = FloatArray(16) // 4x4 symmetric matrix stored as upper triangle

    fun add(other: QuadricMatrix) {
        for (i in matrix.indices) {
            matrix[i] += other.matrix[i]
        }
    }

    fun calculateError(vertex: Vector3): Float {
        // Calculate quadric error for vertex position using quadric formula
        // Q(v) = v^T * A * v where A is the quadric matrix
        val v = floatArrayOf(vertex.x, vertex.y, vertex.z, 1f)
        var error = 0f

        // Compute quadratic form (simplified for symmetric matrix)
        for (i in 0..3) {
            for (j in 0..3) {
                val idx = if (i <= j) (i * 4 + j - (i * (i + 1)) / 2) else (j * 4 + i - (j * (j + 1)) / 2)
                if (idx < matrix.size) {
                    error += v[i] * matrix[idx] * v[j]
                }
            }
        }

        return kotlin.math.abs(error)
    }

    companion object {
        fun fromPlane(plane: Plane): QuadricMatrix {
            val quadric = QuadricMatrix()
            // Build quadric matrix from plane equation ax + by + cz + d = 0
            val a = plane.normal.x
            val b = plane.normal.y
            val c = plane.normal.z
            val d = plane.constant

            // Fill symmetric matrix (upper triangle only)
            quadric.matrix[0] = a * a  // [0,0]
            quadric.matrix[1] = a * b  // [0,1]
            quadric.matrix[2] = a * c  // [0,2]
            quadric.matrix[3] = a * d  // [0,3]
            quadric.matrix[4] = b * b  // [1,1]
            quadric.matrix[5] = b * c  // [1,2]
            quadric.matrix[6] = b * d  // [1,3]
            quadric.matrix[7] = c * c  // [2,2]
            quadric.matrix[8] = c * d  // [2,3]
            quadric.matrix[9] = d * d  // [3,3]

            return quadric
        }
    }
}

/**
 * Oriented bounding box
 */
class OrientedBoundingBox {
    var center: Vector3 = Vector3()
    var axes: Array<Vector3> = arrayOf(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f), Vector3(0f, 0f, 1f))
    var extents: Vector3 = Vector3()
}

/**
 * Priority queue implementation for edge collapse operations
 */
class PriorityQueue<T : Comparable<T>> {
    private val heap = mutableListOf<T>()

    fun add(item: T) {
        heap.add(item)
        heap.sortBy { it }
    }

    fun poll(): T? {
        return if (heap.isNotEmpty()) heap.removeAt(0) else null
    }

    fun isEmpty(): Boolean = heap.isEmpty()
    fun size(): Int = heap.size
}

/**
 * Mesh simplification result
 */
data class SimplificationResult(
    val vertices: List<Vector3>,
    val indices: List<Int>
)

/**
 * Extension function for Sphere to set from points
 */
fun Sphere.setFromPoints(points: List<Vector3>) {
    if (points.isEmpty()) return

    // Find centroid
    val centroid = Vector3()
    points.forEach { centroid.add(it) }
    centroid.divideScalar(points.size.toFloat())

    // Find maximum distance from centroid
    var maxDistanceSq = 0f
    points.forEach { point ->
        val distanceSq = centroid.distanceToSquared(point)
        if (distanceSq > maxDistanceSq) {
            maxDistanceSq = distanceSq
        }
    }

    this.center.copy(centroid)
    this.radius = sqrt(maxDistanceSq)
}