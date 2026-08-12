package com.mcpratapp.dto.response

import com.mcpratapp.model.RefreshToken
import java.util.UUID

data class AuthResponse(
    val token: String,
    val user: UserResponse,
    val expiresIn: Long,
    val refreshToken: String
)