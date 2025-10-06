# Research: Fully Functional WebGPU/Vulkan Backend

## Decision Log

### 1. WebGPU Beta Readiness
- **Decision**: Target Chromium 128+ and Safari Technology Preview with WebGPU enabled by default; document fallback instructions for enterprise browsers requiring feature flags.
- **Rationale**: These builds deliver stable WebGPU compute pipelines, allowing full parity with Vulkan features while covering the majority of beta testers.
- **Alternatives Considered**:
  - Shipping against Firefox Nightly: rejected due to incomplete compute support and vendor gating.
  - Maintaining WebGL2 fallback: rejected for this feature (fails fast requirement, avoids diluting parity goals).

### 2. Vulkan Feature Parity Scope
- **Decision**: Require Vulkan 1.3 with VK_KHR_ray_tracing_pipeline and VK_KHR_dynamic_rendering on desktop, MoltenVK 1.2+ with ray tracing extensions on Apple platforms.
- **Rationale**: Guarantees compute/ray tracing parity and aligns with Apple and integrated GPU roadmaps while using widely available driver versions.
- **Alternatives Considered**:
  - Supporting Vulkan 1.1 baseline: rejected because dynamic rendering and ray tracing parity would be impossible.
  - Using engine-specific abstraction to emulate ray tracing: rejected due to performance penalties and maintenance burden.

### 3. Performance & Telemetry Instrumentation
- **Decision**: Integrate per-frame GPU/CPU timing queries and initialization timestamps into the renderer core, backed by KreeKt telemetry hooks with 24h retention.
- **Rationale**: Enables automated validation of 60 FPS / <3s goals and feeds denial events into compliance pipelines without extra tooling.
- **Alternatives Considered**:
  - Manual profiling only: rejected due to lack of automated enforcement and beta readiness tracking.
  - Third-party telemetry SDK: rejected to avoid new licensing/compliance risks.

### 4. Backend Denial Telemetry Schema
- **Decision**: Log backend name, error code, device vendor/ID, driver version, OS build, detected feature flags, anonymized session ID, and call stack; scrub PII at source and ship JSON payloads to secure ingest endpoint.
- **Rationale**: Meets compliance requirements while providing actionable diagnostics for support and engineering.
- **Alternatives Considered**:
  - Minimal logging: rejected because compliance reviewers require device/driver detail.
  - Full user identity capture: rejected for privacy and regulatory reasons.

### 5. Android & iOS Launch Baseline
- **Decision**: Ship Android on Vulkan 1.3 (API 33+) with GPU family G (Adreno 7xx, Mali G710) and iOS via MoltenVK on A15/M1 devices; include device matrix in beta documentation.
- **Rationale**: Matches integrated GPU performance targets and ensures parity scenes run within FPS envelope on mobile.
- **Alternatives Considered**:
  - Delaying iOS: rejected by product requirement for day-one mobile parity.
  - Supporting legacy Android API < 30: rejected due to missing Vulkan 1.3 capabilities.

## References
- Chromium WebGPU status tracker (2025-09 snapshot)
- Vulkan 1.3 feature tables (Khronos)
- MoltenVK release notes 1.2.x
- Android GPU Inspector profiles (Adreno 7xx, Mali G710)
- Internal compliance telemetry guidelines v4
