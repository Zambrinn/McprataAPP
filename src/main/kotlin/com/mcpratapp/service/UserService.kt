package com.mcpratapp.service

import com.mcpratapp.dto.request.UserRequest
import com.mcpratapp.dto.response.UserResponse
import com.mcpratapp.model.User
import com.mcpratapp.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class UserService (
    private val userRepository: UserRepository
) {
    fun createUser(request: UserRequest): UserResponse {
        userRepository.findByEmail(request.email)?.let {
            throw IllegalArgumentException("Email já cadastrado.")
        }

        val userToSave = User(
            username = request.name,
            email = request.email,
            password = request.password,
            role = request.role,
            createdAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(userToSave)
        return savedUser.toResponse()
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

    fun updateUser(userId: UUID, request: UserRequest): UserResponse {
        val existingUser = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("Usuário com id: ${userId} não encontrado.")

        val updatedUser = User(
            id = existingUser.id,
            username = request.name,
            email = request.email,
            password = request.password,
            role = request.role,
            createdAt = existingUser.createdAt,
            updatedAt = LocalDateTime.now()
        )

        return userRepository.save(updatedUser).toResponse()
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
            createdAt = this.createdAt
        )
    }
}