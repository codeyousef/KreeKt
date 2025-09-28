<!--
Sync Impact Report:
Version: 0.0.0 → 1.0.0
Modified principles: N/A (new constitution)
Added sections:
- Core Principles (5 principles)
- Quality Standards
- Development Workflow
- Governance
Removed sections: N/A
Templates requiring updates:
✅ plan-template.md - Constitution Check section matches
✅ tasks-template.md - TDD requirements aligned
✅ spec-template.md - compatible with quality standards
Follow-up TODOs: None
-->

# KreeKt Constitution

## Core Principles

### I. Test-Driven Development (NON-NEGOTIABLE)

Test-Driven Development is strictly enforced using the Red-Green-Refactor cycle. Tests MUST be written before any
implementation code. All tests MUST pass before moving to the next task. No stubs, workarounds, TODOs, "in the
meantime", "for now", or "in a real implementation" placeholders are permitted. Compilation timeouts MUST be resolved by
increasing timeout duration rather than skipping compilation verification.

**Rationale**: Ensures code correctness, prevents technical debt, and guarantees production readiness from the start.

### II. Production-Ready Code Only

All code delivered MUST be 100% production-ready with no temporary solutions, incomplete implementations, or deferred
work. Every function, class, and module MUST be fully implemented and tested. Code reviews MUST verify production
readiness before merge approval.

**Rationale**: Eliminates technical debt accumulation and ensures consistent quality standards across the codebase.

### III. Cross-Platform Compatibility

All features MUST work consistently across JVM, JavaScript, Linux, macOS, Windows, iOS, and Android platforms using
Kotlin Multiplatform's expect/actual pattern. Platform-specific implementations MUST maintain API compatibility and
behavior consistency. Performance characteristics MUST be documented and validated per platform.

**Rationale**: Core library mission requires reliable cross-platform 3D graphics capabilities.

### IV. Performance Standards

Target performance is 60 FPS with 100k+ triangles across all supported platforms. Memory usage MUST stay within defined
budgets: Mobile (256MB), Standard (1GB), High (2GB), Ultra (4GB+). All performance-critical code MUST include benchmarks
and performance regression tests.

**Rationale**: 3D graphics applications require consistent high performance to provide acceptable user experience.

### V. Type Safety and API Design

Leverage Kotlin's type system for compile-time validation with no runtime casts. API design MUST follow Three.js
compatibility patterns while maintaining Kotlin idioms. Use sealed classes for type hierarchies, data classes for
immutable structures, and inline classes for performance-critical operations.

**Rationale**: Prevents runtime errors and provides familiar API patterns for developers migrating from Three.js.

## Quality Standards

**Testing Requirements**:

- Unit tests for all math operations, data structures, and utilities
- Integration tests for renderer initialization and scene rendering
- Platform-specific tests for each target platform
- Performance tests measuring frame rate, memory usage, and initialization time
- Visual tests ensuring rendering consistency across platforms

**Code Coverage**: Minimum 90% line coverage, 85% branch coverage required for all modules.

**Documentation Standards**: All public APIs MUST have KDoc documentation with usage examples. Architecture decisions
MUST be documented with rationale.

## Development Workflow

**Implementation Process**:

1. Write failing tests for the required functionality
2. Verify tests fail (Red phase)
3. Implement minimal code to make tests pass (Green phase)
4. Refactor for quality while keeping tests passing (Refactor phase)
5. Run full test suite to ensure no regressions
6. Verify compilation across all target platforms with sufficient timeouts

**Code Review Requirements**:

- All changes MUST pass automated test suites
- Manual testing on at least two target platforms
- Performance impact assessment for changes affecting rendering pipeline
- API compatibility verification for public interface changes

**Compilation Standards**: All target platforms MUST compile successfully before code acceptance. Compilation timeouts
MUST be resolved by extending timeout duration, never by skipping compilation verification.

## Governance

**Constitutional Authority**: This constitution supersedes all other development practices and guidelines. Any conflicts
between this constitution and other documentation MUST be resolved in favor of constitutional requirements.

**Amendment Process**: Constitutional amendments require documentation of rationale, impact assessment on existing
codebase, and migration plan for non-compliant code. Version increments follow semantic versioning: MAJOR for
backward-incompatible governance changes, MINOR for new principles or expanded guidance, PATCH for clarifications and
refinements.

**Compliance Review**: All pull requests MUST verify constitutional compliance. Complexity deviations MUST be explicitly
justified with rationale for why simpler alternatives are insufficient. Non-compliance findings MUST be addressed before
merge approval.

**Enforcement**: Development tools and CI/CD pipelines MUST enforce constitutional requirements automatically where
possible. Manual review processes MUST verify constitutional compliance for aspects that cannot be automated.

**Version**: 1.0.0 | **Ratified**: 2025-01-14 | **Last Amended**: 2025-01-14