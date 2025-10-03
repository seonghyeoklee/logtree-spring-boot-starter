package io.github.logtree.spring.admin

import io.github.logtree.core.SpanContext
import io.github.logtree.core.TraceContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong

/**
 * Collects and stores LogTree metrics and trace information
 */
@Component
class LogTreeMetricsCollector {

    private val recentTraces = ConcurrentLinkedDeque<TraceInfo>()
    private val traceMap = ConcurrentHashMap<String, TraceInfo>()
    private val maxTraces = 100

    // Metrics
    private val totalTraces = AtomicLong(0)
    private val totalSpans = AtomicLong(0)
    private val totalErrors = AtomicLong(0)
    private val totalDuration = AtomicLong(0)

    /**
     * Record a completed trace
     */
    fun recordTrace(traceContext: TraceContext, spans: List<SpanContext>, error: Throwable? = null) {
        val traceInfo = TraceInfo(
            traceId = traceContext.traceId,
            startTime = traceContext.startTime,
            endTime = traceContext.endTime,
            duration = traceContext.duration,
            spanCount = spans.size,
            error = error?.let { ErrorInfo(it.javaClass.simpleName, it.message) },
            spans = spans.map { span ->
                SpanInfo(
                    spanId = span.spanId,
                    parentSpanId = span.parentSpanId,
                    name = span.name,
                    depth = span.depth,
                    startTime = span.startTime,
                    endTime = span.endTime,
                    duration = span.duration,
                    tags = span.tags
                )
            }
        )

        // Add to recent traces (limit to maxTraces)
        recentTraces.addFirst(traceInfo)
        while (recentTraces.size > maxTraces) {
            val removed = recentTraces.removeLast()
            traceMap.remove(removed.traceId)
        }

        traceMap[traceInfo.traceId] = traceInfo

        // Update metrics
        totalTraces.incrementAndGet()
        totalSpans.addAndGet(spans.size.toLong())
        totalDuration.addAndGet(traceContext.duration)
        if (error != null) {
            totalErrors.incrementAndGet()
        }
    }

    /**
     * Get recent traces
     */
    fun getRecentTraces(limit: Int = 20): List<TraceInfo> {
        return recentTraces.take(limit.coerceAtMost(maxTraces))
    }

    /**
     * Get trace by ID
     */
    fun getTrace(traceId: String): TraceInfo? {
        return traceMap[traceId]
    }

    /**
     * Get collected metrics
     */
    fun getMetrics(): Map<String, Any> {
        val traces = totalTraces.get()
        val avgDuration = if (traces > 0) totalDuration.get() / traces else 0
        val errorRate = if (traces > 0) (totalErrors.get().toDouble() / traces * 100) else 0.0

        return mapOf(
            "totalTraces" to traces,
            "totalSpans" to totalSpans.get(),
            "totalErrors" to totalErrors.get(),
            "errorRate" to String.format("%.2f", errorRate),
            "averageDuration" to avgDuration,
            "recentTracesCount" to recentTraces.size
        )
    }

    /**
     * Clear all collected data
     */
    fun clear() {
        recentTraces.clear()
        traceMap.clear()
        totalTraces.set(0)
        totalSpans.set(0)
        totalErrors.set(0)
        totalDuration.set(0)
    }
}

/**
 * Trace information data class
 */
data class TraceInfo(
    val traceId: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val spanCount: Int,
    val error: ErrorInfo? = null,
    val spans: List<SpanInfo> = emptyList()
)

/**
 * Span information data class
 */
data class SpanInfo(
    val spanId: String,
    val parentSpanId: String?,
    val name: String,
    val depth: Int,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val tags: Map<String, String> = emptyMap()
)

/**
 * Error information data class
 */
data class ErrorInfo(
    val type: String,
    val message: String?
)
