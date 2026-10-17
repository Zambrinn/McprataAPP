package com.mcpratapp.messaging.rabbitmq

import com.mcpratapp.config.RabbitMQConfig
import com.mcpratapp.dto.request.PaymentProcessMessage
import com.mcpratapp.messaging.port.PaymentQueueProducer
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class RabbitPaymentProducer (
   private val rabbitTemplate: RabbitTemplate
): PaymentQueueProducer {
    private val logger = LoggerFactory.getLogger(rabbitTemplate::class.java)

    override fun enqueuePayment(message: PaymentProcessMessage) {
        logger.info(
            "Enviando pagamento para fila: orderId={}, paymentId={}, valor={}",
            message.orderId,
            message.paymentId,
            message.amount
        )

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PAYMENT_EXCHANGE,
            RabbitMQConfig.PAYMENT_ROUTING_KEY,
            message
        )
    }

}