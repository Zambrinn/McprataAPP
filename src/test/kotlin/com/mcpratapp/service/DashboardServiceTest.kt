package com.mcpratapp.service

import com.mcpratapp.model.Client
import com.mcpratapp.model.Order
import com.mcpratapp.model.OrderItem
import com.mcpratapp.model.OrderStatus
import com.mcpratapp.model.Product
import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import com.mcpratapp.security.AuthenticatedUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class DashboardServiceTest {

    @Mock
    private lateinit var orderService: OrderService

    @InjectMocks
    private lateinit var dashboardService: DashboardService

    private val client = Client(
        id = UUID.randomUUID(),
        name = "Cliente",
        whatsappNumber = "5511999999999",
        email = "c@test.com",
        address = "Rua A",
        cpf = "52998224725",
        isActive = true
    )

    private fun user(name: String) = User(
        id = UUID.randomUUID(),
        username = name,
        email = "$name@test.com",
        password = "hash",
        role = Role.VENDOR
    )

    private fun product(name: String) = Product(
        id = UUID.randomUUID(),
        sku = "SKU-$name",
        name = name,
        totalQuantity = 100,
        reservedQuantity = 0
    )

    private fun order(vendor: User, status: OrderStatus, prod: Product, qty: Int, price: BigDecimal): Order {
        val o = Order(id = UUID.randomUUID(), client = client, vendor = vendor, status = status)
        val sub = price * qty.toBigDecimal()
        o.items.add(OrderItem(id = UUID.randomUUID(), order = o, product = prod, quantity = qty, unitPrice = price, subtotal = sub, vendor = vendor))
        o.totalAmount = sub
        return o
    }

    @Test
    fun `getDashboardSummary calcula receita, ticket medio, top 5 produtos e performance para ADMIN`() {
        val admin = AuthenticatedUser(UUID.randomUUID(), "admin@test.com", Role.ADMIN)
        val v1 = user("alice")
        val v2 = user("bob")
        val p1 = product("Anel")
        val p2 = product("Colar")

        val orders = listOf(
            order(v1, OrderStatus.CONFIRMED, p1, 3, BigDecimal("100.00")),
            order(v2, OrderStatus.DELIVERED, p2, 5, BigDecimal("50.00")),
            order(v1, OrderStatus.PENDING, p1, 10, BigDecimal("100.00")),
            order(v2, OrderStatus.CANCELED, p2, 2, BigDecimal("50.00"))
        )

        `when`(orderService.getOrdersEntities(null, null, null, null, null, admin)).thenReturn(orders)

        val summary = dashboardService.getDashboardSummary(null, null, admin)

        assertEquals(4L, summary.totalOrders)
        assertEquals(BigDecimal("550.00"), summary.totalRevenue)
        assertEquals(BigDecimal("275.00"), summary.averageTicket)
        assertEquals(1L, summary.ordersByStatus[OrderStatus.CONFIRMED])
        assertEquals(1L, summary.ordersByStatus[OrderStatus.DELIVERED])
        assertEquals(1L, summary.ordersByStatus[OrderStatus.PENDING])
        assertEquals(1L, summary.ordersByStatus[OrderStatus.CANCELED])
        assertEquals("Colar", summary.topProducts[0].productName)
        assertEquals(5, summary.topProducts[0].quantitySold)
        assertNotNull(summary.vendorPerformance)
        assertEquals("alice", summary.vendorPerformance!![0].vendorName)
        assertEquals(BigDecimal("300.00"), summary.vendorPerformance!![0].totalRevenue)
    }

    @Test
    fun `getDashboardSummary oculta vendorPerformance para VENDOR e retorna ticket zero quando vazio`() {
        val vendor = AuthenticatedUser(UUID.randomUUID(), "v@test.com", Role.VENDOR)
        `when`(orderService.getOrdersEntities(null, null, null, null, null, vendor)).thenReturn(emptyList())

        val summary = dashboardService.getDashboardSummary(null, null, vendor)

        assertEquals(0L, summary.totalOrders)
        assertEquals(BigDecimal.ZERO, summary.totalRevenue)
        assertEquals(BigDecimal.ZERO, summary.averageTicket)
        assertNull(summary.vendorPerformance)
    }
}
