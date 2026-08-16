package com.mcpratapp.service

import com.mcpratapp.dto.response.SalesReportItemResponse
import com.mcpratapp.dto.response.SalesReportResponse
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.Role
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.security.AuthenticatedUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ReportService(
    private val orderRepository: OrderRepository
) {

    fun generateSalesReport(
        status: OrderStatus?,
        clientId: UUID?,
        vendorId: UUID?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        requester: AuthenticatedUser
    ): SalesReportResponse {
        val effectiveVendorId = if (requester.role == Role.VENDOR) requester.id else vendorId

        val orders = orderRepository.findOrdersWithFilters(
            status = status,
            clientId = clientId,
            vendorId = effectiveVendorId,
            startDate = startDate,
            endDate = endDate
        )

        val paidOrders = orders.filter { it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.DELIVERED }
        val totalRevenue = paidOrders.fold(BigDecimal.ZERO) { acc, order -> acc + order.totalAmount }

        val totalDiscounts = orders.fold(BigDecimal.ZERO) { acc, order -> acc + order.discountAmount }
        val totalItemsSold = orders.flatMap { it.items }.sumOf { it.quantity }

        val items = orders.map { order ->
            SalesReportItemResponse(
                orderId = order.id!!,
                clientName = order.client.name,
                vendorName = order.vendor.username,
                status = order.status,
                totalAmount = order.totalAmount,
                discountAmount = order.discountAmount,
                createdAt = order.createdAt
            )
        }

        return SalesReportResponse(
            totalRevenue = totalRevenue,
            totalDiscounts = totalDiscounts,
            totalOrders = orders.size,
            totalItemsSold = totalItemsSold,
            items = items
        )
    }
}