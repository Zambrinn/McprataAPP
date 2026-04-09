package com.mcpratapp.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "clients")
class Client (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 150)
    val name: String,

    @Column(nullable = false, unique = true, length = 13)
    val whatsappNumber: String,

    @Column(nullable = true, unique = true, length = 150)
    val email: String? = null,

    @Column(nullable = false, length = 300)
    val address: String,

    @Column(nullable = true, length = 150)
    val companyName: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
}