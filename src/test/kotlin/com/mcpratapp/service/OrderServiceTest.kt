package com.mcpratapp.service

import com.mcpratapp.anyNonNull
import com.mcpratapp.dto.request.OrderDiscountRequest
import com.mcpratapp.dto.request.PaymentProcessMessage
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ForbidenException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.messaging.port.PaymentQueueProducer
import com.mcpratapp.model.Client
import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderItem
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.Payment
import com.mcpratapp.model.PaymentMethod
import com.mcpratapp.model.PaymentStatus
import com.mcpratapp.model.Product
import com.mcpratapp.model.ProductVendor
import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import com.mcpratapp.model.UserStatus
import com.mcpratapp.repository.ClientRepository
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.PaymentRepository
import com.mcpratapp.repository.ProductRepository
import com.mcpratapp.repository.ProductVendorRepository
import com.mcpratapp.repository.UserRepository
import com.mcpratapp.security.AuthenticatedUser
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var clientRepository: ClientRepository

    @Mock
    private lateinit var productVendorRepository: ProductVendorRepository

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var entityManager: EntityManager

    @Mock
    private lateinit var paymentQueueProducer: PaymentQueueProducer

    @Mock
    private lateinit var stripeService: StripeService

    @InjectMocks
    private lateinit var orderService: OrderService

    private val vendorId = UUID.randomUUID()
    private val otherVendorId = UUID.randomUUID()
    private val adminId = UUID.randomUUID()
    private val clientId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    private val vendorAuth = AuthenticatedUser(vendorId, "vendor@test.com", Role.VENDOR)
    private val otherVendorAuth = AuthenticatedUser(otherVendorId, "other@test.com", Role.VENDOR)
    private val adminAuth = AuthenticatedUser(adminId, "admin@test.com", Role.ADMIN)

    private fun vendor(status: UserStatus = UserStatus.ACTIVE) = User(
        id = vendorId,
        username = "vendedor",
        email = "vendor@test.com",
        password = "hash",
        role = Role.VENDOR,
        status = status
    )

    private fun client(isActive: Boolean = true) = Client(
        id = clientId,
        name = "Cliente",
        whatsappNumber = "5511999999999",
        email = "cliente@test.com",
        address = "Rua A",
        cpf = "52998224725",
        isActive = isActive
    )

    private fun product(total: Int = 10, reserved: Int = 0, isActive: Boolean = true) = Product(
        id = productId,
        sku = "SKU-1",
        name = "Anel Prata",
        totalQuantity = total,
        reservedQuantity = reserved,
        isActive = isActive
    )

    private fun orderWithItem(
        orderId: UUID = UUID.randomUUID(),
        status: OrderStatus = OrderStatus.PENDING,
        product: Product = product(total = 10, reserved = 2),
        quantity: Int = 2,
        unitPrice: BigDecimal = BigDecimal("100.00")
    ): Order {
        val v = vendor()
        val order = Order(id = orderId, client = client(), vendor = v, status = status)
        val subtotal = unitPrice * quantity.toBigDecimal()
        val item = OrderItem(
            id = UUID.randomUUID(),
            order = order,
            product = product,
            quantity = quantity,
            unitPrice = unitPrice,
            subtotal = subtotal,
            vendor = v
        )
        order.items.add(item)
        order.totalAmount = subtotal
        return order
    }

    @Test
    fun `createEmptyOrder cria pedido pendente quando cliente e vendedor estao ativos`() {
        val savedId = UUID.randomUUID()
        `when`(userRepository.findById(vendorId)).thenReturn(Optional.of(vendor()))
        `when`(clientRepository.findById(clientId)).thenReturn(Optional.of(client()))
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { invocation ->
            val arg = invocation.getArgument<Order>(0)
            Order(id = savedId, client = arg.client, vendor = arg.vendor)
        }

        val response = orderService.createEmptyOrder(vendorId, clientId)

        assertEquals(savedId, response.id)
        assertEquals(OrderStatus.PENDING, response.status)
        assertEquals(BigDecimal.ZERO, response.totalAmount)
    }

    @Test
    fun `createEmptyOrder rejeita cliente inativo, vendedor inativo ou entidades ausentes`() {
        `when`(userRepository.findById(vendorId)).thenReturn(Optional.empty())
        assertThrows(ResourceNotFoundException::class.java) { orderService.createEmptyOrder(vendorId, clientId) }

        `when`(userRepository.findById(vendorId)).thenReturn(Optional.of(vendor()))
        `when`(clientRepository.findById(clientId)).thenReturn(Optional.empty())
        assertThrows(ResourceNotFoundException::class.java) { orderService.createEmptyOrder(vendorId, clientId) }

        `when`(clientRepository.findById(clientId)).thenReturn(Optional.of(client(isActive = false)))
        assertThrows(ConflictException::class.java) { orderService.createEmptyOrder(vendorId, clientId) }

        `when`(userRepository.findById(vendorId)).thenReturn(Optional.of(vendor(status = UserStatus.INACTIVE)))
        `when`(clientRepository.findById(clientId)).thenReturn(Optional.of(client(isActive = true)))
        assertThrows(ConflictException::class.java) { orderService.createEmptyOrder(vendorId, clientId) }
    }

    @Test
    fun `applyDiscount aplica desconto ate 20 por cento e bloqueia acima do limite`() {
        val order = orderWithItem(quantity = 2, unitPrice = BigDecimal("100.00"))
        val orderId = order.id!!

        `when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        `when`(paymentRepository.findByOrderId(orderId)).thenReturn(null)
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { it.getArgument<Order>(0) }

        assertThrows(ForbidenException::class.java) {
            orderService.applyDiscount(orderId, OrderDiscountRequest(BigDecimal("10.00")), otherVendorAuth)
        }

        assertThrows(ConflictException::class.java) {
            orderService.applyDiscount(orderId, OrderDiscountRequest(BigDecimal("40.01")), vendorAuth)
        }

        val response = orderService.applyDiscount(orderId, OrderDiscountRequest(BigDecimal("40.00")), vendorAuth)
        assertEquals(BigDecimal("40.00"), response.discountAmount)
        assertEquals(BigDecimal("160.00"), response.totalAmount)
    }

    @Test
    fun `applyDiscount rejeita pedido com pagamento, nao pendente ou sem itens`() {
        val emptyOrder = Order(id = UUID.randomUUID(), client = client(), vendor = vendor(), status = OrderStatus.PENDING)
        val orderId = emptyOrder.id!!
        `when`(orderRepository.findById(orderId)).thenReturn(Optional.of(emptyOrder))
        `when`(paymentRepository.findByOrderId(orderId)).thenReturn(null)

        assertThrows(ConflictException::class.java) {
            orderService.applyDiscount(orderId, OrderDiscountRequest(BigDecimal.ONE), adminAuth)
        }

        val paidOrder = orderWithItem()
        val paidOrderId = paidOrder.id!!
        `when`(orderRepository.findById(paidOrderId)).thenReturn(Optional.of(paidOrder))
        `when`(paymentRepository.findByOrderId(paidOrderId)).thenReturn(
            Payment(id = UUID.randomUUID(), order = paidOrder, method = PaymentMethod.PIX, amount = BigDecimal("200.00"))
        )
        assertThrows(ConflictException::class.java) {
            orderService.applyDiscount(paidOrderId, OrderDiscountRequest(BigDecimal.TEN), adminAuth)
        }
    }

    @Test
    fun `addItemToOrder reserva estoque e recalcula total`() {
        val orderId = UUID.randomUUID()
        val order = Order(id = orderId, client = client(), vendor = vendor(), status = OrderStatus.PENDING)
        val prod = product(total = 10, reserved = 2)
        val pv = ProductVendor(id = UUID.randomUUID(), product = prod, vendor = vendor(), price = BigDecimal("50.00"), isActive = true)

        `when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        `when`(productRepository.findById(productId)).thenReturn(Optional.of(prod))
        `when`(paymentRepository.findByOrderId(orderId)).thenReturn(null)
        `when`(productVendorRepository.findByVendorIdAndProductId(vendorId, productId)).thenReturn(pv)
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { invocation ->
            val saved = invocation.getArgument<Order>(0)
            val mappedItems = saved.items.map {
                OrderItem(
                    id = it.id ?: UUID.randomUUID(),
                    order = saved,
                    product = it.product,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    subtotal = it.subtotal,
                    vendor = it.vendor
                )
            }
            saved.items.clear()
            saved.items.addAll(mappedItems)
            saved
        }

        val response = orderService.addItemToOrder(orderId, productId, 3, vendorAuth)

        assertEquals(5, prod.reservedQuantity)
        assertEquals(BigDecimal("150.00"), response.totalAmount)
        assertEquals(1, response.items.size)
    }

    @Test
    fun `addItemToOrder rejeita estoque insuficiente e produto sem vinculo ativo`() {
        val orderId = UUID.randomUUID()
        val order = Order(id = orderId, client = client(), vendor = vendor(), status = OrderStatus.PENDING)
        val prod = product(total = 5, reserved = 4)

        `when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        `when`(productRepository.findById(productId)).thenReturn(Optional.of(prod))
        `when`(paymentRepository.findByOrderId(orderId)).thenReturn(null)

        assertThrows(ConflictException::class.java) {
            orderService.addItemToOrder(orderId, productId, 2, vendorAuth)
        }

        prod.reservedQuantity = 0
        `when`(productVendorRepository.findByVendorIdAndProductId(vendorId, productId)).thenReturn(null)
        assertThrows(ConflictException::class.java) {
            orderService.addItemToOrder(orderId, productId, 1, vendorAuth)
        }
    }

    @Test
    fun `registerPayment enfileira pagamento para PIX e usa Stripe para CREDIT_CARD`() {
        val order = orderWithItem()
        val orderId = order.id!!
        val paymentId = UUID.randomUUID()

        `when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        `when`(paymentRepository.findByOrderId(orderId)).thenReturn(null)
        `when`(paymentRepository.save(anyNonNull<Payment>())).thenAnswer { invocation ->
            val arg = invocation.getArgument<Payment>(0)
            Payment(
                id = paymentId,
                order = arg.order,
                method = arg.method,
                status = arg.status,
                amount = arg.amount,
                stripePaymentIntentId = arg.stripePaymentIntentId,
                stripeClientSecret = arg.stripeClientSecret
            )
        }

        val pixResponse = orderService.registerPayment(orderId, PaymentMethod.PIX)
        assertEquals(paymentId, pixResponse.id)
        verify(paymentQueueProducer).enqueuePayment(
            PaymentProcessMessage(orderId, paymentId, order.totalAmount, PaymentMethod.PIX)
        )

        `when`(stripeService.createPaymentIntent(orderId, order.totalAmount))
            .thenReturn(StripeIntentResult("pi_123", "secret_123"))

        val cardResponse = orderService.registerPayment(orderId, PaymentMethod.CREDIT_CARD)
        assertEquals("pi_123", cardResponse.stripePaymentIntentId)
        assertEquals("secret_123", cardResponse.stripeClientSecret)
    }

    @Test
    fun `confirmPayment baixa estoque total e reservado e marca pedido como CONFIRMED`() {
        val prod = product(total = 10, reserved = 3)
        val order = orderWithItem(product = prod, quantity = 3)
        val orderId = order.id!!
        val paymentId = UUID.randomUUID()
        val payment = Payment(id = paymentId, order = order, method = PaymentMethod.PIX, amount = order.totalAmount)

        `when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        `when`(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment))
        `when`(paymentRepository.save(anyNonNull<Payment>())).thenAnswer { it.getArgument<Payment>(0) }
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { it.getArgument<Order>(0) }

        val response = orderService.confirmPayment(orderId, paymentId)

        assertEquals(OrderStatus.CONFIRMED, response.status)
        assertNotNull(response.confirmedAt)
        assertEquals(PaymentStatus.PAID, payment.status)
        assertNotNull(payment.paidAt)
        assertEquals(7, prod.totalQuantity)
        assertEquals(0, prod.reservedQuantity)
    }

    @Test
    fun `confirmPayment rejeita pagamento de outro pedido ou ja processado`() {
        val order = orderWithItem()
        val otherOrder = orderWithItem()
        val paymentId = UUID.randomUUID()
        val wrongPayment = Payment(id = paymentId, order = otherOrder, method = PaymentMethod.PIX, amount = BigDecimal.TEN)

        `when`(orderRepository.findById(order.id!!)).thenReturn(Optional.of(order))
        `when`(paymentRepository.findById(paymentId)).thenReturn(Optional.of(wrongPayment))

        assertThrows(ConflictException::class.java) {
            orderService.confirmPayment(order.id!!, paymentId)
        }

        val alreadyPaid = Payment(
            id = paymentId,
            order = order,
            method = PaymentMethod.PIX,
            status = PaymentStatus.PAID,
            amount = BigDecimal.TEN
        )
        `when`(paymentRepository.findById(paymentId)).thenReturn(Optional.of(alreadyPaid))
        assertThrows(ConflictException::class.java) {
            orderService.confirmPayment(order.id!!, paymentId)
        }
    }

    @Test
    fun `deliverOrder entrega pedido confirmado e rejeita pedido pendente`() {
        val confirmedOrder = orderWithItem(status = OrderStatus.CONFIRMED)
        val orderId = confirmedOrder.id!!
        `when`(orderRepository.findById(orderId)).thenReturn(Optional.of(confirmedOrder))
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { it.getArgument<Order>(0) }

        val delivered = orderService.deliverOrder(orderId, vendorAuth)
        assertEquals(OrderStatus.DELIVERED, delivered.status)
        assertNotNull(delivered.deliveredAt)

        val pendingOrder = orderWithItem(status = OrderStatus.PENDING)
        `when`(orderRepository.findById(pendingOrder.id!!)).thenReturn(Optional.of(pendingOrder))
        assertThrows(ConflictException::class.java) {
            orderService.deliverOrder(pendingOrder.id!!, vendorAuth)
        }
    }

    @Test
    fun `cancelOrder libera estoque reservado e marca pagamento pendente como FAILED`() {
        val prod = product(total = 10, reserved = 4)
        val order = orderWithItem(product = prod, quantity = 2)
        val orderId = order.id!!
        val payment = Payment(id = UUID.randomUUID(), order = order, method = PaymentMethod.PIX, amount = order.totalAmount)

        `when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        `when`(paymentRepository.findByOrderId(orderId)).thenReturn(payment)
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { it.getArgument<Order>(0) }

        val canceled = orderService.cancelOrder(orderId, vendorAuth)

        assertEquals(OrderStatus.CANCELED, canceled.status)
        assertEquals(2, prod.reservedQuantity)
        assertEquals(PaymentStatus.FAILED, payment.status)
        verify(paymentRepository).save(payment)
    }

    @Test
    fun `removeItemFromOrder libera estoque e zera desconto e total quando ultimo item e removido`() {
        val prod = product(total = 10, reserved = 2)
        val order = orderWithItem(product = prod, quantity = 2, unitPrice = BigDecimal("100.00"))
        order.discountAmount = BigDecimal("20.00")
        order.totalAmount = BigDecimal("180.00")
        val itemId = order.items.first().id!!

        `when`(orderRepository.findById(order.id!!)).thenReturn(Optional.of(order))
        `when`(paymentRepository.findByOrderId(order.id!!)).thenReturn(null)
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { it.getArgument<Order>(0) }

        val response = orderService.removeItemFromOrder(order.id!!, itemId, vendorAuth)

        assertEquals(0, prod.reservedQuantity)
        assertEquals(0, response.items.size)
        assertEquals(BigDecimal.ZERO, response.discountAmount)
        assertEquals(BigDecimal.ZERO, response.totalAmount)
    }

    @Test
    fun `updateItemQuantity ajusta reserva para mais e para menos e reajusta desconto proporcional`() {
        val prod = product(total = 10, reserved = 2)
        val order = orderWithItem(product = prod, quantity = 2, unitPrice = BigDecimal("100.00"))
        order.discountAmount = BigDecimal("40.00")
        order.totalAmount = BigDecimal("160.00")
        val itemId = order.items.first().id!!

        `when`(orderRepository.findById(order.id!!)).thenReturn(Optional.of(order))
        `when`(paymentRepository.findByOrderId(order.id!!)).thenReturn(null)
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { it.getArgument<Order>(0) }

        assertThrows(ConflictException::class.java) {
            orderService.updateItemQuantity(order.id!!, itemId, 0, vendorAuth)
        }

        val increased = orderService.updateItemQuantity(order.id!!, itemId, 5, vendorAuth)
        assertEquals(5, prod.reservedQuantity)
        assertEquals(BigDecimal("460.00"), increased.totalAmount)

        val decreased = orderService.updateItemQuantity(order.id!!, itemId, 1, vendorAuth)
        assertEquals(1, prod.reservedQuantity)
        assertEquals(0, BigDecimal("20.00").compareTo(decreased.discountAmount))
        assertEquals(0, BigDecimal("80.00").compareTo(decreased.totalAmount))
    }

    @Test
    fun `handleStripePaymentSucceeded enfileira apenas quando pagamento esta PENDING`() {
        val order = orderWithItem()
        val payment = Payment(
            id = UUID.randomUUID(),
            order = order,
            method = PaymentMethod.CREDIT_CARD,
            status = PaymentStatus.PENDING,
            amount = order.totalAmount,
            stripePaymentIntentId = "pi_abc"
        )
        `when`(paymentRepository.findByStripePaymentIntentId("pi_abc")).thenReturn(payment)

        orderService.handleStripePaymentSucceeded("pi_abc")
        verify(paymentQueueProducer).enqueuePayment(
            PaymentProcessMessage(order.id!!, payment.id!!, payment.amount, PaymentMethod.CREDIT_CARD)
        )

        val paidPayment = Payment(
            id = UUID.randomUUID(),
            order = order,
            method = PaymentMethod.CREDIT_CARD,
            status = PaymentStatus.PAID,
            amount = order.totalAmount,
            stripePaymentIntentId = "pi_paid"
        )
        `when`(paymentRepository.findByStripePaymentIntentId("pi_paid")).thenReturn(paidPayment)
        orderService.handleStripePaymentSucceeded("pi_paid")
        verify(paymentQueueProducer, never()).enqueuePayment(
            PaymentProcessMessage(order.id!!, paidPayment.id!!, paidPayment.amount, PaymentMethod.CREDIT_CARD)
        )

        `when`(paymentRepository.findByStripePaymentIntentId("pi_missing")).thenReturn(null)
        assertThrows(ResourceNotFoundException::class.java) {
            orderService.handleStripePaymentSucceeded("pi_missing")
        }
    }

    @Test
    fun `getOrderByID retorna OrderResponse ou null`() {
        val order = orderWithItem()
        `when`(orderRepository.findById(order.id!!)).thenReturn(Optional.of(order))
        assertEquals(order.id, orderService.getOrderByID(order.id!!)?.id)

        val missing = UUID.randomUUID()
        `when`(orderRepository.findById(missing)).thenReturn(Optional.empty())
        assertNull(orderService.getOrderByID(missing))
    }
}
