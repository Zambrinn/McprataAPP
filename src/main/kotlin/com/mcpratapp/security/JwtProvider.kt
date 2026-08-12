package com.mcpratapp.security

import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtProvider(
    @Value("\${app.jwt.secret:}")
    private val secret: String,
    @Value("\${app.jwt.expiration:90000}")
    private val expiration: Long
) {
    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    @PostConstruct
    fun validateSecret() {
        require(secret.isNotBlank() && secret.length >= 32) {
            "ERRO CRÍTICO DE SEGURANÇA: A propriedade 'app.jwt.secret' deve ser configurada e conter pelo menos 32 caracteres (256 bits)."
        }
    }
    fun generateToken(user: User): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun getAuthenticatedUserFromToken(token: String): AuthenticatedUser? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload

            val userId = UUID.fromString(claims.subject)
            val email = claims.get("email", String::class.java)
            val roleStr = claims.get("role", String::class.java)
            val role = Role.valueOf(roleStr)

            AuthenticatedUser(userId, email, role)
        } catch (e: Exception) {
            null
        }
    }

    fun getEmailFromToken(token: String): String? {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
        } catch (e: Exception) {
            null
        }
    }

    fun getUserIdFromToken(token: String): String? {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
                .get("userId", String::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}