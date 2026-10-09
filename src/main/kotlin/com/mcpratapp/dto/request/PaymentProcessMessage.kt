package com.mcpratapp.dto.request

import com.mcpratapp.model.PaymentMethod
import java.math.BigDecimal
import java.util.UUID

data class PaymentProcessMessage(
    val orderId: UUID,
    val paymentId: UUID,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod
)
