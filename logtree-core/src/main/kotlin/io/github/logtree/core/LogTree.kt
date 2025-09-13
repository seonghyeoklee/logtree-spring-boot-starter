package io.github.logtree.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Main LogTree API for hierarchical logging
 */
object LogTree {
    @PublishedApi
    internal val logger: Logger = LoggerFactory.getLogger(LogTree::class.java)
    
    /**
     * Start a new trace and execute the given block
     */
    inline fun <T> trace(
        name: String? = null,
        traceId: String? = null,
        crossinline block: () -> T
    ): T {
        val actualTraceId = traceId ?: LogTreeContext.generateTraceId()
        LogTreeContext.startTrace(actualTraceId)
        
        return try {
            if (name != null) {
                span(name) { block() }
            } else {
                block()
            }
        } finally {
            LogTreeContext.endTrace()
        }
    }
    
    /**
     * Create a new span within the current trace
     */
    inline fun <T> span(
        name: String,
        crossinline block: () -> T
    ): T {
        val spanId = LogTreeContext.enterSpan(name)
        val startTime = System.currentTimeMillis()
        
        logger.info("$name started")
        
        return try {
            val result = block()
            val duration = System.currentTimeMillis() - startTime
            logger.info("$name completed (${duration}ms)")
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("$name failed (${duration}ms): ${e.message}")
            throw e
        } finally {
            LogTreeContext.exitSpan()
        }
    }
    
    /**
     * Log an info message within the current trace context
     */
    fun info(message: String, vararg args: Any?) {
        logger.info(message, *args)
    }
    
    /**
     * Log a debug message within the current trace context
     */
    fun debug(message: String, vararg args: Any?) {
        logger.debug(message, *args)
    }
    
    /**
     * Log a warning message within the current trace context
     */
    fun warn(message: String, vararg args: Any?) {
        logger.warn(message, *args)
    }
    
    /**
     * Log an error message within the current trace context
     */
    fun error(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            logger.error(message, throwable)
        } else {
            logger.error(message)
        }
    }
}

/**
 * Extension function for convenient span creation
 */
inline fun <T> T.logSpan(
    name: String,
    crossinline block: T.() -> Unit
): T {
    LogTree.span(name) {
        block()
    }
    return this
}