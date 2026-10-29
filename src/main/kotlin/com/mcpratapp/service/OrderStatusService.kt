package com.mcpratapp.service

import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderItem
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.ProductRepository
import com.mcpratapp.repository.ProductVendorRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
@Transactional
class OrderStatusService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val productVendorRepository: ProductVendorRepository
) {
    private val logger = LoggerFactory.getLogger(OrderStatusService::class.java)

    fun confirmOrder(orderId: UUID): Order {
        val order = getOrder(orderId)
        order.transitionTo(OrderStatus.CONFIRMED)

        for (item in order.items) {
            val product = item.product
            product.totalQuantity -= item.quantity
            product.reservedQuantity -= item.quantity
            productRepository.save(product)
        }

        logger.info("Pedido {} confirmado com sucesso.", orderId)
        return orderRepository.save(order)
    }

    fun approveOrder(orderId: UUID): Order = confirmOrder(orderId)

    fun markAsDelivered(orderId: UUID): Order {
        val order = getOrder(orderId)
        order.transitionTo(OrderStatus.DELIVERED)
        logger.info("Pedido {} marcado como entregue.", orderId)
        return orderRepository.save(order)
    }

    fun cancelOrder(orderId: UUID, reason: String? = null): Order {
        val order = getOrder(orderId)
        order.transitionTo(OrderStatus.CANCELED)

        for (item in order.items) {
            val product = item.product
            product.reservedQuantity -= item.quantity
            productRepository.save(product)
        }

        logger.warn("Pedido {} cancelado. Motivo: {}", orderId, reason ?: "Não informado")
        return orderRepository.save(order)
    }

    fun addItemToOrder(orderId: UUID, productId: UUID, quantity: Int): Order {
        val order = getOrder(orderId)

        if (order.status != OrderStatus.PENDING) {
            throw ConflictException("Itens só podem ser adicionados em pedidos PENDENTES.")
        }

        val product = productRepository.findById(productId).orElseThrow {
            ResourceNotFoundException("Produto não encontrado para o ID: $productId")
        }

        if (!product.isActive) {
            throw ConflictException("Produto inativo não pode ser adicionado ao pedido.")
        }

        val availableStock = product.totalQuantity - product.reservedQuantity
        if (availableStock < quantity) {
            throw ConflictException(
                "Estoque insuficiente para '${product.name}'. Disponível: $availableStock, solicitado: $quantity"
            )
        }

        val vendorId = order.vendor.id ?: throw ConflictException("Vendedor do pedido inválido.")
        val productVendor = productVendorRepository.findByVendorIdAndProductId(vendorId, productId)
            ?: throw ConflictException("Este vendedor não possui autorização/preço definido para este produto.")

        if (!productVendor.isActive) {
            throw ConflictException("Este produto está inativo para este vendedor.")
        }

        val unitPrice = productVendor.price
        val subtotal = unitPrice * quantity.toBigDecimal()

        product.reservedQuantity += quantity
        productRepository.save(product)

        val orderItem = OrderItem(
            order = order,
            product = product,
            quantity = quantity,
            unitPrice = unitPrice,
            subtotal = subtotal,
            vendor = order.vendor
        )

        order.items.add(orderItem)

        val itemsSubtotal = order.items.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.subtotal
        }
        order.totalAmount = itemsSubtotal - order.discountAmount

        return orderRepository.save(order)
    }

    private fun getOrder(orderId: UUID): Order {
        return orderRepository.findById(orderId).orElseThrow {
            ResourceNotFoundException("Pedido não encontrado para o ID: $orderId")
        }
    }
}
