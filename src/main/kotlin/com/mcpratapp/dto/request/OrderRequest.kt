package com.mcpratapp.dto.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class OrderRequest(
    @NotNull(message = "client_id não pode ser nulo")
    val clientId: UUID
)
