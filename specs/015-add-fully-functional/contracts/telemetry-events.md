# Contract: Telemetry Events Schema

## Event Types
- `INITIALIZED`
- `DENIED`
- `DEVICE_LOST`
- `PERFORMANCE_DEGRADED`

## Payload Schema (JSON)
```json
{
  "eventId": "uuid",
  "eventType": "INITIALIZED | DENIED | DEVICE_LOST | PERFORMANCE_DEGRADED",
  "backendId": "WEBGPU | VULKAN",
  "device": {
    "vendorId": "0x8086",
    "productId": "0x4905"
  },
  "driverVersion": "string",
  "osBuild": "string",
  "featureFlags": {
    "COMPUTE": "SUPPORTED | MISSING | EMULATED",
    "RAY_TRACING": "SUPPORTED | MISSING | EMULATED",
    "XR_SURFACE": "SUPPORTED | MISSING | EMULATED"
  },
  "performance": {
    "initMs": 2450,
    "avgFps": 62.1,
    "minFps": 31.5
  },
  "sessionId": "anonymized-hash",
  "callStack": ["io.kreekt.renderer.BackendNegotiator.initialize", "..."],
  "limitations": ["Ray tracing unsupported"],
  "timestamp": "2025-10-06T12:30:45Z"
}
```

## Behavioral Requirements
1. `sessionId` MUST be generated via SHA-256 of anonymous session token; no PII may be included.
2. `featureFlags` MUST include all parity-required features even when `SUPPORTED`.
3. `performance` object is mandatory for `INITIALIZED` and `PERFORMANCE_DEGRADED`; optional (null) for `DENIED`.
4. `callStack` MUST include at least the top three frames leading to the event.
5. Events MUST be emitted within 500ms of occurrence and retried up to 3 times on network failure.

## Contract Test Scenarios (to implement)
1. **Denied Event Completeness**: Simulate missing ray tracing → expect payload with `DENIED`, populated `featureFlags`, `limitations`, and call stack.
2. **Initialization Event**: Successful init on integrated GPU → payload includes performance metrics meeting budget and `INITIALIZED` tag.
3. **Privacy Guardrail**: Attempt to inject email/user ID into `sessionId` field → validation rejects payload.
4. **Retry Logic**: Force first two transmissions to fail → expect third attempt success and telemetry queue drained.

Tests should target a shared telemetry validator under `src/commonTest/.../TelemetryEventContractTest.kt`.
