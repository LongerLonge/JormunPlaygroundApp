package com.jormun.likerouter_compiler

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class RouterProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.warn("return RouterAnnotationProcessor")
        return RouterAnnotationProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
    }
}