package com.mcpratapp.security

import com.mcpratapp.model.Role
import java.util.UUID

data class AuthenticatedUser(
    val id: UUID,
    val email: String,
    val role: Role
)
