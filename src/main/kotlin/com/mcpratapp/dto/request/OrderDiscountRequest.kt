package com.mcpratapp.dto.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class OrderDiscountRequest(
    @field:NotNull(message = "O desconto não pode ser nulo")
    @field:DecimalMin(value = "0.00", message = "O desconto não pode ser negativo")
    val discountAmount: BigDecimal
)
