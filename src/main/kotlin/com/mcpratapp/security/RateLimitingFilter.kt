package com.mcpratapp.security

import com.mcpratapp.exception.ApiError
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.catalina.util.RateLimiter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@Component
class RateLimitingFilter (
    private val objectMapper: ObjectMapper
): OncePerRequestFilter() {
    private val authLimit = 10
    private val generalLimit = 10
    private val timeWindowMs = 60_000L

    private val requestCounts = ConcurrentHashMap<String, RateLimitCounter>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val clientIp = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr
            ?: "unknown"

        val isAuthRoute = request.servletPath.startsWith("/api/v1/auth/login")
        val key = "$clientIp:${if (isAuthRoute) "auth" else "general"}"
        val limit = if (isAuthRoute) authLimit else generalLimit

        val now = System.currentTimeMillis()

        val counter = requestCounts.compute(key) { _, current ->
            if (current == null || now - current.startTime > timeWindowMs) {
                RateLimitCounter(startTime = now, count = 1)
            } else {
                current.copy(count = current.count + 1)
            }
        }

        if (counter != null && counter.count > limit) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE

            val errorResponse = ApiError(
                status = HttpStatus.TOO_MANY_REQUESTS.value(),
                error = HttpStatus.TOO_MANY_REQUESTS.reasonPhrase,
                message = "Limite de requisições excedido. Tente novamente em 1 minuto.",
                path = request.servletPath
            )
            response.writer.write(objectMapper.writeValueAsString(errorResponse))
            return
        }
        filterChain.doFilter(request, response)
    }
    private data class RateLimitCounter(val startTime: Long, val count: Int)
}