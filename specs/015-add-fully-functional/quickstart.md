# Quickstart: Validating WebGPU/Vulkan Backend

## Prerequisites
- Kotlin 1.9.x toolchain with KMP enabled
- Supported GPUs/drivers:
  - Web: Chromium 128+ or Safari Technology Preview with WebGPU enabled
  - Desktop: Vulkan 1.3 drivers with ray tracing extensions (Intel Arc, Apple M1/M2 via MoltenVK)
  - Android: API 33+ device with Vulkan 1.3 (Adreno 7xx/Mali G710)
  - iOS: A15/M1+ running iOS 17+/visionOS for XR surfaces
- Access to telemetry ingest endpoint for denial logs

## 1. Build & Smoke Tests
```bash
./gradlew clean build
./gradlew test
./gradlew :ests:integration:checkBackendParity
```
- Ensures core modules compile across all targets.
- Integration suite should fail until backend parity work is implemented; expect assertions around feature matrix gaps.

## 2. Configure Backend Selection
1. Update `BackendConfiguration` (new) in `src/commonMain` to expose:
   - Preferred backend order per platform
   - Telemetry endpoint URL and retention policy
2. Add platform-specific adapters:
   - `src/jsMain/.../WebGPUBackendAdapter.kt`
   - `src/jvmMain/.../VulkanBackendAdapter.kt`
   - `src/androidMain/.../AndroidVulkanAdapter.kt`
   - `src/iosMain/.../MoltenVKAdapter.kt`
3. Ensure adapters populate `DeviceCapabilityReport` with driver, OS, and feature flags.

## 3. Implement Contracts (see `/contracts`)
- Follow `backend-negotiation.md` to implement detection → selection → initialization flow.
- Wire performance metrics described in `performance-monitoring.md`.
- Emit telemetry payloads per `telemetry-events.md`, including anonymized session ID.

## 4. Run Performance & Telemetry Validation
```bash
./gradlew :ests:performance:run --tests "*IntegratedGpuBaseline*"
./gradlew :ests:integration:run --tests "*TelemetryDenialTest"
```
- Performance suite verifies FPS and init budgets on reference hardware profiles (simulated/emulated where needed).
- Telemetry test asserts payload completeness for denied backends.

## 5. Visual Regression Checks
```bash
./gradlew :ests:visual:renderParityScenes
```
- Generates reference renders for WebGPU and Vulkan.
- Compare outputs (`ests/visual/output`) to ensure color/lighting parity within fixed tolerances.

## 6. Documentation & Samples
1. Update README platform matrix with beta readiness notes.
2. Refresh `examples/basic-scene` to demonstrate backend auto-selection telemetry output.
3. Publish beta launch checklist referencing telemetry dashboard and known device matrix.

## Expected Outcomes
- All parity integration tests pass for WebGPU/Vulkan across targeted platforms.
- Telemetry dashboards show denial events with complete payloads within 24h retention.
- Performance suite reports ≥60 FPS target on integrated GPUs and <3s init times.
- Visual regression suite produces identical (or tolerance-bound) renders across backends for flagship scenes.
