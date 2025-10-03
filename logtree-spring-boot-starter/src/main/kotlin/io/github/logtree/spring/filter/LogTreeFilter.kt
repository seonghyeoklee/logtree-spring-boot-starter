package io.github.logtree.spring.filter

import io.github.logtree.core.LogTreeContext
import io.github.logtree.spring.admin.LogTreeMetricsCollector
import io.github.logtree.spring.config.LogTreeProperties
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.util.AntPathMatcher

/**
 * Servlet filter for automatic HTTP request tracing
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class LogTreeFilter(
    private val properties: LogTreeProperties
) : Filter {

    @Autowired(required = false)
    private var metricsCollector: LogTreeMetricsCollector? = null
    
    private val logger = LoggerFactory.getLogger(LogTreeFilter::class.java)
    private val pathMatcher = AntPathMatcher()
    
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        if (request !is HttpServletRequest || response !is HttpServletResponse) {
            chain.doFilter(request, response)
            return
        }
        
        // Check if URL should be excluded
        if (shouldExclude(request.requestURI)) {
            chain.doFilter(request, response)
            return
        }
        
        val traceId = request.getHeader("X-Trace-Id") 
            ?: request.getHeader("X-Request-Id")
            ?: LogTreeContext.generateTraceId()
        
        LogTreeContext.startTrace(traceId)
        val startTime = System.currentTimeMillis()
        var error: Throwable? = null

        try {
            logger.info("HTTP ${request.method} ${request.requestURI}")

            if (properties.includeHeaders) {
                logHeaders(request)
            }

            chain.doFilter(request, response)

            val duration = System.currentTimeMillis() - startTime
            logger.info("HTTP ${response.status} (${duration}ms)")

            // Add trace ID to response header
            response.setHeader("X-Trace-Id", traceId)

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("HTTP ${request.method} ${request.requestURI} failed (${duration}ms)", e)
            error = e
            throw e
        } finally {
            // Collect metrics before clearing context
            val traceContext = LogTreeContext.currentContext()
            if (traceContext != null && metricsCollector != null) {
                val spans = LogTreeContext.getAllSpans()
                metricsCollector?.recordTrace(traceContext, spans, error)
            }

            LogTreeContext.clearContext()
        }
    }
    
    private fun shouldExclude(uri: String): Boolean {
        return properties.excludeUrls.any { pattern ->
            pathMatcher.match(pattern, uri)
        }
    }
    
    private fun logHeaders(request: HttpServletRequest) {
        val headers = request.headerNames.toList()
            .filter { !properties.excludeHeaders.contains(it) }
            .associate { it to request.getHeader(it) }
        
        if (headers.isNotEmpty()) {
            logger.debug("Request headers: $headers")
        }
    }
}