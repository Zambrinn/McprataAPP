package com.mcpratapp.security

import com.mcpratapp.model.UserStatus
import com.mcpratapp.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtFilter(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository
    ) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(JwtFilter::class.java)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {

        val uri = request.servletPath  // servletPath em vez de requestURI
        val method = request.method

        logger.info("JwtFilter - Requisição: $method $uri")

        if (method == "OPTIONS") {
            logger.info("JwtFilter - Preflight request (OPTIONS), permitindo...")
            filterChain.doFilter(request, response)
            return
        }

        if (uri.startsWith("/auth/")) {
            logger.info("JwtFilter - Auth endpoint, sem validação JWT")
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader("Authorization")
        logger.info("JwtFilter Authorization header: ${authHeader?.take(20)}...")

        val token = authHeader?.removePrefix("Bearer ")

        if (token != null && jwtProvider.isTokenValid(token)) {
            val authenticatedUser = jwtProvider.getAuthenticatedUserFromToken(token)

            if (authenticatedUser != null) {
                val authority = SimpleGrantedAuthority("ROLE_${authenticatedUser.role.name}")

                val auth = UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    listOf(authority)
                )
            }
        } else {
            logger.warn("JwtFilter Token inválido ou ausente")
        }

        filterChain.doFilter(request, response)
    }
}