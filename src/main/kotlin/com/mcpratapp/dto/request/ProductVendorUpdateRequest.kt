package com.mcpratapp.dto.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class ProductVendorUpdateRequest(
    @field:NotNull(message = "Preço é obrigatório")
    @field:DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    val price: BigDecimal
)
