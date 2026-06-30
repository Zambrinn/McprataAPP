package com.mcpratapp.service

import com.mcpratapp.dto.request.ConfirmOrderRequest
import com.mcpratapp.dto.response.OrderItemResponse
import com.mcpratapp.dto.response.OrderResponse
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderItem
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.Payment
import com.mcpratapp.model.PaymentStatus
import com.mcpratapp.model.UserStatus
import com.mcpratapp.repository.ClientRepository
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.PaymentRepository
import com.mcpratapp.repository.ProductRepository
import com.mcpratapp.repository.ProductVendorRepository
import com.mcpratapp.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.cglib.core.Local
import org.springframework.data.repository.findByIdOrNull
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

        if (!client.isActive) {
            throw ConflictException("Não é possível registrar venda em um cliente inativo.")
        }

        if (vendor.status != UserStatus.ACTIVE) {
            throw ConflictException("Não é possivel registrar venda para um vendedor inativo.")
        }


        val orderToSave = Order (
            client = client,
            vendor = vendor
        )

        val createdEmptyOrder = orderRepository.save(orderToSave)
        return createdEmptyOrder.toResponse()
    }

    fun addItemToOrder(orderId: UUID, productId: UUID, quantity: Int): OrderResponse {
        val existingOrder = orderRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("Pedido não encontrado.") }

        val existingProduct = productRepository.findById(productId)
            .orElseThrow { IllegalArgumentException("Produto não encontrado.") }

        if (existingOrder.status != OrderStatus.PENDING) {
            throw ConflictException("Só é possível adicionar itens em uma venda pendente.")
        }

        if (!existingProduct.isActive) {
            throw ConflictException("Produto inativo não pode ser adicionado à venda.")
        }

        val disponibleStock = existingProduct.totalQuantity - existingProduct.reservedQuantity

        if (disponibleStock < quantity) {
            throw ConflictException("Sem estoque disponível. Temos no momento: $disponibleStock")
        }

        val vendorId = existingOrder.vendor.id
            ?: throw ConflictException("Vendedor sem ID inválido.")

        val productVendor = productVendorRepository.findByVendorIdAndProductId(vendorId, productId)
            ?: throw ConflictException("Este vendedor não vende este produto.")

        if (!productVendor.isActive) {
            throw ConflictException("Este produto não está ativo para este vendedor.")
        }

        val unitPrice = productVendor.price
        val subtotal = quantity.toBigDecimal() * unitPrice
        existingOrder.totalAmount += subtotal

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
            .orElseThrow { ConflictException("Pedido não encontrado.") }

        if (existingOrder.status != OrderStatus.PENDING) {
            throw ConflictException("O status do pedido ao ser criado tem que ser pendente.")
        }

        if (existingOrder.items.isEmpty()) {
            throw ConflictException("Não é possível confirmar uma venda sem itens.")
        }

        existingOrder.items.forEach { item ->
            item.product.apply {
                totalQuantity -= item.quantity
                reservedQuantity -= item.quantity
            }
        }

        val payment = Payment(
            order = existingOrder,
            method = request.paymentMethod,
            status = PaymentStatus.PAID,
            amount = existingOrder.totalAmount,
            paidAt = LocalDateTime.now(),
        )

        paymentRepository.save(payment)
        productRepository.saveAll(existingOrder.items.map { it.product })

        existingOrder.status = OrderStatus.DELIVERED
        existingOrder.confirmedAt = LocalDateTime.now()
        existingOrder.deliveredAt = LocalDateTime.now()

        val orderToSave = orderRepository.save(existingOrder)
        return orderToSave.toResponse()
    }

//    fun confirmPayment(orderId: UUID, paymentId: UUID): OrderResponse {
//        val existingOrder = orderRepository.findById(orderId)
//            .orElseThrow { IllegalArgumentException("Pedido não encontrado.") }
//
//        val payment = paymentRepository.findById(paymentId)
//            .orElseThrow { IllegalArgumentException("Pagamento não encontrado.")  }
//
//        if (payment.order.id != existingOrder.id) {
//            throw IllegalArgumentException("O pagamento ${payment.order.id} não pertence ao pedido ${existingOrder.id}")
//        }
//
//        if (existingOrder.status != OrderStatus.CONFIRMED) {
//            throw IllegalArgumentException("O status do pedido deveria estar confirmado.")
//        }
//
//        if (payment.status != PaymentStatus.PENDING) {
//            throw IllegalArgumentException("O pagamento já foi processado.")
//        }
//
//        existingOrder.items.forEach { item ->
//            item.product.apply {
//                totalQuantity -= item.quantity
//                reservedQuantity -= item.quantity
//            }
//        }
//
//        payment.status = PaymentStatus.PAID
//        payment.paidAt = LocalDateTime.now()
//        existingOrder.status = OrderStatus.COMPLETED
//
//        paymentRepository.save(payment)
//        productRepository.saveAll(existingOrder.items.map { it.product })
//        orderRepository.save(existingOrder)
//
//        return existingOrder.toResponse()
//    }

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
            productId = item.product.id ?: throw ConflictException("É necessário informar o id do produto."),
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            subtotal = item.subtotal
        )
    }

    private fun Order.toResponse(): OrderResponse {
        return OrderResponse(
            id = this.id!!,
            clientId = this.client.id ?: throw IllegalStateException("Pedido salvo sem id do cliente."),
            vendorId = this.vendor.id ?: throw IllegalStateException("Vendedor sem ID, inválido."),
            status = this.status,
            totalAmount = this.totalAmount,
            items = this.items.map { convertOrderItemToDto(it) },
            createdAt = this.createdAt,
            confirmedAt = this.confirmedAt,
            deliveredAt = this.deliveredAt
        )
    }
}
