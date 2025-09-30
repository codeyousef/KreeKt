package io.kreekt.validation.reporting

import io.kreekt.validation.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/**
 * Generates validation reports in various formats.
 *
 * The ReportGenerator creates comprehensive reports from production readiness
 * validation results, supporting multiple output formats including HTML, JSON,
 * and Markdown.
 */
class ReportGenerator {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Generates an HTML report from validation results.
     *
     * @param report The production readiness report
     * @param includeCharts Whether to include JavaScript charts
     * @return HTML content as a string
     */
    fun generateHtmlReport(
        report: ProductionReadinessReport,
        includeCharts: Boolean = true
    ): String {
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang='en'>")
            appendLine("<head>")
            appendLine("    <meta charset='UTF-8'>")
            appendLine("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            appendLine("    <title>KreeKt Production Readiness Report</title>")
            appendLine(getHtmlStyles())
            if (includeCharts) {
                appendLine(getChartScripts())
            }
            appendLine("</head>")
            appendLine("<body>")

            // Header
            appendLine("<div class='header'>")
            appendLine("    <h1>KreeKt Production Readiness Report</h1>")
            appendLine("    <div class='timestamp'>Generated: ${formatTimestamp(report.timestamp.toEpochMilliseconds())}</div>")
            appendLine("    <div class='project-path'>Branch: ${report.branchName} (${report.commitHash.take(8)})</div>")
            appendLine("</div>")

            // Executive Summary
            appendLine("<div class='section executive-summary'>")
            appendLine("    <h2>Executive Summary</h2>")
            appendLine("    <div class='summary-card ${if (report.isProductionReady) "ready" else "not-ready"}'>")
            appendLine("        <div class='status-indicator'></div>")
            appendLine("        <div class='status-text'>")
            appendLine("            ${if (report.isProductionReady) "✓ Production Ready" else "✗ Not Production Ready"}")
            appendLine("        </div>")
            appendLine("        <div class='overall-score'>Overall Score: ${formatPercentage(report.overallScore)}</div>")
            appendLine("    </div>")
            appendLine("</div>")

            // Constitutional Requirements
            appendLine("<div class='section constitutional'>")
            appendLine("    <h2>Constitutional Requirements</h2>")
            appendLine("    <table class='requirements-table'>")
            appendLine("        <thead>")
            appendLine("            <tr>")
            appendLine("                <th>Requirement</th>")
            appendLine("                <th>Status</th>")
            appendLine("            </tr>")
            appendLine("        </thead>")
            appendLine("        <tbody>")
            // Get constitutional category if it exists
            val constitutionalCategory = report.categories.find { it.name == ValidationCategory.CONSTITUTIONAL }
            constitutionalCategory?.criteria?.forEach { criterion ->
                val met = criterion.status == ValidationStatus.PASSED
                appendLine("            <tr class='${if (met) "passed" else "failed"}'>")
                appendLine("                <td>${criterion.name}</td>")
                appendLine("                <td>${if (met) "✓ Met" else "✗ Not Met"}</td>")
                appendLine("            </tr>")
            }
            appendLine("        </tbody>")
            appendLine("    </table>")
            appendLine("</div>")

            // Category Scores
            appendLine("<div class='section scores'>")
            appendLine("    <h2>Validation Category Scores</h2>")
            if (includeCharts) {
                appendLine("    <canvas id='scoresChart' width='400' height='200'></canvas>")
            }
            appendLine("    <table class='scores-table'>")
            appendLine("        <thead>")
            appendLine("            <tr>")
            appendLine("                <th>Category</th>")
            appendLine("                <th>Score</th>")
            appendLine("                <th>Status</th>")
            appendLine("            </tr>")
            appendLine("        </thead>")
            appendLine("        <tbody>")
            report.categories.forEach { category ->
                val status = getScoreStatus(category.score)
                appendLine("            <tr class='$status'>")
                appendLine("                <td>${category.name}</td>")
                appendLine("                <td>${formatPercentage(category.score)}</td>")
                appendLine("                <td>$status</td>")
                appendLine("            </tr>")
            }
            appendLine("        </tbody>")
            appendLine("    </table>")
            appendLine("</div>")

            // Platform Status - derive from criteria with platform info
            val platformCriteria = report.categories.flatMap { it.criteria }.filter { it.platform != null }
            if (platformCriteria.isNotEmpty()) {
                appendLine("<div class='section platforms'>")
                appendLine("    <h2>Platform Support</h2>")
                appendLine("    <div class='platform-grid'>")
                platformCriteria.groupBy { it.platform }.forEach { (platform, criteria) ->
                    val failedCount = criteria.count { it.status == ValidationStatus.FAILED }
                    val statusClass = if (failedCount == 0) "passed" else "failed"
                    appendLine("        <div class='platform-card $statusClass'>")
                    appendLine("            <h3>$platform</h3>")
                    appendLine("            <div class='platform-status'>${if (failedCount == 0) "✓ Supported" else "✗ Issues Found"}</div>")
                    if (failedCount > 0) {
                        appendLine("            <div class='platform-issues'>")
                        appendLine("                <strong>Issues ($failedCount):</strong>")
                        appendLine("                <ul>")
                        criteria.filter { it.status == ValidationStatus.FAILED }.take(3).forEach { criterion ->
                            appendLine("                    <li>${criterion.name}</li>")
                        }
                        if (failedCount > 3) {
                            appendLine("                    <li>...and ${failedCount - 3} more</li>")
                        }
                        appendLine("                </ul>")
                        appendLine("            </div>")
                    }
                    appendLine("        </div>")
                }
                appendLine("    </div>")
                appendLine("</div>")
            }

            // Issues Summary - derive from failed criteria
            val failedCriteria = report.categories.flatMap { it.criteria }.filter {
                it.status in listOf(ValidationStatus.FAILED, ValidationStatus.ERROR)
            }
            if (failedCriteria.isNotEmpty()) {
                appendLine("<div class='section issues'>")
                appendLine("    <h2>Issues Summary</h2>")

                // Group issues by severity
                val issuesBySeverity = failedCriteria.groupBy { it.severity }

                Severity.values().forEach { severity ->
                    val severityIssues = issuesBySeverity[severity] ?: emptyList()
                    if (severityIssues.isNotEmpty()) {
                        appendLine("    <div class='severity-group ${severity.name.lowercase()}'>")
                        appendLine("        <h3>${severity.name} (${severityIssues.size})</h3>")
                        appendLine("        <div class='issues-list'>")
                        severityIssues.take(5).forEach { criterion ->
                            appendLine("            <div class='issue'>")
                            appendLine("                <div class='issue-header'>")
                            appendLine("                    <strong>${criterion.name}</strong>")
                            appendLine("                </div>")
                            appendLine("                <div class='issue-description'>${criterion.description}</div>")
                            appendLine("                <div>Required: ${criterion.requirement}</div>")
                            appendLine("                <div>Actual: ${criterion.actual}</div>")
                            appendLine("            </div>")
                        }
                        if (severityIssues.size > 5) {
                            appendLine("            <div class='more-issues'>...and ${severityIssues.size - 5} more</div>")
                        }
                        appendLine("        </div>")
                        appendLine("    </div>")
                    }
                }
                appendLine("</div>")
            }

            // Metadata
            appendLine("<div class='section metadata'>")
            appendLine("    <h2>Validation Metadata</h2>")
            appendLine("    <dl class='metadata-list'>")
            appendLine("        <dt>Branch:</dt>")
            appendLine("        <dd>${report.branchName}</dd>")
            appendLine("        <dt>Commit:</dt>")
            appendLine("        <dd>${report.commitHash.take(8)}</dd>")
            appendLine("        <dt>Execution Time:</dt>")
            appendLine("        <dd>${report.executionTime}</dd>")
            appendLine("    </dl>")
            appendLine("</div>")

            // Footer
            appendLine("<div class='footer'>")
            appendLine("    <p>Generated by KreeKt Validation System</p>")
            appendLine("</div>")

            if (includeCharts) {
                appendLine(generateChartScript(report))
            }

            appendLine("</body>")
            appendLine("</html>")
        }
    }

    /**
     * Generates a JSON report from validation results.
     *
     * @param report The production readiness report
     * @return JSON content as a string
     */
    fun generateJsonReport(report: ProductionReadinessReport): String {
        return json.encodeToString(report)
    }

    /**
     * Generates a Markdown report from validation results.
     *
     * @param report The production readiness report
     * @return Markdown content as a string
     */
    fun generateMarkdownReport(report: ProductionReadinessReport): String {
        return buildString {
            appendLine("# KreeKt Production Readiness Report")
            appendLine()
            appendLine("**Generated:** ${formatTimestamp(report.timestamp.toEpochMilliseconds())}")
            appendLine("**Branch:** `${report.branchName}` **Commit:** `${report.commitHash.take(8)}`")
            appendLine()

            // Executive Summary
            appendLine("## Executive Summary")
            appendLine()
            if (report.isProductionReady) {
                appendLine("✅ **PRODUCTION READY**")
            } else {
                appendLine("❌ **NOT PRODUCTION READY**")
            }
            appendLine()
            appendLine("**Overall Score:** ${formatPercentage(report.overallScore)}")
            appendLine()

            // Constitutional Requirements
            val constitutionalCategory = report.categories.find { it.name == ValidationCategory.CONSTITUTIONAL }
            if (constitutionalCategory != null) {
                appendLine("## Constitutional Requirements")
                appendLine()
                appendLine("| Requirement | Status |")
                appendLine("|------------|--------|")
                constitutionalCategory.criteria.forEach { criterion ->
                    val met = criterion.status == ValidationStatus.PASSED
                    appendLine("| ${criterion.name} | ${if (met) "✅ Met" else "❌ Not Met"} |")
                }
                appendLine()
            }

            // Category Scores
            appendLine("## Validation Category Scores")
            appendLine()
            appendLine("| Category | Score | Status |")
            appendLine("|----------|-------|--------|")
            report.categories.forEach { category ->
                val status = getScoreStatus(category.score)
                val emoji = getStatusEmoji(status)
                appendLine("| ${category.name} | ${formatPercentage(category.score)} | $emoji $status |")
            }
            appendLine()

            // Platform Support
            val platformCriteria = report.categories.flatMap { it.criteria }.filter { it.platform != null }
            if (platformCriteria.isNotEmpty()) {
                appendLine("## Platform Support")
                appendLine()
                appendLine("| Platform | Status | Issues |")
                appendLine("|----------|--------|--------|")
                platformCriteria.groupBy { it.platform }.forEach { (platform, criteria) ->
                    val failedCount = criteria.count { it.status == ValidationStatus.FAILED }
                    val statusEmoji = if (failedCount == 0) "✅" else "❌"
                    val statusText = if (failedCount == 0) "PASSED" else "FAILED"
                    appendLine("| $platform | $statusEmoji $statusText | $failedCount |")
                }
                appendLine()
            }

            // Issues Summary
            val allFailedCriteria = report.categories.flatMap { it.criteria }.filter {
                it.status in listOf(ValidationStatus.FAILED, ValidationStatus.ERROR)
            }
            if (allFailedCriteria.isNotEmpty()) {
                appendLine("## Issues Summary")
                appendLine()

                val issuesBySeverity = allFailedCriteria.groupBy { it.severity }

                Severity.values().forEach { severity ->
                    val severityIssues = issuesBySeverity[severity] ?: emptyList()
                    if (severityIssues.isNotEmpty()) {
                        appendLine("### ${severity.name} Issues (${severityIssues.size})")
                        appendLine()
                        severityIssues.take(10).forEach { criterion ->
                            appendLine("- **${criterion.name}**: ${criterion.description}")
                            appendLine("  - Required: ${criterion.requirement}")
                            appendLine("  - Actual: ${criterion.actual}")
                        }
                        if (severityIssues.size > 10) {
                            appendLine("- ...and ${severityIssues.size - 10} more")
                        }
                        appendLine()
                    }
                }
            }

            // Recommendations
            appendLine("## Recommendations")
            appendLine()
            appendLine("Based on the validation results, here are the top priorities:")
            appendLine()

            val topIssues = allFailedCriteria
                .sortedBy { it.severity.ordinal }
                .take(5)

            if (topIssues.isNotEmpty()) {
                appendLine("1. **Fix Critical Issues**")
                topIssues.forEach { criterion ->
                    appendLine("   - ${criterion.description}")
                }
            }

            if (report.overallScore < 0.8f) {
                appendLine("2. **Improve Overall Score**")
                appendLine("   - Current: ${formatPercentage(report.overallScore)}")
                appendLine("   - Target: 80% or higher")
            }

            val constitutionalCat = report.categories.find { it.name == ValidationCategory.CONSTITUTIONAL }
            val failedRequirements = constitutionalCat?.criteria?.filter {
                it.status != ValidationStatus.PASSED
            } ?: emptyList()

            if (failedRequirements.isNotEmpty()) {
                appendLine("3. **Meet Constitutional Requirements**")
                failedRequirements.forEach { criterion ->
                    appendLine("   - ${criterion.name}")
                }
            }

            appendLine()
            appendLine("---")
            appendLine("*Generated by KreeKt Validation System*")
        }
    }

    /**
     * Generates a summary report suitable for CI/CD logs.
     *
     * @param report The production readiness report
     * @return Console-friendly summary
     */
    fun generateConsoleSummary(report: ProductionReadinessReport): String {
        return buildString {
            appendLine("╔══════════════════════════════════════════════════════════╗")
            appendLine("║         KreeKt Production Readiness Summary             ║")
            appendLine("╚══════════════════════════════════════════════════════════╝")
            appendLine()

            val status = if (report.isProductionReady) "✅ READY" else "❌ NOT READY"
            appendLine("Status: $status")
            appendLine("Score:  ${formatPercentage(report.overallScore)}")
            appendLine()

            val constitutionalCategory2 = report.categories.find { it.name == ValidationCategory.CONSTITUTIONAL }
            if (constitutionalCategory2 != null) {
                appendLine("Constitutional Requirements:")
                constitutionalCategory2.criteria.forEach { criterion ->
                    val symbol = if (criterion.status == ValidationStatus.PASSED) "✓" else "✗"
                    appendLine("  $symbol ${criterion.name}")
                }
                appendLine()
            }

            appendLine("Category Scores:")
            report.categories.forEach { category ->
                val bar = generateProgressBar(category.score, 20)
                appendLine("  ${category.name.padEnd(15)} $bar ${formatPercentage(category.score)}")
            }
            appendLine()

            val allCriteria = report.categories.flatMap { it.criteria }
            val criticalIssues =
                allCriteria.count { it.severity == Severity.CRITICAL && it.status != ValidationStatus.PASSED }
            val highIssues = allCriteria.count { it.severity == Severity.HIGH && it.status != ValidationStatus.PASSED }
            val mediumIssues =
                allCriteria.count { it.severity == Severity.MEDIUM && it.status != ValidationStatus.PASSED }
            val lowIssues = allCriteria.count { it.severity == Severity.LOW && it.status != ValidationStatus.PASSED }

            appendLine("Issues: Critical: $criticalIssues | High: $highIssues | Medium: $mediumIssues | Low: $lowIssues")

            if (!report.isProductionReady) {
                appendLine()
                appendLine("⚠️  Fix the above issues before deploying to production")
            }
        }
    }

    private fun getHtmlStyles(): String = """
        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                line-height: 1.6;
                color: #333;
                background: #f5f5f5;
            }

            .header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 2rem;
                text-align: center;
            }

            .header h1 {
                margin-bottom: 0.5rem;
            }

            .timestamp, .project-path {
                opacity: 0.9;
                font-size: 0.9rem;
            }

            .section {
                max-width: 1200px;
                margin: 2rem auto;
                padding: 2rem;
                background: white;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }

            .section h2 {
                color: #667eea;
                margin-bottom: 1rem;
                padding-bottom: 0.5rem;
                border-bottom: 2px solid #f0f0f0;
            }

            .summary-card {
                padding: 2rem;
                border-radius: 8px;
                text-align: center;
                position: relative;
            }

            .summary-card.ready {
                background: #d4edda;
                border: 1px solid #c3e6cb;
                color: #155724;
            }

            .summary-card.not-ready {
                background: #f8d7da;
                border: 1px solid #f5c6cb;
                color: #721c24;
            }

            .status-text {
                font-size: 2rem;
                font-weight: bold;
                margin-bottom: 1rem;
            }

            .overall-score {
                font-size: 1.2rem;
            }

            table {
                width: 100%;
                border-collapse: collapse;
            }

            th, td {
                padding: 0.75rem;
                text-align: left;
                border-bottom: 1px solid #ddd;
            }

            th {
                background: #f8f9fa;
                font-weight: 600;
            }

            tr.passed, tr.excellent {
                background: #d4edda;
            }

            tr.failed, tr.poor {
                background: #f8d7da;
            }

            tr.good {
                background: #d1ecf1;
            }

            tr.warning {
                background: #fff3cd;
            }

            .platform-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 1rem;
                margin-top: 1rem;
            }

            .platform-card {
                padding: 1rem;
                border-radius: 4px;
                border: 1px solid #ddd;
            }

            .platform-card.passed {
                background: #d4edda;
                border-color: #c3e6cb;
            }

            .platform-card.failed {
                background: #f8d7da;
                border-color: #f5c6cb;
            }

            .platform-card h3 {
                margin-bottom: 0.5rem;
            }

            .severity-group {
                margin-bottom: 1.5rem;
            }

            .severity-group.critical h3 {
                color: #dc3545;
            }

            .severity-group.high h3 {
                color: #fd7e14;
            }

            .severity-group.medium h3 {
                color: #ffc107;
            }

            .severity-group.low h3 {
                color: #6c757d;
            }

            .issue {
                padding: 1rem;
                margin-bottom: 0.5rem;
                background: #f8f9fa;
                border-left: 3px solid #667eea;
                border-radius: 4px;
            }

            .issue-header {
                display: flex;
                gap: 1rem;
                margin-bottom: 0.5rem;
            }

            .issue-category, .issue-type {
                display: inline-block;
                padding: 0.25rem 0.5rem;
                background: #667eea;
                color: white;
                border-radius: 4px;
                font-size: 0.85rem;
            }

            .issue-description {
                margin-bottom: 0.5rem;
            }

            .issue-location {
                font-family: monospace;
                color: #666;
                font-size: 0.9rem;
            }

            .issue-remediation {
                margin-top: 0.5rem;
                padding-top: 0.5rem;
                border-top: 1px dashed #ddd;
                color: #28a745;
            }

            .metadata-list dt {
                float: left;
                width: 200px;
                font-weight: 600;
                clear: left;
            }

            .metadata-list dd {
                margin-left: 220px;
                margin-bottom: 0.5rem;
            }

            .footer {
                text-align: center;
                padding: 2rem;
                color: #666;
                font-size: 0.9rem;
            }
        </style>
    """

    private fun getChartScripts(): String = """
        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    """

    private fun generateChartScript(report: ProductionReadinessReport): String = """
        <script>
            const ctx = document.getElementById('scoresChart');
            if (ctx) {
                new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: [${report.categories.joinToString { "'${it.name}'" }}],
                        datasets: [{
                            label: 'Score (%)',
                            data: [${report.categories.joinToString { (it.score * 100).roundToInt().toString() }}],
                            backgroundColor: [
                                'rgba(102, 126, 234, 0.8)',
                                'rgba(118, 75, 162, 0.8)',
                                'rgba(40, 167, 69, 0.8)',
                                'rgba(255, 193, 7, 0.8)',
                                'rgba(220, 53, 69, 0.8)',
                                'rgba(23, 162, 184, 0.8)'
                            ],
                            borderColor: [
                                'rgba(102, 126, 234, 1)',
                                'rgba(118, 75, 162, 1)',
                                'rgba(40, 167, 69, 1)',
                                'rgba(255, 193, 7, 1)',
                                'rgba(220, 53, 69, 1)',
                                'rgba(23, 162, 184, 1)'
                            ],
                            borderWidth: 1
                        }]
                    },
                    options: {
                        scales: {
                            y: {
                                beginAtZero: true,
                                max: 100
                            }
                        },
                        plugins: {
                            legend: {
                                display: false
                            }
                        }
                    }
                });
            }
        </script>
    """

    private fun formatTimestamp(timestamp: Long): String {
        // Simple formatting - in real implementation would use proper date formatting
        return "Timestamp: $timestamp"
    }

    private fun formatPercentage(score: Float): String {
        return "${(score * 100).roundToInt()}%"
    }

    private fun getScoreStatus(score: Float): String {
        return when {
            score >= 0.9f -> "excellent"
            score >= 0.7f -> "good"
            score >= 0.5f -> "warning"
            else -> "poor"
        }
    }

    private fun getStatusEmoji(status: String): String {
        return when (status) {
            "excellent" -> "🟢"
            "good" -> "🔵"
            "warning" -> "🟡"
            "poor" -> "🔴"
            else -> "⚪"
        }
    }

    private fun formatMetadataKey(key: String): String {
        // Split camelCase to separate words
        return key.replace(Regex("([A-Z])"), " $1")
            .trim()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }
    }

    private fun generateProgressBar(value: Float, width: Int): String {
        val filled = (value * width).roundToInt()
        val empty = width - filled
        return "[${"█".repeat(filled)}${"░".repeat(empty)}]"
    }
}