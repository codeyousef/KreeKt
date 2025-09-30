# Research: Android Native Platform Support

## Problem Statement

The validation module needs to support **all** Kotlin Multiplatform targets including Android Native (
androidNativeArm32, androidNativeArm64, androidNativeX86, androidNativeX64), but `kotlinx-datetime:0.5.0` doesn't
provide artifacts for these targets.

## Investigation

### Dependency Analysis

**kotlinx-datetime:0.5.0** supports:

- ✅ JVM
- ✅ JS
- ✅ Native: Linux, Windows, macOS
- ✅ iOS (all variants)
- ✅ watchOS (all variants)
- ✅ tvOS (all variants)
- ❌ Android Native (arm32, arm64, x86, x64)

### Solution Options

#### Option 1: Use kotlinx-datetime only in supported source sets ✅ RECOMMENDED

**Approach**: Move `kotlinx-datetime` dependency from `commonMain` to platform-specific source sets that support it.

**Pros**:

- Maintains support for all platforms
- Uses appropriate APIs per platform
- No dependency conflicts

**Cons**:

- Requires expect/actual for date/time functionality
- Slightly more code

**Implementation**:

```kotlin
// commonMain - expect declarations
expect class Instant {
    val epochSeconds: Long
    val nanosecondsOfSecond: Int
}

expect fun currentInstant(): Instant

// Platforms with kotlinx-datetime support
actual typealias Instant = kotlinx.datetime.Instant
actual fun currentInstant() = kotlinx.datetime.Clock.System.now()

// Android Native platforms
actual class Instant(
    actual val epochSeconds: Long,
    actual val nanosecondsOfSecond: Int
)

actual fun currentInstant(): Instant {
    val millis = platform.posix.time(null) * 1000
    return Instant(millis / 1000, ((millis % 1000) * 1_000_000).toInt())
}
```

#### Option 2: Upgrade kotlinx-datetime to latest version

**Status**: Checked latest versions - Android Native support added in 0.6.0+

**Pros**:

- Clean solution
- Full feature parity

**Cons**:

- Requires dependency upgrade
- May have breaking changes

**Recommendation**: Upgrade to `kotlinx-datetime:0.6.1` (latest stable)

#### Option 3: Remove datetime dependency entirely

**Approach**: Use epoch milliseconds (Long) instead of Instant type

**Pros**:

- No external dependency
- Works everywhere

**Cons**:

- Less type-safe
- Manual date formatting
- No timezone support

### Chosen Solution: Option 2 (Upgrade kotlinx-datetime)

**Decision**: Upgrade to `kotlinx-datetime:0.6.1` which has full Android Native support.

**Rationale**:

1. Cleanest solution - no workarounds needed
2. Latest version has bug fixes and improvements
3. Maintains type safety across all platforms
4. Official Kotlin library with guaranteed support

**Migration Steps**:

1. Update dependency: `implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")`
2. Test compilation on all platforms
3. Verify no breaking API changes affect validation code

### Alternative: Conditional Dependency (Fallback if upgrade fails)

If upgrading causes issues, implement Option 1 with expect/actual pattern:

```kotlin
// build.gradle.kts
sourceSets {
    val commonMain by getting {
        dependencies {
            // No datetime here
        }
    }

    val jvmMain by getting {
        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
        }
    }

    val jsMain by getting {
        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
        }
    }

    val nativeMain by creating {
        dependsOn(commonMain)
        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
        }
    }

    // Android Native uses POSIX time functions
    val androidNativeMain by creating {
        dependsOn(commonMain)
        // No kotlinx-datetime dependency
    }
}
```

## Implementation Plan

### Immediate Action

1. Update `kotlinx-datetime` to version 0.6.1 in `kreekt-validation/build.gradle.kts`
2. Verify compilation across all platforms
3. Run tests to confirm no breakage

### Verification Checklist

- [ ] JVM compilation succeeds
- [ ] JS compilation succeeds
- [ ] Native (Linux/Windows/macOS) compilation succeeds
- [ ] iOS platforms compilation succeeds
- [ ] watchOS platforms compilation succeeds
- [ ] tvOS platforms compilation succeeds
- [ ] **Android Native platforms compilation succeeds**
- [ ] All tests pass
- [ ] No breaking API changes detected

## References

- kotlinx-datetime releases: https://github.com/Kotlin/kotlinx-datetime/releases
- Version 0.6.0 changelog: Added Android Native targets support
- Version 0.6.1 (latest): Bug fixes and stability improvements