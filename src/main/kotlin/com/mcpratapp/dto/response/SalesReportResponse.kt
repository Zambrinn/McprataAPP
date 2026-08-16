package com.mcpratapp.dto.response

import com.mcpratapp.model.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class SalesReportItemResponse(
    val orderId: UUID,
    val clientName: String,
    val vendorName: String,
    val status: OrderStatus,
    val totalAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val createdAt: LocalDateTime?
)

data class SalesReportResponse(
    val totalRevenue: BigDecimal,
    val totalDiscounts: BigDecimal,
    val totalOrders: Int,
    val totalItemsSold: Int,
    val items: List<SalesReportItemResponse>
)