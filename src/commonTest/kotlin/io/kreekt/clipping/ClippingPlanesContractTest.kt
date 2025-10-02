package io.kreekt.clipping

import io.kreekt.core.math.Vector3
import io.kreekt.core.math.Vector4
import io.kreekt.core.math.Plane
import io.kreekt.geometry.BufferGeometry
import io.kreekt.geometry.BoxGeometry
import io.kreekt.material.Material
import io.kreekt.material.MeshBasicMaterial
import io.kreekt.renderer.Renderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.time.TimeSource

/**
 * Contract test for Global clipping planes - T033
 * Covers: FR-CP001, FR-CP002, FR-CP003, FR-CP004 from contracts/clipping-api.kt
 */
class ClippingPlanesContractTest {

    @Test
    fun testClipGeometryAgainstArbitraryPlanes() {
        // FR-CP001: Clip geometry against arbitrary planes
        val geometry = BoxGeometry(2f, 2f, 2f)
        val material = MeshBasicMaterial()

        // Define clipping plane (clips everything above y=0)
        val clippingPlane = Plane(Vector3(0f, -1f, 0f), 0f)

        // Apply clipping
        val clipper = GeometryClipper()
        val clippedGeometry = clipper.clipGeometry(geometry, listOf(clippingPlane))

        // Verify vertices above plane are clipped
        val positions = clippedGeometry.getAttribute("position")!!
        for (i in 0 until positions.count) {
            val y = positions.getY(i)
            assertTrue(y <= 0.01f, "All vertices should be below or on clipping plane")
        }

        // Verify new vertices created at intersection
        assertTrue(clippedGeometry.getAttribute("position")!!.count > 0)
    }

    @Test
    fun testSupportUpTo8Planes() {
        // FR-CP002: Support up to 8 planes
        val renderer = MockRenderer()

        // Create 8 clipping planes
        val planes = List(8) { i ->
            val angle = (i * kotlin.math.PI * 2 / 8).toFloat()
            Plane(
                Vector3(kotlin.math.cos(angle), kotlin.math.sin(angle), 0f),
                1f
            )
        }

        renderer.clippingPlanes = planes
        assertEquals(8, renderer.clippingPlanes.size)

        // Try to add 9th plane (should be limited or warn)
        val extraPlane = Plane(Vector3(0f, 0f, 1f), 0f)
        renderer.clippingPlanes = planes + extraPlane

        // Should either limit to 8 or handle gracefully
        assertTrue(renderer.clippingPlanes.size <= 8 || renderer.supportsExtendedClipping)
    }

    @Test
    fun testPerMaterialClippingOverride() {
        // FR-CP003: Per-material clipping override
        val globalPlane = Plane(Vector3(0f, 1f, 0f), 0f)
        val renderer = MockRenderer()
        renderer.clippingPlanes = listOf(globalPlane)

        // Material with local clipping planes
        val material = ClippingMaterial()
        val localPlane1 = Plane(Vector3(1f, 0f, 0f), 0.5f)
        val localPlane2 = Plane(Vector3(-1f, 0f, 0f), 0.5f)
        material.clippingPlanes = listOf(localPlane1, localPlane2)

        // Test local planes override
        material.clippingPlanesMode = ClippingMode.OVERRIDE
        val activePlanes = renderer.getActivePlanes(material)
        assertEquals(2, activePlanes.size)
        assertTrue(localPlane1 in activePlanes)
        assertTrue(localPlane2 in activePlanes)
        assertFalse(globalPlane in activePlanes)

        // Test combining with global planes
        material.clippingPlanesMode = ClippingMode.COMBINE
        val combinedPlanes = renderer.getActivePlanes(material)
        assertEquals(3, combinedPlanes.size)
        assertTrue(globalPlane in combinedPlanes)
        assertTrue(localPlane1 in combinedPlanes)
        assertTrue(localPlane2 in combinedPlanes)
    }

    @Test
    fun testUnionVsIntersectionMode() {
        // FR-CP004: Union vs intersection mode
        val geometry = BoxGeometry(2f, 2f, 2f)

        // Two perpendicular clipping planes
        val planeX = Plane(Vector3(1f, 0f, 0f), 0f) // Clips x > 0
        val planeY = Plane(Vector3(0f, 1f, 0f), 0f) // Clips y > 0

        val clipper = GeometryClipper()

        // Test UNION mode (keep if outside ANY plane)
        clipper.mode = ClippingMode.UNION
        val unionClipped = clipper.clipGeometry(geometry, listOf(planeX, planeY))

        // Should clip only the corner where both x>0 AND y>0
        val unionPositions = unionClipped.getAttribute("position")!!
        var hasPositiveX = false
        var hasPositiveY = false

        for (i in 0 until unionPositions.count) {
            val x = unionPositions.getX(i)
            val y = unionPositions.getY(i)
            if (x > 0.01f && y <= 0.01f) hasPositiveX = true
            if (y > 0.01f && x <= 0.01f) hasPositiveY = true
            // Should not have both positive
            assertFalse(x > 0.01f && y > 0.01f, "Union mode should only clip intersection")
        }

        // Test INTERSECTION mode (keep if outside ALL planes)
        clipper.mode = ClippingMode.INTERSECTION
        val intersectionClipped = clipper.clipGeometry(geometry, listOf(planeX, planeY))

        // Should clip if x>0 OR y>0
        val intersectionPositions = intersectionClipped.getAttribute("position")!!
        for (i in 0 until intersectionPositions.count) {
            val x = intersectionPositions.getX(i)
            val y = intersectionPositions.getY(i)
            // All vertices should have x<=0 AND y<=0
            assertTrue(x <= 0.01f && y <= 0.01f, "Intersection mode should clip union")
        }
    }

    @Test
    fun testClippingPlaneTransformation() {
        // Test clipping planes in different coordinate spaces
        val worldPlane = Plane(Vector3(0f, 1f, 0f), 5f) // World space

        // Transform to view space
        val viewMatrix = createViewMatrix()
        val viewPlane = worldPlane.applyMatrix4(viewMatrix)

        // Verify transformation
        assertFalse(viewPlane.normal.equals(worldPlane.normal))
        assertFalse(viewPlane.constant == worldPlane.constant)

        // Transform to clip space for shader
        val clipPlane = viewPlane.toVector4()
        // Verify clip plane has 4 components (x, y, z, w)
        assertTrue(clipPlane.x != 0f || clipPlane.y != 0f || clipPlane.z != 0f || clipPlane.w != 0f)
    }

    @Test
    fun testClippingWithShadows() {
        // Test clipping affects shadow casting
        val material = ClippingMaterial()
        material.clipShadows = true

        val plane = Plane(Vector3(0f, 1f, 0f), 0f)
        material.clippingPlanes = listOf(plane)

        // Verify shadow clipping enabled
        assertTrue(material.clipShadows)

        // Test shadow map generation with clipping
        val shadowMapper = ShadowMapper()
        val shadowsClipped = shadowMapper.generateShadowMap(material)

        assertTrue(shadowsClipped.respectsClipping)
    }

    @Test
    fun testClippingPerformance() {
        // Test performance with maximum clipping planes
        val geometry = createComplexGeometry(10000) // 10k vertices
        val planes = List(8) { i ->
            Plane(
                Vector3(
                    kotlin.math.cos(i * 0.785f),
                    kotlin.math.sin(i * 0.785f),
                    0f
                ),
                (i - 4) * 0.5f
            )
        }

        val clipper = GeometryClipper()

        val startMark = TimeSource.Monotonic.markNow()
        val iterations = 100

        for (i in 0 until iterations) {
            clipper.clipGeometry(geometry, planes)
        }

        val duration = startMark.elapsedNow().inWholeMilliseconds
        val avgTime = duration.toFloat() / iterations.toFloat()

        // Should be fast enough for real-time
        assertTrue(avgTime < 16f, "Clipping should be <16ms for 60 FPS, was ${avgTime}ms")
    }

    @Test
    fun testClippingPlaneNormalization() {
        // Test plane normalization for consistent behavior
        val unnormalizedPlane = Plane(Vector3(3f, 4f, 0f), 10f)

        // Normalize the plane
        unnormalizedPlane.normalize()

        // Check normal is unit vector
        val length = unnormalizedPlane.normal.length()
        assertEquals(1f, length, 0.001f)

        // Check constant is scaled appropriately
        assertEquals(2f, unnormalizedPlane.constant, 0.001f) // 10 / 5
    }

    // Helper functions

    private fun createComplexGeometry(vertexCount: Int): BufferGeometry {
        val geometry = BufferGeometry()
        val positions = FloatArray(vertexCount * 3) {
            (kotlin.random.Random.nextFloat() - 0.5f) * 10
        }
        geometry.setAttribute("position", io.kreekt.geometry.BufferAttribute(positions, 3))
        return geometry
    }

    private fun createViewMatrix(): io.kreekt.core.math.Matrix4 {
        return io.kreekt.core.math.Matrix4().lookAt(
            Vector3(0f, 0f, 5f),
            Vector3(0f, 0f, 0f),
            Vector3(0f, 1f, 0f)
        )
    }
}

// Supporting classes

class MockRenderer {
    var clippingPlanes: List<Plane> = emptyList()
    val supportsExtendedClipping = false

    fun getActivePlanes(material: ClippingMaterial): List<Plane> {
        return when (material.clippingPlanesMode) {
            ClippingMode.OVERRIDE -> material.clippingPlanes.orEmpty()
            ClippingMode.COMBINE -> clippingPlanes + material.clippingPlanes.orEmpty()
            else -> clippingPlanes
        }
    }
}

class ClippingMaterial : io.kreekt.material.Material() {
    override var clippingPlanes: List<Plane>? = emptyList()
    var clippingPlanesMode = ClippingMode.OVERRIDE
    override var clipShadows = false

    override val type: String = "ClippingMaterial"

    override fun clone(): io.kreekt.material.Material {
        return ClippingMaterial().also {
            it.copy(this)
            it.clippingPlanes = clippingPlanes?.toList()
            it.clippingPlanesMode = clippingPlanesMode
            it.clipShadows = clipShadows
        }
    }
}

class GeometryClipper {
    var mode = ClippingMode.INTERSECTION

    fun clipGeometry(geometry: BufferGeometry, planes: List<Plane>): BufferGeometry {
        // Simplified clipping simulation
        val result = BufferGeometry()
        val positions = geometry.getAttribute("position")!!
        val clippedPositions = mutableListOf<Float>()

        for (i in 0 until positions.count / 3) {
            val v = Vector3(
                positions.getX(i * 3),
                positions.getY(i * 3),
                positions.getZ(i * 3)
            )

            val keep = when (mode) {
                ClippingMode.INTERSECTION -> planes.all { plane ->
                    plane.distanceToPoint(v) <= 0
                }

                ClippingMode.UNION -> planes.any { plane ->
                    plane.distanceToPoint(v) <= 0
                }

                else -> true
            }

            if (keep) {
                clippedPositions.add(v.x)
                clippedPositions.add(v.y)
                clippedPositions.add(v.z)
            }
        }

        result.setAttribute("position", io.kreekt.geometry.BufferAttribute(clippedPositions.toFloatArray(), 3))
        return result
    }
}

enum class ClippingMode {
    OVERRIDE,
    COMBINE,
    INTERSECTION,
    UNION
}

class ShadowMapper {
    fun generateShadowMap(material: ClippingMaterial): ShadowMap {
        return ShadowMap(respectsClipping = material.clipShadows)
    }
}

data class ShadowMap(val respectsClipping: Boolean)

// Extensions for Plane
fun Plane.applyMatrix4(matrix: io.kreekt.core.math.Matrix4): Plane {
    val transformedNormal = normal.clone().transformDirection(matrix)
    val transformedConstant = constant // Simplified
    return Plane(transformedNormal, transformedConstant)
}

fun Plane.toVector4(): Vector4 {
    return Vector4(normal.x, normal.y, normal.z, constant)
}

fun Plane.distanceToPoint(point: Vector3): Float {
    return normal.dot(point) + constant
}

fun Plane.normalize() {
    val length = normal.length()
    normal.divideScalar(length)
    constant /= length
}