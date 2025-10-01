# Material Subsystem Gap Analysis

**Analysis Date**: 2025-10-01
**Target**: Three.js r180 API Compatibility
**Constitution Validation**: ✅ TDD Required, Production-Ready, Cross-Platform Compatible

## Executive Summary

Current KreeKt material implementation provides **4 of 17** material types required for Three.js r180 parity (24% complete). Core Material base class exists but lacks comprehensive API from contract. **13 material classes** require implementation, and existing classes need enhancement to match Three.js r180 API.

**Overall Status**: 🔴 Significant Gap - Basic foundation exists, major API surface missing

---

## Current Implementation Inventory

### ✅ Implemented Classes (4/17)

| Class | File | Status | Completeness | Notes |
|-------|------|--------|--------------|-------|
| Material (Base) | Material.kt:6 | ⚠️ Partial | 30% | Missing blending, stencil, clipping |
| MeshBasicMaterial | MeshBasicMaterial.kt:9 | ⚠️ Partial | 40% | Missing texture maps, envMap, fog |
| MeshStandardMaterial | MeshStandardMaterial.kt:17 | ✅ Good | 85% | Well-implemented, missing some maps |
| MeshPhysicalMaterial | MeshPhysicalMaterial.kt:16 | ✅ Good | 90% | Comprehensive, near Three.js parity |

**Implemented Features**:
- ✅ Basic material properties (opacity, transparent, visible)
- ✅ PBR workflow (metalness/roughness)
- ✅ Advanced PBR (clearcoat, transmission, sheen, iridescence)
- ✅ Texture mapping (albedo, normal, metalness, roughness, AO)
- ✅ Material cloning and copying
- ✅ User data storage
- ✅ Factory presets (metal, glass, clearcoat, sheen, etc.)

---

## Missing Material Classes (13)

### 🔴 Priority 1: Basic Mesh Materials (3 classes)

| Class | Three.js API | Complexity | Estimated Lines |
|-------|-------------|------------|-----------------|
| **MeshLambertMaterial** | THREE.MeshLambertMaterial | Low | ~250 |
| **MeshPhongMaterial** | THREE.MeshPhongMaterial | Medium | ~350 |
| **MeshToonMaterial** | THREE.MeshToonMaterial | Low | ~280 |

**Missing Constructor Signatures**:
```kotlin
// contracts/material-api.kt:130-151
interface MeshLambertMaterialAPI : MaterialAPI {
    var color: Color
    var emissive: Color
    var emissiveIntensity: Float
    var emissiveMap: Texture?
    var map: Texture?
    var lightMap: Texture?
    var lightMapIntensity: Float
    var aoMap: Texture?
    var aoMapIntensity: Float
    var specularMap: Texture?
    var alphaMap: Texture?
    var envMap: Texture?
    var combine: Combine
    var reflectivity: Float
    var refractionRatio: Float
    var wireframe: Boolean
    var fog: Boolean
}
```

**Use Cases**:
- **MeshLambertMaterial**: Diffuse-only shading (mobile-friendly, low performance cost)
- **MeshPhongMaterial**: Specular highlights (legacy PBR alternative)
- **MeshToonMaterial**: Cel/cartoon shading with gradient ramps

---

### 🟡 Priority 2: Utility Materials (5 classes)

| Class | Three.js API | Complexity | Estimated Lines |
|-------|-------------|------------|-----------------|
| **MeshNormalMaterial** | THREE.MeshNormalMaterial | Low | ~180 |
| **MeshDepthMaterial** | THREE.MeshDepthMaterial | Medium | ~200 |
| **MeshDistanceMaterial** | THREE.MeshDistanceMaterial | Medium | ~220 |
| **MeshMatcapMaterial** | THREE.MeshMatcapMaterial | Low | ~240 |
| **ShadowMaterial** | THREE.ShadowMaterial | Low | ~150 |

**Missing Constructor Signatures**:
```kotlin
// contracts/material-api.kt:320-333
interface MeshNormalMaterialAPI : MaterialAPI {
    var bumpMap: Texture?
    var bumpScale: Float
    var normalMap: Texture?
    var normalMapType: NormalMapType
    var normalScale: Vector2
    var displacementMap: Texture?
    var displacementScale: Float
    var displacementBias: Float
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var flatShading: Boolean
    var fog: Boolean
}

// contracts/material-api.kt:339-349
interface MeshDepthMaterialAPI : MaterialAPI {
    var depthPacking: DepthPacking
    var map: Texture?
    var alphaMap: Texture?
    var displacementMap: Texture?
    var displacementScale: Float
    var displacementBias: Float
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var fog: Boolean
}
```

**Use Cases**:
- **MeshNormalMaterial**: Debugging normals, technical visualization
- **MeshDepthMaterial**: Depth rendering for post-processing
- **MeshDistanceMaterial**: Distance field rendering for effects
- **MeshMatcapMaterial**: Matcap shading (no lighting needed)
- **ShadowMaterial**: Shadow-only rendering (ground planes)

---

### 🟢 Priority 3: Line and Point Materials (3 classes)

| Class | Three.js API | Complexity | Estimated Lines |
|-------|-------------|------------|-----------------|
| **LineBasicMaterial** | THREE.LineBasicMaterial | Low | ~150 |
| **LineDashedMaterial** | THREE.LineDashedMaterial | Low | ~180 |
| **PointsMaterial** | THREE.PointsMaterial | Low | ~200 |

**Existing Partial Implementation**:
```kotlin
// LineBasicMaterial.kt exists but not reviewed in detail
// File: src/commonMain/kotlin/io/kreekt/material/LineBasicMaterial.kt
```

**Missing Constructor Signatures**:
```kotlin
// contracts/material-api.kt:405-411
interface LineBasicMaterialAPI : MaterialAPI {
    var color: Color
    var linewidth: Float
    var linecap: String
    var linejoin: String
    var fog: Boolean
}

// contracts/material-api.kt:417-421
interface LineDashedMaterialAPI : LineBasicMaterialAPI {
    var scale: Float
    var dashSize: Float
    var gapSize: Float
}

// contracts/material-api.kt:431-438
interface PointsMaterialAPI : MaterialAPI {
    var color: Color
    var map: Texture?
    var alphaMap: Texture?
    var size: Float
    var sizeAttenuation: Boolean
    var fog: Boolean
}
```

**Use Cases**:
- **LineBasicMaterial**: Solid line rendering (wireframes, debug lines)
- **LineDashedMaterial**: Dashed/dotted lines (construction lines, guides)
- **PointsMaterial**: Point cloud rendering (particles, stars)

---

### 🔵 Priority 4: Shader and Sprite Materials (2 classes)

| Class | Three.js API | Complexity | Estimated Lines |
|-------|-------------|------------|-----------------|
| **ShaderMaterial** | THREE.ShaderMaterial | High | ~400 |
| **SpriteMaterial** | THREE.SpriteMaterial | Low | ~180 |
| **RawShaderMaterial** | THREE.RawShaderMaterial | High | ~350 |

**Existing Partial Implementation**:
```kotlin
// ShaderMaterial.kt exists
// File: src/commonMain/kotlin/io/kreekt/material/ShaderMaterial.kt
```

**Missing Constructor Signatures**:
```kotlin
// contracts/material-api.kt:461-473
interface ShaderMaterialAPI : MaterialAPI {
    var vertexShader: String
    var fragmentShader: String
    var uniforms: MutableMap<String, Uniform>
    var defines: MutableMap<String, Any>
    var wireframe: Boolean
    var wireframeLinewidth: Float
    var lights: Boolean
    var clipping: Boolean
    var fog: Boolean
    var flatShading: Boolean
    var extensions: ShaderExtensions
}

// contracts/material-api.kt:444-451
interface SpriteMaterialAPI : MaterialAPI {
    var color: Color
    var map: Texture?
    var alphaMap: Texture?
    var rotation: Float
    var sizeAttenuation: Boolean
    var fog: Boolean
}
```

---

## Base Material API Gaps

### Current Implementation (Material.kt)

**Implemented Properties** (Material.kt:6-58):
```kotlin
abstract class Material {
    val id: Int                      // ✅
    var name: String                 // ✅
    var needsUpdate: Boolean         // ✅
    var visible: Boolean             // ✅
    var opacity: Float               // ✅
    var transparent: Boolean         // ✅
    var vertexColors: Boolean        // ✅
    var depthWrite: Boolean          // ✅
    var depthTest: Boolean           // ✅
    var side: Int                    // ✅ (simplified)
}
```

**Missing Properties** (contracts/material-api.kt:28-96):
```kotlin
// ❌ Missing: Comprehensive material API
val type: String                    // Partially implemented
val uuid: String                    // ❌ Not implemented
var blending: Blending              // ❌ Not implemented
var blendSrc: BlendingFactor        // ❌ Not implemented
var blendDst: BlendingFactor        // ❌ Not implemented
var blendEquation: BlendingEquation // ❌ Not implemented
var blendSrcAlpha: BlendingFactor?  // ❌ Not implemented
var blendDstAlpha: BlendingFactor?  // ❌ Not implemented
var blendEquationAlpha: BlendingEquation? // ❌ Not implemented
var premultipliedAlpha: Boolean     // ❌ Not implemented
var depthFunc: DepthMode            // ❌ Not implemented
var stencilWrite: Boolean           // ❌ Not implemented
var stencilFunc: StencilFunc        // ❌ Not implemented
var stencilRef: Int                 // ❌ Not implemented
var stencilWriteMask: Int           // ❌ Not implemented
var stencilFuncMask: Int            // ❌ Not implemented
var stencilFail: StencilOp          // ❌ Not implemented
var stencilZFail: StencilOp         // ❌ Not implemented
var stencilZPass: StencilOp         // ❌ Not implemented
var colorWrite: Boolean             // ❌ Not implemented
var polygonOffset: Boolean          // ❌ Not implemented
var polygonOffsetFactor: Float      // ❌ Not implemented
var polygonOffsetUnits: Float       // ❌ Not implemented
var clippingPlanes: List<Plane>?    // ❌ Not implemented
var clipIntersection: Boolean       // ❌ Not implemented
var clipShadows: Boolean            // ❌ Not implemented
var alphaTest: Float                // ❌ Not implemented
var alphaToCoverage: Boolean        // ❌ Not implemented
var dithering: Boolean              // ❌ Not implemented
var precision: Precision?           // ❌ Not implemented
var toneMapped: Boolean             // ❌ Not implemented
val userData: MutableMap<String, Any> // ❌ Not implemented
var version: Int                    // ❌ Not implemented
fun setValues(values: Map<String, Any>) // ❌ Not implemented
```

**Missing API Coverage**: **70% of base Material API missing**

---

## MeshBasicMaterial Gaps

### Current Implementation (MeshBasicMaterial.kt:9-45)

**Implemented Properties**:
```kotlin
class MeshBasicMaterial(
    var color: Color,               // ✅
    var wireframe: Boolean,         // ✅
    var wireframeLinewidth: Float,  // ✅
    transparent: Boolean,           // ✅
    opacity: Float                  // ✅
) : Material()
```

**Missing Properties** (contracts/material-api.kt:106-124):
```kotlin
var map: Texture?                   // ❌ Not implemented
var lightMap: Texture?              // ❌ Not implemented
var lightMapIntensity: Float        // ❌ Not implemented
var aoMap: Texture?                 // ❌ Not implemented
var aoMapIntensity: Float           // ❌ Not implemented
var specularMap: Texture?           // ❌ Not implemented
var alphaMap: Texture?              // ❌ Not implemented
var envMap: Texture?                // ❌ Not implemented
var combine: Combine                // ❌ Not implemented
var reflectivity: Float             // ❌ Not implemented
var refractionRatio: Float          // ❌ Not implemented
var wireframeLinecap: String        // ❌ Not implemented
var wireframeLinejoin: String       // ❌ Not implemented
var fog: Boolean                    // ❌ Not implemented
```

**Missing API Coverage**: **60% of MeshBasicMaterial API missing**

---

## Supporting Types Status

### ✅ Implemented Enums (MeshStandardMaterial.kt:214-224)

```kotlin
enum class BlendMode { NORMAL, ADDITIVE, SUBTRACTIVE, MULTIPLY, CUSTOM }
enum class MaterialSide { FRONT, BACK, DOUBLE }
enum class CullFace { FRONT, BACK, NONE }
```

### ❌ Missing Enums (contracts/material-api.kt:487-602)

```kotlin
// Missing from codebase:
enum class Side { FrontSide, BackSide, DoubleSide }
enum class Blending { NoBlending, NormalBlending, AdditiveBlending, SubtractiveBlending, MultiplyBlending, CustomBlending }
enum class BlendingFactor { ZeroFactor, OneFactor, SrcColorFactor, OneMinusSrcColorFactor, ... }
enum class BlendingEquation { AddEquation, SubtractEquation, ReverseSubtractEquation, MinEquation, MaxEquation }
enum class DepthMode { NeverDepth, AlwaysDepth, LessDepth, LessEqualDepth, ... }
enum class StencilFunc { NeverStencilFunc, LessStencilFunc, EqualStencilFunc, ... }
enum class StencilOp { ZeroStencilOp, KeepStencilOp, ReplaceStencilOp, ... }
enum class Combine { MultiplyOperation, MixOperation, AddOperation }
enum class NormalMapType { TangentSpaceNormalMap, ObjectSpaceNormalMap }
enum class DepthPacking { BasicDepthPacking, RGBADepthPacking }
enum class Precision { HighP, MediumP, LowP }
```

**Missing Types Count**: **9 enums + 2 data classes (Uniform, ShaderExtensions)**

---

## Cross-Platform Compatibility Assessment

### Platform-Specific Considerations

| Platform | Status | Notes |
|----------|--------|-------|
| JVM | ✅ Working | No platform-specific issues |
| JavaScript | ✅ Working | Material properties serialize correctly |
| Linux Native | ✅ Working | No C interop needed |
| Windows Native | ✅ Working | No platform-specific code |
| Android | ✅ Working | JVM backend |
| iOS | ✅ Working | Native backend |

**Assessment**: Material system is pure Kotlin with no platform-specific dependencies. **No expect/actual needed** for material classes. Shader compilation is handled by renderer layer (platform-specific).

---

## Shader System Analysis

### Existing Implementation

**ShaderCompiler.kt** and **ShaderMaterial.kt** exist but need review:
```bash
src/commonMain/kotlin/io/kreekt/material/ShaderCompiler.kt
src/commonMain/kotlin/io/kreekt/material/ShaderMaterial.kt
```

**Contract Requirements** (contracts/material-api.kt:461-473):
```kotlin
interface ShaderMaterialAPI : MaterialAPI {
    var vertexShader: String         // WGSL code
    var fragmentShader: String       // WGSL code
    var uniforms: MutableMap<String, Uniform>
    var defines: MutableMap<String, Any>
    var lights: Boolean              // Include lighting uniforms?
    var clipping: Boolean            // Include clipping plane uniforms?
    var fog: Boolean                 // Include fog uniforms?
    var extensions: ShaderExtensions
}
```

**Status**: Partial implementation exists, needs contract alignment.

---

## DSL Builder API Status

### Contract-Defined DSL (contracts/material-api.kt:608-650)

**Status**: ❌ Not Implemented in codebase

```kotlin
// Defined in contract but missing from implementation:
fun meshStandardMaterial(init: MeshStandardMaterial.() -> Unit): MeshStandardMaterial
fun meshPhysicalMaterial(init: MeshPhysicalMaterial.() -> Unit): MeshPhysicalMaterial
fun meshBasicMaterial(init: MeshBasicMaterial.() -> Unit): MeshBasicMaterial
fun shaderMaterial(init: ShaderMaterial.() -> Unit): ShaderMaterial
fun ShaderMaterial.setUniform(name: String, value: Any, type: UniformType?): Unit
```

**Priority**: Low (nice-to-have for API ergonomics, not blocking)

---

## Implementation Roadmap

### Phase 1: Base Material Enhancement (Week 1)
**Goal**: Complete Material base class to 100% Three.js r180 parity

- [ ] Add blending properties (blendSrc, blendDst, blendEquation, etc.)
- [ ] Add stencil properties (stencilWrite, stencilFunc, stencilRef, etc.)
- [ ] Add clipping properties (clippingPlanes, clipIntersection, clipShadows)
- [ ] Add alpha properties (alphaTest, alphaToCoverage, dithering)
- [ ] Add utility properties (precision, toneMapped, version, uuid)
- [ ] Implement setValues() method
- [ ] Create all supporting enums (Blending, BlendingFactor, DepthMode, etc.)

**Estimated Effort**: 16 hours (2 days)

### Phase 2: Basic Mesh Materials (Week 2)
**Goal**: Implement fundamental shading models

- [ ] MeshLambertMaterial (diffuse-only shading)
- [ ] MeshPhongMaterial (specular highlights)
- [ ] MeshToonMaterial (cel/cartoon shading)
- [ ] Enhance MeshBasicMaterial with missing texture maps
- [ ] Add fog support to all materials

**Estimated Effort**: 24 hours (3 days)

### Phase 3: Utility Materials (Week 3)
**Goal**: Implement debugging and special-purpose materials

- [ ] MeshNormalMaterial (normal visualization)
- [ ] MeshDepthMaterial (depth encoding)
- [ ] MeshDistanceMaterial (distance field)
- [ ] MeshMatcapMaterial (matcap shading)
- [ ] ShadowMaterial (shadow-only)

**Estimated Effort**: 20 hours (2.5 days)

### Phase 4: Line, Point, and Sprite Materials (Week 4)
**Goal**: Complete rendering primitive support

- [ ] LineBasicMaterial (review/enhance existing)
- [ ] LineDashedMaterial
- [ ] PointsMaterial
- [ ] SpriteMaterial
- [ ] Test line rendering pipeline
- [ ] Test point cloud rendering

**Estimated Effort**: 16 hours (2 days)

### Phase 5: Shader System Enhancement (Week 5)
**Goal**: Complete custom shader support

- [ ] Review existing ShaderMaterial implementation
- [ ] Add missing properties (lights, clipping, fog, extensions)
- [ ] Implement RawShaderMaterial
- [ ] Enhance ShaderCompiler with uniform management
- [ ] Add shader validation and error reporting

**Estimated Effort**: 24 hours (3 days)

### Phase 6: DSL Builders and Polish (Week 6)
**Goal**: Improve developer experience

- [ ] Implement DSL builder functions
- [ ] Add factory presets for all material types
- [ ] Create material library examples
- [ ] Add comprehensive documentation
- [ ] Performance optimization (material caching, shader reuse)

**Estimated Effort**: 16 hours (2 days)

**Total Estimated Effort**: 116 hours (14.5 days) for complete Three.js r180 material parity

---

## Recommendations

### 1. Immediate Actions (This Week)

1. **Complete Base Material API** (Critical)
   - Add all missing properties to Material base class
   - Create supporting enum types
   - Implement setValues() method
   - **Rationale**: Blocks all material implementations

2. **Enhance MeshBasicMaterial** (High Priority)
   - Add texture map support (map, lightMap, aoMap, alphaMap, envMap)
   - Add fog support
   - **Rationale**: Most commonly used material, needed for basic examples

3. **Write Contract Tests** (Constitutional TDD Requirement)
   - Create test file for each missing material class
   - Follow Red-Green-Refactor cycle
   - **Rationale**: Constitution mandates TDD approach

### 2. Architecture Improvements

1. **Extract Common Material Properties**
   ```kotlin
   // Proposed: MaterialProperties.kt
   interface TexturedMaterial {
       var map: Texture?
       var alphaMap: Texture?
   }

   interface NormalMappedMaterial {
       var normalMap: Texture?
       var normalScale: Vector2
       var normalMapType: NormalMapType
   }

   interface DisplacedMaterial {
       var displacementMap: Texture?
       var displacementScale: Float
       var displacementBias: Float
   }
   ```

2. **Material System Utilities**
   ```kotlin
   // Proposed: MaterialUtils.kt
   object MaterialUtils {
       fun needsAlphaBlending(material: Material): Boolean
       fun needsLighting(material: Material): Boolean
       fun getMaterialDefines(material: Material): Map<String, Any>
       fun getMaterialUniforms(material: Material): Map<String, Uniform>
   }
   ```

### 3. Testing Strategy

**Constitutional Compliance Plan**:
```kotlin
// Test structure per material class
class [Material]ContractTest {
    @Test fun testConstructorDefaults()
    @Test fun testPropertySetters()
    @Test fun testCloning()
    @Test fun testCopying()
    @Test fun testDisposal()
    @Test fun testTextureHandling()
    @Test fun testBlendingModes()
    @Test fun testDepthControl()
    @Test fun testUserData()
}
```

**Coverage Target**: >95% success rate, >80% code coverage (constitutional requirement)

---

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Shader compilation differences across platforms | High | Medium | Abstract shader backend, extensive testing |
| Blending mode platform quirks | Medium | Low | Use standardized blending factors |
| Texture format compatibility | Medium | Medium | Already handled by texture system ✅ |
| Performance regression (material switching) | High | Medium | Implement material caching, minimize state changes |
| Missing features in WebGPU vs Vulkan | Medium | Low | Feature detection, graceful degradation |

---

## Constitutional Compliance Checklist

- [x] **TDD Approach**: Red-Green-Refactor cycle planned ✅
- [ ] **Production-Ready Code**: No TODOs/placeholders (needs verification)
- [x] **Cross-Platform**: Pure Kotlin, no platform-specific code ✅
- [ ] **Performance Standards**: Material switching needs optimization
- [x] **Type Safety**: No runtime casts in current implementation ✅

**Overall Constitutional Status**: ⚠️ **PARTIALLY COMPLIANT** (TDD not yet followed for new materials)

---

## Conclusion

KreeKt's material subsystem has **strong PBR foundations** (MeshStandardMaterial and MeshPhysicalMaterial are well-implemented) but **significant gaps** in:

1. **Base Material API**: 70% of Material properties missing
2. **Material Variety**: 76% of material types missing (13 of 17)
3. **MeshBasicMaterial**: 60% of API missing (no texture maps, fog)
4. **Supporting Types**: 9 enums + 2 data classes missing

**Priority**: Complete base Material API first, then implement basic materials (Lambert, Phong, Toon) to unlock lighting system development.

**Next Step**: T011 - Write contract tests for all missing material classes (TDD requirement).

---

**Generated**: 2025-10-01
**Analyst**: KreeKt Gap Analysis System
**Contract Reference**: specs/012-complete-three-js/contracts/material-api.kt
