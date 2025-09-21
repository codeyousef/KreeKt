# 🚀 KreeKt

> **A powerful Kotlin Multiplatform 3D graphics library bringing Three.js-like capabilities to native platforms**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org)
[![Multiplatform](https://img.shields.io/badge/Multiplatform-JVM%20|%20JS%20|%20Native-brightgreen.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)](https://github.com/your-username/kreekt/actions)

---

## ✨ Features

**KreeKt** enables developers to create stunning 3D graphics applications with a familiar Three.js-inspired API, deployable across all major platforms from a single codebase.

### 🎯 **Core Capabilities**
- 🔹 **Unified 3D API** - Write once, run everywhere with Three.js-compatible patterns
- 🔹 **Modern Rendering** - WebGPU-first with WebGL2/Vulkan fallbacks
- 🔹 **Cross-Platform Physics** - Rapier (Web) and Bullet (Native) integration
- 🔹 **Advanced Animation** - Skeletal animation, IK, and state machines
- 🔹 **Immersive XR** - VR/AR support via WebXR and native APIs
- 🔹 **High Performance** - 60 FPS with 100k+ triangles target

### 🏗️ **Platform Support**

| Platform | Status | Renderer | Physics | XR |
|----------|--------|----------|---------|-----|
| **🌐 Web** | ✅ Ready | WebGPU/WebGL2 | Rapier | WebXR |
| **☕ JVM** | ✅ Ready | Vulkan (LWJGL) | Bullet | - |
| **🍎 iOS** | ✅ Ready | MoltenVK | Bullet | ARKit |
| **🤖 Android** | 🚧 In Progress | Vulkan | Bullet | ARCore |
| **🍎 macOS** | ✅ Ready | MoltenVK | Bullet | - |
| **🐧 Linux** | ✅ Ready | Vulkan | Bullet | - |
| **🪟 Windows** | ✅ Ready | Vulkan | Bullet | - |

---

## 🚀 Quick Start

### Installation

Add KreeKt to your Kotlin Multiplatform project:

```kotlin
// build.gradle.kts
dependencies {
    commonMain {
        implementation("io.kreekt:kreekt-core:0.1.0-alpha01")
    }
}
```

### Basic Usage

```kotlin
import io.kreekt.core.scene.*
import io.kreekt.core.math.*
import io.kreekt.geometry.*
import io.kreekt.material.*
import io.kreekt.renderer.*

// Create a scene
val scene = Scene()

// Add a rotating cube with PBR material
val cube = Mesh().apply {
    geometry = BoxGeometry(1f, 1f, 1f)
    material = PBRMaterial().apply {
        baseColor = Color(0xff6b46c1)
        metallic = 0.3f
        roughness = 0.4f
    }
}
scene.add(cube)

// Set up camera and renderer
val camera = PerspectiveCamera(75f, aspectRatio, 0.1f, 1000f)
camera.position.z = 5f

val renderer = createRenderer(canvas)
renderer.setSize(width, height)

// Animation loop
fun animate() {
    cube.rotation.x += 0.01f
    cube.rotation.y += 0.01f

    renderer.render(scene, camera)
    requestAnimationFrame(::animate)
}
animate()
```

---

## 🏗️ Architecture

KreeKt follows a modular architecture with clear separation of concerns:

```
📦 KreeKt Core Modules
├── 🔧 kreekt-core          # Math primitives, utilities
├── 🎨 kreekt-renderer      # WebGPU/Vulkan abstraction
├── 🌳 kreekt-scene         # Scene graph system
├── 📐 kreekt-geometry      # Geometry classes and primitives
├── 🎭 kreekt-material      # Material system and shaders
├── 🎬 kreekt-animation     # Animation clips and mixers
├── 📁 kreekt-loader        # Asset loading (GLTF, OBJ, FBX)
├── 🎮 kreekt-controls      # Camera controls and interaction
├── ⚡ kreekt-physics       # Physics engine integration
├── 🥽 kreekt-xr           # VR/AR support
└── ✨ kreekt-postprocess  # Post-processing effects
```

### 🔄 Platform Strategy

KreeKt uses Kotlin's `expect`/`actual` pattern for platform-specific implementations:

- **Common**: Shared API definitions and business logic
- **JS**: WebGPU with @webgpu/types bindings
- **JVM**: Vulkan via LWJGL 3.3.3
- **Native**: Direct Vulkan bindings with MoltenVK on Apple platforms

---

## 🎯 Development Status

### ✅ **Phase 1: Foundation** (Completed)
- ✅ Project structure and specifications
- ✅ Core math library (Vector3, Matrix4, Quaternion)
- ✅ WebGPU/Vulkan abstraction layer
- ✅ Platform-specific surface creation
- ✅ Basic scene graph system

### 🚧 **Phase 2-3: Advanced Features** (In Progress)
- 🔄 Advanced geometry system
- 🔄 PBR material pipeline
- 🔄 Lighting system (IBL, shadows)
- 🔄 Skeletal animation
- 🔄 Physics integration
- 🔄 XR support
- 🔄 Post-processing effects

### 🛠️ **Phase 4: Tooling** (Planned)
- 📝 Scene editor (web-based)
- 🎨 Material editor
- 📊 Performance profiler
- 📚 Documentation system

---

## 🔧 Development Setup

### Prerequisites
- Kotlin 1.9+
- Gradle 8.0+
- Platform-specific SDKs as needed

### Build the Project

```bash
# Clone the repository
git clone https://github.com/your-username/kreekt.git
cd kreekt

# Build all targets
./gradlew build

# Build specific targets
./gradlew compileKotlinJvm     # JVM target
./gradlew compileKotlinJs      # JavaScript target
./gradlew compileKotlinLinuxX64 # Linux native
```

### Run Tests

```bash
# Run all tests
./gradlew test

# Platform-specific tests
./gradlew jvmTest
./gradlew jsTest
```

---

## 📊 Performance Targets

| Quality Tier | Target FPS | Max Triangles | Memory Budget | Features |
|--------------|------------|---------------|---------------|----------|
| **Mobile** | 60 | 50k | 256MB GPU | Basic effects |
| **Standard** | 60 | 100k | 1GB GPU | Advanced effects |
| **High** | 60 | 500k | 2GB GPU | Full pipeline |
| **Ultra** | 120+ | Unlimited | 4GB+ GPU | Experimental |

---

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Workflow
1. 🍴 Fork the repository
2. 🌿 Create a feature branch (`git checkout -b feature/amazing-feature`)
3. ✅ Write tests for your changes
4. 📝 Commit your changes (`git commit -m 'Add amazing feature'`)
5. 📤 Push to the branch (`git push origin feature/amazing-feature`)
6. 🔄 Open a Pull Request

---

## 📖 Documentation

- 📚 **[API Documentation](https://docs.kreekt.io)** - Complete API reference
- 🎓 **[Getting Started Guide](https://docs.kreekt.io/getting-started)** - Tutorials and examples
- 🔄 **[Migration from Three.js](https://docs.kreekt.io/migration)** - Porting guide
- 🏗️ **[Architecture Overview](https://docs.kreekt.io/architecture)** - Technical deep-dive

---

## 🎨 Examples

```kotlin
// 🌟 Basic Scene with Lighting
val scene = Scene()
val ambientLight = AmbientLight(Color.WHITE, 0.4f)
val directionalLight = DirectionalLight(Color.WHITE, 0.8f)
scene.add(ambientLight, directionalLight)

// 🎭 PBR Materials
val material = PBRMaterial().apply {
    baseColor = Color(0xff6366f1)
    metallic = 0.7f
    roughness = 0.3f
    emissive = Color(0x001122)
}

// 🎬 Animation
val mixer = AnimationMixer(model)
val action = mixer.clipAction(walkAnimation)
action.play()

// ⚡ Physics
val world = PhysicsWorld()
val rigidBody = RigidBody(BoxShape(1f, 1f, 1f), 1.0f)
world.addRigidBody(rigidBody)
```

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

## 🌟 Acknowledgments

- Inspired by [Three.js](https://threejs.org/) for the elegant 3D API design
- Built on [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- Powered by modern graphics APIs: WebGPU, Vulkan, Metal

---

<div align="center">

**⭐ Star this repository if KreeKt helps your project! ⭐**

[🚀 Get Started](https://docs.kreekt.io/getting-started) • [📚 Documentation](https://docs.kreekt.io) • [💬 Community](https://github.com/your-username/kreekt/discussions)

</div>