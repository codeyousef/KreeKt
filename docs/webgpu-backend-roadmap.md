# WebGPU & Vulkan Backend Task Roadmap

This roadmap turns the renderer upgrade into a sequence of small, test-first tasks that we can tackle across multiple sessions. Each task is scoped to an incremental deliverable with explicit acceptance tests before implementation begins.

## Working Agreement
- **TDD-first**: For every task, write or update the failing test (unit/integration/golden image) _before_ writing production code.
- **Incremental delivery**: Merge-ready change per task (or short task group) with all tests passing.
- **Regression protection**: Extend existing suites (`ktlint`, JVM/JS tests, ests/ system suites) whenever behaviour shifts.
- **Documentation**: Update this roadmap (checkboxes, notes) as tasks complete; add code comments only when behaviour is non-obvious.

## Phase 1 – Foundations & Contracts
1. **Inventory Current Renderer Contracts** *(docs-only)*  
   - Write spec covering current `Renderer`, material system, and surface contracts.  
   - Acceptance: markdown doc reviewed; no code changes.
2. **Define GPU Abstraction Interfaces** *(commonMain)*  
   - Add `GpuDevice`, queues, buffers, textures, bind group descriptors.  
   - Tests: multiplatform unit tests asserting default descriptors + make sure existing modules compile via type smoke tests.
3. **Capability Mapping** *(commonMain)*  
   - Extend `RendererCapabilities` to ingest the new abstractions.  
   - Tests: unit tests for mapping logic; ensure regression on existing WebGL path.

## Phase 2 – WebGPU Backend (JS)
4. **JS Interop Shim** *(jsMain)*  
   - Create safe bindings for `navigator.gpu`, adapters, devices.  
   - Tests: JS unit tests with fake objects (using `kotlin.test`) verifying fallback when WebGPU unavailable.
5. **Device Acquisition & Swap Chain Config**  
   - Implement `WebGPURenderer.initialize`; add canvas context wiring.  
   - Tests: integration test using mocked canvas/context verifying failure modes.
6. **Command Encoding Skeleton**  
   - Minimal render loop clearing current texture.  
   - Tests: golden checksum (headless) verifying colour output; stats counter test.
7. **Pipeline Cache & Shader Module Wiring**  
   - Map shared descriptors to WebGPU pipeline descriptors.  
   - Tests: unit tests stubbing `GPUDevice` to assert layout translation.
8. **Buffer & Texture Upload Utilities**  
   - Implement staging buffer helpers, `queue.writeBuffer/Texture`.  
   - Tests: JS tests verifying byte transfer and error handling.
9. **Integrate with voxelcraft toggle**  
   - Runtime detection to choose WebGPU; fallback to WebGL.  
   - Tests: example smoke test (`:examples:voxelcraft:jsTest`) asserting backend selection; update docs/screenshots.

## Phase 3 – Vulkan Backend (JVM)
10. **LWJGL Bootstrap Skeleton**  
    - Stub renderer with device/instance selection strategy.  
    - Tests: JVM unit test mocking LWJGL calls (or using mock binding layer).
11. **Swapchain Manager & Recreation Hooks**  
    - Implement create/recreate/destroy logic.  
    - Tests: integration test using mock handles verifying state transitions.
12. **Sync Primitives & Frame Loop**  
    - Add semaphore/fence rotation; submit empty command buffer.  
    - Tests: unit tests ensuring frame index cycling; integration smoke via `:examples:basic-scene:test` once stubbed.
13. **Pipeline & Resource Uploads**  
    - Mirror GPU abstractions to Vulkan pipeline creation; buffer/texture staging via VMA or equivalent.  
    - Tests: JVM integration harness that records command submissions using mock handles.

## Phase 4 – QA & Tooling
14. **Golden Image Harness** *(shared)*  
    - Capture reference frames for WebGPU + Vulkan + WebGL.  
    - Tests: new suite comparing SSIM within tolerance.
15. **Profiler Hooks**  
    - Extend `tools/profiler` to read GPU timings from both backends.  
    - Tests: CLI regression verifying metrics JSON output.
16. **Release Checklist & Docs**  
    - Document driver matrix, feature toggles, validation steps.  
    - Tests: lint/doc build (`./gradlew dokkaHtml`) plus manual checklist verification.

## Tracking
Use this file to tick tasks `[ ] -> [x]`, jot down blockers, and link to PRs/tests per item. Update after each session so we keep continuity.
