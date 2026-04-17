package com.mcpratapp.controller

import com.mcpratapp.dto.request.LoginRequest
import com.mcpratapp.dto.request.RegisterRequest
import com.mcpratapp.dto.request.UserRequest
import com.mcpratapp.dto.response.AuthResponse
import com.mcpratapp.repository.UserRepository
import com.mcpratapp.security.JwtProvider
import com.mcpratapp.service.UserService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RequestMapping("/api/v1/auth")
@RestController
class AuthController(
    @Value($$"${app.jwt.expiration:86400000}")
    private val expiration: Long,
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder,
    private val userService: UserService
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val foundUser = userRepository.findByEmail(request.email)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (!passwordEncoder.matches(request.password, foundUser.password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val token = jwtProvider.generateToken(foundUser.email, foundUser.id.toString())
        return ResponseEntity.ok(AuthResponse(
            token = token,
            email = foundUser.email,
            userId = foundUser.id!!,
            expiresIn = expiration
        ))
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        userRepository.findByEmail(request.email)?.let {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        val userRequest = UserRequest(
            name = request.name,
            email = request.email,
            password = request.password,
            role = request.role
        )

        val createdUser = userService.createUser(userRequest)

        println("DEBUG - createdUser: $createdUser")
        println("DEBUG - createdUser.id: ${createdUser.id}")
        println("DEBUG - createdUser.email: ${createdUser.email}")

        val token = jwtProvider.generateToken(createdUser.email, createdUser.id.toString())
        println("DEBUG - token: $token")
        return ResponseEntity.status(HttpStatus.CREATED).body(
            AuthResponse(
                token = token,
                email = createdUser.email,
                userId = createdUser.id,
                expiresIn = expiration
            )
        )
    }
}