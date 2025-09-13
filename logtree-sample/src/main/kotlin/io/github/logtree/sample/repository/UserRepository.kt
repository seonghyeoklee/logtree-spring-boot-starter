package io.github.logtree.sample.repository

import io.github.logtree.sample.model.User
import io.github.logtree.spring.annotation.Traceable
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class UserRepository {
    private val users = ConcurrentHashMap<Long, User>()
    private val idCounter = AtomicLong(1)
    
    init {
        // Add some sample data
        save(User(0, "John Doe", "john@example.com"))
        save(User(0, "Jane Smith", "jane@example.com"))
        save(User(0, "Bob Johnson", "bob@example.com"))
    }
    
    @Traceable(name = "UserRepository.findById")
    fun findById(id: Long): User? {
        // Simulate database delay
        Thread.sleep(50)
        return users[id]
    }
    
    @Traceable(name = "UserRepository.save")
    fun save(user: User): User {
        // Simulate database delay
        Thread.sleep(100)
        
        val savedUser = if (user.id == 0L) {
            user.copy(id = idCounter.getAndIncrement())
        } else {
            user
        }
        
        users[savedUser.id] = savedUser
        return savedUser
    }
    
    @Traceable(name = "UserRepository.search")
    fun search(query: String): List<User> {
        // Simulate database delay
        Thread.sleep(75)
        
        return users.values.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
            user.email.contains(query, ignoreCase = true)
        }
    }
}