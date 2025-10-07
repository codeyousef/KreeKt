# Migration Guide: Feature 019

**From**: WebGL-only renderer with manual instantiation
**To**: WebGPU/Vulkan primary with automatic backend selection

## Overview

Feature 019 refactors KreeKt to use modern graphics APIs (WebGPU/Vulkan) as primary backends, with WebGL as fallback
only. This guide helps you migrate existing code to the new architecture.

## Breaking Changes

### 1. Renderer Instantiation

#### Before (❌ Old API)

```kotlin
// JS
val renderer = WebGLRenderer(canvas)

// JVM
val renderer = OpenGLRenderer(window)
```

#### After (✅ New API)

```kotlin
// JS
val surface = WebGPUSurface(canvas)
val renderer = RendererFactory.create(surface).getOrThrow()
// Automatically selects WebGPU → WebGL fallback

// JVM
val surface = VulkanSurface(window)
val renderer = RendererFactory.create(surface).getOrThrow()
// Automatically selects Vulkan (no fallback per FR-002)
```

### 2. Surface Creation

#### Before (❌ Old API)

```kotlin
// JS
val surface = WebGPURenderSurface(canvas)

// JVM
val surface = VulkanRenderSurface(window, width, height)
```

#### After (✅ New API)

```kotlin
// JS
val surface = WebGPUSurface(canvas)

// JVM
val surface = VulkanSurface(window)
```

**Changes**:

- `WebGPURenderSurface` → `WebGPUSurface`
- `VulkanRenderSurface` → `VulkanSurface`
- Width/height automatically queried (no manual parameters)

### 3. Error Handling

#### Before (❌ Old API)

```kotlin
try {
    val renderer = WebGLRenderer(canvas)
    // ... rendering ...
} catch (e: Exception) {
    console.error("Renderer failed: ${e.message}")
}
```

#### After (✅ New API)

```kotlin
val renderer = try {
    RendererFactory.create(surface).getOrElse { exception ->
        when (exception) {
            is RendererInitializationException.NoGraphicsSupportException -> {
                showErrorDialog("Graphics not supported: ${exception.message}")
            }
            is RendererInitializationException.DeviceCreationFailedException -> {
                showErrorDialog("GPU device creation failed: ${exception.message}")
            }
            else -> {
                showErrorDialog("Renderer initialization failed: ${exception.message}")
            }
        }
        exitProcess(1)
    }
} catch (e: Throwable) {
    console.error("Unexpected error: ${e.message}")
    exitProcess(1)
}
```

**Changes**:

- Generic `Exception` → Typed `RendererInitializationException` hierarchy
- `Result<T, E>` pattern for functional error handling
- Comprehensive diagnostics in exception messages

### 4. Backend Detection

#### Before (❌ Not Available)

```kotlin
// No API for backend detection
// Manual checks: js("'gpu' in navigator"), etc.
```

#### After (✅ New API)

```kotlin
val availableBackends = RendererFactory.detectAvailableBackends()
// Returns: [WEBGPU, WEBGL] on modern browsers
// Returns: [VULKAN] on JVM with Vulkan support
// Returns: [] if no graphics support

println("Available backends: ${availableBackends.joinToString(", ")}")
```

### 5. Renderer Configuration

#### Before (❌ Old API)

```kotlin
// Limited configuration
val renderer = WebGLRenderer(canvas)
renderer.autoClear = true
renderer.clearColor = Color(0x000000)
```

#### After (✅ New API)

```kotlin
val config = RendererConfig(
    preferredBackend = BackendType.WEBGPU, // or null for auto-detect
    enableValidation = true, // Debug layers for development
    vsync = true, // Enable V-sync
    msaaSamples = 4, // Antialiasing (1, 2, 4, 8, 16)
    powerPreference = PowerPreference.HIGH_PERFORMANCE // or LOW_POWER
)

val renderer = RendererFactory.create(surface, config).getOrThrow()
```

### 6. Renderer Capabilities

#### Before (❌ Old API)

```kotlin
val capabilities = renderer.capabilities
println("Max texture size: ${capabilities.maxTextureSize}")
```

#### After (✅ New API - Enhanced)

```kotlin
val capabilities = renderer.capabilities
println("Backend: ${capabilities.backend}") // NEW: WEBGPU, VULKAN, or WEBGL
println("Device: ${capabilities.deviceName}") // NEW: GPU name
println("Driver: ${capabilities.driverVersion}") // NEW: Driver version
println("Compute: ${capabilities.supportsCompute}") // NEW: Compute shader support
println("Ray Tracing: ${capabilities.supportsRayTracing}") // NEW: RT support
println("Max texture size: ${capabilities.maxTextureSize}")
println("Max MSAA: ${capabilities.maxSamples}")
```

### 7. Render Statistics

#### Before (❌ Old API)

```kotlin
data class RenderStats(
    val frame: Int,
    val calls: Int,
    val triangles: Int,
    // ... other fields
)
```

#### After (✅ New API - Simplified)

```kotlin
data class RenderStats(
    val fps: Double, // Frames per second
    val frameTime: Double, // Frame time in milliseconds
    val triangles: Int, // Triangle count
    val drawCalls: Int, // Draw call count
    val textureMemory: Long, // Texture memory in bytes
    val bufferMemory: Long, // Buffer memory in bytes
    val timestamp: Long // Timestamp of capture
)

// Convenience properties
stats.meetsMinimumFps // >= 30 FPS (FR-019 requirement)
stats.meetsTargetFps // >= 60 FPS (FR-019 target)
```

## Migration Steps

### Step 1: Update Imports

```kotlin
// Remove old imports
import io.kreekt.renderer.WebGLRenderer
import io.kreekt.renderer.WebGPURenderSurface
import io.kreekt.renderer.VulkanRenderSurface

// Add new imports
import io.kreekt.renderer.RendererFactory
import io.kreekt.renderer.RendererInitializationException
import io.kreekt.renderer.RendererConfig
import io.kreekt.renderer.BackendType
import io.kreekt.renderer.webgpu.WebGPUSurface // JS
import io.kreekt.renderer.vulkan.VulkanSurface // JVM
```

### Step 2: Replace Renderer Creation

```kotlin
// Old
val renderer = WebGLRenderer(canvas)

// New
val surface = WebGPUSurface(canvas)
val renderer = RendererFactory.create(surface).getOrElse { error ->
    console.error("Failed to create renderer: ${error.message}")
    throw error
}
```

### Step 3: Add Error Handling

Wrap renderer creation in comprehensive error handling:

```kotlin
suspend fun initializeRenderer(canvas: HTMLCanvasElement): Renderer {
    val surface = WebGPUSurface(canvas)

    return try {
        RendererFactory.create(surface).getOrElse { exception ->
            when (exception) {
                is RendererInitializationException.NoGraphicsSupportException -> {
                    Logger.error("Graphics not supported")
                    Logger.error("Platform: ${exception.platform}")
                    Logger.error("Available: ${exception.availableBackends}")
                    Logger.error("Required: ${exception.requiredFeatures}")
                    throw exception
                }
                else -> {
                    Logger.error("Renderer init failed: ${exception.message}")
                    throw exception
                }
            }
        }
    } catch (e: RendererInitializationException) {
        // Re-throw with additional context
        throw e
    }
}
```

### Step 4: Update Renderer Usage

No changes needed! The Renderer interface is backward compatible:

```kotlin
// Still works the same
renderer.render(scene, camera)
renderer.resize(width, height)
renderer.dispose()
```

### Step 5: Test on All Platforms

```bash
# Test JS (browser)
./gradlew :examples:voxelcraft:runJs

# Test JVM
./gradlew :examples:voxelcraft:runJvm
```

## Common Issues

### Issue 1: "WebGPU not available"

**Symptom**: `NoGraphicsSupportException` on browsers

**Solution**:

- Ensure browser supports WebGPU (Chrome 113+, Edge 113+)
- Check `chrome://gpu` for WebGPU status
- Falls back to WebGL automatically (no action needed)

### Issue 2: "Vulkan not found"

**Symptom**: `NoGraphicsSupportException` on JVM

**Solution**:

- Install/update GPU drivers
- Verify Vulkan support: `vulkaninfo` (Vulkan SDK)
- Check GLFW Vulkan support: `GLFWVulkan.glfwVulkanSupported()`

### Issue 3: "Shader compilation failed"

**Symptom**: `ShaderCompilationException`

**Solution**:

- Check WGSL shader syntax
- Validate with Tint: `tint --validate shader.wgsl`
- For SPIR-V: `spirv-val shader.spv`

### Issue 4: "Cannot find WebGPUSurface"

**Symptom**: Import error

**Solution**:

```kotlin
// Correct import (note: webgpu package)
import io.kreekt.renderer.webgpu.WebGPUSurface
```

### Issue 5: "Result type mismatch"

**Symptom**: `Result<Renderer, RendererInitializationException>` not compatible

**Solution**:

```kotlin
// Use .getOrElse {} or .getOrThrow()
val renderer = RendererFactory.create(surface).getOrElse { error ->
    // Handle error
    throw error
}

// Or
val renderer = RendererFactory.create(surface).getOrThrow()
```

## Feature Requirements Checklist

After migration, verify:

- ✅ **FR-001**: WebGPU primary for JS (automatic)
- ✅ **FR-002**: Vulkan primary for JVM (automatic)
- ✅ **FR-003**: WebGL fallback only (automatic)
- ✅ **FR-004**: Backend detection works
- ✅ **FR-007**: No manual renderer instantiation in examples
- ✅ **FR-011**: Unified interface across platforms
- ✅ **FR-022**: Fail-fast error handling
- ✅ **FR-024**: Capability detection before rendering

## Automated Migration Script

For bulk migrations, use this Kotlin script:

```kotlin
// migrate.main.kts
import java.io.File

File("src").walk().filter { it.extension == "kt" }.forEach { file ->
    var content = file.readText()

    // Replace renderer instantiation
    content = content.replace(
        Regex("""WebGLRenderer\(([^)]+)\)"""),
        """RendererFactory.create(WebGPUSurface($1)).getOrThrow()"""
    )

    // Replace surface types
    content = content.replace("WebGPURenderSurface", "WebGPUSurface")
    content = content.replace("VulkanRenderSurface", "VulkanSurface")

    file.writeText(content)
    println("Migrated: ${file.path}")
}
```

Run: `kotlin migrate.main.kts`

## Support

For migration issues:

- Check: `specs/019-we-should-not/IMPLEMENTATION_PROGRESS.md`
- Examples: `examples/voxelcraft/` (fully migrated)
- Report issues: GitHub Issues

## Conclusion

Feature 019 modernizes KreeKt with:

- ✅ Modern graphics APIs (WebGPU/Vulkan primary)
- ✅ Automatic backend selection
- ✅ Comprehensive error handling
- ✅ Cross-platform consistency

Migration is straightforward and backward compatible at the interface level.
