package com.mcpratapp.controller

import com.mcpratapp.dto.request.ConfirmOrderRequest
import com.mcpratapp.dto.request.OrderItemRequest
import com.mcpratapp.dto.request.OrderRequest
import com.mcpratapp.dto.response.OrderResponse
import com.mcpratapp.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/orders")
class OrderController (
    private val orderService: OrderService
) {
    @PostMapping
    fun createOrder(@Valid @RequestBody request: OrderRequest): ResponseEntity<OrderResponse> {
        val order = orderService.createEmptyOrder(request.vendorId, request.clientId)
        return ResponseEntity.status(HttpStatus.CREATED).body(order)
    }

    @PostMapping("/{orderId}/items")
    fun addItemToOrder(@PathVariable orderId: UUID,
                       @Valid @RequestBody request: OrderItemRequest
    ): ResponseEntity<OrderResponse> {
        val order = orderService.addItemToOrder(orderId, request.productId, request.quantity)
        return ResponseEntity.status(HttpStatus.OK).body(order)
    }

    @PostMapping("/{orderId}/confirm")
    fun confirmOrder(@PathVariable orderId: UUID,
                     @Valid @RequestBody request: ConfirmOrderRequest
    ): ResponseEntity<OrderResponse> {
        if (orderId != request.orderId) {
            throw IllegalArgumentException("Order ID da URL não corresponde ao do request body")
        }

        val order = orderService.confirmOrder(request)
        return ResponseEntity.status(HttpStatus.OK).body(order)
    }

    @GetMapping
    fun getOrders(): List<OrderResponse> {
        return orderService.getOrders()
    }

    @GetMapping("/{orderId}")
    fun getOrderById(@PathVariable orderId: UUID): ResponseEntity<OrderResponse> {
        val foundOrder = orderService.getOrderByID(orderId)
        return foundOrder?.let { order -> ResponseEntity.ok(order) }
            ?: ResponseEntity.notFound().build()
    }
}