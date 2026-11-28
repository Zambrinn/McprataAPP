package com.mcpratapp.messaging

import com.mcpratapp.dto.request.PaymentProcessMessage
import com.mcpratapp.dto.response.OrderResponse
import com.mcpratapp.messaging.rabbitmq.RabbitPaymentConsumer
import com.mcpratapp.messaging.sqs.SqsPaymentConsumer
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.PaymentMethod
import com.mcpratapp.service.OrderService
import com.mcpratapp.service.SseService
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PaymentConsumerTest {

    @Mock
    private lateinit var orderService: OrderService

    @Mock
    private lateinit var sseService: SseService

    private fun sampleMessage() = PaymentProcessMessage(
        orderId = UUID.randomUUID(),
        paymentId = UUID.randomUUID(),
        amount = BigDecimal("100.00"),
        paymentMethod = PaymentMethod.PIX
    )

    private fun sampleResponse(orderId: UUID) = OrderResponse(
        id = orderId,
        clientId = UUID.randomUUID(),
        vendorId = UUID.randomUUID(),
        status = OrderStatus.CONFIRMED,
        totalAmount = BigDecimal("100.00"),
        discountAmount = BigDecimal.ZERO,
        items = emptyList(),
        createdAt = LocalDateTime.now(),
        confirmedAt = LocalDateTime.now(),
        deliveredAt = null
    )

    @Test
    fun `RabbitPaymentConsumer confirma pagamento e notifica via SSE`() {
        val consumer = RabbitPaymentConsumer(orderService, sseService)
        val message = sampleMessage()
        val response = sampleResponse(message.orderId)
        `when`(orderService.confirmPayment(message.orderId, message.paymentId)).thenReturn(response)

        consumer.consumePayment(message)

        verify(orderService).confirmPayment(message.orderId, message.paymentId)
        verify(sseService).notifyPaymentConfirmed(response.id, response)
    }

    @Test
    fun `RabbitPaymentConsumer relanca excecao e nao notifica SSE em caso de erro`() {
        val consumer = RabbitPaymentConsumer(orderService, sseService)
        val message = sampleMessage()
        `when`(orderService.confirmPayment(message.orderId, message.paymentId)).thenThrow(RuntimeException("Falha"))

        assertThrows(RuntimeException::class.java) { consumer.consumePayment(message) }
        verify(sseService, never()).notifyPaymentConfirmed(message.orderId, message)
    }

    @Test
    fun `SqsPaymentConsumer confirma pagamento e notifica via SSE`() {
        val consumer = SqsPaymentConsumer(orderService, sseService)
        val message = sampleMessage()
        val response = sampleResponse(message.orderId)
        `when`(orderService.confirmPayment(message.orderId, message.paymentId)).thenReturn(response)

        consumer.consumePayment(message)

        verify(orderService).confirmPayment(message.orderId, message.paymentId)
        verify(sseService).notifyPaymentConfirmed(response.id, response)
    }

    @Test
    fun `SqsPaymentConsumer relanca excecao em caso de erro`() {
        val consumer = SqsPaymentConsumer(orderService, sseService)
        val message = sampleMessage()
        `when`(orderService.confirmPayment(message.orderId, message.paymentId)).thenThrow(IllegalStateException("Erro SQS"))

        assertThrows(IllegalStateException::class.java) { consumer.consumePayment(message) }
    }
}
