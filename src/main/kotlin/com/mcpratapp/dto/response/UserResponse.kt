package com.mcpratapp.dto.response

import com.mcpratapp.model.Role
import java.time.LocalDateTime
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val role: Role,
    val createdAt: LocalDateTime
)

