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
     * Whether to include request headers in trace
     */
    var includeHeaders: Boolean = false,
    
    /**
     * Headers to exclude from tracing
     */
    var excludeHeaders: List<String> = listOf("Authorization", "Cookie"),
    
    /**
     * URL patterns to exclude from tracing
     */
    var excludeUrls: List<String> = listOf("/health", "/actuator/**"),
    
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