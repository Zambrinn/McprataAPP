package com.mcpratapp.messaging.sqs

import com.mcpratapp.dto.request.PaymentProcessMessage
import com.mcpratapp.service.OrderService
import com.mcpratapp.service.SseService
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("prod")
class SqsPaymentConsumer(
    private val orderService: OrderService,
    private val sseService: SseService
) {
    private val logger = LoggerFactory.getLogger(SqsPaymentConsumer::class.java)

    @SqsListener("\${app.sqs.payment-queue:payment-process-queue}")
    fun consumePayment(message: PaymentProcessMessage) {
        logger.info(
            "Consumindo pagamento do AWS SQS: orderId={}, paymentId={}, valor={}",
            message.orderId,
            message.paymentId,
            message.amount
        )

        try {
            val confirmedOrder = orderService.confirmPayment(message.orderId, message.paymentId)

            logger.info("Notificando cliente via túnel SSE para o pedido: {}", confirmedOrder.id)
            sseService.notifyPaymentConfirmed(
                confirmedOrder.id,
                confirmedOrder
            )
        } catch (e: Exception) {
            logger.error("Erro ao processar pagamento SQS para o pedido {}: {}", message.orderId, e.message)
            throw e
        }
    }
}