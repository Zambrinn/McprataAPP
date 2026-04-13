package com.mcpratapp.dto.request

import com.mcpratapp.model.Role
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.apache.logging.log4j.util.StringMap

data class UserRequest(
    @NotBlank(message = "O nome do usuário não pode ser nulo")
    val name: String,
    @NotBlank(message = "O email não pode ser nulo")
    val email: String,
    @NotBlank(message = "A senha não pode ser nula")
    val password: String,
    @NotNull(message =  "A role do usuário não pode ser nula")
    val role: Role
)
