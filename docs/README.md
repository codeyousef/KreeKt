# KreeKt Documentation

Comprehensive documentation for the KreeKt 3D graphics library - A Kotlin Multiplatform Three.js equivalent.

## Quick Links

- [Getting Started](guides/getting-started.md) - Quick start guide for new users
- [API Reference](api-reference/README.md) - Complete API documentation
- [Architecture](architecture/overview.md) - System architecture and design
- [Examples](examples/basic-usage.md) - Code examples and patterns
- [Platform-Specific Guides](guides/platform-specific.md) - Platform considerations

## What is KreeKt?

KreeKt is a Kotlin Multiplatform library providing Three.js-equivalent 3D graphics capabilities using WebGPU/Vulkan
rendering backends. Write 3D applications once and deploy across JVM, Web, Android, iOS, and Native platforms.

## Core Features

- **Scene Graph System**: Object3D hierarchy with transformation management
- **Advanced Rendering**: WebGPU/Vulkan abstraction with PBR materials
- **Animation System**: Skeletal animation, morph targets, and state machines
- **Physics Integration**: Rapier-based physics engine
- **XR Support**: VR/AR capabilities via WebXR and native APIs
- **Asset Loading**: GLTF, OBJ, FBX format support
- **Post-Processing**: Bloom, tone mapping, and temporal effects

## Documentation Structure

### [API Reference](api-reference/README.md)

Complete API documentation organized by module:

- **Core**: Math primitives, scene graph, utilities
- **Renderer**: Rendering system and GPU abstraction
- **Material**: Material system and shaders
- **Geometry**: Geometry classes and primitives
- **Animation**: Animation system and skeletal animation
- **Physics**: Physics integration
- **XR**: VR/AR support
- **Loader**: Asset loading system

### [Guides](guides/README.md)

User guides and tutorials:

- Getting Started - Quick start guide
- Materials Guide - Working with materials
- Animation Guide - Animation system
- Physics Guide - Physics integration
- Platform-Specific - Platform considerations

### [Architecture](architecture/README.md)

System architecture documentation:

- Overview - System architecture
- Rendering Pipeline - How rendering works
- Cross-Platform - Multiplatform strategy
- Performance - Performance considerations

### [Examples](examples/README.md)

Code examples and best practices:

- Basic Usage - Simple examples
- Advanced Techniques - Advanced patterns
- Best Practices - Recommended practices

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.kreekt:kreekt-core:1.0.0")
}
```

### Maven

```xml
<dependency>
    <groupId>io.kreekt</groupId>
    <artifactId>kreekt-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Example

```kotlin
import io.kreekt.core.scene.*
import io.kreekt.core.math.*
import io.kreekt.geometry.BoxGeometry
import io.kreekt.material.MeshStandardMaterial
import io.kreekt.camera.PerspectiveCamera

// Create scene
val scene = Scene()

// Add camera
val camera = PerspectiveCamera(
    fov = 75f,
    aspect = 16f / 9f,
    near = 0.1f,
    far = 1000f
)
camera.position.z = 5f

// Create mesh
val geometry = BoxGeometry(1f, 1f, 1f)
val material = MeshStandardMaterial().apply {
    color = Color(0x00ff00)
}
val cube = Mesh(geometry, material)
scene.add(cube)

// Animation loop
fun animate() {
    cube.rotation.x += 0.01f
    cube.rotation.y += 0.01f
    renderer.render(scene, camera)
}
```

## Platform Support

| Platform   | Status | Rendering Backend           |
|------------|--------|-----------------------------|
| JVM        | Stable | Vulkan via LWJGL            |
| JavaScript | Stable | WebGPU with WebGL2 fallback |
| Android    | Stable | Native Vulkan API           |
| iOS        | Beta   | MoltenVK (Vulkan-to-Metal)  |
| Native     | Beta   | Direct Vulkan bindings      |

## Performance Targets

- **Frame Rate**: 60 FPS with 100k triangles
- **Library Size**: <5MB base library (modular architecture)
- **Memory**: Efficient object pooling and caching
- **Initialization**: <1s renderer initialization

## Contributing

See [CONTRIBUTING.md](../../CONTRIBUTING.md) for contribution guidelines.

## License

This project is licensed under the Apache 2.0 License - see [LICENSE](../../LICENSE) file for details.

## Resources

- [GitHub Repository](https://github.com/your-org/kreekt)
- [API Documentation](https://kreekt.io/api)
- [Examples](https://kreekt.io/examples)
- [Community Discord](https://discord.gg/kreekt)
