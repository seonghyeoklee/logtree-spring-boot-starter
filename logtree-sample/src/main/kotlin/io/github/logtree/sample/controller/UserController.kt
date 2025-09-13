package io.github.logtree.sample.controller

import io.github.logtree.sample.model.User
import io.github.logtree.sample.service.UserService
import io.github.logtree.spring.annotation.Traceable
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {
    
    @GetMapping("/{id}")
    @Traceable(name = "getUserById", includeArgs = true, includeResult = true)
    fun getUserById(@PathVariable id: Long): User {
        return userService.findById(id)
    }
    
    @PostMapping
    @Traceable(name = "createUser", includeArgs = true)
    fun createUser(@RequestBody user: User): User {
        return userService.createUser(user)
    }
    
    @GetMapping("/search")
    @Traceable(name = "searchUsers")
    fun searchUsers(@RequestParam query: String): List<User> {
        return userService.searchUsers(query)
    }
    
    @GetMapping("/error-demo")
    @Traceable(name = "errorDemo", trackErrors = true)
    fun errorDemo(): String {
        return userService.demonstrateError()
    }
    
    @PostMapping("/transaction-demo")
    @Traceable(name = "transactionDemo", includeArgs = true)
    fun transactionDemo(@RequestBody user: User): User {
        return userService.createUser(user)
    }
}