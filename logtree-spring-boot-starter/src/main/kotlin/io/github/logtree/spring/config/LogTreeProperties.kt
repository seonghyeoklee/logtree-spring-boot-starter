package io.github.logtree.spring.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for LogTree
 */
@ConfigurationProperties(prefix = "logtree")
data class LogTreeProperties(
    /**
     * Whether LogTree is enabled
     */
    var enabled: Boolean = true,
    
    /**
     * Whether to auto-trace all controllers
     */
    var autoTraceControllers: Boolean = true,
    
    /**
     * Whether to auto-trace all services
     */
    var autoTraceServices: Boolean = false,
    
    /**
     * Whether to auto-trace all repositories
     */
    var autoTraceRepositories: Boolean = false,
    
    /**
     * Whether to include request headers in trace
     */
    var includeHeaders: Boolean = false,
    
    /**
     * Whether to include method arguments in trace
     */
    var includeArgs: Boolean = false,
    
    /**
     * Whether to include method return values in trace
     */
    var includeResult: Boolean = false,
    
    /**
     * Whether to track errors with causality chains
     */
    var trackErrors: Boolean = true,
    
    /**
     * Whether to trace transaction boundaries with visual markers
     */
    var traceTransactionBoundaries: Boolean = true,
    
    /**
     * Whether to trace SQL queries within LogTree spans (replaces p6spy functionality)
     */
    var traceSqlQueries: Boolean = false,
    
    /**
     * Maximum length for SQL query logging (longer queries will be truncated)
     */
    var sqlQueryMaxLength: Int = 500,
    
    /**
     * Headers to exclude from tracing
     */
    var excludeHeaders: List<String> = listOf("Authorization", "Cookie"),
    
    /**
     * URL patterns to exclude from tracing
     */
    var excludeUrls: List<String> = listOf("/health", "/actuator/**"),
    
    /**
     * Package patterns to exclude from tracing
     */
    var excludePackages: List<String> = listOf(),
    
    /**
     * Maximum depth for nested spans
     */
    var maxDepth: Int = 10,
    
    /**
     * Whether to use colored console output
     */
    var coloredOutput: Boolean = true,
    
    /**
     * Log pattern style: "tree", "flat", "json"
     */
    var logStyle: String = "tree"
)