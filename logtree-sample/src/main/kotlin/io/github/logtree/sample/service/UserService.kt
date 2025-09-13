package io.github.logtree.sample.service

import io.github.logtree.core.CausalityChain
import io.github.logtree.core.LogTree
import io.github.logtree.sample.model.User
import io.github.logtree.sample.repository.UserRepository
import io.github.logtree.spring.annotation.Traceable
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val validationService: ValidationService
) {
    
    @Traceable(name = "UserService.findById")
    fun findById(id: Long): User {
        LogTree.info("Fetching user with id: $id")
        
        val user = userRepository.findById(id)
        if (user != null) {
            LogTree.info("User found: ${user.name}")
        } else {
            LogTree.warn("User not found with id: $id")
            throw NoSuchElementException("User not found")
        }
        
        return user
    }
    
    @Traceable(name = "UserService.createUser", includeArgs = true)
    fun createUser(user: User): User {
        LogTree.span("Validation") {
            validationService.validateUser(user)
        }
        
        return LogTree.span("Database Save") {
            val savedUser = userRepository.save(user)
            LogTree.info("User created with id: ${savedUser.id}")
            savedUser
        }
    }
    
    @Traceable(name = "UserService.searchUsers")
    fun searchUsers(query: String): List<User> {
        LogTree.info("Searching users with query: $query")
        
        val results = LogTree.span("Database Query") {
            userRepository.search(query)
        }
        
        LogTree.info("Found ${results.size} users")
        return results
    }
    
    @Traceable(name = "UserService.demonstrateError", trackErrors = true)
    fun demonstrateError(): String {
        try {
            // Simulate a chain of errors
            performRiskyOperation()
        } catch (e: Exception) {
            val chain = CausalityChain.current()
            chain.addCause(
                event = "User service error demonstration",
                causedBy = "Intentional error for testing",
                metadata = mapOf(
                    "service" to "UserService",
                    "method" to "demonstrateError"
                )
            )
            throw e
        }
        return "This should not be reached"
    }
    
    private fun performRiskyOperation() {
        try {
            accessDatabase()
        } catch (e: Exception) {
            throw RuntimeException("Failed to perform risky operation", e)
        }
    }
    
    private fun accessDatabase() {
        try {
            connectToDatabase()
        } catch (e: Exception) {
            throw IllegalStateException("Database access failed", e)
        }
    }
    
    private fun connectToDatabase() {
        throw java.net.ConnectException("Connection refused: Database is down")
    }
}