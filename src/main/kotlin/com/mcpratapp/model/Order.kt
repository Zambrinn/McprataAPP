package com.mcpratapp.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "orders")
class Order (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    val client: Client,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    val vendor: User, // Quem criou o order

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(nullable = false, precision = 12, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 12, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItem> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "confirmed_at")
    var confirmedAt: LocalDateTime? = null,

    @Column(name = "delivered_at")
    var deliveredAt: LocalDateTime? = null,

    @Column(name = "canceled_at")
    var canceledAt: LocalDateTime? = null
) {
    fun transitionTo(newStatus: OrderStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw IllegalStateException(
                "Transição de status inválida: não é permitido mudar de ${this.status} para $newStatus"
            )
        }
        this.status = newStatus

        when(newStatus) {
            OrderStatus.CONFIRMED -> this.confirmedAt = LocalDateTime.now()
            OrderStatus.DELIVERED -> this.deliveredAt = LocalDateTime.now()
            OrderStatus.CANCELED -> this.canceledAt = LocalDateTime.now()
            else -> {}
        }
    }
}

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    DELIVERED,
    CANCELED;

    fun canTransitionTo(newStatus: OrderStatus): Boolean {
        return when (this) {
            OrderStatus.PENDING -> newStatus in listOf(CONFIRMED, OrderStatus.CANCELED)
            OrderStatus.CONFIRMED -> newStatus in listOf(DELIVERED, OrderStatus.CANCELED)
            OrderStatus.DELIVERED -> false
            OrderStatus.CANCELED -> false
        }
    }
}

@Entity
@Table(name = "order_items")
class OrderItem (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false, precision = 10, scale = 2)
    var unitPrice: BigDecimal,

    @Column(nullable = false, precision = 12, scale = 2)
    var subtotal: BigDecimal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    val vendor: User
)
