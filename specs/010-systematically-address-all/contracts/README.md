# API Contracts

This directory contains contract definitions for the key interfaces that must be implemented to complete the KreeKt production readiness feature.

## Contracts

1. **validation-scanner-contract.kt** - PlaceholderScanner interface for detecting TODOs/stubs
2. **compilation-validator-contract.kt** - CompilationValidator for running gradle builds
3. **renderer-factory-contract.kt** - RendererFactory for platform-specific renderer creation
4. **file-system-contract.kt** - FileScanner for cross-platform file I/O

## Contract Structure

Each contract file contains:
- Interface definition with detailed contract requirements
- Constitutional compliance notes
- Acceptance criteria derived from test expectations

## Usage

These contracts serve as:
- Reference for implementation
- Test specification
- Documentation of expected behavior

All implementations must satisfy the contracts to pass the corresponding test suites.
