package com.mcpratapp.service

import com.mcpratapp.dto.request.UserRequest
import com.mcpratapp.dto.request.UserUpdateRequest
import com.mcpratapp.dto.response.UserResponse
import com.mcpratapp.model.User
import com.mcpratapp.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class UserService (
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun createUser(request: UserRequest): UserResponse {
        userRepository.findByEmail(request.email)?.let {
            throw IllegalArgumentException("Email já cadastrado.")
        }

        val userToSave = User(
            username = request.name,
            email = request.email,
            password = passwordEncoder.encode(request.password)
                ?: throw IllegalStateException("Falha ao codificar a senha"),
            role = request.role
        )
        
        val savedUser = userRepository.save(userToSave)
        return savedUser.toResponse()
    }

    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun getAllUsers(): List<UserResponse> {
        val allUsers: List<User> = userRepository.findAll()
        return allUsers.map { it.toResponse() }
    }

    fun getUserById(userId: UUID): UserResponse {
        val foundUser = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("Usuário com id: ${userId} não encontrado.")

        return foundUser.toResponse()
    }

    fun updateUser(userId: UUID, request: UserUpdateRequest): UserResponse {
        val existingUser = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("Usuário com id: ${userId} não encontrado.")

        existingUser.username = request.name
        existingUser.email = request.email
        existingUser.role = request.role

        request.password
            ?.takeIf { it.isNotBlank() }
            ?.let { existingUser.password = passwordEncoder.encode(it).toString()    }

        return userRepository.save(existingUser).toResponse()
    }

    fun deleteUser(userId: UUID) {
        val foundUser = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("Usuário com id: ${userId} não encontrado.")

        return userRepository.delete(foundUser)
    }

    private fun User.toResponse(): UserResponse {
        return UserResponse(
            id = this.id!!,
            name = this.username,
            email = this.email,
            role = this.role,
            createdAt = this.createdAt ?: LocalDateTime.now()
        )
    }
}