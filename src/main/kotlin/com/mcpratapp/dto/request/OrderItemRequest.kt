package com.mcpratapp.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class OrderItemRequest(
    @NotNull(message = "orderId não pode ser nulo")
    val orderId: UUID,

    @NotNull(message = "productId não pode ser nulo")
    val productId: UUID,

    @Min(value = 1, message = "quantity deve ser maior que 0")
    val quantity: Int
)
