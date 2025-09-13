package io.github.logtree.spring.aspect

import io.github.logtree.core.LogTree
import io.github.logtree.spring.config.LogTreeProperties
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component

/**
 * Separate AOP Aspect for JPA repository tracing
 * Only loaded when JPA is available in the classpath
 */
@Aspect
@Component
@ConditionalOnClass(name = ["org.springframework.data.jpa.repository.JpaRepository"])
class LogTreeJpaAutoTraceAspect(
    private val properties: LogTreeProperties
) {
    private val logger = LoggerFactory.getLogger(LogTreeJpaAutoTraceAspect::class.java)
    
    /**
     * Auto-trace JPA repository methods
     */
    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))")
    fun traceJpaRepositories(joinPoint: ProceedingJoinPoint): Any? {
        // Use autoTraceServices as fallback for repository tracing
        if (!properties.autoTraceServices) {
            return joinPoint.proceed()
        }
        
        return executeWithTrace(joinPoint, "JpaRepository")
    }
    
    private fun executeWithTrace(joinPoint: ProceedingJoinPoint, category: String): Any? {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        val className = joinPoint.target.javaClass.simpleName
        val methodName = method.name
        
        val spanName = "[$category] $className.$methodName"
        
        return try {
            LogTree.span(spanName) {
                val result = joinPoint.proceed()
                
                // Log method execution info for debugging
                if (logger.isDebugEnabled) {
                    val methodName = method.name
                    val paramTypes = method.parameterTypes.joinToString(", ") { it.simpleName }
                    val returnType = if (result != null) result.javaClass.simpleName else "void"
                    LogTree.debug("$methodName($paramTypes) → $returnType")
                }
                
                result
            }
        } catch (e: Exception) {
            LogTree.error("$category method failed: ${e.message}")
            throw e
        }
    }
}