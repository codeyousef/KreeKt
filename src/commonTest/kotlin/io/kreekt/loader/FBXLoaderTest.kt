package io.kreekt.loader

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class FBXLoaderTest {
    @Test
    fun testFBXLoaderCreation() = runTest {
        val loader = FBXLoader()
        assertNotNull(loader)
    }
}
