package io.github.logtree.spring.aspect

import io.github.logtree.core.CausalityChain
import io.github.logtree.core.LogTree
import io.github.logtree.spring.annotation.Traceable
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * AOP Aspect for automatic method tracing
 */
@Aspect
@Component
class LogTreeAspect {
    private val logger = LoggerFactory.getLogger(LogTreeAspect::class.java)
    
    @Around("@annotation(traceable)")
    fun traceMethod(joinPoint: ProceedingJoinPoint, traceable: Traceable): Any? {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        
        val spanName = if (traceable.name.isNotEmpty()) {
            traceable.name
        } else {
            "${joinPoint.target.javaClass.simpleName}.${method.name}"
        }
        
        // Log method arguments if requested
        if (traceable.includeArgs && joinPoint.args.isNotEmpty()) {
            val argsStr = joinPoint.args.joinToString(", ") { 
                it?.toString() ?: "null" 
            }
            LogTree.debug("Arguments: $argsStr")
        }
        
        // Add tags if provided
        if (traceable.tags.isNotEmpty()) {
            LogTree.debug("Tags: ${traceable.tags.joinToString(", ")}")
        }
        
        return try {
            LogTree.span(spanName) {
                val result = joinPoint.proceed()
                
                // Log return value if requested
                if (traceable.includeResult && result != null) {
                    LogTree.debug("Result: $result")
                }
                
                result
            }
        } catch (e: Exception) {
            if (traceable.trackErrors) {
                trackError(e, spanName, joinPoint)
            }
            throw e
        }
    }
    
    @Around("@within(traceable)")
    fun traceClass(joinPoint: ProceedingJoinPoint, traceable: Traceable): Any? {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        
        // Skip if method has its own @Traceable annotation
        if (method.isAnnotationPresent(Traceable::class.java)) {
            return joinPoint.proceed()
        }
        
        val spanName = "${joinPoint.target.javaClass.simpleName}.${method.name}"
        
        return try {
            LogTree.span(spanName) {
                joinPoint.proceed()
            }
        } catch (e: Exception) {
            if (traceable.trackErrors) {
                trackError(e, spanName, joinPoint)
            }
            throw e
        }
    }
    
    private fun trackError(
        exception: Exception,
        spanName: String,
        joinPoint: ProceedingJoinPoint
    ) {
        val chain = CausalityChain.current()
        
        // Add method context
        chain.addCause(
            event = "Method execution failed: $spanName",
            metadata = mapOf(
                "class" to joinPoint.target.javaClass.name,
                "method" to joinPoint.signature.name,
                "args" to joinPoint.args.map { it?.javaClass?.simpleName ?: "null" }
            )
        )
        
        // Add exception chain
        chain.addException(exception)
        
        // Log the causality chain
        chain.log()
    }
}