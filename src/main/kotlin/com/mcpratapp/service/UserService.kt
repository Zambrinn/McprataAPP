package com.mcpratapp.service

import com.mcpratapp.dto.request.UserRequest
import com.mcpratapp.dto.request.UserUpdateRequest
import com.mcpratapp.dto.response.UserResponse
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import com.mcpratapp.model.UserStatus
import com.mcpratapp.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
            throw ConflictException("Email já cadastrado.")
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

    fun getAllUsers(search: String?, role: Role?, status: UserStatus?, pageable: Pageable): Page<UserResponse> {
        val normalizedSearch = search
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }

        val users = if (normalizedSearch == null) {
            userRepository.findUsers(
                role = role,
                status = status,
                pageable = pageable
            )
        } else {
            userRepository.searchUsers(
                search = normalizedSearch,
                role = role,
                status = status,
                pageable = pageable
            )
        }
        return users.map { it.toResponse() }
    }

    fun getUserById(userId: UUID): UserResponse {
        val foundUser = userRepository.findByIdOrNull(userId)
            ?: throw ResourceNotFoundException("Usuário com id: ${userId} não encontrado.")

        return foundUser.toResponse()
    }

    fun updateUser(userId: UUID, request: UserUpdateRequest): UserResponse {
        val existingUser = userRepository.findByIdOrNull(userId)
            ?: throw ResourceNotFoundException("Usuário com id: ${userId} não encontrado.")

        if (existingUser.status != UserStatus.ACTIVE) {
            throw ConflictException("Não é possível editar usuários desativados.")
        }

        val emailOwner = userRepository.findByEmail(request.email)

        if (emailOwner != null && emailOwner.id != existingUser.id) {
            throw ConflictException("Email já cadastrado.")
        }

        existingUser.username = request.name
        existingUser.email = request.email
        existingUser.role = request.role

        request.password
            ?.takeIf { it.isNotBlank() }
            ?.let { existingUser.password = passwordEncoder.encode(it).toString()    }

        return userRepository.save(existingUser).toResponse()
    }

    fun deactivateUser(userId: UUID): UserResponse {
        val existingUser = userRepository.findByIdOrNull(userId)
            ?: throw ResourceNotFoundException("Usuário com id: ${userId} não encontrado.")

        if (existingUser.status != UserStatus.ACTIVE) {
            throw ConflictException("Somente usuários ativos podem ser desativados.")
        }
        
        existingUser.status = UserStatus.INACTIVE
        return userRepository.save(existingUser).toResponse()
    }

    fun restoreUser(userId: UUID): UserResponse {
        val existingUser = userRepository.findByIdOrNull(userId)
            ?: throw ResourceNotFoundException("Usuário com id: ${userId} não encontrado.")

        if (existingUser.status == UserStatus.ACTIVE) {
            throw ConflictException("O usuário já está ativo.")
        }

        val emailOwner = userRepository.findByEmail(existingUser.email)
        if (emailOwner != null && emailOwner.id != existingUser.id && emailOwner.status == UserStatus.ACTIVE) {
            throw ConflictException("Já existe um usuário ativo com esse e-mail.")
        } 

        existingUser.status = UserStatus.ACTIVE
        return userRepository.save(existingUser).toResponse()
    }

    fun deleteUser(userId: UUID) {
        val foundUser = userRepository.findByIdOrNull(userId)
            ?: throw ResourceNotFoundException("Usuário com id: ${userId} não encontrado.")

        return userRepository.delete(foundUser)
    }

    private fun User.toResponse(): UserResponse {
        return UserResponse(
            id = this.id!!,
            name = this.username,
            email = this.email,
            role = this.role,
            createdAt = this.createdAt ?: LocalDateTime.now(),
            status = this.status
        )
    }
}