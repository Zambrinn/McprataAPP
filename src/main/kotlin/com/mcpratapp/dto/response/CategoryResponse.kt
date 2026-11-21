package com.mcpratapp.dto.response

import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val skuPrefix: String?,
    val isActive: Boolean
)