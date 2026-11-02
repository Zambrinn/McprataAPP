package com.mcpratapp.messaging.rabbitmq

import com.mcpratapp.config.RabbitMQConfig
import com.mcpratapp.dto.request.PaymentProcessMessage
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.PaymentStatus
import com.mcpratapp.repository.OrderRepository
import com.mcpratapp.repository.PaymentRepository
import com.mcpratapp.service.OrderService
import com.mcpratapp.service.SseService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Profile("!prod")
@Component
class RabbitPaymentConsumer (
    private val orderService: OrderService,
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
            val confirmedOrder = orderService.confirmPayment(message.orderId, message.paymentId)

            logger.info("Notificando cliente via tunel SSE para o pedido: {}", confirmedOrder.id)
            sseService.notifyPaymentConfirmed(
                confirmedOrder.id,
                confirmedOrder
            )
        } catch (e: Exception) {
            logger.error("Erro ao processar pagamento para o pedido {}: {}", message.orderId, e.message)
            throw e
        }
    }
}