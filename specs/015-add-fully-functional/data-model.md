# Data Model: WebGPU/Vulkan Backend

## Overview
The backend integrates runtime detection, capability reporting, parity tracking, and telemetry. Entities below live in shared core (`commonMain`) with platform-specific adapters populating fields.

## Entities

### RenderingBackendProfile
- **Purpose**: Describes a backend implementation (WebGPU, Vulkan) per platform build.
- **Attributes**:
  - `backendId` (enum: `WEBGPU`, `VULKAN`)
  - `supportedFeatures` (set of `BackendFeature` – e.g., `COMPUTE`, `RAY_TRACING`, `XR_SURFACE`)
  - `performanceBudget` (structure: `targetFps`, `minFps`, `initBudgetMs`)
  - `fallbackPriority` (integer; lower number → higher priority)
  - `apiVersion` (string; e.g., `WebGPU 1.0`, `Vulkan 1.3`)
  - `platformTargets` (list of platforms: `WEB`, `DESKTOP`, `ANDROID`, `IOS`)
- **Relationships**: Referenced by `DeviceCapabilityReport` during selection; joined with `FeatureParityMatrix` entries.
- **Validation Rules**:
  - Must include all parity-required features (`COMPUTE`, `RAY_TRACING`, `XR_SURFACE`).
  - `performanceBudget.targetFps` ≥ 60 and `minFps` ≥ 30.
  - `fallbackPriority` unique per platform target.

### DeviceCapabilityReport
- **Purpose**: Runtime snapshot of detected GPU/device capabilities.
- **Attributes**:
  - `deviceId` (string: vendor + product)
  - `driverVersion` (string)
  - `osBuild` (string)
  - `featureFlags` (map `BackendFeature` → `SUPPORTED | MISSING | EMULATED`)
  - `preferredBackend` (enum matching RenderingBackendProfile)
  - `limitations` (list of human-readable blockers)
  - `timestamp` (ISO-8601)
- **Relationships**: Generated prior to backend selection; stored alongside `BackendDiagnosticsLog` entries; compared against `RenderingBackendProfile` to resolve compatibility.
- **Validation Rules**:
  - Must contain WebGPU adapter info when platform is web.
  - `preferredBackend` must correspond to a profile where all required features are `SUPPORTED`.

### RenderSurfaceDescriptor
- **Purpose**: Defines the rendering surface/swapchain configuration for the active backend.
- **Attributes**:
  - `surfaceId` (string)
  - `backendId` (enum)
  - `width`, `height` (integers, pixels)
  - `colorFormat` (enum; e.g., `RGBA16F`, `BGRA8Unorm`)
  - `depthFormat` (enum; e.g., `Depth24Stencil8`)
  - `presentMode` (enum; e.g., `FIFO`, `MAILBOX`)
  - `isXRSurface` (boolean)
- **Relationships**: Created after backend initialization; referenced by performance monitoring for frame timing.
- **Validation Rules**:
  - Dimensions must match target canvas/window size.
  - XR surfaces require `XR_SURFACE` support in `RenderingBackendProfile`.

### FeatureParityMatrix
- **Purpose**: Tracks parity coverage across features and backends per platform.
- **Attributes**:
  - `featureId` (string; e.g., `PBR_SHADING`, `OMNI_SHADOWS`, `COMPUTE_SKINNING`)
  - `webgpuStatus` / `vulkanStatus` (enum: `COMPLETE`, `DEGRADED`, `BLOCKED`)
  - `notes` (string)
  - `mitigation` (string referencing documentation or issue)
  - `visualBaselineId` (link to reference render output)
- **Relationships**: Consumed by validation suites and release gates; cross-linked with `RenderingBackendProfile.supportedFeatures`.
- **Validation Rules**:
  - `COMPLETE` allowed only when parity tests pass across platforms.
  - If status is `DEGRADED` or `BLOCKED`, `mitigation` must be populated.

### BackendDiagnosticsLog
- **Purpose**: Persistent record of initialization, device loss, and denial events.
- **Attributes**:
  - `logId` (UUID)
  - `eventType` (enum: `INITIALIZED`, `DENIED`, `DEVICE_LOST`, `PERFORMANCE_DEGRADED`)
  - `backendId` (enum)
  - `capabilityReport` (reference to DeviceCapabilityReport)
  - `telemetryPayload` (JSON blob matching telemetry contract)
  - `sessionId` (hash; anonymized)
  - `timestamp` (ISO-8601)
- **Relationships**: Emitted to telemetry pipeline; used to reproduce failures.
- **Validation Rules**:
  - `telemetryPayload` must include fields mandated by FR-017.
  - `DENIED` events require `limitations` from linked `DeviceCapabilityReport`.

## State & Workflow

### Backend Selection State Machine
1. `Detected` → capability probing completed (`DeviceCapabilityReport` generated).
2. `Evaluating` → compare report against available `RenderingBackendProfile`s.
3. `Initialized` → backend selected, `RenderSurfaceDescriptor` created, logging `INITIALIZED` event.
4. `Denied` → no compatible backend, fail-fast, emit `DENIED` log with telemetry payload.
5. `Recovering` → device-loss event; attempt reinitialization or escalate to `Denied`.

Transitions require telemetry capture and parity verification hooks before scene render begins.

## Data Volume & Retention
- Capability reports stored in-memory during session; optionally persisted to diagnostics cache for 24h to aid beta support.
- Telemetry logs shipped upstream immediately; local retention limited to rolling 100 entries to cap memory usage.

## Integration Notes
- Entities live in shared modules; platform-specific layers provide adapter data (e.g., WebGPU adapter info, Vulkan physical device properties).
- All entities must serialize to JSON for telemetry/testing snapshots to support ests suites.
