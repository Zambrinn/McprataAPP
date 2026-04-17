package com.mcpratapp.dto.request

import com.mcpratapp.model.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class RegisterRequest(
    @Email
    val email: String,
    @NotBlank(message = "A senha não pode ser nula")
    val password: String,
    @NotBlank(message = "Nome não pode ser nulo")
    val name: String,
    @NotNull(message = "O cargo não pode ser nulo")
    val role: Role
)
