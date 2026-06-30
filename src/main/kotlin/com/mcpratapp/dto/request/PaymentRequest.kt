package com.mcpratapp.dto.request

import com.mcpratapp.model.PaymentMethod
import jakarta.validation.constraints.NotNull

data class PaymentRequest(
    @field:NotNull(message = "O método de pagamento não pode ser nulo")
    val paymentMethod: PaymentMethod
)
