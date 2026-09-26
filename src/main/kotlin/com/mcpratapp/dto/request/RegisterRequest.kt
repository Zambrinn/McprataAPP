package com.mcpratapp.dto.request

import com.mcpratapp.model.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class RegisterRequest(
    @field:NotBlank(message = "O e-mail não pode ser nulo")
    @field:Email(message = "E-mail inválido")
    val email: String,
    @field:NotBlank(message = "A senha não pode ser nula")
    val password: String,
    @field:NotBlank(message = "Nome não pode ser nulo")
    val name: String,
    @field:NotNull(message = "O cargo não pode ser nulo")
    val role: Role
)
