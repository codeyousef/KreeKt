package io.kreekt.loader

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class KTX2LoaderTest {
    @Test
    fun testKTX2LoaderCreation() = runTest {
        val loader = KTX2Loader()
        assertNotNull(loader)
    }
}
