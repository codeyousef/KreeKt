# Research: Three.js Feature Parity Implementation

**Feature**: Complete Three.js Feature Parity
**Date**: 2025-09-30
**Researcher**: AI Planning Agent

## Executive Summary

This research document consolidates findings on implementing 25 major Three.js feature categories in KreeKt. Research focused on cross-platform implementation strategies, performance optimization techniques, and API design patterns for Kotlin Multiplatform. All technical decisions support the goal of 95%+ Three.js r180 API compatibility while maintaining 60 FPS performance with 100k+ triangles across 7 target platforms.

## Research Areas

### 1. Audio System Platform APIs

**Decision**: Use platform-specific audio APIs with common abstraction layer

**Rationale**:
- Web Audio API provides comprehensive spatialization on JS platform
- OpenAL offers cross-platform 3D audio for JVM/Native
- Platform-native APIs (AudioTrack, AVAudioEngine) provide optimal performance
- Common interface enables Three.js-compatible API

**Platform Implementations**:

| Platform | API | Key Features |
|----------|-----|--------------|
| JS/Web | Web Audio API | PannerNode, AudioListener, Doppler, Distance models |
| JVM | OpenAL Soft | 3D positioning, Distance attenuation, Doppler effect |
| Android | AudioTrack + Oboe | Low-latency audio, Spatial audio APIs |
| iOS | AVAudioEngine | AVAudio3DNode, Spatial audio, AirPods support |
| Linux/Win | OpenAL | Cross-platform 3D audio |

**Implementation Pattern**:
```kotlin
// Common API
expect class AudioListener
expect class PositionalAudio

// Platform-specific
actual class AudioListener(camera: Camera) {
    // JS: Web Audio API AudioListener
    // JVM: OpenAL listener setup
    // etc.
}
```

**Performance Targets**:
- <10ms spatialization latency
- Support 32+ simultaneous audio sources
- Real-time Doppler calculation
- Distance attenuation with minimal CPU overhead

**Alternatives Considered**:
- Pure Kotlin audio engine: Rejected due to complexity and performance
- Third-party libraries: Rejected to maintain zero-dependency goal for core features

### 2. Raycasting Optimization Strategies

**Decision**: Implement BVH (Bounding Volume Hierarchy) with optional spatial partitioning

**Rationale**:
- BVH provides O(log n) intersection tests vs O(n) brute force
- Three.js uses BVH for large scenes
- GPU-accelerated picking too platform-specific for cross-platform library
- Adaptive structure supports dynamic scenes

**BVH Structure**:
- AABB (Axis-Aligned Bounding Box) nodes
- Surface Area Heuristic (SAH) for tree construction
- Incremental updates for dynamic objects
- Cache-friendly memory layout

**Optimization Techniques**:

1. **Broad Phase**:
   - Frustum culling before raycasting
   - Layer-based filtering
   - Early-out for empty nodes

2. **Narrow Phase**:
   - Triangle intersection with Möller-Trumbore algorithm
   - SIMD optimizations for batch testing
   - Backface culling option

3. **Instanced Mesh Support**:
   - Per-instance AABB testing
   - Transform ray to instance space
   - Return instance ID with intersection

**Performance Targets**:
- 10,000 objects tested per frame at 60 FPS
- <5% CPU overhead for BVH maintenance
- <16ms ray-triangle intersection for typical scenes

**Code Pattern**:
```kotlin
class Raycaster(
    val origin: Vector3,
    val direction: Vector3
) {
    fun intersectObjects(
        objects: List<Object3D>,
        recursive: Boolean = true
    ): List<Intersection> {
        // Use BVH if available, fallback to brute force
    }
}
```

**Alternatives Considered**:
- Octree: More memory, similar performance, harder to update
- k-d tree: Better for static scenes, worse for dynamic
- Grid: Simple but wastes memory on non-uniform distributions

### 3. Curve Tessellation Algorithms

**Decision**: Adaptive subdivision based on curvature and screen-space error

**Rationale**:
- Balances visual quality with performance
- Three.js uses fixed subdivision (often wasteful)
- Curvature-based subdivision concentrates detail where needed
- Screen-space error metric adapts to camera distance

**Tessellation Strategy**:

1. **Initial Subdivision**:
   - Bezier curves: De Casteljau's algorithm
   - Catmull-Rom: Matrix-based evaluation
   - Ellipse/Arc: Trigonometric evaluation

2. **Adaptive Refinement**:
   - Measure maximum deviation from chord
   - Split if deviation exceeds tolerance
   - Recursively refine until smooth

3. **Screen-Space Optimization**:
   - Project control points to screen space
   - Calculate pixel-space error
   - Reduce subdivision for distant curves

**Arc Length Parametrization**:
- Precompute length table for uniform sampling
- Binary search for parameter mapping
- Cache results for repeated queries

**Performance Targets**:
- 1000+ curves rendered at 60 FPS
- <0.1 pixel maximum screen-space error
- <100ms for complex path generation

**Implementation Classes**:
```kotlin
abstract class Curve {
    abstract fun getPoint(t: Float): Vector3
    abstract fun getTangent(t: Float): Vector3
    abstract fun getLength(): Float

    fun getSpacedPoints(divisions: Int): List<Vector3> {
        // Arc-length parametrized sampling
    }
}

class CatmullRomCurve3(
    points: List<Vector3>,
    closed: Boolean = false,
    curveType: CurveType = CurveType.CENTRIPETAL
) : Curve()
```

**Alternatives Considered**:
- Fixed subdivision: Wasteful, not distance-adaptive
- GPU tessellation shaders: Not available on WebGL/Mobile
- Analytical methods: Limited curve types

### 4. Texture Compression Formats

**Decision**: Platform-specific format selection with runtime detection

**Rationale**:
- No single format supported across all platforms
- Quality/size tradeoffs vary by content type
- Runtime capability detection ensures compatibility
- Preprocessing tools generate multiple formats

**Format Support Matrix**:

| Format | Platforms | Use Case | Compression | Quality |
|--------|-----------|----------|-------------|---------|
| BC7/DXT | Desktop (JVM, Win) | General RGBA | 4:1 to 6:1 | Excellent |
| ETC2 | Android, Linux | General RGBA | 4:1 | Good |
| ASTC | Android 8+, iOS | Adaptive | 2:1 to 32:1 | Excellent |
| PVRTC | iOS (fallback) | General | 4:1 | Fair |
| Uncompressed | Fallback | All platforms | 1:1 | Perfect |

**Runtime Selection**:
```kotlin
class CompressedTexture(
    val formats: Map<TextureFormat, ByteArray>
) {
    fun selectFormat(capabilities: RenderCapabilities): TextureFormat {
        return when {
            capabilities.supportsASTC -> TextureFormat.ASTC_4x4
            capabilities.supportsETC2 -> TextureFormat.ETC2_RGBA8
            capabilities.supportsBC7 -> TextureFormat.BC7_RGBA
            else -> TextureFormat.RGBA8_UNCOMPRESSED
        }
    }
}
```

**Asset Pipeline**:
1. Author textures in high quality (PNG, EXR)
2. Build-time compression to multiple formats
3. Bundle format selection based on target
4. Runtime fallback if format unsupported

**Memory Savings**:
- 4K texture: 64MB → 16MB (BC7/ETC2)
- 4K texture: 64MB → 2-8MB (ASTC adaptive)
- Typical scene: 75-85% memory reduction

**Alternatives Considered**:
- Single format: Poor cross-platform support
- Runtime compression: Too slow, poor quality
- JPEG/PNG: No alpha, slow decompression

### 5. GPU Instancing Best Practices

**Decision**: Use native GPU instancing with dynamic buffer updates

**Rationale**:
- Single draw call for thousands of instances
- Per-instance data via vertex attributes
- Three.js InstancedMesh API is battle-tested
- Frustum culling per instance saves bandwidth

**Instancing Architecture**:

```kotlin
class InstancedMesh(
    geometry: BufferGeometry,
    material: Material,
    count: Int
) : Mesh(geometry, material) {
    val instanceMatrix: InstancedBufferAttribute  // 4x4 transform per instance
    val instanceColor: InstancedBufferAttribute?   // Optional per-instance color

    fun setMatrixAt(index: Int, matrix: Matrix4)
    fun setColorAt(index: Int, color: Color)
}
```

**Buffer Management**:
1. **Static Instances**:
   - Upload once, never update
   - GPU memory only
   - Fastest rendering

2. **Dynamic Instances**:
   - Partial buffer updates
   - Map/unmap buffer regions
   - Dirty tracking for efficiency

3. **Streaming Instances**:
   - Circular buffer approach
   - Update subset each frame
   - For particle systems

**Frustum Culling**:
- Per-instance AABB testing
- Build instance visibility mask
- Use indirect drawing when supported
- Fallback to CPU-side mask

**Platform Specifics**:
- **WebGPU**: Instance buffer + `@builtin(instance_index)`
- **Vulkan**: VkBuffer with instance input rate
- **OpenGL**: `glDrawElementsInstanced` with divisor

**Performance Targets**:
- 50,000 instances at 60 FPS (static)
- 10,000 instances at 60 FPS (dynamic updates)
- <1ms frustum culling for 50k instances

**Alternatives Considered**:
- Geometry instancing: Not supported on all platforms
- Pseudo-instancing (manual batching): Much slower
- Compute shader culling: Not available WebGL

### 6. Morph Target Shader Optimization

**Decision**: Dynamic shader generation based on active morph target count

**Rationale**:
- Shader complexity scales with target count
- Most models use <10 targets
- Pre-compile common configurations
- Runtime generation for exotic cases

**Shader Generation Strategy**:

1. **Template Shader**:
   - Base vertex/fragment shaders
   - Morph target injection points
   - Conditional compilation based on features

2. **Morph Attribute Layout**:
```wgsl
struct VertexInput {
    @location(0) position: vec3f,
    @location(1) normal: vec3f,
    @location(2) uv: vec2f,
    // Morph targets dynamically added:
    @location(3) morphTarget0Position: vec3f,
    @location(4) morphTarget0Normal: vec3f,
    // ... up to 8 targets
}
```

3. **Vertex Shader Morph**:
```wgsl
var morphedPosition = position;
var morphedNormal = normal;

// Unrolled loop for active targets
morphedPosition += morphTarget0Position * morphInfluences[0];
morphedNormal += morphTarget0Normal * morphInfluences[0];
morphedPosition += morphTarget1Position * morphInfluences[1];
// ...
```

**Optimization Techniques**:
- Shader caching by morph target count
- Zero-influence targets disabled at runtime
- Packed morph attributes (8-bit or 16-bit)
- Combined skinning + morph in single pass

**Memory Layout**:
- Interleaved: Better cache coherency
- Separate buffers: Easier updates
- Choose based on update frequency

**Performance Targets**:
- 4 morph targets: <5% overhead vs base mesh
- 8 morph targets: <10% overhead vs base mesh
- <50ms shader compilation per configuration

**Combined Skinning + Morph**:
```kotlin
class SkinnedMesh(
    geometry: BufferGeometry,
    material: Material
) : Mesh(geometry, material) {
    val skeleton: Skeleton
    val morphTargetInfluences: FloatArray?

    // Shader applies skinning THEN morphing
}
```

**Alternatives Considered**:
- Compute shader morphing: Not available WebGL
- CPU-side morphing: Too slow for realtime
- Fixed target count: Wasteful for simple models

### 7. Clipping Plane Implementation

**Decision**: Use gl_ClipDistance in vertex shaders with platform fallbacks

**Rationale**:
- Hardware clipping is fastest
- Three.js uses this approach
- Fragment discard fallback for unsupported platforms
- Up to 8 planes supported (WebGL2/Vulkan limit)

**Shader Integration**:

**Vertex Shader** (WGSL):
```wgsl
struct ClippingPlanes {
    planes: array<vec4f, 8>,  // xyz = normal, w = constant
    numPlanes: u32,
}

@group(0) @binding(3) var<uniform> clipping: ClippingPlanes;

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) @interpolate(linear) clipDistances: vec4f,  // Pack 4 distances
}

@vertex
fn vertexMain(input: VertexInput) -> VertexOutput {
    var output: VertexOutput;
    let worldPos = modelMatrix * vec4f(input.position, 1.0);

    // Calculate clip distances
    output.clipDistances = vec4f(
        dot(worldPos.xyz, clipping.planes[0].xyz) + clipping.planes[0].w,
        dot(worldPos.xyz, clipping.planes[1].xyz) + clipping.planes[1].w,
        dot(worldPos.xyz, clipping.planes[2].xyz) + clipping.planes[2].w,
        dot(worldPos.xyz, clipping.planes[3].xyz) + clipping.planes[3].w
    );

    output.position = projectionMatrix * viewMatrix * worldPos;
    return output;
}
```

**Fragment Shader** (discard fallback):
```wgsl
@fragment
fn fragmentMain(input: VertexOutput) -> @location(0) vec4f {
    // Discard if behind any clipping plane
    if (input.clipDistances.x < 0.0 ||
        input.clipDistances.y < 0.0 ||
        input.clipDistances.z < 0.0 ||
        input.clipDistances.w < 0.0) {
        discard;
    }

    // Regular fragment shading...
}
```

**API Design**:
```kotlin
class ClippingPlane(
    val normal: Vector3,
    val constant: Float
) {
    fun distanceToPoint(point: Vector3): Float =
        normal.dot(point) + constant
}

// Usage in material
material.clippingPlanes = listOf(
    ClippingPlane(Vector3(0, 1, 0), 0.0)  // Clip below Y=0
)
```

**Platform Specifics**:
- **WebGPU/Vulkan**: Native gl_ClipDistance support
- **WebGL**: Fragment discard (slower but works)
- **Fallback**: CPU-side geometry clipping (very slow)

**Performance Impact**:
- Hardware clipping: <1% overhead
- Fragment discard: 5-10% overhead (fill-rate dependent)
- CPU clipping: 100-500% overhead (not recommended)

**Alternatives Considered**:
- Stencil buffer clipping: Limited to simple cases
- Boolean geometry operations: Too slow for realtime
- Shader-only approach: Good but needs fragment discard

### 8. Render Target Management

**Decision**: Pool-based framebuffer management with automatic sizing

**Rationale**:
- Framebuffer creation/destruction is expensive
- Post-processing chains reuse targets
- Automatic sizing adapts to window resize
- Three.js WebGLRenderTarget API is proven

**Render Target Architecture**:

```kotlin
class WebGLRenderTarget(
    width: Int,
    height: Int,
    options: RenderTargetOptions = RenderTargetOptions()
) : EventDispatcher() {
    var texture: Texture
    var depthBuffer: Boolean = options.depthBuffer
    var stencilBuffer: Boolean = options.stencilBuffer
    var depthTexture: DepthTexture? = options.depthTexture

    fun setSize(width: Int, height: Int)
    fun dispose()
}

data class RenderTargetOptions(
    val format: TextureFormat = TextureFormat.RGBA8,
    val type: TextureType = TextureType.UNSIGNED_BYTE,
    val depthBuffer: Boolean = true,
    val stencilBuffer: Boolean = false,
    val generateMipmaps: Boolean = false,
    val samples: Int = 1  // MSAA sample count
)
```

**Framebuffer Pooling**:
1. **Allocation**:
   - Request target by size/format
   - Reuse from pool if available
   - Create new if pool empty

2. **Release**:
   - Return to pool (don't dispose)
   - Automatic cleanup after N frames unused
   - Size/format bucketing for reuse

3. **Resize Handling**:
   - Detect window size changes
   - Regenerate targets with new dimensions
   - Keep aspect ratio for effects

**Multiple Render Targets (MRT)**:
```kotlin
class WebGLMultipleRenderTargets(
    width: Int,
    height: Int,
    count: Int,
    options: RenderTargetOptions = RenderTargetOptions()
) : WebGLRenderTarget(width, height, options) {
    val textures: Array<Texture> = Array(count) { Texture() }

    // Render to multiple textures in single pass
}
```

**Common Use Cases**:
1. **Post-Processing**:
   - Scene → RenderTarget
   - RenderTarget → Post-effect → RenderTarget
   - Final RenderTarget → Screen

2. **Shadows**:
   - Depth → Shadow Map RenderTarget
   - Shadow Map → Scene shader

3. **Reflections**:
   - CubeCamera → CubeRenderTarget
   - CubeRenderTarget → Environment map

4. **Deferred Rendering**:
   - Scene → MRT (albedo, normal, depth)
   - MRT textures → Lighting pass → Screen

**Performance Best Practices**:
- Minimize target switching (batch by target)
- Use appropriate precision (R8/R16/R32)
- Enable mipmaps only when needed
- Clear only required buffers
- Use same size targets to avoid rebinds

**Memory Management**:
- 1920x1080 RGBA8: 8MB per target
- With depth: +4MB
- MSAA 4x: 4x memory usage
- Pool size limits total memory

**Alternatives Considered**:
- No pooling: Wasteful, fragmented memory
- Fixed pool size: Inflexible for dynamic scenes
- Texture arrays: Not well supported across platforms

## Implementation Priority

Based on research findings, recommended implementation order:

### Phase 1 (High Priority - Weeks 1-3)
1. **Raycasting** - Foundational for interaction
2. **RenderTarget** - Required for post-processing
3. **Fog** - Simple atmospheric effects
4. **Helper Objects** - Development tools

### Phase 2 (High Priority - Weeks 4-6)
1. **Advanced Textures** (Cube, Video, Canvas, Compressed)
2. **Instancing** - Performance critical
3. **Curve System** - Widely used

### Phase 3 (Medium Priority - Weeks 7-10)
1. **Points & Sprites**
2. **Morph Targets**
3. **Clipping Planes**
4. **Layers**
5. **LOD System**

### Phase 4 (Lower Priority - Weeks 11-12)
1. **Audio System**
2. **Advanced Cameras**
3. **Enhanced Lines**
4. **Shape & Path**

## Technical Decisions Summary

| Feature | Technology Choice | Key Tradeoff |
|---------|------------------|--------------|
| Audio | Platform APIs (Web Audio, OpenAL) | Complexity vs Performance |
| Raycasting | BVH with SAH | Memory vs Speed |
| Curves | Adaptive tessellation | Quality vs Performance |
| Textures | Multi-format with runtime selection | Bundle size vs Compatibility |
| Instancing | Native GPU instancing | API complexity vs Performance |
| Morph | Dynamic shader generation | Compilation time vs Runtime speed |
| Clipping | Hardware + fragment discard | Platform support vs Performance |
| RenderTarget | Pooled framebuffers | Memory vs Allocation cost |

## Risk Assessment

### Technical Risks

1. **Platform API Fragmentation**
   - Risk: Different capabilities across platforms
   - Mitigation: Feature detection + graceful degradation
   - Impact: Medium

2. **Performance Variance**
   - Risk: Mobile vs desktop performance gap
   - Mitigation: Adaptive quality settings, LOD
   - Impact: High

3. **Shader Compilation**
   - Risk: WGSL → SPIR-V translation issues
   - Mitigation: Extensive platform testing, fallback shaders
   - Impact: Medium

4. **Memory Constraints**
   - Risk: Mobile memory limits
   - Mitigation: Texture compression, streaming, pooling
   - Impact: High

### Mitigation Strategies

1. **Comprehensive Testing**:
   - Per-platform test suites
   - Performance regression tests
   - Visual consistency tests

2. **Progressive Enhancement**:
   - Core features work everywhere
   - Advanced features detect capabilities
   - Fallbacks for unsupported platforms

3. **Documentation**:
   - Platform capability matrix
   - Best practices per feature
   - Migration guide from Three.js

## References

- Three.js r180 Source Code: https://github.com/mrdoob/three.js/tree/r180
- WebGPU Specification: https://www.w3.org/TR/webgpu/
- Vulkan 1.3 Specification: https://registry.khronos.org/vulkan/
- Web Audio API: https://www.w3.org/TR/webaudio/
- OpenAL Soft Documentation: https://openal-soft.org/
- Real-Time Rendering 4th Edition (Tomas Akenine-Möller et al.)
- Game Engine Architecture 3rd Edition (Jason Gregory)

---

**Research Complete**: All technical decisions documented with rationale
**Next Phase**: Generate contracts and data models (Phase 1)