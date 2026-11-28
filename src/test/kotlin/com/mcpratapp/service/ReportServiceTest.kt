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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ReportServiceTest {

    @Mock
    private lateinit var orderService: OrderService

    @InjectMocks
    private lateinit var reportService: ReportService

    @Test
    fun `generateSalesReport consolida receita de pagos, descontos e itens vendidos`() {
        val requester = AuthenticatedUser(UUID.randomUUID(), "admin@test.com", Role.ADMIN)
        val client = Client(
            id = UUID.randomUUID(),
            name = "Maria",
            whatsappNumber = "5511999999999",
            email = "m@test.com",
            address = "Rua A",
            cpf = "52998224725",
            isActive = true
        )
        val vendor = User(
            id = UUID.randomUUID(),
            username = "joao",
            email = "j@test.com",
            password = "hash",
            role = Role.VENDOR
        )
        val product = Product(id = UUID.randomUUID(), sku = "SKU-1", name = "Brinco", totalQuantity = 50, reservedQuantity = 0)

        val confirmed = Order(id = UUID.randomUUID(), client = client, vendor = vendor, status = OrderStatus.CONFIRMED).apply {
            items.add(OrderItem(id = UUID.randomUUID(), order = this, product = product, quantity = 2, unitPrice = BigDecimal("100.00"), subtotal = BigDecimal("200.00"), vendor = vendor))
            discountAmount = BigDecimal("20.00")
            totalAmount = BigDecimal("180.00")
        }
        val pending = Order(id = UUID.randomUUID(), client = client, vendor = vendor, status = OrderStatus.PENDING).apply {
            items.add(OrderItem(id = UUID.randomUUID(), order = this, product = product, quantity = 3, unitPrice = BigDecimal("50.00"), subtotal = BigDecimal("150.00"), vendor = vendor))
            discountAmount = BigDecimal("10.00")
            totalAmount = BigDecimal("140.00")
        }

        `when`(orderService.getOrdersEntities(null, null, null, null, null, requester))
            .thenReturn(listOf(confirmed, pending))

        val report = reportService.generateSalesReport(null, null, null, null, null, requester)

        assertEquals(BigDecimal("180.00"), report.totalRevenue)
        assertEquals(BigDecimal("30.00"), report.totalDiscounts)
        assertEquals(2, report.totalOrders)
        assertEquals(5, report.totalItemsSold)
        assertEquals(2, report.items.size)
        assertEquals("Maria", report.items[0].clientName)
        assertEquals("joao", report.items[0].vendorName)
    }
}
