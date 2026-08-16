package com.mcpratapp.dto.response

import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderStatus
import java.math.BigDecimal
import java.util.UUID

data class TopProductsResponse(
    val productId: UUID,
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: BigDecimal
)

data class VendorPerfomanceResponse(
    val vendorId: UUID,
    val vendorName: String,
    val totalOrders: Long,
    val totalRevenue: BigDecimal
)

data class DashboardSummaryResponse(
    val totalRevenue: BigDecimal,
    val totalOrders: Long,
    val averageTicket: BigDecimal,
    val ordersByStatus: Map<OrderStatus, Long>,
    val topProducts: List<TopProductsResponse>,
    val vendorPerformance: List<VendorPerfomanceResponse>? = null
)