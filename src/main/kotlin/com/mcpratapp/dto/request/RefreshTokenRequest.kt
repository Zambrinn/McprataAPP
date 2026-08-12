package com.mcpratapp.dto.request

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank(message = "refreshToken é obrigatório")
    val refreshToken: String
)
