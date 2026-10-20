package com.mcpratapp.messaging.sqs

import com.mcpratapp.dto.request.PaymentProcessMessage
import com.mcpratapp.messaging.port.PaymentQueueProducer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("prod")
class SqsPaymentProducer (
    private val sqsTemplate: SqsTemplate,

    @Value("\${app.sqs.payment-queue:payment-process-queue}")
    private val queueName: String
): PaymentQueueProducer {
    private val logger = LoggerFactory.getLogger(SqsPaymentProducer::class.java)

    override fun enqueuePayment(message: PaymentProcessMessage) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        sendToSqs(message)
                    }
                }
            )
        } else {
            sendToSqs(message)
        }
    }

    private fun sendToSqs(message: PaymentProcessMessage) {
        logger.info(
            "Enviando pagamento para fila: orderId={}, paymentId={}, valor={}",
            message.orderId,
            message.paymentId,
            message.amount
        )

        sqsTemplate.send(queueName, message)
    }

}