package com.mcpratapp.controller

import com.mcpratapp.dto.response.SalesReportResponse
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.security.SecurityUtils
import com.mcpratapp.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/reports")
class ReportController(
    private val reportService: ReportService
) {

    @GetMapping("/sales")
    fun getSalesReport(
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(required = false) clientId: UUID?,
        @RequestParam(required = false) vendorId: UUID?,
        @RequestParam(required = false) startDate: LocalDateTime?,
        @RequestParam(required = false) endDate: LocalDateTime?
    ): ResponseEntity<SalesReportResponse> {
        val currentUser = SecurityUtils.getCurrentUser()
        val report = reportService.generateSalesReport(
            status = status,
            clientId = clientId,
            vendorId = vendorId,
            startDate = startDate,
            endDate = endDate,
            requester = currentUser
        )
        return ResponseEntity.ok(report)
    }
}