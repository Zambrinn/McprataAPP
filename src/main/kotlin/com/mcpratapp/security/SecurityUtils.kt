package com.mcpratapp.security

import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

object SecurityUtils {
    fun getCurrentUser(): AuthenticatedUser {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Nenhum usuário autenticado encontrado")

        return auth.principal as? AuthenticatedUser
            ?: throw IllegalStateException("Principal do securityHolder não é AuthenticatedUser")
    }

    fun getCurrentUserId(): UUID = getCurrentUser().id
}