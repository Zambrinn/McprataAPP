package com.mcpratapp.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ClientRequest (
    @field:NotBlank(message = "O nome não pode ser nulo")
    val name: String,
    @field:NotBlank(message = "O número de whatsapp não pode ser nulo")
    val whatsappNumber: String,
    @field:NotBlank(message = "O e-mail não pode ser nulo")
    @field:Email(message = "E-mail inválido")
    val email: String,
    @field:NotBlank(message = "O endereço não pode ser nulo")
    val address: String,
    val companyName: String?,
)