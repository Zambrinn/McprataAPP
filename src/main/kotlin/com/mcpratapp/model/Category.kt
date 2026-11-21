package com.mcpratapp.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID
import java.time.Instant
import java.time.LocalDateTime

@Entity
@Table(name = "categories")
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID ?= null,

    @Column(nullable = false, length = 150)
    var name: String,

    @Column(nullable = false, length = 150)
    var skuPrefix: String ?= null,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)