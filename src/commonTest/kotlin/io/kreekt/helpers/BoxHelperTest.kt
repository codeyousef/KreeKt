package io.kreekt.helpers

import kotlin.test.Test
import kotlin.test.assertNotNull

class BoxHelperTest {
    @Test
    fun testBoxHelperCreation() {
        val helper = BoxHelper()
        assertNotNull(helper)
    }
}
