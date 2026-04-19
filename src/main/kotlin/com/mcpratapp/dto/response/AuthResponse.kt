package com.mcpratapp.dto.response

import java.util.UUID

data class AuthResponse(
    val token: String,
    val user: UserResponse,
    val expiresIn: Long
)