package com.mcpratapp.service

import com.mcpratapp.exception.ForbidenException
import com.mcpratapp.model.RefreshToken
import com.mcpratapp.model.User
import com.mcpratapp.repository.RefreshTokenRepository
import com.mcpratapp.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import java.util.UUID
import java.time.Instant

class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    @Value("\${app.jwt.refresh-expiration:604800000}")
    private val expirationInMs: Long
) {
    @Transactional
    fun createRefreshToken(user: User): RefreshToken {
        refreshTokenRepository.deleteByUser(user) // Remove token antigo do usuário
        val refreshToken = RefreshToken(
            token = UUID.randomUUID().toString(),
            user = user,
            expiryDate = Instant.now().plusMillis(expirationInMs)
        )
        return refreshTokenRepository.save(refreshToken)
    }

    fun verifyExpiration(token: RefreshToken): RefreshToken {
        if (token.expiryDate < Instant.now()) {
            refreshTokenRepository.delete(token)
            throw ForbidenException("Refresh token expirou. Faça login novamente")
        }
        return token
    }

    fun findByToken(token: String): RefreshToken? {
        return refreshTokenRepository.findByToken(token).orElse(null)
    }
}