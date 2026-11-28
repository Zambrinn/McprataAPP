package com.mcpratapp.service

import com.mcpratapp.anyNonNull
import com.mcpratapp.exception.ForbidenException
import com.mcpratapp.model.RefreshToken
import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import com.mcpratapp.repository.RefreshTokenRepository
import com.mcpratapp.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RefreshTokenServiceTest {

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var refreshTokenService: RefreshTokenService

    @BeforeEach
    fun setUp() {
        refreshTokenService = RefreshTokenService(refreshTokenRepository, userRepository, 60_000L)
    }

    private fun sampleUser() = User(
        id = UUID.randomUUID(),
        username = "admin",
        email = "admin@test.com",
        password = "hash",
        role = Role.ADMIN
    )

    @Test
    fun `createRefreshToken cria novo token quando usuario ainda nao possui`() {
        val user = sampleUser()
        `when`(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty())
        `when`(refreshTokenRepository.save(anyNonNull<RefreshToken>())).thenAnswer { it.getArgument<RefreshToken>(0) }

        val token = refreshTokenService.createRefreshToken(user)

        assertEquals(user, token.user)
        assertTrue(token.expiryDate.isAfter(Instant.now()))
    }

    @Test
    fun `createRefreshToken rotaciona token existente do usuario`() {
        val user = sampleUser()
        val existing = RefreshToken(
            id = UUID.randomUUID(),
            token = "token-antigo",
            user = user,
            expiryDate = Instant.now().minusSeconds(10)
        )
        `when`(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(existing))
        `when`(refreshTokenRepository.save(anyNonNull<RefreshToken>())).thenAnswer { it.getArgument<RefreshToken>(0) }

        val updated = refreshTokenService.createRefreshToken(user)

        assertEquals(existing.id, updated.id)
        assertNotEquals("token-antigo", updated.token)
        assertTrue(updated.expiryDate.isAfter(Instant.now()))
    }

    @Test
    fun `verifyExpiration retorna token valido sem deletar`() {
        val valid = RefreshToken(
            id = UUID.randomUUID(),
            token = "valido",
            user = sampleUser(),
            expiryDate = Instant.now().plusSeconds(300)
        )

        val result = refreshTokenService.verifyExpiration(valid)

        assertEquals(valid, result)
        verify(refreshTokenRepository, never()).delete(anyNonNull())
    }

    @Test
    fun `verifyExpiration deleta token expirado e lanca ForbidenException`() {
        val expired = RefreshToken(
            id = UUID.randomUUID(),
            token = "expirado",
            user = sampleUser(),
            expiryDate = Instant.now().minusSeconds(10)
        )

        assertThrows(ForbidenException::class.java) {
            refreshTokenService.verifyExpiration(expired)
        }
        verify(refreshTokenRepository).delete(expired)
    }

    @Test
    fun `findByToken retorna token ou null`() {
        val token = RefreshToken(id = UUID.randomUUID(), token = "abc", user = sampleUser(), expiryDate = Instant.now().plusSeconds(60))
        `when`(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(token))
        `when`(refreshTokenRepository.findByToken("xyz")).thenReturn(Optional.empty())

        assertEquals(token, refreshTokenService.findByToken("abc"))
        assertNull(refreshTokenService.findByToken("xyz"))
    }
}
