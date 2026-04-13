package com.mcpratapp.dto.request

import com.mcpratapp.model.PaymentMethod
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class ConfirmOrderRequest(
    val orderId: UUID,
    @field:NotNull(message = "O método de pagamento não pode ser nulo")
    val paymentMethod: PaymentMethod
)
