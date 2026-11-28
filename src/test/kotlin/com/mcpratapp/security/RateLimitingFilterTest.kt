package com.mcpratapp.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@ExtendWith(MockitoExtension::class)
class RateLimitingFilterTest {

    @Mock
    private lateinit var filterChain: FilterChain

    private lateinit var filter: RateLimitingFilter

    @BeforeEach
    fun setUp() {
        val mapper = ObjectMapper().registerModule(JavaTimeModule())
        filter = RateLimitingFilter(mapper)
    }

    @Test
    fun `ignora requisicoes OPTIONS e rotas do swagger`() {
        val optionsReq = MockHttpServletRequest("OPTIONS", "/api/v1/auth/login").apply {
            servletPath = "/api/v1/auth/login"
        }
        val swaggerReq = MockHttpServletRequest("GET", "/swagger-ui/index.html").apply {
            servletPath = "/swagger-ui/index.html"
        }

        filter.doFilter(optionsReq, MockHttpServletResponse(), filterChain)
        filter.doFilter(swaggerReq, MockHttpServletResponse(), filterChain)

        verify(filterChain, times(2)).doFilter(org.mockito.Mockito.any(), org.mockito.Mockito.any())
    }

    @Test
    fun `bloqueia rota de login apos 60 requisicoes do mesmo IP`() {
        repeat(60) {
            val request = MockHttpServletRequest("POST", "/api/v1/auth/login").apply {
                servletPath = "/api/v1/auth/login"
                remoteAddr = "10.0.0.1"
            }
            val response = MockHttpServletResponse()
            filter.doFilter(request, response, filterChain)
            assertEquals(HttpStatus.OK.value(), response.status)
        }

        val blockedRequest = MockHttpServletRequest("POST", "/api/v1/auth/login").apply {
            servletPath = "/api/v1/auth/login"
            remoteAddr = "10.0.0.1"
        }
        val blockedResponse = MockHttpServletResponse()
        filter.doFilter(blockedRequest, blockedResponse, filterChain)

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), blockedResponse.status)
        assertTrue(blockedResponse.contentAsString.contains("Limite de requisições excedido"))
        verify(filterChain, times(60)).doFilter(org.mockito.Mockito.any(), org.mockito.Mockito.any())
    }

    @Test
    fun `isola contagem por IP usando X-Forwarded-For`() {
        repeat(60) {
            val request = MockHttpServletRequest("POST", "/api/v1/auth/login").apply {
                servletPath = "/api/v1/auth/login"
                addHeader("X-Forwarded-For", "192.168.1.10, 10.0.0.1")
            }
            filter.doFilter(request, MockHttpServletResponse(), filterChain)
        }

        val otherIpRequest = MockHttpServletRequest("POST", "/api/v1/auth/login").apply {
            servletPath = "/api/v1/auth/login"
            addHeader("X-Forwarded-For", "192.168.1.20")
        }
        val otherIpResponse = MockHttpServletResponse()
        filter.doFilter(otherIpRequest, otherIpResponse, filterChain)

        assertEquals(HttpStatus.OK.value(), otherIpResponse.status)
        verify(filterChain, times(61)).doFilter(org.mockito.Mockito.any(), org.mockito.Mockito.any())
    }
}
