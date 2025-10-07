# T032: Shader Compilation Guide

**Feature**: 019-we-should-not

## Overview

This document describes the shader compilation process for Feature 019. WGSL shaders are compiled to SPIR-V for Vulkan,
while WebGPU uses WGSL directly.

## Compilation Strategy

### Source Format

- **WGSL** (WebGPU Shading Language) is the source format
- Location: `src/commonMain/resources/shaders/*.wgsl`

### Platform Targets

- **WebGPU (JS)**: Uses WGSL directly (no compilation needed)
- **Vulkan (JVM)**: Requires SPIR-V compilation via Tint or naga

## Manual Compilation (Current)

### Using Tint (Recommended)

```bash
# Install Tint (Google's WebGPU shader compiler)
# Download from: https://dawn.googlesource.com/tint

# Compile WGSL to SPIR-V
tint --format spirv \
     src/commonMain/resources/shaders/basic.wgsl \
     -o src/jvmMain/resources/shaders/basic.spv
```

### Using naga (Rust Alternative)

```bash
# Install naga
cargo install naga-cli

# Compile WGSL to SPIR-V
naga src/commonMain/resources/shaders/basic.wgsl \
     src/jvmMain/resources/shaders/basic.spv
```

### Using glslangValidator (Via GLSL Intermediate)

```bash
# Convert WGSL → GLSL → SPIR-V
# (Not recommended due to potential semantic differences)

# 1. Manually convert WGSL to GLSL
# 2. Compile GLSL to SPIR-V
glslangValidator -V shader.vert.glsl -o shader.vert.spv
glslangValidator -V shader.frag.glsl -o shader.frag.spv
```

## Automated Gradle Task (Deferred)

Full automation deferred to post-MVP. Current blockers:

1. Tint requires native binary (platform-specific)
2. naga requires Rust toolchain
3. Cross-platform build complexity

### Future Implementation (T032 Full)

```kotlin
// build.gradle.kts

tasks.register("compileShaders") {
    description = "Compile WGSL shaders to SPIR-V"
    group = "build"

    doLast {
        val wgslDir = file("src/commonMain/resources/shaders")
        val spirvDir = file("src/jvmMain/resources/shaders")
        spirvDir.mkdirs()

        fileTree(wgslDir) {
            include("**/*.wgsl")
        }.forEach { wgslFile ->
            val spirvFile = File(spirvDir, wgslFile.nameWithoutExtension + ".spv")

            exec {
                commandLine("tint", "--format", "spirv", wgslFile.absolutePath, "-o", spirvFile.absolutePath)
            }

            println("Compiled: ${wgslFile.name} → ${spirvFile.name}")
        }
    }
}

tasks.named("jvmProcessResources") {
    dependsOn("compileShaders")
}
```

## Current Workaround

For MVP, shaders are:

1. Written in WGSL (T031) ✅
2. Manually compiled to SPIR-V (developer responsibility)
3. Checked into version control
4. Loaded at runtime by renderers

## Validation

### WGSL Validation

```bash
# Using Tint
tint --validate src/commonMain/resources/shaders/basic.wgsl

# Should output: "Success"
```

### SPIR-V Validation

```bash
# Using spirv-val (from Vulkan SDK)
spirv-val src/jvmMain/resources/shaders/basic.spv

# Should output: "No errors found"
```

## File Structure

```
src/
├── commonMain/
│   └── resources/
│       └── shaders/
│           └── basic.wgsl          # Source shader (WGSL)
├── jvmMain/
│   └── resources/
│       └── shaders/
│           └── basic.spv           # Compiled shader (SPIR-V)
└── jsMain/
    # (No compiled shaders - uses WGSL from commonMain directly)
```

## Runtime Shader Loading

### JVM (Vulkan)

```kotlin
// VulkanPipeline.kt
val spirvBytes = javaClass.getResourceAsStream("/shaders/basic.spv")?.readBytes()
    ?: throw RendererInitializationException.ShaderCompilationException(
        "basic.spv",
        listOf("Shader file not found")
    )

val shaderModule = createShaderModule(ByteBuffer.wrap(spirvBytes))
```

### JS (WebGPU)

```kotlin
// WebGPUPipeline.kt
val wgslCode = """
    // Load from resource or embed directly
    @vertex fn vs_main(...) { ... }
    @fragment fn fs_main(...) { ... }
""".trimIndent()

val shaderModule = device.createShaderModule(jsObject {
    code = wgslCode
})
```

## Troubleshooting

### "tint: command not found"

- Install Tint from Dawn project
- Or use naga (Rust alternative)
- Or manually convert to GLSL + glslangValidator

### "spirv-val: command not found"

- Install Vulkan SDK
- Or validate using online tools (https://shader-playground.timjones.io/)

### Shader compilation errors

- Check WGSL syntax (https://www.w3.org/TR/WGSL/)
- Ensure compatibility with both WebGPU and Vulkan semantics
- Avoid platform-specific extensions

## References

- WGSL Spec: https://www.w3.org/TR/WGSL/
- Tint Compiler: https://dawn.googlesource.com/tint
- naga Compiler: https://github.com/gfx-rs/naga
- SPIR-V Tools: https://github.com/KhronosGroup/SPIRV-Tools
