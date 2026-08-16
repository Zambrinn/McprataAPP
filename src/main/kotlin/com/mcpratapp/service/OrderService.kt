package com.mcpratapp.service

import com.mcpratapp.dto.request.ConfirmOrderRequest
import com.mcpratapp.dto.request.OrderDiscountRequest
import com.mcpratapp.dto.response.OrderItemResponse
import com.mcpratapp.dto.response.OrderResponse
import com.mcpratapp.dto.response.PaymentResponse
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ForbidenException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderItem
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.Payment
import com.mcpratapp.model.PaymentMethod
import com.mcpratapp.model.PaymentStatus
import com.mcpratapp.model.Role
import com.mcpratapp.model.UserStatus
import com.mcpratapp.repository.ClientRepository
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.PaymentRepository
import com.mcpratapp.repository.ProductRepository
import com.mcpratapp.repository.ProductVendorRepository
import com.mcpratapp.repository.UserRepository
import com.mcpratapp.security.AuthenticatedUser
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.aspectj.weaver.ast.Or
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.system.exitProcess

@Service
@Transactional
class OrderService (
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val clientRepository: ClientRepository,
    private val productVendorRepository: ProductVendorRepository,
    private val paymentRepository: PaymentRepository,
    private val userRepository: UserRepository,
    private val entityManager: EntityManager
) {
    fun createEmptyOrder(vendorId: UUID, clientId: UUID): OrderResponse {
        val vendor = userRepository.findById(vendorId)
            .orElseThrow { ResourceNotFoundException("Vendedor não encontrado.") }

        val client = clientRepository.findById(clientId)
            .orElseThrow { ResourceNotFoundException("Cliente não encontrado.") }

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

    fun applyDiscount(orderId: UUID, request: OrderDiscountRequest, requester: AuthenticatedUser): OrderResponse {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw ResourceNotFoundException("Não foi possível encontrar um pedido com id: $orderId")

        assertVendorOwnership(order, requester)

        if (paymentRepository.findByOrderId(orderId) != null) {
            throw ConflictException("Não é possível alterar uma venda com pagamento registrado.")
        }

        if (order.status != OrderStatus.PENDING) {
            throw ConflictException("Não é possível adicionar desconto em um pedido que não está pendente.")
        }

        if (order.items.isEmpty()) {
            throw ConflictException("O pedido não tem nenhum item.")
        }

        val subtotal = order.items.fold(BigDecimal.ZERO) { total, item ->
            total + item.subtotal
        }

        val maxDiscount = subtotal * BigDecimal("0.20")

        if (request.discountAmount > maxDiscount) {
            throw ConflictException("O desconto não pode ser maior que 20% do subtotal da venda")
        }

        order.discountAmount = request.discountAmount
        order.totalAmount = subtotal - request.discountAmount

        return orderRepository.save(order).toResponse()
    }

    fun addItemToOrder(orderId: UUID, productId: UUID, quantity: Int, requester: AuthenticatedUser): OrderResponse {
        val existingOrder = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Pedido não encontrado.") }

        assertVendorOwnership(existingOrder, requester)

        val existingProduct = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Produto não encontrado.") }

        if (paymentRepository.findByOrderId(orderId) != null) {
            throw ConflictException("Não é possível alterar uma venda com pagamento registrado.")
        }

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
        recalculateTotal(existingOrder)

        val updatedOrder = orderRepository.save(existingOrder)
        return updatedOrder.toResponse()
    }

    fun confirmOrder(orderId: UUID, paymentMethod: PaymentMethod, requester: AuthenticatedUser): OrderResponse {
        val existingOrder = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Pedido não encontrado.") }

        assertVendorOwnership(existingOrder, requester)
        registerPayment(orderId, paymentMethod)

        return existingOrder.toResponse()
    }

    fun registerPayment(orderId: UUID, paymentMethod: PaymentMethod): PaymentResponse {
        val existingOrder = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Pedido não encontrado.") }

        if (existingOrder.status != OrderStatus.PENDING) {
            throw ConflictException("Só é possível registrar pagamento para uma venda pendente.")
        }

        if (existingOrder.items.isEmpty()) {
            throw ConflictException("Não é possível registrar pagamento para uma venda sem itens.")
        }

        if (paymentRepository.findByOrderId(orderId) != null) {
            throw ConflictException("Esta venda já possui pagamento registrado.")
        }

        val payment = Payment(
            order = existingOrder,
            method = paymentMethod,
            amount = existingOrder.totalAmount
        )

        return paymentRepository.save(payment).toResponse()
    }

    fun confirmPayment(orderId: UUID, paymentId: UUID): OrderResponse {
        val existingOrder = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Pedido não encontrado.") }

        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { ResourceNotFoundException("Pagamento não encontrado.") }

        if (payment.order.id != existingOrder.id) {
            throw ConflictException("O pagamento ${payment.id} não pertence ao pedido ${existingOrder.id}.")
        }

        if (existingOrder.status != OrderStatus.PENDING) {
            throw ConflictException("Só é possível confirmar pagamento de uma venda pendente.")
        }

        if (payment.status != PaymentStatus.PENDING) {
            throw ConflictException("O pagamento já foi processado.")
        }

        existingOrder.items.forEach { item ->
            item.product.apply {
                totalQuantity -= item.quantity
                reservedQuantity -= item.quantity
            }
        }

        payment.status = PaymentStatus.PAID
        payment.paidAt = LocalDateTime.now()
        existingOrder.status = OrderStatus.CONFIRMED
        existingOrder.confirmedAt = LocalDateTime.now()

        paymentRepository.save(payment)
        productRepository.saveAll(existingOrder.items.map { it.product })
        orderRepository.save(existingOrder)

        return existingOrder.toResponse()
    }

    fun deliverOrder(orderId: UUID, requester: AuthenticatedUser): OrderResponse {
        val existingOrder = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Pedido não encontrado.") }

        assertVendorOwnership(existingOrder, requester)

        if (existingOrder.status != OrderStatus.CONFIRMED) {
            throw ConflictException("Só é possível entregar uma venda com pagamento confirmado.")
        }

        existingOrder.status = OrderStatus.DELIVERED
        existingOrder.deliveredAt = LocalDateTime.now()

        return orderRepository.save(existingOrder).toResponse()
    }

    fun cancelOrder(orderId: UUID, requester: AuthenticatedUser): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Pedido não encontrado.") }

        assertVendorOwnership(order, requester)

        if (order.status != OrderStatus.PENDING) {
            throw ConflictException("Só é possível cancelar uma venda pendente.")
        }

        order.items.forEach { item ->
            item.product.reservedQuantity -= item.quantity
        }

        paymentRepository.findByOrderId(orderId)?.let { payment ->
            payment.status = PaymentStatus.FAILED
            paymentRepository.save(payment)
        }

        order.status = OrderStatus.CANCELED

        productRepository.saveAll(order.items.map { it.product })
        return orderRepository.save(order).toResponse()
    }

    fun getOrders(
        status: OrderStatus?,
        clientId: UUID?,
        vendorId: UUID?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        requester: AuthenticatedUser
    ): List<OrderResponse> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(Order::class.java)
        val root = query.from(Order::class.java)
        val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
        val effectiveVendorId = if (requester.role == Role.VENDOR) requester.id else vendorId

        status?.let {
            predicates.add(criteriaBuilder.equal(root.get<OrderStatus>("status"), it))
        }

        clientId?.let {
            predicates.add(criteriaBuilder.equal(root.get<Any>("client").get<UUID>("id"), it))
        }

        effectiveVendorId?.let {
            predicates.add(criteriaBuilder.equal(root.get<Any>("vendor").get<UUID>("id"), it))
        }

        startDate?.let {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), it))
        }

        endDate?.let {
            predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), it))
        }

        query
            .select(root)
            .where(*predicates.toTypedArray())
            .orderBy(criteriaBuilder.desc(root.get<LocalDateTime>("createdAt")))

        return entityManager.createQuery(query).resultList.map { it.toResponse() }
    }

    fun getOrderByID(orderId: UUID): OrderResponse? {
        val foundOrder = orderRepository.findByIdOrNull(orderId)
        return foundOrder?.toResponse()
    }

    private fun calculateSubtotal(order: Order): BigDecimal {
        return order.items.fold(BigDecimal.ZERO) { total, item ->
            total + item.subtotal
        }
    }

    private fun recalculateTotal(order: Order) {
        order.totalAmount = calculateSubtotal(order) - order.discountAmount
    }

    private fun convertOrderItemToDto(item: OrderItem): OrderItemResponse {
        return OrderItemResponse(
            id = item.id ?: throw IllegalStateException("Item do pedido salvo sem ID."),
            productId = item.product.id ?: throw ConflictException("É necessário informar o id do produto."),
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            subtotal = item.subtotal
        )
    }

    private fun assertVendorOwnership(order: Order, requester: AuthenticatedUser) {
        if (requester.role == Role.ADMIN) return
        if (order.vendor.id != requester.id) {
            throw ForbidenException("Você não tem permissão para acessar esse pedido")
        }
    }

    private fun Order.toResponse(): OrderResponse {
        return OrderResponse(
            id = this.id!!,
            clientId = this.client.id ?: throw IllegalStateException("Pedido salvo sem id do cliente."),
            vendorId = this.vendor.id ?: throw IllegalStateException("Vendedor sem ID, inválido."),
            status = this.status,
            totalAmount = this.totalAmount,
            discountAmount = this.discountAmount,
            items = this.items.map { convertOrderItemToDto(it) },
            createdAt = this.createdAt,
            confirmedAt = this.confirmedAt,
            deliveredAt = this.deliveredAt
        )
    }

    private fun Payment.toResponse(): PaymentResponse {
        return PaymentResponse(
            id = this.id ?: throw IllegalStateException("Pagamento salvo sem ID."),
            orderId = this.order.id ?: throw IllegalStateException("O pagamento precisa ter o id do pedido."),
            method = this.method,
            status = this.status,
            amount = this.amount,
            paidAt = this.paidAt,
            createdAt = this.createdAt
        )
    }
}
