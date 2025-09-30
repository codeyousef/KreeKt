# Quickstart: Production Readiness Validation

This guide walks through validating that the KreeKt codebase is production-ready.

## Prerequisites

- Kotlin 1.9+ with Multiplatform plugin
- Gradle 8.0+
- Platform SDKs for targets you want to validate:
    - JDK 17+ for JVM
    - Node.js 18+ for JavaScript
    - Android SDK for Android
    - Xcode for iOS/macOS
    - Visual Studio or MinGW for Windows
    - GCC/Clang for Linux

## Quick Validation

### 1. Run Complete Validation

```bash
# From project root
./gradlew validateProductionReadiness
```

This executes all validation categories and generates a comprehensive report.

### 2. View Results

```bash
# Open HTML report
open build/validation-reports/index.html

# View JSON report for CI/CD
cat build/validation-reports/validation-latest.json | jq .overallStatus
```

## Category-Specific Validation

### Compilation Validation

Verify code compiles across all platforms:

```bash
./gradlew validateCompilation

# Expected output:
# ✅ JVM: Compiled successfully
# ✅ JS: Compiled successfully
# ✅ Native Linux: Compiled successfully
# ...
```

### Test Coverage Validation

Run tests and check coverage meets 95% requirement:

```bash
./gradlew validateTestCoverage

# Expected output:
# Total tests: 523
# Passed: 523
# Failed: 0
# Coverage: 96.2% (PASSED - requirement: 95%)
```

### Performance Validation

Benchmark 3D rendering performance:

```bash
./gradlew validatePerformance

# Expected output:
# FPS: min=125, avg=142, max=165, p95=135
# ✅ Meets 120 FPS requirement
# Memory: 187 MB used (within budget)
# Initialization: 1243ms (< 2000ms requirement)
```

### Security Validation

Check for vulnerabilities:

```bash
./gradlew validateSecurity

# Expected output:
# Scanning dependencies...
# No critical vulnerabilities found
# 2 low severity issues (see report for details)
```

### Constitutional Compliance

Validate against KreeKt constitution principles:

```bash
./gradlew validateConstitutional

# Expected output:
# ✅ TDD Compliance: PASSED
# ✅ Production-Ready Code: PASSED
# ✅ Cross-Platform Compatibility: PASSED
# ✅ Performance Standards: PASSED
# ✅ Type Safety: PASSED
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Production Readiness Check

on: [push, pull_request]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Validate Production Readiness
        run: ./gradlew validateProductionReadiness

      - name: Upload Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: validation-report
          path: build/validation-reports/

      - name: Check Status
        run: |
          STATUS=$(cat build/validation-reports/validation-latest.json | jq -r .overallStatus)
          if [ "$STATUS" != "PASSED" ]; then
            echo "Production readiness check failed!"
            exit 1
          fi
```

## Programmatic Usage

### Kotlin Example

```kotlin
import io.kreekt.validation.ProductionReadinessChecker
import io.kreekt.validation.ValidationConfiguration

fun main() {
    val checker = ProductionReadinessChecker()

    val config = ValidationConfiguration(
        enabledCategories = setOf("compilation", "testing", "performance"),
        platforms = Platform.all(),
        coverageThreshold = 95.0f,
        performanceRequirements = PerformanceRequirements(
            minFps = 120.0f,
            maxInitTime = 2000L
        )
    )

    val result = checker.validate(
        projectPath = ".",
        configuration = config
    )

    println("Production Ready: ${result.overallStatus == ValidationStatus.PASSED}")
    println("Overall Score: ${result.overallScore}/1.0")

    // Print remediation actions
    result.remediationActions.forEach { action ->
        println("\n📋 ${action.title}")
        action.steps.forEach { step ->
            println("   - $step")
        }
    }
}
```

## Common Issues and Remediation

### Test Coverage Below 95%

```bash
# Generate detailed coverage report
./gradlew koverHtmlReport

# Open report
open build/reports/kover/html/index.html

# Focus on uncovered code in core modules
# Add tests for uncovered branches
```

### Platform Compilation Failure

```bash
# Check specific platform
./gradlew :kreekt-core:compileKotlinJs

# Common fixes:
# - Update Kotlin version
# - Check expect/actual implementations
# - Verify platform dependencies
```

### Performance Below 120 FPS

```bash
# Profile rendering
./gradlew runPerformanceProfiler

# Common optimizations:
# - Enable GPU instancing
# - Reduce draw calls
# - Optimize shaders
# - Implement LOD system
```

### Library Size Exceeds 2MB

```bash
# Analyze artifact size
./gradlew analyzeArtifactSize

# Size reduction strategies:
# - Enable R8/ProGuard minification
# - Remove unused dependencies
# - Split into smaller modules
# - Use dynamic feature modules
```

## Validation Scenarios

### Scenario 1: Pre-Release Validation

**Given**: Ready to release new version
**When**: Run full validation suite
**Then**: Get comprehensive report with go/no-go decision

```bash
./gradlew validateProductionReadiness --strict
```

### Scenario 2: PR Validation

**Given**: Pull request with new feature
**When**: Validate affected modules
**Then**: Ensure no regressions

```bash
./gradlew validateProductionReadiness --incremental
```

### Scenario 3: Daily Build

**Given**: Nightly build process
**When**: Run validation
**Then**: Track trends over time

```bash
./gradlew validateProductionReadiness --baseline
```

## Configuration Options

### Custom Thresholds

Create `validation.properties`:

```properties
coverage.threshold=95
performance.fps.min=120
size.max.bytes=2097152
compilation.timeout=300000
```

### Platform Selection

```bash
# Validate specific platforms only
./gradlew validateProductionReadiness \
  -Pplatforms=JVM,JS,ANDROID

# Skip unavailable platforms
./gradlew validateProductionReadiness \
  -PskipUnavailable=true
```

### Report Formats

```bash
# JSON only
./gradlew validateProductionReadiness \
  -PreportFormat=json

# HTML with charts
./gradlew validateProductionReadiness \
  -PreportFormat=html-enhanced

# JUnit XML for CI
./gradlew validateProductionReadiness \
  -PreportFormat=junit
```

## Troubleshooting

### Validation Hangs

```bash
# Run with timeout
./gradlew validateProductionReadiness \
  --timeout=10m

# Debug mode
./gradlew validateProductionReadiness \
  --debug
```

### Out of Memory

```bash
# Increase heap size
./gradlew validateProductionReadiness \
  -Dorg.gradle.jvmargs="-Xmx4g"
```

### Platform Not Found

```bash
# Check available platforms
./gradlew listPlatforms

# Install missing SDKs
# See platform-specific setup guides
```

## Next Steps

1. Review the generated report
2. Address any failed criteria
3. Implement suggested remediations
4. Re-run validation
5. Integrate into CI/CD pipeline

For detailed documentation, see the [Production Readiness Guide](../../docs/production-readiness.md).