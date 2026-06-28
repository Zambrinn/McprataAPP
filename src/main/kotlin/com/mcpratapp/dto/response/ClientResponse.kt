package com.mcpratapp.dto.response

import java.time.LocalDateTime
import java.util.UUID

data class ClientResponse(
    val id: UUID,
    val name: String,
    val whatsappNumber: String,
    val email: String,
    val address: String,
    val companyName: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)