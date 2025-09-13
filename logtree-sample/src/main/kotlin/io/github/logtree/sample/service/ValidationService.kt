package io.github.logtree.sample.service

import io.github.logtree.core.LogTree
import io.github.logtree.sample.model.User
import io.github.logtree.spring.annotation.Traceable
import org.springframework.stereotype.Service

@Service
class ValidationService {
    
    @Traceable(name = "ValidationService.validateUser")
    fun validateUser(user: User) {
        LogTree.span("Validate Email") {
            if (!isValidEmail(user.email)) {
                throw IllegalArgumentException("Invalid email format: ${user.email}")
            }
            LogTree.info("Email validation passed")
        }
        
        LogTree.span("Validate Name") {
            if (user.name.isBlank()) {
                throw IllegalArgumentException("Name cannot be blank")
            }
            if (user.name.length < 2) {
                throw IllegalArgumentException("Name must be at least 2 characters")
            }
            LogTree.info("Name validation passed")
        }
        
        LogTree.info("User validation completed successfully")
    }
    
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}