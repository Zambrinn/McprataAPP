package com.mcpratapp.service

import com.mcpratapp.anyNonNull
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Client
import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderItem
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.Product
import com.mcpratapp.model.ProductVendor
import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.ProductRepository
import com.mcpratapp.repository.ProductVendorRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class OrderStatusServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var productVendorRepository: ProductVendorRepository

    @InjectMocks
    private lateinit var orderStatusService: OrderStatusService

    private val vendor = User(
        id = UUID.randomUUID(),
        username = "vendedor",
        email = "v@test.com",
        password = "hash",
        role = Role.VENDOR
    )

    private val client = Client(
        id = UUID.randomUUID(),
        name = "Cliente",
        whatsappNumber = "5511999999999",
        email = "c@test.com",
        address = "Rua A",
        cpf = "52998224725",
        isActive = true
    )

    private fun product(total: Int = 10, reserved: Int = 2, isActive: Boolean = true) = Product(
        id = UUID.randomUUID(),
        sku = "SKU-1",
        name = "Colar",
        totalQuantity = total,
        reservedQuantity = reserved,
        isActive = isActive
    )

    private fun orderWithItem(status: OrderStatus = OrderStatus.PENDING, prod: Product = product()): Order {
        val order = Order(id = UUID.randomUUID(), client = client, vendor = vendor, status = status)
        order.items.add(
            OrderItem(
                id = UUID.randomUUID(),
                order = order,
                product = prod,
                quantity = 2,
                unitPrice = BigDecimal("80.00"),
                subtotal = BigDecimal("160.00"),
                vendor = vendor
            )
        )
        return order
    }

    @Test
    fun `confirmOrder e approveOrder baixam estoque e confirmam pedido`() {
        val prod = product(total = 10, reserved = 2)
        val order = orderWithItem(OrderStatus.PENDING, prod)
        `when`(orderRepository.findById(order.id!!)).thenReturn(Optional.of(order))
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { it.getArgument<Order>(0) }

        val confirmed = orderStatusService.approveOrder(order.id!!)

        assertEquals(OrderStatus.CONFIRMED, confirmed.status)
        assertNotNull(confirmed.confirmedAt)
        assertEquals(8, prod.totalQuantity)
        assertEquals(0, prod.reservedQuantity)
    }

    @Test
    fun `markAsDelivered transiciona pedido CONFIRMED para DELIVERED`() {
        val order = orderWithItem(OrderStatus.CONFIRMED)
        `when`(orderRepository.findById(order.id!!)).thenReturn(Optional.of(order))
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { it.getArgument<Order>(0) }

        val delivered = orderStatusService.markAsDelivered(order.id!!)

        assertEquals(OrderStatus.DELIVERED, delivered.status)
        assertNotNull(delivered.deliveredAt)
    }

    @Test
    fun `cancelOrder libera estoque reservado e marca CANCELED`() {
        val prod = product(total = 10, reserved = 5)
        val order = orderWithItem(OrderStatus.PENDING, prod)
        `when`(orderRepository.findById(order.id!!)).thenReturn(Optional.of(order))
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { it.getArgument<Order>(0) }

        val canceled = orderStatusService.cancelOrder(order.id!!, "Desistência")

        assertEquals(OrderStatus.CANCELED, canceled.status)
        assertEquals(3, prod.reservedQuantity)
    }

    @Test
    fun `addItemToOrder adiciona item, reserva estoque e recalcula total com desconto`() {
        val prod = product(total = 10, reserved = 0)
        val order = Order(id = UUID.randomUUID(), client = client, vendor = vendor, status = OrderStatus.PENDING)
        order.discountAmount = BigDecimal("10.00")
        val pv = ProductVendor(id = UUID.randomUUID(), product = prod, vendor = vendor, price = BigDecimal("50.00"))

        `when`(orderRepository.findById(order.id!!)).thenReturn(Optional.of(order))
        `when`(productRepository.findById(prod.id!!)).thenReturn(Optional.of(prod))
        `when`(productVendorRepository.findByVendorIdAndProductId(vendor.id!!, prod.id!!)).thenReturn(pv)
        `when`(orderRepository.save(anyNonNull<Order>())).thenAnswer { it.getArgument<Order>(0) }

        val updated = orderStatusService.addItemToOrder(order.id!!, prod.id!!, 2)

        assertEquals(2, prod.reservedQuantity)
        assertEquals(BigDecimal("90.00"), updated.totalAmount)
        assertEquals(1, updated.items.size)
    }

    @Test
    fun `addItemToOrder valida pedido nao encontrado, status nao pendente e produto inativo`() {
        val missingId = UUID.randomUUID()
        `when`(orderRepository.findById(missingId)).thenReturn(Optional.empty())
        assertThrows(ResourceNotFoundException::class.java) {
            orderStatusService.addItemToOrder(missingId, UUID.randomUUID(), 1)
        }

        val confirmed = orderWithItem(OrderStatus.CONFIRMED)
        `when`(orderRepository.findById(confirmed.id!!)).thenReturn(Optional.of(confirmed))
        assertThrows(ConflictException::class.java) {
            orderStatusService.addItemToOrder(confirmed.id!!, UUID.randomUUID(), 1)
        }

        val pending = orderWithItem(OrderStatus.PENDING)
        val inactiveProd = product(isActive = false)
        `when`(orderRepository.findById(pending.id!!)).thenReturn(Optional.of(pending))
        `when`(productRepository.findById(inactiveProd.id!!)).thenReturn(Optional.of(inactiveProd))
        assertThrows(ConflictException::class.java) {
            orderStatusService.addItemToOrder(pending.id!!, inactiveProd.id!!, 1)
        }
    }
}
