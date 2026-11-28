package com.mcpratapp.security

import com.mcpratapp.model.Role
import com.mcpratapp.repository.UserRepository
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class JwtFilterTest {

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var filterChain: FilterChain

    private lateinit var jwtFilter: JwtFilter

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        jwtFilter = JwtFilter(jwtProvider, userRepository)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `ignora requisicoes OPTIONS sem validar token`() {
        val request = MockHttpServletRequest("OPTIONS", "/api/v1/orders")
        request.servletPath = "/api/v1/orders"
        val response = MockHttpServletResponse()

        jwtFilter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `ignora rotas publicas como auth, webhook e swagger`() {
        listOf(
            "/api/v1/auth/login",
            "/api/v1/auth",
            "/api/v1/payments/stripe/webhook",
            "/swagger-ui/index.html",
            "/api-docs/swagger-config",
            "/v3/api-docs"
        ).forEach { path ->
            val request = MockHttpServletRequest("GET", path)
            request.servletPath = path
            request.addHeader("Authorization", "Bearer qualquer-token")
            val response = MockHttpServletResponse()

            jwtFilter.doFilter(request, response, filterChain)

            assertNull(SecurityContextHolder.getContext().authentication)
        }
        verify(jwtProvider, never()).isTokenValid("qualquer-token")
    }

    @Test
    fun `autentica usuario no SecurityContext quando token e valido`() {
        val user = AuthenticatedUser(UUID.randomUUID(), "user@test.com", Role.ADMIN)
        val request = MockHttpServletRequest("GET", "/api/v1/orders")
        request.servletPath = "/api/v1/orders"
        request.addHeader("Authorization", "Bearer token-valido")
        val response = MockHttpServletResponse()

        `when`(jwtProvider.isTokenValid("token-valido")).thenReturn(true)
        `when`(jwtProvider.getAuthenticatedUserFromToken("token-valido")).thenReturn(user)

        jwtFilter.doFilter(request, response, filterChain)

        val auth = SecurityContextHolder.getContext().authentication
        assertNotNull(auth)
        assertEquals(user, auth.principal)
        assertEquals(listOf("ROLE_ADMIN"), auth.authorities.map { it.authority })
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `nao autentica quando token e invalido`() {
        val request = MockHttpServletRequest("GET", "/api/v1/orders")
        request.servletPath = "/api/v1/orders"
        request.addHeader("Authorization", "Bearer token-invalido")
        val response = MockHttpServletResponse()

        `when`(jwtProvider.isTokenValid("token-invalido")).thenReturn(false)

        jwtFilter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `nao autentica quando header Authorization esta ausente`() {
        val request = MockHttpServletRequest("GET", "/api/v1/orders")
        request.servletPath = "/api/v1/orders"
        val response = MockHttpServletResponse()

        jwtFilter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }
}
