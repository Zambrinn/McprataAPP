package com.mcpratapp.controller

import com.mcpratapp.dto.request.UserRequest
import com.mcpratapp.dto.response.UserResponse
import com.mcpratapp.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController (
    private val userService: UserService
) {
    @PostMapping
    fun createUser(@Valid @RequestBody request: UserRequest): ResponseEntity<UserResponse> {
        val userToSave = userService.createUser(request)
        return ResponseEntity.ok(userToSave)
    }

    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserResponse>> {
        val allUsers: List<UserResponse> = userService.getAllUsers()
        return ResponseEntity.ok(allUsers)
    }

    @GetMapping("/{id}")
    fun getUserById(@Valid @PathVariable userId: UUID): ResponseEntity<UserResponse> {
        val foundUser = userService.getUserById(userId)
        return foundUser?.let { user -> ResponseEntity.ok(user) }
            ?: ResponseEntity.notFound().build()
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable userId: UUID, @Valid @RequestBody request: UserRequest): ResponseEntity<*>? {
        val updatedUser = userService.updateUser(userId, request)
        return ResponseEntity.ok(updatedUser)
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable userId: UUID): Unit {
        val foundUser = userService.getUserById(userId)
        val deletedUser = userService.deleteUser(userId)
        return deletedUser
    }
}