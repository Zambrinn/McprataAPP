package com.mcpratapp.dto.response

import java.util.UUID

data class AuthResponse(
    val token: String,
    val email: String,
    val userId: UUID,
    val expiresIn: Long
)