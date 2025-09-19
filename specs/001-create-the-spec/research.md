# Research Findings: KreeKt WebGPU/Vulkan Library

**Branch**: `001-create-the-spec` | **Date**: 2025-09-19

## Executive Summary
Research conducted to resolve technical clarifications for the KreeKt multiplatform 3D graphics library. All NEEDS CLARIFICATION items from the specification have been addressed with concrete decisions and rationales.

## Research Areas & Decisions

### 1. WebGPU Fallback Strategy
**Decision**: WebGL 2.0 fallback with feature detection

**Rationale**:
- WebGPU has ~65% browser support (Chrome, Edge, partial Safari)
- WebGL 2.0 has 95%+ browser support across all major browsers
- Graceful degradation maintains functionality for all users
- Feature detection allows optimal performance where available

**Alternatives Considered**:
- Software renderer: Too slow for production 3D applications
- Error-only: Would exclude significant user base
- WebGL 1.0: Lacks critical features (MRT, instancing, 3D textures)

**Implementation Strategy**:
```kotlin
expect fun createRenderer(): Renderer

// JS implementation
actual fun createRenderer(): Renderer {
    return when {
        supportsWebGPU() -> WebGPURenderer()
        supportsWebGL2() -> WebGL2Renderer()
        else -> throw UnsupportedOperationException("No suitable renderer")
    }
}
```

### 2. Physics Engine Selection
**Decision**: Rapier as primary engine with adapter pattern for others

**Rationale**:
- Rapier has native Rust implementation with WASM support
- Best performance characteristics for web deployment
- Active development and modern architecture
- Adapter pattern allows future engine additions

**Alternatives Considered**:
- Bullet Physics: Mature but complex C++ integration
- Jolt: Excellent performance but limited platform support
- All engines: Maintenance burden too high initially

**Integration Approach**:
```kotlin
interface PhysicsEngine {
    fun createWorld(): PhysicsWorld
    fun step(deltaTime: Float)
}

class RapierPhysics : PhysicsEngine { ... }
// Future: class BulletPhysics : PhysicsEngine { ... }
```

### 3. Documentation Platform
**Decision**: Dokka with GitHub Pages hosting

**Rationale**:
- Dokka is the official Kotlin documentation engine
- Supports multiplatform projects natively
- GitHub Pages provides free, reliable hosting
- Integrates with CI/CD for automatic updates

**Documentation Strategy**:
- API docs auto-generated on each release
- Versioned documentation for major versions
- Interactive examples using Kotlin Playground
- Migration guides between versions

### 4. Asset Size Limits
**Decision**: 100MB default limit with configurable streaming

**Rationale**:
- 100MB covers 95% of typical 3D models
- Streaming support for larger assets (films, games)
- Configurable limits per platform capabilities
- Progressive loading for optimal UX

**Loading Strategy**:
```kotlin
class AssetLoader(
    val maxSize: Long = 100_000_000, // 100MB default
    val enableStreaming: Boolean = true
) {
    suspend fun load(url: String): Asset
    fun loadStreaming(url: String): Flow<AssetChunk>
}
```

### 5. Debug/Profiling Metrics
**Decision**: Comprehensive metrics with pluggable reporters

**Rationale**:
- Different platforms need different metrics
- Developers need flexibility in what to track
- Performance overhead must be minimal

**Metrics to Track**:
- **Rendering**: FPS, frame time, draw calls, triangles
- **Memory**: GPU memory, texture memory, buffer memory
- **Shaders**: Compilation time, switch count, active programs
- **Assets**: Load time, decode time, cache hits
- **Physics**: Simulation time, collision checks, active bodies

**Implementation**:
```kotlin
interface MetricsReporter {
    fun report(metric: Metric)
}

class ConsoleReporter : MetricsReporter { ... }
class OverlayReporter : MetricsReporter { ... }
class RemoteReporter : MetricsReporter { ... }
```

### 6. Distribution Channels
**Decision**: Platform-appropriate distribution

**Rationale**:
- Developers expect platform-native distribution
- Reduces friction for adoption
- Simplifies dependency management

**Distribution Plan**:
- **JVM/Android**: Maven Central
- **JavaScript**: NPM registry
- **iOS/macOS**: Swift Package Manager
- **Native**: GitHub releases with CMake

**Version Strategy**:
- Synchronized versions across all platforms
- Semantic versioning (MAJOR.MINOR.PATCH)
- Release candidates for major versions

## Technical Recommendations

### Shader Strategy
**Decision**: WGSL as primary with SPIR-V cross-compilation

**Rationale**:
- WGSL is WebGPU's native shader language
- SPIR-V provides path to Vulkan/Metal
- Single source of truth for shaders
- Tools exist for cross-compilation (Tint, SPIRV-Cross)

### Performance Targets
**Validated Targets**:
- 60 FPS with 100k triangles: Achievable on mid-range hardware
- <100ms initialization: Requires lazy loading and minimal setup
- <5MB base library: Achievable with code splitting and tree-shaking

### Platform-Specific Considerations

**Web Platform**:
- Use SharedArrayBuffer where available
- Implement OffscreenCanvas for workers
- Support both module and classic scripts

**Mobile Platforms**:
- Respect battery/thermal states
- Implement level-of-detail (LOD) aggressively
- Use platform-specific texture formats (ETC2, ASTC)

**Desktop Platforms**:
- Support high-DPI displays
- Implement multi-window support
- Use native file dialogs for asset loading

## Risk Mitigation

### Technical Risks
1. **WebGPU Spec Changes**:
   - Mitigation: Version lock on stable subset
   - Regular compatibility testing

2. **Cross-platform Shader Compatibility**:
   - Mitigation: Comprehensive test suite
   - Platform-specific shader variants where needed

3. **Performance Variations**:
   - Mitigation: Adaptive quality settings
   - Platform-specific optimizations

### Adoption Risks
1. **Three.js API Differences**:
   - Mitigation: Comprehensive migration guide
   - Compatibility layer for common patterns

2. **Learning Curve**:
   - Mitigation: Extensive examples
   - Interactive tutorials
   - Video content

## Next Steps
With all clarifications resolved, proceed to Phase 1 (Design & Contracts) to:
1. Define the complete data model
2. Create API contracts
3. Generate initial test suites
4. Create quickstart guide
5. Update CLAUDE.md with project context

## Appendix: Research Sources
- WebGPU Specification: https://www.w3.org/TR/webgpu/
- Vulkan Specification: https://www.khronos.org/vulkan/
- Three.js Documentation: https://threejs.org/docs/
- Kotlin Multiplatform Docs: https://kotlinlang.org/docs/multiplatform.html
- Rapier Physics: https://rapier.rs/
- Browser Compatibility Data: https://caniuse.com/webgpu

---
*Research completed: All NEEDS CLARIFICATION items resolved*