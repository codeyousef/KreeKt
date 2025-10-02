package io.kreekt.loader

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class STLLoaderTest {
    @Test
    fun testSTLLoaderCreation() = runTest {
        val loader = STLLoader()
        assertNotNull(loader)
    }
}
