package com.mcpratapp.repository

import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByVendorIdAndStatus(vendorId: UUID, status: OrderStatus): List<Order>
    fun findByClientIdAndStatus(clientId: UUID, status: OrderStatus): List<Order>
}