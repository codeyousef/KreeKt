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

### Phase 1: Foundation (Weeks 1-4) - COMPLETED
- [x] Project structure and specification
- [x] Math library implementation
- [x] WebGPU/Vulkan abstraction layer
- [x] Platform-specific surface creation

### Phase 2: Scene Graph (Weeks 5-8) - COMPLETED
- [x] Object3D hierarchy
- [x] Camera system
- [x] Basic geometries (Box, Sphere, Plane)

### Phase 3: Advanced Features (Current Development)
- [x] Specification and API contracts for advanced features (Phases 2-13)
- [ ] Advanced geometry system (procedural generation, extrusion, text)
- [ ] PBR material system with physically-based shading
- [ ] Advanced lighting (IBL, area lights, shadows, light probes)
- [ ] Skeletal animation with IK and state machines
- [ ] Physics engine integration (Rapier primary, Bullet fallback)
- [ ] XR support (WebXR, ARKit, ARCore integration)
- [ ] Post-processing effects pipeline
- [ ] Performance optimization (LOD, instancing, culling)
- [ ] Development tools (scene editor, material editor)

### Advanced Feature Categories (Phase 2-13)
1. **Geometry**: Procedural generation, extrusion, text rendering, optimization
2. **Materials**: PBR shading, custom shaders, texture atlasing
3. **Lighting**: Image-based lighting, area lights, cascaded shadows
4. **Animation**: Skeletal animation, morph targets, inverse kinematics
5. **Physics**: Rigid body dynamics, collision detection, character controllers
6. **XR**: VR/AR session management, spatial tracking, hand tracking
7. **Post-Processing**: Bloom, tone mapping, temporal effects
8. **Optimization**: LOD systems, frustum culling, GPU-driven rendering
9. **Tools**: Visual editors, performance profilers, asset pipeline

### Dependencies
```kotlin
// Common dependencies
kotlinx-coroutines-core:1.8.0
kotlinx-serialization-json:1.6.0
kotlin-math:0.5.0

// Platform-specific
LWJGL:3.3.3 (JVM)
@webgpu/types:0.1.40 (JS)

// Advanced feature dependencies (optional)
// Physics: Rapier WASM bindings (Web), Bullet JNI (JVM/Native)
// XR: WebXR APIs (Web), ARKit (iOS), ARCore (Android)
// Asset processing: DRACO, KTX2, FreeType
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

## Production Readiness Validation System

### Comprehensive Validation Framework

KreeKt includes a complete production readiness audit system that validates:

#### Constitutional Compliance Validation

- **60 FPS Performance**: Validates frame rate meets constitutional 60 FPS requirement across all platforms
- **5MB Size Limit**: Ensures library size stays under constitutional 5MB constraint
- **Type Safety**: Validates no runtime casts and compile-time type safety
- **Cross-Platform Consistency**: Ensures API behavior consistency between platforms

#### Implementation Completeness Validation

- **Placeholder Detection**: Scans entire codebase for TODO, FIXME, STUB patterns
- **Implementation Gap Analysis**: Validates expect/actual implementations across platforms
- **Renderer Validation**: Audits platform-specific renderer implementations
- **Feature Completeness**: Validates all planned features are implemented

#### Quality Assurance Validation

- **Test Suite Validation**: Ensures >95% test success rate and >80% code coverage
- **Performance Validation**: Validates 60 FPS performance and memory constraints
- **Cross-Platform Validation**: Tests consistency across JVM, JS, Native platforms
- **Production Readiness Score**: Generates overall readiness score with detailed breakdown

### Validation Usage

#### Quick Production Readiness Check

```kotlin
val checker = DefaultProductionReadinessChecker()
val result = checker.validateProductionReadiness(
    projectRoot = "/path/to/kreekt",
    config = ValidationConfiguration.strict()
)

println("Production Ready: ${result.overallStatus == ValidationStatus.PASSED}")
println("Overall Score: ${result.overallScore}/1.0")
```

#### Component-Specific Validation

```kotlin
// Performance validation
val performanceResult = checker.validatePerformance(projectRoot)
println("Meets 60 FPS: ${performanceResult.meetsFrameRateRequirement}")
println("Under 5MB: ${performanceResult.meetsSizeRequirement}")

// Cross-platform validation
val rendererAudit = checker.auditRendererImplementations(projectRoot)
rendererAudit.rendererComponents.forEach { (platform, component) ->
    println("$platform: Ready=${component.isProductionReady}, Score=${component.performanceScore}")
}

// Test suite validation
val testResults = checker.executeTestSuite(projectRoot)
val successRate = testResults.passedTests.toFloat() / testResults.totalTests.toFloat()
println("Test Success Rate: ${String.format("%.1f", successRate * 100)}%")
```

#### Constitutional Compliance Check

```kotlin
val complianceResult = checker.checkConstitutionalCompliance(result)
println("Constitutional Compliance: ${complianceResult.overallCompliance}")

complianceResult.constitutionalRequirements.forEach { (requirement, met) ->
    println("$requirement: ${if (met) "✅" else "❌"}")
}
```

#### Automated Recommendations

```kotlin
val recommendations = checker.generateRecommendations(result)
recommendations.forEach { recommendation ->
    println("📋 $recommendation")
}
```

### Validation Test Coverage

The validation system includes comprehensive test coverage:

#### Performance Tests (`PerformanceValidationTest`)

- Frame rate requirement validation (60 FPS constitutional requirement)
- Library size constraint validation (<5MB constitutional limit)
- Memory usage validation within acceptable limits
- Cross-platform performance consistency validation

#### Cross-Platform Tests (`CrossPlatformValidationTest`)

- All platform support validation (JVM, JS, Native, Android, iOS)
- Renderer consistency across platforms
- Multiplatform API consistency validation
- Platform-specific implementation validation
- JavaScript renderer black screen issue validation

#### Test Suite Tests (`TestSuiteValidationTest`)

- Complete test suite execution validation
- Test success rate validation (>95% constitutional requirement)
- Test coverage validation (>80% constitutional requirement)
- Test quality and performance validation

### Validation Configuration

#### Strict Mode (Production)

```kotlin
ValidationConfiguration.strict() // Requires all constitutional standards
```

#### Permissive Mode (Development)

```kotlin
ValidationConfiguration.permissive() // Allows some flexibility during development
```

#### Incremental Mode (CI/CD)

```kotlin
ValidationConfiguration.incremental() // Validates only changed components
```

### Integration with Development Workflow

#### Pre-Commit Validation

```bash
# Run quick validation before commits
./gradlew validateProductionReadiness

# Check specific components
./gradlew validatePerformance
./gradlew validateCrossPlatform
```

#### CI/CD Integration

```yaml
# GitHub Actions integration
- name: Validate Production Readiness
  run: ./gradlew allValidationTests
```

#### IDE Integration

The validation system provides real-time feedback during development through IDE plugins and build integration.

## Recent Changes
- 011-reference-https-threejs: Added Kotlin 1.9+ with Multiplatform plugin
- 010-systematically-address-all: Added Kotlin 1.9+ with Kotlin Multiplatform plugin

1. **2025-09-29**: Complete production readiness validation system implemented
2. **2025-09-29**: Constitutional compliance validation (60 FPS, 5MB, type safety, cross-platform)
3. **2025-09-29**: Performance validation tests ensuring 60 FPS requirement maintenance
4. **2025-09-29**: Cross-platform validation tests for JVM, JS, Native consistency
5. **2025-09-29**: Test suite validation ensuring >95% success rate and >80% coverage
6. **2025-09-19**: Phase 1 foundation implementation completed (math, scene graph, renderer abstraction)
7. **2025-09-19**: Advanced features specification (Phase 2-13) generated and planned
8. **2025-09-19**: Comprehensive API contracts created for geometry, materials, lighting, animation, physics, XR
9. **2025-09-19**: Research findings documented with technical decisions for all advanced features

## Next Development Steps
1. Implement advanced geometry system (procedural generation, extrusion, text rendering)
2. Develop PBR material system with comprehensive shader management
3. Create advanced lighting pipeline (IBL, area lights, cascaded shadows)
4. Build skeletal animation system with IK and state machines
5. Integrate physics engine (Rapier primary, Bullet fallback)
6. Implement XR support for VR/AR experiences
7. Add post-processing effects and optimization systems
8. Create development tools (scene editor, material editor, profilers)

## Tooling & Production Readiness (Phase 3)

### Development Tools
- **Scene Editor**: Web-based primary (Compose Multiplatform for desktop)
  - Visual scene composition and real-time preview
  - Object manipulation, property editing, asset management
  - Export to code or industry formats (GLTF, USD)

- **Material Editor**: WGSL shader development environment
  - Syntax highlighting and real-time compilation
  - Uniform tweaking with live preview
  - Material library management and sharing

- **Animation Timeline**: Keyframe editing and preview
  - Track-based animation editing
  - Bezier curve interpolation controls
  - Export to animation clips

- **Performance Monitor**: Real-time profiling and debugging
  - FPS, GPU usage, memory tracking
  - Draw call analysis and bottleneck detection
  - Frame capture and inspection

### Testing Infrastructure
- **Unit Testing**: Kotlin Test multiplatform framework
- **Integration Testing**: Platform-specific rendering validation
- **Visual Regression**: Automated screenshot comparison
- **Performance Benchmarking**: Automated performance tracking
- **Matrix Testing**: Cross-platform test execution

### Documentation System
- **API Reference**: Dokka-generated with enhanced navigation
- **Interactive Examples**: Live code samples with WebGPU execution
- **Migration Guides**: Automated Three.js to KreeKt conversion
- **Platform Guides**: Setup instructions for each target platform

### CI/CD Pipeline
- **Multi-Platform Builds**: Automated artifact generation
- **Quality Gates**: Test coverage, performance thresholds
- **Automated Publishing**: Maven Central, npm, CocoaPods, GitHub
- **Release Management**: Versioning, changelogs, distribution

### Tool Architecture Decisions
1. **Deployment Model**: Web-based primary, optional desktop apps
2. **UI Framework**: Compose Multiplatform (desktop) + Web Components (browser)
3. **Testing Matrix**: Tier 1 (Chrome, Firefox, JVM 17+, Android 24+, iOS 14+)
4. **Distribution**: Maven Central, npm, CocoaPods, GitHub Releases
5. **Performance Targets**: 60 FPS with hardware-appropriate triangle counts

### Tool Module Structure
```
tools/
├── editor/              # Scene, material, animation editors
│   ├── web/            # Web Components implementation
│   └── desktop/        # Compose Multiplatform implementation
├── profiler/           # Performance monitoring tools
├── tests/              # Testing infrastructure and frameworks
├── docs/               # Documentation generation and serving
└── cicd/               # CI/CD scripts and configurations
```

---
*Keep this file updated as implementation progresses. Focus on multiplatform considerations and Three.js compatibility.*
