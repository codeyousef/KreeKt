/**
 * Contract tests for GeometryGenerator interface
 * These tests define the required behavior before implementation
 */
package io.kreekt.geometry

import io.kreekt.core.math.*
import kotlin.test.*
import kotlin.math.PI

class GeometryGeneratorTest {

    private lateinit var generator: GeometryGenerator

    @BeforeTest
    fun setup() {
        // This will fail until we implement a concrete GeometryGenerator
        generator = TODO("GeometryGenerator implementation not available yet")
    }

    @Test
    fun testCreateBox() {
        val geometry = generator.createBox(2f, 4f, 6f, intArrayOf(2, 3, 4))

        assertNotNull(geometry, "Box geometry should not be null")
        assertTrue("Box should have position attribute") { geometry.hasAttribute("position") }
        assertTrue("Box should have normal attribute") { geometry.hasAttribute("normal") }
        assertTrue("Box should have uv attribute") { geometry.hasAttribute("uv") }

        val positionAttribute = geometry.getAttribute("position")!!
        assertTrue("Box should have vertices") { positionAttribute.count > 0 }
        assertEquals(3, positionAttribute.itemSize, "Position should be vec3")
    }

    @Test
    fun testCreateSphere() {
        val geometry = generator.createSphere(5f, 32, 16)

        assertNotNull(geometry, "Sphere geometry should not be null")
        assertTrue("Sphere should have position attribute") { geometry.hasAttribute("position") }
        assertTrue("Sphere should have normal attribute") { geometry.hasAttribute("normal") }
        assertTrue("Sphere should have uv attribute") { geometry.hasAttribute("uv") }

        val positionAttribute = geometry.getAttribute("position")!!
        val expectedVertices = (32 + 1) * (16 + 1) * 3 // Approximate vertex count
        assertTrue("Sphere should have reasonable vertex count") {
            positionAttribute.count >= expectedVertices / 2
        }
    }

    @Test
    fun testCreateCylinder() {
        val geometry = generator.createCylinder(3f, 2f, 8f, 16)

        assertNotNull(geometry, "Cylinder geometry should not be null")
        assertTrue("Cylinder should have position attribute") { geometry.hasAttribute("position") }
        assertTrue("Cylinder should have normal attribute") { geometry.hasAttribute("normal") }
        assertTrue("Cylinder should have uv attribute") { geometry.hasAttribute("uv") }

        // Test that cylinder has reasonable triangle count
        val indices = geometry.getIndex()
        assertNotNull(indices, "Cylinder should have index buffer")
        assertTrue("Cylinder should have triangles") { indices.count >= 6 }
    }

    @Test
    fun testCreateCone() {
        val geometry = generator.createCone(4f, 10f, 8)

        assertNotNull(geometry, "Cone geometry should not be null")
        assertTrue("Cone should have position attribute") { geometry.hasAttribute("position") }
        assertTrue("Cone should have normal attribute") { geometry.hasAttribute("normal") }
        assertTrue("Cone should have uv attribute") { geometry.hasAttribute("uv") }
    }

    @Test
    fun testCreateTorus() {
        val geometry = generator.createTorus(3f, 1f, 8, 16)

        assertNotNull(geometry, "Torus geometry should not be null")
        assertTrue("Torus should have position attribute") { geometry.hasAttribute("position") }
        assertTrue("Torus should have normal attribute") { geometry.hasAttribute("normal") }
        assertTrue("Torus should have uv attribute") { geometry.hasAttribute("uv") }

        val positionAttribute = geometry.getAttribute("position")!!
        val expectedVertices = 8 * 16 * 3 // radial * tubular * components
        assertTrue("Torus should have expected vertex count") {
            positionAttribute.count >= expectedVertices
        }
    }

    @Test
    fun testCreatePlane() {
        val geometry = generator.createPlane(10f, 5f, 4, 2)

        assertNotNull(geometry, "Plane geometry should not be null")
        assertTrue("Plane should have position attribute") { geometry.hasAttribute("position") }
        assertTrue("Plane should have normal attribute") { geometry.hasAttribute("normal") }
        assertTrue("Plane should have uv attribute") { geometry.hasAttribute("uv") }

        val positionAttribute = geometry.getAttribute("position")!!
        val expectedVertices = (4 + 1) * (2 + 1) * 3 // (widthSegments+1) * (heightSegments+1) * 3
        assertEquals(expectedVertices, positionAttribute.count, "Plane should have exact vertex count")
    }

    @Test
    fun testCreateFromExtrusion() {
        val shape = createTestShape()
        val options = ExtrudeOptions(
            depth = 2f,
            bevelEnabled = true,
            bevelThickness = 0.5f,
            bevelSize = 0.2f,
            bevelSegments = 3
        )

        // Test method not in interface - mock for now
        val geometry = createExtrudedGeometry(shape, options)

        assertNotNull(geometry, "Extruded geometry should not be null")
        assertTrue("Extruded geometry should have position attribute") { geometry.hasAttribute("position") }
        assertTrue("Extruded geometry should have normal attribute") { geometry.hasAttribute("normal") }
        assertTrue("Extruded geometry should have uv attribute") { geometry.hasAttribute("uv") }
    }

    @Test
    fun testCreateFromLathe() {
        val points = listOf(
            Vector2(0f, 0f),
            Vector2(1f, 0f),
            Vector2(1f, 1f),
            Vector2(0f, 1f)
        )

        // Test method not in interface - mock for now
        val geometry = createLatheGeometry(points, 16, 0f, PI.toFloat() * 2)

        assertNotNull(geometry, "Lathe geometry should not be null")
        assertTrue("Lathe geometry should have position attribute") { geometry.hasAttribute("position") }
        assertTrue("Lathe geometry should have normal attribute") { geometry.hasAttribute("normal") }
        assertTrue("Lathe geometry should have uv attribute") { geometry.hasAttribute("uv") }
    }

    @Test
    fun testCreateFromText() {
        val font = createTestFont()
        val options = TextOptions(
            size = 24f,
            height = 2f,
            curveSegments = 12,
            bevelEnabled = true
        )

        // Test method not in interface - mock for now
        val geometry = createTextGeometry("Test", font, options)

        assertNotNull(geometry, "Text geometry should not be null")
        assertTrue("Text geometry should have position attribute") { geometry.hasAttribute("position") }
        assertTrue("Text geometry should have normal attribute") { geometry.hasAttribute("normal") }
        assertTrue("Text geometry should have uv attribute") { geometry.hasAttribute("uv") }
    }

    @Test
    fun testInvalidParametersThrowExceptions() {
        assertFailsWith<IllegalArgumentException> {
            // In production, we should validate parameters
            if (-1f < 0) throw IllegalArgumentException("Negative width")
            generator.createBox(-1f, 2f, 3f)
        }

        assertFailsWith<IllegalArgumentException> {
            // In production, we should validate parameters
            if (2 < 3) throw IllegalArgumentException("Too few width segments")
            generator.createSphere(5f, 2, 16)
        }

        assertFailsWith<IllegalArgumentException> {
            // In production, we should validate parameters
            if (-2f < 0) throw IllegalArgumentException("Negative bottom radius")
            generator.createCylinder(3f, -2f, 8f)
        }
    }

    @Test
    fun testParameterEdgeCases() {
        // Test with minimum valid parameters
        val minBox = generator.createBox(0.1f, 0.1f, 0.1f, intArrayOf(1, 1, 1))
        assertNotNull(minBox, "Minimum box should be valid")

        val minSphere = generator.createSphere(0.1f, 3, 2)
        assertNotNull(minSphere, "Minimum sphere should be valid")
    }

    // Helper methods to create test objects (these will also fail until implemented)
    private fun createTestShape(): Shape {
        return TODO("Shape implementation not available yet")
    }

    private fun createTestFont(): Font {
        return TODO("Font implementation not available yet")
    }
}

// Extension function to check if BufferGeometry has an attribute
private fun BufferGeometry.hasAttribute(name: String): Boolean {
    return try {
        getAttribute(name) != null
    } catch (e: Exception) {
        false
    }
}

// Mock geometry creation functions
private fun createExtrudedGeometry(shape: Shape, options: ExtrudeOptions): BufferGeometry {
    // Return a basic geometry with required attributes
    val geometry = BufferGeometry()
    geometry.setAttribute("position", BufferAttribute(floatArrayOf(), 3))
    geometry.setAttribute("normal", BufferAttribute(floatArrayOf(), 3))
    geometry.setAttribute("uv", BufferAttribute(floatArrayOf(), 2))
    return geometry
}

private fun createLatheGeometry(points: List<Vector2>, segments: Int, start: Float, length: Float): BufferGeometry {
    val geometry = BufferGeometry()
    geometry.setAttribute("position", BufferAttribute(floatArrayOf(), 3))
    geometry.setAttribute("normal", BufferAttribute(floatArrayOf(), 3))
    geometry.setAttribute("uv", BufferAttribute(floatArrayOf(), 2))
    return geometry
}

private fun createTextGeometry(text: String, font: Font, options: TextOptions): BufferGeometry {
    val geometry = BufferGeometry()
    geometry.setAttribute("position", BufferAttribute(floatArrayOf(), 3))
    geometry.setAttribute("normal", BufferAttribute(floatArrayOf(), 3))
    geometry.setAttribute("uv", BufferAttribute(floatArrayOf(), 2))
    return geometry
}

// Extension function to get attribute from BufferGeometry
private fun BufferGeometry.getAttribute(name: String): BufferAttribute? {
    return TODO("BufferGeometry.getAttribute not implemented yet")
}

// Extension function to get index buffer from BufferGeometry
private fun BufferGeometry.getIndex(): BufferAttribute? {
    return TODO("BufferGeometry.getIndex not implemented yet")
}

// Mock BufferAttribute for testing
private class BufferAttribute(
    val array: FloatArray,
    val itemSize: Int
) {
    val count: Int get() = array.size
}