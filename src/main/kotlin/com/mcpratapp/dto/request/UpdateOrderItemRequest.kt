package com.mcpratapp.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class UpdateOrderItemRequest(
    @field:NotNull(message = "A quantidade é obrigatória.")
    @field:Min(value = 1, message = "A quantidade deve ser maior que zero.")
    val quantity: Int
)
