# Repository Guidelines

## Project Structure & Module Organization

- Core multiplatform source lives in src/commonMain with platform-specific implementations in src/jvmMain, src/jsMain,
  and src/nativeMain; keep shared APIs in common and gate platform code behind expect/actual.
- Feature modules like kreekt-loader, kreekt-helpers, and kreekt-validation hold loaders, utilities, and the validation
  CLI; runnable demos sit in examples/*, reference material in docs/, and audit specs under specs/.
- System tests live in ests/ grouped into contract, integration, performance, and isual; mirror this layout when adding
  suites.

## Build, Test, and Development Commands

- ./gradlew build compiles all targets and runs default checks; prefer this before pushing.
- ./gradlew test executes multiplatform unit suites; scope to a module with ./gradlew :kreekt-validation:test.
- ./gradlew koverVerify enforces the current 50% coverage gate, and ./gradlew koverHtmlReport produces
  uild/reports/kover/html/index.html.
- ./gradlew listExamples surfaces runnable demos; launch the primary scene via ./gradlew :examples:basic-scene:runJvm or
  unJs.
- ./gradlew validateProductionReadiness runs the validation CLI and emits reports in
  kreekt-validation/build/validation-reports/; invoke ./gradlew dependencyCheckAnalyze during security reviews.
- ./gradlew dokkaHtml builds API docs into uild/dokka/html.

## Coding Style & Naming Conventions

- Follow Kotlin official style: four-space indentation, trailing commas where they improve diffs, and explicit
  visibility; avoid !! and unchecked casts as per core guidelines.
- Public APIs use UpperCamelCase types and lowerCamelCase members; keep Gradle tasks in lowerCamelCase (
  unSimpleDemo) and module directories hyphenated (kreekt-loader).
- Run the formatter in your IDE; CI tooling under ools/cicd reads ktlint reports from uild/reports/ktlint/, so mirror
  that output if you lint locally.

## Testing Guidelines

- Write unit tests in src/*Test using kotlin.test; JVM-specific cases rely on JUnit 5 and MockK (see kreekt-validation).
- Integration and performance scenarios belong in ests/integration with descriptive *Test.kt filenames (
  BasicSceneTest.kt, PerformanceTest.kt).
- Generate coverage via ./gradlew koverHtmlReport and keep overall coverage above the enforced 50% while working toward
  the documented 95% goal.

## Commit & Pull Request Guidelines

- Commits mirror the existing history: short, imperative subject lines without prefixes (Fix VoxelCraft terrain
  generation, Add comprehensive documentation).
- Each PR should summarise scope, list validation steps (./gradlew build, key demos), and link the relevant roadmap item
  or issue; attach coverage artifacts for core logic and screenshots for visual work.
- Keep branches focused, rebase before review, and request CI validation for cross-platform changes.
