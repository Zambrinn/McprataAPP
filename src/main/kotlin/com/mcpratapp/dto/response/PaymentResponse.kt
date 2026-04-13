package com.mcpratapp.dto.response

import com.mcpratapp.model.PaymentMethod
import com.mcpratapp.model.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class PaymentResponse(
    val id: UUID,
    val orderId: UUID,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val amount: BigDecimal,
    val paidAt: LocalDateTime?,
    val createdAt: LocalDateTime
)