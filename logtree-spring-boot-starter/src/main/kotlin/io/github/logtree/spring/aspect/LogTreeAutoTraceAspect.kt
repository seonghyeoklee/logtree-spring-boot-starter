package io.github.logtree.spring.aspect

import io.github.logtree.core.LogTree
import io.github.logtree.spring.config.LogTreeProperties
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * AOP Aspect for automatic method tracing based on configuration
 * Automatically traces controllers, services, and repositories without requiring @Traceable annotations
 */
@Aspect
@Component
class LogTreeAutoTraceAspect(
    private val properties: LogTreeProperties
) {
    private val logger = LoggerFactory.getLogger(LogTreeAutoTraceAspect::class.java)
    
    /**
     * Auto-trace all controller methods when auto-trace-controllers is enabled
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller)")
    fun traceControllers(joinPoint: ProceedingJoinPoint): Any? {
        if (!properties.autoTraceControllers) {
            return joinPoint.proceed()
        }
        
        return executeWithTrace(joinPoint, "Controller")
    }
    
    /**
     * Auto-trace all service methods when auto-trace-services is enabled
     */
    @Around("@within(org.springframework.stereotype.Service)")
    fun traceServices(joinPoint: ProceedingJoinPoint): Any? {
        if (!properties.autoTraceServices) {
            return joinPoint.proceed()
        }
        
        return executeWithTrace(joinPoint, "Service")
    }
    
    /**
     * Auto-trace all repository methods when auto-trace-repositories is enabled
     */
    @Around("@within(org.springframework.stereotype.Repository)")
    fun traceRepositories(joinPoint: ProceedingJoinPoint): Any? {
        // Note: auto-trace-repositories property would need to be added to LogTreeProperties
        // For now, we'll check if autoTraceServices is enabled as a fallback
        if (!properties.autoTraceServices) {
            return joinPoint.proceed()
        }
        
        return executeWithTrace(joinPoint, "Repository")
    }
    
    /**
     * Auto-trace transactional methods with enhanced boundary visualization
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    fun traceTransactional(joinPoint: ProceedingJoinPoint): Any? {
        if (!properties.autoTraceServices) {
            return joinPoint.proceed()
        }
        
        return executeWithTransactionTrace(joinPoint)
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
    
    private fun executeWithTransactionTrace(joinPoint: ProceedingJoinPoint): Any? {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        val className = joinPoint.target.javaClass.simpleName
        val methodName = method.name
        
        val spanName = "[Transaction] $className.$methodName"
        
        // Check transaction state before method execution
        val wasTransactionActiveBeforeMethod = TransactionSynchronizationManager.isActualTransactionActive()
        val transactionNameBefore = TransactionSynchronizationManager.getCurrentTransactionName()
        
        return try {
            LogTree.span(spanName) {
                // Check transaction state after entering method
                val isTransactionActiveAfterMethod = TransactionSynchronizationManager.isActualTransactionActive()
                val transactionNameAfter = TransactionSynchronizationManager.getCurrentTransactionName()
                
                // Transaction boundary start logging with propagation detection
                if (properties.traceTransactionBoundaries) {
                    when {
                        !wasTransactionActiveBeforeMethod && isTransactionActiveAfterMethod -> {
                            LogTree.info("┌─────────── TRANSACTION START (NEW TRANSACTION) ───────────┐")
                        }
                        wasTransactionActiveBeforeMethod && isTransactionActiveAfterMethod -> {
                            if (transactionNameBefore == transactionNameAfter) {
                                LogTree.info("├─────────── TRANSACTION JOIN (JOIN EXISTING) ─────────────┤")
                            } else {
                                LogTree.info("├─────────── TRANSACTION NESTED (NESTED START) ────────┤")
                            }
                        }
                        wasTransactionActiveBeforeMethod && !isTransactionActiveAfterMethod -> {
                            LogTree.info("├─────────── TRANSACTION SUSPENDED (SUSPENDED) ──────┤")
                        }
                        else -> {
                            LogTree.info("┌─────────── TRANSACTION START ───────────┐")
                        }
                    }
                }
                
                val result = joinPoint.proceed()
                
                // Check transaction state after method completion
                val isTransactionActiveAfterCompletion = TransactionSynchronizationManager.isActualTransactionActive()
                
                // Transaction boundary completion logging
                if (properties.traceTransactionBoundaries) {
                    when {
                        isTransactionActiveAfterMethod && !isTransactionActiveAfterCompletion -> {
                            LogTree.info("└─────────── TRANSACTION COMMIT (COMPLETED) ────────────┘")
                        }
                        isTransactionActiveAfterMethod && isTransactionActiveAfterCompletion -> {
                            LogTree.info("├─────────── TRANSACTION CONTINUE (CONTINUE) ───────────┤")
                        }
                        !isTransactionActiveAfterMethod && isTransactionActiveAfterCompletion -> {
                            LogTree.info("├─────────── TRANSACTION RESUME (RESUME) ─────────────┤")
                        }
                        else -> {
                            LogTree.info("└─────────── TRANSACTION COMMIT ───────────┘")
                        }
                    }
                }
                
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
            if (properties.traceTransactionBoundaries) {
                val isTransactionActiveOnError = TransactionSynchronizationManager.isActualTransactionActive()
                if (isTransactionActiveOnError) {
                    LogTree.error("└─────────── TRANSACTION ROLLBACK (ROLLBACK) ─────────┘ ${e.message}")
                } else {
                    LogTree.error("└─────────── TRANSACTION ROLLBACK ─────────┘ ${e.message}")
                }
            }
            LogTree.error("Transaction method failed: ${e.message}")
            throw e
        }
    }
}