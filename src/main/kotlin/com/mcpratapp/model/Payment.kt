package com.mcpratapp.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "payments")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    val order: Order,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val method: PaymentMethod,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(nullable = false, precision = 12, scale = 2)
    val amount: BigDecimal,

    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class PaymentMethod {
    PIX,
    TICKET,
    CREDIT_CARD,
    MONEY
}

enum class PaymentStatus {
    PENDING,
    FAILED,
    PAID
}
