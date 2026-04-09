package com.mcpratapp.dto.response

import com.mcpratapp.model.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val clientId: UUID,
    val vendorId: UUID,
    val status: OrderStatus,
    val totalAmount: BigDecimal,
    val items: List<OrderItemResponse>,
    val createdAt: LocalDateTime,
    val confirmedAt: LocalDateTime?
)