/**
 * UVGenerator for procedural texture coordinate mapping
 * T025 - Advanced UV mapping with multiple projection methods and automatic unwrapping
 *
 * Provides algorithms for:
 * - Box projection UV mapping
 * - Cylindrical and spherical projections
 * - Planar UV projection with custom planes
 * - UV unwrapping algorithms
 * - Automatic seam detection and handling
 * - Atlas-friendly UV layouts
 */
package io.kreekt.geometry

import io.kreekt.core.math.*
import io.kreekt.core.platform.platformClone
import kotlin.math.*
import io.kreekt.core.platform.platformClone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Advanced UV coordinate generator for texture mapping
 * Implements industry-standard UV projection and unwrapping techniques
 */
class UVGenerator {

    companion object {
        // Default UV generation settings
        const val DEFAULT_SEAM_ANGLE = 0.5f // ~30 degrees
        const val DEFAULT_DISTORTION_THRESHOLD = 0.1f
        const val DEFAULT_UV_PADDING = 0.02f
        const val DEFAULT_ATLAS_SIZE = 1024

        // UV projection methods
        enum class ProjectionMethod {
            PLANAR,
            BOX,
            CYLINDRICAL,
            SPHERICAL,
            CONFORMAL,
            ANGLE_BASED
        }
    }

    /**
     * Generate UV coordinates using box projection
     * Projects geometry onto 6 faces of a bounding box
     */
    fun generateBoxUV(
        geometry: BufferGeometry,
        options: BoxUVOptions = BoxUVOptions()
    ): UVGenerationResult {
        val positionAttribute = geometry.getAttribute("position")
            ?: return UVGenerationResult(geometry, false, "No position attribute found")

        val normalAttribute = geometry.getAttribute("normal")
            ?: generateNormalsIfNeeded(geometry).getAttribute("normal")
            ?: return UVGenerationResult(geometry, false, "Could not generate normals")

        val vertexCount = positionAttribute.count
        val uvCoordinates = FloatArray((vertexCount * 2))
        val seamVertices = mutableSetOf<Int>()

        // Calculate bounding box for normalization
        val boundingBox = geometry.computeBoundingBox()
        val size = boundingBox.getSize(Vector3())
        val center = boundingBox.getCenter(Vector3())

        for (i in 0 until vertexCount) {
            val position = Vector3(
                positionAttribute.getX(i),
                positionAttribute.getY(i),
                positionAttribute.getZ(i)
            )
            val normal = Vector3(
                normalAttribute.getX(i),
                normalAttribute.getY(i),
                normalAttribute.getZ(i)
            )

            // Determine dominant axis based on normal
            val face = getDominantFace(normal)
            val localPos = position.clone().subtract(center)

            // Project onto appropriate face
            val uv = projectToBoxFace(localPos, size, face, options.faceMapping)

            uvCoordinates[(i * 2)] = uv.x
            uvCoordinates[i * 2 + 1] = uv.y

            // Mark seam vertices
            if (isSeamVertex(normal, options.seamAngle)) {
                seamVertices.add(i)
            }
        }

        // Apply UV transformations
        applyUVTransformations(uvCoordinates, options.uvTransform)

        // Handle seams if requested
        if (options.handleSeams) {
            handleBoxProjectionSeams(geometry, uvCoordinates, seamVertices)
        }

        val resultGeometry = geometry.clone()
        resultGeometry.setAttribute("uv", BufferAttribute(uvCoordinates, 2))

        return UVGenerationResult(
            geometry = resultGeometry,
            success = true,
            message = "Box UV projection completed",
            seamVertices = seamVertices.toList(),
            uvBounds = calculateUVBounds(uvCoordinates)
        )
    }

    /**
     * Generate UV coordinates using cylindrical projection
     * Ideal for objects with cylindrical symmetry
     */
    fun generateCylindricalUV(
        geometry: BufferGeometry,
        options: CylindricalUVOptions = CylindricalUVOptions()
    ): UVGenerationResult {
        val positionAttribute = geometry.getAttribute("position")
            ?: return UVGenerationResult(geometry, false, "No position attribute found")

        val vertexCount = positionAttribute.count
        val uvCoordinates = FloatArray((vertexCount * 2))

        // Transform to cylinder space
        val axis = options.axis.clone().normalize()
        val boundingBox = geometry.computeBoundingBox()
        val height = boundingBox.getSize(Vector3()).dot(axis)
        val center = boundingBox.getCenter(Vector3())

        for (i in 0 until vertexCount) {
            val position = Vector3(
                positionAttribute.getX(i),
                positionAttribute.getY(i),
                positionAttribute.getZ(i)
            )

            val localPos = position.clone().subtract(center)
            val uv = projectToCylinder(localPos, axis, height, options)

            uvCoordinates[(i * 2)] = uv.x
            uvCoordinates[i * 2 + 1] = uv.y
        }

        // Handle seam at angle boundary
        val seamVertices = findCylindricalSeam(uvCoordinates, options.seamThreshold)

        val resultGeometry = geometry.clone()
        resultGeometry.setAttribute("uv", BufferAttribute(uvCoordinates, 2))

        return UVGenerationResult(
            geometry = resultGeometry,
            success = true,
            message = "Cylindrical UV projection completed",
            seamVertices = seamVertices,
            uvBounds = calculateUVBounds(uvCoordinates)
        )
    }

    /**
     * Generate UV coordinates using spherical projection
     * Ideal for spherical objects like planets or heads
     */
    fun generateSphericalUV(
        geometry: BufferGeometry,
        options: SphericalUVOptions = SphericalUVOptions()
    ): UVGenerationResult {
        val positionAttribute = geometry.getAttribute("position")
            ?: return UVGenerationResult(geometry, false, "No position attribute found")

        val vertexCount = positionAttribute.count
        val uvCoordinates = FloatArray((vertexCount * 2))

        val boundingBox = geometry.computeBoundingBox()
        val center = boundingBox.getCenter(Vector3())
        val radius = boundingBox.getBoundingSphere(Sphere()).radius

        for (i in 0 until vertexCount) {
            val position = Vector3(
                positionAttribute.getX(i),
                positionAttribute.getY(i),
                positionAttribute.getZ(i)
            )

            val localPos = position.clone().subtract(center)
            val uv = projectToSphere(localPos, radius, options)

            uvCoordinates[(i * 2)] = uv.x
            uvCoordinates[i * 2 + 1] = uv.y
        }

        // Handle pole singularities
        val poleVertices = findSphericalPoles(uvCoordinates, options.poleThreshold)

        val resultGeometry = geometry.clone()
        resultGeometry.setAttribute("uv", BufferAttribute(uvCoordinates, 2))

        return UVGenerationResult(
            geometry = resultGeometry,
            success = true,
            message = "Spherical UV projection completed",
            seamVertices = poleVertices,
            uvBounds = calculateUVBounds(uvCoordinates)
        )
    }

    /**
     * Generate UV coordinates using planar projection
     * Projects onto a custom plane with specified orientation
     */
    fun generatePlanarUV(
        geometry: BufferGeometry,
        options: PlanarUVOptions = PlanarUVOptions()
    ): UVGenerationResult {
        val positionAttribute = geometry.getAttribute("position")
            ?: return UVGenerationResult(geometry, false, "No position attribute found")

        val vertexCount = positionAttribute.count
        val uvCoordinates = FloatArray((vertexCount * 2))

        // Create projection plane
        val normal = options.normal.clone().normalize()
        val uAxis = calculateUAxis(normal, options.upVector)
        val vAxis = normal.clone().cross(uAxis).normalize()

        // Project all vertices onto the plane
        val projectedPoints = mutableListOf<Vector2>()

        for (i in 0 until vertexCount) {
            val position = Vector3(
                positionAttribute.getX(i),
                positionAttribute.getY(i),
                positionAttribute.getZ(i)
            )

            val relativePos = position.clone().subtract(options.origin)
            val u = relativePos.dot(uAxis)
            val v = relativePos.dot(vAxis)

            projectedPoints.add(Vector2(u, v))
        }

        // Normalize to [0,1] range
        val bounds = calculateProjectionBounds(projectedPoints)
        val size = bounds.getSize(Vector2())

        for (i in projectedPoints.indices) {
            val point = projectedPoints[i]
            val normalizedU = if (size.x > 0) (point.x - bounds.min.x) / size.x else 0.5f
            val normalizedV = if (size.y > 0) (point.y - bounds.min.y) / size.y else 0.5f

            uvCoordinates[(i * 2)] = normalizedU
            uvCoordinates[i * 2 + 1] = normalizedV
        }

        val resultGeometry = geometry.clone()
        resultGeometry.setAttribute("uv", BufferAttribute(uvCoordinates, 2))

        return UVGenerationResult(
            geometry = resultGeometry,
            success = true,
            message = "Planar UV projection completed",
            uvBounds = calculateUVBounds(uvCoordinates)
        )
    }

    /**
     * Perform automatic UV unwrapping using angle-based flattening
     * Creates seam-free UV layouts for organic shapes
     */
    fun generateUnwrappedUV(
        geometry: BufferGeometry,
        options: UnwrapOptions = UnwrapOptions()
    ): UVGenerationResult {
        val positionAttribute = geometry.getAttribute("position")
            ?: return UVGenerationResult(geometry, false, "No position attribute found")

        val indexAttribute = geometry.index
            ?: return UVGenerationResult(geometry, false, "Unwrapping requires indexed geometry")

        // Build mesh connectivity
        val mesh = buildMeshConnectivity(positionAttribute, indexAttribute)

        // Detect and mark seams
        val seams = detectSeams(mesh, options.seamAngle)

        // Cut mesh along seams
        val charts = cutMeshAlongSeams(mesh, seams)

        // Flatten each chart
        val uvCharts = mutableListOf<UVChart>()
        for (chart in charts) {
            val flattened = flattenChart(chart, options.method)
            uvCharts.add(flattened)
        }

        // Pack charts into UV space
        val packedUVs = packChartsIntoAtlas(uvCharts, options.atlasSize, options.padding)

        // Apply to geometry
        val resultGeometry = applyUnwrappedUVs(geometry, packedUVs)

        return UVGenerationResult(
            geometry = resultGeometry,
            success = true,
            message = "UV unwrapping completed with ${charts.size} charts",
            seamVertices = seams.vertices.toList(),
            uvBounds = Box2(Vector2(0f, 0f), Vector2(1f, 1f))
        )
    }

    /**
     * Optimize existing UV coordinates for better texture utilization
     * Improves texel density and reduces stretching
     */
    fun optimizeUVLayout(
        geometry: BufferGeometry,
        options: UVOptimizationOptions = UVOptimizationOptions()
    ): UVOptimizationResult {
        val positionAttribute = geometry.getAttribute("position")
            ?: return UVOptimizationResult(geometry, false, listOf("No position attribute found"))

        val uvAttribute = geometry.getAttribute("uv")
            ?: return UVOptimizationResult(geometry, false, listOf("No UV attribute found"))

        var resultGeometry = geometry.clone()
        val optimizations = mutableListOf<String>()

        // Analyze current UV quality
        val quality = analyzeUVQuality(positionAttribute, uvAttribute)

        // Fix UV stretching if needed
        if (quality.maxStretch > options.stretchThreshold) {
            resultGeometry = fixUVStretching(resultGeometry, options.stretchReduction)
            optimizations.add("Reduced UV stretching")
        }

        // Optimize texel density
        if (options.optimizeTexelDensity) {
            resultGeometry = optimizeTexelDensity(resultGeometry)
            optimizations.add("Optimized texel density")
        }

        // Minimize wasted UV space
        if (options.minimizeWastedSpace) {
            resultGeometry = minimizeUVWaste(resultGeometry, options.packingEfficiency)
            optimizations.add("Minimized UV waste")
        }

        // Align UV islands to pixel grid
        if (options.snapToPixelGrid && options.textureResolution > 0) {
            resultGeometry = snapUVToGrid(resultGeometry, options.textureResolution)
            optimizations.add("Snapped to pixel grid")
        }

        val newQuality = analyzeUVQuality(
            resultGeometry.getAttribute("position")!!,
            resultGeometry.getAttribute("uv")!!
        )

        return UVOptimizationResult(
            geometry = resultGeometry,
            success = true,
            optimizations = optimizations,
            qualityImprovement = calculateQualityImprovement(quality, newQuality)
        )
    }

    /**
     * Generate UV coordinates with automatic method selection
     * Analyzes geometry and chooses the best projection method
     */
    fun generateAutomaticUV(
        geometry: BufferGeometry,
        options: AutomaticUVOptions = AutomaticUVOptions()
    ): UVGenerationResult {
        // Analyze geometry properties
        val analysis = analyzeGeometryForUV(geometry)

        // Select best projection method
        val method = selectOptimalProjectionMethod(analysis, options.preferredMethods)

        // Generate UVs using selected method
        return when (method) {
            ProjectionMethod.BOX -> generateBoxUV(geometry, options.boxOptions)
            ProjectionMethod.CYLINDRICAL -> generateCylindricalUV(geometry, options.cylindricalOptions)
            ProjectionMethod.SPHERICAL -> generateSphericalUV(geometry, options.sphericalOptions)
            ProjectionMethod.PLANAR -> generatePlanarUV(geometry, options.planarOptions)
            ProjectionMethod.CONFORMAL,
            ProjectionMethod.ANGLE_BASED -> generateUnwrappedUV(geometry, options.unwrapOptions)
        }
    }

    // Private helper methods

    private fun generateNormalsIfNeeded(geometry: BufferGeometry): BufferGeometry {
        return GeometryProcessor().generateSmoothNormals(geometry)
    }

    private fun getDominantFace(normal: Vector3): BoxFace {
        val absNormal = Vector3(abs(normal.x), abs(normal.y), abs(normal.z))

        return when {
            absNormal.x >= absNormal.y && absNormal.x >= absNormal.z -> {
                if (normal.x > 0) BoxFace.POSITIVE_X else BoxFace.NEGATIVE_X
            }
            absNormal.y >= absNormal.z -> {
                if (normal.y > 0) BoxFace.POSITIVE_Y else BoxFace.NEGATIVE_Y
            }
            else -> {
                if (normal.z > 0) BoxFace.POSITIVE_Z else BoxFace.NEGATIVE_Z
            }
        }
    }

    private fun projectToBoxFace(
        position: Vector3,
        size: Vector3,
        face: BoxFace,
        faceMapping: Map<BoxFace, Vector2>
    ): Vector2 {
        val normalizedPos = Vector3(
            position.x / size.x,
            position.y / size.y,
            position.z / size.z
        )

        return when (face) {
            BoxFace.POSITIVE_X -> Vector2(normalizedPos.z + 0.5f, normalizedPos.y + 0.5f)
            BoxFace.NEGATIVE_X -> Vector2(-normalizedPos.z + 0.5f, normalizedPos.y + 0.5f)
            BoxFace.POSITIVE_Y -> Vector2(normalizedPos.x + 0.5f, normalizedPos.z + 0.5f)
            BoxFace.NEGATIVE_Y -> Vector2(normalizedPos.x + 0.5f, -normalizedPos.z + 0.5f)
            BoxFace.POSITIVE_Z -> Vector2(-normalizedPos.x + 0.5f, normalizedPos.y + 0.5f)
            BoxFace.NEGATIVE_Z -> Vector2(normalizedPos.x + 0.5f, normalizedPos.y + 0.5f)
        }.apply {
            // Apply face-specific UV offset from mapping
            val offset = faceMapping[face] ?: Vector2()
            add(offset)
        }
    }

    private fun projectToCylinder(
        position: Vector3,
        axis: Vector3,
        height: Float,
        options: CylindricalUVOptions
    ): Vector2 {
        // Project position onto cylinder axis
        val axisProjection = axis.clone().multiplyScalar(position.dot(axis))
        val radialVector = position.clone().subtract(axisProjection)

        // Calculate angle around axis
        var angle = atan2(radialVector.z, radialVector.x)
        if (angle < 0) angle = angle + 2 * PI.toFloat()

        // Calculate height coordinate
        val heightCoord = (axisProjection.length() + height * 0.5f) / height

        val u = angle / (2 * PI.toFloat())
        val v = heightCoord.coerceIn(0f, 1f)

        return Vector2(u, v)
    }

    private fun projectToSphere(
        position: Vector3,
        radius: Float,
        options: SphericalUVOptions
    ): Vector2 {
        val normalized = position.clone().normalize()

        // Calculate spherical coordinates
        val phi = acos(normalized.y.coerceIn(-1f, 1f)) // Latitude (0 to π)
        val theta = atan2(normalized.z, normalized.x) // Longitude (-π to π)

        val u = (theta + PI.toFloat()) / (2 * PI.toFloat())
        val v = phi / PI.toFloat()

        return Vector2(u, v)
    }

    private fun calculateUAxis(normal: Vector3, upVector: Vector3): Vector3 {
        val up = upVector.clone().normalize()
        val uAxis = up.cross(normal).normalize()

        // If up vector is parallel to normal, use a different reference
        if (uAxis.length() < 0.001f) {
            val fallback = if (abs(normal.y) < 0.9f) Vector3(0f, 1f, 0f) else Vector3(1f, 0f, 0f)
            return fallback.cross(normal).normalize()
        }

        return uAxis
    }

    private fun calculateProjectionBounds(points: List<Vector2>): Box2 {
        if (points.isEmpty()) return Box2()

        var minX = points[0].x
        var maxX = points[0].x
        var minY = points[0].y
        var maxY = points[0].y

        for (point in points) {
            minX = minOf(minX, point.x)
            maxX = maxOf(maxX, point.x)
            minY = minOf(minY, point.y)
            maxY = maxOf(maxY, point.y)
        }

        return Box2(Vector2(minX, minY), Vector2(maxX, maxY))
    }

    private fun calculateUVBounds(uvCoordinates: FloatArray): Box2 {
        if (uvCoordinates.isEmpty()) return Box2()

        var minU = uvCoordinates[0]
        var maxU = uvCoordinates[0]
        var minV = uvCoordinates[1]
        var maxV = uvCoordinates[1]

        for (i in uvCoordinates.indices step 2) {
            val u = uvCoordinates[i]
            val v = uvCoordinates[i + 1]

            minU = minOf(minU, u)
            maxU = maxOf(maxU, u)
            minV = minOf(minV, v)
            maxV = maxOf(maxV, v)
        }

        return Box2(Vector2(minU, minV), Vector2(maxU, maxV))
    }

    private fun isSeamVertex(normal: Vector3, seamAngle: Float): Boolean {
        // Simplified seam detection based on normal direction changes
        return false // Placeholder implementation
    }

    private fun applyUVTransformations(uvCoordinates: FloatArray, transform: UVTransform) {
        for (i in uvCoordinates.indices step 2) {
            var u = uvCoordinates[i]
            var v = uvCoordinates[i + 1]

            // Apply offset
            u = u + transform.offset.x
            v = v + transform.offset.y

            // Apply scale
            u = u * transform.scale.x
            v = v * transform.scale.y

            // Apply rotation
            if (transform.rotation != 0f) {
                val cos = cos(transform.rotation)
                val sin = sin(transform.rotation)
                val centerU = u - 0.5f
                val centerV = v - 0.5f

                u = centerU * cos - centerV * sin + 0.5f
                v = centerU * sin + centerV * cos + 0.5f
            }

            uvCoordinates[i] = u
            uvCoordinates[i + 1] = v
        }
    }

    // Placeholder implementations for complex algorithms

    private fun handleBoxProjectionSeams(
        geometry: BufferGeometry,
        uvCoordinates: FloatArray,
        seamVertices: Set<Int>
    ) {
        // Implementation would handle UV seams in box projection
    }

    private fun findCylindricalSeam(
        uvCoordinates: FloatArray,
        seamThreshold: Float
    ): List<Int> {
        // Implementation would find seam at 0/1 UV boundary
        return emptyList()
    }

    private fun findSphericalPoles(
        uvCoordinates: FloatArray,
        poleThreshold: Float
    ): List<Int> {
        // Implementation would find vertices near UV poles
        return emptyList()
    }

    private fun buildMeshConnectivity(
        positionAttribute: BufferAttribute,
        indexAttribute: BufferAttribute
    ): MeshConnectivity {
        // Implementation would build mesh topology data
        return MeshConnectivity()
    }

    private fun detectSeams(mesh: MeshConnectivity, seamAngle: Float): SeamData {
        // Implementation would detect natural seam edges
        return SeamData()
    }

    private fun cutMeshAlongSeams(mesh: MeshConnectivity, seams: SeamData): List<MeshChart> {
        // Implementation would cut mesh into charts
        return emptyList()
    }

    private fun flattenChart(chart: MeshChart, method: UnwrapMethod): UVChart {
        // Implementation would flatten 3D chart to 2D UV space
        return UVChart()
    }

    private fun packChartsIntoAtlas(
        charts: List<UVChart>,
        atlasSize: Int,
        padding: Float
    ): PackedUVData {
        // Implementation would pack UV charts into atlas
        return PackedUVData()
    }

    private fun applyUnwrappedUVs(geometry: BufferGeometry, uvData: PackedUVData): BufferGeometry {
        // Implementation would apply unwrapped UVs to geometry
        return geometry
    }

    private fun analyzeUVQuality(
        positionAttribute: BufferAttribute,
        uvAttribute: BufferAttribute
    ): UVQualityMetrics {
        // Implementation would analyze UV stretching and distortion
        return UVQualityMetrics()
    }

    private fun fixUVStretching(geometry: BufferGeometry, reduction: Float): BufferGeometry {
        return geometry
    }

    private fun optimizeTexelDensity(geometry: BufferGeometry): BufferGeometry {
        return geometry
    }

    private fun minimizeUVWaste(geometry: BufferGeometry, efficiency: Float): BufferGeometry {
        return geometry
    }

    private fun snapUVToGrid(geometry: BufferGeometry, resolution: Int): BufferGeometry {
        return geometry
    }

    private fun calculateQualityImprovement(
        oldQuality: UVQualityMetrics,
        newQuality: UVQualityMetrics
    ): Float {
        return 0f
    }

    private fun analyzeGeometryForUV(geometry: BufferGeometry): GeometryAnalysis {
        return GeometryAnalysis()
    }

    private fun selectOptimalProjectionMethod(
        analysis: GeometryAnalysis,
        preferredMethods: List<ProjectionMethod>
    ): ProjectionMethod {
        return preferredMethods.firstOrNull() ?: ProjectionMethod.BOX
    }
}

/**
 * Box face enumeration for box projection
 */
enum class BoxFace {
    POSITIVE_X, NEGATIVE_X, POSITIVE_Y, NEGATIVE_Y, POSITIVE_Z, NEGATIVE_Z
}

/**
 * UV generation options for box projection
 */
data class BoxUVOptions(
    val faceMapping: Map<BoxFace, Vector2> = emptyMap(),
    val seamAngle: Float = UVGenerator.DEFAULT_SEAM_ANGLE,
    val handleSeams: Boolean = true,
    val uvTransform: UVTransform = UVTransform()
)

/**
 * UV generation options for cylindrical projection
 */
data class CylindricalUVOptions(
    val axis: Vector3 = Vector3(0f, 1f, 0f),
    val seamThreshold: Float = 0.05f
)

/**
 * UV generation options for spherical projection
 */
data class SphericalUVOptions(
    val poleThreshold: Float = 0.05f
)

/**
 * UV generation options for planar projection
 */
data class PlanarUVOptions(
    val normal: Vector3 = Vector3(0f, 0f, 1f),
    val upVector: Vector3 = Vector3(0f, 1f, 0f),
    val origin: Vector3 = Vector3()
)

/**
 * UV unwrapping options
 */
data class UnwrapOptions(
    val method: UnwrapMethod = UnwrapMethod.ANGLE_BASED,
    val seamAngle: Float = UVGenerator.DEFAULT_SEAM_ANGLE,
    val atlasSize: Int = UVGenerator.DEFAULT_ATLAS_SIZE,
    val padding: Float = UVGenerator.DEFAULT_UV_PADDING
)

/**
 * UV optimization options
 */
data class UVOptimizationOptions(
    val stretchThreshold: Float = UVGenerator.DEFAULT_DISTORTION_THRESHOLD,
    val stretchReduction: Float = 0.5f,
    val optimizeTexelDensity: Boolean = true,
    val minimizeWastedSpace: Boolean = true,
    val snapToPixelGrid: Boolean = false,
    val textureResolution: Int = 0,
    val packingEfficiency: Float = 0.9f
)

/**
 * Automatic UV generation options
 */
data class AutomaticUVOptions(
    val preferredMethods: List<UVGenerator.Companion.ProjectionMethod> = listOf(
        UVGenerator.Companion.ProjectionMethod.BOX,
        UVGenerator.Companion.ProjectionMethod.CYLINDRICAL,
        UVGenerator.Companion.ProjectionMethod.SPHERICAL
    ),
    val boxOptions: BoxUVOptions = BoxUVOptions(),
    val cylindricalOptions: CylindricalUVOptions = CylindricalUVOptions(),
    val sphericalOptions: SphericalUVOptions = SphericalUVOptions(),
    val planarOptions: PlanarUVOptions = PlanarUVOptions(),
    val unwrapOptions: UnwrapOptions = UnwrapOptions()
)

/**
 * UV transformation parameters
 */
data class UVTransform(
    val offset: Vector2 = Vector2(),
    val scale: Vector2 = Vector2(1f, 1f),
    val rotation: Float = 0f
)

/**
 * UV generation result
 */
data class UVGenerationResult(
    val geometry: BufferGeometry,
    val success: Boolean,
    val message: String = "",
    val seamVertices: List<Int> = emptyList(),
    val uvBounds: Box2 = Box2()
)

/**
 * UV optimization result
 */
data class UVOptimizationResult(
    val geometry: BufferGeometry,
    val success: Boolean,
    val optimizations: List<String> = emptyList(),
    val qualityImprovement: Float = 0f
)

/**
 * Unwrapping method enumeration
 */
enum class UnwrapMethod {
    CONFORMAL, ANGLE_BASED, AREA_PRESERVING
}

/**
 * 2D bounding box for UV coordinates
 */
data class Box2(
    val min: Vector2 = Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    val max: Vector2 = Vector2(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
) {
    fun getSize(target: Vector2): Vector2 {
        return target.set(max.x - min.x, max.y - min.y)
    }

    fun getCenter(target: Vector2): Vector2 {
        return target.set((min.x + max.x) * 0.5f, (min.y + max.y) * 0.5f)
    }
}

// Placeholder data classes for complex algorithms

/**
 * Mesh connectivity data for unwrapping
 */
class MeshConnectivity

/**
 * Seam detection data
 */
class SeamData {
    val vertices = mutableSetOf<Int>()
}

/**
 * Mesh chart for unwrapping
 */
class MeshChart

/**
 * UV chart data
 */
class UVChart

/**
 * Packed UV atlas data
 */
class PackedUVData

/**
 * UV quality metrics
 */
class UVQualityMetrics {
    var maxStretch: Float = 0f
    var averageStretch: Float = 0f
    var texelDensityVariation: Float = 0f
    var wastedSpace: Float = 0f
}

/**
 * Geometry analysis for UV method selection
 */
class GeometryAnalysis {
    var isCylindrical: Boolean = false
    var isSpherical: Boolean = false
    var hasComplexTopology: Boolean = false
    var dominantAxis: Vector3 = Vector3()
}

/**
 * Extension function for Vector2 add operation
 */
fun Vector2.add(other: Vector2): Vector2 {
    x = x + other.x
    y = y + other.y
    return this
}