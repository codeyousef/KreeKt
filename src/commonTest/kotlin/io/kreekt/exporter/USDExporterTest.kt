package io.kreekt.exporter

import kotlin.test.Test
import kotlin.test.assertNotNull

class USDExporterTest {
    @Test
    fun testUSDExporterCreation() {
        val exporter = USDExporter()
        assertNotNull(exporter)
    }
}
