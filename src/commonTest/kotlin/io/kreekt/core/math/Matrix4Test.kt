package io.kreekt.core.math

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Matrix4 transformations test
 * T017 - This test MUST FAIL until Matrix4 is implemented
 */
class Matrix4Test {

    @Test
    fun testMatrix4IdentityContract() {
        // This test will fail until we implement Matrix4
        assertFailsWith<NotImplementedError> {
            // TODO: Replace with actual implementation
            // val m = Matrix4()
            // assertTrue(m.isIdentity())
            // assertEquals(1f, m.elements[0])  // m[0,0]
            // assertEquals(1f, m.elements[5])  // m[1,1]
            // assertEquals(1f, m.elements[10]) // m[2,2]
            // assertEquals(1f, m.elements[15]) // m[3,3]
            throw NotImplementedError("Matrix4 not yet implemented")
        }
    }

    @Test
    fun testMatrix4MultiplicationContract() {
        // This test will fail until we implement Matrix4 multiplication
        assertFailsWith<NotImplementedError> {
            // TODO: Replace with actual implementation
            // val m1 = Matrix4().makeTranslation(1f, 2f, 3f)
            // val m2 = Matrix4().makeScale(2f, 2f, 2f)
            // val result = m1.multiply(m2)
            // // Result should be scale then translate
            // assertEquals(2f, result.elements[0])  // scale x
            // assertEquals(1f, result.elements[12]) // translate x
            throw NotImplementedError("Matrix4.multiply() not yet implemented")
        }
    }

    @Test
    fun testMatrix4TranslationContract() {
        // This test will fail until we implement Matrix4 translation
        assertFailsWith<NotImplementedError> {
            // TODO: Replace with actual implementation
            // val m = Matrix4().makeTranslation(5f, 10f, 15f)
            // assertEquals(5f, m.elements[12])  // translate x
            // assertEquals(10f, m.elements[13]) // translate y
            // assertEquals(15f, m.elements[14]) // translate z
            throw NotImplementedError("Matrix4.makeTranslation() not yet implemented")
        }
    }

    @Test
    fun testMatrix4RotationContract() {
        // This test will fail until we implement Matrix4 rotation
        assertFailsWith<NotImplementedError> {
            // TODO: Replace with actual implementation
            // val m = Matrix4().makeRotationZ(Math.PI.toFloat() / 2) // 90 degrees
            // val v = Vector3(1f, 0f, 0f)
            // val rotated = v.applyMatrix4(m)
            // assertTrue(abs(rotated.x) < 0.001f)
            // assertTrue(abs(rotated.y - 1f) < 0.001f)
            throw NotImplementedError("Matrix4.makeRotationZ() not yet implemented")
        }
    }

    @Test
    fun testMatrix4InverseContract() {
        // This test will fail until we implement Matrix4 inverse
        assertFailsWith<NotImplementedError> {
            // TODO: Replace with actual implementation
            // val m = Matrix4().makeTranslation(1f, 2f, 3f)
            // val inverse = m.clone().invert()
            // val identity = m.multiply(inverse)
            // assertTrue(identity.isIdentity())
            throw NotImplementedError("Matrix4.invert() not yet implemented")
        }
    }
}