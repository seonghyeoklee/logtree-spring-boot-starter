package io.github.logtree.spring.admin

import io.github.logtree.spring.config.LogTreeProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.*

/**
 * REST controller for LogTree administration
 */
@RestController
@RequestMapping("/admin/logtree")
@ConditionalOnProperty(
    prefix = "logtree.admin",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class LogTreeAdminController(
    private val properties: LogTreeProperties,
    private val metricsCollector: LogTreeMetricsCollector
) {

    /**
     * Get current LogTree configuration
     */
    @GetMapping("/api/config")
    fun getConfig(): Map<String, Any> {
        return mapOf(
            "enabled" to properties.enabled,
            "autoTraceControllers" to properties.autoTraceControllers,
            "autoTraceServices" to properties.autoTraceServices,
            "autoTraceRepositories" to properties.autoTraceRepositories,
            "includeHeaders" to properties.includeHeaders,
            "includeArgs" to properties.includeArgs,
            "includeResult" to properties.includeResult,
            "trackErrors" to properties.trackErrors,
            "traceTransactionBoundaries" to properties.traceTransactionBoundaries,
            "traceSqlQueries" to properties.traceSqlQueries,
            "sqlQueryMaxLength" to properties.sqlQueryMaxLength,
            "excludeHeaders" to properties.excludeHeaders,
            "excludeUrls" to properties.excludeUrls,
            "excludePackages" to properties.excludePackages,
            "maxDepth" to properties.maxDepth,
            "coloredOutput" to properties.coloredOutput,
            "logStyle" to properties.logStyle
        )
    }

    /**
     * Update LogTree configuration at runtime
     */
    @PutMapping("/api/config")
    fun updateConfig(@RequestBody updates: Map<String, Any>): Map<String, Any> {
        updates.forEach { (key, value) ->
            when (key) {
                "autoTraceControllers" -> properties.autoTraceControllers = value as Boolean
                "autoTraceServices" -> properties.autoTraceServices = value as Boolean
                "autoTraceRepositories" -> properties.autoTraceRepositories = value as Boolean
                "includeHeaders" -> properties.includeHeaders = value as Boolean
                "includeArgs" -> properties.includeArgs = value as Boolean
                "includeResult" -> properties.includeResult = value as Boolean
                "trackErrors" -> properties.trackErrors = value as Boolean
                "traceTransactionBoundaries" -> properties.traceTransactionBoundaries = value as Boolean
                "traceSqlQueries" -> properties.traceSqlQueries = value as Boolean
                "coloredOutput" -> properties.coloredOutput = value as Boolean
                "maxDepth" -> properties.maxDepth = (value as Number).toInt()
                "sqlQueryMaxLength" -> properties.sqlQueryMaxLength = (value as Number).toInt()
                "logStyle" -> properties.logStyle = value as String
            }
        }

        return mapOf("status" to "updated", "config" to getConfig())
    }

    /**
     * Get collected metrics and statistics
     */
    @GetMapping("/api/metrics")
    fun getMetrics(): Map<String, Any> {
        return metricsCollector.getMetrics()
    }

    /**
     * Get recent traces
     */
    @GetMapping("/api/traces/recent")
    fun getRecentTraces(@RequestParam(defaultValue = "20") limit: Int): List<TraceInfo> {
        return metricsCollector.getRecentTraces(limit)
    }

    /**
     * Get trace details by ID
     */
    @GetMapping("/api/traces/{traceId}")
    fun getTraceDetails(@PathVariable traceId: String): TraceInfo? {
        return metricsCollector.getTrace(traceId)
    }

    /**
     * Clear collected metrics
     */
    @DeleteMapping("/api/metrics")
    fun clearMetrics(): Map<String, String> {
        metricsCollector.clear()
        return mapOf("status" to "cleared")
    }
}
