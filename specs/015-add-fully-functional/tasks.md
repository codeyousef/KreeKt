# Tasks: Fully Functional WebGPU/Vulkan Backend

**Input**: Design documents from `/specs/015-add-fully-functional/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

## Execution Flow (main)
```
1. Load plan.md from feature directory
2. Load data-model.md, contracts/, research.md, quickstart.md
3. Generate ordered tasks with TDD-first sequencing
4. Validate coverage of entities, contracts, and scenarios
5. Return: SUCCESS (tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Task can run in parallel (no shared files or dependencies)
- Include explicit file paths and expected command snippets where relevant

## Phase 3.1: Setup
- [x] T001 Align `build.gradle.kts` + `gradle.properties` with WebGPU/Vulkan beta dependencies (WebGPU bindings, LWJGL 3.3.3 Vulkan extras, MoltenVK artifacts) and expose new source sets.
- [x] T002 Scaffold backend negotiation package in `src/commonMain/kotlin/io/kreekt/renderer/backend/` with placeholder expect interfaces/classes referenced by tests.
- [x] T003 [P] Add integrated GPU capability fixtures to `ests/common/resources/device-capabilities.json` covering Apple M1 and Intel Iris Xe baselines.

## Phase 3.2: Tests First (TDD)
- [x] T004 [P] Add `BackendNegotiationContractTest` in `src/commonTest/kotlin/io/kreekt/renderer/backend/BackendNegotiationContractTest.kt` covering happy-path, denial, timeout, and device-loss scenarios.
- [x] T005 [P] Add `PerformanceMonitorContractTest` in `src/commonTest/kotlin/io/kreekt/renderer/metrics/PerformanceMonitorContractTest.kt` validating initialization timing and rolling FPS windows.
- [x] T006 [P] Add `TelemetryEventContractTest` in `src/commonTest/kotlin/io/kreekt/telemetry/TelemetryEventContractTest.kt` enforcing payload completeness, privacy guardrails, and retry logic.
- [x] T007 [P] Create `BackendAutoSelectionTest` in `ests/integration/backend/BackendAutoSelectionTest.kt` simulating platform detection across Web, Desktop, Android, and iOS.
- [x] T008 [P] Create `FailFastFallbackTest` in `ests/integration/backend/FailFastFallbackTest.kt` asserting denial messaging when neither backend qualifies.
- [x] T009 [P] Create `DiagnosticsTelemetryTest` in `ests/integration/backend/DiagnosticsTelemetryTest.kt` verifying emitted telemetry plugs into compliance sink.
- [x] T010 [P] Create `IntegratedGpuBaselineTest` in `ests/performance/IntegratedGpuBaselineTest.kt` asserting ≥60 FPS / <3s init on reference profiles.
- [x] T011 [P] Create `BackendParityVisualTest` in `ests/visual/BackendParityVisualTest.kt` comparing WebGPU vs Vulkan renders against parity baselines.

## Phase 3.3: Core Models & Infrastructure (post failing tests)
- [x] T012 [P] Implement `RenderingBackendProfile` data class per spec in `src/commonMain/kotlin/io/kreekt/renderer/backend/RenderingBackendProfile.kt` with validation logic.
- [x] T013 [P] Implement `DeviceCapabilityReport` in `src/commonMain/kotlin/io/kreekt/renderer/backend/DeviceCapabilityReport.kt` capturing feature flags and preferred backend rules.
- [x] T014 [P] Implement `RenderSurfaceDescriptor` in `src/commonMain/kotlin/io/kreekt/renderer/backend/RenderSurfaceDescriptor.kt` with XR surface constraints.
- [x] T015 [P] Implement `FeatureParityMatrix` in `src/commonMain/kotlin/io/kreekt/renderer/backend/FeatureParityMatrix.kt` including mitigation metadata.
- [x] T016 [P] Implement `BackendDiagnosticsLog` in `src/commonMain/kotlin/io/kreekt/telemetry/BackendDiagnosticsLog.kt` with serialization helpers.

## Phase 3.4: Core Implementation (depends on Phase 3.3)
- [x] T017 Implement shared `BackendNegotiator` expect API and orchestration pipeline in `src/commonMain/kotlin/io/kreekt/renderer/backend/BackendNegotiator.kt` (detect → select → initialize → fail-fast).
- [x] T018 [P] Provide WebGPU actuals in `src/jsMain/kotlin/io/kreekt/renderer/backend/WebGPUBackendNegotiator.kt` including adapter probes and surface creation.
- [x] T019 [P] Provide desktop Vulkan actuals in `src/jvmMain/kotlin/io/kreekt/renderer/backend/VulkanBackendNegotiator.kt` leveraging LWJGL 3.3.3 and MoltenVK on macOS.
- [x] T020 [P] Provide Android Vulkan actuals in `src/androidMain/kotlin/io/kreekt/renderer/backend/AndroidVulkanNegotiator.kt` targeting API 33+ devices.
- [x] T021 [P] Provide iOS MoltenVK actuals in `src/iosMain/kotlin/io/kreekt/renderer/backend/IosMoltenVkNegotiator.kt` covering XR surface hooks.
- [x] T022 Implement shared `PerformanceMonitor` expect API in `src/commonMain/kotlin/io/kreekt/renderer/metrics/PerformanceMonitor.kt` with rolling window logic.
- [x] T023 [P] Provide Web timer actual (`src/jsMain/kotlin/io/kreekt/renderer/metrics/WebPerformanceMonitor.kt`) using WebGPU timestamp queries.
- [x] T024 [P] Provide desktop timer actual (`src/jvmMain/kotlin/io/kreekt/renderer/metrics/VulkanPerformanceMonitor.kt`) using Vulkan timestamp queries.
- [x] T025 [P] Provide mobile timer actuals (`src/androidMain/kotlin/io/kreekt/renderer/metrics/AndroidPerformanceMonitor.kt`, `src/iosMain/kotlin/io/kreekt/renderer/metrics/IosPerformanceMonitor.kt`).
- [x] T026 Implement telemetry event builder & retry queue in `src/commonMain/kotlin/io/kreekt/telemetry/BackendTelemetryEmitter.kt` matching contract schema.
- [x] T027 Integrate feature parity evaluation into renderer lifecycle via `src/commonMain/kotlin/io/kreekt/renderer/FeatureParityEvaluator.kt` feeding diagnostics and tests.

## Phase 3.5: Integration & Wiring
- [x] T028 Wire backend negotiation + performance monitor into `src/commonMain/kotlin/io/kreekt/renderer/Renderer.kt` initialization path and ensure fail-fast messaging surfaces to callers.
- [x] T029 Connect telemetry emitter to existing validation hooks in `src/commonMain/kotlin/io/kreekt/validation/RendererValidation.kt` and send events to compliance pipeline.
- [x] T030 Extend ests harness (`ests/common/src/main/kotlin/io/kreekt/ests/BackendTestHarness.kt`) to load fixtures, orchestrate parity matrix snapshots, and capture artifacts referenced by quickstart.

## Phase 3.6: Polish & Release Readiness
- [x] T031 [P] Update `README.md` platform matrix and beta notes with WebGPU/Vulkan parity status and mobile readiness.
- [x] T032 [P] Refresh `examples/basic-scene` (JS & JVM launchers) to showcase backend auto-selection and telemetry output overlays.
- [x] T033 [P] Document telemetry/performance operations in `docs/beta/webgpu-vulkan.md` including device matrix and troubleshooting.
- [x] T034 Execute quickstart validation steps, archive reports/screenshots referenced in `specs/015-add-fully-functional/quickstart.md`, and link artifacts in spec.
- [x] T035 Run final coverage + performance gates (`./gradlew build koverVerify :ests:integration:run :ests:performance:run`) and publish results to CI dashboards.

## Dependencies
- T002 depends on T001.
- T004-T011 require T001-T003.
- T012-T016 depend on completion of all Phase 3.2 tests (T004-T011).
- T017 depends on T012-T016.
- T018-T021 depend on T017; platform-specific tasks can run in parallel after T017.
- T022 depends on T012-T016; T023-T025 depend on T022.
- T026 depends on T013, T016; T027 depends on T015 and T017.
- T028 depends on T017-T027.
- T029 depends on T026 and T027.
- T030 depends on T007-T011 and T027.
- T031-T035 occur after all implementation tasks (T017-T030) complete.

## Parallel Execution Example
```
Task: "T004 Add BackendNegotiationContractTest in src/commonTest/..."
Task: "T005 Add PerformanceMonitorContractTest in src/commonTest/..."
Task: "T006 Add TelemetryEventContractTest in src/commonTest/..."
Task: "T007 Create BackendAutoSelectionTest in ests/integration/backend/BackendAutoSelectionTest.kt"
```

## Notes
- Contract and integration tests (T004-T011) MUST fail before starting implementation tasks.
- When running [P] tasks, ensure no shared files or Gradle touchpoints conflict; keep commits scoped per task.
- Capture telemetry payload samples and parity renders during T034 for documentation reuse.
