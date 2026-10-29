package com.mcpratapp.controller

import com.mcpratapp.dto.request.PaymentRequest
import com.mcpratapp.dto.response.OrderResponse
import com.mcpratapp.dto.response.PaymentResponse
import com.mcpratapp.service.OrderService
import com.mcpratapp.service.PaymentService
import com.mcpratapp.service.StripeService
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.PaymentIntent
import com.stripe.net.Webhook
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.logging.Logger

@RestController
@RequestMapping("/api/v1/")
class PaymentController (
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val stripeService: StripeService
)   {
    private val logger = LoggerFactory.getLogger(PaymentController::class.java)
    @GetMapping("/payments/{paymentId}")
    fun getPaymentById(@PathVariable paymentId: UUID): ResponseEntity<PaymentResponse> {
        val payment = paymentService.getPaymentById(paymentId)
        return ResponseEntity.ok(payment)
    }

    @GetMapping("/orders/{orderId}/payments")
    fun getPaymentsByOrderId(@PathVariable orderId: UUID): ResponseEntity<List<PaymentResponse>> {
        val payments = paymentService.getPaymentByOrderId(orderId)
        return ResponseEntity.ok(payments)
    }

    @PostMapping("/orders/{orderId}/payments")
    fun registerPayment(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: PaymentRequest
    ): ResponseEntity<PaymentResponse> {
        val payment = orderService.registerPayment(orderId, request.paymentMethod)
        return ResponseEntity.status(HttpStatus.CREATED).body(payment)
    }

    @PostMapping("/orders/{orderId}/payments/{paymentId}/confirm")
    fun confirmPayment(@PathVariable orderId: UUID,
                       @PathVariable paymentId: UUID): ResponseEntity<OrderResponse> {
        val order = orderService.confirmPayment(orderId, paymentId)
        return ResponseEntity.ok(order)
    }

    @PostMapping("/payments/stripe/webhook")
    fun handleStripeWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") sigHeader: String
    ): ResponseEntity<String> {
        try {
            val event = stripeService.validateWebhookSignature(payload, sigHeader)

            if (event.type == "payment_intent.succeeded") {
                val stripeObject = event.dataObjectDeserializer.`object`.orElse(null)
                if (stripeObject is PaymentIntent) {
                    orderService.handleStripePaymentSucceeded(stripeObject.id)
                }
            }
            return ResponseEntity.ok("Received")
        } catch (e: SignatureVerificationException) {
            logger.error("Assinatura de webhook invalida da Stripe: {}", e.message)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature")
        } catch (e: Exception) {
            logger.error("Erro ao processar webhook da Stripe", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error")
        }
    }
}
