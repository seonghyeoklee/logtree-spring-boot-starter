package io.github.logtree.core

import org.slf4j.MDC
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Core context management for hierarchical tracing
 */
class LogTreeContext {
    companion object {
        private const val TRACE_ID_KEY = "traceId"
        private const val SPAN_ID_KEY = "spanId"
        private const val PARENT_SPAN_ID_KEY = "parentSpanId"
        private const val DEPTH_KEY = "depth"
        private const val PATH_KEY = "path"
        private const val VISUAL_KEY = "visual"
        
        private val contextHolder = ThreadLocal<TraceContext>()
        private val spanStack = ThreadLocal.withInitial { Stack<SpanContext>() }
        
        /**
         * Start a new trace
         */
        fun startTrace(traceId: String = generateTraceId()): String {
            val context = TraceContext(
                traceId = traceId,
                startTime = System.currentTimeMillis(),
                depth = 0
            )
            contextHolder.set(context)
            updateMDC(context)
            return traceId
        }
        
        /**
         * Enter a new span within the current trace
         */
        fun enterSpan(name: String): String {
            val currentContext = contextHolder.get() ?: run {
                startTrace()
                contextHolder.get()!!
            }
            
            val parentSpanId = if (spanStack.get().isEmpty()) null else spanStack.get().peek().spanId
            val spanId = generateSpanId()
            val depth = spanStack.get().size + 1
            
            val spanContext = SpanContext(
                spanId = spanId,
                parentSpanId = parentSpanId,
                name = name,
                depth = depth,
                startTime = System.currentTimeMillis()
            )
            
            spanStack.get().push(spanContext)
            currentContext.depth = depth
            updateMDC(currentContext, spanContext)
            
            return spanId
        }
        
        /**
         * Exit the current span
         */
        fun exitSpan(): SpanContext? {
            if (spanStack.get().isEmpty()) return null
            
            val spanContext = spanStack.get().pop()
            spanContext.endTime = System.currentTimeMillis()
            spanContext.duration = spanContext.endTime - spanContext.startTime
            
            val currentContext = contextHolder.get()
            if (currentContext != null) {
                currentContext.depth = spanStack.get().size
                if (spanStack.get().isEmpty()) {
                    updateMDC(currentContext)
                } else {
                    updateMDC(currentContext, spanStack.get().peek())
                }
            }
            
            return spanContext
        }
        
        /**
         * End the current trace
         */
        fun endTrace() {
            val context = contextHolder.get()
            if (context != null) {
                context.endTime = System.currentTimeMillis()
                context.duration = context.endTime - context.startTime
            }
            
            clearContext()
        }
        
        /**
         * Get current trace context
         */
        fun currentContext(): TraceContext? = contextHolder.get()
        
        /**
         * Get current span context
         */
        fun currentSpan(): SpanContext? = 
            if (spanStack.get().isEmpty()) null else spanStack.get().peek()
        
        /**
         * Clear all context
         */
        fun clearContext() {
            contextHolder.remove()
            spanStack.get().clear()
            MDC.clear()
        }
        
        private fun updateMDC(traceContext: TraceContext, spanContext: SpanContext? = null) {
            MDC.put(TRACE_ID_KEY, traceContext.traceId)
            MDC.put(DEPTH_KEY, traceContext.depth.toString())
            MDC.put(VISUAL_KEY, createVisual(traceContext.depth))
            
            if (spanContext != null) {
                MDC.put(SPAN_ID_KEY, spanContext.spanId)
                spanContext.parentSpanId?.let { MDC.put(PARENT_SPAN_ID_KEY, it) }
                MDC.put(PATH_KEY, getSpanPath())
            } else {
                MDC.remove(SPAN_ID_KEY)
                MDC.remove(PARENT_SPAN_ID_KEY)
                MDC.remove(PATH_KEY)
            }
        }
        
        private fun createVisual(depth: Int): String {
            return when (depth) {
                0 -> ""  // Root level has no visual prefix
                1 -> "├─"
                else -> {
                    val indent = "│ ".repeat(depth - 1)
                    "$indent├─"
                }
            }
        }
        
        private fun getSpanPath(): String {
            return spanStack.get().map { it.name }.joinToString(" → ")
        }
        
        fun generateTraceId(): String = UUID.randomUUID().toString()
        fun generateSpanId(): String = UUID.randomUUID().toString().substring(0, 8)
    }
}

/**
 * Trace context data
 */
data class TraceContext(
    val traceId: String,
    val startTime: Long,
    var depth: Int = 0,
    var endTime: Long = 0,
    var duration: Long = 0,
    val metadata: MutableMap<String, Any> = ConcurrentHashMap()
)

/**
 * Span context data
 */
data class SpanContext(
    val spanId: String,
    val parentSpanId: String?,
    val name: String,
    val depth: Int,
    val startTime: Long,
    var endTime: Long = 0,
    var duration: Long = 0,
    val tags: MutableMap<String, String> = ConcurrentHashMap()
)