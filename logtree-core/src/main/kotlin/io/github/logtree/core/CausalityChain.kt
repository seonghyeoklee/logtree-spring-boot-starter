package io.github.logtree.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Causality Chain for tracking error cause relationships
 */
class CausalityChain {
    private val chain = ConcurrentLinkedDeque<CausalityNode>()
    private val logger: Logger = LoggerFactory.getLogger(CausalityChain::class.java)
    
    /**
     * Add a cause to the chain
     */
    fun addCause(
        event: String,
        causedBy: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): CausalityChain {
        val node = CausalityNode(
            event = event,
            causedBy = causedBy,
            timestamp = System.currentTimeMillis(),
            depth = chain.size,
            metadata = metadata
        )
        chain.add(node)
        return this
    }
    
    /**
     * Add an exception as a cause
     */
    fun addException(
        exception: Throwable,
        context: String? = null
    ): CausalityChain {
        val event = context ?: exception.message ?: exception.javaClass.simpleName
        val causedBy = exception.cause?.let { 
            "${it.javaClass.simpleName}: ${it.message}"
        }
        
        val metadata = mutableMapOf<String, Any>(
            "exceptionType" to exception.javaClass.name,
            "stackTrace" to exception.stackTrace.take(5).map { it.toString() }
        )
        
        addCause(event, causedBy, metadata)
        
        // Recursively add causes
        var currentCause = exception.cause
        while (currentCause != null) {
            addCause(
                event = "${currentCause.javaClass.simpleName}: ${currentCause.message}",
                causedBy = currentCause.cause?.message,
                metadata = mapOf("exceptionType" to currentCause.javaClass.name)
            )
            currentCause = currentCause.cause
        }
        
        return this
    }
    
    /**
     * Log the entire causality chain
     */
    fun log(level: LogLevel = LogLevel.ERROR) {
        if (chain.isEmpty()) return
        
        val visual = buildVisualChain()
        when (level) {
            LogLevel.INFO -> logger.info(visual)
            LogLevel.WARN -> logger.warn(visual)
            LogLevel.ERROR -> logger.error(visual)
        }
    }
    
    /**
     * Build visual representation of the chain
     */
    fun buildVisualChain(): String {
        if (chain.isEmpty()) return "No causality chain"
        
        val sb = StringBuilder()
        sb.appendLine("Causality Chain:")
        
        chain.forEachIndexed { index, node ->
            val prefix = when (index) {
                0 -> "┌─"
                chain.size - 1 -> "└─"
                else -> "├─"
            }
            
            sb.appendLine("$prefix ${node.event}")
            
            if (node.causedBy != null) {
                val causePrefix = if (index == chain.size - 1) "  " else "│ "
                sb.appendLine("$causePrefix └─ caused by: ${node.causedBy}")
            }
            
            if (node.metadata.isNotEmpty()) {
                val metaPrefix = if (index == chain.size - 1) "  " else "│ "
                node.metadata.forEach { (key, value) ->
                    if (key != "stackTrace") {
                        sb.appendLine("$metaPrefix   • $key: $value")
                    }
                }
            }
        }
        
        return sb.toString()
    }
    
    /**
     * Get the root cause
     */
    fun getRootCause(): CausalityNode? = chain.lastOrNull()
    
    /**
     * Get the immediate cause
     */
    fun getImmediateCause(): CausalityNode? = chain.firstOrNull()
    
    /**
     * Clear the chain
     */
    fun clear() {
        chain.clear()
    }
    
    /**
     * Convert to list for serialization
     */
    fun toList(): List<CausalityNode> = chain.toList()
    
    companion object {
        private val chainHolder = ThreadLocal<CausalityChain>()
        
        /**
         * Get or create current chain
         */
        fun current(): CausalityChain {
            return chainHolder.get() ?: CausalityChain().also {
                chainHolder.set(it)
            }
        }
        
        /**
         * Clear current chain
         */
        fun clear() {
            chainHolder.remove()
        }
    }
}

/**
 * Node in the causality chain
 */
data class CausalityNode(
    val event: String,
    val causedBy: String? = null,
    val timestamp: Long,
    val depth: Int,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Log level for causality chain output
 */
enum class LogLevel {
    INFO, WARN, ERROR
}