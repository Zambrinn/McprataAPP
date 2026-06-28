package com.mcpratapp.dto.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class ProductVendorRequest(
    @field:NotNull(message = "Produto é obrigatório")
    val productId: UUID,

    @field:NotNull(message = "Vendedor é obrigatório")
    val vendorId: UUID,

    @field:NotNull(message = "Preço é obrigatório")
    @field:DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        val price: BigDecimal
)