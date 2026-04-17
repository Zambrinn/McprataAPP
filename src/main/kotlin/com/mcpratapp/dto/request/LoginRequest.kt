package com.mcpratapp.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @Email
    val email: String,

    @NotBlank(message = "A senha não pode ser nula")
    val password: String
)