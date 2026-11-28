package com.mcpratapp.service

import com.mcpratapp.exception.ConflictException
import com.stripe.Stripe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class StripeServiceTest {

    @Test
    fun `createPaymentIntent lanca ConflictException quando api key esta vazia`() {
        val service = StripeService("", "whsec_test")

        assertThrows(ConflictException::class.java) {
            service.createPaymentIntent(UUID.randomUUID(), BigDecimal("100.00"))
        }
    }

    @Test
    fun `init configura Stripe apiKey quando chave nao esta vazia`() {
        val service = StripeService("sk_test_12345", "whsec_test")

        service.init()

        assertEquals("sk_test_12345", Stripe.apiKey)
    }
}
