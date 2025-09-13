package io.github.logtree.spring.config

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import io.github.logtree.spring.logging.LogTreeSqlAppender
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import jakarta.annotation.PostConstruct

/**
 * Auto-configuration for LogTree SQL logging integration
 */
@AutoConfiguration
@EnableConfigurationProperties(LogTreeProperties::class)
@ConditionalOnProperty(prefix = "logtree", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class LogTreeSqlLoggingAutoConfiguration(
    private val properties: LogTreeProperties
) {
    
    @Bean
    @ConditionalOnProperty(prefix = "logtree", name = ["trace-sql-queries"], havingValue = "true")
    fun logTreeSqlAppender(): LogTreeSqlAppender {
        val appender = LogTreeSqlAppender()
        appender.name = "LogTreeSqlAppender"
        appender.maxQueryLength = properties.sqlQueryMaxLength
        appender.enabled = properties.traceSqlQueries
        
        // Get the logback context and configure the appender
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        appender.context = loggerContext
        appender.start()
        
        return appender
    }
    
    @PostConstruct
    fun configureSqlLogging() {
        if (!properties.traceSqlQueries) {
            println("LogTree SQL logging is disabled")
            return
        }
        
        println("Configuring LogTree SQL logging...")
        
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val sqlAppender = LogTreeSqlAppender()
        sqlAppender.name = "LogTreeSqlAppender"
        sqlAppender.maxQueryLength = properties.sqlQueryMaxLength
        sqlAppender.enabled = properties.traceSqlQueries
        sqlAppender.context = loggerContext
        sqlAppender.start()
        
        println("LogTree SQL appender started")
        
        // Add the appender to SQL loggers
        val sqlLoggers = listOf(
            "org.hibernate.SQL",
            "org.hibernate.type.descriptor.sql.BasicBinder",
            "org.hibernate.type.descriptor.sql.BasicExtractor"
        )
        
        sqlLoggers.forEach { loggerName ->
            val logger = loggerContext.getLogger(loggerName) as Logger
            logger.addAppender(sqlAppender)
            // Set level to DEBUG to capture SQL logs
            logger.level = ch.qos.logback.classic.Level.DEBUG
            // Keep additive true to also see normal hibernate logs
            logger.isAdditive = true
            println("Added LogTree SQL appender to logger: $loggerName")
        }
        
        println("LogTree SQL logging configuration completed")
    }
}