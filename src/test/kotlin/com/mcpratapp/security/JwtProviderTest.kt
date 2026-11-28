package com.mcpratapp.security

import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtProviderTest {

    private val validSecret = "uma-chave-secreta-super-segura-com-mais-de-32-caracteres"

    private fun sampleUser(): User = User(
        id = UUID.randomUUID(),
        username = "admin",
        email = "admin@mcprata.com",
        password = "encoded-password",
        role = Role.ADMIN
    )

    @Test
    fun `validateSecret falha quando segredo esta vazio ou curto`() {
        val blankProvider = JwtProvider("", 60_000L)
        val shortProvider = JwtProvider("curto-demais-123456", 60_000L)

        assertThrows(IllegalArgumentException::class.java) { blankProvider.validateSecret() }
        assertThrows(IllegalArgumentException::class.java) { shortProvider.validateSecret() }
    }

    @Test
    fun `validateSecret aceita segredo com pelo menos 32 caracteres`() {
        val provider = JwtProvider(validSecret, 60_000L)
        provider.validateSecret()
    }

    @Test
    fun `generateToken gera token valido e extrai AuthenticatedUser corretamente`() {
        val provider = JwtProvider(validSecret, 60_000L)
        val user = sampleUser()

        val token = provider.generateToken(user)

        assertTrue(provider.isTokenValid(token))
        val authUser = provider.getAuthenticatedUserFromToken(token)
        assertNotNull(authUser)
        assertEquals(user.id, authUser!!.id)
        assertEquals(user.email, authUser.email)
        assertEquals(user.role, authUser.role)
        assertEquals(user.id.toString(), provider.getEmailFromToken(token))
    }

    @Test
    fun `token expirado e considerado invalido`() {
        val provider = JwtProvider(validSecret, -1_000L)
        val token = provider.generateToken(sampleUser())

        assertFalse(provider.isTokenValid(token))
        assertNull(provider.getAuthenticatedUserFromToken(token))
    }

    @Test
    fun `token assinado com outra chave ou malformado e invalido`() {
        val providerA = JwtProvider(validSecret, 60_000L)
        val providerB = JwtProvider("outra-chave-secreta-totalmente-diferente-32-chars", 60_000L)
        val tokenFromA = providerA.generateToken(sampleUser())

        assertFalse(providerB.isTokenValid(tokenFromA))
        assertNull(providerB.getAuthenticatedUserFromToken(tokenFromA))
        assertFalse(providerA.isTokenValid("token.invalido.aqui"))
        assertNull(providerA.getEmailFromToken("invalido"))
        assertNull(providerA.getUserIdFromToken("invalido"))
    }
}
