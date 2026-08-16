package com.mcpratapp.controller

import com.mcpratapp.dto.response.DashboardSummaryResponse
import com.mcpratapp.security.SecurityUtils
import com.mcpratapp.service.DashboardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController (
    private val dashboardService: DashboardService
) {

    @GetMapping
    fun getDashbord(
        @RequestParam(required = false) startDate: LocalDateTime?,
        @RequestParam(required = false) endDate: LocalDateTime?
    ): ResponseEntity<DashboardSummaryResponse> {
        val currentUser = SecurityUtils.getCurrentUser()
        val summary = dashboardService.getDashboardSummary(startDate, endDate, currentUser)
        return ResponseEntity.ok(summary)
    }
}