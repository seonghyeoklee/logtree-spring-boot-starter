package io.github.logtree.spring.config

import io.github.logtree.spring.aspect.LogTreeAspect
import io.github.logtree.spring.aspect.LogTreeAutoTraceAspect
import io.github.logtree.spring.filter.LogTreeFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

/**
 * Auto-configuration for LogTree
 */
@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(LogTreeProperties::class)
@ConditionalOnProperty(
    prefix = "logtree",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class LogTreeAutoConfiguration {
    
    @Bean
    @ConditionalOnClass(name = ["org.aspectj.lang.ProceedingJoinPoint"])
    fun logTreeAspect(): LogTreeAspect {
        return LogTreeAspect()
    }
    
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    fun logTreeFilter(properties: LogTreeProperties): LogTreeFilter {
        return LogTreeFilter(properties)
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "logtree", name = ["autoTraceControllers"], havingValue = "true", matchIfMissing = true)
    fun logTreeAutoTraceAspect(properties: LogTreeProperties): LogTreeAutoTraceAspect {
        return LogTreeAutoTraceAspect(properties)
    }
}