package io.kreekt.helpers

import kotlin.test.Test
import kotlin.test.assertNotNull

class SkeletonHelperTest {
    @Test
    fun testSkeletonHelperCreation() {
        val helper = SkeletonHelper()
        assertNotNull(helper)
    }
}
