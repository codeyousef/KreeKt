# Quickstart: Validation and Production Readiness Workflow

**Feature**: 010-systematically-address-all
**Date**: 2025-09-30
**Purpose**: Demonstrate how to use validation infrastructure once implemented

## Overview

This quickstart guide demonstrates the production readiness validation workflow for the KreeKt library. Follow these steps to scan for placeholders, validate compilation, and generate comprehensive readiness reports.

---

## Prerequisites

- KreeKt project cloned and configured
- JDK 17+ installed
- Gradle wrapper available (`./gradlew`)
- All implementations from this feature complete

---

## Quick Start (5 minutes)

### 1. Scan for Placeholders

```kotlin
import io.kreekt.validation.scanner.DefaultPlaceholderScanner

suspend fun scanCodebase() {
    val scanner = DefaultPlaceholderScanner()

    val result = scanner.scanDirectory(
        rootPath = "/path/to/KreeKt",
        filePatterns = listOf("*.kt", "*.md"),
        excludePatterns = listOf("**/build/**", "**/node_modules/**")
    )

    println("📊 Scan Results:")
    println("  Files scanned: ${result.totalFilesScanned}")
    println("  Placeholders found: ${result.placeholders.size}")
    println("  Duration: ${result.scanDurationMs}ms")

    // Group by criticality
    result.placeholders
        .groupBy { it.criticality }
        .forEach { (level, instances) ->
            println("  $level: ${instances.size}")
        }
}
```

**Expected Output** (once complete):
```
📊 Scan Results:
  Files scanned: 450
  Placeholders found: 0
  Duration: 2500ms
  CRITICAL: 0
  HIGH: 0
  MEDIUM: 0
  LOW: 0
```

### 2. Validate Compilation

```kotlin
import io.kreekt.validation.services.CompilationValidator
import io.kreekt.validation.models.Platform

suspend fun validateCompilation() {
    val validator = CompilationValidator()

    val result = validator.validateCompilation(
        projectPath = ".",
        platforms = listOf(
            Platform.JVM,
            Platform.JS,
            Platform.NATIVE_LINUX_X64,
            Platform.NATIVE_WINDOWS_X64
        ),
        timeoutMillis = 300_000L // 5 minutes
    )

    println("✅ Compilation Status: ${result.status}")

    result.platformResults.forEach { (platform, platformResult) ->
        val status = if (platformResult.success) "✓" else "✗"
        println("  $status $platform")
        if (!platformResult.success) {
            platformResult.errorMessages.take(3).forEach { error ->
                println("    - $error")
            }
        }
    }
}
```

**Expected Output**:
```
✅ Compilation Status: PASSED
  ✓ jvm
  ✓ js
  ✓ native_linux_x64
  ✓ native_windows_x64
```

### 3. Generate Production Readiness Report

```kotlin
import io.kreekt.validation.api.ProductionReadinessChecker
import io.kreekt.validation.models.ValidationConfiguration

suspend fun checkProductionReadiness() {
    val checker = ProductionReadinessChecker()
    val config = ValidationConfiguration.strict()

    val report = checker.validateProductionReadiness(
        projectPath = ".",
        configuration = config
    )

    println("🎯 Production Readiness Report")
    println("  Overall Status: ${report.overallStatus}")
    println("  Generated: ${report.generatedAt}")
    println()

    // Component results
    println("📋 Component Status:")
    println("  Compilation: ${report.compilationResults.status}")
    println("  Placeholders: ${report.placeholderScanResult.placeholders.size} found")
    println("  Tests: ${report.testResults.passedCount}/${report.testResults.totalCount} passed")
    println("  Performance: ${report.performanceMetrics.targetsMet}/${report.performanceMetrics.totalTargets} met")

    // Recommendations
    if (report.recommendations.isNotEmpty()) {
        println()
        println("💡 Recommendations:")
        report.recommendations.take(5).forEach { action ->
            println("  - ${action.description}")
        }
    }
}
```

**Expected Output** (once complete):
```
🎯 Production Readiness Report
  Overall Status: PASSED
  Generated: 1704067200000

📋 Component Status:
  Compilation: PASSED
  Placeholders: 0 found
  Tests: 337/337 passed
  Performance: 4/4 met

✅ Project is production ready!
```

---

## Detailed Workflows

### Workflow 1: Daily Development Check (2 minutes)

Run before committing code to ensure no new placeholders introduced:

```bash
# Option A: Command-line tool (if implemented)
./gradlew validateProductionReadiness --config=permissive

# Option B: Kotlin script
kotlin -script scripts/validate.kts
```

### Workflow 2: Pre-Release Validation (10 minutes)

Comprehensive check before releasing:

```kotlin
suspend fun preReleaseValidation() {
    val checker = ProductionReadinessChecker()

    // Strict validation with all checks enabled
    val report = checker.validateProductionReadiness(
        projectPath = ".",
        configuration = ValidationConfiguration(
            strictMode = true,
            enablePerformanceTests = true,
            requireAllPlatforms = true,
            allowPlaceholders = false,
            incrementalMode = false
        )
    )

    // Export report
    val reportFile = File("build/validation-reports/report.html")
    reportFile.writeText(report.toHtml())

    println("Report saved to: ${reportFile.absolutePath}")

    // Fail if not ready
    if (report.overallStatus != ValidationStatus.PASSED) {
        throw IllegalStateException("Production readiness check failed!")
    }
}
```

### Workflow 3: CI/CD Integration

Add to GitHub Actions / GitLab CI:

```yaml
# .github/workflows/production-readiness.yml
name: Production Readiness Check

on: [push, pull_request]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run production readiness validation
        run: ./gradlew validateProductionReadinessStrict
      - name: Upload validation report
        uses: actions/upload-artifact@v3
        with:
          name: validation-report
          path: build/validation-reports/
```

---

## Example Scenarios

### Scenario 1: New Feature Development

**Goal**: Ensure new feature doesn't introduce placeholders

```kotlin
// Before implementing feature
val beforeScan = scanner.scanDirectory(rootPath = ".")
println("Baseline: ${beforeScan.placeholders.size} placeholders")

// ... implement feature ...

// After implementing feature
val afterScan = scanner.scanDirectory(rootPath = ".")
val newPlaceholders = afterScan.placeholders.size - beforeScan.placeholders.size

if (newPlaceholders > 0) {
    println("⚠️ Warning: Feature introduced $newPlaceholders new placeholders")
    // Prompt developer to complete implementation
} else {
    println("✅ Feature is complete (no new placeholders)")
}
```

### Scenario 2: Fixing Failing Tests

**Goal**: Track progress on test completion

```kotlin
val validator = CompilationValidator()

// Run tests and collect results
val testResult = validator.executeTests(projectPath = ".")

val failuresByModule = testResult.failures.groupBy { it.module }

println("❌ Failing Tests by Module:")
failuresByModule.forEach { (module, failures) ->
    println("  $module: ${failures.size} failures")
    failures.take(3).forEach { failure ->
        println("    - ${failure.testName}")
    }
}

// Generate fix recommendations
failuresByModule.forEach { (module, failures) ->
    failures.forEach { failure ->
        val recommendation = generateFixRecommendation(failure)
        println("💡 Fix: $recommendation")
    }
}
```

### Scenario 3: Platform-Specific Validation

**Goal**: Validate specific platform before release

```kotlin
// Validate only JVM platform
val jvmResult = validator.validateCompilation(
    projectPath = ".",
    platforms = listOf(Platform.JVM)
)

if (jvmResult.status == ValidationStatus.PASSED) {
    println("✅ JVM platform ready for release")

    // Run JVM-specific tests
    val jvmTests = runJvmTests()
    println("  Tests: ${jvmTests.passedCount}/${jvmTests.totalCount}")

    // Check JVM examples
    val examplesWork = verifyJvmExamples()
    println("  Examples: ${if (examplesWork) "✓ Working" else "✗ Failed"}")
}
```

---

## Troubleshooting

### Issue: Scan finds unexpected placeholders

**Problem**: Scanner detects TODOs in documentation or tests

**Solution**: Check if placeholders are in excluded paths:

```kotlin
val result = scanner.scanDirectory(
    rootPath = ".",
    excludePatterns = listOf(
        "**/build/**",
        "**/node_modules/**",
        "**/specs/**",  // Exclude specification documents
        "**/*.md"       // Exclude markdown files
    )
)
```

### Issue: Compilation validation times out

**Problem**: `validateCompilation()` exceeds timeout

**Solution**: Increase timeout or validate fewer platforms:

```kotlin
val result = validator.validateCompilation(
    projectPath = ".",
    platforms = listOf(Platform.JVM),  // Validate only JVM first
    timeoutMillis = 600_000L  // 10 minutes
)
```

### Issue: False positives in placeholder detection

**Problem**: Scanner detects valid code as placeholders

**Solution**: Use `validatePlaceholder()` to check context:

```kotlin
val instances = scanner.scanFile("src/MyFile.kt")

val genuine = instances.filter { instance ->
    val fileContent = File("src/MyFile.kt").readText()
    scanner.validatePlaceholder(instance, fileContent)
}

println("Genuine placeholders: ${genuine.size}")
println("False positives: ${instances.size - genuine.size}")
```

---

## Next Steps

Once all implementations are complete:

1. ✅ Run full validation: `./gradlew validateProductionReadiness`
2. ✅ Review report: Open `build/validation-reports/report.html`
3. ✅ Fix any issues identified
4. ✅ Re-run validation until PASSED
5. ✅ Integrate into CI/CD pipeline

---

## API Reference

See contract files in `specs/010-systematically-address-all/contracts/` for complete API details.

**Key Interfaces**:
- `PlaceholderScanner` - Placeholder detection
- `CompilationValidator` - Compilation checking
- `ProductionReadinessChecker` - Comprehensive validation
- `FileScanner` - Cross-platform file I/O

---

**Quickstart Complete** | **Ready for Implementation**