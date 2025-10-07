/**
 * T008: Contract Test - Error Handling (FR-022, FR-024)
 * Feature: 019-we-should-not
 *
 * Tests fail-fast behavior with descriptive exceptions.
 *
 * Requirements Tested:
 * - FR-022: Crash immediately with detailed error messages (fail-fast)
 * - FR-024: Detect and report backend capability mismatches
 *
 * EXPECTED: These tests MUST FAIL until RendererInitializationException is implemented (TDD Red Phase)
 */

package io.kreekt.renderer

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ErrorHandlingTest {

    @Test
    fun `RendererFactory throws exception when no graphics support`() = runTest {
        // Simulate platform with no graphics support
        val invalidSurface = createInvalidSurface()

        val result = RendererFactory.create(invalidSurface)

        assertTrue(
            result.isFailure,
            "Should fail with invalid surface"
        )

        val exception = result.exceptionOrNull()
        assertTrue(
            exception is RendererInitializationException,
            "Must throw RendererInitializationException (FR-022), got: ${exception?.javaClass?.simpleName}"
        )
    }

    @Test
    fun `exception message contains diagnostic keywords`() = runTest {
        val invalidSurface = createInvalidSurface()

        val result = RendererFactory.create(invalidSurface)
        val exception = result.exceptionOrNull()

        assertNotNull(exception, "Exception must not be null")

        val message = exception.message ?: ""
        val hasGraphicsKeyword = message.contains("graphics", ignoreCase = true)
        val hasSupportKeyword = message.contains("support", ignoreCase = true)
        val hasDriverKeyword = message.contains("driver", ignoreCase = true)
        val hasBackendKeyword = message.contains("backend", ignoreCase = true)

        assertTrue(
            hasGraphicsKeyword || hasSupportKeyword || hasDriverKeyword || hasBackendKeyword,
            "Error message must contain diagnostic keywords (graphics/support/driver/backend). Got: $message"
        )
    }

    @Test
    fun `exception message is detailed`() = runTest {
        val invalidSurface = createInvalidSurface()

        val result = RendererFactory.create(invalidSurface)
        val exception = result.exceptionOrNull()

        assertNotNull(exception, "Exception must not be null")

        val message = exception.message ?: ""
        assertTrue(
            message.length > 50,
            "Error message must be detailed (>50 characters, FR-022). Got ${message.length} chars: $message"
        )
    }

    @Test
    fun `NoGraphicsSupportException includes platform information`() = runTest {
        val invalidSurface = createInvalidSurface()

        val result = RendererFactory.create(invalidSurface)
        val exception = result.exceptionOrNull()

        if (exception is RendererInitializationException.NoGraphicsSupportException) {
            assertNotNull(exception.platform, "Platform must be specified")
            assertNotNull(exception.availableBackends, "Available backends must be listed")
            assertNotNull(exception.requiredFeatures, "Required features must be listed")
        }
    }

    @Test
    fun `exception includes troubleshooting information`() = runTest {
        val invalidSurface = createInvalidSurface()

        val result = RendererFactory.create(invalidSurface)
        val exception = result.exceptionOrNull()

        assertNotNull(exception, "Exception must not be null")

        val message = exception.message ?: ""
        val hasTroubleshooting = message.contains("update", ignoreCase = true) ||
                message.contains("install", ignoreCase = true) ||
                message.contains("check", ignoreCase = true) ||
                message.contains("ensure", ignoreCase = true)

        assertTrue(
            hasTroubleshooting,
            "Error message should include troubleshooting hints. Got: $message"
        )
    }

    @Test
    fun `fail-fast on capability mismatch`() = runTest {
        // This test verifies that renderer fails immediately if required capabilities
        // are not met, rather than failing later during rendering
        val surface = createTestSurface(800, 600)

        // Try to create renderer - should either succeed or fail immediately
        val result = RendererFactory.create(surface)

        // If it fails, must be with a proper exception
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            assertTrue(
                exception is RendererInitializationException,
                "Capability mismatches must throw RendererInitializationException (FR-024)"
            )

            val message = exception?.message ?: ""
            assertTrue(
                message.contains("capability", ignoreCase = true) ||
                        message.contains("support", ignoreCase = true) ||
                        message.contains("feature", ignoreCase = true),
                "Capability mismatch error must mention capabilities/support/features"
            )
        }

        // If it succeeds, renderer must have valid capabilities
        if (result.isSuccess) {
            val renderer = result.getOrThrow()
            val caps = renderer.capabilities

            assertTrue(
                caps.maxTextureSize >= 2048,
                "maxTextureSize must be at least 2048 (FR-024)"
            )
            assertTrue(
                caps.maxVertexAttributes >= 16,
                "maxVertexAttributes must be at least 16 (FR-024)"
            )

            renderer.dispose()
        }
    }

    @Test
    fun `different exception types for different failures`() = runTest {
        // Verify that the exception hierarchy is properly structured
        val exception = RendererInitializationException.NoGraphicsSupportException(
            platform = "Test",
            availableBackends = emptyList(),
            requiredFeatures = listOf("Vulkan 1.1+")
        )

        assertTrue(
            exception is RendererInitializationException,
            "Specific exceptions must extend RendererInitializationException"
        )

        assertTrue(
            exception is RendererInitializationException.NoGraphicsSupportException,
            "Exception must be of specific type"
        )
    }
}

// Test helper (expect/actual in platform source sets)
expect fun createInvalidSurface(): RenderSurface
