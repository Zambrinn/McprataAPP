package com.mcpratapp.messaging.rabbitmq

import com.mcpratapp.config.RabbitMQConfig
import com.mcpratapp.dto.request.PaymentProcessMessage
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.PaymentStatus
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.PaymentRepository
import com.mcpratapp.service.SseService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class RabbitPaymentConsumer (
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val sseService: SseService
) {
    private val logger = LoggerFactory.getLogger(RabbitPaymentConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.PAYMENT_QUEUE])
    fun consumePayment(message: PaymentProcessMessage) {
        logger.info(
            "Consumindo pagamento da fila: orderId={}, paymentId={}, valor={}",
            message.orderId,
            message.paymentId,
            message.amount
        )

        try {
            val payment = paymentRepository.findById(message.paymentId).orElse(null)
            if (payment != null) {
                payment.status = PaymentStatus.PAID
                payment.paidAt = LocalDateTime.now()
                paymentRepository.save(payment)
            }

            val order = orderRepository.findById(message.orderId).orElse(null)
            if (order != null) {
                order.status = OrderStatus.CONFIRMED
                order.confirmedAt = LocalDateTime.now()
                orderRepository.save(order)
            }

            logger.info("Notificando cliente via tunel SSE para o pedido: {}", order.id)
            sseService.notifyPaymentConfirmed(
                orderId = order.id!!,
                payload = mapOf(
                    "orderId" to order.id,
                    "status" to "CONFIRMED",
                    "paymentStatus" to "PAID",
                    "confirmedAt" to order.confirmedAt.toString()
                )
            )
        } catch (e: Exception) {
            logger.error("Erro ao processar pagamento para o pedido {}: {}", message.orderId, e.message)
            throw e
        }
    }
}