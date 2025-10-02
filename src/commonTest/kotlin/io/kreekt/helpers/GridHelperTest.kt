package io.kreekt.helpers

import kotlin.test.Test
import kotlin.test.assertNotNull

class GridHelperTest {
    @Test
    fun testGridHelperCreation() {
        val helper = GridHelper()
        assertNotNull(helper)
    }
}
