package io.kreekt.exporter

import kotlin.test.Test
import kotlin.test.assertNotNull

class GLTFExporterTest {
    @Test
    fun testGLTFExporterCreation() {
        val exporter = GLTFExporter()
        assertNotNull(exporter)
    }
}
