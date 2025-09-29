#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

package tools.scanning

import kotlinx.coroutines.*
import java.io.File
import java.nio.file.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.streams.asSequence

/**
 * T031: Expect/Actual Validation Across All Platform Source Sets
 *
 * This script validates that all expect declarations have corresponding
 * actual implementations across JVM, JS, Native platforms and identifies
 * any gaps or incomplete implementations.
 *
 * Usage: kotlin T031_ExpectActualValidation.kt [project-root]
 */

// Copy required data types locally for script execution
enum class Platform(val sourceDir: String) {
    JVM("jvmMain"),
    JS("jsMain"),
    ANDROID("androidMain"),
    IOS("iosMain"),
    LINUX_X64("linuxX64Main"),
    MINGW_X64("mingwX64Main"),
    MACOS_X64("macosX64Main"),
    MACOS_ARM64("macosArm64Main"),
    NATIVE("nativeMain")
}

data class GapAnalysisResult(
    val gaps: List<ImplementationGap>,
    val analysisTimestamp: Long,
    val totalExpectDeclarations: Int,
    val platformsCovered: List<Platform>,
    val modulesCovered: List<String>
)

data class ImplementationGap(
    val filePath: String,
    val expectedSignature: String,
    val platform: Platform,
    val module: String,
    val lineNumber: Int,
    val gapType: GapType,
    val severity: GapSeverity,
    val context: String
)

enum class GapType {
    MISSING_ACTUAL,
    INCOMPLETE_IMPLEMENTATION,
    STUB_IMPLEMENTATION,
    PLATFORM_SPECIFIC_MISSING
}

enum class GapSeverity {
    CRITICAL,    // Core functionality missing
    HIGH,        // Important feature missing
    MEDIUM,      // Nice-to-have feature missing
    LOW          // Optional or minor feature missing
}

class ExpectActualValidator {
    companion object {
        // Patterns to identify expect declarations
        private val EXPECT_PATTERNS = listOf(
            "(?m)^\\s*expect\\s+class\\s+(\\w+)",
            "(?m)^\\s*expect\\s+interface\\s+(\\w+)",
            "(?m)^\\s*expect\\s+object\\s+(\\w+)",
            "(?m)^\\s*expect\\s+fun\\s+(\\w+)",
            "(?m)^\\s*expect\\s+val\\s+(\\w+)",
            "(?m)^\\s*expect\\s+var\\s+(\\w+)"
        )

        // Patterns to identify actual declarations
        private val ACTUAL_PATTERNS = listOf(
            "(?m)^\\s*actual\\s+class\\s+(\\w+)",
            "(?m)^\\s*actual\\s+interface\\s+(\\w+)",
            "(?m)^\\s*actual\\s+object\\s+(\\w+)",
            "(?m)^\\s*actual\\s+fun\\s+(\\w+)",
            "(?m)^\\s*actual\\s+val\\s+(\\w+)",
            "(?m)^\\s*actual\\s+var\\s+(\\w+)"
        )

        // Patterns that indicate stub implementations
        private val STUB_PATTERNS = listOf(
            "(?i)\\btodo\\b.*",
            "(?i)\\bfixme\\b.*",
            "(?i)notImplemented\\(\\)",
            "(?i)throw\\s+.*notImplementedError",
            "(?i)throw\\s+.*unsupportedOperationException",
            "(?i)\\bstub\\b.*implementation",
            "(?i)return\\s+null\\s*//.*stub",
            "(?i)//\\s*stub.*implementation",
            "(?i)return\\s+emptyList\\(\\)\\s*//.*stub",
            "(?i)return\\s+false\\s*//.*stub",
            "(?i)return\\s+true\\s*//.*stub"
        )

        // Critical modules that must have complete implementations
        private val CRITICAL_MODULES = setOf("core", "renderer", "scene", "geometry", "material")

        // Priority platforms for validation
        private val PRIORITY_PLATFORMS = listOf(Platform.JVM, Platform.JS, Platform.ANDROID)
    }

    suspend fun analyzeImplementationGaps(sourceRoot: String): GapAnalysisResult = withContext(Dispatchers.IO) {
        val analysisTimestamp = System.currentTimeMillis()
        val gaps = mutableListOf<ImplementationGap>()
        val modulesCovered = mutableSetOf<String>()

        println("🔍 Starting expect/actual validation...")
        println("📁 Source root: $sourceRoot")

        try {
            // Find all expect declarations in commonMain
            val expectDeclarations = findExpectDeclarations(sourceRoot)
            println("📊 Found ${expectDeclarations.size} expect declarations")

            if (expectDeclarations.isEmpty()) {
                println("⚠️ No expect declarations found - this might indicate the scan location is incorrect")
                return@withContext GapAnalysisResult(
                    gaps = gaps,
                    analysisTimestamp = analysisTimestamp,
                    totalExpectDeclarations = 0,
                    platformsCovered = emptyList(),
                    modulesCovered = emptyList()
                )
            }

            // Check which platforms have source directories
            val availablePlatforms = findAvailablePlatforms(sourceRoot)
            println("🎯 Available platforms: ${availablePlatforms.map { it.name }}")

            // For each expect declaration, check if it has actual implementations on all platforms
            for ((index, expectInfo) in expectDeclarations.withIndex()) {
                if (index % 10 == 0) {
                    println("📦 Processing expect declaration ${index + 1}/${expectDeclarations.size}")
                }

                modulesCovered.add(expectInfo.module)

                for (platform in availablePlatforms) {
                    val hasActual = hasActualImplementation(expectInfo.signature, platform, sourceRoot)

                    if (!hasActual) {
                        gaps.add(
                            ImplementationGap(
                                filePath = expectInfo.filePath,
                                expectedSignature = expectInfo.signature,
                                platform = platform,
                                module = expectInfo.module,
                                lineNumber = expectInfo.lineNumber,
                                gapType = GapType.MISSING_ACTUAL,
                                severity = calculateGapSeverity(expectInfo.module, expectInfo.signature),
                                context = expectInfo.context
                            )
                        )
                    } else {
                        // Check if the actual implementation is complete (not a stub)
                        val stubGaps = findStubImplementations(sourceRoot, platform, expectInfo.signatureName)
                        gaps.addAll(stubGaps)
                    }
                }
            }

        } catch (e: Exception) {
            println("❌ Error analyzing implementation gaps: ${e.message}")
            e.printStackTrace()
        }

        val endTime = System.currentTimeMillis()
        println("✅ Analysis completed in ${endTime - analysisTimestamp}ms")
        println("🔍 Found ${gaps.size} implementation gaps")

        GapAnalysisResult(
            gaps = gaps,
            analysisTimestamp = analysisTimestamp,
            totalExpectDeclarations = expectDeclarations.size,
            platformsCovered = availablePlatforms,
            modulesCovered = modulesCovered.toList()
        )
    }

    private fun findExpectDeclarations(sourceRoot: String): List<ExpectInfo> {
        val declarations = mutableListOf<ExpectInfo>()

        try {
            val commonMainDir = File(sourceRoot, "src/commonMain/kotlin")
            if (!commonMainDir.exists()) {
                println("⚠️ commonMain directory not found: ${commonMainDir.absolutePath}")
                return declarations
            }

            Files.walk(commonMainDir.toPath())
                .asSequence()
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .forEach { file ->
                    try {
                        val content = Files.readString(file)
                        val lines = content.lines()
                        val module = extractModuleName(file.toString())

                        for ((lineIndex, line) in lines.withIndex()) {
                            for (pattern in EXPECT_PATTERNS) {
                                val matches = Regex(pattern).findAll(line)
                                for (match in matches) {
                                    val signature = line.trim()
                                    val signatureName = match.groupValues[1]

                                    declarations.add(
                                        ExpectInfo(
                                            filePath = file.toString(),
                                            signature = signature,
                                            signatureName = signatureName,
                                            module = module,
                                            lineNumber = lineIndex + 1,
                                            context = extractContext(lines, lineIndex)
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("⚠️ Error reading file ${file}: ${e.message}")
                    }
                }

        } catch (e: Exception) {
            println("❌ Error finding expect declarations: ${e.message}")
        }

        return declarations
    }

    private fun findAvailablePlatforms(sourceRoot: String): List<Platform> {
        val srcDir = File(sourceRoot, "src")
        if (!srcDir.exists()) return emptyList()

        return Platform.values().filter { platform ->
            File(srcDir, platform.sourceDir).exists()
        }
    }

    private fun hasActualImplementation(expectSignature: String, platform: Platform, sourceRoot: String): Boolean {
        try {
            val signatureName = extractSignatureName(expectSignature)
            val platformSourceDir = File(sourceRoot, "src/${platform.sourceDir}/kotlin")

            if (!platformSourceDir.exists()) return false

            // Search for actual implementations in platform source directory
            return Files.walk(platformSourceDir.toPath())
                .asSequence()
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .any { file ->
                    try {
                        val content = Files.readString(file)
                        ACTUAL_PATTERNS.any { pattern ->
                            val regex = Regex(pattern)
                            regex.findAll(content).any { match ->
                                match.groupValues[1] == signatureName
                            }
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
        } catch (e: Exception) {
            return false
        }
    }

    private fun findStubImplementations(
        sourceRoot: String,
        platform: Platform,
        signatureName: String
    ): List<ImplementationGap> {
        val stubs = mutableListOf<ImplementationGap>()

        try {
            val platformSourceDir = File(sourceRoot, "src/${platform.sourceDir}/kotlin")
            if (!platformSourceDir.exists()) return stubs

            Files.walk(platformSourceDir.toPath())
                .asSequence()
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .forEach { file ->
                    try {
                        val content = Files.readString(file)
                        val lines = content.lines()
                        val module = extractModuleName(file.toString())

                        // Look for the specific signature and check if it has stubs
                        if (content.contains(signatureName)) {
                            for ((lineIndex, line) in lines.withIndex()) {
                                for (pattern in STUB_PATTERNS) {
                                    if (Regex(pattern).containsMatchIn(line)) {
                                        // Try to find the function/class name this stub belongs to
                                        val signature = findSignatureForStub(lines, lineIndex, signatureName)

                                        if (signature.contains(signatureName)) {
                                            stubs.add(
                                                ImplementationGap(
                                                    filePath = file.toString(),
                                                    expectedSignature = signature,
                                                    platform = platform,
                                                    module = module,
                                                    lineNumber = lineIndex + 1,
                                                    gapType = GapType.STUB_IMPLEMENTATION,
                                                    severity = calculateGapSeverity(module, signature),
                                                    context = extractContext(lines, lineIndex)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Skip files we can't read
                    }
                }

        } catch (e: Exception) {
            println("❌ Error finding stub implementations: ${e.message}")
        }

        return stubs
    }

    private fun extractSignatureName(declaration: String): String {
        return Regex("\\b(?:class|interface|object|fun|val|var)\\s+(\\w+)").find(declaration)?.groupValues?.get(1) ?: ""
    }

    private fun extractModuleName(filePath: String): String {
        val path = filePath.replace("\\", "/")

        return when {
            path.contains("/core/") -> "core"
            path.contains("/renderer/") -> "renderer"
            path.contains("/scene/") -> "scene"
            path.contains("/geometry/") -> "geometry"
            path.contains("/material/") -> "material"
            path.contains("/animation/") -> "animation"
            path.contains("/loader/") -> "loader"
            path.contains("/controls/") -> "controls"
            path.contains("/physics/") -> "physics"
            path.contains("/xr/") -> "xr"
            path.contains("/postprocess/") -> "postprocess"
            path.contains("/tools/") -> "tools"
            path.contains("/examples/") -> "examples"
            path.contains("Test") -> "test"
            else -> "common"
        }
    }

    private fun calculateGapSeverity(module: String, signature: String): GapSeverity {
        return when {
            module in CRITICAL_MODULES -> {
                when {
                    signature.contains("render") || signature.contains("draw") -> GapSeverity.CRITICAL
                    signature.contains("init") || signature.contains("create") -> GapSeverity.HIGH
                    else -> GapSeverity.MEDIUM
                }
            }

            else -> GapSeverity.LOW
        }
    }

    private fun extractContext(lines: List<String>, lineIndex: Int): String {
        val startIndex = maxOf(0, lineIndex - 2)
        val endIndex = minOf(lines.size - 1, lineIndex + 2)
        return lines.subList(startIndex, endIndex + 1).joinToString("\\n")
    }

    private fun findSignatureForStub(lines: List<String>, stubLineIndex: Int, targetSignature: String): String {
        // Look backwards from the stub line to find the function/class signature
        for (i in stubLineIndex downTo maxOf(0, stubLineIndex - 10)) {
            val line = lines[i].trim()
            if (line.contains(targetSignature) &&
                line.matches(Regex(".*\\b(fun|class|object|interface|val|var)\\s+$targetSignature.*"))
            ) {
                return line
            }
        }
        return "unknown signature containing $targetSignature"
    }

    private data class ExpectInfo(
        val filePath: String,
        val signature: String,
        val signatureName: String,
        val module: String,
        val lineNumber: Int,
        val context: String
    )
}

// Report generation functions
fun generateGapAnalysisReport(result: GapAnalysisResult): String {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    val criticalCount = result.gaps.count { it.severity == GapSeverity.CRITICAL }
    val highCount = result.gaps.count { it.severity == GapSeverity.HIGH }
    val mediumCount = result.gaps.count { it.severity == GapSeverity.MEDIUM }
    val lowCount = result.gaps.count { it.severity == GapSeverity.LOW }

    val gapTypeBreakdown = result.gaps
        .groupBy { it.gapType }
        .mapValues { it.value.size }

    val platformBreakdown = result.gaps
        .groupBy { it.platform }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    val moduleBreakdown = result.gaps
        .groupBy { it.module }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    return """
# T031: Expect/Actual Validation Report
Generated: $timestamp

## Executive Summary
- **Total expect declarations:** ${result.totalExpectDeclarations}
- **Total implementation gaps:** ${result.gaps.size}
- **Platforms analyzed:** ${result.platformsCovered.size}
- **Modules covered:** ${result.modulesCovered.size}

## Gap Severity Breakdown
- 🔴 **CRITICAL:** $criticalCount gaps (blocks deployment)
- 🟠 **HIGH:** $highCount gaps (important features missing)
- 🟡 **MEDIUM:** $mediumCount gaps (nice-to-have features)
- 🟢 **LOW:** $lowCount gaps (optional features)

## Gap Type Analysis
${gapTypeBreakdown.entries.joinToString("\n") { "- **${it.key}:** ${it.value} instances" }}

## Platform-Specific Gaps
${platformBreakdown.joinToString("\n") { "- **${it.first}:** ${it.second} gaps" }}

## Module Analysis
${moduleBreakdown.take(10).joinToString("\n") { "- **${it.first}:** ${it.second} gaps" }}

## Critical Implementation Gaps (Immediate Action Required)
${
        result.gaps.filter { it.severity == GapSeverity.CRITICAL }.take(20).joinToString("\n") {
            "### ${it.module}.${it.expectedSignature} (${it.platform})\n" +
                    "- **File:** ${it.filePath}\n" +
                    "- **Line:** ${it.lineNumber}\n" +
                    "- **Type:** ${it.gapType}\n" +
                    "- **Context:** ${it.context.replace("\\n", " ")}\n"
        }
    }

## High Priority Gaps
${
        result.gaps.filter { it.severity == GapSeverity.HIGH }.take(15).joinToString("\n") {
            "### ${it.module}.${it.expectedSignature} (${it.platform})\n" +
                    "- **File:** ${it.filePath}\n" +
                    "- **Line:** ${it.lineNumber}\n" +
                    "- **Type:** ${it.gapType}\n"
        }
    }

## Platform Completeness Analysis
${
        result.platformsCovered.joinToString("\n") { platform ->
            val platformGaps = result.gaps.filter { it.platform == platform }
            val completeness = if (result.totalExpectDeclarations > 0) {
                ((result.totalExpectDeclarations - platformGaps.size).toFloat() / result.totalExpectDeclarations * 100).toInt()
            } else 100
            "- **${platform.name}:** $completeness% complete (${platformGaps.size} gaps)"
        }
    }

## Recommendations
1. **Immediate:** Fix all CRITICAL gaps (${criticalCount} items) - deployment blocked
2. **Next:** Complete HIGH priority implementations in core modules
3. **Medium-term:** Address MEDIUM priority gaps for feature completeness
4. **Long-term:** Implement LOW priority items for platform parity

## Focus Areas for T032 Automated Fixing
1. **Renderer module:** Critical for JavaScript black screen issue
2. **Core module:** Foundation functionality
3. **Scene module:** Core 3D functionality
4. **Stub implementations:** Easy wins with pattern replacement

## Next Steps
1. Review critical gaps and prioritize fixes
2. Run T032 automated placeholder fixing
3. Implement missing actual declarations
4. Validate all platforms build successfully
5. Ensure 627 tests continue to pass
    """.trimIndent()
}

fun saveGapAnalysisResults(result: GapAnalysisResult, outputDir: String) {
    val outputFile = File(outputDir, "T031_gap_analysis.md")
    outputFile.parentFile.mkdirs()

    val report = generateGapAnalysisReport(result)
    outputFile.writeText(report)

    println("📊 Gap analysis report saved to: ${outputFile.absolutePath}")

    // Also save raw data for further processing
    val dataFile = File(outputDir, "T031_gap_data.txt")
    val dataOutput = buildString {
        appendLine("# Raw Gap Analysis Data - T031")
        appendLine("# Total gaps: ${result.gaps.size}")
        appendLine("# Analysis timestamp: ${result.analysisTimestamp}")
        appendLine("")

        for (gap in result.gaps) {
            appendLine("FILE: ${gap.filePath}")
            appendLine("LINE: ${gap.lineNumber}")
            appendLine("SIGNATURE: ${gap.expectedSignature}")
            appendLine("PLATFORM: ${gap.platform}")
            appendLine("MODULE: ${gap.module}")
            appendLine("TYPE: ${gap.gapType}")
            appendLine("SEVERITY: ${gap.severity}")
            appendLine("CONTEXT: ${gap.context.replace("\\n", "\\\\n")}")
            appendLine("---")
        }
    }
    dataFile.writeText(dataOutput)

    println("💾 Raw gap data saved to: ${dataFile.absolutePath}")
}

// Main execution
suspend fun main(args: Array<String>) {
    val projectRoot = args.getOrNull(0) ?: File("").absolutePath
    val outputDir = File(projectRoot, "docs/private/scan-results").absolutePath

    println("🚀 T031: Expect/Actual Validation Starting...")
    println("📁 Project root: $projectRoot")
    println("📊 Output directory: $outputDir")

    val validator = ExpectActualValidator()
    val result = validator.analyzeImplementationGaps(projectRoot)

    // Generate and save reports
    saveGapAnalysisResults(result, outputDir)

    // Print summary to console
    println("\n" + "=".repeat(80))
    println("📊 T031 VALIDATION COMPLETE")
    println("=".repeat(80))
    println("✅ Expect declarations: ${result.totalExpectDeclarations}")
    println("🔍 Implementation gaps: ${result.gaps.size}")
    println("🎯 Platforms analyzed: ${result.platformsCovered.size}")
    println("📦 Modules covered: ${result.modulesCovered.size}")
    println("🔴 Critical gaps: ${result.gaps.count { it.severity == GapSeverity.CRITICAL }}")
    println("🟠 High priority gaps: ${result.gaps.count { it.severity == GapSeverity.HIGH }}")

    val platformCompleteness = result.platformsCovered.map { platform ->
        val platformGaps = result.gaps.filter { it.platform == platform }
        val completeness = if (result.totalExpectDeclarations > 0) {
            ((result.totalExpectDeclarations - platformGaps.size).toFloat() / result.totalExpectDeclarations * 100).toInt()
        } else 100
        platform.name to completeness
    }.sortedBy { it.second }

    println("\n🎯 Platform completeness:")
    platformCompleteness.forEach { (platform, percentage) ->
        println("   - $platform: $percentage%")
    }

    println("\n📋 Next Steps:")
    println("   1. Review critical gaps in report")
    println("   2. Implement missing actual declarations")
    println("   3. Run T032 automated placeholder fixing")
    println("   4. Focus on renderer module for JS black screen")

    if (result.gaps.any { it.severity == GapSeverity.CRITICAL }) {
        println("\n⚠️  CRITICAL gaps found - production deployment blocked!")
    }
}

// Execute the main function
runBlocking {
    main(args)
}