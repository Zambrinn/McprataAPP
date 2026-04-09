package com.mcpratapp.dto.response

import java.math.BigDecimal
import java.util.UUID

data class OrderItemResponse(
    val id: UUID,
    val productId: UUID,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val subtotal: BigDecimal
)
