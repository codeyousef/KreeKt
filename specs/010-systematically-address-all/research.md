# Research: Complete Implementation of All Unfinished Functionality

**Feature**: 010-systematically-address-all
**Date**: 2025-09-30
**Purpose**: Research technical approaches for completing all stub implementations and fixing failing tests

## Executive Summary

This research document consolidates findings for systematically completing all unfinished implementations in the KreeKt codebase. Unlike typical features requiring extensive research into unknowns, this effort leverages existing patterns and interfaces already present in the codebase. The primary research focus is identifying the most efficient implementation patterns to follow for consistency.

**Key Finding**: All required patterns already exist in the codebase. Implementation involves following established templates rather than discovering new approaches.

## 1. File System I/O Patterns

### Decision

Use the existing `FileSystem` expect/actual pattern with platform-specific implementations:
- **JVM**: `java.io.File` with `kotlinx.coroutines.Dispatchers.IO`
- **JS**: Node.js `fs` module (Node.js environment) or stub implementations (browser environment)
- **Native**: Platform-specific file APIs (Linux: POSIX, Windows: Win32 via kotlinx.cinterop)

### Rationale

Existing implementations in `FileSystem.jvm.kt`, `FileSystem.linux.kt`, and `FileSystem.mingw.kt` demonstrate working patterns. These implementations already handle:
- Asynchronous file I/O with coroutines
- Error handling with try-catch and appropriate exceptions
- Directory traversal with recursive walking
- Pattern matching for file filtering

### Existing Implementation Reference

From `FileSystem.jvm.kt`:
```kotlin
actual suspend fun readFile(filePath: String): String = withContext(Dispatchers.IO) {
    val file = File(filePath)
    if (!file.exists()) throw FileNotFoundException("File not found: $filePath")
    if (!file.canRead()) throw SecurityException("Cannot read file: $filePath")
    file.readText()
}

actual suspend fun listFilesRecursively(directoryPath: String, extensions: List<String>): List<String> =
    withContext(Dispatchers.IO) {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList()

        val result = mutableListOf<String>()
        directory.walkTopDown()
            .filter { it.isFile }
            .filter { file -> extensions.isEmpty() || extensions.any { ext ->
                file.name.endsWith(".$ext", ignoreCase = true)
            }}
            .forEach { file -> result.add(file.absolutePath) }
        result
    }
```

### Implementation Plan

1. **JvmFileScanner**: Implement `FileScanner` interface using `java.io.File` APIs
2. **JsFileScanner**: Implement using dynamic Node.js fs module imports with browser fallback stubs
3. **NativeFileScannerFactory**: Create platform-appropriate scanner instances

### Alternatives Considered

- **kotlinx-io library**: Not mature enough for multiplatform, limited platform support
- **okio multiplatform**: Adds dependency overhead, doesn't provide directory listing APIs needed
- **Direct platform APIs everywhere**: Would require extensive cinterop work for each platform

### References

- Existing `FileSystem.jvm.kt` implementation
- Kotlin coroutines documentation for Dispatchers.IO
- LWJGL file handling examples (for context)

---

## 2. Gradle Compilation Execution

### Decision

Use `ProcessBuilder` (JVM only) to execute `./gradlew` commands and parse output streams to detect compilation success/failure.

### Rationale

`CompilationValidator` is JVM-only code in the `kreekt-validation` module, so platform-specific considerations don't apply. `ProcessBuilder` provides:
- Output stream capture (stdout/stderr)
- Exit code detection
- Working directory control
- Environment variable passing

### Implementation Pattern

```kotlin
actual suspend fun validateCompilation(projectPath: String, platforms: List<Platform>, timeoutMillis: Long): CompilationResult =
    withContext(Dispatchers.IO) {
        val gradleCommand = if (System.getProperty("os.name").startsWith("Windows")) {
            "gradlew.bat"
        } else {
            "./gradlew"
        }

        val platformResults = mutableMapOf<String, PlatformCompilationResult>()

        for (platform in platforms) {
            val compileTask = when (platform) {
                Platform.JVM -> "compileKotlinJvm"
                Platform.JS -> "compileKotlinJs"
                Platform.NATIVE_LINUX_X64 -> "compileKotlinLinuxX64"
                // ... other platforms
            }

            val process = ProcessBuilder(gradleCommand, compileTask)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            platformResults[platform.name.lowercase()] = PlatformCompilationResult(
                success = exitCode == 0,
                errorMessages = if (exitCode != 0) parseErrors(output) else emptyList(),
                buildLog = output
            )
        }

        CompilationResult(
            status = if (platformResults.values.all { it.success }) ValidationStatus.PASSED else ValidationStatus.FAILED,
            platformResults = platformResults
        )
    }
```

### Error Parsing Strategy

Parse gradle output for common error patterns:
- `e: ` prefix indicates compilation error
- `BUILD FAILED` indicates overall failure
- Extract file paths, line numbers, and error messages using regex

### Alternatives Considered

- **Gradle Tooling API**: More complex, requires Gradle daemon management, overkill for simple compilation checks
- **Kotlin compiler API**: Would require reimplementing multiplatform compilation logic
- **Shell script execution**: Less portable, harder to test

### References

- Java ProcessBuilder documentation
- Gradle command-line interface documentation
- Existing CI/CD scripts for pattern examples

---

## 3. Placeholder Pattern Detection

### Decision

Use regex-based scanning with context-aware filtering. Patterns already defined in `DefaultPlaceholderScanner`:
- `TODO`, `FIXME`, `placeholder`, `stub` (excluding test `stub()` calls)
- `in the meantime`, `for now`, `in a real implementation`
- `mock` and `temporary` in implementation context

### Rationale

`DefaultPlaceholderScanner` already contains comprehensive pattern list and context-aware validation logic. The missing piece is integration with actual file I/O via `FileScanner`.

### Implementation Approach

Replace stub file operations in `DefaultPlaceholderScanner` with calls to platform-specific `FileScanner`:

```kotlin
// Change from:
private fun fileExists(path: String): Boolean = true  // stub

// To:
private suspend fun fileExists(path: String): Boolean =
    FileScannerFactory.createFileScanner().exists(path)

// Change from:
private fun readFileContent(path: String): String = ""  // stub

// To:
private suspend fun readFileContent(path: String): String? =
    FileScannerFactory.createFileScanner().readFileContent(path) ?: ""
```

### False Positive Filtering

Existing validation logic already handles:
- Documentation examples mentioning TODO/FIXME
- Test files describing what should be tested
- Markdown files with implementation plans
- Contract/specification comments

### Alternatives Considered

- **AST-based analysis**: Overkill for pattern detection, would require full Kotlin parser
- **Git blame integration**: Interesting for age tracking but not required for basic functionality
- **AI-based classification**: Not needed, regex patterns are sufficient

### References

- Existing `DefaultPlaceholderScanner` implementation
- Validation data types in `ValidationDataTypes.kt`

---

## 4. Renderer Initialization Patterns

### Decision

Platform-specific renderer factories following existing infrastructure:
- **JVM**: LWJGL 3 Vulkan initialization
- **JS**: WebGPU context creation

### Rationale

Existing `Renderer` interface and platform-specific surface creation code provide the structure. Need to implement actual initialization logic rather than throwing `NotImplementedError`.

### JVM Renderer Implementation Pattern

```kotlin
actual fun createRenderer(config: RendererConfiguration): RendererResult<Renderer> = try {
    // Initialize LWJGL
    if (!GLFW.glfwInit()) {
        return RendererResult.Failure(IllegalStateException("Failed to initialize GLFW"))
    }

    // Set window hints for Vulkan
    GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API)

    // Create window
    val window = GLFW.glfwCreateWindow(800, 600, "KreeKt JVM Example", 0L, 0L)
    if (window == 0L) {
        GLFW.glfwTerminate()
        return RendererResult.Failure(IllegalStateException("Failed to create window"))
    }

    // Create Vulkan instance (simplified)
    val appInfo = VkApplicationInfo.calloc()
        .sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
        .pApplicationName(stack.UTF8("KreeKt"))
        .applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0))
        .pEngineName(stack.UTF8("KreeKt"))
        .engineVersion(VK10.VK_MAKE_VERSION(1, 0, 0))
        .apiVersion(VK10.VK_API_VERSION_1_0)

    // ... continue with Vulkan initialization

    RendererResult.Success(JvmRenderer(window, /* vulkan objects */))
} catch (e: Exception) {
    RendererResult.Failure(e)
}
```

### JS Renderer Implementation Pattern

```kotlin
actual fun createRenderer(config: RendererConfiguration): RendererResult<Renderer> = try {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = 800
    canvas.height = 600
    document.body?.appendChild(canvas)

    // Get WebGPU context
    val context = canvas.getContext("webgpu") as? GPUCanvasContext
        ?: return RendererResult.Failure(UnsupportedOperationException("WebGPU not supported"))

    // Request adapter and device (simplified, needs Promise handling)
    val adapter = navigator.gpu.requestAdapter().await()
    val device = adapter.requestDevice().await()

    // Configure canvas
    context.configure(object {
        val device = device
        val format = "bgra8unorm"
    })

    RendererResult.Success(JsRenderer(canvas, context, device))
} catch (e: Exception) {
    RendererResult.Failure(e)
}
```

### Alternatives Considered

- **Full production renderer**: Out of scope, just need initialization for examples to work
- **Mock/stub renderers**: Violates "no stubs" constitutional requirement
- **Unified cross-platform renderer**: Not feasible given Vulkan vs WebGPU differences

### References

- LWJGL 3 Vulkan tutorial: https://lwjgl.org/guide
- WebGPU samples: https://webgpu.github.io/webgpu-samples/
- Existing `RenderSurface` implementations

---

## 5. Test Implementation Strategy

### Decision

Replace placeholder test implementations (`assertTrue(false, "message")`) with real test logic using:
1. Actual `FileSystem` APIs to create temporary test directories and files
2. Mocked gradle output for `CompilationValidator` tests
3. Test fixtures for `ProductionReadinessChecker` tests
4. Renderer initialization verification for example tests

### Rationale

Contract tests already define expected behavior through their assertion structure. Implementation involves:
- Creating test data (temp files with known placeholder patterns)
- Invoking the implementation
- Verifying results match assertions

### Example Test Implementation Pattern

**Before** (placeholder):
```kotlin
@Test
fun `scanDirectory should detect placeholder patterns in source files`() {
    assertTrue(false, "Implementation needed - see TODO in createPlaceholderScanner()")
}
```

**After** (real test):
```kotlin
@Test
fun `scanDirectory should detect placeholder patterns in source files`() = runTest {
    // Arrange
    val scanner = DefaultPlaceholderScanner()
    val tempDir = createTempDirectory()
    val testFile = File(tempDir, "Test.kt")
    testFile.writeText("""
        class Test {
            // TODO: Implement this
            fun doSomething() {}
        }
    """.trimIndent())

    // Act
    val result = scanner.scanDirectory(
        rootPath = tempDir.absolutePath,
        filePatterns = listOf("*.kt"),
        excludePatterns = emptyList()
    )

    // Assert
    assertTrue(result.placeholders.isNotEmpty(), "Should detect TODO comment")
    val placeholder = result.placeholders.first()
    assertEquals("TODO", placeholder.type)
    assertTrue(placeholder.filePath.endsWith("Test.kt"))

    // Cleanup
    testFile.delete()
    tempDir.delete()
}
```

### Mock Strategy for CompilationValidator

```kotlin
@Test
fun `validateCompilation should report compilation errors`() = runTest {
    // Arrange - Create validator with mocked ProcessBuilder
    val validator = CompilationValidator()
    val mockOutput = """
        > Task :compileKotlinJvm FAILED
        e: /path/to/file.kt:10:5: Unresolved reference: foo
        BUILD FAILED in 5s
    """.trimIndent()

    // Act
    val result = validator.parseGradleOutput(mockOutput)

    // Assert
    assertEquals(CompilationStatus.FAILED, result.status)
    assertTrue(result.errors.any { it.contains("Unresolved reference: foo") })
}
```

### Alternatives Considered

- **Skip failing tests**: Violates constitution requirement for all tests to pass
- **Comment out placeholder tests**: Hides required functionality
- **Integration tests only**: Would miss unit-level verification

### References

- Kotlin test framework documentation
- Existing passing tests for pattern examples
- JUnit 5 temporary directory support

---

## 6. Stub Removal Patterns

### Decision

Systematically replace each stub/TODO with production implementation or explicit unavailability status:

1. **File operations in DefaultPlaceholderScanner**: Use FileScanner
2. **Physics engine stubs**: Return unavailable status or basic integration
3. **Camera/Object3D NotImplementedError**: Implement minimal required functionality or remove if not needed by tests

### Rationale

Constitutional requirement: "No stubs, workarounds, TODOs, 'in the meantime', 'for now', or 'in a real implementation' placeholders are permitted."

Each placeholder must be addressed explicitly:
- Implement if tests require it
- Provide "unavailable" status if platform doesn't support feature
- Remove code if not actually used anywhere

### Stub Categories

**Category 1: Simple stubs (just need real I/O)**
```kotlin
// Before
private fun fileExists(path: String): Boolean = true  // stub

// After
private suspend fun fileExists(path: String): Boolean =
    FileScannerFactory.createFileScanner().exists(path)
```

**Category 2: Platform feature unavailability**
```kotlin
// Before
actual fun createPhysicsEngine(): PhysicsEngine = TODO("Not implemented")

// After
actual fun createPhysicsEngine(): PhysicsEngine =
    UnavailablePhysicsEngine("Physics not available on this platform")

class UnavailablePhysicsEngine(override val reason: String) : PhysicsEngine {
    override fun step(deltaTime: Float) {} // No-op
    override fun addRigidBody(body: RigidBody) {} // No-op
    // ... other no-op implementations
}
```

**Category 3: Genuinely unfinished features**
```kotlin
// Before
fun updateProjectionMatrix() {
    throw NotImplementedError("Camera projection not implemented")
}

// After
fun updateProjectionMatrix() {
    projectionMatrix = when (this) {
        is PerspectiveCamera -> Matrix4.perspective(fov, aspect, near, far)
        is OrthographicCamera -> Matrix4.orthographic(left, right, bottom, top, near, far)
    }
    projectionMatrixNeedsUpdate = false
}
```

### Verification Strategy

After stub removal:
1. Run codebase scan for placeholder patterns
2. Verify zero results in production code (non-test files)
3. Test documentation comments allowed to mention patterns
4. Run full test suite to confirm no breakage

### Alternatives Considered

- **Leave some stubs "for later"**: Violates constitution
- **Add TODO comments to track future work**: Violates constitution
- **Mark functions as experimental**: Doesn't address completeness requirement

---

## 7. Cross-Platform Testing Strategy

### Decision

Ensure all platform-specific implementations are tested:
- JVM tests use java.io.File operations
- JS tests use jsdom or Node.js environment
- Native tests use platform file APIs

### Rationale

Constitution requires cross-platform consistency. Each platform must have working implementations that pass the same contract tests.

### Platform-Specific Test Execution

Tests automatically run on appropriate platforms via Gradle:
```bash
./gradlew jvmTest        # Runs JVM-specific tests
./gradlew jsTest         # Runs JS tests in Node.js or browser
./gradlew linuxX64Test   # Runs Linux native tests
./gradlew mingwX64Test   # Runs Windows native tests (on Windows or with cross-compilation)
```

### Handling Platform Limitations

- **Browser JS**: File system access limited, tests may need mocking or skip markers
- **Native**: Some APIs may require platform-specific imports
- **Missing platforms**: Tests skip gracefully with clear warnings

---

## Summary of Decisions

| Component | Decision | Rationale |
|-----------|----------|-----------|
| File I/O | Use existing FileSystem expect/actual pattern | Already implemented pattern, just needs expansion |
| Compilation Validation | ProcessBuilder with output parsing | Standard approach for Gradle automation |
| Placeholder Detection | Regex patterns with context filtering | Already designed in DefaultPlaceholderScanner |
| Renderer Init | Platform-specific factories (LWJGL/WebGPU) | Following existing infrastructure |
| Test Implementation | Real temp files + mocked external calls | Standard TDD practice |
| Stub Removal | Implement or provide unavailable status | Constitutional requirement |
| Cross-Platform Testing | Platform-specific test execution | Ensures consistent behavior |

## Open Questions

None - All technical approaches are clear from existing codebase patterns.

## Next Steps

Proceed to Phase 1:
1. Generate data model documentation
2. Create contract files from existing interfaces
3. Write quickstart validation workflow
4. Update CLAUDE.md with implementation patterns

---

**Research Complete**: 2025-09-30
**Ready for Phase 1**: Yes