# 🚀 Running KreeKt Examples

This guide shows you how to run the KreeKt 3D library examples on your system.

## 🎯 Quick Start

### Windows

```powershell
# PowerShell
.\run-examples.ps1 build
.\run-examples.ps1 simple

# Command Prompt
run-examples.bat build
run-examples.bat simple
```

### Linux/macOS

```bash
./run-examples.sh build
./run-examples.sh simple
```

## 📝 Available Examples

### 1. **Simple Demo** (Recommended Start)

- **Description**: Demonstrates core KreeKt functionality without complex setup
- **What it shows**: Math operations, scene graph, geometry, materials, animation
- **Requirements**: Just Java/Kotlin
- **Run**: `./run-examples.sh simple` or `run-examples.bat simple`

### 2. **JVM Interactive Example**

- **Description**: Full 3D scene with LWJGL backend
- **What it shows**: Real 3D rendering, WebGPU/Vulkan abstraction
- **Requirements**: Java + LWJGL natives
- **Run**: `./run-examples.sh jvm` or `run-examples.bat jvm`

### 3. **Web Browser Example**

- **Description**: Browser-based 3D scene using WebGPU
- **What it shows**: Cross-platform rendering in web browsers
- **Requirements**: Modern web browser with WebGPU support
- **Run**: `./run-examples.sh web` or `run-examples.bat web`

## 🔧 Direct Gradle Commands

If you prefer using Gradle directly:

```bash
# List all available examples
./gradlew listExamples

# Quick start (build + run)
./gradlew quickStart

# Individual examples
./gradlew :examples:basic-scene:runSimple
./gradlew :examples:basic-scene:runJvm
./gradlew :examples:basic-scene:runJs

# Build and test
./gradlew build
./gradlew test
```

## 🌐 Platform-Specific Commands

### Compile for specific platforms

```bash
./gradlew compileKotlinJvm          # JVM target
./gradlew compileKotlinJs           # JavaScript target
./gradlew compileKotlinLinuxX64     # Linux native
./gradlew compileKotlinMingwX64     # Windows native
```

### Run platform-specific tests

```bash
./gradlew jvmTest                   # JVM tests
./gradlew jsTest                    # JavaScript tests
./gradlew linuxX64Test              # Linux native tests
./gradlew mingwX64Test              # Windows native tests
```

## 🛠️ Development Tools

```bash
# Generate API documentation
./gradlew dokkaHtml

# Development mode with hot reload (web)
./gradlew :examples:basic-scene:dev

# Clean build artifacts
./gradlew clean
```

## 📋 Requirements

### Minimum Requirements

- **Java 11+** (required for all examples)
- **Gradle** (wrapper included)

### For JVM Examples

- **LWJGL natives** (automatically downloaded)
- **OpenGL 3.3+** or **Vulkan** support

### For Web Examples

- **Modern browser** with WebGPU support:
    - Chrome 94+ (experimental flag enabled)
    - Firefox 103+ (experimental flag enabled)
    - Safari 16+ (experimental support)

### For Native Examples

- **Native toolchain** for your platform
- **Vulkan drivers** (for native rendering)

## 🎮 What Each Example Demonstrates

### Simple Demo Output

```
🚀 KreeKt Basic Scene Example - Simple Launcher
==================================================

📐 Testing Core Math Library:
  Vector3 addition: Vector3(1.0, 2.0, 3.0) + Vector3(4.0, 5.0, 6.0) = Vector3(5.0, 7.0, 9.0)
  Vector3 length: |Vector3(1.0, 2.0, 3.0)| = 3.7416574
  Vector3 dot product: Vector3(1.0, 2.0, 3.0) · Vector3(4.0, 5.0, 6.0) = 32.0

🏗️ Testing Scene Graph:
  Scene objects: 2
  Cube children: 1
  Total objects in scene: 4

✅ Core KreeKt functionality working!
```

### JVM Example Features

- Real-time 3D rendering
- LWJGL integration
- Vulkan/OpenGL backend
- Interactive camera controls
- Material and lighting systems

### Web Example Features

- WebGPU rendering pipeline
- Browser-based 3D scenes
- Cross-platform shader compilation
- Touch/mouse interaction
- Responsive design

## 🐛 Troubleshooting

### Common Issues

**1. "gradlew not found" or "permission denied"**

```bash
chmod +x gradlew
chmod +x run-examples.sh
```

**2. "kotlinc not recognized"**

- Install Kotlin: `choco install kotlin` (Windows) or use the gradle tasks instead

**3. "WebGPU not supported"**

- Enable experimental WebGPU flag in browser settings
- Use Chrome/Firefox with experimental features enabled

**4. "LWJGL natives not found"**

- Run `./gradlew :examples:basic-scene:build` to download natives
- Check Java version (requires Java 11+)

**5. "Build failed"**

- Run `./gradlew clean build` to clean and rebuild
- Check that all compilation errors are fixed (should be ✅ from previous work)

### Getting Help

1. **List available tasks**: `./gradlew tasks --group=examples`
2. **Check project status**: `./gradlew build --info`
3. **View detailed output**: Add `--info` or `--debug` to any gradle command

## 🎯 Recommended Learning Path

1. **Start Simple**: `./run-examples.sh simple`
    - Understand core concepts
    - See math and scene graph in action

2. **Try JVM**: `./run-examples.sh jvm`
    - Experience real 3D rendering
    - Explore LWJGL integration

3. **Explore Web**: `./run-examples.sh web`
    - See cross-platform capabilities
    - Compare WebGPU vs native rendering

4. **Build Your Own**: Use the examples as templates for your own 3D applications

## 📚 Next Steps

- **API Documentation**: `./gradlew dokkaHtml`
- **Source Code**: Explore `src/commonMain/kotlin/io/kreekt/`
- **Examples Source**: Check `examples/basic-scene/src/`
- **Advanced Features**: Look at the comprehensive API contracts in the codebase

---

**Happy 3D coding with KreeKt! 🎮✨**