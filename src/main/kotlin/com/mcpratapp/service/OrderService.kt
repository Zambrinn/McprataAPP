package com.mcpratapp.service

import com.mcpratapp.dto.response.OrderItemResponse
import com.mcpratapp.dto.response.OrderResponse
import com.mcpratapp.model.Client
import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderItem
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.repository.ClientRepository
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.PaymentRepository
import com.mcpratapp.repository.ProductRepository
import com.mcpratapp.repository.ProductVendorRepository
import com.mcpratapp.repository.UserRepository
import jakarta.transaction.Transactional
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
        return convertToDto(createdEmptyOrder)
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
            throw IllegalArgumentException("O status da venda ao ser criado tem que ser pendente.")
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
        return convertToDto(updatedOrder)
    }

    fun confirmOrder(orderId: UUID): OrderResponse {
        val existingOrder = orderRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("Pedido não encontrado.") }

        if (existingOrder.status != OrderStatus.PENDING) {
            throw IllegalArgumentException("O status da venda ao ser criado tem que ser pendente.")
        }


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

    private fun convertToDto(order: Order): OrderResponse {
        return OrderResponse(
            id = order.id,
            clientId = order.client.id,
            vendorId = order.vendor.id,
            status = order.status,
            totalAmount = order.totalAmount,
            items = order.items.map { convertOrderItemToDto(it) },
            createdAt = order.createdAt,
            confirmedAt = order.confirmedAt
        )
    }
}