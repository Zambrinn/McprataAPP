package com.mcpratapp.security

import com.mcpratapp.model.Role
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class SecurityUtilsTest {

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `retorna usuario e id autenticados quando SecurityContext esta preenchido`() {
        val user = AuthenticatedUser(UUID.randomUUID(), "vendedor@test.com", Role.VENDOR)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(user, null, emptyList())

        assertEquals(user, SecurityUtils.getCurrentUser())
        assertEquals(user.id, SecurityUtils.getCurrentUserId())
    }

    @Test
    fun `lanca IllegalStateException quando nao ha autenticacao`() {
        assertThrows(IllegalStateException::class.java) {
            SecurityUtils.getCurrentUser()
        }
    }

    @Test
    fun `lanca IllegalStateException quando principal nao e AuthenticatedUser`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("anonymous", null, emptyList())

        assertThrows(IllegalStateException::class.java) {
            SecurityUtils.getCurrentUser()
        }
    }
}
