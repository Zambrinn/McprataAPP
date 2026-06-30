package com.mcpratapp.repository

import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByVendorIdAndStatus(vendorId: UUID, status: OrderStatus): List<Order>
    fun findByClientIdAndStatus(clientId: UUID, status: OrderStatus): List<Order>
    @Query("""
      SELECT o FROM Order o
      WHERE (:status IS NULL OR o.status = :status)
        AND (:clientId IS NULL OR o.client.id = :clientId)
        AND (:vendorId IS NULL OR o.vendor.id = :vendorId)
        AND (:startDate IS NULL OR o.createdAt >= :startDate)
        AND (:endDate IS NULL OR o.createdAt < :endDate)
  """)
    fun findOrdersWithFilters(
        @Param("status") status: OrderStatus?,
        @Param("clientId") clientId: UUID?,
        @Param("vendorId") vendorId: UUID?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): List<Order>
}