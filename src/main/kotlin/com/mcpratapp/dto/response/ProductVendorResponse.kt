package com.mcpratapp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class ProductVendorResponse(
    val id: UUID,
    val productId: UUID,
    val vendorId: UUID,
    val price: BigDecimal,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
)
