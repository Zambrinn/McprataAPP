package com.mcpratapp.service

import com.mcpratapp.dto.response.DashboardSummaryResponse
import com.mcpratapp.dto.response.TopProductsResponse
import com.mcpratapp.dto.response.VendorPerfomanceResponse
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.Role
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.security.AuthenticatedUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class DashboardService (
    private val orderRepository: OrderRepository
) {
    fun getDashboardSummary(
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        requester: AuthenticatedUser
    ): DashboardSummaryResponse {
        val effectiveVendorId = if (requester.role == Role.VENDOR) requester.id else null

        val orders = orderRepository.findOrdersWithFilters(
            status = null,
            clientId = null,
            vendorId = effectiveVendorId,
            startDate = startDate,
            endDate = endDate
        )

        val totalOrders = orders.size.toLong()

        val ordersByStatus = OrderStatus.entries.associateWith { status ->
            orders.count { it.status == status }.toLong()
        }

        val paidOrders = orders.filter { it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.DELIVERED }

        val totalRevenue = paidOrders.fold(BigDecimal.ZERO) { acc, order ->
            acc + order.totalAmount
        }

        val averageTicket = if (paidOrders.isNotEmpty()) {
            totalRevenue.divide(BigDecimal(paidOrders.size), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val topProducts = paidOrders
            .flatMap { it.items }
            .groupBy { it.product.id to it.product.name }
            .map { (key, items) ->
                TopProductsResponse(
                    productId = key.first ?: throw IllegalStateException("Produto sem ID"),
                    productName = key.second,
                    quantitySold = items.sumOf { it.quantity },
                    totalRevenue = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.subtotal}
                )
            }
            .sortedByDescending { it.quantitySold }
            .take(5)
        val vendorsPerformance = if (requester.role == Role.ADMIN) {
            orders
                .groupBy { it.vendor }
                .map { (vendor, vendorOrders) ->
                    val paid = vendorOrders.filter { it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.DELIVERED }
                    val revenue = paid.fold(BigDecimal.ZERO) { acc, o -> acc + o.totalAmount}
                    VendorPerfomanceResponse(
                        vendorId = vendor.id!!,
                        vendorName = vendor.username,
                        totalOrders = vendorOrders.size.toLong(),
                        totalRevenue = revenue
                    )
                }
                .sortedByDescending { it.totalRevenue }
        } else {
            null
        }

        return DashboardSummaryResponse(
            totalRevenue = totalRevenue,
            totalOrders = totalOrders,
            averageTicket = averageTicket,
            ordersByStatus = ordersByStatus,
            topProducts = topProducts,
            vendorPerformance = vendorsPerformance
        )
    }
}