package io.github.logtree.spring.annotation

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget
import kotlin.annotation.Retention
import kotlin.annotation.Target

/**
 * Annotation to mark methods for automatic tracing
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Traceable(
    /**
     * Custom name for the span. If empty, method name will be used
     */
    val name: String = "",
    
    /**
     * Whether to include method parameters in the trace
     */
    val includeArgs: Boolean = false,
    
    /**
     * Whether to include return value in the trace
     */
    val includeResult: Boolean = false,
    
    /**
     * Whether to track errors with causality chain
     */
    val trackErrors: Boolean = true,
    
    /**
     * Tags to add to the span
     */
    val tags: Array<String> = []
)