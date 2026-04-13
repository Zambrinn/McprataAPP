package com.mcpratapp.service

import com.mcpratapp.dto.request.ConfirmOrderRequest
import com.mcpratapp.dto.response.OrderItemResponse
import com.mcpratapp.dto.response.OrderResponse
import com.mcpratapp.dto.response.PaymentResponse
import com.mcpratapp.model.Client
import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderItem
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.Payment
import com.mcpratapp.model.PaymentStatus
import com.mcpratapp.repository.ClientRepository
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.PaymentRepository
import com.mcpratapp.repository.ProductRepository
import com.mcpratapp.repository.ProductVendorRepository
import com.mcpratapp.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.spel.spi.ExtensionIdAware
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class OrderService (
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val clientRepository: ClientRepository,
    private val productVendorRepository: ProductVendorRepository,
    private val paymentRepository: PaymentRepository,
    private val userRepository: UserRepository
) {
    fun createEmptyOrder(vendorId: UUID, clientId: UUID): OrderResponse {
        val vendor = userRepository.findById(vendorId)
            .orElseThrow { IllegalArgumentException("Vendedor não encontrado.") }

        val client = clientRepository.findById(clientId)
            .orElseThrow { IllegalArgumentException("Cliente não encontrado.") }

        val orderToSave = Order (
            clientId,
            client,
            vendor,
        )

        val createdEmptyOrder = orderRepository.save(orderToSave)
        return createdEmptyOrder.toResponse()
    }

    fun addItemToOrder(orderId: UUID, productId: UUID, quantity: Int): OrderResponse {
        val existingOrder = orderRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("Pedido não encontrado.") }
        val existingProduct = productRepository.findById(productId)
            .orElseThrow { IllegalArgumentException("Produto não encontrado.") }
        val vendorId = existingOrder.vendor.id
        val disponibleStock = existingProduct.totalQuantity - existingProduct.reservedQuantity
        val productVendorValidation = productVendorRepository.findByVendorIdAndProductId(vendorId, productId)
            ?: throw IllegalArgumentException("Este vendedor não vende este produto.")
        val unitPrice = productVendorValidation.price
        val subtotal = quantity.toBigDecimal() * unitPrice
        existingOrder.totalAmount += subtotal
        if (disponibleStock < quantity) {
            throw IllegalArgumentException("Sem estoque disponível. Temos no momento: $disponibleStock")
        }

        if (existingOrder.status != OrderStatus.PENDING) {
            throw IllegalArgumentException("O status do pedido ao ser criado tem que ser pendente.")
        }

        val orderItem = OrderItem(
            order = existingOrder,
            product = existingProduct,
            quantity = quantity,
            unitPrice = unitPrice,
            subtotal = subtotal,
            vendor = existingOrder.vendor
        )
        existingOrder.items.add(orderItem)
        existingProduct.reservedQuantity += quantity

        val updatedOrder = orderRepository.save(existingOrder)
        return updatedOrder.toResponse()
    }

    fun confirmOrder(request: ConfirmOrderRequest): OrderResponse {
        val existingOrder = orderRepository.findById(request.orderId)
            .orElseThrow { IllegalArgumentException("Pedido não encontrado.") }

        if (existingOrder.status != OrderStatus.PENDING) {
            throw IllegalArgumentException("O status do pedido ao ser criado tem que ser pendente.")
        }

        val payment = Payment(
            order = existingOrder,
            method = request.paymentMethod,
            amount = existingOrder.totalAmount,
        )
        paymentRepository.save(payment)

        existingOrder.status = OrderStatus.CONFIRMED
        existingOrder.confirmedAt = LocalDateTime.now()
        val orderToSave = orderRepository.save(existingOrder)
        return orderToSave.toResponse()
    }

    fun confirmPayment(orderId: UUID, paymentId: UUID): OrderResponse {
        val existingOrder = orderRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("Pedido não encontrado.") }

        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { IllegalArgumentException("Pagamento não encontrado.")  }

        if (payment.order.id != existingOrder.id) {
            throw IllegalArgumentException("O pagamento ${payment.order.id} não pertence ao pedido ${existingOrder.id}")
        }

        if (existingOrder.status != OrderStatus.CONFIRMED) {
            throw IllegalArgumentException("O status do pedido deveria estar confirmado.")
        }

        if (payment.status != PaymentStatus.PENDING) {
            throw IllegalArgumentException("O pagamento já foi processado.")
        }

        existingOrder.items.forEach { item ->
            item.product.apply {
                totalQuantity -= item.quantity
                reservedQuantity -= item.quantity
            }
        }

        payment.status = PaymentStatus.PAID
        payment.paidAt = LocalDateTime.now()
        existingOrder.status = OrderStatus.COMPLETED

        paymentRepository.save(payment)
        productRepository.saveAll(existingOrder.items.map { it.product })
        orderRepository.save(existingOrder)

        return existingOrder.toResponse()
    }

    fun getOrders(): List<OrderResponse> {
        val orders: List<Order> = orderRepository.findAll()
        return orders.map { it.toResponse() }
    }

    fun getOrderByID(orderId: UUID): OrderResponse? {
        val foundOrder = orderRepository.findByIdOrNull(orderId)
        return foundOrder?.toResponse()
    }

    private fun convertOrderItemToDto(item: OrderItem): OrderItemResponse {
        return OrderItemResponse(
            id = item.id,
            productId = item.product.id,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            subtotal = item.subtotal
        )
    }

    private fun Order.toResponse(): OrderResponse {
        return OrderResponse(
            id = this.id!!,
            clientId = this.client.id,
            vendorId = this.vendor.id,
            status = this.status,
            totalAmount = this.totalAmount,
            items = this.items.map { convertOrderItemToDto(it) },
            createdAt = this.createdAt,
            confirmedAt = this.confirmedAt
        )
    }
}