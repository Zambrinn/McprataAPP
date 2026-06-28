package com.mcpratapp.dto.response

import java.time.LocalDateTime
import java.util.UUID

data class ProductResponse(
    val id: UUID,
    val sku: String,
    val name: String,
    val description: String?,
    val totalQuantity: Int,
    val reservedQuantity: Int,
    val isActive: Boolean,
    val createdAt: LocalDateTime
)
