package com.mcpratapp.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ProductRequest(
    @field:NotBlank(message = "SKU do produto não pode ser nulo")
    val sku: String,
    @field:NotBlank(message = "O nome do produto não pode ser nulo")
    val name: String,
    val description: String? = null,
    @field:NotNull(message = "A quantidade do produto não pode ser nula")
    @field:Min(value = 0, message = "A quantidade do produto não pode ser negativa")
    val totalQuantity: Int,
)