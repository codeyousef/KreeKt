package io.kreekt.helpers

import kotlin.test.Test
import kotlin.test.assertNotNull

class CameraHelperTest {
    @Test
    fun testCameraHelperCreation() {
        val helper = CameraHelper()
        assertNotNull(helper)
    }
}
