/**
 * Geometry API Contract
 *
 * This file defines the complete API surface for the geometry subsystem,
 * ensuring Three.js r180 compatibility with Kotlin idioms.
 *
 * Contract Design Principles:
 * - Type-safe method signatures
 * - Data classes for immutable configuration
 * - Default parameters for common use cases
 * - Sealed classes for type hierarchies
 * - Extension functions for DSL builders
 */

package io.kreekt.contracts.geometry

import io.kreekt.core.math.*
import io.kreekt.geometry.*

// ============================================================================
// Core Geometry API
// ============================================================================

/**
 * BufferGeometry: GPU-efficient vertex data storage
 * Three.js equivalent: THREE.BufferGeometry
 */
interface BufferGeometryAPI {
    // Identity
    val id: Int
    var name: String
    val type: String

    // Attribute management
    fun setAttribute(name: String, attribute: BufferAttribute): BufferGeometry
    fun getAttribute(name: String): BufferAttribute?
    fun deleteAttribute(name: String): Boolean
    fun hasAttribute(name: String): Boolean

    // Index management
    fun setIndex(indices: IntArray): BufferGeometry
    fun setIndex(indices: ShortArray): BufferGeometry
    fun setIndex(attribute: BufferAttribute?): BufferGeometry
    fun getIndex(): BufferAttribute?

    // Bounding volume computation
    fun computeBoundingBox()
    fun computeBoundingSphere()
    fun computeVertexNormals()
    fun computeTangents()

    // Groups for multi-material rendering
    fun addGroup(start: Int, count: Int, materialIndex: Int = 0)
    fun clearGroups()
    fun getGroups(): List<GeometryGroup>

    // Morph targets
    fun setMorphAttribute(name: String, attributes: List<BufferAttribute>)
    fun getMorphAttribute(name: String): List<BufferAttribute>?

    // Draw range
    fun setDrawRange(start: Int, count: Int)
    fun getDrawRange(): DrawRange

    // Cloning and copying
    fun clone(): BufferGeometry
    fun copy(source: BufferGeometry): BufferGeometry

    // Disposal
    fun dispose()

    // User data
    val userData: MutableMap<String, Any>
}

/**
 * BufferAttribute: Typed array wrapper for vertex attributes
 * Three.js equivalent: THREE.BufferAttribute
 */
interface BufferAttributeAPI {
    val array: FloatArray
    val itemSize: Int
    val count: Int
    var normalized: Boolean
    var needsUpdate: Boolean

    // Data access
    fun getX(index: Int): Float
    fun setX(index: Int, x: Float): BufferAttribute
    fun getY(index: Int): Float
    fun setY(index: Int, y: Float): BufferAttribute
    fun getZ(index: Int): Float
    fun setZ(index: Int, z: Float): BufferAttribute
    fun getW(index: Int): Float
    fun setW(index: Int, w: Float): BufferAttribute

    // Bulk operations
    fun set(array: FloatArray, offset: Int = 0): BufferAttribute
    fun copyArray(array: FloatArray): BufferAttribute
    fun copyVector2sArray(vectors: List<Vector2>): BufferAttribute
    fun copyVector3sArray(vectors: List<Vector3>): BufferAttribute
    fun copyVector4sArray(vectors: List<Vector4>): BufferAttribute
    fun copyColorsArray(colors: List<Color>): BufferAttribute

    // Transformations
    fun applyMatrix3(m: Matrix3): BufferAttribute
    fun applyMatrix4(m: Matrix4): BufferAttribute
    fun applyNormalMatrix(m: Matrix3): BufferAttribute
    fun transformDirection(m: Matrix4): BufferAttribute

    // Cloning
    fun clone(): BufferAttribute
    fun copy(source: BufferAttribute): BufferAttribute
}

/**
 * InterleavedBuffer: Interleaved attribute storage
 * Three.js equivalent: THREE.InterleavedBuffer
 */
interface InterleavedBufferAPI {
    val array: FloatArray
    val stride: Int
    val count: Int
    var needsUpdate: Boolean

    fun setUsage(usage: BufferUsage): InterleavedBuffer
    fun clone(): InterleavedBuffer
    fun copy(source: InterleavedBuffer): InterleavedBuffer
}

/**
 * InterleavedBufferAttribute: View into interleaved buffer
 * Three.js equivalent: THREE.InterleavedBufferAttribute
 */
interface InterleavedBufferAttributeAPI {
    val data: InterleavedBuffer
    val itemSize: Int
    val offset: Int
    val normalized: Boolean
    val count: Int

    fun getX(index: Int): Float
    fun setX(index: Int, x: Float): InterleavedBufferAttribute
    fun getY(index: Int): Float
    fun setY(index: Int, y: Float): InterleavedBufferAttribute
    fun getZ(index: Int): Float
    fun setZ(index: Int, z: Float): InterleavedBufferAttribute
    fun getW(index: Int): Float
    fun setW(index: Int, w: Float): InterleavedBufferAttribute
}

// ============================================================================
// Primitive Geometries
// ============================================================================

/**
 * BoxGeometry: Rectangular cuboid
 * Three.js equivalent: THREE.BoxGeometry
 */
interface BoxGeometryAPI {
    val width: Float
    val height: Float
    val depth: Float
    val widthSegments: Int
    val heightSegments: Int
    val depthSegments: Int

    // Inherited from BufferGeometry
    val attributes: Map<String, BufferAttribute>
    val index: BufferAttribute?
}

/**
 * SphereGeometry: Sphere with configurable segments
 * Three.js equivalent: THREE.SphereGeometry
 */
interface SphereGeometryAPI {
    val radius: Float
    val widthSegments: Int
    val heightSegments: Int
    val phiStart: Float
    val phiLength: Float
    val thetaStart: Float
    val thetaLength: Float
}

/**
 * PlaneGeometry: Rectangular plane
 * Three.js equivalent: THREE.PlaneGeometry
 */
interface PlaneGeometryAPI {
    val width: Float
    val height: Float
    val widthSegments: Int
    val heightSegments: Int
}

/**
 * CylinderGeometry: Cylindrical shape
 * Three.js equivalent: THREE.CylinderGeometry
 */
interface CylinderGeometryAPI {
    val radiusTop: Float
    val radiusBottom: Float
    val height: Float
    val radialSegments: Int
    val heightSegments: Int
    val openEnded: Boolean
    val thetaStart: Float
    val thetaLength: Float
}

/**
 * ConeGeometry: Conical shape (cylinder with radiusTop=0)
 * Three.js equivalent: THREE.ConeGeometry
 */
interface ConeGeometryAPI {
    val radius: Float
    val height: Float
    val radialSegments: Int
    val heightSegments: Int
    val openEnded: Boolean
    val thetaStart: Float
    val thetaLength: Float
}

/**
 * TorusGeometry: Donut shape
 * Three.js equivalent: THREE.TorusGeometry
 */
interface TorusGeometryAPI {
    val radius: Float
    val tube: Float
    val radialSegments: Int
    val tubularSegments: Int
    val arc: Float
}

/**
 * TorusKnotGeometry: Torus knot
 * Three.js equivalent: THREE.TorusKnotGeometry
 */
interface TorusKnotGeometryAPI {
    val radius: Float
    val tube: Float
    val tubularSegments: Int
    val radialSegments: Int
    val p: Int  // Number of times curve winds around
    val q: Int  // Number of times curve winds through center
}

/**
 * CircleGeometry: Circular disc
 * Three.js equivalent: THREE.CircleGeometry
 */
interface CircleGeometryAPI {
    val radius: Float
    val segments: Int
    val thetaStart: Float
    val thetaLength: Float
}

/**
 * RingGeometry: Flat ring
 * Three.js equivalent: THREE.RingGeometry
 */
interface RingGeometryAPI {
    val innerRadius: Float
    val outerRadius: Float
    val thetaSegments: Int
    val phiSegments: Int
    val thetaStart: Float
    val thetaLength: Float
}

/**
 * IcosahedronGeometry: Icosahedron (20-sided polyhedron)
 * Three.js equivalent: THREE.IcosahedronGeometry
 */
interface IcosahedronGeometryAPI {
    val radius: Float
    val detail: Int  // Subdivision level
}

/**
 * OctahedronGeometry: Octahedron (8-sided polyhedron)
 * Three.js equivalent: THREE.OctahedronGeometry
 */
interface OctahedronGeometryAPI {
    val radius: Float
    val detail: Int
}

/**
 * TetrahedronGeometry: Tetrahedron (4-sided polyhedron)
 * Three.js equivalent: THREE.TetrahedronGeometry
 */
interface TetrahedronGeometryAPI {
    val radius: Float
    val detail: Int
}

/**
 * DodecahedronGeometry: Dodecahedron (12-sided polyhedron)
 * Three.js equivalent: THREE.DodecahedronGeometry
 */
interface DodecahedronGeometryAPI {
    val radius: Float
    val detail: Int
}

/**
 * PolyhedronGeometry: Generic polyhedron from vertices and indices
 * Three.js equivalent: THREE.PolyhedronGeometry
 */
interface PolyhedronGeometryAPI {
    val vertices: FloatArray
    val indices: IntArray
    val radius: Float
    val detail: Int
}

/**
 * CapsuleGeometry: Capsule (cylinder with hemispheres)
 * Three.js equivalent: THREE.CapsuleGeometry
 */
interface CapsuleGeometryAPI {
    val radius: Float
    val length: Float
    val capSegments: Int
    val radialSegments: Int
}

// ============================================================================
// Advanced Geometries
// ============================================================================

/**
 * ExtrudeGeometry: Extrude 2D shape along Z axis
 * Three.js equivalent: THREE.ExtrudeGeometry
 */
interface ExtrudeGeometryAPI {
    val shapes: List<Shape>
    val options: ExtrudeGeometryOptions
}

data class ExtrudeGeometryOptions(
    val depth: Float = 1f,
    val bevelEnabled: Boolean = true,
    val bevelThickness: Float = 0.2f,
    val bevelSize: Float = 0.1f,
    val bevelOffset: Float = 0f,
    val bevelSegments: Int = 3,
    val steps: Int = 1,
    val extrudePath: Curve? = null,
    val UVGenerator: UVGenerator? = null
)

/**
 * LatheGeometry: Revolve 2D points around axis
 * Three.js equivalent: THREE.LatheGeometry
 */
interface LatheGeometryAPI {
    val points: List<Vector2>
    val segments: Int
    val phiStart: Float
    val phiLength: Float
}

/**
 * ShapeGeometry: 2D shape to 3D geometry
 * Three.js equivalent: THREE.ShapeGeometry
 */
interface ShapeGeometryAPI {
    val shapes: List<Shape>
    val curveSegments: Int
}

/**
 * TubeGeometry: Tube following a 3D curve
 * Three.js equivalent: THREE.TubeGeometry
 */
interface TubeGeometryAPI {
    val path: Curve3D
    val tubularSegments: Int
    val radius: Float
    val radialSegments: Int
    val closed: Boolean
}

/**
 * ParametricGeometry: Geometry from parametric function
 * Three.js equivalent: THREE.ParametricGeometry
 */
interface ParametricGeometryAPI {
    val func: (u: Float, v: Float, target: Vector3) -> Vector3
    val slices: Int
    val stacks: Int
}

/**
 * TextGeometry: 3D text from font
 * Three.js equivalent: THREE.TextGeometry
 */
interface TextGeometryAPI {
    val text: String
    val parameters: TextGeometryParameters
}

data class TextGeometryParameters(
    val font: Font,
    val size: Float = 100f,
    val height: Float = 50f,
    val curveSegments: Int = 12,
    val bevelEnabled: Boolean = false,
    val bevelThickness: Float = 10f,
    val bevelSize: Float = 8f,
    val bevelOffset: Float = 0f,
    val bevelSegments: Int = 3
)

/**
 * EdgesGeometry: Edges of another geometry
 * Three.js equivalent: THREE.EdgesGeometry
 */
interface EdgesGeometryAPI {
    val geometry: BufferGeometry
    val thresholdAngle: Float  // Angle threshold for edge detection (degrees)
}

/**
 * WireframeGeometry: Wireframe of another geometry
 * Three.js equivalent: THREE.WireframeGeometry
 */
interface WireframeGeometryAPI {
    val geometry: BufferGeometry
}

// ============================================================================
// Geometry Utilities
// ============================================================================

/**
 * GeometryUtils: Static utility functions for geometry manipulation
 */
interface GeometryUtilsAPI {
    // Merging
    fun mergeGeometries(
        geometries: List<BufferGeometry>,
        useGroups: Boolean = false
    ): BufferGeometry?

    fun mergeBufferAttributes(
        attributes: List<BufferAttribute>
    ): BufferAttribute?

    // Computation
    fun computeMorphedAttributes(
        object3D: Any  // Mesh or SkinnedMesh
    ): MorphedAttributes

    // Conversion
    fun toTrianglesDrawMode(
        geometry: BufferGeometry,
        drawMode: TrianglesDrawMode
    ): BufferGeometry

    // Simplification
    fun simplifyGeometry(
        geometry: BufferGeometry,
        targetRatio: Float
    ): BufferGeometry
}

data class MorphedAttributes(
    val position: BufferAttribute,
    val normal: BufferAttribute?
)

enum class TrianglesDrawMode {
    TrianglesDrawMode,
    TriangleStripDrawMode,
    TriangleFanDrawMode
}

/**
 * UVGenerator: Generate UV coordinates for extrude/shape geometries
 */
interface UVGenerator {
    fun generateTopUV(
        geometry: BufferGeometry,
        vertices: FloatArray,
        indexA: Int,
        indexB: Int,
        indexC: Int
    ): List<Vector2>

    fun generateSideWallUV(
        geometry: BufferGeometry,
        vertices: FloatArray,
        indexA: Int,
        indexB: Int,
        indexC: Int,
        indexD: Int
    ): List<Vector2>
}

// ============================================================================
// Instanced Geometry
// ============================================================================

/**
 * InstancedBufferGeometry: Geometry with instanced attributes
 * Three.js equivalent: THREE.InstancedBufferGeometry
 */
interface InstancedBufferGeometryAPI : BufferGeometryAPI {
    var instanceCount: Int
    var maxInstanceCount: Int
}

/**
 * InstancedBufferAttribute: Attribute for instanced rendering
 * Three.js equivalent: THREE.InstancedBufferAttribute
 */
interface InstancedBufferAttributeAPI : BufferAttributeAPI {
    var meshPerAttribute: Int  // Number of meshes per attribute instance
}

// ============================================================================
// Supporting Types
// ============================================================================

data class GeometryGroup(
    val start: Int,
    val count: Int,
    val materialIndex: Int = 0
)

data class DrawRange(
    val start: Int,
    val count: Int
)

enum class BufferUsage {
    StaticDraw,
    DynamicDraw,
    StreamDraw,
    StaticRead,
    DynamicRead,
    StreamRead,
    StaticCopy,
    DynamicCopy,
    StreamCopy
}

// ============================================================================
// Factory Functions (Kotlin Idiom)
// ============================================================================

/**
 * DSL builder for BufferGeometry
 */
fun bufferGeometry(init: BufferGeometry.() -> Unit): BufferGeometry {
    val geometry = BufferGeometry()
    geometry.init()
    return geometry
}

/**
 * DSL builder for BufferAttribute from Vector3 list
 */
fun vector3Attribute(vectors: List<Vector3>): BufferAttribute {
    val array = FloatArray(vectors.size * 3)
    vectors.forEachIndexed { index, vector ->
        array[index * 3] = vector.x
        array[index * 3 + 1] = vector.y
        array[index * 3 + 2] = vector.z
    }
    return BufferAttribute(array, itemSize = 3)
}

/**
 * DSL builder for BufferAttribute from Vector2 list
 */
fun vector2Attribute(vectors: List<Vector2>): BufferAttribute {
    val array = FloatArray(vectors.size * 2)
    vectors.forEachIndexed { index, vector ->
        array[index * 2] = vector.x
        array[index * 2 + 1] = vector.y
    }
    return BufferAttribute(array, itemSize = 2)
}

/**
 * Extension function for easy attribute setting
 */
fun BufferGeometry.setPositions(positions: List<Vector3>): BufferGeometry {
    return setAttribute("position", vector3Attribute(positions))
}

fun BufferGeometry.setNormals(normals: List<Vector3>): BufferGeometry {
    return setAttribute("normal", vector3Attribute(normals))
}

fun BufferGeometry.setUVs(uvs: List<Vector2>): BufferGeometry {
    return setAttribute("uv", vector2Attribute(uvs))
}

fun BufferGeometry.setColors(colors: List<Color>): BufferGeometry {
    val array = FloatArray(colors.size * 3)
    colors.forEachIndexed { index, color ->
        array[index * 3] = color.r
        array[index * 3 + 1] = color.g
        array[index * 3 + 2] = color.b
    }
    return setAttribute("color", BufferAttribute(array, itemSize = 3))
}

// ============================================================================
// Usage Examples
// ============================================================================

/**
 * Example: Create custom geometry
 */
fun exampleCustomGeometry(): BufferGeometry {
    return bufferGeometry {
        // Define vertices
        val positions = listOf(
            Vector3(0f, 1f, 0f),   // Top
            Vector3(-1f, -1f, 1f), // Front left
            Vector3(1f, -1f, 1f),  // Front right
            Vector3(0f, -1f, -1f)  // Back
        )

        // Define indices (triangle faces)
        val indices = intArrayOf(
            0, 1, 2,  // Front face
            0, 2, 3,  // Right face
            0, 3, 1,  // Left face
            1, 3, 2   // Bottom face
        )

        // Set attributes
        setPositions(positions)
        setIndex(indices)
        computeVertexNormals()
        computeBoundingSphere()
    }
}

/**
 * Example: Create box geometry with default parameters
 */
fun exampleBoxGeometry(): BoxGeometry {
    return BoxGeometry(width = 1f, height = 1f, depth = 1f)
}

/**
 * Example: Create sphere with custom segments
 */
fun exampleSphereGeometry(): SphereGeometry {
    return SphereGeometry(
        radius = 5f,
        widthSegments = 32,
        heightSegments = 16
    )
}

/**
 * Example: Merge multiple geometries
 */
fun exampleMergeGeometries(geometries: List<BufferGeometry>): BufferGeometry? {
    val utils = GeometryUtils()
    return utils.mergeGeometries(geometries, useGroups = true)
}
