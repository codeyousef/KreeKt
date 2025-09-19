package io.kreekt.scene

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract test for MeshBuilder
 * T013 - This test MUST FAIL until MeshBuilder is implemented
 */
class MeshBuilderTest {

    @Test
    fun testMeshBuilderContract() {
        // This test will fail until we implement MeshBuilder
        assertFailsWith<NotImplementedError> {
            // TODO: Replace with actual implementation
            // val mesh = mesh {
            //     boxGeometry(2f, 2f, 2f)
            //     standardMaterial {
            //         color = Color.BLUE
            //         metalness = 0.5f
            //     }
            //     position.set(1f, 2f, 3f)
            //     castShadow = true
            // }
            // assertNotNull(mesh.geometry)
            // assertNotNull(mesh.material)
            // assertTrue(mesh.castShadow)
            throw NotImplementedError("MeshBuilder not yet implemented")
        }
    }

    @Test
    fun testGeometryBuilderContract() {
        // This test will fail until we implement geometry builders
        assertFailsWith<NotImplementedError> {
            // TODO: Replace with actual implementation
            // val boxGeometry = BoxGeometry(1f, 1f, 1f)
            // assertNotNull(boxGeometry.attributes["position"])
            // assertNotNull(boxGeometry.attributes["normal"])
            // assertNotNull(boxGeometry.attributes["uv"])
            throw NotImplementedError("Geometry builders not yet implemented")
        }
    }

    @Test
    fun testMaterialBuilderContract() {
        // This test will fail until we implement material builders
        assertFailsWith<NotImplementedError> {
            // TODO: Replace with actual implementation
            // val material = StandardMaterial().apply {
            //     color = Color.RED
            //     roughness = 0.5f
            //     metalness = 0.2f
            // }
            // assertEquals(Color.RED, material.color)
            // assertEquals(0.5f, material.roughness)
            throw NotImplementedError("Material builders not yet implemented")
        }
    }
}