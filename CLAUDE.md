# Claude Code Instructions: KreeKt Library

## Project Overview
This is a Kotlin Multiplatform library providing Three.js-equivalent 3D graphics capabilities using WebGPU/Vulkan rendering backends. The library enables developers to write 3D applications once and deploy across JVM, Web, Android, iOS, and Native platforms.

## Architecture

### Core Modules
- **kreekt-core**: Math primitives (Vector3, Matrix4, Quaternion), utilities
- **kreekt-renderer**: WebGPU/Vulkan abstraction layer with platform-specific implementations
- **kreekt-scene**: Scene graph system (Object3D, Scene, Camera, lights)
- **kreekt-geometry**: Geometry classes and primitive generation
- **kreekt-material**: Material system and shader management
- **kreekt-animation**: Animation clips, mixers, and interpolation
- **kreekt-loader**: Asset loading for GLTF, OBJ, FBX formats
- **kreekt-controls**: Camera controls and user interaction
- **kreekt-physics**: Physics engine integration (Rapier primary)
- **kreekt-xr**: VR/AR support via WebXR and native APIs
- **kreekt-postprocess**: Post-processing effects pipeline

### Platform Strategy
- **Common**: Shared API definitions using expect/actual pattern
- **JVM**: Vulkan via LWJGL 3.3.3 bindings
- **JS**: WebGPU with WebGL2 fallback using @webgpu/types
- **Android**: Native Vulkan API
- **iOS**: MoltenVK (Vulkan-to-Metal translation)
- **Native**: Direct Vulkan bindings

### Key Design Principles
1. **Type Safety**: No runtime casts, compile-time validation
2. **Three.js Compatibility**: Familiar API patterns for easy migration
3. **Performance**: 60 FPS with 100k triangles target
4. **Size Constraint**: <5MB base library, modular architecture
5. **Modern Graphics**: WebGPU-first with sensible fallbacks

## Development Guidelines

### Code Style
- Use data classes for immutable structures (Vector3, Color, etc.)
- Implement Builder DSL pattern for complex object creation
- Prefer sealed classes for type hierarchies (Material, Light, etc.)
- Use inline classes for performance-critical math operations
- Apply @OptIn annotations for experimental multiplatform APIs

### Testing Strategy
- **Unit Tests**: Math operations, data structures, utilities
- **Integration Tests**: Renderer initialization, scene rendering
- **Platform Tests**: Platform-specific functionality
- **Performance Tests**: Frame rate, memory usage, initialization time
- **Visual Tests**: Rendering consistency across platforms

### Performance Considerations
- Object pooling for frequently created math objects
- Dirty flag optimization for matrix updates
- Frustum culling and batching for rendering
- Lazy initialization of GPU resources
- Progressive asset loading for large models

### Multiplatform Patterns
```kotlin
// Expect/actual for platform differences
expect class TypedArray
expect fun createRenderer(): Renderer

// Platform-specific implementations
actual class TypedArray // JS: use native TypedArray
actual class TypedArray // JVM: wrap NIO ByteBuffer
```

### Error Handling
- Use sealed Result classes for async operations
- Provide detailed error messages with context
- Graceful degradation for unsupported features
- Platform-specific error reporting

## Current Implementation Status

### Phase 1: Foundation (Weeks 1-4)
- [x] Project structure and specification
- [ ] Math library implementation
- [ ] WebGPU/Vulkan abstraction layer
- [ ] Platform-specific surface creation

### Phase 2: Scene Graph (Weeks 5-8)
- [ ] Object3D hierarchy
- [ ] Camera system
- [ ] Basic geometries (Box, Sphere, Plane)

### Dependencies
```kotlin
// Common dependencies
kotlinx-coroutines-core:1.8.0
kotlinx-serialization-json:1.6.0
kotlin-math:0.5.0

// Platform-specific
LWJGL:3.3.3 (JVM)
@webgpu/types:0.1.40 (JS)
```

### Build Configuration
- Kotlin 1.9+ with Multiplatform plugin
- Gradle 8.0+ with version catalogs
- Android target API 24+
- iOS deployment target 14+

### Documentation
- API docs generated with Dokka
- Examples and tutorials in quickstart.md
- Platform-specific setup guides
- Migration guide from Three.js

## Known Technical Challenges

### WebGPU/Vulkan Differences
- Shader languages: WGSL vs SPIR-V
- Resource binding models
- Synchronization primitives
- Memory management approaches

### Cross-Platform Shader Compilation
- Use WGSL as source with SPIR-V Cross for Vulkan
- Platform-specific shader variants where needed
- Runtime feature detection and adaptation

### Asset Pipeline
- Platform-appropriate texture formats (BC, ETC2, ASTC)
- Progressive loading strategies
- Cross-platform file system access

### Performance Optimization
- Platform-specific GPU memory management
- Adaptive quality settings based on hardware
- Efficient batching and instancing

## Development Workflow

### Branch Strategy
- Feature branches: `001-feature-name` format
- Specification in `specs/001-feature-name/`
- Implementation follows TDD approach

### Testing Approach
1. Write contract tests first
2. Implement platform-common logic
3. Add platform-specific implementations
4. Validate cross-platform consistency

### CI/CD Pipeline
- Multi-platform builds on GitHub Actions
- Automated testing on all target platforms
- Performance regression detection
- Documentation generation and deployment

## Debugging and Profiling

### Metrics Tracking
- FPS and frame time measurements
- GPU memory usage monitoring
- Draw call and triangle counting
- Shader compilation timing

### Debug Tools
- Renderer capability introspection
- Scene graph visualization
- Performance overlay options
- Console and remote logging

### Platform-Specific Debugging
- **Web**: Browser DevTools integration
- **JVM**: JProfiler/VisualVM compatibility
- **Mobile**: Platform-native profiling tools

## Recent Changes
1. **2025-09-19**: Initial project specification and planning completed
2. **2025-09-19**: Core data model and API contracts defined
3. **2025-09-19**: Research findings documented for technical decisions

## Next Development Steps
1. Implement core math library (Vector3, Matrix4, Quaternion)
2. Create WebGPU/Vulkan device abstraction
3. Build basic geometry system
4. Develop material and shader management
5. Add scene rendering pipeline

---
*Keep this file updated as implementation progresses. Focus on multiplatform considerations and Three.js compatibility.*