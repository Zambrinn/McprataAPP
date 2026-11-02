package com.mcpratapp.messaging.port

import com.mcpratapp.dto.request.PaymentProcessMessage

interface PaymentQueueProducer {
    fun enqueuePayment(message: PaymentProcessMessage)
}