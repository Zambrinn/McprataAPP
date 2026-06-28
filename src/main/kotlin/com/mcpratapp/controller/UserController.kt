package com.mcpratapp.controller

import com.mcpratapp.dto.request.UserRequest
import com.mcpratapp.dto.request.UserUpdateRequest
import com.mcpratapp.dto.response.UserResponse
import com.mcpratapp.model.Role
import com.mcpratapp.model.UserStatus
import com.mcpratapp.service.UserService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestParam
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
    fun getAllUsers(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) role: Role?,
        @RequestParam(required = false) status: UserStatus?,
        @PageableDefault(size = 10, sort = ["username"]) pageable: Pageable): ResponseEntity<Page<UserResponse>> {
            return ResponseEntity.ok(
                userService.getAllUsers(
                    search = search,
                    role = role,
                    status = status,
                    pageable = pageable
                )
            )
    }

    @GetMapping("/{id}")
    fun getUserById(@Valid @PathVariable("id") userId: UUID): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.getUserById(userId))
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable("id") userId: UUID,
                   @Valid @RequestBody request: UserUpdateRequest): ResponseEntity<UserResponse>? {
        return ResponseEntity.ok(userService.updateUser(userId, request))
    }

    @PutMapping("/{id}/deactivate")
    fun deactivateUser(@PathVariable("id") userId: UUID): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.deactivateUser(userId))
    }

    @PutMapping("/{id}/restore")
    fun restoreUser(@PathVariable("id") userId: UUID): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.restoreUser(userId))
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable("id") userId: UUID): ResponseEntity<Void> {
        userService.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }
}