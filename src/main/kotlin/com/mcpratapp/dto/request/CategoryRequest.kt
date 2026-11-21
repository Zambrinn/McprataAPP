package com.mcpratapp.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CategoryRequest(

    @field:NotBlank(message = "O nome da categoria é obrigatório")
    @field:Size(max = 80, message = "O nome da categoria deve ter no máximo 80 caracteres")
    val name: String,

    @field:Size(max = 10, message = "O prefixo SKU deve ter no máximo 10 caracteres")
    val skuPrefix: String ?= null
)
