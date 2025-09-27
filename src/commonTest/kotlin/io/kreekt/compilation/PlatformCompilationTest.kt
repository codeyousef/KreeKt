package io.kreekt.compilation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Platform-specific compilation validation tests.
 *
 * CRITICAL: These tests MUST FAIL before implementation.
 * Following TDD constitutional requirement - tests first, implementation after.
 */
class PlatformCompilationTest {

    @Test
    fun testPlatformCompilationValidatorContract() {
        // This test must fail until PlatformCompilationValidator is implemented
        assertFailsWith<NotImplementedError> {
            PlatformCompilationValidator.create()
        }
    }

    @Test
    fun testPlatformValidationResultDataClass() {
        // Verify data class is properly defined
        val result = PlatformValidationResult(
            platform = Platform.JVM,
            success = false,
            dependencies = emptyList(),
            nativeLibraries = emptyList(),
            bindingsAvailable = emptyMap(),
            targetJavaVersion = 11,
            bytecodeCompatible = true,
            jniLibraries = emptyList(),
            webAPIs = emptyList(),
            fallbackAPIs = emptyList(),
            jsTarget = "ES2015",
            moduleSystem = "UMD",
            npmDependencies = emptyList(),
            vulkanSupport = VulkanSupport(
                direct = false,
                implementation = "",
                version = "",
                metalTranslation = false
            ),
            windowingSystem = "",
            displaySupport = false,
            architecture = "",
            simulatorTarget = false,
            commonAPIImplemented = false,
            apiInconsistencies = emptyList()
        )
        assertNotNull(result)
        assertEquals(Platform.JVM, result.platform)
    }

    @Test
    fun testDependencyDataClass() {
        val dependency = Dependency(
            name = "lwjgl",
            version = "3.3.3",
            scope = "implementation"
        )
        assertNotNull(dependency)
        assertEquals("lwjgl", dependency.name)
        assertEquals("3.3.3", dependency.version)
    }

    @Test
    fun testVulkanSupportDataClass() {
        val vulkanSupport = VulkanSupport(
            direct = true,
            implementation = "MoltenVK",
            version = "1.3",
            metalTranslation = true
        )
        assertNotNull(vulkanSupport)
        assertEquals(true, vulkanSupport.direct)
        assertEquals("MoltenVK", vulkanSupport.implementation)
    }
}

// Data classes and interfaces that MUST be implemented
data class PlatformValidationResult(
    val platform: Platform,
    val success: Boolean,
    val dependencies: List<Dependency>,
    val nativeLibraries: List<String>,
    val bindingsAvailable: Map<String, Boolean>,
    val targetJavaVersion: Int,
    val bytecodeCompatible: Boolean,
    val jniLibraries: List<JNILibrary>,
    val webAPIs: List<String>,
    val fallbackAPIs: List<String>,
    val jsTarget: String,
    val moduleSystem: String,
    val npmDependencies: List<NPMDependency>,
    val vulkanSupport: VulkanSupport,
    val windowingSystem: String,
    val displaySupport: Boolean,
    val architecture: String,
    val simulatorTarget: Boolean,
    val commonAPIImplemented: Boolean,
    val apiInconsistencies: List<String>
)

data class Dependency(
    val name: String,
    val version: String,
    val scope: String
)

data class JNILibrary(
    val name: String,
    val platform: Platform,
    val loadable: Boolean
)

data class NPMDependency(
    val name: String,
    val version: String,
    val resolved: Boolean
)

data class VulkanSupport(
    val direct: Boolean,
    val implementation: String,
    val version: String,
    val metalTranslation: Boolean
)

data class ExpectActualValidationResult(
    val platform: Platform,
    val success: Boolean,
    val missingActuals: List<String>,
    val missingExpects: List<String>,
    val orphanedActuals: List<String>
)

data class MathLibraryConsistencyResult(
    val platform: Platform,
    val vectorOpsConsistent: Boolean,
    val matrixOpsConsistent: Boolean,
    val quaternionOpsConsistent: Boolean,
    val inconsistencies: List<String>
)

interface PlatformCompilationValidator {
    suspend fun validatePlatform(platform: Platform): PlatformValidationResult
    suspend fun validateExpectActual(platform: Platform): ExpectActualValidationResult
    suspend fun validateMathLibraryConsistency(platform: Platform): MathLibraryConsistencyResult

    companion object {
        fun create(): PlatformCompilationValidator {
            // This MUST fail until implementation is provided
            throw NotImplementedError("PlatformCompilationValidator implementation required")
        }
    }
}