package com.mcpratapp.service

import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Client
import com.mcpratapp.model.Order
import com.mcpratapp.model.Payment
import com.mcpratapp.model.PaymentMethod
import com.mcpratapp.model.PaymentStatus
import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.PaymentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
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
class PaymentServiceTest {

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var orderRepository: OrderRepository

    @InjectMocks
    private lateinit var paymentService: PaymentService

    private fun sampleOrder(id: UUID = UUID.randomUUID()) = Order(
        id = id,
        client = Client(
            id = UUID.randomUUID(),
            name = "Cliente",
            whatsappNumber = "5511999999999",
            email = "c@test.com",
            address = "Rua A",
            cpf = "52998224725",
            isActive = true
        ),
        vendor = User(
            id = UUID.randomUUID(),
            username = "vendedor",
            email = "v@test.com",
            password = "hash",
            role = Role.VENDOR
        )
    )

    @Test
    fun `getPaymentById retorna pagamento ou lanca ResourceNotFoundException`() {
        val paymentId = UUID.randomUUID()
        val order = sampleOrder()
        val payment = Payment(
            id = paymentId,
            order = order,
            method = PaymentMethod.PIX,
            status = PaymentStatus.PAID,
            amount = BigDecimal("150.00")
        )
        `when`(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment))

        val response = paymentService.getPaymentById(paymentId)
        assertEquals(paymentId, response.id)
        assertEquals(order.id, response.orderId)
        assertEquals(BigDecimal("150.00"), response.amount)

        val missing = UUID.randomUUID()
        `when`(paymentRepository.findById(missing)).thenReturn(Optional.empty())
        assertThrows(ResourceNotFoundException::class.java) { paymentService.getPaymentById(missing) }
    }

    @Test
    fun `getPaymentByOrderId retorna lista com pagamento, lista vazia ou 404 se pedido nao existe`() {
        val order = sampleOrder()
        val payment = Payment(
            id = UUID.randomUUID(),
            order = order,
            method = PaymentMethod.MONEY,
            amount = BigDecimal("80.00")
        )

        `when`(orderRepository.findById(order.id!!)).thenReturn(Optional.of(order))
        `when`(paymentRepository.findByOrderId(order.id!!)).thenReturn(payment)
        val found = paymentService.getPaymentByOrderId(order.id!!)
        assertEquals(1, found.size)
        assertEquals(payment.id, found[0].id)

        `when`(paymentRepository.findByOrderId(order.id!!)).thenReturn(null)
        assertTrue(paymentService.getPaymentByOrderId(order.id!!).isEmpty())

        val missingOrder = UUID.randomUUID()
        `when`(orderRepository.findById(missingOrder)).thenReturn(Optional.empty())
        assertThrows(ResourceNotFoundException::class.java) { paymentService.getPaymentByOrderId(missingOrder) }
    }
}
