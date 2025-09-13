package io.github.logtree.spring.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.github.logtree.core.LogTree
import io.github.logtree.core.LogTreeContext
import org.slf4j.MDC

/**
 * Custom Logback appender that intercepts Hibernate SQL logs
 * and formats them for LogTree hierarchical logging
 */
class LogTreeSqlAppender : AppenderBase<ILoggingEvent>() {
    
    var maxQueryLength: Int = 500
    var enabled: Boolean = true
    
    companion object {
        private val SQL_LOGGERS = setOf(
            "org.hibernate.SQL",
            "org.hibernate.type.descriptor.sql.BasicBinder",
            "org.hibernate.type.descriptor.sql.BasicExtractor"
        )
    }
    
    override fun append(event: ILoggingEvent) {
        if (!enabled) {
            println("[LogTreeSqlAppender] Disabled")
            return
        }
        
        println("[LogTreeSqlAppender] Processing log from: ${event.loggerName}")
        
        // Only process SQL-related logs
        if (!SQL_LOGGERS.contains(event.loggerName)) {
            println("[LogTreeSqlAppender] Not a SQL logger: ${event.loggerName}")
            return
        }
        
        // Only process if we're within a LogTree trace
        val currentContext = LogTreeContext.currentContext()
        if (currentContext == null) {
            println("[LogTreeSqlAppender] No LogTree context")
            return
        }
        
        val message = event.formattedMessage
        if (message.isNullOrBlank()) {
            println("[LogTreeSqlAppender] Empty message")
            return
        }
        
        println("[LogTreeSqlAppender] Processing message: $message")
        
        when (event.loggerName) {
            "org.hibernate.SQL" -> {
                formatAndLogSqlQuery(message)
            }
            "org.hibernate.type.descriptor.sql.BasicBinder" -> {
                formatAndLogParameter(message)
            }
            "org.hibernate.type.descriptor.sql.BasicExtractor" -> {
                formatAndLogExtraction(message)
            }
        }
    }
    
    private fun formatAndLogSqlQuery(sql: String) {
        val cleanSql = sql.trim()
        if (cleanSql.isEmpty()) return
        
        val truncatedSql = if (cleanSql.length > maxQueryLength) {
            "${cleanSql.substring(0, maxQueryLength)}..."
        } else {
            cleanSql
        }
        
        // Format SQL for better readability within LogTree hierarchy
        val formattedSql = formatSqlForDisplay(truncatedSql)
        
        LogTree.info("SQL: $formattedSql")
    }
    
    private fun formatAndLogParameter(message: String) {
        // Extract parameter binding information
        if (message.contains("binding parameter")) {
            val paramInfo = extractParameterInfo(message)
            if (paramInfo.isNotEmpty()) {
                LogTree.debug("   ├─ Param: $paramInfo")
            }
        }
    }
    
    private fun formatAndLogExtraction(message: String) {
        // Extract result extraction information
        if (message.contains("extracted value")) {
            val extractInfo = extractResultInfo(message)
            if (extractInfo.isNotEmpty()) {
                LogTree.debug("   └─ Result: $extractInfo")
            }
        }
    }
    
    private fun formatSqlForDisplay(sql: String): String {
        return sql
            .replace(Regex("\\s+"), " ")  // Normalize whitespace
            .replace(" SELECT ", " SELECT\n       ")
            .replace(" FROM ", "\n       FROM ")
            .replace(" WHERE ", "\n       WHERE ")
            .replace(" JOIN ", "\n       JOIN ")
            .replace(" LEFT JOIN ", "\n       LEFT JOIN ")
            .replace(" RIGHT JOIN ", "\n       RIGHT JOIN ")
            .replace(" INNER JOIN ", "\n       INNER JOIN ")
            .replace(" ORDER BY ", "\n       ORDER BY ")
            .replace(" GROUP BY ", "\n       GROUP BY ")
            .replace(" HAVING ", "\n       HAVING ")
            .replace(" INSERT INTO ", " INSERT INTO\n       ")
            .replace(" VALUES ", "\n       VALUES ")
            .replace(" UPDATE ", " UPDATE\n       ")
            .replace(" SET ", "\n       SET ")
            .replace(" DELETE FROM ", " DELETE FROM\n       ")
    }
    
    private fun extractParameterInfo(message: String): String {
        // Parse messages like "binding parameter [1] as [VARCHAR] - [testValue]"
        val regex = Regex("binding parameter \\[(.+?)\\] as \\[(.+?)\\] - \\[(.+?)\\]")
        val matchResult = regex.find(message)
        
        return if (matchResult != null) {
            val (index, type, value) = matchResult.destructured
            "$index=$value ($type)"
        } else {
            ""
        }
    }
    
    private fun extractResultInfo(message: String): String {
        // Parse result extraction messages
        val regex = Regex("extracted value \\[(.+?)\\] : \\[(.+?)\\]")
        val matchResult = regex.find(message)
        
        return if (matchResult != null) {
            val (column, value) = matchResult.destructured
            "$column=$value"
        } else {
            ""
        }
    }
}