package io.kreekt.validation.contracts

import io.kreekt.validation.*

/**
 * Contract interface for PlaceholderScanner implementation.
 *
 * This contract defines the required behavior for scanning source code
 * to detect placeholder patterns that need production implementation.
 *
 * CONTRACT REQUIREMENTS:
 * 1. scanDirectory() MUST recursively scan all matching files
 * 2. scanFile() MUST detect all defined placeholder patterns
 * 3. getDetectionPatterns() MUST return comprehensive pattern list
 * 4. validatePlaceholder() MUST filter false positives (documentation examples)
 * 5. estimateReplacementEffort() MUST categorize by complexity
 * 6. All file I/O MUST use actual platform-specific FileSystem APIs
 * 7. NO stub implementations returning empty results are allowed
 *
 * CONSTITUTIONAL COMPLIANCE:
 * - Test-Driven Development: Tests exist first, implementation must make them pass
 * - Production-Ready Only: No temporary solutions or placeholders in scanner itself
 * - Cross-Platform: Must work on JVM, JS, and Native platforms
 */
interface PlaceholderScanner {

    /**
     * Scans the specified directory for placeholder patterns.
     *
     * CONTRACT:
     * - MUST scan all files matching filePatterns recursively
     * - MUST exclude files matching excludePatterns
     * - MUST return ScanResult with all detected placeholders
     * - MUST include accurate metadata (timestamp, duration, file count)
     * - MUST handle permission errors gracefully (skip file, continue scanning)
     * - MUST detect patterns case-insensitively
     *
     * @param rootPath The root directory to scan (must be absolute path)
     * @param filePatterns File patterns to include (e.g., "*.kt", "*.md")
     * @param excludePatterns Patterns to exclude from scanning (e.g., "**/build/**")
     * @return ScanResult containing all detected placeholders
     * @throws IllegalArgumentException if rootPath is not absolute
     */
    suspend fun scanDirectory(
        rootPath: String,
        filePatterns: List<String> = listOf("*.kt", "*.md", "*.gradle.kts"),
        excludePatterns: List<String> = listOf("**/build/**", "**/node_modules/**")
    ): ScanResult

    /**
     * Scans a single file for placeholder patterns.
     *
     * CONTRACT:
     * - MUST detect all placeholder patterns defined in getDetectionPatterns()
     * - MUST include context (surrounding lines) for each match
     * - MUST determine criticality based on module and pattern type
     * - MUST identify module name from file path
     * - MUST identify platform (jvm/js/native) from file path if applicable
     * - MUST return empty list if file doesn't exist or can't be read
     * - MUST filter out false positives using validatePlaceholder()
     *
     * @param filePath Path to file to scan (must be absolute path)
     * @return List of PlaceholderInstance found in the file
     */
    suspend fun scanFile(filePath: String): List<PlaceholderInstance>

    /**
     * Gets the list of placeholder patterns this scanner detects.
     *
     * CONTRACT:
     * - MUST include: TODO, FIXME, placeholder, stub (excluding test stub() calls)
     * - MUST include: "in the meantime", "for now", "in a real implementation"
     * - MUST include: mock/temporary in implementation context
     * - MUST return regex patterns as strings
     * - MUST be case-insensitive patterns
     * - Pattern list MUST match constitution requirements
     *
     * @return List of regex patterns used for detection
     */
    fun getDetectionPatterns(): List<String>

    /**
     * Validates that a potential placeholder is actually a placeholder
     * and not a false positive (e.g., in documentation about placeholders).
     *
     * CONTRACT:
     * - MUST return false for documentation examples mentioning TODO/FIXME
     * - MUST return false for test files describing what should be tested
     * - MUST return false for markdown files with implementation plans
     * - MUST return false for contract/specification comments
     * - MUST return true for actual code with placeholder implementations
     * - MUST analyze context, not just pattern match
     *
     * @param instance The potential placeholder instance
     * @param fileContent Full file content for context analysis
     * @return True if this is a genuine placeholder to be replaced
     */
    fun validatePlaceholder(instance: PlaceholderInstance, fileContent: String): Boolean

    /**
     * Estimates the effort required to replace a placeholder.
     *
     * CONTRACT:
     * - MUST categorize as TRIVIAL, SMALL, MEDIUM, or LARGE
     * - MUST consider placeholder type (STUB generally LARGE)
     * - MUST consider context keywords (implement/design = LARGE, fix/add = SMALL)
     * - MUST be consistent across multiple calls with same input
     *
     * @param instance The placeholder instance
     * @param fileContent Full file content for context analysis
     * @return Estimated effort level for replacement
     */
    fun estimateReplacementEffort(instance: PlaceholderInstance, fileContent: String): EffortLevel
}

/**
 * ACCEPTANCE CRITERIA:
 *
 * 1. Directory Scanning:
 *    GIVEN a directory with .kt files containing TODO comments
 *    WHEN scanDirectory() is called
 *    THEN all TODO instances are detected and returned in ScanResult
 *
 * 2. File Pattern Filtering:
 *    GIVEN filePatterns = ["*.kt"]
 *    WHEN scanDirectory() is called
 *    THEN only .kt files are scanned, other files are ignored
 *
 * 3. Exclude Pattern Filtering:
 *    GIVEN excludePatterns = ["**/build/**"]
 *    WHEN scanDirectory() is called
 *    THEN files in build/ directories are skipped
 *
 * 4. False Positive Filtering:
 *    GIVEN a file with "Example: TODO in documentation"
 *    WHEN scanFile() is called
 *    THEN this instance is filtered out (validatePlaceholder returns false)
 *
 * 5. Context Extraction:
 *    GIVEN a TODO comment on line 10
 *    WHEN scanFile() is called
 *    THEN PlaceholderInstance includes lines 8-12 as context
 *
 * 6. Criticality Assessment:
 *    GIVEN a TODO in "kreekt-core" module
 *    WHEN scanFile() is called
 *    THEN PlaceholderInstance has criticality = CRITICAL
 *
 * 7. Pattern Coverage:
 *    GIVEN getDetectionPatterns() returns N patterns
 *    WHEN a file contains all N pattern types
 *    THEN scanFile() detects all N instances
 *
 * 8. Effort Estimation:
 *    GIVEN PlaceholderInstance with type=STUB and context containing "implement"
 *    WHEN estimateReplacementEffort() is called
 *    THEN it returns EffortLevel.LARGE
 *
 * 9. Cross-Platform Support:
 *    GIVEN the scanner is used on JVM, JS, and Native platforms
 *    WHEN scanDirectory() is called on each platform
 *    THEN results are consistent (same placeholders detected)
 *
 * 10. Performance:
 *     GIVEN a directory with 1000 files
 *     WHEN scanDirectory() is called
 *     THEN scan completes within 30 seconds
 */