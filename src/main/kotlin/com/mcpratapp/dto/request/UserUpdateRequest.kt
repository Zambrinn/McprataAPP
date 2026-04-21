package com.mcpratapp.dto.request

import com.mcpratapp.model.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UserUpdateRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    val name: String,

    @field:Email(message = "Email é inválido")
    @field:NotBlank(message = "Email é obrigatório")
    val email: String,

    @field:Size(min = 6, message = "Senha deve conter pelo menos 6 caracteres")
    val password: String? = null,

    @field:NotNull(message = "Role é obrigatório")
    val role: Role
)
