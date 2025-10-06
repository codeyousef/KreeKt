# Implementation Plan: Production-Ready WebGPU Renderer

**Branch**: `016-implement-production-ready` | **Date**: 2025-10-06 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/home/yousef/Projects/kmp/KreeKt/specs/016-implement-production-ready/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → ✅ Loaded spec.md with clarifications session
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → ✅ All clarifications resolved in Session 2025-10-06
   → Detect Project Type: Kotlin Multiplatform library (web=JS platform)
   → Set Structure Decision: Multiplatform library with platform-specific implementations
3. Fill the Constitution Check section
   → ✅ 60 FPS at 1M triangles requirement
   → ✅ <5MB library size limit
4. Evaluate Constitution Check section
   → ✅ No violations - performance and size targets align with constitution
   → Update Progress Tracking: Initial Constitution Check ✅
5. Execute Phase 0 → research.md
   → ✅ Research WebGPU API patterns, WGSL shaders, fallback strategies
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, AGENTS.md
   → ✅ Generate renderer contracts, pipeline entities, usage examples
7. Re-evaluate Constitution Check section
   → ✅ Design maintains constitutional compliance
   → Update Progress Tracking: Post-Design Constitution Check ✅
8. Plan Phase 2 → Describe task generation approach
   → ✅ TDD-based task ordering with contract tests first
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 8. Phase 2 is executed by /tasks command.

## Summary

Implement a production-ready WebGPU renderer for KreeKt that achieves feature parity with the existing WebGLRenderer while leveraging modern WebGPU API capabilities. The renderer must maintain 60 FPS at 1,000,000 triangles (1920x1080 resolution), automatically fall back to WebGLRenderer when WebGPU is unavailable, and stay within the 5MB library size constraint. Key technical components include: WGSL shader compilation, GPU buffer management, render pipeline state caching, and automatic context loss recovery.

## Technical Context

**Language/Version**: Kotlin 1.9+ with Multiplatform plugin
**Primary Dependencies**:
- @webgpu/types 0.1.40 (WebGPU TypeScript definitions for JS target)
- kotlinx-coroutines-core 1.8.0 (async operations)
- Existing KreeKt core modules (scene graph, camera, materials)

**Storage**: N/A (renderer is stateless, GPU resources managed in-memory)
**Testing**:
- Kotlin Test multiplatform framework
- Contract tests for Renderer interface compliance
- Performance benchmarks for 60 FPS validation
- Visual regression tests for feature parity

**Target Platform**: Browser (JavaScript/WebAssembly via Kotlin/JS compilation)
**Project Type**: Multiplatform library - platform-specific JS implementation
**Performance Goals**:
- 60 FPS at 1,000,000 triangles @ 1920x1080
- Renderer initialization <2000ms
- Efficient GPU memory utilization with automatic management

**Constraints**:
- Constitutional <5MB library size limit (gzipped)
- Constitutional 60 FPS performance requirement
- Must maintain API compatibility with existing Renderer interface
- Must work in browsers: Chrome 113+, Firefox 121+, Safari 18+ (WebGPU 1.0)
- Automatic fallback to WebGLRenderer when WebGPU unavailable

**Scale/Scope**:
- Single WebGPU renderer implementation (~3000-5000 LOC)
- WGSL shader library (~500-1000 LOC)
- Feature parity with WebGLRenderer (18 functional requirements)
- Integration with existing KreeKt scene graph, materials, cameras

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Constitutional Requirements from CLAUDE.md

**✅ Type Safety**:
- Status: PASS
- Rationale: Kotlin/JS with strict typing, no runtime casts
- Implementation: Use expect/actual pattern for platform abstraction

**✅ Performance (60 FPS with 100k triangles target)**:
- Status: PASS (exceeds - targets 1M triangles)
- Rationale: WebGPU's modern architecture supports high-performance rendering
- Implementation: GPU-side batching, pipeline caching, efficient buffer management
- Validation: Performance benchmarks must verify 60 FPS @ 1M triangles

**✅ Size Constraint (<5MB base library)**:
- Status: PASS
- Rationale: Renderer implementation ~3-5K LOC, WGSL shaders compact
- Implementation: Code splitting, tree-shaking compatible architecture
- Validation: Webpack bundle analysis post-compilation

**✅ Three.js Compatibility**:
- Status: PASS
- Rationale: Implements existing Renderer interface (FR-016)
- Implementation: Feature parity with WebGLRenderer (FR-008)

**✅ Modern Graphics (WebGPU-first with fallbacks)**:
- Status: PASS
- Rationale: Primary WebGPU implementation with automatic WebGL fallback (FR-001)
- Implementation: Backend detection and graceful degradation

### Additional Constitutional Validation

**✅ Test Coverage (>80% requirement)**:
- Implementation: Contract tests for all 18 functional requirements
- Unit tests for shader compilation, buffer management
- Integration tests for scene rendering pipeline
- Performance tests for FPS validation

**✅ Cross-Platform Consistency**:
- Implementation: Platform-specific JS implementation
- Validation: Visual regression tests ensure identical output to WebGLRenderer

**No Violations Detected** - Design aligns with all constitutional principles.

## Project Structure

### Documentation (this feature)
```
specs/016-implement-production-ready/
├── spec.md              # Feature specification (completed with clarifications)
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
│   ├── WebGPURenderer.contract.kt
│   ├── ShaderModule.contract.kt
│   ├── GPUBuffer.contract.kt
│   └── RenderPipeline.contract.kt
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
# Kotlin Multiplatform Library Structure
src/
├── commonMain/kotlin/io/kreekt/renderer/
│   ├── Renderer.kt                    # Existing interface (no changes)
│   ├── RendererCapabilities.kt        # Existing (no changes)
│   ├── RenderTarget.kt                # Existing (no changes)
│   └── webgpu/                        # NEW: WebGPU abstractions
│       ├── WebGPUDevice.kt            # GPU device abstraction
│       ├── RenderPipelineDescriptor.kt
│       ├── ShaderModuleDescriptor.kt
│       └── BufferDescriptor.kt
│
├── jsMain/kotlin/io/kreekt/renderer/
│   ├── webgpu/                        # NEW: WebGPU renderer implementation
│   │   ├── WebGPURenderer.kt          # Main renderer class
│   │   ├── WebGPUPipeline.kt          # Pipeline state management
│   │   ├── WebGPUBuffer.kt            # Buffer upload/management
│   │   ├── WebGPUTexture.kt           # Texture handling
│   │   ├── WebGPUShaderCompiler.kt    # WGSL compilation
│   │   └── shaders/                   # WGSL shader library
│   │       ├── basic.vert.wgsl        # Basic vertex shader
│   │       ├── basic.frag.wgsl        # Basic fragment shader
│   │       └── ShaderLibrary.kt       # Shader string management
│   └── backend/
│       └── WebGPUBackendNegotiator.kt # Existing (may need updates)
│
└── jsTest/kotlin/io/kreekt/renderer/webgpu/
    ├── WebGPURendererContractTest.kt  # Contract compliance tests
    ├── WebGPUPipelineTest.kt          # Pipeline state tests
    ├── WebGPUBufferTest.kt            # Buffer management tests
    ├── ShaderCompilationTest.kt       # WGSL compilation tests
    ├── PerformanceBenchmark.kt        # 60 FPS validation
    └── FeatureParityTest.kt           # WebGL output comparison
```

**Structure Decision**: Kotlin Multiplatform library with platform-specific JS implementation. The WebGPU renderer is a JS-only component (src/jsMain) that implements the common Renderer interface (src/commonMain). This maintains the existing KreeKt architecture while adding modern WebGPU capabilities for browser targets.

## Phase 0: Outline & Research

### Research Tasks

1. **WebGPU API Patterns**
   - Decision: Use direct navigator.gpu API access with Kotlin/JS interop
   - Rationale: Matches @webgpu/types definitions, minimal abstraction overhead
   - Alternatives: Full Kotlin wrapper (rejected - adds complexity and size)

2. **WGSL Shader Language**
   - Decision: Embed WGSL shaders as Kotlin string literals
   - Rationale: Type-safe, IDE-friendly, allows shader hot-reloading
   - Alternatives: External .wgsl files (rejected - complicates build)

3. **Pipeline State Management**
   - Decision: Cache compiled pipelines by hash of (shader + vertex format + render state)
   - Rationale: Avoids redundant GPU work, critical for 60 FPS @ 1M triangles
   - Alternatives: Recompile per-draw (rejected - too slow)

4. **Buffer Management Strategy**
   - Decision: Pool buffers by size class, reuse within frames
   - Rationale: Reduces GPU allocations, improves performance
   - Alternatives: Create/destroy per-mesh (rejected - memory thrashing)

5. **Fallback Architecture**
   - Decision: Factory pattern - attempt WebGPU, return WebGLRenderer on failure
   - Rationale: Transparent to user, maintains API compatibility (FR-001)
   - Alternatives: Explicit user choice (rejected - violates "automatic" requirement)

6. **Context Loss Recovery**
   - Decision: Store resource descriptors, recreate on loss event
   - Rationale: Enables automatic recovery (clarification answer)
   - Alternatives: Fail and notify (rejected - violates FR-011)

7. **Performance Optimization Techniques**
   - Decision: Frustum culling + draw call batching + instancing support
   - Rationale: Required to hit 60 FPS @ 1M triangles
   - Alternatives: Naive rendering (rejected - won't meet performance target)

8. **Memory Management**
   - Decision: Automatic resource disposal with reference counting
   - Rationale: Prevents leaks (FR-010), utilizes GPU efficiently (NFR-002)
   - Alternatives: Manual disposal (rejected - error-prone)

**Output**: research.md created with detailed findings

## Phase 1: Design & Contracts

### 1. Data Model (data-model.md)

**Core Entities**:

- **WebGPURenderer**: Implements Renderer interface
  - Fields: device, context, pipeline cache, buffer pool, stats tracker
  - Relationships: owns GPUDevice, manages multiple RenderPipeline instances
  - Lifecycle: initialize() → render() loop → dispose()

- **RenderPipeline**: Compiled shader + state configuration
  - Fields: GPUPipeline handle, vertex layout, shader modules, depth/stencil state
  - Relationships: references ShaderModule instances
  - Validation: Must match BufferGeometry attribute layout
  - State: Cached (immutable after creation)

- **ShaderModule**: Compiled WGSL shader code
  - Fields: GPUShaderModule handle, entry point, stage (vertex/fragment)
  - Relationships: owned by RenderPipeline
  - Validation: WGSL syntax validation at compile time
  - State: Immutable after creation

- **GPUBuffer**: Vertex/index/uniform data storage
  - Fields: GPUBuffer handle, size, usage flags, data pointer
  - Relationships: referenced by RenderPipeline during draw calls
  - Lifecycle: upload() → bind() → dispose()
  - State transitions: Created → Uploaded → Bound → Disposed

- **TextureResource**: 2D/3D image data on GPU
  - Fields: GPUTexture handle, format, dimensions, mip levels
  - Relationships: referenced by materials, bound to samplers
  - Lifecycle: create() → upload() → sample() → dispose()

- **CommandEncoder**: Frame rendering command recording
  - Fields: GPUCommandEncoder handle, render pass descriptors
  - Lifecycle: begin() → record commands → submit() (ephemeral per-frame)

### 2. API Contracts (contracts/ directory)

**WebGPURenderer.contract.kt**:
```kotlin
interface WebGPURendererContract : Renderer {
    // FR-002: Initialize with surface
    suspend fun initialize(surface: RenderSurface): RendererResult<Unit>

    // FR-003: Render scene with camera
    fun render(scene: Scene, camera: Camera): RendererResult<Unit>

    // FR-010: Resource disposal
    fun dispose(): RendererResult<Unit>

    // FR-011: Context loss recovery
    fun handleContextLoss(): RendererResult<Unit>

    // FR-017: Statistics tracking
    fun getStats(): RenderStats
}
```

**ShaderModule.contract.kt**:
```kotlin
interface ShaderModuleContract {
    // FR-005: Shader compilation
    suspend fun compile(source: String, stage: ShaderStage): Result<ShaderModule>

    // Shader validation
    fun validate(source: String): Result<Unit>
}
```

**GPUBuffer.contract.kt**:
```kotlin
interface GPUBufferContract {
    // FR-004: Buffer upload
    suspend fun upload(data: FloatArray, usage: BufferUsage): Result<GPUBuffer>

    // Buffer binding
    fun bind(slot: Int): Result<Unit>

    // FR-010: Disposal
    fun dispose()
}
```

**RenderPipeline.contract.kt**:
```kotlin
interface RenderPipelineContract {
    // FR-006: Pipeline state management
    suspend fun create(descriptor: PipelineDescriptor): Result<RenderPipeline>

    // FR-013: Pipeline caching
    fun getOrCreate(key: PipelineKey): Result<RenderPipeline>
}
```

### 3. Contract Tests (failing tests created)

**WebGPURendererContractTest.kt**:
- Test FR-001: Detect WebGPU, fallback to WebGL
- Test FR-002: Initialize with surface properties
- Test FR-003: Render scene correctly
- Test FR-008: Visual parity with WebGLRenderer
- Test FR-009: 60 FPS @ 1M triangles
- Test FR-011: Context loss recovery
- Test FR-017: Statistics tracking

**ShaderCompilationTest.kt**:
- Test FR-005: WGSL compilation success
- Test FR-014: Clear error messages on failure

**BufferManagementTest.kt**:
- Test FR-004: Efficient buffer upload
- Test FR-010: No memory leaks on disposal

**FeatureParityTest.kt**:
- Test FR-008: Identical visual output
- Test FR-018: MeshBasicMaterial support

**PerformanceBenchmark.kt**:
- Test NFR-001: Initialization <2000ms
- Test NFR-003: 60 FPS @ 1M triangles
- Test NFR-004: Bundle size <5MB

### 4. Quickstart Documentation (quickstart.md)

Example usage demonstrating:
1. Initialize WebGPU renderer (automatic fallback)
2. Create scene with 1M triangle mesh
3. Render loop maintaining 60 FPS
4. Handle context loss gracefully
5. Dispose resources properly

### 5. Update AGENTS.md

Execute agent context update script to add WebGPU implementation details.

**Output**: data-model.md, contracts/, failing tests, quickstart.md, AGENTS.md updated

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
1. Load `.specify/templates/tasks-template.md` as base template
2. Generate tasks from Phase 1 design artifacts:
   - Each contract interface → contract test task [P]
   - Each entity (RenderPipeline, ShaderModule, etc.) → model creation task [P]
   - Each FR/NFR → validation test task
   - Implementation tasks to make tests pass

**Ordering Strategy** (TDD-compliant):
1. **Foundation tasks** [P]:
   - Create WebGPU device abstraction
   - Implement buffer management
   - Implement shader compilation

2. **Contract test tasks** [P]:
   - Write WebGPURenderer contract tests (failing)
   - Write ShaderModule contract tests (failing)
   - Write GPUBuffer contract tests (failing)
   - Write RenderPipeline contract tests (failing)

3. **Implementation tasks** (dependency order):
   - Implement ShaderModule (makes shader tests pass)
   - Implement GPUBuffer (makes buffer tests pass)
   - Implement RenderPipeline (makes pipeline tests pass)
   - Implement WebGPURenderer (makes renderer tests pass)

4. **Integration tasks**:
   - Implement fallback mechanism
   - Implement context loss recovery
   - Implement performance optimizations (batching, culling)

5. **Validation tasks**:
   - Run performance benchmarks
   - Run feature parity tests
   - Validate bundle size <5MB
   - Run all contract tests

**Estimated Output**: 30-35 numbered, dependency-ordered tasks in tasks.md

**Task Markers**:
- [P] = Parallelizable (independent files/modules)
- [D] = Depends on previous tasks
- [TEST] = Test creation (must fail initially)
- [IMPL] = Implementation (makes tests pass)
- [VALID] = Validation/benchmarking

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation (execute tasks.md following TDD principles)
**Phase 5**: Validation (run all tests, benchmarks, bundle analysis)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

**No Violations** - Design remains within constitutional boundaries:
- Single renderer implementation (not adding project complexity)
- Performance targets exceed constitutional minimums (1M vs 100K triangles)
- Size constraint respected through code splitting
- No architectural anti-patterns introduced

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented (none)

---
*Based on KreeKt Constitution (60 FPS, <5MB) - See `/CLAUDE.md`*
