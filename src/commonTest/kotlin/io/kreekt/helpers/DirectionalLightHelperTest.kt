package io.kreekt.helpers

import kotlin.test.Test
import kotlin.test.assertNotNull

class DirectionalLightHelperTest {
    @Test
    fun testDirectionalLightHelperCreation() {
        val helper = DirectionalLightHelper()
        assertNotNull(helper)
    }
}
