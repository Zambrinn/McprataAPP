package com.mcpratapp.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class OrderModelTest {

    private fun newOrder(status: OrderStatus = OrderStatus.PENDING): Order {
        val client = Client(
            id = UUID.randomUUID(),
            name = "Cliente Teste",
            whatsappNumber = "5511999999999",
            email = "cliente@test.com",
            address = "Rua A, 123",
            cpf = "52998224725",
            isActive = true
        )
        val vendor = User(
            id = UUID.randomUUID(),
            username = "vendedor",
            email = "vendedor@test.com",
            password = "hash",
            role = Role.VENDOR,
            status = UserStatus.ACTIVE
        )
        return Order(id = UUID.randomUUID(), client = client, vendor = vendor, status = status)
    }

    @Test
    fun `PENDING pode transicionar para CONFIRMED e CANCELED`() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED))
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELED))
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING))
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED))
    }

    @Test
    fun `CONFIRMED pode transicionar para DELIVERED e CANCELED`() {
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DELIVERED))
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELED))
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PENDING))
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CONFIRMED))
    }

    @Test
    fun `DELIVERED e CANCELED sao estados finais`() {
        OrderStatus.entries.forEach { target ->
            assertFalse(OrderStatus.DELIVERED.canTransitionTo(target))
            assertFalse(OrderStatus.CANCELED.canTransitionTo(target))
        }
    }

    @Test
    fun `transitionTo para CONFIRMED atualiza status e confirmedAt`() {
        val order = newOrder(OrderStatus.PENDING)

        order.transitionTo(OrderStatus.CONFIRMED)

        assertEquals(OrderStatus.CONFIRMED, order.status)
        assertNotNull(order.confirmedAt)
        assertNull(order.deliveredAt)
        assertNull(order.canceledAt)
    }

    @Test
    fun `transitionTo para DELIVERED atualiza status e deliveredAt`() {
        val order = newOrder(OrderStatus.CONFIRMED)

        order.transitionTo(OrderStatus.DELIVERED)

        assertEquals(OrderStatus.DELIVERED, order.status)
        assertNotNull(order.deliveredAt)
    }

    @Test
    fun `transitionTo para CANCELED atualiza status e canceledAt`() {
        val order = newOrder(OrderStatus.PENDING)

        order.transitionTo(OrderStatus.CANCELED)

        assertEquals(OrderStatus.CANCELED, order.status)
        assertNotNull(order.canceledAt)
    }

    @Test
    fun `transitionTo invalida lanca IllegalStateException`() {
        val order = newOrder(OrderStatus.PENDING)

        assertThrows(IllegalStateException::class.java) {
            order.transitionTo(OrderStatus.DELIVERED)
        }
        assertEquals(OrderStatus.PENDING, order.status)
    }
}
