package io.kreekt.loader

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class ColladaLoaderTest {
    @Test
    fun testColladaLoaderCreation() = runTest {
        val loader = ColladaLoader()
        assertNotNull(loader)
    }
}
