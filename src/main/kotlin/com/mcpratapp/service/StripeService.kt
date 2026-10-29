package com.mcpratapp.service

import com.stripe.Stripe
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.net.Webhook
import com.stripe.param.PaymentIntentCreateParams
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class StripeIntentResult(
    val paymentIntentId: String,
    val clientSecret: String
)

@Service
class StripeService (
    @Value("\${stripe.api.key}")
    private val stripeApiKey: String,
    @Value("\${stripe.webhook.secret}")
    private val stripeWebhookSecret: String
) {
    private val logger = LoggerFactory.getLogger(StripeService::class.java)

    @PostConstruct
    fun init() {
        if (stripeApiKey.isNotBlank()) {
            Stripe.apiKey = stripeApiKey
            logger.info("Stripe SDK inicializado")
        } else {
            logger.warn("STRIPE_API_KEY nao configurada! Pagamentos com cartao usarao modo simulado até configurar a chave.")
        }
    }

    fun createPaymentIntent(orderId: UUID, amount: BigDecimal): StripeIntentResult {
        val amountInCents = amount.multiply(BigDecimal(100)).toLong()

        val params = PaymentIntentCreateParams.builder()
            .setAmount(amountInCents)
            .setCurrency("brl")
            .putMetadata("orderId", orderId.toString())
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .build()
        val intent = PaymentIntent.create(params)
        logger.info("Payment Intent criado: id={}, orderId={}, valorCentavos={}", intent.id, orderId, amountInCents)

        return StripeIntentResult(
            intent.id,
            intent.clientSecret
        )
    }

    fun validateWebhookSignature(payload: String, sigHeader: String): Event {
        return Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret)
    }

}