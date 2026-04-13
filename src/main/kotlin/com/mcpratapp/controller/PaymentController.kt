package com.mcpratapp.controller

import com.mcpratapp.dto.response.OrderResponse
import com.mcpratapp.dto.response.PaymentResponse
import com.mcpratapp.service.OrderService
import com.mcpratapp.service.PaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class PaymentController (
    private val paymentService: PaymentService,
    private val orderService: OrderService
)   {
    @GetMapping("/payments/{paymentId}")
    fun getPaymentById(@PathVariable paymentId: UUID): ResponseEntity<PaymentResponse> {
        val payment = paymentService.getPaymentById(paymentId)
        return ResponseEntity.ok(payment)
    }

    @GetMapping("/orders/{orderId}/payments")
    fun getPaymentsByOrderId(@PathVariable orderId: UUID): ResponseEntity<List<PaymentResponse>> {
        val payments = paymentService.getPaymentByOrderId(orderId)
        return ResponseEntity.ok(payments)
    }

    @PostMapping("/orders/{orderId}/payments/{paymentId}/confirm")
    fun confirmPayment(@PathVariable orderId: UUID,
                       @PathVariable paymentId: UUID): ResponseEntity<OrderResponse> {
        val order = orderService.confirmPayment(orderId, paymentId)
        return ResponseEntity.ok(order)
    }

}