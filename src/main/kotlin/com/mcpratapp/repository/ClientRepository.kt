package com.mcpratapp.repository

import com.mcpratapp.model.Client
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ClientRepository : JpaRepository<Client, UUID> {
    fun findByEmail(email: String): Client?
    fun findByWhatsappNumber(whatsappNumber: String): Client?
    fun existsByWhatsappNumberAndIdNot(whatsappNumber: String, id: UUID): Boolean
    fun existsByEmailAndIdNot(email: String, id: UUID): Boolean
}