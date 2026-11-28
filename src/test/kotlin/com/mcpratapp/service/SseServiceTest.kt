package com.mcpratapp.service

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class SseServiceTest {

    @Test
    fun `subscribe cria emitter e notifyPaymentConfirmed trata pedido conectado e desconectado`() {
        val service = SseService()
        val connectedOrderId = UUID.randomUUID()
        val unconnectedOrderId = UUID.randomUUID()

        val emitter = service.subscribe(connectedOrderId)
        assertNotNull(emitter)

        service.notifyPaymentConfirmed(connectedOrderId, mapOf("status" to "CONFIRMED"))
        service.notifyPaymentConfirmed(unconnectedOrderId, mapOf("status" to "CONFIRMED"))
    }
}
