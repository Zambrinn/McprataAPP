package com.mcpratapp.dto.request

import com.mcpratapp.model.Role
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.apache.logging.log4j.util.StringMap

data class UserRequest(
    @field:NotBlank(message = "O nome do usuário não pode ser nulo")
    val name: String,
    @field:NotBlank(message = "O email não pode ser nulo")
    val email: String,
    @field:NotBlank(message = "A senha não pode ser nula")
    val password: String,
    @field:NotNull(message =  "A role do usuário não pode ser nula")
    val role: Role
)
